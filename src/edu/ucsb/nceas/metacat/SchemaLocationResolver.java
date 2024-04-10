package edu.ucsb.nceas.metacat;


import java.sql.*;
import java.util.Vector;
import java.io.File;
import java.io.BufferedInputStream;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xml.sax.SAXException;

import edu.ucsb.nceas.metacat.database.DBConnection;
import edu.ucsb.nceas.metacat.database.DBConnectionPool;
import edu.ucsb.nceas.metacat.service.ServiceService;
import edu.ucsb.nceas.metacat.service.XMLSchemaParser;
import edu.ucsb.nceas.metacat.service.XMLSchemaService;
import edu.ucsb.nceas.metacat.util.SystemUtil;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;

/**
 * A database aware Class to handle schema location. If namespace is in the
 * xml_catalog table (public_id), the schema location specified in xml document
 * will be ignored by parser setting external schema location. If the
 * name space is not in the table, it will be download to metacat and register
 * in table
 */
public class SchemaLocationResolver
{
  
  private String nameSpace = null; //public id
  private String schemaLocation = null; // system id
  private String WHITESPACESTRING =" ";   
  private boolean downloadNewSchema = false;

  private static Log logMetacat = LogFactory.getLog(SchemaLocationResolver.class);

  public SchemaLocationResolver()
  {	  
  }
  
  /**
   * Construct an instance of the SchemaLocationResolver class
   *
   * @param  myNameSpaceAndLocation it is come from xsi:schemaLocation=
   *         "namespace location"
   */
  public SchemaLocationResolver(String myNameSpaceAndLocation)
  {
	  parse(myNameSpaceAndLocation);

  }

  /**
   * Construct an instance of the SchemaLocationResolver class
   *
   * @param  myNameSpaceAndLocation it is come from xsi:schemaLocation=
   *         "namespace location"
   */
  public SchemaLocationResolver(String myNameSpace, String myLocation)
  {
      nameSpace = myNameSpace;
      schemaLocation = myLocation;
  }
  
  /**
   * When got a name space, check if it is in the database, if it is do nothing
   * else upload it to metacat to store it and register it in catalog table
   */
  public void resolveNameSpace ()
  {
  
    // if name space is not in table
    if (nameSpace !=null && schemaLocation != null &&
        !XMLSchemaService.getInstance().getNameSpaceList().contains(nameSpace))
    {
       try
       {
        
        // upload schema into metacat
        String newURLInMetacat = uploadSchemaFromURL(schemaLocation);
        ServiceService.refreshService("XMLSchemaService");
       
        // check the name space list again.  It may not have appeared the first time 
        // because the schema was in the db but there was no file on disk.  If that's 
        // the case, it will show up in the name space list now.  If it doesn't we need
        // to register the schema in the database.
        if (!XMLSchemaService.getInstance().getNameSpaceList().contains(nameSpace)) {
        	registerSchema(newURLInMetacat);
        	ServiceService.refreshService("XMLSchemaService");
        }
        downloadNewSchema = true;
        //handle included schema in above schema
        String externalBaseURL = XMLSchemaService.getBaseUrlFromSchemaURL(schemaLocation);
        //System.out.println("==========the externalBaseURL is "+externalBaseURL);
        handleIncludedSchema(newURLInMetacat, externalBaseURL);
       }
       catch(Exception e)
       {
         logMetacat.error("Error in SchemaLocation.resolveNameSpace" +
                                  e.getMessage());
       }
 
    }//if
 
  }
  
