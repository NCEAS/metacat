package edu.ucsb.nceas.metacat.oaipmh.harvester;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.StringTokenizer;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;
import java.util.zip.ZipInputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.xpath.XPathAPI;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;


/**
 * HarvesterVerb is the parent class for each of the OAI verbs.
 * 
 * @author Duane Costa, University of New Mexico, LTER Network Office
 * @author Jeffrey A. Young, OCLC Online Computer Library Center
 */
public abstract class HarvesterVerb {
  
  /* Class variables */

  private static Log logger = LogFactory.getLog(HarvesterVerb.class);
  
  public static final String SCHEMA_LOCATION_V2_0 = 
    "http://www.openarchives.org/OAI/2.0/ " +
    "http://www.openarchives.org/OAI/2.0/OAI-PMH.xsd";
  
  private static HashMap<Thread, DocumentBuilder> builderMap = 
                                         new HashMap<Thread, DocumentBuilder>();
  private static DocumentBuilderFactory documentBuilderFactory = null;
  private static Element namespaceElement = null;
  private static TransformerFactory transformerFactory = 
                                               TransformerFactory.newInstance();

  
  /* Instance variables */
  
  private Document document = null;
  private String schemaLocation = null;
  private String requestURL = null;
  
  
  /* Constructors */
  
  /**
   * Mock object creator (for unit testing purposes)
   */
  public HarvesterVerb() {
  }


  /**
   * Performs the OAI request
   * 
   * @param requestURL
   * @throws IOException
   * @throws ParserConfigurationException
   * @throws SAXException
   * @throws TransformerException
   */
  public HarvesterVerb(String requestURL) throws IOException,
      ParserConfigurationException, SAXException, TransformerException {
    this.requestURL = requestURL;
  }


  /* Static initialization code */
  
  static {
    try {
      /* Load DOM Document */
      documentBuilderFactory = DocumentBuilderFactory.newInstance();
      documentBuilderFactory.setNamespaceAware(true);
      Thread thread = Thread.currentThread();
      DocumentBuilder builder = documentBuilderFactory.newDocumentBuilder();
      builderMap.put(thread, builder);

      DOMImplementation impl = builder.getDOMImplementation();
      
      Document namespaceHolder = impl.createDocument(
                          "http://www.oclc.org/research/software/oai/harvester",
                          "harvester:namespaceHolder", 
                          null
                                                    );
      
      namespaceElement = namespaceHolder.getDocumentElement();
      
      namespaceElement.setAttributeNS(
                          "http://www.w3.org/2000/xmlns/",
                          "xmlns:harvester",
                          "http://www.oclc.org/research/software/oai/harvester"
                                     );
      
      namespaceElement.setAttributeNS(
                          "http://www.w3.org/2000/xmlns/",
                          "xmlns:xsi", 
                          "http://www.w3.org/2001/XMLSchema-instance"
                                     );
      
      namespaceElement.setAttributeNS(
                          "http://www.w3.org/2000/xmlns/",
                          "xmlns:oai20", 
                          "http://www.openarchives.org/OAI/2.0/"
                                     );
    } 
    catch (Exception e) {
      e.printStackTrace();
    }
    
  }

  
  /* Instance methods */

  /* Primary OAI namespaces */

  /**
   * Get the OAI response as a DOM object
   * 
   * @return the DOM for the OAI response
   */
  public Document getDocument() {
    return document;
  }


  /**
   * Get the OAI errors
   * 
   * @return a NodeList of /oai:OAI-PMH/oai:error elements
   * @throws TransformerException
   */
  public NodeList getErrors() throws TransformerException {
    if (SCHEMA_LOCATION_V2_0.equals(getSchemaLocation())) {
      return getNodeList("/oai20:OAI-PMH/oai20:error");
    } 
    else {
      return null;
    }
  }


  /**
   * Get a NodeList containing the nodes in the response DOM for the specified
   * xpath
   * 
   * @param xpath
   * @return the NodeList for the xpath into the response DOM
   * @throws TransformerException
   */
  public NodeList getNodeList(String xpath) throws TransformerException {
    Document document = getDocument();
    return XPathAPI.selectNodeList(document, xpath, namespaceElement);
  }


  /**
   * Get the OAI request URL for this response
   * 
   * @return the OAI request URL as a String
   */
  public String getRequestURL() {
    return requestURL;
  }


