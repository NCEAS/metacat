package edu.ucsb.nceas.metacat.dataone;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigInteger;
import java.util.Collections;
import java.util.Date;
import java.util.Vector;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.apache.wicket.protocol.http.mock.MockHttpServletRequest;
import org.dataone.client.D1Node;
import org.dataone.client.NodeLocator;
import org.dataone.client.exception.ClientSideException;
import org.dataone.client.v2.CNode;
import org.dataone.client.v2.itk.D1Client;
import org.dataone.client.v2.formats.ObjectFormatCache;
import org.dataone.service.types.v1.AccessPolicy;
import org.dataone.service.types.v1.AccessRule;
import org.dataone.service.types.v1.Checksum;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v2.Node;
import org.dataone.service.types.v2.ObjectFormatList;
import org.dataone.service.types.v1.NodeReference;
import org.dataone.service.types.v1.NodeType;
import org.dataone.service.types.v1.Permission;
import org.dataone.service.types.v1.Session;
import org.dataone.service.types.v1.Subject;
import org.dataone.service.types.v2.SystemMetadata;
import org.dataone.service.types.v1.comparators.SystemMetadataDateUploadedComparator;
import org.dataone.service.types.v1.util.ChecksumUtil;
import org.dataone.service.types.v2.util.ObjectFormatServiceImpl;
import org.dataone.service.util.Constants;
import org.dataone.service.util.TypeMarshaller;


import edu.ucsb.nceas.MCTestCase;
import edu.ucsb.nceas.metacat.client.Metacat;
import edu.ucsb.nceas.metacat.client.MetacatFactory;

/**
 * A class for testing the scenarios of getting the the head version of an SID chain
 */
public class SIDTest extends MCTestCase {   
    
    private static final String OBSOLETES = "obsoletes";
    private static final String OBSOLETEDBY = "obsoletedBy";
   
	/**
    * constructor for the test
    */
    public SIDTest(String name) {
        super(name);
    }
  
    /**
	 * Establish a testing framework by initializing appropriate objects
	 */
    public void setUp() throws Exception {
    	
    }

	/**
	 * Release any objects after tests are complete
	 */
	public void tearDown() {
		
	}
	
	/**
     * Create a suite of tests to be run together
     */
    public static Test suite() 
    {
        TestSuite suite = new TestSuite();
        suite.addTest(new SIDTest("initialize"));
        suite.addTest(new SIDTest("testCases"));
        return suite;
    }
	
	
	
	/**
	 * Run an initial test that always passes to check that the test harness is
	 * working.
	 */
	public void initialize() 
	{
		assertTrue(1 == 1);
	}
	
	public void testCases() throws Exception {
	    testCase1();
	    testCase2();
	    testCase3();
	    testCase4();
	    testCase5();
	    testCase6();
	    testCase7();
	    testCase8();
	    testCase9();
	    testCase10();
	    testCase11();
	    testCase12();
	    testCase13();
	    testCase14();
	    testCase15();
	    testCase16();
        testCase17();
        testCase18();
	}
	
	/**
	 * case 1. P1(S1) <-> P2(S1),  S1 = P2 (Rule 1)
	 */
	private void testCase1() throws Exception {
	    Identifier s1 = new Identifier();
        s1.setValue("S1");
        Identifier s2 = new Identifier();
        s2.setValue("S2");
        Identifier p1 = new Identifier();
        p1.setValue("P1");
        Identifier p2 = new Identifier();
        p2.setValue("P2");
       
        SystemMetadata p1Sys = new SystemMetadata();
        p1Sys.setIdentifier(p1);
        p1Sys.setSeriesId(s1);
        p1Sys.setObsoletedBy(p2);
        p1Sys.setDateUploaded(new Date(100));
        
        SystemMetadata p2Sys = new SystemMetadata();
        p2Sys.setIdentifier(p2);
        p2Sys.setSeriesId(s1);
        p2Sys.setObsoletes(p1);
        p2Sys.setDateUploaded(new Date(200));
        
        Vector<SystemMetadata> chain = new Vector<SystemMetadata>();
        chain.add(p1Sys);
        chain.add(p2Sys);
      
        System.out.println("Case 1:");
        Identifier head = getHeadVersion(s1, chain);
        //System.out.println("The head is "+head.getValue());
        assertTrue(head.equals(p2));
	}
	
	
	/**
     * Case 2. P1(S1) ? P2(S1), S1 = P2, Error condition, P2 not allowed (should not exist) (Rule 2)
     */
    private void testCase2() throws Exception {
        Identifier s1 = new Identifier();
        s1.setValue("S1");
        Identifier s2 = new Identifier();
        s2.setValue("S2");
        Identifier p1 = new Identifier();
        p1.setValue("P1");
        Identifier p2 = new Identifier();
        p2.setValue("P2");
       
        
        SystemMetadata p1Sys = new SystemMetadata();
        p1Sys.setIdentifier(p1);
        p1Sys.setSeriesId(s1);
        p1Sys.setDateUploaded(new Date(100));
        
        SystemMetadata p2Sys = new SystemMetadata();
        p2Sys.setIdentifier(p2);
        p2Sys.setSeriesId(s1);
        p2Sys.setDateUploaded(new Date(200));
        
        Vector<SystemMetadata> chain = new Vector<SystemMetadata>();
        chain.add(p1Sys);
        chain.add(p2Sys);
      
        System.out.println("Case 2:");
        Identifier head = getHeadVersion(s1, chain);
        //System.out.println("The head is "+head.getValue());
        assertTrue(head.equals(p2));
    }
    
