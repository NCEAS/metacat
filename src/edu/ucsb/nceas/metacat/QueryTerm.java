/**
 *  '$RCSfile$'
 *    Purpose: A Class that represents a structured query, and can be
 *             constructed from an XML serialization conforming to
 *             pathquery.dtd. The printSQL() method can be used to print
 *             a SQL serialization of the query.
 *  Copyright: 2000 Regents of the University of California and the
 *             National Center for Ecological Analysis and Synthesis
 *    Authors: Matt Jones
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
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package edu.ucsb.nceas.metacat;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Vector;

import javax.xml.bind.DatatypeConverter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.ucsb.nceas.metacat.shared.MetacatUtilException;
import edu.ucsb.nceas.metacat.util.SystemUtil;

/** a utility class that represents a single term in a query */
public class QueryTerm
{
    private static Log log = LogFactory.getLog(QueryTerm.class);

    private boolean casesensitive = false;

    private String searchmode = null;

    private String value = null;

    private String pathexpr = null;

    private boolean percentageSymbol = false;

    private int countPercentageSearchItem = 0;
    
    private boolean inUnionGroup = false;
    
    public static final String CONTAINS = "contains";
    
    public static final String EQUALS = "equals";

    /**
     * Construct a new instance of a query term for a free text search (using
     * the value only)
     *
     * @param casesensitive
     *            flag indicating whether case is used to match
     * @param searchmode
     *            determines what kind of substring match is performed (one of
     *            starts-with|ends-with|contains|matches-exactly)
     * @param value
     *            the text value to match
     */
    public QueryTerm(boolean casesensitive, String searchmode, String value)
    {
        this.casesensitive = casesensitive;
        this.searchmode = searchmode;
        this.value = value;
    }

    /**
     * Construct a new instance of a query term for a structured search
     * (matching the value only for those nodes in the pathexpr)
     *
     * @param casesensitive
     *            flag indicating whether case is used to match
     * @param searchmode
     *            determines what kind of substring match is performed (one of
     *            starts-with|ends-with|contains|matches-exactly)
     * @param value
     *            the text value to match
     * @param pathexpr
     *            the hierarchical path to the nodes to be searched
     */
    public QueryTerm(boolean casesensitive, String searchmode, String value,
            String pathexpr)
    {
        this(casesensitive, searchmode, value);
        this.pathexpr = pathexpr;
    }

    /** determine if the QueryTerm is case sensitive */
    public boolean isCaseSensitive()
    {
        return casesensitive;
    }

    /** get the searchmode parameter */
    public String getSearchMode()
    {
        return searchmode;
    }

    /** get the Value parameter */
    public String getValue()
    {
        return value;
    }

    /** get the path expression parameter */
    public String getPathExpression()
    {
        return pathexpr;
    }

    /** get the percentage count for one query term */
    public int getPercentageSymbolCount()
    {
        return countPercentageSearchItem;
    }
    
    /**
     * Set the query term in a union group
     * @param inUnionGroup
     */
    public void setInUnionGroup (boolean inUnionGroup)
    {
    	this.inUnionGroup = inUnionGroup;
    }
    
