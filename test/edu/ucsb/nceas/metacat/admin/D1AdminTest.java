package edu.ucsb.nceas.metacat.admin;

import edu.ucsb.nceas.LeanTestUtils;
import edu.ucsb.nceas.metacat.database.DBConnection;
import edu.ucsb.nceas.metacat.database.DBConnectionPool;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import org.dataone.client.auth.CertificateManager;
import org.dataone.client.v2.CNode;
import org.dataone.client.v2.itk.D1Client;
import org.dataone.service.types.v1.NodeReference;
import org.dataone.service.types.v2.Node;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.security.cert.X509Certificate;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

/**
 * <p>
 * Tests for the DataONE Member Node Registration and Update functionality in D1Admin
 */
public class D1AdminTest {

    private static final String PREVIOUS_NODE_ID = "urn:node:TestingPreviousNodeId";
    private static final String CONTAINERIZED = "METACAT_IS_RUNNING_IN_A_CONTAINER";
    private D1Admin d1Admin;

    @Before
    public void setUp() throws Exception {
        LeanTestUtils.initializePropertyService(LeanTestUtils.PropertiesMode.UNIT_TEST);
        d1Admin = D1Admin.getInstance();
    }

    @After
    public void tearDown() throws Exception {
        LeanTestUtils.setTestEnvironmentVariable(CONTAINERIZED, "");
    }

    /**
     * ============================
     * MN HAS NEVER BEEN REGISTERED
     * ============================
     *
     *           * START *
     *               |
     *               v
     *    +-----------------------+         +-----------------------+
     *    | (K8s ONLY) Registration         |  UPDATE DB            |
     *    |  change Requested?    - - NO -> |  *(authoritativeMNId  - -> | DONE |
     *    |  (yaml flag ==        | (local  |  & nodeId history)*   |
     *    |  today's date?)       | change) +-----------------------+
     *    +----------|------------+
     *               | YES
     *               v
     *    +-----------------------+
     *    | New NodeId            |
     *    | Matches Client Cert?  - - NO -> | LOG ERROR && DONE |
     *    +----------|------------+
     *               | YES
     *               v
     *    +-----------------------+
     *    |  REGISTER @ CN        - - FAIL -> | LOG ERROR && DONE |
     *    +----------|------------+
     *               | SUCCESS
     *               v
     *    +-----------------------+
     *    |  UPDATE DB            |
     *    |  *(authoritativeMNId  |
     *    |  & nodeId history)*   |
     *    +-----------------------+
     *               |
     *               v
     *             DONE
     * </pre>
     */

    // Happy Path
    @Test
    public void configUnregisteredMN() throws Exception {
        Properties withProperties = new Properties();
        // dataone.autoRegisterMemberNode valid @ today's date
        withProperties.setProperty("dataone.autoRegisterMemberNode", LocalDate.now().toString());
        try (MockedStatic<PropertyService> ignored
                 = LeanTestUtils.initializeMockPropertyService(withProperties)) {
            setK8sEnv();
            assertTrue(d1Admin.canChangeNodeId());
            runWithMockedClientCert(
                "CN=urn:node:NewTestMemberNode", null,
                () -> runWithMockedDataBaseConnection(() -> {
                    Node mockMN = getMockNode("urn:node:NewTestMemberNode");
                    registerWithMockedCN(true,
                                         () -> d1Admin.configUnregisteredMN(
                                             mockMN),
                                         "urn:node:NewTestMemberNode");
                }));
        }
    }


