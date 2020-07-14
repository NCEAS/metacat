/**
 *  Copyright: 2020 Regents of the University of California and the
 *             National Center for Ecological Analysis and Synthesis
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
package edu.ucsb.nceas.metacat.dataone.quota;

import org.dataone.bookkeeper.api.Usage;

/**
 * A child class of the Usage class of BookKeeper. It has a new field - local id which represents sequence id in the
 * local quota_usage table
 * @author tao
 *
 */
public class LocalUsage extends Usage {
    private int localId = -1;
    
    /**
     * Get the local id associated with the local usage
     * @return the local id
     */
    public int getLocalId() {
        return this.localId;
    }
    
    /**
     * Set the local id
     * @param localId  the local id
     */
    public void setLocalId(int localId) {
        this.localId = localId;
    }
    
}
