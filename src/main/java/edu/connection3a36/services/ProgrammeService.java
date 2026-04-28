package edu.connection3a36.services;

import edu.connection3a36.interfaces.IService;
import edu.connection3a36.tools.MyConnection;
import edu.connection3a36.entities.Medaille;
import edu.connection3a36.entities.Programme;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ProgrammeService implements IService<Programme> {

    private final Connection cnx = MyConnection.getInstance().getCnx();

    @Override
    public void addEntity(Programme p) throws SQLException {
        String req = "INSERT INTO programme (titre, dategeneration, score_pourcentage, meilleure_medaille) VALUES (?,?,?,?)";
        PreparedStatement pst = cnx.prepareStatement(req, Statement.RETURN_GENERATED_KEYS);
        pst.setString(1, p.getTitre());
        pst.setDate(2, Date.valueOf(p.getDategeneration() != null ? p.getDategeneration() : LocalDate.now()));
        pst.setInt(3, p.getScorePourcentage());
        pst.setString(4, p.getMeilleureMedaille() != null ? p.getMeilleureMedaille().getValue() : null);
        pst.executeUpdate();
        ResultSet rs = pst.getGeneratedKeys();
        if (rs.next()) p.setId(rs.getInt(1));
    }

    /** Utilisé uniquement si on veut créer un programme standalone (sans objectif). */
    public void addForObjectif(Programme p, int objectifId) throws SQLException {
        addEntity(p);
        if (objectifId > 0 && p.getId() > 0) {
            PreparedStatement upd = cnx.prepareStatement("UPDATE objectif SET programme_id=? WHERE id=?");
            upd.setInt(1, p.getId());
            upd.setInt(2, objectifId);
            upd.executeUpdate();
        }
    }

    @Override
    public void deleteEntity(Programme p) throws SQLException {
        new MotivationService().deleteByProgramme(p.getId());
        new TacheService().deleteByProgramme(p.getId());
        PreparedStatement pst = cnx.prepareStatement("DELETE FROM programme WHERE id = ?");
        pst.setInt(1, p.getId());
        pst.executeUpdate();
    }

    @Override
    public void updateEntity(int id, Programme p) throws SQLException {
        String req = "UPDATE programme SET titre=?, score_pourcentage=?, meilleure_medaille=? WHERE id=?";
        PreparedStatement pst = cnx.prepareStatement(req);
        pst.setString(1, p.getTitre());
        pst.setInt(2, p.getScorePourcentage());
        pst.setString(3, p.getMeilleureMedaille() != null ? p.getMeilleureMedaille().getValue() : null);
        pst.setInt(4, id);
        pst.executeUpdate();
    }

    @Override
    public List<Programme> getData() throws SQLException {
        List<Programme> list = new ArrayList<>();
        ResultSet rs = cnx.createStatement().executeQuery("SELECT * FROM programme ORDER BY dategeneration DESC");
        while (rs.next()) list.add(map(rs));
        return list;
    }

    public Programme getById(int id) throws SQLException {
        PreparedStatement pst = cnx.prepareStatement("SELECT * FROM programme WHERE id = ?");
        pst.setInt(1, id);
        ResultSet rs = pst.executeQuery();
        if (rs.next()) return map(rs);
        return null;
    }

    /** Récupère le programme via objectif.programme_id */
    public Programme getByObjectifId(int objectifId) throws SQLException {
        PreparedStatement pst = cnx.prepareStatement(
                "SELECT p.* FROM programme p INNER JOIN objectif o ON o.programme_id = p.id WHERE o.id = ?");
        pst.setInt(1, objectifId);
        ResultSet rs = pst.executeQuery();
        if (rs.next()) return map(rs);
        return null;
    }

    public void updateScore(int programmeId, int score, String medaille) throws SQLException {
        PreparedStatement pst = cnx.prepareStatement(
                "UPDATE programme SET score_pourcentage=?, meilleure_medaille=? WHERE id=?");
        pst.setInt(1, score);
        pst.setString(2, medaille);
        pst.setInt(3, programmeId);
        pst.executeUpdate();
    }

    private Programme map(ResultSet rs) throws SQLException {
        Programme p = new Programme();
        p.setId(rs.getInt("id"));
        p.setTitre(rs.getString("titre"));
        Date dg = rs.getDate("dategeneration");
        if (dg != null) p.setDategeneration(dg.toLocalDate());
        p.setScorePourcentage(rs.getInt("score_pourcentage"));
        p.setMeilleureMedaille(Medaille.fromValue(rs.getString("meilleure_medaille")));
        return p;
    }
}
