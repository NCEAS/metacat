package edu.ucsb.nceas.metacat.event;

import java.util.ArrayList;
import java.util.List;

public class MetacatEventService {
	
	private static MetacatEventService instance;
	
	private List<MetacatEventObserver> observers;
	
	private MetacatEventService() {
		observers = new ArrayList<MetacatEventObserver>();
	}
	
	public static MetacatEventService getInstance() {
		if (instance == null) {
			instance = new MetacatEventService();
		}
		return instance;
	}
	
	public void addMetacatEventObserver(MetacatEventObserver o) {
		observers.add(o);
	}
	
	public void removeMetacatEventObserver(MetacatEventObserver o) {
		observers.remove(o);
	}
	
	public void notifyMetacatEventObservers(MetacatEvent e) {
		for (MetacatEventObserver o: observers) {
			o.handleEvent(e);
		}
	}
}