    /**
     * <pre>
     * ==========================================
     * MN HAS ALREADY BEEN REGISTERED IN THE PAST
     * ==========================================
     *
     *   This logic is encapsulated by the method:
     *       D1Admin::configPreregisteredMN()
     *
     *           * START *
     *               |
     *               v
     *    +----------------------+         +-----------------------+
     *    |   nodeId changed?    |         | UPDATE @ CN           |
     *    |  (yaml:nodeId        - - NO -> | - Push MN config to   - - > | DONE |
     *    |    != DB latest?)    |         | CN (nodeId UNCHANGED) |
     *    +----------|-----------+         +-----------------------+
     *               | YES
     *               v
     *    +-----------------------+
     *    | (K8s ONLY) Registration
     *    |  change Requested?    - - NO -> | LOG ERROR && DONE |
     *    |  (yaml flag ==        |
     *    |  today's date?)       |
     *    +----------|------------+
     *               | YES
     *               v
     *    +-----------------------+
     *    | New NodeId            |
     *    | Matches Client Cert?  - - NO -> | LOG ERROR && DONE |
     *    +----------|------------+
     *               | YES
     *               v
     *    +-----------------------+
     *    |  UPDATE @ CN          |
     *    |  - Push MN config to  |
     *    |  CN. (nodeId CHANGED) - - FAIL -> | LOG ERROR && DONE |
     *    +----------|------------+
     *               | SUCCESS
     *               v
     *    +-----------------------+
     *    |  UPDATE DB            |
     *    |  *(authoritativeMNId  |
     *    |  & nodeId history)*   |
     *    +-----------------------+
     *               |
     *               v
     *             DONE
     * </pre>
     *
     */

    // Happy Path
    @Test
    public void configPreregisteredMN() throws Exception {
        Properties withProperties = new Properties();
        // dataone.autoRegisterMemberNode valid @ today's date
        withProperties.setProperty("dataone.autoRegisterMemberNode", LocalDate.now().toString());
        try (MockedStatic<PropertyService> ignored
                 = LeanTestUtils.initializeMockPropertyService(withProperties)) {
            assertTrue(d1Admin.canChangeNodeId());
            setK8sEnv();
            runWithMockedClientCert(
                "CN=urn:node:NewTestMemberNode", null,
                () -> runWithMockedDataBaseConnection(() -> {
                    Node mockMN = getMockNode("urn:node:NewTestMemberNode");
                    updateMockedCN(true, () -> d1Admin.configPreregisteredMN(mockMN));
                }));
        }
    }

    @Test(expected = AdminException.class)
    public void configPreregisteredMN_nullNode() throws AdminException {
        try {
            d1Admin.configPreregisteredMN(null);
        } catch (AdminException expectedException) {
            String sub = "configPreregisteredMN() received a null node!";
            assertTrue("e.getMessage() didn't contain expected substring ('"+ sub + "')",
                       expectedException.getMessage().contains(sub));
            throw expectedException;
        }
    }

    @Test(expected = AdminException.class)
    public void configPreregisteredMN_noAutoRegisterProp() throws Exception {

        setK8sEnv();
        String sub = "*Not Permitted* to push update to CN without operator consent";
        runWithMockedClientCert("CN=Jing Tao", sub,
                                () -> {
                                    Node mockMN = getMockNode("Jing Tao");
                                    d1Admin.configPreregisteredMN(mockMN); // should throw exception
                                });
    }


    @Test(expected = AdminException.class)
    public void configPreregisteredMN_nonMatchingClientCert() throws Exception {
        Properties withProperties = new Properties();

        // dataone.autoRegisterMemberNode valid @ today's date
        withProperties.setProperty("dataone.autoRegisterMemberNode", LocalDate.now().toString());
        try (MockedStatic<PropertyService> ignored = LeanTestUtils.initializeMockPropertyService(
            withProperties)) {
            assertTrue(d1Admin.canChangeNodeId());
            String sub
                = "node Id does not agree with the 'Subject CN' value in the client certificate";
            runWithMockedClientCert("CN=urn:node:TestMemberNodeOLD", sub, () -> {
                Node mockMN = getMockNode("urn:node:TestMemberNodeNEW");
                updateMockedCN(true, () -> d1Admin.configPreregisteredMN(mockMN));
            });
        }
    }

    @Test
    public void getMostRecentNodeId() throws Exception {
        runWithMockedDataBaseConnection(() -> {
            String result = d1Admin.getMostRecentNodeId();
            assertEquals(PREVIOUS_NODE_ID, result);
        });
    }

    @Test
    public void canChangeNodeId_legacyDeployment() {
        assertTrue(d1Admin.canChangeNodeId());
    }

