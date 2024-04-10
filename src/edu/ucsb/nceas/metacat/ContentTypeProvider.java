package edu.ucsb.nceas.metacat;

import java.io.StringReader;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Hashtable;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.xpath.objects.XObject;
import org.apache.xpath.XPathAPI;
import org.apache.xerces.parsers.DOMParser;
import org.apache.xerces.dom.DocumentTypeImpl;
import org.w3c.dom.Attr;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.DocumentType;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.stream.*;
import javax.xml.transform.dom.*;

import org.ecoinformatics.eml.EMLParser;

import edu.ucsb.nceas.metacat.database.DBConnection;
import edu.ucsb.nceas.metacat.database.DBConnectionPool;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.metacat.util.DocumentUtil;
import edu.ucsb.nceas.metacat.util.MetacatUtil;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;
/**
 * This class will figure out which content type it is for a given data file.
 * First, from xml_relation to get all relative files to this data file.
 * Then from xml_documents to get physical files. From physical file pull out
 * the content type
 */
public class ContentTypeProvider
{
  private String dataFileId = null;
  private String contentType = null;
  private String packageType = null;
  private Hashtable contentTypeHash = new Hashtable();

  //Constant
  private String BETA = "beta";
  private String EML2 = "eml2";
  private static String DEFAULTCONTENTTYPE;
  static {
		try {
			DEFAULTCONTENTTYPE = PropertyService.getProperty("replication.defaultcontenttype");
		} catch (PropertyNotFoundException pnfe) {
			System.err.println("Could not get property DEFAULTCONTENTTYPE:" 
					+ pnfe.getMessage());
		}
	}
  private String FORMATPATH = "//format";
  private String TEXT       = "text";
  private String TEXTYPE    ="text/plain";
  private String XML        = "xml";
  private String XMLTYPE    = "text/xml";
  private String HTML       = "HTML";
  private String HTMLTYPE   = "text/html";
  private String GIF        = "gif";
  private String JPEG       = "jpeg";
  private String JPEGTYPE   = "image/jpeg";
  private String GIFTYPE    = "image/gif";
  private String BMP        = "bmp";
  private String BMPTYPE    = "image/bmp";
  private String TAR        = "tar";
  private String TARTYPE    ="application/x-tar";
  private String ZIP        = "zip";
  private String ZIPTYPE    = "application/x-zip-compressed";
  private String BINARY     = "binary";
  private String BINARYTYPE = "application/octet-stream";

  private String ENTITYDOCTYPE = "xml.entitydoctype";
  private String PHYSICALDOCTYPE = "xml.physicaldoctype";
  private String EML2DOCTYPE = "eml2namespace";
  private String DATAFORMAT = "dataFormat";
  private String TEXTFORMAT = "textFormat";
  private String EXTENALFORMAT = "externallyDefinedFormat";
  private String FORMATNAME = "formatName";
  private String BINARYRASTERFORMAT = "binaryRasterFormat";

  private String DATAFILEPATH ="//physical/distribution/online/url";
  private static Log logMetacat = LogFactory.getLog(ContentTypeProvider.class);

  /**
   * Constructor of ContentTypeProvider
   */
  public ContentTypeProvider(String docIdWithRevision)
  {
    dataFileId = DocumentUtil.getDocIdFromString(docIdWithRevision);
    //get relative doclist for data file and package type
    Vector docLists = null;
    docLists = getRelativeDocIdList(dataFileId);

    if ( packageType == null)
    {
      // other situation, contenetype is default value
      contentType = DEFAULTCONTENTTYPE;
    }
    else if (packageType.equals(BETA))
    {
      // for beta package and get entity docid for the data file
      String entityDocid = getTargetDocIdForBeta(docLists, ENTITYDOCTYPE);
      // get physical docid for data file
      docLists = getRelativeDocIdList(entityDocid);
      String physicalDocId = getTargetDocIdForBeta(docLists, PHYSICALDOCTYPE);
      // if no physical docid assign to this data file, content type is default
      if (physicalDocId == null)
      {

        contentType = DEFAULTCONTENTTYPE;
      }
      else
      {

        parsePhysicalDocumentForBeta(physicalDocId);
      }
    }
    else if (packageType.equals(EML2))
    {
      // for eml2 package
      // get eml document for data file
      //String eml2Docid = getTargetDocIdForBeta(docLists, EML2DOCTYPE);
      String eml2Docid = (String)docLists.elementAt(0);
      findContentTypeInEML2(eml2Docid);

    }

  }

