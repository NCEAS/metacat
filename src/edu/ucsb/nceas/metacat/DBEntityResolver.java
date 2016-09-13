/**
 *  '$RCSfile$'
 *    Purpose: A Class that implements org.xml.sax.EntityResolver interface
 *             for resolving external entities
 *  Copyright: 2000 Regents of the University of California and the
 *             National Center for Ecological Analysis and Synthesis
 *    Authors: Jivka Bojilova, Matt Jones
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

import org.apache.log4j.Logger;
import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;

import edu.ucsb.nceas.metacat.accesscontrol.AccessControlList;
import edu.ucsb.nceas.metacat.database.DBConnection;
import edu.ucsb.nceas.metacat.database.DBConnectionPool;
import edu.ucsb.nceas.metacat.util.SystemUtil;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;

import java.sql.*;
import java.io.File;
import java.io.Reader;
import java.io.BufferedReader;
import java.io.BufferedInputStream;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.IOException;
import java.net.URL;
import java.net.MalformedURLException;

/**
 * A database aware Class implementing EntityResolver interface for the SAX
 * parser to call when processing the XML stream and intercepting any
 * external entities (including the external DTD subset and external
 * parameter entities, if any) before including them.
 */
public class DBEntityResolver implements EntityResolver
{
  private DBConnection connection = null;
  private DefaultHandler handler = null;
  private String docname = null;
  private String doctype = null;
  private String systemid = null;
  private Reader dtdtext = null;
  private static Logger logMetacat = Logger.getLogger(DBEntityResolver.class);
  
  /**
   * Construct an instance of the DBEntityResolver class
   *
   * @param conn the JDBC connection to which information is written
   */
  public DBEntityResolver(DBConnection conn)
  {
    this.connection= conn;
  }
  /**
   * Construct an instance of the DBEntityResolver class
   *
   * @param conn the JDBC connection to which information is written
   * @param handler the SAX handler to determine parsing context
   * @param dtd Reader of new dtd to be uploaded on server's file system
   */
  public DBEntityResolver(DBConnection conn, DefaultHandler handler, Reader dtd)
  {
    this.connection = conn;
    this.handler = handler;
    this.dtdtext = dtd;
  }

