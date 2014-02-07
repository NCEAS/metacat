/**
 *  '$RCSfile$'
 *    Purpose: A Class that represents an XML document
 *  Copyright: 2000 Regents of the University of California and the
 *             National Center for Ecological Analysis and Synthesis
 *    Authors: Matt Jones
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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.math.BigInteger;
import java.net.URL;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;
import java.util.Hashtable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Stack;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.bind.DatatypeConverter;

import edu.ucsb.nceas.utilities.access.AccessControlInterface;
import edu.ucsb.nceas.metacat.accesscontrol.AccessControlList;
import edu.ucsb.nceas.metacat.client.InsufficientKarmaException;
import edu.ucsb.nceas.metacat.common.query.EnabledQueryEngines;
import edu.ucsb.nceas.metacat.database.DBConnection;
import edu.ucsb.nceas.metacat.database.DBConnectionPool;
import edu.ucsb.nceas.metacat.database.DatabaseService;
import edu.ucsb.nceas.metacat.dataone.SyncAccessPolicy;
import edu.ucsb.nceas.metacat.dataone.hazelcast.HazelcastService;
import edu.ucsb.nceas.metacat.index.MetacatSolrIndex;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.metacat.replication.ForceReplicationHandler;
import edu.ucsb.nceas.metacat.replication.ReplicationService;
import edu.ucsb.nceas.metacat.service.XMLSchema;
import edu.ucsb.nceas.metacat.service.XMLSchemaService;
import edu.ucsb.nceas.metacat.shared.AccessException;
import edu.ucsb.nceas.metacat.spatial.SpatialHarvester;
import edu.ucsb.nceas.metacat.util.AuthUtil;
import edu.ucsb.nceas.metacat.util.DocumentUtil;
import edu.ucsb.nceas.metacat.util.MetacatUtil;
import edu.ucsb.nceas.metacat.util.SystemUtil;
import edu.ucsb.nceas.utilities.FileUtil;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;
import edu.ucsb.nceas.utilities.UtilException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.XmlStreamReader;
import org.apache.log4j.Logger;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.SystemMetadata;
import org.xml.sax.ContentHandler;
import org.xml.sax.DTDHandler;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

/**
 * A class that represents an XML document. It can be created with a simple
 * document identifier from a database connection. It also will write an XML
 * text document to a database connection using SAX.
 */
public class DocumentImpl
{
    /* Constants */
    public static final String SCHEMA = "Schema";
    public static final String DTD = "DTD";
    public static final String EML200 = "eml200";
    public static final String EML210 = "eml210";
    public static final String EXTERNALSCHEMALOCATIONPROPERTY = "http://apache.org/xml/properties/schema/external-schemaLocation";
    public static final String REVISIONTABLE = "xml_revisions";
    public static final String DOCUMENTTABLE = "xml_documents";
    /*
     * public static final String EXTERNALSCHEMALOCATION =
     * "eml://ecoinformatics.org/eml-2.0.0
     * http://dev.nceas.ucsb.edu/tao/schema/eml.xsd"+ "
     * http://www.xml-cml.org/schema/stmml
     * http://dev.nceas.ucsb.edu/tao/schema/stmml.xsd";
     */
    public static final String DECLARATIONHANDLERPROPERTY = "http://xml.org/sax/properties/declaration-handler";
    public static final String LEXICALPROPERTY = "http://xml.org/sax/properties/lexical-handler";
    public static final String VALIDATIONFEATURE = "http://xml.org/sax/features/validation";
    public static final String SCHEMAVALIDATIONFEATURE = "http://apache.org/xml/features/validation/schema";
    public static final String FULLSCHEMAVALIDATIONFEATURE = "http://apache.org/xml/features/validation/schema-full-checking";
    public static final String NAMESPACEFEATURE = "http://xml.org/sax/features/namespaces";
    public static final String NAMESPACEPREFIXESFEATURE = "http://xml.org/sax/features/namespace-prefixes";
    
    public static final String EML2_0_0NAMESPACE;
    public static final String EML2_0_1NAMESPACE;
    public static final String EML2_1_0NAMESPACE;
    public static final String EML2_1_1NAMESPACE;
    public static final String RDF_SYNTAX_NAMESPACE;
    
    static {
    	String eml200NameSpace = null;
    	String eml201NameSpace = null;
    	String eml210NameSpace = null;
    	String eml211NameSpace = null;
    	String rdfNameSpace = null;
    	try {
    		eml200NameSpace = PropertyService.getProperty("xml.eml2_0_0namespace");
    		eml201NameSpace = PropertyService.getProperty("xml.eml2_0_1namespace");
    		eml210NameSpace = PropertyService.getProperty("xml.eml2_1_0namespace");
    		eml211NameSpace = PropertyService.getProperty("xml.eml2_1_1namespace");
    		rdfNameSpace = PropertyService.getProperty("xml.rdf_syntax_namespace");
    	} catch (PropertyNotFoundException pnfe) {
    		System.err.println("Could not get property in static block: " 
					+ pnfe.getMessage());
    	}
    	
    	EML2_1_1NAMESPACE = eml211NameSpace;
        // "eml://ecoinformatics.org/eml-2.1.1";
    	EML2_1_0NAMESPACE = eml210NameSpace;
        // "eml://ecoinformatics.org/eml-2.1.0";
        EML2_0_1NAMESPACE = eml201NameSpace;
        // "eml://ecoinformatics.org/eml-2.0.1";
        EML2_0_0NAMESPACE = eml200NameSpace;
        // "eml://ecoinformatics.org/eml-2.0.0";
        RDF_SYNTAX_NAMESPACE = rdfNameSpace;
    }
     
    public static final String DOCNAME = "docname";
    public static final String PUBLICID = "publicid";
    public static final String SYSTEMID = "systemid";
    static final int ALL = 1;
    static final int WRITE = 2;
    static final int READ = 4;
    protected DBConnection connection = null;
    //protected String updatedVersion = null;
    protected String docname = null;
    protected String doctype = null;
    private String validateType = null; //base on dtd or schema
    private Date createdate = null;
    private Date updatedate = null;
    private String system_id = null;
    private String userowner = null;
    private String userupdated = null;
    protected String docid = null; // without revision
    private int rev;
    private int serverlocation;
    private String docHomeServer;
    private String publicaccess;
    protected long rootnodeid;
    private ElementNode rootNode = null;
    private TreeSet<NodeRecord> nodeRecordList = null;
    
    private Vector<String> pathsForIndexing = null;
  
    private static Logger logMetacat = Logger.getLogger(DocumentImpl.class);
    private static Logger logReplication = Logger.getLogger("ReplicationLogging");

    /**
     * Default constructor
     *
     */
    public DocumentImpl()
    {
        
    }

    /**
     * Constructor used to create a document and read the document information
     * from the database. If readNodes is false, then the node data is not read
     * at this time, but is deferred until it is needed (such as when a call to
     * toXml() is made).
     *
     * @param conn
     *            the database connection from which to read the document
     * @param docid
     *            the identifier of the document to be created, it should
     *            be with revision
     * @param readNodes
     *            flag indicating whether the xmlnodes should be read
     */
    public DocumentImpl(String accNum, boolean readNodes) throws McdbException
    {
        try {
            //this.conn = conn;
            this.docid = DocumentUtil.getDocIdFromAccessionNumber(accNum);
            this.rev   = DocumentUtil.getRevisionFromAccessionNumber(accNum);
            
            pathsForIndexing = SystemUtil.getPathsForIndexing();

            // Look up the document information
            getDocumentInfo(docid, rev);

            if (readNodes) {
                // Download all of the document nodes using a single SQL query
                // The sort order of the records is determined by the
                // NodeComparator
                // class, and needs to represent a depth-first traversal for the
                // toXml() method to work properly
                nodeRecordList = getNodeRecordList(rootnodeid);
            }

        } catch (McdbException ex) {
            throw ex;
        } catch (Throwable t) {
        	throw new McdbException("Error reading document: " + docid);
        }
    }

    /**
     * Constructor, creates document from database connection, used for reading
     * the document
     *
     * @param conn
     *            the database connection from which to read the document
     * @param docid
     *            the identifier of the document to be created
     */
    public DocumentImpl(String docid) throws McdbException
    {
        this(docid, true);
    }

    /**
     * Construct a new document instance, writing the contents to the database.
     * This method is called from DBSAXHandler because we need to know the root
     * element name for documents without a DOCTYPE before creating it.
     *
     * In this constructor, the docid is without rev. There is a string rev to
     * specify the revision user want to upadate. The revion is only need to be
     * greater than current one. It is not need to be sequent number just after
     * current one. So it is only used in update action
     *
     * @param conn
     *            the JDBC Connection to which all information is written
     * @param rootnodeid -
     *            sequence id of the root node in the document
     * @param docname -
     *            the name of DTD, i.e. the name immediately following the
     *            DOCTYPE keyword ( should be the root element name ) or the
     *            root element name if no DOCTYPE declaration provided (Oracle's
     *            and IBM parsers are not aware if it is not the root element
     *            name)
     * @param doctype -
     *            Public ID of the DTD, i.e. the name immediately following the
     *            PUBLIC keyword in DOCTYPE declaration or the docname if no
     *            Public ID provided or null if no DOCTYPE declaration provided
     * @param docid
     *            the docid to use for the UPDATE, no version number
     * @param version,
     *            need to be update
     * @param action
     *            the action to be performed (INSERT OR UPDATE)
     * @param user
     *            the user that owns the document
     * @param pub
     *            flag for public "read" access on document
     * @param serverCode
     *            the serverid from xml_replication on which this document
     *            resides.
     *
     */
    public DocumentImpl(DBConnection conn, long rootNodeId, String docName,
            String docType, String docId, String newRevision, String action,
            String user, String pub, String catalogId, int serverCode, 
            Date createDate, Date updateDate)
            throws SQLException, Exception
    {
        this.connection = conn;
        this.rootnodeid = rootNodeId;
        this.docname = docName;
        this.doctype = docType;
        this.docid = docId;
        this.rev = (new Integer(newRevision)).intValue();
        
        pathsForIndexing = SystemUtil.getPathsForIndexing();
        
        //this.updatedVersion = newRevision;
        writeDocumentToDB(action, user, pub, catalogId, serverCode, createDate, updateDate);
    }

    /**
     * This method will be call in handleUploadRequest in MetacatServlet class
     */
    public static void registerDocument(String docname, String doctype,
            String accnum, String user, String[] groupnames) throws SQLException,
            AccessionNumberException, Exception
    {
        try {
            // get server location for this doc
            int serverLocation = getServerLocationNumber(accnum);
            registerDocument(docname, doctype, accnum, user, groupnames,
                             serverLocation);
        } catch (Exception e) {
            throw e;
        }
    }

    /**
     * Register a document that resides on the filesystem with the database.
     * (ie, just an entry in xml_documents, nothing in xml_nodes). Creates a
     * reference to a filesystem document (used for non-xml data files). This
     * class only be called in MetaCatServerlet.
     *
     * @param conn
     *            the JDBC Connection to which all information is written
     * @param docname -
     *            the name of DTD, i.e. the name immediately following the
     *            DOCTYPE keyword ( should be the root element name ) or the
     *            root element name if no DOCTYPE declaration provided (Oracle's
     *            and IBM parsers are not aware if it is not the root element
     *            name)
     * @param doctype -
     *            Public ID of the DTD, i.e. the name immediately following the
     *            PUBLIC keyword in DOCTYPE declaration or the docname if no
     *            Public ID provided or null if no DOCTYPE declaration provided
     * @param accnum
     *            the accession number to use for the INSERT OR UPDATE, which
     *            includes a revision number for this revision of the document
     *            (e.g., knb.1.1)
     * @param user
     *            the user that owns the document
     * @param groupnames
     *            the groups that owns the document
     * @param serverCode
     *            the serverid from xml_replication on which this document
     *            resides.
     */
    public static void registerDocument(String docname, String doctype,
            String accnum, String user, String[] groups, int serverCode)
            throws SQLException, AccessionNumberException, Exception
    {
        DBConnection conn = null;
        int serialNumber = -1;
        try
        {
            conn = DBConnectionPool
                .getDBConnection("DocumentImpl.registerDocumentInreplication");
            serialNumber = conn.getCheckOutSerialNumber();
            conn.setAutoCommit(false);
            String action = null;
            String docIdWithoutRev = DocumentUtil.getDocIdFromAccessionNumber(accnum);
            int userSpecifyRev = DocumentUtil.getRevisionFromAccessionNumber(accnum);
            action = checkRevInXMLDocuments(docIdWithoutRev, userSpecifyRev);
            logMetacat.debug("after check rev, the action is " + action);
            if (action.equals("UPDATE"))
            {
                //archive the old entry
            	// check permissions on the old doc when updating
            	int latestRevision = DBUtil.getLatestRevisionInDocumentTable(docIdWithoutRev);
				String previousDocid = 
					docIdWithoutRev + PropertyService.getProperty("document.accNumSeparator") + latestRevision;
                if (!hasWritePermission(user, groups, previousDocid)) { 
                	throw new Exception(
                        "User " + user
                        + " does not have permission to update the document"
                        + accnum); }
                archiveDocRevision(docIdWithoutRev, user, conn);  
            }
            
            String rev = Integer.toString(userSpecifyRev);
            modifyRecordsInGivenTable(DOCUMENTTABLE, action,docIdWithoutRev, doctype, docname,
                        user, rev, serverCode, null, null, conn);
                        // null and null is createdate and updatedate
                        // null will create current time
            conn.commit();
            conn.setAutoCommit(true);
        }
        catch(Exception e)
        {
            conn.rollback();
            conn.setAutoCommit(true);
            throw e;
        }
        finally
        {
            //check in DBConnection
            DBConnectionPool.returnDBConnection(conn, serialNumber);
        }
        
    }

    /**
     * Register a document that resides on the filesystem with the database.
     * (ie, just an entry in xml_documents, nothing in xml_nodes). Creates a
     * reference to a filesystem document (used for non-xml data files) This
     * method will be called for register data file in xml_documents in
     * Replication. This method is revised from registerDocument.
     *
     * @param conn
     *            the JDBC Connection to which all information is written
     * @param docname -
     *            the name of DTD, i.e. the name immediately following the
     *            DOCTYPE keyword ( should be the root element name ) or the
     *            root element name if no DOCTYPE declaration provided (Oracle's
     *            and IBM parsers are not aware if it is not the root element
     *            name)
     * @param doctype -
     *            Public ID of the DTD, i.e. the name immediately following the
     *            PUBLIC keyword in DOCTYPE declaration or the docname if no
     *            Public ID provided or null if no DOCTYPE declaration provided
     * @param accnum
     *            the accession number to use for the INSERT OR UPDATE, which
     *            includes a revision number for this revision of the document
     *            (e.g., knb.1.1)
     * @param user
     *            the user that owns the document
     * @param serverCode
     *            the serverid from xml_replication on which this document
     *            resides.
     */
    public static void registerDocumentInReplication(String docname,
            String doctype, String accnum, String user, int serverCode, 
            String tableName, Date createDate, Date updateDate)
            throws SQLException, AccessionNumberException, Exception
    {
        DBConnection conn = null;
        int serialNumber = -1;
        try
        {
            //check out DBConnection
            conn = DBConnectionPool
                    .getDBConnection("DocumentImpl.registerDocumentInreplication");
            serialNumber = conn.getCheckOutSerialNumber();
            conn.setAutoCommit(false);
            String action = null;
            String docIdWithoutRev = DocumentUtil.getDocIdFromAccessionNumber(accnum);
            int userSpecifyRev = DocumentUtil.getRevisionFromAccessionNumber(accnum);
            if (tableName.equals(DOCUMENTTABLE))
            {
                action = checkRevInXMLDocuments(docIdWithoutRev, userSpecifyRev);
                if (action.equals("UPDATE"))
                {
                        //archive the old entry
                        archiveDocRevision(docIdWithoutRev, user, conn);  
                }
            }
            else if (tableName.equals(REVISIONTABLE))
            {
                action = checkXMLRevisionTable(docIdWithoutRev, userSpecifyRev);
            }
            else
            {
                throw new Exception("Couldn't handle this table name "+tableName);
            }
           
            String rev = Integer.toString(userSpecifyRev);
            modifyRecordsInGivenTable(tableName, action,docIdWithoutRev, doctype, docname,
                        user, rev, serverCode, createDate, updateDate, conn);
            conn.commit();
            conn.setAutoCommit(true);
        }
        catch(Exception e)
        {
            conn.rollback();
            conn.setAutoCommit(true);
            throw e;
        }
        finally
        {
            //check in DBConnection
            DBConnectionPool.returnDBConnection(conn, serialNumber);
        }
       
    }
    
   /*
    * This method will insert or update xml-documents or xml_revision table
    */
   private static void modifyRecordsInGivenTable(String tableName, String action,
                       String docid, String doctype, String docname, String user,
                       String rev, int serverCode, Date createDate, Date updateDate,
                       DBConnection dbconn) throws Exception
   {
      
      PreparedStatement pstmt = null;
      int revision = (new Integer(rev)).intValue();
      String sqlDateString = DatabaseService.getInstance().getDBAdapter().getDateTimeFunction();
      Date today = new Date(Calendar.getInstance().getTimeInMillis());
      
      if (createDate == null){
          createDate = today;
      }
      
      if (updateDate == null) {
          updateDate = today;
      }
      
      try {
        
        StringBuffer sql = new StringBuffer();
        if (action != null && action.equals("INSERT")) {
            
            sql.append("insert into ");
            sql.append(tableName);
            sql.append(" (docid, docname, doctype, ");
            sql.append("user_owner, user_updated, server_location, rev, date_created, ");
            sql.append("date_updated, public_access) values (");
            sql.append("?, ");
            sql.append("?, ");
            sql.append("?, ");
            sql.append("?, ");
            sql.append("?, ");
            sql.append("?, ");
            sql.append("?, ");
            sql.append("?, ");
            sql.append("?, ");
            sql.append("'0')");
            // set the values
            pstmt = dbconn.prepareStatement(sql.toString());
            pstmt.setString(1, docid);
            pstmt.setString(2, docname);
            pstmt.setString(3, doctype);
            pstmt.setString(4, user);
            pstmt.setString(5, user);
            pstmt.setInt(6, serverCode);
            pstmt.setInt(7, revision);
            pstmt.setTimestamp(8, new Timestamp(createDate.getTime()));
            pstmt.setTimestamp(9, new Timestamp(updateDate.getTime()));

        } else if (action != null && action.equals("UPDATE")) {
            
            sql.append("update xml_documents set docname = ?,");
            sql.append("user_updated = ?, ");
            sql.append("server_location= ?, ");
            sql.append("rev = ?, ");
            sql.append("date_updated = ?");
            sql.append(" where docid = ? ");
            // set the values
            pstmt = dbconn.prepareStatement(sql.toString());
            pstmt.setString(1, docname);
            pstmt.setString(2, user);
            pstmt.setInt(3, serverCode);
            pstmt.setInt(4, revision);
            pstmt.setTimestamp(5, new Timestamp(updateDate.getTime()));
            pstmt.setString(6, docid);
        }
        logMetacat.debug("DocumentImpl.modifyRecordsInGivenTable - executing SQL: " + pstmt.toString());
        pstmt.execute();
        pstmt.close();
       
    }
    catch(Exception e) 
    {
        logMetacat.debug("Caught a general exception: " + e.getMessage());
        throw e;
    }
    finally 
    {
       if(pstmt != null)
       {
         pstmt.close();
       }
     }     
   }
    /**
     * This method will register a data file entry in xml_documents and save a
     * data file input Stream into file system.. It is only used in replication
     *
     * @param input,
     *            the input stream which contain the file content.
     * @param ,
     *            the input stream which contain the file content
     * @param docname -
     *            the name of DTD, for data file, it is a docid number.
     * @param doctype -
     *            "BIN" for data file
     * @param accnum
     *            the accession number to use for the INSERT OR UPDATE, which
     *            includes a revision number for this revision of the document
     *            (e.g., knb.1.1)
     * @param user
     *            the user that owns the document
     * @param docHomeServer,
     *            the home server of the docid
     * @param notificationServer,
     *            the server to notify force replication info to local metacat
     */
    public static void writeDataFileInReplication(InputStream input,
            String filePath, String docname, String doctype, String accnum,
            String user, String docHomeServer, String notificationServer, 
            String tableName, boolean timedReplication, Date createDate, Date updateDate)
            throws SQLException, AccessionNumberException, Exception
    {
        int serverCode = -2;

        if (filePath == null || filePath.equals("")) { throw new Exception(
                "Please specify the directory where file will be store"); }
        if (accnum == null || accnum.equals("")) { throw new Exception(
                "Please specify the stored file name"); }

        // If server is not int the xml replication talbe, insert it into
        // xml_replication table
        //serverList.addToServerListIfItIsNot(docHomeServer);
        insertServerIntoReplicationTable(docHomeServer);

        // Get server code again
        serverCode = getServerCode(docHomeServer);

        
        //write inputstream into file system.
        File dataDirectory = new File(filePath);
        File newFile = null;
        try
        {
            newFile = new File(dataDirectory, accnum);
    
            // create a buffered byte output stream
            // that uses a default-sized output buffer
            FileOutputStream fos = new FileOutputStream(newFile);
            BufferedOutputStream outPut = new BufferedOutputStream(fos);
    
            BufferedInputStream bis = null;
            bis = new BufferedInputStream(input);
            byte[] buf = new byte[4 * 1024]; // 4K buffer
            int b = bis.read(buf);
    
            while (b != -1) {
                outPut.write(buf, 0, b);
                b = bis.read(buf);
            }
            bis.close();
            outPut.close();
            fos.close();
            
            //register data file into xml_documents table
            registerDocumentInReplication(docname, doctype, accnum, user,
                    serverCode, tableName, createDate, updateDate);
        }
        catch (Exception ee)
        {
            newFile.delete();
            throw ee;
        }
        
        // Force replicate data file
        if (!timedReplication)
        {
          ForceReplicationHandler forceReplication = new ForceReplicationHandler(
                accnum, false, notificationServer);
          logMetacat.info("ForceReplicationHandler created: " + forceReplication.toString());
        }
    }
    
    
    /*
     * This method will determine if we need to insert or update xml_document base
     * on given docid, rev and rev in xml_documents table
     */
     private static String checkRevInXMLDocuments(String docid, int userSpecifyRev) throws Exception
     {
        String action = null;
        logMetacat.debug("The docid without rev is "+docid);
        logMetacat.debug("The user specifyRev: " + userSpecifyRev);
        // Revision for this docid in current database
        int revInDataBase =DBUtil.getLatestRevisionInDocumentTable(docid);
        logMetacat.debug("The rev in data base: " + revInDataBase);
        // String to store the revision
//        String rev = null;

        //revIndataBase=-1, there is no record in xml_documents table
        //the document is a new one for local server, inert it into table
        //user specified rev should be great than 0
        if (revInDataBase == -1 && userSpecifyRev >= 0) {
            // rev equals user specified
//            rev = (new Integer(userSpecifyRev)).toString();
            // action should be INSERT
            action = "INSERT";
        }
        //rev is greater the last revsion number and revInDataBase isn't -1
        // it is a updated file
        else if (userSpecifyRev > revInDataBase && revInDataBase >= 0) {
            // rev equals user specified
//            rev = (new Integer(userSpecifyRev)).toString();
            // action should be update
            action = "UPDATE";
        }
        // local server has newer version, then notify the remote server
        else if (userSpecifyRev < revInDataBase && revInDataBase > 0) {
            throw new Exception("Local server: "
                    + SystemUtil.getSecureServerURL()
                    + " has newer revision of doc: " + docid + "."
                    + revInDataBase + ". Please notify it.");
        }
        //other situation
        else {

            throw new Exception("The docid" + docid
                    + "'s revision number couldn't be " + userSpecifyRev);
        }
        return action;
     }
     
