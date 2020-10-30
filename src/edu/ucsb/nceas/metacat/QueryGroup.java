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
import java.util.Enumeration;
import java.util.List;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.ucsb.nceas.metacat.shared.MetacatUtilException;
import edu.ucsb.nceas.metacat.util.SystemUtil;

 /** a utility class that represents a group of terms in a query */
public  class QueryGroup {
    private String operator = null;  // indicates how query terms are combined
    private Vector children = null;  // the list of query terms and groups
    private int countPercentageSearchItem = 0;
    private Vector queryTermsWithSameValue = null;//this two dimension vectors.
                                                  //will hold query terms which has same search value.
    private Vector queryTermsInPathIndex = null; //this vector holds query terms without same value
                                                                                 // and search path is in path index.
    private Vector<QueryTerm> queryTerms = null;//this vector only holds query terms without same search value.
                                                             // and search path is NOT in path index.
    private Vector queryGroupsChildren = null;
    private static Log logMetacat = LogFactory.getLog(QueryGroup.class);
    public static String UNION = "UNION";
    public static String INTERSECT = "INTERSECT";


    /**
     * construct a new QueryGroup
     *
     * @param operator the boolean conector used to connect query terms
     *                    in this query group
     */
    public QueryGroup(String operator) {
      this.operator = operator;
      children = new Vector();
      queryTermsWithSameValue = new Vector();
      queryTermsInPathIndex = new Vector(); 
      queryTerms = new Vector<QueryTerm>();
      queryGroupsChildren = new Vector();
    }

    /**
     * Add a child QueryGroup to this QueryGroup
     *
     * @param qgroup the query group to be added to the list of terms
     */
    public void addChild(QueryGroup qgroup) {
      children.add((Object)qgroup);
      queryGroupsChildren.add(qgroup);
    }

    /**
     * Add a child QueryTerm to this QueryGroup
     *
     * @param qterm the query term to be added to the list of terms
     */
    public void addChild(QueryTerm qterm) {
      children.add((Object)qterm);
      handleNewQueryTerms(qterm);
      
    }

    /*
     * Retrieve an Enumeration of query terms for this QueryGroup
     */
    private Enumeration getChildren() {
      return children.elements();
    }

    public int getPercentageSymbolCount()
    {
      return countPercentageSearchItem;
    }

