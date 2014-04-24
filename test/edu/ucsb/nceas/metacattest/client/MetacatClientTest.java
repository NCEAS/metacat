/**
 *  '$RCSfile$'
 *  Copyright: 2003 Regents of the University of California and the
 *              National Center for Ecological Analysis and Synthesis
 *  Purpose: To test the MetaCatURL class by JUnit
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

package edu.ucsb.nceas.metacattest.client;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;

import edu.ucsb.nceas.MCTestCase;
import edu.ucsb.nceas.metacat.client.DocumentNotFoundException;
import edu.ucsb.nceas.metacat.client.InsufficientKarmaException;
import edu.ucsb.nceas.metacat.client.Metacat;
import edu.ucsb.nceas.metacat.client.MetacatAuthException;
import edu.ucsb.nceas.metacat.client.MetacatException;
import edu.ucsb.nceas.metacat.client.MetacatFactory;
import edu.ucsb.nceas.metacat.client.MetacatInaccessibleException;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.utilities.IOUtil;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;
import junit.framework.Test;
import junit.framework.TestSuite;
import java.io.FileInputStream;

import org.apache.commons.io.IOUtils;


/**
 * A JUnit test for testing Step class processing
 */
public class MetacatClientTest extends MCTestCase
{

    private String wrongMetacatUrl=
    	"http://localhostBAD/some/servlet/metacat";
  
    private static String metacatUrl;
    private static String username;
	private static String password;
	private static String anotheruser;
	private static String anotherpassword;
	static {
		try {
		    metacatUrl = PropertyService.getProperty("test.metacatUrl");
			username = PropertyService.getProperty("test.mcUser");
			password = PropertyService.getProperty("test.mcPassword");
			anotheruser = PropertyService.getProperty("test.mcAnotherUser");
			anotherpassword = PropertyService.getProperty("test.mcAnotherPassword");
		} catch (PropertyNotFoundException pnfe) {
			System.err.println("Could not get property in static block: " 
					+ pnfe.getMessage());
		}
	}
    private String failpass = "uidfnkj43987yfdn";
    private String newdocid = null;
    private String testfile = "test/jones.204.22.xml";
    private String testEMLWithAccess = "test/tao.14563.1.xml";
    private String onlinetestdatafile = "test/onlineDataFile1";
    private String queryFile = "test/query.xml";
    private String testdocument = "";
    private Metacat m;
    private String spatialTestFile = "test/spatialEml.xml";
    private static final int TIME = 5;
    private static final String DOCID = "docid";
    private static final String DATAID = "dataid";
    
    /**
     * Constructor to build the test
     *
     * @param name the name of the test method
     */
    public MetacatClientTest(String name)
    {
        super(name);
        // prefix is a generalization of the term 'scope' which is the beginning part of an identifier
        // because of the getLatestDocid() test, we need to make sure that prefix (which is used as scope)
        // is specific to these tests, and that docids are sequentially assigned.
        // (so keep this value for prefix, or make it more specific!)
        prefix = "metacatClientTest";
        newdocid = generateDocumentId();
    }

    /**
     * Establish a testing framework by initializing appropriate objects
     */
    public void setUp()
    {
        FileInputStream fis = null;
        try {
        	fis = new FileInputStream(testfile);
            testdocument = IOUtils.toString(fis);
            fis.close();
        } catch (IOException ioe) {
            fail("Can't read test data to run the test: " + testfile);
        } finally {
            IOUtils.closeQuietly(fis);
        }

        try {
            debug("Test Metacat: " + metacatUrl);
            m = MetacatFactory.createMetacatConnection(metacatUrl);
        } catch (MetacatInaccessibleException mie) {
            System.err.println("Metacat is: " + metacatUrl);
            fail("Metacat connection failed." + mie.getMessage());
        }
    }

    /**
     * Release any objects after tests are complete
     */
    public void tearDown()
    {
    }

