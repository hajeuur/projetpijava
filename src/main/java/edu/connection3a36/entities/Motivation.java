package edu.connection3a36.entities;

import java.time.LocalDate;

/**
 * Représente un message de motivation généré par l'IA Ollama.
 * Chaque message est lié à un programme et stocké en BDD.
 *
 * Relation : Programme (1) ←→ (N) Motivation
 *
 * Table BDD : "motivation"
 * Colonnes  : id, dategeneratiomm (typo BDD), messagemotivant, programme_id
 *
 * Note : la colonne date s'appelle "dategeneratiomm" (double 'm') dans la BDD.
 * C'est une erreur de frappe conservée pour ne pas casser la base de données.
 *
 * Le message est généré par OllamaService.genererMessageMotivant() en tenant
 * compte du score actuel, de l'évolution et de la deadline.
 */
public class Motivation {

    // ── Attributs ─────────────────────────────────────────────────────────────

    /** Identifiant unique généré par la BDD */
    private int id;

    /** Date à laquelle le message a été généré par l'IA */
    private LocalDate dategeneration;

    /** Texte du message motivant généré par Ollama (2-3 phrases) */
    private String messagemotivant;

    /** ID du programme auquel ce message est associé */
    private int programmeId;

    // ── Constructeur ─────────────────────────────────────────────────────────

    /** Constructeur vide — requis pour lire les données depuis la BDD */
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
