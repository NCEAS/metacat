package edu.ucsb.nceas.metacat.dataone;

import edu.ucsb.nceas.MCTestCase;
import edu.ucsb.nceas.metacat.properties.SkinPropertyService;
import edu.ucsb.nceas.metacat.service.ServiceService;
import edu.ucsb.nceas.metacat.startup.MetacatInitializer;
import edu.ucsb.nceas.metacat.storage.Storage;
import edu.ucsb.nceas.metacat.util.SkinUtil;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.io.IOUtils;
import org.apache.wicket.protocol.http.mock.MockHttpServletRequest;
import org.dataone.client.D1Node;
import org.dataone.client.NodeLocator;
import org.dataone.client.exception.ClientSideException;
import org.dataone.client.v2.CNode;
import org.dataone.client.v2.formats.ObjectFormatCache;
import org.dataone.client.v2.itk.D1Client;
import org.dataone.configuration.Settings;
import org.dataone.exceptions.MarshallingException;
import org.dataone.service.exceptions.IdentifierNotUnique;
import org.dataone.service.exceptions.InsufficientResources;
import org.dataone.service.exceptions.InvalidRequest;
import org.dataone.service.exceptions.InvalidSystemMetadata;
import org.dataone.service.exceptions.InvalidToken;
import org.dataone.service.exceptions.NotAuthorized;
import org.dataone.service.exceptions.NotFound;
import org.dataone.service.exceptions.NotImplemented;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.exceptions.UnsupportedType;
import org.dataone.service.types.v1.AccessPolicy;
import org.dataone.service.types.v1.AccessRule;
import org.dataone.service.types.v1.Checksum;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.NodeReference;
import org.dataone.service.types.v1.NodeType;
import org.dataone.service.types.v1.ObjectFormatIdentifier;
import org.dataone.service.types.v1.Permission;
import org.dataone.service.types.v1.Session;
import org.dataone.service.types.v1.Subject;
import org.dataone.service.types.v1.SubjectInfo;
import org.dataone.service.types.v1.util.AccessUtil;
import org.dataone.service.types.v1.util.ChecksumUtil;
import org.dataone.service.types.v2.Node;
import org.dataone.service.types.v2.SystemMetadata;
import org.dataone.service.types.v2.util.ObjectFormatServiceImpl;
import org.dataone.service.util.Constants;
import org.junit.After;
import org.junit.Before;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

/**
 * A JUnit superclass for testing the dataone Node implementations
 */
public class D1NodeServiceTest extends MCTestCase {
    public static final int MAX_TRIES = 100;
    protected MockHttpServletRequest request;
    public static final ObjectFormatIdentifier eml_2_1_1_format = new ObjectFormatIdentifier();
    public static final ObjectFormatIdentifier eml_2_0_1_format = new ObjectFormatIdentifier();
    public static final ObjectFormatIdentifier eml_2_1_0_format = new ObjectFormatIdentifier();
    public static final ObjectFormatIdentifier eml_dataset_beta_6_format = new ObjectFormatIdentifier();
    private static Storage storage = null;
    private MockedStatic<Settings> mockStaticSettings;

    static {
        eml_2_1_1_format.setValue("eml://ecoinformatics.org/eml-2.1.1");
        eml_2_1_0_format.setValue("eml://ecoinformatics.org/eml-2.1.0");
        eml_2_0_1_format.setValue("eml://ecoinformatics.org/eml-2.0.1");
        eml_dataset_beta_6_format.setValue("-//ecoinformatics.org//eml-dataset-2.0.0beta6//EN");
    }

    /**
     * constructor for the test
     */
    public D1NodeServiceTest(String name) {
        super(name);
        if (storage == null) {
            synchronized(D1NodeServiceTest.class) {
                if (storage == null) {
                    try {
                        MetacatInitializer.initStorage();
                        storage = MetacatInitializer.getStorage();
                    } catch (Exception e) {
                        throw new RuntimeException(e.getMessage());
                    }
                }
            }
        }
        // set up the fake request (for logging)
        request = new MockHttpServletRequest(null, null, null);
    }

    public static Test suite() {
        TestSuite suite = new TestSuite();
        suite.addTest(new D1NodeServiceTest("initialize"));
        suite.addTest(new D1NodeServiceTest("testExpandRightsHolder"));
        suite.addTest(new D1NodeServiceTest("testIsValidIdentifier"));
        suite.addTest(new D1NodeServiceTest("testAddParamsFromSkinProperties"));
        suite.addTest(new D1NodeServiceTest("testAccessPolicyEqual"));
        suite.addTest(new D1NodeServiceTest("testIsAccessControlDirty"));

        return suite;
    }

    /**
     * Establish a testing framework by initializing appropriate objects
     */
    @Before
    public void setUp() throws Exception {

        super.setUp();
        mockConfig();

        NodeLocator nodeLocator = new NodeLocator() {
            @Override
            public D1Node getCNode() throws ClientSideException {
                D1Node node = null;
                try {
                    node = new MockCNode();
                } catch (IOException e) {
                    throw new ClientSideException(e.getMessage());
                }
                return node;
            }
        };

        //add the replicate node into the locator
        NodeReference nodeRef = new NodeReference();
        nodeRef.setValue(MockReplicationMNode.NODE_ID);
        MockReplicationMNode mNode = new MockReplicationMNode("http://replication.node.com");
        nodeLocator.putNode(nodeRef, mNode);
        D1Client.setNodeLocator(nodeLocator );
        D1Node node = D1Client.getCN();//call this method can clear the default cn
        System.out.println("in the D1NodeServiceTest set up................. the base url is for cn is " + node.getNodeBaseServiceUrl());

    }

    /**
     * Release any objects after tests are complete
     */
    @After
    public void tearDown() {
        // set back to force it to use defaults
        D1Client.setNodeLocator(null);
        mockStaticSettings.close();
    }
    
    /**
     * Get the http servlet request
     * @return the http servlet request
     */
    public HttpServletRequest getServletRequest() {
        return request;
    }

