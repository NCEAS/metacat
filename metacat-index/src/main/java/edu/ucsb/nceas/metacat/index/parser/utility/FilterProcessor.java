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


import org.dataone.cn.indexer.parser.utility.LeafElement;
import org.w3c.dom.Node;

import javax.xml.xpath.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Used by FilterRootElement.
 *
 * For each matching 'filter' element in a document, build a query term from it. These individual
 * query terms will assembled into a complete query by calling routine.
 *
 * Each filter type can have a set of leaf elements that will be used to build the search term
 * contributed by that filter to the entire query being assembled.
 * Each filter can have parameters defined that control how the query is built.
 *
 * @author slaughter
 *
 */
public class FilterProcessor {

    private String name;
    private String matchElement;
    private Boolean escapeSpecialChars = false;
    private Boolean quoteMultipleWords = false;
    private String xPath;
    private XPathExpression xPathExpression = null;
    private String delimiter;
    private List<LeafElement> leafs = new ArrayList<LeafElement>();
    private String template;
    private String defaults;
    private HashMap<String, String> defaultValues = new HashMap<String, String>();
    private final String DEFAULT_OPERATOR = "AND";

    public FilterProcessor() {
    }

    /**
     *
     * @param node the XML node that the filter will be applied to
     * @return the query term produced by this filter
     * @throws XPathExpressionException
     */
    public String getFilterValue(Node node) throws XPathExpressionException {

        //System.out.println("FilterProcessor.getFilterValues");
        Map<String, String> leafDelimeters = new HashMap<String, String>();
        // Leaf names that have corresponding, present values in the XML
        Set<String> leafNames = new HashSet<String>();
        // All possible leaf names, every ones that don't appear in the template
        Set<String> allLeafNames = new HashSet<String>();

        // These are the leaf possible values that also have values in the corresponding xml
        HashMap<String, String> leafValues = new HashMap<String, String>();

        //System.out.println("getting filter values for node: " + node.getNodeName());
        String value = null;
        String filterValue = null;
        name = getName();
        int nFiltersOut = 1;

        Pattern pattern;
        //String delimeter;
        Boolean excludeCondition = false;
        Boolean matchSubstring = false;

        // Multiple templates may exist for each filter type. The processor will attempt to fill out each one in turn. The
        // first one that is filled out completely will be used. Each template is parsed for the alphanumeric words to determine
        // what needs to be filled in, i.e. words in the template match element names (and there values) in the filter XML.
        template = getTemplate();
        defaults = getDefaults();
        if(defaults != null && ! defaults.isEmpty()) {
            for (String term : defaults.split(",")) {
                String [] tokens = term.split(":");
                defaultValues.put(tokens[0], tokens[1]);
            }
        }

        // Start with only one copy of the template. If a 'concatenated' leaf value is found, we
        // may have to make another pass.
        int nTemplate = 1;
        int totalTemplateCount = 1;
        Boolean moreTemplates = true;
        while(moreTemplates) {
            String thisFilterValue = null;
            // Assume we aren't going to find another 'concatenated' leaf value
            moreTemplates = false;
            // Loop through each leaf bean that may have the corresponding element in the XML
            for (LeafElement leafElement : getLeafs()) {
                String leafName = leafElement.getName();
                allLeafNames.add(leafName);
                value = leafElement.getLeafValue(node);

                // If a leaf value doesn't exist, use a default if specified.
                if (value == null || value.isEmpty()) {
                    // Use default value for a leaf
                    if (defaultValues.containsKey(leafName)) {
                        leafValues.put(leafName, defaultValues.get(leafName));
                        leafNames.add(leafName);
                    } else {
                        // No supplied name, nor default for this leaf, so skip it.
                        continue;
                    }
                } else {
                    // Found a value for this leaf, use it.
                    leafNames.add(leafName);

                    // Check for a couple of special leaf values, specifically
                    // <exclude>true</exclude> and <matchSubstring>true</matchSubstring>
                    if(leafName.compareToIgnoreCase("exclude") == 0) {
                        if(Boolean.parseBoolean(value))
                            excludeCondition = true;
                    } else if(leafName.compareToIgnoreCase("matchSubstring") == 0) {
                        if(Boolean.parseBoolean(value))
                            matchSubstring = true;
                    }

                    // Now check if this is a 'compound' leaf value - the LeafElement
                    // class will concatenate multiple values together if a LeafElement repeats,
                    // for exmaple, <field>keywords</field><field>attribute</field> are returned by
                    // LeafElement as 'keywords--attribute'.
                    String delimeter = leafElement.getDelimiter();
                    if(delimeter != null && ! delimeter.isEmpty()) {
                        String thisLeafValues[] = value.split(delimeter);
                        int valueCount = thisLeafValues.length;
                        if (valueCount > 1) {
                            // put the nth compound leaf value into the nth template
                            value = thisLeafValues[nTemplate - 1];
                            leafValues.put(leafName, value);
                            if (valueCount > totalTemplateCount) {
                                moreTemplates = true;
                                totalTemplateCount = valueCount;
                            }
                        } else
                            leafValues.put(leafName, value);
                    } else
                        leafValues.put(leafName, value);
                }
            }

            // Multiple templates may be defined in the Spring context file, so choose which one matches the
            // XML element. The first one that matches is used, so order of the templates in the context file
            // is important.
            thisFilterValue = selectTemplate(leafNames, allLeafNames);

            // Now apply the leaf values for the leaves that were present
            for (String name : leafNames) {
                value = leafValues.get(name);
                // If the 'matchSubstring' modifier is true, then have to surround
                // the 'value' item with '*'.
                if(matchSubstring) {
                    if(name.compareToIgnoreCase("value") == 0) {
                        value = "*" + value + "*";
                    }
                }
                // Apply the filter 'value' modifiers, if they are specified in the XML, to the
                // <value> element.
                if(name.compareToIgnoreCase("value") == 0) {
                    if(escapeSpecialChars) value = escapeSpecialChars(value);
                    if(quoteMultipleWords) value = checkQuoting(value);
                }
                thisFilterValue = applyTemplate(name, value, thisFilterValue);
            }

            // Apply the 'exclude' modifier, if it was present in the XML
            if(excludeCondition) {
                thisFilterValue = "-" + thisFilterValue;
            }

            // Are multiple versions of this filter being made, i.e. different 'name', same 'value'
            // Example:
            //         <dateFilter>
            //            <field>dateUploaded</field>
            //            <field>beginDate</field>
            //            <min>1800-01-01T00:00:00Z</min>
            //            <max>2009-01-01T00:00:00Z</max>
            //        </dateFilter>
            if(nTemplate > 1) {
                String operator = null;
                if(leafValues.containsKey("operator")) {
                    operator = leafValues.get("operator");
                } else {
                    // If an operator wasn't defined for this filter, we have to use a default,
                    // otherwise the query that is build will be syntactilly invalid.
                    operator = DEFAULT_OPERATOR;
                }

                filterValue = filterValue + " " + operator + " " + thisFilterValue;
            } else {
                filterValue = thisFilterValue;
            }

            nTemplate += 1;
            // Failsafe - this should never be true
            if(nTemplate > totalTemplateCount) {
                break;
            }
        }

        if(totalTemplateCount > 1) {
            filterValue = "(" + filterValue + ")";
        }
        filterValue = filterValue.trim();
        //System.out.println("    * * * * Final filter value: " + filterValue);

        return filterValue;
    }

