package edu.ucsb.nceas.metacat.index;

import edu.ucsb.nceas.LeanTestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.dataone.service.exceptions.InvalidRequest;
import org.junit.Before;
import org.junit.Test;

import java.io.InputStream;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * An IT test for MetacatSolrIndex
 */
public class MetacatSolrIndexIT {
    /**
     * Setup
     * @throws Exception
     */
    @Before
    public void setUp() throws Exception {
        LeanTestUtils.initializePropertyService(LeanTestUtils.PropertiesMode.UNIT_TEST);
    }

    /**
     * Test the query method with/without the parameter qt
     * @throws Exception
     */
    @Test
    public void testQtQuery() throws Exception {
        ModifiableSolrParams params = new ModifiableSolrParams();
        params.set("q", "id:*");
        params.set("rows", 10);
        params.set("wt", "xml");
        InputStream inputStream = MetacatSolrIndex.getInstance().query(params, null, false,
                                                         SolrRequest.METHOD.GET);
        String input = IOUtils.toString(inputStream, "UTF-8");
        //It must have a record on solr
        assertTrue(input.contains("name=\"checksum\""));
        inputStream = MetacatSolrIndex.getInstance().query(params, null, false,
                                                                       SolrRequest.METHOD.POST);
        input = IOUtils.toString(inputStream, "UTF-8");
        assertTrue(input.contains("name=\"checksum\""));
        // QT (uppercase) is fine.
        params.set("QT", "/admin/file");
        params.set("file", "solrconfig.xml");
        inputStream = MetacatSolrIndex.getInstance().query(params, null, false,
                                                           SolrRequest.METHOD.GET);
        input = IOUtils.toString(inputStream, "UTF-8");
        assertTrue(input.contains("name=\"checksum\""));
        assertFalse(input.contains("solr.SchemaCodecFactory"));
        inputStream = MetacatSolrIndex.getInstance().query(params, null, false,
                                                           SolrRequest.METHOD.POST);
        input = IOUtils.toString(inputStream, "UTF-8");
        assertTrue(input.contains("name=\"checksum\""));
        assertFalse(input.contains("solr.SchemaCodecFactory"));
        // Add the qt parameter
        params.set("qt", "/admin/file");
        params.remove("QT");
        // Requests containing "qt" must be rejected in the both get and post methods
        try {
            MetacatSolrIndex.getInstance().query(params, null, false, SolrRequest.METHOD.GET);
            fail("Test can't get there since the previous statement should throw an exception");
        } catch (Exception e) {
            assertTrue(e instanceof InvalidRequest);
        }
        try {
            MetacatSolrIndex.getInstance().query(params, null, false, SolrRequest.METHOD.POST);
            fail("Test can't get there since the previous statement should throw an exception");
        } catch (Exception e) {
            assertTrue(e instanceof InvalidRequest);
        }
    }

}
