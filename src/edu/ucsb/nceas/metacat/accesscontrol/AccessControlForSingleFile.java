package edu.ucsb.nceas.metacat.accesscontrol;

import java.io.IOException;
import java.io.StringReader;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.xml.sax.ContentHandler;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import edu.ucsb.nceas.metacat.DBUtil;
import edu.ucsb.nceas.metacat.IdentifierManager;
import edu.ucsb.nceas.metacat.McdbDocNotFoundException;
import edu.ucsb.nceas.metacat.McdbException;
import edu.ucsb.nceas.metacat.PermissionController;
import edu.ucsb.nceas.metacat.database.DBConnection;
import edu.ucsb.nceas.metacat.database.DBConnectionPool;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.metacat.shared.AccessException;
import edu.ucsb.nceas.metacat.util.DocumentUtil;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;
import edu.ucsb.nceas.utilities.access.AccessControlInterface;
import edu.ucsb.nceas.utilities.access.DocInfoHandler;
import edu.ucsb.nceas.utilities.access.XMLAccessDAO;


/** 
 * A Class that loads eml-access.xml file containing ACL for a metadata
 * document into relational DB. It extends DefaultHandler class to handle
 * SAX parsing events when processing the XML stream.
 */
public class AccessControlForSingleFile implements AccessControlInterface 
{

  private String _guid;
  
  private Log logMetacat = LogFactory.getLog(AccessControlForSingleFile.class);
 
    /**
	 * Construct an instance of the AccessControlForSingleFile class.  This
	 * instance will represent one file only.
	 * 
	 * @param myAccessNumber
	 *            the docid or docid with dev will be controlled
	 */
	public AccessControlForSingleFile(String accessionNumber)
			throws AccessControlException {

		// this is a local metacat id
		String docid = DocumentUtil.getDocIdFromString(accessionNumber);
		// get the revision
		String revision = DocumentUtil.getRevisionStringFromString(accessionNumber);
		int rev = -1;
		if (revision != null) {
			// we were given it
			rev = Integer.valueOf(revision);
		} else {
			// look up the latest
			try {
				rev = DBUtil.getLatestRevisionInDocumentTable(docid);
			} catch (SQLException e) {
				AccessControlException ace = new AccessControlException(e.getMessage());
				ace.initCause(e);
				throw ace;
			}
			if (rev <= 0) {
				// look in the revisions table
				try {
					rev = DBUtil.getMaxRevFromRevisionTable(docid);
				} catch (SQLException e) {
					AccessControlException ace = new AccessControlException(e.getMessage());
					ace.initCause(e);
					throw ace;
				}
			}
		}
		
		// find the guid for this docid.rev
		try {
			_guid = IdentifierManager.getInstance().getGUID(docid, rev);
		} catch (McdbDocNotFoundException e) {
			AccessControlException ace = new AccessControlException(e.getMessage());
			ace.initCause(e);
			throw ace;
		}
		
		// couldn't find it?
		if (_guid == null || _guid.equals("")) {
			throw new AccessControlException("Guid cannot be null");
		}

		logMetacat.debug("AccessControlForSingleFile() - docid: " + _guid);
	}
  
  	/**
	 * Insert a single access record into the database based on access DAO
	 * object
	 * 
	 * @param xmlAccessDAO
	 *            dao object holding info to insert
	 */
	public void insertPermissions(XMLAccessDAO xmlAccessDAO) 
			throws AccessControlException, PermOrderException{
		insertPermissions(xmlAccessDAO.getPrincipalName(), xmlAccessDAO.getPermission(), 
				xmlAccessDAO.getPermType(), xmlAccessDAO.getPermOrder(), xmlAccessDAO.getAccessFileId(), xmlAccessDAO.getSubTreeId());
	}

