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


import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;

import org.dataone.cn.indexer.parser.utility.LeafElement;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Used by FilterCommonRootSolrField.
 *
 * Assembled a query string based on a set of filters in a DataONE collection document.
 *
 * @author slaughter
 *
 * Based on CommonRootSolrField by sroseboo
 *
 */
public class FilterRootElement {

    private String name;
    private String xPath;
    private XPathExpression xPathExpression = null;
    private String delimiter = " ";
    private List<LeafElement> leafs = new ArrayList<LeafElement>();
    private List<FilterRootElement> subRoots = new ArrayList<FilterRootElement>();
    private List<FilterProcessor> filters = null;
    private String prefixMatch = null;
    private String fixedTerm = null;
    private String postfixMatch = null;

    public FilterRootElement() {
    }

    /**
     *
     * @param docOrNode - An XML document root or sub-node of a DataONE collection document
     * @return the value of the query derived from the document
     * @throws XPathExpressionException
     *
     * <p>
     * This method parses a DataONE collection document and builds a Solr query from the filters
     * defined in the document. The filters define the set of DataONE pids that are of interest
     * to the outhor of the collection (portal). The Solr query provides a way for client
     * programs to fetch the same set of pids that are defined by the filters.
     * </p>
     * <p>
     * One example client use case for this Solr 'collectionQuery' field is to retrive all
     * the pids for a collection and run metadata quality scores on them, to allow the portal
     * users to determine the quality of the metadata for their collection.
     * </p>
     */
    public String getRootValues(Object docOrNode)
            throws XPathExpressionException {

        NodeList nodeList = (NodeList) getxPathExpression().evaluate(docOrNode,
                XPathConstants.NODESET);

        String prefilterValue = null;
        String postfilterValue = null;

        String filterValue = null;
        List<FilterProcessor> filters = getFilters();
        prefixMatch = getPrefixMatch();
        fixedTerm = getFixedTerm();
        postfixMatch = getPostfixMatch();

        // A typical query prefilter: (isPartOf:urn\:uuid\:349aa330-4645-4dab-a02d-3bf950cf708 OR seriesId:urn:uuid:8c63bc73-c60e-4082-8dc2-8e3ea20bd6e5)
        // A main filter: ((text:soil) AND (-(keywords:*soil layer*) AND -(attribute:*soil layer*)) AND ((dateUploaded:[1800-01-01T00:00:00Z TO 2009-12-31T23:59:59Z])
        // A 'fixed' filter: AND (-obsoletedBy:* AND formatType:METADATA))
        // A 'poastfix' filter: OR (id:urn:uuid:298073d0-dc2b-4f59-bf35-f7a8e60efa0e OR id:urn:uuid:6c6040ef-1393-47f9-a725-645f125f61ef)
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

        String mainFilterValue = null;
        String completeFilterValue = null;
        String operator = "AND";
        Boolean prefilter;
        Boolean postfilter;
        int nFilters = filters.size();
        int iFilter;

        // Loop through the nodes that match the filter xpath, for example "//definition/booleanFilter | //definition/dateFilter | //definition/filter | //definition/numericFilter"
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);

            // For each node, search for a matching filter that can process that filter type
            // Only the first filter that matches is used
            for (FilterProcessor filterProcessor : filters) {
                if (node.getNodeName().equalsIgnoreCase(filterProcessor.getMatchElement())) {
                    //System.out.println("Running Filter processor name: " + filterProcessor.getName());
                    filterProcessor.initXPathExpressions();
                    filterValue = filterProcessor.getFilterValue(node);
                    break;
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

        // ( ((prefilter) OR (mail filters)) AND (fixedTerm)) OR (postfilter)
        // Add the prefilter value, if defined.
        if(prefilterValue != null) {
            completeFilterValue = "(" + prefilterValue + ")";
        }

        // Next add the main filter value, if defined.
        if(mainFilterValue != null) {
            if(completeFilterValue != null) {
                completeFilterValue = "(" + completeFilterValue + " OR " + "(" + mainFilterValue + "))";
            } else {
                completeFilterValue = "(" + mainFilterValue + ")";
            }
        }

        // Add the fixed terms
        if(fixedTerm != null) {
            if(completeFilterValue != null) {
                completeFilterValue = "(" + completeFilterValue + " AND " + fixedTerm + ")";
            } else {
                completeFilterValue = "(" + fixedTerm + ")";
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

        // This cause shouldn't happen (no terms found or specified), but check anyway
        if(completeFilterValue == null) {
            completeFilterValue = "(id:*)";
        }

        completeFilterValue = "(" + completeFilterValue + ")";

        return completeFilterValue;
    }

    public void initXPathExpressions(XPath xPathObject) {
        //System.out.println("FilterRootElement.initXPathExpressions");
        try {
            if (xPathExpression == null) {
                xPathExpression = xPathObject.compile(xPath);
            }
            for (LeafElement leaf : leafs) {
                leaf.initXPathExpression(xPathObject);
            }
            for (FilterRootElement subRoot : subRoots) {
                subRoot.initXPathExpressions(xPathObject);
            }
        } catch (XPathExpressionException e) {
            e.printStackTrace();
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getxPath() {
        return xPath;
    }

    public void setxPath(String xPath) {
        this.xPath = xPath;
    }

    public XPathExpression getxPathExpression() {
        return xPathExpression;
    }

    public void setxPathExpression(XPathExpression xPathExpression) {
        this.xPathExpression = xPathExpression;
    }

    public String getDelimiter() {
        return delimiter;
    }

    public void setDelimiter(String delimiter) {
        this.delimiter = delimiter;
    }

    public String getPrefixMatch() {
        return prefixMatch;
    }

    public void setPrefixMatch(String prefixMatch) {
        this.prefixMatch = prefixMatch;
    }

    public String getFixedTerm() {
        return fixedTerm;
    }

    public void setFixedTerm(String fixedTerm) {
        this.fixedTerm = fixedTerm;
    }

    public String getPostfixMatch() {
        return postfixMatch;
    }

    public void setPostfixMatch(String postfixMatch) {
        this.postfixMatch = postfixMatch;
    }

    public List<LeafElement> getLeafs() {
        return leafs;
    }

    public void setLeafs(List<LeafElement> leafs) {
        this.leafs = leafs;
    }

    public List<FilterRootElement> getSubRoots() {
        return subRoots;
    }

    public void setSubRoots(List<FilterRootElement> subRoots) {
        this.subRoots = subRoots;
    }

    public List<FilterProcessor> getFilters() {
        return filters;
    }

    public void setFilters(List<FilterProcessor> filters) {
        this.filters = filters;
    }
}

