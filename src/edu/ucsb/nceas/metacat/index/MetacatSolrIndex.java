/**
 *  '$RCSfile$'
 *  Copyright: 2000-2011 Regents of the University of California and the
 *              National Center for Ecological Analysis and Synthesis
 *
 *   '$Author: leinfelder $'
 *     '$Date: 2012-11-29 16:52:29 -0800 (Thu, 29 Nov 2012) $'
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
package edu.ucsb.nceas.metacat.index;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.URLDecoder;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.params.MultiMapSolrParams;
import org.apache.solr.common.params.SolrParams;

import org.apache.solr.servlet.SolrRequestParsers;
import org.dataone.service.exceptions.NotFound;
import org.dataone.service.exceptions.NotImplemented;
import org.dataone.service.exceptions.UnsupportedType;
import org.dataone.service.types.v1.Event;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Subject;
import org.dataone.service.types.v2.SystemMetadata;
import org.dataone.service.util.Constants;
import org.xml.sax.SAXException;

import edu.ucsb.nceas.metacat.DBTransform;
import edu.ucsb.nceas.metacat.EventLog;
import edu.ucsb.nceas.metacat.common.index.IndexTask;
import edu.ucsb.nceas.metacat.common.query.SolrQueryResponseWriterFactory;
import edu.ucsb.nceas.metacat.common.query.SolrQueryService;
import edu.ucsb.nceas.metacat.common.query.SolrQueryServiceController;
import edu.ucsb.nceas.metacat.common.query.stream.ContentTypeByteArrayInputStream;
import edu.ucsb.nceas.metacat.dataone.hazelcast.HazelcastService;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.metacat.rabbitmq.RabbitMQService;
import edu.ucsb.nceas.metacat.shared.ServiceException;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;


/**
 * This class will query the solr server and return the result.
 * @author tao
 *
 */
public class MetacatSolrIndex {
    
    
    //public static final String SOLRQUERY = "solr";
    //public static final String SOLR_HOME_PROPERTY_NAME = "solr.homeDir";
    //public static final String SOLR_CONFIG_FILE_NAME_PROPERTY_NAME = "solr.configFileName";
    //public static final String SOLR_COLLECTION_NAME_PROPERTY_NAME = "solr.collectionName";
    //public static final String SOLR_SERVER_CLASSNAME_PROPERTY_NAME = "solr.server.classname";
   
    
    private static Log log = LogFactory.getLog(MetacatSolrIndex.class);
    private static MetacatSolrIndex  solrIndex = null;
    private static String nodeType = null;
    
    public static MetacatSolrIndex getInstance() throws Exception {
        if (solrIndex == null) {
            solrIndex = new MetacatSolrIndex();
        }
        return solrIndex;
    }
    
    /**
     * Constructor
     * @throws SAXException 
     * @throws IOException 
     * @throws ParserConfigurationException 
     */
    private MetacatSolrIndex() throws Exception {
    	    nodeType = PropertyService.getProperty("dataone.nodeType");
    }
    
    
    
    
    /**
     * Query the solr server
     * @param query  the solr query string 
     * @param authorizedSubjects the authorized subjects in this query session
     * @param isMNadmin the indicator of the authorized subjects are the mn admin or not
     * @return the result as the InputStream
     * @throws SolrServerException 
     * @throws ClassNotFoundException 
     * @throws SQLException 
     * @throws PropertyNotFoundException 
     * @throws SAXException 
     * @throws ParserConfigurationException 
     * @throws UnsupportedType 
     * @throws NotFound 
     * @throws NotImplemented 
     */
    public InputStream query(String query, Set<Subject>authorizedSubjects, boolean isMNadmin) throws SolrServerException, IOException, PropertyNotFoundException, SQLException, 
    ClassNotFoundException, ParserConfigurationException, SAXException, NotImplemented, NotFound, UnsupportedType, SolrException{
        //allow "+" in query syntax, see: https://projects.ecoinformatics.org/ecoinfo/issues/6435
        query = query.replaceAll("\\+", "%2B");
        SolrParams solrParams = parseQueryString(query);
        return query(solrParams, authorizedSubjects, isMNadmin);
     
    }
    
    
    /**
     * Given a standard query string map it into solr params
     *
     */
    private static MultiMapSolrParams parseQueryString(String queryString) {
      Map<String,String[]> map = new HashMap<String, String[]>();
      if (queryString != null && queryString.length() > 0) {
        try {
          for (String kv : queryString.split("&")) {
            int idx = kv.indexOf('=');
            if (idx > 0) {
              String name = URLDecoder.decode(kv.substring(0, idx), "UTF-8");
              String value = URLDecoder.decode(kv.substring(idx+1), "UTF-8");
              log.debug("SolrIndex.parseQueryString - add the name " + name + " and value " + value +" pair to the prama map");
              MultiMapSolrParams.addParam(name, value, map);
            } else {
              String name = URLDecoder.decode(kv, "UTF-8");
              log.debug("SolrIndex.parseQueryString - add the name " + name + " to the prama map");
              MultiMapSolrParams.addParam(name, "", map);
            }
          }
        } catch (UnsupportedEncodingException uex) {
          throw new SolrException(SolrException.ErrorCode.SERVER_ERROR, uex);
        }
      }
      return new MultiMapSolrParams(map);
    }
    
