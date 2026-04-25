package edu.connection3a36.controllers;

import edu.connection3a36.entities.Utilisateur;
import edu.connection3a36.services.UtilisateurService;
import edu.connection3a36.tools.AlertUtil;
import edu.connection3a36.tools.ValidationUtil;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.sql.SQLException;

public class UtilisateurFormController {

    @FXML private Label formTitle;
    @FXML private TextField tfPrenom;
    @FXML private TextField tfNom;
    @FXML private TextField tfEmail;
    @FXML private PasswordField tfMdp;
    @FXML private ComboBox<String> cbRole;
    @FXML private ComboBox<String> cbStatus;

    @FXML private Label errPrenom;
    @FXML private Label errNom;
    @FXML private Label errEmail;
    @FXML private Label errMdp;

    private final UtilisateurService service = new UtilisateurService();
    private Utilisateur userToEdit = null;

    @FXML
    public void initialize() {
        cbRole.setItems(FXCollections.observableArrayList("ETUDIANT", "ENSEIGNANT", "ADMINISTRATEUR"));
        cbRole.setValue("ETUDIANT");

        cbStatus.setItems(FXCollections.observableArrayList("actif", "inactif", "banni"));
        cbStatus.setValue("actif");
    }

    public void setUserToEdit(Utilisateur user) {
        this.userToEdit = user;
        formTitle.setText("Modifier Utilisateur #" + user.getId());
        tfPrenom.setText(user.getPrenom());
        tfNom.setText(user.getNom());
        tfEmail.setText(user.getEmail());
        
        // Cacher le mot de passe lors de la modification (pour simplifier, on ne le modifie pas depuis ce form de base)
        tfMdp.setDisable(true);
        tfMdp.setPromptText("Non modifiable ici");

        cbRole.setValue(user.getRole());
        cbStatus.setValue(user.getStatus());
    }

    @FXML
    void handleSave() {
        clearErrors();
        boolean hasError = false;

        String prenomErr = ValidationUtil.validateTextField(tfPrenom.getText(), "Prénom", true, 2, 50);
        if (prenomErr != null) { showError(errPrenom, prenomErr); hasError = true; }

        String nomErr = ValidationUtil.validateTextField(tfNom.getText(), "Nom", true, 2, 50);
        if (nomErr != null) { showError(errNom, nomErr); hasError = true; }

        if (tfEmail.getText().trim().isEmpty() || !tfEmail.getText().contains("@")) {
            showError(errEmail, "Email invalide");
            hasError = true;
        }

        if (userToEdit == null && tfMdp.getText().trim().length() < 6) {
            showError(errMdp, "Le mot de passe doit faire au moins 6 caractères");
            hasError = true;
        }

        if (hasError) return;

        try {
            Utilisateur u = userToEdit != null ? userToEdit : new Utilisateur();
            u.setPrenom(tfPrenom.getText().trim());
            u.setNom(tfNom.getText().trim());
            u.setEmail(tfEmail.getText().trim());
            u.setRole(cbRole.getValue());
            u.setStatus(cbStatus.getValue());

            if (userToEdit != null) {
                service.updateEntity(u.getId(), u);
                AlertUtil.showSuccess("Utilisateur modifié avec succès !");
            } else {
                u.setMdp(tfMdp.getText().trim()); // Normalement à hasher
                service.addEntity(u);
                AlertUtil.showSuccess("Utilisateur créé avec succès !");
            }
            closeWindow();
        } catch (SQLException e) {
            AlertUtil.showError("Erreur SQL : " + e.getMessage());
        }
    }

    @FXML
    void handleCancel() {
        closeWindow();
    }

    private void showError(Label label, String message) {
        label.setText(message);
        label.setVisible(true);
        label.setManaged(true);
    }

    private void clearErrors() {
        for (Label lbl : new Label[]{errPrenom, errNom, errEmail, errMdp}) {
            lbl.setText("");
            lbl.setVisible(false);
            lbl.setManaged(false);
        }
    }

    private void closeWindow() {
        Stage stage = (Stage) tfNom.getScene().getWindow();
        stage.close();
    }
}
