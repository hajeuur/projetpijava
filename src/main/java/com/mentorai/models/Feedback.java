package com.mentorai.models;

import java.time.LocalDate;

/**
 * Cette classe représente la table "feedback" de ta base de données.
 * Chaque attribut correspond à une colonne de la table.
 */
public class Feedback {

    private int id;
    private String contenu;
    private int note;
    private LocalDate datefeedback;
    private String typefeedback;    // "probleme" ou "satisfaction"
    private String etatfeedback;    // "en_attente" ou "traite"
    private int traitementId;       // clé étrangère vers Traitement
    private int utilisateurId;      // clé étrangère vers Utilisateur

    // ---- Constructeur vide (nécessaire pour JavaFX) ----
    public Feedback() {}

    // ---- Constructeur avec tous les paramètres (sans id, pour l'ajout) ----
    public Feedback(String contenu, int note, LocalDate datefeedback,
                    String typefeedback, String etatfeedback,
                    int traitementId, int utilisateurId) {
        this.contenu = contenu;
        this.note = note;
        this.datefeedback = datefeedback;
        this.typefeedback = typefeedback;
        this.etatfeedback = etatfeedback;
        this.traitementId = traitementId;
        this.utilisateurId = utilisateurId;
    }

    // ---- Constructeur complet (avec id, pour la lecture depuis BDD) ----
    public Feedback(int id, String contenu, int note, LocalDate datefeedback,
                    String typefeedback, String etatfeedback,
                    int traitementId, int utilisateurId) {
        this.id = id;
        this.contenu = contenu;
        this.note = note;
        this.datefeedback = datefeedback;
        this.typefeedback = typefeedback;
        this.etatfeedback = etatfeedback;
        this.traitementId = traitementId;
        this.utilisateurId = utilisateurId;
    }

    // ---- Getters et Setters (Java a besoin de ces méthodes pour lire/modifier) ----

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getContenu() { return contenu; }
    public void setContenu(String contenu) { this.contenu = contenu; }

    public int getNote() { return note; }
    public void setNote(int note) { this.note = note; }

    public LocalDate getDatefeedback() { return datefeedback; }
    public void setDatefeedback(LocalDate datefeedback) { this.datefeedback = datefeedback; }

    public String getTypefeedback() { return typefeedback; }
    public void setTypefeedback(String typefeedback) { this.typefeedback = typefeedback; }

    public String getEtatfeedback() { return etatfeedback; }
    public void setEtatfeedback(String etatfeedback) { this.etatfeedback = etatfeedback; }

    public int getTraitementId() { return traitementId; }
    public void setTraitementId(int traitementId) { this.traitementId = traitementId; }

    public int getUtilisateurId() { return utilisateurId; }
    public void setUtilisateurId(int utilisateurId) { this.utilisateurId = utilisateurId; }

    // ---- toString : utile pour afficher un feedback dans la console ----
    @Override
    public String toString() {
        return "Feedback{id=" + id + ", note=" + note +
                ", type=" + typefeedback + ", etat=" + etatfeedback + "}";
    }
}
