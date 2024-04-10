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