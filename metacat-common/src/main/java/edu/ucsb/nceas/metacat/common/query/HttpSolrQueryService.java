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
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.lucene.util.Version;
import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.core.SolrResourceLoader;
import org.apache.solr.schema.IndexSchema;
import org.apache.solr.schema.SchemaField;
import org.dataone.configuration.Settings;
import org.dataone.service.exceptions.NotFound;
import org.dataone.service.exceptions.UnsupportedType;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Subject;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import edu.ucsb.nceas.metacat.common.query.stream.ContentTypeSolrResponseInputStream;




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
    private static HttpClient httpClient = HttpClientBuilder.create().build();
    private static Log log = LogFactory.getLog(HttpSolrQueryService.class);
    /**
     * Constructor
     * @param httpSolrServer
     * @throws SAXException 
     * @throws IOException 
     * @throws ParserConfigurationException 
     * @throws MalformedURLException 
     */
    public HttpSolrQueryService(String httpSolrServerBaseUrl) throws MalformedURLException, ParserConfigurationException, IOException, SAXException {
        this.solrServerBaseURL = httpSolrServerBaseUrl;
        getIndexSchemaFieldFromServer();
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
     * @param method  the method such as GET, POST and et al will be used in this query. This only works for the HTTP Solr server.
     * @return the response
     * @throws IOException 
     * @throws NotFound 
     * @throws Exception
     */
    public  InputStream query(SolrParams query, Set<Subject> subjects, SolrRequest.METHOD method) throws IOException, NotFound, UnsupportedType, SolrServerException {
        InputStream inputStream = null;
        String wt = query.get(WT);
        if (wt == null) {
            wt = "xml"; //The http solr server default wt is json, but our server is xml. We have to set to xml so it wouldn't change the behavior of Metacat.
            NamedList<String> list = new NamedList<String>();
            list.add(WT, wt);
            SolrParams wtParam = list.toSolrParams();
            query = SolrParams.wrapAppended(query, wtParam);
        }
        if (isSupportedWT(wt)) {
            if (method.equals(SolrRequest.METHOD.GET)) {
                inputStream = httpGetQuery(query, subjects);
            } else {
                inputStream = httpPostQuery(query, subjects);
            }
        } else {
            throw new UnsupportedType("0000","HttpSolrQueryService.query - the wt type " + wt + " in the solr query is not supported");
        }
        ContentTypeSolrResponseInputStream responseStream = new ContentTypeSolrResponseInputStream(inputStream);
        responseStream.setContentType(SolrQueryResponseWriterFactory.getContentType(wt));
        return responseStream;
    }
    
    /**
     * Use a http client to query the solr server directly by the get method
     * @param query the query params.
     * @param subjects  subjects the user's identity which sent the query. If the Subjects is null, there wouldn't be any access control.
     * @return the response input stream from the solr server
     * @throws IOException
     */
    private InputStream httpGetQuery(SolrParams query, Set<Subject> subjects) throws IOException {
        InputStream stream = null;
        query = appendAccessFilterParams(query, subjects);
        String queryStr = solrServerBaseURL + SELECTIONPHASE + query.toQueryString();
        log.info("HttpSolrQueryService.httpGetQuery - the query string is " + queryStr);
        HttpGet get = new HttpGet(queryStr);
        HttpResponse response = httpClient.execute(get);
        stream = response.getEntity().getContent();
        return stream;
    }
    
    /**
     * Use a http client to query the solr server directly by the post method
     * @param query the query params.
     * @param subjects  subjects the user's identity which sent the query. If the Subjects is null, there wouldn't be any access control.
     * @return the response input stream from the solr server
     * @throws ClientProtocolException
     * @throws IOException
     */
    private InputStream httpPostQuery(SolrParams query, Set<Subject> subjects) throws ClientProtocolException, IOException {
        InputStream stream = null;
        HttpPost httpPost = new HttpPost(solrServerBaseURL + SELECTIONPHASE);
        List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
        if (query != null) {
            Iterator<String> paraNames = query.getParameterNamesIterator();
            while (paraNames.hasNext()) {
                String name = paraNames.next();
                String[] values = query.getParams(name);
                if (values != null) {
                    for (int i=0; i<values.length; i++) {
                        log.info("HttpSolrQueryService.httpPostQuery - add the param: " + name + " with value " + values[i] + " into the http name/value pair.");
                        params.add(new BasicNameValuePair(name, values[i]));
                    }
                }
            }
        }
        //append the access
        StringBuffer accessFilter = generateAccessFilterParamsString(subjects);
        if (accessFilter != null && accessFilter.length() != 0) {
            String accessStr = accessFilter.toString();
            log.info("HttpSolrQueryService.httpPostQuery - the access  string is " + accessStr);
            params.add(new BasicNameValuePair(FILTERQUERY, accessStr));
        }
        httpPost.setEntity(new UrlEncodedFormEntity(params));
        HttpResponse response = httpClient.execute(httpPost);
        stream = response.getEntity().getContent();
        return stream;
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
        log.debug("get filed map from server (downloading files) ==========================");
        SolrResourceLoader loader = new SolrResourceLoader();
        schema = new IndexSchema("dataone", new InputSource(lookupSchema()), Version.LUCENE_8_4_1, loader);
        log.info("Intialize the schema is +++++++++++++++++++++++++++++++++++++++++++++++++++"+schema);
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
        log.info("HttpSolrQueryService.getSolrConfig - the url of getting the solr configure file is "+configURL);
        return (new URL(configURL)).openStream();
    }
    /*
     * Get the schema InputStream from the url which is specified in the metacat.properties and transform it to a Document.
     */
    private InputStream lookupSchema() throws MalformedURLException, IOException {
        String schemaURLAppendix = Settings.getConfiguration().getString(SOLR_SCHEMA_URLAPPENDIX);
        String schemaURL = solrServerBaseURL+schemaURLAppendix;
        log.info("HttpSolrQueryService.lookupSchema - the url of getting the solr configure file is "+schemaURL);
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
