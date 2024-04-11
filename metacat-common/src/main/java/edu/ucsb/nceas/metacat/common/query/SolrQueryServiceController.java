package edu.ucsb.nceas.metacat.common.query;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;


import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.schema.IndexSchema;
import org.apache.solr.schema.SchemaField;
import org.dataone.service.exceptions.NotFound;
import org.dataone.service.exceptions.NotImplemented;
import org.dataone.service.exceptions.UnsupportedType;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Subject;
import org.xml.sax.SAXException;

import edu.ucsb.nceas.metacat.common.SolrServerFactory;

/**
 * A class to handle the solr query. It will call the SolrServerFactory to generate a SolrServer and
 * determine the type - the embedded or http solr server.
 * @author tao
 *
 */
public class SolrQueryServiceController {
    private static SolrQueryServiceController controller = null;
    private boolean isEmbeddedSolrServer = false;
    private EmbeddedSolrQueryService embeddedQueryService = null;
    private HttpSolrQueryService httpQueryService = null;
    
    /*
     * Private consctructor
     */
    private SolrQueryServiceController() throws UnsupportedType, ParserConfigurationException, IOException, SAXException, NotFound {
        SolrClient solrServer = SolrServerFactory.createSolrServer();
        if(solrServer instanceof EmbeddedSolrServer) {
            isEmbeddedSolrServer = true;
            EmbeddedSolrServer embeddedServer = (EmbeddedSolrServer) solrServer;
            CoreContainer coreContainer = SolrServerFactory.getCoreContainer();
            String collectionName = SolrServerFactory.getCollectionName();
            embeddedQueryService = new EmbeddedSolrQueryService(embeddedServer, coreContainer, collectionName);
        } else {
            isEmbeddedSolrServer = false;
            HttpSolrClient httpServer = (HttpSolrClient)solrServer;
            httpQueryService = new HttpSolrQueryService(httpServer);
        }
    }
    
    /**
     * Get the controller
     * @return
     * @throws UnsupportedType
     * @throws NotFound
     * @throws ParserConfigurationException
     * @throws IOException
     * @throws SAXException
     */
    public static SolrQueryServiceController getInstance() throws UnsupportedType, NotFound, ParserConfigurationException, IOException, SAXException {
        if(controller == null) {
            controller = new SolrQueryServiceController();
        }
        return controller;
    }
    
   
    
    /**
     * Query the Solr server with specified query string or SolrParams and the user's identity. 
     * @param query  query string. It is for the HttpSolrQueryService.
     * @param params the SolrParam. It is for the EmbeddedSolrQueryService.
     * @param subjects
     * @param method  the method such as GET, POST and et al will be used in this query. This only works for the HTTP Solr server.
     * @return
     * @throws NotImplemented
     * @throws NotFound
     * @throws IOException
     * @throws SolrServerException 
     * @throws SAXException 
     * @throws ParserConfigurationException 
     * @throws UnsupportedType 
     */
    public InputStream query(SolrParams params,Set<Subject> subjects, SolrRequest.METHOD method) throws NotImplemented, NotFound, IOException, UnsupportedType, ParserConfigurationException, SAXException, SolrServerException  {
        if(isEmbeddedSolrServer) {
            return embeddedQueryService.query(params, subjects, method);
        } else {
            return httpQueryService.query(params, subjects, method);
        }
      
    }
    
    /**
     * Get the spec version of the solr server
     * @return
     */
    public String getSolrSpecVersion() {
        if(isEmbeddedSolrServer) {
            return embeddedQueryService.getSolrServerVersion();
        } else {
            return httpQueryService.getSolrServerVersion();
        }
    }
    
    /**
     * Get the fields list of the index schema
     * @return
     * @throws SAXException 
     * @throws IOException 
     * @throws ParserConfigurationException 
     * @throws MalformedURLException 
     * @throws Exception
     */
    public  Map<String, SchemaField> getIndexSchemaFields() throws MalformedURLException, ParserConfigurationException, IOException, SAXException  {
        if(isEmbeddedSolrServer) {
            return embeddedQueryService.getIndexSchemaFields();
        } else {
            return httpQueryService.getIndexSchemaFields();
        }
    }

    /**
     * Access the SOLR index schema
     * @return
     * @throws SAXException 
     * @throws IOException 
     * @throws ParserConfigurationException 
     * @throws MalformedURLException 
     */
    public IndexSchema getSchema() throws MalformedURLException, ParserConfigurationException, IOException, SAXException {
        if(isEmbeddedSolrServer) {
            return embeddedQueryService.getSchema();
        } else{
            return httpQueryService.getSchema();
        }
       
    }
    
    /**
     * If the solr client was configured as an embedded solr server.
     * @return true if it is; false otherwise.
     */
    public boolean isEmbeddedSolrServer() {
    	return isEmbeddedSolrServer;
    }
    
    
    /**
     * If the solr server has at least one solr doc for the given id.
     * @param id
     * @return true if the server has at lease one solr doc; false otherwise.
     */
    /*public boolean hasSolrDoc(Identifier id) throws ParserConfigurationException, SolrServerException, IOException, SAXException{
    	 if(isEmbeddedSolrServer) {
             return embeddedQueryService.hasSolrDoc(id);
         } else{
             return httpQueryService.hasSolrDoc(id);
         }
    }*/

    
}
