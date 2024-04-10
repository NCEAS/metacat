package edu.ucsb.nceas.metacat.object.handler;

import org.dataone.service.types.v1.ObjectFormatIdentifier;

import edu.ucsb.nceas.MCTestCase;
import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * The Junit test class for the factory class NonXMLMetadataHandlers
 * @author tao
 *
 */
public class NonXMLMetadataHandlersTest extends MCTestCase{
    
    /**
     * Constructor
     * @param name  the name of the test method
     */
    public NonXMLMetadataHandlersTest(String name) {
        super(name);
    }
    
    /**
     * Create a test suite
     * @return the generated test suite
     */
    public static Test suite() {
        TestSuite suite = new TestSuite();
        suite.addTest(new NonXMLMetadataHandlersTest("testNewNonXMLMetadataHandler"));
        return suite;
    }
    
    /**
     * Test the method of NewNonXMLMetadataHandler
     * @throws Exception
     */
    public void testNewNonXMLMetadataHandler() throws Exception {
        //test the null format id
        ObjectFormatIdentifier formatId = null;
        NonXMLMetadataHandler handler = NonXMLMetadataHandlers.newNonXMLMetadataHandler(formatId);
        assertTrue (handler == null);
        
        //test an XML metadata format id
        formatId = new ObjectFormatIdentifier();
        formatId.setValue("https://eml.ecoinformatics.org/eml-2.2.0");
        handler = NonXMLMetadataHandlers.newNonXMLMetadataHandler(formatId);
        assertTrue (handler == null);
        
        //test a data format id
        formatId = new ObjectFormatIdentifier();
        formatId.setValue("http://www.cuahsi.org/waterML/1.0/");
        handler = NonXMLMetadataHandlers.newNonXMLMetadataHandler(formatId);
        assertTrue (handler == null);
        
        //test the json-ld format id which is a non-xml format type
        formatId = new ObjectFormatIdentifier();
        formatId.setValue(NonXMLMetadataHandlers.JSON_LD);
        handler = NonXMLMetadataHandlers.newNonXMLMetadataHandler(formatId);
        assertTrue (handler instanceof JsonLDHandler);
        
    }

}