    /**
     * case 3. P1(S1) <- P2(S1), S1 = P2, Discouraged, but not error condition, S1 = P2 (Rule 2, missingFields)
     */
    private void testCase3() throws Exception {
        Identifier s1 = new Identifier();
        s1.setValue("S1");
        Identifier s2 = new Identifier();
        s2.setValue("S2");
        Identifier p1 = new Identifier();
        p1.setValue("P1");
        Identifier p2 = new Identifier();
        p2.setValue("P2");
       
        
        SystemMetadata p1Sys = new SystemMetadata();
        p1Sys.setIdentifier(p1);
        p1Sys.setSeriesId(s1);
        p1Sys.setDateUploaded(new Date(100));
        
        SystemMetadata p2Sys = new SystemMetadata();
        p2Sys.setIdentifier(p2);
        p2Sys.setSeriesId(s1);
        p2Sys.setObsoletes(p1);
        p2Sys.setDateUploaded(new Date(200));
        
        Vector<SystemMetadata> chain = new Vector<SystemMetadata>();
        chain.add(p1Sys);
        chain.add(p2Sys);
      
        System.out.println("Case 3:");
        Identifier head = getHeadVersion(s1, chain);
        //System.out.println("The head is "+head.getValue());
        assertTrue(head.equals(p2));
    }
    
    /**
     * case 4. P1(S1) <-> P2(S1) <-> P3(S2), S1 = P2(use Rule 3), S2 = P3 (use Rule 1)
     */
    private void testCase4() throws Exception {
        Identifier s1 = new Identifier();
        s1.setValue("S1");
        Identifier s2 = new Identifier();
        s2.setValue("S2");
        Identifier p1 = new Identifier();
        p1.setValue("P1");
        Identifier p2 = new Identifier();
        p2.setValue("P2");
        Identifier p3 = new Identifier();
        p3.setValue("P3");
       
        
        SystemMetadata p1Sys = new SystemMetadata();
        p1Sys.setIdentifier(p1);
        p1Sys.setSeriesId(s1);
        p1Sys.setObsoletedBy(p2);
        p1Sys.setDateUploaded(new Date(100));
        
        SystemMetadata p2Sys = new SystemMetadata();
        p2Sys.setIdentifier(p2);
        p2Sys.setSeriesId(s1);
        p2Sys.setObsoletes(p1);
        p2Sys.setObsoletedBy(p3);
        p2Sys.setDateUploaded(new Date(200));
        
        SystemMetadata p3Sys = new SystemMetadata();
        p3Sys.setIdentifier(p3);
        p3Sys.setSeriesId(s2);
        p3Sys.setObsoletes(p2);
        p3Sys.setDateUploaded(new Date(300));

        Vector<SystemMetadata> chain = new Vector<SystemMetadata>();
        chain.add(p1Sys);
        chain.add(p2Sys);
        chain.add(p3Sys);
      
        System.out.println("Case 4:");
        Identifier head = getHeadVersion(s1, chain);
        //System.out.println("The head is "+head.getValue());
        assertTrue(head.equals(p2));
        Identifier head2 = getHeadVersion(s2, chain);
        //System.out.println("The head is "+head.getValue());
        assertTrue(head2.equals(p3));
    }
    
    /**
     * case 5. P1(S1) <- P2(S1) <- P3(S2), S1 = P2 (use Rule 2 or have missing field), S2 = P3  (use Rule 1)
     */
    private void testCase5() throws Exception {
        Identifier s1 = new Identifier();
        s1.setValue("S1");
        Identifier s2 = new Identifier();
        s2.setValue("S2");
        Identifier p1 = new Identifier();
        p1.setValue("P1");
        Identifier p2 = new Identifier();
        p2.setValue("P2");
        Identifier p3 = new Identifier();
        p3.setValue("P3");
       
        
        SystemMetadata p1Sys = new SystemMetadata();
        p1Sys.setIdentifier(p1);
        p1Sys.setSeriesId(s1);
        p1Sys.setDateUploaded(new Date(100));
        
        SystemMetadata p2Sys = new SystemMetadata();
        p2Sys.setIdentifier(p2);
        p2Sys.setSeriesId(s1);
        p2Sys.setObsoletes(p1);
        p2Sys.setDateUploaded(new Date(200));
        
        SystemMetadata p3Sys = new SystemMetadata();
        p3Sys.setIdentifier(p3);
        p3Sys.setSeriesId(s2);
        p3Sys.setObsoletes(p2);
        p3Sys.setDateUploaded(new Date(300));

        Vector<SystemMetadata> chain = new Vector<SystemMetadata>();
        chain.add(p1Sys);
        chain.add(p2Sys);
        chain.add(p3Sys);
      
        System.out.println("Case 5:");
        Identifier head = getHeadVersion(s1, chain);
        //System.out.println("The head is "+head.getValue());
        assertTrue(head.equals(p2));
        Identifier head2 = getHeadVersion(s2, chain);
        //System.out.println("The head is "+head.getValue());
        assertTrue(head2.equals(p3));
    }
    
    /**
     * case 6. P1(S1) <-> P2(S1) <-> P3(), S1 = P2 (use Rule 3)
     */
    private void testCase6() throws Exception {
        Identifier s1 = new Identifier();
        s1.setValue("S1");
        Identifier s2 = new Identifier();
        s2.setValue("S2");
        Identifier p1 = new Identifier();
        p1.setValue("P1");
        Identifier p2 = new Identifier();
        p2.setValue("P2");
        Identifier p3 = new Identifier();
        p3.setValue("P3");
       
        
        SystemMetadata p1Sys = new SystemMetadata();
        p1Sys.setIdentifier(p1);
        p1Sys.setSeriesId(s1);
        p1Sys.setObsoletedBy(p2);
        p1Sys.setDateUploaded(new Date(100));
        
        SystemMetadata p2Sys = new SystemMetadata();
        p2Sys.setIdentifier(p2);
        p2Sys.setSeriesId(s1);
        p2Sys.setObsoletes(p1);
        p2Sys.setObsoletedBy(p3);
        p2Sys.setDateUploaded(new Date(200));
        
        SystemMetadata p3Sys = new SystemMetadata();
        p3Sys.setIdentifier(p3);
        //p3Sys.setSeriesId(s2);
        p3Sys.setObsoletes(p2);
        p3Sys.setDateUploaded(new Date(300));

        Vector<SystemMetadata> chain = new Vector<SystemMetadata>();
        chain.add(p1Sys);
        chain.add(p2Sys);
        chain.add(p3Sys);
      
        System.out.println("Case 6:");
        Identifier head = getHeadVersion(s1, chain);
        //System.out.println("The head is "+head.getValue());
        assertTrue(head.equals(p2));
    }
    