  /**
   * The Parser call this method before opening any external entity
   * except the top-level document entity (including the external DTD subset,
   * external entities referenced within the DTD, and external entities
   * referenced within the document element)
   */
  public InputSource resolveEntity (String publicId, String systemId)
                     throws SAXException
  {
    InputSource dtdSource= null;
    logMetacat.debug("DBEntityResolver.resolveEntity - in resolveEntity "+" the public id is "+publicId+" and systemId is "+systemId);
    String dbSystemID;
    String doctype = null;

    // Won't have a handler under all cases
    if ( handler != null ) {
        String message = "Metacat can't determine the public id or the name of the root element of the document, so the validation can't be applied and the document is rejected";
        logMetacat.debug("DBEntityResolver.resolveEntity - the handler class is "+handler.getClass().getCanonicalName());
      if ( handler instanceof DBSAXHandler ) {
        DBSAXHandler dhandler = null;
        dhandler = (DBSAXHandler)handler;
        if ( dhandler.processingDTD() ) {
            logMetacat.debug("DBEntityResolver.resolveEntity - in the branch of the handler class is  DBSAXHandler");
          // public ID is doctype
          if (publicId != null) {
            doctype = publicId;
            logMetacat.debug("DBEntityResolver.resolveEntity - the publicId is not null, so the publicId is the doctype. The doctype is: "
                                     + doctype);
          // assume public ID (doctype) is docname
          } else {
            doctype = dhandler.getDocname();
            logMetacat.debug("DBEntityResolver.resolveEntity - the publicId is null and we treat the doc name(the root element name) as the doc type. The doctype is: "
                    + doctype);
          }
          
          if(doctype == null || doctype.trim().equals("")) {
              //we can't determine the public id or the name of the root element in for this dtd defined xml document
              logMetacat.error("DBEntityResolver.resolveEntity - "+message);
              throw new SAXException(message);
          } else {
              logMetacat.debug("DBEntityResolver.resolveEntity - the final doctype for DBSAXHandler "+doctype);
          }
        }
      } else if ( handler instanceof AccessControlList ) {
          logMetacat.debug("DBEntityResolver.resolveEntity - in the branch of the handler class is AccessControlList");
        AccessControlList ahandler = null;
        ahandler = (AccessControlList)handler;
        if ( ahandler.processingDTD() ) {
          // public ID is doctype
          if (publicId != null) {
            doctype = publicId;
            logMetacat.debug("DBEntityResolver.resolveEntity - the publicId is not null, so the publicId is the doctype. The doctype in AccessControlList is: "
                    + doctype);
          // assume public ID (doctype) is docname
          } else {
            doctype = ahandler.getDocname();
            logMetacat.debug("DBEntityResolver.resolveEntity - the publicId is null and we treat the doc name(the root element name) as the doc type. The doctype in AccessControlList is: "
                    + doctype);
          }
          if(doctype == null || doctype.trim().equals("")) {
              //we can't determine the public id or the name of the root element in for this dtd defined xml document
              logMetacat.error("DBEntityResolver.resolveEntity - "+message);
              throw new SAXException(message);
          } else {
              logMetacat.debug("DBEntityResolver.resolveEntity - the final doctype for AccessControList "+doctype);
          }
        } else {
            logMetacat.debug("DBEntityResolver.resolveEntity - the method resolverEntity for the AccessControList class is not processing a dtd");
        }
      } else {
          logMetacat.debug("DBEntityResolver.resolveEntity - in the branch of the other handler class");
      }
    } else {
        logMetacat.debug("DBEntityResolver.resolveEntity - the xml handler is null. So we can't find the doctype.");
    }

    // get System ID for doctype
    if (doctype != null) {
      // look at db XML Catalog for System ID
      logMetacat.info("DBEntityResolver.resolveEntity - get systemId from doctype: " + doctype);
      dbSystemID = getDTDSystemID(doctype);
      logMetacat.info("DBEntityResolver.resolveEntity - The Systemid from xml_catalog table is: " + dbSystemID);
      if(dbSystemID == null) {
          logMetacat.error("DBEntityResolver.resolveEntity - "+"The doctype: "+doctype+" , which was defined by a DTD document, isn't registered in Metacat. Please contact the operator of the Metacat");
          throw new SAXException("The doctype: "+doctype+" , which was defined by a DTD document, isn't registered in Metacat. Please contact the operator of the Metacat");
      }
      // check that it is accessible on our system before getting too far
      try {
    	  InputStream in = checkURLConnection(dbSystemID);
    	  dtdSource = new InputSource(in);
	  } catch (SAXException se) {
	      se.printStackTrace();
	      throw se;
		  // after an upgrade, the dtd will not exist on disk, but it is in xml catalog.  The db system id may be pointing 
		  // back at this system  Try and download it from the original system id and see if we still have a problem
		  // checking the URL connection.
		  /*logMetacat.warn("DBEntityResolver.resolveEntity - Problem when checking URL Connection: " + se.getMessage());
		  logMetacat.warn("DBEntityResolver.resolveEntity - Probably, dtd for doc type " + doctype + " existed in xml catalog, but not on disk.  Uploading from: " + systemId);
		  InputStream istream = checkURLConnection(systemId);
		  uploadDTDFromURL(istream, systemId);
		  try {
			  Thread.currentThread().sleep(6000);
			  checkURLConnection(dbSystemID);
		  } catch (Exception e2) {
			  logMetacat.error("DBEntityResolver.resolveEntity - still could not find dtd for doc type " + doctype + " at " 
					  + dbSystemID + " : " + e2.getMessage());
			  dbSystemID = null;
		  }*/
	  } 
      /*boolean doctypeIsInDB = true;
      // no System ID found in db XML Catalog
      if (dbSystemID == null) {
        doctypeIsInDB = false;
        // use the provided System ID
        if (systemId != null) {
          dbSystemID = systemId;
        }
        logMetacat.info("DBEntityResolver.resolveEntity - If above Systemid is null, then get "
                                 + "system id from file: " + dbSystemID);
      }
      // there are dtd text provided; try to upload on Metacat
      if ( dtdtext != null ) {
        dbSystemID = uploadDTD(dbSystemID);
      }

      // open URLConnection to check first
      InputStream istream = checkURLConnection(dbSystemID);

      // need to register System ID in db XML Catalog if not yet
      if ( !doctypeIsInDB ) {
        // new DTD from outside URL location; try to upload on Metacat
        if ( dtdtext == null ) {
          dbSystemID = uploadDTDFromURL(istream, dbSystemID);
        }
        registerDTD(doctype, dbSystemID);
      }
      // return a byte-input stream for use
      InputSource is = new InputSource(dbSystemID);

      // close and open URLConnection again
      try {
        istream.close();
      } catch (IOException e) {
        throw new SAXException
        ("DBEntityResolver.resolveEntity - I/O issue when resolving entity: " + e.getMessage());
      }
      istream = checkURLConnection(dbSystemID);
      is.setByteStream(istream);
      return is;*/
    } else {
    
      //InputStream istream = checkURLConnection(systemId);
      //return null;
    }
    return dtdSource;

  }

