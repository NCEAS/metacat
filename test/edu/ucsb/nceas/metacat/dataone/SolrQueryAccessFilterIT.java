package edu.ucsb.nceas.metacat.dataone;


import org.apache.wicket.protocol.http.mock.MockHttpServletRequest;
import org.dataone.client.auth.CertificateManager;
import org.dataone.client.v2.MNode;
import org.dataone.client.v2.formats.ObjectFormatCache;
import org.dataone.client.v2.itk.D1Client;
import org.dataone.configuration.Settings;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.types.v1.AccessPolicy;
import org.dataone.service.types.v1.AccessRule;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Permission;
import org.dataone.service.types.v1.Person;
import org.dataone.service.types.v1.Session;
import org.dataone.service.types.v1.Subject;
import org.dataone.service.types.v1.SubjectInfo;
import org.dataone.service.types.v2.SystemMetadata;
import org.dataone.service.util.Constants;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;


/**
 * A test class to test the access filter mechanism for the solr query
 *
 * @author tao
 */
public class SolrQueryAccessFilterIT {

    private D1NodeServiceTest d1NodeServiceTest;
    private MockHttpServletRequest request;
    private static final String SOLR = "solr";
    private static final String EML201NAMESPACE = "eml://ecoinformatics.org/eml-2.0.1";
    private static final String CREATEUSER =
        "CN=Christopher Jones A583,O=Google,C=US,DC=cilogon,DC=org";
    private static final String QUERYUSER =
        "CN=ben leinfelder A756,O=Google,C=US,DC=cilogon,DC=org";
    private static final String GROUP1 = "CN=PISCO-data-managers,DC=cilogon,DC=org";
    private static final String GROUP2 = "CN=dataone-coredev,DC=cilogon,DC=org";
    private static final String USERWITHCERT = "CN=Jing Tao,OU=NCEAS,O=UCSB,ST=California,C=US";
    private static final String EMLFILE = "test/restfiles/knb-lter-gce.109.6.xml";
    private static final String INTRUSTCERTFILE = "test/test-credentials/test-user.pem";
    private static final String IDXPATH = "//response/result/doc/str[@name='id']/text()";
    private static final String TITLEPATH = "//response/result/doc/str[@name='title']/text()";
    private static final String TITLE =
        "Mollusc population abundance monitoring: Fall 2000 mid-marsh and creekbank infaunal and "
            + "epifaunal mollusc abundance based on collections from GCE marsh, monitoring sites "
            + "1-10";




    /**
     * Set up the test fixtures
     *
     * @throws Exception
     */
    @Before
    public void setUp() throws Exception {
        d1NodeServiceTest = new D1NodeServiceTest("initialize");
        request = d1NodeServiceTest.request;
        d1NodeServiceTest.setUp();
        // set up the configuration for d1client
        Settings.getConfiguration().setProperty("D1Client.cnClassName", MockCNode.class.getName());
    }

    /**
     * Release any objects after tests are complete
     */
    @After
    public void tearDown() {
        // set back to force it to use defaults
        d1NodeServiceTest.tearDown();
    }

    /**
     * Test to query a public readable document
     */
    @Test
    public void testPublicReadable() throws Exception {
        Session session = getSession(CREATEUSER, null);
        Identifier id = generateIdentifier();
        String[] allowUsers = {Constants.SUBJECT_PUBLIC};
        File object = new File(EMLFILE);
        SystemMetadata sysmeta =
            generateSystemMetadata(id, session.getSubject(), object, allowUsers);
        createObject(session, id, object, sysmeta);
        Thread.sleep(10000);
        Session querySession = getSession(Constants.SUBJECT_PUBLIC, null);
        InputStream input = query(querySession, id);
        Document doc = generateDoc(input);
        String resultId = extractElementValue(doc, IDXPATH);
        assertEquals("testPublicReadable: query result ID is incorrect", id.getValue(), resultId);
        String title = extractElementValue(doc, TITLEPATH);
        assertEquals(TITLE, title);
        Session querySession2 = getSession(QUERYUSER, null);
        input = query(querySession2, id);
        doc = generateDoc(input);
        String resultId2 = extractElementValue(doc, IDXPATH);
        assertEquals(
            "In the testPublicReadable method, the query result should have the id " + id.getValue()
                + " rather than " + resultId2, resultId2, id.getValue());
        title = extractElementValue(doc, TITLEPATH);
        assertEquals(TITLE, title);

        archive(session, id);
        input = query(querySession2, id);
        doc = generateDoc(input);
        String resultId3 = extractElementValue(doc, IDXPATH);
        int count = 0;
        while (resultId3 != null && count++ < D1NodeServiceTest.MAX_TRIES) {
            Thread.sleep(1000);
            input = query(querySession2, id);
            doc = generateDoc(input);
            resultId3 = extractElementValue(doc, IDXPATH);
        }
        assertNull(
            "In the testPublicReadable method, the query result should be null since the document"
                + " was archived. ", resultId3);
    }


