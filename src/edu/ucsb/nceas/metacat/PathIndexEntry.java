package edu.ucsb.nceas.metacat;

import java.sql.Timestamp;


/**
 * PathIndexEntry contains all of the data fields needed to insert an path into
 * the xml_index table or the xml_path_index table, depending on which 
 * constructor is used.
 *
 * @author jones
 */
public class PathIndexEntry
{
    protected long nodeId;
    protected String path;
    protected String docid;
    protected String docType;
    protected long parentId;
    protected String nodeData;
    protected float nodeDataNumerical;
    protected Timestamp nodeDataDate;


    /**
     * Construct a new PathIndexEntry for the xml_index table.
     *
     * @param nodeId the node identifier
     * @param path the path in the xml document
     * @param docid the document identifier
     * @param docType the document type
     * @param parentId the identifier of the parent node
     */
    public PathIndexEntry(long nodeId, String path, String docid,
            String docType, long parentId)
    {
        this.nodeId = nodeId;
        this.path = path;
        this.docid = docid;
        this.docType = docType;
        this.parentId = parentId;
    }

    /**
     * Construct a new PathIndexEntry for the xml_path_index table.
     *
     * @param nodeId the node identifier
     * @param path the path in the xml document
     * @param docid the document identifier
     * @param docType the document type
     * @param parentId the identifier of the parent node
     * @param nodeData the node value as a string
     * @param nodeDataNumerical the node value as a double precision number
     */
    public PathIndexEntry(long nodeId, String path, String docid,
            long parentId, String nodeData, float nodeDataNumerical, Timestamp nodeDataDate )
    {
        this.nodeId = nodeId;
        this.path = path;
        this.docid = docid;
        this.nodeData = nodeData;
        this.nodeDataNumerical = nodeDataNumerical;
        this.nodeDataDate = nodeDataDate;
        this.parentId = parentId;        
    }
}
