package edu.ucsb.nceas.metacat.event;

public class MetacatDocumentEvent implements MetacatEvent {

	private String docid;
	private String action;
	private String doctype;
	private String user;
	private String[] groups;
	
	public MetacatDocumentEvent() {
	}

	public String getDocid() {
		return docid;
	}

	public void setDocid(String docid) {
		this.docid = docid;
	}

	public String getDoctype() {
		return doctype;
	}

	public void setDoctype(String doctype) {
		this.doctype = doctype;
	}

	public String getAction() {
		return action;
	}

	public void setAction(String action) {
		this.action = action;
	}

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public String[] getGroups() {
		return groups;
	}

	public void setGroups(String[] groups) {
		this.groups = groups;
	}
	
}
