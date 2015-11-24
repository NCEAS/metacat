/**
 *  '$RCSfile$'
 *  Copyright: 2000 Regents of the University of California and the
 *              National Center for Ecological Analysis and Synthesis
 *    Purpose: To test the ReplicationServerList class by JUnit
 *    Authors: Jing Tao
 *
 *   '$Author$'
 *     '$Date$'
 * '$Revision$'
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package edu.ucsb.nceas.metacattest;

import edu.ucsb.nceas.metacat.*;
import edu.ucsb.nceas.metacat.client.Metacat;
import edu.ucsb.nceas.metacat.client.MetacatFactory;
import edu.ucsb.nceas.metacat.replication.*;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.metacat.util.MetacatUtil;
import edu.ucsb.nceas.utilities.IOUtil;
import edu.ucsb.nceas.utilities.XMLUtilities;
//import edu.ucsb.nceas.morpho.framework.*;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestResult;
import junit.framework.TestSuite;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;


/**
 * This class is used to change the data file location for IPCC eml documents.
 * Currently IPCC eml documents point data file ti SRB server. However, the srb 
 * earthgrid is not very stable. We decided to change the online URL from srb to knb.
 * So this class will handle this case.
 * Before running this program, it needs:
 * 1. Downloaded data files from SRB
 * 2. A list of IPCC docid(with revision number) text file. If the text file is not available, it need
 * a metacat query file to search metacat to get the doicd list.
 * What the class will do:
 * 1. It will read the eml from Metacat.
 * 2. Get online URL information from eml document by DOM parser.
 * 3. Base on the URL information, this program will find the data file in
 *     the direcotry which contains the srb data file.
* 4. It will generate docid for the data file.
* 5. Upload the download srb data file to Metacat with assigned docid.
* 6. Modify the eml document with the new URL information (pointing to
 *     knb) and new version number in eml
 * 7. Update it to a new version in Metacat.
 * 8 . Go through above 7 steps for every eml document in the list.
 * 
 */ 
public class UploadIPCCDataTest extends TestCase
{
  
	 
	  /* Initialize properties*/
	  static
	  {
		  try
		  {
			  PropertyService.getInstance();
		  }
		  catch(Exception e)
		  {
			  System.err.println("Exception in initialize option in MetacatServletNetTest "+e.getMessage());
		  }
	  }
	  
	  /**Constants*/
	  private static String SRBDATAFILEDIR = "/home/tao/data-file/IPCC"; // Dir for storing srb data file
	  private static String DOCLISTFILE       = "docidList"; // File name which stores IPCC document id
	  private static String METACATURL      = "http://chico.dyndns.org/metacat/metacat";
	  private static String USERNAME          = "uid=dpennington,o=LTER,dc=ecoinformatics,dc=org";
	  private static String PASSWORD           = "password";
	  private static String TABLEONLINEURL= "/eml:eml/dataset/dataTable/physical/distribution/online/url";
	  private static String SPATIALONLINEURL = "/eml:eml/dataset/spatialRaster/physical/distribution/online/url";
	  private static String PACKAGEID               ="/eml:eml/@packageId";
	  private static String SRB                           = "srb://";
	  private static String KNB                           = "ecogrid://knb/";
	  private static String DATAIDPREFIX          = "IPCC";
	  private static String DOT                           = ".";
	  private static String SUCCESSLOG             = "update.log";
	  private static String ERRORLOG                = "error.log";
	  private static String CURRENT_CORRECTFILENAME = "correct_filename.csv";
	  private File log = new File(SUCCESSLOG);
	  private File error = new File (ERRORLOG);
	  
	  
	  /**
	   * Constructor to build the test
	   *
	   * @param name the name of the test method
	   */
	  public UploadIPCCDataTest(String name)
	  {
	    super(name);
	    
	  }


	  /**
	   * Create a suite of tests to be run together
	   */
	  public static Test suite()
	  {
		   TestSuite suite = new TestSuite();
		   //suite.addTest(new UploadIPCCDataTest("modifyEMLDocsWithCorrectDataFileName"));
		   //suite.addTest(new UploadIPCCDataTest("modifyEMLDocsWithIncorrectDataFileName"));
		    return suite;
	 }
	  
