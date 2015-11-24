/**
 *  '$RCSfile$'
 *    Purpose: A Class that implements administrative methods 
 *  Copyright: 2008 Regents of the University of California and the
 *             National Center for Ecological Analysis and Synthesis
 *    Authors: Michael Daigle
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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Hashtable;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import edu.ucsb.nceas.metacat.shared.BaseException;
import edu.ucsb.nceas.metacat.shared.MetacatUtilException;
import edu.ucsb.nceas.utilities.FileUtil;

public class ResponseUtil {
	
	// 101XXX - general errors
	public static Long GENERAL_UTILITY_ERROR = new Long(101001);	
	public static Long METACAT_UTILITY_ERROR = new Long(101002);
	// 104XXX - property errors
	public static Long PROPERTY_NOT_FOUND = new Long(104001);	
	// 105XXX - permission errors
	public static Long NO_READ_PERMISSION = new Long(105001);
	// 130XXX - scheduler errors
	public static Long SCHEDULE_WORKFLOW_ERROR = new Long(130001);
	public static Long UNSCHEDULE_WORKFLOW_ERROR = new Long(130002);
	public static Long RESCHEDULE_WORKFLOW_ERROR = new Long(130003);
	public static Long GET_SCHEDULED_WORKFLOW_ERROR = new Long(130004);
	public static Long DELETE_SCHEDULED_WORKFLOW_ERROR = new Long(130005);
	
	
	// errorCodes is a lookup table for generic messages for each code.  It
	// is better to use the sendError() versions that accept an explicit 
	// error message;
	static Hashtable<Long, String> errorCodes = new Hashtable<Long, String>();
	static {
		errorCodes.put(GENERAL_UTILITY_ERROR, "General utility error");
		errorCodes.put(METACAT_UTILITY_ERROR, "Metacat utility error");
		errorCodes.put(PROPERTY_NOT_FOUND, "Property not found");
		errorCodes.put(NO_READ_PERMISSION, "Read permission denied for user");
		errorCodes.put(SCHEDULE_WORKFLOW_ERROR, "Schedule workflow error");
		errorCodes.put(UNSCHEDULE_WORKFLOW_ERROR, "Unschedule workflow error");
		errorCodes.put(RESCHEDULE_WORKFLOW_ERROR, "Reschedule workflow error");
		errorCodes.put(GET_SCHEDULED_WORKFLOW_ERROR, "Get scheduled workflow error");
		errorCodes.put(DELETE_SCHEDULED_WORKFLOW_ERROR, "Delete scheduled workflow error");
	}
	
	private static Logger logMetacat = Logger.getLogger(ResponseUtil.class);
	
	private static int DEFAULT_BUFFER_SIZE = 4 * 1024; // 4K buffer
	
	/**
	 * private constructor - all methods are static so there is no
     * no need to instantiate.
	 */
	private ResponseUtil() {}
	
	/**
	 * Redirect a response.
	 * 
	 * @param response
	 *            that is to be redirected
	 * @param destination
	 *            the context-relative URL to which the request is forwarded
	 */
	public static void redirectResponse(HttpServletRequest request,
			HttpServletResponse response, String destination) throws MetacatUtilException {
		try {
			logMetacat.debug("Redirecting response to " + request.getContextPath() + destination);
			response.sendRedirect(request.getContextPath() + destination);
		} catch (IOException ioe) {
			throw new MetacatUtilException("I/O error when redirecting response to: " + destination);
		}
	}
	
	public static void writeFileToOutput(HttpServletResponse response, String fileDir, String fileName)
		throws MetacatUtilException {
		
		writeFileToOutput(response, fileDir, fileName, DEFAULT_BUFFER_SIZE);
	}
	
	public static void writeFileToOutput(HttpServletResponse response, String fileDir, String fileName, int bufferSize)
			throws MetacatUtilException {
		String filePath = "";
		InputStream inputStream = null;
		OutputStream outputStream = null;
		try {
			filePath = fileDir + FileUtil.getFS() + fileName;
			
			int lastFileSep = fileName.lastIndexOf(FileUtil.getFS());
			String shortFileName = fileName.substring(lastFileSep + 1, fileName.length());
			response.setHeader("Content-Disposition", "attachment; filename=\"" + shortFileName + "\"");
			
			inputStream = new FileInputStream(filePath);
			outputStream = response.getOutputStream();
			
			byte[] byteBuffer = new byte[bufferSize]; 

			int b = 0;
			while ((b = inputStream.read(byteBuffer)) != -1) {
				outputStream.write(byteBuffer, 0, b);
			}
			outputStream.close();
			inputStream.close();
			
		} catch (FileNotFoundException fnfe) {
			throw new MetacatUtilException("Error finding file: " + filePath 
					+ " when writing to output");
		} catch (IOException ioe) {
			throw new MetacatUtilException("I/O Error when writing: " + filePath 
					+ "  to output");
		} finally {
		    IOUtils.closeQuietly(inputStream);
		    IOUtils.closeQuietly(outputStream);
		}
	}
	
	public static void send(HttpServletResponse response, String content) throws ErrorSendingErrorException {

		PrintWriter out = null;
		try {
			out = response.getWriter();
			response.setContentType("text/xml");

			out.print(content);

		} catch (IOException ioe) {
			throw new ErrorSendingErrorException("I/O error when sending content: "
					+ content + " : " + ioe.getMessage());
		} finally {
			if (out != null) {
				out.close();
			}
		}
	}
	
	public static void sendErrorXML(HttpServletResponse response, Long errorCode,
			BaseException be) throws ErrorSendingErrorException {

		logMetacat.error(errorCodes.get(errorCode) + " : " + be.getMessage());

		PrintWriter out = null;
		try {
			out = response.getWriter();
			response.setContentType("text/xml");

			out.println("<?xml version=\"1.0\"?>");
			out.println("<error>");
			out.println("<code>" + errorCode + "</code>");
			out.println("<defaultMessage>" + errorCodes.get(errorCode) + "</defaultMessage>");
			out.println("<coreMessage>" + be.getCoreMessage() + "</coreMessage>");
			out.println("<chainedMessage>" + be.getMessage() + "</chainedMessage>");
			out.println("</error>");

		} catch (IOException ioe) {
			throw new ErrorSendingErrorException("I/O error when returning error: "
					+ errorCode + " : " + ioe.getMessage());
		} finally {
			if (out != null) {
				out.close();
			}
		}
	}
	
	public static void sendErrorXML(HttpServletResponse response, Long errorCode,
			String message) throws ErrorSendingErrorException {

		logMetacat.error(errorCodes.get(errorCode) + " : " + message);

		PrintWriter out = null;
		try {
			out = response.getWriter();
			response.setContentType("text/xml");

			out.println("<?xml version=\"1.0\"?>");
			out.println("<error>");
			out.println("<code>" + errorCode + "</code>");
			out.println("<defaultMessage>" + errorCodes.get(errorCode) + "</defaultMessage>");
			out.println("<coreMessage>" + message + "</coreMessage>");
			out.println("<chainedMessage></chainedMessage>");
			out.println("</error>");

		} catch (IOException ioe) {
			throw new ErrorSendingErrorException("I/O error when returning error: "
					+ errorCode + " : " + ioe.getMessage());
		} finally {
			if (out != null) {
				out.close();
			}
		}
	}
	
	public static void sendSuccessXML(HttpServletResponse response, String message) 
		throws ErrorSendingErrorException {

		PrintWriter out = null;
		try {
			out = response.getWriter();
			response.setContentType("text/xml");

			out.println("<?xml version=\"1.0\"?>");
			out.println("<success>" + message + "</success>");
		} catch (IOException ioe) {
			throw new ErrorSendingErrorException("I/O error when returning success XML: "
					+ message + " : " + ioe.getMessage());
		} finally {
			if (out != null) {
				out.close();
			}
		}
	}
	
	public static boolean isSuccessXML(String message) {

		return message.indexOf("<success>") != -1;
	}
	
}
