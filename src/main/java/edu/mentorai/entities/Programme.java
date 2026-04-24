package edu.mentorai.entities;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class Programme {
    private int id;
    private String titre;
    private LocalDate dategeneration;
    private int scorePourcentage = 0;
    private Medaille meilleureMedaille;
    private List<Tache> taches = new ArrayList<>();
    private List<Motivation> motivations = new ArrayList<>();

    public Programme() {}

    public Programme(String titre, LocalDate dategeneration) {
        this.titre = titre;
        this.dategeneration = dategeneration;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }

    public LocalDate getDategeneration() { return dategeneration; }
    public void setDategeneration(LocalDate dategeneration) { this.dategeneration = dategeneration; }

    public int getScorePourcentage() { return scorePourcentage; }
    public void setScorePourcentage(int scorePourcentage) { this.scorePourcentage = scorePourcentage; }

    public Medaille getMeilleureMedaille() { return meilleureMedaille; }
    public void setMeilleureMedaille(Medaille meilleureMedaille) { this.meilleureMedaille = meilleureMedaille; }

    public List<Tache> getTaches() { return taches; }
    public void setTaches(List<Tache> taches) { this.taches = taches; }

    public List<Motivation> getMotivations() { return motivations; }
    public void setMotivations(List<Motivation> motivations) { this.motivations = motivations; }

    @Override
    public String toString() { return titre; }
}
