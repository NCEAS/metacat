package edu.ucsb.nceas.metacat.startup;

import edu.ucsb.nceas.metacat.MetacatVersion;
import edu.ucsb.nceas.metacat.admin.AdminException;
import edu.ucsb.nceas.metacat.admin.D1Admin;
import edu.ucsb.nceas.metacat.admin.DBAdmin;
import edu.ucsb.nceas.metacat.database.DBVersion;
import edu.ucsb.nceas.metacat.util.SystemUtil;
import edu.ucsb.nceas.utilities.GeneralPropertyException;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.servlet.ServletException;

/**
 * Collection of administrative initialization tasks that need to be performed automatically on
 * startup, when running in Kubernetes. Typically, the public <code>initializeK8sInstance()</code>
 * method is called during system startup - e.g. from the servlet <code>init()</code> method.
 *
 * (In legacy deployments, these tasks are carried out manually, using the admin UI. However, this
 * isn't possible or desirable in Kubernetes, since the site properties file is a read-only
 * configmap)
 */
public class K8sAdminInitializer {

    private static final Log logMetacat = LogFactory.getLog(K8sAdminInitializer.class);

    /**
     * private constructor, since this is a singleton
     */
    private K8sAdminInitializer() {
    }

    /**
     * Method to be called during system startup - e.g. from the servlet <code>init()</code> method
     * . This method, in turn, will verify that this is a containerized/k8s instance, and then
     * will perform the necessary initialization steps
     *
     * @throws ServletException if anything goes wrong
     */
    public static void initializeK8sInstance() throws ServletException {

        verifyK8s();

        logMetacat.info("initializeK8sInstance() configuring for K8s environment...");

        //check database and update if necessary
        initK8sDBConfig();

        // check D1 Admin settings and update if necessary
        initK8sD1Admin();
    }

    /**
     * Checks the environment variable "METACAT_IN_K8S", set by the helm chart,
     * to verify that this instance is running in Kubernetes.
     *
     * @throws ServletException if this instance is NOT running in a container
     * @implNote package private to allow for unit testing
     */
    static void verifyK8s() throws ServletException {
        if (!Boolean.parseBoolean(System.getenv("METACAT_IN_K8S"))) {
            throw new ServletException("initializeK8sInstance() called, but Metacat is NOT running "
                                           + "in a container (or METACAT_IN_K8S env var has not "
                                           + "been set correctly)");
        }
    }

    /**
     * Call the DBAdmin code to check for and perform any database updates that may be necessary.
     * (In a non-k8s environment, database updates are performed manually, via the admin UI)
     *
     * @throws ServletException if updates were unsuccessful
     * @implNote package private to allow for unit testing
     */
    static void initK8sDBConfig() throws ServletException {

        logMetacat.info("initK8sDBConfig(): checking for necessary database updates...");
        MetacatVersion metacatVersion;
        DBVersion dbVersion;
        String mcVerStr = "NULL";
        String dbVerStr = "NULL";
        try {
            metacatVersion = SystemUtil.getMetacatVersion();
            mcVerStr = metacatVersion.getVersionString();
            dbVersion = DBAdmin.getInstance().getDBVersion();
            dbVerStr = dbVersion.getVersionString();
            if (metacatVersion.compareTo(dbVersion) == 0) {
                // ALREADY UPGRADED
                logMetacat.info(
                    "initK8sDBConfig(): NO DATABASE UPDATES REQUIRED, since database " + "version ("
                        + dbVerStr + ") matches metacat version (" + mcVerStr + ").");
            } else {
                // NEEDS UPGRADE
                logMetacat.info(
                    "initK8sDBConfig(): UPDATING DATABASE, " + "since database version (" + dbVerStr
                        + ") is behind Metacat version (" + mcVerStr + ").");
                DBAdmin.getInstance().upgradeDatabase();
            }
        } catch (AdminException | PropertyNotFoundException e) {
            String msg = "initializeContainerisedDBConfiguration(): error getting metacat version ("
                + mcVerStr + ") or database version (" + dbVerStr + "). Error was: "
                + e.getMessage();
            logMetacat.error(msg, e);
            ServletException se = new ServletException(msg, e);
            se.fillInStackTrace();
            throw se;
        }
    }

    /**
     * Call the D1Admin upRegD1MemberNode() method to handle DataONE Member Node registration or
     * updates that may be necessary. (In a non-k8s environment, Member Node updates are performed
     * manually, via the admin UI)
     *
     * @throws ServletException if MN updates were unsuccessful
     * @implNote package private to allow for unit testing
     */
    static void initK8sD1Admin() throws ServletException {
        logMetacat.info("Running in a container; calling D1Admin::upRegD1MemberNode");
        try {
            D1Admin.getInstance().upRegD1MemberNode();
        } catch (GeneralPropertyException | AdminException e) {
            String msg = "initializeContainerizedD1Admin(): error calling "
                + "D1Admin.getInstance().upRegD1MemberNode: " + e.getMessage();
            logMetacat.error(msg, e);
            ServletException se = new ServletException(msg, e);
            se.fillInStackTrace();
            throw se;
        }
    }
}
