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
import javafx.stage.Stage;
import javafx.scene.Scene;

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
        boolean isMissing = pane == flowMissing;
        for (int i = 0; i < array.length(); i++) {
            String skill = array.getString(i);
            Label tag = new Label(skill);
            String style = "-fx-background-color: " + bgColor + "; -fx-text-fill: " + textColor + "; -fx-padding: 5 12; -fx-background-radius: 20; -fx-font-weight: bold; -fx-font-size: 13px;";
            if (isMissing) {
                style += " -fx-cursor: hand;";
                tag.setTooltip(new Tooltip("Cliquez pour générer une roadmap !"));
                tag.setOnMouseClicked(e -> genererRoadmap(skill));
            }
            tag.setStyle(style);
            pane.getChildren().add(tag);
        }
    }

    private void genererRoadmap(String skill) {
        String targetJob = cbTargetJob.getValue();
        paneLoading.setVisible(true);
        
        String prompt = "Génère une roadmap de formation structurée sur 3 mois pour apprendre '" + skill + "' " +
                        "dans l'optique de devenir '" + targetJob + "'. " +
                        "CONSIGNES CRITIQUES POUR LES LIENS : " +
                        "1. Ne donne JAMAIS de liens YouTube directs (ils sont souvent faux). Donne à la place un lien de RECHERCHE YouTube bien balisé, par exemple : 'https://www.youtube.com/results?search_query=formation+complete+" + skill.replace(" ", "+") + "'. " +
                        "2. Pour la documentation, utilise uniquement des domaines officiels connus (ex: spring.io, react.dev, docs.docker.com). " +
                        "3. Pour chaque mois, fournis : " +
                        "   - Un objectif clair. " +
                        "   - Un projet pratique. " +
                        "   - Un bouton/lien de recherche YouTube optimisé. " +
                        "   - Un lien vers la doc officielle. " +
                        "4. Réponds en français avec un ton de mentor expert. Ne renvoie aucun JSON.";

        edu.connection3a36.services.GroqService.getResponse(prompt, false)
            .thenAccept(response -> {
                Platform.runLater(() -> {
                    paneLoading.setVisible(false);
                    String clean = response.replaceAll("\\[JSON\\].*?\\[/JSON\\]", "").trim();
                    afficherRoadmap(skill, clean);
                });
            });
    }

    private void afficherRoadmap(String skill, String roadmapText) {
        Stage stage = new Stage();
        stage.setTitle("Ma Roadmap IA : " + skill);

        VBox root = new VBox(15);
        root.setStyle("-fx-padding: 30; -fx-background-color: white;");
        
        Label title = new Label("🎯 Ma Progression : " + skill);
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #6366f1;");

        TextArea content = new TextArea(roadmapText);
        content.setWrapText(true);
        content.setEditable(false);
        content.setPrefHeight(500);
        content.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 14px; -fx-background-color: #f1f5f9; -fx-padding: 10;");

        Button btnDownload = new Button("📥 Exporter en PDF Premium");
        btnDownload.setStyle("-fx-background-color: #10b981; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 12 25; -fx-background-radius: 30; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 5, 0, 0, 2);");
        btnDownload.setOnAction(e -> exportToPDF(skill, roadmapText));

        HBox footer = new HBox(btnDownload);
        footer.setAlignment(Pos.CENTER_RIGHT);

        root.getChildren().addAll(title, content, footer);

        Scene scene = new Scene(root, 700, 650);
        stage.setScene(scene);
        stage.show();
    }

    private void exportToPDF(String skill, String content) {
        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle("Enregistrer la Roadmap Premium");
        fileChooser.setInitialFileName("MentorAI_Roadmap_" + skill.replace(" ", "_") + ".pdf");
        fileChooser.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        
        java.io.File file = fileChooser.showSaveDialog(lblScore.getScene().getWindow());
        
        if (file != null) {
            try {
                com.itextpdf.text.Document document = new com.itextpdf.text.Document();
                com.itextpdf.text.pdf.PdfWriter.getInstance(document, new java.io.FileOutputStream(file));
                document.setMargins(50, 50, 50, 50);
                document.open();
                
                // Colors
                com.itextpdf.text.BaseColor primaryColor = new com.itextpdf.text.BaseColor(99, 102, 241);
                com.itextpdf.text.BaseColor darkColor = new com.itextpdf.text.BaseColor(30, 41, 59);
                com.itextpdf.text.BaseColor linkColor = new com.itextpdf.text.BaseColor(37, 99, 235);

                // Fonts
                com.itextpdf.text.Font headerFont = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 24, com.itextpdf.text.Font.BOLD, primaryColor);
                com.itextpdf.text.Font subHeaderFont = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 14, com.itextpdf.text.Font.ITALIC, com.itextpdf.text.BaseColor.GRAY);
                com.itextpdf.text.Font bodyFont = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 11, com.itextpdf.text.Font.NORMAL, darkColor);
                com.itextpdf.text.Font linkFont = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 11, com.itextpdf.text.Font.UNDERLINE, linkColor);
                com.itextpdf.text.Font monthFont = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 13, com.itextpdf.text.Font.BOLD, com.itextpdf.text.BaseColor.BLACK);

                // Header
                document.add(new com.itextpdf.text.Paragraph("MentorAI - Roadmap de Formation", headerFont));
                document.add(new com.itextpdf.text.Paragraph("Objectif : " + cbTargetJob.getValue(), subHeaderFont));
                document.add(new com.itextpdf.text.Paragraph("Spécialité : " + skill + "\n\n", bodyFont));
                
                // Capture Radar Chart (Snapshot)
                try {
                    javafx.scene.image.WritableImage snapshot = chartContainer.snapshot(null, null);
                    java.awt.image.BufferedImage bImage = javafx.embed.swing.SwingFXUtils.fromFXImage(snapshot, null);
                    java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
                    javax.imageio.ImageIO.write(bImage, "png", baos);
                    com.itextpdf.text.Image img = com.itextpdf.text.Image.getInstance(baos.toByteArray());
                    img.scaleToFit(200, 150);
                    img.setAlignment(com.itextpdf.text.Element.ALIGN_RIGHT);
                    document.add(img);
                } catch (Exception ex) {
                    // Ignore chart if snapshot fails
                }

                document.add(new com.itextpdf.text.Paragraph("\n"));
                com.itextpdf.text.pdf.draw.LineSeparator ls = new com.itextpdf.text.pdf.draw.LineSeparator();
                ls.setLineColor(primaryColor);
                document.add(new com.itextpdf.text.Chunk(ls));

                // Process Content and Links
                String[] lines = content.split("\n");
                for (String line : lines) {
                    if (line.toLowerCase().contains("mois")) {
                        com.itextpdf.text.Paragraph p = new com.itextpdf.text.Paragraph(line, monthFont);
                        p.setSpacingBefore(15);
                        document.add(p);
                    } else if (line.contains("http")) {
                        // Extract URL
                        String url = "";
                        int start = line.indexOf("http");
                        int end = line.indexOf(" ", start);
                        if (end == -1) end = line.length();
                        url = line.substring(start, end).trim();
                        
                        String label = line.substring(0, start).replace("*", "").replace(":", "").trim();
                        if (label.isEmpty()) label = "Lien utile";
                        
                        com.itextpdf.text.Anchor anchor = new com.itextpdf.text.Anchor("➜ " + label + " [Cliquez ici]", linkFont);
                        anchor.setReference(url);
                        
                        com.itextpdf.text.Paragraph p = new com.itextpdf.text.Paragraph();
                        p.add(new com.itextpdf.text.Chunk("  ")); // Indentation
                        p.add(anchor);
                        document.add(p);
                    } else {
                        document.add(new com.itextpdf.text.Paragraph(line, bodyFont));
                    }
                }

                document.close();
                afficherInfo("Succès", "Votre Roadmap Premium a été générée avec le graphique Skill Gap ! ✨");
            } catch (Exception e) {
                e.printStackTrace();
                afficherErreur("Erreur Export PDF", e.getMessage());
            }
        }
    }

    private void afficherInfo(String titre, String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(titre);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.show();
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
