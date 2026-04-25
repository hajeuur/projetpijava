package com.mentorai.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

<<<<<<< HEAD
public class MyConnection {

    private static MyConnection instance;
    private Connection connection;

    private final String URL = "jdbc:mysql://localhost:3306/mentorai";
    private final String USER = "root";
    private final String PASSWORD = "";

    private MyConnection() {
        try {
            connection = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("✅ Connected to DB: " + connection.getCatalog());
        } catch (SQLException e) {
            System.out.println("❌ Connection failed");
            e.printStackTrace();
        }
    }

    public static MyConnection getInstance() {
        if (instance == null) {
            instance = new MyConnection();
        }
        return instance;
    }

    public Connection getConnection() {
=======
/**
 * Cette classe crée UNE SEULE connexion à la base de données.
 * C'est le "pont" entre Java et MySQL.
 * On utilise le pattern Singleton : une seule instance partagée.
 */
public class MyConnection {

    // L'adresse de ta base de données
    private static final String URL = "jdbc:mysql://localhost:3306/mentorai";

    // Ton nom d'utilisateur MySQL (par défaut : root)
    private static final String USER = "root";

    // Ton mot de passe MySQL (laisse vide si tu n'en as pas)
    private static final String PASSWORD = "";

    // La connexion (une seule pour tout le projet)
    private static Connection connection = null;

    // Constructeur privé : personne ne peut créer une instance depuis dehors
    private MyConnection() {}

    /**
     * Retourne la connexion. Si elle n'existe pas encore, elle la crée.
     */
    public static Connection getInstance() {
        if (connection == null) {
            try {
                connection = DriverManager.getConnection(URL, USER, PASSWORD);
                System.out.println("✅ Connexion à la base de données réussie !");
            } catch (SQLException e) {
                System.out.println("❌ Erreur de connexion : " + e.getMessage());
            }
        }
>>>>>>> origin/integration_amal
        return connection;
    }
}