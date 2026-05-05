package com.esprit.utils;

public class GoogleUserInfo {

    private String email;
    private String nom;
    private String prenom;
    private String photoUrl;
    private String googleId;

    public GoogleUserInfo() {}

    public String getEmail()    { return email; }
    public String getNom()      { return nom; }
    public String getPrenom()   { return prenom; }
    public String getPhotoUrl() { return photoUrl; }
    public String getGoogleId() { return googleId; }

    public void setEmail(String email)       { this.email = email; }
    public void setNom(String nom)           { this.nom = nom; }
    public void setPrenom(String prenom)     { this.prenom = prenom; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }
    public void setGoogleId(String googleId) { this.googleId = googleId; }

    @Override
    public String toString() {
        return "GoogleUserInfo{email='" + email + "', nom='" + nom + "', prenom='" + prenom + "'}";
    }
}