	  /**
	   * Modify EML Docs' data url online from SRB to ecogrid.
	   *Those eml docs pointe valide srb file names.
	   */
	  public void modifyEMLDocsWithCorrectDataFileName()
	  {
		  boolean originalDataFileIncorrect = false;
		  updateEML(originalDataFileIncorrect);
	  }
	  /**
	   * Modify EML Docs' data url online from SRB to ecogrid.
	   *Those eml docs pointe invalide srb file names.
	   */
	  public void modifyEMLDocsWithIncorrectDataFileName()
	  {
		  boolean originalDataFileIncorrect = true;
		  updateEML(originalDataFileIncorrect);
	  }
	  /*
	   * Upload the data file to Metacat and modify the eml documents
	   * @return
	   * @throws Exception
	   */
	  private void updateEML(boolean originalDataFileIncorrect)
	  {
		  
		      // Get eml document first
			  Vector list = getDocumentList();
			  //If list is not empty, goes through every document by handleSingleEML method -
			  //1. It will read the eml from Metacat.
			  // 2. Get online URL information from eml document by DOM parser.
			  // 3. Base on the URL information, this program will find the data file in
			  // the direcotry which contains the srb data file.
			  // 4. It will generate docid for the data file
              // 5. At last upload the download srb data file to Metacat with assigned docid.
			  // 6. Modify the eml document with the new URL information (pointing to
			  // knb) and new version number in eml.
			  // 7.Update it to a new version in Metacat.
			  
              if (list != null && !list.isEmpty())
              {
            	   int size = list.size();
            	   for (int i=0; i<size; i++)
            	   {
            		   String docid = null;
            		   try
            		   {
            			   docid = (String)list.elementAt(i);
            			   String dataId = handleSingleEML(docid, originalDataFileIncorrect);
            			   String message = "Successfully update eml "+docid + " with data id "+dataId;
            			   writeLog(log, message);
            		   }
            		   catch(Exception e)
            		   {
            			   System.err.println("Failed to handle eml document "+docid + " since "+
            					   e.getMessage());
            			   String message = "failed to update eml "+docid + "\n "+e.getMessage();
            			   writeLog(error, message);
            		   }
            	   }
              }
              else
              {
            	  System.err.println("There is no EML document to handle");
              }
		
	  }
	  
	  /*
	   * Does actually job to upload data file and modify eml document for a given id.
	   * Here are its tasks:
	   * 1. It will read the eml from Metacat.
	   * 2. Get online URL information from eml document by DOM parser.
	   * 3. Base on the URL information, this program will find the data file in
	   *     the direcotry which contains the srb data file.
	   * 4. It will generate docid for the data file.
	   * 5. Upload the download srb data file to Metacat with assigned docid.
	   * 6. Modify the eml document with the new URL information (pointing to
	   *     knb) and new version number in eml
	   * 7. Update it to a new version in Metacat.
	   * 
	   */	 
	  private String handleSingleEML(String docid,boolean originalDataFileIncorrect) throws Exception
	  {
		  Metacat metacat = MetacatFactory.createMetacatConnection(METACATURL);
		  // login metacat 
		  String response = metacat.login(USERNAME, PASSWORD);
		  if (response.indexOf("<login>") == -1)
		  {
			  throw new Exception("login failed "+response);
		  }
		  // 1. Reads eml document from metacat
		  Reader r = new InputStreamReader(metacat.read(docid));
          Document DOMdoc = XMLUtilities.getXMLReaderAsDOMDocument(r);
          Node rootNode = (Node)DOMdoc.getDocumentElement();
          
          //2.  Gets online url information. If onlineUrl is not SRB, through an exception
          String onlineUrl = getOnLineURL(rootNode);
          //System.out.println("=================The url is "+onlineUrl);
          
          //3. Find the srb data file 
          String dataFileName = getDataFileNameFromURL(onlineUrl);
          //System.out.println("the data fileName in eml "+dataFileName);
          //If the dataFileName in original eml is wrong, we need to look up the
          // the correct name first
          if (originalDataFileIncorrect)
          {
        	  Hashtable correctName = getCurrent_CorrectFileNamesPair();
        	  dataFileName =(String) correctName.get(dataFileName);
          }
          //System.out.println("=================The data file is "+dataFileName);
          File dataFile = null;
          dataFile = new File(SRBDATAFILEDIR,dataFileName);
           if (!dataFile.exists())
           {
        	  throw new Exception("Couldn't find the data file in srb data directory "+dataFile);
          }
           
          //4. Generate docid for data file
          String dataId = generateId();
          //System.out.println("=======The docid for data file will be "+dataId);
          
          //5. upload data file to Metacat
          response = metacat.upload(dataId, dataFile);
          if (response.indexOf("<success>") == -1)
          {
        	  throw new Exception("Couldn't upload data file "+dataFileName +
        			  " with id "+dataId+ " into Metacat since "+response);
          }
          
          //6. Updates eml online url and package id in DOM
          String newId = updateEMLDoc(rootNode, docid, dataId);
          //System.out.println("The new docid is ========"+newId);
          
          //Put EML DOM with the new packagId and oneline url into a StringWriter and store it to String
          StringWriter stringWriter = new StringWriter();
          PrintWriter printWriter = new PrintWriter(stringWriter);
		  XMLUtilities.print(rootNode, printWriter);
		  String xml = stringWriter.toString();
		  //System.out.println("the xml is "+xml);		  
		  
		  //7.insert new (update) EML document into Metacat
          StringReader xmlReader = new StringReader(xml);
          response = metacat.update(newId, xmlReader, null);
          if (response.indexOf("<success>") == -1)
          {
        	  throw new Exception("Upload data file "+dataFileName +
        			  " with id "+dataId+ " successfully but update eml "+newId +" failed since "+ response);
          }
          metacat.logout();
          return dataId;
	  }
	  
