package edu.ucsb.nceas.metacat.restservice;

import javax.servlet.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


import java.io.*;

/**
 * @author berkley
 *
 */
public class D1URLFilter implements Filter
{
    ServletContext context;
    private static Log logMetacat = LogFactory.getLog(D1URLFilter.class);
    
    public void init(FilterConfig filterConfig) 
    {
        logMetacat.debug("D1URLFilter.init - init.");
        this.context = filterConfig.getServletContext();
    }
    
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) 
        throws IOException, ServletException 
    {
        logMetacat.debug("D1URLFilter.doFilter - do filtering");
        D1HttpRequest d1h = new D1HttpRequest(request);
        chain.doFilter(d1h, response);
    }
    
    public void destroy() 
    {
        logMetacat.debug("D1URLFilter.destory - destroy filter");
    }
}
