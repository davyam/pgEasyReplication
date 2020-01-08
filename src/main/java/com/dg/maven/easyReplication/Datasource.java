package com.dg.maven.easyReplication;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

import org.postgresql.PGProperty;

public class Datasource {
	
	private static String server;
	private static String port;
	private static String database;
	private static String ssl;
	private static String user;
	private static String password;
	private static Connection sqlConnection;
	private static Connection repConnection;
	
	public static void setProperties(String server, String port, String database, String ssl, String user, String password) {
		
		Datasource.server = server;
		Datasource.port = port;
		Datasource.database = database;
		Datasource.ssl = ssl;
		Datasource.user = user;
		Datasource.password = password;
	}
	
	public static void createReplicationConnection() {

		String url = "jdbc:postgresql://" + Datasource.server + ":" + Datasource.port + "/" + Datasource.database;
		
		Properties props = new Properties();
		
		PGProperty.USER.set(props, Datasource.user);
		PGProperty.PASSWORD.set(props, Datasource.password);
		PGProperty.SSL.set(props, Datasource.ssl);
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
		
		Datasource.repConnection = conn;
	}
	
	public static Connection getReplicationConnection() {
		return Datasource.repConnection;
	}
	
	public static void createSQLConnection() {

		String url = "jdbc:postgresql://" + Datasource.server + ":" + Datasource.port + "/" + Datasource.database;
		
		Properties props = new Properties();
		
		props.setProperty("user",Datasource.user);
		props.setProperty("password",Datasource.password);
		props.setProperty("ssl",Datasource.ssl);
		
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

		Datasource.sqlConnection = conn;
	}
	
	public static Connection getSQLConnection() {
		return Datasource.sqlConnection;
	}

}
