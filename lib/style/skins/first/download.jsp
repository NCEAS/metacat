<%@page 
import="java.util.Iterator"%><%@page 
import="java.util.ArrayList"%><%@page 
import="edu.ucsb.nceas.utilities.OrderedMap"%><%@page 
import="java.util.List"%><%@page 
import="java.util.Enumeration"%><%@page 
import="java.sql.SQLException"%><%@page 
import="org.ecoinformatics.datamanager.transpose.DataTranspose"%><%@page 
import="au.com.bytecode.opencsv.CSVWriter"%><%@page 
import="java.io.OutputStreamWriter"%><%@page 
import="java.io.Writer"%><%@page 
import="java.sql.ResultSet"%><%@page 
import="edu.ucsb.nceas.metacat.dataquery.DataQuery"%><%@page 
import="java.io.IOException"%><%@page 
import="edu.ucsb.nceas.utilities.PropertyNotFoundException"%><%@page
import="edu.ucsb.nceas.metacat.util.SystemUtil"%><%@page 
import="java.util.Hashtable"%><%@ page 
language="java" %><%
/**
 * 
 * '$RCSfile$'
 * Copyright: 2008 Regents of the University of California and the
 *             National Center for Ecological Analysis and Synthesis
 *    '$Author: leinfelder $'
 *      '$Date: 2008-08-22 16:48:56 -0700 (Fri, 22 Aug 2008) $'
 * '$Revision: 4305 $'
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
     
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */  
%><%
Hashtable params = getParams(request);
handleDataquery(params,response,request.getSession().getId());
%><%!
	private Hashtable getParams(HttpServletRequest request) {
		Hashtable params = new Hashtable();
		Enumeration<String> paramlist = (Enumeration<String>)request.getParameterNames();
		while (paramlist.hasMoreElements()) {
	
			String name = paramlist.nextElement();
			String[] value = request.getParameterValues(name);
			params.put(name, value);
		}
	
		return params;
	}
%><%!
	private List processResultsSet(ResultSet rs, int omitColumn, String omitColumnLabel) throws SQLException {
		List retTable = new ArrayList();
		int colCount = rs.getMetaData().getColumnCount();
		List columnHeaders = new ArrayList();
		int recordCount = 1;
		OrderedMap uniqueIds = new OrderedMap();
		
		while (rs.next()) {
			List row = new ArrayList();
			//get the values for this row
			for (int i = 1; i <= colCount; i++) {

				String colName = rs.getMetaData().getColumnName(i);
				String value = rs.getString(i);
					
				//clean up the value
				value = cleanUp(value);
				
				//hide the ids
				if (i == omitColumn) {
					String lookupValue = (String) uniqueIds.get(value);
					if (lookupValue == null) {
						lookupValue = recordCount + "";
					}
					uniqueIds.put(value, lookupValue);
					value = lookupValue;
					colName = omitColumnLabel;
				}
				if (recordCount == 1) {
					columnHeaders.add(colName);
				}
				
				row.add(value);
			}
			retTable.add(row.toArray(new String[0]));
			recordCount++;
		}
		//make sure there are no "?column?" headers
		for (int i = 0; i < columnHeaders.size(); i++) {
			String columnName = (String) columnHeaders.get(i);
			if (columnName.equals("?column?")) {
				String previousColumn = (String) columnHeaders.get(i-1);
				String[] previousParts = previousColumn.split("_");
				int previousCount = Integer.parseInt(previousParts[1]);
				columnName = previousParts[0]  + "_"+ (previousCount + 1);
				columnHeaders.set(i, columnName);
			}
		}
		retTable.add(0, columnHeaders.toArray(new String[0]));
		return retTable;
	}
