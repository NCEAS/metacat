package edu.ucsb.nceas.metacat.systemmetadata;


import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.Date;

import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Session;
import org.dataone.service.types.v2.SystemMetadata;

import edu.ucsb.nceas.metacat.IdentifierManager;
import edu.ucsb.nceas.metacat.dataone.D1NodeServiceTest;
import junit.framework.Test;
import junit.framework.TestSuite;

public class SystemMetadataValidatorTest extends D1NodeServiceTest {
    
    /**
     * Constructor
     * @param name  the name of test
     */
    public SystemMetadataValidatorTest(String name) {
        super(name);
    }
    
    /**
     * Create a suite of tests to be run together
     */
    public static Test suite() {
        TestSuite suite = new TestSuite();
        suite.addTest(new SystemMetadataValidatorTest("testHasLatestVersion"));
        return suite;
    }
    
    /**
     * Test the hasLatestVersion method
     * @throws Exception
     */
    public void testHasLatestVersion() throws Exception {
        Date oldTime = new Date();
        Thread.sleep(1000);
        String docid = "testHasLatestVersion." + new Date().getTime() + ".1";
        String guid = "guid:" + docid;
        //create a mapping (identifier-docid)
        IdentifierManager im = IdentifierManager.getInstance();
        im.createMapping(guid, docid);
        Session session = getTestSession();
        Identifier id = new Identifier();
        id.setValue(guid);
        InputStream object = new ByteArrayInputStream("test".getBytes("UTF-8"));
        SystemMetadata sysmeta = createSystemMetadata(id, session.getSubject(), object);
        Date originalDate = sysmeta.getDateSysMetadataModified();
        BigInteger serialVersion = new BigInteger("4");
        sysmeta.setSerialVersion(serialVersion);
        SystemMetadataValidator validator = new SystemMetadataValidator();
        boolean hasLatestVersion = validator.hasLatestVersion(sysmeta);
        assertTrue(hasLatestVersion == true);
        SystemMetadataManager.getInstance().store(sysmeta);
        
        //The originalDate was set by the client. However, base on the definition,
        //the modification should be set by MN. So there is slight difference
        sysmeta.setDateSysMetadataModified(originalDate);
        assertTrue(hasLatestVersion == true);
        try {
            hasLatestVersion = validator.hasLatestVersion(sysmeta);
            fail(
                "we shouldn't get there since the system metadata shouldn't have the same "
                    + "modification as the one set by client");
        } catch (InvalidSystemMetadata e) {
            
        }
        
        SystemMetadata readSysmeta = SystemMetadataManager.getInstance().get(id);
        
        //the system metadata read from store should be fine
        hasLatestVersion = validator.hasLatestVersion(readSysmeta);
        assertTrue(hasLatestVersion == true);
        
        //serial version 5 should be fine
        BigInteger serialVersion2 = new BigInteger("5");
        readSysmeta.setSerialVersion(serialVersion2);
        hasLatestVersion = validator.hasLatestVersion(readSysmeta);
        assertTrue(hasLatestVersion == true);
        
        //test a new serial version which is less than current one - 4
        BigInteger serialVersion1 = new BigInteger("3");
        readSysmeta.setSerialVersion(serialVersion1);
        try {
            hasLatestVersion = validator.hasLatestVersion(readSysmeta);
            fail("we shouldn't get there since the serial version is less than 4");
        } catch (InvalidSystemMetadata e) {
            
        }
        
        //serial version 4 should be fine
        readSysmeta.setSerialVersion(serialVersion);
        hasLatestVersion = validator.hasLatestVersion(readSysmeta);
        assertTrue(hasLatestVersion == true);
        
        //setting an earlier time to the modification date will fail
        readSysmeta.setDateSysMetadataModified(oldTime);
        try {
            hasLatestVersion = validator.hasLatestVersion(readSysmeta);
            fail(
                "we shouldn't get here since the earlier time doesn't match the modification date");
        } catch (InvalidSystemMetadata e) {
            
        }
        //setting a later time to the modification date will fail
        Date newTime = new Date();
        readSysmeta.setDateSysMetadataModified(newTime);
        try {
            hasLatestVersion = validator.hasLatestVersion(readSysmeta);
            fail("we shouldn't get there since the later time doesn't match the modification date");
        } catch (InvalidSystemMetadata e) {
            
        }

        //remove the system metadata
        im.deleteSystemMetadata(guid);
        //remove the mapping
        im.removeMapping(guid, docid);
    }

}
