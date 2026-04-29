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
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Contrôleur IA Pédagogique (Enseignant) — chatbot pédagogique avec :
 *  - Chat multi-tour avec l'API Groq
 *  - Historique de session
 *  - Créer un Plan d'Action depuis la réponse IA
 *  - Recommandation d'articles liés
 *  - Génération automatique d'articles
 */
public class AIPedagogiqueController {

    @FXML private VBox       chatBox;
    @FXML private ScrollPane chatScroll;
    @FXML private TextField  inputField;
    @FXML private Button     btnSend;
    @FXML private Button     btnRecord;
    @FXML private Label      lblStatus;

    private final edu.connection3a36.services.VoiceRecorderService voiceService = new edu.connection3a36.services.VoiceRecorderService();
    private final GroqService            groqService   = new GroqService();
    private final PlanActionsService     planService   = new PlanActionsService();
    private final ReferenceArticleService articleService = new ReferenceArticleService();
    private final WikipediaService       wikiService    = new WikipediaService();

    private final List<Map<String, String>> conversationHistory = new ArrayList<>();
    private String lastAIResponse = "";

    // Historique persistant de session (partagé entre instances)
    private static final List<String[]> chatHistory = new ArrayList<>();

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("dd/MM HH:mm");

    @FXML
    public void initialize() {
        addAIMessage("Bonjour ! 👋 Je suis MentorAI, votre assistant pédagogique.\n\n"
                + "Je peux vous aider à :\n"
                + "• 📊 Analyser les performances de vos étudiants\n"
                + "• ⚠️ Détecter les risques de décrochage\n"
                + "• 📋 Créer des plans d'action pédagogiques\n"
                + "• 📰 Recommander des articles pédagogiques\n"
                + "• ✍️ Générer des articles automatiquement\n\n"
                + "Comment puis-je vous aider ?");
    }

