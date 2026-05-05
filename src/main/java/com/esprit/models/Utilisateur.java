package com.esprit.models;

import java.util.Date;

public class Utilisateur {

    private int id;
    private String nom;
    private String prenom;
    private String email;
    private String mdp;
    private String pdpUrl;
    private Date dateInscription;
    private String role;
    private String resetToken;
    private Date resetTokenExpiresAt;
    private String status;
    private double trustScore;
    private String riskLevel;
    private int flaggedDuplicate;
    private int loginAttempts;
    private Date lastLogin;
    private String registrationIp;
    private String aiVerdict;
    private String preferences;

    // Constructeur vide
    public Utilisateur() {}

    // Constructeur avec paramètres principaux
    public Utilisateur(String nom, String prenom, String email, String mdp, String role) {
        this.nom = nom;
        this.prenom = prenom;
        this.email = email;
        this.mdp = mdp;
        this.role = role;
        this.status = "actif";
        this.trustScore = 100;
        this.riskLevel = "LOW";
    }

    // Getters et Setters
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

    public Date getDateInscription() { return dateInscription; }
    public void setDateInscription(Date dateInscription) { this.dateInscription = dateInscription; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public double getTrustScore() { return trustScore; }
    public void setTrustScore(double trustScore) { this.trustScore = trustScore; }

    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }

    public int getLoginAttempts() { return loginAttempts; }
    public void setLoginAttempts(int loginAttempts) { this.loginAttempts = loginAttempts; }

    public Date getLastLogin() { return lastLogin; }
    public void setLastLogin(Date lastLogin) { this.lastLogin = lastLogin; }

    public String getRegistrationIp() { return registrationIp; }
    public void setRegistrationIp(String registrationIp) { this.registrationIp = registrationIp; }

    public String getAiVerdict() { return aiVerdict; }
    public void setAiVerdict(String aiVerdict) { this.aiVerdict = aiVerdict; }

    public String getPreferences() { return preferences; }
    public void setPreferences(String preferences) { this.preferences = preferences; }

    public int getFlaggedDuplicate() { return flaggedDuplicate; }
    public void setFlaggedDuplicate(int f) { this.flaggedDuplicate = f; }
    @Override
    public String toString() {
        return "Utilisateur{id=" + id + ", nom=" + nom + ", prenom=" + prenom +
                ", email=" + email + ", role=" + role + ", status=" + status + "}";
    }
}