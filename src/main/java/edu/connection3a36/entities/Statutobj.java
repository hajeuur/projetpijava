package edu.connection3a36.entities;

/**
 * ============================================================
 * ENUM : Statutobj
 * ============================================================
 * Représente le statut global d'un objectif.
 *
 * IMPORTANT : Ce statut est calculé et mis à jour AUTOMATIQUEMENT
 * par ScoreService.calculerStatutObjectif() à chaque changement
 * d'état d'une tâche. L'utilisateur ne le modifie pas manuellement.
 *
 * RÈGLES DE CALCUL AUTOMATIQUE :
 * ┌──────────────────────────────┬──────────────────┐
 * │ Condition                    │ Statut résultant │
 * ├──────────────────────────────┼──────────────────┤
 * │ Score = 0% (aucune tâche     │ Abandonner       │
 * │ réalisée)                    │                  │
 * ├──────────────────────────────┼──────────────────┤
 * │ Score = 100% (toutes les     │ Atteint          │
 * │ tâches réalisées)            │                  │
 * ├──────────────────────────────┼──────────────────┤
 * │ 0% < Score < 100%            │ EnCours          │
 * └──────────────────────────────┴──────────────────┘
 *
 * AFFICHAGE DANS L'INTERFACE :
 * - EnCours    → badge orange
 * - Atteint    → badge vert
 * - Abandonner → badge rouge
 * ============================================================
 */
public enum Statutobj {

    /** L'objectif est en cours de réalisation (score entre 1% et 99%) */
    EnCours("EnCours"),

    /** L'objectif est complètement atteint (score = 100%) */
    Atteint("Atteint"),

    /**
     * L'objectif est abandonné (score = 0%).
     * Cela signifie qu'aucune tâche n'a été réalisée.
     */
    Abandonner("Abandonner");

    /** Valeur stockée en base de données */
    private final String value;

    Statutobj(String value) { this.value = value; }

    /** Retourne la valeur telle qu'elle est stockée en base de données */
    public String getValue() { return value; }

    /**
     * Convertit une chaîne de la BDD en enum Statutobj.
     * Utilisé lors du mapping ResultSet → objet Java.
     *
     * @param value La valeur lue depuis la base de données
     * @return L'enum correspondant
     * @throws IllegalArgumentException si la valeur est inconnue
     */
    public static Statutobj fromValue(String value) {
        for (Statutobj s : values())
            if (s.value.equals(value)) return s;
        throw new IllegalArgumentException("Statut inconnu: " + value);
    }
}
