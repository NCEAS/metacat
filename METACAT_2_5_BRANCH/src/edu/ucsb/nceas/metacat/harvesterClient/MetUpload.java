/**
 *  '$RCSfile$'
 *  Copyright: 2004 University of New Mexico and the 
 *                  Regents of the University of California
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
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package edu.ucsb.nceas.metacat.harvesterClient;

import java.io.*;
import java.util.*;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpUtils;
import javax.servlet.ServletOutputStream;
import com.oreilly.servlet.multipart.FilePart;
import com.oreilly.servlet.multipart.MultipartParser;
import com.oreilly.servlet.multipart.ParamPart;
import com.oreilly.servlet.multipart.Part;

import edu.ucsb.nceas.metacat.client.*;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.metacat.shared.ServiceException;
import edu.ucsb.nceas.metacat.util.SystemUtil;
import edu.ucsb.nceas.utilities.GeneralPropertyException;
import edu.ucsb.nceas.utilities.IOUtil;

/**
 *  MetUpload implements a Harvester servlet to insert, update, or delete
 *  a single file to Metacat.
 */
public class MetUpload extends HttpServlet {

  /*
   * Class fields
   */
  private static final String CONFIG_DIR = "WEB-INF";
  private static final String CONFIG_NAME = "metacat.properties";
   
  /* Object fields */
  private ServletConfig config = null;
  private ServletContext context = null;
  private String metacatURL;

  /**
   *  Handle POST requests made to the Harvester MetUpload servlet.
   *
   *  @param  req   The request
   *  @param  res   The response
   *  @throws IOException
   */
  public void doPost(HttpServletRequest req, HttpServletResponse res) 
          throws IOException {
    Part            aPart;
    boolean         delete = false;
    String          docid = "";
    String          fieldName;
    String          fieldValue;
    String          fileName = "";
    FilePart        fPart = null;
    PrintWriter     out = res.getWriter();
    MultipartParser parser = new MultipartParser(req, 1024 * 1024);
    ParamPart       pPart;
    HttpSession     sess = req.getSession(true);
    String          password = (String) sess.getAttribute("password");
    StringReader    sr = null;
    boolean         upload = false;
    String          username = (String) sess.getAttribute("username");
    
    res.setContentType("text/plain");

    try {
      while ((aPart = parser.readNextPart()) != null) {

        if (aPart.isParam() == true) {
          pPart = (ParamPart) aPart;
          fieldName = pPart.getName();

          if (fieldName != null) {
            if (fieldName.equals("docid")) {
              docid = pPart.getStringValue();
            }
            else if (fieldName.equals("Upload")) {
              upload = true;
            }
            else if (fieldName.equals("Delete")) {
              delete = true;
            }            
          }
        }
        else if (aPart.isFile() == true) {
          fPart = (FilePart) aPart;
          fieldName = fPart.getName();
          fileName = fPart.getFileName();
          sr = writeTempFile(fPart, out, docid, fileName);
        }

      }
    }
    catch (Exception e) {
      System.out.println("Error parsing parameters: " + e.getMessage());
    }

    if (upload) {
      upload(out, docid, sr, username, password);
    }
    if (delete) {
      delete(out, docid, username, password);
    }
  }


  /**
   * Deletes a file from Metacat based on its docid.
   * 
   * @param out        the PrintWriter output stream
   * @param docid      the Metacat document id, e.g. document.1.1
   * @param username   the Metacat username
   * @param password   the Metacat password
   */
  private void delete(PrintWriter out, 
                      String docid,
                      String username,
                      String password
                     ) {
    Metacat  metacat;
    String   metacatResponse;

    if (docid.equals("")) {
      out.println("Error deleting document: No DocID specified");
      return;
    }

    try {    
      metacat = MetacatFactory.createMetacatConnection(metacatURL);
      metacat.login(username, password);
      metacatResponse = metacat.delete(docid);
      out.println(metacatResponse);
    }
    catch (MetacatAuthException e) {
      out.println("Metacat delete failed: MetacatAuthException:" + 
                  e.getMessage());
    }
    catch (MetacatInaccessibleException e) {
      out.println("Metacat delete failed:  MetacatInaccessibleException:" + 
                  e.getMessage());
    }
    catch (InsufficientKarmaException e) {
      out.println("Metacat delete failed: InsufficientKarmaException:" + 
                   e.getMessage());
    }
    catch (MetacatException e) {
      out.println("Metacat delete failed: MetacatException:" + 
                  e.getMessage());
    }

  }

