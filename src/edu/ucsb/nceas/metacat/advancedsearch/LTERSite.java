/**
 *  '$RCSfile$'
 *  Copyright: 2005 University of New Mexico and the 
 *             Regents of the University of California and the
 *             National Center for Ecological Analysis and Synthesis
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
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package edu.ucsb.nceas.metacat.advancedsearch;

/**
 * @author dcosta
 * 
 * The LTERSite class holds information about the LTER sites.
 */
public class LTERSite {

  /* Instance variables */
  public String site;      // The three-letter uppercase acronym for this site.
  
  public final String[] sites = {
      "AND",
      "ARC",
      "BES",
      "BNZ",
      "CAP",
      "CCE",
      "CDR",
      "CWT",
      "FCE",
      "GCE",
      "HBR",
      "HFR",
      "JRN",
      "KBS",
      "KNZ",
      "LNO",
      "LUQ",
      "MCM",
      "MCR",
      "NTL",
      "NWT",
      "PAL",
      "PIE",
      "SBC",
      "SEV",
      "SGS",
      "VCR",
  };
  
  public final String[] siteKeywords = {
      "Andrews LTER",
      "Arctic LTER",
      "Baltimore Ecosystem Study",
      "Bonanza Creek",
      "Central Arizona - Phoenix Urban",
      "California Current Ecosystem",
      "Cedar Creek",
      "Coweeta",
      "Florida Coastal Everglades",
      "Georgia Coastal Ecosystems",
      "Hubbard Brook",
      "Harvard Forest",
      "Jornada Basin",
      "Kellogg Biological Station",
      "Konza Prairie",
      "LTER Network Office",
      "Luquillo",
      "McMurdo Dry Valleys",
      "Moorea Coral Reef",
      "North Temperate Lakes",
      "Niwot Ridge",
      "Palmer Station",
      "Plum Island Ecosystem",
      "Santa Barbara Coastal",
      "Sevilleta",
      "Shortgrass Steppe",
      "Virginia Coastal Reserve",
  };

  public final String[] siteNames = {
      "Andrews LTER",
      "Arctic LTER",
      "Baltimore Ecosystem Study",
      "Bonanza Creek LTER",
      "Central Arizona - Phoenix Urban LTER",
      "California Current Ecosystem",
      "Cedar Creek Natural History Area",
      "Coweeta LTER",
      "Florida Coastal Everglades LTER",
      "Georgia Coastal Ecosystems LTER",
      "Hubbard Brook LTER",
      "Harvard Forest LTER",
      "Jornada Basin LTER",
      "Kellogg Biological Station LTER",
      "Konza Prairie LTER",
      "LTER Network Office",
      "Luquillo LTER",
      "McMurdo Dry Valleys LTER",
      "Moorea Coral Reef",
      "North Temperate Lakes LTER",
      "Niwot Ridge LTER",
      "Palmer Station LTER",
      "Plum Island Ecosystem LTER",
      "Santa Barbara Coastal LTER",
      "Sevilleta LTER",
      "Shortgrass Steppe",
      "Virginia Coastal Reserve LTER",
  };


  /* Constructor 
   * 
   * @param site     The three-letter acronym for this LTER site.
   * 
   */
  public LTERSite(final String site) {
    if (site != null) {
      this.site = site.toUpperCase();
    }
  }
  
  
  /**
   * For a given site, return the packageId attribute search string for that
   * site's EML documents.
   * 
   * @return packageId   The first few letters that uniquely identify a site's
   *                     packageId. Typically "knb-lter-xyz", though there are
   *                     some exceptions.
   */
  public String getPackageId() {
    final String packageId;

    if (site == null) {
      packageId = "";
    }
    else if (site.equals("SEV")) {
      packageId = "sev.";
    }
    else {
      packageId = "knb-lter-" + site.toLowerCase();
    }
    
    return packageId;
  }
  

  /**
   * Get the keyword string for this site. This keyword string is OR'ed with the
   * packageId to find documents that originated from this site.
   * 
   * @return  siteKeyword  The site keyword string.
   */
  public String getSiteKeyword() {
    String siteKeyword = "";

    if (isValidSite()) {
      for (int i = 0; i < sites.length; i++) {
        if (site.equals(sites[i])) { 
          siteKeyword = siteKeywords[i];
          break;
        }
      }
    }
    
    return siteKeyword;
  }


  /**
   * For a given site, return system attribute search string for that
   * site's EML documents.
   * 
   * @return system  A string representing the system attribute used in a LTER
   *                 site's EML documents.
   */
  public String getSystem() {
    final String system;
    
    if (site == null) {
      system = "";
    }
    else if (site.equals("CAP")) {
      system = "ces_dataset";
    }
    else if (site.equals("CWT")) {
      system = "cwt-lter";
    }
    else {
      system = "knb";
    }
    
    return system;
  }
  

  /**
   * Boolean to determine whether a given string is a valid LTER site.
   */
  public boolean isValidSite() {
    boolean isValid = false;
    
    if (site != null) {   
      for (int i = 0; i < sites.length; i++) {
        if (site.equals(sites[i])) { 
          isValid = true;
          break;
        }
      }
    }
    
    return isValid;
  }

}
