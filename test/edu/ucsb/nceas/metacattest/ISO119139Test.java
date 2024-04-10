package edu.ucsb.nceas.metacattest;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.File;

import junit.framework.Test;
import junit.framework.TestSuite;

import edu.ucsb.nceas.metacat.dataone.D1NodeServiceTest;
import edu.ucsb.nceas.metacat.dataone.MNodeService;

import org.apache.commons.io.FileUtils;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.ObjectFormatIdentifier;
import org.dataone.service.types.v1.Session;
import org.dataone.service.types.v2.SystemMetadata;

public class ISO119139Test  extends D1NodeServiceTest {
   
    public ISO119139Test(String name) {
        super(name);
    }
    
    /**
     * Create a suite of tests to be run together
     */
    public static Test suite() {
        TestSuite suite = new TestSuite();
        // Test basic functions
        suite.addTest(new ISO119139Test("TestInsertDocument"));
        return suite;
    }
    /**
     * Initialize the connection to metacat, and insert a document to be 
     * used for testing with a known docid.
     */
    public void setUp() throws Exception{
        metacatConnectionNeeded = true;
        super.setUp();
        

    }
    
  

    /** 
     * Insert a test document, returning the docid that was used. 
     */
    public void TestInsertDocument() throws Exception {
        String path = "./test/isoTestNodc1.xml";
        Session session = getTestSession();
        String metadataIdStr = generateDocumentId() + ".1";
        Identifier metadataId = new Identifier();
        metadataId.setValue(metadataIdStr);
        InputStream metadataObject = new FileInputStream(new File(path));
        SystemMetadata sysmeta = 
                        createSystemMetadata(metadataId, session.getSubject(), metadataObject);
        metadataObject.close();
        ObjectFormatIdentifier formatId = new ObjectFormatIdentifier();
        formatId.setValue("http://www.isotc211.org/2005/gmd");
        sysmeta.setFormatId(formatId);
        metadataObject = new FileInputStream(new File(path));
        MNodeService.getInstance(request).create(session, metadataId, metadataObject, sysmeta);
        SystemMetadata readSysmeta = MNodeService.getInstance(request)
                                        .getSystemMetadata(session, metadataId);
        assertTrue(readSysmeta.getIdentifier().getValue().equals(metadataIdStr));
        metadataObject.close();
    }
    
   
    
}