  /** Method to get content type */
  public String getContentType()
  {
    return contentType;
  }//getContentType

  /* Method to find content type base on data format*/
  private void findContentTypeInEML2(String eml2DocId)
  {
    if (eml2DocId == null)
    {
      contentType = DEFAULTCONTENTTYPE;
      return;
    }
    DocumentImpl xmlDoc = null;
    String xmlString = null;
    StringReader read = null;
    InputSource in = null;
    DocumentBuilderFactory dfactory = null;
    Document doc = null;
    // create xml document
    try
    {
      String accNumber = eml2DocId + PropertyService.getProperty("document.accNumSeparator") +
                    DBUtil.getLatestRevisionInDocumentTable(eml2DocId);
      //System.out.println("the acc number is !!!!!!!!!!!!!!!!!"+accNumber);
      xmlDoc = new DocumentImpl(accNumber);
      xmlString = xmlDoc.toString();
      //System.out.println("the xml doc is "+xmlDoc);
      // create dom tree
      read = new StringReader(xmlString);
      in = new InputSource(read);
      dfactory = DocumentBuilderFactory.newInstance();
      dfactory.setNamespaceAware(false);
      doc = dfactory.newDocumentBuilder().parse(in);
    }
    catch (Exception e)
    {
      // if faild, set default value
      contentType = DEFAULTCONTENTTYPE;
      logMetacat.error("Error in ContentTypeProvider." +
                         "findContentTypeInEML2()" + e.getMessage());
      return;
    }
    Node dataFormatNode = findDataFormatNodeInEML2(doc, DATAFILEPATH,
                                                   dataFileId);
    if (dataFormatNode == null)
    {
      contentType = DEFAULTCONTENTTYPE;
      logMetacat.info("Couldn't find data format node");
      return;

    }
    NodeList childList  = dataFormatNode.getChildNodes();
    // go through childList
    for (int i = 0; i<childList.getLength(); i++)
    {
      Node child = childList.item(i);

      // if has text format child set to text/plain
      if (child.getNodeName() != null && child.getNodeName().equals(TEXTFORMAT))
      {
        logMetacat.info("in text format");
        contentType = TEXTYPE;
      }

      //external format
      if (child.getNodeName() != null && child.getNodeName().equals(EXTENALFORMAT))
      {
        logMetacat.info("in external format ");
        String format = getTextValueForGivenChildTag(child, FORMATNAME);
        logMetacat.info("The format is: "+format);
        // if we can find the format in the contentTypeHash table
        contentType = (String)lookUpContentType(format);
        if (contentType == null)
        {
          contentType = BINARYTYPE;
        }
      }

      // binaryRasterFormat
      if (child.getNodeName() != null && child.getNodeName().
          equals(BINARYRASTERFORMAT))
      {
        contentType = BINARYTYPE;
      }//if
    }//for
    //if contentype still be null, set default value
    if (contentType == null)
    {
      contentType = DEFAULTCONTENTTYPE;
    }
  }

  /* Method get text value of given child tagname*/
  private String getTextValueForGivenChildTag(Node parentNode,
                                              String childTagName)
  {
    String textValue = null;
    NodeList childList = parentNode.getChildNodes();
    for (int i= 0; i<childList.getLength();i++)
    {
      Node child = childList.item(i);
      if (child.getNodeName() != null && child.getNodeName().equals(childTagName))
      {
        logMetacat.info("Find child node: " + childTagName);
        Node textNode = child.getFirstChild();
        if (textNode.getNodeType() == Node.TEXT_NODE)
        {
          textValue = textNode.getNodeValue();
        }//if
      }//if
    }//for
    logMetacat.info("The text value for element- " + childTagName +
                             " is " + textValue);
    return textValue;
  }//getTExtValueForGivenChildTag

