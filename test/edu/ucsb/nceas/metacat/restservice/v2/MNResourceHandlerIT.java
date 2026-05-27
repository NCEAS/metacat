package edu.ucsb.nceas.metacat.restservice.v2;

import edu.ucsb.nceas.metacat.util.SystemUtil;
import org.apache.commons.io.IOUtils;
import org.dataone.client.rest.DefaultHttpMultipartRestClient;
import org.dataone.mimemultipart.SimpleMultipartEntity;
import org.junit.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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
     * Test the solr query with the http get method
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
}


