/**
 *  '$RCSfile$'
 *    Purpose: A Class that represents an XML Text node and its contents,
 *             and can build itself from a database connection
 *  Copyright: 2000 Regents of the University of California and the
 *             National Center for Ecological Analysis and Synthesis
 *    Authors: Matt Jones
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
 */

package edu.ucsb.nceas.metacat.accesscontrol;

import java.util.Vector;
import org.apache.log4j.Logger;
/**
 * A Class that represents an XML access rule. It include principal and 
 * permission
 */
public class AccessRule
{
  private String permissionType = null;
  private Vector<String> principal = new Vector<String>();
  private int permission = 0;
  private Logger logMetacat = Logger.getLogger(AccessRule.class);
  
  /** Set the permssionType */
  public void setPermissionType(String type)
  {
    permissionType = type;
  }
  
  public String getPermissionType()
  {
     return permissionType;
  }
    
  /** Set the a principle */
  public void addPrincipal(String newPrincipal) 
  {
    this.principal.addElement(newPrincipal);
  }

  /** Get the principle vector */
  public Vector<String> getPrincipal() 
  {
    return principal;
  }

  /** 
   * Set a permission. 
   */
  public void setPermission(int myPermission) 
  {
    permission = myPermission;
  }
    
  /**
   * Get permission
   */
  public int getPermission()
  {
    return permission;
  }
  
  /**
   * Copy a AccessRule to another accessrule object
   */
  public Object clone()
  {
    // create a new object
    AccessRule newRule = new AccessRule();
    // set permissiontype
    logMetacat.info("copy permission type: "+
                              this.getPermissionType());
    newRule.setPermissionType(this.getPermissionType());
    // set permission
    logMetacat.info("copy permission: "+
                              this.getPermission());
    newRule.setPermission(this.getPermission());
    // walk through all the principals
    Vector principalVector = this.getPrincipal();
    for (int i=0; i<principalVector.size(); i++)
    {
      String name = (String)principalVector.elementAt(i);
      logMetacat.info("copy principle: "+ name);
      // Add this name to newrules
      newRule.addPrincipal(name);
    }
    return newRule;
  }
  
 
}//AccessRule