%><%!
	private List transpose(ResultSet rs, int idCol, int pivotCol, List pivotAttributes, boolean omitIdValues) throws SQLException {
		//map keyed by id column - data
		OrderedMap table = new OrderedMap();
		//track all the data columns
		OrderedMap widestRow = new OrderedMap();
		//map keyed by the pivot column - metadata
		OrderedMap headerRows = new OrderedMap();
		
		//vocab columns
		String vocabNameCol = "qmetadatavocabulary";
		String vocabValueCol = "qmetadatavalue";
		//maps for the vocab lists
		OrderedMap vocabNames = new OrderedMap();
		OrderedMap vocabValues = new OrderedMap();
		//all vocab names/values
		List allVocabNames = new ArrayList();
		List allVocabValues = new ArrayList();
		boolean twoColumnMetadata = false;
		
		int colCount = rs.getMetaData().getColumnCount();
		String idColName = rs.getMetaData().getColumnName(idCol);
		
		while (rs.next()) {
			String id = rs.getString(idCol);
			String pivotValue = rs.getString(pivotCol);
			
			//look up the data row we are working on
			OrderedMap row = (OrderedMap) table.get(id);
			if (row == null) {
				row = new OrderedMap();
			}
			//look up the metadata row we are working on
			OrderedMap metadataRow = (OrderedMap) table.get(pivotValue);
			if (metadataRow == null) {
				metadataRow = new OrderedMap();
			}
			
			//get the values for this pivot
			for (int i = 1; i <= colCount; i++) {
				if (i != pivotCol) {
					String colName = rs.getMetaData().getColumnName(i);
					
					//check for "missing" headers in the qchoice
					/*
					if (colName.equals("?column?")) {
						String previousColumn = rs.getMetaData().getColumnName(i-1);
						if (previousColumn.startsWith("qchoice")) {
							String[] previousParts = previousColumn.split("_");
							int previousCount = 0;
							if (previousParts.length == 2) {
								previousCount = Integer.parseInt(previousParts[1]);
							}
							colName = previousParts[0]  + "_"+ (previousCount + 1);
						}
					}
					*/
					
					String value = rs.getString(i);
					
					//clean up the value
					value = cleanUp(value);
					
					//do we include this column in the pivot?
					if (pivotAttributes.contains(colName)) {
						//annotate the column name with the pivot column value if not the id column
						if (i != idCol) {
							colName = pivotValue + "_" + colName;
						}
						row.put(colName, value);
					}
					else {
						if (colName.startsWith(vocabNameCol) || colName.startsWith(vocabValueCol)) {
							//don't add it to the normal metadata
						}
						else {
							metadataRow.put(colName, value);
						}
					}
					//names
					if (colName.startsWith(vocabNameCol)) {
						List list = (List) vocabNames.get(pivotValue);
						if (list == null) {
							list = new ArrayList();
						}
						list.add(value);
						vocabNames.put(pivotValue, list);
						allVocabNames.add(value);
					}
					//values
					if (colName.startsWith(vocabValueCol)) {
						List list = (List) vocabValues.get(pivotValue);
						if (list == null) {
							list = new ArrayList();
						}
						list.add(value);
						vocabValues.put(pivotValue, list);
						allVocabValues.add(value);
					}
				}
			}
			//track the data columns - the values are junk
			widestRow.putAll(row);
			
			//put the row back (or maybe it's the first time)
			table.put(id, row);
			
			//put the metadata header back
			headerRows.put(pivotValue, metadataRow);
			
		}
		
		/** Construct the table structure for returning **/
		
		//now make it into a list
		List retTable = new ArrayList();
		
		//map keyed by metadata labels
		OrderedMap metadataHeaders = new OrderedMap();
		
		//do the data header - drives the other columns - based on widest entry
		List header = new ArrayList(widestRow.keySet());
		
		//do the metadata header rows (basically rotate them around)
		Iterator headerIter = header.iterator();
		String lastValue = "";
		while (headerIter.hasNext()) {
			String column = (String) headerIter.next();
			//get the pivotValue part of column name
			String pivotValue = column;
			try {
				pivotValue = column.substring(0, column.indexOf("_"));
			}
			catch (Exception e) {}
			//look up the row from the metadata - keyed by pivot value
			OrderedMap metadataRow = (OrderedMap) headerRows.get(pivotValue);
			if (metadataRow != null) {
				//go through the values
				Iterator metadataIter = metadataRow.keySet().iterator();
				while (metadataIter.hasNext()) {
					String key = (String) metadataIter.next();
					String value = (String) metadataRow.get(key);
					OrderedMap newMetadataRow = (OrderedMap) metadataHeaders.get(key);
					if (newMetadataRow == null) {
						newMetadataRow = new OrderedMap();
					}
					//if it's the same as the last one, just use null value
					if (lastValue.equals(pivotValue)) {
						value = null;
					} 
					newMetadataRow.put(column, value);
					metadataHeaders.put(key, newMetadataRow);
				}
			}
			
			lastValue = pivotValue;

		}
		
		//make metadata rows as list/arrays on the reteurn table
		Iterator metadataLabelIter = metadataHeaders.keySet().iterator();
		while (metadataLabelIter.hasNext()) {
			String label = (String) metadataLabelIter.next();
			OrderedMap row = (OrderedMap) metadataHeaders.get(label);
			List rowValues = new ArrayList(row.values());
			rowValues.add(0, label);
			if (twoColumnMetadata) {
				//add extra column
				rowValues.add(1, null);
			}
			retTable.add(rowValues.toArray(new String[0]));
		}
		
		//create the special vocab matrix rows
		List vocabTable = new ArrayList();
		List uniqueVocabs = new ArrayList();
		for (int i = 0; i < allVocabNames.size(); i++) {
			List vocabRow = new ArrayList();
			String vocabName = (String) allVocabNames.get(i);
			String vocabValue = (String) allVocabValues.get(i);
			String key = vocabName + "/" + vocabValue;
			//check if we've processed this already, skip if so
			if (uniqueVocabs.contains(key)) {
				continue;
			}
			uniqueVocabs.add(key);			
			if (twoColumnMetadata) {
				vocabRow.add(vocabName);
				vocabRow.add(vocabValue);
			}
			else {
				vocabRow.add(key);
			}
			//go through the questions now, again
			String lastPivotValue = "";
			headerIter = header.iterator();
			while (headerIter.hasNext()) {
				String column = (String) headerIter.next();
				//get the pivotValue part of column name if it exists
				String pivotValue = null;
				try {
					pivotValue = column.substring(0, column.indexOf("_"));
				}
				catch (Exception e) {}
				if (pivotValue == null) {
					continue;
				}
				//check to not duplicate values
				if (pivotValue.equals(lastPivotValue)) {
					vocabRow.add(null);
				}
				else {
					//check to see if this question has that keyword
					List names = (List) vocabNames.get(pivotValue);
					List values = (List) vocabValues.get(pivotValue);
					String containsVocabItem = "false";
					if (names != null) {
						int vocabNameIndex = names.indexOf(vocabName); 
						int vocabValueIndex = values.indexOf(vocabValue);
						// contains the vocab and the value _somewhere_
						if (vocabNameIndex > -1 && vocabValueIndex > -1) {
							containsVocabItem = "true";
						}
					}
					vocabRow.add(containsVocabItem);
				}
				lastPivotValue = pivotValue;
			}
			//put the row on
			vocabTable.add(vocabRow.toArray(new String[0]));
		}
		
		//put the vocab matrix on the table
		retTable.addAll(vocabTable);
		
		if (twoColumnMetadata) {
			//add column to data header row
			header.add(1, null);
		}
		
		//replace the "studentId" label
		int temp = header.indexOf("studentid");
		if (header.remove(temp) != null) {
			header.add(temp, "recordNum");
		}
		
		//put the data header row on the table
		retTable.add(header.toArray(new String[0]));
		
		//now the value rows in the table
		Iterator rowIter = table.values().iterator();
		int rowCount = 1;
		while (rowIter.hasNext()) {
			OrderedMap rowMap = (OrderedMap) rowIter.next();
			List row = new ArrayList();
			//iterate over the widest row's columns
			Iterator columnIter = widestRow.keySet().iterator();
			while (columnIter.hasNext()) {
				Object key = columnIter.next();
				Object value = rowMap.get(key);
				//hide the value used for Ids - just increment row
				if (key.equals(idColName) && omitIdValues) {
					value = String.valueOf(rowCount);
				}
				row.add(value);
			}
			rowCount++;
			if (twoColumnMetadata) {
				//add extra column
				row.add(1, null);
			}
			retTable.add(row.toArray(new String[0]));
		}
		
		return retTable;
	}
