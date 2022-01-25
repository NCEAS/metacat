/**
 *  '$RCSfile$'
 *    Purpose: A class that defines the download of a data package.
 *  Copyright: 2019 Regents of the University of California and the
 *             National Center for Ecological Analysis and Synthesis
 *    Authors: Thomas Thelen
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

package edu.ucsb.nceas.metacat.download;

import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.metacat.download.ReadmeFile;
import edu.ucsb.nceas.utilities.FileUtil;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.log4j.Logger;
import org.dataone.client.v2.formats.ObjectFormatCache;
import org.dataone.client.v2.formats.ObjectFormatInfo;
import org.dataone.exceptions.MarshallingException;
import org.dataone.ore.ResourceMapFactory;

import org.dataone.service.exceptions.*;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v2.SystemMetadata;
import org.dataone.service.util.TypeMarshaller;
import org.dataone.speedbagit.SpeedBagIt;
import org.dspace.foresite.ORESerialiserException;
import org.dspace.foresite.ResourceMap;

import java.io.*;
import java.io.File;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.List;

import com.hp.hpl.jena.query.*;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Model;


/**
 * A class that handles downloading data packages under the V2 format.
 *
 */
public class PackageDownloaderV2 {
	// The resource map pid
	private Identifier pid;
	// A list of science and resource map pids
	private List<Identifier> coreMetadataIdentifiers;
	// Identifiers for the science metadata documents
	private List<Identifier> scienceMetadataIdentifiers;
	// The resource map describing the package
	private ResourceMap resourceMap;
	// The system metadata for the resource map
	private SystemMetadata resourceMapSystemMetadata;
	// A map between data object PID and the location where it should go on disk
	private Map<String, String> _filePathMap;
	private List<Pair<SystemMetadata, InputStream>> scienceMetadatas;
	private SystemMetadata _scienceSystemMetadata;
	// The underling SpeedBagIt object that holds the file streams
	public SpeedBagIt speedBag = null;

	private org.apache.commons.logging.Log logMetacat = LogFactory.getLog(this.getClass());

	/**
	 * Creates a PackageDownloader object
	 * @param pid:     The PID of the resource map
	 * @param resourceMap: The package's resource map
	 */
	public PackageDownloaderV2(Identifier pid, ResourceMap resourceMap, SystemMetadata resourceMapSystemMetadata)
			throws InvalidToken, ServiceFailure, NotFound, NotAuthorized, InvalidRequest, NotImplemented {
		// PID of the package
		this.pid = pid;
		this.resourceMap = resourceMap;
		this.resourceMapSystemMetadata = resourceMapSystemMetadata;
		this.coreMetadataIdentifiers = new ArrayList<Identifier>();
		this._filePathMap =  new HashMap<String, String>();
		// A new SpeedBagIt instance that's BagIt Version 1.0 and using MD5 as the hasing algorithm
		try {
			this.speedBag = new SpeedBagIt(1.0, "MD5");
		} catch (IOException e) {
			logMetacat.error("There was an error creating the speedbag");
			throw new ServiceFailure("", e.getMessage());
		}
		// Now that we know the pid of the resource map, add it to the list
		this.addCoreMetadataIdentifier(pid);
		// Locate any file locations in the resource map and populate _filePathMap with them
		this.getObjectLocations();
		// Locate any science metadata documents
		this.getScienceMetadataIds();
	}

	/**
	 * Returns the core metadata identifiers
	 */
	public List<Identifier> getCoreMetadataIdentifiers() {
		return coreMetadataIdentifiers;
	}

	/**
	 * Adds an identifier to the list of core Ids.
	 *
	 * @param id: The Identifier that's being added
	 */
	private void addCoreMetadataIdentifier(Identifier id) {
		this.coreMetadataIdentifiers.add(id);
	}

	/**
	 * Sets the science metadata
	 *
	 * @param scienceMetadataIdentifier: The science metadata identifier
	 */
	public void addScienceSystemMetadata(Identifier scienceMetadataIdentifier) {
		this.scienceMetadataIdentifiers.add(scienceMetadataIdentifier);
	}

	/**
	 * Gets all of the science metadata documents in the downloader
	 * @return
	 */
	public List<Identifier> getScienceMetadataIdentifiers() {
		return scienceMetadataIdentifiers;
	}

