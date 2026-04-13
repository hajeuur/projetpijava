package com.esprit.controllers;

import com.esprit.dao.UtilisateurDAO;
import com.esprit.models.Utilisateur;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class LoginController {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;

    private UtilisateurDAO dao = new UtilisateurDAO();

    @FXML
    public void handleLogin() {
        String email = emailField.getText().trim();
        String mdp = passwordField.getText().trim();

        if (email.isEmpty() || mdp.isEmpty()) {
            errorLabel.setText("Veuillez remplir tous les champs !");
            return;
        }

        Utilisateur u = dao.login(email, mdp);

        if (u == null) {
            errorLabel.setText("Email ou mot de passe incorrect !");
            return;
        }

        // Vérification compte désactivé
        if (u.getStatus().equals("desactiver")) {
            errorLabel.setText("Votre compte est désactivé. Contactez l'administrateur !");
            return;
        }

        try {
            String role = u.getRole();
            String fxmlPath;

            if (role.equals("admin")) {
                fxmlPath = "/com/esprit/views/BackOffice.fxml";
            } else {
                fxmlPath = "/com/esprit/views/FrontOffice.fxml";
            }

            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();

            if (!role.equals("admin")) {
                FrontOfficeController controller = loader.getController();
                controller.setUtilisateur(u);
            }

            Stage stage = (Stage) emailField.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();

        } catch (Exception e) {
            errorLabel.setText("Erreur de navigation : " + e.getMessage());
        }
    }

    @FXML
    public void handleInscription() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/esprit/views/Inscription.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("Inscription");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            errorLabel.setText("Erreur : " + e.getMessage());
        }
    }
}