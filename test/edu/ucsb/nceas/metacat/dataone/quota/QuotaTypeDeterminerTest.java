package edu.ucsb.nceas.metacat.dataone.quota;


import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;

import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.ObjectFormatIdentifier;
import org.dataone.service.types.v1.Session;
import org.dataone.service.types.v2.SystemMetadata;

import edu.ucsb.nceas.metacat.dataone.D1NodeServiceTest;
import edu.ucsb.nceas.metacat.dataone.MNodeReplicationTest;
import edu.ucsb.nceas.metacat.dataone.MNodeService;
import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Junit test to test the class QuotaTypeDeterminer
 * @author tao
 *
 */
public class QuotaTypeDeterminerTest extends D1NodeServiceTest {
    private static String portalFilePath = "test/example-portal.xml";
    
    /**
     * Constructor
     * @param name  name of method will be tested
     */
    public QuotaTypeDeterminerTest(String name) {
        super(name);
    }
    
    /**
     * Create a suite of tests to be run together
     */
    public static Test suite() {
        TestSuite suite = new TestSuite();
        suite.addTest(new QuotaTypeDeterminerTest("testDetermine"));
        return suite;
    }
    
    /**
     * Test the determine method
     * @throws Exception
     */
    public void testDetermine() throws Exception {
        List<String> portalNameSpaces = QuotaServiceManager.retrievePortalNameSpaces();
        System.out.println("===============the list of portal name spaces " + portalNameSpaces);
        QuotaTypeDeterminer determiner = new QuotaTypeDeterminer(portalNameSpaces);
        
        //a portal object
        String uuid_prefix = "urn:uuid:";
        UUID uuid = UUID.randomUUID();
        Session session = getTestSession();
        Identifier guid = new Identifier();
        guid.setValue(uuid_prefix + uuid.toString());
        InputStream object = new FileInputStream(portalFilePath);
        Identifier seriesId = new Identifier();
        uuid = UUID.randomUUID();
        seriesId.setValue(uuid_prefix + uuid.toString());
        SystemMetadata sysmeta = createSystemMetadata(guid, session.getSubject(), object);
        object.close();
        sysmeta.setSeriesId(seriesId);
        ObjectFormatIdentifier formatId4 = new ObjectFormatIdentifier();
        formatId4.setValue("https://purl.dataone.org/portals-1.0.0");
        sysmeta.setFormatId(formatId4);
        determiner.determine(sysmeta);
        assertTrue(determiner.getQuotaType().equals(QuotaTypeDeterminer.PORTAL));
        assertTrue(determiner.getInstanceId().equals(seriesId.getValue()));
        assertTrue(!determiner.getInstanceId().equals(guid.getValue()));
        
        //insert a metadata object
        Identifier guid2 = new Identifier();
        uuid = UUID.randomUUID();
        guid2.setValue(uuid_prefix + uuid.toString());
        Identifier seriesId2 = new Identifier();
        uuid = UUID.randomUUID();
        seriesId2.setValue(uuid_prefix + uuid.toString());
        InputStream object2 = new FileInputStream(new File(MNodeReplicationTest.replicationSourceFile));
        SystemMetadata sysmeta2 = createSystemMetadata(guid2, session.getSubject(), object2);
        object2.close();
        ObjectFormatIdentifier formatId = new ObjectFormatIdentifier();
        formatId.setValue("eml://ecoinformatics.org/eml-2.0.1");
        sysmeta2.setFormatId(formatId);
        sysmeta2.setSeriesId(seriesId2);
        determiner.determine(sysmeta2);
        assertTrue(determiner.getQuotaType().equals(QuotaTypeDeterminer.STORAGE));
        assertTrue(determiner.getInstanceId().equals(guid2.getValue()));
        assertTrue(!determiner.getInstanceId().equals(seriesId2.getValue()));
    }

}
