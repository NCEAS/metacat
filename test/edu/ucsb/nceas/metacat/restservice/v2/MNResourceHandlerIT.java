package edu.ucsb.nceas.metacat.restservice.v2;

import edu.ucsb.nceas.metacat.dataone.D1NodeServiceTest;
import edu.ucsb.nceas.metacat.util.SystemUtil;
import org.apache.commons.io.IOUtils;
import org.apache.wicket.protocol.http.mock.MockHttpServletRequest;
import org.apache.wicket.protocol.http.mock.MockHttpServletResponse;
import org.apache.wicket.protocol.http.mock.MockHttpSession;
import org.dataone.client.rest.DefaultHttpMultipartRestClient;
import org.dataone.client.v2.formats.ObjectFormatCache;
import org.dataone.configuration.Settings;
import org.dataone.mimemultipart.SimpleMultipartEntity;
import org.dataone.ore.ResourceMapFactory;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Session;
import org.dataone.service.types.v2.SystemMetadata;
import org.dspace.foresite.ResourceMap;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Test the query and solr query actions
 */
public class MNResourceHandlerIT extends MNResourceHandlerTest {
    private final static String QUERY = "query";
    private final static String SOLR = "solr";
    private final static String ID_START = "id:*";
    private final static String SELECT_ALL = "q=" + ID_START;
    private final static String CHECKSUM = "<str name=\"checksum\">";
    private final static String SOLR_CONFIG = "solrconfig.xml";
    private final static String FILE_QUERY = "file=" + SOLR_CONFIG;
    private final static String ADMIN_FILE = "/admin/file";
    private final static String qt = "qt";
    private final static String qt_WITH_VALUE = qt + "=" + ADMIN_FILE;
    private final static String qt_QUERY = qt + "&" + FILE_QUERY;
    private final static String QT_QUERY = "QT=/admin/file&" + FILE_QUERY;
    private final static String qt_UPDATE = "qt=update";
    private final static String SOLRCONFIG_PART_CONTENT = "solr.SchemaCodecFactory";
    private final static String qt_NOT_ALLOWED = "qt is not allowed";
    private final static String ENCODED_qt = "%71%74";
    private final static String SOLR_PATH = "/d1/mn/v2/query/solr";

