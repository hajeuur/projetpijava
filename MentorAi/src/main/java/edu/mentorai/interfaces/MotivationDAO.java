package edu.mentorai.interfaces;

import edu.mentorai.entities.Motivation;
import edu.mentorai.tools.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MotivationDAO {

    public void save(Motivation motivation) throws SQLException {
        String sql = "INSERT INTO motivation (dategeneratiomm, messagemotivant, programme_id) VALUES (?, ?, ?)";
        try (PreparedStatement stmt = DatabaseConnection.getInstance().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setDate(1, Date.valueOf(motivation.getDategeneration()));
            stmt.setString(2, motivation.getMessagemotivant());
            stmt.setInt(3, motivation.getProgrammeId());
            stmt.executeUpdate();
            ResultSet keys = stmt.getGeneratedKeys();
            if (keys.next()) motivation.setId(keys.getInt(1));
        }
    }

    public List<Motivation> findByProgramme(int programmeId) throws SQLException {
        List<Motivation> list = new ArrayList<>();
        String sql = "SELECT * FROM motivation WHERE programme_id = ? ORDER BY dategeneratiomm DESC";
        try (PreparedStatement stmt = DatabaseConnection.getInstance().prepareStatement(sql)) {
            stmt.setInt(1, programmeId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    public Motivation findLatestByProgramme(int programmeId) throws SQLException {
        String sql = "SELECT * FROM motivation WHERE programme_id = ? ORDER BY dategeneratiomm DESC LIMIT 1";
        try (PreparedStatement stmt = DatabaseConnection.getInstance().prepareStatement(sql)) {
            stmt.setInt(1, programmeId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return mapRow(rs);
        }
        return null;
    }

    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM motivation WHERE id = ?";
        try (PreparedStatement stmt = DatabaseConnection.getInstance().prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        }
    }

    private Motivation mapRow(ResultSet rs) throws SQLException {
        Motivation m = new Motivation();
        m.setId(rs.getInt("id"));
        m.setDategeneration(rs.getDate("dategeneratiomm").toLocalDate());
        m.setMessagemotivant(rs.getString("messagemotivant"));
        m.setProgrammeId(rs.getInt("programme_id"));
        return m;
    }
}