    /**
     * case 7. P1(S1) <-> P2(S1) <-> P3() <-> P4(S2), S1 = P2 (Rule 3), S2 = P4 (Rule 1)
     */
    private void testCase7() throws Exception {
        Identifier s1 = new Identifier();
        s1.setValue("S1");
        Identifier s2 = new Identifier();
        s2.setValue("S2");
        Identifier p1 = new Identifier();
        p1.setValue("P1");
        Identifier p2 = new Identifier();
        p2.setValue("P2");
        Identifier p3 = new Identifier();
        p3.setValue("P3");
        Identifier p4 = new Identifier();
        p4.setValue("P4");
       
        
        SystemMetadata p1Sys = new SystemMetadata();
        p1Sys.setIdentifier(p1);
        p1Sys.setSeriesId(s1);
        p1Sys.setObsoletedBy(p2);
        p1Sys.setDateUploaded(new Date(100));
        
        SystemMetadata p2Sys = new SystemMetadata();
        p2Sys.setIdentifier(p2);
        p2Sys.setSeriesId(s1);
        p2Sys.setObsoletes(p1);
        p2Sys.setObsoletedBy(p3);
        p2Sys.setDateUploaded(new Date(200));
        
        SystemMetadata p3Sys = new SystemMetadata();
        p3Sys.setIdentifier(p3);
        //p3Sys.setSeriesId(s2);
        p3Sys.setObsoletes(p2);
        p3Sys.setObsoletedBy(p4);
        p3Sys.setDateUploaded(new Date(300));
        
        SystemMetadata p4Sys = new SystemMetadata();
        p4Sys.setIdentifier(p4);
        p4Sys.setSeriesId(s2);
        p4Sys.setObsoletes(p3);
        p4Sys.setDateUploaded(new Date(400));

        Vector<SystemMetadata> chain = new Vector<SystemMetadata>();
        chain.add(p1Sys);
        chain.add(p2Sys);
        chain.add(p3Sys);
        chain.add(p4Sys);
      
        System.out.println("Case 7:");
        Identifier head = getHeadVersion(s1, chain);
        //System.out.println("The head is "+head.getValue());
        assertTrue(head.equals(p2));
        Identifier head2 = getHeadVersion(s2, chain);
        //System.out.println("The head is "+head.getValue());
        assertTrue(head2.equals(p4));
    }
    
    /**
     * case 8. P1(S1) <-> P2(S1) ->  ??  <- P4(S1), S1 = P4, (Rule 1) (Error, but will happen)
     */
    private void testCase8() throws Exception {
        Identifier s1 = new Identifier();
        s1.setValue("S1");
        Identifier s2 = new Identifier();
        s2.setValue("S2");
        Identifier p1 = new Identifier();
        p1.setValue("P1");
        Identifier p2 = new Identifier();
        p2.setValue("P2");
        Identifier p3 = new Identifier();
        p3.setValue("P3");
        Identifier p4 = new Identifier();
        p4.setValue("P4");
       
        
        SystemMetadata p1Sys = new SystemMetadata();
        p1Sys.setIdentifier(p1);
        p1Sys.setSeriesId(s1);
        p1Sys.setObsoletedBy(p2);
        p1Sys.setDateUploaded(new Date(100));
        
        SystemMetadata p2Sys = new SystemMetadata();
        p2Sys.setIdentifier(p2);
        p2Sys.setSeriesId(s1);
        p2Sys.setObsoletes(p1);
        p2Sys.setObsoletedBy(p3);
        p2Sys.setDateUploaded(new Date(200));
        
        
        SystemMetadata p4Sys = new SystemMetadata();
        p4Sys.setIdentifier(p4);
        p4Sys.setSeriesId(s1);
        p4Sys.setObsoletes(p3);
        p4Sys.setDateUploaded(new Date(400));

        Vector<SystemMetadata> chain = new Vector<SystemMetadata>();
        chain.add(p1Sys);
        chain.add(p2Sys);
        chain.add(p4Sys);
      
        System.out.println("Case 8:");
        Identifier head = getHeadVersion(s1, chain);
        //System.out.println("The head is "+head.getValue());
        assertTrue(head.equals(p4));
    }
    
    /**
     * case 9. P1(S1) <-> P2(S1)  ??  <- P4(S1), S1 = P4 (Rule 2) (??: object was not synchronized)
     */
    private void testCase9() throws Exception {
        Identifier s1 = new Identifier();
        s1.setValue("S1");
        Identifier s2 = new Identifier();
        s2.setValue("S2");
        Identifier p1 = new Identifier();
        p1.setValue("P1");
        Identifier p2 = new Identifier();
        p2.setValue("P2");
        Identifier p3 = new Identifier();
        p3.setValue("P3");
        Identifier p4 = new Identifier();
        p4.setValue("P4");
       
        
        SystemMetadata p1Sys = new SystemMetadata();
        p1Sys.setIdentifier(p1);
        p1Sys.setSeriesId(s1);
        p1Sys.setObsoletedBy(p2);
        p1Sys.setDateUploaded(new Date(100));
        
        SystemMetadata p2Sys = new SystemMetadata();
        p2Sys.setIdentifier(p2);
        p2Sys.setSeriesId(s1);
        p2Sys.setObsoletes(p1);
        p2Sys.setDateUploaded(new Date(200));
        
        
        SystemMetadata p4Sys = new SystemMetadata();
        p4Sys.setIdentifier(p4);
        p4Sys.setSeriesId(s1);
        p4Sys.setObsoletes(p3);
        p4Sys.setDateUploaded(new Date(400));

        Vector<SystemMetadata> chain = new Vector<SystemMetadata>();
        chain.add(p1Sys);
        chain.add(p2Sys);
        chain.add(p4Sys);
      
        System.out.println("Case 9:");
        Identifier head = getHeadVersion(s1, chain);
        //System.out.println("The head is "+head.getValue());
        assertTrue(head.equals(p4));
    }
    
