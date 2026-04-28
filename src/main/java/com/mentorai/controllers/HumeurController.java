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
     * For now it is hard-coded to 1.  Change via setProfilId().
     */
    private int profilId = 1;

    /** Id of the current day's mood entry (0 = none yet). */
    private int todayMoodId = 0;

    // ── FXML – header ─────────────────────────────────────────────────────────
    @FXML private Label greetingLabel;

    // ── FXML – sidebar stat labels ────────────────────────────────────────────
    @FXML private Label moyenne7Label;
    @FXML private Label streakLabel;
    @FXML private Label risqueLabel;
    @FXML private Label emojiLabel;
    @FXML private Label moodLabelToday;

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
    @FXML private TextArea facteurField;
    @FXML private Label    facteurCountLabel;

    // Tags checkboxes
    @FXML private CheckBox tagStress;
    @FXML private CheckBox tagSommeil;
    @FXML private CheckBox tagTravail;
    @FXML private CheckBox tagSocial;
    @FXML private CheckBox tagSanté;
    @FXML private CheckBox tagSport;
    @FXML private CheckBox tagAlimentation;
    @FXML private CheckBox tagFocus;

    // ── FXML – buttons / feedback ─────────────────────────────────────────────
    @FXML private Button ajouterBtn;
    @FXML private Button updateBtn;
    @FXML private Label  errorLabel;
    @FXML private Label  statusLabel;

    // ═════════════════════════════════════════════════════════════════════════
    //  Initialisation
    // ═════════════════════════════════════════════════════════════════════════

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        configureSliderListener();
        configureFacteurCounter();
        configureTagLimiter();
        loadStats();
        loadChart();
    }

    /** Called externally (e.g. from the main application) to set the user. */
    public void setProfilId(int id) {
        this.profilId = id;
        loadStats();
        loadChart();
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

    /** Counts characters in the facteur TextArea in real time. */
    private void configureFacteurCounter() {
        facteurField.textProperty().addListener((obs, oldVal, newVal) -> {
            int len = newVal.length();
            facteurCountLabel.setText(len + " / 150");
            if (len > 150) {
                facteurCountLabel.setStyle("-fx-text-fill: #dc2626;");
            } else {
                facteurCountLabel.setStyle("");
            }
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
     * Handles the "Enregistrer" button.
     * Mirrors Symfony's POST /mood-tracker/log route.
     */
    @FXML
    public void ajouterHumeur() {
        clearError();
        Humeur h = buildHumeurFromForm();

        // Quick UI-level validation before hitting the service
        if (h == null) return;   // error already shown

        try {
            humeurService.ajouterHumeur(h);
            todayMoodId = h.getId();
            setStatus("✅  Humeur enregistrée avec succès !");
            resetForm();
            loadStats();
            loadChart();
            updateBtn.setVisible(true);
        } catch (IllegalStateException e) {
            // Already logged today → switch to update mode
            showError(e.getMessage());
            updateBtn.setVisible(true);
        } catch (IllegalArgumentException e) {
            showError(e.getMessage());
        } catch (Exception e) {
            showError("Erreur inattendue : " + e.getMessage());
        }
    }

    /**
     * Handles the "Modifier" button.
     * Mirrors Symfony's POST /mood-tracker/update/{id} route.
     */
    @FXML
    public void updateHumeur() {
        clearError();

        if (todayMoodId == 0) {
            // Try to find today's existing entry
            Humeur today = humeurService.getTodayMood(profilId);
            if (today == null) {
                showError("Aucune humeur trouvée pour aujourd'hui. Ajoutez-en une d'abord.");
                return;
            }
            todayMoodId = today.getId();
        }

        Humeur h = buildHumeurFromForm();
        if (h == null) return;

        try {
            humeurService.updateHumeur(todayMoodId, h);
            setStatus("✏️  Humeur mise à jour avec succès !");
            resetForm();
            loadStats();
            loadChart();
        } catch (IllegalArgumentException e) {
            showError(e.getMessage());
        } catch (Exception e) {
            showError("Erreur inattendue : " + e.getMessage());
        }
    }

    /**
     * Refreshes the statistics sidebar.
     * Mirrors Symfony's dashboard route data gathering.
     */
    @FXML
    public void loadStats() {
        try {
            // Today's mood
            Humeur today = humeurService.getTodayMood(profilId);
            if (today != null) {
                todayMoodId = today.getId();
                emojiLabel.setText(today.getEmoji());
                moodLabelToday.setText(today.getLabel());
                todaySummaryLabel.setText("Je me sens " + today.getLabel().toLowerCase());
                todayTagsLabel.setText(formatTags(today.getTendance()));
                todayJournalLabel.setText(today.getFacteurPrincipal() != null
                        ? today.getFacteurPrincipal() : "");
                todayEmojiLarge.setText(today.getEmoji());

                // Colour the risk label
                String risk = today.getNiveauRisque() != null ? today.getNiveauRisque() : "—";
                risqueLabel.setText(risk);
                risqueLabel.setStyle(risqueStyle(risk));

                // Show update button, hide add button if already logged
                ajouterBtn.setDisable(true);
                updateBtn.setVisible(true);
            } else {
                todayMoodId = 0;
                emojiLabel.setText("❓");
                moodLabelToday.setText("Pas encore enregistrée");
                todaySummaryLabel.setText("Aucune humeur enregistrée pour aujourd'hui");
                todayTagsLabel.setText("");
                todayJournalLabel.setText("");
                todayEmojiLarge.setText("🌤️");
                risqueLabel.setText("—");
                risqueLabel.setStyle("");
                ajouterBtn.setDisable(false);
                updateBtn.setVisible(false);
            }

            // 7-day average
            Integer avg = humeurService.getAverageMood(profilId, 7);
            if (avg != null) {
                moyenne7Label.setText(emojiForValue(avg) + "  " + avg + " / 5");
            } else {
                moyenne7Label.setText("Pas assez de données");
            }

            // Streak
            int streak = humeurService.getLongestStreak(profilId);
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
        try {
            List<Humeur> history = humeurService.getMoodTrend(profilId, 14);

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

        String facteur = facteurField.getText() != null
                ? facteurField.getText().trim() : "";

        if (facteur.length() > 150) {
            showError("Le facteur principal ne peut pas dépasser 150 caractères.");
            return null;
        }

        String tags = getSelectedTags();

        Humeur h = new Humeur(val, facteur, tags, profilId);
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
        facteurField.clear();
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
                .map(s -> "#" + s)
                .collect(Collectors.joining("  "));
    }
}