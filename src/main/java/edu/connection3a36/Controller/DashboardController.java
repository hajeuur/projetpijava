package edu.connection3a36.Controller;

import edu.connection3a36.tools.SessionManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class DashboardController implements Initializable {
    @FXML
    private BorderPane mainContainer;
    @FXML
    private StackPane centerContent;
    @FXML
    private Label lblSidebarName, lblSidebarRole;
    @FXML
    private Button btnHome, btnProjets, btnParcours, btnPricing;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        lblSidebarName.setText("admin");
        lblSidebarRole.setText("Administrateur");
        loadHome();
    }

    @FXML
    private void loadHome() {
        setActive(btnHome);
        loadView("/HomeBack.fxml");
    }

    @FXML
    public void loadProjets() {
        setActive(btnProjets);
        loadView("/BackOfficeProjets.fxml");
    }

    @FXML
    private void loadParcours() {
        setActive(btnParcours);
        loadView("/BackOfficeParcours.fxml");
    }

    @FXML
    private void loadPricing() {
        setActive(btnPricing);
        loadView("/PricingBack.fxml");
    }

    private void setActive(Button activeBtn) {
        btnHome.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: #9dbbce; -fx-font-size: 13px; -fx-padding: 12 25; -fx-cursor: hand;");
        btnProjets.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: #9dbbce; -fx-font-size: 13px; -fx-padding: 12 25; -fx-cursor: hand;");
        btnParcours.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: #9dbbce; -fx-font-size: 13px; -fx-padding: 12 25; -fx-cursor: hand;");

        btnPricing.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: #9dbbce; -fx-font-size: 13px; -fx-padding: 12 25; -fx-cursor: hand;");

        activeBtn.setStyle(
                "-fx-background-color: #1a3a6d; -fx-text-fill: white; -fx-font-size: 13px; -fx-padding: 12 25; -fx-cursor: hand; -fx-background-radius: 0;");
    }

    @FXML
    public void loadBackOffice() {
        loadParcours();
    }

    @FXML
    public void loadStats() {
        loadView("/Statistiques.fxml");
    }

    @FXML
    private void logout() {
        SessionManager.getInstance().logout();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Connexion.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) mainContainer.getScene().getWindow();
            stage.setTitle("MentorAI - Connexion");
            stage.setScene(new Scene(root, 600, 500));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadView(String fxml) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
            Parent view = loader.load();
            centerContent.getChildren().setAll(view);
        } catch (IOException e) {
            System.err.println("Erreur : " + fxml);
            e.printStackTrace();
            centerContent.getChildren().setAll(new Label("Erreur : " + fxml));
        }
    }
}