	/**
	 * Adds a system metadata document and its InputStream to the class record
	 *
	 * @param sysMeta the system metadata being added
	 * @param inputStream An InputStream to the science metadata
	 */
	public void addScienceMetadata(SystemMetadata sysMeta, InputStream inputStream) {
		this.scienceMetadatas.add(new MutablePair<SystemMetadata, InputStream> (sysMeta, inputStream));
	}

	/**
	 * Adds a data file's stream to the bag
	 *
	 * @param systemMetadata The object's system metadata
	 * @param inputStream An input stream to the data file
	 */
	public void addDataFile(SystemMetadata systemMetadata, InputStream inputStream) throws ServiceFailure {
		// Try and determine a sensible filename
		String dataFilename = "";
		// Start by finding its object type
		String objectFormatType = null;
		try {
			logMetacat.debug("Getting object format type");
			objectFormatType = ObjectFormatCache.getInstance().getFormat(systemMetadata.getFormatId()).getFormatType();
		} catch (NotFound e) {
			logMetacat.error("Failed to find the format type of the data object.", e);
			objectFormatType = "data";
		}
		//Our default file name is just the ID + format type (e.g. walker.1.1-DATA)
		Identifier objectSystemMetadataID = systemMetadata.getIdentifier();
		String dataObjectFileName = objectSystemMetadataID.getValue().replaceAll("[^a-zA-Z0-9\\-\\.]", "_") + "-" +
				objectFormatType;

		// ensure there is a file extension for the object
		logMetacat.debug("Getting file extension");
		String extension = ObjectFormatInfo.instance().getExtension(systemMetadata.getFormatId().getValue());
		dataFilename += extension;

		// if SM has the file name, ignore everything else and use that
		if (systemMetadata.getFileName() != null) {
			dataFilename = systemMetadata.getFileName().replaceAll("[^a-zA-Z0-9\\-\\.]", "_");
		}

		// See if it has a path defined in the resource map
		logMetacat.debug("Determining if file has a record in the resource map");
		String dataPath = this._filePathMap.get(objectSystemMetadataID.getValue());
		String filePath = "";
		try {
			String dataDirectory = PropertyService.getProperty("package.download.bag.directory.data");
			if (this._filePathMap.get(objectSystemMetadataID.getValue()) == null) {
				logMetacat.debug("Failed to find a file location for the data file. Defaulting to data/");
				// Create the file path without any additional directories past the default data directory
				filePath = Paths.get(dataDirectory, dataFilename).toString();
			} else {
				filePath = Paths.get(dataDirectory, dataPath, dataFilename).toString();
			}
			this.speedBag.addFile(inputStream, filePath, false);
		} catch (Exception e) {
			logMetacat.error("Error adding the datafile to the bag", e);
			throw new ServiceFailure("There was an error creating the temporary download directories.", e.getMessage());
		}
	}

	/*
	 * Adds all the science metadata objects to the bag
	 */
	public void addScienceMetadatas() throws NoSuchAlgorithmException {
		int metadata_count = 0;
		for (Pair<SystemMetadata, InputStream> scienceMetadata: this.scienceMetadatas) {
			String filename = "";
			try {
				filename = PropertyService.getProperty("package.download.file.science-metadata");
			} catch (PropertyNotFoundException e) {
				logMetacat.error("Failed to find the science metadata name property.", e);
				filename = "science-metadata.xml";
			}
			// extension doesn't include the '.'
			String extension = FilenameUtils.getExtension(filename);
			String name = FilenameUtils.getName(filename);
			if (metadata_count > 0) {
				// Append the count to the file name. This gives naming doc(1), doc(2), doc(3)
				filename = FilenameUtils.getPath(filename) + name+'('+String.valueOf(metadata_count)+')'+'.'+extension;
			}
			// Add the bag directory to the beginning of the filename
			String filePath = "";
			try {
				filePath = Paths.get(PropertyService.getProperty("package.download.bag.directory.metadata"),
						filename).toString();
			}  catch (PropertyNotFoundException e) {
				filePath = Paths.get("metadata", filename).toString();
			}
			this.speedBag.addFile(scienceMetadata.getValue(), filePath, true);
			this.addSystemMetadata(scienceMetadata.getKey());
		}
	}


