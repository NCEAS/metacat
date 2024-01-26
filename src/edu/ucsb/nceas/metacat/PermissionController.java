package edu.ucsb.nceas.metacat;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Hashtable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Session;
import org.dataone.service.types.v1.Subject;
import org.dataone.service.types.v2.SystemMetadata;

import edu.ucsb.nceas.metacat.accesscontrol.AccessControlList;
import edu.ucsb.nceas.metacat.database.DBConnection;
import edu.ucsb.nceas.metacat.database.DBConnectionPool;
import edu.ucsb.nceas.metacat.dataone.D1AuthHelper;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.metacat.service.SessionService;
import edu.ucsb.nceas.metacat.shared.MetacatUtilException;
import edu.ucsb.nceas.metacat.systemmetadata.SystemMetadataManager;
import edu.ucsb.nceas.metacat.util.AuthUtil;
import edu.ucsb.nceas.metacat.util.DocumentUtil;
import edu.ucsb.nceas.metacat.util.MetacatUtil;
import edu.ucsb.nceas.metacat.util.SessionData;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;
import edu.ucsb.nceas.utilities.access.AccessControlInterface;

public class PermissionController {
    private String guid = null;

    private boolean hasSubTreeAccessControl = false; // flag if has a subtree

    private static final long TOPLEVELSTARTNODEID = 0; //if start node is 0, means it is top
    //level document

    private static Log logMetacat = LogFactory.getLog(PermissionController.class);

    /**
     * Constructor for PermissionController
     *
     * @param myDocid the docid need to access
     */
    public PermissionController(String myDocid) throws McdbException {

        // find the guid if we can
        String docId = null;
        int rev = -1;

        // get the parts
        docId = DocumentUtil.getSmartDocId(myDocid);
        rev = DocumentUtil.getRevisionFromAccessionNumber(myDocid);

        // this is what we really want
        guid = IdentifierManager.getInstance().getGUID(docId, rev);

    }

    /**
     * Return if a document has subtree access control
     */
    public boolean hasSubTreeAccessControl() {
        return hasSubTreeAccessControl;
    }

    public boolean hasPermission(String sessionId, String myPermission) throws SQLException {
        SessionData sessionData = null;
        sessionData = SessionService.getInstance().getRegisteredSession(sessionId);
        if (sessionData == null) {
            return false;
        }

        return hasPermission(sessionData.getUserName(), sessionData.getGroupNames(), myPermission);
    }


    /**
     * Check from db connection if at least one of the list of @principals
     * Administrators are allowed all permission
     * @param user  the user name
     * @param groups  the groups which the use is in
     * @param myPermission  permission type to check for
     */
    public boolean hasPermission(String user, String[] groups, String myPermission)
        throws SQLException //, Exception
    {
        boolean hasPermission = false;
        String[] userPackage = null;
        int permission = AccessControlList.intValue(myPermission);

        //for the command line invocation and replication
        if ((user == null) && (groups == null || groups.length == 0)) {
            return true;
        }

        // for administrators
        //see http://bugzilla.ecoinformatics.org/show_bug.cgi?id=4728
        try {
            if (AuthUtil.isAdministrator(user, groups)) {
                return true;
            }
        } catch (MetacatUtilException e) {
            // not much we can do here, except treat them as normal
            logMetacat.warn("Error checking for administrator: " + e.getMessage(), e);
        }

        // for DataONE rightsHolder permission
        boolean isOwner = false;
        try {
            Session userSession = new Session();
            Subject subject = new Subject();
            subject.setValue(user);
            userSession.setSubject(subject);
            Identifier pid = new Identifier();
            pid.setValue(guid);
            SystemMetadata sysMeta = SystemMetadataManager.getInstance().get(pid);
            isOwner = (sysMeta.getRightsHolder().equals(subject));
            if (!isOwner) {
                isOwner = D1AuthHelper.expandRightsHolder(sysMeta.getRightsHolder(), subject);
            }
        } catch (Exception e) {
            logMetacat.warn("Error checking for DataONE permissions: " + e.getMessage(), e);
            isOwner = false;
        }
        if (isOwner) {
            return true;
        }

        logMetacat.debug(
            "Checking permission on " + this.guid + " for user: " + user + " and groups: "
                + Arrays.toString(groups));

        //create a userpackage including user, public and group member
        userPackage = createUsersPackage(user, groups);

        //if the requested document is access documents and requested permission
        //is "write", the user should have "all" right

        if (isAccessDocument() && (permission == AccessControlInterface.WRITE)) {

            hasPermission = hasPermission(userPackage, 7);// 7 is all permission
        }//if
        else //in other situation, just check the request permission
        {
            // Check for @permission on @docid for @user and/or @groups
            hasPermission = hasPermission(userPackage, permission);
        }//else

        return hasPermission;
    }


