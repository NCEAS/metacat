package edu.ucsb.nceas.metacat.common;

import static org.junit.Assert.fail;

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
    public void setup () throws ConfigurationException, IOException {
        super.setup();
    }

    @Test
    public void testCreateSolrServer() throws Exception {
        SolrClient server = SolrServerFactory.createSolrServer();
        if(server == null) {
            fail("testCreateSolrServer - the server shouldn't be null");
        }
    }
}
