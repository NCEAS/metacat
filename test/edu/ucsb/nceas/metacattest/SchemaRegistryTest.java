package edu.ucsb.nceas.metacattest;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringReader;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.SimpleTimeZone;
import java.util.TimeZone;
import java.util.Vector;

import edu.ucsb.nceas.MCTestCase;
import edu.ucsb.nceas.metacat.client.InsufficientKarmaException;
import edu.ucsb.nceas.metacat.client.Metacat;
import edu.ucsb.nceas.metacat.client.MetacatAuthException;
import edu.ucsb.nceas.metacat.client.MetacatException;
import edu.ucsb.nceas.metacat.client.MetacatFactory;
import edu.ucsb.nceas.metacat.client.MetacatInaccessibleException;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.metacat.util.SystemUtil;
import edu.ucsb.nceas.utilities.FileUtil;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;
import edu.ucsb.nceas.utilities.UtilException;
import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * A JUnit test for testing Access Control in Metacat
 */
public class SchemaRegistryTest extends MCTestCase {

	private static String metacatDeployDir;
	private static String username;
	private static String password;
	static {
		try {
			metacatDeployDir = PropertyService.getProperty("application.deployDir");
			username = PropertyService.getProperty("test.mcUser");
			password = PropertyService.getProperty("test.mcPassword");
		} catch (PropertyNotFoundException pnfe) {
			System.err.println("Could not get property in static block: "
					+ pnfe.getMessage());
		}
	}

	private static String testFileLocation = null;
	private static String serverSchemaLocation = null;
	private static String badSchemaLocationXMLFile = "bad-schema-location.xml";
	private static String nonConformingXMLFile = "non-conforming-xml.xml";
	private static String goodSchemaXMLFile = "good-schema.xml";
	private static String goodSchemaFile1 = "chapter05ord.xsd";
	private static String goodSchemaFile2 = "chapter05prod.xsd";
	private static String includedSchemaXMLFile = "company.xml";
	private static String includedSchemaFile1="company.xsd";
	private static String includedSchemaFile2="worker.xsd";
	private static String includedSchemaFile3="item.xsd";
	private static String includedSchemaFile4="product.xsd";
	
	
	private static String getRegisteredTestSchemaSql =
		"SELECT * FROM xml_catalog " + 
		"WHERE entry_type = 'Schema' " + 
		"AND system_id LIKE '%chapter05%'";
	
	private static String deleteRegisteredTestSchemaSql =
		"DELETE FROM xml_catalog " + 
		"WHERE entry_type = 'Schema' " + 
		"AND system_id LIKE '%chapter05%'";	
	
	private static String getRegisteredIncludedTestSchemaSql =
    "SELECT * FROM xml_catalog " + 
    "WHERE entry_type = 'Schema' " + 
    "AND system_id LIKE '%company%'";
  
  private static String deleteRegisteredIncludedTestSchemaSql =
    "DELETE FROM xml_catalog " + 
    "WHERE entry_type = 'Schema' " + 
    "AND system_id LIKE '%company%'"; 

	private Metacat m;

	/**
	 * Constructor to build the test
	 * 
	 * @param name
	 *            the name of the test method
	 */
	public SchemaRegistryTest(String name) {
		super(name);
	}

