package edu.ucsb.nceas.dbadapter;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * The PostgreSQL db adapter implementation.
 */
public class PostgresqlAdapter extends AbstractDatabase {

  /**
   * The PostgreSQL unique ID /sequence generator
   * The name of the sequence used to generate the unique id 
   * is made from the name of the table that uses the id by 
   * appending "_id_seq" to it.
   * When record is inserted in the table and insert trigger gets
   * a nextval from that sequence, select currval of that sequence
   * can return the generated key in the same db connection.
   *
   * @param conn db connection in which the unique id was generated
   * @param tableName the name of table which unique id was generate
   * @exception SQLException any SQLException that can be thrown 
   *            during the db operation
   * @return return the generated unique id as a long type
   */
  public long getUniqueID(Connection conn, String tableName) 
                                         throws SQLException {
    long uniqueid = 0;
    Statement stmt = null;
    stmt = conn.createStatement();
    stmt.execute("SELECT currval('" + tableName + "_id_seq')");
    ResultSet rs = stmt.getResultSet();
    if ( rs.next() ) 
	  {
        uniqueid = rs.getLong(1);
    }
    stmt.close();
 
    return uniqueid;
  }

  /**
   * The PostgreSQL function name that gets the current date 
   * and time from the database server
   *
   * @return return the current date and time function name: "now()"
   */
  public String getDateTimeFunction() {

    //System.out.println("The date and time function: " + "now()");    
		//to return just the date use now()::date
		//and for the time use now()::time
    return "now()";
  }

  /**
   * The PostgreSQL function name that is used to return non-NULL value
   *
   * @return return the non-NULL function name: "coalesce"
   */
  public String getIsNULLFunction() {
    
    return "coalesce";
  }

  /**
   * PostgreSQL's string delimiter character: single quote (')
   *
   * @return return the string delimiter: single quote (')
   */
  public String getStringDelimiter() {

    return "\"";
  }
  
  /**
   * PostgreSQL's syntax for doing a left join
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
      String sql ="select a.docid, a.rev, a.doctype from ( xml_documents as a left outer join  xml_revisions as b on (a.docid=b.docid and  a.rev<=b.rev)) where b.docid is null ";
      return sql;
  }
  
  public String getPagedQuery(String queryWithOrderBy, Integer start, Integer count) {
	  String query = queryWithOrderBy;
	  if (count != null) {
		  query = query + " LIMIT " + count;
	  }
	  if (start != null) {
		  query = query + " OFFSET " + start;
	  }
	  return query;
  }
}
    