    /**
     * case 10: P1(S1) <-> P2(S1) ->  XX  <- P4(S1), S1 = P4, (Rule 1) (XX: object P3 was deleted)
     */
    private void testCase10() throws Exception {
        Identifier s1 = new Identifier();
        s1.setValue("S1");
        Identifier s2 = new Identifier();
        s2.setValue("S2");
        Identifier p1 = new Identifier();
        p1.setValue("P1");
        Identifier p2 = new Identifier();
        p2.setValue("P2");
        Identifier p3 = new Identifier();
        p3.setValue("P3");
        Identifier p4 = new Identifier();
        p4.setValue("P4");
       
        
        SystemMetadata p1Sys = new SystemMetadata();
        p1Sys.setIdentifier(p1);
        p1Sys.setSeriesId(s1);
        p1Sys.setObsoletedBy(p2);
        p1Sys.setDateUploaded(new Date(100));
        
        SystemMetadata p2Sys = new SystemMetadata();
        p2Sys.setIdentifier(p2);
        p2Sys.setSeriesId(s1);
        p2Sys.setObsoletes(p1);
        p2Sys.setObsoletedBy(p3);
        p2Sys.setDateUploaded(new Date(200));
        
        
        SystemMetadata p4Sys = new SystemMetadata();
        p4Sys.setIdentifier(p4);
        p4Sys.setSeriesId(s1);
        p4Sys.setObsoletes(p3);
        p4Sys.setDateUploaded(new Date(400));

        Vector<SystemMetadata> chain = new Vector<SystemMetadata>();
        chain.add(p1Sys);
        chain.add(p2Sys);
        chain.add(p4Sys);
      
        System.out.println("Case 10:");
        Identifier head = getHeadVersion(s1, chain);
        //System.out.println("The head is "+head.getValue());
        assertTrue(head.equals(p4));
    }
    
    /**
     * case 11: P1(S1) <-> P2(S1) <-> [archived:P3(S1)], S1 = P3, (Rule 1) 
     */
    private void testCase11() throws Exception {
        Identifier s1 = new Identifier();
        s1.setValue("S1");
        Identifier s2 = new Identifier();
        s2.setValue("S2");
        Identifier p1 = new Identifier();
        p1.setValue("P1");
        Identifier p2 = new Identifier();
        p2.setValue("P2");
        Identifier p3 = new Identifier();
        p3.setValue("P3");
        Identifier p4 = new Identifier();
        p4.setValue("P4");
       
        
        SystemMetadata p1Sys = new SystemMetadata();
        p1Sys.setIdentifier(p1);
        p1Sys.setSeriesId(s1);
        p1Sys.setObsoletedBy(p2);
        p1Sys.setDateUploaded(new Date(100));
        
        SystemMetadata p2Sys = new SystemMetadata();
        p2Sys.setIdentifier(p2);
        p2Sys.setSeriesId(s1);
        p2Sys.setObsoletes(p1);
        p2Sys.setObsoletedBy(p3);
        p2Sys.setDateUploaded(new Date(200));
        
        
        SystemMetadata p3Sys = new SystemMetadata();
        p3Sys.setIdentifier(p3);
        p3Sys.setSeriesId(s1);
        p3Sys.setObsoletes(p2);
        p3Sys.setArchived(true);
        p3Sys.setDateUploaded(new Date(300));
        

        Vector<SystemMetadata> chain = new Vector<SystemMetadata>();
        chain.add(p1Sys);
        chain.add(p2Sys);
        chain.add(p3Sys);
      
        System.out.println("Case 11:");
        Identifier head = getHeadVersion(s1, chain);
        //System.out.println("The head is "+head.getValue());
        assertTrue(head.equals(p3));
    }
    
    /**
     * case 12. P1(S1) <-> P2(S1) -> ??, S1 = P2, (Rule 4) (Error, but will happen)
     * @throws Exception
     */
    private void testCase12() throws Exception {
        Identifier s1 = new Identifier();
        s1.setValue("S1");
        Identifier s2 = new Identifier();
        s2.setValue("S2");
        Identifier p1 = new Identifier();
        p1.setValue("P1");
        Identifier p2 = new Identifier();
        p2.setValue("P2");
        Identifier p3 = new Identifier();
        p3.setValue("P3");
        
        SystemMetadata p1Sys = new SystemMetadata();
        p1Sys.setIdentifier(p1);
        p1Sys.setSeriesId(s1);
        p1Sys.setObsoletedBy(p2);
        p1Sys.setDateUploaded(new Date(100));
        
        SystemMetadata p2Sys = new SystemMetadata();
        p2Sys.setIdentifier(p2);
        p2Sys.setSeriesId(s1);
        p2Sys.setObsoletes(p1);
        p2Sys.setObsoletedBy(p3);
        p2Sys.setDateUploaded(new Date(200));
        

        Vector<SystemMetadata> chain = new Vector<SystemMetadata>();
        chain.add(p1Sys);
        chain.add(p2Sys);

        System.out.println("Case 12:");
        Identifier head = getHeadVersion(s1, chain);
        //System.out.println("The head is "+head.getValue());
        assertTrue(head.equals(p2));
    }
    
