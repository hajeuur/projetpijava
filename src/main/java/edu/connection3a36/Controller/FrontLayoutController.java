package edu.connection3a36.Controller;

import edu.connection3a36.tools.SessionManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class FrontLayoutController implements Initializable {

    @FXML
    private BorderPane mainContainer;
    @FXML
    private Label lblUserName;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        if (SessionManager.getInstance().getCurrentUser() != null) {
            String email = SessionManager.getInstance().getCurrentUser().getEmail();
            lblUserName.setText(email.split("@")[0]); // Afficher le début de l'email
        }

        // Charger la vue principale du Front-Office par défaut
        loadView("/AfficherParcours.fxml");
    }

    @FXML
    private void loadParcours() {
        loadView("/AfficherParcours.fxml");
    }

    @FXML
    private void loadProjets() {
        loadView("/AfficherProjetsGlobal.fxml");
    }

    @FXML
    private void loadSkillGap() {
        loadView("/SkillGap.fxml");
    }

    @FXML
    private void loadPricing() {
        loadView("/Pricing.fxml");
    }

    @FXML
    private void loadAbout() {
        loadView("/About.fxml");
    }

    @FXML
    private void logout() {
        SessionManager.getInstance().logout();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Connexion.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) mainContainer.getScene().getWindow();
            stage.setTitle("MentorAI - Connexion");

            // On s'assure que la connexion a une taille correcte
            stage.setScene(new Scene(root, 600, 500));
            stage.setResizable(false);
            stage.centerOnScreen();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadView(String fxml) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
            Parent view = loader.load();
            mainContainer.setCenter(view);
        } catch (IOException e) {
            System.err.println("Erreur de chargement de la vue Front : " + fxml);
            e.printStackTrace();
            mainContainer.setCenter(new Label("Erreur 404 : Vue introuvable."));
        }
    }
}
