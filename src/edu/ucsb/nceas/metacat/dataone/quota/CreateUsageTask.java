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
 * This class represents a task to report a creation of a usage to the remote bookkeeper server and 
 * set the reported date in the local usages table after the succeeded reporting
 * @author tao
 *
 */
public class CreateUsageTask implements Runnable {
    private static Log logMetacat  = LogFactory.getLog(CreateUsageTask.class);
    
    private Usage usage = null;
    private BookKeeperClient bookkeeperClient = null;
    
    /**
     * Constructor
     * @param usage  the usage will be reported
     * @param bookkeeperClient  the client to report the usage to the remote server
     */
    public CreateUsageTask(Usage usage, BookKeeperClient bookkeeperClient) {
        this.usage = usage;
        this.bookkeeperClient = bookkeeperClient;
    }
    
    @Override
    public void run() {
        if (usage != null) {
            try {
                bookkeeperClient.createUsage(usage);
            } catch (Exception e) {
                logMetacat.warn("CreateUsageTask.run - can't report the usage to the remote server since " + e.getMessage());
                //Reporting usage to the remote bookkeeper server failed. So we need to create a usage record without the reported date in the local database (by setting the date null).
                //Another periodic thread will try to report the usage again some time later.
                try {
                    Date now = null;
                    QuotaDBManager.createUsage(usage, now);
                } catch (Exception ee) {
                    logMetacat.error("CreateUsageTask.run - can't save the usage with to the local usages table since " + ee.getMessage() + 
                            " The usage is with the quota id " + usage.getQuotaId() + " instance id " + usage.getInstanceId() + " the quantity " + usage.getQuantity());
                }
                return;
            }
            //Reported the usage to the remote bookkeeper server succeeded. So we need to create a usage record with reported date in the local database.
            Date now = new Date();
            try {
                QuotaDBManager.createUsage(usage, now);
            } catch (Exception ee) {
                logMetacat.error("CreateUsageTask.run - can't save the usage with to the local usages table since " + ee.getMessage() +
                        " The usage is with the quota id " + usage.getQuotaId() + " instance id " + usage.getInstanceId() + " the quantity " + usage.getQuantity() + " the reported date " + now.getTime());
            }
        }
    }
    
}
