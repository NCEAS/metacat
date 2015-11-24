/**
 *  '$RCSfile$'
 *  Copyright: 2004 University of New Mexico and the 
 *                  Regents of the University of California
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
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package edu.ucsb.nceas.metacat.harvesterClient;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.Properties;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import javax.swing.text.Document;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

/**
 * The Harvest List Editor reads a Harvest List XML file and displays it as a 
 * JTable. Allows user to add, modify, or delete <Document> elements in the
 * Harvest List, and save changes back to the disk.
 * 
 */
public class HarvestListEditor extends JFrame implements ActionListener {

  String clipboardDocumentType = "";
  String clipboardDocumentURL = "";
  String clipboardIdentifier = "";
  String clipboardRevision = "";
  String clipboardScope = "";
  JButton copyButton;
  JButton cutButton;
  String defaultDocumentType = "eml://ecoinformatics.org/eml-2.0.1";
  String defaultDocumentURL = "http://";
  String defaultHarvestList = "";
  String defaultIdentifier = "1";
  String defaultRevision = "1";
  String defaultScope = "dataset";
  Document doc;
  JScrollPane docPane;
  JFileChooser fileChooser = new JFileChooser();
  File harvestListFile;
  boolean harvestListHasChanged = false;
  JMenuBar menuBar;
  final int numColumns = 6;
  final int numRows = 1200;
  JButton pasteButton;
  JButton pasteDefaultsButton;
  Properties properties;
  private String schemaLocation = 
    "eml://ecoinformatics.org/harvestList harvestList.xsd";
  int selectedRow = -1;
  final JTable table;
  TableModel tableModel;
  File tempFile;
  String title = "Harvest List Editor";
  
  // Menu items
  JMenuItem exitMenuItem = new JMenuItem("Exit");
  JMenuItem newMenuItem = new JMenuItem("New");
  JMenuItem openMenuItem = new JMenuItem("Open...");
  JMenuItem saveMenuItem = new JMenuItem("Save");
  JMenuItem saveAsMenuItem = new JMenuItem("Save As...");
  JMenuItem validateMenuItem = new JMenuItem("Validate");
  

  /**
   * The main program. Instantiate the main frame, a HarvestListEditor object.
   * 
   * @param args
   */
  public static void main(String[] args) {
    HarvestListEditor harvestListEditor = new HarvestListEditor();
  }


