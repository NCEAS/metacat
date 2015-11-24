/**
 *  '$RCSfile$'
 *  Copyright: 2005 University of New Mexico and the 
 *             Regents of the University of California and the
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
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package edu.ucsb.nceas.metacat.advancedsearch;

import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;

import edu.ucsb.nceas.metacat.client.*;
import edu.ucsb.nceas.utilities.*;

/**
 * @author dcosta
 * 
 * AdvancedSearch class constructs queries that use the pathquery feature of 
 * Metacat. It can execute either an advanced search, where the user fills in
 * fields in a web form, or a simple search on a string.
 */
public class AdvancedSearch  {

  // Object variables
  private AdvancedSearchBean advancedSearchBean = null;
  private String caseSensitive;
  private final String globalOperator;
  private boolean hasSubjectSearch = false;
  private boolean hasAuthorSearch = false;
  private boolean hasSpatialSearch = false;
  private boolean hasTaxonomicSearch = false;
  private boolean hasTemporalSearch = false;
  private boolean hasSiteFilter = false;
  private int indentLevel = 2;
  private boolean isCaseSensitive = false;
  private AdvancedSearchPathQuery pathQuery;
  private AdvancedSearchQueryGroup queryGroup;
  private String queryString;
  private String site;
  private final String title = "Advanced Search";


  /**
   * Constructor. Used when the user has filled in an Advanced Search form.
   * The property values are contained in the advancedSearchBean object.
   * For a simple search, the advancedSearchBean object is passed as null.
   * 
   * @param advancedSearchBean   An AdvancedSearch bean.
   */
  public AdvancedSearch(final AdvancedSearchBean advancedSearchBean) {
    int allAny = AdvancedSearchBean.MATCH_ALL;
    String indent = getIndent(indentLevel * 1);
    
    if (advancedSearchBean != null) {
      allAny = advancedSearchBean.getFormAllAny();
      this.isCaseSensitive = advancedSearchBean.isCaseSensitive();
      site = advancedSearchBean.getSiteValue();
    }

    if (allAny == AdvancedSearchBean.MATCH_ALL) {
      globalOperator = "INTERSECT";
    }
    else {
      globalOperator = "UNION";
    }
    
    if (isCaseSensitive == true) {
      this.caseSensitive = "true";
    }
    else {
      this.caseSensitive = "false";
    }

    this.queryGroup = new AdvancedSearchQueryGroup(globalOperator, indent);
    this.pathQuery = new AdvancedSearchPathQuery(title, queryGroup, indent);
    this.advancedSearchBean = advancedSearchBean;
  }
  

  /**
   * Adds a string to an ArrayList of terms. An auxiliary method to the
   * parseTermsAdvanced() method.
   * 
   * @param terms      ArrayList of strings.
   * @param term       the new string to add to the ArrayList, but only if
   *                   it isn't an empty string.
   */
  private void addTerm(ArrayList terms, final StringBuffer term) {
    final String s = term.toString().trim();
      
    if (s.length() > 0) {
      terms.add(s);
    }
  }


