package edu.connection3a36.controllers;

import edu.connection3a36.services.MockDataService;
import edu.connection3a36.services.PlanActionsService;
import edu.connection3a36.tools.SessionManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import javax.sound.sampled.*;
import java.io.IOException;
import java.util.*;

public class GamesHubController {

    @FXML private Button tabInterventions, tabDiagnostic, tabAccessibilite, tabImpact;
    @FXML private VBox paneInterventions, paneDiagnostic, paneAccessibilite, paneImpact;

    @FXML private ComboBox<String> cbNiveau;
    @FXML private ComboBox<String> cbProfil;
    @FXML private TextArea taIntervention;
    @FXML private TextArea taRemediation;
    @FXML private TextArea taScriptSeance;

    @FXML private ComboBox<String> cbQ1, cbQ2, cbQ3, cbQ4, cbQ5;
    @FXML private Label lblDiagScore;
    @FXML private TextArea taDiagRecommendation;

    @FXML private Button btnDyslexia;
    @FXML private Slider fontSizeSlider, letterSpacingSlider;
    @FXML private VBox previewBox;
    @FXML private Label previewText1, previewText2, previewText3;
    @FXML private ComboBox<String> cbAudioSymbol;
    @FXML private Label lblSoundFeedback;

    @FXML private Label lblPomodoroTime, lblPomodoroMode, lblPomodoroSessions, lblPomodoroMotivation;
    private Timer pomodoroTimer;
    private int pomodoroSeconds = 25 * 60;
    private boolean pomodoroWork = true, pomodoroRunning = false;
    private int pomodoroSessionsDone = 0;
    private static final String[] QUOTES = {
            "\"Progression > perfection.\"",
            "\"Une séance claire vaut mieux qu'une séance longue.\"",
            "\"Le suivi régulier bat les actions ponctuelles.\"",
            "\"Chaque feedback utile augmente l'impact pédagogique.\""
    };

    @FXML private Label lblAtRiskCount, lblMissingFeedback, lblLatePlans, lblImpactSummary;
    @FXML private TextArea taWeeklyActions, taImpactDetail;

    private String currentBg = "#fdf6e3";
    private boolean dyslexiaActive = false;
    private final PlanActionsService planService = new PlanActionsService();

    @FXML
    public void initialize() {
        cbNiveau.getItems().setAll("L1", "L2", "L3", "M1", "M2", "3A36");
        cbNiveau.setValue("3A36");
        cbProfil.getItems().setAll("À risque", "Dyslexie", "Attention", "Absentéisme", "Irrégulier", "Excellent", "Moyen");
        cbProfil.setValue("À risque");

        List<String> scale = Arrays.asList("Jamais", "Rarement", "Parfois", "Souvent");
        cbQ1.getItems().setAll(scale); cbQ2.getItems().setAll(scale); cbQ3.getItems().setAll(scale);
        cbQ4.getItems().setAll(scale); cbQ5.getItems().setAll(scale);
        cbQ1.setValue("Parfois"); cbQ2.setValue("Parfois"); cbQ3.setValue("Parfois"); cbQ4.setValue("Parfois"); cbQ5.setValue("Parfois");

        cbAudioSymbol.getItems().setAll("A", "E", "I", "O", "U", "MA", "PA", "TA", "KA", "LA");
        cbAudioSymbol.setValue("A");

        showPane(paneInterventions);
        activateTab(tabInterventions);
        applyPreview();
        updateTimerLabels();
        refreshImpact();
    }

    @FXML void showInterventions() { showPane(paneInterventions); activateTab(tabInterventions); }
    @FXML void showDiagnostic() { showPane(paneDiagnostic); activateTab(tabDiagnostic); }
    @FXML void showAccessibilite() { showPane(paneAccessibilite); activateTab(tabAccessibilite); }
    @FXML void showImpact() { showPane(paneImpact); activateTab(tabImpact); refreshImpact(); }

    private void showPane(VBox active) {
        for (VBox p : new VBox[]{paneInterventions, paneDiagnostic, paneAccessibilite, paneImpact}) {
            p.setVisible(p == active);
            p.setManaged(p == active);
        }
    }

