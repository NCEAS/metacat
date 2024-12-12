package edu.ucsb.nceas.metacat.download;

import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.MutablePair;
import org.dataone.client.v2.formats.ObjectFormatCache;
import org.dataone.client.v2.formats.ObjectFormatInfo;
import org.dataone.exceptions.MarshallingException;
import org.dataone.ore.ResourceMapFactory;

import org.dataone.service.exceptions.*;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v2.SystemMetadata;
import org.dataone.service.util.TypeMarshaller;
import org.dataone.speedbagit.SpeedBagIt;
import org.dataone.speedbagit.SpeedBagException;
import org.dspace.foresite.ORESerialiserException;
import org.dspace.foresite.ResourceMap;

import java.io.*;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.List;

import com.hp.hpl.jena.query.*;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Model;


/**
 * A class that handles downloading data packages under the V2 format. It contains a number of
 * private members that hold information about the data package (lists of identifiers, metadata,
 * file paths, etc). The public member functions are used as interfaces: identifiers, metadata,
 * etc. are added to this class through them.
 */
public class PackageDownloaderV2 {
    // A list of science and resource map pids
    private List<Identifier> coreMetadataIdentifiers;

    // Identifiers for the science metadata documents
    private List<Identifier> scienceMetadataIdentifiers = new ArrayList<>();
    // The resource map describing the package
    private ResourceMap resourceMap;
    // The system metadata for the resource map
    private SystemMetadata resourceMapSystemMetadata;
    // A map between data object PID and the location where it should go on disk
    private Map<String, String> _filePathMap;
    private List<Pair<SystemMetadata, InputStream>> scienceMetadatas = new ArrayList<>();
    // The underling SpeedBagIt object that holds the file streams
    public SpeedBagIt speedBag = null;

    private org.apache.commons.logging.Log logMetacat = LogFactory.getLog(this.getClass());

    /**
     * Creates a PackageDownloader object
     * @param pid:     The PID of the resource map
     * @param resourceMap: The package's resource map
     */
    public PackageDownloaderV2(Identifier pid, ResourceMap resourceMap,
        SystemMetadata resourceMapSystemMetadata)
        throws ServiceFailure {
        // PID of the package
        this.resourceMap = resourceMap;
        this.resourceMapSystemMetadata = resourceMapSystemMetadata;
        this.coreMetadataIdentifiers = new ArrayList<Identifier>();
        // A map of a Subject to its full filepath from prov:atLocation. Unsanitized.
        this._filePathMap = new HashMap<String, String>();
        // A new SpeedBagIt instance that's BagIt Version 1.0 and using MD5 as the hashing algorithm
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
        this.scienceMetadatas.add(
            new MutablePair<SystemMetadata, InputStream>(sysMeta, inputStream));
    }