    public void testExpandRightsHolder() throws Exception {
        printTestHeader("testExpandRightsHolder");
        // set back to force it to use defaults
        NodeLocator nodeLocator1 = new NodeLocator() {
            @Override
            public D1Node getCNode() throws ClientSideException {
                D1Node node = null;
                try {
                    node = D1Client.getCN("https://cn.dataone.org/cn");
                } catch (Exception e) {
                    throw new ClientSideException(e.getMessage());
                }
                return node;
            }
        };
        D1Client.setNodeLocator(nodeLocator1);
        D1Node node = D1Client.getCN();//call this method can clear the mock cn
        System.out.println("in the testExpandRightsHolder, ---the base url is for cn is "
                               + node.getNodeBaseServiceUrl());
        Subject rightsHolder = new Subject();
        rightsHolder.setValue("CN=arctic-data-admins,DC=dataone,DC=org");
        Subject user = new Subject();

        user.setValue("http://orcid.org/0000-0002-1209-5268");
        assertTrue(D1AuthHelper.expandRightsHolder(rightsHolder, user));

        user.setValue("uid=foo");
        assertFalse(D1AuthHelper.expandRightsHolder(rightsHolder, user));

        user.setValue("http://orcid.org/0000-0003-0077-4738");
        assertTrue(D1AuthHelper.expandRightsHolder(rightsHolder, user));

        rightsHolder.setValue("CN=foo,,DC=dataone,DC=org");
        assertFalse(D1AuthHelper.expandRightsHolder(rightsHolder, user));

        user.setValue("uid=foo");
        assertFalse(D1AuthHelper.expandRightsHolder(rightsHolder, user));

        rightsHolder.setValue(null);
        assertFalse(D1AuthHelper.expandRightsHolder(rightsHolder, user));

        rightsHolder.setValue("CN=foo,,DC=dataone,DC=org");
        user.setValue(null);
        assertFalse(D1AuthHelper.expandRightsHolder(rightsHolder, user));

        rightsHolder.setValue(null);
        assertFalse(D1AuthHelper.expandRightsHolder(rightsHolder, user));

        rightsHolder.setValue("");
        user.setValue("");
        assertFalse(D1AuthHelper.expandRightsHolder(rightsHolder, user));
        NodeLocator nodeLocator = new NodeLocator() {
            @Override
            public D1Node getCNode() throws ClientSideException {
                D1Node node = null;
                try {
                    node = new MockCNode();
                } catch (IOException e) {
                    throw new ClientSideException(e.getMessage());
                }
                return node;
            }
        };
        D1Client.setNodeLocator(nodeLocator);
    }

    /**
     * Test the isValidIdentifier method
     */
    public void testIsValidIdentifier() {
        Identifier pid = null;
        assertFalse(D1NodeService.isValidIdentifier(pid));
        pid = new Identifier();
        assertFalse(D1NodeService.isValidIdentifier(pid));
        pid.setValue("");
        assertFalse(D1NodeService.isValidIdentifier(pid));
        pid.setValue(" ");
        assertFalse(D1NodeService.isValidIdentifier(pid));
        pid.setValue("\nasfd");
        assertFalse(D1NodeService.isValidIdentifier(pid));
        pid.setValue("as\tfd");
        assertFalse(D1NodeService.isValidIdentifier(pid));
        pid.setValue("as fd");
        assertFalse(D1NodeService.isValidIdentifier(pid));
        pid.setValue("asfd ");
        assertFalse(D1NodeService.isValidIdentifier(pid));
        pid.setValue("  asfd");
        assertFalse(D1NodeService.isValidIdentifier(pid));
        pid.setValue("asfd\r");
        assertFalse(D1NodeService.isValidIdentifier(pid));
        pid.setValue("\fasfd\r");
        assertFalse(D1NodeService.isValidIdentifier(pid));
        pid.setValue("as\u000Bfd");
        assertFalse(D1NodeService.isValidIdentifier(pid));
        pid.setValue("as\u001Cfd");
        assertFalse(D1NodeService.isValidIdentifier(pid));
        pid.setValue("as\u001Dfd");
        assertFalse(D1NodeService.isValidIdentifier(pid));
        pid.setValue("as\u001Efd");
        assertFalse(D1NodeService.isValidIdentifier(pid));
        pid.setValue("as\u001Ffd");
        assertFalse(D1NodeService.isValidIdentifier(pid));
        pid.setValue("`1234567890-=~!@#$%^&*()_+[]{}|\\:;,./<>?\"'");
        assertTrue(D1NodeService.isValidIdentifier(pid));
        pid.setValue("ess-dive-aa6e33480c133b0-20181019T234605514");
        assertTrue(D1NodeService.isValidIdentifier(pid));
        pid.setValue("doi:10.3334/CDIAC/ATG.DB1001");
        assertTrue(D1NodeService.isValidIdentifier(pid));
        pid.setValue("{00053F3B-7552-444F-8F57-6670756212BA}");
        assertTrue(D1NodeService.isValidIdentifier(pid));
        pid.setValue("urn:uuid:8009cc13-08d5-4bb2-ad9a-dc0f5dbfbcd0");
        assertTrue(D1NodeService.isValidIdentifier(pid));
        pid.setValue("ark:/90135/q1f769jn/2/mrt-eml.xml");
        assertTrue(D1NodeService.isValidIdentifier(pid));
        pid.setValue("https://doi.org/10.5061/dryad.k6gf1tf/15?ver=2018-09-18T03:54:10.492+00:00");
        assertTrue(D1NodeService.isValidIdentifier(pid));
        pid.setValue("p1312.ds2636_20181109_0300");
        assertTrue(D1NodeService.isValidIdentifier(pid));
    }

    /**
     * Test the method of addParasFromSkinProperties
     * @throws Exception
     */
    public void testAddParamsFromSkinProperties() throws Exception {
        String SKIN_NAME = "test-theme";
        //settings for testing
        Vector<String> originalSkinNames = SkinUtil.getSkinNames();
        Vector<String> newNames = new Vector<>();
        newNames.add(SKIN_NAME);
        SkinUtil.setSkinName(newNames);
        ServletContext servletContext = Mockito.mock(ServletContext.class);
        Mockito.when(servletContext.getContextPath()).thenReturn("context");
        Mockito.when(servletContext.getRealPath("/")).thenReturn("/");
        Mockito.when(servletContext.getRealPath("/style/skins")).thenReturn("test/skins");
        ServiceService.getInstance(servletContext);
        SkinPropertyService service = SkinPropertyService.getInstance();

        Hashtable<String, String[]> params = new Hashtable<>();
        D1NodeService.addParamsFromSkinProperties(params, SKIN_NAME);
        String[] value = params.get("serverName");
        assertEquals("https://foo.com", value[0]);//real value
        value = params.get("testUser");
        assertEquals("uid=kepler,o=unaffiliated,dc=ecoinformatics,dc=org", value[0]); //value of "test.mcUser" from the metacat.properties file
        value = params.get("testThirdUser");
        assertEquals("http://orcid.org/0023-0001-7868-2567", value[0]); //value of "test.mcThirdUser" from the metacat.properties file
        value = params.get("organization");
        assertEquals("ESS-DIVE", value[0]); //real value
        SkinUtil.setSkinName(originalSkinNames);

    }

