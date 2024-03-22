package edu.ucsb.nceas.metacat.object.handler;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.dataone.service.exceptions.InvalidRequest;
import org.junit.Before;
import org.junit.Test;

import edu.ucsb.nceas.LeanTestUtils;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


/**
 * JUnit test class for the JsonLDHandler class
 * @author tao
 *
 */
public class JsonLDHandlerTest {
    public static final String JSON_LD_FILE_PATH = "test/json-ld.json";
    public static final String CHECKSUM_JSON_FILE = "847e1655bdc98082804698dbbaf85c35";
    public static final String INVALID_JSON_LD_FILE_PATH = "test/invalid-json-ld.json";
    public static final String CHECKSUM_INVALID_JSON_FILE = "ede435691fa0c68e9a3c23697ffc92d4";

    /**
     * Set up the test fixtures
     * @throws PropertyNotFoundException
     */
    @Before
    public void setUp() throws PropertyNotFoundException {
        LeanTestUtils.initializePropertyService(LeanTestUtils.PropertiesMode.UNIT_TEST);
    }


    /**
     * Test the valid method
     * @throws Exception
     */
    @Test
    public void testInvalid() throws Exception {
        JsonLDHandler handler = new JsonLDHandler();
        InputStream input = new FileInputStream(new File(JSON_LD_FILE_PATH));
        assertTrue(handler.validate(input));
        input = new FileInputStream(new File(INVALID_JSON_LD_FILE_PATH));
        try {
            handler.validate(input);
            fail("We can't reach here since it should throw an exception");
        } catch (Exception e) {
            assertTrue(e instanceof InvalidRequest);
        }
    }

    /*
     * A utility method to generate a temporary file.
     */
    public static File generateTmpFile(String prefix) throws IOException {
        String newPrefix = prefix + "-" + System.currentTimeMillis();
        String suffix =  null;
        File newFile = null;
        try {
            newFile = File.createTempFile(newPrefix, suffix, new File("."));
        } catch (Exception e) {
            //try again if the first time fails
            newFile = File.createTempFile(newPrefix, suffix, new File("."));
        }
        return newFile;
    }
}
