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
 * method is called during system startup - eg from the servlet <code>init()</code> method.
 *
 * (In legacy deployments, these tasks are carried out manually, using the admin UI. However,
 * this isn't possible or desirable in Kubernetes, since the site properties file is a read-only
 * configmap)
 */
public class K8sAdminInitializer {

    private static final Log logMetacat = LogFactory.getLog(K8sAdminInitializer.class);

    /**
     * private constructor, since this is a singleton
     */
    private K8sAdminInitializer() {
    }

    public static void initializeK8sInstance() throws ServletException {

        logMetacat.info("initializeK8sInstance() called...");

        if (!isK8sInstance()) {
            throw new ServletException("initializeK8sInstance() called, but Metacat is NOT "
                                           + "running in a container (or the "
                                           + "METACAT_IS_RUNNING_IN_A_CONTAINER env var has not "
                                           + "been set correctly)");
        }

        //check database and update if necessary
        initK8sDBConfig();

        // check D1 Admin settings and update if necessary
        initK8sD1Admin();
    }


    /**
     * Check if we're running in a container/Kubernetes, and if so, call the DBAdmin code to
     * check for and perform any database updates that may be necessary.
     *
     * If this is a legacy deployment (i.e. not containerized/k8s), this method does nothing, since
     * database updates are performed manually, via the admin pages.
     *
     * @throws ServletException if updates were unsuccessful
     */
    void initializeContainerizedDBConfiguration() throws ServletException {
        if (isContainerized()) {
            logMetacat.info("Running in a container; checking for necessary database updates...");
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
                    logMetacat.info("initializeContainerisedDBConfiguration(): NO DATABASE "
                                        + "UPDATES REQUIRED, since database version (" + dbVerStr
                                        + ") matches metacat version (" + mcVerStr + ").");
                } else {
                    // NEEDS UPGRADE
                    logMetacat.info("initializeContainerisedDBConfiguration(): UPDATING DATABASE, "
                                        + "since database version (" + dbVerStr
                                        + ") is behind Metacat version (" + mcVerStr + ").");
                    DBAdmin.getInstance().upgradeDatabase();
                }
            } catch (AdminException | PropertyNotFoundException e) {
                String msg =
                    "initializeContainerisedDBConfiguration(): error getting metacat version ("
                        + mcVerStr + ") or database version (" + dbVerStr + "). Error was: "
                        + e.getMessage();
                logMetacat.error(msg, e);
                ServletException se = new ServletException(msg, e);
                se.fillInStackTrace();
                throw se;
            }
        } else {
            logMetacat.info("NOT Running in a container; no automatic database updates");
        }
    }

    /**
     * Checks the environment variable "METACAT_IS_RUNNING_IN_A_CONTAINER", set by the helm
     * chart, to determine if this instance is running in Kubernetes
     *
     * @return true if this instance is running in a container; false otherwise
     */
    public static boolean isK8sInstance() {
        return Boolean.parseBoolean(System.getenv("METACAT_IS_RUNNING_IN_A_CONTAINER"));
    }

    /**
     * Call the DBAdmin code to check for and perform any database updates that may be necessary.
     *
     * @throws ServletException if updates were unsuccessful
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
     * Check if we're running in a container/Kubernetes, and if so, call the D1Admin
     * upRegD1MemberNode() method to handle DataONE Member Node registration or updates that may be
     * necessary. If this is a legacy deployment (i.e. not containerized/k8s), this method does
     * nothing, since Member Node updates are performed manually, via the admin pages.
     *
     * @throws ServletException if MN updates were unsuccessful
     */
    static void initK8sD1Admin() throws ServletException {
        if (D1AdminCNUpdater.isMetacatRunningInAContainer()) {
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
        } else {
            logMetacat.info("NOT Running in a container; no automatic D1MemberNode updates");
        }
    }
}
