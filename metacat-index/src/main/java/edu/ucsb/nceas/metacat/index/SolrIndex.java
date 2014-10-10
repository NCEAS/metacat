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
package edu.ucsb.nceas.metacat.index;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.codec.EncoderException;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.schema.IndexSchema;
import org.dataone.cn.index.task.IndexTask;
import org.dataone.cn.indexer.XMLNamespaceConfig;
import org.dataone.cn.indexer.convert.SolrDateConverter;
import org.dataone.cn.indexer.parser.IDocumentSubprocessor;
import org.dataone.cn.indexer.parser.SolrField;
import org.dataone.cn.indexer.resourcemap.ResourceEntry;
import org.dataone.cn.indexer.resourcemap.ResourceMap;
import org.dataone.cn.indexer.resourcemap.ResourceMapFactory;
import org.dataone.cn.indexer.solrhttp.SolrDoc;
import org.dataone.cn.indexer.solrhttp.SolrElementAdd;
import org.dataone.cn.indexer.solrhttp.SolrElementField;
import org.dataone.service.exceptions.NotFound;
import org.dataone.service.exceptions.NotImplemented;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.exceptions.UnsupportedType;
import org.dataone.service.types.v1.Event;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.SystemMetadata;
import org.dataone.service.util.DateTimeMarshaller;
import org.dataone.service.util.TypeMarshaller;
import org.dspace.foresite.OREParserException;
import org.jibx.runtime.JiBXException;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import edu.ucsb.nceas.metacat.common.index.event.IndexEvent;
import edu.ucsb.nceas.metacat.common.query.SolrQueryServiceController;
import edu.ucsb.nceas.metacat.index.event.EventlogFactory;
import edu.ucsb.nceas.metacat.index.resourcemap.ResourceMapSubprocessor;

/**
 * A class does insert, update and remove indexes to a SOLR server
 * @author tao
 *
 */
public class SolrIndex {
            
    public static final String ID = "id";
    private static final String IDQUERY = ID+":*";
    private List<IDocumentSubprocessor> subprocessors = null;
    private SolrServer solrServer = null;
    private XMLNamespaceConfig xmlNamespaceConfig = null;
    private List<SolrField> sysmetaSolrFields = null;

    private static DocumentBuilderFactory documentBuilderFactory = null;
    private static DocumentBuilder builder = null;

    private static XPathFactory xpathFactory = null;
    private static XPath xpath = null;
    Log log = LogFactory.getLog(SolrIndex.class);
    
    static {
        documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setNamespaceAware(true);
        try {
            builder = documentBuilderFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }
        xpathFactory = XPathFactory.newInstance();
        xpath = xpathFactory.newXPath();
    }
    
    /**
     * Constructor
     * @throws SAXException 
     * @throws IOException 
     */
    public SolrIndex(XMLNamespaceConfig xmlNamespaceConfig, List<SolrField> sysmetaSolrFields)
                    throws XPathExpressionException, ParserConfigurationException, IOException, SAXException {
         this.xmlNamespaceConfig = xmlNamespaceConfig;
         this.sysmetaSolrFields = sysmetaSolrFields;
         init();
    }
    
    private void init() throws ParserConfigurationException, XPathExpressionException {
        xpath.setNamespaceContext(xmlNamespaceConfig);
        initExpressions();
    }

    private void initExpressions() throws XPathExpressionException {
        for (SolrField field : sysmetaSolrFields) {
            field.initExpression(xpath);
        }

    }
    
    
    /**
     * Get the list of the Subprocessors in this index.
     * @return the list of the Subprocessors.
     */
    public List<IDocumentSubprocessor> getSubprocessors() {
        return subprocessors;
    }

    /**
     * Set the list of Subprocessors.
     * @param subprocessorList  the list will be set.
     */
    public void setSubprocessors(List<IDocumentSubprocessor> subprocessorList) {
        for (IDocumentSubprocessor subprocessor : subprocessorList) {
            subprocessor.initExpression(xpath);
        }
        this.subprocessors = subprocessorList;
    }
    
