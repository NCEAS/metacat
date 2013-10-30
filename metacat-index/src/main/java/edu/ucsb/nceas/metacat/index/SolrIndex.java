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
import java.util.ArrayList;
import java.util.Calendar;
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
import org.dataone.cn.indexer.XMLNamespaceConfig;
import org.dataone.cn.indexer.parser.IDocumentSubprocessor;
import org.dataone.cn.indexer.parser.SolrField;
import org.dataone.cn.indexer.resourcemap.ResourceEntry;
import org.dataone.cn.indexer.resourcemap.ResourceMap;
import org.dataone.cn.indexer.resourcemap.ResourceMapFactory;
import org.dataone.cn.indexer.solrhttp.SolrDoc;
import org.dataone.cn.indexer.solrhttp.SolrElementField;
import org.dataone.service.exceptions.NotFound;
import org.dataone.service.exceptions.NotImplemented;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.exceptions.UnsupportedType;
import org.dataone.service.types.v1.Event;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.SystemMetadata;
import org.dataone.service.util.TypeMarshaller;
import org.dspace.foresite.OREParserException;
import org.jibx.runtime.JiBXException;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import edu.ucsb.nceas.metacat.common.index.event.IndexEvent;
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
                                log.error(e.getStackTrace().toString());
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
        if (indexedDocument == null || indexedDocument.getFieldList().size() <= 0) {
            return indexDocument;
        } else {
            for (SolrElementField field : indexedDocument.getFieldList()) {
                if ((field.getName().equals(SolrElementField.FIELD_ISDOCUMENTEDBY)
                        || field.getName().equals(SolrElementField.FIELD_DOCUMENTS) || field
                        .getName().equals(SolrElementField.FIELD_RESOURCEMAP))
                        && !indexDocument.hasFieldWithValue(field.getName(), field.getValue())) {
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
        String objectPath = null;
        InputStream data = null;
        try {
            objectPath = DistributedMapsFactory.getObjectPathMap().get(pid);
            data = new FileInputStream(objectPath);
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
        checkParams(pid, systemMetadata, data);
        boolean isArchive = systemMetadata.getArchived();
        if(isArchive ) {
            //delete the index for the archived objects
            remove(pid.getValue(), systemMetadata);
            log.info("SolrIndex.update============================= archive the idex for the identifier "+pid);
        } else {
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
     * Remove a resource map pid
     */
    private void removeDataPackage(String pid) throws ServiceFailure, SAXException, XPathExpressionException, NotImplemented, NotFound, UnsupportedType, SolrServerException, IOException, ParserConfigurationException, OREParserException  {
        Document resourceMapDoc = generateXmlDocument(DistributedMapsFactory.getDataObject(pid));
        //ResourceMap resourceMap = new ResourceMap(resourceMapDoc);
        ResourceMap resourceMap = ResourceMapFactory.buildResourceMap(resourceMapDoc);
        List<String> documentIds = resourceMap.getAllDocumentIDs();
        List<SolrDoc> indexDocuments =ResourceMapSubprocessor.getSolrDocs(documentIds);
        removeFromIndex(pid);
        //List<SolrDoc> docsToUpdate = new ArrayList<SolrDoc>();
        // for each document in data package:
        for (SolrDoc indexDoc : indexDocuments) {

            if (indexDoc.getIdentifier().equals(pid)) {
                continue; // skipping the resource map, no need update
                          // it.
                          // will
                          // be removed.
            }

            // Remove resourceMap reference
            indexDoc.removeFieldsWithValue(SolrElementField.FIELD_RESOURCEMAP,
                    resourceMap.getIdentifier());

            // // Remove documents/documentedby values for this resource
            // map
            for (ResourceEntry entry : resourceMap.getMappedReferences()) {
                if (indexDoc.getIdentifier().equals(entry.getIdentifier())) {
                    for (String documentedBy : entry.getDocumentedBy()) {
                        // Using removeOneFieldWithValue in-case same
                        // documents
                        // are in more than one data package. just
                        // remove
                        // one
                        // instance of data package info.
                        indexDoc.removeOneFieldWithValue(SolrElementField.FIELD_ISDOCUMENTEDBY,
                                documentedBy);
                    }
                    for (String documents : entry.getDocuments()) {
                        indexDoc.removeOneFieldWithValue(SolrElementField.FIELD_DOCUMENTS,
                                documents);
                    }
                    break;
                }
            }
            removeFromIndex(indexDoc.getIdentifier());
            insertToIndex(indexDoc);
            //docsToUpdate.add(indexDoc);
        }
        //SolrElementAdd addCommand = new SolrElementAdd(docsToUpdate);
        //httpService.sendUpdate(solrIndexUri, addCommand);
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
