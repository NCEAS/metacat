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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Date;
import java.util.EmptyStackException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Stack;
import java.util.Vector;

import edu.ucsb.nceas.metacat.database.DBConnection;
import edu.ucsb.nceas.metacat.database.DBConnectionPool;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.metacat.service.XMLSchema;
import edu.ucsb.nceas.metacat.util.MetacatUtil;
import edu.ucsb.nceas.utilities.triple.Triple;
import edu.ucsb.nceas.utilities.triple.TripleCollection;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;
import edu.ucsb.nceas.utilities.StringUtil;

import org.apache.log4j.Logger;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.ext.DeclHandler;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.helpers.DefaultHandler;

/**
 * A database aware Class implementing callback bethods for the SAX parser to
 * call when processing the XML stream and generating events.
 */
public class DBSAXHandler extends DefaultHandler implements LexicalHandler,
        DeclHandler
{

    protected boolean atFirstElement;

    protected boolean processingDTD;

    protected String docname = null;

    protected String doctype;
    
    protected String catalogid = null;

    protected String systemid;

    private boolean stackCreated = false;

    protected Stack<DBSAXNode> nodeStack;

    protected Vector<DBSAXNode> nodeIndex;

    protected DBConnection connection = null;

    protected DocumentImpl currentDocument;
    
    protected Date createDate = null;
    
    protected Date updateDate = null;

    protected DBSAXNode rootNode;

    protected String action = null;

    protected String docid = null;

    protected String revision = null;

    protected String user = null;

    protected String[] groups = null;

    protected String pub = null;
    
	protected String encoding = null;

//    private boolean endDocument = false;

    protected int serverCode = 1;

    protected Hashtable<String,String> namespaces = new Hashtable<String,String>();

    protected boolean hitTextNode = false; // a flag to hit text node

    // a buffer to keep all text nodes for same element
    // it is for if element was split
    protected StringBuffer textBuffer = new StringBuffer();

//    protected Stack textBufferStack = new Stack();

    public static final int MAXDATACHARS = 4000;

    //protected static final int MAXDATACHARS = 50;

    // methods writeChildNodeToDB, setAttribute, setNamespace,
    // writeTextForDBSAXNode will increase endNodeId.
    protected long endNodeId = -1; // The end node id for a substree
    // DOCTITLE attr cleared from the db
    //   private static final int MAXTITLELEN = 1000;
    
    private boolean isRevisionDoc  = false;
    
    protected Vector<XMLSchema> schemaList = new Vector<XMLSchema>();

    //HandlerTriple stuff
    TripleCollection tripleList = new TripleCollection();

    Triple currentTriple = new Triple();

    boolean startParseTriple = false;

    boolean hasTriple = false;
    
	protected boolean writeAccessRules = true;   
	
	protected boolean ignoreDenyFirst = true;

    public static final String ECOGRID = "ecogrid://";

    private Logger logMetacat = Logger.getLogger(DBSAXHandler.class);

    /**
     * Construct an instance of the handler class
     *
     * @param conn the JDBC connection to which information is written
     */
    private DBSAXHandler(DBConnection conn, Date createDate, Date updateDate)
    {
        this.connection = conn;
        this.atFirstElement = true;
        this.processingDTD = false;
        this.createDate = createDate;
        this.updateDate = updateDate;

        // Create the stack for keeping track of node context
        // if it doesn't already exist
        if (!stackCreated) {
            nodeStack = new Stack<DBSAXNode>();
            nodeIndex = new Vector<DBSAXNode>();
            stackCreated = true;
        }
    }

    /**
     * Construct an instance of the handler class
     *
     * @param conn the JDBC connection to which information is written
     * @param action - "INSERT" or "UPDATE"
     * @param docid to be inserted or updated into JDBC connection
     * @param user the user connected to MetaCat servlet and owns the document
     * @param groups the groups to which user belongs
     * @param pub flag for public "read" access on document
     * @param serverCode the serverid from xml_replication on which this
     *            document resides.
     *
     */
/* TODO excise this constructor because not used anywhere in project
    public DBSAXHandler(DBConnection conn, String action, String docid,
            String user, String[] groups, String pub, int serverCode)
    {
        this(conn);
        this.action = action;
        this.docid = docid;
        this.user = user;
        this.groups = groups;
        this.pub = pub;
        this.serverCode = serverCode;
        this.xmlIndex = new Thread(this);
    }
*/
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
    public DBSAXHandler(DBConnection conn, String action, String docid,
            String revision, String user, String[] groups, String pub,
            int serverCode, Date createDate, Date updateDate, boolean writeAccessRules)
    {
        this(conn, createDate, updateDate);
        this.action = action;
        this.docid = docid;
        this.revision = revision;
        this.user = user;
        this.groups = groups;
        this.pub = pub;
        this.serverCode = serverCode;
        this.writeAccessRules = writeAccessRules;
    }

    /** SAX Handler that receives notification of beginning of the document */
    public void startDocument() throws SAXException
    {
        logMetacat.debug("DBSaxHandler.startDocument - starting document");

        // Create the document node representation as root
        rootNode = new DBSAXNode(connection, this.docid);
        // Add the node to the stack, so that any text data can be
        // added as it is encountered
        nodeStack.push(rootNode);
    }

    /** SAX Handler that receives notification of end of the document */
	public void endDocument() throws SAXException {
		logMetacat.debug("DBSaxHandler.endDocument - ending document");
		// Starting new thread for writing XML Index.
		// It calls the run method of the thread.

		try {
			// if it is data package insert triple into relation table;
			if (doctype != null
					&& MetacatUtil.getOptionList(
							PropertyService.getProperty("xml.packagedoctype")).contains(
							doctype) && hasTriple && !isRevisionDoc) {

				// initial handler and write into relation db only for
				// xml-documents
				if (!isRevisionDoc) {
					RelationHandler handler = new RelationHandler(docid, doctype,
							connection, tripleList);
				}
			}
		} catch (Exception e) {
			logMetacat.error("DBSaxHandler.endDocument - Failed to write triples into relation table"
					+ e.getMessage());
			throw new SAXException("Failed to write triples into relation table "
					+ e.getMessage());
		}
		
		// If we get here, the document and schema parsed okay.  If there are
		// any schemas in the schema list, they are new and need to be registered.
    	for (XMLSchema xmlSchema : schemaList) {
    		String externalFileUri = xmlSchema.getExternalFileUri();
    		String fileNamespace = xmlSchema.getFileNamespace();
    		SchemaLocationResolver resolver = 
    			new SchemaLocationResolver(fileNamespace, externalFileUri);
    		resolver.resolveNameSpace();
    	}
	}

    /** SAX Handler that is called at the start of Namespace */
    public void startPrefixMapping(String prefix, String uri)
            throws SAXException
    {
        logMetacat.debug("DBSaxHandler.startPrefixMapping - Starting namespace");

        namespaces.put(prefix, uri);
    }

    /** SAX Handler that is called at the start of each XML element */
    public void startElement(String uri, String localName, String qName,
            Attributes atts) throws SAXException
    {
        // for element <eml:eml...> qname is "eml:eml", local name is "eml"
        // for element <acl....> both qname and local name is "eml"
        // uri is namespace
        logMetacat.debug("DBSaxHandler.startElement - Start ELEMENT(qName) " + qName);
        logMetacat.debug("DBSaxHandler.startElement - Start ELEMENT(localName) " + localName);
        logMetacat.debug("DBSaxHandler.startElement - Start ELEMENT(uri) " + uri);

        DBSAXNode parentNode = null;
        DBSAXNode currentNode = null;

        // Get a reference to the parent node for the id
        try {
            
            parentNode = (DBSAXNode) nodeStack.peek();
        } catch (EmptyStackException e) {
            parentNode = null;
        }

        // If hit a text node, we need write this text for current's parent
        // node This will happen if the element is mixed
        if (hitTextNode && parentNode != null) {
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
                // otherwise root element
                if (uri != null && !(uri.trim()).equals("")) {
                    doctype = uri;
                } else {
                    doctype = docname;
                }
                logMetacat.debug("DBSaxHandler.startElement - DOCNAME-a: " + docname);
                logMetacat.debug("DBSaxHandler.startElement - DOCTYPE-a: " + doctype);
            } else if (doctype == null) {
                // because docname is not null and it is declared in dtd
                // so could not be in schema, no namespace
                doctype = docname;
                logMetacat.debug("DBSaxHandler.startElement - DOCTYPE-b: " + doctype);
            }
           
            rootNode.writeNodename(docname);
          
            try {
                // for validated XML Documents store a reference to XML DB
                // Catalog
                // Because this is select statement and it needn't to roll back
                // if
                // insert document action failed.
                // In order to decrease DBConnection usage count, we get a new
                // dbconnection from pool
               
                DBConnection dbConn = null;
                int serialNumber = -1;
               
                if (systemid != null) {
                    try {
                        // Get dbconnection
                        dbConn = DBConnectionPool
                                .getDBConnection("DBSAXHandler.startElement");
                        serialNumber = dbConn.getCheckOutSerialNumber();

                        String sql = "SELECT catalog_id FROM xml_catalog "
                            + "WHERE entry_type = 'DTD' "
                            + "AND public_id = ?";
                        	
                        PreparedStatement pstmt = dbConn.prepareStatement(sql);
                        pstmt.setString(1, doctype);
                        ResultSet rs = pstmt.executeQuery();
                        boolean hasRow = rs.next();
                        if (hasRow) {
                            catalogid = rs.getString(1);
                        }
                        pstmt.close();
                    }//try
                    finally {
                        // Return dbconnection
                        DBConnectionPool.returnDBConnection(dbConn, serialNumber);
                    }//finally
                }

                //create documentImpl object by the constructor which can
                // specify
                //the revision
              
                if (!isRevisionDoc)
                {
                  currentDocument = new DocumentImpl(connection, rootNode
                        .getNodeID(), docname, doctype, docid, revision,
                        action, user, this.pub, catalogid, this.serverCode, 
                        createDate, updateDate);
                }               
            } catch (Exception ane) {
                ane.printStackTrace(System.out);
                ane.printStackTrace(System.err);
                throw (new SAXException("Error in DBSaxHandler.startElement for action "
                        + action + " : " + ane.getMessage(), ane));
            }
        }

        // Create the current node representation
        currentNode = new DBSAXNode(connection, qName, localName,
                parentNode, rootNode.getNodeID(), docid, doctype);

        // Add all of the namespaces
        String prefix;
        String nsuri;
        Enumeration<String> prefixes = namespaces.keys();
        while (prefixes.hasMoreElements()) {
            prefix = (String) prefixes.nextElement();
            nsuri = (String) namespaces.get(prefix);
            currentNode.setNamespace(prefix, nsuri, docid);
        }
        namespaces = null;
        namespaces = new Hashtable<String,String>();

        // Add all of the attributes
        for (int i = 0; i < atts.getLength(); i++) {
            String attributeName = atts.getQName(i);
            String attributeValue = atts.getValue(i);
            endNodeId = currentNode.setAttribute(attributeName, attributeValue,
                    docid);

            // To handle name space and schema location if the attribute name
            // is xsi:schemaLocation. If the name space is in not in catalog 
            // table it will be registered.
            if (attributeName != null
                    && attributeName
                            .indexOf(MetaCatServlet.SCHEMALOCATIONKEYWORD) != -1) {
            	// These schemas will be registered in the end endDocument() method
            	// assuming parsing is successful.
        		// each namespace could have several schema locations.  parsedUri will
        		// hold a list of uri and files.
            	attributeValue = StringUtil.replaceTabsNewLines(attributeValue);
            	attributeValue = StringUtil.replaceDuplicateSpaces(attributeValue);
        		Vector<String> parsedUri = StringUtil.toVector(attributeValue, ' ');
        		for (int j = 0; j < parsedUri.size(); j = j + 2 ) {
        			if (j + 1 >= parsedUri.size()) {
        				throw new SAXException("Odd number of elements found when parsing schema location: " + 	
        						attributeValue + ". There should be an even number of uri/files in location.");
        			}
        			//since we don't have format id information here, we set it null
        			String formatId = null;
        			XMLSchema xmlSchema = 
        				new XMLSchema(parsedUri.get(j), parsedUri.get(j + 1), formatId);
        			schemaList.add(xmlSchema);
        		}
            }
        }

        // Add the node to the stack, so that any text data can be
		// added as it is encountered
		nodeStack.push(currentNode);
		// Add the node to the vector used by thread for writing XML Index
		nodeIndex.addElement(currentNode);
		// start parsing triple
		try {
			if (doctype != null
					&& MetacatUtil.getOptionList(
							PropertyService.getProperty("xml.packagedoctype")).contains(doctype)
					&& localName.equals("triple")) {
				startParseTriple = true;
				hasTriple = true;
				currentTriple = new Triple();
			}
		} catch (PropertyNotFoundException pnfe) {
			pnfe.printStackTrace(System.out);
			pnfe.printStackTrace(System.err);
			throw (new SAXException("Error in DBSaxHandler.startElement for action " + action +
			        " : " + pnfe.getMessage(), pnfe));
		}
	}               
    

    /** SAX Handler that is called for each XML text node */
    public void characters(char[] cbuf, int start, int len) throws SAXException
    {
        logMetacat.debug("DBSaxHandler.characters - starting characters");
        // buffer all text nodes for same element. This is for if text was split
        // into different nodes
        textBuffer.append(new String(cbuf, start, len));
        // set hittextnode true
        hitTextNode = true;
        // if text buffer .size is greater than max, write it to db.
        // so we can save memory
        if (textBuffer.length() > MAXDATACHARS) {
            logMetacat.debug("DBSaxHandler.characters - Write text into DB in charaters"
                    + " when text buffer size is greater than maxmum number");
            DBSAXNode currentNode = (DBSAXNode) nodeStack.peek();
            endNodeId = writeTextForDBSAXNode(endNodeId, textBuffer,
                    currentNode);
            textBuffer = null;
            textBuffer = new StringBuffer();
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
        logMetacat.debug("DBSaxHandler.ignorableWhitespace - in ignorableWhitespace");

        DBSAXNode currentNode = (DBSAXNode) nodeStack.peek();

            // Write the content of the node to the database
            endNodeId = currentNode.writeChildNodeToDB("TEXT", null, new String(cbuf, start, len),
                    docid);
    }

    /**
     * SAX Handler called once for each processing instruction found: node that
     * PI may occur before or after the root element.
     */
    public void processingInstruction(String target, String data)
            throws SAXException
    {
        logMetacat.debug("DBSaxHandler.processingInstruction - in processing instructions");
        DBSAXNode currentNode = (DBSAXNode) nodeStack.peek();
        endNodeId = currentNode.writeChildNodeToDB("PI", target, data, docid);
    }

    /** SAX Handler that is called at the end of each XML element */
    public void endElement(String uri, String localName, String qName)
            throws SAXException
    {
        logMetacat.debug("DBSaxHandler.endElement - End element " + qName);

        // write buffered text nodes into db (so no splited)
        DBSAXNode currentNode = (DBSAXNode) nodeStack.peek();

        // If before the end element, the parser hit text nodes and store them
        // into the buffer, write the buffer to data base. The reason we put
        // write database here is for xerces some time split text node
        if (hitTextNode) {
            logMetacat.debug("DBSaxHandler.endElement - Write text into DB in End Element");
            endNodeId = writeTextForDBSAXNode(endNodeId, textBuffer,
                    currentNode);

            //if it is triple parsing process
            if (startParseTriple) {

                String content = textBuffer.toString().trim();
                if (localName.equals("subject")) { //get the subject content
                    currentTriple.setSubject(content);
                } else if (localName.equals("relationship")) { //get the
                                                               // relationship
                                                               // content
                    currentTriple.setRelationship(content);
                } else if (localName.equals("object")) { //get the object
                                                         // content
                    currentTriple.setObject(content);
                }
            }

        }//if

        //set hitText false
        hitTextNode = false;
        // reset textbuff
        textBuffer = null;
        textBuffer = new StringBuffer();

        // Get the node from the stack
        currentNode = (DBSAXNode) nodeStack.pop();
        //finishing parsing single triple
        if (startParseTriple && localName.equals("triple")) {
            // add trip to triple collection
            tripleList.addTriple(currentTriple);
            //rest variable
            currentTriple = null;
            startParseTriple = false;
        }
    }

    //
    // the next section implements the LexicalHandler interface
    //

    /** SAX Handler that receives notification of DOCTYPE. Sets the DTD */
    public void startDTD(String name, String publicId, String systemId)
            throws SAXException
    {
        docname = name;
        doctype = publicId;
        systemid = systemId;

        processingDTD = true;
        DBSAXNode currentNode = (DBSAXNode) nodeStack.peek();
        //create a DTD node and write docname,publicid and system id into db
        // we don't put the dtd node into node stack
        DBSAXNode dtdNode = new DBSAXNode(connection, name, publicId, systemId,
                currentNode, currentNode.getRootNodeID(), docid);
        logMetacat.debug("DBSaxHandler.startDTD - Start DTD");
        logMetacat.debug("DBSaxHandler.startDTD - Setting processingDTD to true");
        logMetacat.debug("DBSaxHandler.startDTD - DOCNAME: " + docname);
        logMetacat.debug("DBSaxHandler.startDTD - DOCTYPE: " + doctype);
        logMetacat.debug("DBSaxHandler.startDTD - SYSID: " + systemid);
    }

    /**
     * SAX Handler that receives notification of end of DTD
     */
    public void endDTD() throws SAXException
    {

        processingDTD = false;
        logMetacat.debug("DBSaxHandler.endDTD - Setting processingDTD to false");
        logMetacat.debug("DBSaxHandler.endDTD - end DTD");
    }

    /**
     * SAX Handler that receives notification of comments in the DTD
     */
    public void comment(char[] ch, int start, int length) throws SAXException
    {
        logMetacat.debug("DBSaxHandler.comment - starting comment");
        if (!processingDTD) {
            DBSAXNode currentNode = (DBSAXNode) nodeStack.peek();
            endNodeId = currentNode.writeChildNodeToDB("COMMENT", null,
                    new String(ch, start, length), docid);
        }
    }

    /**
     * SAX Handler that receives notification of the start of CDATA sections
     */
    public void startCDATA() throws SAXException
    {
        logMetacat.debug("DBSaxHandler.startCDATA - starting CDATA");
    }

    /**
     * SAX Handler that receives notification of the end of CDATA sections
     */
    public void endCDATA() throws SAXException
    {
        logMetacat.debug("DBSaxHandler.endCDATA - end CDATA");
    }

    /**
     * SAX Handler that receives notification of the start of entities
     */
    public void startEntity(String name) throws SAXException
    {
        logMetacat.debug("DBSaxHandler.startEntity - starting entity: " + name);
        //System.out.println("start ENTITY: " + name);
        if (name.equals("[dtd]")) {
            processingDTD = true;
        }
    }

    /**
     * SAX Handler that receives notification of the end of entities
     */
    public void endEntity(String name) throws SAXException
    {
        logMetacat.debug("DBSaxHandler.endEntity - ending entity: " + name);
        //System.out.println("end ENTITY: " + name);
        if (name.equals("[dtd]")) {
            processingDTD = false;
        }
    }

    /**
     * SAX Handler that receives notification of element declarations
     */
    public void elementDecl(String name, String model)
            throws org.xml.sax.SAXException
    {
        //System.out.println("ELEMENTDECL: " + name + " " + model);
        logMetacat.debug("DBSaxHandler.elementDecl - element declaration: " + name + " " + model);
    }

    /**
     * SAX Handler that receives notification of attribute declarations
     */
    public void attributeDecl(String eName, String aName, String type,
            String valueDefault, String value) throws org.xml.sax.SAXException
    {

        //System.out.println("ATTRIBUTEDECL: " + eName + " "
        //                        + aName + " " + type + " " + valueDefault + " "
        //                        + value);
        logMetacat.debug("DBSaxHandler.attributeDecl - attribute declaration: " + eName + " " + aName + " "
                + type + " " + valueDefault + " " + value);
    }

    /**
     * SAX Handler that receives notification of internal entity declarations
     */
    public void internalEntityDecl(String name, String value)
            throws org.xml.sax.SAXException
    {
        //System.out.println("INTERNENTITYDECL: " + name + " " + value);
        logMetacat.debug("DBSaxHandler.internalEntityDecl - internal entity declaration: " + name + " " + value);
    }

    /**
     * SAX Handler that receives notification of external entity declarations
     */
    public void externalEntityDecl(String name, String publicId, String systemId)
            throws org.xml.sax.SAXException
    {
        //System.out.println("EXTERNENTITYDECL: " + name + " " + publicId
        //                              + " " + systemId);
        logMetacat.debug("DBSaxHandler.externalEntityDecl - external entity declaration: " + name + " " + publicId
                + " " + systemId);
        // it processes other external entity, not the DTD;
        // it doesn't signal for the DTD here
        processingDTD = false;
    }

    //
    // the next section implements the ErrorHandler interface
    //

    /**
     * SAX Handler that receives notification of fatal parsing errors
     */
    public void fatalError(SAXParseException exception) throws SAXException
    {
        logMetacat.fatal("DBSaxHandler.fatalError - " + exception.getMessage());
        throw (new SAXException("Fatal processing error.", exception));
    }

    /**
     * SAX Handler that receives notification of recoverable parsing errors
     */
    public void error(SAXParseException exception) throws SAXException
    {
        logMetacat.error("DBSaxHandler.error - " + exception.getMessage());
        throw (new SAXException(exception.getMessage(), exception));
    }

    /**
     * SAX Handler that receives notification of warnings
     */
    public void warning(SAXParseException exception) throws SAXException
    {
        logMetacat.warn("DBSaxHandler.warning - " + exception.getMessage());
        throw (new SAXException(exception.getMessage(), exception));
    }

    //
    // Helper, getter and setter methods
    //

    /**
     * get the document name
     */
    public String getDocname()
    {
        return docname;
    }

    /**
     * get the document processing state
     */
    public boolean processingDTD()
    {
        return processingDTD;
    }
    
    
    /**
     * get the the is revision doc
     * @return
     */
    public boolean getIsRevisionDoc()
    {
        return isRevisionDoc;
    }
    
    /**
     * Set the the handler is for revisionDoc
     * @param isRevisionDoc
     */
    public void setIsRevisionDoc(boolean isRevisionDoc)
    {
       this.isRevisionDoc = isRevisionDoc;   
    }

    public String getEncoding() {
		return encoding;
	}

	public void setEncoding(String encoding) {
		this.encoding = encoding;
	}

	/* Method to write a text buffer for DBSAXNode */
    protected long writeTextForDBSAXNode(long previousEndNodeId,
            StringBuffer strBuffer, DBSAXNode node) throws SAXException
    {
        long nodeId = previousEndNodeId;
        // Check parameter
        if (strBuffer == null || node == null) { return nodeId; }
        boolean moredata = true;

        String normalizedData = strBuffer.toString();
        logMetacat.debug("DBSAXHandler.writeTextForDBSAXNode - Before normalize in write process: " + normalizedData);
        String afterNormalize = MetacatUtil.normalize(normalizedData);
        logMetacat.debug("DBSAXHandler.writeTextForDBSAXNode - After normalize in write process: " + afterNormalize);
        strBuffer = new StringBuffer(afterNormalize);;

        int bufferSize = strBuffer.length();
        int start = 0;

        // if there are some cotent in buffer, write it
        if (bufferSize > 0) {
            logMetacat.debug("DBSAXHandler.writeTextForDBSAXNode - Write text into DB");

                // Write the content of the node to the database
                nodeId = node.writeChildNodeToDB("TEXT", null, new String(strBuffer), docid);
        }//if
        return nodeId;
    }
    
    public long getRootNodeId()
    {
        return rootNode.getNodeID();
    }
    
    public String getDocumentType()
    {
        return doctype;
    }
    
    public String getDocumentName()
    {
        return docname;
    }
    
    public String getCatalogId()
    {
        return catalogid;
    }
}