    /**
     * Test to query a document which can only be read by a specified user
     */
    @Test
    public void testOnlyUserReadable() throws Exception {
        Thread.sleep(15000);
        Session session = getSession(CREATEUSER, null);
        Identifier id = generateIdentifier();
        String[] allowUsers = {QUERYUSER};
        File object = new File(EMLFILE);
        SystemMetadata sysmeta =
            generateSystemMetadata(id, session.getSubject(), object, allowUsers);
        createObject(session, id, object, sysmeta);

        Thread.sleep(10000);
        Session querySession = getSession(Constants.SUBJECT_PUBLIC, null);
        InputStream input = query(querySession, id);
        Document doc = generateDoc(input);
        String resultId = extractElementValue(doc, IDXPATH);
        assertNull("In the testOnlyUserReadable method, the query result id should be null for the "
                       + "public rather than " + resultId, resultId);
        Session querySession2 = getSession(QUERYUSER, null);
        input = query(querySession2, id);
        doc = generateDoc(input);
        resultId = extractElementValue(doc, IDXPATH);
        assertEquals("testOnlyUserReadable: query result for the user " + QUERYUSER + " has the "
                         + "wrong ID", id.getValue(), resultId);
        String title = extractElementValue(doc, TITLEPATH);
        assertEquals(TITLE, title);
        archive(session, id);
    }

    /**
     * Test to query a document which can be read by a specified group
     */
    @Test
    public void testGroupReadable() throws Exception {
        Thread.sleep(15000);
        Session session = getSession(CREATEUSER, null);
        Identifier id = generateIdentifier();
        String[] allowUsers = {GROUP1, GROUP2};
        File object = new File(EMLFILE);
        SystemMetadata sysmeta =
            generateSystemMetadata(id, session.getSubject(), object, allowUsers);
        createObject(session, id, object, sysmeta);
        Thread.sleep(10000);
        Session querySession = getSession(Constants.SUBJECT_PUBLIC, null);
        InputStream input = query(querySession, id);
        Document doc = generateDoc(input);
        String resultId = extractElementValue(doc, IDXPATH);
        assertNull(
            "In the testGroupReadable method, the query result id should be null for the public ",
            resultId);
        Session querySession2 = getSession(QUERYUSER, null);
        input = query(querySession2, id);
        doc = generateDoc(input);
        resultId = extractElementValue(doc, IDXPATH);
        assertNull("In the testGroupReadable method, the query result for the user " + QUERYUSER
                       + " which doesn't belong to the group should be null ", resultId);
        String[] groups = {GROUP1};
        Session querySession3 = getSession(QUERYUSER, groups);
        input = query(querySession3, id);
        doc = generateDoc(input);
        resultId = extractElementValue(doc, IDXPATH);

        assertEquals("testGroupReadable: query result for the user " + QUERYUSER
                         + " which belong to the group has the wrong ID ", id.getValue(), resultId);
        String title = extractElementValue(doc, TITLEPATH);
        assertEquals(TITLE, title);
        archive(session, id);
    }


