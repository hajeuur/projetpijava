package edu.connection3a36.services;

import edu.connection3a36.entities.Utilisateur;
import edu.connection3a36.interfaces.IService;
import edu.connection3a36.tools.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UtilisateurService implements IService<Utilisateur> {

    private Connection cnx;

    public UtilisateurService() {
        cnx = MyConnection.getInstance().getCnx();
    }

    @Override
    public void addEntity(Utilisateur u) throws SQLException {
        String req = "INSERT INTO utilisateur (nom, prenom, email, mdp, pdp_url, role, status) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pst = cnx.prepareStatement(req)) {
            pst.setString(1, u.getNom());
            pst.setString(2, u.getPrenom());
            pst.setString(3, u.getEmail());
            pst.setString(4, u.getMdp()); // Normalement hashé BCrypt (côté Java, on gère ça plus tard si besoin)
            pst.setString(5, u.getPdpUrl());
            pst.setString(6, u.getRole() != null ? u.getRole() : "ENSEIGNANT");
            pst.setString(7, u.getStatus() != null ? u.getStatus() : "actif");
            pst.executeUpdate();
        }
    }

    @Override
    public void deleteEntity(Utilisateur u) throws SQLException {
        String req = "DELETE FROM utilisateur WHERE id = ?";
        try (PreparedStatement pst = cnx.prepareStatement(req)) {
            pst.setInt(1, u.getId());
            pst.executeUpdate();
        }
    }

    @Override
    public void updateEntity(int id, Utilisateur u) throws SQLException {
        String req = "UPDATE utilisateur SET nom = ?, prenom = ?, email = ?, role = ?, status = ? WHERE id = ?";
        try (PreparedStatement pst = cnx.prepareStatement(req)) {
            pst.setString(1, u.getNom());
            pst.setString(2, u.getPrenom());
            pst.setString(3, u.getEmail());
            pst.setString(4, u.getRole());
            pst.setString(5, u.getStatus());
            pst.setInt(6, id);
            pst.executeUpdate();
        }
    }

    @Override
    public List<Utilisateur> getData() throws SQLException {
        List<Utilisateur> list = new ArrayList<>();
        String req = "SELECT * FROM utilisateur";
        try (Statement st = cnx.createStatement(); ResultSet rs = st.executeQuery(req)) {
            while (rs.next()) {
                list.add(extractUser(rs));
            }
        }
        return list;
    }

    public List<Utilisateur> searchByRole(String role) throws SQLException {
        List<Utilisateur> list = new ArrayList<>();
        String req = "SELECT * FROM utilisateur WHERE role LIKE ?";
        try (PreparedStatement pst = cnx.prepareStatement(req)) {
            pst.setString(1, "%" + role + "%");
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                list.add(extractUser(rs));
            }
        }
        return list;
    }

    public Utilisateur getUserByEmail(String email) throws SQLException {
        String req = "SELECT * FROM utilisateur WHERE email = ?";
        try (PreparedStatement pst = cnx.prepareStatement(req)) {
            pst.setString(1, email);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                return extractUser(rs);
            }
        }
        return null;
    }

    private Utilisateur extractUser(ResultSet rs) throws SQLException {
        Utilisateur u = new Utilisateur();
        u.setId(rs.getInt("id"));
        u.setNom(rs.getString("nom"));
        u.setPrenom(rs.getString("prenom"));
        u.setEmail(rs.getString("email"));
        u.setMdp(rs.getString("mdp"));
        u.setPdpUrl(rs.getString("pdp_url"));
        
        Timestamp ts = rs.getTimestamp("date_inscription");
        if (ts != null) {
            u.setDateInscription(ts.toLocalDateTime());
        }
        
        u.setRole(rs.getString("role"));
        u.setStatus(rs.getString("status"));
        return u;
    }
}
