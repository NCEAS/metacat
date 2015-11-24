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

import java.io.Serializable;

/**
 * @author dcosta
 *
 * The AdvancedSearchBean class stores query properties and values for
 * conducting an advanced Metacat search.
 */
public class AdvancedSearchBean implements Serializable {
  
  static final long serialVersionUID = 0;  // Needed for Eclipse warning.

  // Possible values for the various ___QueryType fields. These are translated
  // to the various "searchmode" values in the Metacat pathquery.
  public static final int CONTAINS = 0;     // LIKE "%word%"
  public static final int EXACT_MATCH = 1; 	// LIKE = "word"
  public static final int STARTS_WITH = 2;	// LIKE "word%"
  public static final int ENDS_WITH = 3;    // LIKE "%word"
	
  // Possible values for subjectAllAny field
  public static final int MATCH_ALL = 0;
  public static final int MATCH_ANY = 1;
	
  private boolean boundaryContained;
  private boolean caseSensitive;
  private String  creatorOrganization;
  private int     creatorOrganizationQueryType;
  private String  creatorSurname;
  private int     creatorSurnameQueryType;
  private String  dateField;      // "PUBLICATION", "COLLECTION", or "ALL"
  private String  eastBound;
  private String  endDate;
  private int     formAllAny;     // MATCH_ALL or MATCH_ANY
  private String  locationName;
  private String  namedTimescale;
  private int     namedTimescaleQueryType;
  private String  northBound;
  private String  southBound;
  private String  siteValue;
  private String  startDate;
  private int     subjectAllAny;  // MATCH_ALL or MATCH_ANY
  private String  subjectField;   // "ALL", "TITLE", "ABSTRACT", or "KEYWORD"
  private int     subjectQueryType;
  private String  subjectValue;
  private String  taxon;
  private int     taxonQueryType;
  private String  westBound;


  /**
   * @return Returns the creatorOrganization.
   */
  public String getCreatorOrganization() {
    return creatorOrganization == null ? null : creatorOrganization.trim();
  }
  
  
  /**
   * @return Returns the creatorOrganizationQueryType.
   */
  public int getCreatorOrganizationQueryType() {
    return creatorOrganizationQueryType;
  }
  
  
  /**
   * @return Returns the creatorSurname.
   */
  public String getCreatorSurname() {
    return creatorSurname == null ? null : creatorSurname.trim();
  }
  
  
  /**
   * @return Returns the creatorSurnameQueryType.
   */
  public int getCreatorSurnameQueryType() {
    return creatorSurnameQueryType;
  }
  
  
  /**
   * @return Returns the dateField. Possible values: "PUBLICATION", "COLLECTION" 
   */
  public String getDateField() {
    return dateField == null ? null : dateField.trim();
  }
  
  
  /**
   * @return Returns the eastBound.
   */
  public String getEastBound() {
    return eastBound;
  }
  
  
  /**
   * @return Returns the endDate.
   */
  public String getEndDate() {
    return endDate == null ? null : endDate.trim();
  }
  
  
  /**
   * @return Returns the formAllAny value.  Possible values are 0 or 1.      
   */
  public int getFormAllAny() {
    return formAllAny;
  }
  
  
  /**
   * @return Returns the locationName.
   */
  public String getLocationName() {
    return locationName == null ? null : locationName.trim();
  }
  
  
  /**
   * @return Returns the namedTimescale.
   */
  public String getNamedTimescale() {
    return namedTimescale == null ? null : namedTimescale.trim();
  }
  
  
  /**
   * @return Returns the namedTimescaleQueryType.
   */
  public int getNamedTimescaleQueryType() {
    return namedTimescaleQueryType;
  }
  
  
  /**
   * @return Returns the northBound.
   */
  public String getNorthBound() {
    return northBound;
  }
  
  
  /**
   * @return Returns the siteValue string.
   */
  public String getSiteValue() {
    return siteValue;
  }
  
  
  /**
   * @return Returns the southBound.
   */
  public String getSouthBound() {
    return southBound;
  }
  
  
  /**
   * @return Returns the startDate.
   */
  public String getStartDate() {
    return startDate == null ? null : startDate.trim();
  }
  
  
  /**
   * @return Returns the subjectAllAny value.  Possible values are 0 or 1.      
   */
  public int getSubjectAllAny() {
    return subjectAllAny;
  }
  
  
  /**
   * @return Returns the subjectField.  Possible values are:
   *         "ALL", "TITLE", "ABSTRACT", "KEYWORD"
   */
  public String getSubjectField() {
    return subjectField;
  }
  
  
  /**
   * @return Returns the subjectQueryType. Possible values are:
   * 0 (contains), 1 (exact match), 2 (starts with), 3 (ends with).
   */
  public int getSubjectQueryType() {
    return subjectQueryType;
  }
  
  
  /**
   * @return Returns the subjectValue.
   */
  public String getSubjectValue() {
    return subjectValue == null ? null : subjectValue.trim();
  }
  
  
  /**
   * @return Returns the taxon.
   */
  public String getTaxon() {
    return taxon == null ? null : taxon.trim();
  }
  
  
  /**
   * @return Returns the taxonConditonType.
   */
  public int getTaxonQueryType() {
    return taxonQueryType;
  }
  
  
  /**
   * @return Returns the westBound.
   */
  public String getWestBound() {
    return westBound;
  }
  
  
  /**
   * @return Returns the boundaryContained value.
   */
  public boolean isBoundaryContained() {
    return boundaryContained;
  }
  

  /**
   * @return Returns the caseSensitive value;
   */
  public boolean isCaseSensitive() {
    return caseSensitive;
  }
  

