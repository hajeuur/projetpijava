package edu.connection3a36.entities;

import java.time.LocalDateTime;

public class Utilisateur {
    private int id;
    private String nom;
    private String prenom;
    private String email;
    private String mdp;
    private String pdpUrl;
    private LocalDateTime dateInscription;
    private String role;
    private String status;

    public Utilisateur() {}

    public Utilisateur(int id, String nom, String prenom, String email, String mdp, String pdpUrl, LocalDateTime dateInscription, String role, String status) {
        this.id = id;
        this.nom = nom;
        this.prenom = prenom;
        this.email = email;
        this.mdp = mdp;
        this.pdpUrl = pdpUrl;
        this.dateInscription = dateInscription;
        this.role = role;
        this.status = status;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getPrenom() { return prenom; }
    public void setPrenom(String prenom) { this.prenom = prenom; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getMdp() { return mdp; }
    public void setMdp(String mdp) { this.mdp = mdp; }

    public String getPdpUrl() { return pdpUrl; }
    public void setPdpUrl(String pdpUrl) { this.pdpUrl = pdpUrl; }

    public LocalDateTime getDateInscription() { return dateInscription; }
    public void setDateInscription(LocalDateTime dateInscription) { this.dateInscription = dateInscription; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    @Override
    public String toString() {
        return prenom + " " + nom + " (" + role + ")";
    }
}
