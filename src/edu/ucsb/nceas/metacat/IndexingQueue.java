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
import java.util.Vector;
import java.util.HashMap;
import java.lang.Comparable;

import edu.ucsb.nceas.metacat.common.query.EnabledQueryEngines;
import edu.ucsb.nceas.metacat.database.DBConnection;
import edu.ucsb.nceas.metacat.database.DBConnectionPool;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;

import org.apache.log4j.Logger;

public class IndexingQueue {

	private static Logger logMetacat = Logger.getLogger(IndexingQueue.class);
	//	 Map used to keep tracks of docids to be indexed
	private HashMap<String, IndexingQueueObject> indexingMap = new HashMap<String, IndexingQueueObject>();     
	private Vector<IndexingTask> currentThreads = new Vector<IndexingTask>();
	public Vector<String> currentDocidsBeingIndexed = new Vector<String>();
	private boolean metacatRunning = true;
	
	private static IndexingQueue instance = null;

	final static int NUMBEROFINDEXINGTHREADS;
	static {
		int numIndexingThreads = 0;
		try {
			numIndexingThreads = 
				Integer.parseInt(PropertyService.getProperty("database.numberOfIndexingThreads"));
		} catch (PropertyNotFoundException pnfe) {
			logMetacat.error("Could not get property in static block: " 
					+ pnfe.getMessage());
		}
		NUMBEROFINDEXINGTHREADS = numIndexingThreads;
	}

    private IndexingQueue() {
        if(!EnabledQueryEngines.getInstance().isEnabled(EnabledQueryEngines.PATHQUERYENGINE)) {
            return;
        }
	    for (int i = 0; i < NUMBEROFINDEXINGTHREADS; i++) {
	    	IndexingTask thread = new IndexingTask();
	    	thread.start();
	    	currentThreads.add(thread);
	    }
    }
	
	public static synchronized IndexingQueue getInstance(){
		if (instance == null) {
			instance = new IndexingQueue();
		}
		return instance;
	}//getInstance

    public void add(String docid, String rev) {
        if(!EnabledQueryEngines.getInstance().isEnabled(EnabledQueryEngines.PATHQUERYENGINE)) {
            return;
        }
    	add(new IndexingQueueObject(docid, rev, 0));
    }
    
    protected void add(IndexingQueueObject queueObject) {
	    synchronized (indexingMap) {
	    	if(!indexingMap.containsKey(queueObject.getDocid())){
	    		indexingMap.put(queueObject.getDocid(), queueObject);
	    		indexingMap.notify();
	    	} else {
	    		IndexingQueueObject oldQueueObject = indexingMap.get(queueObject.getDocid());
	    		if(oldQueueObject.compareTo(queueObject) < 0){
	    	  		indexingMap.put(queueObject.getDocid(), queueObject);
		    		indexingMap.notify();  			
	    		}
	    	}
	    }
	  }
    
	public boolean getMetacatRunning(){
		return this.metacatRunning;
	}
	
	public void setMetacatRunning(boolean metacatRunning){
	    if(!EnabledQueryEngines.getInstance().isEnabled(EnabledQueryEngines.PATHQUERYENGINE)) {
            return;
        }
		this.metacatRunning = metacatRunning;
		
		if(!metacatRunning){
			for(int count=0; count<currentThreads.size(); count++){
				currentThreads.get(count).metacatRunning = false;
				currentThreads.get(count).interrupt();
			}
		}
	}
	
    protected IndexingQueueObject getNext() {
    	IndexingQueueObject returnVal = null;
        synchronized (indexingMap) {
          while (indexingMap.isEmpty() && metacatRunning) {
            try {
            	indexingMap.wait();
            } catch (InterruptedException ex) {
              logMetacat.error("Interrupted");
            }
          }
          
          if(metacatRunning){
        	  String docid = indexingMap.keySet().iterator().next();
        	  returnVal = indexingMap.get(docid);
        	  indexingMap.remove(docid);
          }
        }
        return returnVal;
      }
    
    /**
     * Removes the Indexing Task object from the queue
     * for the given docid. Currently, rev is ignored
     * This method should be used to cancel scheduled indexing on a document
     * (typically if it is being deleted but indexing has not completed yet)
     * see http://bugzilla.ecoinformatics.org/show_bug.cgi?id=5750
     * @param docid the docid (without revision)
     * @param rev the docid's rev (ignored)
     */
    public void remove(String docid, String rev) {
        if(!EnabledQueryEngines.getInstance().isEnabled(EnabledQueryEngines.PATHQUERYENGINE)) {
            return;
        }
		synchronized (indexingMap) {
			if (indexingMap.containsKey(docid)) {
				logMetacat.debug("Removing indexing queue task for docid: " + docid);
				indexingMap.remove(docid);
			}
		}
	}

}

class IndexingTask extends Thread {
  	  private Logger logMetacat = Logger.getLogger(IndexingTask.class);
  	  
  	  
      protected final static long MAXIMUMINDEXDELAY;
      static {
    	  long maxIndexDelay = 0;
    	  try {
    		  maxIndexDelay = 
    			  Integer.parseInt(PropertyService.getProperty("database.maximumIndexDelay")); 
    	  } catch (PropertyNotFoundException pnfe) {
    		  System.err.println("Could not get property in static block: " 
  					+ pnfe.getMessage());
    	  }
    	  MAXIMUMINDEXDELAY = maxIndexDelay;
      }
      
