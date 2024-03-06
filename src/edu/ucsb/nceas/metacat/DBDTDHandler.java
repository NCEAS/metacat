package edu.ucsb.nceas.metacat;

import org.xml.sax.*;


/** 
 * A database aware Class implementing DTDHandler interface for the SAX 
 * parser to call when processing the XML stream and intercepting notations 
 * and unparsed entities
 */
public class DBDTDHandler implements DTDHandler {

    /** Construct an instance of the DBDTDHandler clas
     *
     * @param conn the JDBC connection to which information is written
     */
    public DBDTDHandler() {

    }

    /** Notation declarations are not signaled */
    public void notationDecl(String name, String publicId, String systemId) throws SAXException {
        //do nothing
    }

    /** All are reported after startDocument and before first
     * startElement event
     */
    public void unparsedEntityDecl(String name, String publicId,
                                  String systemId, String notationName) throws SAXException {
        //do nothing
    }
}
