package edu.ucsb.nceas.metacat.annotation;

import edu.ucsb.nceas.metacat.dataone.D1NodeServiceTest;
import edu.ucsb.nceas.metacat.dataone.MNodeService;
import org.apache.commons.io.IOUtils;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.ObjectFormatIdentifier;
import org.dataone.service.types.v1.Session;
import org.dataone.service.types.v2.SystemMetadata;
import org.junit.Before;
import org.junit.Test;

import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class OrcidServiceTest {

    private D1NodeServiceTest d1NSTest;

    /**
     * constructor for the test
     */
    public OrcidServiceTest() {
    }

    /**
     * Establish a testing framework by initializing appropriate objects
     */
    @Before
    public void setUp() throws Exception {
        // D1NodeServiceTest extends MCTestCase, so it automatically calls
        // LeanTestUtils.initializePropertyService(LeanTestUtils.PropertiesMode.LIVE_TEST);
        d1NSTest = new D1NodeServiceTest("OrcidServiceTest");
    }

    @Test
    public void testLookup() {
        List<String> otherNames = List.of("Matthew Bentley Jones");
        String orcid = OrcidService.lookupOrcid(null, null, null, otherNames);
        assertEquals("http://orcid.org/0000-0003-0077-4738", orcid);
    }

    @Test
    public void findMatches() throws Exception {

        // insert an object in case there are no objects on the server
        String path = "test/eml-datacite.xml";
        Session session = d1NSTest.getTestSession();
        Identifier guid = new Identifier();
        guid.setValue("findMatches." + System.currentTimeMillis());
        InputStream object = new FileInputStream(path);
        SystemMetadata sysmeta =
            D1NodeServiceTest.createSystemMetadata(guid, session.getSubject(), object);
        ObjectFormatIdentifier formatId = new ObjectFormatIdentifier();
        formatId.setValue("eml://ecoinformatics.org/eml-2.0.1");
        sysmeta.setFormatId(formatId);
        object = new FileInputStream(path);
        Identifier pid = MNodeService.getInstance(d1NSTest.getServletRequest())
            .create(session, guid, object, sysmeta);
        assertNotNull("pid was null!", pid);
        object.close();
        String query = "q=id:"+guid.getValue();
        String resultStr;
        InputStream stream;
        int times = 0;
        int maxTries = 40;
        do {
            System.out.println(times);
            Thread.sleep(500);
            stream = MNodeService.getInstance(d1NSTest.getServletRequest()).query(session, "solr", query);
            resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        } while ( (resultStr == null || !resultStr.contains("checksum")) && ++times <= maxTries);
        assertNotNull(resultStr);
        assertTrue("query (" + query + ") result didn't contain \"checksum\"",
                   resultStr.contains("checksum"));

        int count = 0;
        Map<String, String> matches = new HashMap<>();

        List<String> creators = OrcidService.lookupCreators(true);
        for (String creator: creators) {
            String orcid = OrcidService.lookupOrcid(null, null, null, List.of(creator));
            if (orcid != null) {
                matches.put(orcid, creator);
                count++;
            }
         }
        assertTrue(count > 0);
        for (Entry<String, String> entry : matches.entrySet()) {
            System.out.println("Found ORCID: " + entry.getKey() + " for creator: " + entry.getValue());
        }
    }

}
