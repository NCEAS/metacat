package edu.ucsb.nceas.metacat.restservice.v1;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import edu.ucsb.nceas.metacat.restservice.D1ResourceHandler;
import edu.ucsb.nceas.metacat.restservice.D1RestServlet;


/**
 * Routes MN REST service requests to the appropriate handler
 *  
 */
public class MNRestServlet extends D1RestServlet {
    
	/**
	 * Provide an MNResourceHandler subclass of D1ResourceHandler
	 */
	@Override
    protected D1ResourceHandler createHandler(HttpServletRequest request, HttpServletResponse response) 
    	throws ServletException, IOException {
        return new MNResourceHandler(getServletContext(), request, response);
    }

}
