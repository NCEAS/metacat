/**
 *  '$RCSfile$'
 *    Purpose: A class that represents a readme file in a downloaded dataset.
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


/**
 * Class that abstracts the Readme file that is placed in a downloaded dataset's bag.
 * The Readme displays useful information about the data package and the files within. Because
 * of this, you'll see a lot of Identifier objects sprinkled through the class. These are pointing
 * to data files, their metadata, the ORE, and science metadata.
 *
 * The Readme is mostly just the science metadata XSLT'd into HTML. In the comments, I refer to this as the
 * 'vanilla readme'.
 *
 * @author Thomas Thelen
 **/
package edu.ucsb.nceas.metacat.download;

import edu.ucsb.nceas.metacat.common.query.stream.ContentTypeByteArrayInputStream;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.metacat.NodeRecord;
import edu.ucsb.nceas.metacat.dataone.MNodeService;

import edu.ucsb.nceas.utilities.PropertyNotFoundException;
import edu.ucsb.nceas.metacat.DBTransform;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.dataone.service.exceptions.*;
import org.dataone.exceptions.MarshallingException;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v2.SystemMetadata;
import org.dataone.service.types.v1.Session;
import org.dataone.service.util.TypeMarshaller;
import org.dataone.client.v2.formats.ObjectFormatCache;

import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.TransformerConfigurationException;

import java.io.*;
import java.sql.SQLException;
import java.util.Hashtable;
import java.util.List;


public class ReadmeFile {
    private Logger logMetacat = Logger.getLogger(NodeRecord.class);

    // Holds Identifiers for resource map and science metadata
    private List<Identifier> _coreMetadataIdentifiers;
    // Holds Identifiers for all the metadata file IDs
    private List<Identifier> _metadataIds;
    // The science metadata ID
    private Identifier _scienceMetadataId;
    // The system metadata for the science metadata document
    private SystemMetadata _scienceSystemMetadata;
    private Session _session;
    // The html readme document
    public String _doc;
    // The html table that displays the files in the package
    private String _sysmetaTable;
    // JSON-LD describing the package
    private String _jsonld;
    private MNodeService _node;
    /**
    * Creates an instance of a ReadmeFile. Note that upon construction, the entire
    * Readme is generated in memory. When ready to write it to disk, call createFile.
    *
    * @param metadataIds: A list of all of the data objects' Identifiers for metadata objects
    * @param coreMetadataIdentifiers: Identifiers for the resource map and the science metadata
     **/
    public ReadmeFile(List<Identifier> metadataIds,
                      List<Identifier> coreMetadataIdentifiers,
                      Session session,
                      MNodeService node) {
        logMetacat.info("Contructing readme object");
        this._metadataIds = metadataIds;
        this._coreMetadataIdentifiers = coreMetadataIdentifiers;
        this._session = session;
        this._node = node;

        // Create the HTML table that lists the data files
        logMetacat.info("Creating sysmeta table");
        this.findScienceMetadata();
        this.createSysMetaTable();
        // Creates the vanilla readme
        logMetacat.info("Generating readme file");
        this.generateReadmeHTMLFile();
        // Inserts the file table into the vanilla readme
        logMetacat.info("Inserting into readme");
        this.insertTableIntoReadme();
    }

    private void setHtmlDoc(String doc) {this._doc = doc;}

    /**
    * Copies the Readme from memory to a file on the filesystem.
    *
    * @param rootDirectory: The direcrory where the Readme file will be written to
    * @param path: Any additional
    * @return readmeFile: A File that represents the Readme file
     **/
    public File createFile(File rootDirectory) {

        File readmeFile = new File(rootDirectory.getAbsolutePath() + "README.html");
        try{
            // Write the html to a stream
            ContentTypeByteArrayInputStream resultInputStream = new ContentTypeByteArrayInputStream(this._doc.getBytes());
            // Copy the bytes to the html file
            IOUtils.copy(resultInputStream, new FileOutputStream(readmeFile, true));
        }
        catch (IOException e) {
            logMetacat.error("There was an error creating the README file.", e);
        }
        return readmeFile;
    }

