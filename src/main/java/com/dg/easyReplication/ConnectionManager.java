package com.dg.easyReplication;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import org.postgresql.PGProperty;

public class ConnectionManager {

    private static String server;
    private static String database;
    private static String user;
    private static String password;
    private static Connection sqlConnection;
    private static Connection repConnection;

    public static void setProperties(String server, String database, String user, String password) {
        ConnectionManager.server = server;
        ConnectionManager.database = database;
        ConnectionManager.user = user;

        if (password == null) {
            password = "";
        }
        ConnectionManager.password = password;
    }

    public static void createReplicationConnection() {
        String url = "jdbc:postgresql://" + ConnectionManager.server + "/" + ConnectionManager.database;

        Properties props = new Properties();

        PGProperty.USER.set(props, ConnectionManager.user);
        PGProperty.PASSWORD.set(props, ConnectionManager.password);
        PGProperty.ASSUME_MIN_SERVER_VERSION.set(props, "10");
        PGProperty.REPLICATION.set(props, "database");
        PGProperty.PREFER_QUERY_MODE.set(props, "simple");

        Connection conn = null;

        try {
            Class.forName("org.postgresql.Driver");
            conn = DriverManager.getConnection(url, props);

        } catch (ClassNotFoundException e1) {
            e1.printStackTrace();
        } catch (SQLException e2) {
            e2.printStackTrace();
        }

        ConnectionManager.repConnection = conn;
    }

    public static Connection getReplicationConnection() {
        return ConnectionManager.repConnection;
    }

    public static void closeReplicationConnection() throws Exception {
        ConnectionManager.repConnection.close();
    }

    public static void createSQLConnection() {
        String url = "jdbc:postgresql://" + ConnectionManager.server + "/" + ConnectionManager.database;

        Properties props = new Properties();

        props.setProperty("user", ConnectionManager.user);
        props.setProperty("password", ConnectionManager.password);

        Connection conn = null;

        try {

            Class.forName("org.postgresql.Driver");
            conn = DriverManager.getConnection(url, props);
            conn.setAutoCommit(true);

        } catch (ClassNotFoundException e1) {
            e1.printStackTrace();
        } catch (SQLException e2) {
            e2.printStackTrace();
        }

        ConnectionManager.sqlConnection = conn;
    }

    public static Connection getSQLConnection() {
        return ConnectionManager.sqlConnection;
    }

    public static void closeSQLConnection() throws Exception {
        ConnectionManager.sqlConnection.close();
    }
}
