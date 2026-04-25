package edu.connection3a36.entities;

import edu.connection3a36.enums.CategorieSortie;
import edu.connection3a36.enums.Statut;
import java.time.LocalDateTime;

/**
 * Entité PlanActions — plans d'action créés par les enseignants/admins.
 * Correspondance avec la table `plan_actions` dans MySQL.
 *
 * Relations:
 *  - ManyToOne → Utilisateur (etudiant, auteur, feedbackAuteur)
 *  - ManyToOne → SortieAI (sortieAI)
 *  - ManyToMany → ReferenceArticle (via plan_actions_articles)
 */
public class PlanActions {
    private int id;
    private int etudiantId;             // FK → utilisateur.id (nullable)
    private String decision;
    private String description;
    private LocalDateTime date;
    private LocalDateTime updatedAt;
    private Statut statut;
    private CategorieSortie categorie;
    private int sortieAIId;             // FK → sortie_ai.id
    private String feedbackEnseignant;
    private LocalDateTime feedbackDate;
    private int feedbackAuteurId;       // FK → utilisateur.id
    private int auteurId;               // FK → utilisateur.id

    // ======================== CONSTRUCTEURS ========================

    public PlanActions() {
        this.date = LocalDateTime.now();
    }

    public PlanActions(String decision, String description, Statut statut,
                       CategorieSortie categorie, int sortieAIId, int auteurId) {
        this.decision = decision;
        this.description = description;
        this.statut = statut;
        this.categorie = categorie;
        this.sortieAIId = sortieAIId;
        this.auteurId = auteurId;
        this.date = LocalDateTime.now();
    }

    public PlanActions(int id, int etudiantId, String decision, String description,
                       LocalDateTime date, LocalDateTime updatedAt, Statut statut,
                       CategorieSortie categorie, int sortieAIId, String feedbackEnseignant,
                       LocalDateTime feedbackDate, int feedbackAuteurId, int auteurId) {
        this.id = id;
        this.etudiantId = etudiantId;
        this.decision = decision;
        this.description = description;
        this.date = date;
        this.updatedAt = updatedAt;
        this.statut = statut;
        this.categorie = categorie;
        this.sortieAIId = sortieAIId;
        this.feedbackEnseignant = feedbackEnseignant;
        this.feedbackDate = feedbackDate;
        this.feedbackAuteurId = feedbackAuteurId;
        this.auteurId = auteurId;
    }

    // ======================== GETTERS / SETTERS ========================

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getEtudiantId() { return etudiantId; }
    public void setEtudiantId(int etudiantId) { this.etudiantId = etudiantId; }

    public String getDecision() { return decision; }
    public void setDecision(String decision) { this.decision = decision; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDateTime getDate() { return date; }
    public void setDate(LocalDateTime date) { this.date = date; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public Statut getStatut() { return statut; }
    public void setStatut(Statut statut) { this.statut = statut; }

    public CategorieSortie getCategorie() { return categorie; }
    public void setCategorie(CategorieSortie categorie) { this.categorie = categorie; }

    public int getSortieAIId() { return sortieAIId; }
    public void setSortieAIId(int sortieAIId) { this.sortieAIId = sortieAIId; }

    public String getFeedbackEnseignant() { return feedbackEnseignant; }
    public void setFeedbackEnseignant(String feedbackEnseignant) {
        this.feedbackEnseignant = feedbackEnseignant;
        if (feedbackEnseignant != null && !feedbackEnseignant.isEmpty()) {
            this.feedbackDate = LocalDateTime.now();
        }
    }

    public LocalDateTime getFeedbackDate() { return feedbackDate; }
    public void setFeedbackDate(LocalDateTime feedbackDate) { this.feedbackDate = feedbackDate; }

    public int getFeedbackAuteurId() { return feedbackAuteurId; }
    public void setFeedbackAuteurId(int feedbackAuteurId) { this.feedbackAuteurId = feedbackAuteurId; }

    public int getAuteurId() { return auteurId; }
    public void setAuteurId(int auteurId) { this.auteurId = auteurId; }

    // ======================== MÉTHODES MÉTIER ========================

    /**
     * Retourne le libellé de la catégorie ou chaîne vide si null
     */
    public String getCategorieNom() {
        return categorie != null ? categorie.name() : "";
    }

    /**
     * Retourne le libellé du statut ou chaîne vide si null
     */
    public String getStatutNom() {
        return statut != null ? statut.name() : "";
    }

    // ======================== UTILITAIRES ========================

    @Override
    public String toString() {
        return decision != null ? decision : "Plan d'action sans titre";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PlanActions that = (PlanActions) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }
}
