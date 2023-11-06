package edu.ucsb.nceas.metacat.index.queue;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Timer;

import org.apache.commons.io.IOUtils;
import org.dataone.service.types.v1.Identifier;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import edu.ucsb.nceas.metacat.common.index.event.IndexEvent;
import edu.ucsb.nceas.metacat.index.IndexEventDAO;
import edu.ucsb.nceas.metacat.index.MetacatSolrIndex;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;

/**
 * A junit test class for the FailedIndexResubmitTimerTask class
 * @author tao
 *
 */
public class FailedIndexResubmitTimerTaskTest {
    private final IndexEvent event = new IndexEvent();
    private IndexEventDAO indexEventDAO = Mockito.mock(IndexEventDAO.class);

    /**
     * Set up a Mockito answer to answer the IndexEventDAO.get method
     * @throws Exception
     */
    @Before
    public void setUp() throws Exception {
        Mockito.when(indexEventDAO.get(anyString(),any(Date.class))).thenAnswer(new Answer() {
            public List<IndexEvent> answer(InvocationOnMock invocation) {
                List<IndexEvent> result = new ArrayList<IndexEvent>();
                Date now = new Date();
                // the date of event is not maxAgeOfFailedIndexTask (10 days) older than now
                if ((now.getTime() - event.getDate().getTime()) 
                                        < FailedIndexResubmitTimerTask.maxAgeOfFailedIndexTask) {
                    result.add(event);
                }
                return result;
            }
        });
    }

    /**
     * Test the scenario that a create index task can't be put into the index queue
     * @throws Exception
     */
    @Test
    public void testCreateFailure() throws Exception {
        // create an event
        Identifier guid = new Identifier();
        guid.setValue("FaileIndexResubmitTestCreateFailure." + System.currentTimeMillis());
        //add the identifier to the index event as a create_failure index task
        event.setAction(IndexEvent.CREATE_FAILURE_TO_QUEUE);
        event.setDate(Calendar.getInstance().getTime());
        event.setDescription("Testing DAO");
        event.setIdentifier(guid);
        FailedIndexResubmitTimerTask resubmitTask = new FailedIndexResubmitTimerTask();
        resubmitTask.setIndexEventDAO(indexEventDAO);
        // create timer to resubmit the failed index task
        Timer indexTimer = new Timer();
        long delay = 0;
        indexTimer.schedule(resubmitTask, delay);
        Thread.sleep(200);
        Mockito.verify(indexEventDAO);
    }

}