    /**
     * Use the default GET method to handle the query when the query is on the key/value format
     * @param solrParams  the query with the key/value format
     * @param authorizedSubjects  the authorized subjects in this query session
     * @param isMNadmin  the indicator of the authorized subjects are the mn admin or not
     * @param method  the method such as GET, POST and et al will be used in the query
     * @return the query result as the InputStream object
     * @throws SolrServerException
     * @throws IOException
     * @throws PropertyNotFoundException
     * @throws SQLException
     * @throws ClassNotFoundException
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws NotImplemented
     * @throws NotFound
     * @throws UnsupportedType
     */
    public InputStream query(SolrParams solrParams, Set<Subject>authorizedSubjects, boolean isMNadmin) throws SolrServerException, IOException, PropertyNotFoundException, SQLException, 
    ClassNotFoundException, ParserConfigurationException, SAXException, NotImplemented, NotFound, UnsupportedType {
        return query(solrParams, authorizedSubjects, isMNadmin, SolrRequest.METHOD.GET);
        
    }
    
    /**
     * Handle the query when the query is on the key/value format
     * @param solrParams  the query with the key/value format
     * @param authorizedSubjects  the authorized subjects in this query session
     * @param isMNadmin  the indicator of the authorized subjects are the mn admin or not
     * @param method  the method such as GET, POST and et al will be used in the query
     * @return the query result as the InputStream object
     * @throws SolrServerException
     * @throws IOException
     * @throws PropertyNotFoundException
     * @throws SQLException
     * @throws ClassNotFoundException
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws NotImplemented
     * @throws NotFound
     * @throws UnsupportedType
     */
    public InputStream query(SolrParams solrParams, Set<Subject>authorizedSubjects, boolean isMNadmin, SolrRequest.METHOD method) throws SolrServerException, IOException, PropertyNotFoundException, SQLException, 
    ClassNotFoundException, ParserConfigurationException, SAXException, NotImplemented, NotFound, UnsupportedType {
        if(authorizedSubjects == null || authorizedSubjects.isEmpty()) {
            //throw new SolrServerException("MetacatSolrIndex.query - There is no any authorized subjects(even the public user) in this query session.");
            Subject subject = new Subject();
            subject.setValue(Constants.SUBJECT_PUBLIC);
            authorizedSubjects = new HashSet<Subject>();
            authorizedSubjects.add(subject);
        }
        if(isMNadmin) {
            authorizedSubjects = null;//bypass the access rule since it is mn admin
            log.debug("MetacatSolrIndex.query - this is the mn admin object and the query will bypass the access controls rules.");
        }
        InputStream inputStream = null;
        String wt = solrParams.get(SolrQueryService.WT);
        // handle normal and skin-based queries
        if (SolrQueryService.isSupportedWT(wt)) {
            // just handle as normal solr query
            inputStream = SolrQueryServiceController.getInstance().query(solrParams, authorizedSubjects, method);
        }
        else {
            // assume it is a skin name
            String qformat = wt;
            
            // perform the solr query using wt=XML
            wt = SolrQueryResponseWriterFactory.XML;
            ModifiableSolrParams msp = new ModifiableSolrParams(solrParams);
            msp.set(SolrQueryService.WT, wt);
            inputStream = SolrQueryServiceController.getInstance().query(msp, authorizedSubjects, method);
            
            // apply the stylesheet (XML->HTML)
            DBTransform transformer = new DBTransform();
            String documentContent = IOUtils.toString(inputStream, "UTF-8");
            String sourceType = "solr";
            String targetType = "-//W3C//HTML//EN";
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Writer writer = new OutputStreamWriter(baos , "UTF-8");
            // TODO: include more params?
            Hashtable<String, String[]> params = new Hashtable<String, String[]>();
            params.put("qformat", new String[] {qformat});
            transformer.transformXMLDocument(
                    documentContent , 
                    sourceType, 
                    targetType , 
                    qformat, 
                    writer, 
                    params, 
                    null //sessionid
                    );
            
            // finally, get the HTML back
            inputStream = new ContentTypeByteArrayInputStream(baos.toByteArray());
            ((ContentTypeByteArrayInputStream) inputStream).setContentType("text/html");
        }
        
        return inputStream;
     
    }

   
    /**
     * Submit a deleting-index task
     * @param pid the pid's solr document will be deleted.
     */
    public void submitDeleteTask(Identifier pid, SystemMetadata sysMeta) {
        if (nodeType == null || !nodeType.equalsIgnoreCase("mn")) {
            //only works for MNs
            log.info("MetacatSolrIndex.submit - The node is not configured as a member node. So the object  " + pid.getValue() +
                    " will not be submitted into the index queue on the RabbitMQ service.");
            return;
        }
        if(pid != null) {
            log.debug("MetacatSolrIndex.submitDeleteTask - will put the pid " + pid.getValue() + " into the index queue on the RabbitMQ service.");
            try {
                String type = RabbitMQService.DELETE_INDEX_TYPE;
                RabbitMQService.getInstance().publishToIndexQueue(pid, type, null);
                log.info("MetacatSolrIndex.submitDeleteTask - put the pid " + pid.getValue() + " with the index type " + type + "into the index queue on the RabbitMQ service successfully.");
            } catch (ServiceException e) {
                log.error("MetacatSolrIndex.submitDeleteTask - can NOT put the pid " +  pid.getValue() + " into the index queue on the RabbitMQ service since: " + e.getCoreMessage());
            }
        }
    }
    
