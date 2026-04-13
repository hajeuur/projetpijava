package edu.connection3a36.tools;

import java.util.prefs.Preferences;

/**
 * Gestionnaire des préférences utilisateur (persistées dans le registre OS via java.util.prefs).
 * Stocke : dark mode, taille de police, mode dyslexique.
 */
public class UserPreferences {

    private static final String NODE = "mentorai/prefs";
    private static final Preferences PREFS = Preferences.userRoot().node(NODE);

    private static final String KEY_DARK    = "dark_mode";
    private static final String KEY_LARGE   = "large_text";
    private static final String KEY_DYSLEX  = "dyslexic_mode";

    public static boolean isDarkMode()     { return PREFS.getBoolean(KEY_DARK,   false); }
    public static boolean isLargeText()    { return PREFS.getBoolean(KEY_LARGE,  false); }
    public static boolean isDyslexicMode() { return PREFS.getBoolean(KEY_DYSLEX, false); }

    public static void setDarkMode(boolean v)     { PREFS.putBoolean(KEY_DARK,   v); }
    public static void setLargeText(boolean v)    { PREFS.putBoolean(KEY_LARGE,  v); }
    public static void setDyslexicMode(boolean v) { PREFS.putBoolean(KEY_DYSLEX, v); }

    /** Applique les préférences sur une scène (ajoute/retire les classes CSS) */
    public static void applyToScene(javafx.scene.Scene scene) {
        if (scene == null) return;
        scene.getRoot().getStyleClass().removeAll("dark-mode", "large-text", "dyslexic-mode");

        if (isDarkMode())     scene.getRoot().getStyleClass().add("dark-mode");
        if (isLargeText())    scene.getRoot().getStyleClass().add("large-text");
        if (isDyslexicMode()) scene.getRoot().getStyleClass().add("dyslexic-mode");
    }
}
