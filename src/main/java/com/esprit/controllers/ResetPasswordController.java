package com.esprit.controllers;

import com.esprit.models.Utilisateur;
import com.esprit.services.UtilisateurService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.util.List;

public class ResetPasswordController {

    @FXML private TextField codeField;
    @FXML private PasswordField newMdpField;
    @FXML private PasswordField confirmMdpField;
    @FXML private Label messageLabel;

    private String tokenAttendu;
    private String emailUtilisateur;
    private final UtilisateurService service = new UtilisateurService();

    public void setTokenEtEmail(String token, String email) {
        this.tokenAttendu   = token;
        this.emailUtilisateur = email;
    }

    @FXML
    public void handleReset() {
        String code       = codeField.getText().trim().toUpperCase();
        String newMdp     = newMdpField.getText().trim();
        String confirmMdp = confirmMdpField.getText().trim();

        messageLabel.setStyle("-fx-font-size: 12;");
        messageLabel.setText("");

        if (code.isEmpty() || newMdp.isEmpty() || confirmMdp.isEmpty()) {
            messageLabel.setStyle("-fx-text-fill: red; -fx-font-size: 12;");
            messageLabel.setText("Veuillez remplir tous les champs !"); return;
        }
        if (!code.equals(tokenAttendu)) {
            messageLabel.setStyle("-fx-text-fill: red; -fx-font-size: 12;");
            messageLabel.setText("Code incorrect ! Vérifiez votre email."); return;
        }
        if (!service.mdpValide(newMdp)) {
            messageLabel.setStyle("-fx-text-fill: red; -fx-font-size: 12;");
            messageLabel.setText("Le mot de passe doit contenir au moins 8 caractères !"); return;
        }
        if (!newMdp.equals(confirmMdp)) {
            messageLabel.setStyle("-fx-text-fill: red; -fx-font-size: 12;");
            messageLabel.setText("Les mots de passe ne correspondent pas !"); return;
        }

        // Trouve l'utilisateur par email via le service
        List<Utilisateur> tous = service.getAll();
        Utilisateur utilisateur = tous.stream()
                .filter(u -> u.getEmail().equalsIgnoreCase(emailUtilisateur))
                .findFirst().orElse(null);

        if (utilisateur == null) {
            messageLabel.setStyle("-fx-text-fill: red; -fx-font-size: 12;");
            messageLabel.setText("Utilisateur introuvable !"); return;
        }

        utilisateur.setMdp(service.hashPassword(newMdp));
        service.modifier(utilisateur);

        messageLabel.setStyle("-fx-text-fill: green; -fx-font-size: 12;");
        messageLabel.setText("Mot de passe réinitialisé avec succès !");

        new Thread(() -> {
            try {
                Thread.sleep(2000);
                javafx.application.Platform.runLater(() -> {
                    try {
                        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/esprit/views/Login.fxml"));
                        Parent root = loader.load();
                        Stage stage = (Stage) codeField.getScene().getWindow();
                        stage.setScene(new Scene(root));
                        stage.show();
                    } catch (Exception e) { e.printStackTrace(); }
                });
            } catch (InterruptedException e) { e.printStackTrace(); }
        }).start();
    }

    @FXML
    public void handleRetour() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/esprit/views/Login.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) codeField.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) { e.printStackTrace(); }
    }
}