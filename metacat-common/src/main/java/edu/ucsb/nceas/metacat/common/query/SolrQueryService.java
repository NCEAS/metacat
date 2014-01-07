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
package edu.ucsb.nceas.metacat.common.query;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.solr.common.params.AppendedSolrParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.schema.IndexSchema;
import org.apache.solr.schema.SchemaField;
import org.dataone.service.types.v1.Subject;
import org.dataone.service.util.Constants;

import edu.ucsb.nceas.metacat.common.query.SolrQueryResponseWriterFactory;


/**
 * An abstract query service class for the solr server
 * @author tao
 *
 */
public abstract class SolrQueryService {
    
    public static final String WT = "wt";//the property name to specify the return type
    
    protected static final String FILTERQUERY = "fq";
    protected static final String UNKNOWN = "Unknown";
    private static final String READPERMISSION = "readPermission";
    private static final String RIGHTSHOLDER = "rightsHolder";
    private static final String OPENPARENTHESE = "(";
    private static final String CLOSEPARENTHESE = ")";
    private static final String COLON = ":";
    private static final String OR = "OR";
    
    private static Log log = LogFactory.getLog(SolrQueryService.class);
    private static List<String> supportedWriterTypes = null;
    
    protected IndexSchema schema = null;
    protected Map<String, SchemaField> fieldMap = null;
    protected List<String> validSolrFieldNames = null;
    protected String solrSpecVersion = null;
    
    static {
        supportedWriterTypes = new ArrayList<String>();
        supportedWriterTypes.add(SolrQueryResponseWriterFactory.CSV);
        supportedWriterTypes.add(SolrQueryResponseWriterFactory.JSON);
        supportedWriterTypes.add(SolrQueryResponseWriterFactory.PHP);
        supportedWriterTypes.add(SolrQueryResponseWriterFactory.PHPS);
        supportedWriterTypes.add(SolrQueryResponseWriterFactory.RUBY);
        supportedWriterTypes.add(SolrQueryResponseWriterFactory.VELOCITY);
        supportedWriterTypes.add(SolrQueryResponseWriterFactory.PYTHON);
        supportedWriterTypes.add(SolrQueryResponseWriterFactory.XML);
    }
  
    
    /**
     * Query the Solr server with specified query and user's identity. If the Subjects
     * is null, there will be no access rules for the query. This is for the embedded solr server.
     * @param query the query params. 
     * @param subjects the user's identity which sent the query
     * @return the response
     * @throws Exception
     */
    public abstract InputStream query(SolrParams query, Set<Subject>subjects) throws Exception;
    
    
  
    
    /**
     * Get the fields list of the index schema
     * @return
     * @throws Exception
     */
    public abstract Map<String, SchemaField> getIndexSchemaFields() throws Exception;
    
    public IndexSchema getSchema() {
		return schema;
	}

	/**
     * Get the version of the solr server.
     * @return
     */
    public abstract String getSolrServerVersion();
    
    /**
     * Get the list of the valid field name (moved the fields names of the CopyFieldTarget).
     * @return
     */
    protected List<String> getValidSchemaFields() {
        if (validSolrFieldNames != null && !validSolrFieldNames.isEmpty()) {
            //System.out.println("the valid file name is\n"+validSolrFieldNames);
            return validSolrFieldNames;
        } else {
            validSolrFieldNames = new ArrayList<String>();
            if(fieldMap != null) {
                Set<String> fieldNames = fieldMap.keySet();
                for(String fieldName : fieldNames) {
                    SchemaField field = fieldMap.get(fieldName);
                    //remove the field which is the target field of a CopyField.
                    if(field != null && !schema.isCopyFieldTarget(field)) {
                         validSolrFieldNames.add(fieldName);
                    }
                }
            }
            //System.out.println("the valid file name is\n"+validSolrFieldNames);
            return validSolrFieldNames;
        }
    }
    
    /**
     * If the solr server supports the specified wt.
     * @param wt
     * @return true if it supports; otherwise false.
     */
    public static boolean isSupportedWT(String wt) {
        if (wt == null ||supportedWriterTypes.contains(wt)) {
            return true;
        } else {
            return false;
        }
    }
    
    /*
     * Append the access filter query to the params
     */
    protected SolrParams appendAccessFilterParams(SolrParams solrParams, Set<Subject>subjects) {
        SolrParams append = null;
        if(solrParams != null) {
            StringBuffer query = generateAccessFilterParamsString(subjects);      
            if(query != null && query.length() != 0) {
                log.info("=================== fq query is "+query.toString());
                NamedList fq = new NamedList();
                fq.add(FILTERQUERY, query.toString());
                SolrParams fqParam = SolrParams.toSolrParams(fq);
                append = new AppendedSolrParams(solrParams, fqParam);
            } else {
                append = solrParams;
            }
        }
        return append;
    }
    
    protected StringBuffer generateAccessFilterParamsString(Set<Subject>subjects) {
        StringBuffer query = new StringBuffer();
        boolean first = true;
        if(subjects != null) {
            for(Subject subject : subjects) {
                if(subject != null) {
                    String subjectName = subject.getValue();
                    if(subjectName != null && !subjectName.trim().equals("")) {
                        if(first) {
                            first = false;
                            query.append(OPENPARENTHESE+READPERMISSION+COLON+"\""+subjectName+"\""+CLOSEPARENTHESE);
                            if(!subjectName.equals(Constants.SUBJECT_PUBLIC) && !subjectName.equals(Constants.SUBJECT_AUTHENTICATED_USER)) {
                                query.append(OR+OPENPARENTHESE+RIGHTSHOLDER+COLON+"\""+subjectName+"\""+CLOSEPARENTHESE);
                            }
                        } else {
                            query.append(OR + OPENPARENTHESE+READPERMISSION+COLON+"\""+subjectName+"\""+CLOSEPARENTHESE);
                            if(!subjectName.equals(Constants.SUBJECT_PUBLIC) && !subjectName.equals(Constants.SUBJECT_AUTHENTICATED_USER)) {
                                query.append(OR + OPENPARENTHESE+RIGHTSHOLDER+COLON+"\""+subjectName+"\""+CLOSEPARENTHESE);
                            }
                        }
                    }
                   
                }
               
            }
        }
        return query;
    }
}
