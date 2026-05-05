package edu.connection3a36.entities;

/**
 * Enum représentant l'état d'avancement d'une tâche.
 *
 * Valeurs possibles :
 *   encours    → tâche en cours (état par défaut à la création)
 *   realisee   → tâche terminée → compte dans le calcul du score
 *   Abandonner → tâche abandonnée → déclenche l'analyse de risque IA
 *
 * Impact sur le score (ScoreService) :
 *   score = (nb "realisee" / nb total) × 100
 *
 * Impact sur l'IA (RisqueAbandonService) :
 *   Les tâches "Abandonner" sont envoyées à Ollama pour évaluer le risque.
 */
public enum Etat {

    /** Tâche en cours de réalisation — état par défaut à la création */
    realisee("realisee"),

    /** Tâche terminée avec succès — compte dans le calcul du score */
    encours("encours"),

    /** Tâche abandonnée — déclenche l'analyse de risque IA et affiche le badge 🚨 */
    Abandonner("Abandonner");

    /** Valeur stockée en base de données (chaîne de caractères) */
    private final String value;

    Etat(String value) { this.value = value; }

    /** Retourne la valeur telle qu'elle est stockée en BDD */
    public String getValue() { return value; }

    /**
     * Convertit une chaîne lue depuis la BDD en enum Etat.
     * Utilisé dans la méthode map() de TacheService.
     *
     * @throws IllegalArgumentException si la valeur est inconnue
     */
    public static Etat fromValue(String value) {
        for (Etat e : values())
            if (e.value.equals(value)) return e;
        throw new IllegalArgumentException("Etat inconnu: " + value);
    }
}
