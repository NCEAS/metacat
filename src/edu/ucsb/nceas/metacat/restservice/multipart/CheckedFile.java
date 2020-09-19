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

import org.dataone.service.types.v1.Checksum;

/**
 * A file object with the expected checksum
 * @author tao
 *
 */
public class CheckedFile extends File {
    
    private Checksum checksum = null;
    
    /**
     * Constructor
     * @param path 
     * @param checksum
     */
    public CheckedFile(String path, Checksum checksum) {
        super(path);
        this.checksum = checksum;
    }
    
    /**
     * Get the checksum of this file
     * @return the checksum of this file
     */
    public Checksum getChecksum() {
        return this.checksum;
    }

}
