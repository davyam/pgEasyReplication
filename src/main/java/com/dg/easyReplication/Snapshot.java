package com.dg.easyReplication;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.postgresql.PGConnection;
import org.postgresql.copy.CopyManager;

public class Snapshot {

	private String publication;
	
	public Snapshot(String pub) {
		this.publication = pub;
	}


	public ArrayList<String> getPublicationTables() throws SQLException {
		
    	PreparedStatement stmt = ConnectionManager.getSQLConnection()
    			.prepareStatement("SELECT schemaname, tablename FROM pg_publication_tables WHERE pubname = ?");
    	
    	stmt.setString(1, this.publication);
    	ResultSet rs = stmt.executeQuery();
    	
    	ArrayList<String> pubTables = new ArrayList<String>();
    	
    	while(rs.next()) {
    		pubTables.add(rs.getString(1) + "." + rs.getString(2));
    	}
    	
    	rs.close();
    	stmt.close();
    	
    	return pubTables;
	}
	
	public ArrayList<String> getInitialSnapshotTable(String tableName) throws SQLException, IOException {
		
		PGConnection pgcon = ConnectionManager.getSQLConnection().unwrap(PGConnection.class);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		
		CopyManager manager = pgcon.getCopyAPI();
		manager.copyOut("COPY (SELECT REGEXP_REPLACE(ROW_TO_JSON(t)::TEXT, '\\\\', '\\', 'g') FROM (SELECT * FROM " + tableName + ") t) TO STDOUT", out);
		
		return new ArrayList<String>(Arrays.asList(out.toString("UTF-8").split("\n")));
	}
	
	@SuppressWarnings("unchecked")
	public LinkedList<String> getInitialSnapshot() throws SQLException, IOException {
		
		LinkedList<String> messageQueue = new LinkedList<String>();
		
		ArrayList<String> pubTables = this.getPublicationTables();		
		JSONObject jsonSnapshot = new JSONObject();
				
		for (String table : pubTables) {			
			ArrayList<String> lines = this.getInitialSnapshotTable(table);
			JSONArray tableLines = new JSONArray();
			
			for (String line : lines) {
				tableLines.add(line);				
			}
			
			jsonSnapshot.put(table, tableLines);
		}
		
		messageQueue.addFirst("{\"snaphost\":" + jsonSnapshot.toJSONString().replace("\\\"", "\"") + "}");
		
		return messageQueue;
	}
}
