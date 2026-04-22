package com.esprit.controllers;

import com.esprit.dao.UtilisateurDAO;
import com.esprit.models.Utilisateur;
import com.esprit.utils.GoogleAuthService;
import com.esprit.utils.GoogleUserInfo;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LoginController {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;
    @FXML private Button googleLoginButton;

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

        if (u.getStatus().equals("desactiver")) {
            errorLabel.setText("Votre compte est désactivé. Contactez l'administrateur !");
            return;
        }

        redirectByRole(u);
    }

    @FXML
    public void handleGoogleLogin() {
        errorLabel.setText("Ouverture du navigateur Google...");
        if (googleLoginButton != null) googleLoginButton.setDisable(true);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            try {
                System.out.println(">>> Début authentification Google...");
                GoogleUserInfo googleUser = GoogleAuthService.authenticate();
                System.out.println(">>> Authentification OK : " + googleUser);

                if (googleUser == null || googleUser.getEmail() == null) {
                    Platform.runLater(() -> errorLabel.setText("Authentification Google échouée."));
                    return;
                }

                System.out.println(">>> Recherche en base : " + googleUser.getEmail());
                Utilisateur existing = dao.findByEmail(googleUser.getEmail());
                System.out.println(">>> Résultat findByEmail : " + (existing != null ? "TROUVÉ id=" + existing.getId() : "NON TROUVÉ"));

                if (existing != null) {
                    System.out.println(">>> Status : " + existing.getStatus());
                    if (existing.getStatus().equals("desactiver")) {
                        Platform.runLater(() ->
                                errorLabel.setText("Votre compte est désactivé. Contactez l'administrateur !"));
                        return;
                    }
                    System.out.println(">>> Redirection vers rôle : " + existing.getRole());
                    Platform.runLater(() -> redirectByRole(existing));

                } else {
                    System.out.println(">>> Création nouveau compte Google...");
                    Utilisateur nouveau = new Utilisateur();
                    nouveau.setEmail(googleUser.getEmail());
                    nouveau.setNom(googleUser.getNom()    != null ? googleUser.getNom()    : "");
                    nouveau.setPrenom(googleUser.getPrenom() != null ? googleUser.getPrenom() : "");
                    nouveau.setMdp("");
                    nouveau.setRole("etudiant");
                    nouveau.setStatus("activer");

                    dao.ajouter(nouveau);
                    System.out.println(">>> Compte créé !");

                    Utilisateur created = dao.findByEmail(googleUser.getEmail());
                    System.out.println(">>> Compte récupéré : " + (created != null ? "id=" + created.getId() : "NULL !"));
                    Platform.runLater(() -> redirectByRole(created));
                }

            } catch (Exception e) {
                System.out.println(">>> ERREUR GOOGLE : " + e.getClass().getName() + " : " + e.getMessage());
                e.printStackTrace();
                Platform.runLater(() -> errorLabel.setText(e.getClass().getSimpleName() + ": " + e.getMessage()));
            } finally {
                Platform.runLater(() -> {
                    if (googleLoginButton != null) googleLoginButton.setDisable(false);
                });
                executor.shutdown();
            }
        });
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

    @FXML
    public void handleForgetPassword() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/esprit/views/ForgetPassword.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) emailField.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            errorLabel.setText("Erreur : " + e.getMessage());
        }
    }

    private void redirectByRole(Utilisateur u) {
        try {
            if (u == null) {
                System.out.println(">>> ERREUR: utilisateur NULL dans redirectByRole !");
                errorLabel.setText("Erreur : utilisateur introuvable.");
                return;
            }
            String role = u.getRole();
            System.out.println(">>> redirectByRole : role=" + role + " id=" + u.getId());

            String fxmlPath = role.equals("admin")
                    ? "/com/esprit/views/BackOffice.fxml"
                    : "/com/esprit/views/FrontOffice.fxml";

            System.out.println(">>> Chargement FXML : " + fxmlPath);
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            System.out.println(">>> FXML chargé !");

            if (!role.equals("admin")) {
                FrontOfficeController controller = loader.getController();
                controller.setUtilisateur(u);
            }

            Stage stage = (Stage) emailField.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
            System.out.println(">>> Redirection effectuée !");

        } catch (Exception e) {
            System.out.println(">>> ERREUR redirectByRole : " + e.getClass() + " : " + e.getMessage());
            e.printStackTrace();
            errorLabel.setText("Erreur navigation : " + e.getMessage());
        }
    }
}