package edu.connection3a36.services;

import edu.connection3a36.entities.Utilisateur;
import edu.connection3a36.tools.SessionManager;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Labeled;
import javafx.scene.control.TextInputControl;
import javafx.scene.layout.Region;

import java.util.prefs.Preferences;

public class UserPreferencesService {

    public static class UserPreferencesModel {
        public boolean darkMode;
        public boolean dyslexicMode;
        public double uiScale;
        public double letterSpacing;
        public String readingBg;
    }

    private static final Preferences PREFS = Preferences.userRoot().node("mentorai/userprefs");

    private static String keyPrefix() {
        Utilisateur user = SessionManager.getCurrentUser();
        String identity = (user != null && user.getEmail() != null && !user.getEmail().isBlank())
                ? user.getEmail().toLowerCase()
                : "guest";
        return identity + ".";
    }

    public UserPreferencesModel load() {
        String k = keyPrefix();
        UserPreferencesModel p = new UserPreferencesModel();
        p.darkMode = PREFS.getBoolean(k + "darkMode", false);
        p.dyslexicMode = PREFS.getBoolean(k + "dyslexicMode", false);
        p.uiScale = PREFS.getDouble(k + "uiScale", 1.0);
        p.letterSpacing = PREFS.getDouble(k + "letterSpacing", 0.0);
        p.readingBg = PREFS.get(k + "readingBg", "#f0f4f9");
        return p;
    }

    public void save(UserPreferencesModel p) {
        String k = keyPrefix();
        PREFS.putBoolean(k + "darkMode", p.darkMode);
        PREFS.putBoolean(k + "dyslexicMode", p.dyslexicMode);
        PREFS.putDouble(k + "uiScale", p.uiScale);
        PREFS.putDouble(k + "letterSpacing", p.letterSpacing);
        PREFS.put(k + "readingBg", p.readingBg != null ? p.readingBg : "#f0f4f9");
    }

    public void applyToRoot(Parent root, UserPreferencesModel p) {
        if (root == null || p == null) return;

        root.getStyleClass().removeAll("dark-mode", "dyslexic-mode", "large-text");
        if (p.darkMode) root.getStyleClass().add("dark-mode");
        if (p.dyslexicMode) root.getStyleClass().add("dyslexic-mode");
        if (p.uiScale >= 1.12) root.getStyleClass().add("large-text");

        double fontPx = 13.0 * Math.max(0.85, Math.min(1.6, p.uiScale));
        double spacing = Math.max(0.0, Math.min(3.0, p.letterSpacing));
        String bg = p.readingBg == null || p.readingBg.isBlank() ? "#f0f4f9" : p.readingBg;
        root.setStyle("-fx-font-size: " + String.format("%.1f", fontPx) + "px; "
                + "-fx-letter-spacing: " + String.format("%.1f", spacing) + "; "
                + "-fx-background-color: " + bg + ";");

        applyDarkModeOverrides(root, p.darkMode);
    }

    private void applyDarkModeOverrides(Parent root, boolean darkMode) {
        applyDarkModeToNode(root, darkMode);
        ObservableList<Node> nodes = root.getChildrenUnmodifiable();
        for (Node node : nodes) {
            walkAndApply(node, darkMode);
        }
    }

    private void walkAndApply(Node node, boolean darkMode) {
        applyDarkModeToNode(node, darkMode);
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                walkAndApply(child, darkMode);
            }
        }
    }

    private void applyDarkModeToNode(Node node, boolean darkMode) {
        final String KEY = "mentorai.originalStyle";
        String original = (String) node.getProperties().get(KEY);
        if (!darkMode) {
            if (original != null) node.setStyle(original);
            return;
        }

        if (original == null) {
            node.getProperties().put(KEY, node.getStyle() == null ? "" : node.getStyle());
            original = (String) node.getProperties().get(KEY);
        }

        String extra = "";
        if (node instanceof Labeled) {
            extra += " -fx-text-fill: #e6edf3;";
        }
        if (node instanceof TextInputControl) {
            extra += " -fx-text-fill: #e6edf3; -fx-prompt-text-fill: #9aa4b2; "
                    + "-fx-control-inner-background: #0d1117; -fx-background-color: #0d1117; -fx-border-color: #30363d;";
        }
        if (node instanceof Region && !(node instanceof TextInputControl)) {
            extra += " -fx-background-color: #161b22;";
        }
        if (!extra.isBlank()) {
            node.setStyle((original == null ? "" : original) + " " + extra);
        }
    }
}
