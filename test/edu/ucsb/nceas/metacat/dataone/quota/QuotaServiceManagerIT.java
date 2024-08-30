package edu.ucsb.nceas.metacat.dataone.quota;

import java.io.FileInputStream;
import java.io.InputStream;
import java.sql.ResultSet;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.servlet.http.HttpServletRequest;

import org.apache.wicket.protocol.http.mock.MockHttpServletRequest;
import org.dataone.bookkeeper.api.Quota;
import org.dataone.bookkeeper.api.Usage;
import org.dataone.configuration.Settings;
import org.dataone.service.exceptions.InsufficientResources;
import org.dataone.service.exceptions.NotFound;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.ObjectFormatIdentifier;
import org.dataone.service.types.v1.Session;
import org.dataone.service.types.v1.Subject;
import org.dataone.service.types.v2.SystemMetadata;
import org.junit.Before;
import org.junit.Test;

import edu.ucsb.nceas.metacat.dataone.D1NodeServiceTest;
import edu.ucsb.nceas.metacat.dataone.MNodeService;
import edu.ucsb.nceas.metacat.systemmetadata.SystemMetadataManager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * A IT test class to test all quota services
 * @author tao
 *
 */
public class QuotaServiceManagerIT {
    private final static String nodeId = Settings.getConfiguration().getString("dataone.nodeId");
    private final static String SUBSCRIBERWITHOUTENOUGHQUOTA =
        "CN=membership-EF2C028D-2168-495A-8B15-2177D0F18DB4,DC=dataone,DC=org";
    public final static String SUBSCRIBER =
        "CN=membership-090E3A1B-9E79-4CC5-964B-2598EEEBFC8C,DC=dataone,DC=org";
    public final static String REQUESTOR = "http://orcid.org/0000-0003-1501-0861";
    private final static String DELINGUENT_SUBSCRIBER =
        "CN=membership-A9B9217B-7023-4704-BE19-251866B60E09,DC=dataone,DC=org";
    private final static String DELINGUENT_REQUESTOR = "http://orcid.org/0000-0003-1758-9950";
    private final static String DEFICIT_SUBSCRIBER =
        "CN=membership-EF2C028D-2168-495A-8B15-2177D0F18DB4,DC=dataone,DC=org";
    private final static String DEFICIT_REQUESTOR =
        "CN=Robert Nahf A579,O=Google,C=US,DC=cilogon,DC=org";

    private static int maxAttempt = 50;
    private static String portalFilePath = "test/example-portal.xml";

    private D1NodeServiceTest d1NodeTest = null;
    private MockHttpServletRequest request = null;

    /**
     * Set up the test fixtures
     * @throws Exception
     */
    @Before
    public void setUp() throws Exception {
        d1NodeTest = new D1NodeServiceTest("initialize");
        request = (MockHttpServletRequest)d1NodeTest.getServletRequest();
        java.util.logging.Logger.getLogger("org.apache.http.wire")
            .setLevel(java.util.logging.Level.FINEST);
        java.util.logging.Logger.getLogger("org.apache.http.headers")
            .setLevel(java.util.logging.Level.FINEST);

        System.setProperty(
            "org.apache.commons.logging.Log", "org.apache.commons.logging.impl.SimpleLog");
        System.setProperty("org.apache.commons.logging.simplelog.showdatetime", "true");
        System.setProperty("org.apache.commons.logging.simplelog.log.httpclient.wire", "debug");
        System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.http", "debug");
        System.setProperty(
            "org.apache.commons.logging.simplelog.log.org.apache.http.headers", "debug");
    }


    /*************************************************************
     * Test the BookKeeperClient class
     *************************************************************/

    /**
     * Test the BookKeeperClient.createUsageMethod
     * @throws Exception
     */
    @Test
    public void testBookKeeperClientMethods() throws Exception {
        if (!QuotaServiceManager.getInstance().isEnabled()) {
            return;
        }
        //test to list quotas
        List<Quota> quotas = null;
        try {
            quotas = BookKeeperClient.getInstance()
                .listQuotas("foo", REQUESTOR, QuotaTypeDeterminer.PORTAL);
            fail("Should throw an exception and can't get here");
        } catch (Exception e) {

        }
        try {
            quotas = BookKeeperClient.getInstance()
                .listQuotas(DELINGUENT_SUBSCRIBER, DELINGUENT_REQUESTOR,
                            QuotaTypeDeterminer.PORTAL);
            fail("Should throw an exception and can't get here");
        } catch (NotFound e) {
            assertTrue(e.getMessage().contains(DELINGUENT_SUBSCRIBER));
        }
        quotas = BookKeeperClient.getInstance()
            .listQuotas(DEFICIT_SUBSCRIBER, DEFICIT_REQUESTOR, QuotaTypeDeterminer.PORTAL);
        assertTrue(quotas.size() >= 1);

        quotas = BookKeeperClient.getInstance()
            .listQuotas(SUBSCRIBER, REQUESTOR, QuotaTypeDeterminer.PORTAL);
        assertTrue(quotas.size() >= 1);
        int portalQuotaId = quotas.get(0).getId();

        //test to create a usage (local record one)
        String instanceId = generateUUID();
        String status = QuotaServiceManager.ACTIVE;
        double quantity = 1;
        Usage usage = createUsage(portalQuotaId, instanceId, quantity, status);
        BookKeeperClient.getInstance().createUsage(usage);
        List<Usage> usages = BookKeeperClient.getInstance().listUsages(portalQuotaId, instanceId);
        assertTrue(usages.size() == 1);
        Usage returnedUsage = usages.get(0);
        assertTrue(returnedUsage.getInstanceId().equals(instanceId));
        assertTrue(returnedUsage.getStatus().equals(status));
        assertTrue(returnedUsage.getQuotaId() == portalQuotaId);
        assertTrue(returnedUsage.getNodeId().equals(nodeId));
        int remoteUsageId = returnedUsage.getId();

        //test to create a usage (local record two)
        returnedUsage.setStatus(QuotaServiceManager.INACTIVE);
        BookKeeperClient.getInstance().updateUsage(portalQuotaId, instanceId, returnedUsage);
        //The above updateUsage method is asynchronized, so we should wait until the status
        // changed in the remote server
        int times = 0;
        while (times < maxAttempt) {
            usages = BookKeeperClient.getInstance().listUsages(portalQuotaId, instanceId);
            assertTrue(usages.size() == 1);
            returnedUsage = usages.get(0);
            if (!returnedUsage.getStatus().equals(QuotaServiceManager.INACTIVE)) {
                Thread.sleep(2000);
                times++;//The status hasn't been updated, continue to try until it reaches the
                // max attempt.
            } else {
                break;
            }
        }
        assertTrue(returnedUsage.getInstanceId().equals(instanceId));
        assertTrue(returnedUsage.getStatus().equals(QuotaServiceManager.INACTIVE));
        assertTrue(returnedUsage.getQuotaId() == portalQuotaId);
        assertTrue(returnedUsage.getId() == remoteUsageId);
        assertTrue(returnedUsage.getNodeId().equals(nodeId));

        //test getRemoteUsageid method without an local cached remote usage id
        assertTrue(BookKeeperClient.getInstance().getRemoteUsageId(portalQuotaId, instanceId)
                       == remoteUsageId);

        //test to delete a usage (local record three)
        BookKeeperClient.getInstance().deleteUsage(portalQuotaId, instanceId);
        boolean notFound = false;
        times = 0;
        while (times < maxAttempt) {
            try {
                usages = BookKeeperClient.getInstance().listUsages(portalQuotaId, instanceId);
            } catch (NotFound e) {
                notFound = true;
            }
            if (!notFound) {
                Thread.sleep(2000);
                times++;//The usage in the remote server hasn't been deleted, continue to try
                // until it reaches the max attempt.
            } else {
                break;
            }
        }
        assertTrue(notFound == true);

        //check the local database which should have zero record
        ResultSet rs = QuotaDBManagerIT.getResultSet(portalQuotaId, instanceId);
        int index = 0;
        int indexActive = 0;
        int indexArchived = 0;
        int indexDeleted = 0;
        while (rs.next()) {
            assertTrue(rs.getInt(1) > 0);
            assertTrue(rs.getInt(2) == portalQuotaId);
            assertTrue(rs.getString(3).equals(instanceId));
            assertTrue(rs.getDouble(4) == quantity);
            assertTrue(rs.getTimestamp(5) != null);
            if (rs.getString(6).equals(QuotaServiceManager.ACTIVE)) {
                indexActive++;
            } else if (rs.getString(6).equals(QuotaServiceManager.INACTIVE)) {
                indexArchived++;
            } else if (rs.getString(6).equals(QuotaServiceManager.DELETED)) {
                indexDeleted++;
            }
            index++;
        }
        rs.close();
        assertTrue(index == 0);
        assertTrue(indexActive == 0);
        assertTrue(indexArchived == 0);
        assertTrue(indexDeleted == 0);

        //test the method listUsage to throw an NotFound exception when the instance id doesn't
        // exist.
        String foo = "foooo";
        try {
            BookKeeperClient.getInstance().listUsages(portalQuotaId, foo);
            fail("We shouldn't get there since the above statement should throw an exception");
        } catch (NotFound e) {
            assertTrue(e.getMessage().contains(foo));
        }
    }

