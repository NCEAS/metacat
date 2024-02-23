/**
 *  '$RCSfile$'
 *    Purpose: A Class that implements database configuration methods
 *  Copyright: 2008 Regents of the University of California and the
 *             National Center for Ecological Analysis and Synthesis
 *    Authors: Michael Daigle
 * 
 *   '$Author$'
 *     '$Date$'
 * '$Revision$'
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

package edu.ucsb.nceas.metacat.admin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.sql.Statement;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.TreeSet;
import java.util.Vector;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import edu.ucsb.nceas.metacat.MetacatVersion;
import edu.ucsb.nceas.metacat.admin.upgrade.PrerequisiteChecker300;
import edu.ucsb.nceas.metacat.admin.upgrade.UpgradeUtilityInterface;
import edu.ucsb.nceas.metacat.admin.upgrade.solr.SolrSchemaModificationException;
import edu.ucsb.nceas.metacat.database.DBConnection;
import edu.ucsb.nceas.metacat.database.DBVersion;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.metacat.shared.MetacatUtilException;
import edu.ucsb.nceas.metacat.util.DatabaseUtil;
import edu.ucsb.nceas.metacat.util.RequestUtil;
import edu.ucsb.nceas.metacat.util.SystemUtil;

import edu.ucsb.nceas.utilities.DBUtil;
import edu.ucsb.nceas.utilities.FileUtil;
import edu.ucsb.nceas.utilities.GeneralPropertyException;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Control the display of the database configuration page and the processing
 * of the configuration values.
 */
public class DBAdmin extends MetacatAdmin {
    // db statuses used by discovery code
    public static final int DB_DOES_NOT_EXIST = 0;
    public static final int TABLES_DO_NOT_EXIST = 1;
    public static final int TABLES_EXIST = 2;

    // db version statuses. This allows us to keep version history
    // in the db. Only the latest version record should be active.
    public static final int VERSION_INACTIVE = 0;
    public static final int VERSION_ACTIVE = 1;
    private final static String UPDATE3_0_0_ClASS_NAME =
                                                "edu.ucsb.nceas.metacat.admin.upgrade.Upgrade3_0_0";

    private TreeSet<DBVersion> versionSet = null;

    private static DBAdmin dbAdmin = null;
    private Log logMetacat = LogFactory.getLog(DBAdmin.class);
    private HashSet<String> sqlCommandSet = new HashSet<String>();
    private Map<String, String> scriptSuffixMap = new HashMap<String, String>();
    private static DBVersion databaseVersion = null;


    /**
     * private constructor since this is a singleton
     */
    private DBAdmin() throws AdminException {
        sqlCommandSet.add("INSERT");
        sqlCommandSet.add("UPDATE");
        sqlCommandSet.add("DELETE");
        sqlCommandSet.add("ALTER");
        sqlCommandSet.add("CREATE");
        sqlCommandSet.add("DROP");
        sqlCommandSet.add("BEGIN");
        sqlCommandSet.add("COMMIT");
        sqlCommandSet.add("WITH");
        sqlCommandSet.add("SELECT");

        // gets all the upgrade version objects
        try {
            versionSet = DatabaseUtil.getUpgradeVersions();
            scriptSuffixMap = DatabaseUtil.getScriptSuffixes();
        } catch (PropertyNotFoundException pnfe) {
            throw new AdminException("DBAdmin() - Could not retrieve database upgrade " 
                    + "versions during instantiation" + pnfe.getMessage());
        } catch (NumberFormatException nfe) {
            throw new AdminException("DBAdmin() - Bad version format numbering: "
                    + nfe.getMessage());
        }
    }

    /**
     * Get the single instance of DBAdmin.
     * @return the single instance of DBAdmin
     * @throws AdminException
     */
    public static DBAdmin getInstance() throws AdminException {
        if (dbAdmin == null) {
            synchronized (DBAdmin.class) {
                if (dbAdmin == null) {
                    dbAdmin = new DBAdmin();
                }
            }
        }
        return dbAdmin;
    }

