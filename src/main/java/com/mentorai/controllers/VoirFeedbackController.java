package com.mentorai.controllers;

import com.mentorai.models.Feedback;
import com.mentorai.models.Traitement;
import com.mentorai.services.TraitementService;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class VoirFeedbackController {

    @FXML private Label labelTitre;
    @FXML private Label labelSousTitre;
    @FXML private Label labelEtat;
    @FXML private Label labelUtilisateur;
    @FXML private Label labelType;
    @FXML private Label labelEtoiles;
    @FXML private Label labelNoteChiffre;
    @FXML private Label labelDate;
    @FXML private Label labelMessage;
    @FXML private Label labelTypeTraitement;
    @FXML private Label labelReponse;
    @FXML private Label labelDateTraitement;
    @FXML private VBox cardReponse;
    @FXML private VBox cardEnAttente;

    private TraitementService traitementService = new TraitementService();

    public void setFeedback(Feedback feedback) {
        labelTitre.setText("Détails du Feedback #" + feedback.getId());
        labelSousTitre.setText("Feedback #" + feedback.getId() + " — " + feedback.getDatefeedback());

        boolean traite = feedback.getEtatfeedback().equals("traite");
        labelEtat.setText(traite ? "✅ Traité" : "⏳ En attente");
        labelEtat.setStyle(
                "-fx-background-color: " + (traite ? "#28a745" : "#f0a500") + ";" +
                        "-fx-text-fill: white; -fx-padding: 5 14 5 14; -fx-background-radius: 20;" +
                        "-fx-font-weight: bold; -fx-font-size: 12px;"
        );

        labelUtilisateur.setText("Utilisateur ID : " + feedback.getUtilisateurId());

        String couleur = switch (feedback.getTypefeedback()) {
            case "probleme"     -> "#d52e28";
            case "satisfaction" -> "#28a745";
            case "suggestion"   -> "#9dbbce";
            default             -> "#888";
        };
        labelType.setText(feedback.getTypefeedback());
        labelType.setStyle(
                "-fx-background-color: " + couleur + "; -fx-text-fill: white;" +
                        "-fx-padding: 3 10 3 10; -fx-background-radius: 10;" +
                        "-fx-font-size: 11px; -fx-font-weight: bold;"
        );

        int note = feedback.getNote();
        labelEtoiles.setText("★".repeat(note) + "☆".repeat(5 - note));
        labelNoteChiffre.setText("(" + note + "/5)");
        labelDate.setText(feedback.getDatefeedback().toString());
        labelMessage.setText(feedback.getContenu());

        if (feedback.getTraitementId() != 0) {
            Traitement t = traitementService.getById(feedback.getTraitementId());
            if (t != null) {
                cardReponse.setVisible(true);
                cardReponse.setManaged(true);
                cardEnAttente.setVisible(false);
                cardEnAttente.setManaged(false);
                labelTypeTraitement.setText(t.getTypetraitement());
                labelReponse.setText(t.getDescription());
                labelDateTraitement.setText(t.getDatetraitement().toString());
                return;
            }
        }
        cardReponse.setVisible(false);
        cardReponse.setManaged(false);
        cardEnAttente.setVisible(true);
        cardEnAttente.setManaged(true);
    }

    @FXML
    private void fermer() {
        ((Stage) labelTitre.getScene().getWindow()).close();
    }
}