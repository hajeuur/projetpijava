package edu.connection3a36.controllers;

import edu.connection3a36.entities.Projet;
import edu.connection3a36.services.CareerService;
import edu.connection3a36.services.ProjetService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Priority;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import java.io.IOException;

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
        if (paneLoading != null) paneLoading.setVisible(true);
        if (vboxPredictions != null) vboxPredictions.getChildren().clear();

        new Thread(() -> {
            try {
                List<Projet> projets = projetService.getData(); 
                String allSkills = projets.stream()
                        .map(p -> p.getTechnologies() + " " + p.getTitre())
                        .collect(Collectors.joining(", "));

                if (allSkills.trim().isEmpty()) {
                    allSkills = "Java, SQL, HTML, CSS"; 
                }

                CareerService.PredictionResult result = careerService.predict(allSkills);

                Platform.runLater(() -> {
                    if (paneLoading != null) paneLoading.setVisible(false);
                    if (result.success && !result.predictions.isEmpty()) {
                        updateUI(result.predictions);
                    } else {
                        if (lblTopJob != null) lblTopJob.setText("Erreur IA");
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> { if (paneLoading != null) paneLoading.setVisible(false); });
            }
        }).start();
    }

    private void updateUI(List<CareerService.PredictionItem> predictions) {
        if (predictions.isEmpty()) return;
        
        var top = predictions.get(0);
        if (lblTopJob != null) lblTopJob.setText(top.job);
        if (lblTopScore != null) lblTopScore.setText(top.score + "%");
        if (lblSalary != null) lblSalary.setText(top.salary);
        if (lblDemand != null) lblDemand.setText(top.demand);

        if (vboxPredictions != null) {
            for (int i = 0; i < predictions.size(); i++) {
                vboxPredictions.getChildren().add(createPredictionRow(predictions.get(i)));
            }
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

    @FXML
    private void ouvrirEntretien() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/EntretienIA.fxml"));
            Parent view = loader.load();
            MainController.getInstance().loadInContentArea(view);
        } catch (IOException e) { e.printStackTrace(); }
    }
}
