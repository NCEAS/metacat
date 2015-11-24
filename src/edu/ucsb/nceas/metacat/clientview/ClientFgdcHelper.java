/*
 * ClientFgdcHelper.java
 *
 * Created on June 25, 2007, 9:58 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package edu.ucsb.nceas.metacat.clientview;

import edu.ucsb.nceas.metacat.client.MetacatException;
import edu.ucsb.nceas.utilities.XMLUtilities;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author barteau
 */
public abstract class ClientFgdcHelper {
    
    private static XPath                    xpath = XPathFactory.newInstance().newXPath();
    
    /**
     * Data Document ID location within an FGDC document.  XPath expression.
     */
    public static final String              FGDC_DATA_FILE_DOCID_XPATH = "/metadata/distinfo/stdorder/digform/digtopt/onlinopt/computer/networka/networkr";
    public static final String              FGDC_DATA_FILE_QUERY_XPATH = FGDC_DATA_FILE_DOCID_XPATH + "[text()='%1s']";
    public static final String              FGDC_DATA_FILE_NAME_XPATH = "digtinfo/formcont";
    
    /**
     * FGDC Data Document ID location within an FGDC document, relative to the "distinfo" parent node.
     * XPath expression.
     */
    public static final String              FGDC_DATA_FILE_NODES_XPATH = "stdorder/digform/digtopt/onlinopt/computer/networka/networkr";
    
    public static final String              PATH4ANCESTOR = FGDC_DATA_FILE_DOCID_XPATH + "[text()='%1s']/ancestor::node()[name()='%2s']";
    public static final String              SUB_DOCS_PATH = FGDC_DATA_FILE_DOCID_XPATH + "/text()";
    
    /**
     * Metadata Document ID location within an FGDC document.  XPath expression.
     */
    public static final String              FGDC_DOCID_XPATH = "/metadata/distinfo/resdesc";
    public static final String              FGDC_FILE_NAME_XPATH = "custom";
    /**
     * Metadata Document ID query template within an FGDC document.  Substitute %1s with Doc Id.
     */
    public static final String              XPATH_QUERY_TEMPLATE = FGDC_DOCID_XPATH + "[text()='%1s']";
    public static final String              FGDC_DOCID_ROOT_XPATH = XPATH_QUERY_TEMPLATE + "/ancestor::node()[name()='distinfo']";
    
    /**
     * Identifies the FGDC DTD.
     */
    public static final String              FGDC_SYSTEM_ID = "http://www.fgdc.gov/metadata/fgdc-std-001-1998.dtd";
    
