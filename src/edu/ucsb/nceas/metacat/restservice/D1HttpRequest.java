/**
 *  '$RCSfile$'
 *  Copyright: 2010 Regents of the University of California and the
 *              National Center for Ecological Analysis and Synthesis
 *
 *   '$Author: berkley $'
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

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

/**
 * @author berkley
 * Class to override getPathInfo on the servlet request
 */
public class D1HttpRequest extends HttpServletRequestWrapper
{   
    /**
     * HttpServletRequestWrapper(HttpServletRequest request) 
     */
    public D1HttpRequest(ServletRequest request)
    {
        super((HttpServletRequest)request);
    }
    
    /**
     * override getPathInfo to handle special characters
     * @return
     */
    @Override
    public String getPathInfo() 
    {
        String s = super.getPathInfo();
        System.out.println("original pathInfo: " + s);
        String reqUri = this.getRequestURI();
        System.out.println("original requestURI: " + reqUri);
        String strip = this.getContextPath() + this.getServletPath();
        System.out.println("stripping " + strip + " from requestURI");
        s = reqUri.substring(strip.length());
        /*try
        {
            s = URLDecoder.decode(s, "UTF-8");
        }
        catch (UnsupportedEncodingException e)
        {
            s = URLDecoder.decode(s);
        }*/
        System.out.println("new pathinfo: " + s);
        return s;
    }
    
}
