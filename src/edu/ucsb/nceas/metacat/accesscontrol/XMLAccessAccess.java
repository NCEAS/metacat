package edu.ucsb.nceas.metacat.accesscontrol;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.ucsb.nceas.metacat.database.DBConnection;
import edu.ucsb.nceas.metacat.database.DBConnectionPool;
import edu.ucsb.nceas.metacat.shared.AccessException;
import edu.ucsb.nceas.metacat.shared.BaseAccess;
import edu.ucsb.nceas.utilities.access.AccessControlInterface;
import edu.ucsb.nceas.utilities.access.XMLAccessDAO;

public class XMLAccessAccess extends BaseAccess {

    private Log logMetacat = LogFactory.getLog(XMLAccessAccess.class);

    // Constructor
    public XMLAccessAccess() throws AccessException {
    }


    /**
     * Get all xml access for a document
     *
     * @param guid the id of the document
     * @return an xml access DAO list
     */
    public Vector<XMLAccessDAO> getXMLAccessForDoc(String guid) throws AccessException {

        Vector<XMLAccessDAO> xmlAccessList = new Vector<XMLAccessDAO>();

        if (guid == null) {
            throw new AccessException("XMLAccessAccess.getXMLAccessForDoc - doc id "
                                          + "must be specified when selecting xml_access record");
        }

        // first get the xml access from the db and put it into a DAO list
        PreparedStatement pstmt = null;
        DBConnection conn = null;
        int serialNumber = -1;
        try {
            conn = DBConnectionPool.getDBConnection("XMLAccessAccess.getXMLAccessForDoc");
            serialNumber = conn.getCheckOutSerialNumber();

            String sql = "SELECT * FROM xml_access WHERE guid = ?";
            pstmt = conn.prepareStatement(sql);

            pstmt.setString(1, guid);

            String sqlReport = "XMLAccessAccess.getXMLAccessForDoc - SQL: " + sql;
            sqlReport += " [" + guid + "]";

            logMetacat.debug(sqlReport);

            pstmt.execute();

            ResultSet resultSet = pstmt.getResultSet();
            while (resultSet.next()) {
                XMLAccessDAO xmlAccessDAO = populateDAO(resultSet);
                xmlAccessList.add(xmlAccessDAO);
            }

            // make sure permission orders do not conflict in the database
            validateDocXMLAccessList(xmlAccessList);

            return xmlAccessList;

        } catch (SQLException sqle) {
            throw new AccessException(
                "XMLAccessAccess.getXMLAccessForDoc - SQL error when getting access " + " for id: "
                    + guid + " : " + sqle.getMessage());
        } catch (PermOrderException poe) {
            String errorStr =
                "XMLAccessAccess.getXMLAccessForDoc - Permission order error when getting "
                    + "access record for doc id: " + guid + " : " + poe.getMessage();
            logMetacat.error(errorStr);
            throw new AccessException(errorStr);
        } finally {
            closeDBObjects(pstmt, conn, serialNumber, logMetacat);
        }
    }

    /**
     * Get all xml access for a principal for a certain document
     *
     * @param guid          the id of the document
     * @param principalName the credentials of the principal in the database
     * @return an xml access DAO list
     */
    public Vector<XMLAccessDAO> getXMLAccessForPrincipal(String guid, String principalName)
        throws AccessException {

        Vector<XMLAccessDAO> xmlAccessList = new Vector<XMLAccessDAO>();

        if (guid == null) {
            throw new AccessException("XMLAccessAccess.getXMLAccessForPrincipal - doc id "
                                          + "must be specified when selecting xml_access record");
        }
        if (principalName == null) {
            throw new AccessException("XMLAccessAccess.getXMLAccessForPrincipal - doc id "
                                          + "must be specified when selecting xml_access record");
        }

        // first get the xml access for this principal from the db and put it into a DAO list
        PreparedStatement pstmt = null;
        DBConnection conn = null;
        int serialNumber = -1;
        try {
            conn = DBConnectionPool.getDBConnection("XMLAccessAccess.getXMLAccessForPrincipal");
            serialNumber = conn.getCheckOutSerialNumber();

            String sql = "SELECT * FROM xml_access WHERE guid = ? AND principal_name = ?";
            pstmt = conn.prepareStatement(sql);

            pstmt.setString(1, guid);
            pstmt.setString(2, principalName);

            String sqlReport = "XMLAccessAccess.getXMLAccessForPrincipal - SQL: " + sql;
            sqlReport += " [" + guid + "," + principalName + "]";

            logMetacat.info(sqlReport);

            pstmt.execute();

            ResultSet resultSet = pstmt.getResultSet();
            while (resultSet.next()) {
                XMLAccessDAO xmlAccessDAO = populateDAO(resultSet);
                xmlAccessList.add(xmlAccessDAO);
            }

            // make sure permission orders do not conflict in the database
            validatePrincipalXMLAccessList(xmlAccessList);

            return xmlAccessList;

        } catch (SQLException sqle) {
            throw new AccessException(
                "XMLAccessAccess.getXMLAccessForPrincipal - SQL error when getting access "
                    + " for id: " + guid + ", principal: " + principalName + " : "
                    + sqle.getMessage());
        } catch (PermOrderException poe) {
            String errorStr =
                "XMLAccessAccess.getXMLAccessForPrincipal - Permission order error when getting "
                    + "access record for id: " + guid + ", principal: " + principalName + " : "
                    + poe.getMessage();
            logMetacat.error(errorStr);
            throw new AccessException(errorStr);
        } finally {
            closeDBObjects(pstmt, conn, serialNumber, logMetacat);
        }
    }

