/**
 *  '$RCSfile$'
 *    Purpose: Implements a service for managing a Hazelcast cluster member
 *  Copyright: 2011 Regents of the University of California and the
 *             National Center for Ecological Analysis and Synthesis
 *    Authors: Christopher Jones
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

package edu.ucsb.nceas.metacat.dataone.hazelcast;

import java.io.FileNotFoundException;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;

import org.apache.log4j.Logger;
import org.dataone.service.exceptions.InvalidSystemMetadata;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.SystemMetadata;

import com.hazelcast.config.Config;
import com.hazelcast.config.FileSystemXmlConfig;
import com.hazelcast.core.EntryEvent;
import com.hazelcast.core.EntryListener;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ILock;
import com.hazelcast.core.IMap;
import com.hazelcast.core.ISet;
import com.hazelcast.core.ItemEvent;
import com.hazelcast.core.ItemListener;
import com.hazelcast.core.LifecycleEvent;
import com.hazelcast.core.LifecycleListener;
import com.hazelcast.core.Member;
import com.hazelcast.core.MembershipEvent;
import com.hazelcast.core.MembershipListener;
import com.hazelcast.partition.Partition;
import com.hazelcast.partition.PartitionService;

import edu.ucsb.nceas.metacat.IdentifierManager;
import edu.ucsb.nceas.metacat.McdbDocNotFoundException;
import edu.ucsb.nceas.metacat.common.index.IndexTask;
import edu.ucsb.nceas.metacat.common.index.event.IndexEvent;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.metacat.shared.BaseService;
import edu.ucsb.nceas.metacat.shared.ServiceException;
import edu.ucsb.nceas.metacat.util.DocumentUtil;
import edu.ucsb.nceas.utilities.FileUtil;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;
/**
 * The Hazelcast service enables Metacat as a Hazelcast cluster member
 */
