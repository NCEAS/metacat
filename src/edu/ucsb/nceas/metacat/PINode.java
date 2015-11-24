/**
 *  '$RCSfile$'
 *    Purpose: A Class that represents an XML PI node and its contents,
 *             and can build itself from a database connection
 *  Copyright: 2000 Regents of the University of California and the
 *             National Center for Ecological Analysis and Synthesis
 *    Authors: Matt Jones
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

/**
 * A Class that represents an XML PI node and its contents,
 */
public class PINode extends BasicNode {

    private String      nodeData = null;

    /** 
     * Construct a new PINode instance
     *
     * @param nodeid the id for the node to be created
     * @param parentnodeid the id of the parent node
     * @param nodename the name of the PI node
     * @param nodedata the contents of the PI node
     */
    public PINode (long nodeid, long parentnodeid, String nodename,
                          String nodedata) {
      setNodeID(nodeid);
      setParentID(parentnodeid);
      setTagName(nodename);
      setNodeData(nodedata);
      setNodeType("PI");
    }

    /** Set the node data to the given string */
    public void setNodeData(String nodedata) {
      this.nodeData = nodedata;
    }

    /** Get the node data as a string value */
    public String getNodeData() {
      return nodeData;
    }

    /** 
     * String representation of this text node
     */
    public String toString () {
        return ("<?" + getTagName() + " " + nodeData + " ?>");
    }
}

