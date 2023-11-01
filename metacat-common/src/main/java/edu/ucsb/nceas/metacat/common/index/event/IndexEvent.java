/**
 *  '$RCSfile$'
 *    Purpose: A class represents an event for the solr indexing.
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
package edu.ucsb.nceas.metacat.common.index.event;

import java.io.Serializable;
import java.util.Date;

import org.dataone.service.types.v1.Identifier;


/**
 * A class represents an event for the solr indexing.
 * @author tao
 *
 */
public class IndexEvent implements Serializable {
    private static final String FAILURE = "failure";
    private static final String UNDERSCORE = "_";
    private static final String QUEUE = "queue";
    private static final long serialVersionUID = 1L;

    //Events(actions). Delete events should start with "delete".
    public static final String CREATE = "create";
    public static final String DELETE = "delete";
    public static final String CREATE_FAILURE_TO_QUEUE = CREATE + UNDERSCORE + FAILURE 
                                                         + UNDERSCORE + "to" + UNDERSCORE + QUEUE;
    public static final String DELETE_FAILURE_TO_QUEUE = DELETE + UNDERSCORE + FAILURE 
                                                         + UNDERSCORE + "to" + UNDERSCORE + QUEUE;

    private String action = null;
    private Date date = null;
    private Identifier identifier = null;
    private String description = null;

    /**
     * Get the action of the event.
     * @return the action of the event
     */
    public String getAction() {
        return this.action;
    }

    /**
     * Set the action of the event
     * @param action  the action should be the String constants defined in
     * this class, such as CREATE
     */
    public void setAction(String action) {
        this.action = action;
    }

    /**
     * Get the date when the event happened
     * @return
     */
    public Date getDate() {
        return date;
    }

    /**
     * Set the date when the event happened
     * @param date
     */
    public void setDate(Date date) {
        this.date = date;
    }

    /**
     * Get the identifier of the data object involving the event
     * @return
     */
    public Identifier getIdentifier() {
        return identifier;
    }

    /**
     * Set the identifier of the data object involving the event.
     * @param pid
     */
    public void setIdentifier(Identifier pid) {
        this.identifier = pid;
    }

    /**
     * Get the description of the event
     * @return
     */
    public String getDescription() {
        return description;
    }

    /**
     * Set the description of the event
     * @param description
     */
    public void setDescription(String description) {
        this.description = description;
    }

}
