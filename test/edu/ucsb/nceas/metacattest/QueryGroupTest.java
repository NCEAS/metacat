/**
 *  '$RCSfile$'
 *  Copyright: 2004 Regents of the University of California and the
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
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package edu.ucsb.nceas.metacattest;

import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;

import junit.framework.Test;
import junit.framework.TestSuite;

import edu.ucsb.nceas.MCTestCase;
import edu.ucsb.nceas.metacat.DBQuery;
import edu.ucsb.nceas.metacat.QueryGroup;
import edu.ucsb.nceas.metacat.QueryTerm;
import edu.ucsb.nceas.metacat.database.DBConnectionPool;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.metacat.util.MetacatUtil;

/**
 * @author jones
 * 
 * Test the output of the QuerySpecification class
 */
public class QueryGroupTest extends MCTestCase
{
	private QueryGroup group = null;
	 /* Initialize properties*/
    static
    {
  	  try
  	  {
			PropertyService.getInstance();
 	  }
  	  catch(Exception e)
  	  {
  		  System.err.println("Exception in initialize option in MetacatServletNetTest "+e.getMessage());
  	  }
    }
    /**
     * NOTE: there are no quotes around the values because we are comparing it to a PreparedStatement.toString()
     * String after binding the parameter values. Please trust that the PreparedStatement is correct.
     */
    private String query = 
    	"(SELECT DISTINCT docid FROM xml_path_index WHERE  (UPPER(nodedata) "+
        "LIKE %LAND% AND path IN ( dataset/title, geographicCoverage/boundingCoordinates/southBoundingCoordinate )) " +
        "OR ((UPPER(nodedata) LIKE %JONES% AND path LIKE organizationName) ) OR ((UPPER(nodedata) LIKE %LAND % AND path LIKE keyword) ) " +
        "OR ((UPPER(nodedata) LIKE %DATOS% AND path LIKE entityName) ) UNION ((SELECT DISTINCT docid FROM xml_nodes WHERE UPPER(nodedata) " +
        "LIKE %VALUE1% AND parentnodeid IN (SELECT nodeid FROM xml_index WHERE path LIKE path1) )  UNION " +
        "(SELECT DISTINCT docid FROM xml_nodes WHERE UPPER(nodedata) LIKE %VALUE2% AND parentnodeid IN " +
        "(SELECT nodeid FROM xml_index WHERE path LIKE path2) ) ))";
    
    /**
     * Constructor to build the test
     * 
     * @param name the name of the test method
     */
    public QueryGroupTest(String name)
    {
        super(name);
    }
    /**
     * Establishes a testing framework by initializing appropriate objects.
     */
    protected void setUp() throws Exception
    {
      super.setUp();
   
    }

    
    /**
     * Releases any objects after tests are complete.
     */
    protected void tearDown() throws Exception
    {
 
      super.tearDown();
    }

  

    /**
     * Tests initial
     */
    public void initial()
    {
    	assertTrue(1 == 1);   
    }
    
    /**
     * Tests print out sql command of QueryGroup with UNION 
     */
    public void printUnion()
    {
    	group = new QueryGroup("UNION");
    	QueryTerm term1 = new QueryTerm ( false, "contains", "land", "dataset/title");
    	QueryTerm term2 = new QueryTerm ( false, "contains", "jones", "organizationName");
    	QueryTerm term3 = new QueryTerm ( false, "contains", "land ", "keyword");
    	QueryTerm term4 = new QueryTerm ( false, "contains", "land", "geographicCoverage/boundingCoordinates/southBoundingCoordinate");
    	QueryTerm term5 = new QueryTerm ( false, "contains", "datos",  "entityName");
    	QueryGroup child = new QueryGroup("UNION");
    	QueryTerm term6 = new QueryTerm ( false, "contains", "value1", "path1");
    	QueryTerm term7 = new QueryTerm ( false, "contains", "value2", "path2");
    	child.addChild(term6);
    	child.addChild(term7);
    	group.addChild(term1);
    	group.addChild(term2);
    	group.addChild(term3);
    	group.addChild(term4);
    	group.addChild(term5);
    	group.addChild(child);
    	// keep track of parameter values
        List<Object> parameterValues = new ArrayList<Object>();
    	String queryString = group.printSQL(true, parameterValues);
    	try {
    		// fill in the values to really check the query string matches original/expected
    		PreparedStatement pstmt = DBConnectionPool.getDBConnection("queryGroupTest").prepareStatement(queryString);
    		pstmt = DBQuery.setPreparedStatementValues(parameterValues, pstmt);
    		String preparedQueryString = pstmt.toString();
    		System.out.println("Prepared query: " + preparedQueryString);
    		System.out.println("Original query: " + query);

    		assertEquals(query, preparedQueryString);
    	} catch (Exception e) {
    		e.printStackTrace();
			fail(e.getMessage());
		}
    }
    
    
    /**
     * Create a suite of tests to be run together
     */
    public static Test suite()
    {
        TestSuite suite = new TestSuite();
        suite.addTest(new QueryGroupTest("initial"));
        suite.addTest(new QueryGroupTest("printUnion"));
        return suite;
    }

   
}