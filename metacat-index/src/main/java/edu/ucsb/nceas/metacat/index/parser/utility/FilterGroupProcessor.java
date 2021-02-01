package edu.ucsb.nceas.metacat.index.parser.utility;

/**
 *  Copyright: 2019 Regents of the University of California and the
 *             National Center for Ecological Analysis and Synthesis
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.xpath.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dataone.cn.indexer.parser.utility.LeafElement;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * Assembled a query string based on a set of filters in a DataONE collection document.
 * <p>
 * Used by FilterCommonRootSolrField.
 * Based on CommonRootSolrField by sroseboo
 * </p>
 * @author slaughter
 *
 */
public class FilterGroupProcessor {

    private String name;
    private String matchElement;
    private String delimiter = " ";
    private List<LeafElement> leafs = new ArrayList<LeafElement>();
    private List<FilterProcessor> filters = null;
    private Log log = LogFactory.getLog(FilterGroupProcessor.class);

    public FilterGroupProcessor () {
    }

    /**
     * Get the document processor values for the document or node
     *
     * @param docOrNode - An XML document root or sub-node of a DataONE collection document
     * @return the value of the query derived from the document
     * @throws XPathExpressionException
     *
     * <p>
     * This method parses a DataONE collection document and builds a Solr query from the filters
     * defined in the document. The filters define the set of DataONE pids that are of interest
     * to the author of the collection (portal). The Solr query provides a way for client
     * programs to fetch the same set of pids that are defined by the filters.
     * </p>
     * <p>
     * One example client use case for this Solr 'collectionQuery' field is to retrive all
     * the pids for a collection and run metadata quality scores on them, to allow the portal
     * users to determine the quality of the metadata for their collection.
     * </p>
     * <p>
     * Note that this method is responsible for parsing/processing all XML elements at the top
     * level of a collection/portal 'definition' (i.e. 'definition/filter', 'definition/numericFilter'),
     * including 'filterGroup' elements. As 'filterGroup' elements can be nested, this method can be called
     * recursively to process to descend into nested filterGroups.
     * </p>
     */
    public String getFilterGroupValue(Object docOrNode,
                                      List<FilterProcessor> filters,
                                      FilterGroupProcessor filterGroup,
                                      String prefixMatch,
                                      String postfixMatch,
                                      String xPath)
            throws XPathExpressionException {

        // Create XPath object
        XPathFactory xpathFactory = XPathFactory.newInstance();
        XPath xpath = xpathFactory.newXPath();
        XPathExpression xPathExpression = xpath.compile(xPath);

        log.trace("FilterGroupProcessor.getFilterGroupValue xpath: " + xPath);

        NodeList nodeList = (NodeList) xPathExpression.evaluate(docOrNode,
                XPathConstants.NODESET);

        String prefilterValue = null;
        String postfilterValue = null;
        String filterValue = null;
        String mainFilterValue = null;
        String completeFilterValue = null;
        Boolean prefilter;
        Boolean postfilter;

        // A typical query prefilter: (isPartOf:urn\:uuid\:349aa330-4645-4dab-a02d-3bf950cf708 OR seriesId:urn:uuid:8c63bc73-c60e-4082-8dc2-8e3ea20bd6e5)
        // A main filter: ((text:soil) AND (-(keywords:*soil layer*) AND -(attribute:*soil layer*)) AND ((dateUploaded:[1800-01-01T00:00:00Z TO 2009-12-31T23:59:59Z])
        // A 'fixed' filter: AND (-obsoletedBy:* AND formatType:METADATA))
        // A 'postfix' filter: OR (id:urn:uuid:298073d0-dc2b-4f59-bf35-f7a8e60efa0e OR id:urn:uuid:6c6040ef-1393-47f9-a725-645f125f61ef)
        // These are concatenated together to arrive at the full query:
        //     (((<prefix filter) OR (<main filter>)) AND (<fixed filter>)) OR (<postfix filter)

        // Collect the terms that are used to identify a 'prefilter' item. These terms will be added to
        // the front of the complete query and 'OR'd together
        HashSet<String> prefixMatchingFields = new HashSet<String>();
        if(prefixMatch != null && !prefixMatch.isEmpty()) {
            String tokens[] = prefixMatch.split(",");
            for (String token : tokens) {
                prefixMatchingFields.add(token);
            }
        }

        // Collect the terms that are used to identify a 'postfilter' item. These terms will be added to
        // the front of the complete query and 'OR'd together
        HashSet<String> postfixMatchingFields = new HashSet<String>();
        if(postfixMatch != null && !postfixMatch.isEmpty()) {
            String tokens[] = postfixMatch.split(",");
            for (String token : tokens) {
                postfixMatchingFields.add(token);
            }
        }

        String operator = "AND";
        Boolean exclude = false;

        // Find elements '<operator>', '<fieldsOperator>' // and '<exclude>' elements.
        XPath xpath2 = xpathFactory.newXPath();

        XPathExpression xPathExpression2 = xpath2.compile(".//definition/operator | operator");
        NodeList nl = (NodeList) xPathExpression2.evaluate(docOrNode, XPathConstants.NODESET);
        if (nl.getLength() > 0) {
            operator = nl.item(0).getTextContent();
        }

        xPathExpression2 = xpath2.compile(".//definition/exclude | exclude");
        nl = (NodeList) xPathExpression2.evaluate(docOrNode, XPathConstants.NODESET);
        if (nl.getLength() > 0) {
            exclude = Boolean.parseBoolean(nl.item(0).getTextContent());
        }

        // Loop through the nodes that match the filter xpath, for example "//definition/booleanFilter | //definition/dateFilter | //definition/filter | //definition/numericFilter"
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);