    /**
     * Adds a data file's stream to the bag
     *
     * @param systemMetadata The object's system metadata
     * @param inputStream An input stream to the data file
     */
    public void addDataFile(SystemMetadata systemMetadata, InputStream inputStream)
        throws ServiceFailure {
        // Try and determine a filename
        // Start by finding its object type
        String objectFormatType = null;
        try {
            objectFormatType =
                ObjectFormatCache.getInstance().getFormat(systemMetadata.getFormatId())
                    .getFormatType();
        } catch (NotFound e) {
            logMetacat.error("Failed to find the format type of the data object.", e);
            objectFormatType = "data";
        }
        //Our default file name is just the ID + format type (e.g. walker.1.1-DATA)
        Identifier objectSystemMetadataID = systemMetadata.getIdentifier();
        String dataObjectFileName =
            this.sanitizeFilename(objectSystemMetadataID.getValue()) + "-" + objectFormatType;

        // Try and determine the extension. Leave it extensionless if one isn't defined in the
        // system metadata
        try {
            String extension =
                ObjectFormatInfo.instance().getExtension(systemMetadata.getFormatId().getValue());
            dataObjectFileName += extension;
        } catch (Exception e) {
        }

        // if SM has the file name, ignore everything else and use that
        if (systemMetadata.getFileName() != null) {
            logMetacat.debug("Failed to find any filename in the system metadata.");
            dataObjectFileName = this.sanitizeFilename(systemMetadata.getFileName());
        }

        // See if it has a path defined in the resource map
        logMetacat.debug("Determining if file has a record in the resource map");
        String dataPath = this._filePathMap.get(objectSystemMetadataID.getValue());
        String bagDataFilePath;
        try {
            String dataDirectory =
                PropertyService.getProperty("package.download.bag.directory.data");
            if (dataPath == null) {
                bagDataFilePath = dataObjectFileName;
                dataPath = Paths.get(dataDirectory, dataObjectFileName).toString();
            } else {
                dataPath = this.sanitizeFilepath(dataPath);
                bagDataFilePath = dataPath;
                dataPath = Paths.get(dataDirectory, dataPath).toString();
            }
            //check for duplicate paths
            if (this.speedBag.getDataFiles().containsKey(dataPath)) {
                Path path = Paths.get(bagDataFilePath);
                Path parentPath = path.getParent();
                String dataFilePath = (parentPath == null) ? "" : parentPath.toString();
                int index = 0;
                String duplicateDataPath = dataPath;
                while (this.speedBag.getDataFiles().containsKey(duplicateDataPath)) {
                    duplicateDataPath = Paths.get(dataDirectory, dataFilePath,
                        index++ + "-duplicate-" + path.getFileName()).toString();
                }
                logMetacat.warn("Duplicate data filename (" + dataPath + ") was renamed...");
                dataPath = duplicateDataPath;
            }
            logMetacat.debug("Adding data file to the bag at " + dataPath);

            this.speedBag.addFile(inputStream, dataPath, false);

        } catch (SpeedBagException e) {
            throw new ServiceFailure("There was an error creating the BagIt bag.", e.getMessage());
        } catch (NoSuchAlgorithmException e) {
            throw new ServiceFailure("There was an error creating the BagIt bag.", e.getMessage());
        } catch (PropertyNotFoundException e) {
            throw new ServiceFailure("There was an error creating the BagIt bag.", e.getMessage());
        }
    }

    /*
     * Adds all the science metadata objects to the bag
     */
    public void addScienceMetadatas() throws NoSuchAlgorithmException, ServiceFailure {
        int metadata_count = 0;

        for (Pair<SystemMetadata, InputStream> scienceMetadata : this.scienceMetadatas) {
            String filename;
            try {
                filename = PropertyService.getProperty("package.download.file.science-metadata");
            } catch (PropertyNotFoundException e) {
                logMetacat.error("Failed to find the science metadata name property.", e);
                filename = "science-metadata.xml";
            }
            if (metadata_count > 0) {
                // Append the count to the file name. This gives naming doc(1), doc(2), doc(3)
                filename = FilenameUtils.getPath(filename) + FilenameUtils.getBaseName(filename) + '('
                    + metadata_count + ')' + '.' + FilenameUtils.getExtension(filename);
            }
            // Add the bag directory to the beginning of the filename
            String filePath;
            try {
                filePath = Paths.get(
                    PropertyService.getProperty("package.download.bag.directory.metadata"),
                    filename).toString();
            } catch (PropertyNotFoundException e) {
                filePath = Paths.get("metadata", filename).toString();
            }
            logMetacat.debug("Adding metadata file to the bag as " + filePath);
            try {
                this.speedBag.addFile(scienceMetadata.getValue(), filePath, true);
                this.addSystemMetadata(scienceMetadata.getKey());
            } catch (SpeedBagException e) {
                e.printStackTrace();
                ServiceFailure sf = new ServiceFailure("1030",
                    "Error while adding science metadata to the bag. " + e.getMessage());
                sf.initCause(e);
                throw sf;
            }
            metadata_count++;
        }
    }

