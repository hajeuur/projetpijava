package edu.connection3a36.Controller;

import edu.connection3a36.entities.Projet;
import edu.connection3a36.services.CareerService;
import edu.connection3a36.services.ProjetService;
import edu.connection3a36.tools.SessionManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Priority;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class CareerDashboardController implements Initializable {

    @FXML private Label lblTopJob, lblTopScore, lblSalary, lblDemand;
    @FXML private VBox vboxPredictions, paneLoading;

    private final CareerService careerService = new CareerService();
    private final ProjetService projetService = new ProjetService();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        refreshAnalysis();
    }

    @FXML
    private void refreshAnalysis() {
        paneLoading.setVisible(true);
        vboxPredictions.getChildren().clear();

        new Thread(() -> {
            try {
                // 1. Récupérer toutes les technologies des projets de l'utilisateur
                // Pour la démo, on prend tous les projets disponibles ou ceux de la session
                List<Projet> projets = projetService.getData(); 
                String allSkills = projets.stream()
                        .map(p -> p.getTechnologies() + " " + p.getTitre() + " " + p.getDescription())
                        .collect(Collectors.joining(", "));

                if (allSkills.trim().isEmpty()) {
                    allSkills = "Java, Swing, JDBC"; // Fallback pour démo
                }

                // 2. Appeler l'IA
                CareerService.PredictionResult result = careerService.predict(allSkills);

                Platform.runLater(() -> {
                    paneLoading.setVisible(false);
                    if (result.success && !result.predictions.isEmpty()) {
                        updateUI(result.predictions);
                    } else {
                        lblTopJob.setText("Erreur IA");
                        System.err.println("Career Error: " + result.error);
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> paneLoading.setVisible(false));
            }
        }).start();
    }

    private void updateUI(List<CareerService.PredictionItem> predictions) {
        // Le premier est le meilleur match
        var top = predictions.get(0);
        lblTopJob.setText(top.job);
        lblTopScore.setText(top.score + "%");
        lblSalary.setText(top.salary);
        lblDemand.setText(top.demand);

        // Liste des autres
        for (int i = 0; i < predictions.size(); i++) {
            var p = predictions.get(i);
            vboxPredictions.getChildren().add(createPredictionRow(p));
        }
    }

    private HBox createPredictionRow(CareerService.PredictionItem p) {
        HBox row = new HBox(15);
        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        row.setStyle("-fx-background-color: white; -fx-padding: 15; -fx-background-radius: 10; -fx-border-color: #f1f5f9;");

        VBox info = new VBox(5);
        Label name = new Label(p.job);
        name.setStyle("-fx-font-weight: bold; -fx-text-fill: #1e293b;");
        
        ProgressBar bar = new ProgressBar(p.score / 100.0);
        bar.setMaxWidth(Double.MAX_VALUE);
        bar.setPrefHeight(8);
        bar.setStyle("-fx-accent: #6366f1;");
        
        info.getChildren().addAll(name, bar);
        HBox.setHgrow(info, Priority.ALWAYS);

        Label score = new Label(p.score + "%");
        score.setStyle("-fx-font-weight: bold; -fx-text-fill: #6366f1; -fx-min-width: 50;");

        row.getChildren().addAll(info, score);
        return row;
    }
}
