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
    
            // process each field query
            Map<String, SolrDoc> documentsToIndex = new HashMap<String, SolrDoc>();
            long startField = System.currentTimeMillis();
            for (ISolrDataField field : this.fieldList) {
                long filed = System.currentTimeMillis();
                String q = null;
                if (field instanceof SparqlField) {
                    q = ((SparqlField) field).getQuery();
                    q = q.replaceAll("\\$GRAPH_NAME", name);
                    Query query = QueryFactory.create(q);
                    log.trace("Executing SPARQL query:\n" + query.toString());
                    QueryExecution qexec = QueryExecutionFactory.create(query, dataset);
                    ResultSet results = qexec.execSelect();
                    while (results.hasNext()) {
                        SolrDoc solrDoc = null;
                        QuerySolution solution = results.next();
                        log.trace(solution.toString());
    
                        // find the index document we are trying to augment with the annotation
                        if (solution.contains("pid")) {
                            String id = solution.getLiteral("pid").getString();
    
                            // TODO: check if anyone with permissions on the annotation document has write permission on the document we are annotating
                            boolean statementAuthorized = true;
                            if (!statementAuthorized) {
                                continue;
                            }
    
                            // otherwise carry on with the indexing
                            solrDoc = documentsToIndex.get(id);
                            if (solrDoc == null) {
                                solrDoc = new SolrDoc();
                                solrDoc.addField(new SolrElementField(SolrElementField.FIELD_ID, id));
                                documentsToIndex.put(id, solrDoc);
                            }
                        }
    
                        // add the field to the index document
                        if (solution.contains(field.getName())) {
                            String value = solution.get(field.getName()).toString();
                            SolrElementField f = new SolrElementField(field.getName(), value);
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
    
            // merge the existing index with the new[er] values
            long getStart = System.currentTimeMillis();
            Map<String, SolrDoc> existingDocuments = getSolrDocs(documentsToIndex.keySet());
            perfLog.log("RdfXmlSubprocess.process get existing solr docs ", System.currentTimeMillis() - getStart);
            mergedDocuments = mergeDocs(documentsToIndex, existingDocuments);
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

    /*
     * Merge existing documents from the Solr index with pending documents
     */
    private Map<String, SolrDoc> mergeDocs(Map<String, SolrDoc> pending,
            Map<String, SolrDoc> existing) throws Exception {
        long start = System.currentTimeMillis();
        Map<String, SolrDoc> merged = new HashMap<String, SolrDoc>();

        Iterator<String> pendingIter = pending.keySet().iterator();
        while (pendingIter.hasNext()) {
            String id = pendingIter.next();
            SolrDoc pendingDoc = pending.get(id);
            SolrDoc existingDoc = existing.get(id);
            SolrDoc mergedDoc = new SolrDoc();
            if (existingDoc != null) {
                // merge the existing fields
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
            merged.put(id, mergedDoc);
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
