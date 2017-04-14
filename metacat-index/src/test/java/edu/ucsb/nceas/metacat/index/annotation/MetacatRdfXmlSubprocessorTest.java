package edu.ucsb.nceas.metacat.index.annotation;

import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.dataone.cn.indexer.parser.IDocumentSubprocessor;
import org.dataone.cn.indexer.solrhttp.SolrDoc;
import org.dataone.cn.indexer.solrhttp.SolrElementField;
import org.junit.Test;
import org.dataone.cn.indexer.annotation.RdfXmlSubprocessor;

import edu.ucsb.nceas.metacat.index.SolrIndex;
import edu.ucsb.nceas.metacat.index.SolrIndexIT;

public class MetacatRdfXmlSubprocessorTest {
    
    /**
     * Test the case that the resource map contains a component which is not int the solr index server.
     * @throws Exception
     */
    @Test
    public void testProcessDocument() throws Exception {
        //Settings.getConfiguration().setProperty("dataone.hazelcast.configFilePath", "../lib/hazelcast.xml");
        String id = "resourceMap_urn:uuid:04780847-6082-455b-9831-22269c9ec0a6";
        InputStream is = getResourceMapDoc();
        List<SolrElementField> sysSolrFields = new ArrayList<SolrElementField>();
        SolrElementField idField = new SolrElementField();
        idField.setName("id");
        idField.setValue(id);
        sysSolrFields.add(idField);
        SolrDoc indexDocument = new SolrDoc(sysSolrFields);
        Map<String, SolrDoc> docs = new HashMap<String, SolrDoc>();
        docs.put(id, indexDocument);
        MetacatRdfXmlSubprocessor processor = getMetacatRdfXmlSubprocessor();
        
        try {
            Map<String, SolrDoc> result = processor.processDocument(id, docs, is);
            Set<String> ids = result.keySet();
            for(String newId: ids) {
                SolrDoc resultSolrDoc = result.get(newId);
                if(newId.equals("urn:uuid:2c20432e-116a-4085-b8d6-abfc4b2dada2")){
                    SolrElementField field1 = resultSolrDoc.getField("prov_wasDerivedFrom");
                    assertTrue(" the wasDerivedFrom value should be urn:uuid:621a115f-f1ed-4bef-bca5-f8741793a540. But the real value is "+field1.getValue(), field1.getValue().equals("urn:uuid:621a115f-f1ed-4bef-bca5-f8741793a540"));
                    SolrElementField field2 = resultSolrDoc.getField("prov_generatedByExecution");
                    assertTrue("the wasGeneratedBy value should be urn:uuid:9ceeaeb3-6ef3-4b1e-bc4d-96e299fab3a4. But the real value is"+field2.getValue(), field2.getValue().equals("urn:uuid:9ceeaeb3-6ef3-4b1e-bc4d-96e299fab3a4"));
                } else if (newId.equals("urn:uuid:9ceeaeb3-6ef3-4b1e-bc4d-96e299fab3a4")) {
                    SolrElementField field1 = resultSolrDoc.getField("prov_used");
                    //assertTrue(" the used value should be urn:uuid:621a115f-f1ed-4bef-bca5-f8741793a540. But the real value is "+field1.getValue(), field1.getValue().equals("urn:uuid:621a115f-f1ed-4bef-bca5-f8741793a540"));
                } else if (newId.equals("urn:uuid:621a115f-f1ed-4bef-bca5-f8741793a540")) {
                    List<String>list = resultSolrDoc.getAllFieldValues("prov_wasDerivedFrom");
                    assertTrue("The list must contain urn:uuid:ee635a61-c930-444d-8214-db65178a1a47", list.contains("urn:uuid:ee635a61-c930-444d-8214-db65178a1a47"));
                    assertTrue("The list must contain urn:uuid:23495598-d50b-4317-b0e1-05a9d9e52632", list.contains("urn:uuid:23495598-d50b-4317-b0e1-05a9d9e52632"));
                    assertTrue("The list must contain urn:uuid:urn:uuid:672ba6c5-8812-4c05-a324-246af172c67a", list.contains("urn:uuid:672ba6c5-8812-4c05-a324-246af172c67a"));
                    assertTrue("The list must contain urn:uuid:e534b2ab-3a1b-44ed-8a99-ce54a0487aea", list.contains("urn:uuid:e534b2ab-3a1b-44ed-8a99-ce54a0487aea"));
                    list = resultSolrDoc.getAllFieldValues("prov_generatedByExecution");
                    assertTrue("The list must contain urn:uuid:d8e46217-2650-42b2-896f-0f006b1a2d3b", list.contains("urn:uuid:d8e46217-2650-42b2-896f-0f006b1a2d3b"));
                    list = resultSolrDoc.getAllFieldValues("prov_hasDerivations");
                    assertTrue("The output must contain prov_hasDerivations urn:uuid:2c20432e-116a-4085-b8d6-abfc4b2dada2", list.contains("urn:uuid:2c20432e-116a-4085-b8d6-abfc4b2dada2"));
                } else if (newId.equals("urn:uuid:a0e104da-c925-4765-af60-29310de1b99a")) {
                    List<String>list = resultSolrDoc.getAllFieldValues("prov_generatedByExecution");
                    assertTrue("The list must contain urn:uuid:9ceeaeb3-6ef3-4b1e-bc4d-96e299fab3a4", list.contains("urn:uuid:9ceeaeb3-6ef3-4b1e-bc4d-96e299fab3a4"));
                }  else if (newId.equals("urn:uuid:d8e46217-2650-42b2-896f-0f006b1a2d3b")) {
                    List<String>list = resultSolrDoc.getAllFieldValues("prov_used");
                    /*assertTrue("The list must contain urn:uuid:ee635a61-c930-444d-8214-db65178a1a47", list.contains("urn:uuid:ee635a61-c930-444d-8214-db65178a1a47"));
                    assertTrue("The list must contain urn:uuid:23495598-d50b-4317-b0e1-05a9d9e52632", list.contains("urn:uuid:23495598-d50b-4317-b0e1-05a9d9e52632"));
                    assertTrue("The list must contain urn:uuid:672ba6c5-8812-4c05-a324-246af172c67a", list.contains("urn:uuid:672ba6c5-8812-4c05-a324-246af172c67a"));
                    assertTrue("The list must contain urn:uuid:e534b2ab-3a1b-44ed-8a99-ce54a0487aea", list.contains("urn:uuid:e534b2ab-3a1b-44ed-8a99-ce54a0487aea"));*/
                }
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                resultSolrDoc.serialize(baos, "UTF-8");
                System.out.println("after process, the solr doc is \n"+baos.toString());
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            assertTrue("It shouldn't throw an exception.", false);
        }
    }
    
    /*
     * Get the document format of the test resource map file 
     */
    private InputStream getResourceMapDoc() throws Exception{
    	File file = new File("src/test/resources/rdfxml-example.xml");
        InputStream is = new FileInputStream(file);
        return is;
    }
    
    /*
     * Get the ResourceMapSubprocessor
     */
    private MetacatRdfXmlSubprocessor getMetacatRdfXmlSubprocessor() throws Exception {
        MetacatRdfXmlSubprocessor resorceMapprocessor = null;
        SolrIndex solrIndex = SolrIndexIT.generateSolrIndex();
        List<IDocumentSubprocessor> processors = solrIndex.getSubprocessors();
        for(IDocumentSubprocessor processor : processors) {
            if(processor instanceof MetacatRdfXmlSubprocessor) {
                resorceMapprocessor = (MetacatRdfXmlSubprocessor) processor;
            }
        }
        return resorceMapprocessor;
    }
    
   
}
