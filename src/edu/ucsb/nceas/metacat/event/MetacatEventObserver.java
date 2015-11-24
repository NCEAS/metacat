package edu.ucsb.nceas.metacat.event;

public interface MetacatEventObserver {
	
	public void handleEvent(MetacatEvent e);

}