    /**
     * Get all xml access for a principal/permType/permOrder for a certain document
     *
     * @param guid          the id of the document
     * @param principalName the credentials of the principal in the database
     * @return an xml access DAO list
     */
    public Vector<XMLAccessDAO> getXMLAccessForPrincipal(
        String guid, String principalName, String permType, String permOrder)
        throws AccessException {

        Vector<XMLAccessDAO> xmlAccessList = new Vector<XMLAccessDAO>();

        if (guid == null) {
            throw new AccessException("XMLAccessAccess.getXMLAccessForPrincipal - doc id "
                                          + "must be specified when selecting xml_access record");
        }
        if (principalName == null) {
            throw new AccessException("XMLAccessAccess.getXMLAccessForPrincipal - doc id "
                                          + "must be specified when selecting xml_access record");
        }
        if (permType == null) {
            throw new AccessException("XMLAccessAccess.getXMLAccessForPrincipal - permission type "
                                          + "must be specified when selecting xml_access record");
        }
        if (permOrder == null) {
            throw new AccessException("XMLAccessAccess.getXMLAccessForPrincipal - permission order "
                                          + "must be specified when selecting xml_access record");
        }

        // first get the xml access for this principal from the db and put it into a DAO list
        PreparedStatement pstmt = null;
        DBConnection conn = null;
        int serialNumber = -1;
        try {
            conn = DBConnectionPool.getDBConnection("XMLAccessAccess.getXMLAccessForPrincipal");
            serialNumber = conn.getCheckOutSerialNumber();

            String sql = "SELECT * FROM xml_access WHERE guid = ? AND principal_name = ? "
                + "AND perm_type = ? AND perm_order = ?";
            pstmt = conn.prepareStatement(sql);

            pstmt.setString(1, guid);
            pstmt.setString(2, principalName);
            pstmt.setString(3, permType);
            pstmt.setString(4, permOrder);

            String sqlReport = "XMLAccessAccess.getXMLAccessForPrincipal - SQL: " + sql;
            sqlReport += " [" + guid + "," + principalName + "," + permType + "," + permOrder + "]";

            logMetacat.info(sqlReport);

            pstmt.execute();

            ResultSet resultSet = pstmt.getResultSet();
            while (resultSet.next()) {
                XMLAccessDAO xmlAccessDAO = populateDAO(resultSet);
                xmlAccessList.add(xmlAccessDAO);
            }

            validatePrincipalXMLAccessList(xmlAccessList);

            return xmlAccessList;

        } catch (SQLException sqle) {
            throw new AccessException(
                "XMLAccessAccess.getXMLAccessForPrincipal - SQL error when getting access "
                    + " for id: " + guid + ", principal: " + principalName + " : "
                    + sqle.getMessage());
        } catch (PermOrderException poe) {
            String errorStr =
                "XMLAccessAccess.getXMLAccessForPrincipal - Permission order error when getting "
                    + "access record for id: " + guid + ", principal: " + principalName + " : "
                    + poe.getMessage();
            logMetacat.error(errorStr);
            throw new AccessException(errorStr);
        } finally {
            closeDBObjects(pstmt, conn, serialNumber, logMetacat);
        }
    }

    /**
     * Add permissions for a given principal on a given document. If the principal already exists,
     * bitwise OR the permission to the existing permission and update.
     *
     * @param guid          document id
     * @param principalName principal credentials
     * @param permission    permission bitmap
     * @param permType      permission type
     * @param permOrder     permission order
     */
    public void addXMLAccess(
        String guid, String principalName, Long permission, String permType, String permOrder,
        String accessFileId, String subTreeId) throws AccessException, PermOrderException {

        permOrderConflict(guid, permOrder);

        Vector<XMLAccessDAO> xmlAccessList =
            getXMLAccessForPrincipal(guid, principalName, permType, permOrder);

        // if more than one record exists for this principal on this document with the same
        // access type / access order combination, call cleanup to combine common access and then
        // re-retrieve the access list.
        if (xmlAccessList.size() == 0) {
            insertXMLAccess(
                guid, principalName, permission, permType, permOrder, accessFileId, subTreeId);
            return;
        }

        if (xmlAccessList.size() > 1) {
            cleanupXMLAccessForPrincipal(xmlAccessList);
            xmlAccessList = getXMLAccessForPrincipal(guid, principalName, permType, permOrder);
        }

        if (xmlAccessList.size() == 0) {
            throw new AccessException(
                "XMLAccessAccess.addXMLAccess - xml access list is empty when "
                    + "it shouldn't be for id: " + guid + ", principal name: " + principalName
                    + ", perm type " + permType + ", perm order: " + permOrder);
        }

        XMLAccessDAO xmlAccessDAO = xmlAccessList.get(0);

        // if the permission on the xml access dao does not already contain the permission we are
        // trying to add, update the access record with the existing permission bitwise OR-ed with
        // our new permission
        if ((xmlAccessDAO.getPermission() & permission) != permission) {
            updateXMLAccessPermission(
                guid, principalName, xmlAccessDAO.getPermission() | permission);
        }
    }

