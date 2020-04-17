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
 * A class represents the citation query ( a list of filter)
 * @author tao
 *
 */
public class CitationsQuery {
    private List<CitationsFilter> filterBy = new Vector<CitationsFilter>();

    /**
     * Get the filterBy list
     * @return the list of filter
     */
    public List<CitationsFilter> getFilterBy() {
        return filterBy;
    }

    /**
     * Set the filter list
     * @param filterBy the list of liter will be set
     */
    public void setFilterBy(List<CitationsFilter> filterBy) {
        this.filterBy = filterBy;
    }
    
    /**
     * Add a new filter to the filter list
     * @param filter  the filter will be added
     */
    public void addFilter(CitationsFilter filter) {
        this.filterBy.add(filter);
    }

}
