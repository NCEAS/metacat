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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.TimerTask;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.solr.client.solrj.SolrServerException;
import org.dataone.configuration.Settings;
import org.dataone.service.exceptions.InvalidRequest;
import org.dataone.service.exceptions.InvalidToken;
import org.dataone.service.exceptions.NotAuthorized;
import org.dataone.service.exceptions.NotFound;
import org.dataone.service.exceptions.NotImplemented;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.exceptions.UnsupportedType;
import org.dataone.service.types.v1.Event;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.ObjectFormatIdentifier;
import org.dataone.service.types.v1.SystemMetadata;
import org.dspace.foresite.OREParserException;
import org.xml.sax.SAXException;

import com.hazelcast.core.IMap;
import com.hazelcast.core.ISet;

import edu.ucsb.nceas.metacat.common.SolrServerFactory;
import edu.ucsb.nceas.metacat.common.index.event.IndexEvent;
import edu.ucsb.nceas.metacat.index.event.EventlogFactory;
import edu.ucsb.nceas.metacat.index.event.IndexEventLogException;


/**
 * A class represents the object to generate massive solr indexes.
 * This can happen during an update of Metacat (generating index for all existing documents)
 * or regenerate index for those documents
 * failing to build index during the insert or update.
 * 
 * @author tao
 *
 */
public class IndexGeneratorTimerTask extends TimerTask {
    
    private static final int FIRST =0;
    private static final int SECOND =1;
    private static final int THIRD = 2;
    private static final int FOURTH = 3;
    public static final int WAITTIME = 10000;
    public static final int MAXWAITNUMBER = 180;
    private static final String HTTP = "http://";
    private static final String MNAPPENDIX = "/d1/mn";
    private static final String RESOURCEMAPPROPERYNAME = "index.resourcemap.namespace";
    public static final String WAITIMEPOPERTYNAME = "index.regenerate.start.waitingtime";
    public static final String MAXATTEMPTSPROPERTYNAME = "index.regenerate.start.maxattempts";
    
    
    private SolrIndex solrIndex = null;
    //private SystemMetadataEventListener systemMetadataListener = null;
    private IMap<Identifier, SystemMetadata> systemMetadataMap;
    private IMap<Identifier, String> objectPathMap;
    private ISet<SystemMetadata> indexQueue;
    private Log log = LogFactory.getLog(IndexGeneratorTimerTask.class);
    //private MNode mNode = null;
    private static List<String> resourceMapNamespaces = null;
    
    /**
     * Constructor
     * @param solrIndex
     * @param systemMetadataListener
     */
    public IndexGeneratorTimerTask(SolrIndex solrIndex) {
        this.solrIndex = solrIndex;
        resourceMapNamespaces = Settings.getConfiguration().getList(RESOURCEMAPPROPERYNAME);
        //this.systemMetadataListener = systemMetadataListener;
        //this.mNode = new MNode(buildMNBaseURL());
      
    }
    
   
    
    /**
     * Build the index for all documents.
     * @throws SolrServerException 
     * @throws ServiceFailure 
     * @throws NotImplemented 
     * @throws NotAuthorized 
     * @throws InvalidToken 
     * @throws InvalidRequest 
     * @throws IndexEventLogException 
     * @throws IllegalAccessException 
     * @throws InstantiationException 
     * @throws ClassNotFoundException 
     * @throws ParserConfigurationException 
     * @throws SAXException 
     * @throws IOException 
     * @throws UnsupportedType 
     * @throws NotFound 
     * @throws XPathExpressionException 
     * @throws OREParserException 
     */
    public void indexAll() throws InvalidRequest, InvalidToken,
                NotAuthorized, NotImplemented, ServiceFailure, SolrServerException, ClassNotFoundException, InstantiationException, IllegalAccessException, IndexEventLogException, XPathExpressionException, NotFound, UnsupportedType, IOException, SAXException, ParserConfigurationException, OREParserException {
        Date since = null;
        Date until = null;
        index(since, until);
    }
    
