/**
 *  '$RCSfile$'
 *  Copyright: 2021 Regents of the University of California and the
 *              National Center for Ecological Analysis and Synthesis
 *
 *   '$Author:  $'
 *     '$Date:  $'
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package edu.ucsb.nceas.metacat.object.handler;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.dataone.service.exceptions.InvalidRequest;
import org.dataone.service.exceptions.InvalidSystemMetadata;
import org.dataone.service.types.v1.Checksum;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Session;

import edu.ucsb.nceas.MCTestCase;
import edu.ucsb.nceas.metacat.IdentifierManager;
import edu.ucsb.nceas.metacat.database.DBConnection;
import edu.ucsb.nceas.metacat.database.DBConnectionPool;
import edu.ucsb.nceas.metacat.dataone.D1NodeServiceTest;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.metacat.restservice.multipart.DetailedFileInputStream;
import edu.ucsb.nceas.metacat.util.DocumentUtil;
import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * JUnit test class for the JsonLDHandler class
 * @author tao
 *
 */
public class JsonLDHandlerTest extends D1NodeServiceTest {
    private static String metadataStoragePath = null;
    private static String ip = "196.168.0.10";
    private static String agent = "java/junit_test";
    
    public static final String JSON_LD_FILE_PATH = "test/json-ld.json";
    private static final String CHECKSUM_JSON_FILE = "847e1655bdc98082804698dbbaf85c35";
    public static final String INVALID_JSON_LD_FILE_PATH = "test/invalid-json-ld.json";
    private static final String CHECKSUM_INVALID_JSON_FILE = "ede435691fa0c68e9a3c23697ffc92d4";
    
    /**
     * Constructor
     * @param name of the test method
     */
    public JsonLDHandlerTest(String name) {
        super(name);
        try {
            metadataStoragePath = PropertyService.getProperty("application.documentfilepath");
        } catch (Exception e) {
            e.printStackTrace();
        }
        
    }
    
    /**
     * Create a test suite
     * @return the generated test suite
     */
    public static Test suite() {
        TestSuite suite = new TestSuite();
        suite.addTest(new JsonLDHandlerTest("testInvalid"));
        suite.addTest(new JsonLDHandlerTest("testSave"));
        suite.addTest(new JsonLDHandlerTest("saveByteArray"));
        suite.addTest(new JsonLDHandlerTest("saveFile"));
        return suite;
    }
    
    /**
     * Test the valid method
     * @throws Exception
     */
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
    