  /**
   * A full subject query searches the title, abstract, and keyword sections of
   * the document. Individual searches on these sections is also supported.
   */
  private void buildQuerySubject() {
    int allAny = advancedSearchBean.getSubjectAllAny();
    String emlField;
    String indent;
    final String innerOperator = "UNION";
    AdvancedSearchQueryGroup innerQuery = null;
    final String outerOperator;
    AdvancedSearchQueryGroup outerQuery;
    AdvancedSearchQueryTerm qt;
    String searchMode;
    final String subjectField = advancedSearchBean.getSubjectField();
    final int subjectQueryType = advancedSearchBean.getSubjectQueryType();
    String term;
    ArrayList terms;
    String value = advancedSearchBean.getSubjectValue();
 
    if ((value != null) && (!(value.equals("")))) {
      hasSubjectSearch = true;

      if (allAny == AdvancedSearchBean.MATCH_ALL) {
        outerOperator = "INTERSECT";
      }
      else {
        outerOperator = "UNION";
      }

      indent = getIndent(indentLevel * 2);
      outerQuery = new AdvancedSearchQueryGroup(outerOperator, indent);
      terms = parseTermsAdvanced(value);
      searchMode = metacatSearchMode(subjectQueryType);
      
      for (int i = 0; i < terms.size(); i++) {
        term = (String) terms.get(i);
        indent = getIndent(indentLevel * 3);
        innerQuery = new AdvancedSearchQueryGroup(innerOperator, indent);
        indent = getIndent(indentLevel * 4);
            
        if (subjectField.equals("ALL") || subjectField.equals("TITLE")) {
          emlField = "dataset/title";
          qt = new AdvancedSearchQueryTerm(searchMode, caseSensitive, emlField, 
                                           term, indent);
          innerQuery.addQueryTerm(qt);
        }

        if (subjectField.equals("ALL") || subjectField.equals("ABSTRACT")) {
          emlField = "dataset/abstract/para";
          qt = new AdvancedSearchQueryTerm(searchMode, caseSensitive, emlField, 
                                           term, indent);
          innerQuery.addQueryTerm(qt);

          emlField = "dataset/abstract/section/para";
          qt = new AdvancedSearchQueryTerm(searchMode, caseSensitive, emlField, 
                                           term, indent);
          innerQuery.addQueryTerm(qt);
        }

        if (subjectField.equals("ALL") || subjectField.equals("KEYWORDS")) {
          emlField = "keywordSet/keyword";
          qt = new AdvancedSearchQueryTerm(searchMode, caseSensitive, emlField, 
                                           term, indent);
          innerQuery.addQueryTerm(qt);
        }
      
        outerQuery.addQueryGroup(innerQuery);
      }

      // Minimize the number of query groups that get created, depending on
      // which criteria the user specified.
      //
      if (terms.size() > 1) {
        queryGroup.addQueryGroup(outerQuery);
      }
      else if (terms.size() == 1){
        queryGroup.addQueryGroup(innerQuery);
      }
    }

  }
  

  /**
   * An author query will search the creator/individualName/surName field, the
   * creator/organizationName field, or an intersection of both fields.
   */
  private void buildQueryAuthor() {
    boolean addQueryGroup = false;
    final int creatorSurnameQueryType = 
                                advancedSearchBean.getCreatorSurnameQueryType();
    final int creatorOrganizationQueryType = 
                           advancedSearchBean.getCreatorOrganizationQueryType();
    String emlField;
    String indent = getIndent(indentLevel * 2);
    AdvancedSearchQueryGroup qg = 
                           new AdvancedSearchQueryGroup(globalOperator, indent);
    AdvancedSearchQueryTerm qt;
    String searchMode;
    String value = advancedSearchBean.getCreatorSurname();

    indent = getIndent(indentLevel * 3);
    if ((value != null) && (!(value.equals("")))) {
      emlField = "dataset/creator/individualName/surName";
      searchMode = metacatSearchMode(creatorSurnameQueryType);
      qt = new AdvancedSearchQueryTerm(searchMode, caseSensitive, emlField, 
                                       value, indent);
      qg.addQueryTerm(qt);        
      addQueryGroup = true;
    }

    value = advancedSearchBean.getCreatorOrganization();
      
    if ((value != null) && (!(value.equals("")))) {
      emlField = "creator/organizationName";
      searchMode = metacatSearchMode(creatorOrganizationQueryType);
      qt = new AdvancedSearchQueryTerm(searchMode, caseSensitive, emlField, 
                                       value, indent);
      qg.addQueryTerm(qt);        
      addQueryGroup = true;
    }
    
    if (addQueryGroup) {      
      hasAuthorSearch = true;
      queryGroup.addQueryGroup(qg);
    }
  }
  

