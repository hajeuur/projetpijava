package com.esprit;

import edu.connection3a36.tools.MyConnection;
import java.sql.Connection;

public class DatabaseConnection {
    public static Connection getInstance() {
        return MyConnection.getInstance().getCnx();
    }
}

