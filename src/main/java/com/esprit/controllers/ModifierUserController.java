package com.esprit.controllers;

import com.esprit.dao.UtilisateurDAO;
import com.esprit.models.Utilisateur;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;

public class ModifierUserController implements Initializable {

    @FXML private TextField prenomField;
    @FXML private TextField nomField;
    @FXML private TextField emailField;
    @FXML private ComboBox<String> roleCombo;
    @FXML private ComboBox<String> statusCombo;
    @FXML private Label errorLabel;

    private UtilisateurDAO dao = new UtilisateurDAO();
    private Utilisateur utilisateur;
    private BackOfficeController backOfficeController;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        roleCombo.getItems().addAll("admin", "adminm", "etudiant", "enseignant");
        statusCombo.getItems().addAll("actif", "desactiver");
    }

    public void setUtilisateur(Utilisateur u, BackOfficeController controller) {
        this.utilisateur = u;
        this.backOfficeController = controller;
        prenomField.setText(u.getPrenom());
        nomField.setText(u.getNom());
        emailField.setText(u.getEmail());
        roleCombo.setValue(u.getRole());
        statusCombo.setValue(u.getStatus());
    }

    @FXML
    public void handleModifier() {
        String prenom = prenomField.getText().trim();
        String nom = nomField.getText().trim();
        String email = emailField.getText().trim();
        String role = roleCombo.getValue();
        String status = statusCombo.getValue();

        if (prenom.isEmpty() || nom.isEmpty() || email.isEmpty() || role == null || status == null) {
            errorLabel.setText("Veuillez remplir tous les champs !");
            return;
        }

        utilisateur.setPrenom(prenom);
        utilisateur.setNom(nom);
        utilisateur.setEmail(email);
        utilisateur.setRole(role);
        utilisateur.setStatus(status);

        dao.modifier(utilisateur);

        if (backOfficeController != null) {
            backOfficeController.refreshTable();
        }

        handleAnnuler();
    }

    @FXML
    public void handleAnnuler() {
        Stage stage = (Stage) nomField.getScene().getWindow();
        stage.close();
    }
}