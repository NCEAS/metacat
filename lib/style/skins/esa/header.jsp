<%@ page    language="java" %>
<%@ page import="edu.ucsb.nceas.metacat.properties.SkinPropertyService" %>
<%
	/**
	 * '$RCSfile$'
	 * Copyright: 2008 Regents of the University of California and the
	 *             National Center for Ecological Analysis and Synthesis
	 * '$Author$'
	 * '$Date$'
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
%>

<%
	String esaHome = SkinPropertyService.getProperty("esa", "registryurl");
%>

<%@ include file="../../common/common-settings.jsp"%>
<%@ include file="../../common/configure-check.jsp"%>

<html>
<head>
<title>header.gif</title>
<meta http-equiv="Content-Type" content="text/html;">
<!--Please note that in order for the "roll overs" to exactly swap they need to be created in conjunction with the slices. Slices must be in place when exporting the individual *.gif file to be used with the swap image behavior. I found it helpful to use the exact slice that was used for creating the top "img" file. This was done by moving the slice down, changing the orginal img (such as the text color), then moving the slice back up before exporting the individual *.gif type of file to be used as a swap image file. It might be necessary after exporting the individual swap image file to return the top img file (such as changing the text color back) before exporting the final html file. Export with File name: HTML and Images, Save as type: HTML and Images, HTML: Export HTML File, Slices: Export Slices. I included areas withou slices and Put images in a subfolder titled ESAHeaderSlices. Before the final export the swap image *.gif files were placed in the ESAHeaderSlices/RollOvers directory.
cjbwdish -->
<script language="JavaScript">
<!--
function MM_findObj(n, d) { //v4.01
  var p,i,x;  if(!d) d=document; if((p=n.indexOf("?"))>0&&parent.frames.length) {
    d=parent.frames[n.substring(p+1)].document; n=n.substring(0,p);}
  if(!(x=d[n])&&d.all) x=d.all[n]; for (i=0;!x&&i<d.forms.length;i++) x=d.forms[i][n];
  for(i=0;!x&&d.layers&&i<d.layers.length;i++) x=MM_findObj(n,d.layers[i].document);
  if(!x && d.getElementById) x=d.getElementById(n); return x;
}
function MM_swapImage() { //v3.0
  var i,j=0,x,a=MM_swapImage.arguments; document.MM_sr=new Array; for(i=0;i<(a.length-2);i+=3)
   if ((x=MM_findObj(a[i]))!=null){document.MM_sr[j++]=x; if(!x.oSrc) x.oSrc=x.src; x.src=a[i+2];}
}
function MM_swapImgRestore() { //v3.0
  var i,x,a=document.MM_sr; for(i=0;a&&i<a.length&&(x=a[i])&&x.oSrc;i++) x.src=x.oSrc;
}

function MM_preloadImages() { //v3.0
 var d=document; if(d.images){ if(!d.MM_p) d.MM_p=new Array();
   var i,j=d.MM_p.length,a=MM_preloadImages.arguments; for(i=0; i<a.length; i++)
   if (a[i].indexOf("#")!=0){ d.MM_p[j]=new Image; d.MM_p[j++].src=a[i];}}
}

function loginStatus(){
 	var httpRequest=false;
        /*@cc_on @*/
        /*@if (@_jscript_version >= 5)
        try {
            httpRequest = new ActiveXObject("Msxml2.XMLHTTP");
        } catch (e) {
            try {
                httpRequest = new ActiveXObject("Microsoft.XMLHTTP");
            } catch (E) {
                httpRequest = false;
            }
        }
        @end @*/

        try{
        	if (!httpRequest && typeof XMLHttpRequest!='undefined') {
           		httpRequest = new XMLHttpRequest();
        	}

        	if(!httpRequest){
                	exit(0);
        	}

        	httpRequest.open("POST", "<%=SERVLET_URL%>?action=getloggedinuserinfo", true);
        	httpRequest.setRequestHeader("Content-Type", "text/xml");
        	var stringToSend = "action=getloggedinuserinfo";
        	httpRequest.onreadystatechange=function() {
                if (httpRequest.readyState==4) {
					var response = httpRequest.responseText;
					var login_block = document.getElementById('login_block');
					var submission_block = document.getElementById('submission_block');
					var userSearch = document.getElementById('userSearch');
					var modSearch = document.getElementById('modSearch');
					login_block.innerHTML="";
					submission_block.innerHTML="";
					if(response.indexOf("public") > 0 || response.indexOf("null") > 0) {
						login_block.innerHTML = "<a href='<%=CGI_URL%>/register-dataset.cgi?cfg=esa&stage=loginform' target='_top' onMouseOut='MM_swapImgRestore();' onMouseOver='MM_swapImage(\"Login\",\"\",\"ESAHeaderSlices/RollOvers/LoginR.gif\",1);'><img name='Login' src='ESAHeaderSlices/Login.gif' width='62' height='18' border='0' title='Login' alt='Login'></a>";
						submission_block.innerHTML = "<a href='<%=CGI_URL%>/register-dataset.cgi?cfg=esa&stage=loginform&submission=true' target='_top' onMouseOut='MM_swapImgRestore();' onMouseOver='MM_swapImage(\"MySubmissions\",\"\",\"ESAHeaderSlices/RollOvers/MySubmissionsR.gif\",1);'><img name='MySubmissions' src='ESAHeaderSlices/MySubmissions.gif' width='114' height='18' border='0' title='My Submissions - Before you can view your submissions you must login.' alt='My Submissions'></a>";
					} else {
						login_block.innerHTML = "<a href='<%=CGI_URL%>/register-dataset.cgi?cfg=esa&stage=logout' target='_top' onMouseOut='MM_swapImgRestore();' onMouseOver='MM_swapImage(\"Logout\",\"\",\"ESAHeaderSlices/RollOvers/LogoutR.gif\",1);'><img name='Logout' src='ESAHeaderSlices/Logout.gif' width='62' height='18' border='0' title ='Logout' alt='Logout'></a>";
					
						if(response.indexOf("isModerator") > 0) {
							submission_block.innerHTML = "<a onClick='modSearch.submit()' target='_top' onMouseOut='MM_swapImgRestore();' onMouseOver='MM_swapImage(\"ViewSubmissions\",\"\",\"ESAHeaderSlices/RollOvers/ViewSubmissionsR.gif\",1);'><img name='ViewSubmissions' src='ESAHeaderSlices/ViewSubmissions.gif' width='114' height='18' border='0' title='View Submissions' alt='View Submissions'></a>";
						} else {
							submission_block.innerHTML = "<a onClick='userSearch.submit()' target='_top' onMouseOut='MM_swapImgRestore();' onMouseOver='MM_swapImage(\"MySubmissions\",\"\",\"ESAHeaderSlices/RollOvers/MySubmissionsR.gif\",1);'><img name='MySubmissions' src='ESAHeaderSlices/MySubmissions.gif' width='114' height='18' border='0' title='My Submissions - Before you can view your submissions you must login.' alt='My Submissions'></a>";
						}
					}
				}
			}
			httpRequest.send(stringToSend);
		} catch (e) {
        	alert("caught an exception: " + e
                 + " \nresponse was: \n" + httpRequest.responseText);
	}     
}

