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
import java.io.Writer;
import java.sql.SQLException;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.params.SolrParams;

import org.apache.solr.servlet.SolrRequestParsers;
import org.dataone.service.exceptions.NotFound;
import org.dataone.service.exceptions.NotImplemented;
import org.dataone.service.exceptions.UnsupportedType;
import org.dataone.service.types.v1.Event;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Subject;
import org.dataone.service.types.v2.SystemMetadata;
import org.xml.sax.SAXException;

import edu.ucsb.nceas.metacat.DBTransform;
import edu.ucsb.nceas.metacat.EventLog;
import edu.ucsb.nceas.metacat.common.index.IndexTask;
import edu.ucsb.nceas.metacat.common.query.SolrQueryResponseWriterFactory;
import edu.ucsb.nceas.metacat.common.query.SolrQueryService;
import edu.ucsb.nceas.metacat.common.query.SolrQueryServiceController;
import edu.ucsb.nceas.metacat.common.query.stream.ContentTypeByteArrayInputStream;
import edu.ucsb.nceas.metacat.dataone.hazelcast.HazelcastService;
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
    	
    }
    
    
    
    
    /**
     * Query the solr server
     * @param query  the solr query
     * @param authorizedSubjects the authorized subjects in this query session
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
    public InputStream query(String query, Set<Subject>authorizedSubjects) throws SolrServerException, IOException, PropertyNotFoundException, SQLException, 
    ClassNotFoundException, ParserConfigurationException, SAXException, NotImplemented, NotFound, UnsupportedType {
        if(authorizedSubjects == null || authorizedSubjects.isEmpty()) {
            throw new SolrServerException("MetacatSolrIndex.query - There is no any authorized subjects(even the public user) in this query session.");
        }
        InputStream inputStream = null;
        // allow "+" in query syntax, see: https://projects.ecoinformatics.org/ecoinfo/issues/6435
        query = query.replaceAll("\\+", "%2B");
        SolrParams solrParams = SolrRequestParsers.parseQueryString(query);
        String wt = solrParams.get(SolrQueryService.WT);
        // handle normal and skin-based queries
        if (SolrQueryService.isSupportedWT(wt)) {
            // just handle as normal solr query
           
            inputStream = SolrQueryServiceController.getInstance().query(solrParams, authorizedSubjects);
        }
        else {
            // assume it is a skin name
            String qformat = wt;
            
            // perform the solr query using wt=XML
            wt = SolrQueryResponseWriterFactory.XML;
            ModifiableSolrParams msp = new ModifiableSolrParams(solrParams);
            msp.set(SolrQueryService.WT, wt);
            inputStream = SolrQueryServiceController.getInstance().query(msp, authorizedSubjects);
            
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

   
    
    public void submit(Identifier pid, SystemMetadata systemMetadata, Map<String, List<Object>> fields, boolean followRevisions) {
    	IndexTask task = new IndexTask();
    	task.setSystemMetadata(systemMetadata);
    	task.setFields(fields);
		HazelcastService.getInstance().getIndexQueue().put(pid, task);
		
		// submit older revisions recursively otherwise they stay in the index!
		if (followRevisions && systemMetadata != null && systemMetadata.getObsoletes() != null) {
			Identifier obsoletedPid = systemMetadata.getObsoletes();
			SystemMetadata obsoletedSysMeta = HazelcastService.getInstance().getSystemMetadataMap().get(obsoletedPid);
		    Map<String, List<Object>> obsoletedFields = EventLog.getInstance().getIndexFields(obsoletedPid, Event.READ.xmlValue());
			this.submit(obsoletedPid, obsoletedSysMeta , obsoletedFields, followRevisions);
		}
    }
    
    

}
