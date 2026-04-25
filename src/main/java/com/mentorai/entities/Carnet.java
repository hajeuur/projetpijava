package com.mentorai.entities;

import java.time.LocalDateTime;

public class Carnet {

    private int id;
    private String titre;
    private String contenu;
    private LocalDateTime dateCreation;
    private LocalDateTime dateModification;
    private String couleur = "#ffffff";
    private String visibilite = "visible";

    public Carnet() {}

    public Carnet(String titre, String contenu) {
        this.titre = titre;
        this.contenu = contenu;
        this.dateCreation = LocalDateTime.now();
        this.dateModification = LocalDateTime.now();
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }

    public String getContenu() { return contenu; }
    public void setContenu(String contenu) { this.contenu = contenu; }

    public LocalDateTime getDateCreation() { return dateCreation; }
    public void setDateCreation(LocalDateTime dateCreation) { this.dateCreation = dateCreation; }

    public LocalDateTime getDateModification() { return dateModification; }
    public void setDateModification(LocalDateTime dateModification) { this.dateModification = dateModification; }

    public LocalDateTime getDateAffichee() {
        return dateModification != null ? dateModification : dateCreation;
    }

    public String getCouleur() { return couleur; }
    public void setCouleur(String couleur) { this.couleur = couleur; }

    public String getVisibilite() { return visibilite; }
    public void setVisibilite(String visibilite) { this.visibilite = visibilite; }

    @Override
    public String toString() {
        return titre;
    }
}