package edu.connection3a36.tools;

import java.sql.DriverManager;
import java.sql.Connection;
import java.sql.SQLException;

public class MyConnection {

    private String url="jdbc:mysql://localhost:3306/mentorai";
    private String login="root";
    private String pwd="";

    private Connection cnx;

    public static MyConnection instance;


    private MyConnection() {
        try {
            cnx=DriverManager.getConnection(url,login,pwd);
            System.out.println("Connection established");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }


    }

    public Connection getCnx() {
        return cnx;
    }

    public static MyConnection getInstance() {
        if (instance==null) {
            instance=new MyConnection();

        }
        return instance;
    }
}
