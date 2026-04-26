package edu.connection3a36.Controller;

import edu.connection3a36.entities.Utilisateur;
import edu.connection3a36.services.UtilisateurService;
import edu.connection3a36.tools.SessionManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;

public class ConnexionController {

    @FXML
    private TextField txtEmail;
    @FXML
    private PasswordField txtMdp;
    @FXML
    private Label lblErreur;

    private final UtilisateurService utilisateurService = new UtilisateurService();

    @FXML
    private void seConnecter() {
        lblErreur.setText("");
        String email = txtEmail.getText().trim();
        String mdp = txtMdp.getText().trim();

        if (email.isEmpty() || mdp.isEmpty()) {
            lblErreur.setText("❌ Veuillez remplir tous les champs.");
            return;
        }

        try {
            Utilisateur user = utilisateurService.login(email, mdp);
            if (user != null) {
                System.out.println("✅ Connexion réussie pour : " + user.getEmail());
                SessionManager.getInstance().setCurrentUser(user);

                String fxmlToLoad;
                String title;
                if ("ROLE_ADMIN".equalsIgnoreCase(user.getRole()) || "ADMIN".equalsIgnoreCase(user.getRole())) {
                    fxmlToLoad = "/Dashboard.fxml";
                    title = "MentorAI - Back-Office (Admin)";
                } else {
                    fxmlToLoad = "/FrontLayout.fxml";
                    title = "MentorAI - Espace Étudiant";
                }

                FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlToLoad));
                Parent root = loader.load();
                Stage stage = (Stage) txtEmail.getScene().getWindow();
                stage.setTitle(title);

                Scene scene = new Scene(root);
                stage.setScene(scene);

                // Forcer le plein écran (Maximisé)
                stage.setMaximized(true);
                stage.show();
                
                // Centrage de sécurité
                stage.centerOnScreen();
            } else {
                lblErreur.setText("❌ Identifiants incorrects.");
                System.out.println("⚠️ Échec de connexion : identifiants non reconnus.");
            }
        } catch (Exception e) {
            lblErreur.setText("❌ Erreur : " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void loginArslen() {
        System.out.println("🚀 Tentative Connexion Rapide : Arslen");
        txtEmail.setText("arslene.amira@gmail.com");
        txtMdp.setText("arslen");
        seConnecter();
    }

    @FXML
    private void loginAdmin() {
        System.out.println("🚀 Tentative Connexion Rapide : Admin");
        txtEmail.setText("admin");
        txtMdp.setText("12345678910");
        seConnecter();
    }
}
