/**
 *  '$RCSfile: XSLTransform.java,v $'
 *  Copyright: 2003 Regents of the University of California and the
 *             National Center for Ecological Analysis and Synthesis
 *
 *   '$Author: jones $'
 *     '$Date: 2003/08/18 20:27:03 $'
 * '$Revision: 1.4 $'
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

package edu.ucsb.nceas.metacat.util;

import java.io.*;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import org.apache.log4j.Logger;

import edu.ucsb.nceas.metacat.DocumentImpl;
import edu.ucsb.nceas.metacat.DocumentImplWrapper;
import edu.ucsb.nceas.metacat.McdbException;
import edu.ucsb.nceas.metacat.database.DBConnection;
import edu.ucsb.nceas.metacat.database.DBConnectionPool;
import edu.ucsb.nceas.metacat.properties.PropertyService;

import javax.xml.transform.TransformerFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.URIResolver;
import org.ecoinformatics.eml.EMLParser;


/**
 * A Class that transforms older eml version to newer eml version utitlizing XSL style sheets. 
 */
public class EMLVersionsTransformer {
	
	private static org.apache.log4j.Logger logMetacat = Logger.getLogger(EMLVersionsTransformer.class);
	private static String eml210StyleFile = null;
	static{
		try
		{
			eml210StyleFile =PropertyService.getProperty("application.deployDir")+"/"+PropertyService
			.getProperty("application.context")+ "/style/common/eml201to210.xsl"; //eml201to210.xsl place
		}
		catch(Exception e)
		{
			logMetacat.warn("Couldn't get eml201to210.xsl stylesheet");
		}
	}
	private static String DOT = ".";
	private static int CAP = 100000; // How many documents you want to transform.

    /**
     * Public constructor because all methods are static and do not need 
     * an instance.
     */
    public EMLVersionsTransformer() 
    {
    	
    }
    
    /**
     * Method to upgrade old versions of eml to new version
     */
    public void upgrade()
    {
    	upgradeEML200ToEML210();
    }
    
    /*
     * Upgrade every eml200 or eml210 documents into eml210
     */
    private  void upgradeEML200ToEML210()
    {
    	Vector list = getEML2DocList();
    	if(list != null)
    	{
    		// Choose the smaller value between the size of vector and CAP
    		int size = list.size();
    		if (size > CAP)
    		{
    			size = CAP;
    		}
    		for(int i=0; i<size; i++)
    		{
    			OwnerAndDocid pair = (OwnerAndDocid)list.elementAt(i);
    			String docid = pair.getDocid();
    			String owner = pair.getOwner();
    			try
    			{
    				handleSingleEML200Document(docid, owner);
    				try
    				{
    					Thread.sleep(5000);
    				}
    				catch(Exception e)
    				{
    					logMetacat.warn("Couldn't sleep 5 seconds");
    				}
    			}
    			catch(Exception e)
    			{
    				logMetacat.warn("The docid "+docid+" with owner "+owner+" couldn't be transformed to eml-2.1.0 since "+e.getMessage());
    				transformErrorLog("The docid "+docid+" with owner "+owner+" couldn't be transformed to eml-2.1.0 since "+e.getMessage());
    			}
    		}
    	}
    }
    
    /*
     * Handle single eml201 or eml 200 document: read the document, transform it to eml210 document 
     * then save it to 210 document into metacat
     */
    private  void handleSingleEML200Document(String docidWithRev, String owner) throws Exception
    {
    	DocumentImpl docImpl = new DocumentImpl(docidWithRev);
    	String eml200Content = docImpl.toString();
    	StringReader eml200Source= new StringReader(eml200Content);
    	//PipedWriter eml210OutputAfterTransform = new PipedWriter();
    	//PipedReader eml210SourceForNewDoc = new PipedReader();
    	//eml210SourceForNewDoc.connect(eml210OutputAfterTransform);
    	StringWriter strWriter = new StringWriter();
    	String newId = increaseRevisionNumber(docidWithRev);
    	if(newId != null)
    	{
    	     transformEML200ToEML210(eml200Source, eml210StyleFile,  strWriter, newId);
    	     String eml210Content = strWriter.toString();
    	     String rule = DocumentImpl.EML210;
             // using emlparser to check id validation
             EMLParser parser = new EMLParser(eml210Content);
             DocumentImplWrapper documentWrapper = new DocumentImplWrapper(rule, true, true);
//             StringReader xml = new StringReader(eml210Content);
             String  doAction = "UPDATE";
             String pub = null;
             String []groups = null;
             DBConnection dbconn = null;
             StringReader dtd = null;
             int serialNumber = -1;
             try
             {
            	 dbconn = DBConnectionPool
                 .getDBConnection("EMLVersionsTransformer.handleSingleEML200Document");
                  serialNumber = dbconn.getCheckOutSerialNumber();
                  documentWrapper.write(dbconn, eml210Content, pub, dtd,
                          doAction, newId, owner, groups, null);
                  logMetacat.warn("Doc "+docidWithRev+" was transformed to eml210 with new id "+newId);
                  transformLog("Doc "+docidWithRev+" was transformed to eml210 with new id "+newId);
             }
             catch(Exception e)
             {
            	 throw e;
             }
             finally
             {
            	 // Return db connection
                 DBConnectionPool.returnDBConnection(dbconn, serialNumber);
             }
    	}
    	else
    	{
    		logMetacat.warn("Couldn't increase docid "+docidWithRev+"'s revision");
    	}
    }

    /*
     * Transform single eml201 (Reader) to eml 210 (Writer)
     */
    private static void transformEML200ToEML210(Reader reader, String xslfile, Writer writer, String packageid) throws Exception{    
        	Hashtable param = null;
            if (packageid != null)
            {
            	param = new Hashtable();
            	param.put("package-id", packageid);
            }
            EMLVersionsTransformer.transform(reader, xslfile, writer, param);
         
    }


