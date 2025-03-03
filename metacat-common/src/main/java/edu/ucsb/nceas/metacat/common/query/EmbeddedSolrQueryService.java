package edu.ucsb.nceas.metacat.common.query;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;

import java.util.Map;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.XML;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.SolrCore;
import org.apache.solr.schema.SchemaField;
import org.dataone.service.exceptions.NotFound;
import org.dataone.service.exceptions.UnsupportedType;
import org.dataone.service.types.v1.Subject;
import org.xml.sax.SAXException;



/**
 *The query service of the embedded solr server.
 * @author tao
 *
 */
public class EmbeddedSolrQueryService extends SolrQueryService {
    private static final Log log = LogFactory.getLog(EmbeddedSolrQueryService.class);
    private EmbeddedSolrServer solrServer = null;
    private CoreContainer coreContainer = null;
    private String collectionName = null;
    private SolrCore solrCore = null;

    /**
     * Constructor.
     * @param solrServer  the solr server associated with the service
     * @param coreContainer  the container of the core
     * @param collectionName  the name of the solr collection
     * @throws NotFound 
     */
    public EmbeddedSolrQueryService(
        EmbeddedSolrServer solrServer, CoreContainer coreContainer, String collectionName)
        throws NotFound {
        if (solrServer == null) {
            throw new NullPointerException(
                "EmbeddedSolrQueryService.constructor - the EmbeddedSolrServer parameter can't be"
                    + " null.");
        }
        if (coreContainer == null) {
            throw new NullPointerException(
                "EmbeddedSolrQueryService.constructor - the CoreContainer parameter can't be null"
                    + ".");
        }
        if (collectionName == null || collectionName.isBlank()) {
            throw new NullPointerException(
                "EmbeddedSolrQueryService.constructor - the name of Collection parameter can't be"
                    + " null or empty.");
        }
        this.solrServer = solrServer;
        this.coreContainer = coreContainer;
        this.collectionName = collectionName;
        this.solrCore = this.coreContainer.getCore(collectionName);
        if (solrCore == null) {
            throw new NotFound(
                "0000", "EmbeddedSolrQueryService.constructor - There is no SolrCore named "
                + collectionName + ".");
        }
        schema = solrCore.getLatestSchema();
        fieldMap = schema.getFields();
    }

    /**
     * Query the Solr server with specified query and user's identity. If the Subjects is null,
     * there will be no access rules for the query. This is for the embedded solr server.
     *
     * @param query    the query params.
     * @param subjects the user's identity which sent the query
     * @param method   the method such as GET, POST and et al will be used in this query. This only
     *                 works for the HTTP Solr server.
     * @return the response
     * @throws SAXException
     * @throws IOException
     * @throws ParserConfigurationException
     * @throws SolrServerException
     * @throws UnsupportedType
     */
    public InputStream query(SolrParams query, Set<Subject> subjects, SolrRequest.METHOD method)
        throws ParserConfigurationException, IOException, SAXException, SolrServerException,
        UnsupportedType {
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
            throw new UnsupportedType("0000", "EmbeddedSolrQueryService.query - the wt type " + wt
                + " in the solr query is not supported");
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
     * Get the version of the solr server.
     * @return the version of the solr server
     */
    public String getSolrServerVersion() {
        if (solrSpecVersion != null) {
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
                    log.error("Can't get the solr version since " + e.getMessage());
                }
            }
            if (solrSpecVersion == null || solrSpecVersion.isBlank()) {
                solrSpecVersion = UNKNOWN;
            } 
            return solrSpecVersion;
        }
    }

}