    /*************************************************************
     * Test the FailedReportingAttemptChecker class
     *************************************************************/
    /**
     * Test the run method in the FailedReportingAttemptCheck class with three status - active,
     * archived and deleted.
     * @throws Exception
     */
    @Test
    public void testFailedReportingAttemptChecker_Run() throws Exception {
        if (!QuotaServiceManager.getInstance().isEnabled()) {
            return;
        }
        List<Quota> quotas = BookKeeperClient.getInstance()
            .listQuotas(SUBSCRIBER, REQUESTOR, QuotaTypeDeterminer.PORTAL);
        assertTrue(quotas.size() >= 1);
        int portalQuotaId = quotas.get(0).getId();
        //Create three usages without reported date locally
        Date now = null;
        double quantity = 1;
        String instanceId = generateUUID();
        Usage usage = createUsage(portalQuotaId, instanceId, quantity, QuotaServiceManager.ACTIVE);
        QuotaDBManager.createUsage(usage, now);
        usage.setStatus(QuotaServiceManager.INACTIVE);
        QuotaDBManager.createUsage(usage, now);
        usage.setStatus(QuotaServiceManager.DELETED);
        QuotaDBManager.createUsage(usage, now);
        ResultSet rs = QuotaDBManagerIT.getResultSet(portalQuotaId, instanceId);
        //check local database to see if we have those records
        int index = 0;
        int indexActive = 0;
        int indexArchived = 0;
        int indexDeleted = 0;
        while (rs.next()) {
            assertTrue(rs.getInt(1) > 0);
            assertTrue(rs.getInt(2) == portalQuotaId);
            assertTrue(rs.getString(3).equals(instanceId));
            assertTrue(rs.getDouble(4) == quantity);
            assertTrue(rs.getTimestamp(5) == null);
            if (rs.getString(6).equals(QuotaServiceManager.ACTIVE)) {
                indexActive++;
            } else if (rs.getString(6).equals(QuotaServiceManager.INACTIVE)) {
                indexArchived++;
            } else if (rs.getString(6).equals(QuotaServiceManager.DELETED)) {
                indexDeleted++;
            }
            index++;
        }
        rs.close();
        assertTrue(index == 3);
        assertTrue(indexActive == 1);
        assertTrue(indexArchived == 1);
        assertTrue(indexDeleted == 1);
        //check the usages in the remote the book keeper server to make sure we don't have those
        // usages.
        List<Usage> usages = null;
        boolean notFound = false;
        int times = 0;
        while (times < maxAttempt) {
            try {
                usages = BookKeeperClient.getInstance().listUsages(portalQuotaId, instanceId);
            } catch (NotFound e) {
                notFound = true;
            }
            if (!notFound) {
                Thread.sleep(2000);
                times++;//The usage in the remote server hasn't been deleted, continue to try
                // until it reaches the max attempt.
            } else {
                break;
            }
        }
        assertTrue(notFound == true);
        Thread.sleep(1000);
        //Start to run another thread to report those usages to the remote server.
        ExecutorService executor = Executors.newFixedThreadPool(2);
        Thread thread =
            new Thread(new FailedReportingAttemptChecker(executor, BookKeeperClient.getInstance()));
        thread.start();
        //waiting until it is done
        times = 0;
        while (thread.isAlive() && times < maxAttempt) {
            Thread.sleep(1000);
            times++;
        }
        //check the three records in the local database already have the reported date
        times = 0;
        boolean reportIsDone = false;
        while (times < maxAttempt) {
            rs = QuotaDBManagerIT.getResultSet(portalQuotaId, instanceId);
            //check local database to see if we have those records
            int number = 0;
            while (rs.next()) {
                if (rs.getTimestamp(5) != null) {
                    number++;
                }
            }
            rs.close();
            if (number == 3) {
                reportIsDone = true;
                break;//all three usages are reported to the remote book keeper server.
            } else {
                //maybe the process hasn't done. Wait two seconds and try again. If the
                // maxAttempt times reaches, the test will fail.
                Thread.sleep(2000);
                times++;
            }
        }
        assertTrue(reportIsDone == true);//make sure to check this variable.
        //it is ready check local database to see if we have those records
        rs = QuotaDBManagerIT.getResultSet(portalQuotaId, instanceId);
        index = 0;
        indexActive = 0;
        indexArchived = 0;
        indexDeleted = 0;
        while (rs.next()) {
            assertTrue(rs.getInt(1) > 0);
            assertTrue(rs.getInt(2) == portalQuotaId);
            assertTrue(rs.getString(3).equals(instanceId));
            assertTrue(rs.getDouble(4) == quantity);
            assertTrue(rs.getTimestamp(5) != null);
            if (rs.getString(6).equals(QuotaServiceManager.ACTIVE)) {
                indexActive++;
            } else if (rs.getString(6).equals(QuotaServiceManager.INACTIVE)) {
                indexArchived++;
            } else if (rs.getString(6).equals(QuotaServiceManager.DELETED)) {
                indexDeleted++;
            }
            index++;
        }
        rs.close();
        assertTrue(index == 3);
        assertTrue(indexActive == 1);
        assertTrue(indexArchived == 1);
        assertTrue(indexDeleted == 1);

        //now the remote usages should be deleted
        notFound = false;
        times = 0;
        while (times < maxAttempt) {
            try {
                usages = BookKeeperClient.getInstance().listUsages(portalQuotaId, instanceId);
            } catch (NotFound e) {
                notFound = true;
            }
            if (!notFound) {
                Thread.sleep(2000);
                times++;//The usage in the remote server hasn't been deleted, continue to try
                // until it reaches the max attempt.
            } else {
                break;
            }
        }
        assertTrue(notFound == true);
    }

    /**
     * Test the run method in the FailedReportingAttemptCheck class with two status - active and
     * archived.
     * @throws Exception
     */
    @Test
    public void testFailedReportingAttemptChecker_Run2() throws Exception {
        if (!QuotaServiceManager.getInstance().isEnabled()) {
            return;
        }
        List<Quota> quotas = BookKeeperClient.getInstance()
            .listQuotas(SUBSCRIBER, REQUESTOR, QuotaTypeDeterminer.PORTAL);
        assertTrue(quotas.size() >= 1);
        int portalQuotaId = quotas.get(0).getId();
        //Create two usages without reported date locally
        Date now = null;
        double quantity = 1;
        String instanceId = generateUUID();
        Usage usage = createUsage(portalQuotaId, instanceId, quantity, QuotaServiceManager.ACTIVE);
        QuotaDBManager.createUsage(usage, now);
        usage.setStatus(QuotaServiceManager.INACTIVE);
        QuotaDBManager.createUsage(usage, now);
        //check local database to see if we have those records
        ResultSet rs = QuotaDBManagerIT.getResultSet(portalQuotaId, instanceId);
        int index = 0;
        int indexActive = 0;
        int indexArchived = 0;
        while (rs.next()) {
            assertTrue(rs.getInt(1) > 0);
            assertTrue(rs.getInt(2) == portalQuotaId);
            assertTrue(rs.getString(3).equals(instanceId));
            assertTrue(rs.getDouble(4) == quantity);
            assertTrue(rs.getTimestamp(5) == null);
            if (rs.getString(6).equals(QuotaServiceManager.ACTIVE)) {
                indexActive++;
            } else if (rs.getString(6).equals(QuotaServiceManager.INACTIVE)) {
                indexArchived++;
            }
            index++;
        }
        rs.close();
        assertTrue(index == 2);
        assertTrue(indexActive == 1);
        assertTrue(indexArchived == 1);
        //check the usages in the remote the book keeper server to make sure we don't have those
        // usages.
        List<Usage> usages = null;
        boolean notFound = false;
        int times = 0;
        while (times < maxAttempt) {
            try {
                usages = BookKeeperClient.getInstance().listUsages(portalQuotaId, instanceId);
            } catch (NotFound e) {
                notFound = true;
            }
            if (!notFound) {
                Thread.sleep(2000);
                times++;//The usage in the remote server hasn't been deleted, continue to try
                // until it reaches the max attempt.
            } else {
                break;
            }
        }
        assertTrue(notFound == true);

        //Start to run another thread to report those usages to the remote server.
        ExecutorService executor = Executors.newFixedThreadPool(2);
        Thread thread =
            new Thread(new FailedReportingAttemptChecker(executor, BookKeeperClient.getInstance()));
        thread.start();
        //waiting until it is done
        times = 0;
        while (thread.isAlive() && times < maxAttempt) {
            Thread.sleep(1000);
            times++;
        }

        //check the three records in the local database already have the reported date
        times = 0;
        while (times < maxAttempt) {
            rs = QuotaDBManagerIT.getResultSet(portalQuotaId, instanceId);
            //check local database to see if we have those records
            index = 0;
            indexActive = 0;
            indexArchived = 0;
            try {
                while (rs.next()) {
                    assertTrue(rs.getInt(1) > 0);
                    assertTrue(rs.getInt(2) == portalQuotaId);
                    assertTrue(rs.getString(3).equals(instanceId));
                    assertTrue(rs.getDouble(4) == quantity);
                    assertTrue(rs.getTimestamp(5) != null);
                    if (rs.getString(6).equals(QuotaServiceManager.ACTIVE)) {
                        indexActive++;
                    } else if (rs.getString(6).equals(QuotaServiceManager.INACTIVE)) {
                        indexArchived++;
                    }
                    index++;
                }
                rs.close();
                break;
            } catch (Exception e) {
                //maybe the process hasn't done. Wait two seconds and try again. If the
                // maxAttempt times reaches, the test will fail.
                Thread.sleep(2000);
                times++;
            }
        }
        assertTrue(index == 2);
        assertTrue(indexActive == 1);
        assertTrue(indexArchived == 1);

        //now the remote usages should one record and its status is archived
        usages = BookKeeperClient.getInstance().listUsages(portalQuotaId, instanceId);
        assertTrue(usages.size() == 1);
        Usage returnedUsage = usages.get(0);
        assertTrue(returnedUsage.getInstanceId().equals(instanceId));
        assertTrue(returnedUsage.getStatus().equals(QuotaServiceManager.INACTIVE));
        assertTrue(returnedUsage.getQuotaId() == portalQuotaId);
        assertTrue(returnedUsage.getNodeId().equals(nodeId));
    }


