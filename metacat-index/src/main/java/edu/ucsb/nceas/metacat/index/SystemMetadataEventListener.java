/**
 *  Copyright: 2013 Regents of the University of California and the
 *             National Center for Ecological Analysis and Synthesis
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
package edu.ucsb.nceas.metacat.index;

import java.io.FileNotFoundException;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v2.SystemMetadata;

import com.hazelcast.core.EntryEvent;
import com.hazelcast.core.EntryListener;
import com.hazelcast.core.IMap;

import edu.ucsb.nceas.metacat.common.index.IndexTask;

public class SystemMetadataEventListener implements EntryListener<Identifier, IndexTask>, Runnable {
	
	private static Log log = LogFactory.getLog(SystemMetadataEventListener.class);
	
	private SolrIndex solrIndex = null;
	
	private IMap<Identifier, IndexTask> source = null;
	        
    /**
     * Default constructor - caller needs to initialize manually
     * @see setSolrIndex()
     * @see start()
     */
    public SystemMetadataEventListener() {
    }
    
    /**
     * Sets the SolrIndex instance and initializes the listener 
     * @param solrIndex
     */
    public SystemMetadataEventListener(SolrIndex solrIndex) {
    	this.solrIndex = solrIndex;
    	try {
			run();
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
    }
    
    /**
     * Get the SolrIndex that this listener communicates with
     * @return a SolrIndex instance that indexes the content being listened to
     */
    public SolrIndex getSolrIndex() {
		return solrIndex;
	}

    /**
     * Set the SolrIndex instance that the listener modifies
     * @param solrIndex
     */
	public void setSolrIndex(SolrIndex solrIndex) {
		this.solrIndex = solrIndex;
	}

	/**
     * Registers this instance as a system metadata map event listener
	 * @throws ServiceFailure 
	 * @throws FileNotFoundException 
     */
    public void run() {
    	
        // get shared structures and add listener
    	try {
	        IMap<Identifier, String> objectPathMap = DistributedMapsFactory.getObjectPathMap();
	        IMap<Identifier, IndexTask> indexQueue = DistributedMapsFactory.getIndexQueue();
	        indexQueue.addEntryListener(this, true);
	        this.source = indexQueue;
	        log.info("System Metadata size: " + indexQueue.size());
	        log.info("Object path size:" + objectPathMap.size());
    	} catch (Exception e) {
    		log.error("Cannot start SystemMetadata listener" , e);
    	}
    }

    /**
     * Removes this instance as a system metadata map event listener
     * @throws ServiceFailure 
     * @throws FileNotFoundException 
     */
    public void stop() throws FileNotFoundException, ServiceFailure {
    	log.info("stopping index entry listener...");
    	DistributedMapsFactory.getIndexQueue().removeEntryListener(this);
    }

    public void entryRemoved(EntryEvent<Identifier, IndexTask> entryEvent) {
        // do nothing
    }
    public void entryEvicted(EntryEvent<Identifier, IndexTask> entryEvent) {
    	// do nothing
    }
    public void entryAdded(EntryEvent<Identifier, IndexTask> entryEvent) {
    	entryUpdated(entryEvent);
    }
    
	public void entryUpdated(EntryEvent<Identifier, IndexTask> entryEvent) {
	    //System.out.println("===================================calling entryUpdated method ");
	    log.info("===================================SystemMetadataEventListener. entryUpdated - calling SystemMetadataEventListener.itemAdded method ");
		// add to the index
		Identifier pid = entryEvent.getKey();
		//System.out.println("===================================update the document "+pid.getValue());
		log.info("===================================SystemMetadataEventListener. entryUpdated - adding the document " + pid.getValue());
		
		// what do we have to index?
		IndexTask task = entryEvent.getValue();
		SystemMetadata systemMetadata = task.getSystemMetadata();
		Map<String, List<Object>> fields = task.getFields();
		
		/*if(systemMetadata == null) {
		    writeEventLog(systemMetadata, pid, "SystemMetadataEventListener.itemAdded -could not get the SystemMetadata");
		    return;
		}*/
		
		// make sure we remove this task so that it can be re-added in the future
		if (source != null && pid != null) {
			source.remove(pid);
		}
		
		if (systemMetadata != null) {
		    solrIndex.update(pid, systemMetadata);
			
		}
		if (fields != null) {
			solrIndex.insertFields(pid, fields);
		}
		
	}
	
	
    
}
