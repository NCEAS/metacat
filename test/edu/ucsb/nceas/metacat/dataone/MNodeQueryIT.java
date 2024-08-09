package edu.ucsb.nceas.metacat.dataone;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import edu.ucsb.nceas.LeanTestUtils;
import edu.ucsb.nceas.metacat.dataone.quota.QuotaServiceManager;
import edu.ucsb.nceas.metacat.dataone.quota.QuotaServiceManagerIT;
import edu.ucsb.nceas.metacat.dataone.resourcemap.ResourceMapModifier;
import edu.ucsb.nceas.metacat.object.handler.JsonLDHandlerTest;
import edu.ucsb.nceas.metacat.object.handler.NonXMLMetadataHandlers;
import edu.ucsb.nceas.metacat.util.SystemUtil;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.wicket.protocol.http.mock.MockHttpServletRequest;
import org.dataone.client.rest.DefaultHttpMultipartRestClient;
import org.dataone.configuration.Settings;
import org.dataone.mimemultipart.SimpleMultipartEntity;
import org.dataone.ore.ResourceMapFactory;
import org.dataone.service.types.v1.AccessPolicy;
import org.dataone.service.types.v1.AccessRule;
import org.dataone.service.types.v1.Checksum;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.ObjectFormatIdentifier;
import org.dataone.service.types.v1.Permission;
import org.dataone.service.types.v1.Session;
import org.dataone.service.types.v1.Subject;
import org.dataone.service.types.v2.SystemMetadata;
import org.dataone.service.util.TypeMarshaller;
import org.dataone.vocabulary.CITO;
import org.dspace.foresite.ResourceMap;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * A JUnit test to exercise the Metacat Member Node query service implementation.
 *
 * @author cjones
 */
public class MNodeQueryIT {
    private final long DEFAULT_SLEEP_MS= 1000;
    private static String taxonomyFilePath = "test/eml-with-taxonomy.xml";
    private static String portalFilePath =
        "metacat-index/src/test/resources/collection/portal-example-simple.xml";
    private static String portalResultFilePath =
        "metacat-index/src/test/resources/collection/collectionQuery-result-example-simple.txt";
    private static String portal110FilePath =
        "metacat-index/src/test/resources/collection/portal-1.1.0-example.xml";
    private static String portal110ResultFilePath =
        "metacat-index/src/test/resources/collection/collectionQuery-result-portal-1.1.0.txt";
    private static String collection110FilePath =
        "metacat-index/src/test/resources/collection/collection-1.1"
            + ".0-example-filterGroup-operator.xml";
    private static String collection110ResultFilePath =
        "metacat-index/src/test/resources/collection/collectionQuery-result-example-filterGroup"
            + "-operator.txt";
    private static String emlWithAnnotation = "test/eml220withAnnotation.xml";

    private static String collectionResult = null;
    private static String collectionQueryPortal110 = null;
    private static String collectionQueryCollection110 = null;
    private static final String CN_BASE_URL = "https://cn.dataone.org/cn";
    private static final String longQueryFile = "test/test-queries/long-solr-query-partial.txt";
    private final D1NodeServiceTest d1NodeServiceTest;
    private final MockHttpServletRequest request;

    /**
     * Set up the test fixtures
     *
     * @throws Exception
     */
    @Before
    public void setUp() throws Exception {
        d1NodeServiceTest.setUp();
        Settings.getConfiguration().clearProperty("D1Client.CN_URL");
        Settings.getConfiguration().addProperty("D1Client.CN_URL", CN_BASE_URL);
        // set up the configuration for d1client
        Settings.getConfiguration().setProperty("D1Client.cnClassName", MockCNode.class.getName());
    }

    /**
     * Remove the test fixtures
     */
    @After
    public void tearDown() {
        d1NodeServiceTest.tearDown();
    }

