package edu.ucsb.nceas.metacattest.restservice;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.SimpleTimeZone;
import java.util.TimeZone;
import java.util.Vector;

import junit.framework.Test;
import junit.framework.TestSuite;
import edu.ucsb.nceas.MCTestCase;
import edu.ucsb.nceas.metacat.IdentifierManager;
import edu.ucsb.nceas.metacat.client.DocumentNotFoundException;
import edu.ucsb.nceas.metacat.client.InsufficientKarmaException;
import edu.ucsb.nceas.metacat.client.MetacatAuthException;
import edu.ucsb.nceas.metacat.client.MetacatException;
import edu.ucsb.nceas.metacat.client.MetacatInaccessibleException;
import edu.ucsb.nceas.metacat.client.rest.MetacatRest;
import edu.ucsb.nceas.metacat.client.rest.MetacatRestClient;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.metacat.util.SystemUtil;
import edu.ucsb.nceas.utilities.IOUtil;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;

/**
 * A JUnit test for testing Step class processing.
 * 
 * This test is deprecated because the associated RestClient will be removed.
 */
@Deprecated
public class MetacatRestClientTest extends MCTestCase{
	protected static String contextUrl;
	static {
		try {
		    contextUrl = SystemUtil.getContextURL();
		    //System.out.println("The conext url is =========================="+contextUrl);
			username = PropertyService.getProperty("test.mcUser");
			password = PropertyService.getProperty("test.mcPassword");
			anotheruser = PropertyService.getProperty("test.mcAnotherUser");
			anotherpassword = PropertyService.getProperty("test.mcAnotherPassword");
		} catch (PropertyNotFoundException pnfe) {
			System.err.println("Could not get property in static block: " 
					+ pnfe.getMessage());
		}
	}
	
    private String failpass = "sdfsdfsdfsd";
    private MetacatRest m;
    private static final String DOC_TITLE = "Test MetacatRest service";    
    
    /**
     * Constructor to build the test
     *
     * @param name the name of the test method
     */
    public MetacatRestClientTest(String name)
    {
        super(name);
    }

    /**
     * Establish a testing framework by initializing appropriate objects
     */
    public void setUp()
    {
        System.out.println("contextUrl: " + contextUrl);
        m = new MetacatRestClient(contextUrl);
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
      suite.addTest(new MetacatRestClientTest("initialize"));
      
      // Commented out tests because these servlets are no longer configured in metacat
      // so they all need to be removed along with MetacatRestClient
//      suite.addTest(new MetacatRestClientTest("invalidLogin"));
//      suite.addTest(new MetacatRestClientTest("login"));
//      suite.addTest(new MetacatRestClientTest("logout"));
//      suite.addTest(new MetacatRestClientTest("get"));
//      suite.addTest(new MetacatRestClientTest("invalidGet"));
//      suite.addTest(new MetacatRestClientTest("getPrivate"));
//      suite.addTest(new MetacatRestClientTest("authenticatedGet"));
//      suite.addTest(new MetacatRestClientTest("query"));
//      suite.addTest(new MetacatRestClientTest("authenticatedQuery"));     
//      suite.addTest(new MetacatRestClientTest("crud"));
//      suite.addTest(new MetacatRestClientTest("delete"));
//      suite.addTest(new MetacatRestClientTest("getNextObject"));
//      suite.addTest(new MetacatRestClientTest("getNextRevision"));
//      suite.addTest(new MetacatRestClientTest("getAllDocids"));
//      suite.addTest(new MetacatRestClientTest("isRegistered"));
//      suite.addTest(new MetacatRestClientTest("addLSID"));
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
        debug("-------------------------------------------------------");
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
        debug("-------------------------------------------------------");
        // Try an invalid login
        String response = "";
        try {
        	response = m.login(username, failpass);        	
            fail("Authorization should have failed.");
        } catch (MetacatAuthException mae) {
        	debug("invalid login(): response=" + mae.getMessage());
            assertNotNull(mae);
        } catch (MetacatInaccessibleException mie) {
            fail("Metacat Inaccessible:\n" + mie.getMessage());
        } 
    }
    