  /*
   * Some schemas (parent schema)also include other schemas (children) in 
   * its definition. The syntax is:  <xsd:include schemaLocation="product.xsd"/>
   * The included schemas need to be downloaded as well. 
   * The included schemas always have the same target namespace. So there
   * is no need to register the schema into xml_catalog table.
   * Note: the included schemas may have included schemas (grandchildren) as well.
   */
  private void handleIncludedSchema(String parentSchemaNewURLInMetacat, 
                                                                          String externalBaseURL) 
                                                          throws SAXException, IOException,PropertyNotFoundException 
  {
    //Gets the included schema information from parent schema
    //System.out.println("the parent schema new url in metacat ============="+parentSchemaNewURLInMetacat);
    if(externalBaseURL != null)
    {
      InputStream in = DBEntityResolver.checkURLConnection(SystemUtil.getInternalContextURL()+parentSchemaNewURLInMetacat);
      XMLSchemaParser parser = new XMLSchemaParser(in);
      parser.parse();
      Vector<String> includedSchemaPaths = parser.getIncludedSchemaFilePathes();
      //System.out.println("the include schema paths is ============ "+includedSchemaPaths);
      if(includedSchemaPaths!=  null)
      {
        for(int i=0; i<includedSchemaPaths.size(); i++)
        {
          String schemaFilePath = includedSchemaPaths.elementAt(i);
          //System.out.println("start to handle the included path ========="+schemaFilePath);
          try
          {
            String newShemaURLInMetacat = 
                           uploadSchemaFromURL(externalBaseURL+schemaFilePath);
            //System.out.println("success download the included schema and new url is  ========="+newShemaURLInMetacat);
            //recursively download the included schema
            handleIncludedSchema(newShemaURLInMetacat, externalBaseURL);
          }
          catch(Exception e)
          {
            logMetacat.warn("Warning on SchemaLocationResolver.handleIncludedSchema"+e.getMessage());
          }
          
        }
      }
    }
    
  }

  /**
   * Upload new Schema located at outside URL to Metacat file system
   */
  private String uploadSchemaFromURL(String schemaLocationURL) throws Exception
  {
   
	  String relativeSchemaPath = XMLSchemaService.SCHEMA_DIR;
    String fullSchemaPath = SystemUtil.getContextDir() + relativeSchemaPath;
    String schemaURL = SystemUtil.getContextURL()  + relativeSchemaPath;

    // get filename from systemId
    String filename = XMLSchemaService.getSchemaFileNameFromUri(schemaLocationURL);
  
    if (filename != null && !(filename.trim()).equals(""))
    {
      int slash = Math.max(filename.lastIndexOf('/'), filename.lastIndexOf('\\'));
      if ( slash > -1 ) 
      {
        filename = filename.substring(slash + 1);
      }
    }
    else
    {
      return null;
    }
    // writing schema text on Metacat file system as filename
    try {
      InputStream istream = DBEntityResolver.checkURLConnection(schemaLocationURL);
      // create a buffering character-input stream
      // that uses a default-sized input buffer
      BufferedInputStream in = new BufferedInputStream(istream);

      // open file writer to write the input into it
      File f = new File(fullSchemaPath, filename);
      synchronized (f) 
      {
        try 
        {
          if ( f.exists() ) 
          {
           throw new IOException("File already exist: " + f.getCanonicalFile());
          }
        } 
        catch (SecurityException se) 
        {
         // if a security manager exists,
          throw se;
       }
      // create a buffered character-output stream
      // that uses a default-sized output buffer
      FileWriter fw = new FileWriter(f);
      BufferedWriter out = new BufferedWriter(fw);

      // read the input and write into the file writer
      int inputByte;
      while ( (inputByte = in.read()) != -1 ) 
      {
        out.write(inputByte);
        //out.newLine(); //instead of out.write('\r\n');
      }

      // the input and the output streams must be closed
      in.close();
      out.flush();
      out.close();
      fw.close();
     } // end of synchronized
    } 
    catch (Exception e) 
    {
      throw new Exception
      ("shemaLocationResolver.uploadSchemaFromURL(): " + e.getMessage());
    } 
    logMetacat.warn("new schema location is: " + schemaURL + 
                              filename);
    return  relativeSchemaPath + filename;
  }

  
  /*
   * Register new schema identified by @systemId in Metacat XML Catalog
   */
  private void registerSchema(String systemId )
  {
    // check systemid is not null
    if (systemId == null || nameSpace == null || (nameSpace.trim()).equals(""))
    {    
      return;
    }
    
    DBConnection conn = null;
    int serialNumber = -1;
    PreparedStatement pstmt = null;
    String sql = "INSERT INTO xml_catalog " +
             "(entry_type, public_id, system_id) " +
             "VALUES ('" + DocumentImpl.SCHEMA + "', ?, ?)";
    
 
    try 
    {
      //check out DBConnection
      conn=DBConnectionPool.getDBConnection("schemaLocation.registerSchema");
      serialNumber=conn.getCheckOutSerialNumber();
      pstmt = conn.prepareStatement(sql);
     
      // Bind the values to the query
      pstmt.setString(1, nameSpace);
      pstmt.setString(2, systemId);
      // Do the insertion
      pstmt.execute();
      pstmt.close();
    } 
    catch (SQLException e) 
    {
      logMetacat.error("SchemaLocation.egisterSchema(): " + e.getMessage());
    }
    finally
    {
      try
      {
        pstmt.close();
      }//try
      catch (SQLException sqlE)
      {
        logMetacat.error("Error in SchemaLocation.egisterSchema(): "
                                    +sqlE.getMessage());
      }//catch
      DBConnectionPool.returnDBConnection(conn, serialNumber);

    }//finally
    XMLSchemaService.getInstance().populateRegisteredSchemaList();
  }
  
