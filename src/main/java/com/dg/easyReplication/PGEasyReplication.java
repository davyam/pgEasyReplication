package com.dg.easyReplication;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.LinkedList;

import org.postgresql.PGConnection;

public class PGEasyReplication {

	private String publication;
	private boolean messagePretty;
	private String slot;
	private Stream stream;

	public PGEasyReplication(String server, String port, String database, String ssl, String user, String password, String pub) {
		
		this(server, port, database, ssl, user, password, pub, true);
	}
	
	public PGEasyReplication(String server, String port, String database, String ssl, String user, String password, String pub, boolean pretty) {
		
		Datasource.setProperties(server, port, database, ssl, user, password);
		Datasource.createSQLConnection();
		Datasource.createReplicationConnection();

		this.publication = pub;
		this.slot = "easy_slot_" + pub;
		this.messagePretty = pretty;
	}

	public void initializeLogicalReplication() {

		this.dropReplicationSlot();
		this.createReplicationSlot();
	}

	public void createReplicationSlot() {
		
		PGConnection pgcon;
		try {
			pgcon = Datasource.getReplicationConnection().unwrap(PGConnection.class);
			
			pgcon.getReplicationAPI()
				.createReplicationSlot()
				.logical()
				.withSlotName(this.slot)
				.withOutputPlugin("pgoutput")
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
			// Ignore ERROR: replication slot ... does not exist
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

	public LinkedList<String> readLogicalReplicationSlot() {

		LinkedList<String> readMessageQueue = new LinkedList<String>();

		try {			
			if(this.stream == null)	{			
				this.stream = new Stream(this.publication, this.slot, this.messagePretty);
			}
				
			readMessageQueue = this.stream.readStream();

		} catch (SQLException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}

		return readMessageQueue;
	}
}
