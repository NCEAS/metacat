package edu.ucsb.nceas.metacat.index.event;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.util.Calendar;

import org.dataone.service.types.v1.Event;
import org.dataone.service.types.v1.Identifier;
import org.junit.Test;

import edu.ucsb.nceas.metacat.common.index.event.IndexEvent;
import edu.ucsb.nceas.metacat.index.DistributedMapsFactory;

public class IndexEventEntryListenerIT {
	
	
	@Test
	public void testRoundtrip() {
		
		try {
			Identifier identifier = new Identifier();
			identifier.setValue("IndexEventEntryListenerIT" + System.currentTimeMillis());
			IndexEvent event = new IndexEvent();
			event.setAction(Event.CREATE);
			event.setDate(Calendar.getInstance().getTime());
			event.setDescription("Testing DAO");
			event.setIdentifier(identifier);
			DistributedMapsFactory.getIndexEventMap().put(identifier, event);
			
			// check
			IndexEvent savedEvent = DistributedMapsFactory.getIndexEventMap().get(identifier);
			assertNotNull(savedEvent);
			assertEquals(event.getIdentifier(), savedEvent.getIdentifier());
			assertEquals(event.getAction(), savedEvent.getAction());
			assertEquals(event.getDate(), savedEvent.getDate());
			assertEquals(event.getDescription(), savedEvent.getDescription());
			
			// remove
			DistributedMapsFactory.getIndexEventMap().remove(identifier);

			// check
			savedEvent = DistributedMapsFactory.getIndexEventMap().get(identifier);
			assertNull(savedEvent);
			
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		
	}

}
