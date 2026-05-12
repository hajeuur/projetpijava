package com.esprit.controllers;

import com.esprit.models.Utilisateur;
import com.esprit.services.UtilisateurService;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;

public class ModifierUserController implements Initializable {

    @FXML private TextField        prenomField;
    @FXML private TextField        nomField;
    @FXML private TextField        emailField;
    @FXML private ComboBox<String> roleCombo;
    @FXML private ComboBox<String> statusCombo;
    @FXML private Label            errorLabel;
    @FXML private ImageView        photoView;
    @FXML private Label            photoPathLabel;

    private final UtilisateurService service = new UtilisateurService();
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
        // Afficher la photo actuelle
        chargerPhoto(u.getPdpUrl());
        if (photoPathLabel != null && u.getPdpUrl() != null && !u.getPdpUrl().isBlank()) {
            photoPathLabel.setText(new File(u.getPdpUrl()).getName());
        }
    }

    /** Charge et affiche la photo depuis le chemin absolu. */
    private void chargerPhoto(String pdpUrl) {
        if (photoView == null) return;
        if (pdpUrl != null && !pdpUrl.isBlank()) {
            File f = new File(pdpUrl);
            if (f.exists()) {
                try { photoView.setImage(new Image(f.toURI().toString())); return; } catch (Exception ignored) {}
            }
        }
        // Image par défaut
        java.net.URL defUrl = getClass().getResource("/com/esprit/views/default_avatar.png");
        if (defUrl != null) {
            try { photoView.setImage(new Image(defUrl.toExternalForm())); } catch (Exception ignored) {}
        }
    }

    @FXML
    public void handleChangerPhoto() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Choisir une photo de profil");
        fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images", "*.jpg", "*.jpeg", "*.png", "*.gif", "*.bmp"));
        File file = fc.showOpenDialog((Stage) nomField.getScene().getWindow());
        if (file != null) {
            utilisateur.setPdpUrl(file.getAbsolutePath());
            chargerPhoto(file.getAbsolutePath());
            if (photoPathLabel != null) photoPathLabel.setText(file.getName());
        }
    }

    @FXML
    public void handleModifier() {
        String prenom = prenomField.getText().trim();
        String nom    = nomField.getText().trim();
        String email  = emailField.getText().trim();
        String role   = roleCombo.getValue();
        String status = statusCombo.getValue();

        if (prenom.isEmpty() || nom.isEmpty() || email.isEmpty() || role == null || status == null) {
            errorLabel.setText("Veuillez remplir tous les champs !"); return;
        }

        utilisateur.setPrenom(prenom);
        utilisateur.setNom(nom);
        utilisateur.setEmail(email);
        utilisateur.setRole(role);
        utilisateur.setStatus(status);
        // pdp_url déjà mis à jour dans handleChangerPhoto si l'utilisateur a changé la photo

        service.modifier(utilisateur);

        if (backOfficeController != null) backOfficeController.refreshTable();
        handleAnnuler();
    }

    @FXML
    public void handleAnnuler() {
        Stage stage = (Stage) nomField.getScene().getWindow();
        stage.close();
    }
}