    /**
     * Check from db connection if the users in String array @principals has
     * @permission on @docid*
     * @param principals, names in userPakcage need to check for @permission
     * @param docid, document identifier to check on
     * @param permission, permission (write or all...) to check for
     */
    private boolean hasPermission(String[] principals, int permission) throws SQLException {
        long startId = TOPLEVELSTARTNODEID;// this is for top level, so startid is 0
        try {
            //first, if there is a docid owner in user package, return true
            //because doc owner has all permssion
            if (containDocumentOwner(principals)) {

                return true;
            }

            //If there is no owner in user package, checking the table
            //check perm_order
            if (isAllowFirst(principals, startId)) {

                if (hasExplicitDenyRule(principals, permission, startId)) {
                    //if it is allowfirst and has deny rule(either explicit )
                    //deny access

                    return false;
                }//if
                else if (hasAllowRule(principals, permission, startId)) {
                    //if it is allowfirst and hasn't deny rule and has allow rule
                    //allow access

                    return true;
                }//else if
                else {
                    //other situation deny access

                    return false;
                }//else
            }//if isAllowFirst
            else //denyFirst
            {
                if (hasAllowRule(principals, permission, startId)) {
                    //if it is denyFirst and has allow rule, allow access
                    return true;
                } else {
                    //if it is denyfirst but no allow rule, deny access
                    return false;
                }
            }//else denyfirst
        }//try
        catch (Exception e) {
            logMetacat.warn(
                "PermissionController.hasPermission - There is a exception in hasPermission "
                    + "method: "
                    + e.getMessage());
        }

        return false;
    }//hasPermission

    /**
     *  Method to check if a person has permission to a inline data file
     * @param user String
     * @param groups String[]
     * @param myPermission String
     * @param inlineDataId String
     * @throws McdbException
     * @return boolean
     */
    private boolean hasPermissionForInlineData(
        String user, String[] groups, String myPermission, String inlineDataId)
        throws McdbException {
        // this method can call the public method - hasPermission(...)
        // the only difference is about the ownership, you couldn't find the owner
        // from inlinedataId directly. You should get it from eml document itself
        String[] userPackage = createUsersPackage(user, groups);
        try {
            if (containDocumentOwner(userPackage)) {
                return true;
            } else {
                // is a funky inline data id with the extra part, so we set it manually
                PermissionController controller = new PermissionController(guid);
                controller.guid = inlineDataId;
                return controller.hasPermission(user, groups, myPermission);
            }
        } catch (SQLException e) {
            throw new McdbException(e.getMessage());
        }
    }


    /**
     * Check if a document id is a access document. Access document need user
     * has "all" permission to access it.
     *
     * @param docId,
     *            the document id need to be checked
     */
    private boolean isAccessDocument() throws SQLException {
        // get the docid from the guid
        String docId = null;
        try {
            docId = IdentifierManager.getInstance().getLocalId(guid);
        } catch (McdbDocNotFoundException e) {
            return false;
            //throw new SQLException(e);
        }

        docId = DocumentUtil.getDocIdFromString(docId);
        PreparedStatement pStmt = null;
        DBConnection conn = null;
        int serialNumber = -1;
        try {
            // check out DBConnection
            conn = DBConnectionPool.getDBConnection("PermissionControl.isAccessDoc");
            serialNumber = conn.getCheckOutSerialNumber();
            pStmt = conn.prepareStatement("select doctype from xml_documents where docid like ? ");
            pStmt.setString(1, docId);
            pStmt.execute();
            ResultSet rs = pStmt.getResultSet();
            boolean hasRow = rs.next();
            String doctype = null;
            if (hasRow) {
                doctype = rs.getString(1);

            }
            pStmt.close();

            // if it is an access document
            if (doctype != null && ((MetacatUtil
                .getOptionList(PropertyService.getProperty("xml.accessdoctype"))
                .contains(doctype)))) {

                return true;
            }

        } catch (SQLException e) {
            throw new SQLException(
                "PermissionControl.isAccessDocument " + "Error checking" + " on document " + docId
                    + ". " + e.getMessage());
        } catch (PropertyNotFoundException pnfe) {
            throw new SQLException(
                "PermissionControl.isAccessDocument " + "Error checking" + " on document " + docId
                    + ". " + pnfe.getMessage());
        } finally {
            try {
                pStmt.close();
            } finally {
                DBConnectionPool.returnDBConnection(conn, serialNumber);
            }
        }

        return false;
    }// isAccessDocument


