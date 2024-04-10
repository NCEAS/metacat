package edu.ucsb.nceas.metacat.lsid;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.util.Hashtable;
import java.util.ResourceBundle;
import java.util.Enumeration;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ibm.lsid.LSID;
import com.ibm.lsid.MalformedLSIDException;
import com.ibm.lsid.MetadataResponse;
import com.ibm.lsid.server.LSIDMetadataService;
import com.ibm.lsid.server.LSIDRequestContext;
import com.ibm.lsid.server.LSIDServerException;
import com.ibm.lsid.server.LSIDServiceConfig;

import edu.ucsb.nceas.metacat.MetaCatServlet;
import edu.ucsb.nceas.metacat.client.Metacat;
import edu.ucsb.nceas.metacat.client.MetacatFactory;
import edu.ucsb.nceas.metacat.client.MetacatInaccessibleException;

public class LSIDAuthorityMetaData implements LSIDMetadataService
{
    private LSIDDataLookup lookup = null;
    private static Hashtable currentLSIDs = new Hashtable();
    private static Log logger = LogFactory
                    .getLog("edu.ucsb.nceas.metacat.lsid");

    public void initService(LSIDServiceConfig cf) throws LSIDServerException
    {
        logger.info("Starting LSIDAuthorityMetadata.");
        lookup = new LSIDDataLookup();
    }

    private static final String RDF_NS = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
    private static final String DC_NS = "http://purl.org/dc/elements/1.1/";
    private static final String I3CP_NS = "urn:lsid:i3c.org:predicates:";
    private static final String I3C_CONTENT = "urn:lsid:i3c.org:types:content";
    private static final String DEFAULT_STYLESHEET = "default.xslt";

    public MetadataResponse getMetadata(LSIDRequestContext req, String[] formats)
                    throws LSIDServerException
    {
        LSID lsid = req.getLsid();
        ByteArrayInputStream theMetadata = doMetadataRequest(lsid);
        return new MetadataResponse(theMetadata, null,
                                    MetadataResponse.RDF_FORMAT);
    }

    private ByteArrayInputStream doMetadataRequest(LSID lsid) throws
        LSIDServerException {
      logger.debug("getting metadata for lsid " + lsid.getLsid());

      try {

        LSIDDataLookup myLookup = new LSIDDataLookup();
        InputStream content = myLookup.lsidData(lsid);
        InputStream content2 = myLookup.lsidData(lsid);
        if (!isEML(content2)) {
          content = getEML(lsid);
        }
        content2.close();
        setCurrentLSID(lsid);

        // based on the doctype choose the appropriate stylesheet
        String styleSheetName = null;
        String _docType = myLookup.getDocType(lsid);

        try {
          ResourceBundle rb = ResourceBundle.getBundle("metacat-lsid");
          styleSheetName = rb.getString(_docType.replaceAll(":", ""));
        }
        catch (java.util.MissingResourceException mre) {
          logger.warn("there is no style corresponding to: '" + _docType +
                      "' -- using default");
          styleSheetName = this.DEFAULT_STYLESHEET;
          mre.getMessage();
        }
        InputStream styleSheet = getClass()
            .getResourceAsStream(styleSheetName);
        TransformerFactory factory = TransformerFactory.newInstance();
        Transformer transformer = factory
            .newTransformer(new StreamSource(styleSheet));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        transformer.transform(new StreamSource(content),
                              new StreamResult(out));
        content.close();
        clearState();
        return new ByteArrayInputStream(out.toByteArray());
      }
      catch (Exception e) {
        throw new LSIDServerException(e, "Error transforming XML for: "
                                      + lsid);
      }
    }

    public static String getStringFromInputStream(InputStream input)
    {
        StringBuffer result = new StringBuffer();
        BufferedReader in = new BufferedReader(new InputStreamReader(input));
        String line;
        try {
            while ((line = in.readLine()) != null) {
                result.append(line);
            }
        } catch (IOException e) {
            logger.error("IOexception " + e);
        }
        return result.toString();
    }