    /**
     * Inserts a file table into the readme. This works by searching for the
     * first table row in the first table group and pasting the table in front of it.
     * If for some reason the readme HTML is empty, just return the table.
     **/
    private void insertTableIntoReadme() {
        if (this._doc.length() > 1) {
            // Look for the first table within the first table group
            String bodyTag = "<table class=\"subGroup onehundred_percent\">\n";
            int bodyTagLocation = this._doc.indexOf(bodyTag);
            // Insert the file table above the table
            int insertLocation = bodyTagLocation + bodyTag.length();
            StringBuilder builder = new StringBuilder(this._doc);
            builder.insert(insertLocation, this._sysmetaTable);
            this.setHtmlDoc(builder.toString());
        }
    }

    /**
     * Inserts a string into the head of an html document. In this case, it is most likely
     * JSON-LD.
     *
     * @param fullHTML: The HTML document
     **/
    private void insertJSONIntoReadme()
    {
        if (this._doc.length() >1) {
            // Look for the first table within the first table group
            String headTag = "<head>";
            int headTagLocation = this._doc.indexOf(headTag);
            // Insert the file table above the table
            int insertLocation = headTagLocation+headTag.length();
            StringBuilder builder = new StringBuilder(this._doc);

            String fullInstertion = "<script type=\"application/ld+json\">" + this._jsonld + "</script>";
            builder.insert(insertLocation, fullInstertion);
            this.setHtmlDoc(builder.toString());
        }
    }

    /**
     * Transform an XML document using an XSLT stylesheet to another format.
     * In this case, the eml to json-ld xsl is used to transform the EML into
     * JSON-LD
     *
     * @param doc: The document to be transformed
     * @param xslSystemId: The system location of the stylesheet
     * @param pw: The PrintWriter to which output is printed
     **/
    private String emlToJson(String doc, String xslSystemId)
    {
        try {
            StreamSource xslSource =
                    new StreamSource(xslSystemId);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            Result outputTarget = new StreamResult(outputStream);

            Transformer jsonTransformer = TransformerFactory.newInstance().newTransformer(xslSource);
            StringReader strReader = new StringReader(doc);
            jsonTransformer.transform(new StreamSource(strReader), outputTarget);
            return outputStream.toString("UTF-8");
        } catch (TransformerConfigurationException e) {
            logMetacat.error("There was an error in configuring the XML transformer.", e);
        } catch (TransformerException e) {
            logMetacat.error("Failed to transform the EML into JSON-LD", e);
        } catch (UnsupportedEncodingException e) {
            logMetacat.error("There was an error encoding the json-ld", e);
        }
        return "";
    }

    /**
     * Generates a schema.org compliant JSON-LD representation of the EML document. If the
     * generation failed, an empty string is returned.
     **/
    public void generatePackageJSONLD() {
        try {
            // Get a stream to the EML content
            InputStream metadataStream = this._node.get(this._session, this.getScienceMetadataId());
            // Turn the stream into a string for the xslt
            String emlDoc = IOUtils.toString(metadataStream, "UTF-8");
            String filePath = PropertyService.getProperty("application.deployDir")+"/"+PropertyService
                    .getProperty("application.context")+ "/style/common/conversions/emltojson-ld.xsl";
            this._jsonld = this.emlToJson(emlDoc, filePath);
        } catch(InvalidToken e)
        {
            logMetacat.error("Invalid token.", e);
        } catch(IOException e) {
            logMetacat.error("Failed to convert the metadata stream to a string.", e);
        } catch(PropertyNotFoundException e) {
            logMetacat.error("Failed to locate the eml-jsonld transformation file.", e);
        } catch(ServiceFailure e) {
            logMetacat.error("Failed to retrive the EML metadata.", e);
        } catch(NotAuthorized e) {
            logMetacat.error("Not authorized to retrive metadata.", e);
        } catch(NotFound e) {
            logMetacat.error("EML document not found.", e);
        } catch(NotImplemented e) {
            logMetacat.error("Feature is not implemented.", e);
        }
        this._jsonld = "";
    }

    private Identifier getScienceMetadataId() { return this._scienceMetadataId; }
    private SystemMetadata getScienceSystemMetadata() { return this._scienceSystemMetadata; }

