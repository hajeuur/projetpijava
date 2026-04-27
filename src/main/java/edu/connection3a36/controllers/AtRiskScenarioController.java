package edu.connection3a36.controllers;

import edu.connection3a36.enums.CategorieSortie;
import edu.connection3a36.enums.Statut;
import edu.connection3a36.services.GroqService;
import edu.connection3a36.services.MockDataService;
import edu.connection3a36.services.NotificationService;
import edu.connection3a36.services.PlanActionsService;
import edu.connection3a36.tools.AlertUtil;
import edu.connection3a36.tools.MarkdownRenderer;
import edu.connection3a36.tools.SessionManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;

/**
 * Scénario guidé : Étudiant à risque → Analyse IA → Plan personnalisé → Suivi
 */
public class AtRiskScenarioController {

    @FXML private ComboBox<String> comboStudent;
    @FXML private Label lblStudentProfile;
    @FXML private VBox boxAnalysis;
    @FXML private VBox boxPlanResult;
    @FXML private TextArea taCustomNote;
    @FXML private Label lblStep;
    @FXML private ProgressBar progressScenario;
    @FXML private Button btnAnalyze, btnCreatePlan, btnSave;

    private final GroqService groq = new GroqService();
    private final PlanActionsService planService = new PlanActionsService();
    private final NotificationService notifService = new NotificationService();

    private String lastAiAnalysis = "";
    private String selectedStudentKey = "";

    // Mock student profiles with risk indicators
    private static final String[][] STUDENTS = {
        {"Sarah Trabelsi (ID:102)", "À RISQUE", "Moyenne: 9.2 | Absences: 5 | Baisse brutale des notes depuis 1 mois | Semble isolée"},
        {"Rayen Khemiri (ID:107)", "À RISQUE", "Moyenne: 8.5 | Absences: 8 | Absentéisme chronique le matin | Convocation nécessaire"},
        {"Mehdi Bouaziz (ID:109)", "IRRÉGULIER", "Moyenne: 10.8 | Absences: 3 | Notes très fluctuantes | Manque de méthode"},
        {"Youssef Gharbi (ID:103)", "ATTENTION", "Moyenne: 11.5 | Absences: 1 | Perturbateur mais capacités pratiques réelles"},
        {"Ahmed Khelil (ID:105)", "SUIVI", "Moyenne: 12.5 | Absences: 0 | Discret, besoin d'encouragement"},
    };

