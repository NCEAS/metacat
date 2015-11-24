/**
 *  '$RCSfile$'
 *    Purpose: A Class that implements org.xml.sax.DTDHandler interface
 *             for resolving external entities
 *  Copyright: 2000 Regents of the University of California and the
 *             National Center for Ecological Analysis and Synthesis
 *    Authors: Jivka Bojilova
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

import org.xml.sax.*;

import edu.ucsb.nceas.metacat.database.DBConnection;

import java.sql.*;
import java.net.URL;
import java.net.MalformedURLException;
import java.util.Stack;
import java.util.EmptyStackException;

/** 
 * A database aware Class implementing DTDHandler interface for the SAX 
 * parser to call when processing the XML stream and intercepting notations 
 * and unparsed entities
 */
public class DBDTDHandler implements DTDHandler
{
   private DBConnection	connection = null;

   /** Construct an instance of the DBDTDHandler clas
    *
    * @param conn the JDBC connection to which information is written
    */
   public DBDTDHandler(DBConnection conn)
   {
      this.connection = conn;
   }
   
   /** Notation declarations are not signaled */
   public void notationDecl(String name, String publicId, String systemId)
            throws SAXException
   {
    System.out.println("from DBDTDHandler.notationDecl");
    System.out.print(name);
    System.out.print(publicId);
    System.out.print(systemId);
    return;
   }
   
   /** All are reported after startDocument and before first 
    * startElement event
    */
   public void unparsedEntityDecl(String name, String publicId, 
                                  String systemId, String notationName)
            throws SAXException
   {
    System.out.println("from DBDTDHandler.unparsedEntityDecl");
    System.out.print(name);
    System.out.print(publicId);
    System.out.print(systemId);
    System.out.println(notationName);
    //String doctype = DBEntityResolver.doctype;
    //if ( getEntitySystemID(conn, doctype, publicId) == "" ) 
    //    registerEntityPublicID(conn, doctype, publicId, systemId);
    return;
   }

   /** 
    * Look at db XML Catalog to get System ID (if any) for that Public ID 
    * and doctype.
    * Return empty string if there are not 
    */
/*
 * If this ever gets uncommented, you will need to verify the 
 * server url on the front of the system ID.
 * 
   private String getEntitySystemID (Connection conn, String doctype, 
                                     String publicId)
   {
        String system_id = "";
        Statement stmt;
        try {
          stmt = conn.createStatement();
          stmt.execute("SELECT system_id FROM xml_catalog " + 
                       "WHERE entry_type = 'ENTITY' AND source_doctype = '" + 
                        doctype + "' AND public_id = '" + publicId + "'");
          try {
            ResultSet rs = stmt.getResultSet();
            try {
              boolean tableHasRows = rs.next();
              if (tableHasRows) {
                try {
                  system_id = rs.getString(1);
                } catch (SQLException e) {
                  System.out.println("DBDTDHandler.getEntitySystemID() " +
                         "- Error with getString: " + e.getMessage());
                }
              }
            } catch (SQLException e) {
              System.out.println("DBDTDHandler.getEntitySystemID() " +
                         "- Error with next: " + e.getMessage());
            }
          } catch (SQLException e) {
            System.out.println("DBDTDHandler.getEntitySystemID() " +
                         "- Error with getrset: " + e.getMessage());
          }
          stmt.close();
        } catch (SQLException e) {
          System.out.println("DBDTDHandler.getEntitySystemID() " +
                         "- Error getting id: " + e.getMessage());
        }

        // return the selected System ID
        return system_id;
   }
*/
   /** 
    * Register Public ID in db XML Catalog 
    */
/*
   private void registerEntityPublicID (Connection conn, String doctype, 
                                        String publicId, String systemId)
   {
        try {
          PreparedStatement pstmt;
          pstmt = conn.prepareStatement(
                "INSERT INTO xml_catalog (entity_id, entity_name, " +
                "entry_type, source_doctype, public_id, system_id) " +
                "VALUES (null, null, 'ENTITY', ?, ?, ?)");
          // Bind the values to the query
          pstmt.setString(1, doctype);
          pstmt.setString(2, publicId);
          pstmt.setString(3, systemId);
          // Do the insertion
          pstmt.execute();
          pstmt.close();
        } catch (SQLException e) {
          System.out.println(e.getMessage());
        }
   }
*/

}
