package com.esprit.repositories;

import com.esprit.models.Utilisateur;
import com.esprit.DatabaseConnection;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Implémentation de IUtilisateur — couche d'accès aux données (Repository)
 * Contient uniquement la logique SQL, sans logique métier.
 */
public class UtilisateurRepository implements IUtilisateur {

    public UtilisateurRepository() {
        // Migration auto : ajoute pdp_url si la colonne n'existe pas encore
        try {
            Connection cnx = DatabaseConnection.getInstance();
            if (cnx != null) {
                // Vérifie si la colonne existe déjà
                ResultSet rs = cnx.createStatement().executeQuery(
                    "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS " +
                    "WHERE TABLE_SCHEMA = DATABASE() " +
                    "AND TABLE_NAME = 'utilisateur' " +
                    "AND COLUMN_NAME = 'pdp_url'"
                );
                if (rs.next() && rs.getInt(1) == 0) {
                    cnx.createStatement().executeUpdate(
                        "ALTER TABLE utilisateur ADD COLUMN pdp_url VARCHAR(500) NULL"
                    );
                    System.out.println("✅ Colonne pdp_url ajoutée à la table utilisateur");
                }
            }
        } catch (Exception e) {
            System.out.println("⚠️ Migration pdp_url : " + e.getMessage());
        }
    }

    // ── Hachage SHA-256 ───────────────────────────────────────────────────────

    private String hashPassword(String password) {
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

    // ── CRUD de base ──────────────────────────────────────────────────────────

    @Override
    public void ajouter(Utilisateur u) {
        Connection cnx = DatabaseConnection.getInstance();
        String sql = "INSERT INTO utilisateur (nom, prenom, email, mdp, role, status, trust_score, risk_level, flagged_duplicate, login_attempts, registration_ip, pdp_url) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try {
            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setString(1, u.getNom());
            ps.setString(2, u.getPrenom());
            ps.setString(3, u.getEmail());
            ps.setString(4, u.getMdp());
            ps.setString(5, u.getRole());
            ps.setString(6, u.getStatus());
            ps.setDouble(7, u.getTrustScore());
            ps.setString(8, u.getRiskLevel());
            ps.setInt(9, u.getFlaggedDuplicate());
            ps.setInt(10, u.getLoginAttempts());
            ps.setString(11, u.getRegistrationIp());
            ps.setString(12, u.getPdpUrl());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Erreur ajout : " + e.getMessage());
        }
    }

    @Override
    public void modifier(Utilisateur u) {
        Connection cnx = DatabaseConnection.getInstance();
        String sql = "UPDATE utilisateur SET nom=?, prenom=?, email=?, mdp=?, role=?, status=?, trust_score=?, risk_level=?, flagged_duplicate=?, login_attempts=?, pdp_url=? WHERE id=?";
        try {
            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setString(1, u.getNom());
            ps.setString(2, u.getPrenom());
            ps.setString(3, u.getEmail());
            ps.setString(4, u.getMdp());
            ps.setString(5, u.getRole());
            ps.setString(6, u.getStatus());
            ps.setDouble(7, u.getTrustScore());
            ps.setString(8, u.getRiskLevel());
            ps.setInt(9, u.getFlaggedDuplicate());
            ps.setInt(10, u.getLoginAttempts());
            ps.setString(11, u.getPdpUrl());
            ps.setInt(12, u.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Erreur modification : " + e.getMessage());
        }
    }

    @Override
    public void supprimer(int id) {
        Connection cnx = DatabaseConnection.getInstance();
        String sql = "DELETE FROM utilisateur WHERE id=?";
        try {
            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Erreur suppression : " + e.getMessage());
        }
    }

    @Override
    public Utilisateur getOne(int id) {
        Connection cnx = DatabaseConnection.getInstance();
        String sql = "SELECT * FROM utilisateur WHERE id=?";
        try {
            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        } catch (SQLException e) {
            System.out.println("Erreur getOne : " + e.getMessage());
        }
        return null;
    }

    @Override
    public List<Utilisateur> getAll() {
        Connection cnx = DatabaseConnection.getInstance();
        List<Utilisateur> list = new ArrayList<>();
        String sql = "SELECT * FROM utilisateur";
        try {
            PreparedStatement ps = cnx.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            System.out.println("Erreur getAll : " + e.getMessage());
        }
        return list;
    }

    @Override
    public Utilisateur login(String email, String mdp) {
        Connection cnx = DatabaseConnection.getInstance();
        // Récupérer l'utilisateur par email uniquement
        String sql = "SELECT * FROM utilisateur WHERE email=?";
        try {
            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Utilisateur u = mapRow(rs);
                String storedHash = u.getMdp();
                boolean passwordOk;
                if (storedHash != null && storedHash.startsWith("$2")) {
                    // Hash bcrypt (format Symfony $2y$ ou $2a$)
                    // BCrypt.checkpw attend $2a$ — on remplace $2y$ si nécessaire
                    String bcryptHash = storedHash.startsWith("$2y$")
                            ? "$2a$" + storedHash.substring(4)
                            : storedHash;
                    passwordOk = org.mindrot.jbcrypt.BCrypt.checkpw(mdp, bcryptHash);
                } else {
                    // Hash SHA-256 (format natif du module com.esprit)
                    passwordOk = storedHash != null && storedHash.equals(hashPassword(mdp));
                }
                if (passwordOk) return u;
            }
        } catch (SQLException e) {
            System.out.println("Erreur login : " + e.getMessage());
        }
        return null;
    }

