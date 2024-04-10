package edu.ucsb.nceas.metacat.dataone;


import edu.ucsb.nceas.LeanTestUtils;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import org.dataone.service.exceptions.InvalidRequest;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.NodeReference;
import org.dataone.service.types.v1.ObjectFormatIdentifier;
import org.dataone.service.types.v1.Session;
import org.dataone.service.types.v2.SystemMetadata;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;

import java.io.FileInputStream;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.Properties;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class NodeReplicationPolicyCheckerTest {
    private static final String taxononmyFilePath = "test/eml-with-taxonomy.xml";
    private D1NodeServiceTest d1NSTest;
    Properties withProperties;

    /**
     * Constructor
     */
    public NodeReplicationPolicyCheckerTest () {
        // D1NodeServiceTest extends MCTestCase, so it automatically calls
        // LeanTestUtils.initializePropertyService(LeanTestUtils.PropertiesMode.LIVE_TEST);
        d1NSTest = new D1NodeServiceTest("NodeReplicationPolicyCheckerTest");
    }

    @Before
    public void setUp() {

        withProperties = new Properties();
        withProperties.setProperty("dataone.node.replicationpolicy.maxObjectSize", "-1");
    }

    /**
     * Test the check method
     * @throws Exception
     */
    @Test
    public void testCheck() throws Exception {

        d1NSTest.printTestHeader("testCheck");
        String eml210 = "eml://ecoinformatics.org/eml-2.1.0";
        String text = "text/xml";
        String eml200 = "eml://ecoinformatics.org/eml-2.0.0";
        String one_mega = "1000000";
        String two_mega = "2000000";
        String nodeStr1 = "urn:node:test1";
        String nodeStr2 = "urn:node:test2";
        String nodeStr3 = "urn:node:test3";

        Session session = d1NSTest.getTestSession();
        Identifier guid = new Identifier();
        guid.setValue(
            "testNodeReplicationPolicyCheckerTestCheck." + System.currentTimeMillis());
        InputStream object = new FileInputStream(taxononmyFilePath);
        SystemMetadata sysmeta =
            D1NodeServiceTest.createSystemMetadata(guid, session.getSubject(), object);
        ObjectFormatIdentifier formatId = new ObjectFormatIdentifier();
        formatId.setValue(eml210);
        sysmeta.setFormatId(formatId);
        sysmeta.setSize(new BigInteger(one_mega));

        NodeReference sourceNode = new NodeReference();
        sourceNode.setValue(nodeStr2);

        //set the max objects size to -1, the allowed node string and format list to blank
        withProperties.setProperty("dataone.node.replicationpolicy.maxObjectSize", "-1");
        withProperties.setProperty("dataone.node.replicationpolicy.allowedNode", "");
        withProperties.setProperty("dataone.node.replicationpolicy.allowedObjectFormat", "");
        try (MockedStatic<PropertyService> ignored
                 = LeanTestUtils.initializeMockPropertyService(withProperties)) {
            NodeReplicationPolicyChecker.refresh();
            assertTrue(NodeReplicationPolicyChecker.check(sourceNode, sysmeta));
        }

        //set the allowed nodes which doesn't include the source node
        withProperties.setProperty(
                "dataone.node.replicationpolicy.allowedNode", nodeStr1 + ";" + nodeStr3);
        try (MockedStatic<PropertyService> ignored
                 = LeanTestUtils.initializeMockPropertyService(withProperties)) {

            NodeReplicationPolicyChecker.refresh();
            try {
                assertFalse(NodeReplicationPolicyChecker.check(sourceNode, sysmeta));
                fail("We can't get here since the the node is not allowed");
            } catch (Exception e) {
                assertTrue(e instanceof InvalidRequest);
                assertTrue(e.getMessage().contains(nodeStr2));
            }
        }

        //set the allowed nodes which include the source node
        withProperties.setProperty("dataone.node.replicationpolicy.allowedNode",
                                   nodeStr1 + ";" + nodeStr2 + ";" + nodeStr3);
        try (MockedStatic<PropertyService> ignored
                 = LeanTestUtils.initializeMockPropertyService(withProperties)) {

            PropertyService.persistProperties();
            NodeReplicationPolicyChecker.refresh();
            assertTrue(NodeReplicationPolicyChecker.check(sourceNode, sysmeta));
        }
        //set the object size less than the current object
        withProperties.setProperty(
            "dataone.node.replicationpolicy.maxObjectSize", "500000");
        try (MockedStatic<PropertyService> ignored
                 = LeanTestUtils.initializeMockPropertyService(withProperties)) {
            NodeReplicationPolicyChecker.refresh();
            try {
                assertFalse(NodeReplicationPolicyChecker.check(sourceNode, sysmeta));
                fail("We can't get here since the the size is too big");
            } catch (Exception e) {
                assertTrue(e instanceof InvalidRequest);
                assertTrue(e.getMessage().contains(one_mega));
                assertTrue(e.getMessage().contains(guid.getValue()));
            }
        }

        //set the object size greater than the current object
        withProperties.setProperty(
            "dataone.node.replicationpolicy.maxObjectSize", two_mega);
        try (MockedStatic<PropertyService> ignored
                 = LeanTestUtils.initializeMockPropertyService(withProperties)) {
            NodeReplicationPolicyChecker.refresh();
            assertTrue(NodeReplicationPolicyChecker.check(sourceNode, sysmeta));
        }


        //set the allowed format list not including the eml210
        withProperties.setProperty(
            "dataone.node.replicationpolicy.allowedObjectFormat", eml200 + ";" + text);
        try (MockedStatic<PropertyService> ignored
                 = LeanTestUtils.initializeMockPropertyService(withProperties)) {
            NodeReplicationPolicyChecker.refresh();
            try {
                assertFalse(NodeReplicationPolicyChecker.check(sourceNode, sysmeta));
                fail("We can't get here since the the format is not allowed");
            } catch (Exception e) {
                assertTrue(e instanceof InvalidRequest);
                assertTrue(e.getMessage().contains(eml210));
                assertTrue(e.getMessage().contains(guid.getValue()));
            }
        }

        //set the allowed format list including the eml210
        withProperties.setProperty("dataone.node.replicationpolicy.allowedObjectFormat",
                                    eml200 + ";" + text + ";" + eml210);
        try (MockedStatic<PropertyService> ignored
                 = LeanTestUtils.initializeMockPropertyService(withProperties)) {
            NodeReplicationPolicyChecker.refresh();
            assertTrue(NodeReplicationPolicyChecker.check(sourceNode, sysmeta));
        }

        //set the all of the three properties against the replication
        withProperties.setProperty(
            "dataone.node.replicationpolicy.maxObjectSize", "30000");
        withProperties.setProperty("dataone.node.replicationpolicy.allowedNode", nodeStr1);
        withProperties.setProperty(
            "dataone.node.replicationpolicy.allowedObjectFormat", text);

        try (MockedStatic<PropertyService> ignored
                 = LeanTestUtils.initializeMockPropertyService(withProperties)) {
            NodeReplicationPolicyChecker.refresh();
            try {
                assertFalse(NodeReplicationPolicyChecker.check(sourceNode, sysmeta));
                fail("We can't get here since the the node is not allowed");
            } catch (Exception e) {
                assertTrue(e instanceof InvalidRequest);
                assertTrue(e.getMessage().contains(nodeStr2));
            }

            //set guid to null in sysmeta
            sysmeta.setIdentifier(null);
            NodeReplicationPolicyChecker.refresh();
            try {
                assertFalse(NodeReplicationPolicyChecker.check(sourceNode, sysmeta));
                fail("We can't get here since the identifier is null");
            } catch (Exception e) {
                assertTrue(e instanceof InvalidRequest);
            }

            //set the source node to null
            sysmeta.setIdentifier(guid);
            NodeReplicationPolicyChecker.refresh();
            try {
                assertFalse(NodeReplicationPolicyChecker.check(null, sysmeta));
                fail("We can't get here since the identifier is null");
            } catch (Exception e) {
                assertTrue(e instanceof InvalidRequest);
                assertTrue(e.getMessage().contains(guid.getValue()));
            }

            //set the object format id to null
            sysmeta.setFormatId(null);
            NodeReplicationPolicyChecker.refresh();
            try {
                assertFalse(NodeReplicationPolicyChecker.check(sourceNode, sysmeta));
                fail("We can't get here since the format id is null");
            } catch (Exception e) {
                assertTrue(e instanceof InvalidRequest);
                assertTrue(e.getMessage().contains(guid.getValue()));
            }
        }
    }

}
