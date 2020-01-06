/**
 *  '$RCSfile$'
 *  Copyright: 2004 Regents of the University of California and the
 *             National Center for Ecological Analysis and Synthesis
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
