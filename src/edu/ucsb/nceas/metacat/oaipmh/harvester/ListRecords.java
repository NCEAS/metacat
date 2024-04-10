package edu.ucsb.nceas.metacat.oaipmh.harvester;

import java.io.IOException;
import java.net.URLEncoder;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import org.xml.sax.SAXException;


/**
 * This class represents an ListRecords response on either the server or on the
 * client
 * 
 * @author Jeffrey A. Young, OCLC Online Computer Library Center
 */
public class ListRecords extends HarvesterVerb {

  /**
   * Mock object constructor (for unit testing purposes)
   */
  public ListRecords() {
    super();
  }


  /**
   * Client-side ListRecords verb constructor
   * 
   * @param baseURL
   *          the baseURL of the server to be queried
   * @exception MalformedURLException
   *              the baseURL is bad
   * @exception SAXException
   *              the xml response is bad
   * @exception IOException
   *              an I/O error occurred
   */
  public ListRecords(String baseURL, String from, String until,
                     String metadataPrefix, String setSpec) 
          throws IOException, ParserConfigurationException,
      SAXException, TransformerException {
    super(getRequestURL(baseURL, from, until, metadataPrefix, setSpec));
  }


  /**
   * Client-side ListRecords verb constructor (resumptionToken version)
   * 
   * @param baseURL
   * @param resumptionToken
   * @throws IOException
   * @throws ParserConfigurationException
   * @throws SAXException
   * @throws TransformerException
   */
  public ListRecords(String baseURL, String resumptionToken)
      throws IOException, ParserConfigurationException, SAXException,
      TransformerException {
    super(getRequestURL(baseURL, resumptionToken));
  }


  /**
   * Get the oai:resumptionToken from the response
   * 
   * @return the oai:resumptionToken value
   * @throws TransformerException
   * @throws NoSuchFieldException
   */
  public String getResumptionToken() throws TransformerException,
      NoSuchFieldException {
    String schemaLocation = getSchemaLocation();
    if (schemaLocation.indexOf(SCHEMA_LOCATION_V2_0) != -1) {
      return getSingleString("/oai20:OAI-PMH/oai20:ListRecords/oai20:resumptionToken");
    } 
    else {
      throw new NoSuchFieldException(schemaLocation);
    }
  }


  /**
   * Construct the query portion of the http request
   * 
   * @return a String containing the query portion of the http request
   */
  private static String getRequestURL(String baseURL, String from,
                                      String until, String metadataPrefix, 
                                      String setSpec) 
  {
    StringBuffer requestURL = new StringBuffer(baseURL);
    requestURL.append("?verb=ListRecords");
    if (from != null)
      requestURL.append("&from=").append(from);
    if (until != null)
      requestURL.append("&until=").append(until);
    if (setSpec != null)
      requestURL.append("&set=").append(setSpec);
    requestURL.append("&metadataPrefix=").append(metadataPrefix);
    return requestURL.toString();
  }


  /**
   * Construct the query portion of the http request (resumptionToken version)
   * 
   * @param baseURL
   * @param resumptionToken
   * @return
   */
  @SuppressWarnings("deprecation")
  private static String getRequestURL(String baseURL, String resumptionToken) {
    StringBuffer requestURL = new StringBuffer(baseURL);
    requestURL.append("?verb=ListRecords");
    requestURL.append("&resumptionToken=").append(
        URLEncoder.encode(resumptionToken));
    return requestURL.toString();
  }
}
