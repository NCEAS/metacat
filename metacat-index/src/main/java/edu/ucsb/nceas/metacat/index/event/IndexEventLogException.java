/**
 *  '$RCSfile$'
 *    Purpose: A class represents the exceptions in the IndexEventLog process.
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

/**
 * A class represents the exceptions in the IndexEventLog process.
 * @author tao
 *
 */
public class IndexEventLogException extends Exception{
    
    /**
     * Constructor
     * @param message
     */
    public IndexEventLogException (String message) { 
        super(message); 
    }
    /**
     * Constructor
     * @param message
     * @param cause
     */
    public IndexEventLogException (String message, Exception cause) {
        super(message, cause); 
    }
}
