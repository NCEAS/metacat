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

import org.kepler.web.client.DynaTableDataProvider.RowDataAcceptor;

import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DockPanel;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.TableListener;
import com.google.gwt.user.client.ui.SourcesTableEvents;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.Frame;

public class DynaTableWidget extends Composite implements TableListener{

  private class NavBar extends Composite implements ClickListener {

    public NavBar() {
      initWidget(bar);
      bar.setStyleName("navbar");
      status.setStyleName("status");
      
      HorizontalPanel searchPanel = new HorizontalPanel();
      searchField.setVisibleLength(12);
      searchButton.setStyleName("searchButton");
      searchPanel.add(searchField);
      searchPanel.add(searchButton);
      
      HorizontalPanel buttons = new HorizontalPanel();
      buttons.add(gotoFirst);
      buttons.add(gotoPrev);
      buttons.add(gotoNext);
      buttons.add(gotoLast);
      bar.add(buttons, DockPanel.EAST);
      bar.setCellHorizontalAlignment(buttons, DockPanel.ALIGN_RIGHT);
      
      HorizontalPanel statusPanel = new HorizontalPanel();
      statusPanel.add(statusImage);
      statusPanel.add(new HTML("&nbsp;&nbsp;"));
      statusPanel.add(status);
      bar.add(statusPanel, DockPanel.CENTER);
      
      bar.setVerticalAlignment(DockPanel.ALIGN_MIDDLE);
      bar.setCellHorizontalAlignment(statusPanel, HasAlignment.ALIGN_RIGHT);
      bar.setCellVerticalAlignment(statusPanel, HasAlignment.ALIGN_MIDDLE);
      bar.setCellWidth(statusPanel, "100%");
      
      bar.add(searchPanel, DockPanel.WEST);
      

      // Initialize prev & first button to disabled.
      //
      gotoPrev.setEnabled(false);
      gotoFirst.setEnabled(false);
    }

    public void onClick(Widget sender) {
      if (sender == gotoNext) {
        startRow += getDataRowCount();
        selectRow(-1);
        refresh();
      } else if (sender == gotoPrev) {
        startRow -= getDataRowCount();
        if (startRow < 0) {
          startRow = 0;
        }
        selectRow(-1);
        refresh();
      } else if (sender == gotoFirst) {
        startRow = 0;
        selectRow(-1);
        refresh();
      } else if (sender == gotoLast) {
        startRow = getMaxRowCount() - (getMaxRowCount() % getRowInc());
        selectRow(-1);
        refresh();
      } else if(sender == searchButton) {
        searchString = searchField.getText();
        startRow = 0;
        selectRow(-1);
        search(searchField.getText());
      }
    }

    public final DockPanel bar = new DockPanel();
    public final Button gotoFirst = new Button("&lt;&lt;", this);
    public final Button gotoNext = new Button("&gt;", this);
    public final Button gotoPrev = new Button("&lt;", this);
    public final Button gotoLast = new Button("&gt;&gt;", this);
    public final Button searchButton = new Button("Search", this);
    public final TextBox searchField = new TextBox();
    public final HTML status = new HTML();
    public final Image statusImage = new Image("spinner-anim.gif");
  }

  private class RowDataAcceptorImpl implements RowDataAcceptor {
    
    public void accept(int startRow, String[][] data) {
      int destRowCount = getDataRowCount();
      int destColCount = grid.getCellCount(0);
      assert (data.length <= destRowCount) : "Too many rows";

      int srcRowIndex = 0;
      int srcRowCount = data.length;
      int destRowIndex = 1; // skip navbar row
      for (; srcRowIndex < srcRowCount; ++srcRowIndex, ++destRowIndex) {
        String[] srcRowData = data[srcRowIndex];
        assert (srcRowData.length == destColCount) : " Column count mismatch";
        for (int srcColIndex = 0; srcColIndex < destColCount; ++srcColIndex) {
          String cellHTML = srcRowData[srcColIndex];
          grid.setText(destRowIndex, srcColIndex, cellHTML);
          grid.getCellFormatter().setStyleName(destRowIndex, 0, "infoIcon");
        }
      }

      // Clear remaining table rows.
      //
      boolean isLastPage = false;
      for (; destRowIndex < destRowCount + 1; ++destRowIndex) {
        isLastPage = true;
        for (int destColIndex = 0; destColIndex < destColCount; ++destColIndex) {
          grid.clearCell(destRowIndex, destColIndex);
        }
      }

      // Synchronize the nav buttons.
      //
      navbar.gotoNext.setEnabled(!isLastPage);
      navbar.gotoFirst.setEnabled(startRow > 0);
      navbar.gotoPrev.setEnabled(startRow > 0);
      navbar.gotoLast.setEnabled(!isLastPage);

      // Update the status message.
      //
      setStatusText((startRow + 1) + " - " + (startRow + srcRowCount) + " of " + 
        getMaxRowCount());
      setStatusImage(false);
      currentEndRecordNum = startRow + srcRowCount;
      currentBeginRecordNum = startRow + 1;
    }

    public void failed(Throwable caught) {
      String msg = "Failed to access data";
      if (caught != null) {
        msg += ": " + caught.getMessage();
      }
      setStatusText(msg);
    }
  }
  
  private static class InfoDialog extends DialogBox implements ClickListener {
    
