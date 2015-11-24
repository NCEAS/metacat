/*  '$RCSfile$'
 *  Copyright: 2010 Regents of the University of California and the
 *              National Center for Ecological Analysis and Synthesis
 *  Purpose: To test the Access Controls in metacat by JUnit
 *
 *   '$Author: berkley $'
 *     '$Date: 2010-05-03 14:26:08 -0700 (Fri, 14 Aug 2009) $'
 * '$Revision: 5027 $'
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

package edu.ucsb.nceas.metacat;

import java.util.*;
import java.io.*;

import edu.ucsb.nceas.MCTestCase;
import edu.ucsb.nceas.metacat.MetacatProfiler.Profile;
import junit.framework.Test;
import junit.framework.TestSuite;


/**
 * A JUnit test for testing profiler
 */
public class MetacatProfilerTest extends MCTestCase 
{   
    private int createCount = 0;
    
    /**
    * consstructor for the test
    */
    public MetacatProfilerTest(String name)
    {
        super(name);
    }
  
    /**
	 * Establish a testing framework by initializing appropriate objects
	 */
	public void setUp() throws Exception 
	{
		super.setUp();
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
		suite.addTest(new MetacatProfilerTest("testProfiling"));
		suite.addTest(new MetacatProfilerTest("testSort"));
		return suite;
	}
	
	public void testSort()
	{
	    MetacatProfiler mp = MetacatProfiler.getInstance();
	    mp.reset();
        mp.startTime("test1");
        sleep(100);
        mp.stopTime("test1");
        mp.startTime("test2");
        sleep(200);
        mp.stopTime("test2");
        mp.startTime("test3");
        sleep(500);
        mp.stopTime("test3");
        mp.startTime("test4");
        sleep(150);
        mp.stopTime("test4");
        mp.startTime("test3");
        sleep(150);
        mp.stopTime("test3");
        try
        {
          mp.printSortedCSV(null, "total");
          System.out.println("");
          mp.printSortedCSV(null, "callorder");
          System.out.println("");
          mp.printSortedCSV(null, "callcount");
        }
        catch(Exception e)
        {
            e.printStackTrace();
            fail("error printing sorted csv");
        }
	}
	
	/**
	 * insert an invalid document and make sure create fails and does not
	 * leave any artifacts behind
	 */
	public void testProfiling()
	{
	    MetacatProfiler mp = MetacatProfiler.getInstance();
	    mp.startTime("test1");
	    sleep(100);
	    mp.stopTime("test1");
	    Hashtable<String, Profile> h = mp.getProfiles();
	    Profile p = h.get("test1");
	    System.out.println("p: " + p.toString());
	    assertTrue(p.total >= 100 && p.total < 150);
	    assertTrue(p.methodcalls == 1);
	    
	    
	    mp.reset();
	    mp.startTime("test2");
	    sleep(200);
	    mp.stopTime("test2");
	    mp.startTime("test2");
	    sleep(100);
	    mp.stopTime("test2");
	    mp.startTime("test3");
	    sleep(50);
	    mp.stopTime("test3");
	    h = mp.getProfiles();
	    assertTrue(h.size() == 2);
	    p = h.get("test2");
	    System.out.println("p: " + p.toString());
	    assertTrue(p.total >= 300 && p.total < 350);
	    assertTrue(p.methodcalls == 2);
	    Profile p2 = h.get("test3");
	    System.out.println("p2: " + p2.toString());
	    assertTrue(p2.total >= 50 && p2.total < 100);
	    assertTrue(p2.methodcalls == 1);
	}
	
	private void sleep(long millis)
	{
	    try
	    {
	        Thread.currentThread().sleep(millis);
	    }
	    catch(Exception e)
	    {
	    }
	}
}