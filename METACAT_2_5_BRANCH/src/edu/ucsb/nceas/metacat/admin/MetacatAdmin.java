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

package edu.ucsb.nceas.metacat.admin;

import java.util.Vector;

import javax.servlet.http.HttpServletRequest;

/**
 * A suite of utility classes for querying DB
 * 
 */
public abstract class MetacatAdmin {
	
	/**
	 * Require subclasses to implement a properties validator.
	 * 
	 * @return a vector holding error message for any fields that fail
	 *         validation.
	 */
	protected abstract Vector<String> validateOptions(HttpServletRequest request);

}
