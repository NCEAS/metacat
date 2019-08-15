package edu.ucsb.nceas.metacat.index.annotation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.hp.hpl.jena.ontology.Ontology;

import org.dataone.cn.indexer.parser.IDocumentSubprocessor;
import org.dataone.cn.indexer.solrhttp.SolrDoc;
import org.dataone.cn.indexer.solrhttp.SolrElementField;
import org.dataone.cn.indexer.parser.ISolrField;
import org.dataone.cn.indexer.parser.ISolrDataField;

import org.junit.Test;
import org.junit.Before;

import edu.ucsb.nceas.metacat.index.annotation.OntologyModelService;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.ContextConfiguration;
import edu.ucsb.nceas.metacat.index.SolrIndex;
import edu.ucsb.nceas.metacat.index.SolrIndexIT;

public class OntologyModelServiceTest {

    @Before
    public void setUp() {
        try {
            SolrIndexIT.generateSolrIndex();
        } catch (Exception e) {
            System.out.println("Failed to run SolrIndexIT.generateSolrIndex()");
        }
    }

    /**
     * Just a basic smoke test to make sure things are hooked up.
     * EMLAnnotationSubprocessorTest exercises this more
     */

    @Test
    public void testOntologyModelService() throws Exception {
        OntologyModelService service = OntologyModelService.getInstance();

        System.out.println("FOOBAR: " + service.getClass().getName());
        List<ISolrDataField> fieldList = service.getFieldList();
        assertEquals(1, fieldList.size());

        Map<String, String> altEntries = service.getAltEntryList();
        assertEquals(16, altEntries.size());
    }
}