  /**
   * Boolean to determine whether a string is empty. A string is considered to
   * be empty if its value is either null or "".
   * 
   * @param  s      the string to check
   * @return        true if the string is empty, else false.
   */
  public boolean isEmpty(String s) {
    if (s != null && !s.equals(""))
      return false;
    else
      return true;
  }
  
  
  /**
   * @return Returns the limitedByBoundaries.
   */
  public boolean isLimitedByBoundaries() {
    if (!isEmpty(this.eastBound) && !isEmpty(this.westBound)) {
      return true;
    }
    else if (!isEmpty(this.southBound) && !isEmpty(this.northBound)) {
      return true;
    }
    else {
      return false;
    }
  }

  
  /**
   * @return Returns the limitedByDate.
   */
  public boolean isLimitedByDate() {
    if (!isEmpty(this.endDate) || !isEmpty(this.startDate)) {
      return true;
    }
    else {
      return false;
    }
  }


  /**
   * @param boundaryContained The boundaryContained value to set.
   */
  public void setBoundaryContained(final boolean boundaryContained) {
    this.boundaryContained = boundaryContained;
  }
  
  
  /**
   * @param caseSensitive The caseSensitive value to set.
   */
  public void setCaseSensitive(final boolean caseSensitive) {
    this.caseSensitive = caseSensitive;
  }
  
  
  /**
   * @param creatorOrganization The creatorOrganization to set.
   */
  public void setCreatorOrganization(final String creatorField) {
    this.creatorOrganization = creatorField;
  }
  
  
  /**
   * @param creatorOrganizationQueryType The creatorOrganizationQueryType to set
   */
  public void setCreatorOrganizationQueryType(final int creatorConditionType) {
    this.creatorOrganizationQueryType = creatorConditionType;
  }
  
  
  /**
   * @param creatorSurname   The creatorSurname to set.
   */
  public void setCreatorSurname(final String creatorValue) {
    this.creatorSurname = creatorValue;
  }
  
  
  /**
   * @param creatorSurnameQueryType   The creatorSurnameQueryType to set.
   */
  public void setCreatorSurnameQueryType(final int creatorSurnameQueryType) {
    this.creatorSurnameQueryType = creatorSurnameQueryType;
  }


  /**
   * @param dateField The dateField to set.
   */
  public void setDateField(final String dateField) {
    this.dateField = dateField;
  }
  
  
  /**
   * @param eastBound The eastBound to set.
   */
  public void setEastBound(final String eastBound) {
    this.eastBound = eastBound;
  }
  
  
  /**
   * @param endDate The endDate to set.
   */
  public void setEndDate(final String endDate) {
    this.endDate = endDate;
  }
  
  
  /**
   * @return Sets the formAllAny value.  Possible values are
   *         MATCH_ALL (0) or MATCH_ANY (1).  
   */
  public void setFormAllAny(final int allAny) {
    this.formAllAny = allAny;
  }
  
  
  /**
   * @param locationName The locationName to set.
   */
  public void setLocationName(final String locationName) {
    this.locationName = locationName;
  }
  
  
  /**
   * @param namedTimescale The namedTimescale to set.
   */
  public void setNamedTimescale(final String namedTimescale) {
    this.namedTimescale = namedTimescale;
  }
  
  
  /**
   * @param namedTimescaleQueryType The namedTimescaleQueryType to set.
   */
  public void setNamedTimescaleQueryType(final int namedTimescaleQueryType) {
    this.namedTimescaleQueryType = namedTimescaleQueryType;
  }
  
  
  /**
   * @param northBound The northBound to set.
   */
  public void setNorthBound(final String northBound) {
    this.northBound = northBound;
  }

  
  /**
   * @param siteValue    the siteValue to set.
   */
  public void setSiteValue(final String siteValue) {
    this.siteValue = siteValue;
  }
  

  /**
   * @param southBound The southBound to set.
   */
  public void setSouthBound(final String southBound) {
    this.southBound = southBound;
  }
  
  
  /**
   * @param startDate The startDate to set.
   */
  public void setStartDate(final String startDate) {
    this.startDate = startDate;
  }
  
  
  /**
   * Sets the subjectAllAny value.  
   * 
   * @param allAny Possible values are MATCH_ALL (0) or MATCH_ANY (1).  
   */
  public void setSubjectAllAny(final int allAny) {
    this.subjectAllAny = allAny;
  }
  
  
  /**
   * @param subjectField The subjectField to set.
   */
  public void setSubjectField(final String subjectField) {
    this.subjectField = subjectField;
  }
  
  
  /**
   * @param subjectQueryType The subjectQueryType to set. Possible values are:
   *                         0 (contains), 1 (exact match), 2 (starts with), 
   *                         3 (ends with).   
   */
  public void setSubjectQueryType(final int subjectQueryType) {
    this.subjectQueryType = subjectQueryType;
  }
  
  
  /**
   * @param subjectValue The subjectValue to set.
   */
  public void setSubjectValue(final String subjectValue) {
    this.subjectValue = subjectValue;
  }
  
  
  /**
   * @param taxon The taxon to set.
   */
  public void setTaxon(final String taxon) {
    this.taxon = taxon;
  }
  
  
  /**
   * @param taxonQueryType The taxonQueryType to set.
   */
  public void setTaxonQueryType(final int taxonQueryType) {
    this.taxonQueryType = taxonQueryType;
  }
  
  
  /**
   * @param westBound The westBound to set.
   */
  public void setWestBound(final String westBound) {
    this.westBound = westBound;
  }

}