    /**
     * create a SQL serialization of the query that this instance represents
     */
    public String printSQL(boolean useXMLIndex, List<Object> parameterValues) {
    	
      StringBuffer self = new StringBuffer();
      StringBuffer queryString = new StringBuffer();

      boolean first = true;
      
      if (!queryTermsWithSameValue.isEmpty() || !queryTermsInPathIndex.isEmpty())
      {
    	  // keep track of the values we add as prepared statement question marks (?)
    	  List<Object> groupValues = new ArrayList<Object>();
    	  String pathIndexQueryString = printSQLStringInPathIndex(groupValues);
    	  parameterValues.addAll(groupValues);
    	  queryString.append(pathIndexQueryString);
    	  if (queryString != null)
    	  {
    		  first = false;
    	  }
      }
      
      for (int i=0; i<queryGroupsChildren.size(); i++)
      {
      
    	  // keep track of the values we add as prepared statement question marks (?)
    	  List<Object> childrenValues = new ArrayList<Object>();
    	  // get the group
    	  QueryGroup qg = (QueryGroup) queryGroupsChildren.elementAt(i);
    	  String queryGroupSQL = qg.printSQL(useXMLIndex, childrenValues);
    	  logMetacat.info("In QueryGroup.printSQL.. found a QueryGroup: " + queryGroupSQL);       	
        	if (first) {
        		first = false;
        	} else {
        		if(!queryString.toString().equals("") && queryGroupSQL != null &&!queryGroupSQL.equals("")){
                    queryString.append(" " + operator + " ");
        		}
        	}
        	// add the sql
   		  	queryString.append(queryGroupSQL);
   		  	// add the parameter values
   		  	parameterValues.addAll(childrenValues);
   		  	
   		  	// count percentage number
   		  	int count = qg.getPercentageSymbolCount();
   		  	countPercentageSearchItem = countPercentageSearchItem + count;
      }
      
      for (int i=0; i<queryTerms.size(); i++)
      {
    	  // keep track of the values we add as prepared statement question marks (?)
    	  List<Object> termValues = new ArrayList<Object>();
    	  // get the term
    	  QueryTerm qt = (QueryTerm)queryTerms.elementAt(i);
    	  String termQueryString = qt.printSQL(useXMLIndex, termValues);
    	  logMetacat.info("In QueryGroup.printSQL.. found a QueryGroup: " + termQueryString);
           if (!(qt.getSearchMode().equals("contains") && qt.getValue().equals("%"))){
        	   if (first) {
                   first = false;
               } else {
                   if(!queryString.toString().equals("")){
                       queryString.append(" " + operator + " ");
                   }
               }
        	   // include the sql
               queryString.append(termQueryString);
               // include the parameter values
               parameterValues.addAll(termValues);
               
           // count percerntage number
           int count = qt.getPercentageSymbolCount();
           countPercentageSearchItem = countPercentageSearchItem + count;
        } 
      }

      if(!queryString.toString().equals("")){
          self.append("(");
          self.append(queryString.toString());
          self.append(")");
      }
      
      logMetacat.info("In QueryGroup.printSQL.. final query returned is: " 
			+ self.toString());
      return self.toString();
    }
    
    
    
    
    /*
     * If every query term in a queryGroup share a search value and search path
     * is in xml_path_index, we should use a new query to replace the original query term query in order to
     * improve performance. Also if even the term doesn't share any value with other term
     * we still use "OR" to replace UNION action (we only handle union operator in the query group).
     * 
     */
    private String printSQLStringInPathIndex(List<Object> parameterValues)
    {
    	String sql ="";
    	String value ="";
    	StringBuffer sqlBuff = new StringBuffer();
    	int index =0;
    	if (queryTermsWithSameValue != null && queryTermsInPathIndex != null)
    	{
    		
    		sqlBuff.append("SELECT DISTINCT docid FROM xml_path_index WHERE ");
    		if (!queryTermsWithSameValue.isEmpty())
    		{
    			boolean firstVector = true;
	    		for (int j=0; j<queryTermsWithSameValue.size(); j++)
	    		{
	    	   		Vector queryTermVector = (Vector)queryTermsWithSameValue.elementAt(j);
		    		QueryTerm term1 = (QueryTerm)queryTermVector.elementAt(0);
		        	value = term1.getValue();
		        	boolean first = true;
		        	if (firstVector)
		        	{
					  firstVector = false;
		        	}
		        	else
		        	{
		        		sqlBuff.append(" "+"OR"+" ");
		        	}
		        	
					sqlBuff.append(" (");
		        	
					// keep track of parameter values
			        List<Object> searchValues = new ArrayList<Object>();
			        
		        	// get the general search criteria (no path info)
		        	String searchTermSQL = term1.printSearchExprSQL(searchValues);
		        	
		        	// add the SQL
					sqlBuff.append(searchTermSQL);
					
					// add parameter values
					parameterValues.addAll(searchValues);
					
					sqlBuff.append("AND path IN ( ");

		    		//gets every path in query term object
		    		for (int i=0; i<queryTermVector.size(); i++)
		    		{
		    			QueryTerm term = (QueryTerm)queryTermVector.elementAt(i);
		    			value = term.getValue();
		    			String path = term.getPathExpression();
		    			if (path != null && !path.equals(""))
		    			{
		    				if (first)
		    				{
		    					first = false;
		    					sqlBuff.append("?");
		    					parameterValues.add(path);
		    				}
		    				else
		    				{
		    					sqlBuff.append(", ?");
		    					parameterValues.add(path);
		    				}
		    				index++;
		     				if (value != null && (value.equals("%") || value.equals("%%%")))
		                    {
		    				  countPercentageSearchItem++;
		                    }
	    			     }
	    		    }
	    		    sqlBuff.append(" ))");
	    	
	    	    }
	    	}
    		if (!queryTermsInPathIndex.isEmpty())
    		{
    			for (int j=0; j<queryTermsInPathIndex.size(); j++)
    			{
    				QueryTerm term = (QueryTerm)queryTermsInPathIndex.elementAt(j);
    				if (term != null)
    				{
	    				term.setInUnionGroup(true);
		    			 if (index > 0)
		    			 {
		    				 sqlBuff.append(" "+"OR"+" ");
		    			 }
		    			 sqlBuff.append("(");
		    			 // keep track of the parameter values for this sql
		    			 List<Object> termParameterValues = new ArrayList<Object>();
		    			 String termSQL = term.printSQL(true, termParameterValues);
	    				 sqlBuff.append(termSQL);
	    				 sqlBuff.append(")");
	    				 // add the param values
	    				 parameterValues.addAll(termParameterValues);
	    				 index++;
	    			}
    			}
    		}
    	}
    	if (index >0)
    	{
    		sql = sqlBuff.toString();
    	}
    	return sql;
    }

