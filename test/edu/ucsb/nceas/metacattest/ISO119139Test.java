package edu.ucsb.nceas.metacattest;

import edu.ucsb.nceas.metacat.dataone.D1NodeServiceTest;
import edu.ucsb.nceas.metacat.dataone.MNodeService;
import java.io.FileInputStream;
import java.io.InputStream;

import javax.servlet.http.HttpServletRequest;

import java.io.File;

import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.ObjectFormatIdentifier;
import org.dataone.service.types.v1.Session;
import org.dataone.service.types.v2.SystemMetadata;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Test to insert an object of ISO119139
 */
public class ISO119139Test {
    private HttpServletRequest request;
    D1NodeServiceTest d1NodeServiceTest;

    /**
     * Initialize the connection to metacat, and insert a document to be 
     * used for testing with a known docid.
     */
    @Before
    public void setUp() throws Exception{
        d1NodeServiceTest = new D1NodeServiceTest("initialize");
        request = d1NodeServiceTest.getServletRequest();
    }

    /** 
     * Insert a test document, returning the docid that was used. 
     */
    @Test
    public void TestInsertDocument() throws Exception {
        String path = "./test/isoTestNodc1.xml";
        Session session = d1NodeServiceTest.getTestSession();
        String metadataIdStr = D1NodeServiceTest.generateDocumentId() + ".1";
        Identifier metadataId = new Identifier();
        metadataId.setValue(metadataIdStr);
        InputStream metadataObject = new FileInputStream(new File(path));
        SystemMetadata sysmeta = D1NodeServiceTest
                            .createSystemMetadata(metadataId, session.getSubject(), metadataObject);
        metadataObject.close();
        ObjectFormatIdentifier formatId = new ObjectFormatIdentifier();
        formatId.setValue("http://www.isotc211.org/2005/gmd");
        sysmeta.setFormatId(formatId);
        metadataObject = new FileInputStream(new File(path));
        d1NodeServiceTest.mnCreate(session, metadataId, metadataObject, sysmeta);
        SystemMetadata readSysmeta = MNodeService.getInstance(request)
                                        .getSystemMetadata(session, metadataId);
        assertEquals(metadataIdStr, readSysmeta.getIdentifier().getValue());
        metadataObject.close();
    }

}
