/**
 *  '$RCSfile$'
 *  Copyright: 2022 Regents of the University of California and the
 *              National Center for Ecological Analysis and Synthesis
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
package edu.ucsb.nceas.metacat.index.queue;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.TimerTask;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

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
import org.dataone.service.types.v2.SystemMetadata;
import org.dspace.foresite.OREParserException;
import org.xml.sax.SAXException;

import edu.ucsb.nceas.metacat.common.index.IndexTask;
import edu.ucsb.nceas.metacat.common.index.event.IndexEvent;
import edu.ucsb.nceas.metacat.common.resourcemap.ResourceMapNamespaces;
import edu.ucsb.nceas.metacat.index.MetacatSolrIndex;
import edu.ucsb.nceas.metacat.systemmetadata.SystemMetadataManager;


/*
 * A timer to regenerate failed index tasks or index tasks in a given time frame.
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
    //private static final String RESOURCEMAPPROPERYNAME = "index.resourcemap.namespace";
    public static final String WAITIMEPOPERTYNAME = "index.regenerate.start.waitingtime";
    public static final String MAXATTEMPTSPROPERTYNAME = "index.regenerate.start.maxattempts";
    
    private Log log = LogFactory.getLog(IndexGeneratorTimerTask.class);
    //private MNode mNode = null;
    private static List<String> resourceMapNamespaces = null;
    private boolean needReindexFailedEvent =true; //if this task need to reindex the previously failed index task
    private boolean needReindexSinceLastProcessDate = true; //objects whose modified date is younger than the last process date
    private long maxAgeOfFailedIndexTask = 864000000; // 10 days
    
    /**
     * Constructor
     * @param solrIndex
     * @param systemMetadataListener
     */
    public IndexGeneratorTimerTask() {
        resourceMapNamespaces = ResourceMapNamespaces.getNamespaces();
        try {
            needReindexFailedEvent = Settings.getConfiguration().getBoolean("index.regenerate.failedObject");
        } catch (Exception e) {
            log.warn("IndexGeneratorTimeTask.constructor - the value of property - index.regenerate.failedObject can't be got since "+e.getMessage()+" and we will set it to true as default.");
            needReindexFailedEvent = true;
        }
        try {
            needReindexSinceLastProcessDate = Settings.getConfiguration().getBoolean("index.regenerate.sincelastProcessDate");
        } catch (Exception e) {
            log.warn("IndexGeneratorTimeTask.constructor - the value of property - index.regenerate.sincelastProcessDate can't be got since "+e.getMessage()+" and we will set it to true as default.");
            needReindexSinceLastProcessDate = true;
        }
        maxAgeOfFailedIndexTask = Settings.getConfiguration().getLong("index.regenerate.failedTask.max.age", 864000000);
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
                NotAuthorized, NotImplemented, ServiceFailure, SolrServerException, ClassNotFoundException, InstantiationException, IllegalAccessException, 
                XPathExpressionException, NotFound, UnsupportedType, IOException, SAXException, ParserConfigurationException, OREParserException {
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
                    NotAuthorized, NotImplemented, ServiceFailure, SolrServerException, ClassNotFoundException, InstantiationException, IllegalAccessException, 
                    XPathExpressionException, NotFound, UnsupportedType, IOException, SAXException, ParserConfigurationException, OREParserException {
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
                                                InvalidToken, NotAuthorized, NotImplemented, ServiceFailure, ClassNotFoundException, InstantiationException, IllegalAccessException, XPathExpressionException, NotFound, UnsupportedType, IOException, SAXException, ParserConfigurationException, OREParserException {
        Date processedDate = null;
        List<String> solrIds = null;
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
        
        
        Date latestResourceId = null;
        if (resourceMapIds != null && !resourceMapIds.isEmpty()) {
            int size = resourceMapIds.size();
            String id = resourceMapIds.get(size-1);
            SystemMetadata sysmeta = getSystemMetadata(id);
            latestResourceId = sysmeta.getDateSysMetadataModified();
            maxCollection.add(new Long(latestResourceId.getTime()));
        }
        
        
        if(!maxCollection.isEmpty()) {
            Long max = Collections.max(maxCollection);
            processedDate = new Date(max.longValue());
        }
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
            LastReindexDateManager.getInstance().setLastProcessDate(processedDate);
        }
        
    }
    
    /**
     * Reindex the failed index tasks stored in the index_event table
     * @throws ClassNotFoundException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws IndexEventLogException
     * @throws ServiceFailure 
     * @throws FileNotFoundException 
     */
    private void reIndexFailedTasks() throws ClassNotFoundException, InstantiationException, IllegalAccessException, FileNotFoundException, ServiceFailure {
        //add the failedPids 
        List<IndexEvent> failedEvents = EventlogFactory.createIndexEventLog().getEvents(null, null, null, null);
        //List<String> failedOtherIds = new ArrayList<String>();
        List<String> failedResourceMapIds = new ArrayList<String>();
        if(failedEvents != null) {
            for(IndexEvent event : failedEvents) {
                if(event != null && event.getIdentifier() != null) {
                    String id = event.getIdentifier().getValue();
                    Date now = new Date();
                    //if the event is too old, we will ignore it.
                    if(event.getDate() == null || (event.getDate() != null && ((now.getTime() - event.getDate().getTime()) <= maxAgeOfFailedIndexTask))) {
                        try {
                            if(event.getAction().compareTo(Event.DELETE) == 0) {
                                //this is a delete event
                                deleteIndex(id);
                            } else {
                                SystemMetadata sysmeta = getSystemMetadata(id);
                                if(sysmeta != null) {
                                    ObjectFormatIdentifier formatId =sysmeta.getFormatId();
                                    if(formatId != null && formatId.getValue() != null && resourceMapNamespaces != null && isResourceMap(formatId)) {
                                        failedResourceMapIds.add(id);
                                    } else {
                                        //failedOtherIds.add(id);
                                        submitIndex(id);
                                    }
                                } else {
                                    log.info("IndexGenerate.reIndexFAiledTasks - we wouldn't submit the reindex task for the pid "+id+" since there is no system metadata associate it");
                                }
                            }
                        } catch (Exception e) {
                            log.warn("IndexGenerate.reIndexFAiledTasks - failed to submit the reindex task for the pid "+id+" since "+e.getMessage());
                        }
                    } else {
                        log.info("IndexGenerate.reIndexFAiledTasks - we wouldn't submit the reindex task for the pid "+id+" since it is too old.");
                    }
                }
            }
        }
        //index(failedOtherIds);
        index(failedResourceMapIds);
    }
    
    
    /*
     * Put the ids into the index queue
     */
    private void index(List<String> metacatIds) {
        if(metacatIds != null) {
            for(String metacatId : metacatIds) {
                try {
                    submitIndex(metacatId);
                } catch (Exception e) {
                    log.warn("IndexGeneratorTimeTask.index - can't submit the index task for the id "+metacatId +" since "+e.getMessage());
                }
            }
        }
    }
    
    /**
     * Submit the index task to the queue for the given id
     * @param id the id will be submitted
     * @throws Exception 
     */
    private void submitIndex(String id) throws Exception {
        if(id != null) {
            SystemMetadata sysmeta = getSystemMetadata(id);
            Identifier pid = new Identifier();
            pid.setValue(id);
            boolean isSysmetaChangeOnly = false;
            boolean followRevisions = false;
            MetacatSolrIndex.getInstance().submit(pid, sysmeta, isSysmetaChangeOnly, followRevisions);
            log.info("IndexGenerator.index - submitted the pid " + pid.getValue() +" into RabbitMQ successfully.");
       }
    }
    
    /**
     * Put a delete index task into the index queue
     * @param id
     */
    private void deleteIndex(String id) throws Exception {
        if(id != null) {
            SystemMetadata sysmeta = getSystemMetadata(id);
            Identifier pid = new Identifier();
            pid.setValue(id);
            MetacatSolrIndex.getInstance().submitDeleteTask(pid, sysmeta);
            log.info("IndexGenerator.deleteIndex - submitted the task which deletes pid " + pid.getValue() 
                     + " into Rabbitmq successfully.");
        }
    }
    
   
    
    public void run() {
    
        try {
            log.info("IndexGenerator.run - start to run the index generator timer--------------------------------");
            if(needReindexFailedEvent) {
                log.info("IndexGenerator.run - start to reindex previous failed index tasks--------------------------------");
                reIndexFailedTasks();
            }
            if(needReindexSinceLastProcessDate) {
                log.info("IndexGenerator.run - start to index objects whose modified date is younger than the last process date--------------------------------");
                Date since = LastReindexDateManager.getInstance().getLastProcessDate();
                index(since);
            }
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
        } catch (ParseException e) {
            
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
                        } else if (correctTimeRange) {
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
       return ResourceMapNamespaces.isResourceMap(formatId);
    }
    
    /**
     * Get the SystemMetadata for the specified id from the distributed Map.
     * The null maybe is returned if there is no system metadata found.
     * @param id  the specified id.
     * @return the SystemMetadata associated with the id.
     * @throws ServiceFailure 
     */
    private SystemMetadata getSystemMetadata(String id) throws ServiceFailure {
        SystemMetadata metadata = null;
        if(id != null && !id.trim().equals("")) {
            Identifier identifier = new Identifier();
            identifier.setValue(id);
            metadata = SystemMetadataManager.getInstance().get(identifier);
        }
        return metadata;
    }

    
    /**
     * Overwrite and do nothing
     */
    public boolean cancel() {
        return true;
    }

}
