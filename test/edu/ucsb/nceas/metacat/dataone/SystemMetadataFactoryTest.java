package edu.ucsb.nceas.metacat.dataone;

import junit.framework.Test;
import junit.framework.TestSuite;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.ObjectFormatIdentifier;
import org.dataone.service.types.v1.ReplicationPolicy;
import org.dataone.service.types.v1.Session;
import org.dataone.service.types.v2.SystemMetadata;

import edu.ucsb.nceas.MCTestCase;
import edu.ucsb.nceas.metacat.IdentifierManager;

/**
 * A class for testing the generation of SystemMetadata from defaults
 */
public class SystemMetadataFactoryTest extends MCTestCase {

    /**
     * constructor for the test
     */
    public SystemMetadataFactoryTest(String name) {
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
        suite.addTest(new SystemMetadataFactoryTest("initialize"));
        suite.addTest(new SystemMetadataFactoryTest("getDefaultReplicationPolicy"));
        suite.addTest(new SystemMetadataFactoryTest("testGetDocumentInfoMap"));
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
     * Test the getDefaultRepicationPolicy method
     * @throws Exception
     */
    public void getDefaultReplicationPolicy() throws Exception {
        ReplicationPolicy rp = SystemMetadataFactory.getDefaultReplicationPolicy();
        assertNotNull(rp);
        assertTrue(!rp.getReplicationAllowed());
        assertTrue(rp.getNumberReplicas() >= 0);
    }

    /**
     * Tests the getDocumentInfoMap method
     */
    public void testGetDocumentInfoMap() throws Exception {
        D1NodeServiceTest d1NodeTest = new D1NodeServiceTest("initialize");
        HttpServletRequest request = d1NodeTest.getServletRequest();
        //insert metadata
        Session session = d1NodeTest.getTestSession();
        Identifier guid = new Identifier();
        guid.setValue("testDocumentInfo." + System.currentTimeMillis());
        InputStream object =
                new FileInputStream(MNodeReplicationTest.replicationSourceFile);
        SystemMetadata sysmeta = D1NodeServiceTest
                                .createSystemMetadata(guid, session.getSubject(), object);
        object.close();
        ObjectFormatIdentifier formatId = new ObjectFormatIdentifier();
        formatId.setValue("eml://ecoinformatics.org/eml-2.0.1");
        sysmeta.setFormatId(formatId);
        object = new FileInputStream(MNodeReplicationTest.replicationSourceFile);
        MNodeService.getInstance(request).create(session, guid, object, sysmeta);
        // the docid
        String docid = IdentifierManager.getInstance().getLocalId(guid.getValue());
        Map<String, String> docInfo = SystemMetadataFactory.getDocumentInfoMap(docid);
        assertEquals(docInfo.get("doctype"), "eml://ecoinformatics.org/eml-2.0.1");
        assertEquals(docInfo.get("user_owner"), "cn=test,dc=dataone,dc=org");
        assertEquals(docInfo.get("docname"), "eml");
    }
}
