package edu.ucsb.nceas.metacat.common.query;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.Writer;

import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.core.SolrCore;
import org.apache.solr.request.LocalSolrQueryRequest;
import org.apache.solr.response.QueryResponseWriter;
import org.apache.solr.response.SolrQueryResponse;

import edu.ucsb.nceas.metacat.common.query.stream.ContentTypeByteArrayInputStream;

/**
 * Transform the solr QueryReponse to the InputStream. Currently it works only for
 * The EmbeddedSolrServer.
 * @author tao
 *
 */
public class SolrQueryResponseTransformer {
    
    private SolrCore core = null;
    /**
     * Constructor
     * @param core
     */
    public SolrQueryResponseTransformer(SolrCore core) {
        this.core = core;
    }
    
    
    /**
     * Transform the query response the the inputstream.
     * @param request
     * @param response
     * @param wt
     * @return
     * @throws SolrServerException
     * @throws IOException
     */
    public InputStream transformResults(SolrParams request, QueryResponse response, String wt) throws SolrServerException, IOException {
        //InputStream stream = null;
        QueryResponseWriter writer = SolrQueryResponseWriterFactory.generateResponseWriter(wt);
        Writer results = new StringWriter();
        SolrQueryResponse sResponse = new SolrQueryResponse();
        sResponse.setAllValues(response.getResponse());
        writer.write(results, new LocalSolrQueryRequest(core, request), sResponse);
        ContentTypeByteArrayInputStream ctbais = new ContentTypeByteArrayInputStream(results.toString().getBytes("UTF-8"));
        ctbais.setContentType(SolrQueryResponseWriterFactory.getContentType(wt));
        return ctbais;
    }
}