    /**
     * constructs a "fake" session with a test subject
     * @return a Session object with the test subject
     */
    public Session getTestSession() throws Exception {
        Session session = new Session();
        Subject subject = new Subject();
        subject.setValue("cn=test,dc=dataone,dc=org");
        session.setSubject(subject);
        return session;
    }

    /**
     * constructs a "fake" session with the MN subject
     * @return
     */
    public Session getMNSession() throws Exception {
        Session session = new Session();
        Subject subject = MNodeService.getInstance(request).getCapabilities().getSubject(0);
        session.setSubject(subject);
        return session;
    }

    public static Session getCNSession() throws Exception {
        Session session = new Session();
        Subject subject = null;
        CNode cn = D1Client.getCN();
        List<Node> nodes = cn.listNodes().getNodeList();

        // find the first CN in the node list
        for (Node node : nodes) {
            if (node.getType().equals(NodeType.CN)) {
                subject = node.getSubject(0);
                break;
            }
        }
        System.out.println("the subject " + subject.getValue() + " was created for cn session----------------------- ");
        session.setSubject(subject);
        return session;

    }

    public static Session getAnotherSession() throws Exception {
        Session session = new Session();
        Subject subject = new Subject();
        subject.setValue("cn=test2,dc=dataone,dc=org");
        session.setSubject(subject);
        return session;

    }

    public static Session getThirdSession() throws Exception {
        Session session = new Session();
        Subject subject = new Subject();
        subject.setValue("cn=test34,dc=dataone,dc=org");
        session.setSubject(subject);
        return session;

    }

    /**
     * Get a session whose subject is a member of MNode subject (which is a group)
     * @return a session with the subject info
     * @throws Exception
     */
    public Session getMemberOfMNodeSession() throws Exception {
        Session session = new Session();
        Subject subject = new Subject();
        subject.setValue(MockCNode.MNODEMEMBERADMINSUBJECT);
        session.setSubject(subject);
        SubjectInfo subjectInfo = D1Client.getCN().getSubjectInfo(null, subject);
        session.setSubjectInfo(subjectInfo);
        return session;
    }

    /**
     * Get a session whose subject is a member of CNode subject (which is a group)
     * @return a session with the subject info
     * @throws Exception
     */
    public Session getMemberOfCNodeSession() throws Exception {
        Session session = new Session();
        Subject subject = new Subject();
        subject.setValue(MockCNode.CNODEMEMBERADMINSUBJECT);
        session.setSubject(subject);
        SubjectInfo subjectInfo = D1Client.getCN().getSubjectInfo(null, subject);
        session.setSubjectInfo(subjectInfo);
        return session;
    }

    /**
     * Run an initial test that always passes to check that the test harness is
     * working.
     */
    public void initialize()
    {
        printTestHeader("initialize");
        assertTrue(1 == 1);
    }

    /**
     * create system metadata with a specified id
     * @param id  the identifier of the system metadata
     * @param owner  the rights holder of the system metadata
     * @param object  the object associated with the system metadata
     * @return
     * @throws Exception
     */
    public static SystemMetadata createSystemMetadata(Identifier id, Subject owner,
                                                        InputStream object) throws Exception {
        SystemMetadata sm = new SystemMetadata();
        addSystemMetadataInfo(sm, id, owner, object);
        return sm;
    }

    /**
     * Create a system metadata object with the v1 version
     * @param id
     * @param owner
     * @param object
     * @return
     * @throws Exception
     */
    public org.dataone.service.types.v1.SystemMetadata createV1SystemMetadata(Identifier id, Subject owner, InputStream object) throws Exception {
              org.dataone.service.types.v1.SystemMetadata sm = new org.dataone.service.types.v1.SystemMetadata();
              addSystemMetadataInfo(sm, id, owner, object);
              return sm;
    }

    /**
     * Add the system metadata information to a given system metadata object.
     * @param sm  the system metadata object which have the given information
     * @param id  the identifier of the system metadata
     * @param owner  the rights holder of the system metadata
     * @param object  the object associated with the system metadata
     * @throws Exception
     */
    private static void addSystemMetadataInfo(org.dataone.service.types.v1.SystemMetadata sm, 
                                Identifier id, Subject owner, InputStream object) throws Exception {
        sm.setSerialVersion(BigInteger.valueOf(1));
        // set the id
        sm.setIdentifier(id);
        sm.setFormatId(ObjectFormatCache.getInstance()
                                        .getFormat("application/octet-stream").getFormatId());
        byte[] array = IOUtils.toByteArray(object);
        if (object.markSupported()) {
            object.reset();
        }
        int size = array.length;
        String sizeStr = "" + size;
        sm.setSize(new BigInteger(sizeStr));
        // create the checksum
        InputStream input = new ByteArrayInputStream(array);
        Checksum checksum = new Checksum();
        String ca = "MD5";
        checksum.setValue("test");
        checksum.setAlgorithm(ca);
        // actually generate one
        if (input != null) {
            checksum = ChecksumUtil.checksum(input, ca);
        }
        input.close();
        sm.setChecksum(checksum);
        sm.setSubmitter(owner);
        sm.setRightsHolder(owner);
        sm.setDateUploaded(new Date());
        sm.setDateSysMetadataModified(new Date());
        String currentNodeId = Settings.getConfiguration().getString("dataone.nodeId");
        if(currentNodeId == null || currentNodeId.trim().equals("")) {
            throw new Exception("there should be value in the dataone.nodeId "
                                    + "in the metacat.properties file.");
        }
        NodeReference nr = new NodeReference();
        nr.setValue(currentNodeId);
        sm.setOriginMemberNode(nr);
        sm.setAuthoritativeMemberNode(nr);
        // set the access to public read
        AccessPolicy accessPolicy = new AccessPolicy();
        AccessRule allow = new AccessRule();
        allow.addPermission(Permission.READ);
        Subject publicSubject = new Subject();
        publicSubject.setValue(Constants.SUBJECT_PUBLIC);
        allow.addSubject(publicSubject);
        accessPolicy.addAllow(allow);
        sm.setAccessPolicy(accessPolicy);
}

