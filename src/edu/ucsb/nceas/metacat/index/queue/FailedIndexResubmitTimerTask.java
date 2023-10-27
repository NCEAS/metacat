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

import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.TimerTask;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dataone.configuration.Settings;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v2.SystemMetadata;

import edu.ucsb.nceas.metacat.common.index.event.IndexEvent;
import edu.ucsb.nceas.metacat.index.IndexEventDAO;
import edu.ucsb.nceas.metacat.index.MetacatSolrIndex;


/**
 * A timer to regenerate failed index tasks or index tasks in a given time frame.
 */
public class FailedIndexResubmitTimerTask extends TimerTask {

    public static final int WAITTIME = 10000;
    public static final int MAXWAITNUMBER = 180;
    public static final String WAITIMEPOPERTYNAME = "index.regenerate.start.waitingtime";
    public static final String MAXATTEMPTSPROPERTYNAME = "index.regenerate.start.maxattempts";
    
    private static Log log = LogFactory.getLog(FailedIndexResubmitTimerTask.class);
    //if this task need to reindex the previously failed index task
    private boolean needReindexFailedEvent = true;
    private long maxAgeOfFailedIndexTask = 864000000; // 10 days
    
    /**
     * Constructor
     * @param solrIndex
     * @param systemMetadataListener
     */
    public FailedIndexResubmitTimerTask() {
        try {
            needReindexFailedEvent = Settings.getConfiguration()
                                            .getBoolean("index.regenerate.failedObject");
        } catch (Exception e) {
            log.warn("FailedIndexResubmitTimerTask.constructor - the value of property "
                                 + "- index.regenerate.failedObject can't be got since "
                                 +  e.getMessage() + " and we will set it to true as default.");
            needReindexFailedEvent = true;
        }
        log.info("FailedIndexResubmitTimerTask.constructor - we need to reindex the failed "
                                                + "index tasks: " + needReindexFailedEvent);
        maxAgeOfFailedIndexTask = Settings.getConfiguration()
                                    .getLong("index.regenerate.failedTask.max.age", 864000000);
    }
    
    /**
     * Reindex the failed index tasks stored in the index_event table
     */
    private void reindexFailedTasks() {
        try {
            List<IndexEvent> failedCreateEvents = IndexEventDAO.getInstance()
                                                        .get(IndexEvent.CREATE_FAILURE_TO_QUEUE);
            reindexFailedTasks(failedCreateEvents);
        } catch (SQLException e) {
            log.error("FailedIndexResubmitTimerTask.reIndexFAiledTasks - failed to get the failure "
                     + "create index task list since " + e.getMessage());
        }
        
        try {
            List<IndexEvent> failedDeleteEvents = IndexEventDAO.getInstance()
                                                         .get(IndexEvent.DELETE_FAILURE_TO_QUEUE);
            reindexFailedTasks(failedDeleteEvents);
        } catch (SQLException e) {
            log.error("FailedIndexResubmitTimerTask.reIndexFAiledTasks - failed to get the failure "
                    + "delete index task list since " + e.getMessage());
        }
        
    }

    /**
     * Reindex the failed index tasks stored in the index_event table
     * @param failedEvents  the failed index event list
     */
    private void reindexFailedTasks(List<IndexEvent> failedEvents) {
        if(failedEvents != null) {
            boolean firstTime = true;
            boolean deleteEvent = false;
            for(IndexEvent event : failedEvents) {
                if(event != null && event.getIdentifier() != null) {
                    String id = event.getIdentifier().getValue();
                    if (id != null && !id.trim().equals("")) {
                        Date now = new Date();
                        //if the event is too old, we will ignore it.
                        if(event.getDate() == null || (event.getDate() != null &&
                           ((now.getTime() - event.getDate().getTime())
                                   <= maxAgeOfFailedIndexTask))) {
                            try {
                                if(firstTime && event.getAction()
                                              .compareTo(IndexEvent.DELETE_FAILURE_TO_QUEUE) == 0) {
                                    firstTime = false;
                                    deleteEvent = true;
                                }
                                if (deleteEvent) {
                                    //this is a delete event
                                    deleteIndex(id);
                                    //Succeeded and remove it from the index event table
                                    IndexEventDAO.getInstance().remove(event.getIdentifier());
                                } else {
                                    IndexGeneratorTimerTask.submitIndex(id);
                                    //Succeeded and remove it from the index event table
                                    IndexEventDAO.getInstance().remove(event.getIdentifier());
                                }
                            } catch (Exception e) {
                                log.warn("FailedIndexResubmitTimerTask.reIndexFAiledTasks - failed "
                                        + "to submit the reindex task for the pid " + id
                                        + " since " + e.getMessage());
                            }
                        } else {
                            log.info("FailedIndexResubmitTimerTask.reIndexFAiledTasks - we wouldn't"
                                  + " submit the reindex task for the pid " + id 
                                  + " since it is too old.");
                        }
                    }
                }
            }
        }
    }

    
    /**
     * Put a delete index task into the index queue
     * @param id  the id whose solr doc needs to be deleted
     */
    private void deleteIndex(String id) throws Exception {
        if(id != null) {
            SystemMetadata sysmeta = IndexGeneratorTimerTask.getSystemMetadata(id);
            Identifier pid = new Identifier();
            pid.setValue(id);
            MetacatSolrIndex.getInstance().submitDeleteTask(pid, sysmeta);
            log.debug("FailedIndexResubmitTimerTask.deleteIndex - "
                          + "submitted the task which deletes pid "
                          + pid.getValue() + " into Rabbitmq successfully.");
        }
    }

    public void run() {
        if (needReindexFailedEvent) {
            log.debug("FailedIndexResubmitTimerTask.run - start to reindex previous "
                        + "failed index tasks");
            reindexFailedTasks();
        }
    }

    /**
     * Overwrite and do nothing
     */
    public boolean cancel() {
        return true;
    }

}
