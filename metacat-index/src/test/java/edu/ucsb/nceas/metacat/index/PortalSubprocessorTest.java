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

/**
 *
 */
public class PortalSubprocessorTest {

    /**
     * Attempt to parse example 'portal' documents using the indexer PortalSubProcessor. See 'application-context-portals.xml'
     * for details on the indexer beans that are configured for parsing these type of documents.
     * @throws Exception something bad happened
     */
    @Test
    public void testProcessDocument() throws Exception {
        String id = "urn:uuid:349aa330-4645-4dab-a02d-3bf950cf708i";

        // Three different portal documents will be processed
        ArrayList<String> portalFiles = new ArrayList<String>();
        portalFiles.add("src/test/resources/collection/collection-example.xml");
        portalFiles.add("src/test/resources/collection/portal-example-full.xml");
        portalFiles.add("src/test/resources/collection/portal-example-seriesId.xml");
        portalFiles.add("src/test/resources/collection/portal-example-sasap.xml");
        portalFiles.add("src/test/resources/collection/portal-example-multiple-pids.xml");
        portalFiles.add("src/test/resources/collection/portal-example-only-pids.xml");
        portalFiles.add("src/test/resources/collection/portal-example-multiple-fields-values.xml");
        // The resulting 'collectionQuery' field will be compared to known values
        ArrayList<String> collectionQueryResultFiles = new ArrayList<String>();
        collectionQueryResultFiles.add("src/test/resources/collection/collectionQuery-result-example.txt");
        collectionQueryResultFiles.add("src/test/resources/collection/collectionQuery-result-example-full.txt");
        collectionQueryResultFiles.add("src/test/resources/collection/collectionQuery-result-example-seriesId.txt");
        collectionQueryResultFiles.add("src/test/resources/collection/collectionQuery-result-example-sasap.txt");
        collectionQueryResultFiles.add("src/test/resources/collection/collectionQuery-result-example-multiple-pids.txt");
        collectionQueryResultFiles.add("src/test/resources/collection/collectionQuery-result-example-only-pids.txt");
        collectionQueryResultFiles.add("src/test/resources/collection/collectionQuery-result-example-multiple-fields-values.txt");

        // Also test that the title is properly added and retrievable
        ArrayList<String> portalNames = new ArrayList<String>();
        portalNames.add("My saved search");
        portalNames.add("My Portal");
        portalNames.add("Another test portal");
        portalNames.add("Lauren's test project - updated");
        portalNames.add("Multiple pids");
        portalNames.add("Only pids");
        portalNames.add("Multiple fields, multiple values");

        for(int i=0; i < portalFiles.size(); i++) {
            String collectionQuery = null;
            InputStream is = getPortalDoc(portalFiles.get(i));
            System.out.println("Processing documment: " + portalFiles.get(i));
            List<SolrElementField> sysSolrFields = new ArrayList<SolrElementField>();
            SolrDoc indexDocument = new SolrDoc(sysSolrFields);
            Map<String, SolrDoc> docs = new HashMap<String, SolrDoc>();
            docs.put(id, indexDocument);

            // Process the document, creating a Solr document from the input XML document
            IDocumentSubprocessor processor = getPortalSubprocessor();

            String queryResult = null;
            try {
                // Read in the query string that the processor should create. This is read in
                // from disk so that we don't have to bother with special character escaping.
                File file = new File(collectionQueryResultFiles.get(i));
                collectionQuery = FileUtils.readFileToString(file);
                docs = processor.processDocument(id, docs, is);
                // Extract the processed document we just created
                SolrDoc myDoc = docs.get("urn:uuid:349aa330-4645-4dab-a02d-3bf950cf708i");
                // Extract fields and check the values
                String title = myDoc.getField("title").getValue();
                String queryStr = myDoc.getField("collectionQuery").getValue();
                queryStr = queryStr.trim();
                System.out.println("query field value:  " + "\"" + queryStr + "\"");

                // Did the index sub processor correctly extract the 'title' field from the portal document?
                assertTrue("The portalSubprocessor correctly build the document with the correct value in the title field.", title.equalsIgnoreCase(portalNames.get(i)));
                // Did the index sub processor correctly extract the 'collectionQuery' field from the portal document?
                assertTrue("The portalSubprocessor correctly built the document with the correct value in the \"collectionQuery\" field.", queryStr.equalsIgnoreCase(collectionQuery.trim()));

            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }

    /**
     * Get the document to test with the portal processor
     * @param filename the filename of the document to test
     * @return the document to test with the portal subprocessor
     * @throws Exception
     */
    private InputStream getPortalDoc(String filename) throws Exception{
        File file = new File(filename);
        InputStream is = new FileInputStream(file);
        return is;
    }

    /**
     * Search all subprocessors and return the portal subprocessor
     * @return the portal subprocessor
     * @throws Exception
     */
    private IDocumentSubprocessor getPortalSubprocessor() throws Exception {
        ScienceMetadataDocumentSubprocessor portalSubprocessor = null;
        SolrIndex solrIndex = SolrIndexIT.generateSolrIndex();
        List<IDocumentSubprocessor> processors = solrIndex.getSubprocessors();
        for(IDocumentSubprocessor processor : processors) {
            if(processor.canProcess("https://purl.dataone.org/portals-1.0.0")) {
                return processor;
            }
        }
        return null;
    }
}