    /**
     * For fresh Metacat installations without the Object Format List
     * we insert the default version from d1_common.jar
     * @throws Exception 
     */
    public void setUpFormats() throws Exception {
        int rev = 1;
        Identifier guid = new Identifier();
        guid.setValue(ObjectFormatService.OBJECT_FORMAT_PID_PREFIX + rev);
        // check if it exists already
        InputStream is = null;
        Session session = getCNSession();
        try {
            is = CNodeService.getInstance(request).get(session, guid);
        } catch (Exception e) {
            System.out.println("the message is " + e.getMessage());
        }
        if (is == null) {
            // get the default from d1_common
            InputStream object = ObjectFormatServiceImpl.getInstance().getObjectFormatFile();
            SystemMetadata sysmeta = createSystemMetadata(guid, session.getSubject(), object);
            object.close();
            ObjectFormatIdentifier format = new ObjectFormatIdentifier();
            format.setValue("http://ns.dataone.org/service/types/v2.0:ObjectFormatList");
            sysmeta.setFormatId(format);
            //sysmeta.setFormatId(ObjectFormatCache.getInstance().getFormat("text/xml").getFormatId());
            object = ObjectFormatServiceImpl.getInstance().getObjectFormatFile();
            cnCreate(session, guid, object, sysmeta);
        }
    }

