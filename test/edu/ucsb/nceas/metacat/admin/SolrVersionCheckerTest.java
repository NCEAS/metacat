/**
 *  '$RCSfile$'
 *  Copyright: 2004 Regents of the University of California and the
 *              National Center for Ecological Analysis and Synthesis
 *  Purpose: To test the Access Controls in metacat by JUnit
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

package edu.ucsb.nceas.metacat.admin;

import junit.framework.Test;
import junit.framework.TestSuite;
import edu.ucsb.nceas.MCTestCase;

public class SolrVersionCheckerTest extends MCTestCase {
    
    public SolrVersionCheckerTest(String name) {
        super(name);
    }
        
    /**
     * Establish a testing framework by initializing appropriate objects
     */
    public void setUp() {
        
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
        suite.addTest(new SolrVersionCheckerTest("testIsVersion34"));
        return suite;
    }
    
    public void testIsVersion34() throws Exception {
        String solrHome = "metacat-common/src/main/resources/solr-home";
        SolrVersionChecker checker = new SolrVersionChecker();
        assertTrue(!checker.isVersion_3_4(solrHome));
    }
}