	  /*
	   * Gets onlineUrl value from a given eml DOM document.
	   * The online url xpath can be "/eml/dataset/dataTable/physical/distribution/online/url"
	   * or "/eml/dataset/spatialRaster/physical/distribution/online/url"
	   */
	  private String getOnLineURL(Node root) throws Exception
	  {
		  String url = null;
		  if (root == null)
		  {
			  throw new Exception("root node for this EML is null and couldn't get online url from it");
		  }
		  Node urlNode = XMLUtilities.getTextNodeWithXPath(root, TABLEONLINEURL);
		  // in table online url does exist, we will try to use another xpath - SPATIALONLEURL
		  if (urlNode == null)
		  {
			  urlNode = XMLUtilities.getTextNodeWithXPath(root, SPATIALONLINEURL);
		  }
		  // Couldn't find any matche element, throw exception
		  if(urlNode == null)
		  {
			  throw new Exception("Couldn't find any onlie url information in eml document");
		  }
		  //Gets text node value and if the url doesn't contain "srb;//", it will throw a exception
		 url = urlNode.getNodeValue();
		 if (url == null || url.indexOf(SRB)== -1)
		 {
			 throw new Exception("The online url doesn't have srb protocol and we don't need to handle");
		 }
		  return url;
	  }
	  
	  /*
	   * Automatically to generate a unique id for ddata file. 
	   * This id will be looked like - DATAIDPREFIX.numberBaseonTime.1, e.g
	   * IPCC.20072321.1
	   */
	  private String generateId()
	  {
		  int version = 1;
		  StringBuffer docid = new StringBuffer(DATAIDPREFIX);
		  docid.append(DOT);
				     
		  // Create a calendar to get the date formatted properly
		  String[] ids = TimeZone.getAvailableIDs(-8 * 60 * 60 * 1000);
		  SimpleTimeZone pdt = new SimpleTimeZone(-8 * 60 * 60 * 1000, ids[0]);
		  pdt.setStartRule(Calendar.APRIL, 1, Calendar.SUNDAY, 2*60*60*1000);
		  pdt.setEndRule(Calendar.OCTOBER, -1, Calendar.SUNDAY, 2*60*60*1000);
		  Calendar calendar = new GregorianCalendar(pdt);
		  Date trialTime = new Date();
		  calendar.setTime(trialTime);

			int time = 0; 
			
			docid.append(calendar.get(Calendar.YEAR));
			
			time = calendar.get(Calendar.DAY_OF_YEAR);
			if(time < 10){
				docid.append("0");
				docid.append("0");
				docid.append(time);
			} else if(time < 100) {
				docid.append("0");
				docid.append(time);
			} else {
				docid.append(time);
			}
			
			time = calendar.get(Calendar.HOUR_OF_DAY);
			if(time < 10){
				docid.append("0");
				docid.append(time);
			} else {
				docid.append(time);
			}
			
			time = calendar.get(Calendar.MINUTE);
			if(time < 10){
				docid.append("0");
				docid.append(time);
			} else {
				docid.append(time);
			}
			
			time = calendar.get(Calendar.SECOND);
			if(time < 10){
				docid.append("0");
				docid.append(time);
			} else {
				docid.append(time);
			}		    
			 //sometimes this number is not unique, so we append a random number
			int random = (new Double(Math.random()*100)).intValue();
			docid.append(random);
			docid.append(DOT);
			docid.append(version);
			
			return docid.toString();
		 
	  }
	  /*
	   * Get data file name from online url. SRB oneline url will looks like -
	   * srb://seek:/home/beam.seek/IPCC_climate/Present/ccld6190.dat.
	   * The last part - ccld6190.dat is the file name. This method will get
	   * the file name from the give url
	   */
	  private String getDataFileNameFromURL(String onlineUrl) throws Exception
	  {
		  String dataFile = null;
		  String slash = "/";
		  if (onlineUrl != null)
		  {
			  int index = onlineUrl.lastIndexOf(slash);
			  try
			  {
			     dataFile = onlineUrl.substring(index+1);
			  }
			  catch(Exception e)
			  {
				  throw new Exception("Couldn't get data file name from the given url "+onlineUrl+
						  " since "+e.getMessage());
			  }
		  }
		  return dataFile;
	  }
	  
	  
     
