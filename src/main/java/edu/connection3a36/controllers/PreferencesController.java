package edu.connection3a36.controllers;

import edu.connection3a36.services.UserPreferencesService;
import edu.connection3a36.tools.AlertUtil;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.VBox;

public class PreferencesController {

    @FXML private CheckBox chkDarkMode;
    @FXML private CheckBox chkDyslexic;
    @FXML private Slider sliderUiScale;
    @FXML private Slider sliderLetterSpacing;
    @FXML private ComboBox<String> comboReadingBg;
    @FXML private Label lblScaleValue;
    @FXML private Label lblSpacingValue;
    @FXML private VBox previewBox;
    @FXML private Label previewText;

    private final UserPreferencesService prefsService = new UserPreferencesService();

    @FXML
    public void initialize() {
        comboReadingBg.getItems().addAll("Clair", "Crème", "Bleu pâle", "Vert pâle", "Sombre");
        var p = prefsService.load();
        chkDarkMode.setSelected(p.darkMode);
        chkDyslexic.setSelected(p.dyslexicMode);
        sliderUiScale.setValue(p.uiScale);
        sliderLetterSpacing.setValue(p.letterSpacing);
        comboReadingBg.setValue(toLabelBg(p.readingBg));
        refreshPreview();
    }

    @FXML
    void handlePreviewChange() {
        refreshPreview();
    }

    @FXML
    void handleSavePreferences() {
        UserPreferencesService.UserPreferencesModel p = buildModelFromUi();
        prefsService.save(p);
        var mc = MainController.getInstance();
        if (mc != null) mc.applyGlobalPreferences();
        AlertUtil.showSuccess("Préférences sauvegardées et appliquées globalement.");
    }

    private UserPreferencesService.UserPreferencesModel buildModelFromUi() {
        UserPreferencesService.UserPreferencesModel p = new UserPreferencesService.UserPreferencesModel();
        p.darkMode = chkDarkMode.isSelected();
        p.dyslexicMode = chkDyslexic.isSelected();
        p.uiScale = sliderUiScale.getValue();
        p.letterSpacing = sliderLetterSpacing.getValue();
        p.readingBg = toHexBg(comboReadingBg.getValue());
        return p;
    }

    private void refreshPreview() {
        lblScaleValue.setText(String.format("%.0f%%", sliderUiScale.getValue() * 100));
        lblSpacingValue.setText(String.format("%.1f", sliderLetterSpacing.getValue()));
        String bg = toHexBg(comboReadingBg.getValue());
        String textColor = chkDarkMode.isSelected() ? "#e6edf3" : "#102c59";
        previewBox.setStyle("-fx-background-color:" + bg + "; -fx-background-radius:10; -fx-padding:14;");
        previewText.setStyle("-fx-text-fill:" + textColor + "; -fx-font-size:" + (13.0 * sliderUiScale.getValue())
                + "px; -fx-letter-spacing:" + sliderLetterSpacing.getValue() + ";");
    }

    private String toHexBg(String label) {
        if (label == null) return "#f0f4f9";
        return switch (label) {
            case "Crème" -> "#fdf6e3";
            case "Bleu pâle" -> "#e3f2fd";
            case "Vert pâle" -> "#e8f5e9";
            case "Sombre" -> "#0d1117";
            default -> "#f0f4f9";
        };
    }

    private String toLabelBg(String hex) {
        if (hex == null) return "Clair";
        return switch (hex.toLowerCase()) {
            case "#fdf6e3" -> "Crème";
            case "#e3f2fd" -> "Bleu pâle";
            case "#e8f5e9" -> "Vert pâle";
            case "#0d1117" -> "Sombre";
            default -> "Clair";
        };
    }
}
