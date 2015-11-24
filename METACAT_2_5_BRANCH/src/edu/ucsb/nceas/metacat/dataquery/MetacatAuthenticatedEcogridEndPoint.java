/**
 *    '$RCSfile: ConfigurableEcogridEndPoint.java,v $'
 *
 *     '$Author: leinfelder $'
 *       '$Date: 2008/06/20 17:47:12 $'
 *   '$Revision: 1.1 $'
 *
 *  For Details: http://ecoinformatics.org
 *
 * Copyright (c) 2007 The Regents of the University of California.
 * All rights reserved.
 * 
 * Permission is hereby granted, without written agreement and without
 * license or royalty fees, to use, copy, modify, and distribute this
 * software and its documentation for any purpose, provided that the
 * above copyright notice and the following two paragraphs appear in
 * all copies of this software.
 * 
 * IN NO EVENT SHALL THE UNIVERSITY OF CALIFORNIA BE LIABLE TO ANY PARTY
 * FOR DIRECT, INDIRECT, SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES
 * ARISING OUT OF THE USE OF THIS SOFTWARE AND ITS DOCUMENTATION, EVEN
 * IF THE UNIVERSITY OF CALIFORNIA HAS BEEN ADVISED OF THE POSSIBILITY
 * OF SUCH DAMAGE.
 * 
 * THE UNIVERSITY OF CALIFORNIA SPECIFICALLY DISCLAIMS ANY WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE. THE SOFTWARE
 * PROVIDED HEREUNDER IS ON AN "AS IS" BASIS, AND THE UNIVERSITY
 * OF CALIFORNIA HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT,
 * UPDATES, ENHANCEMENTS, OR MODIFICATIONS.
 */
package edu.ucsb.nceas.metacat.dataquery;

import org.ecoinformatics.datamanager.download.AuthenticatedEcogridEndPointInterface;

import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;

/**
 * This class implements AuthenticatedEcogridEndPointInterface 
 * and is configurable using the properties file.
 * Useful for switching to remote endpoints etc.
 * 
 * @author leinfelder
 *
 */
public class MetacatAuthenticatedEcogridEndPoint 
	extends MetacatEcogridEndPoint implements AuthenticatedEcogridEndPointInterface  {  
		
	private String metacatAuthenticatedEcogridEndPoint = null;
	private String sessionId = null;
	
	public MetacatAuthenticatedEcogridEndPoint() {
		super();
		if (!loadProperties()) {
			metacatAuthenticatedEcogridEndPoint = baseURL + "AuthenticatedQueryService";
		}
	}
	
	public MetacatAuthenticatedEcogridEndPoint(String sessionId) {
		this();
		this.sessionId = sessionId;
	}
	
	private boolean loadProperties() {
		try {
			metacatAuthenticatedEcogridEndPoint = PropertyService.getProperty("datamanager.endpoint.authenticatedquery");
		} catch (PropertyNotFoundException e) {
			//e.printStackTrace();
			return false;
		}
		if (metacatAuthenticatedEcogridEndPoint == null || metacatAuthenticatedEcogridEndPoint.length() == 0) {
			return false;
		}
		return true;
	}
	
	 /**
	    * Gets the end point which Metacat implements authenticated ecogrid interface.
	    * This end point will be used to handle ecogrid protocol
        * 
	    * @return end point url string
	    */
	   public String getMetacatAuthenticatedEcogridEndPoint()
	   {
		   return metacatAuthenticatedEcogridEndPoint;
	   }

	   public String getSessionId()
	   {
		   return sessionId;
	   }
	   
	   public void setSessionId(String id) {
		   sessionId = id;
	   }
       
}

