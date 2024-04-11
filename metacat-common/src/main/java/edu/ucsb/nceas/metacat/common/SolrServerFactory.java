package edu.ucsb.nceas.metacat.common;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Properties;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.SolrClient;
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
    public static final String SOLR_ENPOINT_PROPERTY_NAME = "solr.baseURL";
    private static final String EMBEDDEDSERVERCLASS = "org.apache.solr.client.solrj.embedded.EmbeddedSolrServer";
    private static final String HTTPSERVERCLASS = "org.apache.solr.client.solrj.impl.CommonsHttpSolrServer";
    private static final String CORENAME_PROPERTY_NAME = "solr.coreName";
    private static final String SLASH = "/";

	public static Log log = LogFactory.getLog(SolrServerFactory.class);
	
	private static CoreContainer coreContainer = null;
	private static String collectionName = null;
	private static SolrClient solrServer = null;
	private static SolrClient solrAdminClient = null;//only for the http solr server.
	//private static String solrServerBaseURL = null;

	public static SolrClient createSolrServer() throws ParserConfigurationException, IOException, SAXException, UnsupportedType {
	    if(solrServer == null) {
	        String className = Settings.getConfiguration().getString(SOLR_SERVER_CLASSNAME_PROPERTY_NAME);
	        if (className != null && className.equals(EMBEDDEDSERVERCLASS)) {
	            generateEmbeddedServer();
	        } else if (className != null && className.equals(HTTPSERVERCLASS)) {
	            String solrServerBaseURL = Settings.getConfiguration().getString(SOLR_ENPOINT_PROPERTY_NAME);
	            String coreName = Settings.getConfiguration().getString(CORENAME_PROPERTY_NAME);
	            if (solrServerBaseURL != null && solrServerBaseURL.endsWith(SLASH)) {
	                solrServerBaseURL = solrServerBaseURL+coreName;
	            } else {
	                solrServerBaseURL = solrServerBaseURL+SLASH+coreName;
	            }
	            log.info("SolrServerFactory.createSolrServer - the final solr server base url is "+solrServerBaseURL);
	            solrServer = new HttpSolrClient.Builder(solrServerBaseURL).build();
	        } else {
	            throw new UnsupportedType("0000","SolrServerFactory.createSolrServer - MetacatIndex doesn't support this solr server type: "+className);
	        }
	    }
        return solrServer;
	}
	
	
	/**
	 * Create a solr admin client. It is only for the http solr server. It only has the base solr url without the core name.
	 * @return a solr admin client. The null will be returned if it is an embedded solr server.
	 * @throws ParserConfigurationException
	 * @throws IOException
	 * @throws SAXException
	 * @throws UnsupportedType
	 */
	public static SolrClient createSolrAdminClient() throws ParserConfigurationException, IOException, SAXException, UnsupportedType {
        if(solrAdminClient == null) {
            String className = Settings.getConfiguration().getString(SOLR_SERVER_CLASSNAME_PROPERTY_NAME);
            if (className != null && className.equals(EMBEDDEDSERVERCLASS)) {
                solrAdminClient = null;
            } else if (className != null && className.equals(HTTPSERVERCLASS)) {
                String solrServerBaseURL = Settings.getConfiguration().getString(SOLR_ENPOINT_PROPERTY_NAME);
                log.info("SolrServerFactory.createSolrServer - the final solr server base url is "+solrServerBaseURL);
                solrAdminClient = new HttpSolrClient.Builder(solrServerBaseURL).build();
            } else {
                throw new UnsupportedType("0000","SolrServerFactory.createSolrServer - MetacatIndex doesn't support this solr server type: "+className);
            }
        }
        return solrAdminClient;
    }
	
	private static void generateEmbeddedServer() throws ParserConfigurationException, IOException, SAXException  {
	    String solrHomeDir = Settings.getConfiguration().getString(SOLR_HOME_PROPERTY_NAME);
        log.info("The configured solr home from properties is " + solrHomeDir);
        String configFileName = Settings.getConfiguration().getString(SOLR_CONFIG_FILE_NAME_PROPERTY_NAME);
        File configFile = new File(solrHomeDir, configFileName);
        Properties properties = new Properties();
        coreContainer = new CoreContainer(Paths.get(solrHomeDir), properties);
        coreContainer.load();
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
