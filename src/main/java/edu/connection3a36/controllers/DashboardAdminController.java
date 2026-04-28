package edu.connection3a36.controllers;

import edu.connection3a36.services.CategorieArticleService;
import edu.connection3a36.services.PlanActionsService;
import edu.connection3a36.services.ReferenceArticleService;
import edu.connection3a36.services.UserPreferencesService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import java.time.format.DateTimeFormatter;
import java.util.List;
import edu.connection3a36.entities.PlanActions;

import java.sql.SQLException;
import java.util.Map;

/**
 * Contrôleur Dashboard Admin — KPI et graphiques séparés du Chatbot.
 */
public class DashboardAdminController {

    // ── Stats ─────────────────────────────────────────────────────────────────
    @FXML private Label lblTotalPlans;
    @FXML private Label lblTotalArticles;
    @FXML private Label lblTotalCategories;
    @FXML private Label lblRecentArticles;
    @FXML private Label lblFeedbackCoverage;

    @FXML private PieChart pieStatut;
    @FXML private BarChart<String, Number> barCategorie;
    @FXML private VBox vboxRecentFeedbacks;

    // ── Services ──────────────────────────────────────────────────────────────
    private final PlanActionsService      planService      = new PlanActionsService();
    private final ReferenceArticleService articleService   = new ReferenceArticleService();
    private final CategorieArticleService categorieService = new CategorieArticleService();
    private final UserPreferencesService  prefsService     = new UserPreferencesService();

    @FXML
    public void initialize() {
        loadStats();
        loadCharts();
        loadRecentFeedbacks();
    }

    @FXML
    void handleRefresh() {
        loadStats();
        loadCharts();
        loadRecentFeedbacks();
    }

    private void loadStats() {
        try {
            lblTotalPlans.setText(String.valueOf(planService.countAll()));
            lblTotalArticles.setText(String.valueOf(articleService.countPublished()));
            lblTotalCategories.setText(String.valueOf(categorieService.countAll()));
            lblRecentArticles.setText(String.valueOf(articleService.countRecentArticles(7)));
            int totalPlans = planService.countAll();
            int withFeedback = planService.countWithFeedback();
            int coverage = totalPlans == 0 ? 0 : (withFeedback * 100 / totalPlans);
            if (lblFeedbackCoverage != null) lblFeedbackCoverage.setText(coverage + "%");
        } catch (SQLException e) {
            System.err.println("Erreur stats: " + e.getMessage());
        }
    }

    private void loadCharts() {
        try {
            Map<String, Integer> statuts = planService.countByStatut();
            pieStatut.setData(FXCollections.observableArrayList());
            for (Map.Entry<String, Integer> e : statuts.entrySet()) {
                pieStatut.getData().add(new PieChart.Data(e.getKey() + " (" + e.getValue() + ")", e.getValue()));
            }
            pieStatut.setLabelsVisible(true);
        } catch (SQLException e) {
            System.err.println("Erreur PieChart: " + e.getMessage());
        }
        
        try {
            Map<String, Integer> categories = planService.countByCategorie();
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("Plans d'actions");
            for (Map.Entry<String, Integer> e : categories.entrySet()) {
                series.getData().add(new XYChart.Data<>(e.getKey() != null ? e.getKey() : "Non classé", e.getValue()));
            }
            barCategorie.getData().clear();
            barCategorie.getData().add(series);
            barCategorie.setLegendVisible(false);
        } catch (SQLException e) {
            System.err.println("Erreur BarChart: " + e.getMessage());
        }
    }

    private void loadRecentFeedbacks() {
        if (vboxRecentFeedbacks == null) return;
        vboxRecentFeedbacks.getChildren().clear();
        try {
            List<PlanActions> feedbacks = planService.getRecentFeedbacks();
            boolean darkMode = prefsService.load().darkMode;
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            for (PlanActions p : feedbacks) {
                VBox box = new VBox(4);
                box.setStyle("-fx-padding: 8; -fx-background-color: " + (darkMode ? "#1f2937" : "#f8f9fa")
                        + "; -fx-border-color: " + (darkMode ? "#30363d" : "#ecf0f1") + "; -fx-border-radius: 6; -fx-background-radius: 6;");

                Label lblHeader = new Label("Plan #" + p.getId() + " - " + p.getDecision());
                lblHeader.setStyle("-fx-font-weight: bold; -fx-text-fill: " + (darkMode ? "#e6edf3" : "#102c59") + ";");

                String fbDate = p.getFeedbackDate() != null ? dtf.format(p.getFeedbackDate()) : "";
                Label lblSub = new Label("Posté le " + fbDate);
                lblSub.setStyle("-fx-font-size: 11px; -fx-text-fill: " + (darkMode ? "#8b949e" : "#7a8fa5") + ";");

                Label lblDesc = new Label(p.getFeedbackEnseignant());
                lblDesc.setStyle("-fx-font-size: 12px; -fx-text-fill: " + (darkMode ? "#c9d1d9" : "#102c59") + ";");
                lblDesc.setWrapText(true);

                box.getChildren().addAll(lblHeader, lblSub, lblDesc);
                vboxRecentFeedbacks.getChildren().add(box);
            }
            if (feedbacks.isEmpty()) {
                vboxRecentFeedbacks.getChildren().add(new Label("Aucun feedback récent."));
            }
        } catch (Exception e) {
            System.err.println("Erreur load feedbacks: " + e.getMessage());
        }
    }
}