    /**
     * Constructor for the tests
     */
    public MNodeQueryIT() {
        d1NodeServiceTest = new D1NodeServiceTest("MNodeQueryIT");
        request = d1NodeServiceTest.request;
        try {
            collectionQueryPortal110 = FileUtils
                .readFileToString(new File(portal110ResultFilePath), StandardCharsets.UTF_8).trim();
            collectionQueryCollection110 = FileUtils
                .readFileToString(new File(collection110ResultFilePath), StandardCharsets.UTF_8)
                .trim();
            collectionResult =
                FileUtils.readFileToString(new File(portalResultFilePath), StandardCharsets.UTF_8)
                    .trim();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testQueryOfArchivedObjects() throws Exception {
        D1NodeServiceTest.printTestHeader("testQueryOfArchivedObjects");

        Session session = d1NodeServiceTest.getTestSession();
        Identifier guid = new Identifier();
        HashMap<String, String[]> params = null;
        guid.setValue("testUpdate." + System.currentTimeMillis());
        InputStream object = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
        SystemMetadata sysmeta =
            D1NodeServiceTest.createSystemMetadata(guid, session.getSubject(), object);
        object = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
        Identifier pid = d1NodeServiceTest.mnCreate(session, guid, object, sysmeta);
        String query = "q=id:" + guid.getValue();
        InputStream stream = MNodeService.getInstance(request).query(session, "solr", query);
        String resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        int count = 0;
        while ((resultStr == null || !resultStr.contains("checksum"))
            && count <= D1NodeServiceTest.MAX_TRIES) {
            Thread.sleep(DEFAULT_SLEEP_MS);
            count++;
            stream = MNodeService.getInstance(request).query(session, "solr", query);
            resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        }
        assertNotNull(resultStr);
        assertTrue(resultStr.contains("<str name=\"id\">" + guid.getValue() + "</str>"));
        assertTrue(resultStr.contains("<bool name=\"archived\">false</bool>"));
        // post query
        params = new HashMap<>();
        String[] qValue = {"id:" + guid.getValue()};
        params.put("q", qValue);
        stream = MNodeService.getInstance(request).postQuery(session, "solr", params);
        resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        assertTrue(resultStr.contains("<str name=\"id\">" + guid.getValue() + "</str>"));
        assertTrue(resultStr.contains("<bool name=\"archived\">false</bool>"));
        // only return id
        String[] flValue = {"id"};
        params.put("fl", flValue);
        stream = MNodeService.getInstance(request).postQuery(session, "solr", params);
        resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        assertTrue(resultStr.contains("<str name=\"id\">" + guid.getValue() + "</str>"));
        assertFalse(resultStr.contains("<bool name=\"archived\">false</bool>"));

        MNodeService.getInstance(request).archive(session, guid);
        SystemMetadata result = MNodeService.getInstance(request).getSystemMetadata(session, guid);
        stream = MNodeService.getInstance(request).query(session, "solr", query);
        resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        count = 0;
        while ((resultStr == null || resultStr.contains("<bool name=\"archived\">"))
            && count <= D1NodeServiceTest.MAX_TRIES) {
            Thread.sleep(DEFAULT_SLEEP_MS);
            count++;
            stream = MNodeService.getInstance(request).query(session, "solr", query);
            resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        }

        assertNotNull(resultStr);
        assertFalse(resultStr.contains("<str name=\"id\">" + guid.getValue() + "</str>"));
        assertFalse(resultStr.contains("<bool name=\"archived\">false</bool>"));
        // postquery
        stream = MNodeService.getInstance(request).postQuery(session, "solr", params);
        resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        assertFalse(
            "guid value is: " + guid.getValue() + ", and resultStr contains: <str name=\"id\">"
                + guid.getValue() + "</str>)",
            resultStr.contains("<str name=\"id\">" + guid.getValue() + "</str>"));
        assertFalse(resultStr.contains("<bool name=\"archived\">false</bool>"));

        query = "q=id:" + guid.getValue() + "&archived=archived:true";
        stream = MNodeService.getInstance(request).query(session, "solr", query);
        resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        LeanTestUtils.debug("the guid is " + guid.getValue());
        LeanTestUtils.debug("the string is +++++++++++++++++++++++++++++++++++\n" + resultStr);
        assertTrue(resultStr.contains("<str name=\"id\">" + guid.getValue() + "</str>"));
        assertTrue(resultStr.contains("<bool name=\"archived\">true</bool>"));
        // postquery
        params = new HashMap<>();
        String[] qValue2 = {"id:" + guid.getValue()};
        params.put("q", qValue2);
        String[] archivedValue = {"archived:true"};
        params.put("archived", archivedValue);
        stream = MNodeService.getInstance(request).postQuery(session, "solr", params);
        resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        assertTrue(resultStr.contains("<str name=\"id\">" + guid.getValue() + "</str>"));
        assertTrue(resultStr.contains("<bool name=\"archived\">true</bool>"));

    }

    /***
     * Test the indexing a package (the resource map, metadata and data files)
     * @throws Exception
     */
    @Test
    public void testPackage() throws Exception {
        D1NodeServiceTest.printTestHeader("testPackage");
        // insert data
        Session session = d1NodeServiceTest.getTestSession();
        Identifier guid = new Identifier();
        HashMap<String, String[]> params = null;
        guid.setValue("testPackage-data." + System.currentTimeMillis());
        LeanTestUtils.debug("the data file id is ==== " + guid.getValue());
        InputStream object = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
        SystemMetadata sysmeta =
            D1NodeServiceTest.createSystemMetadata(guid, session.getSubject(), object);
        object = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
        d1NodeServiceTest.mnCreate(session, guid, object, sysmeta);

        // insert metadata
        Identifier guid2 = new Identifier();
        guid2.setValue("testPackage-metadata." + System.currentTimeMillis());
        LeanTestUtils.debug("the metadata  file id is ==== " + guid2.getValue());
        InputStream object2 = new FileInputStream(MNodeReplicationTest.replicationSourceFile);
        SystemMetadata sysmeta2 =
            D1NodeServiceTest.createSystemMetadata(guid2, session.getSubject(), object2);
        object2.close();
        ObjectFormatIdentifier formatId = new ObjectFormatIdentifier();
        formatId.setValue("eml://ecoinformatics.org/eml-2.0.1");
        sysmeta2.setFormatId(formatId);
        object2 = new FileInputStream(MNodeReplicationTest.replicationSourceFile);
        d1NodeServiceTest.mnCreate(session, guid2, object2, sysmeta2);

        // Make sure both data and metadata objects have been indexed
        String query = "q=id:" + guid.getValue();
        InputStream stream = MNodeService.getInstance(request).query(session, "solr", query);
        String resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        int count = 0;
        while ((resultStr == null || !resultStr.contains("checksum"))
            && count <= D1NodeServiceTest.MAX_TRIES) {
            Thread.sleep(DEFAULT_SLEEP_MS);
            count++;
            stream = MNodeService.getInstance(request).query(session, "solr", query);
            resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        }

        assertNotNull(resultStr);

        query = "q=id:" + guid2.getValue();
        stream = MNodeService.getInstance(request).query(session, "solr", query);
        resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        count = 0;
        while ((resultStr == null || !resultStr.contains("checksum"))
            && count <= D1NodeServiceTest.MAX_TRIES) {
            Thread.sleep(DEFAULT_SLEEP_MS);
            count++;
            stream = MNodeService.getInstance(request).query(session, "solr", query);
            resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        }

        assertNotNull(resultStr);

        Map<Identifier, List<Identifier>> idMap = new HashMap<>();
        List<Identifier> dataIds = new ArrayList<>();
        dataIds.add(guid);
        idMap.put(guid2, dataIds);
        Identifier resourceMapId = new Identifier();
        // use the local id, not the guid in case we have DOIs for them already
        resourceMapId.setValue("testPackage-resourcemap." + System.currentTimeMillis());
        LeanTestUtils.debug("the resource file id is ==== " + resourceMapId.getValue());
        ResourceMap rm = ResourceMapFactory.getInstance().createResourceMap(resourceMapId, idMap);
        String resourceMapXML = ResourceMapFactory.getInstance().serializeResourceMap(rm);
        InputStream object3 =
            new ByteArrayInputStream(resourceMapXML.getBytes(StandardCharsets.UTF_8));
        SystemMetadata sysmeta3 =
            D1NodeServiceTest.createSystemMetadata(resourceMapId, session.getSubject(), object3);
        ObjectFormatIdentifier formatId3 = new ObjectFormatIdentifier();
        formatId3.setValue("http://www.openarchives.org/ore/terms");
        sysmeta3.setFormatId(formatId3);
        object3 = new ByteArrayInputStream(resourceMapXML.getBytes(StandardCharsets.UTF_8));
        d1NodeServiceTest.mnCreate(session, resourceMapId, object3, sysmeta3);

        query = "q=id:" + guid.getValue();
        stream = MNodeService.getInstance(request).query(session, "solr", query);
        resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        count = 0;
        while ((resultStr == null || !resultStr.contains("isDocumentedBy"))
            && count <= D1NodeServiceTest.MAX_TRIES) {
            Thread.sleep(DEFAULT_SLEEP_MS);
            count++;
            stream = MNodeService.getInstance(request).query(session, "solr", query);
            resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        }

        assertNotNull(resultStr);
        assertTrue(resultStr.contains("<arr name=\"isDocumentedBy\">"));
        assertTrue("resultStr should contain guid2 value: " + guid2.getValue(),
                   resultStr.contains(guid2.getValue()));
        assertTrue(resultStr.contains("<arr name=\"resourceMap\">"));
        assertTrue("resultStr should contain resourceMapId value: " + resourceMapId.getValue(),
                   resultStr.contains(resourceMapId.getValue()));

        // postquery
        params = new HashMap<>();
        String[] qValue = {"id:" + guid.getValue()};
        params.put("q", qValue);
        stream = MNodeService.getInstance(request).postQuery(session, "solr", params);
        resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);

        assertNotNull(resultStr);
        assertTrue(resultStr.contains("<arr name=\"isDocumentedBy\">"));
        assertTrue("resultStr should contain guid2 value: " + guid2.getValue(),
                   resultStr.contains(guid2.getValue()));
        assertTrue(resultStr.contains("<arr name=\"resourceMap\">"));
        assertTrue("resultStr should contain resourceMapId value: " + resourceMapId.getValue(),
                   resultStr.contains(resourceMapId.getValue()));

        query = "q=id:" + guid2.getValue();
        stream = MNodeService.getInstance(request).query(session, "solr", query);
        resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        count = 0;
        while ((resultStr == null || !resultStr.contains("documents"))
            && count <= D1NodeServiceTest.MAX_TRIES) {
            Thread.sleep(500);
            count++;
            stream = MNodeService.getInstance(request).query(session, "solr", query);
            resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        }
        assertNotNull(resultStr);
        assertTrue(resultStr.contains("<arr name=\"documents\">"));
        assertTrue(resultStr.contains(guid.getValue()));
        assertTrue(resultStr.contains("<arr name=\"resourceMap\">"));
        assertTrue(resultStr.contains(resourceMapId.getValue()));

        // postquery
        params = new HashMap<>();
        String[] qValue2 = {"id:" + guid2.getValue()};
        params.put("q", qValue2);
        stream = MNodeService.getInstance(request).postQuery(session, "solr", params);
        resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        assertNotNull(resultStr);
        assertTrue(resultStr.contains("<arr name=\"documents\">"));
        assertTrue(resultStr.contains(guid.getValue()));
        assertTrue(resultStr.contains("<arr name=\"resourceMap\">"));
        assertTrue(resultStr.contains(resourceMapId.getValue()));

        // query the resource map object
        query = "q=id:" + resourceMapId.getValue();
        stream = MNodeService.getInstance(request).query(session, "solr", query);
        resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        count = 0;
        while ((resultStr == null || !resultStr.contains("checksum"))
            && count <= D1NodeServiceTest.MAX_TRIES) {
            Thread.sleep(500);
            count++;
            stream = MNodeService.getInstance(request).query(session, "solr", query);
            resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        }

        assertNotNull(resultStr);
        assertTrue(resultStr.contains("<str name=\"id\">" + resourceMapId.getValue() + "</str>"));

        // postquery
        params = new HashMap<>();
        String[] qValue3 = {"id:" + resourceMapId.getValue()};
        params.put("q", qValue3);
        stream = MNodeService.getInstance(request).postQuery(session, "solr", params);
        resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        assertTrue(resultStr.contains("<str name=\"id\">" + resourceMapId.getValue() + "</str>"));


        // =======================================update the package
        // update the metadata object
        Identifier guid4 = new Identifier();
        guid4.setValue("testPackage-metadata-new-version." + System.currentTimeMillis());
        LeanTestUtils.debug(
            "the new version of the metadata object id is ==== " + guid4.getValue());
        InputStream object4 = new FileInputStream(MNodeReplicationTest.replicationSourceFile);
        SystemMetadata sysmeta4 =
            D1NodeServiceTest.createSystemMetadata(guid4, session.getSubject(), object4);
        object4.close();
        sysmeta4.setFormatId(formatId);
        object4 = new FileInputStream(MNodeReplicationTest.replicationSourceFile);
        d1NodeServiceTest.mnUpdate(session, guid2, object4, guid4, sysmeta4);
        // make sure the new metadata object was indexed
        query = "q=id:" + guid4.getValue();
        stream = MNodeService.getInstance(request).query(session, "solr", query);
        resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        count = 0;
        while ((resultStr == null || !resultStr.contains("checksum"))
            && count <= D1NodeServiceTest.MAX_TRIES) {
            Thread.sleep(DEFAULT_SLEEP_MS);
            count++;
            stream = MNodeService.getInstance(request).query(session, "solr", query);
            resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        }

        assertNotNull(resultStr);
        LeanTestUtils.debug("the result str is " + resultStr);
        assertTrue(resultStr.contains("name=\"obsoletes\">" + guid2.getValue()));

        // make sure guid2 was obsoleted
        query = "q=id:" + guid2.getValue();
        stream = MNodeService.getInstance(request).query(session, "solr", query);
        resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        count = 0;
        while (
            (resultStr == null || !resultStr.contains("name=\"obsoletedBy\">" + guid4.getValue()))
                && count <= D1NodeServiceTest.MAX_TRIES) {
            Thread.sleep(DEFAULT_SLEEP_MS);
            count++;
            stream = MNodeService.getInstance(request).query(session, "solr", query);
            resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        }

        assertNotNull(resultStr);
        assertTrue(resultStr.contains("name=\"obsoletedBy\">" + guid4.getValue()));

        // create a new resourcemap with the new metadata object and old data object
        idMap = new HashMap<>();
        dataIds = new ArrayList<>();
        dataIds.add(guid);
        idMap.put(guid4, dataIds);
        Identifier newResourceMapId = new Identifier();
        // use the local id, not the guid in case we have DOIs for them already
        newResourceMapId.setValue("newTestPackage-resourcemap." + System.currentTimeMillis());
        LeanTestUtils.debug("the resource file id is ==== " + newResourceMapId.getValue());
        rm = ResourceMapFactory.getInstance().createResourceMap(newResourceMapId, idMap);
        resourceMapXML = ResourceMapFactory.getInstance().serializeResourceMap(rm);
        InputStream object5 =
            new ByteArrayInputStream(resourceMapXML.getBytes(StandardCharsets.UTF_8));
        SystemMetadata sysmeta5 =
            D1NodeServiceTest.createSystemMetadata(newResourceMapId, session.getSubject(), object5);
        sysmeta5.setFormatId(formatId3);
        object5 = new ByteArrayInputStream(resourceMapXML.getBytes(StandardCharsets.UTF_8));
        d1NodeServiceTest.mnUpdate(session, resourceMapId, object5, newResourceMapId, sysmeta5);
        // make sure the old resource map has the obsoletedBy field.
        query = "q=id:" + resourceMapId.getValue();
        stream = MNodeService.getInstance(request).query(session, "solr", query);
        resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        count = 0;
        while ((resultStr == null || !resultStr.contains(
            "name=\"obsoletedBy\">" + newResourceMapId.getValue()))
            && count <= D1NodeServiceTest.MAX_TRIES) {
            Thread.sleep(DEFAULT_SLEEP_MS);
            count++;
            stream = MNodeService.getInstance(request).query(session, "solr", query);
            resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        }

        assertNotNull(resultStr);
        assertTrue(resultStr.contains("name=\"obsoletedBy\">" + newResourceMapId.getValue()));
        // make sure the new resource map was indexed
        query = "q=id:" + newResourceMapId.getValue();
        stream = MNodeService.getInstance(request).query(session, "solr", query);
        resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        count = 0;
        while ((resultStr == null || !resultStr.contains("checksum"))
            && count <= D1NodeServiceTest.MAX_TRIES) {
            Thread.sleep(DEFAULT_SLEEP_MS);
            count++;
            stream = MNodeService.getInstance(request).query(session, "solr", query);
            resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        }
        assertNotNull(resultStr);
        assertTrue(resultStr.contains("name=\"obsoletes\">" + resourceMapId.getValue()));
        // make sure the new metadata object was reindexed and have the new resource map
        query = "q=id:" + guid4.getValue();
        stream = MNodeService.getInstance(request).query(session, "solr", query);
        resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        count = 0;
        while ((resultStr == null || !resultStr.contains("resourceMap"))
            && count <= D1NodeServiceTest.MAX_TRIES) {
            Thread.sleep(DEFAULT_SLEEP_MS);
            count++;
            stream = MNodeService.getInstance(request).query(session, "solr", query);
            resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        }

        assertNotNull(resultStr);
        assertTrue(resultStr.contains("name=\"obsoletes\">" + guid2.getValue()));
        assertTrue(resultStr.contains("<arr name=\"documents\">"));
        assertTrue(resultStr.contains(guid.getValue()));// the data object id
        assertTrue(resultStr.contains("<arr name=\"resourceMap\">"));
        assertTrue(resultStr.contains(newResourceMapId.getValue()));
        // make sure the data object has been reindexed with the new information
        query = "q=id:" + guid.getValue();
        stream = MNodeService.getInstance(request).query(session, "solr", query);
        resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        count = 0;
        while ((resultStr == null || !resultStr.contains(guid4.getValue()))
            && count <= D1NodeServiceTest.MAX_TRIES) {
            Thread.sleep(DEFAULT_SLEEP_MS);
            count++;
            stream = MNodeService.getInstance(request).query(session, "solr", query);
            resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        }
        assertNotNull(resultStr);
        assertTrue(resultStr.contains("<arr name=\"isDocumentedBy\">"));
        assertTrue(resultStr.contains(guid2.getValue()));
        assertTrue(resultStr.contains(guid4.getValue()));
        assertTrue(resultStr.contains("<arr name=\"resourceMap\">"));
        assertTrue(resultStr.contains(resourceMapId.getValue()));
        assertTrue(resultStr.contains(newResourceMapId.getValue()));
    }

    /***
     * Test the indexing a package (the resource map, metadata and data files) whose metadata
     * object has an SID.
     * @throws Exception
     */
    @Test
    public void testPackageWithSID() throws Exception {
        D1NodeServiceTest.printTestHeader("testPackageWithSID");
        // insert data
        Session session = d1NodeServiceTest.getTestSession();
        Identifier guid = new Identifier();
        HashMap<String, String[]> params = null;
        guid.setValue("testPackage-data." + System.currentTimeMillis());
        LeanTestUtils.debug("the data file id is ==== " + guid.getValue());
        InputStream object = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
        SystemMetadata sysmeta =
            D1NodeServiceTest.createSystemMetadata(guid, session.getSubject(), object);
        object = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
        d1NodeServiceTest.mnCreate(session, guid, object, sysmeta);

        // insert metadata
        Identifier sid = new Identifier();
        sid.setValue("sid-testPackage-metadata." + System.currentTimeMillis());
        Identifier guid2 = new Identifier();
        guid2.setValue("testPackage-metadata." + System.currentTimeMillis());
        LeanTestUtils.debug("the metadata  file id is ==== " + guid2.getValue());
        InputStream object2 = new FileInputStream(MNodeReplicationTest.replicationSourceFile);
        SystemMetadata sysmeta2 =
            D1NodeServiceTest.createSystemMetadata(guid2, session.getSubject(), object2);
        object2.close();
        ObjectFormatIdentifier formatId = new ObjectFormatIdentifier();
        formatId.setValue("eml://ecoinformatics.org/eml-2.0.1");
        sysmeta2.setFormatId(formatId);
        sysmeta2.setSeriesId(sid);
        object2 = new FileInputStream(MNodeReplicationTest.replicationSourceFile);
        d1NodeServiceTest.mnCreate(session, guid2, object2, sysmeta2);

        // Make sure both data and metadata objects have been indexed
        String query = "q=id:" + guid.getValue();
        InputStream stream = MNodeService.getInstance(request).query(session, "solr", query);
        String resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        int count = 0;
        while ((resultStr == null || !resultStr.contains("checksum"))
            && count <= D1NodeServiceTest.MAX_TRIES) {
            Thread.sleep(DEFAULT_SLEEP_MS);
            count++;
            stream = MNodeService.getInstance(request).query(session, "solr", query);
            resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        }
        query = "q=id:" + guid2.getValue();
        stream = MNodeService.getInstance(request).query(session, "solr", query);
        resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        count = 0;
        while ((resultStr == null || !resultStr.contains("checksum"))
            && count <= D1NodeServiceTest.MAX_TRIES) {
            Thread.sleep(DEFAULT_SLEEP_MS);
            count++;
            stream = MNodeService.getInstance(request).query(session, "solr", query);
            resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        }
        Map<Identifier, List<Identifier>> idMap = new HashMap<>();
        List<Identifier> dataIds = new ArrayList<>();
        dataIds.add(guid);
        idMap.put(guid2, dataIds);
        Identifier resourceMapId = new Identifier();
        // use the local id, not the guid in case we have DOIs for them already
        resourceMapId.setValue("testPackage-resourcemap." + System.currentTimeMillis());
        LeanTestUtils.debug("the resource file id is ==== " + resourceMapId.getValue());
        ResourceMap rm = ResourceMapFactory.getInstance().createResourceMap(resourceMapId, idMap);
        String resourceMapXML = ResourceMapFactory.getInstance().serializeResourceMap(rm);
        InputStream object3 =
            new ByteArrayInputStream(resourceMapXML.getBytes(StandardCharsets.UTF_8));
        SystemMetadata sysmeta3 =
            D1NodeServiceTest.createSystemMetadata(resourceMapId, session.getSubject(), object3);
        ObjectFormatIdentifier formatId3 = new ObjectFormatIdentifier();
        formatId3.setValue("http://www.openarchives.org/ore/terms");
        sysmeta3.setFormatId(formatId3);
        object3 = new ByteArrayInputStream(resourceMapXML.getBytes(StandardCharsets.UTF_8));
        d1NodeServiceTest.mnCreate(session, resourceMapId, object3, sysmeta3);

        query = "q=id:" + guid.getValue();
        stream = MNodeService.getInstance(request).query(session, "solr", query);
        resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        count = 0;
        while ((resultStr == null || !resultStr.contains("isDocumentedBy"))
            && count <= D1NodeServiceTest.MAX_TRIES) {
            Thread.sleep(2000);
            count++;
            stream = MNodeService.getInstance(request).query(session, "solr", query);
            resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        }
        assertNotNull(resultStr);
        assertTrue(resultStr.contains("<arr name=\"isDocumentedBy\">"));
        assertTrue(resultStr.contains(guid2.getValue()));
        assertTrue(resultStr.contains("<arr name=\"resourceMap\">"));
        assertTrue(resultStr.contains(resourceMapId.getValue()));
        // postquery
        params = new HashMap<>();
        String[] qValue = {"id:" + guid.getValue()};
        params.put("q", qValue);
        stream = MNodeService.getInstance(request).postQuery(session, "solr", params);
        resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        assertTrue(resultStr.contains("<arr name=\"isDocumentedBy\">"));
        assertTrue(resultStr.contains(guid2.getValue()));
        assertTrue(resultStr.contains("<arr name=\"resourceMap\">"));
        assertTrue(resultStr.contains(resourceMapId.getValue()));

        query = "q=id:" + guid2.getValue();
        stream = MNodeService.getInstance(request).query(session, "solr", query);
        resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        count = 0;
        while ((resultStr == null || !resultStr.contains("documents"))
            && count <= D1NodeServiceTest.MAX_TRIES) {
            Thread.sleep(2000);
            count++;
            stream = MNodeService.getInstance(request).query(session, "solr", query);
            resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        }
        LeanTestUtils.debug("the string is +++++++++++++++++++++++++++++++++++\n" + resultStr);
        assertNotNull(resultStr);
        assertTrue(resultStr.contains("<arr name=\"documents\">"));
        assertTrue(resultStr.contains(guid.getValue()));
        assertTrue(resultStr.contains("<arr name=\"resourceMap\">"));
        assertTrue(resultStr.contains(resourceMapId.getValue()));
        // postquery
        params = new HashMap<>();
        String[] qValue2 = {"id:" + guid2.getValue()};
        params.put("q", qValue2);
        stream = MNodeService.getInstance(request).postQuery(session, "solr", params);
        resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        assertTrue(resultStr.contains("<arr name=\"documents\">"));
        assertTrue(resultStr.contains(guid.getValue()));
        assertTrue(resultStr.contains("<arr name=\"resourceMap\">"));
        assertTrue(resultStr.contains(resourceMapId.getValue()));

        query = "q=id:" + resourceMapId.getValue();
        stream = MNodeService.getInstance(request).query(session, "solr", query);
        resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        count = 0;
        while ((resultStr == null || !resultStr.contains("checksum"))
            && count <= D1NodeServiceTest.MAX_TRIES) {
            Thread.sleep(2000);
            count++;
            stream = MNodeService.getInstance(request).query(session, "solr", query);
            resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        }
        LeanTestUtils.debug("the string is +++++++++++++++++++++++++++++++++++\n" + resultStr);
        assertNotNull(resultStr);
        assertTrue(resultStr.contains("<str name=\"id\">" + resourceMapId.getValue() + "</str>"));
        // postquery
        params = new HashMap<>();
        String[] qValue3 = {"id:" + resourceMapId.getValue()};
        params.put("q", qValue3);
        stream = MNodeService.getInstance(request).postQuery(session, "solr", params);
        resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        assertTrue(resultStr.contains("<str name=\"id\">" + resourceMapId.getValue() + "</str>"));

        // update the metadata object
        Identifier guid4 = new Identifier();
        guid4.setValue("testPackage-metadata." + System.currentTimeMillis());
        LeanTestUtils.debug("the new metadata  file id is ==== " + guid4.getValue());
        InputStream object4 = new FileInputStream(MNodeReplicationTest.replicationSourceFile);
        SystemMetadata sysmeta4 =
            D1NodeServiceTest.createSystemMetadata(guid4, session.getSubject(), object4);
        object4.close();
        sysmeta4.setFormatId(formatId);
        sysmeta4.setSeriesId(sid);
        sysmeta4.setObsoletes(guid2);
        object4 = new FileInputStream(MNodeReplicationTest.replicationSourceFile);
        d1NodeServiceTest.mnUpdate(session, guid2, object4, guid4, sysmeta4);

        // update the resourceMap
        query = "q=id:" + guid4.getValue();
        stream = MNodeService.getInstance(request).query(session, "solr", query);
        resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        count = 0;
        while ((resultStr == null || !resultStr.contains("checksum"))
            && count <= D1NodeServiceTest.MAX_TRIES) {
            Thread.sleep(2000);
            count++;
            stream = MNodeService.getInstance(request).query(session, "solr", query);
            resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        }
        Map<Identifier, List<Identifier>> idMap5 = new HashMap<>();
        idMap5.put(guid4, dataIds);
        Identifier resourceMapId2 = new Identifier();
        // use the local id, not the guid in case we have DOIs for them already
        resourceMapId2.setValue("testPackage-resourcemap." + System.currentTimeMillis());
        LeanTestUtils.debug("the new resource file id is ==== " + resourceMapId2.getValue());
        ResourceMap rm2 =
            ResourceMapFactory.getInstance().createResourceMap(resourceMapId2, idMap5);
        String resourceMapXML2 = ResourceMapFactory.getInstance().serializeResourceMap(rm2);
        InputStream object5 =
            new ByteArrayInputStream(resourceMapXML2.getBytes(StandardCharsets.UTF_8));
        SystemMetadata sysmeta5 =
            D1NodeServiceTest.createSystemMetadata(resourceMapId2, session.getSubject(), object5);
        sysmeta5.setFormatId(formatId3);
        object5 = new ByteArrayInputStream(resourceMapXML2.getBytes(StandardCharsets.UTF_8));
        d1NodeServiceTest.mnUpdate(session, resourceMapId, object5, resourceMapId2, sysmeta5);


        query = "q=id:" + guid.getValue();
        stream = MNodeService.getInstance(request).query(session, "solr", query);
        resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        count = 0;
        while ((resultStr == null || !resultStr.contains(resourceMapId2.getValue()))
            && count <= D1NodeServiceTest.MAX_TRIES) {
            Thread.sleep(DEFAULT_SLEEP_MS);
            count++;
            stream = MNodeService.getInstance(request).query(session, "solr", query);
            resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        }
        assertNotNull(resultStr);
        assertTrue(resultStr.contains("<arr name=\"isDocumentedBy\">"));
        assertTrue(resultStr.contains(guid2.getValue()));
        assertTrue(resultStr.contains(guid4.getValue()));
        assertTrue(resultStr.contains("<arr name=\"resourceMap\">"));
        assertTrue(resultStr.contains(resourceMapId.getValue()));
        assertTrue(resultStr.contains(resourceMapId2.getValue()));
        // postquery
        params = new HashMap<>();
        String[] qValue4 = {"id:" + guid.getValue()};
        params.put("q", qValue4);
        stream = MNodeService.getInstance(request).postQuery(session, "solr", params);
        resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        assertTrue(resultStr.contains("<arr name=\"isDocumentedBy\">"));
        assertTrue(resultStr.contains(guid2.getValue()));
        assertTrue(resultStr.contains(guid4.getValue()));
        assertTrue(resultStr.contains("<arr name=\"resourceMap\">"));
        assertTrue(resultStr.contains(resourceMapId.getValue()));
        assertTrue(resultStr.contains(resourceMapId2.getValue()));

        query = "q=id:" + guid4.getValue();
        stream = MNodeService.getInstance(request).query(session, "solr", query);
        resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        count = 0;
        while ((resultStr == null || !resultStr.contains(resourceMapId2.getValue()))
            && count <= D1NodeServiceTest.MAX_TRIES) {
            Thread.sleep(DEFAULT_SLEEP_MS);
            count++;
            stream = MNodeService.getInstance(request).query(session, "solr", query);
            resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        }
        LeanTestUtils.debug("the string is +++++++++++++++++++++++++++++++++++\n" + resultStr);
        assertNotNull(resultStr);
        assertTrue(resultStr.contains("<arr name=\"documents\">"));
        assertTrue(resultStr.contains(guid.getValue()));
        assertTrue(resultStr.contains("<arr name=\"resourceMap\">"));
        assertTrue(resultStr.contains(resourceMapId2.getValue()));
        // postquery
        params = new HashMap<>();
        String[] qValue5 = {"id:" + guid4.getValue()};
        params.put("q", qValue5);
        stream = MNodeService.getInstance(request).postQuery(session, "solr", params);
        resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        assertTrue(resultStr.contains("<arr name=\"documents\">"));
        assertTrue(resultStr.contains(guid.getValue()));
        assertTrue(resultStr.contains("<arr name=\"resourceMap\">"));
        assertTrue(resultStr.contains(resourceMapId2.getValue()));

        query = "q=id:" + resourceMapId2.getValue();
        stream = MNodeService.getInstance(request).query(session, "solr", query);
        resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        count = 0;
        while ((resultStr == null || !resultStr.contains(
            "<str name=\"id\">" + resourceMapId2.getValue() + "</str>"))
            && count <= D1NodeServiceTest.MAX_TRIES) {
            Thread.sleep(2000);
            count++;
            stream = MNodeService.getInstance(request).query(session, "solr", query);
            resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        }
        LeanTestUtils.debug("the string is +++++++++++++++++++++++++++++++++++\n" + resultStr);
        assertNotNull(resultStr);
        assertTrue(resultStr.contains("<str name=\"id\">" + resourceMapId2.getValue() + "</str>"));
        // postquery
        params = new HashMap<>();
        String[] qValue6 = {"id:" + resourceMapId2.getValue()};
        params.put("q", qValue6);
        stream = MNodeService.getInstance(request).postQuery(session, "solr", params);
        resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        assertTrue(resultStr.contains("<str name=\"id\">" + resourceMapId2.getValue() + "</str>"));
    }


    /**
     * Test if the MN subject can query a private object
     *
     * @throws Exception
     */
    @Test
    public void testQueryAccessControlAgainstPrivateObject() throws Exception {
        D1NodeServiceTest.printTestHeader("testQueryAccessControlAgainstPrivateObject");
        Session session = d1NodeServiceTest.getTestSession();
        Identifier guid = new Identifier();
        HashMap<String, String[]> params = null;
        guid.setValue("testUpdate." + System.currentTimeMillis());
        InputStream object = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
        SystemMetadata sysmeta =
            D1NodeServiceTest.createSystemMetadata(guid, session.getSubject(), object);
        sysmeta.setAccessPolicy(new AccessPolicy());
        object = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
        Identifier pid = d1NodeServiceTest.mnCreate(session, guid, object, sysmeta);
        String query = "q=id:" + guid.getValue();
        InputStream stream = MNodeService.getInstance(request).query(session, "solr", query);
        String resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        int count = 0;
        while ((resultStr == null || !resultStr.contains("checksum"))
            && count <= D1NodeServiceTest.MAX_TRIES) {
            Thread.sleep(DEFAULT_SLEEP_MS);
            count++;
            stream = MNodeService.getInstance(request).query(session, "solr", query);
            resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        }
        assertNotNull(resultStr);
        assertTrue(resultStr.contains("<str name=\"id\">" + guid.getValue() + "</str>"));
        assertTrue(resultStr.contains("<bool name=\"archived\">false</bool>"));
        // postquery
        params = new HashMap<>();
        String[] qValue = {"id:" + guid.getValue()};
        params.put("q", qValue);
        stream = MNodeService.getInstance(request).postQuery(session, "solr", params);
        resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        assertTrue(resultStr.contains("<str name=\"id\">" + guid.getValue() + "</str>"));
        assertTrue(resultStr.contains("<bool name=\"archived\">false</bool>"));

        Session anotherSession = d1NodeServiceTest.getAnotherSession();
        stream = MNodeService.getInstance(request).query(anotherSession, "solr", query);
        resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        LeanTestUtils.debug("the guid is " + guid.getValue());
        LeanTestUtils.debug("the string is +++++++++++++++++++++++++++++++++++\n" + resultStr);
        assertFalse(resultStr.contains("<str name=\"id\">" + guid.getValue() + "</str>"));
        assertFalse(resultStr.contains("<bool name=\"archived\">false</bool>"));
        // postquery
        stream = MNodeService.getInstance(request).postQuery(anotherSession, "solr", params);
        resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        assertFalse(resultStr.contains("<str name=\"id\">" + guid.getValue() + "</str>"));
        assertFalse(resultStr.contains("<bool name=\"archived\">false</bool>"));

        // public session
        Session publicSession = new Session();
        Subject subject = new Subject();
        subject.setValue("public");
        publicSession.setSubject(subject);
        stream = MNodeService.getInstance(request).query(publicSession, "solr", query);
        resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        LeanTestUtils.debug("the guid is " + guid.getValue());
        LeanTestUtils.debug("the string is +++++++++++++++++++++++++++++++++++\n" + resultStr);
        assertFalse(resultStr.contains("<str name=\"id\">" + guid.getValue() + "</str>"));
        assertFalse(resultStr.contains("<bool name=\"archived\">false</bool>"));
        // postquery
        stream = MNodeService.getInstance(request).postQuery(publicSession, "solr", params);
        resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        assertFalse(resultStr.contains("<str name=\"id\">" + guid.getValue() + "</str>"));
        assertFalse(resultStr.contains("<bool name=\"archived\">false</bool>"));

        // null session
        Session nullSession = null;
        stream = MNodeService.getInstance(request).query(nullSession, "solr", query);
        resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        LeanTestUtils.debug("the guid is " + guid.getValue());
        LeanTestUtils.debug("the string is +++++++++++++++++++++++++++++++++++\n" + resultStr);
        assertFalse(resultStr.contains("<str name=\"id\">" + guid.getValue() + "</str>"));
        assertFalse(resultStr.contains("<bool name=\"archived\">false</bool>"));
        // postquery
        stream = MNodeService.getInstance(request).postQuery(nullSession, "solr", params);
        resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        assertFalse(resultStr.contains("<str name=\"id\">" + guid.getValue() + "</str>"));
        assertFalse(resultStr.contains("<bool name=\"archived\">false</bool>"));

        // empty session
        Session emptySession = new Session();
        stream = MNodeService.getInstance(request).query(emptySession, "solr", query);
        resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        LeanTestUtils.debug("the guid is " + guid.getValue());
        LeanTestUtils.debug("the string is +++++++++++++++++++++++++++++++++++\n" + resultStr);
        assertFalse(resultStr.contains("<str name=\"id\">" + guid.getValue() + "</str>"));
        assertFalse(resultStr.contains("<bool name=\"archived\">false</bool>"));
        // postquery
        stream = MNodeService.getInstance(request).postQuery(emptySession, "solr", params);
        resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        assertFalse(resultStr.contains("<str name=\"id\">" + guid.getValue() + "</str>"));
        assertFalse(resultStr.contains("<bool name=\"archived\">false</bool>"));

        // MN session
        Session mnSession = d1NodeServiceTest.getMNSession();
        stream = MNodeService.getInstance(request).query(mnSession, "solr", query);
        resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        LeanTestUtils.debug("the guid is " + guid.getValue());
        LeanTestUtils.debug("the string is +++++++++++++++++++++++++++++++++++\n" + resultStr);
        assertTrue(resultStr.contains("<str name=\"id\">" + guid.getValue() + "</str>"));
        assertTrue(resultStr.contains("<bool name=\"archived\">false</bool>"));
        // postquery
        stream = MNodeService.getInstance(request).postQuery(mnSession, "solr", params);
        resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        assertTrue(resultStr.contains("<str name=\"id\">" + guid.getValue() + "</str>"));
        assertTrue(resultStr.contains("<bool name=\"archived\">false</bool>"));

        // CN session
        Session cnSession = d1NodeServiceTest.getCNSession();
        stream = MNodeService.getInstance(request).query(cnSession, "solr", query);
        resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        LeanTestUtils.debug("the guid is " + guid.getValue());
        LeanTestUtils.debug("the string is +++++++++++++++++++++++++++++++++++\n" + resultStr);
        assertTrue(resultStr.contains("<str name=\"id\">" + guid.getValue() + "</str>"));
        assertTrue(resultStr.contains("<bool name=\"archived\">false</bool>"));
        // postquery
        stream = MNodeService.getInstance(request).postQuery(cnSession, "solr", params);
        resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        assertTrue(resultStr.contains("<str name=\"id\">" + guid.getValue() + "</str>"));
        assertTrue(resultStr.contains("<bool name=\"archived\">false</bool>"));
    }

    /**
     * Test if the MN subject can query a private object
     *
     * @throws Exception
     */
    @Test
    public void testQueryAccessControlAgainstPublicObject() throws Exception {
        D1NodeServiceTest.printTestHeader("testQueryAccessControlAgainstPublicObject");
        Session session = d1NodeServiceTest.getTestSession();
        Identifier guid = new Identifier();
        HashMap<String, String[]> params = null;
        guid.setValue("testUpdate." + System.currentTimeMillis());
        InputStream object = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
        SystemMetadata sysmeta =
            D1NodeServiceTest.createSystemMetadata(guid, session.getSubject(), object);
        object = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
        Identifier pid = d1NodeServiceTest.mnCreate(session, guid, object, sysmeta);
        String query = "q=id:" + guid.getValue();
        InputStream stream = MNodeService.getInstance(request).query(session, "solr", query);
        String resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        int count = 0;
        while ((resultStr == null || !resultStr.contains("checksum"))
            && count <= D1NodeServiceTest.MAX_TRIES) {
            Thread.sleep(DEFAULT_SLEEP_MS);
            count++;
            stream = MNodeService.getInstance(request).query(session, "solr", query);
            resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        }
        assertNotNull(resultStr);
        assertTrue("resultStr should contain: <str name=\"id\">" + guid.getValue() + "</str>",
                   resultStr.contains("<str name=\"id\">" + guid.getValue() + "</str>"));
        assertTrue(resultStr.contains("<bool name=\"archived\">false</bool>"));

        // postquery
        params = new HashMap<>();
        String[] qValue = {"id:" + guid.getValue()};
        params.put("q", qValue);
        stream = MNodeService.getInstance(request).postQuery(session, "solr", params);
        resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);

        assertNotNull(resultStr);
        assertTrue("resultStr should contain: <str name=\"id\">" + guid.getValue() + "</str>",
                   resultStr.contains("<str name=\"id\">" + guid.getValue() + "</str>"));
        assertTrue(resultStr.contains("<bool name=\"archived\">false</bool>"));

        Session anotherSession = d1NodeServiceTest.getAnotherSession();
        stream = MNodeService.getInstance(request).query(anotherSession, "solr", query);
        resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        LeanTestUtils.debug("the guid is " + guid.getValue());
        LeanTestUtils.debug("the string is +++++++++++++++++++++++++++++++++++\n" + resultStr);

        assertNotNull(resultStr);
        assertTrue("resultStr should contain: <str name=\"id\">" + guid.getValue() + "</str>",
                   resultStr.contains("<str name=\"id\">" + guid.getValue() + "</str>"));
        assertTrue(resultStr.contains("<bool name=\"archived\">false</bool>"));
        // postquery
        stream = MNodeService.getInstance(request).postQuery(anotherSession, "solr", params);
        resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        assertTrue(resultStr.contains("<str name=\"id\">" + guid.getValue() + "</str>"));
        assertTrue(resultStr.contains("<bool name=\"archived\">false</bool>"));

        // public session
        Session publicSession = new Session();
        Subject subject = new Subject();
        subject.setValue("public");
        publicSession.setSubject(subject);
        stream = MNodeService.getInstance(request).query(publicSession, "solr", query);
        resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        LeanTestUtils.debug("the guid is " + guid.getValue());
        LeanTestUtils.debug("the string is +++++++++++++++++++++++++++++++++++\n" + resultStr);
        assertTrue(resultStr.contains("<str name=\"id\">" + guid.getValue() + "</str>"));
        assertTrue(resultStr.contains("<bool name=\"archived\">false</bool>"));
        // postquery
        stream = MNodeService.getInstance(request).postQuery(publicSession, "solr", params);
        resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        assertTrue(resultStr.contains("<str name=\"id\">" + guid.getValue() + "</str>"));
        assertTrue(resultStr.contains("<bool name=\"archived\">false</bool>"));

        // null session
        Session nullSession = null;
        stream = MNodeService.getInstance(request).query(nullSession, "solr", query);
        resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        LeanTestUtils.debug("the guid is " + guid.getValue());
        LeanTestUtils.debug("the string is +++++++++++++++++++++++++++++++++++\n" + resultStr);
        assertTrue(resultStr.contains("<str name=\"id\">" + guid.getValue() + "</str>"));
        assertTrue(resultStr.contains("<bool name=\"archived\">false</bool>"));
        // postquery
        stream = MNodeService.getInstance(request).postQuery(nullSession, "solr", params);
        resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        assertTrue(resultStr.contains("<str name=\"id\">" + guid.getValue() + "</str>"));
        assertTrue(resultStr.contains("<bool name=\"archived\">false</bool>"));

        // empty session
        Session emptySession = new Session();
        stream = MNodeService.getInstance(request).query(emptySession, "solr", query);
        resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        LeanTestUtils.debug("the guid is " + guid.getValue());
        LeanTestUtils.debug("the string is +++++++++++++++++++++++++++++++++++\n" + resultStr);
        assertTrue(resultStr.contains("<str name=\"id\">" + guid.getValue() + "</str>"));
        assertTrue(resultStr.contains("<bool name=\"archived\">false</bool>"));
        // postquery
        stream = MNodeService.getInstance(request).postQuery(emptySession, "solr", params);
        resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        assertTrue(resultStr.contains("<str name=\"id\">" + guid.getValue() + "</str>"));
        assertTrue(resultStr.contains("<bool name=\"archived\">false</bool>"));

        // MN session
        Session mnSession = d1NodeServiceTest.getMNSession();
        stream = MNodeService.getInstance(request).query(mnSession, "solr", query);
        resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        LeanTestUtils.debug("the guid is " + guid.getValue());
        LeanTestUtils.debug("the string is +++++++++++++++++++++++++++++++++++\n" + resultStr);
        assertTrue(resultStr.contains("<str name=\"id\">" + guid.getValue() + "</str>"));
        assertTrue(resultStr.contains("<bool name=\"archived\">false</bool>"));
        // postquery
        stream = MNodeService.getInstance(request).postQuery(mnSession, "solr", params);
        resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        assertTrue(resultStr.contains("<str name=\"id\">" + guid.getValue() + "</str>"));
        assertTrue(resultStr.contains("<bool name=\"archived\">false</bool>"));

        // CN session
        Session cnSession = d1NodeServiceTest.getCNSession();
        stream = MNodeService.getInstance(request).query(cnSession, "solr", query);
        resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        LeanTestUtils.debug("the guid is " + guid.getValue());
        LeanTestUtils.debug("the string is +++++++++++++++++++++++++++++++++++\n" + resultStr);
        assertTrue(resultStr.contains("<str name=\"id\">" + guid.getValue() + "</str>"));
        assertTrue(resultStr.contains("<bool name=\"archived\">false</bool>"));
        // postquery
        stream = MNodeService.getInstance(request).postQuery(cnSession, "solr", params);
        resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        assertTrue(resultStr.contains("<str name=\"id\">" + guid.getValue() + "</str>"));
        assertTrue(resultStr.contains("<bool name=\"archived\">false</bool>"));
    }


