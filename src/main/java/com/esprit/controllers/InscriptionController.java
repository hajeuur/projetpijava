package com.esprit.controllers;

import com.esprit.dao.UtilisateurDAO;
import com.esprit.models.Utilisateur;
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

    // Méthode de hachage SHA-256
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

    // Vérification unicité email
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

        // Champs vides
        if (prenom.isEmpty() || nom.isEmpty() || email.isEmpty()
                || mdp.isEmpty() || role == null) {
            errorLabel.setText("Veuillez remplir tous les champs !");
            return;
        }

        // Validation email
        if (!email.matches("^[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}$")) {
            errorLabel.setText("Adresse email invalide !");
            return;
        }

        // Unicité email
        if (emailExiste(email)) {
            errorLabel.setText("Cet email est déjà utilisé !");
            return;
        }

        // Longueur mot de passe
        if (mdp.length() < 8) {
            errorLabel.setText("Le mot de passe doit contenir au moins 8 caractères !");
            return;
        }

        // Hachage mot de passe
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