    /**
     * Handle configuration of the database the first time that Metacat starts
     * or when it is explicitly called. Collect necessary update information
     * from the administrator.
     * @param request
     *            the http request information
     * @param response
     *            the http response to be sent back to the client
     */
    public void configureDatabase(HttpServletRequest request,
            HttpServletResponse response) throws AdminException {

        String processForm = request.getParameter("processForm");
        String formErrors = (String) request.getAttribute("formErrors");
        HttpSession session = request.getSession();
        String supportEmail = null;

        if (processForm == null || !processForm.equals("true") || formErrors != null) {
            // The servlet configuration parameters have not been set, or there
            // were form errors on the last attempt to configure, so redirect to
            // the web form for configuring metacat

            try {
                // get the current metacat version and the database version. If
                // the database version is older that the metacat version, run
                // the appropriate scripts to get them synchronized.

                databaseVersion = discoverDBVersion();
                MetacatVersion metacatVersion = SystemUtil.getMetacatVersion();
                
                session.setAttribute("metacatVersion", MetacatVersion.getVersionID());

                // if the db version is already the same as the metacat
                // version, update metacat.properties
                if (databaseVersion.compareTo(metacatVersion) == 0) {
                    PropertyService.setProperty("configutil.databaseConfigured",
                            PropertyService.CONFIGURED);
                }
                
                MetacatVersion metaCatVersion = SystemUtil.getMetacatVersion();
                request.setAttribute("metacatVersion", metaCatVersion);
                DBVersion dbVersionString = getDBVersion();
                request.setAttribute("databaseVersion", dbVersionString);
                Vector<String> updateScriptList = getUpdateScripts();
                request.setAttribute("updateScriptList", updateScriptList);
                supportEmail = PropertyService.getProperty("email.recipient");
                request.setAttribute("supportEmail", supportEmail);

                // Forward the request to the JSP page
                RequestUtil.clearRequestMessages(request);
                RequestUtil.forwardRequest(request, response,
                        "/admin/database-configuration.jsp", null);
            } catch (GeneralPropertyException gpe) {
                throw new AdminException("DBAdmin.configureDatabase - Problem getting or "
                            + "setting property while initializing system properties page: "
                              + gpe.getMessage());
            } catch (MetacatUtilException mue) {
                throw new AdminException("DBAdmin.configureDatabase - utility problem while "
                        + "initializing system properties page:" + mue.getMessage());
            } 
        } else {
            // The configuration form is being submitted and needs to be
            // processed, setting the properties in the configuration file
            // then restart metacat

            // The configuration form is being submitted and needs to be
            // processed.
            Vector<String> validationErrors = new Vector<String>();
            Vector<String> processingSuccess = new Vector<String>();

            try {
                // Validate that the options provided are legitimate. Note that
                // we've allowed them to persist their entries. As of this point
                // there is no other easy way to go back to the configure form
                // and
                // preserve their entries.
                supportEmail = PropertyService.getProperty("email.recipient");
                validationErrors.addAll(validateOptions(request));
                
                
                upgradeDatabase();
                
                

                // Now that the options have been set, change the
                // 'databaseConfigured' option to 'true' so that normal metacat
                // requests will go through
                PropertyService.setProperty("configutil.databaseConfigured",
                        PropertyService.CONFIGURED);
                PropertyService.persistMainBackupProperties();
               
                    // Reload the main metacat configuration page
                    processingSuccess.add("Database successfully upgraded");
                    RequestUtil.clearRequestMessages(request);
                    RequestUtil.setRequestSuccess(request, processingSuccess);
                    RequestUtil.forwardRequest(request, response,
                            "/admin?configureType=configure&processForm=false", null);
                 
            
            } catch (GeneralPropertyException gpe) {
                throw new AdminException("DBAdmin.configureDatabase - Problem getting or setting " 
                                    + "property while upgrading database: " + gpe.getMessage());
            }  catch (MetacatUtilException mue) {
                throw new AdminException("DBAdmin.configureDatabase - utility problem while"
                                            + " upgrading database: " + mue.getMessage());
            } 
        }
    }


    /**
     * Performs a status check on the database.
     * @returns integer representing the status of the database. These can be: 
     *         -- DB_DOES_NOT_EXIST = 0; 
     *      -- TABLES_DO_NOT_EXIST = 1; 
     *      -- TABLES_EXIST = 2;
     */
    public static int getDBStatus() throws SQLException, PropertyNotFoundException {
        Connection connection = DBUtil.getConnection(PropertyService
                .getProperty("database.connectionURI"), PropertyService
                .getProperty("database.user"), PropertyService
                .getProperty("database.password"));

        if (DBUtil.tableExists(connection, "xml_documents")) {
            return TABLES_EXIST;
        }

        return TABLES_DO_NOT_EXIST;
    }

    /**
     * Get the version of the database as a string
     * @returns string representing the version of the database.
     */
    public DBVersion getDBVersion() throws AdminException {

        // don't even try to search for a database version until system
        // properties have been configured
        try {
            if (!PropertyService.arePropertiesConfigured()) {
                throw new AdminException("DBAdmin.getDBVersion - An attempt was made to get " + 
                        "the database version before system properties were configured");
            }
        } catch (GeneralPropertyException gpe) {
            throw new AdminException("DBAdmin.getDBVersion - Could not determine the database version: "
                    + gpe.getMessage());
        }
        if (databaseVersion == null) {
            databaseVersion = discoverDBVersion();
        }

        if (databaseVersion == null) {
            throw new AdminException("DBAdmin.getDBVersion - Could not find database version");
        }

        return databaseVersion;
    }

