package edu.ucsb.nceas.metacat.index.resourcemap;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.dataone.cn.indexer.parser.IDocumentSubprocessor;
import org.dataone.cn.indexer.solrhttp.SolrDoc;
import org.dataone.cn.indexer.solrhttp.SolrElementField;
import org.dataone.configuration.Settings;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

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
        Document doc = getResourceMapDoc();
        System.out.println("the lenght of child is "+doc.getChildNodes().getLength());
        System.out.println("the name of the childs is "+doc.getChildNodes().item(0).getLocalName());
        Node child = doc.getChildNodes().item(0);
        List<SolrElementField> sysSolrFields = new ArrayList<SolrElementField>();
        SolrDoc indexDocument = new SolrDoc(sysSolrFields);
        Map<String, SolrDoc> docs = new HashMap<String, SolrDoc>();
        docs.put(id, indexDocument);
        ResourceMapSubprocessor processor = getResourceMapSubprocessor();
        try {
            processor.processDocument(id, docs, doc);
            assertTrue("ResourceMapProcessor should throw an exception because the resource map has a component which doesn't exist.", false);
        } catch (Exception e) {
            assertTrue("ResourceMapProcessor should throw a ResourceMapException because the resource map has a component which doesn't exist.", e instanceof ResourceMapException);
            //System.out.println("the message is "+e.getMessage());
        }
    }
    
    /*
     * Get the document format of the test resource map file 
     */
    private Document getResourceMapDoc() throws Exception{
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setNamespaceAware(true);
        DocumentBuilder builder = documentBuilderFactory.newDocumentBuilder();
        File file = new File("src/test/resources/resorcemap-example");
        Document doc = builder.parse(new FileInputStream(file));
        return doc;
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