    /**
     * Create a suite of tests to be run together
     */
    public static Test suite()
    {
      TestSuite suite = new TestSuite();     
      suite.addTest(new MetacatClientTest("initialize"));
      suite.addTest(new MetacatClientTest("invalidLogin"));
      suite.addTest(new MetacatClientTest("logoutAndInvalidInsert"));
      suite.addTest(new MetacatClientTest("login"));
      suite.addTest(new MetacatClientTest("insert"));
      suite.addTest(new MetacatClientTest("getLastDocid"));  // needs to directly follow insert test!!
      suite.addTest(new MetacatClientTest("getNewestDocRevision"));  // (also tries to insert)
      suite.addTest(new MetacatClientTest("upload"));
      suite.addTest(new MetacatClientTest("upload_stream"));
      suite.addTest(new MetacatClientTest("upload_stream_chunked"));
      suite.addTest(new MetacatClientTest("invalidRead"));
      suite.addTest(new MetacatClientTest("read"));
      suite.addTest(new MetacatClientTest("query"));
      suite.addTest(new MetacatClientTest("queryWithQformat"));
      suite.addTest(new MetacatClientTest("invalidUpdate"));
      suite.addTest(new MetacatClientTest("update"));
      suite.addTest(new MetacatClientTest("invalidDelete"));
      suite.addTest(new MetacatClientTest("delete"));
      suite.addTest(new MetacatClientTest("inaccessibleMetacat"));
      suite.addTest(new MetacatClientTest("reuseSession"));
      suite.addTest(new MetacatClientTest("reuseInvalidSession"));
      suite.addTest(new MetacatClientTest("insertSpatialDocs"));
      suite.addTest(new MetacatClientTest("getAccessControl"));
      suite.addTest(new MetacatClientTest("getAccessControlFromDocWithoutAccess"));
      return suite;
  }

    /**
     * Run an initial test that always passes to check that the test
     * harness is working.
     */
    public void initialize()
    {
        assertTrue(1 == 1);
    }

    /**
     * Test the login() function with valid credentials
     */
    public void login()
    {
        debug("\nStarting login test...");
        // Try a valid login
        try {
            String response = m.login(username, password);
            debug("login(): response=" + response);
            assertTrue(response != null);
            assertTrue(response.indexOf("<login>") != -1);
            String sessionId = m.getSessionId();
            debug("login(): Session ID=" + m.getSessionId());
            assertTrue(sessionId != null);
            assertTrue(response.indexOf(m.getSessionId()) != -1);
        } catch (MetacatAuthException mae) {
            fail("Authorization failed:\n" + mae.getMessage());
        } catch (MetacatInaccessibleException mie) {
            fail("Metacat Inaccessible:\n" + mie.getMessage());
        }
    }

    /**
     * Test the login() function with INVALID credentials
     */
    public void invalidLogin()
    {
        debug("\nStarting invalidLogin test...");
        // Try an invalid login
        try {
            m.login(username, failpass);
            fail("Authorization should have failed.");
        } catch (MetacatAuthException mae) {
            assertTrue(1 == 1);
        } catch (MetacatInaccessibleException mie) {
            fail("Metacat Inaccessible:\n" + mie.getMessage());
        }
    }

    /**
     * Test the logout() function. When logout, user will be public, it couldn't
     * insert a document.
     */
    public void logoutAndInvalidInsert()
    {
       debug("\nStarting logoutAndInvalidInsert test...");
       try {
            String identifier = newdocid + ".1";
            m.login(username, password);
            m.logout();
            String response = m.insert(identifier,
                    new StringReader(testdocument), null);
            debug("logoutAndInvalidInsert(): Response in logout="+response);
            assertTrue(response.indexOf("<success>") == -1);
        } catch (MetacatAuthException mae) {
            fail("Authorization failed:\n" + mae.getMessage());
        } catch (MetacatInaccessibleException mie) {
            fail("Metacat Inaccessible:\n" + mie.getMessage());
        } catch (InsufficientKarmaException ike) {
            fail("Insufficient karma:\n" + ike.getMessage());
        } catch (MetacatException me) {
            if(me.getMessage().
              indexOf("Permission denied for user public inser") == -1){
               fail("Metacat Error:\n" + me.getMessage());
           }
        } catch (Exception e) {
            fail("General exception:\n" + e.getMessage());
        }
    }

