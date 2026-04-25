package com.mentorai.controllers;

import com.mentorai.models.Feedback;
import com.mentorai.models.Traitement;
import com.mentorai.services.FeedbackService;
import com.mentorai.services.TraitementService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;

public class ModifierTraitementController implements Initializable {

    @FXML private Label labelSousTitre;
    @FXML private Label labelTypeFeedback;
    @FXML private Label labelNoteFeedback;
    @FXML private Label labelDateFeedback;
    @FXML private Label labelMessageFeedback;
    @FXML private Label labelDateCreation;
    @FXML private ComboBox<String> comboType;
    @FXML private TextArea textDescription;
    @FXML private Label labelErreur;

    // ✅ SUPPRIMÉ : comboDecision (plus nécessaire)

    private Feedback feedbackConcerne;
    private Traitement traitementAModifier;
    private AdminFeedbackController adminController;

    private TraitementService traitementService = new TraitementService();
    private FeedbackService feedbackService = new FeedbackService();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        comboType.setItems(FXCollections.observableArrayList(
                "geste_commercial",
                "remboursement",
                "prolongation_abonnement",
                "reponse_simple"
        ));
        comboType.setMaxWidth(Double.MAX_VALUE);
    }

    public void setFeedback(Feedback feedback, AdminFeedbackController adminCtrl) {
        this.feedbackConcerne = feedback;
        this.adminController = adminCtrl;

        // ✅ Header sous-titre
        labelSousTitre.setText("Feedback #" + feedback.getId());

        // ✅ Colonne gauche — info feedback
        labelTypeFeedback.setText(feedback.getTypefeedback());

        // Couleur du badge selon le type
        String couleur = switch (feedback.getTypefeedback()) {
            case "probleme"     -> "#d52e28";
            case "satisfaction" -> "#28a745";
            case "suggestion"   -> "#9dbbce";
            default             -> "#888";
        };
        labelTypeFeedback.setStyle(
                "-fx-background-color: " + couleur + "; -fx-text-fill: white;" +
                        "-fx-padding: 2 8 2 8; -fx-background-radius: 10;" +
                        "-fx-font-size: 11px; -fx-font-weight: bold;"
        );

        labelNoteFeedback.setText(feedback.getNote() + "/5");
        labelDateFeedback.setText(feedback.getDatefeedback().toString());
        labelMessageFeedback.setText(feedback.getContenu());

        // ✅ Pré-remplir avec le traitement existant
        if (feedback.getTraitementId() != 0) {
            traitementAModifier = traitementService.getById(feedback.getTraitementId());
            if (traitementAModifier != null) {
                comboType.setValue(traitementAModifier.getTypetraitement());
                textDescription.setText(traitementAModifier.getDescription());
                labelDateCreation.setText(
                        "Date de création : " + traitementAModifier.getDatetraitement()
                );
            }
        }
    }

    @FXML
    private void enregistrer() {
        labelErreur.setText("");

        // ===== CONTRÔLE DE SAISIE =====
        if (comboType.getValue() == null) {
            labelErreur.setText("⚠️ Veuillez choisir un type de traitement.");
            return;
        }

        String description = textDescription.getText().trim();
        if (description.isEmpty()) {
            labelErreur.setText("⚠️ Veuillez écrire une réponse.");
            return;
        }
        if (description.length() < 10) {
            labelErreur.setText("⚠️ La réponse doit contenir au moins 10 caractères.");
            return;
        }
        if (description.length() > 1000) {
            labelErreur.setText("⚠️ La réponse ne peut pas dépasser 1000 caractères.");
            return;
        }

        // ===== ENREGISTRER =====
        if (traitementAModifier != null) {
            traitementAModifier.setTypetraitement(comboType.getValue());
            traitementAModifier.setDescription(description);
            // ✅ décision = type (plus de comboDecision)
            traitementAModifier.setDecision(comboType.getValue());
            traitementService.update(traitementAModifier);
        }

        if (adminController != null) adminController.chargerDonnees();
        fermer();
    }

    @FXML
    private void annuler() { fermer(); }

    private void fermer() {
        ((Stage) comboType.getScene().getWindow()).close();
    }
}