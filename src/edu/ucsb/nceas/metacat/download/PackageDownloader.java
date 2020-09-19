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

import edu.ucsb.nceas.metacat.util.DeleteOnCloseFileInputStream;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.metacat.download.ReadmeFile;
import edu.ucsb.nceas.utilities.FileUtil;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;

import gov.loc.repository.bagit.Bag;
import gov.loc.repository.bagit.BagFactory;
import gov.loc.repository.bagit.writer.impl.ZipWriter;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
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
import org.dspace.foresite.ORESerialiserException;
import org.dspace.foresite.ResourceMap;

import java.io.*;
import java.util.*;
import java.io.InputStream;

import com.hp.hpl.jena.query.*;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Model;

/**
 * A class that abstracts the process of downloading a data package.
 *
 * Behaviour: When a package is downloaded, we place the data files in a data directory. If
 * the files are described with prov:atLocation in the resource map, they'll be placed in their
 * proper locations. Files that lack the prov:atLocation are placed in the data directory.
 *
 * We additionally add the system metadata, ORE, and science metadata to the metadata folder. We
 * generate a Readme file and place it in the bag root. This file is created by transforming the
 * science metadata docuemnt to HTML. If there was an error while generating this file, the Readme
 * will not be added to the bag.
 *
 *
 */
public class PackageDownloader {
    private Logger logMetacat = Logger.getLogger(PackageDownloader.class);
    // The resource map pid
    private Identifier pid;
    // A list of science and resource map pids
    private List<Identifier> coreMetadataIdentifiers;
    //
    private List<Identifier> scienceMetadataIdentifiers;
    // The resource map describing the package
    private ResourceMap resourceMap;
    // The system metadata for the resource map
    private SystemMetadata resourceMapSystemMetadata;
    // A map between data object PID and the location where it should go on disk
    private Map<String, String> _filePathMap;
    private File tempBagRoot = null;
    private File _metadataRoot = null;
    private File _systemMetadataDirectory = null;
    private File _dataRoot = null;
    private List<Pair<SystemMetadata, InputStream>> scienceMetadatas;
    private SystemMetadata _scienceSystemMetadata;
    private BagFactory _bagFactory;
    private Bag _bag;


