/**
 *  Copyright: 2013 Regents of the University of California and the
 *             National Center for Ecological Analysis and Synthesis
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
package edu.ucsb.nceas.metacat.index;



import java.io.File;
import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


/**
 * A servlet class for the Metadata Index module. This class only does one thing - initialize the ApplicationController class.
 * @author tao
 *
 */
public class MetacatIndexServlet extends HttpServlet {
    
    // Use the file prefix to indicate this is a absolute path.
    // see http://www.docjar.com/docs/api/org/springframework/context/support/FileSystemXmlApplicationContext.html
    //private static final String FILEPREFIX = "file:";
    
	private static Log log = LogFactory.getLog(MetacatIndexServlet.class);

    /**
     * Initialize the servlet 
     */
    public void init(ServletConfig config) throws ServletException {
        //System.out.println("++++++++++++++++++++++++------------------- start the servlet");
    	//initializeSharedConfiguration(config);
    	// initialize the application using the configured application-context
        //URL url = getClass().getResource("/index-processor-context.xml");
        //find the sibling metacat.properties file
        String metacatPropertiesFilePath = config.getServletContext().getInitParameter("metacat.properties.path");
        File contextDeploymentDir = new File(config.getServletContext().getRealPath("/"));
        String fullMetacatPropertiesFilePath = contextDeploymentDir.getParent()  + metacatPropertiesFilePath;
        //System.out.println("the url is "+url);
        //System.out.println("the path is "+url.getPath());
        //System.out.println("the file is "+url.getPath());
        //ApplicationController controller = null;
        try {
             //ApplicationController controller = new ApplicationController(FILEPREFIX + url.getFile(), fullMetacatPropertiesFilePath);
            ApplicationController controller = new ApplicationController("/index-processor-context.xml", fullMetacatPropertiesFilePath);
             //Start the controller in other thread - SystemmetadataEventListener and to generate indexes for those haven't been indexed in another thread
             Thread controllerThread = new Thread(controller);
             controllerThread.start();
        } catch (Exception e) {
            throw new ServletException(e.getMessage());
        }
       
        //controller.startIndex();//Start to generate indexes for those haven't been indexed in another thread
        //List<SolrIndex> list = controller.getSolrIndexes();
        //System.out.println("++++++++++++++++++++++++------------------- the size is  "+list.size());
    }
    
    /**
     * Loads the metacat.prioerties into D1 Settings utility
     * this gives us access to all metacat properties as well as 
     * overriding any properties as needed.
     * 
     * Makes sure shared Hazelcast configuration file location is set
     * 
     * @param config the servlet config
     */
    /*private void initializeSharedConfiguration(ServletConfig config) {
    	
		try {
			// find the sibling metacat.properties file
			String metacatPropertiesFilePath = config.getServletContext().getInitParameter("metacat.properties.path");
			File contextDeploymentDir = new File(config.getServletContext().getRealPath("/"));
			String fullMetacatPropertiesFilePath = contextDeploymentDir.getParent()  + metacatPropertiesFilePath;
			Settings.augmentConfiguration(fullMetacatPropertiesFilePath);
		} catch (ConfigurationException e) {
			log.error("Could not initialize shared Metacat properties. " + e.getMessage(), e);
		}
		
		// make sure hazelcast configuration is defined so that
		String hzConfigFileName = Settings.getConfiguration().getString("dataone.hazelcast.configFilePath");
		if (hzConfigFileName == null) {
			// use default metacat hazelcast.xml file in metacat deployment
			hzConfigFileName = 
    				Settings.getConfiguration().getString("application.deployDir") +
    				"/" +
    				Settings.getConfiguration().getString("application.context") + 
    				"/WEB-INF/hazelcast.xml";
			// set it for other parts of the code
			Settings.getConfiguration().setProperty("dataone.hazelcast.configFilePath", hzConfigFileName);
			//set data.hazelcast.location.clientconfig. This property will be used in d1_cn_index_processor module.
			//if we don't set this property, d1_cn_index_processor will use the default location /etc/dataone/storage.
			Settings.getConfiguration().setProperty("dataone.hazelcast.location.clientconfig", hzConfigFileName);
		}
    }*/
    
    /**
     *Actions needed to be done before close the servlet
     */
    public void destroy() {
     //do nothing
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
