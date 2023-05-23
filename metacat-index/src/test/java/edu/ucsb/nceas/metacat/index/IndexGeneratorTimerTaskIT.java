package edu.ucsb.nceas.metacat.index;

import java.util.List;

import org.junit.Test;

public class IndexGeneratorTimerTaskIT {
    /**
     * Test building index for an insert.
     */
    @Test
    public void testGenerateAll() throws Exception {
        SolrIndex solrIndex = generateSolrIndex();
        SystemMetadataEventListener systeMetaListener = new SystemMetadataEventListener(solrIndex);
        systeMetaListener.run();
        IndexGeneratorTimerTask generator = new IndexGeneratorTimerTask();
        generator.indexAll();
        String result = SolrIndexIT.doQuery(solrIndex.getSolrServer());
        systeMetaListener.stop();
    }
    
    private SolrIndex generateSolrIndex() throws Exception {
        String springConfigFile = "/index-processor-context.xml";
        //in this test, we use the test.properties file, don't pass the metacat properties files.
        ApplicationController controller = new ApplicationController(springConfigFile);
        controller.initialize();
        List<SolrIndex> list = controller.getSolrIndexes();
        SolrIndex[] solrIndexesarray = list.toArray(new SolrIndex[list.size()]);
        SolrIndex index = solrIndexesarray[0];
        //SolrServer solrServer = SolrServerFactory.createSolrServer();
        //index.setSolrServer(solrServer);
        return index;
    }
}
