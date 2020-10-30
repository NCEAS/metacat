/**
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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.ucsb.nceas.metacat.database.DBConnection;
import edu.ucsb.nceas.metacat.database.DBConnectionPool;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.metacat.util.DocumentUtil;
import edu.ucsb.nceas.metacat.util.MetacatUtil;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;
import edu.ucsb.nceas.utilities.triple.Triple;
import edu.ucsb.nceas.utilities.triple.TripleCollection;


public class RelationHandler //implements Runnable
{
  private DBConnection connection = null;
  private String docid = null;
  private String docType = null;
  private static Log logMetacat = LogFactory.getLog(RelationHandler.class);

  TripleCollection tripleForPackage = null;
   
  /** 
   * Constructor for this class.  finds all of the relations to a single xml
   * document and writes them to the database.  This includes transitive
   * relations.
   * @param docid the ID of the XML document to index.
   * @param doctype the doctype of this document
   * @param conn the db connection
   * @param list the triple list
   */
  public RelationHandler(String docid, String doctype, 
                        DBConnection conn, TripleCollection list)
              throws McdbException, SQLException, AccessionNumberException
  {
    this.connection = conn;
    this.docid = docid;
    this.docType = doctype;
    tripleForPackage = list;
    createRelations();
  }
  
   /**
   * insert the relations specified in the triples into xml_relation table
   */ 
  private void createRelations() 
              throws McdbException, SQLException, AccessionNumberException
  {
    String packagetype = docType;
    String subject = null;
    String subjectParentId = null;
    String subDoctype = null;
    String relationship = null;
    String relationshipParentId = null;
    String object = null;
    String objDoctype = null;
    PreparedStatement tstmt = null; // to insert each relation into xml_relation
   
    logMetacat.info("Running relation handler!");
   
    // first delete the relations for this package document if any
    deleteRelations(docid);
 
    //get the vetor of triples 
    Vector tripleList= new Vector();
    //get vector form tripleCollection
    if (tripleForPackage != null)
    {
      tripleList =tripleForPackage.getCollection();
    }
    
    if (tripleList != null && tripleList.size()>0) 
    {
      
       tstmt = connection.prepareStatement("INSERT INTO xml_relation (" +
                                    "docid,packagetype,subject,subdoctype," +
                                    "relationship, object, objdoctype) " + 
                                    "VALUES (?, ?, ?, ?, ?, ?, ?)");
      
      // go through tripe list 
      for (int i= 0; i<tripleList.size(); i++)
      {
        //increase usage count
        connection.increaseUsageCount(1);
        // Get the triple
        Triple triple = (Triple)tripleList.elementAt(i);
        logMetacat.info("Info from triple: ");
        logMetacat.info("subject from triple:"+triple.getSubject());
        logMetacat.info("relationship from triple:"+triple.getRelationship());
        logMetacat.info("object from triple: "+triple.getObject());
        //subject = (new DocumentIdentifier(triple.getSubject())).getIdentifier();
        subject = DocumentUtil.getDocIdFromString(triple.getSubject());
        relationship = triple.getRelationship();
        //object = (new DocumentIdentifier(triple.getObject())).getIdentifier();
        object = DocumentUtil.getDocIdFromString(triple.getObject());
        
        if (subject != null && relationship != null && object != null)
        {
        
          //put the new relation into xml_relation
          logMetacat.info("Insert into xml_relation table");
          tstmt.setString(1, docid);
          logMetacat.info("Insert docid into xml_relation table" + 
                                docid);
          tstmt.setString(2, packagetype);
          tstmt.setString(3, subject);
          logMetacat.info("Insert subject into xml_relation table" + 
                               subject);
          tstmt.setString(4, subDoctype);
          tstmt.setString(5, relationship);
          logMetacat.info("Insert relationship into xml_relation table" + 
                                relationship);
          tstmt.setString(6, object);
          logMetacat.info("Insert object into xml_relation table" + 
                                object);
          tstmt.setString(7, objDoctype);
          tstmt.execute();  
        }//if
     
      }//for
    }//if
    
    if ( tstmt != null ) 
    {
      tstmt.close();
    }
  
   
  }
 
  /**
   * Deletes all of the relations with a docid of 'docid'.
   * @param docid the docid of the package which relations to delete.
   */
  public void deleteRelations(String docid) throws SQLException
  {
    try {
      PreparedStatement pstmt = connection.prepareStatement(
                                "DELETE FROM xml_relation " +
                                "WHERE docid = ?");
      pstmt.setString(1, docid);
      //increase usage count
      connection.increaseUsageCount(1);
      pstmt.execute();
      pstmt.close();
    } catch(SQLException e) {
      logMetacat.error("error in RelationHandler.deleteRelations(): " + 
                          e.getMessage());
      
      throw e;
    }
  }

  /**
	 * Get the access file id for a package
	 * @param docid the document identifier of the package
	 * @return the document identifier of the access file for that package
	 */
	public static String getAccessFileIDWithRevision(String docid) throws SQLException {
		String aclid = null;
		int rev;
		PreparedStatement pstmt = null;
		DBConnection dbConn = null;
		int serialNumber = -1;

		StringBuffer sql = new StringBuffer();
		sql
				.append("SELECT docid, rev FROM xml_documents WHERE docid in (SELECT subject ");
		sql.append("FROM xml_relation WHERE docid = ? ");
		sql.append(" AND (");
		Vector accessdoctypes;
		try {
			accessdoctypes = MetacatUtil.getOptionList(PropertyService
				.getProperty("xml.accessdoctype"));
		} catch (PropertyNotFoundException pnfe) {
			throw new SQLException("Could not find access doctype: " + pnfe.getMessage());
		}
		for (int i = 0; i < accessdoctypes.size(); i++) {
			String atype = (String) accessdoctypes.elementAt(i);
			sql.append("doctype='").append(atype).append("'");
			if (i < accessdoctypes.size() - 1) {
				sql.append(" OR ");
			}
		}
		sql.append("))");
		//System.out.println("new sql script: " + sql.toString());

		try {
			dbConn = DBConnectionPool.getDBConnection("RelationHandler.getAccessFileID");
			serialNumber = dbConn.getCheckOutSerialNumber();
			pstmt = dbConn.prepareStatement(sql.toString());
			pstmt.setString(1, docid);
			pstmt.execute();
			ResultSet rs = pstmt.getResultSet();
			boolean hasRow = rs.next();
			if (hasRow) {
				aclid = rs.getString(1);
				rev = rs.getInt(2);
				String sep = ".";
				try {
					sep = PropertyService.getProperty("document.accNumSeparator");
				} catch (PropertyNotFoundException pnfe) {
					logMetacat.error("Could not find account separator.  Setting to '.': " + pnfe.getMessage());
				}
				aclid = sep + rev;
			}
			pstmt.close();
		}//try
		finally {
			try {
				pstmt.close();
			}//try
			finally {
				DBConnectionPool.returnDBConnection(dbConn, serialNumber);
			}//finally
		}//finally

		logMetacat.info("The access docid get from xml_relation is: " + aclid);
		return aclid;
	}

}
