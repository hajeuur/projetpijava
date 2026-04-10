package com.esprit.controllers;

import com.esprit.dao.UtilisateurDAO;
import com.esprit.models.Utilisateur;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;

public class InscriptionController implements Initializable {

    @FXML private TextField prenomField;
    @FXML private TextField nomField;
    @FXML private TextField emailField;
    @FXML private PasswordField mdpField;
    @FXML private ComboBox<String> roleCombo;
    @FXML private Label errorLabel;
    @FXML private Label successLabel;

    private UtilisateurDAO dao = new UtilisateurDAO();
    private BackOfficeController backOfficeController;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        roleCombo.getItems().addAll("admin", "adminm", "etudiant", "enseignant");
    }

    public void setBackOfficeController(BackOfficeController controller) {
        this.backOfficeController = controller;
    }

    @FXML
    public void handleCreer() {
        String prenom = prenomField.getText().trim();
        String nom = nomField.getText().trim();
        String email = emailField.getText().trim();
        String mdp = mdpField.getText().trim();
        String role = roleCombo.getValue();

        errorLabel.setText("");
        successLabel.setText("");

        if (prenom.isEmpty() || nom.isEmpty() || email.isEmpty()
                || mdp.isEmpty() || role == null) {
            errorLabel.setText("Veuillez remplir tous les champs !");
            return;
        }

        if (mdp.length() < 8) {
            errorLabel.setText("Le mot de passe doit contenir au moins 8 caractères !");
            return;
        }

        if (!email.contains("@")) {
            errorLabel.setText("Adresse email invalide !");
            return;
        }

        Utilisateur u = new Utilisateur(nom, prenom, email, mdp, role);
        dao.ajouter(u);
        successLabel.setText("Utilisateur créé avec succès !");

        if (backOfficeController != null) {
            backOfficeController.refreshTable();
        }

        handleEffacer();
        Stage stage = (Stage) nomField.getScene().getWindow();
        stage.close();
    }

    @FXML
    public void handleEffacer() {
        prenomField.clear();
        nomField.clear();
        emailField.clear();
        mdpField.clear();
        roleCombo.setValue(null);
        errorLabel.setText("");
    }

    @FXML
    public void handleRetour() {
        Stage stage = (Stage) nomField.getScene().getWindow();
        stage.close();
    }
}