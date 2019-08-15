/**
 *  '$RCSfile$'
 *  Copyright: 2018 Regents of the University of California and the
 *             National Center for Ecological Analysis and Synthesis
 *
 *   '$Author$'
 *     '$Date$'
 * '$Revision$'
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package edu.ucsb.nceas.metacattest;

import edu.ucsb.nceas.MCTestCase;
import edu.ucsb.nceas.metacat.EventLog;
import edu.ucsb.nceas.metacat.EventLogData;
import edu.ucsb.nceas.metacat.EventLogFilter;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Test the EventLogFilter class
 * @author tao
 *
 */
public class EventLogFilterTest extends MCTestCase {
    
    /**
     * Constructor
     * @param name
     */
    public EventLogFilterTest(String name) {
        super(name);
    }
    
    /**
     * Establish a testing framework by initializing appropriate objects
     */
    public void setUp() throws Exception {
       
    }

    /**
     * Release any objects after tests are complete
     */
    public void tearDown() {
    }

    /**
     * Create a suite of tests to be run together
     */
    public static Test suite() {
        TestSuite suite = new TestSuite();
        suite.addTest(new EventLogFilterTest("initialize"));
        // Test basic functions
        suite.addTest(new EventLogFilterTest("testFilter"));
        return suite;
    }
    
    /**
     * Run an initial test that always passes to check that the test harness is
     * working.
     */
    public void initialize() {
        assertTrue(1 == 1);
    }
    
    /**
     * Test the filter method
     * @throws Exception
     */
    public void testFilter() throws Exception {
        String[] principals = {"uid=user,o=NCEAS,dc=ecoinformatics,dc=org", "public", "http://orcid.org/0000-0002-1209-5268"};
        String[] ipList = {"192.168.1.103", "192.168.1.104", "192.168.106"};
        String[] docList = {"test.2.1", "test.2", "test.3"};
        String[] eventList = {"read", "insert", "update"};
        String userAgent = "morpho";
        //Tests without black lists.
        PropertyService.setPropertyNoPersist("event.log.blacklist.ipaddress", "");
        PropertyService.setPropertyNoPersist("event.log.blacklist.subject", "");
        EventLogFilter filter = new EventLogFilter();
        EventLogData data = new EventLogData(ipList[0], userAgent, principals[0], docList[0], eventList[0]);
        assertTrue(!filter.filter(data));
        data = new EventLogData(ipList[1], userAgent, principals[1], docList[1], eventList[1]);
        assertTrue(!filter.filter(data));
        data = new EventLogData(ipList[2], userAgent, principals[2], docList[2], eventList[2]);
        assertTrue(!filter.filter(data));
        //Tests with ip blacklist
        PropertyService.setPropertyNoPersist("event.log.blacklist.ipaddress", "192.168.1.103:192.168.1.104");
        filter = new EventLogFilter();
        data = new EventLogData(ipList[0], userAgent, principals[0], docList[0], eventList[0]);
        assertTrue(filter.filter(data));
        data = new EventLogData(ipList[1], userAgent, principals[1], docList[1], eventList[1]);
        assertTrue(filter.filter(data));
        data = new EventLogData(ipList[2], userAgent, principals[2], docList[2], eventList[2]);
        assertTrue(!filter.filter(data));
        //Tests with subject blacklist 
        PropertyService.setPropertyNoPersist("event.log.blacklist.ipaddress", "");
        PropertyService.setPropertyNoPersist("event.log.blacklist.subject", "uid=user,o=NCEAS,dc=ecoinformatics,dc=org:http\\://orcid.org/0000-0002-1209-5268");
        filter = new EventLogFilter();
        data = new EventLogData(ipList[0], userAgent, principals[0], docList[0], eventList[0]);
        assertTrue(filter.filter(data));
        data = new EventLogData(ipList[1], userAgent, principals[1], docList[1], eventList[1]);
        assertTrue(!filter.filter(data));
        data = new EventLogData(ipList[2], userAgent, principals[2], docList[2], eventList[2]);
        assertTrue(filter.filter(data));
        //Tests both blacklists 
        PropertyService.setPropertyNoPersist("event.log.blacklist.ipaddress", "192.168.1.103:192.168.1.104");
        PropertyService.setPropertyNoPersist("event.log.blacklist.subject", "uid=user,o=NCEAS,dc=ecoinformatics,dc=org:http\\://orcid.org/0000-0002-1209-5268");
        filter = new EventLogFilter();
        data = new EventLogData(ipList[0], userAgent, principals[0], docList[0], eventList[0]);
        assertTrue(filter.filter(data));
        data = new EventLogData(ipList[1], userAgent, principals[1], docList[1], eventList[1]);
        assertTrue(filter.filter(data));
        data = new EventLogData(ipList[2], userAgent, principals[2], docList[2], eventList[2]);
        assertTrue(filter.filter(data));
        data = new EventLogData(ipList[2], userAgent, principals[1], docList[2], eventList[2]);
        assertTrue(!filter.filter(data));
        data = null;
        assertTrue(filter.filter(data));
    }

}