    /**
     * Try to discover the database version, first by calling
     * getRegisteredDBVersion() to see if the database version is in a table in
     * the database. If not, getUnRegisteredDBVersion() is called to see if we
     * can devine the version by looking for unique changes made by scripts for
     * each version update.

     * @returns string representing the version of the database, null if none
     *          could be found.
     */
    private DBVersion discoverDBVersion() throws AdminException {
        try {
            int dbStatus = getDBStatus();
            if (dbStatus == DB_DOES_NOT_EXIST) {
                throw new AdminException("DBAdmin.discoverDBVersion - Database does not exist " +
                        "for connection" + PropertyService.getProperty("database.connectionURI"));
            } else if (dbStatus == TABLES_DO_NOT_EXIST) {
                databaseVersion = new DBVersion("0.0.0");
                return databaseVersion;
            }

            databaseVersion = getRegisteredDBVersion();
            if (databaseVersion != null) {
                return databaseVersion;
            }

            databaseVersion = getUnRegisteredDBVersion();
            
        } catch (SQLException sqle) {
            String errorMessage = "DBAdmin.discoverDBVersion - SQL error during  database "
                                    + "version discovery: " + sqle.getMessage();
            logMetacat.error(errorMessage);
            throw new AdminException(errorMessage);
        } catch (PropertyNotFoundException pnfe) {
            String errorMessage = "DBAdmin.discoverDBVersion - Property not found during  database " + 
            "version discovery: " + pnfe.getMessage();
            logMetacat.error(errorMessage);
            throw new AdminException(errorMessage);
        } catch (NumberFormatException nfe) {
            throw new AdminException("DBAdmin.discoverDBVersion - Bad version format numbering: "
                    + nfe.getMessage());
        }
        
        if (databaseVersion == null) {
            throw new AdminException("DBAdmin.discoverDBVersion - Database version discovery returned null");
        }
        return databaseVersion;
    }


    /**
     * Gets the version of the database from the db_version table. Usually this
     * is the same as the version of the product, however the db version could
     * be more granular if we applied a maintenance patch for instance.
     * @returns string representing the version of the database.
     */
    private DBVersion getRegisteredDBVersion() throws AdminException, SQLException {
        String dbVersionString = null;
        PreparedStatement pstmt = null;

        try {
            // check out DBConnection
            Connection connection = 
                DBUtil.getConnection(
                        PropertyService.getProperty("database.connectionURI"),
                        PropertyService.getProperty("database.user"),
                        PropertyService.getProperty("database.password"));

            if (!DBUtil.tableExists(connection, "db_version")) {
                return null;
            }

            pstmt = 
                connection.prepareStatement("SELECT version FROM db_version WHERE status = ?");

            // Bind the values to the query
            pstmt.setInt(1, VERSION_ACTIVE);
            pstmt.execute();
            ResultSet rs = pstmt.getResultSet();
            boolean hasRows = rs.next();
            if (hasRows) {
                dbVersionString = rs.getString(1);
            }
            
            if (dbVersionString == null) {
                return null;
            } 
                
            return new DBVersion(dbVersionString);
            
        } catch (SQLException sqle) {
            throw new AdminException("DBAdmin.getRegisteredDBVersion - Could not run SQL to "
                                        + "get registered db version: " + sqle.getMessage());
        } catch (PropertyNotFoundException pnfe) {
            throw new AdminException("DBAdmin.getRegisteredDBVersion - Could not get property "
                                        + "for registered db version: " + pnfe.getMessage());
        } catch (NumberFormatException nfe) {
            throw new AdminException("DBAdmin.getRegisteredDBVersion - Bad version format numbering: "
                    + nfe.getMessage());
        } finally {
            if (pstmt != null) {
                pstmt.close();
            }
        }
    }

