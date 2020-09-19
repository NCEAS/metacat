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
 *
 * @author Thomas Thelen
 **/
package edu.ucsb.nceas.metacat.download;

import edu.ucsb.nceas.metacat.common.query.stream.ContentTypeByteArrayInputStream;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.metacat.DBTransform;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.exceptions.NotFound;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Session;
import org.dataone.service.types.v2.SystemMetadata;

import javax.xml.transform.Result;
import javax.xml.transform.Transformer;

import java.io.*;
import java.nio.file.Files;
import java.sql.SQLException;
import java.util.Hashtable;

/**
 * A class that represents an HTML readme file that describes a downloaded dataset.
 */
public class ReadmeFile {
    private Logger logMetacat = Logger.getLogger(ReadmeFile.class);

    // The science metadata document
    private String _scienceMetadata;
    // The system metadata for the science metadata document
    private SystemMetadata _scienceSystemMetadata;
    // The html readme document
    public String _doc;

    /**
     * Creates an instance of a ReadmeFile. When ready to write it to disk, call createFile.
     *
     * @param scienceMetadata:       The package science metadata object
     * @param scienceSystemMetadata: The science metadata's system metadata
     * @throws ServiceFailure
     **/
    public ReadmeFile(String scienceMetadata,
                      SystemMetadata scienceSystemMetadata)
            throws ServiceFailure {
        logMetacat.debug("Constructing ReadmeFile class");
        this._scienceMetadata = scienceMetadata;
        this._scienceSystemMetadata = scienceSystemMetadata;
        this.generateReadme();
    }

    /**
     * Copies the Readme from memory to a location on disk.
     *
     * @param rootDirectory: The direcrory where the Readme file will be written to
     * @return readmeFile: A File that represents the Readme file
     * @throws ServiceFailure
     **/
    public File writeToFile(File rootDirectory) throws ServiceFailure {
        logMetacat.debug("Writing Readme file to disk");
        File readmeFile = null;
        try {
            readmeFile = new File(rootDirectory.getAbsolutePath(),
                    PropertyService.getProperty("package.download.file.readme"));
        } catch (PropertyNotFoundException e) {
            logMetacat.error("Failed to find the README name property", e);
            readmeFile = new File(rootDirectory.getAbsolutePath(), "README.html");
        }

        try {
            ContentTypeByteArrayInputStream resultInputStream =
                    new ContentTypeByteArrayInputStream(_doc.getBytes("UTF-8"));
            // Copy the bytes to the html file
            IOUtils.copy(resultInputStream, new FileOutputStream(readmeFile, true));
        } catch (IOException e) {
            logMetacat.error("There was an error writing the README file.", e);
            throw new ServiceFailure("There was an error writing the README file.", e.getMessage());
        }
        return readmeFile;
    }

    /**
     * Generates a readme file using the default xslt.
     *
     * @throws ServiceFailure
     **/
    public void generateReadme() throws ServiceFailure { //TODO: Remove extra styling files
        logMetacat.debug("Generating readme.");
        String readmeBody = new String();
        Hashtable<String, String[]> params = new Hashtable<String, String[]>();
        try {
            // Get the type so the xslt can properly parse it
            String sourceType = this._scienceSystemMetadata.getFormatId().getValue();
            // Holds the HTML that the transformer returns
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            // Handles writing the xslt response to the byte array
            Writer writer = new OutputStreamWriter(baos, "UTF-8");
            DBTransform transformer = new DBTransform();
            transformer.transformXMLDocument(this._scienceMetadata,
                    sourceType,
                    "-//W3C//HTML//EN",
                    "default",
                    writer,
                    params,
                    null);
            this._doc = baos.toString();
        } catch (IOException e) {
            logMetacat.error("Error converting the metadata to a string.", e);
        } catch (SQLException e) {
            logMetacat.error("Error transforming the EML document.", e);
        } catch (ClassNotFoundException e) {
            logMetacat.error("Failed to locate the readme xsl file to transform the EML document.", e);
        } catch (PropertyNotFoundException e) {
            logMetacat.error("Failed to locate the location of the readme xslt file.", e);
        }
    }
}
