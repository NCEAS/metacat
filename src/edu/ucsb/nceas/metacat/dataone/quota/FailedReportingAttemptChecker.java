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

import java.sql.ResultSet;
import java.util.concurrent.ExecutorService;
import java.util.TimerTask;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dataone.bookkeeper.api.Usage;



/**
 * This class periodically gets reporting usages which were failed to be reported to the 
 * remote book keeper service (e.g. the failure of the network) and report them again.
 * The indicator of the failed reporting is that the reported_date is null in the local usage table.
 * @author tao
 *
 */
public class FailedReportingAttemptChecker extends TimerTask {
    private static Log logMetacat  = LogFactory.getLog(FailedReportingAttemptChecker.class);
    
    private ExecutorService executor = null;
    BookKeeperClient bookkeeperClient = null;
    
    /**
     * Constructor
     * @param executor  the executor which will accept the runnable reporting classes
     * @param bookkeeperClient  the book keeper client which will access the remote the server
     */
    public FailedReportingAttemptChecker (ExecutorService executor, BookKeeperClient bookkeeperClient) {
        this.executor = executor;
        this.bookkeeperClient = bookkeeperClient;
    }
    
    /**
     * It will get the list of unreported usages and re-submit them to the book keeper server again.
     */
    public void run() {
        try {
            ResultSet rs = QuotaDBManager.getUnReportedUsages();
            int localId = -1;
            while (rs.next()) {
                try {
                    Usage usage = new Usage();
                    localId = rs.getInt(1);
                    usage.setId(localId);//Note: this is the local usage id. It doesn't match the one in the remote book keeper server.
                    usage.setQuotaId(rs.getInt(2));
                    usage.setInstanceId(rs.getString(3));
                    usage.setQuantity(rs.getDouble(4));
                    usage.setObject(rs.getString(5));
                    String status = rs.getString(6);
                    usage.setStatus(status);
                    UsageTask task = null;
                    if (status != null && status.equals(QuotaServiceManager.ACTIVE)) {
                        task = new CreateUsageTask(usage, bookkeeperClient);
                    } else if (status != null && status.equals(QuotaServiceManager.ARCHIVED)) {
                        task = new UpdateUsageTask(usage, bookkeeperClient);
                    } else if (status != null && status.equals(QuotaServiceManager.DELETED)) {
                        task = new DeleteUsageTask(usage, bookkeeperClient);
                    } else {
                        throw new Exception("Doesn't support the status of the usage " + status);
                    }
                    task.setIsLoggedLocally(true);//indicates that the local db have the record
                    executor.submit(task);
                } catch (Exception ee) {
                    logMetacat.error("FailedReportingAttemptChecker.run - can't report the usage to the remote book server with the local id " + localId + " since " + 
                                     ee.getMessage() + ". If the local id is -1. It means the local id can't be got from the local db.");
                }
            }
        } catch (Exception e) {
            logMetacat.error("FailedReportingAttemptChecker.run - can't get the result set of un-reported usages since " + e.getMessage());
        }
    }

}
