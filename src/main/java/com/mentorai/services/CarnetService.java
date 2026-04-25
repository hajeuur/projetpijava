package com.mentorai.services;

import com.mentorai.entities.Carnet;
import com.mentorai.utils.MyConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class CarnetService {

    private final Connection connection;

    public CarnetService() {
        this.connection = MyConnection.getInstance().getConnection();
        checkDatabaseStructure(); // 🔥 ONLY CHECK (no creation)
    }

    // 🔥 VERIFY DB STRUCTURE (NO CREATION)
    private void checkDatabaseStructure() {
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SHOW COLUMNS FROM carnet")) {

            boolean hasCouleur = false;
            boolean hasVisibilite = false;

            while (rs.next()) {
                String col = rs.getString("Field");
                if ("couleur".equalsIgnoreCase(col)) hasCouleur = true;
                if ("visibilite".equalsIgnoreCase(col)) hasVisibilite = true;
            }

            if (!hasCouleur || !hasVisibilite) {
                System.err.println("❌ DATABASE ERROR:");
                System.err.println("Missing columns:");
                if (!hasCouleur) System.err.println(" - couleur");
                if (!hasVisibilite) System.err.println(" - visibilite");
                System.err.println("👉 Fix manually in phpMyAdmin.");
            } else {
                System.out.println("✅ DB structure OK");
            }

        } catch (SQLException e) {
            System.err.println("❌ Structure check failed: " + e.getMessage());
        }
    }

    // ─── CREATE ────────────────────────────────────────────────────────────────

    public boolean create(Carnet carnet) {
        String sql = "INSERT INTO carnet (titre, contenu, date_creation, date_modification, couleur, visibilite) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, carnet.getTitre());
            ps.setString(2, carnet.getContenu());

            LocalDateTime now = LocalDateTime.now();
            ps.setTimestamp(3, Timestamp.valueOf(now));
            ps.setTimestamp(4, Timestamp.valueOf(now));

            ps.setString(5, carnet.getCouleur() != null ? carnet.getCouleur() : "#ffffff");
            ps.setString(6, carnet.getVisibilite() != null ? carnet.getVisibilite() : "visible");

            int rows = ps.executeUpdate();

            if (rows > 0) {
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        carnet.setId(keys.getInt(1));
                        carnet.setDateCreation(now);
                        carnet.setDateModification(now);
                    }
                }
                return true;
            }

        } catch (SQLException e) {
            System.err.println("❌ Erreur create : " + e.getMessage());
        }
        return false;
    }

    // ─── READ ALL ───────────────────────────────────────────────────────────────

    public List<Carnet> findAll() {
        List<Carnet> list = new ArrayList<>();
        String sql = "SELECT * FROM carnet ORDER BY date_modification DESC";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                list.add(mapRow(rs));
            }

        } catch (SQLException e) {
            System.err.println("❌ Erreur findAll : " + e.getMessage());
        }

        return list;
    }

    // ─── UPDATE ─────────────────────────────────────────────────────────────────

    public boolean update(Carnet carnet) {
        String sql = "UPDATE carnet SET titre = ?, contenu = ?, date_modification = ?, couleur = ?, visibilite = ? WHERE id = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {

            LocalDateTime now = LocalDateTime.now();

            ps.setString(1, carnet.getTitre());
            ps.setString(2, carnet.getContenu());
            ps.setTimestamp(3, Timestamp.valueOf(now));
            ps.setString(4, carnet.getCouleur());
            ps.setString(5, carnet.getVisibilite());
            ps.setInt(6, carnet.getId());

            boolean ok = ps.executeUpdate() > 0;

            if (ok) carnet.setDateModification(now);

            return ok;

        } catch (SQLException e) {
            System.err.println("❌ Erreur update : " + e.getMessage());
        }

        return false;
    }

    // ─── DELETE ─────────────────────────────────────────────────────────────────

    public boolean delete(int id) {
        String sql = "DELETE FROM carnet WHERE id = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("❌ Erreur delete : " + e.getMessage());
        }

        return false;
    }

    // ─── SEARCH ─────────────────────────────────────────────────────────────────

    public List<Carnet> search(String keyword) {
        List<Carnet> list = new ArrayList<>();

        String sql = "SELECT * FROM carnet WHERE titre LIKE ? OR contenu LIKE ? ORDER BY date_modification DESC";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {

            String like = "%" + keyword + "%";
            ps.setString(1, like);
            ps.setString(2, like);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }

        } catch (SQLException e) {
            System.err.println("❌ Erreur search : " + e.getMessage());
        }

        return list;
    }

    // ─── EXISTS BY TITRE ────────────────────────────────────────────────────────

    /**
     * Returns true if any row in carnet has the given titre (case-insensitive),
     * excluding the row with the given id (pass 0 when creating a new note).
     */
    public boolean existsByTitre(String titre, int excludeId) {
        String sql = "SELECT COUNT(*) FROM carnet WHERE LOWER(titre) = LOWER(?) AND id != ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, titre);
            ps.setInt(2, excludeId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur existsByTitre : " + e.getMessage());
        }
        return false;
    }

    // ─── MAPPER ─────────────────────────────────────────────────────────────────

    private Carnet mapRow(ResultSet rs) throws SQLException {
        Carnet c = new Carnet();

        c.setId(rs.getInt("id"));
        c.setTitre(rs.getString("titre"));
        c.setContenu(rs.getString("contenu"));
        c.setCouleur(rs.getString("couleur"));
        c.setVisibilite(rs.getString("visibilite"));

        Timestamp created  = rs.getTimestamp("date_creation");
        Timestamp modified = rs.getTimestamp("date_modification");

        if (created  != null) c.setDateCreation(created.toLocalDateTime());
        if (modified != null) c.setDateModification(modified.toLocalDateTime());

        return c;
    }
}