/**
 *  '$RCSfile$'
 *  Copyright: 2000 Regents of the University of California and the
 *              National Center for Ecological Analysis and Synthesis
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

package edu.ucsb.nceas.metacat.client;

import java.io.Reader;

/**
 *  This class is a factory which allows the caller to create an instance of
 *  a Metacat object for accessing a metacat server.
 */
public class MetacatFactory
{
    private static final String metacatClientClass = 
         "edu.ucsb.nceas.metacat.client.MetacatClient";

    /**
     *  Create a new instance of a Metacat object of raccessing a server.
     *
     *  @param metacatUrl the url location of the metacat server
     *  @throws MetacatInaccessibleException when the metacat server can not
     *                    be reached
     */
    public static Metacat createMetacatConnection(String metacatUrl) 
           throws MetacatInaccessibleException
    {
        Metacat m = null;
        try {
            Class c = Class.forName(metacatClientClass);
            m = (Metacat)c.newInstance();
        } catch (InstantiationException e) {
            throw new MetacatInaccessibleException(e.getMessage());
        } catch (IllegalAccessException e) {
            throw new MetacatInaccessibleException(e.getMessage());
        } catch (ClassNotFoundException e) {
            throw new MetacatInaccessibleException(e.getMessage());
        }

        m.setMetacatUrl(metacatUrl);

        return m;
    }
}
