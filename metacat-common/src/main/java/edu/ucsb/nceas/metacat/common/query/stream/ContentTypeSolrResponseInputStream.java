/**
 *  Copyright: 2013 Regents of the University of California and the
 *             National Center for Ecological Analysis and Synthesis
 *    Authors: Ben Leinfelder
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
package edu.ucsb.nceas.metacat.common.query.stream;

import java.io.IOException;
import java.io.InputStream;

/**
 * A wrapper class to wrap an input stream object into a ContentTypeInputStream object
 * @author tao
 *
 */
public class ContentTypeSolrResponseInputStream extends InputStream implements ContentTypeInputStream{
    private String contentType;
    private InputStream inputStream;
    
    /**
     * Constructor
     * @param inputStream  the input stream object will be wrapped
     */
    public ContentTypeSolrResponseInputStream(InputStream inputStream) {
        this.inputStream = inputStream;
    }
    
    @Override
    public int available() throws IOException {
        return inputStream.available();
    }
    
    @Override
    public void close() throws IOException {
        inputStream.close();
    }
    
    @Override
    public void mark(int readlimit) {
        inputStream.mark(readlimit);
    }
    
    @Override
    public boolean markSupported() {
        return inputStream.markSupported();
    }
    
    @Override
    public int read() throws IOException {
        return inputStream.read();
    }
    
    @Override
    public int read(byte[] b) throws IOException {
        return inputStream.read(b);
    }
    
    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        return inputStream.read(b, off, len);
    }
    
    @Override
    public void reset() throws IOException {
        inputStream.reset();
    }
    
    @Override
    public long skip(long n) throws IOException {
        return inputStream.skip(n);
    }
    

    
    @Override
    public String getContentType() {
        return contentType;
    }

    @Override
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

}
