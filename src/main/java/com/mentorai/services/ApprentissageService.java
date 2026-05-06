package com.mentorai.services;

import com.mentorai.entities.Carnet;
import com.mentorai.entities.Humeur;
import com.mentorai.entities.PlanningEtude;
import com.mentorai.utils.MyConnection;

import java.sql.*;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public class ApprentissageService {

    // ═══════════════════════════════════════════════════════
    // DATA STRUCTURES (Pure Data-Driven)
    // ═══════════════════════════════════════════════════════

    public static class CurrentBehavior {
        public double avgMood = 0;
        public String moodTrend = "Stable";
        public int completionRate = 0;
        public double sessionsPerWeek = 0;
        public int avgDurationReal = 0;
        public int avgDurationPlanned = 0;
        public List<String> patterns = new ArrayList<>();
        public List<String> weakSubjects = new ArrayList<>();
        public List<String> strongSubjects = new ArrayList<>();
        public String behaviorProfile = "Non défini";
        public String mentalState = "stable";
        public String sessionInsight = "";
    }

    public static class RapportData {
        public CurrentBehavior current = new CurrentBehavior();
        
        // AI fields (Strict matching with JSON)
        public String resumeGlobal = "";
        public String analyseComportement = "";
        public String analyseMentale = "";
        public String diagnosticMatieres = "";
        public String profil = "";
        public List<String> insights = new ArrayList<>();
        public String planAction = "";
        public String planning = "";
        public String messageFinal = "";
    }

    private final HumeurService humeurService = new HumeurService();
    private final PlanningService planningService = new PlanningService();
    private final CarnetService carnetService = new CarnetService();

    // ═══════════════════════════════════════════════════════
    // MAIN ENTRY POINT
    // ═══════════════════════════════════════════════════════

    public RapportData analyserPerformances(int utilisateurId) throws Exception {
        RapportData report = new RapportData();
        int profilId = humeurService.getOrCreateProfilId(utilisateurId);

        // 1. Compute Behavior (The only source of truth)
        computeCurrentBehavior(report.current, utilisateurId, profilId);
        
        // 2. Deterministic Analysis
        determineBehaviorProfile(report.current);
        determineMoodTrend(report.current, profilId);
        
        // 3. Update Profil Apprentissage (OUTPUT of the analysis)
        updateProfileInDB(profilId, report);

        return report;
    }

    // ═══════════════════════════════════════════════════════
    // ANALYSIS LOGIC
    // ═══════════════════════════════════════════════════════

    private void determineBehaviorProfile(CurrentBehavior cur) {
        if (cur.avgDurationReal >= 60 && cur.completionRate >= 70) cur.behaviorProfile = "Deep Worker (Endurant)";
        else if (cur.avgDurationReal <= 35 && cur.sessionsPerWeek >= 4) cur.behaviorProfile = "Sprinter (Rapide et Fréquent)";
        else if (cur.completionRate < 45) cur.behaviorProfile = "Apprenant Occasionnel (Instable)";
        else cur.behaviorProfile = "Régulier";

        if (cur.avgDurationReal > 0 && cur.avgDurationReal < cur.avgDurationPlanned * 0.70) {
            cur.sessionInsight = "Fatigue cognitive détectée après " + cur.avgDurationReal + " minutes.";
        } else {
            cur.sessionInsight = "Maintien de l'attention conforme aux objectifs.";
        }
    }

    private void determineMoodTrend(CurrentBehavior cur, int profilId) {
        List<Humeur> last7 = humeurService.getLastNDays(profilId, 7);
        List<Humeur> last30 = humeurService.getLastNDays(profilId, 30);
        
        double avg7 = last7.stream().mapToInt(Humeur::getValeurHumeur).average().orElse(0);
        double avg30 = last30.stream().mapToInt(Humeur::getValeurHumeur).average().orElse(0);

        if (avg7 > avg30 + 0.4) cur.moodTrend = "En amélioration";
        else if (avg7 < avg30 - 0.4) cur.moodTrend = "En baisse (besoin de repos)";
        else cur.moodTrend = "Stable";
        
        if (cur.avgMood < 2.5) cur.mentalState = "fatigué / à risque";
        else if (cur.avgMood > 4.0) cur.mentalState = "excellent";
        else cur.mentalState = "stable";
    }

    private void computeCurrentBehavior(CurrentBehavior cur, int utilisateurId, int profilId) {
        List<Humeur> moods = humeurService.getLastNDays(profilId, 30);
        cur.avgMood = moods.stream().mapToInt(Humeur::getValeurHumeur).average().orElse(3.0);

        try {
            List<PlanningEtude> sessions = planningService.findByDateRange(LocalDate.now().minusDays(30), LocalDate.now())
                .stream().filter(s -> s.getUtilisateurId() == null || s.getUtilisateurId().equals(utilisateurId))
                .collect(Collectors.toList());

            if (!sessions.isEmpty()) {
                long done = sessions.stream().filter(s -> "done".equals(s.getEtat())).count();
                cur.completionRate = (int) ((done * 100) / sessions.size());
                cur.sessionsPerWeek = sessions.size() / 4.28;
                cur.avgDurationPlanned = (int) sessions.stream().filter(s -> s.getDureePrevue() != null).mapToInt(PlanningEtude::getDureePrevue).average().orElse(0);
                cur.avgDurationReal = (int) sessions.stream().filter(s -> s.getDureeReelle() != null).mapToInt(PlanningEtude::getDureeReelle).average().orElse(0);
                
                if (cur.completionRate < 50) cur.patterns.add("Fragilité de la régularité");
                if (cur.avgDurationReal < cur.avgDurationPlanned * 0.7) cur.patterns.add("Sessions écourtées systématiquement");
            }
        } catch (Exception e) { e.printStackTrace(); }

        carnetService.findAll().forEach(n -> {
            String txt = ((n.getTitre() != null ? n.getTitre() : "") + " " + (n.getContenu() != null ? n.getContenu() : "")).toLowerCase();
            if (txt.contains("difficile") || txt.contains("pas compris") || txt.contains("bloqué")) {
                String sub = n.getTitre() != null ? n.getTitre().split(" ")[0] : "Inconnu";
                if (!cur.weakSubjects.contains(sub)) cur.weakSubjects.add(sub);
            } else if (txt.contains("compris") || txt.contains("facile") || txt.contains("maîtrisé")) {
                 String sub = n.getTitre() != null ? n.getTitre().split(" ")[0] : "Inconnu";
                 if (!cur.strongSubjects.contains(sub)) cur.strongSubjects.add(sub);
            }
        });
    }

    private void updateProfileInDB(int profilId, RapportData report) {
        String sql = "UPDATE profil_apprentissage SET niveau_concentration = ?, vitesse_apprentissage = ?, matieres_faibles = ?, matieres_fortes = ? WHERE id = ?";
        try (Connection conn = MyConnection.getInstance();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, report.current.completionRate > 70 ? "Élevée" : (report.current.completionRate > 40 ? "Modérée" : "Faible"));
            ps.setString(2, report.current.avgDurationReal > 45 ? "Rapide" : "Normale");
            ps.setString(3, String.join(",", report.current.weakSubjects));
            ps.setString(4, String.join(",", report.current.strongSubjects));
            ps.setInt(5, profilId);
            ps.executeUpdate();
        } catch (Exception e) { e.printStackTrace(); }
    }
}
