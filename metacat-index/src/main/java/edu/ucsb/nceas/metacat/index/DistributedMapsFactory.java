/**
 *  Copyright: 2013 Regents of the University of California and the
 *             National Center for Ecological Analysis and Synthesis
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dataone.configuration.Settings;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.SystemMetadata;

import com.hazelcast.client.ClientConfig;
import com.hazelcast.client.HazelcastClient;
import com.hazelcast.config.Config;
import com.hazelcast.config.FileSystemXmlConfig;
import com.hazelcast.core.IMap;
import com.hazelcast.core.ISet;

import edu.ucsb.nceas.metacat.common.index.event.IndexEvent;


/**
 * A factory to get distributed maps from the haszel cast client.
 * @author tao
 *
 */
public class DistributedMapsFactory {
    
    private static final String IDENTIFIERSETNAME = "hzIdentifiers";
    private static Log log = LogFactory.getLog(DistributedMapsFactory.class);
    
    private static HazelcastClient hzClient = null;
    private static String hzSystemMetadata = null;
    private static String hzObjectPath = null;
    private static String hzIndexQueue = null;
    private static int waitingTime = IndexGenerator.WAITTIME;
    private static int maxAttempts = IndexGenerator.MAXWAITNUMBER;
    private static IMap<Identifier, SystemMetadata> systemMetadataMap = null;
    private static IMap<Identifier, String> objectPathMap = null;
    private static ISet<SystemMetadata> indexQueue = null;
    /* The name of the identifiers set */
    private static String identifiersSetName = IDENTIFIERSETNAME;
    /* The Hazelcast distributed identifiers set */
    private static ISet<Identifier> identifiersSet = null;
    
    // for sending index events to metacat 
    private static String hzIndexEventMap = null;
    private static IMap<Identifier, IndexEvent> indexEventMap = null;
    
    /**
     * Start the hazel cast client
     */
    private static void startHazelCastClient() throws FileNotFoundException, ServiceFailure{
        
        try {
            waitingTime = Settings.getConfiguration().getInt(IndexGenerator.WAITIMEPOPERTYNAME);
            maxAttempts = Settings.getConfiguration().getInt(IndexGenerator.MAXATTEMPTSPROPERTYNAME);
        } catch (Exception e) {
            log.warn("DistributedMapFactory.startHazelCastClient - couldn't read the waiting time or maxattempts from the metacat.properties file since : "+e.getMessage()+". Default values will be used");
            waitingTime = IndexGenerator.WAITTIME;
            maxAttempts = IndexGenerator.MAXWAITNUMBER;
        }
        try {
            identifiersSetName = Settings.getConfiguration().getString("dataone.hazelcast.storageCluster.identifiersSet");
        } catch (Exception e) {
            log.warn("DistributedMapFactory.startHazelCastClient - couldn't read the name of the identifiersSet from the metacat.properties file since : "+e.getMessage()+". Default values will be used");
            identifiersSetName = IDENTIFIERSETNAME;
        }
        
        // the index queue name to listen to
        hzIndexQueue = Settings.getConfiguration().getString("index.hazelcast.indexqueue");        
        
        // the index event map name to send events to
        hzIndexEventMap = Settings.getConfiguration().getString("index.hazelcast.indexeventmap");
        
        // get config values
        hzSystemMetadata = Settings.getConfiguration().getString(
                "dataone.hazelcast.storageCluster.systemMetadataMap");
        hzObjectPath = Settings.getConfiguration().getString(
                "dataone.hazelcast.storageCluster.objectPathMap");
        String configFileName = Settings.getConfiguration().getString(
                "dataone.hazelcast.configFilePath");;
        Config hzConfig = null;
        try {
            hzConfig = new FileSystemXmlConfig(configFileName);
        } catch (FileNotFoundException e) {
            log.error("could not load hazelcast configuration file from: " + configFileName, e);
            throw e;
        }
        
        String hzGroupName = hzConfig.getGroupConfig().getName();
        String hzGroupPassword = hzConfig.getGroupConfig().getPassword();
        String hzAddress = hzConfig.getNetworkConfig().getInterfaces().getInterfaces().iterator().next() + ":" + hzConfig.getNetworkConfig().getPort();

        log.info("starting index entry listener...");
        log.info("System Metadata value: " + hzSystemMetadata);
        log.info("Object path value: " + hzObjectPath);
        log.info("Group Name: " + hzGroupName);
        log.info("Group Password: " + "*****"); // don't show value
        log.info("HZ Address: " + hzAddress);

        // connect to the HZ cluster
        ClientConfig cc = new ClientConfig();
        cc.getGroupConfig().setName(hzGroupName);
        cc.getGroupConfig().setPassword(hzGroupPassword);
        cc.addAddress(hzAddress);
        
        int times = 0;
        while(true) {
            //System.out.println("here ==================");
            try {
                hzClient = HazelcastClient.newHazelcastClient(cc);
                break;
            } catch (Exception e) {
                    if(times <= maxAttempts) {
                        log.warn("DistributedMapFactory.startHazelCastClient - the hazelcast service is not ready : "
                                         +e.getMessage()+"\nWe will try to access it "+waitingTime/1000+" seconds later ");
                        try {
                            Thread.sleep(waitingTime);
                        } catch (Exception ee) {
                            log.warn("DistributedMapFactory.startHazelCastClient - the thread can't sleep for "+waitingTime/1000+" seconds to wait the hazelcast service");
                        }
                       
                    } else {
                        throw new ServiceFailure("0000", "DistributedMapFactory.startHazelCastClient - the hazelcast service is not ready even though Metacat-index wailted for "+
                                        maxAttempts*waitingTime/1000+" seconds. We can't get the system metadata from it and the building index can't happen this time");
                    }
             }
             times++;
             //System.out.println("here ==================2");
        }
        

    }
    
