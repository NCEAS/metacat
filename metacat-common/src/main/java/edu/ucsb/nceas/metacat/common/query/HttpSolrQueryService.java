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
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.codec.net.URLCodec;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.solr.client.solrj.impl.CommonsHttpSolrServer;
import org.apache.solr.client.solrj.util.ClientUtils;

import org.apache.solr.common.params.SolrParams;
import org.apache.solr.core.SolrConfig;
import org.apache.solr.schema.FieldType;
import org.apache.solr.schema.IndexSchema;
import org.apache.solr.schema.SchemaField;
import org.apache.solr.schema.TextField;
import org.dataone.configuration.Settings;
import org.dataone.service.exceptions.NotFound;
import org.dataone.service.exceptions.NotImplemented;
import org.dataone.service.types.v1.Subject;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;



/**
 * The query service for the http solr server.
 * @author tao
 *
 */
public class HttpSolrQueryService extends SolrQueryService {
    private static final String SELECTIONPHASE = "/select";
    private static final String SOLR_SYSTEMINFO_URLAPPENDIX = "solr.systeminfo.urlappendix";
    private static final String SOLR_SCHEMA_URLAPPENDIX = "sorl.schema.urlappendix";
    private static final String SOLR_CONFIG_URLAPPENDIX = "solr.config.urlappendix";
    private static final String SPEC_PATH = "//str[@name='solr-spec-version']";
    private static final String FIELDS_PATH = "//fields//field";
    private static final String COPY_FIELDS_PATH = "//copyField";
    private static final String DEST = "dest";
    private static final String TRUE = "true";
    
    private String solrServerBaseURL = null;
    private CommonsHttpSolrServer httpSolrServer = null;
    private static Log log = LogFactory.getLog(HttpSolrQueryService.class);
    /**
     * Constructor
     * @param httpSolrServer
     */
    public HttpSolrQueryService(CommonsHttpSolrServer httpSolrServer) {
        if(httpSolrServer == null) {
            throw new NullPointerException("HttpSolrQueryService.constructor - The httpSolrServer parameter can't be null");
        }
        this.httpSolrServer = httpSolrServer;
        this.solrServerBaseURL = httpSolrServer.getBaseURL();
    }
    
    /**
     * Query the Solr server with specified query string and the user's identity. This is the for the http solr server.
     * It is hard to transform the SolrQueryReponse object to the InputStream object for the HttpSolrServer
     * since the transform needs the SolrCore. We have to open the solr url directly to get the InputStream.
     * @param query the query string
     * @param subjects the user's identity which sent the query
     * @return the response
     * @throws NotFound 
     * @throws IOException 
     * @throws Exception
     */
    /*public InputStream query(String query, Set<Subject>subjects) throws NotFound, IOException {
        StringBuffer accessFilter = generateAccessFilterParamsString(subjects);
        if(accessFilter != null && accessFilter.length() != 0) {
            query = solrServerBaseURL+"/select?"+query+"&"+FILTERQUERY+"="+accessFilter.toString();
            query = ClientUtils.escapeQueryChars(query);
        } else {
            throw new NotFound("0000", "HttpSolrQueryService.query - There is no identity (even user public) for the user who issued the query");
        }
        log.info("==========HttpSolrQueryService.query - the final url for querying the solr http server is "+query);
        URL url = new URL(query);
        
        return url.openStream();
    }*/
    
    /**
     * Query the Solr server with specified query and user's identity. 
     * It is hard to transform the SolrQueryReponse object to the InputStream object for the HttpSolrServer
     * since the transform needs the SolrCore. We have to open the solr url directly to get the InputStream.
     * @param query the query params. 
     * @param subjects the user's identity which sent the query. If the Subjects is null, there wouldn't be any access control.
     * @return the response
     * @throws IOException 
     * @throws NotFound 
     * @throws Exception
     */
    public  InputStream query(SolrParams query, Set<Subject>subjects) throws IOException, NotFound {
        boolean xmlFormat = false;
        String queryString = ClientUtils.toQueryString(query, xmlFormat);
        log.info("==========HttpSolrQueryService.query - the query string after transforming from the SolrParams to the string "+queryString);
        StringBuffer accessFilter = generateAccessFilterParamsString(subjects);
        if(accessFilter != null && accessFilter.length() != 0) {
            String accessStr = accessFilter.toString();
            log.debug("==========HttpSolrQueryService.query - the access string is "+accessStr);
            URLCodec urlCodec = new URLCodec();
            accessStr = urlCodec.encode(accessStr, "UTF-8");
            log.debug("==========HttpSolrQueryService.query - the access string after escape special characters string "+accessStr);
            queryString = queryString+"&"+FILTERQUERY+"="+accessStr;
           
        }
        
        
        //queryString = ClientUtils.escapeQueryChars(queryString);
        queryString = solrServerBaseURL+SELECTIONPHASE+queryString;
        log.info("==========HttpSolrQueryService.query - the final url for querying the solr http server is "+queryString);
        URL url = new URL(queryString);    
        return url.openStream();
        //throw new NotImplemented("0000", "HttpSolrQueryService - the method of  query(SolrParams query, Set<Subject>subjects) is not for the HttpSolrQueryService. We donot need to implemente it");
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
        if(fieldMap == null || fieldMap.isEmpty()) {
            getIndexSchemaFieldFromServer();
        }
        //System.out.println("get filed map ==========================");
        return fieldMap;
    }
    
   
    