    /**
     * Test the save method
     * @throws Exception
     */
    public void testSave() throws Exception {
       
        Session session = getTestSession();
        Checksum expectedChecksum = new Checksum();
        expectedChecksum.setAlgorithm("MD5");
        expectedChecksum.setValue(CHECKSUM_JSON_FILE);
        
        //save the DetaiedFileInputStream from the valid json-ld object without a checksum
        File temp1 = generateTmpFile("temp-json-ld-valid");
        InputStream input = new FileInputStream(new File(JSON_LD_FILE_PATH));
        OutputStream out = new FileOutputStream(temp1);
        IOUtils.copy(input, out);
        out.close();
        input.close();
        Checksum checksum = null;
        DetailedFileInputStream data = new DetailedFileInputStream(temp1, checksum);
        JsonLDHandler handler = new JsonLDHandler();
        Identifier pid = new Identifier();
        pid.setValue("test-id1-" + System.currentTimeMillis());
        assertTrue(temp1.exists());
        String localId = handler.save(data, NonXMLMetadataHandlers.JSON_LD, pid, expectedChecksum, session, ip, agent);
        assertTrue(!temp1.exists());
        assertTrue(IdentifierManager.getInstance().mappingExists(pid.getValue()));
        IdentifierManager.getInstance().removeMapping(pid.getValue(), localId);
        deleteXMLDocuments(localId);
        File savedFile = new File(metadataStoragePath, localId);
        assertTrue(savedFile.exists());
        
        //save the DetaiedFileInputStream from the valid json-ld object with the expected checksum
        File temp2 = generateTmpFile("temp2-json-ld-valid");
        input = new FileInputStream(new File(JSON_LD_FILE_PATH));
        out = new FileOutputStream(temp2);
        IOUtils.copy(input, out);
        out.close();
        input.close();
        data = new DetailedFileInputStream(temp2, expectedChecksum);
        pid = new Identifier();
        pid.setValue("test-id2-" + System.currentTimeMillis());
        assertTrue(temp2.exists());
        localId = handler.save(data, NonXMLMetadataHandlers.JSON_LD, pid, expectedChecksum, session, ip, agent);
        assertTrue(!temp2.exists());
        assertTrue(IdentifierManager.getInstance().mappingExists(pid.getValue()));
        IdentifierManager.getInstance().removeMapping(pid.getValue(), localId);
        deleteXMLDocuments(localId);
        File savedFile2 = new File(metadataStoragePath, localId);
        assertTrue(savedFile2.exists());
        
        Checksum expectedChecksumForInvalidJson = new Checksum();
        expectedChecksumForInvalidJson.setAlgorithm("MD5");
        expectedChecksumForInvalidJson.setValue(CHECKSUM_INVALID_JSON_FILE);
        
        //save the DetaiedFileInputStream from the invalid json-ld object without a checksum
        File temp3 = generateTmpFile("temp3-json-ld-valid");
        input = new FileInputStream(new File(INVALID_JSON_LD_FILE_PATH));
        out = new FileOutputStream(temp3);
        IOUtils.copy(input, out);
        out.close();
        input.close();
        checksum = null;
        data = new DetailedFileInputStream(temp3, checksum);
        pid = new Identifier();
        pid.setValue("test-id3-" + System.currentTimeMillis());
        assertTrue(temp3.exists());
        try {
            localId = handler.save(data, NonXMLMetadataHandlers.JSON_LD, pid, expectedChecksumForInvalidJson, session, ip, agent);
            fail("We can't reach here since it should throw an exception");
        } catch (Exception e) {
            assertTrue(e instanceof InvalidRequest);
        }
        assertTrue(!IdentifierManager.getInstance().mappingExists(pid.getValue()));
        temp3.delete();
       
        //save the DetaiedFileInputStream from the invalid json-ld object with the expected checksum
        File temp4 = generateTmpFile("temp4-json-ld-valid");
        input = new FileInputStream(new File(INVALID_JSON_LD_FILE_PATH));
        out = new FileOutputStream(temp4);
        IOUtils.copy(input, out);
        out.close();
        input.close();
        data = new DetailedFileInputStream(temp4, expectedChecksumForInvalidJson);
        pid = new Identifier();
        pid.setValue("test-id4-" + System.currentTimeMillis());
        assertTrue(temp4.exists());
        try {
            localId = handler.save(data, NonXMLMetadataHandlers.JSON_LD, pid, expectedChecksumForInvalidJson, session, ip, agent);
            fail("We can't reach here since it should throw an exception");
        } catch (Exception e) {
            assertTrue(e instanceof InvalidRequest);
        }
        assertTrue(!IdentifierManager.getInstance().mappingExists(pid.getValue()));
        temp4.delete();
        
        //save the DetaiedFileInputStream from the valid json-ld object with a wrong checksum
        File temp5 = generateTmpFile("temp5-json-ld-valid");
        input = new FileInputStream(new File(JSON_LD_FILE_PATH));
        out = new FileOutputStream(temp5);
        IOUtils.copy(input, out);
        out.close();
        input.close();
        data = new DetailedFileInputStream(temp5, expectedChecksum);
        pid = new Identifier();
        pid.setValue("test-id5-" + System.currentTimeMillis());
        assertTrue(temp5.exists());
        try {
            checksum = new Checksum();
            checksum.setAlgorithm("MD5");
            checksum.setValue(CHECKSUM_INVALID_JSON_FILE);
            localId = handler.save(data, NonXMLMetadataHandlers.JSON_LD, pid, checksum, session, ip, agent);
            fail("We can't reach here since it should throw an exception");
        } catch (Exception e) {
            assertTrue(e instanceof InvalidSystemMetadata);
        }
        assertTrue(!IdentifierManager.getInstance().mappingExists(pid.getValue()));
        temp5.delete();
    }
    
    /**
     * Save the jsonLD object coming from a byte array input stream
     * @throws Exception
     */
    public void saveByteArray() throws Exception {
        Session session = getTestSession();
        Checksum expectedChecksum = new Checksum();
        expectedChecksum.setAlgorithm("MD5");
        expectedChecksum.setValue(CHECKSUM_JSON_FILE);
        
        Checksum expectedChecksumForInvalidJson = new Checksum();
        expectedChecksumForInvalidJson.setAlgorithm("MD5");
        expectedChecksumForInvalidJson.setValue(CHECKSUM_INVALID_JSON_FILE);
        
        //save a valid json file with correct expected checksum
        String content = FileUtils.readFileToString(new File(JSON_LD_FILE_PATH), "UTF-8");
        InputStream data = new ByteArrayInputStream(content.getBytes());
        JsonLDHandler handler = new JsonLDHandler();
        Identifier pid = new Identifier();
        pid.setValue("testbye-id1-" + System.currentTimeMillis());
        String localId = handler.save(data, NonXMLMetadataHandlers.JSON_LD, pid, expectedChecksum, session, ip, agent);
        assertTrue(IdentifierManager.getInstance().mappingExists(pid.getValue()));
        IdentifierManager.getInstance().removeMapping(pid.getValue(), localId);
        deleteXMLDocuments(localId);
        File savedFile = new File(metadataStoragePath, localId);
        assertTrue(savedFile.exists());
        
        //save the  valid json-ld object with the wrong checksum
        data.reset();
        pid = new Identifier();
        pid.setValue("testbye-id2-" + System.currentTimeMillis());
        try {
            localId = handler.save(data, NonXMLMetadataHandlers.JSON_LD, pid, expectedChecksumForInvalidJson, session, ip, agent);
            fail("We can't reach here since it should throw an exception");
        } catch (Exception e) {
            assertTrue(e instanceof InvalidSystemMetadata);
        }
        assertTrue(!IdentifierManager.getInstance().mappingExists(pid.getValue()));
        
        //save an invalid jsonld file
        content = FileUtils.readFileToString(new File(INVALID_JSON_LD_FILE_PATH), "UTF-8");
        data = new ByteArrayInputStream(content.getBytes());
        pid = new Identifier();
        pid.setValue("testbye-id3-" + System.currentTimeMillis());
        try {
            localId = handler.save(data, NonXMLMetadataHandlers.JSON_LD, pid, expectedChecksumForInvalidJson, session, ip, agent);
            fail("We can't reach here since it should throw an exception");
        } catch (Exception e) {
            assertTrue(e instanceof InvalidRequest);
        }
        assertTrue(!IdentifierManager.getInstance().mappingExists(pid.getValue()));
    }
    
