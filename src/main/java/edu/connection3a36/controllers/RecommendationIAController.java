package edu.connection3a36.controllers;

import edu.connection3a36.entities.Parcours;
import edu.connection3a36.entities.Projet;
import edu.connection3a36.entities.Utilisateur;
import edu.connection3a36.services.ParcoursService;
import edu.connection3a36.services.ProjetService;
import edu.connection3a36.tools.SessionManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.List;

public class RecommendationIAController implements Initializable {

    @FXML private FlowPane flowResults;
    @FXML private StackPane paneLoading;

    private final String APP_ID = "4e630c20";
    private final String APP_KEY = "f6e51e220eccb4de666ad92ef1653bd9";
    
    private final ParcoursService parcoursService = new ParcoursService();
    private final ProjetService projetService = new ProjetService();

    @Override
    public void initialize(URL url, ResourceBundle rb) {}

    @FXML
    private void lancerAnalyse() {
        paneLoading.setVisible(true);
        paneLoading.setManaged(true);
        flowResults.getChildren().clear();

        new Thread(() -> {
            try {
                List<Parcours> userParcours = parcoursService.getData();
                List<Projet> userProjets = projetService.getData();
                Set<String> userTechs = new HashSet<>();
                for (Projet p : userProjets) {
                    if (p.getTechnologies() != null) {
                        for (String t : p.getTechnologies().split(",")) userTechs.add(t.trim().toLowerCase());
                    }
                }
                
                String query = userTechs.isEmpty() ? "développeur" : String.join(" ", userTechs.stream().limit(2).toArray(String[]::new));
                JSONArray results = fetchJobs(query);

                // Fallback: si aucun résultat, on tente une recherche plus large
                if (results.length() == 0 && !userTechs.isEmpty()) {
                    query = "développeur " + userTechs.iterator().next(); // On ne prend que la 1ère tech
                    results = fetchJobs(query);
                }
                
                // Second Fallback: recherche générique
                if (results.length() == 0) {
                    query = "développeur informatique";
                    results = fetchJobs(query);
                }

                final String finalQuery = query;
                final JSONArray finalResults = results;

                Platform.runLater(() -> {
                    paneLoading.setVisible(false);
                    paneLoading.setManaged(false);
                    
                    if (finalResults.length() == 0) {
                        Label lblNoResult = new Label("Aucune offre trouvée, même en recherche élargie.");
                        lblNoResult.setStyle("-fx-text-fill: #64748b; -fx-font-size: 16px;");
                        flowResults.getChildren().add(lblNoResult);
                    } else {
                        for (int i = 0; i < finalResults.length(); i++) {
                            JSONObject job = finalResults.getJSONObject(i);
                            double score = calculateMatchingScore(job, userParcours, userProjets, userTechs);
                            ajouterCardJob(job, score);
                        }
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    paneLoading.setVisible(false);
                    paneLoading.setManaged(false);
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Erreur de Connexion");
                    alert.setHeaderText("Impossible de contacter le serveur d'offres d'emploi");
                    alert.setContentText("Vérifiez votre connexion internet ou vos clés API.\n\nDétail : " + e.getMessage());
                    alert.show();
                    
                    Label lblRetry = new Label("Vérifiez votre connexion et réessayez.");
                    lblRetry.setStyle("-fx-text-fill: #ef4444;");
                    flowResults.getChildren().add(lblRetry);
                });
            }
        }).start();
    }



    @FXML
    private void retourParcours() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/AfficherParcours.fxml"));
            Parent view = loader.load();
            MainController.getInstance().loadInContentArea(view);
        } catch (IOException e) { e.printStackTrace(); }
    }

    private JSONArray fetchJobs(String query) throws Exception {
        String urlAdzuna = String.format("https://api.adzuna.com/v1/api/jobs/fr/search/1?app_id=%s&app_key=%s&what=%s&results_per_page=12",
                APP_ID, APP_KEY, URLEncoder.encode(query, StandardCharsets.UTF_8));
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder().uri(java.net.URI.create(urlAdzuna)).build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        JSONObject json = new JSONObject(response.body());
        return json.getJSONArray("results");
    }

    private double calculateMatchingScore(JSONObject job, List<Parcours> parcours, List<Projet> projets, Set<String> userTechs) {
        String desc = job.getString("description").toLowerCase();
        double skillsScore = 0;
        for (Parcours p : parcours) if (desc.contains(p.getTitre().toLowerCase())) skillsScore += 40;
        double projectsScore = Math.min(100, projets.size() * 25);
        double techScore = 0;
        for (String tech : userTechs) if (desc.contains(tech)) techScore += 35;
        double finalScore = (Math.min(100, skillsScore) * 0.4) + (Math.min(100, projectsScore) * 0.3) + (Math.min(100, techScore) * 0.2) + (80 * 0.1);
        return Math.min(99, finalScore + (Math.random() * 8));
    }

    private void ajouterCardJob(JSONObject job, double score) {
        VBox card = new VBox(15);
        card.setPrefWidth(320); card.setMinHeight(420);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 15; -fx-padding: 25; " +
                     "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 15, 0, 0, 4); " +
                     "-fx-border-color: #e2e8f0; -fx-border-radius: 15;");
        
        StackPane scoreContainer = new StackPane();
        scoreContainer.setPrefSize(60, 60); scoreContainer.setAlignment(Pos.CENTER);
        Circle backgroundCircle = new Circle(25); backgroundCircle.setFill(Color.web("#f1f5f9")); backgroundCircle.setStroke(Color.web("#10b981")); backgroundCircle.setStrokeWidth(3);
        Label lblScoreValue = new Label(String.format("%.0f%%", score)); lblScoreValue.setStyle("-fx-text-fill: #065f46; -fx-font-size: 14px; -fx-font-weight: bold;");
        scoreContainer.getChildren().addAll(backgroundCircle, lblScoreValue);
        HBox scoreHeader = new HBox(scoreContainer); scoreHeader.setAlignment(Pos.CENTER_LEFT);

        VBox infoArea = new VBox(5);
        Label lblTitle = new Label(job.getString("title")); lblTitle.setWrapText(true); lblTitle.setStyle("-fx-text-fill: #1e293b; -fx-font-size: 15px; -fx-font-weight: bold;");
        Label lblCompany = new Label(job.getJSONObject("company").getString("display_name")); lblCompany.setStyle("-fx-text-fill: #64748b; -fx-font-weight: bold; -fx-font-size: 13px;");
        Label lblLocation = new Label("📍 " + job.getJSONObject("location").getString("display_name")); lblLocation.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px;");
        infoArea.getChildren().addAll(lblTitle, lblCompany, lblLocation);

        Label lblDesc = new Label(job.getString("description")); lblDesc.setWrapText(true); lblDesc.setMaxHeight(60); lblDesc.setStyle("-fx-text-fill: #64748b; -fx-font-size: 12px;");
        
        Button btnPostuler = new Button("VOIR L'OFFRE"); btnPostuler.setMaxWidth(Double.MAX_VALUE);
        btnPostuler.setOnAction(e -> { try { java.awt.Desktop.getDesktop().browse(new java.net.URI(job.getString("redirect_url"))); } catch (Exception ex) {} });
        btnPostuler.setStyle("-fx-background-color: #102c59; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 10; -fx-cursor: hand;");
        
        card.getChildren().addAll(scoreHeader, infoArea, lblDesc, btnPostuler);
        flowResults.getChildren().add(card);
    }
}
