package edu.mentorai.interfaces;

import edu.mentorai.entities.Etat;
import edu.mentorai.entities.Tache;
import edu.mentorai.tools.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TacheDAO {

    public void save(Tache tache) throws SQLException {
        String sql = "INSERT INTO tache (ordre, titre, description, etat, programme_id) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = DatabaseConnection.getInstance().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, tache.getOrdre());
            stmt.setString(2, tache.getTitre());
            stmt.setString(3, tache.getDescription());
            stmt.setString(4, tache.getEtat().getValue());
            stmt.setInt(5, tache.getProgrammeId());
            stmt.executeUpdate();
            ResultSet keys = stmt.getGeneratedKeys();
            if (keys.next()) tache.setId(keys.getInt(1));
        }
    }

    public List<Tache> findByProgramme(int programmeId) throws SQLException {
        List<Tache> list = new ArrayList<>();
        String sql = "SELECT * FROM tache WHERE programme_id = ? ORDER BY ordre ASC";
        try (PreparedStatement stmt = DatabaseConnection.getInstance().prepareStatement(sql)) {
            stmt.setInt(1, programmeId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    public Tache findById(int id) throws SQLException {
        String sql = "SELECT * FROM tache WHERE id = ?";
        try (PreparedStatement stmt = DatabaseConnection.getInstance().prepareStatement(sql)) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return mapRow(rs);
        }
        return null;
    }

    public void update(Tache tache) throws SQLException {
        String sql = "UPDATE tache SET ordre=?, titre=?, description=?, etat=? WHERE id=?";
        try (PreparedStatement stmt = DatabaseConnection.getInstance().prepareStatement(sql)) {
            stmt.setInt(1, tache.getOrdre());
            stmt.setString(2, tache.getTitre());
            stmt.setString(3, tache.getDescription());
            stmt.setString(4, tache.getEtat().getValue());
            stmt.setInt(5, tache.getId());
            stmt.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM tache WHERE id = ?";
        try (PreparedStatement stmt = DatabaseConnection.getInstance().prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        }
    }

    public void deleteByProgramme(int programmeId) throws SQLException {
        String sql = "DELETE FROM tache WHERE programme_id = ?";
        try (PreparedStatement stmt = DatabaseConnection.getInstance().prepareStatement(sql)) {
            stmt.setInt(1, programmeId);
            stmt.executeUpdate();
        }
    }

    private Tache mapRow(ResultSet rs) throws SQLException {
        Tache t = new Tache();
        t.setId(rs.getInt("id"));
        t.setOrdre(rs.getInt("ordre"));
        t.setTitre(rs.getString("titre"));
        t.setDescription(rs.getString("description"));
        t.setEtat(Etat.fromValue(rs.getString("etat")));
        t.setProgrammeId(rs.getInt("programme_id"));
        return t;
    }
}