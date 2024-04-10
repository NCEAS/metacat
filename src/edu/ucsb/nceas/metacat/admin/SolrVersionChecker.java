package edu.ucsb.nceas.metacat.admin;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.xpath.XPathAPI;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * This class will check if the given solr home directory is for an embedded solr server
 * which is used in the earlier Metacat versions. From Metacat 2.9.0, we have migrated the solr
 * server from embedded to http. For the new http server, we like it to have a new solr home.
 * This class will check if the given solr home is still the old one.
 * @author tao
 *
 */
public class SolrVersionChecker {
    private static final String CONFDIR = "conf";
    private static final String CONFILE = "solrconfig.xml";
    private static final String LUCENEPATH = "//luceneMatchVersion";
    private static final String VERSION34 = "LUCENE_34";
    
    private DocumentBuilder builder = null;
    private Log logMetacat = LogFactory.getLog(SolrVersionChecker.class);
    
    /**
     * Default constructor to initialize the parser factory.
     * @throws ParserConfigurationException 
     */
    public SolrVersionChecker() throws ParserConfigurationException {
        DocumentBuilderFactory dFactory = DocumentBuilderFactory.newInstance();
        dFactory.setNamespaceAware(false);
        builder = dFactory.newDocumentBuilder();
        //doc = dfactory.newDocumentBuilder().parse(in);
    }

    /**
     * Check if given the solr home is for Lucene version 3.4.
     * @param solrHome  the solr home will be checked
     * @return true if it is for Lucene; otherwise false.
     * @throws IOException 
     * @throws SAXException 
     * @throws TransformerException 
     */
    public boolean isVersion_3_4 (String solrHome) throws SAXException, IOException, TransformerException {
        boolean isVersion34 = false;
        String configFile = solrHome+File.separator+CONFDIR+File.separator+CONFILE;
        Reader reader = new InputStreamReader(new FileInputStream(configFile), "UTF-8");
        InputSource source = new InputSource(reader);
        Document doc = builder.parse(source);
        NodeList list = XPathAPI.selectNodeList(doc, LUCENEPATH);
        if (list != null) {
            for (int i = 0; i < list.getLength(); i++){
                Node node = list.item(i);
                Node textNode = node.getFirstChild();
                if (textNode.getNodeType() == node.TEXT_NODE) {
                    String version = textNode.getNodeValue();
                    logMetacat.info("SolrVersionChecker.isVersion_3_4 - the value for the path "+LUCENEPATH +" has been found and its value is "+version+ " And our target value is "+VERSION34);
                    if(version != null && version.equals(VERSION34)) {
                        isVersion34 = true;
                    }
                }
            }
        }
        return isVersion34;
    }
}