  /* Find the data format node in eml2 document */
  private Node findDataFormatNodeInEML2(Document xml, String xPath,
                                       String targetDocId)
  {
    Node targetNode = null;
    Node node = findDataFileNodeInEML2(xml, xPath, targetDocId);
    if (node != null)
    {
      // get the phycial the prent is online, grandparent is distribution
      // the grand'parent is physical
      Node phyicalNode = node.getParentNode().getParentNode().getParentNode();
      NodeList list = phyicalNode.getChildNodes();
      for (int i = 0; i < list.getLength(); i++)
      {
        Node kid = list.item(i);
        // find dataFormat node
        if (kid.getNodeType() == node.ELEMENT_NODE &&
            kid.getNodeName().equals(DATAFORMAT))
        {
          targetNode = kid;
          break;
        } //if
      } //for
      if (targetNode != null)
      {
        logMetacat.info("dataFormat node'name: " +
                                 targetNode.getNodeName());
      }
    }//if
    return targetNode;
  }
  /* Find the datafile node */
  private Node findDataFileNodeInEML2(Document xml, String xPath,
                                String targetDocId)
  {
    Node dataFileNode = null;
    NodeList list = null;
    try
    {
      list = XPathAPI.selectNodeList(xml, xPath);
    }
    catch (Exception e)
    {
      // catch an error and return null
      logMetacat.error("Error in findDataFileNode: "+e.getMessage());
      return dataFileNode;
    }
    // go through the list and find target docid in online/url
    if (list != null)
    {
      for (int i = 0; i < list.getLength(); i++)
      {
        Node node = list.item(i);
        Node textNode = node.getFirstChild();
        if (textNode.getNodeType() == node.TEXT_NODE)
        {
          String URLData = textNode.getNodeValue();
          logMetacat.info("online/url text data: " + URLData);
          //Only handle ecogrid data file
          if (URLData.indexOf(DBSAXHandler.ECOGRID) != -1 )
          {
            // Get docid from url
            String docId = 
            	DocumentUtil.getAccessionNumberFromEcogridIdentifier(URLData);
            // Get rid of revision
            docId = DocumentUtil.getDocIdFromAccessionNumber(docId);
            logMetacat.info("docid from url element in xml is: " +
                                     docId);
            //if this docid equals target one, we find it
            if (docId != null && docId.equals(targetDocId))
            {
              logMetacat.info("Find target docid in online/url: " +
                                       docId);
              dataFileNode = node;
              break;
            }
          } //if

        } //if
      } //for
    }//if

    return dataFileNode;
  }//findDataFileNode

