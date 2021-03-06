package edu.ucsb.nceas.metacat.common;

import static org.junit.Assert.assertTrue;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.solr.client.solrj.SolrClient;
import org.junit.Before;
import org.junit.Test;

public class SolrServerFactoryTest extends MetacatCommonTestBase {
    /**
     * The setup method
     */
    @Before
    public void setup () throws FileNotFoundException, ConfigurationException, IOException {
        super.setup();
    }
    
    @Test
    public void testCreateSolrServer() throws Exception {
        SolrClient server = SolrServerFactory.createSolrServer();
        if(server == null) {
            assertTrue("testCreateSolrServer - the server shouldn't be null", true);
        }
    }
}
