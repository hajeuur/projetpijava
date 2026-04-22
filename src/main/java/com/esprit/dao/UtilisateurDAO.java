package com.esprit.dao;

import com.esprit.models.Utilisateur;
import com.esprit.DatabaseConnection;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UtilisateurDAO implements IUtilisateur {

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

    @Override
    public void ajouter(Utilisateur u) {
        Connection cnx = DatabaseConnection.getInstance();
        String sql = "INSERT INTO utilisateur (nom, prenom, email, mdp, role, status, trust_score, risk_level) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
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
            ps.executeUpdate();
            System.out.println("Utilisateur ajouté avec succès !");
        } catch (SQLException e) {
            System.out.println("Erreur ajout : " + e.getMessage());
        }
    }

    @Override
    public void modifier(Utilisateur u) {
        Connection cnx = DatabaseConnection.getInstance();
        String sql = "UPDATE utilisateur SET nom=?, prenom=?, email=?, mdp=?, role=?, status=?, trust_score=?, risk_level=? WHERE id=?";
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
            ps.setInt(9, u.getId());
            ps.executeUpdate();
            System.out.println("Utilisateur modifié avec succès !");
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
            System.out.println("Utilisateur supprimé avec succès !");
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
            if (rs.next()) {
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
                return u;
            }
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
            while (rs.next()) {
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
                list.add(u);
            }
        } catch (SQLException e) {
            System.out.println("Erreur getAll : " + e.getMessage());
        }
        return list;
    }

    @Override
    public Utilisateur login(String email, String mdp) {
        Connection cnx = DatabaseConnection.getInstance();
        String mdpHache = hashPassword(mdp);
        String sql = "SELECT * FROM utilisateur WHERE email=? AND mdp=?";
        try {
            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setString(1, email);
            ps.setString(2, mdpHache);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Utilisateur u = new Utilisateur();
                u.setId(rs.getInt("id"));
                u.setNom(rs.getString("nom"));
                u.setPrenom(rs.getString("prenom"));
                u.setEmail(rs.getString("email"));
                u.setRole(rs.getString("role"));
                u.setStatus(rs.getString("status"));
                return u;
            }
        } catch (SQLException e) {
            System.out.println("Erreur login : " + e.getMessage());
        }
        return null;
    }

    // ── Nouveau : utilisé par LoginController et InscriptionController ─────────

    public Utilisateur findByEmail(String email) {
        Connection cnx = DatabaseConnection.getInstance();
        String sql = "SELECT * FROM utilisateur WHERE email=?";
        try {
            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
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
                return u;
            }
        } catch (SQLException e) {
            System.out.println("Erreur findByEmail : " + e.getMessage());
        }
        return null;
    }
}