
/**
 *    Copyright: 2023 Regents of the University of California and the
 *             National Center for Ecological Analysis and Synthesis
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
package edu.ucsb.nceas.metacat.index.queue;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.io.FileUtils;
import org.dataone.configuration.Settings;



/**
 * A class to save/retrieve the last re-index date.
 * @author tao
 *
 */
public class LastReindexDateManager {
    private static final String FIELDSEPERATOR = " ";
    private static final String LOGFILENAME = "solr-index.log";
    private static final String LASTPROCESSEDDATEFILENAME = "solr-last-proccessed-date";
    private File logFile = null;
    private File lastProcessedDateFile = null;
    private long index=1;
    private SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
    private static LastReindexDateManager manager = null;
    
    /**
     * Get the singleton instance of the class
     * @return  the LastReindexDateManager instance
     * @throws IOException
     */
    public static LastReindexDateManager getInstance() throws IOException {
        if (manager == null) {
            synchronized(LastReindexDateManager.class) {
                if (manager == null) {
                    manager = new LastReindexDateManager();
                }
            }
        }
        return manager;
    }
    
    /**
     * Constructor. Initialize the log file. The log file locates solr.homeDir. 
     * If the solr.homeDir doesn't exists in the metacat.properties, the it will locate the user's home directory.
     * @throws IOException 
     */
    private LastReindexDateManager() throws IOException {
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
     * Get the latest SystemMetadata modification Date of the objects that were built
     * the solr index during the previous timed indexing process.
     * @return the date. The null will be returned if there is no such date.
     * @throws IndexEventLogException
     */
    public Date getLastProcessDate() throws IOException, ParseException {
        Date date = null;
        String dateStr = FileUtils.readFileToString(lastProcessedDateFile, "UTF-8");
        if(dateStr != null && !dateStr.trim().equals("")) {
            date = format.parse(dateStr);
        }
        return date;
    }
    
    /**
     * Set the SystemMetadata modification Date of the objects that were built
     * the solr index during the previous timed indexing process.
     * @throws IndexEventLogException
     */
    public void setLastProcessDate(Date date) throws IOException {
        String dateStr = format.format(date);
        FileUtils.writeStringToFile(lastProcessedDateFile, dateStr, "UTF-8");
    }
    
}