    @Test
    public void canChangeNodeId_k8sDeployment() throws Exception {

        setK8sEnv();
        Properties withProperties = new Properties();

        // dataone.autoRegisterMemberNode valid @ today's date
        withProperties.setProperty("dataone.autoRegisterMemberNode", LocalDate.now().toString());
        try (MockedStatic<PropertyService> ignored
                 = LeanTestUtils.initializeMockPropertyService(withProperties)){
            assertTrue(d1Admin.canChangeNodeId());
        }

        // dataone.autoRegisterMemberNode non-valid @ past date
        withProperties.setProperty("dataone.autoRegisterMemberNode", "1993-05-01");
        try (MockedStatic<PropertyService> ignored
                 = LeanTestUtils.initializeMockPropertyService(withProperties)){
            assertFalse(d1Admin.canChangeNodeId());
        }

        // dataone.autoRegisterMemberNode non-valid @ future date
        withProperties.setProperty("dataone.autoRegisterMemberNode", "2123-05-01");
        try (MockedStatic<PropertyService> ignored
                 = LeanTestUtils.initializeMockPropertyService(withProperties)){
            assertFalse(d1Admin.canChangeNodeId());
        }

        // dataone.autoRegisterMemberNode non-valid - not set
        withProperties.setProperty("dataone.autoRegisterMemberNode", "");
        try (MockedStatic<PropertyService> ignored
                 = LeanTestUtils.initializeMockPropertyService(withProperties)){
            assertFalse(d1Admin.canChangeNodeId());
        }
    }

    @Test
    public void nodeIdMatchesClientCert() throws Exception {

        // mock cert; malformed Subject -- no commas: only a CN and no DC components
        runWithMockedClientCert(
            "CN=urn:node:TestBROOKELT", null,
            () -> assertTrue(d1Admin.nodeIdMatchesClientCert("urn:node:TestBROOKELT")));

        // mock cert; malformed Subject -- no 'CN='
        runWithMockedClientCert("urn:node:TestBROOKELT,DC=ucsb,DC=org", null,
                                () -> assertFalse(d1Admin.nodeIdMatchesClientCert("urn:node:TestBROOKELT,DC=ucsb,DC=org")));

        // real test cert:
        //      $ openssl x509 -text -in test/test-credentials/test-user.pem | grep "Subject:"
        //              Subject: C = US, ST = California, O = UCSB, OU = NCEAS, CN = Jing Tao
        //
        String mnCertificatePath = "test/test-credentials/test-user.pem";
        CertificateManager.getInstance().setCertificateLocation(mnCertificatePath);
        assertTrue(d1Admin.nodeIdMatchesClientCert("Jing Tao"));
        assertFalse(d1Admin.nodeIdMatchesClientCert("urn:node:Bogus"));
    }

    @Test
    public void registerWithCN() throws Exception {
        Node mockMN = getMockNode("myNode");

        // cn.register() succeeds and returns correct node ID
        registerWithMockedCN(true, () -> assertTrue(d1Admin.registerWithCN(mockMN)), "myNode");

        // cn.register() succeeds but returns wrong node ID
        registerWithMockedCN(true, () -> assertFalse(d1Admin.registerWithCN(mockMN)), "wrongId");
        registerWithMockedCN(true, () -> assertFalse(d1Admin.registerWithCN(mockMN)), "");
        registerWithMockedCN(true, () -> assertFalse(d1Admin.registerWithCN(mockMN)), null);

        // unsuccessful update (cn.register() fails)
        registerWithMockedCN(false, () -> assertFalse(d1Admin.registerWithCN(mockMN)), null);
    }

    @Test
    public void updateCN() throws Exception {
        Node mockMN = getMockNode("myNode");

        // success case
        updateMockedCN(true, () -> assertTrue(d1Admin.updateCN(mockMN)));

        // unsuccessful update (cn.updateNodeCapabilities() returns false)
        updateMockedCN(false, () -> assertFalse(d1Admin.updateCN(mockMN)));
    }