  /**
   * Look at db XML Catalog to get System ID (if any) for @doctype.
   * Return null if there are no System ID found for @doctype
   */
  public static String getDTDSystemID( String doctype )
                 throws SAXException
  {
    String systemid = null;
    PreparedStatement pstmt = null;
    DBConnection conn = null;
    int serialNumber = -1;
    ResultSet rs = null;
    try {
      //check out DBConnection
      conn=DBConnectionPool.getDBConnection("DBEntityResolver.getDTDSystemID");
      serialNumber=conn.getCheckOutSerialNumber();

      String sql = "SELECT system_id FROM xml_catalog " +
      "WHERE entry_type = 'DTD' AND public_id = ?";
      
      pstmt = conn.prepareStatement(sql);
      pstmt.setString(1, doctype);
      
      pstmt.execute();
      rs = pstmt.getResultSet();
      boolean tableHasRows = rs.next();
      if (tableHasRows) {
        systemid = rs.getString(1);
        // system id may not have server url on front.  Add it if not.
        if (!systemid.startsWith("http://")) {
        	systemid = SystemUtil.getContextURL() + systemid;
        }
      }
      //pstmt.close();
    } catch (SQLException e) {
      throw new SAXException
      ("DBEntityResolver.getDTDSystemID - SQL error when getting DTD system ID: " + e.getMessage());
    } catch (PropertyNotFoundException pnfe) {
        throw new SAXException
        ("DBEntityResolver.getDTDSystemID - Property error when getting DTD system ID:  " + pnfe.getMessage());
      }
    finally
    {
      try
      {
          if(rs != null) {
              rs.close();
          }
          if(pstmt != null) {
              pstmt.close();
          }
        
      }//try
      catch (SQLException sqlE)
      {
        logMetacat.error("DBEntityResolver.getDTDSystemId - SQL error: " + sqlE.getMessage());
      }//catch
      finally
      {
        DBConnectionPool.returnDBConnection(conn, serialNumber);
      }//finally
    }//finally

    // return the selected System ID
    return systemid;
  }

