/**
 *  '$RCSfile$'
 *  Copyright: 2004 Regents of the University of California and the
 *             National Center for Ecological Analysis and Synthesis
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