    /**
     * Set permissions for a given document. This means first removing all access control for the
     * document and then adding the given rules.
     *
     * @param guid          document id
     * @param xmlAccessList list of xml access dao objects that hold new access for the document
     */
    public void replaceAccess(String guid, List<XMLAccessDAO> xmlAccessList)
        throws AccessException {
        deleteXMLAccessForDoc(guid);

        insertAccess(guid, xmlAccessList);
    }

    /**
     * Set permissions for a given document. This means first removing all access control for the
     * document and then adding the given rules.
     *
     * @param guid          document id
     * @param xmlAccessList list of xml access dao objects that hold new access for the document
     * @throws AccessException if there is a problem with access rights
     */
    public void insertAccess(String guid, List<XMLAccessDAO> xmlAccessList) throws AccessException {

        // if more than one record exists for this principal on this document with the same
        // access type / access order combination, call cleanup to combine common access and then
        // re-retrieve the access list.
        for (XMLAccessDAO xmlAccessDAO : xmlAccessList) {
            insertXMLAccess(guid, xmlAccessDAO.getPrincipalName(), xmlAccessDAO.getPermission(),
                            xmlAccessDAO.getPermType(), xmlAccessDAO.getPermOrder(),
                            xmlAccessDAO.getAccessFileId(), xmlAccessDAO.getSubTreeId());
        }
    }

    /**
     * Set permissions for a given document. This means first removing all access control for the
     * document and then adding the given rules.
     *
     * @param guid          document id
     * @param xmlAccessList list of xml access dao objects that hold new access for the document
     * @param conn          the database connection to run the query
     */
    public void insertAccess(String guid, List<XMLAccessDAO> xmlAccessList, DBConnection conn)
        throws AccessException, SQLException {

        // if more than one record exists for this principal on this document with the same
        // access type / access order combination, call cleanup to combine common access and then
        // re-retrieve the access list.
        for (XMLAccessDAO xmlAccessDAO : xmlAccessList) {
            insertXMLAccess(conn, guid, xmlAccessDAO.getPrincipalName(),
                            xmlAccessDAO.getPermission(), xmlAccessDAO.getPermType(),
                            xmlAccessDAO.getPermOrder(), xmlAccessDAO.getAccessFileId(),
                            xmlAccessDAO.getSubTreeId());
        }
    }

    /**
     * Insert an xml access record.  It is assumed that the checks have already been made to make
     * sure the principal does not already have an access record for this document.  If one does
     * already exist, that record should be updated and this insert not called.
     *
     * @param guid          document id
     * @param principalName principal credentials
     * @param permission    permission bitmap
     * @param permType      permission type
     * @param permOrder     permission order
     * @param accessFileId  access File id
     * @param subTreeId     subtree id
     * @throws AccessException
     */
    private void insertXMLAccess(
        String guid, String principalName, Long permission, String permType, String permOrder,
        String accessFileId, String subTreeId) throws AccessException {
        try {
            if (permission == -1) {
                throw new Exception("Permission is -1 in XMLAccessAccess.insertXMLAccess().");
            }
        } catch (Exception e) {
            e.printStackTrace();
            logMetacat.warn(e.getMessage());
        }

        if (guid == null) {
            throw new AccessException("XMLAccessAccess.insertXMLAccess -guid required when "
                                          + "inserting XML access record");
        }
        if (principalName == null) {
            throw new AccessException(
                "XMLAccessAccess.insertXMLAccess - principalName required when "
                    + "inserting XML access record");
        }
        if (permission == null) {
            throw new AccessException(
                "XMLAccessAccess.insertXMLAccess - permission is required when "
                    + "inserting XML access record");
        }
        if (permType == null) {
            throw new AccessException("XMLAccessAccess.insertXMLAccess - permType is required when "
                                          + "inserting XML access record");
        }
        if (permOrder == null) {
            permOrder = AccessControlInterface.ALLOWFIRST;
        }
        DBConnection conn = null;
        int serialNumber = -1;
        try {
            // check out DBConnection
            conn = DBConnectionPool.getDBConnection("XMLAccessAccess.insertXMLAccess");
            serialNumber = conn.getCheckOutSerialNumber();
            insertXMLAccess(conn, guid, principalName, permission, permType, permOrder,
                            accessFileId, subTreeId);
        } catch (SQLException sqle) {
            throw new AccessException("XMLAccessAccess.insertXMLAccess - SQL error when inserting"
                                          + "xml access permissions for id: " + guid
                                          + ", principal: " + principalName + ":"
                                          + sqle.getMessage());
        } finally {
            DBConnectionPool.returnDBConnection(conn, serialNumber);
        }
    }

