package com.mentorai.services;

import com.mentorai.models.Feedback;
import java.util.List;
import java.util.stream.Collectors;

public class PrioriteService {

    // ===== CALCUL PRIORITÉ depuis les données BDD existantes =====
    public String calculerPriorite(Feedback f) {
        String type = f.getTypefeedback();
        int note    = f.getNote();
        String etat = f.getEtatfeedback();

        // Déjà traité → pas urgent
        if (etat.equals("traite")) return "Faible";

        // Problème avec mauvaise note → Urgent
        if (type.equals("probleme") && note <= 2) return "Urgent";

        // Problème avec note moyenne → Normal
        if (type.equals("probleme") && note == 3) return "Normal";

        // Problème avec bonne note → Normal
        if (type.equals("probleme") && note >= 4) return "Normal";

        // Suggestion → Normal
        if (type.equals("suggestion")) return "Normal";

        // Satisfaction avec mauvaise note → Normal
        if (type.equals("satisfaction") && note <= 2) return "Normal";

        // Satisfaction avec bonne note → Faible
        if (type.equals("satisfaction") && note >= 3) return "Faible";

        return "Normal";
    }

    // ===== COULEUR du badge selon priorité =====
    public String getCouleurPriorite(String priorite) {
        return switch (priorite) {
            case "Urgent" -> "#d52e28";
            case "Normal" -> "#f0a500";
            case "Faible" -> "#28a745";
            default       -> "#888";
        };
    }

    // ===== TRIER les feedbacks par priorité =====
    public List<Feedback> trierParPriorite(List<Feedback> feedbacks) {
        return feedbacks.stream()
                .sorted((a, b) -> {
                    int pa = prioriteEnChiffre(calculerPriorite(a));
                    int pb = prioriteEnChiffre(calculerPriorite(b));
                    return Integer.compare(pa, pb);
                })
                .collect(Collectors.toList());
    }

    private int prioriteEnChiffre(String priorite) {
        return switch (priorite) {
            case "Urgent" -> 1; // ← en premier
            case "Normal" -> 2;
            case "Faible" -> 3;
            default       -> 4;
        };
    }

    // ===== STATS depuis la BDD =====
    public long compterUrgents(List<Feedback> feedbacks) {
        return feedbacks.stream()
                .filter(f -> calculerPriorite(f).equals("Urgent"))
                .count();
    }

    public long compterNormaux(List<Feedback> feedbacks) {
        return feedbacks.stream()
                .filter(f -> calculerPriorite(f).equals("Normal"))
                .count();
    }

    public long compterFaibles(List<Feedback> feedbacks) {
        return feedbacks.stream()
                .filter(f -> calculerPriorite(f).equals("Faible"))
                .count();
    }
}