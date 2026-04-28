package edu.connection3a36.entities;

/**
 * ============================================================
 * ENUM : Etat
 * ============================================================
 * Représente l'état d'avancement d'une tâche.
 *
 * VALEURS POSSIBLES :
 * ┌─────────────┬──────────────────────────────────────────────────┐
 * │ Valeur      │ Signification                                    │
 * ├─────────────┼──────────────────────────────────────────────────┤
 * │ encours     │ Tâche en cours de réalisation (état par défaut)  │
 * │ realisee    │ Tâche terminée → compte dans le score (+1 point) │
 * │ Abandonner  │ Tâche abandonnée → déclenche l'analyse IA risque │
 * └─────────────┴──────────────────────────────────────────────────┘
 *
 * IMPACT SUR LE SCORE (ScoreService) :
 * - Seules les tâches "realisee" comptent dans le calcul du score.
 * - score = (nb realisee / nb total) × 100
 *
 * IMPACT SUR L'ANALYSE IA (RisqueAbandonService) :
 * - Les tâches "Abandonner" sont envoyées à Ollama pour analyser
 *   si elles mettent en danger l'objectif.
 *
 * NOTE : La valeur stockée en BDD est la chaîne retournée par getValue().
 * ============================================================
 */
public enum Etat {

    /** Tâche en cours de réalisation — état par défaut à la création */
    realisee("realisee"),

    /** Tâche terminée avec succès — compte dans le calcul du score */
    encours("encours"),

    /**
     * Tâche abandonnée par l'utilisateur.
     * Déclenche automatiquement l'analyse de risque par l'IA Ollama
     * et peut afficher un badge 🚨 dans l'interface.
     */
    Abandonner("Abandonner");

    /** Valeur stockée en base de données */
    private final String value;

    Etat(String value) { this.value = value; }

    /** Retourne la valeur telle qu'elle est stockée en base de données */
    public String getValue() { return value; }

    /**
     * Convertit une chaîne de la BDD en enum Etat.
     * Utilisé lors du mapping ResultSet → objet Java.
     *
     * @param value La valeur lue depuis la base de données
     * @return L'enum correspondant
     * @throws IllegalArgumentException si la valeur est inconnue
     */
    public static Etat fromValue(String value) {
        for (Etat e : values())
            if (e.value.equals(value)) return e;
        throw new IllegalArgumentException("Etat inconnu: " + value);
    }
}
