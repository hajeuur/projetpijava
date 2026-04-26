package edu.connection3a36.Controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import org.json.JSONObject;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ResourceBundle;
import java.util.UUID;
import org.json.JSONArray;

public class AnalyseCVController implements Initializable {

    @FXML private VBox uploadBox, resultsBox, boxDetails;
    @FXML private Label lblFileName, lblNomCandidat, lblEmailCandidat, lblPosteLabel, lblScorePercent, lblScoreComment;
    @FXML private ProgressBar progressScore;
    @FXML private StackPane paneLoading;

    // Clé API Affinda réelle fournie par l'utilisateur
    private final String AFFINDA_API_KEY = "aff_cba74e4a5b6d9f8c1fd7b2af3a55c1d357c0bfc3"; 

    @Override
    public void initialize(URL url, ResourceBundle rb) {}

    @FXML
    private void choisirFichier() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        File file = fileChooser.showOpenDialog(uploadBox.getScene().getWindow());
        
        if (file != null) {
            lblFileName.setText(file.getName());
            analyserCV(file);
        }
    }

    private void analyserCV(File file) {
        paneLoading.setVisible(true);
        paneLoading.setManaged(true);

        new Thread(() -> {
            try {
                // Appel au script Python intermédiaire
                ProcessBuilder pb = new ProcessBuilder("python", "analyze_cv.py", AFFINDA_API_KEY, file.getAbsolutePath());
                pb.directory(new File(System.getProperty("user.dir")));
                Process process = pb.start();

                // Lire la réponse JSON du script
                java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(process.getInputStream()));
                StringBuilder res = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    res.append(line);
                }

                String output = res.toString();
                if (output.trim().isEmpty()) {
                    throw new Exception("Le script Python n'a renvoyé aucune donnée.");
                }

                JSONObject json = new JSONObject(output);
                
                if (json.has("error")) {
                    throw new Exception(json.getString("error"));
                }

                // Extraction des données (données parsées sont dans l'objet racine ou "data")
                JSONObject data = json.optJSONObject("data");
                if (data == null) data = json;
                
                final JSONObject finalData = data;
                Platform.runLater(() -> afficherResultats(finalData));

            } catch (Exception e) {
                System.err.println("Erreur Analyse Python : " + e.getMessage());
                Platform.runLater(() -> {
                    new Alert(Alert.AlertType.WARNING, "Échec Python : " + e.getMessage() + "\nActivation du mode Simulation...").show();
                    lancerSimulation();
                });
            }
        }).start();
    }

    private void lancerSimulation() {
        // Crée des données fictives réalistes pour montrer le dashboard
        JSONObject sim = new JSONObject();
        sim.put("profession", "Développeur Java Fullstack");
        sim.put("emails", new org.json.JSONArray().put("candidat@mentorai.tn"));
        sim.put("name", new JSONObject().put("raw", "UTILISATEUR DEMO"));
        sim.put("skills", new org.json.JSONArray().put("Java").put("JavaFX").put("Spring").put("MySQL").put("Git").put("IA"));
        sim.put("workExperience", new org.json.JSONArray().put(1).put(2));
        sim.put("education", new org.json.JSONArray().put(1));
        
        paneLoading.setVisible(false);
        afficherResultats(sim);
    }

    private void afficherResultats(JSONObject data) {
        paneLoading.setVisible(false);
        uploadBox.setVisible(false);
        uploadBox.setManaged(false);
        resultsBox.setVisible(true);
        resultsBox.setManaged(true);
        boxDetails.getChildren().clear();

        // Extraction des données de base
        JSONObject nameObj = data.optJSONObject("name");
        String nom = (nameObj != null) ? nameObj.optString("raw", "Inconnu") : "Inconnu";
        String email = data.optJSONArray("emails") != null && !data.getJSONArray("emails").isEmpty() 
                       ? data.getJSONArray("emails").getString(0) : "Non trouvé";
        String job = data.optString("profession", "Non identifié");

        lblNomCandidat.setText(nom);
        lblEmailCandidat.setText(email);
        lblPosteLabel.setText(job);

        // Algorithme de score de conformité aux normes (ATS/Structure)
        double score = 0;
        if (!nom.equals("Inconnu")) score += 20;
        if (!email.equals("Non trouvé")) score += 20;
        if (data.optJSONArray("workExperience") != null && data.getJSONArray("workExperience").length() > 0) score += 30;
        if (data.optJSONArray("education") != null && data.getJSONArray("education").length() > 0) score += 20;
        if (data.optJSONArray("skills") != null && data.getJSONArray("skills").length() > 5) score += 10;

        progressScore.setProgress(score / 100.0);
        lblScorePercent.setText((int)score + "%");
        
        if (score > 80) lblScoreComment.setText("✅ CV EXCELLENT - Normes respectées");
        else if (score > 50) lblScoreComment.setText("⚠️ CV CORRECT - Quelques manques");
        else lblScoreComment.setText("❌ CV NON-CONFORME - Sections manquantes");

        // Détails dynamiques
        ajouterDetail("📋 Nombre d'expériences : " + (data.optJSONArray("workExperience") != null ? data.getJSONArray("workExperience").length() : 0));
        ajouterDetail("🎓 Formations trouvées : " + (data.optJSONArray("education") != null ? data.getJSONArray("education").length() : 0));
        ajouterDetail("🛠 Compétences clés identifiées : " + (data.optJSONArray("skills") != null ? Math.min(10, data.getJSONArray("skills").length()) : 0));
        ajouterDetail("🌍 Langues : " + (data.optJSONArray("languages") != null ? data.getJSONArray("languages").length() : "Standard"));
    }

    private void ajouterDetail(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: #64748b; -fx-font-size: 14px;");
        boxDetails.getChildren().add(l);
    }

    @FXML
    private void retourParcours() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/AfficherParcours.fxml"));
            Parent view = loader.load();
            BorderPane mainLayout = (BorderPane) resultsBox.getScene().lookup("#mainContainer");
            if (mainLayout != null) mainLayout.setCenter(view);
        } catch (IOException e) { e.printStackTrace(); }
    }
}