  /**
   * Register new DTD identified by @systemId in Metacat XML Catalog
   * . make a reference with @systemId for @doctype in Metacat DB
   */
  private void registerDTD ( String doctype, String systemId )
                 throws SAXException
  {
	  String existingSystemId = getDTDSystemID(doctype);
	  if (existingSystemId != null && existingSystemId.equals(systemId)) {
		  logMetacat.warn("DBEntityResolver.registerDTD - doctype/systemId already registered in DB: " + doctype);
		  return;
	  }
    //DBConnection conn = null;
    //int serialNumber = -1;
    PreparedStatement pstmt = null;
    // make a reference in db catalog table with @systemId for @doctype
    try {
      //check out DBConnection
      //conn=DBConnectionPool.getDBConnection("DBEntityResolver.registerDTD");
      //serialNumber=conn.getCheckOutSerialNumber();


      pstmt = connection.prepareStatement(
             "INSERT INTO xml_catalog " +
             "(entry_type, public_id, system_id) " +
             "VALUES ('DTD', ?, ?)");
      // Increase usage count
      connection.increaseUsageCount(1);
      // Bind the values to the query
      pstmt.setString(1, doctype);
      pstmt.setString(2, systemId);
      // Do the insertion
      pstmt.execute();
      int updateCnt = pstmt.getUpdateCount();
      logMetacat.debug("DBEntityReolver.registerDTD - DTDs registered: " + updateCnt);
      pstmt.close();
    } catch (SQLException e) {
      throw new SAXException
      ("DBEntityResolver.registerDTD - SQL issue when registering DTD: " + e.getMessage());
    }
    finally
    {
      try
      {
        pstmt.close();
      }//try
      catch (SQLException sqlE)
      {
        logMetacat.error("DBEntityResolver.registerDTD - SQL error: " + sqlE.getMessage());
      }//catch
      //DBConnectionPool.returnDBConnection(conn, serialNumber);
    }//finally

  }

  /**
	 * Upload new DTD text identified by
	 * 
	 * @systemId to Metacat file system
	 */
	private String uploadDTD(String systemId) throws SAXException {
		String dtdPath = null;
		String dtdURL = null;
		try {
			dtdPath = SystemUtil.getContextDir() + "/dtd/";
			dtdURL = SystemUtil.getContextURL() + "/dtd/";
		} catch (PropertyNotFoundException pnfe) {
			throw new SAXException("DBEntityResolver.uploadDTD: " + pnfe.getMessage());
		}

		// get filename from systemId
		String filename = systemId;
		int slash = Math.max(filename.lastIndexOf('/'), filename.lastIndexOf('\\'));
		if (slash > -1) {
			filename = filename.substring(slash + 1);
		}

		// writing dtd text on Metacat file system as filename
		try {
			// create a buffering character-input stream
			// that uses a default-sized input buffer
			BufferedReader in = new BufferedReader(dtdtext);

			// open file writer to write the input into it
			// String dtdPath = "/opt/tomcat/webapps/bojilova/dtd/";
			File f = new File(dtdPath, filename);
			synchronized (f) {
				try {
					if (f.exists()) {
						throw new IOException("File already exist: "
								+ f.getCanonicalFile());
						// if ( f.exists() && !f.canWrite() ) {
						// throw new IOException("Not writable: " +
						// f.getCanonicalFile());
					}
				} catch (SecurityException se) {
					// if a security manager exists,
					// its checkRead method is called for f.exist()
					// or checkWrite method is called for f.canWrite()
					throw se;
				}
				// create a buffered character-output stream
				// that uses a default-sized output buffer
				FileWriter fw = new FileWriter(f);
				BufferedWriter out = new BufferedWriter(fw);

				// read the input and write into the file writer
				String inputLine;
				while ((inputLine = in.readLine()) != null) {
					out.write(inputLine, 0, inputLine.length());
					out.newLine(); // instead of out.write('\r\n');
				}

				// the input and the output streams must be closed
				in.close();
				out.flush();
				out.close();
				fw.close();
			} // end of synchronized
		} catch (MalformedURLException e) {
			throw new SAXException("DBEntityResolver.uploadDTD() - Malformed URL when uploading DTD: " + e.getMessage());
		} catch (IOException e) {
			throw new SAXException("DBEntityResolver.uploadDTD - I/O issue when uploading DTD: " + e.getMessage());
		} catch (SecurityException e) {
			throw new SAXException("DBEntityResolver.uploadDTD() - Security issue when uploading DTD: " + e.getMessage());
		}

		// String dtdURL = "http://dev.nceas.ucsb.edu/bojilova/dtd/";
		return dtdURL + filename;
	}


