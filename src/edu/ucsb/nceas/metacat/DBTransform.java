/**
 *  '$RCSfile$'
 *    Purpose: A Class that transforms an XML text document
 *             into a another type using XSL
 *  Copyright: 2000 Regents of the University of California and the
 *             National Center for Ecological Analysis and Synthesis
 *    Authors: Matt Jones
 *
 * '$Author$'
 * '$Date$'
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

package edu.ucsb.nceas.metacat;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.io.Writer;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

import javax.xml.transform.TransformerFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.log4j.Logger;
import org.apache.xerces.parsers.DOMParser;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.apache.xpath.XPathAPI;

import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.metacat.properties.SkinPropertyService;
import edu.ucsb.nceas.metacat.util.SystemUtil;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;
import edu.ucsb.nceas.utilities.SortedProperties;

/**
 * A Class that transforms XML documents utitlizing XSL style sheets
 */
public class DBTransform {


  private String 	configDir = null;
  private String	defaultStyle = null;
  private Logger logMetacat = Logger.getLogger(DBTransform.class);
  private String httpServer = null;
  private String contextURL = null;
  private String servletURL = null;
  private String userManagementURL = null;
  
  /**
   * construct a DBTransform instance.
   *
   * Generally, one calls transformXMLDocument() after constructing the instance
   *
   * @param conn the database connection from which to lookup the public ids
   */
  public DBTransform()
                  throws IOException,
                         SQLException,
                         ClassNotFoundException,
                         PropertyNotFoundException
  {
    configDir = SystemUtil.getStyleSkinsDir();
    defaultStyle = PropertyService.getProperty("application.default-style");
    httpServer = SystemUtil.getServerURL();
    contextURL = SystemUtil.getContextURL();
    servletURL = SystemUtil.getServletURL();
    userManagementURL = PropertyService.getProperty("auth.userManagementUrl");
  }

  /**
   * Transform an XML document using the stylesheet reference from the db
   *
   * @param doc the document to be transformed
   * @param sourcetype the document type of the source
   * @param targettype the target document type
   * @param qformat the name of the style set to use
   * @param pw the PrintWriter to which output is printed
   * @param params some parameters for eml2 transformation
   */
  public void transformXMLDocument(String doc, String sourceType,
                                   String targetType, String qformat,
                                   Writer w, Hashtable<String, String[]> param,
                                   String sessionid)
 {

	  String xslSystemId = getStyleSystemId(qformat, sourceType, targetType);
	  try {
		  // Look up the stylesheet for this type combination
		  if (xslSystemId != null) {
			// Create a stylesheet from the system id that was found
			doc = removeDOCTYPE(doc);
			StreamSource xml = new StreamSource(new StringReader(doc));
			StreamResult result = new StreamResult(w);
			doTransform(xml, result, xslSystemId, param, qformat, sessionid);
		  }
		  else {
			  // No stylesheet registered form this document type, so just return the
			  // XML stream we were passed
			  w.write(doc);
		  }
      }
      catch (Exception e)
      {
    	  try {
    		  String msg = xslSystemId + ": Error transforming document in " +
	           "DBTransform.transformXMLDocument: " +
	           e.getMessage();
    		  w.write(msg);
    		  w.flush();
    		  logMetacat.error(msg, e);
		} catch (IOException e1) {
			logMetacat.error(e1.getMessage(), e1);
		}

      }
    
  }


