package edu.ucsb.nceas.metacat.index;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.*;
import org.dataone.cn.indexer.parser.IDocumentSubprocessor;
import org.dataone.cn.indexer.parser.ScienceMetadataDocumentSubprocessor;
import org.dataone.cn.indexer.solrhttp.SolrDoc;
import org.dataone.cn.indexer.solrhttp.SolrElementField;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class PortalSubprocessorIT {

    /**
     * Attempt to parse a 'portal' document using the indexer. See 'application-context-portals.xml'
     * for details on the indexer beans that are configured for parsing these type of documents.
     * @throws Exception something bad happened
     */
    @Test
    public void testProcessDocument() throws Exception {
        String id = "urn:uuid:349aa330-4645-4dab-a02d-3bf950cf708i";
        String memberQuery = null;
        InputStream is = getPortalDoc();
        List<SolrElementField> sysSolrFields = new ArrayList<SolrElementField>();
        SolrDoc indexDocument = new SolrDoc(sysSolrFields);
        Map<String, SolrDoc> docs = new HashMap<String, SolrDoc>();
        docs.put(id, indexDocument);
        IDocumentSubprocessor processor = getPortalSubprocessor();
        try {
            // Read in the query string that the processor should create. This is read in
            // from disk so that we don't have to bother with special character escaping.
            File file = new File("src/test/resources/portal-memberQuery.txt");
            memberQuery = FileUtils.readFileToString(file);

            docs = processor.processDocument(id, docs, is);
            // Extract the processed document we just created
            SolrDoc myDoc = docs.get("urn:uuid:349aa330-4645-4dab-a02d-3bf950cf708i");
            // Extract fields and check the values
            String title = myDoc.getField("title").getValue();
            String queryStr = myDoc.getField("memberQuery").getValue();

            //System.out.println("query field value: " + myDoc.getField("memberQuery").getValue());

            // Did the index sub processor correctly extract the 'title' field from the portal document?
            assertTrue("The portalSubprocessor correctly build the document with the correct value in the title field.", title.equalsIgnoreCase("My Portal"));

            // Did the index sub processor correctly extract the 'memberQuery' field from the portal document?
            assertTrue("The portalSubprocessor correctly built the document with the correct value in the \"memberQuery\" field.", queryStr.equalsIgnoreCase(memberQuery));

        } catch (Exception e) {
            System.out.println("Error: " +e.getMessage());
        }
    }

    /*
     * Get the document format of the test resource map file
     */
    private InputStream getPortalDoc() throws Exception{
        File file = new File("src/test/resources/portal-example-full.xml");
        InputStream is = new FileInputStream(file);
        return is;
    }

    /*
     * Get the ResourceMapSubprocessor
     */
    private IDocumentSubprocessor getPortalSubprocessor() throws Exception {
        ScienceMetadataDocumentSubprocessor portalSubprocessor = null;
        SolrIndex solrIndex = SolrIndexIT.generateSolrIndex();
        List<IDocumentSubprocessor> processors = solrIndex.getSubprocessors();
        for(IDocumentSubprocessor processor : processors) {
            if(processor.canProcess("https://purl.dataone.org/portals-1.0.0")) {
                //System.out.println("found processor..." + processor.toString());
                return processor;
            }
        }
        return null;
    }
}