    /**
     * Finds the version of the database for a database that does not have a
     * dbVersion table yet. Work backwards with various clues found in update
     * scripts to find the version.
     * @returns string representing the version of the database.
     */
    public DBVersion getUnRegisteredDBVersion() throws AdminException, SQLException {
        Connection connection = null;
        try {
            connection = DBUtil.getConnection(PropertyService
                    .getProperty("database.connectionURI"), PropertyService
                    .getProperty("database.user"), PropertyService
                    .getProperty("database.password"));

            String dbVersionString = null;

            if (is1_9_1(connection)) {
                dbVersionString = "1.9.1";
            } else if (is1_9_0(connection)) {
                dbVersionString = "1.9.0";
            } else if (is1_8_0(connection)) {
                dbVersionString = "1.8.0";
            } else if (is1_7_0(connection)) {
                dbVersionString = "1.7.0";
            } else if (is1_6_0(connection)) {
                dbVersionString = "1.6.0";
            } else if (is1_5_0(connection)) {
                dbVersionString = "1.5.0";
            } else if (is1_4_0(connection)) {
                dbVersionString = "1.4.0";
            } else if (is1_3_0(connection)) {
                dbVersionString = "1.3.0";
            } else if (is1_2_0(connection)) {
                dbVersionString = "1.2.0";
            }

            if (dbVersionString == null) {
                return null;
            } else {
                return new DBVersion(dbVersionString);
            }
        } catch (PropertyNotFoundException pnfe) {
            throw new AdminException("DBAdmin.getUnRegisteredDBVersion - Could not get " + 
                    "property for unregistered db version: " + pnfe.getMessage());
        } catch (NumberFormatException nfe) {
            throw new AdminException("DBAdmin.getUnRegisteredDBVersion - Bad version format numbering: "
                    + nfe.getMessage());
        }
    }


    /**
     * Validate connectivity to the database. Validation methods return a string
     * error message if there is an issue. This allows the calling code to run
     * several validations and compile the errors into a list that can be
     * displayed on a web page if desired.
     * @param dbDriver
     *            the database driver
     * @param connection
     *            the jdbc connection string
     * @param user
     *            the user name
     * @param password
     *            the login password
     * @return a string holding error message if validation fails.
     */
    public String validateDBConnectivity(String dbDriver, String connection,
            String user, String password) {
        try {
            DBConnection.testConnection(dbDriver, connection, user, password);
        } catch (SQLException se) {
            return "Invalid database credential was provided: "
                    + se.getMessage();
        }

        return null;
    }

    /**
     * Checks to see if this is a 1.9.0 database schema by looking for the
     * db_version table which was created for 1.9.0. Note, there is no guarantee
     * that this table will not be removed in subsequent versions. You should
     * search for db versions from newest to oldest, only getting to this
     * function when newer versions have not been matched.
     * @param dbMetaData
     *            the meta data for this database.
     * @returns boolean which is true if table is found, false otherwise
     */
    private boolean is1_9_0(Connection connection) throws SQLException {
        return DBUtil.tableExists(connection, "db_version");
    }
    
    /**
     * Checks to see if this is a 1.9.1 database schema by looking for the
     * scheduled_job table which was created for 1.9.0. Note, there is no guarantee
     * that this table will not be removed in subsequent versions. You should
     * search for db versions from newest to oldest, only getting to this
     * function when newer versions have not been matched.
     * @param dbMetaData
     *            the meta data for this database.
     * @returns boolean which is true if table is found, false otherwise
     */
    private boolean is1_9_1(Connection connection) throws SQLException {
        return DBUtil.tableExists(connection, "db_version");
    }

    /**
     * Checks to see if this is a 1.8.0 database schema by looking for the
     * xml_nodes_idx4 index which was created for 1.8.0. Note, there is no
     * guarantee that this index will not be removed in subsequent versions. You
     * should search for db versions from newest to oldest, only getting to this
     * function when newer versions have not been matched.
     * @param dbMetaData
     *            the meta data for this database.
     * @returns boolean which is true if index is found, false otherwise
     */
    private boolean is1_8_0(Connection connection) throws SQLException, PropertyNotFoundException {
        String tableName = "xml_nodes";
        String dbType = PropertyService.getProperty("database.type");
            
        boolean isOracle = dbType.equals("oracle");
        if(isOracle) {
            tableName = "XML_NODES";
        }
        return DBUtil.indexExists(connection, tableName, "xml_nodes_idx4");
    }

    /**
     * Checks to see if this is a 1.7.0 database schema by looking for the
     * xml_documents_idx2 index which was created for 1.7.0. Note, there is no
     * guarantee that this index will not be removed in subsequent versions. You
     * should search for db versions from newest to oldest, only getting to this
     * function when newer versions have not been matched.
     * @param dbMetaData
     *            the meta data for this database.
     * @returns boolean which is true if index is found, false otherwise
     */
    private boolean is1_7_0(Connection connection) throws SQLException, PropertyNotFoundException {
        String tableName = "xml_documents";
        String dbType = PropertyService.getProperty("database.type");
        boolean isOracle = dbType.equals("oracle");
        if(isOracle) {
            tableName = "XML_DOCUMENTS";
        }
        return DBUtil.indexExists(connection, tableName, "xml_documents_idx2");
    }

