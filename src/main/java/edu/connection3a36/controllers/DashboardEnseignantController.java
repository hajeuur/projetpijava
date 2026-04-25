package edu.connection3a36.controllers;

import edu.connection3a36.services.PlanActionsService;
import edu.connection3a36.services.ReferenceArticleService;
import edu.connection3a36.tools.SessionManager;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.util.Map;
import java.util.HashMap;

public class DashboardEnseignantController {

    @FXML private Label lblPlans;
    @FXML private Label lblArticles;

    @FXML private TableView<Map<String, String>> tableRisk;
    @FXML private TableColumn<Map<String, String>, String> colNomRisk;
    @FXML private TableColumn<Map<String, String>, String> colScoreRisk;
    @FXML private TableColumn<Map<String, String>, String> colMatiereRisk;

    @FXML private TableView<edu.connection3a36.entities.PlanActions> tableFeedbacks;
    @FXML private TableColumn<edu.connection3a36.entities.PlanActions, String> colFeedbackPlan;
    @FXML private TableColumn<edu.connection3a36.entities.PlanActions, String> colFeedbackTexte;

    private final PlanActionsService planService = new PlanActionsService();
    private final ReferenceArticleService articleService = new ReferenceArticleService();

    @FXML
    public void initialize() {
        try {
            int userId = SessionManager.getCurrentUser().getId();
            
            int myPlansCount = (int) planService.getData().stream().filter(p -> p.getAuteurId() == userId).count();
            int myArticlesCount = (int) articleService.getData().stream().filter(a -> a.getAuteurId() == userId).count();

            lblPlans.setText(String.valueOf(myPlansCount));
            lblArticles.setText(String.valueOf(myArticlesCount));

            // ==== Mock data: Étudiants à risque ====
            colNomRisk.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().get("nom")));
            colScoreRisk.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().get("score")));
            colMatiereRisk.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().get("matiere")));
            tableRisk.getItems().addAll(
                Map.of("nom", "Ahmed Salah", "score", "45%", "matiere", "Java Avancé"),
                Map.of("nom", "Mohamed Slim", "score", "38%", "matiere", "SGBD"),
                Map.of("nom", "Eya Tlili", "score", "51%", "matiere", "Réseaux") // Pour l'anecdote
            );

            // ==== Vrais Feedbacks Enseignants ====
            colFeedbackPlan.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getDecision()));
            colFeedbackTexte.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getFeedbackEnseignant()));

            java.util.List<edu.connection3a36.entities.PlanActions> feedbacks = planService.getRecentUserFeedbacks(userId);
            tableFeedbacks.getItems().clear();
            tableFeedbacks.getItems().addAll(feedbacks);

        } catch (Exception e) {
            System.err.println("Erreur chargement dashboard enseignant: " + e.getMessage());
        }
    }
}
