package com.esprit.controllers;

import com.esprit.dao.UtilisateurDAO;
import com.esprit.models.Utilisateur;
import com.esprit.utils.EmailUtil;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.util.List;

public class ForgetPasswordController {

    @FXML private TextField emailField;
    @FXML private Label messageLabel;
    @FXML private Button envoyerButton;

    private final UtilisateurDAO dao = new UtilisateurDAO();

    @FXML
    public void handleEnvoyer() {
        String email = emailField.getText().trim();

        messageLabel.setStyle("-fx-font-size: 12;");
        messageLabel.setText("");

        if (email.isEmpty()) {
            messageLabel.setStyle("-fx-text-fill: red; -fx-font-size: 12;");
            messageLabel.setText("Veuillez entrer votre adresse email !");
            return;
        }

        if (!email.matches("^[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}$")) {
            messageLabel.setStyle("-fx-text-fill: red; -fx-font-size: 12;");
            messageLabel.setText("Adresse email invalide !");
            return;
        }

        // Vérifie si l'email existe dans la base
        List<Utilisateur> tous = dao.getAll();
        boolean existe = tous.stream()
                .anyMatch(u -> u.getEmail().equalsIgnoreCase(email));

        if (!existe) {
            messageLabel.setStyle("-fx-text-fill: red; -fx-font-size: 12;");
            messageLabel.setText("Aucun compte associé à cet email !");
            return;
        }

        // Génère le token
        String token = EmailUtil.genererToken();

        messageLabel.setStyle("-fx-text-fill: orange; -fx-font-size: 12;");
        messageLabel.setText("Envoi en cours...");
        envoyerButton.setDisable(true);

        // Envoi dans un thread séparé pour ne pas bloquer l'UI
        new Thread(() -> {
            boolean envoye = EmailUtil.envoyerEmailReset(email, token);

            javafx.application.Platform.runLater(() -> {
                if (envoye) {
                    try {
                        FXMLLoader loader = new FXMLLoader(
                                getClass().getResource("/com/esprit/views/ResetPassword.fxml"));
                        Parent root = loader.load();
                        ResetPasswordController controller = loader.getController();
                        controller.setTokenEtEmail(
                                token.substring(0, 8).toUpperCase(), email);
                        Stage stage = (Stage) emailField.getScene().getWindow();
                        stage.setScene(new Scene(root));
                        stage.show();
                    } catch (Exception e) {
                        messageLabel.setStyle("-fx-text-fill: red; -fx-font-size: 12;");
                        messageLabel.setText("Erreur navigation : " + e.getMessage());
                        envoyerButton.setDisable(false);
                    }
                } else {
                    messageLabel.setStyle("-fx-text-fill: red; -fx-font-size: 12;");
                    messageLabel.setText("Erreur envoi email. Vérifiez votre connexion !");
                    envoyerButton.setDisable(false);
                }
            });
        }).start();
    }

    @FXML
    public void handleRetour() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/esprit/views/Login.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) emailField.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}