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
package edu.ucsb.nceas.metacat.index.resourcemap;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import org.apache.commons.codec.EncoderException;
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
import org.dataone.cn.indexer.resourcemap.ResourceMap;
import org.dataone.cn.indexer.resourcemap.ResourceMapFactory;
import org.dataone.cn.indexer.solrhttp.SolrDoc;
import org.dataone.cn.indexer.solrhttp.SolrElementField;
import org.dataone.service.exceptions.NotFound;
import org.dataone.service.exceptions.NotImplemented;
import org.dataone.service.exceptions.UnsupportedType;
import org.dataone.service.util.DateTimeMarshaller;
import org.dspace.foresite.OREParserException;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import edu.ucsb.nceas.metacat.common.SolrServerFactory;
import edu.ucsb.nceas.metacat.common.query.SolrQueryServiceController;
import edu.ucsb.nceas.metacat.index.SolrIndex;


/**
 * A solr index parser for the ResourceMap file.
 * The solr doc of the ResourceMap self only has the system metadata information.
 * The solr docs of the science metadata doc and data file have the resource map package information.
 */
public class ResourceMapSubprocessor extends AbstractDocumentSubprocessor implements IDocumentSubprocessor {

    private static final String QUERY ="q=id:";
    private static Log log = LogFactory.getLog(SolrIndex.class);
    private static SolrServer solrServer =  null;
    static {
        try {
            solrServer = SolrServerFactory.createSolrServer();
        } catch (Exception e) {
            log.error("ResourceMapSubprocessor - can't generate the SolrServer since - "+e.getMessage());
        }
    }
          
    @Override
    public Map<String, SolrDoc> processDocument(String identifier, Map<String, SolrDoc> docs,
    Document doc) throws IOException, EncoderException, SAXException,
    XPathExpressionException, ParserConfigurationException, SolrServerException, NotImplemented, NotFound, UnsupportedType, OREParserException, ResourceMapException {
        SolrDoc resourceMapDoc = docs.get(identifier);
        List<SolrDoc> processedDocs = processResourceMap(resourceMapDoc, doc);
        Map<String, SolrDoc> processedDocsMap = new HashMap<String, SolrDoc>();
        for (SolrDoc processedDoc : processedDocs) {
            processedDocsMap.put(processedDoc.getIdentifier(), processedDoc);
        }
        return processedDocsMap;
    }

    private List<SolrDoc> processResourceMap(SolrDoc indexDocument, Document resourceMapDocument)
                    throws XPathExpressionException, IOException, SAXException, ParserConfigurationException, EncoderException, SolrServerException, NotImplemented, NotFound, UnsupportedType, OREParserException, ResourceMapException{
        //ResourceMap resourceMap = new ResourceMap(resourceMapDocument);
        ResourceMap resourceMap = ResourceMapFactory.buildResourceMap(resourceMapDocument);
        List<String> documentIds = resourceMap.getAllDocumentIDs();//this list includes the resourceMap id itself.
        //List<SolrDoc> updateDocuments = getHttpService().getDocuments(getSolrQueryUri(), documentIds);
        List<SolrDoc> updateDocuments = getSolrDocs(resourceMap.getIdentifier(), documentIds);
        List<SolrDoc> mergedDocuments = resourceMap.mergeIndexedDocuments(updateDocuments);
        /*if(mergedDocuments != null) {
            for(SolrDoc doc : mergedDocuments) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                doc.serialize(out, "UTF-8");
                String result = new String(out.toByteArray(), "UTF-8");
                System.out.println("after updated document===========================");
                System.out.println(result);
            }
        }*/
        mergedDocuments.add(indexDocument);
        return mergedDocuments;
    }
    
    private List<SolrDoc> getSolrDocs(String resourceMapId, List<String> ids) throws SolrServerException, IOException, ParserConfigurationException, SAXException, XPathExpressionException, NotImplemented, NotFound, UnsupportedType, ResourceMapException {
        List<SolrDoc> list = new ArrayList<SolrDoc>();
        if(ids != null) {
            for(String id : ids) {
            	SolrDoc doc = getSolrDoc(id);
                if(doc != null) {
                    list.add(doc);
                } else if ( !id.equals(resourceMapId)) {
                    throw new ResourceMapException("Solr index doesn't have the information about the id "+id+" which is a component in the resource map "+resourceMapId+". Metacat-Index can't process the resource map prior to its components.");
                }
            }
        }
        return list;
    } 
    
    /*
     * Get the SolrDoc list for the list of the ids.
     */
    public static List<SolrDoc> getSolrDocs(List<String> ids) throws SolrServerException, IOException, ParserConfigurationException, SAXException, XPathExpressionException, NotImplemented, NotFound, UnsupportedType {
        List<SolrDoc> list = new ArrayList<SolrDoc>();
        if(ids != null) {
            for(String id : ids) {
            	SolrDoc doc = getSolrDoc(id);
                if(doc != null) {
                    list.add(doc);
                }
            }
        }
        return list;
    }
    
	/*
	 * Get the SolrDoc for the specified id
	 */
	public static SolrDoc getSolrDoc(String id) throws SolrServerException,
			IOException, ParserConfigurationException, SAXException,
			XPathExpressionException, NotImplemented, NotFound, UnsupportedType {
		SolrDoc doc = null;

		if (solrServer != null) {
			String query = QUERY + "\"" + id + "\"";
			SolrParams solrParams = SolrRequestParsers.parseQueryString(query);
			QueryResponse qr = solrServer.query(solrParams);
			if (qr.getResults().size() > 0) {
				SolrDocument orig = qr.getResults().get(0);
				doc = new SolrDoc();
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

		}
		return doc;
	}


}
