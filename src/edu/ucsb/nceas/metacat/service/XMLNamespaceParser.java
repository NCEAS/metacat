package edu.ucsb.nceas.metacat.service;

import java.io.IOException;
import java.io.Reader;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;


/**
 * This class will parse the root element to figure out the namespace of the root element(we also call
 * it the namespace of the element). If it doesn't have a namespace, but it does have an attribute of noNamespaceSchemaLocation
 * at the root element, it will get the value as well. 
 * @author tao
 *
 */
public class XMLNamespaceParser extends DefaultHandler {

    private Reader xml = null;
    private XMLReader parser = null;
    private boolean rootElement = true;
    private static Log logMetacat = LogFactory.getLog(XMLNamespaceParser.class);
    private String namespace = null;
    private String noNamespaceSchemaLocation = null;
    
    /**
     * Constructor
     * @param xml the xml object which will be parsed
     */
    public XMLNamespaceParser(Reader xml) throws SAXException, PropertyNotFoundException {
      this.xml = xml;
      initParser();
    }
    
    /*
     * Initialize sax parser
     */
    private void initParser() throws SAXException, PropertyNotFoundException {   
      // Get an instance of the parser
       String parserName = PropertyService.getProperty("xml.saxparser");
       parser = XMLReaderFactory.createXMLReader(parserName);
       parser.setContentHandler(this);
      
    }
    
    /**
     * Parse the xml file
     * @throws SAXException if some sax related exception happens
     * @throws IOException if the schema content couldn't be found
     */
    public void parse() throws SAXException, IOException {
        try {
            parser.parse(new InputSource(xml));
        } catch (ParsingEndException e) {
            logMetacat.debug("XMLNamespace.parse - The parsing process stopped.");
        }
      
    }
    
    /** SAX Handler that is called at the start of each XML element */
    @Override
    public void startElement(String uri, String localName, String qName,
            Attributes atts) throws SAXException{
      logMetacat.debug("XMLNamespace.startElement - uri: "+uri);
      logMetacat.debug("XMLNamespace.startElement - local name: "+localName);
      logMetacat.debug("XMLNamespace.startElement - qualified name: "+qName);
      if(!rootElement) {
          throw new ParsingEndException("We only parse the root elment. We got there and the parsing stopped.");
      } else {
          rootElement = false;
          if(uri != null && !uri.trim().equals("")) {
              namespace = uri;
          }
          logMetacat.debug("XMLNamespace.startElement - the namespace is: "+namespace);
          if(atts != null) {
              for(int i=0; i<atts.getLength(); i++) {
                  if((atts.getURI(i) != null && atts.getURI(i).equals("http://www.w3.org/2001/XMLSchema-instance")) &&
                          (atts.getLocalName(i) != null && atts.getLocalName(i).equals("noNamespaceSchemaLocation"))) {
                      if(atts.getValue(i) != null && !atts.getValue(i).trim().equals("")) {
                          noNamespaceSchemaLocation = atts.getValue(i);
                      }
                      logMetacat.debug("XMLNamespace.startElement - we found the attribute of the noNamespaceSchemaLocation and its value is: "+noNamespaceSchemaLocation);
                      break;
                  }
              }
          }
          
      }
    }
    
    
    /**
     * Get the namespace of the document (root element). The parse() method should be called first
     * @return the value of the namespace. A null will be returned if it can't be found.
     */
    public String getNamespace() {
        logMetacat.debug("XMLNamespace.getNamespace - the namespace is: "+namespace);
        return namespace;
    }
    
    
    /**
     * Get the value of noNamespaceSchemaLocation of the document (root element). The parse() method should be called first
     * @return the value of the noNamespaceSchemaLocation. A null will be returned if it can't be found.
     */
    public String getNoNamespaceSchemaLocation() {
        logMetacat.debug("XMLNamespace.getNoNamespaceSchemaLocation - the NoNamespaceSchemaLocation is: "+noNamespaceSchemaLocation);
        return noNamespaceSchemaLocation;
    }
    
    /**
     * A class signals that the parsing process stop early. 
     * @author tao
     *
     */
    class ParsingEndException extends RuntimeException {
        public ParsingEndException(String message) {
            super(message);
        }
    }
}
