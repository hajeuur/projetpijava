package edu.connection3a36.services;

import edu.connection3a36.entities.Personne;
import edu.connection3a36.interfaces.IService;
import edu.connection3a36.tools.MyConnection;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class PersonneService implements IService <Personne>
{


    @Override
    public void addEntity(Personne personne) throws SQLException {
        String requete = "INSERT INTO personne (nom,prenom) " +
                "VALUES"+"('"+personne.getNom()+"','"+personne.getPrenom()+"')";
        Statement st = MyConnection.getInstance().getCnx().createStatement();
        st.executeUpdate(requete);
        System.out.println("personne added");
    }

    public void addEntity2(Personne personne) throws SQLException {
        String requete = "INSERT INTO personne (nom,prenom) " +
                "VALUES"+"(?,?)";
        PreparedStatement pst = MyConnection.getInstance().getCnx().prepareStatement(requete);
        pst.setString(1,personne.getNom());
        pst.setString(2,personne.getPrenom());
        pst.executeUpdate();
        System.out.println("personne added");
    }

    @Override
    public void deleteEntity(Personne personne) throws SQLException {

    }

    @Override
    public void updateEntity(int id, Personne personne) throws SQLException {

    }

    @Override
    public List<Personne> getData() throws  SQLException {
        List<Personne> data = new ArrayList<>();
        String requete = "SELECT * FROM personne";
        Statement st = MyConnection.getInstance().getCnx().createStatement();
        ResultSet rs = st.executeQuery(requete);
        while (rs.next()) {
            Personne p = new Personne();
            p.setId (rs.getInt(1));
            p.setNom(rs.getString("nom"));
            p.setPrenom(rs.getString("prenom"));
            data.add(p);

        }
        return data;
    }

}
