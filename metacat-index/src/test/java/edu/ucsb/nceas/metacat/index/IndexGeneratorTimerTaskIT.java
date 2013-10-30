package edu.ucsb.nceas.metacat.index;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.SolrCore;
import org.apache.solr.request.LocalSolrQueryRequest;
import org.apache.solr.response.SolrQueryResponse;
import org.apache.solr.response.XMLResponseWriter;
import org.apache.solr.servlet.SolrRequestParsers;
import org.dataone.service.types.v1.SystemMetadata;
import org.dataone.service.util.TypeMarshaller;
import org.junit.Before;
import org.junit.Test;

public class IndexGeneratorTimerTaskIT {
    /**
     * Test building index for an insert.
     */
    @Test
    public void testGenerateAll() throws Exception {
        SolrIndex solrIndex = generateSolrIndex();
        SystemMetadataEventListener systeMetaListener = new SystemMetadataEventListener(solrIndex);
        systeMetaListener.start();
        IndexGeneratorTimerTask generator = new IndexGeneratorTimerTask(solrIndex);
        generator.indexAll();
        String result = SolrIndexIT.doQuery(solrIndex.getSolrServer());
        systeMetaListener.stop();
    }
    
    private SolrIndex generateSolrIndex() throws Exception {
        String springConfigFile = "/index-processor-context.xml";
        String metacatPropertyFile = null; //in this test, we use the test.properties file rather than metacat.properties file. so set it to be null.
        ApplicationController controller = new ApplicationController(springConfigFile, metacatPropertyFile);
        controller.initialize();
        List<SolrIndex> list = controller.getSolrIndexes();
        SolrIndex[] solrIndexesarray = list.toArray(new SolrIndex[list.size()]);
        SolrIndex index = solrIndexesarray[0];
        //SolrServer solrServer = SolrServerFactory.createSolrServer();
        //index.setSolrServer(solrServer);
        return index;
    }
}
