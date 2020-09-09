/**
 *  '$RCSfile$'
 *    Purpose: A Class that represents an XML element and its contents,
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

import java.sql.*;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.TreeSet;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A Class that represents an XML element and its contents,
 * and can build itself from a database connection
 */
public class ElementNode extends BasicNode {

  private static Log logMetacat = LogFactory.getLog(ElementNode.class);

  /** 
   * Construct a new ElementNode instance, and recursively create its children
   *
   * @param nodeRecordList the nodedata to use to initialize, which is a
   *        TreeSet of NodeRecord objects
   * @param nodeid the identifier for the node to be created
   */
  public ElementNode (TreeSet nodeRecordList, long nodeid) {

    // Step through all of the node records we were given
    Iterator it = nodeRecordList.iterator();
    while (it.hasNext()) {
      NodeRecord currentNode = (NodeRecord)it.next();
      if (currentNode.getNodeId() == nodeid) {
        logMetacat.info("Got Node ID: " + currentNode.getNodeId() +
                          " (" + currentNode.getParentNodeId() +
                          ", " + currentNode.getNodeIndex() + 
                          ", " + currentNode.getNodeType() + ")");
        // Process the current node
        setNodeType(currentNode.getNodeType());
        setNodeID(currentNode.getNodeId());
        setParentID(currentNode.getParentNodeId());
        setTagName(currentNode.getNodeName());
      } else {
        // Process the children nodes
        if (currentNode.getParentNodeId() == getNodeID()) {
        	logMetacat.info("  Processing child: " + currentNode.getNodeId() +
                          " (" + currentNode.getParentNodeId() +
                          ", " + currentNode.getNodeIndex() + 
                          ", " + currentNode.getNodeType() + ")");

          if ((currentNode.getNodeType()).equals("ELEMENT")) {
        	logMetacat.info("Creating child node: " + currentNode.getNodeId());
            ElementNode child = new ElementNode(nodeRecordList,
                                                currentNode.getNodeId());
            addChildNode(child);
          } else if (currentNode.getNodeType().equals("ATTRIBUTE")) {
            setAttribute(currentNode.getNodeName(),currentNode.getNodeData());
          } else if (currentNode.getNodeType().equals("TEXT")) {
            TextNode child = new TextNode(currentNode.getNodeId(),
                                          currentNode.getParentNodeId(),
                                          currentNode.getNodeData());
            addChildNode(child);
          } else if (currentNode.getNodeType().equals("COMMENT")) {
            CommentNode child = new CommentNode(currentNode.getNodeId(),
                                                currentNode.getParentNodeId(),
                                                currentNode.getNodeData());
            addChildNode(child);
          } else if (currentNode.getNodeType().equals("PI")) {
            PINode child = new PINode(currentNode.getNodeId(),
                                      currentNode.getParentNodeId(),
                                      currentNode.getNodeName(),
                                      currentNode.getNodeData());
            addChildNode(child);
          }

        } else {
        	logMetacat.info("  Discarding child: " + currentNode.getNodeId() +
                          " (" + currentNode.getParentNodeId() +
                          ", " + currentNode.getNodeIndex() +
                          ", " + currentNode.getNodeType() + ")");
        }
      }
    }
  }

  /** 
   * String representation for display purposes (recursively descends through
   * children to create an XML subtree)
   */
  public String toString () {

    StringBuffer value = new StringBuffer();
    String nodetype = getNodeType();

    if (nodetype.equals("ELEMENT")) {
      value.append('<');
      value.append(getTagName());
      value.append(getAttributes().toString());
      value.append('>');
    } 

    // Process children recursively here
    BasicNode child = null;
    Enumeration e = getChildren();
    while (e.hasMoreElements()) {
      child = (BasicNode)e.nextElement(); 
      value.append(child);
    }

    if (nodetype.equals("ELEMENT")) {
      value.append("</");
      value.append(getTagName());
      value.append('>');
    }

    return value.toString();
  }
}
