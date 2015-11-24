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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;



import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.solr.common.util.XML;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.SolrCore;
import org.apache.solr.schema.IndexSchema;
import org.apache.solr.schema.SchemaField;
import org.dataone.service.exceptions.NotFound;
import org.dataone.service.exceptions.UnsupportedType;
import org.dataone.service.types.v1_1.QueryEngineDescription;
import org.dataone.service.types.v1_1.QueryField;
import org.xml.sax.SAXException;

import edu.ucsb.nceas.metacat.common.SolrServerFactory;
import edu.ucsb.nceas.metacat.common.query.EnabledQueryEngines;
import edu.ucsb.nceas.metacat.common.query.SolrQueryServiceController;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.utilities.FileUtil;


/**
 * This class handles the request for getting the solr engine description.
 * @author tao
 *
 */
public class MetacatSolrEngineDescriptionHandler {
    private static final String UNKNOWN = "Unknown";
    private static final String DESCRIPTIONFILENAME= "solrQueryFieldDescriptions.properties";
    private static final String HTTPSOLRSERVERSCHEMAURLPATH="/admin/file/?contentType=text/xml;charset=utf-8&file=schema.xml";
    
    private static MetacatSolrEngineDescriptionHandler handler = null;
    private static Log logger = LogFactory.getLog(MetacatSolrEngineDescriptionHandler.class);
    private QueryEngineDescription qed = null;
    
    /**
     * Get an instance of the class.
     * @return
     */
    public static MetacatSolrEngineDescriptionHandler getInstance() throws Exception {
        if(handler == null) {
            handler = new MetacatSolrEngineDescriptionHandler();
        }
        return handler;
    }
    
    /**
     * Get the QueryEngineDescription
     * @return
     */
    public QueryEngineDescription getQueryEngineDescritpion() {
        return qed;
    }
    
    /*
     * Constructor
     */
    private MetacatSolrEngineDescriptionHandler() throws Exception {
       /*CoreContainer container = SolrServerFactory.getCoreContainer();
       if(container == null) {
           throw new Exception("MetacatSolrEngineDescriptionHandler - The Solr Server is not configured as an EmbeddedSolrServer and the EmbeddedSolrServer is the only SolrServer that the Metacat can provide the Query Engine Description.");
       }
       String coreName = SolrServerFactory.getCollectionName();
       if(container == null) {
           throw new Exception("MetacatSolrEngineDescriptionHandler - The collection name should not be null. Please check the value of the property \"solr.collectionName\" in the metacat.properties file.");
       }
       SolrCore core = container.getCore(coreName);
       if(core == null) {
           throw new Exception("MetacatSolrEngineDescriptionHandler - Metacat can't find the SolrCore for the given name - "+SolrServerFactory.getCollectionName());
       }*/
       init();
    }
    
   
    
    private void init() throws IOException, UnsupportedType, NotFound, ParserConfigurationException, SAXException {
        Map<String, String>fieldDescriptions = loadSchemaFieldDescriptions();
        qed = new QueryEngineDescription();
        qed.setName(EnabledQueryEngines.SOLRENGINE);
        //setSchemaVersionFromPropertiesFile(qed);
        setSolrVersion();

        //IndexSchema schema = core.getSchema();
        Map<String, SchemaField> fieldMap = SolrQueryServiceController.getInstance().getIndexSchemaFields();
        for (SchemaField schemaField : fieldMap.values()) {
            qed.addQueryField(createQueryFieldFromSchemaField(schemaField, fieldDescriptions));
        }
        Collections.sort(qed.getQueryFieldList(), new QueryFieldAlphaComparator());
    }
    
    /**
     * Based on org.apache.solr.handler.admin.SystemInfoHandler.getLuceneInfo()
     * @throws SAXException 
     * @throws IOException 
     * @throws ParserConfigurationException 
     * @throws NotFound 
     * @throws UnsupportedType 
     */
    private void setSolrVersion() throws UnsupportedType, NotFound, ParserConfigurationException, IOException, SAXException {
        qed.setQueryEngineVersion(SolrQueryServiceController.getInstance().getSolrSpecVersion());
    }
    

   
    
    private Map<String, String> loadSchemaFieldDescriptions() throws IOException {
        Map<String, String>fieldDescriptions = new HashMap<String, String>();
        Properties descriptionProperties = new Properties();
        String propertyFilePath = PropertyService.CONFIG_FILE_DIR + FileUtil.getFS() + DESCRIPTIONFILENAME;
        //System.out.println("the input strema is ================= "+propertyFilePath);
        descriptionProperties.load(new FileInputStream(new File(propertyFilePath)));
        Set<Object> names = descriptionProperties.keySet();
        for(Object nameObj : names) {
            String name = (String) nameObj;
            String description = (String) descriptionProperties.get(name);
            fieldDescriptions.put(name, description);
        }
        return fieldDescriptions;
         
    }
    
    private QueryField createQueryFieldFromSchemaField(SchemaField field,
                    Map<String, String> fieldDescriptions) {
                QueryField queryField = new QueryField();
                queryField.setName(field.getName());
                queryField.setType(field.getType().getTypeName());
                String description = fieldDescriptions.get(field.getName());
                if ( description != null && !description.trim().equals("")) {
                    queryField.addDescription(description);
                }
                queryField.setSearchable(field.indexed());
                queryField.setReturnable(field.stored());
                queryField.setMultivalued(field.multiValued());
                queryField.setSortable(isSortable(field));
                return queryField;
            }

    private boolean isSortable(SchemaField field) {
                String type = field.getType().getTypeName();
                if ("int".equals(type) || "long".equals(type) || "float".equals(type)
                        || "double".equals(type)) {
                    return false;
                } else {
                    return true;
                }
    }
    
    private class QueryFieldAlphaComparator implements Comparator<QueryField> {
        public int compare(QueryField arg0, QueryField arg1) {
            String field1Name = arg0.getName();
            String field2Name = arg1.getName();
            return field1Name.compareToIgnoreCase(field2Name);
        }
    }
}
