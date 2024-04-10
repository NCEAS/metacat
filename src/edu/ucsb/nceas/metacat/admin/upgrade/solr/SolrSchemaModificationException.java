package edu.ucsb.nceas.metacat.admin.upgrade.solr;

import edu.ucsb.nceas.metacat.admin.AdminException;

/**
 * An exception happens when an administrator modified a the schema.xml in the solr home.
 * @author tao
 *
 */
public class SolrSchemaModificationException extends AdminException {
    
    /**
     * Constructor
     * @param error  the eror message.
     */
    public SolrSchemaModificationException(String error) {
        super(error);
    }
}
