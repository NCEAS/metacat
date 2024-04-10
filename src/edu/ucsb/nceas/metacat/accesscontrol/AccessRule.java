package edu.ucsb.nceas.metacat.accesscontrol;

import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A Class that represents an XML access rule. It include principal and 
 * permission
 */
public class AccessRule
{
  private String permissionType = null;
  private Vector<String> principal = new Vector<String>();
  private int permission = 0;
  private Log logMetacat = LogFactory.getLog(AccessRule.class);
  
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
