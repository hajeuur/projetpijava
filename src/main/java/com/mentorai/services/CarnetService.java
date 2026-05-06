package com.mentorai.services;

import com.mentorai.entities.Carnet;
import com.mentorai.entities.PlanningEtude;
import com.mentorai.utils.MyConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class CarnetService {

    public CarnetService() {
        checkDatabaseStructure(); // 🔥 ONLY CHECK (no creation)
    }

    // 🔥 VERIFY DB STRUCTURE (NO CREATION)
    private void checkDatabaseStructure() {
        try (Connection connection = MyConnection.getInstance();
             Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SHOW COLUMNS FROM carnet")) {

            boolean hasCouleur = false;
            boolean hasVisibilite = false;

            while (rs.next()) {
                String col = rs.getString("Field");
                if ("couleur".equalsIgnoreCase(col)) hasCouleur = true;
                if ("visibilite".equalsIgnoreCase(col)) hasVisibilite = true;
            }

            if (!hasCouleur) System.err.println("⚠️ Column 'couleur' missing in carnet table!");
            if (!hasVisibilite) System.err.println("⚠️ Column 'visibilite' missing in carnet table!");

        } catch (SQLException e) {
            System.err.println("❌ Erreur checkDatabaseStructure : " + e.getMessage());
        }
    }

    // ===================== CRUD =====================

    public List<Carnet> findAll() {
        List<Carnet> list = new ArrayList<>();
        // Specify columns explicitly to avoid reading removed columns like planning_id
        String sql = "SELECT id, titre, contenu, date_creation, date_modification, couleur, visibilite FROM carnet ORDER BY date_modification DESC";
        try (Connection connection = MyConnection.getInstance();
             Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            System.err.println("❌ Erreur findAll : " + e.getMessage());
        }
        return list;
    }

    public List<Carnet> search(String query) {
        List<Carnet> list = new ArrayList<>();
        String sql = "SELECT id, titre, contenu, date_creation, date_modification, couleur, visibilite FROM carnet " +
                     "WHERE titre LIKE ? OR contenu LIKE ? ORDER BY date_modification DESC";
        try (Connection connection = MyConnection.getInstance();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, "%" + query + "%");
            ps.setString(2, "%" + query + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur search : " + e.getMessage());
        }
        return list;
    }

    public void save(Carnet c) {
        if (c.getId() == 0) create(c);
        else update(c);
    }

    public boolean create(Carnet c) {
        String sql = "INSERT INTO carnet (titre, contenu, couleur, visibilite, date_creation, date_modification) VALUES (?,?,?,?,?,?)";
        try (Connection connection = MyConnection.getInstance();
             PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, c.getTitre());
            ps.setString(2, c.getContenu());
            ps.setString(3, c.getCouleur());
            ps.setString(4, c.getVisibilite());
            ps.setTimestamp(5, Timestamp.valueOf(LocalDateTime.now()));
            ps.setTimestamp(6, Timestamp.valueOf(LocalDateTime.now()));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) c.setId(keys.getInt(1));
            }
            return true;
        } catch (SQLException e) {
            System.err.println("❌ Erreur create : " + e.getMessage());
            return false;
        }
    }

    public boolean update(Carnet c) {
        String sql = "UPDATE carnet SET titre=?, contenu=?, couleur=?, visibilite=?, date_modification=? WHERE id=?";
        try (Connection connection = MyConnection.getInstance();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, c.getTitre());
            ps.setString(2, c.getContenu());
            ps.setString(3, c.getCouleur());
            ps.setString(4, c.getVisibilite());
            ps.setTimestamp(5, Timestamp.valueOf(LocalDateTime.now()));
            ps.setInt(6, c.getId());
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("❌ Erreur update : " + e.getMessage());
            return false;
        }
    }

    public boolean delete(int id) {
        try (Connection connection = MyConnection.getInstance()) {
            // First detach from all plannings
            detachNotesByPlanning(id); 
            
            String sql = "DELETE FROM carnet WHERE id = ?";
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setInt(1, id);
                ps.executeUpdate();
            }
            return true;
        } catch (SQLException e) {
            System.err.println("❌ Erreur delete : " + e.getMessage());
            return false;
        }
    }

    public boolean existsByTitre(String title, int excludeId) {
        String sql = "SELECT id FROM carnet WHERE LOWER(titre) = LOWER(?) AND id <> ?";
        try (Connection connection = MyConnection.getInstance();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, title);
            ps.setInt(2, excludeId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur existsByTitre : " + e.getMessage());
            return false;
        }
    }

    /**
     * Maps a ResultSet row from the 'carnet' table to a Carnet object.
     */
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

    // ════════ Planning ↔ Carnet link methods (via carnet_planning_etude) ════════

    public boolean linkNoteToPlanning(int noteId, int planningId) {
        String sql = "INSERT IGNORE INTO carnet_planning_etude (carnet_id, planning_etude_id) VALUES (?, ?)";
        try (Connection connection = MyConnection.getInstance();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, noteId);
            ps.setInt(2, planningId);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("❌ Erreur linkNoteToPlanning : " + e.getMessage());
            return false;
        }
    }

    public boolean unlinkNote(int noteId) {
        String sql = "DELETE FROM carnet_planning_etude WHERE carnet_id = ?";
        try (Connection connection = MyConnection.getInstance();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, noteId);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("❌ Erreur unlinkNote : " + e.getMessage());
            return false;
        }
    }

    public boolean unlinkNoteFromPlanning(int noteId, int planningId) {
        String sql = "DELETE FROM carnet_planning_etude WHERE carnet_id = ? AND planning_etude_id = ?";
        try (Connection connection = MyConnection.getInstance();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, noteId);
            ps.setInt(2, planningId);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("❌ Erreur unlinkNoteFromPlanning : " + e.getMessage());
            return false;
        }
    }

    public List<Carnet> getNotesByPlanning(int planningId) {
        List<Carnet> list = new ArrayList<>();
        String sql = "SELECT c.id, c.titre, c.contenu, c.date_creation, c.date_modification, c.couleur, c.visibilite FROM carnet c "
                   + "INNER JOIN carnet_planning_etude cpe ON cpe.carnet_id = c.id "
                   + "WHERE cpe.planning_etude_id = ? "
                   + "ORDER BY c.date_modification DESC";
        try (Connection connection = MyConnection.getInstance();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, planningId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur getNotesByPlanning : " + e.getMessage());
        }
        return list;
    }

    public List<Integer> getLinkedPlanningIds(int noteId) {
        List<Integer> ids = new ArrayList<>();
        String sql = "SELECT planning_etude_id FROM carnet_planning_etude WHERE carnet_id = ?";
        try (Connection connection = MyConnection.getInstance();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, noteId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) ids.add(rs.getInt("planning_etude_id"));
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur getLinkedPlanningIds : " + e.getMessage());
        }
        return ids;
    }

    public PlanningEtude getPlanningByNote(int noteId) {
        String sql = "SELECT pe.* FROM planning_etude pe "
                   + "INNER JOIN carnet_planning_etude cpe ON cpe.planning_etude_id = pe.id "
                   + "WHERE cpe.carnet_id = ? "
                   + "ORDER BY pe.date_seance DESC LIMIT 1";
        try (Connection connection = MyConnection.getInstance();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, noteId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    PlanningEtude p = new PlanningEtude();
                    p.setId(rs.getInt("id"));
                    p.setTitreP(rs.getString("titre_p"));
                    java.sql.Date ds = rs.getDate("date_seance");
                    if (ds != null) p.setDateSeance(ds.toLocalDate());
                    java.sql.Time hd = rs.getTime("heure_debut");
                    if (hd != null) p.setHeureDebut(hd.toLocalTime());
                    int dp = rs.getInt("duree_prevue");
                    if (!rs.wasNull()) p.setDureePrevue(dp);
                    p.setTypeActivite(rs.getString("type_activite"));
                    return p;
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur getPlanningByNote : " + e.getMessage());
        }
        return null;
    }

    public void deleteNotesByPlanning(int planningId) {
        String delNotes = "DELETE c FROM carnet c "
                        + "INNER JOIN carnet_planning_etude cpe ON cpe.carnet_id = c.id "
                        + "WHERE cpe.planning_etude_id = ?";
        String delJunction = "DELETE FROM carnet_planning_etude WHERE planning_etude_id = ?";
        try (Connection connection = MyConnection.getInstance()) {
            try (PreparedStatement ps = connection.prepareStatement(delNotes)) {
                ps.setInt(1, planningId);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = connection.prepareStatement(delJunction)) {
                ps.setInt(1, planningId);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur deleteNotesByPlanning : " + e.getMessage());
        }
    }

    public void detachNotesByPlanning(int planningId) {
        String sql = "DELETE FROM carnet_planning_etude WHERE planning_etude_id = ?";
        try (Connection connection = MyConnection.getInstance();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, planningId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("❌ Erreur detachNotesByPlanning : " + e.getMessage());
        }
    }
}