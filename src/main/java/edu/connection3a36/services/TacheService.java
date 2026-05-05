package edu.connection3a36.services;

import edu.connection3a36.interfaces.IService;
import edu.connection3a36.tools.MyConnection;
import edu.connection3a36.entities.Etat;
import edu.connection3a36.entities.Tache;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Gère toutes les opérations sur les tâches d'un programme.
 * Implémente IService<Tache> pour les opérations CRUD de base.
 *
 * Méthodes supplémentaires :
 *   getByProgramme()   → récupère les tâches d'un programme (la plus utilisée)
 *   updateEtat()       → change l'état d'une tâche (déclenche le recalcul du score)
 *   deleteByProgramme()→ supprime toutes les tâches d'un programme (cascade)
 *   validate()         → vérifie les données avant insertion
 */
public class TacheService implements IService<Tache> {

    /** Connexion unique à la BDD (pattern Singleton) */
    private final Connection cnx = MyConnection.getInstance().getCnx();

    // ── CRUD de base ──────────────────────────────────────────────────────────

    /** Insère une nouvelle tâche en BDD et récupère son ID généré. */
    @Override
    public void addEntity(Tache t) throws SQLException {
        String req = "INSERT INTO tache (ordre, titre, description, etat, programme_id) VALUES (?,?,?,?,?)";
        PreparedStatement pst = cnx.prepareStatement(req, Statement.RETURN_GENERATED_KEYS);
        pst.setInt(1, t.getOrdre());
        pst.setString(2, t.getTitre());
        pst.setString(3, t.getDescription() != null ? t.getDescription() : "");
        // Si l'état est null, on utilise "encours" par défaut
        pst.setString(4, t.getEtat() != null ? t.getEtat().getValue() : Etat.encours.getValue());
        pst.setInt(5, t.getProgrammeId());
        pst.executeUpdate();
        // Récupérer l'ID auto-généré par MySQL
        ResultSet rs = pst.getGeneratedKeys();
        if (rs.next()) t.setId(rs.getInt(1));
    }

    /** Supprime une tâche par son ID. */
    @Override
    public void deleteEntity(Tache t) throws SQLException {
        PreparedStatement pst = cnx.prepareStatement("DELETE FROM tache WHERE id = ?");
        pst.setInt(1, t.getId());
        pst.executeUpdate();
    }

    /** Met à jour le titre, la description, l'ordre et l'état d'une tâche. */
    @Override
    public void updateEntity(int id, Tache t) throws SQLException {
        String req = "UPDATE tache SET ordre=?, titre=?, description=?, etat=? WHERE id=?";
        PreparedStatement pst = cnx.prepareStatement(req);
        pst.setInt(1, t.getOrdre());
        pst.setString(2, t.getTitre());
        pst.setString(3, t.getDescription() != null ? t.getDescription() : "");
        pst.setString(4, t.getEtat() != null ? t.getEtat().getValue() : Etat.encours.getValue());
        pst.setInt(5, id);
        pst.executeUpdate();
    }

    /** Retourne toutes les tâches de tous les programmes (utilisé rarement). */
    @Override
    public List<Tache> getData() throws SQLException {
        List<Tache> list = new ArrayList<>();
        ResultSet rs = cnx.createStatement().executeQuery(
                "SELECT * FROM tache ORDER BY programme_id, ordre");
        while (rs.next()) list.add(map(rs));
        return list;
    }

    // ── Méthodes spécifiques ──────────────────────────────────────────────────

    /**
     * Récupère les tâches d'un programme spécifique, triées par ordre croissant.
     * C'est la méthode la plus utilisée — appelée par ScoreService, ProgrammeDetailController, etc.
     */
    public List<Tache> getByProgramme(int programmeId) throws SQLException {
        List<Tache> list = new ArrayList<>();
        PreparedStatement pst = cnx.prepareStatement(
                "SELECT * FROM tache WHERE programme_id = ? ORDER BY ordre ASC");
        pst.setInt(1, programmeId);
        ResultSet rs = pst.executeQuery();
        while (rs.next()) list.add(map(rs));
        return list;
    }

    /**
     * Met à jour uniquement l'état d'une tâche (encours / realisee / Abandonner).
     * Appelé par ProgrammeDetailController.changerEtat() quand l'utilisateur
     * change le ComboBox d'état d'une tâche.
     */
    public void updateEtat(int tacheId, String etat) throws SQLException {
        PreparedStatement pst = cnx.prepareStatement("UPDATE tache SET etat=? WHERE id=?");
        pst.setString(1, etat);
        pst.setInt(2, tacheId);
        pst.executeUpdate();
    }

    /**
     * Supprime toutes les tâches d'un programme.
     * Appelé lors de la suppression en cascade d'un objectif.
     */
    public void deleteByProgramme(int programmeId) throws SQLException {
        PreparedStatement pst = cnx.prepareStatement("DELETE FROM tache WHERE programme_id = ?");
        pst.setInt(1, programmeId);
        pst.executeUpdate();
    }

    /**
     * Valide les données d'une tâche avant insertion ou modification.
     * Retourne une liste d'erreurs (liste vide = données valides).
     */
    public List<String> validate(Tache t) {
        List<String> erreurs = new ArrayList<>();
        // Le titre est obligatoire et doit avoir au moins 2 caractères
        if (t.getTitre() == null || t.getTitre().isBlank())
            erreurs.add("Le titre de la tache est obligatoire");
        else if (t.getTitre().trim().length() < 2)
            erreurs.add("Le titre doit contenir au moins 2 caractères");
        // L'ordre doit être un entier positif
        if (t.getOrdre() <= 0)
            erreurs.add("L ordre doit etre un entier positif");
        // Le programme parent est obligatoire
        if (t.getProgrammeId() <= 0)
            erreurs.add("Le programme est obligatoire");
        return erreurs;
    }

    // ── Mapping BDD → objet Java ──────────────────────────────────────────────

    /**
     * Convertit une ligne de la BDD (ResultSet) en objet Tache.
     * Appelé après chaque requête SELECT.
     * Si l'état en BDD est inconnu, on utilise Abandonner par défaut.
     */
    private Tache map(ResultSet rs) throws SQLException {
        Tache t = new Tache();
        t.setId(rs.getInt("id"));
        t.setOrdre(rs.getInt("ordre"));
        t.setTitre(rs.getString("titre"));
        t.setDescription(rs.getString("description"));
        // Conversion chaîne BDD → enum Etat (avec gestion d'erreur)
        try { t.setEtat(Etat.fromValue(rs.getString("etat"))); }
        catch (Exception e) { t.setEtat(Etat.Abandonner); }
        t.setProgrammeId(rs.getInt("programme_id"));
        return t;
    }
}
