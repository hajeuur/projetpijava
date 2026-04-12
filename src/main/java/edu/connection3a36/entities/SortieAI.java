package edu.connection3a36.entities;

import edu.connection3a36.enums.*;
import java.time.LocalDateTime;

/**
 * Entité SortieAI — sorties de l'intelligence artificielle (alertes, prédictions, etc.)
 * Correspondance avec la table `sortie_ai` dans MySQL.
 */
public class SortieAI {
    private int id;
    private int etudiantId;            // FK → utilisateur.id
    private StatutSortie statut;
    private Cible cible;
    private TypeSortie typeSortie;
    private Criticite criticite;
    private CategorieSortie categorieSortie;
    private String contenu;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ======================== CONSTRUCTEURS ========================

    public SortieAI() {
        this.statut = StatutSortie.NOUVEAU;
        this.createdAt = LocalDateTime.now();
    }

    public SortieAI(int etudiantId, Cible cible, TypeSortie typeSortie,
                    Criticite criticite, CategorieSortie categorieSortie, String contenu) {
        this.etudiantId = etudiantId;
        this.statut = StatutSortie.NOUVEAU;
        this.cible = cible;
        this.typeSortie = typeSortie;
        this.criticite = criticite;
        this.categorieSortie = categorieSortie;
        this.contenu = contenu;
        this.createdAt = LocalDateTime.now();
    }

    public SortieAI(int id, int etudiantId, StatutSortie statut, Cible cible,
                    TypeSortie typeSortie, Criticite criticite, CategorieSortie categorieSortie,
                    String contenu, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.etudiantId = etudiantId;
        this.statut = statut;
        this.cible = cible;
        this.typeSortie = typeSortie;
        this.criticite = criticite;
        this.categorieSortie = categorieSortie;
        this.contenu = contenu;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // ======================== GETTERS / SETTERS ========================

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getEtudiantId() { return etudiantId; }
    public void setEtudiantId(int etudiantId) { this.etudiantId = etudiantId; }

    public StatutSortie getStatut() { return statut; }
    public void setStatut(StatutSortie statut) { this.statut = statut; }

    public Cible getCible() { return cible; }
    public void setCible(Cible cible) { this.cible = cible; }

    public TypeSortie getTypeSortie() { return typeSortie; }
    public void setTypeSortie(TypeSortie typeSortie) { this.typeSortie = typeSortie; }

    public Criticite getCriticite() { return criticite; }
    public void setCriticite(Criticite criticite) { this.criticite = criticite; }

    public CategorieSortie getCategorieSortie() { return categorieSortie; }
    public void setCategorieSortie(CategorieSortie categorieSortie) { this.categorieSortie = categorieSortie; }

    public String getContenu() { return contenu; }
    public void setContenu(String contenu) { this.contenu = contenu; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    // ======================== UTILITAIRES ========================

    @Override
    public String toString() {
        return String.format("Sortie AI #%d (%s)", id, typeSortie != null ? typeSortie.name() : "?");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SortieAI that = (SortieAI) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }
}
