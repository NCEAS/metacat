package edu.ucsb.nceas.metacat.index.annotation;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.xpath.XPathExpressionException;

import org.apache.commons.codec.EncoderException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dataone.cn.indexer.annotation.AnnotatorSubprocessor;
import org.dataone.cn.indexer.solrhttp.SolrDoc;
import org.dataone.cn.indexer.solrhttp.SolrElementField;

import edu.ucsb.nceas.metacat.index.resourcemap.ResourceMapSubprocessor;

public class MetacatAnnotatorSubprocessor extends AnnotatorSubprocessor {
	
    private static Log log = LogFactory.getLog(MetacatAnnotatorSubprocessor.class);
	
    
    @Override
    public Map<String, SolrDoc> processDocument(String annotationId, Map<String, SolrDoc> docs,
            InputStream is) throws Exception {

        // check for annotations, and add them if found
        SolrDoc annotations = parseAnnotation(is);
        if (annotations != null) {
            String referencedPid = annotations.getIdentifier();
            SolrDoc referencedDoc = docs.get(referencedPid);

            // make sure we have a reference for the document we annotating
            boolean referenceExists = true;
            if (referencedDoc == null) {
                try {
                    referencedDoc = ResourceMapSubprocessor.getSolrDoc(referencedPid);
                } catch (Exception e) {
                    log.error("Unable to retrieve solr document: " + referencedPid
                            + ".  Exception attempting to communicate with solr server.", e);
                }

                
                if (referencedDoc == null) {
                    referencedDoc = new SolrDoc();
                    referenceExists = false;
                }
                docs.put(referencedPid, referencedDoc);
            }

            // make sure we say we annotate the object
            SolrDoc annotationDoc = docs.get(annotationId);
            if (annotationDoc != null) {
                annotationDoc.addField(new SolrElementField(FIELD_ANNOTATES, referencedPid));
            }

            // add the annotations to the referenced document
            Iterator<SolrElementField> annotationIter = annotations.getFieldList().iterator();
            while (annotationIter.hasNext()) {
                SolrElementField annotation = annotationIter.next();
                // only skip merge field if there was an existing record
                if (referenceExists && !this.getFieldsToMerge().contains(annotation.getName())) {
                    log.debug("SKIPPING field (not in fieldsToMerge): " + annotation.getName());
                    continue;
                }
                referencedDoc.addField(annotation);
                log.debug("ADDING annotation to " + referencedPid + ": " + annotation.getName()
                        + "=" + annotation.getValue());
            }
        } else {
            log.warn("Annotations were not found when parsing: " + annotationId);
        }
        // return the collection that we have augmented
        return docs;
    }
    
    /**
     * Merge updates with existing solr documents
     * 
     * @param indexDocument
     * @return
     * @throws IOException
     * @throws EncoderException
     * @throws XPathExpressionException
     */
    public SolrDoc mergeWithIndexedDocument(SolrDoc indexDocument) throws IOException,
            EncoderException, XPathExpressionException {	
        
		return mergeWithIndexedDocument(indexDocument, getFieldsToMerge());
    }
    
    /**
     * Inspired by SubprocessorUtility method, but works with embedded solr server
     * @param indexDocument
     * @param fieldsToMerge
     * @return
     * @throws IOException
     * @throws EncoderException
     * @throws XPathExpressionException
     */
    private SolrDoc mergeWithIndexedDocument(SolrDoc indexDocument, List<String> fieldsToMerge)
            throws IOException, EncoderException, XPathExpressionException {

        log.debug("about to merge indexed document with new doc to insert for pid: "
                + indexDocument.getIdentifier());
        SolrDoc solrDoc = null;
		try {
			solrDoc = ResourceMapSubprocessor.getSolrDoc(indexDocument.getIdentifier());
		} catch (Exception e) {
			log.error("Could not retrieve existing index document: " + indexDocument.getIdentifier(), e);
		} 
        if (solrDoc != null) {
            log.debug("found existing doc to merge for pid: " + indexDocument.getIdentifier());
            for (SolrElementField field : solrDoc.getFieldList()) {
                if (fieldsToMerge.contains(field.getName())
                        && !indexDocument.hasFieldWithValue(field.getName(), field.getValue())) {
                    indexDocument.addField(field);
                    log.debug("merging field: " + field.getName() + " with value: "
                            + field.getValue());
                }
            }
        }
        return indexDocument;
    }

}
