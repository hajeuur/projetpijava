package com.mentorai.controllers;

import com.mentorai.models.Feedback;
import com.mentorai.services.FeedbackService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.net.URL;
import java.time.LocalDate;
import java.util.ResourceBundle;

public class AjouterFeedbackController implements Initializable {

    @FXML private ComboBox<String> comboType;
    @FXML private TextArea textMessage;
    @FXML private Label labelNote;
    @FXML private Label labelResultat;

    @FXML private Button star1, star2, star3, star4, star5;
    @FXML private Label erreurType, erreurNote, erreurMessage;

    private int noteSelectionnee = 0;
    private int utilisateurIdConnecte = 11; // user existant dans ta BDD

    private FeedbackService feedbackService = new FeedbackService();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // ✅ CORRECTION : items + taille forcée
        comboType.setItems(FXCollections.observableArrayList(
                "probleme",
                "satisfaction",
                "suggestion"
        ));
        comboType.setMaxWidth(Double.MAX_VALUE);
    }

    // ===== ÉTOILES =====
    @FXML private void noterUn()     { setNote(1); }
    @FXML private void noterDeux()   { setNote(2); }
    @FXML private void noterTrois()  { setNote(3); }
    @FXML private void noterQuatre() { setNote(4); }
    @FXML private void noterCinq()   { setNote(5); }

    private void setNote(int note) {
        noteSelectionnee = note;
        labelNote.setText("(" + note + "/5)");
        erreurNote.setText("");

        Button[] etoiles = {star1, star2, star3, star4, star5};
        for (int i = 0; i < 5; i++) {
            String couleur = (i < note) ? "#f0a500" : "#ccc";
            etoiles[i].setStyle(
                    "-fx-background-color: transparent; -fx-text-fill: " + couleur + ";" +
                            "-fx-font-size: 24px; -fx-cursor: hand; -fx-padding: 0;"
            );
        }
    }

    // ===== ENVOYER =====
    @FXML
    private void envoyerFeedback() {
        erreurType.setText("");
        erreurNote.setText("");
        erreurMessage.setText("");
        labelResultat.setText("");

        boolean valide = true;

        if (comboType.getValue() == null) {
            erreurType.setText("⚠️ Veuillez choisir un type de feedback.");
            valide = false;
        }

        if (noteSelectionnee == 0) {
            erreurNote.setText("⚠️ Veuillez donner une note (1 à 5 étoiles).");
            valide = false;
        }

        String message = textMessage.getText().trim();
        if (message.isEmpty()) {
            erreurMessage.setText("⚠️ Veuillez écrire un message.");
            valide = false;
        } else if (message.length() < 10) {
            erreurMessage.setText("⚠️ Le message doit contenir au moins 10 caractères.");
            valide = false;
        } else if (message.length() > 1000) {
            erreurMessage.setText("⚠️ Le message ne peut pas dépasser 1000 caractères.");
            valide = false;
        }

        if (!valide) return;

        Feedback feedback = new Feedback(
                message,
                noteSelectionnee,
                LocalDate.now(),
                comboType.getValue(),
                "en_attente",
                0,   // traitement_id = 0 → sera converti en NULL dans le service
                utilisateurIdConnecte
        );

        feedbackService.add(feedback);

        labelResultat.setStyle(
                "-fx-text-fill: #28a745; -fx-font-size: 13px; -fx-font-weight: bold;"
        );
        labelResultat.setText("✅ Votre feedback a été envoyé avec succès !");

        // ✅ CORRECTION reset propre
        comboType.setValue(null);
        textMessage.clear();
        noteSelectionnee = 0;
        labelNote.setText("(0/5)");
        Button[] etoiles = {star1, star2, star3, star4, star5};
        for (Button e : etoiles) {
            e.setStyle(
                    "-fx-background-color: transparent; -fx-text-fill: #ccc;" +
                            "-fx-font-size: 24px; -fx-cursor: hand; -fx-padding: 0;"
            );
        }
    }

    // ===== NAVIGATION =====
    @FXML
    private void voirMesFeedbacks() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/MesFeedbacks.fxml")
            );
            VBox root = loader.load();

            MesFeedbacksController ctrl = loader.getController();
            ctrl.setUtilisateurId(utilisateurIdConnecte);

            Stage stage = (Stage) comboType.getScene().getWindow();
            stage.setTitle("MentorAI - Mes Feedbacks");
            stage.setScene(new Scene(root));
        } catch (Exception e) {
            System.out.println("❌ Erreur navigation : " + e.getMessage());
            e.printStackTrace();
        }
    }
}