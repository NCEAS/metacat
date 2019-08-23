/**
 *  '$RCSfile$'
 *  Copyright: 2006 Regents of the University of California and the
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
package edu.ucsb.nceas.metacat;

import java.io.File;
import java.util.Hashtable;

import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.metacat.shared.ServiceException;
import edu.ucsb.nceas.utilities.FileUtil;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;

/**
 * Generate a pathquery document representing a metacat query for a list of
 * document IDs.
 */
public class DocumentIdQuery {

	/**
	 * Create an squery parameters table using an already-initialized hashtable
	 * 
	 * @param docidList
	 *            an array of document identifiers to search for
	 * @param params
	 *            the hashtable to add the query parameters to.
	 * 
	 */
	public static Hashtable createDocidQueryParams(String[] docidList, Hashtable params) {
		params = getDefaultQueryParams();
		if (docidList != null) {
			params.put("/eml/@packageId", docidList);
		}
		return params;
	}

	/**
	 * Create an squery using some preset values for the query parameters, only
	 * passing in the document ids to be searched.
	 * 
	 * @param docidList
	 *            an array of document identifiers to search for
	 */
	public static String createDocidQuery(String[] docidList)
			throws PropertyNotFoundException {
		String pathQuery = "";
		Hashtable params = getDefaultQueryParams();
		if (docidList != null) {
			params.put("/eml/@packageId", docidList);
		}

		pathQuery = DBQuery.createSQuery(params);
		return pathQuery;
	}

	/**
	 * Create a paramter list containing default parameters for a query
	 * 
	 * @return Hashtable containing the default parameters
	 */
	public static Hashtable getDefaultQueryParams() {
		Hashtable params = new Hashtable();

		String[] operator = new String[1];
		operator[0] = "UNION";
		params.put("operator", operator);

		String[] doctypes = new String[8];
		doctypes[0] = "eml://ecoinformatics.org/eml-2.0.1";
		doctypes[1] = "eml://ecoinformatics.org/eml-2.0.0";
		doctypes[2] = "eml://ecoinformatics.org/eml-2.1.0";
		doctypes[3] = "eml://ecoinformatics.org/eml-2.1.1";
		doctypes[4] = "-//ecoinformatics.org//eml-dataset-2.0.0beta6//EN";
		doctypes[5] = "-//ecoinformatics.org//eml-dataset-2.0.0beta4//EN";
		doctypes[6] = "metadata";
		doctypes[7] = "https://ecoinformatics.org/eml-2.2.0";
		params.put("returndoctype", doctypes);

		String[] fields = new String[12];
		fields[0] = "originator/individualName/surName";
		fields[1] = "originator/individualName/givenName";
		fields[2] = "creator/individualName/surName";
		fields[3] = "creator/individualName/givenName";
		fields[4] = "originator/organizationName";
		fields[5] = "creator/organizationName";
		fields[6] = "dataset/title";
		fields[7] = "keyword";
		fields[8] = "idinfo/citation/citeinfo/title";
		fields[9] = "idinfo/citation/citeinfo/origin";
		fields[10] = "idinfo/keywords/theme/themekey";
		fields[11] = "dataset/pubDate";
		params.put("returnfield", fields);

		return params;
	}

	/**
	 * Main method used for testing the class output
	 * 
	 * @param args
	 *            no arguments used in this main method
	 */
	public static void main(String[] args) {
		String CONFIG_DIR = "lib";
		File dirPath = new File(CONFIG_DIR);
		try {
			PropertyService.getInstance(dirPath.getPath() + FileUtil.getFS() + "metacat.properties");
		} catch (ServiceException ioe) {
			System.err.println("Error in loading properties: " + ioe.getMessage());
		}

		String[] ids = new String[3];
		ids[0] = "ces_dataset.23.1";
		ids[1] = "knb-lter-vcr.97.1";
		ids[2] = "obfs.400.1";

		String pathquery = null;
		try {
			pathquery = createDocidQuery(ids);
		} catch (PropertyNotFoundException pnfe) {
			System.out.println("Could not create doc id query: " + pnfe.getMessage());
		}
		System.out.println(pathquery);
	}

}