  /**
   * Get the xsi:schemaLocation for the OAI response
   * 
   * @return the xsi:schemaLocation value
   */
  public String getSchemaLocation() {
    return schemaLocation;
  }


  /**
   * Get the String value for the given XPath location in the response DOM
   * 
   * @param xpath
   * @return a String containing the value of the XPath location.
   * @throws TransformerException
   */
  public String getSingleString(String xpath) throws TransformerException {
    Document document = getDocument();
    org.apache.xpath.objects.XObject xobject;
    
    xobject = XPathAPI.eval(document, xpath, namespaceElement);
    String str = xobject.str();
    
    return str;
  }


  /**
   * Preforms the OAI request for this OAI-PMH verb
   * 
   * @throws IOException
   * @throws ParserConfigurationException
   * @throws SAXException
   * @throws TransformerException
   */
  public void runVerb() 
          throws IOException, ParserConfigurationException, 
                 SAXException, TransformerException {
    //logger.debug("requestURL=" + requestURL);
    InputStream in = null;
    URL url = new URL(requestURL);
    HttpURLConnection con = null;
    int responseCode = 0;
    
    do {
      con = (HttpURLConnection) url.openConnection();
      con.setRequestProperty("User-Agent", "OAIHarvester/2.0");
      con.setRequestProperty("Accept-Encoding", "compress, gzip, identify");
      
      try {
        responseCode = con.getResponseCode();
        //logger.debug("responseCode=" + responseCode);
      } 
      catch (FileNotFoundException e) {
        // assume it's a 503 response
        logger.info(requestURL, e);
        responseCode = HttpURLConnection.HTTP_UNAVAILABLE;
      }

      if (responseCode == HttpURLConnection.HTTP_UNAVAILABLE) {
        long retrySeconds = con.getHeaderFieldInt("Retry-After", -1);
        
        if (retrySeconds == -1) {
          long now = (new Date()).getTime();
          long retryDate = con.getHeaderFieldDate("Retry-After", now);
          retrySeconds = retryDate - now;
        }
        
        if (retrySeconds == 0) { // Apparently, it's a bad URL
          throw new FileNotFoundException("Bad URL?");
        }
        
        System.err.println("Server response: Retry-After=" + retrySeconds);
        
        if (retrySeconds > 0) {
          try {
            Thread.sleep(retrySeconds * 1000);
          } 
          catch (InterruptedException ex) {
            ex.printStackTrace();
          }
        }
        
      }
    } while (responseCode == HttpURLConnection.HTTP_UNAVAILABLE);
    
    String contentEncoding = con.getHeaderField("Content-Encoding");
    //logger.debug("contentEncoding=" + contentEncoding);
    if ("compress".equals(contentEncoding)) {
      ZipInputStream zis = new ZipInputStream(con.getInputStream());
      zis.getNextEntry();
      in = zis;
    } 
    else if ("gzip".equals(contentEncoding)) {
      in = new GZIPInputStream(con.getInputStream());
    } 
    else if ("deflate".equals(contentEncoding)) {
      in = new InflaterInputStream(con.getInputStream());
    } 
    else {
      in = con.getInputStream();
    }

    InputSource data = new InputSource(in);

    Thread t = Thread.currentThread();
    DocumentBuilder builder = builderMap.get(t);
    
    if (builder == null) {
      builder = documentBuilderFactory.newDocumentBuilder();
      builderMap.put(t, builder);
    }
    
    document = builder.parse(data);

    String singleString = getSingleString("/*/@xsi:schemaLocation");
    StringTokenizer tokenizer = new StringTokenizer(singleString, " ");
    StringBuffer sb = new StringBuffer();
    
    while (tokenizer.hasMoreTokens()) {
      if (sb.length() > 0) sb.append(" ");
      sb.append(tokenizer.nextToken());
    }
    
    String schemaLocationStr = sb.toString();
    this.schemaLocation = schemaLocationStr;
  }


  /**
   * Transform the document content to a string and return it.
   * 
   * @return returnString - the string that results from transforming the
   *                        document
   */
  public String toString() {
    Document document = getDocument();
    Source source = new DOMSource(document);
    StringWriter stringWriter = new StringWriter();
    Result result = new StreamResult(stringWriter);
    
    try {
      Transformer idTransformer = transformerFactory.newTransformer();
      idTransformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
      idTransformer.transform(source, result);
      String returnString = stringWriter.toString();
      return returnString;
    } 
    catch (TransformerException e) {
      return e.getMessage();
    }
  }
  
}
