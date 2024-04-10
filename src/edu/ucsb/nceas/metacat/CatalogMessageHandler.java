package edu.ucsb.nceas.metacat;

import java.sql.*;
import java.util.Stack;
import java.util.Vector;
import java.util.Enumeration;
import java.util.EmptyStackException;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.ext.DeclHandler;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.helpers.DefaultHandler;

/** 
 * A Class implementing callback bethods for the SAX parser to
 * call when processing the XML messages from the replication handler
 */
public class CatalogMessageHandler extends DefaultHandler 
{
  private Vector updates = new Vector();
  private Vector indivUpdate = new Vector();
  String currentTag = new String();
  StringBuffer textBuffer = new StringBuffer();
  
  /**
   * This method starts a new vector for each updatedDocument tag.
   */
  public void startElement(String uri, String localName, String qName, 
                           Attributes attributes) throws SAXException
  {
    currentTag = localName;
    if(localName.equals("row"))
    {
      indivUpdate = new Vector();
    }
    textBuffer = new StringBuffer();
  }
  
  /**
   * This method write the indivUpdate to updates when it finds the end of
   */
  public void endElement(String uri, String localName, String qName) 
              throws SAXException
  {
    if(currentTag.equals("entry_type") || currentTag.equals("source_doctype")
       || currentTag.equals("target_doctype") || currentTag.equals("public_id")
       || currentTag.equals("system_id"))
    {
      
      indivUpdate.add((textBuffer.toString()).trim());
    }
    if(localName.equals("row"))
    {
      updates.add(new Vector(indivUpdate));
    }
  }
  
  /**
   * Take the data out of the docid and date_updated fields
   */
  public void characters(char[] ch, int start, int length) throws SAXException
  {
    textBuffer.append(new String(ch, start,length));
  }
  
  public Vector getCatalogVect()
  {
    return updates;
  }
  
}
