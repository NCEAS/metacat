package edu.ucsb.nceas.metacat.dataone.hazelcast;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.dataone.exceptions.MarshallingException;
import org.dataone.service.types.v1.Identifier;
import org.xml.sax.SAXException;

import com.hazelcast.core.MapLoader;

import edu.ucsb.nceas.metacat.IdentifierManager;
import edu.ucsb.nceas.metacat.McdbDocNotFoundException;
import edu.ucsb.nceas.metacat.McdbException;
import edu.ucsb.nceas.metacat.accesscontrol.AccessControlException;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.metacat.replication.ReplicationService;
import edu.ucsb.nceas.metacat.shared.HandlerException;
import edu.ucsb.nceas.metacat.shared.ServiceException;
import edu.ucsb.nceas.utilities.FileUtil;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;


/**
 * MapLoader implementation for a hazelcast hzObjectPath.  This class is called
 * when the IMap get methods needs to refresh against the persistent data-store.
 * The use case for this class is to communicate the filepath between JVMs on
 * the same machine, specifically between the metacat instance and the d1_indexer.
 * 
 * d1_indexer will get Identifiers from elsewhere, but use this class to get
 * the paths to their associated files.  The getAllKeys() method will
 * return null in a live setting, to avoid possibly expensive preloading
 * 
 * @author rnahf
 */
public class ObjectPathMap implements MapLoader<Identifier, String> {
	private static IdentifierManager im;
	private static String dataPath;
	private static String metadataPath;
  private Logger logMetacat = Logger.getLogger(ObjectPathMap.class);

	
	/**
	 * creates an ObjectPathMap
	 */
	public ObjectPathMap() {
		try {
			PropertyService ps = PropertyService.getInstance();
			dataPath = PropertyService.getProperty("application.datafilepath");
			metadataPath = PropertyService.getProperty("application.documentfilepath");
		} catch (PropertyNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ServiceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		im = IdentifierManager.getInstance();
	}

	
	/*
	 * Metadata is stored in a different place on the filesystem than
	 * the data.  The doctype value for metadata can vary, but for data
	 * is always 'BIN', so using a simple if-then-else to separate
	 */
	private String pathToDocid(String localid) throws AccessControlException, HandlerException, MarshallingException, IOException, McdbException, SAXException  
	{	
		Hashtable<String, String> ht = ReplicationService.getDocumentInfoMap(localid);
		if (ht.get("doctype").equals("BIN")) {
			return dataPath + FileUtil.getFS() + localid;
		} else {
			return metadataPath + FileUtil.getFS() + localid;
		}		
	}

	
	/**
	 *  Implementation of hazelcast MapLoader interface method.
	 *  For the provided Identifier (as key), returns the path to the
	 *  document on the local filesystem.  Returns null if it can't 
	 *  create the path. 
	 */
	@Override
	public String load(Identifier key) 
	{

		String docid = null;
		String path = null;
		try {
			docid = im.getLocalId(key.getValue());
			path = pathToDocid(docid);			
		} catch (Exception e) {
			if (logMetacat.isDebugEnabled()) {
        e.printStackTrace();
    }
            return null;
		}
		return path;
	}
	
	
	/**
	 *  Implementation of hazelcast MapLoader interface method.  This method loads
	 *  mappings for all Identifiers in the parameters.  Any Identifier not found
	 *  is not included in the resulting map.
	 */
	@Override
	public Map<Identifier, String> loadAll(Collection<Identifier> identifiers) {
		
		
		Hashtable<Identifier,String> map = new Hashtable<Identifier,String>();
		for (Identifier id : identifiers) {
			try {
				String docid = im.getLocalId(id.getValue());
				map.put(id, pathToDocid(docid));

			} catch (Exception e) {
		      if (logMetacat.isDebugEnabled()) {
		          e.printStackTrace();
		      }
			}
		}
		return map;
	}

	
	/**
	 * Return the full set of guids in the local metacat repository as
	 * dataone Identifiers.
	 * 
	 * (Hazelcast allows avoiding pre-loading by returning NULL, so will
	 * do this to avoid pre-loading a very long list unnecessarily)
	 */
	@Override
	public Set<Identifier> loadAllKeys() 
	{
		return null;
		
//		List<String> guids = im.getAllGUIDs();
//
//		Set<Identifier> set = Collections.synchronizedSet(new HashSet<Identifier>());
//		for (String guid : guids) {
//			Identifier id = new Identifier();
//			id.setValue(guid);
//			set.add(id);
//		}
//		return set;
	}
}