    /*************************************************************
     * Test the QuotaServiceManager class
     *************************************************************/
    /**
     * Test the enforce method in the QuotaServiceManager class with three actions - create,
     * archive and delete
     * @throws Exception
     */
    @Test
    public void testQuotaServiceManagerQuotaEnforce() throws Exception {
        if (!QuotaServiceManager.getInstance().isEnabled()) {
            return;
        }
        //Test to enforce the portal quota service
        Identifier guid = new Identifier();
        guid.setValue("testPortal." + System.currentTimeMillis());
        InputStream object = new FileInputStream(portalFilePath);
        Subject submitter = new Subject();
        submitter.setValue(REQUESTOR);
        SystemMetadata sysmeta = d1NodeTest.createSystemMetadata(guid, submitter, object);
        ObjectFormatIdentifier formatId = new ObjectFormatIdentifier();
        formatId.setValue("https://purl.dataone.org/portals-1.0.0");
        sysmeta.setFormatId(formatId);
        String sidStr = generateUUID();
        Identifier sid = new Identifier();
        sid.setValue(sidStr);
        sysmeta.setSeriesId(sid);
        object.close();
        SystemMetadataManager.lock(guid);
        SystemMetadataManager.getInstance().store(sysmeta);
        SystemMetadataManager.unLock(guid);
        //Check if we have enough portal quota space in the remote server
        List<Quota> quotas = BookKeeperClient.getInstance()
            .listQuotas(SUBSCRIBER, REQUESTOR, QuotaTypeDeterminer.PORTAL);
        int quotaId = 0;
        double originalUsages = 0;
        for (Quota quota : quotas) {
            if (quota.getHardLimit() >= 1) {
                quotaId = quota.getId();
                Double originalUsagesObj = quota.getTotalUsage();
                if (originalUsagesObj != null) {
                    originalUsages = originalUsagesObj.doubleValue();
                }
                break;
            }
        }

        if (quotaId > 0) {
            //Successfully use one from the quota
            QuotaServiceManager.getInstance()
                .enforce(SUBSCRIBER, submitter, sysmeta, QuotaServiceManager.CREATEMETHOD);
            //local and remote server has a reord for the usage.
            ResultSet rs = null;
            int index = 0;
            int indexActive = 0;
            int times = 0;
            while (times < maxAttempt) {
                rs = QuotaDBManagerIT.getResultSet(quotaId, sidStr);
                //check local database to see if we have those records
                index = 0;
                indexActive = 0;
                if (rs.next()) {
                    if (rs.getTimestamp(5) == null) {
                        //make sure the usages has been reported to the remote server
                        Thread.sleep(2000);
                        times++;
                        continue;
                    }
                    assertTrue(rs.getInt(1) > 0);
                    assertTrue(rs.getInt(2) == quotaId);
                    assertTrue(rs.getString(3).equals(sidStr));
                    assertTrue(rs.getDouble(4) == 1);
                    if (rs.getString(6).equals(QuotaServiceManager.ACTIVE)) {
                        indexActive++;
                    }
                    index++;
                    rs.close();
                    break;
                } else {
                    //maybe the process hasn't done. Wait two seconds and try again. If the
                    // maxAttempt times reaches, the test will fail.
                    Thread.sleep(2000);
                    times++;
                }
            }
            assertTrue(index == 1);
            assertTrue(indexActive == 1);
            List<Usage> usages = BookKeeperClient.getInstance().listUsages(quotaId, sidStr);
            assertTrue(usages.size() == 1);
            Usage returnedUsage = usages.get(0);
            assertTrue(returnedUsage.getInstanceId().equals(sidStr));
            assertTrue(returnedUsage.getStatus().equals(QuotaServiceManager.ACTIVE));
            assertTrue(returnedUsage.getQuotaId() == quotaId);
            assertTrue(returnedUsage.getNodeId().equals(nodeId));
            //test the getRemouteUsageId method when there is a local cached copy
            int remoteId = BookKeeperClient.getInstance().getRemoteUsageId(quotaId, sidStr);
            assertTrue(remoteId == returnedUsage.getId());

            quotas = BookKeeperClient.getInstance()
                .listQuotas(SUBSCRIBER, REQUESTOR, QuotaTypeDeterminer.PORTAL);
            double newUsages = 0;
            for (Quota quota : quotas) {
                if (quota.getId() == quotaId) {
                    Double newUsagesObj = quota.getTotalUsage();
                    if (newUsagesObj != null) {
                        newUsages = newUsagesObj.doubleValue();
                    }
                    break;
                }
            }
            System.out.println("+++++++++++++++the new usages is " + newUsages);
            System.out.println("+++++++++++++++the original usages is " + originalUsages);
            assertTrue((originalUsages + 1) == newUsages);//we should add a new usage

            //archiving the chain will not release one from quota
            QuotaServiceManager.getInstance()
                .enforce(SUBSCRIBER, submitter, sysmeta, QuotaServiceManager.ARCHIVEMETHOD);
            //local should have two usages and remote only have one usage with the archived status.
            int indexArchived = 0;
            times = 0;
            while (times < maxAttempt) {
                rs = QuotaDBManagerIT.getResultSet(quotaId, sidStr);
                //check local database to see if we have those records
                index = 0;
                indexActive = 0;
                indexArchived = 0;
                while (rs.next()) {
                    if (rs.getTimestamp(5) == null) {
                        //make sure the usages has been reported to the remote server
                        Thread.sleep(2000);
                        times++;
                        break;
                    }
                    assertTrue(rs.getInt(1) > 0);
                    assertTrue(rs.getInt(2) == quotaId);
                    assertTrue(rs.getString(3).equals(sidStr));
                    assertTrue(rs.getDouble(4) == 1);
                    if (rs.getString(6).equals(QuotaServiceManager.ACTIVE)) {
                        indexActive++;
                    } else if (rs.getString(6).equals(QuotaServiceManager.INACTIVE)) {
                        indexArchived++;
                    }
                    index++;
                }
                rs.close();
                if (index != 2) {
                    Thread.sleep(2000);
                    times++;
                    continue;
                }
                break;
            }
            assertTrue(index == 2);
            assertTrue(indexActive == 1);
            assertTrue(indexArchived == 1);
            usages = BookKeeperClient.getInstance().listUsages(quotaId, sidStr);
            assertTrue(usages.size() == 1);
            returnedUsage = usages.get(0);
            assertTrue(returnedUsage.getInstanceId().equals(sidStr));
            assertTrue(returnedUsage.getStatus().equals(QuotaServiceManager.INACTIVE));
            assertTrue(returnedUsage.getQuotaId() == quotaId);
            assertTrue(returnedUsage.getNodeId().equals(nodeId));
            quotas = BookKeeperClient.getInstance()
                .listQuotas(SUBSCRIBER, REQUESTOR, QuotaTypeDeterminer.PORTAL);
            newUsages = 0;
            for (Quota quota : quotas) {
                if (quota.getId() == quotaId) {
                    Double newUsagesObj = quota.getTotalUsage();
                    if (newUsagesObj != null) {
                        newUsages = newUsagesObj.doubleValue();
                    }
                    break;
                }
            }
            System.out.println("+++++++++++++++the new usages is " + newUsages);
            System.out.println("+++++++++++++++the original usages is " + originalUsages);
            assertTrue(newUsages == originalUsages);//usages decrease

            //delete the chain and the quota account will increase 1
            QuotaServiceManager.getInstance()
                .enforce(SUBSCRIBER, submitter, sysmeta, QuotaServiceManager.DELETEMETHOD);
            //local should have two usages and remote only have one usage with the archived status.
            int indexDeleted = 0;
            times = 0;
            while (times < maxAttempt) {
                rs = QuotaDBManagerIT.getResultSet(quotaId, sidStr);
                //check local database to see if we have those records
                index = 0;
                indexActive = 0;
                indexArchived = 0;
                indexDeleted = 0;
                while (rs.next()) {
                    //make sure the usages has been reported to the remote server
                    if (rs.getTimestamp(5) == null) {
                        Thread.sleep(2000);
                        times++;
                        break;
                    }
                    assertTrue(rs.getInt(1) > 0);
                    assertTrue(rs.getInt(2) == quotaId);
                    assertTrue(rs.getString(3).equals(sidStr));
                    assertTrue(rs.getDouble(4) == 1);
                    if (rs.getString(6).equals(QuotaServiceManager.ACTIVE)) {
                        indexActive++;
                    } else if (rs.getString(6).equals(QuotaServiceManager.INACTIVE)) {
                        indexArchived++;
                    } else if (rs.getString(6).equals(QuotaServiceManager.DELETED)) {
                        indexDeleted++;
                    }
                    index++;
                }
                if (index != 3) {
                    Thread.sleep(2000);
                    times++;
                    continue;
                }
                rs.close();
                break;

            }
            assertTrue(index == 3);
            assertTrue(indexActive == 1);
            assertTrue(indexArchived == 1);
            assertTrue(indexDeleted == 1);
            boolean notFound = false;
            times = 0;
            while (times < maxAttempt) {
                try {
                    usages = BookKeeperClient.getInstance().listUsages(quotaId, sidStr);
                } catch (NotFound e) {
                    notFound = true;
                }
                if (!notFound) {
                    Thread.sleep(2000);
                    times++;//The usage in the remote server hasn't been deleted, continue to try
                    // until it reaches the max attempt.
                } else {
                    break;
                }
            }
            assertTrue(notFound == true);
            quotas = BookKeeperClient.getInstance()
                .listQuotas(SUBSCRIBER, REQUESTOR, QuotaTypeDeterminer.PORTAL);
            newUsages = 0;
            for (Quota quota : quotas) {
                if (quota.getId() == quotaId) {
                    Double newUsagesObj = quota.getTotalUsage();
                    if (newUsagesObj != null) {
                        newUsages = newUsagesObj.doubleValue();
                    }
                    break;
                }
            }
            System.out.println("+++++++++++++++the new usages is " + newUsages);
            System.out.println("+++++++++++++++the original usages is " + originalUsages);
            assertTrue(newUsages
                           == originalUsages);//deleting an archived usages should not change the
            // usage.
        } else {
            //couldn't find a quota id with enough quota
            try {
                QuotaServiceManager.getInstance()
                    .enforce(SUBSCRIBER, submitter, sysmeta, QuotaServiceManager.CREATEMETHOD);
                fail("Test can't get here since the user doesn't have enough quota");
            } catch (InsufficientResources e) {
                assertTrue(true);
            }
        }
    }