            Boolean foundNode = false;
            // For each node, search for a matching filter that can process that filter type
            // Only the first filter that matches is used
            for (FilterProcessor filterProcessor : filters) {
                if (node.getNodeName().equalsIgnoreCase(filterProcessor.getMatchElement())) {
                    foundNode = true;
                    filterProcessor.initXPathExpressions();
                    filterValue = filterProcessor.getFilterValue(node);
                    log.trace("returned filterValue: " + filterValue);
                    break;
                }
            }

            // Process a '<filterGroup>', if that is the currnt node
            if(!foundNode) {
                if (node.getNodeName().equalsIgnoreCase(filterGroup.getMatchElement())) {
                    filterValue = filterGroup.getFilterGroupValue(node, filters, filterGroup, prefixMatch, postfixMatch, xPath);
                    log.trace("returned filterGroup filterValue: " + filterValue);
                }
            }

            // If no value was returned from the filter, then go to the next node;
            if(filterValue == null)
                continue;

            prefilter = false;
            postfilter = false;
            // See if this filter value matches one of the 'prefix' filters, that will be 'OR'd
            // with the other filters. The 'prefilters' will be accumulated and prepended to the query
            // string, to be in sync with the way metacatui does things, and to make it easy to apply
            // the correct logical operators.
            if(!prefixMatchingFields.isEmpty()) {
                for(String term : prefixMatchingFields) {
                    // Only match the term if it is preceded by a "(" or " " and followed by a ":"
                    // Example: 'id' matches '(id:10)' or 'id:10', but doesn't match 'myId:10'
                    Pattern p = Pattern.compile("[(-]" + term + ":" + "|" + "^" + term + ":");
                    Matcher m1 = null;
                    m1 = p.matcher(filterValue);
                    if(m1.find()) {
                        prefilter = true;
                        if(prefilterValue == null) {
                            prefilterValue = filterValue;
                        } else {
                            prefilterValue += " OR " + filterValue;
                        }
                        continue;
                    }
                }
            }

            // Check this filter for a match with the 'postfix' filter pattern.
            if(!postfixMatchingFields.isEmpty()) {
                for(String term : postfixMatchingFields) {
                    // Only match the term if it is surrounded by non-alpha characters, i.e.
                    // term to match "id" is not embedded in another string such as "myId". That
                    // doesn't match, but "(id:1234)" does.
                    Pattern p = Pattern.compile("[(-]" + term + ":" + "|" + "^" + term + ":");
                    Matcher m1 = null;
                    m1 = p.matcher(filterValue);
                    if(m1.find()) {
                        postfilter = true;
                        if(postfilterValue == null) {
                            postfilterValue = filterValue;
                        } else {
                            postfilterValue += " OR " + filterValue;
                        }
                        continue;
                    }
                }
            }

            // If we found a prefilter item, don't add this portion to the completed query string yet. It
            // will be added after all other filters are processed.
            if(prefilter || postfilter) {
                continue;
            }

            // Add this search term to the complete filter
            if(mainFilterValue == null) {
                mainFilterValue = filterValue;
            } else {
                mainFilterValue += " " + operator + " " + filterValue;
            }
        }

        // Now assemble the complete query
        // (((prefilter) OR (main filters)) AND (fixedTerm)) OR (postfilter)
        // Add the prefilter value, if defined.
        if(prefilterValue != null) {
            completeFilterValue = "(" + prefilterValue + ")";
        }

        // Next add the main filter value, if defined.
        if(mainFilterValue != null) {
            if(completeFilterValue != null) {
                completeFilterValue = completeFilterValue + " OR " + "(" + mainFilterValue + ")";
            } else {
                completeFilterValue = mainFilterValue;
            }
        }

        // Add the postfix terms
        if(postfilterValue != null && !postfilterValue.isEmpty()) {
            if(completeFilterValue != null) {
                completeFilterValue = completeFilterValue + " OR " + "(" + postfilterValue + ")";
            } else {
                completeFilterValue = postfilterValue;
            }
        }

        completeFilterValue = "(" + completeFilterValue + ")";

        if (exclude) completeFilterValue = "-" + completeFilterValue;

        return completeFilterValue;
    }

    /**
     * Set the name of this processor
     * @return the name of this processor
     */
    public String getName() {
        return name;
    }

    /**
     * Get the name of this processor
     * @param name the name of this processor
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Get the 'leaf' elements defined for a filter
     * @return the leaf elements
     * @see "application-context-collection.xml"
     */
    public List<LeafElement> getLeafs() {
        return leafs;
    }

    /**
     * Get the 'leaf' elements defined for a filter
     * @param leafs the 'leaf' elements defined for a filter
     * @see "application-context-collection.xml"
     */
    public void setLeafs(List<LeafElement> leafs) {
        this.leafs = leafs;
    }

    /**
     * Get all defined filter processors
     * @return all defined filter processors
     * @see "application-context-collection.xml"
     */
    public List<FilterProcessor> getFilters() {
        return filters;
    }

    /**
     * Get all defined filter processors
     * @param filters all defined filter processors
     * @see "application-context-collection.xml"
     */
    public void setFilters(List<FilterProcessor> filters) {
        this.filters = filters;
    }

    /**
     * Get the string used to match this filter to an application context description
     * @return the string used to match this filter to an application context description
     * @see "application-context-collections.xml"*
     */
    public String getMatchElement() {
        return matchElement;
    }

    /**
     * Set the string used to match this filter to an application context description
     * @param matchElement
     */
    public void setMatchElement(String matchElement) {
        this.matchElement = matchElement;
    }
}