    /**
     * Test the read() function with a known document
     */
    public void read()
    {
        debug("\nStarting read test...");
        try {
            m.login(username, password);
            String docid = readIdFromFile(DOCID);
            debug("docid=" + docid);
            InputStream is = m.read(docid+".1");
            String doc = IOUtils.toString(is);
            // why oh why were we adding a newline??
            //doc = doc +"\n";
            if (!doc.equals(testdocument)) {
                debug("doc         :" + doc + "EOF");
                debug("testdocument:" + testdocument + "EOF");
            }
            assertTrue(doc.equals(testdocument));
        } catch (MetacatAuthException mae) {
            fail("Authorization failed:\n" + mae.getMessage());
        } catch (MetacatInaccessibleException mie) {
            fail("Metacat Inaccessible:\n" + mie.getMessage());
        } catch (Exception e) {
            fail("General exception:\n" + e.getMessage());
        }
    }

    /**
     * A user try to read a document which it doesn't have read permission
     */
    public void invalidRead()
    {
        debug("\nStarting invalidRead test...");
        try {
            m.login(anotheruser, anotherpassword);
            String docid = readIdFromFile(DOCID);
            Reader r = new InputStreamReader(m.read(docid+".1"));
            String doc = IOUtil.getAsString(r, true);
            assertTrue(doc.indexOf("<error>") != -1);
            debug("invalidRead(): doc="+ doc);
        } catch (MetacatAuthException mae) {
            fail("Authorization failed:\n" + mae.getMessage());
        } catch (MetacatInaccessibleException mie) {
            fail("Metacat Inaccessible:\n" + mie.getMessage());
        } catch (InsufficientKarmaException ike) {
            assertTrue(1 == 1);
        } catch (MetacatException e) {
            fail("Metacat exception:\n" + e.getMessage());
        } catch (IOException ioe) {
            fail("IO exception:\n" + ioe.getMessage());
        } catch (DocumentNotFoundException ne) {
            fail("DocumentNotFound exception:\n" + ne.getMessage());

        } catch(Exception e) {
            fail("Exception:\n" + e.getMessage());
        }
    }

    /**
     * Test the query() function with a known document
     */
    public void query()
    {
        debug("\nStarting query test...");
        try {
            m.login(username,password);
            FileReader fr = new FileReader(queryFile);
            Reader r = m.query(fr);
            String result = IOUtil.getAsString(r, true);
            debug("query(): Query result=\n" + result);
            String docid = readIdFromFile(DOCID);
            assertTrue(result.indexOf(docid+".1")!=-1);
            assertTrue(result.indexOf("<?xml")!=-1);
        } catch (MetacatAuthException mae) {
            fail("Authorization failed:\n" + mae.getMessage());
        } catch (MetacatInaccessibleException mie) {
            fail("Metacat Inaccessible:\n" + mie.getMessage());
        } catch (Exception e) {
            fail("General exception:\n" + e.getMessage());
        }
    }
    /**
     * Test the query() function with a known document
     */
    public void queryWithQformat()
    {
        debug("\nStarting queryWithQformat test...");
        try {
            m.login(username,password);
            FileReader fr = new FileReader(queryFile);
            String qformat = "knb";
            Reader r = m.query(fr, qformat);
            String result = IOUtil.getAsString(r, true);
            debug("queryWithQformat(): Query result=\n" + result);
            String docid = readIdFromFile(DOCID);
            assertTrue(result.indexOf(docid+".1")!=-1);
            assertTrue(result.indexOf("<html>")!=-1);
        } catch (MetacatAuthException mae) {
            fail("Authorization failed:\n" + mae.getMessage());
        } catch (MetacatInaccessibleException mie) {
            fail("Metacat Inaccessible:\n" + mie.getMessage());
        } catch (Exception e) {
            fail("General exception:\n" + e.getMessage());
        }
    }

    /**
     * Test the insert() function with a known document
     */
    public void insert()
    {
        debug("\nStarting insert test...");
        try {
            String identifier = newdocid + ".1";
            //write newdocid into file for persistance
            writeIdToFile(DOCID, newdocid);
            m.login(username, password);
            String response = m.insert(identifier,
                    new StringReader(testdocument), null);
            assertTrue(response.indexOf("<success>") != -1);
            assertTrue(response.indexOf(identifier) != -1);
            debug("insert(): response=" + response);

        } catch (MetacatAuthException mae) {
            fail("Authorization failed:\n" + mae.getMessage());
        } catch (MetacatInaccessibleException mie) {
            fail("Metacat Inaccessible:\n" + mie.getMessage());
        } catch (InsufficientKarmaException ike) {
            assertTrue(1 == 1);
            fail("Insufficient karma:\n" + ike.getMessage());
        } catch (MetacatException me) {
            fail("Metacat Error:\n" + me.getMessage());
        } catch (Exception e) {
            fail("General exception:\n" + e.getMessage());
        }
    }
    