    /**
     * Build the index for the docs which have been modified since the specified date.
     * @param since
     * @throws SolrServerException 
     * @throws ServiceFailure 
     * @throws NotImplemented 
     * @throws NotAuthorized 
     * @throws InvalidToken 
     * @throws InvalidRequest 
     * @throws IndexEventLogException 
     * @throws IllegalAccessException 
     * @throws InstantiationException 
     * @throws ClassNotFoundException 
     * @throws ParserConfigurationException 
     * @throws SAXException 
     * @throws IOException 
     * @throws UnsupportedType 
     * @throws NotFound 
     * @throws XPathExpressionException 
     * @throws OREParserException 
     */
    public void index(Date since) throws InvalidRequest, InvalidToken, 
                    NotAuthorized, NotImplemented, ServiceFailure, SolrServerException, ClassNotFoundException, InstantiationException, IllegalAccessException, IndexEventLogException, XPathExpressionException, NotFound, UnsupportedType, IOException, SAXException, ParserConfigurationException, OREParserException {
        Date until = null;
        index(since, until);
    }
    
    /**
     *  Build the index for the docs which have been modified between the specified date.s
     * @param since
     * @param until
     * @throws SolrServerException 
     * @throws ServiceFailure 
     * @throws NotImplemented 
     * @throws NotAuthorized 
     * @throws InvalidToken 
     * @throws InvalidRequest 
     * @throws IndexEventLogException 
     * @throws IllegalAccessException 
     * @throws InstantiationException 
     * @throws ClassNotFoundException 
     * @throws ParserConfigurationException 
     * @throws SAXException 
     * @throws IOException 
     * @throws UnsupportedType 
     * @throws NotFound 
     * @throws XPathExpressionException 
     * @throws OREParserException 
     */
    public void index(Date since, Date until) throws SolrServerException, InvalidRequest, 
                                                InvalidToken, NotAuthorized, NotImplemented, ServiceFailure, ClassNotFoundException, InstantiationException, IllegalAccessException, IndexEventLogException, XPathExpressionException, NotFound, UnsupportedType, IOException, SAXException, ParserConfigurationException, OREParserException {
        Date processedDate = null;
        List<String> solrIds = null;
        initSystemMetadataMap();
        initObjectPathMap();
        initIndexQueue();
        List[] metacatIds = getMetacatIds(since, until);
        List<String> otherMetacatIds = metacatIds[FIRST];
        List<String> resourceMapIds =  metacatIds[SECOND];
        //List<String> otherDeletedMetacatIds = metacatIds[THIRD];
        //List<String> resourceMapDeletedIds = metacatIds[FOURTH];
        
        //figure out the procesedDate by comparing the last element of otherMetacatIds and resourceMapIds.
        List<Long> maxCollection = new ArrayList<Long>();
        Date latestOtherId = null;
        if (otherMetacatIds != null && !otherMetacatIds.isEmpty()) {
            int size = otherMetacatIds.size();
            String id = otherMetacatIds.get(size-1);
            SystemMetadata sysmeta = getSystemMetadata(id);
            latestOtherId = sysmeta.getDateSysMetadataModified();
            maxCollection.add(new Long(latestOtherId.getTime()));
        }
        
        /*Date latestDeletedOtherIds = null;
        if (otherDeletedMetacatIds != null && !otherDeletedMetacatIds.isEmpty()) {
            int size = otherDeletedMetacatIds.size();
            String id = otherDeletedMetacatIds.get(size-1);
            SystemMetadata sysmeta = getSystemMetadata(id);
            latestDeletedOtherIds = sysmeta.getDateSysMetadataModified();
            maxCollection.add(new Long(latestDeletedOtherIds.getTime()));
        }*/
        
        Date latestResourceId = null;
        if (resourceMapIds != null && !resourceMapIds.isEmpty()) {
            int size = resourceMapIds.size();
            String id = resourceMapIds.get(size-1);
            SystemMetadata sysmeta = getSystemMetadata(id);
            latestResourceId = sysmeta.getDateSysMetadataModified();
            maxCollection.add(new Long(latestResourceId.getTime()));
        }
        
        /*Date latestDeletedResourceId = null;
        if(resourceMapDeletedIds != null && !resourceMapDeletedIds.isEmpty()) {
            int size = resourceMapDeletedIds.size();
            String id = resourceMapDeletedIds.get(size-1);
            SystemMetadata sysmeta = getSystemMetadata(id);
            latestDeletedResourceId = sysmeta.getDateSysMetadataModified();
            maxCollection.add(new Long(latestDeletedResourceId.getTime()));
        }*/
        
        if(!maxCollection.isEmpty()) {
            Long max = Collections.max(maxCollection);
            processedDate = new Date(max.longValue());
        }
        /*if(latestOtherId != null && latestResourceId != null && latestOtherId.getTime() > latestResourceId.getTime()) {
            processedDate = latestOtherId;
        } else if (latestOtherId != null && latestResourceId != null && latestOtherId.getTime()  <= latestResourceId.getTime()) {
            processedDate = latestResourceId;
        } else if (latestOtherId == null && latestResourceId != null) {
            processedDate = latestResourceId;
        } else if (latestOtherId != null && latestResourceId == null) {
            processedDate = latestOtherId;
        }*/
        
        
        //add the failedPids 
        List<IndexEvent> failedEvents = EventlogFactory.createIndexEventLog().getEvents(null, null, null, null);
        List<String> failedOtherIds = new ArrayList<String>();
        List<String> failedResourceMapIds = new ArrayList<String>();
        if(failedEvents != null) {
            for(IndexEvent event : failedEvents) {
            	String id = event.getIdentifier().getValue();
                SystemMetadata sysmeta = getSystemMetadata(id);
                if(sysmeta != null) {
                    ObjectFormatIdentifier formatId =sysmeta.getFormatId();
                    if(formatId != null && formatId.getValue() != null && resourceMapNamespaces != null && isResourceMap(formatId)) {
                        failedResourceMapIds.add(id);
                    } else {
                        failedOtherIds.add(id);
                    }
                }
            }
        }
        //indexFailedIds(failedOtherIds);
        //indexFailedIds(failedResourceMapIds);
        
        index(failedOtherIds);
        index(failedResourceMapIds);
        
        /*if(!failedOtherIds.isEmpty()) {
            failedOtherIds.addAll(otherMetacatIds);
        } else {
            failedOtherIds = otherMetacatIds;
        }
        
        if(!failedResourceMapIds.isEmpty()) {
            failedResourceMapIds.addAll(resourceMapIds);
        } else {
            failedResourceMapIds = resourceMapIds;
        }*/
        //log.info("the ids in index_event for reindex ( except the resourcemap)=====================================\n "+failedOtherIds);
        //log.info("the resourcemap ids in index_event for reindex =====================================\n "+failedResourceMapIds);
        log.info("the metacat ids (except the resource map ids)-----------------------------"+otherMetacatIds);
        //logFile(otherMetacatIds, "ids-for-timed-indexing-log");
        //log.info("the deleted metacat ids (except the resource map ids)-----------------------------"+otherDeletedMetacatIds);
        log.info("the metacat resroucemap ids -----------------------------"+resourceMapIds);
        //logFile(resourceMapIds, "ids-for-timed-indexing-log");
        //log.info("the deleted metacat resroucemap ids -----------------------------"+resourceMapDeletedIds);
        index(otherMetacatIds);
        //removeIndex(otherDeletedMetacatIds);
        index(resourceMapIds);
        //removeIndex(resourceMapDeletedIds);
       
        //record the timed index.
        if(processedDate != null) {
            EventlogFactory.createIndexEventLog().setLastProcessDate(processedDate);
        }
        
    }
    