    /**
     * case 13. P1(S1) <- P2(S1) -> ??, S1 = P2
     * @throws Exception
     */
    private void testCase13() throws Exception {
        Identifier s1 = new Identifier();
        s1.setValue("S1");
        Identifier s2 = new Identifier();
        s2.setValue("S2");
        Identifier p1 = new Identifier();
        p1.setValue("P1");
        Identifier p2 = new Identifier();
        p2.setValue("P2");
        Identifier p3 = new Identifier();
        p3.setValue("P3");
        
        SystemMetadata p1Sys = new SystemMetadata();
        p1Sys.setIdentifier(p1);
        p1Sys.setSeriesId(s1);
        p1Sys.setDateUploaded(new Date(100));
        
        SystemMetadata p2Sys = new SystemMetadata();
        p2Sys.setIdentifier(p2);
        p2Sys.setSeriesId(s1);
        p2Sys.setObsoletes(p1);
        p2Sys.setObsoletedBy(p3);
        p2Sys.setDateUploaded(new Date(200));
        

        Vector<SystemMetadata> chain = new Vector<SystemMetadata>();
        chain.add(p1Sys);
        chain.add(p2Sys);
      
        
        System.out.println("Case 13:");
        Identifier head = getHeadVersion(s1, chain);
        //System.out.println("The head is "+head.getValue());
        assertTrue(head.equals(p2));
    }
	
	/**
	 * Case 14: P1(S1) <- P2(S1) -> P3(S2).
	 * It has a missing obsoletedBy fields, we use the uploadedDate field.S1 = P2
	 * @throws Exception
	 */
	private void testCase14() throws Exception {
	    Identifier s1 = new Identifier();
	    s1.setValue("S1");
	    Identifier s2 = new Identifier();
        s2.setValue("S2");
	    Identifier p1 = new Identifier();
	    p1.setValue("P1");
	    Identifier p2 = new Identifier();
        p2.setValue("P2");
        Identifier p3 = new Identifier();
        p3.setValue("P3");
	    
        SystemMetadata p1Sys = new SystemMetadata();
	    p1Sys.setIdentifier(p1);
	    p1Sys.setSeriesId(s1);
	    p1Sys.setDateUploaded(new Date(100));
	    
	    SystemMetadata p2Sys = new SystemMetadata();
        p2Sys.setIdentifier(p2);
        p2Sys.setSeriesId(s1);
        p2Sys.setObsoletes(p1);
        p2Sys.setObsoletedBy(p3);
        p2Sys.setDateUploaded(new Date(200));
        
        SystemMetadata p3Sys = new SystemMetadata();
        p3Sys.setIdentifier(p3);
        p3Sys.setSeriesId(s2);
        p3Sys.setDateUploaded(new Date(300));
        
        Vector<SystemMetadata> chain = new Vector<SystemMetadata>();
        chain.add(p1Sys);
        chain.add(p2Sys);
        chain.add(p3Sys);
        
        System.out.println("Case 14:");
        Identifier head = getHeadVersion(s1, chain);
        //System.out.println("The head is "+head.getValue());
        assertTrue(head.equals(p2));
	}
	
	/**
     * case 15. P1(S1) <-> P2(S1)  ?? <- P4(S1) <-> P5(S2), S1 = P4 (Rule 1) 
     * @throws Exception
     */
    private void testCase15() throws Exception {
        Identifier s1 = new Identifier();
        s1.setValue("S1");
        Identifier s2 = new Identifier();
        s2.setValue("S2");
        Identifier p1 = new Identifier();
        p1.setValue("P1");
        Identifier p2 = new Identifier();
        p2.setValue("P2");
        Identifier p3 = new Identifier();
        p3.setValue("P3");
        Identifier p4 = new Identifier();
        p4.setValue("P4");
        Identifier p5 = new Identifier();
        p5.setValue("P5");
        
        SystemMetadata p1Sys = new SystemMetadata();
        p1Sys.setIdentifier(p1);
        p1Sys.setSeriesId(s1);
        p1Sys.setObsoletedBy(p2);
        p1Sys.setDateUploaded(new Date(100));
        
        SystemMetadata p2Sys = new SystemMetadata();
        p2Sys.setIdentifier(p2);
        p2Sys.setSeriesId(s1);
        p2Sys.setObsoletes(p1);
        p2Sys.setDateUploaded(new Date(200));
        
        SystemMetadata p4Sys = new SystemMetadata();
        p4Sys.setIdentifier(p4);
        p4Sys.setSeriesId(s1);
        p4Sys.setObsoletes(p3);
        p4Sys.setObsoletedBy(p5);
        p4Sys.setDateUploaded(new Date(400));
        
        SystemMetadata p5Sys = new SystemMetadata();
        p5Sys.setIdentifier(p5);
        p5Sys.setSeriesId(s2);
        p5Sys.setObsoletes(p4);
        p5Sys.setDateUploaded(new Date(500));
        
        
        Vector<SystemMetadata> chain = new Vector<SystemMetadata>();
        chain.add(p1Sys);
        chain.add(p2Sys);
        chain.add(p4Sys);
        chain.add(p5Sys);
        
        System.out.println("Case 15:");
        Identifier head = getHeadVersion(s1, chain);
        //System.out.println("The head is "+head.getValue());
        assertTrue(head.equals(p4));
    }
    