    /**
     * Check if a stirng array contains a given documents' owner
     *
     * @param principals,
     *            a string array storing the username, groups name and public.
     * @param docid,
     *            the id of given documents
     */
    private boolean containDocumentOwner(String[] principals) throws SQLException {

        // get the docid
        String docId = null;
        try {
            docId = IdentifierManager.getInstance().getLocalId(guid);
            docId = DocumentUtil.getDocIdFromString(docId);
        } catch (McdbDocNotFoundException e) {
            // should be true if we own the parent doc, but likely won't be checked in that case
            return false;
        }

        int lengthOfArray = principals.length;
        boolean hasRow;
        PreparedStatement pStmt = null;
        DBConnection conn = null;
        int serialNumber = -1;

        try {
            //check out DBConnection
            conn = DBConnectionPool.getDBConnection("PermissionControl.containDocOnwer");
            serialNumber = conn.getCheckOutSerialNumber();
            pStmt = conn.prepareStatement(
                "SELECT 'x' FROM xml_documents " + "WHERE docid = ? AND lower(user_owner) = ? "
                    + "UNION ALL " + "SELECT 'x' FROM xml_revisions "
                    + "WHERE docid = ? AND lower(user_owner) = ? ");
            //check every element in the string array too see if it conatains
            //the owner of document
            for (int i = 0; i < lengthOfArray; i++) {

                // Bind the values to the query
                pStmt.setString(1, docId);
                pStmt.setString(2, principals[i]);
                pStmt.setString(3, docId);
                pStmt.setString(4, principals[i]);
                logMetacat.info(
                    "PermissionController.containDocumentOwner - the principle stack is : "
                        + principals[i]);

                pStmt.execute();
                ResultSet rs = pStmt.getResultSet();
                hasRow = rs.next();
                if (hasRow) {
                    pStmt.close();
                    logMetacat.info("PermissionController.containDocumentOwner - find the owner");
                    return true;
                }//if

            }//for
        }//try
        catch (SQLException e) {
            pStmt.close();

            throw new SQLException(
                "PermissionControl.hasPermission - " + "Error checking ownership for "
                    + principals[0] + " on document #" + docId + ". " + e.getMessage());
        }//catch
        finally {
            try {
                pStmt.close();
            } finally {
                DBConnectionPool.returnDBConnection(conn, serialNumber);
            }
        }
        return false;
    }//containDocumentOwner

    /**
     * Check if the permission order for user at that documents is allowFirst
     * @param principals, list of names of principals to check for
     * @param docid, document identifier to check for
     */
    private boolean isAllowFirst(String[] principals, long startId) throws SQLException, Exception {
        int lengthOfArray = principals.length;
        boolean hasRow;
        PreparedStatement pStmt = null;
        DBConnection conn = null;
        int serialNumber = -1;
        String sql = null;
        boolean topLever = false;
        if (startId == TOPLEVELSTARTNODEID) {
            //top level
            topLever = true;
            sql = "SELECT perm_order FROM xml_access " + "WHERE lower(principal_name) = ? "
                + "AND guid = ? " + "AND startnodeid is NULL";
        } else {
            //sub tree level
            sql = "SELECT perm_order FROM xml_access " + "WHERE lower(principal_name) = ? "
                + "AND guid = ? " + "AND startnodeid = ?";
        }

        try {
            //check out DBConnection
            conn = DBConnectionPool.getDBConnection("AccessControlList.isAllowFirst");
            serialNumber = conn.getCheckOutSerialNumber();

            //select permission order from database
            pStmt = conn.prepareStatement(sql);

            //check every name in the array
            for (int i = 0; i < lengthOfArray; i++) {
                //bind value
                pStmt.setString(1, principals[i]);//user name
                pStmt.setString(2, guid);//guid

                // if subtree, we need set subtree id
                if (!topLever) {
                    pStmt.setLong(3, startId);
                }

                pStmt.execute();
                ResultSet rs = pStmt.getResultSet();
                hasRow = rs.next();
                if (hasRow) {
                    //get the permission order from data base
                    String permissionOrder = rs.getString(1);
                    //if the permission order is "allowFirst
                    if (permissionOrder.equalsIgnoreCase(AccessControlInterface.ALLOWFIRST)) {
                        pStmt.close();
                        return true;
                    } else {
                        pStmt.close();
                        return false;
                    }
                }//if
            }//for
        }//try
        catch (SQLException e) {
            throw e;
        } finally {
            try {
                pStmt.close();
            } finally {
                DBConnectionPool.returnDBConnection(conn, serialNumber);
            }
        }

        //if reach here, means there is no permssion record for given names and
        //docid. So throw a exception.

        throw new Exception(
            "PermissionController.isAllowFirst - There is no permission record for user "
                + principals[0] + " at document " + guid);

    }//isAllowFirst

