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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dataone.cn.indexer.parser.utility.LeafElement;
import org.w3c.dom.Node;

import javax.xml.xpath.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * For each matching 'filter' element in a document, build a query term from it. These individual
 * query terms will assembled into a complete query by calling routine.
 * <p>
 *   Each filter type can have a set of leaf elements that will be used to build the search term
 *   contributed by that filter to the entire query being assembled.
 *   Each filter can have parameters defined that control how the query is built.
 * </p>
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
    private final String DEFAULT_FIELDS_OPERATOR = "AND";

    private Log log = LogFactory.getLog(FilterProcessor.class);

    public FilterProcessor() {
    }

    /**
     * Convert an XML '<filter>' entry into a Solr query string.
     * @param node the XML node (i.e. a "<filter>" entry in the colleciton document) that the filter will be applied to
     * @return the query term produced by this filter
     * @throws XPathExpressionException
     */
    public String getFilterValue(Node node) throws XPathExpressionException {

        HashMap<String, ArrayList<String>> leafValues = new HashMap<String, ArrayList<String>>();

        // Leaf names that have corresponding, present values in the XML
        Set<String> leafNames = new HashSet<String>();
        // All possible leaf names, every ones that don't appear in the template
        Set<String> allLeafNames = new HashSet<String>();

        String value = null;
        String completeFilterValue = null;
        name = getName();

        Boolean excludeCondition = false;
        Boolean matchSubstring = false;
        String operator = DEFAULT_OPERATOR;
        String fieldsOperator = DEFAULT_FIELDS_OPERATOR;

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

        // Assume we are only making one pass through this code that will apply the template, e.g. 'field:value'
        // If a 'concatenated' leaf value is found, we may have to make more passes. The concatenated value occurs when multiple
        // elements in the filter repeat, so the value returned by 'getLeafValue' will be a concatenation, i.e.
        //     <filter>
        //        <field>keyword</keyword>
        //        <field>text</text>
        //     ...
        //     </filter>
        //
        // will be returned as
        //     "keyword--text"
        // The same is true for <value> elements:
        //     <filter>
        //        ...
        //        <value>1234</text>
        //        <value>5678</text>
        //        ...
        //     </filter>
        //
        // will be returned as
        //     "1234--5678"

        // The <field> element has to occur at least once, but may occur multiple times, so a pass must be made for each <field>
        // value. In addition, the <value> element can occur 0 or more times, so a pass has to be made for each value of the <value>
        // field, for each value of the <field>, i.e. pass 1: 1st field, 1st value, pass 2: 1st field, 2nd value, pass 3: 1st field, 3rd value, etc.
        // So we have to keep indexes for both, keeping in mind that <value> may not have been specified in the filter.

        String thisFilterValue = null;
        // Assume we aren't going to find another 'concatenated' leaf value
        // Loop through each leaf bean that may have the corresponding element in the XML
        // These are the leafs that are specified in the application context file
        for (LeafElement leafElement : getLeafs()) {
            String leafName = leafElement.getName();
            log.trace("Processing leafname: " + leafName);
            allLeafNames.add(leafName);
            value = leafElement.getLeafValue(node);
            String delimeter = leafElement.getDelimiter();

            // If a leaf value doesn't exist, use a default value if specified.
            if (value == null || value.isEmpty()) {
                // Use default value for a leaf
                if (defaultValues.containsKey(leafName)) {
                    leafValues = addLeafValue(leafValues, leafName, defaultValues.get(leafName), delimeter);
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
                if (leafName.compareToIgnoreCase("exclude") == 0) {
                    if (Boolean.parseBoolean(value))
                        excludeCondition = true;
                } else if (leafName.compareToIgnoreCase("matchSubstring") == 0) {
                    if (Boolean.parseBoolean(value))
                        matchSubstring = true;
                } else if (leafName.compareToIgnoreCase("operator") == 0) {
                    operator = value;
                } else if (leafName.compareToIgnoreCase("fieldsOperator") == 0) {
                    fieldsOperator = value;
                }

                leafValues = addLeafValue(leafValues, leafName, value, delimeter);
            }
        }

        // Multiple templates may be defined in the Spring context file, so choose which one matches the
        // XML element. The first one that matches is used, so order of the templates in the context file
        // is important.
        String selectedTemplate = selectTemplate(leafNames, allLeafNames);

        for (String key: leafValues.keySet()) {
            log.trace("leafValues name: " + key);
        }
        // Now that we have all the leaf names and values, apply the template to each one
        int nFields = 0;
        int nValues = 0;

        // <field> element has to exist, as stated by the collections.xsd
        ArrayList currentValues = leafValues.get("field");
        nFields = currentValues.size();

        // <value> element might exist
        if(leafValues.containsKey("value")) {
            currentValues = leafValues.get("value");
            nValues = currentValues.size();
        } else {
            nValues = 1;
        }

        log.trace("nFields: " + nFields);
        log.trace("nValues: " + nValues);

        // Make a filter pass for each <field> entry
        for(int iField = 0; iField < nFields; iField++) {
            String subFilterValue = null;
            // If multiple values exist for this field, surround them with parenthesis.
            log.trace("iField: " + iField);
            // Make a pass for each <value> element, with the current <field>
            // if no <value> elements are present, then just make one pass (we may have <min>, <max>, etc
            for(int iValue = 0; iValue < nValues; iValue++) {
                log.trace("iValue: " + iValue);

                // Reset the template for this pass
                thisFilterValue = selectedTemplate;

                // Now apply the leaf values for the leaves that were present
                for (String lname : leafNames) {
                    if (lname.compareToIgnoreCase("field") == 0) {
                        value = getLeafValue(leafValues, lname, iField);
                    } else if (lname.compareToIgnoreCase("value") == 0) {
                        value = getLeafValue(leafValues, lname, iValue);
                    } else {
                        value = getLeafValue(leafValues, lname, 0);
                    }

                    // If the 'matchSubstring' modifier is true, then have to surround
                    // the 'value' item with '*'.
                    if (matchSubstring) {
                        if (lname.compareToIgnoreCase("value") == 0) {
                            value = "*" + value + "*";
                        }
                    }
                    // Apply the filter 'value' modifiers, if they are specified in the XML, to the
                    // <value> element.
                    if (lname.compareToIgnoreCase("value") == 0) {
                        if (escapeSpecialChars) value = escapeSpecialChars(value);
                        if (quoteMultipleWords) value = checkQuoting(value);
                    }
                    // Apply the current value to the filter
                    thisFilterValue = applyTemplate(lname, value, thisFilterValue);
                }

                log.trace("thisFilterValue: " + thisFilterValue);

                // Accumulate value terms
                if(iValue > 0) {
                    subFilterValue = subFilterValue + " " + operator + " " + thisFilterValue;
                } else {
                    subFilterValue = thisFilterValue;
                }

                // If this is the last pass, then surround this term by parens if needed.
                if ((iValue == nValues - 1) && nValues > 1) {
                    subFilterValue = "(" + subFilterValue + ")";
                }
                log.trace("subFilterValue: " + subFilterValue);
            }
            if (iField > 0) {
                completeFilterValue = completeFilterValue + " " + fieldsOperator + " " + subFilterValue;
            } else {
                completeFilterValue = subFilterValue;
            }
        }

        // If this subquery contains multiple terms, surround it with parenthesis
        if (nFields > 1 || excludeCondition) {
            completeFilterValue = "(" + completeFilterValue + ")";
        }

        // Apply the 'exclude' modifier, if it was present in the XML
        if (excludeCondition) {
            completeFilterValue = "-" + completeFilterValue;
        }

        completeFilterValue = completeFilterValue.trim();
        log.trace("    * * * * Final filter value: " + completeFilterValue);

        return completeFilterValue;
    }

    /**
     *
     * Templates are used to convert XML elements, attributes and values into the form defined by
     * the template.
     * <p>
     *     Several templates may be available for a filter to use. The 'best' template will be selected
     *     by determining which template can be completely filled in by the values that are provided
     *     in a given XML document to be processed.
     * </p>
     * <p>
     *     The 'leaf' values for a filter are compared to the tokens in the template to find a match
     * </p>
     * @param leafNames - XML 'leaf' elements available to the filter
     * @param allLeafNames - all possible leaf name values
     * @return the template string that the filter processor will use.
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
                log.trace("Selecting template: " + tval);
                break;
            }
        }

        if(! matchAll) {
            selectedTemplate = templateValues[0];
            log.trace("Can't find template match, using template: " + selectedTemplate);
        }

        return selectedTemplate;
    }

    /**
     * Fill in the template with the appropriate name and value
     * @param name the template name, i.e. 'name:value'
     * @param value the template value
     * @param filterValue the resulting filter value after the name and value have been substituted in
     * @return the filled in template value.
     */
    private String applyTemplate(String name, String value, String filterValue) {
        HashMap<String, String> thisMap;

        if(value == null || value.isEmpty()) return filterValue;
        filterValue = applyItem(name, value, filterValue);

        return filterValue;
    }

    /**
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

    /**
     * Add a leaf element value to the hash of leaf values.
     * <p>
     *     The leaf value may be compound, i.e. "keyword--text", so add an entry for each of these
     *     compound values.
     * </p>
     * @param leafName the name of the leaf, which is the hashmap key
     * @param leafValue a single value to add to the list of values for this entry
     * @return the hash containing all current leaf values
     */
    private HashMap<String, ArrayList<String>> addLeafValue(HashMap<String, ArrayList<String>> leafValues,
                                                                 String leafName, String leafValue, String delimeter) {

        ArrayList<String> currentValues = new ArrayList<String>();
        Boolean replace = false;

        if(leafValues.containsKey(leafName)) {
            replace = true;
            currentValues = leafValues.get(leafName);
        }

        // Now check if this is a 'compound' leaf value - the LeafElement
        // class will concatenate multiple values together if a LeafElement repeats,
        // for exmaple, <field>keywords</field><field>attribute</field> are returned by
        // LeafElement as 'keywords--attribute'.
        // If the leaf value is a compound value, split it and add each separate value
        if(delimeter != null && ! delimeter.isEmpty()) {
            String thisLeafValues[] = leafValue.split(delimeter);
            for(String val : thisLeafValues) {
                log.trace("Adding leaf name, value: " + leafName + ", " + val);
                currentValues.add(val);
            }
        } else {
            // No delimeter is defined, so we have no alternative but to add the entire value into
            // one entry
            currentValues.add(leafValue);
            log.trace("Adding leaf name,value: " + leafName + ", " + leafValue);
        }

        if(replace) {
            leafValues.remove(leafName);
            leafValues.put(leafName, currentValues);
        } else {
            leafValues.put(leafName, currentValues);
        }

        return leafValues;
    }

    /**
     * Get the 'leaf' values of a filter, which are all the 1st level child elements of the filter.
     * <p>
     *     Each leaf (indexed by name) may contain multiple values
     * </p>
     * @param leafValues the hash of leaf values
     * @param leafName the leaf to extract
     * @param index the index of the leaf values to extract
     * @return the single leaf value
     */
    private String getLeafValue(HashMap<String, ArrayList<String>>leafValues, String leafName, int index) {
        ArrayList<String> currentValues = leafValues.get(leafName);
        log.trace("Getting leaf name, value: " + leafName + ", " + currentValues.get(index));
        return currentValues.get(index);
    }

    /**
     * Apply escape characters in order to 'protect' a string
     * @param value The value to be escaped
     * @return the escaped value.
     */
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

    /**
     * Surround a value with quotes if it contains multiple tokens.
     * @param value the value to check and apply quotes to.
     * @return the quoted (or not quaoted) string
     */
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

    /**
     * Initialize an XPath expression used to find the nodes in a filter.
     */
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

    /**
     * Get the name of this filter
     * @return the name of this filter
     */
    public String getName() {
        return name;
    }

    /**
     * Set the name of this filter
     * @param name the name of this filter
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Get the XPath used by this filter
     * @return the XPath used by this filter
     */
    public String getxPath() {
        return xPath;
    }

    /**
     * Set the XPath used by this filter
     * @param xPath the XPath used by this filter
     */
    public void setxPath(String xPath) {
        this.xPath = xPath;
    }

    /**
     * Get default filter element values
     * @return the default values for this filter
     * @see "application-context-collections.xml"
     */
    public String getDefaults() {
        return defaults;
    }

    /**
     * Set default filter element values
     * @param defaults the filter element defaults
     */
    public void setDefaults(String defaults) {
        this.defaults = defaults;
    }

    /**
     * Get the setting that determines if special characters should be escaped in this filter value
     * @return the setting value
     * @see "application-context-collections.xml"
     */
    public Boolean getEscapeSpecialChars() {
        return escapeSpecialChars;
    }

    /**
     * Set the setting that determines if special characters should be escaped in this filter value
     * @param escapeSpecialChars
     */
    public void setEscapeSpecialChars(Boolean escapeSpecialChars) {
        this.escapeSpecialChars = escapeSpecialChars;
    }

    /**
     * Get the setting that determines if multiple word values are quoted
     * @return the setting for multiple words being quoted
     * @see "application-context-collections.xml"
     */
    public Boolean getQuoteMultipleWords() {
        return quoteMultipleWords;
    }

    /**
     * Set the setting that determines if multiple word values are quoted
     * @param quoteMultipleWords
     */
    public void setQuoteMultipleWords(Boolean quoteMultipleWords) {
        this.quoteMultipleWords = quoteMultipleWords;
    }

    /**
     * Get the XPath expression used to process this filter
     * @return the XPath expression used to process this filter
     */
    public XPathExpression getxPathExpression() {
        return xPathExpression;
    }

    /**
     * Set the XPath expression used to process this filter
     * @param xPathExpression
     */
    public void setxPathExpression(XPathExpression xPathExpression) {
        this.xPathExpression = xPathExpression;
    }

    /**
     * Get the token delimeter used to separate values
     * @return the token delimeter used to sepapate values
     */
    public String getDelimiter() {
        return delimiter;
    }

    /**
     * Set the token delimeter used to separate values
     * @param delimiter
     */
    public void setDelimiter(String delimiter) {
        this.delimiter = delimiter;
    }

    /**
     * Get the template used by this filter
     * @return the template used by this filter
     */
    public String getTemplate() {
        return template;
    }

    /**
     * Set the template used by this filter
     * @param template the template used by this filter
     */
    public void setTemplate(String template) {
        this.template = template;
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

    /**
     * Get the leaf (child element) of a filter
     * @return the leaf (child element) of a filter
     */
    public List<LeafElement> getLeafs() {
        return leafs;
    }

    /**
     * Set the leaf (child element) of a filter
     * @param leafs the leaf (child element) of a filter
     */
    public void setLeafs(List<LeafElement> leafs) {
        this.leafs = leafs;
    }
}
