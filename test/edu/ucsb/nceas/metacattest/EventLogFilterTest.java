package edu.ucsb.nceas.metacattest;

import org.junit.Before;
import org.junit.Test;

import edu.ucsb.nceas.LeanTestUtils;
import edu.ucsb.nceas.metacat.EventLogData;
import edu.ucsb.nceas.metacat.EventLogFilter;
import edu.ucsb.nceas.metacat.properties.PropertyService;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Test the EventLogFilter class
 * @author tao
 *
 */
public class EventLogFilterTest {

    /**
     * Establish a testing framework by initializing appropriate objects
     */
    @Before
    public void setUp() throws Exception {
        LeanTestUtils.initializePropertyService(LeanTestUtils.PropertiesMode.UNIT_TEST);
    }



    /**
     * Test the filter method
     * @throws Exception
     */
    @Test
    public void testFilter() throws Exception {
        String[] principals = {"uid=user;o=NCEAS;dc=ecoinformatics;dc=org", "public",
                                    "http://orcid.org/0000-0002-1209-5268"};
        String[] ipList = {"192.168.1.103", "192.168.1.104", "192.168.106"};
        String[] docList = {"test.2.1", "test.2", "test.3"};
        String[] eventList = {"read", "insert", "update"};
        String userAgent = "morpho";
        String subject =
             "uid=user\\;o=NCEAS\\;dc=ecoinformatics\\;dc=org;http://orcid.org/0000-0002-1209-5268";
        //Tests without black lists.
        PropertyService.setPropertyNoPersist("event.log.deny.ipaddress", "");
        PropertyService.setPropertyNoPersist("event.log.deny.subject", "");
        EventLogFilter filter = new EventLogFilter();
        EventLogData data = new EventLogData(ipList[0], userAgent, principals[0], docList[0], eventList[0]);
        assertFalse(filter.filter(data));
        data = new EventLogData(ipList[1], userAgent, principals[1], docList[1], eventList[1]);
        assertFalse(filter.filter(data));
        data = new EventLogData(ipList[2], userAgent, principals[2], docList[2], eventList[2]);
        assertFalse(filter.filter(data));
        //Tests with ip blacklist
        PropertyService.setPropertyNoPersist("event.log.deny.ipaddress", "192.168.1.103;192.168.1.104");
        filter = new EventLogFilter();
        data = new EventLogData(ipList[0], userAgent, principals[0], docList[0], eventList[0]);
        assertTrue(filter.filter(data));
        data = new EventLogData(ipList[1], userAgent, principals[1], docList[1], eventList[1]);
        assertTrue(filter.filter(data));
        data = new EventLogData(ipList[2], userAgent, principals[2], docList[2], eventList[2]);
        assertFalse(filter.filter(data));
        //Tests with subject blacklist 
        PropertyService.setPropertyNoPersist("event.log.deny.ipaddress", "");
        PropertyService.setPropertyNoPersist("event.log.deny.subject", subject);
        filter = new EventLogFilter();
        data = new EventLogData(ipList[0], userAgent, principals[0], docList[0], eventList[0]);
        assertTrue(filter.filter(data));
        data = new EventLogData(ipList[1], userAgent, principals[1], docList[1], eventList[1]);
        assertFalse(filter.filter(data));
        data = new EventLogData(ipList[2], userAgent, principals[2], docList[2], eventList[2]);
        assertTrue(filter.filter(data));
        //Tests both blacklists 
        PropertyService.setPropertyNoPersist("event.log.deny.ipaddress", "192.168.1.103;192.168.1.104");
        PropertyService.setPropertyNoPersist("event.log.deny.subject", subject);
        filter = new EventLogFilter();
        data = new EventLogData(ipList[0], userAgent, principals[0], docList[0], eventList[0]);
        assertTrue(filter.filter(data));
        data = new EventLogData(ipList[1], userAgent, principals[1], docList[1], eventList[1]);
        assertTrue(filter.filter(data));
        data = new EventLogData(ipList[2], userAgent, principals[2], docList[2], eventList[2]);
        assertTrue(filter.filter(data));
        data = new EventLogData(ipList[2], userAgent, principals[1], docList[2], eventList[2]);
        assertFalse(filter.filter(data));
        data = null;
        assertTrue(filter.filter(data));
    }

}
