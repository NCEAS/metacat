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
        logMetacat.info("D1HttpRequest.getPathInfo - original requestURI: " + reqUri);
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
        logMetacat.info("D1HttpRequest.getPathInfo - the new pathInfo which comes from requestURI is: " + s);
        return s;
    }
    
}