    @Test
    public void testQueryEMLTaxonomy() throws Exception {
        D1NodeServiceTest.printTestHeader("testQueryEMLTaxonomy");
        Session session = d1NodeServiceTest.getTestSession();
        Identifier guid = new Identifier();
        HashMap<String, String[]> params = null;
        guid.setValue("testUpdate." + System.currentTimeMillis());
        InputStream object = new FileInputStream(taxonomyFilePath);
        SystemMetadata sysmeta =
            D1NodeServiceTest.createSystemMetadata(guid, session.getSubject(), object);
        ObjectFormatIdentifier formatId = new ObjectFormatIdentifier();
        formatId.setValue("eml://ecoinformatics.org/eml-2.1.0");
        sysmeta.setFormatId(formatId);
        object.close();
        object = new FileInputStream(taxonomyFilePath);
        Identifier pid = d1NodeServiceTest.mnCreate(session, guid, object, sysmeta);
        String query = "q=id:" + guid.getValue();
        InputStream stream = MNodeService.getInstance(request).query(session, "solr", query);
        String resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        int count = 0;
        while ((resultStr == null || !resultStr.contains("checksum"))
            && count <= D1NodeServiceTest.MAX_TRIES) {
            Thread.sleep(DEFAULT_SLEEP_MS);
            count++;
            stream = MNodeService.getInstance(request).query(session, "solr", query);
            resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        }
        assertNotNull(resultStr);
        resultStr = resultStr.replaceAll("\\s", "");
        assertTrue(resultStr.contains("<strname=\"id\">" + guid.getValue() + "</str>"));
        assertTrue(resultStr.contains(
            "<arrname=\"genus\"><str>Sarracenia</str><str>sarracenia</str></arr>"));
        assertTrue(
            resultStr.contains("<arrname=\"family\"><str>Family</str><str>family</str></arr>"));
        assertTrue(resultStr.contains(
            "<arrname=\"species\"><str>Purpurea</str><str>purpurea</str></arr>"));
        assertTrue(
            resultStr.contains("<arrname=\"kingdom\"><str>Animal</str><str>animal</str></arr>"));
        assertTrue(resultStr.contains("<arrname=\"order\"><str>Order</str><str>order</str></arr>"));
        assertTrue(
            resultStr.contains("<arrname=\"phylum\"><str>Phylum</str><str>phylum</str></arr>"));
        assertTrue(resultStr.contains("<arrname=\"class\"><str>Class</str><str>class</str></arr>"));

        // post query
        params = new HashMap<>();
        String[] qValue = {"id:" + guid.getValue()};
        params.put("q", qValue);
        stream = MNodeService.getInstance(request).postQuery(session, "solr", params);
        resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        resultStr = resultStr.replaceAll("\\s", "");
        assertTrue(resultStr.contains("<strname=\"id\">" + guid.getValue() + "</str>"));
        assertTrue(resultStr.contains(
            "<arrname=\"genus\"><str>Sarracenia</str><str>sarracenia</str></arr>"));
        assertTrue(
            resultStr.contains("<arrname=\"family\"><str>Family</str><str>family</str></arr>"));
        assertTrue(resultStr.contains(
            "<arrname=\"species\"><str>Purpurea</str><str>purpurea</str></arr>"));
        assertTrue(
            resultStr.contains("<arrname=\"kingdom\"><str>Animal</str><str>animal</str></arr>"));
        assertTrue(resultStr.contains("<arrname=\"order\"><str>Order</str><str>order</str></arr>"));
        assertTrue(
            resultStr.contains("<arrname=\"phylum\"><str>Phylum</str><str>phylum</str></arr>"));
        assertTrue(resultStr.contains("<arrname=\"class\"><str>Class</str><str>class</str></arr>"));
    }

