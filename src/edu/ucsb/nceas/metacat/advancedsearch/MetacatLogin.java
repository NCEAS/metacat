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

import edu.ucsb.nceas.metacat.client.*;


/**
 * @author dcosta
 * 
 * MetacatLogin class executes a Metacat login using the Metacat client.
 */
public class MetacatLogin  {

  /* Object variables */
  private LoginBean loginBean = null;

  
  /**
   * Constructor. Initializes the loginBean object variable.
   * 
   * @param loginBean  the LoginBean object, holds username and password
   */
  public MetacatLogin(final LoginBean loginBean) {
    this.loginBean = loginBean;
  }

  
  /**
   * Executes the Metacat login.
   * 
   * @param metacatURL      URL to the metacat servlet
   * @param metacat         a Metacat object, possibly null
   * 
   * @return loginSuccess   true if metacat login was successful, else false
   */
  public boolean executeLogin(final String metacatURL, final Metacat metacat) {
    final String DN;                            // LDAP distinguished name
    boolean loginSuccess = false;
    MetacatHelper metacatHelper = new MetacatHelper();
    String metacatResponse = "";
    final String organization = loginBean.getOrganization();
    final String password = loginBean.getPassword();
    final String username = loginBean.getUsername();
    
    if (
        username == null || 
        organization == null || 
        password == null || 
        username.equals("") || 
        organization.equals("") || 
        password.equals("")) 
    {
      return loginSuccess;
    }
    else {    
      System.err.println("Metacat URL: " + metacatURL);

      try {
        DN = metacatHelper.constructDN(username, organization);    
        metacatResponse = metacat.login(DN, password);
        System.err.println("metacatResponse:\n" + metacatResponse);
        
        if (metacatResponse.indexOf("Authentication successful") > -1) {
          loginSuccess = true;
        }
      } 
      catch (MetacatAuthException mae) {
        System.err.println("MetacatAuthException:\n" + mae.getMessage());
      } 
      catch (MetacatInaccessibleException mie) {
        System.err.println("Metacat Inaccessible:\n" + mie.getMessage());
      }
      catch (Exception e) {
        System.err.println("General exception:\n" + e.getMessage());
        e.printStackTrace();
      }
    }
    
    return loginSuccess;
  }

}