    /**
     * Test the enforce method in the QuotaServiceManager class with two actions - create and delete
     * @throws Exception
     */
    @Test
    public void testQuotaServiceManagerQuotaEnforce2() throws Exception {
        if (!QuotaServiceManager.getInstance().isEnabled()) {
            return;
        }
        //Test to enforce the portal quota service
        Identifier guid = new Identifier();
        guid.setValue("testPortal." + System.currentTimeMillis());
        InputStream object = new FileInputStream(portalFilePath);
        Subject submitter = new Subject();
        submitter.setValue(REQUESTOR);
        SystemMetadata sysmeta = d1NodeTest.createSystemMetadata(guid, submitter, object);
        ObjectFormatIdentifier formatId = new ObjectFormatIdentifier();
        formatId.setValue("https://purl.dataone.org/portals-1.0.0");
        sysmeta.setFormatId(formatId);
        String sidStr = generateUUID();
        Identifier sid = new Identifier();
        sid.setValue(sidStr);
        sysmeta.setSeriesId(sid);
        object.close();
        SystemMetadataManager.lock(guid);
        SystemMetadataManager.getInstance().store(sysmeta);
        SystemMetadataManager.unLock(guid);
        //Check if we have enough portal quota space in the remote server
        List<Quota> quotas = BookKeeperClient.getInstance()
            .listQuotas(SUBSCRIBER, REQUESTOR, QuotaTypeDeterminer.PORTAL);
        int quotaId = 0;
        double originalUsages = 0;
        for (Quota quota : quotas) {
            if (quota.getHardLimit() >= 1) {
                quotaId = quota.getId();
                Double originalUsagesObj = quota.getTotalUsage();
                if (originalUsagesObj != null) {
                    originalUsages = originalUsagesObj.doubleValue();
                }
                break;
            }
        }

        if (quotaId > 0) {
            //Successfully use one from the quota
            QuotaServiceManager.getInstance()
                .enforce(SUBSCRIBER, submitter, sysmeta, QuotaServiceManager.CREATEMETHOD);
            //local and remote server has a record for the usage.
            ResultSet rs = null;
            int index = 0;
            int indexActive = 0;
            int times = 0;
            while (times < maxAttempt) {
                rs = QuotaDBManagerIT.getResultSet(quotaId, sidStr);
                //check local database to see if we have those records
                index = 0;
                indexActive = 0;
                if (rs.next()) {
                    if (rs.getTimestamp(5) == null) {
                        //make sure the usages has been reported to the remote server
                        Thread.sleep(2000);
                        times++;
                        continue;
                    }
                    assertTrue(rs.getInt(1) > 0);
                    assertTrue(rs.getInt(2) == quotaId);
                    assertTrue(rs.getString(3).equals(sidStr));
                    assertTrue(rs.getDouble(4) == 1);
                    if (rs.getString(6).equals(QuotaServiceManager.ACTIVE)) {
                        indexActive++;
                    }
                    index++;
                    rs.close();
                    break;
                } else {
                    //maybe the process hasn't done. Wait two seconds and try again. If the
                    // maxAttempt times reaches, the test will fail.
                    Thread.sleep(2000);
                    times++;
                }
            }
            assertTrue(index == 1);
            assertTrue(indexActive == 1);
            List<Usage> usages = BookKeeperClient.getInstance().listUsages(quotaId, sidStr);
            assertTrue(usages.size() == 1);
            Usage returnedUsage = usages.get(0);
            assertTrue(returnedUsage.getInstanceId().equals(sidStr));
            assertTrue(returnedUsage.getStatus().equals(QuotaServiceManager.ACTIVE));
            assertTrue(returnedUsage.getQuotaId() == quotaId);
            assertTrue(returnedUsage.getNodeId().equals(nodeId));
            quotas = BookKeeperClient.getInstance()
                .listQuotas(SUBSCRIBER, REQUESTOR, QuotaTypeDeterminer.PORTAL);
            double newUsages = 0;
            for (Quota quota : quotas) {
                if (quota.getId() == quotaId) {
                    Double newUsagesObj = quota.getTotalUsage();
                    if (newUsagesObj != null) {
                        newUsages = newUsagesObj.doubleValue();
                    }
                    break;
                }
            }
            System.out.println("+++++++++++++++the new usages is " + newUsages);
            System.out.println("+++++++++++++++the original usages is " + originalUsages);
            assertTrue((originalUsages + 1) == newUsages);//we should add a new usage


            //delete the chain and the quota account will increase 1
            QuotaServiceManager.getInstance()
                .enforce(SUBSCRIBER, submitter, sysmeta, QuotaServiceManager.DELETEMETHOD);
            //local should have two usages and remote doesn't have any.
            int indexDeleted = 0;
            times = 0;
            while (times < maxAttempt) {
                rs = QuotaDBManagerIT.getResultSet(quotaId, sidStr);
                //check local database to see if we have those records
                index = 0;
                indexActive = 0;
                indexDeleted = 0;
                while (rs.next()) {
                    //make sure the usages has been reported to the remote server
                    if (rs.getTimestamp(5) == null) {
                        Thread.sleep(2000);
                        times++;
                        break;
                    }
                    assertTrue(rs.getInt(1) > 0);
                    assertTrue(rs.getInt(2) == quotaId);
                    assertTrue(rs.getString(3).equals(sidStr));
                    assertTrue(rs.getDouble(4) == 1);
                    if (rs.getString(6).equals(QuotaServiceManager.ACTIVE)) {
                        indexActive++;
                    } else if (rs.getString(6).equals(QuotaServiceManager.DELETED)) {
                        indexDeleted++;
                    }
                    index++;
                }
                if (index != 2) {
                    Thread.sleep(2000);
                    times++;
                    continue;
                }
                rs.close();
                break;

            }
            assertTrue(index == 2);
            assertTrue(indexActive == 1);
            assertTrue(indexDeleted == 1);
            boolean notFound = false;
            times = 0;
            while (times < maxAttempt) {
                try {
                    usages = BookKeeperClient.getInstance().listUsages(quotaId, sidStr);
                } catch (NotFound e) {
                    notFound = true;
                }
                if (!notFound) {
                    Thread.sleep(2000);
                    times++;//The usage in the remote server hasn't been deleted, continue to try
                    // until it reaches the max attempt.
                } else {
                    break;
                }
            }
            assertTrue(notFound == true);
            quotas = BookKeeperClient.getInstance()
                .listQuotas(SUBSCRIBER, REQUESTOR, QuotaTypeDeterminer.PORTAL);
            newUsages = 0;
            for (Quota quota : quotas) {
                if (quota.getId() == quotaId) {
                    Double newUsagesObj = quota.getTotalUsage();
                    if (newUsagesObj != null) {
                        newUsages = newUsagesObj.doubleValue();
                    }
                    break;
                }
            }
            System.out.println("+++++++++++++++the new usages is " + newUsages);
            System.out.println("+++++++++++++++the original usages is " + originalUsages);
            assertTrue(newUsages == originalUsages);//deleting a usage should make it back
        } else {
            //couldn't find a quota id with enough quota
            try {
                QuotaServiceManager.getInstance()
                    .enforce(SUBSCRIBER, submitter, sysmeta, QuotaServiceManager.CREATEMETHOD);
                fail("Test can't get here since the user doesn't have enough quota");
            } catch (InsufficientResources e) {
                assertTrue(true);
            }
        }
    }


