package edu.connection3a36.tools;

import edu.connection3a36.entities.Utilisateur;

/**
 * Gère la session utilisateur connectée pour toute l'application.
 */
public class SessionManager {
    private static Utilisateur currentUser;

    public static Utilisateur getCurrentUser() {
        return currentUser;
    }

    public static void setCurrentUser(Utilisateur user) {
        currentUser = user;
    }

    public static void logout() {
        currentUser = null;
        frontMode = true; // reset par défaut
    }

    private static boolean frontMode = true; // par défaut on démarre sur le front

    public static boolean isFrontMode() {
        return frontMode;
    }

    public static void setFrontMode(boolean mode) {
        frontMode = mode;
    }
}
