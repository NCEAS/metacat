/**
 *  '$RCSfile$'
 *    Purpose: A Class that implements utility methods like:
 *             1/ Reding all doctypes from db connection
 *             2/ Reading DTD or Schema file from Metacat catalog system
 *             3/ Reading Lore type Data Guide from db connection
 *  Copyright: 2000 Regents of the University of California and the
 *             National Center for Ecological Analysis and Synthesis
 *    Authors: Jivka Bojilova
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

package edu.ucsb.nceas.metacat.shared;


/**
 * A suite of utility classes for querying DB
 * 
 */
public abstract class BaseService {
	protected String _serviceName = null;

	// package level method reporting if service is refreshable.  Basically,
	// we only want ServiceService calling this.
	public abstract boolean refreshable();
	
	// subclass must define doRefresh.  It is only called from the refresh() method.
	protected abstract void doRefresh() throws ServiceException;
	
	// package level method to refresh service.  We only want ServiceService 
	// calling this.
	public void refresh() throws ServiceException{
		if (refreshable()) {
			doRefresh();
		} else {
			throw new ServiceException("Service: " + _serviceName + " is not refreshable");
		}
	}
	
	public abstract void stop() throws ServiceException;

}
