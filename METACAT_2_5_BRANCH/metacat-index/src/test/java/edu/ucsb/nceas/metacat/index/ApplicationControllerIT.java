package edu.ucsb.nceas.metacat.index;

import static org.junit.Assert.assertTrue;

import java.util.List;

import org.dataone.cn.indexer.parser.IDocumentSubprocessor;
import org.dataone.cn.indexer.parser.ScienceMetadataDocumentSubprocessor;
import org.junit.Test;

public class ApplicationControllerIT {
    
    /**
     * Test lookup from default properties file
     */
    @Test
    public void testConstructor() throws Exception {
        String springConfigFile = "/index-processor-context.xml";
        String metacatPropertyFile = null; //in this test, we use the test.properties file rather than metacat.properties file. so set it to be null.
        ApplicationController controller = new ApplicationController(springConfigFile, metacatPropertyFile);
        controller.initialize();
        List<SolrIndex> list = controller.getSolrIndexes();
        assertTrue(list.size() == 1);
        SolrIndex[] solrIndexesarray = list.toArray(new SolrIndex[list.size()]);
        SolrIndex index = solrIndexesarray[0];
        List<IDocumentSubprocessor> subprocessors = index.getSubprocessors();
        IDocumentSubprocessor[] subprocessorArray = subprocessors.toArray(new IDocumentSubprocessor[subprocessors.size()]);
        assertTrue(subprocessorArray[0] instanceof ScienceMetadataDocumentSubprocessor);
    }
    
}