    /**
     * Generates a basic readme file using the metacatui xslt.
     *
     **/
    public void generateVanillaReadme() {
        logMetacat.info("Generatiing vanilla readme.");
        String readmeBody = new String();
        Hashtable<String, String[]> params = new Hashtable<String, String[]>();
        try {
            // Get a stream to the object
            logMetacat.info("Getting metadata stream");
            logMetacat.info(this.getScienceMetadataId().getValue());
            InputStream metadataStream = this._node.get(this._session, this.getScienceMetadataId());
            // Turn the stream into a string for the xslt
            logMetacat.info("Converting stream to text");
            String documentContent = IOUtils.toString(metadataStream, "UTF-8");
            logMetacat.info("Converted");
            // Get the type so the xslt can properly parse it
            String sourceType = this.getScienceSystemMetadata().getFormatId().getValue();
            // Holds the HTML that the transformer returns
            logMetacat.info("Got source type");
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            // Handles writing the xslt response to the byte array
            logMetacat.info("Creating writer");
            Writer writer = new OutputStreamWriter(baos, "UTF-8");
            logMetacat.info("Transforming");
            DBTransform transformer = new DBTransform();

            transformer.transformXMLDocument(documentContent,
                    sourceType,
                    "-//W3C//HTML//EN",
                    "default",
                    writer,
                    params,
                    null);
            this._doc = baos.toString();
            return;
        } catch (InvalidToken e) {
            logMetacat.error("Invalid token.", e);
        } catch (ServiceFailure e) {
            logMetacat.error("Error getting a stream to the metadata document.", e);
        } catch (IOException e) {
            logMetacat.error("Error converting the metadata to a string.", e);
        } catch (NotAuthorized e) {
            logMetacat.error("Not authorized to retrive the metadata.", e);
        } catch (NotFound e) {
            logMetacat.error("EML metadata document not found.", e);
        } catch (SQLException e) {
            logMetacat.error("Error transforming the EML document.", e);
        } catch (NotImplemented e) {
            logMetacat.error("Error retrieving the EML document.", e);
        } catch (ClassNotFoundException e) {
            logMetacat.error("Failed to locate the xsl file to transform the EML document.", e);
        } catch (PropertyNotFoundException e) {
            logMetacat.error("Failed to locate the xsl file to transform the EML document.", e);
        }
        // If something went wrong, default to an empty readme
        this._doc =  "";
    }

