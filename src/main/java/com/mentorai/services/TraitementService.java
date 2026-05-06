package com.mentorai.services;

import com.mentorai.models.Traitement;
import com.mentorai.utils.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TraitementService {

    public TraitementService() {
    }

    // ============ CREATE ============
    public void add(Traitement t) {
        String query = "INSERT INTO traitement (typetraitement, description, datetraitement, decision) VALUES (?, ?, ?, ?)";
        try (Connection connection = MyConnection.getInstance();
             PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, t.getTypetraitement());
            ps.setString(2, t.getDescription());
            ps.setDate(3, Date.valueOf(t.getDatetraitement()));
            ps.setString(4, t.getDecision());
            ps.executeUpdate();
            System.out.println("✅ Traitement ajouté !");
        } catch (SQLException e) {
            System.out.println("❌ Erreur : " + e.getMessage());
        }
    }

    // ============ READ ============
    public List<Traitement> getAll() {
        List<Traitement> list = new ArrayList<>();
        String query = "SELECT * FROM traitement";
        try (Connection connection = MyConnection.getInstance();
             Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(query)) {
            while (rs.next()) {
                Traitement t = new Traitement(
                        rs.getInt("id"),
                        rs.getString("typetraitement"),
                        rs.getString("description"),
                        rs.getDate("datetraitement").toLocalDate(),
                        rs.getString("decision")
                );
                list.add(t);
            }
        } catch (SQLException e) {
            System.out.println("❌ Erreur : " + e.getMessage());
        }
        return list;
    }

    // ============ UPDATE ============
    public void update(Traitement t) {
        String query = "UPDATE traitement SET typetraitement=?, description=?, datetraitement=?, decision=? WHERE id=?";
        try (Connection connection = MyConnection.getInstance();
             PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, t.getTypetraitement());
            ps.setString(2, t.getDescription());
            ps.setDate(3, Date.valueOf(t.getDatetraitement()));
            ps.setString(4, t.getDecision());
            ps.setInt(5, t.getId());
            ps.executeUpdate();
            System.out.println("✅ Traitement modifié !");
        } catch (SQLException e) {
            System.out.println("❌ Erreur : " + e.getMessage());
        }
    }

    // ============ DELETE ============
    public void delete(int id) {
        String query = "DELETE FROM traitement WHERE id=?";
        try (Connection connection = MyConnection.getInstance();
             PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, id);
            ps.executeUpdate();
            System.out.println("✅ Traitement supprimé !");
        } catch (SQLException e) {
            System.out.println("❌ Erreur : " + e.getMessage());
        }
    }

    // Récupère l'id du dernier traitement inséré
    public int getDernierIdInsere() {
        String query = "SELECT MAX(id) FROM traitement";
        try (Connection connection = MyConnection.getInstance();
             Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(query)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.out.println("❌ Erreur getDernierIdInsere : " + e.getMessage());
        }
        return -1;
    }

    // Récupérer un traitement par son id
    public Traitement getById(int id) {
        String query = "SELECT * FROM traitement WHERE id = ?";
        try (Connection connection = MyConnection.getInstance();
             PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Traitement(
                            rs.getInt("id"),
                            rs.getString("typetraitement"),
                            rs.getString("description"),
                            rs.getDate("datetraitement").toLocalDate(),
                            rs.getString("decision")
                    );
                }
            }
        } catch (SQLException e) {
            System.out.println("❌ Erreur getById : " + e.getMessage());
        }
        return null;
    }
}