    /**
     * case 16. P1(S1) <- P2(S1) -> ?? <-P4(S2) S1 = P2 (two ends, not an ideal chain), S2=P4 (rule1)
     * @throws Exception
     */
    private void testCase16() throws Exception {
        Identifier s1 = new Identifier();
        s1.setValue("S1");
        Identifier s2 = new Identifier();
        s2.setValue("S2");
        Identifier p1 = new Identifier();
        p1.setValue("P1");
        Identifier p2 = new Identifier();
        p2.setValue("P2");
        Identifier p3 = new Identifier();
        p3.setValue("P3");
        Identifier p4 = new Identifier();
        p4.setValue("P4");
       
        
        SystemMetadata p1Sys = new SystemMetadata();
        p1Sys.setIdentifier(p1);
        p1Sys.setSeriesId(s1);
        //p1Sys.setObsoletedBy(p2);
        p1Sys.setDateUploaded(new Date(100));
        
        SystemMetadata p2Sys = new SystemMetadata();
        p2Sys.setIdentifier(p2);
        p2Sys.setSeriesId(s1);
        p2Sys.setObsoletes(p1);
        p2Sys.setObsoletedBy(p3);
        p2Sys.setDateUploaded(new Date(200));
        
        SystemMetadata p4Sys = new SystemMetadata();
        p4Sys.setIdentifier(p4);
        p4Sys.setSeriesId(s2);
        p4Sys.setObsoletes(p3);
        p4Sys.setDateUploaded(new Date(400));
        
 
        
        Vector<SystemMetadata> chain = new Vector<SystemMetadata>();
        chain.add(p1Sys);
        chain.add(p2Sys);
        chain.add(p4Sys);
        
        
        System.out.println("Case 16:");
        Identifier head = getHeadVersion(s1, chain);
        //System.out.println("The head is "+head.getValue());
        assertTrue(head.equals(p2));
        Identifier head2 = getHeadVersion(s2, chain);
        //System.out.println("The head is "+head.getValue());
        assertTrue(head2.equals(p4));
    }
    
    /**
     * case 17. P1(S1) <- P2(S1) -> ?? <-P4(S1) S1 = P4 (P1 and P4 are two ends, not an ideal chain)
     * @throws Exception
     */
    private void testCase17() throws Exception {
        Identifier s1 = new Identifier();
        s1.setValue("S1");
        Identifier s2 = new Identifier();
        s2.setValue("S2");
        Identifier p1 = new Identifier();
        p1.setValue("P1");
        Identifier p2 = new Identifier();
        p2.setValue("P2");
        Identifier p3 = new Identifier();
        p3.setValue("P3");
        Identifier p4 = new Identifier();
        p4.setValue("P4");
       
        
        SystemMetadata p1Sys = new SystemMetadata();
        p1Sys.setIdentifier(p1);
        p1Sys.setSeriesId(s1);
        //p1Sys.setObsoletedBy(p2);
        p1Sys.setDateUploaded(new Date(100));
        
        SystemMetadata p2Sys = new SystemMetadata();
        p2Sys.setIdentifier(p2);
        p2Sys.setSeriesId(s1);
        p2Sys.setObsoletes(p1);
        p2Sys.setObsoletedBy(p3);
        p2Sys.setDateUploaded(new Date(200));
        
        SystemMetadata p4Sys = new SystemMetadata();
        p4Sys.setIdentifier(p4);
        p4Sys.setSeriesId(s1);
        p4Sys.setObsoletes(p3);
        p4Sys.setDateUploaded(new Date(400));
        
 
        
        Vector<SystemMetadata> chain = new Vector<SystemMetadata>();
        chain.add(p1Sys);
        chain.add(p2Sys);
        chain.add(p4Sys);
        
        
        System.out.println("Case 17:");
        Identifier head = getHeadVersion(s1, chain);
        //System.out.println("The head is "+head.getValue());
        assertTrue(head.equals(p4));
      
    }
    
    /**
     * case 18. P1(S1) <->P2(S1) -> ??  ???<-P5(S1) S1 = P5 (P2 is a type 2 end and P4  is a type 1 end, not an ideal chain)
     * @throws Exception
     */
    private void testCase18() throws Exception {
        Identifier s1 = new Identifier();
        s1.setValue("S1");
        Identifier s2 = new Identifier();
        s2.setValue("S2");
        Identifier p1 = new Identifier();
        p1.setValue("P1");
        Identifier p2 = new Identifier();
        p2.setValue("P2");
        Identifier p3 = new Identifier();
        p3.setValue("P3");
        Identifier p4 = new Identifier();
        p4.setValue("P4");
        Identifier p5 = new Identifier();
        p5.setValue("P5");
       
        
        SystemMetadata p1Sys = new SystemMetadata();
        p1Sys.setIdentifier(p1);
        p1Sys.setSeriesId(s1);
        p1Sys.setObsoletedBy(p2);
        p1Sys.setDateUploaded(new Date(100));
        
        SystemMetadata p2Sys = new SystemMetadata();
        p2Sys.setIdentifier(p2);
        p2Sys.setSeriesId(s1);
        p2Sys.setObsoletes(p1);
        p2Sys.setObsoletedBy(p3);
        p2Sys.setDateUploaded(new Date(200));
        
        SystemMetadata p5Sys = new SystemMetadata();
        p5Sys.setIdentifier(p5);
        p5Sys.setSeriesId(s1);
        p5Sys.setObsoletes(p4);
        p5Sys.setDateUploaded(new Date(500));
        
 
        
        Vector<SystemMetadata> chain = new Vector<SystemMetadata>();
        chain.add(p1Sys);
        chain.add(p2Sys);
        chain.add(p5Sys);
        
        
        System.out.println("Case 18:");
        Identifier head = getHeadVersion(s1, chain);
        //System.out.println("The head is "+head.getValue());
        assertTrue(head.equals(p5));
      
    }
	
	
	/*
	 * completed the obsoletes and obsoletedBy information for the given pid. 
	 * We will look up the information from the given chain if its obsoletes or obsoletedBy field is missing.
	 */
	private void decorateSystemMetadata(SystemMetadata targetSysmeta, Vector<SystemMetadata> chain) {
	    if(targetSysmeta != null) {
	        if (targetSysmeta.getObsoletes() == null && targetSysmeta.getObsoletedBy() == null) {
	            Identifier obsoletes = getRelatedIdentifier(targetSysmeta.getIdentifier(), OBSOLETES, chain);
	            if(obsoletes != null) {
	                targetSysmeta.setObsoletedBy(obsoletes);
	            }
	            Identifier obsoleted = getRelatedIdentifier(targetSysmeta.getIdentifier(), OBSOLETEDBY, chain);
	            if(obsoleted != null) {
	                targetSysmeta.setObsoletes(obsoleted);
	            }
	        } else if (targetSysmeta.getObsoletes() != null && targetSysmeta.getObsoletedBy() == null) {
	            Identifier obsoleted = getRelatedIdentifier(targetSysmeta.getIdentifier(), OBSOLETEDBY, chain);
                if(obsoleted != null) {
                    targetSysmeta.setObsoletes(obsoleted);
                }
	            
	        } else if (targetSysmeta.getObsoletes() == null && targetSysmeta.getObsoletedBy() != null) {
	            Identifier obsoletes = getRelatedIdentifier(targetSysmeta.getIdentifier(), OBSOLETES, chain);
                if(obsoletes != null) {
                    targetSysmeta.setObsoletedBy(obsoletes);
                }
            }
	    }
	}
	
