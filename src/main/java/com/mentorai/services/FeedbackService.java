package com.mentorai.services;

import com.mentorai.models.Feedback;
import com.mentorai.utils.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class FeedbackService {

    private Connection connection;

    public FeedbackService() {
        connection = MyConnection.getInstance();
    }

    // ===================== CREATE =====================
    public void add(Feedback feedback) {
        String query = "INSERT INTO feedback (contenu, note, datefeedback, typefeedback, etatfeedback, traitement_id, utilisateur_id) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try {
            PreparedStatement ps = connection.prepareStatement(query);
            ps.setString(1, feedback.getContenu());
            ps.setInt(2, feedback.getNote());
            ps.setDate(3, Date.valueOf(feedback.getDatefeedback()));
            ps.setString(4, feedback.getTypefeedback());
            ps.setString(5, feedback.getEtatfeedback());

            if (feedback.getTraitementId() == 0) {
                ps.setNull(6, Types.INTEGER);
            } else {
                ps.setInt(6, feedback.getTraitementId());
            }

            ps.setInt(7, feedback.getUtilisateurId());
            ps.executeUpdate();
            System.out.println("✅ Feedback ajouté avec succès !");

        } catch (SQLException e) {
            System.out.println("❌ Erreur lors de l'ajout : " + e.getMessage());
        }
    }

    // ===================== CONTRÔLE D'UNICITÉ =====================
    // ✅ Vérifie si l'utilisateur a déjà envoyé exactement le même message
    public boolean existe(int utilisateurId, String contenu) {
        String query = "SELECT * FROM feedback WHERE utilisateur_id = ? AND contenu = ?";
        try {
            PreparedStatement ps = connection.prepareStatement(query);
            ps.setInt(1, utilisateurId);
            ps.setString(2, contenu);
            ResultSet rs = ps.executeQuery();
            return rs.next(); // true = existe déjà, false = n'existe pas
        } catch (SQLException e) {
            System.out.println("❌ Erreur contrôle unicité : " + e.getMessage());
        }
        return false;
    }

    // ===================== READ =====================
    public List<Feedback> getAll() {
        List<Feedback> feedbacks = new ArrayList<>();
        String query = "SELECT * FROM feedback";
        try {
            Statement st = connection.createStatement();
            ResultSet rs = st.executeQuery(query);
            while (rs.next()) {
                Feedback f = new Feedback(
                        rs.getInt("id"),
                        rs.getString("contenu"),
                        rs.getInt("note"),
                        rs.getDate("datefeedback").toLocalDate(),
                        rs.getString("typefeedback"),
                        rs.getString("etatfeedback"),
                        rs.getInt("traitement_id"),
                        rs.getInt("utilisateur_id")
                );
                feedbacks.add(f);
            }
        } catch (SQLException e) {
            System.out.println("❌ Erreur lors de la récupération : " + e.getMessage());
        }
        return feedbacks;
    }

    // ===================== READ par utilisateur =====================
    public List<Feedback> getByUtilisateur(int utilisateurId) {
        List<Feedback> feedbacks = new ArrayList<>();
        String query = "SELECT * FROM feedback WHERE utilisateur_id = ? ORDER BY datefeedback DESC";
        try {
            PreparedStatement ps = connection.prepareStatement(query);
            ps.setInt(1, utilisateurId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Feedback f = new Feedback(
                        rs.getInt("id"),
                        rs.getString("contenu"),
                        rs.getInt("note"),
                        rs.getDate("datefeedback").toLocalDate(),
                        rs.getString("typefeedback"),
                        rs.getString("etatfeedback"),
                        rs.getInt("traitement_id"),
                        rs.getInt("utilisateur_id")
                );
                feedbacks.add(f);
            }
        } catch (SQLException e) {
            System.out.println("❌ Erreur getByUtilisateur : " + e.getMessage());
        }
        return feedbacks;
    }

    // ===================== UPDATE =====================
    public void update(Feedback feedback) {
        String query = "UPDATE feedback SET contenu=?, note=?, datefeedback=?, typefeedback=?, etatfeedback=?, traitement_id=? WHERE id=?";
        try {
            PreparedStatement ps = connection.prepareStatement(query);
            ps.setString(1, feedback.getContenu());
            ps.setInt(2, feedback.getNote());
            ps.setDate(3, Date.valueOf(feedback.getDatefeedback()));
            ps.setString(4, feedback.getTypefeedback());
            ps.setString(5, feedback.getEtatfeedback());

            if (feedback.getTraitementId() == 0) {
                ps.setNull(6, Types.INTEGER);
            } else {
                ps.setInt(6, feedback.getTraitementId());
            }

            ps.setInt(7, feedback.getId());
            ps.executeUpdate();
            System.out.println("✅ Feedback modifié avec succès !");
        } catch (SQLException e) {
            System.out.println("❌ Erreur lors de la modification : " + e.getMessage());
        }
    }

    // ===================== DELETE =====================
    public void delete(int id) {
        String query = "DELETE FROM feedback WHERE id=?";
        try {
            PreparedStatement ps = connection.prepareStatement(query);
            ps.setInt(1, id);
            ps.executeUpdate();
            System.out.println("✅ Feedback supprimé avec succès !");
        } catch (SQLException e) {
            System.out.println("❌ Erreur lors de la suppression : " + e.getMessage());
        }
    }
}