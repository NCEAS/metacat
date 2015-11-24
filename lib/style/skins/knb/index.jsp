<%@ page    language="java" %>
<%
/**
 *  '$RCSfile$'
 *    Copyright: 2000 Regents of the University of California and the
 *               National Center for Ecological Analysis and Synthesis
 *  For Details: http://www.nceas.ucsb.edu/
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
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
 %>

 <%@ include file="../../common/common-settings.jsp"%>

 <% 
/**
 *  Does a redirect to knb homepage at KNB_SITE_URL
 */

       java.util.Properties params = new java.util.Properties();
       java.util.Enumeration paramlist = request.getParameterNames();

       String userName = null;
       String organization = null;
       String password = null;

       while (paramlist.hasMoreElements()) {
          String name = (String) paramlist.nextElement();
          String[] value = request.getParameterValues(name);
            params.put(name, value[0]);
       }

       String reply = null;
       try{

         java.net.URL url = new java.net.URL(KNB_SITE_URL + "/index.jsp");
         edu.ucsb.nceas.utilities.HttpMessage msg =
                            new edu.ucsb.nceas.utilities.HttpMessage(url);
         java.io.InputStream returnStrea = msg.sendPostData(params);
         java.io.InputStreamReader returnStream =
                            new java.io.InputStreamReader(returnStrea);
         java.io.StringWriter sw = new java.io.StringWriter();
         int len;
         char[] characters = new char[512];
         while ((len = returnStream.read(characters, 0, 512)) != -1) {
                sw.write(characters, 0, len);
         }
         returnStream.close();
         reply = sw.toString();
         sw.close();
      } catch(Exception e){
         response.sendRedirect(KNB_SITE_URL);
      }
%>
<%=reply%>

