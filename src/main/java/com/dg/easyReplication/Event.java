package com.dg.easyReplication;

import java.util.LinkedList;

public class Event {
	
	private LinkedList<String> changes;
	private String lastLSN;
	private boolean isSimpleEvent;

	public Event(LinkedList<String> changes, String lsn, boolean simple) {
		this.changes = changes;
		this.lastLSN = lsn;
		this.isSimpleEvent = simple;
	}
	
	public LinkedList<String> getChanges() {
		return changes;
	}

	public String getLastLSN() {
		return lastLSN;
	}
	
	public boolean isSimpleEvent() {
		return isSimpleEvent;
	}
}
