/**
 *  '$RCSfile$'
 *  Copyright: 2008 Regents of the University of California and the
 *             National Center for Ecological Analysis and Synthesis
 *
 * Author: Benjamin Leinfelder 
 * '$Date:  $'
 * '$Revision:  $'
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

package edu.ucsb.nceas.metacat.dataquery;

import java.sql.ResultSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ecoinformatics.datamanager.DataManager;
import org.ecoinformatics.datamanager.database.DatabaseConnectionPoolInterface;
import org.ecoinformatics.datamanager.database.Query;
import org.ecoinformatics.datamanager.database.Union;
import org.ecoinformatics.datamanager.dataquery.DataquerySpecification;
import org.ecoinformatics.datamanager.download.EcogridEndPointInterface;

/**
 * Class to query data
 */
public class DataQuery {

	private static Log log = LogFactory.getLog(DataQuery.class);

	private EcogridEndPointInterface endPointInfo;

	private DatabaseConnectionPoolInterface connectionPool;

	private DataManager dataManager;

	private String parserName = "org.apache.xerces.parsers.SAXParser";

	/**
	 * empty constructor to initialize query
	 */
	public DataQuery() {
		// initialize the endpoint, not authenticated
		endPointInfo = new MetacatEcogridEndPoint();
		init();
	}
	
	public DataQuery(String sessionId) {
		// initialize the necessary parts
		endPointInfo = new MetacatAuthenticatedEcogridEndPoint(sessionId);
		init();
	}
	
	private void init() {
		connectionPool = MetacatDatabaseConnectionPoolFactory.getDatabaseConnectionPoolInterface();
		dataManager = 
			DataManager.getInstance(connectionPool, connectionPool.getDBAdapterName());
	}

	public ResultSet executeQuery(String xml) throws Exception {
		
		long startTime = System.currentTimeMillis();
		
		// parse the query
		DataquerySpecification specification = 
			new DataquerySpecification(xml, parserName, connectionPool, endPointInfo);

		long endTime = System.currentTimeMillis();
		log.debug((endTime - startTime) + " ms to parse query");
		startTime = System.currentTimeMillis();
		
		// get the results
		ResultSet resultset = null;

		Union union = specification.getUnion();

		if (union != null) {
			resultset = 
				dataManager.selectData(
						union, 
						specification.getDataPackages());
		} else {
			Query query = specification.getQuery();
			resultset = 
				dataManager.selectData(
						query, 
						specification.getDataPackages());
		}

		endTime = System.currentTimeMillis();
		log.debug((endTime - startTime) + " ms to select results");
		
		return resultset;

	}

}
