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

import edu.ucsb.nceas.metacat.AuthLdap;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


/**
 * @author dcosta
 * 
 * MetacatHelper provides auxiliary methods for helping the advanced search
 * classes interact with Metacat.
 */
public class MetacatHelper {

	private static Log logMetacat = LogFactory.getLog(AuthLdap.class);
	
  /**
	 * Constructs a DN (Distinguished Name) string for the ecoinformatics.org
	 * LDAP.
	 * 
	 * @param username
	 *            The LDAP uid, e.g. "dcosta"
	 * @param organization
	 *            The LDAP organization, e.g. "LTER"
	 * @return DN The distinguished name string.
	 */
	public String constructDN(final String username, final String organization) {
		String DC;
		try {
			DC = PropertyService.getProperty("auth.base");
		} catch (PropertyNotFoundException pnfe) {
			DC = "dc=ecoinformatics,dc=org";
			logMetacat.error("Could not find property: auth.base.  Setting to: " +
					"dc=ecoinformatics,dc=org : " + pnfe.getMessage());
		}
		final String DN = "uid=" + username + ",o=" + organization
				+ "," + DC;

		return DN;
	}
  

  /**
	 * Constructs a URL to the metacat servlet.
	 * 
	 * @param serverName
	 *            A server name, e.g. "prairie.lternet.edu"
	 * @param serverPort
	 *            A server port, e.g. 8080. If no port is required in the URL,
	 *            pass a 0 and the argument will be ignored.
	 * @param contextString
	 *            The context under which metacat is running, e.g. "knb".
	 * @return metacatURL The URL to the metacat servlet.
	 */
  public String constructMetacatURL(final String serverName, 
                                    final int serverPort,
                                    final String contextString) {
    String metacatURL = "http://" + serverName;
    
    if (serverPort > 0) {
      final Integer serverPortInteger = new Integer(serverPort);
      final String serverPortString = serverPortInteger.toString();
      metacatURL += ":" + serverPortString;
    }
    
    metacatURL += "/" + contextString + "/metacat";

    return metacatURL;
  }
  
  
  /**
   * Gets the relative path to the advancedsearchresults.jsp file.
   * 
   * @return resultsJSP The relative path to the advanced search results JSP.
   */
  public String getResultsJSP() {
    String resultsJSP = "style/skins/default/advancedsearchresults.jsp";
    
    return resultsJSP;
  }

  
  /**
   * Gets the path to the resultset XSL file.
   * 
   * @param request   The HttpServletRequest object.
   * @return xslPath  The real path to the resultset XSL file.
   */
  public String getResultsetXSL(HttpServletRequest request) {
    HttpSession httpSession = request.getSession();
    ServletContext servletContext = httpSession.getServletContext();
    String xslPath = servletContext.getRealPath("style/common/resultset.xsl");
    
    return xslPath;
  }

}
