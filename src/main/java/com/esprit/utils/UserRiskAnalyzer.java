package com.esprit.utils;

import com.esprit.models.Utilisateur;

import java.util.Arrays;
import java.util.List;

public class UserRiskAnalyzer {

    // Domaines d'emails suspects
    private static final List<String> SUSPICIOUS_DOMAINS = Arrays.asList(
            "mailinator.com", "yopmail.com", "tempmail.com", "guerrillamail.com",
            "throwaway.email", "fakeinbox.com", "trashmail.com", "sharklasers.com",
            "maildrop.cc", "dispostable.com", "getnada.com", "10minutemail.com"
    );

    /**
     * Calcule le trust score et risk level d'un utilisateur
     * et met à jour ses attributs directement.
     */
    public static void analyze(Utilisateur u, List<Utilisateur> allUsers) {
        double score = 100.0;

        // ── Règle 1 : Tentatives de connexion excessives (> 5) ────────────────
        if (u.getLoginAttempts() > 5) {
            score -= 15;
            System.out.println(">>> [Risk] " + u.getEmail() + " : -15 (login attempts: " + u.getLoginAttempts() + ")");
        }

        // ── Règle 2 : Email suspect (domaine jetable) ─────────────────────────
        if (u.getEmail() != null) {
            String domain = u.getEmail().contains("@")
                    ? u.getEmail().split("@")[1].toLowerCase()
                    : "";
            if (SUSPICIOUS_DOMAINS.contains(domain)) {
                score -= 20;
                System.out.println(">>> [Risk] " + u.getEmail() + " : -20 (suspicious domain: " + domain + ")");
            }
        }

        // ── Règle 3 : Pas de photo de profil ─────────────────────────────────
        if (u.getPdpUrl() == null || u.getPdpUrl().trim().isEmpty()) {
            score -= 5;
            System.out.println(">>> [Risk] " + u.getEmail() + " : -5 (no photo)");
        }

        // ── Règle 4 : Doublon d'IP ────────────────────────────────────────────
        if (u.getRegistrationIp() != null && !u.getRegistrationIp().trim().isEmpty()) {
            long sameIpCount = allUsers.stream()
                    .filter(other -> other.getId() != u.getId())
                    .filter(other -> u.getRegistrationIp().equals(other.getRegistrationIp()))
                    .count();
            if (sameIpCount > 0) {
                score -= 25;
                u.setFlaggedDuplicate(1);
                System.out.println(">>> [Risk] " + u.getEmail() + " : -25 (duplicate IP: " + u.getRegistrationIp() + ")");
            } else {
                u.setFlaggedDuplicate(0);
            }
        }

        // ── Normalisation du score ────────────────────────────────────────────
        score = Math.max(0, Math.min(100, score));
        u.setTrustScore(score);

        // ── Niveau de risque ──────────────────────────────────────────────────
        if (score >= 80) {
            u.setRiskLevel("LOW");
        } else if (score >= 50) {
            u.setRiskLevel("MEDIUM");
        } else {
            u.setRiskLevel("HIGH");
        }

        System.out.println(">>> [Risk] " + u.getEmail() +
                " → score=" + score + " level=" + u.getRiskLevel());
    }

    /**
     * Retourne une couleur CSS selon le niveau de risque.
     */
    public static String getRiskColor(String riskLevel) {
        if (riskLevel == null) return "#95a5a6";
        return switch (riskLevel.toUpperCase()) {
            case "LOW"    -> "#27ae60"; // vert
            case "MEDIUM" -> "#f39c12"; // orange
            case "HIGH"   -> "#e74c3c"; // rouge
            default       -> "#95a5a6"; // gris
        };
    }

    /**
     * Retourne le libellé français du niveau de risque.
     */
    public static String getRiskLabel(String riskLevel) {
        if (riskLevel == null) return "Inconnu";
        return switch (riskLevel.toUpperCase()) {
            case "LOW"    -> "Faible";
            case "MEDIUM" -> "Moyen";
            case "HIGH"   -> "Élevé";
            default       -> "Inconnu";
        };
    }
}