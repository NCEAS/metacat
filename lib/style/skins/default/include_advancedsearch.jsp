<%@ page     language="java" %>
<!--
/**
  *  '$RCSfile$'
  *      Authors:     Duane Costa
  *      Copyright:   2005 University of New Mexico and
  *                   Regents of the University of California and the
  *                   National Center for Ecological Analysis and Synthesis
  *      For Details: http://www.nceas.ucsb.edu/
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
  */
-->
<%@ include file="settings.jsp"%>
<%@ include file="session_vars.jsp"%>
<!-- *********************** START SEARCHBOX TABLE ************************* -->
<html>
<head>
  <title>Metacat Data Catalog Advanced Search Page</title>
  <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
  <link href="<%=STYLE_SKINS_URL%>/default/default.css" rel="stylesheet" type="text/css">

  <script language="javascript" 
    type="text/javascript" src="<%=STYLE_SKINS_URL%>/default/default.js">
  </script>

  <script language="javascript" type="text/javascript">

      function trim(stringToTrim) {
        return stringToTrim.replace(/^\s*/, '').replace(/\s*$/,'');
      }

      function submitRequest(form) {
        var canSearch = true;
        var x_range = document.map.get_x_range();
        var y_range = document.map.get_y_range();
        var x_index = x_range.indexOf(' ');
        var y_index = y_range.indexOf(' ');
        var x_length = x_range.length;
        var y_length = y_range.length;
        var x_lo = x_range.substring(0, x_index);
        var x_hi = x_range.substring(x_index + 1, x_length);
        var y_lo = y_range.substring(0, y_index);
        var y_hi = y_range.substring(y_index + 1, y_length);

        if (trim(form.subjectValue.value) == "" &&
            trim(form.creatorSurname.value) == "" &&
            trim(form.creatorOrganization.value) == "" &&
            trim(form.locationName.value) == "" &&
            (x_range == "-180.0 180.0") &&
            (y_range == "-90.0 90.0") &&            
            trim(form.taxon.value) == "" &&
            form.siteValue.value == "ALLSITES"
           ) {              
          canSearch = 
             confirm("Show *all* data in the catalog?\n(This may take some time!)");
        }

        if (canSearch) {        
          // Re-initialize the hidden form values prior to submitting
          form.northBound.value = "";
          form.southBound.value = "";
          form.eastBound.value = "";
          form.westBound.value = "";

          if ((x_range != "-180.0 180.0") || (y_range != "-90.0 90.0")) {     
            form.northBound.value = y_hi;
            form.southBound.value = y_lo;
            form.eastBound.value = x_hi;
            form.westBound.value = x_lo;
          }
          
          return(validateAdvancedSearchForm(form));
        }
        else {
          return false;
        }
      }
  </script>
</head>

