package edu.connection3a36.entities;

/**
 * Enum représentant le statut global d'un objectif.
 *
 * Ce statut est calculé et mis à jour AUTOMATIQUEMENT par ScoreService
 * à chaque changement d'état d'une tâche. L'utilisateur ne le modifie jamais.
 *
 * Règles de calcul :
 *   score = 0%   → Abandonner  (aucune tâche réalisée)
 *   score = 100% → Atteint     (toutes les tâches réalisées)
 *   sinon        → EnCours     (en progression)
 *
 * Affichage dans l'interface :
 *   EnCours    → badge orange
 *   Atteint    → badge vert
 *   Abandonner → badge rouge
 */
public enum Statutobj {

    /** Objectif en cours de réalisation (score entre 1% et 99%) */
    EnCours("EnCours"),

    /** Objectif complètement atteint (score = 100%) */
    Atteint("Atteint"),

    /** Objectif abandonné (score = 0%, aucune tâche réalisée) */
    Abandonner("Abandonner");

    /** Valeur stockée en base de données */
    private final String value;

    Statutobj(String value) { this.value = value; }

    /** Retourne la valeur telle qu'elle est stockée en BDD */
    public String getValue() { return value; }

    /**
     * Convertit une chaîne lue depuis la BDD en enum Statutobj.
     * Utilisé dans la méthode map() de ObjectifService.
     *
     * @throws IllegalArgumentException si la valeur est inconnue
     */
    public static Statutobj fromValue(String value) {
        for (Statutobj s : values())
            if (s.value.equals(value)) return s;
        throw new IllegalArgumentException("Statut inconnu: " + value);
    }
}
