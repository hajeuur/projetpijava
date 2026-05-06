package edu.connection3a36.entities;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class Humeur {
    private int id;
    private int valeurHumeur;
    private String facteurPrincipal;
    private String tendance;
    private int moyenne7j;
    private int moyenne30j;
    private String niveauRisque;
    private LocalDateTime creeLe;
    private int profilApprentissageId;

    public Humeur() {
    }

    /** Convenience constructor used by HumeurController#buildHumeurFromForm(). */
    public Humeur(int valeurHumeur, String facteurPrincipal, String tendance, int profilApprentissageId) {
        this.valeurHumeur = valeurHumeur;
        this.facteurPrincipal = facteurPrincipal;
        this.tendance = tendance;
        this.profilApprentissageId = profilApprentissageId;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getValeurHumeur() {
        return valeurHumeur;
    }

    public void setValeurHumeur(int valeurHumeur) {
        this.valeurHumeur = valeurHumeur;
    }

    public String getFacteurPrincipal() {
        return facteurPrincipal;
    }

    public void setFacteurPrincipal(String facteurPrincipal) {
        this.facteurPrincipal = facteurPrincipal;
    }

    public String getTendance() {
        return tendance;
    }

    public void setTendance(String tendance) {
        this.tendance = tendance;
    }

    public int getMoyenne7j() {
        return moyenne7j;
    }

    public void setMoyenne7j(int moyenne7j) {
        this.moyenne7j = moyenne7j;
    }

    public int getMoyenne30j() {
        return moyenne30j;
    }

    public void setMoyenne30j(int moyenne30j) {
        this.moyenne30j = moyenne30j;
    }

    public String getNiveauRisque() {
        return niveauRisque;
    }

    public void setNiveauRisque(String niveauRisque) {
        this.niveauRisque = niveauRisque;
    }

    public LocalDateTime getCreeLe() {
        return creeLe;
    }

    public void setCreeLe(LocalDateTime creeLe) {
        this.creeLe = creeLe;
    }

    public int getProfilApprentissageId() {
        return profilApprentissageId;
    }

    public void setProfilApprentissageId(int profilApprentissageId) {
        this.profilApprentissageId = profilApprentissageId;
    }

    // ── Derived helpers used by HumeurController ──────────────────────────────

    /** Returns an emoji representing the current mood value (1-5). */
    public String getEmoji() {
        return switch (valeurHumeur) {
            case 1 -> "😢";
            case 2 -> "😔";
            case 3 -> "😐";
            case 4 -> "😊";
            case 5 -> "😄";
            default -> "❓";
        };
    }

    /** Returns a French label representing the current mood value (1-5). */
    public String getLabel() {
        return switch (valeurHumeur) {
            case 1 -> "Très mal";
            case 2 -> "Mal";
            case 3 -> "Neutre";
            case 4 -> "Bien";
            case 5 -> "Très bien";
            default -> "Inconnu";
        };
    }

    /**
     * Returns the date part of {@code creeLe} as a {@link LocalDate}.
     * Used by the chart in HumeurController#loadChart().
     */
    public LocalDate getDateJour() {
        return creeLe != null ? creeLe.toLocalDate() : LocalDate.now();
    }
}
