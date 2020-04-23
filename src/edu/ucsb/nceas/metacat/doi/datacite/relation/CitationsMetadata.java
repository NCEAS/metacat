/**
 *  '$RCSfile$'
 *  Copyright: 2020 Regents of the University of California and the
 *              National Center for Ecological Analysis and Synthesis
 *
 *   '$Author:  $'
 *     '$Date:  $'
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
package edu.ucsb.nceas.metacat.doi.datacite.relation;

import java.util.List;

/**
 * A class represents an object of the citation metadata in the query response
 * @author tao
 *
 */
public class CitationsMetadata {
    private String target_id = null;
    private String source_id = null;
    private String source_url = null;
    private List<CitationsOrigin> origin = null;
    private String title = null;
    private String publisher = null;
    private String journal = null;
    private String volume = null;
    private String page = null;
    private int year_of_publishing;
    
    /**
     * Get the target id in the response
     * @return  the target id
     */
    public String getTarget_id() {
        return target_id;
    }
    
    /**
     * Set the target id in the response
     * @param target_id the id will be set
     */
    public void setTarget_id(String target_id) {
        this.target_id = target_id;
    }
    
    /**
     * Get the source id in the response
     * @return  the source id
     */
    public String getSource_id() {
        return source_id;
    }
    
    /**
     * Set the source id in the response
     * @param source_id  the source id will be set
     */
    public void setSource_id(String source_id) {
        this.source_id = source_id;
    }
    
    /**
     * Get the source id in the response
     * @return  the source id of the response
     */
    public String getSource_url() {
        return source_url;
    }
    
    /**
     * Get the source url in the response
     * @param source_url  the source url
     */
    public void setSource_url(String source_url) {
        this.source_url = source_url;
    }
    
    /**
     * Get the list of originators in the response
     * @return  the list of originators
     */
    public List<CitationsOrigin> getOrigin() {
        return origin;
    }
    
    /**
     * Set the originators in the response
     * @param origin  the list of originators will be set
     */
    public void setOrigin(List<CitationsOrigin> origin) {
        this.origin = origin;
    }
    
    /**
     * Get the title of the object in the response
     * @return  the title of the object
     */
    public String getTitle() {
        return title;
    }
    
    /**
     * Set the title of object in the response
     * @param title  the title will be set
     */
    public void setTitle(String title) {
        this.title = title;
    }
    
    /**
     * Get the publisher of the object in the response
     * @return  the publisher of the object
     */
    public String getPublisher() {
        return publisher;
    }
    
    /**
     * Set the publisher for the object
     * @param publisher the publisher will be set
     */
    public void setPublisher(String publisher) {
        this.publisher = publisher;
    }
    
    /**
     * Get the journal in which the object was published
     * @return  the name of the journal
     */
    public String getJournal() {
        return journal;
    }
    
    /**
     * Set the journal in which the object was published
     * @param journal  the name of journal will be set
     */
    public void setJournal(String journal) {
        this.journal = journal;
    }
    
    /**
     * Get the volume of the journal in which the object was published
     * @return  the volume of the journal
     */
    public String getVolume() {
        return volume;
    }
    
    /**
     * Set the volume of the journal in which the object was published
     * @param volume  the volume will be set
     */
    public void setVolume(String volume) {
        this.volume = volume;
    }
    
    /**
     * Get the page number of the volume in which the object was published
     * @return  the page number
     */
    public String getPage() {
        return page;
    }
    
    /**
     * Set the page number of the volume in which the object was published
     * @param page  the page number will be set
     */
    public void setPage(String page) {
        this.page = page;
    }
    
    /**
     * Get the year in which the object was published
     * @return  the publishing year
     */
    public int getYear_of_publishing() {
        return year_of_publishing;
    }
    
    /**
     * Set the publishing year for the object
     * @param year_of_publishing  the year will be set
     */
    public void setYear_of_publishing(int year_of_publishing) {
        this.year_of_publishing = year_of_publishing;
    }
   


}
