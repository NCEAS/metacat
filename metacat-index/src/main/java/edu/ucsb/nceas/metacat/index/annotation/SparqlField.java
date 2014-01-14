package edu.ucsb.nceas.metacat.index.annotation;

import java.util.List;

import javax.xml.xpath.XPath;

import org.dataone.cn.indexer.parser.ISolrField;
import org.dataone.cn.indexer.solrhttp.SolrElementField;
import org.w3c.dom.Document;

public class SparqlField implements ISolrField {
	
	private String name;
	
	private String query;
	
	public SparqlField(String name, String query) {
		this.name = name;
		this.query = query;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getQuery() {
		return query;
	}

	public void setQuery(String query) {
		this.query = query;
	}

	@Override
	public List<SolrElementField> getFields(Document arg0, String arg1)
			throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void initExpression(XPath arg0) {
		// TODO Auto-generated method stub
		
	}

}
