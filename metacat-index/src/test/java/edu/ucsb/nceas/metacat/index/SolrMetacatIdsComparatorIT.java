package edu.ucsb.nceas.metacat.index;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.servlet.SolrRequestParsers;
import org.dataone.cn.indexer.solrhttp.SolrDoc;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import edu.ucsb.nceas.metacat.common.SolrServerFactory;
import edu.ucsb.nceas.metacat.index.resourcemap.ResourceMapSubprocessor;

public class SolrMetacatIdsComparatorIT {
    
  
    private static final String metacatIDFileName = "ids";
    private static final String NOTINSOLR = "not_in_solr_but_in_metacat_ids";
    private static final String NUMBEROFIDS = "number_of_ids_in_solr";
    //private static final String NOTINMETACAT = "in_solr_but_not_in_metacat_ids";
    private File metacatIdsFile = null;
    private File notInSolrFile = null;
    private File notInMetacatFile = null;
    private File numberOfIdsFile = null;
   
    
    @Before
    public void setUp() throws Exception {    
        /*notInMetacatFile = new File(NOTINMETACAT);
        if(notInMetacatFile.exists()) {
            notInMetacatFile.delete();
        }
        notInMetacatFile.createNewFile();*/
    }
    
    
    /**
     * Figure out ids which have been indexed.
     */
    @Test
    @Ignore
    public void figureIdsNotIndexed() throws Exception {
        metacatIdsFile = new File( metacatIDFileName);
        notInSolrFile = new File(NOTINSOLR);
        if(notInSolrFile.exists()) {
            notInSolrFile.delete();
        }
        notInSolrFile.createNewFile();
        List<String> metacatIds = FileUtils.readLines(metacatIdsFile, "UTF-8");
        boolean appending = true;
        if(metacatIds != null) {
            for(String id : metacatIds) {
                //String id = metacatIds.get(0);
                if(id != null && !id.trim().equals("")) {
                    SolrDoc doc = ResourceMapSubprocessor.getSolrDoc(id);
                    if(doc == null) {
                        List<String> line = new ArrayList<String>();
                        line.add(id);
                        FileUtils.writeLines(notInSolrFile, line, appending);
                    } 
                }
            }
        }
    }
    
    /**
     * Figure out ids which have been indexed.
     */
    @Test
    @Ignore
    public void getNumberOfIdsInSolr() throws Exception {
        numberOfIdsFile = new File(NUMBEROFIDS);
        if(numberOfIdsFile.exists()) {
            numberOfIdsFile.delete();
        }
        numberOfIdsFile.createNewFile();
        String query = "q=*:*";
        SolrParams solrParams = SolrRequestParsers.parseQueryString(query);
        SolrServer solrServer = SolrServerFactory.createSolrServer();
        QueryResponse response = solrServer.query(solrParams);
        SolrDocumentList list = response.getResults();
        long number = list.getNumFound();
        FileUtils.writeStringToFile(numberOfIdsFile, (new Long(number)).toString());
    }
}
