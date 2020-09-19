/**
 *  '$RCSfile$'
 *  Copyright: 2019 Regents of the University of California and the
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
package edu.ucsb.nceas.metacat.restservice.multipart;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import org.dataone.service.types.v1.Checksum;


/**
 * A FileInputStream class with more information, such as the object of source file itself and the expected checksum of the source file. 
 * @author tao
 *
 */
public class DetailedFileInputStream extends FileInputStream {
    private Checksum expectedChecksum = null;
    private File file = null;
    
    /**
     * Constructor
     * @param file  the source file where the input stream comes from
     * @param expectedChecksum  the expected checksum of the source file 
     * @throws FileNotFoundException
     */
    public DetailedFileInputStream (File file, Checksum expectedChecksum) throws FileNotFoundException {
        super(file);
        this.file = file;
        this.expectedChecksum = expectedChecksum;
    }
    
    /**
     * Get the expected checksum of the source file
     * @return the checksum of the source file
     */
    public Checksum getExpectedChecksum() {
        return this.expectedChecksum;
    }

    /**
     * Get the source file
     * @return the source file object
     */
    public File getFile() {
        return file;
    }
    
    
}
