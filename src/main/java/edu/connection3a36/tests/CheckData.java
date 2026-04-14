package edu.connection3a36.tests;

import edu.connection3a36.services.ParcoursService;
import java.sql.SQLException;

public class CheckData {
    public static void main(String[] args) {
        try {
            System.out.println("Parcours count: " + new ParcoursService().getData().size());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
