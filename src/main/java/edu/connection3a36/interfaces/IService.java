package edu.connection3a36.interfaces;

import java.sql.SQLException;
import java.util.List;

/**
 * ============================================================
 * INTERFACE : IService<T>
 * ============================================================
 * Interface générique qui définit le contrat CRUD de base
 * pour tous les services de l'application.
 *
 * PRINCIPE DU DESIGN PATTERN "Service Layer" :
 * Toute la logique d'accès à la base de données est centralisée
 * dans des classes Service. Les contrôleurs JavaFX n'accèdent
 * JAMAIS directement à la BDD — ils passent toujours par un Service.
 *
 * SERVICES QUI IMPLÉMENTENT CETTE INTERFACE :
 * - ObjectifService  → gestion des objectifs
 * - ProgrammeService → gestion des programmes
 * - TacheService     → gestion des tâches
 * - MotivationService → gestion des messages de motivation
 *
 * PARAMÈTRE GÉNÉRIQUE <T> :
 * T représente le type d'entité géré par le service.
 * Exemple : IService<Objectif>, IService<Tache>, etc.
 *
 * ARCHITECTURE GLOBALE :
 * ┌─────────────────┐     ┌──────────────────┐     ┌──────────┐
 * │   Controller    │────▶│    Service<T>    │────▶│   BDD    │
 * │  (JavaFX/FXML)  │     │ implements       │     │  MySQL   │
 * │                 │     │ IService<T>      │     │          │
 * └─────────────────┘     └──────────────────┘     └──────────┘
 * ============================================================
 *
 * @param <T> Le type d'entité géré par ce service
 */
public interface IService<T> {

    /**
     * Ajoute une nouvelle entité en base de données.
     * L'ID de l'entité est mis à jour après l'insertion (auto-increment).
     *
     * @param t L'entité à insérer
     * @throws SQLException En cas d'erreur SQL (contrainte, connexion, etc.)
     */
    void addEntity(T t) throws SQLException;

    /**
     * Supprime une entité de la base de données.
     * Certains services implémentent une suppression en cascade
     * (ex: supprimer un objectif supprime aussi son programme et ses tâches).
     *
     * @param t L'entité à supprimer (doit avoir un ID valide)
     * @throws SQLException En cas d'erreur SQL
     */
    void deleteEntity(T t) throws SQLException;

    /**
     * Met à jour une entité existante en base de données.
     *
     * @param id L'identifiant de l'entité à modifier
     * @param t  L'entité avec les nouvelles valeurs
     * @throws SQLException En cas d'erreur SQL
     */
    void updateEntity(int id, T t) throws SQLException;

    /**
     * Récupère toutes les entités de la table correspondante.
     *
     * @return Liste de toutes les entités (peut être vide, jamais null)
     * @throws SQLException En cas d'erreur SQL
     */
    List<T> getData() throws SQLException;
}
