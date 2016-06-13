/**
 *  '$RCSfile$'
 *  Copyright: 2013 Regents of the University of California and the
 *             National Center for Ecological Analysis and Synthesis
 *
 *   '$Author: leinfelder $'
 *     '$Date: 2014-11-19 12:52:14 -0800 (Wed, 19 Nov 2014) $'
 * '$Revision: 8983 $'
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

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import edu.ucsb.nceas.metacat.admin.AdminException;
import edu.ucsb.nceas.metacat.admin.upgrade.solr.SolrSchemaModificationException;
import edu.ucsb.nceas.metacat.admin.upgrade.solr.SolrSchemaUpgrader;
import edu.ucsb.nceas.metacat.shared.ServiceException;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;



/**
 * Upgrade the application to version 2.7.0 
 * @author leinfelder
 *
 */
public class Upgrade2_7_0 implements UpgradeUtilityInterface {
    
    public boolean upgrade() throws AdminException {
        boolean success = true;
        try {
            SolrSchemaUpgrader upgrader = new SolrSchemaUpgrader();
            upgrader.upgrade();
        } catch (PropertyNotFoundException e) {
            throw new AdminException(e.getMessage());
        } catch (IOException e){
            throw new AdminException(e.getMessage());
        } catch(NoSuchAlgorithmException  e) {
            throw new AdminException(e.getMessage());
        } catch (ServiceException  e) {
            throw new AdminException(e.getMessage());
        } catch ( SolrSchemaModificationException e) {
            throw e;
        }
        return success;
    }
    
}