    /**
     * Check if the users array has allow rules for given users, docid and
     * permission.
     * If it has permission rule and ticket count is greater than 0, the ticket
     * number will decrease one for every allow rule
     * @param principals, list of names of principals to check for
     * @param docid, document identifier to check for
     * @param permission, the permssion need to check
     */
    private boolean hasAllowRule(String[] principals, int permission, long startId)
        throws SQLException, Exception {
        int lengthOfArray = principals.length;
        boolean allow = false;//initial value is no allow rule
        ResultSet rs;
        PreparedStatement pStmt = null;
        int permissionValue = permission;
        int permissionValueInTable;
        DBConnection conn = null;
        int serialNumber = -1;
        boolean topLever = false;
        String sql = null;
        if (startId == TOPLEVELSTARTNODEID) {
            // for toplevel
            topLever = true;
            sql = "SELECT permission " + "FROM xml_access " + "WHERE guid = ? "
                + "AND lower(principal_name) = ? " + "AND perm_type = ? "
                + "AND startnodeid is NULL";
        } else {
            topLever = false;
            sql = "SELECT permission " + "FROM xml_access " + "WHERE guid = ? "
                + "AND lower(principal_name) = ? " + "AND perm_type = ? " + "AND startnodeid = ?";
        }
        try {
            //check out DBConnection
            conn = DBConnectionPool.getDBConnection("AccessControlList.hasAllowRule");
            serialNumber = conn.getCheckOutSerialNumber();
            //This sql statement will select entry with
            //begin_time<=currentTime<=end_time in xml_access table
            //If begin_time or end_time is null in table, isnull(begin_time, sysdate)
            //function will assign begin_time=sysdate
            pStmt = conn.prepareStatement(sql);
            //bind docid, perm_type
            pStmt.setString(1, guid);
            pStmt.setString(3, AccessControlInterface.ALLOW);

            // if subtree lever, need to set subTreeId
            if (!topLever) {
                pStmt.setLong(4, startId);
            }

            //bind every elenment in user name array
            for (int i = 0; i < lengthOfArray; i++) {
                logMetacat.debug("Checking permission for principal: " + principals[i]);
                logMetacat.debug("SQL: " + pStmt.toString());

                pStmt.setString(2, principals[i]);
                pStmt.execute();
                rs = pStmt.getResultSet();
                while (rs.next())//check every entry for one user
                {
                    permissionValueInTable = rs.getInt(1);

                    //permission is ok
                    //the user have a permission to access the file
                    if ((permissionValueInTable & permissionValue) == permissionValue) {

                        allow = true;//has allow rule entry
                    }//if
                }//while
            }//for
        }//try
        catch (SQLException sqlE) {
            throw sqlE;
        } catch (Exception e) {
            throw e;
        } finally {
            try {
                pStmt.close();
            } finally {
                DBConnectionPool.returnDBConnection(conn, serialNumber);
            }
        }
        return allow;
    }//hasAllowRule


