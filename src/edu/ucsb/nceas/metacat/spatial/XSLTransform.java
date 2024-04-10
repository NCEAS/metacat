package edu.ucsb.nceas.metacat.spatial;

import java.io.*;
import java.net.URL;
import java.net.MalformedURLException;
import java.sql.*;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Stack;

import javax.xml.transform.TransformerFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.URIResolver;

import org.apache.xerces.parsers.DOMParser;
import org.w3c.dom.Attr;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.DocumentType;
import org.xml.sax.SAXException;
import org.xml.sax.InputSource;
import org.apache.xerces.dom.DocumentTypeImpl;
import org.apache.xpath.XPathAPI;
import org.w3c.dom.NamedNodeMap;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

//import edu.ucsb.nceas.utilities.IOUtil;

/**
 * A Class that transforms XML documents utitlizing XSL style sheets. This is
 * a convenience class that makes it easier to handle the location of
 * stylesheets and the mapping between a request and a particular stylesheet.
 */
public class XSLTransform {

    /**
     * Private constructor because all methids are static and do not need 
     * an instance.
     */
    public XSLTransform() 
    {
    }

    /**
     * Transform an XML document using an XSLT stylesheet to another format,
     * probably HTML or another XML document format.
     *
     * @param docString the document to be transformed
     * @param xslSystemId the system location of the stylesheet
     * @param pw the PrintWriter to which output is printed
     * @param params some parameters for inclusion to the transformation
     */
    public static void transform(String docString, String xslSystemId,
        PrintWriter pw, Hashtable param)
    {
        transform(new StringReader(docString), xslSystemId, pw, param);
    }

    /**
     * Transform an XML document using an XSLT stylesheet to another format,
     * probably HTML or another XML document format.
     *
     * @param doc the document to be transformed
     * @param xslSystemId the system location of the stylesheet
     * @param pw the PrintWriter to which output is printed
     * @param params some parameters for inclusion to the transformation
     */
    public static void transform(Reader doc, String xslSystemId,
        PrintWriter pw, Hashtable param)
    {
        try {

            StreamSource xslSource = 
                new StreamSource(xslSystemId);
            xslSource.setSystemId(xslSystemId);
            // Create a stylesheet from the system id that was found
            TransformerFactory tFactory = TransformerFactory.newInstance();
            Transformer transformer = tFactory.newTransformer(xslSource);

            // Set up parameters for transformation
            if ( param != null) {
                Enumeration en = param.keys();
                while (en.hasMoreElements()) {
                    String key =(String)en.nextElement();
                    String value = ((String)(param.get(key)));
                    transformer.setParameter(key, value);
                }
            }

            // Run the transform engine
            StreamSource ss = new StreamSource(doc);
            StreamResult sr = new StreamResult(pw);
            transformer.transform(ss, sr);
        } catch (Exception e) {
            pw.println("Error transforming document in " +
               "XSLTransform.transform:\n" + e.getMessage());
            e.printStackTrace(pw);
        }
    }

    /**
     * the main routine used to test the transform utility.
     *
     * Usage: java DBTransform
     */
    static public void main(String[] args) {

        if (args.length != 2)
        {
            System.err.println("Wrong number of arguments!!!");
            System.err.println("USAGE: java XSLTransform xml style");
            return;
        } else {
            String xmlfile = args[0];
            String xslfile = args[1];
            try {
                Reader r = new FileReader(xmlfile);
                XSLTransform.transform( r, xslfile, 
                        new PrintWriter(System.out), null);
            } catch (Exception e) {
                System.err.println("EXCEPTION HANDLING REQUIRED");
                System.err.println(e.getMessage());
                e.printStackTrace(System.err);
            }
        }
    }
}