    /**
     * Recherche Wikipedia pour un mot spécifique (via Shift+Clic).
     */
    private void lookupWikipedia(String word) {
        lblStatus.setText("🔍 Recherche rapide : " + word + "...");
        new Thread(() -> {
            try {
                String summary = wikiService.getSummary(word);
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.initOwner(chatBox.getScene().getWindow());
                    alert.setTitle("Définition Wikipedia : " + word);
                    alert.setHeaderText("Notion : " + word);
                    
                    // Contenu défilable si le texte est long
                    TextArea textArea = new TextArea(summary);
                    textArea.setEditable(false);
                    textArea.setWrapText(true);
                    textArea.setPrefHeight(200);
                    textArea.setPrefWidth(450);
                    textArea.setStyle("-fx-font-size: 13px;");
                    
                    alert.getDialogPane().setContent(textArea);
                    alert.show();
                    lblStatus.setText("✅ Définition affichée");
                });
            } catch (Exception e) {
                Platform.runLater(() -> lblStatus.setText("❌ Erreur Wiki : " + e.getMessage()));
            }
        }).start();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ENVOI DE MESSAGE
    // ─────────────────────────────────────────────────────────────────────────

    @FXML
    void handleSend() {
        String message = inputField.getText().trim();
        if (message.isEmpty()) return;

        addUserMessage(message);
        inputField.clear();
        lblStatus.setText("⏳ MentorAI réfléchit...");
        btnSend.setDisable(true);

        final String finalMessage = message;
        new Thread(() -> {
            try {
                String response = groqService.sendMessage(finalMessage, conversationHistory, "ENSEIGNANT");

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
                    lblStatus.setText("✅ Réponse reçue");
                    btnSend.setDisable(false);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    addAIMessage("❌ Erreur : " + e.getMessage());
                    lblStatus.setText("❌ Erreur de communication");
                    btnSend.setDisable(false);
                });
            }
        }).start();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SUGGESTIONS
    // ─────────────────────────────────────────────────────────────────────────

    @FXML void handleSuggestion1() {
        inputField.setText("Analyse les performances globales de mes étudiants en développement Java. "
                + "Identifie les points faibles et propose des actions pédagogiques concrètes.");
        handleSend();
    }

    @FXML void handleSuggestion2() {
        inputField.setText("Quels sont les signaux d'alerte de décrochage scolaire que je dois surveiller ? "
                + "Comment détecter les étudiants à risque et intervenir efficacement ?");
        handleSend();
    }

    @FXML void handleSuggestion3() {
        inputField.setText("Recommande-moi un plan d'action pédagogique complet pour améliorer "
                + "la compréhension du module Machine Learning en 3ème année.");
        handleSend();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CRÉER UN PLAN D'ACTION DEPUIS L'IA
    // ─────────────────────────────────────────────────────────────────────────

    @FXML
    void handleCreatePlan() {
        if (lastAIResponse.isEmpty()) {
            AlertUtil.showError("Aucune réponse IA disponible. Commencez d'abord une conversation.");
            return;
        }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Créer un Plan d'Action depuis l'IA");
        dialog.setHeaderText("Convertir la recommandation IA en Plan d'Action pédagogique");

        VBox content = new VBox(12);
        content.setPadding(new Insets(15));
        content.setPrefWidth(440);

        TextField tfDecision = new TextField();
        String firstLine = lastAIResponse.split("\n")[0].replaceAll("[•*#-]", "").trim();
        tfDecision.setText("Plan : " + (firstLine.length() > 100 ? firstLine.substring(0, 100) : firstLine));
        tfDecision.setPromptText("Décision / Titre du plan...");

        TextArea taDesc = new TextArea(lastAIResponse.length() > 600
                ? lastAIResponse.substring(0, 600) : lastAIResponse);
        taDesc.setWrapText(true);
        taDesc.setPrefRowCount(5);
        taDesc.setEditable(true);

        content.getChildren().addAll(
                new Label("Décision :"), tfDecision,
                new Label("Description (modifiable) :"), taDesc
        );
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        ((Button) dialog.getDialogPane().lookupButton(ButtonType.OK)).setText("✅ Créer");

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            String decision = tfDecision.getText().trim();
            String description = taDesc.getText().trim();
            if (decision.isEmpty() || description.length() < 10) {
                AlertUtil.showError("Décision et description requises (description min 10 car.).");
                return;
            }
            try {
                PlanActions plan = new PlanActions();
                plan.setDecision(decision.length() > 200 ? decision.substring(0, 200) : decision);
                plan.setDescription(description.length() > 1000 ? description.substring(0, 1000) : description);
                plan.setStatut(Statut.EN_ATTENTE);
                plan.setCategorie(CategorieSortie.PEDAGOGIQUE);
                plan.setAuteurId(SessionManager.getCurrentUser().getId());
                plan.setDate(LocalDateTime.now());
                planService.addEntity(plan);
                AlertUtil.showSuccess("✅ Plan d'Action pédagogique créé ! (ID: " + plan.getId() + ")");
            } catch (Exception e) {
                AlertUtil.showError("Erreur création plan : " + e.getMessage());
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // RECOMMANDER DES ARTICLES
    // ─────────────────────────────────────────────────────────────────────────

    @FXML
    void handleRecommendArticles() {
        try {
            List<edu.connection3a36.entities.ReferenceArticle> articles = articleService.getData();
            if (articles.isEmpty()) {
                AlertUtil.showError("Aucun article disponible en base de données.");
                return;
            }
            // Afficher les 5 premiers articles comme recommandations
            StringBuilder sb = new StringBuilder("📚 Articles recommandés pour vous :\n\n");
            int count = Math.min(5, articles.size());
            for (int i = 0; i < count; i++) {
                var a = articles.get(i);
                if (a.isPublished()) {
                    sb.append("• ").append(a.getTitre());
                    if (a.getCategorieNom() != null) sb.append(" [").append(a.getCategorieNom()).append("]");
                    sb.append("\n");
                }
            }
            addAIMessage(sb.toString());
        } catch (SQLException e) {
            AlertUtil.showError("Erreur chargement articles : " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // WIKIPEDIA SEARCH
    // ─────────────────────────────────────────────────────────────────────────

    @FXML
    void handleWikipediaSearch() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("🔍 Expliquer une Notion");
        dialog.setHeaderText("Quelle notion difficile souhaitez-vous expliquer ?");
        dialog.setContentText("Notion :");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(notion -> {
            if (notion.trim().isEmpty()) return;

            lblStatus.setText("🔍 Recherche Wikipedia...");
            btnSend.setDisable(true);

            new Thread(() -> {
                try {
                    String summary = wikiService.getSummary(notion);
                    Platform.runLater(() -> {
                        addAIMessage("📚 **Wikipedia : " + notion + "**\n\n" + summary);
                        lblStatus.setText("✅ Recherche terminée");
                        btnSend.setDisable(false);
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        addAIMessage("❌ Erreur Wikipedia : " + e.getMessage());
                        lblStatus.setText("❌ Erreur");
                        btnSend.setDisable(false);
                    });
                }
            }).start();
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GÉNÉRER UN ARTICLE IA
    // ─────────────────────────────────────────────────────────────────────────

    @FXML
    void handleGenerateArticle() {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("✍️ Générer un Article Automatiquement");
        dialog.setHeaderText("L'IA va rédiger un article pédagogique basé sur la conversation");

        VBox content = new VBox(10);
        content.setPadding(new Insets(15));
        content.setPrefWidth(400);

        TextField tfTitre = new TextField();
        if (!lastAIResponse.isEmpty()) {
            String auto = lastAIResponse.split("\n")[0].replaceAll("[•*#-]", "").trim();
            tfTitre.setText(auto.length() > 60 ? auto.substring(0, 60) : auto);
        }
        tfTitre.setPromptText("Thème / Titre de l'article à générer...");

        content.getChildren().addAll(new Label("Titre ou thème :"), tfTitre,
                new Label("L'IA rédigera un article complet et structuré."));
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        ((Button) dialog.getDialogPane().lookupButton(ButtonType.OK)).setText("✍️ Générer");

        dialog.setResultConverter(bt -> bt == ButtonType.OK ? tfTitre.getText().trim() : null);

        Optional<String> result = dialog.showAndWait();
        result.filter(t -> !t.isEmpty()).ifPresent(titre -> {
            lblStatus.setText("⏳ Génération de l'article...");
            btnSend.setDisable(true);

            new Thread(() -> {
                try {
                    String prompt = "Rédige un article pédagogique complet sur : \"" + titre + "\"\n"
                            + "Structure : Introduction | Corps (3-4 sections) | Conclusion & Recommandations. "
                            + "Style professionnel et accessible pour les enseignants de l'école ESPRIT.";

                    String content2 = groqService.sendSimpleMessage(prompt, "ENSEIGNANT");

                    ReferenceArticle article = new ReferenceArticle();
                    article.setTitre(titre.length() > 255 ? titre.substring(0, 255) : titre);
                    article.setContenu(content2);
                    
                    int fallbackCatId = 1;
                    try {
                        edu.connection3a36.services.CategorieArticleService catServ = new edu.connection3a36.services.CategorieArticleService();
                        var cats = catServ.getData();
                        if (!cats.isEmpty()) { fallbackCatId = cats.get(0).getId(); }
                    } catch (Exception ignored) {}

                    article.setAuteurId(SessionManager.getCurrentUser().getId());
                    article.setPublished(false);
                    articleService.addEntity(article);

                    Platform.runLater(() -> {
                        addAIMessage("✅ Article \"" + titre + "\" généré et sauvegardé en brouillon !\n"
                                + "Retrouvez-le dans la section Articles pour le relire et le publier.");
                        lblStatus.setText("✅ Article généré");
                        btnSend.setDisable(false);
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        AlertUtil.showError("Erreur génération : " + e.getMessage());
                        lblStatus.setText("❌ Erreur");
                        btnSend.setDisable(false);
                    });
                }
            }).start();
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HISTORIQUE
    // ─────────────────────────────────────────────────────────────────────────

    @FXML
    void handleShowHistory() {
        Stage histStage = new Stage();
        histStage.setTitle("🕐 Historique — IA Pédagogique");
        histStage.initModality(Modality.APPLICATION_MODAL);

        VBox root = new VBox(12);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: #f4f7fa;");

        Label title = new Label("🕐 Historique Chatbot Pédagogique");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #102c59;");
        root.getChildren().add(title);

        if (chatHistory.isEmpty()) {
            root.getChildren().add(new Label("Aucune conversation enregistrée pour cette session.") {{
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

                Label dateLabel = new Label("🕐 " + entry[0]);
                dateLabel.getStyleClass().add("history-item-date");

                Label userLabel = new Label("👤 " + entry[1]);
                userLabel.getStyleClass().add("history-item-user");
                userLabel.setWrapText(true);

                String preview = entry[2].length() > 200 ? entry[2].substring(0, 200) + "..." : entry[2];
                Label aiLabel = new Label("🤖 " + preview);
                aiLabel.getStyleClass().add("history-item-ai");

                Button btnReload = new Button("🔄 Reposer la question");
                btnReload.getStyleClass().add("btn-secondary");
                btnReload.setStyle("-fx-font-size: 11px; -fx-padding: 4 10;");
                final String question = entry[1];
                btnReload.setOnAction(e -> {
                    inputField.setText(question);
                    histStage.close();
                });

                item.getChildren().addAll(dateLabel, userLabel, aiLabel, btnReload);
                histList.getChildren().add(item);
            }

            sp.setContent(histList);
            sp.setPrefHeight(420);
            root.getChildren().add(sp);
        }

        Button btnClose = new Button("Fermer");
        btnClose.getStyleClass().add("btn-secondary");
        btnClose.setOnAction(e -> histStage.close());
        root.getChildren().add(btnClose);

        Scene scene = new Scene(root, 600, 520);
        scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
        histStage.setScene(scene);
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
                        inputField.setPromptText("✏️ Posez une question pédagogique...");
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        AlertUtil.showError("Erreur dictée vocale : " + e.getMessage());
                        inputField.setPromptText("✏️ Posez une question pédagogique...");
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

    // ─────────────────────────────────────────────────────────────────────────
    // CLEAR
    // ─────────────────────────────────────────────────────────────────────────

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
        msgLabel.setMaxWidth(400);
        msgLabel.getStyleClass().add("chat-message-user");

        HBox hbox = new HBox(msgLabel);
        hbox.setAlignment(Pos.CENTER_RIGHT);
        hbox.setPadding(new Insets(5, 0, 5, 50));
        chatBox.getChildren().add(hbox);
        scrollToBottom();
    }

    private void addAIMessage(String message) {
        VBox container = new VBox(5);
        container.getStyleClass().add("chat-message-ai");
        container.setMaxWidth(500);

        MarkdownRenderer.render(message, container);

        HBox hbox = new HBox(container);
        hbox.setAlignment(Pos.CENTER_LEFT);
        hbox.setPadding(new Insets(5, 50, 5, 0));
        chatBox.getChildren().add(hbox);
        scrollToBottom();
    }

    private void renderJsonCard(org.json.JSONObject obj, Pane target) {
        VBox dash = new VBox(8);
        dash.setStyle("-fx-background-color: white; -fx-padding: 12; -fx-background-radius: 10;"
                + "-fx-effect: dropshadow(three-pass-box, rgba(16,44,89,0.1), 5, 0, 0, 0);");
        dash.setMaxWidth(490);

        if (obj.has("metrics")) {
            org.json.JSONArray metrics = obj.optJSONArray("metrics");
            if (metrics != null) {
                HBox box = new HBox(8);
                for (int j = 0; j < metrics.length(); j++) {
                    org.json.JSONObject m = metrics.getJSONObject(j);
                    VBox card = new VBox(3);
                    card.setStyle("-fx-background-color: #eef4f9; -fx-padding: 8; -fx-background-radius: 8;");
                    String t = m.optString("trend", "");
                    String c = t.equals("up") ? "#27ae60" : t.equals("down") ? "#d52e28" : "#7a8fa5";
                    Label v = new Label(m.optString("value", "") + m.optString("unit", ""));
                    v.setStyle("-fx-font-weight: bold; -fx-font-size: 15px; -fx-text-fill: " + c + ";");
                    Label l = new Label(m.optString("label", ""));
                    l.setStyle("-fx-font-size: 10px; -fx-text-fill: #102c59;");
                    l.setWrapText(true); l.setMaxWidth(85);
                    card.getChildren().addAll(v, l);
                    box.getChildren().add(card);
                }
                dash.getChildren().add(box);
            }
        }
        if (obj.has("alerts")) {
            org.json.JSONArray alerts = obj.optJSONArray("alerts");
            if (alerts != null) {
                for (int j = 0; j < alerts.length(); j++) {
                    org.json.JSONObject a = alerts.getJSONObject(j);
                    String level = a.optString("level", "medium");
                    String bg = level.equals("high") ? "#fddcdb" : level.equals("low") ? "#d4edda" : "#fef3cd";
                    Label al = new Label("⚠️ " + a.optString("message", ""));
                    al.setWrapText(true);
                    al.setStyle("-fx-background-color:" + bg + "; -fx-text-fill: #102c59;"
                            + "-fx-padding: 8; -fx-background-radius: 6; -fx-font-weight: bold; -fx-font-size: 11px;");
                    dash.getChildren().add(al);
                }
            }
        }
        if (obj.has("decisions")) {
            Label title = new Label("💡 Actions recommandées :");
            title.setStyle("-fx-font-weight: bold; -fx-text-fill: #102c59; -fx-font-size: 12px;");
            dash.getChildren().add(title);
            org.json.JSONArray decs = obj.optJSONArray("decisions");
            if (decs != null) {
                for (int j = 0; j < decs.length(); j++) {
                    Label d = new Label("✅ " + decs.getJSONObject(j).optString("action", ""));
                    d.setWrapText(true);
                    d.setStyle("-fx-font-size: 11px; -fx-text-fill: #27ae60;");
                    dash.getChildren().add(d);
                }
            }
        }
        target.getChildren().add(dash);
    }

    private void scrollToBottom() {
        Platform.runLater(() -> chatScroll.setVvalue(1.0));
    }
}
