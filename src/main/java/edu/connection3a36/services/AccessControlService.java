package edu.connection3a36.services;

import edu.connection3a36.entities.Utilisateur;
import edu.connection3a36.tools.SessionManager;

public class AccessControlService {

    public enum Module {
        DASHBOARD_ADMIN, DASHBOARD_ENSEIGNANT, PLAN_ACTIONS, ARTICLES,
        IA_PEDAGOGIQUE, IA_DECISIONNELLE, IOT, AT_RISK, BACK_PARCOURS, BACK_PROJETS,
        UTILISATEURS, CATEGORIES, PARCOURS_FRONT, PROJETS_FRONT, GAMES_HUB
    }

    public boolean canAccess(Module module) {
        Utilisateur u = SessionManager.getCurrentUser();
        if (u == null) return false;
        String role = u.getRole() != null ? u.getRole().toUpperCase() : "";
        String email = u.getEmail() != null ? u.getEmail().toLowerCase() : "";
        boolean superAdmin = email.equals("admin@esprit.tn") || "ADMINM".equals(role);
        boolean admin = (email.equals("admin@gmail.com") || role.contains("ADMIN")) && !superAdmin;
        boolean enseignant = role.contains("ENSEIGNANT");
        boolean etudiant = role.contains("ETUDIANT") || role.equals("STUDENT");

        return switch (module) {
            case DASHBOARD_ADMIN, IA_DECISIONNELLE, UTILISATEURS, CATEGORIES -> superAdmin;
            case IOT, AT_RISK, BACK_PARCOURS, BACK_PROJETS -> superAdmin || admin;
            case DASHBOARD_ENSEIGNANT, IA_PEDAGOGIQUE -> enseignant;
            case PLAN_ACTIONS, ARTICLES, GAMES_HUB -> superAdmin || enseignant;
            case PARCOURS_FRONT, PROJETS_FRONT -> etudiant;
        };
    }
}
