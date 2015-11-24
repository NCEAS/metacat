<%@ page language="java"%>

<!--
/**
  *  '$RCSfile$'
  *      Authors:     Duane Costa
  *      Copyright:   2006 University of New Mexico and
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

<html>

  <head>
    <link href="<%=STYLE_SKINS_URL%>/lter/lter.css" rel="stylesheet" type="text/css">
    <script language="javascript" type="text/javascript">
    
      function trim(stringToTrim) {
        return stringToTrim.replace(/^\s*/, '').replace(/\s*$/,'');
      }
      
      function keywordSearch(formObj, searchKeyword) {
        var searchString = trim(searchKeyword);
        formObj.browseValue.value=searchString;
        formObj.submit();
        return true;
      }
      
    </script>
    <title>
      Browse the LTER Data Catalog
    </title>
  </head>

  <body>
    <h2>
      Browse the LTER Data Catalog
    </h2>
    <p>
      Browse by category using the links below.
    </p>
    <form name="browseForm" action="advancedbrowseforward.jsp" method="POST" target="_top">
      <input type="hidden" name="browseValue" value="" />
      <table>
        <tr>
          <td width="375">
            &nbsp;
          </td>
          <td width="365" class="searchsubcat">
            &nbsp;
          </td>
        </tr>
        <tr>
          <td width="375" class="searchcat">
            Ecology
          </td>
          <td width="365" class="searchcat">
            Evolution
          </td>
        </tr>
        <tr valign="top">
          <td width="375" class="searchsubcat">
            <a href="#1" onClick="keywordSearch(document.browseForm, 'biodiversity')" class="searchsubcat">
              Biodiversity,
            </a>
            <a href="#2" onClick="keywordSearch(document.browseForm, 'competition')" class="searchsubcat">
              Competition,
            </a>
            <a href="#3" onClick="keywordSearch(document.browseForm, 'decomposition')" class="searchsubcat">
              Decomposition,
            </a>
            <a href="#4" onClick="keywordSearch(document.browseForm, 'disturbance')" class="searchsubcat">
              Disturbance,
            </a>
            <a href="#5" onClick="keywordSearch(document.browseForm, 'endangered species')" class="searchsubcat">
              Endangered Species,
            </a>
            <a href="#6" onClick="keywordSearch(document.browseForm, 'herbivory')" class="searchsubcat">
              Herbivory,
            </a>
            <a href="#7" onClick="keywordSearch(document.browseForm, 'invasive species')" class="searchsubcat">
              Invasive Species,
            </a>
            <a href="#8" onClick="keywordSearch(document.browseForm, 'nutrient cycling')" class="searchsubcat">
              Nutrient Cycling,
            </a>
            <a href="#9" onClick="keywordSearch(document.browseForm, 'parasitism')" class="searchsubcat">
              Parasitism,
            </a>
            <a href="#10" onClick="keywordSearch(document.browseForm, 'population dynamics')" class="searchsubcat">
              Population Dynamics,
            </a>
            <a href="#11" onClick="keywordSearch(document.browseForm, 'predation')" class="searchsubcat">
              Predation,
            </a>
            <a href="#12" onClick="keywordSearch(document.browseForm, 'productivity')" class="searchsubcat">
              Productivity,
            </a>
            <a href="#13" onClick="keywordSearch(document.browseForm, 'succession')" class="searchsubcat">
              Succession,
            </a>
            <a href="#14" onClick="keywordSearch(document.browseForm, 'symbiosis')" class="searchsubcat">
              Symbiosis,
            </a>
            <a href="#15" onClick="keywordSearch(document.browseForm, 'trophic dynamics')" class="searchsubcat">
              Trophic Dynamics
            </a>
          </td>
          <td width="365" class="searchsubcat">
            <a href="#16" onClick="keywordSearch(document.browseForm, 'adaptation')" class="searchsubcat">
              Adaptation,
            </a>
            <a href="#17" onClick="keywordSearch(document.browseForm, 'evolution')" class="searchsubcat">
              Evolution,
            </a>
            <a href="#18" onClick="keywordSearch(document.browseForm, 'extinction')" class="searchsubcat">
              Extinction,
            </a>
            <a href="#19" onClick="keywordSearch(document.browseForm, 'genetics')" class="searchsubcat">
              Genetics,
            </a>
            <a href="#20" onClick="keywordSearch(document.browseForm, 'mutation')" class="searchsubcat">
              Mutation,
            </a>
            <a href="#21" onClick="keywordSearch(document.browseForm, 'selection')" class="searchsubcat">
              Selection,
            </a>
            <a href="#22" onClick="keywordSearch(document.browseForm, 'speciation')" class="searchsubcat">
              Speciation,
            </a>
            <a href="#23" onClick="keywordSearch(document.browseForm, 'survival')" class="searchsubcat">
              Survival
            </a>
          </td>
        </tr>
        <tr>
          <td width="375">
            &nbsp;
          </td>
          <td width="365">
            &nbsp;
          </td>
        </tr>
        <tr>
          <td width="365" class="searchcat">
            Habitat
          </td>
          <td width="375" class="searchcat">
            Level of Organization
          </td>
        </tr>
        <tr valign="top">
          <td width="365" class="searchsubcat">
            <a href="#24" onClick="keywordSearch(document.browseForm, 'alpine')" class="searchsubcat">
              Alpine,
            </a>
            <a href="#25" onClick="keywordSearch(document.browseForm, 'benthic')" class="searchsubcat">
              Benthic,
            </a>
            <a href="#26" onClick="keywordSearch(document.browseForm, 'desert')" class="searchsubcat">
              Desert,
            </a>
            <a href="#27" onClick="keywordSearch(document.browseForm, 'estuary')" class="searchsubcat">
              Estuary,
            </a>
            <a href="#28" onClick="keywordSearch(document.browseForm, 'forest')" class="searchsubcat">
              Forest,
            </a>
            <a href="#29" onClick="keywordSearch(document.browseForm, 'freshwater')" class="searchsubcat">
              Freshwater,
            </a>
            <a href="#30" onClick="keywordSearch(document.browseForm, 'grassland')" class="searchsubcat">
              Grassland,
            </a>
            <a href="#31" onClick="keywordSearch(document.browseForm, 'marine')" class="searchsubcat">
              Marine,
            </a>
            <a href="#32" onClick="keywordSearch(document.browseForm, 'montane')" class="searchsubcat">
              Montane,
            </a>
            <a href="#33" onClick="keywordSearch(document.browseForm, 'terrestrial')" class="searchsubcat">
              Terrestrial,
            </a>
            <a href="#34" onClick="keywordSearch(document.browseForm, 'tundra')" class="searchsubcat">
              Tundra,
            </a>
            <a href="#35" onClick="keywordSearch(document.browseForm, 'urban')" class="searchsubcat">
              Urban,
            </a>
            <a href="#36" onClick="keywordSearch(document.browseForm, 'wetland')" class="searchsubcat">
              Wetland
            </a>
          </td>
          <td width="375" class="searchsubcat">
            <a href="#37" onClick="keywordSearch(document.browseForm, 'cell')" class="searchsubcat">
              Cell,
            </a>
            <a href="#38" onClick="keywordSearch(document.browseForm, 'community')" class="searchsubcat">
              Community,
            </a>
            <a href="#39" onClick="keywordSearch(document.browseForm, 'ecosystem')" class="searchsubcat">
              Ecosystem,
            </a>
            <a href="#40" onClick="keywordSearch(document.browseForm, 'global')" class="searchsubcat">
              Global,
            </a>
            <a href="#41" onClick="keywordSearch(document.browseForm, 'landscape')" class="searchsubcat">
              Landscape,
            </a>
            <a href="#42" onClick="keywordSearch(document.browseForm, 'molecule')" class="searchsubcat">
              Molecule,
            </a>
            <a href="#43" onClick="keywordSearch(document.browseForm, 'organism')" class="searchsubcat">
              Organism,
            </a>
            <a href="#44" onClick="keywordSearch(document.browseForm, 'population')" class="searchsubcat">
              Population
            </a>
          </td>
        </tr>
        <tr>
          <td width="375">
            &nbsp;
          </td>
          <td width="365" class="searchsubcat">
            &nbsp;
          </td>
        </tr>
        <tr>
          <td width="365" class="searchcat">
            Measurements
          </td>
          <td width="375" class="searchcat">
            Taxonomy
          </td>
        </tr>
        <tr valign="top">
          <td width="365" class="searchsubcat">
            <a href="#45" onClick="keywordSearch(document.browseForm, 'biomass')" class="searchsubcat">
              Biomass,
            </a>
            <a href="#46" onClick="keywordSearch(document.browseForm, 'carbon')" class="searchsubcat">
              Carbon,
            </a>
            <a href="#47" onClick="keywordSearch(document.browseForm, 'chlorophyll')" class="searchsubcat">
              Chlorophyll,
            </a>
            <a href="#48" onClick="keywordSearch(document.browseForm, 'gis')" class="searchsubcat">
              GIS,
            </a>
            <a href="#49" onClick="keywordSearch(document.browseForm, 'nitrate')" class="searchsubcat">
              Nitrate,
            </a>
            <a href="#50" onClick="keywordSearch(document.browseForm, 'nutrients')" class="searchsubcat">
              Nutrients,
            </a>
            <a href="#51" onClick="keywordSearch(document.browseForm, 'precipitation')" class="searchsubcat">
              Precipitation,
            </a>
            <a href="#52" onClick="keywordSearch(document.browseForm, 'radiation')" class="searchsubcat">
              Radiation,
            </a>
            <a href="#53" onClick="keywordSearch(document.browseForm, 'temperature')" class="searchsubcat">
              Temperature,
            </a>
            <a href="#54" onClick="keywordSearch(document.browseForm, 'weather')" class="searchsubcat">
              Weather
            </a>
          </td>
          <td width="375" class="searchsubcat">
            <a href="#55" onClick="keywordSearch(document.browseForm, 'amphibian')" class="searchsubcat">
              Amphibian,
            </a>
            <a href="#56" onClick="keywordSearch(document.browseForm, 'bird')" class="searchsubcat">
              Bird,
            </a>
            <a href="#57" onClick="keywordSearch(document.browseForm, 'fish')" class="searchsubcat">
              Fish,
            </a>
            <a href="#58" onClick="keywordSearch(document.browseForm, 'fungus')" class="searchsubcat">
              Fungus,
            </a>
            <a href="#59" onClick="keywordSearch(document.browseForm, 'invertebrate')" class="searchsubcat">
              Invertebrate,
            </a>
            <a href="#60" onClick="keywordSearch(document.browseForm, 'mammal')" class="searchsubcat">
              Mammal,
            </a>
            <a href="#61" onClick="keywordSearch(document.browseForm, 'microbe')" class="searchsubcat">
              Microbe,
            </a>
            <a href="#62" onClick="keywordSearch(document.browseForm, 'plant')" class="searchsubcat">
              Plant,
            </a>
            <a href="#63" onClick="keywordSearch(document.browseForm, 'reptile')" class="searchsubcat">
              Reptile,
            </a>
            <a href="#64" onClick="keywordSearch(document.browseForm, 'virus')" class="searchsubcat">
              Virus
            </a>
          </td>
        </tr>
      </table>
    </form>
  </body>

</html>
