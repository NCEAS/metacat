/**
 *  '$RCSfile$'
 *    Purpose: A Class that implements utility methods for a metadata catalog
 *  Copyright: 2000 Regents of the University of California and the
 *             National Center for Ecological Analysis and Synthesis
 *    Authors: Matt Jones, Jivka Bojilova
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

package edu.ucsb.nceas.metacat.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.util.Hashtable;
import java.util.Vector;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.ucsb.nceas.dbadapter.AbstractDatabase;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;
import edu.ucsb.nceas.utilities.FileUtil;

/**
 * A suite of utility classes for the metadata catalog server
 */
public class MetacatUtil {

    public static final String XMLFORMAT = "xml";
    public static AbstractDatabase dbAdapter;

    private static boolean debugErrorDisplayed = false;

    private static Log logMetacat = LogFactory.getLog(MetacatUtil.class);

    /**
     * Utility method to parse the query part of a URL into parameters. This
     * method assumes the format of the query part of the url is an ampersand
     * separated list of name/value pairs, with equal signs separating the name
     * from the value (e.g., name=tom&zip=99801 ). Returns a has of the name
     * value pairs, hashed on name.
     */
    public static Hashtable<String,String> parseQuery(String query)
            throws MalformedURLException {
        String[][] params = new String[200][2];
        Hashtable<String,String> parameters = new Hashtable<String,String>();

        String temp = "";
        boolean ampflag = true;
        boolean poundflag = false;
        int arrcount = 0;

        if (query != null) {
            for (int i = 0; i < query.length(); i++) {

                // go throught the remainder of the query one character at a time.
                if (query.charAt(i) == '=') {
                    // if the current char is a # then the preceding should be
                    // a name
                    if (!poundflag && ampflag) {
                        params[arrcount][0] = temp.trim();
                        temp = "";
                    } else {
                        //if there are two #s or &s in a row throw an
                        // exception.
                        throw new MalformedURLException(
                                "metacatURL: Two parameter names "
                                        + "not allowed in sequence");
                    }
                    poundflag = true;
                    ampflag = false;
                } else if (query.charAt(i) == '&' || i == query.length() - 1) {
                    //the text preceding the & should be the param value.
                    if (i == query.length() - 1) {
                        //if at the end of the string grab the last value and
                        // append it.
                        if (query.charAt(i) != '=') {
                            //ignore an extra & on the end of the string
                            temp += query.charAt(i);
                        }
                    }

                    if (!ampflag && poundflag) {
                        params[arrcount][1] = temp.trim();
                        parameters
                                .put(params[arrcount][0], params[arrcount][1]);
                        temp = "";
                        arrcount++; //increment the array to the next row.
                    } else {
                        //if there are two =s or &s in a row through an
                        // exception
                        throw new MalformedURLException(
                                "metacatURL: Two parameter values "
                                        + "not allowed in sequence");
                    }
                    poundflag = false;
                    ampflag = true;
                } else {
                    //get the next character in the string
                    temp += query.charAt(i);
                }
            }
        }
        return parameters;
    }

    /**
     * Transform a comma-delimited string of options to a vector object which contains those options
     * @param optiontext  the string contains the options
     * @return a vector object which contains those options
     */
    public static Vector<String> getOptionList(String optionText) {
        Vector<String> optionsVector = new Vector<String>();
        if (optionText.indexOf(",") == -1) {
            optionsVector.addElement(optionText);
            return optionsVector;
        }

        while (optionText.indexOf(",") != -1) {
            String s = optionText.substring(0, optionText.indexOf(","));
            optionsVector.addElement(s.trim());
            optionText = optionText.substring(optionText.indexOf(",") + 1,
                    optionText.length());
            if (optionText.indexOf(",") == -1) { //catch the last list entry
                optionsVector.addElement(optionText.trim());
            }
        }
        return optionsVector;
    }

