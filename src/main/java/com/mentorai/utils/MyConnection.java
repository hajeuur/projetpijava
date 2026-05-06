package com.mentorai.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MyConnection {
    private static final String URL      = "jdbc:mysql://localhost:3306/mentorai";
    private static final String USER     = "root";
    private static final String PASSWORD = "";

    public static Connection getInstance() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}