/**
 *  '$RCSfile$'
 *    Purpose: A Class that validates XML documents
 *             This class is designed to be 'parser independent
 *             i.e. it uses only org.xml.sax classes
 *             It is tied to SAX 2.0 methods
 *  Copyright: 2000 Regents of the University of California and the
 *             National Center for Ecological Analysis and Synthesis
 *    Authors: Dan Higgins, Matt Jones
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
package edu.ucsb.nceas.metacat;


import java.net.URL;
import java.net.MalformedURLException;
import java.util.*;
import java.io.*;
import java.lang.reflect.*;
import java.sql.*;

import org.w3c.dom.*;
import org.xml.sax.*;
import org.xml.sax.helpers.XMLReaderFactory;

import com.arbortext.catalog.*;

import edu.ucsb.nceas.metacat.database.DBConnection;
import edu.ucsb.nceas.metacat.database.DBConnectionPool;
import edu.ucsb.nceas.metacat.properties.PropertyService;

/**
 * Name: DBValidate.java
 *       Purpose: A Class that validates XML documents
 * 			   This class is designed to be parser independent
 *    			   i.e. it uses only org.xml.sax classes
 * 			   It is tied to SAX 2.0 methods
 *     Copyright: 2000 Regents of the University of California and the
 *                National Center for Ecological Analysis and Synthesis
 *                April 28, 2000
 *    Authors: Dan Higgins, Matt Jones
 */
public class DBValidate {
    
  static int WARNING = 0;
  static int ERROR=1;
  static int FATAL_ERROR=2;

  XMLReader parser;
  ErrorStorer ef;
  String xml_doc; // document to be parsed
  public boolean alreadyHandle = false;
    
  /** Construct a new validation object */
  public DBValidate() {
    alreadyHandle = false;
    try {
      // Get an instance of the parser
      String parserName = PropertyService.getProperty("xml.saxparser");
      parser = XMLReaderFactory.createXMLReader(parserName);
      parser.setFeature("http://xml.org/sax/features/validation",true);
      //parser.setValidationMode(true);     // Oracle
    } catch (Exception e) {
      System.err.println("Could not create parser in DBValidate.DBValidate");
    }
  }
    
  /** Construct a new validation object using an OASIS catalog file */
  public DBValidate(String xmlcatalogfile)  {
    this();

    CatalogEntityResolver cer = new CatalogEntityResolver();
    try {
      Catalog myCatalog = new Catalog();
      myCatalog.loadSystemCatalogs();
      myCatalog.parseCatalog(xmlcatalogfile);
      cer.setCatalog(myCatalog);
    } catch (Exception e) {
      System.out.println("Problem creating Catalog in DBValidate.DBValidate");
    }

    parser.setEntityResolver(cer);
  }

  /** Construct a new validation object using a database entity resolver */
  public DBValidate(DBConnection conn) {
    this();

    DBEntityResolver dbresolver = new DBEntityResolver();
    parser.setEntityResolver(dbresolver);
  }

  /** 
   * validate an xml document against its DTD
   *
   * @param doc the filename of the document to validate
   */
  public boolean validate(String doc) {
    xml_doc = doc;    
    ef = new ErrorStorer();
    ef.resetErrors();
    parser.setErrorHandler(ef);
    try {
      parser.parse((createURL(xml_doc)).toString());
    } catch (IOException e) {
      System.out.println("IOException:Could not parse :" + xml_doc +
                         " from DBValidate.validate");
      ParseError eip = null;
      eip = new ParseError("",0,0,
                "IOException:Could not parse :"+xml_doc);
      if (ef.errorNodes == null)  ef.errorNodes = new Vector();
      ef.errorNodes.addElement(eip);
        
    } catch (Exception e) {} 

    
    
    if (ef != null && ef.getErrorNodes()!=null && 
      ef.getErrorNodes().size() > 0 ) {
      return false; 
    } else {
      return true;
    }
  }
    
  /** 
   * validate an xml document against its DTD
   *
   * @param xmldoc the String containing the xml document to validate
   */
  public boolean validateString(String xmldoc) {
    // string is actual XML here, NOT URL or file name    
    ef = new ErrorStorer();
    ef.resetErrors();
    parser.setErrorHandler(ef);
      
    InputSource is = new InputSource(new StringReader(xmldoc));
    try 
    {
      
      parser.parse(is);
     
    }
    catch (SAXParseException e) 
    {
      System.out.println("SAXParseException Error in DBValidate.validateString"
                         +e.getMessage());
      ef.error(e);
    }
    catch (SAXException saxe)
    {
      System.out.println("SAXException error in validateString: "
                          +saxe.getMessage());
      ef.otherError(saxe, null);
      
    }
    catch (IOException ioe)
    {
      System.out.println("IOExcption error in validateString "
                          +ioe.getMessage());
      ef.otherError(ioe, null);
    }

    if (ef != null && ef.getErrorNodes()!=null && 
      ef.getErrorNodes().size() > 0 ) {
      return false; 
    } else {
      return true;
    }
  }
    