  /**
	 * Initializes the servlet. Reads properties and initializes object fields.
	 * 
	 * @throws ServletException
	 */
	public void init(ServletConfig config) throws ServletException {
		String dirPath;

		super.init(config);
		this.config = config;
		this.context = config.getServletContext();
		dirPath = context.getRealPath(CONFIG_DIR);

		try {
			PropertyService.getInstance();
			metacatURL = SystemUtil.getServletURL();
		} catch (ServiceException se) {
			System.out.println("Service problem while initializing MetUpload: " + se.getMessage());
		} catch (GeneralPropertyException gpe) {
			System.out.println("Error initializing properties utility: " + gpe.getMessage());
		}

		System.out.println("metacatURL: " + metacatURL);
	}


  /**
	 * Uploads a file to Metacat for insertion or updating. This implementation
	 * is limited by the fact that all ".1" documents are inserted while all
	 * other revisions are updated. This logic can be improved after the Metacat
	 * interface is extended with additional methods to query information about
	 * revisions, for example, hasDocument() and highestRevision().
	 * 
	 * @param out
	 *            the PrintWriter output stream
	 * @param docid
	 *            the docid, e.g. "document.1.1"
	 * @param sr
	 *            the StringReader containing the contents of the document
	 * @param username
	 *            the Metacat username
	 * @param password
	 *            the Metacat password
	 */
  private void upload(PrintWriter out, 
                      String docid,
                      StringReader sr,
                      String username,
                      String password
                     ) {
    FileReader      fr;
    Metacat         metacat;
    String          metacatResponse;
    String          revision;
    int             strLen;

    if (docid.equals("")) {
      out.println("Error uploading: No docid specified");
      return;
    }
    
    if (sr == null) {
      out.println("Error uploading: No EML file specified");
      return;
    }

    try {
      metacat = MetacatFactory.createMetacatConnection(metacatURL);
      metacat.login(username, password);
      strLen = docid.length();
      revision = docid.substring((strLen - 2), strLen);
                
      if (revision.equals(".1")) {
        metacatResponse = metacat.insert(docid, sr, null);
      }
      else {
        metacatResponse = metacat.update(docid, sr, null);
      }

      out.println(metacatResponse);
    }
    catch (MetacatAuthException e) {
      out.println("Metacat upload failed: MetacatAuthException:" + 
                  e.getMessage());
    }
    catch (MetacatInaccessibleException e) {
      out.println("Metacat upload failed:  MetacatInaccessibleException:" + 
                  e.getMessage());
    }
    catch (InsufficientKarmaException e) {
      out.println("Metacat upload failed: InsufficientKarmaException:" + 
                   e.getMessage());
    }
    catch (MetacatException e) {
      out.println("Metacat upload failed: MetacatException:" + 
                  e.getMessage());
    }
    catch (IOException e) {
      out.println("Metacat upload failed: IOException:" + 
                  e.getMessage());
    }
            
  }
  

  /**
   * Writes the uploaded file to disk and then reads it into a StringReader for
   * subsequent upload to Metacat.
   * 
   * @param fPart     the FilePart object containing the file form parameter
   * @param out       the PrintWriter output stream
   * @param docid     the document id, e.g. "document.1.1"
   * @param fileName  the name of the file to be written to disk
   * @return sr       a StringReader containing the contents of the file
   */
  private StringReader writeTempFile(FilePart fPart,
                                     PrintWriter out, 
                                     String docid,
                                     String fileName
                                    ) {
    FileReader      fr;
    String          metacatResponse;
    StringReader    sr = null;
    File            tmpDir;
    String          tmpDirPath;
    File            tmpFile;
    String          tmpFilePath = "";
    String          xmlString = "";

    if ((fileName == null) || fileName.equals("")) {
      return sr;
    }
    
    tmpDirPath = System.getProperties().getProperty("java.io.tmpdir");
    tmpDir = new File(tmpDirPath);
    
    // Create the temporary directory if it doesn't exist
    try {
      if (!tmpDir.exists()) {
        tmpDir.mkdirs();
      }
    }
    catch (SecurityException e) {
      out.println("Can't create directory: " + tmpDir.getPath());
      out.println(e.getMessage());
    }

    // Write the image to a file
    try {
      tmpFile = new File(tmpDirPath, fileName);
      fPart.writeTo(tmpFile);
      tmpFilePath = tmpDirPath + File.separator + fileName;
      fr = new FileReader(tmpFilePath);
      xmlString = IOUtil.getAsString(fr, true);
      sr = new StringReader(xmlString);
      tmpFile.delete();           // Clean up the temporary file from disk
    }
    catch (IOException e) {
      out.println("IOException: " + tmpFilePath + e.getMessage());
    }

    return sr;
  }
  
  
}