  /**
   * Two kinds of spatial searches are supported. The first is on a specific
   * named location. The second is on north/south/east/west bounding
   * coordinates. An intersection of both searches is done if the user
   * specifies search values for a named location as well as one or more
   * bounding coordinates.
   */
  private void buildQuerySpatialCriteria() {
    boolean addBoundingValues = false;
    boolean addGeographicDescription = false;
    final boolean boundaryContained = advancedSearchBean.isBoundaryContained();
    String emlField;
    final String operator = "INTERSECT";
    String indent = getIndent(indentLevel * 2);
    AdvancedSearchQueryGroup qgBounding;
    AdvancedSearchQueryGroup qgGeographicDescription;
    AdvancedSearchQueryGroup qgSpatial;
    AdvancedSearchQueryTerm qt;
    String searchMode;
    String value, northValue, southValue, eastValue, westValue;

    qgSpatial = new AdvancedSearchQueryGroup(globalOperator, indent);
    indent = getIndent(indentLevel * 3);
    qgBounding = new AdvancedSearchQueryGroup(operator, indent);
    qgGeographicDescription = new AdvancedSearchQueryGroup(operator, indent);
    indent = getIndent(indentLevel * 4);

    /* Check whether user specified a named location. */
    value = advancedSearchBean.getLocationName();   

    if ((value != null) && (!(value.equals("")))) {
      searchMode = "contains";
      emlField = "geographicCoverage/geographicDescription";
      qt = new AdvancedSearchQueryTerm(searchMode, caseSensitive, emlField, 
                                       value, indent);
      qgGeographicDescription.addQueryTerm(qt);
      addGeographicDescription = true;
    }

    /*
     * If the user selects the boundaryContained checkbox, use the following
     * logical expression. N, S, E, and W are the boundaries of the bounding
     * box, while N', S', E', and W' are the boundaries specified in a given
     * EML document:
     *              (N' <= N) && (S' >= S) && (E' <= E) && (W' >= W)
     */
    if (boundaryContained) {
      northValue = advancedSearchBean.getNorthBound();
      if ((northValue != null) && (!(northValue.equals("")))) {
        emlField = 
               "geographicCoverage/boundingCoordinates/northBoundingCoordinate";
        searchMode = "less-than-equals";
        qt=new AdvancedSearchQueryTerm(searchMode,caseSensitive,emlField, 
                                       northValue, indent);
        qgBounding.addQueryTerm(qt);        
        addBoundingValues = true;
      }

      southValue = advancedSearchBean.getSouthBound();
      if ((southValue != null) && (!(southValue.equals("")))) {
        emlField = 
               "geographicCoverage/boundingCoordinates/southBoundingCoordinate";
        searchMode = "greater-than-equals";
        qt=new AdvancedSearchQueryTerm(searchMode,caseSensitive,emlField, 
                                       southValue, indent);
        qgBounding.addQueryTerm(qt);        
        addBoundingValues = true;
      }

      eastValue = advancedSearchBean.getEastBound();
      if ((eastValue != null) && (!(eastValue.equals("")))) {
        emlField =
                "geographicCoverage/boundingCoordinates/eastBoundingCoordinate";
        searchMode = "less-than-equals";
        qt = new AdvancedSearchQueryTerm(searchMode, caseSensitive, emlField, 
                                         eastValue, indent);
        qgBounding.addQueryTerm(qt);        
        addBoundingValues = true;
      }

      westValue = advancedSearchBean.getWestBound();
      if ((westValue != null) && (!(westValue.equals("")))) {
        emlField =
                "geographicCoverage/boundingCoordinates/westBoundingCoordinate";
        searchMode = "greater-than-equals";
        qt = new AdvancedSearchQueryTerm(searchMode, caseSensitive, emlField, 
                                         westValue, indent);
        qgBounding.addQueryTerm(qt);        
        addBoundingValues = true;
      }
    }
   /*
    * Else, if the user does not select the boundaryContained checkbox, use the 
    * following logical expression. N, S, E, and W are the boundaries of the 
    * bounding box, while N', S', E', and W' are the boundaries specified in a 
    * given EML document:
    *              (N' > S) && (S' < N) && (E' > W) && (W' < E)
    */
    else {     
      northValue = advancedSearchBean.getNorthBound();
      if ((northValue != null) && (!(northValue.equals("")))) {
        emlField =
               "geographicCoverage/boundingCoordinates/southBoundingCoordinate";
        searchMode = "less-than";
        qt = new AdvancedSearchQueryTerm(searchMode, caseSensitive, emlField, 
                                         northValue, indent);
        qgBounding.addQueryTerm(qt);        
        addBoundingValues = true;
      }

      southValue = advancedSearchBean.getSouthBound();
      if ((southValue != null) && (!(southValue.equals("")))) {
        emlField = 
               "geographicCoverage/boundingCoordinates/northBoundingCoordinate";
        searchMode = "greater-than";
        qt = new AdvancedSearchQueryTerm(searchMode, caseSensitive, emlField, 
                                         southValue, indent);
        qgBounding.addQueryTerm(qt);        
        addBoundingValues = true;
      }

      eastValue = advancedSearchBean.getEastBound();
      if ((eastValue != null) && (!(eastValue.equals("")))) {
        emlField =
                "geographicCoverage/boundingCoordinates/westBoundingCoordinate";
        searchMode = "less-than";
        qt = new AdvancedSearchQueryTerm(searchMode, caseSensitive, emlField, 
                                         eastValue, indent);
        qgBounding.addQueryTerm(qt);        
        addBoundingValues = true;
      }

      westValue = advancedSearchBean.getWestBound();
      if ((westValue != null) && (!(westValue.equals("")))) {
        emlField =
                "geographicCoverage/boundingCoordinates/eastBoundingCoordinate";
        searchMode = "greater-than";
        qt = new AdvancedSearchQueryTerm(searchMode, caseSensitive, emlField, 
                                         westValue, indent);
        qgBounding.addQueryTerm(qt);        
        addBoundingValues = true;
      }
    }

    // Minimize the number of query groups that get created, depending on
    // which criteria the user specified.
    //
    if (addBoundingValues || addGeographicDescription) {      
      hasSpatialSearch = true;
      
      if (addBoundingValues && addGeographicDescription) { 
        qgSpatial.addQueryGroup(qgBounding);
        qgSpatial.addQueryGroup(qgGeographicDescription);
        queryGroup.addQueryGroup(qgSpatial);
      }    
      else if (addBoundingValues) {
        queryGroup.addQueryGroup(qgBounding);
      }
      else {
        queryGroup.addQueryGroup(qgGeographicDescription);
      }
    }
    
  }
  

