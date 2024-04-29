package edu.ucsb.nceas.metacat.index.queue;


import edu.ucsb.nceas.metacat.dataone.D1NodeServiceTest;
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
