package edu.ucsb.nceas.metacat;

import java.io.*;
import java.util.*;

import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.SAXParser;
import org.xml.sax.InputSource;

/**
 * @author berkley
 * A class to parse, then encapsulate a metacat result set
 */
public class MetacatResultSet extends DefaultHandler
{    
    private Vector<Document> documents;
    private Document document;
    private String currentElement;
    private String paramName;
    private boolean inDocument = false;
    
    public MetacatResultSet(String s)
      throws Exception
    {
        try
        {
            documents = new Vector();
            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setValidating(false);
            SAXParser parser = factory.newSAXParser();
            parser.parse(new InputSource(new StringReader(s)), this);
        }
        catch(Exception e)
        {
            e.printStackTrace();
            throw new Exception("Could not parse the resultset: " + e.getMessage());
        }
    }
    
    public void characters(char[] ch, int start, int length) throws SAXException
    {
        String content = new String(ch, start, length);
        //System.out.println("currentElement: " + currentElement);
        //System.out.println("content: " + content);

        if(currentElement.equals("document"))
        {

        }
        else if(currentElement.equals("docid"))
        {
            document.docid = content;
        }
        else if(currentElement.equals("docname"))
        {
            document.docname = content;
        }
        else if(currentElement.equals("doctype"))
        {
            document.doctype = content;
        }
        else if(currentElement.equals("createdate"))
        {
            document.createdate = content;
        }
        else if(currentElement.equals("updatedate"))
        {
            document.updatedate = content;
        }
        else if(currentElement.equals("param"))
        { 
            //System.out.println("setting param: " + paramName + " with value " + content);
            document.setField(paramName, content);
        }
    }
    
    public void startDocument()
    {
        
    }
    
    public void endDocument()
    {
        
    }
    
    public void startElement(String uri, String localName, String qName, Attributes attributes)
    {
        currentElement = qName;
        //System.out.println("currentElement: " + currentElement);
        //System.out.println("inDocument: " + inDocument);
        if(qName.equals("document"))
        {
            document = new Document();
            inDocument = true;
        }
        
        if(qName.equals("param"))
        {
            paramName = attributes.getValue("name");
        }
    }
    
    public void endElement(String uri, String localName, String qName)
    {
        if(qName.equals("document"))
        {
            documents.add(document);
        }
    }
    
    public List getDocuments()
    {
        return documents;
    }
    
    public class Document
    {
        private Hashtable<String, String> fields = new Hashtable();
        public String docid;
        public String docname;
        public String doctype;
        public String createdate;
        public String updatedate;
        
        public Document()
        {
            docid = null;
            docname = null;
            doctype = null;
            createdate = null;
            updatedate = null;
        }
        
        public String getField(String name)
        {
            return fields.get(name);
        }
        
        public void setField(String name, String value)
        {
            String val = fields.get(name);
            if(val != null)
            {
                val += value;
            }
            else 
            {
                val = value;
            }
            fields.put(name, val);
        }
        
        public String toString()
        {
            String s = new String();
            s = "{docid=" + docid.trim() + ", " +
                "docname=" + docname.trim() + ", " +
                "doctype=" + doctype.trim() + ", " +
                "createdate=" + createdate.trim() + ", " +
                "updatedate=" + updatedate.trim();
            Enumeration keys = fields.keys();
            while(keys.hasMoreElements())
            {
                s += ", ";
                String name = (String)keys.nextElement();
                String value = fields.get(name);
                s += name.trim() + "=" + value.trim();
            }
            s += "}";
            return s;
        }
    }
    
    
}