  /**
   * Constructor for HarvestListEditor class.
   */
  public HarvestListEditor() {
    super("Harvest List Editor");

    JPanel buttonPanel = new JPanel();
    String[] fileItems = 
                  new String[] {"New", "Open...", "Save", "Save As...", "Exit"};
    JMenu fileMenu = new JMenu("File");
    char[] fileShortCuts = {'N', 'O', 'S', 'A', 'X'};
    TableColumn tableColumn;
    
    loadProperties();
    setSize(1000, 400);
    setDefaultCloseOperation(EXIT_ON_CLOSE);
    menuBar = new JMenuBar();
    
    /*
     * Add menu items to the File menu
     */
    newMenuItem.setAccelerator(KeyStroke.getKeyStroke('N',
									                  java.awt.Event.CTRL_MASK,
                                                      false));
    newMenuItem.addActionListener(this);
    fileMenu.add(newMenuItem);

    openMenuItem.setAccelerator(KeyStroke.getKeyStroke('O',
									                   java.awt.Event.CTRL_MASK,
                                                       false));
    openMenuItem.addActionListener(this);
    fileMenu.add(openMenuItem);

    saveMenuItem.setAccelerator(KeyStroke.getKeyStroke('S',
									                   java.awt.Event.CTRL_MASK,
                                                       false));
    saveMenuItem.addActionListener(this);
    saveMenuItem.setEnabled(false);
    fileMenu.add(saveMenuItem);

    saveAsMenuItem.setAccelerator(KeyStroke.getKeyStroke('A',
									                   java.awt.Event.CTRL_MASK,
                                                       false));
    saveAsMenuItem.addActionListener(this);
    fileMenu.add(saveAsMenuItem);

    validateMenuItem.setAccelerator(KeyStroke.getKeyStroke('V',
									                   java.awt.Event.CTRL_MASK,
                                                       false));
    validateMenuItem.addActionListener(this);
    fileMenu.add(validateMenuItem);

    exitMenuItem.setAccelerator(KeyStroke.getKeyStroke('X',
									                   java.awt.Event.CTRL_MASK,
                                                       false));
    exitMenuItem.addActionListener(this);
    fileMenu.add(exitMenuItem);
    
    menuBar.add(fileMenu);      // Add the File menu to the menu bar
    setJMenuBar(menuBar);       // Set the frame's menu bar to this menu bar

    //table = new JTable(numRows, numColumns);
    table = new JTable(new HarvestListTableModel());
    table.setPreferredScrollableViewportSize(new Dimension(900, 300));
    tableColumn = table.getColumnModel().getColumn(0);
    tableColumn.setPreferredWidth(30);
    tableColumn = table.getColumnModel().getColumn(1);
    tableColumn.setPreferredWidth(200);
    tableColumn = table.getColumnModel().getColumn(2);
    tableColumn.setPreferredWidth(50);
    tableColumn = table.getColumnModel().getColumn(3);
    tableColumn.setPreferredWidth(50);
    tableColumn = table.getColumnModel().getColumn(4);
    tableColumn.setPreferredWidth(250);
    tableColumn = table.getColumnModel().getColumn(5);
    tableColumn.setPreferredWidth(320);
    
    table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    tableModel = table.getModel();
    initHarvestList();

    //Ask to be notified of selection changes.
    ListSelectionModel rowSM = table.getSelectionModel();

    rowSM.addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent e) {
        ListSelectionModel lsm;

        //Ignore extra messages.
        if (e.getValueIsAdjusting()) return;

        lsm = (ListSelectionModel)e.getSource();

        // If now row is selected, disable all buttons.
        if (lsm.isSelectionEmpty()) {
          selectedRow = -1;
          cutButton.setEnabled(false);
          copyButton.setEnabled(false);
          pasteButton.setEnabled(false);
          pasteDefaultsButton.setEnabled(false);
        }
        // If a row is selected, manage the buttons based on the selected row
        // and the current clipboard values.
        else {
          selectedRow = lsm.getMinSelectionIndex();
          manageButtons(selectedRow);
        }
      }
    });

    docPane = new JScrollPane(table);
    getContentPane().add(docPane, BorderLayout.CENTER);
    
    cutButton = new JButton("Cut");
    copyButton = new JButton("Copy");
    pasteButton = new JButton("Paste");
    pasteDefaultsButton = new JButton("Paste Defaults");

    // Action listener for the Copy button.    
    copyButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ae) {				
        copyRow(selectedRow);
        manageButtons(selectedRow);
        harvestListHasChanged = true;
			}
		}
    );

    // Action listener for the Cut button.    
    cutButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ae) {				
        cutRow(selectedRow);
        manageButtons(selectedRow);
        harvestListHasChanged = true;
			}
		}
    );

    // Action listener for the Paste button.    
    pasteButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        pasteRow(selectedRow);
        manageButtons(selectedRow);
        harvestListHasChanged = true;
			}
		}
    );

    // Action listener for the Paste Defaults button.    
    pasteDefaultsButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        pasteDefaultValues(selectedRow);
        manageButtons(selectedRow);
        harvestListHasChanged = true;
			}
		}
    );

    cutButton.setEnabled(false);
    copyButton.setEnabled(false);
    pasteButton.setEnabled(false);
    pasteDefaultsButton.setEnabled(false);
    buttonPanel.add(cutButton);
    buttonPanel.add(copyButton);
    buttonPanel.add(pasteButton);
    buttonPanel.add(pasteDefaultsButton);
    buttonPanel.setOpaque(true);
    getContentPane().add(buttonPanel, BorderLayout.SOUTH);

    // If the default Harvest List option has a value, and the file exists, 
    // loads its contents.
    //
    if ((defaultHarvestList != null) && (!defaultHarvestList.equals(""))) {
      harvestListFile = new File(defaultHarvestList);
      if (harvestListFile.exists()) {
        try {
          loadHarvestList(harvestListFile);
          saveMenuItem.setEnabled(true);
        }
        catch (ParserConfigurationException e) {
          System.out.println("Error parsing Harvest List: " + e.getMessage());
        }        
      }
      else {
        System.out.println(
          "Warning: the default harvest list file that was specified in the " + 
          ".harvestListEditor configuration file does not exist:\n" +
          harvestListFile
                          );
        fileNew();
      }
    }
    else {
      fileNew();
    }

    try {    
      tempFile = File.createTempFile("harvestListTemp", ".xml");
    }
    catch (IOException ioe) {
      System.out.println("Error creating temporary file: " + ioe.getMessage());
    }
    
    setVisible(true);
  }


  /**
   * Implements action listeners for menu items.
   * 
   * @param e   An ActionEvent object, determines the menu item that was
   *            selected.
   */
  public void actionPerformed(ActionEvent e) {
    if ((e.getActionCommand()).equals("New")) {
      fileNew();
    }    
    else if ((e.getActionCommand()).equals("Open...")) {
      fileOpen();
    }    
    else if ((e.getActionCommand()).equals("Save")) {
      fileSave();
    }    
    else if ((e.getActionCommand()).equals("Save As...")) {
      fileSaveAs();
    }
    else if ((e.getActionCommand()).equals("Validate")) {
      fileValidate();
    }
    else if ((e.getActionCommand()).equals("Exit")) {
      fileExit();
    }
  }
  

  /**
   * Adds a new row to the table, setting values for each of the five columns
   * in the row.
   * 
   * @param rowIndex              the row index
   * @param scope                 the scope string
   * @param identifier            the identifier string
   * @param revision              the revision string
   * @param documentType          the document type
   * @param documentURL           the document URL
   */
  void addRow(int rowIndex, String scope, String identifier, String revision,
              String documentType, String documentURL) {
    tableModel.setValueAt(scope,                 rowIndex, 1);
    tableModel.setValueAt(identifier,            rowIndex, 2);
    tableModel.setValueAt(revision,              rowIndex, 3);
    tableModel.setValueAt(documentType,          rowIndex, 4);
    tableModel.setValueAt(documentURL,           rowIndex, 5);
  }


  /**
   * Composes a single tag line to be written in the Harvest List.
   * 
   * @param indentLevel    the number of spaces to begin the line with
   * @param tag            the tag name
   * @param text           the text to insert within the begin and end tags
   * @return line          the composed line
   */
  String composeLine(int indentLevel, String tag, String text) {
    String line = "";
    
    for (int i = 0; i < indentLevel; i++) {
      line += " ";
    }
    
    line += "<" + tag + ">";
    line += text;
    line += "</" + tag + ">";
    
    return line;
  }
  

  /**
   * Clears all rows in the table, setting all values to null.
   */
  void clearHarvestList() {
    for (int rowIndex = 0; rowIndex < numRows; rowIndex++) {
      clearRow(rowIndex);
    }
  }
  

  /**
   * Clears a single row in the tables, setting all five fields to null.
   * 
   * @param rowIndex   the index to the table row to be cleared
   */
  void clearRow(int rowIndex) {
    final String nil = "";
    
    tableModel.setValueAt(nil, rowIndex, 1);
    tableModel.setValueAt(nil, rowIndex, 2);
    tableModel.setValueAt(nil, rowIndex, 3);
    tableModel.setValueAt(nil, rowIndex, 4);
    tableModel.setValueAt(nil, rowIndex, 5);    
  }
  

  /**
   * Copies the values in a given table row to the clipboard.
   * 
   * @param rowIndex  the index of the table row to be copied
   */
  void copyRow(int rowIndex) {
    clipboardScope = (String) tableModel.getValueAt(rowIndex, 1);
    clipboardIdentifier = (String) tableModel.getValueAt(rowIndex, 2);
    clipboardRevision = (String) tableModel.getValueAt(rowIndex, 3);
    clipboardDocumentType = (String) tableModel.getValueAt(rowIndex, 4);    
    clipboardDocumentURL = (String) tableModel.getValueAt(rowIndex, 5);
  }
  

  /**
   * Cuts a row from the table. The row is copied to the clipboard and then
   * cleared.
   * 
   * @param rowIndex  the index of the table row to be copied
   */
  void cutRow(int rowIndex) {
    copyRow(rowIndex);
    clearRow(rowIndex);
  }
  

  /**
   * Exit from the Harvest List Editor. Prompt to save changes if appropriate.
   */
  void fileExit() {
    int value;

    if (harvestListHasChanged) {
      value = saveChangesDialog();
      
      if (value == JOptionPane.YES_OPTION) {
        try {
          // Save the changes then exit
          //
          fileSave();
          System.exit(0);
        }
        catch (Exception exception) {
          exception.printStackTrace();
        }
      }
      else if (value == JOptionPane.NO_OPTION) {
        // Exit without saving changes
        System.exit(0);
      } 
    }
    else {
      System.exit(0);
    }
  }
  

  /**
   * Replace the current Harvest List with an empty Harvest List. Prompt to save
   * changes if appropriate.
   */
  void fileNew() {
    int value;
    
    if (harvestListHasChanged) {
      value = saveChangesDialog();
      
      if (value == JOptionPane.YES_OPTION) {
        try {
          fileSave();
        }
        catch (Exception exception) {
          exception.printStackTrace();
        }
      }
      else if (value == JOptionPane.CANCEL_OPTION) {
        return;
      }
    }

    clearHarvestList();
    harvestListFile = null;
    setTitle(title + ": (Untitled)");
    saveMenuItem.setEnabled(false);
    harvestListHasChanged = false;    
  }
  

  /**
   * Opens a file dialog to load a Harvest List. Prompts to save changes to the
   * current Harvest List if appropriate.
   */
  void fileOpen() {
    int value;
    
    if (harvestListHasChanged) {
      value = saveChangesDialog();
      
      if (value == JOptionPane.YES_OPTION) {
        try {
          fileSave();
        }
        catch (Exception exception) {
          exception.printStackTrace();
        }
      }
      else if (value == JOptionPane.CANCEL_OPTION) {
        return;
      }
    }

    value = fileChooser.showOpenDialog(HarvestListEditor.this);
    
    if (value == JFileChooser.APPROVE_OPTION) {
      harvestListFile = fileChooser.getSelectedFile();
      try {
        clearHarvestList();
        loadHarvestList(harvestListFile);
      }
      catch (ParserConfigurationException e) {
        System.out.println("Error parsing Harvest List: " + e.getMessage());
      }        
      harvestListHasChanged = false;
      saveMenuItem.setEnabled(true);
    }
  }
  

  /**
   * Save the current Harvest List to disk.
   */
  void fileSave() {
    if (harvestListFile != null) {
      writeFile(harvestListFile);
      harvestListHasChanged = false;
    }
    else {
      System.out.println("No action taken");
    }
  }
  

  /**
   * Save the current Harvest List as a potentially different file name.
   */
  void fileSaveAs() {
    int returnVal;
    
    returnVal = fileChooser.showOpenDialog(HarvestListEditor.this);
    
    if (returnVal == JFileChooser.APPROVE_OPTION) {
      harvestListFile = fileChooser.getSelectedFile();
      writeFile(harvestListFile);
      setTitle(title + ": " + harvestListFile.getName());
      saveMenuItem.setEnabled(true);
      harvestListHasChanged = false;
    }
  }
  
  
  /**
   * Validate the Harvest List that is currently stored in the table. This is
   * implemented by writing the Harvest List to a temporary file and then
   * running the SAX parser on the temporary file.
   */
  void fileValidate() {
    FileInputStream fis;
    HarvestListHandler harvestListHandler = new HarvestListHandler();
    InputStreamReader inputStreamReader;
    boolean loadHarvestList = false;
    boolean validateHarvestList = true;
    
    writeFile(tempFile);
    
    try {
      fis = new FileInputStream(tempFile);
      inputStreamReader = new InputStreamReader(fis);
      harvestListHandler.runParser(this, inputStreamReader, schemaLocation,
                                   loadHarvestList, validateHarvestList);
      fis.close();
      tempFile.delete();
      harvestListMessage("Harvest List is valid");
    }
    catch (SAXException e) {
      harvestListMessage("Validation error: " + e.getMessage());
    }
    catch (ClassNotFoundException e) {
      System.out.println("ClassNotFoundException: " + e.getMessage());
    }
    catch (IOException ioe) {
      System.out.println("Error opening file: " + ioe.getMessage());
    }
  }
  

  /**
   * Displays a short message in a dialog box.
   * 
   * @param message       the message text
   */
  void harvestListMessage(String message) {
    JOptionPane.showMessageDialog(this, message);
  }
  

  /**
   * Initializes the Harvest List table, filling column 0 with row numbers.
   * This is a non-editable column, so its values should never change after
   * this point.
   */
  void initHarvestList() {
    for (int rowIndex = 0; rowIndex < numRows; rowIndex++) {
      tableModel.setValueAt(new Integer(rowIndex + 1).toString(), rowIndex, 0);
    }
  }
  

  /**
   * Determines whether the clipboard is currently empty. The clipboard is
   * empty if all five of the fields are empty.
   * 
   * @return      true if the clipboard is empty, else false
   */
  boolean isEmptyClipboard() {
    boolean isEmpty = true;
    
    isEmpty = isEmpty && (clipboardScope.equals(""));
    isEmpty = isEmpty && (clipboardIdentifier.equals(""));
    isEmpty = isEmpty && (clipboardRevision.equals(""));
    isEmpty = isEmpty && (clipboardDocumentType.equals(""));
    isEmpty = isEmpty && (clipboardDocumentURL.equals(""));
    
    return isEmpty;
  }
    

  /**
   * Determines whether a given row in the table is empty. A row is empty if
   * all five of its fields contain either null or "".
   * 
   * @param rowIndex    the index to the row in the table that is being checked
   * @return            true if the row is empty, else false
   */
  boolean isEmptyRow(int rowIndex) {
    boolean isEmpty = true;
    String scope = (String) tableModel.getValueAt(rowIndex, 1);
    String identifier = (String) tableModel.getValueAt(rowIndex, 2);
    String revision = (String) tableModel.getValueAt(rowIndex, 3);
    String documentType = (String) tableModel.getValueAt(rowIndex, 4);    
    String documentURL = (String) tableModel.getValueAt(rowIndex, 5);
    
    isEmpty = isEmpty && ((scope == null) || (scope.equals("")));
    isEmpty = isEmpty && ((identifier == null) || (identifier.equals("")));
    isEmpty = isEmpty && ((revision == null) || (revision.equals("")));
    isEmpty = isEmpty && ((documentType == null) || (documentType.equals("")));
    isEmpty = isEmpty && ((documentURL == null) || (documentURL.equals("")));
    
    return isEmpty;
  }
  

  /**
   * Loads the Harvest List from a file. Parses the file using the inner class,
   * HarvestListHandler, a SAX parser.
   * 
   * @param harvestList  the File to be loaded
   * @throws ParserConfigurationException
   */
  void loadHarvestList(File harvestList) throws ParserConfigurationException {
    HarvestListHandler harvestListHandler = new HarvestListHandler();
    FileInputStream fis;
    InputStreamReader inputStreamReader;
    boolean loadHarvestList = true;
    boolean validateHarvestList = false;

    try {
      fis = new FileInputStream(harvestList);
      inputStreamReader = new InputStreamReader(fis);
      //System.out.println("Opened file successfully.");
      harvestListHandler.runParser(this, inputStreamReader, schemaLocation,
                                   loadHarvestList, validateHarvestList);
      fis.close();
      setTitle(title + ": " + harvestListFile.getName());
    }
    catch (SAXException e) {
      System.out.println("Error parsing Harvest List: " + e.getMessage());
    }
    catch (ClassNotFoundException e) {
      System.out.println("ClassNotFoundException: " + e.getMessage());
    }
    catch (IOException ioe) {
      System.out.println("Error opening file: " + ioe.getMessage());
    }
  }
  

  /**
   * Loads properties from the .harvestListEditor file in the user's home
   * directory.
   */
  void loadProperties () {
    String homedir = System.getProperty("user.home");
    File propertiesFile = new File(homedir, ".harvestListEditor");

    properties = new Properties();

    if (propertiesFile.exists()) {
      try {
        properties.load(new FileInputStream(propertiesFile));
        defaultHarvestList = properties.getProperty("defaultHarvestList");
        defaultDocumentType = properties.getProperty("defaultDocumentType");
        defaultDocumentURL = properties.getProperty("defaultDocumentURL");
        defaultIdentifier = properties.getProperty("defaultIdentifier");
        defaultRevision = properties.getProperty("defaultRevision");
        defaultScope = properties.getProperty("defaultScope");    
      }
      catch (IOException ioe) {
        System.out.println("Error loading properties file: " + 
                           ioe.getMessage());
      }  
    }
  }
  

  /**
   * Enables or disables buttons depending on the state of the currently
   * selected row and the state of the clipboard.
   * 
   * @param rowIndex       the index of the currently selected row
   */
  void manageButtons(int rowIndex) {
    
    if (isEmptyRow(rowIndex)) {
      // Selected row is empty, so disable cut and copy
      cutButton.setEnabled(false);
      copyButton.setEnabled(false);
    }
    else {
      // Selected row is not empty, so enable cut and copy
      cutButton.setEnabled(true);
      copyButton.setEnabled(true);
    }

    if (isEmptyClipboard()) {
      // Clipboard is empty, so disable paste
      pasteButton.setEnabled(false);
    }
    else {
      // Clipboard is not empty, so enable paste
      pasteButton.setEnabled(true);
    }

    // Paste Defaults button is enabled whenever a row is selected    
    pasteDefaultsButton.setEnabled(true);
  }
  

  /**
   * Pastes the clipboard values into the specified row.
   * 
   * @param rowIndex      the index of the row that is being pasted to
   */
  void pasteRow(int rowIndex) {
    tableModel.setValueAt(clipboardScope,        rowIndex, 1);
    tableModel.setValueAt(clipboardIdentifier,   rowIndex, 2);
    tableModel.setValueAt(clipboardRevision,     rowIndex, 3);
    tableModel.setValueAt(clipboardDocumentType, rowIndex, 4);
    tableModel.setValueAt(clipboardDocumentURL,  rowIndex, 5);
  }
  

  /**
   * Pastes the default values into the specified row.
   * 
   * @param rowIndex      the index of the row that is being pasted to
   */
  void pasteDefaultValues(int rowIndex) {
    tableModel.setValueAt(defaultScope,        rowIndex, 1);
    tableModel.setValueAt(defaultIdentifier,   rowIndex, 2);
    tableModel.setValueAt(defaultRevision,     rowIndex, 3);
    tableModel.setValueAt(defaultDocumentType, rowIndex, 4);
    tableModel.setValueAt(defaultDocumentURL,  rowIndex, 5);
  }
  

  /**
   * Dialog to determine whether user wants to save changes before proceeding.
   * 
   * @return   integer value that determines whether the user responded with
   *           "Yes", "No", or "Cancel"
   */
  int saveChangesDialog () {
    Object[] options = {"Yes", "No", "Cancel"};
    int value;
    
    value = JOptionPane.showOptionDialog(null,
                                         "Save Changes?",
                                         "Warning",
                                         JOptionPane.DEFAULT_OPTION,
                                         JOptionPane.WARNING_MESSAGE,
                                         null,
                                         options,
                                         options[0]); // default option
    return value;
  }
  

  /**
   * Writes the contents of the table to file as XML.
   * 
   * @param harvestList       the File object to write to
   */
  void writeFile(File harvestList) {
    try {
      PrintWriter out = new PrintWriter(new FileWriter(harvestList));
      writeHarvestList(out);
    }
    catch (IOException ioe) {
      System.out.println("IOException: " + ioe.getMessage());
    }
  }
  

  /**
   * Writes the contents of the table to a PrintWriter.
   * 
   * @param out       the PrintWriter to write the Harvest List to
   */
  void writeHarvestList(PrintWriter out) {
    out.println("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>");
    out.println("");
    out.println(
       "<hrv:harvestList xmlns:hrv=\"eml://ecoinformatics.org/harvestList\" >");

    for (int i = 0; i < numRows; i++) {
      if (!isEmptyRow(i)) {
        writeRow(out, i);
      }
    }

    out.println("");
    out.println("</hrv:harvestList>");
    out.close();
  }
  

  /**
   * Writes a row of the table to file. A row corresponds to a single
   * <Document> element in the Harvest List.
   * 
   * @param out       the PrintWriter object for the file
   * @param rowIndex  the index of the table row that is being written to file
   */
  void writeRow(PrintWriter out, int rowIndex) {
    int indentLevel = 6;
    String scope = (String) tableModel.getValueAt(rowIndex, 1);
    String identifier = (String) tableModel.getValueAt(rowIndex, 2);
    String revision = (String) tableModel.getValueAt(rowIndex, 3);
    String documentType = (String) tableModel.getValueAt(rowIndex, 4);    
    String documentURL = (String) tableModel.getValueAt(rowIndex, 5);

    out.println("");
    out.println("  <document>");
    out.println("    <docid>");
    out.println(composeLine(indentLevel, "scope", scope));
    out.println(composeLine(indentLevel, "identifier", identifier));
    out.println(composeLine(indentLevel, "revision", revision));
    out.println("    </docid>");
    indentLevel = 4;
    out.println(composeLine(indentLevel, "documentType", documentType));
    out.println(composeLine(indentLevel, "documentURL", documentURL));
    out.println("  </document>");
  }
  

  /*
   * Inner class: HarvestListTableModel
   */
    class HarvestListTableModel extends AbstractTableModel {
      final boolean DEBUG = false;
      // Column names for the JTable  
      private String[] columnNames = {
                          "Row #",
                          "Scope",
                          "Identifier",
                          "Revision",
                          "Document Type",
                          "Document URL"
                         };

      private Object[][] data = new Object[numRows][numColumns];

        public int getColumnCount() {
            return columnNames.length;
        }

        public int getRowCount() {
            return data.length;
        }

        public String getColumnName(int col) {
            return columnNames[col];
        }

        public Object getValueAt(int row, int col) {
            return data[row][col];
        }

        /*
         * JTable uses this method to determine the default renderer/
         * editor for each cell.  If we didn't implement this method,
         * then the last column would contain text ("true"/"false"),
         * rather than a check box.
         */
        public Class getColumnClass(int c) {
            return getValueAt(0, c).getClass();
        }

        /*
         * Don't need to implement this method unless your table's
         * editable.
         */
        public boolean isCellEditable(int row, int col) {
            //Note that the data/cell address is constant,
            //no matter where the cell appears onscreen.
            if (col < 1) {
                return false;
            } else {
                return true;
            }
        }

        /*
         * Don't need to implement this method unless your table's
         * data can change.
         */
        public void setValueAt(Object value, int row, int col) {
            if (DEBUG) {
                System.out.println("Setting value at " + row + "," + col
                                   + " to " + value
                                   + " (an instance of "
                                   + value.getClass() + ")");
            }

            data[row][col] = value;
            fireTableCellUpdated(row, col);

            if (DEBUG) {
                System.out.println("New value of data:");
                printDebugData();
            }
        }

        private void printDebugData() {
            int numRows = getRowCount();
            int numCols = getColumnCount();

            for (int i=0; i < numRows; i++) {
                System.out.print("    row " + i + ":");
                for (int j=0; j < numCols; j++) {
                    System.out.print("  " + data[i][j]);
                }
                System.out.println();
            }
            System.out.println("--------------------------");
        }
    }

  /**
   * This inner class extends DefaultHandler. It parses the Harvest List file,
   * writing a new row to the table every time it encounters a </Document>
   * end tag.
   */
  class HarvestListHandler extends DefaultHandler implements ErrorHandler {
  
    public String scope;
    public int identifier;
    public String identifierString;
    public String documentType;
    private HarvestListEditor harvestListEditor;
    boolean loadHarvestList;
    public int revision;
    public String revisionString;
    private int rowIndex = 0;
    public String documentURL;
    private String currentQname;
    public final static String DEFAULT_PARSER = 
           "org.apache.xerces.parsers.SAXParser";
    private boolean schemaValidate = true;
    private boolean validateHarvestList;
	

	  /**
     * This method is called for any plain text within an element.
     * It parses the value for any of the following elements:
     * <scope>, <identifier>, <revision>, <documentType>, <documentURL>
     * 
     * @param ch          the character array holding the parsed text
     * @param start       the start index
     * @param length      the text length
     * 
     */
    public void characters (char ch[], int start, int length) {
      String s = new String(ch, start, length);
 
      if (length > 0) {           
        if (currentQname.equals("scope")) {
          scope += s;
        }
        else if (currentQname.equals("identifier")) {
          identifierString += s;
        }
        else if (currentQname.equals("revision")) {
          revisionString += s;
        }
        else if (currentQname.equals("documentType")) {
          documentType += s;
        }
        else if (currentQname.equals("documentURL")) {
          documentURL += s;
        }
      }
    }


    /** 
     * Handles an end-of-document event.
     */
    public void endDocument () {
    }


    /** 
     * Handles an end-of-element event. If the end tag is </Document>, then
     * creates a new HarvestDocument object and pushes it to the document
     * list.
     * 
     * @param uri
     * @param localname
     * @param qname
     */
    public void endElement(String uri, 
                           String localname,
                           String qname) {
      
      HarvestDocument harvestDocument;
      
      if (qname.equals("identifier")) {
        identifier = Integer.parseInt(identifierString);
      }
      else if (qname.equals("revision")) {
        revision = Integer.parseInt(revisionString);
      }
      else if (qname.equals("document")) {
        if (loadHarvestList) {
          harvestListEditor.addRow(rowIndex, scope, identifierString, 
                                   revisionString, documentType, documentURL);
        }

        rowIndex++;
      }

      currentQname = "";
    }


    /**
     * Method for handling errors during a parse
     *
     * @param exception         The parsing error
     * @exception SAXException  Description of Exception
     */
     public void error(SAXParseException e) throws SAXParseException {
        System.out.println("SAXParseException: " + e.getMessage());
        throw e;
    }


    /**
     * Run the validating parser
     *
     * @param xml             the xml stream to be validated
     * @schemaLocation        relative path the to XML Schema file, e.g. "."
     * @exception IOException thrown when test files can't be opened
     * @exception ClassNotFoundException thrown when SAX Parser class not found
     * @exception SAXException
     * @exception SAXParserException
     */
    public void runParser(HarvestListEditor harvestListEditor,
                          Reader xml, 
                          String schemaLocation,
                          boolean loadHarvestList,
                          boolean validateHarvestList)
           throws IOException, ClassNotFoundException,
                  SAXException, SAXParseException {

      // Get an instance of the parser
      XMLReader parser;
      
      this.harvestListEditor = harvestListEditor;
      this.loadHarvestList = loadHarvestList;
      this.validateHarvestList = validateHarvestList;
      this.rowIndex = 0;

      parser = XMLReaderFactory.createXMLReader(DEFAULT_PARSER);
      // Set Handlers in the parser
      parser.setContentHandler((ContentHandler)this);
      parser.setErrorHandler((ErrorHandler)this);
      parser.setFeature("http://xml.org/sax/features/namespaces", true);
      parser.setFeature("http://xml.org/sax/features/namespace-prefixes", true);
      parser.setFeature("http://xml.org/sax/features/validation", true);
      parser.setProperty(
              "http://apache.org/xml/properties/schema/external-schemaLocation", 
              schemaLocation);

      if (schemaValidate) {
        parser.setFeature("http://apache.org/xml/features/validation/schema", 
                          true);
      }
    
      // Parse the document
      parser.parse(new InputSource(xml));
    }


    /**
     * Handles a start-of-document event.
     */
    public void startDocument () {
      //System.out.println("Started parsing Harvest List");
    }


    /** 
     * Handles a start-of-element event.
     * 
     * @param uri
     * @param localname
     * @param qname
     * @param attributes
     */
    public void startElement(String uri, 
                             String localname,
                             String qname,
                             Attributes attributes) {
      
      currentQname = qname;

      if (qname.equals("scope")) {
        scope = "";
      }
      else if (qname.equals("identifier")) {
        identifierString = "";
      }
      else if (qname.equals("revision")) {
        revisionString = "";
      }
      else if (qname.equals("documentType")) {
        documentType = "";
      }
      else if (qname.equals("documentURL")) {
        documentURL = "";
      }
    }
  }
}