/**
 *  '$RCSfile$'
 *  Copyright: 2004 University of New Mexico and the 
 *                  Regents of the University of California
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

import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import edu.ucsb.nceas.metacat.AuthSession;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.metacat.util.SystemUtil;


/**
 *  HarvesterRegistrationLogin implements a servlet to login to the Harvester
 *  Registration servlet.
 */
public class HarvesterRegistrationLogin extends HttpServlet {

    final String LDAP_DOMAIN = ",dc=ecoinformatics,dc=org";

    /**
     *  Handle "GET" method requests from HTTP clients
     *
     *  @param  req   The request
     *  @param  res   The response
     *  @throws ServletException, java.io.IOException
     */
    public void doGet(HttpServletRequest req, HttpServletResponse res)
                throws ServletException, java.io.IOException {
        handleGetOrPost(req, res);
    }


    /**
     *  Handle "POST" method requests from HTTP clients
     *
     *  @param  req   The request
     *  @param  res  The response
     *  @throws ServletException, java.io.IOException
     */
    public void doPost(HttpServletRequest req, HttpServletResponse res)
                throws ServletException, java.io.IOException {
        handleGetOrPost(req, res);
    }


    /**
     *  Handle "GET" or "POST" method requests from HTTP clients
     *
     *  @param  req   The request
     *  @param  res  The response
     *  @throws ServletException, java.io.IOException
     */
    private void handleGetOrPost(HttpServletRequest req,
                                 HttpServletResponse res)
                 throws ServletException, java.io.IOException {
        AuthSession authSession;
        String authSessionMessage;
        HttpSession httpSession;
        boolean isValid;
        String o = req.getParameter("o");
        String organization;
        String passwd = req.getParameter("passwd");
        PrintWriter out = res.getWriter();
        String uid = req.getParameter("uid");
        String user;

        if ((uid == null) || (uid.equals(""))) {
          out.println("Invalid login: no Username specified.");
          return;
        }
        else if ((o == null) || (o.equals(""))) {
          out.println("Invalid login: no Organization selected.");
          return;
        }
        else if ((passwd == null) || (passwd.equals(""))) {
          out.println("Invalid login: no Password specified.");
          return;
        }
        else {
          user = "uid=" + uid + ",o=" + o + LDAP_DOMAIN;
        }

        res.setContentType("text/plain");
        
        try {
          authSession = new AuthSession();
          isValid = authSession.authenticate(req, user, passwd);
          authSessionMessage = authSession.getMessage();
          System.out.println("authSession.authenticate(): "+authSessionMessage);
          out.println("authSession.authenticate(): " + authSessionMessage);

          if (isValid) {
            httpSession = req.getSession(true);
            httpSession.setAttribute("username", user);
            httpSession.setAttribute("password", passwd);
            String context = PropertyService.getProperty("application.context");
            res.sendRedirect("/" + context + "/harvesterRegistration");
          }
          else {
            out.println("Invalid login");
          }
        } 
        catch (Exception e) {
          System.out.println("Error in AuthSession()" + e.getMessage());
        }
    }
}
