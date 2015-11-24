<% 
 /**
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
  */
%>
<!-- *********************** START SEARCHBOX TABLE ************************* -->
<table width="740" border="0" cellspacing="0" cellpadding="0">
  <tr> 
    <td width="10" align="right" valign="top"><img src="images/panelhead_bg_lcorner.gif" width="10" height="21"></td>
    <td width="720" class="sectionheader">Browse data on the 
      KNB by Keyword</td>
    <td width="10" align="left" valign="top"><img src="images/panelhead_bg_rcorner.gif" width="10" height="21"></td>
  </tr>
    <td colspan="3">
<table width="740" border="0" cellpadding="0" cellspacing="0" class="subpanel">
        <%
/*
US Geography
------------
Northeast, Southeast, South, Midwest, Northwest, Southwest, Pacific Ocean, Atlantic Ocean, 
Great Lakes, (could also list states here)
*/
%>
        <tr> 
          <td width="375" class="searchcat">Taxonomy</td>
          <td width="365" class="searchcat">Measurements</td>
        </tr>
        <tr valign="top"> 
          <td width="375" class="searchsubcat"> <a href="#" onClick="keywordSearch(document.searchForm, 'amphibian')" class="searchsubcat">Amphibian,</a> 
            <a href="#" onClick="keywordSearch(document.searchForm, 'bird')" class="searchsubcat">Bird,</a> 
            <a href="#" onClick="keywordSearch(document.searchForm, 'fish')" class="searchsubcat">Fish,</a> 
            <a href="#" onClick="keywordSearch(document.searchForm, 'fungus')" class="searchsubcat">Fungus,</a> 
            <a href="#" onClick="keywordSearch(document.searchForm, 'invertebrate')" class="searchsubcat">Invertebrate,</a> 
            <a href="#" onClick="keywordSearch(document.searchForm, 'mammal')" class="searchsubcat">Mammal,</a> 
            <a href="#" onClick="keywordSearch(document.searchForm, 'microbe')" class="searchsubcat">Microbe,</a> 
            <a href="#" onClick="keywordSearch(document.searchForm, 'plant')" class="searchsubcat">Plant,</a> 
            <a href="#" onClick="keywordSearch(document.searchForm, 'reptile')" class="searchsubcat">Reptile,</a> 
            <a href="#" onClick="keywordSearch(document.searchForm, 'virus')" class="searchsubcat">Virus</a></td>
          <td width="365" class="searchsubcat"> <a href="#" onClick="keywordSearch(document.searchForm, 'biomass')" class="searchsubcat">Biomass,</a> 
            <a href="#" onClick="keywordSearch(document.searchForm, 'carbon')" class="searchsubcat">Carbon,</a> 
            <a href="#" onClick="keywordSearch(document.searchForm, 'chlorophyll')" class="searchsubcat">Chlorophyll,</a> 
            <a href="#" onClick="keywordSearch(document.searchForm, 'gis')" class="searchsubcat">GIS,</a> 
            <a href="#" onClick="keywordSearch(document.searchForm, 'nitrate')" class="searchsubcat">Nitrate,</a> 
            <a href="#" onClick="keywordSearch(document.searchForm, 'nutrient')" class="searchsubcat">Nutrients,</a> 
            <a href="#" onClick="keywordSearch(document.searchForm, 'precipitation')" class="searchsubcat">Precipitation,</a> 
            <a href="#" onClick="keywordSearch(document.searchForm, 'temperature')" class="searchsubcat">Temperature,</a> 
            <a href="#" onClick="keywordSearch(document.searchForm, 'radiation')" class="searchsubcat">Radiation,</a> 
            <a href="#" onClick="keywordSearch(document.searchForm, 'weather')" class="searchsubcat">Weather</a> 
        </tr>
        <tr> 
          <td width="375">&nbsp;</td>
          <td width="365" class="searchsubcat">&nbsp;</tr>
        <tr> 
          <td width="375" class="searchcat">Level of Organization</td>
          <td width="365" class="searchcat">Evolution</td>
        </tr>
        <tr valign="top"> 
          <td width="375" class="searchsubcat"> <a href="#" onClick="keywordSearch(document.searchForm, 'molecul')" class="searchsubcat">Molecule,</a> 
            <a href="#" onClick="keywordSearch(document.searchForm, 'cell')" class="searchsubcat">Cell,</a> 
            <a href="#" onClick="keywordSearch(document.searchForm, 'organism')" class="searchsubcat">Organism,</a> 
            <a href="#" onClick="keywordSearch(document.searchForm, 'population')" class="searchsubcat">Population,</a> 
            <a href="#" onClick="keywordSearch(document.searchForm, 'community')" class="searchsubcat">Community,</a> 
            <a href="#" onClick="keywordSearch(document.searchForm, 'landscape')" class="searchsubcat">Landscape,</a> 
            <a href="#" onClick="keywordSearch(document.searchForm, 'ecosystem')" class="searchsubcat">Ecosystem,</a> 
            <a href="#" onClick="keywordSearch(document.searchForm, 'global')" class="searchsubcat">Global</a></td>
          <td width="365" class="searchsubcat"> <a href="#" onClick="keywordSearch(document.searchForm, 'adaptation')" class="searchsubcat">Adaptation,</a> 
            <a href="#" onClick="keywordSearch(document.searchForm, 'evolution')" class="searchsubcat">Evolution,</a> 
            <a href="#" onClick="keywordSearch(document.searchForm, 'extinct')" class="searchsubcat">Extinction,</a> 
            <a href="#" onClick="keywordSearch(document.searchForm, 'genetics')" class="searchsubcat">Genetics,</a> 
            <a href="#" onClick="keywordSearch(document.searchForm, 'mutation')" class="searchsubcat">Mutation,</a> 
            <a href="#" onClick="keywordSearch(document.searchForm, 'selection')" class="searchsubcat">Selection,</a> 
            <a href="#" onClick="keywordSearch(document.searchForm, 'speciation')" class="searchsubcat">Speciation,</a> 
            <a href="#" onClick="keywordSearch(document.searchForm, 'survival')" class="searchsubcat">Survival</a></td>
        </tr>
        <tr> 
          <td width="375">&nbsp;</td>
          <td width="365">&nbsp;</td>
        </tr>
        <tr> 
          <td width="375" class="searchcat">Ecology</td>
          <td width="365" class="searchcat">Habitat</td>
        </tr>
        <tr valign="top"> 
          <td width="375" class="searchsubcat"> <a href="#" onClick="keywordSearch(document.searchForm, 'biodiversity')" class="searchsubcat">Biodiversity,</a> 
            <a href="#" onClick="keywordSearch(document.searchForm, 'competition')" class="searchsubcat">Competition,</a> 
            <a href="#" onClick="keywordSearch(document.searchForm, 'decomposition')" class="searchsubcat">Decomposition,</a> 
            <a href="#" onClick="keywordSearch(document.searchForm, 'disturbance')" class="searchsubcat">Disturbance,</a> 
            <a href="#" onClick="keywordSearch(document.searchForm, 'endangered species')" class="searchsubcat">Endangered 
            Species,</a> <a href="#" onClick="keywordSearch(document.searchForm, 'herbivory')" class="searchsubcat">Herbivory,</a> 
            <a href="#" onClick="keywordSearch(document.searchForm, 'invasive species')" class="searchsubcat">Invasive 
            Species,</a> <a href="#" onClick="keywordSearch(document.searchForm, 'nutrient cycling')" class="searchsubcat">Nutrient 
            Cycling,</a> <a href="#" onClick="keywordSearch(document.searchForm, 'parasitism')" class="searchsubcat">Parasitism,</a> 
            <a href="#" onClick="keywordSearch(document.searchForm, 'population dynamics')" class="searchsubcat">Population 
            Dynamics,</a> <a href="#" onClick="keywordSearch(document.searchForm, 'predation')" class="searchsubcat">Predation,</a> 
            <a href="#" onClick="keywordSearch(document.searchForm, 'productivity')" class="searchsubcat">Productivity,</a> 
            <a href="#" onClick="keywordSearch(document.searchForm, 'succession')" class="searchsubcat">Succession,</a> 
            <a href="#" onClick="keywordSearch(document.searchForm, 'symbiosis')" class="searchsubcat">Symbiosis,</a> 
            <a href="#" onClick="keywordSearch(document.searchForm, 'trophic dynamics')" class="searchsubcat">Trophic 
            Dynamics</a></td>
          <td width="365" class="searchsubcat"> <a href="#" onClick="keywordSearch(document.searchForm, 'alpine')" class="searchsubcat">Alpine,</a> 
            <a href="#" onClick="keywordSearch(document.searchForm, 'freshwater')" class="searchsubcat">Freshwater,</a> 
            <a href="#" onClick="keywordSearch(document.searchForm, 'benthic')" class="searchsubcat">Benthic,</a> 
            <a href="#" onClick="keywordSearch(document.searchForm, 'desert')" class="searchsubcat">Desert,</a> 
            <a href="#" onClick="keywordSearch(document.searchForm, 'estuar')" class="searchsubcat">Estuary,</a> 
            <a href="#" onClick="keywordSearch(document.searchForm, 'forest')" class="searchsubcat">Forest,</a> 
            <a href="#" onClick="keywordSearch(document.searchForm, 'grassland')" class="searchsubcat">Grassland,</a> 
            <a href="#" onClick="keywordSearch(document.searchForm, 'marine')" class="searchsubcat">Marine,</a> 
            <a href="#" onClick="keywordSearch(document.searchForm, 'montane')" class="searchsubcat">Montane,</a> 
            <a href="#" onClick="keywordSearch(document.searchForm, 'terrestrial')" class="searchsubcat">Terrestrial,</a> 
            <a href="#" onClick="keywordSearch(document.searchForm, 'tundra')" class="searchsubcat">Tundra,</a> 
            <a href="#" onClick="keywordSearch(document.searchForm, 'urban')" class="searchsubcat">Urban,</a> 
            <a href="#" onClick="keywordSearch(document.searchForm, 'wetland')" class="searchsubcat">Wetland</a></td>
        </tr>
        <tr> 
          <td width="375">&nbsp;</td>
          <td width="365">&nbsp;</td>
        </tr>
      </table></td>
  </tr>
</table>
<!-- ************************* END SEARCHBOX TABLE ************************* -->
