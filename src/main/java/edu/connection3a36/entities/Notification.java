package edu.connection3a36.entities;

import java.time.LocalDateTime;

/**
 * Entité Notification pour le superadmin (ADMINM).
 * Stockée en BDD dans la table `notification`.
 */
public class Notification {
    private int id;
    private String titre;
    private String message;
    private String type; // INFO, WARNING, SUCCESS, ERROR
    private boolean isLu;
    private boolean isDone;
    private LocalDateTime dateCreation;
    private int auteurId; // ID de l'utilisateur qui a déclenché la notif (0 = système)

    public Notification() {
        this.dateCreation = LocalDateTime.now();
        this.type = "INFO";
        this.isLu = false;
        this.isDone = false;
    }

    public Notification(String titre, String message, String type) {
        this();
        this.titre = titre;
        this.message = message;
        this.type = type;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public boolean isLu() { return isLu; }
    public void setLu(boolean lu) { isLu = lu; }

    public boolean isDone() { return isDone; }
    public void setDone(boolean done) { isDone = done; }

    public LocalDateTime getDateCreation() { return dateCreation; }
    public void setDateCreation(LocalDateTime dateCreation) { this.dateCreation = dateCreation; }

    public int getAuteurId() { return auteurId; }
    public void setAuteurId(int auteurId) { this.auteurId = auteurId; }

    /** Retourne l'emoji selon le type */
    public String getTypeEmoji() {
        return switch (type) {
            case "WARNING" -> "⚠️";
            case "SUCCESS" -> "✅";
            case "ERROR"   -> "❌";
            default        -> "ℹ️";
        };
    }

    /** Retourne la couleur de fond selon le type */
    public String getTypeBgColor() {
        return switch (type) {
            case "WARNING" -> "#fff3cd";
            case "SUCCESS" -> "#d4edda";
            case "ERROR"   -> "#fddcdb";
            default        -> "#d1ecf1";
        };
    }

    /** Retourne la couleur du texte selon le type */
    public String getTypeTextColor() {
        return switch (type) {
            case "WARNING" -> "#856404";
            case "SUCCESS" -> "#155724";
            case "ERROR"   -> "#721c24";
            default        -> "#0c5460";
        };
    }
}
