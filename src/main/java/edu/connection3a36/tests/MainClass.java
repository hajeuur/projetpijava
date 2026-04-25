package edu.connection3a36.tests;

import edu.connection3a36.entities.Personne;
import edu.connection3a36.services.PersonneService;
import edu.connection3a36.tools.MyConnection;

import java.sql.SQLException;

public class MainClass {
    public static void main(String[] args) {
        //MyConnection mc = new MyConnection();
        Personne p = new Personne( "Gharbi" , "Anis");
        PersonneService ps = new PersonneService();
        try {
            //ps.addEntity2(p);
            System.out.println(ps.getData());
            MyConnection mc1 = MyConnection.getInstance();
            MyConnection mc2 = MyConnection.getInstance();
            System.out.println(mc1.hashCode() + "-" + mc2.hashCode());
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }
}
