package com.mentorai.services;

import com.mentorai.entities.Humeur;
import com.mentorai.utils.MyConnection;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class HumeurService {
    private final Connection connection;

    public HumeurService() {
        connection = MyConnection.getInstance();
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
        // PREVENT DUPLICATE
        if (getTodayMood(humeur.getProfilApprentissageId()) != null) {
            throw new Exception("Mood already logged for today.");
        }

        calculateAveragesAndRisk(humeur);
        
        if (humeur.getCreeLe() == null) {
            humeur.setCreeLe(LocalDateTime.now());
        }

        String sql = "INSERT INTO humeur (valeur_humeur, facteur_principal, tendance, moyenne7j, moyenne30j, niveau_risque, cree_le, profil_apprentissage_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pst = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pst.setInt(1, humeur.getValeurHumeur());
            pst.setString(2, humeur.getFacteurPrincipal());
            pst.setString(3, humeur.getTendance());
            pst.setInt(4, humeur.getMoyenne7j());
            pst.setInt(5, humeur.getMoyenne30j());
            pst.setString(6, humeur.getNiveauRisque());
            pst.setTimestamp(7, Timestamp.valueOf(humeur.getCreeLe()));
            pst.setInt(8, humeur.getProfilApprentissageId());

            pst.executeUpdate();
            
            try (ResultSet generatedKeys = pst.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    humeur.setId(generatedKeys.getInt(1));
                }
            }
        } catch (SQLException e) {
            throw new Exception("Error during insertion in database: " + e.getMessage(), e);
        }
    }

    public void update(Humeur humeur) throws Exception {
        calculateAveragesAndRisk(humeur);

        String sql = "UPDATE humeur SET valeur_humeur=?, facteur_principal=?, tendance=?, moyenne7j=?, moyenne30j=?, niveau_risque=? WHERE id=? AND profil_apprentissage_id=?";
        try (PreparedStatement pst = connection.prepareStatement(sql)) {
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

    // ── Alias methods called by HumeurController ──────────────────────────

    /** Called by HumeurController#ajouterHumeur(). Delegates to {@link #ajouter(Humeur)}. */
    public void ajouterHumeur(Humeur humeur) throws Exception {
        ajouter(humeur);
    }

    /**
     * Called by HumeurController#updateHumeur().
     * Sets the id on the supplied humeur then delegates to {@link #update(Humeur)}.
     */
    public void updateHumeur(int id, Humeur humeur) throws Exception {
        humeur.setId(id);
        update(humeur);
    }

    /**
     * Called by HumeurController#loadChart().
     * Returns the mood entries for the last {@code days} days, ordered ascending.
     * Delegates to {@link #getLastNDays(int, int)}.
     */
    public List<Humeur> getMoodTrend(int profilId, int days) {
        return getLastNDays(profilId, days);
    }

    public Humeur getTodayMood(int profilId) {
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
        try (PreparedStatement pst = connection.prepareStatement(sql)) {
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
        
        try (PreparedStatement pst = connection.prepareStatement(sql)) {
            pst.setInt(1, profilId);
            try (ResultSet rs = pst.executeQuery()) {
                LocalDate previousDate = null;
                
                while (rs.next()) {
                    LocalDate currentDate = rs.getDate("log_date").toLocalDate();
                    
                    if (previousDate == null) {
                        // First record
                        currentStreak = 1;
                        longestStreak = 1;
                    } else {
                        // Check if it's strictly one day before the previous date we processed
                        if (currentDate.plusDays(1).equals(previousDate)) {
                            currentStreak++;
                            if (currentStreak > longestStreak) {
                                longestStreak = currentStreak;
                            }
                        } else if (!currentDate.equals(previousDate)) {
                            // If gap found and it's not the same day, reset streak
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
        int avg7 = getAverageMood(humeur.getProfilApprentissageId(), 7);
        int avg30 = getAverageMood(humeur.getProfilApprentissageId(), 30);
        
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
