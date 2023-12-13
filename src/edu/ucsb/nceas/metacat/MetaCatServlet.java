package edu.ucsb.nceas.metacat;


import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


/**
 * A metadata catalog server implemented as a Java Servlet
 * All actions are disabled since Metacat 3.0.0
 */
public class MetaCatServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private MetacatHandler handler = null;
    private static Log logMetacat = LogFactory.getLog(MetaCatServlet.class);

    // Constants -- these should be final in a servlet
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
        handler.sendNotSupportMessage(response);
    }

}
