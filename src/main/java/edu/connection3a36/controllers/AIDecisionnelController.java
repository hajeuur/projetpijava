package edu.connection3a36.controllers;

import edu.connection3a36.services.CategorieArticleService;
import edu.connection3a36.services.GroqService;
import edu.connection3a36.services.PlanActionsService;
import edu.connection3a36.services.ReferenceArticleService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.scene.control.Label;

import java.sql.SQLException;
import java.util.Map;

/**
 * Contrôleur IA Décisionnelle (Admin) — dashboard avec statistiques et graphiques.
 */
public class AIDecisionnelController {

    @FXML private Label lblTotalPlans;
    @FXML private Label lblTotalArticles;
    @FXML private Label lblTotalCategories;
    @FXML private Label lblRecentArticles;
    @FXML private PieChart pieStatut;
    @FXML private BarChart<String, Number> barCategorie;
    @FXML private javafx.scene.layout.VBox boxAnalyse;

    private final PlanActionsService planService = new PlanActionsService();
    private final ReferenceArticleService articleService = new ReferenceArticleService();
    private final CategorieArticleService categorieService = new CategorieArticleService();
    private final GroqService groqService = new GroqService();

    @FXML
    public void initialize() {
        loadStats();
        loadCharts();
    }

    private void loadStats() {
        try {
            lblTotalPlans.setText(String.valueOf(planService.countAll()));
            lblTotalArticles.setText(String.valueOf(articleService.countPublished()));
            lblTotalCategories.setText(String.valueOf(categorieService.countAll()));
            lblRecentArticles.setText(String.valueOf(articleService.countRecentArticles(7)));
        } catch (SQLException e) {
            System.err.println("Erreur stats: " + e.getMessage());
        }
    }

    private void loadCharts() {
        // PieChart — Plans par statut
        try {
            Map<String, Integer> statuts = planService.countByStatut();
            pieStatut.setData(FXCollections.observableArrayList());
            for (Map.Entry<String, Integer> entry : statuts.entrySet()) {
                pieStatut.getData().add(new PieChart.Data(
                        entry.getKey() + " (" + entry.getValue() + ")",
                        entry.getValue()));
            }
            pieStatut.setLabelsVisible(true);
        } catch (SQLException e) {
            System.err.println("Erreur PieChart: " + e.getMessage());
        }

        // BarChart — Plans par catégorie
        try {
            Map<String, Integer> categories = planService.countByCategorie();
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("Plans d'actions");
            for (Map.Entry<String, Integer> entry : categories.entrySet()) {
                String label = entry.getKey() != null ? entry.getKey() : "Non classé";
                series.getData().add(new XYChart.Data<>(label, entry.getValue()));
            }
            barCategorie.getData().clear();
            barCategorie.getData().add(series);
            barCategorie.setLegendVisible(false);
        } catch (SQLException e) {
            System.err.println("Erreur BarChart: " + e.getMessage());
        }
    }

