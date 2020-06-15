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
 * This class represents a thread task to report a updating of an exiting usage to the remote bookkeeper server.
 * It also will log the update in the local database. If the reporting to the remote book keeper service succeeds,
 * it will set the reported date in the local usages table; otherwise it keeps the field null.
 * @author tao
 *
 */
public class UpdateUsageTask extends UsageTask {
    private static Log logMetacat  = LogFactory.getLog(UpdateUsageTask.class);
    /**
     * Constructor
     * @param usage  the usage will be reported
     * @param bookkeeperClient  the client to report the usage to the remote server
     */
    public UpdateUsageTask(Usage usage, BookKeeperClient bookkeeperClient) {
        super(usage, bookkeeperClient);
    }
    
    @Override
    protected void reportToBookKeeper() throws Exception {
        logMetacat.debug("UpdateUsageTask.reportToBookeKeeper - update an existing usage in the remote book book with quota id " + usage.getQuotaId() + " instance id " + usage.getInstanceId() + 
                " status " + usage.getStatus() + " quantity " + usage.getQuantity());
        bookkeeperClient.updateUsage(usage.getQuotaId(), usage.getInstanceId(), usage);//This method will get the real usage id from the remote server.
    }
}
