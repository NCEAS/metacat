/**
 *  '$RCSfile$'
 *  Copyright: 2020 Regents of the University of California and the
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
package edu.ucsb.nceas.metacat.doi.datacite.relation;

import java.util.List;
import java.util.Vector;


/**
 * A class represents the citation JSON query
 * @author tao
 *
 */
public class CitationsFilter {
    private String filterType = "dataset";
    private String interpretAs = "list";
    private List<String> values = new Vector<String>();
    
    /**
     * Get the filter type
     * @return  the type of filter
     */
    public String getFilterType() {
        return filterType;
    }
    
    /**
     * Set the filter type 
     * @param filterType
     */
    public void setFilterType(String filterType) {
        this.filterType = filterType;
    }
    
    /**
     * Get the field of interpretAs
     * @return  the field of interpretAs
     */
    public String getInterpretAs() {
        return interpretAs;
    }
    
    /**
     * Set the the field of interpretAs
     * @param interpretAs
     */
    public void setInterpretAs(String interpretAs) {
        this.interpretAs = interpretAs;
    }
    
    /**
     * Get the values of the filter
     * @return the list of values
     */
    public List<String> getValues() {
        return values;
    }
    
    /**
     * Set the values of the filter
     * @param values
     */
    public void setValues(List<String> values) {
        this.values = values;
    }
}
