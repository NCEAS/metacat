/**
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
package edu.ucsb.nceas.metacat.index.annotation;

import edu.ucsb.nceas.metacat.index.resourcemap.ResourceMapSubprocessor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.xpath.XPathExpressionException;

import org.apache.commons.codec.EncoderException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dataone.cn.index.util.PerformanceLogger;
import org.dataone.cn.indexer.annotation.SparqlField;
import org.dataone.cn.indexer.annotation.TripleStoreService;
import org.dataone.cn.indexer.parser.IDocumentSubprocessor;
import org.dataone.cn.indexer.parser.ISolrDataField;
import org.dataone.cn.indexer.parser.SubprocessorUtility;
import org.dataone.cn.indexer.solrhttp.HTTPService;
import org.dataone.cn.indexer.solrhttp.SolrDoc;
import org.dataone.cn.indexer.solrhttp.SolrElementField;
import org.springframework.beans.factory.annotation.Autowired;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.tdb.TDBFactory;

/**
 * A solr index parser for an RDF/XML file.
 * The solr doc of the RDF/XML object only has the system metadata information.
 * The solr docs of the science metadata doc and data file have the annotation information.
 */
public class MetacatRdfXmlSubprocessor implements IDocumentSubprocessor {

    private static Log log = LogFactory.getLog(MetacatRdfXmlSubprocessor.class);
    private static PerformanceLogger perfLog = PerformanceLogger.getInstance();
    /**
     * If xpath returns true execute the processDocument Method
     */
    private List<String> matchDocuments = null;

    private List<ISolrDataField> fieldList = new ArrayList<ISolrDataField>();

    private List<String> fieldsToMerge = new ArrayList<String>();

    @Autowired
    private HTTPService httpService = null;

    @Autowired
    private String solrQueryUri = null;

    @Autowired
    private SubprocessorUtility processorUtility;

    /**
     * Returns true if subprocessor should be run against object
     * 
     * @param formatId the the document to be processed
     * @return true if this processor can parse the formatId
     */
    public boolean canProcess(String formatId) {
        return matchDocuments.contains(formatId);
    }

    public List<String> getMatchDocuments() {
        return matchDocuments;
    }

    public void setMatchDocuments(List<String> matchDocuments) {
        this.matchDocuments = matchDocuments;
    }

    public List<ISolrDataField> getFieldList() {
        return fieldList;
    }

    public void setFieldList(List<ISolrDataField> fieldList) {
        this.fieldList = fieldList;
    }

    @Override
    public Map<String, SolrDoc> processDocument(String identifier, Map<String, SolrDoc> docs,
            InputStream is) throws Exception {

        if (log.isTraceEnabled()) {
            log.trace("INCOMING DOCS to processDocument(): ");
            serializeDocuments(docs);
        }

        SolrDoc resourceMapDoc = docs.get(identifier);
        List<SolrDoc> processedDocs = process(resourceMapDoc, is);
        Map<String, SolrDoc> processedDocsMap = new HashMap<String, SolrDoc>();
        for (SolrDoc processedDoc : processedDocs) {
            processedDocsMap.put(processedDoc.getIdentifier(), processedDoc);
        }

        if (log.isTraceEnabled()) {
            log.trace("PREMERGED DOCS from processDocument(): ");
            serializeDocuments(processedDocsMap);
        }

        // Merge previously processed (but yet to be indexed) documents
        Map<String, SolrDoc> mergedDocs = mergeDocs(docs, processedDocsMap);

        if (log.isTraceEnabled()) {
            log.trace("OUTGOING DOCS from processDocument(): ");
            serializeDocuments(mergedDocs);
        }

        return mergedDocs;
    }

