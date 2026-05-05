package edu.connection3a36.entities;

/**
 * Enum représentant la récompense (gamification) attribuée selon le score.
 *
 * Règles d'attribution (ScoreService.attribuerMedaille()) :
 *   score = 0        → null    (aucune médaille, affiché "?")
 *   1% ≤ score < 50% → Bronze  🥉 (couleur #cd7f32)
 *   50% ≤ score < 80%→ Argent  🥈 (couleur #a8a9ad)
 *   score ≥ 80%      → Or      🥇 (couleur #ffd700)
 *
 * Stockée dans la colonne "meilleure_medaille" de la table "programme".
 * Utilisée pour coloriser la barre de progression dans l'interface.
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

    /** Retourne la valeur telle qu'elle est stockée en BDD */
    public String getValue() { return value; }

    /**
     * Convertit une chaîne lue depuis la BDD en enum Medaille.
     * Retourne null si la valeur est null (aucune médaille encore attribuée).
     */
    public static Medaille fromValue(String value) {
        if (value == null) return null; // pas encore de médaille
        for (Medaille m : values())
            if (m.value.equals(value)) return m;
        return null; // valeur inconnue → pas de médaille
    }
}