  /**
   * Two kinds of temporal searches are supported. The first is on a named
   * time scale. The second is on a specific start date and/or end date.
   */
  private void buildQueryTemporalCriteria() {
    boolean addQueryGroup = false;
    boolean addQueryGroupDates = false;
    boolean addQueryGroupNamed = false;
    final String dateField = advancedSearchBean.getDateField();
    String emlField;
    final String operator = "INTERSECT";
    String indent = getIndent(indentLevel * 2);
    final int namedTimescaleQueryType = 
                                advancedSearchBean.getNamedTimescaleQueryType();
    AdvancedSearchQueryGroup qg= new AdvancedSearchQueryGroup(operator, indent);
    AdvancedSearchQueryGroup qgNamed, qgDates, qgDatesStart, qgDatesEnd;
    AdvancedSearchQueryTerm qt;
    String searchMode;
    final String namedTimescale, startDate, endDate;

    indent = getIndent(indentLevel * 3);
    namedTimescale = advancedSearchBean.getNamedTimescale();
    startDate = advancedSearchBean.getStartDate();
    endDate = advancedSearchBean.getEndDate();

    /* If the user specified a named timescale, check to see whether it occurs
     * in any of three possible places: singleDateTime, beginDate, or endDate.
     */
    qgNamed = new AdvancedSearchQueryGroup("UNION", indent);
    if ((namedTimescale != null) && (!(namedTimescale.equals("")))) {
      indent = getIndent(indentLevel * 4);
      searchMode = metacatSearchMode(namedTimescaleQueryType);
      
      emlField = 
           "temporalCoverage/singleDateTime/alternativeTimeScale/timeScaleName";
      qt = new AdvancedSearchQueryTerm(searchMode, caseSensitive, emlField, 
                                       namedTimescale, indent);
      qgNamed.addQueryTerm(qt);
      
      emlField = 
   "temporalCoverage/rangeOfDates/beginDate/alternativeTimeScale/timeScaleName";
      qt = new AdvancedSearchQueryTerm(searchMode, caseSensitive, emlField, 
                                       namedTimescale, indent);
      qgNamed.addQueryTerm(qt);
      
      emlField = 
     "temporalCoverage/rangeOfDates/endDate/alternativeTimeScale/timeScaleName";
      qt = new AdvancedSearchQueryTerm(searchMode, caseSensitive, emlField, 
                                       namedTimescale, indent);
      qgNamed.addQueryTerm(qt);
      
      addQueryGroupNamed = true;
    }
    
    qgDates = new AdvancedSearchQueryGroup("INTERSECT", indent);

    // If a start date was specified, search for temporal coverage and/or a
    // pubDate greater than or equal to the start date.
    //
    if ((startDate != null) && (!(startDate.equals("")))) {
      indent = getIndent(indentLevel * 4);
      qgDatesStart = new AdvancedSearchQueryGroup("UNION", indent);
      indent = getIndent(indentLevel * 5);
      searchMode = "greater-than-equals";

      if (dateField.equals("ALL") || dateField.equals("COLLECTION")) {
        emlField = "temporalCoverage/rangeOfDates/beginDate/calendarDate";
        qt = new AdvancedSearchQueryTerm(searchMode, caseSensitive, emlField, 
                                         startDate, indent);
        qgDatesStart.addQueryTerm(qt);        

        emlField = "temporalCoverage/singleDateTime/calendarDate";
        qt = new AdvancedSearchQueryTerm(searchMode, caseSensitive, emlField, 
                                         startDate, indent);
        qgDatesStart.addQueryTerm(qt);
      }
      
      if (dateField.equals("ALL") || dateField.equals("PUBLICATION")) {
        emlField = "dataset/pubDate";
        qt = new AdvancedSearchQueryTerm(searchMode, caseSensitive, emlField, 
                                         startDate, indent);
        qgDatesStart.addQueryTerm(qt);        
      }
      
      qgDates.addQueryGroup(qgDatesStart);
      addQueryGroupDates = true;
    }

    // If an end date was specified, search for temporal coverage and/or a
    // pubDate less than or equal to the end date.
    //
    if ((endDate != null) && (!(endDate.equals("")))) {
      indent = getIndent(indentLevel * 4);
      qgDatesEnd = new AdvancedSearchQueryGroup("UNION", indent);
      indent = getIndent(indentLevel * 5);
      searchMode = "less-than-equals";

      if (dateField.equals("ALL") || dateField.equals("COLLECTION")) {
        emlField = "temporalCoverage/rangeOfDates/endDate/calendarDate";
        qt = new AdvancedSearchQueryTerm(searchMode, caseSensitive, emlField, 
                                         endDate, indent);
        qgDatesEnd.addQueryTerm(qt);        

        emlField = "temporalCoverage/singleDateTime/calendarDate";
        qt = new AdvancedSearchQueryTerm(searchMode, caseSensitive, emlField, 
                                         endDate, indent);
        qgDatesEnd.addQueryTerm(qt);
      }
      
      if (dateField.equals("ALL") || dateField.equals("PUBLICATION")) {
        emlField = "dataset/pubDate";
        qt = new AdvancedSearchQueryTerm(searchMode, caseSensitive, emlField, 
                                         endDate, indent);
        qgDatesEnd.addQueryTerm(qt);        
      }      

      qgDates.addQueryGroup(qgDatesEnd);
      addQueryGroupDates = true;
    }
    
    if (addQueryGroupNamed) {
      qg.addQueryGroup(qgNamed);
      addQueryGroup = true;
    }
    
    if (addQueryGroupDates) {
      qg.addQueryGroup(qgDates);
      addQueryGroup = true;
    }
    
    if (addQueryGroup) {
      hasTemporalSearch = true;
      queryGroup.addQueryGroup(qg);
    }

  }