    /**
     * Save the jsonLD object coming from a regular file
     * @throws Exception
     */
    public void saveFile() throws Exception {
        Session session = getTestSession();
        Checksum expectedChecksum = new Checksum();
        expectedChecksum.setAlgorithm("MD5");
        expectedChecksum.setValue(CHECKSUM_JSON_FILE);
        
        Checksum expectedChecksumForInvalidJson = new Checksum();
        expectedChecksumForInvalidJson.setAlgorithm("MD5");
        expectedChecksumForInvalidJson.setValue(CHECKSUM_INVALID_JSON_FILE);
        
        //save a valid json file with correct expected checksum
        InputStream data = new FileInputStream(new File(JSON_LD_FILE_PATH));
        JsonLDHandler handler = new JsonLDHandler();
        Identifier pid = new Identifier();
        pid.setValue("testSaveFile-id1-" + System.currentTimeMillis());
        String localId = handler.save(data, NonXMLMetadataHandlers.JSON_LD, pid, expectedChecksum, session, ip, agent);
        assertTrue(IdentifierManager.getInstance().mappingExists(pid.getValue()));
        IdentifierManager.getInstance().removeMapping(pid.getValue(), localId);
        deleteXMLDocuments(localId);
        File savedFile = new File(metadataStoragePath, localId);
        assertTrue(savedFile.exists());
        data.close();
        
        //save the  valid json-ld object with the wrong checksum
        data = new FileInputStream(new File(JSON_LD_FILE_PATH));
        pid = new Identifier();
        pid.setValue("testSaveFile-id2-" + System.currentTimeMillis());
        try {
            localId = handler.save(data, NonXMLMetadataHandlers.JSON_LD, pid, expectedChecksumForInvalidJson, session, ip, agent);
            fail("We can't reach here since it should throw an exception");
        } catch (Exception e) {
            data.close();
            assertTrue(e instanceof InvalidSystemMetadata);
        }
        assertTrue(!IdentifierManager.getInstance().mappingExists(pid.getValue()));
        
        //save an invalid jsonld file
        pid = new Identifier();
        pid.setValue("test-saveFile-id3-" + System.currentTimeMillis());
        data = new FileInputStream(new File(INVALID_JSON_LD_FILE_PATH));
        try {
            localId = handler.save(data, NonXMLMetadataHandlers.JSON_LD, pid, expectedChecksumForInvalidJson, session, ip, agent);
            fail("We can't reach here since it should throw an exception");
        } catch (Exception e) {
            data.close();
            assertTrue(e instanceof InvalidRequest);
        }
        assertTrue(!IdentifierManager.getInstance().mappingExists(pid.getValue()));
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
    
    /**
     * Delete the record from the xml_documents table
     * @param localId
     * @throws SQLException
     */
    private static void deleteXMLDocuments(String localId) throws SQLException {
        String docId = DocumentUtil.getDocIdFromString(localId);
        DBConnection conn = null;
        int serialNumber = -1;
        PreparedStatement pStmt = null;
        try {
            //check out DBConnection
            conn = DBConnectionPool
                    .getDBConnection("DocumentImpl.deleteXMLDocuments");
            serialNumber = conn.getCheckOutSerialNumber();
            //delete a record
            pStmt = conn.prepareStatement(
                    "DELETE FROM xml_documents WHERE docid = ? ");
            pStmt.setString(1, docId);
            pStmt.execute();
        } finally {
            try {
                pStmt.close();
            } finally {
                //return back DBconnection
                DBConnectionPool.returnDBConnection(conn, serialNumber);
            }
        }
    }
}
