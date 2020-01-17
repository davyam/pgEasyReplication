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
		
			String server = "192.168.32.51:5432";		// PostgreSQL server (host:port)
			String database = "test";					// PostgreSQL database
			String user = "postgres";					// PostgreSQL username
			String password = "";						// PostgreSQL password
			String publication = "cidade_pub";			// PostgreSQL publication name
			String slot = "slot_teste_cidade_pub";		// PostgreSQL slot name (OPTIONAL, DEFAUL "easy_slot_" + publication name)
			boolean slotDropIfExists = false;			// Drop slot if exists (OPTIONAL, DEFAULT false)
			
			
			// Instantiate pgEasyReplication class		
			
			PGEasyReplication pgEasyReplication = new PGEasyReplication(server, database, user, password, publication, slot, slotDropIfExists);
			
			// Snapshot
			
			Event eventSnapshots = pgEasyReplication.getSnapshot();
			LinkedList<String> snapshots = eventSnapshots.getData();
			
			System.out.println("TEST: Printing snapshot ...");
			
			for (String snapshot : snapshots) {
				System.out.println(snapshot);
			}
			
			// Initialize logical replication
			
			pgEasyReplication.initializeLogicalReplication();
			

			// Making data changes
			
			Class.forName("org.postgresql.Driver");
			
			String url = "jdbc:postgresql://" + server + "/" + database;
			Properties props = new Properties();
			props.setProperty("user",user);
			props.setProperty("password",password);
			
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
			
			boolean isSimpleEvent = true;		// Simple JSON data change (DEFAULT is true). Set false to return details like xid, xCommitTime, numColumns, TupleType, LSN, etc.
			boolean withBeginCommit = false;	// Include BEGIN and COMMIT events (DEFAULT is true).
			Long startLSN = null;				// Start LSN (DEFAULT is null). If null, get all the changes pending.
			
			while (true) {
				Event eventChanges = pgEasyReplication.readEvent(isSimpleEvent, withBeginCommit, startLSN);	// Using DEFAULT values: readEvent(), readEvent(isSimpleEvent), readEvent(isSimpleEvent, withBeginCommit)
				LinkedList<String> changes = eventChanges.getData();
				
				System.out.println("TEST: Printing data changes ...");
				
				for (String change : changes) {
					System.out.println(change);
				}
				
				System.out.println("TEST: Last LSN: " + eventChanges.getLastLSN().toString());
				
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