  /* Get relative docid list and packagetype */
  private Vector getRelativeDocIdList(String id)
  {
    Vector docList = new Vector();
    String sql = "SELECT packagetype, subject from xml_relation " +
                 "where object = ?";
    ResultSet rs = null;
    PreparedStatement pStmt=null;
    DBConnection conn = null;
    int serialNumber = -1;
    try
    {
      //check out DBConnection
      conn=DBConnectionPool.getDBConnection
                                   ("ContentTypeProvider.getRelativeDocIdlist");
      serialNumber=conn.getCheckOutSerialNumber();
      pStmt = conn.prepareStatement(sql);
      // binding value
      pStmt.setString(1, id);
      //execute query
      pStmt.execute();
      rs = pStmt.getResultSet();
      // get result list
      String packType = null;
      while (rs.next())
      {
        packType = rs.getString(1);
        String subject = rs.getString(2);

        // get rid of duplicate record and add the docid into vector
        if (!docList.contains(subject))
        {

          docList.add(subject);
        }
      }//while

      // set up data package type
      if ((MetacatUtil.getOptionList(PropertyService.getProperty("xml.packagedoctype"))).
                                     contains(packType))
      {
        //this is beta4 or beta6 version
        logMetacat.info("This is beta package");
        packageType = BETA;
      }
      else if ((MetacatUtil.getOptionList
               (PropertyService.getProperty("xml.eml2_0_0namespace"))).contains(packType))
      {
        // this eml 2 document
        logMetacat.info("This is EML2.0.0 package");
        packageType = EML2;
      }
      else if ((MetacatUtil.getOptionList
               (PropertyService.getProperty("xml.eml2_0_1namespace"))).contains(packType))
      {
        // this eml 2 document
        logMetacat.info("This is EML2.0.1 package");
        packageType = EML2;
      }



    }//try
    catch(SQLException e)
    {

      logMetacat.error("ContenTypProvider.getRelativeDoclist1 " +
                             e.getMessage());
    }//catch
    catch(PropertyNotFoundException pnfe)
    {
      logMetacat.error("ContenTypProvider.getRelativeDoclist1 " +
                             pnfe.getMessage());
    }//catch
    finally
    {
      try
      {
        if(rs != null) {
            rs.close();
        }
        if(pStmt != null) {
            pStmt.close();
        }
        
      }
      catch (SQLException ee)
      {
        logMetacat.error("ContenTypProvider.getRelativeDoclist2 " +
                             ee.getMessage());
      }
      finally
      {
        DBConnectionPool.returnDBConnection(conn, serialNumber);
      }
    }//finally

    return docList;
  }// getRelativeDocIdList

  /* Method to get physical document for data file in xml_documents table for
   * beta eml package
   */
  private String getTargetDocIdForBeta(Vector list, String targetType)
  {
    String docId = null;
    // make sure list is not empty
    if (list.isEmpty())
    {

      return docId;
    }
    // get sql command
    String sql = "SELECT doctype, docid from xml_documents where docid in ( ";
    // the first element
    sql = sql + "?";
    // remaining values
    for (int i = 1; i < list.size(); i++) {
      sql = sql + ", ?";
    }
    // add parentheses
    sql = sql + ")";
    logMetacat.info("SQL for select doctype: "+ sql);
    ResultSet rs = null;
    PreparedStatement pStmt=null;
    DBConnection conn = null;
    int serialNumber = -1;
    try
    {
      //check out DBConnection
      conn=DBConnectionPool.getDBConnection
                                 ("ContentTypeProvider.setPhycialDocIdForBeta");
      serialNumber=conn.getCheckOutSerialNumber();
      pStmt = conn.prepareStatement(sql);
      // set the parameter values
      for (int i = 0; i < list.size(); i++) {
        String docid = (String) list.elementAt(i);
        pStmt.setString(i+1, docid);
      }
      //execute query
      pStmt.execute();
      rs = pStmt.getResultSet();
      // get result list
      while (rs.next())
      {
        String packType = rs.getString(1);
        String targetId  = rs.getString(2);
        // find physical document
        if ((MetacatUtil.getOptionList(PropertyService.getProperty(targetType))).
                                     contains(packType))
       {
         // assign physical document and jump out the while loop
         docId = targetId;
         break;
       }
      }//while

    }//try
    catch(SQLException e)
    {

      logMetacat.error("ContenTypProvider.setPhysicalDocIdForBeta1 " +
                             e.getMessage());
    }//catch
    catch(PropertyNotFoundException pnfe)
    {

      logMetacat.error("ContenTypProvider.setPhysicalDocIdForBeta1 " +
                             pnfe.getMessage());
    }//catch
    finally
    {
      try
      {
          if(rs != null) {
              rs.close();
          }
          if(pStmt != null) {
              pStmt.close();
          }
        
      }
      catch(SQLException ee)
      {
        logMetacat.error("ContenTypProvider.setPhysicalDocIdForBeta2 " +
                             ee.getMessage());
      }//catch
      finally
      {
        DBConnectionPool.returnDBConnection(conn, serialNumber);
      }
    }//finally
    logMetacat.info("target docid is: "+ docId + " "+
                             "for target doctype: "+targetType);
    return docId;
  }




