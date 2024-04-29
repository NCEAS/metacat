/**
 * Copyright 2006 OCLC Online Computer Library Center Licensed under the Apache
 * License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or
 * agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.ucsb.nceas.metacat.oaipmh.provider.server;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.SocketException;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Properties;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPOutputStream;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


import edu.ucsb.nceas.metacat.oaipmh.provider.server.crosswalk.Eml2oai_dc;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.metacat.shared.ServiceException;

import ORG.oclc.oai.server.catalog.AbstractCatalog;
import ORG.oclc.oai.server.verb.BadVerb;
import ORG.oclc.oai.server.verb.GetRecord;
import ORG.oclc.oai.server.verb.Identify;
import ORG.oclc.oai.server.verb.ListIdentifiers;
import ORG.oclc.oai.server.verb.ListMetadataFormats;
import ORG.oclc.oai.server.verb.ListRecords;
import ORG.oclc.oai.server.verb.ListSets;
import ORG.oclc.oai.server.verb.OAIInternalServerError;
import ORG.oclc.oai.server.verb.ServerVerb;


/**
 * OAIHandler is the primary Servlet for OAICat.
 * 
 * @author Jeffrey A. Young, OCLC Online Computer Library Center
 */
public class OAIHandler extends HttpServlet {

  /*
   * Class fields
   */
  
  private static final Log log = LogFactory.getLog(OAIHandler.class);
  private static final long serialVersionUID = 1L;
  private static final String VERSION = "1.5.57";
  private static boolean debug = false;
  
  // Set to false when developing and testing code outside of Metacat
  private static boolean integratedWithMetacat = true;

  
  /*
   * Class methods
   */
  
  /**
   * Get the VERSION number
   */
  public static String getVERSION() {
    return VERSION;
  }
  
  /**
   * Boolean to determine whether the Data Provider code is executing in a
   * Metacat application. This is normally true, but the 
   * 'integratedWithMetacat' value can be set to
   * false by a developer when testing the code outside of Metacat. This
   * eliminates dependencies on Metacat property settings.
   * 
   * @return
   */
  public static boolean isIntegratedWithMetacat() {
    return integratedWithMetacat;
  }


  /*
   * Instance fields
   */
  
  protected HashMap attributesMap = new HashMap();
  private final String CONFIG_DIR = "WEB-INF";
  private final String CONFIG_NAME = "metacat.properties";
  private final String XSLT_DIR = "oaipmh";

  
  /*
   * Instance methods
   */
  
