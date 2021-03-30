/**
 *  '$RCSfile$'
 *  Copyright: 2021 Regents of the University of California and the
 *              National Center for Ecological Analysis and Synthesis
 * 
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


import java.io.FileInputStream;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.HashMap;

import org.dataone.service.exceptions.InvalidRequest;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.NodeReference;
import org.dataone.service.types.v1.ObjectFormatIdentifier;
import org.dataone.service.types.v1.Session;
import org.dataone.service.types.v2.SystemMetadata;

import edu.ucsb.nceas.metacat.properties.PropertyService;
import junit.framework.Test;
import junit.framework.TestSuite;

public class NodeReplicationPolicyCheckerTest extends D1NodeServiceTest {
    private static String taxononmyFilePath = "test/eml-with-taxonomy.xml";
    
    /**
     * The test suite
     * @return
     */
    public static Test suite() {
        TestSuite suite = new TestSuite();
        suite.addTest(new NodeReplicationPolicyCheckerTest("testCheck"));
        return suite;
    }
    
    /**
     * Constructor
     * @param name  the name of the test method
     */
    public NodeReplicationPolicyCheckerTest (String name) {
        super(name);
    }
    
    /**
     * Test the check method
     * @throws Exception
     */
    public void testCheck() throws Exception {
        printTestHeader("testCheck");
        //preserve the original properties values
        String originMaxObjectSize = PropertyService.getProperty("dataone.node.replicationpolicy.maxObjectSize");
        String originAllowedNodeString = PropertyService.getProperty("dataone.node.replicationpolicy.allowedNode");
        String originAllowedFormatString = PropertyService.getProperty("dataone.node.replicationpolicy.allowedObjectFormat");
        try {
            String eml210 = "eml://ecoinformatics.org/eml-2.1.0";
            String text = "text/xml";
            String eml200 = "eml://ecoinformatics.org/eml-2.0.0";
            String one_mega = "1000000";
            String two_mega = "2000000";
            String nodeStr1 = "urn:node:test1";
            String nodeStr2 = "urn:node:test2";
            String nodeStr3 = "urn:node:test3";
            
            Session session = getTestSession();
            Identifier guid = new Identifier();
            HashMap<String, String[]> params = null;
            guid.setValue("testNodeReplicationPolicyCheckerTestCheck." + System.currentTimeMillis());
            InputStream object = new FileInputStream(taxononmyFilePath);
            SystemMetadata sysmeta = createSystemMetadata(guid, session.getSubject(), object);
            ObjectFormatIdentifier formatId = new ObjectFormatIdentifier();
            formatId.setValue(eml210);
            sysmeta.setFormatId(formatId);
            sysmeta.setSize(new BigInteger(one_mega));
            
            NodeReference sourceNode = new NodeReference();
            sourceNode.setValue(nodeStr2);
            
            //set the max objects size to -1, the allowed node string and format list to blank
            PropertyService.setProperty("dataone.node.replicationpolicy.maxObjectSize", "-1");
            PropertyService.setProperty("dataone.node.replicationpolicy.allowedNode", "");
            PropertyService.setProperty("dataone.node.replicationpolicy.allowedObjectFormat", "");
            PropertyService.persistProperties();
            NodeReplicationPolicyChecker.refresh();
            assertTrue(NodeReplicationPolicyChecker.check(sourceNode, sysmeta));
            
            //set the allowed nodes which doesn't include the source node
            PropertyService.setProperty("dataone.node.replicationpolicy.allowedNode", nodeStr1 + ";" + nodeStr3);
            PropertyService.persistProperties();
            NodeReplicationPolicyChecker.refresh();
            try {
                assertTrue(!NodeReplicationPolicyChecker.check(sourceNode, sysmeta));
                fail("We can't get here since the the node is not allowed");
            } catch (Exception e) {
                assertTrue(e instanceof InvalidRequest);
                assertTrue(e.getMessage().contains(nodeStr2));
            }

            //set the allowed nodes which include the source node
            PropertyService.setProperty("dataone.node.replicationpolicy.allowedNode", nodeStr1 + ";" + nodeStr2 + ";" + nodeStr3);
            PropertyService.persistProperties();
            NodeReplicationPolicyChecker.refresh();
            assertTrue(NodeReplicationPolicyChecker.check(sourceNode, sysmeta));
            
            //set the object size less than the current object
            PropertyService.setProperty("dataone.node.replicationpolicy.maxObjectSize", "500000");
            PropertyService.persistProperties();
            NodeReplicationPolicyChecker.refresh();
            try {
                assertTrue(!NodeReplicationPolicyChecker.check(sourceNode, sysmeta));
                fail("We can't get here since the the size is too big");
            } catch (Exception e) {
                assertTrue(e instanceof InvalidRequest);
                assertTrue(e.getMessage().contains(one_mega));
                assertTrue(e.getMessage().contains(guid.getValue()));
            }
            
            //set the object size greater than the current object
            PropertyService.setProperty("dataone.node.replicationpolicy.maxObjectSize", two_mega);
            PropertyService.persistProperties();
            NodeReplicationPolicyChecker.refresh();
            assertTrue(NodeReplicationPolicyChecker.check(sourceNode, sysmeta));
            
            //set the allowed format list not including the eml210
            PropertyService.setProperty("dataone.node.replicationpolicy.allowedObjectFormat", eml200 + ";" + text);
            PropertyService.persistProperties();
            NodeReplicationPolicyChecker.refresh();
            try {
                assertTrue(!NodeReplicationPolicyChecker.check(sourceNode, sysmeta));
                fail("We can't get here since the the format is not allowed");
            } catch (Exception e) {
                assertTrue(e instanceof InvalidRequest);
                assertTrue(e.getMessage().contains(eml210));
                assertTrue(e.getMessage().contains(guid.getValue()));
            }
            
            //set the allowed format list including the eml210
            PropertyService.setProperty("dataone.node.replicationpolicy.allowedObjectFormat", eml200 + ";" + text + ";" + eml210);
            PropertyService.persistProperties();
            NodeReplicationPolicyChecker.refresh();
            assertTrue(NodeReplicationPolicyChecker.check(sourceNode, sysmeta));
            
            //set the all of the three properties against the replication
            PropertyService.setProperty("dataone.node.replicationpolicy.maxObjectSize", "30000");
            PropertyService.setProperty("dataone.node.replicationpolicy.allowedNode", nodeStr1);
            PropertyService.setProperty("dataone.node.replicationpolicy.allowedObjectFormat", text);
            PropertyService.persistProperties();
            NodeReplicationPolicyChecker.refresh();
            try {
                assertTrue(!NodeReplicationPolicyChecker.check(sourceNode, sysmeta));
                fail("We can't get here since the the node is not allowed");
            } catch (Exception e) {
                assertTrue(e instanceof InvalidRequest);
                assertTrue(e.getMessage().contains(nodeStr2));
            }
            
            //set guid to null in sysmeta
            sysmeta.setIdentifier(null);
            NodeReplicationPolicyChecker.refresh();
            try {
                assertTrue(!NodeReplicationPolicyChecker.check(sourceNode, sysmeta));
                fail("We can't get here since the identifier is null");
            } catch (Exception e) {
                assertTrue(e instanceof InvalidRequest);
            }
            
            //set the source node to null
            sysmeta.setIdentifier(guid);
            NodeReplicationPolicyChecker.refresh();
            try {
                assertTrue(!NodeReplicationPolicyChecker.check(null, sysmeta));
                fail("We can't get here since the identifier is null");
            } catch (Exception e) {
                assertTrue(e instanceof InvalidRequest);
                assertTrue(e.getMessage().contains(guid.getValue()));
            }
            
            //set the object format id to null
            sysmeta.setFormatId(null);
            NodeReplicationPolicyChecker.refresh();
            try {
                assertTrue(!NodeReplicationPolicyChecker.check(sourceNode, sysmeta));
                fail("We can't get here since the format id is null");
            } catch (Exception e) {
                assertTrue(e instanceof InvalidRequest);
                assertTrue(e.getMessage().contains(guid.getValue()));
            }
            
        } finally {
            //reset those original values back to the metacat.properties file
            PropertyService.setProperty("dataone.node.replicationpolicy.maxObjectSize", originMaxObjectSize);
            PropertyService.setProperty("dataone.node.replicationpolicy.allowedNode", originAllowedNodeString);
            PropertyService.setProperty("dataone.node.replicationpolicy.allowedObjectFormat", originAllowedFormatString);
            PropertyService.persistProperties();
        }
    }

}