  /* Parser the beta physical document and find the value in format element*/
  private void parsePhysicalDocumentForBeta(String physicalDocid)
  {
    String xmlDoc = null;
    try
    {
      String accNumber = physicalDocid + PropertyService.getProperty("document.accNumSeparator") +
        DBUtil.getLatestRevisionInDocumentTable(physicalDocid);
      //System.out.println("the accenumber is !!!!!!!!!!!!!!!!!!" + accNumber);
      DocumentImpl doc = new DocumentImpl(accNumber);
      xmlDoc = doc.toString();
      //System.out.println("The physical xml is "+xmlDoc);
    }
    catch (Exception e)
    {
      contentType = DEFAULTCONTENTTYPE;
      logMetacat.error("Error in ContentTypeProvider." +
                         "parsePhysicalDocumentForBeta()" + e.getMessage());
      return;
    }
      // get format element's text value
    String format = getTextValueFromPath(new StringReader(xmlDoc), FORMATPATH);

    if (format == null)
    {
      // if couldn't find the format, set contentype default value;
      contentType = DEFAULTCONTENTTYPE;
    }
    else
    {
      // if can find a format and look up from hash to get value
      contentType = lookUpContentType(format);
      // couldn't find the content type for this format in hash table
      if (contentType == null)
      {
        //set default vlaue
        contentType = DEFAULTCONTENTTYPE;
      }//if
    }//else
  }//parsePhysicalDocumentForBeta

  private String getTextValueFromPath(StringReader xml, String xPath)
  {
    String textValue = null;
    // get nodelist from doc by path
    try
    {
      NodeList list = EMLParser.getPathContent(xml, xPath);
      Node elementNode = list.item(0);
      Node textNode = elementNode.getFirstChild();
      if (textNode.getNodeType() == Node.TEXT_NODE)
      {
        textValue = textNode.getNodeValue();// get value
      }

    }
    catch (Exception e)
    {
      logMetacat.error("error in ContentTypeProvider."+
                               "getTextValueFromPath: "+e.getMessage());
    }
    logMetacat.info("The text value for " + xPath + " is: "+
                              textValue);
    return textValue;
  }//getTextValueFromPath

  /* A method to look up contentype */
  private String lookUpContentType(String format)
  {
    String newFormat = null;
    constructContentHashTable();
    newFormat = format.toLowerCase().trim();
    String type = null;
    type = (String)contentTypeHash.get(newFormat);
    logMetacat.info("contentType looked from hashtalbe is: " +
                              type);
    return type;
  }// lookupcontentypes

  /* Construct content type hashtable */
  private void constructContentHashTable()
  {
    contentTypeHash.put(TEXT, TEXTYPE);
    contentTypeHash.put(XML, XMLTYPE);
    contentTypeHash.put(HTML,HTMLTYPE);
    contentTypeHash.put(GIF, GIFTYPE);
    contentTypeHash.put(JPEG, JPEGTYPE);
    contentTypeHash.put(BMP, BMPTYPE);
    contentTypeHash.put(TAR, TARTYPE);
    contentTypeHash.put(ZIP, ZIPTYPE);
    contentTypeHash.put(BINARY, BINARYTYPE);

  }//constructrContentHashTable();



  public static void main(String[] argus)
  {
     try
     {
       DBConnectionPool pool = DBConnectionPool.getInstance();
       //ContentTypeProvider provider = new ContentTypeProvider("tao.9830");
       ContentTypeProvider provider = new ContentTypeProvider("tao.0001");
       String str = provider.getContentType();
       logMetacat.info("content type is : " + str);
     }
     catch(Exception e)
     {
       logMetacat.error("erorr in Schemalocation.main: " +
                                e.getMessage());
     }
  }
}//ContentTypeProvider
