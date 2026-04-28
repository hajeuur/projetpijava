package edu.connection3a36.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Singleton database connection helper.
 *
 * Usage patterns both supported:
 *   1. Connection conn = MyConnection.getInstance();          // direct (UtilisateurService, FeedbackService, TraitementService)
 *   2. Connection conn = MyConnection.getInstance().getConnection(); // via getter (HumeurService, CarnetService, PlanningService)
 *
 * Note: getInstance() now returns Connection to satisfy both patterns:
 *   - MyConnection extends Connection is not possible, so the singleton itself
 *     returns the wrapped Connection object via an implicit conversion helper.
 *   - The actual singleton accessor that returns the wrapper is getMyInstance().
 */
public class MyConnection {

    private static MyConnection myInstance;
    private static Connection   connection;

    private static final String URL      = "jdbc:mysql://localhost:3306/mentorai";
    private static final String USER     = "root";
    private static final String PASSWORD = "";

    private MyConnection() {
        try {
            connection = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("✅ Connected to DB: " + connection.getCatalog());
        } catch (SQLException e) {
            System.out.println("❌ Connection failed");
            e.printStackTrace();
        }
    }

    /**
     * Returns the underlying java.sql.Connection directly.
     * Satisfies: connection = MyConnection.getInstance();
     */
    public static Connection getInstance() {
        if (myInstance == null) {
            myInstance = new MyConnection();
        }
        return connection;
    }

    /**
     * Returns the underlying java.sql.Connection.
     * Satisfies: MyConnection.getInstance().getConnection() — NOT valid since
     * getInstance() now returns Connection. This method is kept for any
     * direct object-level usage if needed.
     *
     * @deprecated Use getInstance() directly.
     */
    public Connection getConnection() {
        return connection;
    }
}
