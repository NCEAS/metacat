/**
 *  '$RCSfile$'
 *    Purpose: A class which creates a instance of an IndexEventLog.
 *    Copyright: 2013 Regents of the University of California and the
 *             National Center for Ecological Analysis and Synthesis
 *    Authors: Leinfelder
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
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.dataone.configuration.Settings;
import org.dataone.service.types.v1.Event;
import org.dataone.service.types.v1.Identifier;

import edu.ucsb.nceas.metacat.common.index.event.IndexEvent;
import edu.ucsb.nceas.metacat.index.DistributedMapsFactory;


/**
 * @author leinfelder
 *
 */
public class HazelcastIndexEventLog implements IndexEventLog {
    
    
    private File lastProcessedDateFile = null;
    private SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
    
    /**
     * Constructor
     * @throws IOException
     */
    public HazelcastIndexEventLog() throws IOException {
        String path = Settings.getConfiguration().getString("solr.homeDir");
        if(path == null || path.trim().equals("")) {
            path = System.getProperty("user.home");
        }
        File pathDir = new File(path);
        lastProcessedDateFile = new File(pathDir, edu.ucsb.nceas.metacat.common.Settings.LASTPROCESSEDDATEFILENAME);
        if(!lastProcessedDateFile.exists()) {
            lastProcessedDateFile.createNewFile();
        }
    }

	/* (non-Javadoc)
	 * @see edu.ucsb.nceas.metacat.index.event.IndexEventLog#write(edu.ucsb.nceas.metacat.index.event.IndexEvent)
	 */
	@Override
	public void write(IndexEvent event) throws IndexEventLogException {
		// write to the map
		try {
			DistributedMapsFactory.getIndexEventMap().put(event.getIdentifier(), event);
		} catch (Exception e) {
			throw new IndexEventLogException("Could not write to event map", e);
		}
	}

	/* (non-Javadoc)
	 * @see edu.ucsb.nceas.metacat.index.event.IndexEventLog#remove(java.lang.String)
	 */
	@Override
	public void remove(Identifier identifier) throws IndexEventLogException {
		// remove from the map
		try {
			DistributedMapsFactory.getIndexEventMap().remove(identifier);
		} catch (Exception e) {
			throw new IndexEventLogException("Could not remove from event map", e);
		}

	}

	/* (non-Javadoc)
	 * @see edu.ucsb.nceas.metacat.index.event.IndexEventLog#getEvents(int, org.dataone.service.types.v1.Identifier, java.util.Date, java.util.Date)
	 */
	@Override
	public List<IndexEvent> getEvents(Event action, Identifier pid, Date start, Date end) throws IndexEventLogException {
		try {
			// TODO: query the map using the parameters
			return new ArrayList<IndexEvent>(DistributedMapsFactory.getIndexEventMap().values());
		} catch (Exception e) {
			throw new IndexEventLogException("Could not remove from event map", e);
		}
	}

	/* (non-Javadoc)
	 * @see edu.ucsb.nceas.metacat.index.event.IndexEventLog#getLastProcessDate()
	 */
	@Override
	public Date getLastProcessDate() throws IndexEventLogException {
	    Date date = null;
        try {
            String dateStr = FileUtils.readFileToString(lastProcessedDateFile, "UTF-8");
            if(dateStr != null && !dateStr.trim().equals("")) {
                date = format.parse(dateStr);
            }
        } catch (IOException e) {
            throw new IndexEventLogException("HazelcastIndexEventLog.getLastProcessedDate - couldn't read the last processed date :", e);
        } catch (ParseException e) {
            // TODO Auto-generated catch block
            throw new IndexEventLogException("HazelcastIndexEventLog.getLastProcessedDate - couldn't read the last processed date since the content of file is not date :", e);
        }
        return date;
	}

	/* (non-Javadoc)
	 * @see edu.ucsb.nceas.metacat.index.event.IndexEventLog#setLastProcessDate(java.util.Date)
	 */
	@Override
	public void setLastProcessDate(Date date) throws IndexEventLogException {
	    String dateStr = format.format(date);
        try {
            FileUtils.writeStringToFile(lastProcessedDateFile, dateStr, "UTF-8");
        } catch (IOException e) {
            // TODO Auto-generated catch block
           throw new IndexEventLogException("HazelcastIndexEventLog.setLastProcessedDate - couldn't set the last processed date :", e);
        }

	}

}
