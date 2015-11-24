/**
 * This work was created by participants in the DataONE project, and is
 * jointly copyrighted by participating institutions in DataONE. For 
 * more information on DataONE, see our web site at http://dataone.org.
 *
 *   Copyright ${year}
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and 
 * limitations under the License.
 * 
 * $Id$
 */

package edu.ucsb.nceas.metacat.index;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.xpath.XPathExpressionException;

import org.apache.commons.codec.EncoderException;
import org.apache.commons.lang.StringUtils;
import org.dataone.cn.indexer.parser.IDocumentDeleteSubprocessor;
import org.dataone.cn.indexer.solrhttp.SolrDoc;
import org.dataone.cn.indexer.solrhttp.SolrElementField;

import edu.ucsb.nceas.metacat.index.resourcemap.ResourceMapSubprocessor;


public class MetacatDocumentDeleteSubprocessor implements IDocumentDeleteSubprocessor {


    private String relationSourceFormatId;
    private String relationSourceField;
    private List<String> biDirectionalRelationFields;
    private List<String> uniDirectionalRelationFields;

    public MetacatDocumentDeleteSubprocessor() {
    }

    public Map<String, SolrDoc> processDocForDelete(String identifier, Map<String, SolrDoc> docs)
            throws Exception {

        SolrDoc indexedDoc = ResourceMapSubprocessor.getSolrDoc(identifier);
        if (indexedDoc != null) {
            if (hasRelationsBySource(indexedDoc)) {
                docs.putAll(removeBiDirectionalRelationsForDoc(identifier, indexedDoc, docs));
            }
            if (isRelationshipSource(indexedDoc)) {
                docs.putAll(removeRelationsBySourceDoc(identifier, indexedDoc, docs));
            }
        }
        return docs;
    }

    private Map<String, SolrDoc> removeRelationsBySourceDoc(String relationSourceId,
            SolrDoc indexedDoc, Map<String, SolrDoc> docs) throws Exception {

        // gather all docs with relations from self source
        List<SolrDoc> relatedDocs = ResourceMapSubprocessor.getDocumentsByQuery(
                "q=" + relationSourceField + ":\"" + relationSourceId + "\"");

        Set<String> otherSourceDocs = new HashSet<String>();

        for (SolrDoc relatedDoc : relatedDocs) {

            // gather other relation source docs from modified list
            otherSourceDocs.addAll(relatedDoc.getAllFieldValues(relationSourceField));

            // remove relation fields (uni and bi-directional)
            // add modified docs to update list
            String docId = relatedDoc.getFirstFieldValue(SolrElementField.FIELD_ID);
            if (docs.get(docId) != null) {
                relatedDoc = docs.get(docId);
            }
            relatedDoc.removeAllFields(relationSourceField);
            for (String relationField : getBiDirectionalRelationFields()) {
                relatedDoc.removeAllFields(relationField);
            }
            for (String relationField : getUniDirectionalRelationFields()) {
                relatedDoc.removeAllFields(relationField);
            }
            docs.put(docId, relatedDoc);
        }

        for (String otherRelatedDoc : otherSourceDocs) {
            if (!otherRelatedDoc.equals(relationSourceId)) {
                docs.put(otherRelatedDoc, null);
            }
        }
        return docs;
    }

    private boolean isRelationshipSource(SolrDoc indexedDoc) throws Exception {
        String formatId = indexedDoc.getFirstFieldValue(SolrElementField.FIELD_OBJECTFORMAT);
        return relationSourceFormatId.equals(formatId);
    }

    private boolean hasRelationsBySource(SolrDoc indexedDoc) throws XPathExpressionException,
            IOException, EncoderException {
        String relationSourceId = indexedDoc.getFirstFieldValue(relationSourceField);
        return StringUtils.isNotEmpty(relationSourceId);
    }

    private Map<String, SolrDoc> removeBiDirectionalRelationsForDoc(String identifier,
            SolrDoc indexedDoc, Map<String, SolrDoc> docs) throws Exception {

        for (String relationField : getBiDirectionalRelationFields()) {
            List<SolrDoc> inverseDocs = ResourceMapSubprocessor.getDocumentsByQuery(
                    "q=" + relationField + ":\"" + identifier + "\"");
            for (SolrDoc inverseDoc : inverseDocs) {
                String inverseDocId = inverseDoc.getFirstFieldValue(SolrElementField.FIELD_ID);
                if (docs.get(inverseDocId) != null) {
                    inverseDoc = docs.get(inverseDocId);
                }
                inverseDoc.removeFieldsWithValue(relationField, identifier);
                docs.put(inverseDocId, inverseDoc);
            }

        }
        return docs;
    }

    private List<String> getBiDirectionalRelationFields() {
        if (biDirectionalRelationFields == null) {
            biDirectionalRelationFields = new ArrayList<String>();
        }
        return biDirectionalRelationFields;
    }

    private List<String> getUniDirectionalRelationFields() {
        if (uniDirectionalRelationFields == null) {
            uniDirectionalRelationFields = new ArrayList<String>();
        }
        return uniDirectionalRelationFields;
    }

    public String getRelationSourceFormatId() {
        return relationSourceFormatId;
    }

    public void setRelationSourceFormatId(String relationSourceFormatId) {
        this.relationSourceFormatId = relationSourceFormatId;
    }

    public String getRelationSourceField() {
        return relationSourceField;
    }

    public void setRelationSourceField(String relationSourceField) {
        this.relationSourceField = relationSourceField;
    }

    public void setBiDirectionalRelationFields(List<String> biDirectionalRelationFields) {
        this.biDirectionalRelationFields = biDirectionalRelationFields;
    }

    public void setUniDirectionalRelationFields(List<String> uniDirectionalRelationFields) {
        this.uniDirectionalRelationFields = uniDirectionalRelationFields;
    }
}
