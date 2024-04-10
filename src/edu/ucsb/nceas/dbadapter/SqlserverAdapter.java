package edu.ucsb.nceas.dbadapter;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * The MS SQL Server db adapter implementation.
 */
public class SqlserverAdapter extends AbstractDatabase {

  /**
   * The SQL Server unique ID generator through use of IDENTITY key
   * The IDENTITY key is a column in the table. When record is inserted
   * in the table, SELECT @@IDENTITY can return the key generated in
   * that IDENTITY column in the same db connection.
   * This is the only way to get unique id: let the SQL Server assign
   * a value in IDENTITY column and get it afterwards for use in the 
   * application.
   *
   * @param conn db connection in which to generate the unique id
   * @param tableName the name of table which unique id to generate
   * @exception SQLException <br/> any SQLException that can be thrown 
   *            during the db operation
   * @return return the generated unique id as a long type
   */
  public long getUniqueID(Connection conn, String tableName) 
                                         throws SQLException {
    long uniqueid = 0;
    Statement stmt = null;
    stmt = conn.createStatement();
    stmt.execute("SELECT @@IDENTITY");
    ResultSet rs = stmt.getResultSet();
    if ( rs.next() ) {
        uniqueid = rs.getLong(1);
    }
      stmt.close();

    return uniqueid;
  }

  /**
   * The SQL Server's function name that gets the current date and time
   * from the database server: "getdate()"
   *
   * @return return the current date and time function name: "getdate()"
   */
  public String getDateTimeFunction() {

    //System.out.println("The date and time function: " + "getdate()");    
    return "getdate()";
  }

  /**
   * The SQL Server's function name that is used to return non-NULL value
   *
   * @return return the non-NULL function name: "isnull"
   */
  public String getIsNULLFunction() {
    
    return "isnull";
  }

  /**
   * The SQL Server's string delimiter character: single quote (')
   *
   * @return return the string delimiter: single quote (')
   */
  public String getStringDelimiter() {

    return "'";
  }
  
 /**
  * MSSQL doesn't support the to_date function, so we transfer text directly.
  * This method will overwrite the method in AbstarctDatabase class
  */
  public String toDate(String dateString, String format)
  {
    return "'" + dateString +"'";
  }
  
  /**
   * MSSQL's syntax for doing a left join
   * Add 'a.' in front of the fields for first table and
   * 'b.' in front of the fields for the second table
   * 
   * @param selectFields fields that you want to be selected
   * @param tableA first table in the join
   * @param tableB second table in the join
   * @param joinCriteria the criteria based on which the join will be made
   * @param nonJoinCriteria all other criterias
   * @return return the string for teh select query
   */
  public String getLeftJoinQuery(String selectFields, String tableA, 
		  String tableB, String joinCriteria, String nonJoinCriteria){

	  return "SELECT " + selectFields + " FROM " + tableA + " a LEFT JOIN " 
	         + tableB + " b ON " + joinCriteria + " WHERE (" 
	         + nonJoinCriteria +")";
  }

/**
   * Return a hard code string to get xml_document list in timed replcation
   */
  public String getReplicationDocumentListSQL()
  {
      String sql ="select a.docid, a.rev, a.doctype from ( xml_documents a left outer join  xml_revisions b on (a.docid=b.docid and  a.rev<=b.rev)) where b.docid is null ";
      return sql;
  }
  
  public String getPagedQuery(String queryFieldsWithOrderBy, Integer start, Integer count) {
	  // TODO: implement MSSQL server
	  return null;

  }
}
    