	/**
	 * Establish a testing framework by initializing appropriate objects
	 */
	public void setUp() {
		try {
			debug("Test Metacat: " + metacatUrl);
			m = MetacatFactory.createMetacatConnection(metacatUrl);
		} catch (MetacatInaccessibleException mie) {
			System.err.println("Metacat is: " + metacatUrl);
			fail("Metacat connection failed." + mie.getMessage());
		}
		
		testFileLocation = "test/clienttestfiles/";
		serverSchemaLocation = metacatDeployDir + "/schema/";
		
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
		suite.addTest(new SchemaRegistryTest("initialize"));
		// Test basic functions
		/*suite.addTest(new SchemaRegistryTest("newGoodSchemaRegisterTest"));
		suite.addTest(new SchemaRegistryTest("existingSchemaRegisterTest"));
		suite.addTest(new SchemaRegistryTest("newNonexistantSchemaLocationRegisterTest"));
		suite.addTest(new SchemaRegistryTest("newGoodSchemaBadXmlRegisterTest"));
		suite.addTest(new SchemaRegistryTest("existingGoodSchemaBadXmlRegisterTest"));
		suite.addTest(new SchemaRegistryTest("schemaInDbNotOnFileSystemTest"));
		suite.addTest(new SchemaRegistryTest("includedSchemaRegisterTest"));*/
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
	 * Tests adding a document that has a new valid schema and the xml conforms to the 
	 * schema. The file we use has two schemas, so we expect to see two rows in the 
	 * xml_catalog table and two schema files added to the server.
	 */
	public void newGoodSchemaRegisterTest() {
		try {
			debug("\nRunning: newGoodSchemaRegisterTest");

			String newdocid = generateDocid();
			
			debug("Refreshing the XMLSchemaService");
			httpPost(metacatUrl + "?action=refreshServices",
					new HashMap<String, String>());

			String testDocument = getTestDocument(testFileLocation + goodSchemaXMLFile);
			System.out.println(""+testDocument);
			// login
			debug("logging in as: username=" + username + " password=" + password);
			m.login(username, password);
			
			Thread.sleep(2000);
			// insert document.  We expect to succeed.
			insertDocid(newdocid + ".1", testDocument, SUCCESS, false);
			
			debug("Checking db for registered schemas");
			Vector<Hashtable<String,Object>> sqlResults = 
				dbSelect(getRegisteredTestSchemaSql, 
						"SchemaRegistryTest.newGoodSchemaRegisterTest");
			if (sqlResults.size() != 2) {
				fail("Expected number of test schemas in the database is 2." +
						" The number found was: " + sqlResults.size());
			}
			
			debug("Checking server for registered schema file: " + serverSchemaLocation + goodSchemaFile1);
			if( FileUtil.getFileStatus(serverSchemaLocation + goodSchemaFile1) < 
					FileUtil.EXISTS_READABLE) {
				fail("Could not find expected schema file on server: " + 
						serverSchemaLocation + goodSchemaFile1);
			}

			debug("Checking server for registered schema file: " + serverSchemaLocation + goodSchemaFile2);			
			if( FileUtil.getFileStatus(serverSchemaLocation + goodSchemaFile2) < 
					FileUtil.EXISTS_READABLE) {
				fail("Could not find expected schema file on server: " + 
						serverSchemaLocation + goodSchemaFile2);
			}
			
			// Clean the system up
			debug("Deleting test schemas from db");
			dbUpdate(deleteRegisteredTestSchemaSql,
					"SchemaRegistryTest.newGoodSchemaRegisterTest");
			
			debug("Deleting test schema files from server file system");
			FileUtil.deleteFile(serverSchemaLocation + goodSchemaFile1);
			FileUtil.deleteFile(serverSchemaLocation + goodSchemaFile2);
			
			debug("Refreshing the XMLSchemaService");
			httpPost(metacatUrl + "?action=refreshServices",
					new HashMap<String, String>());
			
			deleteDocid(newdocid + ".1", SUCCESS, false);
			
			m.logout();

		} catch (MetacatAuthException mae) {
			fail("Authorization failed: " + mae.getMessage());
		} catch (MetacatInaccessibleException mie) {
			fail("Metacat Inaccessible: " + mie.getMessage());
		} catch (IOException ioe) {
			fail("I/O Exception: " + ioe.getMessage());
		} catch (SQLException sqle) {
			fail("SQL Exception: " + sqle.getMessage());
		} catch (Exception e) {
			fail("General exception: " + e.getMessage());
		}
	}
	
	/**
	 * Tests adding a second document that has an existing valid schema and the xml 
	 * conforms to the schema.  Primarily, we want to make sure that the original
	 * two schema entries in the database are the only two and that duplicates did
	 * not get entered.
	 */
	public void existingSchemaRegisterTest() {
		try {
			debug("\nRunning: existingSchemaRegisterTest");

			String newdocid1 = generateDocid();
			
			debug("Refreshing the XMLSchemaService");
			httpPost(metacatUrl + "?action=refreshServices",
					new HashMap<String, String>());

			String testDocument = getTestDocument(testFileLocation + goodSchemaXMLFile);
			
			// login
			debug("logging in as: username=" + username + " password=" + password);
			m.login(username, password);
			
			// insert document.  We expect to succeed.
			insertDocid(newdocid1 + ".1", testDocument, SUCCESS, false);
			
			// create a second doc id and insert another document
			String newdocid2 = generateDocid();
			insertDocid(newdocid2 + ".1", testDocument, SUCCESS, false);
			
	         Thread.sleep(2000);
			
			// Check the db for registered schemas.  We should find two and only
			// two.
			debug("Checking db for registered schemas");
			Vector<Hashtable<String,Object>> sqlResults = 
				dbSelect(getRegisteredTestSchemaSql, 
						"SchemaRegistryTest.existingSchemaRegisterTest");
			if (sqlResults.size() != 2) {
				fail("Expected number of test schemas in the database is 2." +
						" The number found was: " + sqlResults.size());
			}
			
			// Clean the system up
			debug("Deleting test schemas from db");
			dbUpdate(deleteRegisteredTestSchemaSql,
					"SchemaRegistryTest.existingSchemaRegisterTest");
			
			debug("Deleting test schema files from server file system");
			FileUtil.deleteFile(serverSchemaLocation + goodSchemaFile1);
			FileUtil.deleteFile(serverSchemaLocation + goodSchemaFile2);
			
			debug("Refreshing the XMLSchemaService");
			httpPost(metacatUrl + "?action=refreshServices",
					new HashMap<String, String>());
			
			deleteDocid(newdocid1 + ".1", SUCCESS, false);
			deleteDocid(newdocid2 + ".1", SUCCESS, false);
			
			m.logout();		
			
		} catch (MetacatAuthException mae) {
			fail("Authorization failed: " + mae.getMessage());
		} catch (MetacatInaccessibleException mie) {
			fail("Metacat Inaccessible: " + mie.getMessage());
		} catch (IOException ioe) {
			fail("I/O Exception: " + ioe.getMessage());
		} catch (SQLException sqle) {
			fail("SQL Exception: " + sqle.getMessage());
		} catch (Exception e) {
			fail("General exception: " + e.getMessage());
		}
	}
	
	/**
	 * Tests adding a document that has an invalid schema location. The insert
	 * should fail and the schema should not get registered.
	 */
	public void newNonexistantSchemaLocationRegisterTest() {
		try {
			debug("\nRunning: newNonexistantSchemaLocationRegisterTest");

			String newdocid1 = generateDocid();
			
			debug("Refreshing the XMLSchemaService");
			httpPost(metacatUrl + "?action=refreshServices",
					new HashMap<String, String>());

			String testDocument = getTestDocument(testFileLocation + badSchemaLocationXMLFile);
			
			// login
			debug("logging in as: username=" + username + " password=" + password);
			m.login(username, password);
			
			// insert document.  We expect to fail with a MetacatException because the schemaLocation
			// does not exist.
			insertDocidExpectException(newdocid1 + ".1", testDocument, "Failed to read schema document");
	         Thread.sleep(2000);

			// Check the db for registered schemas.  We should find none.
			debug("Checking db for registered schemas");
			Vector<Hashtable<String,Object>> sqlResults = 
				dbSelect(getRegisteredTestSchemaSql, 
						"SchemaRegistryTest.newNonexistantSchemaLocationRegisterTest");
			if (sqlResults.size() != 0) {
				fail("Expected number of test schemas in the database is 0." +
						" The number found was: " + sqlResults.size());
			}
			
			
			// check the system for the schema files.  There should be none.
			debug("Checking server for registered schema file: " + serverSchemaLocation + goodSchemaFile1);
			if( FileUtil.getFileStatus(serverSchemaLocation + goodSchemaFile1) >= 
					FileUtil.EXISTS_ONLY) {
				fail("Found unexpected schema file on server: " + 
						serverSchemaLocation + goodSchemaFile1);
			}

			debug("Checking server for registered schema file: " + serverSchemaLocation + goodSchemaFile2);			
			if( FileUtil.getFileStatus(serverSchemaLocation + goodSchemaFile2) >= 
					FileUtil.EXISTS_ONLY) {
				fail("Found unexpected schema file on server: " + 
						serverSchemaLocation + goodSchemaFile2);
			}
			
			// Clean the system up, just to be safe.
			debug("Deleting test schemas from db");
			dbUpdate(deleteRegisteredTestSchemaSql,
					"SchemaRegistryTest.newNonexistantSchemaLocationRegisterTest");
			
			debug("Deleting test schema files from server file system");
			try {
				FileUtil.deleteFile(serverSchemaLocation + goodSchemaFile1);
			} catch (FileNotFoundException fnfe) {
				// Do nothing here. We are hoping that the file didn't exist.
			}

			try {
				FileUtil.deleteFile(serverSchemaLocation + goodSchemaFile2);
			} catch (FileNotFoundException fnfe) {
				// Do nothing here. We are hoping that the file didn't exist.
			}

			debug("Refreshing the XMLSchemaService");
			httpPost(metacatUrl + "?action=refreshServices",
					new HashMap<String, String>());

			m.logout();		
			
		} catch (MetacatAuthException mae) {
			fail("Authorization failed: " + mae.getMessage());
		} catch (MetacatInaccessibleException mie) {
			fail("Metacat Inaccessible: " + mie.getMessage());
		} catch (IOException ioe) {
			fail("I/O Exception: " + ioe.getMessage());
		} catch (SQLException sqle) {
			fail("SQL Exception: " + sqle.getMessage());
		} catch (Exception e) {
			fail("General exception: " + e.getMessage());
		}
	}
	
	/**
	 * Tests adding a document that has a valid schema location which has
	 * already been registered in metacat, but the xml doesn't conform to the
	 * schema. The insert should fail.
	 */
	public void newGoodSchemaBadXmlRegisterTest() {
		try {
			debug("\nRunning: newGoodSchemaBadXmlRegisterTest");

			String newdocid1 = generateDocid();
			
			debug("Refreshing the XMLSchemaService");
			httpPost(metacatUrl + "?action=refreshServices",
					new HashMap<String, String>());

			String testDocument = getTestDocument(testFileLocation + nonConformingXMLFile);
			
			// login
			debug("logging in as: username=" + username + " password=" + password);
			m.login(username, password);
			
			// insert document.  We expect to fail with a MetacatException because the schemaLocation
			// does not exist.
			insertDocidExpectException(newdocid1 + ".1", testDocument, "is not allowed to appear in element");
			
	         Thread.sleep(2000);
			
			// Check the db for registered schemas.  We should find none.
			debug("Checking db for registered schemas");
			Vector<Hashtable<String,Object>> sqlResults = 
				dbSelect(getRegisteredTestSchemaSql, 
						"SchemaRegistryTest.newGoodSchemaBadXmlRegisterTest");
			if (sqlResults.size() != 0) {
				fail("Expected number of test schemas in the database is 0." +
						" The number found was: " + sqlResults.size());
			}		
			
			// check the system for the schema files.  There should be none.
			debug("Checking server for registered schema file: " + serverSchemaLocation + goodSchemaFile1);
			if( FileUtil.getFileStatus(serverSchemaLocation + goodSchemaFile1) >= 
					FileUtil.EXISTS_ONLY) {
				fail("Found unexpected schema file on server: " + 
						serverSchemaLocation + goodSchemaFile1);
			}

			debug("Checking server for registered schema file: " + serverSchemaLocation + goodSchemaFile2);			
			if( FileUtil.getFileStatus(serverSchemaLocation + goodSchemaFile2) >= 
					FileUtil.EXISTS_ONLY) {
				fail("Found unexpected schema file on server: " + 
						serverSchemaLocation + goodSchemaFile2);
			}
			
			// Clean the system up, just to be safe.
			debug("Deleting test schemas from db");
			dbUpdate(deleteRegisteredTestSchemaSql,
					"SchemaRegistryTest.newNonexistantSchemaLocationRegisterTest");
			
			debug("Deleting test schema files from server file system");
			try {
				FileUtil.deleteFile(serverSchemaLocation + goodSchemaFile1);
			} catch (FileNotFoundException fnfe) {
				// Do nothing here. We are hoping that the file didn't exist.
			}

			try {
				FileUtil.deleteFile(serverSchemaLocation + goodSchemaFile2);
			} catch (FileNotFoundException fnfe) {
				// Do nothing here. We are hoping that the file didn't exist.
			}

			debug("Refreshing the XMLSchemaService");
			httpPost(metacatUrl + "?action=refreshServices",
					new HashMap<String, String>());

			m.logout();		
			
		} catch (MetacatAuthException mae) {
				fail("Authorization failed: " + mae.getMessage());
		} catch (MetacatInaccessibleException mie) {
			fail("Metacat Inaccessible: " + mie.getMessage());
		} catch (IOException ioe) {
			fail("I/O Exception: " + ioe.getMessage());
		} catch (SQLException sqle) {
			fail("SQL Exception: " + sqle.getMessage());
		} catch (Exception e) {
			fail("General exception: " + e.getMessage());
		}
	}
	
	/**
	 * Tests adding a document that has a valid schema location, but the xml
	 * doesn't conform to the schema. The insert should fail and the schema
	 * should not get registered.
	 */
	public void existingGoodSchemaBadXmlRegisterTest() {
		try {
			debug("\nRunning: existingGoodSchemaBadXmlRegisterTest");
		
			debug("Refreshing the XMLSchemaService");
			httpPost(metacatUrl + "?action=refreshServices",
					new HashMap<String, String>());

			// login
			debug("logging in as: username=" + username + " password=" + password);
			m.login(username, password);
			
			// insert good document.  We expect to succeed.
			String newdocid1 = generateDocid();
			String testDocument1 = getTestDocument(testFileLocation + goodSchemaXMLFile);
			insertDocid(newdocid1 + ".1", testDocument1, SUCCESS, false);

			String newdocid2 = generateDocid();
			String testDocument2 = getTestDocument(testFileLocation + nonConformingXMLFile);
			// attempt to insert non-conforming document.  We expect to fail with a MetacatException 
			// because the xml does not conform to the schema
			insertDocidExpectException(newdocid2 + ".1", testDocument2, "is not allowed to appear in element");
			
	         Thread.sleep(2000);
			// Check the db for registered schemas.  We should find none.
			debug("Checking db for registered schemas");
			Vector<Hashtable<String,Object>> sqlResults = 
				dbSelect(getRegisteredTestSchemaSql, 
						"SchemaRegistryTest.existingGoodSchemaBadXmlRegisterTest");
			if (sqlResults.size() != 2) {
				fail("Expected number of test schemas in the database is 2." +
						" The number found was: " + sqlResults.size());
			}		
			
			// Clean the system up.
			debug("Deleting test schemas from db");
			dbUpdate(deleteRegisteredTestSchemaSql,
					"SchemaRegistryTest.newNonexistantSchemaLocationRegisterTest");
			
			debug("Deleting test schema files from server file system");
			FileUtil.deleteFile(serverSchemaLocation + goodSchemaFile1);
			FileUtil.deleteFile(serverSchemaLocation + goodSchemaFile2);

			debug("Refreshing the XMLSchemaService");
			httpPost(metacatUrl + "?action=refreshServices",
					new HashMap<String, String>());
			
			deleteDocid(newdocid1 + ".1", SUCCESS, false);

			m.logout();		
			
		} catch (MetacatAuthException mae) {
			fail("Authorization failed: " + mae.getMessage());
		} catch (MetacatInaccessibleException mie) {
			fail("Metacat Inaccessible: " + mie.getMessage());
		} catch (IOException ioe) {
			fail("I/O Exception: " + ioe.getMessage());
		} catch (SQLException sqle) {
			fail("SQL Exception: " + sqle.getMessage());
		} catch (Exception e) {
			fail("General exception: " + e.getMessage());
		}
	}
	
	/**
	 * Tests adding a second document that has an existing valid schema and the
	 * xml conforms to the schema. Primarily, we want to make sure that the
	 * original two schema entries in the database are the only two and that
	 * duplicates did not get entered.
	 */
	public void schemaInDbNotOnFileSystemTest() {
		try {
			debug("\nRunning: schemaInDbNotOnFileSystemTest");
			
			debug("Refreshing the XMLSchemaService");
			httpPost(metacatUrl + "?action=refreshServices",
					new HashMap<String, String>());
			
			// login
			debug("logging in as: username=" + username + " password=" + password);
			m.login(username, password);
			
			// insert document.  We expect to succeed.			
			String newdocid1 = generateDocid();
			String testDocument = getTestDocument(testFileLocation + goodSchemaXMLFile);
			insertDocid(newdocid1 + ".1", testDocument, SUCCESS, false);
			
			debug("Deleting test schema files from server file system");
			FileUtil.deleteFile(serverSchemaLocation + goodSchemaFile1);
			FileUtil.deleteFile(serverSchemaLocation + goodSchemaFile2);
			
			debug("Refreshing the XMLSchemaService");
			httpPost(metacatUrl + "?action=refreshServices",
					new HashMap<String, String>());
			
			// create a second doc id and insert another document
			String newdocid2 = generateDocid();
			insertDocid(newdocid2 + ".1", testDocument, SUCCESS, false);
			
	         Thread.sleep(2000);
			
			// Check the db for registered schemas.  We should find two and only
			// two.
			debug("Checking db for registered schemas");
			Vector<Hashtable<String,Object>> sqlResults = 
				dbSelect(getRegisteredTestSchemaSql, 
						"SchemaRegistryTest.schemaInDbNotOnFileSystemTest");
			if (sqlResults.size() != 2) {
				fail("Expected number of test schemas in the database is 2." +
						" The number found was: " + sqlResults.size());
			}
			
			debug("Checking server for registered schema file: " + serverSchemaLocation + goodSchemaFile1);
			if( FileUtil.getFileStatus(serverSchemaLocation + goodSchemaFile1) < 
					FileUtil.EXISTS_READABLE) {
				fail("Could not find expected schema file on server: " + 
						serverSchemaLocation + goodSchemaFile1);
			}

			debug("Checking server for registered schema file: " + serverSchemaLocation + goodSchemaFile2);			
			if( FileUtil.getFileStatus(serverSchemaLocation + goodSchemaFile2) < 
					FileUtil.EXISTS_READABLE) {
				fail("Could not find expected schema file on server: " + 
						serverSchemaLocation + goodSchemaFile2);
			}
			
			// Clean the system up
			debug("Deleting test schemas from db");
			dbUpdate(deleteRegisteredTestSchemaSql,
					"SchemaRegistryTest.schemaInDbNotOnFileSystemTest");
			
			debug("Deleting test schema files from server file system");
			FileUtil.deleteFile(serverSchemaLocation + goodSchemaFile1);
			FileUtil.deleteFile(serverSchemaLocation + goodSchemaFile2);
			
			debug("Refreshing the XMLSchemaService");
			httpPost(metacatUrl + "?action=refreshServices",
					new HashMap<String, String>());
			
			deleteDocid(newdocid1 + ".1", SUCCESS, false);
			deleteDocid(newdocid2 + ".1", SUCCESS, false);
			
			m.logout();		
			
		} catch (MetacatAuthException mae) {
			fail("Authorization failed: " + mae.getMessage());
		} catch (MetacatInaccessibleException mie) {
			fail("Metacat Inaccessible: " + mie.getMessage());
		} catch (IOException ioe) {
			fail("I/O Exception: " + ioe.getMessage());
		} catch (SQLException sqle) {
			fail("SQL Exception: " + sqle.getMessage());
		} catch (Exception e) {
			fail("General exception: " + e.getMessage());
		}
	}
	
	/**
   * Tests adding a document that has a new valid schema and the xml conforms to the 
   * schema. The schema has included schemas. We expect one entry will be added to
   * the db. But four schema files will be downloaded (four schemas share the same 
   * target namespace).
   */
  public void includedSchemaRegisterTest() {
    try {
      debug("\nRunning: newGoodSchemaRegisterTest");

      String newdocid = generateDocid();
      
      debug("Refreshing the XMLSchemaService");
      httpPost(metacatUrl + "?action=refreshServices",
          new HashMap<String, String>());

      String testDocument = getTestDocument(testFileLocation + includedSchemaXMLFile);
      
      // login
      debug("logging in as: username=" + username + " password=" + password);
      m.login(username, password);
      
      // insert document.  We expect to succeed.
      insertDocid(newdocid + ".1", testDocument, SUCCESS, false);
      
      Thread.sleep(2000);

      
      debug("Checking db for registered schemas");
      Vector<Hashtable<String,Object>> sqlResults = 
        dbSelect(getRegisteredIncludedTestSchemaSql, 
            "SchemaRegistryTest.includedSchemaRegisterTest");
      if (sqlResults.size() != 1) {
        fail("Expected number of test schemas in the database is 1." +
            " The number found was: " + sqlResults.size());
      }
      deleteDocid(newdocid + ".1", SUCCESS, false);
      debug("Checking server for registered schema file: " + serverSchemaLocation + includedSchemaFile1);
      if( FileUtil.getFileStatus(serverSchemaLocation + includedSchemaFile1) < 
          FileUtil.EXISTS_READABLE) {
        fail("Could not find expected schema file on server: " + 
            serverSchemaLocation + includedSchemaFile1);
      }

      debug("Checking server for registered schema file: " + serverSchemaLocation + includedSchemaFile2);
      if( FileUtil.getFileStatus(serverSchemaLocation + includedSchemaFile2) < 
          FileUtil.EXISTS_READABLE) {
        fail("Could not find expected schema file on server: " + 
            serverSchemaLocation + includedSchemaFile2);
      }
      
      debug("Checking server for registered schema file: " + serverSchemaLocation + includedSchemaFile3);
      if( FileUtil.getFileStatus(serverSchemaLocation + includedSchemaFile3) < 
          FileUtil.EXISTS_READABLE) {
        fail("Could not find expected schema file on server: " + 
            serverSchemaLocation + includedSchemaFile3);
      }
      
      debug("Checking server for registered schema file: " + serverSchemaLocation + includedSchemaFile4);
      if( FileUtil.getFileStatus(serverSchemaLocation + includedSchemaFile4) < 
          FileUtil.EXISTS_READABLE) {
        fail("Could not find expected schema file on server: " + 
            serverSchemaLocation + includedSchemaFile4);
      }
      
      String newdocid2 = generateDocid();
      //insert document.  We expect to succeed.
      insertDocid(newdocid2 + ".1", testDocument, SUCCESS, false);
      String newdocid3 = generateDocid();
      // insert document.  We expect to succeed.
      insertDocid(newdocid3 + ".1", testDocument, SUCCESS, false);
      
      
      deleteDocid(newdocid2 + ".1", SUCCESS, false);
      deleteDocid(newdocid3 + ".1", SUCCESS, false);
      m.logout();
      
      // Clean the system up
      debug("Deleting test schemas from db");
      dbUpdate(deleteRegisteredIncludedTestSchemaSql,
          "SchemaRegistryTest.includedSchemaRegisterTest");
      
      debug("Deleting test schema files from server file system");
      FileUtil.deleteFile(serverSchemaLocation + includedSchemaFile1);
      FileUtil.deleteFile(serverSchemaLocation + includedSchemaFile2);
      FileUtil.deleteFile(serverSchemaLocation + includedSchemaFile3);
      FileUtil.deleteFile(serverSchemaLocation + includedSchemaFile4);
      
      debug("Refreshing the XMLSchemaService");
      httpPost(metacatUrl + "?action=refreshServices",
          new HashMap<String, String>());
      
    

    } catch (MetacatAuthException mae) {
      fail("Authorization failed: " + mae.getMessage());
    } catch (MetacatInaccessibleException mie) {
      fail("Metacat Inaccessible: " + mie.getMessage());
    } catch (IOException ioe) {
      fail("I/O Exception: " + ioe.getMessage());
    } catch (SQLException sqle) {
      fail("SQL Exception: " + sqle.getMessage());
    } catch (Exception e) {
      fail("General exception: " + e.getMessage());
    }
  }
	
	/**
	 * @param documentLocation
	 *            the path of the document to read.
	 * @return a string holding the contents of the document with the contextUrl
	 *         token replaced by the context url
	 */
	private String getTestDocument(String documentLocation) throws IOException,
			PropertyNotFoundException {
		String testDocument = null;
		try {
			testDocument = FileUtil.readFileToString(documentLocation);
		} catch (UtilException ue) {
			throw new IOException("Error reading file to string: " +  ue.getMessage());
		}

		String contextUrl = SystemUtil.getContextURL();
		//System.out.println("The context url is ========================"+contextUrl);
		testDocument = testDocument.replaceAll("@contextUrl@", contextUrl);

		return testDocument;
	}
	
	/**
	 * Insert a document into metacat via the metacat client.
	 * 
	 * @param docid
	 *            the id of the document to insert
	 * @param testdocument
	 *            the contents of the document
	 * @param result
	 *            do we expect this to succed or fail
	 * @param expectKarmaException
	 *            do we expect it to fail with a KarmaException
	 * @return the response that was passed back from metacat
	 */
	private String insertDocid(String docid, String testdocument, boolean result,
			boolean expectKarmaException) {
		debug("insertDocid(): docid=" + docid + " expectedResult=" + result
				+ " expectKarmaException=" + expectKarmaException);
		String response = null;
		try {
			response = m.insert(docid, new StringReader(testdocument), null);
			debug("expected result: " + result);
			if (result) {
				assertTrue(response, (response.indexOf("<success>") != -1));
				assertTrue(response, response.indexOf(docid) != -1);
			} else {
				debug("in else, checking: " + (response.indexOf("<success>") == -1));
				assertTrue(response, (response.indexOf("<success>") == -1));
			}
			debug("insertDocid():  response=" + response);
		} catch (MetacatInaccessibleException mie) {
			fail("Metacat Inaccessible:\n" + mie.getMessage());
		} catch (InsufficientKarmaException ike) {
			if (!expectKarmaException) {
				fail("Insufficient karma:\n" + ike.getMessage());
			}
		} catch (MetacatException me) {
			fail("Metacat Error:\n" + me.getMessage());
		} catch (Exception e) {
			fail("General exception:\n" + e.getMessage());
		}
		return response;
	}
	
	/**
	 * Try to insert a document into metacat. This is used when we expect a
	 * MetacatException
	 * 
	 * @param docid
	 *            the document id we are going to attempt to insert
	 * @param testdocument
	 *            the contents of the document we are going to attempt to insert
	 * @param expectedException
	 *            text that should be a substring of the MetacatException
	 *            message
	 */
	private void insertDocidExpectException(String docid, String testdocument, String expectedException) {
		debug("insertDocidExpectException(): docid=" + docid + " expectedException=" + expectedException);

		String response = null;
		try {
			response = m.insert(docid, new StringReader(testdocument), null);	
			fail("Expecting a MetacatException.  Instead, got response: " + response);
		} catch (MetacatInaccessibleException mie) {
			fail("Metacat Inaccessible:\n" + mie.getMessage());
		} catch (InsufficientKarmaException ike) {
			fail("Insufficient karma:\n" + ike.getMessage());
		} catch (MetacatException me) {
			assertTrue(me.getMessage(), me.getMessage().contains(expectedException));
		} catch (Exception e) {
			fail("General exception:\n" + e.getMessage());
		}
		
		
	}

	/**
	 * Delete a document into metacat. The expected result is passed as result
	 */
	private void deleteDocid(String docid, boolean result, boolean expectedKarmaFailure) {
		debug("deleteDocid(): docid=" + docid + " expectedResult=" + result
				+ " expectedKarmaFailure=" + expectedKarmaFailure);
		try {
			Thread.sleep(5000);
			String response = m.delete(docid);
			if (result) {
				assertTrue(response, response.indexOf("<success>") != -1);
			} else {
				assertTrue(response, response.indexOf("<success>") == -1);
			}
			debug("deleteDocid():  response=" + response);
		} catch (MetacatInaccessibleException mie) {
			fail("Metacat Inaccessible:\n" + mie.getMessage());
		} catch (InsufficientKarmaException ike) {
			if (!expectedKarmaFailure) {
				fail("Insufficient karma:\n" + ike.getMessage());
			}
		} catch (MetacatException me) {
			if (result) {
				fail("Metacat Error:\n" + me.getMessage());
			} else {
				debug("Metacat Error:\n" + me.getMessage());
			}
		} catch (Exception e) {
			fail("General exception:\n" + e.getMessage());
		}
	}

	/**
	 * Create a hopefully unique docid for testing insert and update. Does not
	 * include the 'revision' part of the id.
	 * 
	 * @return a String docid based on the current date and time
	 */
	private String generateDocid() {
		try {
			Thread.sleep(1100);
		} catch (Exception e) {
			debug("Could not sleep: " + e.getMessage());
		}
		StringBuffer docid = new StringBuffer("test");
		docid.append(".");

		// Create a calendar to get the date formatted properly
		String[] ids = TimeZone.getAvailableIDs(-8 * 60 * 60 * 1000);
		SimpleTimeZone pdt = new SimpleTimeZone(-8 * 60 * 60 * 1000, ids[0]);
		pdt.setStartRule(Calendar.APRIL, 1, Calendar.SUNDAY, 2 * 60 * 60 * 1000);
		pdt.setEndRule(Calendar.OCTOBER, -1, Calendar.SUNDAY, 2 * 60 * 60 * 1000);
		Calendar calendar = new GregorianCalendar(pdt);
		Date trialTime = new Date();
		calendar.setTime(trialTime);
		docid.append(calendar.get(Calendar.YEAR));
		docid.append(calendar.get(Calendar.DAY_OF_YEAR));
		docid.append(calendar.get(Calendar.HOUR_OF_DAY));
		docid.append(calendar.get(Calendar.MINUTE));
		docid.append(calendar.get(Calendar.SECOND));

		return docid.toString();
	}
	
}
