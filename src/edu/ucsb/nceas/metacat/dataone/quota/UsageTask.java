/**
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
 * The parent class reporting usages to the remote book keeper server and logging the quota usages events 
 * into the local database. The children classes are CreateUsageTask, UpdateUsageTask and DeleteUsageTask.
 * @author tao
 *
 */
public abstract class UsageTask implements Runnable {
    private static Log logMetacat  = LogFactory.getLog(UsageTask.class);
    
    protected Usage usage = null;
    protected BookKeeperClient bookkeeperClient = null;
    protected boolean isLoggedLocally = false;
    
    /**
     * Constructor
     * @param usage  the usage will be reported
     * @param bookkeeperClient  the client to report the usage to the remote server
     */
    public UsageTask(Usage usage, BookKeeperClient bookkeeperClient) {
        this.usage = usage;
        this.bookkeeperClient = bookkeeperClient;
    }
    
    /**
     * Set the property if the usage has been logged locally or not
     * @param isLoggedLocally  true means local database has the record; otherwise false.
     */
    public void setIsLoggedLocally(boolean isLoggedLocally) {
        this.isLoggedLocally = isLoggedLocally;
    }
    
    @Override
    public void run() {
        if (usage != null) {
            try {
                reportToBookKeeper();
            } catch (Exception e) {
                logMetacat.warn("UsageTask.run - can't report the usage to the remote server since " + e.getMessage());
                if (!isLoggedLocally) {
                    //Reporting usage to the remote bookkeeper server failed. So we need to create a usage record without the reported date in the local database (by setting the date null).
                    //Another periodic thread will try to report the usage again some time later.
                    try {
                        Date now = null;
                        QuotaDBManager.createUsage(usage, now);
                    } catch (Exception ee) {
                        logMetacat.error("UsageTask.run - can't save the usage to the local usages table since " + ee.getMessage() + 
                                " The usage is with the quota id " + usage.getQuotaId() + " instance id " + usage.getInstanceId() + " the quantity " + usage.getQuantity() + " status " + usage.getStatus());
                    }
                } else {
                    logMetacat.debug("UsageTask.run - even though Metacat failed to report the usage with the quota id " + usage.getQuotaId() + 
                            " instance id " + usage.getInstanceId() + " the quantity " + usage.getQuantity() + " status " + usage.getStatus() + 
                            " to the remote book keeper server. However, the local database already has the record, we don't need to do anything.");
                }
                return;
            }
            //reporting succeeded, so we need to change the local database.
            Date now = new Date();
            if (!isLoggedLocally) {
               //Reporting the usage to the remote bookkeeper server succeeded. So we need to create a new usage record with reported date in the local database.
                logMetacat.debug("UsageTask.run - Metacat succeeded to report the usage with the quota id " + usage.getQuotaId() + 
                        " instance id " + usage.getInstanceId() + " the quantity " + usage.getQuantity() + " status " + usage.getStatus() + 
                        " to the remote book keep server. However, the local database does NOT have a record for it and Metacat need to create a new usage record with reported date in the local database.");
                try {
                    QuotaDBManager.createUsage(usage, now);
                } catch (Exception ee) {
                    logMetacat.error("UsageTask.run - can't create a new usage to the local usages table since " + ee.getMessage() +
                            " The usage is with the quota id " + usage.getQuotaId() + " instance id " + usage.getInstanceId() + " the status " + usage.getStatus() + " the quantity " + usage.getQuantity() + " the reported date " + now.getTime() + " object " + usage.getObject());
                }
            } else {
                logMetacat.debug("UsageTask.run - Metacat succeeded to report the usage with the quota id " + usage.getQuotaId() + 
                        " instance id " + usage.getInstanceId() + " the quantity " + usage.getQuantity() + " status " + usage.getStatus() + 
                        " to the remote book keep server. Moreover, the local database already has the record and Metacat needs to set the local reported date in this record.");
                try {
                    QuotaDBManager.setReportedDate(usage.getId(), now);
                } catch (Exception ee) {
                    logMetacat.error("UsageTask.run - can't update the usage in the local quota_usage_event table since " + ee.getMessage() +
                            " The usage is with the with the local usage id " + usage.getId() + " quota id " + usage.getQuotaId() + " instance id " + usage.getInstanceId() + " the quantity " + usage.getQuantity() + " the reported date " + now.getTime());
                }
            }
        }
    }
    
    /**
     * The child class needs to implement this method to detail how to report the usage to
     * the remote book keeper server.
     */
    protected abstract void reportToBookKeeper() throws Exception;

}
