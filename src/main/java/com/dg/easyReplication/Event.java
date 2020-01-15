package com.dg.easyReplication;

import java.util.LinkedList;

public class Event {
	
	private LinkedList<String> data;
	private Long lastLSN;
	private boolean isSimpleEvent;
	private boolean isSnapshot;

	public Event(LinkedList<String> data, Long lsn, boolean isSimple, boolean isSnap) {
		this.data = data;
		this.lastLSN = lsn;
		this.isSimpleEvent = isSimple;
		this.isSimpleEvent = isSnap;
	}
	
	public LinkedList<String> getData() {
		return data;
	}

	public Long getLastLSN() {
		return lastLSN;
	}
	
	public boolean isSimpleEvent() {
		return isSimpleEvent;
	}
	
	public boolean isSnapshot() {
		return isSnapshot;
	}
}