    /** Normalizes a string read from DB. So it will be compatible to HTML */
    public static String normalize(String s) {
        StringBuffer str = new StringBuffer();

             int len = (s != null) ? s.length() : 0;
             for (int i = 0; i < len; i++) {
                 char ch = s.charAt(i);
                 switch (ch) {
                     case '<': {
                         str.append("&lt;");
                         break;
                     }
                     case '>': {
                         str.append("&gt;");
                         break;
                     }
                     case '&': {
                         /*
                          * patch provided by Johnoel Ancheta from U of Hawaii
                          */
                         // check if & is for a character reference &#xnnnn;
                         if (i + 1 < len - 1 && s.charAt(i + 1) == '#') {
                             str.append("&#");
                             i += 2;

                             ch = s.charAt(i);
                             while (i < len && ch != ';') {
                                 str.append(ch);
                                 i++;
                                 ch = s.charAt(i);
                             }
                             str.append(';');
                         } else if (i + 4 < len && s.charAt(i + 1) == 'a'
                            && s.charAt(i + 2) == 'm'
                               && s.charAt(i + 3) == 'p'
                                  && s.charAt(i + 4) == ';') {
                             // check if & is in front of amp;
                             // (we dont yet check for other HTML 4.0 Character entities)
                             str.append("&amp;");
                             i += 4;
                         } else if (i + 3 < len && s.charAt(i + 1) == 'l'
                            && s.charAt(i + 2) == 't'
                               && s.charAt(i + 3) == ';' ) {
                             // check if & is in front of it;
                             str.append("&lt;");
                             i += 3;
                         }  else if (i + 3 < len && s.charAt(i + 1) == 'g'
                            && s.charAt(i + 2) == 't'
                               && s.charAt(i + 3) == ';' ) {
                             // check if & is in front of gt;
                             // (we dont yet check for other HTML 4.0 Character entities)
                             str.append("&gt;");
                             i += 3;
                         } else if (i + 5 < len && s.charAt(i + 1) == 'q'
                            && s.charAt(i + 2) == 'u'
                               && s.charAt(i + 3) == 'o'
                            && s.charAt(i + 4) == 't'
                            && s.charAt(i + 5) == ';') {
                             // check if & is in front of quot;
                             // (we dont yet check for other HTML 4.0 Character entities)
                             str.append("&quot;");
                             i += 5;
                         } else if (i + 5 < len && s.charAt(i + 1) == 'a'
                            && s.charAt(i + 2) == 'p'
                               && s.charAt(i + 3) == 'o'
                            && s.charAt(i + 4) == 's'
                            && s.charAt(i + 5) == ';') {
                             // check if & is in front of apostrophe;
                             // (we dont yet check for other HTML 4.0 Character entities)
                             str.append("&apos;");
                             i += 5;
                         } 
                         else {
                             str.append("&amp;");
                         }
                         break;
                     }
                     case '"':
                        str.append("&quot;");
                         break;
                     case '\'':
                        str.append("&apos;");
                         break;
                     default: {
                         if ( (ch<128) && (ch>31) ) {
                             str.append(ch);
                         } else if (ch<32) {
                             if (ch == 10) { // new line
                                 str.append(ch);
                             }
                             if (ch == 13) { // carriage return
                                 str.append(ch);
                             }
                             if (ch == 9) {  // tab
                                 str.append(ch);
                             }
                             // otherwise skip
                         } else {
                            //Don't transfer special character to numeric entity
                             str.append(ch);
                         }
                     }
                 }
             }
             return str.toString();
    }

    /** A method to replace whitespace in url */
    public static String replaceWhiteSpaceForURL(String urlHasWhiteSpace) {
        StringBuffer newUrl = new StringBuffer();
        String whiteSpaceReplace = "%20";
        if (urlHasWhiteSpace == null || urlHasWhiteSpace.trim().equals("")) { return null; }

        for (int i = 0; i < urlHasWhiteSpace.length(); i++) {
            char ch = urlHasWhiteSpace.charAt(i);
            if (!Character.isWhitespace(ch)) {
                newUrl.append(ch);
            } else {
                //it is white sapce, replace it by %20
                newUrl = newUrl.append(whiteSpaceReplace);
            }

        }//for
        logMetacat.info("The new string without space is:"
                + newUrl.toString());
        return newUrl.toString();

    }// replaceWhiteSpaceForUR

    /**
     * Writes debug information into a file. In metacat.properties, if property
     * application.writeDebugToFile is set to true, the debug information will be written to
     * debug file, which value is the property application.debugOutputFile in
     * metacat.properties.
     *
     */
    public static void writeDebugToFile(String debugInfo) {
        String debug = "false";
        try {
            debug = PropertyService.getProperty("application.writeDebugToFile");
            if (debug != null && debug.equalsIgnoreCase("true")) {
                File outputFile =
                        new File(PropertyService.getProperty("application.debugOutputFile"));
                FileOutputStream fos = new FileOutputStream(outputFile, true);
                PrintWriter pw = new PrintWriter(fos);
                pw.println(debugInfo);
                pw.flush();
                pw.close();
                fos.close();
            }
        } catch (PropertyNotFoundException pnfe) {
            // only log debug to file warning once
            if (!debugErrorDisplayed) {
                logMetacat.warn("Could not get debug property.  Write debug to "
                  + "file is set to false: " + pnfe.getMessage());
                debugErrorDisplayed = true;
            }
        } catch (Exception io) {
            logMetacat.warn("Error in MetacatUtil.writeDebugToFile "
               + io.getMessage());
        }
    }

