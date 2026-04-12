package edu.connection3a36.controllers;

import edu.connection3a36.services.GroqService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Contrôleur IA Pédagogique (Enseignant) — chat avec l'API Groq.
 */
public class AIPedagogiqueController {

    @FXML private VBox chatBox;
    @FXML private ScrollPane chatScroll;
    @FXML private TextField inputField;
    @FXML private Button btnSend;
    @FXML private Label lblStatus;

    private final GroqService groqService = new GroqService();
    private final List<Map<String, String>> conversationHistory = new ArrayList<>();

    @FXML
    public void initialize() {
        addAIMessage("Bonjour ! 👋 Je suis MentorAI, votre assistant pédagogique.\n\n"
                + "Je peux vous aider à :\n"
                + "• 📊 Analyser les performances de vos étudiants\n"
                + "• ⚠️ Détecter les risques de décrochage\n"
                + "• 📋 Recommander des plans d'action\n"
                + "• 🎯 Proposer des stratégies pédagogiques\n\n"
                + "Comment puis-je vous aider ?");
    }

    @FXML
    void handleSend() {
        String message = inputField.getText().trim();
        if (message.isEmpty()) return;

        addUserMessage(message);
        inputField.clear();
        lblStatus.setText("⏳ MentorAI réfléchit...");
        btnSend.setDisable(true);

        // Appel API dans un thread séparé (ne pas bloquer l'UI)
        new Thread(() -> {
            try {
                String response = groqService.sendMessage(message, conversationHistory, "ENSEIGNANT");

                // Ajouter à l'historique
                Map<String, String> userMsg = new HashMap<>();
                userMsg.put("role", "user");
                userMsg.put("content", message);
                conversationHistory.add(userMsg);

                Map<String, String> aiMsg = new HashMap<>();
                aiMsg.put("role", "assistant");
                aiMsg.put("content", response);
                conversationHistory.add(aiMsg);

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

    @FXML void handleSuggestion1() {
        inputField.setText("Analyse les performances globales de mes étudiants en développement Java. Identifie les points faibles et propose des actions.");
        handleSend();
    }

    @FXML void handleSuggestion2() {
        inputField.setText("Détecte les étudiants à risque de décrochage dans mes classes. Quels signaux d'alerte dois-je surveiller ?");
        handleSend();
    }

    @FXML void handleSuggestion3() {
        inputField.setText("Recommande un plan d'action pédagogique pour améliorer la compréhension du module Machine Learning.");
        handleSend();
    }

    @FXML
    void handleClear() {
        chatBox.getChildren().clear();
        conversationHistory.clear();
        initialize(); // Réafficher le message de bienvenue
    }

    // ======================== UI HELPERS ========================

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
        VBox messageContainer = new VBox(5);
        messageContainer.getStyleClass().add("chat-message-ai");

        // Simple parser pour les blocs de code (``` json ... ```)
        String[] parts = message.split("```");
        for (int i = 0; i < parts.length; i++) {
            if (i % 2 == 0) {
                // Texte normal
                if (!parts[i].trim().isEmpty()) {
                    Label textLabel = new Label(parts[i].trim());
                    textLabel.setWrapText(true);
                    textLabel.setMaxWidth(500);
                    messageContainer.getChildren().add(textLabel);
                }
            } else {
                // Bloc de code (ex: JSON)
                String code = parts[i].trim();
                boolean isJson = false;
                org.json.JSONObject jsonObj = null;
                if (code.toLowerCase().startsWith("json")) {
                    code = code.substring(4).trim();
                    isJson = true;
                }
                
                if (isJson) {
                    try {
                        jsonObj = new org.json.JSONObject(code);
                    } catch (Exception ignored) {}
                }

                if (jsonObj != null) {
                    // C'est du JSON valide ! Au lieu d'afficher du code, on crée une belle interface utilisateur
                    VBox dashboardBox = new VBox(10);
                    dashboardBox.setStyle("-fx-background-color: white; -fx-padding: 15; -fx-background-radius: 8; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 5, 0, 0, 0);");
                    dashboardBox.setMaxWidth(550);

                    // 1. Métriques
                    if (jsonObj.has("metrics")) {
                        org.json.JSONArray metrics = jsonObj.optJSONArray("metrics");
                        if (metrics != null && metrics.length() > 0) {
                            javafx.scene.layout.HBox metricsBox = new javafx.scene.layout.HBox(10);
                            for (int j = 0; j < metrics.length(); j++) {
                                org.json.JSONObject m = metrics.getJSONObject(j);
                                VBox card = new VBox(5);
                                card.setStyle("-fx-background-color: #f1f2f6; -fx-padding: 10; -fx-background-radius: 5;");
                                String val = m.optString("value", "") + m.optString("unit", "");
                                String trend = m.optString("trend", "");
                                String color = trend.equals("up") ? "#27ae60" : (trend.equals("down") ? "#e74c3c" : "#7f8c8d");
                                
                                Label lblVal = new Label(val);
                                lblVal.setStyle("-fx-font-weight: bold; -fx-font-size: 18px; -fx-text-fill: " + color + ";");
                                Label lblTitle = new Label(m.optString("label", "Indicateur"));
                                lblTitle.setStyle("-fx-font-size: 11px; -fx-text-fill: #2f3542;");
                                lblTitle.setWrapText(true);
                                lblTitle.setMaxWidth(100);
                                card.getChildren().addAll(lblVal, lblTitle);
                                metricsBox.getChildren().add(card);
                            }
                            dashboardBox.getChildren().add(metricsBox);
                        }
                    }

                    // 2. Alertes
                    if (jsonObj.has("alerts")) {
                        org.json.JSONArray alerts = jsonObj.optJSONArray("alerts");
                        if (alerts != null && alerts.length() > 0) {
                            for (int j = 0; j < alerts.length(); j++) {
                                org.json.JSONObject a = alerts.getJSONObject(j);
                                String level = a.optString("level", "medium");
                                String color = level.equals("high") ? "#ffeaa7" : (level.equals("low") ? "#dff9fb" : "#fab1a0");
                                String textCol = level.equals("high") ? "#d35400" : "#2d3436";
                                Label lblAlert = new Label("⚠️ " + a.optString("message", "Alerte"));
                                lblAlert.setWrapText(true);
                                lblAlert.setStyle("-fx-background-color: " + color + "; -fx-text-fill: " + textCol + "; -fx-padding: 8; -fx-background-radius: 5; -fx-font-weight: bold;");
                                lblAlert.setMaxWidth(500);
                                dashboardBox.getChildren().add(lblAlert);
                            }
                        }
                    }

                    // 3. Prédictions & Décisions
                    if (jsonObj.has("decisions") || jsonObj.has("predictions")) {
                        Label lblRec = new Label("💡 Recommandations IA :");
                        lblRec.setStyle("-fx-font-weight: bold; -fx-text-fill: #2980b9; -fx-padding: 10 0 0 0;");
                        dashboardBox.getChildren().add(lblRec);
                        
                        if (jsonObj.has("decisions")) {
                            org.json.JSONArray decs = jsonObj.optJSONArray("decisions");
                            if (decs != null) {
                                for(int j=0; j<decs.length(); j++) {
                                    Label d = new Label("✅ " + decs.getJSONObject(j).optString("action", ""));
                                    d.setWrapText(true);
                                    d.setMaxWidth(500);
                                    dashboardBox.getChildren().add(d);
                                }
                            }
                        }
                    }
                    messageContainer.getChildren().add(dashboardBox);
                } else {
                    // Si ce n'est pas un JSON valide et qu'on ne peut rien faire, on le met proprement sans bloc noir
                    Label codeLabel = new Label(code);
                    codeLabel.setWrapText(true);
                    codeLabel.setMaxWidth(500);
                    codeLabel.setStyle("-fx-background-color: #ecf0f1; -fx-text-fill: #2c3e50; -fx-padding: 10; -fx-background-radius: 8;");
                    messageContainer.getChildren().add(codeLabel);
                }
            }
        }

        HBox hbox = new HBox(messageContainer);
        hbox.setAlignment(Pos.CENTER_LEFT);
        hbox.setPadding(new Insets(5, 50, 5, 0));

        chatBox.getChildren().add(hbox);
        scrollToBottom();
    }

    private void scrollToBottom() {
        Platform.runLater(() -> chatScroll.setVvalue(1.0));
    }
}
