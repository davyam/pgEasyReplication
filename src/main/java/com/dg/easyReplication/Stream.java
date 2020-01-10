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

	private boolean messagePretty;		
	private PGReplicationStream repStream;
	private Decode decode;

	public Stream(String pub, String slt, boolean pretty) throws SQLException {

		PGConnection pgcon = Datasource.getReplicationConnection().unwrap(PGConnection.class);
		
		// More details about pgoutput plugin: https://github.com/postgres/postgres/blob/master/src/backend/replication/pgoutput/pgoutput.c

		this.repStream = pgcon.getReplicationAPI()
				.replicationStream()
				.logical()
				.withSlotName(slt)
				.withSlotOption("proto_version", "1")
				.withSlotOption("publication_names", pub)
				.withStatusInterval(1, TimeUnit.SECONDS)
				.start();
		
		this.messagePretty = pretty;
	}

	public LinkedList<String> readStream()
			throws SQLException, InterruptedException, ParseException, UnsupportedEncodingException {

		LinkedList<String> messageQueue = new LinkedList<String>();

		if (this.decode == null) {
			this.decode = new Decode();
			decode.loadDataTypes();
		}

		while (true) {
			ByteBuffer buffer = this.repStream.readPending();

			if (buffer == null) {
				break;
			}

			JSONObject json = new JSONObject();
			
			String message = "";

			if (this.messagePretty) {
				message = this.decode.decodeLogicalReplicationMessagePretty(buffer, json).toJSONString();
			} else {
				message = this.decode.decodeLogicalReplicationMessage(buffer, json).toJSONString().replace("\\\"", "\"");
			}
			
			if (!message.equals("{}")) // Skip empty transactions
				messageQueue.addLast(message);

			/* Feedback */
			this.repStream.setAppliedLSN(this.repStream.getLastReceiveLSN());
			this.repStream.setFlushedLSN(this.repStream.getLastReceiveLSN());
		}

		return messageQueue;
	}
	
	public LogSequenceNumber getLastReceiveLSN() {
		return this.repStream.getLastReceiveLSN();
	}
}
