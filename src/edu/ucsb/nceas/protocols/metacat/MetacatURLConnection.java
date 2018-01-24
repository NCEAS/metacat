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
