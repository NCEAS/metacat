/**
 *  '$RCSfile$'
 *  Copyright: 2005 University of New Mexico and the 
 *             Regents of the University of California and the
 *             National Center for Ecological Analysis and Synthesis
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

package edu.ucsb.nceas.metacat.advancedsearch;

import java.io.File;
import java.io.IOException;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import edu.ucsb.nceas.metacat.client.*;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.metacat.shared.ServiceException;
import edu.ucsb.nceas.metacat.util.SystemUtil;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;

/** 
 * @author dcosta
 * 
 * The LoginServlet class executes a metacat login via a metacat client.
 */
public class LoginServlet extends HttpServlet {

  /*
   * Class fields
   */
  private static final String CONFIG_DIR = "WEB-INF";
  private static final String CONFIG_NAME = "metacat.properties";
  static final long serialVersionUID = 0;  // Needed for Eclipse warning.
				   
  // Instance Variables
  private String contextString = "";

  // Methods
  
  /**
   * Executes a metacat login action. On a successful login, the metacat object
   * is stored as an attribute of the HttpSession object.
   */
  public void doPost(HttpServletRequest request, HttpServletResponse response)
          throws IOException, ServletException {
    RequestDispatcher dispatcher;
    HttpSession httpSession = request.getSession();
    LoginBean loginBean;
    boolean loginSuccess = false;
    Metacat metacat = (Metacat) httpSession.getAttribute("metacat");
    MetacatLogin metacatLogin;
    String metacatURL;
    ServletContext servletContext = httpSession.getServletContext();
    String username;
    
    // First check whether the metacat URL was set as a context-param.
    metacatURL = servletContext.getInitParameter("metacatURL");
 
    // If no metacat URL was configured, then derive the metacat URL from the
    // server name and server port.
    if (metacatURL == null || metacatURL.equals("")) {
      MetacatHelper metacatHelper = new MetacatHelper();
      String serverName = request.getServerName();
      int serverPort = request.getServerPort();
      metacatURL = 
       metacatHelper.constructMetacatURL(serverName, serverPort, contextString);
    }
    
    if (metacat == null) {
      try {
        metacat = MetacatFactory.createMetacatConnection(metacatURL);
      }
      catch (MetacatInaccessibleException mie) {
        System.err.println("Metacat Inaccessible:\n" + mie.getMessage());
      }
    }
    
    loginBean = (LoginBean) request.getAttribute("loginBean");
    metacatLogin = new MetacatLogin(loginBean);
    loginSuccess = metacatLogin.executeLogin(metacatURL, metacat);
    httpSession.setAttribute("metacat", metacat);

    // If login succeeds, forward to the simple search page. If login fails,
    // forward to the login page where an error message will be displayed.
    if (loginSuccess) {
      httpSession.setAttribute("loggedIn", new Boolean(true));
      username = loginBean.getUsername();
      httpSession.setAttribute("username", username);
      //dispatcher = request.getRequestDispatcher("/style/skins/lter/metacatsearch.jsp");
      dispatcher = request.getRequestDispatcher("/style/skins/lter/index.jsp");
    }
    else {
      httpSession.setAttribute("loggedIn", new Boolean(false));
      request.setAttribute("loginFailure", "true");
      dispatcher = request.getRequestDispatcher("/style/skins/lter/index_login.jsp");
    }

    dispatcher.forward(request, response);
  }


  /**
	 * Initializes the servlet. Reads properties and initializes object fields.
	 * 
	 * @throws ServletException
	 */
	public void init(ServletConfig config) throws ServletException {
		ServletContext context = null;
		String dirPath;

		super.init(config);
		context = config.getServletContext();
		dirPath = context.getRealPath(CONFIG_DIR);

		try {
			PropertyService.getInstance();
			this.contextString = PropertyService.getProperty("application.context");
		} catch (ServiceException se) {
			System.err.println("Error in loading properties: " + se.getMessage());
		} catch (PropertyNotFoundException pnfe) {
			System.err.println("couldn't read property during initialization: "
					+ pnfe.getMessage());
		}
	}
}
