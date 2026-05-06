package com.mentorai.controllers;

import com.mentorai.entities.Humeur;
import com.mentorai.services.HumeurService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import edu.connection3a36.tools.SessionManager;
import edu.connection3a36.entities.Utilisateur;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * HumeurController – JavaFX controller that mirrors the Symfony
 * HumeurTrackerController's routes: dashboard, logMood, updateMood, getMoodData.
 *
 * Responsibilities
 * ─────────────────────────────────────────────────────────────────────────────
 * • Wire UI events to HumeurService calls
 * • Validate tag count (UI-level quick check; service performs authoritative check)
 * • Display errors via an Alert dialog AND an inline error label
 * • Refresh stats + chart after every insert / update
 * • Show emoji / colour depending on mood value
 */
public class HumeurController implements Initializable {

    // ── Service ───────────────────────────────────────────────────────────────
    private final HumeurService humeurService = new HumeurService();

    /**
     * Currently authenticated profil_apprentissage id.
     * In a real app this would come from a session / auth context.
    /** Currently authenticated utilisateur_id. */
    private int utilisateurId = 1;
    /** Resolved profil_apprentissage_id. */
    private int resolvedProfilId = -1;

    /** Id of the current day's mood entry (0 = none yet). */
    private int todayMoodId = 0;

    // ── FXML – header ─────────────────────────────────────────────────────────
    @FXML private Label greetingLabel;

    // ── FXML – sidebar stat labels ────────────────────────────────────────────
    @FXML private Label moyenne7Label;
    @FXML private Label streakLabel;
    @FXML private Label risqueLabel;

    // ── FXML – today banner ───────────────────────────────────────────────────
    @FXML private Label todaySummaryLabel;
    @FXML private Label todayTagsLabel;
    @FXML private Label todayJournalLabel;
    @FXML private Label todayEmojiLarge;

    // ── FXML – chart ──────────────────────────────────────────────────────────
    @FXML private LineChart<String, Number> moodChart;
    @FXML private CategoryAxis              chartXAxis;
    @FXML private NumberAxis                chartYAxis;

    // ── FXML – form inputs ────────────────────────────────────────────────────
    @FXML private Slider   humeurSlider;
    @FXML private Label    sliderValueLabel;
    @FXML private Label    sliderEmojiInline;

    // Tags checkboxes
    @FXML private CheckBox tagStress;
    @FXML private CheckBox tagSommeil;
    @FXML private CheckBox tagTravail;
    @FXML private CheckBox tagSocial;
    @FXML private CheckBox tagSanté;
    @FXML private CheckBox tagSport;
    @FXML private CheckBox tagAlimentation;
    @FXML private CheckBox tagFocus;

    // ── FXML – navigation ──
    @FXML private ScrollPane dashboardView;
    @FXML private VBox       guidedFlowView;
    @FXML private VBox       step1, step2;
    @FXML private Label      stepIndicatorLabel, stepTitleLabel;
    @FXML private Button     btnPrev, btnNext, btnSubmit;

    @FXML private Label  errorLabel;
    @FXML private Label  statusLabel;
    @FXML private Button mainActionButton;

    private int currentStep = 1;

