package com.mentorai.controllers;

import com.mentorai.models.Feedback;
import com.mentorai.models.Traitement;
import com.mentorai.services.EmailService;
import com.mentorai.services.FeedbackService;
import com.mentorai.services.GeminiService;
import com.mentorai.services.TraitementService;
import com.mentorai.services.UtilisateurService;
import com.mentorai.services.SentimentService;
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
    
    // Analyse de sentiment
    @FXML private Label labelSentimentPrincipal;
    @FXML private Label labelScorePositif;
    @FXML private Label labelScoreNeutre;
    @FXML private Label labelScoreNegatif;
    @FXML private Label labelRecommandation;
    @FXML private Label labelExplication;

    private Feedback feedbackATraiter;
    private AdminFeedbackController adminController;

    private FeedbackService     feedbackService     = new FeedbackService();
    private TraitementService   traitementService   = new TraitementService();
    private GeminiService       geminiService       = new GeminiService();
    private EmailService        emailService        = new EmailService();
    private UtilisateurService  utilisateurService  = new UtilisateurService();
    private SentimentService    sentimentService    = new SentimentService();

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
        
        // ✅ ANALYSE DE SENTIMENT AUTOMATIQUE
        analyserSentimentFeedback();
    }

    /**
     * Analyse le sentiment du feedback avec Azure IA
     */
    private void analyserSentimentFeedback() {
        if (feedbackATraiter == null) return;
        
        new Thread(() -> {
            java.util.Map<String, String> analyse = sentimentService.analyserSentimentDetaille(
                    feedbackATraiter.getContenu()
            );
            
            Platform.runLater(() -> {
                String sentiment = analyse.get("sentiment");
                String emoji = sentimentService.getEmojiSentiment(sentiment);
                String couleur = sentimentService.getCouleurSentiment(sentiment);
                
                labelSentimentPrincipal.setText(emoji + " " + sentiment.toUpperCase());
                labelSentimentPrincipal.setStyle(
                        "-fx-background-color: " + couleur + "; -fx-text-fill: white;" +
                        "-fx-padding: 8 16 8 16; -fx-background-radius: 20;" +
                        "-fx-font-size: 14px; -fx-font-weight: bold;" +
                        "-fx-effect: dropshadow(gaussian,rgba(0,0,0,0.2),4,0,0,2);"
                );
                
                labelScorePositif.setText(analyse.get("positif") + "%");
                labelScoreNeutre.setText(analyse.get("neutre") + "%");
                labelScoreNegatif.setText(analyse.get("negatif") + "%");
                labelRecommandation.setText(analyse.get("recommandation"));
                labelExplication.setText(analyse.get("explication"));
            });
        }).start();
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
        edu.connection3a36.controllers.MainController.getInstance().loadInContentArea("/fxml/AdminFeedback.fxml");
    }
}