    /**
     * Test the method if two AccessPolicy objects equal
     */
    public void testAccessPolicyEqual() {
        printTestHeader("testAccessPolicyEqual");
        String[] subjectsPublic = {"public"};
        String[] subjectsOrcid = {"http://orcid.org/0000-0002-8121-2341"};
        String[] subjectsLDAP = {"CN=Bryce Mecum A27576,O=Google,C=US,DC=cilogon,DC=org"};
        String[] subjectsMix = {"http://orcid.org/0000-0002-8121-2341", "CN=Bryce Mecum A27576,O=Google,C=US,DC=cilogon,DC=org"};
        String[] subjectsTest = {"test"};
        Permission[] permissionsREAD = {Permission.READ};
        Permission[] permissionsWRITE = {Permission.WRITE};
        Permission[] permissionsCHANGE = {Permission.CHANGE_PERMISSION};
        Permission[] permissionsWRITEREAD = {Permission.WRITE, Permission.READ};
        //some edge cases
        AccessPolicy ap1 = null;
        AccessPolicy ap2 = null;
        assertTrue(D1NodeService.equals(ap1, ap2));
        assertTrue(D1NodeService.equals(ap2, ap1));
        ap2 = new AccessPolicy();
        assertTrue(D1NodeService.equals(ap1, ap2));
        assertTrue(D1NodeService.equals(ap2, ap1));
        Vector<AccessRule> rules20 = new Vector<AccessRule>();
        ap2.setAllowList(rules20);
        assertTrue(D1NodeService.equals(ap1, ap2));
        assertTrue(D1NodeService.equals(ap2, ap1));

        //same subject, but different permission
        ap1 = AccessUtil.createSingleRuleAccessPolicy(subjectsTest, permissionsREAD);
        ap2 = AccessUtil.createSingleRuleAccessPolicy(subjectsTest, permissionsCHANGE);
        assertFalse(D1NodeService.equals(ap1, ap2));
        assertFalse(D1NodeService.equals(ap2, ap1));
        assertEquals(1, ap1.getAllowList().size());
        assertEquals("test", ap1.getAllow(0).getSubject(0).getValue());
        assertEquals(ap1.getAllow(0).getPermission(0), Permission.READ);
        assertEquals(1, ap2.getAllowList().size());
        assertEquals("test", ap2.getAllow(0).getSubject(0).getValue());
        assertEquals(ap2.getAllow(0).getPermission(0), Permission.CHANGE_PERMISSION);

        //add a new permission for an existed user
        ap1 = new AccessPolicy();
        ap1.addAllow(AccessUtil.createAccessRule(subjectsPublic, permissionsREAD));
        ap1.addAllow(AccessUtil.createAccessRule(subjectsOrcid, permissionsREAD));
        ap2 = new AccessPolicy();
        ap2.addAllow(AccessUtil.createAccessRule(subjectsPublic, permissionsREAD));
        ap2.addAllow(AccessUtil.createAccessRule(subjectsOrcid, AccessUtil.createReadWritePermissions()));
        assertFalse(D1NodeService.equals(ap1, ap2));
        assertFalse(D1NodeService.equals(ap2, ap1));
        assertEquals(2, ap1.getAllowList().size());
        assertEquals("public", ap1.getAllow(0).getSubject(0).getValue());
        assertEquals(ap1.getAllow(0).getPermission(0), Permission.READ);
        assertEquals("http://orcid.org/0000-0002-8121-2341",
                     ap1.getAllow(1).getSubject(0).getValue());
        assertEquals(ap1.getAllow(1).getPermission(0), Permission.READ);
        assertEquals(2, ap2.getAllowList().size());
        assertEquals("public", ap2.getAllow(0).getSubject(0).getValue());
        assertEquals(ap2.getAllow(0).getPermission(0), Permission.READ);
        assertEquals("http://orcid.org/0000-0002-8121-2341",
                     ap2.getAllow(1).getSubject(0).getValue());
        assertEquals(ap2.getAllow(1).getPermission(0), Permission.READ);
        assertEquals(ap2.getAllow(1).getPermission(1), Permission.WRITE);

        //add a new permission for a new user
        ap1 = new AccessPolicy();
        ap1.addAllow(AccessUtil.createAccessRule(subjectsPublic, permissionsREAD));
        ap1.addAllow(AccessUtil.createAccessRule(subjectsOrcid, permissionsREAD));
        ap2 = new AccessPolicy();
        ap2.addAllow(AccessUtil.createAccessRule(subjectsPublic, permissionsREAD));
        ap2.addAllow(AccessUtil.createAccessRule(subjectsOrcid, permissionsREAD));
        ap2.addAllow(AccessUtil.createAccessRule(subjectsLDAP, permissionsWRITE));
        assertFalse(D1NodeService.equals(ap1, ap2));
        assertFalse(D1NodeService.equals(ap2, ap1));
        assertEquals(2, ap1.getAllowList().size());
        assertEquals("public", ap1.getAllow(0).getSubject(0).getValue());
        assertEquals(ap1.getAllow(0).getPermission(0), Permission.READ);
        assertEquals("http://orcid.org/0000-0002-8121-2341",
                     ap1.getAllow(1).getSubject(0).getValue());
        assertEquals(ap1.getAllow(1).getPermission(0), Permission.READ);
        assertEquals(3, ap2.getAllowList().size());
        assertEquals("public", ap2.getAllow(0).getSubject(0).getValue());
        assertEquals(ap2.getAllow(0).getPermission(0), Permission.READ);
        assertEquals("http://orcid.org/0000-0002-8121-2341",
                     ap2.getAllow(1).getSubject(0).getValue());
        assertEquals(ap2.getAllow(1).getPermission(0), Permission.READ);
        assertEquals(
            "CN=Bryce Mecum A27576,O=Google,C=US,DC=cilogon,DC=org",
            ap2.getAllow(2).getSubject(0).getValue());
        assertEquals(ap2.getAllow(2).getPermission(0), Permission.WRITE);

        //nothing change
        ap1 = new AccessPolicy();
        ap1.addAllow(AccessUtil.createAccessRule(subjectsPublic, permissionsREAD));
        ap1.addAllow(AccessUtil.createAccessRule(subjectsMix, AccessUtil.createReadWritePermissions()));
        ap2 = new AccessPolicy();
        ap2.addAllow(AccessUtil.createAccessRule(subjectsPublic, permissionsREAD));
        ap2.addAllow(AccessUtil.createAccessRule(subjectsMix, AccessUtil.createReadWritePermissions()));
        assertTrue(D1NodeService.equals(ap1, ap2));
        assertTrue(D1NodeService.equals(ap2, ap1));
        assertEquals(2, ap1.getAllowList().size());
        assertEquals("public", ap1.getAllow(0).getSubject(0).getValue());
        assertEquals(ap1.getAllow(0).getPermission(0), Permission.READ);
        assertEquals("http://orcid.org/0000-0002-8121-2341",
                     ap1.getAllow(1).getSubject(0).getValue());
        assertEquals(
            "CN=Bryce Mecum A27576,O=Google,C=US,DC=cilogon,DC=org",
            ap1.getAllow(1).getSubject(1).getValue());
        assertEquals(ap1.getAllow(1).getPermission(0), Permission.READ);
        assertEquals(ap1.getAllow(1).getPermission(1), Permission.WRITE);
        assertEquals(2, ap2.getAllowList().size());
        assertEquals("public", ap2.getAllow(0).getSubject(0).getValue());
        assertEquals(ap2.getAllow(0).getPermission(0), Permission.READ);
        assertEquals("http://orcid.org/0000-0002-8121-2341",
                     ap2.getAllow(1).getSubject(0).getValue());
        assertEquals(
            "CN=Bryce Mecum A27576,O=Google,C=US,DC=cilogon,DC=org",
            ap2.getAllow(1).getSubject(1).getValue());
        assertEquals(ap2.getAllow(1).getPermission(0), Permission.READ);
        assertEquals(ap2.getAllow(1).getPermission(1), Permission.WRITE);

        //different order, but same access rules
        ap1 = new AccessPolicy();
        ap1.addAllow(AccessUtil.createAccessRule(subjectsMix, AccessUtil.createReadWritePermissions()));
        ap1.addAllow(AccessUtil.createAccessRule(subjectsPublic, permissionsREAD));
        ap2 = new AccessPolicy();
        ap2.addAllow(AccessUtil.createAccessRule(subjectsPublic, permissionsREAD));
        ap2.addAllow(AccessUtil.createAccessRule(subjectsMix, permissionsWRITEREAD));
        assertTrue(D1NodeService.equals(ap1, ap2));
        assertTrue(D1NodeService.equals(ap2, ap1));
        assertEquals(2, ap1.getAllowList().size());
        assertEquals("http://orcid.org/0000-0002-8121-2341",
                     ap1.getAllow(0).getSubject(0).getValue());
        assertEquals(
            "CN=Bryce Mecum A27576,O=Google,C=US,DC=cilogon,DC=org",
            ap1.getAllow(0).getSubject(1).getValue());
        assertEquals(ap1.getAllow(0).getPermission(0), Permission.READ);
        assertEquals(ap1.getAllow(0).getPermission(1), Permission.WRITE);
        assertEquals("public", ap1.getAllow(1).getSubject(0).getValue());
        assertEquals(ap1.getAllow(1).getPermission(0), Permission.READ);
        assertEquals(2, ap2.getAllowList().size());
        assertEquals("public", ap2.getAllow(0).getSubject(0).getValue());
        assertEquals(ap2.getAllow(0).getPermission(0), Permission.READ);
        assertEquals("http://orcid.org/0000-0002-8121-2341",
                     ap2.getAllow(1).getSubject(0).getValue());
        assertEquals(
            "CN=Bryce Mecum A27576,O=Google,C=US,DC=cilogon,DC=org",
            ap2.getAllow(1).getSubject(1).getValue());
        assertEquals(ap2.getAllow(1).getPermission(0), Permission.WRITE);
        assertEquals(ap2.getAllow(1).getPermission(1), Permission.READ);

        //test duplicate rules
        ap1 = new AccessPolicy();
        ap1.addAllow(AccessUtil.createAccessRule(subjectsMix, AccessUtil.createReadWritePermissions()));
        ap1.addAllow(AccessUtil.createAccessRule(subjectsPublic, permissionsREAD));
        ap2 = new AccessPolicy();
        ap2.addAllow(AccessUtil.createAccessRule(subjectsMix, AccessUtil.createReadWritePermissions()));
        ap2.addAllow(AccessUtil.createAccessRule(subjectsPublic, permissionsREAD));
        ap2.addAllow(AccessUtil.createAccessRule(subjectsMix, permissionsWRITEREAD));
        ap2.addAllow(AccessUtil.createAccessRule(subjectsPublic, permissionsREAD));
        assertTrue(D1NodeService.equals(ap1, ap2));
        assertTrue(D1NodeService.equals(ap2, ap1));
        assertEquals(2, ap1.getAllowList().size());
        assertEquals("http://orcid.org/0000-0002-8121-2341",
                     ap1.getAllow(0).getSubject(0).getValue());
        assertEquals(
            "CN=Bryce Mecum A27576,O=Google,C=US,DC=cilogon,DC=org",
            ap1.getAllow(0).getSubject(1).getValue());
        assertEquals(ap1.getAllow(0).getPermission(0), Permission.READ);
        assertEquals(ap1.getAllow(0).getPermission(1), Permission.WRITE);
        assertEquals("public", ap1.getAllow(1).getSubject(0).getValue());
        assertEquals(ap1.getAllow(1).getPermission(0), Permission.READ);
        assertEquals(4, ap2.getAllowList().size());
        assertEquals("http://orcid.org/0000-0002-8121-2341",
                     ap2.getAllow(0).getSubject(0).getValue());
        assertEquals(
            "CN=Bryce Mecum A27576,O=Google,C=US,DC=cilogon,DC=org",
            ap2.getAllow(0).getSubject(1).getValue());
        assertEquals(ap2.getAllow(0).getPermission(0), Permission.READ);
        assertEquals(ap2.getAllow(0).getPermission(1), Permission.WRITE);
        assertEquals("public", ap2.getAllow(1).getSubject(0).getValue());
        assertEquals(ap2.getAllow(1).getPermission(0), Permission.READ);
        assertEquals("http://orcid.org/0000-0002-8121-2341",
                     ap2.getAllow(2).getSubject(0).getValue());
        assertEquals(
            "CN=Bryce Mecum A27576,O=Google,C=US,DC=cilogon,DC=org",
            ap2.getAllow(2).getSubject(1).getValue());
        assertEquals(ap2.getAllow(2).getPermission(0), Permission.WRITE);
        assertEquals(ap2.getAllow(2).getPermission(1), Permission.READ);
        assertEquals("public", ap2.getAllow(3).getSubject(0).getValue());
        assertEquals(ap2.getAllow(3).getPermission(0), Permission.READ);

        //test expanded access rule. One has read and write; another has write
        ap1 = new AccessPolicy();
        ap1.addAllow(AccessUtil.createAccessRule(subjectsMix, AccessUtil.createReadWritePermissions()));
        ap1.addAllow(AccessUtil.createAccessRule(subjectsPublic, permissionsREAD));
        ap2 = new AccessPolicy();
        ap2.addAllow(AccessUtil.createAccessRule(subjectsPublic, permissionsREAD));
        ap2.addAllow(AccessUtil.createAccessRule(subjectsMix, permissionsWRITE));
        assertTrue(D1NodeService.equals(ap1, ap2));
        assertTrue(D1NodeService.equals(ap2, ap1));
        assertEquals(2, ap1.getAllowList().size());
        assertEquals("http://orcid.org/0000-0002-8121-2341",
                     ap1.getAllow(0).getSubject(0).getValue());
        assertEquals(
            "CN=Bryce Mecum A27576,O=Google,C=US,DC=cilogon,DC=org",
            ap1.getAllow(0).getSubject(1).getValue());
        assertEquals(ap1.getAllow(0).getPermission(0), Permission.READ);
        assertEquals(ap1.getAllow(0).getPermission(1), Permission.WRITE);
        assertEquals("public", ap1.getAllow(1).getSubject(0).getValue());
        assertEquals(ap1.getAllow(1).getPermission(0), Permission.READ);
        assertEquals(2, ap2.getAllowList().size());
        assertEquals("public", ap2.getAllow(0).getSubject(0).getValue());
        assertEquals(ap2.getAllow(0).getPermission(0), Permission.READ);
        assertEquals("http://orcid.org/0000-0002-8121-2341",
                     ap2.getAllow(1).getSubject(0).getValue());
        assertEquals(
            "CN=Bryce Mecum A27576,O=Google,C=US,DC=cilogon,DC=org",
            ap2.getAllow(1).getSubject(1).getValue());
        assertEquals(ap2.getAllow(1).getPermission(0), Permission.WRITE);

        //test expanded access rule. One has read, write and change_permission, another has change_permission.
        ap1 = new AccessPolicy();
        ap1.addAllow(AccessUtil.createAccessRule(subjectsMix, AccessUtil.createReadWritePermissions()));
        ap1.addAllow(AccessUtil.createAccessRule(subjectsPublic, permissionsREAD));
        ap1.addAllow(AccessUtil.createAccessRule(subjectsMix, permissionsCHANGE));
        ap2 = new AccessPolicy();
        ap2.addAllow(AccessUtil.createAccessRule(subjectsPublic, permissionsREAD));
        ap2.addAllow(AccessUtil.createAccessRule(subjectsMix, permissionsCHANGE));
        assertTrue(D1NodeService.equals(ap1, ap2));
        assertTrue(D1NodeService.equals(ap2, ap1));
        assertEquals(3, ap1.getAllowList().size());
        assertEquals("http://orcid.org/0000-0002-8121-2341",
                     ap1.getAllow(0).getSubject(0).getValue());
        assertEquals(
            "CN=Bryce Mecum A27576,O=Google,C=US,DC=cilogon,DC=org",
            ap1.getAllow(0).getSubject(1).getValue());
        assertEquals(ap1.getAllow(0).getPermission(0), Permission.READ);
        assertEquals(ap1.getAllow(0).getPermission(1), Permission.WRITE);
        assertEquals("public", ap1.getAllow(1).getSubject(0).getValue());
        assertEquals(ap1.getAllow(1).getPermission(0), Permission.READ);
        assertEquals("http://orcid.org/0000-0002-8121-2341",
                     ap1.getAllow(2).getSubject(0).getValue());
        assertEquals(
            "CN=Bryce Mecum A27576,O=Google,C=US,DC=cilogon,DC=org",
            ap1.getAllow(2).getSubject(1).getValue());
        assertEquals(ap1.getAllow(2).getPermission(0), Permission.CHANGE_PERMISSION);
        assertEquals(2, ap2.getAllowList().size());
        assertEquals("public", ap2.getAllow(0).getSubject(0).getValue());
        assertEquals(ap2.getAllow(0).getPermission(0), Permission.READ);
        assertEquals("http://orcid.org/0000-0002-8121-2341",
                     ap2.getAllow(1).getSubject(0).getValue());
        assertEquals(
            "CN=Bryce Mecum A27576,O=Google,C=US,DC=cilogon,DC=org",
            ap2.getAllow(1).getSubject(1).getValue());
        assertEquals(ap2.getAllow(1).getPermission(0), Permission.CHANGE_PERMISSION);
    }