	/**
	 * Insert a single access record into the database.
	 * 
	 * @param principalName
	 *            the principal credentials
	 * @param permission
	 *            the permission
	 * @param permType
	 *            the permission type
	 * @param permOrder
	 *            the permission order
	 */
	public void insertPermissions(String principalName, Long permission, String permType, String permOrder, String accessFileId, String subTreeId) 
			throws AccessControlException, PermOrderException {
		try {
			// The addXMLAccess method will create the permission record if it does not exist.  
			// It will bitwise OR to permissions if the principal already has a record for this
			// doc id.
			XMLAccessAccess xmlAccessAccess = new XMLAccessAccess();
			//System.out.println("permission in accessControlForSingleFile.insertPermissions: " + permission);
			xmlAccessAccess.addXMLAccess(_guid, principalName, new Long(permission), permType, permOrder, accessFileId, subTreeId);
		} catch (AccessException ae) {
			throw new AccessControlException("AccessControlForSingleFile.insertPermissions - "
					+ "DB access error when inserting permissions: " + ae.getMessage());
		} 
	}
  
	/**
	 * Replace existing permissions with a given block of permissions for this
	 * document.
	 * 
	 * @param accessBlock
	 *            the xml access block. This is the same structure as that
	 *            returned by the getdocumentinfo action in metacat.
	 */
	public void insertPermissions(String accessBlock) throws AccessControlException {
		try {	
			// use DocInfoHandler to parse the access section into DAO objects
			XMLReader parser = null;
			DocInfoHandler docInfoHandler = new DocInfoHandler(_guid); 
			ContentHandler chandler = docInfoHandler;

			// Get an instance of the parser
			String parserName = PropertyService.getProperty("xml.saxparser");
			parser = XMLReaderFactory.createXMLReader(parserName);

			// Turn off validation
			parser.setFeature("http://xml.org/sax/features/validation", false);
			parser.setContentHandler((ContentHandler)chandler);
			parser.setErrorHandler((ErrorHandler)chandler);

			parser.parse(new InputSource(new StringReader(accessBlock)));
			
			XMLAccessAccess xmlAccessAccess = new XMLAccessAccess();
				
			// replace all access on the document
	        Vector<XMLAccessDAO> accessControlList = docInfoHandler.getAccessControlList();
	        xmlAccessAccess.replaceAccess(_guid, accessControlList);

		} catch (PropertyNotFoundException pnfe) {
			throw new AccessControlException("AccessControlForSingleFile.insertPermissions - "
					+ "property error when replacing permissions: " + pnfe.getMessage());
		} catch (AccessException ae) {
			throw new AccessControlException("AccessControlForSingleFile.insertPermissions - "
					+ "DB access error when replacing permissions: " + ae.getMessage());
		} catch (SAXException se) {
			throw new AccessControlException("AccessControlForSingleFile.insertPermissions - "
					+ "SAX error when replacing permissions: " + se.getMessage());
		} catch(IOException ioe) {
			throw new AccessControlException("AccessControlForSingleFile.insertPermissions - "
					+ "I/O error when replacing permissions: " + ioe.getMessage());
		}
	}
  
	/**
	 * Check if access control comination for
	 * docid/principal/permission/permorder/permtype already exists.
	 * 
	 * @param xmlAccessDAO
	 *            the dao object holding the access we want to check for.
	 * @return true if the Access Control for this file already exists in the DB
	 */
	public boolean accessControlExists(XMLAccessDAO xmlAccessDAO) throws AccessControlException {
		boolean exists = false;
		PreparedStatement pstmt = null;
		DBConnection conn = null;
		int serialNumber = -1;
		try {
			//check out DBConnection
			conn=DBConnectionPool.getDBConnection
                               ("AccessControlForSingleFiel.accessControlExists");
			serialNumber=conn.getCheckOutSerialNumber();
			pstmt = conn.prepareStatement(
				"SELECT * FROM xml_access " + 
				"WHERE guid = ? " +
				"AND principal_name = ? " +
				"AND permission = ? " +
				"AND perm_type = ? " +
				"AND perm_order = ? ");
     
			// Bind the values to the query
			pstmt.setString(1, _guid);
			pstmt.setString(2, xmlAccessDAO.getPrincipalName());
			pstmt.setLong(3, xmlAccessDAO.getPermission());
			pstmt.setString(4, xmlAccessDAO.getPermType());
			pstmt.setString(5, xmlAccessDAO.getPermOrder());
      
			pstmt.execute();
			ResultSet rs = pstmt.getResultSet();
			exists = rs.next();
      
		} catch (SQLException sqle){
			throw new AccessControlException("AccessControlForSingleFile.accessControlExists - SQL error when " +  
					"checking if access control exists: " + sqle.getMessage());
		} finally {
			try {
				if(pstmt != null) {
					pstmt.close();
				}
			} catch (SQLException sqle) {
				logMetacat.error("AccessControlForSingleFile.accessControlExists - Could not close " + 
						"prepared statement: " +sqle.getMessage());
			} finally {
				DBConnectionPool.returnDBConnection(conn, serialNumber);
			}
		}
    
		return exists;  
	}
  
