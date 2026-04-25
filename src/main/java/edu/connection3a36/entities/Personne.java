package edu.connection3a36.entities;

import java.util.Objects;

public class Personne {
    protected int id;
    protected String nom;
    protected String prenom;

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

    public String getPrenom() {
        return prenom;
    }

    public void setPrenom(String prenom) {
        this.prenom = prenom;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Personne personne = (Personne) o;
        return nom == personne.nom && prenom == personne.prenom;
    }

    @Override
    public int hashCode() {
        return Objects.hash(nom, prenom);
    }

    public Personne(String nom, String prenom) {
        this.nom = nom;
        this.prenom = prenom;
    }

    public Personne(int id) {
        this.id = id;
    }
    public Personne() {}
}
