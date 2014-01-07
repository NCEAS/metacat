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
import java.net.MalformedURLException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.impl.CommonsHttpSolrServer;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.schema.IndexSchema;
import org.apache.solr.schema.SchemaField;
import org.dataone.service.exceptions.NotFound;
import org.dataone.service.exceptions.NotImplemented;
import org.dataone.service.exceptions.UnsupportedType;
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
        SolrServer solrServer = SolrServerFactory.createSolrServer();
        if(solrServer instanceof EmbeddedSolrServer) {
            isEmbeddedSolrServer = true;
            EmbeddedSolrServer embeddedServer = (EmbeddedSolrServer) solrServer;
            CoreContainer coreContainer = SolrServerFactory.getCoreContainer();
            String collectionName = SolrServerFactory.getCollectionName();
            embeddedQueryService = new EmbeddedSolrQueryService(embeddedServer, coreContainer, collectionName);
        } else {
            isEmbeddedSolrServer = false;
            CommonsHttpSolrServer httpServer = (CommonsHttpSolrServer)solrServer;
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
     * @return
     * @throws NotImplemented
     * @throws NotFound
     * @throws IOException
     * @throws SolrServerException 
     * @throws SAXException 
     * @throws ParserConfigurationException 
     * @throws UnsupportedType 
     */
    public InputStream query(SolrParams params,Set<Subject>subjects) throws NotImplemented, NotFound, IOException, UnsupportedType, ParserConfigurationException, SAXException, SolrServerException  {
        if(isEmbeddedSolrServer) {
            return embeddedQueryService.query(params, subjects);
        } else {
            return httpQueryService.query(params, subjects);
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

    
}