    /**
     * Insert an xml access record.  It is assumed that the checks have already been made to make
     * sure the principal does not already have an access record for this document.  If one does
     * already exist, that record should be updated and this insert not called.
     *
     * @param conn          the database connection to run the query
     * @param guid          document id
     * @param principalName principal credentials
     * @param permission    permission bitmap
     * @param permType      permission type
     * @param permOrder     permission order
     * @param accessFileId  Access File id
     * @param subTreeId     Subtree id
     * @throws AccessException if there's an access or permissions issue
     * @throws SQLException    if there's a database problem
     */
    private void insertXMLAccess(
        DBConnection conn, String guid, String principalName, Long permission, String permType,
        String permOrder, String accessFileId, String subTreeId)
        throws AccessException, SQLException {
        try {
            if (permission == -1) {
                throw new Exception("Permission is -1 in XMLAccessAccess.insertXMLAccess().");
            }
        } catch (Exception e) {
            e.printStackTrace();
            logMetacat.warn(e.getMessage());
        }

        if (guid == null) {
            throw new AccessException("XMLAccessAccess.insertXMLAccess -guid required when "
                                          + "inserting XML access record");
        }
        if (principalName == null) {
            throw new AccessException(
                "XMLAccessAccess.insertXMLAccess - principalName required when "
                    + "inserting XML access record");
        }
        if (permission == null) {
            throw new AccessException(
                "XMLAccessAccess.insertXMLAccess - permission is required when "
                    + "inserting XML access record");
        }
        if (permType == null) {
            throw new AccessException("XMLAccessAccess.insertXMLAccess - permType is required when "
                                          + "inserting XML access record");
        }
        if (permOrder == null) {
            permOrder = AccessControlInterface.ALLOWFIRST;
        }

        PreparedStatement pstmt = null;
        try {

            String sql = "INSERT INTO xml_access "
                + "(guid, principal_name, permission, perm_type, perm_order, accessfileid, "
                + "subtreeid ) " + "VALUES (?,?,?,?,?,?,?)";
            pstmt = conn.prepareStatement(sql);
            // Bind the values to the query
            pstmt.setString(1, guid);
            pstmt.setString(2, principalName);
            pstmt.setLong(3, permission);
            pstmt.setString(4, permType);
            pstmt.setString(5, permOrder);
            pstmt.setString(6, accessFileId);
            pstmt.setString(7, subTreeId);

            String sqlReport = "XMLAccessAccess.insertXMLAccess - SQL: " + sql;
            sqlReport += " [" + guid + "," + principalName + "," + permission + "," + permType + ","
                + permOrder + "]";

            logMetacat.info(sqlReport);

            pstmt.execute();
        } catch (SQLException sqle) {
            throw new AccessException("XMLAccessAccess.insertXMLAccess - SQL error when inserting"
                                          + "xml access permissions for id: " + guid
                                          + ", principal: " + principalName + ":"
                                          + sqle.getMessage());
        } finally {
            if (pstmt != null) {
                pstmt.close();
            }
        }
    }

    /**
     * Update existing xml access permissions in the db.  The permission value should be the
     * combined value of pre-existing permissions plus new permissions.
     *
     * @param guid          document id
     * @param principalName principal credentials
     * @param permission    permission bitmap
     */
    private void updateXMLAccessPermission(String guid, String principalName, Long permission)
        throws AccessException {
        if (guid == null) {
            throw new AccessException(
                "XMLAccessAccess.updateXMLAccessPermission -guid required when "
                    + "updating XML access record");
        }
        if (principalName == null) {
            throw new AccessException(
                "XMLAccessAccess.updateXMLAccessPermission - principalName required when "
                    + "updating XML access record");
        }
        if (permission == null) {
            throw new AccessException(
                "XMLAccessAccess.updateXMLAccessPermission - permission is required when "
                    + "updating XML access record");
        }

        PreparedStatement pstmt = null;
        DBConnection conn = null;
        int serialNumber = -1;
        try {
            // check out DBConnection
            conn = DBConnectionPool.getDBConnection("XMLAccessAccess.updateXMLAccessPermission");
            serialNumber = conn.getCheckOutSerialNumber();

            String sql =
                "UPDATE xml_access SET permission = ?" + "WHERE guid = ? AND principal_name = ?";
            pstmt = conn.prepareStatement(sql);

            // Bind the values to the query
            pstmt.setLong(1, permission);
            pstmt.setString(2, guid);
            pstmt.setString(3, principalName);

            String sqlReport = "XMLAccessAccess.updateXMLAccessPermission - SQL: " + sql;
            sqlReport += " [" + permission + "," + guid + "," + principalName + "]";

            logMetacat.info(sqlReport);

            pstmt.execute();
        } catch (SQLException sqle) {
            throw new AccessException(
                "XMLAccessAccess.updateXMLAccessPermission - SQL error when updating"
                    + "xml access permissions for id: " + guid + ", principal: " + principalName
                    + ":" + sqle.getMessage());
        } finally {
            closeDBObjects(pstmt, conn, serialNumber, logMetacat);
        }

    }

