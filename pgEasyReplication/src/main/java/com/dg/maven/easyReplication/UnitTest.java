package com.dg.maven.easyReplication;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedList;
import java.util.Properties;

public class UnitTest {

	public static void main(String[] args) {
		try {
			
			// OBS: Execute the pre_unit_test_postgresql.sql script on database before this test!

			// Variables
			
			String pgServer = "192.168.32.51";
			String pgPort = "5432";
			String pgDatabase = "test";
			String pgSSL = "false";
			String pgUser = "postgres";
			String pgPassword = "";
			String pgPublication = "cidade_pub";
			boolean messagePretty = false;
			
			
			// Initialize Logical Replication		

			PGEasyReplication pgEasyReplication = new PGEasyReplication(pgServer, pgPort, pgDatabase, pgSSL, pgUser, pgPassword, pgPublication, messagePretty);
			
			pgEasyReplication.initializeLogicalReplication();
			
			
			// Print snapshot
			
			System.out.println("TEST: Printing snapshot ...");
			
			LinkedList<String> snapshots = pgEasyReplication.getSnapshot();
			
			for (String snapshot : snapshots) {
				System.out.println(snapshot);
			}
			

			// Making data changes
			
			Class.forName("org.postgresql.Driver");
			
			String url = "jdbc:postgresql://" + pgServer + ":" + pgPort + "/" + pgDatabase;
			Properties props = new Properties();
			props.setProperty("user",pgUser);
			props.setProperty("password",pgPassword);
			props.setProperty("ssl",pgSSL);
			
			Connection conn = DriverManager.getConnection(url, props);

			if (conn != null)
				System.out.println("TEST: Connected to PostgreSQL Server");
			
			conn.setAutoCommit(true);
			Statement st = conn.createStatement();

	    	st.execute("INSERT INTO cidade (codigo, data_fund, nome) VALUES (4, '1929-10-19', 'UBERLANDIA');");
	    	st.execute("UPDATE cidade SET codigo = 20 WHERE codigo = 4;");
	    	st.execute("UPDATE cidade SET nome = 'TERRA DO PAO DE QUEIJO' WHERE nome = 'UBERLANDIA';");
	    	st.execute("DELETE FROM cidade WHERE codigo >= 4;");
	    	
			st.close();
			
			
			// Print data changes

			while (true) {
				System.out.println("TEST: Printing data changes ...");
				
				LinkedList<String> changes = pgEasyReplication.readLogicalReplicationSlot();
				
				for (String change : changes) {
					System.out.println(change);
				}

				try {
					Thread.sleep(3000);	// Sleep 3 seconds
				} catch (InterruptedException ex) {
					ex.printStackTrace();
				}

			}

		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
}
