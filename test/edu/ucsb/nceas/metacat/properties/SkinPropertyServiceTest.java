package edu.ucsb.nceas.metacat.properties;

import java.util.Vector;

import javax.servlet.ServletContext;

import edu.ucsb.nceas.MCTestCase;
import edu.ucsb.nceas.metacat.service.ServiceService;
import edu.ucsb.nceas.metacat.util.SkinUtil;
import junit.framework.Test;
import junit.framework.TestSuite;

import org.mockito.Mockito;

/**
 * Junit tests for the SkinPropertyServiceTest class
 * @author tao
 *
 */
public class SkinPropertyServiceTest extends MCTestCase {
    
    private static final String SKIN_NAME = "test-theme";
    
    /**
     * Constructor
     * @param name  name of the test method
     */
    public SkinPropertyServiceTest(String name) {
        super(name);
    }

    
    /**
     * Establish a testing framework by initializing appropriate objects
     */
    public void setUp() throws Exception {
        super.setUp();
    }

    /**
     * Release any objects after tests are complete
     */
    public void tearDown() {
    }

    /**
     * Create a suite of tests to be run together
     */
    public static Test suite() throws Exception {
        TestSuite suite = new TestSuite();
        suite.addTest(new SkinPropertyServiceTest("initialize"));
        suite.addTest(new SkinPropertyServiceTest("testGetPropertyNamesByGroup"));
        return suite;
    }
    
    /**
     * init
     */
    public void initialize() {
        assertTrue(1==1);
    }
    
    /**
     * Test the method of getPropertyNamesByGroup
     */
    public void testGetPropertyNamesByGroup() throws Exception {
        Vector<String> originalSkinNames = SkinUtil.getSkinNames();
        Vector<String> newNames = new Vector<String>();
        newNames.add(SKIN_NAME);
        SkinUtil.setSkinName(newNames);
        ServletContext servletContext = Mockito.mock(ServletContext.class);
        Mockito.when(servletContext.getContextPath()).thenReturn("context");
        Mockito.when(servletContext.getRealPath("/")).thenReturn("/");
        Mockito.when(servletContext.getRealPath("/style/skins")).thenReturn("test/skins");
        ServiceService.getInstance(servletContext);
        SkinPropertyService service = SkinPropertyService.getInstance();
        Vector<String> keys = service.getPropertyNamesByGroup(SKIN_NAME, "stylesheet.parameters");
        assertTrue(service.getProperty(SKIN_NAME, keys.elementAt(0)).equals("serverName"));
        assertTrue(service.getProperty(SKIN_NAME,keys.elementAt(1)).equals("https://foo.com"));
        assertTrue(service.getProperty(SKIN_NAME,keys.elementAt(2)).equals("testUser"));
        assertTrue(service.getProperty(SKIN_NAME,keys.elementAt(3)).equals("test.mcUser"));
        assertTrue(service.getProperty(SKIN_NAME,keys.elementAt(4)).equals("testThirdUser"));
        assertTrue(service.getProperty(SKIN_NAME,keys.elementAt(5)).equals("test.mcThirdUser"));
        assertTrue(service.getProperty(SKIN_NAME,keys.elementAt(6)).equals("organization"));
        assertTrue(service.getProperty(SKIN_NAME,keys.elementAt(7)).equals("ESS-DIVE"));
        SkinUtil.setSkinName(originalSkinNames);
    }

}
