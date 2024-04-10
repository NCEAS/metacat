package edu.ucsb.nceas.metacat;

/**
 * A data structure holding the information to be logged about an event.
 * 
 * TODO: add a timestamp field for when the event occurred.
 * 
 * @author jones
 */
public class EventLogData {
	private String ipAddress;
	private String userAgent;
	private String principal;
	private String docid;
	private String event;
	
	
	/**
	 * Construct an EventLogData object with event log information.
	 * 
	 * @param ipAddress the internet protocol address for the event
	 * @param principal the principal for the event (a username, etc)
	 * @param docid the identifier of the document to which the event applies
	 * @param revision the revision of the document to which the event applies
	 * @param event the string code for the event
	 */
	public EventLogData(String ipAddress, String userAgent, String principal, String docid, String event) {
		this.ipAddress = ipAddress;
		this.userAgent = userAgent;
		this.principal = principal;
		this.docid = docid;
		this.event = event;
	}
	
	/**
	 * Get the current value of the document identifier.
	 * @return Returns the docid.
	 */
	public String getDocid() {
		return docid;
	}
	
	/**
	 * Set the document identifier.
	 * 
	 * @param docid The docid to set.
	 */
	public void setDocid(String docid) {
		this.docid = docid;
	}
	
	/**
	 * Get the current value of the event.
	 * 
	 * @return Returns the event.
	 */
	public String getEvent() {
		return event;
	}
	
	/**
	 * Set the current value of the event.
	 * 
	 * @param event The event to set.
	 */
	public void setEvent(String event) {
		this.event = event;
	}
	
	/**
	 * Get the current value of the internet protocol address.
	 * 
	 * @return Returns the ipAddress.
	 */
	public String getIpAddress() {
		return ipAddress;
	}
	
	/**
	 * Set the current value of the internet protocol address.
	 * 
	 * @param ipAddress The ipAddress to set.
	 */
	public void setIpAddress(String ipAddress) {
		this.ipAddress = ipAddress;
	}
	
	/**
	 * Get the current value of the principal.  This will be a username or 
	 * other user identifier.
	 * 
	 * @return Returns the principal.
	 */
	public String getPrincipal() {
		return principal;
	}
	
	/**
	 * Set the current value of the principal.  This will be a username or 
	 * other user identifier.
	 * 
	 * @param principal The principal to set.
	 */
	public void setPrincipal(String principal) {
		this.principal = principal;
	}

	public String getUserAgent() {
		return userAgent;
	}

	public void setUserAgent(String userAgent) {
		this.userAgent = userAgent;
	}
}
