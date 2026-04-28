package edu.connection3a36.services;

import edu.connection3a36.models.Traitement;
import edu.connection3a36.tools.MyConnection;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class TraitementService {

    private final Connection cnx = MyConnection.getInstance().getCnx();

    public void add(Traitement t) {
        String req = "INSERT INTO traitement (typetraitement, description, datetraitement, decision) VALUES (?,?,?,?)";
        try (PreparedStatement pst = cnx.prepareStatement(req)) {
            pst.setString(1, t.getTypetraitement());
            pst.setString(2, t.getDescription());
            pst.setDate(3, Date.valueOf(t.getDatetraitement() != null ? t.getDatetraitement() : LocalDate.now()));
            pst.setString(4, t.getDecision());
            pst.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erreur add Traitement : " + e.getMessage());
        }
    }

    public void update(Traitement t) {
        String req = "UPDATE traitement SET typetraitement=?, description=?, decision=? WHERE id=?";
        try (PreparedStatement pst = cnx.prepareStatement(req)) {
            pst.setString(1, t.getTypetraitement());
            pst.setString(2, t.getDescription());
            pst.setString(3, t.getDecision());
            pst.setInt(4, t.getId());
            pst.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erreur update Traitement : " + e.getMessage());
        }
    }

    public void delete(int id) {
        try (PreparedStatement pst = cnx.prepareStatement("DELETE FROM traitement WHERE id=?")) {
            pst.setInt(1, id);
            pst.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erreur delete Traitement : " + e.getMessage());
        }
    }

    public List<Traitement> getAll() {
        List<Traitement> list = new ArrayList<>();
        try (ResultSet rs = cnx.createStatement().executeQuery(
                "SELECT * FROM traitement ORDER BY datetraitement DESC")) {
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) {
            System.err.println("Erreur getAll Traitement : " + e.getMessage());
        }
        return list;
    }

    public Traitement getById(int id) {
        try (PreparedStatement pst = cnx.prepareStatement(
                "SELECT * FROM traitement WHERE id=?")) {
            pst.setInt(1, id);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) return map(rs);
        } catch (SQLException e) {
            System.err.println("Erreur getById Traitement : " + e.getMessage());
        }
        return null;
    }

    public int getDernierIdInsere() {
        try (ResultSet rs = cnx.createStatement().executeQuery(
                "SELECT MAX(id) FROM traitement")) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            System.err.println("Erreur getDernierIdInsere : " + e.getMessage());
        }
        return 0;
    }

    private Traitement map(ResultSet rs) throws SQLException {
        return new Traitement(
                rs.getInt("id"),
                rs.getString("typetraitement"),
                rs.getString("description"),
                rs.getDate("datetraitement").toLocalDate(),
                rs.getString("decision")
        );
    }
}
