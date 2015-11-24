/**
 *  '$RCSfile$'
 *  Copyright: 2005 University of New Mexico and the 
 *             Regents of the University of California and the
 *             National Center for Ecological Analysis and Synthesis
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
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package edu.ucsb.nceas.metacat.advancedsearch;

import java.util.ArrayList;

/**
 * @author dcosta
 * 
 * AdvancedSearchQueryGroup holds the data needed to produce a valid querygroup 
 * string. A querygroup is composed of one or more querygroups and/or 
 * queryterms.
 */
public class AdvancedSearchQueryGroup  {
  
  // Object variables
  private boolean includeOuterQueryGroup = true;
  private String indent;                // String of spaces for indenting output
  private final int initialLength = 500; // Initial length of the stringBuffer
  private String operator;              // "INTERSECT" or "UNION" operator
  private StringBuffer stringBuffer;    // Holds the querygroup string
  private ArrayList queryGroupList = new ArrayList(); // List of querygroups
  private ArrayList queryTermList = new ArrayList();  // List of queryterms


  /**
   * Constructor. Initializes the operator and the indent.
   * 
   * @param operator       Must be either "INTERSECT" or "UNION"
   * @param indent         A string of spaces for indenting the xml output
   */
  public AdvancedSearchQueryGroup(final String operator, final String indent) {
    this.operator = operator;
    this.indent = indent;
    
    if (!((operator.equals("INTERSECT")) || (operator.equals("UNION")))) {
      System.err.println("Invalid AdvancedSearchQueryGroup operator: " + 
                         operator);
    }
  }
  

  /**
   * Adds a AdvancedSearchQueryGroup to this AdvancedSearchQueryGroup's list of 
   * querygroups.
   * 
   * @param queryGroup   The AdvancedSearchQueryGroup object to be added to 
   *                     the list.
   */
  public void addQueryGroup(final AdvancedSearchQueryGroup queryGroup) {
    queryGroupList.add(queryGroup);
  }
  

  /**
   * Adds a AdvancedSearchQueryTerm to this AdvancedSearchQueryGroup's list of 
   * queryterms.
   * 
   * @param queryTerm   The AdvancedSearchQueryTerm object to be added to the 
   *                    list.
   */
  public void addQueryTerm(final AdvancedSearchQueryTerm queryTerm) {
    queryTermList.add(queryTerm);
  }
  
 
  /**
   * Sets the boolean value of includeOuterQueryGroup. This enables an
   * optimization. If the user enter search values for only one part of the
   * advanced search form, then includeOuterQueryGroup can be set to false.
   * When false, the QueryGroup object will omit the outer query group from
   * the PathQuery, resulting in a less nested SQL statement.
   * 
   * @param b  When false, allows the outer QueryGroup to be stripped off,
   *           resulting in a less nested SQL statement.
   */
  public void setIncludeOuterQueryGroup(boolean b) {
    this.includeOuterQueryGroup = b;
  }
  

  /**
   * Creates the XML string that represents this AdvancedSearchQueryGroup, 
   * including the querygroups and queryterms that are descendants of this 
   * querygroup.
   * 
   * @return    A XML string fragment representing this querygroup.
   */
  public String toString() {
    AdvancedSearchQueryGroup queryGroup;
    AdvancedSearchQueryTerm queryTerm;
    
    stringBuffer = new StringBuffer(initialLength);
    
    if (includeOuterQueryGroup == true) {
      stringBuffer.append(indent + 
                          "<querygroup operator=\"" + operator + "\">\n");
    }
    
    for (int i = 0; i < queryGroupList.size(); i++) {
      queryGroup = (AdvancedSearchQueryGroup) queryGroupList.get(i);
      stringBuffer.append(queryGroup.toString());
    }

    for (int i = 0; i < queryTermList.size(); i++) {
      queryTerm = (AdvancedSearchQueryTerm) queryTermList.get(i);
      stringBuffer.append(queryTerm.toString());
    }
    
    if (includeOuterQueryGroup == true) {
      stringBuffer.append(indent + "</querygroup>\n");
    }

    return stringBuffer.toString();
  }
  
}