//-->
</script>
</head>
<body bgcolor="#ffffff" onLoad="MM_preloadImages('ESAHeaderSlices/RollOvers/ESARegistryR.gif','ESAHeaderSlices/RollOvers/RegisterDataR.gif','ESAHeaderSlices/RollOvers/SearchForDataR.gif','ESAHeaderSlices/RollOvers/MySubmissionsR.gif','ESAHeaderSlices/RollOvers/LogoutR.gif');loginStatus();">
<table border="0" cellpadding="0" cellspacing="0" width="703">
<!-- fwtable fwsrc="ESAHeaderDocsb.png" fwbase="header.gif" fwstyle="Dreamweaver" fwdocid = "1407099340" fwnested="0" -->
  <tr>
   <td><img src="ESAHeaderSlices/spacer.gif" width="19" height="1" border="0" alt=""></td>
   <td><img src="ESAHeaderSlices/spacer.gif" width="143" height="1" border="0" alt=""></td>
   <td><img src="ESAHeaderSlices/spacer.gif" width="7" height="1" border="0" alt=""></td>
   <td><img src="ESAHeaderSlices/spacer.gif" width="99" height="1" border="0" alt=""></td>
   <td><img src="ESAHeaderSlices/spacer.gif" width="15" height="1" border="0" alt=""></td>
   <td><img src="ESAHeaderSlices/spacer.gif" width="82" height="1" border="0" alt=""></td>
   <td><img src="ESAHeaderSlices/spacer.gif" width="105" height="1" border="0" alt=""></td>
   <td><img src="ESAHeaderSlices/spacer.gif" width="43" height="1" border="0" alt=""></td>
   <td><img src="ESAHeaderSlices/spacer.gif" width="16" height="1" border="0" alt=""></td>
   <td><img src="ESAHeaderSlices/spacer.gif" width="55" height="1" border="0" alt=""></td>
   <td><img src="ESAHeaderSlices/spacer.gif" width="62" height="1" border="0" alt=""></td>
   <td><img src="ESAHeaderSlices/spacer.gif" width="57" height="1" border="0" alt=""></td>
   <td><img src="ESAHeaderSlices/spacer.gif" width="1" height="1" border="0" alt=""></td>
  </tr>

  <tr>
   <td colspan="12"><img name="header_r1_c1" src="ESAHeaderSlices/header_r1_c1.gif" width="703" height="32" border="0" alt=""></td>
   <td><img src="ESAHeaderSlices/spacer.gif" width="1" height="32" border="0" alt=""></td>
  </tr>
  <tr>
   <td rowspan="4"><img name="header_r2_c1" src="ESAHeaderSlices/header_r2_c1.gif" width="19" height="118" border="0" alt=""></td>
   <td><a href="http://www.esa.org/" target="_top"><img name="ESAHomeLogo" src="ESAHeaderSlices/ESAHomeLogo.gif" width="143" height="54" border="0" title="Ecological Society of America" alt="ESA Home"></a></td>
   <td rowspan="2" colspan="10"><img name="header_r2_c3" src="ESAHeaderSlices/header_r2_c3.gif" width="541" height="71" border="0" alt=""></td>
   <td><img src="ESAHeaderSlices/spacer.gif" width="1" height="54" border="0" alt=""></td>
  </tr>
  <tr>
   <td rowspan="3"><img name="header_r3_c2" src="ESAHeaderSlices/header_r3_c2.gif" width="143" height="64" border="0" alt=""></td>
   <td><img src="ESAHeaderSlices/spacer.gif" width="1" height="17" border="0" alt=""></td>
  </tr>
  <tr>
   <td rowspan="2"><img name="header_r4_c3" src="ESAHeaderSlices/header_r4_c3.gif" width="7" height="47" border="0" alt=""></td>
   <td><a href="<%=esaHome%>" target="_top" onMouseOut="MM_swapImgRestore();" onMouseOver="MM_swapImage('ESARegistry','','ESAHeaderSlices/RollOvers/ESARegistryR.gif',1);"><img name="ESARegistry" src="ESAHeaderSlices/ESARegistry.gif" width="99" height="18" border="0" title="ESA Registry" alt="ESA Registry"></a></td>
   <td colspan="2"><a href="<%=CGI_URL%>/register-dataset.cgi?cfg=esa" target="_top" onMouseOut="MM_swapImgRestore();" onMouseOver="MM_swapImage('RegisterData','','ESAHeaderSlices/RollOvers/RegisterDataR.gif',1);"><img name="RegisterData" src="ESAHeaderSlices/RegisterData.gif" width="97" height="18" border="0" title="Register Data" alt="Register Data"></a></td>
   <td><a href="<%=esaHome%>#search" target="_top" onMouseOut="MM_swapImgRestore();" onMouseOver="MM_swapImage('SearchForData','','ESAHeaderSlices/RollOvers/SearchForDataR.gif',1);"><img name="SearchForData" src="ESAHeaderSlices/SearchForData.gif" width="105" height="18" border="0" title="Search for Data" alt="Search for Data"></a></td>
   <td colspan="3"><div style="cursor:pointer;cursor: hand;" id="submission_block"><a href="#" onMouseOut="MM_swapImgRestore();" onMouseOver="MM_swapImage('MySubmissions','','ESAHeaderSlices/RollOvers/MySubmissionsR.gif',1);"><img name="MySubmissions" src="ESAHeaderSlices/MySubmissions.gif" width="114" height="18" border="0" title="My Submissions - Before you can view your submissions you must login." alt="My Submissions"></a></div></td>
   <td><div id="login_block"><a href="<%=CGI_URL%>/register-dataset.cgi?cfg=esa&stage=loginform" onMouseOut="MM_swapImgRestore();" onMouseOver="MM_swapImage('Login','','ESAHeaderSlices/RollOvers/LoginR.gif',1);"><img name="Login" src="ESAHeaderSlices/Login.gif" width="62" height="18" border="0" title="Login" alt="Login"></a></div></td>
   <td rowspan="2"><img name="header_r4_c12" src="ESAHeaderSlices/header_r4_c12.gif" width="57" height="47" border="0" alt=""></td>
   <td><img src="ESAHeaderSlices/spacer.gif" width="1" height="18" border="0" alt=""></td>
  </tr>
  <tr>
   <td colspan="8"><img name="header_r5_c4" src="ESAHeaderSlices/header_r5_c4.gif" width="477" height="29" border="0" alt=""></td>
   <td><img src="ESAHeaderSlices/spacer.gif" width="1" height="29" border="0" alt=""></td>
  </tr>
