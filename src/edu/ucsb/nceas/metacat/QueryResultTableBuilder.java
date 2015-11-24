/**
 *  '$RCSfile$'
 *    Purpose: A Class that tracks sessions for MetaCatServlet users.
 *  Copyright: 2000 Regents of the University of California and the
 *             National Center for Ecological Analysis and Synthesis
 *    Authors: Matt Jones
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
package edu.ucsb.nceas.metacat;

import java.util.TimerTask;

import org.apache.log4j.Logger;

/**
 *  QueryResutlTableBuilder is class to build xml_queryresult table during metacat initialization.
 *  xml_querytable is a db table to store query returnfiled value. It is a cache of resultset and 
 *  peformance optimization approach to replace xml_index and xml_node tables.  In metacat 1.8.0,
 *  xml_queryresult table is built during the first time of query hitting metacat. So first time search
 *  will take longer time since this search involves xml_index and xml_node tables.  In order to
 *  decrease the first query search time, this class will build the xml_querytable during the metacat
 *  intialization base on some metacat common clients' query , e.g. kepler, morpho, esa, knb, nceas,
 *  sanparks' regestiry.
 */
public class QueryResultTableBuilder extends TimerTask
{
	
	private String[] docidList = null;
	private String[] returnFieldList = null;
	private Logger logMetacat = Logger.getLogger(QueryResultTableBuilder.class);
	/**
	 * Default the constructor.  It will get doc id list from current xml_table
	 *
	 */
	QueryResultTableBuilder()
	{
		 docidList = getDocListFromXMLDocumentTables();
		 returnFieldList = getReturnFieldList();
	}
	
	/**
	 *  Constructor with given docid list
	 * @param docList  list of docid
	 */
	 QueryResultTableBuilder(String[] docList)
	 {
		 docidList = docList;
		 returnFieldList = getReturnFieldList();
	 }
	 
	 /**
	  * Builds the result table base on the given docid list and returnfields
	  */
	public void run()
	{
		buildQueryResultTable();
	}
	
	/*
	 * Build query result table.
	 */
	private void buildQueryResultTable()
	{
		if (docidList != null && returnFieldList != null )
		{
			
		}
		else
		{
			logMetacat.warn("There is no doc list or returnfFieldList. We wouldn't build queryresult table ");
		}
	}
	
	/*
	 * Gets return document list array from xml_documents table.
	 */
	private String[] getDocListFromXMLDocumentTables()
	{
		String[] returnDocList = null;
		return returnDocList;
	}
	
	/*
	 * Gets common return fields list from metacat.properties file.
	 */
	private String[] getReturnFieldList()
	{
		String[] returnField = null;
		return returnField;
		
	}

}
