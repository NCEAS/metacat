/**
 *  '$RCSfile$'
 *  Copyright: 2007 Regents of the University of California and the
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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimerTask;
import java.net.URLEncoder;

import org.apache.log4j.Logger;
import org.apache.commons.lang.StringEscapeUtils;

import edu.ucsb.nceas.metacat.database.DBConnection;
import edu.ucsb.nceas.metacat.database.DBConnectionPool;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;

/**
 * A Sitemap represents a document that lists all of the content of the Metacat
 * server for use by harvesting spiders that wish to index the contents of the
 * Metacat site. It is used to generate an XML representation of all of the URLs
 * of the site in order to facilitate indexing of the metacat site by search
 * engines.
 * 
 * Which objects are included?
 * 
 * - Only documents with public read permission are included
 * - Only documents with object_formats in the xml_catalog table are included
 * - All non-obsoleted metadata objects are included in the sitemap(s)
 * 
 * Other notes:
 * 
 * - The sitemaps this class generates are intended to be served another
 *   application such as MetacatUI
 * - A sitemap index is generated regardless of the number of URLs present
 * - URLs for the location of the sitemaps and the entries themselves are 
 *   controlled by the 'sitemap.location.base' and  'sitemap.entry.base' 
 *   properties which can be full URLs or absolute paths.
 * 
 *   - sitemap.location.base controls first part of the URLs in the sitemap 
 *     index
 *   - sitemap.entry.base controls the first part of the URLs in the sitemap
 *     files themselves
 * 
 * @author Matt Jones
 * @author Bryce Mecum
 */
public class Sitemap extends TimerTask {
	
	private static Logger logMetacat = Logger.getLogger(Sitemap.class);

    /**
     * Construct a new instance of the Sitemap.
     * 
     * @param directory
     *            the location to store sitemap files
     * @param locationBase
     *            the base URL for constructing sitemap location URLs
     * @param entryBase
     *             the base URL for constructing sitemap entry URLs
     * 
     */
    public Sitemap(File directory, String locationBase, String entryBase) {
        super();
        this.directory = directory;
        this.locationBase = locationBase;
        this.entryBase = entryBase;
    }

    /**
     * Execute the timed task when called, in this case by generating the
     * sitemap files needed for this Metacat instance.
     */
    public void run() {
        generateSitemaps();
    }

