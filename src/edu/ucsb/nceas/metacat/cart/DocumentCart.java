package edu.ucsb.nceas.metacat.cart;

import java.util.HashMap;
import java.util.Map;

public class DocumentCart {

	
	private Map docids = new HashMap();
	
	private Map fields = new HashMap();
	
	public void addDocument(String docid, Map fields) {
		docids.put(docid, fields);
	}
	
	public void removeDocument(String docid) {
		docids.remove(docid);
	}
	
	public Map getFields(String docid) {
		return (Map) docids.get(docid);
	}
	
	public String[] getDocids() {
		return (String[]) docids.keySet().toArray(new String[0]);
	}
	
	public void clear() {
		docids.clear();
	}
	
	//for accessing the metadata attribute field mappings
	public void addField(String label, String path) {
		fields.put(label, path);
	}
	
	public void removeField(String label) {
		fields.remove(label);
	}
	
	public void clearFields() {
		fields.clear();
	}
	
	public Map getFields() {
		return fields;
	}
	
	public void addFields(Map f) {
		fields.putAll(f);
	}
	
	public String[] getLabels() {
		return (String[]) fields.keySet().toArray(new String[0]);
	}
	
	public String[] getPaths() {
		return (String[]) fields.values().toArray(new String[0]);
	}
}
