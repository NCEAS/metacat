<%@page contentType="text/html"%>
<%@page pageEncoding="UTF-8"%>
<%@page import="edu.ucsb.nceas.metacat.util.MetacatUtil"%>
<%@ include file="../../common/common-settings.jsp"%>
<%@ include file="../../common/configure-check.jsp"%>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">

<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>ESAHeader.gif</title>
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
        //-->
        </script>
    </head>
    <body style="margin-left:0px;" bgcolor="#ffffff" onLoad="MM_preloadImages('ESAHeaderSlices/ESAHome2_f2.gif','ESAHeaderSlices/RollOvers/ESAHomeR.gif','ESAHeaderSlices/RollOvers/ESARegistryR.gif','ESAHeaderSlices/RollOvers/ESANewDataSetR.gif','ESAHeaderSlices/RollOvers/ESASearchR.gif','ESAHeaderSlices/RollOvers/ESALoginR.gif','ESAHeaderSlices/RollOvers/ESADocPendR.gif');">
        <table border="0" cellpadding="0" cellspacing="0" width="703">
            <!-- fwtable fwsrc="ESAHeaderDocs2.png" fwbase="ESAHeader.gif" fwstyle="Dreamweaver" fwdocid = "1407099340" fwnested="0" -->
            <tr>
                <td><img src="ESAHeaderSlices/spacer.gif" width="19" height="1" border="0" alt=""></td>
                <td><img src="ESAHeaderSlices/spacer.gif" width="143" height="1" border="0" alt=""></td>
                <td><img src="ESAHeaderSlices/spacer.gif" width="78" height="1" border="0" alt=""></td>
                <td><img src="ESAHeaderSlices/spacer.gif" width="43" height="1" border="0" alt=""></td>
                <td><img src="ESAHeaderSlices/spacer.gif" width="59" height="1" border="0" alt=""></td>
                <td><img src="ESAHeaderSlices/spacer.gif" width="154" height="1" border="0" alt=""></td>
                <td><img src="ESAHeaderSlices/spacer.gif" width="17" height="1" border="0" alt=""></td>
                <td><img src="ESAHeaderSlices/spacer.gif" width="85" height="1" border="0" alt=""></td>
                <td><img src="ESAHeaderSlices/spacer.gif" width="57" height="1" border="0" alt=""></td>
                <td><img src="ESAHeaderSlices/spacer.gif" width="48" height="1" border="0" alt=""></td>
                <td><img src="ESAHeaderSlices/spacer.gif" width="1" height="1" border="0" alt=""></td>
            </tr>
            
            <tr>
                <td colspan="10"><img name="ESAHeader_r1_c1" src="ESAHeaderSlices/ESAHeader_r1_c1.gif" width="703" height="32" border="0" alt=""></td>
                <td><img src="ESAHeaderSlices/spacer.gif" width="1" height="32" border="0" alt=""></td>
            </tr>
            <tr>
                <td rowspan="6"><img name="ESAHeader_r2_c1" src="ESAHeaderSlices/ESAHeader_r2_c1.gif" width="19" height="118" border="0" alt=""></td>
                <td><a href="http://www.esa.org/"><img name="ESAHomeLogo" src="ESAHeaderSlices/ESAHomeLogo.gif" width="143" height="54" border="0" alt="ESA Home"></a></td>
                <td rowspan="2" colspan="8"><img name="ESAHeader_r2_c3" src="ESAHeaderSlices/ESAHeader_r2_c3.gif" width="541" height="67" border="0" alt=""></td>
                <td><img src="ESAHeaderSlices/spacer.gif" width="1" height="54" border="0" alt=""></td>
            </tr>
            <tr>
                <td rowspan="5"><img name="ESAHeader_r3_c2" src="ESAHeaderSlices/ESAHeader_r3_c2.gif" width="143" height="64" border="0" alt=""></td>
                <td><img src="ESAHeaderSlices/spacer.gif" width="1" height="13" border="0" alt=""></td>
            </tr>
            <tr>
                <td><a href="http://www.esa.org/" target="_top" onMouseOut="MM_swapImgRestore();" onMouseOver="MM_swapImage('ESAHome2','','ESAHeaderSlices/ESAHome2_f2.gif','ESAHome2','','ESAHeaderSlices/RollOvers/ESAHomeR.gif',1);"><img name="ESAHome2" src="ESAHeaderSlices/ESAHome2.gif" width="78" height="17" border="0" alt="ESA Home"></a></td>
                <td colspan="2"><a href="./index.jsp" target="_top" onMouseOut="MM_swapImgRestore();" onMouseOver="MM_swapImage('ESARegistry2','','ESAHeaderSlices/RollOvers/ESARegistryR.gif',1);"><img name="ESARegistry2" src="ESAHeaderSlices/ESARegistry2.gif" width="102" height="17" border="0" alt="Registry Home"></a></td>
                <td><a href="<%= CGI_URL %>/register-dataset.cgi?cfg=esa" target="_top" onMouseOut="MM_swapImgRestore();" onMouseOver="MM_swapImage('ESARegisterNewData','','ESAHeaderSlices/RollOvers/ESANewDataSetR.gif',1);"><img name="ESARegisterNewData" src="ESAHeaderSlices/ESARegisterNewData.gif" width="154" height="17" border="0" alt="Register a New Data Set"></a></td>
                <td colspan="2"><a href="./index.jsp#search" target="_top" onMouseOut="MM_swapImgRestore();" onMouseOver="MM_swapImage('ESASearch','','ESAHeaderSlices/RollOvers/ESASearchR.gif',1);"><img name="ESASearch" src="ESAHeaderSlices/ESASearch.gif" width="102" height="17" border="0" alt="Search for Data"></a></td>
                <td><a href="#" target="_top" onMouseOut="MM_swapImgRestore();" onMouseOver="MM_swapImage('ESALogin','','ESAHeaderSlices/RollOvers/ESALoginR.gif',1);"><img name="ESALogin" src="ESAHeaderSlices/ESALogin.gif" width="57" height="17" border="0" alt="Login"></a></td>
                <td rowspan="4"><img name="ESAHeader_r4_c10" src="ESAHeaderSlices/ESAHeader_r4_c10.gif" width="48" height="51" border="0" alt=""></td>
                <td><img src="ESAHeaderSlices/spacer.gif" width="1" height="17" border="0" alt=""></td>
            </tr>
            <tr>
                <td colspan="7"><img name="ESAHeader_r5_c3" src="ESAHeaderSlices/ESAHeader_r5_c3.gif" width="493" height="3" border="0" alt=""></td>
                <td><img src="ESAHeaderSlices/spacer.gif" width="1" height="3" border="0" alt=""></td>
            </tr>
            <tr>
                <td rowspan="2" colspan="2"><img name="ESAHeader_r6_c3" src="ESAHeaderSlices/ESAHeader_r6_c3.gif" width="121" height="31" border="0" alt=""></td>
                <td colspan="3"><a href="#" onMouseOut="MM_swapImgRestore();" onMouseOver="MM_swapImage('ESADocPend','','ESAHeaderSlices/RollOvers/ESADocPendR.gif',1);"><img name="ESADocPend" src="ESAHeaderSlices/ESADocPend.gif" width="230" height="20" border="0" alt="View Documents Pending Moderation"></a></td>
                <td rowspan="2" colspan="2"><img name="ESAHeader_r6_c8" src="ESAHeaderSlices/ESAHeader_r6_c8.gif" width="142" height="31" border="0" alt=""></td>
                <td><img src="ESAHeaderSlices/spacer.gif" width="1" height="20" border="0" alt=""></td>
            </tr>
            <tr>
                <td colspan="3"><img name="ESAHeader_r7_c5" src="ESAHeaderSlices/ESAHeader_r7_c5.gif" width="230" height="11" border="0" alt=""></td>
                <td><img src="ESAHeaderSlices/spacer.gif" width="1" height="11" border="0" alt=""></td>
            </tr>
        </table>
    </body>
</html>
