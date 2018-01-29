/**
 *  '$RCSfile$'
 *  Copyright: 2004 Regents of the University of California and the
 *             National Center for Ecological Analysis and Synthesis
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
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package edu.ucsb.nceas.metacat;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v2.Log;
import org.dataone.service.types.v2.LogEntry;
import org.dataone.service.types.v1.Event;
import org.dataone.service.types.v1.NodeReference;
import org.dataone.service.types.v1.Subject;
import org.dataone.service.util.DateTimeMarshaller;

import edu.ucsb.nceas.metacat.database.DBConnection;
import edu.ucsb.nceas.metacat.database.DBConnectionPool;
import edu.ucsb.nceas.metacat.database.DatabaseService;
import edu.ucsb.nceas.metacat.index.MetacatSolrIndex;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.metacat.util.DocumentUtil;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;

/**
 * EventLog is used to intialize and store a log of events that occur in an
 * application. The events are registered with the logger as they occur, but
 * EventLog writes them to permenant storage when it is most convenient or
 * efficient. EventLog is a Singleton as there should always be only one object
 * for these logging events.
 * 
 * TODO: Logging to the database needn't be synchronous with the event.  
 * Instead, a separate thread can be launched that periodically sleeps and only
 * wakes periodically to see if metacat is idle.  The log event can be cached
 * and inserted later when the thread wakes and finds metacat idle.
 * 
 * TODO: Write a function that archives a part of the log table to an 
 * external text file so that the log table doesn't get to big.  This 
 * function should be able to be called manually or on a schedule. 
 * 
 * TODO: Write an access function that returns an XML report for a
 * specific subset of events.  Users should be able to query on
 * principal, docid/rev, date, event, and possibly other fields.
 * 
 * @author jones
 */
public class EventLog
{
    public static final String DELETE = "delete";
    /**
     * The single instance of the event log that is always returned.
     */
    private static EventLog self = null;
    private Logger logMetacat = Logger.getLogger(EventLog.class);
    private static final int USERAGENTLENGTH = 512;


    /**
     * A private constructor that initializes the class when getInstance() is
     * called.
     */
    private EventLog()
    {
    }

    /**
     * Return the single instance of the event log after initializing it if it
     * wasn't previously initialized.
     * 
     * @return the single EventLog instance
     */
    public static EventLog getInstance()
    {
        if (self == null) {
            self = new EventLog();
        }
        return self;
    }

    /**
     * Log an event of interest to the application. The information logged can
     * include basic identification information about the principal or computer
     * that initiated the event.
     * 
     * @param ipAddress the internet protocol address for the event
     * @param userAgent the agent making the request
	 * @param principal the principal for the event (a username, etc)
	 * @param docid the identifier of the document to which the event applies
	 * @param event the string code for the event
     */
    public void log(String ipAddress, String userAgent, String principal, String docid, String event) {
        EventLogData logData = new EventLogData(ipAddress, userAgent, principal, docid, event);
        insertLogEntry(logData);
        
        // update the event information in the index
        try {
	        String localId = DocumentUtil.getSmartDocId(docid);
			int rev = DocumentUtil.getRevisionFromAccessionNumber(docid);
			
	        String guid = IdentifierManager.getInstance().getGUID(localId, rev);
	        Identifier pid = new Identifier();
	        pid.setValue(guid);
	        
	        // submit for indexing
	        MetacatSolrIndex.getInstance().submit(pid, null, this.getIndexFields(pid, event), false);
	        
        } catch (Exception e) {
        	logMetacat.error("Could not update event index information", e);
        }
    }
    