     /*
      * This method will check if the xml_revision table already has the
      * document or not
      */
     private static String checkXMLRevisionTable(String docid, int rev) throws Exception
     {
         String action = "INSERT";
         Vector<Integer> localrev = null;
           
         try
         {
           localrev = DBUtil.getRevListFromRevisionTable(docid);
         }
         catch (SQLException e)
         {
           logMetacat.error("Local rev for docid "+ docid + " could not "+
                                  " be found because " + e.getMessage());
           logReplication.error("Docid "+ docid + " could not be "+
                   "written because error happend to find it's local revision");
           throw new Exception (e.getMessage());
         }
         logMetacat.debug("rev list in xml_revision table for docid "+ docid + " is "+
                                 localrev.toString());
         
         // if the rev is in the xml_revision, it throws a exception
         if (localrev.contains(new Integer(rev)))
         {
            throw new Exception("The docid and rev is already in xml_revision table");       
         }

         return action;
     }
     
     /*
      * 
      */

    /**
     * Get a lock for a given document.
     */
    public static boolean getDataFileLockGrant(String accnum) throws Exception
    {
        try {
            int serverLocation = getServerLocationNumber(accnum);
            return getDataFileLockGrant(accnum, serverLocation);
        } catch (Exception e) {
            throw e;
        }
    }

    /**
     * The method will check if metacat can get data file lock grant If server
     * code is 1, it get. If server code is not 1 but call replication getlock
     * successfully, it get else, it didn't get
     *
     * @param accnum,
     *            the ID of the document
     * @param action,
     *            the action to the document
     * @param serverCode,
     *            the server location code
     */
    public static boolean getDataFileLockGrant(String accnum, int serverCode)
            throws Exception
    {
        boolean flag = true;
        String docid = DocumentUtil.getDocIdFromString(accnum);
        int rev = DocumentUtil.getVersionFromString(accnum);

        if (serverCode == 1) {
            flag = true;
            return flag;
        }

        //if((serverCode != 1 && action.equals("UPDATE")) )
        if (serverCode != 1) { //if this document being written is not a
                               // resident of this server then
            //we need to try to get a lock from it's resident server. If the
            //resident server will not give a lock then we send the user a
            // message
            //saying that he/she needs to download a new copy of the file and
            //merge the differences manually.

            String server = ReplicationService
                    .getServerNameForServerCode(serverCode);
            logReplication.info("attempting to lock " + accnum);
            URL u = new URL("https://" + server + "?server="
                    + MetacatUtil.getLocalReplicationServerName()
                    + "&action=getlock&updaterev=" + rev + "&docid=" + docid);
            //System.out.println("sending message: " + u.toString());
            String serverResStr = ReplicationService.getURLContent(u);
            String openingtag = serverResStr.substring(0, serverResStr
                    .indexOf(">") + 1);
            if (openingtag.equals("<lockgranted>")) {
                //the lock was granted go ahead with the insert
                //System.out.println("In lockgranted");
            	logReplication.info("lock granted for " + accnum
                        + " from " + server);
                flag = true;
                return flag;
            }//if

            else if (openingtag.equals("<filelocked>")) {//the file is
                                                         // currently locked by
                                                         // another user
                //notify our user to wait a few minutes, check out a new copy
                // and try
                //again.
                //System.out.println("file locked");
            	logReplication.error("lock denied for " + accnum + " on "
                        + server + " reason: file already locked");
                throw new Exception(
                        "The file specified is already locked by another "
                                + "user.  Please wait 30 seconds, checkout the "
                                + "newer document, merge your changes and try "
                                + "again.");
            } else if (openingtag.equals("<outdatedfile>")) {//our file is
                                                             // outdated. notify
                                                             // our user to
                                                             // check out a new
                                                             // copy of the
                //file and merge his version with the new version.
                //System.out.println("outdated file");
            	logReplication.error("lock denied for " + accnum + " on "
                        + server + " reason: local file outdated");
                throw new Exception(
                        "The file you are trying to update is an outdated"
                         + " version.  Please checkout the newest document, "
                         + "merge your changes and try again.");
            }
        }
        return flag;
    }

    /**
     * get the document name
     */
    public String getDocname()
    {
        return docname;
    }

    /**
     * get the document type (which is the PublicID)
     */
    public String getDoctype()
    {
        return doctype;
    }

    /**
     * get the system identifier
     */
    public String getSystemID()
    {
        return system_id;
    }

    /**
     * get the root node identifier
     */
    public long getRootNodeID()
    {
        return rootnodeid;
    }

    /**
     * get the creation date
     */
    public Date getCreateDate()
    {
        return createdate;
    }

    /**
     * get the update date
     */
    public Date getUpdateDate()
    {
        return updatedate;
    }

    /**
     * Get the document identifier (docid)
     */
    public String getDocID()
    {
        return docid;
    }

    public String getUserowner()
    {
        return userowner;
    }

    public String getUserupdated()
    {
        return userupdated;
    }

    public int getServerlocation()
    {
        return serverlocation;
    }

    public String getDocHomeServer()
    {
        return docHomeServer;
    }

    public String getPublicaccess()
    {
        return publicaccess;
    }

    public int getRev()
    {
        return rev;
    }

    public String getValidateType()
    {
        return validateType;
    }