    /**
     * If this query group in Union group
     * @return
     */
    public boolean isInUnionGroup()
    {
    	return this.inUnionGroup;
    }

    
    public String printSearchExprSQL(List<Object> parameterValues) {

        // Uppercase the search string if case match is not important
        String casevalue = null;
        String nodedataterm = null;
        boolean notEqual = false;
        if (casesensitive) {
            nodedataterm = "nodedata";
            casevalue = value;
        } else {
            nodedataterm = "UPPER(nodedata)";
            casevalue = value.toUpperCase();
        }

        // Add appropriate wildcards to search string
        String searchexpr = null;
        if (searchmode.equals("starts-with")) {
            searchexpr = nodedataterm + " LIKE ? ";
            parameterValues.add(casevalue + "%");
        } else if (searchmode.equals("ends-with")) {
            searchexpr = nodedataterm + " LIKE ? ";
            parameterValues.add("%" + casevalue + "%");
        } else if (searchmode.equals("contains")) {
            if (!casevalue.equals("%")) {
                searchexpr = nodedataterm + " LIKE ? ";
                parameterValues.add("%" + casevalue + "%");
            } else {
                searchexpr = nodedataterm + " LIKE ? ";
                parameterValues.add(casevalue);
                // find percentage symbol
                percentageSymbol = true;
            }
        } else if (searchmode.equals("not-contains")) {
        	notEqual = true;
            searchexpr = nodedataterm + " LIKE ? ";
            parameterValues.add("%" + casevalue + "%");
        } else if (searchmode.equals("equals")) {
            searchexpr = nodedataterm + " = ? ";
            parameterValues.add(casevalue);
        } else if (searchmode.equals("isnot-equal")) {
        	notEqual = true;
            searchexpr = nodedataterm + " = ? ";
            parameterValues.add(casevalue);
        } else {
            String oper = null;
            if (searchmode.equals("greater-than")) {
                oper = ">";
                nodedataterm = "nodedatanumerical";
            } else if (searchmode.equals("greater-than-equals")) {
                oper = ">=";
                nodedataterm = "nodedatanumerical";
            } else if (searchmode.equals("less-than")) {
                oper = "<";
                nodedataterm = "nodedatanumerical";
            } else if (searchmode.equals("less-than-equals")) {
                oper = "<=";
                nodedataterm = "nodedatanumerical";
            } else {
                System.out
                        .println("NOT expected case. NOT recognized operator: "
                                + searchmode);
                return null;
            }
            
            
            try {
                // it is number; numeric comparison
            	Double doubleVal = new Double(casevalue);
                searchexpr = nodedataterm + " " + oper + " ? ";
                parameterValues.add(doubleVal);
            } catch (NumberFormatException nfe) {
            	// is it a date?
            	try {
                	// try ISO 8601 formats
                	Calendar dataDateValue = DatatypeConverter.parseDateTime(casevalue);
                	//String lexicalString = DatatypeConverter.printDateTime(dataDateValue);
                    nodedataterm = "nodedatadate";
            		searchexpr = 
            			nodedataterm + " " + oper + " ? ";
            		parameterValues.add(dataDateValue.getTime());
            	} catch (Exception pe) {
            		// these are characters; character comparison
                    searchexpr = nodedataterm + " " + oper + " ? ";
                    parameterValues.add(casevalue);
            	}
            }
        }
        return searchexpr;
    }
    
    public boolean isNotEqualTerm() {

        boolean notEqual = false;

        if (searchmode.equals("not-contains")) {
        	notEqual = true;
        } else if (searchmode.equals("isnot-equal")) {
        	notEqual = true;
        }
        return notEqual;
    }
    
