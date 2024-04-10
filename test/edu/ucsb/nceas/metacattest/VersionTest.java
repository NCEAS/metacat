package edu.ucsb.nceas.metacattest;

import java.sql.SQLException;

import edu.ucsb.nceas.MCTestCase;
import edu.ucsb.nceas.metacat.MetacatVersion;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;



/**
 * @author jones
 *
 * Test class for the Version class.
 */
public class VersionTest extends MCTestCase
{
    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception
    {
        super.setUp();
    }
    
    /**
     * Test the "getVersion" method by printing its output.
     */
    public void testGetVersion()
    {
    	try {
            assertTrue(PropertyService.getProperty("application.metacatVersion").equals(MetacatVersion.getVersionID()));
            System.out.println(MetacatVersion.getVersionID());
    	} catch (PropertyNotFoundException pnfe) {
    		fail ("Could not get metacat version property; " + pnfe.getMessage());
    	}

    }

    /**
     * Test the "printVersionAsXml" method by printing its output.
     */
    public void testPrintVersionAsXml() throws SQLException {
    	try {
        System.out.println(MetacatVersion.getVersionAsXml());
        assertTrue(MetacatVersion.getVersionAsXml().indexOf(
                PropertyService.getProperty("application.metacatVersion")) != -1);
    	} catch (PropertyNotFoundException pnfe) {
    		fail ("Could not get metacat version property; " + pnfe.getMessage());
    	}
    }

    /**
     * Test the method of getVersionFromDB
     * @throws SQLException
     * @throws PropertyNotFoundException
     */
    public void testGetVersionFromDB() throws SQLException, PropertyNotFoundException {
        assertTrue(MetacatVersion.getVersionFromDB().equals(
                PropertyService.getProperty("application.metacatVersion")));
    }
}
