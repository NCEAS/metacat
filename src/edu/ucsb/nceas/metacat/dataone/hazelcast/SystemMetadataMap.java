package edu.ucsb.nceas.metacat.dataone.hazelcast;

import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.dataone.service.exceptions.InvalidSystemMetadata;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v2.SystemMetadata;

import com.hazelcast.core.MapLoader;
import com.hazelcast.core.MapStore;

import edu.ucsb.nceas.metacat.IdentifierManager;
import edu.ucsb.nceas.metacat.McdbDocNotFoundException;

/**
 * Storage implementation for Hazelcast System Metadata
 * @author leinfelder
 *
 */
public class SystemMetadataMap 
    implements MapStore<Identifier, SystemMetadata>, MapLoader<Identifier, SystemMetadata> {

  private Logger logMetacat = Logger.getLogger(SystemMetadataMap.class);

	@Override
	public void delete(Identifier arg0) {
		if(arg0!= null) {
			logMetacat.debug("delete the identifier"+arg0.getValue());
			boolean success = IdentifierManager.getInstance().deleteSystemMetadata(arg0.getValue());
			if(!success) {
				throw new RuntimeException("SystemMetadataMap.delete - the system metadata of guid - "+arg0.getValue()+" can't be removed successfully.");
			}
		}
		
	}

	@Override
	public void deleteAll(Collection<Identifier> arg0) {
		// we do not delete system metadata	
	}

	@Override
	public void store(Identifier pid, SystemMetadata sm) {
		try {
			logMetacat.debug("Storing System Metadata to store: " + pid.getValue());
			IdentifierManager.getInstance().insertOrUpdateSystemMetadata(sm);
		} catch (McdbDocNotFoundException e) {
			throw new RuntimeException(e.getMessage(), e);
			
		} catch (SQLException e) {
	      throw new RuntimeException(e.getMessage(), e);
	      
    } catch (InvalidSystemMetadata e) {
        throw new RuntimeException(e.getMessage(), e);
        }
	}

	@Override
	public void storeAll(Map<Identifier, SystemMetadata> map) {
		for (Identifier key: map.keySet()) {
			store(key, map.get(key));
		}
	}

	@Override
	public SystemMetadata load(Identifier pid) {
		SystemMetadata sm = null;
    
    
    try {
			logMetacat.debug("loading from store: " + pid.getValue());
			sm = IdentifierManager.getInstance().getSystemMetadata(pid.getValue());
		} catch (McdbDocNotFoundException e) {
			//throw new RuntimeException(e.getMessage(), e);
			// not found => null
			logMetacat.warn("could not load system metadata for: " +  pid.getValue(), e);
			return null;
		}
		catch (Exception e) {
			throw new RuntimeException(e.getMessage(), e);
		}
		return sm;
	}

	@Override
	public Map<Identifier, SystemMetadata> loadAll(Collection<Identifier> keys) {
		Map<Identifier, SystemMetadata> map = new HashMap<Identifier, SystemMetadata>();
		for (Identifier key: keys) {
			SystemMetadata value = load(key);
			map.put(key, value);
		}
		return map;
	}

	/**
	 * Returning null so that no entries are loaded on map initialization
	 * 
	 * @see http://www.hazelcast.com/docs/1.9.4/manual/single_html/#Map
	 * 
	 * As of 1.9.3 MapLoader has the new MapLoader.loadAllKeys API. 
	 * It is used for pre-populating the in-memory map when the map is first touched/used. 
	 * If MapLoader.loadAllKeys returns NULL then nothing will be loaded. 
	 * Your MapLoader.loadAllKeys implementation can return all or some of the keys. 
	 * You may select and return only the hot keys, for instance. 
	 * Also note that this is the fastest way of pre-populating the map as 
	 * Hazelcast will optimize the loading process by having each node loading owned portion of the entries.
	 */
	@Override
	public Set<Identifier> loadAllKeys() {
		return null;
	}

}