	/*
	 * Get the identifier in chain which obsoleted or obsoletedBy the target id.
	 */
	private Identifier getRelatedIdentifier(Identifier target, String keyword, Vector<SystemMetadata> chain) {
	    Identifier identifier = null;
	    if(keyword.equals(OBSOLETES)) {
	        for(SystemMetadata sysmeta :chain) {
	            Identifier obsoletes = sysmeta.getObsoletes();
	            if(obsoletes != null && obsoletes.equals(target)) {
	                identifier = sysmeta.getIdentifier();
	            }
	        }
	    } else if(keyword.equals(OBSOLETEDBY)) {
	        for(SystemMetadata sysmeta :chain) {
	            Identifier obsoletedBy = sysmeta.getObsoletedBy();
	            if(obsoletedBy != null && obsoletedBy.equals(target)) {
	                identifier = sysmeta.getIdentifier();
	            }
	        }
	        
	    }
	    return identifier;
	}
	
	
	/*
	 * Decide if a system metadata object missing a obsoletes or obsoletedBy fields:
	 * 1. The system metadata object has both "oboletes" and "obsoletedBy" fields.
     * 2. If a system metadata object misses "oboletes" field, another system metadata object whose "obsoletedBy" fields points to the identifier doesn't exist.
    *  3. If a system metadata object misses "oboletedBy" field, another system metadata object whose "obsoletes" fields points to the identifier doesn't exist.
	 */
	private boolean hasMissingObsolescenceFields(SystemMetadata targetSysmeta, Vector<SystemMetadata> chain) {
	    boolean has = false;
	    if(targetSysmeta != null) {
            if (targetSysmeta.getObsoletes() == null && targetSysmeta.getObsoletedBy() == null) {
                if(foundIdentifierInAnotherSystemMetadata(targetSysmeta.getIdentifier(), OBSOLETEDBY, chain)) {
                    has = true;
                } else {
                    if(foundIdentifierInAnotherSystemMetadata(targetSysmeta.getIdentifier(), OBSOLETES, chain)) {
                        has = true;
                    }
                }
                
            } else if (targetSysmeta.getObsoletes() != null && targetSysmeta.getObsoletedBy() == null) {
                if(foundIdentifierInAnotherSystemMetadata(targetSysmeta.getIdentifier(), OBSOLETES, chain)) {
                    has = true;
                }
                
            } else if (targetSysmeta.getObsoletes() == null && targetSysmeta.getObsoletedBy() != null) {
                if(foundIdentifierInAnotherSystemMetadata(targetSysmeta.getIdentifier(), OBSOLETEDBY, chain)) {
                    has = true;
                }
            }
        }
	    return has;
	}
	
	/**
	 * Determine if the sepcified identifier exists in the sepcified field in another system metadata object.
	 * @param target
	 * @param fieldName
	 * @param chain
	 * @return true if we found it; false otherwise.
	 */
	private boolean foundIdentifierInAnotherSystemMetadata(Identifier target, String fieldName, Vector<SystemMetadata> chain) {
	    boolean found = false;
	    if(fieldName.equals(OBSOLETES)) {
            for(SystemMetadata sysmeta :chain) {
                Identifier obsoletes = sysmeta.getObsoletes();
                if(obsoletes != null && obsoletes.equals(target)) {
                    System.out.println("missing obsoletedBy");
                    found = true;
                }
            }
        } else if(fieldName.equals(OBSOLETEDBY)) {
            for(SystemMetadata sysmeta :chain) {
                Identifier obsoletedBy = sysmeta.getObsoletedBy();
                if(obsoletedBy != null && obsoletedBy.equals(target)) {
                    System.out.println("missing obsoletes");
                    found = true;
                }
            }
            
        }
	    return found;
	}
	