    /*************************************************************
     * Test the API method from the MNodeService class
     *************************************************************/
    /**
     * Test the create, update and archive methods in MNService when portal quota is enabled.
     * @throws Exception
     */
    @Test
    public void testMNodeMethodWithPortalQuota() throws Exception {
        if (!QuotaServiceManager.getInstance().isEnabled()) {
            return;
        }
        //Check if we have enough portal quota space in the remote server
        List<Quota> quotas = BookKeeperClient.getInstance()
            .listQuotas(SUBSCRIBER, REQUESTOR, QuotaTypeDeterminer.PORTAL);
        request.setHeader(QuotaServiceManager.QUOTASUBJECTHEADER, SUBSCRIBER);
        int quotaId = 0;
        double originalUsages = 0;
        for (Quota quota : quotas) {
            if (quota.getHardLimit() >= 1) {
                quotaId = quota.getId();
                Double originalUsagesObj = quota.getTotalUsage();
                if (originalUsagesObj != null) {
                    originalUsages = originalUsagesObj.doubleValue();
                }
                break;
            }
        }

        if (quotaId > 0) {
            /*********************************************************************
             *First portal object chain. It will create, update, archive.
             **********************************************************************/
            //create a portal object
            Identifier guid = new Identifier();
            guid.setValue("testMNodeMethodWithPortalQuota1." + System.currentTimeMillis());
            InputStream object = new FileInputStream(portalFilePath);
            Subject submitter = new Subject();
            submitter.setValue(REQUESTOR);
            Session session = new Session();
            session.setSubject(submitter);
            SystemMetadata sysmeta = d1NodeTest.createSystemMetadata(guid, submitter, object);
            ObjectFormatIdentifier formatId = new ObjectFormatIdentifier();
            formatId.setValue("https://purl.dataone.org/portals-1.0.0");
            sysmeta.setFormatId(formatId);
            String sidStr = generateUUID();
            Identifier sid = new Identifier();
            sid.setValue(sidStr);
            sysmeta.setSeriesId(sid);
            object.close();
            object = new FileInputStream(portalFilePath);
            Identifier pid =
                MNodeService.getInstance(request).create(session, guid, object, sysmeta);

            //local and remote server has a record for the usage.
            ResultSet rs = null;
            int index = 0;
            int indexActive = 0;
            int times = 0;
            while (times < maxAttempt) {
                rs = QuotaDBManagerIT.getResultSet(quotaId, sidStr);
                //check local database to see if we have those records
                index = 0;
                indexActive = 0;
                if (rs.next()) {
                    if (rs.getTimestamp(5) == null) {
                        //make sure the usages has been reported to the remote server
                        Thread.sleep(2000);
                        times++;
                        continue;
                    }
                    assertTrue(rs.getInt(1) > 0);
                    assertTrue(rs.getInt(2) == quotaId);
                    assertTrue(rs.getString(3).equals(sidStr));
                    assertTrue(rs.getDouble(4) == 1);
                    if (rs.getString(6).equals(QuotaServiceManager.ACTIVE)) {
                        indexActive++;
                    }
                    index++;
                    rs.close();
                    break;
                } else {
                    //maybe the process hasn't done. Wait two seconds and try again. If the
                    // maxAttempt times reaches, the test will fail.
                    Thread.sleep(2000);
                    times++;
                }
            }
            assertTrue(index == 1);
            assertTrue(indexActive == 1);
            List<Usage> usages = BookKeeperClient.getInstance().listUsages(quotaId, sidStr);
            assertTrue(usages.size() == 1);
            Usage returnedUsage = usages.get(0);
            assertTrue(returnedUsage.getInstanceId().equals(sidStr));
            assertTrue(returnedUsage.getStatus().equals(QuotaServiceManager.ACTIVE));
            assertTrue(returnedUsage.getQuotaId() == quotaId);
            assertTrue(returnedUsage.getNodeId().equals(nodeId));
            quotas = BookKeeperClient.getInstance()
                .listQuotas(SUBSCRIBER, REQUESTOR, QuotaTypeDeterminer.PORTAL);
            double newUsages = 0;
            for (Quota quota : quotas) {
                if (quota.getId() == quotaId) {
                    Double newUsagesObj = quota.getTotalUsage();
                    if (newUsagesObj != null) {
                        newUsages = newUsagesObj.doubleValue();
                    }
                    break;
                }
            }
            System.out.println("+++++++++++++++the new usages is " + newUsages);
            System.out.println("+++++++++++++++the original usages is " + originalUsages);
            assertTrue((originalUsages + 1) == newUsages);//we should add a new usage


            //Update the portal object. It wouldn't change anything in the quota usage.
            Identifier guid2 = new Identifier();
            guid2.setValue("testMNodeMethodWithPortalQuota2." + System.currentTimeMillis());
            object = new FileInputStream(portalFilePath);
            sysmeta = d1NodeTest.createSystemMetadata(guid2, submitter, object);
            sysmeta.setFormatId(formatId);
            sysmeta.setSeriesId(sid);
            object.close();
            object = new FileInputStream(portalFilePath);
            MNodeService.getInstance(request).update(session, guid, object, guid2, sysmeta);
            //local and remote server still has a record for the usage
            index = 0;
            indexActive = 0;
            times = 0;
            while (times < maxAttempt) {
                rs = QuotaDBManagerIT.getResultSet(quotaId, sidStr);
                //check local database to see if we have those records
                index = 0;
                indexActive = 0;
                if (rs.next()) {
                    if (rs.getTimestamp(5) == null) {
                        //make sure the usages has been reported to the remote server
                        Thread.sleep(2000);
                        times++;
                        continue;
                    }
                    assertTrue(rs.getInt(1) > 0);
                    assertTrue(rs.getInt(2) == quotaId);
                    assertTrue(rs.getString(3).equals(sidStr));
                    assertTrue(rs.getDouble(4) == 1);
                    if (rs.getString(6).equals(QuotaServiceManager.ACTIVE)) {
                        indexActive++;
                    }
                    index++;
                    rs.close();
                    break;
                } else {
                    //maybe the process hasn't done. Wait two seconds and try again. If the
                    // maxAttempt times reaches, the test will fail.
                    Thread.sleep(2000);
                    times++;
                }
            }
            assertTrue(index == 1);
            assertTrue(indexActive == 1);
            usages = BookKeeperClient.getInstance().listUsages(quotaId, sidStr);
            assertTrue(usages.size() == 1);
            returnedUsage = usages.get(0);
            assertTrue(returnedUsage.getInstanceId().equals(sidStr));
            assertTrue(returnedUsage.getStatus().equals(QuotaServiceManager.ACTIVE));
            assertTrue(returnedUsage.getQuotaId() == quotaId);
            assertTrue(returnedUsage.getNodeId().equals(nodeId));
            quotas = BookKeeperClient.getInstance()
                .listQuotas(SUBSCRIBER, REQUESTOR, QuotaTypeDeterminer.PORTAL);
            newUsages = 0;
            for (Quota quota : quotas) {
                if (quota.getId() == quotaId) {
                    Double newUsagesObj = quota.getTotalUsage();
                    if (newUsagesObj != null) {
                        newUsages = newUsagesObj.doubleValue();
                    }
                    break;
                }
            }
            System.out.println("+++++++++++++++the new usages is " + newUsages);
            System.out.println("+++++++++++++++the original usages is " + originalUsages);
            assertTrue((originalUsages + 1) == newUsages);//it doesn't change anything

            //archive the first pid in the series chain. It wouldn't change anything in the quota
            // usage.
            MNodeService.getInstance(request).archive(session, guid);
            //local and remote server still has a record for the usage
            index = 0;
            indexActive = 0;
            times = 0;
            while (times < maxAttempt) {
                rs = QuotaDBManagerIT.getResultSet(quotaId, sidStr);
                //check local database to see if we have those records
                index = 0;
                indexActive = 0;
                if (rs.next()) {
                    if (rs.getTimestamp(5) == null) {
                        //make sure the usages has been reported to the remote server
                        Thread.sleep(2000);
                        times++;
                        continue;
                    }
                    assertTrue(rs.getInt(1) > 0);
                    assertTrue(rs.getInt(2) == quotaId);
                    assertTrue(rs.getString(3).equals(sidStr));
                    assertTrue(rs.getDouble(4) == 1);
                    if (rs.getString(6).equals(QuotaServiceManager.ACTIVE)) {
                        indexActive++;
                    }
                    index++;
                    rs.close();
                    break;
                } else {
                    //maybe the process hasn't done. Wait two seconds and try again. If the
                    // maxAttempt times reaches, the test will fail.
                    Thread.sleep(2000);
                    times++;
                }
            }
            assertTrue(index == 1);
            assertTrue(indexActive == 1);
            usages = BookKeeperClient.getInstance().listUsages(quotaId, sidStr);
            assertTrue(usages.size() == 1);
            returnedUsage = usages.get(0);
            assertTrue(returnedUsage.getInstanceId().equals(sidStr));
            assertTrue(returnedUsage.getStatus().equals(QuotaServiceManager.ACTIVE));
            assertTrue(returnedUsage.getQuotaId() == quotaId);
            assertTrue(returnedUsage.getNodeId().equals(nodeId));
            quotas = BookKeeperClient.getInstance()
                .listQuotas(SUBSCRIBER, REQUESTOR, QuotaTypeDeterminer.PORTAL);
            newUsages = 0;
            for (Quota quota : quotas) {
                if (quota.getId() == quotaId) {
                    Double newUsagesObj = quota.getTotalUsage();
                    if (newUsagesObj != null) {
                        newUsages = newUsagesObj.doubleValue();
                    }
                    break;
                }
            }
            System.out.println("+++++++++++++++the new usages is " + newUsages);
            System.out.println("+++++++++++++++the original usages is " + originalUsages);
            assertTrue((originalUsages + 1) == newUsages);//it doesn't change anything

            //Archive the second object in the series chain. Since the whole chain are archived,
            // the local will have two records - one for active and one for archive.
            //Remote the usage will have one record with status archive. The total usage will
            // decrease one
            Thread.sleep(2000);
            MNodeService.getInstance(request).archive(session, guid2);
            //local and remote server still has a record for the usage
            index = 0;
            indexActive = 0;
            int indexArchived = 0;
            times = 0;
            while (times < maxAttempt) {
                rs = QuotaDBManagerIT.getResultSet(quotaId, sidStr);
                //check local database to see if we have those records
                index = 0;
                indexActive = 0;
                while (rs.next()) {
                    if (rs.getTimestamp(5) == null) {
                        //make sure the usages has been reported to the remote server
                        Thread.sleep(2000);
                        times++;
                        continue;
                    }
                    assertTrue(rs.getInt(1) > 0);
                    assertTrue(rs.getInt(2) == quotaId);
                    assertTrue(rs.getString(3).equals(sidStr));
                    assertTrue(rs.getDouble(4) == 1);
                    assertTrue(rs.getTimestamp(5) != null);
                    if (rs.getString(6).equals(QuotaServiceManager.ACTIVE)) {
                        indexActive++;
                    } else if (rs.getString(6).equals(QuotaServiceManager.INACTIVE)) {
                        indexArchived++;
                    }
                    index++;
                }
                rs.close();
                if (index != 2) {
                    Thread.sleep(2000);
                    times++;
                    continue;
                }
                break;
            }
            assertTrue(index == 2);
            assertTrue(indexActive == 1);
            assertTrue(indexArchived == 1);
            //check the remote usage
            usages = BookKeeperClient.getInstance().listUsages(quotaId, sidStr);
            assertTrue(usages.size() == 1);
            returnedUsage = usages.get(0);
            assertTrue(returnedUsage.getInstanceId().equals(sidStr));
            assertTrue(returnedUsage.getStatus().equals(QuotaServiceManager.INACTIVE));
            assertTrue(returnedUsage.getQuotaId() == quotaId);
            assertTrue(returnedUsage.getNodeId().equals(nodeId));
            //check the quota
            quotas = BookKeeperClient.getInstance()
                .listQuotas(SUBSCRIBER, REQUESTOR, QuotaTypeDeterminer.PORTAL);
            newUsages = 0;
            for (Quota quota : quotas) {
                if (quota.getId() == quotaId) {
                    Double newUsagesObj = quota.getTotalUsage();
                    if (newUsagesObj != null) {
                        newUsages = newUsagesObj.doubleValue();
                    }
                    break;
                }
            }
            System.out.println("+++++++++++++++the new usages is " + newUsages);
            System.out.println("+++++++++++++++the original usages is " + originalUsages);
            assertTrue(originalUsages == newUsages);//it doesn't change anything

            /*********************************************************************
             *Another portal object chain. It will create, update, and updateSystemMetadata
             **********************************************************************/
            Identifier guid3 = new Identifier();
            guid3.setValue("testMNodeMethodWithPortalQuota3." + System.currentTimeMillis());
            object = new FileInputStream(portalFilePath);
            sysmeta = d1NodeTest.createSystemMetadata(guid3, submitter, object);
            sysmeta.setFormatId(formatId);
            String sidStr2 = generateUUID();
            sid.setValue(sidStr2);
            sysmeta.setSeriesId(sid);
            object.close();
            object = new FileInputStream(portalFilePath);
            MNodeService.getInstance(request).create(session, guid3, object, sysmeta);

            //update
            Thread.sleep(2000);
            Identifier guid4 = new Identifier();
            guid4.setValue("testMNodeMethodWithPortalQuota4." + System.currentTimeMillis());
            object = new FileInputStream(portalFilePath);
            sysmeta = d1NodeTest.createSystemMetadata(guid4, submitter, object);
            sysmeta.setFormatId(formatId);
            sid.setValue(sidStr2);
            sysmeta.setSeriesId(sid);
            object.close();
            object = new FileInputStream(portalFilePath);
            MNodeService.getInstance(request).update(session, guid3, object, guid4, sysmeta);

            Thread.sleep(2000);
            //updateSystemMetadata to set the archive true. The quota and usages would not change
            SystemMetadata returnedSysmeta =
                MNodeService.getInstance(request).getSystemMetadata(session, guid3);
            returnedSysmeta.setArchived(true);
            MNodeService.getInstance(request).updateSystemMetadata(session, guid3, returnedSysmeta);

            //local and remote server still has a record for the usage
            index = 0;
            indexActive = 0;
            times = 0;
            while (times < maxAttempt) {
                rs = QuotaDBManagerIT.getResultSet(quotaId, sidStr2);
                //check local database to see if we have those records
                index = 0;
                indexActive = 0;
                if (rs.next()) {
                    if (rs.getTimestamp(5) == null) {
                        //make sure the usages has been reported to the remote server
                        Thread.sleep(2000);
                        times++;
                        continue;
                    }
                    assertTrue(rs.getInt(1) > 0);
                    assertTrue(rs.getInt(2) == quotaId);
                    assertTrue(rs.getString(3).equals(sidStr2));
                    assertTrue(rs.getDouble(4) == 1);
                    if (rs.getString(6).equals(QuotaServiceManager.ACTIVE)) {
                        indexActive++;
                    }
                    index++;
                    rs.close();
                    break;
                } else {
                    //maybe the process hasn't done. Wait two seconds and try again. If the
                    // maxAttempt times reaches, the test will fail.
                    Thread.sleep(2000);
                    times++;
                }
            }
            assertTrue(index == 1);
            assertTrue(indexActive == 1);
            usages = BookKeeperClient.getInstance().listUsages(quotaId, sidStr2);
            assertTrue(usages.size() == 1);
            returnedUsage = usages.get(0);
            assertTrue(returnedUsage.getInstanceId().equals(sidStr2));
            assertTrue(returnedUsage.getStatus().equals(QuotaServiceManager.ACTIVE));
            assertTrue(returnedUsage.getQuotaId() == quotaId);
            assertTrue(returnedUsage.getNodeId().equals(nodeId));
            quotas = BookKeeperClient.getInstance()
                .listQuotas(SUBSCRIBER, REQUESTOR, QuotaTypeDeterminer.PORTAL);
            newUsages = 0;
            for (Quota quota : quotas) {
                if (quota.getId() == quotaId) {
                    Double newUsagesObj = quota.getTotalUsage();
                    if (newUsagesObj != null) {
                        newUsages = newUsagesObj.doubleValue();
                    }
                    break;
                }
            }
            System.out.println("+++++++++++++++the new usages is " + newUsages);
            System.out.println("+++++++++++++++the original usages is " + originalUsages);
            assertTrue((originalUsages + 1) == newUsages);//it doesn't change anything

            //updateSystemMetadata on the second object in the chain. Since all object in the
            // chain are archived, total usages of quota will be restored
            returnedSysmeta = MNodeService.getInstance(request).getSystemMetadata(session, guid4);
            returnedSysmeta.setArchived(true);
            MNodeService.getInstance(request).updateSystemMetadata(session, guid4, returnedSysmeta);
            //local has two records (one is active and the other is archived) and remote server
            // still has one record for the usage with archived status
            index = 0;
            indexActive = 0;
            indexArchived = 0;
            times = 0;
            while (times < maxAttempt) {
                rs = QuotaDBManagerIT.getResultSet(quotaId, sidStr2);
                //check local database to see if we have those records
                index = 0;
                indexActive = 0;
                indexArchived = 0;
                while (rs.next()) {
                    if (rs.getTimestamp(5) == null) {
                        //make sure the usages has been reported to the remote server
                        Thread.sleep(2000);
                        times++;
                        continue;
                    }
                    assertTrue(rs.getInt(1) > 0);
                    assertTrue(rs.getInt(2) == quotaId);
                    assertTrue(rs.getString(3).equals(sidStr2));
                    assertTrue(rs.getDouble(4) == 1);
                    assertTrue(rs.getTimestamp(5) != null);
                    if (rs.getString(6).equals(QuotaServiceManager.ACTIVE)) {
                        indexActive++;
                    } else if (rs.getString(6).equals(QuotaServiceManager.INACTIVE)) {
                        indexArchived++;
                    }
                    index++;
                }
                rs.close();
                if (index != 2) {
                    Thread.sleep(2000);
                    times++;
                    continue;
                }
                break;
            }
            assertTrue(index == 2);
            assertTrue(indexActive == 1);
            assertTrue(indexArchived == 1);
            //check the remote usage
            usages = BookKeeperClient.getInstance().listUsages(quotaId, sidStr2);
            assertTrue(usages.size() == 1);
            returnedUsage = usages.get(0);
            assertTrue(returnedUsage.getInstanceId().equals(sidStr2));
            assertTrue(returnedUsage.getStatus().equals(QuotaServiceManager.INACTIVE));
            assertTrue(returnedUsage.getQuotaId() == quotaId);
            assertTrue(returnedUsage.getNodeId().equals(nodeId));
            //check the quota
            quotas = BookKeeperClient.getInstance()
                .listQuotas(SUBSCRIBER, REQUESTOR, QuotaTypeDeterminer.PORTAL);
            newUsages = 0;
            for (Quota quota : quotas) {
                if (quota.getId() == quotaId) {
                    Double newUsagesObj = quota.getTotalUsage();
                    if (newUsagesObj != null) {
                        newUsages = newUsagesObj.doubleValue();
                    }
                    break;
                }
            }
            System.out.println("+++++++++++++++the new usages is " + newUsages);
            System.out.println("+++++++++++++++the original usages is " + originalUsages);
            assertTrue(originalUsages == newUsages);//restore the total usages.

            /*********************************************************************
             *A portal object chain. It will create, update, and delete portal objects
             **********************************************************************/
            Identifier guid5 = new Identifier();
            guid5.setValue("testMNodeMethodWithPortalQuota5." + System.currentTimeMillis());
            object = new FileInputStream(portalFilePath);
            sysmeta = d1NodeTest.createSystemMetadata(guid5, submitter, object);
            sysmeta.setFormatId(formatId);
            String sidStr3 = generateUUID();
            sid.setValue(sidStr3);
            sysmeta.setSeriesId(sid);
            object.close();
            object = new FileInputStream(portalFilePath);
            MNodeService.getInstance(request).create(session, guid5, object, sysmeta);

            //update
            Thread.sleep(2000);
            Identifier guid6 = new Identifier();
            guid6.setValue("testMNodeMethodWithPortalQuota6." + System.currentTimeMillis());
            object = new FileInputStream(portalFilePath);
            sysmeta = d1NodeTest.createSystemMetadata(guid6, submitter, object);
            sysmeta.setFormatId(formatId);
            sysmeta.setSeriesId(sid);
            object.close();
            object = new FileInputStream(portalFilePath);
            MNodeService.getInstance(request).update(session, guid5, object, guid6, sysmeta);

            //Delete the first object. The quota and usages would not change
            Thread.sleep(2000);
            Session adminsession = d1NodeTest.getCNSession();
            MNodeService.getInstance(request).delete(adminsession, guid5);
            //local and remote server still has a record for the usage
            index = 0;
            indexActive = 0;
            times = 0;
            while (times < maxAttempt) {
                rs = QuotaDBManagerIT.getResultSet(quotaId, sidStr3);
                //check local database to see if we have those records
                index = 0;
                indexActive = 0;
                while (rs.next()) {
                    if (rs.getTimestamp(5) == null) {
                        //make sure the usages has been reported to the remote server
                        Thread.sleep(2000);
                        times++;
                        continue;
                    }
                    assertTrue(rs.getInt(1) > 0);
                    assertTrue(rs.getInt(2) == quotaId);
                    assertTrue(rs.getString(3).equals(sidStr3));
                    assertTrue(rs.getDouble(4) == 1);
                    assertTrue(rs.getTimestamp(5) != null);
                    if (rs.getString(6).equals(QuotaServiceManager.ACTIVE)) {
                        indexActive++;
                    }
                    index++;
                }
                rs.close();
                break;
            }
            assertTrue(index == 1);
            assertTrue(indexActive == 1);
            usages = BookKeeperClient.getInstance().listUsages(quotaId, sidStr3);
            assertTrue(usages.size() == 1);
            returnedUsage = usages.get(0);
            assertTrue(returnedUsage.getInstanceId().equals(sidStr3));
            assertTrue(returnedUsage.getStatus().equals(QuotaServiceManager.ACTIVE));
            assertTrue(returnedUsage.getQuotaId() == quotaId);
            assertTrue(returnedUsage.getNodeId().equals(nodeId));
            //check the quota
            quotas = BookKeeperClient.getInstance()
                .listQuotas(SUBSCRIBER, REQUESTOR, QuotaTypeDeterminer.PORTAL);
            newUsages = 0;
            for (Quota quota : quotas) {
                if (quota.getId() == quotaId) {
                    Double newUsagesObj = quota.getTotalUsage();
                    if (newUsagesObj != null) {
                        newUsages = newUsagesObj.doubleValue();
                    }
                    break;
                }
            }
            System.out.println("+++++++++++++++the new usages is " + newUsages);
            System.out.println("+++++++++++++++the original usages is " + originalUsages);
            assertTrue(originalUsages + 1
                           == newUsages);//Nothing change after deleting the first object in the
            // chain

            //Delete the second object in the chain. Since all object in the chain are deleted,
            // it should restore one quota back
            MNodeService.getInstance(request).delete(adminsession, guid6);
            //local has two records (one is active and the other is deleted) and remote server
            // will not have any usage
            index = 0;
            indexActive = 0;
            int indexDeleted = 0;
            times = 0;
            while (times < maxAttempt) {
                rs = QuotaDBManagerIT.getResultSet(quotaId, sidStr3);
                //check local database to see if we have those records
                index = 0;
                indexActive = 0;
                indexArchived = 0;
                indexDeleted = 0;
                while (rs.next()) {
                    if (rs.getTimestamp(5) == null) {
                        //make sure the usages has been reported to the remote server
                        Thread.sleep(2000);
                        times++;
                        continue;
                    }
                    assertTrue(rs.getInt(1) > 0);
                    assertTrue(rs.getInt(2) == quotaId);
                    assertTrue(rs.getString(3).equals(sidStr3));
                    assertTrue(rs.getDouble(4) == 1);
                    assertTrue(rs.getTimestamp(5) != null);
                    if (rs.getString(6).equals(QuotaServiceManager.ACTIVE)) {
                        indexActive++;
                    } else if (rs.getString(6).equals(QuotaServiceManager.DELETED)) {
                        indexDeleted++;
                    }
                    index++;
                }
                rs.close();
                if (index != 2) {
                    Thread.sleep(2000);
                    times++;
                    continue;
                }
                break;
            }
            assertTrue(index == 2);
            assertTrue(indexActive == 1);
            assertTrue(indexDeleted == 1);
            boolean notFound = false;
            times = 0;
            while (times < maxAttempt) {
                try {
                    usages = BookKeeperClient.getInstance().listUsages(quotaId, sidStr3);
                } catch (NotFound e) {
                    notFound = true;
                }
                if (!notFound) {
                    Thread.sleep(2000);
                    times++;//The usage in the remote server hasn't been deleted, continue to try
                    // until it reaches the max attempt.
                } else {
                    break;
                }
            }
            assertTrue(notFound == true);
            //check the quota
            quotas = BookKeeperClient.getInstance()
                .listQuotas(SUBSCRIBER, REQUESTOR, QuotaTypeDeterminer.PORTAL);
            newUsages = 0;
            for (Quota quota : quotas) {
                if (quota.getId() == quotaId) {
                    Double newUsagesObj = quota.getTotalUsage();
                    if (newUsagesObj != null) {
                        newUsages = newUsagesObj.doubleValue();
                    }
                    break;
                }
            }
            System.out.println("+++++++++++++++the new usages is " + newUsages);
            System.out.println("+++++++++++++++the original usages is " + originalUsages);
            assertTrue(originalUsages == newUsages);//restore the total usages.
        } else {

        }
    }

