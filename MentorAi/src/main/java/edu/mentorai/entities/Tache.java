package edu.mentorai.entities;

public class Tache {
    private int id;
    private int ordre;
    private String titre;
    private String description;
    private Etat etat;
    private int programmeId;

    public Tache() {}

    public Tache(int ordre, String titre, String description, Etat etat, int programmeId) {
        this.ordre = ordre;
        this.titre = titre;
        this.description = description;
        this.etat = etat;
        this.programmeId = programmeId;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getOrdre() { return ordre; }
    public void setOrdre(int ordre) { this.ordre = ordre; }

    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Etat getEtat() { return etat; }
    public void setEtat(Etat etat) { this.etat = etat; }

    public int getProgrammeId() { return programmeId; }
    public void setProgrammeId(int programmeId) { this.programmeId = programmeId; }

    @Override
    public String toString() { return ordre + ". " + titre; }
}