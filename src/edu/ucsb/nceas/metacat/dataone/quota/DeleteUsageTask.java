/**
 *    Purpose: Implements a service for managing a Hazelcast cluster member
 *  Copyright: 2020 Regents of the University of California and the
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
package edu.ucsb.nceas.metacat.dataone.quota;

import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dataone.bookkeeper.api.Usage;

/**
 * This class represents a thread task to report an event to delete of an exiting usage to the remote bookkeeper server.
 * It also will log the event in the local database. If the reporting to the remote book keeper service succeeds,
 * it will set the reported date in the local usages table; otherwise it keeps the field null.
 * @author tao
 *
 */
public class DeleteUsageTask implements Runnable {
    private Usage usage = null;
    private BookKeeperClient bookkeeperClient = null;
    private static Log logMetacat  = LogFactory.getLog(DeleteUsageTask.class);
    
    /**
     * Constructor
     * @param usage  the usage will be reported
     * @param bookkeeperClient  the client to report the usage to the remote server
     */
    public DeleteUsageTask(Usage usage, BookKeeperClient bookkeeperClient) {
        this.usage = usage;
        this.bookkeeperClient = bookkeeperClient;
    }
    
    /**
     * Report the deleting usage event to the book keeper server and also log it in the local db.
     */
    public void run() {
        if (usage != null) {
            try {
                bookkeeperClient.deleteUsage(usage.getQuotaId(), usage.getInstanceId());
            } catch (Exception e) {
                logMetacat.warn("DeleteUsageTask.run - can't report the updated usage to the remote server since " + e.getMessage());
                //Reporting usage to the remote bookkeeper server failed. So we need to create a usage record without the reported date in the local database (by setting the date null).
                //Another periodic thread will try to report the usage again some time later.
                try {
                    Date now = null;
                    QuotaDBManager.createUsage(usage, now);
                } catch (Exception ee) {
                    logMetacat.error("DeleteUsageTask.run - can't save the usage with to the local usages table since " + ee.getMessage() + 
                            " The usage is with the quota id " + usage.getQuotaId() + " instance id " + usage.getInstanceId() + " the quantity " + usage.getQuantity());
                }
                return;
            }
            //Reported the usage to the remote bookkeeper server succeeded. So we need to create a usage record with reported date in the local database.
            Date now = new Date();
            try {
                QuotaDBManager.createUsage(usage, now);
            } catch (Exception ee) {
                logMetacat.error("DeleteUsageTask.run - can't save the usage with to the local usages table since " + ee.getMessage() +
                        " The usage is with the quota id " + usage.getQuotaId() + " instance id " + usage.getInstanceId() + " the quantity " + usage.getQuantity() + " the reported date " + now.getTime());
            }
        }
    }

}
