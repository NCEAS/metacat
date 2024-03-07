package edu.ucsb.nceas.metacat;

import java.io.File;
import java.io.FileInputStream;

import org.apache.commons.io.IOUtils;
import org.dataone.service.exceptions.InvalidRequest;
import org.dataone.service.types.v1.ObjectFormatIdentifier;
import org.junit.Before;
import org.junit.Test;
import org.xml.sax.SAXException;

import static org.junit.Assert.fail;
import static org.junit.Assert.assertTrue;

import edu.ucsb.nceas.LeanTestUtils;
import edu.ucsb.nceas.metacat.object.handler.NonXMLMetadataHandlers;

/**
 * The test class of MetacatHandler
 * @author tao
 *
 */
public class MetacatHandlerTest {
    private MetacatHandler handler;

    /**
     * Setup
     * @throws Exception
     */
    @Before
    public void setUp() throws Exception {
        LeanTestUtils.initializePropertyService(LeanTestUtils.PropertiesMode.UNIT_TEST);
        handler = new MetacatHandler();
    }

    /**
     * Test the validateSciMeta method
     * @throws Exception
     */
    @Test
    public void testValidateSciMeta() throws Exception {
        // Test a valid eml2.2.0 object
        File eml = new File("test/eml-2.2.0.xml");
        byte[] xmlBytes = IOUtils.toByteArray(new FileInputStream(eml));
        ObjectFormatIdentifier formatId = new ObjectFormatIdentifier();
        formatId.setValue("https://eml.ecoinformatics.org/eml-2.2.0");
        handler.validateSciMeta(xmlBytes, formatId);
        // Test an invalid eml2.2.0 object - duplicated ids
        eml = new File("test/resources/eml-error-2.2.0.xml");
        xmlBytes = IOUtils.toByteArray(new FileInputStream(eml));
        try {
            handler.validateSciMeta(xmlBytes, formatId);
            fail("Test can reach here since the eml object is invalid");
        } catch (Exception e) {
            assertTrue("The message should say the id must be unique",
                        e.getMessage().contains("unique"));
        }
        // Test an invalid eml 2.1.1 object
        formatId.setValue("eml://ecoinformatics.org/eml-2.1.1");
        eml = new File("test/resources/eml-error.xml");
        xmlBytes = IOUtils.toByteArray(new FileInputStream(eml));
        try {
            handler.validateSciMeta(xmlBytes, formatId);
            fail("Test can reach here since the eml object is invalid");
        } catch (Exception e) {
            assertTrue("The exception should be SAXException", e instanceof SAXException);
            assertTrue("The message should say the element principal1 is incorrect",
                        e.getMessage().contains("principal1"));
        }
        // Test a valid eml 2.1.0 object
        formatId.setValue("eml://ecoinformatics.org/eml-2.0.1");
        eml = new File("test/eml-sample.xml");
        xmlBytes = IOUtils.toByteArray(new FileInputStream(eml));
        handler.validateSciMeta(xmlBytes, formatId);
        // Test a valid eml-beta 6
        formatId.setValue("-//ecoinformatics.org//eml-dataset-2.0.0beta6//EN");
        eml = new File("./test/jones.204.22.xml");
        xmlBytes = IOUtils.toByteArray(new FileInputStream(eml));
        handler.validateSciMeta(xmlBytes, formatId);
        // Test a valid json object
        File json = new File("test/json-ld.json");
        byte[] object = IOUtils.toByteArray(new FileInputStream(json));
        formatId.setValue(NonXMLMetadataHandlers.JSON_LD);
        handler.validateSciMeta(object, formatId);
        // Test a invalid json object
        json = new File("test/invalid-json-ld.json");
        object = IOUtils.toByteArray(new FileInputStream(json));
        try {
            handler.validateSciMeta(object, formatId);
            fail("Test can reach here since the json-ld object is invalid");
        } catch (Exception e) {
            assertTrue("The exception should be InvalidRequest",e instanceof InvalidRequest);
        }
    }

}
