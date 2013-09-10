/**
 *  '$RCSfile$'
 *    Purpose: A Class that handles the SAX XML events as they
 *             are generated from XML documents
 *  Copyright: 2000 Regents of the University of California and the
 *             National Center for Ecological Analysis and Synthesis
 *    Authors: Matt Jones, Jivka Bojilova
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
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
import edu.ucsb.nceas.metacat.util.DocumentUtil;
import edu.ucsb.nceas.metacat.util.MetacatUtil;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;

/**
 * A database aware Class implementing callback methods for the SAX parser to
 * call when processing the XML stream and generating events
 */
public class Eml210SAXHandler extends DBSAXHandler implements AccessControlInterface {

	private boolean processingTopLevelAccess = false;

	private boolean processingAdditionalAccess = false;

	private boolean processingOtherAccess = false;

	private AccessSection accessObject = null;

	private AccessRule accessRule = null;

	// all access rules
	private Vector<AccessSection> accessObjectList = new Vector<AccessSection>(); 

	private Hashtable<String, AccessSection> topLevelAccessControlMap = new Hashtable<String, AccessSection>();

	// subtree access for additionalmetadata
	private Hashtable<String, AccessSection> additionalAccessControlMap = new Hashtable<String, AccessSection>();
	
	
	private Vector<Hashtable<String, AccessSection>> additionalAccessMapList = new Vector<Hashtable<String, AccessSection>>();
	
	// ids from additionalmetadata/describes
	private Vector<String> describesId = new Vector<String>(); 
	
	private Stack<SubTree> subTreeInfoStack = new Stack<SubTree>();

	private Vector<SubTree> subTreeList = new Vector<SubTree>();
	
	private boolean needToCheckAccessModule = false;

	private Vector<AccessSection> unChangeableAccessSubTreeVector = new Vector<AccessSection>();

	private Stack<NodeRecord> currentUnchangeableAccessModuleNodeStack = new Stack<NodeRecord>();

	private AccessSection topAccessSection;

	// we need an another stack to store the access node which we pull out just
	// from xml. If a reference happened, we can use the stack the compare nodes
	private Stack<NodeRecord> storedAccessNodeStack = new Stack<NodeRecord>();

	// vector stored the data file id which will be write into relation table
	private Vector<String> onlineDataFileIdInRelationVector = new Vector<String>();

	// vector stored the data file id which will be write top access rules to
	// access table
	private Vector<String> onlineDataFileIdInTopAccessVector = new Vector<String>();

	// Indicator of inline data
	private boolean handleInlineData = false;

	private Hashtable<String, String> inlineDataNameSpace = null;

	private Writer inlineDataFileWriter = null;

	private String inlineDataFileName = null;

	DistributionSection currentDistributionSection = null;

	Vector<DistributionSection> allDistributionSections = new Vector<DistributionSection>();

	// This variable keeps a counter of each distribution element. This index
	// will be used to name the inline data file that gets written to disk, and to
	// strip inline data from the metadata document on file if the user does not have
	// read access.
	private int distributionIndex = 0;

	// private String distributionOnlineFileName = null;

	// This is used to delete inline files if the xml does not parse correctly.
	private Vector<String> inlineFileIdList = new Vector<String>();

	// Constant
	private static final String EML = "eml";

	private static final String DISTRIBUTION = "distribution";

	private static final String ORDER = "order";

	private static final String ID = "id";

	private static final String REFERENCES = "references";

	public static final String INLINE = "inline";

	private static final String ONLINE = "online";

	private static final String URL = "url";

	// private static final String PERMISSIONERROR = "User tried to update a
	// subtree "
	// + "when they don't have write permission!";

	private static final String UPDATEACCESSERROR = "User tried to update an "
			+ "access module when they don't have \"ALL\" permission!";

	private static final String TOPLEVEL = "top";

	private static final String SUBTREELEVEL = "subtree";

	private static final String RELATION = "Provides info for";

	private Logger logMetacat = Logger.getLogger(Eml210SAXHandler.class);

	/**
	 * Construct an instance of the handler class In this constructor, user can
	 * specify the version need to update
	 * 
	 * @param conn
	 *            the JDBC connection to which information is written
	 * @param action -
	 *            "INSERT" or "UPDATE"
	 * @param docid
	 *            to be inserted or updated into JDBC connection
	 * @param revision,
	 *            the user specified the revision need to be update
	 * @param user
	 *            the user connected to MetaCat servlet and owns the document
	 * @param groups
	 *            the groups to which user belongs
	 * @param pub
	 *            flag for public "read" access on document
	 * @param serverCode
	 *            the serverid from xml_replication on which this document
	 *            resides.
	 * 
	 */
	public Eml210SAXHandler(DBConnection conn, String action, String docid,
			String revision, String user, String[] groups, String pub, int serverCode,
			Date createDate, Date updateDate, boolean writeAccessRules) throws SAXException {
		super(conn, action, docid, revision, user, groups, pub, serverCode, createDate,
				updateDate, writeAccessRules);
		// Get the unchangeable subtrees (user doesn't have write permission)
		try {

			if (action.equals("UPDATE")) {
				// If the action is update and user doesn't have "ALL" permission
				// we need to check if user can update access subtree			
				int latestRevision = DBUtil.getLatestRevisionInDocumentTable(docid);
				String previousDocid = 
					docid + PropertyService.getProperty("document.accNumSeparator") + latestRevision;
				
				PermissionController control = new PermissionController(previousDocid );
				if (!control.hasPermission(user, groups, AccessControlInterface.ALLSTRING)
						&& action != null) {
					needToCheckAccessModule = true;
					unChangeableAccessSubTreeVector = getAccessSubTreeListFromDB();
				}
			}

		} catch (Exception e) {
			throw new SAXException(e.getMessage());
		}
	}

	/*
	 * Get the subtree node info from xml_accesssubtree table
	 */
	private Vector<AccessSection> getAccessSubTreeListFromDB() throws Exception {
		Vector<AccessSection> result = new Vector<AccessSection>();
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		String sql = "SELECT controllevel, subtreeid, startnodeid, endnodeid "
				+ "FROM xml_accesssubtree WHERE docid like ? "
				+ "ORDER BY startnodeid ASC";

		try {

			pstmt = connection.prepareStatement(sql);
			// Increase DBConnection usage count
			connection.increaseUsageCount(1);
			// Bind the values to the query
			pstmt.setString(1, docid);
			pstmt.execute();

			// Get result set
			rs = pstmt.getResultSet();
			while (rs.next()) {
				String level = rs.getString(1);
				String sectionId = rs.getString(2);
				long startNodeId = rs.getLong(3);
				long endNodeId = rs.getLong(4);
				// create a new access section
				AccessSection accessObj = new AccessSection();
				accessObj.setControlLevel(level);
				accessObj.setDocId(docid);
				accessObj.setSubTreeId(sectionId);
				accessObj.setStartNodeId(startNodeId);
				accessObj.setEndNodeId(endNodeId);
				Stack<NodeRecord> nodeStack = accessObj.getSubTreeNodeStack();
				accessObj.setSubTreeNodeStack(nodeStack);
				// add this access obj into vector
				result.add(accessObj);
				// Get the top level access subtree control
				if (level != null && level.equals(TOPLEVEL)) {
					topAccessSection = accessObj;
				}
			}
			pstmt.close();
		}// try
		catch (SQLException e) {
			throw new SAXException("EMLSAXHandler.getAccessSubTreeListFromDB(): "
					+ e.getMessage());
		}// catch
		finally {
			try {
				pstmt.close();
			} catch (SQLException ee) {
				throw new SAXException("EMLSAXHandler.getAccessSubTreeListFromDB(): "
						+ ee.getMessage());
			}
		}// finally
		return result;
	}