  /**
   * Peform the http GET action. Note that POST is shunted to here as well. The
   * verb widget is taken from the request and used to invoke an OAIVerb object
   * of the corresponding kind to do the actual work of the verb.
   * 
   * @param request
   *          the servlet's request information
   * @param response
   *          the servlet's response information
   * @exception IOException
   *              an I/O error occurred
   */
  public void doGet(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {
    ServletConfig servletConfig = this.getServletConfig();
    //init_debug(servletConfig); // used when debugging the init() method
    String pathInfo = request.getPathInfo();
    HashMap attributes = getAttributes(pathInfo);
    Properties properties;
    
    if (!filterRequest(request, response)) { return; }
    
    log.debug("attributes=" + attributes);
    properties = (Properties) attributes.get("OAIHandler.properties");
    boolean monitor = false;
    
    if (properties.getProperty("OAIHandler.monitor") != null) {
      monitor = true;
    }
    
    boolean serviceUnavailable = isServiceUnavailable(properties);

    HashMap serverVerbs = ServerVerb.getVerbs(properties);
    Transformer transformer = 
                         (Transformer) attributes.get("OAIHandler.transformer");

    boolean forceRender = false;
    
    if ("true".equals(properties.getProperty("OAIHandler.forceRender"))) {
      forceRender = true;
    }

    // try {
    request.setCharacterEncoding("UTF-8");
    // } catch (UnsupportedEncodingException e) {
    // e.printStackTrace();
    // throw new IOException(e.getMessage());
    // }
    
    Date then = null;
    if (monitor) then = new Date();
    
    if (debug) {
      Enumeration headerNames = request.getHeaderNames();
      System.out.println("OAIHandler.doGet: ");
      
      while (headerNames.hasMoreElements()) {
        String headerName = (String) headerNames.nextElement();
        System.out.print(headerName);
        System.out.print(": ");
        System.out.println(request.getHeader(headerName));
      }
    }
    
    if (serviceUnavailable) {
      response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE,
          "Sorry. This server is down for maintenance");
    } 
    else {
      try {
        String userAgent = request.getHeader("User-Agent");
        
        if (userAgent == null) {
          userAgent = "";
        } else {
          userAgent = userAgent.toLowerCase();
        }
        
        Transformer serverTransformer = null;
        
        if (transformer != null) {
          // return HTML if the client is an old browser
          if (forceRender
              || userAgent.indexOf("opera") != -1
              || (userAgent.startsWith("mozilla") && userAgent
                  .indexOf("msie 6") == -1
              /* && userAgent.indexOf("netscape/7") == -1 */)) {
            serverTransformer = transformer;
          }
        }
        
        String result = getResult(attributes, request, response,
                                  serverTransformer, serverVerbs);
        
        // log.debug("result=" + result);
        // if (serverTransformer) { // render on the server
        // response.setContentType("text/html; charset=UTF-8");
        // StringReader stringReader = new StringReader(getResult(request));
        // StreamSource streamSource = new StreamSource(stringReader);
        // StringWriter stringWriter = new StringWriter();
        // transformer.transform(streamSource, new StreamResult(stringWriter));
        // result = stringWriter.toString();
        // } else { // render on the client
        // response.setContentType("text/xml; charset=UTF-8");
        // result = getResult(request);
        // }

        Writer out = getWriter(request, response);
        out.write(result);
        out.close();
      } 
      catch (FileNotFoundException e) {
        if (debug) {
          e.printStackTrace();
          System.out.println("SC_NOT_FOUND: " + e.getMessage());
        }
        response.sendError(HttpServletResponse.SC_NOT_FOUND, e.getMessage());
      } 
      catch (TransformerException e) {
        response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                           e.getMessage()
                          );
      } 
      catch (OAIInternalServerError e) {
        response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                           e.getMessage()
                          );
      } 
      catch (SocketException e) {
        System.out.println(e.getMessage());
        response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                           e.getMessage()
                          );
      } 
      catch (Throwable e) {
        e.printStackTrace();
        response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                           e.getMessage()
                          );
      }
    }
    
    if (monitor) {
      StringBuffer reqUri = new StringBuffer(request.getRequestURI().toString());
      String queryString = request.getQueryString(); // d=789
      
      if (queryString != null) {
        reqUri.append("?").append(queryString);
      }
      
      Runtime rt = Runtime.getRuntime();
      System.out.println(rt.freeMemory() + "/" + rt.totalMemory() + " "
          + ((new Date()).getTime() - then.getTime()) + "ms: "
          + reqUri.toString());
    }
  }


  /**
   * Perform a POST action. Actually this gets shunted to GET.
   * 
   * @param request
   *          the servlet's request information
   * @param response
   *          the servlet's response information
   * @exception IOException
   *              an I/O error occurred
   */
  public void doPost(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {
    doGet(request, response);
  }

  
  /**
   * Override to do any pre-qualification; return false if the response should be
   * returned immediately, without further action.
   * 
   * @param request
   * @param response
   * @return false=return immediately, true=continue
   */
  protected boolean filterRequest(HttpServletRequest request,
      HttpServletResponse response) {
    return true;
  }


  public HashMap getAttributes(Properties properties) 
          throws Throwable {
    HashMap attributes = new HashMap();
    Enumeration attrNames = getServletContext().getAttributeNames();
    String serviceUnavailable;
    
    while (attrNames.hasMoreElements()) {
      String attrName = (String) attrNames.nextElement();
      Object attrValue = getServletContext().getAttribute(attrName);
      attributes.put(attrName, attrValue);
    }

    attributes.put("OAIHandler.properties", properties);
    String missingVerbClassName = properties.getProperty(
        "OAIHandler.missingVerbClassName", "ORG.oclc.oai.server.verb.BadVerb");
    Class missingVerbClass = Class.forName(missingVerbClassName);
    attributes.put("OAIHandler.missingVerbClass", missingVerbClass);

    serviceUnavailable = properties.getProperty("OAIHandler.serviceUnavailable");
    if (!"true".equals(serviceUnavailable)) {
      attributes.put("OAIHandler.version", VERSION);
      AbstractCatalog metacatCatalog = 
                       AbstractCatalog.factory(properties);
      attributes.put("OAIHandler.catalog", metacatCatalog);
    }
    
    boolean forceRender = false;
    if ("true".equals(properties.getProperty("OAIHandler.forceRender"))) {
      forceRender = true;
    }

    String styleSheet = properties.getProperty("OAIHandler.styleSheet");
    String appBase = properties.getProperty("OAIHandler.appBase");

    if (appBase == null) appBase = "webapps";

    String render = properties.getProperty("OAIHandler.renderForOldBrowsers");
    if ((styleSheet != null) && 
        ("true".equalsIgnoreCase(render) || forceRender)
       ) {
      InputStream is;
      
      try {
        is = new FileInputStream(appBase + "/" + styleSheet);
      } 
      catch (FileNotFoundException e) {
        // This is a silly way to skip the context name in the styleSheet name
        is = new FileInputStream(getServletContext().getRealPath(
            styleSheet.substring(styleSheet.indexOf("/", 1) + 1)));
      }
      
      StreamSource xslSource = new StreamSource(is);
      TransformerFactory tFactory = TransformerFactory.newInstance();
      Transformer transformer = tFactory.newTransformer(xslSource);
      attributes.put("OAIHandler.transformer", transformer);
      is.close();
    }

    return attributes;
  }


  public HashMap getAttributes(String pathInfo) {
    HashMap attributes = null;
    log.debug("pathInfo=" + pathInfo);

    if (pathInfo != null && pathInfo.length() > 0) {
      if (attributesMap.containsKey(pathInfo)) {
        log.debug("attributesMap containsKey");
        attributes = (HashMap) attributesMap.get(pathInfo);
      } 
      else {
        log.debug("!attributesMap containsKey");
        
        try {
          String fileName = pathInfo.substring(1) + ".properties";
          log.debug("attempting load of " + fileName);
          Thread thread = Thread.currentThread();
          ClassLoader classLoader = thread.getContextClassLoader();
          InputStream in = classLoader.getResourceAsStream(fileName);
          
          if (in != null) {
            log.debug("file found");
            Properties properties = new Properties();
            properties.load(in);
            attributes = getAttributes(properties);
          } 
          else {
            log.debug("file not found");
          }
          
          attributesMap.put(pathInfo, attributes);
        } 
        catch (Throwable e) {
          log.debug("Couldn't load file", e);
          // do nothing
        }
      }
    }
    
    if (attributes == null) log.debug("use global attributes");
    attributes = (HashMap) attributesMap.get("global");

    return attributes;
  }


  public static String getResult(HashMap attributes,
                                 HttpServletRequest request, 
                                 HttpServletResponse response,
                                 Transformer serverTransformer, 
                                 HashMap serverVerbs
                                ) 
      throws Throwable {
    String verb = request.getParameter("verb");
    log.debug("verb: " + verb);
    String result = null;

    if (verb != null) {
      if (verb.equals("GetRecord")) {
        result = GetRecord.construct(attributes, request, response,
          serverTransformer);
      } 
      else if (verb.equals("Identify")) {
        result = Identify.construct(attributes, request, response,
          serverTransformer);
      } 
      else if (verb.equals("ListIdentifiers")) {
        result = ListIdentifiers.construct(attributes, request, response,
          serverTransformer);
      } 
      else if (verb.equals("ListMetadataFormats")) {
        result = ListMetadataFormats.construct(attributes, request, response,
          serverTransformer);
      } 
      else if (verb.equals("ListRecords")) {
        result = ListRecords.construct(attributes, request, response,
          serverTransformer);
      } 
      else if (verb.equals("ListSets")) {
        result = ListSets.construct(attributes, request, response,
          serverTransformer);
      } 
      else {
        result = BadVerb.construct(attributes, request, response,
          serverTransformer);
      }
    }
    else {
      // Return a 'badVerb' response when the verb is missing from the request
      result = BadVerb.construct(attributes, request, response,
          serverTransformer);
    }

    return result;
    
    /*
     * } catch (NoSuchMethodException e) { throw new
     * OAIInternalServerError(e.getMessage()); } catch (IllegalAccessException
     * e) { throw new OAIInternalServerError(e.getMessage()); }
     */
  }


  /**
   * Get a response Writer depending on acceptable encodings
   * 
   * @param request
   *          the servlet's request information
   * @param response
   *          the servlet's response information
   * @exception IOException
   *              an I/O error occurred
   */
  public static Writer getWriter(HttpServletRequest request,
      HttpServletResponse response) throws IOException {
    Writer out;
    String encodings = request.getHeader("Accept-Encoding");
    
    if (debug) { System.out.println("encodings=" + encodings); }
    
    if (encodings != null && encodings.indexOf("gzip") != -1) {
      // System.out.println("using gzip encoding");
      // log.debug("using gzip encoding");
      response.setHeader("Content-Encoding", "gzip");
      out = new OutputStreamWriter(new GZIPOutputStream(response
          .getOutputStream()), "UTF-8");
      // } else if (encodings != null && encodings.indexOf("compress") != -1) {
      // // System.out.println("using compress encoding");
      // response.setHeader("Content-Encoding", "compress");
      // ZipOutputStream zos = new ZipOutputStream(response.getOutputStream());
      // zos.putNextEntry(new ZipEntry("dummy name"));
      // out = new OutputStreamWriter(zos, "UTF-8");
    } 
    else if (encodings != null && encodings.indexOf("deflate") != -1) {
      // System.out.println("using deflate encoding");
      // log.debug("using deflate encoding");
      response.setHeader("Content-Encoding", "deflate");
      out = new OutputStreamWriter(new DeflaterOutputStream(response
          .getOutputStream()), "UTF-8");
    } 
    else {
      // log.debug("using no encoding");
      out = response.getWriter();
    }
    
    return out;
  }


  /**
   * init is called one time when the Servlet is loaded. This is the place where
   * one-time initialization is done. Specifically, we load the properties file
   * for this application, and create the AbstractCatalog object for subsequent
   * use.
   * 
   * @param config
   *          servlet configuration information
   * @exception ServletException
   *              there was a problem with initialization
   */
  //public void init_debug(ServletConfig config) throws ServletException {
    public void init(ServletConfig config) throws ServletException {
        super.init(config);

        if (isIntegratedWithMetacat()) {
            try {
                PropertyService.getInstance();
            } catch (ServiceException se) {
                System.err.println("Error in loading properties: " + se.getMessage());
            }
        }

        ServletContext servletContext = config.getServletContext();
        String configDirPath = servletContext.getRealPath(CONFIG_DIR);
        String configPath = configDirPath + "/" + CONFIG_NAME;

        // Initialize the directory path to the crosswalk XSLT files
        String xsltDirPath = servletContext.getRealPath(XSLT_DIR);
        Eml2oai_dc.setDirPath(xsltDirPath);

        try {
            HashMap attributes = null;
            Properties properties = new Properties();
            for (String key : PropertyService.getInstance().getPropertyNames()) {
                properties.setProperty(key, PropertyService.getInstance().getProperty(key));
            }
            attributes = getAttributes(properties);
            attributesMap.put("global", attributes);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            throw new ServletException(e.getMessage());
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            throw new ServletException(e.getMessage());
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            throw new ServletException(e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
            throw new ServletException(e.getMessage());
        } catch (Throwable e) {
            e.printStackTrace();
            throw new ServletException(e.getMessage());
        }
    }


  /**
   * Should the server report itself down for maintenance? Override this method
   * if you want to do this check another way.
   * 
   * @param properties
   * @return true=service is unavailable, false=service is available
   */
  protected boolean isServiceUnavailable(Properties properties) {
    if (properties.getProperty("OAIHandler.serviceUnavailable") != null) {
      return true;
    }
    
    return false;
  }

}
