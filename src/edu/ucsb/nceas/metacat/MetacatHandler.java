/**
 *  '$RCSfile$'
 *  Copyright: 2010 Regents of the University of California and the
 *             National Center for Ecological Analysis and Synthesis
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
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.activation.MimetypesFileTypeMap;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.XmlStreamReader;
import org.apache.log4j.Logger;
import org.dataone.service.types.v1.AccessPolicy;
import org.dataone.service.types.v1.Event;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Session;
import org.dataone.service.types.v1.SystemMetadata;
import org.ecoinformatics.eml.EMLParser;

import au.com.bytecode.opencsv.CSVWriter;

import com.oreilly.servlet.multipart.FilePart;
import com.oreilly.servlet.multipart.MultipartParser;
import com.oreilly.servlet.multipart.ParamPart;
import com.oreilly.servlet.multipart.Part;

import edu.ucsb.nceas.metacat.accesscontrol.AccessControlException;
import edu.ucsb.nceas.metacat.accesscontrol.AccessControlForSingleFile;
import edu.ucsb.nceas.utilities.access.AccessControlInterface;
import edu.ucsb.nceas.metacat.accesscontrol.AccessControlList;
import edu.ucsb.nceas.metacat.cart.CartManager;
import edu.ucsb.nceas.metacat.client.InsufficientKarmaException;
import edu.ucsb.nceas.metacat.common.query.EnabledQueryEngines;
import edu.ucsb.nceas.metacat.database.DBConnection;
import edu.ucsb.nceas.metacat.database.DBConnectionPool;
import edu.ucsb.nceas.metacat.dataone.D1NodeService;
import edu.ucsb.nceas.metacat.dataone.SyncAccessPolicy;
import edu.ucsb.nceas.metacat.dataone.SystemMetadataFactory;
import edu.ucsb.nceas.metacat.dataone.hazelcast.HazelcastService;
import edu.ucsb.nceas.metacat.dataquery.DataQuery;
import edu.ucsb.nceas.metacat.event.MetacatDocumentEvent;
import edu.ucsb.nceas.metacat.event.MetacatEventService;
import edu.ucsb.nceas.metacat.index.MetacatSolrIndex;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.metacat.replication.ForceReplicationHandler;
import edu.ucsb.nceas.metacat.service.SessionService;
import edu.ucsb.nceas.metacat.service.XMLSchemaService;
import edu.ucsb.nceas.metacat.shared.HandlerException;
import edu.ucsb.nceas.metacat.shared.MetacatUtilException;
import edu.ucsb.nceas.metacat.shared.ServiceException;
import edu.ucsb.nceas.metacat.spatial.SpatialHarvester;
import edu.ucsb.nceas.metacat.spatial.SpatialQuery;
import edu.ucsb.nceas.metacat.util.AuthUtil;
import edu.ucsb.nceas.metacat.util.DocumentUtil;
import edu.ucsb.nceas.metacat.util.MetacatUtil;
import edu.ucsb.nceas.metacat.util.RequestUtil;
import edu.ucsb.nceas.metacat.util.SystemUtil;
import edu.ucsb.nceas.utilities.FileUtil;
import edu.ucsb.nceas.utilities.LSIDUtil;
import edu.ucsb.nceas.utilities.ParseLSIDException;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;

/**
 * General entry point for the Metacat server which is called from various 
 * mechanisms such as the standard MetacatServlet class and the various web
 * service servlets such as RestServlet class.  All application logic should be
 * encapsulated in this class, and the calling classes should only contain
 * parameter marshaling and demarshaling code, delegating all else to this
 * MetacatHandler instance.
 * @author Matthew Jones
 */
public class MetacatHandler {

    private static boolean _sitemapScheduled = false;
    
    private static Logger logMetacat = Logger.getLogger(MetacatHandler.class);

    // Constants -- these should be final in a servlet    
    private static final String PROLOG = "<?xml version=\"1.0\"?>";
    private static final String SUCCESS = "<success>";
    private static final String SUCCESSCLOSE = "</success>";
    private static final String ERROR = "<error>";
    private static final String ERRORCLOSE = "</error>";
    public static final String FGDCDOCTYPE = "metadata";
    
	private Timer timer;
	
    public MetacatHandler(Timer timer) {
    	this.timer = timer;
    }
    
    
    protected void handleDataquery(
            Hashtable<String, String[]> params,
            HttpServletResponse response,
            String sessionId) throws PropertyNotFoundException, IOException {
        
        DataQuery dq = null;
        if (sessionId != null) {
            dq = new DataQuery(sessionId);
        }
        else {
            dq = new DataQuery();
        }
        
        String dataqueryXML = (params.get("dataquery"))[0];

        ResultSet rs = null;
        try {
            rs = dq.executeQuery(dataqueryXML);
        } catch (Exception e) {
            //probably need to do something here
            e.printStackTrace();
            return;
        }
        
        //process the result set
        String qformat = "csv";
        String[] temp = params.get("qformat");
        if (temp != null && temp.length > 0) {
            qformat = temp[0];
        }
        String fileName = "query-results." + qformat;
        
        //get the results as csv file
        if (qformat != null && qformat.equalsIgnoreCase("csv")) {
            response.setContentType("text/csv");
            //response.setContentType("application/csv");
            response.setHeader("Content-Disposition", "attachment; filename=" + fileName);
            
            Writer writer = new OutputStreamWriter(response.getOutputStream());
            CSVWriter csv = new CSVWriter(writer, CSVWriter.DEFAULT_SEPARATOR, CSVWriter.NO_QUOTE_CHARACTER);
            try {
                
                csv.writeAll(rs, true);
                
                csv.flush();
                response.flushBuffer();
                 
                rs.close();
                
            } catch (SQLException e) {
                e.printStackTrace();
            }
            
            return;
        }
        
    }
    
    protected void handleEditCart(
            Hashtable<String, String[]> params,
            HttpServletResponse response,
            String sessionId) throws PropertyNotFoundException, IOException {
        
        CartManager cm = null;
        if (sessionId != null) {
            cm = new CartManager(sessionId);
        }
        else {
            cm = new CartManager();
        }
        
        String editOperation = (params.get("operation"))[0];
        
        String[] docids = params.get("docid");
        String[] field = params.get("field");
        String[] path = params.get("path");
        
        Map<String,String> fields = null;
        if (field != null && path != null) {
            fields = new HashMap<String,String>();
            fields.put(field[0], path[0]);
        }
        
        //TODO handle attribute map (metadata fields)
        cm.editCart(editOperation, docids, fields);
        
    }
    
    // ///////////////////////////// METACAT SPATIAL ///////////////////////////
    
    /**
     * handles all spatial queries -- these queries may include any of the
     * queries supported by the WFS / WMS standards
     * 
     * handleSQuery(out, params, response, username, groupnames, sess_id);
     * @throws HandlerException 
     */
    protected void handleSpatialQuery(Writer out, Hashtable<String, String[]> params,
            HttpServletResponse response,
            String username, String[] groupnames,
            String sess_id) throws PropertyNotFoundException, HandlerException {
        
        Logger logMetacat = Logger.getLogger(MetacatHandler.class);
        
        if ( !PropertyService.getProperty("spatial.runSpatialOption").equals("true") ) {
            response.setContentType("text/html");
            try {
				out.write("<html> Metacat Spatial Option is turned off </html>");
	            out.close();
			} catch (IOException e) {
				throw new HandlerException(e.getMessage());
			}
            return;
        }
        
        /*
         * Perform spatial query against spatial cache
         */
        float _xmax = Float.valueOf( (params.get("xmax"))[0] ).floatValue();
        float _ymax = Float.valueOf( (params.get("ymax"))[0] ).floatValue();
        float _xmin = Float.valueOf( (params.get("xmin"))[0] ).floatValue();
        float _ymin = Float.valueOf( (params.get("ymin"))[0] ).floatValue();
        SpatialQuery sq = new SpatialQuery();
        Vector<String> docids = sq.filterByBbox( _xmin, _ymin, _xmax, _ymax );
        // logMetacat.info(" --- Spatial Query completed. Passing on the SQuery
        // handler");
        // logMetacat.warn("\n\n ******* after spatial query, we've got " +
        // docids.size() + " docids \n\n");
        
        /*
         * Create an array matching docids
         */
        String [] docidArray = new String[docids.size()];
        docids.toArray(docidArray);
        
        /*
         * Create squery string
         */
        String squery = DocumentIdQuery.createDocidQuery( docidArray );
        // logMetacat.info("-----------\n" + squery + "\n------------------");
        String[] queryArray = new String[1];
        queryArray[0] = squery;
        params.put("query", queryArray);
        
        /*
         * Determine qformat
         */
        String[] qformatArray = new String[1];
        try {
            String _skin = (params.get("skin"))[0];
            qformatArray[0] = _skin;
        } catch (java.lang.NullPointerException e) {
            // should be "default" but keep this for backwards compatibility
            // with knp site
            logMetacat.warn("MetacatHandler.handleSpatialQuery - No SKIN specified for metacat actions=spatial_query... defaulting to 'knp' skin !\n");
            qformatArray[0] = "knp";
        }
        params.put("qformat", qformatArray);
        
        // change the action
        String[] actionArray = new String[1];
        actionArray[0] = "squery";
        params.put("action", actionArray);
        
        /*
         * Pass the docids to the DBQuery contructor
         */
        // This is a hack to get the empty result set to show...
        // Otherwise dbquery sees no docidOverrides and does a full % percent
        // query
        if (docids.size() == 0)
            docids.add("");
        
        DBQuery queryobj = new DBQuery(docids);
        queryobj.findDocuments(response, out, params, username, groupnames, sess_id);
        
    }
    
    // LOGIN & LOGOUT SECTION
    /**
     * Handle the login request. Create a new session object. Do user
     * authentication through the session.
     * @throws IOException 
     */
    public void handleLoginAction(Writer out, Hashtable<String, String[]> params,
            HttpServletRequest request, HttpServletResponse response) throws IOException {
        Logger logMetacat = Logger.getLogger(MetacatHandler.class);
        AuthSession sess = null;
        
        if(params.get("username") == null){
            response.setContentType("text/xml");
            out.write("<?xml version=\"1.0\"?>");
            out.write("<error>");
            out.write("Username not specified");
            out.write("</error>");
            return;
        }
        
        // }
        
        if(params.get("password") == null){
            response.setContentType("text/xml");
            out.write("<?xml version=\"1.0\"?>");
            out.write("<error>");
            out.write("Password not specified");
            out.write("</error>");
            return;
        }
        
        String un = (params.get("username"))[0];
        logMetacat.info("MetacatHandler.handleLoginAction - user " + un + " is trying to login");
        String pw = (params.get("password"))[0];
        
        String qformat = "xml";
        if (params.get("qformat") != null) {
            qformat = (params.get("qformat"))[0];
        }
        
        try {
            sess = new AuthSession();
        } catch (Exception e) {
            String errorMsg = "MetacatServlet.handleLoginAction - Problem in MetacatServlet.handleLoginAction() authenicating session: "
                + e.getMessage();
            logMetacat.error(errorMsg);
            out.write(errorMsg);
            e.printStackTrace(System.out);
            return;
        }
        boolean isValid = sess.authenticate(request, un, pw);
        
        //if it is authernticate is true, store the session
        if (isValid) {
            HttpSession session = sess.getSessions();
            String id = session.getId();
            
            logMetacat.debug("MetacatHandler.handleLoginAction - Store session id " + id
                    + " which has username" + session.getAttribute("username")
                    + " into hash in login method");
            try {
                System.out.println("registering session with id " + id);
                System.out.println("username: " + (String) session.getAttribute("username"));
                SessionService.getInstance().registerSession(id, 
                        (String) session.getAttribute("username"), 
                        (String[]) session.getAttribute("groupnames"), 
                        (String) session.getAttribute("password"), 
                        (String) session.getAttribute("name"));
                
                    
            } catch (ServiceException se) {
                String errorMsg = "MetacatServlet.handleLoginAction - service problem registering session: "
                        + se.getMessage();
                logMetacat.error("MetacatHandler.handleLoginAction - " + errorMsg);
                out.write(errorMsg);
                se.printStackTrace(System.out);
                return;
            }           
        }
                
        // format and transform the output
        if (qformat.equals("xml")) {
            response.setContentType("text/xml");
            out.write(sess.getMessage());
        } else {
            try {
                DBTransform trans = new DBTransform();
                response.setContentType("text/html");
                trans.transformXMLDocument(sess.getMessage(),
                        "-//NCEAS//login//EN", "-//W3C//HTML//EN", qformat,
                        out, null, null);
            } catch (Exception e) {               
                logMetacat.error("MetacatHandler.handleLoginAction - General error"
                        + e.getMessage());
                e.printStackTrace(System.out);
            }
        }
    }
    
    /**
     * Handle the logout request. Close the connection.
     * @throws IOException 
     */
    public void handleLogoutAction(Writer out, Hashtable<String, String[]> params,
            HttpServletRequest request, HttpServletResponse response) throws IOException {
        Logger logMetacat = Logger.getLogger(MetacatHandler.class);
        String qformat = "xml";
        if(params.get("qformat") != null){
            qformat = params.get("qformat")[0];
        }
        
        // close the connection
        HttpSession sess = request.getSession(false);
        logMetacat.info("MetacatHandler.handleLogoutAction - After get session in logout request");
        if (sess != null) {
            logMetacat.info("MetacatHandler.handleLogoutAction - The session id " + sess.getId()
            + " will be invalidate in logout action");
            logMetacat.info("MetacatHandler.handleLogoutAction - The session contains user "
                    + sess.getAttribute("username")
                    + " will be invalidate in logout action");
            sess.invalidate();
            SessionService.getInstance().unRegisterSession(sess.getId());
        }
        
        // produce output
        StringBuffer output = new StringBuffer();
        output.append("<?xml version=\"1.0\"?>");
        output.append("<logout>");
        output.append("User logged out");
        output.append("</logout>");
        
        //format and transform the output
        if (qformat.equals("xml")) {
            response.setContentType("text/xml");
            out.write(output.toString());
        } else {
            try {
                DBTransform trans = new DBTransform();
                response.setContentType("text/html");
                trans.transformXMLDocument(output.toString(),
                        "-//NCEAS//login//EN", "-//W3C//HTML//EN", qformat,
                        out, null, null);
            } catch (Exception e) {
                logMetacat.error(
                        "MetacatHandler.handleLogoutAction - General error: "
                        + e.getMessage());
                e.printStackTrace(System.out);
            }
        }
    }
    
    // END OF LOGIN & LOGOUT SECTION
    
    // SQUERY & QUERY SECTION
    /**
     * Retreive the squery xml, execute it and display it
     *
     * @param out the output stream to the client
     * @param params the Hashtable of parameters that should be included in the
     *            squery.
     * @param response the response object linked to the client
     * @param conn the database connection
     */
    protected void handleSQuery(Writer out, Hashtable<String, String[]> params,
            HttpServletResponse response, String user, String[] groups,
            String sessionid) throws PropertyNotFoundException {
        Logger logMetacat = Logger.getLogger(MetacatHandler.class);
        long squeryWarnLimit = Long.parseLong(PropertyService.getProperty("database.squeryTimeWarnLimit"));
        
        long startTime = System.currentTimeMillis();
        DBQuery queryobj = new DBQuery();
        queryobj.findDocuments(response, out, params, user, groups, sessionid);
        long outPutTime = System.currentTimeMillis();
        long runTime = outPutTime - startTime;

        if (runTime > squeryWarnLimit) {
            logMetacat.warn("MetacatHandler.handleSQuery - Long running squery.  Total time: " + runTime + 
                    " ms, squery: " + ((String[])params.get("query"))[0]);
        }
        logMetacat.debug("MetacatHandler.handleSQuery - squery: " + ((String[])params.get("query"))[0] + 
                " ran in " + runTime + " ms");
    }
    
