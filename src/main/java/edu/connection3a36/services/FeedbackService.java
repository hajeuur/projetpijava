package edu.connection3a36.services;

import edu.connection3a36.models.Feedback;
import edu.connection3a36.tools.MyConnection;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class FeedbackService {

    private final Connection cnx = MyConnection.getInstance().getCnx();

    public void add(Feedback f) {
        String req = "INSERT INTO feedback (contenu, note, datefeedback, typefeedback, etatfeedback, traitement_id, utilisateur_id) VALUES (?,?,?,?,?,?,?)";
        try (PreparedStatement pst = cnx.prepareStatement(req)) {
            pst.setString(1, f.getContenu());
            pst.setInt(2, f.getNote());
            pst.setDate(3, Date.valueOf(f.getDatefeedback() != null ? f.getDatefeedback() : LocalDate.now()));
            pst.setString(4, f.getTypefeedback());
            pst.setString(5, f.getEtatfeedback());
            pst.setInt(6, f.getTraitementId());
            pst.setInt(7, f.getUtilisateurId());
            pst.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erreur add Feedback : " + e.getMessage());
        }
    }

    public void update(Feedback f) {
        String req = "UPDATE feedback SET contenu=?, note=?, typefeedback=?, etatfeedback=?, traitement_id=? WHERE id=?";
        try (PreparedStatement pst = cnx.prepareStatement(req)) {
            pst.setString(1, f.getContenu());
            pst.setInt(2, f.getNote());
            pst.setString(3, f.getTypefeedback());
            pst.setString(4, f.getEtatfeedback());
            pst.setInt(5, f.getTraitementId());
            pst.setInt(6, f.getId());
            pst.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erreur update Feedback : " + e.getMessage());
        }
    }

    public void delete(int id) {
        try (PreparedStatement pst = cnx.prepareStatement("DELETE FROM feedback WHERE id=?")) {
            pst.setInt(1, id);
            pst.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erreur delete Feedback : " + e.getMessage());
        }
    }

    public List<Feedback> getAll() {
        List<Feedback> list = new ArrayList<>();
        try (ResultSet rs = cnx.createStatement().executeQuery(
                "SELECT * FROM feedback ORDER BY datefeedback DESC")) {
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) {
            System.err.println("Erreur getAll Feedback : " + e.getMessage());
        }
        return list;
    }

    public List<Feedback> getByUtilisateur(int utilisateurId) {
        List<Feedback> list = new ArrayList<>();
        try (PreparedStatement pst = cnx.prepareStatement(
                "SELECT * FROM feedback WHERE utilisateur_id=? ORDER BY datefeedback DESC")) {
            pst.setInt(1, utilisateurId);
            ResultSet rs = pst.executeQuery();
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) {
            System.err.println("Erreur getByUtilisateur : " + e.getMessage());
        }
        return list;
    }

    public boolean existe(int utilisateurId, String contenu) {
        try (PreparedStatement pst = cnx.prepareStatement(
                "SELECT COUNT(*) FROM feedback WHERE utilisateur_id=? AND contenu=?")) {
            pst.setInt(1, utilisateurId);
            pst.setString(2, contenu);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) return rs.getInt(1) > 0;
        } catch (SQLException e) {
            System.err.println("Erreur existe : " + e.getMessage());
        }
        return false;
    }

    private Feedback map(ResultSet rs) throws SQLException {
        return new Feedback(
                rs.getInt("id"),
                rs.getString("contenu"),
                rs.getInt("note"),
                rs.getDate("datefeedback").toLocalDate(),
                rs.getString("typefeedback"),
                rs.getString("etatfeedback"),
                rs.getInt("traitement_id"),
                rs.getInt("utilisateur_id")
        );
    }
}
