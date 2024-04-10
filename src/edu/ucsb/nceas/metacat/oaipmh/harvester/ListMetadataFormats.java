package edu.ucsb.nceas.metacat.oaipmh.harvester;

import java.io.IOException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import org.xml.sax.SAXException;


/**
 * This class represents an ListMetadataFormats response on either the server or
 * on the client
 * 
 * @author Jeffrey A. Young, OCLC Online Computer Library Center
 */
public class ListMetadataFormats extends HarvesterVerb {

  /**
   * Mock object constructor (for unit testing purposes)
   */
  public ListMetadataFormats() {
    super();
  }


  /**
   * Client-side ListMetadataFormats verb constructor
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
  public ListMetadataFormats(String baseURL) throws IOException,
      ParserConfigurationException, SAXException, TransformerException {
    this(baseURL, null);
  }


  /**
   * Client-side ListMetadataFormats verb constructor (identifier version)
   * 
   * @param baseURL
   * @param identifier
   * @throws IOException
   * @throws ParserConfigurationException
   * @throws SAXException
   * @throws TransformerException
   */
  public ListMetadataFormats(String baseURL, String identifier)
      throws IOException, ParserConfigurationException, SAXException,
      TransformerException {
    super(getRequestURL(baseURL, identifier));
  }


  /**
   * Construct the query portion of the http request
   * 
   * @return a String containing the query portion of the http request
   */
  private static String getRequestURL(String baseURL, String identifier) {
    StringBuffer requestURL = new StringBuffer(baseURL);
    requestURL.append("?verb=ListMetadataFormats");
    if (identifier != null)
      requestURL.append("&identifier=").append(identifier);
    return requestURL.toString();
  }
}