    /**
     * Test the isAccessControlDirty method
     * @throws Exception
     */
    public void testIsAccessControlDirty() throws Exception {
        printTestHeader("testIsAccessControlDirty");
        String[] subjectsPublic = {"public"};
        String[] subjectsMix = {"http://orcid.org/0000-0002-8121-2341", "CN=Bryce Mecum A27576,O=Google,C=US,DC=cilogon,DC=org"};
        String rightsHolder1 = "http://orcid.org/0000-0002-8121-2341";
        String rightsHolder2 = "uid=tao,o=NCEAS,dc=ecoinformatics,dc=org";
        String rightsHolder3 = "UID=tao,O=NCEAS,DC=ecoinformatics,DC=org";
        Permission[] permissionsREAD = {Permission.READ};
        Permission[] permissionsCHANGE = {Permission.CHANGE_PERMISSION};

        Subject sbjRightsHolder1 = new Subject();
        sbjRightsHolder1.setValue(rightsHolder1);
        Subject sbjRightsHolder2 = new Subject();
        sbjRightsHolder2.setValue(rightsHolder2);
        Subject sbjRightsHolder3 = new Subject();
        sbjRightsHolder3.setValue(rightsHolder3);
        AccessPolicy ap1 = null;
        AccessPolicy ap2 = null;
        ap1 = AccessUtil.createSingleRuleAccessPolicy(subjectsPublic, permissionsREAD);
        ap2 = AccessUtil.createSingleRuleAccessPolicy(subjectsMix, permissionsCHANGE);

        //the rights holder changes but access policy doesn't change
        SystemMetadata sysmeta1 = new SystemMetadata();
        sysmeta1.setRightsHolder(sbjRightsHolder1);
        sysmeta1.setAccessPolicy(ap1);
        SystemMetadata sysmeta2 = new SystemMetadata();
        sysmeta2.setRightsHolder(sbjRightsHolder2);
        sysmeta2.setAccessPolicy(ap1);
        assertTrue(D1NodeService.isAccessControlDirty(sysmeta1, sysmeta2));

        //both the rights holder and access policy change
        sysmeta1 = new SystemMetadata();
        sysmeta1.setRightsHolder(sbjRightsHolder1);
        sysmeta1.setAccessPolicy(ap1);
        sysmeta2 = new SystemMetadata();
        sysmeta2.setRightsHolder(sbjRightsHolder2);
        sysmeta2.setAccessPolicy(ap2);
        assertTrue(D1NodeService.isAccessControlDirty(sysmeta1, sysmeta2));

        //both the rights holder and access policy doesn't change
        sysmeta1 = new SystemMetadata();
        sysmeta1.setRightsHolder(sbjRightsHolder1);
        sysmeta1.setAccessPolicy(ap1);
        sysmeta2 = new SystemMetadata();
        sysmeta2.setRightsHolder(sbjRightsHolder1);
        sysmeta2.setAccessPolicy(ap1);
        assertFalse(D1NodeService.isAccessControlDirty(sysmeta1, sysmeta2));

        sysmeta1 = new SystemMetadata();
        sysmeta1.setRightsHolder(sbjRightsHolder2);
        sysmeta1.setAccessPolicy(ap1);
        sysmeta2 = new SystemMetadata();
        sysmeta2.setRightsHolder(sbjRightsHolder3); //just change the letter case
        sysmeta2.setAccessPolicy(ap1);
        assertFalse(D1NodeService.isAccessControlDirty(sysmeta1, sysmeta2));

        //the rights holder doesn't change but the access policy changes
        sysmeta1 = new SystemMetadata();
        sysmeta1.setRightsHolder(sbjRightsHolder1);
        sysmeta1.setAccessPolicy(ap1);
        sysmeta2 = new SystemMetadata();
        sysmeta2.setRightsHolder(sbjRightsHolder1);
        sysmeta2.setAccessPolicy(ap2);
        assertTrue(D1NodeService.isAccessControlDirty(sysmeta1, sysmeta2));

        //some scenario with null values
        sysmeta1 = null;
        sysmeta2 = null;
        assertFalse(D1NodeService.isAccessControlDirty(sysmeta1, sysmeta2));

        sysmeta1 = null;
        sysmeta2 = new SystemMetadata();
        sysmeta2.setRightsHolder(sbjRightsHolder1);
        sysmeta2.setAccessPolicy(ap2);
        assertTrue(D1NodeService.isAccessControlDirty(sysmeta1, sysmeta2));

        //without rights holder
        sysmeta1 = new SystemMetadata();
        sysmeta1.setAccessPolicy(ap1);
        sysmeta2 = new SystemMetadata();
        sysmeta2.setAccessPolicy(ap1);
        assertFalse(D1NodeService.isAccessControlDirty(sysmeta1, sysmeta2));

        sysmeta1 = new SystemMetadata();
        sysmeta1.setAccessPolicy(ap1);
        sysmeta2 = new SystemMetadata();
        sysmeta2.setAccessPolicy(ap2);
        assertTrue(D1NodeService.isAccessControlDirty(sysmeta1, sysmeta2));

        sysmeta1 = new SystemMetadata();
        sysmeta1.setAccessPolicy(ap1);
        sysmeta2 = new SystemMetadata();
        sysmeta2.setRightsHolder(sbjRightsHolder1);
        sysmeta2.setAccessPolicy(ap1);
        assertTrue(D1NodeService.isAccessControlDirty(sysmeta1, sysmeta2));

        sysmeta1 = new SystemMetadata();
        sysmeta1.setRightsHolder(sbjRightsHolder1);
        sysmeta1.setAccessPolicy(ap1);
        sysmeta2 = new SystemMetadata();
        sysmeta2.setAccessPolicy(ap1);
        assertTrue(D1NodeService.isAccessControlDirty(sysmeta1, sysmeta2));
    }
    