    /**
     * Create the xml query, execute it and display the results.
     *
     * @param out the output stream to the client
     * @param params the Hashtable of parameters that should be included in the
     *            squery.
     * @param response the response object linked to the client
     * @throws IOException 
     * @throws UnsupportedEncodingException 
     */
    protected void handleQuery(Writer out, Hashtable<String, String[]> params,
            HttpServletResponse response, String user, String[] groups,
            String sessionid) throws PropertyNotFoundException, UnsupportedEncodingException, IOException {
        Logger logMetacat = Logger.getLogger(MetacatHandler.class);
        long queryWarnLimit = Long.parseLong(PropertyService.getProperty("database.queryTimeWarnLimit"));
        
        //create the query and run it
        String xmlquery = DBQuery.createSQuery(params);
        String[] queryArray = new String[1];
        queryArray[0] = xmlquery;
        params.put("query", queryArray);
        long startTime = System.currentTimeMillis();
        DBQuery queryobj = new DBQuery();
        queryobj.findDocuments(response, out, params, user, groups, sessionid);
        long outPutTime = System.currentTimeMillis();
        long runTime = outPutTime -startTime;

        if (runTime > queryWarnLimit) {
            logMetacat.warn("MetacatHandler.handleQuery - Long running squery.  Total time: " + runTime + 
                    " ms, squery: " + ((String[])params.get("query"))[0]);
        }
        logMetacat.debug("MetacatHandler.handleQuery - query: " + ((String[])params.get("query"))[0] + 
                " ran in " + runTime + " ms");
    }
    
    // END OF SQUERY & QUERY SECTION
    
    //Export section
    /**
     * Handle the "export" request of data package from Metacat in zip format
     *
     * @param params the Hashtable of HTTP request parameters
     * @param response the HTTP response object linked to the client
     * @param user the username sent the request
     * @param groups the user's groupnames
     */
    protected void handleExportAction(Hashtable<String, String[]> params,
            HttpServletResponse response,
            String user, String[] groups, String passWord) {
        Logger logMetacat = Logger.getLogger(MetacatHandler.class);
        // Output stream
        ServletOutputStream out = null;
        // Zip output stream
        ZipOutputStream zOut = null;
        DBQuery queryObj = null;
        
        String[] docs = new String[10];
        String docId = "";
        
        try {
            // read the params
            if (params.containsKey("docid")) {
                docs = params.get("docid");
            }
            // Create a DBuery to handle export
            queryObj = new DBQuery();
            String qformat = null;
            if (params.containsKey("qformat")) {
                qformat = params.get("qformat")[0];
                queryObj.setQformat(qformat);
            }
            // Get the docid
            docId = docs[0];
            // Make sure the client specify docid
            if (docId == null || docId.equals("")) {
                response.setContentType("text/xml"); //MIME type
                // Get a printwriter
                PrintWriter pw = response.getWriter();
                // Send back message
                pw.println("<?xml version=\"1.0\"?>");
                pw.println("<error>");
                pw.println("You didn't specify requested docid");
                pw.println("</error>");
                // Close printwriter
                pw.close();
                return;
            }
            // Get output stream
            response.setContentType("application/zip"); //MIME type
            response.setHeader("Content-Disposition",
                    "attachment; filename="
                    + docId + ".zip"); // Set the name of the zip file
            out = response.getOutputStream();            
            zOut = new ZipOutputStream(out);
            zOut = queryObj
                    .getZippedPackage(docId, out, user, groups, passWord);
            zOut.finish(); //terminate the zip file
            zOut.close(); //close the zip stream
            
        } catch (Exception e) {
            try {
                response.setContentType("text/xml"); //MIME type
                // Send error message back
                if (out != null) {
                    PrintWriter pw = new PrintWriter(out);
                    pw.println("<?xml version=\"1.0\"?>");
                    pw.println("<error>");
                    pw.println(e.getMessage());
                    pw.println("</error>");
                    // Close printwriter
                    pw.close();
                    // Close output stream
                    out.close();
                }
                // Close zip output stream
                if (zOut != null) {
                    zOut.close();
                }
            } catch (IOException ioe) {
                logMetacat.error("MetacatHandler.handleExportAction - Problem with the servlet output: "
                        + ioe.getMessage());
                e.printStackTrace(System.out);
            }
            
            logMetacat.error("MetacatHandler.handleExportAction - General error: "
                    + e.getMessage());
            e.printStackTrace(System.out);
            
        }
        
    }
    
    /**
     * In eml2 document, the xml can have inline data and data was stripped off
     * and store in file system. This action can be used to read inline data
     * only
     *
     * @param params the Hashtable of HTTP request parameters
     * @param response the HTTP response object linked to the client
     * @param user the username sent the request
     * @param groups the user's groupnames
     */
    protected void handleReadInlineDataAction(Hashtable<String, String[]> params,
            HttpServletRequest request, HttpServletResponse response,
            String user, String passWord, String[] groups) {
        Logger logMetacat = Logger.getLogger(MetacatHandler.class);
        String[] docs = new String[10];
        String inlineDataId = null;
        String docId = "";
        ServletOutputStream out = null;
        
        try {
            // read the params
            if (params.containsKey("inlinedataid")) {
                docs = params.get("inlinedataid");
            }
            // Get the docid
            inlineDataId = docs[0];
            // Make sure the client specify docid
            if (inlineDataId == null || inlineDataId.equals("")) {
                throw new Exception("You didn't specify requested inlinedataid"); }
            
            // check for permission, use full docid with revision
            docId = DocumentUtil.getDocIdFromInlineDataID(inlineDataId);

            PermissionController controller = new PermissionController(docId);
            // check top level read permission
            if (!controller.hasPermission(user, groups,
                    AccessControlInterface.READSTRING)) {
                throw new Exception("User " + user
                        + " doesn't have permission " + " to read document "
                        + docId);
            } else {
                //check data access level
                try {
                    Hashtable<String,String> unReadableInlineDataList =
                            PermissionController.getUnReadableInlineDataIdList(docId, user, groups);
                    if (unReadableInlineDataList.containsValue(inlineDataId)) {
                        throw new Exception("User " + user
                                + " doesn't have permission " + " to read inlinedata "
                                + inlineDataId);
                        
                    }//if
                }//try
                catch (Exception e) {
                    throw e;
                }//catch
            }//else
            
            // Get output stream
            out = response.getOutputStream();
            // read the inline data from the file
            String inlinePath = PropertyService.getProperty("application.inlinedatafilepath");
            File lineData = new File(inlinePath, inlineDataId);
            FileInputStream input = new FileInputStream(lineData);
            byte[] buffer = new byte[4 * 1024];
            int bytes = input.read(buffer);
            while (bytes != -1) {
                out.write(buffer, 0, bytes);
                bytes = input.read(buffer);
            }
            out.close();
            
            EventLog.getInstance().log(request.getRemoteAddr(), request.getHeader("User-Agent"), user,
                    inlineDataId, "readinlinedata");
        } catch (Exception e) {
            try {
                PrintWriter pw = null;
                // Send error message back
                if (out != null) {
                    pw = new PrintWriter(out);
                } else {
                    pw = response.getWriter();
                }
                pw.println("<?xml version=\"1.0\"?>");
                pw.println("<error>");
                pw.println(e.getMessage());
                pw.println("</error>");
                // Close printwriter
                pw.close();
                // Close output stream if out is not null
                if (out != null) {
                    out.close();
                }
            } catch (IOException ioe) {
                logMetacat.error("MetacatHandler.handleReadInlineDataAction - Problem with the servlet output: "
                        + ioe.getMessage());
                e.printStackTrace(System.out);
            }
            logMetacat.error("MetacatHandler.handleReadInlineDataAction - General error: "
                    + e.getMessage());
            e.printStackTrace(System.out);
        }
    }
    
    /**
     * Handle the "read" request of metadata/data files from Metacat or any
     * files from Internet; transformed metadata XML document into HTML
     * presentation if requested; zip files when more than one were requested.
     *
     * @param params the Hashtable of HTTP request parameters
     * @param request the HTTP request object linked to the client
     * @param response the HTTP response object linked to the client
     * @param user the username sent the request
     * @param groups the user's groupnames
     */
    public void handleReadAction(Hashtable<String, String[]> params, HttpServletRequest request,
            HttpServletResponse response, String user, String passWord,
            String[] groups) {
        Logger logMetacat = Logger.getLogger(MetacatHandler.class);
        ServletOutputStream out = null;
        ZipOutputStream zout = null;
        PrintWriter pw = null;
        boolean zip = false;
        boolean withInlineData = true;
        
        try {
            String[] docs = new String[0];
            String docid = "";
            String qformat = "";
            
            // read the params
            if (params.containsKey("docid")) {
                docs = params.get("docid");
            }
            if (params.containsKey("qformat")) {
                qformat = params.get("qformat")[0];
            }
            // the param for only metadata (eml)
            // we don't support read a eml document without inline data now.
            /*if (params.containsKey("inlinedata")) {
             
                String inlineData = ((String[]) params.get("inlinedata"))[0];
                if (inlineData.equalsIgnoreCase("false")) {
                    withInlineData = false;
                }
            }*/
            // handle special case where the PID was given
            if (params.containsKey("pid")) {
                docs = params.get("pid");
            	for (int i = 0; i < docs.length; i++) {
            		String pid = docs[i];
            		// look up the pid if we have it
            		String localId = IdentifierManager.getInstance().getLocalId(pid);
            		docs[i] = localId;
            	}
            	// put docid in parms for downstream methods to use
            	params.put("docid", docs);
            }
            
            if ((docs.length > 1) || qformat.equals("zip")) {
                zip = true;
                out = response.getOutputStream();
                response.setContentType("application/zip"); //MIME type
                zout = new ZipOutputStream(out);
            }
            // go through the list of docs to read
            for (int i = 0; i < docs.length; i++) {
                String providedFileName = null;
                if (params.containsKey(docs[i])) {
                    providedFileName = params.get(docs[i])[0];
                }
                try {
                    
                    URL murl = new URL(docs[i]);
                    Hashtable<String,String> murlQueryStr = MetacatUtil.parseQuery(
                            murl.getQuery());
                    // case docid="http://.../?docid=aaa"
                    // or docid="metacat://.../?docid=bbb"
                    if (murlQueryStr.containsKey("docid")) {
                        // get only docid, eliminate the rest
                        docid = murlQueryStr.get("docid");
                        if (zip) {
                            addDocToZip(request, docid, providedFileName, zout, user, groups);
                        } else {
                            readFromMetacat(request.getRemoteAddr(), request.getHeader("User-Agent"), response, response.getOutputStream(), docid, qformat,
                                    user, groups, withInlineData, params);
                        }
                        
                        // case docid="http://.../filename"
                    } else {
                        docid = docs[i];
                        if (zip) {
                            addDocToZip(request, docid, providedFileName, zout, user, groups);
                        } else {
                            readFromURLConnection(response, docid);
                        }
                    }
                    
                } catch (MalformedURLException mue) {
                    docid = docs[i];
                    if (zip) {
                        addDocToZip(request, docid, providedFileName, zout, user, groups);
                    } else {
                    	if (out == null) {
                    		out = response.getOutputStream();
                    	}
                        readFromMetacat(request.getRemoteAddr(), request.getHeader("User-Agent"), response, out, docid, qformat,
                                user, groups, withInlineData, params);
                    }
                }
            }
            
            if (zip) {
                zout.finish(); //terminate the zip file
                zout.close(); //close the zip stream
            }
            
        } catch (McdbDocNotFoundException notFoundE) {
            // To handle doc not found exception
            // the docid which didn't be found
            String notFoundDocId = notFoundE.getUnfoundDocId();
            String notFoundRevision = notFoundE.getUnfoundRevision();
            logMetacat.warn("MetacatHandler.handleReadAction - Missed id: " + notFoundDocId);
            logMetacat.warn("MetacatHandler.handleReadAction - Missed rev: " + notFoundRevision);
            try {
                // read docid from remote server
                readFromRemoteMetaCat(response, notFoundDocId,
                        notFoundRevision, user, passWord, out, zip, zout);
                // Close zout outputstream
                if (zout != null) {
                    zout.close();
                }
                // close output stream
                if (out != null) {
                    out.close();
                }
                
            } catch (Exception exc) {
                logMetacat.error("MetacatHandler.handleReadAction - General error: "
                        + exc.getMessage());
                exc.printStackTrace(System.out);
                try {
                    if (out != null) {
                        response.setContentType("text/xml");
                        // Send back error message by printWriter
                        pw = new PrintWriter(out);
                        pw.println("<?xml version=\"1.0\"?>");
                        pw.println("<error>");
                        pw.println(notFoundE.getMessage());
                        pw.println("</error>");
                        pw.close();
                        out.close();
                        
                    } else {
                        response.setContentType("text/xml"); //MIME type
                        // Send back error message if out = null
                        if (pw == null) {
                            // If pw is null, open the respnose
                            pw = response.getWriter();
                        }
                        pw.println("<?xml version=\"1.0\"?>");
                        pw.println("<error>");
                        pw.println(notFoundE.getMessage());
                        pw.println("</error>");
                        pw.close();
                    }
                    // close zout
                    if (zout != null) {
                        zout.close();
                    }
                } catch (IOException ie) {
                    logMetacat.error("MetacatHandler.handleReadAction - Problem with the servlet output: "
                            + ie.getMessage());
                    ie.printStackTrace(System.out);
                }
            }
        } catch (Exception e) {
            try {
                
                if (out != null) {
                    response.setContentType("text/xml"); //MIME type
                    pw = new PrintWriter(out);
                    pw.println("<?xml version=\"1.0\"?>");
                    pw.println("<error>");
                    pw.println(e.getMessage());
                    pw.println("</error>");
                    pw.close();
                    out.close();
                } else {
                    response.setContentType("text/xml"); //MIME type
                    // Send back error message if out = null
                    if (pw == null) {
                        pw = response.getWriter();
                    }
                    pw.println("<?xml version=\"1.0\"?>");
                    pw.println("<error>");
                    pw.println(e.getMessage());
                    pw.println("</error>");
                    pw.close();
                    
                }
                // Close zip output stream
                if (zout != null) {
                    zout.close();
                }
                
            } catch (Exception e2) {
                logMetacat.error("MetacatHandler.handleReadAction - " + 
                		         "Problem with the servlet output: "+ 
                		         e2.getMessage());
                e2.printStackTrace(System.out);
                
            }
            
            logMetacat.error("MetacatHandler.handleReadAction - General error: "
                    + e.getMessage());
            e.printStackTrace(System.out);
        }
    }
    
