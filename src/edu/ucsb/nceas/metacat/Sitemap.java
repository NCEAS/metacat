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
import java.util.List;

import java.net.URLEncoder;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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
import org.dataone.client.v2.formats.ObjectFormatCache;
import org.dataone.service.types.v2.ObjectFormatList;
import org.dataone.service.types.v2.ObjectFormat;

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
    private static Log logMetacat = LogFactory.getLog(Sitemap.class);

    /** Create just a single document builder factory and builder to be
     * re-used through this class.
     */
    DocumentBuilderFactory documentBuilderFactory;
    DocumentBuilder documentBuilder;

    /** The directory in which sitemaps are written. */
    private File directory;

    /** The root url for constructing sitemap location URLs. */
    private String locationBase;

    /** The root url for constructing sitemap entry URLs for any metadata records. */
    private String entryBase;

    /** The root url for constructing sitemap entry URLs for portals. */
    private String portalBase;

    /** Set of format IDs to determine whether a record is a portal or not. */
    private List<String> portalFormats;

    /** Maximum number of URLs to write to a single sitemap file */
    static final int MAX_URLS_IN_FILE = 50000; // 50,000 according to Google

    /** The root name to be used in naming sitemap files. */
    static final String fileRoot = "sitemap";

    /** The name to give to the sitemap index file */
    static final String indexFilename = "sitemap_index.xml";

    /** lastmod (SystemMetadata.date_modified) format strings to try */
    static final String lastModFormatFull = "yyyy-MM-dd H:mm:ss.S";
    static final String lastModFormatShort = "yyyy-MM-dd H:mm:ss";

    /**
     * Construct a new instance of the Sitemap class.
     *
     * @param directory      The location to store sitemap files
     * @param locationBase   The base URL for constructing sitemap location URLs
     * @param entryBase      The base URL for constructing sitemap entry URLs any metadata records
     * @param portalBase     The base URL for constructing sitemap entry URLs for portals
     * @param portalFormats  Set of format IDs to determine whether a record is a portal or not
     */
    public Sitemap(File directory, String locationBase, String entryBase, String portalBase, List<String> portalFormats) {
        super();

        this.directory = directory;
        this.locationBase = locationBase;
        this.entryBase = entryBase;
        this.portalBase = portalBase;
        this.portalFormats = portalFormats;

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
        Date start = new Date(); // For logging the time to run this method

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
         * as we go.
         *
         * Depends on ObjectFormatCache fetching a list of available format IDs.
         *
         * We query for documents that are:
         *
         * - Metadata (their object_format is in the xml_catalog)
         * - Latest/head versions (their obsoleted_by field is NULL)
         * - Publicly readable (their access policy has a public + read perm)
         */

        String query =
            "SELECT " +
                "identifier.guid as pid, " +
                "systemmetadata.series_id as sid, " +
                "systemmetadata.date_modified as lastmod, " +
                "systemmetadata.object_format as format " +
            "FROM identifier " +
            "LEFT JOIN systemmetadata on " +
                    "identifier.guid = systemmetadata.guid " +
            "LEFT JOIN xml_access on identifier.guid = xml_access.guid " +
            "WHERE " +
            "systemmetadata.object_format in (" +
                getMetadataFormatsQueryString() +
            ") AND " +
            "systemmetadata.obsoleted_by is NULL AND " +
            "systemmetadata.archived = FALSE AND " +
            "xml_access.principal_name = 'public' AND " +
            "xml_access.perm_type = 'allow' " +
            "ORDER BY systemmetadata.date_uploaded ASC;";

        DBConnection dbConn = null;
        int serialNumber = -1;

        try {
            // Get a database connection from the pool
            dbConn = DBConnectionPool
                    .getDBConnection("Sitemap.generateSitemap()");
            serialNumber = dbConn.getCheckOutSerialNumber();

            // Execute the query statement
            PreparedStatement stmt = dbConn.prepareStatement(query);
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
                if (counter % MAX_URLS_IN_FILE == 0) {
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

                Element urlElement = createSitemapEntry(document, rs.getString(1), rs.getString(2), rs.getString(3),
                        rs.getString(4));
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

            // Onlhy close the sitemapWriter if one's still instantiated
            // which will happen when we have zero documents
            if (sitemapWriter != null) {
                sitemapWriter.close();
            }
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

        logMetacat.info("sitemap task took " + 
            ((new Date()).getTime() - start.getTime()) / 1000 + " seconds.");
    }

    /**
     * Create and return a `url` element for a single PID.
     *
     * @param document An instance of the Document so we can create a new
     *                 element
     * @param pid      The identifier to be turned into a URL and written in the
     *                 sitemap file
     * @param sid      The serids id to be turned into a URL and written in the
     *                 sitemap file. Used for portals.
     * @param lastmod  The datetime at which the objec associated with `pid` was
     *                 last modified
     * @param format   The format of the object associated with `pid`
     *
     * @return The newly-created `url` element
     */
    private Element createSitemapEntry(Document document, String pid, String sid, String lastmod, String format)
    {
        Element urlElement = document.createElement("url");

        if (document == null || pid == null || entryBase == null) {
            return urlElement;
        }

        try {
            // Dynamically generate the url text from the PID
            StringBuffer url = new StringBuffer();

            // url
            // Does different stuff depending on whether this is a portal or not
            if (portalFormats.contains(format)) {
                url.append(portalBase);

                if (!portalBase.endsWith("/")) {
                    url.append("/");
                }

                // Use a SID only if we have one (we should), otherwise use the pid
                if (sid != null) {
                    url.append(StringEscapeUtils.escapeXml(URLEncoder.encode(sid, "UTF-8")));
                } else {
                    url.append(StringEscapeUtils.escapeXml(URLEncoder.encode(pid, "UTF-8")));
                }
            } else {
                url.append(entryBase);

                if (!entryBase.endsWith("/")) {
                    url.append("/");
                }

                url.append(StringEscapeUtils.escapeXml(URLEncoder.encode(pid, "UTF-8")));
            }

            // loc
            Element locElement = document.createElement("loc");
            locElement.setTextContent(url.toString());
            urlElement.appendChild(locElement);

            // lastmod
            // Parsing can fail so we guard this with a null check
            Date lastmodDate = tryParseLastModDateTime(lastmod);

            if (lastmodDate != null) {
                Element lastmodElement = document.createElement("lastmod");
                SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd");
                lastmodElement.setTextContent(fmt.format(lastmodDate));
                urlElement.appendChild(lastmodElement);
            }

            return urlElement;
        } catch (UnsupportedEncodingException  e) {
            logMetacat.warn("Couldn't encode PID " + pid + " in UTF-8 so this" +
                    " entry will be skipped.");
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

    private Date tryParseLastModDateTime(String lastmod) {
        Date lastmodDate = null;

        try {
            lastmodDate =
                    new SimpleDateFormat(lastModFormatFull).parse(lastmod);
        } catch (ParseException e) {
            logMetacat.debug("Failed to parse " + lastmod +
                    " with SimpleDateFormat of " + lastModFormatFull + " " +
                    "trying the next format.");
        }

        if (lastmodDate != null) {
            return lastmodDate;
        }

        try {
            lastmodDate =
                    new SimpleDateFormat(lastModFormatShort).parse(lastmod);
        } catch (ParseException e) {
            logMetacat.debug("Failed to parse " + lastmod +
                    " with SimpleDateFormat of " + lastModFormatShort + " a " +
                    "lastmod element won't be inserted for this sitemap entry" +
                    ".");
        }

        return lastmodDate;
    }

    /**
     * Generate a comma-separated list of metadata format IDs so
     * generateSitemaps can filter the available objects to just metadata
     * objects.
     *
     * @return (string) List of metadata format ids as a comma-separated string
     * suitable for including in an SQL query. Each value is wrapped in single
     * quotes.
     */
    public String getMetadataFormatsQueryString() {
        ObjectFormatList objectFormatList = ObjectFormatCache.getInstance().listFormats();
        StringBuilder sb = new StringBuilder();

        for (org.dataone.service.types.v2.ObjectFormat fmt : objectFormatList.getObjectFormatList()) {
            if (!fmt.getFormatType().equals("METADATA")) {
                continue;
            }

            sb.append("'");
            sb.append(fmt.getFormatId().getValue());
            sb.append("'");
            sb.append(",");
        }

        // Remove final comma so we get valid SQL "in ( ... )"
        sb.deleteCharAt(sb.lastIndexOf(","));

        return sb.toString();
    }
}