    /**
     * A wrapper method of MN.create.
     * @param session  the subject which will create the object
     * @param id  the identifier of the created object
     * @param object  the bytes of the object
     * @param sysmeta  the system metadata associated with the object
     * @throws InvalidToken
     * @throws ServiceFailure
     * @throws NotAuthorized
     * @throws IdentifierNotUnique
     * @throws UnsupportedType
     * @throws InsufficientResources
     * @throws InvalidSystemMetadata
     * @throws NotImplemented
     * @throws InvalidRequest
     * @throws MarshallingException
     * @throws InterruptedException
     * @throws RuntimeException
     * @throws IOException
     * @throws IllegalAccessException
     * @throws InstantiationException
     * @throws NoSuchAlgorithmException
     */
    public Identifier mnCreate(Session session, Identifier id, InputStream object,
                                    SystemMetadata sysmeta)
                                        throws InvalidToken, ServiceFailure, NotAuthorized,
                                        IdentifierNotUnique, UnsupportedType, InsufficientResources,
                                         InvalidSystemMetadata, NotImplemented, InvalidRequest,
                                         NoSuchAlgorithmException, InstantiationException,
                                         IllegalAccessException, IOException, RuntimeException,
                                        InterruptedException, MarshallingException {
        storeData(object, sysmeta);
        return MNodeService.getInstance(request).create(session, id, object, sysmeta);
    }

