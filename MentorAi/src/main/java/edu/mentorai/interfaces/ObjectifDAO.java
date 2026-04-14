package edu.mentorai.interfaces;

import edu.mentorai.entities.Objectif;
import edu.mentorai.entities.Statutobj;
import edu.mentorai.tools.DatabaseConnection;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ObjectifDAO {

    // ✅ CREATE
    public void save(Objectif objectif) throws SQLException {
        String sql = "INSERT INTO objectif (titre, description, datedebut, datefin, statut, programme_id, utilisateur_id) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = DatabaseConnection.getInstance().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, objectif.getTitre());
            stmt.setString(2, objectif.getDescription());
            stmt.setDate(3, Date.valueOf(objectif.getDatedebut()));
            stmt.setDate(4, Date.valueOf(objectif.getDatefin()));
            stmt.setString(5, objectif.getStatut().getValue());
            stmt.setInt(6, objectif.getProgramme().getId());
            stmt.setInt(7, objectif.getUtilisateurId());
            stmt.executeUpdate();
            ResultSet keys = stmt.getGeneratedKeys();
            if (keys.next()) objectif.setId(keys.getInt(1));
        }
    }

    // ✅ READ ALL
    public List<Objectif> findAll() throws SQLException {
        List<Objectif> list = new ArrayList<>();
        String sql = "SELECT * FROM objectif ORDER BY datedebut ASC";
        try (Statement stmt = DatabaseConnection.getInstance().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    // ✅ READ BY ID
    public Objectif findById(int id) throws SQLException {
        String sql = "SELECT * FROM objectif WHERE id = ?";
        try (PreparedStatement stmt = DatabaseConnection.getInstance().prepareStatement(sql)) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return mapRow(rs);
        }
        return null;
    }

    // ✅ READ BY UTILISATEUR
    public List<Objectif> findByUtilisateur(int utilisateurId) throws SQLException {
        List<Objectif> list = new ArrayList<>();
        String sql = "SELECT * FROM objectif WHERE utilisateur_id = ? ORDER BY datedebut ASC";
        try (PreparedStatement stmt = DatabaseConnection.getInstance().prepareStatement(sql)) {
            stmt.setInt(1, utilisateurId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    // ✅ UPDATE
    public void update(Objectif objectif) throws SQLException {
        String sql = "UPDATE objectif SET titre=?, description=?, datedebut=?, datefin=?, statut=? WHERE id=?";
        try (PreparedStatement stmt = DatabaseConnection.getInstance().prepareStatement(sql)) {
            stmt.setString(1, objectif.getTitre());
            stmt.setString(2, objectif.getDescription());
            stmt.setDate(3, Date.valueOf(objectif.getDatedebut()));
            stmt.setDate(4, Date.valueOf(objectif.getDatefin()));
            stmt.setString(5, objectif.getStatut().getValue());
            stmt.setInt(6, objectif.getId());
            stmt.executeUpdate();
        }
    }

    // ✅ DELETE
    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM objectif WHERE id = ?";
        try (PreparedStatement stmt = DatabaseConnection.getInstance().prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        }
    }

    // ✅ SEARCH BY TITRE
    public List<Objectif> searchByTitre(String titre) throws SQLException {
        List<Objectif> list = new ArrayList<>();
        String sql = "SELECT * FROM objectif WHERE titre LIKE ?";
        try (PreparedStatement stmt = DatabaseConnection.getInstance().prepareStatement(sql)) {
            stmt.setString(1, "%" + titre + "%");
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    // ✅ MAP ROW
    private Objectif mapRow(ResultSet rs) throws SQLException {
        Objectif o = new Objectif();
        o.setId(rs.getInt("id"));
        o.setTitre(rs.getString("titre"));
        o.setDescription(rs.getString("description"));
        o.setDatedebut(rs.getDate("datedebut").toLocalDate());
        o.setDatefin(rs.getDate("datefin").toLocalDate());
        o.setStatut(Statutobj.fromValue(rs.getString("statut")));
        o.setUtilisateurId(rs.getInt("utilisateur_id"));
        return o;
    }
}