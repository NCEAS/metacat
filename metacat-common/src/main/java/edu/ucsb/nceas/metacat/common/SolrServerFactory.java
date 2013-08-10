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
package edu.ucsb.nceas.metacat.common;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.impl.CommonsHttpSolrServer;
import org.apache.solr.core.CoreContainer;
import org.dataone.configuration.Settings;
import org.dataone.service.exceptions.UnsupportedType;
import org.xml.sax.SAXException;

/**
 * A factory for creating SolrServer implementations based on
 * configuration in Settings.java
 * Options are as follows.
 * 
 * Embedded (default):
 * solr.server.classname=org.apache.solr.client.solrj.embedded.EmbeddedSolrServer
 * solr.homeDir=/path/to/solr/home
 * 
 * HTTP:
 * solr.server.classname=org.apache.solr.client.solrj.impl.CommonsHttpSolrServer
 * solr.endpoint=http://endpoint/to/solr/service
 * 
 * @author leinfelder + tao
 *
 */
public class SolrServerFactory {
	
    public static final String SOLR_HOME_PROPERTY_NAME = "solr.homeDir";
    public static final String SOLR_CONFIG_FILE_NAME_PROPERTY_NAME = "solr.configFileName";
    public static final String SOLR_COLLECTION_NAME_PROPERTY_NAME = "solr.collectionName";
    public static final String SOLR_SERVER_CLASSNAME_PROPERTY_NAME = "solr.server.classname";
    public static final String SOLR_ENPOINT_PROPERTY_NAME = "solr.endpoint";
    private static final String EMBEDDEDSERVERCLASS = "org.apache.solr.client.solrj.embedded.EmbeddedSolrServer";
    private static final String HTTPSERVERCLASS = "org.apache.solr.client.solrj.impl.CommonsHttpSolrServer";

	public static Log log = LogFactory.getLog(SolrServerFactory.class);
	
	private static CoreContainer coreContainer = null;
	private static String collectionName = null;
	private static SolrServer solrServer = null;
	//private static String solrServerBaseURL = null;

	public static SolrServer createSolrServer() throws ParserConfigurationException, IOException, SAXException, UnsupportedType   {
	    if(solrServer == null) {
	        String className = Settings.getConfiguration().getString(SOLR_SERVER_CLASSNAME_PROPERTY_NAME);
	        if (className != null && className.equals(EMBEDDEDSERVERCLASS)) {
	            generateEmbeddedServer();
	        } else if (className != null && className.equals(HTTPSERVERCLASS)) {
	            String solrServerBaseURL = Settings.getConfiguration().getString(SOLR_ENPOINT_PROPERTY_NAME);
	            solrServer = new CommonsHttpSolrServer(solrServerBaseURL);
	        } else {
	            throw new UnsupportedType("0000","SolrServerFactory.createSolrServer - MetacatIndex doesn't support this solr server type: "+className);
	        }
	    }
        return solrServer;
	}
	
	private static void generateEmbeddedServer() throws ParserConfigurationException, IOException, SAXException  {
	    String solrHomeDir = Settings.getConfiguration().getString(SOLR_HOME_PROPERTY_NAME);
        log.info("The configured solr home from properties is " + solrHomeDir);
        String configFileName = Settings.getConfiguration().getString(SOLR_CONFIG_FILE_NAME_PROPERTY_NAME);
        File configFile = new File(solrHomeDir, configFileName);
        coreContainer = new CoreContainer(solrHomeDir, configFile);
        coreContainer.load(solrHomeDir, configFile);
        collectionName = getCollectionName();
        solrServer = new EmbeddedSolrServer(coreContainer, collectionName);
	}
	
	/**
	 * Get the the CoreContainer of the generated EmbeddedSolrServer.
	 * @return it may return null if the solr is configured as the SolrHttpServer
	 * @throws SAXException 
	 * @throws IOException 
	 * @throws ParserConfigurationException 
	 * @throws UnsupportedType 
	 * @throws Exception 
	 */
	public static CoreContainer getCoreContainer() throws UnsupportedType, ParserConfigurationException, IOException, SAXException  {
	    if(coreContainer == null) {
	        createSolrServer();
	    }
	    return coreContainer;
	}
	
	/**
	 * Get the CollectionName of the generated EmbeddedSolrServer. It can be null if the solr is configured as a SolrHttpServer
	 * @return
	 */
	public static String getCollectionName() {
	    if(collectionName == null) {
	        collectionName = Settings.getConfiguration().getString(SOLR_COLLECTION_NAME_PROPERTY_NAME);
	    }
	    return collectionName;
	}
	
	
}