    /**
     * Test a subscriber without enough quota
     * @throws Exception
     */
    @Test
    public void testNoEnoughQuota() throws Exception {
        if (!QuotaServiceManager.getInstance().isEnabled()) {
            return;
        }
        try {
            request.setHeader(QuotaServiceManager.QUOTASUBJECTHEADER, DEFICIT_SUBSCRIBER);
            Identifier guid = new Identifier();
            guid.setValue("testPortal." + System.currentTimeMillis());
            InputStream object = new FileInputStream(portalFilePath);
            Subject submitter = new Subject();
            submitter.setValue(DEFICIT_REQUESTOR);
            SystemMetadata sysmeta = d1NodeTest.createSystemMetadata(guid, submitter, object);
            ObjectFormatIdentifier formatId = new ObjectFormatIdentifier();
            formatId.setValue("https://purl.dataone.org/portals-1.0.0");
            sysmeta.setFormatId(formatId);
            String sidStr = generateUUID();
            Identifier sid = new Identifier();
            sid.setValue(sidStr);
            sysmeta.setSeriesId(sid);
            object.close();
            SystemMetadataManager.lock(guid);
            SystemMetadataManager.getInstance().store(sysmeta);
            SystemMetadataManager.unLock(guid);
            QuotaServiceManager.getInstance()
                .enforce(SUBSCRIBERWITHOUTENOUGHQUOTA, submitter, sysmeta,
                         QuotaServiceManager.CREATEMETHOD);
            fail("Test can't get here since the user doesn't have enough quota");
        } catch (InsufficientResources e) {
            assertTrue(
                e.getMessage().contains("doesn't have enough " + QuotaTypeDeterminer.PORTAL));
        }
    }