    private static Node getMockNode(String NodeId) {
        NodeReference mockNodeRef = Mockito.mock(NodeReference.class);
        Mockito.when(mockNodeRef.getValue()).thenReturn(NodeId);

        Node mockMN = Mockito.mock(Node.class);
        Mockito.when(mockMN.getIdentifier()).thenReturn(mockNodeRef);
        return mockMN;
    }

    private static void updateMockedCN(boolean isUpdateSuccessful, TestCode testCode)
        throws Exception {
        registerWithMockedCN(isUpdateSuccessful, testCode, null);
    }
    private static void registerWithMockedCN(boolean isUpdateSuccessful, TestCode testCode,
                                             String returnNodeIdWhenRegSuccess) throws Exception {

        try (MockedStatic<D1Client> mockD1Client = Mockito.mockStatic(D1Client.class)) {
            final String CN_URL = PropertyService.getProperty("D1Client.CN_URL");
            CNode mockCN = Mockito.mock(CNode.class);
            Mockito.when(mockCN.getNodeBaseServiceUrl()).thenReturn(CN_URL);
            Mockito.when(mockCN.updateNodeCapabilities(eq(null), any(), any()))
                .thenReturn(isUpdateSuccessful);

            NodeReference mockReturnedNodeRef = Mockito.mock(NodeReference.class);
            Mockito.when(mockReturnedNodeRef.getValue()).thenReturn((isUpdateSuccessful)?
                                                            returnNodeIdWhenRegSuccess : null);
            Mockito.when(mockCN.register(eq(null), any()))
                .thenReturn(mockReturnedNodeRef);
            mockD1Client.when(D1Client::getCN).thenReturn(mockCN);

            testCode.execute();
        }
    }

    private void runWithMockedClientCert(String subjDN, String expectedExceptionSubstring,
                                         TestCode testCode) throws Exception {
        try (MockedStatic<CertificateManager> ignored
                 = Mockito.mockStatic(CertificateManager.class)) {
            CertificateManager mockCertMgr = Mockito.mock(CertificateManager.class);
            Mockito.when(CertificateManager.getInstance()).thenReturn(mockCertMgr);
            X509Certificate mockClientCert = Mockito.mock(X509Certificate.class);
            Mockito.when(mockCertMgr.loadCertificate()).thenReturn(mockClientCert);
            Mockito.when(mockCertMgr.getSubjectDN(any())).thenReturn(subjDN);

            testCode.execute();
        } catch (Exception expectedException) {
            expectedExceptionSubstring = (expectedExceptionSubstring == null)?
                                         "SET ME!!" : expectedExceptionSubstring;
            assertTrue(
                "e.getMessage() didn't contain expected substring ('" + expectedExceptionSubstring
                    + "')", expectedException.getMessage().contains(expectedExceptionSubstring));
            throw expectedException;
        }
    }

    private void runWithMockedDataBaseConnection(TestCode testCode) throws Exception {

        try (MockedStatic<DBConnectionPool> mockDbConnPool =
                 Mockito.mockStatic(DBConnectionPool.class)) {

            try (PreparedStatement mockPs = Mockito.mock(PreparedStatement.class)) {
                try (ResultSet mockRs = Mockito.mock(ResultSet.class)) {

                    // ResultSet
                    Mockito.when(mockRs.next()).thenReturn(true);
                    Mockito.when(mockRs.getString(1)).thenReturn(PREVIOUS_NODE_ID);

                    // PreparedStatement
                    Mockito.when(mockPs.executeUpdate()).thenReturn(911);
                    Mockito.when(mockPs.executeQuery()).thenReturn(mockRs);

                    // DBConnection
                    DBConnection mockDbConn = Mockito.mock(DBConnection.class);
                    Mockito.when(mockDbConn.prepareStatement(anyString())).thenReturn(mockPs);

                    mockDbConnPool.when(() -> DBConnectionPool.getDBConnection(anyString()))
                        .thenReturn(mockDbConn);

                    testCode.execute();
                }
            }
        }
    }

    private void setK8sEnv() throws Exception {
        LeanTestUtils.setTestEnvironmentVariable(CONTAINERIZED, "true");
    }

    @FunctionalInterface
    interface TestCode {
        void execute() throws Exception;
    }

}
