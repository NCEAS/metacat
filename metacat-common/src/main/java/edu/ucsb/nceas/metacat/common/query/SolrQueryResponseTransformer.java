/**
 *  '$RCSfile$'
 *    Purpose: A class that gets Accession Number, check for uniqueness
 *             and register it into db
 *  Copyright: 2000 Regents of the University of California and the
 *             National Center for Ecological Analysis and Synthesis
 *    Authors: Jivka Bojilova, Matt Jones
 *
 *   '$Author: tao $'
 *     '$Date: 2013-04-19 17:47:14 -0700 (Fri, 19 Apr 2013) $'
 * '$Revision: 7595 $'
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package edu.ucsb.nceas.metacat.common.query;

import java.io.ByteArrayInputStream;
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
