package edu.connection3a36.entities;

/**
 * ============================================================
 * ENTITÉ : Tache
 * ============================================================
 * Représente une étape concrète à réaliser dans un programme.
 * Les tâches sont les "briques" qui composent un objectif.
 *
 * RELATION :
 * Programme (1) ──────────── (N) Tache
 *
 * STRUCTURE EN BASE DE DONNÉES (table "tache") :
 * ┌──────────────┬──────────────────────────────────────────────┐
 * │ Colonne      │ Description                                  │
 * ├──────────────┼──────────────────────────────────────────────┤
 * │ id           │ Identifiant unique auto-incrémenté           │
 * │ ordre        │ Numéro d'ordre (1, 2, 3...) pour le tri      │
 * │ titre        │ Nom de la tâche (ex: "Lire le chapitre 1")   │
 * │ description  │ Détails de la tâche (optionnel)              │
 * │ etat         │ encours / realisee / Abandonner              │
 * │ programme_id │ Clé étrangère → table programme              │
 * └──────────────┴──────────────────────────────────────────────┘
 *
 * IMPACT SUR LE SCORE :
 * Chaque changement d'état d'une tâche déclenche un recalcul du score
 * du programme via ScoreService.recalculerEtSauvegarder().
 *
 * GÉNÉRATION PAR IA :
 * Les tâches peuvent être générées automatiquement par Ollama (llama3.1:8b)
 * via OllamaService.genererTaches() lors de la création d'un objectif.
 * ============================================================
 */
public class Tache {

    // ── Attributs ─────────────────────────────────────────────────────────────

    /** Identifiant unique en base de données */
    private int id;

    /**
     * Numéro d'ordre de la tâche dans le programme (1, 2, 3...).
     * Utilisé pour trier les tâches dans l'affichage.
     */
    private int ordre;

    /** Titre de la tâche (ex: "Lire le chapitre 1 du cours") */
    private String titre;

    /** Description détaillée de la tâche (peut être null ou vide) */
    private String description;

    /**
     * État actuel de la tâche.
     * Valeurs possibles (enum Etat) :
     * - encours    : tâche en cours de réalisation (état par défaut)
     * - realisee   : tâche terminée (compte dans le score)
     * - Abandonner : tâche abandonnée (déclenche l'analyse de risque IA)
     */
    private Etat etat;

    /** ID du programme auquel appartient cette tâche */
    private int programmeId;

    // ── Constructeurs ─────────────────────────────────────────────────────────

    /** Constructeur vide requis pour le mapping depuis la base de données */
    public Tache() {}

    /**
     * Constructeur complet utilisé lors de la création d'une tâche.
     *
     * @param ordre       Numéro d'ordre dans le programme
     * @param titre       Nom de la tâche
     * @param description Description détaillée
     * @param etat        État initial (généralement Etat.encours)
     * @param programmeId ID du programme parent
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

    /** Affiche "1. Titre de la tâche" dans les listes JavaFX */
    @Override
    public String toString() { return ordre + ". " + titre; }
}
