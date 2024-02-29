package edu.ucsb.nceas.metacat.admin.upgrade;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.ucsb.nceas.metacat.admin.AdminException;
import edu.ucsb.nceas.metacat.admin.DBAdmin;
import edu.ucsb.nceas.metacat.database.DBVersion;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;

/**
 * This class will ensure the upgrade process to 3.0.0 is from version 2.19.*.
 * @author tao
 *
 */
public class PrerequisiteChecker300 {
    private static Log logMetacat = LogFactory.getLog(PrerequisiteChecker300.class);

    /**
     * Check if the upgrade is starting from version 2.19.* 
     * @throws AdminException
     */
    public void check() throws AdminException {
        String error = "Metacat 3.* should be upgraded from 2.19.0. "
                                + "Please upgrade the Metacat instance to 2.19.0 first.";
        DBVersion version = DBAdmin.getInstance().getDBVersion();
        logMetacat.debug("The PrerequisiteChecker300.check - dbVersion from db is "
                                                                + version.getVersionString());
        try {
            DBVersion version200 = new DBVersion("2.0.0");
            DBVersion version219 = new DBVersion("2.19.0");
            DBVersion version000 = new DBVersion("0.0.0");
             //// 0.0.0 means a new fresh database (from scratch)
            if (version.compareTo(version200) < 0 && version.compareTo(version000) != 0) {
                throw new AdminException(error + " Also make sure it is compliance with DataONE API"
                                    + ", which means you should generate system metadata for the"
                                    + " existing objects. Details please see"
                                    + " https://knb.ecoinformatics.org/knb/docs"
                                    + "/dataone.html#generating-dataone-system-metadata");
            } else if (version.compareTo(version219) < 0 && version.compareTo(version000) != 0) {
                throw new AdminException(error);
            }
        } catch (PropertyNotFoundException e) {
            throw new AdminException(e.getMessage());
        }
    }

}
