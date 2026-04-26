package edu.connection3a36.Controller;

import edu.connection3a36.entities.Projet;
import edu.connection3a36.services.ProjetService;
import edu.connection3a36.services.PythonIAIService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URL;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

public class SkillGapController implements Initializable {

    @FXML private ComboBox<String> cbTargetJob;
    @FXML private ScrollPane scrollResults;
    @FXML private VBox paneEmpty, paneLoading;
    @FXML private Label lblScore, lblSuggestion;
    @FXML private ProgressIndicator progressScore;
    @FXML private StackPane chartContainer;
    @FXML private FlowPane flowMastered, flowMissing;

    private final ProjetService projetService = new ProjetService();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        cbTargetJob.setItems(FXCollections.observableArrayList(
            "Data Scientist", "Fullstack Developer", "Frontend Developer", 
            "Backend Developer", "Mobile Developer", "DevOps Engineer"
        ));
    }

    @FXML
    private void lancerAnalyse() {
        String target = cbTargetJob.getValue();
        if (target == null) return;

        paneEmpty.setVisible(false);
        paneLoading.setVisible(true);
        scrollResults.setVisible(false);

        new Thread(() -> {
            try {
                // 1. Get all skills from user projects
                List<Projet> projets = projetService.getData();
                Set<String> allSkills = new HashSet<>();
                for (Projet p : projets) {
                    if (p.getTechnologies() != null) {
                        String[] techs = p.getTechnologies().split(",");
                        for (String t : techs) allSkills.add(t.trim());
                    }
                }

                // 2. Call Python AI
                JSONObject result = PythonIAIService.analyzeSkillGap(new ArrayList<>(allSkills), target);

                Platform.runLater(() -> {
                    paneLoading.setVisible(false);
                    if (result.has("error")) {
                        afficherErreur("Erreur IA", result.getString("error"));
                        paneEmpty.setVisible(true);
                    } else {
                        afficherResultats(result);
                    }
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    paneLoading.setVisible(false);
                    afficherErreur("Erreur", e.getMessage());
                });
            }
        }).start();
    }

    private void afficherResultats(JSONObject data) {
        scrollResults.setVisible(true);
        double score = data.getDouble("score");
        lblScore.setText(String.format("%.0f%%", score));
        progressScore.setProgress(score / 100.0);
        lblSuggestion.setText(data.getString("suggestion"));

        // Skills List
        populateFlow(flowMastered, data.getJSONArray("mastered"), "#dcfce7", "#166534");
        populateFlow(flowMissing, data.getJSONArray("missing"), "#fee2e2", "#991b1b");

        // Radar Chart
        drawRadarChart(data.getJSONObject("radar_data"));
    }

    private void populateFlow(FlowPane pane, JSONArray array, String bgColor, String textColor) {
        pane.getChildren().clear();
        for (int i = 0; i < array.length(); i++) {
            Label tag = new Label(array.getString(i));
            tag.setStyle("-fx-background-color: " + bgColor + "; -fx-text-fill: " + textColor + "; -fx-padding: 5 12; -fx-background-radius: 20; -fx-font-weight: bold; -fx-font-size: 13px;");
            pane.getChildren().add(tag);
        }
    }

    private void drawRadarChart(JSONObject radarData) {
        chartContainer.getChildren().clear();
        
        JSONArray labels = radarData.getJSONArray("labels");
        JSONArray values = radarData.getJSONArray("values");
        int count = labels.length();
        if (count == 0) return;

        double centerX = 200;
        double centerY = 150;
        double radius = 100;

        Pane pane = new Pane();
        pane.setPrefSize(400, 300);

        // Draw background circles
        for (int i = 1; i <= 5; i++) {
            javafx.scene.shape.Circle c = new javafx.scene.shape.Circle(centerX, centerY, (radius / 5) * i);
            c.setFill(Color.TRANSPARENT);
            c.setStroke(Color.web("#e2e8f0"));
            pane.getChildren().add(c);
        }

        Polygon poly = new Polygon();
        poly.setFill(Color.web("#6366f1", 0.4));
        poly.setStroke(Color.web("#6366f1"));
        poly.setStrokeWidth(2);

        for (int i = 0; i < count; i++) {
            double angle = Math.toRadians(i * (360.0 / count) - 90);
            double val = values.getDouble(i) / 100.0;
            
            double x = centerX + Math.cos(angle) * (radius * val);
            double y = centerY + Math.sin(angle) * (radius * val);
            poly.getPoints().addAll(x, y);

            // Access point axis
            double ax = centerX + Math.cos(angle) * radius;
            double ay = centerY + Math.sin(angle) * radius;
            Line line = new Line(centerX, centerY, ax, ay);
            line.setStroke(Color.web("#cbd5e1"));
            pane.getChildren().add(line);

            // Label text
            double tx = centerX + Math.cos(angle) * (radius + 25);
            double ty = centerY + Math.sin(angle) * (radius + 25);
            Text t = new Text(labels.getString(i));
            t.setFont(Font.font("System", 10));
            t.setX(tx - 20);
            t.setY(ty);
            pane.getChildren().add(t);
        }

        pane.getChildren().add(poly);
        chartContainer.getChildren().add(pane);
    }

    private void afficherErreur(String titre, String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(titre);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.show();
    }
}
