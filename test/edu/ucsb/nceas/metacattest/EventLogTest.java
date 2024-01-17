package edu.ucsb.nceas.metacattest;

import org.junit.Before;
import org.junit.Test;

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import edu.ucsb.nceas.LeanTestUtils;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.metacat.EventLog;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Test the logging facility against the database connection.
 *
 * @author jones
 */
public class EventLogTest {
    private static final String USERAGENT="useragent-useragent-useragent-useragent-"
                    + "useragent-useragent-useragent-useragent-useragent-useragent-"
                    + "useragent-useragent-useragent-useragent-useragent-useragent-"
                    + "useragent-useragent-useragent-useragent-"
                    + "useragent-useragent-useragent-useragent-useragent-useragent-useragent-"
                    + "useragent-useragent-useragent-"
                    + "useragent-useragent-useragent-useragent-useragent-useragent-useragent-"
                    + "useragent-useragent-useragent-useragent-useragent-useragent-useragent-"
                    + "useragent-useragent-useragent-useragent-useragent-useragent-useragent-12";

    /**
     * Establish the property service
     */
    @Before
    public void setUp() throws Exception {
        LeanTestUtils.initializePropertyService(LeanTestUtils.PropertiesMode.LIVE_TEST);
    }

    /**
     * Test whether a valid instance of the EventLog can be retrieved.
     *
     */
    @Test
    public void testGetInstance() {
        EventLog logger = EventLog.getInstance();
        assertNotNull(logger);
    }

    /**
     * Test whether the log method can properly insert a log record.
     */
    @Test
    public void testLog() throws Exception {
        long time = System.nanoTime();
        String id = "test-1934-wemewen-3-2"+time+".1";
        EventLog.getInstance().log("192.168.1.103", "Mozilla", "public", id, "read");
        Thread.sleep(2000);
        Timestamp startDate = null;
        Timestamp endDate = null;
        boolean anonymous = false;
        String[] principals = {"public", "someone"};
        String[] ipList = {"192.168.1.103", "192.168.1.104"};
        String[] docList = {id};
        String[] eventList = {"read", "insert", "update"};
        String report = EventLog.getInstance().getReport(ipList, principals, docList,
                eventList, startDate, endDate, anonymous);
        assertTrue(report.contains("<event>read</event>"));
        assertTrue(report.contains("<ipAddress>192.168.1.103</ipAddress>"));
        assertTrue(report.contains("<userAgent>Mozilla</userAgent>"));
        assertTrue(report.contains("<principal>public</principal>"));
        assertTrue(report.contains("<docid>" + id + "</docid>"));

        //test a case with a user-agent which length is greater than 512.
        time = System.nanoTime();
        id = "test-1934-wemewen-3-3"+time+".1";
        EventLog.getInstance().
                    log("192.168.1.103", USERAGENT + "extral characters", "public", id, "read");
        Thread.sleep(2000);

        String[] docs = {id};
        report = EventLog.getInstance().getReport(ipList, principals, docs,
                eventList, startDate, endDate, anonymous);
        assertTrue(report.contains("<event>read</event>"));
        assertTrue(report.contains("<ipAddress>192.168.1.103</ipAddress>"));
        assertTrue(report.contains("<userAgent>" + USERAGENT + "</userAgent>"));
        assertTrue(report.contains("<principal>public</principal>"));
        assertTrue(report.contains("<docid>" + id + "</docid>"));
    }

    /**
     * Test whether getReport returns correct reports.
     */
    @Test
    public void testGetReport() {
        String[] principals = {"jones", "public", "someone"};
        String[] ipList = {"192.168.1.103", "192.168.1.104"};
        String[] docList = {"test.2.1", "test.2"};
        String[] eventList = {"read", "insert", "update"};
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Timestamp startDate = null;
        Timestamp endDate = null;
        try {
            startDate = new Timestamp((format.parse("2004-04-08 02:32:00")).getTime());
            endDate = new Timestamp((format.parse("2004-04-08 11:20:00")).getTime());
        } catch (ParseException e) {
            System.out.println("Failed to created endDate from format.");
        }
        String report = EventLog.getInstance().getReport(ipList, principals, docList, 
                        eventList, null, null, false);
        assertTrue(report.contains("<log>"));
        report = EventLog.getInstance().getReport(null, null, null, 
                        null, startDate, endDate, false);
        assertTrue(report.contains("<log>"));
    }

    /**
     * Test if the isDeleted method
     */
    @Test
    public void testIsDeleted() throws Exception{
        long time = System.nanoTime();
        String id = "test-1934-weme123-3-1"+time+".1";
        EventLog.getInstance().log("192.168.1.103", "Mozilla", "public", id, "read");
        Thread.sleep(2000);
        boolean deleted = EventLog.getInstance().isDeleted(id);
        assertFalse(deleted);
        Thread.sleep(2000);
        EventLog.getInstance().log("192.168.1.103", "Mozilla", "public", id, EventLog.DELETE);
        deleted = EventLog.getInstance().isDeleted(id);
        assertTrue(deleted == true);
    }

    /**
     * Test logging when the feature is disabled
     * @throws Exception
     */
    @Test
    public void testDisableEventLog() throws Exception {
        PropertyService.setPropertyNoPersist("event.log.enabled", "false");
        EventLog.getInstance().refreshLogProperties();
        long time = System.nanoTime();
        String id = "test-1934-wemewen-3-2"+time+".1";
        EventLog.getInstance().log("192.168.1.103", "Mozilla", "public", id, "read");
        Thread.sleep(2000);
        Timestamp startDate = null;
        Timestamp endDate = null;
        boolean anonymous = false;
        String[] principals = {"public", "someone"};
        String[] ipList = {"192.168.1.103", "192.168.1.104"};
        String[] docList = {id};
        String[] eventList = {"read", "insert", "update"};
        String report = EventLog.getInstance().getReport(ipList, principals, docList,
                eventList, startDate, endDate, anonymous);
        assertTrue(report.contains("<log>"));
        assertFalse(report.contains("<event>read</event>"));
        assertFalse(report.contains("<ipAddress>192.168.1.103</ipAddress>"));
        assertFalse(report.contains("<userAgent>Mozilla</userAgent>"));
        assertFalse(report.contains("<principal>public</principal>"));
        assertFalse(report.contains("<docid>" + id + "</docid>"));
        PropertyService.setPropertyNoPersist("event.log.enabled", "true");
        EventLog.getInstance().refreshLogProperties();
    }
}
