package edu.connection3a36.entities;

import java.time.LocalDate;
import java.util.Objects;

public class Projet {

    private int id;
    private String titre;
    private String type;
    private String description;
    private String technologies;
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private LocalDate dateCreation;
    private LocalDate dateModification;
    private int parcoursId; // FK

    public Projet() {}

    public Projet(String titre, String type, String description, String technologies,
                  LocalDate dateDebut, LocalDate dateFin, int parcoursId) {
        this.titre = titre;
        this.type = type;
        this.description = description;
        this.technologies = technologies;
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
        this.parcoursId = parcoursId;
        this.dateCreation = LocalDate.now();
    }

    // Getters & Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getTechnologies() { return technologies; }
    public void setTechnologies(String technologies) { this.technologies = technologies; }

    public LocalDate getDateDebut() { return dateDebut; }
    public void setDateDebut(LocalDate dateDebut) { this.dateDebut = dateDebut; }

    public LocalDate getDateFin() { return dateFin; }
    public void setDateFin(LocalDate dateFin) { this.dateFin = dateFin; }

    public LocalDate getDateCreation() { return dateCreation; }
    public void setDateCreation(LocalDate dateCreation) { this.dateCreation = dateCreation; }

    public LocalDate getDateModification() { return dateModification; }
    public void setDateModification(LocalDate dateModification) { this.dateModification = dateModification; }

    public int getParcoursId() { return parcoursId; }
    public void setParcoursId(int parcoursId) { this.parcoursId = parcoursId; }

    @Override
    public String toString() {
        return titre + " (" + type + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Projet projet = (Projet) o;
        return id == projet.id && Objects.equals(titre, projet.titre);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, titre);
    }
}