      /*
       * Gets eml document list from text file. The text file format should be:
       * tao.1.1
       * tao.2.1
       */
	  private Vector getDocumentListFromFile() throws Exception
	  {
		  Vector docList = new Vector();
		  File docListFile = new File(SRBDATAFILEDIR,DOCLISTFILE);
		  FileReader docListFileReader= new FileReader(docListFile);
		  BufferedReader readDocList = new BufferedReader(docListFileReader);
		  // Read every line from the text file and put it into a vector
		  String docid = readDocList.readLine();
		  while (docid != null)
		  {
			  // If the docid string is not empty, put it into vector
			  if (!docid.trim().equals(""))
			  {
			     docList.add(docid.trim());
			  }
			  docid = readDocList.readLine();
		  }
		  return docList;
	  }
	  
	  /*
	   * Update the given eml document (in DOM). There are two places to be updated 
	   * The package id will be increased 1, i.e.,  from 1 to 2. The distribution online url will
	   * point to the new ecogrid id, i.e. , ecogrid://knb/IPCC.2007.1 
	   */
	  private String updateEMLDoc(Node root, String docid, String dataId) throws Exception 
	  {
		  // update package id
		  docid = getIncreasedNewDocid(docid);
		  XMLUtilities.addAttributeNodeToDOMTree( root, PACKAGEID, docid);
	      // update online url.  oneline url should either in spatialRaster or dataTable.
		  // First try to see if spatialRaster exist or not. If not try data table
		  String newUrl = KNB+dataId; //new url looks like ecogrid://knb/IPCC.2007.1
		  boolean isSpatialRaster = true;
		  boolean isDataTable = false;
		  Node urlNode = XMLUtilities.getTextNodeWithXPath(root, SPATIALONLINEURL);
		  if (urlNode == null)
		  {
			  // has no spatialRaster 
			 isSpatialRaster = false;		
		  }
		  else
		  {
			  // has spatialRaster
			  isSpatialRaster = true;		  
		  }
		  // determin if has datable or not
		  urlNode = XMLUtilities.getTextNodeWithXPath(root,TABLEONLINEURL);
		  if (urlNode != null)
		  {
				 isDataTable = true;
		  }
		  
		  if (isSpatialRaster && !isDataTable)
		  {
			 //only has spatialRaster and no dataTable, update spatialRaster online url
			  XMLUtilities.addTextNodeToDOMTree(root, SPATIALONLINEURL, newUrl);
		  }
		  else if (!isSpatialRaster && isDataTable)
		  {
              //only has dataTable and no spatialRaster, update dataTable online url
			  XMLUtilities.addTextNodeToDOMTree(root, TABLEONLINEURL, newUrl);
		  }
		  else
		  {
			  //some strange things happen
			  throw new Exception("The eml either has both dataTable or spatialRaster OR doesn't has any entity");
		  }
		  return docid;
	  }
	  
