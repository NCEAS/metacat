/* WMS Filter
 * Author: MP
 * Status: Just a test
 * MPTODO: make this a true servlet filter to append sld to any incoming wms request
 */
package edu.ucsb.nceas.metacat.spatial;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.IOException;
import java.util.Enumeration;

public final class WmsFilter implements Filter {
   private FilterConfig filterConfig = null;

   public void init(FilterConfig filterConfig) 
      throws ServletException {
      this.filterConfig = filterConfig;
   }

   public void destroy() {
      this.filterConfig = null;
   }

   public void doFilter(ServletRequest request,
      ServletResponse response, FilterChain chain) 
      throws IOException, ServletException {

      if (filterConfig == null)
         return;

      System.out.println("\n===============");

      System.out.println(" The filter Works !!!");
      long before = System.currentTimeMillis();
      
      // Attributes != Paramters but there is no setParameter
      //request.setAttribute("SLD", "http://pmark.msi.ucsb.edu:8180/metacat/style/skins/ebm/spatial/data_bounds_style.sld");

      Enumeration e = request.getParameterNames();
      while( e.hasMoreElements() ) {
          String name = (String) e.nextElement();
          System.out.println( name + " = " + request.getParameter(name));
      }

      
      chain.doFilter(request, response);
      long after = System.currentTimeMillis();
      System.out.println(" *** Time  " + (after - before) + "ms");


      System.out.println("===============\n");

      // A simple redirect won't work since it will filter itself endlessly
      //HttpServletResponse hres = (HttpServletResponse) response;
      //HttpServletRequest hreq = (HttpServletRequest) response;
      //hres.sendRedirect( hreq.getRequestURL().toString() );
   }
}
