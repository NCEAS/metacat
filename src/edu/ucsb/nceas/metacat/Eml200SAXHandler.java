/**
 *  '$RCSfile$'
 *    Purpose: A Class that handles the SAX XML events as they
 *             are generated from XML documents
 *  Copyright: 2000 Regents of the University of California and the
 *             National Center for Ecological Analysis and Synthesis
 *    Authors: Jing Tao
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.EmptyStackException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Stack;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import edu.ucsb.nceas.utilities.access.AccessControlInterface;
import edu.ucsb.nceas.metacat.accesscontrol.AccessRule;
import edu.ucsb.nceas.metacat.accesscontrol.AccessSection;
import edu.ucsb.nceas.metacat.database.DBConnection;
import edu.ucsb.nceas.metacat.database.DBConnectionPool;
import edu.ucsb.nceas.metacat.dataone.hazelcast.HazelcastService;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.metacat.util.AuthUtil;
import edu.ucsb.nceas.metacat.util.DocumentUtil;
import edu.ucsb.nceas.metacat.util.MetacatUtil;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;

/**
 * A database aware Class implementing callback methods for the SAX parser to
 * call when processing the XML stream and generating events
 * Here is the rules for user which has write permission in top level access
 * rules(he can update metadata but can't update access module) try to update
 * a document:
 * 1. Checking access part (both in top level and additional level, node by node)
 *    If something was modified, reject the document. Note: for additional part
 *    The access subtree startnode starts at "<describe>" rather than <access>.
 *    This is because make sure ids wouldn't be mess up by user.
 * 2. Checking ids in additional access part, if they points a distribution
 *    module with online or inline. If some ids don't, reject the documents.
 * 3. For inline distribution:
 *    If user can't read the inline data, the empty string in inline element
 *    will be send to user if he read this eml document(user has read access
 *    at top level - metadata, but user couldn't read inline data).
 *    Here is some interested scenario: If user does have read and write
 *    permission in top level access rules(for metadata)
 *    but 1) user doesn't have read and write permission in inline data block,
 *    so if user update the document with some inline data rather than
 *    empty string in same inline data block(same distribution inline id),
 *    this means user updated the inline data. So the document should be
 *    rejected.
 *    2) user doesn't have read permission, but has write permission in
 *    inline data block. If user send back inline data block with empty
 *    string, we will think user doesn't update inline data and we will
 *    copy old inline data back to the new one. If user send something
 *    else, we will overwrite the old inline data by new one.
 * 4. For online distribution:
 *    It is easy than inline distribution. When the user try to change a external
 *    document id, we will checking if the user have "all" permission to it.
 *    If don't have, reject the document. If have, delete the old rules and apply
 *    The new rules.
 *
 *
 * Here is some info about additional access rules ( data object rules):
 *  The data access rules format looks like:
 *  <additionalMetadata>
 *    <describes>100</describes>
 *    <describes>300</describes>
 *    <access>rulesone</access>
 *  </additionalMetadata>
 *  <additionalMetadata>
 *    <describes>200</describes>
 *    <access>rulesthree</access>
 *  </additionalMetadata>
 *  Because eml additionalMetadata is (describes+, any) and any ocurrence is 1.
 *  So following xml will be rejected by xerces.
 *  <additionalMetadata>
 *    <describes>100</describes>
 *    <describes>300</describes>
 *    <other>....</other>
 *    <access>rulesone</access>
 *  </additionalMetadata>
 */