    /*
     * Write the docids which will be indexed into a file. 
     */
    /*private void logFile(List<String> ids, String fileName)  {
        if(ids != null) {
            try {
                String tempDir = System.getProperty("java.io.tmpdir");
                log.info("the temp dir is ===================== "+tempDir);
                File idsForIndex = new File(tempDir, fileName);
                if(!idsForIndex.exists()) {
                    idsForIndex.createNewFile();
                } 
                
                Date date = Calendar.getInstance().getTime();
                SimpleDateFormat format = new SimpleDateFormat("yyyy.MM.dd G 'at' HH:mm:ss z");
                String dateStr = format.format(date);
                List<String> dateList = new ArrayList<String>();
                dateList.add(dateStr);
                Boolean append = true;
                FileUtils.writeLines(idsForIndex, dateList, append);//write time string
                FileUtils.writeLines(idsForIndex, ids, append);
            } catch (Exception e) {
                log.warn("IndexGenerator.logFile - Couldn't log the ids which will be indexed since - "+e.getMessage());
            }
           
        }
    }*/
    /*
     * Doing index
     */
    private void index(List<String> metacatIds) {
        if(metacatIds != null) {
            for(String metacatId : metacatIds) {
                if(metacatId != null) {
                     generateIndex(metacatId);
                }
            }
        }
    }
    
