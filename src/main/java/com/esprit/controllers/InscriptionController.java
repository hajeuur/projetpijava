package com.esprit.controllers;

import com.esprit.dao.UtilisateurDAO;
import com.esprit.models.Utilisateur;
import com.esprit.utils.GoogleAuthService;
import com.esprit.utils.GoogleUserInfo;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
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

    private UtilisateurDAO dao = new UtilisateurDAO();
    private BackOfficeController backOfficeController;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        roleCombo.getItems().addAll("admin", "adminm", "etudiant", "enseignant");
    }

    public void setBackOfficeController(BackOfficeController controller) {
        this.backOfficeController = controller;
    }

    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            return password;
        }
    }

    private boolean emailExiste(String email) {
        List<Utilisateur> tous = dao.getAll();
        for (Utilisateur u : tous) {
            if (u.getEmail().equalsIgnoreCase(email)) {
                return true;
            }
        }
        return false;
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

        if (!email.matches("^[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}$")) {
            errorLabel.setText("Adresse email invalide !");
            return;
        }

        if (emailExiste(email)) {
            errorLabel.setText("Cet email est déjà utilisé !");
            return;
        }

        if (mdp.length() < 8) {
            errorLabel.setText("Le mot de passe doit contenir au moins 8 caractères !");
            return;
        }

        String mdpHache = hashPassword(mdp);
        Utilisateur u = new Utilisateur(nom, prenom, email, mdpHache, role);
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
    public void handleGoogleSignup() {
        errorLabel.setText("");
        successLabel.setText("Ouverture du navigateur Google...");
        if (googleSignupButton != null) googleSignupButton.setDisable(true);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            try {
                System.out.println(">>> [Inscription Google] Début...");
                GoogleUserInfo googleUser = GoogleAuthService.authenticate();
                System.out.println(">>> [Inscription Google] Auth OK : " + googleUser);

                if (googleUser == null || googleUser.getEmail() == null) {
                    Platform.runLater(() -> {
                        successLabel.setText("");
                        errorLabel.setText("Authentification Google échouée.");
                    });
                    return;
                }

                System.out.println(">>> [Inscription Google] Vérification email : " + googleUser.getEmail());
                if (emailExiste(googleUser.getEmail())) {
                    System.out.println(">>> [Inscription Google] Email déjà existant !");
                    Platform.runLater(() -> {
                        successLabel.setText("");
                        errorLabel.setText("Ce compte Google est déjà enregistré. Connectez-vous directement.");
                    });
                    return;
                }

                System.out.println(">>> [Inscription Google] Création compte...");
                Utilisateur nouveau = new Utilisateur();
                nouveau.setEmail(googleUser.getEmail());
                nouveau.setNom(googleUser.getNom()    != null ? googleUser.getNom()    : "");
                nouveau.setPrenom(googleUser.getPrenom() != null ? googleUser.getPrenom() : "");
                nouveau.setMdp("");
                nouveau.setRole("etudiant");
                nouveau.setStatus("activer");
                nouveau.setTrustScore(0.0);   // ✅ valeur par défaut
                nouveau.setRiskLevel("low");  // ✅ valeur par défaut

                System.out.println(">>> [Inscription Google] Ajout en base...");
                dao.ajouter(nouveau);
                System.out.println(">>> [Inscription Google] Compte ajouté !");

                // Vérification
                Utilisateur check = dao.findByEmail(googleUser.getEmail());
                System.out.println(">>> [Inscription Google] Vérification DB : " +
                        (check != null ? "OK id=" + check.getId() : "INTROUVABLE !"));

                if (backOfficeController != null) {
                    Platform.runLater(() -> backOfficeController.refreshTable());
                }

                Platform.runLater(() -> {
                    errorLabel.setText("");
                    successLabel.setText("Compte Google créé avec succès ! Vous pouvez vous connecter.");
                    new Thread(() -> {
                        try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
                        Platform.runLater(() -> {
                            Stage stage = (Stage) nomField.getScene().getWindow();
                            stage.close();
                        });
                    }).start();
                });

            } catch (Exception e) {
                System.out.println(">>> [Inscription Google] ERREUR : " + e.getClass() + " : " + e.getMessage());
                e.printStackTrace();
                Platform.runLater(() -> {
                    successLabel.setText("");
                    errorLabel.setText("Erreur Google : " + e.getMessage());
                });
            } finally {
                Platform.runLater(() -> {
                    if (googleSignupButton != null) googleSignupButton.setDisable(false);
                });
                executor.shutdown();
            }
        });
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