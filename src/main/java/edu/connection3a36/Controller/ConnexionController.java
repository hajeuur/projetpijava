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
                // Stocker la session
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

                // Rediriger
                FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlToLoad));
                Parent root = loader.load();
                Stage stage = (Stage) txtEmail.getScene().getWindow();
                stage.setTitle(title);

                // Correction erreur disposition de fenetres (toujours extensible, bien centrée)
                Scene scene = new Scene(root, 1200, 800);
                stage.setScene(scene);
                stage.setMaximized(true);
                stage.setResizable(true);
                stage.setMinWidth(1000);
                stage.setMinHeight(700);
                stage.centerOnScreen();

            } else {
                lblErreur.setText("❌ Identifiants incorrects. Testez admin@mentor.com ou etudiant@mentor.com.");
            }
        } catch (Exception e) {
            lblErreur.setText("❌ Erreur de base de données : " + e.getMessage());
            e.printStackTrace();
        }
    }
}
