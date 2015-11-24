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

import java.io.Serializable;

/**
 * @author dcosta
 * 
 * Bean to store login form properties and values.
 *
 */
public class LoginBean implements Serializable {
  
  static final long serialVersionUID = 0;  // Needed for Eclipse warning.

  private String organization;      /* LDAP organization, e.g. "LTER" */
  private String password;          /* login password string */
  private String username;          /* login username string */
   
  /**
   * @return Returns the organization.
   */
  public String getOrganization() {
    return organization;
  }
  
  
  /**
   * @param organization The organization to set.
   */
  public void setOrganization(final String organization) {
    this.organization = organization;
  }
  
  
  /**
   * @return Returns the password.
   */
  public String getPassword() {
    return password;
  }
  
  
  /**
   * @param password The password to set.
   */
  public void setPassword(final String password) {
    this.password = password;
  }
  
  
  /**
   * @return Returns the username.
   */
  public String getUsername() {
    return username;
  }
  
  
  /**
   * @param username The username to set.
   */
  public void setUsername(final String username) {
    this.username = username;
  }
  
}