    private D1NodeServiceTest d1NodeTest = null;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        d1NodeTest = new D1NodeServiceTest("initialize");
        Settings.getConfiguration().clearProperty("D1Client.CN_URL");
        Settings.getConfiguration().addProperty("D1Client.CN_URL", "https://cn.dataone.org/cn");
    }


    /**
     * Test the solr query with the http get method
     * @throws Exception
     */
    @Test
    public void testGetQuery() throws Exception {
        // Regular query to select all
        refreshResourceHandler("/" + QUERY + "/" + SOLR + "/" + SELECT_ALL);
        resourceHandler.handle(GET);
        String result = new String(response.getBinaryContent());
        assertTrue(result.contains(CHECKSUM));
        // Regular query with QT. The QT parameter will be ignored.
        refreshResourceHandler("/" + QUERY + "/" + SOLR + "/" + SELECT_ALL + "&" + QT_QUERY);
        resourceHandler.handle(GET);
        result = new String(response.getBinaryContent());
        assertTrue(result.contains(CHECKSUM));
        assertFalse(result.contains(SOLRCONFIG_PART_CONTENT));
        // Queries containing qt will be rejected
        refreshResourceHandler("/" + QUERY + "/" + SOLR + "/" + SELECT_ALL + "&" + qt_QUERY);
        resourceHandler.handle(GET);
        assertEquals(500, response.getStatus());
        result = new String(response.getBinaryContent());
        assertTrue(result.contains(qt_NOT_ALLOWED));
        refreshResourceHandler("/" + QUERY + "/" + SOLR + "/?" + SELECT_ALL + "&" + qt_QUERY);
        resourceHandler.handle(GET);
        assertEquals(500, response.getStatus());
        result = new String(response.getBinaryContent());
        assertTrue(result.contains(qt_NOT_ALLOWED));
        refreshResourceHandler("/" + QUERY + "/" + SOLR + "?" + SELECT_ALL + "&" + qt_QUERY);
        resourceHandler.handle(GET);
        assertEquals(500, response.getStatus());
        result = new String(response.getBinaryContent());
        assertTrue(result.contains(qt_NOT_ALLOWED));
        refreshResourceHandler("/" + QUERY + "/" + SOLR + "?" + qt_QUERY);
        resourceHandler.handle(GET);
        assertEquals(500, response.getStatus());
        result = new String(response.getBinaryContent());
        assertTrue(result.contains(qt_NOT_ALLOWED));
        refreshResourceHandler("/" + QUERY + "/" + SOLR + "?" + qt_UPDATE);
        resourceHandler.handle(GET);
        assertEquals(500, response.getStatus());
        result = new String(response.getBinaryContent());
        assertTrue(result.contains(qt_NOT_ALLOWED));
        refreshResourceHandler("/" + QUERY + "/" + SOLR + "/?" + ENCODED_qt + "=update");
        resourceHandler.handle(GET);
        assertEquals(500, response.getStatus());
        result = new String(response.getBinaryContent());
        assertTrue(result.contains(qt_NOT_ALLOWED));
    }


    /**
     * Test the solr query with the http post method
     * @throws Exception
     */
    @Test
    public void testPostQuery() throws Exception {
        DefaultHttpMultipartRestClient multipartRestClient = new DefaultHttpMultipartRestClient();
        String server = SystemUtil.getContextURL();
        SimpleMultipartEntity params = new SimpleMultipartEntity();
        // Regular query
        params.addParamPart("q", ID_START);
        InputStream stream =
            multipartRestClient.doPostRequest(server + SOLR_PATH, params, 30000);
        String resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        assertTrue(resultStr.contains(CHECKSUM));
        assertFalse(resultStr.contains(SOLRCONFIG_PART_CONTENT));
        // Regular query with qt
        params.addParamPart(qt, ADMIN_FILE);
        params.addParamPart("file", SOLR_CONFIG);
        try {
            multipartRestClient.doPostRequest(server + SOLR_PATH, params, 30000);
            fail("Test can reach here");
        } catch (Exception e) {
            assertTrue(e.getMessage().contains(qt));
        }
        // QT will be ignored
        params = new SimpleMultipartEntity();
        params.addParamPart("q", ID_START);
        params.addParamPart("QT", ADMIN_FILE);
        params.addParamPart("file", SOLR_CONFIG);
        stream = multipartRestClient.doPostRequest(server + SOLR_PATH, params, 30000);
        resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        assertTrue(resultStr.contains(CHECKSUM));
        assertFalse(resultStr.contains(SOLRCONFIG_PART_CONTENT));
        // %71%74 will be ignored since it is not in the url
        params = new SimpleMultipartEntity();
        params.addParamPart(ENCODED_qt, ADMIN_FILE);
        params.addParamPart("file", SOLR_CONFIG);
        params.addParamPart("q", ID_START);
        stream = multipartRestClient.doPostRequest(server + SOLR_PATH, params, 30000);
        resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        assertTrue(resultStr.contains(CHECKSUM));
        assertTrue(resultStr.contains(ENCODED_qt));
        assertFalse(resultStr.contains(SOLRCONFIG_PART_CONTENT));
    }


    /**
     * Test the package API
     */
    @Test
    public void testGetPackage() throws Exception {
        // construct the ORE package
        Identifier resourceMapId = new Identifier();
        //resourceMapId.setValue("doi://1234/AA/map.1.1");
        resourceMapId.setValue("testGetOREPackage-" + System.currentTimeMillis());
        Identifier metadataId = new Identifier();
        metadataId.setValue("doi://1234/AA/meta.1." + System.currentTimeMillis());
        List<Identifier> dataIds = new ArrayList<>();
        Identifier dataId = new Identifier();
        dataId.setValue("doi://1234/AA/data.1." + System.currentTimeMillis());
        Identifier dataId2 = new Identifier();
        dataId2.setValue("doi://1234/AA/data.2." + System.currentTimeMillis());
        dataIds.add(dataId);
        dataIds.add(dataId2);
        Map<Identifier, List<Identifier>> idMap = new HashMap<>();
        idMap.put(metadataId, dataIds);
        ResourceMapFactory rmf = ResourceMapFactory.getInstance();
        ResourceMap resourceMap = rmf.createResourceMap(resourceMapId, idMap);
        assertNotNull(resourceMap);
        String rdfXml = ResourceMapFactory.getInstance().serializeResourceMap(resourceMap);
        assertNotNull(rdfXml);

        Session session = d1NodeTest.getTestSession();
        InputStream object = null;
        SystemMetadata sysmeta = null;

        // save the data objects (data just contains their ID)
        InputStream dataObject1 = new ByteArrayInputStream(dataId.getValue().getBytes(StandardCharsets.UTF_8));
        sysmeta = D1NodeServiceTest.createSystemMetadata(dataId, session.getSubject(), dataObject1);
        dataObject1 = new ByteArrayInputStream(dataId.getValue().getBytes(StandardCharsets.UTF_8));
        d1NodeTest.mnCreate(session, dataId, dataObject1, sysmeta);

        // second data file
        InputStream dataObject2 =
            new ByteArrayInputStream(dataId2.getValue().getBytes(StandardCharsets.UTF_8));
        sysmeta = D1NodeServiceTest.createSystemMetadata(dataId2, session.getSubject(), dataObject2);
        dataObject2 =
            new ByteArrayInputStream(dataId2.getValue().getBytes(StandardCharsets.UTF_8));
        d1NodeTest.mnCreate(session, dataId2, dataObject2, sysmeta);

        // metadata file
        InputStream metadataObject =
            new ByteArrayInputStream(metadataId.getValue().getBytes(StandardCharsets.UTF_8));
        sysmeta = D1NodeServiceTest.createSystemMetadata(metadataId, session.getSubject(), metadataObject);
        metadataObject =
            new ByteArrayInputStream(metadataId.getValue().getBytes(StandardCharsets.UTF_8));
        d1NodeTest.mnCreate(session, metadataId, metadataObject, sysmeta);

        // save the ORE object
        object = new ByteArrayInputStream(rdfXml.getBytes(StandardCharsets.UTF_8));
        sysmeta = D1NodeServiceTest.createSystemMetadata(resourceMapId, session.getSubject(), object);
        sysmeta.setFormatId(
            ObjectFormatCache
                .getInstance().getFormat("http://www.openarchives.org/ore/terms")
                .getFormatId());
        object = new ByteArrayInputStream(rdfXml.getBytes(StandardCharsets.UTF_8));
        Identifier pid =
            d1NodeTest.mnCreate(session, resourceMapId, object, sysmeta);
        request = Mockito.spy(new MockHttpServletRequest(
            null,
            new MockHttpSession(context),
            context));
        Mockito.doReturn("/packages/application%2Fbagit-1.0/" + resourceMapId.getValue())
            .when(request)
            .getPathInfo();
        response = new MockHttpServletResponse(request);
        resourceHandler = new MNResourceHandler(request, response);
        resourceHandler.handle(GET);
        Path bagFile = Files.createTempFile("bagit.", ".zip");
        try {
            byte[] bytes = response.getBinaryContent();
            Files.write(bagFile, bytes);
            // Check that the resource map is the same
            ZipFile zipFile = new ZipFile(bagFile.toFile());
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            Vector<String> list = new Vector<>();
            list.add("resourceMap");
            list.add("scienceMeta");
            list.add("data1");
            list.add("data2");
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                // Check if it's the ORE
                if (entry.getName().contains("testGetOREPackage")) {
                    InputStream stream2 = zipFile.getInputStream(entry);
                    assertNotNull(stream2);
                    list.remove("resourceMap");
                } else if (entry.getName().contains("metadata/science-metadata.xml")) {
                    InputStream stream2 = zipFile.getInputStream(entry);
                    metadataObject.reset();
                    assertTrue(IOUtils.contentEquals(stream2, metadataObject));
                    list.remove("scienceMeta");
                } else if (entry.getName().contains("data.1") && !(entry.getName()
                    .contains("sysmeta"))) {
                    InputStream stream2 = zipFile.getInputStream(entry);
                    dataObject1.reset();
                    assertTrue(IOUtils.contentEquals(stream2, dataObject1));
                    list.remove("data1");
                } else if (entry.getName().contains("data.2") && !(entry.getName()
                    .contains("sysmeta"))) {
                    InputStream stream2 = zipFile.getInputStream(entry);
                    dataObject2.reset();
                    assertTrue(IOUtils.contentEquals(stream2, dataObject2));
                    list.remove("data2");
                }
            }
            assertEquals(0, list.size());
        } finally {
            Files.deleteIfExists(bagFile);
        }
    }

    /**
     * Test the package API
     */
    @Test
    public void testGetPackageWithMissingMember() throws Exception {
        // construct the ORE package
        Identifier resourceMapId = new Identifier();
        //resourceMapId.setValue("doi://1234/AA/map.1.1");
        resourceMapId.setValue("testGetOREPackage-" + System.currentTimeMillis());
        Identifier metadataId = new Identifier();
        metadataId.setValue("doi://1234/AA/meta.1." + System.currentTimeMillis());
        List<Identifier> dataIds = new ArrayList<>();
        Identifier dataId = new Identifier();
        dataId.setValue("doi://1234/AA/data.1." + System.currentTimeMillis());
        Identifier dataId2 = new Identifier();
        dataId2.setValue("doi://1234/AA/data.2." + System.currentTimeMillis());
        dataIds.add(dataId);
        dataIds.add(dataId2);
        Map<Identifier, List<Identifier>> idMap = new HashMap<>();
        idMap.put(metadataId, dataIds);
        ResourceMapFactory rmf = ResourceMapFactory.getInstance();
        ResourceMap resourceMap = rmf.createResourceMap(resourceMapId, idMap);
        assertNotNull(resourceMap);
        String rdfXml = ResourceMapFactory.getInstance().serializeResourceMap(resourceMap);
        assertNotNull(rdfXml);

        Session session = d1NodeTest.getTestSession();
        InputStream object = null;
        SystemMetadata sysmeta = null;

        // save the data objects (data just contains their ID)
        InputStream dataObject1 = new ByteArrayInputStream(dataId.getValue().getBytes(StandardCharsets.UTF_8));
        sysmeta = D1NodeServiceTest.createSystemMetadata(dataId, session.getSubject(), dataObject1);
        dataObject1 = new ByteArrayInputStream(dataId.getValue().getBytes(StandardCharsets.UTF_8));
        d1NodeTest.mnCreate(session, dataId, dataObject1, sysmeta);

        // No second data file created

        // No metadata file created

        // save the ORE object
        object = new ByteArrayInputStream(rdfXml.getBytes(StandardCharsets.UTF_8));
        sysmeta = D1NodeServiceTest.createSystemMetadata(resourceMapId, session.getSubject(), object);
        sysmeta.setFormatId(
            ObjectFormatCache
                .getInstance().getFormat("http://www.openarchives.org/ore/terms")
                .getFormatId());
        object = new ByteArrayInputStream(rdfXml.getBytes(StandardCharsets.UTF_8));
        Identifier pid =
            d1NodeTest.mnCreate(session, resourceMapId, object, sysmeta);
        request = Mockito.spy(new MockHttpServletRequest(
            null,
            new MockHttpSession(context),
            context));
        Mockito.doReturn("/packages/application%2Fbagit-1.0/" + resourceMapId.getValue())
            .when(request)
            .getPathInfo();
        response = new MockHttpServletResponse(request);
        resourceHandler = new MNResourceHandler(request, response);
        resourceHandler.handle(GET);
        Path bagFile = Files.createTempFile("bagit.", ".zip");
        try {
            Vector<String> list = new Vector<>();
            list.add("resourceMap");
            list.add("data1");
            byte[] bytes = response.getBinaryContent();
            Files.write(bagFile, bytes);
            // Check that the resource map is the same
            ZipFile zipFile = new ZipFile(bagFile.toFile());
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                // Check if it's the ORE
                if (entry.getName().contains("testGetOREPackage")) {
                    InputStream stream2 = zipFile.getInputStream(entry);
                    assertNotNull(stream2);
                    list.remove("resourceMap");
                } else if (entry.getName().contains("metadata/science-metadata.xml")) {
                   fail("The metadata object shouldn't be in the package");
                } else if (entry.getName().contains("data.1") && !(entry.getName()
                    .contains("sysmeta"))) {
                    InputStream stream2 = zipFile.getInputStream(entry);
                    dataObject1.reset();
                    assertTrue(IOUtils.contentEquals(stream2, dataObject1));
                    list.remove("data1");
                } else if (entry.getName().contains("data.2")) {
                    fail("The second data object shouldn't be in the package");
                }
            }
            assertEquals(0, list.size());
        } finally {
            Files.deleteIfExists(bagFile);
        }
    }
}


