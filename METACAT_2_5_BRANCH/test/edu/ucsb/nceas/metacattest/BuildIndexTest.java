/**
 *  '$RCSfile$'
 *  Copyright: 2004 Regents of the University of California and the
 *              National Center for Ecological Analysis and Synthesis
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

import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.SimpleTimeZone;
import java.util.TimeZone;

import edu.ucsb.nceas.MCTestCase;
import edu.ucsb.nceas.metacat.DocumentImpl;
import edu.ucsb.nceas.metacat.McdbException;
import edu.ucsb.nceas.metacat.client.InsufficientKarmaException;
import edu.ucsb.nceas.metacat.client.Metacat;
import edu.ucsb.nceas.metacat.client.MetacatAuthException;
import edu.ucsb.nceas.metacat.client.MetacatException;
import edu.ucsb.nceas.metacat.client.MetacatFactory;
import edu.ucsb.nceas.metacat.client.MetacatInaccessibleException;
import edu.ucsb.nceas.metacat.database.DatabaseService;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.metacat.shared.ServiceException;
import edu.ucsb.nceas.metacat.util.MetacatUtil;
import edu.ucsb.nceas.utilities.IOUtil;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;
import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * A JUnit test for testing the indexing routines for XML Paths
 */
public class BuildIndexTest extends MCTestCase {
	private static String metacatUrl;
	private static String username;
	private static String password;
	static {
		try {
			PropertyService.getInstance();
			metacatUrl = PropertyService.getProperty("test.metacatUrl");
			username = PropertyService.getProperty("test.mcUser");
			password = PropertyService.getProperty("test.mcPassword");
		} catch (PropertyNotFoundException pnfe) {
			System.err.println("Could not get property in static block: "
					+ pnfe.getMessage());
		} catch (ServiceException se) {
			System.err.println("Service problem in static block: "
					+ se.getMessage());
		}
	}

	private String prefix = "test";
	private String newdocid = null;
	private String testfile = "test/eml-sample.xml";
	private String testdocument = "";
	private Metacat m;

	/**
	 * Constructor to build the test
	 * 
	 * @param name
	 *            the name of the test method
	 */
	public BuildIndexTest(String name) {
		super(name);
		newdocid = generateDocid();
	}

	/**
	 * Establish a testing framework by initializing appropriate objects
	 */
	public void setUp() {
		try {
			DatabaseService.getInstance();
//			PropertyService.getInstance("build/tests");
			PropertyService.getInstance();
			metacatUrl = PropertyService.getProperty("test.metacatUrl");
		} catch (ServiceException se) {
			fail(se.getMessage());
		} catch (PropertyNotFoundException pnfe) {
			fail(pnfe.getMessage());
		}

		try {
			FileReader fr = new FileReader(testfile);
			testdocument = IOUtil.getAsString(fr, true);
		} catch (IOException ioe) {
			fail("Can't read test data to run the test: " + testfile);
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
		suite.addTest(new BuildIndexTest("initialize"));
		suite.addTest(new BuildIndexTest("read"));
		suite.addTest(new BuildIndexTest("buildIndex"));
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
	 * Test the read() function with a known document
	 */
	public void read() {
		try {
			System.err.println("Test Metacat: " + metacatUrl);
			m = MetacatFactory.createMetacatConnection(metacatUrl);
			String identifier = newdocid + ".1";
			m.login(username, password);
			String response = m.insert(identifier, new StringReader(testdocument), null);
			System.err.println(response);
			Reader r = new InputStreamReader(m.read(newdocid + ".1"));
			String doc = IOUtil.getAsString(r, true);
			// doc = doc +"\n";
			System.err.println(doc);
			// assertTrue(doc.equals(testdocument));
		} catch (MetacatInaccessibleException mie) {
			System.err.println("Metacat is: " + metacatUrl);
			fail("Metacat connection failed." + mie.getMessage());
		} catch (MetacatAuthException mae) {
			fail("Authorization failed:\n" + mae.getMessage());
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
	 * Test the buildIndex() function with a known document
	 */
	public void buildIndex() {
		DocumentImpl d = null;
		try {
			d = new DocumentImpl(newdocid + ".1", false);
			d.buildIndex();
		} catch (McdbException me) {
			System.err.println("Caught McdbException (1): " + me.getMessage());
		}
	}

	/**
	 * Create a hopefully unique docid for testing insert and update. Does not
	 * include the 'revision' part of the id.
	 * 
	 * @return a String docid based on the current date and time
	 */
	private String generateDocid() {
		StringBuffer docid = new StringBuffer(prefix);
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
