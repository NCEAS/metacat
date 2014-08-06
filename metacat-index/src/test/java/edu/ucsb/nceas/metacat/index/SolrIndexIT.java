package edu.ucsb.nceas.metacat.index;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import edu.ucsb.nceas.metacat.common.SolrServerFactory;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.SolrCore;
import org.apache.solr.request.LocalSolrQueryRequest;
import org.apache.solr.response.SolrQueryResponse;
import org.apache.solr.response.XMLResponseWriter;
import org.apache.solr.servlet.SolrRequestParsers;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v2.SystemMetadata;
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
    
	private String annotation_id = "http://doi.org/annotation.1.1";
	private static final String ANNOTATION_SYSTEM_META_FILE_PATH = "src/test/resources/annotation-system-meta-example.xml";
	private static final String AO_FILE_PATH = "src/test/resources/ao-example.rdf";;
	private static final String OA_FILE_PATH = "src/test/resources/oa-example.rdf";;

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
       //List<String> chain = null;
       Identifier pid = new Identifier();
       pid.setValue(id);
       solrIndex.update(pid, systemMetadata, EMLFILEPATH);
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
       /*obsoletes.add(id);
       obsoletes.add("tao");*/
       Identifier pid = new Identifier();
       pid.setValue(newId);
       solrIndex.update(pid, systemMetadata, EMLUPDATEFILEPATH);
       String result = doQuery(solrIndex.getSolrServer());
       assertTrue(result.contains("version1"));
       assertTrue(result.contains("version2"));
       
       // have to re-index the older version
       Identifier obsoletedPid = systemMetadata.getObsoletes();
       SystemMetadata obsoletedSystemMetadata = TypeMarshaller.unmarshalTypeFromFile(SystemMetadata.class, SYSTEMMETAFILEPATH);
       assertTrue(obsoletedSystemMetadata.getIdentifier().getValue().equals(obsoletedPid.getValue()));
       obsoletedSystemMetadata.setObsoletedBy(pid);
       solrIndex.update(obsoletedPid, obsoletedSystemMetadata, EMLFILEPATH);
       
       // old version should be marked as obsoleted and not returned
       result = doQuery(solrIndex.getSolrServer(), "&fq=-obsoletedBy:*");
       assertTrue(!result.contains("version1"));
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
       /*ArrayList<String> obsoletes = new ArrayList<String>();
       obsoletes.add(id);
       obsoletes.add("tao");*/
       Identifier pid = new Identifier();
       pid.setValue(newId);
       solrIndex.update(pid, systemMetadata, EMLUPDATEFILEPATH);
       String result = doQuery(solrIndex.getSolrServer());
       assertTrue(result.contains("version1"));
       assertTrue(!result.contains("version2"));
    }
    
    
    /**
     * Test building index for dynamic fields.
     */
    @Test
    public void testDynamicFields() throws Exception {
    	
       SystemMetadata systemMetadata = TypeMarshaller.unmarshalTypeFromFile(SystemMetadata.class, SYSTEMMETAFILEPATH);
       Identifier pid = new Identifier();
       pid.setValue(id);
       solrIndex.update(pid, systemMetadata, EMLFILEPATH);
       String result = doQuery(solrIndex.getSolrServer());
       List<String> ids = solrIndex.getSolrIds();
       boolean foundId = false;
       for(String identifiers :ids) {
           if (id.equals(identifiers)) {
               foundId = true;
           }
       }
       assertTrue(foundId);
       assertTrue(result.contains("version1"));
       
       // augment with the dynamic field
       String fieldName = "test_count_i";
       Map<String, List<Object>> fields = new HashMap<String, List<Object>>();
       List<Object> values = new ArrayList<Object>();
       values.add(6);
       fields.put(fieldName, values);
       solrIndex.insertFields(pid, fields);
       result = doQuery(solrIndex.getSolrServer(), "&fq=" + fieldName + ":[0 TO 5]");
       assertFalse(result.contains(id));
       result = doQuery(solrIndex.getSolrServer(), "&fq=" + fieldName + ":[6 TO 6]");
       assertTrue(result.contains(id));
       
       // now update the value
       values.clear();
       values.add(7);
       fields.put(fieldName, values);
       solrIndex.insertFields(pid, fields);
       result = doQuery(solrIndex.getSolrServer(), "&fq=" + fieldName + ":[7 TO 7]");
       assertTrue(result.contains(id));
       
    }
    
    /**
     * Test building index for annotation using OpenAnnotation.
     */
    @Test
    public void testOpenAnnotation() throws Exception {
    	
       SystemMetadata systemMetadata = TypeMarshaller.unmarshalTypeFromFile(SystemMetadata.class, SYSTEMMETAFILEPATH);
       Identifier pid = new Identifier();
       pid.setValue(id);
       DistributedMapsFactory.getSystemMetadataMap().put(pid, systemMetadata);
       solrIndex.update(pid, systemMetadata, EMLFILEPATH);
       String result = doQuery(solrIndex.getSolrServer());
       List<String> ids = solrIndex.getSolrIds();
       boolean foundId = false;
       for(String identifiers :ids) {
           if (id.equals(identifiers)) {
               foundId = true;
           }
       }
       assertTrue(foundId);
       assertTrue(result.contains("version1"));
       
       // augment with the dynamic field
       SystemMetadata annotationSystemMetadata = TypeMarshaller.unmarshalTypeFromFile(SystemMetadata.class, ANNOTATION_SYSTEM_META_FILE_PATH);
       Identifier annotationPid = new Identifier();
       annotationPid.setValue(annotation_id);
       DistributedMapsFactory.getSystemMetadataMap().put(annotationPid, annotationSystemMetadata);
       solrIndex.update(annotationPid, annotationSystemMetadata, OA_FILE_PATH);
       String annotationResult = doQuery(solrIndex.getSolrServer(), "&fq=standard_sm:\"http://ecoinformatics.org/oboe/oboe.1.0/oboe-standards.owl#Gram\"");
       assertTrue(annotationResult.contains(pid.getValue()));
       assertTrue(annotationResult.contains("http://ecoinformatics.org/oboe/oboe.1.0/oboe-standards.owl#Gram"));

       // check that it contains the creator annotation as well
       assertTrue(annotationResult.contains("creator_sm"));
       assertTrue(annotationResult.contains("http://sandbox-1.orcid.org/0000-0003-2141-4459"));

    }
    
    /**
     * Do query - with no additional params
     */
    public static String doQuery(SolrServer server)
                    throws SolrServerException {
    	return doQuery(server, null);
    }
    
    /**
     * Do query, allowing additional parameters
     */
    public static String doQuery(SolrServer server, String moreParams)
                    throws SolrServerException {
                StringBuffer request = new StringBuffer();
                request.append("q=" + "*:*");
                if (moreParams != null) {
                    request.append(moreParams);
                }
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
    
    /**
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

