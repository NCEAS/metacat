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

import java.io.*;
import java.nio.charset.Charset;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimerTask;

import java.net.URLEncoder;

import org.apache.log4j.Logger;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.apache.commons.lang.StringEscapeUtils;

import edu.ucsb.nceas.metacat.database.DBConnection;
import edu.ucsb.nceas.metacat.database.DBConnectionPool;

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

    /** Create just a single document builder factory and builder to be
     * re-used through this class.
     */
    DocumentBuilderFactory documentBuilderFactory;
    DocumentBuilder documentBuilder;

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

    /**
     * Construct a new instance of the Sitemap class.
     *
     * @param directory    The location to store sitemap files
     * @param locationBase The base URL for constructing sitemap location URLs
     * @param entryBase    The base URL for constructing sitemap entry URLs
     */
    public Sitemap(File directory, String locationBase, String entryBase) {
        super();

        this.directory = directory;
        this.locationBase = locationBase;
        this.entryBase = entryBase;

        this.documentBuilderFactory = DocumentBuilderFactory.newInstance();
        try {
            this.documentBuilder =
                    this.documentBuilderFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            logMetacat.error(e.getMessage());
        }
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
     * http://www.sitemaps.org/protocol.html
     * URLs are written to a one or more files and a sitemap index file is
     * always written. The number of sitemap files is determined by
     * MAX_URLS_IN_FILE and how many metadata documents you have registered
     * in Metacat.
     * <p>
     * The sitemap index can be registered with search index providers such as
     * Google, but beware that it needs to be accessible in a location above the
     * mount point for the service URLs.  By default the files are placed in
     * {context}/sitemaps, but you will need to expose them at a location
     * matching what's set in the sitemap.location.base and sitemap.entry.base
     * properties in order to be trusted by Google.  See the Sitemaps.org
     * documentation for details.
     */
    public void generateSitemaps() {
        logMetacat.info("Running the Sitemap task. Directory is " +
                directory + " and locationBase is " + locationBase + ".");

        // Stop if we don't have a place to write sitemaps
        if (!directory.isDirectory()) {
            logMetacat.error("Sitemap.generateSitemaps(): Sitemap(s) not " +
                    "created because directory " +
                    directory.getAbsolutePath() + " is not valid.");

            return;
        }

        // Stop if we don't have a document builder
        if (documentBuilder == null) {
            logMetacat.error("Couldn't generate sitemaps because we didn't " +
                    "have a document builder instance.");

            return;
        }

        /**
         * Query the database for documents that are considered to be metadata
         * and iterate through them, generating sitemap index and sitemap files
         * as we go
         */

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
            "SELECT " +
                "identifier.guid as pid, " +
                "systemmetadata.date_modified as lastmod " +
            "FROM identifier " +
            "LEFT JOIN systemmetadata on " +
                    "identifier.guid = systemmetadata.guid " +
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

            // Set up the first file
            File sitemapFile = null;
            OutputStreamWriter sitemapWriter = null;
            Document document = null;
            Element rootNode = null;

            int counter = 0;
            int fileNumber = 0;

            while (rs.next()) {
                // Write out the current sitemap file and set up a new one if
                // we need to
                if (counter % this.MAX_URLS_IN_FILE == 0) {
                    // Only write out the sitemap file if one's already open
                    // This basically prevents writing before we even begin
                    if (sitemapFile != null && sitemapFile.canWrite()) {
                        Transformer tr = TransformerFactory.newInstance()
                                .newTransformer();
                        tr.setOutputProperty(OutputKeys.INDENT, "yes");
                        tr.setOutputProperty(OutputKeys.METHOD, "xml");
                        tr.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
                        tr.transform(new DOMSource(document),
                                new StreamResult(sitemapWriter));
                    }

                    // Open a new sitemap file for writing
                    fileNumber++;
                    sitemapFile = new File(directory, fileRoot +
                            fileNumber + ".xml");
                    sitemapWriter = new OutputStreamWriter(
                            new FileOutputStream(sitemapFile),
                            Charset.forName("UTF-8"));
                    document = this.documentBuilder.newDocument();
                    rootNode = document.createElement("urlset");
                    rootNode.setAttribute("xmlns",
                            "http://www.sitemaps.org/schemas/sitemap/0.9");
                    document.appendChild(rootNode);
                }

                Element urlElement = createSitemapEntry(document,
                        rs.getString(1), rs.getString(2));
                rootNode.appendChild(urlElement);
                counter++;
            }

            stmt.close();

            // Write out the last sitemap file and the index file so long as we
            // had at least one URL
            if (counter > 0) {
                if (sitemapFile != null && sitemapFile.canWrite()) {

                    Transformer tr = TransformerFactory.newInstance()
                            .newTransformer();
                    tr.setOutputProperty(OutputKeys.INDENT, "yes");
                    tr.setOutputProperty(OutputKeys.METHOD, "xml");
                    tr.setOutputProperty(OutputKeys.ENCODING, "UTF-8");

                    tr.transform(new DOMSource(document),
                            new StreamResult(sitemapWriter));
                }

                // Write out the sitemap index file
                writeSitemapIndex(fileNumber);
            }

            sitemapWriter.close();
        } catch (SQLException e) {
            logMetacat.warn("Error while writing to the sitemap file: "
                    + e.getMessage());
        } catch (IOException ioe) {
            logMetacat.warn("Could not open or write to the sitemap file: "
                    + ioe.getMessage());
        } catch (TransformerConfigurationException e) {
            logMetacat.warn("Could not transform (serialize) the sitemap file: "
                    + e.getMessage());
        } catch (TransformerException e) {
            logMetacat.warn("Could not transform (serialize) the sitemap file: "
                    + e.getMessage());
        } finally {
            // Return database connection to the pool
            DBConnectionPool.returnDBConnection(dbConn, serialNumber);
        }
    }

    /**
     * Create and return a `url` element for a single PID.
     *
     * @param document An instance of the Document so we can create a new
     *                 element
     * @param pid      The identifier to be turned into a URL and written in the
     *                 sitemap file
     * @return The newly-created `url` element
     */
    private Element createSitemapEntry(Document document, String pid,
                                       String lastmod)
    {
        Element urlElement = document.createElement("url");

        if (document == null || pid == null || entryBase == null) {
            return urlElement;
        }

        try {
            // Dynamically generate the url text from the PID
            StringBuffer url = new StringBuffer();
            url.append(entryBase);

            if (!entryBase.endsWith("/")) {
                url.append("/");
            }

            url.append(StringEscapeUtils.escapeXml(
                    URLEncoder.encode(pid, "UTF-8")));

            // loc
            Element locElement = document.createElement("loc");
            locElement.setTextContent(url.toString());

            // lastmod
            Date lastmodDate =
                    new SimpleDateFormat("yyyy-MM-dd H:mm:ss.S").parse(lastmod);
            Element lastmodElement = document.createElement("lastmod");
            SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd");
            lastmodElement.setTextContent(fmt.format(lastmodDate));

            urlElement.appendChild(locElement);
            urlElement.appendChild(lastmodElement);

            return urlElement;
        } catch (UnsupportedEncodingException  e) {
            logMetacat.warn("Couldn't encode PID " + pid + " in UTF-8 so this" +
                    " entry will be skipped.");
        } catch (ParseException e) {
            System.out.println("Couldn't parse " + lastmod);
            logMetacat.warn("Couldn't parse lastmod datetime of " + lastmod +
                    " so this entry will be skipped.");
        }

        return urlElement;
    }

    /**
     * Create an index file listing all of the sitemap files that were created.
     *
     * @param totalFiles The number of sitemap files that were created.
     */
    private void writeSitemapIndex(int totalFiles) {
        File sitemapIndexFile = new File(directory, indexFilename);
        OutputStreamWriter sitemapIndex = null;

        try {
            sitemapIndex = new OutputStreamWriter(
                    new FileOutputStream(sitemapIndexFile),
                    Charset.forName("UTF-8"));
            Document doc = this.documentBuilder.newDocument();
            Element root = doc.createElement("sitemapindex");
            root.setAttribute("xmlns",
                    "http://www.sitemaps.org/schemas/sitemap/0.9");

            // Write out one index entry for each sitemap file
            for (int fileNumber = 1; fileNumber <= totalFiles; fileNumber++) {
                String filename = fileRoot + fileNumber + ".xml";
                Element entry = createSitemapIndexEntry(doc, filename);
                root.appendChild(entry);
            }

            Transformer tr = TransformerFactory.newInstance().newTransformer();
            tr.setOutputProperty(OutputKeys.INDENT, "yes");
            tr.setOutputProperty(OutputKeys.METHOD, "xml");
            tr.setOutputProperty(OutputKeys.ENCODING, "UTF-8");

            doc.appendChild(root);
            tr.transform(new DOMSource(doc), new StreamResult(sitemapIndex));

            // Close the index file
            if (sitemapIndex != null) {
                sitemapIndex.close();
            }
        } catch (IOException e) {
            logMetacat.warn("Could not open or write to the sitemap index " +
                    "file: " + e.getMessage());
        } catch (TransformerException e) {
            logMetacat.warn("Could not transform (serialize) the index file:" +
                    e.getMessage());
        }
    }

    /**
     * Write a single line of the sitemap index file containing the URL to a
     * specific sitemap file.
     *
     * @param document An instance of the Document so we can create a new
     *                 element
     * @param filename the name of the index file to be used
     */
    private Element createSitemapIndexEntry(Document document,
                                            String filename) {
        Element sitemapElement = document.createElement("sitemap");

        if (filename == null || locationBase == null) {
            return sitemapElement;
        }

        // loc
        Element loc = document.createElement("loc");

        // url
        StringBuffer url = new StringBuffer();
        url.append(locationBase);

        if (!locationBase.endsWith("/")) {
            url.append("/");
        }

        url.append(filename);
        loc.setTextContent(url.toString());

        // lastmod
        Element lastmod = document.createElement("lastmod");
        Date now = new Date();
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd");
        lastmod.setTextContent(fmt.format(now));

        sitemapElement.appendChild(loc);
        sitemapElement.appendChild(lastmod);

        return sitemapElement;
    }
}
