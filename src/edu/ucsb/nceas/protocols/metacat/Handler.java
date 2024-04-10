package edu.ucsb.nceas.protocols.metacat;

import java.net.URL;
import java.net.URLConnection;

/**
 * Class handling metacat scheme URLs by delegating connection requests
 * to the MetacatURLConnection class
 */
public class Handler extends java.net.URLStreamHandler
{
  /**
   * Open a new connection to the URL by delegating to the MetacatURLConnection
   *
   * @param u the URL to which a connection is requested
   */
  protected URLConnection openConnection(URL u)
  {
    //...create and return a custom 
    // URLConnection initialized
    // with a reference to the target 
    // URL object...
    return new MetacatURLConnection(u);
  }
}
