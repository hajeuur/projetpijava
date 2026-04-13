package edu.connection3a36.entities;

import java.time.LocalDate;
import java.util.Objects;

public class Parcours {

    private int id;
    private String typeParcours;
    private String titre;
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private String description;
    private String etablissement;
    private String diplome;
    private String specialite;
    private String entreprise;
    private String poste;
    private String typeContrat;
    private LocalDate dateCreation;
    private LocalDate dateModification;

    public Parcours() {}

    public Parcours(String typeParcours, String titre, LocalDate dateDebut, LocalDate dateFin,
                    String description, String etablissement, String diplome,
                    String specialite, String entreprise, String poste, String typeContrat) {
        this.typeParcours = typeParcours;
        this.titre = titre;
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
        this.description = description;
        this.etablissement = etablissement;
        this.diplome = diplome;
        this.specialite = specialite;
        this.entreprise = entreprise;
        this.poste = poste;
        this.typeContrat = typeContrat;
        this.dateCreation = LocalDate.now();
    }

    // Getters & Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTypeParcours() { return typeParcours; }
    public void setTypeParcours(String typeParcours) { this.typeParcours = typeParcours; }

    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }

    public LocalDate getDateDebut() { return dateDebut; }
    public void setDateDebut(LocalDate dateDebut) { this.dateDebut = dateDebut; }

    public LocalDate getDateFin() { return dateFin; }
    public void setDateFin(LocalDate dateFin) { this.dateFin = dateFin; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getEtablissement() { return etablissement; }
    public void setEtablissement(String etablissement) { this.etablissement = etablissement; }

    public String getDiplome() { return diplome; }
    public void setDiplome(String diplome) { this.diplome = diplome; }

    public String getSpecialite() { return specialite; }
    public void setSpecialite(String specialite) { this.specialite = specialite; }

    public String getEntreprise() { return entreprise; }
    public void setEntreprise(String entreprise) { this.entreprise = entreprise; }

    public String getPoste() { return poste; }
    public void setPoste(String poste) { this.poste = poste; }

    public String getTypeContrat() { return typeContrat; }
    public void setTypeContrat(String typeContrat) { this.typeContrat = typeContrat; }

    public LocalDate getDateCreation() { return dateCreation; }
    public void setDateCreation(LocalDate dateCreation) { this.dateCreation = dateCreation; }

    public LocalDate getDateModification() { return dateModification; }
    public void setDateModification(LocalDate dateModification) { this.dateModification = dateModification; }

    @Override
    public String toString() {
        return titre + " (" + typeParcours + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Parcours parcours = (Parcours) o;
        return id == parcours.id && Objects.equals(titre, parcours.titre);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, titre);
    }
}
