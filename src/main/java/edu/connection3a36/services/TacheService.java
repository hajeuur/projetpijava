package edu.connection3a36.services;

import edu.connection3a36.interfaces.IService;
import edu.connection3a36.tools.MyConnection;
import edu.connection3a36.entities.Etat;
import edu.connection3a36.entities.Tache;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TacheService implements IService<Tache> {

    private final Connection cnx = MyConnection.getInstance().getCnx();

    @Override
    public void addEntity(Tache t) throws SQLException {
        String req = "INSERT INTO tache (ordre, titre, description, etat, programme_id) VALUES (?,?,?,?,?)";
        PreparedStatement pst = cnx.prepareStatement(req, Statement.RETURN_GENERATED_KEYS);
        pst.setInt(1, t.getOrdre());
        pst.setString(2, t.getTitre());
        pst.setString(3, t.getDescription() != null ? t.getDescription() : "");
        pst.setString(4, t.getEtat() != null ? t.getEtat().getValue() : Etat.encours.getValue());
        pst.setInt(5, t.getProgrammeId());
        pst.executeUpdate();
        ResultSet rs = pst.getGeneratedKeys();
        if (rs.next()) t.setId(rs.getInt(1));
    }

    @Override
    public void deleteEntity(Tache t) throws SQLException {
        PreparedStatement pst = cnx.prepareStatement("DELETE FROM tache WHERE id = ?");
        pst.setInt(1, t.getId());
        pst.executeUpdate();
    }

    @Override
    public void updateEntity(int id, Tache t) throws SQLException {
        String req = "UPDATE tache SET ordre=?, titre=?, description=?, etat=? WHERE id=?";
        PreparedStatement pst = cnx.prepareStatement(req);
        pst.setInt(1, t.getOrdre());
        pst.setString(2, t.getTitre());
        pst.setString(3, t.getDescription() != null ? t.getDescription() : "");
        pst.setString(4, t.getEtat() != null ? t.getEtat().getValue() : Etat.encours.getValue());
        pst.setInt(5, id);
        pst.executeUpdate();
    }

    @Override
    public List<Tache> getData() throws SQLException {
        List<Tache> list = new ArrayList<>();
        ResultSet rs = cnx.createStatement().executeQuery("SELECT * FROM tache ORDER BY programme_id, ordre");
        while (rs.next()) list.add(map(rs));
        return list;
    }

    public List<Tache> getByProgramme(int programmeId) throws SQLException {
        List<Tache> list = new ArrayList<>();
        PreparedStatement pst = cnx.prepareStatement(
                "SELECT * FROM tache WHERE programme_id = ? ORDER BY ordre ASC");
        pst.setInt(1, programmeId);
        ResultSet rs = pst.executeQuery();
        while (rs.next()) list.add(map(rs));
        return list;
    }

    public void updateEtat(int tacheId, String etat) throws SQLException {
        PreparedStatement pst = cnx.prepareStatement("UPDATE tache SET etat=? WHERE id=?");
        pst.setString(1, etat);
        pst.setInt(2, tacheId);
        pst.executeUpdate();
    }

    public void deleteByProgramme(int programmeId) throws SQLException {
        PreparedStatement pst = cnx.prepareStatement("DELETE FROM tache WHERE programme_id = ?");
        pst.setInt(1, programmeId);
        pst.executeUpdate();
    }

    /**
     * Valide une tâche avant insertion/modification.
     * @return liste des erreurs (vide = valide)
     */
    public List<String> validate(edu.connection3a36.entities.Tache t) {
        List<String> erreurs = new ArrayList<>();
        if (t.getTitre() == null || t.getTitre().isBlank())
            erreurs.add("Le titre de la tache est obligatoire");
        else if (t.getTitre().trim().length() < 2)
            erreurs.add("Le titre doit contenir au moins 2 caractères");
        if (t.getOrdre() <= 0)
            erreurs.add("L ordre doit etre un entier positif");
        if (t.getProgrammeId() <= 0)
            erreurs.add("Le programme est obligatoire");
        return erreurs;
    }

    private Tache map(ResultSet rs) throws SQLException {
        Tache t = new Tache();
        t.setId(rs.getInt("id"));
        t.setOrdre(rs.getInt("ordre"));
        t.setTitre(rs.getString("titre"));
        t.setDescription(rs.getString("description"));
        try { t.setEtat(Etat.fromValue(rs.getString("etat"))); }
        catch (Exception e) { t.setEtat(Etat.Abandonner); }
        t.setProgrammeId(rs.getInt("programme_id"));
        return t;
    }
}
