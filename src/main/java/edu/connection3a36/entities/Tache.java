package edu.connection3a36.entities;

/**
 * Représente une étape concrète à réaliser dans un programme.
 * Les tâches sont les "briques" qui composent un objectif.
 *
 * Relation : Programme (1) ←→ (N) Tache
 *
 * Table BDD : "tache"
 * Colonnes  : id, ordre, titre, description, etat, programme_id
 *
 * Chaque changement d'état d'une tâche déclenche un recalcul du score
 * via ScoreService.recalculerEtSauvegarder().
 * Les tâches peuvent être générées automatiquement par l'IA Ollama.
 */
public class Tache {

    // ── Attributs ─────────────────────────────────────────────────────────────

    /** Identifiant unique généré par la BDD */
    private int id;

    /** Numéro d'ordre dans le programme (1, 2, 3...) — utilisé pour le tri */
    private int ordre;

    /** Nom de la tâche, ex : "Lire le chapitre 1 du cours" */
    private String titre;

    /** Description détaillée de la tâche (optionnelle) */
    private String description;

    /**
     * État actuel de la tâche (voir enum Etat) :
     *   encours    → en cours de réalisation (état par défaut)
     *   realisee   → terminée, compte dans le score
     *   Abandonner → abandonnée, déclenche l'analyse de risque IA
     */
    private Etat etat;

    /** ID du programme auquel cette tâche appartient */
    private int programmeId;

    // ── Constructeurs ─────────────────────────────────────────────────────────

    /** Constructeur vide — requis pour lire les données depuis la BDD */
    public Tache() {}

    /**
     * Constructeur utilisé lors de la création d'une tâche.
     * L'état initial est généralement Etat.encours.
     */
    public Tache(int ordre, String titre, String description, Etat etat, int programmeId) {
        this.ordre = ordre;
        this.titre = titre;
        this.description = description;
        this.etat = etat;
        this.programmeId = programmeId;
    }

    // ── Getters et Setters ────────────────────────────────────────────────────

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getOrdre() { return ordre; }
    public void setOrdre(int ordre) { this.ordre = ordre; }

    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Etat getEtat() { return etat; }
    public void setEtat(Etat etat) { this.etat = etat; }

    public int getProgrammeId() { return programmeId; }
    public void setProgrammeId(int programmeId) { this.programmeId = programmeId; }

    /** Retourne "1. Titre de la tâche" — utilisé dans les listes JavaFX */
    @Override
    public String toString() { return ordre + ". " + titre; }
}