    /**
     * figure out is this inputstream is an eml document or not
     * TODO: need a better way to figure out if this is an eml document
     */
    private boolean isEML(InputStream input)
    {

        if (input == null) { return false; }

        int loop = 0;
        boolean itIsEML = false;
        String line = "";
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(input));
            while ((loop < 20) && (line != null) && (!itIsEML)) {
                line = in.readLine();
                line = line.toLowerCase();
                if (line.indexOf("eml:eml") != -1) {
                    itIsEML = true;
                }
                loop++;
            }
        } catch (IOException e) {
            logger.error("ioerror in LSIDAuthorityMetadata: " + e);
        }
        return itIsEML;
    }

    /**
     * this lsid points to a data object - get the metadata objects which refer
     * to this document
     */
    private InputStream getEML(LSID theLSID)
    {

        InputStream response = null;

        // need to find things with this object in any of the elements
        // using the metacat api
        // get back dataset/docid and dataset/title

        // create the query
        String theQuery = getMetaCatQuery(theLSID);

        // get the metacat record
        Reader metaCatResponse = getMetaCatResponse(theQuery);

        // parse the metadata to get the applicable rdf information
        response = parseMetaCatResponse(metaCatResponse, theLSID);

        return response;

    }

    /**
     * given an LSID return a metacat query which will return docs mentioning
     * this LSID
     */
    private String getMetaCatQuery(LSID lsid)
    {
        logger.debug("getting Metacat Query for: " + lsid.toString());
        String ns = lsid.getNamespace();
        String id = lsid.getObject();
        String ver = lsid.getRevision();
        String theName = ns + "." + id + "." + ver;

        String theQuery = null;
        theQuery = "<?xml version=\"1.0\"?>\n"
                   + "<pathquery version=\"1.2\">\n"
                   + "  <querytitle>"
                   + theName
                   + " search</querytitle>\n"
                   + "  <returnfield>dataset/docid</returnfield>\n"
                   + "  <returnfield>dataset/title</returnfield>\n"
                   + "  <querygroup operator=\"UNION\">\n"
                   + "    <queryterm searchmode=\"contains\" casesensitive=\"false\">\n"
                   + "      <value>" + theName + "</value>\n"
                   + "      <pathexpr>anyfield</pathexpr>\n"
                   + "    </queryterm>\n" + "  </querygroup>\n"
                   + "<pathquery>\n";

        return theQuery;

    }

    /**
     * given a query string, query MetaCat and return the response
     */
    private Reader getMetaCatResponse(String query)
    {
        logger.debug("Querying the metacat server.");
        // get the metacat server from the configuration file
        //
        ResourceBundle rb = ResourceBundle.getBundle("metacat-lsid");
        String url = rb.getString("metacatserver");
        Reader r = null;
        try {

            Metacat m = MetacatFactory.createMetacatConnection(url);
            r = m.query(new StringReader(query));

        } catch (MetacatInaccessibleException mie) {
            logger.error("Metacat Inaccessible:\n" + mie.getMessage());
        } catch (Exception e) {
            logger.error("General exception:\n" + e.getMessage());
        }
        return r;
    }

    /**
     * Given a reader which is a metacat response, parse it and return the
     * appropriate rdf
     */
    private InputStream parseMetaCatResponse(Reader reader, LSID theLSID)
    {
        InputStream response = null;
        logger.debug("Parsing the metacat response.");
        // if there's more than one document, then return rdf listing the
        // documents
        // otherwise get the document and return rdf based on it

        String contents = getStringFromReader(reader);
        if (numberDocuments(contents) < 1) {
            response = noMetaDataResponse(theLSID);
        } else if (numberDocuments(contents) > 1) {
            response = metaDataList(contents, theLSID);
        } else {
            response = getMetaData(contents, theLSID);
        }
        return response;
    }

    /**
     * There's no metadata for this document
     */
    private ByteArrayInputStream noMetaDataResponse(LSID theLSID)
    {
        ResourceBundle rb = ResourceBundle.getBundle("metacat-lsid");
        String metadataLabels = rb.getString("metadatalabels");

        String result = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                        + "<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" \n"
                        + "	xmlns:dc=\"http://purl.org/dc/elements/1.1/\" \n"
                        + "	xmlns:pred=\"urn:lsid:i3c.org:predicates:\" xmlns=\"urn:lsid:"
                        + metadataLabels
                        + ":predicates:\"> \n"
                        + "<rdf:Description rdf:about=\""
                        + theLSID.getLsid()
                        + "\"> \n"
                        + "	<pred:title xmlns:pred=\"http://purl.org/dc/elements/1.1/\">There is no metadata for this LSID.</pred:title>\n"
                        + "</rdf:Description>\n" + "</rdf:RDF>\n";

        ByteArrayInputStream resultBytes = null;
        try {
        	resultBytes = new ByteArrayInputStream(result.getBytes(MetaCatServlet.DEFAULT_ENCODING));
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return resultBytes;
    }

    /**
     * There's more than one metdata document
     */
    private ByteArrayInputStream metaDataList(String contents, LSID theLSID)
    {
        ResourceBundle rb = ResourceBundle.getBundle("metacat");
        String metadataLabels = rb.getString("metadatalabels");

        String result = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                        + "<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" \n"
                        + "	xmlns:dc=\"http://purl.org/dc/elements/1.1/\" \n"
                        + "	xmlns:pred=\"urn:lsid:i3c.org:predicates:\" xmlns=\"urn:lsid:"
                        + metadataLabels
                        + ":predicates:\"> \n"
                        + "<rdf:Description rdf:about=\""
                        + theLSID.getLsid()
                        + "\"> \n"
                        + "	<pred:title xmlns:pred=\"http://purl.org/dc/elements/1.1/\">There is more than one metadata document for this LSID - which confuses me right now.  Try again soon and I'll be less confused.</pred:title>\n"
                        + "</rdf:Description>\n" + "</rdf:RDF>\n";

        ByteArrayInputStream resultBytes = null;
        try {
        	resultBytes = new ByteArrayInputStream(result.getBytes(MetaCatServlet.DEFAULT_ENCODING));
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return resultBytes;
    }

    /**
     * There's just one metadata document
     */
    private ByteArrayInputStream getMetaData(String contents, LSID theLSID)
    {
        String paramString = "<param name=\"dataset/title\">";
        ByteArrayInputStream result = null;

        if (contents.indexOf(paramString) == -1) {
            return noMetaDataResponse(theLSID);
        } else {
            String parts[] = contents.split(paramString);
            String parts2[] = parts[1].split("</param>");
            try {
                LSID newLSID = new LSID(parts2[0]);
                result = doMetadataRequest(newLSID);

            } catch (MalformedLSIDException e) {
                logger.error("problem generating LSID: " + e);
                e.printStackTrace();
            } catch (LSIDServerException e) {
                logger.error("problem generating LSID: " + e);
                e.printStackTrace();
            }
        }
        return result;
    }

    /**
     * Find out how many contents are in this metacat response I'm just using
     * string stuff for this - sort of lame, but does the trick more cool would
     * be to use xml stuff
     */
    private int numberDocuments(String contents)
    {

        String[] docSplit = contents.split("<document>");
        return (docSplit.length - 1);
    }

    /**
     * Given a reader, return a string
     */
    private String getStringFromReader(Reader reader)
    {
        StringBuffer response = new StringBuffer();

        try {
            BufferedReader bufReader = new BufferedReader(reader);

            String line = null;
            while ((line = bufReader.readLine()) != null) {
                response.append(line);
            }
            bufReader.close();

        } catch (IOException e) {
            logger.error("error getting string from reader " + e);
        }
        return response.toString();
    }

    /**
     * set the LSID for the current thread
     */
    static void setCurrentLSID(LSID lsid)
    {
        currentLSIDs.put(Thread.currentThread(), lsid);
    }

    static void clearState()
    {
        currentLSIDs.remove(Thread.currentThread());
    }

    /**
     * get the current LSID for the given thread, for use in XSLT so return a
     * string
     */
    public static String getLSID(
                         org.apache.xalan.extensions.XSLProcessorContext foo,
                         org.apache.xalan.templates.ElemExtensionCall bar)
                    throws MalformedLSIDException
    {
        return ((LSID) currentLSIDs.get(Thread.currentThread())).toString();
    }

    public static String getLSID() throws MalformedLSIDException
    {
        return ((LSID) currentLSIDs.get(Thread.currentThread())).toString();
    }
}
