package com.esprit.controllers;

import com.esprit.models.Utilisateur;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class ProfilUserController {

    @FXML private Label nomCompletLabel;
    @FXML private Label emailHeaderLabel;
    @FXML private Label roleLabel;
    @FXML private Label idLabel;
    @FXML private Label nomLabel;
    @FXML private Label prenomLabel;
    @FXML private Label emailLabel;
    @FXML private Label roleInfoLabel;
    @FXML private Label statusLabel;

    public void setUtilisateur(Utilisateur u) {
        nomCompletLabel.setText(u.getPrenom() + " " + u.getNom());
        emailHeaderLabel.setText(u.getEmail());
        roleLabel.setText(u.getRole().toUpperCase());
        idLabel.setText("#" + u.getId());
        nomLabel.setText(u.getNom());
        prenomLabel.setText(u.getPrenom());
        emailLabel.setText(u.getEmail());
        roleInfoLabel.setText(u.getRole());
        statusLabel.setText(u.getStatus());
    }

    @FXML
    public void handleFermer() {
        Stage stage = (Stage) nomLabel.getScene().getWindow();
        stage.close();
    }
}