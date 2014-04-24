/**
 *  '$RCSfile$'
 *  Copyright: 2013 Regents of the University of California and the
 *             National Center for Ecological Analysis and Synthesis
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

package edu.ucsb.nceas.metacat.admin.upgrade;

import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.SystemMetadata;

import edu.ucsb.nceas.metacat.DBUtil;
import edu.ucsb.nceas.metacat.DocumentImpl;
import edu.ucsb.nceas.metacat.IdentifierManager;
import edu.ucsb.nceas.metacat.admin.AdminException;
import edu.ucsb.nceas.metacat.dataone.DOIService;
import edu.ucsb.nceas.metacat.dataone.hazelcast.HazelcastService;
import edu.ucsb.nceas.metacat.util.DocumentUtil;



/**
 * Updates existing DOI registrations for EML versions
 * @author leinfelder
 *
 */
public class UpdateDOI implements UpgradeUtilityInterface {
    
	private static Log log = LogFactory.getLog(UpdateDOI.class);

	private int serverLocation = 1;

	public int getServerLocation() {
		return serverLocation;
	}

	public void setServerLocation(int serverLocation) {
		this.serverLocation = serverLocation;
	}
	
	private void updateDOIRegistration(List<String> identifiers) {
		for (String pid: identifiers) {
			try {

				String docid = DocumentUtil.getDocIdFromAccessionNumber(pid);
				int rev = DocumentUtil.getRevisionFromAccessionNumber(pid);
				String guid = IdentifierManager.getInstance().getGUID(docid, rev);
				Identifier identifier = new Identifier();
				identifier.setValue(guid);
				SystemMetadata sysMeta = HazelcastService.getInstance().getSystemMetadataMap().get(pid);
				DOIService.getInstance().registerDOI(sysMeta);
			} catch (Exception e) {
				// what to do? nothing
				e.printStackTrace();
				continue;
			}
			
		}
	}
	
    public boolean upgrade() throws AdminException {
        boolean success = true;
        
        try {
        	// get only local ids for this server
            List<String> idList = null;
            
            idList = DBUtil.getAllDocidsByType(DocumentImpl.EML2_0_0NAMESPACE, true, serverLocation);
            Collections.sort(idList);
            updateDOIRegistration(idList);
            
            idList = DBUtil.getAllDocidsByType(DocumentImpl.EML2_0_1NAMESPACE, true, serverLocation);
            Collections.sort(idList);
            updateDOIRegistration(idList);
            
            idList = DBUtil.getAllDocidsByType(DocumentImpl.EML2_1_0NAMESPACE, true, serverLocation);
            Collections.sort(idList);
            updateDOIRegistration(idList);
            
            idList = DBUtil.getAllDocidsByType(DocumentImpl.EML2_1_1NAMESPACE, true, serverLocation);
            Collections.sort(idList);
            updateDOIRegistration(idList);
            
		} catch (Exception e) {
			String msg = "Problem updating DOIs: " + e.getMessage();
			log.error(msg, e);
			success = false;
			throw new AdminException(msg);
		}
        
        
        return success;
    }
    
}
