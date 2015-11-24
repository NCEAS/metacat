/*
 * Copyright 2006 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.kepler.web.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.ServiceDefTarget;
import com.google.gwt.user.client.ui.Composite;

public class MetacatResultsWidget extends Composite {
  
  
  private final MetacatProvider metacatProvider = new MetacatProvider();
  private final DynaTableWidget dynaTable;
  private Command pendingRefresh;
  private boolean queryPerformed = false;
  private String lastSearch = null;

  public class MetacatProvider implements DynaTableDataProvider {

    public MetacatProvider() {
      // Initialize the service.
      //
      keplerService = (KeplerServiceAsync) GWT.create(KeplerService.class);

      // By default, we assume we'll make RPCs to a servlet, but see
      // updateRowData(). There is special support for canned RPC responses.
      // (Which is a totally demo hack, by the way :-)
      // 
      ServiceDefTarget target = (ServiceDefTarget) keplerService;
      
      // Use a module-relative URLs to ensure that this client code can find 
      // its way home, even when the URL changes (as might happen when you 
      // deploy this as a webapp under an external servlet container). 

      //String moduleRelativeURL = GWT.getModuleBaseURL() + "kepler";      
      //String moduleRelativeURL = "http://library.kepler-project.org/kepler/gwt";
      //String moduleRelativeURL = "http://offhegoes.kicks-ass.net:8080/kepler/gwt";
      String moduleRelativeURL = "http://library.kepler-project.org/kepler/gwt";
      System.out.println("connection url: " + moduleRelativeURL);
      target.setServiceEntryPoint(moduleRelativeURL);
    }

    public void updateRowData(final int startRow, final int maxRows,
        final RowDataAcceptor acceptor, String searchTerm) 
    {
      // Check the simple cache first.
      if(searchTerm == null)
      {
        searchTerm = "%";
      }
      
      if(queryPerformed && lastSearch.trim().equals(searchTerm.trim()))
      {
          // Use the cached batch.
          pushResults(acceptor, startRow, maxRows, lastResult);
          return;
      }
      else
      {
        lastSearch = searchTerm;
      }

      /*keplerService.login("uid=kepler,o=unaffiliated,dc=ecoinformatics,dc=org", 
        new AsyncCallback() {
          public void onFailure(Throwable caught) {
          acceptor.failed(caught);
        }

        public void onSuccess(Object result) {
          //handle successful login
        }
      });*/
      
      //do the query
      if(searchTerm == null)
      {
        lastSearch = "%";
      }
      
      keplerService.query(lastSearch, "", new AsyncCallback() {
        public void onFailure(Throwable caught) {
          acceptor.failed(caught);
        }

        public void onSuccess(Object result) {
          //handle successful query
          MetacatQueryResult[] queryResult = (MetacatQueryResult[])result;
          lastStartRow = startRow;
          lastMaxRows = maxRows;
          lastResult = queryResult;
          queryPerformed = true;
          dynaTable.setMaxRowCount(queryResult.length);
          pushResults(acceptor, startRow, maxRows, queryResult);
        }

      });

    }

    private void pushResults(RowDataAcceptor acceptor, int startRow,
        int maxRows, MetacatQueryResult[] queryResults) {
      int offset = (queryResults.length - 0) - startRow;
      if(offset > maxRows)
      {
        offset = maxRows;
      }
      
      String[][] rows = new String[offset][];
      for (int i = 0; i < offset; i++) {
        MetacatQueryResult result = queryResults[startRow + i];
        rows[i] = new String[3];
        rows[i][0] = " ";
        rows[i][1] = result.getName();
        rows[i][2] = result.getDocid();
      }
      acceptor.accept(startRow, rows);
    }

    private final KeplerServiceAsync keplerService;
    private int lastMaxRows = -1;
    private MetacatQueryResult[] lastResult;
    private int lastStartRow = -1;
  }

  public MetacatResultsWidget(int visibleRows) {
    String[] columns = new String[]{"", "Name", "DocId"};
    String[] styles = new String[]{"", "name", "docid"};
    dynaTable = new DynaTableWidget(metacatProvider, columns, styles, visibleRows);
    initWidget(dynaTable);
  }

  protected void onLoad() {
    dynaTable.refresh();
  }
}