      protected boolean metacatRunning = true;
      	
	  public void run() {
	    while (metacatRunning) {
	      // blocks until job
	      IndexingQueueObject indexQueueObject = 
	    	  IndexingQueue.getInstance().getNext();

	      if(indexQueueObject != null){
	    	  if(!IndexingQueue.getInstance().
	    			currentDocidsBeingIndexed.contains(indexQueueObject.getDocid())){
    		  try {
    			  IndexingQueue.getInstance().
    			  		currentDocidsBeingIndexed.add(indexQueueObject.getDocid());
    		      String docid = indexQueueObject.getDocid() + "." + indexQueueObject.getRev();
    			  if(checkDocumentTable(docid, "xml_documents")){
    				  logMetacat.warn("Calling buildIndex for " + docid);
    				  DocumentImpl doc = new DocumentImpl(docid, false);
    				  doc.buildIndex();
    				  logMetacat.warn("finish building index for doicd "+docid);
    			  } else {
    				  logMetacat.warn("Couldn't find the docid:" + docid 
	                			+ " in xml_documents table");
	                  sleep(MAXIMUMINDEXDELAY);
	                  throw(new Exception("Couldn't find the docid:" + docid 
	                			+ " in xml_documents table"));
    			  }
    		  	} catch (Exception e) {
    			  logMetacat.warn("Exception: " + e);
    			  e.printStackTrace();
	        
    			  if(indexQueueObject.getCount() < 25){
    				  indexQueueObject.setCount(indexQueueObject.getCount()+1);
    				  // add the docid back to the list
    				  IndexingQueue.getInstance().add(indexQueueObject);
    			  } else {
    				  logMetacat.fatal("Docid " + indexQueueObject.getDocid() 
	        			+ " has been inserted to IndexingQueue "
	        			+ "more than 25 times. Not adding the docid to"
	        			+ " the queue again.");
    			  }
    		  	} finally {
    			  	IndexingQueue.getInstance().currentDocidsBeingIndexed
    			  		.remove(indexQueueObject.getDocid());	    	  
    		  	}
	    	  } else {
	    		  indexQueueObject.setCount(indexQueueObject.getCount()+1);
	    		  IndexingQueue.getInstance().add(indexQueueObject);    		  
	    	  }
	      }
	    }
	  }
	  
      private boolean checkDocumentTable(String docid, String tablename) throws Exception{
	        DBConnection dbConn = null;
	        int serialNumber = -1;
	        boolean inxmldoc = false;
            
	        String revision = docid.substring(docid.lastIndexOf(".")+1,docid.length());
	        int rev = Integer.parseInt(revision);
	        docid = docid.substring(0,docid.lastIndexOf("."));

	        logMetacat.info("Checking if document exists in xml_documents: docid is " 
	        		+ docid + " and revision is " + revision);

	        try {
	            // Opening separate db connection for writing XML Index
	            dbConn = DBConnectionPool
	                    .getDBConnection("DBSAXHandler.checkDocumentTable");
	            serialNumber = dbConn.getCheckOutSerialNumber();

	            String xmlDocumentsCheck = "SELECT distinct docid FROM " + tablename
	                        + " WHERE docid = ? " 
	                        + " AND rev = ?";

                PreparedStatement xmlDocCheck = dbConn.prepareStatement(xmlDocumentsCheck);
                xmlDocCheck.setString(1, docid);
                xmlDocCheck.setInt(2, rev);
                // Increase usage count
                dbConn.increaseUsageCount(1);
	            xmlDocCheck.execute();
	            ResultSet doccheckRS = xmlDocCheck.getResultSet();
	            boolean tableHasRows = doccheckRS.next();
	            if (tableHasRows) {
	            	inxmldoc = true;
	            }
	            doccheckRS.close();
	            xmlDocCheck.close();
	        } catch (SQLException e) {
	        	   e.printStackTrace();
	        } finally {
	            DBConnectionPool.returnDBConnection(dbConn, serialNumber);
	        }//finally
	        
	        return inxmldoc;
      }
	}

class IndexingQueueObject implements Comparable{
	// the docid of the document to be indexed. 
	private String docid;
	// the docid of the document to be indexed. 
	private String rev;
	// the count of number of times the document has been in the queue
	private int count;
	
	IndexingQueueObject(String docid, String rev, int count){
		this.docid = docid;
		this.rev = rev;
		this.count = count;
	}
	
	public int getCount(){
		return count;
	}

	public String getDocid(){
		return docid;
	}

	public String getRev(){
		return rev;
	}

	public void setCount(int count){
		this.count = count;
	}
	
	public void setDocid(String docid){
		this.docid = docid;
	}

	public void setRev(String rev){
		this.rev = rev;
	}
	
	public int compareTo(Object o){
		if(o instanceof IndexingQueueObject){
			int revision = Integer.parseInt(rev);
			int oRevision = Integer.parseInt(((IndexingQueueObject)o).getRev());
			
			if(revision == oRevision) {
				return 0;
			} else if (revision > oRevision) {
				return 1;
			} else {
				return -1;
			}
		} else {
			throw new java.lang.ClassCastException();
		}
	}
}
