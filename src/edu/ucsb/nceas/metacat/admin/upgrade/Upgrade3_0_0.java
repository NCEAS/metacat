package edu.ucsb.nceas.metacat.admin.upgrade;

import java.io.IOException;
import java.sql.SQLException;

import edu.ucsb.nceas.metacat.admin.AdminException;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;

/**
 * Run this class when the users upgrade their Metacat instances to 3.0.0.
 * @author tao
 *
 */
public class Upgrade3_0_0 implements UpgradeUtilityInterface {

    /**
     * This method will run the class - XMLNodesToFilesChecker, which basically makes sure that
     * every metadata document has been imported from xml_nodes or xml_nodes_revision tables to
     * the file system.
     */
    public boolean upgrade() throws AdminException {
        boolean success = true;
        try {
            XMLNodesToFilesChecker checker = new XMLNodesToFilesChecker();
            checker.check();
            String backupPath = PropertyService.getProperty("application.backupDir");
            DroppedTableBackupper300 backupper = new DroppedTableBackupper300(backupPath);
            backupper.backup();
        } catch (PropertyNotFoundException | SQLException | IOException e) {
            throw new AdminException(e.getMessage());
        }
        return success;
    }

}