    /**
     * Check if the users array has explicit deny rules for given users, docid
     * and permission. That means the perm_type is deny and current time is
     * less than end_time and greater than begin time, or no time limit.
     * @param principals, list of names of principals to check for
     * @param docid, document identifier to check for
     * @param permission, the permission need to check
     */
    private boolean hasExplicitDenyRule(String[] principals, int permission, long startId)
        throws SQLException {
        int lengthOfArray = principals.length;
        ResultSet rs;
        PreparedStatement pStmt = null;
        int permissionValue = permission;
        int permissionValueInTable;
        DBConnection conn = null;
        int serialNumber = -1;
        String sql = null;
        boolean topLevel = false;

        // decide top level or subtree level
        if (startId == TOPLEVELSTARTNODEID) {
            topLevel = true;
            sql = "SELECT permission " + "FROM xml_access " + "WHERE guid = ? "
                + "AND lower(principal_name) = ? " + "AND perm_type = ? "
                + "AND startnodeid is NULL";
        } else {
            topLevel = false;
            sql = "SELECT permission " + "FROM xml_access " + "WHERE guid = ? "
                + "AND lower(principal_name) = ? " + "AND perm_type = ? " + "AND startnodeid = ?";
        }

        try {
            //check out DBConnection
            conn = DBConnectionPool.getDBConnection("PermissionControl.hasExplicitDeny");
            serialNumber = conn.getCheckOutSerialNumber();

            pStmt = conn.prepareStatement(sql);
            //bind docid, perm_type
            pStmt.setString(1, guid);
            pStmt.setString(3, AccessControlInterface.DENY);

            // subtree level need to set up subtreeid
            if (!topLevel) {
                pStmt.setLong(4, startId);
            }

            //bind every elenment in user name array
            for (int i = 0; i < lengthOfArray; i++) {
                pStmt.setString(2, principals[i]);
                pStmt.execute();
                rs = pStmt.getResultSet();
                while (rs.next())//check every entry for one user
                {
                    permissionValueInTable = rs.getInt(1);

                    //permission is ok the user doesn't have permission to access the file
                    if ((permissionValueInTable & permissionValue) == permissionValue) {
                        pStmt.close();
                        return true;
                    }//if
                }//while
            }//for
        }//try
        catch (SQLException e) {
            throw e;
        }//catch
        finally {
            try {
                pStmt.close();
            } finally {
                DBConnectionPool.returnDBConnection(conn, serialNumber);
            }
        }//finally
        return false;//no deny rule
    }//hasExplicitDenyRule


    /**
     * Creat a users pakages to check permssion rule, user itself, public and
     * the gourps the user belong will be include in this package
     * @param user, the name of user
     * @param groups, the string array of the groups that user belong to
     */
    private String[] createUsersPackage(String user, String[] groups) {
        String[] usersPackage = null;
        int lengthOfPackage;

        if (groups != null) {
            //if gouprs is not null and user is not public, we should create a array
            //to store the groups and user and public.
            //So the length of userPackage is the length of group plus two
            if (!user.equalsIgnoreCase(AccessControlInterface.PUBLIC)) {
                lengthOfPackage = (groups.length) + 2;
                usersPackage = new String[lengthOfPackage];
                //the first two elements is user self and public
                //in order to ignore case sensitive, we transfer user to lower case
                if (user != null) {
                    usersPackage[0] = user.toLowerCase();
                    logMetacat.info(
                        "PermissionController.createUsersPackage - after transfer to lower case"
                            + "(not null): "
                            + usersPackage[0]);
                } else {
                    usersPackage[0] = user;
                    usersPackage[0] = user.toLowerCase();
                    logMetacat.info(
                        "PermissionController.createUsersPackage - after transfer to lower case"
                            + "(null): "
                            + usersPackage[0]);
                }
                usersPackage[1] = AccessControlInterface.PUBLIC;
                //put groups element from index 0 to lengthOfPackage-3 into userPackage
                //from index 2 to lengthOfPackage-1
                for (int i = 2; i < lengthOfPackage; i++) {
                    //tansfer group to lower case too
                    if (groups[i - 2] != null) {
                        usersPackage[i] = groups[i - 2].toLowerCase();
                    }
                } //for
            }//if user!=public
            else//use=public
            {
                lengthOfPackage = (groups.length) + 1;
                usersPackage = new String[lengthOfPackage];
                //the first lements is public
                usersPackage[0] = AccessControlInterface.PUBLIC;
                //put groups element from index 0 to lengthOfPackage-2 into userPackage
                //from index 1 to lengthOfPackage-1
                for (int i = 1; i < lengthOfPackage; i++) {
                    if (groups[i - 1] != null) {
                        usersPackage[i] = groups[i - 1].toLowerCase();
                    }
                } //for
            }//else user=public

        }//if groups!=null
        else {
            //because no groups, the userPackage only need two elements
            //one is for user, the other is for public
            if (!user.equalsIgnoreCase(AccessControlInterface.PUBLIC)) {
                lengthOfPackage = 2;
                usersPackage = new String[lengthOfPackage];
                if (user != null) {
                    usersPackage[0] = user.toLowerCase();
                } else {
                    usersPackage[0] = user;
                }
                usersPackage[1] = AccessControlInterface.PUBLIC;
            }//if user!=public
            else //user==public
            {
                //only put public into array
                lengthOfPackage = 1;
                usersPackage = new String[lengthOfPackage];
                usersPackage[0] = AccessControlInterface.PUBLIC;
            }
        }//else groups==null
        return usersPackage;
    }//createUsersPackage