    /**
     * Remove xml access.  This modifies the access in the database for a principal for a given
     * document.  If the provided permission is exactly the same as what the principal has, the
     * record is deleted from the database.
     *
     * @param guid          document id
     * @param principalName principal credentials
     */
    public void removeXMLAccessForPrincipal(String guid, String principalName, Long permission)
        throws AccessException {
        if (guid == null) {
            throw new AccessException(
                "XMLAccessAccess.removeXMLAccessForPrincipal -guid required when "
                    + "removing XML access");
        }
        if (principalName == null) {
            throw new AccessException(
                "XMLAccessAccess.removeXMLAccessForPrincipal - principalName required when "
                    + "deleting XML access");
        }
        if (permission == null) {
            throw new AccessException(
                "XMLAccessAccess.removeXMLAccessForPrincipal - permission is required when "
                    + "updating XML access");
        }

        Vector<XMLAccessDAO> xmlAccessList = getXMLAccessForPrincipal(guid, principalName);
        if (xmlAccessList.size() == 0) {
            logMetacat.warn(
                "XMLAccessAccess.removeXMLAccessForPrincipal - attempting to remove access when no "
                    + "access record exists for id: " + guid + ", principal: " + principalName);
        } else {
            long permissionMask = 0;
            for (XMLAccessDAO xmlAccessDAO : xmlAccessList) {
                permissionMask |= xmlAccessDAO.getPermission();
            }
            permissionMask |= permission;

            // in this case, the only existing permissions are the ones we want to remove, so
            // delete the record(s) for this principal on this document
            if ((permissionMask & permission) == permission) {
                deleteXMLAccessForPrincipal(guid, principalName);
            }

            if (xmlAccessList.size() > 1) {

            } else {
                updateXMLAccessPermission(guid, principalName, permission);
            }
        }

    }

    /**
     * Delete xml access.  This removes all access records from the database for a given document
     *
     * @param guid document id
     */
    public void deleteXMLAccessForDoc(String guid) throws AccessException {
        if (guid == null) {
            throw new AccessException(
                "XMLAccessAccess.deleteXMLAccessForPrincipal -guid required when "
                    + "deleting XML access record");
        }

        PreparedStatement pstmt = null;
        DBConnection conn = null;
        int serialNumber = -1;
        try {

            // check out DBConnection
            conn = DBConnectionPool.getDBConnection("XMLAccessAccess.deleteXMLAccessForDoc");
            serialNumber = conn.getCheckOutSerialNumber();

            String sql = "DELETE FROM xml_access WHERE guid = ?";
            pstmt = conn.prepareStatement(sql);

            // Bind the values to the query
            pstmt.setString(1, guid);

            String sqlReport = "XMLAccessAccess.deleteXMLAccessForDoc - SQL: " + sql;
            sqlReport += " [" + guid + "]";

            logMetacat.info(sqlReport);

            pstmt.execute();
        } catch (SQLException sqle) {
            throw new AccessException(
                "XMLAccessAccess.deleteXMLAccessForDoc - SQL error when deleting"
                    + "xml access permissions for id: " + guid + ":" + sqle.getMessage());
        } finally {
            closeDBObjects(pstmt, conn, serialNumber, logMetacat);
        }
    }

    /**
     * Delete xml access.  This removes all access records from the database for a principal for a
     * given document
     *
     * @param guid          document id
     * @param principalName principal credentials
     */
    private void deleteXMLAccessForPrincipal(String guid, String principalName)
        throws AccessException {
        if (guid == null) {
            throw new AccessException(
                "XMLAccessAccess.deleteXMLAccessForPrincipal -guid required when "
                    + "deleting XML access record");
        }
        if (principalName == null) {
            throw new AccessException(
                "XMLAccessAccess.deleteXMLAccessForPrincipal - principalName required when "
                    + "deleting XML access record");
        }

        PreparedStatement pstmt = null;
        DBConnection conn = null;
        int serialNumber = -1;
        try {
            // check out DBConnection
            conn = DBConnectionPool.getDBConnection("XMLAccessAccess.deleteXMLAccessForPrincipal");
            serialNumber = conn.getCheckOutSerialNumber();

            String sql = "DELETE FROM xml_access WHERE guid = ? AND principal_name = ?";
            pstmt = conn.prepareStatement(sql);

            // Bind the values to the query
            pstmt.setString(1, guid);
            pstmt.setString(2, principalName);

            String sqlReport = "XMLAccessAccess.deleteXMLAccessForPrincipal - SQL: " + sql;
            sqlReport += " [" + guid + "," + principalName + "]";

            logMetacat.info(sqlReport);

            pstmt.execute();
        } catch (SQLException sqle) {
            throw new AccessException(
                "XMLAccessAccess.deleteXMLAccessForPrincipal - SQL error when deleting"
                    + "xml access permissions for id: " + guid + ", principal: " + principalName
                    + ":" + sqle.getMessage());
        } finally {
            closeDBObjects(pstmt, conn, serialNumber, logMetacat);
        }
    }


