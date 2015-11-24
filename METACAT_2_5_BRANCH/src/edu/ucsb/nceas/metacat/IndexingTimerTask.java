/**
 *  '$RCSfile$'
 *    Purpose: A Class that tracks sessions for MetaCatServlet users.
 *  Copyright: 2000 Regents of the University of California and the
 *             National Center for Ecological Analysis and Synthesis
 *    Authors: Matt Jones
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

package edu.ucsb.nceas.metacat;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.TimerTask;
import java.util.Vector;
import java.util.Iterator;

import edu.ucsb.nceas.metacat.common.query.EnabledQueryEngines;
import edu.ucsb.nceas.metacat.database.DBConnection;
import edu.ucsb.nceas.metacat.database.DBConnectionPool;
import edu.ucsb.nceas.metacat.database.DatabaseService;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.metacat.util.MetacatUtil;

import org.apache.log4j.Logger;

public class IndexingTimerTask extends TimerTask{

	 private Logger logMetacat = Logger.getLogger(IndexingTimerTask.class);

	 int count = 0;
	 
	  /*
	     * Run a separate thread to build the XML index for this document.  This
	     * thread is run asynchronously in order to more quickly return control to
	     * the submitting user.  The run method checks to see if the document has
	     * been fully inserted before trying to update the xml_index table.
	     */
	    public void run()
	    {
	        if(!EnabledQueryEngines.getInstance().isEnabled(EnabledQueryEngines.PATHQUERYENGINE)) {
	            return;
	        }
	    	DBConnection dbConn = null;
	    	int serialNumber = 0;
	    	try{
	    		logMetacat.warn("Running indexing timer task");
	    		
	    		dbConn = DBConnectionPool.getDBConnection("IndexingThread");
	    		serialNumber = dbConn.getCheckOutSerialNumber();
	            Vector indexNamespaces = MetacatUtil.getOptionList(PropertyService.getProperty("xml.indexNamespaces"));
	            String nonJoinCriteria = "b.docid is NULL";
	    		boolean first = true;

	    		if(indexNamespaces != null && !indexNamespaces.isEmpty()){
	    			Iterator it = indexNamespaces.iterator();
	    			while(it.hasNext()){
	    				if(first){
	    					nonJoinCriteria = nonJoinCriteria + " AND (";
	    					nonJoinCriteria = nonJoinCriteria 
	    						+ " a.doctype like '" 
	    						+ it.next()
	    						+ "'";

	    					first = false;
	    				} else {
	    					nonJoinCriteria = nonJoinCriteria + " OR ";
	    					nonJoinCriteria = nonJoinCriteria 
	    						+ " a.doctype like '" 
	    						+ it.next()
	    						+ "'";
	    				}
	    			}
	    			nonJoinCriteria = nonJoinCriteria + ")";
	    		} else {
	    			// if no namespaces are defined in metacat.properties then return
	    			return;
	    		}
	    		
	    		String xmlDocumentsCheck = 
	    			DatabaseService.getInstance().getDBAdapter().getLeftJoinQuery("a.docid, a.rev", "xml_documents", 
	    					"xml_index", "a.docid = b.docid", nonJoinCriteria);

	    		PreparedStatement xmlDocCheck = dbConn.prepareStatement(xmlDocumentsCheck);
	    		
	    		// Increase usage count
	    		dbConn.increaseUsageCount(1);
	    		xmlDocCheck.execute();
	    		ResultSet rs = xmlDocCheck.getResultSet();
	    		
	    		boolean tableHasRows = rs.next();
	    		while (tableHasRows) {
	    			String docid = rs.getString(1);
	    			String rev = rs.getString(2);
	    			
	    			IndexingQueue.getInstance().add(docid, rev);

	    			tableHasRows = rs.next();
	            }
	                	
	    		rs.close();
	    		xmlDocCheck.close();
	    		
	       	} catch (SQLException se){
	       				se.printStackTrace();
		        		
		    } catch (Exception e){
	        		e.printStackTrace();
	        		
	        }finally {
	                DBConnectionPool.returnDBConnection(dbConn, serialNumber);
	        }
		      	
			logMetacat.warn("Indexing timer task returning");		
			count++;
	    }
}