    @Test
    public void testISO211() throws Exception {
        D1NodeServiceTest.printTestHeader("testISO211");
        Session session = d1NodeServiceTest.getTestSession();
        Identifier guid = new Identifier();
        guid.setValue("testPangaea." + System.currentTimeMillis());
        InputStream object = new FileInputStream("test/pangaea.xml");
        InputStream sysmetaInput = new FileInputStream("test/sysmeta-pangaea.xml");
        SystemMetadata sysmeta =
            TypeMarshaller.unmarshalTypeFromStream(SystemMetadata.class, sysmetaInput);
        Subject rightsHolder = session.getSubject();
        sysmeta.setRightsHolder(rightsHolder);
        sysmeta.setIdentifier(guid);
        sysmeta.setAuthoritativeMemberNode(
            MNodeService.getInstance(request).getCapabilities().getIdentifier());
        object = new FileInputStream("test/pangaea.xml");
        Identifier pid = d1NodeServiceTest.mnCreate(session, guid, object, sysmeta);
        assertEquals(pid.getValue(), guid.getValue());
        String query = "q=id:" + guid.getValue();
        InputStream stream = MNodeService.getInstance(request).query(session, "solr", query);
        String resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        int count = 0;
        while ((resultStr == null || !resultStr.contains("checksum"))
            && count <= D1NodeServiceTest.MAX_TRIES) {
            Thread.sleep(DEFAULT_SLEEP_MS);
            count++;
            stream = MNodeService.getInstance(request).query(session, "solr", query);
            resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        }
        assertNotNull(resultStr);
        assertTrue(resultStr.contains("<str name=\"pubDate\">2017-07-26T10:15:22Z</str>"));
        assertTrue(resultStr.contains(
            "<str name=\"formatId\">http://www.isotc211.org/2005/gmd-pangaea</str>"));
    }

    /**
     * Test to insert a portal document
     *
     * @throws Exception
     */
    @Test
    public void testPortalDocument() throws Exception {
        D1NodeServiceTest.printTestHeader("testPortalDocument");
        Session session = d1NodeServiceTest.getTestSession();
        Identifier guid = new Identifier();
        guid.setValue("testPortal." + System.currentTimeMillis());
        InputStream object = new FileInputStream(portalFilePath);
        SystemMetadata sysmeta =
            D1NodeServiceTest.createSystemMetadata(guid, session.getSubject(), object);
        ObjectFormatIdentifier formatId = new ObjectFormatIdentifier();
        formatId.setValue("https://purl.dataone.org/portals-1.0.0");
        sysmeta.setFormatId(formatId);
        LeanTestUtils.debug("the checksum is " + sysmeta.getChecksum().getValue());
        Identifier sid = new Identifier();
        sid.setValue("testPortal-sid" + System.currentTimeMillis());
        sysmeta.setSeriesId(sid);
        object.close();
        InputStream object2 = new FileInputStream(portalFilePath);
        LeanTestUtils.debug("before insert the object +++++++++++++++++++++ " + guid.getValue());
        if (QuotaServiceManager.getInstance().isEnabled()) {
            try {
                Identifier pid =
                    d1NodeServiceTest.mnCreate(session, guid, object2, sysmeta);
                fail("We shouldn't get there since the test session doesn't have a portal quota.");
            } catch (Exception e) {
                LeanTestUtils.debug("the error is " + e.getMessage());
                e.printStackTrace();
            }
        }

        session = new Session();
        Subject subject = new Subject();
        subject.setValue(QuotaServiceManagerIT.REQUESTOR);
        session.setSubject(subject);
        if (QuotaServiceManager.getInstance().isEnabled()) {
            try {
                object2 = new FileInputStream(portalFilePath);
                Identifier pid =
                    d1NodeServiceTest.mnCreate(session, guid, object2, sysmeta);
                fail("We shouldn't get there since the quota subject header hasn't been set.");
            } catch (Exception e) {
                LeanTestUtils.debug("the error is " + e.getMessage());
                e.printStackTrace();
            }
        }

        try {
            object2 = new FileInputStream(portalFilePath);
            request.setHeader(
                QuotaServiceManager.QUOTASUBJECTHEADER, QuotaServiceManagerIT.SUBSCRIBER);
            Identifier pid =
                d1NodeServiceTest.mnCreate(session, guid, object2, sysmeta);
        } catch (Exception e) {
            LeanTestUtils.debug("the error is " + e.getMessage());
            e.printStackTrace();
        }

        LeanTestUtils.debug("After insert the object =========================");
        String query = "q=id:" + guid.getValue();
        InputStream stream = MNodeService.getInstance(request).query(session, "solr", query);
        String resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        int count = 0;
        while ((resultStr == null || !resultStr.contains("checksum"))
            && count <= D1NodeServiceTest.MAX_TRIES) {
            Thread.sleep(DEFAULT_SLEEP_MS);
            count++;
            stream = MNodeService.getInstance(request).query(session, "solr", query);
            resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        }
        assertNotNull(resultStr);
        LeanTestUtils.debug(resultStr);
        assertTrue(resultStr.contains("<str name=\"label\">my-portal</str>"));
        assertTrue(resultStr.contains(
            "<str name=\"logo\">urn:uuid:349aa330-4645-4dab-a02d-3bf950cf708d</str>"));
        assertTrue(resultStr.contains(collectionResult));
    }


