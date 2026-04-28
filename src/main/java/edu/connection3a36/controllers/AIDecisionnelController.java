package edu.connection3a36.controllers;

import edu.connection3a36.entities.PlanActions;
import edu.connection3a36.entities.ReferenceArticle;
import edu.connection3a36.enums.CategorieSortie;
import edu.connection3a36.enums.Statut;
import edu.connection3a36.services.GroqService;
import edu.connection3a36.services.MockDataService;
import edu.connection3a36.services.PlanActionsService;
import edu.connection3a36.services.ReferenceArticleService;
import edu.connection3a36.tools.AlertUtil;
import edu.connection3a36.tools.AIJsonParser;
import edu.connection3a36.tools.AIJsonSchemas;
import edu.connection3a36.tools.MarkdownRenderer;
import edu.connection3a36.tools.SessionManager;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import javax.sound.sampled.*;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Contrôleur IA Décisionnelle (Admin) — chatbot stratégique pur.
 */
public class AIDecisionnelController {

    // ── Analyse textuelle ─────────────────────────────────────────────────────
    @FXML private VBox boxAnalyse;

    // ── Chatbot décisionnel ───────────────────────────────────────────────────
    @FXML private ScrollPane chatScroll;
    @FXML private VBox chatBox;
    @FXML private TextField inputField;
    @FXML private Button btnSend;
    @FXML private Button btnRecord;
    @FXML private Label lblChatStatus;
    
    // ── Sous-pages IA Décisionnelle + Hub intervention ───────────────────────
    @FXML private Button tabChatbot, tabInterventions, tabDiagnostic, tabAccessibilite, tabImpact;
    @FXML private VBox paneChatbot, paneInterventions, paneDiagnostic, paneAccessibilite, paneImpact;
    
    // Interventions
    @FXML private ComboBox<String> cbNiveau, cbProfil;
    @FXML private TextArea taIntervention, taRemediation, taScriptSeance;
    
    // Diagnostic
    @FXML private ComboBox<String> cbQ1, cbQ2, cbQ3, cbQ4, cbQ5;
    @FXML private Label lblDiagScore;
    @FXML private TextArea taDiagRecommendation;
    
    // Accessibilité
    @FXML private Button btnDyslexia;
    @FXML private Slider fontSizeSlider, letterSpacingSlider;
    @FXML private VBox previewBox;
    @FXML private Label previewText1, previewText2, previewText3;
    @FXML private ComboBox<String> cbAudioSymbol;
    @FXML private Label lblSoundFeedback;
    @FXML private Label lblPomodoroTime, lblPomodoroMode, lblPomodoroSessions, lblPomodoroMotivation;
    
    // Impact
    @FXML private Label lblAtRiskCount, lblMissingFeedback, lblLatePlans, lblImpactSummary;
    @FXML private TextArea taWeeklyActions, taImpactDetail;

    // ── Services ──────────────────────────────────────────────────────────────
    private final PlanActionsService      planService      = new PlanActionsService();
    private final ReferenceArticleService articleService   = new ReferenceArticleService();
    private final GroqService             groqService      = new GroqService();
    private final edu.connection3a36.services.VoiceRecorderService voiceService = new edu.connection3a36.services.VoiceRecorderService();

    // ── État chatbot ──────────────────────────────────────────────────────────
    private final List<Map<String, String>> conversationHistory = new ArrayList<>();
    private String lastAIResponse = "";

    // Historique persistant
    private static final Map<Integer, List<String[]>> chatHistoryByUser = new HashMap<>();
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("dd/MM HH:mm");
    private Timer pomodoroTimer;
    private int pomodoroSeconds = 25 * 60;
    private boolean pomodoroWork = true, pomodoroRunning = false;
    private int pomodoroSessionsDone = 0;
    private String currentBg = "#fdf6e3";
    private boolean dyslexiaActive = false;
    private static final String[] HUB_QUOTES = {
            "\"Progression > perfection.\"",
            "\"Une séance claire vaut mieux qu'une séance longue.\"",
            "\"Le suivi régulier bat les actions ponctuelles.\"",
            "\"Chaque feedback utile augmente l'impact pédagogique.\""
    };