    /**
     * Generate the index for the given information
     * @param id
     * @param systemMetadata
     * @param dataStream
     * @return
     * @throws IOException
     * @throws SAXException
     * @throws ParserConfigurationException
     * @throws XPathExpressionException
     * @throws JiBXException 
     * @throws SolrServerException 
     * @throws EncoderException
     * @throws UnsupportedType 
     * @throws NotFound 
     * @throws NotImplemented 
     */
    private Map<String, SolrDoc> process(String id, SystemMetadata systemMetadata, InputStream dataStream)
                    throws IOException, SAXException, ParserConfigurationException,
                    XPathExpressionException, JiBXException, EncoderException, SolrServerException, NotImplemented, NotFound, UnsupportedType{

        // Load the System Metadata document
        ByteArrayOutputStream systemMetadataOutputStream = new ByteArrayOutputStream();
        TypeMarshaller.marshalTypeToOutputStream(systemMetadata, systemMetadataOutputStream);
        ByteArrayInputStream systemMetadataStream = new ByteArrayInputStream(systemMetadataOutputStream.toByteArray());
        Document sysMetaDoc = generateXmlDocument(systemMetadataStream);
        if (sysMetaDoc == null) {
            log.error("Could not load System metadata for ID: " + id);
            return null;
        }

        // Extract the field values from the System Metadata
        List<SolrElementField> sysSolrFields = processSysmetaFields(sysMetaDoc, id);
        SolrDoc indexDocument = new SolrDoc(sysSolrFields);
        Map<String, SolrDoc> docs = new HashMap<String, SolrDoc>();
        docs.put(id, indexDocument);

        // Determine if subprocessors are available for this ID
        if (subprocessors != null) {
                    // for each subprocessor loaded from the spring config
                    for (IDocumentSubprocessor subprocessor : subprocessors) {
                        // Does this subprocessor apply?
                        if (subprocessor.canProcess(sysMetaDoc)) {
                            // if so, then extract the additional information from the
                            // document.
                            try {
                                // docObject = the resource map document or science
                                // metadata document.
                                // note that resource map processing touches all objects
                                // referenced by the resource map.
                                Document docObject = generateXmlDocument(dataStream);
                                if (docObject == null) {
                                    throw new Exception("Could not load OBJECT for ID " + id );
                                } else {
                                    docs = subprocessor.processDocument(id, docs, docObject);
                                }
                            } catch (Exception e) {
                                log.error(e.getMessage(), e);
                                throw new SolrServerException(e.getMessage());
                            }
                        }
                    }
       }

       // TODO: in the XPathDocumentParser class in d1_cn_index_process module,
       // merge is only for resource map. We need more work here.
       for (SolrDoc mergeDoc : docs.values()) {
           if (!mergeDoc.isMerged()) {
                 mergeWithIndexedDocument(mergeDoc);
           }
       }

       //SolrElementAdd addCommand = getAddCommand(new ArrayList<SolrDoc>(docs.values()));
               
       return docs;
    }
    
    /**
     * Merge updates with existing solr documents
     * 
     * This method appears to re-set the data package field data into the
     * document about to be updated in the solr index. Since packaging
     * information is derived from the package document (resource map), this
     * information is not present when processing a document contained in a data
     * package. This method replaces those values from the existing solr index
     * record for the document being processed. -- sroseboo, 1-18-12
     * 
     * @param indexDocument
     * @return
     * @throws IOException
     * @throws EncoderException
     * @throws XPathExpressionException
     * @throws SAXException 
     * @throws ParserConfigurationException 
     * @throws SolrServerException 
     * @throws UnsupportedType 
     * @throws NotFound 
     * @throws NotImplemented 
     */
    // TODO:combine merge function with resourcemap merge function

    private SolrDoc mergeWithIndexedDocument(SolrDoc indexDocument) throws IOException,
            EncoderException, XPathExpressionException, SolrServerException, ParserConfigurationException, SAXException, NotImplemented, NotFound, UnsupportedType {
        List<String> ids = new ArrayList<String>();
        ids.add(indexDocument.getIdentifier());
        List<SolrDoc> indexedDocuments = ResourceMapSubprocessor.getSolrDocs(ids);
        SolrDoc indexedDocument = indexedDocuments == null || indexedDocuments.size() <= 0 ? null
                : indexedDocuments.get(0);
        
        IndexSchema indexSchema = SolrQueryServiceController.getInstance().getSchema();

        if (indexedDocument == null || indexedDocument.getFieldList().size() <= 0) {
            return indexDocument;
        } else {
            for (SolrElementField field : indexedDocument.getFieldList()) {
                if ((field.getName().equals(SolrElementField.FIELD_ISDOCUMENTEDBY)
                        || field.getName().equals(SolrElementField.FIELD_DOCUMENTS) || field
                        .getName().equals(SolrElementField.FIELD_RESOURCEMAP))
                        && !indexDocument.hasFieldWithValue(field.getName(), field.getValue())) {
                    indexDocument.addField(field);
                } else if (!indexSchema.isCopyFieldTarget(indexSchema.getField(field.getName())) && !indexDocument.hasField(field.getName())) {
                    indexDocument.addField(field);
                }
            }

            indexDocument.setMerged(true);
            return indexDocument;
        }
    }
    
    /*
     * Generate a Document from the InputStream
     */
    private Document generateXmlDocument(InputStream smdStream) throws SAXException {
        Document doc = null;

        try {
            doc = builder.parse(smdStream);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }

        return doc;
    }
    
