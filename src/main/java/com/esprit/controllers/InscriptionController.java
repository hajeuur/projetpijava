package com.esprit.controllers;

import com.esprit.models.Utilisateur;
import com.esprit.services.UtilisateurService;
import com.esprit.utils.GoogleAuthService;
import com.esprit.utils.GoogleUserInfo;
import javafx.application.Platform;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class InscriptionController implements Initializable {

    @FXML private TextField prenomField;
    @FXML private TextField nomField;
    @FXML private TextField emailField;
    @FXML private PasswordField mdpField;
    @FXML private ComboBox<String> roleCombo;
    @FXML private Label errorLabel;
    @FXML private Label successLabel;
    @FXML private Button googleSignupButton;
    @FXML private ImageView photoPreview;
    @FXML private Label photoPathLabel;

    private String selectedPhotoPath = null;

    private final UtilisateurService service = new UtilisateurService();
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
        String nom    = nomField.getText().trim();
        String email  = emailField.getText().trim();
        String mdp    = mdpField.getText().trim();
        String role   = roleCombo.getValue();

        errorLabel.setText("");
        successLabel.setText("");

        if (prenom.isEmpty() || nom.isEmpty() || email.isEmpty() || mdp.isEmpty() || role == null) {
            errorLabel.setText("Veuillez remplir tous les champs !"); return;
        }
        if (!service.emailValide(email)) {
            errorLabel.setText("Adresse email invalide !"); return;
        }
        if (service.emailExiste(email)) {
            errorLabel.setText("Cet email est déjà utilisé !"); return;
        }
        if (!service.mdpValide(mdp)) {
            errorLabel.setText("Le mot de passe doit contenir au moins 8 caractères !"); return;
        }

        Utilisateur u = new Utilisateur(nom, prenom, email, service.hashPassword(mdp), role);
        u.setPdpUrl(selectedPhotoPath);
        service.ajouter(u);
        successLabel.setText("Utilisateur créé avec succès !");

        if (backOfficeController != null) backOfficeController.refreshTable();

        handleEffacer();
        Stage stage = (Stage) nomField.getScene().getWindow();
        stage.close();
    }

    @FXML
    public void handleGoogleSignup() {
        errorLabel.setText("");
        successLabel.setText("Ouverture du navigateur Google...");
        if (googleSignupButton != null) googleSignupButton.setDisable(true);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            try {
                GoogleUserInfo googleUser = GoogleAuthService.authenticate();

                if (googleUser == null || googleUser.getEmail() == null) {
                    Platform.runLater(() -> { successLabel.setText(""); errorLabel.setText("Authentification Google échouée."); });
                    return;
                }

                if (service.emailExiste(googleUser.getEmail())) {
                    Platform.runLater(() -> { successLabel.setText(""); errorLabel.setText("Ce compte Google est déjà enregistré."); });
                    return;
                }

                Utilisateur nouveau = new Utilisateur();
                nouveau.setEmail(googleUser.getEmail());
                nouveau.setNom(googleUser.getNom()    != null ? googleUser.getNom()    : "");
                nouveau.setPrenom(googleUser.getPrenom() != null ? googleUser.getPrenom() : "");
                nouveau.setMdp("");
                nouveau.setRole("etudiant");
                nouveau.setStatus("activer");
                nouveau.setTrustScore(0.0);
                nouveau.setRiskLevel("low");

                service.ajouter(nouveau);

                if (backOfficeController != null)
                    Platform.runLater(() -> backOfficeController.refreshTable());

                Platform.runLater(() -> {
                    errorLabel.setText("");
                    successLabel.setText("Compte Google créé avec succès !");
                    new Thread(() -> {
                        try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
                        Platform.runLater(() -> { Stage stage = (Stage) nomField.getScene().getWindow(); stage.close(); });
                    }).start();
                });

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> { successLabel.setText(""); errorLabel.setText("Erreur Google : " + e.getMessage()); });
            } finally {
                Platform.runLater(() -> { if (googleSignupButton != null) googleSignupButton.setDisable(false); });
                executor.shutdown();
            }
        });
    }

    @FXML
    public void handleChoisirPhoto() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Choisir une photo de profil");
        fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images", "*.jpg", "*.jpeg", "*.png", "*.gif", "*.bmp"));
        File file = fc.showOpenDialog((Stage) nomField.getScene().getWindow());
        if (file != null) {
            selectedPhotoPath = file.getAbsolutePath();
            if (photoPathLabel != null) photoPathLabel.setText(file.getName());
            if (photoPreview != null) {
                try {
                    photoPreview.setImage(new Image(file.toURI().toString()));
                } catch (Exception ignored) {}
            }
        }
    }

    @FXML
    public void handleEffacer() {
        prenomField.clear(); nomField.clear(); emailField.clear();
        mdpField.clear(); roleCombo.setValue(null); errorLabel.setText("");
        selectedPhotoPath = null;
        if (photoPreview != null) photoPreview.setImage(null);
        if (photoPathLabel != null) photoPathLabel.setText("Aucune photo choisie");
    }

    @FXML
    public void handleRetour() {
        Stage stage = (Stage) nomField.getScene().getWindow();
        stage.close();
    }
}