  /*
   * A method to parse the value for xis:schemaLocation="namespace location"
   * and store the namespace in nameSpace and location in schemaLocaion
   */
  private void parse(String nameSpaceAndLocation)
  {
    // check the parameter
    if (nameSpaceAndLocation == null||(nameSpaceAndLocation.trim()).equals(""))
    {
      return;
    }
    //Get white space index (delimiter)
    int indexOfWhiteSpace = nameSpaceAndLocation.lastIndexOf(WHITESPACESTRING);
    if (indexOfWhiteSpace != -1)
    {
      // before white space is namespace
      nameSpace = nameSpaceAndLocation.substring(0,indexOfWhiteSpace);
      // after white sapce is schema location
      schemaLocation = nameSpaceAndLocation.substring(indexOfWhiteSpace+1);
      // Gebug message
      logMetacat.info("Namespace after parsing: " + nameSpace);
      logMetacat.info("Location after parsing: " + schemaLocation);
    } 
  }
  
  /*public static void main(String[] argus)
  {
     try
     {
       DBConnectionPool pool = DBConnectionPool.getInstance();
       // Print out a empty schema list
       SchemaLocationResolver schema = new SchemaLocationResolver();
       logMetacat.warn("Namespace and Location String: "+ 
                                XMLSchemaService.getInstance().getNameSpaceAndLocationString());
       // input a schemalocation
       SchemaLocationResolver schema2 = new SchemaLocationResolver(
                                        "eml://ecoinformatics.org/eml-2.0.0 " +
                                "http://dev.nceas.ucsb.edu/tao/schema/eml.xsd");
       schema2.resolveNameSpace();
       // input a wrong name space location
       SchemaLocationResolver schema3 = new SchemaLocationResolver(
                                        "http://www.xm.org/schema/stmml " +
                              "http://dev.nceas.ucsb.edu/tao/schema/stmml.xsd");
       schema3.resolveNameSpace();
       // print out new schema list in db
       SchemaLocationResolver schema4 = new SchemaLocationResolver();
       logMetacat.warn("Namespace and Location String: "+ 
    		   XMLSchemaService.getInstance().getNameSpaceAndLocationString());
     }
     catch(Exception e)
     {
       logMetacat.error("erorr in Schemalocation.main: " + 
                                e.getMessage());
     }
  }*/
  
  /**
   * Gets the downloadNewSchema's value.
   * @return
   */
 public boolean getDownloadNewSchema()
 {
	 return this.downloadNewSchema;
 }
}