    // ═════════════════════════════════════════════════════════════════════════
    //  Initialisation
    // ═════════════════════════════════════════════════════════════════════════

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        configureSliderListener();
        configureTagLimiter();
        showDashboard();
    }

    /** Called externally (e.g. from the main application) to set the user. */
    public void setProfilId(int id) {
        this.utilisateurId = id;
        try {
            this.resolvedProfilId = humeurService.getOrCreateProfilId(id);
        } catch (Exception e) {
            e.printStackTrace();
        }
        loadStats();
        loadChart();
    }

    private int getSafeProfilId() {
        if (resolvedProfilId == -1) {
            try {
                // Try to get current user from session
                Utilisateur currentUser = SessionManager.getCurrentUser();
                if (currentUser != null) {
                    this.utilisateurId = currentUser.getId();
                    this.resolvedProfilId = humeurService.getOrCreateProfilId(this.utilisateurId);
                } else {
                    // Fallback to default if no session (for dev/testing)
                    this.resolvedProfilId = humeurService.getOrCreateProfilId(utilisateurId);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return resolvedProfilId;
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Live UI wiring
    // ═════════════════════════════════════════════════════════════════════════

    /** Updates the value label and emoji in real time as the slider moves. */
    private void configureSliderListener() {
        humeurSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            int val = (int) Math.round(newVal.doubleValue());
            sliderValueLabel.setText(String.valueOf(val));
            sliderEmojiInline.setText(emojiForValue(val));
        });
    }



    /**
     * Prevents selecting more than 3 tag checkboxes.
     * If the user tries to select a 4th, it is automatically deselected
     * and an inline error is shown briefly.
     */
    private void configureTagLimiter() {
        List<CheckBox> allTags = getAllTagBoxes();
        for (CheckBox cb : allTags) {
            cb.selectedProperty().addListener((obs, wasSelected, isNowSelected) -> {
                if (isNowSelected) {
                    long selectedCount = allTags.stream().filter(CheckBox::isSelected).count();
                    if (selectedCount > 3) {
                        cb.setSelected(false);
                        showInlineError("Maximum 3 tags autorisés.");
                    }
                }
            });
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Public FXML action handlers
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Handles the final submission.
     */
    @FXML
    public void ajouterHumeur() {
        clearError();
        Humeur h = buildHumeurFromForm();
        if (h == null) return;

        try {
            if (todayMoodId == 0) {
                humeurService.ajouterHumeur(h);
            } else {
                humeurService.updateHumeur(todayMoodId, h);
            }
            showDashboard();
            setStatus("✅  Humeur enregistrée avec succès !");
        } catch (Exception e) {
            showError("Erreur : " + e.getMessage());
        }
    }

    /**
     * Opens the guided flow to either add or update today's mood.
     */
    @FXML
    public void handleMainAction() {
        showGuidedFlow();
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Guided Flow Navigation
    // ═════════════════════════════════════════════════════════════════════════

    @FXML
    public void showDashboard() {
        guidedFlowView.setVisible(false);
        guidedFlowView.setManaged(false);
        dashboardView.setVisible(true);
        dashboardView.setManaged(true);
        loadStats();
        loadChart();
    }

    @FXML
    public void showGuidedFlow() {
        dashboardView.setVisible(false);
        dashboardView.setManaged(false);
        guidedFlowView.setVisible(true);
        guidedFlowView.setManaged(true);
        
        int pid = getSafeProfilId();
        Humeur today = humeurService.getTodayMood(pid);
        if (today != null) {
            prePopulateForm(today);
            todayMoodId = today.getId();
        } else {
            resetForm();
            todayMoodId = 0;
        }

        currentStep = 1;
        updateStepVisibility();
    }

    private void prePopulateForm(Humeur h) {
        humeurSlider.setValue(h.getValeurHumeur());
        
        // Reset all tags first
        getAllTagBoxes().forEach(cb -> cb.setSelected(false));
        
        // Select tags from facteur_principal (which stores our tags now)
        if (h.getFacteurPrincipal() != null) {
            String[] tags = h.getFacteurPrincipal().split(",");
            for (String tag : tags) {
                String cleanTag = tag.trim().toLowerCase();
                getAllTagBoxes().stream()
                    .filter(cb -> cb.getText().trim().toLowerCase().equals(cleanTag))
                    .findFirst()
                    .ifPresent(cb -> cb.setSelected(true));
            }
        }
    }

    @FXML
    private void nextStep() {
        if (currentStep < 2) {
            currentStep++;
            updateStepVisibility();
        }
    }

    @FXML
    private void prevStep() {
        if (currentStep > 1) {
            currentStep--;
            updateStepVisibility();
        }
    }

    private void updateStepVisibility() {
        step1.setVisible(currentStep == 1);
        step1.setManaged(currentStep == 1);
        step2.setVisible(currentStep == 2);
        step2.setManaged(currentStep == 2);

        stepIndicatorLabel.setText("Étape " + currentStep + " sur 2");
        
        switch (currentStep) {
            case 1 -> stepTitleLabel.setText("Comment vous sentez-vous ?");
            case 2 -> stepTitleLabel.setText("Quels sont les facteurs principaux ?");
        }

        btnPrev.setDisable(currentStep == 1);
        btnNext.setVisible(currentStep < 2);
        btnNext.setManaged(currentStep < 2);
        btnSubmit.setVisible(currentStep == 2);
        btnSubmit.setManaged(currentStep == 2);
    }



    /**
     * Refreshes the statistics sidebar.
     * Mirrors Symfony's dashboard route data gathering.
     */
    @FXML
    public void loadStats() {
        int pid = getSafeProfilId();
        try {
            // Today's mood
            Humeur today = humeurService.getTodayMood(pid);
            if (today != null) {
                todayMoodId = today.getId();
                todaySummaryLabel.setText("Je me sens " + today.getLabel().toLowerCase());
                todayTagsLabel.setText(formatTags(today.getTendance()));
                todayJournalLabel.setText(today.getFacteurPrincipal() != null
                        ? today.getFacteurPrincipal() : "");
                todayEmojiLarge.setText(today.getEmoji());

                // Colour the risk label
                String risk = today.getNiveauRisque() != null ? today.getNiveauRisque() : "—";
                risqueLabel.setText(risk);
                risqueLabel.setStyle(risqueStyle(risk));

                mainActionButton.setText("✏️ Modifier l'humeur");
            } else {
                todayMoodId = 0;
                todaySummaryLabel.setText("Aucune humeur enregistrée pour aujourd'hui");
                todayTagsLabel.setText("");
                todayJournalLabel.setText("");
                todayEmojiLarge.setText("🌤️");
                risqueLabel.setText("—");
                risqueLabel.setStyle("");

                mainActionButton.setText("➕ Ajouter humeur");
            }

            // 7-day average
            Integer avg = humeurService.getAverageMood(pid, 7);
            if (avg != null) {
                moyenne7Label.setText(emojiForValue(avg) + "  " + avg + " / 5");
            } else {
                moyenne7Label.setText("Pas assez de données");
            }

            // Streak
            int streak = humeurService.getLongestStreak(pid);
            streakLabel.setText("🔥 " + streak + " jour" + (streak != 1 ? "s" : ""));

            setStatus("Dernière actualisation : " +
                    java.time.LocalTime.now().format(
                            DateTimeFormatter.ofPattern("HH:mm:ss")));

        } catch (Exception e) {
            showError("Impossible de charger les statistiques : " + e.getMessage());
        }
    }

    /**
     * Builds and populates the mood LineChart with the last 14 days of data.
     * Mirrors Symfony's getMoodData route and the chartData array mapping.
     *
     * Chart strategy:
     *  1. Fetch mood history via service (already ordered by date ASC)
     *  2. Map each Humeur → (date string, mood value) pair
     *  3. Clear old series and add a fresh XYChart.Series
     */
    @FXML
    public void loadChart() {
        int pid = getSafeProfilId();
        try {
            List<Humeur> history = humeurService.getMoodTrend(pid, 14);

            // Build ordered category list for the X axis
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MM/dd");
            List<String> categories = history.stream()
                    .map(h -> h.getDateJour().format(fmt))
                    .collect(Collectors.toList());

            chartXAxis.setCategories(FXCollections.observableArrayList(categories));

            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("Humeur");

            for (Humeur h : history) {
                XYChart.Data<String, Number> point =
                        new XYChart.Data<>(h.getDateJour().format(fmt), h.getValeurHumeur());
                series.getData().add(point);
            }

            moodChart.getData().clear();
            moodChart.getData().add(series);

            // Style each data node with the mood colour after rendering
            Platform.runLater(() -> {
                for (XYChart.Data<String, Number> data : series.getData()) {
                    int val = data.getYValue().intValue();
                    String colour = colourForValue(val);
                    if (data.getNode() != null) {
                        data.getNode().setStyle(
                                "-fx-background-color: " + colour + ", white;");
                    }
                }
            });

        } catch (Exception e) {
            showError("Impossible de charger le graphique : " + e.getMessage());
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Form helpers
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Reads all form controls and builds a {@link Humeur} ready for the service.
     * Returns null (and shows an error) if the facteur is too long.
     */
    private Humeur buildHumeurFromForm() {
        int val = (int) Math.round(humeurSlider.getValue());
        String tags = getSelectedTags();

        // The user requested to store selected tags in "facteur_principal" instead of text.
        // We'll map the tags to both columns to ensure consistency with current logic.
        Humeur h = new Humeur(val, tags, tags, utilisateurId);
        return h;
    }

    /** Collects the text of all selected CheckBoxes and joins them with commas. */
    private String getSelectedTags() {
        return getAllTagBoxes().stream()
                .filter(CheckBox::isSelected)
                .map(cb -> cb.getText().replaceAll("[^\\w]", "").toLowerCase())
                .collect(Collectors.joining(","));
    }

    /** Returns all tag CheckBoxes in a fixed order. */
    private List<CheckBox> getAllTagBoxes() {
        return List.of(tagStress, tagSommeil, tagTravail, tagSocial,
                tagSanté, tagSport, tagAlimentation, tagFocus);
    }

    /** Clears all form inputs back to defaults. */
    private void resetForm() {
        humeurSlider.setValue(3);
        getAllTagBoxes().forEach(cb -> cb.setSelected(false));
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  UI feedback helpers
    // ═════════════════════════════════════════════════════════════════════════

    private void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setVisible(true);

        // Also show an alert dialog for critical errors
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    private void showInlineError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
    }

    private void clearError() {
        errorLabel.setText("");
        errorLabel.setVisible(false);
    }

    private void setStatus(String msg) {
        statusLabel.setText(msg);
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Mapping helpers (Symfony constants → Java)
    // ═════════════════════════════════════════════════════════════════════════

    private String emojiForValue(int val) {
        return switch (val) {
            case 1  -> "😢";
            case 2  -> "😔";
            case 3  -> "😐";
            case 4  -> "😊";
            case 5  -> "😄";
            default -> "❓";
        };
    }

    private String colourForValue(int val) {
        return switch (val) {
            case 1  -> "#FF6B6B";
            case 2  -> "#9B59B6";
            case 3  -> "#3498DB";
            case 4  -> "#2ECC71";
            case 5  -> "#F1C40F";
            default -> "#94a3b8";
        };
    }

    /** Returns inline style for the risk label based on level string. */
    private String risqueStyle(String niveau) {
        return switch (niveau) {
            case "High"   -> "-fx-text-fill: #dc2626; -fx-font-weight: bold;";
            case "Medium" -> "-fx-text-fill: #d97706; -fx-font-weight: bold;";
            case "Low"    -> "-fx-text-fill: #16a34a; -fx-font-weight: bold;";
            default       -> "";
        };
    }

    /** Converts a comma-separated tendance string to a readable tag list. */
    private String formatTags(String tendance) {
        if (tendance == null || tendance.isBlank()) return "";
        String[] parts = tendance.split(",");
        return Arrays.stream(parts)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.joining("  "));
    }
}