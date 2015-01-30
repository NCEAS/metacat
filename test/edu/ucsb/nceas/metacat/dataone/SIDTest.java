/**
 *  '$RCSfile$'
 *  Copyright: 2010 Regents of the University of California and the
 *              National Center for Ecological Analysis and Synthesis
 *  Purpose: To test the Access Controls in metacat by JUnit
 *
 *   '$Author: leinfelder $'
 *     '$Date: 2014-08-07 14:28:35 -0700 (Thu, 07 Aug 2014) $'
 * '$Revision: 8834 $'
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
	    testCase14();
	}
	
	/**
	 * Case 14: P1(S1) <- P2(S1) -> P3(S2).
	 * After decorating sysmeta, it changed to P1(S1) <-> P2(S1) <-> P3(S2).
	 * S1 = P2 (Rule 3) 
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
        
        Vector<SystemMetadata> chain = new Vector<SystemMetadata>();
        chain.add(p1Sys);
        chain.add(p2Sys);
        chain.add(p3Sys);
        
        Identifier head = getHeadVersion(s1, chain);
        //System.out.println("The head is "+head.getValue());
        assertTrue(head.equals(p2));
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
                Identifier obsoletes = getRelatedIdentifier(targetSysmeta.getIdentifier(), OBSOLETES, chain);
                if(obsoletes != null) {
                    has = true;
                    
                } else {
                    Identifier obsoleted = getRelatedIdentifier(targetSysmeta.getIdentifier(), OBSOLETEDBY, chain);
                    if(obsoleted != null) {
                        has = true;
                    }
                }
                
            } else if (targetSysmeta.getObsoletes() != null && targetSysmeta.getObsoletedBy() == null) {
                Identifier obsoleted = getRelatedIdentifier(targetSysmeta.getIdentifier(), OBSOLETEDBY, chain);
                if(obsoleted != null) {
                    has = true;
                }
                
            } else if (targetSysmeta.getObsoletes() == null && targetSysmeta.getObsoletedBy() != null) {
                Identifier obsoletes = getRelatedIdentifier(targetSysmeta.getIdentifier(), OBSOLETES, chain);
                if(obsoletes != null) {
                    has = true;
                }
            }
        }
	    return has;
	}
	
	/**
	 * Get the head version of the chain
	 * @param sid
	 * @return
	 */
	public Identifier getHeadVersion(Identifier sid, Vector<SystemMetadata> chain) {
	    Identifier pid = null;
	    Vector<SystemMetadata> sidChain = new Vector<SystemMetadata>();
	    int noObsoletedByCount =0;
	    boolean hasMissingObsolescenceFields = false;
	    if(chain != null) {
	        for(SystemMetadata sysmeta : chain) {
	            if(sysmeta.getSeriesId() != null && sysmeta.getSeriesId().equals(sid)) {
	                //decorateSystemMetadata(sysmeta, chain);
	                /*System.out.println("identifier "+sysmeta.getIdentifier().getValue()+" :");
	                if(sysmeta.getObsoletes() == null) {
	                    System.out.println("obsolets "+sysmeta.getObsoletes());
	                } else {
	                    System.out.println("obsolets "+sysmeta.getObsoletes().getValue());
	                }
	                if(sysmeta.getObsoletedBy() == null) {
	                    System.out.println("obsoletedBy "+sysmeta.getObsoletedBy());
	                } else {
	                    System.out.println("obsoletedBy "+sysmeta.getObsoletedBy().getValue());
	                }*/
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
	        System.out.println("It has an object whose system metadata has missing obsoletes or obsoletedBy field.");
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
	                         System.out.println("rule 3-2 (close without sid");
	                         pid = sysmeta.getIdentifier();
	                         break;
	                     }
	                 }
	                 
	             }
	         }
	    }
	    
	    return pid;
	}
	
	
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
}
