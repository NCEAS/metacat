package edu.ucsb.nceas.metacat.admin.upgrade.dataone;
/**
 *  '$RCSfile$'
 *    Purpose: A Class for upgrading the database to version 1.5
 *  Copyright: 2000 Regents of the University of California and the
 *             National Center for Ecological Analysis and Synthesis
 *    Authors: Saurabh Garg
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


import java.io.InputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dataone.client.D1Client;
import org.dataone.client.MNode;
import org.dataone.client.ObjectFormatCache;
import org.dataone.ore.ResourceMapFactory;
import org.dataone.service.types.v1.Checksum;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.ObjectFormatIdentifier;
import org.dataone.service.types.v1.ObjectInfo;
import org.dataone.service.types.v1.ObjectList;
import org.dataone.service.types.v1.SystemMetadata;
import org.dataone.service.types.v1.util.ChecksumUtil;
import org.dspace.foresite.ResourceMap;

import edu.ucsb.nceas.metacat.DBUtil;
import edu.ucsb.nceas.metacat.DocumentImpl;
import edu.ucsb.nceas.metacat.MetaCatServlet;
import edu.ucsb.nceas.metacat.admin.AdminException;
import edu.ucsb.nceas.metacat.admin.upgrade.UpgradeUtilityInterface;
import edu.ucsb.nceas.metacat.dataone.SystemMetadataFactory;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;
import edu.ucsb.nceas.utilities.SortedProperties;

public class GenerateORE implements UpgradeUtilityInterface {

	private static Log log = LogFactory.getLog(GenerateORE.class);
	
	private int serverLocation = 1;

    public boolean upgrade() throws AdminException {
        boolean success = true;
        
        // include ORE, data, for this server only
        boolean includeOre = true;
        boolean downloadData = false;
        try {
			downloadData = Boolean.parseBoolean(PropertyService.getProperty("dataone.ore.downloaddata"));
		} catch (PropertyNotFoundException e) {
			// ignore, default to false
			log.warn("Could not find ORE 'dataone.ore.downloaddata' property, defaulting to false", e);
		}


        try {
        	// get only local ids for this server
            List<String> idList = null;
            
            idList = DBUtil.getAllDocidsByType(DocumentImpl.EML2_0_0NAMESPACE, true, serverLocation);
            filterOutExisting(idList);
            Collections.sort(idList);
            SystemMetadataFactory.generateSystemMetadata(idList, includeOre, downloadData);
            
            idList = DBUtil.getAllDocidsByType(DocumentImpl.EML2_0_1NAMESPACE, true, serverLocation);
            filterOutExisting(idList);
            Collections.sort(idList);
            SystemMetadataFactory.generateSystemMetadata(idList, includeOre, downloadData);
            
            idList = DBUtil.getAllDocidsByType(DocumentImpl.EML2_1_0NAMESPACE, true, serverLocation);
            filterOutExisting(idList);
            Collections.sort(idList);
            SystemMetadataFactory.generateSystemMetadata(idList, includeOre, downloadData);
            
            idList = DBUtil.getAllDocidsByType(DocumentImpl.EML2_1_1NAMESPACE, true, serverLocation);
            filterOutExisting(idList);
            Collections.sort(idList);
            SystemMetadataFactory.generateSystemMetadata(idList, includeOre, downloadData);
            
		} catch (Exception e) {
			String msg = "Problem generating missing system metadata: " + e.getMessage();
			log.error(msg, e);
			success = false;
			throw new AdminException(msg);
		}
    	return success;
    }
    
    private List<String> filterOutExisting(List<String> idList) {
    	List<String> toRemove = new ArrayList<String>();
    	for (String id: idList) {
    		Identifier identifier = new Identifier();
    		identifier.setValue(id);
			boolean exists = SystemMetadataFactory.oreExistsFor(identifier);
			if (exists) {
				toRemove.add(id);
			}
    	}
    	for (String id: toRemove) {
    		idList.remove(id);
    	}
    	return idList;
    }
    
    public int getServerLocation() {
		return serverLocation;
	}

	public void setServerLocation(int serverLocation) {
		this.serverLocation = serverLocation;
	}
	
	/**
	 * Need to update the existing ORE maps to have correct dateTime serializations
	 * see: https://redmine.dataone.org/issues/3046
	 * @param mnBaseUrl
	 */
	public static void updateOREdateFormat(String mnBaseUrl) {
		
		List<Identifier> orePids = getAllOREpids(mnBaseUrl);
		updateOREs(orePids, "b", mnBaseUrl);
	}
	
	/**
	 * Retrieves a list of all ORE objects on the given MN
	 * @param mnBaseUrl
	 * @return
	 */
	public static List<Identifier> getAllOREpids(String mnBaseUrl) {
		
		MNode mn = null;
		ObjectFormatIdentifier formatId = null;
		List<Identifier> pids = null;
		try {
			
			// get the MN
			mn = D1Client.getMN(mnBaseUrl);
			
			// get the ORE format id
	        formatId = ObjectFormatCache.getInstance().getFormat("http://www.openarchives.org/ore/terms").getFormatId();
	        
	        // get the objects that match
			ObjectList objectList = mn.listObjects(null, null, null, formatId , null, 0, Integer.MAX_VALUE);
			pids = new ArrayList<Identifier>();
			for (ObjectInfo o: objectList.getObjectInfoList()) {
				pids.add(o.getIdentifier());
			}
			
		} catch (Exception e) {
			log.error("Could not get MN list of ORE pids", e);
		}
		
		return pids;
				
	}
	
	/**
	 * Updates the given OREs by regenerating and reserializing the RDF using the updated foresite library
	 * Only non-obsolete, non-archived ORE objects are updated and their SystemMetadata is based on the original version.
	 * see: https://redmine.dataone.org/issues/3046
	 * @param orePids
	 * @param pidSuffix
	 * @param mnBaseUrl
	 */
	public static void updateOREs(List<Identifier> orePids, String pidSuffix, String mnBaseUrl) {
		
		// get a MN client for this local node, or use the given baseUrl
		MNode mn = null;
		try {
			mn = D1Client.getMN(mnBaseUrl);
		} catch (Exception e) {
			log.error("Could not get MN client", e);
			// nothing more we can do here
			return;
		}
		
		// make sure we have something for the suffix
		if (pidSuffix == null) {
			pidSuffix = "b";
		}
		
		for (Identifier orePid: orePids) {
			try {
				
				log.debug("processing ORE pid: " + orePid.getValue());

				// get original SystemMetadata
				SystemMetadata originalOreSysMeta = mn.getSystemMetadata(orePid);
				
				// only update the CURRENT revision of the ORE
				if (originalOreSysMeta.getObsoletedBy() != null || (originalOreSysMeta.getArchived() != null && originalOreSysMeta.getArchived())) {
					log.debug("ORE pid is obsolete or archived, skipping: " + orePid.getValue());
					continue;
				}
				
				// get the original ORE map
				InputStream originalOreStream = mn.get(orePid);
				Map<Identifier, Map<Identifier, List<Identifier>>> originalOre = ResourceMapFactory.getInstance().parseResourceMap(originalOreStream);

				// generate the updated ORE map, in this case we aren't changing any values, just altering the serialization using a newer foresite library
				Identifier updatedOrePid = new Identifier();
				updatedOrePid.setValue(orePid.getValue() + pidSuffix);
				ResourceMap updatedOre = ResourceMapFactory.getInstance().createResourceMap(updatedOrePid , originalOre.entrySet().iterator().next().getValue());
				String resourceMapXML = ResourceMapFactory.getInstance().serializeResourceMap(updatedOre);
	            Checksum oreChecksum = ChecksumUtil.checksum(IOUtils.toInputStream(resourceMapXML, MetaCatServlet.DEFAULT_ENCODING), "MD5");
	            Date today = Calendar.getInstance().getTime();
	            
				// copy the existing SystemMetada into the new SystemMetadata
				SystemMetadata updatedOreSysMeta = new SystemMetadata();
				BeanUtils.copyProperties(updatedOreSysMeta, originalOreSysMeta);
				
				// set the new SystemMetadata values
				updatedOreSysMeta.setIdentifier(updatedOrePid);
				updatedOreSysMeta.setObsoletes(orePid);
				updatedOreSysMeta.setObsoletedBy(null);
				updatedOreSysMeta.setArchived(false);
	            updatedOreSysMeta.setChecksum(oreChecksum);
	            updatedOreSysMeta.setSize(BigInteger.valueOf(SystemMetadataFactory.sizeOfStream(IOUtils.toInputStream(resourceMapXML, MetaCatServlet.DEFAULT_ENCODING))));
	            updatedOreSysMeta.setDateSysMetadataModified(today);
	            updatedOreSysMeta.setDateUploaded(today);
	            updatedOreSysMeta.setReplicaList(null);

	            // save the updated ORE to the MN
				InputStream updatedOreStream = IOUtils.toInputStream(resourceMapXML, MetaCatServlet.DEFAULT_ENCODING);
				mn.update(null, orePid, updatedOreStream, updatedOrePid, updatedOreSysMeta);
				
				
			} catch (Exception e) {
				log.error("Could not update ORE map: " + orePid, e);
				
				// go to the next record, there's nothing else to do here
				continue;
			}
			
		}
	}

	public static void main(String [] ags){

        try {
        	// set up the properties based on the test/deployed configuration of the workspace
        	SortedProperties testProperties = 
				new SortedProperties("test/test.properties");
			testProperties.load();
			String metacatContextDir = testProperties.getProperty("metacat.contextDir");
			PropertyService.getInstance(metacatContextDir + "/WEB-INF");
			// now run it
            GenerateORE upgrader = new GenerateORE();
	        upgrader.upgrade();
	        
        } catch (Exception ex) {
            System.out.println("Exception:" + ex.getMessage());
            ex.printStackTrace();
        }
    }
}