  /**
   * A taxon query searches the taxonomicClassification/taxonRankValue field,
   * matching the field if the user-specified value is contained in the field.
   */
  private void buildQueryTaxon() {
    boolean addQueryGroup = false;
    final String emlField;
    String indent = getIndent(indentLevel * 2);
    final String operator = "INTERSECT";
    AdvancedSearchQueryGroup qg= new AdvancedSearchQueryGroup(operator, indent);
    AdvancedSearchQueryTerm qt;
    final String searchMode;
    int taxonQueryType = advancedSearchBean.getTaxonQueryType();
    final String value = advancedSearchBean.getTaxon();
      
    indent = getIndent(indentLevel * 3);

    if ((value != null) && (!(value.equals("")))) {
      emlField = "taxonomicClassification/taxonRankValue";
      searchMode = metacatSearchMode(taxonQueryType);
      qt = new AdvancedSearchQueryTerm(searchMode, caseSensitive, emlField, 
                                       value, indent);
      qg.addQueryTerm(qt);        
      addQueryGroup = true;
    }

    if (addQueryGroup) {      
      hasTaxonomicSearch = true;
      queryGroup.addQueryGroup(qg);
    }
  }
  

  /**
   * Build a site filter. If the AdvancedSearch's site value is non-null, add a
   * query group that limits the results to a particular LTER site. Do this
   * by searching for a packageId attribute that starts with "knb-lter-xyz"
   * where "xyz" is the three-letter site acronym, or for a site keyword
   * phrase (e.g. "Kellogg Biological Station") anywhere in the documment.
   */
  private void buildSiteFilter() {
    String attributeValue = "";
    String emlField = "";
    String indent = getIndent(indentLevel * 2);
    final LTERSite lterSite = new LTERSite(site);
    final String operator = "UNION";
    AdvancedSearchQueryGroup qg= new AdvancedSearchQueryGroup(operator, indent);
    AdvancedSearchQueryTerm qt;
    String searchMode;
    final String siteKeyword;
   
    if (lterSite.isValidSite()) {
      hasSiteFilter = true;
      indent = getIndent(indentLevel * 3);
      
      // Handle some LTER sites with irregular naming conventions in their EML
      // For CAP and CWT, we need to search on the system attribute rather than
      // the packageId attribute
      //
      if (site.equals("CAP") || site.equals("CWT")) {
        emlField = "/eml/@system";
        attributeValue = lterSite.getSystem();
      }
      else {
        // For other LTER sites, search the packageId
        emlField = "/eml/@packageId";
        attributeValue = lterSite.getPackageId();
      }
      
      searchMode = "starts-with";
      qt = new AdvancedSearchQueryTerm(searchMode, caseSensitive, emlField, 
                                       attributeValue, indent);
      qg.addQueryTerm(qt);        

      // Search for site keyword phrase
      siteKeyword = lterSite.getSiteKeyword();
      emlField = "";
      searchMode = "contains";
      qt = new AdvancedSearchQueryTerm(searchMode, caseSensitive, emlField, 
                                       siteKeyword, indent);
      qg.addQueryTerm(qt);    
      
      queryGroup.addQueryGroup(qg); 
    }
  }


