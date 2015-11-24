/**
 *  '$RCSfile$'
 *    Purpose: A class that handles xml messages passed by the 
 *             replication handler
 *  Copyright: 2000 Regents of the University of California and the
 *             National Center for Ecological Analysis and Synthesis
 *    Authors: Chad Berkley
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

package edu.ucsb.nceas.metacat.replication;


import java.util.Vector;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/** 
 * A Class implementing callback bethods for the SAX parser to
 * call when processing the XML messages from the replication handler
 */
public class ReplMessageHandler extends DefaultHandler 
{
  private Vector<String> indivUpdate = new Vector<String>();
  private Vector<Vector<String>> updates = new Vector<Vector<String>>();
  private Vector<String> indivSystemMetadata = new Vector<String>();
  private Vector<Vector<String>> systemMetadataEntries = new Vector<Vector<String>>();
  private Vector<String> indivDelete = new Vector<String>();
  private Vector<Vector<String>> deletes = new Vector<Vector<String>>();
  private Vector<String> indivRevision = new Vector<String>();
  private Vector<Vector<String>> revisions = new Vector<Vector<String>>();
  private String server;
  private String dataFile;
  private boolean update = false;
  private boolean delete = false;
  private boolean revision = false;
  private boolean systemMetadata = false;
  String currentTag = new String();
  StringBuffer textBuffer = null;
  
  public ReplMessageHandler()
  {
  }
  
  /**
   * This method starts a new vector for each updatedDocument tag.
   */
  public void startElement(String uri, String localName, String qName, 
                           Attributes attributes) throws SAXException
  {
    textBuffer = new StringBuffer();
    currentTag = localName;
    if(localName.equals("updatedDocument"))
    {
      indivUpdate = new Vector<String>();
      update = true;
    }
    else if(localName.equals("deletedDocument"))
    {
      indivDelete = new Vector<String>();
      delete = true;
    }
    else if (localName.equals("revisionDocument"))
    {
      indivRevision = new Vector<String>();
      revision = true;
    }
    else if (localName.equals("updatedSystemMetadata"))
    {
    	indivSystemMetadata = new Vector<String>();
    	systemMetadata = true;
    }
            
  }
  
  /**
   * This method write the indivUpdate to updates when it finds the end of
   */
  public void endElement(String uri, String localName, String qName) 
              throws SAXException
  {
    if(currentTag.equals("docid") && update)
    {
      indivUpdate.add(textBuffer.toString());
    }
    else if(currentTag.equals("docid") && delete)
    {
      indivDelete.add(textBuffer.toString());
    }
    else if (currentTag.equals("docid") && revision)
    {
      indivRevision.add(textBuffer.toString());
    }
    else if (currentTag.equals("guid") && systemMetadata)
    {
      indivSystemMetadata.add(textBuffer.toString());
      indivSystemMetadata.add(server);
    }
    
    if(currentTag.equals("rev") && update)
    {
      indivUpdate.add(textBuffer.toString());
      indivUpdate.add(server);
    }
    else if(currentTag.equals("rev") && delete)
    {
      indivDelete.add(textBuffer.toString());
      indivDelete.add(server);
    }
    else if(currentTag.equals("rev") && revision)
    {
      indivRevision.add(textBuffer.toString());
      indivRevision.add(server);
    }
    
    if(currentTag.equals("date_updated") && update)
    {
      indivUpdate.add(textBuffer.toString());
      indivUpdate.add(server);
    }
    else if(currentTag.equals("date_updated") && delete)
    {
      indivDelete.add(textBuffer.toString());
      indivDelete.add(server);
    }
    else if(currentTag.equals("date_updated") && revision)
    {
      indivRevision.add(textBuffer.toString());
      indivRevision.add(server);
    }
    
    if(currentTag.equals("server"))
    {
      server = textBuffer.toString();
    }
    
    //Adding data file attribute into indivUpdate vector
    if (currentTag.equals("datafile"))
    {
      dataFile = textBuffer.toString();
      if (update)
      {
        indivUpdate.add(dataFile);
      }
      else if (revision)
      {
        indivRevision.add(dataFile);   
      }
          
      
    }
    
    if(localName.equals("updatedDocument"))
    {
      updates.add(new Vector<String>(indivUpdate));
      update = false;
    }
    else if(localName.equals("deletedDocument"))
    {
      deletes.add(new Vector<String>(indivDelete));
      delete = false;
    }
    else if (localName.equals("revisionDocument"))
    {
       revisions.add(new Vector<String>(indivRevision));
       revision = false;
    }
    else if (localName.equals("updatedSystemMetadata"))
    {
       systemMetadataEntries.add(new Vector<String>(indivSystemMetadata));
       systemMetadata = false;
    }
  }
  
  /**
   * Take the data out of the docid and date_updated fields
   */
  public void characters(char[] ch, int start, int length) throws SAXException
  {
    textBuffer.append(new String(ch, start,length));
  }
  
  public Vector<Vector<String>> getUpdatesVect()
  {
    return updates;
  }
  
  public Vector<Vector<String>> getDeletesVect()
  {
    return deletes;
  }
  
  public Vector<Vector<String>> getRevisionsVect()
  {
     return revisions;
  }
  
  public Vector<Vector<String>> getSystemMetadataVect()
  {
     return systemMetadataEntries;
  }
  
  /**
   * Gets the server name
   * @return
   */
  public String getServerName()
  {
	  return server;
  }
}