    /**
     * Delete xml access.  This removes all access records from the database for a principal for a
     * given document
     *
     * @param guid     document id
     * @param permType perm type
     * @param conn     the db connection which will be used to run the delete query
     */
    public void deleteXMLAccessForDoc(String guid, String permType, DBConnection conn)
        throws AccessException, SQLException {
        if (guid == null) {
            throw new AccessException("XMLAccessAccess.deleteXMLAccessForDoc - guid required when "
                                          + "deleting XML access record");
        }
        if (permType == null) {
            throw new AccessException(
                "XMLAccessAccess.deleteXMLAccessForDoc - permType is required when "
                    + "deleting XML access record");
        }

        PreparedStatement pstmt = null;
        try {
            String sql = "DELETE FROM xml_access WHERE guid = ? AND perm_type = ?";
            pstmt = conn.prepareStatement(sql);

            // Bind the values to the query
            pstmt.setString(1, guid);
            pstmt.setString(2, permType);

            String sqlReport = "XMLAccessAccess.deleteXMLAccessForDoc - SQL: " + sql;
            sqlReport += " [" + guid + "," + permType + "]";

            logMetacat.info(sqlReport);

            pstmt.execute();
        } catch (SQLException sqle) {
            throw new AccessException(
                "XMLAccessAccess.deleteXMLAccessForDoc - SQL error when deleting"
                    + "xml access permissions for id: " + guid + ", permType: " + permType + ":"
                    + sqle.getMessage());
        } finally {
            if (pstmt != null) {
                pstmt.close();
            }
        }
    }

    /**
     * Checks to see if there is a permission order conflict for a given document.  Each document is
     * only allowed to have a single permission order
     *
     * @param guid      document id
     * @param permOrder perm Order
     */
    private void permOrderConflict(String guid, String permOrder)
        throws AccessException, PermOrderException {
        if (guid == null) {
            throw new AccessException("XMLAccessAccess.permOrderConflict -guid required when "
                                          + "determining perm order conflict");
        }
        if (permOrder == null) {
            throw new AccessException(
                "XMLAccessAccess.permOrderConflict - perm order is required when "
                    + "determining perm order conflict");
        }

        PreparedStatement pstmt = null;
        DBConnection conn = null;
        int serialNumber = -1;
        try {
            // check out DBConnection
            conn = DBConnectionPool.getDBConnection("XMLAccessAccess.permOrderConflict");
            serialNumber = conn.getCheckOutSerialNumber();

            String sql = "SELECT * FROM xml_access WHERE guid = ? AND perm_order != ?";
            pstmt = conn.prepareStatement(sql);

            // Bind the values to the query
            pstmt.setString(1, guid);
            pstmt.setString(2, permOrder);

            String sqlReport = "XMLAccessAccess.permOrderConflict - SQL: " + sql;
            sqlReport += " [" + guid + "," + permOrder + "]";

            logMetacat.info(sqlReport);

            pstmt.execute();

            ResultSet resultSet = pstmt.getResultSet();
            if (resultSet.next()) {
                throw new PermOrderException(
                    "XMLAccessAccess.addXMLAccess - cannot add permission " + "record for id: "
                        + guid + "with permOrder: " + permOrder + " due to permOrder conflict");
            }
        } catch (SQLException sqle) {
            throw new AccessException("XMLAccessAccess.permOrderConflict - SQL error when checking"
                                          + "for perm order conflict on: " + guid + ":"
                                          + sqle.getMessage());
        } finally {
            closeDBObjects(pstmt, conn, serialNumber, logMetacat);
        }

    }

    /**
     * Delete xml access.  This removes all access records from the database for a principal for a
     * given document, perm type and perm order
     *
     * @param guid          document id
     * @param principalName principal credentials
     * @param permType      perm Type
     * @param permOrder     perm Order
     * @throws AccessException
     */
    private void deleteXMLAccessForPrincipal(
        String guid, String principalName, String permType, String permOrder)
        throws AccessException {
        if (guid == null) {
            throw new AccessException(
                "XMLAccessAccess.deleteXMLAccessForPrincipal -guid required when "
                    + "deleting XML access record");
        }
        if (principalName == null) {
            throw new AccessException(
                "XMLAccessAccess.deleteXMLAccessForPrincipal - principalName required when "
                    + "deleting XML access record");
        }
        if (permType == null) {
            throw new AccessException(
                "XMLAccessAccess.deleteXMLAccessForPrincipal - perm type is required when "
                    + "deleting XML access record");
        }
        if (permOrder == null) {
            throw new AccessException(
                "XMLAccessAccess.deleteXMLAccessForPrincipal - perm order is required when "
                    + "deleting XML access record");
        }

        PreparedStatement pstmt = null;
        DBConnection conn = null;
        int serialNumber = -1;
        try {
            // check out DBConnection
            conn = DBConnectionPool.getDBConnection("XMLAccessAccess.deleteXMLAccessForPrincipal");
            serialNumber = conn.getCheckOutSerialNumber();

            String sql = "DELETE FROM xml_access WHERE guid = ? AND principal_name = ? "
                + "AND perm_type = ? AND perm_order = ?";
            pstmt = conn.prepareStatement(sql);

            // Bind the values to the query
            pstmt.setString(1, guid);
            pstmt.setString(2, principalName);
            pstmt.setString(3, permType);
            pstmt.setString(4, permOrder);

            String sqlReport = "XMLAccessAccess.deleteXMLAccessForPrincipal - SQL: " + sql;
            sqlReport += " [" + guid + "," + principalName + "," + permType + "," + permOrder + "]";

            logMetacat.info(sqlReport);

            pstmt.execute();
        } catch (SQLException sqle) {
            throw new AccessException(
                "XMLAccessAccess.deleteXMLAccessForPrincipal - SQL error when deleting"
                    + "xml access permissions for id: " + guid + ", principal: " + principalName
                    + ":" + sqle.getMessage());
        } finally {
            closeDBObjects(pstmt, conn, serialNumber, logMetacat);
        }

    }