	/** SAX Handler that is called at the start of each XML element */
	public void startElement(String uri, String localName, String qName, Attributes atts)
			throws SAXException {
		// for element <eml:eml...> qname is "eml:eml", local name is "eml"
		// for element <acl....> both qname and local name is "eml"
		// uri is namesapce
		logMetacat.debug("Start ELEMENT(qName) " + qName);
		logMetacat.debug("Start ELEMENT(localName) " + localName);
		logMetacat.debug("Start ELEMENT(uri) " + uri);

		DBSAXNode parentNode = null;
		DBSAXNode currentNode = null;

		if (!handleInlineData) {
			// Get a reference to the parent node for the id
			try {
				parentNode = (DBSAXNode) nodeStack.peek();
			} catch (EmptyStackException e) {
				parentNode = null;
			}

			// start handle inline data
			if (localName.equals(INLINE)) {
				handleInlineData = true;
				// initialize namespace hash for in line data
				inlineDataNameSpace = new Hashtable<String, String>();
				// initialize file writer
				String docidWithoutRev = DocumentUtil.getDocIdFromString(docid);
				String seperator = ".";
				try {
					seperator = PropertyService.getProperty("document.accNumSeparator");
				} catch (PropertyNotFoundException pnfe) {
					logMetacat.error("Could not fing property 'accNumSeparator'. "
							+ "Setting separator to '.': " + pnfe.getMessage());
				}
				// the new file name will look like docid.rev.2
				inlineDataFileName = docidWithoutRev + seperator + revision + seperator
						+ distributionIndex;
				inlineDataFileWriter = createInlineDataFileWriter(inlineDataFileName, encoding);
				// put the inline file id into a vector. If upload failed,
				// metacat will delete the inline data file
				inlineFileIdList.add(inlineDataFileName);
				
				currentDistributionSection.setDistributionType(DistributionSection.INLINE_DATA_DISTRIBUTION);
				currentDistributionSection.setDataFileName(inlineDataFileName);

			}

			// If hit a text node, we need write this text for current's parent
			// node This will happen if the element is mixed
			if (hitTextNode && parentNode != null) {

				// compare top level access module
				if (processingTopLevelAccess && needToCheckAccessModule) {
					compareAccessTextNode(currentUnchangeableAccessModuleNodeStack, textBuffer);
				}

				if (needToCheckAccessModule
						&& (processingAdditionalAccess || processingOtherAccess || processingTopLevelAccess)) {
					// stored the pull out nodes into storedNode stack
					NodeRecord nodeElement = new NodeRecord(-2, -2, -2, "TEXT", null,
							null, MetacatUtil.normalize(textBuffer.toString()));
					storedAccessNodeStack.push(nodeElement);

				}

				// write the textbuffer into db for parent node.
				endNodeId = writeTextForDBSAXNode(endNodeId, textBuffer, parentNode);
				// rest hitTextNode
				hitTextNode = false;
				// reset textbuffer
				textBuffer = null;
				textBuffer = new StringBuffer();

			}

			// Document representation that points to the root document node
			if (atFirstElement) {
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
					logMetacat.debug("DOCNAME-a: " + docname);
					logMetacat.debug("DOCTYPE-a: " + doctype);
				} else if (doctype == null) {
					// because docname is not null and it is declared in dtd
					// so could not be in schema, no namespace
					doctype = docname;
					logMetacat.debug("DOCTYPE-b: " + doctype);
				}
				rootNode.writeNodename(docname);
				try {
					// for validated XML Documents store a reference to XML DB
					// Catalog. Because this is select statement and it needn't
					// roll back if insert document action failed. In order to
					// decrease DBConnection usage count, we get a new
					// dbconnection from pool String catalogid = null;
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
					}// try
					finally {
						// Return dbconnection
						DBConnectionPool.returnDBConnection(dbConn, serialNumber);
					}// finally

					// create documentImpl object by the constructor which can
					// specify the revision
					if (!super.getIsRevisionDoc()) {
						currentDocument = new DocumentImpl(connection, rootNode
								.getNodeID(), docname, doctype, docid, revision, action,
								user, this.pub, catalogid, this.serverCode, createDate,
								updateDate);
					}

				} catch (McdbDocNotFoundException mdnfe) {
					Vector<Integer> revList = null;
					
					try {
						revList = DBUtil.getRevListFromRevisionTable(docid);
					} catch (SQLException sqle) {
						logMetacat.error("SQL error when trying to get rev list for doc " + docid + " : " + sqle.getMessage());
						throw (new SAXException("Doc ID " + docid + " was not found and cannot be updated.")); 
					}
					
					if (revList.size() > 0) {
						throw (new SAXException("EML210SaxHandler.startElement - Doc ID " + docid + " was deleted and cannot be updated."));
					} else {
						throw (new SAXException("EML210SaxHandler.startElement - Doc ID " + docid + " was not found and cannot be updated.")); 
					}
				} catch (Exception e) {
                    throw (new SAXException("EML210SaxHandler.startElement - error with action " + 
                    		action + " : " + e.getMessage()));
				}
			}

			// Create the current node representation
			currentNode = new DBSAXNode(connection, qName, localName, parentNode,
					rootNode.getNodeID(), docid, doctype);
			// Use a local variable to store the element node id
			// If this element is a start point of subtree(section), it will be
			// stored otherwise, it will be discarded
			long startNodeId = currentNode.getNodeID();
			// Add all of the namespaces
			String prefix = null;
			String nsuri = null;
			Enumeration<String> prefixes = namespaces.keys();
			while (prefixes.hasMoreElements()) {
				prefix = prefixes.nextElement();
				nsuri = namespaces.get(prefix);
				endNodeId = currentNode.setNamespace(prefix, nsuri, docid);
			}

			// Add all of the attributes
			for (int i = 0; i < atts.getLength(); i++) {
				String attributeName = atts.getQName(i);
				String attributeValue = atts.getValue(i);
				endNodeId = currentNode
						.setAttribute(attributeName, attributeValue, docid);

				// To handle name space and schema location if the attribute
				// name is xsi:schemaLocation. If the name space is in not
				// in catalog table it will be registered.
				if (attributeName != null
						&& attributeName.indexOf(MetaCatServlet.SCHEMALOCATIONKEYWORD) != -1) {
					SchemaLocationResolver resolver = new SchemaLocationResolver(
							attributeValue);
					resolver.resolveNameSpace();

				} else if (attributeName != null && attributeName.equals(ID)) {

				}
			}// for

			// handle access stuff
			if (localName.equals(ACCESS)) {
				if (parentNode.getTagName().equals(EML)) {
					processingTopLevelAccess = true;
				} else if (parentNode.getTagName() == DISTRIBUTION) {
					processingAdditionalAccess = true;
				} else {
					// process other access embedded into resource level
					// module
					processingOtherAccess = true;
				}
				// create access object
				accessObject = new AccessSection();
				// set permission order
				String permOrder = currentNode.getAttribute(ORDER);
				accessObject.setPermissionOrder(permOrder);
				// set access id
				String accessId = currentNode.getAttribute(ID);
				accessObject.setSubTreeId(accessId);
				accessObject.setStartNodeId(startNodeId);
				accessObject.setDocId(docid);
				if (processingAdditionalAccess) {
					accessObject.setDataFileName(inlineDataFileName);
				}

				// load top level node stack to
				// currentUnchangableAccessModuleNodeStack
				if (processingTopLevelAccess && needToCheckAccessModule) {
					// get the node stack for
					currentUnchangeableAccessModuleNodeStack = topAccessSection
							.getSubTreeNodeStack();
				}
			} else if (localName.equals(DISTRIBUTION)) {
				distributionIndex++;
				currentDistributionSection = new DistributionSection(distributionIndex);

				// handle subtree info
				SubTree subTree = new SubTree();
				// set sub tree id
				subTree.setSubTreeId(String.valueOf(distributionIndex));
				// set sub tree start element name
				subTree.setStartElementName(currentNode.getTagName());
				// set start node number
				subTree.setStartNodeId(startNodeId);
				// add to stack, but it didn't get end node id yet
				subTreeInfoStack.push(subTree);
			}
			// Set up a access rule for allow
			else if (parentNode.getTagName() != null
					&& (parentNode.getTagName()).equals(ACCESS)
					&& localName.equals(ALLOW)) {

				accessRule = new AccessRule();

				// set permission type "allow"
				accessRule.setPermissionType(ALLOW);

			}
			// set up an access rule for deny
			else if (parentNode.getTagName() != null
					&& (parentNode.getTagName()).equals(ACCESS) && localName.equals(DENY)) {
				accessRule = new AccessRule();
				// set permission type "allow"
				accessRule.setPermissionType(DENY);
			}

			// Add the node to the stack, so that any text data can be
			// added as it is encountered
			nodeStack.push(currentNode);
			// Add the node to the vector used by thread for writing XML Index
			nodeIndex.addElement(currentNode);

			// compare top access level module
			if (processingTopLevelAccess && needToCheckAccessModule) {
				compareElementNameSpaceAttributes(
						currentUnchangeableAccessModuleNodeStack, namespaces, atts,
						localName, UPDATEACCESSERROR);

			}

