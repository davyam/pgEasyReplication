package com.dg.easyReplication;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.LinkedList;
import java.util.concurrent.TimeUnit;

import org.json.simple.JSONObject;
import org.postgresql.PGConnection;
import org.postgresql.replication.LogSequenceNumber;
import org.postgresql.replication.PGReplicationStream;

public class Stream {
	
	private PGReplicationStream repStream;
	private String lastReceiveLSN;
	private Decode decode;
	
	public Stream(String pub, String slt) throws SQLException {
		this(pub, slt, null);
	}

	public Stream(String pub, String slt, String lsn) throws SQLException {
		PGConnection pgcon = Datasource.getReplicationConnection().unwrap(PGConnection.class);
		
		// More details about pgoutput options: https://github.com/postgres/postgres/blob/master/src/backend/replication/pgoutput/pgoutput.c

		if(lsn == null) {
			this.repStream = pgcon.getReplicationAPI()
					.replicationStream()
					.logical()
					.withSlotName(slt)
					.withSlotOption("proto_version", "1")
					.withSlotOption("publication_names", pub)
					.withStatusInterval(1, TimeUnit.SECONDS)
					.start();
			
		} else {	// Reading from LSN start position
			this.repStream = pgcon.getReplicationAPI()
					.replicationStream()
					.logical()
					.withSlotName(slt)
					.withSlotOption("proto_version", "1")
					.withSlotOption("publication_names", pub)
					.withStatusInterval(1, TimeUnit.SECONDS)
					.withStartPosition(LogSequenceNumber.valueOf(lsn))
					.start();
		}
	}

	public Event readStream(boolean isSimpleEvent)
			throws SQLException, InterruptedException, ParseException, UnsupportedEncodingException {

		LinkedList<String> changes = new LinkedList<String>();

		if (this.decode == null) {	// First read
			this.decode = new Decode();
			decode.loadDataTypes();
		}

		while (true) {
			ByteBuffer buffer = this.repStream.readPending();

			if (buffer == null) {
				break;
			}

			JSONObject json = new JSONObject();
			String change = "";

			if (isSimpleEvent) {
				change = this.decode.decodeLogicalReplicationMessageSimple(buffer, json).toJSONString();
			} else {
				change = this.decode.decodeLogicalReplicationMessage(buffer, json).toJSONString().replace("\\\"", "\"");
			}
			
			if (!change.equals("{}")) // Skip empty transactions
				changes.addLast(change);
			
			/* Feedback */
			this.repStream.setAppliedLSN(this.repStream.getLastReceiveLSN());
			this.repStream.setFlushedLSN(this.repStream.getLastReceiveLSN());
		}
		
		this.lastReceiveLSN = this.repStream.getLastReceiveLSN().asString();

		return new Event(changes, this.lastReceiveLSN, isSimpleEvent);
	}
	
	public String getLastReceiveLSN() {
		return this.lastReceiveLSN;
	}
}
