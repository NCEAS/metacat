/**
 *  '$RCSfile$'
 *    Purpose: A Class that stores user session information 
 *  Copyright: 2008 Regents of the University of California and the
 *             National Center for Ecological Analysis and Synthesis
 *    Authors: Michael Daigle
 * 
 *   '$Author$'
 *     '$Date$'
 * '$Revision$'
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

package edu.ucsb.nceas.metacat.util;

import java.util.Calendar;
import java.util.Date;

import org.apache.log4j.Logger;

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
	
	private Logger logMetacat = Logger.getLogger(SessionData.class);
	
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
