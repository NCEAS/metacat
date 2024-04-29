package edu.ucsb.nceas.metacat.accesscontrol;

import edu.ucsb.nceas.metacat.BasicNode;
import edu.ucsb.nceas.metacat.database.DBConnection;
import edu.ucsb.nceas.metacat.database.DBConnectionPool;
import edu.ucsb.nceas.utilities.access.AccessControlInterface;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.helpers.DefaultHandler;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Stack;
import java.util.Vector;

/**
 * A Class that loads eml-access.xml file containing ACL for a metadata
 * document into relational DB. It extends DefaultHandler class to handle
 * SAX parsing events when processing the XML stream.
 */
public class AccessControlList extends DefaultHandler
    implements AccessControlInterface, LexicalHandler {

    private DBConnection connection;
    private String parserName;
    private Stack elementStack;
    //private String server;
    private String sep;

    private boolean processingDTD;
    private String aclid;
    private String docname;
    private Vector principal;
    private int permission;
    private String permOrder;
    private String beginTime;
    private String endTime;
    private int ticketCount;

    private Vector aclObjects = new Vector();
    private boolean instarttag = true;
    private String tagName = "";

    private static Log logMetacat = LogFactory.getLog(AccessControlList.class);


    /**
     * Callback method used by the SAX Parser when beginning of the document
     */
    public void startDocument() throws SAXException {
        //delete all previously submitted permissions @ relations
        //this happens only on UPDATE of the access file
        try {
            this.aclObjects = getACLObjects(aclid);

            //delete all permissions for resources related to @aclid if any
            if (aclid != null) {
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
    @Override
    public void startElement(
        String uri, String localName, String qName, Attributes atts) throws SAXException {
        instarttag = true;
        if (localName.equals("allow")) {
            tagName = "allow";
        } else if (localName.equals("deny")) {
            tagName = "deny";
        }
        BasicNode currentNode = new BasicNode(localName);
        if (atts != null) {
            int len = atts.getLength();
            for (int i = 0; i < len; i++) {
                currentNode.setAttribute(atts.getLocalName(i), atts.getValue(i));
            }
        }
        if (currentNode.getTagName().equals("acl")) {
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
    public void characters(char[] ch, int start, int length) throws SAXException {
        if (!instarttag) {
            return;
        }
        String inputString = new String(ch, start, length);
        inputString = inputString.trim();
        //System.out.println("==============inputString: " + inputString);
        BasicNode currentNode = (BasicNode) elementStack.peek();
        String currentTag = currentNode.getTagName();

        if (currentTag.equals("principal")) {

            principal.addElement(inputString);

        } else if (currentTag.equals("permission")) {

            if (inputString.trim().toUpperCase().equals("READ")) {
                permission = permission | READ;
            } else if (inputString.trim().toUpperCase().equals("WRITE")) {
                permission = permission | WRITE;
            } else if (inputString.trim().toUpperCase().equals("CHANGEPERMISSION")) {
                permission = permission | CHMOD;
            } else if (inputString.trim().toUpperCase().equals("ALL")) {
                permission = permission | ALL;
            }/*else{
          throw new SAXException("Unknown permission type: " + inputString);
        }*/

        } else if (currentTag.equals("startDate") && beginTime == null) {
            beginTime = inputString.trim();

        } else if (currentTag.equals("stopDate") && endTime == null) {
            endTime = inputString.trim();

        } else if (currentTag.equals("ticketCount") && ticketCount == 0) {
            try {
                ticketCount = Integer.parseInt(inputString.trim());
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
    public void endElement(String uri, String localName, String qName) throws SAXException {
        instarttag = false;
        BasicNode leaving = (BasicNode) elementStack.pop();
        String leavingTagName = leaving.getTagName();

        if (leavingTagName.equals("allow") || leavingTagName.equals("deny")) {

            if (permission > 0) {

                // insert into db calculated permission for the list of principals
                try {
                    // go through the objects in xml_relation about this acl doc
                    for (int i = 0; i < aclObjects.size(); i++) {
                        // docid of the current object
                        String docid = (String) aclObjects.elementAt(i);
                        insertPermissions(docid, leavingTagName);
                    }
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
    @Override
    public void startDTD(String name, String publicId, String systemId) throws SAXException {
        processingDTD = true;
        logMetacat.debug("AccessControlList.startDTD - Setting processingDTD to true");
        logMetacat.debug("AccessControlList.startDTD - start DTD");
        docname = name;
    }

    /**
     * SAX Handler that receives notification of end of DTD
     */
    @Override
    public void endDTD() throws SAXException {

        processingDTD = false;
        logMetacat.debug("AccessControlList.endDTD - Setting processingDTD to false");
        logMetacat.debug("AccessControlList.endDTD - end DTD");
    }

    /**
     * SAX Handler that receives notification of the start of entities.
     * @param name name of the entity
     */
    public void startEntity(String name) throws SAXException {
        logMetacat.debug("AccessControlList.startEntity ");
        if (name.equals("[dtd]")) {
            logMetacat.debug("AccessControlList.startEntity  set processingDTD to true.");
            processingDTD = true;
        }
    }

    /**
     * SAX Handler that receives notification of the end of entities.
     * @param name name of the entity
     */
    public void endEntity(String name) throws SAXException {
        logMetacat.debug("AccessControlList.endEntity ");
        if (name.equals("[dtd]")) {
            logMetacat.debug("AccessControlList.endEntity  set processingDTD to false.");
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
    private Vector getACLObjects(String aclid) throws SQLException {
        Vector aclObjects = new Vector();
        DBConnection conn = null;
        int serialNumber = -1;
        PreparedStatement pstmt = null;
        try {
            //get connection from DBConnectionPool
            conn = DBConnectionPool.getDBConnection("AccessControlList.getACLObject");
            serialNumber = conn.getCheckOutSerialNumber();

            // delete all acl records for resources related to @aclid if any
            pstmt =
                conn.prepareStatement("SELECT object FROM xml_relation " + "WHERE subject = ? ");
            pstmt.setString(1, aclid);
            pstmt.execute();
            ResultSet rs = pstmt.getResultSet();
            boolean hasRows = rs.next();
            while (hasRows) {
                String object = rs.getString(1);
                //System.out.println("add acl object into vector !!!!!!!!!!!!!!!!!"+object);
                aclObjects.addElement(object);
                hasRows = rs.next();
            }//whil
        } catch (SQLException e) {
            throw e;
        } finally {
            try {
                pstmt.close();
            } finally {
                //retrun DBConnection
                DBConnectionPool.returnDBConnection(conn, serialNumber);
            }
        }

        return aclObjects;
    }

    /* Delete from db all permission for resources related to @aclid if any.*/
    private void deletePermissionsForRelatedResources(String aclid) throws SQLException {
        PreparedStatement pstmt = null;
        try {
            String sql = "DELETE FROM xml_access WHERE accessfileid = ?";
            // delete all acl records for resources related to @aclid if any
            pstmt = connection.prepareStatement(sql);
            pstmt.setString(1, aclid);
            // Increase DBConnection usage count
            connection.increaseUsageCount(1);
            logMetacat.debug("running sql: " + pstmt);
            pstmt.execute();
        } catch (SQLException e) {
            throw e;
        } finally {
            pstmt.close();
        }
    }

    /* Insert into db calculated permission for the list of principals
     * The DBConnection it is use is class field. Because we want to keep rollback
     * features, and it needs to use same connection
     */
    private void insertPermissions(String docid, String permType) throws SQLException {
        PreparedStatement pstmt = null;
        try {
            // TODO: look up guid? this is for very old pre-release versions of EML
            String guid = docid;
            pstmt = connection.prepareStatement("INSERT INTO xml_access "
                                                    + "(guid, principal_name, permission, "
                                                    + "perm_type, perm_order,"
                                                    + "ticket_count, accessfileid) VALUES "
                                                    + "(?,?,?,?,?,?,?)");
            // Increase DBConnection usage count
            connection.increaseUsageCount(1);
            // Bind the values to the query
            pstmt.setString(1, guid);
            pstmt.setInt(3, permission);
            pstmt.setString(4, permType);
            pstmt.setString(5, permOrder);
            pstmt.setString(7, aclid);
            if (ticketCount > 0) {
                pstmt.setInt(6, ticketCount);
            } else {
                pstmt.setInt(6, 0);
            }

            String prName;
            for (int j = 0; j < principal.size(); j++) {
                prName = (String) principal.elementAt(j);
                pstmt.setString(2, prName);
                logMetacat.debug("running sql: " + pstmt);
                pstmt.execute();
            }
            pstmt.close();

        } catch (SQLException e) {
            throw new SQLException("AccessControlList.insertPermissions(): " + e.getMessage());
        } finally {
            pstmt.close();
        }
    }

    /* Get the int value of READ, WRITE, CHMOD or ALL. */
    public static int intValue(String permission) {

        int thisPermission = 0;
        try {
            thisPermission = Integer.parseInt(permission);
            if (thisPermission >= 0 && thisPermission <= 7) {
                return thisPermission;
            } else {
                thisPermission = -1;
            }
        } catch (Exception e) { //do nothing.  this must be a word
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


  /**
     * SAX Handler that receives notification of comments in the DTD
     */
    public void comment(char[] ch, int start, int length) throws SAXException {
        logMetacat.trace("AccessControlList.comment - starting comment");

    }

    /**
     * SAX Handler that receives notification of the start of CDATA sections
     */
    public void startCDATA() {
        logMetacat.trace("AccessControlList.startCDATA - starting CDATA");
    }

    /**
     * SAX Handler that receives notification of the end of CDATA sections
     */
    public void endCDATA() {
        logMetacat.trace("AccessControlList.endCDATA - end CDATA");
    }

}
