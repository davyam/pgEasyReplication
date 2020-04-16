package com.dg.easyReplication;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;

import org.postgresql.PGConnection;

import com.fasterxml.jackson.core.JsonProcessingException;

public class PGEasyReplication {

    private String publication;
    private String slot;
    private Stream stream;

    public static final boolean SLOT_DROP_IF_EXISTS_DEFAULT = false;
    public static final boolean IS_SIMPLE_EVENT_DEFAULT = true;
    public static final boolean INCLUDE_BEGIN_COMMIT_DEFAULT = false;
    public static final String MIME_TYPE_OUTPUT_DEFAULT = "application/json";

    public PGEasyReplication(String server, String database, String user, String password, String pub) {
        this(server, database, user, password, pub, "easy_slot_" + pub);
    }

    public PGEasyReplication(String server, String database, String user, String password, String pub, String slt) {
        this.publication = pub;
        this.slot = slt;

        ConnectionManager.setProperties(server, database, user, password);
        ConnectionManager.createSQLConnection();
        ConnectionManager.createReplicationConnection();
    }

    public void initializeLogicalReplication() {
        this.initializeLogicalReplication(SLOT_DROP_IF_EXISTS_DEFAULT);
    }

    public void initializeLogicalReplication(boolean slotDropIfExists) {
        try {
            PreparedStatement stmt = ConnectionManager.getSQLConnection().prepareStatement("select 1 from pg_catalog.pg_replication_slots WHERE slot_name = ?");

            stmt.setString(1, this.slot);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                // If slot exists
                if (slotDropIfExists) {
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

            // More details about pgoutput options in PostgreSQL project: https://github.com/postgres, source file: postgres/src/backend/replication/pgoutput/pgoutput.c
            pgcon.getReplicationAPI().createReplicationSlot().logical().withSlotName(this.slot).withOutputPlugin("pgoutput").make();

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
        return this.getSnapshot(MIME_TYPE_OUTPUT_DEFAULT);
    }

    public Event getSnapshot(String outputFormat) {
        Event event = null;

        try {
            Snapshot snapshot = new Snapshot(this.publication);
            event = snapshot.getInitialSnapshot(outputFormat);

        } catch (SQLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return event;
    }

    public Event readEvent() {
        return this.readEvent(IS_SIMPLE_EVENT_DEFAULT, INCLUDE_BEGIN_COMMIT_DEFAULT, MIME_TYPE_OUTPUT_DEFAULT, null);
    }

    public Event readEvent(boolean isSimpleEvent) {
        return this.readEvent(isSimpleEvent, INCLUDE_BEGIN_COMMIT_DEFAULT, MIME_TYPE_OUTPUT_DEFAULT, null);
    }

    public Event readEvent(boolean isSimpleEvent, boolean withBeginCommit) {
        return this.readEvent(isSimpleEvent, withBeginCommit, MIME_TYPE_OUTPUT_DEFAULT, null);
    }

    public Event readEvent(boolean isSimpleEvent, boolean withBeginCommit, String outputFormat) {
        return this.readEvent(isSimpleEvent, withBeginCommit, outputFormat, null);
    }

    public Event readEvent(boolean isSimpleEvent, boolean withBeginCommit, String outputFormat, Long startLSN) {
        Event event = null;

        try {
            if (this.stream == null) {
                // First read stream
                this.stream = new Stream(this.publication, this.slot, startLSN);
            }

            event = this.stream.readStream(isSimpleEvent, withBeginCommit, outputFormat);

        } catch (SQLException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        return event;
    }
}
