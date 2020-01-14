package com.dg.easyReplication;

import java.util.LinkedList;

public class Event {
	
	private LinkedList<String> changes;
	private Long lastLSN;
	private boolean isSimpleEvent;

	public Event(LinkedList<String> changes, Long lsn, boolean simple) {
		this.changes = changes;
		this.lastLSN = lsn;
		this.isSimpleEvent = simple;
	}
	
	public LinkedList<String> getChanges() {
		return changes;
	}

	public Long getLastLSN() {
		return lastLSN;
	}
	
	public boolean isSimpleEvent() {
		return isSimpleEvent;
	}
}
