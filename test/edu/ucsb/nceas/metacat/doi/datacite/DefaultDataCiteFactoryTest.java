package edu.ucsb.nceas.metacat.doi.datacite;

import java.io.FileInputStream;
import java.io.InputStream;

import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v2.SystemMetadata;
import org.dataone.service.util.TypeMarshaller;

import edu.ucsb.nceas.MCTestCase;
import junit.framework.Test;
import junit.framework.TestSuite;

public class DefaultDataCiteFactoryTest extends MCTestCase {
    private static final String RESULT= "<?xml version=\"1.0\" encoding=\"UTF-8\"?><resource xmlns=\"http://datacite.org/schema/kernel-4\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://datacite.org/schema/kernel-4 https://schema.datacite.org/meta/kernel-4.3/metadata.xsd\"><identifier identifierType=\"DOI\">10.5063/F1X34VQ5</identifier><creators><creator><creatorName>CN=Monica Ihli A139616,O=Google,C=US,DC=cilogon,DC=org</creatorName></creator></creators><titles><title xml:lang=\"en\">(:unkn)</title></titles><publisher>My Metacat Node</publisher><publicationYear>2017</publicationYear><resourceType resourceTypeGeneral=\"Dataset\"/><formats><format>text/xml</format></formats></resource>";
    
    public DefaultDataCiteFactoryTest (String name)  {
        super(name);
    }
    
    public static Test suite() 
    {
        TestSuite suite = new TestSuite();
        suite.addTest(new DefaultDataCiteFactoryTest("initialize"));
        suite.addTest(new DefaultDataCiteFactoryTest("testGenerateMetadata"));
        return suite;
    }
    
    public void initialize() {
        assertTrue(true);
    }
    
    public void testGenerateMetadata() throws Exception{
        Identifier guid = new Identifier();
        guid.setValue("doi:10.5063/F1X34VQ5");
        InputStream sysmetaInput = new FileInputStream("test/sysmeta-pangaea.xml");
        SystemMetadata sysmeta = TypeMarshaller.unmarshalTypeFromStream(SystemMetadata.class, sysmetaInput);
        sysmeta.setIdentifier(guid);
        DefaultDataCiteFactory factory = new DefaultDataCiteFactory();
        String metadata = factory.generateMetadata(guid, sysmeta);
        System.out.println(""+metadata);
        assertTrue(metadata.equals(RESULT));
    }

}