    /**
     * Test to get access control part of a document
     */
    public void getAccessControl() {
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(testEMLWithAccess);
            String document = IOUtils.toString(fis);
            String identifier = newdocid + ".1";
            m.login(username, password);
            String response = m.insert(identifier, new StringReader(document), null);
            assertTrue(response.indexOf("<success>") != -1);
            assertTrue(response.indexOf(identifier) != -1);
            response = m.getAccessControl(identifier);
            //System.out.println("reponse is "+reponse);
            assertTrue(response.indexOf("<permission>read</permission>") != -1);
            assertTrue(response.indexOf("<principal>public</principal>") != -1);
            fis.close();
        } catch (MetacatAuthException mae) {
            fail("Authorization failed:\n" + mae.getMessage());
        } catch (MetacatInaccessibleException mie) {
            fail("Metacat Inaccessible:\n" + mie.getMessage());
        } catch (InsufficientKarmaException ike) {
            assertTrue(1 == 1);
            fail("Insufficient karma:\n" + ike.getMessage());
        } catch (MetacatException me) {
            fail("Metacat Error:\n" + me.getMessage());
        } catch (Exception e) {
            fail("General exception:\n" + e.getMessage());
        } finally {
            IOUtils.closeQuietly(fis);
        }
    }
    
    /**
     * Test to get access control part of a document
     */
    public void getAccessControlFromDocWithoutAccess() {
      try {
        String identifier = newdocid + ".1";
        m.login(username, password);
        String response = m.insert(identifier,
                new StringReader(testdocument), null);
        assertTrue(response.indexOf("<success>") != -1);
        assertTrue(response.indexOf(identifier) != -1);
        response = m.getAccessControl(identifier);
        //System.out.println("the identifier is "+identifier);
        //System.out.println("reponse is "+response);
        assertTrue(response.indexOf("<permission>read</permission>") == -1);
        assertTrue(response.indexOf("<principal>public</principal>") == -1);
        assertTrue(response.indexOf("<access authSystem=") != -1);
    } catch (MetacatAuthException mae) {
        fail("Authorization failed:\n" + mae.getMessage());
    } catch (MetacatInaccessibleException mie) {
        fail("Metacat Inaccessible:\n" + mie.getMessage());
    } catch (InsufficientKarmaException ike) {
        assertTrue(1 == 1);
        fail("Insufficient karma:\n" + ike.getMessage());
    } catch (MetacatException me) {
        fail("Metacat Error:\n" + me.getMessage());
    } catch (Exception e) {
        fail("General exception:\n" + e.getMessage());
    }
     
  }

    /**
     * Test the upload() function with a data file.
     * 1. Insert version 1 successfully.
     * 2. Update version 2 successfully
     * 3. Update version 2 again and should be failed.
     */
    public void upload()
    {
        debug("\nStarting upload test...");
        try {
            newdocid = generateDocumentId();
            String identifier = newdocid + ".1";
            writeIdToFile(DATAID, newdocid);
            m.login(username, password);
            String response = m.upload(identifier,
                                     new File(onlinetestdatafile));
            assertTrue(response.indexOf("<success>") != -1);
            assertTrue(response.indexOf(identifier) != -1);
            identifier = newdocid +".2";
            response = m.upload(identifier,
                    new File(onlinetestdatafile));
            assertTrue(response.indexOf("<success>") != -1);
            assertTrue(response.indexOf(identifier) != -1);
            debug("upload(): response=" + response);
            //upload the same identifier again. it should return an error
            try
            {
                response = m.upload(identifier,
                      new File(onlinetestdatafile));
                fail("Metacat shouldn't successfully upload the same identifier twice "+response);
            }
            catch(Exception ee)
            {
                assertTrue(ee.getMessage().indexOf("<error>") != -1);
            }

        } catch (MetacatAuthException mae) {
            fail("Authorization failed:\n" + mae.getMessage());
        } catch (MetacatInaccessibleException mie) {
          mie.printStackTrace();
            fail("Metacat Inaccessible:\n" + mie.getMessage());
        } catch (InsufficientKarmaException ike) {
            assertTrue(1 == 1);
            fail("Insufficient karma:\n" + ike.getMessage());
        } catch (MetacatException me) {
            fail("Metacat Error:\n" + me.getMessage());
        } catch (Exception e) {
            fail("General exception:\n" + e.getMessage());
        }
    }
    


    /**
     * Test the upload() function by passing an InputStream
     */
    public void upload_stream()
    {
        debug("\nStarting upload_stream test...");
        try {
            newdocid = generateDocumentId();
            String identifier = newdocid + ".1";
            m.login(username, password);
            File testFile = new File(onlinetestdatafile);
            String response = m.upload(identifier, "onlineDataFile1",
                                       new FileInputStream(testFile),
                                       (int) testFile.length());

            assertTrue(response.indexOf("<success>") != -1);
            assertTrue(response.indexOf(identifier) != -1);
            identifier = newdocid + ".2";
            response = m.upload(identifier, "onlineDataFile1",
                    new FileInputStream(testFile),
                    (int) testFile.length());

           assertTrue(response.indexOf("<success>") != -1);
           assertTrue(response.indexOf(identifier) != -1);
           debug("upload_stream(): response=" + response);

        } catch (MetacatAuthException mae) {
            fail("Authorization failed:\n" + mae.getMessage());
        } catch (MetacatInaccessibleException mie) {
          mie.printStackTrace();
            fail("Metacat Inaccessible:\n" + mie.getMessage());
        } catch (InsufficientKarmaException ike) {
            assertTrue(1 == 1);
            fail("Insufficient karma:\n" + ike.getMessage());
        } catch (MetacatException me) {
            fail("Metacat Error:\n" + me.getMessage());
        } catch (Exception e) {
            fail("General exception:\n" + e.getMessage());
        }
    }
    
    /**
     * Test the upload() function by passing an InputStream
     */
    public void upload_stream_chunked()
    {
        debug("\nStarting upload_stream_chunked test...");
        try {
            newdocid = generateDocumentId();
            String identifier = newdocid + ".1";
            m.login(username, password);
            File testFile = new File(onlinetestdatafile);
            String response = m.upload(identifier, "onlineDataFile1",
                                       new FileInputStream(testFile), -1);

            assertTrue(response.indexOf("<success>") != -1);
            assertTrue(response.indexOf(identifier) != -1);
            identifier = newdocid + ".2";
            response = m.upload(identifier, "onlineDataFile1",
                    new FileInputStream(testFile), -1);

           assertTrue(response.indexOf("<success>") != -1);
           assertTrue(response.indexOf(identifier) != -1);
           debug("upload_stream_chunked(): response=" + response);

        } catch (MetacatAuthException mae) {
            fail("Authorization failed:\n" + mae.getMessage());
        } catch (MetacatInaccessibleException mie) {
          mie.printStackTrace();
            fail("Metacat Inaccessible:\n" + mie.getMessage());
        } catch (InsufficientKarmaException ike) {
            assertTrue(1 == 1);
            fail("Insufficient karma:\n" + ike.getMessage());
        } catch (MetacatException me) {
            fail("Metacat Error:\n" + me.getMessage());
        } catch (Exception e) {
            fail("General exception:\n" + e.getMessage());
        }
    }

    /**
     * Test the invalidUpdate() function. A user try to update a document
     * which it doesn't have permission
     */
    public void invalidUpdate()
    {
        debug("\nStarting invalidUpdate test...");
        try {
        	String docid = readIdFromFile(DOCID);
            String identifier = docid + ".2";
            m.login(anotheruser, anotherpassword);
            String response = m.update(identifier,
                    new StringReader(testdocument), null);
            assertTrue(response.indexOf("<success>") == -1);

        } catch (MetacatAuthException mae) {
            fail("Authorization failed:\n" + mae.getMessage());
        } catch (MetacatInaccessibleException mie) {
            fail("Metacat Inaccessible:\n" + mie.getMessage());
        } catch (InsufficientKarmaException ike) {
            assertTrue(1 == 1);
        } catch (MetacatException me) {
            fail("Metacat Error:\n" + me.getMessage());
        } catch (Exception e) {
            fail("General exception:\n" + e.getMessage());
        }
    }

    /**
     * Test the update() function with a known document
     */
    public void update()
    {
        debug("\nStarting update test...");
        try {
        	String docid = readIdFromFile(DOCID);
            String identifier = docid + ".2";
            m.login(username, password);
            String response = m.update(identifier,
                    new StringReader(testdocument), null);
            assertTrue(response.indexOf("<success>") != -1);
            assertTrue(response.indexOf(identifier) != -1);
            debug("update(): response=" + response);

        } catch (MetacatAuthException mae) {
            fail("Authorization failed:\n" + mae.getMessage());
        } catch (MetacatInaccessibleException mie) {
            fail("Metacat Inaccessible:\n" + mie.getMessage());
        } catch (InsufficientKarmaException ike) {
            fail("Insufficient karma:\n" + ike.getMessage());
        } catch (MetacatException me) {
            fail("Metacat Error:\n" + me.getMessage());
        } catch (Exception e) {
            fail("General exception:\n" + e.getMessage());
        }
    }

    /**
     * A user try delete a document which it doesn't have permission
     */
    public void invalidDelete()
    {
        debug("\nStarting invalidDelete test...");
        try {
            String identifier = newdocid + ".2";
            m.login(anotheruser, anotherpassword);
            String response = m.delete(identifier);
            assertTrue(response.indexOf("<success>") == -1);
            debug("invalidDelete(): response=" + response);

        } catch (MetacatAuthException mae) {
            fail("Authorization failed:\n" + mae.getMessage());
        } catch (MetacatInaccessibleException mie) {
            fail("Metacat Inaccessible:\n" + mie.getMessage());
        } catch (InsufficientKarmaException ike) {
            assertTrue(1 == 1);
        } catch (MetacatException me) {
            assertTrue(1 == 1);
        } catch (Exception e) {
            fail("General exception:\n" + e.getMessage());
        }
    }

    /**
     * Test the delete() function with a known document
     */
    public void delete()
    {
        debug("\nStarting delete test...");
        try {
        	Thread.sleep(10000);
        	String docid = readIdFromFile(DOCID);
            String identifier = docid + ".2";
            m.login(username, password);
            String response = m.delete(identifier);
            assertTrue(response.indexOf("<success>") != -1);
            // delete the docid persistence file.
            deleteFile(DOCID);
            deleteFile(DATAID);
            debug("delete(): response=" + response);

        } catch (MetacatAuthException mae) {
            fail("Authorization failed:\n" + mae.getMessage());
        } catch (MetacatInaccessibleException mie) {
            fail("Metacat Inaccessible:\n" + mie.getMessage());
        } catch (InsufficientKarmaException ike) {
            fail("Insufficient karma:\n" + ike.getMessage());
        } catch (MetacatException me) {
            fail("Metacat Error:\n" + me.getMessage());
        } catch (Exception e) {
            fail("General exception:\n" + e.getMessage());
        }
    }

    /**
     * Test the to connect a wrong metacat url. Create a new metacat with a
     * inaccessible url. Then try to login it. If get a MetacatInaccessible
     * exception, this is right.
     */
    public void inaccessibleMetacat()
    {
        debug("\nStarting inaccessibleMetacat test...");
        Metacat mWrong = null;
        try {
            mWrong = MetacatFactory.createMetacatConnection(wrongMetacatUrl);
        } catch (MetacatInaccessibleException mie) {
            fail("Metacat Inaccessible:\n" + mie.getMessage());
        }

        try {
            mWrong.login(username, password);
        } catch (MetacatInaccessibleException mie) {
            assertTrue(1 == 1);
        } catch (MetacatAuthException mae) {
            fail("Authorization failed:\n" + mae.getMessage());
        }
    }

    /**
     * Try to perform an action that requires a session to be valid without
     * having logged in first by setting the sessionId from a previous
     * session.  Then insert a document.
     */
    public void reuseSession()
    {
        debug("\nStarting reuseSession test...");
        String oldSessionId = "";
        try {
        	debug("creating metacat connection to: " + metacatUrl);
            Metacat mtemp = MetacatFactory.createMetacatConnection(metacatUrl);
        	debug("creating metacat connection with credentials: " + username + ":" + password);
            String response = mtemp.login(username, password);
            oldSessionId = mtemp.getSessionId();
            debug("preparing to reuse session with oldSessionId:" + oldSessionId);
        } catch (MetacatAuthException mae) {
            fail("Authorization failed:\n" + mae.getMessage());
        } catch (MetacatInaccessibleException mie) {
            System.err.println("Metacat is: " + metacatUrl);
            fail("Metacat connection failed." + mie.getMessage());
        }

        try {
            String identifier = generateDocumentId() + ".1";
            Metacat m2 = null;
            try {
            	debug("Creating second connection and logging out of it.");
                m2 = MetacatFactory.createMetacatConnection(metacatUrl);
                m2.login(username, password);
                m2.logout();
            } catch (MetacatInaccessibleException mie) {
                System.err.println("Could not connect to metacat at: " + metacatUrl 
                		+ " : " + mie.getMessage());
                fail("Metacat connection failed." + mie.getMessage());
            }
            m2.setSessionId(oldSessionId);
            debug("Reusing second session with session id :" + m2.getSessionId());
            String response = m2.insert(identifier,
                    new StringReader(testdocument), null);
            debug("Reuse second session insert response: " + response);
            assertTrue(response.indexOf("<success>") != -1);
        } catch (MetacatInaccessibleException mie) {
            fail("Metacat Inaccessible:\n" + mie.getMessage());
        } catch (InsufficientKarmaException ike) {
            fail("Insufficient karma:\n" + ike.getMessage());
        } catch (MetacatException me) {
            fail("Metacat Error:\n" + me.getMessage());
        } catch (Exception e) {
            fail("General exception:\n" + e.getMessage());
        }
    }

    /**
     * Try to perform an action that requires a session to be valid without
     * having logged in first, but use an invalid session identifier.
     * Then insert a document, which should fail.
     */
    public void reuseInvalidSession()
    {
        debug("\nStarting resuseInvalidSession test...");
        String oldSessionId = "foobar";
        try {
            String identifier = generateDocumentId() + ".1";
            Metacat m3 = null;
            try {
                m3 = MetacatFactory.createMetacatConnection(metacatUrl);
                m3.logout();
            } catch (MetacatInaccessibleException mie) {
                System.err.println("Metacat is: " + metacatUrl);
                fail("Metacat connection failed." + mie.getMessage());
            }
            debug("reuseInvalidSession(): SessionId (m3): " + m3.getSessionId());
            m3.setSessionId(oldSessionId);
            debug("reuseInvalidSession(): SessionId(m3 after set)=" + m3.getSessionId());
            debug("reuseInvalidSession(): identifier=" + identifier);
            String response = m3.insert(identifier,
                    new StringReader(testdocument), null);
            debug("reuseInvalidSession(): response=" + response);
            assertTrue(response.indexOf("<success>") == -1);
        } catch (MetacatInaccessibleException mie) {
            fail("Metacat Inaccessible:\n" + mie.getMessage());
        } catch (InsufficientKarmaException ike) {
            fail("Insufficient karma:\n" + ike.getMessage());
        } catch (MetacatException me) {
            if(me.getMessage().
               indexOf("Permission denied for user public inser") == -1){
                fail("Metacat Error:\n" + me.getMessage());
            }
        } catch (Exception e) {
            fail("General exception:\n" + e.getMessage());
        }
    }
    
   /**
    * Get the most recent document id for a given scope and be sure
    * that it matches the one we last inserted. Assumes this test is run
    * immediately following a successful insert() test, AND that the docid
    * inserted has the maximal numerical value.
    */
   public void getLastDocid()
   {
       debug("\nStarting getLastDocid test...");
       try {
           Metacat m3 = null;
           try {
               m3 = MetacatFactory.createMetacatConnection(metacatUrl);
           } catch (MetacatInaccessibleException mie) {
               System.err.println("Metacat is: " + metacatUrl);
               fail("Metacat connection failed." + mie.getMessage());
           }
           String lastId = m3.getLastDocid(prefix);
           
           debug("getLastDocid(): Scope = '"+ prefix + "', Last Id = " + lastId);
           
           //get docid from file
           String docid = readIdFromFile(DOCID);
           debug("getLastDocid(): File Id = " + docid + ".1");
           assertTrue(lastId.equals(docid + ".1"));
       } catch (MetacatException me) {
           fail("Metacat Error:\n" + me.getMessage());
       } catch (Exception e) {
           fail("General exception:\n" + e.getMessage());
       }
   }

   /**
    * Try to perform an action that requires a session to be valid without
    * having logged in first, but use an invalid session identifier.
    * Then insert a document, which should fail.
    */
   public void getNewestDocRevision()
   {
       debug("\nStarting getNewestDocRevision test...");
       try {
           
           Metacat m3 = null;
           try {
               m3 = MetacatFactory.createMetacatConnection(metacatUrl);
           } catch (MetacatInaccessibleException mie) {
               System.err.println("Metacat is: " + metacatUrl);
               fail("Metacat connection failed." + mie.getMessage());
           }
           // get docid from file
           String docid = readIdFromFile(DOCID);
           int revision = m3.getNewestDocRevision(docid);
           debug("getNewestDocRevision(): revision=" + revision);
           assertTrue(revision == 1);
       } catch (MetacatException me) {
           if(me.getMessage().
              indexOf("Permission denied for user public inser") == -1){
               fail("Metacat Error:\n" + me.getMessage());
           }
       } catch (Exception e) {
           fail("General exception:\n" + e.getMessage());
       }
   }
   
   /**
    * Try to insert a bunch of eml documents which contain spatial information
    * This test is used to try to find a bug in spatial part of metacat
    */
    public void insertSpatialDocs() throws IOException
    {
        debug("\nStarting insertSpatialDocs test...");
        
    	FileReader fr = new FileReader(spatialTestFile);
        String spatialtestdocument = IOUtil.getAsString(fr, true);
        debug("insertSpatialDocs(): the eml is "+spatialtestdocument);
    	for (int i=0; i<TIME; i++)
    	{
	    	try {
	    		Thread.sleep(20000);
	    		newdocid = generateDocumentId();
	            String identifier = newdocid + ".1";
	            debug("insertSpatialDocs(): the docid is "+identifier);
	            m.login(username, password);
	            String response = m.insert(identifier,
	                    new StringReader(spatialtestdocument), null);
	            assertTrue(response.indexOf("<success>") != -1);
	            assertTrue(response.indexOf(identifier) != -1);
	            Thread.sleep(20000);
	            identifier = newdocid +".2";
	            response = m.update(identifier,
	                    new StringReader(spatialtestdocument), null);
	            assertTrue(response.indexOf("<success>") != -1);
	            Thread.sleep(20000);
	            String response2 = m.delete(identifier);
	            assertTrue(response2.indexOf("<success>") != -1);
	            debug("insertSpatialDocs(): response=" + response);
	
	        } catch (MetacatAuthException mae) {
	            fail("Authorization failed:\n" + mae.getMessage());
	        } catch (MetacatInaccessibleException mie) {
	            fail("Metacat Inaccessible:\n" + mie.getMessage());
	        } catch (InsufficientKarmaException ike) {
	            assertTrue(1 == 1);
	            fail("Insufficient karma:\n" + ike.getMessage());
	        } catch (MetacatException me) {
	            fail("Metacat Error:\n" + me.getMessage());
	        } catch (Exception e) {
	            fail("General exception:\n" + e.getMessage());
	        }
    	}
    }
    
    /*
     * Write id to a file for persistance
     */
    private void writeIdToFile(String fileName, String id) throws Exception
    {
    	File file = new File(fileName);
    	StringReader reader = new StringReader(id);
    	FileWriter writer = new FileWriter(file);
    	char [] buffer = new char[1024];
    	int c = reader.read(buffer);
    	while (c != -1)
    	{
    		writer.write(buffer, 0, c);
    		c = reader.read(buffer);
    	}
    	writer.close();
    	reader.close();
    }
    
    /*
     * Read id from a given file
     */
    private String readIdFromFile(String fileName) throws Exception
    {
    	File file = new File(fileName);
    	FileReader reader = new FileReader(file);
    	StringWriter writer = new StringWriter();
    	char [] buffer = new char[1024];
    	int c = reader.read(buffer);
    	while (c != -1)
    	{
    		writer.write(buffer, 0, c);
    		c = reader.read(buffer);
    	}
    	reader.close();
    	return writer.toString();
    }
    
    /*
     * Delete the given file
     */
    private void deleteFile(String fileName) throws Exception
    {
    	File file = new File(fileName);
    	file.delete();
    }
}
