package edu.ucsb.nceas.metacat.common.query;

import static org.junit.Assert.assertTrue;

import java.io.InputStream;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.servlet.SolrRequestParsers;
import org.junit.Test;

import edu.ucsb.nceas.metacat.common.MetacatCommonTestBase;
import edu.ucsb.nceas.metacat.common.SolrServerFactory;

public class SolrQueryResponseTransformerTest extends MetacatCommonTestBase {
    
    /**
     * Test the transformResults method
     */
    @Test
    public void testTransformResults() throws Exception {
        
        SolrServer solrServer = SolrServerFactory.createSolrServer();
        CoreContainer coreContainer = SolrServerFactory.getCoreContainer();
        String collectionName = SolrServerFactory.getCollectionName();
        SolrQueryResponseTransformer solrTransformer = new SolrQueryResponseTransformer(coreContainer.getCore(collectionName));
        InputStream inputStream = null;
        String wt = "xml";
        String queryStr = "q=*:*";
        SolrParams query = SolrRequestParsers.parseQueryString(queryStr);
        QueryResponse response = solrServer.query(query);
        inputStream = solrTransformer.transformResults(query, response, wt);
        assertTrue(inputStream != null);
    }
}
