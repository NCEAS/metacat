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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.schema.IndexSchema;
import org.apache.solr.servlet.SolrRequestParsers;
import org.dataone.cn.indexer.convert.SolrDateConverter;
import org.dataone.cn.indexer.parser.AbstractDocumentSubprocessor;
import org.dataone.cn.indexer.parser.IDocumentSubprocessor;
import org.dataone.cn.indexer.parser.ISolrField;
import org.dataone.cn.indexer.solrhttp.SolrDoc;
import org.dataone.cn.indexer.solrhttp.SolrElementField;
import org.dataone.service.exceptions.NotFound;
import org.dataone.service.exceptions.UnsupportedType;
import org.dataone.service.util.DateTimeMarshaller;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

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

import edu.ucsb.nceas.metacat.common.SolrServerFactory;
import edu.ucsb.nceas.metacat.common.query.SolrQueryServiceController;


/**
 * A solr index parser for an RDF/XML file.
 * The solr doc of the RDF/XML object only has the system metadata information.
 * The solr docs of the science metadata doc and data file have the annotation information.
 */
public class RdfXmlSubprocessor extends AbstractDocumentSubprocessor implements IDocumentSubprocessor {

    private static final String QUERY ="q=id:";
    private static Log log = LogFactory.getLog(RdfXmlSubprocessor.class);
    private static SolrServer solrServer =  null;
    static {
        try {
            solrServer = SolrServerFactory.createSolrServer();
        } catch (Exception e) {
            log.error("RdfXmlSubprocessor - can't generate the SolrServer since - "+e.getMessage());
        }
    }
          
    @Override
    public Map<String, SolrDoc> processDocument(String identifier, Map<String, SolrDoc> docs, Document doc) throws Exception {
        SolrDoc resourceMapDoc = docs.get(identifier);
        List<SolrDoc> processedDocs = process(resourceMapDoc, doc);
        Map<String, SolrDoc> processedDocsMap = new HashMap<String, SolrDoc>();
        for (SolrDoc processedDoc : processedDocs) {
            processedDocsMap.put(processedDoc.getIdentifier(), processedDoc);
        }
        return processedDocsMap;
    }

    private InputStream toInputStream(Document doc) throws TransformerConfigurationException, TransformerException, TransformerFactoryConfigurationError {
    	ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    	Source xmlSource = new DOMSource(doc);
    	Result outputTarget = new StreamResult(outputStream);
    	TransformerFactory.newInstance().newTransformer().transform(xmlSource, outputTarget);
    	InputStream is = new ByteArrayInputStream(outputStream.toByteArray());
    	return is;
    }
    
    private List<SolrDoc> process(SolrDoc indexDocument, Document rdfXmlDocument) throws Exception {
    	
    	// get the triplestore dataset
		Dataset dataset = TripleStoreService.getInstance().getDataset();
		
    	// read the annotation
		InputStream source = toInputStream(rdfXmlDocument);
    	String name = indexDocument.getIdentifier();
    	boolean loaded = dataset.containsNamedModel(name);
		if (!loaded) {
			OntModel ontModel = ModelFactory.createOntologyModel();
			ontModel.read(source, name);
			dataset.addNamedModel(name, ontModel);
		}
		//dataset.getDefaultModel().add(ontModel);
		
		// process each field query
        Map<String, SolrDoc> documentsToIndex = new HashMap<String, SolrDoc>();
		for (ISolrField field: this.getFieldList()) {
			String q = null;
			if (field instanceof SparqlField) {
				q = ((SparqlField) field).getQuery();
				q = q.replaceAll("\\$GRAPH_NAME", name);
				Query query = QueryFactory.create(q);
				QueryExecution qexec = QueryExecutionFactory.create(query, dataset);
				ResultSet results = qexec.execSelect();
				
				while (results.hasNext()) {
					SolrDoc solrDoc = null;
					QuerySolution solution = results.next();
					System.out.println(solution.toString());
					
					// find the index document we are trying to augment with the annotation
					if (solution.contains("pid")) {
						String id = solution.getLiteral("pid").getString();
						solrDoc = documentsToIndex.get(id);
						if (solrDoc == null) {
							solrDoc = new SolrDoc();
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
		}
		
		// clean up the triple store
		TDBFactory.release(dataset);

		// merge the existing index with the new[er] values
        Map<String, SolrDoc> existingDocuments = getSolrDocs(documentsToIndex.keySet());
        Map<String, SolrDoc> mergedDocuments = mergeDocs(documentsToIndex, existingDocuments);
        mergedDocuments.put(indexDocument.getIdentifier(), indexDocument);
        
        return new ArrayList<SolrDoc>(mergedDocuments.values());
    }
    
    private Map<String, SolrDoc> getSolrDocs(Set<String> ids) throws Exception {
        Map<String, SolrDoc> list = new HashMap<String, SolrDoc>();
        if (ids != null) {
            for (String id : ids) {
            	SolrDoc doc = getSolrDoc(id);
                if (doc != null) {
                    list.put(id, doc);
                }
            }
        }
        return list;
    }
    
    private Map<String, SolrDoc> mergeDocs(Map<String, SolrDoc> pending, Map<String, SolrDoc> existing) {
    	Map<String, SolrDoc> merged = new HashMap<String, SolrDoc>();
    	Iterator<String> pendingIter = pending.keySet().iterator();
    	while (pendingIter.hasNext()) {
    		String id = pendingIter.next();
    		SolrDoc pendingDoc = pending.get(id);
    		SolrDoc existingDoc = existing.get(id);
    		SolrDoc mergedDoc = new SolrDoc();
    		if (existingDoc != null) {
    			// merge the existing fields
    			for (SolrElementField field: existingDoc.getFieldList()) {
    				mergedDoc.addField(field);
    				
    			}
    		}
    		// add the pending
    		for (SolrElementField field: pendingDoc.getFieldList()) {
				mergedDoc.addField(field);
				
			}
    		
    		// include in results
			merged.put(id, mergedDoc);
    	}
    	return merged;
    }
	/*
	 * Get the SolrDoc for the specified id
	 */
	public static SolrDoc getSolrDoc(String id) throws SolrServerException, MalformedURLException, UnsupportedType, NotFound, ParserConfigurationException, IOException, SAXException {
		SolrDoc doc = new SolrDoc();

		if (solrServer != null) {
			String query = QUERY + "\"" + id + "\"";
			SolrParams solrParams = SolrRequestParsers.parseQueryString(query);
			QueryResponse qr = solrServer.query(solrParams);
			SolrDocument orig = qr.getResults().get(0);
			IndexSchema indexSchema = SolrQueryServiceController.getInstance().getSchema();
			for (String fieldName : orig.getFieldNames()) {
				// don't transfer the copyTo fields, otherwise there are errors
				if (indexSchema.isCopyFieldTarget(indexSchema.getField(fieldName))) {
					continue;
				}
				for (Object value : orig.getFieldValues(fieldName)) {
					String stringValue = value.toString();
					// special handling for dates in ISO 8601
					if (value instanceof Date) {
						stringValue = DateTimeMarshaller.serializeDateToUTC((Date) value);
						SolrDateConverter converter = new SolrDateConverter();
						stringValue = converter.convert(stringValue);
					}
					SolrElementField field = new SolrElementField(fieldName, stringValue);
					log.debug("Adding field: " + fieldName);
					doc.addField(field);
				}
			}

		}
		return doc;
	}


}
