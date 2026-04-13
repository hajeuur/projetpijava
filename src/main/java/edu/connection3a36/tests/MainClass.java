package edu.connection3a36.tests;

import edu.connection3a36.entities.Personne;
import edu.connection3a36.services.PersonneService;
import edu.connection3a36.tools.MyConnection;

import java.sql.SQLException;

public class MainClass {

    public static void main(String[] args) {
        //MyConnection mc = new MyConnection();
        Personne personne = new Personne("Arbii" , "Seif");
        PersonneService ps = new PersonneService();
        try {
            //ps.addEntity2(personne);
            System.out.println(ps.getData());
            MyConnection mc1 = MyConnection.getInstance();
            MyConnection mc2= MyConnection.getInstance();
            System.out.println(mc1.hashCode()+" - " +mc2.hashCode());

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }
}