	/**
	 * Adds the resource map to the bag.
	 */
	public void addResourceMap() throws NoSuchAlgorithmException {
		// Add its associated system metadata to the bag
		this.addSystemMetadata(this.resourceMapSystemMetadata);
		String resmapPath = "";
		try {
			resmapPath = Paths.get(PropertyService.getProperty(
					"package.download.bag.directory.metadata"), PropertyService.getProperty(
					"package.download.file.resource-map")).toString();
		} catch (PropertyNotFoundException e) {
			resmapPath = Paths.get("metadata", "oai-ore.xml").toString();
		}
		String resMapString = "";
		try {
			resMapString = ResourceMapFactory.getInstance().serializeResourceMap(this.resourceMap);
		} catch (ORESerialiserException e) {
			logMetacat.error("Failed to de-serialize the resource map " +
					"and write it to disk.", e);
		}
		this.speedBag.addFile(new ByteArrayInputStream(resMapString.getBytes()), resmapPath, true);
	}

	/**
	 * Adds a system metadata object to the bag
	 *
	 * @param systemMetadata: The system metadata object being added to the bag
	 */
	public void addSystemMetadata(SystemMetadata systemMetadata) throws NoSuchAlgorithmException {
		// Start by generating a filename
		String systemMetadataFilename = null;
		try {
			systemMetadataFilename = PropertyService.getProperty("package.download.file.sysmeta-prepend") +
					systemMetadata.getIdentifier().getValue() +
					PropertyService.getProperty("package.download.file.sysmeta-extension");
			systemMetadataFilename.replaceAll("[^a-zA-Z0-9\\-\\.]", "_");
		} catch (PropertyNotFoundException e) {
			// Give a best bet for the file extension
			logMetacat.error("Failed to find the system metadata name property.", e);
			systemMetadataFilename = "system-metadata-" + systemMetadata.getIdentifier().getValue() + ".xml";
		}
		try{
			// The type marshler needs an OutputStream and we need an InputStream, so get an output
			// stream and turn it into an InputStream
			ByteArrayOutputStream sysMetaOutputstream = new ByteArrayOutputStream();
			TypeMarshaller.marshalTypeToOutputStream(systemMetadata, sysMetaOutputstream);
			InputStream sysMetaInputstream = new ByteArrayInputStream(sysMetaOutputstream.toString().getBytes("UTF-8"));
			// Construct the path
			String systemMetaPath = "";
			try {
				systemMetaPath = Paths.get(PropertyService.getProperty("package.download.bag.directory.sysmeta"),
						systemMetadataFilename).toString();
			} catch (PropertyNotFoundException e) {
				systemMetaPath = Paths.get("sysmeta", systemMetadataFilename).toString();
			}
			// Add it to the bag
			this.speedBag.addFile(sysMetaInputstream, systemMetaPath, true);
		} catch (MarshallingException e) {
			logMetacat.error("There was an error converting the metadata document. ID: " +
					systemMetadata.getIdentifier().getValue(), e);
		} catch (FileNotFoundException e) {
			logMetacat.error("Failed to find the temporary file when writing object. ID: " +
					systemMetadata.getIdentifier().getValue(), e);
		} catch (IOException e) {
			logMetacat.error("Failed to write to temporary file when writing object. ID: " +
					systemMetadata.getIdentifier().getValue(), e);
		}
	}


	/**
	 * Responsible for creating the readme document and adding it to the bag.
	 *
	 * @param primaryScienceMetadata The primary EML/sicnece metadata document
	 * @param primaryScienceSystemMetadata The system metadata document for the science metadata
	 */
	public void generateReadme(InputStream primaryScienceMetadata, SystemMetadata primaryScienceSystemMetadata) throws
			NoSuchAlgorithmException{
		// Create the README.html document. If the readme fails to be be created, still
		// serve the download without the README.
		try {
			ReadmeFile readme = new ReadmeFile(IOUtils.toString(primaryScienceMetadata, "UTF-8"),
					primaryScienceSystemMetadata);
			// The readme is in the root directory, so leave it as the filename
			String readmePath = PropertyService.getProperty("package.download.file.readme");
			this.speedBag.addFile(readme.getStream(), readmePath, true);
		} catch (ServiceFailure | PropertyNotFoundException | IOException e) {
			logMetacat.error("Failed to create the readme file." , e);
		}
	}

	public InputStream download() throws ServiceFailure, InvalidToken,
			NotAuthorized, NotFound, NotImplemented {
		try {
			this.addResourceMap();
			this.addScienceMetadatas();
			return speedBag.stream();
		} catch (NullPointerException | IOException e) {
			e.printStackTrace();
			ServiceFailure sf = new ServiceFailure("1030", "There was an " +
					"error while streaming the downloaded data package. " + e.getMessage());
			sf.initCause(e);
			throw sf;
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			ServiceFailure sf = new ServiceFailure("1030", "While creating the package " +
					"download, an unsupported checksumming algorithm was encountered. " + e.getMessage());
			sf.initCause(e);
			throw sf;
		}
	}

