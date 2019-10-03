package edu.ucsb.nceas.metacat.index.parser.utility;

/**
 *  Copyright: 2013 Regents of the University of California and the
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
    private String prefixMatch;
    private String postfix;
    private List<LeafElement> leafs = new ArrayList<LeafElement>();
    private List<FilterRootElement> subRoots = new ArrayList<FilterRootElement>();
    private List<FilterProcessor> filters = null;

    public FilterRootElement() {
    }

    public String getRootValues(Object docOrNode)
            throws XPathExpressionException {

        NodeList nodeList = (NodeList) getxPathExpression().evaluate(docOrNode,
                XPathConstants.NODESET);

        String preFilterStr = null;

        String filterValue = null;
        List<FilterProcessor> filters = getFilters();
        String prefixMatch = getPrefixMatch();

        // A typical query prefilter: (isPartOf:urn\:uuid\:349aa330-4645-4dab-a02d-3bf950cf708i)) OR
        // A main filter: ((text:soil) AND (-(keywords:*soil layer*) AND -(attribute:*soil layer*)) AND ((dateUploaded:[1800-01-01T00:00:00Z TO 2009-12-31T23:59:59Z])
        // A postfilter: AND (-obsoletedBy:* AND formatType:METADATA))
        // These are concatenated together to arrive at the full query

        // Collect the terms that are used to identify a 'prefilter' item. These terms will be added to
        // the front of the complete query and 'OR'd together
        HashSet<String> prefixMatchingFields = new HashSet<String>();
        if(prefixMatch != null && !prefixMatch.isEmpty()) {
            String tokens[] = prefixMatch.split(",");
            for (String token : tokens) {
                prefixMatchingFields.add(token);
            }
        }

        String completeFilterValue = null;
        String operator = "AND";
        Boolean prefilter;
        int nFilters = filters.size();
        int iFilter;
        Boolean preFilter;

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

            preFilter = false;
            // See if this filter value matches one of the 'prefix' filters, that will be 'OR'd
            // with the other filters. The 'prefilters' will be accumulated and prepended to the query
            // string, to be in sync with the way metacatui does things, and to make it easy to apply
            // the correct logical operators.
            if(!prefixMatchingFields.isEmpty()) {
                for(String term : prefixMatchingFields) {
                    // Only match the term if it is surrounded by non-alpha characters, i.e.
                    // term to match "id" is not embedded in another string such as "myId". That
                    // doesn't match, but "(id:1234)" does.
                    Pattern p = Pattern.compile("[^a-zA-Z0-9]" + term + "[^a-zA-Z0-9]");
                    Matcher m1 = null;
                    m1 = p.matcher(filterValue);
                    if(m1.find()) {
                        preFilter = true;
                        if(preFilterStr == null) {
                            preFilterStr = filterValue;
                        } else {
                            preFilterStr += " OR " + filterValue;
                        }
                        //System.out.println("matched term " + term + ", prefilter: " + preFilterStr);
                        continue;
                    }
                }
            }

            // If we found a prefilter item, don't add this portion to the completed query string yet. It
            // will be added after all other filters are processed.
            if(preFilter) {
                continue;
            }

            // The default operator to join search terms
            operator = " AND ";

            // Add this search term to the complete filter
            if(completeFilterValue == null) {
                completeFilterValue = filterValue ;
            } else {
                completeFilterValue += operator + filterValue;
            }
        }

        // Add the prefilter terms to the competed query
        // Only four possibilities - One of these cases has to be true
        if(preFilterStr != null && completeFilterValue != null) {
            completeFilterValue = "(" + preFilterStr + ") OR (" + completeFilterValue + ")";
        } else if(preFilterStr != null && completeFilterValue == null) {
            completeFilterValue = "(" + preFilterStr + ")";
        } else if(preFilterStr == null && completeFilterValue != null) {
            completeFilterValue = "(" + completeFilterValue + ")";
        } else if(preFilterStr == null && completeFilterValue == null) {
            // This case should never happen - no collection filters are defined!
            // Enter a value that will ensure that the query succeeds, but doesn't return any values.
            completeFilterValue = "(-id:*)";
        }

        // Add the postfilter terms
        if(postfix != null && !postfix.isEmpty()) {
            completeFilterValue = completeFilterValue + " " + postfix;
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

    public String getPostfix() {
        return postfix;
    }

    public void setPostfix(String postfix) {
        this.postfix = postfix;
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