    /**
     * Checks to see if this is a 1.6.0 database schema by looking for the
     * identifier table which was created for 1.6.0. Note, there is no guarantee
     * that this table will not be removed in subsequent versions. You should
     * search for db versions from newest to oldest, only getting to this
     * function when newer versions have not been matched.
     * @param dbMetaData
     *            the meta data for this database.
     * @returns boolean which is true if table is found, false otherwise
     */
    private boolean is1_6_0(Connection connection) throws SQLException {
        return DBUtil.tableExists(connection, "identifier");
    }

    /**
     * Checks to see if this is a 1.5.0 database schema by looking for the
     * xml_returnfield table which was created for 1.5.0. Note, there is no
     * guarantee that this table will not be removed in subsequent versions. You
     * should search for db versions from newest to oldest, only getting to this
     * function when newer versions have not been matched.
     * @param dbMetaData
     *            the meta data for this database.
     * @returns boolean which is true if table is found, false otherwise
     */
    private boolean is1_5_0(Connection connection) throws SQLException {
        return DBUtil.tableExists(connection, "xml_returnfield");
    }

    /**
     * Checks to see if this is a 1.4.0 database schema by looking for the
     * access_log table which was created for 1.4.0. Note, there is no guarantee
     * that this table will not be removed in subsequent versions. You should
     * search for db versions from newest to oldest, only getting to this
     * function when newer versions have not been matched.
     * @param dbMetaData
     *            the meta data for this database.
     * @returns boolean which is true if table is found, false otherwise
     */
    private boolean is1_4_0(Connection connection) throws SQLException {
        return DBUtil.tableExists(connection, "access_log");
    }

    /**
     * Checks to see if this is a 1.3.0 database schema by looking for the
     * xml_accesssubtree table which was created for 1.3.0. Note, there is no
     * guarantee that this table will not be removed in subsequent versions. You
     * should search for db versions from newest to oldest, only getting to this
     * function when newer versions have not been matched.
     * @param dbMetaData
     *            the meta data for this database.
     * @returns boolean which is true if table is found, false otherwise
     */
    private boolean is1_3_0(Connection connection) throws SQLException {
        return DBUtil.tableExists(connection, "xml_accesssubtree");
    }

    /**
     * Checks to see if this is a 1.2.0 database schema by looking for the
     * datareplicate column which was created on the xml_replication table for
     * 1.2.0. Note, there is no guarantee that this column will not be removed
     * in subsequent versions. You should search for db versions from newest to
     * oldest, only getting to this function when newer versions have not been
     * matched.
     * @param dbMetaData
     *            the meta data for this database.
     * @returns boolean which is true if column is found, false otherwise
     */
    private boolean is1_2_0(Connection connection) throws SQLException {
        return DBUtil.columnExists(connection, "xml_replication",
                "datareplicate");
    }

    /**
     * Creates a list of database update script names by looking at the database
     * version and the metacat version and then getting any script that is
     * inbetween the two (inclusive of metacat version).
     * @returns a Vector of Strings holding the names of scripts that need to be
     *          run to get the database updated to this version of metacat
     */
    public Vector<String> getUpdateScripts() throws AdminException {
        Vector<String> updateScriptList = new Vector<String>();
        String sqlFileLocation = null;
        String databaseType = null;
        MetacatVersion metaCatVersion = null;
        
        // get the location of sql scripts
        try {
            metaCatVersion = SystemUtil.getMetacatVersion();
            sqlFileLocation = SystemUtil.getSQLDir();
            databaseType = PropertyService.getProperty("database.type");
        } catch (PropertyNotFoundException pnfe) {
            throw new AdminException("DBAdmin.getUpdateScripts - Could not get property while trying " 
                    + "to retrieve database update scripts: " + pnfe.getMessage());
        }
        
        // Each type of db has it's own set of scripts.  For instance, Oracle
        // scripts end in -oracle.sql.  Postges end in -postgres.sql, etc
        String sqlSuffix = "-" + scriptSuffixMap.get("database.scriptsuffix." + databaseType);
        
        // if either of these is null, we don't want to do anything.  Just 
        // return an empty list.
        if (metaCatVersion == null || databaseVersion == null) {
            return updateScriptList;
        }

        // go through all the versions that the the software went through and
        // figure out which ones need to be applied to the database
        for (DBVersion nextVersion : versionSet) {
            Vector<String> versionUpdateScripts = nextVersion
                    .getUpdateScripts();
            
            // if the database version is 0.0.0, it is new.
            // apply all scripts.
            if (databaseVersion.getVersionString().equals("0.0.0")
                    && nextVersion.getVersionString().equals("0.0.0")) {
                for (String versionUpdateScript : versionUpdateScripts) {
                    updateScriptList.add(sqlFileLocation + FileUtil.getFS()
                            + versionUpdateScript + sqlSuffix);
                }
                return updateScriptList;
            }

            // add every update script that is > than the db version
            // but <= to the metacat version to the update list.
            if (nextVersion.compareTo(databaseVersion) > 0
                    && nextVersion.compareTo(metaCatVersion) <= 0
                    && nextVersion.getUpdateScripts() != null) {
                for (String versionUpdateScript : versionUpdateScripts) {
                    updateScriptList.add(sqlFileLocation + FileUtil.getFS()
                            + versionUpdateScript + sqlSuffix);
                }
            }
        }

        // this should now hold all the script names that need to be run
        // to bring the database up to date with this version of metacat
        return updateScriptList;
    }

