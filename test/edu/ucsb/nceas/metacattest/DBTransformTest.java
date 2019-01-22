/**
 *  '$RCSfile$'
 *  Copyright: 2010 Regents of the University of California and the
 *             National Center for Ecological Analysis and Synthesis
 *
 *   '$Author: jones $'
 *     '$Date: 2010-02-03 17:58:12 -0900 (Wed, 03 Feb 2010) $'
 * '$Revision: 5211 $'
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import edu.ucsb.nceas.metacat.shared.ServiceException;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * This is NOT a direct test of the getTransformer method in DBTransform, because
 * I was having trouble separating the functionality being tested from context
 * configuration.
 * 
 * Instead, it is a cut-and-paste duplication of the key method into the test case,
 * and measurement of performance against it.
 * @author rnahf
 *
 */
public class DBTransformTest extends TestCase {

	// TODO:  this needs to be generalized to make this test useful
	
    static String BASEDIR = "/Users/rnahf/software/git_checkouts";
	
	
    public DBTransformTest(String name) {
        super(name);
    }
    
    /**
     * Create a suite of tests to be run together
     */
    public static Test suite() {
        TestSuite suite = new TestSuite();
//        suite.addTest(new DBTransformTest("initialize"));
        // Test basic functions
        suite.addTest(new DBTransformTest("testGetTransformerPerformance"));
        suite.addTest(new DBTransformTest("testGetUniqueTransformer"));
        suite.addTest(new DBTransformTest("testTransformation"));
        
        return suite;
    }

    public void setUp() throws ServiceException {

    	
    }
    
	static final protected Map<String,Templates> TemplatesMap = new HashMap<>();                                                                      
	static final protected TransformerFactory transformerFactory = TransformerFactory.newInstance();                                                  
	                                                                                                                                                    
	/**                                                                                                                                               
	 * The method that manages the Templates Map instances that will be used to build                                                                 
	 * transformers from.                                                                                                                             
	 * 
	 * @param xslSystemId - the URL for the stylesheet 
	 * @param forceRebuild - if true, forces reload of the stylesheet from the system, else use the existing one, if there
	 * @return
	 * @throws TransformerConfigurationException
	 */
	protected static synchronized Transformer getTransformer(String xslSystemId, boolean forceRebuild) throws TransformerConfigurationException {     
		if (forceRebuild || !TemplatesMap.containsKey(xslSystemId) ) {                                                                                
			Templates templates = transformerFactory.newTemplates(new StreamSource(xslSystemId));    
			System.out.println("Templates instance: " + templates);
			TemplatesMap.put(xslSystemId,templates);                                                                                                  
		}

		return TemplatesMap.get(xslSystemId) != null ? TemplatesMap.get(xslSystemId).newTransformer() : null;                                                                                        
	}        

	/**
	 * test getting a guid from the systemmetadata table
	 * @throws SQLException 
	 * @throws IOException 
	 * @throws PropertyNotFoundException 
	 * @throws ClassNotFoundException 
	 * @throws TransformerException 
	 */
	public void testGetUniqueTransformer() throws ClassNotFoundException, PropertyNotFoundException, IOException, SQLException, TransformerException
	{		
		String skin = BASEDIR + "/metacat/lib/style/skins/default/default.xml";

		Transformer t1 = DBTransformTest.getTransformer(skin, false);
		Transformer t2 = DBTransformTest.getTransformer(skin, false);
		assertNotSame("Should get different transformer instances", t1, t2);
	}
	
	/**
	 * A test to 
	 * @throws Exception
	 */
	public void testGetTransformerPerformance() throws Exception {
		
		String skin = BASEDIR + "/metacat/lib/style/skins/default/default.xml";
		
		long t0 = System.nanoTime();
		Transformer t = transformerFactory.newTransformer(new StreamSource(skin));
		long t1 = System.nanoTime();
		
		long[] laps = new long[101];
		long average = 0;
		for (int i=0; i<=100; i++) {
			long beg= System.nanoTime();
			Transformer tft = DBTransformTest.getTransformer(skin, false);
			long end = System.nanoTime();
			laps[i] = end - beg;
			if (i>0)
				average += laps[i];
		}
		average /= 100;
		
		System.out.println("Direct-built one: " + (t1-t0) + " nanosec [" + (t1-t0)/1000/1000 + " millisec]");
		System.out.println("First one: " + laps[0] + " nanosec [" + laps[0]/1000/1000 + " millisec]");
		System.out.println("second one: " + laps[1]);
		System.out.println("Average: " + average);
		System.out.println("Fold increase: " + (t1-t0) / average );
	
		assertTrue("There should be consistent (20x) reduction in build time", average * 20 < t1-t0);
		
	}	
	
	
	/**
	 * Test just to see if the transformer works.  Side effect is to output the performance timing.
	 * @throws Exception
	 */
	public void testTransformation() throws Exception {
		
		String skin = BASEDIR + "/metacat/lib/style/skins/default/default.xml";
		String doc = BASEDIR + "/metacat/test/eml-sample.xml";
		
		long start = System.nanoTime();
		Transformer t2 = DBTransformTest.getTransformer(skin, false);
		long lap1 = System.nanoTime();
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		
		t2.transform(new StreamSource(doc), new StreamResult(baos));
		long lap2 = System.nanoTime();
		System.out.println(baos.toString());
		long lap3 = System.nanoTime();
		
		System.out.println("Transformer build: " + (lap1-start));
		System.out.println("Transformation: " + (lap2-lap1) + " nanos [" 
				+ (lap2-lap1+500000)/1000/1000 + " millisec]");   // (Adding 500000ns to effect rounding
		System.out.println("serialize: " + (lap3-lap2));
		
		
	}
}
