package edu.connection3a36.controllers;

import edu.connection3a36.entities.PlanActions;
import edu.connection3a36.entities.ReferenceArticle;
import edu.connection3a36.enums.CategorieSortie;
import edu.connection3a36.enums.Statut;
import edu.connection3a36.services.GroqService;
import edu.connection3a36.services.PlanActionsService;
import edu.connection3a36.services.ReferenceArticleService;
import edu.connection3a36.services.WikipediaService;
import edu.connection3a36.tools.AlertUtil;
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

    // ── Services ──────────────────────────────────────────────────────────────
    private final PlanActionsService      planService      = new PlanActionsService();
    private final ReferenceArticleService articleService   = new ReferenceArticleService();
    private final GroqService             groqService      = new GroqService();
    private final edu.connection3a36.services.VoiceRecorderService voiceService = new edu.connection3a36.services.VoiceRecorderService();
    private final WikipediaService        wikiService      = new WikipediaService();

    // ── État chatbot ──────────────────────────────────────────────────────────
    private final List<Map<String, String>> conversationHistory = new ArrayList<>();
    private String lastAIResponse = "";

    // Historique persistant
    private static final List<String[]> chatHistory = new ArrayList<>();
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("dd/MM HH:mm");

    @FXML
    public void initialize() {
        addAIMessage("👋 Bonjour ! Je suis votre assistant décisionnel IA.\n\n"
                + "Je suis à votre disposition pour analyser les données de l'établissement, détecter les risques, "
                + "proposer des plans d'actions stratégiques ou rédiger des articles institutionnels.\n\n"
                + "Que souhaitez-vous examiner aujourd'hui ?");
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

                chatHistory.add(new String[]{
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

    @FXML
    void handleWikipediaSearch() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Aide Notion (Wiki)");
        dialog.setHeaderText("Quelle notion stratégique ou technique souhaitez-vous expliquer ?");
        dialog.setContentText("Notion :");
        dialog.initOwner(chatBox.getScene().getWindow());

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(notion -> {
            if (notion.isEmpty()) return;
            
            lblChatStatus.setText("🔍 Recherche Wiki : " + notion + "...");
            new Thread(() -> {
                try {
                    String summary = wikiService.getSummary(notion);
                    Platform.runLater(() -> {
                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                        alert.initOwner(chatBox.getScene().getWindow());
                        alert.setTitle("Définition Wikipedia : " + notion);
                        alert.setHeaderText("Notion : " + notion);
                        
                        TextArea textArea = new TextArea(summary);
                        textArea.setEditable(false);
                        textArea.setWrapText(true);
                        textArea.setPrefHeight(200);
                        textArea.setPrefWidth(450);
                        textArea.setStyle("-fx-font-size: 13px;");
                        
                        alert.getDialogPane().setContent(textArea);
                        alert.show();
                        lblChatStatus.setText("✅ Définition affichée");
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> lblChatStatus.setText("❌ Erreur Wiki"));
                }
            }).start();
        });
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

                    String articleContent = groqService.sendSimpleMessage(prompt, "ADMIN");

                    ReferenceArticle article = new ReferenceArticle();
                    article.setTitre(titre.length() > 255 ? titre.substring(0, 255) : titre);
                    article.setContenu(articleContent);
                    
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
                      + "Fais une analyse éclair (3 paragraphes max) sur la situation managériale et les points forts.",
                        totalPlans, statuts, totalArticles);

                String response = groqService.sendSimpleMessage(prompt, "ADMIN");

                Platform.runLater(() -> {
                    boxAnalyse.getChildren().clear();
                    renderResponse(response, boxAnalyse);
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

        if (chatHistory.isEmpty()) {
            root.getChildren().add(new Label("Aucune conversation en mémoire.") {{
                setStyle("-fx-text-fill: #7a8fa5; -fx-font-style: italic;");
            }});
        } else {
            ScrollPane sp = new ScrollPane();
            sp.setFitToWidth(true);
            sp.setStyle("-fx-background-color: transparent;");

            VBox histList = new VBox(10);
            histList.setPadding(new Insets(5));

            List<String[]> reversed = new ArrayList<>(chatHistory);
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
        MarkdownRenderer.render(response, target);
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

    private void scrollToBottom() {
        Platform.runLater(() -> chatScroll.setVvalue(1.0));
    }
}
