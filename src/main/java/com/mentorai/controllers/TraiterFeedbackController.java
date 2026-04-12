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
import java.time.LocalDate;
import java.util.ResourceBundle;

public class TraiterFeedbackController implements Initializable {

    @FXML private Label labelInfoFeedback;
    @FXML private Label labelSousTitre;      // ✅ NOUVEAU
    @FXML private ComboBox<String> comboType;
    @FXML private TextArea textDescription;
    @FXML private Label labelErreur;

    // ✅ SUPPRIMÉ : comboDecision (plus dans le nouveau FXML)
    // La décision = même valeur que le type maintenant

    private Feedback feedbackATraiter;
    private AdminFeedbackController adminController;

    private FeedbackService feedbackService = new FeedbackService();
    private TraitementService traitementService = new TraitementService();

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
        this.feedbackATraiter = feedback;
        this.adminController = adminCtrl;

        // ✅ Sous-titre dans le header
        labelSousTitre.setText(
                "Feedback #" + feedback.getId() + " — " + feedback.getTypefeedback()
        );

        // ✅ Info détaillée dans la card
        labelInfoFeedback.setText(
                "Type : " + feedback.getTypefeedback() +
                        "  |  Note : " + feedback.getNote() + "/5" +
                        "  |  Date : " + feedback.getDatefeedback() +
                        "\n\nMessage : " + feedback.getContenu()
        );
    }

    @FXML
    private void confirmer() {
        // ===== VALIDATION =====
        labelErreur.setText("");

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

        // ===== CRÉER LE TRAITEMENT =====
        // ✅ La décision = même valeur que le type choisi
        Traitement traitement = new Traitement(
                comboType.getValue(),
                description,
                LocalDate.now(),
                comboType.getValue()  // décision = type
        );
        traitementService.add(traitement);

        int idNouveauTraitement = traitementService.getDernierIdInsere();

        // ===== METTRE À JOUR LE FEEDBACK =====
        feedbackATraiter.setEtatfeedback("traite");
        feedbackATraiter.setTraitementId(idNouveauTraitement);
        feedbackService.update(feedbackATraiter);

        System.out.println("✅ Traitement créé et feedback mis à jour !");

        if (adminController != null) {
            adminController.chargerDonnees();
        }

        fermerPopup();
    }

    @FXML
    private void annuler() {
        fermerPopup();
    }

    private void fermerPopup() {
        Stage stage = (Stage) comboType.getScene().getWindow();
        stage.close();
    }
}