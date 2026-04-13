package edu.connection3a36.controllers;

import edu.connection3a36.entities.Utilisateur;
import edu.connection3a36.tools.AlertUtil;
import edu.connection3a36.tools.SessionManager;
import edu.connection3a36.tools.UserPreferences;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.stage.Stage;

/**
 * Contrôleur pour la vue Préférences.
 * Gère l'activation du Dark Mode, Grande Police et mode Dyslexique.
 */
public class PreferencesController {

    @FXML private CheckBox chkDarkMode;
    @FXML private CheckBox chkLargeText;
    @FXML private CheckBox chkDyslexic;
    @FXML private Label lblUserInfo;

    @FXML
    public void initialize() {
        // Charger les préférences actuelles
        chkDarkMode.setSelected(UserPreferences.isDarkMode());
        chkLargeText.setSelected(UserPreferences.isLargeText());
        chkDyslexic.setSelected(UserPreferences.isDyslexicMode());

        // Afficher les infos de l'utilisateur
        Utilisateur user = SessionManager.getCurrentUser();
        if (user != null) {
            String roleStr = switch(user.getRole()) {
                case "ADMINM" -> "Administrateur Système";
                case "ADMIN"  -> "Administrateur Simple";
                case "ENSEIGNANT" -> "Enseignant";
                case "USER" -> "Étudiant";
                default -> user.getRole();
            };

            lblUserInfo.setText(String.format("Connecté en tant que: %s %s\nEmail: %s\nRôle: %s",
                    user.getPrenom(), user.getNom(), user.getEmail(), roleStr));
        }
    }

    @FXML
    void handleSave() {
        // Sauvegarder dans le gestionnaire de préférences
        UserPreferences.setDarkMode(chkDarkMode.isSelected());
        UserPreferences.setLargeText(chkLargeText.isSelected());
        UserPreferences.setDyslexicMode(chkDyslexic.isSelected());

        // Appliquer immédiatement sur la scène courante
        UserPreferences.applyToScene(chkDarkMode.getScene());

        AlertUtil.showSuccess("✅ Préférences enregistrées avec succès !");
    }

    @FXML
    void handleReset() {
        chkDarkMode.setSelected(false);
        chkLargeText.setSelected(false);
        chkDyslexic.setSelected(false);
        handleSave();
    }

    @FXML
    void handleLogout() {
        if (AlertUtil.showConfirmation("Voulez-vous vraiment vous déconnecter ?")) {
            SessionManager.logout();

            try {
                // Retour à la page de connexion
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Login.fxml"));
                Parent root = loader.load();

                Stage stage = (Stage) chkDarkMode.getScene().getWindow();
                Scene sc = new Scene(root, 1200, 750);
                sc.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
                stage.setTitle("MentorAI — Connexion");
                stage.setScene(sc);
            } catch (Exception e) {
                e.printStackTrace();
                AlertUtil.showError("Erreur lors de la déconnexion : " + e.getMessage());
            }
        }
    }
}
