package edu.connection3a36.entities;

import java.time.LocalDate;

/**
 * ============================================================
 * ENTITÉ : Objectif
 * ============================================================
 * Représente un objectif personnel défini par un utilisateur.
 *
 * Un objectif est le point de départ de tout le module coaching.
 * Quand un utilisateur crée un objectif, le système crée automatiquement
 * un Programme lié (qui contiendra les tâches et le score).
 *
 * STRUCTURE EN BASE DE DONNÉES (table "objectif") :
 * ┌─────────────────┬──────────────────────────────────────────┐
 * │ Colonne         │ Description                              │
 * ├─────────────────┼──────────────────────────────────────────┤
 * │ id              │ Identifiant unique auto-incrémenté       │
 * │ titre           │ Nom de l'objectif (ex: "Apprendre Java") │
 * │ description     │ Détails de l'objectif (optionnel)        │
 * │ datedebut       │ Date de début                            │
 * │ datefin         │ Deadline (date limite)                   │
 * │ statut          │ EnCours / Atteint / Abandonner           │
 * │ programme_id    │ Clé étrangère → table programme          │
 * │ utilisateur_id  │ Clé étrangère → table utilisateur        │
 * └─────────────────┴──────────────────────────────────────────┘
 *
 * RELATION AVEC PROGRAMME :
 * Objectif (1) ──────────── (1) Programme
 *                                    │
 *                                    ├── (N) Tache
 *                                    └── (N) Motivation
 *
 * STATUT AUTOMATIQUE (géré par ScoreService) :
 * - Score = 0%   → Abandonner
 * - Score = 100% → Atteint
 * - Sinon        → EnCours
 * ============================================================
 */
public class Objectif {

    // ── Attributs ─────────────────────────────────────────────────────────────

    /** Identifiant unique en base de données */
    private int id;

    /** Titre de l'objectif (ex: "Apprendre le développement web") */
    private String titre;

    /** Description détaillée de l'objectif (peut être null) */
    private String description;

    /** Date de début de l'objectif */
    private LocalDate datedebut;

    /** Date limite (deadline) pour atteindre l'objectif */
    private LocalDate datefin;

    /**
     * Statut actuel de l'objectif.
     * IMPORTANT : ce statut est mis à jour AUTOMATIQUEMENT par ScoreService
     * selon le pourcentage de tâches réalisées. Ne pas le modifier manuellement.
     * Valeur par défaut : Atteint (sera écrasée à la création)
     */
    private Statutobj statut = Statutobj.Atteint;

    /**
     * Le programme lié à cet objectif.
     * Contient les tâches, le score et la médaille.
     * Créé automatiquement lors de la création de l'objectif.
     */
    private Programme programme;

    /** ID de l'utilisateur propriétaire de cet objectif */
    private int utilisateurId;

    // ── Constructeurs ─────────────────────────────────────────────────────────

    /** Constructeur vide requis pour le mapping depuis la base de données */
    public Objectif() {}

    /**
     * Constructeur complet utilisé lors de la création d'un nouvel objectif.
     * Le statut est initialisé à EnCours car aucune tâche n'est encore réalisée.
     *
     * @param titre         Nom de l'objectif
     * @param description   Description détaillée
     * @param datedebut     Date de début
     * @param datefin       Date limite (deadline)
     * @param utilisateurId ID de l'utilisateur propriétaire
     */
    public Objectif(String titre, String description, LocalDate datedebut,
                    LocalDate datefin, int utilisateurId) {
        this.titre = titre;
        this.description = description;
        this.datedebut = datedebut;
        this.datefin = datefin;
        this.utilisateurId = utilisateurId;
        this.statut = Statutobj.EnCours; // Statut initial : en cours
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

    /** Utilisé par les ComboBox JavaFX pour afficher le titre de l'objectif */
    @Override
    public String toString() { return titre; }
}
