package edu.ucsb.nceas.metacat.index.resourcemap;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.dataone.cn.indexer.parser.IDocumentSubprocessor;
import org.dataone.cn.indexer.solrhttp.SolrDoc;
import org.dataone.cn.indexer.solrhttp.SolrElementField;
import org.junit.Test;

import edu.ucsb.nceas.metacat.index.SolrIndex;
import edu.ucsb.nceas.metacat.index.SolrIndexIT;

public class ResourceMapSubprocessorIT {
    
    /**
     * Test the case that the resource map contains a component which is not int the solr index server.
     * @throws Exception
     */
    @Test
    public void testProcessDocument() throws Exception {
        //Settings.getConfiguration().setProperty("dataone.hazelcast.configFilePath", "../lib/hazelcast.xml");
        String id = "resourceMap_urn:uuid:04780847-6082-455b-9831-22269c9ec0a5";
        InputStream is = getResourceMapDoc();
        List<SolrElementField> sysSolrFields = new ArrayList<SolrElementField>();
        SolrDoc indexDocument = new SolrDoc(sysSolrFields);
        Map<String, SolrDoc> docs = new HashMap<String, SolrDoc>();
        docs.put(id, indexDocument);
        ResourceMapSubprocessor processor = getResourceMapSubprocessor();
        try {
            processor.processDocument(id, docs, is);
            assertTrue("ResourceMapProcessor should throw an exception because the resource map has a component which doesn't exist.", false);
        } catch (Exception e) {
            assertTrue("ResourceMapProcessor should throw a ResourceMapException because the resource map has a component which doesn't exist.", e instanceof ResourceMapException);
            //System.out.println("the message is "+e.getMessage());
        }
    }
    
    /*
     * Get the document format of the test resource map file 
     */
    private InputStream getResourceMapDoc() throws Exception{
    	File file = new File("src/test/resources/resorcemap-example");
        InputStream is = new FileInputStream(file);
        return is;
    }
    
    /*
     * Get the ResourceMapSubprocessor
     */
    private ResourceMapSubprocessor getResourceMapSubprocessor() throws Exception {
        ResourceMapSubprocessor resorceMapprocessor = null;
        SolrIndex solrIndex = SolrIndexIT.generateSolrIndex();
        List<IDocumentSubprocessor> processors = solrIndex.getSubprocessors();
        for(IDocumentSubprocessor processor : processors) {
            if(processor instanceof ResourceMapSubprocessor) {
                resorceMapprocessor = (ResourceMapSubprocessor) processor;
            }
        }
        return resorceMapprocessor;
    }
}
