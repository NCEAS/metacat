package edu.ucsb.nceas.metacat.index.resourcemap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dataone.cn.index.processor.IndexVisibilityDelegateHazelcastImpl;
import org.dataone.service.types.v1.Identifier;


/**
 * This class represents a IndexVisibility object when the archived objects are visible on the solr server.
 * It overrides the IndexVisibilityDelegateHazelcastImpl on d1-cn-index-processor
 * @author tao
 *
 */
public class IndexVisibilityHazelcastImplWithArchivedObj extends IndexVisibilityDelegateHazelcastImpl {
    
    private static Log log = LogFactory.getLog(IndexVisibilityHazelcastImplWithArchivedObj.class);
    
    /**
     * Default constructor
     */
    public IndexVisibilityHazelcastImplWithArchivedObj() {
    }

    /**
     * Determine if the given pid is visible on the solr server.
     * Since we keep the archived object on the solr server, it always return true. Its supper class
     * returns false when an object is archived.
     * @param pid the given pid 
     * @return true
     */
    @Override
    public boolean isDocumentVisible(Identifier pid) {
        log.debug("IndexVisibilityHazelcastImplWithArchivedObj.isDocumentVisible --- always return true");
        boolean visible = true;
        return visible;
    }

}
