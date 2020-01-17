package com.dg.easyReplication;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;

import org.postgresql.PGConnection;

public class PGEasyReplication {

	private String publication;
	private String slot;
	private boolean slotDropIfExists;
	private Stream stream;
	
	public PGEasyReplication(String server, String database, String user, String password, String pub) {
		this(server, database, user, password, pub, "easy_slot_" + pub, false);
	}
	
	public PGEasyReplication(String server, String database, String user, String password, String pub, String slt) {
		this(server, database, user, password, pub, slt, false);
	}
	
	public PGEasyReplication(String server, String database, String user, String password, String pub, String slt, boolean sltDropIfExists) {
		this.publication = pub;
		this.slot = slt;
		this.slotDropIfExists = sltDropIfExists;
		
		ConnectionManager.setProperties(server, database, user, password);
		ConnectionManager.createSQLConnection();
		ConnectionManager.createReplicationConnection();
	}

	public void initializeLogicalReplication() {
		try {
			PreparedStatement stmt = ConnectionManager.getSQLConnection()
					.prepareStatement("select 1 from pg_catalog.pg_replication_slots WHERE slot_name = ?");
			
	    	stmt.setString(1, this.slot);
	    	ResultSet rs = stmt.executeQuery();
	    	
	    	if(rs.next()) {	// If slot exists
	    		if (this.slotDropIfExists) {
	    			this.dropReplicationSlot();
	    			this.createReplicationSlot();
	    		}
    		} else {
    			this.createReplicationSlot();
    		}
			
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public void createReplicationSlot() {						
		try {
			PGConnection pgcon = ConnectionManager.getReplicationConnection().unwrap(PGConnection.class);
			
			pgcon.getReplicationAPI()
				.createReplicationSlot()
				.logical()
				.withSlotName(this.slot)
				.withOutputPlugin("pgoutput") // More details about pgoutput options: https://github.com/postgres/postgres/blob/master/src/backend/replication/pgoutput/pgoutput.c
				.make();
			
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public void dropReplicationSlot() {	
		try {
			PGConnection pgcon = ConnectionManager.getReplicationConnection().unwrap(PGConnection.class);
			pgcon.getReplicationAPI().dropReplicationSlot(this.slot);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public Event getSnapshot() {
		Event event = null;

		try {
			Snapshot snapshot = new Snapshot(this.publication);
			event = snapshot.getInitialSnapshot();
			
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return event;
	}

	public Event readEvent() {
		return this.readEvent(true, true, null);
	}
	
	public Event readEvent(boolean isSimpleEvent) {
		return this.readEvent(isSimpleEvent, true, null);
	}
	
	public Event readEvent(boolean isSimpleEvent, boolean withBeginCommit) {
		return this.readEvent(isSimpleEvent, withBeginCommit, null);
	}
	
	public Event readEvent(boolean isSimpleEvent, boolean withBeginCommit, Long startLSN) {
		Event event = null;

		try {			
			if(this.stream == null)	{	// First read
				this.stream = new Stream(this.publication, this.slot, startLSN);
			}
				
			event = this.stream.readStream(isSimpleEvent, withBeginCommit);

		} catch (SQLException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}

		return event;
	}
}
