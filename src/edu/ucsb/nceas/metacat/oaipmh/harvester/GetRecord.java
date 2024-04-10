package edu.ucsb.nceas.metacat.oaipmh.harvester;

import java.io.IOException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import org.xml.sax.SAXException;


/**
 * This class represents an GetRecord response on either the server or on the
 * client
 * 
 * @author Duane Costa, University of New Mexico, LTER Network Office
 * @author Jeffrey A. Young, OCLC Online Computer Library Center
 */
public class GetRecord extends HarvesterVerb {
  
  /* Constructors */

  /**
   * Mock object constructor (for unit testing purposes)
   */
  public GetRecord() {
    super();
  }


  /**
   * Client-side GetRecord verb constructor
   * 
   * @param baseURL                baseURL of the OAI-PMH provider to be queried
   * @param identifier             identifier of the record that we're getting
   * @param metadataPrefix         the metadata prefix, e.g. "oai_pmh"
   * 
   * @exception MalformedURLException  the baseURL is bad
   * @exception SAXException           the xml response is bad
   * @exception IOException            an I/O error occurred
   */
  public GetRecord(String baseURL, String identifier, String metadataPrefix)
      throws IOException, ParserConfigurationException, SAXException,
             TransformerException 
  {
    super(getRequestURL(baseURL, identifier, metadataPrefix));
  }

  
  /* Instance methods */

  /**
   * Get the oai:identifier from the oai:header
   * 
   * @return the oai:identifier as a String
   * @throws TransformerException
   * @throws NoSuchFieldException
   */
  public String getIdentifier()
          throws TransformerException, NoSuchFieldException 
  {
    if (SCHEMA_LOCATION_V2_0.equals(getSchemaLocation())) {
      return getSingleString(
     "/oai20:OAI-PMH/oai20:GetRecord/oai20:record/oai20:header/oai20:identifier"
                            );
    } 
    else {
      throw new NoSuchFieldException(getSchemaLocation());
    }
  }


  /**
   * Construct the query portion of the http request
   * 
   * @param baseURL                baseURL of the OAI-PMH provider to be queried
   * @param identifier             identifier of the record that we're getting
   * @param metadataPrefix         the metadata prefix, e.g. "oai_pmh"
   * @return a String containing the query portion of the http request
   */
  private static String getRequestURL(String baseURL, 
                                      String identifier,
                                      String metadataPrefix) {
    StringBuffer requestURL = new StringBuffer(baseURL);
    requestURL.append("?verb=GetRecord");
    requestURL.append("&identifier=").append(identifier);
    requestURL.append("&metadataPrefix=").append(metadataPrefix);
    return requestURL.toString();
  }
  
}