    /**
     * Print a string representation of the XML document
     * NOTE: this detects the character encoding, or uses the XML default
     */
    public String toString(String user, String[] groups, boolean withInlinedata)
    {
    	ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            this.toXml(out, user, groups, withInlinedata);
        } catch (McdbException mcdbe) {
            return null;
        }
        String encoding = null;
        String document = null;
        try {
        	XmlStreamReader xsr = new XmlStreamReader(new ByteArrayInputStream(out.toByteArray()));
        	encoding = xsr.getEncoding();
        	document = out.toString(encoding);
        } catch (Exception e) {
        	document = out.toString();
		}
        return document;
    }

    /**
     * Print a string representation of the XML document
     * NOTE: this detects the character encoding, or uses the XML default
     */
    public String toString()
    {
    	ByteArrayOutputStream out = new ByteArrayOutputStream();
    	String userName = null;
        String[] groupNames = null;
        boolean withInlineData = true;
        try {
            this.toXml(out, userName, groupNames, withInlineData);
        } catch (McdbException mcdbe) {
        	logMetacat.warn("Could not convert documentImpl to xml: " + mcdbe.getMessage());
            return null;
        }
        String encoding = null;
        String document = null;
        try {
        	XmlStreamReader xsr = new XmlStreamReader(new ByteArrayInputStream(out.toByteArray()));
        	encoding = xsr.getEncoding();
        	document = out.toString(encoding);
        } catch (Exception e) {
        	document = out.toString();
		}
        return document;
    }
    
	public byte[] getBytes() {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
    	String userName = null;
        String[] groupNames = null;
        boolean withInlineData = true;
        try {
            this.toXml(out, userName, groupNames, withInlineData);
        } catch (McdbException mcdbe) {
        	logMetacat.warn("Could not convert documentImpl to xml: " + mcdbe.getMessage());
            return null;
        }
        return out.toByteArray();
	}

    /**
     * Get a text representation of the XML document as a string This older
     * algorithm uses a recursive tree of Objects to represent the nodes of the
     * tree. Each object is passed the data for the document and searches all of
     * the document data to find its children nodes and recursively build. Thus,
     * because each node reads the whole document, this algorithm is extremely
     * slow for larger documents, and the time to completion is O(N^N) wrt the
     * number of nodes. See toXml() for a better algorithm.
     */
    public String readUsingSlowAlgorithm() throws McdbException
    {
        StringBuffer doc = new StringBuffer();

        // First, check that we have the needed node data, and get it if not
        if (nodeRecordList == null) {
            nodeRecordList = getNodeRecordList(rootnodeid);
        }

        // Create the elements from the downloaded data in the TreeSet
        rootNode = new ElementNode(nodeRecordList, rootnodeid);

        // Append the resulting document to the StringBuffer and return it
        doc.append("<?xml version=\"1.0\"?>\n");

        if (docname != null) {
            if ((doctype != null) && (system_id != null)) {
                doc.append("<!DOCTYPE " + docname + " PUBLIC \"" + doctype
                        + "\" \"" + system_id + "\">\n");
            } else {
                doc.append("<!DOCTYPE " + docname + ">\n");
            }
        }
        doc.append(rootNode.toString());

        return (doc.toString());
    }

    /**
	 * Print a text representation of the XML document to a Writer
	 * 
	 * @param pw
	 *            the Writer to which we print the document Now we decide no
	 *            matter withinInlineData's value, the document will
	 * 
	 */
	public InputStream toXml(OutputStream out, String user, String[] groups, boolean withInLineData)
			throws McdbException {
		String documentDir = null;
		String documentPath = null;
		FileOutputStream fos = null;
		try {
			String separator = PropertyService.getProperty("document.accNumSeparator");
			documentDir = PropertyService.getProperty("application.documentfilepath");
			documentPath = documentDir + FileUtil.getFS() + docid + separator + rev;

			if (FileUtil.getFileStatus(documentPath) == FileUtil.DOES_NOT_EXIST
					|| FileUtil.getFileSize(documentPath) == 0) {
				fos = new FileOutputStream(documentPath);
				toXmlFromDb(fos, user, groups, true);
				fos.close();
			}
		} catch (PropertyNotFoundException pnfe) {
			throw new McdbException("Could not write file: " + documentPath + " : "
					+ pnfe.getMessage());
		} catch (IOException ioe) {
			throw new McdbException("Could not write file: " + documentPath + " : "
					+ ioe.getMessage());
        } finally {
            IOUtils.closeQuietly(fos);
        }
		
		if (FileUtil.getFileSize(documentPath) == 0) {
			throw new McdbException("Attempting to read a zero length document from disk: " + documentPath);
		}
		
		return readFromFileSystem(out, user, groups, documentPath);
	}
    
    /**
	 * Print a text representation of the XML document to a Writer
	 * 
	 * @param pw
	 *            the Writer to which we print the document Now we decide no
	 *            matter withinInlineData's value, the document will
	 * 
	 */
    public void toXmlFromDb(OutputStream outputStream, String user, String[] groups,
            boolean withInLineData) throws McdbException, IOException
    {
        // flag for process eml2
        boolean proccessEml2 = false;
        boolean storedDTD = false;//flag to inidate publicid or system
        // id stored in db or not
        boolean firstElement = true;
        String dbDocName = null;
        String dbPublicID = null;
        String dbSystemID = null;

        if (doctype != null
                && (doctype.equals(EML2_0_0NAMESPACE)
                        || doctype.equals(EML2_0_1NAMESPACE) 
                        || doctype.equals(EML2_1_0NAMESPACE)
                		|| doctype.equals(EML2_1_1NAMESPACE))) {
            proccessEml2 = true;
        }
        // flag for process inline data
        boolean processInlineData = false;

        TreeSet<NodeRecord> nodeRecordLists = null;
        
        // Note: we haven't stored the encoding, so we use the default for XML
        String encoding = "UTF-8";
        Writer out = new OutputStreamWriter(outputStream, encoding);
       
        // Here add code to handle subtree access control
        /*
         * PermissionController control = new PermissionController(docid);
         * Hashtable unaccessableSubTree =control.hasUnaccessableSubTree(user,
         * groups, AccessControlInterface.READSTRING);
         *
         * if (!unaccessableSubTree.isEmpty()) {
         *
         * nodeRecordLists = getPartNodeRecordList(rootnodeid,
         * unaccessableSubTree);
         *  } else { nodeRecordLists = getNodeRecordList(rootnodeid); }
         */
        
        if(this.nodeRecordList == null){
            nodeRecordLists = getNodeRecordList(rootnodeid);
        } else {
        	nodeRecordLists = this.nodeRecordList;
        }
        Stack<NodeRecord> openElements = new Stack<NodeRecord>();
        boolean atRootElement = true;
        boolean previousNodeWasElement = false;

        // Step through all of the node records we were given

        Iterator<NodeRecord> it = nodeRecordLists.iterator();

        while (it.hasNext()) {

            NodeRecord currentNode = it.next();
            logMetacat.debug("[Got Node ID: " + currentNode.getNodeId() + " ("
                    + currentNode.getParentNodeId() + ", " + currentNode.getNodeIndex()
                    + ", " + currentNode.getNodeType() + ", " + currentNode.getNodeName()
                    + ", " + currentNode.getNodeData() + ")]");
            // Print the end tag for the previous node if needed
            //
            // This is determined by inspecting the parent nodeid for the
            // currentNode. If it is the same as the nodeid of the last element
            // that was pushed onto the stack, then we are still in that
            // previous
            // parent element, and we do nothing. However, if it differs, then
            // we
            // have returned to a level above the previous parent, so we go into
            // a loop and pop off nodes and print out their end tags until we
            // get
            // the node on the stack to match the currentNode parentnodeid
            //
            // So, this of course means that we rely on the list of elements
            // having been sorted in a depth first traversal of the nodes, which
            // is handled by the NodeComparator class used by the TreeSet
            if (!atRootElement) {
                NodeRecord currentElement = openElements.peek();
                if (currentNode.getParentNodeId() != currentElement.getNodeId()) {
                    while (currentNode.getParentNodeId() != currentElement.getNodeId()) {
                        currentElement = (NodeRecord) openElements.pop();
                        logMetacat.debug("\n POPPED: "
                                + currentElement.getNodeName());
                        if (previousNodeWasElement) {
                            out.write(">");
                            previousNodeWasElement = false;
                        }
                        if (currentElement.getNodePrefix() != null) {
                            out.write("</" + currentElement.getNodePrefix() + ":"
                                    + currentElement.getNodeName() + ">");
                        } else {
                            out.write("</" + currentElement.getNodeName() + ">");
                        }
                        currentElement = openElements.peek();
                    }
                }
            }

            // Handle the DOCUMENT node
            if (currentNode.getNodeType().equals("DOCUMENT")) {
                out.write("<?xml version=\"1.0\"?>");

                // Handle the ELEMENT nodes
            } else if (currentNode.getNodeType().equals("ELEMENT")) {
                if (atRootElement) {
                    atRootElement = false;
                } else {
                    if (previousNodeWasElement) {
                        out.write(">");
                    }
                }

                // if publicid or system is not stored into db send it out by
                // default
                if (!storedDTD & firstElement) {
                    if (docname != null && validateType != null
                            && validateType.equals(DTD)) {
                        if ((doctype != null) && (system_id != null)) {

                            out.write("<!DOCTYPE " + docname + " PUBLIC \""
                                    + doctype + "\" \"" + system_id + "\">");
                        } else {

                            out.write("<!DOCTYPE " + docname + ">");
                        }
                    }
                }
                firstElement = false;
                openElements.push(currentNode);
                logMetacat.debug("\n PUSHED: " + currentNode.getNodeName());
                previousNodeWasElement = true;
                if (currentNode.getNodePrefix() != null) {
                    out.write("<" + currentNode.getNodePrefix() + ":"
                            + currentNode.getNodeName());
                } else {
                    out.write("<" + currentNode.getNodeName());
                }

                // if currentNode is inline and handle eml2, set flag process
                // on
                if (currentNode.getNodeName() != null
                        && currentNode.getNodeName().equals(Eml200SAXHandler.INLINE)
                        && proccessEml2) {
                	processInlineData = true;
                }

                // Handle the ATTRIBUTE nodes
            } else if (currentNode.getNodeType().equals("ATTRIBUTE")) {
                if (currentNode.getNodePrefix() != null) {
                    out.write(" " + currentNode.getNodePrefix() + ":"
                            + currentNode.getNodeName() + "=\""
                            + currentNode.getNodeData() + "\"");
                } else {
                    out.write(" " + currentNode.getNodeName() + "=\""
                            + currentNode.getNodeData() + "\"");
                }

                // Handle the NAMESPACE nodes
            } else if (currentNode.getNodeType().equals("NAMESPACE")) {
                String nsprefix = " xmlns:";
                if(currentNode.getNodeName() == null || currentNode.getNodeName().trim().equals(""))
                {
                  nsprefix = " xmlns";
                }
                
                out.write(nsprefix + currentNode.getNodeName() + "=\""
                          + currentNode.getNodeData() + "\"");

                // Handle the TEXT nodes
            } else if (currentNode.getNodeType().equals("TEXT")) {
                if (previousNodeWasElement) {
                    out.write(">");
                }
                if (!processInlineData) {
                    // if it is not inline data just out put data
                    out.write(currentNode.getNodeData());
                } else {
                    // if it is inline data first to get the inline data
                    // internal id
                    String fileName = currentNode.getNodeData();
                    // use full docid with revision
                    String accessfileName = fileName; //DocumentUtil.getDocIdWithoutRevFromInlineDataID(fileName);
                    
                    // check if user has read permision for this inline data
                    boolean readInlinedata = false;
                    try {
                        Hashtable<String, String> unReadableInlineDataList =
                            PermissionController.getUnReadableInlineDataIdList(accessfileName, user, groups);
                        if (!unReadableInlineDataList.containsValue(fileName)) {
                            readInlinedata = true;
                        }
                    } catch (Exception e) {
                        throw new McdbException(e.getMessage());
                    }

                    if (readInlinedata) {
                        //user want to see it, pull out from file system and 
                    	// output it for inline data, the data base only store 
                    	// the file name, so we can combine the file name and
                    	// inline data file path, to get it

                        Reader reader = Eml200SAXHandler
                                .readInlineDataFromFileSystem(fileName, encoding);
                        char[] characterArray = new char[4 * 1024];
                        try {
                            int length = reader.read(characterArray);
                            while (length != -1) {
                                out.write(new String(characterArray, 0,
                                                length));
                                out.flush();
                                length = reader.read(characterArray);
                            }
                            reader.close();
                        } catch (IOException e) {
                            throw new McdbException(e.getMessage());
                        }
                    }//if can read inline data
                    else {
                        // if user can't read it, we only send it back a empty
                        // string in inline element.
                        out.write("");
                    }// else can't read inlinedata
                    // reset proccess inline data false
                    processInlineData = false;
                }// in inlinedata part
                previousNodeWasElement = false;
                // Handle the COMMENT nodes
            } else if (currentNode.getNodeType().equals("COMMENT")) {
                if (previousNodeWasElement) {
                    out.write(">");
                }
                out.write("<!--" + currentNode.getNodeData() + "-->");
                previousNodeWasElement = false;

                // Handle the PI nodes
            } else if (currentNode.getNodeType().equals("PI")) {
                if (previousNodeWasElement) {
                    out.write(">");
                }
                out.write("<?" + currentNode.getNodeName() + " "
                        + currentNode.getNodeData() + "?>");
                previousNodeWasElement = false;
                // Handle the DTD nodes (docname, publicid, systemid)
            } else if (currentNode.getNodeType().equals(DTD)) {
                storedDTD = true;
                if (currentNode.getNodeName().equals(DOCNAME)) {
                    dbDocName = currentNode.getNodeData();
                }
                if (currentNode.getNodeName().equals(PUBLICID)) {
                    dbPublicID = currentNode.getNodeData();
                }
                if (currentNode.getNodeName().equals(SYSTEMID)) {
                    dbSystemID = currentNode.getNodeData();
                    // send out <!doctype .../>
                    if (dbDocName != null) {
                        if ((dbPublicID != null) && (dbSystemID != null)) {

                            out
                                    .write("<!DOCTYPE " + dbDocName
                                            + " PUBLIC \"" + dbPublicID
                                            + "\" \"" + dbSystemID + "\">");
                        } else {

                            out.write("<!DOCTYPE " + dbDocName + ">");
                        }
                    }

                    //reset these variable
                    dbDocName = null;
                    dbPublicID = null;
                    dbSystemID = null;
                }

                // Handle any other node type (do nothing)
            } else {
                // Any other types of nodes are not handled.
                // Probably should throw an exception here to indicate this
            }
            
            out.flush();
        }

        // Print the final end tag for the root element
        while (!openElements.empty()) {
            NodeRecord currentElement = (NodeRecord) openElements.pop();
            logMetacat.debug("\n POPPED: " + currentElement.getNodeName());
            if (currentElement.getNodePrefix() != null) {
                out.write("</" + currentElement.getNodePrefix() + ":"
                        + currentElement.getNodeName() + ">");
            } else {
                out.write("</" + currentElement.getNodeName() + ">");
            }
        }
        out.flush();
    }
    
    /**
	 * Read the XML document from the file system and write to a Writer. Strip
	 * out any inline data that the user does not have permission to read.
	 * 
	 * @param pw
	 *            the Writer to which we print the document
	 * @param user
	 *            the user we will use to verify inline data access
	 * @param groups
	 *            the groups we will use to verify inline data access
	 * @param documentPath
	 *            the location of the document on disk
	 * 
	 */
    public InputStream readFromFileSystem(
    		OutputStream out, String user, String[] groups, String documentPath) throws McdbException {
        
		String xmlFileContents = null;
		String encoding = null;
		
		try {
			
			// get a list of inline data sections that are not readable
			// by this user
			String fullDocid = docid + "." + rev;
            Hashtable<String, String> unReadableInlineDataList =
                PermissionController.getUnReadableInlineDataIdList(fullDocid, user, groups);
            
            // If this is for each unreadable section, strip the inline data
			// from the doc
			if (unReadableInlineDataList.size() > 0 && doctype != null) {
				
				// detect and use correct encoding
	            xmlFileContents = FileUtil.readFileToString(documentPath);
	            // guess the encoding from default bytes
				XmlStreamReader xsr = new XmlStreamReader(new ByteArrayInputStream(xmlFileContents.getBytes()));
				encoding = xsr.getEncoding();
				xsr.close();
				// reread the contents using the correct encoding
				if (encoding != null) {
					xmlFileContents = FileUtil.readFileToString(documentPath, encoding);
				}
				
				Set<String> inlineKeySet = unReadableInlineDataList.keySet();
				boolean pre210Doc = doctype.equals(EML2_0_0NAMESPACE)
						|| doctype.equals(EML2_0_1NAMESPACE);

				for (String inlineKey : inlineKeySet) {
					String inlineValue = unReadableInlineDataList.get(inlineKey);
					if (inlineValue.startsWith(docid)) {
						// There are two different methods of stripping inline data depending
						// on whether this is a 2.0.1 or earlier doc or 2.1.0 and later.  This 
						// is because of eml schema changes for inline access.
						if (pre210Doc) {
							xmlFileContents = stripInline20XData(xmlFileContents, inlineKey);
						} else {
							xmlFileContents = stripInline21XData(xmlFileContents, inlineKey);
						}
					}
				}
			}
            
			// will get input either from string content or file on disk
			InputStream is = null;
			
			// get the input stream
			if (xmlFileContents != null) {
				is = IOUtils.toInputStream(xmlFileContents, encoding);
			} else {
				is = new FileInputStream(documentPath);
			}

			// send it to out
			if (out != null) {
				IOUtils.copyLarge(is, out);
			}
			// return the stream
			return is;
			
	     } catch (UtilException e) {
             throw new McdbException(e.getMessage());
         } catch (IOException e) {
             throw new McdbException(e.getMessage());
        }
		
    }
    
    /**
	 * Write an XML document to the file system.
	 * 
	 * @param xml
	 *            the document we want to write out
	 * @param accNumber
	 *            the document id which is used to name the output file
	 */
    private static void writeToFileSystem(String xml, String accNumber, String encoding) throws McdbException {

		// write the document to disk
		String documentDir = null;
		String documentPath = null;

		try {				
			documentDir = PropertyService.getProperty("application.documentfilepath");
			documentPath = documentDir + FileUtil.getFS() + accNumber;

			if (xml == null || xml.equals("")) {
				throw new McdbException("Attempting to write a file with no xml content: " + documentPath);
			}
			
			if (accNumber == null) {
				throw new McdbException("Could not write document file.  Accession Number number is null" );
			}
			
			if (FileUtil.getFileStatus(documentPath) >= FileUtil.EXISTS_ONLY) {
				throw new McdbException("The file you are trying to write already exists "
                        + " in metacat.  Please update your version number.");
			}
			
			if (accNumber != null
					&& (FileUtil.getFileStatus(documentPath) == FileUtil.DOES_NOT_EXIST 
							|| FileUtil.getFileSize(documentPath) == 0)) {

			    FileOutputStream fos = null;
                try {
                    fos = new FileOutputStream(documentPath);
                    IOUtils.write(xml.getBytes(encoding), fos);

                    fos.flush();
                    fos.close();
                } catch (IOException ioe) {
                    throw new McdbException("Could not write file: " + documentPath + " : " + ioe.getMessage());
                } finally {
                    IOUtils.closeQuietly(fos);
                }
			}			

		} catch (PropertyNotFoundException pnfe) {
			throw new McdbException("Could not write file: " + documentPath + " : "
					+ pnfe.getMessage());
		}
	}	
    
    /**
     * Deletes a doc or data file from the filesystem using the accession number.
     * 
     * @param accNumber
     * @param isXml
     * @throws McdbException
     */
    private static void deleteFromFileSystem(String accNumber, boolean isXml) throws McdbException {

    	// must have an id
    	if (accNumber == null) {
			throw new McdbException("Could not delete file.  Accession Number number is null" );
		}
    	
		// remove the document from disk
		String documentDir = null;
		String documentPath = null;

		try {
			// get the correct location on disk
			if (isXml) {
				documentDir = PropertyService.getProperty("application.documentfilepath");
			} else {
				documentDir = PropertyService.getProperty("application.datafilepath");
			}
			documentPath = documentDir + FileUtil.getFS() + accNumber;

			// delete it if it exists			
			if (accNumber != null && FileUtil.getFileStatus(documentPath) != FileUtil.DOES_NOT_EXIST) {
			    try {
			    	FileUtil.deleteFile(documentPath);
			    } catch (IOException ioe) {
			        throw new McdbException("Could not delete file: " + documentPath + " : " + ioe.getMessage());
			    }
			}			
		} catch (PropertyNotFoundException pnfe) {
			throw new McdbException(pnfe.getClass().getName() + ": Could not delete file because: " 
					+ documentPath + " : " + pnfe.getMessage());
		}
	}
    
    /**
	 * Strip out an inline data section from a 2.0.X version document. This assumes 
	 * that the inline element is within a distribution element and the id for the
	 * distribution is the same as the subtreeid in the xml_access table.
	 * 
	 * @param xmlFileContents
	 *            the contents of the file
	 * @param inLineKey
	 *            the unique key for this inline element
	 */
    private String stripInline20XData(String xmlFileContents, String inLineId)
			throws McdbException {
		String changedString = xmlFileContents;
		
    	Pattern distStartPattern = Pattern.compile("<distribution", Pattern.CASE_INSENSITIVE); 
    	Pattern distEndPattern = Pattern.compile("</distribution>", Pattern.CASE_INSENSITIVE);
    	Pattern idPattern = Pattern.compile("id=\"" + inLineId);
    	Pattern inlinePattern = Pattern.compile("<inline>.*</inline>");
    	
    	Matcher distStartMatcher = distStartPattern.matcher(xmlFileContents);
    	Matcher distEndMatcher = distEndPattern.matcher(xmlFileContents);
    	Matcher idMatcher = idPattern.matcher(xmlFileContents);
    	Matcher inlineMatcher = inlinePattern.matcher(xmlFileContents);
    	 
    	// loop through the document looking for distribution elements
    	while (distStartMatcher.find()) {
    		// find the index of the corresponding end element
    		int distStart = distStartMatcher.end();
        	if (!distEndMatcher.find(distStart)) {
        		throw new McdbException("Could not find end tag for distribution");
        	}
        	int distEnd = distEndMatcher.start();
        	
        	// look for our inline id within the range of this distribution.
        	idMatcher.region(distStart, distEnd);
        	if (idMatcher.find()) {
        		// if this distribution has the desired id, replace any 
        		// <inline>.*</inline> in this distribution with <inline></inline>
        		inlineMatcher.region(distStart, distEnd);
            	if (inlineMatcher.find()) {
            		changedString = inlineMatcher.replaceAll("<inline></inline>");
            	} else {
            		logMetacat.warn("Could not find an inline element for distribution: " + inLineId);
            	}
        	}
    		
    	}

		return changedString;
	}
    
    /**
	 * Strip out an inline data section from a 2.1.X version document. This
	 * assumes that the inline element is within a distribution element and the
	 * subtreeid in the xml_access table is an integer that represents the nth
	 * distribution element in the document.
	 * 
	 * @param xmlFileContents
	 *            the contents of the file
	 * @param inLineKey
	 *            the unique key for this inline element
	 */
    private String stripInline21XData(String xmlFileContents, String inLineId) throws McdbException {
    	int distributionIndex = Integer.valueOf(inLineId);
    	String changedString = xmlFileContents;
    	Pattern distStartPattern = Pattern.compile("<distribution", Pattern.CASE_INSENSITIVE); 
    	Pattern distEndPattern = Pattern.compile("</distribution>", Pattern.CASE_INSENSITIVE); 
    	Pattern inlinePattern = Pattern.compile("<inline>.*</inline>");
    	Matcher matcher = distStartPattern.matcher(xmlFileContents);
    	
    	// iterate through distributions until we find the nth match.  The nth match 
    	// corresponds to the inLineKey that was passed in.  Use the end of that match
    	// to set the start range we will search for the inline element.
    	for (int i = 0; i < distributionIndex; i++) {
    		if (!matcher.find()) {
    			throw new McdbException("Could not find distribution number " + (i + 1));
    		}
    	}	
    	int distStart = matcher.end();
    	
    	// find the end tag for the distribution.  Use the beginning of that match to set
    	// the end range we will search for the inline element
    	matcher.usePattern(distEndPattern);
    	if (!matcher.find(distStart)) {
    		throw new McdbException("Could not find end tag for distribution");
    	}
    	int distEnd = matcher.start();
    	
    	// set the inline search range
    	matcher.region(distStart, distEnd);
    	
    	// match any inline elements and replace with an empty inline element
    	matcher.usePattern(inlinePattern);  	
    	if (matcher.find()) {
    		changedString = matcher.replaceAll("<inline></inline>");
    	} else {
    		logMetacat.warn("Could not find an inline element for distribution: " 
    				+ inLineId);
    	}
    	
    	return changedString;
    }
    
