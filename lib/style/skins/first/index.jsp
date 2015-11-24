<%@ page    language="java" %>
<%
	/**
 * 
 * '$RCSfile$'
 * Copyright: 2008 Regents of the University of California and the
 *             National Center for Ecological Analysis and Synthesis
 *    '$Author$'
 *      '$Date$'
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
     
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
%>

<%@ include file="../../common/common-settings.jsp"%>
<%@ include file="../../common/configure-check.jsp"%>

<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<html>
<head>
<title>FIRST Assessment Metadata Repository</title>
<link rel="stylesheet" type="text/css"
	href="<%=STYLE_SKINS_URL%>/first/first.css">
<script language="JavaScript" type="text/JavaScript"
	src="<%=STYLE_SKINS_URL%>/first/first.js"></script>
<script language="JavaScript" type="text/JavaScript"
	src="<%=STYLE_SKINS_URL%>/first/search.js"></script>
<script language="JavaScript" type="text/JavaScript"
	src="<%=STYLE_COMMON_URL%>/branding.js"></script>
<script language="Javascript" type="text/JavaScript"
	src="<%=STYLE_COMMON_URL%>/prototype-1.5.1.1/prototype.js">
</script>
<script language="Javascript" type="text/JavaScript"
	src="<%=STYLE_COMMON_URL%>/effects.js">
</script>
<script language="Javascript">

		function browseAll() {
			document.getElementById("searchBox").value = "%";
			document.getElementById("includeItems").checked = false;
			searchAssessments(false);
		}
		
		function searchAssessments(includeItems) {
			var searchString = document.getElementById("searchBox").value;
			var institution = document.getElementById("institution").value;
			var instructor = document.getElementById("instructor").value;
			var course = document.getElementById("course").value;
			var year = document.getElementById("year").value;
			var otherField = document.getElementById("otherField").value;
			var otherValue = document.getElementById("otherValue").value;
			//keywords
			//var keywordThesaurus1 = document.getElementById("keywordThesaurus1").value;
			var keyword1 = document.getElementById("keyword1").value;

			
			var searchTerms = new Object();
			searchTerms["anyValue"] = searchString;
			searchTerms["institution/organizationName"] = institution;
			searchTerms["instructor/individualName/surName"] = instructor;
			searchTerms["course/lom/general/title/string"] = course;
			searchTerms["course/year"] = year;
			searchTerms[otherField] = otherValue;
			//TODO: implement thesaurus matching (compound INTERSECTION)
			searchTerms["keyword"] = keyword1;
			searchTerms["fieldentry"] = keyword1;
			
			var operator = "UNION";
			if (document.getElementById("all").checked) {
				operator = "INTERSECT";
			}
			if (document.getElementById("includeItems").checked) {
				includeItems = true;
			} else {
				includeItems = false;
			}
			
			var metacatURL = "<%=CONTEXT_URL%>/metacat";
			
			//generate the query for items
			var itemQueryString = 
				generateSearchString(
					searchTerms,
					null,
					operator,
					false, 
					true);
			
			//alert("itemQueryString=" + itemQueryString);

			loadAssessments = function(transport) {
				
				//harvest the itemIds
				var itemIds = new Object();
				var itemIdForm = document.getElementById("itemIdForm");
				if (itemIdForm) {
					var itemIdObj = itemIdForm.itemIds;
					//alert("itemIdObj=" + itemIdObj);

					if (itemIdObj.length > 1) {
						for (var i=0; i < itemIdObj.length; i++) {
							itemIds[i] = itemIdObj[i].value;
						}
					} else {
						itemIds[0] = itemIdObj.value;
					}
				}
				
				//generate the assessment query with item ids included
				var queryString = 
					generateSearchString(
							searchTerms,
							itemIds,
							operator,
							true, 
							false);
	
				//alert("queryString=" + queryString);
	
				//load the assessments
				callAjax(metacatURL, queryString, "first-assessment", "ajaxSearchResults", null);
				Effect.Appear('ajaxSearchResults', {duration: 1.5});
			};

			//do we search using the items or not?
			if (includeItems) {
				//load the items (which calls the function above)
				callAjax(metacatURL, itemQueryString, "first-item", "itemSearchResults", loadAssessments);
			} else {
				//generate the assessment query with item ids included
				var queryString = 
					generateSearchString(
							searchTerms,
							null,
							operator,
							true, 
							false);
				// just load the assessments
				callAjax(metacatURL, queryString, "first-assessment", "ajaxSearchResults", null);
				Effect.Appear('ajaxSearchResults', {duration: 1.5});
			}	
		}
   </script>
</head>
<body>
<script language="JavaScript">
          insertTemplateOpening("<%=CONTEXT_URL%>");
          insertSearchBox("<%=CONTEXT_URL%>");
      </script>

<table width="100%" border="0" cellspacing="20" cellpadding="0">
	<tr>
		<th colspan="2">
			Search
		</th>
	</tr>		
	<tr>
		<td>
			<form method="POST" action="<%=SERVLET_URL%>" target="_top" id="searchForm">
			
			<table class="tables" cellpadding="8" cellspacing="0">
				<tr class="sectheader">

					<td class="borderbottom" align="left" colspan="2">
						Any field:
						<input size="30" name="searchstring" type="text" value="" id="searchBox">
					</td>

				</tr>
				<tr>
					<td valign="top" align="left" class="borderbottom">
							
						<input name="query" type="hidden"> 
						<input name="qformat" value="first" type="hidden"> 
						<input type="hidden" name="action" value="squery"> 								
						
						<table>
							<tr>
								<td>Institution: </td>
								<td><input name="institution" id="institution" type="text" size="14"/></td>
							</tr>
							<tr>
								<td>Course: </td>
								<td><input name="course" id="course" type="text" size="14"/></td>
							</tr>
							<tr>
								<td>Instructor: </td>
								<td><input name="instructor" id="instructor" type="text" size="14"/></td>
							</tr>
							<tr>
								<td>Year: </td>
								<td><input name="year" id="year" type="text" size="4"/></td>
							</tr>
							
						</table>
					</td>
					<td valign="top" class="borderbottom">
						<table>
							<tr>
								<td>
									<select id="otherField" name="otherField">
										<option value="assessment/title">Assessment Title</option>
										<option value="assessment/type">Assessment Type</option>
										<option value="assessment/duration">Assessment Duration</option>
										<option value="assessment/grading/@group">Group Grading</option>
										<option value="assessment/grading/@size">Group Size</option>
										<option value="assessment/grading">Group Grading Approach</option>

										<option value="course/lom/general/identifier/entry">Course Id</option>
										<option value="course/lom/general/title/string">Course Title</option>
										<option value="course/lom/general/description/string">Course Description</option>
										<option value="course/term">Course Term</option>
										<option value="course/year">Course Year</option>
										<option value="course/coverage/rangeOfDates/beginDate/calendarDate">Course Start Date</option>
										<option value="course/coverage/rangeOfDates/endDate/calendarDate">Course End Date</option>

										<option value="institution/organizationName">Institution Name</option>
										<option value="instructor/individualName/surName">Instructor Surname</option>
										<option value="instructor/organizationName">Instructor Organization</option>

									</select>
								</td>
								<td><input name="otherValue" id="otherValue" type="text" size="14"/></td>
							</tr>
							<tr>
								<td>
									Keyword:
								</td>
								<td><input name="keyword1" id="keyword1" type="text" size="14"/></td>
							</tr>
							
						</table>
					</td>
				</tr>
				<tr>
					<td valign="top" class="borderbottom">
						<table>
							<tr>
								<td nowrap="nowrap">
									<input name="anyAll" id="any" value="UNION" type="radio" checked="checked"/>
								</td>
								<td nowrap="nowrap">Match any</td>
							</tr>
							<tr>		
								<td nowrap="nowrap">
									<input name="anyAll" id="all" value="INTERSECT" type="radio"/>
								</td>	
								<td nowrap="nowrap">Match all</td>
							</tr>
							<tr>		
								<td nowrap="nowrap">
									<input name="includeItems" id="includeItems" checked="checked" type="checkbox"/>
								</td>	
								<td nowrap="nowrap">Search across Items</td>
							</tr>
						</table>
					</td>
					<td valign="bottom" class="borderbottom">
						<table>
							<tr>
								<td colspan="2">
									<input type="button" onclick="javascript:searchAssessments(true)" value="Search"/>
								</td>
							</tr>
						</table>
					</td>
				</tr>
			</table>
			</form>
			
		</td>
		<td colspan="1" valign="top">
			<div align="left">
				<p align="left">This tool allows you to search for Assessments either by keyword,
				or with a structured search that targets particular facets of an assessment.
				<br />
				<br />
				You can use the '%' character as a wildcard in your searches (e.g.,
				'%biology%' would locate any phrase with the word biology embedded within it).
				</p>
				<a href="javascript:browseAll()">Browse All Assessments...</a>
			</div>
		</td>
	</tr>
	<tr>
		<td valign="top" colspan="2">	
			<div id="ajaxSearchResults" style="display:none;">
				Loading search results 
				<img src="<%=CONTEXT_URL%>/style/images/spinner.gif" border="none"/>
			</div>
		</td>
	</tr>
	<tr>
		<td valign="top" colspan="2">	
			<div id="itemSearchResults" style="display:none;">
				Loading Assessment Item results 
				<img src="<%=CONTEXT_URL%>/style/images/spinner.gif" border="none"/>
			</div>
		</td>
	</tr>
</table>

<script language="JavaScript">          
    insertTemplateClosing("<%=CONTEXT_URL%>");
</script>
</body>
</html>
