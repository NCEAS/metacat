package edu.ucsb.nceas.metacattest;

import java.io.InputStream;
import java.math.BigInteger;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.IOUtils;
import org.dataone.service.exceptions.InvalidRequest;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.ObjectFormatIdentifier;
import org.dataone.service.types.v1.Session;
import org.dataone.service.types.v2.SystemMetadata;
import org.junit.Before;
import org.junit.Test;

import edu.ucsb.nceas.LeanTestUtils;
import edu.ucsb.nceas.MCTestCase;
import edu.ucsb.nceas.metacat.dataone.D1NodeServiceTest;
import edu.ucsb.nceas.metacat.dataone.MNodeService;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * A JUnit test for testing Dryad documents
 */
public class DryadTest {
    private static final String DRYAD_TEST_DOC = "test/dryad-metadata-profile-sample.xml";
    private static final String DRYAD_TEST_DOC_INVALID = "test/dryad-metadata-profile-invalid.xml";
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
     * Insert test doc using D1 API
     */
    @Test
    public void d1InsertDoc() {
        try {
            String docid = MCTestCase.generateDocumentId();
            String documentContents = MCTestCase.getTestDocFromFile(DRYAD_TEST_DOC);
            Session session = d1NodeTest.getTestSession();
            Identifier pid = new Identifier();
            pid.setValue(docid);
            SystemMetadata sysmeta = D1NodeServiceTest.createSystemMetadata(pid,
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
            fail(e.getMessage());
        }
    }

    /**
     * Insert test doc using D1 API
     */
    @Test
    public void d1InsertInvalidDoc() {
        try {
            String docid = MCTestCase.generateDocumentId();
            String documentContents = MCTestCase.getTestDocFromFile(DRYAD_TEST_DOC_INVALID);
            Session session = d1NodeTest.getTestSession();
            Identifier pid = new Identifier();
            pid.setValue(docid);
            SystemMetadata sysmeta = D1NodeServiceTest.createSystemMetadata(pid, session.getSubject(),
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
            fail(e.getMessage());
        }

    }

}
