package com.mentorai.controllers;

import com.mentorai.models.Feedback;
import com.mentorai.models.Traitement;
import com.mentorai.services.EmailService;
import com.mentorai.services.FeedbackService;
import com.mentorai.services.GeminiService;
import com.mentorai.services.TraitementService;
import com.mentorai.services.UtilisateurService;
import javafx.application.Platform;
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
    @FXML private Label labelSousTitre;
    @FXML private ComboBox<String> comboType;
    @FXML private TextArea textDescription;
    @FXML private Label labelErreur;
    @FXML private Button btnGemini;
    @FXML private Label labelGeminiStatus;

    private Feedback feedbackATraiter;
    private AdminFeedbackController adminController;

    private FeedbackService     feedbackService     = new FeedbackService();
    private TraitementService   traitementService   = new TraitementService();
    private GeminiService       geminiService       = new GeminiService();
    private EmailService        emailService        = new EmailService();
    private UtilisateurService  utilisateurService  = new UtilisateurService();

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
        this.adminController  = adminCtrl;
        labelSousTitre.setText(
                "Feedback #" + feedback.getId() + " — " + feedback.getTypefeedback()
        );
        labelInfoFeedback.setText(
                "Type : " + feedback.getTypefeedback() +
                        "  |  Note : " + feedback.getNote() + "/5" +
                        "  |  Date : " + feedback.getDatefeedback() +
                        "\n\nMessage : " + feedback.getContenu()
        );
    }

    // ✅ GEMINI
    @FXML
    private void suggererAvecGemini() {
        if (feedbackATraiter == null) return;
        btnGemini.setDisable(true);
        btnGemini.setText("Analyse en cours...");
        labelGeminiStatus.setText("Gemini analyse le feedback...");
        labelGeminiStatus.setStyle("-fx-text-fill: #f0a500; -fx-font-size: 11px;");

        new Thread(() -> {
            String suggestion = geminiService.suggererReponse(
                    feedbackATraiter.getTypefeedback(),
                    String.valueOf(feedbackATraiter.getNote()),
                    feedbackATraiter.getContenu()
            );
            Platform.runLater(() -> {
                btnGemini.setDisable(false);
                btnGemini.setText("Suggerer avec Gemini IA");
                if (suggestion != null && !suggestion.isEmpty()) {
                    textDescription.setText(suggestion);
                    labelGeminiStatus.setText("Reponse intelligente generee ! Vous pouvez la modifier.");
                    labelGeminiStatus.setStyle(
                            "-fx-text-fill: #28a745; -fx-font-size: 11px; -fx-font-weight: bold;"
                    );
                } else {
                    labelGeminiStatus.setText("Erreur systeme. Ecrivez la reponse manuellement.");
                    labelGeminiStatus.setStyle("-fx-text-fill: #d52e28; -fx-font-size: 11px;");
                }
            });
        }).start();
    }

    @FXML
    private void confirmer() {
        labelErreur.setText("");

        if (comboType.getValue() == null) {
            labelErreur.setText("Veuillez choisir un type de traitement.");
            return;
        }
        String description = textDescription.getText().trim();
        if (description.isEmpty()) {
            labelErreur.setText("Veuillez ecrire une reponse.");
            return;
        }
        if (description.length() < 10) {
            labelErreur.setText("La reponse doit contenir au moins 10 caracteres.");
            return;
        }
        if (description.length() > 1000) {
            labelErreur.setText("La reponse ne peut pas depasser 1000 caracteres.");
            return;
        }

        // ===== CRÉER TRAITEMENT =====
        Traitement traitement = new Traitement(
                comboType.getValue(), description,
                LocalDate.now(), comboType.getValue()
        );
        traitementService.add(traitement);
        int idNouveauTraitement = traitementService.getDernierIdInsere();

        // ===== METTRE À JOUR FEEDBACK =====
        feedbackATraiter.setEtatfeedback("traite");
        feedbackATraiter.setTraitementId(idNouveauTraitement);
        feedbackService.update(feedbackATraiter);

        // ✅ ENVOYER EMAIL dans un thread séparé
        new Thread(() -> {
            String email = utilisateurService.getEmailById(
                    feedbackATraiter.getUtilisateurId()
            );
            String nomPrenom = utilisateurService.getNomPrenomById(
                    feedbackATraiter.getUtilisateurId()
            );

            if (email != null) {
                boolean envoye = emailService.envoyerEmailTraitement(
                        email,
                        nomPrenom,
                        feedbackATraiter.getTypefeedback(),
                        feedbackATraiter.getContenu(),
                        comboType.getValue(),
                        description
                );
                if (envoye) {
                    System.out.println("Email envoye a : " + email);
                } else {
                    System.out.println("Echec envoi email a : " + email);
                }
            }
        }).start();

        if (adminController != null) adminController.chargerDonnees();
        fermerPopup();
    }

    @FXML
    private void annuler() { fermerPopup(); }

    private void fermerPopup() {
        ((Stage) comboType.getScene().getWindow()).close();
    }
}