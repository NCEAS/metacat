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
import java.sql.ResultSet;
import java.util.Enumeration;
import java.util.Hashtable;

import edu.ucsb.nceas.MCTestCase;
import edu.ucsb.nceas.metacat.database.DBConnection;
import edu.ucsb.nceas.metacat.database.DBConnectionPool;
import edu.ucsb.nceas.metacat.properties.PropertyService;

import junit.framework.Test;
import junit.framework.TestSuite;


/**
 * @author jones
 *
 * Test class for getting idea how long it will take - to build 
 * records xml_queryresult table for a given return fields
 */
public class QueryResultBuilderTest extends MCTestCase
{
	/** DBConnection pool*/
	private DBConnectionPool connPool = null;
	/** Return field value in xml_returnfield table*/
	private String[] returnFieldString = {"dataset/title", "entityName", "individualName/surName", "keyword"};
	/** Return field id value for above return field string in xml_returnfield*/
	private int returnFieldId =11;
	
	/**
     * Create a suite of tests to be run together
     */
    public static Test suite()
    {
        TestSuite suite = new TestSuite();
        suite.addTest(new QueryResultBuilderTest("initialize"));
        //suite.addTest(new QueryResultBuilderTest("testBuildRecords"));
        return suite;
    }
   
    /**
     * Constructor to build the test
     * 
     * @param name the name of the test method
     */
    public QueryResultBuilderTest(String name)
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
        	connPool = DBConnectionPool.getInstance();      
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }
    
    /**
     * Run an initial test that always passes to check that the test
     * harness is working.
     */
    public void initialize()
    {
        assertTrue(1 == 1);
    }
    
    

    /**
     * Tests how long it will take to build records in xml_queryresult table
     */
    public void testBuildRecords() throws Exception
    {
        double start = System.currentTimeMillis()/1000;
        DBConnection dbconn = DBConnectionPool.getDBConnection("DBQuery.findDocuments");
        int serialNumber = dbconn.getCheckOutSerialNumber();
        String sql = "SELECT docid, nodedata, path FROM xml_path_index WHERE path IN (";
        PreparedStatement pstmt = null;
        ResultSet result = null;
        //Gets the document information such as doctype, docname and et al
        Hashtable docsInformation = getDocumentsInfo();
        //The key of the hash is docid, the element is the return field value (in xml_format)
        Hashtable table = new Hashtable();
        int size = returnFieldString.length;
        // Constructs the sql base on the return fields
        boolean first = true;
        for (int i=0; i< size; i++)
        {
        	if (!first)
        	{
        		sql=sql+",";
        	}
        	sql=sql+"'";
        	String path = returnFieldString[i];
        	 sql = sql+path;
        	 sql=sql+"'";
        	 first = false;
        }
        sql = sql+")";
        System.out.println("The final sql is "+sql);
    	pstmt = dbconn.prepareStatement(sql);
        pstmt.execute();
        result = pstmt.getResultSet();
        boolean hasNext = result.next();
        // Gets returning value for docids and puts them into hashtable
        while (hasNext)
        {
        	String docid = result.getString(1);
        	String value = result.getString(2);
        	String path  = result.getString(3);
        	// Gets document information from document information hash table.
        	String docInfo = (String)docsInformation.get(docid);
        	StringBuffer buffer  = new StringBuffer();
        	if (docInfo != null)
        	{
        		buffer.append(docInfo);
        	}
        	buffer.append("<param name=\"");
            buffer.append(path);
            buffer.append("\">");
            buffer.append(value);
            buffer.append("</param>");
            String xmlValue = buffer.toString();
            //If the hashtable already has key for this docid,
            //we should append obove value to the old value
        	if (table.containsKey(docid))
        	{
        		String oldValue = (String)table.get(docid);
        		String newValue = oldValue+xmlValue;
        		table.put(docid, newValue);
        	}
        	else
        	{
        		table.put(docid, xmlValue);
        	}
        	hasNext = result.next();
        }
        result.close();
        pstmt.close();
        // Insert the hashtable value into xml_queryresult table
        Enumeration docids = table.keys();
        while (docids.hasMoreElements())
        {
        	String docId = (String)docids.nextElement();
        	String returnString = (String)table.get(docId);
        	String query = "INSERT INTO xml_queryresult (returnfield_id, docid, "
                + "queryresult_string) VALUES (?, ?, ?)";
         
            pstmt = dbconn.prepareStatement(query);
            pstmt.setInt(1,returnFieldId );
            pstmt.setString(2,docId);
            pstmt.setString(3, returnString);
           	pstmt.execute();     
            pstmt.close();
           
        }
        
        double end = System.currentTimeMillis()/1000;
        System.out.println("The time to build xml_queryresult is "+(end-start));
        DBConnectionPool.returnDBConnection(dbconn, serialNumber);
        assertTrue(1 == 1);
    }
    
    /*
     * Gets a Hashtable which contains documents' information. 
     * The key of this Hashtable is docid. The element value is the information, like
     * docid, docname,doctype and et al. This method will be used in testBuildRecords 
     * method to combine a completed return information for query.
     */
    private Hashtable getDocumentsInfo() throws Exception
    {
    	DBConnection dbconn = DBConnectionPool.getDBConnection("DBQuery.findDocuments");
        int serialNumber = dbconn.getCheckOutSerialNumber();
        String sql = "SELECT docid, rev, docname, doctype, date_created,date_updated from xml_documents";
        PreparedStatement pstmt = null;
        ResultSet result = null;
        //The key of the hash is docid, the element is the document information (in xml_format)
        Hashtable table = new Hashtable();
        pstmt = dbconn.prepareStatement(sql);
        pstmt.execute();
        result = pstmt.getResultSet();
        boolean hasNext = result.next();
        // Get  values from the xml_document table
        while (hasNext)
        {
        	String             docid = result.getString(1);
        	int                      rev = result.getInt(2);
        	String        docname = result.getString(3);
        	String         doctype = result.getString(4);
        	String   createDate = result.getString(5);
        	String updateDate = result.getString(6);
        	String completeDocid = docid
                             + PropertyService.getProperty("document.accNumSeparator");
            completeDocid += rev;
            // Put everything into a string buffer
            StringBuffer document = new StringBuffer();
            document.append("<docid>").append(completeDocid).append("</docid>");
            if (docname != null)
            {
                document.append("<docname>" + docname + "</docname>");
            }
            if (doctype != null)
            {
               document.append("<doctype>" + doctype + "</doctype>");
            }
            if (createDate != null)
            {
                document.append("<createdate>" + createDate + "</createdate>");
            }
            if (updateDate != null)
            {
              document.append("<updatedate>" + updateDate + "</updatedate>");
            }
            String information = document.toString();
            // Put the docid and info into Hashtable
            table.put(docid, information);
            hasNext = result.next();
        }
        result.close();
        pstmt.close();
        return table;
    }

}
