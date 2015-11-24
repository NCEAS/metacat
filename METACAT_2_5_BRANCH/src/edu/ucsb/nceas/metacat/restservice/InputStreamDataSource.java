/**
 *  Copyright: 2010 Regents of the University of California and the
 *              National Center for Ecological Analysis and Synthesis
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
package edu.ucsb.nceas.metacat.restservice;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.activation.DataSource;

/**
 * Encapsulate an InputStream within a DataSource interface so that it is 
 * accessible to MIME processors.
 * 
 * @author Matthew Jones
 */
public class InputStreamDataSource implements DataSource {
    private String name;
    private InputStream stream;
    private boolean readOnce;
    
    public InputStreamDataSource(String name, InputStream stream) {
        super();
        this.name = name;
        this.stream = stream;
        this.readOnce = false;
    }

    public String getContentType() {
        return "application/octet-stream";
    }

    public InputStream getInputStream() throws IOException {
        if (readOnce) {
            throw new IOException("Only call getInputStream() once.");
        }
        readOnce = true;
        
        return stream;
    }

    public String getName() {
        return this.name;
    }

    public OutputStream getOutputStream() throws IOException {
        throw new IOException("Can't get an OutputStream from an InputStreamDataSource.");
    }
}
