/**
 *  '$RCSfile$'
 *  Copyright: 2020 Regents of the University of California and the
 *              National Center for Ecological Analysis and Synthesis
 *
 *   '$Author:  $'
 *     '$Date:  $'
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
package edu.ucsb.nceas.metacat.doi.datacite.relation;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.htrace.shaded.fasterxml.jackson.core.JsonParseException;
import org.apache.htrace.shaded.fasterxml.jackson.core.JsonProcessingException;
import org.apache.htrace.shaded.fasterxml.jackson.databind.JsonMappingException;
import org.apache.htrace.shaded.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.dataone.configuration.Settings;

import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.rdf.model.Statement;

import edu.ucsb.nceas.metacat.doi.datacite.DataCiteMetadataFactory;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.metacat.shared.ServiceException;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;

/**
 *  A class to query the DataONE metrics service to get a list of known citations for a dataset.
 * @author tao
 *
 */
public class CitationRelationHandler implements RelationshipHandler {
    public static final String ISCITEDBY = "IsCitedBy";
    public static final String CITATIONS = "citations";
    public static final String QUERY = "query";
    private static String citationServerURL = null;
    private static Log logMetacat  = LogFactory.getLog(CitationRelationHandler.class);
    
    /**
     * Constructor
     * @throws ServiceException 
     * @throws PropertyNotFoundException 
     */
    public CitationRelationHandler()  {
        if (citationServerURL == null) {
            citationServerURL = Settings.getConfiguration().getString("dataone.metric.serviceUrl");
            logMetacat.debug("CitationRelationHandler.CitationRelationHandler - the citation service url is " + citationServerURL);
            if (citationServerURL != null && !citationServerURL.endsWith("/")) {
                citationServerURL = citationServerURL + "/";
            }
        }
        logMetacat.debug("CitationRelationHandler.CitationRelationHandler - the server url is " + citationServerURL);
    }
    
    
    /**
     * Method to get the relationship. Now we only handle IsCitedBy
     * @param identifier  the subject of the triple relationship
     * @return a vector of triple statements
     */
    public Vector<Statement> getRelationships(String identifier) {
        Vector<Statement> statementList = new Vector<Statement>();
        //Get the statements of the predicate - IsCitedBy
        Property isCitedByPredic = ResourceFactory.createProperty(DataCiteMetadataFactory.NAMESPACE +"/", ISCITEDBY);
        Resource subject = ResourceFactory.createResource(identifier);
        List<String> ids = null;
        try {
            ids = getIsCitedBys(identifier);
        } catch (IOException e) {
            ids = new Vector<String>();
            logMetacat.error("CitationRelationHandler.getRelationships - can't get the citation relationship for id " + identifier + " since " + e.getMessage());
            e.printStackTrace();
        }
        for (String id : ids) {
            if (id != null && !id.trim().equals("")) {
                Literal object =  ResourceFactory.createPlainLiteral(id);
                Statement statement = ResourceFactory.createStatement(subject, isCitedByPredic, object);
                statementList.add(statement);
                logMetacat.debug("CitationRelationHandler.getRelationships - the id " + id + " was added to statement with the relationship IsCitedBy");
            }
        }
        return statementList;
    }
    
    /**
     * Get the list of ids which cites the given identifier - the identifier isCitedBy id
     * @param identifier  the identifier is the subject of the relation isCitedBy
     * @return  the list of ids which cite the given identifier
     * @throws ClientProtocolException
     * @throws IOException
     */
    public List<String> getIsCitedBys(String identifier) throws ClientProtocolException, IOException {
        List<String> ids = new Vector<String>();
        String restStr = buildRestString(identifier);
        HttpClient client = HttpClientBuilder.create().build();
        // Send http GET request
        HttpGet get = new HttpGet(restStr);
        HttpResponse response = client.execute(get);
        CitationsResponse citationsResponse = parseResponse(response.getEntity().getContent());
        if(citationsResponse != null) {
            List<CitationsMetadata> citationsMetadatas = citationsResponse.getCitationsMetadata();
            if (citationsMetadatas != null) {
                for (CitationsMetadata metadata : citationsMetadatas) {
                    if (metadata != null && metadata.getSource_id() != null && !metadata.getSource_id().trim().equals("")) {
                        logMetacat.debug("CitationRelationHandler.getIsCitedBys - add the source id " + metadata.getSource_id() + " into the IsCitedBy list");
                        ids.add(metadata.getSource_id());
                    }
                }
            }
        } else {
            logMetacat.info("CitationRelationHandler.getIsCitedBys - Metacat didn't get the citations information from the json query response");
        }
        return ids;
        
    }
    
    /**
     * Build the rest command to query the metric server with the relation - the given identifier isCitedBy ids which we are interested in
     * @param identifier  the subject of the triple with the isCitedBy relationship
     * @return  the rest command to query the metric server
     * @throws JsonProcessingException
     * @throws UnsupportedEncodingException 
     */
    public String buildRestString(String identifier) throws JsonProcessingException, UnsupportedEncodingException {
        String command = null;
        if (identifier != null && !identifier.trim().equals("")) {
            Vector<String> ids = new Vector<String>();
            ids.add(identifier);
            String query =  buildQuery(ids);
            if (query != null && citationServerURL != null) {
                command = citationServerURL + CITATIONS + "?" + QUERY + "=" + URLEncoder.encode(query, "UTF-8");
            }
        }
        logMetacat.debug("CitationRelationHandler.buildRestString - the rest string is " + command);
        return command;
    }
    
    /**
     * Build a query string (the JSON format) to get a list of source ids which has this relation:
     * targetId isCitedBy sourceId
     * @param targetIds  the ids which is the subject of the triple - the subject isCitedBy the object
     * @return the string of the query. Null will be returned if this method can't construct the query string
     * @throws JsonProcessingException 
     */
    private String buildQuery(Vector<String> targetIds) throws JsonProcessingException {
        String query = null;
        if (targetIds != null) {
            CitationsFilter filter = new CitationsFilter();
            for (String id : targetIds) {
                if (id != null && !id.trim().equals("")) {
                    logMetacat.debug("CitationRelationHandler.buildQuery - add the target id " + id + " into the citation query filter.");
                    filter.addValue(id);
                }
            }
            if (filter.getValues().size() > 0) {
                CitationsQuery citationQuery = new CitationsQuery();
                citationQuery.addFilter(filter);
                ObjectMapper mapper = new ObjectMapper();
                query = mapper.writeValueAsString(citationQuery);
            }
        }
        logMetacat.debug("CitationRelationHandler.buildQuery - the jason query string is " + query);
        return query;
    }
    
    /**
     * Parse the query response (the json string format) to an JAVA object
     * @param jsonStr  the response needs to be parsed
     * @return  the java object representation of the response. Null maybe returned if the json string is null or blank. 
     * @throws JsonParseException
     * @throws JsonMappingException
     * @throws IOException
     */
    public CitationsResponse parseResponse(InputStream jsonStream) throws JsonParseException, JsonMappingException, IOException {
        CitationsResponse response = null;
        if (jsonStream != null) {
            ObjectMapper mapper = new ObjectMapper();
            response = mapper.readValue(jsonStream, CitationsResponse.class);
        }
        return response;
        
    }

}
