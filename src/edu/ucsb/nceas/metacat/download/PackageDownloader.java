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
import edu.ucsb.nceas.metacat.download.ReadmeFile;
import edu.ucsb.nceas.metacat.NodeRecord;
import edu.ucsb.nceas.metacat.dataone.MNodeService;
import gov.loc.repository.bagit.Bag;
import gov.loc.repository.bagit.BagFactory;
import gov.loc.repository.bagit.writer.impl.ZipWriter;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dataone.client.v2.formats.ObjectFormatCache;
import org.dataone.client.v2.formats.ObjectFormatInfo;
import org.dataone.exceptions.MarshallingException;
import org.dataone.ore.ResourceMapFactory;



import org.dataone.service.exceptions.*;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.ObjectFormatIdentifier;
import org.dataone.service.types.v1.Session;
import org.dataone.service.types.v2.Node;
import org.dataone.service.types.v2.NodeList;
import org.dataone.service.types.v2.SystemMetadata;
import org.dataone.service.util.TypeMarshaller;
import org.dspace.foresite.OREException;
import org.dspace.foresite.OREParserException;
import org.dspace.foresite.ORESerialiserException;
import org.dspace.foresite.ResourceMap;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.net.URISyntaxException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    private Logger logMetacat = Logger.getLogger(NodeRecord.class);

    private Identifier _pid;
    private ObjectFormatIdentifier _formatId;
    private List<Identifier> _coreMetadataIdentifiers;
    private List<Identifier> _metadataIds;
    private ResourceMap _resourceMap;
    private BagFactory _bagFactory;
    private Bag _bag;
    private Session _session;
    private MNodeService _node;
    /**
     * Constructor
     *
     * @param session: The user's session
     * @param formatId:
     * @param pid: The PID of the package
     */
    public PackageDownloader(MNodeService node,
                             Session session,
                             ObjectFormatIdentifier formatId,
                             Identifier pid)
        throws InvalidToken, ServiceFailure, NotFound, NotAuthorized, InvalidRequest, NotImplemented {
        this._session = session;
        this._node = node;
        logMetacat.info("Setting format ID");
        this.setFormatID(formatId);
        logMetacat.info("Setting PID");
        this.setPID(pid);

        logMetacat.info("Setting core metadata IDs");
        this.setCoreMetadataIdentifiers(new ArrayList<Identifier>());
        logMetacat.info("Setting metadata ids");
        this.setMetadataIds(new ArrayList<Identifier>());
        logMetacat.info("Setting resource map");
        this.setResourceMap();
        logMetacat.info("Setting bag factory");
        this.setBagFactory(new BagFactory());
        logMetacat.info("Setting bag");
        this._bag = this.getBagFactory().createBag();
    }

    private void setMetadataIds(ArrayList ids) { this._metadataIds=ids; }
    private BagFactory getBagFactory() {
        return this._bagFactory;
    }

    private void setBagFactory(BagFactory factory) {
        this._bagFactory = factory;
    }

    public void setFormatID(ObjectFormatIdentifier formatId) { this._formatId=formatId; }

    /**
     * Returns a list that has the ORE and science metadata pids.
     */
    public List<Identifier> getCoreMetadataIdentifiers() {
        return _coreMetadataIdentifiers;
    }

    /** Setter methods **/

    /**
     * Sets the pid of the dataset being downloaded
     */
    public void setPID(Identifier pid) {
        _pid = pid;
    }

    /**
     * Adds an identifier to the list of core Ids.
     *
     * @param id: The Identifier that's being added
     */
    public void addCoreMetadataIdentifiers(Identifier id) {
        this._coreMetadataIdentifiers.add(id);
    }

    private void setResourceMap()
    throws InvalidToken, NotFound, InvalidRequest, ServiceFailure, NotAuthorized, InvalidRequest, NotImplemented {
        SystemMetadata sysMeta = this._node.getSystemMetadata(this._session, this._pid);
        if (ObjectFormatCache.getInstance().getFormat(sysMeta.getFormatId()).getFormatType().equals("RESOURCE")) {
            // Attempt to open/parse the resource map so that we can get a list of pids inside
            try {
                InputStream oreInputStream = this._node.get(this._session, this._pid);
                this._resourceMap = ResourceMapFactory.getInstance().deserializeResourceMap(oreInputStream);
                return;
            } catch (OREException e) {
                logMetacat.error("Failed to parse the ORE.", e);
            } catch (URISyntaxException e) {
                logMetacat.error("Error with a URI in the ORE.", e);
            } catch (UnsupportedEncodingException e) {
                logMetacat.error("Unsupported encoding format.", e);
            } catch (OREParserException e) {
                logMetacat.error("Failed to parse the ORE.", e);
            }
        } else {
            //throw an invalid request exception if there's just a single pid
            throw new InvalidRequest("2873", "The given pid " + this._pid.getValue() + " is not a package id (resource map id). Please use a package id instead.");
        }
    }

    /**
     * Sets the list of core Ids.
     *
     * @param ids:
     */
    public void setCoreMetadataIdentifiers(List<Identifier> ids) {
        this._coreMetadataIdentifiers = ids;
    }

    /**
     * Gets the namespace prefix for the prov ontology.
     *
     * @param resourceMap: The resource map that is being checked.
     */
    public String getProvNamespacePrefix(String resourceMap) {
        // Pattern that searches for the text between the last occurance of 'xmlns and
        // the prov namepsace string
        Pattern objectPattern = Pattern.compile("(xmlns:)(?!.*\\1)(.*)(\"http://www.w3.org/ns/prov#\")");
        Matcher m = objectPattern.matcher(resourceMap);
        if (m.find()) {
            // Save the file path for later when it gets written to disk
            return StringUtils.substringBetween(m.group(0), "xmlns:", "=\"http://www.w3.org/ns/prov#\"");
        }
        return "";
    }

    /**
     * Creates a bag file from a directory and returns a stream to it.
     *
     * @param streamedBagFile: The folder that holds the data being exported
     * @param dataRoot: The root data folder
     * @param metadataRoot: The root metadata directory
     * @param pid: The package pid
     */
    private InputStream createExportBagStream(File streamedBagFile,
                                              File dataRoot,
                                              File metadataRoot,
                                              Identifier pid) {
        InputStream bagInputStream = null;
        String bagName = pid.getValue().replaceAll("\\W", "_");
        try {
            File[] files = dataRoot.listFiles();
            for (File fle : files) {
                this._bag.addFileToPayload(fle);
            }
            this._bag.addFileAsTag(metadataRoot);
            File bagFile = new File(streamedBagFile, bagName + ".zip");
            this._bag.setFile(bagFile);
            this._bag = this._bag.makeComplete();
            ZipWriter zipWriter = new ZipWriter(this._bagFactory);
            this._bag.write(zipWriter, bagFile);
            // Make sure the bagFile is current
            bagFile = this._bag.getFile();
            // use custom FIS that will delete the file when closed
            bagInputStream = new DeleteOnCloseFileInputStream(bagFile);
            // also mark for deletion on shutdown in case the stream is never closed
            bagFile.deleteOnExit();
        } catch (FileNotFoundException e) {
            logMetacat.error("Failed to find a file to delete.", e);
        }
        return bagInputStream;
    }

    /*
     * Writes an EML document to disk. In particular, it writes it to the metadata/ directory which eventually
     * gets added to the bag.
     *
     * @param metadataID: The ID of the EML document
     * @param metadataRoot: The File that represents the metadata/ folder

     */
    private void writeEMLDocument(Identifier metadataID,
                                  File metadataRoot) {

        InputStream emlStream = null;
        try {
            emlStream = this._node.get(this._session, metadataID);
        } catch (InvalidToken e) {
            logMetacat.error("Invalid token.", e);
        } catch (ServiceFailure e) {
            logMetacat.error("Failed to retrive the EML metadata document.", e);
        } catch (NotAuthorized e) {
            logMetacat.error("Not authorized to retrive metadata.", e);
        } catch (NotFound e) {
            logMetacat.error("EML document not found.", e);
        } catch (NotImplemented e) {
            logMetacat.error("Not implemented.", e);
        }
        try {
            // Write the EML document to the bag zip
            String documentContent = IOUtils.toString(emlStream, "UTF-8");
            String filename = metadataRoot.getAbsolutePath() + "/" + "eml.xml";
            File systemMetadataDocument = new File(filename);
            BufferedWriter writer = new BufferedWriter(new FileWriter(systemMetadataDocument));
            writer.write(documentContent);
            writer.close();
        } catch (IOException e) {
            logMetacat.error("Failed to write the EML document.", e);
        }
    }

    /**
     * Searches through the resource map for any objects that have had their location specified with
     * prov:atLocation. The filePathMap parameter is mutated with the file path and corresponding pid.
     *
     * @param resMap: The resource map that's being parsed
     * @param filePathMap: Mapping between pid and file path. Should be empty when passed in

     */
    private void documentObjectLocations(ResourceMap resMap,
                                         Map<String, String> filePathMap) {
        try {
            String resMapString = ResourceMapFactory.getInstance().serializeResourceMap(resMap);
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder;
            builder = factory.newDocumentBuilder();
            Document document = builder.parse(new InputSource(new StringReader(resMapString)));

            // Attempt to search for any atLocation references
            String atLocationPrefix = getProvNamespacePrefix(resMapString);
            if (atLocationPrefix.length() > 0) {
                org.w3c.dom.NodeList nodeList = document.getElementsByTagName(atLocationPrefix + ":atLocation");

                // For each atLocation record, we want to save the location and the pid of the object
                for (int i = 0; i < nodeList.getLength(); i++) {
                    org.w3c.dom.Node node = nodeList.item(i);
                    org.w3c.dom.NamedNodeMap parentAttributes = node.getParentNode().getAttributes();
                    String parentURI = parentAttributes.item(0).getTextContent();
                    String filePath = node.getTextContent();
                    filePath = filePath.replaceAll("\"", "");

                    // We're given the full URI of the object, but we only want the PID at the end
                    Pattern objectPattern = Pattern.compile("(?<=object/).*(?)");
                    Matcher m = objectPattern.matcher(parentURI);
                    if (m.find()) {
                        // Save the file path for later when it gets written to disk
                        filePathMap.put(m.group(0), filePath);
                    } else {
                        objectPattern = Pattern.compile("(?<=resolve/).*(?)");
                        m = objectPattern.matcher(parentURI);
                        if (m.find()) {
                            // Save the file path for later when it gets written to disk
                            filePathMap.put(m.group(0), filePath);
                        }
                    }
                }
            }
        } catch (ORESerialiserException e) {
            logMetacat.warn("Failed to serialize the resource map.", e);
        } catch (ParserConfigurationException e) {
            logMetacat.warn("There was a configuration error in the XML parser.", e);
        } catch (SAXException e) {
            logMetacat.warn("SAX failed to parse the XML.", e);
        } catch (IOException e) {
            logMetacat.warn("Failed to parse the resource map.", e);
        }
    }

    /**
     * Writes the resource map to disk. This file gets written to the metadata/ folder.
     *
     * @param resMap: The resource map that's being written to disk
     * @param metadataRoot: The File that represents the metadata/ folder
     *
     */
    private void writeResourceMap(ResourceMap resMap,
                                  File metadataRoot) {

        // Write the resource map to the metadata directory
        String filename = metadataRoot.getAbsolutePath() + "/" + "oai-ore.xml";
        try {
            String resMapString = ResourceMapFactory.getInstance().serializeResourceMap(resMap);
            File systemMetadataDocument = new File(filename);
            BufferedWriter writer = new BufferedWriter(new FileWriter(systemMetadataDocument));
            writer.write(resMapString);
            writer.close();
        } catch (IOException e) {
            logMetacat.error("Failed to write resource map to the bag.", e);
        } catch (ORESerialiserException e) {
            logMetacat.error("Failed to de-serialize the resource map", e);
        }
    }

    public InputStream getPackageStream()
            throws ServiceFailure, InvalidToken,
            NotAuthorized, NotFound, NotImplemented {
        logMetacat.info("In getPackageStream");
        // Holds any science metadata and resource map pids
        this.addCoreMetadataIdentifiers(this._pid);

        // Map of objects to filepaths
        Map<String, String> filePathMap = new HashMap<String, String>();

        // the pids to include in the package
        List<Identifier> packagePids = new ArrayList<Identifier>();

        // Container that holds the pids of all of the objects that are in a package
        List<Identifier> pidsOfPackageObjects = new ArrayList<Identifier>();

        /*
           The bag has a few standard directories (metadata, metadata/sysmeta, data/). Create File objects
           representing each of these directories so that the appropriate files can be added to them. Initialize them
           to null so that they can be used outside of the try/catch block.
         */

        // A temporary directory where the non-zipped bag is formed
        File tempBagRoot = null;
        // A temporary direcotry within the tempBagRoot that represents the metadata/ direcrory
        File metadataRoot = null;
        // A temporary directory within metadataRoot that holds system metadata
        File systemMetadataDirectory = null;
        // A temporary directory within tempBagRoot that holds data objects
        File dataRoot = null;

        // Tie the File objects above to actual locations on the filesystem
        try {
            logMetacat.info("Creating files");
            tempBagRoot = new File(System.getProperty("java.io.tmpdir") + Long.toString(System.nanoTime()));
            tempBagRoot.mkdirs();

            metadataRoot = new File(tempBagRoot.getAbsolutePath() + "/metadata");
            metadataRoot.mkdir();

            systemMetadataDirectory = new File(metadataRoot.getAbsolutePath() + "/sysmeta");
            systemMetadataDirectory.mkdir();

            dataRoot = new File(tempBagRoot.getAbsolutePath() + "/data");
            dataRoot.mkdir();
        } catch (Exception e) {
            logMetacat.warn("Error creating bag files", e);
            throw new ServiceFailure("", "Metacat failed to create the temporary bag archive.");
        }

        logMetacat.info("Getting system metadata");
        // Get the system metadata for the package
        SystemMetadata sysMeta = this._node.getSystemMetadata(this._session, this._pid);

        // Maps a resource map to a list of aggregated identifiers. Use this to get the list of pids inside
        Map<Identifier, Map<Identifier, List<Identifier>>> resourceMapStructure = null;

        InputStream oreInputStream = null;

        logMetacat.info("Getting ORE stream");
        oreInputStream = this._node.get(this._session, this._pid);
        logMetacat.info("Got ORE stream");
        try {
            resourceMapStructure = ResourceMapFactory.getInstance().parseResourceMap(oreInputStream);
        } catch (OREException | OREParserException e) {
            throw new ServiceFailure("", "Failed to parse the resource map during package download.");
        } catch (URISyntaxException e) {
            throw new ServiceFailure("", "There was a malformation in the resource map.");
        } catch (UnsupportedEncodingException e) {
            throw new ServiceFailure("", "There was an error decoding the resource map.");
        }
        pidsOfPackageObjects.addAll(resourceMapStructure.keySet());

        for (Map<Identifier, List<Identifier>> entries : resourceMapStructure.values()) {
            Set<Identifier> metadataIdentifiers = entries.keySet();

            pidsOfPackageObjects.addAll(entries.keySet());
            for (List<Identifier> dataPids : entries.values()) {
                pidsOfPackageObjects.addAll(dataPids);
            }

        }

        // Attempt to open/parse the resource map so that we can get a list of pids inside
        // Check for prov:atLocation and save them in filePathMap
        this.documentObjectLocations(this._resourceMap, filePathMap);
        // Write the resource map to disk
        this.writeResourceMap(this._resourceMap, metadataRoot);

        // Use the list of pids to gather and store information about each corresponding sysmeta document
        try {
            for (Map<Identifier, List<Identifier>> entries : resourceMapStructure.values()) {
                Set<Identifier> metadataIdentifiers = entries.keySet();

                // Loop over each metadata document
                for (Identifier metadataID : metadataIdentifiers) {
                    //Get the system metadata for this metadata object
                    SystemMetadata metadataSysMeta = this._node.getSystemMetadata(this._session, metadataID);
                    // Handle any system metadata
                    if (ObjectFormatCache.getInstance().getFormat(metadataSysMeta.getFormatId()).getFormatType().equals("METADATA")) {
                        //If this is in eml format, write it to the temporary bag metadata directory
                        String metadataType = metadataSysMeta.getFormatId().getValue();
                        if (metadataType.startsWith("eml://") || metadataSysMeta.getFormatId().getValue().startsWith("https://eml.ecoinformatics.org")) {
                            // Add the ID to the list of metadata pids
                            this.addCoreMetadataIdentifiers(metadataID);

                            // Write the EML document to disk
                            this.writeEMLDocument(metadataID, metadataRoot);
                        }
                    }
                }
            }
        } catch (InvalidToken e) {
            logMetacat.error("Invalid token.", e);
        } catch (NotFound e) {
            logMetacat.error("Failed to locate the metadata.", e);
        } catch (ServiceFailure e) {
            logMetacat.error("Service failure while writing the EML document to disk.", e);
        } catch (NotAuthorized e) {
            logMetacat.error("Not authorized to access the EML metadata document.", e);
        } catch (NotImplemented e) {
            logMetacat.error("Not implemented.", e);
        }

        List<Identifier> metadataIds = new ArrayList<Identifier>();
        /*
		    Loop over each pid in the resource map. First, the system metadata gets written. Next, the data file that
			corresponds to the system metadata gets written.
		*/

        // loop through the package contents
        for (Identifier entryPid : pidsOfPackageObjects) {
            //Get the system metadata for the objbect with pid entryPid
            SystemMetadata entrySysMeta = this._node.getSystemMetadata(this._session, entryPid);

            // Write its system metadata to disk
            Identifier objectSystemMetadataID = entrySysMeta.getIdentifier();
            metadataIds.add(objectSystemMetadataID);
            try {
                String filename = systemMetadataDirectory.getAbsolutePath() + "/sysmeta-" + objectSystemMetadataID.getValue() + ".xml";
                File systemMetadataDocument = new File(filename);
                FileOutputStream sysMetaStream = new FileOutputStream(systemMetadataDocument);
                TypeMarshaller.marshalTypeToOutputStream(entrySysMeta, sysMetaStream);
            } catch (MarshallingException e) {
                logMetacat.error("There was an error accessing the metadata document.", e);
            } catch (FileNotFoundException e) {
                logMetacat.error("Failed to create the document.", e);
            } catch (IOException e) {
                logMetacat.error("Failed to write to temporary file.", e);
            }

            // Skip the resource map and the science metadata so that we don't write them to the data direcotry
            if (this.getCoreMetadataIdentifiers().contains(entryPid)) {
                continue;
            }

            String objectFormatType = ObjectFormatCache.getInstance().getFormat(entrySysMeta.getFormatId()).getFormatType();
            String fileName = null;

            //TODO: Be more specific of what characters to replace. Make sure periods arent replaced for the filename from metadata
            //Our default file name is just the ID + format type (e.g. walker.1.1-DATA)
            fileName = entryPid.getValue().replaceAll("[^a-zA-Z0-9\\-\\.]", "_") + "-" + objectFormatType;

            // ensure there is a file extension for the object
            String extension = ObjectFormatInfo.instance().getExtension(entrySysMeta.getFormatId().getValue());
            fileName += extension;

            // if SM has the file name, ignore everything else and use that
            if (entrySysMeta.getFileName() != null) {
                fileName = entrySysMeta.getFileName().replaceAll("[^a-zA-Z0-9\\-\\.]", "_");
            }

            // Create a new file for this data file. The default location is in the data diretory
            File dataPath = dataRoot;

            // Create the directory for the data file, if it was specified in the resource map
            if (filePathMap.containsKey(entryPid.getValue())) {
                dataPath = new File(dataRoot.getAbsolutePath() + "/" + filePathMap.get(entryPid.getValue()));
            }
            // We want to make sure that the directory exists before referencing it later
            if (!dataPath.exists()) {
                dataPath.mkdirs();
            }

            // Create a temporary file that will hold the bytes of the data object. This file will get
            // placed into the bag
            File tempFile = new File(dataPath, fileName);
            try {
                InputStream entryInputStream = this._node.get(this._session, entryPid);
                IOUtils.copy(entryInputStream, new FileOutputStream(tempFile));
            } catch (InvalidToken e) {
                logMetacat.error("Invalid token.", e);
            } catch (ServiceFailure e) {
                logMetacat.error("Failed to retrive data file.", e);
            } catch (NotAuthorized e) {
                logMetacat.error("Not authorized to the data file.", e);
            } catch (NotFound e) {
                logMetacat.error("Data file not found.", e);
            } catch (NotImplemented e) {
                logMetacat.error("Not implemented.", e);
            } catch (FileNotFoundException e) {
                logMetacat.error("Failed to find the temp file.", e);
            } catch (IOException e) {
                logMetacat.error("Failed to write to the temp file.", e);
            }
        }

        // Create the README.html document
        ReadmeFile readme = new ReadmeFile(this._metadataIds,
                this._coreMetadataIdentifiers,
                this._session,
                this._node);
        File readmeFile = readme.createFile(tempBagRoot);
        this._bag.addFileAsTag(readmeFile);

        // The directory where the actual bag zipfile is saved (and streamed from)
        File streamedBagFile = new File(System.getProperty("java.io.tmpdir") + Long.toString(System.nanoTime()));

        InputStream bagStream = createExportBagStream(streamedBagFile,
                dataRoot,
                metadataRoot,
                this._pid);
        try {
            logMetacat.info("Deleting temp directories");
            FileUtils.deleteDirectory(tempBagRoot);
            FileUtils.deleteDirectory(streamedBagFile);
        } catch (IOException e) {
            logMetacat.error("There was an error deleting the bag artifacts.", e);
        }
        logMetacat.info("Returning bag");
        return bagStream;
    }
}