    /**
     * Test upload/query a package with the hasPart/isPartOf relationship
     */
    @Test
    public void testPackageWithParts() throws Exception {
        D1NodeServiceTest.printTestHeader("testPackageWithParts");
        String uuid_prefix = "urn:uuid:";
        UUID uuid = UUID.randomUUID();
        // insert a portal object with series id
        Session session = new Session();
        Subject subject1 = new Subject();
        subject1.setValue(QuotaServiceManagerIT.REQUESTOR);
        session.setSubject(subject1);
        Identifier guid = new Identifier();
        guid.setValue(uuid_prefix + uuid);
        LeanTestUtils.debug("the collection file id is ==== " + guid.getValue());
        InputStream object = new FileInputStream(portalFilePath);
        Identifier seriesId = new Identifier();
        uuid = UUID.randomUUID();
        seriesId.setValue(uuid_prefix + uuid);
        SystemMetadata sysmeta =
            D1NodeServiceTest.createSystemMetadata(guid, session.getSubject(), object);
        object.close();
        InputStream object8 = new FileInputStream(portalFilePath);
        sysmeta.setSeriesId(seriesId);
        ObjectFormatIdentifier formatId4 = new ObjectFormatIdentifier();
        formatId4.setValue("https://purl.dataone.org/portals-1.0.0");
        sysmeta.setFormatId(formatId4);
        request.setHeader(
            QuotaServiceManager.QUOTASUBJECTHEADER, QuotaServiceManagerIT.SUBSCRIBER);
        d1NodeServiceTest.mnCreate(session, guid, object8, sysmeta);
        object8.close();

        // insert a metadata object
        Identifier guid2 = new Identifier();
        uuid = UUID.randomUUID();
        guid2.setValue(uuid_prefix + uuid);
        LeanTestUtils.debug("the metadata  file id is ==== " + guid2.getValue());
        InputStream object2 = new FileInputStream(MNodeReplicationTest.replicationSourceFile);
        SystemMetadata sysmeta2 =
            D1NodeServiceTest.createSystemMetadata(guid2, session.getSubject(), object2);
        object2.close();
        ObjectFormatIdentifier formatId = new ObjectFormatIdentifier();
        formatId.setValue("eml://ecoinformatics.org/eml-2.0.1");
        sysmeta2.setFormatId(formatId);
        object2 = new FileInputStream(MNodeReplicationTest.replicationSourceFile);
        d1NodeServiceTest.mnCreate(session, guid2, object2, sysmeta2);

        // insert another metadata object
        Identifier guid3 = new Identifier();
        uuid = UUID.randomUUID();
        guid3.setValue(uuid_prefix + uuid);
        LeanTestUtils.debug("the second metadata  file id is ==== " + guid3.getValue());
        InputStream object5 = new FileInputStream("test/eml-2.2.0.xml");
        SystemMetadata sysmeta5 =
            D1NodeServiceTest.createSystemMetadata(guid3, session.getSubject(), object5);
        object5.close();
        ObjectFormatIdentifier formatId5 = new ObjectFormatIdentifier();
        formatId5.setValue("https://eml.ecoinformatics.org/eml-2.2.0");
        sysmeta5.setFormatId(formatId5);
        object5 = new FileInputStream("test/eml-2.2.0.xml");
        d1NodeServiceTest.mnCreate(session, guid3, object5, sysmeta5);


        // Make sure both portal and metadata objects have been indexed
        String query = "q=id:" + "\"" + guid.getValue() + "\"";
        InputStream stream = MNodeService.getInstance(request).query(session, "solr", query);
        String resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        int count = 0;
        while ((resultStr == null || !resultStr.contains("checksum"))
            && count <= D1NodeServiceTest.MAX_TRIES) {
            Thread.sleep(DEFAULT_SLEEP_MS);
            count++;
            stream = MNodeService.getInstance(request).query(session, "solr", query);
            resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        }
        query = "q=id:" + "\"" + guid2.getValue() + "\"";
        stream = MNodeService.getInstance(request).query(session, "solr", query);
        resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        count = 0;
        while ((resultStr == null || !resultStr.contains("checksum"))
            && count <= D1NodeServiceTest.MAX_TRIES) {
            Thread.sleep(DEFAULT_SLEEP_MS);
            count++;
            stream = MNodeService.getInstance(request).query(session, "solr", query);
            resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        }
        query = "q=id:" + "\"" + guid3.getValue() + "\"";
        stream = MNodeService.getInstance(request).query(session, "solr", query);
        resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        count = 0;
        while ((resultStr == null || !resultStr.contains("checksum"))
            && count <= D1NodeServiceTest.MAX_TRIES) {
            Thread.sleep(DEFAULT_SLEEP_MS);
            count++;
            stream = MNodeService.getInstance(request).query(session, "solr", query);
            resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        }

        // generate the resource map with the documents/documentedBy and isPartOf/hasPart
        // relationships.
        Identifier resourceMapId = new Identifier();
        uuid = UUID.randomUUID();
        resourceMapId.setValue("testPackageWithParts_resourceMap_" + uuid_prefix + uuid);
        Subject subject = new Subject();
        subject.setValue("Jhon Smith");
        Model model = ModelFactory.createDefaultModel();
        // create a resourceMap resource
        Resource resourceMap =
            ResourceMapModifier.generateNewOREResource(model, subject, resourceMapId);
        // create an aggregation resource
        Resource aggregation =
            ResourceFactory.createResource(resourceMap.getURI() + "#aggregation");
        // create a collection object resource
        Resource collection = ResourceMapModifier.generateNewComponent(model,
                                                                       seriesId.getValue());// it
        // only works with a
        // series id
        // create a metadata object resource
        Resource metadata = ResourceMapModifier.generateNewComponent(model, guid2.getValue());
        // create the second metadata object resource
        Resource metadata2 = ResourceMapModifier.generateNewComponent(model, guid3.getValue());
        // add relationships to the model
        Property predicate =
            ResourceFactory.createProperty(ResourceMapModifier.ORE_TER_NAMESPACE, "isDescribedBy");
        model.add(model.createStatement(aggregation, predicate, resourceMap));
        predicate =
            ResourceFactory.createProperty(ResourceMapModifier.ORE_TER_NAMESPACE, "aggregates");
        model.add(model.createStatement(aggregation, predicate, metadata));
        model.add(model.createStatement(aggregation, predicate, metadata2));
        predicate =
            ResourceFactory.createProperty(ResourceMapModifier.ORE_TER_NAMESPACE, "isAggregatedBy");
        model.add(model.createStatement(metadata, predicate, aggregation));
        model.add(model.createStatement(metadata2, predicate, aggregation));
        model.add(model.createStatement(metadata, CITO.isDocumentedBy, metadata));
        model.add(model.createStatement(metadata, CITO.documents, metadata));
        model.add(model.createStatement(metadata2, CITO.isDocumentedBy, metadata2));
        model.add(model.createStatement(metadata2, CITO.documents, metadata2));
        predicate = ResourceFactory.createProperty("https://schema.org/", "isPartOf");
        model.add(model.createStatement(metadata, predicate, collection));
        model.add(model.createStatement(metadata2, predicate, collection));
        predicate = ResourceFactory.createProperty("https://schema.org/", "hasPart");
        model.add(model.createStatement(collection, predicate, metadata));
        model.add(model.createStatement(collection, predicate, metadata2));
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        model.write(System.out);
        model.write(output);
        InputStream object3 = new ByteArrayInputStream(output.toByteArray());
        SystemMetadata sysmeta3 =
            D1NodeServiceTest.createSystemMetadata(resourceMapId, session.getSubject(), object3);
        ObjectFormatIdentifier formatId3 = new ObjectFormatIdentifier();
        formatId3.setValue("http://www.openarchives.org/ore/terms");
        sysmeta3.setFormatId(formatId3);
        object3 = new ByteArrayInputStream(output.toByteArray());
        d1NodeServiceTest.mnCreate(session, resourceMapId, object3, sysmeta3);

        query = "q=id:" + "\"" + guid.getValue() + "\"";
        stream = MNodeService.getInstance(request).query(session, "solr", query);
        resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        count = 0;
        while ((resultStr == null || !resultStr.contains("hasPart"))
            && count <= D1NodeServiceTest.MAX_TRIES) {
            Thread.sleep(DEFAULT_SLEEP_MS);
            count++;
            stream = MNodeService.getInstance(request).query(session, "solr", query);
            resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        }
        assertNotNull(resultStr);
        assertTrue(resultStr.contains("<str name=\"label\">my-portal</str>"));
        assertTrue(resultStr.contains(
            "<str name=\"logo\">urn:uuid:349aa330-4645-4dab-a02d-3bf950cf708d</str>"));
        assertTrue(resultStr.contains(collectionResult));
        resultStr = resultStr.replaceAll("\\s", "");
        assertTrue(resultStr.contains(
            "<arrname=\"hasPart\"><str>" + guid2.getValue() + "</str><str>" + guid3.getValue()
                + "</str></arr>") || resultStr.contains(
            "<arrname=\"hasPart\"><str>" + guid3.getValue() + "</str><str>" + guid2.getValue()
                + "</str></arr>"));

        query = "q=id:" + "\"" + guid2.getValue() + "\"";
        stream = MNodeService.getInstance(request).query(session, "solr", query);
        resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        count = 0;
        while ((resultStr == null || !resultStr.contains("documents"))
            && count <= D1NodeServiceTest.MAX_TRIES) {
            Thread.sleep(DEFAULT_SLEEP_MS);
            count++;
            stream = MNodeService.getInstance(request).query(session, "solr", query);
            resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        }
        LeanTestUtils.debug(resultStr);
        assertNotNull(resultStr);
        assertTrue(resultStr.contains("<arr name=\"documents\">"));
        assertTrue(resultStr.contains("<arr name=\"isDocumentedBy\">"));
        assertTrue(resultStr.contains("<arr name=\"resourceMap\">"));
        assertTrue(resultStr.contains(resourceMapId.getValue()));
        resultStr = resultStr.replaceAll("\\s", "");
        assertTrue(resultStr.contains(
            "<arrname=\"isPartOf\"><str>" + seriesId.getValue() + "</str></arr>"));

        query = "q=id:" + "\"" + guid3.getValue() + "\"";
        stream = MNodeService.getInstance(request).query(session, "solr", query);
        resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        count = 0;
        while ((resultStr == null || !resultStr.contains("documents"))
            && count <= D1NodeServiceTest.MAX_TRIES) {
            Thread.sleep(DEFAULT_SLEEP_MS);
            count++;
            stream = MNodeService.getInstance(request).query(session, "solr", query);
            resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        }
        LeanTestUtils.debug(resultStr);
        assertNotNull(resultStr);
        assertTrue(resultStr.contains("<arr name=\"documents\">"));
        assertTrue(resultStr.contains("<arr name=\"isDocumentedBy\">"));
        assertTrue(resultStr.contains("<arr name=\"resourceMap\">"));
        assertTrue(resultStr.contains(resourceMapId.getValue()));
        resultStr = resultStr.replaceAll("\\s", "");
        assertTrue(resultStr.contains(
            "<arrname=\"isPartOf\"><str>" + seriesId.getValue() + "</str></arr>"));
    }

    /**
     * Test to post a long query
     *
     * @throws Exception
     */
    @Test
    public void testPostLongQuery() throws Exception {
        D1NodeServiceTest.printTestHeader("testPostLongQuery");

        String uuid_prefix = "urn:uuid:";
        Session session = d1NodeServiceTest.getTestSession();
        Identifier guid3 = new Identifier();
        UUID uuid = UUID.randomUUID();
        guid3.setValue(uuid_prefix + uuid);
        LeanTestUtils.debug("the new metadata object id is ==== " + guid3.getValue());
        InputStream object5 = new FileInputStream("test/eml-2.2.0.xml");
        SystemMetadata sysmeta5 =
            D1NodeServiceTest.createSystemMetadata(guid3, session.getSubject(), object5);
        object5.close();
        ObjectFormatIdentifier formatId5 = new ObjectFormatIdentifier();
        formatId5.setValue("https://eml.ecoinformatics.org/eml-2.2.0");
        sysmeta5.setFormatId(formatId5);
        object5 = new FileInputStream("test/eml-2.2.0.xml");
        d1NodeServiceTest.mnCreate(session, guid3, object5, sysmeta5);

        String newId = guid3.getValue().replaceAll(":", "\\\\:");
        String queryWithExtraSlash = "(id:(" + "\"" + newId + "\" OR "
            + "\"urn\\:uuid\\:0163b16e-4718-4e6c-89b4-42f6eb30c6cf\" OR "
            + "\"urn\\:uuid\\:056be5dc-cbde-4a4d-9540-92e495b755d2\") OR seriesId:"
            + "(\"urn\\:uuid\\:0163b16e-4718-4e6c-89b4-42f6eb30c6cf\" OR "
            + "\"urn\\:uuid\\:056be5dc-cbde-4a4d-9540-92e495b755d2\")) AND (formatType:METADATA "
            + "OR formatType:DATA) AND -obsoletedBy:*";
        LeanTestUtils.debug("The query with the extral slash on the urn is " + queryWithExtraSlash);
        String query2 = "(id:(" + "\"" + guid3.getValue() + "\" OR "
            + "\"urn:uuid:0163b16e-4718-4e6c-89b4-42f6eb30c6cf\" OR "
            + "\"urn:uuid:056be5dc-cbde-4a4d-9540-92e495b755d2\") OR seriesId:"
            + "(\"urn:uuid:0163b16e-4718-4e6c-89b4-42f6eb30c6cf\" OR "
            + "\"urn:uuid:056be5dc-cbde-4a4d-9540-92e495b755d2\")) AND (formatType:METADATA OR "
            + "formatType:DATA) AND -obsoletedBy:*";
        LeanTestUtils.debug("The query without extra slash is " + query2);

        DefaultHttpMultipartRestClient multipartRestClient = new DefaultHttpMultipartRestClient();
        String server = SystemUtil.getContextURL();
        SimpleMultipartEntity params = new SimpleMultipartEntity();
        params.addParamPart("q", queryWithExtraSlash);
        InputStream stream =
            multipartRestClient.doPostRequest(server + "/d1/mn/v2/query/solr", params, 30000);
        String resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        int count = 0;
        while ((resultStr == null || !resultStr.contains("checksum"))
            && count <= D1NodeServiceTest.MAX_TRIES) {
            Thread.sleep(DEFAULT_SLEEP_MS);
            count++;
            stream =
                multipartRestClient.doPostRequest(server + "/d1/mn/v2/query/solr", params, 30000);
            resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        }
        assertNotNull(resultStr);
        assertTrue(
            resultStr.contains("<str name=\"checksum\">f4ea2d07db950873462a064937197b0f</str>"));

        // query without the extra slash
        SimpleMultipartEntity params2 = new SimpleMultipartEntity();
        params2.addParamPart("q", query2);
        stream = multipartRestClient.doPostRequest(server + "/d1/mn/v2/query/solr", params2, 30000);
        resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        assertTrue(
            resultStr.contains("<str name=\"checksum\">f4ea2d07db950873462a064937197b0f</str>"));

        // post a long query
        File queryFile = new File(longQueryFile);
        String longPartialQuery = FileUtils.readFileToString(queryFile, (Charset) null);
        String longQuery = "(id:(" + "\"" + newId + "\" OR " + longPartialQuery;
        LeanTestUtils.debug("The long query is " + longQuery);
        SimpleMultipartEntity params3 = new SimpleMultipartEntity();
        params3.addParamPart("q", longQuery);
        stream = multipartRestClient.doPostRequest(server + "/d1/mn/v2/query/solr", params3, 30000);
        resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        assertTrue(
            resultStr.contains("<str name=\"checksum\">f4ea2d07db950873462a064937197b0f</str>"));
    }

    /**
     * Test any query it has the encoded Chinese characters.
     */
    @Test
    public void testChineseCharacters() throws Exception {
        D1NodeServiceTest.printTestHeader("testChineseCharacters");

        String server = SystemUtil.getContextURL();
        String query = "q=%20-obsoletedBy:*%20AND%20%20-formatId:*dataone"
            + ".org%2Fcollections*%20AND%20%20-formatId:*dataone"
            + ".org%2Fportals*%20AND%20%E4%B8%AD%E6%96%87%20AND%20formatType:METADATA%20AND"
            + "%20endDate:[1800-01-01T00:00:00Z%20TO%202020-01-24T21:56:15"
            + ".422Z]%20AND%20-obsoletedBy:*&rows=1&fl=endDate&sort=endDate%20desc&wt=json";
        DefaultHttpMultipartRestClient multipartRestClient = new DefaultHttpMultipartRestClient();
        try (InputStream response = multipartRestClient.doGetRequest(
            server + "/d1/mn/v2/query/solr/?" + query, 1000)) {
            assertNotNull(response);
        } catch (Exception e) {
            e.printStackTrace();
            fail("unexpected exception making query: " + server + "/d1/mn/v2/query/solr/?" + query);
        }
    }

