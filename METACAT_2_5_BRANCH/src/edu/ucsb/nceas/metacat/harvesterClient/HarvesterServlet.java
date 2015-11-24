/**
 *  '$RCSfile$'
 *  Copyright: 2004 University of New Mexico and the 
 *             Regents of the University of California
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
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package edu.ucsb.nceas.metacat.harvesterClient;

import javax.servlet.*;
import javax.servlet.http.*;
/**
 * HarvesterServlet class allows Harvester to be run as a background
 * process. This eliminates the need to run Harvester in a terminal window.
 * To activate this servlet, uncomment the HarvesterServlet entry in the
 * appropriate 'lib/web.xml.tomcat*' file.
 * 
 * @author costa
 */
public class HarvesterServlet extends HttpServlet implements Runnable {

  /*
   * Class fields
   */
  private static final String SCHEMA_DIR = "harvester";
  private static String SCHEMA_LOCATION;
  private static final String SCHEMA_NAME = "harvestList.xsd";
  static final long serialVersionUID = 0;  // Needed for Eclipse warning.

  /*
   * Object fields
   */  
  Thread harvesterThread;                // background Harvester thread


  /**
   * When the thread is destroyed, sets the Harvester.keepRunning flag to false.
   */
  public void destroy() {
    Harvester.setKeepRunning(false);
  }
  

  /**
   * Initializes the servlet. Reads properties and initializes object fields.
   * 
   * @throws ServletException
   */
  public void init(ServletConfig config) throws ServletException {
    ServletContext context = null;
    String fileSeparator = System.getProperty("file.separator");
    String schemaPath;

    super.init(config);
    context = config.getServletContext();
    schemaPath = context.getRealPath(SCHEMA_DIR) + fileSeparator + SCHEMA_NAME;
    SCHEMA_LOCATION = "eml://ecoinformatics.org/harvestList " + schemaPath;
    harvesterThread = new Thread(this);
    harvesterThread.setPriority(Thread.MIN_PRIORITY);  // be a good citizen
    harvesterThread.start();
  }


  /**
   * Runs the Harvester main program in a separate thread. First sleeps for
   * 30 seconds to give Metacat a chance to fully initialize.
   */
  public void run() {
      String[] args = new String[2];
      args[0] = "false";     // Set to true if in command line mode or test mode
      args[1] = SCHEMA_LOCATION;

      try {
        Thread.sleep(30000);    // Sleep 30 seconds before starting Harvester
      }
      catch (InterruptedException e) {
        System.err.println("InterruptedException: " + e.getMessage());
      }

      Harvester.main(args);
  }

}
