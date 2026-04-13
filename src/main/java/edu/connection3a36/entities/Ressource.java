package edu.connection3a36.entities;

import java.time.LocalDate;
import java.util.Objects;

public class Ressource {

    private int id;
    private String nom;
    private String urlRessource;
    private String description;
    private String typeRessource; // PDF, VIDEO, LIEN, ARTICLE, AUTRE
    private LocalDate dateCreation;
    private LocalDate dateModification;
    private int projetId; // FK

    public Ressource() {
    }

    public Ressource(String nom, String urlRessource, String description,
            String typeRessource, int projetId) {
        this.nom = nom;
        this.urlRessource = urlRessource;
        this.description = description;
        this.typeRessource = typeRessource;
        this.projetId = projetId;
        this.dateCreation = LocalDate.now();
    }

    // Getters & Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getUrlRessource() {
        return urlRessource;
    }

    public void setUrlRessource(String urlRessource) {
        this.urlRessource = urlRessource;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTypeRessource() {
        return typeRessource;
    }

    public void setTypeRessource(String typeRessource) {
        this.typeRessource = typeRessource;
    }

    public LocalDate getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(LocalDate dateCreation) {
        this.dateCreation = dateCreation;
    }

    public LocalDate getDateModification() {
        return dateModification;
    }

    public void setDateModification(LocalDate dateModification) {
        this.dateModification = dateModification;
    }

    public int getProjetId() {
        return projetId;
    }

    public void setProjetId(int projetId) {
        this.projetId = projetId;
    }

    @Override
    public String toString() {
        return nom + " [" + typeRessource + "]";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Ressource ressource = (Ressource) o;
        return id == ressource.id && Objects.equals(nom, ressource.nom);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, nom);
    }
}