    /**
     * Submit the index task to the index queue
     * @param pid  the pid will be indexed
     * @param systemMetadata  the system metadata associated with the pid
     * @param fields  extra fields which need to be indexed 
     * @param followRevisions .. if the obsoleted version will be indexed
     */
    public void submit(Identifier pid, SystemMetadata systemMetadata, Map<String, List<Object>> fields, boolean followRevisions) {
        boolean isSysmetaChangeOnly = false;
        submit(pid, systemMetadata, isSysmetaChangeOnly, fields, followRevisions);
    }
    
    
    /**
     * Submit a index task into the index queue
     * @param pid  the pid of the object which will be indexed
     * @param systemMetadata  the system metadata associated with pid
     * @param isSysmetaChangeOnly  if this is the event of system metadata change only
     * @param fields  extra fields which need to be indexed 
     * @param followRevisions  if the obsoleted version will be indexed
     */
    public void submit(Identifier pid, SystemMetadata systemMetadata, boolean isSysmetaChangeOnly, Map<String, List<Object>> fields, boolean followRevisions) {
        if (nodeType == null || !nodeType.equalsIgnoreCase("mn")) {
            //only works for MNs
            log.info("MetacatSolrIndex.submit - The node is not configured as a member node. So the object  " + pid.getValue() +
                     " will not be submitted into the index queue on the RabbitMQ service.");
            return;
        }
        String type = RabbitMQService.CREATE_INDEXT_TYPE;
        if (isSysmetaChangeOnly) {
            type = RabbitMQService.SYSMETA_CHANGE_TYPE;
        }
        try {
            RabbitMQService.getInstance().publishToIndexQueue(pid, type, systemMetadata);
            log.info("MetacatSolrIndex.submit - put the pid " + pid.getValue() + " with type " + type + " into the index queue on the RabbitMQ service successfully.");
        } catch (ServiceException e) {
            log.error("MetacatSolrIndex.submitTask - can NOT put the pid " +  pid.getValue() + " into the index queue on the RabbitMQ service since: " + e.getCoreMessage());
        }
        // submit older revisions recursively otherwise they stay in the index!
		if (followRevisions && systemMetadata != null && systemMetadata.getObsoletes() != null) {
			Identifier obsoletedPid = systemMetadata.getObsoletes();
			SystemMetadata obsoletedSysMeta = HazelcastService.getInstance().getSystemMetadataMap().get(obsoletedPid);
		    Map<String, List<Object>> obsoletedFields = null;
			this.submit(obsoletedPid, obsoletedSysMeta , obsoletedFields, followRevisions);
		}
    }
    
    

}