    @Override
    public Utilisateur findByEmail(String email) {
        Connection cnx = DatabaseConnection.getInstance();
        String sql = "SELECT * FROM utilisateur WHERE email=?";
        try {
            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        } catch (SQLException e) {
            System.out.println("Erreur findByEmail : " + e.getMessage());
        }
        return null;
    }

    @Override
    public void updateRisk(Utilisateur u) {
        Connection cnx = DatabaseConnection.getInstance();
        String sql = "UPDATE utilisateur SET trust_score=?, risk_level=?, flagged_duplicate=? WHERE id=?";
        try {
            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setDouble(1, u.getTrustScore());
            ps.setString(2, u.getRiskLevel());
            ps.setInt(3, u.getFlaggedDuplicate());
            ps.setInt(4, u.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Erreur updateRisk : " + e.getMessage());
        }
    }

    @Override
    public void saveAiVerdict(Utilisateur u) {
        Connection cnx = DatabaseConnection.getInstance();
        String sql = "UPDATE utilisateur SET ai_verdict=? WHERE id=?";
        try {
            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setString(1, u.getAiVerdict());
            ps.setInt(2, u.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Erreur saveAiVerdict : " + e.getMessage());
        }
    }

    @Override
    public void incrementLoginAttempts(String email) {
        Connection cnx = DatabaseConnection.getInstance();
        String sql = "UPDATE utilisateur SET login_attempts = login_attempts + 1 WHERE email=?";
        try {
            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setString(1, email);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Erreur incrementLoginAttempts : " + e.getMessage());
        }
    }

    @Override
    public void resetLoginAttempts(String email) {
        Connection cnx = DatabaseConnection.getInstance();
        String sql = "UPDATE utilisateur SET login_attempts = 0 WHERE email=?";
        try {
            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setString(1, email);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Erreur resetLoginAttempts : " + e.getMessage());
        }
    }

    // ── Mapping ResultSet → Utilisateur ──────────────────────────────────────

    private Utilisateur mapRow(ResultSet rs) throws SQLException {
        Utilisateur u = new Utilisateur();
        u.setId(rs.getInt("id"));
        u.setNom(rs.getString("nom"));
        u.setPrenom(rs.getString("prenom"));
        u.setEmail(rs.getString("email"));
        u.setMdp(rs.getString("mdp"));
        u.setRole(rs.getString("role"));
        u.setStatus(rs.getString("status"));
        u.setTrustScore(rs.getDouble("trust_score"));
        u.setRiskLevel(rs.getString("risk_level"));
        try { u.setFlaggedDuplicate(rs.getInt("flagged_duplicate")); } catch (Exception ignored) {}
        try { u.setLoginAttempts(rs.getInt("login_attempts")); } catch (Exception ignored) {}
        try { u.setRegistrationIp(rs.getString("registration_ip")); } catch (Exception ignored) {}
        try { u.setPdpUrl(rs.getString("pdp_url")); } catch (Exception ignored) {}
        try { u.setAiVerdict(rs.getString("ai_verdict")); } catch (Exception ignored) {}
        return u;
    }
}