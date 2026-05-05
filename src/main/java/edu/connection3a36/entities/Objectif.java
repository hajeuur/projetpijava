package edu.connection3a36.entities;

import java.time.LocalDate;

/**
 * Représente un objectif personnel défini par un utilisateur.
 * C'est le point de départ du module coaching.
 * Chaque objectif est automatiquement lié à un Programme à sa création.
 *
 * Table BDD : "objectif"
 * Colonnes  : id, titre, description, datedebut, datefin, statut, programme_id, utilisateur_id
 *
 * Règle importante : le statut (EnCours/Atteint/Abandonner) est calculé
 * automatiquement par ScoreService selon le % de tâches réalisées.
 */
public class Objectif {

    // ── Attributs ─────────────────────────────────────────────────────────────

    /** Identifiant unique généré par la BDD (AUTO_INCREMENT) */
    private int id;

    /** Nom de l'objectif, ex : "Apprendre le développement web" */
    private String titre;

    /** Description détaillée (optionnelle) */
    private String description;

    /** Date à laquelle l'utilisateur commence à travailler sur l'objectif */
    private LocalDate datedebut;

    /** Date limite pour atteindre l'objectif (deadline) */
    private LocalDate datefin;

    /**
     * Statut actuel de l'objectif.
     * NE PAS modifier manuellement — géré automatiquement par ScoreService :
     *   score = 0%   → Abandonner
     *   score = 100% → Atteint
     *   sinon        → EnCours
     */
    private Statutobj statut = Statutobj.Atteint;

    /**
     * Le programme lié à cet objectif (contient les tâches, le score, la médaille).
     * Créé automatiquement par ObjectifService.addEntity().
     */
    private Programme programme;

    /** ID de l'utilisateur propriétaire de cet objectif */
    private int utilisateurId;

    // ── Constructeurs ─────────────────────────────────────────────────────────

    /** Constructeur vide — requis pour lire les données depuis la BDD */
    public Objectif() {}

    /**
     * Constructeur utilisé lors de la création d'un nouvel objectif.
     * Le statut est initialisé à EnCours (aucune tâche réalisée au départ).
     */
    public Objectif(String titre, String description, LocalDate datedebut,
                    LocalDate datefin, int utilisateurId) {
        this.titre = titre;
        this.description = description;
        this.datedebut = datedebut;
        this.datefin = datefin;
        this.utilisateurId = utilisateurId;
        this.statut = Statutobj.EnCours; // statut initial : en cours
    }

    // ── Getters et Setters ────────────────────────────────────────────────────

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDate getDatedebut() { return datedebut; }
    public void setDatedebut(LocalDate datedebut) { this.datedebut = datedebut; }

    public LocalDate getDatefin() { return datefin; }
    public void setDatefin(LocalDate datefin) { this.datefin = datefin; }

    public Statutobj getStatut() { return statut; }
    public void setStatut(Statutobj statut) { this.statut = statut; }

    public Programme getProgramme() { return programme; }
    public void setProgramme(Programme programme) { this.programme = programme; }

    public int getUtilisateurId() { return utilisateurId; }
    public void setUtilisateurId(int utilisateurId) { this.utilisateurId = utilisateurId; }

    /** Retourne le titre — utilisé par les ComboBox JavaFX */
    @Override
    public String toString() { return titre; }
}
