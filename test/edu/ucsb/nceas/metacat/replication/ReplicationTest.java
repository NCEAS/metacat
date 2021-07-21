/**
 *  '$RCSfile$'
 *  Copyright: 2004 Regents of the University of California and the
 *              National Center for Ecological Analysis and Synthesis
 *  Purpose: To test the Access Controls in metacat by JUnit
 *
 *   '$Author$'
 *     '$Date$'
 * '$Revision$'
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

package edu.ucsb.nceas.metacat.replication;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.net.URL;
import java.util.Hashtable;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.apache.commons.io.IOUtils;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.ObjectFormatIdentifier;
import org.dataone.service.types.v1.Session;
import org.dataone.service.types.v2.SystemMetadata;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import edu.ucsb.nceas.metacat.MetaCatServlet;
import edu.ucsb.nceas.metacat.client.Metacat;
import edu.ucsb.nceas.metacat.client.MetacatFactory;
import edu.ucsb.nceas.metacat.client.MetacatInaccessibleException;
import edu.ucsb.nceas.metacat.dataone.D1NodeServiceTest;
import edu.ucsb.nceas.metacat.dataone.MNodeService;
import edu.ucsb.nceas.metacat.object.handler.JsonLDHandlerTest;
import edu.ucsb.nceas.metacat.object.handler.NonXMLMetadataHandlers;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.metacat.util.DocumentUtil;
import edu.ucsb.nceas.metacat.util.MetacatUtil;
import edu.ucsb.nceas.metacat.util.ReplicationUtil;

import edu.ucsb.nceas.utilities.access.AccessControlInterface;
import edu.ucsb.nceas.utilities.access.DocInfoHandler;


/**
 * A JUnit test for testing Metacat replication
 */
