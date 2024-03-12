package edu.ucsb.nceas.metacattest;

import java.io.InputStream;
import java.math.BigInteger;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.apache.commons.io.IOUtils;
import org.dataone.service.exceptions.InvalidRequest;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.ObjectFormatIdentifier;
import org.dataone.service.types.v1.Session;
import org.dataone.service.types.v2.SystemMetadata;

import edu.ucsb.nceas.metacat.dataone.D1NodeServiceTest;
import edu.ucsb.nceas.metacat.dataone.MNodeService;

/**
 * A JUnit test for testing Dryad documents
 */
public class DryadTest
    extends D1NodeServiceTest {
    
    private static final String DRYAD_TEST_DOC = "test/dryad-metadata-profile-sample.xml";
    
    private static final String DRYAD_TEST_DOC_INVALID = "test/dryad-metadata-profile-invalid.xml";

    /**
     * Constructor to build the test
     *
     * @param name the name of the test method
     */
    public DryadTest(String name) {
        super(name);
    }

    /**
     * Create a suite of tests to be run together
     */
    public static Test suite() {
        TestSuite suite = new TestSuite();
        suite.addTest(new DryadTest("initialize"));
        // Test basic functions
        suite.addTest(new DryadTest("d1InsertDoc"));
        suite.addTest(new DryadTest("d1InsertInvalidDoc"));

        return suite;
    }

    /**
     * Run an initial test that always passes to check that the test
     * harness is working.
     */
    public void initialize() {
        assertTrue(1 == 1);
    }

    /**
     * Insert test doc using D1 API
     */
    public void d1InsertDoc() {
        try {
            String docid = this.generateDocumentId();
            String documentContents = this.getTestDocFromFile(DRYAD_TEST_DOC);
            Session session = getTestSession();
            Identifier pid = new Identifier();
            pid.setValue(docid);
            SystemMetadata sysmeta = this.createSystemMetadata(pid,
                       session.getSubject(), IOUtils.toInputStream(documentContents, "UTF-8"));
            ObjectFormatIdentifier formatId = new ObjectFormatIdentifier();
            formatId.setValue("http://datadryad.org/profile/v3.1");
            sysmeta.setFormatId(formatId);
            sysmeta.setSize(BigInteger.valueOf(documentContents.getBytes().length));
            MNodeService.getInstance(request).create(session, pid,
                                    IOUtils.toInputStream(documentContents, "UTF-8"), sysmeta);
            InputStream results = MNodeService.getInstance(request).get(session, pid);
            String resultString = IOUtils.toString(results, "UTF-8");
            assertEquals(documentContents, resultString);
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    /**
     * Insert test doc using D1 API
     */
    public void d1InsertInvalidDoc() {
        try {
            String docid = this.generateDocumentId();
            String documentContents = this.getTestDocFromFile(DRYAD_TEST_DOC_INVALID);
            Session session = getTestSession();
            Identifier pid = new Identifier();
            pid.setValue(docid);
            SystemMetadata sysmeta = this.createSystemMetadata(pid, session.getSubject(),
                                                IOUtils.toInputStream(documentContents, "UTF-8"));
            ObjectFormatIdentifier formatId = new ObjectFormatIdentifier();
            formatId.setValue("http://datadryad.org/profile/v3.1");
            sysmeta.setFormatId(formatId);
            sysmeta.setSize(BigInteger.valueOf(documentContents.getBytes().length));
            try {
                MNodeService.getInstance(request).create(session, pid,
                                        IOUtils.toInputStream(documentContents, "UTF-8"), sysmeta);
            } catch (Exception expectedException) {
                assertTrue(expectedException instanceof InvalidRequest);
                return;
            }
            fail("Should not allow inserting invalid Dryad content");
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }

    }

}
