package edu.ucsb.nceas.metacat.index;



import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.dataone.cn.indexer.IndexWorker;
import org.dataone.configuration.Settings;


/**
 * A servlet class for the Metadata Index module. This class only does one thing - initialize the ApplicationController class.
 * @author tao
 *
 */
public class MetacatIndexServlet extends HttpServlet {
    private static Log log = LogFactory.getLog(MetacatIndexServlet.class);

    /**
     * Initialize the servlet
     */
    public void init(ServletConfig config) throws ServletException {
        try {
            //add the metacat.properties file as the dataone indexer property file
            String metacatPropertiesFilePath = config.getServletContext()
                                        .getInitParameter("metacat.properties.path");
            File contextDeploymentDir = new File(config.getServletContext().getRealPath("/"));
            String fullMetacatPropertiesFilePath = contextDeploymentDir.getParent() 
                                                                    + metacatPropertiesFilePath;
            log.debug("MetacatIndexServlet.init - The fullMetacatPropertiesFilePath is " 
                                                                + fullMetacatPropertiesFilePath);
            IndexWorker.loadExternalPropertiesFile(fullMetacatPropertiesFilePath);
            //load the site property file
            // metacatSitePropertiesFile is outside the tomcat webapps dir, so it should be available
            Path metacatSitePropertiesFilePath = Paths.get(
                Settings.getConfiguration().getString("application.sitePropertiesDir"),
                "metacat-site.properties");
            if (!metacatSitePropertiesFilePath.toFile().exists()) {
                String errorMsg =
                    "Could not find Metacat site properties at: " + metacatSitePropertiesFilePath;
                log.error(errorMsg);
                throw new IOException(errorMsg);
            }
            IndexWorker.loadAdditionalPropertyFile(metacatSitePropertiesFilePath.toString());
            IndexWorker worker = new IndexWorker();
            worker.start();
        } catch (Exception e) {
            throw new ServletException(e.getMessage());
        }
    }
    
    /**
     *Actions needed to be done before close the servlet
     */
    public void destroy() {
       //don nothing
    }
    
    /** Handle "GET" method requests from HTTP clients */
    public void doGet(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
        //do nothing
      
    }
    
    /** Handle "POST" method requests from HTTP clients */
    public void doPost(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
        //do nothing
      
    }
}