//    private static String readerToString(Reader reader) throws IOException {
//		String xmlString = "";
//		int tmp = reader.read();
//		while (tmp != -1) {
//			xmlString += (char) tmp;
//			tmp = reader.read();
//		}
//		
//		reader.reset();
//		return xmlString;
//	}

    /**
	 * Build the index records for this document. For each node, all absolute
	 * and relative paths to the root of the document are created and inserted
	 * into the xml_index table. This requires that the DocumentImpl instance
	 * exists, so first call the constructor that reads the document from the
	 * database.
	 * 
	 * @throws McdbException
	 *             on error getting the node records for the document
	 */
    public void buildIndex() throws McdbException
    {
        //if the pathquery option is off, we don't need to build the index.
        if(!EnabledQueryEngines.getInstance().isEnabled(EnabledQueryEngines.PATHQUERYENGINE)) {
            return;
        }
    	logMetacat.info("DocumentImpl.buildIndex - building index for docid " + docid);
    	double start = System.currentTimeMillis()/1000;
        TreeSet<NodeRecord> nodeRecordLists = getNodeRecordList(rootnodeid);
        boolean atRootElement = true;
        long rootNodeId = -1;

        // Build a map of the same records that are present in the
        // TreeSet so that any node can be easily accessed by nodeId
        HashMap<Long, NodeRecord> nodeRecordMap = new HashMap<Long, NodeRecord>();
        Iterator<NodeRecord> it = nodeRecordLists.iterator();
        while (it.hasNext()) {
            NodeRecord currentNode = (NodeRecord) it.next();
            Long nodeId = new Long(currentNode.getNodeId());
            nodeRecordMap.put(nodeId, currentNode);
        }

//        String doc = docid;
      double afterPutNode = System.currentTimeMillis()/1000;
      logMetacat.debug("DocumentImpl.buildIndex - The time to put node id into map is " + (afterPutNode - start));
      double afterDelete = 0;
        // Opening separate db connection for deleting and writing
        // XML Index -- be sure that it is all in one db transaction
        int serialNumber = -1;
        DBConnection dbConn = null;
        try {
            dbConn = DBConnectionPool.getDBConnection("DocumentImpl.buildIndex");
            serialNumber = dbConn.getCheckOutSerialNumber();
            dbConn.setAutoCommit(false);
            //make sure record is done
            //checkDocumentTable();

            // Delete the previous index entries for this document
            deleteNodeIndex(dbConn);
            afterDelete = System.currentTimeMillis()/1000;
            // Step through all of the node records we were given
            // and build the new index and update the database. Process
            // TEXT nodes with their parent ELEMENT node ids to associate the
            // element with it's node data (stored in the text node)
            it = nodeRecordLists.iterator();
            HashMap<String, PathIndexEntry> pathsFound = new HashMap<String, PathIndexEntry>();
            while (it.hasNext()) {
                NodeRecord currentNode = (NodeRecord) it.next();
                HashMap<String, PathIndexEntry> pathList = new HashMap<String, PathIndexEntry>();
                if ( currentNode.getNodeType().equals("ELEMENT") ||
                     currentNode.getNodeType().equals("ATTRIBUTE") ){

                    if (atRootElement) {
                        rootNodeId = currentNode.getNodeId();
                        atRootElement = false;
                    }
                    traverseParents(nodeRecordMap, rootNodeId,
                                    currentNode.getNodeId(),
                                    currentNode.getNodeId(), 
                                    "", pathList, pathsFound);

                    updateNodeIndex(dbConn, pathList);
                } else if ( currentNode.getNodeType().equals("TEXT") ) {
                  
                  // A non-empty TEXT node represents the node data of its
                  // parent ELEMENT node.  Traverse the parents starting from 
                  // the TEXT node.  The xml_path_index table will be populated 
                  // with ELEMENT paths with TEXT nodedata (since it's modeled 
                  // this way in the DOM)
                  NodeRecord parentNode = 
                	  nodeRecordMap.get(new Long(currentNode.getParentNodeId()));

                  if ( parentNode.getNodeType().equals("ELEMENT") ) {
                    
                    currentNode.setNodeType(parentNode.getNodeType());
                    currentNode.setNodeName("");
                    logMetacat.debug("DocumentImpl.buildIndex - Converted node " + currentNode.getNodeId() + 
                      " to type " + parentNode.getNodeType());
                    
                  	traverseParents(nodeRecordMap, rootNodeId,
                                    currentNode.getNodeId(),
                                    currentNode.getNodeId(),
                                    "", pathList, pathsFound);
                  }
                }
                // Lastly, update the xml_path_index table
                if(!pathsFound.isEmpty()){
                	logMetacat.debug("DocumentImpl.buildIndex - updating path index");
                    	
                	updatePathIndex(dbConn, pathsFound);

                	pathsFound.clear();
                }
            }
            
            dbConn.commit();
        } catch (SQLException sqle) {
            logMetacat.error("DocumentImpl.buildIndex - SQL Exception while indexing "
            		+ "document " + docid + " : " + sqle.getMessage());
            try {
                dbConn.rollback();
            } catch (SQLException sqle2) {
                logMetacat.error("DocumentImpl.buildIndex - Error while rolling back: "
                		 + sqle2.getMessage());
            }
            throw new McdbException("SQL error when building Index: " + sqle.getMessage());
        } finally {
			DBConnectionPool.returnDBConnection(dbConn, serialNumber);
		}
		double finish = System.currentTimeMillis() / 1000;
		logMetacat.info("DocumentImpl.buildIndex - The time for inserting is " + (finish - afterDelete));
		logMetacat.info("DocumentImpl.buildIndex - BuildIndex complete for docid " + docid);

		// Adds the docid to the spatial data cache
		try {
			if (PropertyService.getProperty("spatial.runSpatialOption").equals("true")) {
				SpatialHarvester spatialHarvester = new SpatialHarvester();
				logMetacat.debug("DocumentImpl.buildIndex -  Attempting to update the spatial cache for docid "
								+ docid);
				spatialHarvester.addToUpdateQue(docid);
				spatialHarvester.destroy();
				logMetacat.debug("DocumentImpl.buildIndex - Finished updating the spatial cache for docid "
								+ docid);
			}
		} catch (PropertyNotFoundException pnfe) {
			logMetacat.error("DocumentImpl.buildIndex - Could not get 'runSpatialOption' property.  Spatial " 
					+ "cache not run for docid: " + docid + " : " + pnfe.getMessage());
		} catch (StringIndexOutOfBoundsException siobe) {
			logMetacat.error("DocumentImpl.buildIndex -  String indexing problem.  Spatial " 
					+ "cache not run for docid: " + docid + " : " + siobe.getMessage());		
		}
	}

    /**
	 * Recurse up the parent node hierarchy and add each node to the hashmap of
	 * paths to be indexed. Note: pathsForIndexing is a hash map of paths
	 * 
	 * @param records
	 *            the set of records hashed by nodeId
	 * @param rootNodeId
	 *            the id of the root element of the document
	 * @param leafNodeId
	 *            the id of the leafNode being processed
	 * @param id
	 *            the id of the current node to be processed
	 * @param children
	 *            the string representation of all child nodes of this id
	 * @param pathList
	 *            the hash to which paths are added
	 * @param nodedata
	 *            the nodedata for the current node
	 */
    private void traverseParents(HashMap<Long,NodeRecord> records, long rootNodeId,
            long leafNodeId, long id,String children, 
            HashMap<String, PathIndexEntry> pathList, 
            HashMap<String, PathIndexEntry> pathsFoundForIndexing) {
    	Long nodeId = new Long(id);
        NodeRecord current = (NodeRecord)records.get(nodeId);
        long parentId = current.getParentNodeId();
        String currentName = current.getNodeName();
//        String nodeData = current.getNodeData();
//        float nodeDataNumerical = current.getNodeDataNumerical();
        NodeRecord leafRecord = (NodeRecord)records.get(new Long(leafNodeId));
        String leafData = leafRecord.getNodeData();
        long leafParentId = leafRecord.getParentNodeId();
        float leafDataNumerical = leafRecord.getNodeDataNumerical();
        Timestamp leafDataDate = leafRecord.getNodeDataDate();
        
        if ( current.getNodeType().equals("ELEMENT") ||
            current.getNodeType().equals("ATTRIBUTE") ) {
        	  
        	  // process leaf node xpaths
            if (children.equals("")) {
                if (current.getNodeType().equals("ATTRIBUTE")) {
                    currentName = "@" + currentName;
                }
                logMetacat.debug("DocumentImpl.traverseParents - A: " + currentName +"\n");
                if ( currentName != null ) {
                  if ( !currentName.equals("") ) {
                    pathList.put(currentName, new PathIndexEntry(leafNodeId,
                      currentName, docid, doctype, parentId));
                  }
                }
				if (pathsForIndexing.contains(currentName)
						&& leafData.trim().length() != 0) {
					logMetacat.debug("DocumentImpl.traverseParents - paths found for indexing: " + currentName);
					pathsFoundForIndexing.put(currentName, new PathIndexEntry(
							leafNodeId, currentName, docid, leafParentId, leafData,
							leafDataNumerical, leafDataDate));
				}
            }
            
            // process relative xpaths
            if ( !currentName.equals("") ) {
              currentName = "/" + currentName;
              currentName = currentName + children;
            }
            if (parentId != 0) {
                traverseParents(records, rootNodeId, leafNodeId,
                    parentId, currentName, pathList, pathsFoundForIndexing);
            }
            String path = current.getNodeName() + children;
            
            if ( !children.equals("") ) {
                logMetacat.debug("DocumentImpl.traverseParents - B: " + path +"\n");
                pathList.put(path, new PathIndexEntry(leafNodeId, path, docid,
                    doctype, parentId));
				if (pathsForIndexing.contains(path)
						&& leafData.trim().length() != 0) {
					logMetacat.debug("DocumentImpl.traverseParents - paths found for indexing: " + currentName);
					pathsFoundForIndexing.put(path, new PathIndexEntry(leafNodeId,
							path, docid, leafParentId, leafData, leafDataNumerical, leafDataDate));
				}
            }
            // process absolute xpaths
            if (id == rootNodeId) {
                String fullPath = "";
                if ( !path.equals("") ) {
                  fullPath = '/' + path;
                }
                logMetacat.debug("DocumentImpl.traverseParents - C: " + fullPath +"\n");
                pathList.put(fullPath, new PathIndexEntry(leafNodeId, fullPath,
                    docid, doctype, parentId));

				if (pathsForIndexing.contains(fullPath)
						&& leafData.trim().length() != 0) {
					logMetacat.debug("DocumentImpl.traverseParents - paths found for indexing: " + currentName);
					pathsFoundForIndexing.put(fullPath, new PathIndexEntry(
							leafNodeId, fullPath, docid, leafParentId, leafData,
							leafDataNumerical, leafDataDate));
				}
            }
        } 
    }

    /**
	 * Delete the paths from the xml_index table on the database in preparation
	 * of a subsequent update.
	 * 
	 * @param conn
	 *            the database connection to use, keeping a single transaction
	 * @throws SQLException
	 *             if there is an error deleting from the db
	 */
    private void deleteNodeIndex(DBConnection conn) throws SQLException
    {
        //String familyId = MetacatUtil.getDocIdFromString(docid);
    	double start = System.currentTimeMillis()/1000;
        String familyId = docid;
        String sql = "DELETE FROM xml_index WHERE docid = ?";

        PreparedStatement pstmt = conn.prepareStatement(sql);

        // Increase usage count for the connection
        conn.increaseUsageCount(1);

        // Execute the delete and close the statement
        pstmt.setString(1, familyId);
        logMetacat.debug("DocumentImpl.deleteNodeIndex - executing SQL: " + pstmt.toString());
        int rows = pstmt.executeUpdate();
        pstmt.close();
        logMetacat.debug("DocumentImpl.deleteNodeIndex - Deleted " + rows + " rows from xml_index " +
            "for document " + docid);
        double afterDeleteIndex = System.currentTimeMillis()/1000;
        logMetacat.debug("DocumentImpl.deleteNodeIndex - The delete index time is "+(afterDeleteIndex - start));
        // Delete all the entries in xml_queryresult
        
//        pstmt = conn.prepareStatement(
//                "DELETE FROM xml_queryresult WHERE docid = ?");
//        pstmt.setString(1, docid);
//        rows = pstmt.executeUpdate();
//        conn.increaseUsageCount(1);
//        pstmt.close();
        try {
            XMLQueryresultAccess xmlQueryresultAccess = new XMLQueryresultAccess();
            xmlQueryresultAccess.deleteXMLQueryresulForDoc(docid);
        } catch (AccessException ae) {
        	throw new SQLException("Problem deleting xml query result for docid " + 
        			docid + " : " + ae.getMessage());
        }
        logMetacat.debug("DocumentImpl.deleteNodeIndex - Deleted " + rows + " rows from xml_queryresult " +
                "for document " + docid);
        double afterDeleteQueryResult = System.currentTimeMillis()/1000;
        logMetacat.debug("DocumentImpl.deleteNodeIndex - The delete query result time is "+(afterDeleteQueryResult - afterDeleteIndex ));
        // Delete all the entries in xml_path_index
        pstmt = conn.prepareStatement(
                "DELETE FROM xml_path_index WHERE docid = ?");
        pstmt.setString(1, docid);
        logMetacat.debug("DocumentImpl.deleteNodeIndex - executing SQL: " + pstmt.toString());
        rows = pstmt.executeUpdate();
        conn.increaseUsageCount(1);
        pstmt.close();
        double afterDeletePathIndex = System.currentTimeMillis()/1000;
        logMetacat.info("DocumentImpl.deleteNodeIndex - The delete path index time is "+ (afterDeletePathIndex - afterDeleteQueryResult));
        logMetacat.info("DocumentImpl.deleteNodeIndex - Deleted " + rows + " rows from xml_path_index " +
                "for document " + docid);

    }

    /**
	 * Insert the paths from the pathList into the xml_index table on the
     * database.
     *
     * @param conn the database connection to use, keeping a single transaction
     * @param pathList the hash of paths to insert
     * @throws SQLException if there is an error inserting into the db
     */
    private void updateNodeIndex(DBConnection conn, HashMap<String, PathIndexEntry> pathList)
    	throws SQLException
    {
        // Create an insert statement to reuse for all of the path
        // insertions
        PreparedStatement pstmt = conn.prepareStatement(
                "INSERT INTO xml_index (nodeid, path, docid, doctype, " +
                "parentnodeid) " + "VALUES (?, ?, ?, ?, ?)");
        // Increase usage count for the connection
        conn.increaseUsageCount(1);
        String familyId = docid;
        pstmt.setString(3, familyId);
        pstmt.setString(4, doctype);

        // Step through the hashtable and insert each of the path values
        Iterator<PathIndexEntry> it = pathList.values().iterator();
        while (it.hasNext()) {
        	 PathIndexEntry entry = (PathIndexEntry)it.next();
        	 pstmt.setLong(1, entry.nodeId);
        	 pstmt.setString(2, entry.path);
        	 pstmt.setLong(5, entry.parentId);
        	 logMetacat.debug("DocumentImpl.updateNodeIndex - executing SQL: " + pstmt.toString());
        	 pstmt.executeUpdate();
         }
        // Close the database statement
        pstmt.close();
    }

    /**
     * Insert the paths from the pathList into the xml_path_index table on the
     * database.
     *
     * @param conn the database connection to use, keeping a single transaction
     * @param pathList the hash of paths to insert
     * @throws SQLException if there is an error inserting into the db
     */
    private void updatePathIndex(DBConnection conn, HashMap<String, PathIndexEntry> pathsFound)
        throws SQLException {
        // Increase usage count for the connection
        conn.increaseUsageCount(1);
        
        // Create an insert statement to reuse for all of the path
        // insertions
        PreparedStatement pstmt = conn.prepareStatement("INSERT INTO "
                + "xml_path_index (docid, path, nodedata, "
                + "nodedatanumerical, nodedatadate, parentnodeid)"
                + " VALUES (?, ?, ?, ?, ?, ?)");
        
        // Step through the hashtable and insert each of the path values
        Iterator<PathIndexEntry> it = pathsFound.values().iterator();
         while (it.hasNext()) {
             PathIndexEntry entry = (PathIndexEntry)it.next();
             if (entry.path.length() > 2784) {
            	 logMetacat.warn("DocumentImpl.updatePathIndex - the path for doc id " + entry.docid + 
            			 " is too long and will db break indexing.  This path was not indexed: " + entry.path);
            	 continue;
             }
             if (entry.nodeData.length() > 2784) {
            	 logMetacat.warn("DocumentImpl.updatePathIndex - the node data for doc id " + entry.docid + 
            			 " is too long and will break db indexing.  This path was not indexed: " + entry.path);
            	 continue;
             }
             pstmt.setString(1,entry.docid);
             pstmt.setString(2, entry.path);
             pstmt.setString(3, entry.nodeData);
             pstmt.setFloat(4, entry.nodeDataNumerical);
             pstmt.setTimestamp(5, entry.nodeDataDate);
             pstmt.setLong(6, entry.parentId);  
             logMetacat.debug("DocumentImpl.updatePathIndex - executing SQL: " + pstmt.toString());
             pstmt.execute();
        }
        // Close the database statement
        pstmt.close();
    }

    private boolean isRevisionOnly(String docid, int revision) throws Exception
    {
        //System.out.println("inRevisionOnly given "+ docid + "."+ revision);
        DBConnection dbconn = null;
        int serialNumber = -1;
        PreparedStatement pstmt = null;
        String newid = docid;

        try {
            dbconn = DBConnectionPool
                    .getDBConnection("DocumentImpl.isRevisionOnly");
            serialNumber = dbconn.getCheckOutSerialNumber();
            pstmt = dbconn.prepareStatement("select rev from xml_documents "
                    + "where docid like ?");
            pstmt.setString(1, newid);
            logMetacat.debug("DocumentImpl.isRevisionOnly - executing SQL: " + pstmt.toString());
            pstmt.execute();
            ResultSet rs = pstmt.getResultSet();
            boolean tablehasrows = rs.next();
            //if (rev.equals("newest") || rev.equals("all")) { return false; }
            
            if (tablehasrows) {
                int r = rs.getInt(1);
                //System.out.println("the rev in xml_documents table is "+r);
                pstmt.close();
                if (revision == r) { //the current revision
                                                        // in in xml_documents
                    //System.out.println("returning false");
                    return false;
                } else if (revision < r) { //the current
                                                              // revision is in
                                                              // xml_revisions.
                    //System.out.println("returning true");
                    return true;
                } else if (revision > r) { //error, rev
                                                              // cannot be
                                                              // greater than r
                throw new Exception(
                        "requested revision cannot be greater than "
                                + "the latest revision number."); }
            }
            else
            {
                //System.out.println("in revision table branch -------");
                // if we couldn't find it in xml_documents we 
                // need to find it in xml_revision table
                Vector<Integer> revList = DBUtil.getRevListFromRevisionTable(docid);
                /*for (int i=0; i<revList.size(); i++)
                {
                    //  Integer k = (Integer) revList.elementAt(i);
                    // System.out.println("The rev in xml_revision table "+ k.toString());
                }
                
                if (revList.contains(new Integer(revision)))
                {
                   return true;     
                }*/
                if(revList != null && !revList.isEmpty())
                {
                  return true;
                }
            }
            // Get miss docid and rev, throw to McdDocNotFoundException
            String missDocId = docid;
            String missRevision = (new Integer(revision)).toString();
            throw new McdbDocNotFoundException("the requested docid '"
                    + docid.toString() + "' does not exist", missDocId,
                    missRevision);
        }//try
        finally {
            pstmt.close();
            DBConnectionPool.returnDBConnection(dbconn, serialNumber);
        }//finally
    }

    /*private void getDocumentInfo(String docid) throws McdbException,
            AccessionNumberException, Exception
    {
        getDocumentInfo(new DocumentIdentifier(docid));
    }*/

    /**
     * Look up the document type information from the database
     *
     * @param docid
     *            the id of the document to look up
     */
    private void getDocumentInfo(String docid, int revision)
            throws McdbException, Exception
    {
        DBConnection dbconn = null;
        int serialNumber = -1;
        PreparedStatement pstmt = null;
        String table = "xml_documents";

        try {
            if (isRevisionOnly(docid, revision)) { //pull the document from xml_revisions
                                         // instead of from xml_documents;
                table = "xml_revisions";
            }
        }
        // catch a McdbDocNotFoundException throw it
        catch (McdbDocNotFoundException notFound) {
            throw notFound;
        } catch (Exception e) {

            logMetacat.error("DocumentImpl.getDocumentInfo - general error: " +
                    e.getMessage());
            throw e;
        }

        try {
            dbconn = DBConnectionPool
                    .getDBConnection("DocumentImpl.getDocumentInfo");
            serialNumber = dbconn.getCheckOutSerialNumber();
            StringBuffer sql = new StringBuffer();
            sql.append("SELECT docname, doctype, rootnodeid, ");
            sql.append("date_created, date_updated, user_owner, user_updated,");
            sql.append(" server_location, public_access, rev");
            sql.append(" FROM ").append(table);
            sql.append(" WHERE docid LIKE ? ");
            sql.append(" and rev = ? ");

            pstmt = dbconn.prepareStatement(sql.toString());
            pstmt.setString(1, docid);
            pstmt.setInt(2, revision);

            logMetacat.debug("DocumentImpl.getDocumentInfo - executing SQL: " + pstmt.toString());
            pstmt.execute();
            ResultSet rs = pstmt.getResultSet();
            boolean tableHasRows = rs.next();
            if (tableHasRows) {
                this.docname = rs.getString(1);
                this.doctype = rs.getString(2);
                this.rootnodeid = rs.getLong(3);
                this.createdate = rs.getTimestamp(4);
                this.updatedate = rs.getTimestamp(5);
                this.userowner = rs.getString(6);
                this.userupdated = rs.getString(7);
                this.serverlocation = rs.getInt(8);
                this.publicaccess = rs.getString(9);
                this.rev = rs.getInt(10);
            }
            pstmt.close();

            //get doc home server name
            pstmt = dbconn.prepareStatement("select server "
                    + "from xml_replication where serverid = ?");
            //because connection use twice here, so we need to increase one
            dbconn.increaseUsageCount(1);
            pstmt.setInt(1, serverlocation);
            logMetacat.debug("DocumentImpl.getDocumentInfo - executing SQL: " + pstmt.toString());
            pstmt.execute();
            rs = pstmt.getResultSet();
            tableHasRows = rs.next();
            if (tableHasRows) {

                String server = rs.getString(1);
                //get homeserver name
                if (!server.equals("localhost")) {
                    this.docHomeServer = server;
                } else {
                    this.docHomeServer = MetacatUtil
                            .getLocalReplicationServerName();
                }
                logMetacat.debug("DocumentImpl.getDocumentInfo - server: " + docHomeServer);

            }
            pstmt.close();
            if (this.doctype != null) {
                pstmt = dbconn.prepareStatement("SELECT system_id, entry_type "
                        + "FROM xml_catalog " + "WHERE public_id = ?");
                //should increase usage count again
                dbconn.increaseUsageCount(1);
                // Bind the values to the query
                pstmt.setString(1, doctype);

                logMetacat.debug("DocumentImpl.getDocumentInfo - executing SQL: " + pstmt.toString());
                pstmt.execute();
                rs = pstmt.getResultSet();
                tableHasRows = rs.next();
                if (tableHasRows) {
                    this.system_id = rs.getString(1);
                    // system id may not have server url on front.  Add it if not.
                    if (!system_id.startsWith("http://")) {
                    	system_id = SystemUtil.getContextURL() + system_id;
                    }
                    this.validateType = rs.getString(2);

                }
                pstmt.close();
            }
        } catch (SQLException e) {
            logMetacat.error("DocumentImpl.getDocumentInfo - Error in DocumentImpl.getDocumentInfo: "
                    + e.getMessage());
            e.printStackTrace(System.out);
            throw new McdbException("DocumentImpl.getDocumentInfo - Error accessing database connection: ", e);
        } finally {
            try {
                pstmt.close();
            } catch (SQLException ee) {
                logMetacat.error("DocumentImpl.getDocumentInfo - General error" + ee.getMessage());
            } finally {
                DBConnectionPool.returnDBConnection(dbconn, serialNumber);
            }
        }

        if (this.docname == null) {
            throw new McdbDocNotFoundException(
                "Document not found: " + docid, docid, (new Integer(revision)).toString());
        }
    }


    /**
     * Look up the node data from the database
     *
     * @param rootnodeid
     *            the id of the root node of the node tree to look up
     */
    private TreeSet<NodeRecord> getNodeRecordList(long rootnodeid) throws McdbException
    {
        PreparedStatement pstmt = null;
        DBConnection dbconn = null;
        int serialNumber = -1;
        TreeSet<NodeRecord> nodeRecordList = new TreeSet<NodeRecord>(new NodeComparator());
        long nodeid = 0;
        long parentnodeid = 0;
        long nodeindex = 0;
        String nodetype = null;
        String nodename = null;
        String nodeprefix = null;
        String nodedata = null;
        float nodedatanumerical = -1;
        Timestamp nodedatadate = null;

//        String quotechar = DatabaseService.getDBAdapter().getStringDelimiter();
        String table = "xml_nodes";
        //System.out.println("in getNodeREcorelist !!!!!!!!!!!for root node id "+rootnodeid);
        try {
            if (isRevisionOnly(docid, rev)) { //pull the document from xml_revisions
                // instead of from xml_documents;
                table = "xml_nodes_revisions";
                //System.out.println("in getNodeREcorelist !!!!!!!!!!!2");
            }
        }  catch (McdbDocNotFoundException notFound) {
            throw notFound;
        } catch (Exception e) {

            logMetacat.error("DocumentImpl.getNodeRecordList - General error: "
                    + e.getMessage());
        }
        //System.out.println("in getNodeREcorelist !!!!!!!!!!!3");
        try {
            dbconn = DBConnectionPool
                    .getDBConnection("DocumentImpl.getNodeRecordList");
            serialNumber = dbconn.getCheckOutSerialNumber();
            pstmt = dbconn
                    .prepareStatement("SELECT nodeid,parentnodeid,nodeindex, "
                            + "nodetype,nodename,nodeprefix,nodedata, nodedatanumerical, nodedatadate "
                            + "FROM " + table + " WHERE rootnodeid = ?");

            // Bind the values to the query
            pstmt.setLong(1, rootnodeid);
            //System.out.println("in getNodeREcorelist !!!!!!!!!!!4");
            logMetacat.debug("DocumentImpl.getNodeRecordList - executing SQL: " + pstmt.toString());
            pstmt.execute();
            ResultSet rs = pstmt.getResultSet();
            //System.out.println("in getNodeREcorelist !!!!!!!!!!!5");
            boolean tableHasRows = rs.next();
            while (tableHasRows) {
                //System.out.println("in getNodeREcorelist !!!!!!!!!!!6");
                nodeid = rs.getLong(1);
                parentnodeid = rs.getLong(2);
                nodeindex = rs.getLong(3);
                nodetype = rs.getString(4);
                nodename = rs.getString(5);
                nodeprefix = rs.getString(6);
                nodedata = rs.getString(7);
                try
                {
                	logMetacat.debug("DocumentImpl.getNodeRecordList - Node data in read process before normalize=== "+nodedata);
                	nodedata = MetacatUtil.normalize(nodedata);
                	logMetacat.debug("DocumentImpl.getNodeRecordList - Node data in read process after normalize==== "+nodedata);
                } catch (java.lang.StringIndexOutOfBoundsException SIO){
                	logMetacat.warn("DocumentImpl.getNodeRecordList - StringIndexOutOfBoundsException in normalize() while reading the document");
                }
                nodedatanumerical = rs.getFloat(8);
                nodedatadate = rs.getTimestamp(9);

                
                // add the data to the node record list hashtable
                NodeRecord currentRecord = new NodeRecord(nodeid, parentnodeid,
                        nodeindex, nodetype, nodename, nodeprefix, nodedata, nodedatanumerical, nodedatadate);
                nodeRecordList.add(currentRecord);

                // Advance to the next node
                tableHasRows = rs.next();
            }
            pstmt.close();
            //System.out.println("in getNodeREcorelist !!!!!!!!!!!7");

        } catch (SQLException e) {
            throw new McdbException("Error in DocumentImpl.getNodeRecordList "
                    + e.getMessage());
        } finally {
            try {
                pstmt.close();
            } catch (SQLException ee) {
                logMetacat.error("DocumentImpl.getNodeRecordList - General error: "
                                + ee.getMessage());
            } finally {
                DBConnectionPool.returnDBConnection(dbconn, serialNumber);
            }
        }
        //System.out.println("in getNodeREcorelist !!!!!!!!!!!8");
        return nodeRecordList;

    }

    /** creates SQL code and inserts new document into DB connection */
    private void writeDocumentToDB(String action, String user, String pub,
            String catalogid, int serverCode, Date createDate, Date updateDate) throws SQLException, Exception
    {
        //System.out.println("!!!!!!!!1write document to db  " +docid +"."+rev);
        String sysdate = DatabaseService.getInstance().getDBAdapter().getDateTimeFunction();
        Date today = Calendar.getInstance().getTime();
        if (createDate == null) {
            createDate = today;
        }
        if (updateDate == null) {
            updateDate = today;
        }
        DocumentImpl thisdoc = null;

        try {
            PreparedStatement pstmt = null;

            if (action.equals("INSERT")) {
                //AccessionNumber ac = new AccessionNumber();
                //this.docid = ac.generate(docid, "INSERT");
                String sql = null;
                if (catalogid != null )
                {
                	sql = "INSERT INTO xml_documents "
                        + "(docid, rootnodeid, docname, doctype, user_owner, "
                        + "user_updated, date_created, date_updated, "
                        + "public_access, server_location, rev, catalog_id) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, "
                        +  "?, ?, ?, ?, ?)";
                }
                else
                {
                	sql = "INSERT INTO xml_documents "
                        + "(docid, rootnodeid, docname, doctype, user_owner, "
                        + "user_updated, date_created, date_updated, "
                        + "public_access, server_location, rev) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, "
                        + "?, ?, ?, ?)";
                }
                /*pstmt = connection
                        .prepareStatement("INSERT INTO xml_documents "
                        + "(docid, rootnodeid, docname, doctype, user_owner, "
                        + "user_updated, date_created, date_updated, "
                        + "public_access, catalog_id, server_location, rev) "
                        + "VALUES (?, ?, ?, ?, ?, ?, " + createDate + ", "
                        + updateDate + ", ?, ?, ?, ?)");*/
                pstmt = connection.prepareStatement(sql);
                // Increase dbconnection usage count
                connection.increaseUsageCount(1);
                
                //note that the server_location is set to 1.
                //this means that "localhost" in the xml_replication table must
                //always be the first entry!!!!!

                // Bind the values to the query
                pstmt.setString(1, this.docid);
                pstmt.setLong(2, rootnodeid);
                pstmt.setString(3, docname);
                pstmt.setString(4, doctype);
                pstmt.setString(5, user);
                pstmt.setString(6, user);
                // dates
                pstmt.setTimestamp(7, new Timestamp(createDate.getTime()));
                pstmt.setTimestamp(8, new Timestamp(updateDate.getTime()));
                //public access is usefulless, so set it to null
                pstmt.setInt(9, 0);
                /*
                 * if ( pub == null ) { pstmt.setString(7, null); } else if (
                 * pub.toUpperCase().equals("YES") || pub.equals("1") ) {
                 * pstmt.setInt(7, 1); } else if (
                 * pub.toUpperCase().equals("NO") || pub.equals("0") ) {
                 * pstmt.setInt(7, 0); }
                 */
                pstmt.setInt(10, serverCode);
                pstmt.setInt(11, rev);
               
                if (catalogid != null)
                {
                  pstmt.setInt(12, (new Integer(catalogid)).intValue());
                }
                
                
            } else if (action.equals("UPDATE")) {
                int thisrev = DBUtil.getLatestRevisionInDocumentTable(docid);
                logMetacat.debug("DocumentImpl.writeDocumentToDB - this revision is: " + thisrev);
                // Save the old document publicaccessentry in a backup table
                String accNumber = docid + PropertyService.getProperty("document.accNumSeparator") + thisrev;
                thisdoc = new DocumentImpl(accNumber, false);
                DocumentImpl.archiveDocAndNodesRevision(connection, docid, user, thisdoc);
                //if the updated vesion is not greater than current one,
                //throw it into a exception
                if (rev <= thisrev) {
                    throw new Exception("Next revision number couldn't be less"
                            + " than or equal " + thisrev);
                } else {
                    //set the user specified revision
                    thisrev = rev;
                }
                logMetacat.debug("DocumentImpl.writeDocumentToDB - final revision is: " + thisrev);
                boolean useXMLIndex = (new Boolean(PropertyService
                        .getProperty("database.usexmlindex"))).booleanValue();
                if (useXMLIndex) {
                	
                	// make sure we don't have a pending index task
                    removeDocidFromIndexingQueue(docid, String.valueOf(rev));
                    
                    double start = System.currentTimeMillis()/1000;
                    // Delete index for the old version of docid
                    // The new index is inserting on the next calls to DBSAXNode
                    pstmt = connection
                            .prepareStatement("DELETE FROM xml_index WHERE docid='"
                                    + this.docid + "'");

                    // Increase dbconnection usage count
                    connection.increaseUsageCount(1);
                  
                    logMetacat.debug("DocumentImpl.writeDocumentToDB - executing SQL: " + pstmt.toString());
                    pstmt.execute();
                  
                    pstmt.close();
                    double end = System.currentTimeMillis()/1000;
                    logMetacat.info("DocumentImpl.writeDocumentToDB - Time for delete xml_index in UPDATE is "+(end -start));
                }

                // Update the new document to reflect the new node tree
                String updateSql = null;
                if (catalogid != null)
                {
                	updateSql = "UPDATE xml_documents "
                        + "SET rootnodeid = ?, docname = ?, doctype = ?, "
                        + "user_updated = ?, date_updated = ?, "
                        + "server_location = ?, rev = ?, public_access = ?, "
                        + "catalog_id = ? "
                        + "WHERE docid = ?";
                }
                else
                {
                	updateSql = "UPDATE xml_documents "
                        + "SET rootnodeid = ?, docname = ?, doctype = ?, "
                        + "user_updated = ?, date_updated = ?, "
                        + "server_location = ?, rev = ?, public_access = ? "
                        + "WHERE docid = ?";
                }
                // Increase dbconnection usage count
                pstmt = connection.prepareStatement(updateSql);
                connection.increaseUsageCount(1);
                // Bind the values to the query
                pstmt.setLong(1, rootnodeid);
                pstmt.setString(2, docname);
                pstmt.setString(3, doctype);
                pstmt.setString(4, user);
                pstmt.setTimestamp(5, new Timestamp(updateDate.getTime()));
                pstmt.setInt(6, serverCode);
                pstmt.setInt(7, thisrev);
                pstmt.setInt(8, 0);
                /*
                 * if ( pub == null ) { pstmt.setString(7, null); } else if (
                 * pub.toUpperCase().equals("YES") || pub.equals("1") ) { pstmt
                 * .setInt(7, 1); } else if ( pub.toUpperCase().equals("NO") ||
                 * pub.equals("0") ) { pstmt.setInt(7, 0); }
                 */
                if (catalogid != null)
                {
                  pstmt.setInt(9, (new Integer(catalogid)).intValue());
                  pstmt.setString(10, this.docid);
                }
                else
                {
                  pstmt.setString(9, this.docid);
                }

            } else {
                logMetacat.error("DocumentImpl.writeDocumentToDB - Action not supported: " + action);
            }

            // Do the insertion
            logMetacat.debug("DocumentImpl.writeDocumentToDB - executing SQL: " + pstmt.toString());
            pstmt.execute();
            
            pstmt.close();
            if(action.equals("UPDATE")){
            	logMetacat.debug("DocumentImpl.writeDocumentToDB - Deleting xml nodes for docid: " + 
            			thisdoc.getDocID() + " using root node ID: " + thisdoc.getRootNodeID());
            	deleteXMLNodes(connection, thisdoc.getRootNodeID());
            }

        } catch (SQLException sqle) {
        	logMetacat.error("DocumentImpl.writeDocumentToDB - SQL error: " + sqle.getMessage());
            sqle.printStackTrace();
            throw sqle;
        } catch (Exception e) {
        	logMetacat.error("DocumentImpl.writeDocumentToDB - General error: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * Write an XML file to the database, given a filename
     *
     * @param conn
     *            the JDBC connection to the database
     * @param filename
     *            the filename to be loaded into the database
     * @param pub
     *            flag for public "read" access on document
     * @param dtdfilename
     *            the dtd to be uploaded on server's file system
     * @param action
     *            the action to be performed (INSERT OR UPDATE)
     * @param docid
     *            the docid to use for the INSERT OR UPDATE
     * @param user
     *            the user that owns the document
     * @param groups
     *            the groups to which user belongs
     * @param writeAccessRules 
     */
    /*
     * public static String write(DBConnection conn,String filename, String pub,
     * String dtdfilename, String action, String docid, String user, String[]
     * groups ) throws Exception {
     *
     * Reader dtd = null; if ( dtdfilename != null ) { dtd = new FileReader(new
     * File(dtdfilename).toString()); } return write ( conn, new FileReader(new
     * File(filename).toString()), pub, dtd, action, docid, user, groups,
     * false); }
     */

    public static String write(DBConnection conn, String xmlString, String pub,
            Reader dtd, String action, String docid, String user,
            String[] groups, String ruleBase, boolean needValidation, boolean writeAccessRules)
            throws Exception
    {
        //this method will be called in handleUpdateOrInsert method
        //in MetacatServlet class and now is wrapper into documentImple
        // get server location for this doc
        int serverLocation = getServerLocationNumber(docid);
        return write(conn, xmlString, pub, dtd, action, docid, user, groups,
                serverLocation, false, ruleBase, needValidation, writeAccessRules);
    }

    /**
     * Write an XML file to the database, given a Reader
     *
     * @param conn
     *            the JDBC connection to the database
     * @param xml
     *            the xml stream to be loaded into the database
     * @param pub
     *            flag for public "read" access on xml document
     * @param dtd
     *            the dtd to be uploaded on server's file system
     * @param action
     *            the action to be performed (INSERT or UPDATE)
     * @param accnum
     *            the docid + rev# to use on INSERT or UPDATE
     * @param user
     *            the user that owns the document
     * @param groups
     *            the groups to which user belongs
     * @param serverCode
     *            the serverid from xml_replication on which this document
     *            resides.
     * @param override
     *            flag to stop insert replication checking. if override = true
     *            then a document not belonging to the local server will not be
     *            checked upon update for a file lock. if override = false then
     *            a document not from this server, upon update will be locked
     *            and version checked.
     * @param writeAccessRules 
     */

    public static String write(DBConnection conn, String xmlString, String pub,
            Reader dtd, String action, String accnum, String user,
            String[] groups, int serverCode, boolean override, String ruleBase,
            boolean needValidation, boolean writeAccessRules) throws Exception
    {
        // NEW - WHEN CLIENT ALWAYS PROVIDE ACCESSION NUMBER INCLUDING REV IN IT
    	
    	// Get the xml as a string so we can write to file later
    	StringReader xmlReader = new StringReader(xmlString);

        logMetacat.debug("DocumentImpl.write - conn usage count before writing: "
                + conn.getUsageCount());
        AccessionNumber ac = new AccessionNumber(accnum, action);
        String docid = ac.getDocid();
        String rev = ac.getRev();
        logMetacat.debug("DocumentImpl.write - action: " + action + " servercode: "
                + serverCode + " override: " + override);

        if ((serverCode != 1 && action.equals("UPDATE")) && !override) {
            // if this document being written is not a resident of this server
            // then we need to try to get a lock from it's resident server. If
            // the resident server will not give a lock then we send the user
            // a  message saying that he/she needs to download a new copy of
            // the file and merge the differences manually.

            // check for 'write' permission for 'user' to update this document
        	// use the previous revision to check the permissions
            String docIdWithoutRev = DocumentUtil.getSmartDocId(accnum);
        	int latestRev = DBUtil.getLatestRevisionInDocumentTable(docIdWithoutRev);
        	String latestDocId = docIdWithoutRev + PropertyService.getProperty("document.accNumSeparator") + latestRev;
            if (!hasWritePermission(user, groups, latestDocId)) {
                throw new Exception(
                    "User " + user
                    + " does not have permission to update XML Document #"
                    + accnum);
            }

            //DocumentIdentifier id = new DocumentIdentifier(accnum);
            int revision = DocumentUtil.getRevisionFromAccessionNumber(accnum);
            String updaterev = (new Integer(revision)).toString();
            String server = ReplicationService
                    .getServerNameForServerCode(serverCode);
            logReplication.info("attempting to lock " + accnum);
            URL u = new URL("https://" + server + "?server="
                    + MetacatUtil.getLocalReplicationServerName()
                    + "&action=getlock&updaterev=" + updaterev + "&docid="
                    + docid);
            //System.out.println("sending message: " + u.toString());
            String openingtag = null;
            try {
            	String serverResStr = ReplicationService.getURLContent(u);
            	openingtag = serverResStr.substring(0, serverResStr.indexOf(">") + 1);
            } catch (IOException e) {
				// give a more meaningful error if replication check fails
            	// see http://bugzilla.ecoinformatics.org/show_bug.cgi?id=4907
            	String msg = "Error during replication lock request on server=" + server;
            	logReplication.error(msg);
            	throw new Exception(msg, e);
			}
            
            if (openingtag.equals("<lockgranted>")) {//the lock was granted go
                                                     // ahead with the insert
                XMLReader parser = null;
                try {
                    //System.out.println("In lockgranted");
                	logReplication.info("lock granted for " + accnum
                            + " from " + server);
                	
                	// detect encoding
                    XmlStreamReader xsr = new XmlStreamReader(new ByteArrayInputStream(xmlString.getBytes()));
			        String encoding = xsr.getEncoding();
			        Vector<String>guidsToSync = new Vector<String>();

                    /*
                     * XMLReader parser = initializeParser(conn, action, docid,
                     * updaterev, validate, user, groups, pub, serverCode, dtd);
                     */
                    logMetacat.debug("DocumentImpl.write - initializing parser");
                    parser = initializeParser(conn, action, docid, xmlReader, updaterev,
                            user, groups, pub, serverCode, dtd, ruleBase,
                            needValidation, false, null, null, encoding, writeAccessRules, guidsToSync);
                    	// false means it is not a revision doc
                                   //null, null are createdate and updatedate
                                   //null will use current time as create date time
                    conn.setAutoCommit(false);
                    logMetacat.debug("DocumentImpl.write - parsing xml");
                    parser.parse(new InputSource(xmlReader));
                    conn.commit();
                    conn.setAutoCommit(true);
                    
                    // update the node data to include numeric and date values
                    updateNodeValues(conn, docid);
                    
                    //write the file to disk
                    logMetacat.debug("DocumentImpl.write - Writing xml to file system");                    
                	writeToFileSystem(xmlString, accnum, encoding);

                    // write to xml_node complete. start the indexing thread.
                    addDocidToIndexingQueue(docid, rev);
                    
			        // The EML parser has already written to systemmetadata and then writes to xml_access when the db transaction
                    // is committed. If the pids that have been updated are for data objects with their own access rules, we
			        // must inform the CN to sync it's access rules with the MN, so the EML 2.1 parser collected such pids from the parse
			        // operation.
            		if (guidsToSync.size() > 0) {
            			try {
            				SyncAccessPolicy syncAP = new SyncAccessPolicy();
            				syncAP.sync(guidsToSync);
            			} catch (Exception e) {
            				logMetacat.error("Error syncing pids with CN: " + " Exception " + e.getMessage());
            				e.printStackTrace(System.out);
            			}
            		}
               } catch (Exception e) {
                   e.printStackTrace();
            	   logMetacat.error("DocumentImpl.write - Problem with parsing: " + e.getMessage());
                    conn.rollback();
                    conn.setAutoCommit(true);
                    //if it is a eml2 document, we need delete online data
                    if (parser != null) {
                        ContentHandler handler = parser.getContentHandler();
                        if (handler instanceof Eml200SAXHandler) {
                            Eml200SAXHandler eml = (Eml200SAXHandler) handler;
                            eml.deleteInlineFiles();
                        }
                    }
                    throw e;
                }
                // run write into access db base one relation table and access
                // object
                runRelationAndAccessHandler(accnum, user, groups, serverCode);

                // Force replication the docid
                ForceReplicationHandler frh = new ForceReplicationHandler(
                        accnum, true, null);
                logMetacat.debug("DocumentImpl.write - ForceReplicationHandler created: " + frh.toString());
                return (accnum);

            }

            else if (openingtag.equals("<filelocked>")) {
                // the file is currently locked by another user notify our
                // user to wait a few minutes, check out a new copy and try
                // again.
            	logReplication.error("DocumentImpl.write - lock denied for " + accnum + " on "
                        + server + " reason: file already locked");
                throw new Exception(
                        "The file specified is already locked by another "
                                + "user.  Please wait 30 seconds, checkout the "
                                + "newer document, merge your changes and try "
                                + "again.");
            } else if (openingtag.equals("<outdatedfile>")) {
                // our file is outdated. notify our user to check out a new
                // copy of the file and merge his version with the new version.
                //System.out.println("outdated file");
            	logReplication.info("DocumentImpl.write - lock denied for " + accnum + " on "
                        + server + " reason: local file outdated");
                throw new Exception(
                        "The file you are trying to update is an outdated"
                        + " version.  Please checkout the newest document, "
                        + "merge your changes and try again.");
            }
        }

        if (action.equals("UPDATE")) {
			// check for 'write' permission for 'user' to update this document
        	// use the previous revision to check the permissions
            String docIdWithoutRev = DocumentUtil.getSmartDocId(accnum);
        	int latestRev = DBUtil.getLatestRevisionInDocumentTable(docIdWithoutRev);
        	String latestDocId = docIdWithoutRev + PropertyService.getProperty("document.accNumSeparator") + latestRev;
            if (!hasWritePermission(user, groups, latestDocId) 
            		&& !AuthUtil.isAdministrator(user, groups)) {
                throw new Exception(
                    "User " + user
                    + " does not have permission to update XML Document #"
                    + latestDocId); }
        }
        XMLReader parser = null;
        try {
            // detect encoding
        	XmlStreamReader xsr = new XmlStreamReader(new ByteArrayInputStream(xmlString.getBytes()));
	        String encoding = xsr.getEncoding();
	        Vector<String>guidsToSync = new Vector<String>();

            parser = initializeParser(conn, action, docid, xmlReader, rev, user, groups,
                    pub, serverCode, dtd, ruleBase, needValidation, false, null, null, encoding, writeAccessRules, guidsToSync);
                    // null and null are createtime and updatetime
                    // null will create current time
                    //false means it is not a revision doc

            conn.setAutoCommit(false);
            logMetacat.debug("DocumentImpl.write - XML to be parsed: " + xmlString);
            parser.parse(new InputSource(xmlReader));

            conn.commit();
            conn.setAutoCommit(true);
            
            //update nodes
            updateNodeValues(conn, docid);
            
            //write the file to disk
        	writeToFileSystem(xmlString, accnum, encoding);

            addDocidToIndexingQueue(docid, rev);
    		if (guidsToSync.size() > 0) {
    			try {
    				SyncAccessPolicy syncAP = new SyncAccessPolicy();
    				syncAP.sync(guidsToSync);
    			} catch (Exception e) {
    				logMetacat.error("Error syncing pids with CN: " + " Exception " + e.getMessage());
    				e.printStackTrace(System.out);
    			}
    		}
        } catch (Exception e) {
        	logMetacat.error("DocumentImpl.write - Problem with parsing: " + e.getMessage());
            e.printStackTrace();
            conn.rollback();
            conn.setAutoCommit(true);
            //if it is a eml2 document, we need delete online data
            if (parser != null) {
                ContentHandler handler = parser.getContentHandler();
                if (handler instanceof Eml200SAXHandler) {
                    Eml200SAXHandler eml = (Eml200SAXHandler) handler;
                    eml.deleteInlineFiles();
                }
            }
            throw e;
        }

        // run access db base on relation table and access object
        //System.out.println("the accnum will be write into access table "+accnum);
        runRelationAndAccessHandler(accnum, user, groups, serverCode);

        // Delete enteries from xml_queryresult for given docid if
        // action is UPDATE
        // These enteries will be created again when the docid is part of a
        // result next time
        if (action.equals("UPDATE")) {
          try {
//              PreparedStatement pstmt = null;
//              pstmt = conn.prepareStatement(
//                      "DELETE FROM xml_queryresult WHERE docid = ?");
//              pstmt.setString(1, docid);
//              pstmt.execute();
//              pstmt.close();
//              conn.increaseUsageCount(1);
              try {
                  XMLQueryresultAccess xmlQueryresultAccess = new XMLQueryresultAccess();
                  xmlQueryresultAccess.deleteXMLQueryresulForDoc(docid);
              } catch (AccessException ae) {
              	throw new SQLException("Problem deleting xml query result for docid " + 
              			docid + " : " + ae.getMessage());
              }
          } catch (Exception e){
              logMetacat.error("DocumentImpl.write - Error in deleting enteries from "
                                       + "xml_queryresult where docid is "
                                       + docid + " in DBQuery.write: "
                                       + e.getMessage());
           }

        }
        
        // Force replicate out the new document to each server in our server
        // list. Start the thread to replicate this new document out to the
        // other servers true mean it is xml document null is because no
        // metacat notify the force replication.
        ForceReplicationHandler frh = new ForceReplicationHandler(accnum,
                action, true, null);
        logMetacat.debug("DocumentImpl.write - ForceReplicationHandler created: " + frh.toString());
        // clear cache after inserting or updating a document
        if (PropertyService.getProperty("database.queryCacheOn").equals("true"))
        {
          //System.out.println("the string stored into cache is "+ resultsetBuffer.toString());
     	   DBQuery.clearQueryResultCache();
        }

        logMetacat.info("DocumentImpl.write - Conn Usage count after writing: "
                + conn.getUsageCount());
        return (accnum);
    }

    
    private static void addDocidToIndexingQueue(String docid, String rev) throws PropertyNotFoundException {
        boolean useXMLIndex =
            (new Boolean(PropertyService.getProperty("database.usexmlindex"))).booleanValue();
        if (useXMLIndex) {
            	IndexingQueue.getInstance().add(docid, rev);
        }
    }
    
    private static void removeDocidFromIndexingQueue(String docid, String rev) throws PropertyNotFoundException {
        boolean useXMLIndex =
            (new Boolean(PropertyService.getProperty("database.usexmlindex"))).booleanValue();
        if (useXMLIndex) {
            	IndexingQueue.getInstance().remove(docid, rev);
        }
    }

    /**
     * Write an XML file to the database during replication
     *
     * @param conn
     *            the JDBC connection to the database
     * @param xml
     *            the xml stream to be loaded into the database
     * @param pub
     *            flag for public "read" access on xml document
     * @param dtd
     *            the dtd to be uploaded on server's file system
     * @param action
     *            the action to be performed (INSERT or UPDATE)
     * @param accnum
     *            the docid + rev# to use on INSERT or UPDATE
     * @param user
     *            the user that owns the document
     * @param groups
     *            the groups to which user belongs
     * @param homeServer
     *            the name of server which the document origanlly create
     * @param validate,
     *            if the xml document is valid or not
     * @param notifyServer,
     *            the server which notify local server the force replication
     *            command
     */
    public static String writeReplication(DBConnection conn, String xmlString,
            String pub, Reader dtd, String action, String accnum, String user,
            String[] groups, String homeServer, String notifyServer,
            String ruleBase, boolean needValidation, String tableName, 
            boolean timedReplication, Date createDate, Date updateDate) throws Exception
    {
    	// Get the xml as a string so we can write to file later
    	StringReader xmlReader = new StringReader(xmlString);
    	
        long rootId;
        String docType = null;
        String docName = null;
        String catalogId = null;
        logMetacat.debug("DocumentImpl.writeReplication - user in replication" + user);
        // Docid without revision
        String docid = DocumentUtil.getDocIdFromAccessionNumber(accnum);
        logMetacat.debug("DocumentImpl.writeReplication - The docid without rev is " + docid);
        // Revision specified by user (int)
        int userSpecifyRev = DocumentUtil.getRevisionFromAccessionNumber(accnum);
        logMetacat.debug("DocumentImpl.writeReplication - The user specifyRev: " + userSpecifyRev);
        // Revision for this docid in current database
        int revInDataBase = DBUtil.getLatestRevisionInDocumentTable(docid);
        logMetacat.debug("DocumentImpl.writeReplication - The rev in data base: " + revInDataBase);
        // String to store the revision
        String rev = (new Integer(userSpecifyRev)).toString();

        if (tableName.equals(DOCUMENTTABLE))
        {
            action = checkRevInXMLDocuments(docid, userSpecifyRev);
        }
        else if (tableName.equals(REVISIONTABLE))
        {
            action = checkXMLRevisionTable(docid, userSpecifyRev);
        }
        else
        {
            throw new Exception("The table name is not support "+tableName);
        }
        // Variable to store homeserver code
        int serverCode = -2;

        // If server is not int the xml replication talbe, insert it into
        // xml_replication table
        //serverList.addToServerListIfItIsNot(homeServer);
        insertServerIntoReplicationTable(homeServer);
        // Get server code again
        serverCode = getServerCode(homeServer);

        logMetacat.info("DocumentImpl.writeReplication - Document " + docid + "." + rev + " " + action
                        + " into local" + " metacat with servercode: "
                        + serverCode);

        // insert into xml_nodes table
        XMLReader parser = null;
        boolean isRevision = false;
        try {
            
            if (tableName.equals(REVISIONTABLE))
            {
                isRevision = true;
            }
            // detect encoding
            XmlStreamReader xsr = new XmlStreamReader(new ByteArrayInputStream(xmlString.getBytes()));
	        String encoding = xsr.getEncoding();
	        
	        // no need to write the EML-contained access rules for replication
	        boolean writeAccessRules = false;
	        Vector<String>guidsToSync = new Vector<String>();

            parser = initializeParser(conn, action, docid, xmlReader, rev, user, groups,
                    pub, serverCode, dtd, ruleBase, needValidation, 
                    isRevision, createDate, updateDate, encoding, writeAccessRules, guidsToSync);
         
            conn.setAutoCommit(false);
            parser.parse(new InputSource(xmlReader));
            conn.commit();
            conn.setAutoCommit(true);
            
            // Write the file to disk
        	writeToFileSystem(xmlString, accnum, encoding);
            
            // write to xml_node complete. start the indexing thread.
            // this only for xml_documents
            if (!isRevision)
            {
            	addDocidToIndexingQueue(docid, rev);
            }
            
            DBSAXHandler dbx = (DBSAXHandler) parser.getContentHandler();
            rootId = dbx.getRootNodeId();
            docType = dbx.getDocumentType();
            docName = dbx.getDocumentName();
            catalogId = dbx.getCatalogId();

        } catch (Exception e) {
        	logMetacat.error("DocumentImpl.writeReplication - Problem with parsing: " + e.getMessage());
            conn.rollback();
            conn.setAutoCommit(true);
            if (parser != null) {
                ContentHandler handler = parser.getContentHandler();
                if (handler instanceof Eml200SAXHandler) {
                    Eml200SAXHandler eml = (Eml200SAXHandler) handler;
                    eml.deleteInlineFiles();
                }
            }
            throw e;
        }

        // run write into access db base on relation table and access rule
        try {
            conn.setAutoCommit(false);
            if (!isRevision)
            {
               runRelationAndAccessHandler(accnum, user, groups, serverCode);
            }
            else
            {
              // in replicate revision documents,
              // we need to move nodes from xml_nodes to xml_nodes_revision and register the record
            	// into xml_revision table
               moveNodesToNodesRevision(conn, rootId);
               deleteXMLNodes(conn, rootId);
               writeDocumentToRevisionTable(conn, docid, rev, docType, docName, user, 
                       catalogId, serverCode, rootId, createDate, updateDate);
              
            }
            conn.commit();
            conn.setAutoCommit(true);
            
        } catch (Exception ee) {
        	conn.rollback();
            conn.setAutoCommit(true);
            if (tableName.equals(REVISIONTABLE))
            {
                // because we couldn't register the table into xml_revsion
                // we need to delete the nodes in xml_ndoes.
                deleteXMLNodes(conn, rootId);
            }
            logReplication.error("DocumentImpl.writeReplication - Failed to " + "create access "
                    + "rule for package: " + accnum + " because "
                    + ee.getMessage());
            logMetacat.error("DocumentImpl.writeReplication - Failed to  " + "create access "
                    + "rule for package: " + accnum + " because "
                    + ee.getMessage());
        }
      
        //Force replication to other server
        if (!timedReplication)
        {
          ForceReplicationHandler forceReplication = new ForceReplicationHandler(
                accnum, action, true, notifyServer);
          logMetacat.debug("DocumentImpl.writeReplication - ForceReplicationHandler created: " + forceReplication.toString());
        }
        
        // clear cache after inserting or updating a document
        if (PropertyService.getProperty("database.queryCacheOn").equals("true"))
        {
          //System.out.println("the string stored into cache is "+ resultsetBuffer.toString());
     	   DBQuery.clearQueryResultCache();
        }
    
        return (accnum);
    }

    /* Running write record to xml_relation and xml_access */
    private static void runRelationAndAccessHandler(String accnumber,
            String userName, String[] group, int servercode) throws Exception
    {
        DBConnection dbconn = null;
        int serialNumber = -1;
//        PreparedStatement pstmt = null;
        String documenttype = getDocTypeFromDBForCurrentDocument(accnumber);
        try {
            String packagedoctype = PropertyService.getProperty("xml.packagedoctype");
            Vector<String> packagedoctypes = new Vector<String>();
            packagedoctypes = MetacatUtil.getOptionList(packagedoctype);
            String docIdWithoutRev = DocumentUtil.getDocIdFromAccessionNumber(accnumber);
            int revision = DocumentUtil.getRevisionFromAccessionNumber(accnumber);
            if (documenttype != null &&
                    packagedoctypes.contains(documenttype)) {
                dbconn = DBConnectionPool.getDBConnection(
                        "DocumentImpl.runRelationAndAccessHandeler");
                serialNumber = dbconn.getCheckOutSerialNumber();
                dbconn.setAutoCommit(false);
                // from the relations get the access file id for that package
                String aclidWithRev = RelationHandler.getAccessFileIDWithRevision(docIdWithoutRev);
                if (aclidWithRev != null)
                {
                  String aclid = DocumentUtil.getDocIdFromAccessionNumber(aclidWithRev);
                  revision = DocumentUtil.getRevisionFromAccessionNumber(aclidWithRev);
                 
                  // if there are access file, write ACL for that package
                  if (aclid != null) {
                    runAccessControlList(dbconn, aclid, revision, userName, group,
                            servercode);
                  }
                }
                dbconn.commit();
                dbconn.setAutoCommit(true);
            }
            // if it is an access file
            else if (documenttype != null
                    && MetacatUtil.getOptionList(
                            PropertyService.getProperty("xml.accessdoctype")).contains(
                            documenttype)) {
                dbconn = DBConnectionPool.getDBConnection(
                        "DocumentImpl.runRelationAndAccessHandeler");
                serialNumber = dbconn.getCheckOutSerialNumber();
                dbconn.setAutoCommit(false);
                // write ACL for the package
                runAccessControlList(dbconn, docIdWithoutRev, revision, userName, group,
                        servercode);
                dbconn.commit();
                dbconn.setAutoCommit(true);

            }

        } catch (Exception e) {
            if (dbconn != null) {
                dbconn.rollback();
                dbconn.setAutoCommit(true);
            }
            logMetacat.error("DocumentImpl.runRelationAndAccessHandler - Error in DocumentImple.runRelationAndAccessHandler "
                            + e.getMessage());
            throw e;
        } finally {
            if (dbconn != null) {
                DBConnectionPool.returnDBConnection(dbconn, serialNumber);
            }
        }
    }

    // It runs in xmlIndex thread. It writes ACL for a package.
    private static void runAccessControlList(DBConnection conn, String aclid,
            int rev, String users, String[] group, int servercode) throws Exception
    {
        // read the access file from xml_nodes
        // parse the access file and store the access info into xml_access
        AccessControlList aclobj = new AccessControlList(conn, aclid, rev, users,
                group, servercode);

    }

    /* Method get document type from db */
    private static String getDocTypeFromDBForCurrentDocument(String accnumber)
            throws SQLException
    {
        String documentType = null;
        String docid = null;
        PreparedStatement pstate = null;
        ResultSet rs = null;
        String sql = "SELECT doctype FROM xml_documents where docid = ?";
        DBConnection dbConnection = null;
        int serialNumber = -1;
        try {
            //get rid of revision number
            docid = DocumentUtil.getDocIdFromString(accnumber);
            dbConnection = DBConnectionPool.getDBConnection(
                    "DocumentImpl.getDocTypeFromDBForCurrentDoc");
            serialNumber = dbConnection.getCheckOutSerialNumber();
            pstate = dbConnection.prepareStatement(sql);
            //bind variable
            pstate.setString(1, docid);
            //excute query
            logMetacat.debug("DocumentImpl.getDocTypeFromDBForCurrentDocument - executing SQL: " + pstate.toString());
            pstate.execute();
            //handle resultset
            rs = pstate.getResultSet();
            if (rs.next()) {
                documentType = rs.getString(1);
            }
            rs.close();
            pstate.close();
        }//try
        catch (SQLException e) {
            logMetacat.error("DocumentImpl.getDocTypeFromDBForCurrentDocument - SQL error: " + 
            		e.getMessage());
            throw e;
        }//catch
        finally {
            pstate.close();
            DBConnectionPool.returnDBConnection(dbConnection, serialNumber);
        }//
        logMetacat.debug("DocumentImpl.getDocTypeFromDBForCurrentDocument - The current doctype from db is: "
                + documentType);
        return documentType;
    }

    /**
     * Delete an XML file from the database (actually, just make it a revision
     * in the xml_revisions table)
     *
     * @param docid
     *            the ID of the document to be deleted from the database
     */
    public static void delete(String accnum, String user, 
      String[] groups, String notifyServer, boolean removeAll)
      throws SQLException, InsufficientKarmaException, McdbDocNotFoundException,
      Exception
    {

        DBConnection conn = null;
        int serialNumber = -1;
        PreparedStatement pstmt = null;
        boolean isXML   = true;
        try {
            //check out DBConnection
            conn = DBConnectionPool.getDBConnection("DocumentImpl.delete");
            serialNumber = conn.getCheckOutSerialNumber();

            // CLIENT SHOULD ALWAYS PROVIDE ACCESSION NUMBER INCLUDING REV
            //AccessionNumber ac = new AccessionNumber(accnum, "DELETE");
            String docid = DocumentUtil.getDocIdFromAccessionNumber(accnum);
            int rev = DocumentUtil.getRevisionFromAccessionNumber(accnum);;

            // Check if the document exists.
            pstmt = conn.prepareStatement("SELECT * FROM xml_documents WHERE docid = ?");
            pstmt.setString(1, docid);
            logMetacat.debug("DocumentImpl.delete - executing SQL: " + pstmt.toString());
            pstmt.execute();
            ResultSet rs = pstmt.getResultSet();
            if(!rs.next()){
                rs.close();
                pstmt.close();
                conn.increaseUsageCount(1);
                throw new McdbDocNotFoundException("Docid " + accnum  + 
                  " does not exist. Please check that you have also " +
                  "specified the revision number of the document.");
            }
            rs.close();
            pstmt.close();
            conn.increaseUsageCount(1);

            // get the type of deleting docid, this will be used in forcereplication
            String type = getDocTypeFromDB(conn, docid);
            if (type != null && type.trim().equals("BIN")) {
            	isXML = false;
            }

            logMetacat.info("DocumentImpl.delete - Start deleting doc " + docid + "...");
            double start = System.currentTimeMillis()/1000;
            // check for 'write' permission for 'user' to delete this document
            if (!hasAllPermission(user, groups, accnum)) {
                if(!AuthUtil.isAdministrator(user, groups)){
                    throw new InsufficientKarmaException(
                        "User " + user + 
                        " does not have permission to delete XML Document #" + 
                        accnum);
                }
            }

            conn.setAutoCommit(false);
            
            // Copy the record to the xml_revisions table if not a full delete
            if (!removeAll) {
            	DocumentImpl.archiveDocAndNodesRevision(conn, docid, user, null);
                logMetacat.info("DocumentImpl.delete - calling archiveDocAndNodesRevision");

            }
            double afterArchiveDocAndNode = System.currentTimeMillis()/1000;
            logMetacat.info("DocumentImpl.delete - The time for archiveDocAndNodesRevision is "+(afterArchiveDocAndNode - start));
            
            // make sure we don't have a pending index task
            removeDocidFromIndexingQueue(docid, String.valueOf(rev));
            
            // Now delete it from the xml_index table
            pstmt = conn.prepareStatement("DELETE FROM xml_index WHERE docid = ?");
            pstmt.setString(1, docid);
            logMetacat.debug("DocumentImpl.delete - executing SQL: " + pstmt.toString());
            pstmt.execute();
            pstmt.close();
            conn.increaseUsageCount(1);
            
            double afterDeleteIndex = System.currentTimeMillis()/1000;
            logMetacat.info("DocumentImpl.delete - The deleting xml_index time is "+(afterDeleteIndex - afterArchiveDocAndNode ));
            
            // Now delete it from xml_access table
            /*************** DO NOT DELETE ACCESS - need to archive this ******************/
            double afterDeleteXmlAccess2 = System.currentTimeMillis()/1000;
            /******* END DELETE ACCESS *************/            
            
            // Delete enteries from xml_queryresult
            try {
                XMLQueryresultAccess xmlQueryresultAccess = new XMLQueryresultAccess();
                xmlQueryresultAccess.deleteXMLQueryresulForDoc(docid);
            } catch (AccessException ae) {
            	throw new SQLException("Problem deleting xml query result for docid " + 
            			docid + " : " + ae.getMessage());
            }
            double afterDeleteQueryResult = System.currentTimeMillis()/1000;
            logMetacat.info("DocumentImpl.delete - The deleting xml_queryresult time is "+(afterDeleteQueryResult - afterDeleteXmlAccess2));
            // Delete it from relation table
            pstmt = conn.prepareStatement("DELETE FROM xml_relation WHERE docid = ?");
            //increase usage count
            pstmt.setString(1, docid);
            logMetacat.debug("DocumentImpl.delete - running sql: " + pstmt.toString());
            pstmt.execute();
            pstmt.close();
            conn.increaseUsageCount(1);
            double afterXMLRelation = System.currentTimeMillis()/1000;
            logMetacat.info("DocumentImpl.delete - The deleting time  relation is "+ (afterXMLRelation - afterDeleteQueryResult) );

            // Delete it from xml_path_index table
            logMetacat.info("DocumentImpl.delete - deleting from xml_path_index");
            pstmt = conn.prepareStatement("DELETE FROM xml_path_index WHERE docid = ?");
            //increase usage count
            pstmt.setString(1, docid);
            logMetacat.debug("DocumentImpl.delete - running sql: " + pstmt.toString());
            pstmt.execute();
            pstmt.close();
            conn.increaseUsageCount(1);

            logMetacat.info("DocumentImpl.delete - deleting from xml_accesssubtree");
            // Delete it from xml_accesssubtree table
            pstmt = conn.prepareStatement("DELETE FROM xml_accesssubtree WHERE docid = ?");
            //increase usage count
            pstmt.setString(1, docid);
            logMetacat.debug("DocumentImpl.delete - running sql: " + pstmt.toString());
            pstmt.execute();
            pstmt.close();
            conn.increaseUsageCount(1);

            // Delete it from xml_documents table
            logMetacat.info("DocumentImpl.delete - deleting from xml_documents");
            pstmt = conn.prepareStatement("DELETE FROM xml_documents WHERE docid = ?");
            pstmt.setString(1, docid);
            logMetacat.debug("DocumentImpl.delete - running sql: " + pstmt.toString());
            pstmt.execute();
            pstmt.close();
            //Usaga count increase 1
            conn.increaseUsageCount(1);
            double afterDeleteDoc = System.currentTimeMillis()/1000;
            logMetacat.info("DocumentImpl.delete - the time to delete  xml_path_index,  xml_accesssubtree, xml_documents time is "+ 
            		(afterDeleteDoc - afterXMLRelation ));
            // Delete the old nodes in xml_nodes table...
            pstmt = conn.prepareStatement("DELETE FROM xml_nodes WHERE docid = ?");

            // Increase dbconnection usage count
            conn.increaseUsageCount(1);
            // Bind the values to the query and execute it
            pstmt.setString(1, docid);
            logMetacat.debug("DocumentImpl.delete - running sql: " + pstmt.toString());
            pstmt.execute();
            pstmt.close();

            double afterDeleteXMLNodes = System.currentTimeMillis()/1000;
            logMetacat.info("DocumentImpl.delete - Deleting xml_nodes time is "+(afterDeleteXMLNodes-afterDeleteDoc));
            
            // remove the file if called for
            if (removeAll) {
            	deleteFromFileSystem(accnum, isXML);
            }
            
            // set as archived in the systemMetadata 
            String pid = IdentifierManager.getInstance().getGUID(docid, rev);
            Identifier guid = new Identifier();
        	guid.setValue(pid);
            SystemMetadata sysMeta = HazelcastService.getInstance().getSystemMetadataMap().get(guid);
            if (sysMeta != null) {
				sysMeta.setSerialVersion(sysMeta.getSerialVersion().add(BigInteger.ONE));
				sysMeta.setArchived(true);
            	sysMeta.setDateSysMetadataModified(Calendar.getInstance().getTime());
				HazelcastService.getInstance().getSystemMetadataMap().put(guid, sysMeta);
                MetacatSolrIndex.getInstance().submit(guid, sysMeta, null);
            }
            
            // clear cache after inserting or updating a document
            if (PropertyService.getProperty("database.queryCacheOn").equals("true")) {
            	//System.out.println("the string stored into cache is "+ resultsetBuffer.toString());
            	DBQuery.clearQueryResultCache();
            }
            
            // only commit if all of this was successful
            conn.commit();
            conn.setAutoCommit(true);
                        
            // add force delete replcation document here.
            String deleteAction = ReplicationService.FORCEREPLICATEDELETE;
            if (removeAll) {
                deleteAction = ReplicationService.FORCEREPLICATEDELETEALL;
            }
            ForceReplicationHandler frh = new ForceReplicationHandler(
                             accnum, deleteAction, isXML, notifyServer);
            logMetacat.debug("DocumentImpl.delete - ForceReplicationHandler created: " + frh.toString());
            
           double end = System.currentTimeMillis()/1000;
           logMetacat.info("DocumentImpl.delete - total delete time is:  " + (end - start));

        } catch ( Exception e ) {
        	// rollback the delete if there was an error
        	conn.rollback();
            logMetacat.error("DocumentImpl.delete -  Error: " + e.getMessage());
            throw e;
        } finally {

            try {
                // close preparedStatement
                if (pstmt != null) {
                    pstmt.close();
                }
            }
            finally {
                //check in DBonnection
                DBConnectionPool.returnDBConnection(conn, serialNumber);
            }
        }
       

    }

    private static String getDocTypeFromDB(DBConnection conn, String docidWithoutRev)
                                 throws SQLException
    {
      String type = null;
      String sql = "SELECT DOCTYPE FROM xml_documents WHERE docid LIKE ?";
      PreparedStatement stmt = null;
      stmt = conn.prepareStatement(sql);
      stmt.setString(1, docidWithoutRev);
      ResultSet result = stmt.executeQuery();
      boolean hasResult = result.next();
      if (hasResult)
      {
        type = result.getString(1);
      }
      logMetacat.debug("DocumentImpl.delete - The type of deleting docid " + docidWithoutRev +
                               " is " + type);
      return type;
    }

    /**
     * Check for "WRITE" permission on @docid for @user and/or @groups
     * from DB connection
     */
    private static boolean hasWritePermission(String user, String[] groups,
            String docid) throws SQLException, Exception
    {
        // Check for WRITE permission on @docid for @user and/or @groups
        PermissionController controller = new PermissionController(docid);
        return controller.hasPermission(user, groups,
                AccessControlInterface.WRITESTRING);
    }

    /**
     * Check for "READ" permission base on docid, user and group
     *
     * @param docid, the document
     * @param user, user name
     * @param groups, user's group
     */
    public static boolean hasReadPermission(String user, String[] groups,
            String docId) throws SQLException, McdbException
    {
        // Check for READ permission on @docid for @user and/or @groups
        PermissionController controller = new PermissionController(docId);
        return controller.hasPermission(user, groups,
                AccessControlInterface.READSTRING);
    }

    /**
     * Check for "WRITE" permission on @docid for @user and/or @groups
     * from DB connection
     */
    private static boolean hasAllPermission(String user, String[] groups,
            String docid) throws SQLException, Exception
    {
        // Check for WRITE permission on @docid for @user and/or @groups
        PermissionController controller = new PermissionController(docid);
        return controller.hasPermission(user, groups,
                AccessControlInterface.ALLSTRING);
    }

    /**
     * Set up the parser handlers for writing the document to the database
     * @param writeAccessRules 
     */
    private static XMLReader initializeParser(DBConnection dbconn,
            String action, String docid, Reader xml, String rev, String user,
            String[] groups, String pub, int serverCode, Reader dtd,
            String ruleBase, boolean needValidation, boolean isRevision,
            Date createDate, Date updateDate, String encoding, boolean writeAccessRules, Vector<String> guidsToSync) throws Exception
    {
        XMLReader parser = null;
        try {
            // handler
            DBSAXHandler chandler;
            EntityResolver eresolver;
            DTDHandler dtdhandler;
            // Get an instance of the parser
            String parserName = PropertyService.getProperty("xml.saxparser");
            parser = XMLReaderFactory.createXMLReader(parserName);
            XMLSchemaService.getInstance().populateRegisteredSchemaList();
            if (ruleBase != null && ruleBase.equals(EML200)) {
                logMetacat.info("DocumentImpl.initalizeParser - Using eml 2.0.0 parser");
                chandler = new Eml200SAXHandler(dbconn, action, docid, rev,
                        user, groups, pub, serverCode, createDate, updateDate, writeAccessRules, guidsToSync);
                chandler.setIsRevisionDoc(isRevision);
                chandler.setEncoding(encoding);
                parser.setContentHandler((ContentHandler) chandler);
                parser.setErrorHandler((ErrorHandler) chandler);
                parser.setProperty(DECLARATIONHANDLERPROPERTY, chandler);
                parser.setProperty(LEXICALPROPERTY, chandler);
                // turn on schema validation feature
                parser.setFeature(VALIDATIONFEATURE, true);
                parser.setFeature(NAMESPACEFEATURE, true);
                //parser.setFeature(NAMESPACEPREFIXESFEATURE, true);
                parser.setFeature(SCHEMAVALIDATIONFEATURE, true);
                // From DB to find the register external schema location
                String externalSchemaLocation = null;
//                SchemaLocationResolver resolver = new SchemaLocationResolver();
                externalSchemaLocation = XMLSchemaService.getInstance().getNameSpaceAndLocationString();
                logMetacat.debug("DocumentImpl.initalizeParser - 2.0.0 external schema location: " + externalSchemaLocation);
                // Set external schemalocation.
                if (externalSchemaLocation != null
                        && !(externalSchemaLocation.trim()).equals("")) {
                    parser.setProperty(EXTERNALSCHEMALOCATIONPROPERTY,
                            externalSchemaLocation);
                }
                logMetacat.debug("DocumentImpl.initalizeParser - 2.0.0 parser configured");
            } else if (ruleBase != null && ruleBase.equals(EML210)) {
                logMetacat.info("DocumentImpl.initalizeParser - Using eml 2.1.0 parser");
                chandler = new Eml210SAXHandler(dbconn, action, docid, rev,
                        user, groups, pub, serverCode, createDate, updateDate, writeAccessRules, guidsToSync);
                chandler.setIsRevisionDoc(isRevision);
                chandler.setEncoding(encoding);
                parser.setContentHandler((ContentHandler) chandler);
                parser.setErrorHandler((ErrorHandler) chandler);
                parser.setProperty(DECLARATIONHANDLERPROPERTY, chandler);
                parser.setProperty(LEXICALPROPERTY, chandler);
                // turn on schema validation feature
                parser.setFeature(VALIDATIONFEATURE, true);
                parser.setFeature(NAMESPACEFEATURE, true);
                //parser.setFeature(NAMESPACEPREFIXESFEATURE, true);
                parser.setFeature(SCHEMAVALIDATIONFEATURE, true);
                // From DB to find the register external schema location
                String externalSchemaLocation = null;
                externalSchemaLocation = XMLSchemaService.getInstance().getNameSpaceAndLocationString();
                logMetacat.debug("DocumentImpl.initalizeParser - 2.1.0 external schema location: " + externalSchemaLocation);
                // Set external schemalocation.
                if (externalSchemaLocation != null
                        && !(externalSchemaLocation.trim()).equals("")) {
                    parser.setProperty(EXTERNALSCHEMALOCATIONPROPERTY,
                            externalSchemaLocation);
                }
                logMetacat.debug("DocumentImpl.initalizeParser - Using eml 2.1.0 parser configured");
            } else {
                //create a DBSAXHandler object which has the revision
                // specification
                chandler = new DBSAXHandler(dbconn, action, docid, rev, user,
                        groups, pub, serverCode, createDate, updateDate, writeAccessRules);
                chandler.setIsRevisionDoc(isRevision);
                chandler.setEncoding(encoding);
                parser.setContentHandler((ContentHandler) chandler);
                parser.setErrorHandler((ErrorHandler) chandler);
                parser.setProperty(DECLARATIONHANDLERPROPERTY, chandler);
                parser.setProperty(LEXICALPROPERTY, chandler);

                if (ruleBase != null && ruleBase.equals(SCHEMA)
                        && needValidation) {
                
                    XMLSchemaService xmlss = XMLSchemaService.getInstance();
                    xmlss.doRefresh();
                    logMetacat.info("DocumentImpl.initalizeParser - Using General schema parser");
                    // turn on schema validation feature
                    parser.setFeature(VALIDATIONFEATURE, true);
                    parser.setFeature(NAMESPACEFEATURE, true);
                    //parser.setFeature(NAMESPACEPREFIXESFEATURE, true);
                    parser.setFeature(SCHEMAVALIDATIONFEATURE, true);
                    
                    Vector<XMLSchema> schemaList = xmlss.findSchemasInXML((StringReader)xml);
                    boolean allSchemasRegistered = 
                    	xmlss.areAllSchemasRegistered(schemaList);
                    if (xmlss.useFullSchemaValidation() && !allSchemasRegistered) {
                    	parser.setFeature(FULLSCHEMAVALIDATIONFEATURE, true);
                    }
                    // From DB to find the register external schema location
                    String externalSchemaLocation = null;
                    externalSchemaLocation = xmlss.getNameSpaceAndLocationString();
                    logMetacat.debug("DocumentImpl.initalizeParser - Generic external schema location: " + externalSchemaLocation);              
                    // Set external schemalocation.
                    if (externalSchemaLocation != null
                            && !(externalSchemaLocation.trim()).equals("")) {
                        parser.setProperty(EXTERNALSCHEMALOCATIONPROPERTY,
                                externalSchemaLocation);
                    }

                } else if (ruleBase != null && ruleBase.equals(DTD)
                        && needValidation) {
                    logMetacat.info("DocumentImpl.initalizeParser - Using dtd parser");
                    // turn on dtd validaton feature
                    parser.setFeature(VALIDATIONFEATURE, true);
                    eresolver = new DBEntityResolver(dbconn,
                            (DBSAXHandler) chandler, dtd);
                    dtdhandler = new DBDTDHandler(dbconn);
                    parser.setEntityResolver((EntityResolver) eresolver);
                    parser.setDTDHandler((DTDHandler) dtdhandler);
                } else {
                    logMetacat.info("DocumentImpl.initalizeParser - Using other parser");
                    // non validation
                    parser.setFeature(VALIDATIONFEATURE, false);
                    eresolver = new DBEntityResolver(dbconn,
                            (DBSAXHandler) chandler, dtd);
                    dtdhandler = new DBDTDHandler(dbconn);
                    parser.setEntityResolver((EntityResolver) eresolver);
                    parser.setDTDHandler((DTDHandler) dtdhandler);
                }
            }//else
        } catch (Exception e) {
            throw e;
        }
        return parser;
    }

    /**
     * Save a document entry in the xml_revisions and xml_nodes_revision
     *  table Connection use as a
     * paramter is in order to rollback feature
     */
    private static void archiveDocAndNodesRevision(DBConnection dbconn, String docid,
            String user, DocumentImpl doc)
    {
        

        // create a record in xml_revisions table
        // for that document as selected from xml_documents

        try {
            if (doc == null) {
                String accNumber = docid + PropertyService.getProperty("document.accNumSeparator") +
                DBUtil.getLatestRevisionInDocumentTable(docid);
                    doc = new DocumentImpl(accNumber);
            }
            
            long rootNodeId = doc.getRootNodeID();

            archiveDocAndNodesRevison(dbconn, docid, user, rootNodeId);

        }catch (Exception e) {
            logMetacat.error(
                    "DocumentImpl.archiveDocAndNodesRevision - Error in DocumentImpl.archiveDocRevision : "
                            + e.getMessage());
        }
       
        
    }
    
    /**
     * This method will archive both xml_revision and xml_nodes_revision.
     * @param dbconn
     * @param docid
     * @param user
     * @param rootNodeId
     * @throws Exception
     */
    private static void archiveDocAndNodesRevison(DBConnection dbconn, String docid, 
                                     String user, long rootNodeId) throws Exception
    {
        String sysdate = DatabaseService.getInstance().getDBAdapter().getDateTimeFunction();
        //DBConnection conn = null;
        //int serialNumber = -1;
        PreparedStatement pstmt = null;
      try
      {
        // Move the nodes from xml_nodes to xml_nodes_revisions table...
        moveNodesToNodesRevision(dbconn, rootNodeId);
        //archiveDocRevision(docid, user);
        //Move the document information to xml_revisions table...
        double start = System.currentTimeMillis()/1000;
        pstmt = dbconn.prepareStatement("INSERT INTO xml_revisions "
                + "(docid, rootnodeid, docname, doctype, "
                + "user_owner, user_updated, date_created, date_updated, "
                + "server_location, rev, public_access, catalog_id) "
                + "SELECT ?, rootnodeid, docname, doctype, "
                + "user_owner, ?, date_created, date_updated, "
                + "server_location, rev, public_access, catalog_id "
                + "FROM xml_documents " + "WHERE docid = ?");

        // Increase dbconnection usage count
        dbconn.increaseUsageCount(1);
        // Bind the values to the query and execute it
        pstmt.setString(1, docid);
        pstmt.setString(2, user);
        pstmt.setString(3, docid);
        logMetacat.debug("DocumentImpl.archiveDocAndNodesRevison - executing SQL: " + pstmt.toString());
        pstmt.execute();
        pstmt.close();
        double end = System.currentTimeMillis()/1000;
        logMetacat.debug("DocumentImpl.archiveDocAndNodesRevision - moving docs from xml_documents to xml_revision takes "+(end -start));

      } catch (SQLException e) {
        logMetacat.error("DocumentImpl.archiveDocAndNodesRevision - SQL error: "
                        + e.getMessage());
        throw e;
      } catch (Exception e) {
        logMetacat.error("DocumentImpl.archiveDocAndNodesRevision - General error: "
                        + e.getMessage());
        throw e;
      }
      finally {
        try {
            pstmt.close();
        } catch (SQLException ee) {
            logMetacat.error(
                    "DocumentImpl.archiveDocAndNodesRevision - SQL error when closing prepared statement: "
                            + ee.getMessage());
        }
      }
       
    }
    
    private static void updateNodeValues(DBConnection dbConnection, String docid) throws SQLException {
    	PreparedStatement sqlStatement = null;
        PreparedStatement pstmt = null;
        ResultSet rset = null;

        sqlStatement = dbConnection.prepareStatement(
        		"SELECT DISTINCT NODEID, NODEDATA "
                + "FROM xml_nodes "
                + "WHERE nodedata IS NOT NULL "
                + "AND docid = ?");
        sqlStatement.setString(1, docid);
        rset = sqlStatement.executeQuery();

        int count = 0;
        while (rset.next()) {

            String nodeid = rset.getString(1);
            String nodedata = rset.getString(2);

            try {
                if (!nodedata.trim().equals("")) {
                	
                	try {
                		double dataNumeric = Double.parseDouble(nodedata);
	                    pstmt = dbConnection.prepareStatement(
	                        "UPDATE xml_nodes " +
	                        " SET nodedatanumerical = ?" +
	                        " WHERE nodeid = " + nodeid);
	                    pstmt.setDouble(1, dataNumeric);
	                    pstmt.execute();
	                    pstmt.close();
                	} catch (Exception e) {
                		// try a date
                		try {
	                		Calendar dataDateValue = DatatypeConverter.parseDateTime(nodedata);
		                    Timestamp dataTimestamp = new Timestamp(dataDateValue.getTimeInMillis());
		                    pstmt = dbConnection.prepareStatement(
		                        "UPDATE xml_nodes " +
		                        " SET nodedatadate = ?" +
		                        " WHERE nodeid = " + nodeid);
		                    pstmt.setTimestamp(1, dataTimestamp);
		                    pstmt.execute();
		                    pstmt.close();
                		} catch (Exception e2) {
							// we are done with this node
						} 
					}

                    count++;
                    if (count%5 == 0) {
                        logMetacat.info(count + "...");
                    }
            		
                }
            } catch (Exception e) {
            	// do nothing, was not a valid date
            	e.printStackTrace();
            } 
        }

        rset.close();
        sqlStatement.close();
    }
    
    private static void moveNodesToNodesRevision(DBConnection dbconn,
                                       long rootNodeId) throws Exception
    {
        logMetacat.debug("DocumentImpl.moveNodesToNodesRevision - the root node id is "+rootNodeId+
                " will be moved from xml_nodes to xml_node_revision table");
        PreparedStatement pstmt = null;
        double start = System.currentTimeMillis()/1000;
        // Move the nodes from xml_nodes to xml_revisions table...
        pstmt = dbconn.prepareStatement("INSERT INTO xml_nodes_revisions "
                + "(nodeid, nodeindex, nodetype, nodename, nodeprefix, "
                + "nodedata, parentnodeid, rootnodeid, docid, date_created,"
                + " date_updated, nodedatanumerical, nodedatadate) "
                + "SELECT nodeid, nodeindex, nodetype, nodename, nodeprefix, "  
                + "nodedata, parentnodeid, rootnodeid, docid, date_created,"
                + " date_updated, nodedatanumerical, nodedatadate "
                + "FROM xml_nodes WHERE rootnodeid = ?");

        // Increase dbconnection usage count
        dbconn.increaseUsageCount(1);
        // Bind the values to the query and execute it
        pstmt.setLong(1, rootNodeId);
        logMetacat.debug("DocumentImpl.moveNodesToNodesRevision - executing SQL: " + pstmt.toString());
        pstmt.execute();
        pstmt.close();
        double end = System.currentTimeMillis()/1000;
        logMetacat.debug("DocumentImpl.moveNodesToNodesRevision - Moving nodes from xml_nodes to xml_nodes_revision takes "+(end -start));
        

    }

    /** Save a document entry in the xml_revisions table */
    private static void archiveDocRevision(String docid, String user, DBConnection conn) throws Exception
    {
        String sysdate = DatabaseService.getInstance().getDBAdapter().getDateTimeFunction();
        PreparedStatement pstmt = null;

        // create a record in xml_revisions table
        // for that document as selected from xml_documents

        try {
            //check out DBConnection
            pstmt = conn.prepareStatement("INSERT INTO xml_revisions "
                    + "(docid, rootnodeid, docname, doctype, "
                    + "user_owner, user_updated, date_created, date_updated, "
                    + "server_location, rev, public_access, catalog_id) "
                    + "SELECT ?, rootnodeid, docname, doctype, "
                    + "user_owner, ?, date_created, date_updated, "
                    + "server_location, rev, public_access, catalog_id "
                    + "FROM xml_documents " + "WHERE docid = ?");
            
            // Bind the values to the query and execute it
            conn.increaseUsageCount(1);
            pstmt.setString(1, docid);
            pstmt.setString(2, user);
            pstmt.setString(3, docid);
            logMetacat.debug("DocumentImpl.archiveDocRevision - executing SQL: " + pstmt.toString());
            pstmt.execute();
            pstmt.close();
        } catch (SQLException e) {
            logMetacat.error("DocumentImpl.archiveDocRevision - SQL error: "
                            + e.getMessage());
            throw e;
        } finally {
            try {
                pstmt.close();
            } catch (SQLException ee) {
                logMetacat.error("DocumentImpl.archiveDocRevision - SQL Error: "
                                + ee.getMessage());
                throw ee;
            } 
        }
    }

    /**
     * delete a entry in xml_table for given docid
     *
     * @param docId,
     *            the id of the document need to be delete
     */
    private static void deleteXMLDocuments(String docId) throws SQLException
    {
        DBConnection conn = null;
        int serialNumber = -1;
        PreparedStatement pStmt = null;
        try {
            //check out DBConnection
            conn = DBConnectionPool
                    .getDBConnection("DocumentImpl.deleteXMLDocuments");
            serialNumber = conn.getCheckOutSerialNumber();
            //delete a record
            pStmt = conn.prepareStatement(
                    "DELETE FROM xml_documents WHERE docid = ? ");
            pStmt.setString(1, docId);
            logMetacat.debug("DocumentImpl.deleteXMLDocuments - executing SQL: " + pStmt.toString());
            pStmt.execute();
        } finally {
            try {
                pStmt.close();
            } catch (SQLException e) {
                logMetacat.error("DocumentImpl.deleteXMLDocuments - SQL error: "
                                + e.getMessage());
            } finally {
                //return back DBconnection
                DBConnectionPool.returnDBConnection(conn, serialNumber);
            }
        }
    }

   

    /**
     * Get server location form database for a accNum
     *
     * @param accum
     *            <sitecode>. <uniqueid>. <rev>
     */
    private static int getServerLocationNumber(String accNum)
            throws SQLException
    {
        //get rid of revNum part
        String docId = DocumentUtil.getDocIdFromString(accNum);
        PreparedStatement pStmt = null;
        int serverLocation = 1;
        DBConnection conn = null;
        int serialNumber = -1;

        try {
            //check out DBConnection
            conn = DBConnectionPool
                    .getDBConnection("DocumentImpl.getServerLocationNumber");
            serialNumber = conn.getCheckOutSerialNumber();

            pStmt = conn
                    .prepareStatement("SELECT server_location FROM xml_documents WHERE docid = ?");
            pStmt.setString(1, docId);
            pStmt.execute();

            ResultSet rs = pStmt.getResultSet();
            boolean hasRow = rs.next();
            //if there is entry in xml_documents, get the serverlocation
            if (hasRow) {
                serverLocation = rs.getInt(1);
                pStmt.close();
            } else {
                //if htere is no entry in xml_documents, we consider it is new
                // document
                //the server location is local host and value is 1
                serverLocation = 1;
                pStmt.close();
            }
        }//try
        finally {
            try {
                pStmt.close();
            }//try
            catch (Exception ee) {
                logMetacat.error("DocumentImpl.getServerLocationNumber - General error: "
                                + ee.getMessage());
            }//catch
            finally {
                DBConnectionPool.returnDBConnection(conn, serialNumber);
            }//finally
        }//finally

        return serverLocation;
    }

    /**
     * Given a server name, return its servercode in xml_replication table. If
     * no server is found, -1 will return
     *
     * @param serverName,
     */
    private static int getServerCode(String serverName)
    {
        PreparedStatement pStmt = null;
        int serverLocation = -2;
        DBConnection dbConn = null;
        int serialNumber = -1;

        //we should consider about local host too
        if (serverName.equals(MetacatUtil.getLocalReplicationServerName())) {
            serverLocation = 1;
            return serverLocation;
        }

        try {
            //check xml_replication table
            //dbConn=util.openDBConnection();
            //check out DBConnection
            dbConn = DBConnectionPool
                    .getDBConnection("DocumentImpl.getServerCode");
            serialNumber = dbConn.getCheckOutSerialNumber();
            pStmt = dbConn
                    .prepareStatement("SELECT serverid FROM xml_replication WHERE server = ?");
            pStmt.setString(1, serverName);
            pStmt.execute();

            ResultSet rs = pStmt.getResultSet();
            boolean hasRow = rs.next();
            //if there is entry in xml_replication, get the serverid
            if (hasRow) {
                serverLocation = rs.getInt(1);
                pStmt.close();
            } else {
                // if htere is no entry in xml_replication, -1 will return
                serverLocation = -1;
                pStmt.close();
            }
        } catch (Exception e) {
            logMetacat.error("DocumentImpl.getServerCode - General Error: "
                    + e.getMessage());
        } finally {
            try {
                pStmt.close();
            } catch (Exception ee) {
                logMetacat.error("DocumentImpl.getServerCode - General error: "
                                + ee.getMessage());
            } finally {
                DBConnectionPool.returnDBConnection(dbConn, serialNumber);
            }
        }

        return serverLocation;
    }

    /**
     * Insert a server into xml_replcation table
     *
     * @param server,
     *            the name of server
     */
    private static synchronized void insertServerIntoReplicationTable(
            String server)
    {
        PreparedStatement pStmt = null;
        DBConnection dbConn = null;
        int serialNumber = -1;

        // Initial value for the server
        int replicate = 0;
        int dataReplicate = 0;
        int hub = 0;

        try {
            // Get DBConnection
            dbConn = DBConnectionPool
                    .getDBConnection("DocumentImpl.insertServIntoReplicationTable");
            serialNumber = dbConn.getCheckOutSerialNumber();

            // Compare the server to dabase
            pStmt = dbConn
                    .prepareStatement("SELECT serverid FROM xml_replication WHERE server = ?");
            pStmt.setString(1, server);
            pStmt.execute();
            ResultSet rs = pStmt.getResultSet();
            boolean hasRow = rs.next();
            // Close preparedstatement and result set
            pStmt.close();
            rs.close();

            // If the server is not in the table, and server is not local host,
            // insert it
            if (!hasRow
                    && !server.equals(MetacatUtil
                            .getLocalReplicationServerName())) {
                // Set auto commit false
                dbConn.setAutoCommit(false);
                /*
                 * pStmt = dbConn.prepareStatement("INSERT INTO xml_replication " +
                 * "(server, last_checked, replicate, datareplicate, hub) " +
                 * "VALUES ('" + server + "', to_date(" + "'01/01/00',
                 * 'MM/DD/YY'), '" + replicate +"', '"+dataReplicate+"','"+ hub +
                 * "')");
                 */
                
                Calendar cal = Calendar.getInstance();
                cal.set(1980, 1, 1);
                pStmt = dbConn
                        .prepareStatement("INSERT INTO xml_replication "
                                + "(server, last_checked, replicate, datareplicate, hub) "
                                + "VALUES (?, ?, ?, ?, ?)");
                pStmt.setString(1, server);
				pStmt.setTimestamp(2, new Timestamp(cal.getTimeInMillis()) );
                pStmt.setInt(3, replicate);
                pStmt.setInt(4, dataReplicate);
                pStmt.setInt(5, hub);

                logMetacat.debug("DocumentImpl.insertServerIntoReplicationTable - executing SQL: " + pStmt.toString());
                pStmt.execute();
                dbConn.commit();
                // Increase usage number
                dbConn.increaseUsageCount(1);
                pStmt.close();

            }
        }//try
        catch (Exception e) {
            logMetacat.error("DocumentImpl.insertServerIntoReplicationTable - General error: "
                            + e.getMessage());
        }//catch
        finally {

            try {
                // Set auto commit true
                dbConn.setAutoCommit(true);
                pStmt.close();

            }//try
            catch (Exception ee) {
                logMetacat.error("DocumentImpl.insertServerIntoReplicationTable - General error: "
                                + ee.getMessage());
            }//catch
            finally {
                DBConnectionPool.returnDBConnection(dbConn, serialNumber);
            }

        }//finally

    }

    /**
     * the main routine used to test the DBWriter utility.
     * <p>
     * Usage: java DocumentImpl <-f filename -a action -d docid>
     *
     * @param filename
     *            the filename to be loaded into the database
     * @param action
     *            the action to perform (READ, INSERT, UPDATE, DELETE)
     * @param docid
     *            the id of the document to process
     */
    static public void main(String[] args)
    {
        DBConnection dbconn = null;
        int serialNumber = -1;
        try {
            String filename = null;
            String dtdfilename = null;
            String action = null;
            String docid = null;
            boolean showRuntime = false;
            boolean useOldReadAlgorithm = false;

            // Parse the command line arguments
            for (int i = 0; i < args.length; ++i) {
                if (args[i].equals("-f")) {
                    filename = args[++i];
                } else if (args[i].equals("-r")) {
                    dtdfilename = args[++i];
                } else if (args[i].equals("-a")) {
                    action = args[++i];
                } else if (args[i].equals("-d")) {
                    docid = args[++i];
                } else if (args[i].equals("-t")) {
                    showRuntime = true;
                } else if (args[i].equals("-old")) {
                    useOldReadAlgorithm = true;
                } else {
                    System.err.println("   args[" + i + "] '" + args[i]
                            + "' ignored.");
                }
            }

            // Check if the required arguments are provided
            boolean argsAreValid = false;
            if (action != null) {
                if (action.equals("INSERT")) {
                    if (filename != null) {
                        argsAreValid = true;
                    }
                } else if (action.equals("UPDATE")) {
                    if ((filename != null) && (docid != null)) {
                        argsAreValid = true;
                    }
                } else if (action.equals("DELETE")) {
                    if (docid != null) {
                        argsAreValid = true;
                    }
                } else if (action.equals("READ")) {
                    if (docid != null) {
                        argsAreValid = true;
                    }
                }
            }

            // Print usage message if the arguments are not valid
            if (!argsAreValid) {
                System.err.println("Wrong number of arguments!!!");
                System.err
                        .println("USAGE: java DocumentImpl [-t] <-a INSERT> [-d docid] <-f filename> "
                                + "[-r dtdfilename]");
                System.err
                        .println("   OR: java DocumentImpl [-t] <-a UPDATE -d docid -f filename> "
                                + "[-r dtdfilename]");
                System.err
                        .println("   OR: java DocumentImpl [-t] <-a DELETE -d docid>");
                System.err
                        .println("   OR: java DocumentImpl [-t] [-old] <-a READ -d docid>");
                return;
            }

            // Time the request if asked for
            double startTime = System.currentTimeMillis();

            // Open a connection to the database

            dbconn = DBConnectionPool.getDBConnection("DocumentImpl.main");
            serialNumber = dbconn.getCheckOutSerialNumber();

            double connTime = System.currentTimeMillis();
            // Execute the action requested (READ, INSERT, UPDATE, DELETE)
            if (action.equals("READ")) {
                DocumentImpl xmldoc = new DocumentImpl(docid);
                if (useOldReadAlgorithm) {
                    logMetacat.error("DocumentImpl.main - " + xmldoc.readUsingSlowAlgorithm());
                } else {
                    xmldoc.toXml(System.out, null, null, true);
                }
            } else if (action.equals("DELETE")) {
                DocumentImpl.delete(docid, null, null, null, false);
                //System.out.println("Document deleted: " + docid);
            } else {
                /*
                 * String newdocid = DocumentImpl.write(dbconn, filename, null,
                 * dtdfilename, action, docid, null, null); if ((docid != null) &&
                 * (!docid.equals(newdocid))) { if (action.equals("INSERT")) {
                 * System.out.println("New document ID generated!!! "); } else
                 * if (action.equals("UPDATE")) { System.out.println("ERROR:
                 * Couldn't update document!!! "); } } else if ((docid == null) &&
                 * (action.equals("UPDATE"))) { System.out.println("ERROR:
                 * Couldn't update document!!! "); }
                 * System.out.println("Document processing finished for: " +
                 * filename + " (" + newdocid + ")");
                 */
            }

            double stopTime = System.currentTimeMillis();
            double dbOpenTime = (connTime - startTime) / 1000;
            double insertTime = (stopTime - connTime) / 1000;
            double executionTime = (stopTime - startTime) / 1000;
            if (showRuntime) {
                logMetacat.info("DocumentImpl.main - Total Execution time was: "
                        + executionTime + " seconds.");
                logMetacat.info("DocumentImpl.main - Time to open DB connection was: "
                        + dbOpenTime + " seconds.");
                logMetacat.info("DocumentImpl.main - Time to insert document was: " + insertTime
                        + " seconds.");
            }
            dbconn.close();
        } catch (McdbException me) {
            me.toXml(new OutputStreamWriter(System.err));
        } catch (AccessionNumberException ane) {
            System.err.println(ane.getMessage());
        } catch (Exception e) {
            System.err.println("EXCEPTION HANDLING REQUIRED");
            System.err.println(e.getMessage());
            e.printStackTrace(System.err);
        } finally {
            // Return db connection
            DBConnectionPool.returnDBConnection(dbconn, serialNumber);
        }
    }
    
    /*
     * This method will write a record to revision table base on given
     * info. The create date and update will be current time.
     * If rootNodeId < 0, this means it has not rootid
     */
    private static void writeDocumentToRevisionTable(DBConnection con, String docId, 
            String rev, String docType, String docName, String user, 
            String catalogid, int serverCode, long rootNodeId, Date createDate, Date updateDate) throws SQLException, Exception
    {
        
        try 
        {
            Date today = Calendar.getInstance().getTime();
            if (createDate == null){
                createDate = today;
            }
            
            logMetacat.debug("DocumentImpl.writeDocumentToRevisionTable - the create date is "+createDate);
            if (updateDate == null){
                updateDate = today;
            }
            logMetacat.debug("DocumentImpl.writeDocumentToRevisionTable - the update date is "+updateDate);
            PreparedStatement pstmt = null;
            String sql = null;
            logMetacat.debug("DocumentImpl.writeDocumentToRevisionTable - the root node id is "+rootNodeId);
            if (rootNodeId <= 0)
            {
            	// this is for data file, not rootnodeid need
               sql = "INSERT INTO xml_revisions "
                   + "(docid, docname, doctype, user_owner, "
                   + "user_updated, date_created, date_updated, "
                   + "public_access, server_location, rev) "
                   + "VALUES (?, ?, ?, ?, ?, ?,"
                   + " ?, ?, ?, ?)";
            }
            else
            {
            	if (catalogid != null)
                {
                    sql = "INSERT INTO xml_revisions "
                    + "(docid, docname, doctype, user_owner, "
                    + "user_updated, date_created, date_updated, "
                    + "public_access, server_location, rev, catalog_id, rootnodeid ) "
                    + "VALUES (?, ?, ?, ?, ?, ?, "
                    + "?, ?, ?, ?, ?, ?)";
                }
                else
                {
                    sql = "INSERT INTO xml_revisions "
                        + "(docid, docname, doctype, user_owner, "
                        + "user_updated, date_created, date_updated, "
                        + "public_access, server_location, rev, rootnodeid ) "
                        + "VALUES (?, ?, ?, ?, ?, ?, "
                        + "?, ?, ?, ?, ?)";
                }
            }
            pstmt = con.prepareStatement(sql);
             // Increase dbconnection usage count
            con.increaseUsageCount(1);

            // Bind the values to the query
            pstmt.setString(1, docId);
            logMetacat.debug("DocumentImpl.writeDocumentToRevisionTable - docid is "+docId);
            pstmt.setString(2, docName);
            logMetacat.debug("DocumentImpl.writeDocumentToRevisionTable - docname is "+docName);
            pstmt.setString(3, docType);
            logMetacat.debug("DocumentImpl.writeDocumentToRevisionTable - docType is "+docType);
            pstmt.setString(4, user);
            logMetacat.debug("DocumentImpl.writeDocumentToRevisionTable - onwer is "+user);
            pstmt.setString(5, user);
            logMetacat.debug("DocumentImpl.writeDocumentToRevisionTable - update user is "+user);
            pstmt.setTimestamp(6, new Timestamp(createDate.getTime()));
            pstmt.setTimestamp(7, new Timestamp(updateDate.getTime()));
            
            pstmt.setInt(8, 0);
            
            pstmt.setInt(9, serverCode);
            logMetacat.debug("DocumentImpl.writeDocumentToRevisionTable - server code is "+serverCode);
            pstmt.setInt(10, Integer.parseInt(rev));
            logMetacat.debug("DocumentImpl.writeDocumentToRevisionTable - rev is "+rev);
            
            if (rootNodeId > 0 )
            {
              if (catalogid != null)
              {
                pstmt.setInt(11, (new Integer(catalogid)).intValue());
                logMetacat.debug("DocumentImpl.writeDocumentToRevisionTable - catalog id is "+catalogid);
                pstmt.setLong(12, rootNodeId);
                logMetacat.debug("DocumentImpl.writeDocumentToRevisionTable - root id is "+rootNodeId);
              }
              else
              {
                 pstmt.setLong(11, rootNodeId);
                 logMetacat.debug("DocumentImpl.writeDocumentToRevisionTable - root id is "+rootNodeId); 
              }
            }
            // Do the insertion
            logMetacat.debug("DocumentImpl.writeDocumentToRevisionTable - executing SQL: " + pstmt.toString());
            pstmt.execute();
            pstmt.close();
            logMetacat.debug("DocumentImpl.writeDocumentToRevisionTable - end of write into revisons");
           
        } 
        catch (SQLException sqle) 
        {
        	logMetacat.error("DocumentImpl.writeDocumentToRevisionTable - SQL error: " + sqle.getMessage());
        	sqle.printStackTrace();
            throw sqle;
        } 
        catch (Exception e) 
        {
        	logMetacat.error("DocumentImpl.writeDocumentToRevisionTable - General error: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
    
    /**
     * This method will generate record in xml_revision table for a data file
     * The reason why we need this method is because data file would be parsed by 
     * xml parser. So the constructor would be called for data file and this
     * method will replace the function
     */
    static private void registerDeletedDataFile(String docname,
            String doctype, String accnum, String user, int serverCode, 
            Date createDate, Date updateDate) throws Exception
    {
        DBConnection dbconn = null;
        int serialNumber = -1;
//        AccessionNumber ac;
//        PreparedStatement pstmt = null;
//        String action = null;
        try 
        {
            //dbconn = util.openDBConnection();
            dbconn = DBConnectionPool.getDBConnection(
                    "DeletedDocumentImpl.registerDeletedDataFile");
            serialNumber = dbconn.getCheckOutSerialNumber();
            String docIdWithoutRev = 
            	DocumentUtil.getDocIdFromAccessionNumber(accnum);
            String rev = DocumentUtil.getRevisionStringFromString(accnum);
            writeDocumentToRevisionTable(dbconn, docIdWithoutRev, 
                    rev, doctype, docname, user,
                    null, serverCode, -1, createDate, updateDate); 
            dbconn.close();
        } 
        finally 
        {
            
            DBConnectionPool.returnDBConnection(dbconn, serialNumber);
        }
    }
    
    /*
     * This method will delete the xml_nodes table for a given root id
     * This method will be called in the time_replication for revision table
     * In revision replication, xml first will insert into xml_nodes, then
     * move to xml_nodes_revision and register into xml_revsion table.
     * if in the second step some error happend, we need to delete the
     * node in xml_nodes table as roll back
     */
    static private void deleteXMLNodes(DBConnection dbconn, long rootId) throws Exception
    {
//        AccessionNumber ac;
    	logMetacat.debug("DocumentImpl.deleteXMLNodes - for root Id: " + rootId);
        PreparedStatement pstmt = null;
        double start = System.currentTimeMillis()/1000;
        String sql = "DELETE FROM xml_nodes WHERE rootnodeid = ? ";
        pstmt = dbconn.prepareStatement(sql);
        pstmt.setLong(1, rootId);
        // Increase dbconnection usage count
        dbconn.increaseUsageCount(1);
        logMetacat.debug("DocumentImpl.deleteXMLNodes - executing SQL: " + pstmt.toString());
        pstmt.execute();
        pstmt.close(); 
        double end = System.currentTimeMillis()/1000;
        logMetacat.info("DocumentImpl.deleteXMLNodes - The time to delete xml_nodes in UPDATE is "+(end -start));
     
    }
}
