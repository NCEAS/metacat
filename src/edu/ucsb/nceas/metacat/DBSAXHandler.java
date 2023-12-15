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
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import edu.ucsb.nceas.metacat.database.DBConnection;
import edu.ucsb.nceas.metacat.database.DBConnectionPool;
import edu.ucsb.nceas.metacat.service.XMLSchema;
import edu.ucsb.nceas.utilities.triple.Triple;
import edu.ucsb.nceas.utilities.triple.TripleCollection;
import edu.ucsb.nceas.utilities.StringUtil;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
    public final static long NODE_ID = -1;
    
    protected boolean atFirstElement;

    protected boolean processingDTD;

    protected String docname = null;

    protected String doctype;
    
    protected String catalogid = null;

    protected String systemid;

    protected DBConnection connection = null;

    protected DocumentImpl currentDocument;
    
    protected Date createDate = null;
    
    protected Date updateDate = null;


    protected String action = null;

    protected String docid = null;

    protected String revision = null;

    protected String user = null;

    protected String[] groups = null;

    protected String pub = null;
    
    protected String encoding = null;

    protected int serverCode = 1;

    protected Hashtable<String,String> namespaces = new Hashtable<String,String>();

    protected boolean hitTextNode = false; // a flag to hit text node

    // a buffer to keep all text nodes for same element
    // it is for if element was split
    protected StringBuffer textBuffer = new StringBuffer();

    public static final int MAXDATACHARS = 4000;
    protected static final String SCHEMALOCATIONKEYWORD = ":schemaLocation";

    // methods writeChildNodeToDB, setAttribute, setNamespace,
    // writeTextForDBSAXNode will increase endNodeId.
    protected long endNodeId = -1; // The end node id for a substree
    
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

    private Log logMetacat = LogFactory.getLog(DBSAXHandler.class);

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
    }

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
        logMetacat.trace("DBSaxHandler.startDocument - starting document");
    }

    /** SAX Handler that receives notification of end of the document */
    public void endDocument() throws SAXException {
        logMetacat.trace("DBSaxHandler.endDocument - ending document");
        // Starting new thread for writing XML Index.
        // It calls the run method of the thread.
    }

    /** SAX Handler that is called at the start of Namespace */
    public void startPrefixMapping(String prefix, String uri)
            throws SAXException
    {
        logMetacat.trace("DBSaxHandler.startPrefixMapping - Starting namespace");

        namespaces.put(prefix, uri);
    }

    /** SAX Handler that is called at the start of each XML element */
    public void startElement(String uri, String localName, String qName,
            Attributes atts) throws SAXException
    {
        // for element <eml:eml...> qname is "eml:eml", local name is "eml"
        // for element <acl....> both qname and local name is "eml"
        // uri is namespace
        logMetacat.trace("DBSaxHandler.startElement - Start ELEMENT(qName) " + qName);
        logMetacat.trace("DBSaxHandler.startElement - Start ELEMENT(localName) " + localName);
        logMetacat.trace("DBSaxHandler.startElement - Start ELEMENT(uri) " + uri);
        
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
                  currentDocument = new DocumentImpl(connection, NODE_ID,
                         docname, doctype, docid, revision,
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

        // Add all of the namespaces
        String prefix;
        String nsuri;
        Enumeration<String> prefixes = namespaces.keys();
        while (prefixes.hasMoreElements()) {
            prefix = (String) prefixes.nextElement();
            nsuri = (String) namespaces.get(prefix);
        }
        namespaces = null;
        namespaces = new Hashtable<String,String>();

        // Add all of the attributes
        for (int i = 0; i < atts.getLength(); i++) {
            String attributeName = atts.getQName(i);
            String attributeValue = atts.getValue(i);
            
            // To handle name space and schema location if the attribute name
            // is xsi:schemaLocation. If the name space is in not in catalog 
            // table it will be registered.
            if (attributeName != null
                    && attributeName.indexOf(SCHEMALOCATIONKEYWORD) != -1) {
                // These schemas will be registered in the end endDocument() method
                // assuming parsing is successful.
                // each namespace could have several schema locations.  parsedUri will
                // hold a list of uri and files.
                attributeValue = StringUtil.replaceTabsNewLines(attributeValue);
                attributeValue = StringUtil.replaceDuplicateSpaces(attributeValue);
                Vector<String> parsedUri = StringUtil.toVector(attributeValue, ' ');
                for (int j = 0; j < parsedUri.size(); j = j + 2 ) {
                    if (j + 1 >= parsedUri.size()) {
                        throw new SAXException("Odd number of elements found when parsing schema "
                                + "location: " + attributeValue
                                + ". There should be an even number of uri/files in location.");
                    }
                    //since we don't have format id information here, we set it null
                    String formatId = null;
                    XMLSchema xmlSchema =
                        new XMLSchema(parsedUri.get(j), parsedUri.get(j + 1), formatId);
                    schemaList.add(xmlSchema);
                }
            }
        }
    }
    

    /** SAX Handler that is called for each XML text node */
    public void characters(char[] cbuf, int start, int len) throws SAXException
    {
        logMetacat.trace("DBSaxHandler.characters - starting characters");
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
        logMetacat.trace("DBSaxHandler.ignorableWhitespace - in ignorableWhitespace");

    }

    /**
     * SAX Handler called once for each processing instruction found: node that
     * PI may occur before or after the root element.
     */
    public void processingInstruction(String target, String data)
            throws SAXException
    {
        logMetacat.trace("DBSaxHandler.processingInstruction - in processing instructions");
    }

    /** SAX Handler that is called at the end of each XML element */
    public void endElement(String uri, String localName, String qName)
            throws SAXException
    {
        logMetacat.trace("DBSaxHandler.endElement - End element " + qName);
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
        logMetacat.trace("DBSaxHandler.startDTD - Start DTD");
        logMetacat.trace("DBSaxHandler.startDTD - Setting processingDTD to true");
        logMetacat.trace("DBSaxHandler.startDTD - DOCNAME: " + docname);
        logMetacat.trace("DBSaxHandler.startDTD - DOCTYPE: " + doctype);
        logMetacat.trace("DBSaxHandler.startDTD - SYSID: " + systemid);
    }

    /**
     * SAX Handler that receives notification of end of DTD
     */
    public void endDTD() throws SAXException
    {

        processingDTD = false;
        logMetacat.trace("DBSaxHandler.endDTD - Setting processingDTD to false");
        logMetacat.trace("DBSaxHandler.endDTD - end DTD");
    }

    /**
     * SAX Handler that receives notification of comments in the DTD
     */
    public void comment(char[] ch, int start, int length) throws SAXException
    {
        logMetacat.trace("DBSaxHandler.comment - starting comment");
    }

    /**
     * SAX Handler that receives notification of the start of CDATA sections
     */
    public void startCDATA() throws SAXException
    {
        logMetacat.trace("DBSaxHandler.startCDATA - starting CDATA");
    }

    /**
     * SAX Handler that receives notification of the end of CDATA sections
     */
    public void endCDATA() throws SAXException
    {
        logMetacat.trace("DBSaxHandler.endCDATA - end CDATA");
    }

    /**
     * SAX Handler that receives notification of the start of entities
     */
    public void startEntity(String name) throws SAXException
    {
        logMetacat.trace("DBSaxHandler.startEntity - starting entity: " + name);
        if (name.equals("[dtd]")) {
            processingDTD = true;
        }
    }

    /**
     * SAX Handler that receives notification of the end of entities
     */
    public void endEntity(String name) throws SAXException
    {
        logMetacat.trace("DBSaxHandler.endEntity - ending entity: " + name);
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
        logMetacat.trace("DBSaxHandler.elementDecl - element declaration: " + name + " " + model);
    }

    /**
     * SAX Handler that receives notification of attribute declarations
     */
    public void attributeDecl(String eName, String aName, String type,
            String valueDefault, String value) throws org.xml.sax.SAXException
    {

        logMetacat.trace("DBSaxHandler.attributeDecl - attribute declaration: " + eName
                       + " " + aName + " " + type + " " + valueDefault + " " + value);
    }

    /**
     * SAX Handler that receives notification of internal entity declarations
     */
    public void internalEntityDecl(String name, String value)
            throws org.xml.sax.SAXException
    {
        logMetacat.trace("DBSaxHandler.internalEntityDecl - internal entity declaration: "
                            + name + " " + value);
    }

    /**
     * SAX Handler that receives notification of external entity declarations
     */
    public void externalEntityDecl(String name, String publicId, String systemId)
            throws org.xml.sax.SAXException
    {
        logMetacat.trace("DBSaxHandler.externalEntityDecl - external entity declaration: "
                            + name + " " + publicId + " " + systemId);
        // it processes other external entity, not the DTD;
        // it doesn't signal for the DTD here
        processingDTD = false;
    }

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
    
    public long getRootNodeId()
    {
        return NODE_ID;
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