    /**
     * Serialize documents to be indexed for debugging
     * 
     * @param docs
     * @throws IOException
     */
    private void serializeDocuments(Map<String, SolrDoc> docs) {
        StringBuilder documents = new StringBuilder();
        documents.append("<docs>");

        for (SolrDoc doc : docs.values()) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try {
                doc.serialize(baos, "UTF-8");

            } catch (IOException e) {
                log.trace("Couldn't serialize documents: " + e.getMessage());
            }
            
            try {
                documents.append(baos.toString());
            } finally {
                IOUtils.closeQuietly(baos);
            }
        }
        documents.append("</docs>");
        log.trace(documents.toString());
    }

    private List<SolrDoc> process(SolrDoc indexDocument, InputStream is) throws Exception {
        
        // get the triplestore dataset
        long start = System.currentTimeMillis();
        Map<String, SolrDoc> mergedDocuments;
        Dataset dataset = TripleStoreService.getInstance().getDataset();
        try {
            perfLog.log("RdfXmlSubprocess.process gets a dataset from tripe store service ", System.currentTimeMillis() - start);
            
            // read the annotation
            String indexDocId = indexDocument.getIdentifier();
            String name = indexDocId;
    
            //Check if the identifier is a valid URI and if not, make it one by prepending "http://"
            URI nameURI;
            String scheme = null;
            try {
                nameURI = new URI(indexDocId);
                scheme = nameURI.getScheme();
                
            } catch (URISyntaxException use) {
                // The identifier can't be parsed due to offending characters. It's not a URL
                
                name = "https://cn.dataone.org/cn/v1/resolve/"+indexDocId;
            }
            
            // The had no scheme prefix. It's not a URL
            if ((scheme == null) || (scheme.isEmpty())) {
                name = "https://cn.dataone.org/cn/v1/resolve/"+indexDocId;
                
            }
            
            long startOntModel = System.currentTimeMillis();
            boolean loaded = dataset.containsNamedModel(name);
            if (!loaded) {
                OntModel ontModel = ModelFactory.createOntologyModel();
                ontModel.read(is, name);
                dataset.addNamedModel(name, ontModel);
            }
            perfLog.log("RdfXmlSubprocess.process adds ont-model ", System.currentTimeMillis() - startOntModel);
            //dataset.getDefaultModel().add(ontModel);
    
            //Start a list of Solr documents to index, mapping by pid and seriesId
            Map<String, SolrDoc> documentsToIndexByPid = new HashMap<String, SolrDoc>();
            Map<String, SolrDoc> documentsToIndexBySeriesId = new HashMap<String, SolrDoc>();
            
            //Track timing of this process
            long startField = System.currentTimeMillis();
            
            //Process each field listed in the fieldList in this subprocessor
            for (ISolrDataField field : this.fieldList) {
                long filed = System.currentTimeMillis();
                String q = null;
                
                //Process Sparql fields
                if (field instanceof SparqlField) {
                	
                	//Get the Sparql query for this field
                    q = ((SparqlField) field).getQuery();
                    
                    //Replace the graph name with the URI of this resource map
                    q = q.replaceAll("\\$GRAPH_NAME", name);
                    
                    //Create a Query object
                    Query query = QueryFactory.create(q);
                    log.trace("Executing SPARQL query:\n" + query.toString());
                    
                    //Execute the Sparql query
                    QueryExecution qexec = QueryExecutionFactory.create(query, dataset);
                    
                    //Get the results of the query
                    ResultSet results = qexec.execSelect();
                    
                    //Iterate over each query result and process it
                    while (results.hasNext()) {
                    	
                    	//Create a SolrDoc for this query result
                        SolrDoc solrDoc = null;
                        QuerySolution solution = results.next();
                        log.trace(solution.toString());
    
                        //Sparql queries can identify a SolrDoc by pid or seriesId. 
                        //If the Sparql query uses a pid, get the SolrDoc by pid
                        if (solution.contains("pid")) {
                        	//Get the pid from the query result
                            String id = solution.getLiteral("pid").getString();

                            //Get the SolrDoc from the hash map, if it exists
                            solrDoc = documentsToIndexByPid.get(id);
                            
                            if (solrDoc == null) {
                            	
                            	//If the id matches the document we are currently indexing, use that SolrDoc
                            	if(id.equals(indexDocId)) {
                            		solrDoc = indexDocument;
                            	}
                                //If the SolrDoc doesn't exist yet, create one
                            	else {
                                    solrDoc = new SolrDoc();
                                    //Add the id as the ID field
                                    solrDoc.addField(new SolrElementField(SolrElementField.FIELD_ID, id));
                                    //Add the SolrDoc to the hash map
                                    documentsToIndexByPid.put(id, solrDoc);	
                            	}
                            }
                        }
                        //If the Sparql query uses a pid, get the SolrDoc by seriesId
                        else if (solution.contains("seriesId")) {
                        	//Get the seriesId
                            String id = solution.getLiteral("seriesId").getString();
                                
                            //Get the SolrDoc from the hash map, if it exists
                            solrDoc = documentsToIndexBySeriesId.get(id);
                            
                            //If the SolrDoc doesn't exist yet, create one
                            if (solrDoc == null) {
                                solrDoc = new SolrDoc();
                                //Add the id as the seriesId field
                                solrDoc.addField(new SolrElementField(SolrElementField.FIELD_SERIES_ID, id));
                                //Add to the hash map
                                documentsToIndexBySeriesId.put(id, solrDoc);
                            }
                        }
    
                        //Get the index field name and value returned from the Sparql query
                        if (solution.contains(field.getName())) {
                        	//Get the value for this field
                            String value = solution.get(field.getName()).toString();
                            
                            //Create an index field for this field name and value
                            SolrElementField f = new SolrElementField(field.getName(), value);
                            
                            //If this field isn't already populated with the same value, then add it
                            if (!solrDoc.hasFieldWithValue(f.getName(), f.getValue())) {
                                solrDoc.addField(f);
                            }
                        }
                    }
                }
                perfLog.log("RdfXmlSubprocess.process process the field "+field.getName(), System.currentTimeMillis() - filed);
            }
            perfLog.log("RdfXmlSubprocess.process process the fields total ", System.currentTimeMillis() - startField);
            // clean up the triple store
            //TDBFactory.release(dataset);
    
            long getStart = System.currentTimeMillis();

            //Get the SolrDocs that already exist in the Solr index for the given seriesIds
            Map<String, SolrDoc> existingDocsBySeriesId = getSolrDocsBySeriesId(documentsToIndexBySeriesId.keySet());
            
            //Get the SolrDocs that already exist in the Solr index for the given pids
            Map<String, SolrDoc> existingDocsByPid = getSolrDocs(documentsToIndexByPid.keySet());
            
            perfLog.log("RdfXmlSubprocess.process get existing solr docs ", System.currentTimeMillis() - getStart);
            
            //Combine the hash maps of existing SolrDocs into a single map
            Map<String, SolrDoc> allExistingDocs = new HashMap<String, SolrDoc>(); 
            allExistingDocs.putAll(existingDocsByPid);
            allExistingDocs.putAll(existingDocsBySeriesId);
            
            //Combine the hash maps of to-be-indexed SolrDocs into a single map
            Map<String, SolrDoc> allDocsToBeIndexed = new HashMap<String, SolrDoc>();
            allDocsToBeIndexed.putAll(documentsToIndexByPid);
            allDocsToBeIndexed.putAll(documentsToIndexBySeriesId);
            
            //Merge the new SolrDocs with the new fields with the existing SolrDocs
            mergedDocuments = mergeDocs(allDocsToBeIndexed, allExistingDocs);
            
            //Add the resource map to the merged documents list
            mergedDocuments.put(indexDocument.getIdentifier(), indexDocument);
            
    
            perfLog.log("RdfXmlSubprocess.process() total take ", System.currentTimeMillis() - start);
        } finally {
            try {
                TripleStoreService.getInstance().destoryDataset(dataset);
            } catch (Exception e) {
                log.warn("A tdb directory can't be removed since "+e.getMessage(), e);
            }
        }
        return new ArrayList<SolrDoc>(mergedDocuments.values());
    }

    private Map<String, SolrDoc> getSolrDocs(Set<String> ids) throws Exception {
        Map<String, SolrDoc> list = new HashMap<String, SolrDoc>();
        if (ids != null) {
            for (String id : ids) {
                //SolrDoc doc = httpService.retrieveDocumentFromSolrServer(id, solrQueryUri);
                SolrDoc doc = ResourceMapSubprocessor.getSolrDoc(id);;
                if (doc != null) {
                    list.put(id, doc);
                }
            }
        }
        return list;
    }
    
    private Map<String, SolrDoc> getSolrDocsBySeriesId(Set<String> ids) throws Exception {
        Map<String, SolrDoc> list = new HashMap<String, SolrDoc>();
        if (ids != null) {
            for (String id : ids) {
                //SolrDoc doc = httpService.retrieveDocumentFromSolrServer(id, solrQueryUri);
                SolrDoc doc = ResourceMapSubprocessor.getDocumentBySeriesId(id);;
                if (doc != null) {
                    list.put(doc.getIdentifier(), doc);
                }
            }
        }
        return list;
    }

    /*
     * Merge existing documents from the Solr index with pending documents
     */
    private Map<String, SolrDoc> mergeDocs(Map<String, SolrDoc> pending,
            Map<String, SolrDoc> existing) throws Exception {
        long start = System.currentTimeMillis();
        Map<String, SolrDoc> merged = new HashMap<String, SolrDoc>();

        Iterator<String> pendingIter = pending.keySet().iterator();
        while (pendingIter.hasNext()) {
        	
        	//Get the next id in the hash map
            String id = pendingIter.next();
            
            //Get the doc with this id from the pending docs
            SolrDoc pendingDoc = pending.get(id);
            //Get the doc with this id from the existing docs
            SolrDoc existingDoc = existing.get(id);
            
            //Start a new SolrDoc to merge them together
            SolrDoc mergedDoc = new SolrDoc();
            

            //If no existing doc was found with the given id, and the pending doc has no id field,
            // see if there is a match with the seriesId field
            if(existingDoc == null && !pendingDoc.hasField(SolrElementField.FIELD_ID)) {
            	for (Map.Entry<String, SolrDoc> entry : existing.entrySet()) {
                    SolrDoc doc = entry.getValue();
                    if( doc.hasFieldWithValue(SolrElementField.FIELD_SERIES_ID, id) ) {
                    	existingDoc = doc;
                    	break;
                    }
                    
            	}
            }
            
            //If there is an existing doc,
            if (existingDoc != null) {
                // Add the existing fields to the merged doc
                for (SolrElementField field : existingDoc.getFieldList()) {
                    mergedDoc.addField(field);

                }
            }
            
            // add the pending
            for (SolrElementField field : pendingDoc.getFieldList()) {
                if (field.getName().equals(SolrElementField.FIELD_ID)
                        && mergedDoc.hasField(SolrElementField.FIELD_ID)) {
                    continue;
                }

                // only add if we don't already have it
                if (!mergedDoc.hasFieldWithValue(field.getName(), field.getValue())) {
                    mergedDoc.addField(field);
                }
            }

            // include in results
            merged.put(mergedDoc.getIdentifier(), mergedDoc);
        }

        // add existing if not yet merged (needed if existing map size > pending map size)
        Iterator<String> existingIter = existing.keySet().iterator();

        while (existingIter.hasNext()) {
            String existingId = existingIter.next();

            if (!merged.containsKey(existingId)) {
                merged.put(existingId, existing.get(existingId));

            }
        }

        if (log.isTraceEnabled()) {
            log.trace("MERGED DOCS with existing from the Solr index: ");
            serializeDocuments(merged);
        }
        perfLog.log("RdfXmlSubprocess.merge total ", System.currentTimeMillis() - start);
        return merged;
    }

    @Override
    public SolrDoc mergeWithIndexedDocument(SolrDoc indexDocument) throws IOException,
            EncoderException, XPathExpressionException {
        return processorUtility.mergeWithIndexedDocument(indexDocument, fieldsToMerge);
    }

    public List<String> getFieldsToMerge() {
        return fieldsToMerge;
    }

    public void setFieldsToMerge(List<String> fieldsToMerge) {
        this.fieldsToMerge = fieldsToMerge;
    }
}