  /**
   * Counts the number of search types on the form. For example, if the user
   * has filled in values for both a subject search and a spatial search,
   * would return 2.
   * 
   * @return  searchTypes  An integer representing the total number of searches
   *                       that the user has filled in for this advanced search.
   */
  private int countSearchTypes () {
    int searchTypes = 0;
    
    if (hasSubjectSearch == true)   { searchTypes++; }
    if (hasAuthorSearch == true)    { searchTypes++; }
    if (hasSpatialSearch == true)   { searchTypes++; }
    if (hasTaxonomicSearch == true) { searchTypes++; }
    if (hasTemporalSearch == true)  { searchTypes++; }
    if (hasSiteFilter == true)      { searchTypes++; }
    
    return searchTypes;
  }
  

  /**
   * Builds and runs an advanced search, returning HTML result string.
   * 
   * @param metacatURL  URL to the metacat servlet
   * @param metacat     A metacat client object, possible null.
   * @param qformat     The qformat (skin) to use when displaying results.
   * @param xslPath     File path to the resultset.xsl stylesheet.
   * @return htmlString HTML string representation of the search results.
   */
  public String executeAdvancedSearch(final String metacatURL,
                                      final Metacat metacat,
                                      final String qformat,
                                      final String xslPath) {
    String htmlString = "";
    int searchTypes;

    buildQuerySubject();
    buildQueryAuthor();
    buildQueryTaxon();
    buildQuerySpatialCriteria();
    buildQueryTemporalCriteria();
    buildSiteFilter();

    // Count the number of search types the user has entered.
    searchTypes = countSearchTypes();

    // If the user has entered values for only one type of search criteria,
    // then optimize the search by setting the QueryGroup object's
    // includeOuterQueryGroup to false. This will strip off the outer query
    // group and result in a more simplified SQL statement.
    //
    if (searchTypes == 1) {
      queryGroup.setIncludeOuterQueryGroup(false);
    }
    
    queryString = pathQuery.toString();
    htmlString = this.runQuery(metacatURL, metacat, qformat, xslPath);
    return htmlString;
  }
  