    /**
     * create a SQL serialization of the query that this instance represents
     */
    public String printSQL(boolean useXMLIndex, List<Object> parameterValues)
    {
    	
        StringBuffer self = new StringBuffer();
        
        // does it contain a not equals?
        boolean notEqual = isNotEqualTerm();

        // keep track of parameter values
        List<Object> searchValues = new ArrayList<Object>();
        
        // get the general search expression
        String searchexpr = printSearchExprSQL(searchValues);
        
        // add our parameter values
        parameterValues.addAll(searchValues);
       
        // to check xml_path_index can be used
        boolean usePathIndex = false;

        // if pathexpr has been specified in metacat.properties for indexing
        if(pathexpr != null){
        	try {
				if (SystemUtil.getPathsForIndexing().contains(pathexpr)) {
					usePathIndex = true;
				}
			} catch (MetacatUtilException ue) {
				log.warn("Could not get index paths: " + ue.getMessage());
			}
        }
        
        if(usePathIndex){
            // using xml_path_index table.....
        	if(notEqual == true ){
        		if (!inUnionGroup)
        		{
        			self.append("SELECT DISTINCT docid from xml_path_index WHERE");
        			self.append(" docid NOT IN (Select docid FROM xml_path_index WHERE ");
        			self.append(searchexpr);
        			self.append("AND path LIKE ? ) ");
        			parameterValues.add(pathexpr);
        		}
        		else
        		{
        			//if this is in union group we need to use "OR" to modify query
        			self.append("("+searchexpr);
        			self.append("AND path LIKE ?) ");
        			parameterValues.add(pathexpr);
        		}
        	} else {
        		if (!inUnionGroup)
        		{
        			self.append("SELECT DISTINCT docid FROM xml_path_index WHERE ");
        			self.append(searchexpr);
        			self.append("AND path LIKE ? ");
        			parameterValues.add(pathexpr);
        		}
        		else
        		{
        			//if this is in union group we need to use "OR" to modify query
        			self.append("("+searchexpr);
        			self.append("AND path LIKE ?) ");
        			parameterValues.add(pathexpr);
        		}
        	}

        } else {
            // using xml_nodes and xml_index tables

        	if(notEqual == true){
        		self.append("SELECT DISTINCT docid from xml_nodes WHERE");
        		self.append(" docid NOT IN (Select docid FROM xml_nodes WHERE ");
        	} else {
        		self.append("(SELECT DISTINCT docid FROM xml_nodes WHERE ");
        	}
        	self.append(searchexpr);
        	
            if (pathexpr != null) {
             String path = pathexpr;
                // use XML Index
                if (useXMLIndex) {
                    if (!hasAttributeInPath(pathexpr)) {
                        // without attributes in path
                        self.append("AND parentnodeid IN ");
                        self.append(
                            "(SELECT nodeid FROM xml_index WHERE path LIKE ?) ");
                        parameterValues.add(path);
                    } else {
                        // has a attribute in path
                        String attributeName = QuerySpecification
                            .getAttributeName(pathexpr);
                        self.append(
                            "AND nodetype LIKE 'ATTRIBUTE' AND nodename LIKE ? ");
                        parameterValues.add(attributeName);
                        // and the path expression includes element content other than
                        // just './' or '../'
                        if ( (!pathexpr.startsWith(QuerySpecification.
                            ATTRIBUTESYMBOL)) &&
                            (!pathexpr.startsWith("./" +
                                                  QuerySpecification.ATTRIBUTESYMBOL)) &&
                            (!pathexpr.startsWith("../" +
                                                  QuerySpecification.ATTRIBUTESYMBOL))) {

                            self.append("AND parentnodeid IN ");
                            path = QuerySpecification
                                .newPathExpressionWithOutAttribute(pathexpr);
                            self.append(
                                "(SELECT nodeid FROM xml_index WHERE path LIKE ?) ");
                            parameterValues.add(path);
                        }
                    }
                }
                else {
                    // without using XML Index; using nested statements instead
                	// keep track of the values we add as prepared statement question marks (?)
                	List<Object> nestedParameterValues = new ArrayList<Object>();
                    //self.append("AND parentnodeid IN ");
                	self.append("AND ");
                	String nested = useNestedStatements(pathexpr, nestedParameterValues);		
                    self.append(nested);
                	// include them
                    parameterValues.addAll(nestedParameterValues);
                }
            }
            else if ( (value.trim()).equals("%")) {
                //if pathexpr is null and search value is %, is a
                // percentageSearchItem
                // the count number will be increase one
                countPercentageSearchItem++;

            }
            self.append(") ");
        }

        return self.toString();
    }

