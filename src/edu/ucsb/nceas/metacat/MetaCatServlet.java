package edu.ucsb.nceas.metacat;


import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.Hashtable;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.ucsb.nceas.metacat.database.DBConnectionPool;
import edu.ucsb.nceas.metacat.service.SessionService;
import edu.ucsb.nceas.metacat.util.RequestUtil;
import edu.ucsb.nceas.metacat.util.SessionData;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;


/**
 * A metadata catalog server implemented as a Java Servlet
 * All actions are disabled since Metacat 3.0.0
 */
public class MetaCatServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private MetacatHandler handler = null;
    private static Log logMetacat = LogFactory.getLog(MetaCatServlet.class);

    // Constants -- these should be final in a servlet
    public static final String SCHEMALOCATIONKEYWORD = ":schemaLocation";
    public static final String NONAMESPACELOCATION = ":noNamespaceSchemaLocation";
    public static final String EML2KEYWORD = ":eml";
    public static final String DEFAULT_ENCODING = "UTF-8";

    /**
     * Initialize the servlet. 
     * The job of initializing Metacat is delegated to the MetacatInitializer class 
     */
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        // Initialize Metacat Handler
       handler = new MetacatHandler();
    }

    /**
     * Destroy the servlet
     */
    public void destroy() {

    }
    
    /** Handle "GET" method requests from HTTP clients */
    public void doGet(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
        // Process the data and send back the response
        handleGetOrPost(request, response);
    }

    /** Handle "POST" method requests from HTTP clients */
    public void doPost(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
        // Process the data and send back the response
        handleGetOrPost(request, response);
    }

    /**
     * Control servlet response depending on the action parameter specified
     */
    @SuppressWarnings("unchecked")
    private void handleGetOrPost(HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException {
        String requestEncoding = request.getCharacterEncoding();
        if (requestEncoding == null) {
            logMetacat.debug("null requestEncoding, setting to application default: "
                                + DEFAULT_ENCODING);
            request.setCharacterEncoding(DEFAULT_ENCODING);
        }
        logMetacat.debug("requestEncoding: " + requestEncoding);
        
        // Update the last update time for this user if they are not new
        HttpSession httpSession = request.getSession(false);
        if (httpSession != null) {
            SessionService.getInstance().touchSession(httpSession.getId());
        }
        
        // If all DBConnection in the pool are free and DBConnection pool
        // size is greater than initial value, shrink the connection pool
        // size to initial value
        DBConnectionPool.shrinkDBConnectionPoolSize();

        // Debug message to print out the method which have a busy DBConnection
        try {
            @SuppressWarnings("unused")
            DBConnectionPool pool = DBConnectionPool.getInstance();
//            pool.printMethodNameHavingBusyDBConnection();
        } catch (SQLException e) {
            logMetacat.error("MetaCatServlet.handleGetOrPost - Error in "
                                + "MetacatServlet.handleGetOrPost: " + e.getMessage());
            e.printStackTrace();
        }

        try {
            String ctype = request.getContentType();
            
            if (ctype != null && ctype.startsWith("multipart/form-data")) {
                handler.handleMultipartForm(request, response);
                return;
            } 

            String name = null;
            String[] value = null;
            String[] docid = new String[3];
            Hashtable<String, String[]> params = new Hashtable<String, String[]>();

            // Check if this is a simple read request that doesn't use the
            // "action" syntax
            // These URLs are of the form:
            // http://localhost:8180/metacat/metacat/docid/skinname
            // e.g., http://localhost:8180/metacat/metacat/test.1.1/knb
            String pathInfo = request.getPathInfo();
            if (pathInfo != null) {
                // otherwise carry on as usual
                handler.handleReadAction(params, request, response, "public", null, null);
                return;
            }

            Enumeration<String> paramlist =
                (Enumeration<String>) request.getParameterNames();
            while (paramlist.hasMoreElements()) {

                name = paramlist.nextElement();
                value = request.getParameterValues(name);

                // Decode the docid and mouse click information
                // THIS IS OBSOLETE -- I THINK -- REMOVE THIS BLOCK
                // 4/12/2007d
                // MBJ
                if (name.endsWith(".y")) {
                    docid[0] = name.substring(0, name.length() - 2);
                    params.put("docid", docid);
                    name = "ypos";
                }
                if (name.endsWith(".x")) {
                    name = "xpos";
                }

                params.put(name, value);
            }

            // handle param is emptpy
            if (params.isEmpty() || params == null) {
                handler.sendNotSupportMessage(response);
                return;
            }

            // if the user clicked on the input images, decode which image
            // was clicked then set the action.
            if (params.get("action") == null) {
                handler.sendNotSupportMessage(response);
                return;
            }

            String action = (params.get("action"))[0];
            logMetacat.info("MetaCatServlet.handleGetOrPost - Action is: " + action);

            // This block handles session management for the servlet
            // by looking up the current session information for all actions
            // other than "login" and "logout"
            String userName = null;
            String password = null;
            String[] groupNames = null;
            String sessionId = null;
            name = null;

            // handle login action
            if (action.equals("login")) {
                handler.handleLoginAction(params, request, response);
                // handle logout action
            } else if (action.equals("logout")) {
                handler.handleLogoutAction(params, request, response);
                // handle session validate request
            } else if (action.equals("validatesession")) {
                 handler.sendNotSupportMessage(response);
                // aware of session expiration on every request
            } else {
                SessionData sessionData = RequestUtil.getSessionData(request);
                if (sessionData != null) {
                    userName = sessionData.getUserName();
                    password = sessionData.getPassword();
                    groupNames = sessionData.getGroupNames();
                    sessionId = sessionData.getId();
                }

                logMetacat.info("MetaCatServlet.handleGetOrPost - The user is : " + userName);
            }

            // Now that we know the session is valid, we can delegate the
            // request to a particular action handler
            if (action.equals("query")) {
                handler.handleQuery(params, response, userName, groupNames, sessionId);
            } else if (action.equals("squery")) {
                handler.handleSQuery(params, response, userName, groupNames, sessionId);
            } else if (action.trim().equals("spatial_query")) {
                handler.handleSpatialQuery(params, response, userName, groupNames, sessionId);
            } else if (action.trim().equals("dataquery")) {
                handler.handleDataquery(params, response, sessionId);
            } else if (action.trim().equals("editcart")) {
                handler.handleEditCart(params, response, sessionId);
            } else if (action.equals("export")) {

                handler.handleExportAction(params, response, userName, groupNames, password);
            } else if (action.equals("read")) {
                handler.handleReadAction(params, request, response, userName, password,
                            groupNames);

            } else if (action.equals("readinlinedata")) {
                handler.handleReadInlineDataAction(params, request, response, userName, password,
                        groupNames);
            } else if (action.equals("insert") || action.equals("update")) {
               handler.sendNotSupportMessage(response);
            } else if (action.equals("delete")) {
                    handler.handleDeleteAction(params, request, response, userName,
                            groupNames);
            } else if (action.equals("validate")) {
                handler.handleValidateAction(response, params);
            } else if (action.equals("setaccess")) {
                handler.handleSetAccessAction(params, userName, request, response);
            } else if (action.equals("getaccesscontrol")) {
                handler.handleGetAccessControlAction(params, response, userName, groupNames);
            } else if (action.equals("isauthorized")) {
                handler.sendNotSupportMessage(response);
            } else if (action.equals("getprincipals")) {
                handler.handleGetPrincipalsAction(response, userName, password);
            } else if (action.equals("getdoctypes")) {
                handler.handleGetDoctypesAction(params, response);
            } else if (action.equals("getdtdschema")) {
                handler.handleGetDTDSchemaAction(params, response);
            } else if (action.equals("getdocid")) {
                handler.handleGetDocid(params, response);
            } else if (action.equals("getlastdocid")) {
                handler.handleGetMaxDocidAction(params, response);
            } else if (action.equals("getalldocids")) {
                handler.handleGetAllDocidsAction(params, response);
            } else if (action.equals("isregistered")) {
                handler.handleIdIsRegisteredAction(params, response);
            } else if (action.equals("getrevisionanddoctype")) {
                handler.handleGetRevisionAndDocTypeAction(response, params);
            } else if (action.equals("getversion")) {
                handler.sendNotSupportMessage(response);
            } else if (action.equals("getlog")) {
                handler.handleGetLogAction(params, request, response, userName,
                                                    groupNames, sessionId);
            } else if (action.equals("getloggedinuserinfo")) {
                handler.sendNotSupportMessage(response);
            } else if (action.equals("buildindex")) {
                handler.handleBuildIndexAction(params, request, response, userName, groupNames);
            } else if (action.equals("reindex")) {
                handler.sendNotSupportMessage(response);
            } else if (action.equals("reindexall")) {
                handler.sendNotSupportMessage(response);
            } else if (action.equals("refreshServices")) {
                handler.sendNotSupportMessage(response);
            } else if (action.equals("shrink")) {
                handler.sendNotSupportMessage(response);
            } else {
                handler.sendNotSupportMessage(response);
            }
        } catch (PropertyNotFoundException pnfe) {
            String errorString = "Critical property not found: " + pnfe.getMessage();
            logMetacat.error("MetaCatServlet.handleGetOrPost - " + errorString);
            throw new ServletException(errorString);
        } 
    }

    /**
     * Check if the Metacat instance is in ready-only mode
     * @param response  the response will be used to write back message
     * @return true if it is in read-only mode; false otherwise.
     * @throws IOException
     */
    public static boolean isReadOnly(HttpServletResponse response) throws IOException {
        boolean readOnly = false;
        ReadOnlyChecker checker = new ReadOnlyChecker();
        readOnly = checker.isReadOnly();
        if(readOnly) {
            PrintWriter out = response.getWriter();
            response.setContentType("text/xml");
            out.println("<?xml version=\"1.0\"?>");
            out.println("<error>");
            out.println("Metacat is in read-only mode and your request can't be fulfilled. "
                                + "Please try again later.");
            out.println("</error>");
            out.close();
        }
        return readOnly;
    }

}
