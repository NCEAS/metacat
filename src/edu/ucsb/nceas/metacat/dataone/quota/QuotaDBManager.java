/**
 *    Purpose: Implements a service for managing a Hazelcast cluster member
 *  Copyright: 2020 Regents of the University of California and the
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
package edu.ucsb.nceas.metacat.dataone.quota;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.dataone.bookkeeper.api.Usage;

/**
 * Represents a class to manipulate the usages table for the quota service in the postgresql database
 * @author tao
 *
 */
public class QuotaDBManager {
    private static final String TABLE = "usages";
    private static final String USAGEID = "usage_id";
    private static final String QUOTAID = "quota_id";
    private static final String INSTANCEID = "instance_id";
    private static final String QUANTITY = "quantity";
    private static final String DATEREPORTED = "date_reported";
    
    /**
     * Create a usage record in the usages table. 
     * However we don't specify the date_reported field here. It will be set when Metacat successfully reports the usage to the remote bookkeeper server.
     * @param usage  the usage will be record into the db table
     */
    public static void createUsage(Usage usage) {
        
    }
    
    /**
     * Set the reported date for a given usage
     * @param usageId  the id of the usage will be set
     * @param date  the date to report to the bookkeeper server
     */
    public static void setReportedDate(String usageId, Date date) {
        
    }
    
    /**
     * Get the list of usages which haven't been reprorted to the bookkeeper server.
     * The indication is the reported date is null in the usages table
     * @return  the list of usages which usages which haven't been reprorted to the bookkeeper server
     */
    public static List<Usage> getUnReportedUsages() {
        List<Usage> list = new ArrayList<Usage>();
        return list;
    }
    

}
