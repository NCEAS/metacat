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
    private static Log log = LogFactory.getLog(FailedIndexResubmitTimerTask.class);
    //if this task need to reindex the previously failed index task
    private boolean needReindexFailedEvent = true;
    protected static long maxAgeOfFailedIndexTask = 864000000; // 10 days
    
    /**
     * Constructor
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
        Date now = new Date();
        //the default oldest age is 10 days earlier (older) than the current time
        Date oldestAge = new Date(now.getTime() - maxAgeOfFailedIndexTask);
        try {
            List<IndexEvent> failedCreateEvents = IndexEventDAO.getInstance()
                                                .get(IndexEvent.CREATE_FAILURE_TO_QUEUE, oldestAge);
            reindexFailedTasks(failedCreateEvents);
        } catch (SQLException e) {
            log.error("FailedIndexResubmitTimerTask.reIndexFAiledTasks - failed to get the failure "
                     + "create index task list since " + e.getMessage());
        }
        
        try {
            List<IndexEvent> failedDeleteEvents = IndexEventDAO.getInstance()
                                                .get(IndexEvent.DELETE_FAILURE_TO_QUEUE, oldestAge);
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