    /*
     * Index the fields of the system metadata
     */
    private List<SolrElementField> processSysmetaFields(Document doc, String identifier) {

        List<SolrElementField> fieldList = new ArrayList<SolrElementField>();
        // solrFields is the list of fields defined in the application context
       
        for (SolrField field : sysmetaSolrFields) {
            try {
                // the field.getFields method can return a single value or
                // multiple values for multi-valued fields
                // or can return multiple SOLR document fields.
                fieldList.addAll(field.getFields(doc, identifier));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return fieldList;

    }
    
    /**
     * Check the parameters of the insert or update methods.
     * @param pid
     * @param systemMetadata
     * @param data
     * @throws SolrServerException
     */
    private void checkParams(Identifier pid, SystemMetadata systemMetadata, InputStream data) throws SolrServerException {
        if(pid == null || pid.getValue() == null || pid.getValue().trim().equals("")) {
            throw new SolrServerException("The identifier of the indexed document should not be null or blank.");
        }
        if(systemMetadata == null) {
            throw new SolrServerException("The system metadata of the indexed document "+pid.getValue()+ " should not be null.");
        }
        if(data == null) {
            throw new SolrServerException("The indexed document itself for pid "+pid.getValue()+" should not be null.");
        }
    }
    
    /**
     * Insert the indexes for a document.
     * @param pid  the id of this document
     * @param systemMetadata  the system metadata associated with the data object
     * @param data  the data object itself
     * @throws SolrServerException 
     * @throws JiBXException 
     * @throws EncoderException 
     * @throws UnsupportedType 
     * @throws NotFound 
     * @throws NotImplemented 
     */
    private synchronized void insert(Identifier pid, SystemMetadata systemMetadata, InputStream data) 
                    throws IOException, SAXException, ParserConfigurationException,
                    XPathExpressionException, SolrServerException, JiBXException, EncoderException, NotImplemented, NotFound, UnsupportedType {
        checkParams(pid, systemMetadata, data);
        Map<String, SolrDoc> docs = process(pid.getValue(), systemMetadata, data);
        
        //transform the Map to the SolrInputDocument which can be used by the solr server
        if(docs != null) {
            Set<String> ids = docs.keySet();
            for(String id : ids) {
                if(id != null) {
                    SolrDoc doc = docs.get(id);
                    insertToIndex(doc);
                }
                
            }
        }
    }
    
    /**
     * Adds the given fields to the solr index for the given pid, preserving the index values
     * that previously existed
     * @param pid
     * @param fields
     */
    public void insertFields(Identifier pid, Map<String, List<Object>> fields) {
    	
    	try {
			// copy the original values already indexed for this document	
	    	SolrQuery query = new SolrQuery("id:\"" + pid.getValue() + "\"");
	    	QueryResponse res = solrServer.query(query);
	    	SolrDoc doc = new SolrDoc();
	    	
	    	// include existing values if they exist
	        if (res.getResults().size() > 0) {
		        SolrDocument orig = res.getResults().get(0);
		        IndexSchema indexSchema = SolrQueryServiceController.getInstance().getSchema();
		    	for (String fieldName: orig.getFieldNames()) {
		        	//  don't transfer the copyTo fields, otherwise there are errors
		        	if (indexSchema.isCopyFieldTarget(indexSchema.getField(fieldName))) {
		        		continue;
		        	}
		        	for (Object value: orig.getFieldValues(fieldName)) {
		        		String stringValue = value.toString();
		        		// special handling for dates in ISO 8601
		        		if (value instanceof Date) {
		        			stringValue = DateTimeMarshaller.serializeDateToUTC((Date)value);
		        			SolrDateConverter converter = new SolrDateConverter();
		        			stringValue = converter.convert(stringValue);
		        		}
						SolrElementField field = new SolrElementField(fieldName, stringValue);
						log.debug("Adding field: " + fieldName);
						doc.addField(field);
		        	}
		        }
	        }
	    	
	        // add the additional fields we are trying to include in the index
	        for (String fieldName: fields.keySet()) {
	    		List<Object> values = fields.get(fieldName);
	    		for (Object value: values) {
	    	    	doc.updateOrAddField(fieldName, value.toString());
	    		}
	    	}
	        
	        // make sure there is an id in the solrdoc so it is added to the index
	        if (!doc.hasField(ID)) {
	        	doc.updateOrAddField(ID, pid.getValue());
	        }
	        
	        // insert the whole thing
	        insertToIndex(doc);
    	} catch (Exception e) {
    		String error = "SolrIndex.insetFields - could not update the solr index: " + e.getMessage();
            writeEventLog(null, pid, error);
            log.error(error, e);
    	}

    }
    
    /*
     * Insert a SolrDoc to the solr server.
     */
    private synchronized void insertToIndex(SolrDoc doc) throws SolrServerException, IOException {
        if(doc != null ) {
            SolrInputDocument solrDoc = new SolrInputDocument();
            List<SolrElementField> list = doc.getFieldList();
            if(list != null) {
                //solrDoc.addField(METACATPIDFIELD, pid);
                Iterator<SolrElementField> iterator = list.iterator();
                while (iterator.hasNext()) {
                    SolrElementField field = iterator.next();
                    if(field != null) {
                        String value = field.getValue();
                        String name = field.getName();
                        //System.out.println("add name/value pair - "+name+"/"+value);
                        solrDoc.addField(name, value);
                    }
                }
            }
            if(!solrDoc.isEmpty()) {
                /*IndexEvent event = new IndexEvent();
                event.setDate(Calendar.getInstance().getTime());
                Identifier pid = new Identifier();
                pid.setValue(doc.getIdentifier());
                event.setIdentifier(pid);*/
                try {
                    UpdateResponse response = solrServer.add(solrDoc);
                    solrServer.commit();
                    /*event.setType(IndexEvent.SUCCESSINSERT);
                    event.setDescription("Successfully insert the solr index for the id "+pid.getValue());
                    try {
                        EventlogFactory.createIndexEventLog().write(event);
                    } catch (Exception e) {
                        log.error("SolrIndex.insertToIndex - IndexEventLog can't log the index inserting event :"+e.getMessage());
                    }*/
                } catch (SolrServerException e) {
                    /*event.setAction(Event.CREATE);
                    event.setDescription("Failed to insert the solr index for the id "+pid.getValue()+" since "+e.getMessage());
                    try {
                        EventlogFactory.createIndexEventLog().write(event);
                    } catch (Exception ee) {
                        log.error("SolrIndex.insertToIndex - IndexEventLog can't log the index inserting event :"+ee.getMessage());
                    }*/
                    throw e;
                } catch (IOException e) {
                    /*event.setAction(Event.CREATE);
                    event.setDescription("Failed to insert the solr index for the id "+pid.getValue()+" since "+e.getMessage());
                    try {
                        EventlogFactory.createIndexEventLog().write(event);
                    } catch (Exception ee) {
                        log.error("SolrIndex.insertToIndex - IndexEventLog can't log the index inserting event :"+ee.getMessage());
                    }*/
                    throw e;
                    
                }
                //System.out.println("=================the response is:\n"+response.toString());
            }
        }
    }
    
    /**
     * Update the solr index. This method handles the three scenarios:
     * 1. Remove an existing doc - if the the system metadata shows the value of the archive is true,
     *    remove the index for the previous version(s) and generate new index for the doc.
     * 2. Add a new doc - if the system metadata shows the value of the archive is false, generate the
     *    index for the doc.
     */
    public void update(Identifier pid, SystemMetadata systemMetadata) {
        if(systemMetadata==null || pid==null) {
        	log.error("SolrIndex.update - the systemMetadata or pid is null. So nothing will be indexed.");
        	return;
        }
    	String objectPath = null;
        InputStream data = null;
        try {
            if(!systemMetadata.getArchived()) {
                objectPath = DistributedMapsFactory.getObjectPathMap().get(pid);
            	data = new FileInputStream(objectPath);
            }            
            update(pid, systemMetadata, data);
            EventlogFactory.createIndexEventLog().remove(pid);
        } catch (Exception e) {
            String error = "SolrIndex.update - could not update the solr index since " + e.getMessage();
            writeEventLog(systemMetadata, pid, error);
            log.error(error, e);
        }
    }
    
  
    
    
    /**
     * Update the solr index. This method handles the three scenarios:
     * 1. Remove an existing doc - if the the system metadata shows the value of the archive is true,
     *    remove the index for the previous version(s) and generate new index for the doc.
     * 2. Add a new doc - if the system metadata shows the value of the archive is false, generate the
     *    index for the doc.
     * @param pid
     * @param systemMetadata
     * @param data
     * @throws SolrServerException
     * @throws ServiceFailure
     * @throws XPathExpressionException
     * @throws NotImplemented
     * @throws NotFound
     * @throws UnsupportedType
     * @throws IOException
     * @throws SAXException
     * @throws ParserConfigurationException
     * @throws OREParserException
     * @throws JiBXException
     * @throws EncoderException
     */
    void update(Identifier pid, SystemMetadata systemMetadata, InputStream data) throws SolrServerException, 
                                ServiceFailure, XPathExpressionException, NotImplemented, NotFound, UnsupportedType, 
                                IOException, SAXException, ParserConfigurationException, OREParserException, JiBXException, EncoderException { 
    	if(systemMetadata==null || pid==null) {
        	log.error("SolrIndex.update - the systemMetadata or pid is null. So nothing will be indexed.");
        }
        boolean isArchive = systemMetadata.getArchived();
        if(isArchive ) {
            //delete the index for the archived objects
            remove(pid.getValue(), systemMetadata);
            log.info("SolrIndex.update============================= archive the idex for the identifier "+pid);
        } else {
        	checkParams(pid, systemMetadata, data);
            //generate index for either add or update.
            insert(pid, systemMetadata, data);
            log.info("SolrIndex.update============================= insert index for the identifier "+pid);
        }
    }
    
   

    /*
     * Is the pid a resource map
     */
    private boolean isDataPackage(String pid, SystemMetadata sysmeta) throws FileNotFoundException, ServiceFailure {
        boolean isDataPackage = false;
        //SystemMetadata sysmeta = DistributedMapsFactory.getSystemMetadata(pid);
        if(sysmeta != null) {
            isDataPackage = IndexGeneratorTimerTask.isResourceMap(sysmeta.getFormatId());
        }
        return isDataPackage;
    }

    private boolean isPartOfDataPackage(String pid) throws XPathExpressionException, NotImplemented, NotFound, UnsupportedType, SolrServerException, IOException, ParserConfigurationException, SAXException {
        SolrDoc dataPackageIndexDoc = ResourceMapSubprocessor.getSolrDoc(pid);
        if (dataPackageIndexDoc != null) {
            String resourceMapId = dataPackageIndexDoc
                    .getFirstFieldValue(SolrElementField.FIELD_RESOURCEMAP);
            return StringUtils.isNotEmpty(resourceMapId);
        } else {
            return false;
        }
    }
    
   
    /**
     * Remove the indexed associated with specified pid.
     * @param pid  the pid which the indexes are associated with
     * @throws IOException
     * @throws SolrServerException
     * @throws ParserConfigurationException 
     * @throws SAXException 
     * @throws UnsupportedType 
     * @throws NotFound 
     * @throws NotImplemented 
     * @throws XPathExpressionException 
     * @throws ServiceFailure 
     * @throws OREParserException 
     */
    private void remove(String pid, SystemMetadata sysmeta) throws IOException, SolrServerException, ServiceFailure, XPathExpressionException, NotImplemented, NotFound, UnsupportedType, SAXException, ParserConfigurationException, OREParserException {
        if (isDataPackage(pid, sysmeta)) {
            removeDataPackage(pid);
        } else if (isPartOfDataPackage(pid)) {
            removeFromDataPackage(pid);
        } else {
            removeFromIndex(pid);
        }
    }
    
   
    
    /*
     * Remove the resource map from the solr index. It doesn't only remove the index for itself and also
     * remove the relationship for the related metadata and data objects.
     */
    private void removeDataPackage(String pid) throws  XPathExpressionException, IOException, 
            SolrServerException, UnsupportedType, NotFound, ParserConfigurationException, SAXException {
        removeFromIndex(pid);
        List<SolrDoc> docsToUpdate = getUpdatedSolrDocsByRemovingResourceMap(pid);
        if (docsToUpdate != null && !docsToUpdate.isEmpty()) {
            //SolrElementAdd addCommand = new SolrElementAdd(docsToUpdate);
            //httpService.sendUpdate(solrIndexUri, addCommand);
            for(SolrDoc doc : docsToUpdate) {
                removeFromIndex(doc.getIdentifier());
                insertToIndex(doc);
            }
        }

    }

    /*
     * Get the list of the solr doc which need to be updated because the removal of the resource map
     */
    private List<SolrDoc> getUpdatedSolrDocsByRemovingResourceMap(String resourceMapId)
            throws UnsupportedType, NotFound, SolrServerException, ParserConfigurationException, SAXException, MalformedURLException, IOException, XPathExpressionException {
        List<SolrDoc> updatedSolrDocs = null;
        if (resourceMapId != null && !resourceMapId.trim().equals("")) {
            /*List<SolrDoc> docsContainResourceMap = httpService.getDocumentsByResourceMap(
                    solrQueryUri, resourceMapId);*/
            List<SolrDoc> docsContainResourceMap = ResourceMapSubprocessor.getDocumentsByResourceMap(resourceMapId);
            updatedSolrDocs = removeResourceMapRelationship(docsContainResourceMap,
                    resourceMapId);
        }
        return updatedSolrDocs;
    }

    /*
     * Get the list of the solr doc which need to be updated because the removal of the resource map
     */
    private List<SolrDoc> removeResourceMapRelationship(List<SolrDoc> docsContainResourceMap,
            String resourceMapId) throws XPathExpressionException, IOException {
        List<SolrDoc> totalUpdatedSolrDocs = new ArrayList<SolrDoc>();
        if (docsContainResourceMap != null && !docsContainResourceMap.isEmpty()) {
            for (SolrDoc doc : docsContainResourceMap) {
                List<SolrDoc> updatedSolrDocs = new ArrayList<SolrDoc>();
                List<String> resourceMapIdStrs = doc
                        .getAllFieldValues(SolrElementField.FIELD_RESOURCEMAP);
                List<String> dataIdStrs = doc
                        .getAllFieldValues(SolrElementField.FIELD_DOCUMENTS);
                List<String> metadataIdStrs = doc
                        .getAllFieldValues(SolrElementField.FIELD_ISDOCUMENTEDBY);
                if ((dataIdStrs == null || dataIdStrs.isEmpty())
                        && (metadataIdStrs == null || metadataIdStrs.isEmpty())) {
                    // only has resourceMap field, doesn't have either documentBy or documents fields.
                    // so we only remove the resource map field.
                    doc.removeFieldsWithValue(SolrElementField.FIELD_RESOURCEMAP, resourceMapId);
                    updatedSolrDocs.add(doc);
                } else if ((dataIdStrs != null && !dataIdStrs.isEmpty())
                        && (metadataIdStrs == null || metadataIdStrs.isEmpty())) {
                    //The solr doc is for a metadata object since the solr doc documents data files
                    updatedSolrDocs = removeAggregatedItems(resourceMapId, doc, resourceMapIdStrs,
                            dataIdStrs, SolrElementField.FIELD_DOCUMENTS);
                } else if ((dataIdStrs == null || dataIdStrs.isEmpty())
                        && (metadataIdStrs != null && !metadataIdStrs.isEmpty())) {
                    //The solr doc is for a data object since it documentedBy elements.
                    updatedSolrDocs = removeAggregatedItems(resourceMapId, doc, resourceMapIdStrs,
                            metadataIdStrs, SolrElementField.FIELD_ISDOCUMENTEDBY);
                } else if ((dataIdStrs != null && !dataIdStrs.isEmpty())
                        && (metadataIdStrs != null && !metadataIdStrs.isEmpty())){
                    // both metadata and data for one object
                    List<SolrDoc> solrDocsRemovedDocuments = removeAggregatedItems(resourceMapId, doc, resourceMapIdStrs,
                            dataIdStrs, SolrElementField.FIELD_DOCUMENTS);
                    List<SolrDoc> solrDocsRemovedDocumentBy = removeAggregatedItems(resourceMapId, doc, resourceMapIdStrs,
                            metadataIdStrs, SolrElementField.FIELD_ISDOCUMENTEDBY);
                    updatedSolrDocs = mergeUpdatedSolrDocs(solrDocsRemovedDocumentBy, solrDocsRemovedDocuments);
                }
                //move them to the final result
                if(updatedSolrDocs != null) {
                    for(SolrDoc updatedDoc: updatedSolrDocs) {
                        totalUpdatedSolrDocs.add(updatedDoc);
                    }
                }
                
            }

        }
        return totalUpdatedSolrDocs;
    }
    
    /*
     * Process the list of ids of the documentBy/documents in a slor doc.
     */
    private List<SolrDoc> removeAggregatedItems(String targetResourceMapId, SolrDoc doc,
            List<String> resourceMapIdsInDoc, List<String> aggregatedItemsInDoc, String fieldNameRemoved) {
        List<SolrDoc> updatedSolrDocs = new ArrayList<SolrDoc>();
        if (doc != null && resourceMapIdsInDoc != null && aggregatedItemsInDoc != null
                && fieldNameRemoved != null) {
            if (resourceMapIdsInDoc.size() == 1) {
                //only has one resource map. remove the resource map. also remove the documentBy
                doc.removeFieldsWithValue(SolrElementField.FIELD_RESOURCEMAP, targetResourceMapId);
                doc.removeAllFields(fieldNameRemoved);
                updatedSolrDocs.add(doc);
            } else if (resourceMapIdsInDoc.size() > 1) {
                //we have multiple resource maps. We should match them.                     
                Map<String, String> ids = matchResourceMapsAndItems(doc.getIdentifier(),
                        targetResourceMapId, resourceMapIdsInDoc, aggregatedItemsInDoc, fieldNameRemoved);
                if (ids != null) {
                    for (String id : ids.keySet()) {
                        doc.removeFieldsWithValue(fieldNameRemoved, id);
                    }
                }
                doc.removeFieldsWithValue(SolrElementField.FIELD_RESOURCEMAP,
                        targetResourceMapId);
                updatedSolrDocs.add(doc);
                /*if (aggregatedItemsInDoc.size() > 1) {
                    

                } else {
                    //multiple resource map aggregate same metadata and data. Just remove the resource map
                    doc.removeFieldsWithValue(SolrElementField.FIELD_RESOURCEMAP,
                            targetResourceMapId);
                    updatedSolrDocs.add(doc);
                }*/
            }
        }
        return updatedSolrDocs;
    }

    /*
     * Return a map of mapping aggregation id map the target resourceMapId.
     * This will look the aggregation information in another side - If the targetId
     * is a metadata object, we will look the data objects which it describes; If 
     * the targetId is a data object, we will look the metadata object which documents it.
     */
    private Map<String, String> matchResourceMapsAndItems(String targetId,
            String targetResourceMapId, List<String> originalResourceMaps, List<String> aggregatedItems, String fieldName) {
        Map<String, String> map = new HashMap<String, String>();
        if (targetId != null && targetResourceMapId != null && aggregatedItems != null
                && fieldName != null) {
            String newFieldName = null;
            if (fieldName.equals(SolrElementField.FIELD_ISDOCUMENTEDBY)) {
                newFieldName = SolrElementField.FIELD_DOCUMENTS;
            } else if (fieldName.equals(SolrElementField.FIELD_DOCUMENTS)) {
                newFieldName = SolrElementField.FIELD_ISDOCUMENTEDBY;
            }
            if (newFieldName != null) {
                for (String item : aggregatedItems) {
                    SolrDoc doc = null;
                    try {
                        doc = getDocumentById(item);
                        List<String> fieldValues = doc.getAllFieldValues(newFieldName);
                        List<String> resourceMapIds = doc
                                .getAllFieldValues(SolrElementField.FIELD_RESOURCEMAP);
                        if ((fieldValues != null && fieldValues.contains(targetId))
                                && (resourceMapIds != null && resourceMapIds
                                        .contains(targetResourceMapId))) {
                            //okay, we found the target aggregation item id and the resource map id
                            //in this solr doc. However, we need check if another resource map with different
                            //id but specify the same relationship. If we have the id(s), we should not
                            // remove the documents( or documentBy) element since we need to preserve the 
                            // relationship for the remain resource map. 
                            boolean hasDuplicateIds = false;
                            if(originalResourceMaps != null) {
                               for(String id :resourceMapIds) {
                                    if (originalResourceMaps.contains(id) && !id.equals(targetResourceMapId)) {
                                        hasDuplicateIds = true;
                                        break;
                                    }
                                }
                            }
                            if(!hasDuplicateIds) {
                                map.put(item, targetResourceMapId);
                            }
                            
                        }
                    } catch (Exception e) {
                        log.warn("SolrIndex.matchResourceMapsAndItems - can't get the solrdoc for the id "
                                + item + " since " + e.getMessage());
                    }
                }
            }
        }
        return map;
    }

    /*
     * Get the solr index doc from the index server for the given id.
     */
    private SolrDoc getDocumentById(String id) throws NotImplemented, NotFound, UnsupportedType, 
                SolrServerException, ParserConfigurationException, SAXException, XPathExpressionException, IOException {
        SolrDoc doc = ResourceMapSubprocessor.getSolrDoc(id);
        return doc;
    }
    
    /*
     * Merge two list of updated solr docs. removedDocumentBy has the correct information about documentBy element.
     * removedDocuments has the correct information about the documents element.
     * So we go through the two list and found the two docs having the same identifier.
     * Get the list of the documents value from the one in the removedDoucments (1). 
     * Remove all values of documents from the one in the removedDocumentBy. 
     * Then copy the list of documents value from (1) to to the one in the removedDocumentBy.
     */
    private List<SolrDoc> mergeUpdatedSolrDocs(List<SolrDoc>removedDocumentBy, List<SolrDoc>removedDocuments) {
        List<SolrDoc> mergedDocuments = new ArrayList<SolrDoc>();
        if(removedDocumentBy == null || removedDocumentBy.isEmpty()) {
            mergedDocuments = removedDocuments;
        } else if (removedDocuments == null || removedDocuments.isEmpty()) {
            mergedDocuments = removedDocumentBy;
        } else {
            int sizeOfDocBy = removedDocumentBy.size();
            int sizeOfDocs = removedDocuments.size();
            for(int i=sizeOfDocBy-1; i>= 0; i--) {
                SolrDoc docInRemovedDocBy = removedDocumentBy.get(i);
                for(int j= sizeOfDocs-1; j>=0; j--) {
                    SolrDoc docInRemovedDocs = removedDocuments.get(j);
                    if(docInRemovedDocBy.getIdentifier().equals(docInRemovedDocs.getIdentifier())) {
                        //find the same doc in both list. let's merge them.
                        //first get all the documents element from the docWithDocs(it has the correct information about the documents element)
                        List<String> idsInDocuments = docInRemovedDocs.getAllFieldValues(SolrElementField.FIELD_DOCUMENTS);
                        docInRemovedDocBy.removeAllFields(SolrElementField.FIELD_DOCUMENTS);//clear out any documents element in docInRemovedDocBy
                        //add the Documents element from the docInRemovedDocs if it has any.
                        // The docInRemovedDocs has the correct information about the documentBy. Now it copied the correct information of the documents element.
                        // So docInRemovedDocs has both correct information about the documentBy and documents elements.
                        if(idsInDocuments != null) {
                            for(String id : idsInDocuments) {
                                if(id != null && !id.trim().equals("")) {
                                    docInRemovedDocBy.addField(new SolrElementField(SolrElementField.FIELD_DOCUMENTS, id));
                                }
                                
                            }
                        }
                        //intersect the resource map ids.
                        List<String> resourceMapIdsInWithDocs = docInRemovedDocs.getAllFieldValues(SolrElementField.FIELD_RESOURCEMAP);
                        List<String> resourceMapIdsInWithDocBy = docInRemovedDocBy.getAllFieldValues(SolrElementField.FIELD_RESOURCEMAP);
                        docInRemovedDocBy.removeAllFields(SolrElementField.FIELD_RESOURCEMAP);
                        Collection resourceMapIds = CollectionUtils.union(resourceMapIdsInWithDocs, resourceMapIdsInWithDocBy);
                        if(resourceMapIds != null) {
                            for(Object idObj : resourceMapIds) {
                                String id = (String)idObj;
                                docInRemovedDocBy.addField(new SolrElementField(SolrElementField.FIELD_RESOURCEMAP, id));
                            }
                        }
                        //we don't need do anything about the documentBy elements since the docInRemovedDocBy has the correct information.
                        mergedDocuments.add(docInRemovedDocBy);
                        //delete the two documents from the list
                        removedDocumentBy.remove(i);
                        removedDocuments.remove(j);
                        break;
                    }
                    
                }
            }
            // when we get there, if the two lists are empty, this will be a perfect merge. However, if something are left. we 
            //just put them in.
            for(SolrDoc doc: removedDocumentBy) {
                mergedDocuments.add(doc);
            }
            for(SolrDoc doc: removedDocuments) {
                mergedDocuments.add(doc);
            }
        }
        return mergedDocuments;
    }
    
   
    /*
     * Remove a pid which is part of resource map.
     */
    private void removeFromDataPackage(String pid) throws XPathExpressionException, NotImplemented, NotFound, UnsupportedType, SolrServerException, IOException, ParserConfigurationException, SAXException  {
        SolrDoc indexedDoc = ResourceMapSubprocessor.getSolrDoc(pid);
        removeFromIndex(pid);
        List<SolrDoc> docsToUpdate = new ArrayList<SolrDoc>();

        List<String> documents = indexedDoc.getAllFieldValues(SolrElementField.FIELD_DOCUMENTS);
        for (String documentsValue : documents) {
            SolrDoc solrDoc = ResourceMapSubprocessor.getSolrDoc(documentsValue);
            solrDoc.removeFieldsWithValue(SolrElementField.FIELD_ISDOCUMENTEDBY, pid);
            removeFromIndex(documentsValue);
            insertToIndex(solrDoc);
        }

        List<String> documentedBy = indexedDoc
                .getAllFieldValues(SolrElementField.FIELD_ISDOCUMENTEDBY);
        for (String documentedByValue : documentedBy) {
            SolrDoc solrDoc = ResourceMapSubprocessor.getSolrDoc(documentedByValue);
            solrDoc.removeFieldsWithValue(SolrElementField.FIELD_DOCUMENTS, pid);
            //docsToUpdate.add(solrDoc);
            removeFromIndex(documentedByValue);
            insertToIndex(solrDoc);
        }

        //SolrElementAdd addCommand = new SolrElementAdd(docsToUpdate);
        //httpService.sendUpdate(solrIndexUri, addCommand);
    }

    /*
     * Remove a pid from the solr index
     */
    private synchronized void removeFromIndex(String pid) throws SolrServerException, IOException {
        if(pid != null && !pid.trim().equals("")) {
            /*IndexEvent event = new IndexEvent();
            event.setDate(Calendar.getInstance().getTime());
            Identifier identifier = new Identifier();
            identifier.setValue(pid);
            event.setIdentifier(identifier);*/
            try {
                solrServer.deleteById(pid);
                solrServer.commit();
                /*event.setType(IndexEvent.SUCCESSDELETE);
                event.setDescription("Successfully remove the solr index for the id "+identifier.getValue());
                try {
                    EventlogFactory.createIndexEventLog().write(event);
                } catch (Exception e) {
                    log.error("SolrIndex.removeFromIndex - IndexEventLog can't log the index deleting event :"+e.getMessage());
                }*/
            } catch (SolrServerException e) {
                /*event.setAction(Event.DELETE);
                event.setDescription("Failurely remove the solr index for the id "+identifier.getValue()+" since "+e.getMessage());
                try {
                    EventlogFactory.createIndexEventLog().write(event);
                } catch (Exception ee) {
                    log.error("SolrIndex.removeFromIndex - IndexEventLog can't log the index deleting event :"+ee.getMessage());
                }*/
                throw e;
                
            } catch (IOException e) {
                /*event.setAction(Event.DELETE);
                event.setDescription("Failurely remove the solr index for the id "+identifier.getValue()+" since "+e.getMessage());
                try {
                    EventlogFactory.createIndexEventLog().write(event);
                } catch (Exception ee) {
                    log.error("SolrIndex.removeFromIndex - IndexEventLog can't log the index deleting event :"+ee.getMessage());
                }*/
                throw e;
            }
            
        }
    }

    /**
     * Get the solrServer
     * @return
     */
    public SolrServer getSolrServer() {
        return solrServer;
    }

    /**
     * Set the solrServer. 
     * @param solrServer
     */
    public void setSolrServer(SolrServer solrServer) {
        this.solrServer = solrServer;
    }
    
    /**
     * Get all indexed ids in the solr server. 
     * @return an empty list if there is no index.
     * @throws SolrServerException
     */
    public List<String> getSolrIds() throws SolrServerException {
        List<String> list = new ArrayList<String>();
        SolrQuery query = new SolrQuery(IDQUERY); 
        query.setRows(Integer.MAX_VALUE); 
        query.setFields(ID); 
        QueryResponse response = solrServer.query(query); 
        SolrDocumentList docs = response.getResults();
        if(docs != null) {
            for(SolrDocument doc :docs) {
                String identifier = (String)doc.getFieldValue(ID);
                //System.out.println("======================== "+identifier);
                list.add(identifier);
            }
        }
        return list;
    }
    
    private void writeEventLog(SystemMetadata systemMetadata, Identifier pid, String error) {
        IndexEvent event = new IndexEvent();
        event.setIdentifier(pid);
        event.setDate(Calendar.getInstance().getTime());
        String action = null;
        if (systemMetadata == null ) {
            action = Event.CREATE.xmlValue();
            event.setAction(Event.CREATE);
        }
        else if(systemMetadata.getArchived()) {
            action = Event.DELETE.xmlValue();
            event.setAction(Event.DELETE);
        } else {
            action = Event.CREATE.xmlValue();
            event.setAction(Event.CREATE);
        }
        event.setDescription("Failed to "+action+"the solr index for the id "+pid.getValue()+" since "+error);
        try {
            EventlogFactory.createIndexEventLog().write(event);
        } catch (Exception ee) {
            log.error("SolrIndex.insertToIndex - IndexEventLog can't log the index inserting event :"+ee.getMessage());
        }
    }
}