    /**
     * Creates a PackageDownloader object. This initializes the member variables
     * and creates the bag filesystem structure.
     *
     * @param pid:     The PID of the resource map
     */
    public PackageDownloader(Identifier pid, ResourceMap resourceMap, SystemMetadata resourceMapSystemMetadata)
            throws InvalidToken, ServiceFailure, NotFound, NotAuthorized, InvalidRequest, NotImplemented {

        this.pid = pid;
        this.resourceMap = resourceMap;
        this.resourceMapSystemMetadata = resourceMapSystemMetadata;
        this.coreMetadataIdentifiers = new ArrayList<Identifier>();
        this._bagFactory = new BagFactory();
        this._bag = this._bagFactory.createBag();
        this._filePathMap =  new HashMap<String, String>();

        /*
           The bag has a few standard directories (metadata, metadata/sysmeta, data/). Create Java File objects
           representing each of these directories so that the appropriate files can be added to them. Initialize them
           to null so that they can be used outside of the try/catch block.
         */
        // A temporary directory where the non-zipped bag is formed
        this.tempBagRoot = null;
        // A temporary directory within the tempBagRoot that represents the metadata/ direcrory
        this._metadataRoot = null;
        // A temporary directory within metadataRoot that holds system metadata
        this._systemMetadataDirectory = null;
        // A temporary directory within tempBagRoot that holds data objects
        this._dataRoot = null;

        // Tie the File objects above to actual locations on the filesystem
        try {
            logMetacat.debug("Creating temporary bag directories");
            this.tempBagRoot = new File(System.getProperty("java.io.tmpdir") + Long.toString(System.nanoTime()));
            this.tempBagRoot.mkdirs();

            this._metadataRoot = new File(this.tempBagRoot.getAbsolutePath(),
                    PropertyService.getProperty("package.download.bag.directory.metadata"));
            this._metadataRoot.mkdir();

            this._systemMetadataDirectory = new File(this._metadataRoot.getAbsolutePath(),
                    PropertyService.getProperty("package.download.bag.directory.sysmeta"));
            this._systemMetadataDirectory.mkdir();

            this._dataRoot = new File(this.tempBagRoot.getAbsolutePath(),
                    PropertyService.getProperty("package.download.bag.directory.data"));
            this._dataRoot.mkdir();
        } catch (Exception e) {
            logMetacat.warn("Error creating bag files", e);
            throw new ServiceFailure("There was an error creating the temporary download directories.", e.getMessage());
        }
        this.addCoreMetadataIdentifier(pid);
        this.getObjectLocations();
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

    public List<Identifier> getScienceMetadataIdentifiers() {
        return scienceMetadataIdentifiers;
    }

    public void addScienceMetadata(SystemMetadata sysMeta, InputStream inputStream) {

        this.scienceMetadatas.add(new MutablePair<SystemMetadata, InputStream> (sysMeta, inputStream));
    }

    /**
     * Creates a bag file from a directory and returns a stream to it.
     *
     * @param streamedBagFile: The folder that holds the data being bagged
     */
    private InputStream createExportBagStream(File streamedBagFile) {
        InputStream bagInputStream = null;

        // Set the name of the bag to the pid, with some filtering
        String bagName = this.pid.getValue().replaceAll("\\W", "_");
        //File[] dataFiles = this._dataRoot.listFiles();
        for (File dataFile : this._dataRoot.listFiles()) {
            this._bag.addFileToPayload(dataFile);
        }
        this._bag.addFileAsTag(this._metadataRoot);

        File bagFile = null;
        try {
            bagFile = new File(streamedBagFile,
                    bagName + PropertyService.getProperty("package.download.file.bag-extension"));
        } catch (PropertyNotFoundException e) {
            logMetacat.warn("Failed to find the bag extension property.", e);
            // Fall back to .zip
            bagFile = new File(streamedBagFile,
                    bagName + ".zip");
        }

        try {
            this._bag.setFile(bagFile);
            this._bag = this._bag.makeComplete();
            ZipWriter zipWriter = new ZipWriter(this._bagFactory);
            this._bag.write(zipWriter, bagFile);
            // Make sure the bagFile is current
            bagFile = this._bag.getFile();
            // use custom File Input Stream that will delete the file when closed
            bagInputStream = new DeleteOnCloseFileInputStream(bagFile);
            // also mark for deletion on shutdown in case the stream is never closed
            // DEVNOTE: What if it's a large file?
            bagFile.deleteOnExit();
        } catch (FileNotFoundException e) {
            logMetacat.error("Failed to find the temporary bag file to delete.", e);
        }
        return bagInputStream;
    }

    /*
     * Writes a science metadata document to disk. In particular, it writes it to the metadata/ directory which eventually
     * gets added to the bag.
     *
     */
    public void writeScienceMetadata() {
        int metadata_count = 0;
        for (Pair<SystemMetadata, InputStream> scienceMetadata: this.scienceMetadatas) {
            File scienceMetadataDocument = null;
            try {
                String filename = PropertyService.getProperty("package.download.file.science-metadata");
                // extension doesn't include the '.'
                String extension = FilenameUtils.getExtension(filename);
                String name = FilenameUtils.getName(filename);
                if (metadata_count > 0) {
                    // Append the count to the file name
                    filename = FilenameUtils.getPath(filename) + name+'('+String.valueOf(metadata_count)+')'+'.'+extension;
                }
                // Write the EML document to the bag zip
                scienceMetadataDocument = new File(this._metadataRoot.getAbsolutePath(),
                        filename);
            } catch (PropertyNotFoundException e) {
                logMetacat.error("Failed to find the science metadata name property.", e);
                scienceMetadataDocument = new File(this._metadataRoot.getAbsolutePath(),
                        "science-metadata.xml");
            }
            try {
                BufferedWriter writer = new BufferedWriter(new FileWriter(scienceMetadataDocument));
                writer.write(IOUtils.toString(scienceMetadata.getValue(), "UTF-8"));
                writer.close();
            } catch (IOException e) {
                // Log that we failed to write the EML, but don't kill the package download
                logMetacat.error("Failed to write the EML document.", e);
            }

            this.writeSystemMetadataObject(scienceMetadata.getKey());
        }
    }


    /**
     * Writes the resource map to disk. This file gets written to the metadata/ folder.
     *
     */
    public void writeResourceMap() {

        this.writeSystemMetadataObject(this.resourceMapSystemMetadata);
        // Write the resource map to the metadata directory
        String resMapString = "";
        File resourceMapFile = null;
        try {
            resMapString = ResourceMapFactory.getInstance().serializeResourceMap(this.resourceMap);
            resourceMapFile = new File(this._metadataRoot.getAbsolutePath(),
                    PropertyService.getProperty("package.download.file.resource-map"));
        } catch (PropertyNotFoundException e) {
            logMetacat.error("Failed to find the resource map name property.", e);
            resourceMapFile = new File(this._metadataRoot.getAbsolutePath(), "oai-ore.xml");
        } catch (ORESerialiserException e) {
            logMetacat.error("Failed to de-serialize the resource map and write it to disk.", e);
        }
        try{
            BufferedWriter writer = new BufferedWriter(new FileWriter(resourceMapFile));
            writer.write(resMapString);
            writer.close();
        } catch (IOException e) {
            logMetacat.error("Failed to write resource map to the bag.", e);
        }
    }

    /**
     * Writes a system metadata object to disk.
     *
     * @param systemMetadata: The system metadata object being written to disk
     */
    private void writeSystemMetadataObject(SystemMetadata systemMetadata) { //TODO :Make ticket for making change in bag library for streams
        String systemMetadataFilename = null;
        try {
            systemMetadataFilename = PropertyService.getProperty("package.download.file.sysmeta-prepend") +
                    systemMetadata.getIdentifier().getValue() + PropertyService.getProperty("package.download.file.sysmeta-extension");
            systemMetadataFilename.replaceAll("[^a-zA-Z0-9\\-\\.]", "_");
        } catch (PropertyNotFoundException e) {
            // Give a best bet for the file extension
            logMetacat.error("Failed to find the system metadata name property.", e);
            systemMetadataFilename = "system-metadata-" + systemMetadata.getIdentifier().getValue() + ".xml";
        }
        try{
            File systemMetadataDocument = new File(this._systemMetadataDirectory.getAbsolutePath(), systemMetadataFilename);
            FileOutputStream sysMetaStream = new FileOutputStream(systemMetadataDocument);
            TypeMarshaller.marshalTypeToOutputStream(systemMetadata, sysMetaStream);
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
     * Writes a data object to disk.
     *
     * @param systemMetadata: The object's system metadata
     * @param entryInputStream: A stream to the object
     */
    public void writeDataObject(SystemMetadata systemMetadata, InputStream entryInputStream) {

        // Write system metadata to disk
        this.writeSystemMetadataObject(systemMetadata);
        Identifier objectSystemMetadataID = systemMetadata.getIdentifier();

        String dataObjectFileName = "";

        // Some generic, default object format type
        String objectFormatType = null;
        try {
            logMetacat.info("Getting object format type");
            objectFormatType = ObjectFormatCache.getInstance().getFormat(systemMetadata.getFormatId()).getFormatType();
        } catch (NotFound e) {
            logMetacat.error("Failed to find the format type of the data object.", e);
            objectFormatType = ".data";
        }
        //Our default file name is just the ID + format type (e.g. walker.1.1-DATA)
        dataObjectFileName = objectSystemMetadataID.getValue().replaceAll("[^a-zA-Z0-9\\-\\.]", "_") + "-" + objectFormatType;

        // ensure there is a file extension for the object
        logMetacat.info("Getting object extension");
        String extension = ObjectFormatInfo.instance().getExtension(systemMetadata.getFormatId().getValue());
        dataObjectFileName += extension;

        // if SM has the file name, ignore everything else and use that
        if (systemMetadata.getFileName() != null) {
            dataObjectFileName = systemMetadata.getFileName().replaceAll("[^a-zA-Z0-9\\-\\.]", "_");
        }

        // Create a new file for this data file. The default location is in the data diretory
        File dataPath = this._dataRoot;

        logMetacat.info("_filePathMap size: " + this._filePathMap.size());
        if (this._filePathMap != null && this._filePathMap.size() > 0) {
            logMetacat.info("Placing file in directory");
            // Create the directory for the data file, if it was specified in the resource map
            if (this._filePathMap.containsKey(objectSystemMetadataID.getValue())) {
                logMetacat.info("Creating new object filepath");
                logMetacat.info(dataObjectFileName);

                dataPath = new File(this._dataRoot.getAbsolutePath() + FileUtil.getFS() +
                        this._filePathMap.get(objectSystemMetadataID.getValue()));
                logMetacat.info(this._filePathMap.get(objectSystemMetadataID.getValue()));
                logMetacat.info(dataPath.getAbsolutePath());
            }
        }

        // We want to make sure that the directory exists before referencing it later
        if (!dataPath.exists()) {
            dataPath.mkdirs();
        }

        // Create a temporary file that will hold the bytes of the data object. This file will get
        // placed into the bag. If there's a failure, keep adding the rest of the files.
        logMetacat.debug("Creating new File on disk for data object");
        File tempFile = new File(dataPath, dataObjectFileName);
        try {
            logMetacat.info("Writing new data object");
            IOUtils.copy(entryInputStream, new FileOutputStream(tempFile));
        } catch (FileNotFoundException e) {
            logMetacat.error("Failed to find the temp file.", e);
        } catch (IOException e) {
            logMetacat.error("Failed to write to the temp file.", e);
        } // TODO: Bring up with other error handling
    }

    public void generateReadme(InputStream primaryScienceMetadata, SystemMetadata primaryScienceSystemMetadata) {
        // Create the README.html document. If the readme fails to be be created, still
        // serve the download (but without the README).
        try {
            ReadmeFile readme = new ReadmeFile(IOUtils.toString(primaryScienceMetadata, "UTF-8"),
                    primaryScienceSystemMetadata);
            File readmeFile = readme.writeToFile(this.tempBagRoot);
            this._bag.addFileAsTag(readmeFile);
        } catch (ServiceFailure | IOException e) {
            logMetacat.error("Failed to create the readme file. " , e);
        }
    }

    public InputStream download() throws ServiceFailure, InvalidToken,
            NotAuthorized, NotFound, NotImplemented {
        this.writeResourceMap();
        this.writeScienceMetadata();

        return getPackageStream();
    }
    /**
     * Returns a stream to the bag archive.
     *
     */
    public InputStream getPackageStream()
            throws ServiceFailure, InvalidToken,
            NotAuthorized, NotFound, NotImplemented {
        logMetacat.debug("In getPackageStream");
        // The directory where the actual bag zipfile is saved (and streamed from)
        File streamedBagFile = new File(System.getProperty("java.io.tmpdir") + Long.toString(System.nanoTime()));
        InputStream bagStream = createExportBagStream(streamedBagFile);
        try {
            FileUtils.deleteDirectory(this.tempBagRoot);
            FileUtils.deleteDirectory(streamedBagFile);
        } catch (IOException e) {
            logMetacat.error("There was an error deleting the bag artifacts.", e);
        } //TODO: Bring this up
        logMetacat.debug("Returning bag stream");
        return bagStream;
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
            // I'm a bastard for this but there are a large number of exceptions that can be thrown from the above.
            // In any case, they should be passed over.
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
