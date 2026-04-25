package edu.connection3a36.services;

import edu.connection3a36.entities.Parcours;
import edu.connection3a36.interfaces.IService;
import edu.connection3a36.tools.MyConnection;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ParcoursService implements IService<Parcours> {

    private final Connection cnx = MyConnection.getInstance().getCnx();

    @Override
    public void addEntity(Parcours parcours) throws SQLException {
        // Unicité : même titre (insensible à la casse)
        if (existsByTitre(parcours.getTitre())) {
            throw new SQLException("Un parcours avec ce titre existe déjà.");
        }
        String req = "INSERT INTO parcours (type_parcours, titre, date_debut, date_fin, description, " +
                "etablissement, diplome, specialite, entreprise, poste, type_contrat, date_creation) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement pst = cnx.prepareStatement(req);
        pst.setString(1, parcours.getTypeParcours());
        pst.setString(2, parcours.getTitre());
        pst.setDate(3, parcours.getDateDebut() != null ? Date.valueOf(parcours.getDateDebut()) : null);
        pst.setDate(4, parcours.getDateFin() != null ? Date.valueOf(parcours.getDateFin()) : null);
        pst.setString(5, parcours.getDescription());
        pst.setString(6, parcours.getEtablissement());
        pst.setString(7, parcours.getDiplome());
        pst.setString(8, parcours.getSpecialite());
        pst.setString(9, parcours.getEntreprise());
        pst.setString(10, parcours.getPoste());
        pst.setString(11, parcours.getTypeContrat());
        pst.setDate(12, Date.valueOf(LocalDate.now()));
        pst.executeUpdate();
        System.out.println("Parcours ajouté avec succès.");
    }

    @Override
    public void deleteEntity(Parcours parcours) throws SQLException {
        // Suppression en cascade : d'abord supprimer tous les projets associés
        ProjetService projetService = new ProjetService();
        List<edu.connection3a36.entities.Projet> projets = projetService.getByParcoursId(parcours.getId());
        for (edu.connection3a36.entities.Projet p : projets) {
            projetService.deleteEntity(p);
        }

        // Ensuite supprimer le parcours lui-même
        String req = "DELETE FROM parcours WHERE id = ?";
        PreparedStatement pst = cnx.prepareStatement(req);
        pst.setInt(1, parcours.getId());
        pst.executeUpdate();
        System.out.println("Parcours supprimé.");
    }

    @Override
    public void updateEntity(int id, Parcours parcours) throws SQLException {
        // Unicité : même titre (exclure l'ID actuel)
        if (existsByTitreExcludingId(parcours.getTitre(), id)) {
            throw new SQLException("Un parcours avec ce titre existe déjà.");
        }
        String req = "UPDATE parcours SET type_parcours=?, titre=?, date_debut=?, date_fin=?, " +
                "description=?, etablissement=?, diplome=?, specialite=?, entreprise=?, " +
                "poste=?, type_contrat=?, date_modification=? WHERE id=?";
        PreparedStatement pst = cnx.prepareStatement(req);
        pst.setString(1, parcours.getTypeParcours());
        pst.setString(2, parcours.getTitre());
        pst.setDate(3, parcours.getDateDebut() != null ? Date.valueOf(parcours.getDateDebut()) : null);
        pst.setDate(4, parcours.getDateFin() != null ? Date.valueOf(parcours.getDateFin()) : null);
        pst.setString(5, parcours.getDescription());
        pst.setString(6, parcours.getEtablissement());
        pst.setString(7, parcours.getDiplome());
        pst.setString(8, parcours.getSpecialite());
        pst.setString(9, parcours.getEntreprise());
        pst.setString(10, parcours.getPoste());
        pst.setString(11, parcours.getTypeContrat());
        pst.setDate(12, Date.valueOf(LocalDate.now()));
        pst.setInt(13, id);
        pst.executeUpdate();
        System.out.println("Parcours modifié.");
    }

    @Override
    public List<Parcours> getData() throws SQLException {
        List<Parcours> list = new ArrayList<>();
        String req = "SELECT * FROM parcours ORDER BY date_creation DESC";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(req);
        while (rs.next()) {
            list.add(mapResultSet(rs));
        }
        return list;
    }

    public Parcours getById(int id) throws SQLException {
        String req = "SELECT * FROM parcours WHERE id = ?";
        PreparedStatement pst = cnx.prepareStatement(req);
        pst.setInt(1, id);
        ResultSet rs = pst.executeQuery();
        if (rs.next()) {
            return mapResultSet(rs);
        }
        return null;
    }

    public List<Parcours> searchByTitre(String keyword) throws SQLException {
        List<Parcours> list = new ArrayList<>();
        String req = "SELECT * FROM parcours WHERE titre LIKE ? OR description LIKE ? ORDER BY titre";
        PreparedStatement pst = cnx.prepareStatement(req);
        pst.setString(1, "%" + keyword + "%");
        pst.setString(2, "%" + keyword + "%");
        ResultSet rs = pst.executeQuery();
        while (rs.next()) {
            list.add(mapResultSet(rs));
        }
        return list;
    }

    public List<Parcours> filterByType(String type) throws SQLException {
        List<Parcours> list = new ArrayList<>();
        String req = "SELECT * FROM parcours WHERE type_parcours = ? ORDER BY titre";
        PreparedStatement pst = cnx.prepareStatement(req);
        pst.setString(1, type);
        ResultSet rs = pst.executeQuery();
        while (rs.next()) {
            list.add(mapResultSet(rs));
        }
        return list;
    }

    public boolean existsByTitre(String titre) throws SQLException {
        String req = "SELECT COUNT(*) FROM parcours WHERE LOWER(titre) = LOWER(?)";
        PreparedStatement pst = cnx.prepareStatement(req);
        pst.setString(1, titre);
        ResultSet rs = pst.executeQuery();
        if (rs.next())
            return rs.getInt(1) > 0;
        return false;
    }

    /**
     * Vérifie si un parcours avec le même titre existe déjà, 
     * en excluant l'ID actuel (pour la modification).
     */
    public boolean existsByTitreExcludingId(String titre, int id) throws SQLException {
        String req = "SELECT COUNT(*) FROM parcours WHERE LOWER(titre) = LOWER(?) AND id != ?";
        PreparedStatement pst = cnx.prepareStatement(req);
        pst.setString(1, titre);
        pst.setInt(2, id);
        ResultSet rs = pst.executeQuery();
        if (rs.next())
            return rs.getInt(1) > 0;
        return false;
    }

    public int countByType(String type) throws SQLException {
        String req = "SELECT COUNT(*) FROM parcours WHERE type_parcours = ?";
        PreparedStatement pst = cnx.prepareStatement(req);
        pst.setString(1, type);
        ResultSet rs = pst.executeQuery();
        if (rs.next())
            return rs.getInt(1);
        return 0;
    }

    private Parcours mapResultSet(ResultSet rs) throws SQLException {
        Parcours p = new Parcours();
        p.setId(rs.getInt("id"));
        p.setTypeParcours(rs.getString("type_parcours"));
        p.setTitre(rs.getString("titre"));
        Date dd = rs.getDate("date_debut");
        if (dd != null)
            p.setDateDebut(dd.toLocalDate());
        Date df = rs.getDate("date_fin");
        if (df != null)
            p.setDateFin(df.toLocalDate());
        p.setDescription(rs.getString("description"));
        p.setEtablissement(rs.getString("etablissement"));
        p.setDiplome(rs.getString("diplome"));
        p.setSpecialite(rs.getString("specialite"));
        p.setEntreprise(rs.getString("entreprise"));
        p.setPoste(rs.getString("poste"));
        p.setTypeContrat(rs.getString("type_contrat"));
        Date dc = rs.getDate("date_creation");
        if (dc != null)
            p.setDateCreation(dc.toLocalDate());
        Date dm = rs.getDate("date_modification");
        if (dm != null)
            p.setDateModification(dm.toLocalDate());
        return p;
    }
}
