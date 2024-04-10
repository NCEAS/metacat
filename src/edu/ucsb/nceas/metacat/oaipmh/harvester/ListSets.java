package edu.ucsb.nceas.metacat.oaipmh.harvester;

import java.io.IOException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import org.xml.sax.SAXException;


/**
 * This class represents an ListSets response on either the server or on the
 * client
 * 
 * @author Jeffrey A. Young, OCLC Online Computer Library Center
 */
public class ListSets extends HarvesterVerb {

  /**
   * Mock object constructor (for unit testing purposes)
   */
  public ListSets() {
    super();
  }


  /**
   * Client-side ListSets verb constructor
   * 
   * @param baseURL
   *          the baseURL of the server to be queried
   * @exception MalformedURLException
   *              the baseURL is bad
   * @exception IOException
   *              an I/O error occurred
   */
  public ListSets(String baseURL) throws IOException,
      ParserConfigurationException, SAXException, TransformerException {
    super(getRequestURL(baseURL));
  }


  /**
   * Get the oai:resumptionToken from the response
   * 
   * @return the oai:resumptionToken as a String
   * @throws TransformerException
   * @throws NoSuchFieldException
   */
  public String getResumptionToken() throws TransformerException,
      NoSuchFieldException {
    if (SCHEMA_LOCATION_V2_0.equals(getSchemaLocation())) {
      return getSingleString("/oai20:OAI-PMH/oai20:ListSets/oai20:resumptionToken");
    } else {
      throw new NoSuchFieldException(getSchemaLocation());
    }
  }


  /**
   * Generate a ListSets request for the given baseURL
   * 
   * @param baseURL
   * @return
   */
  private static String getRequestURL(String baseURL) {
    StringBuffer requestURL = new StringBuffer(baseURL);
    requestURL.append("?verb=ListSets");
    return requestURL.toString();
  }
}
