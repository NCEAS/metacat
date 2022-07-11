/**
 *  '$RCSfile$'
 *  Copyright: 2022 Regents of the University of California and the
 *              National Center for Ecological Analysis and Synthesis
 *  Purpose: To test the Access Controls in metacat by JUnit
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
package edu.ucsb.nceas.metacat.index.queue;


import edu.ucsb.nceas.metacat.dataone.D1NodeServiceTest;
import edu.ucsb.nceas.metacat.doi.osti.OstiErrorEmailAgent;
import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * A junit test for the IndexGenerator class
 * @author tao
 *
 */
public class IndexGeneratorTest extends D1NodeServiceTest {
    
    /**
     * Build the test suite
     * 
     * @return
     */
    public static Test suite() {
        TestSuite suite = new TestSuite();
        suite.addTest(new IndexGeneratorTest("testGetLastSubdir"));
        return suite;
    }
    
    /**
     * Constructor
     * @param name
     */
    public IndexGeneratorTest(String name) {
        super(name);
    }
    
    /**
     * Test the getLastSubdir method
     */
    public void testGetLastSubdir() {
       String path = "/";
       String lastPart = IndexGenerator.getLastSubdir(path);
       assertTrue(lastPart.equals(""));
       path = "/var/data/";
       lastPart = IndexGenerator.getLastSubdir(path);
       assertTrue(lastPart.equals("data"));
       path = "/var/document";
       lastPart = IndexGenerator.getLastSubdir(path);
       assertTrue(lastPart.equals("document"));
       path = "data";
       lastPart = IndexGenerator.getLastSubdir(path);
       assertTrue(lastPart.equals("data"));
       path = "/var";
       lastPart = IndexGenerator.getLastSubdir(path);
       assertTrue(lastPart.equals("var"));
       path = "/metacat/";
       lastPart = IndexGenerator.getLastSubdir(path);
       assertTrue(lastPart.equals("metacat"));
       path = "//";
       lastPart = IndexGenerator.getLastSubdir(path);
       assertTrue(lastPart.equals(""));
       path = "///";
       lastPart = IndexGenerator.getLastSubdir(path);
       assertTrue(lastPart.equals(""));
       
    }

}
