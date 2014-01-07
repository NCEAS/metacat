package edu.ucsb.nceas.metacat.common.query;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.solr.common.params.SolrParams;
import org.apache.solr.schema.SchemaField;
import org.apache.solr.servlet.SolrRequestParsers;
import org.dataone.service.exceptions.NotFound;
import org.dataone.service.exceptions.UnsupportedType;
import org.junit.Test;
import org.xml.sax.SAXException;

import edu.ucsb.nceas.metacat.common.MetacatCommonTestBase;

public class SolrQueryServiceControllerTest extends MetacatCommonTestBase {
    /**
     * Test get the solr version
     * @throws SAXException 
     * @throws IOException 
     * @throws ParserConfigurationException 
     * @throws NotFound 
     * @throws UnsupportedType 
     */
    @Test
    public void testGetSolrSpecVersion() throws UnsupportedType, NotFound, ParserConfigurationException, IOException, SAXException {
        String version = SolrQueryServiceController.getInstance().getSolrSpecVersion();
        //System.out.println("version is ======================== "+version);
        assertTrue(version != null);
        assertTrue("The version should be 3.4.0.2011.09.09.09.06.42 rather than "+version, version.equals("3.4.0.2011.09.09.09.06.42"));
    }
    
    
    /**
     * Test get get valid schema fields.
     * @throws SAXException 
     * @throws IOException 
     * @throws ParserConfigurationException 
     * @throws NotFound 
     * @throws UnsupportedType 
     */
    @Test
    public void testgetIndexSchemaFields() throws Exception {
       Map<String, SchemaField> fields = SolrQueryServiceController.getInstance().getIndexSchemaFields();
       assertTrue(fields != null);
       assertTrue("The number of index schema fields should be 102 rather than "+fields.size(), fields.size() == 102);
    }
    
    /**
     * Test the query method
     */
    @Test
    public void testQuery() throws Exception {
        String query = "q=*:*";
        SolrParams solrParams = SolrRequestParsers.parseQueryString(query);
        InputStream input = SolrQueryServiceController.getInstance().query(solrParams, null);
        assertTrue(input != null);
    }
}