    /**
     * Get the list of the Java and Solr update class files which will run in the upgrade process
     * @return the list of Java class files
     * @throws AdminException
     */
    public Vector<String> getUpdateClasses() throws AdminException {
        Vector<String> updateClassList = new Vector<String>();
        MetacatVersion metaCatVersion = null;

        // get the location of sql scripts
        try {
            metaCatVersion = SystemUtil.getMetacatVersion();
        } catch (PropertyNotFoundException pnfe) {
            throw new AdminException("DBAdmin.getUpdateScripts - Could not get property while trying " 
                    + "to retrieve update utilities: " + pnfe.getMessage());
        }

        if (metaCatVersion != null) {
            logMetacat.debug("DBADmin.getUpdateClasses - the version from the propterty file is "
                                                            + metaCatVersion.getVersionString());
        }
        if (databaseVersion != null) {
            logMetacat.debug("DBADmin.getUpdateClasses - the databaseVersion is "
                                                            + databaseVersion.getVersionString());
        }
        // if either of these is null, or the database version is 0.0.0 (a fresh installation),
        // we don't want to do anything.  Just return an empty list.
        if (metaCatVersion == null || databaseVersion == null ||
                                             databaseVersion.getVersionString().equals("0.0.0")) {
            return updateClassList;
        }

        // go through all the versions that the the software went through and
        // figure out which ones need to be applied to the database
        for (DBVersion nextVersion : versionSet) {
            // add every update script that is > than the db version
            // but <= to the metacat version to the update list.
            if (nextVersion.compareTo(databaseVersion) > 0
                    && nextVersion.compareTo(metaCatVersion) <= 0) {
                String key = "database.upgradeUtility." + nextVersion.getVersionString();
                String className = null;
                try {
                    className = PropertyService.getProperty(key);
                } catch (PropertyNotFoundException pnfe) {
                    // there probably isn't a utility needed for this version
                    logMetacat.warn("No utility defined for version: " + key);
                    continue;
                }
                logMetacat
                        .debug("DBAdmin.getUpdateClasses - add the class to the list " + className);
                updateClassList.add(className);
            }
        }

        // this should now hold all the script names that need to be run
        // to bring the database up to date with this version of metacat
        return updateClassList;
    }
    
