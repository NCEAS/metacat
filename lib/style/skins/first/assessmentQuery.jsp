<%@page 
import="java.sql.ResultSet"%><%@page 
import="edu.ucsb.nceas.first.metacat.client.AssessmentQuery"%><%@page
import="edu.ucsb.nceas.utilities.OrderedMap"%><%@page 
import="au.com.bytecode.opencsv.CSVWriter"%><%@page 
import="java.util.*"%><%@page 
import="java.io.*"%><%

String[] docids = request.getParameterValues("docids");
String questionId = request.getParameter("questionId");
String qformat = request.getParameter("qformat");

if (docids != null && docids.length > 0) {

	List assessmentDocIds = Arrays.asList(docids);
	
	//the "columns" to extract from the metadata document (will be param soon)
	Map attributeMap = new OrderedMap();
	attributeMap.put("id", "//@packageId");
	attributeMap.put("title", "//assessment/title");
	attributeMap.put("duration", "//assessment/duration");
	attributeMap.put("badColumn", "//does/not/exist");
	attributeMap.put("item", "//assessmentItems/assessmentItem/assessmentItemId");
	
	//make a title
	String tableTitle = "Showing data for ";
	String fileName = "results-";
	for (int i=0; i<docids.length; i++) {
		tableTitle += docids[i];
		tableTitle +=", ";
		
		fileName += docids[i];
	}
	tableTitle = tableTitle.substring(0, tableTitle.length()-2);
	fileName += ".csv";
	
	//AssessmentQuery.testEcogrid("edml.4.5");
	ResultSet rs = null;
	if (questionId != null) {
		rs = AssessmentQuery.selectResponseData(assessmentDocIds, questionId, "=", new Integer(1));
	}
	else {
		rs = AssessmentQuery.selectMergedResponseData(assessmentDocIds, attributeMap);
	}
	
	if (rs == null) {
		return;
	}
	
	//get the results as csv file
	if (qformat != null && qformat.equalsIgnoreCase("csv")) {
		response.setContentType("text/csv");
		//response.setContentType("application/csv");
        response.setHeader("Content-Disposition", "attachment; filename=" + fileName);
        
		Writer writer = new OutputStreamWriter(response.getOutputStream());
		CSVWriter csv = new CSVWriter(writer, CSVWriter.DEFAULT_SEPARATOR, CSVWriter.NO_QUOTE_CHARACTER);
		csv.writeAll(rs, true);
		csv.flush();
		
		response.flushBuffer();
		
		rs.close();
		return;
	}
	
	int numColumns = rs.getMetaData().getColumnCount();
%>

<!-- <h3><%=tableTitle %></h3> -->

<table>
<!-- 
<tr>
	<th colspan="<%=numColumns %>"><%=tableTitle %></th>
</tr>
-->
<tr>
<% for (int i=1; i<=numColumns; i++) { %>
	<th><%=rs.getMetaData().getColumnName(i) %></th>
<% } %>
</tr>
<%
while (rs.next()) {
	//System.out.println("rs:" + rs.getInt(1));
%>
	<tr>
<% for (int i=1; i<=numColumns; i++) { %>
		<td><%=rs.getString(i) %></td>
<% } %>		
	</tr> 
<% 
} 
%>
</table>
<%
	//clean up
	if (rs != null) {
		rs.close();
	}	
}//end if docids
else {
%>
No items selected
<%}%>