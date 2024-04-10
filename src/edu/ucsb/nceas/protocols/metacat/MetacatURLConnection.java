package edu.ucsb.nceas.protocols.metacat;

import java.io.IOException;
import java.net.URL;

/** 
 * Handle requests for metacat scheme URLs
 */
public class MetacatURLConnection extends java.net.URLConnection
{
  /** 
   * Construct a new metacat scheme URL connection
   *
   * @param u the URL to which to connect
   */
  public MetacatURLConnection(URL u)
  {
    super(u);
  }

  /** 
   * Set the request header for the URL
   */
  public void setRequestHeader(String name, String value)
  {
  }

  /** 
   * Make a connection to the URL
   */
  public void connect() throws IOException
  {
    super.connected = true;
  }
}