	/**
	 * Get the head version of the chain
	 * @param sid
	 * @return
	 */
	/*public Identifier getHeadVersion(Identifier sid, Vector<SystemMetadata> chain) {
	    Identifier pid = null;
	    Vector<SystemMetadata> sidChain = new Vector<SystemMetadata>();
	    int noObsoletedByCount =0;
	    boolean hasMissingObsolescenceFields = false;
	    if(chain != null) {
	        for(SystemMetadata sysmeta : chain) {
	            if(sysmeta.getSeriesId() != null && sysmeta.getSeriesId().equals(sid)) {
	                //decorateSystemMetadata(sysmeta, chain);
	                System.out.println("identifier "+sysmeta.getIdentifier().getValue()+" :");
	                if(sysmeta.getObsoletes() == null) {
	                    System.out.println("obsolets "+sysmeta.getObsoletes());
	                } else {
	                    System.out.println("obsolets "+sysmeta.getObsoletes().getValue());
	                }
	                if(sysmeta.getObsoletedBy() == null) {
	                    System.out.println("obsoletedBy "+sysmeta.getObsoletedBy());
	                } else {
	                    System.out.println("obsoletedBy "+sysmeta.getObsoletedBy().getValue());
	                }
	                if(!hasMissingObsolescenceFields) {
	                    if(hasMissingObsolescenceFields(sysmeta, chain)) {
	                        hasMissingObsolescenceFields = true;
	                    }
	                }
	                
	                if(sysmeta.getObsoletedBy() == null) {
	                    pid = sysmeta.getIdentifier();
	                    noObsoletedByCount++;
	                }
	                sidChain.add(sysmeta);
	            }
	        }
	    }
	    
	    if(hasMissingObsolescenceFields) {
	        System.out.println("It has an object whose system metadata has a missing obsoletes or obsoletedBy field.");
	        Collections.sort(sidChain, new SystemMetadataDateUploadedComparator());
            pid =sidChain.lastElement().getIdentifier();
	    } else {
	        if(noObsoletedByCount == 1) {
	            //rule 1 . If there is only one object having NULL value in the chain, return the value
	             System.out.println("rule 1");
	             return pid;
	         } else if (noObsoletedByCount > 1 ) {
	             // rule 2. If there is more than one object having NULL value in the chain, return last dateUploaded
	             System.out.println("rule 2");
	             Collections.sort(sidChain, new SystemMetadataDateUploadedComparator());
	             pid =sidChain.lastElement().getIdentifier();
	             
	         } else if (noObsoletedByCount == 0) {
	             // all pids were obsoleted
	             for(SystemMetadata sysmeta : sidChain) {
	                 //System.out.println("=== the pid in system metadata "+sysmeta.getIdentifier().getValue());
	                 Identifier obsoletedBy = sysmeta.getObsoletedBy();
	                 SystemMetadata sysOfObsoletedBy = getSystemMetadata(obsoletedBy, chain);
	                 if(sysOfObsoletedBy == null) {
	                     //Rule 4 We have a obsoletedBy id without system metadata. So we can't decide if a different sid exists. we have to sort it.
	                     System.out.println("rule 4");
	                     Collections.sort(sidChain, new SystemMetadataDateUploadedComparator());
	                     pid = sidChain.lastElement().getIdentifier();
	                     break;
	                 } else {
	                     Identifier sidOfObsoletedBy = sysOfObsoletedBy.getSeriesId();
	                     if(sidOfObsoletedBy != null && !sidOfObsoletedBy.equals(sid)) {
	                         //rule 3, if everything in {S1} is obsoleted, then select object that is obsoleted by another object that does not have the same SID
	                         System.out.println("rule 3-1 (close with another sid "+sidOfObsoletedBy.getValue()+")");
	                         pid = sysmeta.getIdentifier();
	                         break;
	                     } else if (sidOfObsoletedBy == null ) {
	                         //rule 3, If everything in {S1} is obsoleted, then select object that is obsoleted by another object that does not have the same SID (this case, no sid)
	                         System.out.println("rule 3-2 (close without sid)");
	                         pid = sysmeta.getIdentifier();
	                         break;
	                     }
	                 }
	                 
	             }
	         }
	    }
	    
	    return pid;
	}*/
	
	
	private SystemMetadata getSystemMetadata(Identifier id, Vector<SystemMetadata> chain ){
	    SystemMetadata sysmeta = null;
	    if(id != null) {
	        for(SystemMetadata sys : chain) {
	            if(sys.getIdentifier().equals(id)) {
	                sysmeta = sys;
	                break;
	            }
	        }
	    }
	    return sysmeta;
	}
	
	/**
     * Get the head version of the chain
     * @param sid
     * @return
     */
    public Identifier getHeadVersion(Identifier sid, Vector<SystemMetadata> chain) {
        Identifier pid = null;
        Vector<SystemMetadata> sidChain = new Vector<SystemMetadata>();
        int endCounter =0 ;
        String status = null;
        if(chain != null) {
            for(SystemMetadata sysmeta : chain) {
                if(sysmeta.getSeriesId() != null && sysmeta.getSeriesId().equals(sid)) {
                    status = endStatus(sysmeta, chain);
                    if(status.equals(END1) || status.equals(END2)) {
                        endCounter ++;
                        pid = sysmeta.getIdentifier();
                    }
                    sidChain.add(sysmeta);
                }
            }
            if(endCounter != 1) {
                System.out.println("The chain has "+endCounter+" ends and it is not an ideal chain.");
                Collections.sort(sidChain, new SystemMetadataDateUploadedComparator());
                pid =sidChain.lastElement().getIdentifier();
            } else {
                System.out.println(""+status);
            }
        }
        return pid;
    }
	
	
    /*
     * Rules for ends:
     *  Rule 1. An object in the SID chain doesn't have the "obsoletedBy" field.
     *  Rule 2. An object in the SID chain does have the "obsoletedBy" filed, but the "obsoletedBy" value has different the SID value (including no SID value).
     *  It is tricky if the object in the "obsoletedBy" filed is missing since we don't have the knowledge of its series id. Generally we consider it an end except: 
     *  if there is another object in the chain (has the same series id) obsoletes the missing object, the missing object is not an end.
     */
    private String endStatus(SystemMetadata targetSysmeta, Vector<SystemMetadata> chain) {
        String status = END2;
        if(targetSysmeta.getObsoletedBy() == null) {
            status = END1;
        } else {
            Identifier orgSid = targetSysmeta.getSeriesId();
            Identifier obsoletedBy = targetSysmeta.getObsoletedBy();
            SystemMetadata obsoletedBySys = getSystemMetadata(obsoletedBy, chain);
            if(obsoletedBySys != null) {
                if(obsoletedBySys.getSeriesId() != null && obsoletedBySys.getSeriesId().equals(orgSid)) {
                    status = NOTEND;
                }
            } else {
                // the obsoletedBy doesn't exist
                for(SystemMetadata sys : chain) {
                    if(sys.getSeriesId() != null && sys.getSeriesId().equals(orgSid)) {
                        if(sys.getObsoletes() != null && sys.getObsoletes().equals(obsoletedBy)) {
                            status = NOTEND;
                        }
                    }
                }
            }
        }
        return status;
    }
    
    private static final String END1 = "Rule 1";
    private static final String END2 = "Rule 2";
    private static final String NOTEND = "Not an end";
}
