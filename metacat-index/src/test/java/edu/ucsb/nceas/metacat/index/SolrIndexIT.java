package edu.ucsb.nceas.metacat.index;

import static org.junit.Assert.assertTrue;

import edu.ucsb.nceas.metacat.common.SolrServerFactory;
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

public class SolrIndexIT  {
    
    private static final String SYSTEMMETAFILEPATH = "src/test/resources/eml-system-meta-example.xml";
    private static final String EMLFILEPATH = "src/test/resources/eml-example.xml";
    private static final String SYSTEMMETAUPDATEFILEPATH = "src/test/resources/eml-updating-system-meta-example.xml";
    private static final String EMLUPDATEFILEPATH = "src/test/resources/eml-updating-example.xml";
    private static final String SYSTEMMETAARCHIVEFILEPATH = "src/test/resources/eml-archive-system-meta-example.xml";
    private static final String id = "urn:uuid:606a19dd-b531-4bf4-b5a5-6d06c3d39098";
    private static final String newId = "urn:uuid:606a19dd-b531-4bf4-b5a5-6d06c3d39099";
    
	private SolrIndex solrIndex = null;
    
    
    @Before
    public void setUp() throws Exception {
            solrIndex  = generateSolrIndex();
    }
    
    public static SolrIndex generateSolrIndex() throws Exception {
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
    
    /**
     * Test building index for an insert.
     */
    @Test
    public void testInsert() throws Exception {
    	
    	
       //InputStream systemInputStream = new FileInputStream(new File(SYSTEMMETAFILEPATH));
       SystemMetadata systemMetadata = TypeMarshaller.unmarshalTypeFromFile(SystemMetadata.class, SYSTEMMETAFILEPATH);
       InputStream emlInputStream = new FileInputStream(new File(EMLFILEPATH)); 
       List<String> chain = null;
       solrIndex.update(id, chain, systemMetadata, emlInputStream);
       String result = doQuery(solrIndex.getSolrServer());
       List<String> ids = solrIndex.getSolrIds();
       //assertTrue(ids.size() == 1);
       boolean foundId = false;
       for(String identifiers :ids) {
           if(id.equals(identifiers)) {
               foundId = true;
           }
       }
       assertTrue(foundId);
       assertTrue(result.contains("version1"));
    	
    }
    
    /**
     * Test building index for an insert.
     */
    @Test
    public void testUpdate() throws Exception {
       //InputStream systemInputStream = new FileInputStream(new File(SYSTEMMETAFILEPATH));
       SystemMetadata systemMetadata = TypeMarshaller.unmarshalTypeFromFile(SystemMetadata.class, SYSTEMMETAUPDATEFILEPATH);
       InputStream emlInputStream = new FileInputStream(new File(EMLUPDATEFILEPATH));  
       ArrayList<String> obsoletes = new ArrayList<String>();
       obsoletes.add(id);
       obsoletes.add("tao");
       solrIndex.update(newId, obsoletes, systemMetadata, emlInputStream);
       String result = doQuery(solrIndex.getSolrServer());
       assertTrue(result.contains("version2"));
    }
    
    /**
     * Test building index for an insert.
     */
    @Test
    public void testArchive() throws Exception {
       SolrIndex solrIndex = generateSolrIndex();
       //InputStream systemInputStream = new FileInputStream(new File(SYSTEMMETAFILEPATH));
       //System metadata's archive is true.
       SystemMetadata systemMetadata = TypeMarshaller.unmarshalTypeFromFile(SystemMetadata.class, SYSTEMMETAARCHIVEFILEPATH);
       InputStream emlInputStream = new FileInputStream(new File(EMLUPDATEFILEPATH));    
       ArrayList<String> obsoletes = new ArrayList<String>();
       obsoletes.add(id);
       obsoletes.add("tao");
       solrIndex.update(newId, obsoletes, systemMetadata, emlInputStream);
       String result = doQuery(solrIndex.getSolrServer());
       assertTrue(!result.contains("version1"));
       assertTrue(!result.contains("version2"));
    }
    
    
    /*
     * Do query
     */
    public static String doQuery(SolrServer server)
                    throws SolrServerException {
                StringBuffer request = new StringBuffer();
                request.append("q=" + "*:*");
                SolrParams solrParams = SolrRequestParsers.parseQueryString(request
                        .toString());
                QueryResponse reponse = server.query(solrParams);
                String result = toXML(solrParams, reponse);
                System.out.println("**************************************************************************");
                System.out.println("The query result:\n");
                System.out.println(result);
                System.out.println("**************************************************************************");
                return result;
    }
    
    /*
     * Transform the query response to the xml format.
     */
    private static String toXML(SolrParams request, QueryResponse response) {
        XMLResponseWriter xmlWriter = new XMLResponseWriter();
        Writer w = new StringWriter();
        SolrQueryResponse sResponse = new SolrQueryResponse();
        sResponse.setAllValues(response.getResponse());
        try {
            SolrCore core = null;
            CoreContainer container = SolrServerFactory.getCoreContainer();
            core = container.getCore("collection1");
            xmlWriter.write(w, new LocalSolrQueryRequest(core, request), sResponse);
        } catch (Exception e) {
            throw new RuntimeException("Unable to convert Solr response into XML", e);
        }
        return w.toString();
    }


    
    
}

