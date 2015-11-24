/**
 *  '$RCSfile$'
 *    Purpose: A Class that stores xml schema information 
 *  Copyright: 2008 Regents of the University of California and the
 *             National Center for Ecological Analysis and Synthesis
 *    Authors: Jing Tao
 * 
 *   '$Author: leinfelder $'
 *     '$Date: 2008-09-26 15:43:57 -0700 (Fri, 26 Sep 2008) $'
 * '$Revision: 4399 $'
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
package edu.ucsb.nceas.metacat.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.Vector;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;

/**
 * A class will parse an schema file to get information - such as included 
 * xsd files in this schema
 * @author tao
 *
 */
public class XMLSchemaParser extends DefaultHandler
{
  private static String INCLUDE = "include";
  private static String SCHEMALOCATION = "schemaLocation";
  private Vector<String> includedSchemaFilePaths = new Vector<String>();
  private InputStream schemaIn = null;
  private XMLReader parser = null;
  /**
   * Constructor
   * @param schemaIn the schema content as an InputStream object
   */
  public XMLSchemaParser(InputStream schemaIn) throws SAXException, PropertyNotFoundException
  {
    this.schemaIn =schemaIn;
    initParser();
  }
  
  /*
   * Initialize sax parser
   */
  private void initParser() throws SAXException, PropertyNotFoundException
  {   
    // Get an instance of the parser
     String parserName = PropertyService.getProperty("xml.saxparser");
     parser = XMLReaderFactory.createXMLReader(parserName);
     parser.setContentHandler(this);
    
  }
  
  /**
   * Parse the schema file
   * @throws SAXException if some sax related exception happens
   * @throws IOException if the schema content couldn't be found
   */
  public void parse() throws SAXException, IOException
  {
    parser.parse(new InputSource(schemaIn));
  }
  
  /**
   * Get the included schema file paths in this schema
   * @return the included schema file paths
   */
  public Vector<String> getIncludedSchemaFilePathes()
  {
    return includedSchemaFilePaths;
  }
  
  /** SAX Handler that is called at the start of each XML element */
  public void startElement(String uri, String localName, String qName,
          Attributes atts) throws SAXException
  {
    if(localName != null && localName.equals(INCLUDE) && atts != null)
    {
      for (int i = 0; i < atts.getLength(); i++) 
      {
        String attributeName = atts.getQName(i);
        if(attributeName != null && attributeName.equals(SCHEMALOCATION))
        {
          String attributeValue = atts.getValue(i);
          if(attributeValue != null || !attributeValue.trim().equals(""))
          {
            includedSchemaFilePaths.add(attributeValue);
          }
        }
        
      }
    }
  }
  
}