    /**
     * Adds the resource map to the bag.
     */
    public void addResourceMap() throws NoSuchAlgorithmException, ServiceFailure {
        // Add its associated system metadata to the bag
        this.addSystemMetadata(this.resourceMapSystemMetadata);
        String resmapPath = "";
        try {
            resmapPath =
                Paths.get(PropertyService.getProperty("package.download.bag.directory.metadata"),
                    PropertyService.getProperty("package.download.file.resource-map")).toString();
        } catch (PropertyNotFoundException e) {
            resmapPath = Paths.get("metadata", "oai-ore.xml").toString();
        }
        String resMapString = "";
        try {
            resMapString = ResourceMapFactory.getInstance().serializeResourceMap(this.resourceMap);
        } catch (ORESerialiserException e) {
            logMetacat.error("Failed to de-serialize the resource map " + "and write it to disk.",
                e);
        }
        logMetacat.debug("Adding resource map to " + resmapPath);
        try {
            this.speedBag.addFile(new ByteArrayInputStream(resMapString.getBytes()), resmapPath,
                true);
        } catch (SpeedBagException e) {
            e.printStackTrace();
            ServiceFailure sf = new ServiceFailure("1030",
                "Error while adding resource map to the bag. " + e.getMessage());
            sf.initCause(e);
            throw sf;
        }
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
            systemMetadataFilename =
                PropertyService.getProperty("package.download.file.sysmeta-prepend")
                    + systemMetadata.getIdentifier().getValue() + PropertyService.getProperty(
                    "package.download.file.sysmeta-extension");
        } catch (PropertyNotFoundException e) {
            // Give a best bet for the file extension
            logMetacat.error("Failed to find the system metadata name property.", e);
            systemMetadataFilename =
                "system-metadata-" + systemMetadata.getIdentifier().getValue() + ".xml";
        }
        logMetacat.debug("Sanitizing the system metadata filename: " + systemMetadataFilename);
        systemMetadataFilename = this.sanitizeFilename(systemMetadataFilename);
        logMetacat.debug("Sanitized the system metadata filename: " + systemMetadataFilename);
        try {
            // The type marshaller needs an OutputStream and we need an InputStream, so get an
            // output stream and turn it into an InputStream
            ByteArrayOutputStream sysMetaOutputstream = new ByteArrayOutputStream();
            TypeMarshaller.marshalTypeToOutputStream(systemMetadata, sysMetaOutputstream);
            InputStream sysMetaInputstream =
                new ByteArrayInputStream(sysMetaOutputstream.toString().getBytes("UTF-8"));
            // Construct the path
            String systemMetaPath = "";
            try {
                systemMetaPath = Paths.get(
                    PropertyService.getProperty("package.download.bag.directory.metadata"),
                    PropertyService.getProperty("package.download.bag.directory.sysmeta"),
                    systemMetadataFilename).toString();

            } catch (PropertyNotFoundException e) {
                systemMetaPath =
                    Paths.get("metadata", "sysmeta", systemMetadataFilename).toString();
            }
            // Add it to the bag
            this.speedBag.addFile(sysMetaInputstream, systemMetaPath, true);
        } catch (SpeedBagException e) {
            logMetacat.error(
                "Failed to add sysmeta to the BagIt bag. ID: " + systemMetadata.getIdentifier()
                    .getValue(), e);
        } catch (MarshallingException e) {
            logMetacat.error("There was an error converting the metadata document. ID: "
                + systemMetadata.getIdentifier().getValue(), e);
        } catch (FileNotFoundException e) {
            logMetacat.error("Failed to find the temporary file when writing object. ID: "
                + systemMetadata.getIdentifier().getValue(), e);
        } catch (IOException e) {
            logMetacat.error("Failed to write to temporary file when writing object. ID: "
                + systemMetadata.getIdentifier().getValue(), e);
        }
    }

    /**
     * Streams the completed bag to the caller.
     *
     * @return An InputStream consisting of the bag bytes
     * @throws ServiceFailure
     * @throws InvalidToken
     * @throws NotAuthorized
     * @throws NotFound
     * @throws NotImplemented
     */
    public InputStream download()
        throws ServiceFailure, InvalidToken, NotAuthorized, NotFound, NotImplemented {
        try {
            this.addResourceMap();
            this.addScienceMetadatas();
            return speedBag.stream();
        } catch (IOException e) {
            e.printStackTrace();
            ServiceFailure sf = new ServiceFailure("1030",
                "There was an i/o error while streaming the downloaded data package. "
                    + e.getMessage());
            sf.initCause(e);
            throw sf;
        } catch (NullPointerException e) {
            e.printStackTrace();
            ServiceFailure sf = new ServiceFailure("1030",
                "There was an error while streaming the downloaded data package. "
                    + e.getMessage());
            sf.initCause(e);
            throw sf;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            ServiceFailure sf = new ServiceFailure("1030", "While creating the package "
                + "download, an unsupported checksumming algorithm was encountered. "
                + e.getMessage());
            sf.initCause(e);
            throw sf;
        }
    }

    public void getScienceMetadataIds() {
        String rdfQuery =
            "PREFIX cito: <http://purl.org/spar/cito/>\n" + "\n" + "SELECT ?science_metadata\n"
                + "WHERE {\n" + "\n" + "?science_metadata cito:documents ?data_object .\n" + "}";
        try {
            logMetacat.debug("Getting science metadata identifiers");
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
                logMetacat.debug("Found the subject, " + subjectStr);
                // Check if we have any results
                if (subjectStr == null) {
                    logMetacat.warn(
                        "Failed to find any science metadata documents during package download.");
                    continue;
                }
                String id = StringUtils.substringAfterLast(subjectStr, "resolve/");
                id = java.net.URLDecoder.decode(id, "UTF-8");
                Identifier identifier = new Identifier();
                identifier.setValue(id);
                this.addCoreMetadataIdentifier(identifier);
                this.addScienceSystemMetadata(identifier);
            }
        } catch (UnsupportedEncodingException e) {
            logMetacat.error("There was an error with decoding the identifier.", e);
        }
    }

    private ResultSet selectQuery(String rdfQuery) {
        String resMapString = null;
        InputStream targetStream = null;
        // Try to get the resource map as a string for Jena
        try {
            resMapString = ResourceMapFactory.getInstance().serializeResourceMap(this.resourceMap);
            targetStream = IOUtils.toInputStream(resMapString, "UTF-8");
        } catch (ORESerialiserException e) {
            // Log that there was an error, but don't interrupt the download
            logMetacat.error("Problem serializing the resource map.", e);
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
     * it saves them in the file path map. It leaves sanitation to the caller.
     *
     */
    private void getObjectLocations() {
        String rdfQuery = "      PREFIX prov:    <http://www.w3.org/ns/prov#>\n"
            + "                PREFIX dcterms: <http://purl.org/dc/terms/>\n" + "\n"
            + "                SELECT *\n" + "                WHERE {\n" + "\n"
            + "                    ?subject prov:atLocation ?prov_atLocation .\n"
            + "                    ?subject dcterms:identifier ?pidValue .\n" + "                }";
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
                    logMetacat.debug("Failed to find any location values");
                    continue;
                }

                // Make sure that the directory separators will work on Windows or POSIX
                locationStr = FilenameUtils.separatorsToSystem(locationNode.toString());
                // The subject form is pid^^vocabword. Remove everything after ^^
                subjectStr = StringUtils.substringBefore(subjectStr, "^^");
                this._filePathMap.put(subjectStr, locationStr);
            }
        } catch (Exception e) {
            logMetacat.error("There was an error while parsing an atLocation field.", e);
        }
    }

    // Sanitizes the 'filename' parameter; only allows numbers and letters
    String sanitizeFilename(String filename) {
        return filename.replaceAll("[^a-zA-Z0-9\\-\\.]", "_");
    }

    // Sanitizes a file path; it allows forward slashes
    String sanitizeFilepath(String filename) {
        return filename.replaceAll("[^a-zA-Z0-9\\-\\.\\/_ ]", "");
    }
}