    /**
     * Test the case there is not subscriber header in the request
     */
    @Test
    public void testNoSubscriberHeader() throws Exception {
        if (!QuotaServiceManager.getInstance().isEnabled()) {
            return;
        }
        try {
            String header = request.getHeader(QuotaServiceManager.QUOTASUBJECTHEADER);
            assertTrue(header == null);
            Identifier guid = new Identifier();
            guid.setValue("testPortal." + System.currentTimeMillis());
            InputStream object = new FileInputStream(portalFilePath);
            Subject submitter = new Subject();
            submitter.setValue(DEFICIT_REQUESTOR);
            SystemMetadata sysmeta = d1NodeTest.createSystemMetadata(guid, submitter, object);
            ObjectFormatIdentifier formatId = new ObjectFormatIdentifier();
            formatId.setValue("https://purl.dataone.org/portals-1.0.0");
            sysmeta.setFormatId(formatId);
            String sidStr = generateUUID();
            Identifier sid = new Identifier();
            sid.setValue(sidStr);
            sysmeta.setSeriesId(sid);
            object.close();
            SystemMetadataManager.lock(guid);
            SystemMetadataManager.getInstance().store(sysmeta);
            SystemMetadataManager.unLock(guid);
            QuotaServiceManager.getInstance()
                .enforce(SUBSCRIBERWITHOUTENOUGHQUOTA, submitter, sysmeta,
                         QuotaServiceManager.CREATEMETHOD);
            fail("Test can't get here since the user doesn't have enough quota");
        } catch (InsufficientResources e) {
            assertTrue(
                e.getMessage().contains("doesn't have enough " + QuotaTypeDeterminer.PORTAL));
        }
    }