  /**
   * Reads skin's config file if it exists, and populates Transformer paramaters
   * with its contents.
   * It then adds the parameters passed to it via Hashtable param to the Transformer.
   * It then calls the Transformer.transform method.
   */
  private void doTransform(StreamSource xml, 
          StreamResult resultOutput,
          String xslSystemId, 
          Hashtable<String, String[]> param,
          String qformat, 
          String sessionid) 
          throws Exception {
      
      SortedProperties skinOptions;
      TransformerFactory tFactory;
      Transformer transformer;
      String key, value;
      Enumeration<String> en;
      Iterator<Map.Entry<String, String>> iterIt;
      Map.Entry<String, String> entry;
      
      if (xslSystemId != null) {
        tFactory = TransformerFactory.newInstance();
        transformer = tFactory.newTransformer(new StreamSource(xslSystemId));
        
        transformer.setParameter("qformat", qformat);
        logMetacat.info("DBTransform.doTransform - qformat: " + qformat);
        
        skinOptions = SkinPropertyService.getProperties(qformat);
        if (skinOptions != null) {            
            iterIt = skinOptions.getProperties().entrySet().iterator();
            while (iterIt.hasNext()) {
                entry = iterIt.next();
                key = entry.getKey();
                value = entry.getValue();
                //only include the plain properties
                if (key.indexOf('.') == -1) {
                	transformer.setParameter(key, value);
                }
            }
        }
        
        if (sessionid != null && !sessionid.equals("null")) {
          transformer.setParameter("sessid", sessionid);
        }
        
        //set up the default params (can be overridden by the passed in params)
        String cgiPrefix = SystemUtil.getCGI_URL();
        logMetacat.debug("DBTransform.doTransform - cgi-prefix: " + cgiPrefix);
        logMetacat.debug("DBTransform.doTransform - httpServer: " + httpServer);
        logMetacat.debug("DBTransform.doTransform - contextURL: " + contextURL);
        logMetacat.debug("DBTransform.doTransform - serletURL: " + servletURL);
        logMetacat.debug("DBTransform.doTransform - userManagementURL: " + userManagementURL);
        transformer.setParameter("cgi-prefix", cgiPrefix);
        transformer.setParameter("httpServer", httpServer);
        transformer.setParameter("contextURL", contextURL);
        transformer.setParameter("servletURL", servletURL);
        transformer.setParameter("userManagementURL", userManagementURL);
        // Set up parameter for transformation
        if ( param != null) {
          en = param.keys();
          while (en.hasMoreElements()) {
            key = en.nextElement();
            value = (param.get(key))[0];
            logMetacat.info("DBTransform.doTransform - param: " + key + " -- " + value);
            transformer.setParameter(key, value);
          }
        }
        transformer.transform(xml, resultOutput);
    }
  }//doTransform


  /**
   * gets the content of a tag in a given xml file with the given path
   * @param f the file to parse
   * @param path the path to get the content from
   */
  public static NodeList getPathContent(File f, String path)
  {
    if(f == null)
    {
      return null;
    }

    DOMParser parser = new DOMParser();
    InputSource in;
    FileInputStream fs;

    try
    {
      fs = new FileInputStream(f);
      in = new InputSource(fs);
    }
    catch(FileNotFoundException fnf)
    {
      fnf.printStackTrace();
      return null;
    }

    try
    {
      parser.parse(in);
      fs.close();
    }
    catch(Exception e1)
    {
      System.err.println("File: " + f.getPath() + " : parse threw: " +
                         e1.toString());
      return null;
    }

    Document doc = parser.getDocument();

    try
    {
      NodeList docNodeList = XPathAPI.selectNodeList(doc, path);
      return docNodeList;
    }
    catch(Exception se)
    {
      System.err.println("file: " + f.getPath() + " : parse threw: " +
                         se.toString());
      return null;
    }
  }

