package edu.connection3a36.services;

import edu.connection3a36.interfaces.IService;
import edu.connection3a36.tools.MyConnection;
import edu.connection3a36.entities.Motivation;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Gère les messages de motivation générés par l'IA Ollama.
 * Implémente IService<Motivation> pour les opérations CRUD de base.
 *
 * Attention : la colonne date en BDD s'appelle "dategeneratiomm" (double 'm').
 * C'est une erreur de frappe dans la BDD qui est conservée intentionnellement.
 *
 * Méthodes supplémentaires :
 *   getByProgramme()       → tous les messages d'un programme
 *   getLatestByProgramme() → le dernier message (affiché dans ProgrammeDetail)
 *   deleteByProgramme()    → supprime tous les messages (cascade lors de suppression)
 */
public class MotivationService implements IService<Motivation> {

    /** Connexion unique à la BDD (pattern Singleton) */
    private final Connection cnx = MyConnection.getInstance().getCnx();

    // ── CRUD de base ──────────────────────────────────────────────────────────

    /** Insère un nouveau message de motivation en BDD. */
    @Override
    public void addEntity(Motivation m) throws SQLException {
        // Note : le nom de colonne "dategeneratiomm" est une typo dans la BDD
        String req = "INSERT INTO motivation (dategeneratiomm, messagemotivant, programme_id) VALUES (?,?,?)";
        PreparedStatement pst = cnx.prepareStatement(req, Statement.RETURN_GENERATED_KEYS);
        // Si la date est null, on utilise la date du jour
        pst.setDate(1, Date.valueOf(m.getDategeneration() != null ? m.getDategeneration() : LocalDate.now()));
        pst.setString(2, m.getMessagemotivant());
        pst.setInt(3, m.getProgrammeId());
        pst.executeUpdate();
        ResultSet rs = pst.getGeneratedKeys();
        if (rs.next()) m.setId(rs.getInt(1));
    }

    /** Supprime un message de motivation par son ID. */
    @Override
    public void deleteEntity(Motivation m) throws SQLException {
        PreparedStatement pst = cnx.prepareStatement("DELETE FROM motivation WHERE id = ?");
        pst.setInt(1, m.getId());
        pst.executeUpdate();
    }

    /** Met à jour le texte et la date d'un message de motivation. */
    @Override
    public void updateEntity(int id, Motivation m) throws SQLException {
        PreparedStatement pst = cnx.prepareStatement(
                "UPDATE motivation SET messagemotivant=?, dategeneratiomm=? WHERE id=?");
        pst.setString(1, m.getMessagemotivant());
        pst.setDate(2, Date.valueOf(m.getDategeneration() != null ? m.getDategeneration() : LocalDate.now()));
        pst.setInt(3, id);
        pst.executeUpdate();
    }

    /** Retourne tous les messages, triés du plus récent au plus ancien. */
    @Override
    public List<Motivation> getData() throws SQLException {
        List<Motivation> list = new ArrayList<>();
        ResultSet rs = cnx.createStatement().executeQuery(
                "SELECT * FROM motivation ORDER BY dategeneratiomm DESC");
        while (rs.next()) list.add(map(rs));
        return list;
    }

    // ── Méthodes spécifiques ──────────────────────────────────────────────────

    /**
     * Retourne tous les messages de motivation d'un programme,
     * triés du plus récent au plus ancien.
     */
    public List<Motivation> getByProgramme(int programmeId) throws SQLException {
        List<Motivation> list = new ArrayList<>();
        PreparedStatement pst = cnx.prepareStatement(
                "SELECT * FROM motivation WHERE programme_id = ? ORDER BY dategeneratiomm DESC");
        pst.setInt(1, programmeId);
        ResultSet rs = pst.executeQuery();
        while (rs.next()) list.add(map(rs));
        return list;
    }

    /**
     * Retourne uniquement le dernier message de motivation d'un programme.
     * Utilisé dans ProgrammeDetailController.charger() pour afficher le message actuel.
     * Retourne null si aucun message n'a encore été généré.
     */
    public Motivation getLatestByProgramme(int programmeId) throws SQLException {
        PreparedStatement pst = cnx.prepareStatement(
                "SELECT * FROM motivation WHERE programme_id = ? " +
                "ORDER BY dategeneratiomm DESC LIMIT 1");
        pst.setInt(1, programmeId);
        ResultSet rs = pst.executeQuery();
        if (rs.next()) return map(rs);
        return null; // aucun message encore généré
    }

    /**
     * Supprime tous les messages de motivation d'un programme.
     * Appelé lors de la suppression en cascade d'un objectif.
     */
    public void deleteByProgramme(int programmeId) throws SQLException {
        PreparedStatement pst = cnx.prepareStatement(
                "DELETE FROM motivation WHERE programme_id = ?");
        pst.setInt(1, programmeId);
        pst.executeUpdate();
    }

    // ── Mapping BDD → objet Java ──────────────────────────────────────────────

    /** Convertit une ligne de la BDD (ResultSet) en objet Motivation. */
    private Motivation map(ResultSet rs) throws SQLException {
        Motivation m = new Motivation();
        m.setId(rs.getInt("id"));
        // Conversion java.sql.Date → java.time.LocalDate
        Date dg = rs.getDate("dategeneratiomm"); // typo BDD intentionnelle
        if (dg != null) m.setDategeneration(dg.toLocalDate());
        m.setMessagemotivant(rs.getString("messagemotivant"));
        m.setProgrammeId(rs.getInt("programme_id"));
        return m;
    }
}