    public Map<String, List<Object>> getIndexFields(Identifier pid, String event) {
    	// update the search index for the event
        try {
        	
        	if (event != null) {
        		
	        	String fieldName = event + "_count_i";
	        	int eventCount = 0;
	        	
	        	String docid = IdentifierManager.getInstance().getLocalId(pid.getValue());
	        	Log eventLog = this.getD1Report(null, null, new String[] {docid}, event, null, null, false, 0, 0);
	        	eventCount = eventLog.getTotal();
	
		        List<Object> values = new ArrayList<Object>();
				values.add(eventCount);
		        Map<String, List<Object>> fields = new HashMap<String, List<Object>>();
		        fields.put(fieldName, values);
		        
		        return fields;
        	}
	        
        } catch (Exception e) {
        	logMetacat.error("Could not update event index information on pid: " + pid.getValue() + " for event: " + event, e);
        }
        // default if we can't find the event information
    	return null;

    }
    
    /**
     * Insert a single log event record to the database.
     * 
     * @param logData the data to be logged when an event occurs
     */
    private void insertLogEntry(EventLogData logData)
    {
        String insertString = "insert into access_log"
                + "(ip_address, user_agent, principal, docid, "
                + "event, date_logged) "
                + "values ( ?, ?, ?, ?, ?, ? )";
 
        DBConnection dbConn = null;
        int serialNumber = -1;
        try {
            // Get a database connection from the pool
            dbConn = DBConnectionPool.getDBConnection("EventLog.insertLogEntry");
            serialNumber = dbConn.getCheckOutSerialNumber();
            String userAgent = logData.getUserAgent();
            if(userAgent != null && userAgent.length() > USERAGENTLENGTH) {
                userAgent = userAgent.substring(0, USERAGENTLENGTH);
            }
            
            // Execute the insert statement
            PreparedStatement stmt = dbConn.prepareStatement(insertString);
            
            stmt.setString(1, logData.getIpAddress());
            stmt.setString(2, userAgent);
            stmt.setString(3, logData.getPrincipal());
            stmt.setString(4, logData.getDocid());
            stmt.setString(5, logData.getEvent());
            stmt.setTimestamp(6, new Timestamp(new Date().getTime()));
            stmt.executeUpdate();
            stmt.close();
        } catch (SQLException e) {
        	logMetacat.error("Error while logging event to database: " 
                    + e.getMessage());
        } finally {
            // Return database connection to the pool
            DBConnectionPool.returnDBConnection(dbConn, serialNumber);
        }
    }
    