    private void activateTab(Button active) {
        String on = "-fx-background-color:#3b82f6;-fx-text-fill:white;-fx-background-radius:20;-fx-padding:6 16;-fx-cursor:hand;";
        String off = "-fx-background-color:#e2e8f0;-fx-background-radius:20;-fx-padding:6 16;-fx-cursor:hand;";
        for (Button b : new Button[]{tabInterventions, tabDiagnostic, tabAccessibilite, tabImpact}) {
            b.setStyle(b == active ? on : off);
        }
    }

    @FXML
    void generateInterventionPack() {
        String niveau = cbNiveau.getValue();
        String profil = cbProfil.getValue();
        taIntervention.setText(generateMicroActivities(niveau, profil));
        taRemediation.setText(generateRemediationBank(profil));
        taScriptSeance.setText(generateSessionScript(niveau, profil));
    }

    private String generateMicroActivities(String niveau, String profil) {
        return "Objectif 15 min - " + profil + " (" + niveau + ")\n"
                + "1) 3 min: activation rapide (question d'entree ciblee).\n"
                + "2) 7 min: mini-atelier en binome avec consigne unique.\n"
                + "3) 3 min: restitution orale guidee.\n"
                + "4) 2 min: feedback flash (1 acquis, 1 blocage).\n\n"
                + "KPI de seance: taux de participation, nombre de reponses correctes, niveau d'engagement.";
    }

    private String generateRemediationBank(String profil) {
        String base = "Banque de remediation - profil " + profil + "\n";
        if ("Dyslexie".equalsIgnoreCase(profil)) {
            return base + "- Supports police lisible + interligne renforce.\n- Consignes segmentees en etapes.\n- Evaluation orale flash + trace ecrite courte.";
        }
        if ("Attention".equalsIgnoreCase(profil)) {
            return base + "- Tache unique de 5 minutes.\n- Rotation active toutes les 7 minutes.\n- Checkpoint verbal regulier enseignant-etudiant.";
        }
        if ("Absentéisme".equalsIgnoreCase(profil) || "À risque".equalsIgnoreCase(profil)) {
            return base + "- Contractualisation hebdomadaire (micro-objectifs).\n- Point parent/tuteur toutes les 2 semaines.\n- Rattrapage guide sur ressources essentielles.";
        }
        return base + "- Renforcement methodologique (prise de notes, revision active).\n- Pair tutoring.\n- Auto-evaluation de fin de seance.";
    }

    private String generateSessionScript(String niveau, String profil) {
        return "Script pret a l'emploi\n"
                + "Objectif: stabiliser les acquis du groupe " + niveau + " avec focus " + profil + ".\n"
                + "Deroule:\n"
                + "- 00:00-03:00: cadrage + consigne unique.\n"
                + "- 03:00-10:00: activite centrale par binomes.\n"
                + "- 10:00-13:00: correction collective guidee.\n"
                + "- 13:00-15:00: evaluation flash (2 questions + auto-positionnement).\n"
                + "Sortie attendue: 1 trace ecrite + 1 engagement d'action pour la prochaine seance.";
    }

    @FXML
    void runDiagnostic() {
        int score = scaleValue(cbQ1.getValue()) + scaleValue(cbQ2.getValue()) + scaleValue(cbQ3.getValue())
                + scaleValue(cbQ4.getValue()) + scaleValue(cbQ5.getValue());
        lblDiagScore.setText("Score diagnostic: " + score + " / 15");

        String recommendation;
        if (score >= 11) {
            recommendation = "Niveau de risque: FAIBLE\nAction: approfondissement + autonomie guidee.\n"
                    + "Plan: ajouter un mini-defi hebdomadaire et suivi toutes les 2 semaines.";
        } else if (score >= 7) {
            recommendation = "Niveau de risque: MODERE\nAction: remediation ciblee sur 2 axes.\n"
                    + "Plan: micro-activites 15 min x2/semaine + feedback enseignant systématique.";
        } else {
            recommendation = "Niveau de risque: ELEVE\nAction: intervention immediate multi-acteurs.\n"
                    + "Plan: suivi hebdomadaire, contact parent/tuteur, plan personnalise et re-evaluation sous 10 jours.";
        }
        taDiagRecommendation.setText(recommendation);
    }

