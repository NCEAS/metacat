package edu.ucsb.nceas.metacat.common.query;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.util.Version;
import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.core.SolrResourceLoader;
import org.apache.solr.core.ConfigSetService;
import org.apache.solr.schema.IndexSchema;
import org.apache.solr.schema.IndexSchemaFactory;
import org.apache.solr.schema.SchemaField;
import org.dataone.configuration.Settings;
import org.dataone.service.exceptions.NotFound;
import org.dataone.service.exceptions.UnsupportedType;
import org.dataone.service.types.v1.Subject;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;




/**
 * The query service for the http solr server.
 * @author tao
 *
 */
public class HttpSolrQueryService extends SolrQueryService {
    private static final String SOLR_SYSTEMINFO_URLAPPENDIX = "solr.systeminfo.urlappendix";
    private static final String SOLR_SCHEMA_URLAPPENDIX = "solr.schema.urlappendix";
    private static final String SPEC_PATH = "//str[@name='solr-spec-version']";

    private String solrServerBaseURL = null;
    private HttpSolrClient httpSolrServer = null;
    private static Log log = LogFactory.getLog(HttpSolrQueryService.class);

    /**
     * Constructor
     * @param httpSolrServer the http solr server instance associated with this service
     * @throws IOException
     */
    public HttpSolrQueryService(HttpSolrClient httpSolrServer) throws IOException {
        if(httpSolrServer == null) {
            throw new NullPointerException(
                "HttpSolrQueryService.constructor - The httpSolrServer parameter can't be null");
        }
        this.httpSolrServer = httpSolrServer;
        this.solrServerBaseURL = httpSolrServer.getBaseURL();
        getIndexSchemaFieldFromServer();
    }

    /**
     * Query the Solr server with specified query and user's identity. 
     * It is hard to transform the SolrQueryReponse object to the InputStream object for the
     * HttpSolrServer
     * since the transform needs the SolrCore. We have to open the solr url directly to get the
     * InputStream.
     * @param query the query params. 
     * @param subjects the user's identity which sent the query. If the Subjects is null, there
     *                 wouldn't be any access control.
     * @param method  the method such as GET, POST and et al will be used in this query. This
     *                only works for the HTTP Solr server.
     * @return the response
     * @throws IOException
     * @throws NotFound
     * @throws UnsupportedType
     * @throws SolrServerException
     */
    public InputStream query(SolrParams query, Set<Subject> subjects, SolrRequest.METHOD method)
        throws IOException, NotFound, UnsupportedType, SolrServerException {
        InputStream inputStream = null;
        String wt = query.get(WT);
        query = appendAccessFilterParams(query, subjects);
        SolrQueryResponseTransformer solrTransformer = new SolrQueryResponseTransformer(null);
        // handle normal and skin-based queries
        if (isSupportedWT(wt)) {
            // just handle as normal solr query
            //reload the core before query. Only after reloading the core, the query result can
            // reflect the change made in metacat-index module.
            QueryResponse response = httpSolrServer.query(query, method);
            inputStream = solrTransformer.transformResults(query, response, wt);
        } else {
            throw new UnsupportedType("0000", "HttpSolrQueryService.query - the wt type " + wt
                + " in the solr query is not supported");
        }
        return inputStream;
    }

    /**
     * Get the fields list of the index schema
     * @return the map contains the index schema fields
     * @throws IOException
     */
    public  Map<String, SchemaField> getIndexSchemaFields() throws IOException {
        if(fieldMap == null || fieldMap.isEmpty()) {
            getIndexSchemaFieldFromServer();
        }
        return fieldMap;
    }

    /*
     * Get the fieldMap from the http server.
     * @throws IOException
     */
    private void getIndexSchemaFieldFromServer() throws IOException {
        log.debug("get filed map from server (downloading files) ==========================");
        SolrResourceLoader loader = new SolrResourceLoader((Paths.get("")));
        ConfigSetService service = null;
        ConfigSetService.ConfigResource configureResource =
            IndexSchemaFactory.getConfigResource(service, lookupSchema(), loader, "dataone");
        Properties substitutableProperties = new Properties();
        schema = new IndexSchema(
            "dataone", configureResource, Version.LUCENE_8_8_2, loader,
                                 substitutableProperties);
        log.debug(
            "Intialize the schema is +++++++++++++++++++++++++++++++++++++++++++++++++++" + schema);
        fieldMap = schema.getFields();
    }



    /*
     * Get the schema InputStream from the url which is specified in the metacat.properties and
     * transform it to a Document.
     * @return the input stream of the schema
     * @throws IOException
     */
    private InputStream lookupSchema() throws IOException {
        String schemaURLAppendix = Settings.getConfiguration().getString(SOLR_SCHEMA_URLAPPENDIX);
        String schemaURL = solrServerBaseURL+schemaURLAppendix;
        log.info(
            "HttpSolrQueryService.lookupSchema - the url of getting the solr configure file is "
                + schemaURL);
        return (new URL(schemaURL)).openStream();
    }

    /**
     * Get the version of the solr server.
     * @return the version of the solr server
     */
    public String getSolrServerVersion() {
        if(solrSpecVersion == null) {
            getHttpSolrServerVersion();
        }
        return solrSpecVersion;
    }

    /*
     * Get the solr server version from the system information url.
     */
    private void getHttpSolrServerVersion() {
        String systemInfoUrlAppendix =
            Settings.getConfiguration().getString(SOLR_SYSTEMINFO_URLAPPENDIX);
        String systemInfoUrl = solrServerBaseURL+systemInfoUrlAppendix;
        try {
            Document doc = transformInputStreamToDoc((new URL(systemInfoUrl)).openStream());
            NodeList nodeList = (NodeList) XPathFactory.newInstance().newXPath()
                            .evaluate(SPEC_PATH, doc, XPathConstants.NODESET);
            if(nodeList != null && nodeList.getLength() >0) {
                Node specNode = nodeList.item(0);
                solrSpecVersion = specNode.getFirstChild().getNodeValue();
            } else {
                solrSpecVersion = UNKNOWN;
            }

        } catch (Exception e) {
            log.error(
                "HttpSolrQueryService.getHttpSolrServerVersion - can't get the solr specification"
                    + " version since "
                    + e.getMessage());
            solrSpecVersion = UNKNOWN;
        }

    }

    /**
     * Generate a Document from the InputStream
     * @param input the source of the document
     * @return a Document object of the input
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws IOException
     */
    private Document transformInputStreamToDoc(InputStream input)
        throws ParserConfigurationException, SAXException, IOException {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        return dBuilder.parse(input);
    }

}