    /**
     * Generate all of the sitemap files needed to list the URLs from this
     * instance of Metacat, using the open sitemap format described here:
     *   http://www.sitemaps.org/protocol.html
     * URLs are written to a single file, unless the maximum number of URLs
     * allowed in the sitemap file is exceeded, in which subsequent numbered
     * files are created. An index of the sitemaps is also created.
     * 
     * The sitemap index can be registered with search index providers such as
     * Google, but beware that it needs to be accessible in a location above the
     * mount point for the service URLs.  By default the files are placed in 
     * {context}/sitemaps, but you will need to expose them at a location 
     * matching what's set in the sitemap.location.base and sitemap.entry.base
     * properties in order to be trusted by Google.  See the Sitemaps.org 
     * documentation for details.
     */
    public void generateSitemaps() {

        logMetacat.info("Running the Sitemap task. Directory is " + directory + " and locationBase is " + locationBase +".");

        // Test if the passed in File is a directory
        if (directory.isDirectory()) {
            // Query xml_documents to get list of documents
            StringBuffer query = new StringBuffer();

            /** Query for documents that are:
             * - Metadata (their object_format is in the xml_catalog)
             * - Latest/head versions (their obsoleted_by field is NULL)
             * - Publicly readable (their access policy has a public + read perm)
             */

            // We use a subquery to filter documents based upon whether they use
            // a format ID in the xml_catalog table
            String metadata_formats = 
            "SELECT public_id from xml_catalog " + 
            "WHERE public_id is not NULL";

            String entries =
            "SELECT identifier.guid as pid " +
            "FROM identifier " +
            "LEFT JOIN systemmetadata on identifier.guid = systemmetadata.guid " +
            "LEFT JOIN xml_access on identifier.guid = xml_access.guid " +
            "WHERE " +
              "systemmetadata.object_format in (" + metadata_formats + ") AND " +
              "systemmetadata.obsoleted_by is NULL AND " + 
              "systemmetadata.archived = FALSE AND " + 
              "xml_access.principal_name = 'public' AND " + 
              "xml_access.perm_type = 'allow' " +
            "ORDER BY systemmetadata.date_uploaded ASC;";
            
            query.append(entries);

            DBConnection dbConn = null;
            int serialNumber = -1;
            try {
                // Get a database connection from the pool
                dbConn = DBConnectionPool
                        .getDBConnection("Sitemap.generateSitemap()");
                serialNumber = dbConn.getCheckOutSerialNumber();

                // Execute the query statement
                PreparedStatement stmt = dbConn.prepareStatement(query.toString());
                stmt.execute();
                ResultSet rs = stmt.getResultSet();

                // Loop through all of the documents, and write them to a
                // sitemap
                File sitemapFile = null;
                OutputStreamWriter sitemap = null;
                int counter = 0;
                int fileNumber = 0;
                while (rs.next()) {
                    // Check if a new sitemap file needs to be created
                    if (counter % MAX_URLS_IN_FILE == 0) {

                        // if a sitemap file is already open
                        if (sitemapFile != null && sitemapFile.canWrite()) {
                            // write the footer and close the file
                            writeSitemapFooter(sitemap);
                        }

                        // Open a new sitemap file for writing
                        fileNumber++;
                        sitemapFile = new File(directory, fileRoot + fileNumber
                                + ".xml");
                        sitemap = new OutputStreamWriter(new FileOutputStream(sitemapFile), Charset.forName("UTF-8"));

                        // Write the sitemap document header for the new file
                        writeSitemapHeader(sitemap);
                    }

                    writeSitemapEntry(sitemap, rs.getString(1));
                    counter++;
                }
                stmt.close();
                writeSitemapFooter(sitemap);
                writeSitemapIndex(fileNumber);
            } catch (SQLException e) {
                logMetacat.warn("Error while writing to the sitemap file: "
                        + e.getMessage());
            } catch (IOException ioe) {
                logMetacat.warn("Could not open or write to the sitemap file."
                        + ioe.getMessage());
            } finally {
                // Return database connection to the pool
                DBConnectionPool.returnDBConnection(dbConn, serialNumber);
            }
        } else {
            logMetacat.warn("Sitemap not created because directory not valid.");
        }
    }

    /**
     * Write the header information in a single sitemap file. This includes the
     * XML prolog, the root element and namespace declaration, and the elements
     * leading up to the first URL entry.
     * 
     * @param sitemap
     *            the Writer to use for writing the header
     * @throws IOException
     *             if there is a problem writing to the Writer
     */
    private void writeSitemapHeader(Writer sitemap) throws IOException {
        sitemap.write(PROLOG);
        String header = "<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n";
        
        sitemap.write(header);
        sitemap.flush();
    }

    /**
     * Write a URL entry to a single sitemap file. This includes the XML markup
     * surrounding a particular site URL.
     * 
     * @param sitemap
     *            the Writer to use for writing the URL
     * @param pid
     *            the identifier to be written in the URL
     * @throws IOException
     *             if there is a problem writing to the Writer
     */
    private void writeSitemapEntry(Writer sitemap, String pid)
            throws IOException {
        if (sitemap != null && pid != null && entryBase != null) {
            StringBuffer url = new StringBuffer();
            url.append(entryBase);

            if (!entryBase.endsWith("/")) {
                url.append("/");
            }

            // URL-encode _and_ XML escape the PID.
            url.append(StringEscapeUtils.escapeXml(
                URLEncoder.encode(pid, "UTF-8"))
            );
            
            sitemap.write("  <url><loc>");
            sitemap.write(url.toString());
            sitemap.write("</loc></url>\n");
            sitemap.flush();
        }
    }

