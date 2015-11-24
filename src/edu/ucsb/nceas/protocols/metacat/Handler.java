/**
 *  '$RCSfile$'
 *    Purpose: Handle Metacat URL connections
 *  Copyright: 2000 Regents of the University of California and the
 *             National Center for Ecological Analysis and Synthesis
 *    Authors: Matt Jones
 *
 *   '$Author$'
 *     '$Date$'
 * '$Revision$'
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

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
