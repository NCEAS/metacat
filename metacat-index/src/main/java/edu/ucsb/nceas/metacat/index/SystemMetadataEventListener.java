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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.types.v1.Event;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.SystemMetadata;

import com.hazelcast.core.IMap;
import com.hazelcast.core.ISet;
import com.hazelcast.core.ItemEvent;
import com.hazelcast.core.ItemListener;

import edu.ucsb.nceas.metacat.common.index.event.IndexEvent;
import edu.ucsb.nceas.metacat.index.event.EventlogFactory;

public class SystemMetadataEventListener implements ItemListener<SystemMetadata> {
	
	private static Log log = LogFactory.getLog(SystemMetadataEventListener.class);
	
	private SolrIndex solrIndex = null;
	
	private ISet<SystemMetadata> source = null;
	        
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
			start();
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
    public void start() throws FileNotFoundException, ServiceFailure {
    	
        // get shared structures and add listener
        IMap<Identifier, String> objectPathMap = DistributedMapsFactory.getObjectPathMap();
        ISet<SystemMetadata> indexQueue = DistributedMapsFactory.getIndexQueue();
        indexQueue.addItemListener(this, true);
        this.source = indexQueue;
        log.info("System Metadata size: " + indexQueue.size());
        log.info("Object path size:" + objectPathMap.size());
    }

    /**
     * Removes this instance as a system metadata map event listener
     * @throws ServiceFailure 
     * @throws FileNotFoundException 
     */
    public void stop() throws FileNotFoundException, ServiceFailure {
    	log.info("stopping index entry listener...");
    	DistributedMapsFactory.getIndexQueue().removeItemListener(this);
    }
    
    /**
     * Get the obsoletes chain of the specified id. The returned list doesn't include
     * the specified id itself. The newer version has the lower index number in the list.
     * Empty list will be returned if there is no document to be obsoleted by this id.
     * @param id
     * @return
     * @throws ServiceFailure
     * @throws FileNotFoundException 
     */
    private List<String> getObsoletes(String id) throws FileNotFoundException, ServiceFailure {
        List<String> obsoletes = new ArrayList<String>();
        while (id != null) {
            SystemMetadata metadata = DistributedMapsFactory.getSystemMetadata(id);
            id = null;//set it to be null in order to stop the while loop if the id can't be assinged to a new value in the following code.
            if(metadata != null) {
                Identifier identifier = metadata.getObsoletes();
                if(identifier != null && identifier.getValue() != null && !identifier.getValue().trim().equals("")) {
                    obsoletes.add(identifier.getValue());
                    id = identifier.getValue();
                } 
            } 
        }
        return obsoletes;
    }
    

	public void itemRemoved(ItemEvent<SystemMetadata> entryEvent) {
		// do nothing - indexing acts on added objects, even if they need to be deleted
	}

	public void itemAdded(ItemEvent<SystemMetadata> entryEvent) {
	    //System.out.println("===================================calling entryUpdated method ");
	    log.info("===================================calling SystemMetadataEventListener.itemAdded method ");
		// add to the index
		Identifier pid = entryEvent.getItem().getIdentifier();
		//System.out.println("===================================update the document "+pid.getValue());
		log.info("===================================adding the document "+pid.getValue());
		SystemMetadata systemMetadata = entryEvent.getItem();
		if(systemMetadata == null) {
		    writeEventLog(systemMetadata, pid, "SystemMetadataEventListener.itemAdded -could not get the SystemMetadata");
		    return;
		}
		
		// make sure we remove this object so that it can be re-added in the future
		if (source != null) {
			source.remove(systemMetadata);
		}
				
		Identifier obsoletes = systemMetadata.getObsoletes();
		List<String> obsoletesChain = null;
		if (obsoletes != null) {
		    try {
				obsoletesChain = getObsoletes(pid.getValue());
			} catch (Exception e) {
			    String error = "SystemMetadataEventListener.itemAdded -could not look up revision history " + e.getMessage();
			    writeEventLog(systemMetadata, pid, error);
	            log.error(error, e);
	            return;
			}
		}
		String objectPath = null;
		try {
			objectPath = DistributedMapsFactory.getObjectPathMap().get(pid);
		} catch (Exception e) {
		    String error = "SystemMetadataEventListener.itemAdded - could not look up object path" + e.getMessage();
		    writeEventLog(systemMetadata, pid, error);
			log.error(error, e);
			return;
		}
		if(objectPath != null) {
		    InputStream data = null;
	        try {
	            data = new FileInputStream(objectPath);
	            solrIndex.update(pid.getValue(), obsoletesChain, systemMetadata, data);
                EventlogFactory.createIndexEventLog().remove(pid);
	        } catch (Exception e) {
	            String error = "SystemMetadataEventListener.itemAdded - could not comit the index into the solr server since " + e.getMessage();
	            writeEventLog(systemMetadata, pid, error);
	            log.error(error, e);
	        }
		}
		
	}
	
	private void writeEventLog(SystemMetadata systemMetadata, Identifier pid, String error) {
	    IndexEvent event = new IndexEvent();
        event.setIdentifier(pid);
	    event.setDate(Calendar.getInstance().getTime());
	    String action = null;
	    if (systemMetadata == null ) {
	        action = Event.CREATE.xmlValue();
            event.setAction(Event.CREATE);
	    }
	    else if(systemMetadata.getArchived() || systemMetadata.getObsoletedBy() != null) {
            action = Event.DELETE.xmlValue();
            event.setAction(Event.DELETE);
        } else {
            action = Event.CREATE.xmlValue();
            event.setAction(Event.CREATE);
        }
        event.setDescription("Failed to "+action+"the solr index for the id "+pid.getValue()+" since "+error);
        try {
            EventlogFactory.createIndexEventLog().write(event);
        } catch (Exception ee) {
            log.error("SolrIndex.insertToIndex - IndexEventLog can't log the index inserting event :"+ee.getMessage());
        }
	}
    
}
