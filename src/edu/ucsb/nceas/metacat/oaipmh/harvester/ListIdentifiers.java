package edu.ucsb.nceas.metacat.oaipmh.harvester;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import org.xml.sax.SAXException;


/**
 * This class represents an ListIdentifiers response on either the server or on
 * the client.
 * 
 * @author Duane Costa, University of New Mexico, LTER Network Office
 * @author Jeffrey A. Young, OCLC Online Computer Library Center
 */
public class ListIdentifiers extends HarvesterVerb {

  
/* Constructors */
  
  /**
   * Mock object constructor (for unit testing purposes)
   */
  public ListIdentifiers() {
    super();
  }


  /**
   * Client-side ListIdentifiers verb constructor
   * 
   * @param baseURL                baseURL of the OAI-PMH provider to be queried
   * @param from                   the from date, e.g. "2000-01-01"
   * @param until                  the until date. e.g. "2009-12-31"
   * @param metadataPrefix         the metadata prefix, e.g. "oai_pmh"
   * @param setSpec                the set specifier
   * 
   * @exception MalformedURLException  the baseURL is bad
   * @exception SAXException           the xml response is bad
   * @exception IOException            an I/O error occurred
   */
  public ListIdentifiers(String baseURL, String from, String until, 
                         String metadataPrefix, String setSpec) 
          throws IOException, ParserConfigurationException,
      SAXException, TransformerException {
    super(getRequestURL(baseURL, from, until, metadataPrefix, setSpec));
  }


  /**
   * Client-side ListIdentifiers verb constructor (resumptionToken version)
   * 
   * @param baseURL                baseURL of the OAI-PMH provider to be queried
   * @param resumptionToken        the resumptionToken string, as returned by
   *                               the provider server
   * @throws IOException
   * @throws ParserConfigurationException
   * @throws SAXException
   * @throws TransformerException
   */
  public ListIdentifiers(String baseURL, String resumptionToken)
      throws IOException, ParserConfigurationException, SAXException,
      TransformerException {
    super(getRequestURL(baseURL, resumptionToken));
  }

  
/* Class methods */

  /**
   * Construct the query portion of the http request (non-resumptionToken 
   * version)
   * @param baseURL                baseURL of the OAI-PMH provider to be queried
   * @param from                   the from date, e.g. "2000-01-01"
   * @param until                  the until date. e.g. "2009-12-31"
   * @param metadataPrefix         the metadata prefix, e.g. "oai_pmh"
   * @param setSpec                the set specifier
   * 
   * @return a String containing the query portion of the http request
   */
  private static String getRequestURL(String baseURL, String from,
                                      String until, String metadataPrefix,
                                      String setSpec)
  {
    StringBuffer stringBuffer = new StringBuffer(baseURL);
    stringBuffer.append("?verb=ListIdentifiers");
    
    if (from != null) stringBuffer.append("&from=").append(from);
    if (until != null) stringBuffer.append("&until=").append(until);
    if (setSpec != null) stringBuffer.append("&set=").append(setSpec);
    stringBuffer.append("&metadataPrefix=").append(metadataPrefix);
    
    String requestURL = stringBuffer.toString();
    return requestURL;
  }


  /**
   * Construct the query portion of the http request (resumptionToken version)
   * 
   * @param baseURL                baseURL of the OAI-PMH provider to be queried
   * @param resumptionToken        the resumptionToken string, as returned by
   *                               the provider server
   * @return a String containing the query portion of the http request
   */
  private static String getRequestURL(String baseURL, String resumptionToken) 
          throws UnsupportedEncodingException 
  {
    StringBuffer stringBuffer = new StringBuffer(baseURL);
    
    stringBuffer.append("?verb=ListIdentifiers");
    stringBuffer.append("&resumptionToken=");
    stringBuffer.append(URLEncoder.encode(resumptionToken, "UTF-8"));
    
    String requestURL = stringBuffer.toString();
    return requestURL;
  }
  
  
  /* Instance methods */

  /**
   * Get the oai:resumptionToken from the response
   * 
   * @return the oai:resumptionToken value
   * @throws TransformerException
   * @throws NoSuchFieldException
   */
  public String getResumptionToken() 
          throws TransformerException, NoSuchFieldException 
  {
    String schemaLocation = getSchemaLocation();
    String resumptionToken = "";
    
    if (SCHEMA_LOCATION_V2_0.equals(schemaLocation)) {
      resumptionToken = getSingleString(
                    "/oai20:OAI-PMH/oai20:ListIdentifiers/oai20:resumptionToken"
                                       );
    } 
    else {
      throw new NoSuchFieldException(getSchemaLocation());
    }
    
    return resumptionToken;
  }
 
}
