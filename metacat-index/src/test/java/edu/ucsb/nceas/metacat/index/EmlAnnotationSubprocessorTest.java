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
import org.dataone.cn.indexer.parser.ISolrField;
import org.junit.Test;
import org.dataone.cn.indexer.annotation.EmlAnnotationSubprocessor;

import edu.ucsb.nceas.metacat.index.SolrIndex;
import edu.ucsb.nceas.metacat.index.SolrIndexIT;

public class EmlAnnotationSubprocessorTest {

    /**
     * This is a basic smoke test of the EmlAnnotationSubprocessor and its use of the
     * OntologyModelService it uses to expand annotated concepts with superclasses. A few more tests
     * are present in the d1_cn_index_processor where the code for these two classes exists.
     *
     * This test class was copied and modified from MetacatRdfXmlSubprocesstest.
     *
     * @throws Exception
     */

    @Test
    public void testProcessDocument() throws Exception {
        String id = "eml-annotation-scimeta";
        InputStream is = getEmlAnnotationDoc();

        List<SolrElementField> sysSolrFields = new ArrayList<SolrElementField>();
        SolrElementField idField = new SolrElementField();
        idField.setName("id");
        idField.setValue(id);
        sysSolrFields.add(idField);
        SolrDoc indexDocument = new SolrDoc(sysSolrFields);

        Map<String, SolrDoc> docs = new HashMap<String, SolrDoc>();
        docs.put(id, indexDocument);
        EmlAnnotationSubprocessor processor = getEmlAnnotationSubprocessor();

        try {
            Map<String, SolrDoc> result = processor.processDocument(id, docs, is);
            Set<String> ids = result.keySet();

            for(String newId : ids) {
                SolrDoc resultSolrDoc = result.get(newId);

                assertTrue(newId == "eml-annotation-scimeta");

                List<String> values = resultSolrDoc.getAllFieldValues("sem_annotation");
                assertTrue(values.contains("http://purl.dataone.org/odo/ECSO_00000536"));
                assertTrue(values.contains("http://purl.dataone.org/odo/ECSO_00000514"));
                assertTrue(values.contains("http://purl.dataone.org/odo/ECSO_00000513"));
                assertTrue(values.contains("http://ecoinformatics.org/oboe/oboe.1.2/oboe-core.owl#MeasurementType"));
                assertTrue(values.contains("http://purl.dataone.org/odo/ECSO_00001105"));
                assertTrue(values.contains("http://purl.dataone.org/odo/ECSO_00000531"));
                assertTrue(values.contains("http://purl.dataone.org/odo/ECSO_00001143"));

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                resultSolrDoc.serialize(baos, "UTF-8");
                System.out.println("after process, the solr doc is \n"+baos.toString());
            }

        } catch (Exception e) {
            e.printStackTrace();
            assertTrue("It shouldn't throw an exception.", false);
        }
    }

    private InputStream getEmlAnnotationDoc() throws Exception{
    	File file = new File("src/test/resources/eml-annotation-example.xml");
        InputStream is = new FileInputStream(file);
        return is;
    }

    private EmlAnnotationSubprocessor getEmlAnnotationSubprocessor() throws Exception {
        EmlAnnotationSubprocessor emlAnnotationSubprocessor = null;
        SolrIndex solrIndex = SolrIndexIT.generateSolrIndex();
        List<IDocumentSubprocessor> processors = solrIndex.getSubprocessors();
        for(IDocumentSubprocessor processor : processors) {
            if(processor instanceof EmlAnnotationSubprocessor) {
                emlAnnotationSubprocessor = (EmlAnnotationSubprocessor) processor;
            }
        }
        return emlAnnotationSubprocessor;
    }
}
