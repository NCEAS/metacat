<%
  String docid = request.getParameter("docid");
  String status = request.getParameter("status");
%>
<html>
<head>
  <link rel="stylesheet" type="text/css" href="style/skins/sms/sms.css"/>
  <script src="style/skins/sms/jquery-1.2.6.min.js"></script>
  <script src="style/skins/sms/util.js"></script>
  <script type="text/javascript">
    
  </script>
  
  
  <title>Semantic Mediation System</title>
  
</head>
<%

  if(status != null && status.equals("success"))
  {
    out.print("<body onload=\"javascript:checkPublicAccess('" + docid + "')\">");
  }
  else
  {
    out.print("<body onload=\"javascript:checkLogin()\">");
  }


%>
  
  <div style="width: 700px; margin: 0px auto; border:0px; padding:0px;">
  <img style="position:relative; top: 15px;" src="style/skins/sms/sms-page-top.png"/>
  <div id="page">
    <div style="width:650px; border:5px; margin:0px auto;">
      <h1 style="text-align:center">SEEK Semantic Mediation Tools</h1>
    </div>
    <div style="width: 650px; border: 5px; margin: 0px auto; background: grey">
      <div id="loginheaderdiv" style="padding: 3px;">
        <h2 style="text-align:center" id="loginheader">Please Login</h2>
      </div>
      <!--login div-->
      <div id="loginformdiv" style="padding:20px;">
      <form action="metacat" name="loginform" method="POST">
        <div style="width:300px; margin:0px auto; height: 150px;">
        <table>
          <tr>
            <td>Username:</td><td><input id="un" type="text" name="username"/></td>
          </tr>
          <tr>
            <td>Organization:</td><td><input id="org" type="text" name="organization"/></td>
          </tr>
          <tr>
            <td>Password:</td><td><input id="pw" type="password" name="password"/></td>
          </tr>
          <tr>
            <td><a href="javascript:login();" >[Login]</a></td>
          </tr>
        </table>
        
      </form>
      </div>
      </div>
      
      <!--main part of the page after logging in-->
      <div id="maindiv" style="display:none; width:625px; margin:0px auto; padding: 10px;">
        <a href="javascript:showSearchPane()"><img id="searchtabimg" src="style/skins/sms/search-tab.png"/></a><img id="searchtabimgsel" src="style/skins/sms/search-tab-selected.png"/>
        <a href="javascript:showUploadPane()"><img id="uploadtabimg" src="style/skins/sms/upload-tab.png"/></a><img id="uploadtabimgsel" src="style/skins/sms/upload-tab-selected.png"/>
        <a href="javascript:showBrowsePane()"><img id="browsetabimg" src="style/skins/sms/browse-tab.png"/></a><img id="browsetabimgsel" src="style/skins/sms/browse-tab-selected.png"/>
          
          <!--search panel-->
        <div id="searchdiv" style="background:grey; border:1px solid; width: 99%;">
          <div style="padding: 5px;">
            <h4 style="text-align:center">Semantic Search</h4>
            <form>
              <table>
                <tr>
                  <td>Search:</td><td><input id="searchtextbox" type="text" name="anytext"/></td>
                </tr>
                <tr>
                  <td><a href="javascript:search()">[Search]</a></td><td>&nbsp;</td>
                </tr>
              </table>
            </form>
          </div>
          
          <div id="searchresultdiv" style="position: relative; left: 3px; padding: 10px; width: 595px; ">
            <div id="searchresulttable">
              <!--this is filled in via an AJAX call.  see search()-->
            </div>
          </div>
        </div>
          
          
          <!--upload panel-->
        <div id="uploaddiv" style="background:grey; border:1px solid; width: 99%;">
          <div style="padding:5px;">
            <h4 style="text-align:center">Upload an ontology or data package</h4>
            <form action="metacat" name="uploadform" method="POST" enctype="multipart/form-data">
              <input type="hidden" name="action" value="insertmultipart"/>
              <input type="hidden" name="qformat" value="sms"/>
              <input type="hidden" name="updateXMLNS" value="true"/>
              <table>
                <tr>
                  <td>File:<input type="file" id="datafile" name="datafile"/></td>
                </tr>
                <tr>
                  <!--<td>DocId:</td><td><input id="docidtextfield" type="text" name="docid" readonly="true"/></td>-->
                  <td>
                      Identifier Prefix: <input style="display:inline" id="docidfamily" type="text" value="sms" size="3"/>
                      <input style="display:inline" id="docidnumber" type="hidden" size="1"/>
                      <input style="display:inline" id="docidrevision" type="hidden" size="1"/>
                      <input style="display:inline" id="docid" type="hidden" name="docid" value="x"/>
                      <span style="font-size:70%"><a href="javascript:getId()">[Check Id]</a></span>
                  </td>
                </tr>
                <tr>
                  <td>
                    <a href="javascript:uploadfile()" >[Upload]</a>
                    <!--<input type="submit"/>-->
                  </td>
                </tr>
              </table>
            </form>
            <div id="uploadstatus" style="width:312px"> 
            <%
              if(status != null && status.equals("success"))
              {
                out.print("Your file was successfully uploaded with id <a href=\"http://linus.nceas.ucsb.edu/sms/metacat?docid=" + docid 
                + "&action=read&qformat=sms\">" + docid + "</a>.  " +  
                "<a href=\"javascript:makepublic('" + docid + "')\">Click here</a> to make this document publicly readable.");
              }
              else if(status == null)
              {
                //do nothing
              }
              else
              {
                out.print("Sorry, your file upload failed.  Try logging in again.");
              }
              
              %>
            </div>
          </div>
        </div>
      
        <!--browse panel-->
        <div id="browseresultdiv" style="position: relative; left: 3px; border: 1px solid ; padding: 10px; width: 595px; ">
          <div id="browseresulttable">
            <!--this is filled in via an AJAX call.  see showDatasets()-->
          </div>
        </div>
      </div>
      
    </div>
  </div>
  <img id="bottomimg" src="style/skins/sms/sms-page-bottom.png" style="position:relative; bottom: 0px;"/>
  </div>
</body>
</html>
