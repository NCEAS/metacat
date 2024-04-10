package edu.ucsb.nceas.metacat.doi.osti;

import org.junit.Test;

import edu.ucsb.nceas.MCTestCase;

/**
 * A junit test for the OstiErrorEmailAgent class
 * @author tao
 *
 */
public class OstiErrorEmailAgentTest extends MCTestCase {
    
    /**
     * Test the notify method
     */
    @Test
    public void testNotify() {
        OstiErrorEmailAgent agent = new OstiErrorEmailAgent();
       String error = "this is email is a test";
        agent.notify(error);
    }

}