  /**
   * Builds and runs a simple search, returning HTML result string.
   * For a simple search, the AdvancedSearchBean object can be null because
   * all we need is a string value to search on.
   * 
   * @param metacatURL  URL to the metacat servlet
   * @param metacat     A metacat client object, possible null.
   * @param qformat     The qformat (skin) to use when displaying results.
   * @param xslPath     File path to the resultset.xsl stylesheet.
   * @param value       String value to search on.
   * 
   * @return htmlString HTML string representation of the search results.
   */
  public String executeSearch(final String metacatURL,
                              final Metacat metacat,
                              final String qformat,
                              final String xslPath,
                              String value) {
    String emlField = "";
    String htmlString = "";
    String indent = getIndent(indentLevel * 2);
    AdvancedSearchQueryTerm qt;
    String searchMode = "contains";
    
    indent = getIndent(indentLevel * 3);

    /* Check whether user specified an empty search string. 
     */
    if ((value == null) || (value.equals(""))) {
        value = "%";
    }

    qt = new AdvancedSearchQueryTerm(searchMode, caseSensitive, emlField, 
                                     value, indent);
    queryGroup.addQueryTerm(qt);
    queryString = pathQuery.toString();
    htmlString = this.runQuery(metacatURL, metacat, qformat, xslPath);
    return htmlString;
  }
  

  /**
   * Returns a string of spaces that corresponds to the current indent level.
   * 
   * @param indentLevel   The number of spaces to be indented.
   * @return              A string containing indentLevel number of spaces.
   */
  private String getIndent(final int indentLevel) {
    StringBuffer indent = new StringBuffer(12);
    
    for (int i = 0; i < indentLevel; i++) {
      indent.append(" ");
    }
    
    return indent.toString();
  }
  

  /**
   * Given a AdvancedSearchBean query type, return the corresponding Metacat
   * searchmode string.
   * 
   * @param queryType       An int indicating the query type as specified in
   *                        the AdvancedSearchBean class.
   * @return searchmode     A string, the Metacat search mode value.
   */ 
  private String metacatSearchMode(final int queryType) {
    final String searchMode;
    
    switch (queryType) {
      case AdvancedSearchBean.CONTAINS:
        searchMode = "contains";
        break;
      case AdvancedSearchBean.EXACT_MATCH:
        searchMode = "equals";
        break;
      case AdvancedSearchBean.STARTS_WITH:
        searchMode = "starts-with";
        break;
      case AdvancedSearchBean.ENDS_WITH:
        searchMode = "ends-with";
        break;
      default:
        searchMode = "contains";
        break;
    }
    
    return searchMode;
  }
  

  /**
   * Parses search terms from a string. In this simple implementation, the 
   * string is considered to be a list of tokens separated by spaces. The more 
   * advanced implementation (parserTermsAdvanced) parses quoted strings 
   * containing spaces as a term. This method can be eliminated if we are
   * satisfied that parseTermsAdvanced() is working properly.
   * 
   * @param  value    The string value as entered by the user.
   * 
   * @return terms    An ArrayList of String objects. Each space-separated 
   *                  token is a single term.
   *
  private ArrayList parseTerms(final String value) {
    StringTokenizer st;
    ArrayList terms = new ArrayList();
    String token;
    final int tokenCount;
    
    st = new StringTokenizer(value, " ");
    tokenCount = st.countTokens();
    
    for (int i = 0; i < tokenCount; i++) {
      token = st.nextToken();
      terms.add(token);
    }
    
    return terms;
  }
  */

  
  /**
   * Parses search terms from a string. In this advanced implementation,
   * double-quoted strings that contain spaces are considered a single term.
   * 
   * @param  value     The string value as entered by the user.
   * 
   * @return terms    An ArrayList of String objects. Each string is a term.
   */
  private ArrayList parseTermsAdvanced(String value) {
    char c;
    StringBuffer currentTerm = new StringBuffer(100);
    boolean keepSpaces = false;
    final int stringLength;
    ArrayList terms = new ArrayList();

    value = value.trim();
    stringLength = value.length();
    
    for (int i = 0; i < stringLength; i++) {
      c = value.charAt(i);
  
      if (c == '\"') {
        // Termination of a quote-enclosed term. Add the current term to the
        // list and start a new term.
        if (keepSpaces) {
          addTerm(terms, currentTerm);
          currentTerm = new StringBuffer(100);
        }
      
        keepSpaces = !(keepSpaces); // Toggle keepSpaces to its opposite value.
      }
      else if (c == ' ') {
        // If we are inside a quote-enclosed term, append the space.
        if (keepSpaces) {
          currentTerm.append(c);
        }
        // Else, add the current term to the list and start a new term.
        else {
          addTerm(terms, currentTerm);
          currentTerm = new StringBuffer(100);
        }
      }
      else {
        // Append any non-quote, non-space characters to the current term.
        currentTerm.append(c);
      }
    }

    // Add the final term to the list.
    addTerm(terms, currentTerm);

    return terms;
  }


