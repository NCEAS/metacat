package edu.ucsb.nceas.metacat.admin.upgrade;

/**
 * Historically, early versions of Metacat only store the metadata in the xml_nodes and 
 * xml_nodes_revsions tables. Now, we drop the both tables in Metacat 3.0.0. 
 * This class is to make sure that all metadata objects have been serialized into the files.
 * @author tao
 *
 */
public class DBtoFilesChecker {
    /**
     * Default constructor
     */
    public DBtoFilesChecker() {
        
    }
    
    /**
     * This method does the job - make sure all metadata object are stored in the files.
     * Note: this method must be called before running the 3.0.0 upgrade sql script, which will
     * drop the related tables.
     */
    public void check() {
        checkXmlDocumentsTable();
        checkXmlRevisionsTable();
    }
    
    /**
     * Check the objects in the xml_documents table
     */
    private void checkXmlDocumentsTable() {
        
    }
    
    /**
     * Check the objects in the xml_documents table
     */
    private void checkXmlRevisionsTable() {
        
    }

}
