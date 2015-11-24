package org.kepler.web.service;

import com.google.gwt.user.client.rpc.*;
import com.google.gwt.user.server.rpc.*;

import org.kepler.web.client.*;

import edu.ucsb.nceas.metacat.client.*;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.apache.xpath.XPathAPI;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.xml.sax.InputSource;

import java.io.*;
import java.util.*;

public class KeplerServiceServlet extends RemoteServiceServlet implements KeplerService
{
  private String url = "http://library.kepler-project.org/kepler/metacat";
  
  /**
   * takes in a query string and a sessionid and returns a resultset
   */
  public MetacatQueryResult[] query(String query, String sessionid)
  {
    try
    { //query metacat
      MetacatClient client = (MetacatClient)MetacatFactory.createMetacatConnection(url);
      String queryDoc = createQueryDocument(query);
      Reader queryResultReader = client.query(new StringReader(queryDoc));
      //now we have the result document, parse it and return it as MQR[]
      InputSource is = new InputSource(queryResultReader);
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      DocumentBuilder builder = factory.newDocumentBuilder();
      Document doc = builder.parse(is);
      NodeList docs = XPathAPI.selectNodeList(doc, "/resultset/document");
      MetacatQueryResult[] mqr = new MetacatQueryResult[docs.getLength()];
      for(int i=0; i<docs.getLength(); i++)
      {
        Node docNode = docs.item(i);
        //parse each document result into an MQR
        mqr[i] = new MetacatQueryResult();
        String docid = XPathAPI.selectSingleNode(docNode, "docid").getFirstChild().getNodeValue();
        String name = XPathAPI.selectSingleNode(docNode, "param[@name='/entity/@name']").getFirstChild().getNodeValue();
        
        mqr[i].setDocid(docid);
        mqr[i].setName(name);
        mqr[i].setDescription("");
      }
      
      return mqr;
    }
    catch(Exception e)
    {
      return null;
    }
  }
  
  /**
   * takes in credentials and returns a sessionid
   */
  public String login(String user, String pass)
  {
    try
    {
      MetacatClient client = (MetacatClient)MetacatFactory.createMetacatConnection(url);
      return client.login("uid=kepler,o=unaffiliated,dc=ecoinformatics,dc=org", "kepler");
    }
    catch(Exception e)
    {
      return "error: " + e.getMessage();
    }
  }
  
  public String logout()
  {
    return "logout";
  }
  
  /**
   * takes in a docid and a sessionid and returns a document
   */
  public String read(String docid, String sessionid)
  {
    return "read";
  }
  
  private String createQueryDocument(String queryString)
  {
    String query = "<?xml version=\"1.0\"?>" +
      "<pathquery version=\"1.2\">" +
      "<querytitle>Untitled-Search-1</querytitle>" +
      "<returndoctype>entity</returndoctype>" +
      "<returndoctype>eml://ecoinformatics.org//eml-2.0.0</returndoctype>" +
      "<returndoctype>-//ecoinformatics.org//eml-dataset-2.0.0beta4//EN</returndoctype>" +
      "<returndoctype>-//ecoinformatics.org//eml-dataset-2.0.0beta6//EN</returndoctype>" +
      "<returndoctype>-//NCEAS//eml-dataset-2.0//EN</returndoctype>" +
      "<returndoctype>-//NCEAS//resource//EN</returndoctype>" +
      "<returnfield>/entity/@name</returnfield>" +
      "<querygroup operator=\"INTERSECT\">" +
        "<querygroup operator=\"UNION\">" +
            "<queryterm searchmode=\"contains\" casesensitive=\"false\">" +
              "<value>" + queryString + "</value>" +
            "</queryterm>" +
        "</querygroup>" +
      "</querygroup>" +
      "</pathquery>";
    return query;
  }
}
