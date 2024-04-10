package edu.ucsb.nceas.metacat.lsid;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ibm.lsid.ExpiringResponse;
import com.ibm.lsid.LSID;
import com.ibm.lsid.server.LSIDServerException;
import com.ibm.lsid.server.LSIDServiceConfig;
import com.ibm.lsid.server.impl.SimpleAuthority;
import com.ibm.lsid.wsdl.HTTPLocation;
import com.ibm.lsid.wsdl.LSIDDataPort;
import com.ibm.lsid.wsdl.LSIDMetadataPort;
import com.ibm.lsid.wsdl.SOAPLocation;

public class LSIDAuthorityMain extends SimpleAuthority
{

    private LSIDDataLookup lookup = null;
    private static Log logger = LogFactory
                    .getLog("edu.ucsb.nceas.metacat.lsid");

    public void initService(LSIDServiceConfig cf) throws LSIDServerException
    {
        logger.info("Starting LSIDAuthorityMain.");
        lookup = new LSIDDataLookup();
    }

    public ExpiringResponse getKnownURIs() throws LSIDServerException
    {
        logger.debug("In LSIDAuthorityMain.getKnownURIs()");
        return null;
    }

    public LSIDMetadataPort[] getMetadataLocations(LSID lsid, String url)
    {
        logger.debug("In LSIDAuthorityMain.getMetadataLocations()");
        if (lookup == null)
            return null;

        int lsType;
        try {
            lsType = lookup.lsidType(lsid);
        } catch (LSIDServerException ex) {
            ex.printStackTrace();
            lsType = LSIDDataLookup.UNKNOWN;
        }
        if (lsType == LSIDDataLookup.UNKNOWN)
            return null;

        HostDescriptor hd = new HostDescriptor(url);
        return new LSIDMetadataPort[] {
                        // thau added http metadata port here
                        new HTTPLocation(hd.host, hd.port,
                                         hd.pathPrefix + "/authority/metadata"),
                        new SOAPLocation(hd.baseURL + "metadata")};
    }

    public LSIDDataPort[] getDataLocations(LSID lsid, String url)
    {
        logger.debug("In LSIDAuthorityMain.getDataLocations()");
        if (lookup == null)
            return null;

        int lsType;
        try {
            lsType = lookup.lsidType(lsid);
        } catch (LSIDServerException ex) {
            ex.printStackTrace();
            lsType = LSIDDataLookup.UNKNOWN;
        }
        if (lsType == LSIDDataLookup.UNKNOWN)
            return null;
        if (lsType == LSIDDataLookup.ABSTRACT)
            return new LSIDDataPort[0];

        HostDescriptor hd = new HostDescriptor(url);
        return new LSIDDataPort[] {
                        new HTTPLocation(hd.host, hd.port, hd.pathPrefix
                                                           + "/authority/data"),
                        new SOAPLocation(hd.baseURL + "data")};
    }

    private static final Pattern HOST_PTN = Pattern
                    .compile("https?://([^/:]+)(?::(\\d+))?(.*)/authority(.*)");

    /* Q&D implementation */
    private class HostDescriptor
    {
        public String host;
        public int port;
        public String pathPrefix;
        public String baseURL;

        public HostDescriptor(String url)
        {
            logger.debug("Creating a HostDescriptor for: " + url);
            host = "localhost";
            port = -1;
            pathPrefix = "";
            if (url != null || url.length() > 0) {
                logger.debug("HostDescriptor: url is > 0 length");
                Matcher m = HOST_PTN.matcher(url);
                if (m.lookingAt()) {
                    host = m.group(1);
                    logger.debug("HostDescriptor.host: " + host);
                    if ((m.group(2) != null) && (m.group(2).length() > 0)) {
                        port = Integer.parseInt(m.group(2));
                    }
                    logger.debug("HostDescriptor.port: " + port);
                    pathPrefix = m.group(3);
                    logger.debug("HostDescriptor.pathPrefix: " + pathPrefix);
                }
            }
            if (port > 0) {
                baseURL = "http://" + host + ":" + port + pathPrefix
                          + "/authority/";
            } else {
                baseURL = "http://" + host + pathPrefix + "/authority/";
            }
            logger.debug("HostDescriptor.baseURL: " + baseURL);
        }
    }
}
