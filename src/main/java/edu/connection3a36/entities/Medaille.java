package edu.connection3a36.entities;

/**
 * ============================================================
 * ENUM : Medaille
 * ============================================================
 * Représente la récompense (gamification) attribuée à un programme
 * selon le score de progression de l'utilisateur.
 *
 * RÈGLES D'ATTRIBUTION (ScoreService.attribuerMedaille()) :
 * ┌──────────────────┬──────────────┬──────────────────────────┐
 * │ Score            │ Médaille     │ Affichage                │
 * ├──────────────────┼──────────────┼──────────────────────────┤
 * │ score = 0        │ null         │ "?" (aucune médaille)    │
 * │ 1% ≤ score < 50% │ Bronze       │ 🥉 (couleur #cd7f32)     │
 * │ 50% ≤ score < 80%│ Argent       │ 🥈 (couleur #a8a9ad)     │
 * │ score ≥ 80%      │ Or           │ 🥇 (couleur #ffd700)     │
 * └──────────────────┴──────────────┴──────────────────────────┘
 *
 * STOCKAGE EN BDD :
 * La médaille est stockée dans la colonne "meilleure_medaille"
 * de la table "programme" sous forme de chaîne ("Bronze", "Argent", "Or").
 *
 * UTILISATION DANS L'INTERFACE :
 * - ScoreService.emojiMedaille(m) → retourne "🥉 Bronze", "🥈 Argent", "🥇 Or"
 * - ScoreService.couleurMedaille(m) → retourne la couleur CSS correspondante
 * - La barre de progression change de couleur selon la médaille
 * ============================================================
 */
public enum Medaille {

    /** 🥉 Bronze — score entre 1% et 49% */
    Bronze("Bronze"),

    /** 🥈 Argent — score entre 50% et 79% */
    Argent("Argent"),

    /** 🥇 Or — score entre 80% et 100% */
    Or("Or");

    /** Valeur stockée en base de données */
    private final String value;

    Medaille(String value) { this.value = value; }

    /** Retourne la valeur telle qu'elle est stockée en base de données */
    public String getValue() { return value; }

    /**
     * Convertit une chaîne de la BDD en enum Medaille.
     * Retourne null si la valeur est null (aucune médaille encore attribuée).
     *
     * @param value La valeur lue depuis la base de données (peut être null)
     * @return L'enum correspondant, ou null si aucune médaille
     */
    public static Medaille fromValue(String value) {
        if (value == null) return null; // Pas encore de médaille
        for (Medaille m : values())
            if (m.value.equals(value)) return m;
        return null; // Valeur inconnue → pas de médaille
    }
}