    /**
     * Make sure that only one record exists per principal/permType/document. If more than one
     * record exists, delete the existing records, consolidate the permissions insert the new
     * record.
     *
     * @param xmlAccessList the access dao list
     */
    private void cleanupXMLAccessForPrincipal(Vector<XMLAccessDAO> xmlAccessList)
        throws AccessException {

        int numAllowRecords = 0;
        int numDenyRecords = 0;
        long allowPermissionMask = 0;
        long denyPermissionMask = 0;
        String guid = null;
        String principalName = null;
        String permType = null;
        String permOrder = null;
        // TODO: handle these fields
        String accessFileId = null;
        String subTreeId = null;


        // iterate through the list of access dao objects and bitwise OR the permissions.  Most
        // of this is just doing some error checking to make sure each record is valid.
        for (XMLAccessDAO xmlAccessDAO : xmlAccessList) {
            String daoId = xmlAccessDAO.getGuid();
            if (guid == null) {
                guid = daoId;
            } else {
                if (!guid.equals(daoId)) {
                    throw new AccessException(
                        "XMLAccessAccess.cleanupXMLAccessForPrincipal - " + " Conflicting ids "
                            + daoId + " and " + guid);
                }
            }
            if (principalName == null) {
                principalName = xmlAccessDAO.getPrincipalName();
            } else {
                if (!principalName.equals(xmlAccessDAO.getPrincipalName())) {
                    throw new AccessException("XMLAccessAccess.cleanupXMLAccessForPrincipal - "
                                                  + " Conflicting principal names "
                                                  + xmlAccessDAO.getPrincipalName()
                                                  + " and principalName " + principalName);
                }
            }
            if (permType == null) {
                permType = xmlAccessDAO.getPermType();
            } else {
                if (!permType.equals(xmlAccessDAO.getPermType())) {
                    throw new AccessException("XMLAccessAccess.cleanupXMLAccessForPrincipal - "
                                                  + " Conflicting permission orders for document "
                                                  + daoId + "principalName " + principalName
                                                  + ". Database intervention required ");
                }
            }
            if (permOrder == null) {
                permOrder = xmlAccessDAO.getPermOrder();
            } else {
                if (!permOrder.equals(xmlAccessDAO.getPermOrder())) {
                    throw new AccessException("XMLAccessAccess.cleanupXMLAccessForPrincipal - "
                                                  + " Conflicting permission types for document "
                                                  + daoId + "principalName " + principalName
                                                  + ". Database intervention required ");
                }
            }
            if (permType == null) {
                permType = xmlAccessDAO.getPermType();
            } else {
                if (!permType.equals(xmlAccessDAO.getPermType())) {
                    throw new AccessException("XMLAccessAccess.cleanupXMLAccessForPrincipal - "
                                                  + " Conflicting permission orders for document "
                                                  + daoId + "principalName " + principalName
                                                  + ". Database intervention required ");
                }
            }
            if (permType.equals(AccessControlInterface.ALLOW)) {
                numAllowRecords++;
                allowPermissionMask |= xmlAccessDAO.getPermission();
            } else if (permType.equals(AccessControlInterface.DENY)) {
                numDenyRecords++;
                denyPermissionMask |= xmlAccessDAO.getPermission();
            }
            if (accessFileId == null) {
                accessFileId = xmlAccessDAO.getAccessFileId();
            }
            if (subTreeId == null) {
                subTreeId = xmlAccessDAO.getSubTreeId();
            }
        }

        // if there was more than one allow record, remove all allow records for this user on
        // this doc with this perm type and perm order then insert a single record
        if (numAllowRecords > 1) {
            deleteXMLAccessForPrincipal(
                guid, principalName, AccessControlInterface.ALLOW, permOrder);
            insertXMLAccess(guid, principalName, allowPermissionMask, AccessControlInterface.ALLOW,
                            permOrder, accessFileId, subTreeId);
        }
        // if there was more than one deny record, remove all deny records for this user on this
        // doc with this perm type and perm order then insert a single record
        if (numDenyRecords > 1) {
            deleteXMLAccessForPrincipal(
                guid, principalName, AccessControlInterface.DENY, permOrder);
            insertXMLAccess(guid, principalName, denyPermissionMask, AccessControlInterface.DENY,
                            permOrder, accessFileId, subTreeId);
        }
    }

