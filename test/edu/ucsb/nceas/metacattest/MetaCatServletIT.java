package edu.ucsb.nceas.metacattest;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.List;
import java.util.Properties;

import junit.framework.Test;
import junit.framework.TestSuite;
import edu.ucsb.nceas.MCTestCase;
import edu.ucsb.nceas.metacat.IdentifierManager;
import edu.ucsb.nceas.metacat.client.MetacatClient;
import edu.ucsb.nceas.metacat.client.MetacatFactory;
import edu.ucsb.nceas.metacat.client.MetacatInaccessibleException;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;


/**
 * A JUnit test for testing Step class processing
 */
public class MetaCatServletIT extends MCTestCase {
    private MetacatClient metacat = null;
    private String serialNumber;


    /**
     * Constructor to build the test
     *
     * @param name
     *            the name of the test method
     */
    public MetaCatServletIT(String name) {
        super(name);
    }

    /**
     * Constructor to build the test
     *
     * @param name
     *            the name of the test method
     */
    public MetaCatServletIT(String name, String serial) {
        super(name);
        serialNumber = serial;
    }

    /**
     * Establish a testing framework by initializing appropriate objects
     */
    public void setUp() {
        try {
            metacat = (MetacatClient) MetacatFactory.createMetacatConnection(metacatUrl);
        } catch (MetacatInaccessibleException e) {
            fail("Could not initialize MetacatClient: " + e.getMessage());
            e.printStackTrace();
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
        double number = 0;
        String serial = null;

        TestSuite suite = new TestSuite();
        suite.addTest(new MetaCatServletIT("initialize"));
        suite.addTest(new MetaCatServletIT("testOtherReferralLogin"));
        suite.addTest(new MetaCatServletIT("testOtherReferralLoginFail"));
        suite.addTest(new MetaCatServletIT("testNCEASLoginFail"));
        // Should put a login successfully at the end of login test
        // So insert or update can have cookie.
        suite.addTest(new MetaCatServletIT("testNCEASLogin"));

        // create random number for docid, so it can void repeat
        number = Math.random() * 100000;
        serial = Integer.toString(((new Double(number)).intValue()));
        debug("serial: " + serial);
        suite.addTest(new MetaCatServletIT("testInsertXMLDocument", serial));
        suite.addTest(new MetaCatServletIT("testReadXMLDocumentXMLFormat", serial));
        suite.addTest(new MetaCatServletIT("testUpdateXMLDocument", serial));

        suite.addTest(new MetaCatServletIT("testReadXMLDocumentHTMLFormat", serial));
        suite.addTest(new MetaCatServletIT("testReadXMLDocumentZipFormat", serial));

        suite.addTest(new MetaCatServletIT("testDeleteXMLDocument", serial));

        // insert invalid xml document
        number = Math.random() * 100000;
        serial = Integer.toString(((new Double(number)).intValue()));
        suite.addTest(new MetaCatServletIT("testInsertInvalidateXMLDocument", serial));
        // insert non-well-formed document
        number = Math.random() * 100000;
        serial = Integer.toString(((new Double(number)).intValue()));
        suite
                .addTest(new MetaCatServletIT("testInsertNonWellFormedXMLDocument",
                                              serial));

        suite.addTest(new MetaCatServletIT("testLogOut"));

        suite.addTest(new MetaCatServletIT("testReindexFail"));


        return suite;
    }

    /**
     * Run an initial test that always passes to check that the test harness is
     * working.
     */
    public void initialize() {
        assertTrue(1 == 1);
    }

    /**
     * Test the login to nceas successfully
     */
    public void testNCEASLogin() {
        debug("\nRunning: testNCEASLogin test");
        try {
            String user = PropertyService.getProperty("test.mcUser");
            String passwd = PropertyService.getProperty("test.mcPassword");
            assertTrue(logIn(user, passwd));
            this.testLogOut();

        } catch (PropertyNotFoundException pnfe) {
            fail("Could not find property: " + pnfe.getMessage());
        }
    }

    /**
     * Test the login to nceas failed
     */
    public void testNCEASLoginFail() {
        debug("\nRunning: testNCEASLoginFail test");
        try {
            String user = PropertyService.getProperty("test.mcUser");
            String passwd = "BogusPasswordShouldFail";
            assertTrue(!logIn(user, passwd));
            this.testLogOut();

        } catch (PropertyNotFoundException pnfe) {
            fail("Could not find property: " + pnfe.getMessage());
        }
    }

    /**
     * Test the login to Other successfully
     */
    public void testOtherReferralLogin() {
        debug("\nRunning: testOtherReferralLogin test");
        String user = null;
        String passwd = null;
        try {
            user = PropertyService.getProperty("test.referralUser");
            passwd = PropertyService.getProperty("test.referralPassword");
        } catch (PropertyNotFoundException pnfe) {
            fail("Could not find property: " + pnfe.getMessage());
        }
        debug("logging in Other user: " + user + ":" + passwd);
        assertTrue(logIn(user, passwd));
        // assertTrue( withProtocol.getProtocol().equals("http"));
        this.testLogOut();

    }

    /**
     * Test the login to Other failed
     */
    public void testOtherReferralLoginFail() {
        debug("\nRunning: testOtherReferralLoginFail test");
        String user = null;
    String passwd = "wrong";
    try {
      user = PropertyService.getProperty("test.referralUser");
    } catch (PropertyNotFoundException pnfe) {
      fail("Could not find property: " + pnfe.getMessage());
    }
        assertTrue(!logIn(user, passwd));
        // assertTrue( withProtocol.getProtocol().equals("http"));
        this.testLogOut();

    }



    /**
     * Test insert a xml document successfully
     */
    public void testInsertXMLDocument() {
        debug("\nRunning: testInsertXMLDocument test");
        String name = null;
        try {
            String user = PropertyService.getProperty("test.mcUser");
            String passwd = PropertyService.getProperty("test.mcPassword");
            metacat.login(user, passwd);

            name = "john" + PropertyService.getProperty("document.accNumSeparator") + serialNumber
                + PropertyService.getProperty("document.accNumSeparator")
                    + "1";
            debug("insert docid: " + name);
            String content =
                "<?xml version=\"1.0\"?>" + "<!DOCTYPE acl PUBLIC \"-//ecoinformatics.org//"
                    + "eml-access-2.0.0beta6//EN\" "
                    + "\"http://pine.nceas.ucsb.edu:8080/tao/dtd/eml-access-2.0.0beta6.dtd\">"
                    + "<acl authSystem=\"knb\" order=\"allowFirst\">" + "<identifier>" + name
                    + "</identifier>" + "<allow>" + "<principal>" + user + "</principal>"
                    + "<permission>all</permission>" + "</allow>" + "<allow>"
                    + "<principal>public</principal>" + "<permission>read</permission>" + "</allow>"
                    + "</acl>";
            debug("xml document: " + content);
            assertTrue(handleXMLDocument(content, name, "insert"));
            metacat.logout();

        } catch (PropertyNotFoundException pnfe) {
            fail("Could not find property: " + pnfe.getMessage());
        } catch (Exception e) {
            // TODO Auto-generated catch block
            fail(e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Test insert an invalidate xml document successfully In the String, there
     * is no <!Doctype ... Public/System/>
     */
    public void testInsertInvalidateXMLDocument() {
        debug("\nRunning: testInsertInvalidateXMLDocument test");
        String name = null;
        try {
            String user = PropertyService.getProperty("test.mcUser");
            String passwd = PropertyService.getProperty("test.mcPassword");
            metacat.login(user, passwd);

            name = "john" + PropertyService.getProperty("document.accNumSeparator") + serialNumber
                + PropertyService.getProperty("document.accNumSeparator") + "1";
            debug("insert docid: " + name);
            String content =
                "<?xml version=\"1.0\"?>" + "<acl authSystem=\"knb\" order=\"allowFirst\">"
                    + "<identifier>" + name + "</identifier>" + "<allow>"
                    + "<principal>" + user + "</principal>" + "<permission>all</permission>"
                    + "</allow>" + "<allow>" + "<principal>public</principal>"
                    + "<permission>read</permission>" + "</allow>" + "</acl>";
            debug("xml document: " + content);
            assertTrue(handleXMLDocument(content, name, "insert"));
            metacat.logout();

        } catch (Exception pnfe) {
            fail(pnfe.getMessage());
        }
    }

    /**
     * Test insert a non-well-formed xml document successfully There is no
     * </acl> in this string
     */
    public void testInsertNonWellFormedXMLDocument() {
        debug("\nRunning: testInsertNonWellFormedXMLDocument test");
        String name = null;
        try {
            String user = PropertyService.getProperty("test.mcUser");
            String passwd = PropertyService.getProperty("test.mcPassword");
            metacat.login(user, passwd);

            name = "john" + PropertyService.getProperty("document.accNumSeparator") + serialNumber
                + PropertyService.getProperty("document.accNumSeparator") + "1";
            debug("insert non well-formed docid: " + name);
            String content =
                "<?xml version=\"1.0\"?>" + "<acl authSystem=\"knb\" order=\"allowFirst\">"
                    + "<identifier>" + name + "</identifier>" + "<allow>" + "<principal>" + user
                    + "</principal>" + "<permission>all</permission>" + "</allow>" + "<allow>"
                    + "<principal>public</principal>" + "<permission>read</permission>"
                    + "</allow>";

            debug("xml document: " + content);
            assertTrue(!handleXMLDocument(content, name, "insert"));
            this.testLogOut();

        } catch (Exception pnfe) {
            fail(pnfe.getMessage());
        }
    }

    /**
     * Test read a xml document in xml format successfully
     */
    public void testReadXMLDocumentXMLFormat() {
        debug("\nRunning: testReadXMLDocumentXMLFormat test");
        String name = null;
        try {
            String user = PropertyService.getProperty("test.mcUser");
            String passwd = PropertyService.getProperty("test.mcPassword");
            metacat.login(user, passwd);
            name = "john" + PropertyService.getProperty("document.accNumSeparator")
                    + serialNumber
                    + PropertyService.getProperty("document.accNumSeparator") + "1";
            assertTrue(handleReadAction(name, "xml"));
            metacat.logout();
        } catch (Exception pnfe) {
            fail("Could not find property: " + pnfe.getMessage());
        }

    }

    /**
     * Test read a xml document in html format successfully
     */
    public void testReadXMLDocumentHTMLFormat() {
        debug("\nRunning: testReadXMLDocumentHTMLFormat test");
        String name = null;
        try {
            String user = PropertyService.getProperty("test.mcUser");
            String passwd = PropertyService.getProperty("test.mcPassword");
            metacat.login(user, passwd);
            name = "john" + PropertyService.getProperty("document.accNumSeparator")
                    + serialNumber
                    + PropertyService.getProperty("document.accNumSeparator") + "1";
            assertTrue(handleReadAction(name, "html"));
            metacat.logout();
        } catch (Exception pnfe) {
            fail(pnfe.getMessage());
        }

    }

    /**
     * Test read a xml document in zip format successfully
     */
    public void testReadXMLDocumentZipFormat() {
        debug("\nRunning: testReadXMLDocumentZipFormat test");
        String name = null;
        try {
            String user = PropertyService.getProperty("test.mcUser");
            String passwd = PropertyService.getProperty("test.mcPassword");
            metacat.login(user, passwd);
            name = "john" + PropertyService.getProperty("document.accNumSeparator")
                    + serialNumber
                    + PropertyService.getProperty("document.accNumSeparator") + "1";
            assertTrue(handleReadAction(name, "zip"));
            metacat.logout();
        } catch (Exception pnfe) {
            fail(pnfe.getMessage());
        }

    }

    /**
     * Test insert a xml document successfully
     */
    public void testUpdateXMLDocument() {
        debug("\nRunning: testUpdateXMLDocument test");
        String name = null;
        try {
            String user = PropertyService.getProperty("test.mcUser");
            String passwd = PropertyService.getProperty("test.mcPassword");

            metacat.login(user, passwd);

            name = "john" + PropertyService.getProperty("document.accNumSeparator") + serialNumber
                + PropertyService.getProperty("document.accNumSeparator") + "2";
            debug("update docid: " + name);
            String content =
                "<?xml version=\"1.0\"?>" + "<!DOCTYPE acl PUBLIC \"-//ecoinformatics.org//"
                    + "eml-access-2.0.0beta6//EN\" \"http://pine.nceas.ucsb."
                    + "edu:8080/tao/dtd/eml-access-2.0.0beta6.dtd\">"
                    + "<acl authSystem=\"knb\" order=\"allowFirst\">" + "<identifier>" + name
                    + "</identifier>" + "<allow>" + "<principal>" + user + "</principal>"
                    + "<permission>all</permission>" + "</allow>" + "<allow>"
                    + "<principal>public</principal>" + "<permission>read</permission>" + "</allow>"
                    + "</acl>";
            debug("xml document: " + content);
            assertTrue(handleXMLDocument(content, name, "update"));
            this.testLogOut();
        } catch (Exception pnfe) {
            fail(pnfe.getMessage());
        }
    }

    /**
     * Test delete a xml document successfully
     */
    public void testDeleteXMLDocument() {
        debug("\nRunning: testDeleteXMLDocument test");

        String name = null;
        try {
            String user = PropertyService.getProperty("test.mcUser");
            String passwd = PropertyService.getProperty("test.mcPassword");
            metacat.login(user, passwd);

            name = "john" + PropertyService.getProperty("document.accNumSeparator")
                    + serialNumber
                    + PropertyService.getProperty("document.accNumSeparator") + "2";
            debug("delete docid: " + name);
            assertTrue(handleDeleteFile(name));
            metacat.logout();

        } catch (Exception pnfe) {
            fail(pnfe.getMessage());
        }


    }


    /**
     * Test logout action
     */
    public void testLogOut() {
        debug("\nRunning: testLogOut test");
        assertTrue(handleLogOut());

    }

    public void testReindexFail() {

        // find a pid to reindex
        String identifier = null;
        List<String> ids = IdentifierManager.getInstance().getAllSystemMetadataGUIDs();
        if (ids != null && !ids.isEmpty()) {
            identifier = ids.get(0);
        }
        Properties prop = new Properties();
        prop.put("action", "reindex");
        prop.put("pid", identifier);

        String message = getMetacatString(prop);
        debug("Reindex Message: " + message);
        if (message.indexOf("<error>") != -1) {// there was an error
            assertTrue(true);
        } else if (message.indexOf("<success>") != -1) {
            fail("Unauthenticated user should not be able to invoke this action: " + message);
        } else {// something weird happened.
            fail("There was an unexpected error reindexing pid: " + message);
        }
    }

    /**
     * Method to handle login action
     *
     * @param userName,
     *            the DN name of the test method
     * @param passWord,
     *            the passwd of the user
     */

    public boolean logIn(String userName, String passWord) {
        Properties prop = new Properties();
        prop.put("action", "login");
        prop.put("qformat", "xml");
        prop.put("username", userName);
        prop.put("password", passWord);

        // Now contact metacat
        String response = getMetacatString(prop);
        debug("Login Message: " + response);
        boolean connected = false;
        if (response != null && response.indexOf("<login>") != -1) {
            connected = true;
        } else {

            connected = false;
        }

        return connected;
    }

    /**
     * Method to handle logout action
     */
    public boolean handleLogOut() {
        boolean disConnected = false;
        Properties prop = new Properties();
        prop.put("action", "logout");
        prop.put("qformat", "xml");

        String response = getMetacatString(prop);
        debug("Logout Message: " + response);

        if (response.indexOf("<logout>") != -1) {
            disConnected = true;
        } else {
            disConnected = false;
        }

        return disConnected;
    }

    /**
     * Method to handle read both xml and data file
     *
     * @param docid,
     *            the docid of the document want to read
     * @param qformat,
     *            the format of document user want to get
     */
    public boolean handleReadAction(String docid, String qformat) {
        Properties prop = new Properties();
        String message = "";
        prop.put("action", "read");
        prop.put("qformat", qformat);
        prop.put("docid", docid);

        message = getMetacatString(prop);
        message = message.trim();
        if (message == null || message.equals("") || message.indexOf("<error>") != -1) {// there
                                                                                        // was
                                                                                        // an
                                                                                        // error

            return false;
        } else {// successfully
            return true;
        }

    }

    /**
     * Method to handle inset or update xml document
     *
     * @param xmlDocument,
     *            the content of xml qformat
     * @param docid,
     *            the docid of the document
     * @param action,
     *            insert or update
     */
    public boolean handleXMLDocument(String xmlDocument, String docid, String action)

    { // -attempt to write file to metacat
        String access = "no";
        StringBuffer fileText = new StringBuffer();
        StringBuffer messageBuf = new StringBuffer();
        String accessFileId = null;
        Properties prop = new Properties();
        prop.put("action", action);
        prop.put("public", access); // This is the old way of controlling access
        prop.put("doctext", xmlDocument);
        prop.put("docid", docid);

        String message = getMetacatString(prop);
        debug("Insert or Update Message: " + message);
        if (message.indexOf("<error>") != -1) {// there was an error

            return false;
        } else if (message.indexOf("<success>") != -1) {// the operation worked
            // write the file to the cache and return the file object
            return true;

        } else {// something weird happened.
            return false;
        }

    }

    public boolean handleDeleteFile(String name) {

        Properties prop = new Properties();
        prop.put("action", "delete");
        prop.put("docid", name);

        String message = getMetacatString(prop);
        debug("Delete Message: " + message);
        if (message.indexOf("<error>") != -1) {// there was an error

            return false;
        } else if (message.indexOf("<success>") != -1) {// the operation worked
            // write the file to the cache and return the file object
            return true;

        } else {// something weird happened.
            return false;
        }
    }

    public String getMetacatString(Properties prop) {
        String response = null;

        // Now contact metacat and send the request
        try {
            InputStreamReader returnStream = new InputStreamReader(
                    getMetacatInputStream(prop));
            StringWriter sw = new StringWriter();
            int len;
            char[] characters = new char[512];
            while ((len = returnStream.read(characters, 0, 512)) != -1) {
                sw.write(characters, 0, len);
            }
            returnStream.close();
            response = sw.toString();
            sw.close();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        return response;
    }

    /**
     * Send a request to Metacat
     *
     * @param prop
     *            the properties to be sent to Metacat
     * @return InputStream as returned by Metacat
     */
    public InputStream getMetacatInputStream(Properties prop) {
        InputStream returnStream = null;
        // Now contact metacat and send the request
        try {
            returnStream = metacat.sendParameters(prop);
            return returnStream;
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
        return returnStream;
    }
}