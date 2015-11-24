<%@ page    language="java" %>
<%
/*
 *   '$RCSfile$'
 *     Authors: Matthew Brooke
 *   Copyright: 2000 Regents of the University of California and the
 *              National Center for Ecological Analysis and Synthesis
 * For Details: http://www.nceas.ucsb.edu/
 *
 *    '$Author$'
 *      '$Date$'
 *  '$Revision$'
 *
 * Settings file for KNB portal
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
%>

<%@ include file="../../common/common-settings.jsp"%>

<% 
//Global settings for metacat are in common-settings.jsp.  Use this file to
//override or add to global values for this skin.

	// default title for :
	String      DEFAULT_PORTALHEAD_TITLE = "NEW Biocomplexity Data Search";
	
	// if true, POST variables echoed at bottom of client's browser window in a big yellow box
    // Set this value to override the global value
    //boolean     DEBUG_TO_BROWSER      = false;
	
	//version number to be displayed for latest morpho distribution 
	String MORPHO_VERSION_NUMBER = "1.5.1";
	
	// site-root-relative uri's for download morpho links for 3 target OS's. Note that 
	// these should be the distributions that INCLUDE THE JAVA VM (except for the MacOSX 
	// version, since Mac OSX ships with a JRE)
	String      MORPHO_DOWNLOAD_LINK_WINDOWS = "software/dist/morpho-" + MORPHO_VERSION_NUMBER + "-jre.exe";
	String      MORPHO_DOWNLOAD_LINK_MACOSX  = "software/dist/morpho-" + MORPHO_VERSION_NUMBER + "-MACOSX.zip";
	String      MORPHO_DOWNLOAD_LINK_LINUX   = "software/dist/morpho-" + MORPHO_VERSION_NUMBER + "-jre.sh";
	
	// site-root-relative uri's for download morpho links for 3 target OS's. Note that 
	// these should be the distributions that DO NOT INCLUDE THE JAVA VM 
	String      MORPHO_DOWNLOAD_LINK_WINDOWS_NO_JRE = "software/dist/morpho-" + MORPHO_VERSION_NUMBER + ".exe";
	String      MORPHO_DOWNLOAD_LINK_LINUX_NO_JRE   = "software/dist/morpho-" + MORPHO_VERSION_NUMBER + ".sh";
	
	// site-root-relative uri's for download morpho zip & gzip links for binaries and source 
	String      MORPHO_DOWNLOAD_LINK_ZIP            = "software/dist/morpho-" + MORPHO_VERSION_NUMBER + ".zip";
	String      MORPHO_DOWNLOAD_LINK_GZIP           = "software/dist/morpho-" + MORPHO_VERSION_NUMBER + ".tar.gz";
	String      MORPHO_DOWNLOAD_LINK_ZIP_SRC        = "software/dist/morpho-src-" + MORPHO_VERSION_NUMBER + ".zip";
	String      MORPHO_DOWNLOAD_LINK_GZIP_SRC       = "software/dist/morpho-src-" + MORPHO_VERSION_NUMBER + ".tar.gz";
	
	String      MORPHO_CHANGE_LOG_LINK              = "software/morpho/README.txt";
	
	String      MORPHO_DOWNLOAD_LINK_MAC_OS9        = "software/dist/morpho-1.1.2-jre.bin";
	String      MORPHO_DOWNLOAD_LINK_MAC_OS9_NO_JRE = "software/dist/morpho-1.1.2.bin";
	String      MORPHO_DOWNLOAD_LINK_OLD_VERSIONS   = "software/dist/";
	
    // Add any local post fields to COMMON_SEARCH_METACAT_POST_FIELDS, 
    // SIMPLE_SEARCH_METACAT_POST_FIELDS, and ADVANCED_SEARCH_METACAT_POST_FIELD here
	COMMON_SEARCH_METACAT_POST_FIELDS +=
         "<input type=\"hidden\" name=\"qformat\"       value=\"knb2\"\\>\n"
        +"<input type=\"hidden\" name=\"returnfield\"   value=\"dataset/dataTable/distribution/online/url\"\\>\n"
        +"<input type=\"hidden\" name=\"returnfield\"   value=\"dataset/dataTable/distribution/inline\"\\>\n";         
              
    SIMPLE_SEARCH_METACAT_POST_FIELDS +=
         "<input type=\"hidden\" name=\"qformat\"       value=\"knb2\"\\>\n"
        +"<input type=\"hidden\" name=\"returnfield\"   value=\"dataset/dataTable/distribution/online/url\"\\>\n"
        +"<input type=\"hidden\" name=\"returnfield\"   value=\"dataset/dataTable/distribution/inline\"\\>\n";         
        
    ADVANCED_SEARCH_METACAT_POST_FIELDS +=
        "<input type=\"hidden\" name=\"qformat\"       value=\"knb2\"\\>\n"
        +"<input type=\"hidden\" name=\"returnfield\"   value=\"dataset/dataTable/distribution/online/url\"\\>\n"
        +"<input type=\"hidden\" name=\"returnfield\"   value=\"dataset/dataTable/distribution/inline\"\\>\n";         
        
%>