</table>
<form id="modSearch" name="modSearch" action="<%=SERVLET_URL%>" method="post" target="_top">
       <input type="hidden" name="action" value="squery"/>
       <input type="hidden" name="qformat" value="esa"/>
       <input type="hidden" name="enableediting" value="true"/>
       <input type="hidden" name="query" value="<pathquery><querytitle>Moderator-Search</querytitle><returndoctype>eml://ecoinformatics.org/eml-2.1.1</returndoctype><returndoctype>eml://ecoinformatics.org/eml-2.1.0</returndoctype><returndoctype>eml://ecoinformatics.org/eml-2.0.1</returndoctype><returndoctype>eml://ecoinformatics.org/eml-2.0.0</returndoctype><returndoctype>-//ecoinformatics.org//eml-dataset-2.0.0beta6//EN</returndoctype><returndoctype>-//ecoinformatics.org//eml-dataset-2.0.0beta4//EN</returndoctype><returndoctype>-//NCEAS//resource//EN</returndoctype><returndoctype>-//NCEAS//eml-dataset//EN</returndoctype><returnfield>originator/individualName/surName</returnfield><returnfield>originator/individualName/givenName</returnfield><returnfield>creator/individualName/surName</returnfield><returnfield>creator/individualName/givenName</returnfield><returnfield>originator/organizationName</returnfield><returnfield>creator/organizationName</returnfield><returnfield>dataset/title</returnfield><returnfield>keyword</returnfield><querygroup operator='INTERSECT'><queryterm searchmode='not-contains' casesensitive='false'><value>public</value><pathexpr>access/allow/principal</pathexpr></queryterm><queryterm searchmode='not-contains' casesensitive='false'><value>Revision Requested</value><pathexpr>additionalMetadata/moderatorComment</pathexpr></queryterm></querygroup></pathquery>"/>
