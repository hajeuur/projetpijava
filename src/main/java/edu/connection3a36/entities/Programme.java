package edu.connection3a36.entities;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Représente le plan d'action lié à un objectif.
 * Contient les tâches à réaliser, le score de progression et la médaille obtenue.
 *
 * Relation : Objectif (1) ←→ (1) Programme ←→ (N) Tache
 *                                              ←→ (N) Motivation
 *
 * Table BDD : "programme"
 * Colonnes  : id, titre, dategeneration, score_pourcentage, meilleure_medaille
 *
 * Le score est recalculé automatiquement par ScoreService à chaque
 * changement d'état d'une tâche.
 */
public class Programme {

    // ── Attributs ─────────────────────────────────────────────────────────────

    /** Identifiant unique généré par la BDD */
    private int id;

    /** Titre généré automatiquement : "Programme — [titre de l'objectif]" */
    private String titre;

    /** Date de création du programme */
    private LocalDate dategeneration;

    /**
     * Score de progression en % (0 à 100).
     * Calculé par ScoreService : (nb tâches réalisées / nb total) × 100
     */
    private int scorePourcentage = 0;

    /**
     * Meilleure médaille obtenue selon le score :
     *   null         → aucune médaille (score = 0)
     *   Bronze 🥉    → score < 50%
     *   Argent 🥈    → score < 80%
     *   Or     🥇    → score ≥ 80%
     */
    private Medaille meilleureMedaille;

    /** Liste des tâches du programme (chargée en mémoire pour les calculs) */
    private List<Tache> taches = new ArrayList<>();

    /** Liste des messages de motivation générés par l'IA Ollama */
    private List<Motivation> motivations = new ArrayList<>();

    // ── Constructeurs ─────────────────────────────────────────────────────────

    /** Constructeur vide — requis pour lire les données depuis la BDD */
    public Programme() {}

    /**
     * Constructeur utilisé lors de la création automatique d'un programme
     * quand l'utilisateur crée un nouvel objectif.
     */
    public Programme(String titre, LocalDate dategeneration) {
        this.titre = titre;
        this.dategeneration = dategeneration;
    }

    // ── Getters et Setters ────────────────────────────────────────────────────

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }

    public LocalDate getDategeneration() { return dategeneration; }
    public void setDategeneration(LocalDate dategeneration) { this.dategeneration = dategeneration; }

    public int getScorePourcentage() { return scorePourcentage; }
    public void setScorePourcentage(int scorePourcentage) { this.scorePourcentage = scorePourcentage; }

    public Medaille getMeilleureMedaille() { return meilleureMedaille; }
    public void setMeilleureMedaille(Medaille meilleureMedaille) { this.meilleureMedaille = meilleureMedaille; }

    public List<Tache> getTaches() { return taches; }
    public void setTaches(List<Tache> taches) { this.taches = taches; }

    public List<Motivation> getMotivations() { return motivations; }
    public void setMotivations(List<Motivation> motivations) { this.motivations = motivations; }

    /** Retourne le titre — utilisé par les ComboBox JavaFX */
    @Override
    public String toString() { return titre; }
}