  /** provide a list of errors from the validation process */
  public String returnErrors() {
    StringBuffer errorstring = new StringBuffer();
    errorstring.append("<?xml version=\"1.0\" ?>\n");
    if (ef != null && ef.getErrorNodes()!=null && 
        ef.getErrorNodes().size() > 0 ) {
      Vector errors = ef.getErrorNodes();
      errorstring.append("<validationerrors>\n");
      for (Enumeration e = errors.elements() ; e.hasMoreElements() ;) {
        errorstring.append(
                      ((ParseError)(e.nextElement())).toXML());
      }
      errorstring.append("</validationerrors>\n");
    } else {
      errorstring.append("<valid />\n");
    }
    return errorstring.toString();
  }
              
  /** Create a URL object from either a URL string or a plain file name. */
  private URL createURL(String name) throws Exception {
    try {
      URL u = new URL(name);
      return u;
    } catch (MalformedURLException ex) {
    }
    URL u = new URL("file:" + new File(name).getAbsolutePath());
    return u;
  }    

  /** 
   * main method for testing 
   * <p>
   * Usage: java DBValidate <xmlfile or URL>
   */
  public static void main(String[] args) {

    if (args.length != 1) {
      System.out.println("Usage: java DBValidate <xmlfile or URL>");
      System.exit(0);
    }

    String doc = args[0];

    DBConnection conn = null;
    int serailNumber = -1;
    try {
      conn = DBConnectionPool.getDBConnection("DBValidate.main");
      serailNumber = conn.getCheckOutSerialNumber();
  
      DBValidate gxv = new DBValidate(conn);
      if (gxv.validate(doc)) {
        System.out.print(gxv.returnErrors());
      } else {
        System.out.print(gxv.returnErrors());
      }
    } catch (SQLException e) {
      System.out.println("<error>Couldn't open database connection.</error>");
    } 
    finally
    {
      DBConnectionPool.returnDBConnection(conn, serailNumber);
    }//finally
  }

    
  /**
   * ErrorStorer has been revised here to simply create a Vector of 
   * ParseError objects
   *
   */
  class ErrorStorer implements ErrorHandler {

    //
    // Data
    //
    Vector errorNodes = null;
        
    /**
     * Constructor
     */
    public ErrorStorer() {
    }

    /**
     * The client is is allowed to get a reference to the Hashtable,
     * and so could corrupt it, or add to it...
     */
    public Vector getErrorNodes() {
        return errorNodes;
    }

    /**
     * The ParseError object for the node key is returned.
     * If the node doesn't have errors, null is returned.
     */
    public Object getError() {
        if (errorNodes == null)
            return null;
        return errorNodes;
    }
        
    /**
     * Reset the error storage.
     */
    public void resetErrors() {
        if (errorNodes != null)
        errorNodes.removeAllElements();
    }
    
    /***/
    public void warning(SAXParseException ex) {
        
        handleError(ex, WARNING);
        
    }

    public void error(SAXParseException ex) {
      
        handleError(ex, ERROR);
       
    }

    public void fatalError(SAXParseException ex){
      
        handleError(ex, FATAL_ERROR);
        
    }
    
    public void otherError(Exception ex, String fileName)
    {
      if (!alreadyHandle)
      {
        if (errorNodes == null) 
        {
          errorNodes = new Vector();
        }
      
        ParseError error = new ParseError(fileName, ex.getMessage());
        errorNodes.addElement(error);
       
      }
    }
        
    private void handleError(SAXParseException ex, int type) {
     
      if (errorNodes == null) {
        errorNodes = new Vector();
      }
      
      ParseError eip = null;
      
      eip = new ParseError(ex.getSystemId(), ex.getLineNumber(),
                           ex.getColumnNumber(), ex.getMessage());
      
      // put it in the Hashtable.
      errorNodes.addElement(eip);
      alreadyHandle = true;
      
    }
        
  }
    
  /**
   * The ParseError class wraps up all the error info from
   * the ErrorStorer's error method.
   *
   * @see ErrorStorer
   */
  class ParseError extends Object {

    //
    // Data
    //

    String fileName;
    int lineNo;
    int charOffset;
    String msg;

    /**
     * Constructor
     */
    public ParseError(String fileName, int lineNo, int charOffset, String msg) {
      this.fileName=fileName;
      this.lineNo=lineNo;
      this.charOffset=charOffset;
      this.msg=msg;
    }
    public ParseError(String fileName, String msg) {
      this.fileName=fileName;
      this.msg=msg;
    }
    //
    // Getters...
    //
    public String getFileName() { return fileName; }
    public int getLineNo() { return lineNo; }
    public int getCharOffset() { return charOffset;}
    public String getMsg() { return msg; }
    public void setMsg(String s) { msg = s; }

    /** Return the error message as an xml fragment */
    public String toXML() {
      StringBuffer err = new StringBuffer();
      err.append("<error>\n");
      err.append("<filename>").append(getFileName()).append("</filename>\n");
      err.append("<line>").append(getLineNo()).append("</line>\n");
      err.append("<offset>").append(getCharOffset()).append("</offset>\n");
      err.append("<message>").append(getMsg()).append("</message>\n");
      err.append("</error>\n");
      return err.toString();
    }
  }
}
