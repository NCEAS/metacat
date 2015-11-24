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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;

import junit.framework.Test;
import junit.framework.TestSuite;

import edu.ucsb.nceas.MCTestCase;
import edu.ucsb.nceas.metacat.DBQuery;
import edu.ucsb.nceas.metacat.QuerySpecification;
import edu.ucsb.nceas.metacat.database.DBConnectionPool;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.metacat.util.MetacatUtil;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;

/**
 * @author jones
 * 
 * Test the output of the QuerySpecification class
 */
public class QuerySpecificationTest extends MCTestCase
{
    /** A test query document in xml format */
    private FileReader xml;
    
    /** The utilities object for accessing property values */
    
    private static String selectionQuery = 
    	"SELECT xml_documents.docid, identifier.guid, docname, doctype, date_created, date_updated, xml_documents.rev " +
    	"FROM xml_documents, identifier " +
    	"WHERE xml_documents.docid = identifier.docid AND xml_documents.rev = identifier.rev " +
    	"AND xml_documents.docid IN (((((SELECT DISTINCT docid " +
    	"FROM xml_nodes " +
    	"WHERE UPPER(nodedata) LIKE %JONES% ) )))) ";
    /*private static String extendedQuery = "select xml_nodes.docid, 'dataset/title' as path, xml_nodes.nodedata, xml_nodes.parentnodeid from xml_nodes, xml_documents where parentnodeid IN (SELECT nodeid FROM xml_nodes WHERE nodename LIKE 'title' AND parentnodeid IN (SELECT nodeid FROM xml_nodes WHERE nodename LIKE 'dataset' ) )  AND xml_nodes.docid in " +
    		"('obfs.45337', 'obfs.45338', 'obfs.45346') AND xml_nodes.nodetype = 'TEXT' AND " +
    		"xml_nodes.rootnodeid = xml_documents.rootnodeid UNION select xml_nodes.docid, 'originator/individualName/surName' as " +
    		"path, xml_nodes.nodedata, xml_nodes.parentnodeid from xml_nodes, xml_documents where parentnodeid IN " +
    		"(SELECT nodeid FROM xml_nodes WHERE nodename LIKE 'surName' AND parentnodeid IN " +
    		"(SELECT nodeid FROM xml_nodes WHERE nodename LIKE 'individualName' AND parentnodeid IN " +
    		"(SELECT nodeid FROM xml_nodes WHERE nodename LIKE 'originator' ) ) )  AND xml_nodes.docid in " +
    		"('obfs.45337', 'obfs.45338', 'obfs.45346') AND xml_nodes.nodetype = 'TEXT' AND xml_nodes.rootnodeid = " +
    		"xml_documents.rootnodeid UNION select xml_nodes.docid, 'keyword' as path, xml_nodes.nodedata, " +
    		"xml_nodes.parentnodeid from xml_nodes, xml_documents where parentnodeid IN (SELECT nodeid FROM " +
    		"xml_nodes WHERE nodename LIKE 'keyword' )  AND xml_nodes.docid in " +
    		"('obfs.45337', 'obfs.45338', 'obfs.45346') AND xml_nodes.nodetype = 'TEXT' AND " +
    		"xml_nodes.rootnodeid = xml_documents.rootnodeid UNION select xml_nodes.docid, '/eml/@packageId' as " +
    		"path, xml_nodes.nodedata, xml_nodes.parentnodeid from xml_nodes, xml_documents where parentnodeid IN " +
    		"(SELECT nodeid FROM xml_nodes WHERE nodename LIKE '@packageId' AND parentnodeid IN " +
    		"(SELECT nodeid FROM xml_nodes WHERE nodename LIKE 'eml' AND parentnodeid = rootnodeid ) )  " +
    		"AND xml_nodes.docid in ('obfs.45337', 'obfs.45338', 'obfs.45346') AND xml_nodes.nodetype = 'TEXT' AND " +
    		"xml_nodes.rootnodeid = xml_documents.rootnodeid UNION select xml_nodes.docid, " +
    		"'/eml/dataset/access/@authSystem' as path, xml_nodes.nodedata, xml_nodes.parentnodeid from " +
    		"xml_nodes, xml_documents where parentnodeid IN (SELECT nodeid FROM xml_nodes WHERE nodename " +
    		"LIKE '@authSystem' AND parentnodeid IN (SELECT nodeid FROM xml_nodes WHERE nodename LIKE 'access' " +
    		"AND parentnodeid IN (SELECT nodeid FROM xml_nodes WHERE nodename LIKE 'dataset' AND " +
    		"parentnodeid IN (SELECT nodeid FROM xml_nodes WHERE nodename LIKE 'eml' AND " +
    		"parentnodeid = rootnodeid ) ) ) )  AND xml_nodes.docid in ('obfs.45337', 'obfs.45338', 'obfs.45346') AND " +
    		"xml_nodes.nodetype = 'TEXT' AND xml_nodes.rootnodeid = xml_documents.rootnodeid UNION " +
    		"select xml_nodes.docid, '/eml/dataset/access/@order' as path, xml_nodes.nodedata, xml_nodes.parentnodeid " +
    		"from xml_nodes, xml_documents where parentnodeid IN (SELECT nodeid FROM xml_nodes WHERE nodename " +
    		"LIKE '@order' AND parentnodeid IN (SELECT nodeid FROM xml_nodes WHERE nodename LIKE 'access' AND " +
    		"parentnodeid IN (SELECT nodeid FROM xml_nodes WHERE nodename LIKE 'dataset' AND parentnodeid " +
    		"IN (SELECT nodeid FROM xml_nodes WHERE nodename LIKE 'eml' AND parentnodeid = rootnodeid ) ) ) )  " +
    		"AND xml_nodes.docid in ('obfs.45337', 'obfs.45338', 'obfs.45346') AND xml_nodes.nodetype = 'TEXT' " +
    		"AND xml_nodes.rootnodeid = xml_documents.rootnodeid";*/
    private static String extendedQuery =
    		"select xml_nodes.docid, xml_index.path, xml_nodes.nodedata,  xml_nodes.parentnodeid, xml_nodes.nodetype " +
    		"FROM xml_index, xml_nodes " +
    		"WHERE ( (xml_index.nodeid=xml_nodes.parentnodeid AND xml_index.path IN ( dataset/title, originator/individualName/surName , keyword ) " +
    		"AND xml_nodes.nodetype = 'TEXT') "+
            "OR  (xml_index.nodeid=xml_nodes.nodeid AND ( xml_index.path IN ( /eml/@packageId, /eml/dataset/access/@authSystem , /eml/dataset/access/@order ) " +
            "AND xml_nodes.nodetype = 'ATTRIBUTE'))) "+
            "AND xml_nodes.docid in (obfs.45337, obfs.45338, obfs.45346)";
    
