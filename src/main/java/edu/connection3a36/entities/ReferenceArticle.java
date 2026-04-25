package edu.connection3a36.entities;

import java.time.LocalDateTime;

/**
 * Entité ReferenceArticle — articles de référence pédagogique.
 * Correspondance avec la table `reference_article` dans MySQL.
 */
public class ReferenceArticle {
    private int id;
    private String titre;
    private String contenu;
    private int categorieId;           // FK → categorie_article.id
    private String categorieNom;       // Champ transient pour affichage
    private int auteurId;              // FK → utilisateur.id
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean published;

    // ======================== CONSTRUCTEURS ========================

    public ReferenceArticle() {
        this.createdAt = LocalDateTime.now();
        this.published = false;
    }

    public ReferenceArticle(String titre, String contenu, int categorieId, int auteurId) {
        this.titre = titre;
        this.contenu = contenu;
        this.categorieId = categorieId;
        this.auteurId = auteurId;
        this.createdAt = LocalDateTime.now();
        this.published = false;
    }

    public ReferenceArticle(int id, String titre, String contenu, int categorieId,
                            int auteurId, LocalDateTime createdAt, LocalDateTime updatedAt,
                            boolean published) {
        this.id = id;
        this.titre = titre;
        this.contenu = contenu;
        this.categorieId = categorieId;
        this.auteurId = auteurId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.published = published;
    }

    // ======================== GETTERS / SETTERS ========================

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }

    public String getContenu() { return contenu; }
    public void setContenu(String contenu) { this.contenu = contenu; }

    public int getCategorieId() { return categorieId; }
    public void setCategorieId(int categorieId) { this.categorieId = categorieId; }

    public String getCategorieNom() { return categorieNom; }
    public void setCategorieNom(String categorieNom) { this.categorieNom = categorieNom; }

    public int getAuteurId() { return auteurId; }
    public void setAuteurId(int auteurId) { this.auteurId = auteurId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public boolean isPublished() { return published; }
    public void setPublished(boolean published) { this.published = published; }

    // ======================== UTILITAIRES ========================

    @Override
    public String toString() {
        return titre != null ? titre : "Article sans titre";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ReferenceArticle that = (ReferenceArticle) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }
}