    /**
     * Get the list of the valid field name (moved the fields names of the CopyFieldTarget).
     * @return
     * @throws SAXException 
     * @throws IOException 
     * @throws ParserConfigurationException 
     * @throws MalformedURLException 
     */
    public List<String> getValidSchemaField() throws MalformedURLException, ParserConfigurationException, IOException, SAXException {
        if(fieldMap == null || fieldMap.isEmpty()) {
            getIndexSchemaFields();
        }
        return super.getValidSchemaFields();
    }
    
   
    /*
     * Get the fieldMap from the http server. 
     * @throws MalformedURLException
     * @throws ParserConfigurationException
     * @throws IOException
     * @throws SAXException
     */
    private void getIndexSchemaFieldFromServer() throws MalformedURLException, ParserConfigurationException, IOException, SAXException {
        //System.out.println("get filed map from server (downloading files) ==========================");
        SolrConfig config = new SolrConfig("dataone", new InputSource(getSolrConfig())); 
        schema = new IndexSchema(config, "dataone", new InputSource(lookupSchema()));
        fieldMap = schema.getFields();
    }
    
    /*
     * Parse the schema.xml and get the validSolrFieldName list
     */
    /*private void parseSchema() throws MalformedURLException, ParserConfigurationException, SAXException, IOException, XPathExpressionException {
        validSolrFieldNames = new ArrayList<String>();
        Map<String, SchemaField> fieldMap = new HashMap<String, SchemaField>();
        Document schema = transformInputStreamToDoc(getSchema());
        Vector<String>copyFieldTargetNames = new Vector<String>();
        NodeList copyFields = (NodeList) XPathFactory.newInstance().newXPath()
                        .evaluate(COPY_FIELDS_PATH, schema, XPathConstants.NODESET);
        if(copyFields != null) {
            for(int i=0; i<copyFields.getLength(); i++) {
                Element copyField = (Element)copyFields.item(i);
                String target = copyField.getAttribute(DEST);
                if(target != null && !target.trim().equals("")) {
                    copyFieldTargetNames.add(target);
                }
            }
        }
        NodeList fields = (NodeList) XPathFactory.newInstance().newXPath()
                        .evaluate(FIELDS_PATH, schema, XPathConstants.NODESET);
        if(fields!= null) {
            for(int i=0; i<fields.getLength(); i++) {
                Element fieldElement = (Element) fields.item(i);
                String name = fieldElement.getAttribute("name");
                if(name != null && !name.trim().equals("")) {
                    if(!copyFieldTargetNames.contains(name)) {
                        validSolrFieldNames.add(name);
                    }
                }
            }
        }
    }*/
    
    
    /*
     * Get the SolrConfig InputStream.
     * @return
     * @throws MalformedURLException
     * @throws IOException
     */
    private InputStream getSolrConfig() throws MalformedURLException, IOException {
        String solrConfigAppendix = Settings.getConfiguration().getString(SOLR_CONFIG_URLAPPENDIX);
        String configURL = solrServerBaseURL+solrConfigAppendix;
        return (new URL(configURL)).openStream();
    }
    /*
     * Get the schema InputStream from the url which is specified in the metacat.properties and transform it to a Document.
     */
    private InputStream lookupSchema() throws MalformedURLException, IOException {
        String schemaURLAppendix = Settings.getConfiguration().getString(SOLR_SCHEMA_URLAPPENDIX);
        String schemaURL = solrServerBaseURL+schemaURLAppendix;
        return (new URL(schemaURL)).openStream();
    }
    
    /**
     * Get the version of the solr server.
     * @return
     */
    public String getSolrServerVersion() {
        if(solrSpecVersion == null) {
            getHttpSolrServerVersion();
        } 
        //System.out.println("get spec version  ==========================");
        return solrSpecVersion;
    }
    
    
    /*
     * Get the solr server version from the system information url. 
     */
    private void getHttpSolrServerVersion() {
        //System.out.println("get spec version from server (downloading files) ==========================");
        String systemInfoUrlAppendix = Settings.getConfiguration().getString(SOLR_SYSTEMINFO_URLAPPENDIX);
        String systemInfoUrl = solrServerBaseURL+systemInfoUrlAppendix;
        try {
            Document doc = transformInputStreamToDoc((new URL(systemInfoUrl)).openStream());
            NodeList nodeList = (NodeList) XPathFactory.newInstance().newXPath()
                            .evaluate(SPEC_PATH, doc, XPathConstants.NODESET);
            if(nodeList != null && nodeList.getLength() >0) {
                //System.out.println("nodelist is not null branch");
                Node specNode = nodeList.item(0);
                solrSpecVersion = specNode.getFirstChild().getNodeValue();
            } else {
                //System.out.println("nodelist is null branch");
                solrSpecVersion = UNKNOWN;
            }
            
        } catch (Exception e) {
            log.error("HttpSolrQueryService.getHttpSolrServerVersion - can't get the solr specification version since "+e.getMessage());
            solrSpecVersion = UNKNOWN;
        }
        
        
    }
    
    /**
     * Generate a Document from the InputStream
     * @param input
     * @return
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws IOException
     */
    private Document transformInputStreamToDoc(InputStream input) throws ParserConfigurationException, SAXException, IOException {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(input);
        return doc;
    }
    
 
}