    /** A method to judge if a path have attribute */
    private boolean hasAttributeInPath(String path)
    {
        if (path.indexOf(QuerySpecification.ATTRIBUTESYMBOL) != -1) {
            return true;
        } else {
            return false;
        }
    }

   
    public static String useNestedStatements(String pathexpr, List<Object> parameterValues)
    {
    	
        log.info("useNestedStatements()");
        log.info("pathexpr: " + pathexpr);
        String elementPrefix = " parentnodeid IN ";
        String attributePrefix  =  " nodeid IN ";
        boolean lastOneIsAttribute = false;
        StringBuffer nestedStmts = new StringBuffer();
        String path = pathexpr.trim();
        String sql = "";

        if (path.indexOf('/') == 0)
        {
            nestedStmts.append("AND parentnodeid = rootnodeid ");
            path = path.substring(1).trim();
        }

        do
        {
            int inx = path.indexOf('/');
            int predicateStart = -1;
            int predicateEnd;
            String node;
            Vector<String> predicates = new Vector<String>();

            // extract predicates
            predicateStart = path.indexOf(QuerySpecification.PREDICATE_START, predicateStart + 1);

            // any predicates in this node?
            if (inx != -1 && (predicateStart == -1 || predicateStart > inx))
            {
                // no
                node = path.substring(0, inx).trim();
                path = path.substring(inx + 1).trim();
            }
            else if (predicateStart == -1)
            {
                // no and it's the last node
                node = path;
                if (node != null && node.indexOf(QuerySpecification.ATTRIBUTESYMBOL) != -1)
                {
                	lastOneIsAttribute = true;
                	node = removeAttributeSymbol(node);
                }
                path = "";
            }
            else
            {
                // yes
                node = path.substring(0, predicateStart).trim();
                path = path.substring(predicateStart);
                predicateStart = 0;

                while (predicateStart == 0)
                {
                    predicateEnd = path.indexOf(QuerySpecification.PREDICATE_END,
                            predicateStart);

                    if (predicateEnd == -1)
                    {
                        log.warn("useNestedStatements(): ");
                        log.warn("    Invalid path: " + pathexpr);
                        return "";
                    }

                    predicates.add(path.substring(1, predicateEnd).trim());
                    path = path.substring(predicateEnd + 1).trim();
                    inx = path.indexOf('/');
                    predicateStart = path.indexOf(QuerySpecification.PREDICATE_START);
                }

                if (inx == 0)
                    path = path.substring(1).trim();
                else if (!path.equals(""))
                {
                    log.warn("useNestedStatements(): ");
                    log.warn("    Invalid path: " + pathexpr);
                    return "";
                }
            }

            nestedStmts.insert(0, "(SELECT nodeid FROM xml_nodes WHERE nodename LIKE ? ");
            parameterValues.add(0, node);
            
            // for the last statement: it is without " AND parentnodeid IN "
            if (!path.equals("")) {
                nestedStmts.insert(0, "AND parentnodeid IN ");
            }
            
            if (predicates.size() > 0)
            {
                for (int n = 0; n < predicates.size(); n++)
                {
                	// keep track of the values we add as prepared statement question marks (?)
                	List<Object> predicateParameterValues = new ArrayList<Object>();
                    String predSQL = predicate2SQL(predicates.get(n), predicateParameterValues);

                    if (predSQL.equals("")) {
                        return "";
                    }
                    
                    // include the values
                    parameterValues.addAll(predicateParameterValues);
                    // include the sql
                    nestedStmts.append(predSQL).append(' ');
                }
            }

            nestedStmts.append(") ");
        }
        while (!path.equals(""));
        if (lastOneIsAttribute)
        {
        	sql = attributePrefix+nestedStmts.toString();
        }
        else
        {
        	sql = elementPrefix+nestedStmts.toString();
        }
        return sql;
    }
    
    
    /*
     * Removes @ symbol from path. For example, if path is @entity, entity will be returned.
     * If path is entity, entity will be returned. 
     */
    private static String removeAttributeSymbol(String path)
    {
    	String newPath  ="";
    	log.debug("Original string before removing @ is " + path);
    	if (path != null)
    	{
    		
    		int attribute = path.indexOf(QuerySpecification.ATTRIBUTESYMBOL);
    		if (attribute != -1)
    		{
    			// has attribute symbol. Reomve it and return the remained part. 
    			try
    			{
    		         newPath = path.substring(attribute + 1).trim();
    			}
    			catch (Exception e)
    			{
    				newPath = path;
    			}
    		}
    		else
    		{
    			// doesn't have attribute symbol. Return original string
    			newPath = path;
    		}
    	}
    	else
    	{
    		// if is null, return null;
    		newPath = path;
    	}
    	log.debug("String after removing @ is " + newPath);
    	return newPath;
    	
    }

