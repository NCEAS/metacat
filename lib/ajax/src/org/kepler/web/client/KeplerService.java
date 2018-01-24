package org.kepler.web.client;

import java.io.*;
import java.util.*;

import com.google.gwt.user.client.rpc.*;

public interface KeplerService extends RemoteService 
{
  public MetacatQueryResult[] query(String query, String sessionid);
  public String login(String user, String pass);
  public String logout();
  public String read(String docid, String sessionid);
}