    private int scaleValue(String value) {
        if ("Jamais".equals(value)) return 0;
        if ("Rarement".equals(value)) return 1;
        if ("Parfois".equals(value)) return 2;
        return 3;
    }

    @FXML void setBgCream()  { currentBg = "#fdf6e3"; applyPreview(); }
    @FXML void setBgGreen()  { currentBg = "#e8f5e9"; applyPreview(); }
    @FXML void setBgBlue()   { currentBg = "#e3f2fd"; applyPreview(); }
    @FXML void setBgNormal() { currentBg = "white"; applyPreview(); }
    @FXML void adjustFontSize() { applyPreview(); }
    @FXML void adjustLetterSpacing() { applyPreview(); }

    private void applyPreview() {
        double fs = fontSizeSlider.getValue();
        double ls = letterSpacingSlider.getValue();
        String style = String.format("-fx-font-size:%.0fpx; -fx-letter-spacing:%.1f;", fs, ls);
        previewBox.setStyle("-fx-background-color:" + currentBg + ";-fx-padding:16;-fx-background-radius:10;-fx-border-color:#e2e8f0;-fx-border-radius:10;");
        for (Label l : new Label[]{previewText1, previewText2, previewText3}) {
            l.setStyle(style);
        }
    }

    @FXML
    void toggleDyslexiaMode() {
        dyslexiaActive = !dyslexiaActive;
        if (dyslexiaActive) {
            btnDyslexia.setText("📖 Désactiver");
            btnDyslexia.getScene().getRoot().setStyle("-fx-font-family:'Arial'; -fx-font-size:" + fontSizeSlider.getValue() + "px; -fx-background-color:" + currentBg + ";");
        } else {
            btnDyslexia.setText("📖 Mode Dyslexie");
            btnDyslexia.getScene().getRoot().setStyle("");
        }
    }

    @FXML
    void playSelectedAudio() {
        String symbol = cbAudioSymbol.getValue();
        lblSoundFeedback.setText("Lecture du son: " + symbol);
        new Thread(() -> {
            if (!speakSymbolWindows(symbol)) {
                playBeepFor(symbol);
            }
            Platform.runLater(() -> lblSoundFeedback.setText("Lecture terminee: " + symbol));
        }).start();
    }

