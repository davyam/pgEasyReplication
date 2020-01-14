package com.dg.easyReplication;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.LinkedList;

import org.postgresql.PGConnection;

public class PGEasyReplication {

	private String publication;
	private String slot;
	private boolean slotDropIfExists;
	private boolean isSimpleEvent;
	private Stream stream;
	
	public PGEasyReplication(String host, String port, String database, String ssl, String user, String password, String pub) {
		this(host, port, database, ssl, user, password, pub, "easy_slot_" + pub, false, true);
	}
	
	public PGEasyReplication(String host, String port, String database, String ssl, String user, String password, String pub, String slt) {
		this(host, port, database, ssl, user, password, pub, slt, false, true);
	}
	
	public PGEasyReplication(String host, String port, String database, String ssl, String user, String password, String pub, String slt, boolean sltDropIfExists) {
		this(host, port, database, ssl, user, password, pub, slt, sltDropIfExists, true);
	}
	
	public PGEasyReplication(String host, String port, String database, String ssl, String user, String password, String pub, String slt, boolean sltDropIfExists, boolean isSimple) {
		this.publication = pub;
		this.slot = slt;
		this.slotDropIfExists = sltDropIfExists;
		this.isSimpleEvent = isSimple;
		
		Datasource.setProperties(host, port, database, ssl, user, password);
		Datasource.createSQLConnection();
		Datasource.createReplicationConnection();
	}

	public void initializeLogicalReplication() {
		try {
			PreparedStatement stmt = Datasource.getSQLConnection()
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
			PGConnection pgcon = Datasource.getReplicationConnection().unwrap(PGConnection.class);
			
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
			PGConnection pgcon = Datasource.getReplicationConnection().unwrap(PGConnection.class);
			pgcon.getReplicationAPI().dropReplicationSlot(this.slot);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public LinkedList<String> getSnapshot() {
		LinkedList<String> snapshotMessageQueue = new LinkedList<String>();

		try {
			Snapshot snapshot = new Snapshot(this.publication);
			snapshotMessageQueue = snapshot.getInitialSnapshot();
			
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return snapshotMessageQueue;
	}

	public Event readEvent() {
		return this.readEvent(null);
	}
	
	public Event readEvent(String lsn) {
		Event event = null;

		try {			
			if(this.stream == null)	{	// First read		
				this.stream = new Stream(this.publication, this.slot, this.isSimpleEvent, lsn);
			}
				
			event = this.stream.readStream();

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