    /**
     * Write the footer information in a single sitemap file and close the file.
     * This includes the closing tag for the root element.
     * 
     * @param sitemap
     *            the Writer to use for writing the footer
     * @throws IOException
     *             if there is a problem writing to the Writer
     */
    private void writeSitemapFooter(Writer sitemap) throws IOException {
        if (sitemap != null)
        {
	    	String footer = "</urlset>\n";
	        sitemap.write(footer);
	        sitemap.close();
        }
    }

    /**
     * Create an index file listing all of the sitemap files that were created.
     * @param fileNumber the number of sitemap files that were created.
     */
    private void writeSitemapIndex(int fileNumber) {
        
        // Open a new sitemapIndex file for writing
        File sitemapIndexFile = null;
        OutputStreamWriter sitemapIndex = null;
        sitemapIndexFile = new File(directory, indexFilename);
        try {
            sitemapIndex = new OutputStreamWriter(new FileOutputStream(sitemapIndexFile), Charset.forName("UTF-8"));

            // Write the sitemap index header for the new file
            sitemapIndex.write(PROLOG);
            String header = "<sitemapindex xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\"\n"
                    + "xmlns:sm=\"http://www.sitemaps.org/schemas/sitemap/0.9\"\n" + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
                    + "xsi:schemaLocation=\"http://www.sitemaps.org/schemas/sitemap/0.9 http://www.sitemaps.org/schemas/sitemap/0.9/siteindex.xsd\">\n";
            sitemapIndex.write(header);
            sitemapIndex.flush();

            // Write out one index entry for each sitemap file
            for (int fn = 1; fn <= fileNumber; fn++) {
                String filename = fileRoot + fileNumber + ".xml";
                writeSitemapIndexEntry(sitemapIndex, filename);
            }

            // Write the sitemap index footer content
            if (sitemapIndex != null) {
                String footer = "</sitemapindex>\n";
                sitemapIndex.write(footer);
                sitemapIndex.close();
            }

            // Close the index file
            if (sitemapIndex != null) {
                sitemapIndex.close();
            }

        } catch (IOException e) {
            logMetacat.warn("Could not open or write to the sitemap index file." + e.getMessage());
        }
    }
    
    /**
     * Write a single line of the sitemap index file containing the URL to a specific sitemap file.
     * @param sitemapIndex the writer to which the index information is written
     * @param filename the name of the index file to be used
     * @throws IOException on error writing to the index file 
     */
    private void writeSitemapIndexEntry(Writer sitemapIndex, String filename)
            throws IOException {
        if (sitemapIndex != null && filename != null && locationBase != null) {
            StringBuffer url = new StringBuffer();
            url.append(locationBase);
            if (!locationBase.endsWith("/")) {
                url.append("/");
            }
            url.append(filename);
            sitemapIndex.write("  <sitemap>\n    <loc>\n      ");
            sitemapIndex.write(url.toString());
            sitemapIndex.write("\n    </loc>\n");
            Date now = new Date();
            SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd");
            sitemapIndex.write("    <lastmod>"+ fmt.format(now) +"</lastmod>\n");
            sitemapIndex.write("  </sitemap>\n");
            sitemapIndex.flush();
        }
    }
    
    // Member variables

    /** The directory in which sitemaps are written. */
    private File directory;

    /** The root url for constructing sitemap location URLs. */
    private String locationBase;

    /** The root url for constructing sitemap entry URLs. */
    private String entryBase;

    /** Maximum number of URLs to write to a single sitemap file */
    static final int MAX_URLS_IN_FILE = 50000; // 50,000 according to Google

    /** The root name to be used in naming sitemap files. */
    static final String fileRoot = "sitemap";
    
    /** The name to give to the sitemap index file */
    static final String indexFilename = "sitemap_index.xml";

    /** A String constant containing the XML prolog to be written in files. */
    static final String PROLOG = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n";
}
