package edu.connection3a36.entities;

import java.time.LocalDateTime;

/**
 * Entité CategorieArticle — catégorise les articles de référence.
 * Correspondance avec la table `categorie_article` dans MySQL.
 */
public class CategorieArticle {
    private int id;
    private String nomCategorie;
    private String description;
    private int auteurId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ======================== CONSTRUCTEURS ========================

    public CategorieArticle() {
        this.createdAt = LocalDateTime.now();
    }

    public CategorieArticle(String nomCategorie, String description, int auteurId) {
        this.nomCategorie = nomCategorie;
        this.description = description;
        this.auteurId = auteurId;
        this.createdAt = LocalDateTime.now();
    }

    public CategorieArticle(int id, String nomCategorie, String description, int auteurId,
                            LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.nomCategorie = nomCategorie;
        this.description = description;
        this.auteurId = auteurId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // ======================== GETTERS / SETTERS ========================

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNomCategorie() { return nomCategorie; }
    public void setNomCategorie(String nomCategorie) { this.nomCategorie = nomCategorie; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public int getAuteurId() { return auteurId; }
    public void setAuteurId(int auteurId) { this.auteurId = auteurId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    // ======================== UTILITAIRES ========================

    @Override
    public String toString() {
        return nomCategorie != null ? nomCategorie : "Sans catégorie";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CategorieArticle that = (CategorieArticle) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }
}
