/**
 *  '$RCSfile$'
 *  Copyright: 2013 Regents of the University of California and the
 *             National Center for Ecological Analysis and Synthesis
 *
 *   '$Author: tao $'
 *     '$Date: 2012-02-08 10:44:45 -0800 (Wed, 08 Feb 2012) $'
 * '$Revision: 6996 $'
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
package edu.ucsb.nceas.metacat.admin.upgrade.solr;

import edu.ucsb.nceas.metacat.admin.AdminException;

/**
 * An exception happens when an administrator modified a the schema.xml in the solr home.
 * @author tao
 *
 */
public class SolrSchemaModificationException extends AdminException {
    
    /**
     * Constructor
     * @param error  the eror message.
     */
    SolrSchemaModificationException(String error) {
        super(error);
    }
}