    private void playBeepFor(String symbol) {
        try {
            int freq = 440 + Math.abs(symbol.hashCode() % 400);
            AudioFormat fmt = new AudioFormat(44100, 8, 1, true, false);
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, fmt);
            SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
            line.open(fmt);
            line.start();
            int dur = 400;
            byte[] buf = new byte[(int) (fmt.getSampleRate() * dur / 1000)];
            for (int i = 0; i < buf.length; i++) {
                buf[i] = (byte) (75 * Math.sin(2 * Math.PI * freq * i / fmt.getSampleRate()));
            }
            line.write(buf, 0, buf.length);
            line.drain();
            line.close();
        } catch (Exception ignored) {}
    }

    private boolean speakSymbolWindows(String symbol) {
        String text = symbol.replace("'", " ");
        String ps = "Add-Type -AssemblyName System.Speech; "
                + "$s = New-Object System.Speech.Synthesis.SpeechSynthesizer; "
                + "$s.Rate = -1; $s.Speak('" + text + "');";
        try {
            Process p = new ProcessBuilder("powershell", "-Command", ps).start();
            return p.waitFor() == 0;
        } catch (IOException | InterruptedException e) {
            return false;
        }
    }

    @FXML
    void togglePomodoro() {
        if (pomodoroRunning) {
            pomodoroTimer.cancel();
            pomodoroRunning = false;
            return;
        }
        pomodoroRunning = true;
        pomodoroTimer = new Timer();
        pomodoroTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(() -> {
                    if (pomodoroSeconds > 0) {
                        pomodoroSeconds--;
                        updateTimerLabels();
                        return;
                    }
                    pomodoroTimer.cancel();
                    pomodoroRunning = false;
                    if (pomodoroWork) {
                        pomodoroSessionsDone++;
                        pomodoroWork = false;
                        pomodoroSeconds = 5 * 60;
                        lblPomodoroMode.setText("MODE PAUSE");
                    } else {
                        pomodoroWork = true;
                        pomodoroSeconds = 25 * 60;
                        lblPomodoroMode.setText("MODE TRAVAIL");
                        lblPomodoroMotivation.setText(QUOTES[pomodoroSessionsDone % QUOTES.length]);
                    }
                    updateTimerLabels();
                });
            }
        }, 1000, 1000);
    }

    @FXML
    void resetPomodoro() {
        if (pomodoroTimer != null) pomodoroTimer.cancel();
        pomodoroRunning = false;
        pomodoroWork = true;
        pomodoroSeconds = 25 * 60;
        lblPomodoroMode.setText("MODE TRAVAIL");
        updateTimerLabels();
    }

    private void updateTimerLabels() {
        int m = pomodoroSeconds / 60;
        int s = pomodoroSeconds % 60;
        lblPomodoroTime.setText(String.format("%02d:%02d", m, s));
        lblPomodoroSessions.setText("Sessions finalisées: " + pomodoroSessionsDone);
    }

    @FXML
    void refreshImpact() {
        try {
            List<String[]> students = MockDataService.getStudentProfiles();
            int atRisk = 0;
            double avg = 0;
            int abs = 0;
            for (String[] s : students) {
                double m = Double.parseDouble(s[3]);
                int a = Integer.parseInt(s[4]);
                avg += m;
                abs += a;
                if (s[2].toLowerCase().contains("risque") || a >= 3 || m < 11.0) atRisk++;
            }
            avg /= students.size();

            int totalPlans = planService.countAll();
            int feedbackPlans = planService.countWithFeedback();
            int latePlans = planService.countByStatutValue("EN_ATTENTE");
            int missingFeedback = Math.max(0, totalPlans - feedbackPlans);

            lblAtRiskCount.setText(String.valueOf(atRisk));
            lblMissingFeedback.setText(String.valueOf(missingFeedback));
            lblLatePlans.setText(String.valueOf(latePlans));
            lblImpactSummary.setText(String.format("Moyenne classe: %.1f | Absences cumulées: %d | Couverture feedback: %d%%",
                    avg, abs, totalPlans > 0 ? (feedbackPlans * 100 / totalPlans) : 0));

            taImpactDetail.setText(
                    "Centre d'alertes pédagogiques\n"
                            + "- Élèves à risque détectés: " + atRisk + "\n"
                            + "- Plans sans feedback: " + missingFeedback + "\n"
                            + "- Plans en retard (EN_ATTENTE): " + latePlans + "\n\n"
                            + "Suivi avant/après (indicateurs de classe)\n"
                            + "- Moyenne actuelle: " + String.format("%.1f", avg) + "\n"
                            + "- Absences cumulées: " + abs + "\n"
                            + "- Plans avec feedback: " + feedbackPlans + "/" + totalPlans
            );
        } catch (Exception e) {
            taImpactDetail.setText("Erreur de calcul des indicateurs: " + e.getMessage());
        }
    }

    @FXML
    void generateWeeklyActions() {
        String role = SessionManager.getCurrentUser() != null ? SessionManager.getCurrentUser().getRole() : "";
        taWeeklyActions.setText(
                "Assistant de décision — Top 3 actions de la semaine (" + role + ")\n"
                        + "1) Prioriser les 2 étudiants les plus à risque avec un plan de 15 min ciblé.\n"
                        + "2) Clôturer les plans en attente les plus anciens et exiger un feedback enseignant.\n"
                        + "3) Lancer un micro-diagnostic de classe puis adapter la prochaine séance."
        );
    }
}
