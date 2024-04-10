package edu.ucsb.nceas.metacat.index;

import java.sql.SQLException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.dataone.service.types.v1.Event;
import org.dataone.service.types.v1.Identifier;

import edu.ucsb.nceas.MCTestCase;
import edu.ucsb.nceas.metacat.common.index.event.IndexEvent;

/**
 * @author jones
 * 
 *         Test class for the Version class.
 */
public class IndexEventDAOTest extends MCTestCase {

    private IndexEvent event = null;
    private static final String ACTION = IndexEvent.CREATE;

    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();

        // initialize the event
        event = new IndexEvent();
        event.setAction(ACTION);
        event.setDate(Calendar.getInstance().getTime());
        event.setDescription("Testing DAO");
        Identifier pid = new Identifier();
        pid.setValue("IndexEventDAOTest." + System.currentTimeMillis());
        event.setIdentifier(pid);
    }

    /**
     * Test saving
     */
    public void testSave() {
        try {
            // save
            IndexEventDAO.getInstance().add(event);
            // lookup
            IndexEvent savedEvent = IndexEventDAO.getInstance().get(event.getIdentifier());
            // check
            assertEquals(event.getIdentifier(), savedEvent.getIdentifier());
            assertEquals(event.getAction(), savedEvent.getAction());
            assertEquals(event.getDate(), savedEvent.getDate());
            assertEquals(event.getDescription(), savedEvent.getDescription());
        } catch (Exception e) {
            e.printStackTrace();
            fail("Could not save; " + e.getMessage());
        } finally {
            // try to clean up as best we can
            try {
                IndexEventDAO.getInstance().remove(event.getIdentifier());
            } catch (SQLException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    /**
     * Test removing
     */
    public void testRemove() {
        try {
            // save
            IndexEventDAO.getInstance().add(event);
            // remove
            IndexEventDAO.getInstance().remove(event.getIdentifier());
            // check
            IndexEvent savedEvent = IndexEventDAO.getInstance().get(event.getIdentifier());
            assertNull(savedEvent);

        } catch (Exception e) {
            e.printStackTrace();
            fail("Could not test removal; " + e.getMessage());
        }
    }
    
    /**
     * Test listing
     */
    public void testList() {
        try {
            
            // get the count
            Set<Identifier> allIdentifiers = IndexEventDAO.getInstance().getAllIdentifiers();
            int originalSize = allIdentifiers.size();
            
            // get one
            if (allIdentifiers != null && !allIdentifiers.isEmpty()) {
                IndexEvent existingEvent = IndexEventDAO.getInstance()
                                                    .get(allIdentifiers.iterator().next());
                assertNotNull(existingEvent);
            }
            // add one
            IndexEventDAO.getInstance().add(event);
            
            // get the count again
            int newSize = IndexEventDAO.getInstance().getAllIdentifiers().size();
            assertEquals(originalSize+1, newSize);
            
            // clean up
            IndexEventDAO.getInstance().remove(event.getIdentifier());
            

        } catch (Exception e) {
            e.printStackTrace();
            fail("Could not test removal; " + e.getMessage());
        }
    }
    
    /**
     * Test getting 
     * @throws Exception
     */
    public void testGet() throws Exception {
        Date time1 = new Date();
        Date oldestAge = new Date(time1.getTime() - 240000);
        // add one
        IndexEventDAO.getInstance().add(event);
        // get the list of index events which have the action of CREATE_FAILURE_TO_QUEUE
        List<IndexEvent> list = IndexEventDAO.getInstance().get(ACTION, oldestAge);
        boolean found = false;
        for (IndexEvent indexEvent : list) {
            if (indexEvent.getIdentifier().getValue().equals(event.getIdentifier().getValue())) {
                found = true;
                break;
            }
        }
        assertTrue(found);
        
        //set the oldest age to be future, so we shouldn't find the event in the list
        Date newTime = new Date();
        oldestAge = new Date(newTime.getTime() + 10000);
        // get the list of index events which have the action of CREATE_FAILURE_TO_QUEUE
        list = IndexEventDAO.getInstance().get(ACTION, oldestAge);
        found = false;
        for (IndexEvent indexEvent : list) {
            if (indexEvent.getIdentifier().getValue().equals(event.getIdentifier().getValue())) {
                found = true;
                break;
            }
        }
        assertTrue(!found);
        
        //set oldestAge null, which means no age limitation. So it should find the event
        oldestAge = null;
        // get the list of index events which have the action of CREATE_FAILURE_TO_QUEUE
        list = IndexEventDAO.getInstance().get(ACTION, oldestAge);
        found = false;
        for (IndexEvent indexEvent : list) {
            if (indexEvent.getIdentifier().getValue().equals(event.getIdentifier().getValue())) {
                found = true;
                break;
            }
        }
        assertTrue(found);
        
        // clean up
        IndexEventDAO.getInstance().remove(event.getIdentifier());
    }

}