    /**
     * Creates a table of filename, size, and hash for objects in a data package.
     *
     * @param coreMetadataIdentifiers: A list of IDs for the eml and resource map
     **/
    public void createSysMetaTable() {

        Hashtable<String, String[]> params = new Hashtable<String, String[]>();

        // Holds the HTML rows
        StringBuilder tableHTMLBuilder = new StringBuilder();
        for (Identifier metadataID : this._metadataIds) {
            // Check to make sure the object is a data file that the user had uploaded.
            if (this._coreMetadataIdentifiers.contains(metadataID)) {
                continue;
            }
            try {
                SystemMetadata entrySysMeta = this._node.getSystemMetadata(this._session, metadataID);
                Identifier objectSystemMetadataID = entrySysMeta.getIdentifier();

                // Holds the content of the system metadata document
                ByteArrayOutputStream sysMetaStream = new ByteArrayOutputStream();

                // Retrieve the sys meta document
                TypeMarshaller.marshalTypeToOutputStream(entrySysMeta, sysMetaStream);

                // Holds the HTML that the xslt returns
                ByteArrayOutputStream baos = new ByteArrayOutputStream();

                // Handles writing the xslt response to the byte array
                Writer writer = new OutputStreamWriter(baos, "UTF-8");

                // The xslt requires the sysmeta document in string form
                String documentContent = sysMetaStream.toString("UTF-8");
                DBTransform transformer = new DBTransform();

                // Transform the system metadocument using the 'export' xslt, targeting an HTML return type
                transformer.transformXMLDocument(documentContent,
                        "package-export",
                        "-//W3C//HTML//EN",
                        "package-export",
                        writer,
                        params,
                        null //sessionid
                );

                // Write the table to our string builder
                tableHTMLBuilder.append(baos.toString());
            } catch (InvalidToken e) {
                logMetacat.error("Invalid token.", e);
            } catch (MarshallingException e) {
                logMetacat.error("There was an error accessing the metadata document.", e);
            } catch (UnsupportedEncodingException e) {
                logMetacat.error("There was an error encoding the metadata document.", e);
            } catch (ServiceFailure e) {
                logMetacat.error("There was a service failure while transforming the EML document.", e);
            } catch (IOException e) {
                logMetacat.error("There was an error accessing the metadata document.", e);
            } catch (NotAuthorized e) {
                logMetacat.error("Not authorized to access the metadata document.", e);
            } catch (SQLException e) {
                logMetacat.error("Failed to create the xml transformer.", e);
            } catch (NotFound e) {
                logMetacat.error("Faield to find the metadata document.", e);
            } catch (ClassNotFoundException e) {
                logMetacat.error("Failed to locate the xsl for transforming the EML document.", e);
            } catch (NotImplemented e) {
                logMetacat.error("Not implemented.", e);
            } catch (PropertyNotFoundException e) {
                logMetacat.error("Failed to locate the xsl for transforming the EML document.", e);
            }
        }

        String htmlString = tableHTMLBuilder.toString();
        // If the HTML body is empty, we should quit early
        if (htmlString.isEmpty()) {
            this.setFileTable("");
        }

        // This is ugly, but we need to wrap the sysmeta file rows in a table
        String openingHtml = "<td class=\"fortyfive_percent\"><table class=\"subGroup subGroup_border onehundred_percent\">" +
                "<tr>" +
                "<th colspan=\"3\">" +
                "<h4>Files in this downloaded dataset:</h4>" +
                "</tr>" +
                "<tr>" +
                "<TD><B>Name</B></TD>" +
                "<TD><B>Size (Bytes)</B></TD>" +
                "</tr>" +
                "</th>";
        String closingHtml = "</table></td>";

        // Wrap the table rows in a table
        this.setFileTable(openingHtml + htmlString + closingHtml);
    }

    public void setFileTable(String table){String a = "a";}


    public void findScienceMetadata() {
        try {
            // The body of the README is generated from the primary science metadata document. Find this document, and
            // generate the body. Use coreMetadataIdentifiers since it's going to be shorter than metadataIds and contains
            // the EML doc.
            for (Identifier metadataID : this._coreMetadataIdentifiers) {
                SystemMetadata metadataSysMeta = this._node.getSystemMetadata(this._session, metadataID);
                // Look for the science metadata object
                if (ObjectFormatCache.getInstance().getFormat(metadataSysMeta.getFormatId()).getFormatType().equals("METADATA")) {
                    this._scienceMetadataId = metadataID;
                    this._scienceSystemMetadata = metadataSysMeta;
                    break;
                }
            }
        } catch (InvalidToken e) {
            logMetacat.error("Invalid token.", e);
        } catch (NotFound e) {
            logMetacat.error("Failed to locate the EML metadata document.", e);
        } catch (ServiceFailure e) {
            logMetacat.error("Service failure while creating README.html.", e);
        } catch (NotAuthorized e) {
            logMetacat.error("Not authorized to access the EML metadata document.", e);
        } catch (NotImplemented e) {
            logMetacat.error("Not implemented.", e);
        }
    }

    /**
     * Generates the README.html file in the bag root. To do this, a file table is generated via an xslt
     * that pareses system metadata documents. Then, the metacatui theme is used to generate an html
     * document rendition of the EML. The table is then inserted into the html body.
     * The EML document is also fed to an xsl that returns a subset of it as json-ld. This is inserted
     * into the head of the main HTML document.
     **/
    public void generateReadmeHTMLFile() {
        this.generateVanillaReadme();
        this.generatePackageJSONLD();
        if (!this._doc.isEmpty()) {
            // Now that we have the HTML table and the rest of the HTML body, we need to combine them
            this.insertTableIntoReadme();
            this.insertJSONIntoReadme();
        }
    }
}