	  /*
	   * Gets new docid with increased version. Docid looks like tao.1.1. The new docid will be
	   * tao.1.2.
	   */
	  private String getIncreasedNewDocid(String docid) throws Exception
	  {
		  int rev = 1;
		  String revision = null;
		  String prefix = null;
		  String newId = null;
		  if (docid != null)
		  {
			  int index = docid.lastIndexOf(DOT);
			  try
			  {
				 // Get revsion part(1)
			     revision = docid.substring(index+1);
			     // Get prefix part (tao.1.)
			     prefix    = docid.substring(0, index+1);
			     // increase version from 1 to 2
			     rev = (new Integer(revision)).intValue();
			     rev++;
			     // combines the prefix tao.1. and new revision2 to get tao.1.2
			     newId= prefix+rev;
			     
			  }
			  catch(Exception e)
			  {
				  throw new Exception("Couldn't increase revsion number from the given docid "+docid+
						  " since "+e.getMessage());
				
			  }
		  }
		  return newId;
	  }
	  
	  /*
	   * Gets eml document list from searching Metacat
	   * TO-DO: This method need to be implemented
	   */
	  private Vector getDocumentListFromMetacat()
	  {
		  Vector docList = new Vector();
		  return docList;
	  }
	  
	  /*
	   * Get eml document list. First this method will try
	   * to get the eml document list form text file. If the result is empty or
	   * it caught an exception it will try to get eml document list from metacat.
	   */
	  private Vector getDocumentList()
	  {
		  Vector list = null;
		  try
		  {
			  //First, try to get eml doc list from text file
			  list = getDocumentListFromFile();
			  if (list == null || list.isEmpty())
			  {
				  throw new Exception("The eml doclist is empty in text file");
			  }
		  }
		  catch(Exception e)
		  {
			  System.err.println("Couldn't get eml document list from text file: "+e.getMessage());
			  // If an exception happened, try to get eml doc list from metacat
			  list = getDocumentListFromMetacat();
		  }
		  if (list != null)
		  {
			  System.out.println("the list is "+list);
		  }
		  return list;
	  }
	  
	  /*
	   * Writes error message into log file.
	   */
	  private void writeLog(File file, String message)
	  {
	    try
	    {
	      FileOutputStream fos = new FileOutputStream(file, true);
	      PrintWriter pw = new PrintWriter(fos);
	      SimpleDateFormat formatter = new SimpleDateFormat ("yy-MM-dd HH:mm:ss");
	      java.util.Date localtime = new java.util.Date();
	      String dateString = formatter.format(localtime);
	      dateString += " :: " + message;
	      //time stamp each entry
	      pw.println(dateString);
	      pw.flush();
	      pw.close();
	      fos.close();
	    }
	    catch(Exception e)
	    {
	      System.out.println("error writing to replication log from " +
	                         "MetacatReplication.replLog: " + e.getMessage());
	      //e.printStackTrace(System.out);
	    }
	 }
	  
	  /*
	   * Read a csv file which contains current data file name and correct data file name. 
	   * The format of csv file is:
	   * currentname1,correctname1
	   * currentname2,correctname2
	   * ........
	   * The return value is hash table, the current data file name is key and correct file name is
	   * value.
	   */
	   private Hashtable getCurrent_CorrectFileNamesPair() throws Exception
	   {
		   Hashtable fileNamesHash = new Hashtable();
		   File current_correctFileNames = new File(CURRENT_CORRECTFILENAME);
		   FileReader fileReader= new FileReader(current_correctFileNames);
		   BufferedReader readDocList = new BufferedReader(fileReader);
		   // Read every line from the text file, this line will look like:
		   // currentname1,correctname1
		   String lineString = readDocList.readLine();
		   while (lineString != null)
		   {
			   //Get the comma index number
			   int commaIndex = lineString.indexOf(",");
			   if (commaIndex != -1)
			   {
			      //Get the current file name part
			      String currentName = lineString.substring(0, commaIndex);
			      //Get the correct file name part
			      String correctName = lineString.substring(commaIndex+1, lineString.length());
				  if (currentName != null && correctName != null)
				  {
					  fileNamesHash.put(currentName.trim(), correctName.trim());
				  }
			  }
			   lineString = readDocList.readLine();
		   }
		   return fileNamesHash;
	   }
}