    /**
     * Iterates through the list of scripts that need to be run to upgrade
     * the database and calls runSQLFile on each.
     */
    public void upgradeDatabase() throws AdminException {
        boolean persist = true;
        PrerequisiteChecker300 checker = new PrerequisiteChecker300();
        checker.check();
        Vector<String> updateClassList = getUpdateClasses();
        // Update3_0_0 should run before the database update
        if(updateClassList.contains(UPDATE3_0_0_ClASS_NAME)) {
            UpgradeUtilityInterface utility = null;
            try {
                utility = (UpgradeUtilityInterface) Class.forName(UPDATE3_0_0_ClASS_NAME)
                                                    .getDeclaredConstructor().newInstance();
                utility.upgrade();
            } catch (Exception e) {
                try {
                    MetacatAdmin.updateUpgradeStatus("configutil.upgrade.java.status", 
                                                                    MetacatAdmin.FAILURE, persist);
                } catch (Exception ee) {
                    logMetacat.warn("DBAdmin.upgradeDatabase - couldn't update the status of " 
                                      + "the upgrading database process since " + ee.getMessage());
                }
                throw new AdminException("DBAdmin.upgradeDatabase - error to run the class: " 
                                                + UPDATE3_0_0_ClASS_NAME + ". Error message: "
                                                + e.getMessage());
            }
            updateClassList.remove(UPDATE3_0_0_ClASS_NAME);
        }
        try {
            // get a list of the script names that need to be run
            Vector<String> updateScriptList = getUpdateScripts();

            // call runSQLFile on each
            for (String updateScript : updateScriptList) {
                runSQLFile(updateScript);
            }
            try {
                MetacatAdmin.updateUpgradeStatus("configutil.upgrade.database.status",
                                                        MetacatAdmin.SUCCESS, persist);
            } catch (Exception e) {
                logMetacat.warn("DBAdmin.upgradeDatabase - couldn't update the status of the "
                                + "upgrading database process since " + e.getMessage());
            }
        } catch (SQLException sqle) {
            try {
                MetacatAdmin.updateUpgradeStatus("configutil.upgrade.database.status",
                                                    MetacatAdmin.FAILURE, persist);
            } catch (Exception e) {
                logMetacat.warn("DBAdmin.upgradeDatabase - couldn't update the status of "
                                + "the upgrading database process since " + e.getMessage());
            }
            throw new AdminException("DBAdmin.upgradeDatabase - SQL error when running "
                                    + "upgrade scripts: "+ sqle.getMessage());
        } 
            
        // get the classes we need to execute in order to bring DB to current version

        for (String className : updateClassList) {
            UpgradeUtilityInterface utility = null;
            try {
                utility = (UpgradeUtilityInterface) Class.forName(className)
                                                            .getDeclaredConstructor().newInstance();
                utility.upgrade();
            } catch (SolrSchemaModificationException e) {
                //don't throw the exception and continue
               // solrSchemaException = e;
                continue;
            } catch (Exception e) {
                try {
                    MetacatAdmin.updateUpgradeStatus("configutil.upgrade.java.status",
                                                    MetacatAdmin.FAILURE, persist);
                } catch (Exception ee) {
                    logMetacat.warn("DBAdmin.upgradeDatabase - couldn't update the status of the "
                                    + "upgrading database process since " + ee.getMessage());
                }
                throw new AdminException("DBAdmin.upgradeDatabase - error getting utility class: "
                        + className + ". Error message: "
                        + e.getMessage());
            }
        }
        try {
            MetacatAdmin.updateUpgradeStatus("configutil.upgrade.java.status",
                                                MetacatAdmin.SUCCESS, persist);
        } catch (Exception e) {
            logMetacat.warn("DBAdmin.upgradeDatabase - couldn't update the status of the "
                            + "upgrading database process since " + e.getMessage());
        }
        

        // update the db version to be the metacat version
        try {
            databaseVersion = new DBVersion(SystemUtil.getMetacatVersion().getVersionString());
        } catch (PropertyNotFoundException pnfe) {
            throw new AdminException("DBAdmin.upgradeDatabase - Couldn't set the database version since: "
                    + pnfe.getMessage());
        }catch (NumberFormatException nfe) {
            throw new AdminException("DBAdmin.upgradeDatabase - Bad version format numbering: "
                    + nfe.getMessage());
        }
        
    }

    /**
     * Runs the commands in a sql script. Individual commands are loaded into a
     * string vector and run one at a time.
     * @param sqlFileName
     *            the name of the file holding the sql statements that need to
     *            get run.
     */
    public void runSQLFile(String sqlFileName) throws AdminException, SQLException {

        // if update file does not exist, do not do the update.
        if (FileUtil.getFileStatus(sqlFileName) < FileUtil.EXISTS_READABLE) {
            throw new AdminException("Could not read sql update file: "
                    + sqlFileName);
        }

        Connection connection = null;
        try {
            connection = DBUtil.getConnection(PropertyService
                    .getProperty("database.connectionURI"), PropertyService
                    .getProperty("database.user"), PropertyService
                    .getProperty("database.password"));
            connection.setAutoCommit(false);

            // load the sql from the file into a vector of individual statements
            // and execute them.
            logMetacat.debug("DBAdmin.runSQLFile - processing File: " + sqlFileName);
            Vector<String> sqlCommands = loadSQLFromFile(sqlFileName);
            for (String sqlStatement : sqlCommands) {
                Statement statement = connection.createStatement();
                logMetacat.debug("executing sql: " + sqlStatement);
                try {
                    statement.execute(sqlStatement);
                } catch (SQLException sqle) {
                    // Oracle complains if we try and drop a sequence (ORA-02289) or a
                    // trigger (ORA-04098/ORA-04080) or a table/view (ORA-00942)
                    //or and index (ORA-01418) that does not exist.  We don't care if this happens.
                    if (sqlStatement.toUpperCase().startsWith("DROP") &&
                            (sqle.getMessage().contains("ORA-02289") ||
                             sqle.getMessage().contains("ORA-04098") ||
                             sqle.getMessage().contains("ORA-04080") ||
                             sqle.getMessage().contains("ORA-00942"))) {
                        logMetacat.warn("DBAdmin.runSQLFile - did not process sql drop statement: "
                                        + sqle.getMessage());
                    } else {
                        throw sqle;
                    }
                }
            }
            connection.commit();
            
        } catch (IOException ioe) {
            throw new AdminException("DBAdmin.runSQLFile - Could not read SQL file"
                    + ioe.getMessage());
        } catch (PropertyNotFoundException pnfe) {
            throw new AdminException("DBAdmin.runSQLFile - Could not find property to run SQL file"
                    + pnfe.getMessage());
        } catch (SQLException sqle) {
            if (connection != null) {
                connection.rollback();
            }
            throw sqle;
        } finally {
            if (connection != null) {
                connection.close();
            }
        }
    }

