package com.esprit.dao;

import com.esprit.models.Utilisateur;
import com.esprit.DatabaseConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UtilisateurDAO implements IUtilisateur {

    Connection cnx = DatabaseConnection.getInstance();

    @Override
    public void ajouter(Utilisateur u) {
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
        String sql = "UPDATE utilisateur SET nom=?, prenom=?, email=?, role=?, status=?, trust_score=?, risk_level=? WHERE id=?";
        try {
            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setString(1, u.getNom());
            ps.setString(2, u.getPrenom());
            ps.setString(3, u.getEmail());
            ps.setString(4, u.getRole());
            ps.setString(5, u.getStatus());
            ps.setDouble(6, u.getTrustScore());
            ps.setString(7, u.getRiskLevel());
            ps.setInt(8, u.getId());
            ps.executeUpdate();
            System.out.println("Utilisateur modifié avec succès !");
        } catch (SQLException e) {
            System.out.println("Erreur modification : " + e.getMessage());
        }
    }

    @Override
    public void supprimer(int id) {
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
        String sql = "SELECT * FROM utilisateur WHERE email=? AND mdp=?";
        try {
            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setString(1, email);
            ps.setString(2, mdp);
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
}