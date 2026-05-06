package com.mentorai.services;

import com.mentorai.utils.MyConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class IntelligenceService {

    // ==========================================
    // DATA MODELS
    // ==========================================

    public static class PrioritySubject {
        public String matiere;
        public int score;
        public String message;
        public boolean isUrgent;

        public PrioritySubject(String matiere, int score, String message, boolean isUrgent) {
            this.matiere = matiere;
            this.score = score;
            this.message = message;
            this.isUrgent = isUrgent;
        }

        @Override
        public String toString() {
            return String.format("[%d/100] %s : %s %s", score, matiere, message, isUrgent ? "(URGENT)" : "");
        }
    }

    public static class UserProfile {
        public String type;
        public String suggestion;
        public String details;

        public UserProfile(String type, String suggestion, String details) {
            this.type = type;
            this.suggestion = suggestion;
            this.details = details;
        }

        @Override
        public String toString() {
            return String.format("Profil: %s\nSuggestion: %s\nDétails: %s", type, suggestion, details);
        }
    }

    // ==========================================
    // PRIORITY ENGINE
    // ==========================================

    /**
     * Calcule la priorité de révision pour chaque matière de l'utilisateur.
     * Logique de score (sur 100) :
     * - Base : 50
     * - Temps depuis la dernière révision : +2 par jour (max 30)
     * - Sessions ignorées (skipped) : +5 par session (max 20)
     * - Examen imminent (7 jours) : +40
     * - Pénalité rotation (révisé hier/aujourd'hui) : -50
     */
    public List<PrioritySubject> getPrioritySubjects(int userId) {
        List<PrioritySubject> results = new ArrayList<>();
        
        String sql = "SELECT " +
                "    matiere, " +
                "    MAX(CASE WHEN etat = 'done' THEN date_seance ELSE NULL END) as last_study_date, " +
                "    SUM(CASE WHEN etat = 'skipped' THEN 1 ELSE 0 END) as skipped_count, " +
                "    SUM(CASE WHEN (LOWER(type_activite) LIKE '%exam%' OR LOWER(type_activite) LIKE '%test%') " +
                "             AND date_seance >= CURRENT_DATE AND date_seance <= DATE_ADD(CURRENT_DATE, INTERVAL 7 DAY) " +
                "             THEN 1 ELSE 0 END) as upcoming_exams " +
                "FROM planning_etude " +
                "WHERE utilisateur_id = ? AND matiere IS NOT NULL AND matiere != '' " +
                "GROUP BY matiere";

        try (Connection conn = MyConnection.getInstance();
             PreparedStatement ps = conn.prepareStatement(sql)) {
             
            ps.setInt(1, userId);
            
            try (ResultSet rs = ps.executeQuery()) {
                LocalDate today = LocalDate.now();
                
                while (rs.next()) {
                    String matiere = rs.getString("matiere");
                    java.sql.Date lastStudySqlDate = rs.getDate("last_study_date");
                    int skippedCount = rs.getInt("skipped_count");
                    int upcomingExams = rs.getInt("upcoming_exams");
                    
                    int score = 50; // Base
                    
                    // Time since last study
                    long daysSinceLast = 15; // default if never studied
                    if (lastStudySqlDate != null) {
                        LocalDate lastStudy = lastStudySqlDate.toLocalDate();
                        daysSinceLast = ChronoUnit.DAYS.between(lastStudy, today);
                        
                        // Rotation Penalty: Don't study the exact same thing every single day if possible
                        if (daysSinceLast <= 1) {
                            score -= 50;
                        } else {
                            score += Math.min(30, daysSinceLast * 2);
                        }
                    } else {
                        score += 20; // boost for never studied
                    }
                    
                    // Ignored sessions penalty
                    score += Math.min(20, skippedCount * 5);
                    
                    // Urgency layer
                    boolean isUrgent = upcomingExams > 0;
                    if (isUrgent) {
                        score += 40;
                    }
                    
                    // Normalize score between 0 and 100
                    score = Math.max(0, Math.min(100, score));
                    
                    String message;
                    if (score > 80) {
                        message = isUrgent ? "Urgent - Examen en approche" : "Vous commencez à oublier ce sujet (délaissé)";
                    } else if (score >= 60) {
                        message = "Ce sujet a besoin de consolidation";
                    } else if (score >= 40) {
                        message = "Révision d'entretien recommandée";
                    } else {
                        message = "Sujet récent, frais en mémoire";
                    }
                    
                    results.add(new PrioritySubject(matiere, score, message, isUrgent));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // Sort by score descending
        results.sort((a, b) -> Integer.compare(b.score, a.score));
        return results;
    }

    // ==========================================
    // HIDDEN INTELLIGENCE LAYER
    // ==========================================

    /**
     * Détecte automatiquement le profil comportemental de l'utilisateur.
     * Profils:
     * - Sprinter : sessions courtes (<40 min), fréquentes.
     * - Deep Worker : sessions longues (>= 40 min).
     * - Inconsistent : ratio d'accomplissement faible ou longs écarts.
     */
    public UserProfile getUserProfile(int userId) {
        String sql = "SELECT " +
                "    AVG(COALESCE(duree_reelle, duree_prevue)) as avg_duration, " +
                "    SUM(CASE WHEN etat = 'done' THEN 1 ELSE 0 END) as done_count, " +
                "    SUM(CASE WHEN etat = 'skipped' THEN 1 ELSE 0 END) as skipped_count, " +
                "    COUNT(*) as total_count, " +
                "    DATEDIFF(MAX(date_seance), MIN(date_seance)) as days_span " +
                "FROM planning_etude " +
                "WHERE utilisateur_id = ?";

        try (Connection conn = MyConnection.getInstance();
             PreparedStatement ps = conn.prepareStatement(sql)) {
             
            ps.setInt(1, userId);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int totalCount = rs.getInt("total_count");
                    
                    // S'il n'y a pas assez de données
                    if (totalCount < 3) {
                        return new UserProfile(
                            "🌱 Débutant", 
                            "Planifiez plus de sessions pour que nous puissions analyser votre rythme.",
                            "Pas assez de données pour déterminer un profil."
                        );
                    }
                    
                    double avgDuration = rs.getDouble("avg_duration");
                    int doneCount = rs.getInt("done_count");
                    int skippedCount = rs.getInt("skipped_count");
                    int daysSpan = rs.getInt("days_span");
                    daysSpan = Math.max(1, daysSpan); // Eviter division par zéro
                    
                    double completionRate = (double) doneCount / totalCount;
                    double sessionsPerDay = (double) doneCount / daysSpan;
                    
                    // Logic detection
                    if (completionRate < 0.4 || skippedCount > doneCount) {
                        return new UserProfile(
                            "⚠️ Inconstant", 
                            "Essayez d'établir un planning quotidien fixe. Mettez des rappels en douceur.",
                            String.format("Taux de complétion faible (%.0f%%). Trop de sessions ignorées.", completionRate * 100)
                        );
                    }
                    
                    if (avgDuration < 40 && sessionsPerDay >= 0.5) {
                        return new UserProfile(
                            "🔥 Sprinter", 
                            "Privilégiez les sessions de 20-30 min. Faites de multiples petits blocs.",
                            String.format("Sessions courtes (moy. %.0f min) mais régulières.", avgDuration)
                        );
                    }
                    
                    return new UserProfile(
                        "🧠 Deep Worker", 
                        "Privilégiez les sessions de 60-90 min. Moins fréquentes mais très intenses.",
                        String.format("Excellente concentration sur de longues périodes (moy. %.0f min).", avgDuration)
                    );
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return new UserProfile("Inconnu", "Aucune donnée disponible", "");
    }
}