    /**
     * A wrapper method of MN.update
     * @param session  the subject which will create the object
     * @param pid  the identifier which will be updated
     * @param object  the bytes of the new object
     * @param newPid  the identifier which will replace the pid
     * @param sysmeta  the system metadata associated with the new object
     * @return the identifier of the new object
     * @throws IdentifierNotUnique
     * @throws InsufficientResources
     * @throws InvalidRequest
     * @throws InvalidSystemMetadata
     * @throws InvalidToken
     * @throws NotAuthorized
     * @throws NotImplemented
     * @throws ServiceFailure
     * @throws UnsupportedType
     * @throws NotFound
     * @throws MarshallingException
     * @throws InterruptedException
     * @throws RuntimeException
     * @throws IOException
     * @throws IllegalAccessException
     * @throws InstantiationException
     * @throws NoSuchAlgorithmException
     */
    public Identifier mnUpdate(Session session, Identifier pid, InputStream object,
                                           Identifier newPid, SystemMetadata sysmeta)
                              throws IdentifierNotUnique, InsufficientResources,
                              InvalidRequest, InvalidSystemMetadata, InvalidToken, NotAuthorized,
                              NotImplemented, ServiceFailure, UnsupportedType, NotFound,
                              NoSuchAlgorithmException, InstantiationException,
                              IllegalAccessException, IOException, RuntimeException,
                                                   InterruptedException, MarshallingException {
        storeData(object, sysmeta);
        return MNodeService.getInstance(request).update(session, pid, object, newPid, sysmeta);
    }

    /**
     * A wrapper method of CN.create.
     * @param session  the subject which will create the object
     * @param id  the identifier of the created object
     * @param object  the bytes of the object
     * @param sysmeta  the system metadata associated with the object
     * @return the identifier of the created object
     * @throws InvalidToken
     * @throws ServiceFailure
     * @throws NotAuthorized
     * @throws IdentifierNotUnique
     * @throws UnsupportedType
     * @throws InsufficientResources
     * @throws InvalidSystemMetadata
     * @throws NotImplemented
     * @throws InvalidRequest
     * @throws MarshallingException
     * @throws InterruptedException
     * @throws RuntimeException
     * @throws IOException
     * @throws IllegalAccessException
     * @throws InstantiationException
     * @throws NoSuchAlgorithmException
     */
    public Identifier cnCreate(Session session, Identifier id, InputStream object, SystemMetadata sysmeta)
                                throws InvalidToken, ServiceFailure, NotAuthorized,
                                IdentifierNotUnique, UnsupportedType, InsufficientResources,
                                InvalidSystemMetadata, NotImplemented, InvalidRequest,
                                NoSuchAlgorithmException, InstantiationException,
                                IllegalAccessException, IOException, RuntimeException,
                                InterruptedException, MarshallingException {
        storeData(object, sysmeta);
        return CNodeService.getInstance(request).create(session, id, object, sysmeta);
    }

    /**
     * Store the input stream into hash store
     * @param object  the input stream represents the content of the object
     * @param sysmeta  the system metadata of the object
     * @throws NoSuchAlgorithmException
     * @throws InvalidRequest
     * @throws IOException
     * @throws RuntimeException
     * @throws InterruptedException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws MarshallingException
     */
    public static void storeData(InputStream object, org.dataone.service.types.v1.SystemMetadata sysmeta)
                                     throws NoSuchAlgorithmException, InvalidRequest, IOException,
                                     RuntimeException, InterruptedException, InstantiationException,
                                                    IllegalAccessException, MarshallingException {
        // null is for additional algorithm
        storage.storeObject(object, sysmeta.getIdentifier(), null, sysmeta.getChecksum().getValue(),
                            sysmeta.getChecksum().getAlgorithm(), sysmeta.getSize().longValue());
    }

    /**
     * Read a document from metacat and check if it is equal to a given string.
     * The expected result is passed as result
     */
    public void readDocidWhichEqualsDoc(String docid, String testDoc,
                                            boolean result, Session session) {
        try {
            Identifier guid = new Identifier();
            guid.setValue(docid);
            InputStream object = MNodeService.getInstance(request).get(session, guid);
            String doc = IOUtils.toString(object, StandardCharsets.UTF_8);
            if (!testDoc.equals(doc)) {
                    debug("doc    :" + doc);
                    debug("testDoc:" + testDoc);
            }
            assertEquals(testDoc, doc);
        } catch (Exception e) {
            fail("General exception:\n" + e.getMessage());
        }
    }
    
    /**
     * Use the solr query to query a title. If the result doesn't contain the given
     * guid, test will fail.
     */
    public void queryTile(String title, String guid, Session session) throws Exception {
        String query = "q=title:" +"\"" + title +"\"";
        InputStream stream = MNodeService.getInstance(request).query(session, "solr", query);
        String resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        int count = 0;
        while ( (resultStr == null || !resultStr.contains(guid)) 
                                    && count <= D1NodeServiceTest.MAX_TRIES) {
            Thread.sleep(1000);
            count++;
            stream = MNodeService.getInstance(request).query(session, "solr", query);
            resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        }
        assertTrue(resultStr.contains(guid));
    }

    private void mockConfig() {

        Configuration origConfig = Settings.getConfiguration();
        Configuration testConfig = new PropertiesConfiguration();
        for (Iterator<String> it = origConfig.getKeys(); it.hasNext(); ) {
            String key = String.valueOf(it.next());
            testConfig.addProperty(key, origConfig.getProperty(key));
        }
        testConfig.clearProperty("tls.protocol.preferences");
        testConfig.addProperty("tls.protocol.preferences", "TLSv1.3, TLSv1.2, TLS");
        testConfig.clearProperty("D1Client.CN_URL");
        testConfig.addProperty("D1Client.CN_URL", MockCNode.MOCK_CN_BASE_SERVICE_URL);
        testConfig.clearProperty("dataone.subject");
        testConfig.addProperty("dataone.subject", "CN=urn:node:METACAT1,DC=dataone,DC=org");

        mockStaticSettings = Mockito.mockStatic(Settings.class);
        Mockito.when(Settings.getConfiguration()).thenReturn(testConfig);
    }
}
