package com.esprit.services;

import com.esprit.repositories.IUtilisateur;
import com.esprit.repositories.UtilisateurRepository;
import com.esprit.models.Utilisateur;
import com.esprit.utils.UserRiskAnalyzer;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Service Utilisateur — contient toute la logique métier.
 * Les controllers utilisent uniquement ce service.
 * Le service utilise IUtilisateur (interface) pour accéder aux données.
 */
public class UtilisateurService {

    // ── Injection via interface (pas DAO directement) ─────────────────────────
    private final IUtilisateur repository;

    public UtilisateurService() {
        this.repository = new UtilisateurRepository();
    }

    // ── CRUD de base ──────────────────────────────────────────────────────────

    public void ajouter(Utilisateur u) {
        // Logique métier : valeurs par défaut
        if (u.getStatus() == null || u.getStatus().isEmpty())
            u.setStatus("actif");
        if (u.getRiskLevel() == null || u.getRiskLevel().isEmpty())
            u.setRiskLevel("LOW");
        if (u.getTrustScore() == 0)
            u.setTrustScore(100.0);

        repository.ajouter(u);
    }

    public void modifier(Utilisateur u) {
        repository.modifier(u);
    }

    public void supprimer(int id) {
        repository.supprimer(id);
    }

    public Utilisateur getOne(int id) {
        return repository.getOne(id);
    }

    public List<Utilisateur> getAll() {
        return repository.getAll();
    }

    // ── Authentification ──────────────────────────────────────────────────────

    public Utilisateur login(String email, String mdp) {
        return repository.login(email, mdp);
    }

    public Utilisateur findByEmail(String email) {
        return repository.findByEmail(email);
    }

    // ── Validation métier ─────────────────────────────────────────────────────

    public boolean emailValide(String email) {
        return Pattern.matches("^[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}$", email);
    }

    public boolean emailExiste(String email) {
        List<Utilisateur> tous = repository.getAll();
        for (Utilisateur u : tous) {
            if (u.getEmail().equalsIgnoreCase(email)) return true;
        }
        return false;
    }

    public boolean mdpValide(String mdp) {
        return mdp != null && mdp.length() >= 8;
    }

    public String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            return password;
        }
    }

    // ── Gestion du risque ─────────────────────────────────────────────────────

    public void recalculerRisque(Utilisateur u) {
        List<Utilisateur> tous = repository.getAll();
        UserRiskAnalyzer.analyze(u, tous);
        repository.updateRisk(u);
    }

    public void recalculerTousLesRisques() {
        List<Utilisateur> tous = repository.getAll();
        for (Utilisateur u : tous) {
            UserRiskAnalyzer.analyze(u, tous);
            repository.updateRisk(u);
        }
    }

    public void updateRisk(Utilisateur u) {
        repository.updateRisk(u);
    }

    // ── Tentatives de connexion ───────────────────────────────────────────────

    public void incrementLoginAttempts(String email) {
        repository.incrementLoginAttempts(email);
    }

    public void resetLoginAttempts(String email) {
        repository.resetLoginAttempts(email);
    }

    // ── Verdict IA ────────────────────────────────────────────────────────────

    public void saveAiVerdict(Utilisateur u) {
        repository.saveAiVerdict(u);
    }

    // ── Activation / Désactivation ────────────────────────────────────────────

    public void toggleStatus(Utilisateur u) {
        u.setStatus(u.getStatus().equals("actif") ? "desactiver" : "actif");
        repository.modifier(u);
    }
}