			// store access module element and attributes into stored stack
			if (needToCheckAccessModule
					&& (processingAdditionalAccess || processingOtherAccess || processingTopLevelAccess)) {
				// stored the pull out nodes into storedNode stack
				NodeRecord nodeElement = new NodeRecord(-2, -2, -2, "ELEMENT", localName,
						prefix, MetacatUtil.normalize(null));
				storedAccessNodeStack.push(nodeElement);
				for (int i = 0; i < atts.getLength(); i++) {
					String attributeName = atts.getQName(i);
					String attributeValue = atts.getValue(i);
					NodeRecord nodeAttribute = new NodeRecord(-2, -2, -2, "ATTRIBUTE",
							attributeName, null, MetacatUtil.normalize(attributeValue));
					storedAccessNodeStack.push(nodeAttribute);
				}

			}

			// reset name space
			namespaces = null;
			namespaces = new Hashtable<String, String>();
		}// not inline data
		else {
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
			Enumeration<String> prefixes = inlineDataNameSpace.keys();
			while (prefixes.hasMoreElements()) {
				prefix = prefixes.nextElement();
				nsuri = namespaces.get(prefix);
				inlineElements.append(" ");
				inlineElements.append("xmlns:");
				inlineElements.append(prefix);
				inlineElements.append("=\"");
				inlineElements.append(nsuri);
				inlineElements.append("\"");
			}
			inlineElements.append(">");
			// reset inline data name space
			inlineDataNameSpace = null;
			inlineDataNameSpace = new Hashtable<String, String>();
			// write inline data into file
			logMetacat.debug("the inline element data is: " + inlineElements.toString());
			writeInlineDataIntoFile(inlineDataFileWriter, inlineElements);
		}// else

	}

	private void compareElementNameSpaceAttributes(
			Stack<NodeRecord> unchangeableNodeStack,
			Hashtable<String, String> nameSpaces, Attributes attributes,
			String localName, String error) throws SAXException {
		// Get element subtree node stack (element node)
		NodeRecord elementNode = null;
		try {
			elementNode = unchangeableNodeStack.pop();
		} catch (EmptyStackException ee) {
			logMetacat.error("Node stack is empty for element data");
			throw new SAXException(error);
		}
		logMetacat.debug("current node type from xml is ELEMENT");
		logMetacat.debug("node type from stack: " + elementNode.getNodeType());
		logMetacat.debug("node name from xml document: " + localName);
		logMetacat.debug("node name from stack: " + elementNode.getNodeName());
		logMetacat.debug("node data from stack: " + elementNode.getNodeData());
		logMetacat.debug("node id is: " + elementNode.getNodeId());
		// if this node is not element or local name not equal or name space
		// not equals, throw an exception
		if (!elementNode.getNodeType().equals("ELEMENT")
				|| !localName.equals(elementNode.getNodeName()))
		// (uri != null && !uri.equals(elementNode.getNodePrefix())))
		{
			logMetacat.error("Inconsistence happened: ");
			logMetacat.error("current node type from xml is ELEMENT");
			logMetacat.error("node type from stack: " + elementNode.getNodeType());
			logMetacat.error("node name from xml document: " + localName);
			logMetacat.error("node name from stack: " + elementNode.getNodeName());
			logMetacat.error("node data from stack: " + elementNode.getNodeData());
			logMetacat.error("node id is: " + elementNode.getNodeId());
			throw new SAXException(error);
		}

		// compare namespace
		Enumeration<String> nameEn = nameSpaces.keys();
		while (nameEn.hasMoreElements()) {
			// Get namespacke node stack (element node)
			NodeRecord nameNode = null;
			try {
				nameNode = unchangeableNodeStack.pop();
			} catch (EmptyStackException ee) {
				logMetacat.error("Node stack is empty for namespace data");
				throw new SAXException(error);
			}

			String prefixName = nameEn.nextElement();
			String nameSpaceUri = nameSpaces.get(prefixName);
			if (!nameNode.getNodeType().equals("NAMESPACE")
					|| !prefixName.equals(nameNode.getNodeName())
					|| !nameSpaceUri.equals(nameNode.getNodeData())) {
				logMetacat.error("Inconsistence happened: ");
				logMetacat.error("current node type from xml is NAMESPACE");
				logMetacat.error("node type from stack: " + nameNode.getNodeType());
				logMetacat.error("current node name from xml is: " + prefixName);
				logMetacat.error("node name from stack: " + nameNode.getNodeName());
				logMetacat.error("current node data from xml is: " + nameSpaceUri);
				logMetacat.error("node data from stack: " + nameNode.getNodeData());
				logMetacat.error("node id is: " + nameNode.getNodeId());
				throw new SAXException(error);
			}

		}// while

		// compare attributes
		for (int i = 0; i < attributes.getLength(); i++) {
			NodeRecord attriNode = null;
			try {
				attriNode = unchangeableNodeStack.pop();

			} catch (EmptyStackException ee) {
				logMetacat.error("Node stack is empty for attribute data");
				throw new SAXException(error);
			}
			String attributeName = attributes.getQName(i);
			String attributeValue = attributes.getValue(i);
			logMetacat.debug("current node type from xml is ATTRIBUTE ");
			logMetacat.debug("node type from stack: " + attriNode.getNodeType());
			logMetacat.debug("current node name from xml is: " + attributeName);
			logMetacat.debug("node name from stack: " + attriNode.getNodeName());
			logMetacat.debug("current node data from xml is: " + attributeValue);
			logMetacat.debug("node data from stack: " + attriNode.getNodeData());
			logMetacat.debug("node id  is: " + attriNode.getNodeId());

			if (!attriNode.getNodeType().equals("ATTRIBUTE")
					|| !attributeName.equals(attriNode.getNodeName())
					|| !attributeValue.equals(attriNode.getNodeData())) {
				logMetacat.error("Inconsistence happened: ");
				logMetacat.error("current node type from xml is ATTRIBUTE ");
				logMetacat.error("node type from stack: " + attriNode.getNodeType());
				logMetacat.error("current node name from xml is: " + attributeName);
				logMetacat.error("node name from stack: " + attriNode.getNodeName());
				logMetacat.error("current node data from xml is: " + attributeValue);
				logMetacat.error("node data from stack: " + attriNode.getNodeData());
				logMetacat.error("node is: " + attriNode.getNodeId());
				throw new SAXException(error);
			}
		}// for

	}

	/* method to compare current text node and node in db */
	private void compareAccessTextNode(Stack<NodeRecord> nodeStack, StringBuffer text) throws SAXException {
		NodeRecord node = null;
		// get node from current stack
		try {
			node = nodeStack.pop();
		} catch (EmptyStackException ee) {
			logMetacat.error("Node stack is empty for text data in startElement for doc id " + docid);
			throw new SAXException("Access rules could not be found in database.");
		}

		String dbAccessData = node.getNodeData();
		String docAccessData = text.toString().trim();
		
		logMetacat.debug("Eml210SAXHandler.compareAccessTextNode - \n" +
					"\t access node type from db:       " + node.getNodeType() + "\n" +
					"\t access node data from db:       " + node.getNodeData() + "\n" +
					"\t access node data from document: " + text.toString());
		
		if (!node.getNodeType().equals("TEXT")
				|| !docAccessData.equals(dbAccessData)) {
			logMetacat.warn("Eml210SAXHandler.compareAccessTextNode - Access record mismatch: \n" +
					"\t access node type from db:       " + node.getNodeType() + "\n" +
					"\t access node data from db:       " + dbAccessData + "\n" +
					"\t access node data from document: " + docAccessData);
			
			throw new SAXException(UPDATEACCESSERROR + " [Eml210SAXHandler.compareAccessTextNode]");
		}// if
	}

	/** SAX Handler that is called for each XML text node */
	public void characters(char[] cbuf, int start, int len) throws SAXException {
		logMetacat.debug("CHARACTERS");
		if (!handleInlineData) {
			// buffer all text nodes for same element. This is for if text was
			// split into different nodes
			textBuffer.append(new String(cbuf, start, len));
			// set hittextnode true
			hitTextNode = true;
			// if text buffer .size is greater than max, write it to db.
			// so we can save memory
			if (textBuffer.length() > MAXDATACHARS) {
				logMetacat.debug("Write text into DB in charaters"
						+ " when text buffer size is greater than maxmum number");
				DBSAXNode currentNode = (DBSAXNode) nodeStack.peek();
				endNodeId = writeTextForDBSAXNode(endNodeId, textBuffer, currentNode);
				textBuffer = null;
				textBuffer = new StringBuffer();
			}
		} else {
			// this is inline data and write file system directly
			// we don't need to buffer it.
			StringBuffer inlineText = new StringBuffer();
			inlineText.append(new String(cbuf, start, len));
			logMetacat.debug("The inline text data write into file system: "
					+ inlineText.toString());
			writeInlineDataIntoFile(inlineDataFileWriter, inlineText);
		}
	}

	/** SAX Handler that is called at the end of each XML element */
	public void endElement(String uri, String localName, String qName)
			throws SAXException {
		logMetacat.debug("End ELEMENT " + qName);

		if (localName.equals(INLINE) && handleInlineData) {
			// Get the node from the stack
			DBSAXNode currentNode = (DBSAXNode) nodeStack.pop();
			logMetacat.debug("End of inline data");
			// close file writer
			try {
				inlineDataFileWriter.close();
				handleInlineData = false;
			} catch (IOException ioe) {
				throw new SAXException(ioe.getMessage());
			}

			// write put inline data file name into text buffer (without path)
			textBuffer = new StringBuffer(inlineDataFileName);
			// write file name into db
			endNodeId = writeTextForDBSAXNode(endNodeId, textBuffer, currentNode);
			// reset textbuff
			textBuffer = null;
			textBuffer = new StringBuffer();
			return;
		}

		if (!handleInlineData) {
			// Get the node from the stack
			DBSAXNode currentNode = (DBSAXNode) nodeStack.pop();
			String currentTag = currentNode.getTagName();

			// If before the end element, the parser hit text nodes and store
			// them into the buffer, write the buffer to data base. The reason
			// we put write database here is for xerces some time split text
			// node
			if (hitTextNode) {
				// get access value
				String data = null;
				// add principal
				if (currentTag.equals(PRINCIPAL) && accessRule != null) {
					data = (textBuffer.toString()).trim();
					accessRule.addPrincipal(data);

				} else if (currentTag.equals(PERMISSION) && accessRule != null) {
					data = (textBuffer.toString()).trim();
					// we combine different a permission into one value
					int permission = accessRule.getPermission();
					// add permission
					if (data.toUpperCase().equals(READSTRING)) {
						permission = permission | READ;
					} else if (data.toUpperCase().equals(WRITESTRING)) {
						permission = permission | WRITE;
					} else if (data.toUpperCase().equals(CHMODSTRING)) {
						permission = permission | CHMOD;
					} else if (data.toUpperCase().equals(ALLSTRING)) {
						permission = permission | ALL;
					}
					accessRule.setPermission(permission);
				} else if (currentTag.equals(REFERENCES)
						&& (processingTopLevelAccess || processingAdditionalAccess || processingOtherAccess)) {
					// get reference
					data = (textBuffer.toString()).trim();
					// put reference id into accessSection
					accessObject.setReferences(data);

				} else if (currentTag.equals(URL)) {
					// handle online data, make sure its'parent is online
					DBSAXNode parentNode = (DBSAXNode) nodeStack.peek();
					if (parentNode != null && parentNode.getTagName() != null
							&& parentNode.getTagName().equals(ONLINE)) {
						// if online data is in local metacat, add it to the
						// vector
						data = (textBuffer.toString()).trim();
						handleOnlineUrlDataFile(data);

					}// if
				}// else if
				
				// write text to db if it is not inline data
				logMetacat.debug("Write text into DB in End Element");

				// compare top level access module
				if (processingTopLevelAccess && needToCheckAccessModule) {
					compareAccessTextNode(currentUnchangeableAccessModuleNodeStack,
							textBuffer);
				}
				// write text node into db
				endNodeId = writeTextForDBSAXNode(endNodeId, textBuffer, currentNode);
			}
			
			if (needToCheckAccessModule
					&& (processingAdditionalAccess || processingOtherAccess || processingTopLevelAccess)) {
				// stored the pull out nodes into storedNode stack
				NodeRecord nodeElement = new NodeRecord(-2, -2, -2, "TEXT", null,
						null, MetacatUtil.normalize(textBuffer.toString()));
				storedAccessNodeStack.push(nodeElement);

			}

			// set hitText false
			hitTextNode = false;
			// reset textbuff
			textBuffer = null;
			textBuffer = new StringBuffer();

			// hand sub stree stuff
			if (!subTreeInfoStack.empty()) {
				SubTree tree = subTreeInfoStack.peek();// get last
				// subtree
				if (tree != null && tree.getStartElementName() != null
						&& (tree.getStartElementName()).equals(currentTag)) {
					// find the end of sub tree and set the end node id
					tree.setEndNodeId(endNodeId);
					// add the subtree into the final store palace
					subTreeList.add(tree);
					// get rid of it from stack
					subTreeInfoStack.pop();
				}// if
			}// if

			// access stuff
			if (currentTag.equals(ALLOW) || currentTag.equals(DENY)) {
				// finish parser access rule and assign it to new one
				AccessRule newRule = accessRule;
				// add the new rule to access section object
				accessObject.addAccessRule(newRule);
				// reset access rule
				accessRule = null;
			} else if (currentTag.equals(ACCESS)) {
				// finish parsing an access section and assign it to new one
				DBSAXNode parentNode = (DBSAXNode) nodeStack.peek();

				accessObject.setEndNodeId(endNodeId);

				if (parentNode != null && parentNode.getTagName() != null
						&& parentNode.getTagName().equals(DISTRIBUTION)) {
					describesId.add(String.valueOf(distributionIndex));
					currentDistributionSection.setAccessSection(accessObject);
				}

				AccessSection newAccessObject = accessObject;

				if (newAccessObject != null) {

					// add the accessSection into a vector to store it
					// if it is not a reference, need to store it
					if (newAccessObject.getReferences() == null) {

						newAccessObject.setStoredTmpNodeStack(storedAccessNodeStack);
						accessObjectList.add(newAccessObject);
					}
					if (processingTopLevelAccess) {

						// top level access control will handle whole document
						// -docid
						topLevelAccessControlMap.put(docid, newAccessObject);
						// reset processtopleveraccess tag

					}// if
					else if (processingAdditionalAccess) {
						// for additional control put everything in describes
						// value
						// and access object into hash
						for (int i = 0; i < describesId.size(); i++) {

							String subId = describesId.elementAt(i);
							if (subId != null) {
								additionalAccessControlMap.put(subId, newAccessObject);
							}// if
						}// for
						// add this hashtable in to vector

						additionalAccessMapList.add(additionalAccessControlMap);
						// reset this hashtable in order to store another
						// additional
						// accesscontrol
						additionalAccessControlMap = null;
						additionalAccessControlMap = new Hashtable<String, AccessSection>();
					}// if

				}// if
				// check if access node stack is empty after parsing top access
				// module

				if (needToCheckAccessModule && processingTopLevelAccess
						&& !currentUnchangeableAccessModuleNodeStack.isEmpty()) {

					logMetacat.error("Access node stack is not empty after "
							+ "parsing access subtree");
					throw new SAXException(UPDATEACCESSERROR);

				}
				// reset access section object

				accessObject = null;

				// reset tmp stored node stack
				storedAccessNodeStack = null;
				storedAccessNodeStack = new Stack<NodeRecord>();

				// reset flag
				processingAdditionalAccess = false;
				processingTopLevelAccess = false;
				processingOtherAccess = false;

			} else if (currentTag.equals(DISTRIBUTION)) {
				// If the current Distribution is inline or data and it doesn't have an access section
				// we use the top level access section (if it exists)
				if ((currentDistributionSection.getDistributionType() == DistributionSection.DATA_DISTRIBUTION 
						|| currentDistributionSection.getDistributionType() == DistributionSection.INLINE_DATA_DISTRIBUTION)
						&& currentDistributionSection.getAccessSection() == null
						&& topLevelAccessControlMap.size() > 0) {
					
					AccessSection accessSection = new AccessSection();
					accessSection.setDocId(docid);	
					AccessSection topLevelAccess = topLevelAccessControlMap.get(docid);
					accessSection.setPermissionOrder(topLevelAccess.getPermissionOrder());
					Vector<AccessRule> accessRuleList = topLevelAccess.getAccessRules();
					for (AccessRule accessRule : accessRuleList) {
						accessSection.addAccessRule(accessRule);
					}
					currentDistributionSection.setAccessSection(accessSection);
				} 
				if (currentDistributionSection.getAccessSection() != null) {
					currentDistributionSection.getAccessSection().setDataFileName(currentDistributionSection.getDataFileName());
				}
				allDistributionSections.add(currentDistributionSection);
				currentDistributionSection = null;
				describesId = null;
				describesId = new Vector<String>();
			}
		} else {
			// this is in inline part
			StringBuffer endElement = new StringBuffer();
			endElement.append("</");
			endElement.append(qName);
			endElement.append(">");
			logMetacat.debug("inline endElement: " + endElement.toString());
			writeInlineDataIntoFile(inlineDataFileWriter, endElement);
		}
	}

	/**
	 * SAX Handler that receives notification of comments in the DTD
	 */
	public void comment(char[] ch, int start, int length) throws SAXException {
		logMetacat.debug("COMMENT");
		if (!handleInlineData) {
			if (!processingDTD) {
				DBSAXNode currentNode = (DBSAXNode) nodeStack.peek();
				String str = new String(ch, start, length);

				// compare top level access module
				if (processingTopLevelAccess && needToCheckAccessModule) {
					compareCommentNode(currentUnchangeableAccessModuleNodeStack, str,
							UPDATEACCESSERROR);
				}
				endNodeId = currentNode.writeChildNodeToDB("COMMENT", null, str, docid);
				if (needToCheckAccessModule
						&& (processingAdditionalAccess || processingOtherAccess || processingTopLevelAccess)) {
					// stored the pull out nodes into storedNode stack
					NodeRecord nodeElement = new NodeRecord(-2, -2, -2, "COMMENT", null,
							null, MetacatUtil.normalize(str));
					storedAccessNodeStack.push(nodeElement);

				}
			}
		} else {
			// inline data comment
			StringBuffer inlineComment = new StringBuffer();
			inlineComment.append("<!--");
			inlineComment.append(new String(ch, start, length));
			inlineComment.append("-->");
			logMetacat.debug("inline data comment: " + inlineComment.toString());
			writeInlineDataIntoFile(inlineDataFileWriter, inlineComment);
		}
	}

	/* Compare comment from xml and db */
	private void compareCommentNode(Stack<NodeRecord> nodeStack, String string,
			String error) throws SAXException {
		NodeRecord node = null;
		try {
			node = nodeStack.pop();
		} catch (EmptyStackException ee) {
			logMetacat.error("the stack is empty for comment data");
			throw new SAXException(error);
		}
		logMetacat.debug("current node type from xml is COMMENT");
		logMetacat.debug("node type from stack: " + node.getNodeType());
		logMetacat.debug("current node data from xml is: " + string);
		logMetacat.debug("node data from stack: " + node.getNodeData());
		logMetacat.debug("node is from stack: " + node.getNodeId());
		// if not consistent terminate program and throw a exception
		if (!node.getNodeType().equals("COMMENT") || !string.equals(node.getNodeData())) {
			logMetacat.error("Inconsistence happened: ");
			logMetacat.error("current node type from xml is COMMENT");
			logMetacat.error("node type from stack: " + node.getNodeType());
			logMetacat.error("current node data from xml is: " + string);
			logMetacat.error("node data from stack: " + node.getNodeData());
			logMetacat.error("node is from stack: " + node.getNodeId());
			throw new SAXException(error);
		}// if
	}

	/**
	 * SAX Handler called once for each processing instruction found: node that
	 * PI may occur before or after the root element.
	 */
	public void processingInstruction(String target, String data) throws SAXException {
		logMetacat.debug("PI");
		if (!handleInlineData) {
			DBSAXNode currentNode = (DBSAXNode) nodeStack.peek();
			endNodeId = currentNode.writeChildNodeToDB("PI", target, data, docid);
		} else {
			StringBuffer inlinePI = new StringBuffer();
			inlinePI.append("<?");
			inlinePI.append(target);
			inlinePI.append(" ");
			inlinePI.append(data);
			inlinePI.append("?>");
			logMetacat.debug("inline data pi is: " + inlinePI.toString());
			writeInlineDataIntoFile(inlineDataFileWriter, inlinePI);
		}
	}

	/** SAX Handler that is called at the start of Namespace */
	public void startPrefixMapping(String prefix, String uri) throws SAXException {
		logMetacat.debug("NAMESPACE");
		if (!handleInlineData) {
			namespaces.put(prefix, uri);
		} else {
			inlineDataNameSpace.put(prefix, uri);
		}
	}

	/**
	 * SAX Handler that is called for each XML text node that is Ignorable white
	 * space
	 */
	public void ignorableWhitespace(char[] cbuf, int start, int len) throws SAXException {
		// When validation is turned "on", white spaces are reported here
		// When validation is turned "off" white spaces are not reported here,
		// but through characters() callback
		logMetacat.debug("IGNORABLEWHITESPACE");
		if (!handleInlineData) {
			DBSAXNode currentNode = (DBSAXNode) nodeStack.peek();
				String data = new String(cbuf, start, len);
				// compare whitespace in access top module
				if (processingTopLevelAccess && needToCheckAccessModule) {
					compareWhiteSpace(currentUnchangeableAccessModuleNodeStack, data,
							UPDATEACCESSERROR);
				}
				// Write the content of the node to the database
				if (needToCheckAccessModule
						&& (processingAdditionalAccess || processingOtherAccess || processingTopLevelAccess)) {
					// stored the pull out nodes into storedNode stack
					NodeRecord nodeElement = new NodeRecord(-2, -2, -2, "TEXT", null,
							null, MetacatUtil.normalize(data));
					storedAccessNodeStack.push(nodeElement);

				}
				endNodeId = currentNode.writeChildNodeToDB("TEXT", null, data, docid);
		} else {
			// This is inline data write to file directly
			StringBuffer inlineWhiteSpace = new StringBuffer(new String(cbuf, start, len));
			writeInlineDataIntoFile(inlineDataFileWriter, inlineWhiteSpace);
		}

	}

	/* Compare whitespace from xml and db */
	private void compareWhiteSpace(Stack<NodeRecord> nodeStack, String string,
			String error) throws SAXException {
		NodeRecord node = null;
		try {
			node = nodeStack.pop();
		} catch (EmptyStackException ee) {
			logMetacat.error("the stack is empty for whitespace data");
			throw new SAXException(error);
		}
		if (!node.getNodeType().equals("TEXT") || !string.equals(node.getNodeData())) {
			logMetacat.error("Inconsistence happened: ");
			logMetacat.error("current node type from xml is WHITESPACE TEXT");
			logMetacat.error("node type from stack: " + node.getNodeType());
			logMetacat.error("current node data from xml is: " + string);
			logMetacat.error("node data from stack: " + node.getNodeData());
			logMetacat.error("node is from stack: " + node.getNodeId());
			throw new SAXException(error);
		}// if
	}

	/** SAX Handler that receives notification of end of the document */
	public void endDocument() throws SAXException {
		logMetacat.debug("end Document");
		// There are some unchangable subtree didn't be compare
		// This maybe cause user change the subtree id
		if (!super.getIsRevisionDoc()) {
			// write access rule to db
			if (writeAccessRules) {
				writeAccessRuleToDB();
			}
			// delete relation table
			deleteRelations();
			// write relations
			for (int i = 0; i < onlineDataFileIdInRelationVector.size(); i++) {
				String id = onlineDataFileIdInRelationVector.elementAt(i);
				writeOnlineDataFileIdIntoRelationTable(id);
			}
		}
	}

	/* The method to write all access rule into db */
	private void writeAccessRuleToDB() throws SAXException {
		// Delete old permssion
		deletePermissionsInAccessTable();
		// write top leve access rule
		writeTopLevelAccessRuleToDB();
		// write additional access rule
		// writeAdditionalAccessRuleToDB();
		writeAdditionalAccessRulesToDB();
	}// writeAccessRuleToDB

	/* The method to write top level access rule into db. */
	private void writeTopLevelAccessRuleToDB() throws SAXException {
		// for top document level
		AccessSection accessSection = topLevelAccessControlMap.get(docid);
		boolean top = true;
		String subSectionId = null;
		if (accessSection != null) {
			AccessSection accessSectionObj = accessSection;

			// if accessSection is not null and is not reference
			if (accessSectionObj.getReferences() == null) {
				// check for denyFirst permOrder
				String permOrder = accessSectionObj.getPermissionOrder();
				if (permOrder.equals(AccessControlInterface.DENYFIRST) && ignoreDenyFirst) {
					logMetacat.warn("Metacat no longer supports EML 'denyFirst' access rules - ignoring this access block");
			    	return;
			    }
				// write the top level access module into xml_accesssubtree to
				// store info and then when update to check if the user can
				// update it or not
				deleteAccessSubTreeRecord(docid);
				writeAccessSubTreeIntoDB(accessSectionObj, TOPLEVEL);

				// write access section into xml_access table
				writeGivenAccessRuleIntoDB(accessSectionObj, top, subSectionId);
				// write online data file into xml_access too.
				// for (int i= 0; i <onlineDataFileIdInTopAccessVector.size();
				// i++)
				// {
				// String id = onlineDataFileIdInTopAccessVector.elementAt(i);
				// writeAccessRuleForRelatedDataFileIntoDB(accessSectionObj,
				// id);
				// }

			} else {

				// this is a reference and go trough the vector which contains
				// all access object
				String referenceId = accessSectionObj.getReferences();
				boolean findAccessObject = false;
				logMetacat.debug("referered id for top access: " + referenceId);
				for (int i = 0; i < accessObjectList.size(); i++) {
					AccessSection accessObj = accessObjectList.elementAt(i);
					String accessObjId = accessObj.getSubTreeId();
					// check for denyFirst permOrder
					String permOrder = accessObj.getPermissionOrder();
					if (permOrder.equals(AccessControlInterface.DENYFIRST) && ignoreDenyFirst) {
						logMetacat.warn("Metacat no longer supports EML 'denyFirst' access rules - ignoring this access block, subtree id: " + accessObjId);
				    	continue;
				    }
					if (referenceId != null && accessObj != null
							&& referenceId.equals(accessObjId)) {
						// make sure the user didn't change any thing in this
						// access moduel
						// too if user doesn't have all permission
						if (needToCheckAccessModule) {

							Stack<NodeRecord> newStack = accessObj
									.getStoredTmpNodeStack();
							// revise order
							newStack = DocumentUtil.reviseStack(newStack);
							// go throught the vector of
							// unChangeableAccessSubtreevector
							// and find the one whose id is as same as
							// referenceid
							AccessSection oldAccessObj = getAccessSectionFromUnchangableAccessVector(referenceId);
							// if oldAccessObj is null something is wrong
							if (oldAccessObj == null) {
								throw new SAXException(UPDATEACCESSERROR);
							}// if
							else {
								// Get the node stack from old access obj
								Stack<NodeRecord> oldStack = oldAccessObj
										.getSubTreeNodeStack();
								compareNodeStacks(newStack, oldStack);
							}// else
						}// if
						// write accessobject into db
						writeGivenAccessRuleIntoDB(accessObj, top, subSectionId);
						// write online data file into xml_access too.
						// for (int j= 0; j
						// <onlineDataFileIdInTopAccessVector.size(); j++)
						// {
						// String id =
						// onlineDataFileIdInTopAccessVector.elementAt(j);
						// writeAccessRuleForRelatedDataFileIntoDB(accessSectionObj,
						// id);
						// }

						// write the reference access into xml_accesssubtree
						// too write the top level access module into
						// xml_accesssubtree to store info and then when update
						// to check if the user can update it or not
						deleteAccessSubTreeRecord(docid);
						writeAccessSubTreeIntoDB(accessSectionObj, TOPLEVEL);
						writeAccessSubTreeIntoDB(accessObj, SUBTREELEVEL);
						findAccessObject = true;
						break;
					}
				}// for
				// if we couldn't find an access subtree id for this reference
				// id
				if (!findAccessObject) {
					throw new SAXException("The referenceid: " + referenceId
							+ " is not access subtree");
				}// if
			}// else

		}// if
		else {
			// couldn't find a access section object
			logMetacat.warn("couldn't find access control for document: " + docid);
		}

	}// writeTopLevelAccessRuletoDB

	/* Given a subtree id and find the responding access section */
	private AccessSection getAccessSectionFromUnchangableAccessVector(String id) {
		AccessSection result = null;
		// Makse sure the id
		if (id == null || id.equals("")) {
			return result;
		}
		// go throught vector and find the list
		for (int i = 0; i < unChangeableAccessSubTreeVector.size(); i++) {
			AccessSection accessObj = unChangeableAccessSubTreeVector.elementAt(i);
			if (accessObj.getSubTreeId() != null && (accessObj.getSubTreeId()).equals(id)) {
				result = accessObj;
			}// if
		}// for
		return result;
	}// getAccessSectionFromUnchangableAccessVector

	/* Compare two node stacks to see if they are same */
	private void compareNodeStacks(Stack<NodeRecord> stack1, Stack<NodeRecord> stack2)
			throws SAXException {
		// make sure stack1 and stack2 are not empty
		if (stack1.isEmpty() || stack2.isEmpty()) {
			logMetacat.error("Because stack is empty!");
			throw new SAXException(UPDATEACCESSERROR);
		}
		// go throw two stacks and compare every element
		while (!stack1.isEmpty()) {
			// Pop an element from stack1
			NodeRecord record1 = stack1.pop();
			// Pop an element from stack2(stack 2 maybe empty)
			NodeRecord record2 = null;
			try {
				record2 = stack2.pop();
			} catch (EmptyStackException ee) {
				logMetacat.error("Node stack2 is empty but stack1 isn't!");
				throw new SAXException(UPDATEACCESSERROR);
			}
			// if two records are not same throw a exception
			if (!record1.contentEquals(record2)) {
				logMetacat.error("Two records from new and old stack are not " + "same!");
				throw new SAXException(UPDATEACCESSERROR);
			}// if
		}// while

		// now stack1 is empty and we should make sure stack2 is empty too
		if (!stack2.isEmpty()) {
			logMetacat
					.error("stack2 still has some elements while stack " + "is empty! ");
			throw new SAXException(UPDATEACCESSERROR);
		}// if
	}// comparingNodeStacks

	/* The method to write additional access rule into db. */
	private void writeAdditionalAccessRulesToDB() throws SAXException {
		
		// Iterate through every distribution and write access sections for data and inline
		// types to the database
		for (DistributionSection distributionSection : allDistributionSections) {			
			// we're only interested in data and inline distributions
			int distributionType = distributionSection.getDistributionType();
			if (distributionType == DistributionSection.DATA_DISTRIBUTION
					|| distributionType == DistributionSection.INLINE_DATA_DISTRIBUTION) {
				AccessSection accessSection = distributionSection.getAccessSection();
				
				// If the distribution doesn't have an access section, we continue.
				if (accessSection == null) {
					continue;		
				}
				
				// check for denyFirst permOrder
				String permOrder = accessSection.getPermissionOrder();
				if (permOrder.equals(AccessControlInterface.DENYFIRST) && ignoreDenyFirst) {
					logMetacat.warn("Metacat no longer supports EML 'denyFirst' access rules - ignoring this access block: " + distributionSection.getDataFileName());
			    	continue;
			    }
				
				// We want to check file permissions for all online data updates and inserts, or for 
				// inline updates.
//				if (distributionType == DistributionSection.DATA_DISTRIBUTION
//						|| (distributionType == DistributionSection.INLINE_DATA_DISTRIBUTION && action == "UPDATE")) {

				if (distributionType == DistributionSection.DATA_DISTRIBUTION) {
					try {
						// check for the previous version for permissions on update
						// we need to look up the docid from the guid, if we have it
						String dataDocid = distributionSection.getDataFileName();
						try {
							dataDocid = IdentifierManager.getInstance().getLocalId(dataDocid);
						} catch (McdbDocNotFoundException mcdbnfe) {
							// ignore
							logMetacat.warn("Could not find guid/docid mapping for " + dataDocid);
						}
						String previousDocid = dataDocid;
						if (action == "UPDATE") {
							String docidWithoutRev = DocumentUtil.getDocIdFromString(dataDocid);
							int latestRevision = DBUtil.getLatestRevisionInDocumentTable(docidWithoutRev);
							if (latestRevision > 0) {
								previousDocid = docidWithoutRev + PropertyService.getProperty("document.accNumSeparator") + latestRevision;
							}
						}
						
						// check both the previous and current data permissions
						// see: https://projects.ecoinformatics.org/ecoinfo/issues/5647
						PermissionController controller = new PermissionController(previousDocid);
						PermissionController currentController = new PermissionController(dataDocid);

						if (AccessionNumber.accNumberUsed(docid)
								&& 
								!(controller.hasPermission(user, groups, "WRITE") 
										|| currentController.hasPermission(user, groups, "WRITE")
										)
								) {
							throw new SAXException(UPDATEACCESSERROR + " id: " + dataDocid);
						}
					} catch (SQLException sqle) {
						throw new SAXException(
								"Database error checking user permissions: "
										+ sqle.getMessage());
					} catch (Exception e) {
						throw new SAXException(
								"General error checking user permissions: "
										+ e.getMessage());
					}
				} else if (distributionType == DistributionSection.INLINE_DATA_DISTRIBUTION && action == "UPDATE") {
					try {
						
						// check for the previous version for permissions
						int latestRevision = DBUtil.getLatestRevisionInDocumentTable(docid);
						String previousDocid = 
							docid + PropertyService.getProperty("document.accNumSeparator") + latestRevision;
						PermissionController controller = new PermissionController(previousDocid);

						if (!controller.hasPermission(user, groups, "WRITE")) {
							throw new SAXException(UPDATEACCESSERROR);
						}
					} catch (SQLException sqle) {
						throw new SAXException(
								"Database error checking user permissions: "
										+ sqle.getMessage());
					} catch (Exception e) {
						throw new SAXException(
								"General error checking user permissions: "
										+ e.getMessage());
					}
				}
				
				// clear previous versions first
				deleteAccessRule(accessSection, false);
				
				// now write the new ones
				String subSectionId = Integer.toString(distributionSection.getDistributionId());
				writeGivenAccessRuleIntoDB(accessSection, false, subSectionId);
			}

		}
		
		
	}
	
	/**
	 *  delete existing access for given access rule
	 *  
	 */
	private void deleteAccessRule(AccessSection accessSection, boolean topLevel) throws SAXException {
		
		if (accessSection == null) {
			throw new SAXException("The access object is null");
		}
		
		PreparedStatement pstmt = null;

		String sql = null;
		sql = "DELETE FROM xml_access WHERE guid = ?";
			
		try {

			pstmt = connection.prepareStatement(sql);
			// Increase DBConnection usage count
			connection.increaseUsageCount(1);
			// Bind the values to the query
			String guid = null;
			if (topLevel) {
				try {
					guid = IdentifierManager.getInstance().getGUID(docid, Integer.valueOf(revision));
				} catch (NumberFormatException e) {
					throw new SAXException(e.getMessage(), e);
				} catch (McdbDocNotFoundException e) {
					// register the default mapping now
					guid = docid + "." + revision;
					IdentifierManager.getInstance().createMapping(guid, guid);
				}
			} else {
				guid = accessSection.getDataFileName();
				
			}
			pstmt.setString(1, guid);
			logMetacat.debug("guid in accesstable: " + guid);
			
			logMetacat.debug("running sql: " + pstmt.toString());
			pstmt.execute();
			
			pstmt.close();
		}// try
		catch (SQLException e) {
			throw new SAXException("EMLSAXHandler.deleteAccessRule(): "
					+ e.getMessage());
		}// catch
		finally {
			try {
				pstmt.close();
			} catch (SQLException ee) {
				throw new SAXException("EMLSAXHandler.deleteAccessRule(): "
						+ ee.getMessage());
			}
		}// finally

	}

	/* Write a given access rule into db */
	private void writeGivenAccessRuleIntoDB(AccessSection accessSection,
			boolean topLevel, String subSectionId) throws SAXException {
		if (accessSection == null) {
			throw new SAXException("The access object is null");
		}

		String guid = null;
		String referencedGuid = accessSection.getDataFileName();

		try {
			guid = IdentifierManager.getInstance().getGUID(docid, Integer.valueOf(revision));
		} catch (NumberFormatException e) {
			throw new SAXException(e.getMessage(), e);
		} catch (McdbDocNotFoundException e) {
			// register the default mapping now
			guid = docid + "." + revision;
			IdentifierManager.getInstance().createMapping(guid, guid);
		}
		
		String permOrder = accessSection.getPermissionOrder();
		String sql = null;
		PreparedStatement pstmt = null;
		if (topLevel) {
			sql = "INSERT INTO xml_access (guid, principal_name, permission, "
					+ "perm_type, perm_order, accessfileid) VALUES "
					+ " (?, ?, ?, ?, ?, ?)";
		} else {
			sql = "INSERT INTO xml_access (guid,principal_name, "
					+ "permission, perm_type, perm_order, accessfileid, subtreeid"
					+ ") VALUES" + " (?, ?, ?, ?, ?, ?, ?)";
		}
		try {

			pstmt = connection.prepareStatement(sql);
			// Increase DBConnection usage count
			connection.increaseUsageCount(1);
			// Bind the values to the query
			pstmt.setString(6, guid);
			logMetacat.debug("Accessfileid in accesstable: " + guid);
			pstmt.setString(5, permOrder);
			logMetacat.debug("PermOder in accesstable: " + permOrder);
			// if it is not top level, set subsection id
			if (topLevel) {
				pstmt.setString(1, guid);
				logMetacat.debug("Guid in accesstable: " + guid);
			}
			if (!topLevel) {
				// use the referenced guid
				pstmt.setString(1, referencedGuid );
				logMetacat.debug("Docid in accesstable: " + inlineDataFileName);

				// for subtree should specify the
				if (subSectionId == null) {
					throw new SAXException("The subsection is null");
				}

				pstmt.setString(7, subSectionId);
				logMetacat.debug("SubSectionId in accesstable: " + subSectionId);
			}

			Vector<AccessRule> accessRules = accessSection.getAccessRules();
			// go through every rule
			for (int i = 0; i < accessRules.size(); i++) {
				AccessRule rule = accessRules.elementAt(i);
				String permType = rule.getPermissionType();
				int permission = rule.getPermission();
				pstmt.setInt(3, permission);
				logMetacat.debug("permission in accesstable: " + permission);
				pstmt.setString(4, permType);
				logMetacat.debug("Permtype in accesstable: " + permType);
				// go through every principle in rule
				Vector<String> nameVector = rule.getPrincipal();
				for (int j = 0; j < nameVector.size(); j++) {
					String prName = nameVector.elementAt(j);
					pstmt.setString(2, prName);
					logMetacat.debug("Principal in accesstable: " + prName);
					logMetacat.debug("running sql: " + pstmt.toString());
					pstmt.execute();
				}// for
			}// for
			pstmt.close();
		}// try
		catch (SQLException e) {
			throw new SAXException("EMLSAXHandler.writeAccessRuletoDB(): "
					+ e.getMessage());
		}// catch
		finally {
			try {
				pstmt.close();
			} catch (SQLException ee) {
				throw new SAXException("EMLSAXHandler.writeAccessRuletoDB(): "
						+ ee.getMessage());
			}
		}// finally
		
		// for D1, refresh the entries
		HazelcastService.getInstance().refreshSystemMetadataEntry(guid);
		HazelcastService.getInstance().refreshSystemMetadataEntry(referencedGuid);
		

	}// writeGivenAccessRuleIntoDB

	/* Delete from db all permission for resources related to the document, if any */
	private void deletePermissionsInAccessTable() throws SAXException {
		PreparedStatement pstmt = null;
		try {
			
			String sql = "DELETE FROM xml_access " +
					// the file defines access for another file
					"WHERE accessfileid IN " +
						"(SELECT guid from identifier where docid = ? and rev = ?) " +
					// the described file has other versions describing it	
					"OR guid IN " +
						"(SELECT xa.guid from xml_access xa, identifier id" +
						" WHERE xa.accessfileid = id.guid " +
						" AND id.docid = ?" +
						" AND id.rev = ?)";
			// delete all acl records for resources related to @aclid if any
			pstmt = connection.prepareStatement(sql);
			pstmt.setString(1, docid);
			pstmt.setInt(2, Integer.valueOf(revision));
			// second part of query
			pstmt.setString(3, docid);
			pstmt.setInt(4, Integer.valueOf(revision));
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
	}// deletePermissionsInAccessTable

	/*
	 * In order to make sure only usr has "all" permission can update access
	 * subtree in eml document we need to keep access subtree info in
	 * xml_accesssubtree table, such as docid, version, startnodeid, endnodeid
	 */
	private void writeAccessSubTreeIntoDB(AccessSection accessSection, String level)
			throws SAXException {
		if (accessSection == null) {
			throw new SAXException("The access object is null");
		}

		String sql = null;
		PreparedStatement pstmt = null;
		sql = "INSERT INTO xml_accesssubtree (docid, rev, controllevel, "
				+ "subtreeid, startnodeid, endnodeid) VALUES " + " (?, ?, ?, ?, ?, ?)";
		try {

			pstmt = connection.prepareStatement(sql);
			// Increase DBConnection usage count
			connection.increaseUsageCount(1);
			long startNodeId = accessSection.getStartNodeId();
			long endNodeId = accessSection.getEndNodeId();
			String sectionId = accessSection.getSubTreeId();
			// Bind the values to the query
			pstmt.setString(1, docid);
			logMetacat.debug("Docid in access-subtreetable: " + docid);
			pstmt.setLong(2, (new Long(revision)).longValue());
			logMetacat.debug("rev in accesssubtreetable: " + revision);
			pstmt.setString(3, level);
			logMetacat.debug("contorl level in access-subtree table: " + level);
			pstmt.setString(4, sectionId);
			logMetacat.debug("Subtree id in access-subtree table: " + sectionId);
			pstmt.setLong(5, startNodeId);
			logMetacat.debug("Start node id is: " + startNodeId);
			pstmt.setLong(6, endNodeId);
			logMetacat.debug("End node id is: " + endNodeId);
			logMetacat.debug("running sql: " + pstmt.toString());
			pstmt.execute();
			pstmt.close();
		}// try
		catch (SQLException e) {
			throw new SAXException("EMLSAXHandler.writeAccessSubTreeIntoDB(): "
					+ e.getMessage());
		}// catch
		finally {
			try {
				pstmt.close();
			} catch (SQLException ee) {
				throw new SAXException("EMLSAXHandler.writeAccessSubTreeIntoDB(): "
						+ ee.getMessage());
			}
		}// finally

	}// writeAccessSubtreeIntoDB

	/* Delete every access subtree record from xml_accesssubtree. */
	private void deleteAccessSubTreeRecord(String docId) throws SAXException {
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
	}// deleteAccessSubTreeRecord

	// open a file writer for writing inline data to file
	private Writer createInlineDataFileWriter(String fileName, String encoding) throws SAXException {
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
		logMetacat.debug("inline file name: " + newFile);
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
			throws SAXException {
		try {
			writer.write(data.toString());
			writer.flush();
		} catch (Exception e) {
			throw new SAXException(e.getMessage());
		}
	}



	// if xml file failed to upload, we need to call this method to delete
	// the inline data already in file system
	public void deleteInlineFiles() throws SAXException {
		if (!inlineFileIdList.isEmpty()) {
			for (int i = 0; i < inlineFileIdList.size(); i++) {
				String fileName = inlineFileIdList.elementAt(i);
				deleteInlineDataFile(fileName);
			}
		}
	}

	/* delete the inline data file */
	private void deleteInlineDataFile(String fileName) throws SAXException {
		String path;
		try {
			path = PropertyService.getProperty("application.inlinedatafilepath");
		} catch (PropertyNotFoundException pnfe) {
			throw new SAXException("Could not find inline data file path: "
					+ pnfe.getMessage());
		}
		File inlineDataDirectory = new File(path);
		File newFile = new File(inlineDataDirectory, fileName);
		newFile.delete();

	}

	/* Delete relations */
	private void deleteRelations() throws SAXException {
		PreparedStatement pStmt = null;
		String sql = "DELETE FROM xml_relation where docid =?";
		try {
			pStmt = connection.prepareStatement(sql);
			// bind variable
			pStmt.setString(1, docid);
			// execute query
			pStmt.execute();
			pStmt.close();
		}// try
		catch (SQLException e) {
			throw new SAXException("EMLSAXHandler.deleteRelations(): " + e.getMessage());
		}// catch
		finally {
			try {
				pStmt.close();
			}// try
			catch (SQLException ee) {
				throw new SAXException("EMLSAXHandler.deleteRelations: "
						+ ee.getMessage());
			}// catch
		}// finally
	}

	/*
	 * Write an online data file id into xml_relation table. The dataId
	 * shouldnot have the revision
	 */
	private void writeOnlineDataFileIdIntoRelationTable(String dataId)
			throws SAXException {
		PreparedStatement pStmt = null;
		String sql = "INSERT into xml_relation (docid, packagetype, subject, "
				+ "relationship, object) values (?, ?, ?, ?, ?)";
		try {
			pStmt = connection.prepareStatement(sql);
			// bind variable
			pStmt.setString(1, docid);
			pStmt.setString(2, doctype); //DocumentImpl.EML2_1_0NAMESPACE);
			pStmt.setString(3, docid);
			pStmt.setString(4, RELATION);
			pStmt.setString(5, dataId);
			// execute query
			pStmt.execute();
			pStmt.close();
		}// try
		catch (SQLException e) {
			throw new SAXException(
					"EMLSAXHandler.writeOnlineDataFileIdIntoRelationTable(): "
							+ e.getMessage());
		}// catch
		finally {
			try {
				pStmt.close();
			}// try
			catch (SQLException ee) {
				throw new SAXException(
						"EMLSAXHandler.writeOnlineDataFileIdIntoRelationTable(): "
								+ ee.getMessage());
			}// catch
		}// finally

	}// writeOnlineDataFileIdIntoRelationTable

	/*
	 * This method will handle data file in online url. If the data file is in
	 * ecogrid protocol, then the datafile identifier(without rev)should be put
	 * into onlineDataFileRelationVector. The docid in this vector will be
	 * insert into xml_relation table in endDocument(). If the data file doesn't
	 * exsit in xml_documents or xml_revision table, or the user has all
	 * permission to the data file if the docid already existed, the data file
	 * id (without rev)will be put into onlineDataFileTopAccessVector. The top
	 * access rules specified in this eml document will apply to the data file.
	 * NEED to do: We should also need to implement http and ftp. Those external
	 * files should be download and assign a data file id to it.
	 */
	private void handleOnlineUrlDataFile(String url) throws SAXException {
		logMetacat.warn("The url is " + url);

		if (currentDistributionSection == null) {
			throw new SAXException("Trying to set the online file name for a null"
					+ " distribution section");
		}

		// if the url is not in ecogrid protocol, null will be returned
		String accessionNumber = DocumentUtil.getAccessionNumberFromEcogridIdentifier(url);
		
		// check he accession number and get docid.rev if we can
		String docid = null;
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
		
		if (accessionNumber == null) {
			// the accession number is null if the url does not references a
			// local data file (url would start with "ecogrid://"
			currentDistributionSection
					.setDistributionType(DistributionSection.ONLINE_DATA_DISTRIBUTION);
		} else {
			// look up the guid using docid/rev
			String guid = null;
			try {
				guid = IdentifierManager.getInstance().getGUID(docid, rev);
			} catch (McdbDocNotFoundException e1) {
				// no need to do this if we are not writing access rules for the data
				if (!writeAccessRules) {
					logMetacat.warn("Not configured to write access rules for data referenced by: " + url);
					return;
				}
				guid = docid + "." + rev;
				IdentifierManager.getInstance().createMapping(guid, guid);
			}

			currentDistributionSection
					.setDistributionType(DistributionSection.DATA_DISTRIBUTION);
			currentDistributionSection.setDataFileName(guid);

			// distributionOnlineFileName = docid;
			onlineDataFileIdInRelationVector.add(guid);
			try {				
				if (!AccessionNumber.accNumberUsed(docid)) {
					onlineDataFileIdInTopAccessVector.add(guid);
				} else {
					// check the previous revision if we have it
					int previousRevision = rev;
					Vector<Integer> revisions = DBUtil.getRevListFromRevisionTable(docid);
					if (revisions != null && revisions.size() > 0) {
						previousRevision = revisions.get(revisions.size() - 1);
					}
					String previousDocid = 
						docid + PropertyService.getProperty("document.accNumSeparator") + previousRevision;

					// check EITHER previous or current id for access rules
					// see: https://projects.ecoinformatics.org/ecoinfo/issues/5647
					PermissionController previousController = new PermissionController(previousDocid);
					PermissionController currentController = new PermissionController(accessionNumber);				
					if (previousController.hasPermission(user, groups, AccessControlInterface.ALLSTRING)
							|| currentController.hasPermission(user, groups, AccessControlInterface.ALLSTRING)
							) {
						onlineDataFileIdInTopAccessVector.add(guid);
					} else {
						throw new SAXException(UPDATEACCESSERROR);
					}
				} 
			}// try
			catch (Exception e) {
				logMetacat.error("Eorr in "
								+ "Eml210SAXHanlder.handleOnlineUrlDataFile is "
								+ e.getMessage());
				throw new SAXException(e.getMessage());
			}
		}
	}
}