    /**
     * Test to query a document which only can be read by the rightHolder
     */
    @Test
    public void testOnlyRightHolderReadable() throws Exception {
        Thread.sleep(15000);
        Session session = getSession(CREATEUSER, null);
        Identifier id = generateIdentifier();
        String[] allowUsers = null;
        File object = new File(EMLFILE);
        SystemMetadata sysmeta =
            generateSystemMetadata(id, session.getSubject(), object, allowUsers);
        createObject(session, id, object, sysmeta);
        Thread.sleep(10000);
        Session querySession = getSession(Constants.SUBJECT_PUBLIC, null);
        InputStream input = query(querySession, id);
        Document doc = generateDoc(input);
        String resultId = extractElementValue(doc, IDXPATH);
        assertNull(
            "In the testOnlyRightHolderReadable method, the query result id should be null for "
                + "the public ", resultId);
        Session querySession2 = getSession(QUERYUSER, null);
        input = query(querySession2, id);
        doc = generateDoc(input);
        resultId = extractElementValue(doc, IDXPATH);
        assertNull(
            "In the testOnlyRightHolderReadable method, the query result for the user " + QUERYUSER
                + " which doesn't belong to the group should be null.", resultId);
        String[] groups = {GROUP1};
        Session querySession3 = getSession(QUERYUSER, groups);
        input = query(querySession3, id);
        doc = generateDoc(input);
        resultId = extractElementValue(doc, IDXPATH);
        assertNull(
            "In the testOnlyRightHolderReadable method, the query result for the user " + QUERYUSER
                + " which belong to the group should be null.", resultId);
        Session querySession4 = getSession(CREATEUSER, groups);
        input = query(querySession4, id);
        doc = generateDoc(input);
        resultId = extractElementValue(doc, IDXPATH);
        assertEquals("testOnlyRightHolderReadable: query result for the creator " + CREATEUSER
                         + " is incorrect ", id.getValue(), resultId);
        String title = extractElementValue(doc, TITLEPATH);
        assertEquals(TITLE, title);
        archive(session, id);
    }

    /**
     * Test a user with a distrusted certificate.
     *
     * @throws Exception
     */
    @Test
    public void testDistrustCertificate() throws Exception {
        //create an object only be readable by the USERWITHCERT
        Session session = getSession(CREATEUSER, null);
        Identifier id = generateIdentifier();
        String[] allowUsers = {USERWITHCERT};
        File object = new File(EMLFILE);
        SystemMetadata sysmeta =
            generateSystemMetadata(id, session.getSubject(), object, allowUsers);
        createObject(session, id, object, sysmeta);
        Thread.sleep(10000);

        //use faking session, the user can query the document
        Session querySession = getSession(USERWITHCERT, null);

        InputStream input = query(querySession, id);
        Document doc = generateDoc(input);
        String resultId = extractElementValue(doc, IDXPATH);
        assertEquals("testDistrustCertificate: query result for the creator " + CREATEUSER
                         + " is incorrect ", id.getValue(), resultId);
        String title = extractElementValue(doc, TITLEPATH);
        assertEquals(TITLE, title);

        // Use the libclient without the session: the user shouldn't query the document since its
        // certificate is distrusted, and it will be considered public.
        org.dataone.service.types.v2.Node node =
            MNodeService.getInstance(request).getCapabilities();
        CertificateManager.getInstance().setCertificateLocation(INTRUSTCERTFILE);
        String baseURL = node.getBaseURL();
        System.out.println("================The base url is " + baseURL);
        if (baseURL.contains("https://localhost")) {
            // force localhost to skip https - most common for devs
            baseURL = baseURL.replace("https", "http");
            baseURL = baseURL.replace("8443", "8080");
            baseURL = baseURL.replace("443", "80");
        }
        System.out.println("================The MODIFIED base url is " + baseURL);
        MNode mnNode = D1Client.getMN(baseURL);
        try {
            input = mnNode.query(querySession, SOLR, generateQuery(id.getValue()));
            fail("Can't reach here since it is an untrusted certificate");
        } catch (Exception e) {
            System.out.println("The exception is " + e.getMessage());
            System.out.println("the exception class is " + e.getClass().getCanonicalName());
            assertTrue(e instanceof ServiceFailure);
        }
        archive(session, id);
    }