    @FXML
    void handleAnalyse() {
        boxAnalyse.getChildren().clear();
        boxAnalyse.getChildren().add(new Label("⏳ Analyse en cours..."));

        new Thread(() -> {
            try {
                // Construire le contexte statistique pour l'IA
                int totalPlans = planService.countAll();
                Map<String, Integer> statuts = planService.countByStatut();
                Map<String, Integer> categories = planService.countByCategorie();
                int totalArticles = articleService.countAll();
                int publishedArticles = articleService.countPublished();

                String prompt = String.format(
                        "En tant qu'administrateur de l'école ESPRIT, voici les statistiques actuelles :\n"
                      + "- Plans d'actions total : %d\n"
                      + "- Répartition par statut : %s\n"
                      + "- Répartition par catégorie : %s\n"
                      + "- Articles total : %d (publiés : %d)\n\n"
                      + "Analyse ces données et donne-moi :\n"
                      + "1. Un résumé de la situation\n"
                      + "2. Les alertes ou risques détectés\n"
                      + "3. Des recommandations stratégiques concrètes",
                        totalPlans, statuts.toString(), categories.toString(), totalArticles, publishedArticles
                );

                String response = groqService.sendSimpleMessage(prompt, "ADMIN");

                Platform.runLater(() -> {
                    boxAnalyse.getChildren().clear();
                    String[] parts = response.split("```");
                    for (int i = 0; i < parts.length; i++) {
                        if (i % 2 == 0) {
                            if (!parts[i].trim().isEmpty()) {
                                Label textLabel = new Label(parts[i].trim());
                                textLabel.setWrapText(true);
                                boxAnalyse.getChildren().add(textLabel);
                            }
                        } else {
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
                                javafx.scene.layout.VBox dashboardBox = new javafx.scene.layout.VBox(10);
                                dashboardBox.setStyle("-fx-background-color: white; -fx-padding: 15; -fx-background-radius: 8; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 5, 0, 0, 0);");
                                dashboardBox.setMaxWidth(600);

                                // 1. Métriques
                                if (jsonObj.has("metrics")) {
                                    org.json.JSONArray metrics = jsonObj.optJSONArray("metrics");
                                    if (metrics != null && metrics.length() > 0) {
                                        javafx.scene.layout.HBox metricsBox = new javafx.scene.layout.HBox(10);
                                        for (int j = 0; j < metrics.length(); j++) {
                                            org.json.JSONObject m = metrics.getJSONObject(j);
                                            javafx.scene.layout.VBox card = new javafx.scene.layout.VBox(5);
                                            card.setStyle("-fx-background-color: #f1f2f6; -fx-padding: 10; -fx-background-radius: 5;");
                                            String val = m.optString("value", "") + m.optString("unit", "");
                                            String trend = m.optString("trend", "");
                                            String color = trend.equals("up") ? "#27ae60" : (trend.equals("down") ? "#e74c3c" : "#7f8c8d");
                                            
                                            Label lblVal = new Label(val);
                                            lblVal.setStyle("-fx-font-weight: bold; -fx-font-size: 18px; -fx-text-fill: " + color + ";");
                                            Label lblTitle = new Label(m.optString("label", "Indicateur"));
                                            lblTitle.setStyle("-fx-font-size: 11px; -fx-text-fill: #2f3542;");
                                            lblTitle.setWrapText(true);
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
                                            dashboardBox.getChildren().add(lblAlert);
                                        }
                                    }
                                }

                                // 3. Prédictions & Décisions
                                if (jsonObj.has("decisions") || jsonObj.has("predictions")) {
                                    Label lblRec = new Label("💡 Recommandations Stratégiques :");
                                    lblRec.setStyle("-fx-font-weight: bold; -fx-text-fill: #2980b9; -fx-padding: 10 0 0 0;");
                                    dashboardBox.getChildren().add(lblRec);
                                    
                                    if (jsonObj.has("decisions")) {
                                        org.json.JSONArray decs = jsonObj.optJSONArray("decisions");
                                        if (decs != null) {
                                            for(int j=0; j<decs.length(); j++) {
                                                Label d = new Label("✅ " + decs.getJSONObject(j).optString("action", ""));
                                                d.setWrapText(true);
                                                dashboardBox.getChildren().add(d);
                                            }
                                        }
                                    }
                                }
                                boxAnalyse.getChildren().add(dashboardBox);
                            } else {
                                Label codeLabel = new Label(code);
                                codeLabel.setWrapText(true);
                                codeLabel.setStyle("-fx-background-color: #ecf0f1; -fx-text-fill: #2c3e50; -fx-padding: 10; -fx-background-radius: 8;");
                                boxAnalyse.getChildren().add(codeLabel);
                            }
                        }
                    }
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    boxAnalyse.getChildren().clear();
                    boxAnalyse.getChildren().add(new Label("❌ Erreur analyse : " + e.getMessage()));
                });
            }
        }).start();
    }

    @FXML
    void handleRefresh() {
        loadStats();
        loadCharts();
        boxAnalyse.getChildren().clear();
    }
}