	public void getScienceMetadataIds () {
		String rdfQuery = "      PREFIX cito:    <http://www.w3.org/ns/prov#>\n" +
				"\n" +
				"                SELECT ?subject\n" +
				"                WHERE {\n" +
				"\n" +
				"                    ?science_metadata cito:documents ?data_object .\n" +
				"                }";
		try {
			// Execute the SELECT query
			ResultSet queryResults = selectQuery(rdfQuery);
			// Stores a single result from the query
			QuerySolution currentResult;
			// Do as long as there's a result
			while (queryResults.hasNext()) {
				currentResult = queryResults.next();
				// Get the atLocation and pid fields
				RDFNode subjectNode = currentResult.get("science_metadata");

				// Turn results into String
				String subjectStr = subjectNode.toString();

				// Check if we have any results
				if (subjectStr == null) {
					logMetacat.warn("Failed to find any science metadata documents during package download.");
					continue;
				}
				Identifier identifier = new Identifier();
				identifier.setValue(subjectStr);
				this.addCoreMetadataIdentifier(identifier);
				this.addScienceSystemMetadata(identifier);
			}
		} catch (Exception e) {
			logMetacat.error("There was an error while parsing an atLocation field.", e);
		}
	}

	private ResultSet selectQuery(String rdfQuery) {
		String resMapString = null;
		InputStream targetStream = null;
		// Try to get the resource map as a string for Jena
		try {
			resMapString = ResourceMapFactory.getInstance().serializeResourceMap(this.resourceMap);
			targetStream = IOUtils.toInputStream(resMapString);
		} catch (ORESerialiserException e) {
			// Log that there was an error, but don't interrupt the download
			logMetacat.error("There was an error while serializing the resource map.", e);
		}

		if (targetStream == null) {
			try {
				targetStream.close();
			} catch (IOException e) {
				logMetacat.error("Failed to close the resource map InputStream", e);
			}
			logMetacat.error("There was an error while serializing the resource map");
		}
		// Create a Jena model for the resource map
		Model model = ModelFactory.createDefaultModel();
		model.read(targetStream, null);
		// Create the Jena Query object which holds our String
		Query query = QueryFactory.create(rdfQuery);
		// Connect the query with the rdf
		QueryExecution qexec = QueryExecutionFactory.create(query, model);

		ResultSet queryResults = qexec.execSelect();
		return queryResults;
	}

	/**
	 * Queries the resource map for any pids that have an atLocation record. If found,
	 * it saves them.
	 *
	 */
	private void getObjectLocations() {
		String rdfQuery = "      PREFIX prov:    <http://www.w3.org/ns/prov#>\n" +
				"                PREFIX dcterms: <http://purl.org/dc/terms/>\n" +
				"\n" +
				"                SELECT *\n" +
				"                WHERE {\n" +
				"\n" +
				"                    ?subject prov:atLocation ?prov_atLocation .\n" +
				"                    ?subject dcterms:identifier ?pidValue .\n" +
				"                }";
		try {
			// Execute the SELECT query
			ResultSet queryResults = selectQuery(rdfQuery);
			// Stores a single result from the query
			QuerySolution currentResult;
			// Do as long as there's a result
			while (queryResults.hasNext()) {
				currentResult = queryResults.next();
				// Get the atLocation and pid fields
				RDFNode subjectNode = currentResult.get("pidValue");
				RDFNode locationNode = currentResult.get("prov_atLocation");

				// Turn results into String
				String subjectStr = subjectNode.toString();
				String locationStr = locationNode.toString();

				// Check if we have any results
				if (locationStr == null || subjectStr == null) {
					logMetacat.warn("Failed to find any locaton values");
					continue;
				}

				// Make sure that the directory separators will work on Windows or POSIX
				locationStr = FilenameUtils.separatorsToSystem(locationNode.toString());
				// Remove any . characters to prevent directory traversal
				locationStr = locationStr.replace(".", "");
				// The subject form is pid^^vocabword. Remove everything after ^^
				subjectStr = StringUtils.substringBefore(subjectStr, "^^");
				this._filePathMap.put(subjectStr, locationStr);
			}
		} catch (Exception e) {
			// I'm a bastard for this but there are a large number of exceptions that can be thrown from the above.
			// In any case, they should be passed over.
			logMetacat.error("There was an error while parsing an atLocation field.", e);
		}
	}
}