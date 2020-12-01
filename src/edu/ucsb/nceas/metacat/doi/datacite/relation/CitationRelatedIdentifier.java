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

/**
 * A class represents other identifiers relating to the citation
 * @author tao
 *
 */
public class CitationRelatedIdentifier {
    private String identifiier = null;
    private String relation_type = null;
    
    /**
     * Get the related identifier 
     * @return  the identifier of the relationship 
     */
    public String getIdentifiier() {
        return identifiier;
    }
    
    /**
     * Set the related identifier
     * @param identifiier  the identifier will be set
     */
    public void setIdentifiier(String identifiier) {
        this.identifiier = identifiier;
    }
    
    /**
     * Get the relationship of the identifier to the citation
     * @return relation_type 
     */
    public String getRelation_type() {
        return relation_type;
    }
    
    /**
     * Set the relationship for the identifiers
     * @param relation_type  the relationship will be set
     */
    public void setRelation_type(String relation_type) {
        this.relation_type = relation_type;
    }

}
