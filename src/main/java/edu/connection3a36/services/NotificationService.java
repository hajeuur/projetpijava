package edu.connection3a36.services;

import edu.connection3a36.entities.Notification;
import edu.connection3a36.tools.MyConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Service de gestion des notifications pour le superadmin (ADMINM).
 * Crée la table si elle n'existe pas (auto-migration).
 */
public class NotificationService {

    private final Connection cnx;

    public NotificationService() {
        this.cnx = MyConnection.getInstance().getCnx();
        ensureTableExists();
    }

    /** Crée la table notification si elle n'existe pas encore. */
    private void ensureTableExists() {
        String sql = """
                CREATE TABLE IF NOT EXISTS notification (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    titre VARCHAR(255) NOT NULL,
                    message TEXT,
                    type VARCHAR(20) DEFAULT 'INFO',
                    is_lu TINYINT(1) DEFAULT 0,
                    is_done TINYINT(1) DEFAULT 0,
                    date_creation DATETIME DEFAULT CURRENT_TIMESTAMP,
                    auteur_id INT DEFAULT 0
                )
                """;
        try (Statement stmt = cnx.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            System.err.println("❌ Erreur création table notification: " + e.getMessage());
        }
    }

    /** Ajoute une nouvelle notification. */
    public void addNotification(Notification notif) {
        String sql = "INSERT INTO notification (titre, message, type, is_lu, is_done, date_creation, auteur_id) " +
                     "VALUES (?, ?, ?, 0, 0, NOW(), ?)";
        try (PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, notif.getTitre());
            ps.setString(2, notif.getMessage());
            ps.setString(3, notif.getType());
            ps.setInt(4, notif.getAuteurId());
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) notif.setId(rs.getInt(1));
        } catch (SQLException e) {
            System.err.println("❌ Erreur ajout notification: " + e.getMessage());
        }
    }

    /** Raccourci pour créer une notif système rapide. */
    public void addSystemNotification(String titre, String message, String type) {
        Notification n = new Notification(titre, message, type);
        n.setAuteurId(0);
        addNotification(n);
    }

    /** Récupère toutes les notifications (les plus récentes en premier). */
    public List<Notification> getAll() {
        List<Notification> list = new ArrayList<>();
        String sql = "SELECT * FROM notification ORDER BY date_creation DESC";
        try (Statement stmt = cnx.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur chargement notifications: " + e.getMessage());
        }
        return list;
    }

    /** Récupère uniquement les notifications non lues. */
    public List<Notification> getNonLues() {
        List<Notification> list = new ArrayList<>();
        String sql = "SELECT * FROM notification WHERE is_lu = 0 ORDER BY date_creation DESC";
        try (Statement stmt = cnx.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur chargement notifs non lues: " + e.getMessage());
        }
        return list;
    }

    /** Compte les notifications non lues. */
    public int countNonLues() {
        try (Statement stmt = cnx.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM notification WHERE is_lu = 0")) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            System.err.println("❌ Erreur comptage: " + e.getMessage());
        }
        return 0;
    }

    /** Marque une notification comme lue. */
    public void marquerCommeLue(int id) {
        try (PreparedStatement ps = cnx.prepareStatement(
                "UPDATE notification SET is_lu = 1 WHERE id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("❌ Erreur marquer lue: " + e.getMessage());
        }
    }

    /** Marque une notification comme terminée. */
    public void marquerCommeDone(int id) {
        try (PreparedStatement ps = cnx.prepareStatement(
                "UPDATE notification SET is_done = 1, is_lu = 1 WHERE id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("❌ Erreur marquer done: " + e.getMessage());
        }
    }

    /** Marque toutes les notifications comme lues. */
    public void marquerToutesLues() {
        try (Statement stmt = cnx.createStatement()) {
            stmt.executeUpdate("UPDATE notification SET is_lu = 1");
        } catch (SQLException e) {
            System.err.println("❌ Erreur marquer toutes lues: " + e.getMessage());
        }
    }

    /** Supprime une notification par ID. */
    public void supprimer(int id) {
        try (PreparedStatement ps = cnx.prepareStatement(
                "DELETE FROM notification WHERE id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("❌ Erreur suppression: " + e.getMessage());
        }
    }

    /** Supprime toutes les notifications terminées (done). */
    public void supprimerDones() {
        try (Statement stmt = cnx.createStatement()) {
            stmt.executeUpdate("DELETE FROM notification WHERE is_done = 1");
        } catch (SQLException e) {
            System.err.println("❌ Erreur suppression dones: " + e.getMessage());
        }
    }

    /**
<<<<<<< HEAD
     * Crée une notification pour l'admin quand un enseignant soumet un feedback sur un plan d'action.
     * Appelé par PlanActionsListController après soumission d'un feedback.
     *
     * @param planId    ID du plan d'action concerné
     * @param planTitre Titre/décision du plan
     * @param profName  Nom de l'enseignant qui a soumis le feedback
     */
    public void addFeedbackNotificationForAdmin(int planId, String planTitre, String profName) {
        String titre = "💬 Feedback de " + profName;
        String message = "L'enseignant " + profName + " a ajouté un feedback sur le plan #" + planId + " : \"" + planTitre + "\".";
        addSystemNotification(titre, message, "FEEDBACK");
    }

    private Notification mapRow(ResultSet rs) throws SQLException {
        Notification n = new Notification();
        n.setId(rs.getInt("id"));
        n.setTitre(rs.getString("titre"));
        n.setMessage(rs.getString("message"));
        n.setType(rs.getString("type") != null ? rs.getString("type") : "INFO");
        n.setLu(rs.getBoolean("is_lu"));
        n.setDone(rs.getBoolean("is_done"));
        n.setAuteurId(rs.getInt("auteur_id"));
        Timestamp ts = rs.getTimestamp("date_creation");
        if (ts != null) n.setDateCreation(ts.toLocalDateTime());
        return n;
    }
}
