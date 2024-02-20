package edu.ucsb.nceas.metacat.admin.upgrade.solr;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.xpath.XPathAPI;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;


/**
 * A class to find the version of a JVM against which the solr is running
 * @author tao
 *
 */
public class SolrJvmVersionFinder {

    private static final String INFO_APPENDIX = "admin/info/system?wt=xml";
    private static Log logMetacat = LogFactory.getLog(SolrJvmVersionFinder.class);
    private String solrInfoUrl;
    private DocumentBuilderFactory factory;
    private DocumentBuilder builder;

    /**
     * Constructor
     * @param solrBaseUrl  the base url of this solr server
     * @throws IllegalArgumentException
     * @throws ParserConfigurationException
     */
    public SolrJvmVersionFinder(String solrBaseUrl) throws IllegalArgumentException,
                                                                    ParserConfigurationException {
        if (solrBaseUrl == null || !solrBaseUrl.startsWith("http")) {
            throw new IllegalArgumentException("SolrJvmVersionFinder.constructor - the solr base"
                                    + " url should start with http.");
        }
        if (solrBaseUrl.endsWith("/")) {
            solrInfoUrl = solrBaseUrl + INFO_APPENDIX;
        } else {
            solrInfoUrl = solrBaseUrl + "/" + INFO_APPENDIX;
        }
        logMetacat.debug("SolrJvmVersionFinder.constructor - the solr info url is " + solrInfoUrl);
        factory = DocumentBuilderFactory.newInstance();
        builder = factory.newDocumentBuilder();
    }

    /**
     * Parse the solr admin information and find the jvm version
     * @return the jvm version. An empty string will return if it can't find it.
     * @throws URISyntaxException
     * @throws IOException
     * @throws SAXException
     * @throws TransformerException
     */
    public String find() throws URISyntaxException, IOException, SAXException,
                                                                            TransformerException {
        String adminInfoXml = getSolrAdminInfo();
        //Parse the admin info
        Document doc = builder.parse(new InputSource(new StringReader(adminInfoXml)));
        NodeList list = XPathAPI.selectNodeList(doc, "//lst[@name=\"jre\"]/str[@name=\"version\"]");
        String jvm = "";
        if (list != null) {
            for (int i = 0; i < list.getLength(); i++){
                Node node = list.item(i);
                Node textNode = node.getFirstChild();
                if (textNode.getNodeType() == node.TEXT_NODE) {
                    jvm = textNode.getNodeValue();
                    break;
                }
            }
        }
        logMetacat.debug("SolrJvmVersionFinder.find - the jvm version is " + jvm);
        return jvm;
    }

    /**
     * Get the solr admin information from the solr information url.
     * The return xml segment may look like :
     * <lst name="jre">
     *     <str name="vendor">Oracle Corporation</str>
     *     <str name="version">1.8.0_351</str>
     * </lst>
     * @return the solr admin information by the xml format
     * @throws URISyntaxException
     * @throws IOException
     */
    protected String getSolrAdminInfo() throws URISyntaxException, IOException {
        URL url = new URI(solrInfoUrl).toURL();
        try (InputStream input = url.openStream()) {
            byte[] result = IOUtils.toByteArray(input);
            return new String(result, StandardCharsets.UTF_8.name());
        }
    }

}
