package edu.ucsb.nceas.metacat.dataone.v1;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import javax.servlet.http.HttpServletRequest;

import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.ObjectFormatIdentifier;
import org.dataone.service.types.v1.Session;
import org.dataone.service.types.v1.ObjectFormat;
import org.dataone.service.types.v1.SystemMetadata;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import static org.junit.Assert.fail;

import edu.ucsb.nceas.LeanTestUtils;
import edu.ucsb.nceas.metacat.dataone.D1NodeServiceTest;


/**
 * A JUnit test for testing the dataone CNCore implementation
 */
public class CNodeV1ServiceTest {

    private D1NodeServiceTest d1NodeTest;
    private HttpServletRequest request;

    /**
     * Establish a testing framework by initializing appropriate objects
     * @throws Exception
     */
    @Before
    public void setUp() throws Exception {
        LeanTestUtils.initializePropertyService(LeanTestUtils.PropertiesMode.UNIT_TEST);
        d1NodeTest = new D1NodeServiceTest("initialize");
        request = d1NodeTest.getServletRequest();
    }

    /**
     * test for getting system metadata
     */
    @Test
    public void testGetSystemMetadata() {
        D1NodeServiceTest.printTestHeader("testGetSystemMetadata");

        try {
            Session session = d1NodeTest.getTestSession();
            Identifier guid = new Identifier();
            guid.setValue("testGetSystemMetadata." + System.currentTimeMillis());
            InputStream object = new ByteArrayInputStream("test".getBytes("UTF-8"));
            SystemMetadata sysmeta = d1NodeTest.createV1SystemMetadata(guid, session.getSubject(), object);
            Session cnSession = D1NodeServiceTest.getCNSession();
            D1NodeServiceTest.storeData(object, sysmeta);
            Identifier retGuid = CNodeService.getInstance(request).create(cnSession, guid, object, sysmeta);
            assertEquals(guid.getValue(), retGuid.getValue());
            // get it
            SystemMetadata retSysmeta = CNodeService.getInstance(request).getSystemMetadata(session, guid);
            // check it
            assertEquals(sysmeta.getIdentifier().getValue(), retSysmeta.getIdentifier().getValue());
            assertTrue(retSysmeta instanceof org.dataone.service.types.v1.SystemMetadata);
            assertFalse(retSysmeta instanceof org.dataone.service.types.v2.SystemMetadata);
            System.out.println("=== the class name is "+retSysmeta.getClass().getName());
        } catch(Exception e) {
            fail("Unexpected error: " + e.getMessage());
        }
    }

    /**
     * Test the getFormat method
     * @throws Exception
     */
    @Test
    public void testGetFormat() throws Exception {
        // make sure we are set up
        d1NodeTest.setUpFormats();
        String knownFormat = "text/plain";
        ObjectFormatIdentifier fmtid = new ObjectFormatIdentifier();
        fmtid.setValue(knownFormat);
        try {
            ObjectFormat format = CNodeService.getInstance(request).getFormat(fmtid);
            assertTrue( format instanceof org.dataone.service.types.v1.ObjectFormat);
            assertFalse( format instanceof org.dataone.service.types.v2.ObjectFormat);
            System.out.println("=== the class name is "+format.getClass().getName());
            String result = format.getFormatId().getValue();
            System.out.println("Expected result: " + knownFormat);
            System.out.println("Found    result: " + result);
            assertTrue(result.equals(knownFormat));
        } catch (Exception npe) {
            fail("Can't get the returned format : " + npe.getMessage());
        }
    }

}