    /**
     * Writes debug information into a file in delimitered format
     *
     * @param debugInfo
     *            the debug information
     * @param newLine
     *            append the debug info to a line or not
     */
    public static void writeDebugToDelimiteredFile(String debugInfo, boolean newLine) {
        String debug = "false";
        try {
            debug = PropertyService.getProperty("application.writeDebugToFile");
            if (debug != null && debug.equalsIgnoreCase("true")) {
                File outputFile = new File(PropertyService
                  .getProperty("application.delimiteredOutputFile"));
                FileOutputStream fos = new FileOutputStream(outputFile, true);
                PrintWriter pw = new PrintWriter(fos);
                if (newLine) {
                    pw.println(debugInfo);
                } else {
                    pw.print(debugInfo);
                }
                pw.flush();
                pw.close();
                fos.close();
            }
        } catch (PropertyNotFoundException pnfe) {
            // only log debug to file warning once
            if (!debugErrorDisplayed) {
                logMetacat.warn("Could not get delimited debug property. Write debug to "
                  + "file is set to false: " + pnfe.getMessage());
                debugErrorDisplayed = true;
            }
        } catch (Exception io) {
            logMetacat.warn("Eorr in writeDebugToDelimiteredFile "
               + io.getMessage());
        }
    }

    /**
     * Write the uploaded file to disk for temporary storage before moving it to
     * its final Metacat location.
     *
     * @param filePart
     *            the FilePart object containing the file form element
     * @param fileName
     *            the name of the file to be written to disk
     * @return tempFilePath a String containing location of temporary file
     */
    public static File writeTempUploadFile (FileItem fi, String fileName) throws Exception {
        File tempFile = null;
        String tempDirPath = null;
        try {
           tempDirPath = PropertyService.getProperty("application.tempDir") + FileUtil.getFS()
                               + "uploads";
        } catch (PropertyNotFoundException pnfe) {
           logMetacat.warn("Temp property not found.  An attempt will be made "
                 + "to use system temp directory: " + pnfe.getMessage());
        }
        long fileSize;
        File tempDir;

        if ((fileName == null) || fileName.equals("")) {
            return tempFile;
        }

        // the tempfilepath token isn't set, use Java default
        if (tempDirPath == null) {
            String javaTempDir = System.getProperty("java.io.tempdir");
            if (javaTempDir == null) {
                // no paths set, use unix default
                tempDirPath = "/tmp";
            } else {
                tempDirPath = javaTempDir;
            }
        }

        tempDir = new File(tempDirPath);

        // Create the temporary directory if it doesn't exist
        try {
            if (!tempDir.exists()) {
                tempDir.mkdirs();
            }
        } catch (SecurityException e) {
            throw new IOException("Can't create directory: " + tempDir.getPath() + ". Error: "
               + e.getMessage());
        }

        tempFile = File.createTempFile("upload", ".tmp", tempDir);
        fi.write(tempFile);
        fileSize = fi.getSize();

        if (fileSize == 0) {
            logMetacat.warn("Uploaded file '" + fileName + "'is empty!");
        }

        logMetacat.debug("Temporary file is: " + tempFile.getAbsolutePath());

        return tempFile;
    }

    /**
    *
    * Copy a file between two locations specified as strings. Fails if either
    * path cannot be created. Based on the public domain FileCopy class in
    * _Java in a Nutshell_.
    *
    * @param sourceName
    *            the source file to read from disk
    * @param destName
    *            the destination file on disk
    */
    public static void copyFile(String sourceName, String destName) throws IOException {

        File sourceFile = new File(sourceName);
        File destFile = new File(destName);
        FileInputStream source = null;
        FileOutputStream dest = null;
        byte[] buffer;
        int bytesRead;

        try {
            if (!sourceFile.exists() || !sourceFile.isFile()) {
                logMetacat.error("File copy: no such source" +
                                 " file: " + sourceName);
            }
            if (!sourceFile.canRead()) {
                logMetacat.error("File copy: source file " +
                                 "is unreadable: " + sourceName);
            }

            if (destFile.exists()) {
                if (destFile.isFile()) {
                    if (!destFile.canWrite()) {
                        logMetacat.error("File copy: destination " +
                                         "file is unwriteable: " + destFile);
                    }
                } else {
                    logMetacat.error("File copy: destination file " +
                                     "is not a file: " + destFile);
                }
            } else {
                File parentDir = parent(destFile);

                if (!parentDir.exists())
                {
                    logMetacat.error("File copy: destination diretory " +
                                     " doesn't exist: " + destName);
                }
                if (!parentDir.canWrite()) {
                    logMetacat.error("File copy: destination directory " +
                                     " is unwritable: " + destName);
                }
            }

            // Verbose error checking done, copy the file object
            source = new FileInputStream(sourceFile);
            dest = new FileOutputStream(destFile);
            buffer = new byte[1024];

            while (true) {
                bytesRead = source.read(buffer);
                if (bytesRead == -1) {
                    break;
                }
                dest.write(buffer, 0, bytesRead);
            }
        }
        finally {
            if (source != null) {
                try { source.close(); } catch (IOException e) { ; }
            }
            if (dest != null) {
                try { dest.close(); } catch (IOException e) { ; }
            }
        }
    }

    /**
     * Get the parent file of the given file
     * @param f  the file which will be checked
     * @return the parent file of the given file
     */
    private static File parent(File f) {
        String dirname = f.getParent();
        if (dirname == null) {
            if (f.isAbsolute()) return new File(File.separator);
            else return new File(System.getProperty("user.dir"));
        }
        return new File(dirname);
    }
}
