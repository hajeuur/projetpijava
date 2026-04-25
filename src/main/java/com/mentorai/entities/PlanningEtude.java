package com.mentorai.entities;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Entity mirroring the planning_etude database table.
 */
public class PlanningEtude {

    private int id;
    private String titreP;
    private LocalDate dateSeance;
    private LocalTime heureDebut;
    private LocalTime heureFin;
    private String matiere;
    private String typeActivite;
    private String description;
    private String notesPers;
    private Integer dureePrevue;   // minutes
    private Integer dureeReelle;   // minutes
    private String etat;           // "to do" | "done" | "skipped"
    private String couleurActivite;
    private LocalDateTime dateCreation;
    private LocalDateTime dateModification;
    private Integer utilisateurId;

    // ── constructors ────────────────────────────────────────────────────────

    public PlanningEtude() {}

    // ── getters / setters ───────────────────────────────────────────────────

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitreP() { return titreP; }
    public void setTitreP(String titreP) { this.titreP = titreP; }

    public LocalDate getDateSeance() { return dateSeance; }
    public void setDateSeance(LocalDate dateSeance) { this.dateSeance = dateSeance; }

    public LocalTime getHeureDebut() { return heureDebut; }
    public void setHeureDebut(LocalTime heureDebut) { this.heureDebut = heureDebut; }

    public LocalTime getHeureFin() { return heureFin; }
    public void setHeureFin(LocalTime heureFin) { this.heureFin = heureFin; }

    public String getMatiere() { return matiere; }
    public void setMatiere(String matiere) { this.matiere = matiere; }

    public String getTypeActivite() { return typeActivite; }
    public void setTypeActivite(String typeActivite) { this.typeActivite = typeActivite; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getNotesPers() { return notesPers; }
    public void setNotesPers(String notesPers) { this.notesPers = notesPers; }

    public Integer getDureePrevue() { return dureePrevue; }
    public void setDureePrevue(Integer dureePrevue) { this.dureePrevue = dureePrevue; }

    public Integer getDureeReelle() { return dureeReelle; }
    public void setDureeReelle(Integer dureeReelle) { this.dureeReelle = dureeReelle; }

    public String getEtat() { return etat; }
    public void setEtat(String etat) { this.etat = etat; }

    public String getCouleurActivite() { return couleurActivite; }
    public void setCouleurActivite(String couleurActivite) { this.couleurActivite = couleurActivite; }

    public LocalDateTime getDateCreation() { return dateCreation; }
    public void setDateCreation(LocalDateTime dateCreation) { this.dateCreation = dateCreation; }

    public LocalDateTime getDateModification() { return dateModification; }
    public void setDateModification(LocalDateTime dateModification) { this.dateModification = dateModification; }

    public Integer getUtilisateurId() { return utilisateurId; }
    public void setUtilisateurId(Integer utilisateurId) { this.utilisateurId = utilisateurId; }

    // ── helpers ─────────────────────────────────────────────────────────────

    /** Convert HH:mm LocalTime to total minutes since midnight. */
    public int startMinutes() {
        if (heureDebut == null) return 0;
        return heureDebut.getHour() * 60 + heureDebut.getMinute();
    }

    /** Duration label like "1h30" or "2h". */
    public String durationLabel() {
        if (dureePrevue == null || dureePrevue <= 0) return "";
        int h = dureePrevue / 60;
        int m = dureePrevue % 60;
        return m == 0 ? h + "h" : String.format("%dh%02d", h, m);
    }

    @Override
    public String toString() {
        return "PlanningEtude{id=" + id + ", titre='" + titreP + "', date=" + dateSeance + "}";
    }
}