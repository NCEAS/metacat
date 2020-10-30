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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author berkley
 * Class to override getPathInfo on the servlet request
 */
public class D1HttpRequest extends HttpServletRequestWrapper
{   
    private static Log logMetacat = LogFactory.getLog(D1HttpRequest.class);
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
        logMetacat.info("D1HttpRequest.getPathInfo - the orignial pathInfo: "+s);
        String reqUri = this.getRequestURI();
        logMetacat.debug("D1HttpRequest.getPathInfo - original requestURI: "+reqUri);
        String strip = this.getContextPath() + this.getServletPath();
        logMetacat.debug("D1HttpRequest.getPathInfo - stripping "+ strip + " from requestURI");
        s = reqUri.substring(strip.length());
        /*try
        {
            s = URLDecoder.decode(s, "UTF-8");
        }
        catch (UnsupportedEncodingException e)
        {
            s = URLDecoder.decode(s);
        }*/
        logMetacat.info("D1HttpRequest.getPathInfo - the new pathInfo: "+s);
        return s;
    }
    
}