    /**
     * 
     */
    public static String predicate2SQL(String predicate, List<Object> predicateParameterValues)
    {
        String path = predicate.trim();
        int equals = path.indexOf('=');
        String literal = null;

        if (equals != -1)
        {
            literal = path.substring(equals + 1).trim();
            path = path.substring(0, equals).trim();
            int sQuote = literal.indexOf('\'');
            int dQuote = literal.indexOf('"');

            if (sQuote == -1 && dQuote == -1)
            {
                log.warn("predicate2SQL(): ");
                log.warn("  1  Invalid or unsupported predicate: " + predicate);
                return "";
            }

            if (sQuote == -1 &&
                (dQuote != 0 ||
                 literal.indexOf('"', dQuote + 1) != literal.length() - 1))
            {
                log.warn("predicate2SQL(): ");
                log.warn("  2  Invalid or unsupported predicate: " + predicate);
                return "";
            }

            if (sQuote != 0 ||
                literal.indexOf('\'', sQuote + 1) != literal.length() - 1)
            {
                log.warn("predicate2SQL(): ");
                log.warn("  3  Invalid or unsupported predicate: " + predicate);
                return "";
            }
        }

        StringBuffer sql = new StringBuffer();
        int attribute = path.indexOf('@');

        if (attribute == -1)
        {
            if (literal != null)
            {
                sql.append("AND nodeid IN (SELECT parentnodeid FROM xml_nodes WHERE nodetype = 'TEXT' AND nodedata LIKE ")
                    .append("?").append(")");
                // remove single or double quotes
                literal = literal.substring(1, literal.length() - 1);
                // add to vlaues list
                predicateParameterValues.add(literal);
            }
        }
        else
        {
            sql.append(
                    "AND nodeid IN (SELECT parentnodeid FROM xml_nodes WHERE nodetype = 'ATTRIBUTE' AND nodename LIKE ? ");
            // add to values
            predicateParameterValues.add(path.substring(attribute + 1).trim());

            if (literal != null)
            {
                sql.append("AND nodedata LIKE ? ");
                // remove single or double quotes
                literal = literal.substring(1, literal.length() - 1);
                // add to values list
                predicateParameterValues.add(literal);
            }

            sql.append(")");
            path = path.substring(0, attribute).trim();

            if (path.endsWith("/"))
                path = path.substring(0, path.length() - 1).trim();
            else
            {
                if (!path.equals(""))
                {
                    log.warn("predicate2SQL(): ");
                    log.warn("    Invalid or unsupported predicate: " + predicate);
                    return "";
                }
            }
        }

        while (!path.equals(""))
        {
            int ndx = path.lastIndexOf('/');
            int predicateEnd = -1;
            int predicateStart;
            String node;

            if (ndx != -1)
            {
                node = path.substring(ndx + 1).trim();
                path = path.substring(0, ndx).trim();
            }
            else
            {
                node = path;
                path = "";
            }

            if (!node.equals("")) {
                sql.insert(0, "(SELECT parentnodeid FROM xml_nodes WHERE nodename LIKE ? ) ");
                predicateParameterValues.add(0, node);
            }
            else if (!path.equals(""))
            {
                log.warn("predicate2SQL(): ");
                log.warn("    Invalid or unsupported predicate: " + predicate);
                return "";
            }

            if (path.equals(""))
            {
                sql.insert(0,
                        node.equals("") ? "AND rootnodeid IN " : "AND nodeid IN ");
            }
            else
            {
                sql.append("AND nodeid IN ");
            }
        }

        return sql.toString();
    }

    /**
     * Show a string representation of the query
     * @deprecated This should not be used for performing the query
     * because the parameter values are not bound in the raw SQL
     */
    public String toString()
    {

    	// keep track of the values we add as prepared statement question marks (?)
    	List<Object> parameterValues = new ArrayList<Object>();
        return this.printSQL(true, parameterValues);
    }
    
    /**
     * Compare two query terms to see if they have same search value.
     * @param term
     * @return
     */
    public boolean hasSameSearchValue(QueryTerm term)
    {
    	boolean same = false;
    	if (term != null)
    	{
    		String searchValue = term.getValue();
    		if (searchValue != null && this.value != null)
    		{
    			if (searchValue.equalsIgnoreCase(this.value))
    			{
    				if (this.getSearchMode().equals(term.getSearchMode())) {
        				same = true;
    				}
    			}
    		}
    	}
    	return same;
    }
}