    @FXML public void initialize() {
        for (String[] s : STUDENTS) comboStudent.getItems().add(s[0]);
        comboStudent.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> onStudentSelected(newV));
        lblStep.setText("Étape 1 : Sélectionnez un étudiant à risque");
        progressScenario.setProgress(0.0);
    }

    private void onStudentSelected(String name) {
        for (String[] s : STUDENTS) {
            if (s[0].equals(name)) {
                selectedStudentKey = s[0];
                lblStudentProfile.setText(
                    "🚨 Statut : " + s[1] + "\n📊 Indicateurs : " + s[2]
                );
                lblStudentProfile.setStyle("-fx-text-fill: " + (s[1].contains("RISQUE") ? "#dc2626" : "#d97706") + "; -fx-font-size: 13px;");
                boxAnalysis.getChildren().clear();
                boxPlanResult.getChildren().clear();
                lastAiAnalysis = "";
                lblStep.setText("Étape 2 : Lancez l'analyse IA");
                progressScenario.setProgress(0.25);
                btnAnalyze.setDisable(false);
                btnCreatePlan.setDisable(true);
                btnSave.setDisable(true);
                return;
            }
        }
    }

    @FXML void handleAnalyze() {
        if (selectedStudentKey.isEmpty()) { AlertUtil.showError("Sélectionnez un étudiant."); return; }
        btnAnalyze.setDisable(true);
        boxAnalysis.getChildren().clear();
        Label loading = new Label("🔄 Analyse IA en cours...");
        loading.setStyle("-fx-text-fill: #3b82f6; -fx-font-style: italic;");
        boxAnalysis.getChildren().add(loading);
        lblStep.setText("Analyse IA en cours...");
        progressScenario.setProgress(0.5);

        new Thread(() -> {
            try {
                String prompt = "Effectue une analyse pédagogique détaillée pour l'étudiant suivant :\n"
                    + selectedStudentKey + "\n"
                    + MockDataService.getEtudiants3A36Context()
                    + "\n\nFournis :\n1. Diagnostic des causes probables\n2. Risques identifiés\n3. Recommandations pédagogiques immédiates\n4. Plan d'action suggéré (décision + description)\nFormat Markdown.";
                lastAiAnalysis = groq.sendSimpleMessage(prompt, "ADMIN");
                Platform.runLater(() -> {
                    boxAnalysis.getChildren().clear();
                    MarkdownRenderer.render(lastAiAnalysis, boxAnalysis);
                    lblStep.setText("Étape 3 : Créez le plan personnalisé");
                    progressScenario.setProgress(0.75);
                    btnAnalyze.setDisable(false);
                    btnCreatePlan.setDisable(false);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    boxAnalysis.getChildren().clear();
                    boxAnalysis.getChildren().add(new Label("Erreur : " + e.getMessage()));
                    btnAnalyze.setDisable(false);
                });
            }
        }).start();
    }

    @FXML void handleCreatePlan() {
        if (lastAiAnalysis.isEmpty()) { AlertUtil.showError("Lancez d'abord l'analyse."); return; }
        btnCreatePlan.setDisable(true);

        new Thread(() -> {
            try {
                String firstLine = lastAiAnalysis.lines()
                    .filter(l -> !l.isBlank() && !l.startsWith("#"))
                    .findFirst().orElse("Plan de remédiation pour " + selectedStudentKey)
                    .replaceAll("[*#]", "").trim();
                String decision = "Remédiation : " + (firstLine.length() > 150 ? firstLine.substring(0,150) : firstLine);
                String description = "Analyse IA : " + lastAiAnalysis.substring(0, Math.min(400, lastAiAnalysis.length()));
                if (taCustomNote.getText() != null && !taCustomNote.getText().isBlank())
                    description += "\n\nNote de l'enseignant : " + taCustomNote.getText();

                edu.connection3a36.entities.PlanActions plan = new edu.connection3a36.entities.PlanActions();
                plan.setDecision(decision);
                plan.setDescription(description);
                plan.setStatut(Statut.EN_COURS);
                plan.setCategorie(CategorieSortie.PEDAGOGIQUE);
                plan.setAuteurId(SessionManager.getCurrentUser() != null ? SessionManager.getCurrentUser().getId() : 1);

                planService.addEntity(plan);

                notifService.addSystemNotification(
                    "Plan de remédiation créé",
                    "Plan IA créé pour " + selectedStudentKey + " (ID: " + plan.getId() + ")",
                    "SUCCESS"
                );

                Platform.runLater(() -> {
                    boxPlanResult.getChildren().clear();
                    Label ok = new Label("✅ Plan #" + plan.getId() + " créé avec succès !");
                    ok.setStyle("-fx-text-fill:#059669; -fx-font-weight:bold; -fx-font-size:15px;");
                    Label suivi = new Label("📋 Ce plan est maintenant visible dans la liste des Plans d'Action.\nStatut : EN COURS | Catégorie : PÉDAGOGIQUE");
                    suivi.setStyle("-fx-text-fill:#475569; -fx-font-size:12px;");
                    boxPlanResult.getChildren().addAll(ok, suivi);
                    lblStep.setText("✅ Étape 4 : Suivi activé — Plan en cours");
                    progressScenario.setProgress(1.0);
                    btnSave.setDisable(false);
                    btnCreatePlan.setDisable(false);
                    if (MainController.getInstance() != null) {
                        try { int cnt = notifService.countNonLues(); MainController.getInstance().updateNotificationBadge(cnt); } catch (Exception ignored) {}
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> { AlertUtil.showError("Erreur : " + e.getMessage()); btnCreatePlan.setDisable(false); });
            }
        }).start();
    }

    @FXML void handleSave() {
        AlertUtil.showSuccess("✅ Scénario de suivi enregistré ! Vous pouvez retrouver ce plan dans 'Plans d'Actions'.");
    }
}