public class HazelcastService extends BaseService
  implements EntryListener<Identifier, SystemMetadata>, MembershipListener, LifecycleListener, ItemListener<Identifier> {
  
  private static final String MISSING_PID_PREFIX = "missing-";

/* The instance of the logging class */
  private static Logger logMetacat = Logger.getLogger(HazelcastService.class);
  
  /* The singleton instance of the hazelcast service */
  private static HazelcastService hzService = null;
  
  /* The Hazelcast configuration */
  private Config hzConfig;
  
  /* The name of the system metadata map */
  private String systemMetadataMap;
  
  /* The Hazelcast distributed system metadata map */
  private IMap<Identifier, SystemMetadata> systemMetadata;
  
  /* The name of the identifiers set */
  private String identifiersSet;
  
  /* The Hazelcast distributed identifiers set */
  private ISet<Identifier> identifiers;
  
  /* The Hazelcast distributed missing identifiers set */
  private ISet<Identifier> missingIdentifiers;
  
  /* The Hazelcast distributed index queue */
  private String hzIndexQueue;
  private IMap<Identifier, IndexTask> indexQueue;
  
  /* The Hazelcast distributed index event map */
  private String hzIndexEventMap;
  private IMap<Identifier, IndexEvent> indexEventMap;

  private HazelcastInstance hzInstance;
      
  /*
   * Constructor: Creates an instance of the hazelcast service. Since
   * this uses a singleton pattern, use getInstance() to gain the instance.
   */
  private HazelcastService() {
    
    super();
    _serviceName="HazelcastService";
    
    try {
      init();
      
    } catch (ServiceException se) {
      logMetacat.error("There was a problem creating the HazelcastService. " +
                       "The error message was: " + se.getMessage());
      
    }
    
  }
  
  /**
   *  Get the instance of the HazelcastService that has been instantiated,
   *  or instantiate one if it has not been already.
   *
   * @return hazelcastService - The instance of the hazelcast service
   */
  public static HazelcastService getInstance(){
    
    if ( hzService == null ) {
      
      hzService = new HazelcastService();
      
    }
    return hzService;
  }
  
  /**
   * Initializes the Hazelcast service
   */
  public void init() throws ServiceException {
    
    logMetacat.debug("HazelcastService.init() called.");
    
	String configFileName = null;
	try {
		configFileName = PropertyService.getProperty("dataone.hazelcast.configFilePath");
		hzConfig = new FileSystemXmlConfig(configFileName);
	} catch (Exception e) {
		configFileName = PropertyService.CONFIG_FILE_DIR + FileUtil.getFS() + "hazelcast.xml";
		logMetacat.warn("Custom Hazelcast configuration not defined, using default: " + configFileName);
		// make sure we have the config
		try {
			hzConfig = new FileSystemXmlConfig(configFileName);
		} catch (FileNotFoundException e1) {
			String msg = e.getMessage();
			logMetacat.error(msg);
			throw new ServiceException(msg);
		}
	}

	this.hzInstance = Hazelcast.newHazelcastInstance(hzConfig);
  
  	logMetacat.debug("Initialized hzInstance");

    // Get configuration properties on instantiation
    try {
      systemMetadataMap = 
        PropertyService.getProperty("dataone.hazelcast.storageCluster.systemMetadataMap");
      identifiersSet = PropertyService.getProperty("dataone.hazelcast.storageCluster.identifiersSet");

      // Get a reference to the shared system metadata map as a cluster member
      // NOTE: this loads the map from the backing store and can take a long time for large collections
      systemMetadata = this.hzInstance.getMap(systemMetadataMap);
      
      logMetacat.debug("Initialized systemMetadata");

      // Get a reference to the shared identifiers set as a cluster member
      // NOTE: this takes a long time to complete
      logMetacat.warn("Retrieving hzIdentifiers from Hazelcast");
      identifiers = this.hzInstance.getSet(identifiersSet);
      logMetacat.warn("Retrieved hzIdentifiers from Hazelcast");
      
      // for publishing the "PIDs Wanted" list
      missingIdentifiers = this.hzInstance.getSet("hzMissingIdentifiersSet");
      
      missingIdentifiers.addItemListener(this, true);

      // for index tasks
      hzIndexQueue = PropertyService.getProperty("index.hazelcast.indexqueue");
      indexQueue = this.hzInstance.getMap(hzIndexQueue);

      // for index events (failures)
      hzIndexEventMap = PropertyService.getProperty("index.hazelcast.indexeventmap");
      indexEventMap = this.hzInstance.getMap(hzIndexEventMap);
      
      // Listen for changes to the system metadata map
      systemMetadata.addEntryListener(this, true);
      
      // Listen for members added/removed
      hzInstance.getCluster().addMembershipListener(this);
      
      // Listen for lifecycle state changes
      hzInstance.getLifecycleService().addLifecycleListener(this);
      
    } catch (PropertyNotFoundException e) {

      String msg = "Couldn't find Hazelcast properties for the DataONE clusters. " +
        "The error message was: " + e.getMessage();
      logMetacat.error(msg);
      
    }
    
    // make sure we have all metadata locally
    try {
    	// synch on restart
        resynchInThread();
	} catch (Exception e) {
		String msg = "Problem resynchronizing system metadata. " + e.getMessage();
		logMetacat.error(msg, e);
	}
        
  }
  
  /**
   * Get the system metadata map
   * 
   * @return systemMetadata - the hazelcast map of system metadata
   * @param identifier - the identifier of the object as a string
   */
  public IMap<Identifier,SystemMetadata> getSystemMetadataMap() {
	  return systemMetadata;
  }
  
  /**
   * Get the identifiers set
   * @return identifiers - the set of unique DataONE identifiers in the cluster
   */
  public ISet<Identifier> getIdentifiers() {
      return identifiers;
      
  }

  /**
   * Get the index queue
   * @return the set of SystemMetadata to be indexed
   */
  public IMap<Identifier, IndexTask> getIndexQueue() {
      return indexQueue;
  }
  
  /**
   * Get the index event map
   * @return indexEventMap - the hazelcast map of index events
   */
  public IMap<Identifier, IndexEvent> getIndexEventMap() {
	  return indexEventMap;
  }
  
  /**
   * When Metacat changes the underlying store, we need to refresh the
   * in-memory representation of it.
   * @param guid
   */
  public void refreshSystemMetadataEntry(String guid) {
	Identifier identifier = new Identifier();
	identifier.setValue(guid);
	// force hazelcast to update system metadata in memory from the store
	HazelcastService.getInstance().getSystemMetadataMap().evict(identifier);
  }

  public Lock getLock(String identifier) {
    
    Lock lock = null;
    
    try {
        lock = getInstance().getHazelcastInstance().getLock(identifier);
        
    } catch (RuntimeException e) {
        logMetacat.info("Couldn't get a lock for identifier " + 
            identifier + " !!");
    }
    return lock;
      
  }
  
  /**
   * Get the DataONE hazelcast node map
   * @return nodes - the hazelcast map of nodes
   */
//  public IMap<NodeReference, Node> getNodesMap() {
//	  return nodes;
//  }
  
  /**
   * Indicate whether or not this service is refreshable.
   *
   * @return refreshable - the boolean refreshable status
   */
  public boolean refreshable() {
    // TODO: Determine the consequences of restarting the Hazelcast instance
    // Set this to true if it's okay to drop from the cluster, lose the maps,
    // and start back up again
    return false;
    
  }
  
  /**
   * Stop the HazelcastService. When stopped, the service will no longer
   * respond to requests.
   */
  public void stop() throws ServiceException {
    
	  this.hzInstance.getLifecycleService().shutdown();
    
  }

  public HazelcastInstance getHazelcastInstance() {
      return this.hzInstance;
      
  }
  
  /**
   * Refresh the Hazelcast service by restarting it
   */
  @Override
  protected void doRefresh() throws ServiceException {

    // TODO: verify that the correct config file is still used
	  this.hzInstance.getLifecycleService().restart();
    
  }
  
  /**
	 * Implement the EntryListener interface for Hazelcast, reponding to entry
	 * added events in the hzSystemMetadata map. Evaluate the entry and create
	 * CNReplicationTasks as appropriate (for DATA, METADATA, RESOURCE)
	 * 
	 * @param event - The EntryEvent that occurred
	 */
	@Override
	public void entryAdded(EntryEvent<Identifier, SystemMetadata> event) {
	  
	  logMetacat.info("SystemMetadata entry added event on identifier " + 
	      event.getKey().getValue());
		// handle as update - that method will create if necessary
		entryUpdated(event);

	}

	/**
	 * Implement the EntryListener interface for Hazelcast, reponding to entry
	 * evicted events in the hzSystemMetadata map.  Evaluate the entry and create
	 * CNReplicationTasks as appropriate (for DATA, METADATA, RESOURCE)
	 * 
	 * @param event - The EntryEvent that occurred
	 */
	@Override
	public void entryEvicted(EntryEvent<Identifier, SystemMetadata> event) {

      logMetacat.info("SystemMetadata entry evicted event on identifier " + 
          event.getKey().getValue());
      
	    // ensure identifiers are listed in the hzIdentifiers set
      if ( !identifiers.contains(event.getKey()) ) {
          identifiers.add(event.getKey());
      }
	  
	}
	
	/**
	 * Implement the EntryListener interface for Hazelcast, reponding to entry
	 * removed events in the hzSystemMetadata map.  Evaluate the entry and create
	 * CNReplicationTasks as appropriate (for DATA, METADATA, RESOURCE)
	 * 
	 * @param event - The EntryEvent that occurred
	 */
	@Override
	public void entryRemoved(EntryEvent<Identifier, SystemMetadata> event) {
		
    logMetacat.info("SystemMetadata entry removed event on identifier " + 
        event.getKey().getValue());

	  // we typically don't remove objects in Metacat, but can remove System Metadata
		IdentifierManager.getInstance().deleteSystemMetadata(event.getValue().getIdentifier().getValue());

    // keep the hzIdentifiers set in sync with the systemmetadata table
    if ( identifiers.contains(event.getKey()) ) {
        identifiers.remove(event.getKey());
        
    }

	}
	
	/**
	 * Implement the EntryListener interface for Hazelcast, reponding to entry
	 * updated events in the hzSystemMetadata map.  Evaluate the entry and create
	 * CNReplicationTasks as appropriate (for DATA, METADATA, RESOURCE)
	 * 
	 * @param event - The EntryEvent that occurred
	 */
	@Override
	public void entryUpdated(EntryEvent<Identifier, SystemMetadata> event) {

		logMetacat.debug("Entry added/updated to System Metadata map: " + event.getKey().getValue());
		PartitionService partitionService = this.hzInstance.getPartitionService();
		Partition partition = partitionService.getPartition(event.getKey());
		Member ownerMember = partition.getOwner();
		SystemMetadata sysmeta = event.getValue();
		if (!ownerMember.localMember()) {
			if (sysmeta == null) {
				logMetacat.warn("No SystemMetadata provided in the event, getting from shared map: " + event.getKey().getValue());
				sysmeta = getSystemMetadataMap().get(event.getKey());
				if (sysmeta == null) {
					// this is a problem
					logMetacat.error("Could not find SystemMetadata in shared map for: " + event.getKey().getValue());
					// TODO: should probably return at this point since the save will fail
				}
			}
			// need to pull the entry into the local store
			saveLocally(event.getValue());
		}

		// ensure identifiers are listed in the hzIdentifiers set
		if (!identifiers.contains(event.getKey())) {
			identifiers.add(event.getKey());
		}

	}
	
	/**
	 * Save SystemMetadata to local store if needed
	 * @param sm
	 */
	private void saveLocally(SystemMetadata sm) {
		logMetacat.debug("Saving entry locally: " + sm.getIdentifier().getValue());
		try {

			IdentifierManager.getInstance().insertOrUpdateSystemMetadata(sm);

		} catch (McdbDocNotFoundException e) {
			logMetacat.error("Could not save System Metadata to local store.", e);
			
		} catch (SQLException e) {
	      logMetacat.error("Could not save System Metadata to local store.", e);
	      
	    } catch (InvalidSystemMetadata e) {
	        logMetacat.error("Could not save System Metadata to local store.", e);
	        
	    }
	}
	
	/**
	 * Checks the local backing store for missing SystemMetadata,
	 * retrieves those entries from the shared map if they exist,
	 * and saves them locally.
	 */
	private void synchronizeLocalStore() {
		List<String> localIds = IdentifierManager.getInstance().getLocalIdsWithNoSystemMetadata(true, -1);
		if (localIds != null) {
			logMetacat.debug("Member missing SystemMetadata entries, count = " + localIds.size());
			for (String localId: localIds) {
				logMetacat.debug("Processing system metadata for localId: " + localId);
				try {
					String docid = DocumentUtil.getSmartDocId(localId);
					int rev = DocumentUtil.getRevisionFromAccessionNumber(localId);
					String guid = IdentifierManager.getInstance().getGUID(docid, rev);
					logMetacat.debug("Found mapped guid: " + guid);
					Identifier pid = new Identifier();
					pid.setValue(guid);
					SystemMetadata sm = systemMetadata.get(pid);
					logMetacat.debug("Found shared system metadata for guid: " + guid);
					saveLocally(sm);
					logMetacat.debug("Saved shared system metadata locally for guid: " + guid);
				} catch (Exception e) {
					logMetacat.error("Could not save shared SystemMetadata entry locally, localId: " + localId, e);
				}
			}
		}
	}
	
	
	/**
	 * Make sure we have a copy of every entry in the shared map.
	 * We use lazy loading and therefore the CNs may not all be in sync when one
	 * comes back online after an extended period of being offline
	 * This method loops through the entries that a FULLY UP-TO-DATE CN has
	 * and makes sure each one is present on the shared map.
	 * It is meant to overcome a HZ weakness wherein ownership of a key results in 
	 * null values where the owner does not have a complete backing store.
	 * This will be an expensive routine and should be run in a background process so that
	 * the server can continue to service other requests during the synch
	 * @throws Exception
	 */
	private void resynchToRemote() {
		
		// the local identifiers not already present in the shared map
		Set<Identifier> localIdKeys = loadAllKeys();
		
		//  the PIDs missing locally
		Set<Identifier> missingIdKeys = new HashSet<Identifier>();
				
		// only contribute PIDs that are not already shared
		Iterator<Identifier> idIter = identifiers.iterator();
		int processedCount = 0;
		while (idIter.hasNext()) {
			Identifier pid = idIter.next();
			if (localIdKeys.contains(pid)) {
				logMetacat.debug("Shared pid is already in local identifier set: " + pid.getValue());
				localIdKeys.remove(pid);
			} else {
				// we don't have this locally, so we should try to get it
				missingIdKeys.add(pid);
			}
			processedCount++;
		}
		logMetacat.warn("processedCount (identifiers from iterator): " + processedCount);

		logMetacat.warn("local pid count not yet shared: " + localIdKeys.size() + ", shared pid count: " + identifiers.size());

		//identifiers.addAll(idKeys);
		logMetacat.warn("Loading missing local keys into hzIdentifiers");
		for (Identifier key: localIdKeys) {
			if (!identifiers.contains(key)) {
				logMetacat.debug("Adding missing hzIdentifiers key: " + key.getValue());
				identifiers.add(key);
			}
		}
		logMetacat.warn("Initialized identifiers with missing local keys");
		
		logMetacat.warn("Processing missing SystemMetadata for missing pid count: " + missingIdKeys.size());
		
		// loop through all the missing PIDs to find any null (missing) SM that needs to be resynched
		Iterator<Identifier> missingPids = missingIdKeys.iterator();
		while (missingPids.hasNext()) {
			Identifier pid = missingPids.next();
			// publish that we need this SM entry
			logMetacat.debug("Publishing missing pid to wanted list: " + pid.getValue());
			missingIdentifiers.add(pid);
		}
		
	}
	
	public void resynchInThread() {
		logMetacat.debug("launching system metadata resynch in a thread");
		ExecutorService executor = Executors.newSingleThreadExecutor();
		executor.execute(new Runnable() {
			@Override
			public void run() {
				try {
					// this is a push mechanism
					resynchToRemote();
				} catch (Exception e) {
					logMetacat.error("Error in resynchInThread: " + e.getMessage(), e);
				}
			}
		});
		executor.shutdown();
	}

	/**
	 * When there is missing SystemMetadata on the local member,
	 * we retrieve it from the shared map and add it to the local
	 * backing store for safe keeping.
	 */
	@Override
	public void memberAdded(MembershipEvent event) {
		Member member = event.getMember();
		logMetacat.debug("Member added to cluster: " + member.getInetSocketAddress());
		boolean isLocal = member.localMember();
		if (isLocal) {
			logMetacat.debug("Member islocal: " + member.getInetSocketAddress());
			synchronizeLocalStore();
		}
	}

	@Override
	public void memberRemoved(MembershipEvent event) {
		// TODO Auto-generated method stub
		
	}

	/**
	 * In cases where this cluster is paused, we want to 
	 * check that the local store accurately reflects the shared 
	 * SystemMetadata map
	 * @param event
	 */
	@Override
	public void stateChanged(LifecycleEvent event) {
		logMetacat.debug("HZ LifecycleEvent.state: " + event.getState());
		if (event.getState().equals(LifecycleEvent.LifecycleState.RESUMED)) {
			logMetacat.debug("HZ LifecycleEvent.state is RESUMED, calling synchronizeLocalStore()");
			synchronizeLocalStore();
		}
	}

	/**
	 * Load all System Metadata keys from the backing store
	 * @return set of pids
	 */
	private Set<Identifier> loadAllKeys() {

		Set<Identifier> pids = new HashSet<Identifier>();
		
		try {
			
			// ALTERNATIVE 1: this has more overhead than just looking at the GUIDs
//			ObjectList ol = IdentifierManager.getInstance().querySystemMetadata(
//					null, //startTime, 
//					null, //endTime, 
//					null, //objectFormatId, 
//					false, //replicaStatus, 
//					0, //start, 
//					-1 //count
//					);
//			for (ObjectInfo o: ol.getObjectInfoList()) {
//				Identifier pid = o.getIdentifier();
//				if ( !pids.contains(pid) ) {
//					pids.add(pid);
//				}				
//			}
			
			// ALTERNATIVE method: look up all the Identifiers from the table
			List<String> guids = IdentifierManager.getInstance().getAllSystemMetadataGUIDs();
			logMetacat.warn("Local SystemMetadata pid count: " + guids.size());
			for (String guid: guids){
				Identifier pid = new Identifier();
				pid.setValue(guid);
				pids.add(pid);
			}
			
		} catch (Exception e) {
			throw new RuntimeException(e.getMessage(), e);
			
		}
		
		return pids;
	}

	/**
	 * Respond to itemAdded events on the hzMissingIdentifiers Set.  Uses a
	 * distributed ILock to try to prevent multiple put calls on hzSystemMetadata
	 * 
	 * @param pid   the identifier of the event
	 */
	@Override
	public void itemAdded(ItemEvent<Identifier> event) {
		
		Identifier pid = (Identifier) event.getItem();
		// publish the SM for the pid if we have it locally
		logMetacat.debug("Responding to itemAdded for pid: " + pid.getValue());
		
		// lock this event, only if we have a local copy to contribute
		ILock lock = null;
		try {
			// look up the local copy of the SM
			SystemMetadata sm = IdentifierManager.getInstance().getSystemMetadata(pid.getValue());
			if (sm != null) {
				lock = hzInstance.getLock(MISSING_PID_PREFIX + pid.getValue());
				
				if ( lock.tryLock() ) {
			        // "publish" the system metadata to the shared map since it showed up on the missing queue
			        logMetacat.debug("Adding SystemMetadata to shared map for pid: " + pid.getValue());
			        systemMetadata.put(pid, sm);
			        
			        // remove the entry since we processed it
			        missingIdentifiers.remove(pid);
			      
				  } else {
				      logMetacat.debug(MISSING_PID_PREFIX + pid.getValue() + " was already locked. Skipping.");
				  }
			} else {
				// can't help here
				logMetacat.warn("Local system metadata not found for pid: " + pid.getValue());
			}
		} catch (Exception e) {
			logMetacat.error("Error looking up missing system metadata for pid: " + pid.getValue());
		} finally {
			if ( lock != null ) {
				lock.unlock();
			}
        }
	}

	/**
   * Respond to itemRemoved events on the hzMissingIdentifiers Set
   * 
   * @param pid   the identifier of the event
   */
	@Override
	public void itemRemoved(ItemEvent<Identifier> event) {
		// do nothing since someone probably handled the wanted PID
		
	}

}