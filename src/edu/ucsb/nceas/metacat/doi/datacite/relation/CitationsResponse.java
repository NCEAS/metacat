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

/**
 * The class represents the response for the citation query
 * @author tao
 *
 */
public class CitationsResponse {
    private CitationsResultDetails resultDetails = null;

    /**
     * Get the resultDetails from the response
     * @return  the resultDetails
     */
    public CitationsResultDetails getResultDetails() {
        return resultDetails;
    }

    /**
     * Set the resultDetails to the response
     * @param resultDetails  the resultDetails will be set
     */
    public void setResultDetails(CitationsResultDetails resultDetails) {
        this.resultDetails = resultDetails;
    }

}
