package edu.connection3a36.models;

import java.time.LocalDate;

public class Traitement {

    private int id;
    private String typetraitement;
    private String description;
    private LocalDate datetraitement;
    private String decision;

    // Constructeur vide
    public Traitement() {}

    // Constructeur sans id (pour l'ajout)
    public Traitement(String typetraitement, String description,
                      LocalDate datetraitement, String decision) {
        this.typetraitement = typetraitement;
        this.description = description;
        this.datetraitement = datetraitement;
        this.decision = decision;
    }

    // Constructeur complet (pour la lecture BDD)
    public Traitement(int id, String typetraitement, String description,
                      LocalDate datetraitement, String decision) {
        this.id = id;
        this.typetraitement = typetraitement;
        this.description = description;
        this.datetraitement = datetraitement;
        this.decision = decision;
    }

    // Getters et Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTypetraitement() { return typetraitement; }
    public void setTypetraitement(String typetraitement) { this.typetraitement = typetraitement; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDate getDatetraitement() { return datetraitement; }
    public void setDatetraitement(LocalDate datetraitement) { this.datetraitement = datetraitement; }

    public String getDecision() { return decision; }
    public void setDecision(String decision) { this.decision = decision; }

    @Override
    public String toString() {
        return "Traitement{id=" + id + ", type=" + typetraitement +
                ", decision=" + decision + "}";
    }
}