    /**
     * Get a report of the log events that match a set of filters.  The
     * filter parameters can be null; log records are subset based on
     * non-null filter parameters.
     * 
     * @param ipAddress the internet protocol address for the event
	 * @param principal the principal for the event (a username, etc)
	 * @param docid the identifier of the document to which the event applies
	 * @param event the string code for the event
	 * @param startDate beginning of date range for query
	 * @param endDate end of date range for query
	 * @return an XML-formatted report of the access log entries
     */
    public String getReport(String[] ipAddress, String[] principal, String[] docid,
            String[] event, Timestamp startDate, Timestamp endDate, boolean anonymous)
    {
        StringBuffer resultDoc = new StringBuffer();
        StringBuffer query = new StringBuffer();
        query.append("select entryid, ip_address, user_agent, principal, docid, "
            + "event, date_logged from access_log");
//                        + ""
//                        + "event, date_logged) " + "values (" + "'"
//                        + logData.getIpAddress() + "', " + "'"
//                        + logData.getPrincipal() + "', " + "'"
//                        + logData.getDocid() + "', " + "'" + logData.getEvent()
//                        + "', " + " ? " + ")";
        if (ipAddress != null || principal != null || docid != null
                        || event != null || startDate != null || endDate != null) {
            query.append(" where ");
        }
        boolean clauseAdded = false;
        int startIndex = 0;
        int endIndex = 0;
        
        List<String> paramValues = new ArrayList<String>();
        if (ipAddress != null) {
        	query.append("ip_address in (");
        	for (int i = 0; i < ipAddress.length; i++) {
        		if (i > 0) {
            		query.append(", ");
        		}
        		query.append("?");
        		paramValues.add(ipAddress[i]);
        	}
        	query.append(") ");
            clauseAdded = true;
        }
        if (principal != null) {
        	if (clauseAdded) {
                query.append(" and ");
            }
        	query.append("principal in (");
        	for (int i = 0; i < principal.length; i++) {
        		if (i > 0) {
            		query.append(", ");
        		}
        		query.append("?");
        		paramValues.add(principal[i]);
        	}
        	query.append(") ");
            clauseAdded = true;
        }
        if (docid != null) {
        	if (clauseAdded) {
                query.append(" and ");
            }
        	query.append("docid in (");
        	for (int i = 0; i < docid.length; i++) {
        		if (i > 0) {
            		query.append(", ");
        		}
        		query.append("?");
        		String fullDocid = docid[i];
        		// allow docid without revision - look up latest version
        		try {
        			fullDocid = DocumentUtil.appendRev(fullDocid);
        		} catch (Exception e) {
					// just warn about this
        			logMetacat.debug("Could not check docid for revision: " + fullDocid, e);
				}
        		paramValues.add(fullDocid);
        	}
        	query.append(") ");
            clauseAdded = true;
        }
        if (event != null) {
        	if (clauseAdded) {
                query.append(" and ");
            }
        	query.append("event in (");
        	for (int i = 0; i < event.length; i++) {
        		if (i > 0) {
            		query.append(", ");
        		}
        		query.append("?");
        		paramValues.add(event[i]);
        	}
        	query.append(") ");
            clauseAdded = true;
        }
        if (startDate != null) {
            if (clauseAdded) {
                query.append(" and ");
            }
            query.append("date_logged >= ?");
            clauseAdded = true;
            startIndex++;
        }
        if (endDate != null) {
            if (clauseAdded) {
                query.append(" and ");
            }
            query.append("date_logged < ?");
            clauseAdded = true;
            endIndex = startIndex + 1;
        }
        DBConnection dbConn = null;
        int serialNumber = -1;
        try {
            // Get a database connection from the pool
            dbConn = DBConnectionPool.getDBConnection("EventLog.getReport");
            serialNumber = dbConn.getCheckOutSerialNumber();

            // Execute the query statement
            PreparedStatement stmt = dbConn.prepareStatement(query.toString());
            //set the param values
            int parameterIndex = 1;
            for (String val: paramValues) {
            	stmt.setString(parameterIndex++, val);
            }
            if (startDate != null) {
                stmt.setTimestamp(parameterIndex++, startDate); 
            }
            if (endDate != null) {
            	stmt.setTimestamp(parameterIndex++, endDate);
            }
            stmt.execute();
            ResultSet rs = stmt.getResultSet();
            //process the result and return it as an XML document
            resultDoc.append("<?xml version=\"1.0\"?>\n");
            resultDoc.append("<log>\n");
            while (rs.next()) {
                resultDoc.append(
                		generateXmlRecord(
                				rs.getString(1), //id
                				anonymous ? "" : rs.getString(2), //ip
                				rs.getString(3), //userAgent	
                				anonymous ? "" : rs.getString(4), //principal
                                rs.getString(5), 
                                rs.getString(6), 
                                rs.getTimestamp(7)));
            }
            resultDoc.append("</log>");
            stmt.close();
        } catch (SQLException e) {
        	logMetacat.info("Error while logging event to database: "
                            + e.getMessage());
        } finally {
            // Return database connection to the pool
            DBConnectionPool.returnDBConnection(dbConn, serialNumber);
        }
        return resultDoc.toString();
    }
    
    /**
     * A utility method to determine if the given docid was deleted. 
     * @param docid the specified docid
     * @return true if there is a delete event for the id; false otherwise.
     */
    public boolean isDeleted(String docid) {
        boolean deleted =false;
        if(docid != null || !docid.trim().equals("")) {
            String[] docids = new String[1];
            docids[0] = docid;
            String[] events = new String[1];
            events[0]= DELETE;
            String[] ipAddress = null;
            String[] principal = null;
            Timestamp startDate = null;
            Timestamp endDate = null;
            boolean anonymous = false;
            
            String report =getReport(ipAddress, principal, docids,
                     events, startDate, endDate, anonymous);
            //System.out.println("the report is "+report);
            if(report != null && report.contains("<event>"+DELETE+"</event>") ){
                deleted = true;
            }
        }
        return deleted;
    }
    