    /**
     * create a String description of the query that this instance represents.
     * This should become a way to get the XML serialization of the query.
     */
    public String toString() {
      StringBuffer self = new StringBuffer();

      self.append("  (Query group operator=" + operator + "\n");
      Enumeration en= getChildren();
      while (en.hasMoreElements()) {
        Object qobject = en.nextElement();
        self.append(qobject);
      }
      self.append("  )\n");
      return self.toString();
    }
    
    /*
     * When a new QueryTerm come, first we need to compare it to
     * the queryTerm vector, which contains queryTerm that doesn't
     * have same search value to any other queryTerm. Here is algorithm.
     * 1) If new QueryTerm find a QueryTerm in queryTerms which has same search value,
     *    them create a new vector which contain both QueryTerms and add the new vector
     *    to two-dimention vector queryTermsWithSameValue, and remove the QueryTerm which
     *    was in queryTerm.
     * 2) If new QueryTerm couldn't find a QueryTerm in queryTerms which has same search value,
     *    then search queryTermsWithSameValue, to see if this vector already has the search value.
     *    2.1) if has the search value, add the new QueryTerm to the queryTermsWithSameValue.
     *    2.2) if hasn't, add the new QueryTerm to queryTerms vector.
     */
    private void handleNewQueryTerms(QueryTerm newTerm)
    {
    	// currently we only handle UNION group
    	if (newTerm != null )
    	{
    		//System.out.println("new term is not null branch in handle new query term");
    		//we only handle union operator now.
    		try {
    			if (operator != null
						&& operator.equalsIgnoreCase(UNION)
						&& SystemUtil.getPathsForIndexing().contains(
								newTerm.getPathExpression())) {
					// System.out.println("in only union branch in handle new
					// query term");
					for (int i = 0; i < queryTermsInPathIndex.size(); i++) {
						QueryTerm term = (QueryTerm) queryTermsInPathIndex.elementAt(i);
						if (term != null && term.hasSameSearchValue(newTerm)) {
							// System.out.println("1Move a query term and add a
							// new query term into search value in handle new
							// query term");
							// find a target which has same search value
							Vector<QueryTerm> newSameValueVector = new Vector<QueryTerm>();
							newSameValueVector.add(term);
							newSameValueVector.addElement(newTerm);
							queryTermsWithSameValue.add(newSameValueVector);
							queryTermsInPathIndex.remove(i);
							return;
						}
					}
					// no same search value was found in queryTerms.
					// then we need search queryTermsWithSameValue
					for (int i = 0; i < queryTermsWithSameValue.size(); i++) {
						Vector sameValueVec = (Vector) queryTermsWithSameValue.elementAt(i);
						// we only compare the first query term
						QueryTerm term = (QueryTerm) sameValueVec.elementAt(0);
						if (term != null && term.hasSameSearchValue(newTerm)) {
							// System.out.println("2add a new query term into
							// search value in handle new query term");
							sameValueVec.add(newTerm);
							return;
						}
					}
					// nothing found, but the search path is still in
					// xml_path_index,
					// save it into queryTermsInPathIndex vector
					queryTermsInPathIndex.add(newTerm);
					return;
				}    		
    		} catch (MetacatUtilException ue) {
				logMetacat.warn("Could not get index paths: " + ue.getMessage());
			}
    		
    		// add this newTerm to queryTerms since we couldn't find it in xml_path_index
    		queryTerms.add(newTerm);
    	}
    	
    }
  }