  /**
   * Runs the Metacat query for a browse search, simple search, or advanced
   * search.
   * 
   * @param metacatURL  URL to the metacat servlet
   * @param metacat     A metacat client object, possible null.
   * @param qformat     The qformat (skin) to use when displaying results.
   * @param xslPath     File path to the resultset.xsl stylesheet.
   * @return htmlString HTML string representation of the search results.
   */
  private String runQuery(final String metacatURL, 
                          Metacat metacat, 
                          final String qformat,
                          final String xslPath) {
    String htmlString = "";
    Reader reader;
    String resultset = "";
    String sessionId;
    StringReader stringReader;
    Stylizer stylizer = new Stylizer();
    
    if (metacat == null) {
      try {
        metacat = MetacatFactory.createMetacatConnection(metacatURL);
      }
      catch (MetacatInaccessibleException mie) {
        System.err.println("Metacat Inaccessible:\n" + mie.getMessage());
      }
    }
    
    sessionId = metacat.getSessionId();
    
    try {
      System.err.println("Starting query...");
      stringReader = new StringReader(queryString);
      reader = metacat.query(stringReader);
      resultset = IOUtil.getAsString(reader, true);
      System.err.println("Query result:\n" + resultset);
      htmlString = stylizer.resultsetToHTML(resultset, sessionId, 
                                            metacatURL, qformat, xslPath);
    } 
    catch (Exception e) {
      System.err.println("General exception:\n" + e.getMessage());
      e.printStackTrace();
    }

    return(htmlString);
  }
  

  /**
   * Main program to run a test query from the command line.
   * 
   * Pass the server name, server port, and path to resultset.xsl as the first 
   * three command line arguments:
   * 
   * @param argv[0]   The server name, e.g. "earth.lternet.edu"
   * @param argv[1]   The server port, e.g. "8080", or 0 if no port needs
   *                    to be specified.
   * @param argv[2]   The context string, e.g. "knb"
   * @param argv[3]   The path to the resultset.xsl stylesheet, e.g.
   *                    "C:/Tomcat5/webapps/query/style/common/resultset.xsl"
   */
  public static void main(String[] argv) {
    AdvancedSearch advancedSearch;
    AdvancedSearchBean advancedSearchBean = new AdvancedSearchBean();
    String contextString = argv[2];
    MetacatHelper metacatHelper = new MetacatHelper();
    String metacatURL;
    String qformat = "lter";
    final String serverName = argv[0];
    final Integer serverPortInteger = new Integer(argv[1]);
    final int serverPort = serverPortInteger.intValue();
    final String xslPath = argv[3];

    advancedSearchBean.setSubjectField("ALL");
    advancedSearchBean.setSubjectValue("bird");    
    //advancedSearchBean.setCreatorSurname("Walsh");
    //advancedSearchBean.setCreatorSurnameQueryType(0);
    //advancedSearchBean.setCreatorOrganization("Georgia Coastal Ecosystems");    
    //advancedSearchBean.setTaxon("Crustacea");    
    //advancedSearchBean.setLocationName("Georgia");    
    //advancedSearchBean.setNorthBound("31.5");    
    //advancedSearchBean.setSouthBound("10"); 
    //advancedSearchBean.setEastBound("-50");
    //advancedSearchBean.setWestBound("-90");
    //advancedSearchBean.setNamedTimescale("Phanerozoic");
    //advancedSearchBean.setStartDate("2001-01-01");
    //advancedSearchBean.setEndDate("2001-07-01");
    advancedSearch = new AdvancedSearch(advancedSearchBean);
    metacatURL = 
        metacatHelper.constructMetacatURL(serverName, serverPort, contextString);
    advancedSearch.executeAdvancedSearch(metacatURL, null, qformat, xslPath);
  }
  
}
