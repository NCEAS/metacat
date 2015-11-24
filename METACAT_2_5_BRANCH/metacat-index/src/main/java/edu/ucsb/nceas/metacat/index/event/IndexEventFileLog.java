/**
 *  '$RCSfile$'
 *    Purpose: A class represents a file log for the index events.
 *    Copyright: 2013 Regents of the University of California and the
 *             National Center for Ecological Analysis and Synthesis
 *    Authors: Jing Tao
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
package edu.ucsb.nceas.metacat.index.event;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Vector;

import org.apache.commons.io.FileUtils;
import org.dataone.configuration.Settings;
import org.dataone.service.types.v1.Event;
import org.dataone.service.types.v1.Identifier;

import edu.ucsb.nceas.metacat.common.index.event.IndexEvent;


/**
 * A class represents a file log for the index events.
 * @author tao
 *
 */
public class IndexEventFileLog implements IndexEventLog {
    private static final String FIELDSEPERATOR = " ";
    private static final String LOGFILENAME = "solr-index.log";
    private static final String LASTPROCESSEDDATEFILENAME = "solr-last-proccessed-date";
    private File logFile = null;
    private File lastProcessedDateFile = null;
    private long index=1;
    private SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
    
    /**
     * Constructor. Initialize the log file. The log file locates solr.homeDir. 
     * If the solr.homeDir doesn't exists in the metacat.properties, the it will locate the user's home directory.
     * @throws IOException 
     */
    public IndexEventFileLog() throws IOException {
        String path = Settings.getConfiguration().getString("solr.homeDir");
        if(path == null || path.trim().equals("")) {
            path = System.getProperty("user.home");
        }
        File pathDir = new File(path);
        if(!pathDir.exists()) {
            pathDir.mkdirs();
        }
        logFile = new File(pathDir, LOGFILENAME);
        if(!logFile.exists()) {
            logFile.createNewFile();
        }
        lastProcessedDateFile = new File(pathDir, LASTPROCESSEDDATEFILENAME);
        if(!lastProcessedDateFile.exists()) {
            lastProcessedDateFile.createNewFile();
        }
    }
    /**
     * Write an IndexEvent into a file
     * @param event
     * @throws IndexEventLogException
     */
    public synchronized void write(IndexEvent event) throws IndexEventLogException {
        if(event != null) {
            Vector<String> lines = new Vector<String>();
            StringBuffer lineBuffer = new StringBuffer();
            lineBuffer.append("\""+index+"\""+FIELDSEPERATOR);
            Event type = event.getAction();
            lineBuffer.append("\"" + type.xmlValue() + "\"" + FIELDSEPERATOR);
            Date date = event.getDate();
            if(date != null) {
                DateFormat formate = new SimpleDateFormat();
                lineBuffer.append("\""+formate.format(date)+"\""+FIELDSEPERATOR);
            }
            Identifier id = event.getIdentifier();
            if(id != null) {
                lineBuffer.append("\""+id.getValue()+"\""+FIELDSEPERATOR);
            }
            String description = event.getDescription();
            if(description != null) {
                lineBuffer.append("\""+description+"\""+FIELDSEPERATOR);
            }
            lines.add(lineBuffer.toString());
            String lineEnding = null;//null means to use the system default one.
            boolean append = true;
            try {
                FileUtils.writeLines(logFile, "UTF-8", lines, append);
                //FileUtils.writeLines(logFile, "UTF-8", lines, lineEnding);
                //IOUtils.writeLines(lines, lineEnding, new FileOutputStream(logFile), "UTF-8");
            } catch (FileNotFoundException e) {
                // TODO Auto-generated catch block
                throw new IndexEventLogException(e.getMessage());
            } catch (IOException e) {
                // TODO Auto-generated catch block
                throw new IndexEventLogException(e.getMessage());
            }
            index++;
        }
        return;
    }
    
    
    /**
     * Gets the list of IndexEvent matching the specified set of filters. The filter parameters can be null
     * @param action  the action of the event
     * @param pid   the identifier of the data object in the event
     * @param start the start time of the time range for query
     * @param end   the end time of the time range for query
     * @return
     * @throws IndexEventLogException
     */
    public List<IndexEvent> getEvents(Event action, Identifier pid, Date start, Date end) throws IndexEventLogException {
        List<IndexEvent> list = null;
        return list;
    }
    
    
    /**
     * Get the latest SystemMetadata modification Date of the objects that were built
     * the solr index during the previous timed indexing process.
     * @return the date. The null will be returned if there is no such date.
     * @throws IndexEventLogException
     */
    public Date getLastProcessDate() throws IndexEventLogException {
        Date date = null;
        try {
            String dateStr = FileUtils.readFileToString(lastProcessedDateFile, "UTF-8");
            if(dateStr != null && !dateStr.trim().equals("")) {
                date = format.parse(dateStr);
            }
        } catch (IOException e) {
            throw new IndexEventLogException("IndexEventFileLog.getLastProcessedDate - couldn't read the last processed date :", e);
        } catch (ParseException e) {
            // TODO Auto-generated catch block
            throw new IndexEventLogException("IndexEventFileLog.getLastProcessedDate - couldn't read the last processed date since the content of file is not date :", e);
        }
        return date;
    }
    
    
    /**
     * Set the SystemMetadata modification Date of the objects that were built
     * the solr index during the previous timed indexing process.
     * @throws IndexEventLogException
     */
    public void setLastProcessDate(Date date) throws IndexEventLogException {
        String dateStr = format.format(date);
        try {
            FileUtils.writeStringToFile(lastProcessedDateFile, dateStr, "UTF-8");
        } catch (IOException e) {
            // TODO Auto-generated catch block
           throw new IndexEventLogException("IndexEventFileLog.setLastProcessedDate - couldn't set the last processed date :", e);
        }
    }
    
	@Override
	public void remove(Identifier identifier) throws IndexEventLogException {
		// TODO Auto-generated method stub
		
	}
}