	/**
	 * Get Access Control List information for document from db connetion. User
	 * or Group should have permissions for reading access control information
	 * for a document specified by
	 * 
	 * @param user
	 *            name of user connected to Metacat system
	 * @param groups
	 *            names of user's groups to which user belongs
	 */
	public String getACL(String user, String[] groups)
			throws AccessControlException {
		StringBuffer output = new StringBuffer();
		boolean hasPermission = false;

		try {   
			hasPermission = isOwned(user);
			if (!hasPermission) {
				// get the docid for this guid
				String docid = IdentifierManager.getInstance().getLocalId(_guid);
				PermissionController controller = new PermissionController(docid);
				hasPermission = 
					controller.hasPermission(user, groups, READSTRING);
			}

			// if the user has permissions, get the access dao list for this doc and return
			// it as a string.  Otherwise, get the string for an empty access dao list 
			// (which will return the access section with no allow or deny sections)
			if (hasPermission) {
				// Get a list of all access dao objects for this docid
				XMLAccessAccess xmlAccessAccess = new XMLAccessAccess();
				Vector<XMLAccessDAO> xmlAccessDAOList = xmlAccessAccess.getXMLAccessForDoc(_guid);
				output.append(getAccessString(xmlAccessDAOList));
			} else {
				output.append(getAccessString(new Vector<XMLAccessDAO>()));
			}

			return output.toString();

		} catch (SQLException sqle) {
			throw new AccessControlException("AccessControlForSingleFile.getACL() - SQL error when " + 
					"getting ACL: " + sqle.getMessage());
		} catch (AccessException ae) {
			throw new AccessControlException("AccessControlForSingleFile.getACL() - DB access error when " + 
					"getting ACL: " + ae.getMessage());
		} catch (McdbException mcdb) {
			throw new AccessControlException("AccessControlForSingleFile.getACL() - MCDB error when " + 
					"getting ACL: " + mcdb.getMessage());
		}
	}
	
	/**
	 * Get the access xml for all access on this docid
	 * 
	 * @return string representation of access
	 */
	public String getAccessString() throws AccessControlException {
		Vector<XMLAccessDAO> xmlAccessDAOList = null;
		
		try {
			// Get a list of all access dao objects for this docid
			XMLAccessAccess xmlAccessAccess = new XMLAccessAccess();
			xmlAccessDAOList = xmlAccessAccess.getXMLAccessForDoc(_guid);
		} catch (AccessException ae) {
				throw new AccessControlException("AccessControlForSingleFile.getAccessString() - DB access error when " + 
						"getting access string: " + ae.getMessage());
		} 
		
		return getAccessString(xmlAccessDAOList);
	}
	