%><%!
	private String cleanUp(String value) {
		if (value != null) {
			value = value.replaceAll("\n", " ");
			value = value.replaceAll("\\s+", " ");
			value = value.replaceAll("<html>", " ");
			value = value.replaceAll("</html>", " ");
			value = value.replaceAll("<head>", " ");
			value = value.replaceAll("</head>", " ");
			value = value.replaceAll("<body>", " ");
			value = value.replaceAll("</body>", " ");
			//translate any ecogrid urls
			value = convertEcogridURL(value);
		}
		return value;
	}
%><%!
private String convertEcogridURL(String value) {
	if (value != null) {
		String prefix = "ecogrid://knb/";
		if (value.startsWith(prefix)) {
			//String docid = value.substring(prefix.length(), value.length());
			// TODO make URL
			String contextURL = "";
			try {
				contextURL = SystemUtil.getContextURL();
			}
			catch (PropertyNotFoundException pnfe) {
				//do nothing
			}
			value = value.replaceFirst(prefix, contextURL + "/metacat?action=read&docid=");
		}
	}
	return value;
}
%><%!
	private void handleDataquery(
			Hashtable<String, String[]> params,
	        HttpServletResponse response,
	        String sessionId) throws PropertyNotFoundException, IOException {
		
		DataQuery dq = null;
		if (sessionId != null) {
			dq = new DataQuery(sessionId);
		}
		else {
			dq = new DataQuery();
		}
		
		String dataqueryXML = (params.get("dataquery"))[0];
	
		ResultSet rs = null;
		try {
			rs = dq.executeQuery(dataqueryXML);
		} catch (Exception e) {
			//probably need to do something here
			e.printStackTrace();
			return;
		}
		
		//process the result set
		String qformat = "csv";
		String[] temp = params.get("qformat");
		if (temp != null && temp.length > 0) {
			qformat = temp[0];
		}
		String fileName = "query-results." + qformat;
		
		boolean transpose = false;
		temp = params.get("transpose");
		if (temp != null && temp.length > 0) {
			transpose = Boolean.parseBoolean(temp[0]);
		}
		int observation = 0;
		temp = params.get("observation");
		if (temp != null && temp.length > 0) {
			observation = Integer.parseInt(temp[0]);
		}
		int pivot = 0;
		temp = params.get("pivot");
		if (temp != null && temp.length > 0) {
			pivot = Integer.parseInt(temp[0]);
		}
		String[] pivotColumns = null;
		temp = params.get("pivotColumns");
		if (temp != null && temp.length > 0) {
			pivotColumns = temp;
		}
		
		//get the results as csv file
		if (qformat != null && qformat.equalsIgnoreCase("csv")) {
			response.setContentType("text/csv");
			//response.setContentType("application/csv");
	        response.setHeader("Content-Disposition", "attachment; filename=" + fileName);
	        
			Writer writer = new OutputStreamWriter(response.getOutputStream());
			//CSVWriter csv = new CSVWriter(writer, CSVWriter.DEFAULT_SEPARATOR, CSVWriter.NO_QUOTE_CHARACTER);
			CSVWriter csv = new CSVWriter(writer, CSVWriter.DEFAULT_SEPARATOR, CSVWriter.DEFAULT_QUOTE_CHARACTER, CSVWriter.DEFAULT_ESCAPE_CHARACTER);
			try {
				if (transpose) {
					List pivotCols = new ArrayList();
					if (pivotColumns != null) {
						for (int i = 0; i < pivotColumns.length; i++) {
							pivotCols.add(pivotColumns[i]);
						}
					} else {
						pivotCols.add("studentid");
						pivotCols.add("score");
						pivotCols.add("response");
						pivotCols.add("responsefile");
					}
					List transposedTable = transpose(rs, observation, pivot, pivotCols, true);
					csv.writeAll(transposedTable);
				} else {
					List processedTable = processResultsSet(rs, 3, "recordNum");
					csv.writeAll(processedTable);
				}
				
				csv.flush();
				response.flushBuffer();
				
				rs.close();
				
			} catch (SQLException e) {
				e.printStackTrace();
			}
			
			return;
		}
		
	}
%>