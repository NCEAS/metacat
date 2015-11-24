/**
 *  '$RCSfile$'
 *    Purpose: A Class that sorts two NodeRecords
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

import java.util.Comparator;

/**
 * A utility class that sorts two node records.  
 * <p>
 * The order of the records
 * determines how the XML document is printed from DocumentImpl.toXml(),
 * so it is important that the sort order specified here results in a depth
 * first traversal of the nodes in tree.  Currently, the nodes are inserted
 * into the database in this depth-forst order, so the nodeid identifiers
 * are a good indicator of the proper sort order.
 * <p>
 * However, if we modify data loading semantics to allow document nodes to
 * be rearranged, or otherwise change the nodeindex value, this current
 * sort algorithm will fail to work.
 */
public class NodeComparator implements Comparator {

  static int LESS = -1;
  static int EQUALS = 0;
  static int GREATER = 1;

  /**
   * Constructor
   */
  public NodeComparator() {
  }

  /**
   * compare two objects to determine proper sort order -- delegates to 
   * the compare(NodeRecord, NodeRecord) method.
   */
  public int compare(Object o1, Object o2) {
    return compare((NodeRecord)o1, (NodeRecord)o2);
  }

  /**
   * compare two NodeRecord objects to determine proper sort order.  The 
   * node records are equal if their nodeid fields are equal.  One is
   * less than another if its parentnodeid is less, or if its parentnodeid
   * is equal and its nodeindex is less.  One is greater than another if
   * its parentnodeid is greater, or if its parentnodeid is equal and
   * its nodeindex is greater.
   */
  public int compare(NodeRecord o1, NodeRecord o2) {
    if (o1.getNodeId() == o2.getNodeId()) {
      return EQUALS;
    } else if (o1.getNodeId() < o2.getNodeId()) {
      return LESS;
    } else if (o1.getNodeId() > o2.getNodeId()) {
      return GREATER;

/*  // This is old code that used to sort the records into breadth-first
    // traversal order, based on the parentnodeid and the nodeindex.
    //
    if (o1.nodeid == o2.nodeid) {
      return EQUALS;
    } else if (o1.parentnodeid < o2.parentnodeid) {
      return LESS;
    } else if (o1.parentnodeid > o2.parentnodeid) {
      return GREATER;
    } else if (o1.parentnodeid == o2.parentnodeid) {
      if (o1.nodeindex < o2.nodeindex) {
        return LESS;
      } else if (o1.nodeindex > o2.nodeindex) {
        return GREATER;
      } else {
        // this should never happen because (parentnodeid,nodeindex) is unique
        return EQUALS;
      }
*/
    } else {
      // this should never happen because parentnodeid is always <,>, or =
      return EQUALS;
    }
  }
}