	/**
	 * Put together an xml representation of the objects in a given access dao list
	 * @param xmlAccessDAOList list of xml access DAO objects
	 * @return string representation of access
	 */
	public String getAccessString(Vector<XMLAccessDAO> xmlAccessDAOList) throws AccessControlException {
			
		StringBuffer output = new StringBuffer();
		StringBuffer tmpOutput = new StringBuffer();
		StringBuffer allowOutput = new StringBuffer();
		StringBuffer denyOutput = new StringBuffer();
		
		String principal = null;
		int permission = -1;
		String permOrder = ALLOWFIRST;
		String permType = null;
		String accessfileid = null;
		String subtreeid = null;
		
		// We assume that all the records will have the same permission order, so we can just
		// grab the perm order from the first one.
		if (xmlAccessDAOList.size() > 0) {
			permOrder = xmlAccessDAOList.get(0).getPermOrder();
			accessfileid = xmlAccessDAOList.get(0).getAccessFileId();
			subtreeid = xmlAccessDAOList.get(0).getSubTreeId();
		}
		
		// get the docid for this guid
		String docid = _guid;
		try {
			docid = IdentifierManager.getInstance().getLocalId(_guid);
		} catch (McdbDocNotFoundException e) {
			logMetacat.warn("Could not lookup docid for guid, defaulting to guid: " + _guid, e);
		} catch (SQLException e){
		    throw new AccessControlException("Couldn't identify the local id of the object with the specified identifier "+_guid+" since "+e.getMessage());
		}

		output.append("<access authSystem=\"knb\" order=\"" + permOrder + "\" id=\"" + docid + "\" scope=\"document\"");
		if (accessfileid != null) {
			output.append(" accessfileid=\"" + accessfileid + "\"");
		}
		if (subtreeid != null) {
			output.append(" subtreeid=\"" + subtreeid + "\"");
		}
		
		output.append(">\n");
		
		for (XMLAccessDAO xmlAccessDAO : xmlAccessDAOList) {
			principal = xmlAccessDAO.getPrincipalName();
			permission = xmlAccessDAO.getPermission().intValue();
			//System.out.println("accessControlForSingleFile.getAccessString: permission is set to: " + permission);
			permType = xmlAccessDAO.getPermType();
			
			tmpOutput.append("    <" + permType + ">\n");
			tmpOutput.append("      <principal>" + principal + "</principal>\n");
	
			if ((permission & READ) == READ) {
				tmpOutput.append("      <permission>read</permission>\n");
			}
			if ((permission & WRITE) == WRITE) {
				tmpOutput.append("      <permission>write</permission>\n");
			}
			if ((permission & ALL) == ALL) {
				tmpOutput.append("      <permission>all</permission>\n");
			}
			if ((permission & CHMOD) == CHMOD) {
				tmpOutput.append("      <permission>chmod</permission>\n");
			}

			tmpOutput.append("    </" + permType + ">\n");
			
			if (permType.equals(ALLOW)) {
				allowOutput.append(tmpOutput);
			} else if (permType.equals(DENY)) {
				denyOutput.append(tmpOutput);
			}
			tmpOutput = new StringBuffer();
		}
		
		// This just orders the allow/deny sections based on the permOrder.  Not 
		// required, but convenient later when parsing the output.
		if (permOrder.equals(ALLOWFIRST)) {
			output.append(allowOutput);
			output.append(denyOutput);
		} else if (permOrder.equals(DENYFIRST)) {
			output.append(denyOutput);
			output.append(allowOutput);
		}
		
		output.append("</access>");
					
		return output.toString();
	}
	
	/**
	 * check if the docid represented in this class is owned by the user
	 * 
	 * @param user
	 *            the user credentials
	 * @return true if doc is owned by user, false otherwise
	 */
	private boolean isOwned(String user) throws SQLException {
		PreparedStatement pstmt = null;
		DBConnection conn = null;
		int serialNumber = -1;
		try {
			// check out DBConnection
			conn = DBConnectionPool.getDBConnection("AccessControlList.isOwned");
			serialNumber = conn.getCheckOutSerialNumber();
			String query = 
				"SELECT id.guid FROM xml_documents xd, identifier id "
				+ "WHERE xd.docid = id.docid " +
						"AND xd.rev = id.rev " +
						"AND id.guid = ? " + 
						"AND user_owner = ? ";
			pstmt = conn.prepareStatement(query );
			pstmt.setString(1, _guid);
			pstmt.setString(2, user);
			pstmt.execute();
			ResultSet rs = pstmt.getResultSet();
			boolean hasRow = rs.next();
			return hasRow;
		} finally {
			try {
				if (pstmt != null) {
					pstmt.close();
				}
			} finally {
				DBConnectionPool.returnDBConnection(conn, serialNumber);
			}
		}
	}
}
