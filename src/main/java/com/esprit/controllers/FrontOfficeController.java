package com.esprit.controllers;

import com.esprit.models.Utilisateur;
import com.esprit.services.UtilisateurService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;

public class FrontOfficeController implements Initializable {

    @FXML private Label     userNameLabel;
    @FXML private Label     welcomeLabel;
    @FXML private Label     emailInfoLabel;
    @FXML private Label     roleInfoLabel;
    @FXML private Label     nomCardLabel;
    @FXML private Label     emailCardLabel;
    @FXML private Label     roleCardLabel;
    @FXML private ImageView photoView;
    @FXML private Circle    photoClip;

    private Utilisateur utilisateur;
    private final UtilisateurService service = new UtilisateurService();

    @Override
    public void initialize(URL url, ResourceBundle rb) {}

    public void setUtilisateur(Utilisateur u) {
        this.utilisateur = u;
        userNameLabel.setText(u.getPrenom());
        welcomeLabel.setText("Bienvenue " + u.getPrenom() + " !");
        emailInfoLabel.setText("Vous êtes connecté en tant que " + u.getEmail());
        roleInfoLabel.setText("Connecté en tant que " + u.getRole());
        nomCardLabel.setText(u.getNom());
        emailCardLabel.setText(u.getEmail());
        roleCardLabel.setText(u.getRole());
        System.out.println(">>> pdpUrl = " + u.getPdpUrl());
        chargerPhoto(u.getPdpUrl());
    }

    /** Charge la photo depuis le chemin absolu ou affiche l'icône par défaut. */
    private void chargerPhoto(String pdpUrl) {
        if (photoView == null) return;
        Image img = null;

        if (pdpUrl != null && !pdpUrl.isBlank()) {
            try {
                File f = new File(pdpUrl); // new File() gère nativement les backslashes Windows
                if (f.exists()) {
                    String uri = f.toURI().toString(); // encode les espaces en %20 — correct pour JavaFX
                    System.out.println(">>> URI généré: " + uri);
                    img = new Image(uri, false); // false = chargement synchrone
                    if (img.isError()) {
                        System.out.println(">>> Erreur chargement image: " + img.getException());
                        img = null;
                    }
                } else {
                    System.out.println(">>> Fichier photo introuvable: " + pdpUrl);
                }
            } catch (Exception e) {
                System.out.println(">>> Exception chargerPhoto: " + e.getMessage());
                img = null;
            }
        }

        if (img == null) {
            // Image par défaut embarquée dans les resources
            URL defUrl = getClass().getResource("/com/esprit/views/default_avatar.png");
            if (defUrl != null) {
                try { img = new Image(defUrl.toExternalForm()); } catch (Exception ignored) {}
            }
        }

        if (img != null) {
            photoView.setImage(img);
            // Clip circulaire
            Circle clip = new Circle(55, 55, 55);
            photoView.setClip(clip);
        }
    }

    @FXML
    public void handleModifierPhoto() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Choisir une nouvelle photo de profil");
        fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images", "*.jpg", "*.jpeg", "*.png", "*.gif", "*.bmp"));
        Stage stage = (Stage) photoView.getScene().getWindow();
        File file = fc.showOpenDialog(stage);
        if (file != null) {
            String chemin = file.getAbsolutePath();
            utilisateur.setPdpUrl(chemin);
            service.modifier(utilisateur);          // sauvegarde en base
            chargerPhoto(chemin);                   // affichage immédiat
            System.out.println(">>> Photo mise à jour : " + chemin);
        }
    }

    @FXML
    public void handleMonProfil() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/esprit/views/ProfilUser.fxml"));
            Parent root = loader.load();
            ProfilUserController controller = loader.getController();
            controller.setUtilisateur(utilisateur);
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Mon profil");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void handleLogout() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/esprit/views/Login.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) userNameLabel.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