    /*
     * Transform an XML document using an XSLT stylesheet to another format,
     * probably HTML or another XML document format.
     *
     * @param doc the document to be transformed
     * @param xslSystemId the system location of the stylesheet
     * @param pw the PrintWriter to which output is printed
     * @param params some parameters for inclusion to the transformation
     */
    private static void transform(Reader doc, String xslSystemId,
        Writer pw, Hashtable param) throws Exception
    {
        
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
        
    }
    
    /*
     * Get list of document (docid and owner) which type is eml200 or eml201. 
     * The docid in the list will have revision number too.
     */
    private Vector getEML2DocList()
    {
    	Vector list = new Vector();
    	 DBConnection dbconn = null;
         int serialNumber = -1;
         String sql = "select docid, rev, user_owner from xml_documents where doctype like 'eml://ecoinformatics.org/eml-2.0.1' or doctype like 'eml://ecoinformatics.org/eml-2.0.0'";
         PreparedStatement pstmt = null;
         try {
             dbconn = DBConnectionPool
                     .getDBConnection("EMLVersionsTransformer.getEML2DocList");
             serialNumber = dbconn.getCheckOutSerialNumber();
             pstmt = dbconn.prepareStatement(sql.toString());
             pstmt.execute();
             ResultSet rs = pstmt.getResultSet();
             boolean tableHasRows = rs.next();
             while (tableHasRows) {
                 String docidWithoutRev = rs.getString(1);
                 int rev = rs.getInt(2);
                 String owner = rs.getString(3);
                 String docidWithRev = docidWithoutRev+DOT+rev;
                 logMetacat.info("The docid "+docidWithRev+" with owner "+owner+" will be added into list which will be transformed to eml-2.1.0");
                 OwnerAndDocid pair = new OwnerAndDocid(owner, docidWithRev);;
                 list.add(pair);
                 tableHasRows = rs.next();
             }
             pstmt.close();

        
        } catch (SQLException e) {
             logMetacat.error("error in DocumentImpl.getDocumentInfo: "
                + e.getMessage());
             e.printStackTrace(System.out);
        } finally {
            try {
                 pstmt.close();
            } catch (SQLException ee) {
              logMetacat.error(
                    "error in DocumentImple.getDocumentInfo: "
                            + ee.getMessage());
           } finally {
              DBConnectionPool.returnDBConnection(dbconn, serialNumber);
        }
      }
      return list;
    }
    
    /*
     * Increase revision number for the given docid. tao.1.1 will be tao.1.2. null will be returned
     * if couldn't increase it.
     */
    private static String increaseRevisionNumber(String docidWithRev)
    {
    	String newid = null;
    	try
    	{
    	  if (docidWithRev != null)
    	  {
    		int index = docidWithRev.lastIndexOf(DOT);
    		if (index != -1)
    		{
    			String firstTwoParts = docidWithRev.substring(0,index);
    			String revStr = docidWithRev.substring(index+1);
    			Integer revObj = new Integer(revStr);
    			int rev = revObj.intValue();
    			rev= rev+1;
    			newid = firstTwoParts+DOT+rev;
    		}
    	  }
    	}
    	catch(Exception e)
    	{
    		logMetacat.warn("Couldn't increase revision number since "+e.getMessage());
    	}
    	return newid;
    }
    
    
    /*
	 * Method for writing transformation messages to a log file specified in
	 * metacat.properties
	 */
	private static void transformLog(String message) {
		try {
			FileOutputStream fos = 
				new FileOutputStream(PropertyService.getProperty("replication.logdir")
					+ "/transform.log", true);
			PrintWriter pw = new PrintWriter(fos);
			SimpleDateFormat formatter = new SimpleDateFormat("yy-MM-dd HH:mm:ss");
			java.util.Date localtime = new java.util.Date();
			String dateString = formatter.format(localtime);
			dateString += " :: " + message;
			// time stamp each entry
			pw.println(dateString);
			pw.flush();
		} catch (Exception e) {
			logMetacat.warn("error writing to transform log from "
					+ "EMLVersionTransformer.transformlLog: " + e.getMessage());
			// e.printStackTrace(System.out);
		}
	}

  /*
	 * Method for writing transform messages to a log file specified in
	 * metacat.properties
	 */
  private static void transformErrorLog(String message)
  {
    try
    {
    	FileOutputStream fos = 
			new FileOutputStream(PropertyService.getProperty("replication.logdir")
				+ "/transformerror.log", true);
      PrintWriter pw = new PrintWriter(fos);
      SimpleDateFormat formatter = new SimpleDateFormat ("yy-MM-dd HH:mm:ss");
      java.util.Date localtime = new java.util.Date();
      String dateString = formatter.format(localtime);
      dateString += " :: " + message;
      //time stamp each entry
      pw.println(dateString);
      pw.flush();
    }
    catch(Exception e)
    {
      logMetacat.warn("error writing to transforming error log from " +
                         "EMLVersionTransformer.transformErrorLog: " + e.getMessage());
      //e.printStackTrace(System.out);
    }
  }
    
    /*
     * Class reprents a document's docid and its owner 
     * @author tao
     *
     */
    class OwnerAndDocid{
    	private String owner = null;
    	private String docidWithRev = null;
    	
    	public OwnerAndDocid(String owner, String docidWithRev)
    	{
    		this.owner = owner;
    		this.docidWithRev = docidWithRev;
    	}
    	
    	public String getOwner()
    	{
    		return owner;
    	}
    	
    	public String getDocid()
    	{
    		return docidWithRev;
    	}
    }
    

  
}