    /**
     * Get the system metadata map
     * @return
     * @throws FileNotFoundException
     * @throws ServiceFailure
     */
    public static IMap<Identifier, SystemMetadata> getSystemMetadataMap() throws FileNotFoundException, ServiceFailure {
        if(hzClient == null) {
            startHazelCastClient();
        }
        systemMetadataMap = hzClient.getMap(hzSystemMetadata);
        return systemMetadataMap;
    }
    
    /**
     * Get the distributed object path map from the haszel cast client.
     * @return
     * @throws FileNotFoundException
     * @throws ServiceFailure
     */
    public static IMap<Identifier, String> getObjectPathMap() throws FileNotFoundException, ServiceFailure {
        if(hzClient == null) {
            startHazelCastClient();
        }
        objectPathMap = hzClient.getMap(hzObjectPath);
        return objectPathMap;
    }
    
    /**
     * Get the SystemMetadata for the specified id. The null will be returned if there is no SystemMetadata found for this id
     * @param id the specified id.
     * @return the SystemMetadata for the id
     * @throws FileNotFoundException
     * @throws ServiceFailure
     */
    public static SystemMetadata getSystemMetadata(String id) throws FileNotFoundException, ServiceFailure {
        if(systemMetadataMap == null) {
            getSystemMetadataMap();
        }
        SystemMetadata metadata = null;
        if(systemMetadataMap != null && id != null) {
            Identifier identifier = new Identifier();
            identifier.setValue(id);
            metadata = systemMetadataMap.get(identifier);
        }
        return metadata;
    }
    
    /**
     * Get the DataObject for the specified id. The null will be returned if not data object is found.
     * @param id the specified id
     * @return the InputStream of the data object for the specified id.
     * @throws FileNotFoundException
     * @throws ServiceFailure
     */
    public static InputStream getDataObject(String id) throws FileNotFoundException, ServiceFailure {
        if(objectPathMap == null) {
            getObjectPathMap();
        }
        InputStream data = null;
        if(objectPathMap != null && id != null) {
            Identifier identifier = new Identifier();
            identifier.setValue(id);
            String objectPath = objectPathMap.get(identifier);
            if(objectPath != null) {
                data = new FileInputStream(objectPath);
            }
        }
        return data;
    }
    
    /**
     * Get the identifiers set in the hazelcast client. The null may be returned if we can't find the set.
     * @return the identifiersSet
     * @throws FileNotFoundException
     * @throws ServiceFailure
     */
    public static ISet<Identifier> getIdentifiersSet() throws FileNotFoundException, ServiceFailure {
        if(hzClient== null) {
            startHazelCastClient();
        }
        identifiersSet= hzClient.getSet(identifiersSetName);
        return identifiersSet;
    }
    
    /**
     * Get the indexQueue set from hazelcast client
     * @return the indexQueue
     * @throws FileNotFoundException
     * @throws ServiceFailure
     */
    public static ISet<SystemMetadata> getIndexQueue() throws FileNotFoundException, ServiceFailure {
        if(hzClient== null) {
            startHazelCastClient();
        }
        indexQueue = hzClient.getSet(hzIndexQueue);
        return indexQueue;
    }
    
    /**
     * Get the index event map
     * @return the index event map for writing/reading events
     * @throws FileNotFoundException
     * @throws ServiceFailure
     */
    public static IMap<Identifier, IndexEvent> getIndexEventMap() throws FileNotFoundException, ServiceFailure {
        if(hzClient == null) {
            startHazelCastClient();
        }
        indexEventMap = hzClient.getMap(hzIndexEventMap);
        return indexEventMap;
    }
    
}
