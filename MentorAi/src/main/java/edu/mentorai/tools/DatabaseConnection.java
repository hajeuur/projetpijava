package edu.mentorai.tools;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {
    private static final String URL = "jdbc:mysql://localhost:3306/mentorai";
    private static final String USER = "root";
    private static final String PASSWORD = "";

    //2
    private static Connection instance;


    //1
    private DatabaseConnection() {}

    //3

    public static Connection getInstance() throws SQLException {
        if (instance == null || instance.isClosed()) {
            instance = DriverManager.getConnection(URL, USER, PASSWORD);
        }
        return instance;
    }
}