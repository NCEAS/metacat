/**
 *  '$RCSfile$'
 *  Copyright: 2000 Regents of the University of California and the
 *              National Center for Ecological Analysis and Synthesis
 *
 *   '$Author: Serhan AKIN $'
 *     '$Date: 2009-06-13 13:28:21 +0300  $'
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
package edu.ucsb.nceas.metacat.restservice;

import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

/**
 * Metacat implemantation of Earthgrid (Ecogrid) REST API as a servlet. In each request
 * REST Servlet initialize a D1ResourceHandler object and then D1ResourceHandler object 
 * handles with request and writes approriate response. 
 *  
 */
public class D1RestServlet extends HttpServlet {

    protected Logger logMetacat;
    protected D1ResourceHandler handler;

    /**
     * Subclasses should override this method to provide the appropriate handler subclass
     * @param request
     * @param response
     * @return
     * @throws ServletException
     * @throws IOException
     */
    protected D1ResourceHandler createHandler(HttpServletRequest request, HttpServletResponse response) 
		throws ServletException, IOException {
	    D1ResourceHandler handler = new D1ResourceHandler(getServletContext(), request, response);
	    return handler;
	}
    
    /**
     * Initalize servlet by setting logger
     */
    @Override
    public void init(ServletConfig config) throws ServletException {
        logMetacat = Logger.getLogger(this.getClass());
        super.init(config);
    }

    /** Handle "GET" method requests from HTTP clients */
    @Override
    protected void doGet(HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException {
        System.out.println("HTTP Verb: GET");
        logMetacat.info("D1RestServlet.doGet - HTTP Verb: GET");
        handler = createHandler(request, response);
        handler.handle(D1ResourceHandler.GET);
    }

    /** Handle "POST" method requests from HTTP clients */
    @Override
    protected void doPost(HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException {
        System.out.println("HTTP Verb: POST");
        logMetacat.info("D1RestServlet.doPost - HTTP Verb: POST");
        handler = createHandler(request, response);
        handler.handle(D1ResourceHandler.POST);
    }

    /** Handle "DELETE" method requests from HTTP clients */
    @Override
    protected void doDelete(HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException {
        System.out.println("HTTP Verb: DELETE");
        logMetacat.info("D1RestServlet.doDelete - HTTP Verb: DELETE");
        handler = createHandler(request, response);
        handler.handle(D1ResourceHandler.DELETE);
    }

    /** Handle "PUT" method requests from HTTP clients */
    @Override
    protected void doPut(HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException {
        System.out.println("HTTP Verb: PUT");
        logMetacat.info("D1RestServlet.doPut - HTTP Verb: PUT");
        handler = createHandler(request, response);
        handler.handle(D1ResourceHandler.PUT);
    }

    /** Handle "PUT" method requests from HTTP clients */
    @Override
    protected void doHead(HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException {
        System.out.println("HTTP Verb: HEAD");
        logMetacat.info("D1RestServlet.doHead - HTTP Verb: HEAD");
        handler = createHandler(request, response);
        handler.handle(D1ResourceHandler.HEAD);
    }
}