    /**
     * Test the logout() function. 
     */
    public void logout()
    {
       debug("\nStarting logout test...");
       debug("-------------------------------------------------------");
       try {
            m.login(username, password);
            String response = m.logout();
            debug("logout(): Response ="+response);
            assertTrue(response.indexOf("<success>") == -1);
        } catch (MetacatAuthException mae) {
            fail("Authorization failed:\n" + mae.getMessage());
        } catch (MetacatInaccessibleException mie) {
            fail("Metacat Inaccessible:\n" + mie.getMessage());
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
     * Test the get() function with a public document
     */
    public void get()
    {
        debug("\nStarting get test...");
        debug("-------------------------------------------------------"); 
        try {
            String guid = insertTestDocument();
            Reader r =  m.getObject(guid, null);            
            String doc = IOUtil.getAsString(r, true);
            doc = doc +"\n";
            debug("document:\n"+doc);
            assertTrue(doc.indexOf(DOC_TITLE) != -1);
        } catch (MetacatInaccessibleException mie) {
            fail("Metacat Inaccessible:\n" + mie.getMessage());
        } catch (Exception e) {
            fail("General exception:\n" + e.getMessage());
        }
    }
    
    /**
     * Test the get() function with a private document
     */
    public void getPrivate()
    {
        assertTrue("Not implemented yet.", 1==1);
        /*
        debug("\nStarting getprivate  test...");
        debug("-------------------------------------------------------");        
        try {
            Reader r =  m.getObject(authorized_doc_id, null);            
            String doc = IOUtil.getAsString(r, true);
            doc = doc +"\n";
            debug("document:\n"+doc);
        } catch (MetacatInaccessibleException mie) {
            fail("Metacat Inaccessible:\n" + mie.getMessage());
        } catch (Exception e) {
            fail("General exception:\n" + e.getMessage());
        }
        */
    }
    
    /**
     * Test the authenticatedGet() function with a private document without for a valid session
     */
    public void authenticatedGet()
    {
        assertTrue("Not implemented yet.", 1==1);
        /*
        debug("\nStarting authorizedGet test...");
        debug("-------------------------------------------------------");
        try {
        	m.login(username,password);
            Reader r =  m.authenticatedGetObject(authorized_doc_id, null);            
            String doc = IOUtil.getAsString(r, true);
            doc = doc +"\n";
            debug("document:\n"+doc);
        } catch (MetacatInaccessibleException mie) {
            debug("Metacat Inaccessible:\n" + mie.getMessage());
        } catch (Exception e) {
            debug("General exception:\n" + e.getMessage());
        }
        */
    }
    
    /**
     * Test the get() function with a non existing document
     */
    public void invalidGet()
    {
        debug("\nStarting invalid get test ...");
        debug("-------------------------------------------------------");
        try {
            String unregDocid = generateDocumentId() + ".1";
            Reader r =  m.getObject(unregDocid, null);            
            String doc = IOUtil.getAsString(r, true);
            doc = doc +"\n";
            debug("document:\n"+doc);
            assertTrue(doc.indexOf("<error>") != -1);
        } catch (MetacatInaccessibleException mie) {
            debug("Metacat Inaccessible:\n" + mie.getMessage());
        } catch (DocumentNotFoundException doe) {
        	  debug("Document not found:\n" + doe.getMessage());
        } catch (Exception e) {
        	e.printStackTrace();
            debug("General exception:\n" + e.getMessage());
        }
    }
    
    /**
     * Test the query() function with a public session
     */
    public void query()
    {
        assertTrue("Not implemented yet.", 1==1);
        /*
    	debug("\nStarting query test ...");
        debug("-------------------------------------------------------");
    	try {
    	 FileReader fr = new FileReader(ecogridQueryFile);   	
    	 Reader r = m.query(fr);   
         String doc = IOUtil.getAsString(r, true);
         doc = doc +"\n";
         debug("document:\n"+doc);
         
        } catch (MetacatInaccessibleException mie) {
            fail("Metacat Inaccessible:\n" + mie.getMessage());
        } catch (Exception e) {
        	e.printStackTrace();
            fail("General exception:\n" + e.getMessage());
        }
        */
    }
    
    
    
    /**
     * Test the authenticatedQuery() function
     */
    public void authenticatedQuery()
    {
        assertTrue("Not implemented yet.", 1==1);
        /*
    	debug("\nStarting authenticatedQuery test ...");
        debug("-------------------------------------------------------");
    	try {
    	 FileReader fr = new FileReader(ecogridQueryFile);	
    	 m.login(username,password);
    	 Reader r = m.authenticatedQuery(fr);   
         String doc = IOUtil.getAsString(r, true);
         doc = doc +"\n";
         debug("document:\n"+doc);
         
        } catch (MetacatAuthException mae) {
            fail("Authorization failed:\n" + mae.getMessage());
        } catch (MetacatInaccessibleException mie) {
            fail("Metacat Inaccessible:\n" + mie.getMessage());
        } catch (Exception e) {
        	e.printStackTrace();
            fail("General exception:\n" + e.getMessage());
        }
        */
    }
    
    /**
     * Test the insert,update and delete function
     */
    public void crud()
    {
    	debug("\nStarting insert and update document test ...");
        debug("-------------------------------------------------------");
    	try {
    	    String accessBlock = getAccessBlock("public", true, true,
                    false, false, false);
            String emldoc = getTestEmlDoc("Test MetacatRest service", EML2_1_0, null,
                    null, "http://fake.example.com/somedata", null,
                    accessBlock, null, null,
                    null, null);
            String docid = generateDocumentId();
            debug("Generated id: " + docid);
    		StringReader sr = new StringReader(emldoc);	
    		m.login(username,password);

    		debug("\nFirst insert the document...");
    		String response = m.create(docid + ".1",sr);
    		sr.close();
    		debug("response:\n"+response);
            assertTrue(response.indexOf("success") != -1);
    		
            Thread.sleep(5000);

    		sr = new StringReader(emldoc);
    		debug("\nNow update the document...");
    		response = m.update(docid + ".2",sr);    	 
    		debug("response:\n"+response);
            assertTrue(response.indexOf("success") != -1);
    		sr.close();

    	} catch (MetacatAuthException mae) {
    		fail("Authorization failed:\n" + mae.getMessage());
    	} catch (MetacatInaccessibleException mie) {
    		fail("Metacat Inaccessible:\n" + mie.getMessage());
    	} catch (Exception e) {
    		fail("General exception:\n" + e.getMessage());
    	}
    }

    /**
     * Test the delete function
     */
    public void delete()
    {
        debug("\nStarting delete document test ...");
        debug("-------------------------------------------------------");
        try {
            String guid = insertTestDocument();
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                debug("Thread.sleep() failed to execute.");
            }

            debug("\nDelete the document...");
            String response = m.deleteObject(guid);  
            debug("response:\n"+response);
            assertTrue(response.indexOf("success") != -1);
            
        } catch (MetacatInaccessibleException mie) {
            fail("Metacat Inaccessible:\n" + mie.getMessage());
        } catch (InsufficientKarmaException e) {
            fail("not Authorized:\n" + e.getMessage());
        } catch (MetacatException e) {
            fail("Metacat error:\n" + e.getMessage());
        }
    }
    
    /**
     * Get the most recent document id for a given scope and be sure
     * that it matches the one we last inserted. Assumes this test is run
     * immediately following a successful insert() test.
     */
    public void getNextObject() 
    {
        debug("\nStarting getNextObject test...");
        debug("-------------------------------------------------------");
        try {

            String lastId = m.getNextObject(prefix);
            debug("getNextObject(): Last Id=" + lastId);

        } catch (MetacatException me) {
            fail("Metacat Error:\n" + me.getMessage());
        } catch (Exception e) {
            fail("General exception:\n" + e.getMessage());
        }
    }

    /**
     * Get the next revision for a given id. Assumes this test is run
     * immediately following a successful insert() test.
     */
    public void getNextRevision()
    {
        debug("\nStarting getNextRevision test...");
        debug("-------------------------------------------------------");
        try {
            IdentifierManager im = IdentifierManager.getInstance();
            String docid = insertTestDocument();
            int rev_id = m.getNextRevision(docid);
            debug("NextRevision number is " + rev_id);

        } catch (MetacatException me) {
            fail("Metacat Error:\n" + me.getMessage());
        } catch (Exception e) {
            fail("General exception:\n" + e.getMessage());
        }
    }
    
    
    /**
     * Test getAllDocids function
     */
    public void getAllDocids() 
    {
        debug("\nStarting getAllDocids test...");
        debug("-------------------------------------------------------");
        try {

            Vector<String> vector = m.getAllDocids(prefix);
            StringBuffer b = new StringBuffer();
            for (String doc_id:vector)
            	b.append(doc_id+"\n");
            debug("getAllDocids():\n " + b.toString());

        } catch (MetacatException me) {
            fail("Metacat Error:\n" + me.getMessage());
        } catch (Exception e) {
            fail("General exception:\n" + e.getMessage());
        }
    }
    
    /**
     * Test isRegistered function
     */
    public void isRegistered() 
    {
        debug("\nStarting isRegistered test...");
        debug("-------------------------------------------------------");
        try {
            IdentifierManager im = IdentifierManager.getInstance();
            String docid = insertTestDocument();
            boolean registered = m.isRegistered(docid);
            debug("isRegistered(): " + docid + ".1" +" is "+(registered?"":"not ")+"registered");
            assertTrue(registered);
            String unregDocid = generateDocumentId() + ".1";
            registered = m.isRegistered(unregDocid);
            debug("isRegistered(): " + unregDocid+" is "+(registered?"":"not ")+"registered");
            assertFalse(registered);
        } catch (MetacatException me) {
            fail("Metacat Error:\n" + me.getMessage());
        } catch (Exception e) {
            fail("General exception:\n" + e.getMessage());
        }
    }
    
    /**
     * Test addLSID function
     */
    public void addLSID() 
    {
    	  debug("\nStarting addLSID test...");
    	  debug("-------------------------------------------------------");
          try {
              String unregDocid = generateDocumentId() + ".1";
        	  String response =  m.addLSID(unregDocid);
        	  debug("response:\n"+response);
          } catch (MetacatException me) {
              fail("Metacat Error:\n" + me.getMessage());
          } catch (Exception e) {
              fail("General exception:\n" + e.getMessage());
          }
    }
    
    /** Insert a test document, returning the identifier that was used. */
    private String insertTestDocument()
    {
        String accessBlock = getAccessBlock("public", true, true,
                false, false, false);
        String emldoc = getTestEmlDoc(DOC_TITLE, EML2_1_0, null,
                null, "http://fake.example.com/somedata", null,
                accessBlock, null, null,
                null, null);
        //String docid = generateDocumentId() + ".1";
        String guid = "testid:" + generateTimeString();
        StringReader sr = new StringReader(emldoc);        
        String response;
        try {
            m.login(username,password);
            response = m.create(guid, sr);
            debug("response:\n"+response);
            assertTrue(response.indexOf("success") != -1);
        } catch (InsufficientKarmaException e) {
            fail(e.getMessage());
        } catch (MetacatException e) {
            fail(e.getMessage());
        } catch (IOException e) {
            fail(e.getMessage());
        } catch (MetacatInaccessibleException e) {
            fail(e.getMessage());
        } catch (MetacatAuthException e) {
            fail(e.getMessage());
        }
        return guid;
    }
    
    /** Generate a timestamp for use in IDs. */
    private String generateTimeString()
    {
        StringBuffer guid = new StringBuffer();

        // Create a calendar to get the date formatted properly
        String[] ids = TimeZone.getAvailableIDs(-8 * 60 * 60 * 1000);
        SimpleTimeZone pdt = new SimpleTimeZone(-8 * 60 * 60 * 1000, ids[0]);
        pdt.setStartRule(Calendar.APRIL, 1, Calendar.SUNDAY, 2 * 60 * 60 * 1000);
        pdt.setEndRule(Calendar.OCTOBER, -1, Calendar.SUNDAY, 2 * 60 * 60 * 1000);
        Calendar calendar = new GregorianCalendar(pdt);
        Date trialTime = new Date();
        calendar.setTime(trialTime);
        guid.append(calendar.get(Calendar.YEAR));
        guid.append(calendar.get(Calendar.DAY_OF_YEAR));
        guid.append(calendar.get(Calendar.HOUR_OF_DAY));
        guid.append(calendar.get(Calendar.MINUTE));
        guid.append(calendar.get(Calendar.SECOND));
        guid.append(calendar.get(Calendar.MILLISECOND));

        return guid.toString();
    }
}
