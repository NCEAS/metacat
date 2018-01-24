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
 * The AdvancedSearchPathQuery class holds the data needed to produce a 
 * valid PathQuery XML string.
 */
public class AdvancedSearchPathQuery  {
  
  // Object variables
  private String indent;                       // String of spaces
  private final int initialLength = 500;       // Initial length of stringBuffer
  private StringBuffer stringBuffer;           // Holds the pathquery xml
  private ArrayList returnFieldList = new ArrayList(); // List of returnfields
  private AdvancedSearchQueryGroup queryGroup;         // The outer query group
  private String title;                                // The pathquery title
  

  /**
   * Constructor. Initializes the pathquery title, the main query group, and the
   * indent string.
   * 
   * @param title         the title of the pathquery
   * @param queryGroup    the main query group
   * @param indent        a string of spaces used for indenting output
   */
  public AdvancedSearchPathQuery(final String title, 
                                 final AdvancedSearchQueryGroup queryGroup, 
                                 final String indent) {
    this.title = title;
    this.queryGroup = queryGroup;
    this.indent = indent;
    addReturnField("dataset/title");
    addReturnField("originator/individualName/surName");
    addReturnField("dataset/creator/individualName/surName");
    addReturnField("originator/organizationName");
    addReturnField("creator/organizationName");
    addReturnField("keyword");
  }
  

  /**
   * Adds a returnfield to the pathquery xml.
   * 
   * @param s       the name of the returnfield to add
   */
  public void addReturnField(final String s) {
    returnFieldList.add(s);
  }
  

  /**
   * Creates the pathquery xml string.
   * 
   * @return  a string holding the PathQuery XML.
   */
  public String toString() {
    String returnField;

    stringBuffer = new StringBuffer(initialLength);
    stringBuffer.append("<?xml version=\"1.0\"?>\n");
    stringBuffer.append("<pathquery version=\"1.2\">\n");
    stringBuffer.append(indent + "<querytitle>" + 
                        title + "</querytitle>\n");

    for (int i = 0; i < returnFieldList.size(); i++) {
      returnField = (String) returnFieldList.get(i);
      stringBuffer.append(indent + "<returnfield>" + 
                          returnField + "</returnfield>\n");
    }
    
    stringBuffer.append(queryGroup.toString());
    stringBuffer.append("</pathquery>\n");

    return stringBuffer.toString();
  }
  
}