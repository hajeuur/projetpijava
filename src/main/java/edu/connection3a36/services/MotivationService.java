package edu.connection3a36.services;

import edu.connection3a36.interfaces.IService;
import edu.connection3a36.tools.MyConnection;
import edu.connection3a36.entities.Motivation;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * La colonne date dans la BDD s'appelle "dategeneratiomm" (typo existante dans la BDD).
 */
public class MotivationService implements IService<Motivation> {

    private final Connection cnx = MyConnection.getInstance().getCnx();

    @Override
    public void addEntity(Motivation m) throws SQLException {
        String req = "INSERT INTO motivation (dategeneratiomm, messagemotivant, programme_id) VALUES (?,?,?)";
        PreparedStatement pst = cnx.prepareStatement(req, Statement.RETURN_GENERATED_KEYS);
        pst.setDate(1, Date.valueOf(m.getDategeneration() != null ? m.getDategeneration() : LocalDate.now()));
        pst.setString(2, m.getMessagemotivant());
        pst.setInt(3, m.getProgrammeId());
        pst.executeUpdate();
        ResultSet rs = pst.getGeneratedKeys();
        if (rs.next()) m.setId(rs.getInt(1));
    }

    @Override
    public void deleteEntity(Motivation m) throws SQLException {
        PreparedStatement pst = cnx.prepareStatement("DELETE FROM motivation WHERE id = ?");
        pst.setInt(1, m.getId());
        pst.executeUpdate();
    }

    @Override
    public void updateEntity(int id, Motivation m) throws SQLException {
        PreparedStatement pst = cnx.prepareStatement(
                "UPDATE motivation SET messagemotivant=?, dategeneratiomm=? WHERE id=?");
        pst.setString(1, m.getMessagemotivant());
        pst.setDate(2, Date.valueOf(m.getDategeneration() != null ? m.getDategeneration() : LocalDate.now()));
        pst.setInt(3, id);
        pst.executeUpdate();
    }

    @Override
    public List<Motivation> getData() throws SQLException {
        List<Motivation> list = new ArrayList<>();
        ResultSet rs = cnx.createStatement().executeQuery(
                "SELECT * FROM motivation ORDER BY dategeneratiomm DESC");
        while (rs.next()) list.add(map(rs));
        return list;
    }

    public List<Motivation> getByProgramme(int programmeId) throws SQLException {
        List<Motivation> list = new ArrayList<>();
        PreparedStatement pst = cnx.prepareStatement(
                "SELECT * FROM motivation WHERE programme_id = ? ORDER BY dategeneratiomm DESC");
        pst.setInt(1, programmeId);
        ResultSet rs = pst.executeQuery();
        while (rs.next()) list.add(map(rs));
        return list;
    }

    public Motivation getLatestByProgramme(int programmeId) throws SQLException {
        PreparedStatement pst = cnx.prepareStatement(
                "SELECT * FROM motivation WHERE programme_id = ? ORDER BY dategeneratiomm DESC LIMIT 1");
        pst.setInt(1, programmeId);
        ResultSet rs = pst.executeQuery();
        if (rs.next()) return map(rs);
        return null;
    }

    public void deleteByProgramme(int programmeId) throws SQLException {
        PreparedStatement pst = cnx.prepareStatement("DELETE FROM motivation WHERE programme_id = ?");
        pst.setInt(1, programmeId);
        pst.executeUpdate();
    }

    private Motivation map(ResultSet rs) throws SQLException {
        Motivation m = new Motivation();
        m.setId(rs.getInt("id"));
        Date dg = rs.getDate("dategeneratiomm");
        if (dg != null) m.setDategeneration(dg.toLocalDate());
        m.setMessagemotivant(rs.getString("messagemotivant"));
        m.setProgrammeId(rs.getInt("programme_id"));
        return m;
    }
}
