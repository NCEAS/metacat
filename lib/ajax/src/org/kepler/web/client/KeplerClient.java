package org.kepler.web.client;

//import org.kepler.web.service.*;

import com.google.gwt.core.client.*;
import com.google.gwt.user.client.rpc.*;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Widget;


/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class KeplerClient implements EntryPoint 
{

  /**
   * This is the entry point method.
   */
  public void onModuleLoad() 
  {    
    //add the result widget to the results slot in the html
    MetacatResultsWidget mrwidget = new MetacatResultsWidget(15);
    RootPanel.get("results").add(mrwidget);
  }
  
}
