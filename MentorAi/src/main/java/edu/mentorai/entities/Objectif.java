package edu.mentorai.entities;

import java.time.LocalDate;

public class Objectif {
    private int id;
    private String titre;
    private String description;
    private LocalDate datedebut;
    private LocalDate datefin;
    private Statutobj statut = Statutobj.Atteint;
    private Programme programme;
    private int utilisateurId;

    public Objectif() {}

    public Objectif(String titre, String description, LocalDate datedebut,
                    LocalDate datefin, int utilisateurId) {
        this.titre = titre;
        this.description = description;
        this.datedebut = datedebut;
        this.datefin = datefin;
        this.utilisateurId = utilisateurId;
        this.statut = Statutobj.EnCours;
    }

    // Getters & Setters
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

    @Override
    public String toString() { return titre; }
}