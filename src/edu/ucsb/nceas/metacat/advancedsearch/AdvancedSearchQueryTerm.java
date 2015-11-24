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

/** 
 * @author dcosta
 * 
 * The AdvancedSearchQueryTerm class holds the data needed to produce a xml 
 * string fragment representing a single queryterm in a querygroup.
 */
public class AdvancedSearchQueryTerm  {

  // Object variables
  private final String caseSensitive;//Case sensitive setting, "true" or "false"
  private final String indent;       // String of spaces for indenting output
  private final int initialLength = 100; // Initial length of the stringBuffer
  private final String pathExpr;         // The search field, e.g. "keyword"
  private final String searchMode;   // The search mode, e.g. "less-than"
  private StringBuffer stringBuffer; // Holds the queryterm xml string
  private final String value;        // The search value to match, e.g. "35"


  /**
   * Constructor. Initializes searchMode, caseSensitive, value, and indent.
   * 
   * @param searchMode       The search mode, e.g. "less-than-equals"
   * @param caseSensitive    Case sensitive setting, "true" or "false"
   * @param pathExpr         The search field, e.g. "northBoundingCoordinate"
   * @param value            The search value to match, e.g. "35"
   * @param indent           String of spaces for indenting output
   */
  public AdvancedSearchQueryTerm(final String searchMode, 
                                 final String caseSensitive, 
                                 final String pathExpr, 
                                 final String value, 
                                 final String indent
                         ) {
    this.searchMode = searchMode;
    this.caseSensitive = caseSensitive;
    this.pathExpr = pathExpr;
    this.value = value;
    this.indent = indent;
    stringBuffer = new StringBuffer(initialLength);
  }
  

  /**
   * Produce a xml string fragment that represents this queryterm.
   * 
   * @return    A xml string fragment that represents this queryterm.
   */
  public String toString() {
    stringBuffer.append(indent + 
                        "<queryterm searchmode=\"" + 
                        searchMode + 
                        "\" casesensitive=\"" + 
                        caseSensitive + 
                        "\">\n"
                       );

    stringBuffer.append(indent + "  <value>" + value + "</value>\n");

    // For a simple search or a browse search, the pathExpr string will be "".
    if (!pathExpr.equals("")) {
      stringBuffer.append(indent + "  <pathexpr>" + pathExpr + "</pathexpr>\n");
    }
    
    stringBuffer.append(indent + "</queryterm>\n");

    return stringBuffer.toString();
  }

}