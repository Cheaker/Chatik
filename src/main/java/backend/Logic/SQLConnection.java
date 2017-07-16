package backend.Logic;

import java.sql.Connection;
import java.sql.DriverManager;

/**
 * Created by Wladimir on 02.07.2017.
 */
public class SQLConnection {

    private static SQLConnection sqlConnection = null;
    private Connection c = null;

    private SQLConnection() {
        try {
            Class.forName("org.postgresql.Driver");
            c = DriverManager.getConnection("jdbc:postgresql://localhost:5432/inet", "postgres", "123456");
            System.out.println("Opened database successfully");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static SQLConnection getInstance() {

        if (sqlConnection == null) {
            sqlConnection = new SQLConnection();
        }
        return sqlConnection;
    }

    public Connection getConnection() {
        return c;
    }
}