    /*
     * constructs a "fake" session with the specified subject and groups.
     * If groups is not null, the session will have a subjectinfo which contains the person with
     * the subject and is the member of the groups.
     * @return
     */
    private Session getSession(String subjectValue, String[] groups) {
        Session session = new Session();
        Subject subject = new Subject();
        subject.setValue(subjectValue);
        session.setSubject(subject);
        if (groups != null) {
            Person person = new Person();
            person.setSubject(subject);
            person.setVerified(Boolean.TRUE);
            List<Subject> groupSubjects = new ArrayList<>();
            for (String group : groups) {
                Subject groupSub = new Subject();
                groupSub.setValue(group);
                groupSubjects.add(groupSub);
            }
            person.setIsMemberOfList(groupSubjects);
            SubjectInfo subjectInfo = new SubjectInfo();
            subjectInfo.addPerson(person);
            session.setSubjectInfo(subjectInfo);
        }
        return session;
    }

    /*
     * Create a data object in the dataone server.
     * Return the identifier of the created object
     */
    private void createObject(Session session, Identifier id, File object, SystemMetadata sysmeta)
        throws Exception {
        d1NodeServiceTest.mnCreate(session, id, new FileInputStream(object), sysmeta);

    }

    private Identifier generateIdentifier() {
        Identifier guid = new Identifier();
        long random = Math.round(Math.random() * 10000);
        guid.setValue("test." + System.currentTimeMillis() + random);
        return guid;
    }

    /*
     * Archive the given id.
     */
    private void archive(Session session, Identifier id) throws Exception {
        MNodeService.getInstance(request).archive(session, id);
    }


    /*
     * Generate system metadata for the file
     */
    private SystemMetadata generateSystemMetadata(
        Identifier id, Subject owner, File objectFile, String[] allowedSubjects) throws Exception {
        SystemMetadata sysmeta = D1NodeServiceTest.createSystemMetadata(id, owner,
                                                                  new FileInputStream(objectFile));
        AccessPolicy accessPolicy = null;
        if (allowedSubjects != null && allowedSubjects.length > 0) {
            accessPolicy = new AccessPolicy();
            for (int i = 0; i < allowedSubjects.length; i++) {
                AccessRule allow = new AccessRule();
                allow.addPermission(Permission.READ);
                Subject subject = new Subject();
                subject.setValue(allowedSubjects[i]);
                allow.addSubject(subject);
                accessPolicy.addAllow(allow);
            }
        }
        sysmeta.setAccessPolicy(accessPolicy);
        sysmeta.setFormatId(
            ObjectFormatCache.getInstance().getFormat(EML201NAMESPACE).getFormatId());
        return sysmeta;
    }

    /*
     * Query the server to find the doc which matches the specified id
     */
    private InputStream query(Session session, Identifier id) throws Exception {
        String query = generateQuery(id.getValue());
        MNodeService service = MNodeService.getInstance(request);
        service.setSession(session);
        InputStream input = service.query(session, SOLR, query);
        return input;
    }

    /*
     *
     */
    private Document generateDoc(InputStream input) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new InputSource(input));
        return doc;
    }

    /*
     * Extract the return id from the query result input stream
     */
    private String extractElementValue(Document doc, String path) throws Exception {
        String id = null;
        XPathFactory xPathfactory = XPathFactory.newInstance();
        XPath xpath = xPathfactory.newXPath();
        XPathExpression expr = xpath.compile(path);
        Object result = expr.evaluate(doc, XPathConstants.NODESET);
        System.out.println("================ result is " + result);
        if (result != null) {
            NodeList nodes = (NodeList) result;
            if (nodes != null) {
                System.out.println("the length of nodes is " + nodes.getLength());
                Node node = nodes.item(0);
                if (node != null) {
                    id = node.getNodeValue();
                }

            }

        }

        System.out.println("the value of the element " + path + " is ====== " + id);
        return id;

    }

    /*
     * Make a query string which will query "id= the specified id".
     * @param id
     * @return
     */
    private String generateQuery(String id) {
        String query = "q=id:" + id + "&fl=id,title";
        return query;
    }

}
