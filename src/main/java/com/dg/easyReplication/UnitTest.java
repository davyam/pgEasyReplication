package com.dg.easyReplication;

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

			// Parameters (REPLACE VALUES)
		
			String pgHost = "192.168.32.51";			// PostgreSQL host (IP or hostname)
			String pgPort = "5432";						// PostgreSQL port
			String pgDatabase = "test";					// PostgreSQL database
			String pgSSL = "false";						// PostgreSQL SSL connection (true or false)
			String pgUser = "postgres";					// PostgreSQL user
			String pgPassword = "";						// PostgreSQL user password
			String pgPublication = "cidade_pub";		// PostgreSQL publication
			String pgSlot = "slot_teste_cidade_pub";	// PostgreSQL slot name (OPTIONAL)
			boolean slotDropIfExists = true;			// PostgreSQL slot name (OPTIONAL)
			
			
			// Instantiate pgEasyReplication class		
			
			PGEasyReplication pgEasyReplication = new PGEasyReplication(pgHost, pgPort, pgDatabase, pgSSL, pgUser, pgPassword, pgPublication, pgSlot, slotDropIfExists);
			
			// Snapshot
			
			LinkedList<String> snapshots = pgEasyReplication.getSnapshot();
			
			System.out.println("TEST: Printing snapshot ...");
			
			for (String snapshot : snapshots) {
				System.out.println(snapshot);
			}
			
			// Initialize logical replication
			
			pgEasyReplication.initializeLogicalReplication();
			

			// Making data changes
			
			Class.forName("org.postgresql.Driver");
			
			String url = "jdbc:postgresql://" + pgHost + ":" + pgPort + "/" + pgDatabase;
			Properties props = new Properties();
			props.setProperty("user",pgUser);
			props.setProperty("password",pgPassword);
			props.setProperty("ssl",pgSSL);
			
			Connection conn = DriverManager.getConnection(url, props);

			if (conn != null)
				System.out.println("TEST: Connected to PostgreSQL Server");
			
			conn.setAutoCommit(true);
			Statement st = conn.createStatement();
			
			System.out.println("TEST: Changing data ...");

	    	st.execute("INSERT INTO cidade (codigo, data_fund, nome) VALUES (4, '1929-10-19', 'UBERLANDIA');");
	    	st.execute("UPDATE cidade SET codigo = 20 WHERE codigo = 4;");
	    	st.execute("UPDATE cidade SET nome = 'TERRA DO PAO DE QUEIJO' WHERE nome = 'UBERLANDIA';");
	    	st.execute("DELETE FROM cidade WHERE codigo >= 4;");
	    	
			st.close();
			
			
			// Capture data changes
			
			boolean isSimpleEvent = true;	// Simple JSON data change (default is true).  Set false to return details like xid, xCommitTime, numColumns, TupleType, LSN, etc

			while (true) {	
				Event event = pgEasyReplication.readEvent(isSimpleEvent);
				LinkedList<String> changes = event.getChanges();
				
				System.out.println("TEST: Printing data changes ...");
				
				for (String change : changes) {
					System.out.println(change);
				}
				
				System.out.println("TEST: Last LSN: " + event.getLastLSN());
				
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
