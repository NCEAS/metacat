/**
 *  '$RCSfile$'
 *    Purpose: A class which creates a instance of an IndexEventLog.
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

import org.dataone.configuration.Settings;


/**
 *  A class which creates a instance of an IndexEventLog.
 *  This class will read the metacat.properties file and create a instance of the EventLog.
 * @author tao
 *
 */
public class EventlogFactory {
    private static IndexEventLog eventLog = null;
    
    
    /**
     * Create an IndexEventLog. This is a singleton object.
     * @return
     * @throws ClassNotFoundException 
     * @throws IllegalAccessException 
     * @throws InstantiationException 
     */
    public static IndexEventLog createIndexEventLog() throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        if(eventLog == null) {
            String className = Settings.getConfiguration().getString("index.eventlog.classname");
            Class<?> classObj = Class.forName(className);
            eventLog = (IndexEventLog)classObj.newInstance();
        }
        return eventLog;
    }
}
