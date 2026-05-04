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
import java.util.ResourceBundle;

public class ModifierFeedbackController implements Initializable {

    @FXML private ComboBox<String> comboType;
    @FXML private TextArea textMessage;
    @FXML private Label labelNote, labelResultat;
    @FXML private Label erreurType, erreurNote, erreurMessage;
    @FXML private Button star1, star2, star3, star4, star5;

    private int noteSelectionnee = 0;
    private Feedback feedbackAModifier;
    private MesFeedbacksController parentController;

    private FeedbackService feedbackService = new FeedbackService();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        comboType.setItems(FXCollections.observableArrayList(
                "probleme", "satisfaction", "suggestion"
        ));
        comboType.setMaxWidth(Double.MAX_VALUE);
    }

    // Appelé depuis MesFeedbacksController
    public void setFeedback(Feedback feedback, MesFeedbacksController parent) {
        this.feedbackAModifier = feedback;
        this.parentController = parent;

        // Pré-remplir les champs avec les données existantes
        comboType.setValue(feedback.getTypefeedback());
        textMessage.setText(feedback.getContenu());
        setNote(feedback.getNote());
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
        Button[] etoiles = {star1, star2, star3, star4, star5};
        for (int i = 0; i < 5; i++) {
            String couleur = (i < note) ? "#f0a500" : "#ccc";
            etoiles[i].setStyle(
                    "-fx-background-color: transparent; -fx-text-fill: " + couleur + ";" +
                            "-fx-font-size: 24px; -fx-cursor: hand; -fx-padding: 0;"
            );
        }
    }

    // ===== ENREGISTRER =====
    @FXML
    private void enregistrer() {
        erreurType.setText("");
        erreurNote.setText("");
        erreurMessage.setText("");

        boolean valide = true;

        if (comboType.getValue() == null) {
            erreurType.setText("⚠️ Veuillez choisir un type.");
            valide = false;
        }
        if (noteSelectionnee == 0) {
            erreurNote.setText("⚠️ Veuillez donner une note.");
            valide = false;
        }
        String message = textMessage.getText().trim();
        if (message.isEmpty()) {
            erreurMessage.setText("⚠️ Veuillez écrire un message.");
            valide = false;
        } else if (message.length() < 10) {
            erreurMessage.setText("⚠️ Minimum 10 caractères.");
            valide = false;
        } else if (message.length() > 1000) {
            erreurMessage.setText("⚠️ Maximum 1000 caractères.");
            valide = false;
        }

        if (!valide) return;

        // Mettre à jour l'objet feedback
        feedbackAModifier.setTypefeedback(comboType.getValue());
        feedbackAModifier.setNote(noteSelectionnee);
        feedbackAModifier.setContenu(message);

        feedbackService.update(feedbackAModifier);

        labelResultat.setStyle(
                "-fx-text-fill: #28a745; -fx-font-size: 13px; -fx-font-weight: bold;"
        );
        labelResultat.setText("✅ Feedback modifié avec succès !");

        // Retourner à la liste après 1 seconde
        new Thread(() -> {
            try {
                Thread.sleep(1000);
                javafx.application.Platform.runLater(this::retour);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    // ===== RETOUR =====
    @FXML
    private void retour() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/MesFeedbacks.fxml")
            );
            VBox root = loader.load();
            MesFeedbacksController ctrl = loader.getController();
            ctrl.setUtilisateurId(feedbackAModifier.getUtilisateurId());

            edu.connection3a36.controllers.MainController.getInstance().loadInContentArea(root);
        } catch (Exception e) {
            System.out.println("❌ Erreur retour : " + e.getMessage());
        }
    }
}