    /**
     * Test the delinquent user
     * @throws Exception
     */
    @Test
    public void testDelinquentUser() throws Exception {
        if (!QuotaServiceManager.getInstance().isEnabled()) {
            return;
        }
        try {
            request.setHeader(QuotaServiceManager.QUOTASUBJECTHEADER, DELINGUENT_SUBSCRIBER);
            Identifier guid = new Identifier();
            guid.setValue("testPortal." + System.currentTimeMillis());
            InputStream object = new FileInputStream(portalFilePath);
            Subject submitter = new Subject();
            submitter.setValue(DELINGUENT_REQUESTOR);
            SystemMetadata sysmeta = d1NodeTest.createSystemMetadata(guid, submitter, object);
            ObjectFormatIdentifier formatId = new ObjectFormatIdentifier();
            formatId.setValue("https://purl.dataone.org/portals-1.0.0");
            sysmeta.setFormatId(formatId);
            String sidStr = generateUUID();
            Identifier sid = new Identifier();
            sid.setValue(sidStr);
            sysmeta.setSeriesId(sid);
            object.close();
            SystemMetadataManager.lock(guid);
            SystemMetadataManager.getInstance().store(sysmeta);
            SystemMetadataManager.unLock(guid);
            QuotaServiceManager.getInstance()
                .enforce(SUBSCRIBERWITHOUTENOUGHQUOTA, submitter, sysmeta,
                         QuotaServiceManager.CREATEMETHOD);
            fail("Test can't get here since the user doesn't have enough quota");
        } catch (Exception e) {

        }
    }

    /**
     * Test the combineDateAndGivenTime method
     * @throws Exception
     */
    @Test
    public void testCombineDateAndGivenTime() throws Exception {
        String givenTime = "11:59 PM";
        Date date = QuotaServiceManager.combineDateAndGivenTime(new Date(), givenTime);
        DateFormat df = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.SHORT);
        String s = df.format(date);
        assertTrue("The final time string (after transformed by the method "
                     + " combineDateAndGivenTime) " + s
                     + " doesn't have the given time " + givenTime, s.contains(givenTime));

        Date testDateNow = new Date(1706574038000L); //January 30, 2024 12:20:38 AM GMT
        SimpleDateFormat showDate = new SimpleDateFormat("MMMMM dd, yyyy,");
        String tomorrow = showDate.format(new Date(testDateNow.getTime() + 24*60*60*1000));
        //The time is before the testDateNow and should set the date tomorrow
        Date before = new Date(testDateNow.getTime() - 150000);
        SimpleDateFormat shortTime = new SimpleDateFormat("h:mm a");
        String beforeShortTime = shortTime.format(before);
        String combineStr = tomorrow + " " + beforeShortTime;
        Date date1 = QuotaServiceManager.combineDateAndGivenTime(testDateNow, beforeShortTime);
        s = df.format(date1);
        assertTrue("The time parsed string " + s + " should be " + combineStr,
                    s.equals(combineStr));

        //after current time and should set the date today
        String today = showDate.format(testDateNow);
        Date after = new Date(testDateNow.getTime() + 150000);
        String afterShortTime = shortTime.format(after);
        combineStr = today + " " + afterShortTime;
        date1 = QuotaServiceManager.combineDateAndGivenTime(testDateNow, afterShortTime);
        s = df.format(date1);
        assertTrue("The parsed time string " + s + " should be " + combineStr,
                s.equals(combineStr));
    }

    /**
     * Create a usage object
     * @param quotaId
     * @param instanceId
     * @param quantity
     * @param status
     * @return
     */
    private Usage createUsage(int quotaId, String instanceId, double quantity, String status) {
        Usage usage = new Usage();
        usage.setObject(QuotaServiceManager.USAGE);
        usage.setQuotaId(quotaId);
        usage.setInstanceId(instanceId);
        usage.setQuantity(quantity);
        System.out.println("set the node id to the usage " + nodeId);
        usage.setNodeId(nodeId);
        usage.setStatus(status);
        return usage;
    }

    /**
     * Get a unique id
     * @return a uuid
     */
    private String generateUUID() {
        String prefix = "urn:uuid";
        return prefix + UUID.randomUUID().toString();
    }
}
