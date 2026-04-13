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

public class DashboardController implements Initializable {
    @FXML
    private BorderPane mainContainer;
    @FXML
    private Label lblUserName, lblUserRole;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        if (SessionManager.getInstance().getCurrentUser() != null) {
            lblUserName.setText(SessionManager.getInstance().getCurrentUser().getEmail());
            lblUserRole.setText(SessionManager.getInstance().getCurrentUser().getRole());
        }
        loadBackOffice();
    }

    @FXML
    public void loadBackOffice() {
        loadView("/BackOfficeParcours.fxml");
    }

    @FXML
    public void loadProjets() {
        loadView("/BackOfficeProjets.fxml");
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
            mainContainer.setCenter(view);
        } catch (IOException e) {
            System.err.println("Erreur : " + fxml);
            e.printStackTrace();
            mainContainer.setCenter(new Label("Erreur : " + fxml));
        }
    }
}
