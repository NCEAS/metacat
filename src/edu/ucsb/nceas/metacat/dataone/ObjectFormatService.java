/**
 *  '$RCSfile$'
 *  Copyright: 2000 Regents of the University of California and the
 *              National Center for Ecological Analysis and Synthesis
 *
 *   '$Author$'
 *     '$Date$'
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
package edu.ucsb.nceas.metacat.dataone;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;

import org.apache.log4j.Logger;
import org.dataone.service.util.TypeMarshaller;
import org.dataone.service.exceptions.NotFound;
import org.dataone.service.exceptions.NotImplemented;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.types.v2.ObjectFormat;
import org.dataone.service.types.v1.ObjectFormatIdentifier;
import org.dataone.service.types.v2.ObjectFormatList;
import org.jibx.runtime.JiBXException;

import edu.ucsb.nceas.metacat.DBUtil;
import edu.ucsb.nceas.metacat.DocumentImpl;
import edu.ucsb.nceas.metacat.McdbException;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;

/**
 * 
 * Implements a subset of the DataONE CNCore services in Metacat.
 * 
 * @author jones, leinfelder
 */
public class ObjectFormatService {

	private Logger logMetacat = Logger.getLogger(ObjectFormatService.class);

	/* The scope of the object formats docid used as the metacat identifier */
	public static final String OBJECT_FORMAT_DOCID = "OBJECT_FORMAT_LIST.1";

	/* The revision of the object formats document */
	private int rev;

	/* The separator of the object formats document */
	private String separator = ".";

	/* The accession number of the object formats document */
	private String accNumber = null;

	/* The list of object formats */
	private ObjectFormatList objectFormatList = null;

	/* the searchable map of object formats */
	private static HashMap<String, ObjectFormat> objectFormatMap;

	private static ObjectFormatService instance = null;
	
	public static ObjectFormatService getInstance() {
		if (instance == null) {
			instance = new ObjectFormatService();
		}
		return instance;
	}
	
	/**
	 * Constructor, private for singleton access
	 */
	private ObjectFormatService() {
		
	}

	/**
	 * Return the object format based on the given object format identifier
	 * 
	 * @param fmtid
	 *            - the object format identifier to look up
	 * @return objectFormat - the desired object format
	 */
	public ObjectFormat getFormat(ObjectFormatIdentifier fmtid)
			throws ServiceFailure, NotFound, NotImplemented {

		logMetacat.debug("CNCoreImpl.getFormat() called.");

		ObjectFormat objectFormat = null;

		// look up the format in the object format map
		objectFormat = getObjectFormatMap().get(fmtid.getValue());

		// refresh the object format list if the format is null
		if (objectFormat == null) {
			getCachedList();
			objectFormat = getObjectFormatMap().get(fmtid.getValue());

			// the object format isn't registered
			if (objectFormat == null) {
				throw new NotFound("4848", "The format specified by "
						+ fmtid.getValue() + " does not exist at this node.");

			}

		}

		return objectFormat;
	}

	/**
	 * Return the list of object formats registered from the Coordinating Node.
	 * 
	 * @return objectFormatList - the list of object formats
	 */
	public ObjectFormatList listFormats() throws ServiceFailure, NotImplemented {

		objectFormatList = getCachedList();

		return objectFormatList;
	}

	/*
	 * Return the hash containing the fmtid and format mapping
	 * 
	 * @return objectFormatMap - the hash of fmtid/format pairs
	 */
	private HashMap<String, ObjectFormat> getObjectFormatMap() {

		if (objectFormatMap == null) {
			objectFormatMap = new HashMap<String, ObjectFormat>();

		}
		return objectFormatMap;

	}

	/**
	 * Get the object format list cached in Metacat
	 */
	private ObjectFormatList getCachedList() throws ServiceFailure {

		ObjectFormatList objectFormatList = null;

		try {

			// reset the accession number separator in case it is
			// different than the default
			try {

				separator = PropertyService
						.getProperty("document.accNumSeparator");

			} catch (PropertyNotFoundException pnfe) {

				// use the default separator, but log the issue
				logMetacat.debug("There was a problem finding the document "
						+ "separator property. The error message was: "
						+ pnfe.getMessage());
			}

			// get the latest accession number if it is in Metacat
			this.rev = DBUtil
					.getLatestRevisionInDocumentTable(this.OBJECT_FORMAT_DOCID);

			if (this.rev != -1) {
				this.accNumber = this.OBJECT_FORMAT_DOCID + this.separator
						+ this.rev;
				DocumentImpl objectFormatsDocument = new DocumentImpl(
						accNumber, false);
				ByteArrayInputStream bais = new ByteArrayInputStream(
						objectFormatsDocument.getBytes());

				// deserialize the object format list
				try {

					objectFormatList = TypeMarshaller.unmarshalTypeFromStream(
							ObjectFormatList.class, bais);

				} catch (IOException e) {
					throw new ServiceFailure("4841",
							"Unexpected exception from the service - "
									+ e.getClass() + ": " + e.getMessage());

				} catch (InstantiationException e) {
					throw new ServiceFailure("4841",
							"Unexpected exception from the service - "
									+ e.getClass() + ": " + e.getMessage());

				} catch (IllegalAccessException e) {
					throw new ServiceFailure("4841",
							"Unexpected exception from the service - "
									+ e.getClass() + ": " + e.getMessage());

				} catch (JiBXException e) {
					throw new ServiceFailure("4841",
							"Unexpected exception from the service - "
									+ e.getClass() + ": " + e.getMessage());

				}

			} else {
				throw new ServiceFailure("4841",
						"The object formats collection could not "
								+ "be found at this node.");
			}

		} catch (SQLException sqle) {
			throw new ServiceFailure("4841",
					"Unexpected exception from the service - "
							+ sqle.getClass() + ": " + sqle.getMessage());

		} catch (McdbException mcdbe) {
			throw new ServiceFailure("4841",
					"Unexpected exception from the service - "
							+ mcdbe.getClass() + ": " + mcdbe.getMessage());

		}

		// index the object format list based on the format identifier string
		int listSize = objectFormatList.sizeObjectFormatList();

		for (int i = 0; i < listSize; i++) {

			ObjectFormat objectFormat = objectFormatList.getObjectFormat(i);
			String identifier = objectFormat.getFormatId().getValue();
			getObjectFormatMap().put(identifier, objectFormat);

		}

		return objectFormatList;

	}

}
