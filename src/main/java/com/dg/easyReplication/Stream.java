package com.dg.easyReplication;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.postgresql.PGConnection;
import org.postgresql.replication.LogSequenceNumber;
import org.postgresql.replication.PGReplicationStream;

public class Stream {

    private PGReplicationStream repStream;
    private Long lastReceiveLSN;
    private Decode decode;
    
    public static final String MIME_TYPE_OUTPUT_DEFAULT = "application/json";

    public Stream(String pub, String slt) throws SQLException {
        this(pub, slt, null);
    }

    public Stream(String pub, String slt, Long lsn) throws SQLException {
        PGConnection pgcon = ConnectionManager.getReplicationConnection().unwrap(PGConnection.class);

        if (lsn == null) {
            // More details about pgoutput options in PostgreSQL project: https://github.com/postgres, source file: postgres/src/backend/replication/pgoutput/pgoutput.c
            this.repStream = pgcon.getReplicationAPI().replicationStream().logical().withSlotName(slt).withSlotOption("proto_version", "1").withSlotOption("publication_names", pub)
                    .withStatusInterval(1, TimeUnit.SECONDS).start();

        } else {
            // Reading from LSN start position
            LogSequenceNumber startLSN = LogSequenceNumber.valueOf(lsn);

            // More details about pgoutput options in PostgreSQL project: https://github.com/postgres, source file: postgres/src/backend/replication/pgoutput/pgoutput.c
            this.repStream = pgcon.getReplicationAPI().replicationStream().logical().withSlotName(slt).withSlotOption("proto_version", "1").withSlotOption("publication_names", pub)
                    .withStatusInterval(1, TimeUnit.SECONDS).withStartPosition(startLSN).start();
        }
    }

    public Event readStream(boolean isSimpleEvent, boolean withBeginCommit, String outputFormat)
            throws SQLException, InterruptedException, ParseException, UnsupportedEncodingException, JsonProcessingException {

        LinkedList<String> messages = new LinkedList<String>();

        if (this.decode == null) {
            // First read stream
            this.decode = new Decode();
            decode.loadDataTypes();
        }

        while (true) {
            ByteBuffer buffer = this.repStream.readPending();

            if (buffer == null) {
                break;
            }

            HashMap<String, Object> message = null;

            if (isSimpleEvent) {
                message = this.decode.decodeLogicalReplicationMessageSimple(buffer, withBeginCommit);
            } else {
                message = this.decode.decodeLogicalReplicationMessage(buffer, withBeginCommit);
            }

            if (!message.isEmpty()) { // Skip empty messages
                messages.addLast(this.convertMessage(message, outputFormat.trim().toLowerCase()));
            }

            // Replication feedback
            this.repStream.setAppliedLSN(this.repStream.getLastReceiveLSN());
            this.repStream.setFlushedLSN(this.repStream.getLastReceiveLSN());
        }

        this.lastReceiveLSN = this.repStream.getLastReceiveLSN().asLong();

        return new Event(messages, this.lastReceiveLSN, isSimpleEvent, withBeginCommit, false);
    }

    public String convertMessage(HashMap<String, Object> message, String outputFormat) throws JsonProcessingException {

        switch (outputFormat) {
        case MIME_TYPE_OUTPUT_DEFAULT:

            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString(message);

        default:

            throw new IllegalArgumentException("Invalid output format!");
        }
    }

    public Long getLastReceiveLSN() {
        return this.lastReceiveLSN;
    }
}
