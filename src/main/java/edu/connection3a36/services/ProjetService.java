package edu.connection3a36.services;

import edu.connection3a36.entities.Projet;
import edu.connection3a36.interfaces.IService;
import edu.connection3a36.tools.MyConnection;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ProjetService implements IService<Projet> {

    private final Connection cnx = MyConnection.getInstance().getCnx();

    @Override
    public void addEntity(Projet projet) throws SQLException {
        // Unicité : même titre dans le même parcours
        if (existsByTitreAndParcours(projet.getTitre(), projet.getParcoursId())) {
            throw new SQLException("Un projet avec ce titre existe déjà dans ce parcours.");
        }
        String req = "INSERT INTO projet (titre, type, description, technologies, date_debut, date_fin, date_creation, parcours_id) "
                +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement pst = cnx.prepareStatement(req);
        pst.setString(1, projet.getTitre());
        pst.setString(2, projet.getType());
        pst.setString(3, projet.getDescription());
        pst.setString(4, projet.getTechnologies());
        pst.setDate(5, projet.getDateDebut() != null ? Date.valueOf(projet.getDateDebut()) : null);
        pst.setDate(6, projet.getDateFin() != null ? Date.valueOf(projet.getDateFin()) : null);
        pst.setDate(7, Date.valueOf(LocalDate.now()));
        pst.setInt(8, projet.getParcoursId());
        pst.executeUpdate();
        System.out.println("Projet ajouté avec succès.");
    }

    @Override
    public void deleteEntity(Projet projet) throws SQLException {
        // Suppression en cascade (application level) : d'abord supprimer toutes les
        // ressources associées
        RessourceService ressourceService = new RessourceService();
        List<edu.connection3a36.entities.Ressource> ressources = ressourceService.getByProjetId(projet.getId());
        for (edu.connection3a36.entities.Ressource res : ressources) {
            ressourceService.deleteEntity(res);
        }

        // Ensuite supprimer le projet lui-même
        String req = "DELETE FROM projet WHERE id = ?";
        PreparedStatement pst = cnx.prepareStatement(req);
        pst.setInt(1, projet.getId());
        pst.executeUpdate();
        System.out.println("Projet supprimé.");
    }

    @Override
    public void updateEntity(int id, Projet projet) throws SQLException {
        String req = "UPDATE projet SET titre=?, type=?, description=?, technologies=?, " +
                "date_debut=?, date_fin=?, date_modification=?, parcours_id=? WHERE id=?";
        PreparedStatement pst = cnx.prepareStatement(req);
        pst.setString(1, projet.getTitre());
        pst.setString(2, projet.getType());
        pst.setString(3, projet.getDescription());
        pst.setString(4, projet.getTechnologies());
        pst.setDate(5, projet.getDateDebut() != null ? Date.valueOf(projet.getDateDebut()) : null);
        pst.setDate(6, projet.getDateFin() != null ? Date.valueOf(projet.getDateFin()) : null);
        pst.setDate(7, Date.valueOf(LocalDate.now()));
        pst.setInt(8, projet.getParcoursId());
        pst.setInt(9, id);
        pst.executeUpdate();
        System.out.println("Projet modifié.");
    }

    @Override
    public List<Projet> getData() throws SQLException {
        List<Projet> list = new ArrayList<>();
        String req = "SELECT * FROM projet ORDER BY date_creation DESC";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(req);
        while (rs.next()) {
            list.add(mapResultSet(rs));
        }
        return list;
    }

    public List<Projet> getByParcoursId(int parcoursId) throws SQLException {
        List<Projet> list = new ArrayList<>();
        String req = "SELECT * FROM projet WHERE parcours_id = ? ORDER BY titre";
        PreparedStatement pst = cnx.prepareStatement(req);
        pst.setInt(1, parcoursId);
        ResultSet rs = pst.executeQuery();
        while (rs.next()) {
            list.add(mapResultSet(rs));
        }
        return list;
    }

    public List<Projet> searchByTitre(String keyword) throws SQLException {
        List<Projet> list = new ArrayList<>();
        String req = "SELECT * FROM projet WHERE titre LIKE ? OR technologies LIKE ? ORDER BY titre";
        PreparedStatement pst = cnx.prepareStatement(req);
        pst.setString(1, "%" + keyword + "%");
        pst.setString(2, "%" + keyword + "%");
        ResultSet rs = pst.executeQuery();
        while (rs.next()) {
            list.add(mapResultSet(rs));
        }
        return list;
    }

    public boolean existsByTitreAndParcours(String titre, int parcoursId) throws SQLException {
        String req = "SELECT COUNT(*) FROM projet WHERE titre = ? AND parcours_id = ?";
        PreparedStatement pst = cnx.prepareStatement(req);
        pst.setString(1, titre);
        pst.setInt(2, parcoursId);
        ResultSet rs = pst.executeQuery();
        if (rs.next())
            return rs.getInt(1) > 0;
        return false;
    }

    private Projet mapResultSet(ResultSet rs) throws SQLException {
        Projet p = new Projet();
        p.setId(rs.getInt("id"));
        p.setTitre(rs.getString("titre"));
        p.setType(rs.getString("type"));
        p.setDescription(rs.getString("description"));
        p.setTechnologies(rs.getString("technologies"));
        Date dd = rs.getDate("date_debut");
        if (dd != null)
            p.setDateDebut(dd.toLocalDate());
        Date df = rs.getDate("date_fin");
        if (df != null)
            p.setDateFin(df.toLocalDate());
        Date dc = rs.getDate("date_creation");
        if (dc != null)
            p.setDateCreation(dc.toLocalDate());
        Date dm = rs.getDate("date_modification");
        if (dm != null)
            p.setDateModification(dm.toLocalDate());
        p.setParcoursId(rs.getInt("parcours_id"));
        return p;
    }
}
