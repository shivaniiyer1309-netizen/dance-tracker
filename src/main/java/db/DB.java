package db;

import java.sql.Connection;
import java.sql.DriverManager;

public class DB {

    private static final String URL =
            "jdbc:mysql://dancetracker-db.c72emgss8b54.us-east-2.rds.amazonaws.com:3306/dancetracker"
            + "?useSSL=false"
            + "&allowPublicKeyRetrieval=true"
            + "&serverTimezone=UTC";

    private static final String USER = "admin";

    private static final String PASS =
            "DanceRDS123!";

    public static Connection getConnection() throws Exception {

        Class.forName("com.mysql.cj.jdbc.Driver");

        return DriverManager.getConnection(
                URL,
                USER,
                PASS
        );
    }
}