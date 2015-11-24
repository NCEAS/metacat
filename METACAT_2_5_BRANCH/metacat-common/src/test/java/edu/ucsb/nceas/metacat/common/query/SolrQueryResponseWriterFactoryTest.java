package edu.ucsb.nceas.metacat.common.query;

import static org.junit.Assert.assertTrue;


import org.apache.solr.response.JSONResponseWriter;

import org.apache.solr.response.QueryResponseWriter;

import org.apache.solr.response.XMLResponseWriter;
import org.junit.Test;

import edu.ucsb.nceas.metacat.common.MetacatCommonTestBase;

public class SolrQueryResponseWriterFactoryTest extends MetacatCommonTestBase {
    
    /**
     * Test the generate method
     * @throws Exception
     */
    @Test
    public void testGenerateResponseWriter() throws Exception {
        QueryResponseWriter writer = SolrQueryResponseWriterFactory.generateResponseWriter(null);
        assertTrue(writer instanceof XMLResponseWriter);
        writer = SolrQueryResponseWriterFactory.generateResponseWriter(SolrQueryResponseWriterFactory.XML);
        assertTrue(writer instanceof XMLResponseWriter);
        
        writer = SolrQueryResponseWriterFactory.generateResponseWriter(SolrQueryResponseWriterFactory.JSON);
        assertTrue(writer instanceof JSONResponseWriter);
        
        try {
            writer = SolrQueryResponseWriterFactory.generateResponseWriter("other");
            assertTrue("There is not wirter for name - other", false);
        } catch (Exception e ) {
            assertTrue("There is not wirter for name - other", true);
        }
    }
    /**
     * Test the get content type method
     */
    @Test
    public void testGetContentType() throws Exception {
        String contentType = SolrQueryResponseWriterFactory.getContentType(SolrQueryResponseWriterFactory.XML);
        assertTrue(contentType.equals("text/xml"));
        contentType = SolrQueryResponseWriterFactory.getContentType(SolrQueryResponseWriterFactory.JSON);
        assertTrue(contentType.equals("text/json"));
        contentType = SolrQueryResponseWriterFactory.getContentType("other");
        assertTrue(contentType == null);

    }
}
