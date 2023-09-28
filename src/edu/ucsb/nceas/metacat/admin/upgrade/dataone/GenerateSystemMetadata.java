package edu.ucsb.nceas.metacat.admin.upgrade.dataone;


import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.ucsb.nceas.metacat.DBUtil;
import edu.ucsb.nceas.metacat.IdentifierManager;
import edu.ucsb.nceas.metacat.admin.AdminException;
import edu.ucsb.nceas.metacat.admin.upgrade.UpgradeUtilityInterface;
import edu.ucsb.nceas.metacat.dataone.SystemMetadataFactory;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.utilities.SortedProperties;

public class GenerateSystemMetadata implements UpgradeUtilityInterface {

    private static Log log = LogFactory.getLog(GenerateSystemMetadata.class);

    private int serverLocation = 1;

    public boolean upgrade() throws AdminException {

        // do this in a thread too so that we don't have to hang the UI (web)
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Runnable command = new Runnable() {
            @Override
            public void run() {
                // just run it
                try {
                    boolean success = multiThreadUpgrade();
                } catch (AdminException e) {
                    throw new RuntimeException(e);
                }
            }
        };
        executor.execute(command);
        executor.shutdown();

        // wait for it to finish before returning?
        boolean wait = false;
        if (wait) {
            log.debug("Waiting for upgrade to complete");
            try {
                executor.awaitTermination(Long.MAX_VALUE, TimeUnit.HOURS);
            } catch (InterruptedException e) {
                AdminException ae = new AdminException(e.getMessage());
                ae.initCause(e);
                throw ae;
            }
            log.debug("Done waiting for upgrade thread");
        }

        return true;
        //return singleThreadUpgrade();
    }

    /**
     * Use multiple threads to process parts of the complete ID list concurrently
     * @return
     * @throws AdminException
     */
    public boolean multiThreadUpgrade() throws AdminException {

        boolean success = true;

        // do not include ORE or data, but can generate SystemMetadata for ALL records
        final boolean includeOre = false;
        final boolean downloadData = false;

        try {

            // the ids for which to generate system metadata
            List<String> idList = null;
            // only get local objects
            idList = IdentifierManager.getInstance()
                .getLocalIdsWithNoSystemMetadata(true, serverLocation);

            // for testing, subset to a limited random number
            boolean test = false;
            if (test) {
                idList = DBUtil.getAllDocids("knb-lter-gce"); // use a scope
                Collections.sort(idList);
                int start = 0;
                int count = 100;
                int limit = Math.min(idList.size(), start + count);
                idList = idList.subList(start, limit);
                log.debug("limiting test list to: " + start + "-" + limit);
                for (String docid : idList) {
                    log.debug("GENERATING SM TEST: " + docid);
                }
            }

            // make sure the list is sorted so we can break them into sublists for the threads
            Collections.sort(idList);

            // executor
            int availableProcessors = Runtime.getRuntime().availableProcessors();
            int nThreads = availableProcessors * 1;
            //availableProcessors++;
            log.debug("Using nThreads: " + nThreads);

            ExecutorService executor = Executors.newFixedThreadPool(nThreads);
            int taskCount = 0;

            // chunk into groups
            int fromIndex = 0;
            int toIndex = 0;
            String prefix = null;
            for (String docid : idList) {

                // increment the next entry, exclusive
                toIndex++;

                // use scope.docid (without revision) to determine groups
                // handle first document on its own, and a clause for the last document
                if (prefix == null || !docid.startsWith(prefix) || toIndex == idList.size()) {

                    // construct a sublist for this previous group of docids
                    final List<String> subList = idList.subList(fromIndex, toIndex);
                    log.debug("Grouping docid prefix: " + prefix);
                    log.debug("subList.size: " + subList.size());

                    // add the task for this sublist
                    Runnable command = new Runnable() {
                        @Override
                        public void run() {
                            // generate based on this list
                            try {
                                log.debug("Processing subList.size: " + subList.size());
                                SystemMetadataFactory.generateSystemMetadata(
                                    subList, includeOre, downloadData);
                                log.debug("Done processing subList.size: " + subList.size());

                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        }
                    };

                    // execute the task
                    executor.execute(command);
                    taskCount++;

                    // start at the end of this sublist
                    fromIndex = toIndex;

                }

                log.debug("docid: " + docid);

                // get the previous docid prefix
                String previousId = docid;
                prefix = previousId.substring(0, previousId.lastIndexOf("."));

            }

            log.info("done launching threaded tasks, count: " + taskCount);

            // wait for executor to finish
            executor.shutdown();

            // wait a long time
            log.debug("Waiting for all threads to complete");
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.HOURS);
            log.debug("Done waiting for all threads to complete");
            // now we are ready to be a data one node
            PropertyService.setProperty(
                "dataone.systemmetadata.generated", Boolean.TRUE.toString());

        } catch (Exception e) {
            String msg = "Problem generating missing system metadata: " + e.getMessage();
            log.error(msg, e);
            success = false;
            throw new AdminException(msg);
        }
        return success;
    }

    public int getServerLocation() {
        return serverLocation;
    }

    public void setServerLocation(int serverLocation) {
        this.serverLocation = serverLocation;
    }

    public static void main(String[] args) {

        try {
            // set up the properties based on the test/deployed configuration of the workspace
            SortedProperties testProperties = new SortedProperties("test/test.properties");
            testProperties.load();
            String metacatContextDir = testProperties.getProperty("metacat.contextDir");
            PropertyService.getInstance(metacatContextDir + "/WEB-INF");

            // make an upgrader instance
            GenerateSystemMetadata upgrader = new GenerateSystemMetadata();

            // set any command line params, like the home server to run this for
            if (args.length > 0) {
                String serverLocation = args[0];
                upgrader.setServerLocation(Integer.parseInt(serverLocation));
            }

            // now run it
            upgrader.upgrade();

        } catch (Exception ex) {
            System.out.println("Exception:" + ex.getMessage());
            ex.printStackTrace();
        }
    }
}