    /**
     * A static method to get Hashtable which cointains a inlinedata object list that
     * user can't read it. The key is subtree id of inlinedata, the data is
     * internal file name for the inline data which is stored as docid
     * in xml_access table or data object doc id.
     * @param docid (With Rev), metadata docid which should be the accessfileid
     *                         in the table
     * @param user , the name of user
     * @param groups, the group which the user belong to
     */
    public static Hashtable<String, String> getUnReadableInlineDataIdList(
        String docid, String user, String[] groups) throws McdbException {
        Hashtable<String, String> inlineDataList =
            getUnAccessableInlineDataIdList(docid, user, groups, AccessControlInterface.READSTRING);

        return inlineDataList;
    }

    /**
     * A static method to get Hashtable which cointains a inline  data object list that
     * user can't overwrite it. The key is subtree id of inline data distrubition,
     * the value is internal file name for the inline data which is stored as docid
     * in xml_access table or data object doc id.
     * @param docidWithoutRev, metadata docid which should be the accessfileid
     *                         in the table
     * @param user , the name of user
     * @param groups, the group which the user belong to
     */
    public static Hashtable<String, String> getUnWritableInlineDataIdList(
        String docidWithoutRev, String user, String[] groups, boolean withRevision)
        throws Exception {
        Hashtable<String, String> inlineDataList =
            getUnAccessableInlineDataIdList(docidWithoutRev, user, groups,
                                            AccessControlInterface.WRITESTRING);

        return inlineDataList;
    }


    /**
     * This method will get hashtable which contains a unaccessable distribution
     * inlinedata object list
     *
     */
    private static Hashtable<String, String> getUnAccessableInlineDataIdList(
        String docid, String user, String[] groups, String permission) throws McdbException {
        Hashtable<String, String> unAccessibleIdList = new Hashtable();
        if (user == null) {
            return unAccessibleIdList;
        }

        Hashtable allIdList;
        try {
            allIdList = getAllInlineDataIdList(docid);
        } catch (SQLException e) {
            throw new McdbException(e.getMessage());
        }
        Enumeration<String> en = allIdList.keys();
        while (en.hasMoreElements()) {
            String subTreeId = (String) en.nextElement();
            String fileId = (String) allIdList.get(subTreeId);
            //Here fileid is internal file id for line data. It stored in guid
            // field in xml_access table.
            PermissionController controller = new PermissionController(docid);
            if (!controller.hasPermissionForInlineData(user, groups, permission, fileId)) {

                logMetacat.info(
                    "PermissionController.getUnAccessableInlineDataIdList - Put subtree id "
                        + subTreeId + " and " + "inline data file name " + fileId + " into " + "un"
                        + permission + " hash");
                unAccessibleIdList.put(subTreeId, fileId);

            }
        }
        return unAccessibleIdList;
    }


    /*
     * This method will get a hash table from xml_access table for all records
     * about the inline data. The key is subtree id and data is a inline internal
     * file name
     */
    private static Hashtable getAllInlineDataIdList(String docid) throws SQLException {
        Hashtable inlineDataList = new Hashtable();
        String sql = "SELECT subtreeid, guid " + "FROM xml_access " + "WHERE accessfileid = ? "
            + "AND subtreeid IS NOT NULL";
        PreparedStatement pStmt = null;
        ResultSet rs = null;
        DBConnection conn = null;
        int serialNumber = -1;
        try {
            //check out DBConnection
            conn = DBConnectionPool.getDBConnection("PermissionControl.getDataSetId");
            serialNumber = conn.getCheckOutSerialNumber();
            pStmt = conn.prepareStatement(sql);
            //bind the value to query
            pStmt.setString(1, docid);
            //execute the query
            pStmt.execute();
            rs = pStmt.getResultSet();
            //process the result
            while (rs.next()) {
                String subTreeId = rs.getString(1);
                String inlineDataId = rs.getString(2);
                if (subTreeId != null && !subTreeId.trim().equals("") && inlineDataId != null
                    && !inlineDataId.trim().equals("")) {
                    inlineDataList.put(subTreeId, inlineDataId);
                }
            }//while
        }//try
        finally {
            try {
                pStmt.close();
            } finally {
                DBConnectionPool.returnDBConnection(conn, serialNumber);
            }
        }//finally
        return inlineDataList;
    }//getAllInlineDataIdList
}
