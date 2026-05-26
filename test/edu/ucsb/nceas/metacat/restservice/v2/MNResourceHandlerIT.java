package edu.ucsb.nceas.metacat.restservice.v2;


import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Test the query and solr query actions
 */
public class MNResourceHandlerIT extends MNResourceHandlerTest {
    private final static String QUERY = "query";
    private final static String SOLR = "solr";
    private final static String SELECT_ALL = "q=id:*";
    private final static String CHECKSUM = "<str name=\"checksum\">";
    private final static String FILE_QUERY = "file=solrconfig.xml";
    private final static String qt = "qt=/admin/file";
    private final static String qt_QUERY = qt + "&" + FILE_QUERY;
    private final static String QT_QUERY = "QT=/admin/file&" + FILE_QUERY;
    private final static String qt_UPDATE = "qt=update";
    private final static String SOLRCONFIG_PART_CONTENT = "solr.SchemaCodecFactory";
    private final static String qt_NOT_ALLOWED = "qt is not allowed";
    private final static String ENCODED_qt = "%71%74";


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
}