    public Log getD1Report(String[] ipAddress, String[] principal, String[] docid,
            String event, Timestamp startDate, Timestamp endDate, boolean anonymous, Integer start, Integer count)
    {
        boolean isCreateEvent = false;
        Log log = new Log();
    	
    	NodeReference memberNode = new NodeReference();
        String nodeId = "localhost";
        try {
            nodeId = PropertyService.getProperty("dataone.nodeId");
        } catch (PropertyNotFoundException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        memberNode.setValue(nodeId);
        
        // subquery does the heavy lifting
        StringBuffer subQueryFrom = new StringBuffer();
        subQueryFrom.append("from access_log ");
        
        boolean clauseAdded = false;
        
        List<String> paramValues = new ArrayList<String>();
        if (ipAddress != null) {
        	if (clauseAdded) {
        		subQueryFrom.append(" and ");
            } else {
            	subQueryFrom.append(" where ");
            }
        	subQueryFrom.append("ip_address in (");
        	for (int i = 0; i < ipAddress.length; i++) {
        		if (i > 0) {
        			subQueryFrom.append(", ");
        		}
        		subQueryFrom.append("?");
        		paramValues.add(ipAddress[i]);
        	}
        	subQueryFrom.append(") ");
            clauseAdded = true;
        }
        if (principal != null) {
        	if (clauseAdded) {
        		subQueryFrom.append(" and ");
            } else {
            	subQueryFrom.append(" where ");
            }
        	subQueryFrom.append("principal in (");
        	for (int i = 0; i < principal.length; i++) {
        		if (i > 0) {
        			subQueryFrom.append(", ");
        		}
        		subQueryFrom.append("?");
        		paramValues.add(principal[i]);
        	}
        	subQueryFrom.append(") ");
            clauseAdded = true;
        }
        if (docid != null) {
        	if (clauseAdded) {
        		subQueryFrom.append(" and ");
            } else {
            	subQueryFrom.append(" where ");
            }
        	subQueryFrom.append("docid in (");
        	for (int i = 0; i < docid.length; i++) {
        		if (i > 0) {
        			subQueryFrom.append(", ");
        		}
        		subQueryFrom.append("?");
        		paramValues.add(docid[i]);
        	}
        	subQueryFrom.append(") ");
            clauseAdded = true;
        }
        
       //please make sure the handling of event is just before the the startDate clause!!!
        if (event != null) {
            if (clauseAdded) {
                subQueryFrom.append(" and ");
            } else {
                subQueryFrom.append(" where ");
            }
            subQueryFrom.append("event in (");
            subQueryFrom.append("?");
            String eventString = event;
            if (eventString.equals(Event.CREATE.xmlValue())) {
                isCreateEvent = true; //since the create event maps create, insert and et al, we handle it in different way
                subQueryFrom.append(",?");// for INSERT, the insert is handled by line 508
                subQueryFrom.append(",?");// for upload
                subQueryFrom.append(",?");// for UPLOAD
                subQueryFrom.append(",?");// for create
                //eventString = "insert,INSERT,upload,UPLOAD,create";
            } else {
                paramValues.add(eventString);
            }
            subQueryFrom.append(") ");
            clauseAdded = true;
        }

        if (startDate != null) {
            if (clauseAdded) {
            	subQueryFrom.append(" and ");
            } else {
            	subQueryFrom.append(" where ");
            }
            subQueryFrom.append("date_logged >= ?");
            clauseAdded = true;
        }
        if (endDate != null) {
            if (clauseAdded) {
            	subQueryFrom.append(" and ");
            } else {
            	subQueryFrom.append(" where ");
            }
            subQueryFrom.append("date_logged < ?");
            clauseAdded = true;
        }
        
        

        // count query
        String countSelect = "select count(*) ";
        
        // subquery select
        String subquerySelect = "select entryid ";
        
        // for selecting fields we want in the join
        String fieldSelect = 
        		"select " +
        		"entryid, " +
        		"id.guid as identifier, " +
        		"ip_address, " +
        		"user_agent, " +
        		"principal, " +
        		"case " +
        		"	when event = 'insert' then 'create' " +
        		" when event = 'INSERT' then 'create' " +
        		" when event = 'upload' then 'create' " +
        		" when event = 'UPLOAD' then 'create' " +
        		"	else event " +
        		"end as event, " +
        		"date_logged " +	 
        		"from access_log al, identifier id " +
        		"where al.docid = id.docid||'.'||id.rev " +
        		"and al.entryid in ";
        
        // order by
        String orderByClause = " order by entryid ";
        
        // select the count
        String countQuery = countSelect + subQueryFrom.toString();
        logMetacat.debug("The count query is " + countQuery);
		// select the fields using paged subquery and fields join query
        String pagedSubquery = DatabaseService.getInstance().getDBAdapter().getPagedQuery(subquerySelect + subQueryFrom.toString() + orderByClause, start, count);
        String pagedQuery = fieldSelect + " ( " + pagedSubquery + " ) " + orderByClause; 
        logMetacat.debug("The selection query is " + pagedQuery);
        logMetacat.debug("The startDate in the query is " + startDate);
        logMetacat.debug("The endDate in the query is " + startDate);

        DBConnection dbConn = null;
        int serialNumber = -1;
        try {
            // Get a database connection from the pool
            dbConn = DBConnectionPool.getDBConnection("EventLog.getD1Report");
            serialNumber = dbConn.getCheckOutSerialNumber();

            // Execute the query statement
            PreparedStatement fieldsStmt = dbConn.prepareStatement(pagedQuery);
            PreparedStatement countStmt = dbConn.prepareStatement(countQuery);

            //set the param values
            int parameterIndex = 1;
            for (String val: paramValues) {
            	countStmt.setString(parameterIndex, val);
            	fieldsStmt.setString(parameterIndex, val);
            	parameterIndex++;
            }
            
            if(isCreateEvent) {
                //handle the event mapping. If we add another event mapping, we need add a "?" in line 496
                //those values are not in the paramValues.
                countStmt.setString(parameterIndex, "insert");
                fieldsStmt.setString(parameterIndex, "insert");
                parameterIndex++;
                countStmt.setString(parameterIndex, "INSERT");
                fieldsStmt.setString(parameterIndex, "INSERT");
                parameterIndex++;
                countStmt.setString(parameterIndex, "upload");
                fieldsStmt.setString(parameterIndex, "upload");
                parameterIndex++;
                countStmt.setString(parameterIndex, "UPLOAD");
                fieldsStmt.setString(parameterIndex, "UPLOAD");
                parameterIndex++;
                countStmt.setString(parameterIndex, "create");
                fieldsStmt.setString(parameterIndex, "create");
                parameterIndex++;
            }
            
            if (startDate != null) {
            	countStmt.setTimestamp(parameterIndex, startDate); 
                fieldsStmt.setTimestamp(parameterIndex, startDate); 
            	parameterIndex++;
            }
            if (endDate != null) {
            	countStmt.setTimestamp(parameterIndex, endDate);
            	fieldsStmt.setTimestamp(parameterIndex, endDate);
            	parameterIndex++;
            }
            

            // for the return Log list
            List<LogEntry> logs = new Vector<LogEntry>();

            // get the fields form the query
            if (count != 0) {
                long startTime = System.currentTimeMillis();
                logMetacat.debug("Time to start to execute the selection query "+startTime);
	            fieldsStmt.execute();
	            long endTime = System.currentTimeMillis();
	            logMetacat.debug("Time to run the selection query is "+(endTime-startTime)/1000+" seconds.");
	            ResultSet rs = fieldsStmt.getResultSet();
	            //process the result and return it            
	            while (rs.next()) {
	            	LogEntry logEntry = new LogEntry();
	            	logEntry.setEntryId(rs.getString(1));
	            	
	            	Identifier identifier = new Identifier();
	            	identifier.setValue(rs.getString(2));
					logEntry.setIdentifier(identifier);
	
	            	logEntry.setIpAddress(anonymous ? "N/A" : rs.getString(3));
	            	String userAgent = "N/A";
	            	if (rs.getString(4) != null) {
	            		userAgent = rs.getString(4);
	            	}
	            	logEntry.setUserAgent(userAgent);
	            	
	            	Subject subject = new Subject();
	            	subject.setValue(anonymous ? "N/A" : rs.getString(5));
					logEntry.setSubject(subject);
					
					String logEventString = rs.getString(6);
					if(logEventString == null) {
					    logEventString = "unknown";
					}
					logEntry.setEvent(logEventString);
					logEntry.setDateLogged(rs.getTimestamp(7));
					
					logEntry.setNodeIdentifier(memberNode);
					logs.add(logEntry);
	            }
	            fieldsStmt.close();
	            long endTime2 = System.currentTimeMillis();
	            logMetacat.debug("Time to put the query result to the log is "+(endTime2-endTime)/1000+" seconds.");
            }
            
            // set what we have
            log.setLogEntryList(logs);
		    log.setStart(start);
		    log.setCount(logs.size());
            			
			// get total for out query
		    int total = 0;
		    long startTime = System.currentTimeMillis();
            logMetacat.debug("Time to start to execute the counting query "+startTime);
            countStmt.execute();
            long endTime = System.currentTimeMillis();
            logMetacat.debug("Time to run the counting query is "+(endTime-startTime)/1000+" seconds.");
            ResultSet countRs = countStmt.getResultSet();
            if (countRs.next()) {
            	total = countRs.getInt(1);
            }
            countStmt.close();
		    log.setTotal(total);

        } catch (SQLException e) {
        	logMetacat.error("Error while getting log events: " + e.getMessage(), e);
        } finally {
            // Return database connection to the pool
            DBConnectionPool.returnDBConnection(dbConn, serialNumber);
        }
        return log;
    }
    
    /**
     * Format each returned log record as an XML structure.
     * 
     * @param entryId the identifier of the log entry
     * @param ipAddress the internet protocol address for the event
     * @param the agent making the request
	 * @param principal the principal for the event (a username, etc)
	 * @param docid the identifier of the document to which the event applies
	 * @param event the string code for the event
     * @param dateLogged the date on which the event occurred
     * @return String containing the formatted XML
     */
    private String generateXmlRecord(String entryId, String ipAddress, String userAgent,
            String principal, String docid, String event, Timestamp dateLogged)
    {
        StringBuffer rec = new StringBuffer();
        rec.append("<logEntry>");
        rec.append(generateXmlElement("entryid", entryId));
        rec.append(generateXmlElement("ipAddress", ipAddress));
        rec.append(generateXmlElement("userAgent", userAgent));
        rec.append(generateXmlElement("principal", principal));
        rec.append(generateXmlElement("docid", docid));
        rec.append(generateXmlElement("event", event));
        rec.append(generateXmlElement("dateLogged", DateTimeMarshaller.serializeDateToUTC(dateLogged)));
        rec.append("</logEntry>\n");

        return rec.toString();
    }
    
    /**
     * Return an XML formatted element for a given name/value pair.
     * 
     * @param name the name of the xml element
     * @param value the content of the xml element
     * @return the formatted XML element as a String
     */
    private String generateXmlElement(String name, String value)
    {
        return "<" + name + ">" + value + "</" + name + ">";
    }
}