    /* Initialize properties*/
    static
    {
  	  try
  	  {
		  PropertyService.getInstance();
 	  }
  	  catch(Exception e)
  	  {
  		  System.err.println("Exception in initialize option in MetacatServletNetTest " + e.getMessage());
  	  }
    }
    
    /**
     * Constructor to build the test
     * 
     * @param name the name of the test method
     */
    public QuerySpecificationTest(String name)
    {
        super(name);
    }

    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception
    {
        super.setUp();
        try {
//  		  	PropertyService.getInstance("build/tests");
  		  	PropertyService.getInstance();
            String xmlfile = "./test/query.xml";
            xml = new FileReader(new File(xmlfile));
        } catch (FileNotFoundException e) {
            fail(e.getMessage());
        } 
    }

    /**
     * Create a suite of tests to be run together
     */
    public static Test suite()
    {
        TestSuite suite = new TestSuite();
        suite.addTest(new QuerySpecificationTest("testPrintSQL"));
        suite.addTest(new QuerySpecificationTest("testPrintExtendedSQL"));
        return suite;
    }

    /**
     * Print the sql generated from this specification
     */
    public void testPrintSQL()
    {
        try {
            System.out.println("---- SQL ------------------");
            QuerySpecification qspec = new QuerySpecification(xml, 
                    PropertyService.getProperty("xml.saxparser"), 
                    PropertyService.getProperty("document.accNumSeparator"));
            // keep track of parameter values
            List<Object> parameterValues = new ArrayList<Object>();
            String query = qspec.printSQL(false, parameterValues);
    		// fill in the values to really check the query string matches original/expected
    		PreparedStatement pstmt = DBConnectionPool.getDBConnection("queryGroupTest").prepareStatement(query);
    		pstmt = DBQuery.setPreparedStatementValues(parameterValues, pstmt);
    		String preparedQueryString = pstmt.toString();

    		System.out.println("Prepared query: " + preparedQueryString);
            System.out.println("Original query: " + selectionQuery);
            
            assertEquals(selectionQuery, preparedQueryString);
        	 
        } catch (Exception e) {
    		e.printStackTrace();
			fail(e.getMessage());
		}
    }

    /**
     * Print the extended SQL for a result set.
     */
    public void testPrintExtendedSQL()
    {
        try {
            System.out.println("---- orginal EXT SQL  ------------------");
            System.out.println(extendedQuery);
            QuerySpecification qspec = new QuerySpecification(xml, 
                    PropertyService.getProperty("xml.saxparser"), 
                    PropertyService.getProperty("document.accNumSeparator"));
            // keep track of parameter values
            List<Object> parameterValues = new ArrayList<Object>();
            List<Object> docListValues = new ArrayList<Object>();
            docListValues.add("obfs.45337");
            docListValues.add("obfs.45338");
            docListValues.add("obfs.45346");

            String query = 
                    qspec.printExtendedSQL(
                            "?, ?, ?", true, parameterValues, docListValues);
            
            System.out.println("---- built EXT SQL ------------------");
            System.out.println(query);
            
            // fill in the values to really check the query string matches original/expected
    		PreparedStatement pstmt = DBConnectionPool.getDBConnection("queryGroupTest").prepareStatement(query);
    		pstmt = DBQuery.setPreparedStatementValues(parameterValues, pstmt);
    		String preparedQueryString = pstmt.toString();

    		System.out.println("Prepared query: " + preparedQueryString);
            System.out.println("Original query: " + extendedQuery);
            
            assertEquals(extendedQuery, preparedQueryString);
            
        } catch (Exception e) {
        	e.printStackTrace();
            fail(e.getMessage());
        }
    }

 
}
