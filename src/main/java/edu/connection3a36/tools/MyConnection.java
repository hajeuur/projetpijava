package edu.connection3a36.tools;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MyConnection {
    private String url = "jdbc:mysql://localhost:3306/mentorai?zeroDateTimeBehavior=CONVERT_TO_NULL";
    private String login = "root";
    private String pwd = "";
    private Connection cnx;

    private static MyConnection instance;

    private MyConnection() {
        try {
            cnx = DriverManager.getConnection(url, login, pwd);
            System.out.println("✅ Connection établie à mentorai");
        } catch (SQLException e) {
            System.err.println("❌ Échec connexion MySQL: " + e.getMessage());
            System.err.println("💡 Vérifiez que MySQL est démarré (XAMPP/WAMP) sur le port 3306");
            System.err.println("💡 Vérifiez que la base 'mentorai' existe");
        }
    }

    public Connection getCnx() {
        // Tenter une reconnexion si la connexion est null ou fermée
        try {
            if (cnx == null || cnx.isClosed()) {
                System.out.println("🔄 Tentative de reconnexion à MySQL...");
                cnx = DriverManager.getConnection(url, login, pwd);
                System.out.println("✅ Reconnexion réussie !");
            }
        } catch (SQLException e) {
            System.err.println("❌ Reconnexion échouée: " + e.getMessage());
        }
        return cnx;
    }

    public static MyConnection getInstance() {
        if (instance == null)
            instance = new MyConnection();
        return instance;
    }
}
