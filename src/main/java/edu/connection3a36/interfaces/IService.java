package edu.connection3a36.interfaces;

import java.sql.SQLException;
import java.util.List;

/**
 * Interface générique qui définit les 4 opérations CRUD de base.
 * Tous les services du module objectifs l'implémentent.
 * Le paramètre <T> représente le type d'entité géré (ex: Objectif, Tache...).
 */
public interface IService<T> {

    /** Ajoute une nouvelle entité en base de données. */
    void addEntity(T t) throws SQLException;

    /** Supprime une entité de la base de données. */
    void deleteEntity(T t) throws SQLException;

    /** Met à jour une entité existante par son ID. */
    void updateEntity(int id, T t) throws SQLException;

    /** Retourne toutes les entités de la table. */
    List<T> getData() throws SQLException;
}
