/**
 *  '$RCSfile$'
 *    Purpose: An interface to store and query the IndexEvent.
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

import java.util.Date;
import java.util.List;

import org.dataone.service.types.v1.Event;
import org.dataone.service.types.v1.Identifier;

import edu.ucsb.nceas.metacat.common.index.event.IndexEvent;

/**
 * An interface to store and query the IndexEvent.
 * @author tao
 *
 */
public interface IndexEventLog {
    
    /**
     * Write an IndexEvent into a storage
     * @param event
     * @return the serial number for this event
     * @throws IndexEventLogException
     */
    public void write(IndexEvent event) throws IndexEventLogException;
    
    /**
     * Remove an IndexEvent in storage
     * @param identifier
     * @throws IndexEventLogException
     */
    public void remove(Identifier identifier) throws IndexEventLogException;
    
    /**
     * Gets the list of IndexEvent matching the specified set of filters. The filter parameters can be null
     * @param type  the type of the event
     * @param pid   the identifier of the data object in the event
     * @param start the start time of the time range for query
     * @param end   the end time of the time range for query
     * @return
     * @throws IndexEventLogException
     */
    public List<IndexEvent> getEvents(Event action, Identifier pid, Date start, Date end) throws IndexEventLogException;
    
    /**
     * Get the latest SystemMetadata modification Date of the objects that were built
     * the solr index during the previous timed indexing process.
     * @return the date. The null will be returned if there is no such date.
     * @throws IndexEventLogException
     */
    public Date getLastProcessDate() throws IndexEventLogException;
    
    
    /**
     * Set the SystemMetadata modification Date of the objects that were built
     * the solr index during the previous timed indexing process.
     * @throws IndexEventLogException
     */
    public void setLastProcessDate(Date date) throws IndexEventLogException;
 
}
