/**
 *  Copyright: 2013 Regents of the University of California and the
 *             National Center for Ecological Analysis and Synthesis
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

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.XML;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.SolrCore;
import org.apache.solr.schema.IndexSchema;
import org.apache.solr.schema.SchemaField;
import org.apache.solr.servlet.SolrRequestParsers;
import org.dataone.service.exceptions.NotFound;
import org.dataone.service.exceptions.NotImplemented;
import org.dataone.service.exceptions.UnsupportedType;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Subject;
import org.xml.sax.SAXException;

import edu.ucsb.nceas.metacat.common.query.SolrQueryResponseTransformer;


/**
 *The query service of the embedded solr server.
 * @author tao
 *
 */
public class EmbeddedSolrQueryService extends SolrQueryService {
    private EmbeddedSolrServer solrServer = null;
    private CoreContainer coreContainer = null;
    private String collectionName = null;
    private SolrCore solrCore = null;
    
  
    /**
     * Constructor.
     * @param solrServer
     * @param solrCore
     * @throws NotFound 
     */
    public EmbeddedSolrQueryService(EmbeddedSolrServer solrServer, CoreContainer coreContainer, String collectionName) throws NotFound {
        //System.out.println("+++++++++++++++++++++++++++++++++++= created embededsolrqueryservice.");
        if(solrServer == null) {
            throw new NullPointerException("EmbeddedSolrQueryService.constructor - the EmbeddedSolrServer parameter can't be null.");
        }
        if(coreContainer == null) {
            throw new NullPointerException("EmbeddedSolrQueryService.constructor - the CoreContainer parameter can't be null.");
        }
        if(collectionName == null || collectionName.trim().equals("")) {
            throw new NullPointerException("EmbeddedSolrQueryService.constructor - the name of Collection parameter can't be null or empty.");
        }
        this.solrServer = solrServer;
        this.coreContainer = coreContainer;
        this.collectionName = collectionName;
        this.solrCore = this.coreContainer.getCore(collectionName);
        if(solrCore == null) {
            throw new NotFound("0000","EmbeddedSolrQueryService.constructor - There is no SolrCore named "+collectionName+".");
        }
        schema = solrCore.getLatestSchema();
        fieldMap = schema.getFields();
    }
    /**
     * Query the Solr server with specified query and user's identity. If the Subjects
     * is null, there will be no access rules for the query. This is the for the http solr server.
     * @param query the query string
     * @param subjects the user's identity which sent the query
     * @return the response
     * @throws Exception
     */
    /*public InputStream query(String query, Set<Subject>subjects) throws Exception {
        throw new NotImplemented("0000", "EmbeddSolrQueryService - the method of  query(String query, Set<Subject>subjects) is not for the EmbeddedSolrServer. We donot need to implemente it");
    }*/
    
    /**
     * Query the Solr server with specified query and user's identity. If the Subjects
     * is null, there will be no access rules for the query. This is for the embedded solr server.
     * @param query the query params. 
     * @param subjects the user's identity which sent the query
     * @param method  the method such as GET, POST and et al will be used in this query. This only works for the HTTP Solr server.
     * @return the response
     * @throws SAXException 
     * @throws IOException 
     * @throws ParserConfigurationException 
     * @throws SolrServerException 
     * @throws UnsupportedType 
     * @throws Exception
     */
    public  InputStream query(SolrParams query, Set<Subject>subjects, SolrRequest.METHOD method) throws ParserConfigurationException, IOException, SAXException, SolrServerException, UnsupportedType {
        InputStream inputStream = null;
        String wt = query.get(WT);
        query = appendAccessFilterParams(query, subjects);
        SolrQueryResponseTransformer solrTransformer = new SolrQueryResponseTransformer(solrCore);
        // handle normal and skin-based queries
        if (isSupportedWT(wt)) {
            // just handle as normal solr query
            // The statement of coreContainer.load() can be called now. Since the Embedded solr
            // service is for testing only. The change wouldn't cause any issue.
            QueryResponse response = solrServer.query(query);
            inputStream = solrTransformer.transformResults(query, response, wt);
        } else {
            throw new UnsupportedType("0000","EmbeddSolrQueryService.query - the wt type "+wt+" in the solr query is not supported");
        }
        return inputStream;
    }
    
    
    
    
    /**
     * Get the fields map of the index schema
     * @return the fields map (the field name is the key and the SchemaField is the value).
     */
    public  Map<String, SchemaField> getIndexSchemaFields() {
        return fieldMap;
    }
    
    /**
     * Get the list of the valid field name (moved the fields names of the CopyFieldTarget).
     * @return
     */
    public List<String> getValidSchemaField() {
        return super.getValidSchemaFields();
    }
    
    /**
     * Get the version of the solr server.
     * @return
     */
    public String getSolrServerVersion() {
        if( solrSpecVersion !=null ) {
            return solrSpecVersion;
        } else {
            Package p = SolrCore.class.getPackage();
            StringWriter tmp = new StringWriter();
            solrSpecVersion = p.getSpecificationVersion();
            if (null != solrSpecVersion) {
                try {
                    XML.escapeCharData(solrSpecVersion, tmp);
                    solrSpecVersion = tmp.toString();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (solrSpecVersion == null || solrSpecVersion.trim().equals("")) {
                solrSpecVersion = UNKNOWN;
            } 
            return solrSpecVersion;
        }
    }
    
    /**
     * If there is a solr doc for the given id.
     * @param id - the specified id.
     * @return true if there is a solr doc for this id.
     */
    public boolean hasSolrDoc(Identifier id) throws ParserConfigurationException, SolrServerException, IOException, SAXException{
    	boolean hasIt = false;
    	if(id != null && id.getValue() != null && !id.getValue().trim().equals("") ) {
    		SolrParams query = buildIdQuery(id.getValue());
    		coreContainer.reload(collectionName);
            QueryResponse response = solrServer.query(query);
            hasIt = hasResult(response);
    	}
    	return hasIt;
    }
    
    /**
     * Build a query for decide if the solr server has the id.
     * @param id
     * @return the query
     */
    public static SolrParams buildIdQuery(String id) {
    	    String query = "select?q=id:"+id;
    		query = query.replaceAll("\\+", "%2B");
            SolrParams solrParams = SolrRequestParsers.parseQueryString(query);
            return solrParams;
    }
    
    /**
     * Determine if the response has any solr documents.
     * @param response
     * @return true if it has at least one solr document;
     */
    public static boolean hasResult(QueryResponse response) {
    	boolean hasResult = false;
    	if(response != null) {
    		SolrDocumentList list = response.getResults();
    		if(list != null && list.getNumFound() >0) {
    			hasResult = true;
    		}
    	}
    	return hasResult;
    }
}
