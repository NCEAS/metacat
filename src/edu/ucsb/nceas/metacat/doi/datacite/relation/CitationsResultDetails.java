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
 * A class represents the citation resultDetails property in the json code response from the metric server.
 * It contains a list of citations. 
 * @author tao
 *
 */
public class CitationsResultDetails {
    private List<Citation> citations = null;

    /**
     * Get the list of citations in the result details property
     * @return  the list of citations
     */
    public List<Citation> getCitations() {
        return citations;
    }

    /**
     * Set the list the citations to the result details property
     * @param citations  the list will be set
     */
    public void setCitations(List<Citation> citations) {
        this.citations = citations;
    }

}