    /**
     * Very basic utility to read sql from a file and return a vector of the
     * individual sql statements. This ignores any line that starts with /* or *.
     * It strips anything following --. Sql is parsed by looking for lines that
     * start with one of the following identifiers: INSERT, UPDATE, ALTER,
     * CREATE, DROP, BEGIN and COMMIT. It then assumes that everything until the
     * line that ends with ; is part of the sql, excluding comments.
     * @param sqlFileName
     *            the name of the file to read.
     * @return a vector holding the individual sql statements.
     */
    public Vector<String> loadSQLFromFile(String sqlFileName)
            throws IOException {

        // this will end up holding individual sql statements
        Vector<String> sqlCommands = new Vector<String>();

        FileInputStream fin = null;
        try {
            fin = new FileInputStream(sqlFileName);

            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    fin));

            // Read in file
            String fileLine;
            while ((fileLine = reader.readLine()) != null) {
                String endChar = ";";
                String trimmedLine = fileLine.trim();

                // get the first word on the line
                String firstWord = trimmedLine;
                if (trimmedLine.indexOf(' ') > 0) {
                    firstWord = trimmedLine.substring(0, trimmedLine
                            .indexOf(' '));
                }
                if (firstWord.endsWith(endChar)) {
                    firstWord = firstWord.substring(0, firstWord.indexOf(endChar));
                }

                // if the first word is a known sql command, start creating a
                // sql statement.
                if (sqlCommandSet.contains(firstWord.toUpperCase())) {
                    String sqlStatement = "";

                    // keep reading lines until we find one that is not a
                    // comment and ends with endChar
                    do {
                        String trimmedInnerLine = fileLine.trim();
                        
                        // if there is a BEGIN or DECLARE statement, we are now in plsql and we're
                        // using the '/' character as our sql end delimiter.
                        if (trimmedInnerLine.toUpperCase().equals("BEGIN")  ||
                                trimmedInnerLine.toUpperCase().startsWith("BEGIN ")  ||
                                trimmedInnerLine.toUpperCase().equals("DECLARE")  ||
                                trimmedInnerLine.toUpperCase().startsWith("DECLARE ")) {
                            endChar = "/";
                        }
                        
                        // ignore comments and empty lines
                        if (trimmedInnerLine.matches("^$")
                                || trimmedInnerLine.matches("^\\*.*")
                                || trimmedInnerLine.matches("/\\*.*")) {
                            continue;
                        }

                        // get rid of any "--" comments at the end of the line
                        if (trimmedInnerLine.indexOf("--") >= 0) {
                            trimmedInnerLine = trimmedInnerLine.substring(0,
                                    trimmedInnerLine.indexOf("--")).trim();
                        }
                        if (sqlStatement.length() > 0) {
                            sqlStatement += " ";
                        }
                        sqlStatement += trimmedInnerLine;
                        if (trimmedInnerLine.endsWith(endChar)) {
                            sqlStatement =
                                sqlStatement.substring(0, sqlStatement.length() - 1);
                            sqlCommands.add(sqlStatement);
                            break;
                        }
                    } while ((fileLine = reader.readLine()) != null);
                }
            }
            fin.close();
        } finally {
            IOUtils.closeQuietly(fin);
        }

        return sqlCommands;
    }

    /**
     * Validate the most important configuration options submitted by the user.
     * @return a vector holding error message for any fields that fail
     *         validation.
     */
    protected Vector<String> validateOptions(HttpServletRequest request) {
        Vector<String> errorVector = new Vector<String>();

        // TODO MCD validate options.

        return errorVector;
    }
}