  /**
   * Lookup a stylesheet reference from the db catalog
   *
   * @param qformat    the named style-set format
   * @param sourcetype the document type of the source
   * @param targettype the document type of the target
   */
  public String getStyleSystemId(String qformat, String sourcetype,
                String targettype) {
    String systemId = null;

    if ((qformat == null) || (qformat.equals("html"))) {
      qformat = defaultStyle;
    }

    // Load the style-set map for this qformat into a DOM
    try {
      boolean breakflag = false;
      String filename = configDir + "/" + qformat + "/" + qformat + ".xml";
      logMetacat.info("DBTransform.getStyleSystemId - Trying style-set file: " + filename);
      File f = new File(filename);
      NodeList nlDoctype = getPathContent(f, "/style-set/doctype");
      NodeList nlDefault = getPathContent(f, "/style-set/default-style");
      Node nDefault = nlDefault.item(0);
      systemId = nDefault.getFirstChild().getNodeValue(); //set the default

      for(int i=0; i<nlDoctype.getLength(); i++)
      { //look for the right sourcetype
        Node nDoctype = nlDoctype.item(i);
        NamedNodeMap atts = nDoctype.getAttributes();
        Node nAtt = atts.getNamedItem("publicid");
        String doctype = nAtt.getFirstChild().getNodeValue();
        if(doctype.equals(sourcetype))
        { //found the right sourcetype now we need to get the target type
          NodeList nlChildren = nDoctype.getChildNodes();
          for(int j=0; j<nlChildren.getLength(); j++)
          {
            Node nChild = nlChildren.item(j);
            String childName = nChild.getNodeName();
            if(childName.equals("target"))
            {
              NamedNodeMap childAtts = nChild.getAttributes();
              Node nTargetPublicId = childAtts.getNamedItem("publicid");
              String target = nTargetPublicId.getFirstChild().getNodeValue();
              if(target.equals(targettype))
              { //we found the right target type
                NodeList nlTarget = nChild.getChildNodes();
                for(int k=0; k<nlTarget.getLength(); k++)
                {
                  Node nChildText = nlTarget.item(k);
                  if(nChildText.getNodeType() == Node.TEXT_NODE)
                  { //get the text from the target node
                    systemId = nChildText.getNodeValue();
                    breakflag = true;
                    break;
                  }
                }
              }
            }

            if(breakflag)
            {
              break;
            }
          }
        }

        if(breakflag)
        {
          break;
        }
      }
    }
    catch(Exception e)
    {
      System.out.println("Error parsing style-set file: " + e.getMessage());
      e.printStackTrace();
    }
    
    //Check if the systemId is relative path, add a postfix - the contextULR to systemID. 
    if (systemId != null && systemId.indexOf("http://" ) == -1)
    {
    	systemId = contextURL+systemId;
    }
    // Return the system ID for this particular source document type
    logMetacat.info("DBTransform.getStyleSystemId - style system id is: " + systemId);
    return systemId;
  }

// /* Method to modified the system id of xml input -- make sure it
//    points to system id in xml_catalog table
//  */
//  private void modifiedXmlStreamSource(StreamSource xml, String publicId)
//                                       throws Exception
//  {
//    // make sure the xml is not null
//    if (xml == null || publicId == null)
//    {
//      return;
//    }
//    logMetacat.info("public id of input stream is " +publicId);
//    // Get system id from xml_catalog table
//    String systemId = DBEntityResolver.getDTDSystemID(publicId);
//    logMetacat.info("system id of input stream from xml_catalog"
//                               +"table is " +systemId);
//    //set system id to input stream
//    xml.setSystemId(systemId);
//  }

  /*
   * removes the DOCTYPE element and its contents from a Sting
   * used to avoid problems with incorrect SystemIDs
   */
  private String removeDOCTYPE(String in) {
    String ret = "";
    int startpos = in.indexOf("<!DOCTYPE");
    if (startpos>-1) {
      int stoppos = in.indexOf(">", startpos + 8);
      ret = in.substring(0,startpos) + in.substring(stoppos+1,in.length());
    } else {
      return in;
    }
    return ret;
  }

  /**
   * the main routine used to test the transform utility.
   *
   * Usage: java DBTransform
   */
  static public void main(String[] args) {

     if (args.length > 0)
     {
        System.err.println("Wrong number of arguments!!!");
        System.err.println("USAGE: java DBTransform");
        return;
     } else {
        try {

          // Create a test document
          StringBuffer testdoc = new StringBuffer();
          String encoding = "UTF-8";
          testdoc.append("<?xml version=\"1.0\" encoding=\"" + encoding + "\"?>");
          testdoc.append("<eml-dataset><metafile_id>NCEAS-0001</metafile_id>");
          testdoc.append("<dataset_id>DS001</dataset_id>");
          testdoc.append("<title>My test doc</title></eml-dataset>");

          // Transform the document to the new doctype
          Writer w = new OutputStreamWriter(System.out, encoding);
          DBTransform dbt = new DBTransform();
          dbt.transformXMLDocument(testdoc.toString(),
                                   "-//NCEAS//eml-dataset//EN",
                                   "-//W3C//HTML//EN",
                                   "knb",
                                   w, null, null);

        } catch (Exception e) {
          System.err.println("EXCEPTION HANDLING REQUIRED");
          System.err.println(e.getMessage());
          e.printStackTrace(System.err);
        }
     }
  }

//  private void dbg(int position) {
//    System.err.println("Debug flag: " + position);
//  }

}
