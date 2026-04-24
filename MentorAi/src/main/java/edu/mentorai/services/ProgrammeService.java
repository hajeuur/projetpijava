package edu.mentorai.services;

import edu.mentorai.entities.Medaille;
import edu.mentorai.entities.Programme;
import edu.mentorai.interfaces.IProgrammeService;
import edu.mentorai.tools.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProgrammeService implements IProgrammeService {

    @Override
    public Programme save(Programme programme) throws SQLException {
        String sql = "INSERT INTO programme (titre, dategeneration, score_pourcentage) VALUES (?, ?, ?)";
        try (PreparedStatement stmt = DatabaseConnection.getInstance()
                .prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, programme.getTitre());
            stmt.setDate(2, Date.valueOf(programme.getDategeneration()));
            stmt.setInt(3, programme.getScorePourcentage());
            stmt.executeUpdate();
            ResultSet keys = stmt.getGeneratedKeys();
            if (keys.next()) programme.setId(keys.getInt(1));
        }
        return programme;
    }

    @Override
    public Programme findById(int id) throws SQLException {
        String sql = "SELECT * FROM programme WHERE id = ?";
        try (PreparedStatement stmt = DatabaseConnection.getInstance().prepareStatement(sql)) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return mapRow(rs);
        }
        return null;
    }

    @Override
    public List<Programme> findAll() throws SQLException {
        List<Programme> list = new ArrayList<>();
        String sql = "SELECT * FROM programme";
        try (Statement stmt = DatabaseConnection.getInstance().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    @Override
    public Programme findByObjectifId(int objectifId) throws SQLException {
        String sql = "SELECT p.* FROM programme p INNER JOIN objectif o ON o.programme_id = p.id WHERE o.id = ?";
        try (PreparedStatement stmt = DatabaseConnection.getInstance().prepareStatement(sql)) {
            stmt.setInt(1, objectifId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return mapRow(rs);
        }
        return null;
    }

    @Override
    public void updateScore(int programmeId, int score, Medaille medaille) throws SQLException {
        String sql = "UPDATE programme SET score_pourcentage=?, meilleure_medaille=? WHERE id=?";
        try (PreparedStatement stmt = DatabaseConnection.getInstance().prepareStatement(sql)) {
            stmt.setInt(1, score);
            stmt.setString(2, medaille != null ? medaille.getValue() : null);
            stmt.setInt(3, programmeId);
            stmt.executeUpdate();
        }
    }

    @Override
    public void update(Programme programme) throws SQLException {
        String sql = "UPDATE programme SET titre=?, dategeneration=?, score_pourcentage=? WHERE id=?";
        try (PreparedStatement stmt = DatabaseConnection.getInstance().prepareStatement(sql)) {
            stmt.setString(1, programme.getTitre());
            stmt.setDate(2, Date.valueOf(programme.getDategeneration()));
            stmt.setInt(3, programme.getScorePourcentage());
            stmt.setInt(4, programme.getId());
            stmt.executeUpdate();
        }
    }

    @Override
    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM programme WHERE id = ?";
        try (PreparedStatement stmt = DatabaseConnection.getInstance().prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        }
    }

    private Programme mapRow(ResultSet rs) throws SQLException {
        Programme p = new Programme();
        p.setId(rs.getInt("id"));
        p.setTitre(rs.getString("titre"));
        p.setDategeneration(rs.getDate("dategeneration").toLocalDate());
        p.setScorePourcentage(rs.getInt("score_pourcentage"));
        String medaille = rs.getString("meilleure_medaille");
        if (medaille != null) p.setMeilleureMedaille(Medaille.fromValue(medaille));
        return p;
    }
}