package edu.connection3a36.entities;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * ============================================================
 * ENTITÉ : Programme
 * ============================================================
 * Représente le "plan d'action" lié à un objectif.
 * C'est le conteneur des tâches, du score et de la médaille.
 *
 * RELATION :
 * Objectif (1) ──────────── (1) Programme
 *                                    │
 *                                    ├── (N) Tache   → les étapes à réaliser
 *                                    └── (N) Motivation → messages IA générés
 *
 * STRUCTURE EN BASE DE DONNÉES (table "programme") :
 * ┌──────────────────────┬──────────────────────────────────────────┐
 * │ Colonne              │ Description                              │
 * ├──────────────────────┼──────────────────────────────────────────┤
 * │ id                   │ Identifiant unique auto-incrémenté       │
 * │ titre                │ "Programme — [titre objectif]"           │
 * │ dategeneration       │ Date de création du programme            │
 * │ score_pourcentage    │ % de tâches réalisées (0 à 100)          │
 * │ meilleure_medaille   │ Bronze / Argent / Or (ou null)           │
 * └──────────────────────┴──────────────────────────────────────────┘
 *
 * CALCUL DU SCORE (géré par ScoreService) :
 * score = (nb tâches réalisées / nb total tâches) × 100
 *
 * ATTRIBUTION DE MÉDAILLE :
 * - score < 50%  → 🥉 Bronze
 * - score < 80%  → 🥈 Argent
 * - score ≥ 80%  → 🥇 Or
 * ============================================================
 */
public class Programme {

    // ── Attributs ─────────────────────────────────────────────────────────────

    /** Identifiant unique en base de données */
    private int id;

    /** Titre du programme (généré automatiquement : "Programme — [titre objectif]") */
    private String titre;

    /** Date de création du programme */
    private LocalDate dategeneration;

    /**
     * Score de progression en pourcentage (0 à 100).
     * Recalculé automatiquement à chaque changement d'état d'une tâche.
     */
    private int scorePourcentage = 0;

    /**
     * Meilleure médaille obtenue selon le score.
     * null si score = 0, Bronze si < 50%, Argent si < 80%, Or si ≥ 80%.
     */
    private Medaille meilleureMedaille;

    /**
     * Liste des tâches du programme (chargée en mémoire, pas en BDD directement).
     * Utilisée pour les calculs de score et l'affichage.
     */
    private List<Tache> taches = new ArrayList<>();

    /**
     * Liste des messages de motivation générés par l'IA Ollama.
     * Chaque message est lié à ce programme.
     */
    private List<Motivation> motivations = new ArrayList<>();

    // ── Constructeurs ─────────────────────────────────────────────────────────

    /** Constructeur vide requis pour le mapping depuis la base de données */
    public Programme() {}

    /**
     * Constructeur utilisé lors de la création automatique d'un programme
     * quand l'utilisateur crée un nouvel objectif.
     *
     * @param titre          Titre du programme
     * @param dategeneration Date de création
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

    /** Utilisé par les ComboBox JavaFX pour afficher le titre du programme */
    @Override
    public String toString() { return titre; }
}