    /**
     *
     * <p>
     * Templates are used to convert XML elements, attributes and values into the form defined by
     * the template.
     * </p>
     * <p>The 'leaf' values for a filter are compared to the tokens in the template to find a match</p>
     * @param leafNames - XML 'leaf' elements available to the filter
     * @param allLeafNames - all possible leaf name values
     * @return
     */
    private String selectTemplate(Set<String> leafNames, Set<String> allLeafNames)  {
        String selectedTemplate = null;

        // Loop through each template, and select the first one where all the values in the template
        // are filled in.

        String templateValues [] = template.split(",");
        // Only one template specified, so use it.
        if (templateValues.length == 1) {
            return template;
        }

        String tokens;

        // Regex to extract all alphanumeric words from a template
        Pattern p = Pattern.compile("[a-zA-Z]+");
        Matcher m1 = null;

        // Loop through each template until we find one that can be filled
        // out completely
        Boolean matchAll = false;
        for(String tval : templateValues) {
            m1 = p.matcher(tval);
            // Check each alphanumeric value in the template and see if there
            // is a match with a leaf name. If one is missing, then go to the
            // next template
            while (m1.find()) {
                Boolean foundMatch = false;
                matchAll = false;
                String thisToken = m1.group();
                // If this token is not one of the possible leaf values, then skip it - it is
                // another alphanumeric token in the template
                if(!allLeafNames.contains(thisToken)) {
                    continue;
                }

                for (String lname : leafNames) {
                    if(thisToken.compareToIgnoreCase(lname) == 0) {
                        foundMatch = true;
                        continue;
                    }
                }
                // One template term didn't match, go to the next template
                if(!foundMatch) break;
                matchAll = true;
            }
            if(matchAll) {
                selectedTemplate = tval;
                //System.out.println("Selecting template: " + tval);
                break;
            }
        }

        if(! matchAll) {
            selectedTemplate = templateValues[0];
            //System.out.println("Can't find template match, using template: " + selectedTemplate);
        }

        return selectedTemplate;
    }

