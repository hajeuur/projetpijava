package edu.mentorai.services;

import edu.mentorai.entities.Motivation;
import edu.mentorai.interfaces.IMotivationService;
import edu.mentorai.tools.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MotivationService implements IMotivationService {

    @Override
    public Motivation save(Motivation motivation) throws SQLException {
        String sql = "INSERT INTO motivation (dategeneratiomm, messagemotivant, programme_id) VALUES (?, ?, ?)";
        try (PreparedStatement stmt = DatabaseConnection.getInstance().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setDate(1, Date.valueOf(motivation.getDategeneration()));
            stmt.setString(2, motivation.getMessagemotivant());
            stmt.setInt(3, motivation.getProgrammeId());
            stmt.executeUpdate();
            ResultSet keys = stmt.getGeneratedKeys();
            if (keys.next()) motivation.setId(keys.getInt(1));
        }
        return motivation;
    }

    @Override
    public Motivation findById(int id) throws SQLException {
        String sql = "SELECT * FROM motivation WHERE id = ?";
        try (PreparedStatement stmt = DatabaseConnection.getInstance().prepareStatement(sql)) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return mapRow(rs);
        }
        return null;
    }

    @Override
    public List<Motivation> findAll() throws SQLException {
        List<Motivation> list = new ArrayList<>();
        String sql = "SELECT * FROM motivation";
        try (Statement stmt = DatabaseConnection.getInstance().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    @Override
    public void update(Motivation motivation) throws SQLException {
        String sql = "UPDATE motivation SET dategeneratiomm=?, messagemotivant=?, programme_id=? WHERE id=?";
        try (PreparedStatement stmt = DatabaseConnection.getInstance().prepareStatement(sql)) {
            stmt.setDate(1, Date.valueOf(motivation.getDategeneration()));
            stmt.setString(2, motivation.getMessagemotivant());
            stmt.setInt(3, motivation.getProgrammeId());
            stmt.setInt(4, motivation.getId());
            stmt.executeUpdate();
        }
    }

    @Override
    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM motivation WHERE id = ?";
        try (PreparedStatement stmt = DatabaseConnection.getInstance().prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        }
    }

    @Override
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

    @Override
    public Motivation findLatestByProgramme(int programmeId) throws SQLException {
        String sql = "SELECT * FROM motivation WHERE programme_id = ? ORDER BY dategeneratiomm DESC LIMIT 1";
        try (PreparedStatement stmt = DatabaseConnection.getInstance().prepareStatement(sql)) {
            stmt.setInt(1, programmeId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return mapRow(rs);
        }
        return null;
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