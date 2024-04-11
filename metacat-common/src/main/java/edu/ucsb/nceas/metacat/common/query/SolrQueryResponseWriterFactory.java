package edu.ucsb.nceas.metacat.common.query;

import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.response.CSVResponseWriter;
import org.apache.solr.response.JSONResponseWriter;
import org.apache.solr.response.PHPResponseWriter;
import org.apache.solr.response.PHPSerializedResponseWriter;
import org.apache.solr.response.PythonResponseWriter;
import org.apache.solr.response.QueryResponseWriter;
import org.apache.solr.response.RubyResponseWriter;
//import org.apache.solr.response.VelocityResponseWriter;
import org.apache.solr.response.XMLResponseWriter;

/**
 * Generate a QueryResponseWriter base on the specified format.
 * @author tao
 *
 */
public class SolrQueryResponseWriterFactory {
    public static final String XML = "xml";
    public static final String JSON = "json";
    public static final String PYTHON = "python";
    public static final String RUBY = "ruby";
    public static final String PHP = "php";
    public static final String PHPS = "phps";
    public static final String VELOCITY = "velocity";
    public static final String CSV ="csv";
    
   /** Create a QueryReponseWriter based on the specified wt format.    
     * Here is the list of the handler class to handle different format.
     * <queryResponseWriter name="xml" default="true" class="solr.XMLResponseWriter" />
     * <queryResponseWriter name="json" class="solr.JSONResponseWriter"/>
     * <queryResponseWriter name="python" class="solr.PythonResponseWriter"/>
     * <queryResponseWriter name="ruby" class="solr.RubyResponseWriter"/>
     * <queryResponseWriter name="php" class="solr.PHPResponseWriter"/>
     * <queryResponseWriter name="phps" class="solr.PHPSerializedResponseWriter"/>
     * <queryResponseWriter name="velocity" class="solr.VelocityResponseWriter"/>
     * <queryResponseWriter name="csv" class="solr.CSVResponseWriter"/>
     */
    public static QueryResponseWriter generateResponseWriter(String wt) throws SolrServerException {
        QueryResponseWriter writer = null;
        if(wt == null || wt.trim().equals("") || wt.equals(XML)) {
            writer = new XMLResponseWriter();
        } else if(wt.equals(JSON)) {
            writer = new JSONResponseWriter();
        } else if(wt.equals(PYTHON)) {
            writer = new PythonResponseWriter();
        } else if(wt.equals(RUBY)) {
            writer = new RubyResponseWriter();
        } else if(wt.equals(PHP)) {
            writer = new PHPResponseWriter();
        } else if(wt.equals(PHPS)) {
            writer = new PHPSerializedResponseWriter();
        /*} else if(wt.equals(VELOCITY)) {
            writer = new VelocityResponseWriter();*/
        } else if(wt.equals(CSV)) {
            writer = new CSVResponseWriter();
        } else {
            throw new SolrServerException("Metacat doesn't support this response format "+wt);
        }
        return writer;
    }

	public static String getContentType(String wt) {
		String contentType = null;
		if (wt == null || wt.trim().equals("") || wt.equals(XML)) {
			contentType = "text/xml";
		} else if (wt.equals(JSON)) {
			contentType = "text/json";
		} else if (wt.equals(PYTHON)) {
			contentType = "text/plain";
		} else if (wt.equals(RUBY)) {
			contentType = "text/plain";
		} else if (wt.equals(PHP)) {
			contentType = "text/plain";
		} else if (wt.equals(PHPS)) {
			contentType = "text/plain";
		} else if (wt.equals(VELOCITY)) {
			contentType = "text/html";
		} else if (wt.equals(CSV)) {
			contentType = "text/csv";
		} else {
			// just don't know what it is...
			contentType = null;
		}
		return contentType;
	}
}
