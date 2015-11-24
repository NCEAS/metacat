package org.kepler.web.client;

import com.google.gwt.user.client.rpc.*;
import com.google.gwt.user.client.rpc.IsSerializable;

import java.io.*;
import java.util.*;

public class MetacatQueryResult implements IsSerializable
{
  private String name = "";
  private String docid = "";
  private String description = "";
  
  public MetacatQueryResult()
  {
    
  }
  
  public MetacatQueryResult(String name, String docid, String description)
  {
    this.name = name;
    this.docid = docid;
    this.description = description;
  }
  
  public String getName()
  {
    return name;
  }
  
  public String getDocid()
  {
    return docid;
  }
  
  public String getDescription()
  {
    return description;
  }
  
  public void setName(String name)
  {
    this.name = name;
  }
  
  public void setDocid(String docid)
  {
    this.docid = docid;
  }
  
  public void setDescription(String desc)
  {
    this.description = description;
  }
}