    public InfoDialog(String title, String docid) {
      
      setText(title);

      Frame iframe = new Frame("http://library.kepler-project.org/kepler/metacat?action=read&docid=" + docid + "&qformat=kepler");
      Button closeButton = new Button("Close", this);
      /*HTML msg = new HTML(
        "<center>This is an example of a standard dialog box component.<br>  "
          + "You can put pretty much anything you like into it,<br>such as the "
          + "following IFRAME:</center>", true);*/

      DockPanel dock = new DockPanel();
      dock.setSpacing(4);

      dock.add(closeButton, DockPanel.SOUTH);
      //dock.add(msg, DockPanel.NORTH);
      dock.add(iframe, DockPanel.CENTER);

      dock.setCellHorizontalAlignment(closeButton, DockPanel.ALIGN_RIGHT);
      dock.setCellWidth(iframe, "100%");
      dock.setWidth("100%");
      iframe.setWidth("80em");
      iframe.setHeight("50em");
      setWidget(dock);
    }

    public void onClick(Widget sender) {
      hide();
    }
  }

  public DynaTableWidget(DynaTableDataProvider provider, String[] columns,
      String[] columnStyles, int rowCount) {
    rowInc = rowCount;
    if (columns.length == 0) {
      throw new IllegalArgumentException(
        "expecting a positive number of columns");
    }

    if (columnStyles != null && columns.length != columnStyles.length) {
      throw new IllegalArgumentException("expecting as many styles as columns");
    }

    this.provider = provider;
    initWidget(outer);
    grid.setStyleName("table");
    grid.addTableListener(this);
    outer.add(navbar, DockPanel.NORTH);
    outer.add(grid, DockPanel.CENTER);
    initTable(columns, columnStyles, rowCount);
    setStyleName("DynaTable-DynaTableWidget");
  }

  private void initTable(String[] columns, String[] columnStyles, int rowCount) {
    // Set up the header row.  It's one greater than the number of visible rows.
    //

    grid.resize(rowCount+1, columns.length);
    for (int i = 0, n = columns.length; i < n; i++) {
      grid.setText(0, i, columns[i]);
      if (columnStyles != null) {
        grid.getCellFormatter().setStyleName(0, i, columnStyles[i] + " header");
      }
    }
  }

  public void setStatusText(String text) {
    navbar.status.setText(text);
  }

  public void clearStatusText() {
    navbar.status.setHTML("&nbsp;");
  }
  
  public void setStatusImage(boolean on)
  {
    //if on turn the image on
    navbar.statusImage.setVisible(on);
  }

  public void refresh() {
    // Disable buttons temporarily to stop the user from running off the end.
    //
    search(searchString);
  }
  
  public void search(String searchTerm) {
    navbar.gotoFirst.setEnabled(false);
    navbar.gotoPrev.setEnabled(false);
    navbar.gotoNext.setEnabled(false);
    navbar.gotoLast.setEnabled(false);

    setStatusText("Searching...");
    setStatusImage(true);
    provider.updateRowData(startRow, grid.getRowCount() - 1, acceptor, searchTerm);
    
    for(int i=0; i<grid.getRowCount(); i++)
    {
      if(grid.getText(i, 1) == null || 
         grid.getText(i, 1).trim().equals("") || 
         grid.getHTML(i,1).equals("&nbsp;"))
      { //remove the info icon style if there is nothing in the row
        grid.getCellFormatter().removeStyleName(i, 0, "infoIcon");
      }
    }
  }

  public void setRowCount(int rows) {
    grid.resizeRows(rows);
  }
  
  public void onCellClicked(SourcesTableEvents sender, int row, int cell) {
    // Select the row that was clicked (-1 to account for header row).
    if(((row + currentBeginRecordNum) - 2) < currentEndRecordNum)
    {
      if (row > 0)
      {
        selectRow(row - 1);
      }
      
      if(cell == 0)
      {
        //create the dialog and send the name and docid to the dialog
        DialogBox dlg = new InfoDialog(grid.getText(row,1), grid.getText(row,2));
        int left = 50;
        int top = 50;
        dlg.setPopupPosition(left, top);
        dlg.show();
      }
    }
  }
  
  /**
   * Selects the given row (relative to the current page).
   * 
   * @param row the row to be selected
   */
  private void selectRow(int row) {

    styleRow(selectedRow, false);
    styleRow(row, true);

    selectedRow = row;
  }

  private void styleRow(int row, boolean selected) {
    if (row != -1) {
      if (selected)
      {
        //grid.getRowFormatter().addStyleName(row + 1, "SelectedRow");
        //don't select the info icon
        grid.getCellFormatter().setStyleName(row + 1, 1, "SelectedRow");
        grid.getCellFormatter().setStyleName(row + 1, 2, "SelectedRow");
      }
      else
      {
        //grid.getRowFormatter().removeStyleName(row + 1, "SelectedRow");
        grid.getCellFormatter().removeStyleName(row + 1, 1, "SelectedRow");
        grid.getCellFormatter().removeStyleName(row + 1, 2, "SelectedRow");
      }
    }
  }
  
  private int getDataRowCount() {
    return grid.getRowCount() - 1;
  }
  
  public int getMaxRowCount() {
    return maxRowCount;
  }
  
  public void setMaxRowCount(int max) {
    maxRowCount = max;
  }
  
  private int getRowInc() {
    return rowInc;
  }

  private final RowDataAcceptor acceptor = new RowDataAcceptorImpl();
  private final NavBar navbar = new NavBar();
  private final DockPanel outer = new DockPanel();
  private final DynaTableDataProvider provider;
  private int startRow = 0;
  private Grid grid = new Grid();
  private int maxRowCount = 0;
  private int rowInc;
  private int currentEndRecordNum = -1;
  private int currentBeginRecordNum = -1;
  private String searchString = null;
  private int startIndex, selectedRow = -1;
}