    @FXML
    public void initialize() {
        addAIMessage("👋 Bonjour ! Je suis votre assistant décisionnel IA.\n\n"
                + "Je suis à votre disposition pour analyser les données de l'établissement, détecter les risques, "
                + "proposer des plans d'actions stratégiques ou rédiger des articles institutionnels.\n\n"
                + "Que souhaitez-vous examiner aujourd'hui ?");
        initHubInterventionSubPages();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CHATBOT DÉCISIONNEL
    // ─────────────────────────────────────────────────────────────────────────

    @FXML
    void handleSend() {
        String message = inputField.getText().trim();
        if (message.isEmpty()) return;

        addUserMessage(message);
        inputField.clear();
        lblChatStatus.setText("⏳ MentorAI analyse...");
        btnSend.setDisable(true);

        final String finalMessage = message;
        new Thread(() -> {
            try {
                String response = groqService.sendMessage(finalMessage, conversationHistory, "ADMIN");

                Map<String, String> userMsg = new HashMap<>();
                userMsg.put("role", "user");
                userMsg.put("content", finalMessage);
                conversationHistory.add(userMsg);

                Map<String, String> aiMsg = new HashMap<>();
                aiMsg.put("role", "assistant");
                aiMsg.put("content", response);
                conversationHistory.add(aiMsg);

                getCurrentUserHistory().add(new String[]{
                        LocalDateTime.now().format(TIME_FMT),
                        finalMessage,
                        response
                });

                lastAIResponse = response;

                Platform.runLater(() -> {
                    addAIMessage(response);
                    lblChatStatus.setText("✅ Réponse reçue");
                    btnSend.setDisable(false);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    addAIMessage("❌ Erreur : " + e.getMessage());
                    lblChatStatus.setText("❌ Erreur");
                    btnSend.setDisable(false);
                });
            }
        }).start();
    }

    @FXML void handleSuggestion1() {
        inputField.setText("Analyse la situation globale de l'établissement ESPRIT à partir des données disponibles. "
                + "Quels sont nos points de vigilance actuels ?");
        handleSend();
    }

    @FXML void handleSuggestion2() {
        inputField.setText("Identifie les risques pédagogiques et organisationnels actuels. "
                + "Quels signaux d'alerte détectes-tu ?");
        handleSend();
    }

    @FXML void handleSuggestion3() {
        inputField.setText("Propose un plan stratégique d'urgence pour améliorer la réussite des étudiants. "
                + "Liste les actions concrètes par priorité.");
        handleSend();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CRÉER UN PLAN D'ACTION DEPUIS LA SORTIE DU CHATBOT
    // ─────────────────────────────────────────────────────────────────────────

    @FXML
    void handleCreatePlan() {
        if (lastAIResponse.isEmpty()) {
            AlertUtil.showError("Aucune réponse IA disponible. Lancez d'abord une conversation.");
            return;
        }

        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Créer un Plan d'Action depuis l'IA");
        dialog.setHeaderText("Convertir la recommandation IA en Plan d'Action");

        VBox content = new VBox(12);
        content.setPadding(new Insets(15));

        TextField tfDecision = new TextField();
        tfDecision.setPromptText("Titre / Décision du plan...");
        tfDecision.setPrefWidth(400);
        String preview = lastAIResponse.length() > 80
                ? lastAIResponse.substring(0, 80).trim().replaceAll("(?m)^[#*]+", "").replaceAll("\n", " ") + "..."
                : lastAIResponse;
        tfDecision.setText("Plan stratégique : " + preview);

        ComboBox<String> cbCat = new ComboBox<>(
                FXCollections.observableArrayList("PEDAGOGIQUE", "STRATEGIQUE", "ADMINISTRATIVE"));
        cbCat.setValue("STRATEGIQUE");

        TextArea taDesc = new TextArea(lastAIResponse.length() > 600
                ? lastAIResponse.substring(0, 600) : lastAIResponse);
        taDesc.setPrefRowCount(5);
        taDesc.setWrapText(true);

        content.getChildren().addAll(
                new Label("Décision (titre) :"), tfDecision,
                new Label("Catégorie :"), cbCat,
                new Label("Description (modifiable) :"), taDesc
        );

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        ((Button) dialog.getDialogPane().lookupButton(ButtonType.OK)).setText("✅ Créer");

        dialog.setResultConverter(bt -> bt == ButtonType.OK ? tfDecision.getText().trim() : null);

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(decision -> {
            if (decision.isEmpty()) { AlertUtil.showError("La décision ne peut pas être vide."); return; }
            try {
                PlanActions plan = new PlanActions();
                plan.setDecision(decision.length() > 200 ? decision.substring(0, 200) : decision);
                String desc = taDesc.getText().trim();
                plan.setDescription(desc.length() > 1000 ? desc.substring(0, 1000) : desc);
                plan.setStatut(Statut.EN_ATTENTE);
                try { plan.setCategorie(CategorieSortie.valueOf(cbCat.getValue())); }
                catch (Exception ignored) { plan.setCategorie(CategorieSortie.STRATEGIQUE); }
                plan.setAuteurId(SessionManager.getCurrentUser().getId());
                plan.setDate(LocalDateTime.now());

                planService.addEntity(plan);
                AlertUtil.showSuccess("✅ Plan d'Action créé avec succès !\n\nID : " + plan.getId());
            } catch (Exception e) {
                AlertUtil.showError("Erreur création plan : " + e.getMessage());
            }
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GÉNÉRER UN ARTICLE IA
    // ─────────────────────────────────────────────────────────────────────────

    @FXML
    void handleGenerateArticle() {
        if (lastAIResponse.isEmpty()) {
            AlertUtil.showError("Aucune réponse IA disponible. Lancez d'abord une conversation.");
            return;
        }

        Dialog<String[]> dialog = new Dialog<>();
        dialog.setTitle("Générer un Article Stratégique");
        dialog.setHeaderText("Faire rédiger un article officiel basé sur la session IA");

        VBox content = new VBox(10);
        content.setPadding(new Insets(15));
        content.setPrefWidth(420);

        TextField tfTitre = new TextField("Bilan stratégique — " + LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
        tfTitre.setPromptText("Titre de l'article...");

        content.getChildren().addAll(
                new Label("Titre de l'article :"), tfTitre,
                new Label("L'IA va composer un article publiable structuré."));

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        ((Button) dialog.getDialogPane().lookupButton(ButtonType.OK)).setText("✍️ Générer");

        dialog.setResultConverter(bt -> bt == ButtonType.OK ? new String[]{tfTitre.getText().trim()} : null);

        Optional<String[]> result = dialog.showAndWait();
        result.ifPresent(arr -> {
            String titre = arr[0];
            if (titre.isEmpty()) { AlertUtil.showError("Le titre ne peut pas être vide."); return; }

            lblChatStatus.setText("⏳ Génération de l'article...");
            btnSend.setDisable(true);

            new Thread(() -> {
                try {
                    String prompt = "Rédige un article officiel très professionnel pour l'école ESPRIT "
                            + "sur le sujet suivant (basé sur la conversation actuelle) :\n\n"
                            + "Titre : " + titre + "\n\n"
                            + "L'article doit comprendre : Introduction, 3 parties stratégiques et une Conclusion. "
                            + "Ton de communication institutionnelle interne.";

                    String articleContent = groqService.sendSimpleJsonMessage(
                            prompt,
                            "ADMIN",
                            AIJsonSchemas.ARTICLE
                    );

                    ReferenceArticle article = new ReferenceArticle();
                    article.setTitre(titre.length() > 255 ? titre.substring(0, 255) : titre);
                    article.setContenu(AIJsonParser.extractMarkdownContent(articleContent));
                    
                    int fallbackCatId = 1;
                    try {
                        edu.connection3a36.services.CategorieArticleService catServ = new edu.connection3a36.services.CategorieArticleService();
                        var cats = catServ.getData();
                        if (!cats.isEmpty()) { fallbackCatId = cats.get(0).getId(); }
                    } catch (Exception ignored) {}
                    article.setCategorieId(fallbackCatId);
                    
                    article.setAuteurId(SessionManager.getCurrentUser().getId());
                    article.setPublished(false);

                    articleService.addEntity(article);

                    Platform.runLater(() -> {
                        AlertUtil.showSuccess("✅ Article officiel généré et sauvegardé (Brouillon) !\n"
                                + "Titre : " + titre + "\n"
                                + "Rendez-vous dans la section Articles pour la publication finale.");
                        lblChatStatus.setText("✅ Article généré");
                        btnSend.setDisable(false);
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        AlertUtil.showError("Erreur génération article : " + e.getMessage());
                        lblChatStatus.setText("❌ Erreur");
                        btnSend.setDisable(false);
                    });
                }
            }).start();
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ANALYSE TEXTUELLE
    // ─────────────────────────────────────────────────────────────────────────

    @FXML
    void handleAnalyse() {
        boxAnalyse.getChildren().clear();
        Label loading = new Label("⏳ L'IA compile les données globales...");
        loading.setStyle("-fx-text-fill: #7a8fa5; -fx-font-style: italic;");
        boxAnalyse.getChildren().add(loading);

        new Thread(() -> {
            try {
                int totalPlans = planService.countAll();
                Map<String, Integer> statuts = planService.countByStatut();
                int totalArticles = articleService.countAll();

                String prompt = String.format(
                        "Tu es l'IA décisionnelle de l'école ESPRIT. Voici un extrait de la base : "
                      + "Total plans : %d (Statuts : %s) — Total articles internes : %d.\n"
                      + "Fais une analyse éclair structurée sur la situation managériale et les points forts.",
                        totalPlans, statuts, totalArticles);

                String response = groqService.sendSimpleJsonMessage(
                        prompt,
                        "ADMIN",
                        AIJsonSchemas.ANALYSIS
                );
                org.json.JSONObject json = AIJsonParser.extractFirstJsonObject(response);
                final String normalized = (json != null)
                        ? "## Analyse de la situation manageriale\n" + json.optString("resume_executif", "")
                        + "\n\n## Points forts\n" + json.optString("points_forts", "")
                        + "\n\n## Axes d'amelioration\n" + json.optString("axes_amelioration", "")
                        : response;

                Platform.runLater(() -> {
                    boxAnalyse.getChildren().clear();
                    renderResponse(normalized, boxAnalyse);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    boxAnalyse.getChildren().clear();
                    Label err = new Label("❌ " + e.getMessage());
                    err.setStyle("-fx-text-fill: #d52e28;");
                    boxAnalyse.getChildren().add(err);
                });
            }
        }).start();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HISTORIQUE & CLEAR
    // ─────────────────────────────────────────────────────────────────────────

    @FXML
    void handleShowHistory() {
        Stage histStage = new Stage();
        histStage.setTitle("🕐 Historique Décisionnel");
        histStage.initModality(Modality.APPLICATION_MODAL);

        VBox root = new VBox(12);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: #f4f7fa;");

        Label title = new Label("🕐 Archives des questions (Décisionnel)");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #102c59;");
        root.getChildren().add(title);

        List<String[]> userHistory = new ArrayList<>(getCurrentUserHistory());
        if (userHistory.isEmpty()) {
            root.getChildren().add(new Label("Aucune conversation en mémoire.") {{
                setStyle("-fx-text-fill: #7a8fa5; -fx-font-style: italic;");
            }});
        } else {
            ScrollPane sp = new ScrollPane();
            sp.setFitToWidth(true);
            sp.setStyle("-fx-background-color: transparent;");

            VBox histList = new VBox(10);
            histList.setPadding(new Insets(5));

            List<String[]> reversed = new ArrayList<>(userHistory);
            Collections.reverse(reversed);

            for (String[] entry : reversed) {
                VBox item = new VBox(6);
                item.getStyleClass().add("history-item");

                HBox header = new HBox(8);
                header.setAlignment(Pos.CENTER_LEFT);
                Label dateLabel = new Label("🕐 " + entry[0]);
                dateLabel.getStyleClass().add("history-item-date");
                Label userLabel = new Label("👤 " + entry[1]);
                userLabel.getStyleClass().add("history-item-user");
                userLabel.setWrapText(true);
                header.getChildren().addAll(dateLabel, userLabel);

                String preview = entry[2].length() > 150
                        ? entry[2].substring(0, 150) + "..." : entry[2];
                Label aiLabel = new Label("🤖 " + preview);
                aiLabel.getStyleClass().add("history-item-ai");

                Button btnReload = new Button("🔄 Reposer la question");
                btnReload.getStyleClass().add("btn-secondary");
                btnReload.setStyle("-fx-font-size: 11px; -fx-padding: 4 10;");
                btnReload.setOnAction(e -> {
                    inputField.setText(entry[1]);
                    histStage.close();
                });

                item.getChildren().addAll(header, aiLabel, btnReload);
                histList.getChildren().add(item);
            }
            sp.setContent(histList);
            sp.setPrefHeight(400);
            root.getChildren().add(sp);
        }

        Button btnClose = new Button("Fermer");
        btnClose.getStyleClass().add("btn-secondary");
        btnClose.setOnAction(e -> histStage.close());
        root.getChildren().add(btnClose);

        Scene scene = new Scene(root, 600, 500);
        scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
        histStage.setScene(scene);
        histStage.show();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GESTION VOCALE (Whisper)
    // ─────────────────────────────────────────────────────────────────────────

    @FXML
    void handleRecord() {
        if (voiceService.isRecording()) {
            btnRecord.setText("🎤");
            btnRecord.setStyle("-fx-padding: 10 15; -fx-cursor: hand;");
            inputField.setPromptText("Transcription en cours...");
            
            new Thread(() -> {
                try {
                    String text = voiceService.stopRecordingAndTranscribe();
                    Platform.runLater(() -> {
                        if (text != null && !text.isEmpty()) {
                            String current = inputField.getText();
                            inputField.setText(current.isEmpty() ? text : current + " " + text);
                        }
                        inputField.setPromptText("Posez votre question décisionnelle à l'IA...");
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        AlertUtil.showError("Erreur dictée vocale : " + e.getMessage());
                        inputField.setPromptText("Posez votre question décisionnelle à l'IA...");
                    });
                }
            }).start();
        } else {
            try {
                voiceService.startRecording();
                btnRecord.setText("⏹️");
                btnRecord.setStyle("-fx-padding: 10 15; -fx-background-color: #ef4444; -fx-text-fill: white; -fx-cursor: hand;");
                inputField.setPromptText("Écoute en cours (parlez maintenant)...");
            } catch (Exception e) {
                AlertUtil.showError("Erreur micro : " + e.getMessage());
            }
        }
    }

    @FXML
    void handleClear() {
        chatBox.getChildren().clear();
        conversationHistory.clear();
        lastAIResponse = "";
        initialize();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UI HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    private void addUserMessage(String message) {
        Label msgLabel = new Label(message);
        msgLabel.setWrapText(true);
        msgLabel.setMaxWidth(380);
        msgLabel.getStyleClass().add("chat-message-user");

        HBox hbox = new HBox(msgLabel);
        hbox.setAlignment(Pos.CENTER_RIGHT);
        hbox.setPadding(new Insets(4, 0, 4, 50));
        chatBox.getChildren().add(hbox);
        scrollToBottom();
    }

    private void addAIMessage(String response) {
        VBox container = new VBox(8);
        container.getStyleClass().add("chat-message-ai");
        container.setMaxWidth(400);

        renderResponse(response, container);

        HBox hbox = new HBox(container);
        hbox.setAlignment(Pos.CENTER_LEFT);
        hbox.setPadding(new Insets(4, 50, 4, 0));
        chatBox.getChildren().add(hbox);
        scrollToBottom();
    }

    private void renderResponse(String response, Pane target) {
        MarkdownRenderer.render(AIJsonParser.extractMarkdownContent(response), target);
    }

    private void renderJsonDashboard(org.json.JSONObject obj, Pane target) {
        VBox dash = new VBox(8);
        dash.setStyle("-fx-background-color: white; -fx-padding: 12; -fx-background-radius: 10; "
                + "-fx-effect: dropshadow(three-pass-box, rgba(16,44,89,0.1), 5, 0, 0, 0);");
        dash.setMaxWidth(480);

        if (obj.has("metrics")) {
            org.json.JSONArray metrics = obj.optJSONArray("metrics");
            if (metrics != null && metrics.length() > 0) {
                HBox metricsBox = new HBox(8);
                for (int j = 0; j < metrics.length(); j++) {
                    org.json.JSONObject m = metrics.getJSONObject(j);
                    VBox card = new VBox(4);
                    card.setStyle("-fx-background-color: #eef4f9; -fx-padding: 8; -fx-background-radius: 8;");
                    String trend = m.optString("trend", "");
                    String color = trend.equals("up") ? "#27ae60" : trend.equals("down") ? "#d52e28" : "#7a8fa5";
                    Label val = new Label(m.optString("value", "") + m.optString("unit", ""));
                    val.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: " + color + ";");
                    Label lbl = new Label(m.optString("label", ""));
                    lbl.setStyle("-fx-font-size: 10px; -fx-text-fill: #102c59;");
                    lbl.setWrapText(true); lbl.setMaxWidth(85);
                    card.getChildren().addAll(val, lbl);
                    metricsBox.getChildren().add(card);
                }
                dash.getChildren().add(metricsBox);
            }
        }

        if (obj.has("alerts")) {
            org.json.JSONArray alerts = obj.optJSONArray("alerts");
            if (alerts != null) {
                for (int j = 0; j < alerts.length(); j++) {
                    org.json.JSONObject a = alerts.getJSONObject(j);
                    String level = a.optString("level", "medium");
                    String bgColor = level.equals("high") ? "#fddcdb" : level.equals("low") ? "#d4edda" : "#fef3cd";
                    String txtColor = level.equals("high") ? "#6b0f0c" : "#102c59";
                    Label alert = new Label("⚠️ " + a.optString("message", ""));
                    alert.setWrapText(true);
                    alert.setStyle("-fx-background-color: " + bgColor + "; -fx-text-fill: " + txtColor
                            + "; -fx-padding: 8; -fx-background-radius: 6; -fx-font-weight: bold; -fx-font-size: 11px;");
                    dash.getChildren().add(alert);
                }
            }
        }
        
        target.getChildren().add(dash);
    }
    
    // ─────────────────────────────────────────────────────────────────────────
    // SOUS-PAGES HUB INTERVENTION (dans IA Décisionnelle)
    // ─────────────────────────────────────────────────────────────────────────
    
    private void initHubInterventionSubPages() {
        if (cbNiveau == null) return; // sécurité si FXML partiel
        cbNiveau.getItems().setAll("L1", "L2", "L3", "M1", "M2", "3A36");
        cbNiveau.setValue("3A36");
        cbProfil.getItems().setAll("À risque", "Dyslexie", "Attention", "Absentéisme", "Irrégulier", "Excellent", "Moyen");
        cbProfil.setValue("À risque");
        
        List<String> scale = Arrays.asList("Jamais", "Rarement", "Parfois", "Souvent");
        cbQ1.getItems().setAll(scale); cbQ2.getItems().setAll(scale); cbQ3.getItems().setAll(scale);
        cbQ4.getItems().setAll(scale); cbQ5.getItems().setAll(scale);
        cbQ1.setValue("Parfois"); cbQ2.setValue("Parfois"); cbQ3.setValue("Parfois");
        cbQ4.setValue("Parfois"); cbQ5.setValue("Parfois");
        
        cbAudioSymbol.getItems().setAll("A", "E", "I", "O", "U", "MA", "PA", "TA", "KA", "LA");
        cbAudioSymbol.setValue("A");
        
        showSubPage(paneChatbot, tabChatbot);
        applyPreview();
        updateHubTimerLabels();
        refreshImpact();
    }
    
    @FXML void showSubChatbot() { showSubPage(paneChatbot, tabChatbot); }
    @FXML void showSubInterventions() { showSubPage(paneInterventions, tabInterventions); }
    @FXML void showSubDiagnostic() { showSubPage(paneDiagnostic, tabDiagnostic); }
    @FXML void showSubAccessibilite() { showSubPage(paneAccessibilite, tabAccessibilite); }
    @FXML void showSubImpact() { showSubPage(paneImpact, tabImpact); refreshImpact(); }
    
    private void showSubPage(VBox pane, Button tab) {
        for (VBox p : new VBox[]{paneChatbot, paneInterventions, paneDiagnostic, paneAccessibilite, paneImpact}) {
            if (p != null) {
                p.setVisible(p == pane);
                p.setManaged(p == pane);
            }
        }
        String on = "-fx-background-color:#3b82f6; -fx-text-fill:white; -fx-background-radius:20; -fx-padding:6 16;";
        String off = "-fx-background-color:#e2e8f0; -fx-text-fill:#102c59; -fx-background-radius:20; -fx-padding:6 16;";
        for (Button b : new Button[]{tabChatbot, tabInterventions, tabDiagnostic, tabAccessibilite, tabImpact}) {
            if (b != null) b.setStyle(b == tab ? on : off);
        }
    }
    
    @FXML
    void generateInterventionPack() {
        taIntervention.setText(generateMicroActivities(cbNiveau.getValue(), cbProfil.getValue()));
        taRemediation.setText(generateRemediationBank(cbProfil.getValue()));
        taScriptSeance.setText(generateSessionScript(cbNiveau.getValue(), cbProfil.getValue()));
    }
    
    private String generateMicroActivities(String niveau, String profil) {
        return "Objectif 15 min - " + profil + " (" + niveau + ")\n"
                + "1) 3 min: activation rapide.\n"
                + "2) 7 min: mini-atelier en binôme.\n"
                + "3) 3 min: restitution orale guidée.\n"
                + "4) 2 min: feedback flash.\n\n"
                + "KPI: participation, réponses correctes, engagement.";
    }
    
    private String generateRemediationBank(String profil) {
        String base = "Banque de remédiation - profil " + profil + "\n";
        if ("Dyslexie".equalsIgnoreCase(profil)) {
            return base + "- Consignes segmentées.\n- Police lisible et interligne renforcé.\n- Evaluation orale flash.";
        }
        if ("Attention".equalsIgnoreCase(profil)) {
            return base + "- Tâche unique de 5 minutes.\n- Rotation active.\n- Checkpoint verbal régulier.";
        }
        if ("Absentéisme".equalsIgnoreCase(profil) || "À risque".equalsIgnoreCase(profil)) {
            return base + "- Micro-objectifs hebdomadaires.\n- Point parent/tuteur.\n- Rattrapage guidé.";
        }
        return base + "- Renforcement méthodologique.\n- Pair tutoring.\n- Auto-évaluation de fin de séance.";
    }
    
    private String generateSessionScript(String niveau, String profil) {
        return "Script prêt à l'emploi\n"
                + "Objectif: stabiliser les acquis du groupe " + niveau + " (focus " + profil + ").\n"
                + "00:00-03:00 cadrage, 03:00-10:00 activité centrale,\n"
                + "10:00-13:00 correction guidée, 13:00-15:00 évaluation flash.";
    }
    
    @FXML
    void runDiagnostic() {
        int score = scaleValue(cbQ1.getValue()) + scaleValue(cbQ2.getValue()) + scaleValue(cbQ3.getValue())
                + scaleValue(cbQ4.getValue()) + scaleValue(cbQ5.getValue());
        lblDiagScore.setText("Score diagnostic: " + score + " / 15");
        if (score >= 11) {
            taDiagRecommendation.setText("Risque FAIBLE.\nAction: approfondissement + autonomie guidée.");
        } else if (score >= 7) {
            taDiagRecommendation.setText("Risque MODÉRÉ.\nAction: remédiation ciblée x2/semaine.");
        } else {
            taDiagRecommendation.setText("Risque ÉLEVÉ.\nAction: intervention immédiate et suivi hebdomadaire.");
        }
    }
    
    private int scaleValue(String value) {
        if ("Jamais".equals(value)) return 0;
        if ("Rarement".equals(value)) return 1;
        if ("Parfois".equals(value)) return 2;
        return 3;
    }
    
    @FXML void setBgCream() { currentBg = "#fdf6e3"; applyPreview(); }
    @FXML void setBgGreen() { currentBg = "#e8f5e9"; applyPreview(); }
    @FXML void setBgBlue() { currentBg = "#e3f2fd"; applyPreview(); }
    @FXML void setBgNormal() { currentBg = "white"; applyPreview(); }
    @FXML void adjustFontSize() { applyPreview(); }
    @FXML void adjustLetterSpacing() { applyPreview(); }
    
    private void applyPreview() {
        if (previewBox == null) return;
        double fs = fontSizeSlider.getValue();
        double ls = letterSpacingSlider.getValue();
        String style = String.format("-fx-font-size:%.0fpx; -fx-letter-spacing:%.1f;", fs, ls);
        previewBox.setStyle("-fx-background-color:" + currentBg + "; -fx-padding:14; -fx-background-radius:10;");
        previewText1.setStyle(style);
        previewText2.setStyle(style);
        previewText3.setStyle(style);
    }
    
    @FXML
    void toggleDyslexiaMode() {
        dyslexiaActive = !dyslexiaActive;
        if (dyslexiaActive) {
            btnDyslexia.setText("📖 Désactiver");
            btnDyslexia.getScene().getRoot().setStyle("-fx-font-family:'Arial'; -fx-font-size:" + fontSizeSlider.getValue() + "px; -fx-background-color:" + currentBg + ";");
        } else {
            btnDyslexia.setText("📖 Mode Dyslexie");
            btnDyslexia.getScene().getRoot().setStyle("");
        }
    }
    
    @FXML
    void playSelectedAudio() {
        String symbol = cbAudioSymbol.getValue();
        lblSoundFeedback.setText("Lecture du son: " + symbol);
        new Thread(() -> {
            if (!speakSymbolWindows(symbol)) playBeepFor(symbol);
            Platform.runLater(() -> lblSoundFeedback.setText("Lecture terminée: " + symbol));
        }).start();
    }
    
    private boolean speakSymbolWindows(String symbol) {
        String text = symbol.replace("'", " ");
        String ps = "Add-Type -AssemblyName System.Speech; "
                + "$s = New-Object System.Speech.Synthesis.SpeechSynthesizer; "
                + "$s.Rate = -1; $s.Speak('" + text + "');";
        try {
            Process p = new ProcessBuilder("powershell", "-Command", ps).start();
            return p.waitFor() == 0;
        } catch (IOException | InterruptedException e) {
            return false;
        }
    }
    
    private void playBeepFor(String symbol) {
        try {
            int freq = 440 + Math.abs(symbol.hashCode() % 400);
            AudioFormat fmt = new AudioFormat(44100, 8, 1, true, false);
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, fmt);
            SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
            line.open(fmt);
            line.start();
            byte[] buf = new byte[(int) (fmt.getSampleRate() * 350 / 1000)];
            for (int i = 0; i < buf.length; i++) {
                buf[i] = (byte) (75 * Math.sin(2 * Math.PI * freq * i / fmt.getSampleRate()));
            }
            line.write(buf, 0, buf.length);
            line.drain();
            line.close();
        } catch (Exception ignored) {}
    }
    
    @FXML
    void togglePomodoro() {
        if (pomodoroRunning) {
            pomodoroTimer.cancel();
            pomodoroRunning = false;
            return;
        }
        pomodoroRunning = true;
        pomodoroTimer = new Timer();
        pomodoroTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(() -> {
                    if (pomodoroSeconds > 0) {
                        pomodoroSeconds--;
                        updateHubTimerLabels();
                        return;
                    }
                    pomodoroTimer.cancel();
                    pomodoroRunning = false;
                    if (pomodoroWork) {
                        pomodoroWork = false;
                        pomodoroSessionsDone++;
                        pomodoroSeconds = 5 * 60;
                        lblPomodoroMode.setText("MODE PAUSE");
                    } else {
                        pomodoroWork = true;
                        pomodoroSeconds = 25 * 60;
                        lblPomodoroMode.setText("MODE TRAVAIL");
                        lblPomodoroMotivation.setText(HUB_QUOTES[pomodoroSessionsDone % HUB_QUOTES.length]);
                    }
                    updateHubTimerLabels();
                });
            }
        }, 1000, 1000);
    }
    
    @FXML
    void resetPomodoro() {
        if (pomodoroTimer != null) pomodoroTimer.cancel();
        pomodoroRunning = false;
        pomodoroWork = true;
        pomodoroSeconds = 25 * 60;
        lblPomodoroMode.setText("MODE TRAVAIL");
        updateHubTimerLabels();
    }
    
    private void updateHubTimerLabels() {
        int m = pomodoroSeconds / 60;
        int s = pomodoroSeconds % 60;
        lblPomodoroTime.setText(String.format("%02d:%02d", m, s));
        lblPomodoroSessions.setText("Sessions finalisées: " + pomodoroSessionsDone);
    }
    
    @FXML
    void refreshImpact() {
        try {
            List<String[]> students = MockDataService.getStudentProfiles();
            int atRisk = 0;
            double avg = 0;
            int abs = 0;
            for (String[] s : students) {
                double m = Double.parseDouble(s[3]);
                int a = Integer.parseInt(s[4]);
                avg += m; abs += a;
                if (s[2].toLowerCase().contains("risque") || a >= 3 || m < 11.0) atRisk++;
            }
            avg /= students.size();
            
            int totalPlans = planService.countAll();
            int feedbackPlans = planService.countWithFeedback();
            int latePlans = planService.countByStatutValue("EN_ATTENTE");
            int missingFeedback = Math.max(0, totalPlans - feedbackPlans);
            
            lblAtRiskCount.setText(String.valueOf(atRisk));
            lblMissingFeedback.setText(String.valueOf(missingFeedback));
            lblLatePlans.setText(String.valueOf(latePlans));
            lblImpactSummary.setText(String.format("Moyenne %.1f | Absences %d | Feedback %d%%", avg, abs, totalPlans > 0 ? (feedbackPlans * 100 / totalPlans) : 0));
            taImpactDetail.setText("Élèves à risque: " + atRisk + "\nPlans sans feedback: " + missingFeedback + "\nPlans en retard: " + latePlans);
        } catch (Exception e) {
            taImpactDetail.setText("Erreur indicateurs: " + e.getMessage());
        }
    }
    
    @FXML
    void generateWeeklyActions() {
        String role = SessionManager.getCurrentUser() != null ? SessionManager.getCurrentUser().getRole() : "";
        taWeeklyActions.setText("Top 3 actions de la semaine (" + role + ")\n"
                + "1) Prioriser les profils à risque.\n"
                + "2) Clôturer les plans en attente avec feedback.\n"
                + "3) Lancer un diagnostic et adapter la séance suivante.");
    }

    private void scrollToBottom() {
        Platform.runLater(() -> chatScroll.setVvalue(1.0));
    }

    private List<String[]> getCurrentUserHistory() {
        int uid = SessionManager.getCurrentUser() != null ? SessionManager.getCurrentUser().getId() : -1;
        return chatHistoryByUser.computeIfAbsent(uid, k -> new ArrayList<>());
    }
}
