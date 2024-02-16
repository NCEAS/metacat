package edu.ucsb.nceas.metacat.admin.upgrade.solr;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import edu.ucsb.nceas.metacat.admin.AdminException;
import edu.ucsb.nceas.metacat.admin.upgrade.UpgradeUtilityInterface;
import edu.ucsb.nceas.metacat.shared.ServiceException;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;

/**
 * A class to upgrade solr configuration files in the upgrade process to Metacat 3.0.0
 * @author tao
 *
 */
public class SolrUpgrade3_0_0 implements UpgradeUtilityInterface {

    private SolrSchemaUpgrader schemaUpgrader;
    private SolrConfigUpgrader configUpgrader;

    /**
     * Default constructor
     * @throws PropertyNotFoundException
     * @throws ServiceException
     */
    public SolrUpgrade3_0_0() throws PropertyNotFoundException, ServiceException {
        schemaUpgrader = new SolrSchemaUpgrader();
        configUpgrader = new SolrConfigUpgrader();
    }

    @Override
    public boolean upgrade() throws AdminException {
        try {
            schemaUpgrader.upgrade();
            configUpgrader.upgrade();
        } catch (NoSuchAlgorithmException | IOException e) {
            throw new AdminException(e.getMessage());
        }
        return true;
    }
}
