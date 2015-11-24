/**
 *  '$RCSfile$'
 *  Copyright: 2000 Regents of the University of California and the
 *              National Center for Ecological Analysis and Synthesis
 *
 *   '$Author$'
 *     '$Date$'
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
package edu.ucsb.nceas.metacat.restservice.v2;

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
