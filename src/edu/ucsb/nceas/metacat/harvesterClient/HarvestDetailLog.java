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

import java.io.PrintStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * HarvestDetailLog manages data and operations corresponding to the
 * HARVEST_DETAIL_LOG table. It records errors encountered while attempting
 * to harvest a particular EML document.
 * 
 * @author  costa
 */
public class HarvestDetailLog {

  private Connection conn;    
  private Harvester harvester;              // The parent Harvester object
  private int detailLogID;
  private int harvestLogID;
  private HarvestDocument harvestDocument;  // The associated HarvestDocument
  private String errorMessage;
    

  /** 
   * Creates a new instance of HarvestDetailLog and inserts the data into
   * the HARVEST_DETAIL_LOG table.
   *
   * @param  harvester       the Harvester parent object
   * @param  conn            the database connection
   * @param  detailLogID     primary key in the HARVEST_LOG table
   * @param  harvestLogID    foreign key value matching the HARVEST_LOG table
   * @param  harvestDocument HarvestDocument object that generated an error
   * @param  errorMessage    text of the error message
   */
  public HarvestDetailLog(Harvester       harvester,
                          Connection      conn,
                          int             detailLogID,
                          int             harvestLogID,
                          HarvestDocument harvestDocument,
                          String          errorMessage
                         ) {
    this.harvester = harvester;
    this.conn = conn;
    this.detailLogID = detailLogID;
    this.harvestLogID = harvestLogID;
    this.harvestDocument = harvestDocument;
    this.errorMessage = errorMessage;
  }
    

  /**
   * Inserts a new entry into the HARVEST_DETAIL_LOG table, based on the 
   * contents of this HarvestDetailLog object.
   */
  void dbInsertHarvestDetailLogEntry() {
    String dequotedMessage;
    String insertString;
		Statement stmt;
    
    dequotedMessage = harvester.dequoteText(errorMessage);

    // Set the value of the HARVEST_LOG_ID to the current time in UTC seconds
    insertString = "INSERT INTO HARVEST_DETAIL_LOG " +
                   "(DETAIL_LOG_ID, HARVEST_LOG_ID, SCOPE," + 
                   " IDENTIFIER, REVISION," +
                   " DOCUMENT_URL, ERROR_MESSAGE, DOCUMENT_TYPE) " +
                   "values(" +
                   detailLogID + ", " +
                   harvestLogID + ", " +
                   "'" + harvestDocument.scope + "', " +
                   harvestDocument.identifier + ", " +
                   harvestDocument.revision + ", " +
                   "'" + harvestDocument.documentURL + "', " +
                   "'" + dequotedMessage + "'," +
                   "'" + harvestDocument.documentType + "'" +
                   ")";
                   
		try {
			stmt = conn.createStatement();
			stmt.executeUpdate(insertString);
			stmt.close();
		}
    catch(SQLException e) {
			System.out.println("SQLException: " + e.getMessage());
		}
  }


  /**
   * Prints the contents of this HarvestLog object. Used in generating reports.
   * 
   * @param out   the PrintStream to write to
   */
  public void printOutput(PrintStream out) {
    out.println("* detailLogID:          " + detailLogID);
    out.println("* errorMessage:         " + errorMessage);
    harvestDocument.printOutput(out);
  }

}
