/**
 *  '$RCSfile$'
 *    Purpose: A Class that implements session utility methods 
 *  Copyright: 2008 Regents of the University of California and the
 *             National Center for Ecological Analysis and Synthesis
 *    Authors: Michael Daigle
 * 
 *   '$Author: daigle $'
 *     '$Date: 2009-03-25 13:41:15 -0800 (Wed, 25 Mar 2009) $'
 * '$Revision: 4861 $'
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

package edu.ucsb.nceas.metacat;

import java.sql.SQLException;
import java.util.Hashtable;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.metacat.shared.HandlerException;
import edu.ucsb.nceas.metacat.shared.MetacatUtilException;
import edu.ucsb.nceas.metacat.util.ErrorSendingErrorException;
import edu.ucsb.nceas.metacat.util.ResponseUtil;
import edu.ucsb.nceas.utilities.FileUtil;
import edu.ucsb.nceas.utilities.LSIDUtil;
import edu.ucsb.nceas.utilities.ParseLSIDException;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;
import edu.ucsb.nceas.utilities.UtilException;

public class ArchiveHandler  {
	
	private static ArchiveHandler archiveHandler = null;
	
	private static Logger logMetacat = Logger.getLogger(ArchiveHandler.class);

	/**
	 * private constructor since this is a singleton
	 */
	private ArchiveHandler() throws HandlerException {}
	
	/**
	 * Get the single instance of SessionService.
	 * 
	 * @return the single instance of SessionService
	 */
	public static ArchiveHandler getInstance() throws HandlerException {
		if (archiveHandler == null) {
			archiveHandler = new ArchiveHandler();
		}
		return archiveHandler;
	}
	
	public void readArchiveEntry(Hashtable<String, String[]> params, HttpServletRequest request,
            HttpServletResponse response, String user, 
            String passWord, String[] groups) throws HandlerException, ErrorSendingErrorException, ErrorHandledException {
        	
		String[] docs = new String[0];
        String docid = "";
        String qformat = "";
        String[] archiveEntryNames = new String[0];
        String archiveEntryName = "";
        
        try {          
            // read the docid from params
            if (params.containsKey("docid")) {
                docs = params.get("docid");
                docid = docs[0];
                if (docid.startsWith("urn:")) {
                	docid = LSIDUtil.getDocId(docid, true);               	
                }
            } else { 
            	throw new HandlerException("ArchiveHandler.readArchiveEntry - Could not find doc " 
            			+ "id in params when reading archive entry");
            } 
            
            // read the archive file entry name from params
            if (params.containsKey("archiveEntryName")) {
            	archiveEntryNames = params.get("archiveEntryName");
            	archiveEntryName = archiveEntryNames[0];
            } else { 
            	throw new HandlerException("ArchiveHandler.readArchiveEntry - Could not find " 
            			+ "archiveEntryName in params when reading archive entry");
            } 
            
            // qformat is used to hold the type of archive and the format of the content that we wish to 
            // extract from the archive.  The default is jar-file for streaming a file from a jar.  If 
            // we wanted to do some special processing for specific archive or content types, we would use 
            // a different qformat (gzip-pdf for instance).
            if (params.containsKey("qformat")) {
                qformat = params.get("qformat")[0];
            } else { 
            	qformat = "jar-file";
            }       
            
//            //check the permission for read
//            try {
//            	if (!DocumentImpl.hasReadPermission(user, groups, docid)) {
//            		String errorString = "User " + user + " does not have permission"
//    					+ " to read the document with the docid " + docid;
//            		ResponseUtil.sendErrorXML(response, ResponseUtil.NO_READ_PERMISSION, errorString);
//            		return;
//            	}
//            } catch (McdbException mcdbe) {
//            	throw new HandlerException("ArchiveHandler.readArchiveEntry - Error getting " 
//            			+ "permissions for docid: " + docid + " for user: " + user 
//            			+ " : " + mcdbe.getMessage());
//            } catch (SQLException sqle) {
//            	throw new HandlerException("ArchiveHandler.readArchiveEntry - SQL error getting " 
//            			+ "permissions for docid: " + docid + " for user: " + user 
//            			+ " : " + sqle.getMessage());
//            } 
            
            // Get the path to the archive file
            String archiveFileBasePath = PropertyService.getProperty("application.datafilepath");
            String archiveFilePath = archiveFileBasePath + FileUtil.getFS() + docid;
            
            // Get the paths to the expanded archive directory and to the content file within
            // that directory
            String expandedArchiveBasePath = PropertyService.getProperty("application.expandedArchivePath");
            String expandedArchivePath = expandedArchiveBasePath + FileUtil.getFS() + docid;
            String entryFilePath = expandedArchivePath + FileUtil.getFS() + archiveEntryName;
            
            // If the expanded archive directory does not exist, create it.
            if (!FileUtil.isDirectory(expandedArchivePath)) {
            	FileUtil.createDirectory(expandedArchivePath);
            }
            
            // If the entry we want does not exist, make sure it is actually in the archive
            // file.  If so, go ahead and expand the archive into the expanded archive directory.
            if (FileUtil.getFileStatus(entryFilePath) == FileUtil.DOES_NOT_EXIST) {
            	if (FileUtil.getFileStatus(archiveFilePath) < FileUtil.EXISTS_READABLE) {
            		throw new HandlerException("Could not find archive: " + archiveFilePath 
            				+ " in order to get content file: " + archiveEntryName);
            	}
            	
            	// extract the jar file to the expanded archive directory
            	FileUtil.extractJarFile(archiveFilePath, expandedArchivePath);
            }
            
            ResponseUtil.writeFileToOutput(response, expandedArchivePath, archiveEntryName);            
                        
        } catch (UtilException ue) {
        	String errorString = "ArchiveHandler.readArchiveEntry - Utility error reading archive entry for docid: " 
    			+ docid + " : " + ue.getMessage();
        	ResponseUtil.sendErrorXML(response, ResponseUtil.GENERAL_UTILITY_ERROR, errorString);
        	throw new ErrorHandledException(null);
        } catch (PropertyNotFoundException pnfe) {
        	String errorString = "ArchiveHandler.readArchiveEntry -  Property error reading archive entry for docid: " 
        		+ docid + " : " + pnfe.getMessage();
        	ResponseUtil.sendErrorXML(response, ResponseUtil.PROPERTY_NOT_FOUND, errorString);
        	throw new ErrorHandledException(null);
        } catch (MetacatUtilException mue) {
        	String errorString = "ArchiveHandler.readArchiveEntry -  Metacat utility error reading archive entry for docid: " 
        		+ docid + " : " + mue.getMessage();
        	ResponseUtil.sendErrorXML(response, ResponseUtil.METACAT_UTILITY_ERROR, errorString);
        	throw new ErrorHandledException(null);
        } catch (ParseLSIDException ple) {
        	String errorString = "ArchiveHandler.readArchiveEntry -  LSID parsing error reading archive entry for docid: " 
        		+ docid + " : " + ple.getMessage();
        	ResponseUtil.sendErrorXML(response, ResponseUtil.METACAT_UTILITY_ERROR, errorString);
        	throw new ErrorHandledException(null);
        }
	}

}
