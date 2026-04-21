package edu.mentorai.services;

import edu.mentorai.entities.Objectif;
import edu.mentorai.entities.Statutobj;
import edu.mentorai.interfaces.IObjectifService;
import edu.mentorai.tools.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ObjectifService implements IObjectifService {

    // ✅ Vérification unicité titre + description
    private boolean existsWithTitreAndDescription(String titre, String description, int excludeId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM objectif WHERE LOWER(titre) = LOWER(?) AND LOWER(description) = LOWER(?) AND id != ?";
        try (PreparedStatement stmt = DatabaseConnection.getInstance().prepareStatement(sql)) {
            stmt.setString(1, titre);
            stmt.setString(2, description);
            stmt.setInt(3, excludeId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getInt(1) > 0;
        }
        return false;
    }

    @Override
    public Objectif save(Objectif objectif) throws SQLException {
        // ✅ Unicité : excludeId = 0 car pas encore en base
        if (existsWithTitreAndDescription(objectif.getTitre(), objectif.getDescription(), 0)) {
            throw new SQLException("Un objectif avec le même titre et la même description existe déjà.");
        }

        String sql = "INSERT INTO objectif (titre, description, datedebut, datefin, statut, programme_id, utilisateur_id) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = DatabaseConnection.getInstance().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, objectif.getTitre());
            stmt.setString(2, objectif.getDescription());
            stmt.setDate(3, Date.valueOf(objectif.getDatedebut()));
            stmt.setDate(4, Date.valueOf(objectif.getDatefin()));
            stmt.setString(5, objectif.getStatut().getValue());
            if (objectif.getProgramme() != null)
                stmt.setInt(6, objectif.getProgramme().getId());
            else
                stmt.setNull(6, Types.INTEGER);
            stmt.setInt(7, objectif.getUtilisateurId());
            stmt.executeUpdate();
            ResultSet keys = stmt.getGeneratedKeys();
            if (keys.next()) objectif.setId(keys.getInt(1));
        }
        return objectif;
    }

    @Override
    public Objectif findById(int id) throws SQLException {
        String sql = "SELECT * FROM objectif WHERE id = ?";
        try (PreparedStatement stmt = DatabaseConnection.getInstance().prepareStatement(sql)) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return mapRow(rs);
        }
        return null;
    }

    @Override
    public List<Objectif> findAll() throws SQLException {
        List<Objectif> list = new ArrayList<>();
        String sql = "SELECT * FROM objectif ORDER BY datedebut ASC";
        try (Statement stmt = DatabaseConnection.getInstance().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    @Override
    public void update(Objectif objectif) throws SQLException {
        // ✅ Unicité : excludeId = objectif.getId() pour ignorer lui-même
        if (existsWithTitreAndDescription(objectif.getTitre(), objectif.getDescription(), objectif.getId())) {
            throw new SQLException("Un autre objectif avec le même titre et la même description existe déjà.");
        }

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

    @Override
    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM objectif WHERE id = ?";
        try (PreparedStatement stmt = DatabaseConnection.getInstance().prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        }
    }

    @Override
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

    @Override
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