package edu.ucsb.nceas.metacat.oaipmh.harvester;

import java.io.IOException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import org.xml.sax.SAXException;


/**
 * This class represents an Identify response on either the server or on the
 * client
 * 
 * @author Jeffrey A. Young, OCLC Online Computer Library Center
 */
public class Identify extends HarvesterVerb {

  /**
   * Mock object constructor (for unit testing purposes)
   */
  public Identify() {
    super();
  }


  /**
   * Client-side Identify verb constructor
   * 
   * @param baseURL
   *          the baseURL of the server to be queried
   * @exception MalformedURLException
   *              the baseURL is bad
   * @exception IOException
   *              an I/O error occurred
   */
  public Identify(String baseURL) throws IOException,
      ParserConfigurationException, SAXException, TransformerException {
    super(getRequestURL(baseURL));
  }


  /**
   * Get the oai:protocolVersion value from the Identify response
   * 
   * @return the oai:protocolVersion value
   * @throws TransformerException
   * @throws NoSuchFieldException
   */
  public String getProtocolVersion() throws TransformerException,
      NoSuchFieldException {
    if (SCHEMA_LOCATION_V2_0.equals(getSchemaLocation())) {
      return getSingleString(
                           "/oai20:OAI-PMH/oai20:Identify/oai20:protocolVersion"
                            );
    }
    else {
      throw new NoSuchFieldException(getSchemaLocation());
    }
  }


  /**
   * generate the Identify request URL for the specified baseURL
   * 
   * @param baseURL
   * @return the requestURL
   */
  private static String getRequestURL(String baseURL) {
    StringBuffer requestURL = new StringBuffer(baseURL);
    requestURL.append("?verb=Identify");
    return requestURL.toString();
  }
}