  /**
	 * Upload new DTD located at outside URL to Metacat file system
	 */
	private String uploadDTDFromURL(InputStream istream, String systemId)
			throws SAXException {
		String dtdPath = null;
		String dtdURL = null;
		try {
			dtdPath = SystemUtil.getContextDir() + "/dtd/";
			dtdURL = SystemUtil.getContextURL() + "/dtd/";
		} catch (PropertyNotFoundException pnfe) {
			throw new SAXException("DBEntityResolver.uploadDTDFromURL - Property issue when uploading DTD from URL: "
					+ pnfe.getMessage());
		}

		// get filename from systemId
		String filename = systemId;
		int slash = Math.max(filename.lastIndexOf('/'), filename.lastIndexOf('\\'));
		if (slash > -1) {
			filename = filename.substring(slash + 1);
		}

		// writing dtd text on Metacat file system as filename
		try {
			// create a buffering character-input stream
			// that uses a default-sized input buffer
			BufferedInputStream in = new BufferedInputStream(istream);

			// open file writer to write the input into it
			//String dtdPath = "/opt/tomcat/webapps/bojilova/dtd/";
			File f = new File(dtdPath, filename);
			synchronized (f) {
				try {
					if (f.exists()) {
						logMetacat.warn("DBEntityResolver.uploadDTDFromURL - File already exists: " + f.getCanonicalFile());
						//return dtdURL + filename;
						//throw new IOException("File already exist: "
						//		+ f.getCanonicalFile());
						//if ( f.exists() && !f.canWrite() ) {
						//  throw new IOException("Not writable: " + f.getCanonicalFile());
					}
				} catch (SecurityException se) {
					// if a security manager exists,
					// its checkRead method is called for f.exist()
					// or checkWrite method is called for f.canWrite()
					throw se;
				}
				// create a buffered character-output stream
				// that uses a default-sized output buffer
				FileWriter fw = new FileWriter(f);
				BufferedWriter out = new BufferedWriter(fw);

				// read the input and write into the file writer
				int inputByte;
				while ((inputByte = in.read()) != -1) {
					out.write(inputByte);
					//out.newLine(); //instead of out.write('\r\n');
				}

				// the input and the output streams must be closed
				in.close();
				out.flush();
				out.close();
				fw.close();
			} // end of synchronized
		} catch (MalformedURLException e) {
			throw new SAXException("DBEntityResolver.uploadDTDFromURL - Malformed URL when uploading DTD from URL: "
					+ e.getMessage());
		} catch (IOException e) {
			throw new SAXException("DBEntityResolver.uploadDTDFromURL - I/O issue when uploading DTD from URL:  "
					+ e.getMessage());
		} catch (SecurityException e) {
			throw new SAXException("DBEntityResolver.uploadDTDFromURL - Security issue when uploading DTD from URL:  "
					+ e.getMessage());
		}

		//String dtdURL = "http://dev.nceas.ucsb.edu/bojilova/dtd/";
		return dtdURL + filename;
	}

	/**
	 * Check URL Connection for @systemId, and return an InputStream
	 * that can be used to read from the systemId URL.  The parser ends
	 * up using this via the InputSource to read the DTD.
	 *
	 * @param systemId a URI (in practice URL) to be checked and opened
	 */
	public static InputStream checkURLConnection(String systemId) throws SAXException {
		try {
			return (new URL(systemId).openStream());

		} catch (MalformedURLException e) {
			throw new SAXException("DBEntityResolver.checkURLConnection - Malformed URL when checking URL Connection: "
					+ e.getMessage());
		} catch (IOException e) {
			throw new SAXException("DBEntityResolver.checkURLConnection - I/O issue when checking URL Connection: "
					+ e.getMessage());
		}
	}
}
