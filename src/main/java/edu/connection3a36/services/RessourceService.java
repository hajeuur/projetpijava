package edu.connection3a36.services;

import edu.connection3a36.entities.Ressource;
import edu.connection3a36.interfaces.IService;
import edu.connection3a36.tools.MyConnection;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class RessourceService implements IService<Ressource> {

    private final Connection cnx = MyConnection.getInstance().getCnx();

    @Override
    public void addEntity(Ressource ressource) throws SQLException {
        // Unicité : même nom dans le même projet
        if (existsByNomAndProjet(ressource.getNom(), ressource.getProjetId())) {
            throw new SQLException("Une ressource avec ce nom existe déjà dans ce projet.");
        }
        String req = "INSERT INTO ressource (nom, url_ressource, description, type_ressource, date_creation, projet_id) "
                +
                "VALUES (?, ?, ?, ?, ?, ?)";
        PreparedStatement pst = cnx.prepareStatement(req);
        pst.setString(1, ressource.getNom());
        pst.setString(2, ressource.getUrlRessource());
        pst.setString(3, ressource.getDescription());
        pst.setString(4, ressource.getTypeRessource());
        pst.setDate(5, Date.valueOf(LocalDate.now()));
        pst.setInt(6, ressource.getProjetId());
        pst.executeUpdate();
        System.out.println("Ressource ajoutée avec succès.");
    }

    @Override
    public void deleteEntity(Ressource ressource) throws SQLException {
        String req = "DELETE FROM ressource WHERE id = ?";
        PreparedStatement pst = cnx.prepareStatement(req);
        pst.setInt(1, ressource.getId());
        pst.executeUpdate();
        System.out.println("Ressource supprimée.");
    }

    @Override
    public void updateEntity(int id, Ressource ressource) throws SQLException {
        String req = "UPDATE ressource SET nom=?, url_ressource=?, description=?, type_ressource=?, date_modification=?, projet_id=? WHERE id=?";
        PreparedStatement pst = cnx.prepareStatement(req);
        pst.setString(1, ressource.getNom());
        pst.setString(2, ressource.getUrlRessource());
        pst.setString(3, ressource.getDescription());
        pst.setString(4, ressource.getTypeRessource());
        pst.setDate(5, Date.valueOf(LocalDate.now()));
        pst.setInt(6, ressource.getProjetId());
        pst.setInt(7, id);
        pst.executeUpdate();
        System.out.println("Ressource modifiée.");
    }

    @Override
    public List<Ressource> getData() throws SQLException {
        List<Ressource> list = new ArrayList<>();
        String req = "SELECT * FROM ressource ORDER BY date_creation DESC";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(req);
        while (rs.next()) {
            list.add(mapResultSet(rs));
        }
        return list;
    }

    public List<Ressource> getByProjetId(int projetId) throws SQLException {
        List<Ressource> list = new ArrayList<>();
        String req = "SELECT * FROM ressource WHERE projet_id = ? ORDER BY nom";
        PreparedStatement pst = cnx.prepareStatement(req);
        pst.setInt(1, projetId);
        ResultSet rs = pst.executeQuery();
        while (rs.next()) {
            list.add(mapResultSet(rs));
        }
        return list;
    }

    public List<Ressource> filterByType(String typeRessource) throws SQLException {
        List<Ressource> list = new ArrayList<>();
        String req = "SELECT * FROM ressource WHERE type_ressource = ? ORDER BY nom";
        PreparedStatement pst = cnx.prepareStatement(req);
        pst.setString(1, typeRessource);
        ResultSet rs = pst.executeQuery();
        while (rs.next()) {
            list.add(mapResultSet(rs));
        }
        return list;
    }

    public boolean existsByNomAndProjet(String nom, int projetId) throws SQLException {
        String req = "SELECT COUNT(*) FROM ressource WHERE nom = ? AND projet_id = ?";
        PreparedStatement pst = cnx.prepareStatement(req);
        pst.setString(1, nom);
        pst.setInt(2, projetId);
        ResultSet rs = pst.executeQuery();
        if (rs.next())
            return rs.getInt(1) > 0;
        return false;
    }

    private Ressource mapResultSet(ResultSet rs) throws SQLException {
        Ressource r = new Ressource();
        r.setId(rs.getInt("id"));
        r.setNom(rs.getString("nom"));
        r.setUrlRessource(rs.getString("url_ressource"));
        r.setDescription(rs.getString("description"));
        r.setTypeRessource(rs.getString("type_ressource"));
        Date dc = rs.getDate("date_creation");
        if (dc != null)
            r.setDateCreation(dc.toLocalDate());
        Date dm = rs.getDate("date_modification");
        if (dm != null)
            r.setDateModification(dm.toLocalDate());
        r.setProjetId(rs.getInt("projet_id"));
        return r;
    }
}