    /**
     * 
     * @return
     */
    public MetacatResultSet query(String metacatUrl, Hashtable<String, String[]>params, 
            String username, String[] groups, String sessionid)
      throws Exception
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
     // use UTF-8 encoding for DB query
        Writer out = new OutputStreamWriter(baos, MetaCatServlet.DEFAULT_ENCODING);
        handleQuery(out, params, null, username, groups, sessionid);
        out.flush();
        baos.flush();
        //baos.close(); 
        //System.out.println("result from query: " + baos.toString());
        MetacatResultSet rs = new MetacatResultSet(baos.toString(MetaCatServlet.DEFAULT_ENCODING));
        return rs;
    }
    
    /**
     * set the access permissions on the document specified
     */
    public void setAccess(String metacatUrl, String username, String docid, String principal, 
            String permission, String permissionType, String permissionOrder)
      throws Exception
    {
        Hashtable<String,String[]> params = new Hashtable<String,String[]>();
        params.put("principal", new String[] {principal});
        params.put("permission", new String[] {permission});
        params.put("permType", new String[] {permissionType});
        params.put("permOrder", new String[] {permissionOrder});
        params.put("docid", new String[]{docid});
        
        //System.out.println("permission in MetacatHandler.setAccess: " + 
        //                   params.get("permission")[0]);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintWriter out = new PrintWriter(baos);
        handleSetAccessAction(out, params, username, null, null);
        String resp = baos.toString();
        //System.out.println("response from MetacatHandler.setAccess: " + resp);
    }
    
    /**
     * Read a document from metacat and return the InputStream.  The XML or
     * data document should be on disk, but if not, read from the metacat database.
     * 
     * @param  docid - the metacat docid to read
     * @param  username - the DN of the principal attempting the read
     * @param  groups - the list of groups the DN belongs to as a String array
     * @return objectStream - the document as an InputStream
     * @throws InsufficientKarmaException
     * @throws ParseLSIDException
     * @throws PropertyNotFoundException
     * @throws McdbException
     * @throws SQLException
     * @throws ClassNotFoundException
     * @throws IOException
     */
	public static InputStream read(String docid) throws ParseLSIDException,
			PropertyNotFoundException, McdbException, SQLException,
			ClassNotFoundException, IOException {
		logMetacat.debug("MetacatHandler.read() called.");

		InputStream inputStream = null;

		// be sure we have a local ID from an LSID
		if (docid.startsWith("urn:")) {
			try {
				docid = LSIDUtil.getDocId(docid, true);
			} catch (ParseLSIDException ple) {
				logMetacat.debug("There was a problem parsing the LSID. The "
						+ "error message was: " + ple.getMessage());
				throw ple;
			}
		}

		// accomodate old clients that send docids without revision numbers
		docid = DocumentUtil.appendRev(docid);
		DocumentImpl doc = new DocumentImpl(docid, false);

		// deal with data or metadata cases
		if (doc.getRootNodeID() == 0) {

			// this is a data file
			// get the path to the file to read
			try {
				String filepath = PropertyService.getProperty("application.datafilepath");
				// ensure it is a directory path
				if (!(filepath.endsWith("/"))) {
					filepath += "/";
				}
				String filename = filepath + docid;
				inputStream = readFromFilesystem(filename);
			} catch (PropertyNotFoundException pnf) {
				logMetacat.debug("There was a problem finding the "
						+ "application.datafilepath property. The error "
						+ "message was: " + pnf.getMessage());
				throw pnf;
			} // end try()

		} else {
			// this is an metadata document
			// Get the xml (will try disk then DB)
			try {
				// force the InputStream to be returned
				OutputStream nout = null;
				inputStream = doc.toXml(nout, null, null, true);
			} catch (McdbException e) {
				// report the error
				logMetacat.error(
						"MetacatHandler.readFromMetacat() "
								+ "- could not read document " + docid + ": "
								+ e.getMessage(), e);
			}

		}

		return inputStream;
	}
    
    /**
     * Read a file from Metacat's configured file system data directory.
     *
     * @param filename  The full path file name of the file to read
     * 
     * @return fileInputStream  The file to read as a FileInputStream
     */
    private static FileInputStream readFromFilesystem(String filename) 
      throws FileNotFoundException {
        
        logMetacat.debug("MetacatHandler.readFromFilesystem() called.");
        
        FileInputStream fileInputStream = null;
        
        try {
          fileInputStream = new FileInputStream(filename);

        } catch ( FileNotFoundException fnfe ) {
          logMetacat.debug("There was an error reading the file " +
                           filename + ". The error was: "         +
                           fnfe.getMessage());
          throw fnfe;
           
        }
        
      return fileInputStream;  
    }
    
    /*
     * Delete a document in metacat based on the docid.
     *
     * @param out      - the print writer used to send output
     * @param response - the HTTP servlet response to be returned
     * @param docid    - the internal docid as a String
     * @param user     - the username of the principal doing the delete
     * @param groups   - the groups list of the principal doing the delete
     *
     * @throws AccessionNumberException
     * @throws McdbDocNotFoundException
     * @throws InsufficientKarmaException
     * @throws SQLException
     * @throws Exception
     */
    private void deleteFromMetacat(PrintWriter out, HttpServletRequest request,
      HttpServletResponse response, String docid, String user, String[] groups)
      throws McdbDocNotFoundException {
      
      // Delete a document from the database based on the docid
      try {
          
        DocumentImpl.delete(docid, user, groups, null, false); // null: don't notify server
        EventLog.getInstance().log(request.getRemoteAddr(), request.getHeader("User-Agent"),
                user, docid, "delete");
        response.setContentType("text/xml");
        out.println(this.PROLOG);
        out.println(this.SUCCESS);
        out.println("Document deleted.");
        out.println(this.SUCCESSCLOSE);
        logMetacat.info("MetacatHandler.handleDeleteAction - " +
          "Document deleted.");
        
        try {
          // Delete from spatial cache if runningSpatialOption
          if ( PropertyService.getProperty("spatial.runSpatialOption").equals("true") ) {
            SpatialHarvester sh = new SpatialHarvester();
            sh.addToDeleteQue( DocumentUtil.getSmartDocId( docid ) );
            sh.destroy();
          }
          
        } catch ( PropertyNotFoundException pnfe ) {
          logMetacat.error("MetacatHandler.deleteFromMetacat() - "    +
            "Couldn't find spatial.runSpatialOption property during " +
            "document deletion.");
            
        }
          
      } catch (AccessionNumberException ane) {
        response.setContentType("text/xml");
        out.println(this.PROLOG);
        out.println(this.ERROR);
        //out.println("Error deleting document!!!");
        out.println(ane.getMessage());
        out.println(this.ERRORCLOSE);
        logMetacat.error("MetacatHandler.deleteFromMetacat() - " +
          "Document could not be deleted: "
                + ane.getMessage());
        ane.printStackTrace(System.out);
        
      } catch ( SQLException sqle ) {
        response.setContentType("text/xml");
        out.println(this.PROLOG);
        out.println(this.ERROR);
        //out.println("Error deleting document!!!");
        out.println(sqle.getMessage());
        out.println(this.ERRORCLOSE);
        logMetacat.error("MetacatHandler.deleteFromMetacat() - " +
          "Document could not be deleted: "
                + sqle.getMessage());
        sqle.printStackTrace(System.out);
        
      } catch ( McdbDocNotFoundException dnfe ) {
        throw dnfe;
        
      } catch ( InsufficientKarmaException ike ) {
        response.setContentType("text/xml");
        out.println(this.PROLOG);
        out.println(this.ERROR);
        //out.println("Error deleting document!!!");
        out.println(ike.getMessage());
        out.println(this.ERRORCLOSE);
        logMetacat.error("MetacatHandler.deleteFromMetacat() - " +
          "Document could not be deleted: "
                + ike.getMessage());
        ike.printStackTrace(System.out);
        
      } catch ( Exception e ) {
        response.setContentType("text/xml");
        out.println(this.PROLOG);
        out.println(this.ERROR);
        //out.println("Error deleting document!!!");
        out.println(e.getMessage());
        out.println(this.ERRORCLOSE);
        logMetacat.error("MetacatHandler.deleteFromMetacat() - " +
          "Document could not be deleted: "
                + e.getMessage());
        e.printStackTrace(System.out);
        
      }
    }
    
    /** read metadata or data from Metacat
     * @param userAgent 
     * @throws PropertyNotFoundException 
     * @throws ParseLSIDException 
     * @throws InsufficientKarmaException 
     */
    public void readFromMetacat(String ipAddress, String userAgent,
            HttpServletResponse response, OutputStream out, String docid, String qformat,
            String user, String[] groups, boolean withInlineData, 
            Hashtable<String, String[]> params) throws ClassNotFoundException, 
            IOException, SQLException, McdbException, PropertyNotFoundException, 
            ParseLSIDException, InsufficientKarmaException {
        
        Logger logMetacat = Logger.getLogger(MetacatHandler.class);
        try {
            
            if (docid.startsWith("urn:")) {
                docid = LSIDUtil.getDocId(docid, true);                 
            }
            
            // here is hack for handle docid=john.10(in order to tell mike.jim.10.1
            // mike.jim.10, we require to provide entire docid with rev). But
            // some old client they only provide docid without rev, so we need
            // to handle this suituation. First we will check how many
            // seperator here, if only one, we will append the rev in xml_documents
            // to the id.
            docid = DocumentUtil.appendRev(docid);
            
            DocumentImpl doc = new DocumentImpl(docid, false);
            
            //check the permission for read
            if (!DocumentImpl.hasReadPermission(user, groups, docid)) {
                
                InsufficientKarmaException e = 
                	new InsufficientKarmaException("User " + user
                        + " does not have permission"
                        + " to read the document with the docid " + docid);
                throw e;
            }
            
            if (doc.getRootNodeID() == 0) {
                // this is data file, so find the path on disk for the file
                String filepath = PropertyService.getProperty("application.datafilepath");
                if (!filepath.endsWith("/")) {
                    filepath += "/";
                }
                String filename = filepath + docid;
                FileInputStream fin = null;
                fin = new FileInputStream(filename);
                
                if (response != null) {
                    // MIME type
                    //String contentType = servletContext.getMimeType(filename);
                    String contentType = (new MimetypesFileTypeMap()).getContentType(filename);
                    if (contentType == null) {
                        ContentTypeProvider provider = new ContentTypeProvider(
                                docid);
                        contentType = provider.getContentType();
                        logMetacat.info("MetacatHandler.readFromMetacat - Final contenttype is: "
                                + contentType);
                    }
                    response.setContentType(contentType);

                    // Set the output filename on the response
                    String outputname = generateOutputName(docid, params, doc);                    
                    response.setHeader("Content-Disposition",
                            "attachment; filename=\"" + outputname + "\"");
                }
                
                // Write the data to the output stream
                try {
                    byte[] buf = new byte[4 * 1024]; // 4K buffer
                    int b = fin.read(buf);
                    while (b != -1) {
                        out.write(buf, 0, b);
                        b = fin.read(buf);
                    }
                    fin.close();
                } finally {
                    IOUtils.closeQuietly(fin);
                }
                
            } else {
                // this is metadata doc
                if (qformat.equals("xml") || qformat.equals("")) {
                    // if equals "", that means no qformat is specified. hence
                    // by default the document should be returned in xml format
                    // set content type first
                    
                    if (response != null) {
                        response.setContentType("text/xml"); //MIME type
                        response.setHeader("Content-Disposition",
                                "attachment; filename=" + docid + ".xml");
                    }
                    
                    // Try to get the metadata file from disk. If it isn't
                    // found, create it from the db and write it to disk then.
                    try {
                        doc.toXml(out, user, groups, withInlineData);               
                    } catch (McdbException e) {
                        // any exceptions in reading the xml from disc, and we go back to the
                        // old way of creating the xml directly.
                        logMetacat.error("MetacatHandler.readFromMetacat - "  + 
                        		         "could not read from document file " + 
                        		         docid + 
                        		         ": " + 
                        		         e.getMessage());
                        e.printStackTrace(System.out);
                        doc.toXmlFromDb(out, user, groups, withInlineData);
                    }
                } else {
                    // TODO MCD, this should read from disk as well?
                    //*** This is a metadata doc, to be returned in a skin/custom format.
                    //*** Add param to indicate if public has read access or not.
                    logMetacat.debug("User: \n" + user);
                    if (!user.equals("public")) {
                        if (DocumentImpl.hasReadPermission("public", null, docid))
                            params.put("publicRead", new String[] {"true"});
                        else
                            params.put("publicRead", new String[] {"false"});
                    }
                    
                    if(doc.getDoctype() != null && doc.getDoctype().equals(FGDCDOCTYPE)) {
                      //for fgdc doctype, we need to pass parameter enableFGDCediting
                      PermissionController controller = new PermissionController(docid);
                      if(controller.hasPermission(user, groups, AccessControlInterface.WRITESTRING)) {
                        params.put("enableFGDCediting", new String[] {"true"});
                      } else {
                        params.put("enableFGDCediting", new String[] {"false"});
                      }
                    }
                    if (response != null) {
                        response.setContentType("text/html"); //MIME type
                    }
                    
                    // detect actual encoding
                    String docString = doc.toString(user, groups, withInlineData);
                    XmlStreamReader xsr = 
                    	new XmlStreamReader(new ByteArrayInputStream(doc.getBytes()));
        			String encoding = xsr.getEncoding();
                    Writer w = null;
        			if (encoding != null) {
        				w = new OutputStreamWriter(out, encoding);
        			} else {
                        w = new OutputStreamWriter(out);
        			}

                    // Look up the document type
                    String doctype = doc.getDoctype();
                    // Transform the document to the new doctype
                    DBTransform dbt = new DBTransform();
                    dbt.transformXMLDocument(
                    		docString, 
                    		doctype, "-//W3C//HTML//EN",
                            qformat, 
                            w, 
                            params, 
                            null);
                }
                
            }
            EventLog.getInstance().log(ipAddress, userAgent, user, docid, "read");
        } catch (PropertyNotFoundException except) {
            throw except;
        }
    }

    /**
     * Create a filename to be used for naming a downloaded document
     * @param docid the identifier of the document to be named
     * @param params the parameters of the request
     * @param doc the DocumentImpl of the document to be named
     * @return String containing a name for the download
     */
    private String generateOutputName(String docid,
            Hashtable<String, String[]> params, DocumentImpl doc) {
        String outputname = null;
        // check for the existence of a metadatadocid parameter,
        // if this is sent, then send a filename which contains both
        // the metadata docid and the data docid, so the link with
        // metadata is explicitly encoded in the filename.
        String metadatadocid = null;
        Vector<String> nameparts = new Vector<String>();

        if(params.containsKey("metadatadocid")) {
            metadatadocid = params.get("metadatadocid")[0];
        }
        if (metadatadocid != null && !metadatadocid.equals("")) {
            nameparts.add(metadatadocid);
        }
        // we'll always have the docid, include it in the name
        String doctype = doc.getDoctype();
        // TODO: fix this to lookup the associated FGDC metadata document,
        // and grab the doctype tag for it.  These should be set to something 
        // consistent, not 'metadata' as it stands...
        //if (!doctype.equals("metadata")) {
        //    nameparts.add(docid);
        //} 
        nameparts.add(docid);
        // Set the name of the data file to the entity name plus docid,
        // or if that is unavailable, use the docid alone
        String docname = doc.getDocname();
        if (docname != null && !docname.equals("")) {
            nameparts.add(docname); 
        }
        // combine the name elements with a dash, using a 'join' equivalent
        String delimiter = "-";
        Iterator<String> iter = nameparts.iterator();
        StringBuffer buffer = new StringBuffer(iter.next());
        while (iter.hasNext()) buffer.append(delimiter).append(iter.next());
        outputname = buffer.toString();
        return outputname;
    }
    
    /**
     * read data from URLConnection
     */
    private void readFromURLConnection(HttpServletResponse response,
            String docid) throws IOException, MalformedURLException {
        ServletOutputStream out = response.getOutputStream();
        //String contentType = servletContext.getMimeType(docid); //MIME type
        String contentType = (new MimetypesFileTypeMap()).getContentType(docid);
        if (contentType == null) {
            if (docid.endsWith(".xml")) {
                contentType = "text/xml";
            } else if (docid.endsWith(".css")) {
                contentType = "text/css";
            } else if (docid.endsWith(".dtd")) {
                contentType = "text/plain";
            } else if (docid.endsWith(".xsd")) {
                contentType = "text/xml";
            } else if (docid.endsWith("/")) {
                contentType = "text/html";
            } else {
                File f = new File(docid);
                if (f.isDirectory()) {
                    contentType = "text/html";
                } else {
                    contentType = "application/octet-stream";
                }
            }
        }
        response.setContentType(contentType);
        // if we decide to use "application/octet-stream" for all data returns
        // response.setContentType("application/octet-stream");
        
        // this is http url
        URL url = new URL(docid);
        BufferedInputStream bis = null;
        try {
            bis = new BufferedInputStream(url.openStream());
            byte[] buf = new byte[4 * 1024]; // 4K buffer
            int b = bis.read(buf);
            while (b != -1) {
                out.write(buf, 0, b);
                b = bis.read(buf);
            }
        } finally {
            if (bis != null) bis.close();
        }
        
    }
    
    /**
     * read file/doc and write to ZipOutputStream
     *
     * @param docid
     * @param zout
     * @param user
     * @param groups
     * @throws ClassNotFoundException
     * @throws IOException
     * @throws SQLException
     * @throws McdbException
     * @throws Exception
     */
    private void addDocToZip(HttpServletRequest request, String docid, String providedFileName,
            ZipOutputStream zout, String user, String[] groups) throws
            ClassNotFoundException, IOException, SQLException, McdbException,
            Exception {
        byte[] bytestring = null;
        ZipEntry zentry = null;
        
        try {
            URL url = new URL(docid);
            
            // this http url; read from URLConnection; add to zip
            //use provided file name if we have one
            if (providedFileName != null && providedFileName.length() > 1) {
                zentry = new ZipEntry(providedFileName);
            }
            else {
                zentry = new ZipEntry(docid);
            }
            zout.putNextEntry(zentry);
            BufferedInputStream bis = null;
            try {
                bis = new BufferedInputStream(url.openStream());
                byte[] buf = new byte[4 * 1024]; // 4K buffer
                int b = bis.read(buf);
                while (b != -1) {
                    zout.write(buf, 0, b);
                    b = bis.read(buf);
                }
            } finally {
                if (bis != null) bis.close();
            }
            zout.closeEntry();
            
        } catch (MalformedURLException mue) {
            
            // this is metacat doc (data file or metadata doc)
            try {
                DocumentImpl doc = new DocumentImpl(docid, false);
                
                //check the permission for read
                if (!DocumentImpl.hasReadPermission(user, groups, docid)) {
                    Exception e = new Exception("User " + user
                            + " does not have "
                            + "permission to read the document with the docid "
                            + docid);
                    throw e;
                }
                
                if (doc.getRootNodeID() == 0) {
                    // this is data file; add file to zip
                    String filepath = PropertyService.getProperty("application.datafilepath");
                    if (!filepath.endsWith("/")) {
                        filepath += "/";
                    }
                    String filename = filepath + docid;
                    FileInputStream fin = null;
                    fin = new FileInputStream(filename);
                    try {
                        //use provided file name if we have one
                        if (providedFileName != null && providedFileName.length() > 1) {
                            zentry = new ZipEntry(providedFileName);
                        }
                        else {
                            zentry = new ZipEntry(docid);
                        }
                        zout.putNextEntry(zentry);
                        byte[] buf = new byte[4 * 1024]; // 4K buffer
                        int b = fin.read(buf);
                        while (b != -1) {
                            zout.write(buf, 0, b);
                            b = fin.read(buf);
                        }
                        fin.close();
                    } finally {
                        IOUtils.closeQuietly(fin);
                    }
                    zout.closeEntry();
                    
                } else {
                    // this is metadata doc; add doc to zip
                    bytestring = doc.getBytes();
                    //use provided file name if given
                    if (providedFileName != null && providedFileName.length() > 1) {
                        zentry = new ZipEntry(providedFileName);
                    }
                    else {
                        zentry = new ZipEntry(docid + ".xml");
                    }
                    zentry.setSize(bytestring.length);
                    zout.putNextEntry(zentry);
                    zout.write(bytestring, 0, bytestring.length);
                    zout.closeEntry();
                }
                EventLog.getInstance().log(request.getRemoteAddr(), request.getHeader("User-Agent"), user,
                        docid, "read");
            } catch (Exception except) {
                throw except;
            }
        }
    }
    
    /**
     * If metacat couldn't find a data file or document locally, it will read
     * this docid from its home server. This is for the replication feature
     */
    private void readFromRemoteMetaCat(HttpServletResponse response,
            String docid, String rev, String user, String password,
            ServletOutputStream out, boolean zip, ZipOutputStream zout)
            throws Exception {
        // Create a object of RemoteDocument, "" is for zipEntryPath
        RemoteDocument remoteDoc = new RemoteDocument(docid, rev, user,
                password, "");
        String docType = remoteDoc.getDocType();
        // Only read data file
        if (docType.equals("BIN")) {
            // If it is zip format
            if (zip) {
                remoteDoc.readDocumentFromRemoteServerByZip(zout);
            } else {
                if (out == null) {
                    out = response.getOutputStream();
                }
                response.setContentType("application/octet-stream");
                remoteDoc.readDocumentFromRemoteServer(out);
            }
        } else {
            throw new Exception("Docid: " + docid + "." + rev
                    + " couldn't find");
        }
    }
    
    /**
     * Handle the database putdocument request and write an XML document to the
     * database connection
     * @param userAgent 
     * @param generateSystemMetadata 
     */
    public String handleInsertOrUpdateAction(String ipAddress, String userAgent,
            HttpServletResponse response, PrintWriter out, Hashtable<String, String[]> params,
            String user, String[] groups, boolean generateSystemMetadata, boolean writeAccessRules) {
        Logger logMetacat = Logger.getLogger(MetacatHandler.class);
        DBConnection dbConn = null;
        int serialNumber = -1;
        String output = "";
        String qformat = null;
        if(params.containsKey("qformat"))
        {
          qformat = params.get("qformat")[0];
        }
        
        if(params.get("docid") == null){
            String msg = this.PROLOG +
                         this.ERROR  +
                         "Docid not specified" +
                         this.ERRORCLOSE;
            if(out != null)
            {
                out.println(msg);
                logMetacat.error("MetacatHandler.handleInsertOrUpdateAction - Docid not specified");
            }
            return msg; 
        }
        
        try {
            if (!AuthUtil.canInsertOrUpdate(user, groups)) {
                String msg = this.PROLOG +
                             this.ERROR +
                             "User '" + 
                             user + 
                             "' not allowed to insert and update" +
                             this.ERRORCLOSE;
                if(out != null)
                {
                  out.println(msg);
                }
                
                logMetacat.error("MetacatHandler.handleInsertOrUpdateAction - " + 
                		         "User '" + 
                		         user + 
                		         "' not allowed to insert and update");
                return msg;
            }
        } catch (MetacatUtilException ue) {
            logMetacat.error("MetacatHandler.handleInsertOrUpdateAction - " + 
            		         "Could not determine if user could insert or update: " + 
            		         ue.getMessage());
            ue.printStackTrace(System.out);
            // TODO: This is a bug, as it allows one to bypass the access check -- need to throw an exception
        }
        
        try {
          // Get the document indicated
          logMetacat.debug("MetacatHandler.handleInsertOrUpdateAction - params: " + 
        		           params.toString());
          
          String[] doctext = params.get("doctext");
          String pub = null;
          if (params.containsKey("public")) {
              pub = params.get("public")[0];
          }
          
          StringReader dtd = null;
          if (params.containsKey("dtdtext")) {
              String[] dtdtext = params.get("dtdtext");
              try {
                  if (!dtdtext[0].equals("")) {
                      dtd = new StringReader(dtdtext[0]);
                  }
              } catch (NullPointerException npe) {
              }
          }
          
          if(doctext == null){
              String msg = this.PROLOG +
                           this.ERROR +
                           "Document text not submitted." +
                           this.ERRORCLOSE;
              if(out != null)
              {
                out.println(msg);
              }
              
              // TODO: this should really throw an exception
              return msg;
          }
          
          logMetacat.debug("MetacatHandler.handleInsertOrUpdateAction - " + 
        		           "the xml document in metacat servlet (before parsing):\n" + 
        		           doctext[0]);
          StringReader xmlReader = new StringReader(doctext[0]);
          boolean validate = false;
          DocumentImplWrapper documentWrapper = null;
          String namespace = null;

          try {
            // look inside XML Document for <!DOCTYPE ... PUBLIC/SYSTEM ...
            // >
            // in order to decide whether to use validation parser
            validate = needDTDValidation(xmlReader);
            if (validate) {
                // set a dtd base validation parser
                String rule = DocumentImpl.DTD;
                documentWrapper = new DocumentImplWrapper(rule, validate, writeAccessRules);
            } else {
                
                namespace = XMLSchemaService.findDocumentNamespace(xmlReader);
                
                if (namespace != null) {
                    if (namespace.compareTo(DocumentImpl.EML2_0_0NAMESPACE) == 0
                            || namespace.compareTo(
                            DocumentImpl.EML2_0_1NAMESPACE) == 0) {
                        // set eml2 base     validation parser
                        String rule = DocumentImpl.EML200;
                        // using emlparser to check id validation
                        @SuppressWarnings("unused")
                        EMLParser parser = new EMLParser(doctext[0]);
                        documentWrapper = new DocumentImplWrapper(rule, true, writeAccessRules);
                    } else if (
                    		namespace.compareTo(DocumentImpl.EML2_1_0NAMESPACE) == 0
                    		|| namespace.compareTo(DocumentImpl.EML2_1_1NAMESPACE) == 0) {
                        // set eml2 base validation parser
                        String rule = DocumentImpl.EML210;
                        // using emlparser to check id validation
                        @SuppressWarnings("unused")
                        EMLParser parser = new EMLParser(doctext[0]);
                        documentWrapper = new DocumentImplWrapper(rule, true, writeAccessRules);
                    } else {
                        // set schema base validation parser
                        String rule = DocumentImpl.SCHEMA;
                        documentWrapper = new DocumentImplWrapper(rule, true, writeAccessRules);
                    }
                } else {
                    documentWrapper = new DocumentImplWrapper("", false, writeAccessRules);
                }
            }
            
            String[] action = params.get("action");
            String[] docid = params.get("docid");
            String newdocid = null;
            
            String doAction = null;
            if (action[0].equals("insert") || action[0].equals("insertmultipart")) {
                doAction = "INSERT";
                
            } else if (action[0].equals("update")) {
                doAction = "UPDATE";
                
            }
            
            try {
              // get a connection from the pool
              dbConn = DBConnectionPool
                      .getDBConnection("Metacathandler.handleInsertOrUpdateAction");
              serialNumber = dbConn.getCheckOutSerialNumber();
              
              // write the document to the database and disk
              String accNumber = docid[0];
              logMetacat.debug("MetacatHandler.handleInsertOrUpdateAction - " + 
                doAction + " " + accNumber + "...");
              Identifier identifier = new Identifier();
              identifier.setValue(accNumber);
              if(!D1NodeService.isValidIdentifier(identifier)) {
                  String error = "The docid "+accNumber +" is not valid since it is null or contians the white space(s).";
                  logMetacat.warn("MetacatHandler.handleInsertOrUpdateAction - " +error);
                  throw new Exception(error);
              }
              
              /*if (accNumber == null || accNumber.equals("")) {
                  logMetacat.warn("MetacatHandler.handleInsertOrUpdateAction - " +
                		          "writing with null acnumber");
                  newdocid = documentWrapper.write(dbConn, doctext[0], pub, dtd,
                          doAction, null, user, groups);
                  EventLog.getInstance().log(ipAddress, userAgent, user, "", action[0]);
              
              } else {*/
              newdocid = documentWrapper.write(dbConn, doctext[0], pub, dtd,
                          doAction, accNumber, user, groups);
            
              EventLog.getInstance().log(ipAddress, userAgent, user, accNumber, action[0]);
              
              //}
              
              // alert listeners of this event
              MetacatDocumentEvent mde = new MetacatDocumentEvent();
              mde.setDocid(accNumber);
              mde.setDoctype(namespace);
              mde.setAction(doAction);
              mde.setUser(user);
              mde.setGroups(groups);
              MetacatEventService.getInstance().notifyMetacatEventObservers(mde);
              
              // if it was called from Metacat API, we want to generate system metadata for it
              if ( generateSystemMetadata ) {
                
                SystemMetadata sysMeta = null;
				// it's possible that system metadata exists although
                // older clients don't support it. Try updates first.
                try {
                	// get the docid parts
                	String docidWithoutRev = DocumentUtil.getSmartDocId(newdocid);
                    int rev = IdentifierManager.getInstance().getLatestRevForLocalId(newdocid);
                	String guid = IdentifierManager.getInstance().getGUID(docidWithoutRev, rev);
                	sysMeta  = IdentifierManager.getInstance().getSystemMetadata(guid);
                	// TODO: need to update? we just looked it up
                	//IdentifierManager.getInstance().updateSystemMetadata(sysMeta);
                  
                } catch ( McdbDocNotFoundException mnfe) {
                  
                  // handle inserts
                  try {
                	// create the system metadata  
                    sysMeta = SystemMetadataFactory.createSystemMetadata(newdocid, true, false);
                    
                    // save it to the map
                    HazelcastService.getInstance().getSystemMetadataMap().put(sysMeta.getIdentifier(), sysMeta);
                    
                    // submit for indexing
                    MetacatSolrIndex.getInstance().submit(sysMeta.getIdentifier(), sysMeta, null);
                    
                  } catch ( McdbDocNotFoundException dnfe ) {
                    logMetacat.debug(
                      "There was a problem finding the localId " +
                      newdocid + "The error was: " + dnfe.getMessage());
                    throw dnfe;
            
                  } catch ( AccessionNumberException ane ) {
            
                    logMetacat.debug(
                      "There was a problem creating the accession number " +
                      "for " + newdocid + ". The error was: " + ane.getMessage());
                    throw ane;
                    
                  } // end try()
                                        
                }
                
              } // end if()
              
              
            } finally {
                // Return db connection
                DBConnectionPool.returnDBConnection(dbConn, serialNumber);
            }
            
            // set content type and other response header fields first
            //response.setContentType("text/xml");
            output += this.PROLOG;
            output += this.SUCCESS;
            output += "<docid>" + newdocid + "</docid>";
            output += this.SUCCESSCLOSE;
            
          } catch (NullPointerException npe) {
              //response.setContentType("text/xml");
              output += this.PROLOG;
              output += this.ERROR;
              output += npe.getMessage();
              output += this.ERRORCLOSE;
              logMetacat.warn("MetacatHandler.handleInsertOrUpdateAction - " +
            		          "Null pointer error when writing eml " +
            		          "document to the database: " + 
            		          npe.getMessage());
              npe.printStackTrace();
          }
        } catch (Exception e) {
            //response.setContentType("text/xml");
            output += this.PROLOG;
            output += this.ERROR;
            output += e.getMessage();
            output += this.ERRORCLOSE;
            logMetacat.warn("MetacatHandler.handleInsertOrUpdateAction - " +
            		        "General error when writing eml " +
            		        "document to the database: " + 
            		        e.getMessage());
            e.printStackTrace();
        }
        
        if (qformat == null || qformat.equals("xml")) {
            if(response != null)
            {
              response.setContentType("text/xml");
              out.println(output);
            }
            return output;
        } else {
            try {
                DBTransform trans = new DBTransform();
                response.setContentType("text/html");
                trans.transformXMLDocument(output,
                        "message", "-//W3C//HTML//EN", qformat,
                        out, null, null);
            } catch (Exception e) {
                
                logMetacat.error("MetacatHandler.handleInsertOrUpdateAction - " +
                		         "General error: " + 
                		         e.getMessage());
                e.printStackTrace(System.out);
            }
        }
        return null;
    }
    
    /**
     * Parse XML Document to look for <!DOCTYPE ... PUBLIC/SYSTEM ... > in
     * order to decide whether to use validation parser
     */
    private static boolean needDTDValidation(StringReader xmlreader)
    throws IOException {
        Logger logMetacat = Logger.getLogger(MetacatHandler.class);
        StringBuffer cbuff = new StringBuffer();
        java.util.Stack<String> st = new java.util.Stack<String>();
        boolean validate = false;
        boolean commented = false;
        int c;
        int inx;
        
        // read from the stream until find the keywords
        while ((st.empty() || st.size() < 4) && ((c = xmlreader.read()) != -1)) {
            cbuff.append((char) c);
            
            if ((inx = cbuff.toString().indexOf("<!--")) != -1) {
                commented = true;
            }
            
            // "<!DOCTYPE" keyword is found; put it in the stack
            if ((inx = cbuff.toString().indexOf("<!DOCTYPE")) != -1) {
                cbuff = new StringBuffer();
                st.push("<!DOCTYPE");
            }
            // "PUBLIC" keyword is found; put it in the stack
            if ((inx = cbuff.toString().indexOf("PUBLIC")) != -1) {
                cbuff = new StringBuffer();
                st.push("PUBLIC");
            }
            // "SYSTEM" keyword is found; put it in the stack
            if ((inx = cbuff.toString().indexOf("SYSTEM")) != -1) {
                cbuff = new StringBuffer();
                st.push("SYSTEM");
            }
            // ">" character is found; put it in the stack
            // ">" is found twice: fisrt from <?xml ...?>
            // and second from <!DOCTYPE ... >
            if ((inx = cbuff.toString().indexOf(">")) != -1) {
                cbuff = new StringBuffer();
                st.push(">");
            }
        }
        
        // close the stream
        xmlreader.reset();
        
        // check the stack whether it contains the keywords:
        // "<!DOCTYPE", "PUBLIC" or "SYSTEM", and ">" in this order
        if (st.size() == 4) {
            if ((st.pop()).equals(">")
            && ((st.peek()).equals("PUBLIC") | (st.pop()).equals("SYSTEM"))
                    && (st.pop()).equals("<!DOCTYPE")) {
                validate = true && !commented;
            }
        }
        
        logMetacat.info("MetacatHandler.needDTDValidation - Validation for dtd is " + 
        		        validate);
        return validate;
    }
    
    // END OF INSERT/UPDATE SECTION
    
    /**
     * Handle the database delete request and delete an XML document from the
     * database connection
     */
    public void handleDeleteAction(PrintWriter out, Hashtable<String, String[]> params,
      HttpServletRequest request, HttpServletResponse response,
      String user, String[] groups) {
      
      Logger logMetacat = Logger.getLogger(MetacatHandler.class);
      String[] docid = params.get("docid");
      
      if(docid == null){
        response.setContentType("text/xml");
        out.println(this.PROLOG);
        out.println(this.ERROR);
        out.println("Docid not specified.");
        out.println(this.ERRORCLOSE);
        logMetacat.error("MetacatHandler.handleDeleteAction - " +
          "Docid not specified for the document to be deleted.");
      
      } else {
        
        // delete the document from the database
        String localId = null;
        try {
          
          // is the docid a GUID? 
          IdentifierManager im = IdentifierManager.getInstance();
          localId = im.getLocalId(docid[0]);
          this.deleteFromMetacat(out, request, response, localId, 
            user, groups);
          
        } catch (McdbDocNotFoundException mdnfe) {
          
          try {
            localId = docid[0];

            // not a GUID, use the docid instead
            this.deleteFromMetacat(out, request, response, localId, 
              user, groups);
              
          } catch ( McdbDocNotFoundException dnfe ) {
            response.setContentType("text/xml");
            out.println(this.PROLOG);
            out.println(this.ERROR);
            //out.println("Error deleting document!!!");
            out.println(dnfe.getMessage());
            out.println(this.ERRORCLOSE);
            logMetacat.error("MetacatHandler.handleDeleteAction - " +
              "Document could not be deleted: "
                    + dnfe.getMessage());
            dnfe.printStackTrace(System.out);
            
          } // end try()
          
        } // end try()
        
        // alert that it happened
        MetacatDocumentEvent mde = new MetacatDocumentEvent();
        mde.setDocid(localId);
        mde.setDoctype(null);
        mde.setAction("delete");
        mde.setUser(user);
        mde.setGroups(groups);
        MetacatEventService.getInstance().notifyMetacatEventObservers(mde);
        
      } // end if()
      
    }
    
    /**
     * Handle the validation request and return the results to the requestor
     */
    protected void handleValidateAction(PrintWriter out, Hashtable<String, String[]> params) {
        
        // Get the document indicated
        String valtext = null;
        DBConnection dbConn = null;
        int serialNumber = -1;
        
        try {
            valtext = params.get("valtext")[0];
        } catch (Exception nullpe) {
            
            String docid = null;
            try {
                // Find the document id number
                docid = params.get("docid")[0];
                
                // Get the document indicated from the db
                DocumentImpl xmldoc = new DocumentImpl(docid, false);
                valtext = xmldoc.toString();
                
            } catch (NullPointerException npe) {
                
                out.println("<error>Error getting document ID: " + docid
                        + "</error>");
                //if ( conn != null ) { util.returnConnection(conn); }
                return;
            } catch (Exception e) {
                
                out.println(e.getMessage());
            }
        }
        
        try {
            // get a connection from the pool
            dbConn = DBConnectionPool
                    .getDBConnection("MetacatHandler.handleValidateAction");
            serialNumber = dbConn.getCheckOutSerialNumber();
            DBValidate valobj = new DBValidate(dbConn);
//            boolean valid = valobj.validateString(valtext);
            
            // set content type and other response header fields first
            
            out.println(valobj.returnErrors());
            
        } catch (NullPointerException npe2) {
            // set content type and other response header fields first
            
            out.println("<error>Error validating document.</error>");
        } catch (Exception e) {
            
            out.println(e.getMessage());
        } finally {
            // Return db connection
            DBConnectionPool.returnDBConnection(dbConn, serialNumber);
        }
    }
    
    /**
     * Look up the pid (guid)-to-docid mapping.
     * Returns XML on the response, e.g.:
     * <docid>sample.1.1</docid>
     * 
     * @param params
     * @param response
     * @throws IOException
     */
    protected void handleGetDocid(Hashtable<String, String[]> params, HttpServletResponse response) throws IOException {
        response.setContentType("text/xml");
    	ServletOutputStream out = response.getOutputStream();
    	try {
            // Get pid from parameters
    		String pid = null;
            if (params.containsKey("pid")) {
            	pid = params.get("pid")[0];
            }
            String docid = IdentifierManager.getInstance().getLocalId(pid);
            out.println(PROLOG);
            out.print("<docid>");
            out.print(docid);
            out.print("</docid>");
    		
    	} catch (Exception e) {
            // Handle exception
            out.println(PROLOG);
            out.println(ERROR);
            out.println(e.getMessage());
            out.println(ERRORCLOSE);
        } finally {
        	out.close();
        }
    	
    }
    
    /**
     * Handle "getrevsionanddoctype" action Given a docid, return it's current
     * revision and doctype from data base The output is String look like
     * "rev;doctype"
     */
    protected void handleGetRevisionAndDocTypeAction(PrintWriter out,
            Hashtable<String, String[]> params) {
        // To store doc parameter
        String[] docs = new String[10];
        // Store a single doc id
        String givenDocId = null;
        // Get docid from parameters
        if (params.containsKey("docid")) {
            docs = params.get("docid");
        }
        // Get first docid form string array
        givenDocId = docs[0];
        
        try {
            // Make sure there is a docid
            if (givenDocId == null || givenDocId.equals("")) { throw new Exception(
                    "User didn't specify docid!"); }//if
            
            // Create a DBUtil object
            DBUtil dbutil = new DBUtil();
            // Get a rev and doctype
            String revAndDocType = dbutil
                    .getCurrentRevisionAndDocTypeForGivenDocument(givenDocId);
            out.println(revAndDocType);
            
        } catch (Exception e) {
            // Handle exception
            out.println("<?xml version=\"1.0\"?>");
            out.println("<error>");
            out.println(e.getMessage());
            out.println("</error>");
        }
        
    }
    
    /**
     * Handle "getaccesscontrol" action. Read Access Control List from db
     * connection in XML format
     */
    protected void handleGetAccessControlAction(PrintWriter out,
            Hashtable<String,String[]> params, HttpServletResponse response, String username,
            String[] groupnames) {
        
        Logger logMetacat = Logger.getLogger(MetacatHandler.class);

        String docid = params.get("docid")[0];
        if (docid.startsWith("urn:")) {
            try {
                String actualDocId = LSIDUtil.getDocId(docid, false);
                docid = actualDocId;
            } catch (ParseLSIDException ple) {
                logMetacat.error("MetacatHandler.handleGetAccessControlAction - " +
                                 "could not parse lsid: " + 
                                 docid + " : " + ple.getMessage());  
                ple.printStackTrace(System.out);
            }
        }
        
        String qformat = "xml";
        if (params.get("qformat") != null) {
            qformat = (params.get("qformat"))[0];
        }
        
        try {
            AccessControlForSingleFile acfsf = new AccessControlForSingleFile(docid);
            String acltext = acfsf.getACL(username, groupnames);
            if (qformat.equals("xml")) {
                response.setContentType("text/xml");
                out.println(acltext);
            } else {
                DBTransform trans = new DBTransform();
                response.setContentType("text/html");
                trans.transformXMLDocument(acltext,"-//NCEAS//getaccesscontrol//EN", 
                    "-//W3C//HTML//EN", qformat, out, params, null);              
            }            
        } catch (Exception e) {
            out.println("<?xml version=\"1.0\"?>");
            out.println("<error>");
            out.println(e.getMessage());
            out.println("</error>");
        } 
//        finally {
//            // Retrun db connection to pool
//            DBConnectionPool.returnDBConnection(dbConn, serialNumber);
//        }
    }
    
    /**
     * Handle the "getprincipals" action. Read all principals from
     * authentication scheme in XML format
     * @throws IOException 
     */
    protected void handleGetPrincipalsAction(Writer out, String user,
            String password) throws IOException {
        try {
            AuthSession auth = new AuthSession();
            String principals = auth.getPrincipals(user, password);
            out.write(principals);
            
        } catch (Exception e) {
            out.write("<?xml version=\"1.0\"?>");
            out.write("<error>");
            out.write(e.getMessage());
            out.write("</error>");
        }
    }
    
    /**
     * Handle "getdoctypes" action. Read all doctypes from db connection in XML
     * format
     */
    protected void handleGetDoctypesAction(PrintWriter out, Hashtable<String, 
    		String[]> params, HttpServletResponse response) {
        try {
            DBUtil dbutil = new DBUtil();
            String doctypes = dbutil.readDoctypes();
            out.println(doctypes);
        } catch (Exception e) {
            out.println("<?xml version=\"1.0\"?>");
            out.println("<error>");
            out.println(e.getMessage());
            out.println("</error>");
        }
    }
    
    /**
     * Handle the "getdtdschema" action. Read DTD or Schema file for a given
     * doctype from Metacat catalog system
     */
    protected void handleGetDTDSchemaAction(PrintWriter out, Hashtable<String, 
    		String[]> params, HttpServletResponse response) {
        
        String doctype = null;
        String[] doctypeArr = params.get("doctype");
        
        // get only the first doctype specified in the list of doctypes
        // it could be done for all doctypes in that list
        if (doctypeArr != null) {
            doctype = params.get("doctype")[0];
        }
        
        try {
            DBUtil dbutil = new DBUtil();
            String dtdschema = dbutil.readDTDSchema(doctype);
            out.println(dtdschema);
            
        } catch (Exception e) {
            out.println("<?xml version=\"1.0\"?>");
            out.println("<error>");
            out.println(e.getMessage());
            out.println("</error>");
        }
        
    }
    
    /**
     * Check if the document is registered in either the xml_documents or xml_revisions table
     * @param out the writer to write the xml results to
     * @param params request parameters
     * @param response the http servlet response
     */
    public void handleIdIsRegisteredAction(PrintWriter out, Hashtable<String, 
    		String[]> params, HttpServletResponse response) {
        String id = null;
        boolean exists = false;
        if(params.get("docid") != null) {
            id = params.get("docid")[0];
        }
        
        try {
            DBUtil dbutil = new DBUtil();
            exists = dbutil.idExists(id);
        } catch (Exception e) {
            out.println("<?xml version=\"1.0\"?>");
            out.println("<error>");
            out.println(e.getMessage());
            out.println("</error>");
        }
        
        out.println("<?xml version=\"1.0\"?>");
        out.println("<isRegistered>");
        out.println("<docid>" + id + "</docid>");
        out.println("<exists>" + exists + "</exists>");
        out.println("</isRegistered>");
    }
    
    /**
     * Handle the "getalldocids" action. return a list of all docids registered
     * in the system
     */
    public void handleGetAllDocidsAction(PrintWriter out, Hashtable<String, 
    		String[]> params, HttpServletResponse response) {
        String scope = null;
        if(params.get("scope") != null) {
            scope = params.get("scope")[0];
        }
        
        try {
            Vector<String> docids = DBUtil.getAllDocids(scope);
            out.println("<?xml version=\"1.0\"?>");
            out.println("<idList>");
            out.println("  <scope>" + scope + "</scope>");
            for(int i=0; i<docids.size(); i++) {
                String docid = docids.elementAt(i);
                out.println("  <docid>" + docid + "</docid>");
            }
            out.println("</idList>");
            
        } catch (Exception e) {
            out.println("<?xml version=\"1.0\"?>");
            out.println("<error>");
            out.println(e.getMessage());
            out.println("</error>");
        }
    }
    
    /**
     * Handle the "getlastdocid" action. Get the latest docid with rev number
     * from db connection in XML format
     */
    public void handleGetMaxDocidAction(PrintWriter out, Hashtable<String, 
    		String[]> params, HttpServletResponse response) {
        
        String scope = params.get("scope")[0];
        if (scope == null) {
            scope = params.get("username")[0];
        }
        
        try {
            
            DBUtil dbutil = new DBUtil();
            String lastDocid = dbutil.getMaxDocid(scope);
            out.println("<?xml version=\"1.0\"?>");
            out.println("<lastDocid>");
            out.println("  <scope>" + scope + "</scope>");
            out.println("  <docid>" + lastDocid + "</docid>");
            out.println("</lastDocid>");
            
        } catch (Exception e) {
            out.println("<?xml version=\"1.0\"?>");
            out.println("<error>");
            out.println(e.getMessage());
            out.println("</error>");
        }
    }
    
    /**
     * Print a report from the event log based on filter parameters passed in
     * from the web.
     *
     * @param params the parameters from the web request
     * @param request the http request object for getting request details
     * @param response the http response object for writing output
     */
    protected void handleGetLogAction(Hashtable<String, String[]> params, 
    		HttpServletRequest request, HttpServletResponse response, 
    		String username, String[] groups, String sessionId) {
        Logger logMetacat = Logger.getLogger(MetacatHandler.class);
        try {
        	// figure out the output as part of the action
            PrintWriter out = null;
            
            String[] qformatParam = params.get("qformat");
            String qformat = null;
            if (qformatParam != null && qformatParam.length > 0) {
            	qformat = qformatParam[0];
            }
            
            // Get all of the parameters in the correct formats
            String[] ipAddress = params.get("ipaddress");
            String[] principal = params.get("principal");
            String[] docid = params.get("docid");
            String[] event = params.get("event");
            String[] startArray = params.get("start");
            String[] endArray = params.get("end");
            String start = null;
            String end = null;
            if (startArray != null) {
                start = startArray[0];
            }
            if (endArray != null) {
                end = endArray[0];
            }
            Timestamp startDate = null;
            Timestamp endDate = null;
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            try {
                if (start != null) {
                    startDate = new Timestamp((format.parse(start)).getTime());
                }
                if (end != null) {
                    endDate = new Timestamp((format.parse(end)).getTime());
                }
            } catch (ParseException e) {
                logMetacat.error("MetacatHandler.handleGetLogAction - " +
                		         "Failed to created Timestamp from input.");
                e.printStackTrace(System.out);
            }
            
            boolean anon = false;
            // Check that the user is authenticated as an administrator account
            if (!AuthUtil.isAdministrator(username, groups)) {
                anon = true;
            	// public can view only for a specific doc id
                if (docid == null || docid.length == 0) {
                	response.setContentType("text/xml");
                    out = response.getWriter();
	                out.print("<error>");
	                out.print("The user \"" + username +
	                        "\" is not authorized for this action.");
	                out.print("</error>");
	                return;
                }
            }
            
            String report = 
            	EventLog.getInstance().getReport(
            		ipAddress, 
            		principal,
                    docid, 
                    event, 
                    startDate, 
                    endDate, 
                    anon);
            
            // something other than xml
            if (qformat != null && !qformat.equals("xml")) {
                response.setContentType("text/html");
                out = response.getWriter();
                
                try {
	                DBTransform trans = new DBTransform();
					trans.transformXMLDocument(
	                		report,
	                        "-//NCEAS//log//EN", 
	                        "-//W3C//HTML//EN", 
	                        qformat,
	                        out, 
	                        params, 
	                        sessionId);
	            } catch (Exception e) {               
	                logMetacat.error("MetacatHandler.handleGetLogAction - General error"
	                        + e.getMessage());
	                e.printStackTrace(System.out);
	            }
            } else {
            	// output as xml
            	response.setContentType("text/xml");
                out = response.getWriter();
                out.println(report);
	            out.close();
            }
            
        } catch (IOException e) {
            logMetacat.error("MetacatHandler.handleGetLogAction - " +
            		         "Could not open http response for writing: " + 
            		         e.getMessage());
            e.printStackTrace(System.out);
        } catch (MetacatUtilException ue) {
            logMetacat.error("MetacatHandler.handleGetLogAction - " +
            		         "Could not determine if user is administrator: " + 
            		         ue.getMessage());
            ue.printStackTrace(System.out);
        }
    }
    
    /**
     * Rebuild the index for one or more documents. If the docid parameter is
     * provided, rebuild for just that one document or list of documents. If
     * not, then rebuild the index for all documents in the xml_documents table.
     * 
     * @param params
     *            the parameters from the web request
     * @param request
     *            the http request object for getting request details
     * @param response
     *            the http response object for writing output
     * @param username
     *            the username of the authenticated user
     */
    protected void handleBuildIndexAction(Hashtable<String, String[]> params,
            HttpServletRequest request, HttpServletResponse response,
            String username, String[] groups) {
        Logger logMetacat = Logger.getLogger(MetacatHandler.class);
        
        // Get all of the parameters in the correct formats
        String[] docid = params.get("docid");
        
        // Rebuild the indices for appropriate documents
        try {
            response.setContentType("text/xml");
            PrintWriter out = response.getWriter();
            if(!EnabledQueryEngines.getInstance().isEnabled(EnabledQueryEngines.PATHQUERYENGINE)) {
                out.print("<error>");
                out.print(DBQuery.XPATHQUERYOFFINFO);
                out.print("</error>");
                return;
            }
            
            // Check that the user is authenticated as an administrator account
            if (!AuthUtil.isAdministrator(username, groups)) {
                out.print("<error>");
                out.print("The user \"" + username +
                        "\" is not authorized for this action.");
                out.print("</error>");
                return;
            }
            
            // Process the documents
            out.println("<success>");
            if (docid == null || docid.length == 0) {
                // Process all of the documents
                try {
                    Vector<String> documents = getDocumentList();
                    Iterator<String> it = documents.iterator();
                    while (it.hasNext()) {
                        String id = it.next();
                        System.out.println("building doc index for all documents");
                        buildDocumentIndex(id, out);
                        System.out.println("done building doc index for all documents");
                    }
                } catch (SQLException se) {
                    out.print("<error>");
                    out.print(se.getMessage());
                    out.println("</error>");
                }
            } else {
                // Only process the requested documents
                for (int i = 0; i < docid.length; i++) {
                    System.out.println("building doc index for document " + docid[i]);
                    buildDocumentIndex(docid[i], out);
                    System.out.println("done building doc index for document " + docid[i]);
                }
            }
            out.println("</success>");
            out.close();
        } catch (IOException e) {
            logMetacat.error("MetacatHandler.handleBuildIndexAction - " +
            		         "Could not open http response for writing: " + 
            		         e.getMessage());
            e.printStackTrace(System.out);
        } catch (MetacatUtilException ue) {
            logMetacat.error("MetacatHandler.handleBuildIndexAction - " +
            		         "Could not determine if user is administrator: " + 
            		         ue.getMessage());
            ue.printStackTrace(System.out);
        }
    }
    
    /**
     * Rebuild the index for one or more documents. If the "pid" parameter is
     * provided, rebuild for just that one document (or list of documents). If
     * not, an error message will be returned.
     * 
     * @param params
     *            the parameters from the web request
     * @param request
     *            the http request object for getting request details
     * @param response
     *            the http response object for writing output
     * @param username
     *            the username of the authenticated user
     */
    protected void handleReindexAction(Hashtable<String, String[]> params,
            HttpServletRequest request, HttpServletResponse response,
            String username, String[] groups) {
        Logger logMetacat = Logger.getLogger(MetacatHandler.class);
        
        // Get all of the parameters in the correct formats
        String[] pid = params.get("pid");
        PrintWriter out = null;
        // Process the documents
        StringBuffer results = new StringBuffer();
        // Rebuild the indices for appropriate documents
        try {
            response.setContentType("text/xml");
            out = response.getWriter();
            
            // Check that the user is authenticated as an administrator account
            if (!AuthUtil.isAdministrator(username, groups)) {
                out.print("<error>");
                out.print("The user \"" + username +
                        "\" is not authorized for this action.");
                out.print("</error>");
                out.close();
                return;
            }
            
           
            if (pid == null || pid.length == 0) {
                //report the error
                results = new StringBuffer();
                results.append("<error>");
                results.append("The parameter - pid is missing. Please check your parameter list.");
                results.append("</error>");
            } else {
                Vector<String> successList = new Vector<String>();
                Vector<String> failedList = new Vector<String>();
                
                // Only process the requested documents
                for (int i = 0; i < pid.length; i++) {
                    String id = pid[i];
                    logMetacat.info("queueing doc index for pid " + id);
                    Identifier identifier = new Identifier();
                    identifier.setValue(id);
					SystemMetadata sysMeta = HazelcastService.getInstance().getSystemMetadataMap().get(identifier);
					if (sysMeta == null) {
					     failedList.add(id);
					     logMetacat.info("no system metadata was found for pid " + id);
					} else {
						try {
							// submit for indexing
						    Map<String, List<Object>> fields = EventLog.getInstance().getIndexFields(identifier, Event.READ.xmlValue());
	                        MetacatSolrIndex.getInstance().submit(identifier, sysMeta, fields);
						} catch (Exception e) {
							failedList.add(id);
						    logMetacat.info("Error submitting to index for pid " + id);
						    continue;
						}
					    successList.add(id);
					    logMetacat.info("done queueing doc index for pid " + id);
					}
                }
                results.append("<results>\n");
                if(successList.size() >0) {
                    results.append("<success>\n");
                    for(String id : successList) {
                        results.append("<pid>" + id + "</pid>\n");
                    }
                    results.append("</success>");
                }
                
                if(failedList.size()>0) {
                    results.append("<error>\n");
                    for(String id : failedList) {
                        results.append("<pid>" + id + "</pid>\n");
                    }
                    results.append("</error>");
                }
                results.append("</results>\n");
            }
        } catch (IOException e) {
            logMetacat.error("MetacatHandler.handleBuildIndexAction - " +
            		         "Could not open http response for writing: " + 
            		         e.getMessage());
            e.printStackTrace();
        } catch (MetacatUtilException ue) {
            logMetacat.error("MetacatHandler.handleBuildIndexAction - " +
            		         "Could not determine if user is administrator: " + 
            		         ue.getMessage());
            ue.printStackTrace();
        } finally {
            if(out != null) {
                out.print(results.toString());
                out.close();
            }
        }
    }
    
    
    /**
     *Rebuild the index for all documents in the systemMetadata table.
     * 
     * @param params
     *            the parameters from the web request
     * @param request
     *            the http request object for getting request details
     * @param response
     *            the http response object for writing output
     * @param username
     *            the username of the authenticated user
     */
    protected void handleReindexAllAction(Hashtable<String, String[]> params,
            HttpServletRequest request, HttpServletResponse response,
            String username, String[] groups) {
        Logger logMetacat = Logger.getLogger(MetacatHandler.class);
        
      
        
        // Rebuild the indices for all documents which are in the systemmetadata table
        PrintWriter out = null;
     // Process the documents
        StringBuffer results = new StringBuffer();
        try {
            response.setContentType("text/xml");
            out = response.getWriter();
            
            // Check that the user is authenticated as an administrator account
            if (!AuthUtil.isAdministrator(username, groups)) {
                out.print("<error>");
                out.print("The user \"" + username +
                        "\" is not authorized for this action.");
                out.print("</error>");
                out.close();
                return;
            }
          
           // Process all of the documents
           logMetacat.info("queueing doc index for all documents");
           try {
                    List<String> allIdentifiers = IdentifierManager.getInstance().getAllSystemMetadataGUIDs();
                    Iterator<String> it = allIdentifiers.iterator();
                    results.append("<success>");
                    while (it.hasNext()) {
                        String id = it.next();
                        Identifier identifier = new Identifier();
                        identifier.setValue(id);
                        SystemMetadata sysMeta = HazelcastService.getInstance().getSystemMetadataMap().get(identifier);
                        if (sysMeta != null) {
                        	
                            // submit for indexing
    					    Map<String, List<Object>> fields = EventLog.getInstance().getIndexFields(identifier, Event.READ.xmlValue());
                            MetacatSolrIndex.getInstance().submit(identifier, sysMeta, fields);

    					    results.append("<pid>" + id + "</pid>\n");
                            logMetacat.debug("queued SystemMetadata for index on pid: " + id);
                        }
                        
                    }
                    results.append("</success>");
                    logMetacat.info("done queueing index for all documents");
           } catch (Exception e) {
                    // report the error
                    results = new StringBuffer();
                    results.append("<error>");
                    results.append(e.getMessage());
                    results.append("</error>");
           }
          
        } catch (IOException e) {
            logMetacat.error("MetacatHandler.handleBuildIndexAction - " +
                             "Could not open http response for writing: " + 
                             e.getMessage());
            e.printStackTrace();
        } catch (MetacatUtilException ue) {
            logMetacat.error("MetacatHandler.handleBuildIndexAction - " +
                             "Could not determine if user is administrator: " + 
                             ue.getMessage());
            ue.printStackTrace();
        } finally {
            if(out != null) {
                out.print(results.toString());
                out.close();
            }
           
        }
    }
    
    /**
     * Build the index for one document by reading the document and
     * calling its buildIndex() method.
     *
     * @param docid the document (with revision) to rebuild
     * @param out the PrintWriter to which output is printed
     */
    private void buildDocumentIndex(String docid, PrintWriter out) {
        //if the pathquery option is off, we don't need to build index.
        if(!EnabledQueryEngines.getInstance().isEnabled(EnabledQueryEngines.PATHQUERYENGINE)) {
            return;
        }
        try {
            DocumentImpl doc = new DocumentImpl(docid, false);
            doc.buildIndex();
            out.print("<docid>" + docid);
            out.println("</docid>");
        } catch (McdbException me) {
            out.print("<error>");
            out.print(me.getMessage());
            out.println("</error>");
        }
    }
    
    /**
     * Handle documents passed to metacat that are encoded using the
     * "multipart/form-data" mime type. This is typically used for uploading
     * data files which may be binary and large.
     */
    protected void handleMultipartForm(HttpServletRequest request,
            HttpServletResponse response) {
        Logger logMetacat = Logger.getLogger(MetacatHandler.class);
        PrintWriter out = null;
        String action = null;
        File tempFile = null;
        
        // Parse the multipart form, and save the parameters in a Hashtable and
        // save the FileParts in a hashtable
        
        Hashtable<String,String[]> params = new Hashtable<String,String[]>();
        Hashtable<String,String> fileList = new Hashtable<String,String>();
        int sizeLimit = 1000;
        try {
            sizeLimit = 
                (new Integer(PropertyService.getProperty("replication.datafilesizelimit"))).intValue();
        } catch (PropertyNotFoundException pnfe) {
            logMetacat.error("MetacatHandler.handleMultipartForm - " +
            		         "Could not determine data file size limit.  Using 1000. " + 
            		         pnfe.getMessage());
            pnfe.printStackTrace(System.out);
        }
        logMetacat.debug("MetacatHandler.handleMultipartForm - " +
        		         "The size limit of uploaded data files is: " + 
        		         sizeLimit);
        
        try {
            MultipartParser mp = new MultipartParser(request,
                    sizeLimit * 1024 * 1024);
            Part part;
            
            while ((part = mp.readNextPart()) != null) {
                String name = part.getName();
                
                if (part.isParam()) {
                    // it's a parameter part
                    ParamPart paramPart = (ParamPart) part;
                    String[] values = new String[1];
                    values[0] = paramPart.getStringValue();
                    params.put(name, values);
                    if (name.equals("action")) {
                        action = values[0];
                    }
                } else if (part.isFile()) {
                    // it's a file part
                    FilePart filePart = (FilePart) part;
                    String fileName = filePart.getFileName();
                    
                    // the filePart will be clobbered on the next loop, save to disk
                    tempFile = MetacatUtil.writeTempUploadFile(filePart, fileName);
                    fileList.put(name, tempFile.getAbsolutePath());
                    fileList.put("filename", fileName);
                    fileList.put("name", tempFile.getAbsolutePath());
                } else {
                    logMetacat.info("MetacatHandler.handleMultipartForm - " +
                    		        "Upload name '" + name + "' was empty.");
                }
            }
        } catch (IOException ioe) {
            try {
                out = response.getWriter();
            } catch (IOException ioe2) {
                logMetacat.fatal("MetacatHandler.handleMultipartForm - " +
                		         "Fatal Error: couldn't get response output stream.");
            }
            out.println("<?xml version=\"1.0\"?>");
            out.println("<error>");
            out.println("Error: problem reading multipart data: " + ioe.getMessage());
            out.println("</error>");
            out.close();
            return;
        }
        
        // Get the session information
        String username = null;
        String password = null;
        String[] groupnames = null;
        String sess_id = null;
        
        // be aware of session expiration on every request
        HttpSession sess = request.getSession(true);
        if (sess.isNew()) {
            // session expired or has not been stored b/w user requests
            username = "public";
            sess.setAttribute("username", username);
        } else {
            username = (String) sess.getAttribute("username");
            password = (String) sess.getAttribute("password");
            groupnames = (String[]) sess.getAttribute("groupnames");
            try {
                sess_id = (String) sess.getId();
            } catch (IllegalStateException ise) {
                System.out
                        .println("error in  handleMultipartForm: this shouldn't "
                        + "happen: the session should be valid: "
                        + ise.getMessage());
            }
        }
        
        // Get the out stream
        try {
            out = response.getWriter();
        } catch (IOException ioe2) {
            logMetacat.error("MetacatHandler.handleMultipartForm - " +
            		         "Fatal Error: couldn't get response " + 
            		         "output stream.");
            ioe2.printStackTrace(System.out);
        }
        
        if (action.equals("upload")) {
            if (username != null && !username.equals("public")) {
                handleUploadAction(request, out, params, fileList, username,
                        groupnames, response);
            } else {                
                out.println("<?xml version=\"1.0\"?>");
                out.println("<error>");
                out.println("Permission denied for " + action);
                out.println("</error>");
            }
        } else if(action.equals("insertmultipart")) {
          if (username != null && !username.equals("public")) {
            logMetacat.debug("MetacatHandler.handleMultipartForm - handling multipart insert");
              handleInsertMultipartAction(request, response,
                            out, params, fileList, username, groupnames);
          } else {
              out.println("<?xml version=\"1.0\"?>");
              out.println("<error>");
              out.println("Permission denied for " + action);
              out.println("</error>");
          }
        } else {
            /*
             * try { out = response.getWriter(); } catch (IOException ioe2) {
             * System.err.println("Fatal Error: couldn't get response output
             * stream.");
             */
            out.println("<?xml version=\"1.0\"?>");
            out.println("<error>");
            out.println("Error: action not registered.  Please report this error.");
            out.println("</error>");
        }
        
        // clean up the temp file
        if (tempFile != null && tempFile.exists()) {
        	tempFile.delete();
        }
        out.close();
    }
    
    /**
     * Handle the upload action by saving the attached file to disk and
     * registering it in the Metacat db
     */
    private void handleInsertMultipartAction(HttpServletRequest request, 
            HttpServletResponse response,
            PrintWriter out, Hashtable<String,String[]> params, Hashtable<String,String> fileList,
            String username, String[] groupnames)
    {
      Logger logMetacat = Logger.getLogger(MetacatHandler.class);
      String action = null;
      String docid = null;
      String qformat = null;
      String output = "";
      
      /*
       * response.setContentType("text/xml"); try { out =
       * response.getWriter(); } catch (IOException ioe2) {
       * System.err.println("Fatal Error: couldn't get response output
       * stream.");
       */
      if(params.containsKey("qformat")) 
      {
          qformat = params.get("qformat")[0];
      }
      
      if (params.containsKey("docid")) 
      {
          docid = params.get("docid")[0];
      }
      
      Identifier identifier = new Identifier();
      identifier.setValue(docid);
      if(!D1NodeService.isValidIdentifier(identifier)) {
          output += this.PROLOG;
          output += this.ERROR;
          output += "The docid "+docid +" is not valid since it is null or contians the white space(s).";
          output += this.ERRORCLOSE;
          logMetacat.warn("MetacatHandler.handleInsertMultipartAction - " +
                          "The docid "+docid +" is not valid since it is null or contians the white space(s).");
          if (qformat == null || qformat.equals("xml")) {
              response.setContentType("text/xml");
              out.println(output);
          } else {
              try {
                  DBTransform trans = new DBTransform();
                  response.setContentType("text/html");
                  trans.transformXMLDocument(output,
                          "message", "-//W3C//HTML//EN", qformat,
                          out, null, null);
              } catch (Exception e) {

                  logMetacat.error("MetacatHandler.handleInsertMultipartAction - General error: "
                          + e.getMessage());
                  e.printStackTrace(System.out);
              }
          }
          return;
      }
      
      
      
      // Make sure we have a docid and datafile
      if (docid != null && fileList.containsKey("datafile")) 
      {
        logMetacat.info("MetacatHandler.handleInsertMultipartAction - " +
        		        "Uploading data docid: " + docid);
        // Get a reference to the file part of the form
        //FilePart filePart = (FilePart) fileList.get("datafile");
        String fileName = fileList.get("filename");
        logMetacat.debug("MetacatHandler.handleInsertMultipartAction - " +
        		         "Uploading filename: " + fileName);
        // Check if the right file existed in the uploaded data
        if (fileName != null) 
        {
              
          try 
          {
              //logMetacat.info("Upload datafile " + docid
              // +"...", 10);
              //If document get lock data file grant
            if (DocumentImpl.getDataFileLockGrant(docid)) 
            {
              // Save the data file to disk using "docid" as the name
              String datafilepath = PropertyService.getProperty("application.datafilepath");
              File dataDirectory = new File(datafilepath);
              dataDirectory.mkdirs();
              File newFile = null;
    //          File tempFile = null;
              String tempFileName = fileList.get("name");
              String newFileName = dataDirectory + File.separator + docid;
              long size = 0;
              boolean fileExists = false;
                      
              try 
              {
                newFile = new File(newFileName);
                fileExists = newFile.exists();
                logMetacat.info("MetacatHandler.handleInsertMultipartAction - " +
                		        "new file status is: " + fileExists);
                if(fileExists)
                {
                  newFile.delete();
                  newFile.createNewFile();
                  fileExists = false;
                }
                
                if ( fileExists == false ) 
                {
                    // copy file to desired output location
                    try 
                    {
                        MetacatUtil.copyFile(tempFileName, newFileName);
                    } 
                    catch (IOException ioe) 
                    {
                        logMetacat.error("MetacatHandler.handleInsertMultipartAction - " +
                        		         "IO Exception copying file: " +
                                         ioe.getMessage());
                        ioe.printStackTrace(System.out);
                    }
                    size = newFile.length();
                    if (size == 0) 
                    {
                        throw new IOException("MetacatHandler.handleInsertMultipartAction - " +
                        		              "Uploaded file is 0 bytes!");
                    }
                }
                logMetacat.info("MetacatHandler.handleInsertMultipartAction - " +
                		        "Uploading the following to Metacat:" +
                                fileName + ", " + docid + ", " +
                                username + ", " + groupnames);
                
                // Read the file with appropriate encoding
                XmlStreamReader xsr = new XmlStreamReader(new FileInputStream(newFile));
                String encoding = xsr.getEncoding();
                Reader fr = new InputStreamReader(new FileInputStream(newFile), encoding);
                
                char[] c = new char[1024];
                int numread = fr.read(c, 0, 1024);
                StringBuffer sb = new StringBuffer();
                while(numread != -1)
                {
                  sb.append(c, 0, numread);
                  numread = fr.read(c, 0, 1024);
                }
                
                Enumeration<String> keys = params.keys();
                while(keys.hasMoreElements())
                { //convert the params to arrays
                  String key = keys.nextElement();
                  String[] paramValue = params.get(key);
                  String[] s = new String[1];
                  s[0] = paramValue[0];
                  params.put(key, s);
                }
                //add the doctext to the params
                String doctext = sb.toString();
                String[] doctextArr = new String[1];
                doctextArr[0] = doctext;
                params.put("doctext", doctextArr);
                boolean writeAccessRules = true;
                //call the insert routine
                handleInsertOrUpdateAction(request.getRemoteAddr(), request.getHeader("User-Agent"), response, out, 
                          params, username, groupnames, true, writeAccessRules);
              }
              catch(Exception e)
              {
                throw e;
              }
            }
          }
          catch(Exception e)
          {
              logMetacat.error("MetacatHandler.handleInsertMultipartAction - " +
            		           "error uploading text file via multipart: " + 
            		           e.getMessage());
              e.printStackTrace(System.out);;
          }
        }
      }
    }
    
    /**
     * Handle the upload action by saving the attached file to disk and
     * registering it in the Metacat db
     */
    private void handleUploadAction(HttpServletRequest request,
            PrintWriter out, Hashtable<String, String[]> params, 
            Hashtable<String,String> fileList, String username, String[] groupnames, 
            HttpServletResponse response) {
        Logger logMetacat = Logger.getLogger(MetacatHandler.class);
        //PrintWriter out = null;
        //Connection conn = null;
        //        String action = null;
        String docid = null;
        String qformat = null;
        String output = "";

        /*
         * response.setContentType("text/xml"); try { out =
         * response.getWriter(); } catch (IOException ioe2) {
         * System.err.println("Fatal Error: couldn't get response output
         * stream.");
         */

        if (params.containsKey("docid")) {
            docid = params.get("docid")[0];
        }
        
        Identifier identifier = new Identifier();
        identifier.setValue(docid);
        if(!D1NodeService.isValidIdentifier(identifier)) {
            output += this.PROLOG;
            output += this.ERROR;
            output += "The docid "+docid +" is not valid since it is null or contians the white space(s).";
            output += this.ERRORCLOSE;
            logMetacat.warn("MetacatHandler.handleUploadAction - " +
                            "The docid "+docid +" is not valid since it is null or contians the white space(s).");
        } else {
         // Make sure we have a docid and datafile
            if (docid != null && fileList.containsKey("datafile")) {
                logMetacat.info("MetacatHandler.handleUploadAction - " +
                                "Uploading data docid: " + docid);
                // Get a reference to the file part of the form
                //FilePart filePart = (FilePart) fileList.get("datafile");
                String fileName = fileList.get("filename");
                logMetacat.info("MetacatHandler.handleUploadAction - " +
                                "Uploading filename: " + fileName);
                // Check if the right file existed in the uploaded data
                if (fileName != null) {

                    try {
                        //logMetacat.info("Upload datafile " + docid
                        // +"...", 10);
                        //If document get lock data file grant
                        if (DocumentImpl.getDataFileLockGrant(docid)) {
                            // Save the data file to disk using "docid" as the name
                            String datafilepath = PropertyService.getProperty("application.datafilepath");
                            File dataDirectory = new File(datafilepath);
                            dataDirectory.mkdirs();
                            File newFile = null;
                            //                    File tempFile = null;
                            String tempFileName = fileList.get("name");
                            String newFileName = dataDirectory + File.separator + docid;
                            long size = 0;
                            boolean fileExists = false;

                            try {
                                newFile = new File(newFileName);
                                fileExists = newFile.exists();
                                logMetacat.info("MetacatHandler.handleUploadAction - " +
                                                "new file status is: " + fileExists);
                                if ( fileExists == false ) {
                                    // copy file to desired output location
                                    try {
                                        MetacatUtil.copyFile(tempFileName, newFileName);
                                    } catch (IOException ioe) {
                                        logMetacat.error("IO Exception copying file: " +
                                                ioe.getMessage());
                                        ioe.printStackTrace(System.out);
                                    }
                                    size = newFile.length();
                                    if (size == 0) {
                                        throw new IOException("Uploaded file is 0 bytes!");
                                    }
                                } // Latent bug here if the file already exists, then the
                                  // conditional fails but the document is still registered.
                                  // maybe this never happens because we already requested a lock?
                                logMetacat.info("MetacatHandler.handleUploadAction - " +
                                                "Uploading the following to Metacat:" +
                                                fileName + ", " + docid + ", " +
                                                username + ", " + groupnames);
                                //register the file in the database (which generates
                                // an exception
                                //if the docid is not acceptable or other untoward
                                // things happen
                                DocumentImpl.registerDocument(fileName, "BIN", docid,
                                        username, groupnames);
                                
                                // generate system metadata about the doc
                                SystemMetadata sm = SystemMetadataFactory.createSystemMetadata(docid, false, false);
                                
                                // manage it in the store
                                HazelcastService.getInstance().getSystemMetadataMap().put(sm.getIdentifier(), sm);
                                
                                // submit for indexing
                                MetacatSolrIndex.getInstance().submit(sm.getIdentifier(), sm, null);
                                
                            } catch (Exception ee) {
                                // If the file did not exist before this method was 
                                // called and an exception is generated while 
                                // creating or registering it, then we want to delete
                                // the file from disk because the operation failed.
                                // However, if the file already existed before the 
                                // method was called, then the exception probably
                                // occurs when registering the document, and so we
                                // want to leave the old file in place.
                                if ( fileExists == false ) {
                                    newFile.delete();
                                }
                                
                                logMetacat.info("MetacatHandler.handleUploadAction - " +
                                                "in Exception: fileExists is " + fileExists);
                                logMetacat.error("MetacatHandler.handleUploadAction - " +
                                                 "Upload Error: " + ee.getMessage());
                                throw ee;
                            }

                            EventLog.getInstance().log(request.getRemoteAddr(), request.getHeader("User-Agent"),
                                    username, docid, "upload");
                            // Force replication this data file
                            // To data file, "insert" and update is same
                            // The fourth parameter is null. Because it is
                            // notification server
                            // and this method is in MetaCatServerlet. It is
                            // original command,
                            // not get force replication info from another metacat
                            ForceReplicationHandler frh = new ForceReplicationHandler(
                                    docid, "insert", false, null);
                            logMetacat.debug("MetacatHandler.handleUploadAction - " +
                                             "ForceReplicationHandler created: " + 
                                             frh.toString());

                            // set content type and other response header fields
                            // first
                            output += "<?xml version=\"1.0\"?>";
                            output += "<success>";
                            output += "<docid>" + docid + "</docid>";
                            output += "<size>" + size + "</size>";
                            output += "</success>";
                        }

                    } catch (Exception e) {

                        output += "<?xml version=\"1.0\"?>";
                        output += "<error>";
                        output += e.getMessage();
                        output += "</error>";
                    }
                } else {
                    // the field did not contain a file
                    output += "<?xml version=\"1.0\"?>";
                    output += "<error>";
                    output += "The uploaded data did not contain a valid file.";
                    output += "</error>";
                }
            } else {
                // Error bcse docid missing or file missing
                output += "<?xml version=\"1.0\"?>";
                output += "<error>";
                output += "The uploaded data did not contain a valid docid "
                    + "or valid file.";
                output += "</error>";
            }
        }


        if(params.containsKey("qformat")) {
            qformat = params.get("qformat")[0];
        }
        
        if (qformat == null || qformat.equals("xml")) {
            response.setContentType("text/xml");
            out.println(output);
        } else {
            try {
                DBTransform trans = new DBTransform();
                response.setContentType("text/html");
                trans.transformXMLDocument(output,
                        "message", "-//W3C//HTML//EN", qformat,
                        out, null, null);
            } catch (Exception e) {

                logMetacat.error("MetacatHandler.handleUploadAction - General error: "
                        + e.getMessage());
                e.printStackTrace(System.out);
            }
        }
    }
    
    /*
     * A method to handle set access action
     */
    protected void handleSetAccessAction(PrintWriter out, Hashtable<String, String[]> params,
            String username, HttpServletRequest request, HttpServletResponse response) {
        
        String permission = null;
        String permType = null;
        String permOrder = null;
        Vector<String> errorList = new Vector<String>();
        String error = null;
        Vector<String> successList = new Vector<String>();
        String success = null;
        boolean isEmlPkgMember = false;
        
		SystemMetadata mnSysMeta = null;
		Session session = null;
		Identifier pid = new Identifier();
        String guid = null;
		AccessPolicy mnAccessPolicy = null;
		SystemMetadata cnSysMeta = null;
        
        String[] docList = params.get("docid");
        String[] principalList = params.get("principal");
        String[] permissionList = params.get("permission");
        String[] permTypeList = params.get("permType");
        String[] permOrderList = params.get("permOrder");
        String[] qformatList = params.get("qformat");
        String[] accessBlock = params.get("accessBlock");
        if(accessBlock != null) {
            if (docList == null) {
                errorList.addElement("MetacatHandler.handleSetAccessAction - " +
                		             "Doc id missing.  Please check your " +
                		             "parameter list, it should look like: " + 
                		             "?action=setaccess&docid=<doc_id>&accessBlock=<access_section>");
                outputResponse(successList, errorList, out);
                return;
            } else {
                // look-up pid assuming docid
                guid = docList[0];
                try {
   	             String docid = DocumentUtil.getDocIdFromAccessionNumber(docList[0]);
   	             int rev = DocumentUtil.getRevisionFromAccessionNumber(docList[0]);
   	             guid = IdentifierManager.getInstance().getGUID(docid, rev);
               	 logMetacat.debug("Setting access on found pid: " + guid);
                } catch (McdbDocNotFoundException e) {
               	 // log the warning
               	 logMetacat.warn("No pid found for [assumed] docid: " + docList[0]);
                } catch (Exception e) {
                	logMetacat.warn("Error looking up pid for [assumed] dociid: " + docList[0]);
                }
            }
            try {
            	
              	logMetacat.debug("Setting access for docid: " + docList[0]);
                AccessControlForSingleFile accessControl = 
                    new AccessControlForSingleFile(docList[0]);
                accessControl.insertPermissions(accessBlock[0]);
                successList.addElement("MetacatHandler.handleSetAccessAction - " +
                		               "successfully replaced access block for doc id: " + 
                		               docList[0]);
                
                // force hazelcast to update system metadata
                HazelcastService.getInstance().refreshSystemMetadataEntry(guid);
         
                // Update the CN with the modified access policy
                logMetacat.debug("Setting CN access policy for pid: " + guid);

    			try {
    				ArrayList<String> guids = new ArrayList<String>(Arrays.asList(guid));
    				SyncAccessPolicy syncAP = new SyncAccessPolicy();

    				logMetacat.debug("Trying to syncing access policy for pid: "
    						+ guid);
    				syncAP.sync(guids);
    			} catch (Exception e) {
    				logMetacat.error("Error syncing pid: " + guid
    						+ " Exception " + e.getMessage());
                    e.printStackTrace(System.out);
    			}
            } catch(AccessControlException ace) {
                errorList.addElement("MetacatHandler.handleSetAccessAction - " +
                		             "access control error when setting " + 
                                     "access block: " + ace.getMessage());
            }
            outputResponse(successList, errorList, out);
            return;
        }
        
        // Make sure the parameter is not null
        if (docList == null || principalList == null || permTypeList == null
                || permissionList == null) {
            error = "MetacatHandler.handleSetAccessAction - Please check " +
                    "your parameter list, it should look like: "
                    + "?action=setaccess&docid=pipeline.1.1&principal=public"
                    + "&permission=read&permType=allow&permOrder=allowFirst";
            errorList.addElement(error);
            outputResponse(successList, errorList, out);
            return;
        }
        
        // Only select first element for permission, type and order
        permission = permissionList[0];
        permType = permTypeList[0];
        System.out.println("permission in MetacatHandler.handleSetAccessAction: " + permission);
        
        if (permOrderList != null) {
            permOrder = permOrderList[0];
        }
        
        // Get package doctype set
        Vector<String> packageSet = null;
        try {
            packageSet = 
                MetacatUtil.getOptionList(PropertyService.getProperty("xml.packagedoctypeset"));
        } catch (PropertyNotFoundException pnfe) {
            logMetacat.error("MetacatHandler.handleSetAccessAction - " +
            		         "Could not find package doctype set.  Setting to null: " 
                    + pnfe.getMessage());
        }
        //debug
        if (packageSet != null) {
            for (int i = 0; i < packageSet.size(); i++) {
                logMetacat.debug("MetacatHandler.handleSetAccessAction - " +
                		         "doctype in package set: " + 
                		         packageSet.elementAt(i));
            }
        }
        
        // handle every accessionNumber
        for (int i = 0; i < docList.length; i++) {
            String docid = docList[i];

            if (docid.startsWith("urn:")) {
                try {
                    String actualDocId = LSIDUtil.getDocId(docid, false);
                    docid = actualDocId;
                } catch (ParseLSIDException ple) {
                    logMetacat.error("MetacatHandler.handleSetAccessAction - " +
                            "could not parse lsid: " + docid + " : " + ple.getMessage()); 
                    ple.printStackTrace(System.out);
                }
            }
            String accessionNumber = docid;
            String owner = null;
            String publicId = null;
            // Get document owner and public id
            try {
                owner = getFieldValueForDoc(accessionNumber, "user_owner");
                publicId = getFieldValueForDoc(accessionNumber, "doctype");
            } catch (Exception e) {
                logMetacat.error("MetacatHandler.handleSetAccessAction - " +
                		         "Error in handleSetAccessAction: " + 
                		         e.getMessage());
                e.printStackTrace(System.out);
                error = "Error in set access control for document - " + 
                        accessionNumber + e.getMessage();
                errorList.addElement(error);
                continue;
            }
            //check if user is the owner. Only owner can do owner
            if (username == null || owner == null || !username.equals(owner)) {
                error = "User - " + username + " does not have permission to set "
                        + "access control for docid - " + accessionNumber;
                errorList.addElement(error);
                System.out.println("user " + username + " does not have permission to set " + 
                        "access control for docid " + accessionNumber);
                continue;
            }
            
            //*** Find out if the document is a Member of an EML package.
            //*** (for efficiency, only check if it isn't already true
            //*** for the case of multiple files).
            if (isEmlPkgMember == false)
                isEmlPkgMember = (DBUtil.findDataSetDocIdForGivenDocument(accessionNumber) != null);
            
            // If docid publicid is BIN data file or other beta4, 6 package document
            // we could not do set access control. Because we don't want inconsistent
            // to its access docuemnt
            if (publicId != null && packageSet != null
                    && packageSet.contains(publicId) && isEmlPkgMember) {
                error = "Could not set access control to document " + accessionNumber
                        + "because it is in a pakcage and it has a access file for it";
                errorList.addElement(error);
                System.out.println("this is a beta4 or 6 document so we can't set the access element.");
                continue;
            }
            // for every principle
            for (int j = 0; j < principalList.length; j++) {
                String principal = principalList[j];
                try {
                    //insert permission
                    AccessControlForSingleFile accessControl = 
                        new AccessControlForSingleFile(accessionNumber);
                    //System.out.println("permission in MetacatHandler: " + 
                    //                   l.longValue());
                    //System.out.println("permission in MetacatHandler: " + 
                    //                   Integer.valueOf(AccessControlList.intValue(permission)).longValue());
                    accessControl.insertPermissions(principal, 
                      Integer.valueOf(AccessControlList.intValue(permission)).longValue(), 
                      permType, permOrder, null, null);
                    
                    // refresh using guid
                    guid = accessionNumber;
                    try {
          	             String tempDocid = DocumentUtil.getDocIdFromAccessionNumber(accessionNumber);
          	             int rev = DocumentUtil.getRevisionFromAccessionNumber(accessionNumber);
          	             guid = IdentifierManager.getInstance().getGUID(tempDocid, rev);
                      	 logMetacat.debug("Found pid: " + guid);
                    } catch (Exception e) {
                       	logMetacat.warn("Error looking up pid for [assumed] docid: " + accessionNumber);
                    }
                    
            		// force hazelcast to refresh system metadata
                    HazelcastService.getInstance().refreshSystemMetadataEntry(guid);
                    
                    logMetacat.debug("Synching CN access policy for pid: " + guid);

        			try {
        				ArrayList<String> guids = new ArrayList<String>(Arrays.asList(guid));
        				SyncAccessPolicy syncAP = new SyncAccessPolicy();
        				logMetacat.debug("Trying to syncing access policy for pid: " + guid);
        				syncAP.sync(guids);
        			} catch (Exception e) {
        				logMetacat.error("Error syncing pids: " + guid
        						+ " Exception " + e.getMessage(), e);
        			}
                } catch (Exception ee) {
                    logMetacat.error("MetacatHandler.handleSetAccessAction - " +
                    		         "Error inserting permission: " + 
                    		         ee.getMessage(), ee);
                    error = "Failed to set access control for document "
                            + accessionNumber + " because " + ee.getMessage();
                    errorList.addElement(error);
                    continue;
                }
            }
            

            //force replication when this action is called
            boolean isXml = true;
            if (publicId.equalsIgnoreCase("BIN")) {
                isXml = false;
            }
            ForceReplicationHandler frh = 
                new ForceReplicationHandler(accessionNumber, isXml, null);
            logMetacat.debug("MetacatHandler.handleSetAccessAction - " +
            		         "ForceReplicationHandler created: " + frh.toString());
            
        }
        if (errorList.isEmpty()) {
        	successList.addElement("MetacatHandler.handleSetAccessAction - " +
        			               "successfully added individual access for doc id: " + 
        			               docList[0]);
        }
        if (params.get("forwardto")  != null) {
            try {
                RequestUtil.forwardRequest(request, response, params);
            } catch (MetacatUtilException mue) {
                logMetacat.error("metaCatServlet.handleSetAccessAction - could not forward " +
                        "request. Sending output to response writer");
                mue.printStackTrace(System.out);
                outputResponse(successList, errorList, out);
            }               
        } else {
            outputResponse(successList, errorList, out);
        }
    }
    
    /*
     * A method try to determin a docid's public id, if couldn't find null will
     * be returned.
     */
    private String getFieldValueForDoc(String accessionNumber, String fieldName)
    throws Exception {
        if (accessionNumber == null || accessionNumber.equals("")
        || fieldName == null || fieldName.equals("")) { throw new Exception(
                "Docid or field name was not specified"); }
        
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        String fieldValue = null;
        String docId = null;
        DBConnection conn = null;
        int serialNumber = -1;
        
        // get rid of revision if access number has
        docId = DocumentUtil.getDocIdFromString(accessionNumber);
        try {
            //check out DBConnection
            conn = DBConnectionPool
                    .getDBConnection("MetacatHandler.getPublicIdForDoc");
            serialNumber = conn.getCheckOutSerialNumber();
            pstmt = conn.prepareStatement("SELECT " + fieldName
                    + " FROM xml_documents " + "WHERE docid = ? ");
            
            pstmt.setString(1, docId);
            pstmt.execute();
            rs = pstmt.getResultSet();
            boolean hasRow = rs.next();
        //    int perm = 0;
            if (hasRow) {
                fieldValue = rs.getString(1);
            } else {
                throw new Exception("Could not find document: "
                        + accessionNumber);
            }
        } catch (Exception e) {
            logMetacat.error("MetacatHandler.getFieldValueForDoc - General error: "
                    + e.getMessage());
            throw e;
        } finally {
            try {
                rs.close();
                pstmt.close();
                
            } finally {
                DBConnectionPool.returnDBConnection(conn, serialNumber);
            }
        }
        return fieldValue;
    }
    
    /*
     * Get the list of documents from the database and return the list in an
     * Vector of identifiers.
     *
     * @ returns the array of identifiers
     */
    private Vector<String> getDocumentList() throws SQLException {
        Logger logMetacat = Logger.getLogger(MetacatHandler.class);
        Vector<String> docList = new Vector<String>();
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        DBConnection conn = null;
        int serialNumber = -1;
        
        try {
            //check out DBConnection
            conn = DBConnectionPool
                    .getDBConnection("MetacatHandler.getDocumentList");
            serialNumber = conn.getCheckOutSerialNumber();
            pstmt = conn.prepareStatement("SELECT docid, rev"
                    + " FROM xml_documents ");
            pstmt.execute();
            rs = pstmt.getResultSet();
            while (rs.next()) {
                String docid = rs.getString(1);
                String rev = rs.getString(2);
                docList.add(docid + "." + rev);
            }
        } catch (SQLException e) {
            logMetacat.error("MetacatHandler.getDocumentList - General exception: "
                    + e.getMessage());
            throw e;
        } finally {
            try {
                rs.close();
                pstmt.close();
                
            } catch (SQLException se) {
                logMetacat.error("MetacatHandler.getDocumentList - General exception: "
                        + se.getMessage());
                throw se;
            } finally {
                DBConnectionPool.returnDBConnection(conn, serialNumber);
            }
        }
        return docList;
    }
    
    /*
     * A method to output setAccess action result
     */
    private void outputResponse(Vector<String> successList, Vector<String> errorList,
            Writer out) {
    	try {
	        boolean error = false;
	        boolean success = false;
	        // Output prolog
	        out.write(PROLOG);
	        // output success message
	        if (successList != null) {
	            for (int i = 0; i < successList.size(); i++) {
	                out.write(SUCCESS);
	                out.write(successList.elementAt(i));
	                out.write(SUCCESSCLOSE);
	                success = true;
	            }
	        }
	        // output error message
	        if (errorList != null) {
	            for (int i = 0; i < errorList.size(); i++) {
	                out.write(ERROR);
	                out.write(errorList.elementAt(i));
	                out.write(ERRORCLOSE);
	                error = true;
	            }
	        }
	        
	        // if no error and no success info, send a error that nothing happened
	        if (!error && !success) {
	            out.write(ERROR);
	            out.write("Nothing happend for setaccess action");
	            out.write(ERRORCLOSE);
	        }
    	} catch (Exception e) {
			logMetacat.error(e.getMessage(), e);
		} 
    }
    
    /**
     * Schedule the sitemap generator to run periodically and update all
     * of the sitemap files for search indexing engines.
     *
     * @param request a servlet request, from which we determine the context
     */
    protected void scheduleSitemapGeneration(HttpServletRequest request) {
        if (!_sitemapScheduled) {
            String directoryName = null;
            String skin = null;
            long sitemapInterval = 0;
            
            try {
                directoryName = SystemUtil.getContextDir() + FileUtil.getFS() + "sitemaps";
                skin = PropertyService.getProperty("application.default-style");
                sitemapInterval = 
                    Integer.parseInt(PropertyService.getProperty("sitemap.interval"));
            } catch (PropertyNotFoundException pnfe) {
                logMetacat.error("MetacatHandler.scheduleSitemapGeneration - " +
                		         "Could not run site map generation because property " +
                		         "could not be found: " + pnfe.getMessage());
            }
            
            File directory = new File(directoryName);
            directory.mkdirs();
            String urlRoot = request.getRequestURL().toString();
            Sitemap smap = new Sitemap(directory, urlRoot, skin);
            long firstDelay = 60*1000;   // 60 seconds delay
            timer.schedule(smap, firstDelay, sitemapInterval);
            _sitemapScheduled = true;
        }
    }

    /**
     * @param sitemapScheduled toggle the _sitemapScheduled flag
     */
    public void set_sitemapScheduled(boolean sitemapScheduled) {
        _sitemapScheduled = sitemapScheduled;
    }

    
}
