package edu.ucsb.nceas.metacat.dataone.convert;

import edu.ucsb.nceas.metacat.dataone.D1NodeServiceTest;

import java.util.Date;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.dataone.service.types.v1.Event;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.NodeReference;
import org.dataone.service.types.v1.Subject;
import org.dataone.service.types.v2.Log;
import org.dataone.service.types.v2.LogEntry;
import org.junit.After;
import org.junit.Before;

import edu.ucsb.nceas.metacat.dataone.convert.LogV2toV1Converter;


public class LogV2toV1ConverterTest extends D1NodeServiceTest {
    /**
     * Set up the test fixtures
     * 
     * @throws Exception
     */
    @Before
    public void setUp() throws Exception {
     
    }

    /**
     * Remove the test fixtures
     */
    @After
    public void tearDown() {
    }
    
    /**
     * Build the test suite
     * @return
     */
    public static Test suite() {
      
      TestSuite suite = new TestSuite();
      suite.addTest(new LogV2toV1ConverterTest("initialize"));
      // MNStorage tests
      suite.addTest(new LogV2toV1ConverterTest("testConvert"));
      return suite;
      
    }
    
    /**
     * Constructor for the tests
     * 
     * @param name - the name of the test
     */
    public LogV2toV1ConverterTest(String name) {
      super(name);
      
    }

    /**
     * Initial blank test
     */
    public void initialize() {
      assertTrue(1 == 1);
      
    }
    
    
    /**
     * Junit test for the convert method
     */
    public void testConvert() throws Exception {
        Log v2Log = new Log();
        LogEntry v2LogEntry = new LogEntry();
        String entryId1 = "1";
        String event1 = "delete";
        Identifier identifier1 = new Identifier();
        identifier1.setValue("tao..345.34");
        String ip = "1.1.1.1";
        String userAgent = "morpho";
        Subject subject = new Subject();
        subject.setValue("uid=tao2,o=NCEAS,dc=ecoinformatics,dc=org");
        NodeReference node = new NodeReference();
        node.setValue("valley.duckdns.org");
        Date date1 = new Date();
        v2LogEntry.setEntryId(entryId1);
        v2LogEntry.setEvent(event1);
        v2LogEntry.setDateLogged(date1);
        v2LogEntry.setIdentifier(identifier1);
        v2LogEntry.setIpAddress(ip);
        v2LogEntry.setNodeIdentifier(node);
        v2LogEntry.setSubject(subject);
        v2LogEntry.setUserAgent(userAgent);
        v2Log.addLogEntry(v2LogEntry);
        
        LogEntry v2LogEntry2 = new LogEntry();
        Identifier identifier2 = new Identifier();
        identifier2.setValue("tao..345.35");
        Date date2 = new Date();
        String entryId2 = "2";
        String event2 = "create";
        v2LogEntry2.setEntryId(entryId2);
        v2LogEntry2.setEvent(event2);
        v2LogEntry2.setDateLogged(date2);
        v2LogEntry2.setIdentifier(identifier2);
        v2LogEntry2.setIpAddress(ip);
        v2LogEntry2.setNodeIdentifier(node);
        v2LogEntry2.setSubject(subject);
        v2LogEntry2.setUserAgent(userAgent);
        v2Log.addLogEntry(v2LogEntry2);
        
        int start = 3;
        int count = 2;
        int total = 500;
        v2Log.setCount(count);
        v2Log.setStart(start);
        v2Log.setTotal(total);
        
        LogV2toV1Converter convert = new LogV2toV1Converter();
        org.dataone.service.types.v1.Log v1Log = convert.convert(v2Log);
        
        assertTrue("The start number should be same for v1 and v2 Log", v1Log.getStart() == v2Log.getStart());
        assertTrue("The start number should be "+start+" for v1", v1Log.getStart() == start);
        
        assertTrue("The count number should be same for v1 and v2 Log", v1Log.getCount() == v2Log.getCount());
        assertTrue("The count number should be "+count+" for v1", v1Log.getCount() == count);
        
        assertTrue("The total number should be same for v1 and v2 Log", v1Log.getTotal() == v2Log.getTotal());
        assertTrue("The toal number should be "+total+" for v1", v1Log.getTotal() == total);
        
        org.dataone.service.types.v1.LogEntry v1LogEntry1 = v1Log.getLogEntry(0);
        assertTrue(v1LogEntry1.getDateLogged().equals(date1));
        assertTrue(v1LogEntry1.getEntryId().equals(entryId1));
        assertTrue(v1LogEntry1.getEvent().equals(Event.DELETE));
        assertTrue(v1LogEntry1.getIdentifier().equals(identifier1));
        assertTrue(v1LogEntry1.getIpAddress().equals(ip));
        assertTrue(v1LogEntry1.getNodeIdentifier().equals(node));
        assertTrue(v1LogEntry1.getSubject().equals(subject));
        assertTrue(v1LogEntry1.getUserAgent().equals(userAgent));
        
        org.dataone.service.types.v1.LogEntry v1LogEntry2 = v1Log.getLogEntry(1);
        assertTrue(v1LogEntry2.getDateLogged().equals(date2));
        assertTrue(v1LogEntry2.getEntryId().equals(entryId2));
        assertTrue(v1LogEntry2.getEvent().equals(Event.CREATE));
        assertTrue(v1LogEntry2.getIdentifier().equals(identifier2));
        assertTrue(v1LogEntry2.getIpAddress().equals(ip));
        assertTrue(v1LogEntry2.getNodeIdentifier().equals(node));
        assertTrue(v1LogEntry2.getSubject().equals(subject));
        assertTrue(v1LogEntry2.getUserAgent().equals(userAgent));
    }

}