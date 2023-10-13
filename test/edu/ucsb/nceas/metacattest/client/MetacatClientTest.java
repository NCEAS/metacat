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
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;

import edu.ucsb.nceas.MCTestCase;
import edu.ucsb.nceas.metacat.client.DocumentNotFoundException;
import edu.ucsb.nceas.metacat.client.InsufficientKarmaException;
import edu.ucsb.nceas.metacat.client.Metacat;
import edu.ucsb.nceas.metacat.client.MetacatAuthException;
import edu.ucsb.nceas.metacat.client.MetacatClient;
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
    public static final String NOT_SUPPORT = "no longer supported";
    private static final String CAN_NOT_REACH = "It shoudln't reach here since " 
                                               + "the Metacat API is disabled";
    private String wrongMetacatUrl=
    	"http://localhostBAD/some/servlet/metacat";
  
    private static String username;
	private static String password;
	static {
		try {
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
    private MetacatClient m;
    private static final int TIME = 5;
    private static final String DOCID = "docid";
    private static final String DATAID = "dataid";
    
    /**
     * Constructor to build the test
     *
     * @param name the name of the test method
     */
    public MetacatClientTest(String name) {
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
    public void setUp() {
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
            Metacat metacat = MetacatFactory.createMetacatConnection(metacatUrl);
            m = (MetacatClient) metacat;
        } catch (MetacatInaccessibleException mie) {
            System.err.println("Metacat is: " + metacatUrl);
            fail("Metacat connection failed." + mie.getMessage());
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
      suite.addTest(new MetacatClientTest("initialize"));
      suite.addTest(new MetacatClientTest("invalidLogin"));
      suite.addTest(new MetacatClientTest("getloggedinuserinfo"));
      suite.addTest(new MetacatClientTest("logout"));
      suite.addTest(new MetacatClientTest("validateSession"));
      suite.addTest(new MetacatClientTest("isAuthorized"));
      suite.addTest(new MetacatClientTest("readInlineData"));
      suite.addTest(new MetacatClientTest("setAccess1"));
      suite.addTest(new MetacatClientTest("setAccess2"));
      suite.addTest(new MetacatClientTest("getAllDocids"));
      suite.addTest(new MetacatClientTest("isRegistered"));
      suite.addTest(new MetacatClientTest("login"));
      suite.addTest(new MetacatClientTest("insert"));
      suite.addTest(new MetacatClientTest("getLastDocid"));
      suite.addTest(new MetacatClientTest("getNewestDocRevision"));
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
      suite.addTest(new MetacatClientTest("getAccessControl"));
      suite.addTest(new MetacatClientTest("getAccessControlFromDocWithoutAccess"));
      return suite;
  }

    /**
     * Run an initial test that always passes to check that the test
     * harness is working.
     */
    public void initialize() {
        assertTrue(1 == 1);
    }

    /**
     * Test the login() function with valid credentials
     */
    public void login() {
        debug("\nStarting login test...");
     // Try a valid login
        try {
            String response = m.login(username, password);
            fail("Authorization should have failed since it is disabled.");
        } catch (MetacatAuthException mae) {
            assertTrue(1 == 1);
        } catch (MetacatInaccessibleException mie) {
            fail("Metacat Inaccessible:\n" + mie.getMessage());
        }
    }

    /**
     * Test the login() function with INVALID credentials
     */
    public void invalidLogin() {
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
     * Test the getloggedinuserinfo method
     * @throws Exception
     */
    public void getloggedinuserinfo() throws Exception {
        String response = m.getloggedinuserinfo();
        evaluateResponse(response);
    }
    

    /**
     * Test the logout() function. When logout, user will be public, it couldn't
     * insert a document.
     */
    public void logout() {
       debug("\nStarting logoutAndInvalidInsert test...");
       try {
            m.logout();
            fail(CAN_NOT_REACH);
        } catch (MetacatInaccessibleException mie) {
            fail("Metacat Inaccessible:\n" + mie.getMessage());
        } catch (MetacatException me) {
            evaluateResponse(me.getMessage());
        } catch (Exception e) {
            fail("General exception:\n" + e.getMessage());
        }
    }
    
    /**
     * Test the validateSession method
     * @throws MetacatInaccessibleException 
     * @throws Exception
     */
    public void validateSession() throws MetacatInaccessibleException {
        String session="s34swciere";
        try {
            m.validateSession(session);
            fail(CAN_NOT_REACH);
        } catch (MetacatAuthException e) {
            evaluateResponse(e.getMessage());
        }
    }
    
    /**
     * Test the isAuthorizaed method
     * @throws Exception
     */
    public void isAuthorized() throws MetacatInaccessibleException {
        String resourceId = "foo";
        String session="s34swciere";
        String permission = "read";
        try {
            m.isAuthorized(resourceId, permission, session);
            fail(CAN_NOT_REACH);
        } catch (MetacatAuthException  e) {
            evaluateResponse(e.getMessage());
        }
    }

    /**
     * Test the read() function with a known document
     * @throws Exception 
     */
    public void read() throws Exception {
        debug("\nStarting read test...");
        try {
            String docid = readIdFromFile(DOCID);
            debug("docid=" + docid);
            m.read(docid+".1");
            fail(CAN_NOT_REACH);
        } catch (MetacatAuthException mae) {
            fail("Authorization failed:\n" + mae.getMessage());
        } catch (MetacatInaccessibleException mie) {
            fail("Metacat Inaccessible:\n" + mie.getMessage());
        } catch (MetacatException e) {
            evaluateResponse(e.getMessage());
        }
    }

    /**
     * A user try to read a document which it doesn't have read permission
     */
    public void invalidRead() {
        debug("\nStarting invalidRead test...");
        try {
            String docid = readIdFromFile(DOCID);
            m.read(docid + ".1");
            fail(CAN_NOT_REACH);
        } catch (MetacatAuthException mae) {
            fail("Authorization failed:\n" + mae.getMessage());
        } catch (MetacatInaccessibleException mie) {
            fail("Metacat Inaccessible:\n" + mie.getMessage());
        } catch (InsufficientKarmaException ike) {
            fail("Not to have a permissible:\n" + ike.getMessage());
        } catch (MetacatException e) {
            evaluateResponse(e.getMessage());
        } catch (IOException ioe) {
            fail("IO exception:\n" + ioe.getMessage());
        } catch (DocumentNotFoundException ne) {
            fail("DocumentNotFound exception:\n" + ne.getMessage());
        } catch(Exception e) {
            fail("Exception:\n" + e.getMessage());
        }
    }
    
    /**
     * Test the readInlineData method
     * @throws Exception
     */
    public void readInlineData() throws MetacatInaccessibleException,
                                        InsufficientKarmaException {
        String inlineDataId = "inlinedata100";
        try {
            m.readInlineData(inlineDataId);
            fail(CAN_NOT_REACH);
        } catch (MetacatException e) {
            evaluateResponse(e.getMessage());
        }
    }

    /**
     * Test the query() function with a known document
     */
    public void query() {
        debug("\nStarting query test...");
        try {
            FileReader fr = new FileReader(queryFile);
            Reader r = m.query(fr);
            String result = IOUtil.getAsString(r, true);
            debug("query(): Query result=\n" + result);
            evaluateResponse(result);
        } catch (MetacatInaccessibleException mie) {
            fail("Metacat Inaccessible:\n" + mie.getMessage());
        } catch (Exception e) {
            fail("General exception:\n" + e.getMessage());
        }
    }
    /**
     * Test the query() function with a known document
     */
    public void queryWithQformat() {
        debug("\nStarting queryWithQformat test...");
        try {
            FileReader fr = new FileReader(queryFile);
            String qformat = "knb";
            Reader r = m.query(fr, qformat);
            String result = IOUtil.getAsString(r, true);
            evaluateResponse(result);
        } catch (MetacatInaccessibleException mie) {
            fail("Metacat Inaccessible:\n" + mie.getMessage());
        } catch (Exception e) {
            fail("General exception:\n" + e.getMessage());
        }
    }

    /**
     * Test the insert() function with a known document
     */
    public void insert() {
        debug("\nStarting insert test...");
        try {
            String identifier = newdocid + ".1";
            //write newdocid into file for persistance
            writeIdToFile(DOCID, newdocid);
            String response = m.insert(identifier,
                    new StringReader(testdocument), null);
            fail(CAN_NOT_REACH);
        } catch (MetacatAuthException mae) {
            fail("Authorization failed:\n" + mae.getMessage());
        } catch (MetacatInaccessibleException mie) {
            fail("Metacat Inaccessible:\n" + mie.getMessage());
        } catch (InsufficientKarmaException ike) {
            fail("Insufficient karma:\n" + ike.getMessage());
        } catch (MetacatException me) {
            evaluateResponse(me.getMessage());
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
            String response = m.getAccessControl(identifier);
            fail(CAN_NOT_REACH);
        } catch (MetacatInaccessibleException mie) {
            fail("Metacat Inaccessible:\n" + mie.getMessage());
        } catch (InsufficientKarmaException ike) {
            fail("Insufficient karma:\n" + ike.getMessage());
        } catch (MetacatException me) {
            evaluateResponse(me.getMessage());
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
        String response = m.getAccessControl(identifier);
        fail(CAN_NOT_REACH);
    } catch (MetacatInaccessibleException mie) {
        fail("Metacat Inaccessible:\n" + mie.getMessage());
    } catch (InsufficientKarmaException ike) {
        fail("Insufficient karma:\n" + ike.getMessage());
    } catch (MetacatException me) {
        evaluateResponse(me.getMessage());
    } catch (Exception e) {
        fail("General exception:\n" + e.getMessage());
    }
  }
    
    /**
     * Test the setAccess method
     * @throws Exception
     */
    public void setAccess1() throws InsufficientKarmaException, 
                                    MetacatInaccessibleException {
        String docid = "foo.1.1";
        String principal = "public";
        String permission = "read";
        String perType = "allow";
        String perOrder = "allowFirst";
        try {
            m.setAccess(docid, principal, permission, perType, perOrder);
            fail(CAN_NOT_REACH);
        } catch (MetacatException e) {
            evaluateResponse(e.getMessage());
        }
    }
    
    /**
     * Test the setAccess method
     * @throws MetacatInaccessibleException 
     * @throws MetacatException 
     * @throws Exception
     */
    public void setAccess2() throws InsufficientKarmaException, 
                                    MetacatInaccessibleException {
        String accessBlock = "<access order=\"allowFirst\" "
                + "authSystem=\"knb\">\n"
                + "        <allow>\n"
                + "            <principal>public</principal>\n"
                + "            <permission>read</permission>\n"
                + "        </allow>\n"
                + "    </access>";
        String docid = "foo.2.1";
        try {
            m.setAccess(docid, accessBlock);
            fail(CAN_NOT_REACH);
        } catch (MetacatException e) {
            evaluateResponse(e.getMessage());
        }
    }

    /**
     * Test the upload() function with a data file.
     * 1. Insert version 1 successfully.
     * 2. Update version 2 successfully
     * 3. Update version 2 again and should be failed.
     */
    public void upload() {
        debug("\nStarting upload test...");
        try {
            newdocid = generateDocumentId();
            String identifier = newdocid + ".1";
            writeIdToFile(DATAID, newdocid);
            String response = m.upload(identifier,
                                     new File(onlinetestdatafile));
            fail(CAN_NOT_REACH);
        } catch (MetacatAuthException mae) {
            fail("Authorization failed:\n" + mae.getMessage());
        } catch (MetacatInaccessibleException mie) {
            fail("Metacat Inaccessible:\n" + mie.getMessage());
        } catch (InsufficientKarmaException ike) {
            fail("Insufficient karma:\n" + ike.getMessage());
        } catch (MetacatException me) {
            evaluateResponse(me.getMessage());
        } catch (Exception e) {
            fail("General exception:\n" + e.getMessage());
        }
    }
    


    /**
     * Test the upload() function by passing an InputStream
     */
    public void upload_stream() {
        debug("\nStarting upload_stream test...");
        try {
            newdocid = generateDocumentId();
            String identifier = newdocid + ".1";
            File testFile = new File(onlinetestdatafile);
            String response = m.upload(identifier, "onlineDataFile1",
                                       new FileInputStream(testFile),
                                       (int) testFile.length());
            fail(CAN_NOT_REACH);
        } catch (MetacatInaccessibleException mie) {
            fail("Metacat Inaccessible:\n" + mie.getMessage());
        } catch (InsufficientKarmaException ike) {
            fail("Insufficient karma:\n" + ike.getMessage());
        } catch (MetacatException me) {
            evaluateResponse(me.getMessage());
        } catch (Exception e) {
            fail("General exception:\n" + e.getMessage());
        }
    }
    
    /**
     * Test the upload() function by passing an InputStream
     */
    public void upload_stream_chunked() {
        debug("\nStarting upload_stream_chunked test...");
        try {
            newdocid = generateDocumentId();
            String identifier = newdocid + ".1";
            File testFile = new File(onlinetestdatafile);
            String response = m.upload(identifier, "onlineDataFile1",
                                       new FileInputStream(testFile), -1);
            fail(CAN_NOT_REACH);
        } catch (MetacatInaccessibleException mie) {
            fail("Metacat Inaccessible:\n" + mie.getMessage());
        } catch (InsufficientKarmaException ike) {
            fail("Insufficient karma:\n" + ike.getMessage());
        } catch (MetacatException me) {
            evaluateResponse(me.getMessage());
        } catch (Exception e) {
            fail("General exception:\n" + e.getMessage());
        }
    }

    /**
     * Test the invalidUpdate() function. A user try to update a document
     * which it doesn't have permission
     */
    public void invalidUpdate() {
        debug("\nStarting invalidUpdate test...");
        try {
        	String docid = readIdFromFile(DOCID);
            String identifier = docid + ".2";
            String response = m.update(identifier,
                    new StringReader(testdocument), null);
             fail(CAN_NOT_REACH);
        } catch (MetacatAuthException mae) {
            fail("Authorization failed:\n" + mae.getMessage());
        } catch (MetacatInaccessibleException mie) {
            fail("Metacat Inaccessible:\n" + mie.getMessage());
        } catch (InsufficientKarmaException ike) {
            fail("Insufficient karma:\n" + ike.getMessage());
        } catch (MetacatException me) {
            evaluateResponse(me.getMessage());
        } catch (Exception e) {
            fail("General exception:\n" + e.getMessage());
        }
    }

    /**
     * Test the update() function with a known document
     */
    public void update() {
        debug("\nStarting update test...");
        try {
        	String docid = readIdFromFile(DOCID);
            String identifier = docid + ".2";
            String response = m.update(identifier,
                    new StringReader(testdocument), null);
            fail(CAN_NOT_REACH);
        } catch (MetacatAuthException mae) {
            fail("Authorization failed:\n" + mae.getMessage());
        } catch (MetacatInaccessibleException mie) {
            fail("Metacat Inaccessible:\n" + mie.getMessage());
        } catch (InsufficientKarmaException ike) {
            fail("Insufficient karma:\n" + ike.getMessage());
        } catch (MetacatException me) {
            evaluateResponse(me.getMessage());
        } catch (Exception e) {
            fail("General exception:\n" + e.getMessage());
        }
    }

    /**
     * A user try delete a document which it doesn't have permission
     */
    public void invalidDelete() {
        debug("\nStarting invalidDelete test...");
        try {
            String identifier = newdocid + ".2";
            String response = m.delete(identifier);
            fail(CAN_NOT_REACH);
        } catch (MetacatInaccessibleException mie) {
            fail("Metacat Inaccessible:\n" + mie.getMessage());
        } catch (InsufficientKarmaException ike) {
            fail("Insufficient karma:\n" + ike.getMessage());
        } catch (MetacatException me) {
            evaluateResponse(me.getMessage());
        } catch (Exception e) {
            fail("General exception:\n" + e.getMessage());
        }
    }

    /**
     * Test the delete() function with a known document
     */
    public void delete() {
        debug("\nStarting delete test...");
        try {
        	Thread.sleep(10000);
        	String docid = readIdFromFile(DOCID);
            String identifier = docid + ".2";
            String response = m.delete(identifier);
            fail(CAN_NOT_REACH);
        } catch (MetacatAuthException mae) {
            fail("Authorization failed:\n" + mae.getMessage());
        } catch (MetacatInaccessibleException mie) {
            fail("Metacat Inaccessible:\n" + mie.getMessage());
        } catch (InsufficientKarmaException ike) {
            fail("Insufficient karma:\n" + ike.getMessage());
        } catch (MetacatException me) {
            evaluateResponse(me.getMessage());
        } catch (Exception e) {
            fail("General exception:\n" + e.getMessage());
        }
    }

    /**
     * Test the to connect a wrong metacat url. Create a new metacat with a
     * inaccessible url. Then try to login it. If get a MetacatInaccessible
     * exception, this is right.
     */
    public void inaccessibleMetacat() {
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
    * Get the most recent document id for a given scope and be sure
    * that it matches the one we last inserted. Assumes this test is run
    * immediately following a successful insert() test, AND that the docid
    * inserted has the maximal numerical value.
    */
   public void getLastDocid() {
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
           fail(CAN_NOT_REACH);
       } catch (MetacatException me) {
           evaluateResponse(me.getMessage());
       } catch (Exception e) {
           fail("General exception:\n" + e.getMessage());
       }
   }

   /**
    * Try to perform an action that requires a session to be valid without
    * having logged in first, but use an invalid session identifier.
    * Then insert a document, which should fail.
    */
   public void getNewestDocRevision() {
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
           fail(CAN_NOT_REACH);
       } catch (MetacatException me) {
           evaluateResponse(me.getMessage());
       } catch (Exception e) {
           fail("General exception:\n" + e.getMessage());
       }
   }
   
   /**
    * Test the getAllDocids method
    * @throws Exception
    */
   public void getAllDocids() throws Exception {
       String scope = "tao";
       try {
           m.getAllDocids(scope);
           fail(CAN_NOT_REACH);
       } catch (MetacatException e) {
           evaluateResponse(e.getMessage());
       }
   }
   
   /**
    * Test the isRegistered method
    * @throws Exception
    */
   public void isRegistered() {
       String docid = "tao.1.1";
       try {
           m.isRegistered(docid);
           fail(CAN_NOT_REACH);
       } catch (MetacatException e) {
           evaluateResponse(e.getMessage());
       }
   }
   
 
    /*
     * Write id to a file for persistance
     */
    private void writeIdToFile(String fileName, String id) throws Exception {
        File file = new File(fileName);
        StringReader reader = new StringReader(id);
        FileWriter writer = new FileWriter(file);
        char [] buffer = new char[1024];
        int c = reader.read(buffer);
        while (c != -1) {
            writer.write(buffer, 0, c);
            c = reader.read(buffer);
        }
    	writer.close();
    	reader.close();
    }
    
    /*
     * Read id from a given file
     */
    private String readIdFromFile(String fileName) throws Exception {
        File file = new File(fileName);
        FileReader reader = new FileReader(file);
        StringWriter writer = new StringWriter();
        char [] buffer = new char[1024];
        int c = reader.read(buffer);
        while (c != -1) {
            writer.write(buffer, 0, c);
            c = reader.read(buffer);
        }
        reader.close();
        return writer.toString();
    }
    
    
    /**
     * If the response message doesn't have a error message about disabling the
     * Metacat API, it will throw an error. 
     * @param response  the response will be evaluated. 
     */
    private void evaluateResponse(String response) {
        if (response.indexOf("<error>") == -1 ||
                response.indexOf(NOT_SUPPORT) == -1) {
            fail("The Metacat API should be disabled");
        }
    }
}