</form>
<form id="userSearch" name="userSearch" action="<%=SERVLET_URL%>" method="post" target="_top">
       <input type="hidden" name="action" value="squery"/>
       <input type="hidden" name="qformat" value="esa"/>
       <input type="hidden" name="enableediting" value="true"/>
       <input type="hidden" name="query" value="<pathquery><querytitle>Moderator-Search</querytitle><returndoctype>eml://ecoinformatics.org/eml-2.1.1</returndoctype><returndoctype>eml://ecoinformatics.org/eml-2.1.0</returndoctype><returndoctype>eml://ecoinformatics.org/eml-2.0.1</returndoctype><returndoctype>eml://ecoinformatics.org/eml-2.0.0</returndoctype><returndoctype>-//ecoinformatics.org//eml-dataset-2.0.0beta6//EN</returndoctype><returndoctype>-//ecoinformatics.org//eml-dataset-2.0.0beta4//EN</returndoctype><returndoctype>-//NCEAS//resource//EN</returndoctype><returndoctype>-//NCEAS//eml-dataset//EN</returndoctype><returnfield>originator/individualName/surName</returnfield><returnfield>originator/individualName/givenName</returnfield><returnfield>creator/individualName/surName</returnfield><returnfield>creator/individualName/givenName</returnfield><returnfield>originator/organizationName</returnfield><returnfield>creator/organizationName</returnfield><returnfield>dataset/title</returnfield><returnfield>keyword</returnfield><querygroup operator='INTERSECT'><queryterm searchmode='not-contains' casesensitive='false'><value>public</value><pathexpr>access/allow/principal</pathexpr></queryterm></querygroup></pathquery>"/>
</form>
</body>
</html>
