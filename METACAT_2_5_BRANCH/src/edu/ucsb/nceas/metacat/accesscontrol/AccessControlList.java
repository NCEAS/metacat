/**
 *  '$RCSfile$'
 *    Purpose: A Class that loads eml-access.xml file containing ACL 
 *             for a metadata document into relational DB
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

package edu.ucsb.nceas.metacat.accesscontrol;

import java.io.IOException;
import java.io.StringReader;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Stack;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

import edu.ucsb.nceas.metacat.BasicNode;
import edu.ucsb.nceas.metacat.DBEntityResolver;
import edu.ucsb.nceas.metacat.DocumentImpl;
import edu.ucsb.nceas.metacat.McdbException;
import edu.ucsb.nceas.metacat.database.DBConnection;
import edu.ucsb.nceas.metacat.database.DBConnectionPool;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.metacat.util.SystemUtil;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;
import edu.ucsb.nceas.utilities.access.AccessControlInterface;

/** 
 * A Class that loads eml-access.xml file containing ACL for a metadata
 * document into relational DB. It extends DefaultHandler class to handle
 * SAX parsing events when processing the XML stream.
 */
public class AccessControlList extends DefaultHandler 
                               implements AccessControlInterface 
{

 
//  private static String sysdate = DatabaseService.getDBAdapter().getDateTimeFunction();
//  private static String isnull = DatabaseService.getDBAdapter().getIsNULLFunction();
  
  private DBConnection connection;
  private String parserName;
  private Stack elementStack;
  private String server;
  private String sep;
 
  private boolean	processingDTD;
  private String  user;
  private String[] groups;
  private String  aclid;
  private int     rev;
  private String 	docname;
  private String 	doctype;
  private String 	systemid;

  private String docurl;
  private Vector resourceURL;
  private Vector resourceID;
  private Vector principal;
  private int    permission;
  private String permType;
  private String permOrder;
  private String beginTime;
  private String endTime;
  private int    ticketCount;
  private int    serverCode = 1;

  private Vector aclObjects = new Vector();
  private boolean instarttag = true;
  private String tagName = "";
  
  private static Logger logMetacat = Logger.getLogger(AccessControlList.class);
  /**
   * Construct an instance of the AccessControlList class.
   * It is used by the permission check up from DBQuery or DocumentImpl
   * and from "getaccesscontrol" action
   *
   * @param conn the JDBC connection where acl info is get
   */
  public AccessControlList(DBConnection conn) throws SQLException
  {
    this.connection = conn;
  }
  

  

  /**
   * Construct an instance of the AccessControlList class.
   * It parse acl file and loads acl data into db connection.
   *
   * @param conn the JDBC connection where acl data are loaded
   * @param aclid the Accession# of the document with the acl data
   * @param acl the acl file containing acl data
   * @param user the user connected to MetaCat servlet and owns the document
   * @param groups the groups to which user belongs
   * @param serverCode the serverid from xml_replication on which this document
   *        resides.
   */
  public AccessControlList(DBConnection conn, String aclid, int rev,
                           String user, String[] groups, int serverCode)
                  throws SAXException, IOException, McdbException, PropertyNotFoundException
  {
		String parserName = PropertyService.getProperty("xml.saxparser");
		this.server = SystemUtil.getSecureServerURL();
		this.sep = PropertyService.getProperty("document.accNumSeparator");

		this.connection = conn;
		this.parserName = parserName;
		this.processingDTD = false;
		this.elementStack = new Stack();

		this.user = user;
		this.groups = groups;
		this.aclid = aclid;
		this.resourceURL = new Vector();
		this.resourceID = new Vector();
		this.principal = new Vector();
		this.permission = 0;
		this.ticketCount = 0;
		// this.publicAcc = null;
		this.serverCode = serverCode;

		// read the access file from db connection
		DocumentImpl acldoc = new DocumentImpl(aclid + sep + rev);
		String acl = acldoc.toString();
		this.rev = acldoc.getRev();

		// Initialize the parse
		XMLReader parser = initializeParser();
		// parse the access file and write the info to xml_access
		if (acl != null) {
			parser.parse(new InputSource(new StringReader(acl)));
		} else {
			throw new McdbException("Could not retrieve access control list for:  " + aclid + sep + rev);
		}
	}

// NOT USED
// /**
// * Construct an instance of the AccessControlList class.
//   * It parses eml-access file and loads acl data into db connection.
//   * It is used from command line execution.
//   *
//   * @param conn the JDBC connection where acl data are loaded
//   * @param docid the Accession# of the document with the acl data
//   * @param aclfilename the name of acl file containing acl data
//   * @param user the user connected to MetaCat servlet and owns the document
//   * @param groups the groups to which user belongs
//   */
//  public AccessControlList( Connection conn, String aclid, String aclfilename,
//                           String user, String[] groups )
//                  throws SAXException, IOException, McdbException
//  {
//    this(conn, aclid, new FileReader(new File(aclfilename).toString()), 
//         user, groups, 1);
//  }
  
  /* Set up the SAX parser for reading the XML serialized ACL */
  private XMLReader initializeParser() throws SAXException 
  {
    XMLReader parser = null;

    // Get an instance of the parser
    parser = XMLReaderFactory.createXMLReader(parserName);

    // Turn off validation
    parser.setFeature("http://xml.org/sax/features/validation", true);
      
    // Set Handlers in the parser
    // Set the ContentHandler to this instance
    parser.setContentHandler((ContentHandler)this);

    // make a DBEntityResolver instance
    // Set the EntityReslover to DBEntityResolver instance
    EntityResolver eresolver = new DBEntityResolver(connection,this,null);
    parser.setEntityResolver((EntityResolver)eresolver);

    // Set the ErrorHandler to this instance
    parser.setErrorHandler((ErrorHandler)this);

    return parser; 
  }
  
  /**
   * Callback method used by the SAX Parser when beginning of the document
   */
  public void startDocument() throws SAXException 
  {
    //delete all previously submitted permissions @ relations
    //this happens only on UPDATE of the access file
    try {
      this.aclObjects = getACLObjects(aclid);

      //delete all permissions for resources related to @aclid if any
      if ( aclid != null ) {
        deletePermissionsForRelatedResources(aclid);
      }
    } catch (SQLException sqle) {
      throw new SAXException(sqle);
    }
  }
  
  /**
   * Callback method used by the SAX Parser when the start tag of an 
   * element is detected. Used in this context to parse and store
   * the acl information in class variables.
   */
  public void startElement (String uri, String localName, 
                            String qName, Attributes atts) 
         throws SAXException 
  {
    instarttag = true;
    if(localName.equals("allow"))
    {
      tagName = "allow";
    }
    else if(localName.equals("deny"))
    {
      tagName = "deny";
    }
    BasicNode currentNode = new BasicNode(localName);
    if (atts != null) {
      int len = atts.getLength();
      for (int i = 0; i < len; i++) {
        currentNode.setAttribute(atts.getLocalName(i), atts.getValue(i));
      }
    }
    if ( currentNode.getTagName().equals("acl") ) {
      permOrder = currentNode.getAttribute("order");
    //  publicAcc = currentNode.getAttribute("public");
    }
    elementStack.push(currentNode); 
  }

  /**
   * Callback method used by the SAX Parser when the text sequences of an 
   * xml stream are detected. Used in this context to parse and store
   * the acl information in class variables.
   */ 
  public void characters(char ch[], int start, int length)
         throws SAXException 
  {
    if(!instarttag)
    {
      return;
    }
    String inputString = new String(ch, start, length);
    inputString = inputString.trim(); 
    //System.out.println("==============inputString: " + inputString);
    BasicNode currentNode = (BasicNode)elementStack.peek(); 
    String currentTag = currentNode.getTagName();

      if (currentTag.equals("principal")) {

        principal.addElement(inputString);

      } else if (currentTag.equals("permission")) {

        if ( inputString.trim().toUpperCase().equals("READ") ) {
          permission = permission | READ;
        } else if ( inputString.trim().toUpperCase().equals("WRITE") ) {
          permission = permission | WRITE;
        } else if ( inputString.trim().toUpperCase().equals("CHANGEPERMISSION")) 
        {
          permission = permission | CHMOD;
        } else if ( inputString.trim().toUpperCase().equals("ALL") ) {
          permission = permission | ALL;
        }/*else{
          throw new SAXException("Unknown permission type: " + inputString);
        }*/

      } else if ( currentTag.equals("startDate") && beginTime == null ) {
        beginTime = inputString.trim();

      } else if ( currentTag.equals("stopDate") && endTime == null) {
        endTime = inputString.trim();

      } else if (currentTag.equals("ticketCount") && ticketCount == 0 ) {
        try {
          ticketCount = (new Integer(inputString.trim())).intValue();
        } catch (NumberFormatException nfe) {
          throw new SAXException("Wrong integer format for:" + inputString);
        }
      }
  }

  /**
   * Callback method used by the SAX Parser when the end tag of an 
   * element is detected. Used in this context to parse and store
   * the acl information in class variables.
   */
  public void endElement (String uri, String localName, String qName)
         throws SAXException 
  {
    instarttag = false;
    BasicNode leaving = (BasicNode)elementStack.pop();
    String leavingTagName = leaving.getTagName();

    if ( leavingTagName.equals("allow") ||
         leavingTagName.equals("deny")    ) {
      
      if ( permission > 0 ) {

        // insert into db calculated permission for the list of principals
        try {
          // go through the objects in xml_relation about this acl doc
          for (int i=0; i < aclObjects.size(); i++) {
            // docid of the current object
            String docid = (String)aclObjects.elementAt(i); 
            //DocumentIdentifier docID = new DocumentIdentifier(docid);
            //docid = docID.getIdentifier();
            // the docid get here has revision number, so we need to 
            // remove it.
            //docid = MetacatUtil.getDocIdFromAccessionNumber(docid);
            //System.out.println("The docid insert is!!!!!!!!!! "+ docid);
            insertPermissions(docid,leavingTagName);
          }
          
          // if acl is not in object list
          //should insert permission for aclid itself into database
          /*if (!aclObjects.contains(aclid))
          {
            DocumentIdentifier aclIdItself = new DocumentIdentifier(aclid);
            String aclIdString = aclIdItself.getIdentifier();
            insertPermissions(aclIdString,leavingTagName);
          }*/
          

        } catch (SQLException sqle) {
          throw new SAXException(sqle);
        } catch (Exception e) {
          throw new SAXException(e);
        }
      }

      // reset the allow/deny permission
      principal = new Vector();
      permission = 0;
      beginTime = null;
      endTime = null;
      ticketCount = 0;
    
    }

  }

  /** 
    * SAX Handler that receives notification of DOCTYPE. Sets the DTD.
    * @param name name of the DTD
    * @param publicId Public Identifier of the DTD
    * @param systemId System Identifier of the DTD
    */
  public void startDTD(String name, String publicId, String systemId) 
              throws SAXException {
    docname = name;
    doctype = publicId;
    systemid = systemId;
  }

  /** 
   * SAX Handler that receives notification of the start of entities.
   * @param name name of the entity
   */
  public void startEntity(String name) throws SAXException {
    if (name.equals("[dtd]")) {
      processingDTD = true;
    }
  }

  /** 
   * SAX Handler that receives notification of the end of entities.
   * @param name name of the entity
   */
  public void endEntity(String name) throws SAXException {
    if (name.equals("[dtd]")) {
      processingDTD = false;
    }
  }

  /**
   * Get the document name.
   */
  public String getDocname() {
    return docname;
  }

  /**
   * Get the document processing state.
   */
  public boolean processingDTD() {
    return processingDTD;
  }
  
  /* Get all objects associated with @aclid from db.*/
  private Vector getACLObjects(String aclid) 
          throws SQLException 
  {
    Vector aclObjects = new Vector();
    DBConnection conn = null;
    int serialNumber = -1;
    PreparedStatement pstmt = null;
    try
    {
      //get connection from DBConnectionPool
      conn=DBConnectionPool.getDBConnection("AccessControlList.getACLObject");
      serialNumber=conn.getCheckOutSerialNumber();
      
      // delete all acl records for resources related to @aclid if any
      pstmt = conn.prepareStatement(
                             "SELECT object FROM xml_relation " +
                             "WHERE subject = ? ");
      pstmt.setString(1,aclid);
      pstmt.execute();
      ResultSet rs = pstmt.getResultSet();
      boolean hasRows = rs.next();
      while (hasRows) {
        String object = rs.getString(1);
        //System.out.println("add acl object into vector !!!!!!!!!!!!!!!!!"+object);
        aclObjects.addElement(object);
        hasRows = rs.next();
      }//whil
    }
    catch (SQLException e)
    {
      throw e;
    }
    finally
    {
      try
      {
        pstmt.close();
      }
      finally
      {
        //retrun DBConnection
        DBConnectionPool.returnDBConnection(conn,serialNumber);
      }
    }
    
    return aclObjects;
  }

  /* Delete from db all permission for resources related to @aclid if any.*/
  private void deletePermissionsForRelatedResources(String aclid) 
          throws SQLException 
  {
    //DBConnection conn = null;
    //int serialNumber = -1;
    PreparedStatement pstmt = null;
    try
    {
      //check out DBConenction
      //conn=DBConnectionPool.getDBConnection("AccessControlList.deltePerm");
      //serialNumber=conn.getCheckOutSerialNumber();
    	String sql = "DELETE FROM xml_access WHERE accessfileid = ?";
      // delete all acl records for resources related to @aclid if any
      pstmt = connection.prepareStatement(sql);
      pstmt.setString(1, aclid);
      // Increase DBConnection usage count
      connection.increaseUsageCount(1);
      logMetacat.debug("running sql: " + pstmt.toString());
      pstmt.execute();
      //increase usageCount!!!!!!
      //conn.increaseUsageCount(1);
    }
    catch (SQLException e)
    {
      throw e;
    }
    finally
    {
      pstmt.close();
      //retrun DBConnection
      //DBConnectionPool.returnDBConnection(conn,serialNumber);
    }
  }

  /* Insert into db calculated permission for the list of principals 
   * The DBConnection it is use is class field. Because we want to keep rollback
   * features and it need use same connection
  */
  
  private void insertPermissions(String docid, String permType ) 
                                            throws SQLException 
  {
    PreparedStatement pstmt = null;
    //DBConnection conn = null;
    //int serialNumber = -1;
    try {
      //Check out DBConnection
      //conn=DBConnectionPool.getDBConnection("AccessControlList.insertPerm");
      //serialNumber=conn.getCheckOutSerialNumber();
    	// TODO: look up guid? this is for very old pre-release versions of EML
      String guid = docid;
      pstmt = connection.prepareStatement(
              "INSERT INTO xml_access " + 
              "(guid, principal_name, permission, perm_type, perm_order," +
              "ticket_count, accessfileid) VALUES " +
              "(?,?,?,?,?,?,?)");
      // Increase DBConnection usage count
      connection.increaseUsageCount(1);
      // Bind the values to the query
      pstmt.setString(1, guid);
      pstmt.setInt(3, permission);
      pstmt.setString(4, permType);
      pstmt.setString(5, permOrder);
      pstmt.setString(7, aclid);
      if ( ticketCount > 0 ) {
        pstmt.setInt(6, ticketCount);
      } else {
        pstmt.setInt(6, 0);
      }
      
      //incrase usagecount for DBConnection
      //conn.increaseUsageCount(1);
      String prName;
      for ( int j = 0; j < principal.size(); j++ ) {
        prName = (String)principal.elementAt(j);
        pstmt.setString(2, prName);
        logMetacat.debug("running sql: " + pstmt.toString());
        pstmt.execute();
      /*    
        // check if there are conflict with permission's order
        String permOrderOpos = permOrder;
        int perm = getPermissions(permission, prName, docid, permOrder);
        if (  perm != 0 ) {
          if ( permOrder.equals("allowFirst") ) {
            permOrderOpos = "denyFirst";
          } else if ( permOrder.equals("denyFirst") ) {
            permOrderOpos = "allowFirst";
          }
          throw new SQLException("Permission(s) " + txtValue(perm) + 
                    " for \"" + prName + "\" on document #" + docid +
                    " has/have been used with \"" + permOrderOpos + "\"");
        }
      */
      }
      pstmt.close();

    } catch (SQLException e) {
      throw new 
      SQLException("AccessControlList.insertPermissions(): " + e.getMessage());
    }
    finally
    {
      pstmt.close();
      //return the DBConnection
      //DBConnectionPool.returnDBConnection(conn, serialNumber);
    }
  }

  /* Get permissions with permission order different than @permOrder. */
  private int getPermissions(int permission, String principal,
                             String docid, String permOrder)
          throws SQLException 
  {
    PreparedStatement pstmt = null;
    DBConnection conn = null;
    int serialNumber = -1;
    try
    {
      //check out DBConnection
      conn=DBConnectionPool.getDBConnection("AccessControlList.getPermissions");
      serialNumber=conn.getCheckOutSerialNumber();
      pstmt = conn.prepareStatement(
            "SELECT permission FROM xml_access " +
            "WHERE docid = ? " +
            "AND principal_name = ? " +
            "AND perm_order NOT = ?");
      pstmt.setString(1, docid);
      pstmt.setString(2, principal);
      pstmt.setString(3, permOrder);
      logMetacat.debug("running sql: " + pstmt.toString());
      pstmt.execute();
      ResultSet rs = pstmt.getResultSet();
      boolean hasRow = rs.next();
      int perm = 0;
      while ( hasRow ) {
        perm = rs.getInt(1);
        perm = permission & perm;
        if ( perm != 0 ) {
          pstmt.close();
          return perm;
        }
        hasRow = rs.next();
      }
    }//try
    catch (SQLException e)
    {
      throw e;
    }
    finally
    {
      try
      {
        pstmt.close();
      }
      finally
      {
        DBConnectionPool.returnDBConnection(conn, serialNumber);
      }
    }
    return 0;
  }

    /* Get the int value of READ, WRITE, CHMOD or ALL. */
	public static int intValue(String permission) {
	    
		int thisPermission = 0;
		try
		{
		    thisPermission = new Integer(permission).intValue();
		    if(thisPermission >= 0 && thisPermission <= 7)
		    {
		        return thisPermission;
		    }
		    else
		    {
		        thisPermission = -1;
		    }
		}
		catch(Exception e)
		{ //do nothing.  this must be a word
		}
		
		if (permission.toUpperCase().contains(CHMODSTRING)) {
			thisPermission |= CHMOD;
		} 
		if (permission.toUpperCase().contains(READSTRING)) {
			thisPermission |= READ;
		} 
		if (permission.toUpperCase().contains(WRITESTRING)) {
			thisPermission |= WRITE;
		} 
		if (permission.toUpperCase().contains(ALLSTRING)) {
			thisPermission |= ALL;
		}

		return thisPermission;
	}
  
  /* Get the text value of READ, WRITE, CHMOD or ALL. */
	public static String txtValue(int permission) {
		StringBuffer txtPerm = new StringBuffer();
		
		if ((permission & ALL) == ALL) {
			return ALLSTRING;
		}		
		if ((permission & CHMOD) == CHMOD) {
			txtPerm.append(CHMODSTRING);
		}
		if ((permission & READ) == READ) {
			if (txtPerm.length() > 0)
				txtPerm.append(",");
			txtPerm.append(READSTRING);
		}
		if ((permission & WRITE) == WRITE) {
			if (txtPerm.length() > 0)
				txtPerm.append(",");
			txtPerm.append(WRITESTRING);
		}

		return txtPerm.toString();
	}

  /* Get the flag for public "read" access for @docid from db conn. */
  private String getPublicAccess(String docid) throws SQLException {
    
    int publicAcc = 0;
    PreparedStatement pstmt = null;
    DBConnection conn = null;
    int serialNumber = -1;
    try
    {
      //check out DBConnection
      conn=DBConnectionPool.getDBConnection("AccessControlList.getPublicAcces");
      serialNumber=conn.getCheckOutSerialNumber();
      pstmt = conn.prepareStatement("SELECT public_access FROM xml_documents " +
                                  "WHERE docid = ?");
      pstmt.setString(1, docid);
      pstmt.execute();
      ResultSet rs = pstmt.getResultSet();
      boolean hasRow = rs.next();
      if ( hasRow ) {
        publicAcc = rs.getInt(1);
      }
    
      return (publicAcc == 1) ? "yes" : "no";
    }
    finally
    {
      try
      {
        pstmt.close();
      }
      finally
      {
        DBConnectionPool.returnDBConnection(conn, serialNumber);
      }
    }
  }

  /* Get SystemID for @publicID from Metacat DB Catalog. */
  private String getSystemID(String publicID) throws SQLException, PropertyNotFoundException {

		String systemID = "";
		PreparedStatement pstmt = null;
		DBConnection conn = null;
		int serialNumber = -1;

		try {
			//check out DBConnection
			conn = DBConnectionPool.getDBConnection("AccessControlList.getSystemID");
			serialNumber = conn.getCheckOutSerialNumber();

			pstmt = conn.prepareStatement("SELECT system_id FROM xml_catalog "
					+ "WHERE entry_type = 'DTD' " + "AND public_id = ?");
			pstmt.setString(1, publicID);
			pstmt.execute();
			ResultSet rs = pstmt.getResultSet();
			boolean hasRow = rs.next();
			if (hasRow) {
				systemID = rs.getString(1);
				// system id may not have server url on front.  Add it if not.
				if (!systemID.startsWith("http://")) {
					systemID = SystemUtil.getContextURL() + systemID;
				}
			}

			return systemID;
		}//try
		finally {
			try {
				pstmt.close();
			} finally {
				DBConnectionPool.returnDBConnection(conn, serialNumber);
			}
		}//finally
	}
  
  public static void main(String[] args) {
	  System.out.println("text value for CHMOD (" + CHMOD + "): " + txtValue(CHMOD));
	  System.out.println("text value for READ: (" + READ + "): " + txtValue(READ));
	  System.out.println("text value for WRITE: (" + WRITE + "): " + txtValue(WRITE));
	  System.out.println("text value for ALL: (" + ALL + "): " + txtValue(ALL));
	  int chmod_read = CHMOD|READ;
	  System.out.println("text value for CHMOD|READ: (" + chmod_read + "): " + txtValue(CHMOD|READ));
	  int chmod_write = CHMOD|WRITE;
	  System.out.println("text value for CHMOD|WRITE: (" + chmod_write + "): " + txtValue(CHMOD|WRITE));
	  int chmod_all = CHMOD|ALL;
	  System.out.println("text value for CHMOD|ALL: (" + chmod_all + "): " + txtValue(CHMOD|ALL));
	  int read_write = READ|WRITE;
	  System.out.println("text value for READ|WRITE: (" + read_write + "): " + txtValue(READ|WRITE));
	  int read_all = READ|ALL;
	  System.out.println("text value for READ|ALL: (" + read_all + "): " + txtValue(READ|ALL));
	  int write_all = WRITE|ALL;
	  System.out.println("text value for WRITE|ALL: (" + write_all + "): " + txtValue(WRITE|ALL));
	  int chmod_read_write = CHMOD|READ|WRITE;
	  System.out.println("text value for CHMOD|READ|WRITE: (" + chmod_read_write + "): " + txtValue(CHMOD|READ|WRITE));
	  int chmod_read_all = CHMOD|READ|ALL;
	  System.out.println("text value for CHMOD|READ|ALL: (" + chmod_read_all + "): " + txtValue(CHMOD|READ|ALL));
	  int chmod_write_all = CHMOD|WRITE|ALL;
	  System.out.println("text value for CHMOD|WRITE|ALL: (" + chmod_write_all + "): " + txtValue(CHMOD|WRITE|ALL));
	  int read_write_all = READ|WRITE|ALL;
	  System.out.println("text value for READ|WRITE|ALL: (" + read_write_all + "): " + txtValue(READ|WRITE|ALL));
	  int chmod_read_write_all = CHMOD|READ|WRITE|ALL;
	  System.out.println("text value for CHMOD|READ|WRITE|ALL: (" + chmod_read_write_all + "): " + txtValue(CHMOD|READ|WRITE|ALL));
	  System.out.println();
	  System.out.println("int value for GOOBER: " + intValue("GOOBER"));
	  System.out.println("int value for CHANGEPERMISSION: " + intValue("CHANGEPERMISSION"));
	  System.out.println("int value for READ: " + intValue("READ"));
	  System.out.println("int value for WRITE: " + intValue("WRITE"));
	  System.out.println("int value for ALL: " + intValue("ALL"));
	  System.out.println("int value for CHANGEPERMISSION,READ: " + intValue("CHANGEPERMISSION,READ"));
	  System.out.println("int value for CHANGEPERMISSION,WRITE: " + intValue("CHANGEPERMISSION,WRITE"));
	  System.out.println("int value for CHANGEPERMISSION,ALL: " + intValue("CHANGEPERMISSION,ALL"));
	  System.out.println("int value for READ,WRITE: " + intValue("READ,WRITE"));
	  System.out.println("int value for READ,ALL: " + intValue("READ,ALL"));
	  System.out.println("int value for WRITE,ALL: " + intValue("WRITE,ALL"));
	  System.out.println("int value for CHANGEPERMISSION,READ,WRITE: " + intValue("CHANGEPERMISSION,READ,WRITE"));
	  System.out.println("int value for CHANGEPERMISSION,READ,ALL: " + intValue("CHANGEPERMISSION,READ,ALL"));
	  System.out.println("int value for CHANGEPERMISSION,READ,ALL: " + intValue("CHANGEPERMISSION,WRITE,ALL"));
	  System.out.println("int value for READ,WRITE,ALL: " + intValue("READ,WRITE,ALL"));
	  System.out.println("int value for CHANGEPERMISSION,READ,WRITE,ALL: " + intValue("CHANGEPERMISSION,READ,WRITE,ALL"));
  }

}
