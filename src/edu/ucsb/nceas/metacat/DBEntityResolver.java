package edu.ucsb.nceas.metacat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;

import edu.ucsb.nceas.metacat.accesscontrol.AccessControlList;
import edu.ucsb.nceas.metacat.database.DBConnection;
import edu.ucsb.nceas.metacat.database.DBConnectionPool;
import edu.ucsb.nceas.metacat.util.SystemUtil;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;

import java.sql.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.Reader;
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
  private DefaultHandler handler = null;
  private String docname = null;
  private String doctype = null;
  private String systemid = null;
  private Reader dtdtext = null;
  private static Log logMetacat = LogFactory.getLog(DBEntityResolver.class);
  
  /**
   * Construct an instance of the DBEntityResolver class
   *
   */
  public DBEntityResolver() {

  }
  /**
   * Construct an instance of the DBEntityResolver class
   * @param handler the SAX handler to determine parsing context
   * @param dtd Reader of new dtd to be uploaded on server's file system
   */
  public DBEntityResolver(DefaultHandler handler, Reader dtd)
  {
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
                     throws SAXException, IOException
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
          InputStream in = null;
          if (dbSystemID.startsWith("http://") || dbSystemID.startsWith("https://")) {
              in = checkURLConnection(dbSystemID);
          } else {
              in = new FileInputStream( new File(dbSystemID));
          }
          dtdSource = new InputSource(in);
      } catch (SAXException se) {
          logMetacat.error("DBEntityResolver.resolveEntity - can't get the dtd file since "
                                  + se.getMessage());
          throw se;
      } catch (IOException ioe) {
          logMetacat.error("DBEntityResolver.resolveEntity - can't get the dtd file since "
                                  + ioe.getMessage());
          throw ioe;
      }
    }
    return dtdSource;

  }

  /**
   * Look at db XML Catalog to get System ID (if any) for @doctype.
   * Return null if there are no System ID found for @doctype
   */
  private static String getDTDSystemID( String doctype )
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
            systemid = SystemUtil.getContextDir() + systemid;
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