    /**
     *
     * Fill in the template with the appropriate name and value
     * @param name the template name, i.e. 'name:value'
     * @param value the template value
     * @param filterValue the resulting filter value after the name and value have been substituted in
     * @return
     */
    private String applyTemplate(String name, String value, String filterValue) {
        HashMap<String, String> thisMap;
        Pattern pattern;

        if(value == null || value.isEmpty()) return filterValue;
        filterValue = applyItem(name, value, filterValue);

        return filterValue;
    }

    /**
     *
     * Apply one change to the template
     * @param name the filter name
     * @param value the value element of the filter
     * @param filterValue the current value of the resulting filter
     * @return the filter with the item filled in
     */
    private String applyItem(String name, String value, String filterValue) {

       filterValue = filterValue.replace(name, value);
       return filterValue;

    }

    private String escapeSpecialChars(String value) {
        value = value.replace("%7B", "\\%7B");
        value = value.replace("%7D", "\\%7D");
        value = value.replace("%3A", "\\%3A");
        value = value.replace(":", "\\:");
        value = value.replace("(", "\\(");
        value = value.replace(")", "\\)");
        value = value.replace("?", "\\?");
        value = value.replace("%3F", "\\%3F");
        value = value.replace("\"", "\\\"");
        value = value.replace("'", "\\'");

        return value;
    }

    private String checkQuoting(String value) {

        // Match one or more spaces, reluctantly
        Pattern p = Pattern.compile("\\s+?");
        Matcher m1 = null;

        m1 = p.matcher(value);
        if (m1.find()) {
            value = "\"" + value + "\"";
        }

        return value;
    }

    public void initXPathExpressions() {
        // Create XPathFactory object
        XPathFactory xpathFactory = XPathFactory.newInstance();
        // Create XPath object
        XPath xpath = xpathFactory.newXPath();

        for(LeafElement leafElement : getLeafs()) {
            try {
                XPathExpression expr = xpath.compile(leafElement.getxPath());
                leafElement.setxPathExpression(expr);
            } catch (XPathExpressionException e) {
                e.printStackTrace();
            }
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

    public String getDefaults() {
        return defaults;
    }

    public void setDefaults(String defaults) {
        this.defaults = defaults;
    }

    public Boolean getEscapeSpecialChars() {
        return escapeSpecialChars;
    }

    public void setEscapeSpecialChars(Boolean escapeSpecialChars) {
        this.escapeSpecialChars = escapeSpecialChars;
    }

    public Boolean getQuoteMultipleWords() {
        return quoteMultipleWords;
    }

    public void setQuoteMultipleWords(Boolean quoteMultipleWords) {
        this.quoteMultipleWords = quoteMultipleWords;
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

    public String getTemplate() {
        return template;
    }

    public void setTemplate(String template) {
        this.template = template;
    }

    public String getMatchElement() {
        return matchElement;
    }

    public void setMatchElement(String matchElement) {
        this.matchElement = matchElement;
    }

    public List<LeafElement> getLeafs() {
        return leafs;
    }

    public void setLeafs(List<LeafElement> leafs) {
        this.leafs = leafs;
    }
}

