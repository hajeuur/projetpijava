package com.mentorai.services;

import com.mentorai.entities.Humeur;
import com.mentorai.utils.MyConnection;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class HumeurService {

    public HumeurService() {
    }

    public List<String> validateMoodData(Humeur humeur) {
        List<String> errors = new ArrayList<>();
        
        if (humeur.getValeurHumeur() < 1 || humeur.getValeurHumeur() > 5) {
            errors.add("La valeur de l'humeur doit être entre 1 et 5");
        }

        if (humeur.getTendance() != null && !humeur.getTendance().trim().isEmpty()) {
            String[] tags = humeur.getTendance().split(",");
            if (tags.length > 3) {
                errors.add("Maximum 3 tags autorisés");
            }
        }

        if (humeur.getFacteurPrincipal() != null && humeur.getFacteurPrincipal().length() > 150) {
            errors.add("Le journal (facteur principal) ne peut pas dépasser 150 caractères");
        }
        
        return errors;
    }

    public void ajouter(Humeur humeur) throws Exception {
        try (Connection connection = MyConnection.getInstance()) {
            // Resolve profile ID from user ID
            int utilisateurId = humeur.getProfilApprentissageId();
            int realProfilId = getOrCreateProfilId(connection, utilisateurId);
            humeur.setProfilApprentissageId(realProfilId);

            // PREVENT DUPLICATE
            if (getTodayMood(connection, realProfilId) != null) {
                throw new Exception("L'humeur a déjà été enregistrée pour aujourd'hui.");
            }

            // Calculate stats before insert
            calculateAveragesAndRisk(connection, humeur);

            String sql = "INSERT INTO humeur (valeur_humeur, facteur_principal, tendance, moyenne7j, moyenne30j, niveau_risque, cree_le, profil_apprentissage_id) " +
                         "VALUES (?, ?, ?, ?, ?, ?, NOW(), ?)";
            
            try (PreparedStatement pst = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                pst.setInt(1, humeur.getValeurHumeur());
                pst.setString(2, humeur.getFacteurPrincipal());
                pst.setString(3, humeur.getTendance());
                pst.setInt(4, humeur.getMoyenne7j());
                pst.setInt(5, humeur.getMoyenne30j());
                pst.setString(6, humeur.getNiveauRisque());
                pst.setInt(7, realProfilId);

                int affectedRows = pst.executeUpdate();
                if (affectedRows == 0) {
                    throw new SQLException("Échec de l'insertion : aucune ligne affectée.");
                }

                try (ResultSet generatedKeys = pst.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        humeur.setId(generatedKeys.getInt(1));
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace(); // Log to console for the developer
            throw new Exception("Erreur base de données : " + e.getMessage(), e);
        }
    }

    /**
     * Helper to resolve profile using an existing connection.
     */
    public int getOrCreateProfilId(Connection conn, int utilisateurId) throws SQLException {
        String selectSql = "SELECT id FROM profil_apprentissage WHERE utilisateur_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(selectSql)) {
            ps.setInt(1, utilisateurId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("id");
            }
        }

        String insertSql = "INSERT INTO profil_apprentissage (utilisateur_id) VALUES (?)";
        try (PreparedStatement ps = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, utilisateurId);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        throw new SQLException("Profil non trouvé.");
    }

    /**
     * Overload for other methods if needed.
     */
    public int getOrCreateProfilId(int utilisateurId) throws SQLException {
        try (Connection conn = MyConnection.getInstance()) {
            return getOrCreateProfilId(conn, utilisateurId);
        }
    }

    public void update(Humeur humeur) throws Exception {
        // Resolve profile ID from user ID (humeur.profilApprentissageId is currently utilisateur_id)
        int utilisateurId = humeur.getProfilApprentissageId();
        int realProfilId = getOrCreateProfilId(utilisateurId);
        humeur.setProfilApprentissageId(realProfilId);

        calculateAveragesAndRisk(humeur);

        String sql = "UPDATE humeur SET valeur_humeur=?, facteur_principal=?, tendance=?, moyenne7j=?, moyenne30j=?, niveau_risque=? WHERE id=? AND profil_apprentissage_id=?";
        try (Connection connection = MyConnection.getInstance();
             PreparedStatement pst = connection.prepareStatement(sql)) {
            pst.setInt(1, humeur.getValeurHumeur());
            pst.setString(2, humeur.getFacteurPrincipal());
            pst.setString(3, humeur.getTendance());
            pst.setInt(4, humeur.getMoyenne7j());
            pst.setInt(5, humeur.getMoyenne30j());
            pst.setString(6, humeur.getNiveauRisque());
            pst.setInt(7, humeur.getId());
            pst.setInt(8, humeur.getProfilApprentissageId());

            int updated = pst.executeUpdate();
            if (updated == 0) {
                throw new Exception("Update failed, mood entry not found or unauthorized.");
            }
        } catch (SQLException e) {
            throw new Exception("Error during update in database: " + e.getMessage(), e);
        }
    }

    public void ajouterHumeur(Humeur humeur) throws Exception {
        ajouter(humeur);
    }

    public void updateHumeur(int id, Humeur humeur) throws Exception {
        humeur.setId(id);
        update(humeur);
    }

    public List<Humeur> getMoodTrend(int profilId, int days) {
        return getLastNDays(profilId, days);
    }

    public Humeur getTodayMood(int profilId) {
        try (Connection conn = MyConnection.getInstance()) {
            return getTodayMood(conn, profilId);
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public Humeur getTodayMood(Connection connection, int profilId) {
        String sql = "SELECT * FROM humeur WHERE profil_apprentissage_id = ? AND DATE(cree_le) = CURDATE() LIMIT 1";
        try (PreparedStatement pst = connection.prepareStatement(sql)) {
            pst.setInt(1, profilId);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    return extractHumeurFromResultSet(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<Humeur> getLastNDays(int profilId, int days) {
        List<Humeur> moods = new ArrayList<>();
        String sql = "SELECT * FROM humeur WHERE profil_apprentissage_id = ? AND DATE(cree_le) >= DATE_SUB(CURDATE(), INTERVAL ? DAY) ORDER BY cree_le ASC";
        try (Connection connection = MyConnection.getInstance();
             PreparedStatement pst = connection.prepareStatement(sql)) {
            pst.setInt(1, profilId);
            pst.setInt(2, days);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    moods.add(extractHumeurFromResultSet(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return moods;
    }

    public int getAverageMood(int profilId, int days) {
        try (Connection conn = MyConnection.getInstance()) {
            return getAverageMood(conn, profilId, days);
        } catch (SQLException e) {
            e.printStackTrace();
            return 0;
        }
    }

    public int getAverageMood(Connection connection, int profilId, int days) {
        String sql = "SELECT AVG(valeur_humeur) as avg_mood FROM humeur WHERE profil_apprentissage_id = ? AND DATE(cree_le) >= DATE_SUB(CURDATE(), INTERVAL ? DAY)";
        try (PreparedStatement pst = connection.prepareStatement(sql)) {
            pst.setInt(1, profilId);
            pst.setInt(2, days);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    double avg = rs.getDouble("avg_mood");
                    return (int) Math.round(avg);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public int getLongestStreak(int profilId) {
        String sql = "SELECT DATE(cree_le) AS log_date FROM humeur WHERE profil_apprentissage_id = ? ORDER BY cree_le DESC";
        int longestStreak = 0;
        int currentStreak = 0;
        
        try (Connection connection = MyConnection.getInstance();
             PreparedStatement pst = connection.prepareStatement(sql)) {
            pst.setInt(1, profilId);
            try (ResultSet rs = pst.executeQuery()) {
                LocalDate previousDate = null;
                
                while (rs.next()) {
                    LocalDate currentDate = rs.getDate("log_date").toLocalDate();
                    
                    if (previousDate == null) {
                        currentStreak = 1;
                        longestStreak = 1;
                    } else {
                        if (currentDate.plusDays(1).equals(previousDate)) {
                            currentStreak++;
                            if (currentStreak > longestStreak) {
                                longestStreak = currentStreak;
                            }
                        } else if (!currentDate.equals(previousDate)) {
                            currentStreak = 1;
                        }
                    }
                    previousDate = currentDate;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return longestStreak;
    }

    private void calculateAveragesAndRisk(Humeur humeur) {
        try (Connection conn = MyConnection.getInstance()) {
            calculateAveragesAndRisk(conn, humeur);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void calculateAveragesAndRisk(Connection connection, Humeur humeur) {
        int avg7 = getAverageMood(connection, humeur.getProfilApprentissageId(), 7);
        int avg30 = getAverageMood(connection, humeur.getProfilApprentissageId(), 30);
        
        humeur.setMoyenne7j(avg7);
        humeur.setMoyenne30j(avg30);

        int val = humeur.getValeurHumeur();
        if (val <= 2) {
            humeur.setNiveauRisque("High");
        } else if (val == 3) {
            humeur.setNiveauRisque("Medium");
        } else {
            humeur.setNiveauRisque("Low");
        }
    }

    private Humeur extractHumeurFromResultSet(ResultSet rs) throws SQLException {
        Humeur h = new Humeur();
        h.setId(rs.getInt("id"));
        h.setValeurHumeur(rs.getInt("valeur_humeur"));
        h.setFacteurPrincipal(rs.getString("facteur_principal"));
        h.setTendance(rs.getString("tendance"));
        h.setMoyenne7j(rs.getInt("moyenne7j"));
        h.setMoyenne30j(rs.getInt("moyenne30j"));
        h.setNiveauRisque(rs.getString("niveau_risque"));
        
        Timestamp ts = rs.getTimestamp("cree_le");
        if (ts != null) {
            h.setCreeLe(ts.toLocalDateTime());
        }
        h.setProfilApprentissageId(rs.getInt("profil_apprentissage_id"));
        
        return h;
    }
}