public class Eml200SAXHandler extends DBSAXHandler implements
        AccessControlInterface
{
	// Boolean that tells whether we are processing top level (document level) access.
	// this is true when we hit an 'access' element, and the grandparent element is 
	// 'eml' and we are not inside an 'additionalMetadata' section.  Set back to false
	// in endElement
    private boolean processTopLevelAccess = false;

    // now additionalAccess will be explained as distribution access control
    // - data file
    private boolean processAdditionalAccess = false;

	// Boolean that tells whether we are processing other access. This is true when 
    // we find an 'access' element inside an 'additionalMetadata' element.  Set back
    // to false in endElement
    private boolean processOtherAccess = false;
    
    private Vector<String> guidsToSync;

    // if we are inside an 'access' element, we use this object to hold the 
    // current access info
    private AccessSection accessObject = null;

    // for each 'access/allow' and 'access/deny' we create a new access Rule to hold
    // the info
    private AccessRule accessRule = null;

    private Vector describesId = new Vector(); // store the ids in
                                               //additionalmetadata/describes

    //store all distribution element id for online url. key is the distribution
    // id and  data  is url
    private Hashtable onlineURLDistributionIdList = new Hashtable();
    // distribution/oneline/url will store this vector if distribution doesn't
    // have a id.
    private Vector onlineURLDistributionListWithoutId = new Vector();

    //store all distribution element id for online other distribution, such as
    // connection or connectiondefination. key is the distribution id
    // and  data  is distribution id
    private Hashtable onlineOtherDistributionIdList = new Hashtable();

    //store all distribution element id for inline data.
    // key is the distribution id, data is the internal inline id
    private Hashtable inlineDistributionIdList = new Hashtable();

    //store all distribution element id for off line data.
    // key is the distribution id, data is the id too.
    private Hashtable offlineDistributionIdList = new Hashtable();

    // a hash to stored all distribution id, both key and value are id itself
    private Hashtable distributionAllIdList = new Hashtable();

    // temporarily store distribution id
    private String distributionId = null;

    // flag to indicate to handle distribution
    private boolean proccessDistribution = false;

    // a hash table to stored the distribution which is a reference and this
    // distribution has a id too. The key is itself id of this distribution,
    // the value is the referenced id.
    // So, we only stored the format like this:
    // <distribution id ="100"><reference>300</reference></distribution>
    // the reason is:
    // if not id in distribution, then the distribution couldn't be added
    // to additional access module. The distribution block which was referenced
    // id (300) will be stored in above distribution lists.
    private Hashtable distributionReferenceList = new Hashtable();

    // if the action is update and the user does not have ALL permission 
    // and the user is not an administrator, then we will need to compare 
    // access modules to make sure the user has update permissions
    private boolean needToCheckAccessModule = false;

    private AccessSection topAccessSubTreeFromDB = null;

    private Vector additionalAccessSubTreeListFromDB = new Vector();

    private Hashtable referencedAccessSubTreeListFromDB = new Hashtable();

    // this holds the top level (document level) access section.
    private AccessSection topAccessSection;

    private Vector additionalAccessVector = new Vector();

    // key is subtree id and value is accessSection object
    private Hashtable possibleReferencedAccessHash = new Hashtable();

    // we need an another stack to store the access node which we pull out just
    // from xml. If a reference happend, we can use the stack the compare nodes
    private Stack storedAccessNodeStack = new Stack();

    // vector stored the data file id which will be write into relation table
    private Vector onlineDataFileIdInRelationVector = new Vector();

    // Indicator of inline data
    private boolean handleInlineData = false;

    private Hashtable inlineDataNameSpace = null;

    private Writer inlineDataFileWriter = null;

    private String inlineDataFileName = null;

    private int inLineDataIndex = 0;

    private Vector inlineFileIDList = new Vector();

    private boolean inAdditionalMetaData = false;

    //user has unwritable inline data object when it updates a document
    private boolean unWritableInlineDataObject = false;
    //user has unreadable inline data when it updates a dcoument
    private boolean unReadableInlineDataObject = false;

    // the hashtable contains the info from xml_access table which
    // inline data the user can't read when user update a document.
    // The key in hashtable is subtree id and data is the inline data internal
    // file name.

    private Hashtable previousUnreadableInlineDataObjectHash = new Hashtable();

    // the hashtable contains the info from xml_access table which
    // inline data the user can't write when user update a document.
    // The key in hashtable is subtree id and data is the inline data internal
    // file name.
    private Hashtable previousUnwritableInlineDataObjectHash = new Hashtable();

    private Hashtable accessSubTreeAlreadyWriteDBList = new Hashtable();

    //This hashtable will stored the id which already has additional access
    // control. So the top level access control will ignore them.
    private Hashtable onlineURLIdHasadditionalAccess   = new Hashtable();

    // additional access module will be in additionalMetadata part. Its format
    // look like
    //<additionalMetadata>
    //   <describes>100</describes>
    //   <describes>300</describes>
    //   <access>rulesone</access>
    //</additionalMetadata>
    // If user couldn't have all permission to update access rules, he/she could
    // not update access module, but also couldn't update "describes".
    // So the start node id for additional access section is the first describes
    // element
    private boolean firstDescribesInAdditionalMetadata = true;
    private long    firstDescribesNodeId               = -1;

    private int numberOfHitUnWritableInlineData = 0;

    // Constant
    private static final String EML = "eml";

    private static final String DESCRIBES = "describes";

    private static final String ADDITIONALMETADATA = "additionalMetadata";

    private static final String ORDER = "order";

    private static final String ID = "id";

    private static final String REFERENCES = "references";

    public static final String INLINE = "inline";

    private static final String ONLINE = "online";

    private static final String OFFLINE = "offline";

    private static final String CONNECTION = "connection";

    private static final String CONNECTIONDEFINITION = "connectionDefinition";

    private static final String URL = "url";

    private static final String PERMISSIONERROR = "User tried to update a subtree "
        + "when they don't have write permission!";

    private static final String UPDATEACCESSERROR = "User tried to update an "
        + "access module when they don't have \"ALL\" permission on the object ";

    public static final String TOPLEVEL = "top";

    public static final String DATAACCESSLEVEL = "dataAccess";

    // this level is for the access module which is not in top or additional
    // place, but it was referenced by top or additional
    private static final String REFERENCEDLEVEL = "referenced";

    private static final String RELATION = "Provides info for";

    private static final String DISTRIBUTION = "distribution";

    private Logger logMetacat = Logger.getLogger(Eml200SAXHandler.class);   	   	
    
    /**
     * Construct an instance of the handler class In this constructor, user can
     * specify the version need to upadate
     *
     * @param conn the JDBC connection to which information is written
     * @param action - "INSERT" or "UPDATE"
     * @param docid to be inserted or updated into JDBC connection
     * @param revision, the user specified the revision need to be update
     * @param user the user connected to MetaCat servlet and owns the document
     * @param groups the groups to which user belongs
     * @param pub flag for public "read" access on document
     * @param serverCode the serverid from xml_replication on which this
     *            document resides.
     *
     */
    public Eml200SAXHandler(DBConnection conn, String action, String docid,
            String revision, String user, String[] groups, String pub,
            int serverCode, Date createDate, Date updateDate, boolean writeAccessRules, Vector<String> guidsToSync) throws SAXException
    {
        super(conn, action, docid, revision, user, groups, pub, 
                serverCode, createDate, updateDate, writeAccessRules);
		this.guidsToSync = guidsToSync;
        // Get the unchangable subtrees (user doesn't have write permission)
        try
        {

            //Here is for  data object checking.
            if (action != null && action.equals("UPDATE")) {
            	
    			// we need to check if user can update access subtree			
    			int latestRevision = DBUtil.getLatestRevisionInDocumentTable(docid);
    			String previousDocid = 
    				docid + PropertyService.getProperty("document.accNumSeparator") + latestRevision;
    			
    			PermissionController control = new PermissionController(previousDocid);
            	
            	 // If the action is update and user doesn't have "ALL" or "CHMOD" permission
                // we need to check if user update access subtree
            	if ( !control.hasPermission(user, groups, AccessControlInterface.ALLSTRING)
            			&& !control.hasPermission(user, groups, AccessControlInterface.CHMODSTRING)
                        && !AuthUtil.isAdministrator(user, groups) && writeAccessRules) {
                		
                    needToCheckAccessModule = true;
                    topAccessSubTreeFromDB = getTopAccessSubTreeFromDB();
                    additionalAccessSubTreeListFromDB =
                                             getAdditionalAccessSubTreeListFromDB();
                    referencedAccessSubTreeListFromDB =
                                             getReferencedAccessSubTreeListFromDB();
            	}
            	
              //info about inline data object which user doesn't have read
              //permission the info come from xml_access table
              previousUnreadableInlineDataObjectHash = PermissionController.
                            getUnReadableInlineDataIdList(previousDocid, user, groups);

              //info about data object which user doesn't have write permission
              // the info come from xml_accesss table
              previousUnwritableInlineDataObjectHash = PermissionController.
                            getUnWritableInlineDataIdList(previousDocid, user,
                                                          groups, true);

            }


        }
        catch (Exception e)
        {
            logMetacat.error("error in Eml200SAXHandler is " + e.getMessage());
            throw new SAXException(e.getMessage());
        }
    }

    /*
     * Get the top level access subtree info from xml_accesssubtree table.
     * If no top access subtree found, null will be return.
     */
     private AccessSection getTopAccessSubTreeFromDB()
                                                       throws SAXException
     {
       AccessSection topAccess = null;
       PreparedStatement pstmt = null;
       ResultSet rs = null;
       String sql = "SELECT subtreeid, startnodeid, endnodeid "
                + "FROM xml_accesssubtree WHERE docid like ? "
                + "AND controllevel like ?";


       try
       {
            pstmt = connection.prepareStatement(sql);
            // Increase DBConnection usage count
            connection.increaseUsageCount(1);
            // Bind the values to the query
            pstmt.setString(1, docid);
            pstmt.setString(2, TOPLEVEL);
            logMetacat.trace("Eml200SAXHandler.getTopAccessSubTreeFromDB - executing SQL: " + pstmt.toString());
            pstmt.execute();

            // Get result set
            rs = pstmt.getResultSet();
            if (rs.next())
            {
                String sectionId = rs.getString(1);
                long startNodeId = rs.getLong(2);
                long endNodeId = rs.getLong(3);
                // create a new access section
                topAccess = new AccessSection();
                topAccess.setControlLevel(TOPLEVEL);
                topAccess.setDocId(docid);
                topAccess.setSubTreeId(sectionId);
                topAccess.setStartNodeId(startNodeId);
                topAccess.setEndNodeId(endNodeId);
            }
            pstmt.close();
        }//try
        catch (SQLException e)
        {
            throw new SAXException(
                    "EMLSAXHandler.getTopAccessSubTreeFromDB(): "
                            + e.getMessage());
        }//catch
        finally
        {
            try
            {
                pstmt.close();
            }
            catch (SQLException ee)
            {
                throw new SAXException(
                        "EMLSAXHandler.getTopAccessSubTreeFromDB(): "
                                + ee.getMessage());
            }
        }//finally
        return topAccess;

     }

    /*
     * Get the subtree node info from xml_accesssubtree table
     */
    private Vector getAdditionalAccessSubTreeListFromDB() throws Exception
    {
        Vector result = new Vector();
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        String sql = "SELECT subtreeid, startnodeid, endnodeid "
                + "FROM xml_accesssubtree WHERE docid like ? "
                + "AND controllevel like ? "
                + "ORDER BY startnodeid ASC";

        try
        {

            pstmt = connection.prepareStatement(sql);
            // Increase DBConnection usage count
            connection.increaseUsageCount(1);
            // Bind the values to the query
            pstmt.setString(1, docid);
            pstmt.setString(2, DATAACCESSLEVEL);
            pstmt.execute();

            // Get result set
            rs = pstmt.getResultSet();
            while (rs.next())
            {
                String sectionId = rs.getString(1);
                long startNodeId = rs.getLong(2);
                long endNodeId = rs.getLong(3);
                // create a new access section
                AccessSection accessObj = new AccessSection();
                accessObj.setControlLevel(DATAACCESSLEVEL);
                accessObj.setDocId(docid);
                accessObj.setSubTreeId(sectionId);
                accessObj.setStartNodeId(startNodeId);
                accessObj.setEndNodeId(endNodeId);
                // add this access obj into vector
                result.add(accessObj);

            }
            pstmt.close();
        }//try
        catch (SQLException e)
        {
            throw new SAXException(
                    "EMLSAXHandler.getadditionalAccessSubTreeListFromDB(): "
                            + e.getMessage());
        }//catch
        finally
        {
            try
            {
                pstmt.close();
            }
            catch (SQLException ee)
            {
                throw new SAXException(
                        "EMLSAXHandler.getAccessSubTreeListFromDB(): "
                                + ee.getMessage());
            }
        }//finally
        return result;
    }

   /*
    * Get the access subtree for referenced info from xml_accesssubtree table
    */
   private Hashtable getReferencedAccessSubTreeListFromDB() throws Exception
   {
       Hashtable result = new Hashtable();
       PreparedStatement pstmt = null;
       ResultSet rs = null;
       String sql = "SELECT subtreeid, startnodeid, endnodeid "
               + "FROM xml_accesssubtree WHERE docid like ? "
               + "AND controllevel like ? "
               + "ORDER BY startnodeid ASC";

       try
       {

           pstmt = connection.prepareStatement(sql);
           // Increase DBConnection usage count
           connection.increaseUsageCount(1);
           // Bind the values to the query
           pstmt.setString(1, docid);
           pstmt.setString(2, REFERENCEDLEVEL);
           pstmt.execute();

           // Get result set
           rs = pstmt.getResultSet();
           while (rs.next())
           {
               String sectionId = rs.getString(1);
               long startNodeId = rs.getLong(2);
               long endNodeId = rs.getLong(3);
               // create a new access section
               AccessSection accessObj = new AccessSection();
               accessObj.setControlLevel(DATAACCESSLEVEL);
               accessObj.setDocId(docid);
               accessObj.setSubTreeId(sectionId);
               accessObj.setStartNodeId(startNodeId);
               accessObj.setEndNodeId(endNodeId);
               // add this access obj into hastable
               if ( sectionId != null && !sectionId.trim().equals(""))
               {
                 result.put(sectionId, accessObj);
               }

           }
           pstmt.close();
       }//try
       catch (SQLException e)
       {
           throw new SAXException(
                   "EMLSAXHandler.getReferencedAccessSubTreeListFromDB(): "
                           + e.getMessage());
       }//catch
       finally
       {
           try
           {
               pstmt.close();
           }
           catch (SQLException ee)
           {
               throw new SAXException(
                       "EMLSAXHandler.getReferencedSubTreeListFromDB(): "
                               + ee.getMessage());
           }
       }//finally
       return result;
   }



    /** SAX Handler that is called at the start of each XML element */
    public void startElement(String uri, String localName, String qName,
            Attributes atts) throws SAXException
    {
        // for element <eml:eml...> qname is "eml:eml", local name is "eml"
        // for element <acl....> both qname and local name is "eml"
        // uri is namesapce
        logMetacat.trace("Start ELEMENT(qName) " + qName);
        logMetacat.trace("Start ELEMENT(localName) " + localName);
        logMetacat.trace("Start ELEMENT(uri) " + uri);

        DBSAXNode parentNode = null;
        DBSAXNode currentNode = null;
        // none inline part
        //+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
        //+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
        if (!handleInlineData)
        {
            // Get a reference to the parent node for the id
            try
            {
                parentNode = (DBSAXNode) nodeStack.peek();
            }
            catch (EmptyStackException e)
            {
                parentNode = null;
            }

            //start handle inline data
            //=====================================================
            if (qName.equals(INLINE) && !inAdditionalMetaData)
            {
                handleInlineData = true;
                inLineDataIndex++;
                //intitialize namespace hash for in line data
                inlineDataNameSpace = new Hashtable();
                //initialize file writer
                String docidWithoutRev = DocumentUtil.getDocIdFromString(docid);
                String seperator = ".";
                try {
					seperator = PropertyService.getProperty("document.accNumSeparator");
				} catch (PropertyNotFoundException pnfe) {
					logMetacat.error("Could not get property 'accNumSeparator'.  " 
							+ "Setting separator to '.': "+ pnfe.getMessage());
				}
                // the new file name will look like docid.rev.2
                inlineDataFileName = docidWithoutRev + seperator + revision
                        + seperator + inLineDataIndex;
                inlineDataFileWriter = createInlineDataFileWriter(inlineDataFileName, encoding);
                // put the inline file id into a vector. If upload failed,
                // metacat will
                // delete the inline data file
                inlineFileIDList.add(inlineDataFileName);

                // put distribution id and inline file id into a  hash
                if (distributionId != null)
                {
                  //check to see if this inline data is readable or writable to
                  // this user
                  if (!previousUnreadableInlineDataObjectHash.isEmpty() &&
                       previousUnreadableInlineDataObjectHash.containsKey(distributionId))
                  {
                      unReadableInlineDataObject = true;
                  }
                  if (!previousUnwritableInlineDataObjectHash.isEmpty() &&
                       previousUnwritableInlineDataObjectHash.containsKey(distributionId))
                  {
                     unWritableInlineDataObject = true;
                     numberOfHitUnWritableInlineData++;
                  }


                  // store the distributid and inlinedata filename into a hash
                  inlineDistributionIdList.put(distributionId, inlineDataFileName);
                }

            }
            //==============================================================


            // If hit a text node, we need write this text for current's parent
            // node
            // This will happend if the element is mixted
            //==============================================================
            if (hitTextNode && parentNode != null)
            {


                if (needToCheckAccessModule
                        && (processAdditionalAccess || processOtherAccess || processTopLevelAccess)) {
                    // stored the pull out nodes into storedNode stack
                    NodeRecord nodeElement = new NodeRecord(-2, -2, -2, "TEXT",
                            null, null, MetacatUtil.normalize(textBuffer
                                    .toString()));
                    storedAccessNodeStack.push(nodeElement);

                }

                // write the textbuffer into db for parent node.
                endNodeId = writeTextForDBSAXNode(endNodeId, textBuffer,
                        parentNode);
                // rest hitTextNode
                hitTextNode = false;
                // reset textbuffer
                textBuffer = null;
                textBuffer = new StringBuffer();

            }
            //==================================================================

            // Document representation that points to the root document node
            //==================================================================
            if (atFirstElement)
            {
                atFirstElement = false;
                // If no DOCTYPE declaration: docname = root element
                // doctype = root element name or name space
                if (docname == null) {
                    docname = localName;
                    // if uri isn't null doctype = uri(namespace)
                    // othewise root element
                    if (uri != null && !(uri.trim()).equals("")) {
                        doctype = uri;
                    } else {
                        doctype = docname;
                    }
                    logMetacat.info("DOCNAME-a: " + docname);
                    logMetacat.info("DOCTYPE-a: " + doctype);
                } else if (doctype == null) {
                    // because docname is not null and it is declared in dtd
                    // so could not be in schema, no namespace
                    doctype = docname;
                    logMetacat.info("DOCTYPE-b: " + doctype);
                }
                rootNode.writeNodename(docname);
                //System.out.println("here!!!!!!!!!!!!!!!!!!1");
                try {
                    // for validated XML Documents store a reference to XML DB
                    // Catalog
                    // Because this is select statement and it needn't to roll
                    // back if
                    // insert document action fialed.
                    // In order to decrease DBConnection usage count, we get a
                    // new
                    // dbconnection from pool
                    //String catalogid = null;
                    DBConnection dbConn = null;
                    int serialNumber = -1;

                    try {
                        // Get dbconnection
                        dbConn = DBConnectionPool
                                .getDBConnection("DBSAXHandler.startElement");
                        serialNumber = dbConn.getCheckOutSerialNumber();

                        String sql = "SELECT catalog_id FROM xml_catalog "
                            + "WHERE entry_type = 'Schema' "
                            + "AND public_id = ?";
                        PreparedStatement pstmt = dbConn.prepareStatement(sql);
                        pstmt.setString(1, doctype);
                        ResultSet rs = pstmt.executeQuery();
                        boolean hasRow = rs.next();
                        if (hasRow) {
                            catalogid = rs.getString(1);
                        }
                        pstmt.close();
                        //System.out.println("here!!!!!!!!!!!!!!!!!!2");
                    }//try
                    finally {
                        // Return dbconnection
                        DBConnectionPool.returnDBConnection(dbConn,
                                serialNumber);
                    }//finally

                    //create documentImpl object by the constructor which can
                    // specify
                    //the revision
                    if (!super.getIsRevisionDoc())
                    {
                       logMetacat.debug("EML200SaxHandler.startElement - creating new DocumentImple for " + docid);
                       currentDocument = new DocumentImpl(connection, rootNode
                            .getNodeID(), docname, doctype, docid, revision,
                            action, user, this.pub, catalogid, this.serverCode, 
                            createDate, updateDate);
                    }
                   

                } catch (Exception ane) {
                    throw (new SAXException("EML200SaxHandler.startElement - error with action " + 
                    		action + " : " + ane.getMessage()));
                }
                
            }
            //==================================================================

            // node
            //==================================================================
            // Create the current node representation
            currentNode = new DBSAXNode(connection, qName, localName,
                    parentNode, rootNode.getNodeID(), docid,
                    doctype);
            // Use a local variable to store the element node id
            // If this element is a start point of subtree(section), it will be
            // stored
            // otherwise, it will be discated
            long startNodeId = currentNode.getNodeID();
            // Add all of the namespaces
            String prefix = null;
            String nsuri = null;
            Enumeration prefixes = namespaces.keys();
            while (prefixes.hasMoreElements())
            {
                prefix = (String) prefixes.nextElement();
                nsuri = (String) namespaces.get(prefix);
                endNodeId = currentNode.setNamespace(prefix, nsuri, docid);
            }

            //=================================================================
           // attributes
           // Add all of the attributes
          for (int i = 0; i < atts.getLength(); i++)
          {
              String attributeName = atts.getQName(i);
              String attributeValue = atts.getValue(i);
              endNodeId = currentNode.setAttribute(attributeName,
                      attributeValue, docid);

              // To handle name space and schema location if the attribute
              // name is
              // xsi:schemaLocation. If the name space is in not in catalog
              // table
              // it will be regeistered.
              if (attributeName != null
                      && attributeName
                              .indexOf(MetaCatServlet.SCHEMALOCATIONKEYWORD) != -1) {
                  /*SchemaLocationResolver resolver = new SchemaLocationResolver(
                          attributeValue);
                  resolver.resolveNameSpace();*/

              }
              else if (attributeName != null && attributeName.equals(ID) &&
                       currentNode.getTagName().equals(DISTRIBUTION) &&
                       !inAdditionalMetaData)
              {
                 // this is a distribution element and the id is distributionID
                 distributionId = attributeValue;
                 distributionAllIdList.put(distributionId, distributionId);

              }

          }//for


           //=================================================================

            // handle access stuff
            //==================================================================
            if (localName.equals(ACCESS))
            {
                //make sure the access is top level
                // this mean current node's parent's parent should be "eml"
                DBSAXNode tmpNode = (DBSAXNode) nodeStack.pop();// pop out
                                                                    // parent
                                                                    // node
                //peek out grandParentNode
                DBSAXNode grandParentNode = (DBSAXNode) nodeStack.peek();
                // put parent node back
                nodeStack.push(tmpNode);
                String grandParentTag = grandParentNode.getTagName();
                if (grandParentTag.equals(EML) && !inAdditionalMetaData)
                {
                  processTopLevelAccess = true;

                }
                else if ( !inAdditionalMetaData )
                {
                  // process other access embedded into resource level
                  // module
                  processOtherAccess = true;
                }
                else
                {
                  // this for access in additional data which doesn't have a
                  // described element. If it has a described element,
                  // this code won't hurt any thing
                  processAdditionalAccess = true;
                }


                // create access object
                accessObject = new AccessSection();
                // set permission order
                String permOrder = currentNode.getAttribute(ORDER);
                accessObject.setPermissionOrder(permOrder);
                // set access id
                String accessId = currentNode.getAttribute(ID);
                accessObject.setSubTreeId(accessId);
                // for additional access subtree, the  start of node id should
                // be describe element. We also stored the start access element
                // node id too.
                if (processAdditionalAccess)
                {
                  accessObject.setStartedDescribesNodeId(firstDescribesNodeId);
                  accessObject.setControlLevel(DATAACCESSLEVEL);
                }
                else if (processTopLevelAccess)
                {
                  accessObject.setControlLevel(TOPLEVEL);
                }
                else if (processOtherAccess)
                {
                  accessObject.setControlLevel(REFERENCEDLEVEL);
                }

                accessObject.setStartNodeId(startNodeId);
                accessObject.setDocId(docid);



            }
            // Set up a access rule for allow
            else if (parentNode.getTagName() != null
                    && (parentNode.getTagName()).equals(ACCESS)
                    && localName.equals(ALLOW))
           {

                accessRule = new AccessRule();

                //set permission type "allow"
                accessRule.setPermissionType(ALLOW);

            }
            // set up an access rule for den
            else if (parentNode.getTagName() != null
                    && (parentNode.getTagName()).equals(ACCESS)
                    && localName.equals(DENY))
           {
                accessRule = new AccessRule();
                //set permission type "allow"
                accessRule.setPermissionType(DENY);
            }

            //=================================================================
            // some other independ stuff

            // Add the node to the stack, so that any text data can be
            // added as it is encountered
            nodeStack.push(currentNode);
            // Add the node to the vector used by thread for writing XML Index
            nodeIndex.addElement(currentNode);

            // store access module element and attributes into stored stack
            if (needToCheckAccessModule
                    && (processAdditionalAccess || processOtherAccess || processTopLevelAccess))
            {
                // stored the pull out nodes into storedNode stack
                NodeRecord nodeElement = new NodeRecord(-2, -2, -2, "ELEMENT",
                        localName, prefix, MetacatUtil.normalize(null));
                storedAccessNodeStack.push(nodeElement);
                for (int i = 0; i < atts.getLength(); i++) {
                    String attributeName = atts.getQName(i);
                    String attributeValue = atts.getValue(i);
                    NodeRecord nodeAttribute = new NodeRecord(-2, -2, -2,
                            "ATTRIBUTE", attributeName, null, MetacatUtil
                                    .normalize(attributeValue));
                    storedAccessNodeStack.push(nodeAttribute);
                }

            }

            if (currentNode.getTagName().equals(ADDITIONALMETADATA))
            {
              inAdditionalMetaData = true;
            }
            else if (currentNode.getTagName().equals(DESCRIBES) &&
                     parentNode.getTagName().equals(ADDITIONALMETADATA) &&
                     firstDescribesInAdditionalMetadata)
            {
              // this is first decirbes element in additional metadata
              firstDescribesNodeId = startNodeId;
              // we started process additional access rules here
              // because access and describe couldn't be seperated
              NodeRecord nodeElement = new NodeRecord(-2, -2, -2, "ELEMENT",
                        localName, prefix, MetacatUtil.normalize(null));
              storedAccessNodeStack.push(nodeElement);
              processAdditionalAccess = true;
              logMetacat.info("set processAdditonalAccess true when meet describe");
            }
            else if (inAdditionalMetaData && processAdditionalAccess &&
                     parentNode.getTagName().equals(ADDITIONALMETADATA) &&
                     !currentNode.getTagName().equals(DESCRIBES) &&
                     !currentNode.getTagName().equals(ACCESS))
            {
               // we start processadditionalAccess  module when first hit describes
               // in additionalMetadata. So this is possible, there are
               // "describes" but not "access". So here is try to terminate
               // processAddionalAccess. In this situation, there another element
               // rather than "describes" or "access" as a child of additionalMetadata
               // so this is impossible it will have access element now.
               // If additionalMetadata has access element, the flag will be
               // terminated in endElement
               processAdditionalAccess = false;
               logMetacat.warn("set processAddtionAccess false if the there is no access in additional");
            }
            else if (currentNode.getTagName().equals(DISTRIBUTION) &&
                     !inAdditionalMetaData)
            {
              proccessDistribution = true;
            }


             //==================================================================
            // reset name space
            namespaces = null;
            namespaces = new Hashtable();

        }//not inline data
        // inline data
        //++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
        //++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
        else
        {
            // we don't buffer the inline data in characters() method
            // so start character don't need to hand text node.

            // inline data may be the xml format.
            StringBuffer inlineElements = new StringBuffer();
            inlineElements.append("<").append(qName);
            // append attributes
            for (int i = 0; i < atts.getLength(); i++) {
                String attributeName = atts.getQName(i);
                String attributeValue = atts.getValue(i);
                inlineElements.append(" ");
                inlineElements.append(attributeName);
                inlineElements.append("=\"");
                inlineElements.append(attributeValue);
                inlineElements.append("\"");
            }
            // append namespace
            String prefix = null;
            String nsuri = null;
            Enumeration prefixes = inlineDataNameSpace.keys();
            while (prefixes.hasMoreElements()) {
                prefix = (String) prefixes.nextElement();
                nsuri =  (String)  inlineDataNameSpace.get(prefix);
                inlineElements.append(" ");
                inlineElements.append("xmlns:");
                inlineElements.append(prefix);
                inlineElements.append("=\"");
                inlineElements.append(nsuri);
                inlineElements.append("\"");
            }
            inlineElements.append(">");
            //reset inline data name space
            inlineDataNameSpace = null;
            inlineDataNameSpace = new Hashtable();
            //write inline data into file
            logMetacat.info("the inline element data is: "
                    + inlineElements.toString());
            writeInlineDataIntoFile(inlineDataFileWriter, inlineElements);
        }//else
        //++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
        //++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

    }


    /** SAX Handler that is called for each XML text node */
    public void characters(char[] cbuf, int start, int len) throws SAXException
    {
        logMetacat.info("CHARACTERS");
        if (!handleInlineData) {
            // buffer all text nodes for same element. This is for text was
            // splited
            // into different nodes
            textBuffer.append(new String(cbuf, start, len));
            // set hittextnode true
            hitTextNode = true;
            // if text buffer .size is greater than max, write it to db.
            // so we can save memory
            if (textBuffer.length() >= MAXDATACHARS)
            {
                logMetacat.info("Write text into DB in charaters"
                           + " when text buffer size is greater than maxmum number");
                DBSAXNode currentNode = (DBSAXNode) nodeStack.peek();
                endNodeId = writeTextForDBSAXNode(endNodeId, textBuffer,
                        currentNode);
                if (needToCheckAccessModule
                     && (processAdditionalAccess || processOtherAccess || processTopLevelAccess))
                {
                     // stored the pull out nodes into storedNode stack
                     NodeRecord nodeElement = new NodeRecord(-2, -2, -2, "TEXT",
                       null, null, MetacatUtil.normalize(textBuffer
                          .toString()));
                     storedAccessNodeStack.push(nodeElement);

                }
                textBuffer = null;
                textBuffer = new StringBuffer();
            }
        }
        else
        {
            // this is inline data and write file system directly
            // we don't need to buffered it.
            StringBuffer inlineText = new StringBuffer();
            inlineText.append(new String(cbuf, start, len));
            logMetacat.info(
                    "The inline text data write into file system: "
                            + inlineText.toString());
            writeInlineDataIntoFile(inlineDataFileWriter, inlineText);
        }
    }

    /** SAX Handler that is called at the end of each XML element */
    public void endElement(String uri, String localName, String qName)
            throws SAXException
    {
        logMetacat.info("End ELEMENT " + qName);

        // when close inline element
        //++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
        //++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
        if (localName.equals(INLINE) && handleInlineData)
        {
            // Get the node from the stack
            DBSAXNode currentNode = (DBSAXNode) nodeStack.pop();
            String currentTag = currentNode.getTagName();
            logMetacat.info("End of inline data");
            // close file writer
            try
            {
                inlineDataFileWriter.close();
                handleInlineData = false;
            }
            catch (IOException ioe)
            {
                throw new SAXException(ioe.getMessage());
            }

            //check if user changed inine data or not if user doesn't have
            // write permission for this inline block
            // if some error happends, we would delete the inline data file here,
            // we will call a method named deletedInlineFiles in DocumentImple
            if (unWritableInlineDataObject)
            {
                if (unReadableInlineDataObject)
                {
                  // now user just got a empty string in linline part
                  // so if the user send back a empty string is fine and we will
                  // copy the old file to new file. If he send something else,
                  // the document will be rejected
                  if (inlineDataIsEmpty(inlineDataFileName))
                  {
                    copyInlineFile(distributionId, inlineDataFileName);
                  }
                  else
                  {
                    logMetacat.info(
                               "inline data was changed by a user"
                                       + " who doesn't have permission");
                    throw new SAXException(PERMISSIONERROR);

                  }
                }//if
                else
                {
                  // user get the inline data
                  if (modifiedInlineData(distributionId, inlineDataFileName))
                  {
                    logMetacat.info(
                                "inline data was changed by a user"
                                        + " who doesn't have permission");
                    throw new SAXException(PERMISSIONERROR);
                  }//if
                }//else
            }//if
            else
            {
               //now user can update file.
               if (unReadableInlineDataObject)
               {
                  // now user just got a empty string in linline part
                  // so if the user send back a empty string is fine and we will
                  // copy the old file to new file. If he send something else,
                  // the new inline data will overwite the old one(here we need
                  // do nothing because the new inline data already existed
                  if (inlineDataIsEmpty(inlineDataFileName))
                  {
                    copyInlineFile(distributionId, inlineDataFileName);
                  }
                }//if

            }//else
            // put inline data file name into text buffer (without path)
            textBuffer = new StringBuffer(inlineDataFileName);
            // write file name into db
            endNodeId = writeTextForDBSAXNode(endNodeId, textBuffer,
                    currentNode);
            // reset textbuff
            textBuffer = null;
            textBuffer = new StringBuffer();
            // resetinlinedata file name
            inlineDataFileName = null;
            unWritableInlineDataObject = false;
            unReadableInlineDataObject = false;
            return;
        }
        //++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
        //++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++



        // close element which is not in inline data
        //++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
        //++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
        if (!handleInlineData)
        {
            // Get the node from the stack
            DBSAXNode currentNode = (DBSAXNode) nodeStack.pop();
            String currentTag = currentNode.getTagName();

            // If before the end element, the parser hit text nodes and store
            // them
            // into the buffer, write the buffer to data base. The reason we
            // put
            // write database here is for xerces some time split text node
            if (hitTextNode)
            {
                // get access value
                String data = null;
                // add principal
                if (currentTag.equals(PRINCIPAL) && accessRule != null)
                {
                    data = (textBuffer.toString()).trim();
                    accessRule.addPrincipal(data);

                }
                else if (currentTag.equals(PERMISSION) && accessRule != null)
                {
                    data = (textBuffer.toString()).trim();
                    // we conbine different a permission into one value
                    int permission = accessRule.getPermission();
                    // add permision
                    if (data.toUpperCase().equals(READSTRING))
                    {
                        permission = permission | READ;
                    }
                    else if (data.toUpperCase().equals(WRITESTRING))
                    {
                        permission = permission | WRITE;
                    }
                    else if (data.toUpperCase().equals(CHMODSTRING))
                    {
                        permission = permission | CHMOD;
                    }
                    else if (data.toUpperCase().equals(ALLSTRING))
                    {
                        permission = permission | ALL;
                    }
                    accessRule.setPermission(permission);
                }
                // put additionalmetadata/describes into vector
                else if (currentTag.equals(DESCRIBES))
                {
                    data = (textBuffer.toString()).trim();
                    describesId.add(data);
                    //firstDescribesInAdditionalMetadata = false;
                    //firstDescribesNodeId = 0;
                }
                else if (currentTag.equals(REFERENCES)
                        && (processTopLevelAccess || processAdditionalAccess || processOtherAccess))
                {
                    // get reference
                    data = (textBuffer.toString()).trim();
                    // put reference id into accessSection
                    accessObject.setReferences(data);

                }
                else if (currentTag.equals(REFERENCES) && proccessDistribution)
                {
                  // get reference for distribution
                  data = (textBuffer.toString()).trim();
                  // we only stored the distribution reference which itself
                  // has a id
                  if (distributionId != null)
                  {
                    distributionReferenceList.put(distributionId, data);
                  }

                }
                else if (currentTag.equals(URL) && !inAdditionalMetaData)
                {
                    //handle online data, make sure its'parent is online
                    DBSAXNode parentNode = (DBSAXNode) nodeStack.peek();
                    if (parentNode != null && parentNode.getTagName() != null
                            && parentNode.getTagName().equals(ONLINE))
                    {
                        data = (textBuffer.toString()).trim();
                        if (distributionId != null)
                        {
                          onlineURLDistributionIdList.put(distributionId, data);
                        }
                        else
                        {
                          onlineURLDistributionListWithoutId.add(data);
                        }
                    }//if
                }//else if
                // write text to db if it is not inline data

                logMetacat.info(
                            "Write text into DB in End Element");

                 // write text node into db
                 endNodeId = writeTextForDBSAXNode(endNodeId, textBuffer,
                            currentNode);

                if (needToCheckAccessModule
                        && (processAdditionalAccess || processOtherAccess || processTopLevelAccess)) {
                    // stored the pull out nodes into storedNode stack
                    NodeRecord nodeElement = new NodeRecord(-2, -2, -2, "TEXT",
                            null, null, MetacatUtil.normalize(textBuffer
                                    .toString()));
                    storedAccessNodeStack.push(nodeElement);

                }
            }//if handle text node



            //set hitText false
            hitTextNode = false;
            // reset textbuff
            textBuffer = null;
            textBuffer = new StringBuffer();


            // access stuff
            if (currentTag.equals(ALLOW) || currentTag.equals(DENY))
            {
                // finish parser a ccess rule and assign it to new one
                AccessRule newRule = accessRule;
                //add the new rule to access section object
                accessObject.addAccessRule(newRule);
                // reset access rule
                accessRule = null;
            }// ALLOW or DENY
            else if (currentTag.equals(ACCESS))
            {
                // finish parse a access setction and stored them into different
                // places

                accessObject.setEndNodeId(endNodeId);
                AccessSection newAccessObject = accessObject;
                newAccessObject.setStoredTmpNodeStack(storedAccessNodeStack);
                if (newAccessObject != null)
                {

                    if (processTopLevelAccess)
                    {
                       topAccessSection = newAccessObject;

                    }//if
                    else if (processAdditionalAccess)
                    {
                        // for additional control
                        // put discribesId into the accessobject and put this
                        // access object into vector
                        newAccessObject.setDescribedIdList(describesId);
                        additionalAccessVector.add(newAccessObject);

                    }//if
                    else if (processOtherAccess)
                    {
                      // we only stored the access object which has a id
                      // because only the access object which has a id can
                      // be reference
                      if (newAccessObject.getSubTreeId() != null &&
                          !newAccessObject.getSubTreeId().trim().equals(""))
                      {
                         possibleReferencedAccessHash.
                           put(newAccessObject.getSubTreeId(), newAccessObject);
                      }
                    }

                }//if
                //reset access section object
                accessObject = null;

                // reset tmp stored node stack
                storedAccessNodeStack = null;
                storedAccessNodeStack = new Stack();

                // reset flag
                processAdditionalAccess = false;
                processTopLevelAccess = false;
                processOtherAccess = false;

            }//access element
            else if (currentTag.equals(ADDITIONALMETADATA))
            {
                //reset describesId
                describesId = null;
                describesId = new Vector();
                inAdditionalMetaData = false;
                firstDescribesNodeId = -1;
                // reset tmp stored node stack
                storedAccessNodeStack = null;
                storedAccessNodeStack = new Stack();


            }
            else if (currentTag.equals(DISTRIBUTION) && !inAdditionalMetaData)
            {
               //reset distribution id
               distributionId = null;
               proccessDistribution = false;
            }
            else if (currentTag.equals(OFFLINE) && !inAdditionalMetaData)
            {
               if (distributionId != null)
               {
                 offlineDistributionIdList.put(distributionId, distributionId);
               }
            }
            else if ((currentTag.equals(CONNECTION) || currentTag.equals(CONNECTIONDEFINITION))
                     && !inAdditionalMetaData)
            {
              //handle online data, make sure its'parent is online
                 DBSAXNode parentNode = (DBSAXNode) nodeStack.peek();
                 if (parentNode != null && parentNode.getTagName() != null
                         && parentNode.getTagName().equals(ONLINE))
                 {
                     if (distributionId != null)
                     {
                        onlineOtherDistributionIdList.put(distributionId, distributionId);
                     }
                 }//if

            }//else if
            else if (currentTag.equals(DESCRIBES))
            {
                firstDescribesInAdditionalMetadata = false;

            }



        }
        // close elements which are in inline data (inline data can be xml doc)
        //++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
        //++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
        else
        {
            // this is in inline part
            StringBuffer endElement = new StringBuffer();
            endElement.append("</");
            endElement.append(qName);
            endElement.append(">");
            logMetacat.info("inline endElement: "
                    + endElement.toString());
            writeInlineDataIntoFile(inlineDataFileWriter, endElement);
        }
        //++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
        //++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
    }


    /*
     * Method to check if the new line data is as same as the old one
     */
     private boolean modifiedInlineData(String inlineDistributionId,
			String newInlineInternalFileName) throws SAXException {
       boolean modified = true;
       if (inlineDistributionId == null || newInlineInternalFileName == null)
       {
         return modified;
       }
       String oldInlineInternalFileName =
            (String)previousUnwritableInlineDataObjectHash.get(inlineDistributionId);
       if (oldInlineInternalFileName == null ||
           oldInlineInternalFileName.trim().equals(""))
       {
         return modified;
       }
       logMetacat.info("in handle inline data");
       logMetacat.info("the inline data file name from xml_access is: "
                                    + oldInlineInternalFileName);

       try
       {
         if (!compareInlineDataFiles(oldInlineInternalFileName,
                                     newInlineInternalFileName))
         {
           modified = true;

         }
         else
         {
           modified = false;
         }
       }
       catch(Exception e)
       {
         modified = true;
       }

       // delete the inline data file already in file system
       if (modified)
       {
         deleteInlineDataFile(newInlineInternalFileName);

       }
       return modified;
     }

     /*
      * A method to check if a line file is empty
      */
     private boolean inlineDataIsEmpty(String fileName) throws SAXException
     {
        boolean isEmpty = true;
        if ( fileName == null)
        {
          throw new SAXException("The inline file name is null");
        }
        
        try {
			String path = PropertyService.getProperty("application.inlinedatafilepath");
			// the new file name will look like path/docid.rev.2
			File inlineDataDirectory = new File(path);
			File inlineDataFile = new File(inlineDataDirectory, fileName);

			Reader inlineFileReader = new InputStreamReader(new FileInputStream(inlineDataFile), encoding);
			BufferedReader inlineStringReader = new BufferedReader(inlineFileReader);
			String string = inlineStringReader.readLine();
			// at the end oldstring will be null
			while (string != null) {
				string = inlineStringReader.readLine();
				if (string != null && !string.trim().equals("")) {
					isEmpty = false;
					break;
				}
			}

		} catch (Exception e) {
			throw new SAXException(e.getMessage());
		}
		return isEmpty;

     }


    /**
	 * SAX Handler that receives notification of comments in the DTD
	 */
    public void comment(char[] ch, int start, int length) throws SAXException
    {
        logMetacat.info("COMMENT");
        if (!handleInlineData) {
            if (!processingDTD) {
                DBSAXNode currentNode = (DBSAXNode) nodeStack.peek();
                String str = new String(ch, start, length);

                //compare comment if need
                /*if (startCriticalSubTree) {
                    compareCommentNode(currentUnChangedableSubtreeNodeStack,
                            str, PERMISSIONERROR);
                }//if*/
                //compare top level access module
                if (processTopLevelAccess && needToCheckAccessModule) {
                    /*compareCommentNode(currentUnchangableAccessModuleNodeStack,
                            str, UPDATEACCESSERROR);*/
                }
                endNodeId = currentNode.writeChildNodeToDB("COMMENT", null,
                        str, docid);
                if (needToCheckAccessModule
                        && (processAdditionalAccess || processOtherAccess || processTopLevelAccess)) {
                    // stored the pull out nodes into storedNode stack
                    NodeRecord nodeElement = new NodeRecord(-2, -2, -2,
                            "COMMENT", null, null, MetacatUtil.normalize(str));
                    storedAccessNodeStack.push(nodeElement);

                }
            }
        } else {
            // inline data comment
            StringBuffer inlineComment = new StringBuffer();
            inlineComment.append("<!--");
            inlineComment.append(new String(ch, start, length));
            inlineComment.append("-->");
            logMetacat.info("inline data comment: "
                    + inlineComment.toString());
            writeInlineDataIntoFile(inlineDataFileWriter, inlineComment);
        }
    }



    /**
     * SAX Handler called once for each processing instruction found: node that
     * PI may occur before or after the root element.
     */
    public void processingInstruction(String target, String data)
            throws SAXException
    {
        logMetacat.info("PI");
        if (!handleInlineData) {
            DBSAXNode currentNode = (DBSAXNode) nodeStack.peek();
            endNodeId = currentNode.writeChildNodeToDB("PI", target, data,
                    docid);
        } else {
            StringBuffer inlinePI = new StringBuffer();
            inlinePI.append("<?");
            inlinePI.append(target);
            inlinePI.append(" ");
            inlinePI.append(data);
            inlinePI.append("?>");
            logMetacat.info("inline data pi is: "
                    + inlinePI.toString());
            writeInlineDataIntoFile(inlineDataFileWriter, inlinePI);
        }
    }

    /** SAX Handler that is called at the start of Namespace */
    public void startPrefixMapping(String prefix, String uri)
            throws SAXException
    {
        logMetacat.info("NAMESPACE");
        logMetacat.info("NAMESPACE prefix "+prefix);
        logMetacat.info("NAMESPACE uri "+uri);
        if (!handleInlineData) {
            namespaces.put(prefix, uri);
        } else {
            inlineDataNameSpace.put(prefix, uri);
        }
    }

    /**
     * SAX Handler that is called for each XML text node that is Ignorable
     * white space
     */
    public void ignorableWhitespace(char[] cbuf, int start, int len)
            throws SAXException
    {
        // When validation is turned "on", white spaces are reported here
        // When validation is turned "off" white spaces are not reported here,
        // but through characters() callback
        logMetacat.info("IGNORABLEWHITESPACE");
        if (!handleInlineData) {
            DBSAXNode currentNode = (DBSAXNode) nodeStack.peek();
            String data = new String(cbuf, start, len);

                //compare whitespace in access top module
                if (processTopLevelAccess && needToCheckAccessModule) {
                    /*compareWhiteSpace(currentUnchangableAccessModuleNodeStack,
                            data, UPDATEACCESSERROR);*/
                }
                // Write the content of the node to the database
                if (needToCheckAccessModule
                        && (processAdditionalAccess || processOtherAccess || processTopLevelAccess)) {
                    // stored the pull out nodes into storedNode stack
                    NodeRecord nodeElement = new NodeRecord(-2, -2, -2, "TEXT",
                            null, null, MetacatUtil.normalize(data));
                    storedAccessNodeStack.push(nodeElement);

                }
                endNodeId = currentNode.writeChildNodeToDB("TEXT", null, data,
                        docid);
        } else {
            //This is inline data write to file directly
            StringBuffer inlineWhiteSpace = new StringBuffer(new String(cbuf,
                    start, len));
            writeInlineDataIntoFile(inlineDataFileWriter, inlineWhiteSpace);
        }

    }


    /** SAX Handler that receives notification of end of the document */
    public void endDocument() throws SAXException
    {
        logMetacat.info("end Document");
        if (needToCheckAccessModule)
        {
          compareAllAccessModules();
        }

        // user deleted some inline block which it counldn't delete
        if (numberOfHitUnWritableInlineData !=
            previousUnwritableInlineDataObjectHash.size())
        {
          throw new SAXException("user deleted some inline block it couldn't");
        }

        if (!super.getIsRevisionDoc())
        {
          // write access rule to xml_access table which include both top level
          // and additional level(data access level). It also write access subtree
          // info into xml_accesssubtree about the top access, additional access
          // and some third place access modules which are referenced by top
          // level or additional level
        	if (writeAccessRules ) {
        		writeAccessRuleToDB();
        	}

          //delete relation table
          deleteRelations();
          //write relations
           for (int i = 0; i < onlineDataFileIdInRelationVector.size(); i++) {
            String id = (String) onlineDataFileIdInRelationVector.elementAt(i);
            writeOnlineDataFileIdIntoRelationTable(id);
           }
        }

        // clean the subtree record
        accessSubTreeAlreadyWriteDBList = new Hashtable();
    }



    /* The method will compare all access modules in eml document -
     * topLevel, additionalLevel(data access) and referenced access module*/
    private void compareAllAccessModules() throws SAXException
    {
        String guid = docid+"."+revision;
        try {
            guid = IdentifierManager.getInstance().getGUID(docid, Integer.valueOf(revision));
        } catch (Exception e) {
            logMetacat.warn("Eml200SAXHandler.compareAllAccessModules - we can't get object identifier for metacat id "+guid);
        }
      //compare top level
      compareAccessSubtree(topAccessSubTreeFromDB, topAccessSection, guid);

      //compare additional level
      int oldSize = additionalAccessSubTreeListFromDB.size();
      int newSize = additionalAccessVector.size();
     
      
      // if size is different, use deleted or added rules, so throw a exception
      if (oldSize != newSize)
      {
        throw new SAXException(UPDATEACCESSERROR+guid);
      }
      //because access modules are both ordered in ASC in vectors, so we can
      // compare one bye one
      for ( int i = 0; i < newSize; i++)
      {
        AccessSection fromDB = (AccessSection)
                          additionalAccessSubTreeListFromDB.elementAt(i);
        AccessSection fromParser = (AccessSection)
                                additionalAccessVector.elementAt(i);
        compareAccessSubtree(fromDB, fromParser, guid);
      }

      //compare referenced level
      Enumeration em = referencedAccessSubTreeListFromDB.keys();
      while (em.hasMoreElements())
      {
        String id = (String)em.nextElement();
        AccessSection fromDB = (AccessSection)
                               referencedAccessSubTreeListFromDB.get(id);
        AccessSection fromParser = (AccessSection)
                               possibleReferencedAccessHash.get(id);
        compareAccessSubtree(fromDB, fromParser, guid);
      }
    }

    /* The method will compare two access subtree. Currently they compare to
     * nodes one by one. It also can be changed to parse the node first, then
     * compare the parsed result
     */
    private void compareAccessSubtree(AccessSection fromDBTable,
                                       AccessSection fromParser, String identifier)
                                      throws SAXException
    {
       if (fromDBTable == null || fromParser == null)
       {
         throw new SAXException(UPDATEACCESSERROR+identifier);
       }
       Stack nodeStackFromDBTable = fromDBTable.getSubTreeNodeStack();
       Stack nodeStackFromParser  = fromParser.getStoredTmpNodeStack();

       Stack tempStack = new Stack();
       while(!nodeStackFromDBTable.isEmpty()){
           tempStack.push(nodeStackFromDBTable.pop());
       }
       comparingNodeStacks(tempStack, nodeStackFromParser, identifier);
    }

    /* Compare two node stacks to see if they are same */
  private void comparingNodeStacks(Stack stack1, Stack stack2, String identifier)
          throws SAXException
  {
      // make sure stack1 and stack2 are not empty
      if (stack1.isEmpty() || stack2.isEmpty()) {
          logMetacat.info("Because stack is empty!");
          throw new SAXException(UPDATEACCESSERROR+identifier);
      }
      // go throw two stacks and compare every element
      while (!stack1.isEmpty()) {
          // Pop an element from stack1
          NodeRecord record1 = (NodeRecord) stack1.pop();

          // Pop an element from stack2(stack 2 maybe empty)
          NodeRecord record2 = null;
          try {
              record2 = (NodeRecord) stack2.pop();
          } catch (EmptyStackException ee) {

              logMetacat.error(
                      "Node stack2 is empty but stack1 isn't!");
              throw new SAXException(UPDATEACCESSERROR+identifier);
          }
          // if two records are not same throw a exception
          if (!record1.contentEquals(record2)) {
              logMetacat.info("Two records from new and old stack are not "
                                      + "same!" + record1 + "--" +record2);
              throw new SAXException(UPDATEACCESSERROR+identifier);
          }//if
      }//while

      // now stack1 is empty and we should make sure stack2 is empty too
      if (!stack2.isEmpty()) {

          logMetacat.info(
                  "stack2 still have some elements while stack1 "
                          + "is empty! ");
          throw new SAXException(UPDATEACCESSERROR+identifier);
      }//if
  }//comparingNodeStacks


    /* The method to write all access rule into db */
    private void writeAccessRuleToDB() throws SAXException
    {
        // delete xml_accesssubtree table record for this docid
        deleteAccessSubTreeRecord(docid);
        //write additional access rule, and old records in xml_access will be
        //deleted too
        //System.out.println("before write additional access rules");
        writeadditionalAccessRuleToDB();
        //System.out.println("after write additional access rules");
        //write top leve access rule, and old records in xml_access will be
        //deleted too

        if (topAccessSection != null){
          writeTopLevelAccessRuleToDB();
        }
        //System.out.println("after write top access rules");
    }//writeAccessRuleToDB


    /* The method will figure out access reference for given access section -
     * return a new AccessSection which contain access rules that be referenced.
     * And will write the access subtree into xml_access table.
     * this is a recursive method
     */
   private AccessSection resolveAccessRuleReference(AccessSection access)
                                                    throws SAXException
   {
     if (access == null)
     {
       logMetacat.info("access module is null in " +
                                "resolveAccessRulesReference");
       throw new SAXException("An access modules is null");
     }
     String subTreeId = access.getSubTreeId();
     if (subTreeId == null ||
         (subTreeId != null && !accessSubTreeAlreadyWriteDBList.contains(subTreeId)))
     {
        // we should record this access subtree into accesssubtree table.
        // subtreeId is null means it can't be referenced. So this tree couldn't
        // be stored twise in the table. Subtree is not null, but it is in
        // hash yet, so it is new one.
        writeAccessSubTreeIntoDB(access);
        if (subTreeId != null)
        {
          accessSubTreeAlreadyWriteDBList.put(subTreeId, subTreeId);
        }
     }

     String reference = access.getReferences();
     if (reference != null)
     {
       // find the reference in top level
       String topSubtreeId = topAccessSection.getSubTreeId();
       if (topSubtreeId != null && topSubtreeId.equals(reference))
       {
          return resolveAccessRuleReference(topAccessSection);
       }
       else
       {
           // search it the additional access
           for ( int i = 0; i <additionalAccessVector.size(); i++)
           {
             AccessSection additionalAccess = (AccessSection)
                           additionalAccessVector.elementAt(i);
             String additionId = additionalAccess.getSubTreeId();
             if (additionId != null && additionId.equals(reference))
             {
               return resolveAccessRuleReference(additionalAccess);
             }// if
           }// for

           // search possible referenced access hashtable
           if (possibleReferencedAccessHash.containsKey(reference))
           {
             AccessSection referenceAccess = (AccessSection)
                         possibleReferencedAccessHash.get(reference);
             return resolveAccessRuleReference(referenceAccess);
           }

           // if hit here, this means you don't find any id match the reference
           // throw a exception
           throw new SAXException("No access module's id match the reference id");
       }
     }
     else
     {
       // base line reference == null
       AccessSection newAccessSection = new AccessSection();
       access.copyPermOrderAndAccessRules(newAccessSection);
       return newAccessSection;
     }
   }//resolveAccessRuleReference

   /* This method will return a id which points a real distribution block if
    *  given a distribution id which is a reference. If the given id is a real
    *  distribution the id itself will be returned.
    *  Here is segment of eml
    *   <distribution id ="100"><online><url>abc</url></online></distribution>
    *   <distribution id ="200"><reference>100</reference></distribution>
    * if the given value is 200, 100 will be returned.
    * if the given value is 100, 100 will be returned.
    */
   private String resolveDistributionReference(String givenId)
   {
      if (givenId == null )
      {
        return null;
      }
      if (!distributionReferenceList.containsKey(givenId))
      {
        //this is not reference distribution block, return given id
        return givenId;
      }
      else
      {
         String referencedId = (String) distributionReferenceList.get(givenId);
         // search util the referenced id is not in dsitribtionReferenceList
         while (distributionReferenceList.containsKey(referencedId))
         {
           referencedId = (String) distributionReferenceList.get(referencedId);
         }
         return referencedId;
      }
   }


  /* The method to write additional access rule into db. The old rules will be
   * deleted
   * If no describedId in the access object, this access rules will be ingorned
   */
  private void writeadditionalAccessRuleToDB() throws SAXException
  {
     //System.out.println("in write additional");
     // we should delete all inline access rules in xml_access if
     // user has all permission
     if (!needToCheckAccessModule)
     {
       deleteAllInlineDataAccessRules();
     }
     for (int i=0; i < additionalAccessVector.size(); i++)
     {
       //System.out.println("in for loop of write additional");
       AccessSection access = (AccessSection)additionalAccessVector.elementAt(i);
       Vector describeIdList = access.getDescribedIdList();
       // if this access is a reference, a new access object will be created
       // which contains the real access rules referenced. Also, each access tree
       // will be write into xml_accesssubtee table
       AccessSection newAccess = resolveAccessRuleReference(access);
       String permOrder = newAccess.getPermissionOrder();
       if (permOrder.equals(AccessControlInterface.DENYFIRST) && ignoreDenyFirst) {
    	   logMetacat.warn("Metacat no longer supports EML 'denyFirst' access rules - ignoring this access block");
    	   return;
       }
       Vector accessRule = newAccess.getAccessRules();

       if (describeIdList == null || describeIdList.isEmpty())
       {
         continue;
       }

       for (int j = 0; j < describeIdList.size(); j++)
       {
         String subreeid = (String)describeIdList.elementAt(j);
         logMetacat.info("describe id in additional access " +
                                  subreeid);
         // we need to figure out the real id if this subreeid is points to
         // a distribution reference.
         subreeid = resolveDistributionReference(subreeid);
         if (subreeid != null && !subreeid.trim().equals(""))
         {
           logMetacat.info("subtree id is "+ subreeid +
                                    " after resolve reference id" );
           // if this id is for line data, we need to delete the records first
           // then add new records. The key for deleting is subtee id
           if (inlineDistributionIdList.containsKey(subreeid))
           {
             String inlineFileName = (String)
                                        inlineDistributionIdList.get(subreeid);
             deleteSubtreeAccessRule(subreeid);
             logMetacat.info("Write inline data access into " +
                                   "xml_access table for"+ inlineFileName);
             writeGivenAccessRuleIntoDB(permOrder, accessRule,
                                        inlineFileName, subreeid);
             // Save guid of data object for syncing of access policy with CN after parsing
             // is successful (see DocumentImpl.write)
             // look-up pid assuming docid
             String dataGuid = inlineFileName;
             try {
	             String dataDocid = DocumentUtil.getDocIdFromAccessionNumber(inlineFileName);
	             int dataRev = DocumentUtil.getRevisionFromAccessionNumber(inlineFileName);
	             dataGuid = IdentifierManager.getInstance().getGUID(dataDocid, dataRev);
             } catch (McdbDocNotFoundException e) {
            	 // log the warning
            	 logMetacat.warn("No pid found for [assumed] data docid: " + inlineFileName);
             }
 			 guidsToSync.add(dataGuid);
           }
           else if (onlineURLDistributionIdList.containsKey(subreeid))
           {
             String url = (String)onlineURLDistributionIdList.get(subreeid);
             //this method will extrace data file id from url. It also will
             // check if user can change the access rules for the data file id.
             // if couldn't, it will throw a exception. Morover, the docid will
             // be to relation id vector too.
             // for online data, the subtree id we set is null.
             // So in xml_access, only the inline data subteeid is not null
             String dataFileName = handleOnlineUrlDataFile(url);
             logMetacat.info("The data fileName in online url " +
                                      dataFileName);
             if (dataFileName != null)
             {
               deletePermissionsInAccessTableForDoc(dataFileName);
               writeGivenAccessRuleIntoDB(permOrder, accessRule,
                                          dataFileName, null);
               logMetacat.info("Write online data access into " +
                                   "xml_access table for " + dataFileName);
               // Save guid of data object for syncing of access policy with CN after parsing
               // is successful (see DocumentImpl.write)
               // look-up pid assuming docid
               String dataGuid = dataFileName;
               try {
  	             String dataDocid = DocumentUtil.getDocIdFromAccessionNumber(dataFileName);
  	             int dataRev = DocumentUtil.getRevisionFromAccessionNumber(dataFileName);
  	             dataGuid = IdentifierManager.getInstance().getGUID(dataDocid, dataRev);
               } catch (McdbDocNotFoundException e) {
              	 // log the warning
              	 logMetacat.warn("No pid found for [assumed] data docid: " + dataFileName);
               }
               guidsToSync.add(dataGuid);
               // put the id into a hashtalbe. So when we run wirtetop level
               // access, those id will be ignored because they already has
               // additional access rules
               onlineURLIdHasadditionalAccess.put(subreeid, subreeid);
             }
           }//elseif
         }//if
       }//for
     }//for

   }//writeAdditonalLevelAccessRuletoDB


    /* The method to write additional access rule into db. */
    private void writeTopLevelAccessRuleToDB() throws SAXException
    {
       // if top access is reference, we need figure out the real access rules
       // it points to
       //System.out.println("permorder in top level" + topAccessSection.getPermissionOrder());
       AccessSection newAccess = resolveAccessRuleReference(topAccessSection);
       //System.out.println("permorder in new level" + newAccess.getPermissionOrder());
       String permOrder = newAccess.getPermissionOrder();
       if (permOrder.equals(AccessControlInterface.DENYFIRST) && ignoreDenyFirst) {
    	   logMetacat.warn("Metacat no longer supports EML 'denyFirst' access rules - ignoring this access block");
    	   return;
       }
       Vector accessRule = newAccess.getAccessRules();
       String subtree     = null;
       
       // document itself
       // use GUID
       String guid = null;
		try {
			guid = IdentifierManager.getInstance().getGUID(docid, Integer.valueOf(revision));
		} catch (NumberFormatException e) {
			throw new SAXException(e.getMessage(), e);
		} catch (McdbDocNotFoundException e) {
			// register the default mapping now
			guid = docid + "." + revision;
			IdentifierManager.getInstance().createMapping(guid, guid);
		}
       deletePermissionsInAccessTableForDoc(guid);
       writeGivenAccessRuleIntoDB(permOrder, accessRule, guid, subtree);
       
       // for online data, it includes with id and without id.
       // 1. for the data with subtree id, we should ignore the ones already in
       // the hash - onlineURLIdHasAddionalAccess.
       // 2. for those without subreeid, it couldn't have additional access and we
       // couldn't ignore it.
       // 3. for inline data, we need do nothing because if it doesn't have
       // additional access, it default value is the top one.

       // here is the online url with id
       Enumeration em = onlineURLDistributionIdList.keys();
       while (em.hasMoreElements())
       {
         String onlineSubtreeId = (String)em.nextElement();
         if (!onlineURLIdHasadditionalAccess.containsKey(onlineSubtreeId))
         {
            String url =
                       (String)onlineURLDistributionIdList.get(onlineSubtreeId);
            String onlineDataId = handleOnlineUrlDataFile(url);
            if (onlineDataId != null)
            {
              deletePermissionsInAccessTableForDoc(onlineDataId);
              writeGivenAccessRuleIntoDB(permOrder, accessRule,
                                         onlineDataId, subtree);
            }

         }
       }//while

       // here is the onlineURL without id
       for (int i= 0; i < onlineURLDistributionListWithoutId.size(); i++)
       {
         String url = (String)onlineURLDistributionListWithoutId.elementAt(i);
         String onlineDataId = handleOnlineUrlDataFile(url);
         if (onlineDataId != null)
         {
           deletePermissionsInAccessTableForDoc(onlineDataId);
           writeGivenAccessRuleIntoDB(permOrder, accessRule,
                                         onlineDataId, subtree);
         }
       }//for
    }//writeTopAccessRuletoDB

    /* Write a gaven access rule into db */
    private void writeGivenAccessRuleIntoDB(String permOrder, Vector accessRules,
                     String dataId, String subTreeId) throws SAXException
    {
      if (permOrder == null || permOrder.trim().equals("") || dataId == null ||
          dataId.trim().equals("") || accessRules == null ||
          accessRules.isEmpty())
      {
        logMetacat.info("The access object is null and tried to " +
                                  " write to xml_access table");
        throw new SAXException("The access object is null");
      }
      
      // geet the guid, not the docid alone
      String guid = null;
		try {
			guid = IdentifierManager.getInstance().getGUID(docid, Integer.valueOf(revision));
		} catch (NumberFormatException e) {
			throw new SAXException(e.getMessage(), e);
		} catch (McdbDocNotFoundException e) {
			// register the default mapping now
			guid = docid + "." + revision;
			IdentifierManager.getInstance().createMapping(guid, guid);
		}
      
       String sql = null;
       PreparedStatement pstmt = null;
       sql = "INSERT INTO xml_access (guid, principal_name, permission, "
               + "perm_type, perm_order, accessfileid, subtreeid) VALUES "
               + " (?, ?, ?, ?, ?, ?, ?)";

       try
       {

           pstmt = connection.prepareStatement(sql);
           // Increase DBConnection usage count
           connection.increaseUsageCount(1);
           // Bind the values to the query
           pstmt.setString(1, dataId);
           logMetacat.info("guid in accesstable: " + dataId);
           pstmt.setString(6, guid);
           logMetacat.info("Accessfileid in accesstable: " + guid);
           pstmt.setString(5, permOrder);
           logMetacat.info("PermOder in accesstable: " + permOrder);
           pstmt.setString(7, subTreeId);
           logMetacat.info("subtree id in accesstable: " + subTreeId);
           // if it is not top level, set s id

           //Vector accessRules = accessSection.getAccessRules();
           // go through every rule
           for (int i = 0; i < accessRules.size(); i++)
           {
               AccessRule rule = (AccessRule) accessRules.elementAt(i);
               String permType = rule.getPermissionType();
               int permission = rule.getPermission();
               pstmt.setInt(3, permission);
               logMetacat.info("permission in accesstable: "
                       + permission);
               pstmt.setString(4, permType);
               logMetacat.info(
                       "Permtype in accesstable: " + permType);
               // go through every principle in rule
               Vector nameVector = rule.getPrincipal();
               for (int j = 0; j < nameVector.size(); j++)
               {
                   String prName = (String) nameVector.elementAt(j);
                   pstmt.setString(2, prName);
                   logMetacat.info("Principal in accesstable: "
                           + prName);
                   logMetacat.debug("running sql: " + pstmt.toString());
                   pstmt.execute();
               }//for
           }//for
           pstmt.close();
       }//try
       catch (SQLException e)
       {
           throw new SAXException("EMLSAXHandler.writeAccessRuletoDB(): "
                   + e.getMessage());
       }//catch
       finally
       {
           try
           {
               pstmt.close();
           }
           catch (SQLException ee)
           {
               throw new SAXException("EMLSAXHandler.writeAccessRuletoDB(): "
                       + ee.getMessage());
           }
       }//finally
       
       // for D1, refresh the entries
       HazelcastService.getInstance().refreshSystemMetadataEntry(guid);
       HazelcastService.getInstance().refreshSystemMetadataEntry(dataId);

    }//writeGivenAccessRuleIntoDB


    /* Delete from db all permission for resources related guid, if any. */
    private void deletePermissionsInAccessTableForDoc(String guid)
            throws SAXException
    {
        PreparedStatement pstmt = null;
        try {
        	String sql = "DELETE FROM xml_access WHERE guid = ? ";
            // delete all acl records for resources related to guid if any
            pstmt = connection.prepareStatement(sql);
            pstmt.setString(1, guid);
            // Increase DBConnection usage count
            connection.increaseUsageCount(1);
            pstmt.execute();

        } catch (SQLException e) {
            throw new SAXException(e.getMessage());
        } finally {
            try {
                pstmt.close();
            } catch (SQLException ee) {
                throw new SAXException(ee.getMessage());
            }
        }
    }//deletePermissionsInAccessTable

    /* Delete access rules from xml_access for a subtee id */
    private void deleteSubtreeAccessRule(String subtreeid) throws SAXException
    {
      PreparedStatement pstmt = null;
       try
       {
    	   String sql = 
    		   "DELETE FROM xml_access " +
    		   "WHERE accessfileid IN (SELECT guid from identifier where docid = ? and rev = ?) " +
               "AND subtreeid = ?";
           pstmt = connection.prepareStatement(sql);
           pstmt.setString(1, docid);
           pstmt.setInt(2, Integer.valueOf(revision));
           pstmt.setString(3, subtreeid);
           // Increase DBConnection usage count
           connection.increaseUsageCount(1);
           pstmt.execute();
       }
       catch (SQLException e)
       {
           throw new SAXException(e.getMessage());
       }
       finally
       {
           try
           {
               pstmt.close();
           }
           catch (SQLException ee)
           {
               throw new SAXException(ee.getMessage());
           }
       }

    }

    private void deleteAllInlineDataAccessRules() throws SAXException
    {
      PreparedStatement pstmt = null;
       try
       {
    	   String sql = 
    		   "DELETE FROM xml_access " +
    		   "WHERE accessfileid IN (SELECT guid from identifier where docid = ? and rev = ?) " +
    		   "AND subtreeid IS NOT NULL";
           pstmt = connection.prepareStatement(sql);
           pstmt.setString(1, docid);
           pstmt.setInt(2, Integer.valueOf(revision));
           // Increase DBConnection usage count
           connection.increaseUsageCount(1);
           pstmt.execute();
       }
       catch (SQLException e)
       {
           throw new SAXException(e.getMessage());
       }
       finally
       {
           try
           {
               pstmt.close();
           }
           catch (SQLException ee)
           {
               throw new SAXException(ee.getMessage());
           }
       }

    }

    /*
     * In order to make sure only usr has "all" permission can update access
     * subtree in eml document we need to keep access subtree info in
     * xml_accesssubtree table, such as docid, version, startnodeid, endnodeid
     */
    private void writeAccessSubTreeIntoDB(AccessSection accessSection)
                                          throws SAXException
    {
        if (accessSection == null)
        {

          logMetacat.info("Access object is null and tried to write "+
                                   "into access subtree table");
          throw new SAXException("The access object is null to write access " +
                                 "sbutree");
        }

        String sql = null;
        PreparedStatement pstmt = null;
        sql = "INSERT INTO xml_accesssubtree (docid, rev, controllevel, "
                + "subtreeid, startnodeid, endnodeid) VALUES "
                + " (?, ?, ?, ?, ?, ?)";
        try
        {

            pstmt = connection.prepareStatement(sql);
            // Increase DBConnection usage count
            connection.increaseUsageCount(1);
            String level = accessSection.getControlLevel();
            long startNodeId = -1;
            if (level != null && level.equals(DATAACCESSLEVEL))
            {
              // for additional access module the start node id should be
              // descirbes element id
              startNodeId = accessSection.getStartedDescribesNodeId();
              // if in additional access, there is not describes element,
              // in this senario, describesNodeId will be -1. Then we should
              // the start access element id
              if (startNodeId == -1)
              {
                startNodeId = accessSection.getStartNodeId();
              }
            }
            else
            {
                startNodeId = accessSection.getStartNodeId();
            }

            long endNodeId = accessSection.getEndNodeId();
            String sectionId = accessSection.getSubTreeId();

            if (startNodeId ==-1 || endNodeId == -1)
            {
              throw new SAXException("Don't find start node or end node id " +
                                      "for the access subtee");

            }

            // Bind the values to the query
            pstmt.setString(1, docid);
            logMetacat.info("Docid in access-subtreetable: " + docid);
            pstmt.setInt(2, (new Integer(revision)).intValue());
            logMetacat.info("rev in accesssubtreetable: " + revision);
            pstmt.setString(3, level);
            logMetacat.info("contorl level in access-subtree table: "
                    + level);
            pstmt.setString(4, sectionId);
            logMetacat.info("Subtree id in access-subtree table: "
                    + sectionId);
            pstmt.setLong(5, startNodeId);
            logMetacat.info("Start node id is: " + startNodeId);
            pstmt.setLong(6, endNodeId);
            logMetacat.info("End node id is: " + endNodeId);
            logMetacat.trace("Eml200SAXHandler.writeAccessSubTreeIntoDB - executing SQL: " + pstmt.toString());
            pstmt.execute();
            pstmt.close();
        }//try
        catch (SQLException e)
        {
            throw new SAXException("EMLSAXHandler.writeAccessSubTreeIntoDB(): "
                    + e.getMessage());
        }//catch
        finally
        {
            try
            {
                pstmt.close();
            }
            catch (SQLException ee)
            {
                throw new SAXException(
                        "EMLSAXHandler.writeAccessSubTreeIntoDB(): "
                                + ee.getMessage());
            }
        }//finally

    }//writeAccessSubtreeIntoDB

    /* Delete every access subtree record from xml_accesssubtree. */
    private void deleteAccessSubTreeRecord(String docId) throws SAXException
    {
        PreparedStatement pstmt = null;
        try {
        	String sql = "DELETE FROM xml_accesssubtree WHERE docid = ?";
            // delete all acl records for resources related to @aclid if any
            pstmt = connection.prepareStatement(sql);
            pstmt.setString(1, docId);
            // Increase DBConnection usage count
            connection.increaseUsageCount(1);                   
            logMetacat.debug("running sql: " + sql);
            pstmt.execute();

        } catch (SQLException e) {
            throw new SAXException(e.getMessage());
        } finally {
            try {
                pstmt.close();
            } catch (SQLException ee) {
                throw new SAXException(ee.getMessage());
            }
        }
    }//deleteAccessSubTreeRecord

    // open a file writer for writing inline data to file
    private Writer createInlineDataFileWriter(String fileName, String encoding)
            throws SAXException
    {
        Writer writer = null;
        String path;
        try {
        	 path = PropertyService.getProperty("application.inlinedatafilepath");
        } catch (PropertyNotFoundException pnfe) {
            throw new SAXException(pnfe.getMessage());
        }
        /*
         * File inlineDataDirectory = new File(path);
         */
        String newFile = path + "/" + fileName;
        logMetacat.info("inline file name: " + newFile);
        try {
            // true means append
        	writer = new OutputStreamWriter(new FileOutputStream(newFile, true), encoding);
        } catch (IOException ioe) {
            throw new SAXException(ioe.getMessage());
        }
        return writer;
    }

    // write inline data into file system and return file name(without path)
    private void writeInlineDataIntoFile(Writer writer, StringBuffer data)
            throws SAXException
    {
        try {
            writer.write(data.toString());
            writer.flush();
        } catch (Exception e) {
            throw new SAXException(e.getMessage());
        }
    }



    /*
     * In eml2, the inline data wouldn't store in db, it store in file system
     * The db stores file name(without path). We got the old file name from db
     * and compare to the new in line data file
     */
    public boolean compareInlineDataFiles(String oldFileName, String newFileName)
            throws McdbException
    {
        // this method need to be testing
        boolean same = true;
        String data = null;
        try {
        	String path = PropertyService.getProperty("application.inlinedatafilepath");
			// the new file name will look like path/docid.rev.2
			File inlineDataDirectory = new File(path);
			File oldDataFile = new File(inlineDataDirectory, oldFileName);
			File newDataFile = new File(inlineDataDirectory, newFileName);

            Reader oldFileReader = new InputStreamReader(new FileInputStream(oldDataFile), encoding);
            BufferedReader oldStringReader = new BufferedReader(oldFileReader);
            Reader newFileReader = new InputStreamReader(new FileInputStream(newDataFile), encoding);
            BufferedReader newStringReader = new BufferedReader(newFileReader);
            // read first line of data
            String oldString = oldStringReader.readLine();
            String newString = newStringReader.readLine();

            // at the end oldstring will be null
            while (oldString != null) {
                oldString = oldStringReader.readLine();
                newString = newStringReader.readLine();
                if (!oldString.equals(newString)) {
                    same = false;
                    break;
                }
            }

            // if oldString is null but newString is not null, they are same
            if (same) {
                if (newString != null) {
                    same = false;
                }
            }

        } catch (Exception e) {
            throw new McdbException(e.getMessage());
        }
        logMetacat.info("the inline data retrieve from file: " + data);
        return same;
    }

   /*
	 * Copy a old line file to a new inline file
	 */
	public void copyInlineFile(String inlineDistributionId, String newFileName)
			throws SAXException {
		if (inlineDistributionId == null || newFileName == null) {
			throw new SAXException("Could not copy inline file from old one to new "
					+ "one!");
		}
		// get old file id from previousUnreadable data object
		String oldInlineInternalFileName = (String) previousUnreadableInlineDataObjectHash
				.get(inlineDistributionId);

		if (oldInlineInternalFileName == null
				|| oldInlineInternalFileName.trim().equals("")) {
			throw new SAXException("Could not copy inline file from old one to new "
					+ "one because can't find old file name");
		}
		logMetacat.info("in handle inline data");
		logMetacat.info("the inline data file name from xml_access is: "
				+ oldInlineInternalFileName);

		InputStream oldFileReader = null;
		OutputStream newFileWriter = null;
		try {
			String path = PropertyService.getProperty("application.inlinedatafilepath");
			// the new file name will look like path/docid.rev.2
			File inlineDataDirectory = new File(path);
			File oldDataFile = new File(inlineDataDirectory, oldInlineInternalFileName);
			File newDataFile = new File(inlineDataDirectory, newFileName);

			oldFileReader = new FileInputStream(oldDataFile);
			newFileWriter = new FileOutputStream(newDataFile);
			byte[] buf = new byte[4 * 1024]; // 4K buffer
			int b = oldFileReader.read(buf);
			while (b != -1) {
				newFileWriter.write(buf, 0, b);
				b = oldFileReader.read(buf);
			}
		} catch (Exception e) {
			throw new SAXException(e.getMessage());
		} finally {
			if (oldFileReader != null) {
				try {
					oldFileReader.close();
				} catch (Exception ee) {
					throw new SAXException(ee.getMessage());
				}

			}
			if (newFileWriter != null) {
				try {
					newFileWriter.close();
				} catch (Exception ee) {
					throw new SAXException(ee.getMessage());
				}

			}
		}
	}


    // if xml file failed to upload, we need to call this method to delete
    // the inline data already in file system
    public void deleteInlineFiles() throws SAXException
    {
        if (!inlineFileIDList.isEmpty()) {
            for (int i = 0; i < inlineFileIDList.size(); i++) {
                String fileName = (String) inlineFileIDList.elementAt(i);
                deleteInlineDataFile(fileName);
            }
        }
    }

    /* delete the inline data file */
    private void deleteInlineDataFile(String fileName) throws SAXException
    {
    	String path;
    	try {
    		path = PropertyService.getProperty("application.inlinedatafilepath");
    	} catch (PropertyNotFoundException pnfe) {
    		throw new SAXException ("Could not find inline data file path: " 
    				+ pnfe.getMessage());
    	}
        File inlineDataDirectory = new File(path);
        File newFile = new File(inlineDataDirectory, fileName);
        newFile.delete();

    }

    /*
	 * In eml2, the inline data wouldn't store in db, it store in file system
	 * The db stores file name(without path).
	 */
	public static Reader readInlineDataFromFileSystem(String fileName, String encoding)
			throws McdbException {
		// BufferedReader stringReader = null;
		Reader fileReader = null;
		try {
			String path = PropertyService.getProperty("application.inlinedatafilepath");
			// the new file name will look like path/docid.rev.2
			File inlineDataDirectory = new File(path);
			File dataFile = new File(inlineDataDirectory, fileName);

			fileReader = new InputStreamReader(new FileInputStream(dataFile), encoding);
			// stringReader = new BufferedReader(fileReader);
		} catch (Exception e) {
			throw new McdbException(e.getMessage());
		}
		// return stringReader;
		return fileReader;
	}

    /* Delete relations */
    private void deleteRelations() throws SAXException
    {
        PreparedStatement pStmt = null;
        String sql = "DELETE FROM xml_relation where docid =?";
        try {
            pStmt = connection.prepareStatement(sql);
            //bind variable
            pStmt.setString(1, docid);
            //execute query
            logMetacat.trace("Eml200SAXHandler.deleteRelations - executing SQL: " + pStmt.toString());
            pStmt.execute();
            pStmt.close();
        }//try
        catch (SQLException e) {
            throw new SAXException("EMLSAXHandler.deleteRelations(): "
                    + e.getMessage());
        }//catch
        finally {
            try {
                pStmt.close();
            }//try
            catch (SQLException ee) {
                throw new SAXException("EMLSAXHandler.deleteRelations: "
                        + ee.getMessage());
            }//catch
        }//finally
    }

    /* Write an online data file id into xml_relation table. The dataId shouldnot
     * have the revision
     */
    private void writeOnlineDataFileIdIntoRelationTable(String dataId)
            throws SAXException
    {
        PreparedStatement pStmt = null;
        String sql = "INSERT into xml_relation (docid, packagetype, subject, "
                + "relationship, object) values (?, ?, ?, ?, ?)";
        try {
            pStmt = connection.prepareStatement(sql);
            //bind variable
            pStmt.setString(1, docid);
            pStmt.setString(2, doctype);
            pStmt.setString(3, docid);
            pStmt.setString(4, RELATION);
            pStmt.setString(5, dataId);
            //execute query
            logMetacat.trace("Eml200SAXHandler.writeOnlineDataFileIdIntoRelationTable - executing SQL: " + pStmt.toString());
            pStmt.execute();
            pStmt.close();
        }//try
        catch (SQLException e) {
            throw new SAXException(
                    "EMLSAXHandler.writeOnlineDataFileIdIntoRelationTable(): "
                            + e.getMessage());
        }//catch
        finally {
            try {
                pStmt.close();
            }//try
            catch (SQLException ee) {
                throw new SAXException(
                        "EMLSAXHandler.writeOnlineDataFileIdIntoRelationTable(): "
                                + ee.getMessage());
            }//catch
        }//finally

    }//writeOnlineDataFileIdIntoRelationTable

    /*
     * This method will handle data file in online url. If the data file is in
     * ecogrid protocol, then the datafile identifier (guid) be returned.
     * otherwise, null will be returned.
     * If the data file doesn't exsit in xml_documents or
     * xml_revision table, or the user has all permission to the data file if
     * the docid already existed, the data file id (guid) will be returned
     * NEED to do:
     * We should also need to implement http and ftp. Those
     * external files should be download and assign a data file id to it.
     */
    private String handleOnlineUrlDataFile(String url) throws SAXException
    {
      logMetacat.warn("The url is "+ url);
      String docid = null;
      String guid = null;

      // if the url is not a ecogrid protocol, null will be getten
      String accessionNumber =
    	  DocumentUtil.getAccessionNumberFromEcogridIdentifier(url);
      
      // check for valid docid.rev
      int rev = 0;
      if (accessionNumber != null) {
			// get rid of revision number to get the docid.
			try {
				docid = DocumentUtil.getDocIdFromAccessionNumber(accessionNumber);
				rev = DocumentUtil.getRevisionFromAccessionNumber(accessionNumber);
			} catch (Exception e) {
				logMetacat.warn(e.getClass().getName() + " - Problem parsing accession number for: " + accessionNumber + ". Message: " + e.getMessage());
				accessionNumber = null;
			}
		}
      
      if (accessionNumber != null)
      {
		try {
			guid = IdentifierManager.getInstance().getGUID(docid, rev);
		} catch (McdbDocNotFoundException e1) {
			guid = docid + "." + rev;
			IdentifierManager.getInstance().createMapping(guid, guid);
		}
        onlineDataFileIdInRelationVector.add(docid);
        try
        {

          if (!AccessionNumber.accNumberUsed(docid))
          {
            return guid;
          }
          // check the previous revision if we have it
          int previousRevision = rev;
          Vector<Integer> revisions = DBUtil.getRevListFromRevisionTable(docid);
          if (revisions != null && revisions.size() > 0) {
        	  previousRevision = revisions.get(revisions.size() - 1);
          }
          String previousDocid = 
        	  docid + PropertyService.getProperty("document.accNumSeparator") + previousRevision;
          PermissionController controller = new PermissionController(previousDocid);
          if(writeAccessRules) {
              if (controller.hasPermission(user, groups, AccessControlInterface.ALLSTRING)
        		  || controller.hasPermission(user, groups, AccessControlInterface.CHMODSTRING)) {
                  return guid;
              }
              else
              {
                  throw new SAXException("User: " + user + " does not have permission to update " +
                  "access rules for data file "+ guid);
              }
          }
        }//try
        catch(Exception e)
        {
          logMetacat.error("Error in " +
                                "Eml200SAXHanlder.handleOnlineUrlDataFile is " +
                                 e.getMessage());
          throw new SAXException(e.getMessage());
        }
      }
      return guid;
    }

    private void compareElementNameSpaceAttributes(Stack unchangableNodeStack,
            Hashtable nameSpaces, Attributes attributes, String localName,
            String error) throws SAXException
    {
        //Get element subtree node stack (element node)
        NodeRecord elementNode = null;
        try {
            elementNode = (NodeRecord) unchangableNodeStack.pop();
        } catch (EmptyStackException ee) {
            logMetacat.error("Node stack is empty for element data");
            throw new SAXException(error);
        }
        logMetacat.info("current node type from xml is ELEMENT");
        logMetacat.info("node type from stack: "
                + elementNode.getNodeType());
        logMetacat.info("node name from xml document: " + localName);
        logMetacat.info("node name from stack: "
                + elementNode.getNodeName());
        logMetacat.info("node data from stack: "
                + elementNode.getNodeData());
        logMetacat.info("node id is: " + elementNode.getNodeId());
        // if this node is not element or local name not equal or name space
        // not
        // equals, throw an exception
        if (!elementNode.getNodeType().equals("ELEMENT")
                || !localName.equals(elementNode.getNodeName()))
        //  (uri != null && !uri.equals(elementNode.getNodePrefix())))
        {
            logMetacat.info("Inconsistence happend: ");
            logMetacat.info("current node type from xml is ELEMENT");
            logMetacat.info("node type from stack: "
                    + elementNode.getNodeType());
            logMetacat.info("node name from xml document: "
                    + localName);
            logMetacat.info("node name from stack: "
                    + elementNode.getNodeName());
            logMetacat.info("node data from stack: "
                    + elementNode.getNodeData());
            logMetacat.info("node id is: " + elementNode.getNodeId());
            throw new SAXException(error);
        }

        //compare namespace
        Enumeration nameEn = nameSpaces.keys();
        while (nameEn.hasMoreElements()) {
            //Get namespacke node stack (element node)
            NodeRecord nameNode = null;
            try {
                nameNode = (NodeRecord) unchangableNodeStack.pop();
            } catch (EmptyStackException ee) {
                logMetacat.error(
                        "Node stack is empty for namespace data");
                throw new SAXException(error);
            }

            String prefixName = (String) nameEn.nextElement();
            String nameSpaceUri = (String) nameSpaces.get(prefixName);
            if (!nameNode.getNodeType().equals("NAMESPACE")
                    || !prefixName.equals(nameNode.getNodeName())
                    || !nameSpaceUri.equals(nameNode.getNodeData())) {
                logMetacat.info("Inconsistence happend: ");
                logMetacat.info(
                        "current node type from xml is NAMESPACE");
                logMetacat.info("node type from stack: "
                        + nameNode.getNodeType());
                logMetacat.info("current node name from xml is: "
                        + prefixName);
                logMetacat.info("node name from stack: "
                        + nameNode.getNodeName());
                logMetacat.info("current node data from xml is: "
                        + nameSpaceUri);
                logMetacat.info("node data from stack: "
                        + nameNode.getNodeData());
                logMetacat.info("node id is: " + nameNode.getNodeId());
                throw new SAXException(error);
            }

        }//while

        //compare attributes
        for (int i = 0; i < attributes.getLength(); i++) {
            NodeRecord attriNode = null;
            try {
                attriNode = (NodeRecord) unchangableNodeStack.pop();

            } catch (EmptyStackException ee) {
                logMetacat.error(
                        "Node stack is empty for attribute data");
                throw new SAXException(error);
            }
            String attributeName = attributes.getQName(i);
            String attributeValue = attributes.getValue(i);
            logMetacat.info(
                    "current node type from xml is ATTRIBUTE ");
            logMetacat.info("node type from stack: "
                    + attriNode.getNodeType());
            logMetacat.info("current node name from xml is: "
                    + attributeName);
            logMetacat.info("node name from stack: "
                    + attriNode.getNodeName());
            logMetacat.info("current node data from xml is: "
                    + attributeValue);
            logMetacat.info("node data from stack: "
                    + attriNode.getNodeData());
            logMetacat.info("node id  is: " + attriNode.getNodeId());

            if (!attriNode.getNodeType().equals("ATTRIBUTE")
                    || !attributeName.equals(attriNode.getNodeName())
                    || !attributeValue.equals(attriNode.getNodeData())) {
                logMetacat.info("Inconsistence happend: ");
                logMetacat.info(
                        "current node type from xml is ATTRIBUTE ");
                logMetacat.info("node type from stack: "
                        + attriNode.getNodeType());
                logMetacat.info("current node name from xml is: "
                        + attributeName);
                logMetacat.info("node name from stack: "
                        + attriNode.getNodeName());
                logMetacat.info("current node data from xml is: "
                        + attributeValue);
                logMetacat.info("node data from stack: "
                        + attriNode.getNodeData());
                logMetacat.info("node is: " + attriNode.getNodeId());
                throw new SAXException(error);
            }
        }//for

    }

    /* mehtod to compare current text node and node in db */
    private void compareTextNode(Stack nodeStack, StringBuffer text,
            String error) throws SAXException
    {
        NodeRecord node = null;
        //get node from current stack
        try {
            node = (NodeRecord) nodeStack.pop();
        } catch (EmptyStackException ee) {
            logMetacat.error(
                    "Node stack is empty for text data in startElement");
            throw new SAXException(error);
        }
        logMetacat.info(
                "current node type from xml is TEXT in start element");
        logMetacat.info("node type from stack: " + node.getNodeType());
        logMetacat.info("current node data from xml is: "
                + text.toString());
        logMetacat.info("node data from stack: " + node.getNodeData());
        logMetacat.info("node name from stack: " + node.getNodeName());
        logMetacat.info("node is: " + node.getNodeId());
        if (!node.getNodeType().equals("TEXT")
                || !(text.toString()).equals(node.getNodeData())) {
            logMetacat.info("Inconsistence happend: ");
            logMetacat.info(
                    "current node type from xml is TEXT in start element");
            logMetacat.info("node type from stack: "
                    + node.getNodeType());
            logMetacat.info("current node data from xml is: "
                    + text.toString());
            logMetacat.info("node data from stack: "
                    + node.getNodeData());
            logMetacat.info("node name from stack: "
                    + node.getNodeName());
            logMetacat.info("node is: " + node.getNodeId());
            throw new SAXException(error);
        }//if
    }

    /* Comparet comment from xml and db */
    private void compareCommentNode(Stack nodeStack, String string, String error)
            throws SAXException
    {
        NodeRecord node = null;
        try {
            node = (NodeRecord) nodeStack.pop();
        } catch (EmptyStackException ee) {
            logMetacat.error("the stack is empty for comment data");
            throw new SAXException(error);
        }
        logMetacat.info("current node type from xml is COMMENT");
        logMetacat.info("node type from stack: " + node.getNodeType());
        logMetacat.info("current node data from xml is: " + string);
        logMetacat.info("node data from stack: " + node.getNodeData());
        logMetacat.info("node is from stack: " + node.getNodeId());
        // if not consistent terminate program and throw a exception
        if (!node.getNodeType().equals("COMMENT")
                || !string.equals(node.getNodeData())) {
            logMetacat.info("Inconsistence happend: ");
            logMetacat.info("current node type from xml is COMMENT");
            logMetacat.info("node type from stack: "
                    + node.getNodeType());
            logMetacat.info(
                    "current node data from xml is: " + string);
            logMetacat.info("node data from stack: "
                    + node.getNodeData());
            logMetacat.info("node is from stack: " + node.getNodeId());
            throw new SAXException(error);
        }//if
    }

    /* Compare whitespace from xml and db */
   private void compareWhiteSpace(Stack nodeStack, String string, String error)
           throws SAXException
   {
       NodeRecord node = null;
       try {
           node = (NodeRecord) nodeStack.pop();
       } catch (EmptyStackException ee) {
           logMetacat.error("the stack is empty for whitespace data");
           throw new SAXException(error);
       }
       if (!node.getNodeType().equals("TEXT")
               || !string.equals(node.getNodeData())) {
           logMetacat.info("Inconsistence happend: ");
           logMetacat.info(
                   "current node type from xml is WHITESPACE TEXT");
           logMetacat.info("node type from stack: "
                   + node.getNodeType());
           logMetacat.info(
                   "current node data from xml is: " + string);
           logMetacat.info("node data from stack: "
                   + node.getNodeData());
           logMetacat.info("node is from stack: " + node.getNodeId());
           throw new SAXException(error);
       }//if
   }



}