    /**
     * @throws Exception
     */
    @Test
    public void testAccess() throws Exception {
        D1NodeServiceTest.printTestHeader("testAccess");
        String rightsHolder = "rightsHolder";
        Subject rightsHolderSubject = new Subject();
        rightsHolderSubject.setValue(rightsHolder);
        Session rightsHolderSession = new Session();
        rightsHolderSession.setSubject(rightsHolderSubject);

        String hasPermission = "hasPermission";
        Subject hasPermissionSubject = new Subject();
        hasPermissionSubject.setValue(hasPermission);
        Session hasPermissionSession = new Session();
        hasPermissionSession.setSubject(hasPermissionSubject);

        String noPermission = "noPermission";
        Subject noPermissionSubject = new Subject();
        noPermissionSubject.setValue(noPermission);
        Session noPermissionSession = new Session();
        noPermissionSession.setSubject(noPermissionSubject);

        String publicUser = "public";
        Subject publicUserSubject = new Subject();
        publicUserSubject.setValue(publicUser);
        Session publicUserSession = new Session();
        publicUserSession.setSubject(publicUserSubject);

        // a public readable document can be read by any user.
        Session session = d1NodeServiceTest.getTestSession();
        Identifier guid = new Identifier();
        guid.setValue("testAccess." + System.currentTimeMillis());
        InputStream object = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
        SystemMetadata sysmeta =
            D1NodeServiceTest.createSystemMetadata(guid, session.getSubject(), object);
        object = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));

        d1NodeServiceTest.mnCreate(session, guid, object, sysmeta);

        String query = "q=id:" + guid.getValue();
        InputStream stream = MNodeService.getInstance(request).query(session, "solr", query);
        String resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        int count = 0;
        while ((resultStr == null || !resultStr.contains("checksum"))
            && count <= D1NodeServiceTest.MAX_TRIES) {
            Thread.sleep(DEFAULT_SLEEP_MS);
            count++;
            stream = MNodeService.getInstance(request).query(session, "solr", query);
            resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);

        }

        assertNotNull(resultStr);
        assertTrue(resultStr.contains("checksum"));

        stream = MNodeService.getInstance(request).query(rightsHolderSession, "solr", query);
        resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        assertTrue(resultStr.contains("checksum"));

        stream = MNodeService.getInstance(request).query(hasPermissionSession, "solr", query);
        resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        assertTrue(resultStr.contains("checksum"));

        stream = MNodeService.getInstance(request).query(noPermissionSession, "solr", query);
        resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        assertTrue(resultStr.contains("checksum"));

        stream = MNodeService.getInstance(request).query(publicUserSession, "solr", query);
        resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        assertTrue(resultStr.contains("checksum"));

        // a document without access rules
        guid = new Identifier();
        guid.setValue("testAccess0." + System.currentTimeMillis());
        object = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
        sysmeta = D1NodeServiceTest.createSystemMetadata(guid, rightsHolderSubject, object);
        sysmeta.setAccessPolicy(null);
        object = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));

        d1NodeServiceTest.mnCreate(session, guid, object, sysmeta);

        query = "q=id:" + guid.getValue();
        stream = MNodeService.getInstance(request).query(rightsHolderSession, "solr", query);
        resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        count = 0;
        while ((resultStr == null || !resultStr.contains("checksum"))
            && count <= D1NodeServiceTest.MAX_TRIES) {
            Thread.sleep(DEFAULT_SLEEP_MS);
            count++;
            stream = MNodeService.getInstance(request).query(rightsHolderSession, "solr", query);
            resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        }

        assertNotNull(resultStr);
        assertTrue(resultStr.contains("checksum"));
        stream = MNodeService.getInstance(request).query(hasPermissionSession, "solr", query);
        resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);

        count = 0;
        while ((resultStr == null || !resultStr.contains("checksum"))
            && count <= D1NodeServiceTest.MAX_TRIES) {
            Thread.sleep(DEFAULT_SLEEP_MS);
            count++;
            stream = MNodeService.getInstance(request).query(hasPermissionSession, "solr", query);
            resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        }

        assertNotNull(resultStr);
        assertFalse(resultStr.contains("checksum"));

        stream = MNodeService.getInstance(request).query(noPermissionSession, "solr", query);
        resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        assertFalse(resultStr.contains("checksum"));

        stream = MNodeService.getInstance(request).query(publicUserSession, "solr", query);
        resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        assertFalse(resultStr.contains("checksum"));

        // a document with the access rules that hasPermission can write it
        guid = new Identifier();
        guid.setValue("testAccess2." + System.currentTimeMillis());
        object = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
        sysmeta = D1NodeServiceTest.createSystemMetadata(guid, rightsHolderSubject, object);
        AccessPolicy accessPolicy = new AccessPolicy();
        AccessRule allow = new AccessRule();
        allow.addPermission(Permission.WRITE);
        allow.addSubject(hasPermissionSubject);
        accessPolicy.addAllow(allow);
        sysmeta.setAccessPolicy(accessPolicy);
        object = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
        d1NodeServiceTest.mnCreate(session, guid, object, sysmeta);
        query = "q=id:" + guid.getValue();
        stream = MNodeService.getInstance(request).query(rightsHolderSession, "solr", query);
        resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        count = 0;
        while ((resultStr == null || !resultStr.contains("checksum"))
            && count <= D1NodeServiceTest.MAX_TRIES) {
            Thread.sleep(DEFAULT_SLEEP_MS);
            count++;
            stream = MNodeService.getInstance(request).query(rightsHolderSession, "solr", query);
            resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        }

        assertNotNull(resultStr);
        assertTrue(resultStr.contains("checksum"));

        stream = MNodeService.getInstance(request).query(session, "solr", query);
        resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        assertFalse(resultStr.contains("checksum"));

        stream = MNodeService.getInstance(request).query(hasPermissionSession, "solr", query);
        resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        assertTrue(resultStr.contains("checksum"));

        stream = MNodeService.getInstance(request).query(noPermissionSession, "solr", query);
        resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        assertFalse(resultStr.contains("checksum"));

        stream = MNodeService.getInstance(request).query(publicUserSession, "solr", query);
        resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        assertFalse(resultStr.contains("checksum"));


        // a document with the access rules that hasPermission can read it
        guid = new Identifier();
        guid.setValue("testAccess1." + System.currentTimeMillis());
        object = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
        sysmeta = D1NodeServiceTest.createSystemMetadata(guid, rightsHolderSubject, object);
        accessPolicy = new AccessPolicy();
        allow = new AccessRule();
        allow.addPermission(Permission.READ);
        allow.addSubject(hasPermissionSubject);
        accessPolicy.addAllow(allow);
        sysmeta.setAccessPolicy(accessPolicy);
        object = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
        d1NodeServiceTest.mnCreate(session, guid, object, sysmeta);
        query = "q=id:" + guid.getValue();
        stream = MNodeService.getInstance(request).query(rightsHolderSession, "solr", query);
        resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        count = 0;
        while ((resultStr == null || !resultStr.contains("checksum"))
            && count <= D1NodeServiceTest.MAX_TRIES) {
            Thread.sleep(DEFAULT_SLEEP_MS);
            count++;
            stream = MNodeService.getInstance(request).query(rightsHolderSession, "solr", query);
            resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        }

        assertNotNull(resultStr);
        assertTrue(resultStr.contains("checksum"));

        stream = MNodeService.getInstance(request).query(session, "solr", query);
        resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        assertFalse(resultStr.contains("checksum"));

        stream = MNodeService.getInstance(request).query(hasPermissionSession, "solr", query);
        resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        assertTrue(resultStr.contains("checksum"));

        stream = MNodeService.getInstance(request).query(noPermissionSession, "solr", query);
        resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        assertFalse(resultStr.contains("checksum"));

        stream = MNodeService.getInstance(request).query(publicUserSession, "solr", query);
        resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        assertFalse(resultStr.contains("checksum"));


        // a document with the access rules that hasPermission can change it
        guid = new Identifier();
        guid.setValue("testAccess3." + System.currentTimeMillis());
        object = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
        sysmeta = D1NodeServiceTest.createSystemMetadata(guid, rightsHolderSubject, object);
        accessPolicy = new AccessPolicy();
        allow = new AccessRule();
        allow.addPermission(Permission.CHANGE_PERMISSION);
        allow.addSubject(hasPermissionSubject);
        accessPolicy.addAllow(allow);
        sysmeta.setAccessPolicy(accessPolicy);
        object = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
        d1NodeServiceTest.mnCreate(session, guid, object, sysmeta);
        query = "q=id:" + guid.getValue();
        stream = MNodeService.getInstance(request).query(rightsHolderSession, "solr", query);
        resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);

        count = 0;
        while ((resultStr == null || !resultStr.contains("checksum"))
            && count <= D1NodeServiceTest.MAX_TRIES) {
            Thread.sleep(DEFAULT_SLEEP_MS);
            count++;
            stream = MNodeService.getInstance(request).query(rightsHolderSession, "solr", query);
            resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        }

        assertNotNull(resultStr);
        assertTrue(resultStr.contains("checksum"));

        stream = MNodeService.getInstance(request).query(noPermissionSession, "solr", query);
        resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        assertFalse(resultStr.contains("checksum"));

        stream = MNodeService.getInstance(request).query(publicUserSession, "solr", query);
        resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        assertFalse(resultStr.contains("checksum"));

        stream = MNodeService.getInstance(request).query(hasPermissionSession, "solr", query);
        resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        assertTrue(resultStr.contains("checksum"));

        stream = MNodeService.getInstance(request).query(session, "solr", query);
        resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        assertFalse(resultStr.contains("checksum"));

    }


    /**
     * Test insert a portal-1.1.0 document
     *
     * @throws Exception
     */
    @Test
    public void testPortal110() throws Exception {
        D1NodeServiceTest.printTestHeader("testPortal110");
        Session session = d1NodeServiceTest.getTestSession();
        Identifier guid = new Identifier();
        guid.setValue("testPortal101." + System.currentTimeMillis());
        InputStream object = new FileInputStream(portal110FilePath);
        SystemMetadata sysmeta =
            D1NodeServiceTest.createSystemMetadata(guid, session.getSubject(), object);
        ObjectFormatIdentifier formatId = new ObjectFormatIdentifier();
        formatId.setValue("https://purl.dataone.org/portals-1.1.0");
        sysmeta.setFormatId(formatId);
        LeanTestUtils.debug("the checksum is " + sysmeta.getChecksum().getValue());
        Identifier sid = new Identifier();
        sid.setValue("testPortal-sid" + System.currentTimeMillis());
        sysmeta.setSeriesId(sid);
        object.close();
        InputStream object2 = new FileInputStream(portal110FilePath);
        LeanTestUtils.debug("before insert the object +++++++++++++++++++++ " + guid.getValue());
        try {
            request.setHeader(
                QuotaServiceManager.QUOTASUBJECTHEADER, QuotaServiceManagerIT.SUBSCRIBER);
            Identifier pid =
                d1NodeServiceTest.mnCreate(session, guid, object2, sysmeta);
        } catch (Exception e) {
            LeanTestUtils.debug("the error is " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
        LeanTestUtils.debug("After insert the object =========================");
        String query = "q=id:" + guid.getValue();
        InputStream stream = MNodeService.getInstance(request).query(session, "solr", query);
        String resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        int count = 0;
        while ((resultStr == null || !resultStr.contains("checksum"))
            && count <= D1NodeServiceTest.MAX_TRIES) {
            Thread.sleep(DEFAULT_SLEEP_MS);
            count++;
            stream = MNodeService.getInstance(request).query(session, "solr", query);
            resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        }
        LeanTestUtils.debug(resultStr);
        assertNotNull(resultStr);
        assertTrue(resultStr.contains("<str name=\"label\">portal-1.1.0-example</str>"));
        assertTrue(resultStr.contains(
            "<str name=\"logo\">urn:uuid:349aa330-4645-4dab-a02d-3bf950cf708d</str>"));
        assertTrue(resultStr.contains(collectionQueryPortal110));
    }


    /**
     * Test insert a collection-1.1.0 document
     *
     * @throws Exception
     */
    @Test
    public void testCollectionl110() throws Exception {
        D1NodeServiceTest.printTestHeader("testCollectionl110");
        Session session = d1NodeServiceTest.getTestSession();
        Identifier guid = new Identifier();
        guid.setValue("testCollectionl101." + System.currentTimeMillis());
        InputStream object = new FileInputStream(collection110FilePath);
        SystemMetadata sysmeta =
            D1NodeServiceTest.createSystemMetadata(guid, session.getSubject(), object);
        ObjectFormatIdentifier formatId = new ObjectFormatIdentifier();
        formatId.setValue("https://purl.dataone.org/collections-1.1.0");
        sysmeta.setFormatId(formatId);
        LeanTestUtils.debug("the checksum is " + sysmeta.getChecksum().getValue());
        Identifier sid = new Identifier();
        sid.setValue("testPortal-sid" + System.currentTimeMillis());
        sysmeta.setSeriesId(sid);
        object.close();
        InputStream object2 = new FileInputStream(collection110FilePath);
        LeanTestUtils.debug("before insert the object +++++++++++++++++++++ " + guid.getValue());
        try {
            request.setHeader(
                QuotaServiceManager.QUOTASUBJECTHEADER, QuotaServiceManagerIT.SUBSCRIBER);
            Identifier pid =
                d1NodeServiceTest.mnCreate(session, guid, object2, sysmeta);
        } catch (Exception e) {
            LeanTestUtils.debug("the error is " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
        LeanTestUtils.debug("After insert the object =========================");
        String query = "q=id:" + guid.getValue();
        InputStream stream = MNodeService.getInstance(request).query(session, "solr", query);
        String resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        int count = 0;
        while ((resultStr == null || !resultStr.contains("checksum"))
            && count <= D1NodeServiceTest.MAX_TRIES) {
            Thread.sleep(DEFAULT_SLEEP_MS);
            count++;
            stream = MNodeService.getInstance(request).query(session, "solr", query);
            resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        }
        LeanTestUtils.debug(resultStr);
        assertNotNull(resultStr);
        assertTrue(resultStr.contains("<str name=\"label\">filterGroup-operator-example</str>"));
        assertTrue(resultStr.contains(collectionQueryCollection110));
    }

    /**
     * Query a schema.org document after creating it on Metacat
     *
     * @throws Exception
     */
    @Test
    public void testSchemaOrg() throws Exception {
        D1NodeServiceTest.printTestHeader("testSchemaOrg");
        ObjectFormatIdentifier formatId = new ObjectFormatIdentifier();
        formatId.setValue(NonXMLMetadataHandlers.JSON_LD);

        // create a json-ld object successfully
        File temp1 = JsonLDHandlerTest.generateTmpFile("temp-json-ld-valid");
        InputStream input = new FileInputStream(JsonLDHandlerTest.JSON_LD_FILE_PATH);
        OutputStream out = new FileOutputStream(temp1);
        IOUtils.copy(input, out);
        out.close();
        input.close();
        Checksum checksum = null;
        FileInputStream data = new FileInputStream(temp1);

        Session session = d1NodeServiceTest.getTestSession();
        Identifier guid = new Identifier();
        guid.setValue("testschemaOrg1." + System.currentTimeMillis());
        InputStream object = new FileInputStream(JsonLDHandlerTest.JSON_LD_FILE_PATH);
        SystemMetadata sysmeta =
            D1NodeServiceTest.createSystemMetadata(guid, session.getSubject(), object);
        sysmeta.setFormatId(formatId);
        object.close();
        try {
            request.setHeader(
                QuotaServiceManager.QUOTASUBJECTHEADER, QuotaServiceManagerIT.SUBSCRIBER);
            Identifier pid = d1NodeServiceTest.mnCreate(session, guid, data, sysmeta);
        } catch (Exception e) {
            LeanTestUtils.debug("the error is " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
        data.close();
        temp1.delete();

        String query = "q=id:" + guid.getValue();
        InputStream stream = MNodeService.getInstance(request).query(session, "solr", query);
        String resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        int count = 0;
        while ((resultStr == null || !resultStr.contains("checksum"))
            && count <= D1NodeServiceTest.MAX_TRIES) {
            Thread.sleep(DEFAULT_SLEEP_MS);
            count++;
            stream = MNodeService.getInstance(request).query(session, "solr", query);
            resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        }
        assertNotNull(resultStr);
        assertTrue(resultStr.contains(
            "<str name=\"title\">Removal of organic carbon by natural bacterioplankton"));
        assertTrue(resultStr.contains(
            "<str name=\"abstract\">This dataset includes results of laboratory"));
        assertTrue(resultStr.contains("<str name=\"edition\">2013-11-21</str>"));
    }

    /**
     * Test indexing schema.org object with different context settings
     *
     * @throws Exception
     */
    @Test
    public void testSchemaOrgWithContexts() throws Exception {
        D1NodeServiceTest.printTestHeader("testSchemaOrgWithContexts");
        ObjectFormatIdentifier formatId = new ObjectFormatIdentifier();
        formatId.setValue(NonXMLMetadataHandlers.JSON_LD);

        Session session = d1NodeServiceTest.getTestSession();
        Identifier guid = new Identifier();
        guid.setValue("testSchemaOrgWithContexts." + System.currentTimeMillis());
        InputStream object = new FileInputStream("test/context-http-vocab.jsonld");
        SystemMetadata sysmeta =
            D1NodeServiceTest.createSystemMetadata(guid, session.getSubject(), object);
        sysmeta.setFormatId(formatId);
        object.close();
        object = new FileInputStream("test/context-http-vocab.jsonld");
        d1NodeServiceTest.mnCreate(session, guid, object, sysmeta);
        object.close();
        String query = "q=id:" + guid.getValue();
        InputStream stream = MNodeService.getInstance(request).query(session, "solr", query);
        String resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        int count = 0;
        while ((resultStr == null || !resultStr.contains("checksum"))
            && count <= D1NodeServiceTest.MAX_TRIES) {
            Thread.sleep(DEFAULT_SLEEP_MS);
            count++;
            stream = MNodeService.getInstance(request).query(session, "solr", query);
            resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        }
        assertNotNull(resultStr);
        assertTrue(resultStr.contains("<str name=\"title\">test of context normalization"));
        assertTrue(resultStr.contains(
            "<str name=\"abstract\">No remote context, vocab http://schema.org/, creator 03, 02, "
                + "01"));

        guid = new Identifier();
        guid.setValue("testSchemaOrgWithContexts2." + System.currentTimeMillis());
        object = new FileInputStream("test/context-http.jsonld");
        sysmeta = D1NodeServiceTest.createSystemMetadata(guid, session.getSubject(), object);
        sysmeta.setFormatId(formatId);
        object.close();
        object = new FileInputStream("test/context-http.jsonld");
        d1NodeServiceTest.mnCreate(session, guid, object, sysmeta);
        object.close();
        query = "q=id:" + guid.getValue();
        stream = MNodeService.getInstance(request).query(session, "solr", query);
        resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        count = 0;
        while ((resultStr == null || !resultStr.contains("checksum"))
            && count <= D1NodeServiceTest.MAX_TRIES) {
            Thread.sleep(DEFAULT_SLEEP_MS);
            count++;
            stream = MNodeService.getInstance(request).query(session, "solr", query);
            resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        }
        assertNotNull(resultStr);
        assertTrue(resultStr.contains("<str name=\"title\">test of context normalization"));
        assertTrue(resultStr.contains(
            "<str name=\"abstract\">No remote context, vocab https://schema.org/, creator 03, 02,"
                + " 01, using @list"));

        guid = new Identifier();
        guid.setValue("testSchemaOrgWithContexts3." + System.currentTimeMillis());
        object = new FileInputStream("test/context-https-vocab.jsonld");
        sysmeta = D1NodeServiceTest.createSystemMetadata(guid, session.getSubject(), object);
        sysmeta.setFormatId(formatId);
        object.close();
        object = new FileInputStream("test/context-https-vocab.jsonld");
        d1NodeServiceTest.mnCreate(session, guid, object, sysmeta);
        object.close();
        query = "q=id:" + guid.getValue();
        stream = MNodeService.getInstance(request).query(session, "solr", query);
        resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        count = 0;
        while ((resultStr == null || !resultStr.contains("checksum"))
            && count <= D1NodeServiceTest.MAX_TRIES) {
            Thread.sleep(DEFAULT_SLEEP_MS);
            count++;
            stream = MNodeService.getInstance(request).query(session, "solr", query);
            resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        }
        assertNotNull(resultStr);
        assertTrue(resultStr.contains("<str name=\"title\">test of context normalization"));
        assertTrue(resultStr.contains(
            "<str name=\"abstract\">No remote context, vocab https://schema.org/, creator 03, 02,"
                + " 01"));

        guid = new Identifier();
        guid.setValue("testSchemaOrgWithContexts4." + System.currentTimeMillis());
        object = new FileInputStream("test/context-https.jsonld");
        sysmeta = D1NodeServiceTest.createSystemMetadata(guid, session.getSubject(), object);
        sysmeta.setFormatId(formatId);
        object.close();
        object = new FileInputStream("test/context-https.jsonld");
        d1NodeServiceTest.mnCreate(session, guid, object, sysmeta);
        object.close();
        query = "q=id:" + guid.getValue();
        stream = MNodeService.getInstance(request).query(session, "solr", query);
        resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        count = 0;
        while ((resultStr == null || !resultStr.contains("checksum"))
            && count <= D1NodeServiceTest.MAX_TRIES) {
            Thread.sleep(DEFAULT_SLEEP_MS);
            count++;
            stream = MNodeService.getInstance(request).query(session, "solr", query);
            resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        }
        assertNotNull(resultStr);
        assertTrue(resultStr.contains("<str name=\"title\">test of context normalization"));
        assertTrue(resultStr.contains("<str name=\"abstract\">Remote context, creator 03, 02, 01"));

        guid = new Identifier();
        guid.setValue("testSchemaOrgWithContexts5." + System.currentTimeMillis());
        object = new FileInputStream("test/context-http-doc.jsonld");
        sysmeta = D1NodeServiceTest.createSystemMetadata(guid, session.getSubject(), object);
        sysmeta.setFormatId(formatId);
        object.close();
        object = new FileInputStream("test/context-http-doc.jsonld");
        d1NodeServiceTest.mnCreate(session, guid, object, sysmeta);
        object.close();
        query = "q=id:" + guid.getValue();
        stream = MNodeService.getInstance(request).query(session, "solr", query);
        resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        count = 0;
        while ((resultStr == null || !resultStr.contains("checksum"))
            && count <= D1NodeServiceTest.MAX_TRIES) {
            Thread.sleep(DEFAULT_SLEEP_MS);
            count++;
            stream = MNodeService.getInstance(request).query(session, "solr", query);
            resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        }
        assertNotNull(resultStr);
        assertTrue(resultStr.contains("<str name=\"title\">test of context normalization"));
        assertTrue(resultStr.contains(
            "<str name=\"abstract\">No remote context, vocab https://schema.org/, creator 03, 02,"
                + " 01, using @list"));

        guid = new Identifier();
        guid.setValue("testSchemaOrgWithContexts6." + System.currentTimeMillis());
        object = new FileInputStream("test/context-https-doc.jsonld");
        sysmeta = D1NodeServiceTest.createSystemMetadata(guid, session.getSubject(), object);
        sysmeta.setFormatId(formatId);
        object.close();
        object = new FileInputStream("test/context-https-doc.jsonld");
        d1NodeServiceTest.mnCreate(session, guid, object, sysmeta);
        object.close();
        query = "q=id:" + guid.getValue();
        stream = MNodeService.getInstance(request).query(session, "solr", query);
        resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        count = 0;
        while ((resultStr == null || !resultStr.contains("checksum"))
            && count <= D1NodeServiceTest.MAX_TRIES) {
            Thread.sleep(DEFAULT_SLEEP_MS);
            count++;
            stream = MNodeService.getInstance(request).query(session, "solr", query);
            resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        }
        assertNotNull(resultStr);
        System.out.print(resultStr);
        assertTrue(resultStr.contains("<str name=\"title\">test of context normalization"));
        assertTrue(resultStr.contains("<str name=\"abstract\">Remote context, creator 03, 02, 01"));
    }

    /**
     * Test query result after use the updateSystemMetadata method to make an obsolescent chain.
     *
     * @throws Exception
     */
    @Test
    public void testUpdateSystemMetadataToMakeObsolescentChain() throws Exception {
        D1NodeServiceTest.printTestHeader("testUpdateSystemMetadataToMakeObsolescentChain");

        // insert data
        Session session = d1NodeServiceTest.getTestSession();
        Identifier guid = new Identifier();
        guid.setValue(
            "testUpdateSystemmetadataToMakeObsolescentChain." + System.currentTimeMillis());
        InputStream object = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
        SystemMetadata sysmeta =
            D1NodeServiceTest.createSystemMetadata(guid, session.getSubject(), object);
        object = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
        d1NodeServiceTest.mnCreate(session, guid, object, sysmeta);

        // insert data
        Identifier guid1 = new Identifier();
        guid1.setValue(
            "testUpdateSystemmetadataToMakeObsolescentChain-1." + System.currentTimeMillis());
        object = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
        SystemMetadata sysmeta1 =
            D1NodeServiceTest.createSystemMetadata(guid1, session.getSubject(), object);
        object = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
        d1NodeServiceTest.mnCreate(session, guid1, object, sysmeta1);

        // insert data
        Identifier guid2 = new Identifier();
        guid2.setValue(
            "testUpdateSystemmetadataToMakeObsolescentChain-2." + System.currentTimeMillis());
        object = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
        SystemMetadata sysmeta2 =
            D1NodeServiceTest.createSystemMetadata(guid2, session.getSubject(), object);
        object = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
        d1NodeServiceTest.mnCreate(session, guid2, object, sysmeta2);

        SystemMetadata sysmeta3 =
            MNodeService.getInstance(request).getSystemMetadata(session, guid);
        sysmeta3.setObsoletedBy(guid1);
        MNodeService.getInstance(request).updateSystemMetadata(session, guid, sysmeta3);

        SystemMetadata sysmeta4 =
            MNodeService.getInstance(request).getSystemMetadata(session, guid1);
        sysmeta4.setObsoletes(guid);
        sysmeta4.setObsoletedBy(guid2);
        MNodeService.getInstance(request).updateSystemMetadata(session, guid1, sysmeta4);

        SystemMetadata sysmeta5 =
            MNodeService.getInstance(request).getSystemMetadata(session, guid2);
        sysmeta5.setObsoletes(guid1);
        MNodeService.getInstance(request).updateSystemMetadata(session, guid2, sysmeta5);

        String query = "q=id:" + guid.getValue();
        InputStream stream = MNodeService.getInstance(request).query(session, "solr", query);
        String resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        int count = 0;
        while (
            (resultStr == null || !resultStr.contains("name=\"obsoletedBy\">" + guid1.getValue()))
                && count <= D1NodeServiceTest.MAX_TRIES) {
            Thread.sleep(DEFAULT_SLEEP_MS);
            count++;
            stream = MNodeService.getInstance(request).query(session, "solr", query);
            resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        }
        assertNotNull(resultStr);
        assertTrue(resultStr.contains("name=\"obsoletedBy\">" + guid1.getValue()));

        query = "q=id:" + guid1.getValue();
        stream = MNodeService.getInstance(request).query(session, "solr", query);
        resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        count = 0;
        while (
            (resultStr == null || !resultStr.contains("name=\"obsoletedBy\">" + guid2.getValue()))
                && count <= D1NodeServiceTest.MAX_TRIES) {
            Thread.sleep(DEFAULT_SLEEP_MS);
            count++;
            stream = MNodeService.getInstance(request).query(session, "solr", query);
            resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        }
        assertNotNull(resultStr);
        assertTrue(resultStr.contains("name=\"obsoletedBy\">" + guid2.getValue()));
        assertTrue(resultStr.contains("name=\"obsoletes\">" + guid.getValue()));


        query = "q=id:" + guid2.getValue();
        stream = MNodeService.getInstance(request).query(session, "solr", query);
        resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        count = 0;
        while ((resultStr == null || !resultStr.contains("name=\"obsoletes\">" + guid1.getValue()))
            && count <= D1NodeServiceTest.MAX_TRIES) {
            Thread.sleep(DEFAULT_SLEEP_MS);
            count++;
            stream = MNodeService.getInstance(request).query(session, "solr", query);
            resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        }
        assertNotNull(resultStr);
        assertTrue(resultStr.contains("name=\"obsoletes\">" + guid1.getValue()));

    }

    /**
     * Test to index an eml object with annotation
     *
     * @throws Exception
     */
    @Test
    public void testEmlWithAnnotation() throws Exception {
        D1NodeServiceTest.printTestHeader("testEmlWithAnnotation");

        Session session = d1NodeServiceTest.getTestSession();
        Identifier guid = new Identifier();
        guid.setValue("testEmlWithAnnotation." + System.currentTimeMillis());
        InputStream object = new FileInputStream(emlWithAnnotation);
        SystemMetadata sysmeta =
            D1NodeServiceTest.createSystemMetadata(guid, session.getSubject(), object);
        ObjectFormatIdentifier formatId = new ObjectFormatIdentifier();
        formatId.setValue("https://eml.ecoinformatics.org/eml-2.2.0");
        sysmeta.setFormatId(formatId);
        object.close();
        object = new FileInputStream(emlWithAnnotation);
        Identifier pid = d1NodeServiceTest.mnCreate(session, guid, object, sysmeta);
        String query = "q=id:" + guid.getValue();
        InputStream stream = MNodeService.getInstance(request).query(session, "solr", query);
        String resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        int count = 0;
        while ((resultStr == null || !resultStr.contains("checksum"))
            && count <= D1NodeServiceTest.MAX_TRIES) {
            Thread.sleep(DEFAULT_SLEEP_MS);
            count++;
            stream = MNodeService.getInstance(request).query(session, "solr", query);
            resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        }
        assertNotNull(resultStr);
        resultStr = resultStr.replaceAll("\\s", "");
        LeanTestUtils.debug("the guid is " + guid.getValue());
        LeanTestUtils.debug("the string is +++++++++++++++++++++++++++++++++++\n" + resultStr);

        assertTrue(resultStr.contains("<strname=\"id\">" + guid.getValue() + "</str>"));
        assertTrue(resultStr.contains(
            "http://ecoinformatics.org/oboe/oboe.1.2/oboe-core.owl#MeasurementType"));
        assertTrue(
            resultStr.contains("http://ecoinformatics.org/oboe/oboe.1.2/oboe-core.owl#hasUnit"));
        assertTrue(resultStr.contains("http://purl.dataone.org/odo/ECSO_00000629"));
        assertTrue(resultStr.contains(
            "http://ecoinformatics.org/oboe/oboe.1.2/oboe-core.owl#containsMeasurementsOfType"));
        assertTrue(resultStr.contains("http://purl.dataone.org/odo/ECSO_00000518"));
        assertTrue(resultStr.contains("http://purl.dataone.org/odo/ECSO_00000516"));
        assertTrue(resultStr.contains("http://purl.obolibrary.org/obo/UO_0000301"));
        assertTrue(resultStr.contains("http://purl.dataone.org/odo/ECSO_00000512"));
        assertTrue(resultStr.contains("http://purl.dataone.org/odo/ECSO_00001102"));
        assertTrue(resultStr.contains("http://purl.dataone.org/odo/ECSO_00001243"));
        assertTrue(resultStr.contains("http://www.w3.org/2000/01/rdf-schema#Resource"));

    }

    /**
     * Test delete an object not in packages
     *
     * @throws Exception
     */
    @Test
    public void testDelete() throws Exception {
        D1NodeServiceTest.printTestHeader("testDelete");

        // delete a single object
        Session session = d1NodeServiceTest.getTestSession();
        Session mnSession = d1NodeServiceTest.getMNSession();
        Identifier guid0 = new Identifier();
        guid0.setValue("testDelete-data." + System.currentTimeMillis());
        LeanTestUtils.debug("the data file id is ==== " + guid0.getValue());
        InputStream object = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
        SystemMetadata sysmeta =
            D1NodeServiceTest.createSystemMetadata(guid0, session.getSubject(), object);
        object = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
        d1NodeServiceTest.mnCreate(session, guid0, object, sysmeta);
        String query = "q=id:" + guid0.getValue();
        InputStream stream = MNodeService.getInstance(request).query(session, "solr", query);
        String resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        int count = 0;
        while ((resultStr == null || !resultStr.contains("checksum"))
            && count <= D1NodeServiceTest.MAX_TRIES) {
            Thread.sleep(DEFAULT_SLEEP_MS);
            count++;
            stream = MNodeService.getInstance(request).query(session, "solr", query);
            resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        }
        if (resultStr == null || !resultStr.contains("checksum")) {
            fail("The index of data object failed");
        }
        MNodeService.getInstance(request).delete(mnSession, guid0);
        // make sure the solr doc is gone
        stream = MNodeService.getInstance(request).query(session, "solr", query);
        resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        count = 0;
        while ((resultStr != null && resultStr.contains("checksum"))
            && count <= D1NodeServiceTest.MAX_TRIES) {
            Thread.sleep(DEFAULT_SLEEP_MS);
            count++;
            stream = MNodeService.getInstance(request).query(session, "solr", query);
            resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        }
        LeanTestUtils.debug("the query result is:\n" + resultStr);
        assertNotNull(resultStr);
        if (resultStr.contains("checksum")) {
            fail("Failed to delete the index of the data object.");
        }
    }

    /**
     * Delete objects in a package
     */
    @Test
    public void testDeletePackage() throws Exception {
        D1NodeServiceTest.printTestHeader("testDeletePackage");

        Session session = d1NodeServiceTest.getTestSession();
        Session mnSession = d1NodeServiceTest.getMNSession();
        // insert data
        Identifier guid = new Identifier();
        guid.setValue("deletePackage-data." + System.currentTimeMillis());
        LeanTestUtils.debug("the data file id is ==== " + guid.getValue());
        InputStream object = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
        SystemMetadata sysmeta =
            D1NodeServiceTest.createSystemMetadata(guid, session.getSubject(), object);
        object = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
        d1NodeServiceTest.mnCreate(session, guid, object, sysmeta);

        // insert metadata
        Identifier guid2 = new Identifier();
        guid2.setValue("deletePackage-metadata." + System.currentTimeMillis());
        LeanTestUtils.debug("the metadata  file id is ==== " + guid2.getValue());
        InputStream object2 = new FileInputStream(MNodeReplicationTest.replicationSourceFile);
        SystemMetadata sysmeta2 =
            D1NodeServiceTest.createSystemMetadata(guid2, session.getSubject(), object2);
        object2.close();
        ObjectFormatIdentifier formatId = new ObjectFormatIdentifier();
        formatId.setValue("eml://ecoinformatics.org/eml-2.0.1");
        sysmeta2.setFormatId(formatId);
        object2 = new FileInputStream(MNodeReplicationTest.replicationSourceFile);
        d1NodeServiceTest.mnCreate(session, guid2, object2, sysmeta2);

        // Make sure both data and metadata objects have been indexed
        String query = "q=id:" + guid.getValue();
        InputStream stream = MNodeService.getInstance(request).query(session, "solr", query);
        String resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        int count = 0;
        while ((resultStr == null || !resultStr.contains("checksum"))
            && count <= D1NodeServiceTest.MAX_TRIES) {
            Thread.sleep(DEFAULT_SLEEP_MS);
            count++;
            stream = MNodeService.getInstance(request).query(session, "solr", query);
            resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        }
        query = "q=id:" + guid2.getValue();
        stream = MNodeService.getInstance(request).query(session, "solr", query);
        resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        count = 0;
        while ((resultStr == null || !resultStr.contains("checksum"))
            && count <= D1NodeServiceTest.MAX_TRIES) {
            Thread.sleep(DEFAULT_SLEEP_MS);
            count++;
            stream = MNodeService.getInstance(request).query(session, "solr", query);
            resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        }
        // create the resourcemap
        Map<Identifier, List<Identifier>> idMap = new HashMap<>();
        List<Identifier> dataIds = new ArrayList<>();
        dataIds.add(guid);
        idMap.put(guid2, dataIds);
        Identifier resourceMapId = new Identifier();
        resourceMapId.setValue("deletePackage-resourcemap." + System.currentTimeMillis());
        LeanTestUtils.debug("the resource file id is ==== " + resourceMapId.getValue());
        ResourceMap rm = ResourceMapFactory.getInstance().createResourceMap(resourceMapId, idMap);
        String resourceMapXML = ResourceMapFactory.getInstance().serializeResourceMap(rm);
        InputStream object3 =
            new ByteArrayInputStream(resourceMapXML.getBytes(StandardCharsets.UTF_8));
        SystemMetadata sysmeta3 =
            D1NodeServiceTest.createSystemMetadata(resourceMapId, session.getSubject(), object3);
        ObjectFormatIdentifier formatId3 = new ObjectFormatIdentifier();
        formatId3.setValue("http://www.openarchives.org/ore/terms");
        sysmeta3.setFormatId(formatId3);
        object3 = new ByteArrayInputStream(resourceMapXML.getBytes(StandardCharsets.UTF_8));
        d1NodeServiceTest.mnCreate(session, resourceMapId, object3, sysmeta3);
        // make sure the metadata and data has the relationship
        query = "q=id:" + guid2.getValue();
        stream = MNodeService.getInstance(request).query(session, "solr", query);
        resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        count = 0;
        while ((resultStr == null || !resultStr.contains("documents"))
            && count <= D1NodeServiceTest.MAX_TRIES) {
            Thread.sleep(DEFAULT_SLEEP_MS);
            count++;
            stream = MNodeService.getInstance(request).query(session, "solr", query);
            resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        }
        LeanTestUtils.debug("the string is +++++++++++++++++++++++++++++++++++\n" + resultStr);
        assertNotNull(resultStr);
        assertTrue(resultStr.contains("<arr name=\"documents\">"));
        assertTrue(resultStr.contains(guid.getValue()));
        assertTrue(resultStr.contains("<arr name=\"resourceMap\">"));
        assertTrue(resultStr.contains(resourceMapId.getValue()));

        // delete the metadata object
        MNodeService.getInstance(request).delete(mnSession, guid2);
        // the metadata object should be gone.
        query = "q=id:" + guid2.getValue();
        stream = MNodeService.getInstance(request).query(session, "solr", query);
        resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        count = 0;
        while ((resultStr != null && resultStr.contains("checksum"))
            && count <= D1NodeServiceTest.MAX_TRIES) {
            Thread.sleep(DEFAULT_SLEEP_MS);
            count++;
            stream = MNodeService.getInstance(request).query(session, "solr", query);
            resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        }
        assertNotNull(resultStr);
        if (resultStr.contains("checksum")) {
            fail("Failed to delete the index of the metadata object.");
        }
        // the data object exists but the documentedBy should be gone.
        query = "q=id:" + guid.getValue();
        stream = MNodeService.getInstance(request).query(session, "solr", query);
        resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        count = 0;
        while ((resultStr != null && resultStr.contains("documentedBy"))
            && count <= D1NodeServiceTest.MAX_TRIES) {
            Thread.sleep(DEFAULT_SLEEP_MS);
            count++;
            stream = MNodeService.getInstance(request).query(session, "solr", query);
            resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        }
        assertNotNull(resultStr);
        if (resultStr.contains("documentedBy")) {
            fail("Failed to delete the index of the metadata object.");
        }
        assertNotNull(resultStr);
        assertTrue(resultStr.contains(guid.getValue()));
        assertTrue(resultStr.contains("<arr name=\"resourceMap\">"));
        assertTrue(resultStr.contains(resourceMapId.getValue()));

        // delete the resource map
        MNodeService.getInstance(request).delete(mnSession, resourceMapId);
        query = "q=id:" + resourceMapId.getValue();
        stream = MNodeService.getInstance(request).query(session, "solr", query);
        resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        count = 0;
        while ((resultStr != null && resultStr.contains("checksum"))
            && count <= D1NodeServiceTest.MAX_TRIES) {
            Thread.sleep(DEFAULT_SLEEP_MS);
            count++;
            stream = MNodeService.getInstance(request).query(session, "solr", query);
            resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        }
        assertNotNull(resultStr);
        if (resultStr.contains("checksum")) {
            fail("Failed to delete the index of the resource map.");
        }

        // delete the data file
        MNodeService.getInstance(request).delete(mnSession, guid);
        query = "q=id:" + guid.getValue();
        stream = MNodeService.getInstance(request).query(session, "solr", query);
        resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        count = 0;
        while ((resultStr != null && resultStr.contains("checksum"))
            && count <= D1NodeServiceTest.MAX_TRIES) {
            Thread.sleep(DEFAULT_SLEEP_MS);
            count++;
            stream = MNodeService.getInstance(request).query(session, "solr", query);
            resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        }
        assertNotNull(resultStr);
        if (resultStr.contains("checksum")) {
            fail("Failed to delete the index of the data object.");
        }
    }

    /**
     * Delete objects in a package, in a different order from the testDeletePackage method
     */
    @Test
    public void testDeletePackage2() throws Exception {
        D1NodeServiceTest.printTestHeader("testDeletePackage2");

        Session session = d1NodeServiceTest.getTestSession();
        Session mnSession = d1NodeServiceTest.getMNSession();
        // insert data
        Identifier dataGuid = new Identifier();
        dataGuid.setValue("deletePackage-data." + System.currentTimeMillis());
        LeanTestUtils.debug(
            "testDeletePackage2: the data file id (dataGuid) is ==== " + dataGuid.getValue());
        InputStream dataObject = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
        SystemMetadata sysmetaForData =
            D1NodeServiceTest.createSystemMetadata(dataGuid, session.getSubject(), dataObject);
        dataObject = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
        d1NodeServiceTest.mnCreate(session, dataGuid, dataObject, sysmetaForData);

        // insert metadata
        Identifier metadataGuid = new Identifier();
        metadataGuid.setValue("deletePackage-metadata." + System.currentTimeMillis());
        LeanTestUtils.debug("testDeletePackage2: the metadata file id (metadataGuid) is ==== "
                                + metadataGuid.getValue());
        InputStream metadataObject =
            new FileInputStream(MNodeReplicationTest.replicationSourceFile);
        SystemMetadata sysmetaForMetadata =
            D1NodeServiceTest.createSystemMetadata(metadataGuid, session.getSubject(),
                                                   metadataObject);
        metadataObject.close();
        ObjectFormatIdentifier formatId = new ObjectFormatIdentifier();
        formatId.setValue("eml://ecoinformatics.org/eml-2.0.1");
        sysmetaForMetadata.setFormatId(formatId);
        metadataObject = new FileInputStream(MNodeReplicationTest.replicationSourceFile);
        d1NodeServiceTest.mnCreate(session, metadataGuid, metadataObject, sysmetaForMetadata);

        // Make sure both data and metadata objects have been indexed
        String query = "q=id:" + dataGuid.getValue();
        InputStream stream = MNodeService.getInstance(request).query(session, "solr", query);
        String resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        int count = 0;
        while ((resultStr == null || !resultStr.contains("checksum"))
            && count <= D1NodeServiceTest.MAX_TRIES) {
            Thread.sleep(DEFAULT_SLEEP_MS);
            count++;
            stream = MNodeService.getInstance(request).query(session, "solr", query);
            resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        }
        if (count >= D1NodeServiceTest.MAX_TRIES) {
            LeanTestUtils.debug(
                "WARNING: reached D1NodeServiceTest.MAX_TRIES looking for data object index!");
        }
        assertNotNull(resultStr);
        assertTrue(resultStr.contains("checksum"));
        LeanTestUtils.debug("testDeletePackage2: data object has been indexed");

        query = "q=id:" + metadataGuid.getValue();
        stream = MNodeService.getInstance(request).query(session, "solr", query);
        resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        count = 0;
        while ((resultStr == null || !resultStr.contains("checksum"))
            && count <= D1NodeServiceTest.MAX_TRIES) {
            Thread.sleep(DEFAULT_SLEEP_MS);
            count++;
            stream = MNodeService.getInstance(request).query(session, "solr", query);
            resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        }
        if (count >= D1NodeServiceTest.MAX_TRIES) {
            LeanTestUtils.debug(
                "WARNING: reached D1NodeServiceTest.MAX_TRIES looking for metadata object index¡");
        }
        assertNotNull(resultStr);
        assertTrue(resultStr.contains("checksum"));
        LeanTestUtils.debug("testDeletePackage2: metadata object has been indexed");

        // create the resourcemap
        Map<Identifier, List<Identifier>> idMap = new HashMap<>();
        List<Identifier> dataIds = new ArrayList<>();
        dataIds.add(dataGuid);
        idMap.put(metadataGuid, dataIds);
        Identifier resourceMapId = new Identifier();
        resourceMapId.setValue("deletePackage-resourcemap." + System.currentTimeMillis());
        LeanTestUtils.debug("the resource file id is ==== " + resourceMapId.getValue());
        ResourceMap rm = ResourceMapFactory.getInstance().createResourceMap(resourceMapId, idMap);
        String resourceMapXML = ResourceMapFactory.getInstance().serializeResourceMap(rm);
        InputStream resourcemapObject =
            new ByteArrayInputStream(resourceMapXML.getBytes(StandardCharsets.UTF_8));
        SystemMetadata resourcemapSysmeta =
            D1NodeServiceTest.createSystemMetadata(resourceMapId, session.getSubject(),
                                                   resourcemapObject);
        ObjectFormatIdentifier resourcemapFormatId = new ObjectFormatIdentifier();
        resourcemapFormatId.setValue("http://www.openarchives.org/ore/terms");
        resourcemapSysmeta.setFormatId(resourcemapFormatId);
        resourcemapObject =
                new ByteArrayInputStream(resourceMapXML.getBytes(StandardCharsets.UTF_8));
        d1NodeServiceTest.mnCreate(session, resourceMapId, resourcemapObject, resourcemapSysmeta);

        // make sure the metadata and data has the relationship
        query = "q=id:" + metadataGuid.getValue();
        stream = MNodeService.getInstance(request).query(session, "solr", query);
        resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        count = 0;
        while ((resultStr == null || !resultStr.contains("documents"))
            && count <= D1NodeServiceTest.MAX_TRIES) {
            Thread.sleep(DEFAULT_SLEEP_MS);
            count++;
            stream = MNodeService.getInstance(request).query(session, "solr", query);
            resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        }
        LeanTestUtils.debug("testDeletePackage2: metadata resultStr +++++++++++++\n" + resultStr);
        assertNotNull(resultStr);
        assertTrue(resultStr.contains("<arr name=\"documents\">"));
        assertTrue(resultStr.contains(dataGuid.getValue()));
        assertTrue(resultStr.contains("<arr name=\"resourceMap\">"));
        assertTrue(resultStr.contains(resourceMapId.getValue()));

        // delete the resource map
        MNodeService.getInstance(request).delete(mnSession, resourceMapId);

        query = "q=id:" + resourceMapId.getValue();
        stream = MNodeService.getInstance(request).query(session, "solr", query);
        resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        count = 0;
        while ((resultStr != null && resultStr.contains("checksum"))
            && count <= D1NodeServiceTest.MAX_TRIES) {
            Thread.sleep(DEFAULT_SLEEP_MS);
            count++;
            stream = MNodeService.getInstance(request).query(session, "solr", query);
            resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        }
        if (count >= D1NodeServiceTest.MAX_TRIES) {
            LeanTestUtils.debug(
                "WARNING: reached D1NodeServiceTest.MAX_TRIES deleting resourcemap index!");
        }
        assertNotNull(resultStr);
        assertFalse("Failed to delete the index of the resource map.",
                    resultStr.contains("checksum"));

        // Ensure the metadata object does not have any relationship elements in the solr doc
        query = "q=id:" + metadataGuid.getValue();
        stream = MNodeService.getInstance(request).query(session, "solr", query);
        resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        count = 0;
        final long sleepTime = 2 * DEFAULT_SLEEP_MS;
        while (count++ <= D1NodeServiceTest.MAX_TRIES && resultStr != null && (
            resultStr.contains("documents") || resultStr.contains("<arr name=\"resourceMap\">"))) {
            Thread.sleep(sleepTime);
            stream = MNodeService.getInstance(request).query(session, "solr", query);
            resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        }
        if (count >= D1NodeServiceTest.MAX_TRIES) {
            LeanTestUtils.debug(
                "WARNING: reached D1NodeServiceTest.MAX_TRIES deleting relationship elements!");
        }
        assertNotNull(resultStr);
        assertFalse(
            "Failed to delete the relationship (documents) elements in the metadata object",
            resultStr.contains("documents"));
        assertFalse("Failed to delete the <arr name=\"resourceMap\"> elements in the data object",
                    resultStr.contains("<arr name=\"resourceMap\">"));

        // Ensure the data object does not have any relationship elements in the solr doc
        query = "q=id:" + dataGuid.getValue();
        stream = MNodeService.getInstance(request).query(session, "solr", query);
        resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        count = 0;
        while (count++ <= D1NodeServiceTest.MAX_TRIES && resultStr != null && (
            resultStr.contains("documents") || resultStr.contains("<arr name=\"resourceMap\">"))) {
            Thread.sleep(DEFAULT_SLEEP_MS);
            stream = MNodeService.getInstance(request).query(session, "solr", query);
            resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        }
        assertNotNull(resultStr);
        assertFalse("Failed to delete the <arr name=\"resourceMap\"> elements in the data object",
                    resultStr.contains("<arr name=\"resourceMap\">"));

        // delete the metadata object
        MNodeService.getInstance(request).delete(mnSession, metadataGuid);
        // the metadata object should be gone.
        query = "q=id:" + metadataGuid.getValue();
        stream = MNodeService.getInstance(request).query(session, "solr", query);
        resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        count = 0;
        while ((resultStr != null && resultStr.contains("checksum"))
            && count <= D1NodeServiceTest.MAX_TRIES) {
            Thread.sleep(DEFAULT_SLEEP_MS);
            count++;
            stream = MNodeService.getInstance(request).query(session, "solr", query);
            resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        }
        assertNotNull(resultStr);
        if (resultStr.contains("checksum")) {
            fail("Failed to delete the index of the metadata object.");
        }

        // delete the data file
        MNodeService.getInstance(request).delete(mnSession, dataGuid);
        query = "q=id:" + dataGuid.getValue();
        stream = MNodeService.getInstance(request).query(session, "solr", query);
        resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        count = 0;
        while ((resultStr != null && resultStr.contains("checksum"))
            && count <= D1NodeServiceTest.MAX_TRIES) {
            Thread.sleep(DEFAULT_SLEEP_MS);
            count++;
            stream = MNodeService.getInstance(request).query(session, "solr", query);
            resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        }
        assertNotNull(resultStr);
        if (resultStr.contains("checksum")) {
            fail("Failed to delete the index of the data object.");
        }
    }

}
