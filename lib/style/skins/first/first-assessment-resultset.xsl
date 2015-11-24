<?xml version="1.0"?>
<!--
*  '$RCSfile$'
*      Authors: Matt Jones, CHad Berkley
*    Copyright: 2000 Regents of the University of California and the
*               National Center for Ecological Analysis and Synthesis
*  For Details: http://www.nceas.ucsb.edu/
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
*
* This is an XSLT (http://www.w3.org/TR/xslt) stylesheet designed to
* convert an XML file showing the resultset of a query
* into an HTML format suitable for rendering with modern web browsers.
-->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">

	<xsl:output method="html"/>
	<xsl:param name="sessid"/>
	<xsl:param name="qformat">default</xsl:param>
	<xsl:param name="enableediting">false</xsl:param>
	<xsl:param name="contextURL"/>
	<xsl:template match="/">
		<script language="JavaScript">
          <![CDATA[
          submitform = function(action,docid,form_ref) {
              form_ref.action.value=action;
              form_ref.docid.value=docid;
              form_ref.sessionid.value="]]><xsl:value-of select="$sessid" /><![CDATA[";
              form_ref.qformat.value="first";
              form_ref.insertTemplate.value="1";
              form_ref.submit();
          }
          setSelect = function(form_ref, checkBoxName, checked) {
              var formElements = form_ref.elements;
              for (var i=0; i<formElements.length; i++) {
              	var myElement = formElements[i];
              	if (myElement.name == checkBoxName) {
              		myElement.checked = checked;
              	}
              }
          }    
          ]]>
        </script>
        <script type="text/javascript" language="Javascript">
			<![CDATA[
			downloadMergedResponseData = function(formId, fieldFormId) {
			
				var submitFormObj = document.getElementById(formId);
				submitFormObj.qformat.value = 'csv';
				submitFormObj.action.value = 'dataquery';
				
				//the form that holds the metadata field mappings - but in the header!
				var fieldFormObj = 
					getIframeDocument("iframeheaderclass").getElementById(fieldFormId);
				//alert("fieldFormObj: " + fieldFormObj);
				
				multipleAssessmentSearch(submitFormObj, fieldFormObj);
				
				submitFormObj.submit();
			}
			editCart = function(formId, operation, docid) {
			
				var metacatURL = "]]><xsl:value-of select="$contextURL" /><![CDATA[/metacat";
				//if (!areItemsSelected('docid') && docid) {
				//	alert("Please select at least one dataset");
				//	return false;
				//}
				
				var submitFormObj = document.getElementById(formId);
				submitFormObj.qformat.value = 'first';
				submitFormObj.action.value = 'editcart';
				submitFormObj.operation.value = operation;
				if (docid) {
					submitFormObj.docid.value = docid;
				}
				
				var myUpdate = new Ajax.Request(
					metacatURL,
					{	method: 'post',
						parameters: Form.serialize(submitFormObj),
						evalScripts: true,
						onSuccess: function(transport) {
							//alert('Cart changes saved: ' + operation); 
							//if we are in the cart, we should refresh the entire page
							if (document.getElementById("ajaxCartResults")) {
								window.location.reload();
							}
							else {
								window.document.getElementById("iframeheaderclass").src=window.document.getElementById("iframeheaderclass").src;
								//now check the cart contents and update those icons (but wait a second)
								setTimeout("setAllCartIcons()", 1000);
							}
						}, 
						onFailure: function(transport) {alert('failure making ajax call');}
					 });
								
			}
			read = function(formId, docid, divId) {
			
				//alert("divId: " + divId);
				
				var metacatURL = "]]><xsl:value-of select="$contextURL" /><![CDATA[/metacat";
				
				var submitFormObj = document.getElementById(formId);
				submitFormObj.qformat.value = 'first';
				submitFormObj.action.value = 'read';
				submitFormObj.docid.value = docid;
				submitFormObj.insertTemplate.value = '0';
				
				var myUpdate = new Ajax.Updater(
					divId,
					metacatURL,
					{	method: 'post',
						parameters: Form.serialize(submitFormObj),
						evalScripts: true,
						onSuccess: function(transport) {showDiv(divId); }, 
						onFailure: function(transport) {alert('failure making ajax call');}
					 });
								
			}
			areItemsSelected = function(checkBoxName) {
				var checkBoxes = document.getElementsByName(checkBoxName);
				
				for (var i=0; i< checkBoxes.length; i++) {
					if (checkBoxes[i].checked == true) {
						return true;
					}	
				}
				//alert("please select a checkbox: " + checkBoxName);
				return false;
				
			}
			setControlVisibility = function() {
				if (document.getElementById("ajaxCartResults")) {
					new Effect.Appear("download");
					new Effect.Appear("transposeDiv");
					new Effect.Appear("metadataCountDiv");
				}
			}	
			isInCart = function(docid) {
		
				//look up the cart in the header
				var doc = getIframeDocument("iframeheaderclass");
				var objs = doc.getElementsByName("@packageId");
				
				if (objs.length == 0) {
					return false;
				}
				for (var i=0; i< objs.length; i++) {
					if (docid == objs[i].value) {
						return true;
					}
				}
				return false;
			}			
			hideDiv = function(divId) {
				new Effect.Puff(divId);
				//Element.hide(divId);
			}
			showDiv = function(divId) {
				new Effect.Appear(divId)
				//Element.show(divId);
			}
			setAllCartIcons = function() {
				var docids = document.getElementsByName('docid');
				for (var i=0; i< docids.length; i++) {
					var docid = docids[i].value;
					if (docid != "") {
						setCartIcon(docid);
					}	
				}
			}
			setCartIcon = function(docid) {
				//show or hide the div
				//alert("setting icon for: " + docid);
				if (isInCart(docid)) {
					hideDiv("ajaxDiv" + docid + "add");
					showDiv("ajaxDiv" + docid + "remove");
				}
				else {
					hideDiv("ajaxDiv" + docid + "remove");
					showDiv("ajaxDiv" + docid + "add");
				}
			}
			
		 ]]>	
		</script>
        
		<p class="emphasis">Assessments found: <xsl:number value="count(resultset/document)" /></p>      
      
		<!-- This tests to see if there are returned documents,
            if there are not then don't show the query results -->

		<xsl:if test="count(resultset/document) &gt; 0">
			<script type="text/javascript" language="Javascript">
				<![CDATA[
					//call on load
					setControlVisibility();
				]]>	
			</script>
			<div id="download" style="display:none;">
				<p>
					<a>
						<xsl:attribute name="href">javascript:{}</xsl:attribute>
						<xsl:attribute name="onclick">javascript:downloadMergedResponseData('assessmentForm', 'fieldForm')</xsl:attribute>
						<img border="none">
							<xsl:attribute name="src">
								<xsl:value-of select="$contextURL" />
								<xsl:text>/style/images/page_white_put.png</xsl:text>
							</xsl:attribute>
						</img>
						<xsl:text> Download (.csv)</xsl:text>
					</a>
				</p>
			</div>
						
			<!-- for reading the assessment details -->
			<form action="{$contextURL}/metacat" method="POST" id="readForm" name="readForm" >
				<input type="hidden" name="qformat" value="first" />
				<input type="hidden" name="sessionid">
					<xsl:attribute name="value">
						<xsl:value-of select="$sessid" />						
					</xsl:attribute>
				</input>
				<input type="hidden" name="action" value="read" />
				<input type="hidden" name="docid" />
				<input type="hidden" name="insertTemplate" value="1"/>
			</form>
			
			<!-- for editing cart -->				
			<form action="{$contextURL}/metacat" method="POST" id="cartForm" name="cartForm" >
				<input type="hidden" name="qformat" value="first" />
				<input type="hidden" name="sessionid">
					<xsl:attribute name="value">
						<xsl:value-of select="$sessid" />						
					</xsl:attribute>
				</input>
				<input type="hidden" name="action" value="editcart" />
				<input type="hidden" name="operation" />
				<input type="hidden" name="docid" />

			</form>	
			
			<!-- for downloading data -->				
			<form action="{$contextURL}/style/skins/first/download.jsp" method="POST" id="assessmentForm" name="assessmentForm" >
				<input type="hidden" name="qformat" id="qformat" value="first" />
				<input type="hidden" name="sessionid" id="sessionid">
					<xsl:attribute name="value">
						<xsl:value-of select="$sessid" />						
					</xsl:attribute>
				</input>
				<input type="hidden" name="action" id="action" value="read" />
				<input type="hidden" name="dataquery" id="dataquery" />
				<div id="metadataCountDiv" style="display:none;">
					--- Options ---
					<br />
					<select name="metadataCount" id="metadataCount" width="2" >
						<option value="1">1</option>
						<option value="2">2</option>
						<option value="3">3</option>
						<option value="4">4</option>
						<option value="5">5</option>
						<option value="6">6</option>
						<option value="7">7</option>
						<option value="8">8</option>
						<option value="9">9</option>
						<option value="10">10</option>
						<option value="20">20</option>
						<option value="30">30</option>
						<option value="40">40</option>
						<option value="50">50</option>
					</select>
					item metadata fields included
				</div>
				<div id="transposeDiv" style="display:none;">
					<input type="checkbox" name="transpose" id="transpose" value="true" />Transpose Data, include:
					<br />
					<input type="hidden" id="pivotColumns" name="pivotColumns" value="studentid" />
					<input type="checkbox" id="pivotColumns" name="pivotColumns" value="score" checked="checked" />score
					<input type="checkbox" id="pivotColumns" name="pivotColumns" value="response" checked="checked" />response
					<input type="checkbox" id="pivotColumns" name="pivotColumns" value="responsefile" checked="checked" />response file
				</div>
				<input type="hidden" name="observation" id="observation" value="3" />
				<input type="hidden" name="pivot" id="pivot" value="2" />
				<input type="hidden" name="operation" id="operation" />
				
				<xsl:element name="input">
					<xsl:attribute name="type">hidden</xsl:attribute>
					<xsl:attribute name="id">questionId</xsl:attribute>
					<xsl:attribute name="name">questionId</xsl:attribute>
					<xsl:attribute name="value"></xsl:attribute>
				</xsl:element>
				
			<table width="95%" align="left" border="0" cellpadding="0"
				cellspacing="0">
				<tr>
					
					<th style="text-align: left" colspan="2">
						Assessment
					</th>
					<th style="text-align: left">
						Course Title
					</th>
					<th	style="text-align: left">
						Instructor[s]
					</th>
					<th	style="text-align: left">
						Organization[s]
					</th>
					<th	style="text-align: left">
						Keywords
					</th>
				</tr>

				<xsl:for-each select="resultset/document">
					<xsl:sort select="./param[@name='assessment/@id']" />
					<xsl:variable name="divId">
						<xsl:text>ajaxDiv</xsl:text><xsl:value-of select="./docid" />
					</xsl:variable>
					<tr valign="top" class="subpanel">
						<xsl:attribute name="class">
			               <xsl:choose>
			                 <xsl:when test="position() mod 2 = 1">rowodd</xsl:when>
			                 <xsl:when test="position() mod 2 = 0">roweven</xsl:when>
			               </xsl:choose>
			             </xsl:attribute>

						<td class="text_plain" nowrap="nowrap">
						
						<xsl:if test="count(./param[@name='dataset/dataTable/entityName']) = 0" >
							No Data
						</xsl:if>	
						<xsl:if test="count(./param[@name='dataset/dataTable/entityName']) >= 1" >
						
							<script type="text/javascript" language="Javascript">
								<![CDATA[
									//what are our options?
									if (isInCart("]]><xsl:value-of select="./docid" /><![CDATA[")) {
										showDiv("]]><xsl:value-of select="$divId" /><![CDATA[remove");
										hideDiv("]]><xsl:value-of select="$divId" /><![CDATA[add");
									}
									else {
										showDiv("]]><xsl:value-of select="$divId" /><![CDATA[add");
										hideDiv("]]><xsl:value-of select="$divId" /><![CDATA[remove");
									}
								]]>	
							</script>
							<div style="display:none;">
								<xsl:attribute name="id">
									<xsl:value-of select="$divId" />
									<xsl:text>add</xsl:text>
								</xsl:attribute>
								<a>
									<xsl:attribute name="href">javascript:{}</xsl:attribute>
									<xsl:attribute name="onclick"><![CDATA[javascript:editCart('cartForm', 'add',"]]><xsl:value-of select="./docid" /><![CDATA[")]]></xsl:attribute>
									<img border="none">
										<xsl:attribute name="src">
											<xsl:value-of select="$contextURL" />
											<xsl:text>/style/skins/first/images/cart_plus_sm.png</xsl:text>
										</xsl:attribute>
									</img>
								</a>
							</div>
							<div style="display:none;">
								<xsl:attribute name="id">
									<xsl:value-of select="$divId" />
									<xsl:text>remove</xsl:text>
								</xsl:attribute>
								<a>
									<xsl:attribute name="href">javascript:{}</xsl:attribute>
									<xsl:attribute name="onclick"><![CDATA[javascript:editCart('cartForm', 'remove',"]]><xsl:value-of select="./docid" /><![CDATA[")]]></xsl:attribute>
									<!--  <xsl:text>Remove </xsl:text> -->
									<img border="none">
										<xsl:attribute name="src">
											<xsl:value-of select="$contextURL" />
											<xsl:text>/style/skins/first/images/cart_minus_sm.png</xsl:text>
										</xsl:attribute>
									</img>
								</a>
							</div>

							<input type="hidden" name="docid">
								<xsl:attribute name="value">
									<xsl:value-of select="./docid" />
								</xsl:attribute>
							</input>
							
							</xsl:if>
							<xsl:if test="count(./param[@name='dataset/dataTable/entityName']) >= 2" >
								<!-- demographic data is present -->
								<img border="none">
									<xsl:attribute name="src">
										<xsl:value-of select="$contextURL" />
										<xsl:text>/style/skins/first/images/user.gif</xsl:text>
									</xsl:attribute>
								</img>
							</xsl:if>
							
						</td>
						<td>
							
							<a>
								<xsl:attribute name="href">javascript:submitform('read','<xsl:value-of select="./docid" />',document.readForm)</xsl:attribute>
								<xsl:text>&#187;&#160;</xsl:text>
								<xsl:value-of select="./param[@name='assessment/title']" />
							</a>
							(<xsl:value-of select="./docid" />)
							
							<br />
							
							<!--include question ids here -->
							<xsl:for-each select="./param[@name='assessmentItems/assessmentItem/assessmentItemId']" >
								<input type="hidden">
									<xsl:attribute name="name">
										<xsl:value-of select="../docid" />
									</xsl:attribute>
									<xsl:attribute name="value">
										<xsl:value-of select="." />
									</xsl:attribute>
								</input>
							</xsl:for-each>
							
							<!-- demographic data - present or not? -->
							<input type="hidden">
								<xsl:attribute name="name">
									<xsl:value-of select="./docid" />
									<xsl:text>demographicData</xsl:text>
								</xsl:attribute>
								<xsl:attribute name="value">
									<xsl:choose>	
										<xsl:when test="count(./param[@name='dataset/dataTable/entityName']) >= 2" >
											<xsl:text>true</xsl:text>
										</xsl:when>
										<xsl:otherwise>
											<xsl:text></xsl:text>
										</xsl:otherwise>
									</xsl:choose>
								</xsl:attribute>
							</input>
							
							<br/>
														
						</td>

						<td class="text_plain">
							<xsl:for-each
								select="./param[@name='lom/general/title/string']">
								<xsl:value-of select="." />
								<br />
							</xsl:for-each>
						</td>
						<td class="text_plain">
							<xsl:for-each
								select="./param[@name='individualName/surName']">
								<xsl:value-of select="." />
								<br />
							</xsl:for-each>
						</td>
						<td class="text_plain">
							<xsl:for-each
								select="./param[@name='organizationName']">
								<xsl:value-of select="." />
								<br />
							</xsl:for-each>
						</td>

						<td class="text_plain">
							<xsl:for-each
								select="./param[@name='keyword']">
								<xsl:value-of select="." />
								<br />
							</xsl:for-each>
							<xsl:for-each
								select="./param[@name='lom/general/keyword/string']">
								<xsl:value-of select="." />
								<br />
							</xsl:for-each>

						</td>

					</tr>
					
					<tr>
						<td colspan="6">
							Details 
							<a>
								<xsl:attribute name="href">javascript:read('readForm', '<xsl:value-of select="./docid" />', '<xsl:value-of select="$divId" />')</xsl:attribute>
								<img border="none">
									<xsl:attribute name="src">
										<xsl:value-of select="$contextURL" />
										<xsl:text>/style/images/next.gif</xsl:text>
									</xsl:attribute>
								</img>
							</a>
							/
							<a>
								<xsl:attribute name="href">javascript:hideDiv('<xsl:value-of select="$divId" />')</xsl:attribute>
								<img border="none">
									<xsl:attribute name="src">
										<xsl:value-of select="$contextURL" />
										<xsl:text>/style/images/previous.gif</xsl:text>
									</xsl:attribute>
								</img>
							</a>
							
						</td>	
					</tr>
					
					<tr>
						<td colspan="6">
							<div style="display:none;">
								<xsl:attribute name="id">
									<xsl:value-of select="$divId" />
								</xsl:attribute>
								Assessment details...
							</div>
						</td>	
					</tr>
					
					<tr class="searchresultsdivider">
						<td colspan="6">
							<img
								src="{$contextURL}/style/skins/default/images/transparent1x1.gif"
								width="1" height="1" />
						</td>
					</tr>

				</xsl:for-each>
				
			</table>
			
			</form>

		</xsl:if>
	</xsl:template>

</xsl:stylesheet>