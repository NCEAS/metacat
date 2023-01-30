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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dataone.client.v2.formats.ObjectFormatCache;

import org.dataone.cn.indexer.IndexWorker;


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
        try {
            System.out.println("before starting the index worker");
            IndexWorker worker =  new IndexWorker();
            String[] argus = null;
            worker.main(argus);
            System.out.println("after starting the working main method ===========");
        } catch (Exception e) {
            throw new ServletException(e.getMessage());
        }
        //controller.startIndex();//Start to generate indexes for those haven't been indexed in another thread
        //List<SolrIndex> list = controller.getSolrIndexes();
        //System.out.println("++++++++++++++++++++++++------------------- the size is  "+list.size());
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