public class ReplicationTest
    extends D1NodeServiceTest {
    
    private static final long forceReplicationSleep = 1 * 60 * 1000;
	private String targetReplicationServer = null;
	private Metacat targetMetacat = null;
	private final static String TITLE = "Test replication";
    
	/**
     * Constructor to build the test
     *
     * @param name the name of the test method
     */
    public ReplicationTest(String name) {
        super(name);
    }

    /**
     * Establish a testing framework by initializing appropriate objects
     */
    public void setUp() {
        try {
        	// get the target ("B server")
            targetReplicationServer = PropertyService.getProperty("test.replication.targetServer");
            m = MetacatFactory.createMetacatConnection(metacatUrl);
            targetMetacat = MetacatFactory.createMetacatConnection("https://" + targetReplicationServer + "/metacat");

        }
        catch (MetacatInaccessibleException mie) {
            System.err.println("Metacat is: " + metacatUrl);
            fail("Metacat connection failed." + mie.getMessage());
        } catch (Exception e) {
        	e.printStackTrace();
            fail(e.getMessage());
        }
    }

    /**
     * Release any objects after tests are complete
     */
    public void tearDown() {
    }

    /**
     * Create a suite of tests to be run together
     */
    public static Test suite() {
        TestSuite suite = new TestSuite();
        suite.addTest(new ReplicationTest("initialize"));
        // Test basic functions
        suite.addTest(new ReplicationTest("testCertificate"));
        suite.addTest(new ReplicationTest("testReplicateData_AtoB"));
        suite.addTest(new ReplicationTest("testReplicateEML_AtoB"));
        suite.addTest(new ReplicationTest("testReplicateDataLocking"));
        suite.addTest(new ReplicationTest("testDocumentInfo"));
        suite.addTest(new ReplicationTest("testReplicateJsonLD_AtoB"));
        return suite;
    }

    /**
     * Run an initial test that always passes to check that the test
     * harness is working.
     */
    public void initialize() {
        assertTrue(1 == 1);
    }
    
    public void testCertificate() {
    	try {
    	  //System.out.println("test certificate ============");
    		URL u = new URL("https://" + targetReplicationServer  + "/servlet/replication?server=" + MetacatUtil.getLocalReplicationServerName() + "&action=test");
			String test = ReplicationService.getURLContent(u);
			//System.out.println("the result is "+test);
			assertTrue(test.contains("Test successfully"));
			
    	} catch (Exception e) {
    		e.printStackTrace();
			fail(e.getMessage());
		}
    }
    
    public void testReplicateData_AtoB() {
    	try {
    		// the id
    		String baseDocid = DocumentUtil.generateDocumentId("replicationTest", 0);
    		String docid = baseDocid + "." + 1;
    		
    		// the test data
    		String object = "test";
    		
			// insert data locally
    		m.login(username, password);
    		m.upload(docid, "testObject", IOUtils.toInputStream(object, MetaCatServlet.DEFAULT_ENCODING), object.getBytes(MetaCatServlet.DEFAULT_ENCODING).length);
    		
    		// wait for replication (forced)
    		Thread.sleep(forceReplicationSleep);
    		
    		// check the target for the same data
    		targetMetacat.login(username, password);
    		InputStream is = targetMetacat.read(docid);
    		String replicatedObject = IOUtils.toString(is, MetaCatServlet.DEFAULT_ENCODING);
    		
    		assertEquals(object, replicatedObject);
    		
    		// update the object
    		docid = baseDocid + "." + 2;
    		object = "test2";
    		m.upload(docid, "testObject", IOUtils.toInputStream(object, MetaCatServlet.DEFAULT_ENCODING), object.getBytes(MetaCatServlet.DEFAULT_ENCODING).length);

    		// wait for replication (forced)
    		Thread.sleep(forceReplicationSleep);
    		
    		// check the target for the updated data
    		is = targetMetacat.read(docid);
    		replicatedObject = IOUtils.toString(is, MetaCatServlet.DEFAULT_ENCODING);
    		
    		assertEquals(object, replicatedObject);
    		
    		// update the access control rules
    		m.setAccess(
    				docid, 
    				AccessControlInterface.PUBLIC, 
    				AccessControlInterface.READSTRING, 
    				AccessControlInterface.ALLOW, 
    				AccessControlInterface.ALLOWFIRST);
    		
    		// wait for replication (forced)
    		Thread.sleep(forceReplicationSleep);
    		
    		// check the target for the same data, logout to act as public
    		targetMetacat.logout();
    		is = targetMetacat.read(docid);
    		replicatedObject = IOUtils.toString(is, MetaCatServlet.DEFAULT_ENCODING);
    		
    		assertEquals(object, replicatedObject);
    		
    		// delete the object
    		m.delete(docid);
    		
    		// wait for replication (forced)
    		Thread.sleep(forceReplicationSleep);
    		
    		// TODO: check that it is missing
    		// update should fail since it is "archived"
    		docid = baseDocid + "." + 3;
    		object = "test3";
    		try {
    			targetMetacat.upload(docid, "testObject", IOUtils.toInputStream(object, MetaCatServlet.DEFAULT_ENCODING), object.getBytes(MetaCatServlet.DEFAULT_ENCODING).length);
    		} catch (Exception e) {
				// should fail
    			assertTrue(true);
    			return;
			}
    		// if we get here, he have failed
    		fail("Should not have been able to update archived data");
    		
			
    	} catch (Exception e) {
    		e.printStackTrace();
			fail(e.getMessage());
		}
    }
    
    public void testReplicateEML_AtoB() {
    	try {
    		// the id
    		String baseDocid = DocumentUtil.generateDocumentId("replicationTest", 0);
    		String docid = baseDocid + "." + 1;
    		
    		// the test data, no public access
    		String emlContent = null;
			emlContent = getTestEmlDoc(
    				TITLE, //title, 
    				EML2_1_0, //emlVersion, 
    				null, //inlineData1, 
    				null, //inlineData2, 
    				null, //onlineUrl1, 
    				null, //onlineUrl2, 
    				null, //docAccessBlock , 
    				null, //inlineAccessBlock1, 
    				null, //inlineAccessBlock2, 
    				null, //onlineAccessBlock1, 
    				null //onlineAccessBlock2
    				);
    				
    		StringReader xmlReader = new StringReader(emlContent);
    		
			// insert data locally
    		m.login(username, password);
    		m.insert(docid, xmlReader, null);
    		
    		// wait for replication (forced)
    		Thread.sleep(forceReplicationSleep);
    		
    		// check the target for the same data
    		targetMetacat.login(username, password);
    		InputStream is = targetMetacat.read(docid);
    		String replicatedObject = IOUtils.toString(is, MetaCatServlet.DEFAULT_ENCODING);
    		
    		assertEquals(emlContent, replicatedObject);
    		
    		// update the object
    		docid = baseDocid + "." + 2;
    		//emlContent = getTestEmlDoc("Test replication2", EML2_1_0);
    		emlContent = getTestEmlDoc(
    				TITLE, //title, 
    				EML2_1_0, //emlVersion, 
    				null, //inlineData1, 
    				null, //inlineData2, 
    				null, //onlineUrl1, 
    				null, //onlineUrl2, 
    				null, //docAccessBlock , 
    				null, //inlineAccessBlock1, 
    				null, //inlineAccessBlock2, 
    				null, //onlineAccessBlock1, 
    				null //onlineAccessBlock2
    				);
    		xmlReader = new StringReader(emlContent);
    		m.update(docid, xmlReader, null);

    		// wait for replication (forced)
    		Thread.sleep(forceReplicationSleep);
    		
    		// check the target for the updated data
    		is = targetMetacat.read(docid);
    		replicatedObject = IOUtils.toString(is, MetaCatServlet.DEFAULT_ENCODING);
    		
    		assertEquals(emlContent, replicatedObject);
    		
    		// update the access control rules
    		m.setAccess(
    				docid, 
    				AccessControlInterface.PUBLIC, 
    				AccessControlInterface.READSTRING, 
    				AccessControlInterface.ALLOW, 
    				AccessControlInterface.ALLOWFIRST);
    		
    		// wait for replication (forced)
    		Thread.sleep(forceReplicationSleep);
    		
    		// check the target for the same data, logout to act as public
    		targetMetacat.logout();
    		is = targetMetacat.read(docid);
    		replicatedObject = IOUtils.toString(is, MetaCatServlet.DEFAULT_ENCODING);
    		
    		assertEquals(emlContent, replicatedObject);
    		
    		// delete the object
    		m.delete(docid);
    		
    		// wait for replication (forced)
    		Thread.sleep(forceReplicationSleep);
    		
			// query for the docid -- should not be returned since it is archived
			String queryString = getTestEmlQuery(TITLE, EML2_1_0);
			System.out.println("queryString: " + queryString);
			Reader xmlQuery = new StringReader(queryString);
			Reader resultReader = targetMetacat.query(xmlQuery);
			String results = IOUtils.toString(resultReader);
			System.out.println("results: " + results);
			assertFalse(results.contains(docid));
			
    	} catch (Exception e) {
    		e.printStackTrace();
			fail(e.getMessage());
		}
    }

	public void testReplicateDataLocking() {
		try {
			// the id
			String baseDocid = DocumentUtil.generateDocumentId("replicationTest", 0);
			String docid = baseDocid + "." + 1;
			
			// the test data
			String object = "test";
			
			// insert data locally
			m.login(username, password);
			m.upload(docid, "testObject", IOUtils.toInputStream(object, MetaCatServlet.DEFAULT_ENCODING), object.getBytes(MetaCatServlet.DEFAULT_ENCODING).length);
			
			// wait for replication (forced)
			Thread.sleep(forceReplicationSleep);
			
			// check the target for the same data
			targetMetacat.login(username, password);
			InputStream is = targetMetacat.read(docid);
			String replicatedObject = IOUtils.toString(is, MetaCatServlet.DEFAULT_ENCODING);
			
			assertEquals(object, replicatedObject);
			
			// update the object on the target
			docid = baseDocid + "." + 2;
			object = "test2";
			targetMetacat.upload(docid, "testObject", IOUtils.toInputStream(object, MetaCatServlet.DEFAULT_ENCODING), object.getBytes(MetaCatServlet.DEFAULT_ENCODING).length);
	
			// wait for replication (forced)
			Thread.sleep(forceReplicationSleep);
			
			// check the original has the result
			is = m.read(docid);
			replicatedObject = IOUtils.toString(is, MetaCatServlet.DEFAULT_ENCODING);
			
			assertEquals(object, replicatedObject);
			
			
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
	
	/**
	 * Tests the docInfo parser using reserved characters (&) in CDATA tags
	 */
	public void testDocumentInfo() {
		try {
			// the id
			String baseDocid = DocumentUtil.generateDocumentId("replicationTest", 0);
			String docid = baseDocid + "." + 1;
			
			// the test data
			String object = "test";
			String fileName = "testObject with & in filename";
			
			// insert data locally
			m.login(username, password);
			m.upload(docid, fileName, IOUtils.toInputStream(object, MetaCatServlet.DEFAULT_ENCODING), object.getBytes(MetaCatServlet.DEFAULT_ENCODING).length);
    		
    		// get the docinfo string
    		String docInfoStr = ReplicationService.getDocumentInfo(docid);
    					
    		System.out.println("docInfoStr: " + docInfoStr);
    		
			// strip out the system metadata portion
		    String systemMetadataXML = ReplicationUtil.getSystemMetadataContent(docInfoStr);
		   	docInfoStr = ReplicationUtil.getContentWithoutSystemMetadata(docInfoStr);

			//dih is the parser for the docinfo xml format
			DocInfoHandler dih = new DocInfoHandler();
			XMLReader docinfoParser = ReplicationHandler.initParser(dih);
			docinfoParser.parse(new InputSource(new StringReader(docInfoStr)));
			Hashtable<String, String> docinfoHash = dih.getDocInfo();
			
			// get the docname for testing &
			String docName = (String) docinfoHash.get("docname");
    		System.out.println("docName: " + docName);
    		
			assertEquals(fileName, docName);

    		// test user
    		String user = (String) docinfoHash.get("user_owner");
    		System.out.println("user_owner: " + user);
			assertEquals(username, user);

		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	/**
	 * Test the replication of an JsonLD document from A to B
	 */
	public void testReplicateJsonLD_AtoB() throws Exception {
            //create a json-ld object successfull
            String base = DocumentUtil.generateDocumentId("replicationTestJsonLD", 0);
            String guidStr = base + "." + 1;
            Session session = getTestSession();
            Identifier guid = new Identifier();
            guid.setValue(guidStr);
            InputStream input = new FileInputStream(new File(JsonLDHandlerTest.JSON_LD_FILE_PATH));
            SystemMetadata sysmeta = createSystemMetadata(guid, session.getSubject(), input);
            ObjectFormatIdentifier formatid = new ObjectFormatIdentifier();
            formatid.setValue(NonXMLMetadataHandlers.JSON_LD);
            sysmeta.setFormatId(formatid);

            InputStream object = new FileInputStream(new File(JsonLDHandlerTest.JSON_LD_FILE_PATH));
            Identifier pid = 
              MNodeService.getInstance(request).create(session, guid, object, sysmeta);
            SystemMetadata result = MNodeService.getInstance(request).getSystemMetadata(session, pid);
            assertTrue(result.getIdentifier().equals(guid));
            object.close();

            // wait for replication (forced)
            Thread.sleep(forceReplicationSleep);
            //get the docid(autogen)
            URL url = new URL("https://" + targetReplicationServer + "/d1/mn/v2/object/" + guidStr);
            // check the target for the same data
            InputStream is = url.openStream();
            String replicatedObject = IOUtils.toString(is, MetaCatServlet.DEFAULT_ENCODING);
            assertTrue(replicatedObject.contains("\"name\": \"Removal of organic carbon by natural bacterioplankton " 
                          + "communities as a function of pCO2 from laboratory experiments between 2012 and 2016\""));
    }
    
}

