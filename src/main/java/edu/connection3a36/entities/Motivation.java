package edu.connection3a36.entities;

import java.time.LocalDate;

/**
 * ============================================================
 * ENTITÉ : Motivation
 * ============================================================
 * Représente un message de motivation généré par l'IA Ollama
 * pour encourager l'utilisateur dans la réalisation de son objectif.
 *
 * RELATION :
 * Programme (1) ──────────── (N) Motivation
 *
 * STRUCTURE EN BASE DE DONNÉES (table "motivation") :
 * ┌──────────────────┬──────────────────────────────────────────────┐
 * │ Colonne          │ Description                                  │
 * ├──────────────────┼──────────────────────────────────────────────┤
 * │ id               │ Identifiant unique auto-incrémenté           │
 * │ dategeneratiomm  │ Date de génération du message (typo en BDD)  │
 * │ messagemotivant  │ Le texte du message généré par Ollama        │
 * │ programme_id     │ Clé étrangère → table programme              │
 * └──────────────────┴──────────────────────────────────────────────┘
 *
 * NOTE SUR LA TYPO :
 * La colonne s'appelle "dategeneratiomm" (avec deux 'm') dans la BDD.
 * C'est une erreur de frappe existante qui est conservée pour ne pas
 * casser la base de données. Le code s'adapte à ce nom.
 *
 * GÉNÉRATION DU MESSAGE :
 * OllamaService.genererMessageMotivant() envoie à Ollama :
 * - Le titre de l'objectif
 * - Le score actuel (%)
 * - Le score précédent (pour calculer l'évolution)
 * - La deadline (pour contextualiser l'urgence)
 * Ollama retourne un message personnalisé de 2-3 phrases.
 *
 * UTILISATION :
 * - L'utilisateur clique sur "Rafraîchir le message" dans ProgrammeDetail
 * - Un nouveau message est généré et sauvegardé en BDD
 * - Le dernier message est affiché (getLatestByProgramme)
 * ============================================================
 */
public class Motivation {

    // ── Attributs ─────────────────────────────────────────────────────────────

    /** Identifiant unique en base de données */
    private int id;

    /** Date à laquelle le message a été généré */
    private LocalDate dategeneration;

    /** Le texte du message motivant généré par l'IA Ollama */
    private String messagemotivant;

    /** ID du programme auquel ce message est associé */
    private int programmeId;

    // ── Constructeur ─────────────────────────────────────────────────────────

    /** Constructeur vide requis pour le mapping depuis la base de données */
    public Motivation() {}

    // ── Getters et Setters ────────────────────────────────────────────────────

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public LocalDate getDategeneration() { return dategeneration; }
    public void setDategeneration(LocalDate dategeneration) { this.dategeneration = dategeneration; }

    public String getMessagemotivant() { return messagemotivant; }
    public void setMessagemotivant(String messagemotivant) { this.messagemotivant = messagemotivant; }

    public int getProgrammeId() { return programmeId; }
    public void setProgrammeId(int programmeId) { this.programmeId = programmeId; }
}