    /**
     * Make sure for a given list of access DAOs that only one perm order exists. It is assumed that
     * all the DAOs are for the same doc
     *
     * @param xmlAccessList the access dao list
     */
    private void validateDocXMLAccessList(Vector<XMLAccessDAO> xmlAccessList)
        throws PermOrderException {
        String permOrder = null;
        for (XMLAccessDAO xmlAccessDAO : xmlAccessList) {
            String daoId = xmlAccessDAO.getGuid();
            if (permOrder == null) {
                permOrder = xmlAccessDAO.getPermOrder();
            } else {
                if (!permOrder.equals(xmlAccessDAO.getPermOrder())) {
                    throw new PermOrderException("XMLAccessAccess.validateXMLAccessList - "
                                                     + " Conflicting permission orders for "
                                                     + "document " + daoId
                                                     + ". Database intervention required ");
                }
            }
        }
    }

    /**
     * Check that only one permOrder exists for each principal.
     * TODO add check that one of each permType exists as well
     *
     * @param xmlAccessList the access dao list
     */
    private void validatePrincipalXMLAccessList(Vector<XMLAccessDAO> xmlAccessList)
        throws PermOrderException {

        boolean allowFirst = false;
        boolean denyFirst = false;
        String guid = null;

        // These vectors will hold all combinations of access DAOs with different permission
        // orders and permission types.
        Vector<XMLAccessDAO> allowFirstAllows = new Vector<XMLAccessDAO>();
        Vector<XMLAccessDAO> allowFirstDenys = new Vector<XMLAccessDAO>();
        Vector<XMLAccessDAO> denyFirstAllows = new Vector<XMLAccessDAO>();
        Vector<XMLAccessDAO> denyFirstDenys = new Vector<XMLAccessDAO>();

        // sort the access dao records into the appropriate vector
        for (XMLAccessDAO xmlAccessDAO : xmlAccessList) {
            String daoId = xmlAccessDAO.getGuid();
            if (guid == null) {
                guid = daoId;
            }
            if (xmlAccessDAO.getPermOrder().equals(AccessControlInterface.ALLOWFIRST)) {
                allowFirst = true;
                if (xmlAccessDAO.getPermType().equals(AccessControlInterface.ALLOW)) {
                    allowFirstAllows.add(xmlAccessDAO);
                } else if (xmlAccessDAO.getPermType().equals(AccessControlInterface.DENY)) {
                    allowFirstDenys.add(xmlAccessDAO);
                } else {
                    throw new PermOrderException("XMLAccessAccess.validatePrincipalXMLAccessList - "
                                                     + " Invalid permission type: "
                                                     + xmlAccessDAO.getPermType() + " for document "
                                                     + daoId + ". Database intervention required ");
                }
            } else if (xmlAccessDAO.getPermOrder().equals(AccessControlInterface.DENYFIRST)) {
                denyFirst = true;
                if (xmlAccessDAO.getPermType().equals(AccessControlInterface.ALLOW)) {
                    denyFirstAllows.add(xmlAccessDAO);
                } else if (xmlAccessDAO.getPermType().equals(AccessControlInterface.DENY)) {
                    denyFirstDenys.add(xmlAccessDAO);
                } else {
                    throw new PermOrderException("XMLAccessAccess.validatePrincipalXMLAccessList - "
                                                     + " Invalid permission type: "
                                                     + xmlAccessDAO.getPermType() + " for document "
                                                     + daoId + ". Database intervention required ");
                }
            } else {
                throw new PermOrderException("XMLAccessAccess.validatePrincipalXMLAccessList - "
                                                 + " Invalid permission order: "
                                                 + xmlAccessDAO.getPermOrder() + " for document "
                                                 + daoId + ". Database intervention required ");
            }
        }

        // for a given user, there cannot be allowfirst and denyfirst records on the same
        // document
        if (allowFirst && denyFirst) {
            throw new PermOrderException("XMLAccessAccess.validatePrincipalXMLAccessList - "
                                             + " Conflicting permission orders for document " + guid
                                             + ". Database intervention required ");
        }
    }

    /**
     * Populate a job data object with the current row in a resultset
     *
     * @param resultSet the result set which is already pointing to the desired row.
     * @return a scheduled job data object
     */
    protected XMLAccessDAO populateDAO(ResultSet resultSet) throws SQLException {

        XMLAccessDAO xmlAccessDAO = new XMLAccessDAO();
        xmlAccessDAO.setGuid(resultSet.getString("guid"));
        xmlAccessDAO.setAccessFileId(resultSet.getString("accessfileid"));
        xmlAccessDAO.setPrincipalName(resultSet.getString("principal_name"));
        xmlAccessDAO.setPermission(resultSet.getLong("permission"));
        xmlAccessDAO.setPermType(resultSet.getString("perm_type"));
        xmlAccessDAO.setPermOrder(resultSet.getString("perm_order"));
        xmlAccessDAO.setBeginTime(resultSet.getDate("begin_time"));
        xmlAccessDAO.setEndTime(resultSet.getDate("end_time"));
        xmlAccessDAO.setTicketCount(resultSet.getLong("ticket_count"));
        xmlAccessDAO.setSubTreeId(resultSet.getString("subtreeid"));
        xmlAccessDAO.setStartNodeId(resultSet.getString("startnodeid"));
        xmlAccessDAO.setEndNodeId(resultSet.getString("endnodeid"));

        return xmlAccessDAO;
    }

}
