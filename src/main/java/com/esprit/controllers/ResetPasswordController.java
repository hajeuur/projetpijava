package com.esprit.controllers;

import com.esprit.dao.UtilisateurDAO;
import com.esprit.models.Utilisateur;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

public class ResetPasswordController {

    @FXML private TextField codeField;
    @FXML private PasswordField newMdpField;
    @FXML private PasswordField confirmMdpField;
    @FXML private Label messageLabel;

    private String tokenAttendu;
    private String emailUtilisateur;
    private final UtilisateurDAO dao = new UtilisateurDAO();

    public void setTokenEtEmail(String token, String email) {
        this.tokenAttendu = token;
        this.emailUtilisateur = email;
    }

    @FXML
    public void handleReset() {
        String code = codeField.getText().trim().toUpperCase();
        String newMdp = newMdpField.getText().trim();
        String confirmMdp = confirmMdpField.getText().trim();

        messageLabel.setStyle("-fx-font-size: 12;");
        messageLabel.setText("");

        // Validations
        if (code.isEmpty() || newMdp.isEmpty() || confirmMdp.isEmpty()) {
            messageLabel.setStyle("-fx-text-fill: red; -fx-font-size: 12;");
            messageLabel.setText("Veuillez remplir tous les champs !");
            return;
        }

        if (!code.equals(tokenAttendu)) {
            messageLabel.setStyle("-fx-text-fill: red; -fx-font-size: 12;");
            messageLabel.setText("Code incorrect ! Vérifiez votre email.");
            return;
        }

        if (newMdp.length() < 8) {
            messageLabel.setStyle("-fx-text-fill: red; -fx-font-size: 12;");
            messageLabel.setText("Le mot de passe doit contenir au moins 8 caractères !");
            return;
        }

        if (!newMdp.equals(confirmMdp)) {
            messageLabel.setStyle("-fx-text-fill: red; -fx-font-size: 12;");
            messageLabel.setText("Les mots de passe ne correspondent pas !");
            return;
        }

        // Trouve l'utilisateur par email
        List<Utilisateur> tous = dao.getAll();
        Utilisateur utilisateur = tous.stream()
                .filter(u -> u.getEmail().equalsIgnoreCase(emailUtilisateur))
                .findFirst()
                .orElse(null);

        if (utilisateur == null) {
            messageLabel.setStyle("-fx-text-fill: red; -fx-font-size: 12;");
            messageLabel.setText("Utilisateur introuvable !");
            return;
        }

        // Hache le nouveau mot de passe et met à jour
        String newMdpHache = hashPassword(newMdp);
        utilisateur.setMdp(newMdpHache);
        dao.modifier(utilisateur);

        messageLabel.setStyle("-fx-text-fill: green; -fx-font-size: 12;");
        messageLabel.setText("Mot de passe réinitialisé avec succès !");

        // Retour au login après 2 secondes
        new Thread(() -> {
            try {
                Thread.sleep(2000);
                javafx.application.Platform.runLater(() -> {
                    try {
                        FXMLLoader loader = new FXMLLoader(
                                getClass().getResource("/com/esprit/views/Login.fxml"));
                        Parent root = loader.load();
                        Stage stage = (Stage) codeField.getScene().getWindow();
                        stage.setScene(new Scene(root));
                        stage.show();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
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

    @FXML
    public void handleRetour() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/esprit/views/Login.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) codeField.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}