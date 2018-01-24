package org.kepler.web.client;

import com.google.gwt.user.client.rpc.*;

import java.io.*;
import java.util.*;

public interface KeplerServiceAsync 
{
  public void query(String query, String sessionid, AsyncCallback callback);
  public void login(String user, String pass, AsyncCallback callback);
  public void logout(AsyncCallback callback);
  public void read(String docid, String sessionid, AsyncCallback callback);
}
