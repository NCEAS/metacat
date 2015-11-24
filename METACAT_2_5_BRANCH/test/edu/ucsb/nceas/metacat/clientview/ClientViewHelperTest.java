/*
 * ClientViewHelperTest.java
 * JUnit based test
 *
 * Created on October 25, 2007, 5:11 PM
 */

package edu.ucsb.nceas.metacat.clientview;

import java.io.ByteArrayOutputStream;
import java.io.File;
import junit.framework.*;
import com.oreilly.servlet.multipart.MultipartParser;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Stack;
import java.util.TreeMap;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.xml.xpath.XPath;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import edu.ucsb.nceas.metacat.client.MetacatClient;
import edu.ucsb.nceas.metacat.properties.PropertyService;

/**
 * ClientViewHelper JUnit tests
 * @author barteau
 */
public class ClientViewHelperTest extends TestCase {
    ClientViewHelper            instance;
    ClientView                  clientViewBean;
    
//    final static String         PATH_TO_PROPERTIES = "projects/metacat/build/war/WEB-INF/metacat.properties";
    final static String         PATH_TO_PROPERTIES = "projects/metacat/build/war/WEB-INF";
    final static String         USR = "";
    final static String         ORG = "NCEAS";
    final static String         PWD = "";
    final static String         DOWNLOAD_PACKAGE_DOCID = "";
    final static String         DOWNLOAD_DATA_DOCID = "";
    final static String         HOST = "localhost:8084";
    final static String         CONTEXT = "/sanparks";
    
    /**
     * Constructor
     * @param testName String
     */
    public ClientViewHelperTest(String testName) {
        super(testName);
    }
    
    protected void setUp() throws Exception {
        if (PWD.equals("")) 
            fail("Please set the class property PWD before running this test.");
        if (USR.equals("")) 
            fail("Please set the class property USR before running this test.");
        if (DOWNLOAD_PACKAGE_DOCID.equals("")) 
            fail("Please set the class property DOWNLOAD_PACKAGE_DOCID before running this test.");
        if (DOWNLOAD_DATA_DOCID.equals("")) 
            fail("Please set the class property DOWNLOAD_DATA_DOCID before running this test.");
        
//        PropertyService.getInstance(PATH_TO_PROPERTIES);
        PropertyService.getInstance();
        
        clientViewBean = new ClientView();
        clientViewBean.setUsername(USR);
        clientViewBean.setOrganization(ORG);
        clientViewBean.setPassword(PWD);
        
        instance = new ClientViewHelper(HOST, CONTEXT, clientViewBean);
    }
    
    protected void tearDown() throws Exception {
        clientViewBean = null;
        instance = null;
    }
    
    /**
     * Test of handleClientRequest method, of class edu.ucsb.nceas.metacat.clientview.ClientViewHelper.
     */
    public void testHandleClientRequest() {
        HashMap             expResult, result;
        String              expResultTxt, returnResultTxt;
        
        System.out.println("handleClientRequest");
        
        
        System.out.print("...setLoggedIn");
        clientViewBean.setAction("Login");
        
        result = instance.handleClientRequest(null);
        returnResultTxt = (String) result.get("message");
        assertTrue("Login: Failed to login", (returnResultTxt.indexOf("Authentication successful for user:") > -1));
        System.out.println(" ...success!");
        
        
        System.out.print("...download (data file)");
        clientViewBean.setAction("Download");
        clientViewBean.setDocId(DOWNLOAD_DATA_DOCID);
        clientViewBean.setMetaFileDocId(DOWNLOAD_PACKAGE_DOCID);
        
        result = instance.handleClientRequest(null);
        returnResultTxt = (String) result.get("contentType");
        assertEquals("Download: Content Type is incorrect for a data file download", returnResultTxt, "application/octet-stream");
        returnResultTxt = (String) result.get("Content-Disposition");
        assertNotNull("Download: Content-Disposition is not set", returnResultTxt);
        assertNotNull("Download: OutputStream is not set", result.get("outputStream"));
        assertTrue("Download: Returned an empty file", ((ByteArrayOutputStream) result.get("outputStream")).size() > 0);
        System.out.println(" ...success!");
        
        
        System.out.print("...download (package zip file)");
        clientViewBean.setDocId(DOWNLOAD_PACKAGE_DOCID);
        clientViewBean.setMetaFileDocId(DOWNLOAD_PACKAGE_DOCID);
        
        result = instance.handleClientRequest(null);
        returnResultTxt = (String) result.get("contentType");
        assertEquals("Download: Content Type is incorrect for a package download", returnResultTxt, "application/zip");
        returnResultTxt = (String) result.get("Content-Disposition");
        assertNotNull("Download: Content-Disposition is not set", returnResultTxt);
        assertNotNull("Download: OutputStream is not set", result.get("outputStream"));
        assertTrue("Download: Returned an empty file", ((ByteArrayOutputStream) result.get("outputStream")).size() > 0);
        System.out.println(" ...success!");

        
        System.out.print("...logout");
        clientViewBean.setAction("Logout");
        
        result = instance.handleClientRequest(null);
        assertNull("Logout: Session ID still exists", clientViewBean.getSessionid());
        System.out.println(" ...success!");
        
    }
}
