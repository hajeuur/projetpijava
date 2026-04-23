package com.mentorai.services;

import com.mentorai.utils.MyConnection;
import java.sql.*;

public class UtilisateurService {

    private Connection connection;

    public UtilisateurService() {
        connection = MyConnection.getInstance();
    }

    public String getEmailById(int utilisateurId) {
        String query = "SELECT email FROM utilisateur WHERE id = ?";
        try {
            PreparedStatement ps = connection.prepareStatement(query);
            ps.setInt(1, utilisateurId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getString("email");
        } catch (SQLException e) {
            System.out.println("Erreur getEmail : " + e.getMessage());
        }
        return null;
    }

    public String getNomPrenomById(int utilisateurId) {
        String query = "SELECT nom, prenom FROM utilisateur WHERE id = ?";
        try {
            PreparedStatement ps = connection.prepareStatement(query);
            ps.setInt(1, utilisateurId);
            ResultSet rs = ps.executeQuery();
            if (rs.next())
                return rs.getString("prenom") + " " + rs.getString("nom");
        } catch (SQLException e) {
            System.out.println("Erreur getNomPrenom : " + e.getMessage());
        }
        return "Etudiant";
    }
}