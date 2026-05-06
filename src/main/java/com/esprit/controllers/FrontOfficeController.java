package com.esprit.controllers;

import com.esprit.models.Utilisateur;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;

public class FrontOfficeController implements Initializable {

    @FXML private Label userNameLabel;
    @FXML private Label welcomeLabel;
    @FXML private Label emailInfoLabel;
    @FXML private Label roleInfoLabel;
    @FXML private Label nomCardLabel;
    @FXML private Label emailCardLabel;
    @FXML private Label roleCardLabel;

    private Utilisateur utilisateur;

    @Override
    public void initialize(URL url, ResourceBundle rb) {}

    public void setUtilisateur(Utilisateur u) {
        this.utilisateur = u;
        userNameLabel.setText(u.getPrenom());
        welcomeLabel.setText("Bienvenue " + u.getPrenom() + " !");
        emailInfoLabel.setText("Vous êtes connecté en tant que " + u.getEmail());
        roleInfoLabel.setText("Connecté en tant que " + u.getRole());
        nomCardLabel.setText(u.getNom());
        emailCardLabel.setText(u.getEmail());
        roleCardLabel.setText(u.getRole());
    }

    @FXML
    public void handleMonProfil() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/esprit/views/ProfilUser.fxml"));
            Parent root = loader.load();
            ProfilUserController controller = loader.getController();
            controller.setUtilisateur(utilisateur);
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Mon profil");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void handleLogout() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/esprit/views/Login.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) userNameLabel.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}