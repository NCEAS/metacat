package edu.ucsb.nceas.metacat.util;

import java.util.Calendar;
import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.ucsb.nceas.metacat.cart.DocumentCart;

public class SessionData {
	
	private String id = null;
	private String userName = null;
	private String[] groupNames = null;
	private String password = null;
	private String name = null;
	private final Calendar creationTime = Calendar.getInstance();
	private Calendar lastAccessedTime = Calendar.getInstance();
	private DocumentCart documentCart = null;
	
	private Log logMetacat = LogFactory.getLog(SessionData.class);
	
	/**
	 * 
	 * @param userName
	 * @param groupNames
	 * @param password
	 */
	public SessionData(String id, String userName, String[] groupNames, String password, String name) {
		this.id = id;
		this.userName = userName;
		this.groupNames = groupNames;
		this.password = password;
		this.name = name;
	}
	
	public String getId() {
		return id;
	}
	
	public String getUserName() {
		return userName;
	}
	
	public void setUserName(String userName) {
		this.userName = userName;
	}
	
	public String[] getGroupNames() {
		return groupNames;
	}
	
	public void setGroupNames(String[] groupNames) {
		this.groupNames = groupNames;
	}
	
	public String getPassword() {
		return password;
	}
	
	public void setPassword(String password) {
		this.password = password;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public Calendar getLastAccessedTime() {
		return lastAccessedTime;
	}
	
	public void setLastAccessedTime() {
		lastAccessedTime.setTime(new Date());
	}
	
	public Calendar getCreationTime() {
		return creationTime;
	}

	public DocumentCart getDocumentCart() {
		if (documentCart == null) {
			documentCart = new DocumentCart();
		}
		return documentCart;
	}

	public void setDocumentCart(DocumentCart documentCart) {
		this.documentCart = documentCart;
	}
}
