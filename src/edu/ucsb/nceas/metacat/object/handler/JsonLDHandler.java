/**
 *  '$RCSfile$'
 *  Copyright: 2021 Regents of the University of California and the
 *              National Center for Ecological Analysis and Synthesis
 *
 *   '$Author:  $'
 *     '$Date:  $'
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
package edu.ucsb.nceas.metacat.object.handler;

import java.io.IOException;
import java.io.InputStream;

import org.dataone.service.exceptions.InvalidRequest;

import com.github.jsonldjava.utils.JsonUtils;

/**
 * The handler of JSON-LD objects to save bites into disk
 * @author tao
 *
 */
public class JsonLDHandler extends NonXMLMetadataHandler {
    
    /**
     * Default constructor
     */
    public JsonLDHandler() {
        
    }
    
    @Override
    public boolean validate(InputStream source) throws InvalidRequest {
        try {
            Object jsonObject = JsonUtils.fromInputStream(source);
        } catch (IOException e) {
            throw new InvalidRequest("1102", "NonXMLMetadataHandler.save - the metadata object is invalid: " + e.getMessage());
        }
        return true;
    }

}
