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
public class CreateUsageTask extends UsageTask {
    private static Log logMetacat  = LogFactory.getLog(CreateUsageTask.class);
    
    /**
     * Constructor
     * @param usage  the usage will be reported
     * @param bookkeeperClient  the client to report the usage to the remote server
     */
    public CreateUsageTask(Usage usage, BookKeeperClient bookkeeperClient) {
        super(usage, bookkeeperClient);
    }
    
    @Override
    protected int reportToBookKeeper() throws Exception {
        logMetacat.debug("CreateUsageTask.reportToBookeKeeper - " + 
        "create a new usage in the remote book keeper server with quota id " + 
        usage.getQuotaId() + " instance id " + usage.getInstanceId() + 
                " status " + usage.getStatus() + " quantity " + usage.getQuantity());
        return bookkeeperClient.createUsage(usage);
    }
    
}