<body leftmargin="0" topmargin="0" marginwidth="0" marginheight="0">
  <table width="750px" align="center" cellspacing="0" cellpadding="0" class="group group_border">
    <tr> 
     
      <th class="sectionheader">
        advanced search 
      </th>
      
    </tr>
    <tr> 
      <td>
        <table width="100%" border="0" cellpadding="0" cellspacing="0" 
          class="subpanel">
          <tr> 
            <td></td>
          </tr>
          <tr valign="baseline"> 
            <td>
    <form name="advancedSearchForm"
          method="POST"
          action="<%=STYLE_SKINS_URL%>/default/advancedsearchforward.jsp"
          onsubmit="return submitRequest(this)"
          target="_top">
      <table width="100%" border="0" cellpadding="0" cellspacing="0" >
        <tr>
          <th colspan="2">
            <h3>Subject</h3>
          </th>
        </tr>
        <tr>
          <td>&nbsp;</td>
          <td>&nbsp;</td>
        </tr>
        <tr>
          <td align="right" nowrap>
            <select name="subjectField">
              <option value="ALL">Subject</option>
              <option value="TITLE">Title Only</option>
              <option value="ABSTRACT">Abtract Only</option>
              <option value="KEYWORDS">Keywords Only</option>
            </select>
          </td>
          <td>
            <select name="subjectQueryType">
              <option value="0" selected="selected">contains</option>
              <option value="1">matches exactly</option>
              <option value="2">starts with</option>
              <option value="3">ends with</option>
            </select>
            <input type="text" name="subjectValue" value="">
            <input type="radio" name="subjectAllAny" value="0" checked="checked">All Terms
            <input type="radio" name="subjectAllAny" value="1">Any Term
          </td>
        </tr>
        <tr>
          <td>&nbsp;</td>
          <td>&nbsp;</td>
        </tr>
        
        <tr>
          <th colspan="2">
            <h3>Author</h3>
          </th>
        </tr>
        <tr>
          <td>&nbsp;</td>
          <td>&nbsp;</td>
        </tr>
        <tr>
          <td nowrap align="right">Individual's Last Name:
          </td>
          <td>
            <select name="creatorSurnameQueryType">
              <option value="0" selected="selected">contains</option>
              <option value="1">matches exactly</option>
              <option value="2">starts with</option>
              <option value="3">ends with</option></select>
            <input type="text" name="creatorSurname" value="">
          </td>
        </tr>
        <tr>
          <td>&nbsp;</td>
          <td>&nbsp;</td>
        </tr>
        <tr>
          <td align="right" nowrap>Organization:
          </td>
          <td>
            <select name="creatorOrganizationQueryType">
              <option value="0" selected="selected">contains</option>
              <option value="1">matches exactly</option>
              <option value="2">starts with</option>
              <option value="3">ends with</option></select>
            <input type="text" name="creatorOrganization" value="">
          </td>
        </tr>
        <tr>
          <td>&nbsp;</td>
          <td>&nbsp;</td>
        </tr>
        <tr>
          <th colspan="2">
            <h3>Spatial Criteria</h3>
          </th>
        </tr>
        <tr>
          <td>&nbsp;</td>
          <td>&nbsp;</td>
        </tr>
        <tr>
          <td align="right">Geographic Boundaries:</td>
        </tr>
        <tr>
          <td>&nbsp;</td>
          <td>
            <applet
              CODEBASE="<%= request.getContextPath() %>/LiveMap_30"
              CODE="LiveMap_30.class"
              ARCHIVE="LiveMap_30.jar"
              NAME="map" 
              MAYSCRIPT 
              width=500 
              height=160>
              <param name=base_image value="gifs/java_0_world_20k.jpg">
              <param name=img_x_domain value="-180 180">
              <param name=img_y_domain value="-90 90">
              <param name=toolType value="XY">
              <param name=tool_x_range value="-180 180">
              <param name=tool_y_range value="-90 90">
            </applet>
          </td>
        </tr>
        <tr>
          <td>&nbsp;</td>
          <td>
            <!--
             The underlying property value associated with checkbox
             should be of type boolean, and any value you specify 
             should correspond to one of the strings that indicate a 
             true value ("true", "yes", or "on").
            -->
            <input type="checkbox" name="boundaryContained" value="on">
            Dataset must be fully contained within boundaries
          </td>
        </tr>
        <tr>
          <td>&nbsp;</td>
          <td>&nbsp;</td>
        </tr>
        <tr>
          <td align="right">Geographic Place Name: 
          </td>
          <td><input type="text" name="locationName" value="">
          </td>
        </tr>
        <tr>
          <td>&nbsp;</td>
          <td>&nbsp;</td>
        </tr>
        <!-- Temporal Criteria section is disabled because Metacat is
             not able to search date ranges. An enhancement request has
             been entered in Bugzilla (see 
             http://bugzilla.ecoinformatics.org/show_bug.cgi?id=2084 )
        <tr>
          <td colspan="2">
            <h3>Temporal Criteria</h3>
          </td>
        </tr>
        <tr>
          <td>&nbsp;</td>
          <td>&nbsp;</td>
        </tr>
        <tr>
          <td align="right">
            <select name="dateField">
              <option value="ALL">All Dates</option>
              <option value="COLLECTION">Collection Date</option>
              <option value="PUBLICATION">Publication Date</option>
            </select>
          </td>
          <td colspan="2">
            <input type="text" name="startDate" maxlength="10" size="10" value="" style="font-family:monospace;">
            to
            <input type="text" name="endDate" maxlength="10" size="10" value="" style="font-family:monospace;">
          </td>
        </tr>
        <tr>
          <td>&nbsp;</td>
          <td>&nbsp;</td>
        </tr>
        <tr>
          <td align="right">Named Timescale:
            <select name="namedTimescaleQueryType">
              <option value="0" selected="selected">contains</option>
              <option value="1">matches exactly</option>
              <option value="2">starts with</option>
              <option value="3">ends with</option>
            </select>
          </td>
          <td>
            <input type="text" name="namedTimescale" value="">
          </td>
        </tr>
        <tr>
          <td>&nbsp;</td>
          <td>&nbsp;</td>
        </tr>
        <tr>
          <td colspan="2"><hr/></td>
        </tr>
        -->
        <tr>
          <th colspan="2">
            <h3>Taxonomic Criteria</h3>
          </th>
        </tr>
        <tr>
          <td>&nbsp;</td>
          <td>&nbsp;</td>
        </tr>
        <tr>
          <td align="right">Taxon:
          </td>
          <td>
            <select name="taxonQueryType">
              <option value="0" selected="selected">contains</option>
              <option value="1">matches exactly</option>
              <option value="2">starts with</option>
              <option value="3">ends with</option>
            </select>
            <input type="text" name="taxon" value="">
          </td>
        </tr>
        <tr>
          <td>&nbsp;</td>
          <td>&nbsp;
            <input type="hidden" name="siteValue" value="ALLSITES"/>
          </td>
        </tr>

<!--   
    Un-comment the following section to include an input box that allows the
    user to restrict the search to a specific LTER site. Obviously, this is
    probably useful only within LTER. If uncommenting this section, 
    be sure to comment-out the hidden siteValue input above.
-->
       
<!--
        <tr>
          <td colspan="2"><hr/></td>
        </tr>
        <tr>
          <td colspan="2">
            <h3>LTER Site</h3>
          </td>
        </tr>
        <tr>
          <td>&nbsp;</td>
          <td>&nbsp;</td>
        </tr>
        <tr>
          <td align="right">Limit search to LTER site:</td>
          <td>
            <select name="siteValue">
              <option value="ALLSITES">All LTER Sites</option>
              <option value="AND">Andrews LTER</option>
              <option value="ARC">Arctic LTER</option>
              <option value="BES">Baltimore Ecosystem Study</option>
              <option value="BNZ">Bonanza Creek LTER</option>
              <option value="CAP">Central Arizona - Phoenix Urban LTER</option>
              <option value="CCE">California Current Ecosystem</option>
              <option value="CDR">Cedar Creek Natural History Area</option>
              <option value="CWT">Coweeta LTER</option>
              <option value="FCE">Florida Coastal Everglades LTER</option>
              <option value="GCE">Georgia Coastal Ecosystems LTER</option>
              <option value="HBR">Hubbard Brook LTER</option>
              <option value="HFR">Harvard Forest LTER</option>
              <option value="JRN">Jornada Basin LTER</option>
              <option value="KBS">Kellogg Biological Station LTER</option>
              <option value="KNZ">Konza Prairie LTER</option>
              <option value="LNO">LTER Network Office</option>
              <option value="LUQ">Luquillo LTER</option>
              <option value="MCM">McMurdo Dry Valleys LTER</option>
              <option value="MCR">Moorea Coral Reef</option>
              <option value="NTL">North Temperate Lakes LTER</option>
              <option value="NWT">Niwot Ridge LTER</option>
              <option value="PAL">Palmer Station LTER</option>
              <option value="PIE">Plum Island Ecosystem LTER</option>
              <option value="SBC">Santa Barbara Coastal LTER</option>
              <option value="SEV">Sevilleta LTER</option>
              <option value="SGS">Shortgrass Steppe</option>
              <option value="VCR">Virginia Coastal Reserve LTER</option>
            </select>
          </td>
       </tr>
        <tr>
          <td>&nbsp;</td>
          <td>&nbsp;</td>
        </tr>
-->     
        <tr>
          <th colspan="2">
            <h3>Search Options</h3>
          </th>
        </tr>
        <tr>
          <td colspan="2" align="center">
            <table>
              <tr>
                <td>
                  <input type="radio" name="formAllAny" value="0" checked="checked">"And" all search items&nbsp;
                </td>
                <td>
                  <input type="radio" name="formAllAny" value="1">"Or" all search items&nbsp;
                </td>
                <td>
                  <input type="checkbox" name="caseSensitive" value="on">Case sensitive
                </td>
              </tr>
            </table>
          </td>
        </tr>
        <tr>
          <td>&nbsp;</td>
          <td>&nbsp;</td>
        </tr>
        <tr>
          <td colspan="2" align="center">
            <input type="submit" value="Search">&nbsp;&nbsp;
            <!-- <input type="reset" value="Reset"> -->
          </td>
        </tr>
      </table>
      <input type="hidden" name="northBound" value=""/>
      <input type="hidden" name="southBound" value=""/>
      <input type="hidden" name="eastBound" value=""/>
      <input type="hidden" name="westBound" value=""/>
    </form>
    
    <script type="text/javascript" language="Javascript1.1"> 

    <!-- Begin JavaScript input validation checking code.
    var bCancel = false; 

    function validateAdvancedSearchForm(form) {                                                                   
        if (bCancel) 
            return true; 
        else 
            var formValidationResult;
            formValidationResult = validateFloat(form) && validateFloatRange(form); 
            return (formValidationResult == 1);
    } 

    function advancedSearchForm_FloatValidations () { 
      this.a0 = new Array("westBound", "West Boundary must be a number.", new Function ("varName", "this.min='-180.0'; this.max='180.0';  return this[varName];"));
      this.a1 = new Array("eastBound", "East Boundary must be a number.", new Function ("varName", "this.min='-180.0'; this.max='180.0';  return this[varName];"));
      this.a2 = new Array("northBound", "North Boundary must be a number.", new Function ("varName", "this.min='-90.0'; this.max='90.0';  return this[varName];"));
      this.a3 = new Array("southBound", "South Boundary must be a number.", new Function ("varName", "this.min='-90.0'; this.max='90.0';  return this[varName];"));
    } 

    function advancedSearchForm_DateValidations () { 
      this.a0 = new Array("startDate", "Start Date must be a date (YYYY-MM-DD).", new Function ("varName", "this.datePattern='yyyy-MM-dd';  return this[varName];"));
      this.a1 = new Array("endDate", "End Date must be a date (YYYY-MM-DD).", new Function ("varName", "this.datePattern='yyyy-MM-dd';  return this[varName];"));
    } 

    function advancedSearchForm_floatRange () { 
      this.a0 = new Array("westBound", "West Boundary must be in the range -180.0 through 180.0.", new Function ("varName", "this.min='-180.0'; this.max='180.0';  return this[varName];"));
      this.a1 = new Array("eastBound", "East Boundary must be in the range -180.0 through 180.0.", new Function ("varName", "this.min='-180.0'; this.max='180.0';  return this[varName];"));
      this.a2 = new Array("northBound", "North Boundary must be in the range -90.0 through 90.0.", new Function ("varName", "this.min='-90.0'; this.max='90.0';  return this[varName];"));
      this.a3 = new Array("southBound", "South Boundary must be in the range -90.0 through 90.0.", new Function ("varName", "this.min='-90.0'; this.max='90.0';  return this[varName];"));
    } 

    /**
    * Check to see if fields are in a valid float range.
    * Fields are not checked if they are disabled.
    * <p>
    * @param form The form validation is taking place on.
    */
    function validateFloatRange(form) {
        var isValid = true;
        var focusField = null;
        var i = 0;
        var fields = new Array();
        var formName = form.getAttributeNode("name"); 

        oRange = eval('new ' + formName.value + '_floatRange()');
        for (x in oRange) {
            var field = form[oRange[x][0]];
            
            if ((field.type == 'hidden' ||
                field.type == 'text' || field.type == 'textarea') &&
                (field.value.length > 0)  &&
                 field.disabled == false) {
        
                var fMin = parseFloat(oRange[x][2]("min"));
                var fMax = parseFloat(oRange[x][2]("max"));
                var fValue = parseFloat(field.value);
                if (!(fValue >= fMin && fValue <= fMax)) {
                    if (i == 0) {
                        focusField = field;
                    }
                    fields[i++] = oRange[x][1];
                    isValid = false;
                }
            }
        }
        if (fields.length > 0) {
            focusField.focus();
            alert(fields.join('\n'));
        }
        return isValid;
    }


  /**
  * This is a place holder for common utilities used across the javascript validation
  *
  **/


    /**
    * Check to see if fields are a valid byte.
    * Fields are not checked if they are disabled.
    * <p>
    * @param form The form validation is taking place on.
    */
    function validateByte(form) {
        var bValid = true;
        var focusField = null;
        var i = 0;
        var fields = new Array();
        var formName = form.getAttributeNode("name"); 
        oByte = eval('new ' + formName.value + '_ByteValidations()');

        for (x in oByte) {
            var field = form[oByte[x][0]];

            if ((field.type == 'hidden' ||
                field.type == 'text' ||
                field.type == 'textarea' ||
                field.type == 'select-one' ||
                field.type == 'radio')  &&
                field.disabled == false) {

                var value = '';
                // get field's value
                if (field.type == "select-one") {
                    var si = field.selectedIndex;
                    if (si >= 0) {
                        value = field.options[si].value;
                    }
                } else {
                    value = field.value;
                }

                if (value.length > 0) {
                    if (!isAllDigits(value)) {
                        bValid = false;
                        if (i == 0) {
                            focusField = field;
                        }
                        fields[i++] = oByte[x][1];

                    } else {

                        var iValue = parseInt(value);
                        if (isNaN(iValue) || !(iValue >= -128 && iValue <= 127)) {
                            if (i == 0) {
                                focusField = field;
                            }
                            fields[i++] = oByte[x][1];
                            bValid = false;
                        }
                    }
                }

            }
        }
        if (fields.length > 0) {
           focusField.focus();
           alert(fields.join('\n'));
        }
        return bValid;
    }


    /**
    * A field is considered valid if less than the specified maximum.
    * Fields are not checked if they are disabled.
    * <p>
    * <strong>Caution:</strong> Using <code>validateMaxLength</code> on a password field in a 
    *  login page gives unnecessary information away to hackers. While it only slightly
    *  weakens security, we suggest using it only when modifying a password.</p>
    * @param form The form validation is taking place on.
    */
    function validateMaxLength(form) {
        var isValid = true;
        var focusField = null;
        var i = 0;
        var fields = new Array();
        var formName = form.getAttributeNode("name"); 

        oMaxLength = eval('new ' + formName.value + '_maxlength()');        
        for (x in oMaxLength) {
            var field = form[oMaxLength[x][0]];

            if ((field.type == 'hidden' ||
                field.type == 'text' ||
                field.type == 'password' ||
                field.type == 'textarea') &&
                field.disabled == false) {

                var iMax = parseInt(oMaxLength[x][2]("maxlength"));
                if (field.value.length > iMax) {
                    if (i == 0) {
                        focusField = field;
                    }
                    fields[i++] = oMaxLength[x][1];
                    isValid = false;
                }
            }
        }
        if (fields.length > 0) {
           focusField.focus();
           alert(fields.join('\n'));
        }
        return isValid;
    }


    /**
    *  Check to see if fields must contain a value.
    * Fields are not checked if they are disabled.
    * <p>
    * @param form The form validation is taking place on.
    */

    function validateRequired(form) {
        var isValid = true;
        var focusField = null;
        var i = 0;
        var fields = new Array();
        var formName = form.getAttributeNode("name");

        oRequired = eval('new ' + formName.value + '_required()');

        for (x in oRequired) {
            var field = form[oRequired[x][0]];

            if ((field.type == 'hidden' ||
                field.type == 'text' ||
                field.type == 'textarea' ||
                field.type == 'file' ||
                field.type == 'checkbox' ||
                field.type == 'select-one' ||
                field.type == 'password') &&
                field.disabled == false) {

                var value = '';
                // get field's value
                if (field.type == "select-one") {
                    var si = field.selectedIndex;
                    if (si >= 0) {
                        value = field.options[si].value;
                    }
                } else if (field.type == 'checkbox') {
                    if (field.checked) {
                        value = field.value;
                    }
                } else {
                    value = field.value;
                }

                if (trim(value).length == 0) {

                    if (i == 0) {
                        focusField = field;
                    }
                    fields[i++] = oRequired[x][1];
                    isValid = false;
                }
            } else if (field.type == "select-multiple") { 
                var numOptions = field.options.length;
                lastSelected=-1;
                for(loop=numOptions-1;loop>=0;loop--) {
                    if(field.options[loop].selected) {
                        lastSelected = loop;
                        value = field.options[loop].value;
                        break;
                    }
                }
                if(lastSelected < 0 || trim(value).length == 0) {
                    if(i == 0) {
                        focusField = field;
                    }
                    fields[i++] = oRequired[x][1];
                    isValid=false;
                }
            } else if ((field.length > 0) && (field[0].type == 'radio' || field[0].type == 'checkbox')) {
                isChecked=-1;
                for (loop=0;loop < field.length;loop++) {
                    if (field[loop].checked) {
                        isChecked=loop;
                        break; // only one needs to be checked
                    }
                }
                if (isChecked < 0) {
                    if (i == 0) {
                        focusField = field[0];
                    }
                    fields[i++] = oRequired[x][1];
                    isValid=false;
                }
            }
        }
        if (fields.length > 0) {
           focusField.focus();
           alert(fields.join('\n'));
        }
        return isValid;
    }
    
    // Trim whitespace from left and right sides of s.
    function trim(s) {
        return s.replace( /^\s*/, "" ).replace( /\s*$/, "" );
    }


    /**
    * Check to see if fields are a valid integer.
    * Fields are not checked if they are disabled.
    * <p>
    * @param form The form validation is taking place on.
    */
    function validateInteger(form) {
        var bValid = true;
        var focusField = null;
        var i = 0;
        var fields = new Array();
        var formName = form.getAttributeNode("name"); 

        oInteger = eval('new ' + formName.value + '_IntegerValidations()');
        for (x in oInteger) {
            var field = form[oInteger[x][0]];

            if ((field.type == 'hidden' ||
                field.type == 'text' ||
                field.type == 'textarea' ||
                field.type == 'select-one' ||
                field.type == 'radio') &&
                field.disabled == false) {

                var value = '';
                // get field's value
                if (field.type == "select-one") {
                    var si = field.selectedIndex;
                    if (si >= 0) {
                        value = field.options[si].value;
                    }
                } else {
                    value = field.value;
                }

                if (value.length > 0) {

                    if (!isAllDigits(value)) {
                        bValid = false;
                        if (i == 0) {
                            focusField = field;
                        }
                        fields[i++] = oInteger[x][1];

                    } else {
                        var iValue = parseInt(value);
                        if (isNaN(iValue) || !(iValue >= -2147483648 && iValue <= 2147483647)) {
                            if (i == 0) {
                                focusField = field;
                            }
                            fields[i++] = oInteger[x][1];
                            bValid = false;
                       }
                   }
               }
            }
        }
        if (fields.length > 0) {
           focusField.focus();
           alert(fields.join('\n'));
        }
        return bValid;
    }

    function isAllDigits(argvalue) {
        argvalue = argvalue.toString();
        var validChars = "0123456789";
        var startFrom = 0;
        if (argvalue.substring(0, 2) == "0x") {
           validChars = "0123456789abcdefABCDEF";
           startFrom = 2;
        } else if (argvalue.charAt(0) == "0") {
           validChars = "01234567";
           startFrom = 1;
        } else if (argvalue.charAt(0) == "-") {
            startFrom = 1;
        }

        for (var n = startFrom; n < argvalue.length; n++) {
            if (validChars.indexOf(argvalue.substring(n, n+1)) == -1) return false;
        }
        return true;
    }


    /**
    * Check to see if fields are a valid date.
    * Fields are not checked if they are disabled.
    * <p>
    * @param form The form validation is taking place on.
    */
    function validateDate(form) {
       var bValid = true;
       var focusField = null;
       var i = 0;
       var fields = new Array();
       var formName = form.getAttributeNode("name"); 

       oDate = eval('new ' + formName.value + '_DateValidations()');

       for (x in oDate) {
           var field = form[oDate[x][0]];
           var value = field.value;
           var datePattern = oDate[x][2]("datePatternStrict");
           // try loose pattern
           if (datePattern == null)
               datePattern = oDate[x][2]("datePattern");
           if ((field.type == 'hidden' ||
                field.type == 'text' ||
                field.type == 'textarea') &&
               (value.length > 0) && (datePattern.length > 0) &&
                field.disabled == false) {
                 var MONTH = "MM";
                 var DAY = "dd";
                 var YEAR = "yyyy";
                 var orderMonth = datePattern.indexOf(MONTH);
                 var orderDay = datePattern.indexOf(DAY);
                 var orderYear = datePattern.indexOf(YEAR);
                 if ((orderDay < orderYear && orderDay > orderMonth)) {
                     var iDelim1 = orderMonth + MONTH.length;
                     var iDelim2 = orderDay + DAY.length;
                     var delim1 = datePattern.substring(iDelim1, iDelim1 + 1);
                     var delim2 = datePattern.substring(iDelim2, iDelim2 + 1);
                     if (iDelim1 == orderDay && iDelim2 == orderYear) {
                        dateRegexp = new RegExp("^(\\d{2})(\\d{2})(\\d{4})$");
                     } else if (iDelim1 == orderDay) {
                        dateRegexp = new RegExp("^(\\d{2})(\\d{2})[" + delim2 + "](\\d{4})$");
                     } else if (iDelim2 == orderYear) {
                        dateRegexp = new RegExp("^(\\d{2})[" + delim1 + "](\\d{2})(\\d{4})$");
                     } else {
                        dateRegexp = new RegExp("^(\\d{2})[" + delim1 + "](\\d{2})[" + delim2 + "](\\d{4})$");
                     }
                     var matched = dateRegexp.exec(value);
                     if(matched != null) {
                        if (!isValidDate(matched[2], matched[1], matched[3])) {
                           if (i == 0) {
                               focusField = field;
                           }
                           fields[i++] = oDate[x][1];
                           bValid =  false;
                        }
                     } else {
                        if (i == 0) {
                            focusField = field;
                        }
                        fields[i++] = oDate[x][1];
                        bValid =  false;
                     }
                 } else if ((orderMonth < orderYear && orderMonth > orderDay)) {
                     var iDelim1 = orderDay + DAY.length;
                     var iDelim2 = orderMonth + MONTH.length;
                     var delim1 = datePattern.substring(iDelim1, iDelim1 + 1);
                     var delim2 = datePattern.substring(iDelim2, iDelim2 + 1);
                     if (iDelim1 == orderMonth && iDelim2 == orderYear) {
                         dateRegexp = new RegExp("^(\\d{2})(\\d{2})(\\d{4})$");
                     } else if (iDelim1 == orderMonth) {
                         dateRegexp = new RegExp("^(\\d{2})(\\d{2})[" + delim2 + "](\\d{4})$");
                     } else if (iDelim2 == orderYear) {
                         dateRegexp = new RegExp("^(\\d{2})[" + delim1 + "](\\d{2})(\\d{4})$");
                     } else {
                         dateRegexp = new RegExp("^(\\d{2})[" + delim1 + "](\\d{2})[" + delim2 + "](\\d{4})$");
                     }
                     var matched = dateRegexp.exec(value);
                     if(matched != null) {
                         if (!isValidDate(matched[1], matched[2], matched[3])) {
                             if (i == 0) {
                         focusField = field;
                             }
                             fields[i++] = oDate[x][1];
                             bValid =  false;
                          }
                     } else {
                         if (i == 0) {
                             focusField = field;
                         }
                         fields[i++] = oDate[x][1];
                         bValid =  false;
                     }
                 } else if ((orderMonth > orderYear && orderMonth < orderDay)) {
                     var iDelim1 = orderYear + YEAR.length;
                     var iDelim2 = orderMonth + MONTH.length;
                     var delim1 = datePattern.substring(iDelim1, iDelim1 + 1);
                     var delim2 = datePattern.substring(iDelim2, iDelim2 + 1);
                     if (iDelim1 == orderMonth && iDelim2 == orderDay) {
                         dateRegexp = new RegExp("^(\\d{4})(\\d{2})(\\d{2})$");
                     } else if (iDelim1 == orderMonth) {
                         dateRegexp = new RegExp("^(\\d{4})(\\d{2})[" + delim2 + "](\\d{2})$");
                     } else if (iDelim2 == orderDay) {
                         dateRegexp = new RegExp("^(\\d{4})[" + delim1 + "](\\d{2})(\\d{2})$");
                     } else {
                         dateRegexp = new RegExp("^(\\d{4})[" + delim1 + "](\\d{2})[" + delim2 + "](\\d{2})$");
                     }
                     var matched = dateRegexp.exec(value);
                     if(matched != null) {
                         if (!isValidDate(matched[3], matched[2], matched[1])) {
                             if (i == 0) {
                                 focusField = field;
                             }
                             fields[i++] = oDate[x][1];
                             bValid =  false;
                         }
                     } else {
                          if (i == 0) {
                              focusField = field;
                          }
                          fields[i++] = oDate[x][1];
                          bValid =  false;
                     }
                 } else {
                     if (i == 0) {
                         focusField = field;
                     }
                     fields[i++] = oDate[x][1];
                     bValid =  false;
                 }
          }
       }
       if (fields.length > 0) {
          focusField.focus();
          alert(fields.join('\n'));
       }
       return bValid;
    }
    
    function isValidDate(day, month, year) {
        if (month < 1 || month > 12) {
            return false;
        }
        if (day < 1 || day > 31) {
            return false;
        }
        if ((month == 4 || month == 6 || month == 9 || month == 11) &&
            (day == 31)) {
            return false;
        }
        if (month == 2) {
            var leap = (year % 4 == 0 &&
               (year % 100 != 0 || year % 400 == 0));
            if (day>29 || (day == 29 && !leap)) {
                return false;
            }
        }
        return true;
    }


    /**
    * Check to see if fields are a valid creditcard number based on Luhn checksum.
    * Fields are not checked if they are disabled.
    * <p>
    * @param form The form validation is taking place on.
    */
    function validateCreditCard(form) {
        var bValid = true;
        var focusField = null;
        var i = 0;
        var fields = new Array();
        var formName = form.getAttributeNode("name");

        oCreditCard = eval('new ' + formName.value + '_creditCard()');

        for (x in oCreditCard) {
            if ((form[oCreditCard[x][0]].type == 'text' ||
                 form[oCreditCard[x][0]].type == 'textarea') &&
                (form[oCreditCard[x][0]].value.length > 0)  &&
                 form[oCreditCard[x][0]].disabled == false) {
                if (!luhnCheck(form[oCreditCard[x][0]].value)) {
                    if (i == 0) {
                        focusField = form[oCreditCard[x][0]];
                    }
                    fields[i++] = oCreditCard[x][1];
                    bValid = false;
                }
            }
        }
        if (fields.length > 0) {
            focusField.focus();
            alert(fields.join('\n'));
        }
        return bValid;
    }

    /**
     * Checks whether a given credit card number has a valid Luhn checksum.
     * This allows you to spot most randomly made-up or garbled credit card numbers immediately.
     * Reference: http://www.speech.cs.cmu.edu/~sburke/pub/luhn_lib.html
     */
    function luhnCheck(cardNumber) {
        if (isLuhnNum(cardNumber)) {
            var no_digit = cardNumber.length;
            var oddoeven = no_digit & 1;
            var sum = 0;
            for (var count = 0; count < no_digit; count++) {
                var digit = parseInt(cardNumber.charAt(count));
                if (!((count & 1) ^ oddoeven)) {
                    digit *= 2;
                    if (digit > 9) digit -= 9;
                };
                sum += digit;
            };
            if (sum == 0) return false;
            if (sum % 10 == 0) return true;
        };
        return false;
    }

    function isLuhnNum(argvalue) {
        argvalue = argvalue.toString();
        if (argvalue.length == 0) {
            return false;
        }
        for (var n = 0; n < argvalue.length; n++) {
            if ((argvalue.substring(n, n+1) < "0") ||
                (argvalue.substring(n,n+1) > "9")) {
                return false;
            }
        }
        return true;
    }


    /**
    * Check to see if fields is in a valid integer range.
    * Fields are not checked if they are disabled.
    * <p>
    * @param form The form validation is taking place on.
    */
    function validateIntRange(form) {
        var isValid = true;
        var focusField = null;
        var i = 0;
        var fields = new Array();
        var formName = form.getAttributeNode("name"); 

        oRange = eval('new ' + formName.value + '_intRange()');        
        for (x in oRange) {
            var field = form[oRange[x][0]];
            if (field.disabled == false)  {
                var value = '';
                if (field.type == 'hidden' ||
                    field.type == 'text' || field.type == 'textarea' ||
                    field.type == 'radio' ) {
                    value = field.value;
                }
                if (field.type == 'select-one') {
                    var si = field.selectedIndex;
                    if (si >= 0) {
                        value = field.options[si].value;
                    }
                }
                if (value.length > 0) {
                    var iMin = parseInt(oRange[x][2]("min"));
                    var iMax = parseInt(oRange[x][2]("max"));
                    var iValue = parseInt(value);
                    if (!(iValue >= iMin && iValue <= iMax)) {
                        if (i == 0) {
                            focusField = field;
                        }
                        fields[i++] = oRange[x][1];
                        isValid = false;
                    }
                }
            }
        }
        if (fields.length > 0) {
            focusField.focus();
            alert(fields.join('\n'));
        }
        return isValid;
    }


    /**
    *  Check to see if fields are a valid short.
    * Fields are not checked if they are disabled.
    * <p>
    * @param form The form validation is taking place on.
    */
    function validateShort(form) {
        var bValid = true;
        var focusField = null;
        var i = 0;
        var fields = new Array();
        var formName = form.getAttributeNode("name");

        oShort = eval('new ' + formName.value + '_ShortValidations()');

        for (x in oShort) {
            var field = form[oShort[x][0]];

            if ((field.type == 'hidden' ||
                field.type == 'text' ||
                field.type == 'textarea' ||
                field.type == 'select-one' ||
                field.type == 'radio')  &&
                field.disabled == false) {

                var value = '';
                // get field's value
                if (field.type == "select-one") {
                    var si = field.selectedIndex;
                    if (si >= 0) {
                        value = field.options[si].value;
                    }
                } else {
                    value = field.value;
                }

                if (value.length > 0) {
                    if (!isAllDigits(value)) {
                        bValid = false;
                        if (i == 0) {
                            focusField = field;
                        }
                        fields[i++] = oShort[x][1];

                    } else {

                        var iValue = parseInt(value);
                        if (isNaN(iValue) || !(iValue >= -32768 && iValue <= 32767)) {
                            if (i == 0) {
                                focusField = field;
                            }
                            fields[i++] = oShort[x][1];
                            bValid = false;
                        }
                   }
               }
            }
        }
        if (fields.length > 0) {
           focusField.focus();
           alert(fields.join('\n'));
        }
        return bValid;
    }


    /**
    * Check to see if fields are a valid float.
    * Fields are not checked if they are disabled.
    * <p>
    * @param form The form validation is taking place on.
    */
    function validateFloat(form) {
        var bValid = true;
        var focusField = null;
        var i = 0;
        var fields = new Array();
         var formName = form.getAttributeNode("name");

        oFloat = eval('new ' + formName.value + '_FloatValidations()');
        for (x in oFloat) {
            var field = form[oFloat[x][0]];
            
            if ((field.type == 'hidden' ||
                field.type == 'text' ||
                field.type == 'textarea' ||
                field.type == 'select-one' ||
                field.type == 'radio') &&
                field.disabled == false) {
        
                var value = '';
                // get field's value
                if (field.type == "select-one") {
                    var si = field.selectedIndex;
                    if (si >= 0) {
                        value = field.options[si].value;
                    }
                } else {
                    value = field.value;
                }
        
                if (value.length > 0) {
                    // remove '.' before checking digits
                    var tempArray = value.split('.');
                    //Strip off leading '0'
                    var zeroIndex = 0;
                    var joinedString= tempArray.join('');
                    while (joinedString.charAt(zeroIndex) == '0') {
                        zeroIndex++;
                    }
                    var noZeroString = joinedString.substring(zeroIndex,joinedString.length);

                    if (!isAllDigits(noZeroString)) {
                        bValid = false;
                        if (i == 0) {
                            focusField = field;
                        }
                        fields[i++] = oFloat[x][1];

                    } else {
                    var iValue = parseFloat(value);
                    if (isNaN(iValue)) {
                        if (i == 0) {
                            focusField = field;
                        }
                        fields[i++] = oFloat[x][1];
                        bValid = false;
                    }
                    }
                }
            }
        }
        if (fields.length > 0) {
           focusField.focus();
           alert(fields.join('\n'));
        }
        return bValid;
    }


    /**
    * Check to see if fields are a valid email address.
    * Fields are not checked if they are disabled.
    * <p>
    * @param form The form validation is taking place on.
    */
    function validateEmail(form) {
        var bValid = true;
        var focusField = null;
        var i = 0;
        var fields = new Array();
        var formName = form.getAttributeNode("name");


        oEmail = eval('new ' + formName.value + '_email()');

        for (x in oEmail) {
            var field = form[oEmail[x][0]];
            if ((field.type == 'hidden' || 
                 field.type == 'text' ||
                 field.type == 'textarea') &&
                (field.value.length > 0) &&
                field.disabled == false) {
                if (!checkEmail(field.value)) {
                    if (i == 0) {
                        focusField = field;
                    }
                    fields[i++] = oEmail[x][1];
                    bValid = false;
                }
            }
        }
        if (fields.length > 0) {
            focusField.focus();
            alert(fields.join('\n'));
        }
        return bValid;
    }

    /**
     * Reference: Sandeep V. Tamhankar (stamhankar@hotmail.com),
     * http://javascript.internet.com
     */
    function checkEmail(emailStr) {
       if (emailStr.length == 0) {
           return true;
       }
       var emailPat=/^(.+)@(.+)$/;
       var specialChars="\\(\\)<>@,;:\\\\\\\"\\.\\[\\]";
       var validChars="\[^\\s" + specialChars + "\]";
       var quotedUser="(\"[^\"]*\")";
       var ipDomainPat=/^(\d{1,3})[.](\d{1,3})[.](\d{1,3})[.](\d{1,3})$/;
       var atom=validChars + '+';
       var word="(" + atom + "|" + quotedUser + ")";
       var userPat=new RegExp("^" + word + "(\\." + word + ")*$");
       var domainPat=new RegExp("^" + atom + "(\\." + atom + ")*$");
       var matchArray=emailStr.match(emailPat);
       if (matchArray == null) {
           return false;
       }
       var user=matchArray[1];
       var domain=matchArray[2];
       if (user.match(userPat) == null) {
           return false;
       }
       var IPArray = domain.match(ipDomainPat);
       if (IPArray != null) {
           for (var i = 1; i <= 4; i++) {
              if (IPArray[i] > 255) {
                 return false;
              }
           }
           return true;
       }
       var domainArray=domain.match(domainPat);
       if (domainArray == null) {
           return false;
       }
       var atomPat=new RegExp(atom,"g");
       var domArr=domain.match(atomPat);
       var len=domArr.length;
       if ((domArr[domArr.length-1].length < 2) ||
           (domArr[domArr.length-1].length > 3)) {
           return false;
       }
       if (len < 2) {
           return false;
       }
       return true;
    }

  
    /**
    * Check to see if fields are a valid using a regular expression.
    * Fields are not checked if they are disabled.
    * <p>
    * @param form The form validation is taking place on.
    */
    function validateMask(form) {
        var isValid = true;
        var focusField = null;
        var i = 0;
        var fields = new Array();
        var formName = form.getAttributeNode("name"); 

        oMasked = eval('new ' + formName.value + '_mask()');      
        for (x in oMasked) {
            var field = form[oMasked[x][0]];

            if ((field.type == 'hidden' ||
                field.type == 'text' ||
                 field.type == 'textarea' ||
                 field.type == 'file') &&
                 (field.value.length > 0) &&
                 field.disabled == false) {

                if (!matchPattern(field.value, oMasked[x][2]("mask"))) {
                    if (i == 0) {
                        focusField = field;
                    }
                    fields[i++] = oMasked[x][1];
                    isValid = false;
                }
            }
        }

        if (fields.length > 0) {
           focusField.focus();
           alert(fields.join('\n'));
        }
        return isValid;
    }

    function matchPattern(value, mask) {
       return mask.exec(value);
    }


    /**
    * A field is considered valid if greater than the specified minimum.
    * Fields are not checked if they are disabled.
    * <p>
    * <strong>Caution:</strong> Using <code>validateMinLength</code> on a password field in a 
    *  login page gives unnecessary information away to hackers. While it only slightly
    *  weakens security, we suggest using it only when modifying a password.</p>
    * @param form The form validation is taking place on.
    */
    function validateMinLength(form) {
        var isValid = true;
        var focusField = null;
        var i = 0;
        var fields = new Array();
        var formName = form.getAttributeNode("name");


        oMinLength = eval('new ' + formName.value + '_minlength()');

        for (x in oMinLength) {
            var field = form[oMinLength[x][0]];

            if ((field.type == 'hidden' ||
                field.type == 'text' ||
                field.type == 'password' ||
                field.type == 'textarea') &&
                field.disabled == false) {

                var iMin = parseInt(oMinLength[x][2]("minlength"));
                if ((trim(field.value).length > 0) && (field.value.length < iMin)) {
                    if (i == 0) {
                        focusField = field;
                    }
                    fields[i++] = oMinLength[x][1];
                    isValid = false;
                }
            }
        }
        if (fields.length > 0) {
           focusField.focus();
           alert(fields.join('\n'));
        }
        return isValid;
    }

    //End  JavaScript input validation checking code. --> 
    </script>
            </td>
          </tr>
          <tr> 
            <td width="375">&nbsp;</td>
            <td width="365">&nbsp;</td>
          </tr>
        </table>
      </td>
    </tr>
  </table>
</body>
<!-- ************************* END SEARCHBOX TABLE ************************* -->