    /**
     * Handles a client's request to delete a document.
     * If its a data document, it removes the Doc Id from the FGDC metadata in the
     * Metacat database.  It determines what metadata Doc ID is including this Doc Id.
     * It then queries metacat for the parent FGDC document and removes the Doc Id from it, and
     * reloads the new version with a new revision number.
     * If its a metadata document, it deletes any related data documents, then it
     * deletes the metadata.  In either instance, it sets the server feedback in
     * the session ("updateFeedback").
     * @param request HttpServletRequest which contains docId parameter.
     */
    public static void clientDeleteRequest(ClientView clientViewBean, ClientViewHelper clientViewHelper) {
        String                      result = null, docId, subDocId, parentDocId, revisedDocId;
        NodeList                    nodeLst;
        Node                        node;
        Document                    resultSetDoc;
        
        docId = clientViewBean.getDocId();
        try {
            //*** First, determine what metadata file is including this file (if any).
            resultSetDoc = clientViewHelper.query(FGDC_DATA_FILE_DOCID_XPATH, docId, null);
            parentDocId = xpath.evaluate("/resultset/document/docid", resultSetDoc.getDocumentElement());
            if (parentDocId != null && !parentDocId.equals("")) {
                clientViewHelper.setMetadataDoc(parentDocId);
                //*** Remove Doc Id from any parent metadata document.
                revisedDocId = removeDataDocIdFromFGDC(docId, parentDocId, clientViewHelper);
                clientViewBean.setDocId(revisedDocId);
                //*** Set the new Metadata Doc Id in the bean.
                clientViewBean.setMetaFileDocId(revisedDocId);
            } else {
                clientViewHelper.setMetadataDoc(docId);
                //*** This is a metadata document, so remove all of the sub-docId's.
                nodeLst = (NodeList) xpath.evaluate(SUB_DOCS_PATH, clientViewHelper.getMetadataDoc().getDocumentElement(), XPathConstants.NODESET);
                for(int i = 0; i < nodeLst.getLength(); i++) {
                    node = nodeLst.item(i);
                    subDocId = node.getNodeValue();
                    //*** Remove the sub-document.
                    try {
                        clientViewHelper.getMetacatClient().delete(subDocId);
                    } catch (MetacatException ex) {
                        ex.printStackTrace();
                    }
                }
                //*** We're deleting the Meta data doc, so clear it from the bean.
                clientViewBean.setMetaFileDocId(null);
            }
            //*** Remove the document.
            result = clientViewHelper.getMetacatClient().delete(docId);
            
            //*** Save the server feedback in the session, to be used by the view.
            clientViewBean.setMessage(ClientView.DELETE_MESSAGE, result);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    private static String removeDataDocIdFromFGDC(String docId, String parentDocId, ClientViewHelper clientViewHelper) throws Exception {
        String                          pathToDigform, revision = "", xPathQuery, tmp;
        Document                        doc;
        InputStream                     response;
        Properties                      prop;
        Node                            node;
        NodeList                        nodeLst;
        Reader                          reader;
        
        //*** Get the metadata document and remove the digform branch.
        doc = clientViewHelper.getMetadataDoc();
        if (doc != null) {
            tmp = PATH4ANCESTOR.replaceFirst("%1s", docId);
            pathToDigform = tmp.replaceFirst("%2s", "digform");
            node = (Node) xpath.evaluate(pathToDigform, doc.getDocumentElement(), XPathConstants.NODE);
            node.getParentNode().removeChild(node);
            xPathQuery = XPATH_QUERY_TEMPLATE.replaceFirst("%1s", parentDocId);
            revision = clientViewHelper.nextVersion(parentDocId, xPathQuery);
            reader = XMLUtilities.getDOMTreeAsReader(doc.getDocumentElement(), false);
            clientViewHelper.getMetacatClient().update(revision, reader, null);
        }
        return(revision);
    }
    
    public static boolean handlePackageUpload(String metaDocId,
            HashMap dataDocIDs,
            String contactName,
            String metaFNm,
            Document metadataDoc) throws IOException {
        boolean                         result = true;
        Node                            newBranch, metaRootNode;
        
        //*** Store the User Name and Doc Id in the FGDC document.
        newBranch = getFGDCdisinfo(contactName, metaDocId, metaFNm, dataDocIDs);
        //System.out.println("ClientFgdcHelper.handlePackageUpload: " + XMLUtilities.getDOMTreeAsString(newBranch));
        metaRootNode = addDistInfoToFGDC(newBranch, metadataDoc);
        return(result);
    }
    
    
    public static boolean isFGDC(Document metadataDoc) {
        boolean                     result = false;
        DocumentType                docType;
        String                      sysId;
        final String                FGDC_TEST_EXPRESSION = "/metadata/idinfo/citation/citeinfo/title";
        Node                        node = null;
        
        //*** First, try the rigid proper way of determining it.
        if (metadataDoc != null) {
            docType = metadataDoc.getDoctype();
            if (docType != null) {
                sysId = docType.getSystemId();
                if (sysId != null)
                    result = (sysId.indexOf(FGDC_SYSTEM_ID) > -1);
            }
        }
        //*** It might not have a doc type line, so try another method.
        if (metadataDoc != null && !result) {
            try {
                node = (Node) xpath.evaluate(FGDC_TEST_EXPRESSION, metadataDoc.getDocumentElement(), XPathConstants.NODE);
            } catch (XPathExpressionException ex) {
                ex.printStackTrace();
            }
            result = (node != null);
        }
        return(result);
    }
    
    public static boolean hasMetacatInfo(String docId, Document metadataDoc) {
        boolean                     result = false;
        String                      queryResult, xPathQuery;
        
        //xPathQuery = String.format(XPATH_QUERY_TEMPLATE, docId);
        xPathQuery = XPATH_QUERY_TEMPLATE.replaceFirst("%1s", docId);
        try {
            queryResult = xpath.evaluate(xPathQuery, metadataDoc);
            result = (queryResult != null && !queryResult.equals(""));
        } catch (XPathExpressionException ex) {
            ex.printStackTrace();
        }
        return(result);
    }
    
    private static Node getFGDCdisinfo(String contactName, String resourceDescription, String metaFNm, HashMap dataDocIDs) throws IOException {
        Node                        result = null, node, digformBranch, formname, stdorder, formcont;
        Document                    doc;
        Iterator                    iterIt;
        String                      key, value, fileInfo[];
        
        //*** This is a valid/minimal FGDC "distinfo" branch.
        final String XML = "<distinfo>"
                + "    <distrib>"
                + "        <cntinfo>"
                + "            <cntperp>"
                + "                <cntper></cntper>"
                + "            </cntperp>"
                + "            <cntaddr>"
                + "                <addrtype></addrtype>"
                + "                <address></address>"
                + "                <city></city>"
                + "                <state></state>"
                + "                <postal></postal>"
                + "                <country></country>"
                + "            </cntaddr>"
                + "            <cntvoice></cntvoice>"
                + "        </cntinfo>"
                + "    </distrib>"
                + "    <resdesc></resdesc>"
                + "    <distliab></distliab>"
                + "    <stdorder>"
                + "        <digform>"
                + "            <digtinfo>"
                + "                <formname></formname>"
                + "                <formcont></formcont>"
                + "            </digtinfo>"
                + "            <digtopt>"
                + "                <onlinopt>"
                + "                    <computer>"
                + "                        <networka>"
                + "                            <networkr></networkr>"
                + "                        </networka>"
                + "                    </computer>"
                + "                </onlinopt>"
                + "            </digtopt>"
                + "        </digform>"
                + "        <fees></fees>"
                + "    </stdorder>"
                + "    <custom></custom>"
                + "</distinfo>";
        
        doc = XMLUtilities.getXMLReaderAsDOMDocument(new StringReader(XML));
        result = doc.getDocumentElement();
        try {
            //*** Set the Contact Person.
            node = (Node) xpath.evaluate("/distinfo/distrib/cntinfo/cntperp/cntper", result, XPathConstants.NODE);
            ClientViewHelper.setTextContent(xpath, node, contactName);
            //node.setTextContent(contactName);  Not in java 1.4
            
            //*** Set the metadata Doc Id.
            node = (Node) xpath.evaluate("/distinfo/resdesc", result, XPathConstants.NODE);
            ClientViewHelper.setTextContent(xpath, node, resourceDescription);
            //node.setTextContent(resourceDescription);  Not in java 1.4
            
            //*** Set the metadata filename.
            node = (Node) xpath.evaluate("/distinfo/custom", result, XPathConstants.NODE);
            ClientViewHelper.setTextContent(xpath, node, metaFNm);
            //node.setTextContent(metaFNm);  Not in java 1.4
            
            //*** Loop thru the files, setting their format and Doc Id.
            stdorder = (Node) xpath.evaluate("/distinfo/stdorder", result, XPathConstants.NODE);
            digformBranch = (Node) xpath.evaluate("/distinfo/stdorder/digform", result, XPathConstants.NODE);
            iterIt = dataDocIDs.keySet().iterator();
            while(iterIt.hasNext()) {
                //*** Save the data file Doc ID (required).
                key = (String) iterIt.next();
                node = (Node) xpath.evaluate("digtopt/onlinopt/computer/networka/networkr", digformBranch, XPathConstants.NODE);
                ClientViewHelper.setTextContent(xpath, node, key);
                //node.setTextContent(key);
                
                fileInfo = (String[]) dataDocIDs.get(key);
                if (fileInfo != null) {
                    //*** Save the data file format (optional).
                    formname = (Node) xpath.evaluate("digtinfo/formname", digformBranch, XPathConstants.NODE);
                    if ((value = fileInfo[ClientView.FORMAT_TYPE]) != null && !value.equals("")) {
                        ClientViewHelper.setTextContent(xpath, formname, value);
                        //formname.setTextContent(value);
                    } else {
                        //*** We did a deep clone of the branch, so clear prior contents.
                        ClientViewHelper.setTextContent(xpath, formname, "");
                        //formname.setTextContent("");
                    }
                    //*** Save the data file name.
                    formcont = (Node) xpath.evaluate("digtinfo/formcont", digformBranch, XPathConstants.NODE);
                    if ((value = fileInfo[ClientView.FILE_NAME]) != null && !value.equals("")) {
                        ClientViewHelper.setTextContent(xpath, formcont, value);
                        //formcont.setTextContent(value);
                    } else {
                        //*** We did a deep clone of the branch, so clear prior contents.
                        ClientViewHelper.setTextContent(xpath, formcont, "");
                        //formcont.setTextContent("");
                    }
                }
                //*** Clone branch for next file.
                if (iterIt.hasNext()) {
                    digformBranch = digformBranch.cloneNode(true);
                    stdorder.appendChild(digformBranch);
                }
            }
        } catch (XPathExpressionException ex) {
            ex.printStackTrace();
        }
        return(result);
    }
    
    private static Node addDistInfoToFGDC(Node newBranch, Document metadataDoc) {
        Node                        result = null, node;
        
        if (newBranch != null) {
            result = metadataDoc.getDocumentElement();
            try {
                //*** Get a reference to the FGDC required "metainfo" node (only 1 allowed).
                node = (Node) xpath.evaluate("/metadata/metainfo", result, XPathConstants.NODE);
                if (node != null) {
                    newBranch = metadataDoc.importNode(newBranch, true);
                    //*** Add the new "distinfo" before it.
                    result.insertBefore(newBranch, node);
                }
            } catch (XPathExpressionException ex) {
                ex.printStackTrace();
            }
        }
        return(result);
    }
    
    public static void updateFileNameAndType(Node root, String dataDocId, String[] fileInfo) {
        Node                digform;
        String              tmp, pathToDigform;
        final String        FORMNAME_PATH = "digtinfo/formname";  //*** File format type.
        final String        FORMCONT_PATH = "digtinfo/formcont";  //*** Original file name.
        
        tmp = PATH4ANCESTOR.replaceFirst("%1s", dataDocId);
        pathToDigform = tmp.replaceFirst("%2s", "digform");
        digform = ClientViewHelper.getNode(xpath, pathToDigform, root);
        if (digform != null) {
            ClientViewHelper.updateNodeText(digform, xpath, FORMNAME_PATH, fileInfo[ClientView.FORMAT_TYPE]);
            ClientViewHelper.updateNodeText(digform, xpath, FORMCONT_PATH, fileInfo[ClientView.FILE_NAME]);
        }
    }
    
    public static void updateMetadataFileName(Node root, String metadataDocId, String fileName) {
        String                  pathToResdesc;
        Node                    resdesc;
        
        if (fileName != null && !fileName.equals("")) {
            pathToResdesc = XPATH_QUERY_TEMPLATE.replaceFirst("%1s", metadataDocId);
            ClientViewHelper.updateNodeText(root, xpath, pathToResdesc, metadataDocId);
        }
    }
}
