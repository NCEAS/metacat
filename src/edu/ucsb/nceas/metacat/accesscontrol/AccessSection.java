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
import java.util.Stack;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


import edu.ucsb.nceas.metacat.NodeRecord;
import edu.ucsb.nceas.metacat.SubTree;

/**
 * A Class that represents an XML access rule. It include principal and
 * permission
 */
public class AccessSection extends SubTree
{
  //private String accessSectionId = null;
  private String permissionOrder = null;
  private Vector<AccessRule> accessRules = new Vector<AccessRule>();
  private String references = null;
  private String controlLevel = null;
  private Stack<NodeRecord> storedTmpNodeStack = null;
  private Vector<String> describedIdList = new Vector<String>();
  private long startedDescribesNodeId = -1;
  private String dataFileName = null;
  private Log logMetacat = LogFactory.getLog(AccessSection.class);


    /**
     * Set a storedTempNodeStack
     */
    public void setStoredTmpNodeStack(Stack<NodeRecord> myStack)
    {
      this.storedTmpNodeStack = myStack;
    }

    /**
     * Get storedTempNodeStack
     */
    public Stack<NodeRecord> getStoredTmpNodeStack()
    {
      return this.storedTmpNodeStack;
    }

    /**
     * Set a controllevel
     */
    public void setControlLevel(String myLevel)
    {
      this.controlLevel = myLevel;
    }

    /**
     * Get controllevel
     */
    public String getControlLevel()
    {
      return this.controlLevel;
    }

   /**
     * Set a permissionOrder
     */
    public void setPermissionOrder(String myOrder)
    {
      this.permissionOrder = myOrder;
    }

    /**
     * Get permissionOrder
     */
    public String getPermissionOrder()
    {
      return this.permissionOrder;
    }

    /** Add an access rule */
    public void addAccessRule(AccessRule rule)
    {
      this.accessRules.addElement(rule);
    }

    /** Get all access rule */
    public Vector<AccessRule> getAccessRules()
    {
      return this.accessRules;
    }

    /** Set a references */
    public void setReferences(String myReferences)
    {
      this.references = myReferences;
    }

    /** Get the references */
    public String getReferences()
    {
      return this.references;
    }

    /** Set a described id list (in data access part)*/
    public void setDescribedIdList(Vector<String> list)
    {
      describedIdList = list;
    }

    /** Get a described id list */
    public Vector<String> getDescribedIdList()
    {
      return describedIdList;
    }

    /** Set the start "describes" node id*/
   public void setStartedDescribesNodeId(long id)
   {
     startedDescribesNodeId = id;
   }

   /** Get the start described id */
   public long getStartedDescribesNodeId()
   {
     return startedDescribesNodeId;
   }
   
   /** Set the dataFileName */
   public void setDataFileName(String fileName)
   {
     dataFileName = fileName;
   }
   
   /** Get the dataFileName */
   public String getDataFileName()
   {
     return dataFileName;
   }

    /** Method to copy a accesssection object to a new one */
    public void copyPermOrderAndAccessRules(AccessSection newAccessSection)
    {

      // set parameters
      logMetacat.info("Copy permission order: " +
                                this.getPermissionOrder());
      newAccessSection.setPermissionOrder(this.getPermissionOrder());
      Vector<AccessRule> accessRuleVector = this.getAccessRules();
      // go through access rule vector
      for (int i=0; i< accessRuleVector.size(); i++)
      {
        AccessRule access = (AccessRule)accessRuleVector.elementAt(i);
        newAccessSection.addAccessRule(access);
      }
    }

}
