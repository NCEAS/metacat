package edu.ucsb.nceas.metacat.admin.upgrade.solr;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.ucsb.nceas.metacat.admin.AdminException;
import edu.ucsb.nceas.metacat.admin.upgrade.UpgradeUtilityInterface;
import edu.ucsb.nceas.metacat.shared.ServiceException;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;

/**
 * A class to upgrade solr configuration files in the upgrade process to Metacat 3.0.0
 * @author tao
 *
 */
public class SolrSchemaAndConfigUpgrader implements UpgradeUtilityInterface {

    private SolrSchemaUpgrader schemaUpgrader;
    private SolrConfigUpgrader configUpgrader;
    private static SolrSchemaAndConfigUpgrader instance;
    private static boolean hasRun = false;
    private static Log logMetacat = LogFactory.getLog(SolrSchemaAndConfigUpgrader.class);

    /**
     * Default constructor
     * @throws PropertyNotFoundException
     * @throws ServiceException
     */
    private SolrSchemaAndConfigUpgrader() throws PropertyNotFoundException, ServiceException {
        schemaUpgrader = new SolrSchemaUpgrader();
        configUpgrader = new SolrConfigUpgrader();
    }

    /**
     * Get the singleton instance
     * @return the singleton instance of the SolrSchemaAndConfigUpgrader class
     * @throws PropertyNotFoundException
     * @throws ServiceException
     */
    public static SolrSchemaAndConfigUpgrader getInstance() throws PropertyNotFoundException,
                                                                                ServiceException {
        if (instance == null) {
            synchronized (SolrSchemaAndConfigUpgrader.class) {
                instance = new SolrSchemaAndConfigUpgrader();
            }
        }
        return instance;
    }

    @Override
    public boolean upgrade() throws AdminException {
        if (!hasRun) {
            logMetacat.debug("SolrSchemaAndConfigUpgrader.upgrade - Metacat should run the upgrade"
                                                + " classes since this is the first time to run.");
            try {
                configUpgrader.upgrade();
                schemaUpgrader.upgrade();
                hasRun = true;
            } catch (NoSuchAlgorithmException | IOException e) {
                throw new AdminException(e.getMessage());
            }
        } else {
            logMetacat.info("SolrSchemaAndConfigUpgrader.upgrade - Metacat should skip to run the"
                    + " upgrade classes since they already ran.");
        }
        return true;
    }

    /**
     * Set the SolrSchemaUpgrader instance. This method is for the test class only.
     * @param schemaUpgrader  the SolrSchemaUpgrader instance will be set
     */
    public void setSolrSchemaUpgrader(SolrSchemaUpgrader schemaUpgrader) {
        this.schemaUpgrader = schemaUpgrader;
    }

    /**
     * Set the SolrConfigUpgrader instance. This method is for the test class only.
     * @param configUpgrader  the SolrConfigUpgrader instance will be set.
     */
    public void setSolrConfigUpgrader(SolrConfigUpgrader configUpgrader) {
        this.configUpgrader = configUpgrader;
    }
}