    /*
     * Index those ids which failed in the process (We got them from the EventLog)
     */
    /*private void indexFailedIds(List<IndexEvent> events) {
        if(events != null) {
            for(IndexEvent event : events) {
                if(event != null) {
                    Identifier identifier = event.getIdentifier();
                    if(identifier != null) {
                        String id = identifier.getValue();
                        if(id != null) {
                            Event action = event.getAction();
                            //if (action != null && action.equals(Event.CREATE)) {
                                try {
                                    generateIndex(id);
                                    EventlogFactory.createIndexEventLog().remove(identifier);
                                } catch (Exception e) {
                                    log.error("IndexGenerator.indexFailedIds - Metacat Index couldn't generate the index for the id - "+id+" because "+e.getMessage());
                                }
                            
                        }
                    }
                }
            }
        }
    }*/
    
    public void run() {
    
        try {
            Date since = EventlogFactory.createIndexEventLog().getLastProcessDate();
            index(since);
        } catch (InvalidRequest e) {
            // TODO Auto-generated catch block
            //e.printStackTrace();
            log.error("IndexGenerator.run - Metadata-Index couldn't generate indexes for those documents which haven't been indexed : "+e.getMessage());
        } catch (InvalidToken e) {
            // TODO Auto-generated catch block
            //e.printStackTrace();
            log.error("IndexGenerator.run - Metadata-Index couldn't generate indexes for those documents which haven't been indexed : "+e.getMessage());
        } catch (NotAuthorized e) {
            // TODO Auto-generated catch block
            //e.printStackTrace();
        } catch (NotImplemented e) {
            // TODO Auto-generated catch block
            //e.printStackTrace();
            log.error("IndexGenerator.run - Metadata-Index couldn't generate indexes for those documents which haven't been indexed : "+e.getMessage());
        } catch (ServiceFailure e) {
            // TODO Auto-generated catch block
            //e.printStackTrace();
            log.error("IndexGenerator.run - Metadata-Index couldn't generate indexes for those documents which haven't been indexed : "+e.getMessage());
        } catch (SolrServerException e) {
            // TODO Auto-generated catch block
            //e.printStackTrace();
            log.error("IndexGenerator.run - Metadata-Index couldn't generate indexes for those documents which haven't been indexed : "+e.getMessage());
        } catch (FileNotFoundException e) {
            log.error("IndexGenerator.run - Metadata-Index couldn't generate indexes for those documents which haven't been indexed : "+e.getMessage());
        } catch (ClassNotFoundException e) {
            // TODO Auto-generated catch block
            log.error("IndexGenerator.run - Metadata-Index couldn't generate indexes for those documents which haven't been indexed : "+e.getMessage());
        } catch (InstantiationException e) {
            // TODO Auto-generated catch block
            log.error("IndexGenerator.run - Metadata-Index couldn't generate indexes for those documents which haven't been indexed : "+e.getMessage());
        } catch (IllegalAccessException e) {
            // TODO Auto-generated catch block
            log.error("IndexGenerator.run - Metadata-Index couldn't generate indexes for those documents which haven't been indexed : "+e.getMessage());
        } catch (IndexEventLogException e) {
            // TODO Auto-generated catch block
            log.error("IndexGenerator.run - Metadata-Index couldn't generate indexes for those documents which haven't been indexed : "+e.getMessage());
        } catch (XPathExpressionException e) {
            // TODO Auto-generated catch block
            log.error("IndexGenerator.run - Metadata-Index couldn't generate indexes for those documents which haven't been indexed : "+e.getMessage());
        } catch (NotFound e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (UnsupportedType e) {
            // TODO Auto-generated catch block
            log.error("IndexGenerator.run - Metadata-Index couldn't generate indexes for those documents which haven't been indexed : "+e.getMessage());
        } catch (IOException e) {
            // TODO Auto-generated catch block
            log.error("IndexGenerator.run - Metadata-Index couldn't generate indexes for those documents which haven't been indexed : "+e.getMessage());
        } catch (SAXException e) {
            // TODO Auto-generated catch block
            log.error("IndexGenerator.run - Metadata-Index couldn't generate indexes for those documents which haven't been indexed : "+e.getMessage());
        } catch (ParserConfigurationException e) {
            // TODO Auto-generated catch block
            log.error("IndexGenerator.run - Metadata-Index couldn't generate indexes for those documents which haven't been indexed : "+e.getMessage());
        } catch (OREParserException e) {
            // TODO Auto-generated catch block
            log.error("IndexGenerator.run - Metadata-Index couldn't generate indexes for those documents which haven't been indexed : "+e.getMessage());
        }
    }
    
   
    
    /*
     * Get an array of the list of ids of the metacat which has the systemmetadata modification in the range.
     * 
     * If since and util are null, it will return all of them.
     * The first element of the list is the ids except the resource map. The second elements of the list is the ids of the resource map.
     * The reason to split them is when we index the resource map, we need the index of the documents in the resource map ready.
     * The last element in the each list has the latest SystemMetadata modification date. But they are not sorted.
     */
    private List[] getMetacatIds(Date since, Date until) throws InvalidRequest, 
                        InvalidToken, NotAuthorized, NotImplemented, ServiceFailure, FileNotFoundException {
        String fileName = "ids-from-hazelcast";
        List<String> resourceMapIds = new ArrayList();
        //List<String> resourceMapDeletedIds = new ArrayList();
        List<String> otherIds = new ArrayList();
        //List<String> otherDeletedIds = new ArrayList();
        List[] ids = new List[2];
        ids[FIRST]= otherIds;
        ids[SECOND] = resourceMapIds;
        //ids[THIRD]  = otherDeletedIds;
        //ids[FOURTH] = resourceMapDeletedIds;
        ISet<Identifier> metacatIds = DistributedMapsFactory.getIdentifiersSet();
        Date otherPreviousDate = null;
        Date otherDeletedPreviousDate = null;
        Date resourceMapPreviousDate = null;
        Date resourceMapDeletedPreviousDate = null;
        if(metacatIds != null) {
            for(Identifier identifier : metacatIds) {
                if(identifier != null && identifier.getValue() != null && !identifier.getValue().equals("")) {
                    List<String> idLog = new ArrayList<String>();
                    idLog.add(identifier.getValue());
                    //logFile(idLog, fileName);
                    SystemMetadata sysmeta = getSystemMetadata(identifier.getValue());
                    if(sysmeta != null) {
                        ObjectFormatIdentifier formatId =sysmeta.getFormatId();
                        //System.out.println("the object format id is "+formatId.getValue());
                        //System.out.println("the ============ resourcMapNamespaces"+resourceMapNamespaces);
                        boolean correctTimeRange = false;
                        Date sysDate = sysmeta.getDateSysMetadataModified();
                        if(since == null && until == null) {
                            correctTimeRange = true;
                        } else if (since != null && until == null) {
                            if(sysDate.getTime() > since.getTime()) {
                                correctTimeRange = true;
                            }
                        } else if (since == null && until != null) {
                            if(sysDate.getTime() < until.getTime()) {
                                correctTimeRange = true;
                            }
                        } else if (since != null && until != null) {
                            if(sysDate.getTime() > since.getTime() && sysDate.getTime() < until.getTime()) {
                                correctTimeRange = true;
                            }
                        }
                        if(correctTimeRange && formatId != null && formatId.getValue() != null && resourceMapNamespaces != null && isResourceMap(formatId)) {
                            //for the resource map
                            /*if(sysmeta.getArchived() || sysmeta.getObsoletedBy() != null) {
                                //archived ids
                                if(!resourceMapDeletedIds.isEmpty()) {
                                    if(sysDate.getTime() > resourceMapDeletedPreviousDate.getTime()) {
                                        resourceMapDeletedIds.add(identifier.getValue());//append to the end of the list if current is later than the previous one
                                        resourceMapDeletedPreviousDate = sysDate;//reset resourceMapPreviousDate to the bigger one
                                    } else {
                                        int size = resourceMapDeletedIds.size();//
                                        resourceMapDeletedIds.add(size -1, identifier.getValue());//keep the previous one at the end of the list.
                                    }
                                } else {
                                    resourceMapDeletedIds.add(identifier.getValue());
                                    resourceMapDeletedPreviousDate = sysDate;//init resourcemapPreviousDate
                                }
                            } else {*/
                                // for all ids
                                if(!resourceMapIds.isEmpty()) {
                                    if(sysDate.getTime() > resourceMapPreviousDate.getTime()) {
                                        resourceMapIds.add(identifier.getValue());//append to the end of the list if current is later than the previous one
                                        resourceMapPreviousDate = sysDate;//reset resourceMapPreviousDate to the bigger one
                                    } else {
                                        int size = resourceMapIds.size();//
                                        resourceMapIds.add(size -1, identifier.getValue());//keep the previous one at the end of the list.
                                    }
                                } else {
                                    resourceMapIds.add(identifier.getValue());
                                    resourceMapPreviousDate = sysDate;//init resourcemapPreviousDate
                                }
                            //}
                        } else if (correctTimeRange) {
                            /*if(sysmeta.getArchived() || sysmeta.getObsoletedBy() != null) {
                                //for the archived ids
                                if(!otherDeletedIds.isEmpty()) {
                                    if(sysDate.getTime() > otherDeletedPreviousDate.getTime()) {
                                        otherDeletedIds.add(identifier.getValue());
                                        otherDeletedPreviousDate = sysDate;//reset otherDeletedPreviousDate to the bigger one
                                    } else {
                                        int size = otherDeletedIds.size();
                                        otherDeletedIds.add(size-1, identifier.getValue());
                                    }
                                } else {
                                    otherDeletedIds.add(identifier.getValue());
                                    otherDeletedPreviousDate = sysDate;//init otherDeletedPreviousDate
                                }
                            } else {*/
                                //for all ids
                                if(!otherIds.isEmpty()) {
                                    if(sysDate.getTime() > otherPreviousDate.getTime()) {
                                        otherIds.add(identifier.getValue());
                                        otherPreviousDate = sysDate;//reset otherPreviousDate to the bigger one
                                    } else {
                                        int size = otherIds.size();
                                        otherIds.add(size-1, identifier.getValue());
                                    }
                                } else {
                                    otherIds.add(identifier.getValue());
                                    otherPreviousDate = sysDate;//init otherPreviousDate
                                }
                            //}
                        }
                        
                    }
                }
            }
        }
        return ids;
    }
    
    /*
     * If the specified ObjectFormatIdentifier is a resrouce map namespace.
     */
    public static boolean isResourceMap(ObjectFormatIdentifier formatId) {
        boolean isResourceMap = false;
        if(formatId != null && resourceMapNamespaces != null) {
            for(String namespace : resourceMapNamespaces) {
                if(namespace != null && formatId.getValue() != null && !formatId.getValue().trim().equals("") && formatId.getValue().equals(namespace)) {
                    isResourceMap = true;
                    break;
                }
            }
        }
        return isResourceMap;
    }
    
   
    
    /*
     * Generate index for the id.
     */
    private void generateIndex(String id)  {
        //if id is null and sysmeta will be null. If sysmeta is null, it will be caught in solrIndex.update
        SystemMetadata sysmeta = getSystemMetadata(id);
        Identifier pid = new Identifier();
        pid.setValue(id);
        solrIndex.update(pid, sysmeta);
 
    }
    
    /*
     * Remove the solr index for the list of ids
     */
    /*private void removeIndex(List<String> ids) {
        if(ids!= null) {
            for(String id :ids) {
                try {
                    removeIndex(id);
                } catch (Exception e) {
                    IndexEvent event = new IndexEvent();
                    Identifier pid = new Identifier();
                    pid.setValue(id);
                    event.setIdentifier(pid);
                    event.setDate(Calendar.getInstance().getTime());
                    event.setAction(Event.DELETE);
                    String error = "IndexGenerator.index - Metacat Index couldn't remove the index for the id - "+id+" because "+e.getMessage();
                    event.setDescription(error);
                    try {
                        EventlogFactory.createIndexEventLog().write(event);
                    } catch (Exception ee) {
                        log.error("SolrIndex.insertToIndex - IndexEventLog can't log the index deleting event :"+ee.getMessage());
                    }
                    log.error(error);
                }
                
            }
        }
    }*/
    
    /*
     * Remove the index for the id
     */
    /*private void removeIndex(String id) throws ServiceFailure, XPathExpressionException, NotImplemented, NotFound, UnsupportedType, IOException, SolrServerException, SAXException, ParserConfigurationException, OREParserException  {
        if(id != null) {
            //solrIndex.remove(id);
        }
    }*/
    
    /*
     * Initialize the system metadata map
     */
    private void initSystemMetadataMap() throws FileNotFoundException, ServiceFailure{
        int times = 0;
        if(systemMetadataMap == null) {
            systemMetadataMap = DistributedMapsFactory.getSystemMetadataMap();
        }
    }
    
    /*
     * We should call this method after calling initSystemMetadataMap since this method doesn't have the mechanism to wait the readiness of the hazelcast service
     */
    private void initObjectPathMap() throws FileNotFoundException, ServiceFailure {
        if(objectPathMap == null) {
            objectPathMap = DistributedMapsFactory.getObjectPathMap();
        }
    }
    
    
    
    /*
     * Initialize the index queue
     */
    private void initIndexQueue() throws FileNotFoundException, ServiceFailure {
        if(indexQueue == null) {
            indexQueue = DistributedMapsFactory.getIndexQueue();
        }
    }
    /**
     * Get an InputStream as the data object for the specific pid.
     * @param pid
     * @return
     * @throws FileNotFoundException
     */
    private InputStream getDataObject(String pid) throws FileNotFoundException {
        Identifier identifier = new Identifier();
        identifier.setValue(pid);
        String objectPath = objectPathMap.get(identifier);
        InputStream data = null;
        data = new FileInputStream(objectPath);
        return data;

    }
    
    /**
     * Get the SystemMetadata for the specified id from the distributed Map.
     * The null maybe is returned if there is no system metadata found.
     * @param id  the specified id.
     * @return the SystemMetadata associated with the id.
     */
    private SystemMetadata getSystemMetadata(String id) {
        SystemMetadata metadata = null;
        if(systemMetadataMap != null && id != null) {
            Identifier identifier = new Identifier();
            identifier.setValue(id);
            metadata = systemMetadataMap.get(identifier);
        }
        return metadata;
    }
    
    /**
     * Get the obsoletes chain of the specified id. The returned list doesn't include
     * the specified id itself. The newer version has the lower index number in the list.
     * Empty list will be returned if there is no document to be obsoleted by this id.
     * @param id
     * @return
     */
    private List<String> getObsoletes(String id) {
        List<String> obsoletes = new ArrayList<String>();
        while (id != null) {
            SystemMetadata metadata = getSystemMetadata(id);
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
    
    /**
     * Overwrite and do nothing
     */
    public boolean cancel() {
        return true;
    }

}
