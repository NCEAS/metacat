package edu.ucsb.nceas.metacattest;

import edu.ucsb.nceas.MCTestCase;
import edu.ucsb.nceas.metacat.ReadOnlyChecker;



/**
 * @author tao
 *
 * Test class for the Version class.
 */
public class ReadOnlyCheckerTest extends MCTestCase
{
    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();
    }
    
    /**
     * Test the method of isReadOnly
     */
    public void testIsReadOnly() {
           ReadOnlyChecker checker = new ReadOnlyChecker();
           assertTrue(!checker.isReadOnly());
    }


}
