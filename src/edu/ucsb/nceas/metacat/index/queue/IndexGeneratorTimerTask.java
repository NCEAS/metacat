/**
 *  '$RCSfile$'
 *  Copyright: 2022 Regents of the University of California and the
 *              National Center for Ecological Analysis and Synthesis
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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.TimerTask;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.solr.client.solrj.SolrServerException;
import org.dataone.configuration.Settings;
import org.dataone.service.exceptions.InvalidRequest;
import org.dataone.service.exceptions.InvalidToken;
import org.dataone.service.exceptions.NotAuthorized;
import org.dataone.service.exceptions.NotFound;
import org.dataone.service.exceptions.NotImplemented;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.exceptions.UnsupportedType;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.ObjectFormatIdentifier;
import org.dataone.service.types.v2.SystemMetadata;
import org.dspace.foresite.OREParserException;
import org.xml.sax.SAXException;

import edu.ucsb.nceas.metacat.IdentifierManager;
import edu.ucsb.nceas.metacat.common.resourcemap.ResourceMapNamespaces;
import edu.ucsb.nceas.metacat.index.MetacatSolrIndex;
import edu.ucsb.nceas.metacat.systemmetadata.SystemMetadataManager;


/**
 * A timer to regenerate failed index tasks or index tasks in a given time frame.
 */
public class IndexGeneratorTimerTask extends TimerTask {
    
    private static final int FIRST =0;
    private static final int SECOND =1;
    private static final int THIRD = 2;
    private static final int FOURTH = 3;
    public static final int WAITTIME = 10000;
    public static final int MAXWAITNUMBER = 180;
    private static final String HTTP = "http://";
    private static final String MNAPPENDIX = "/d1/mn";
    
    private static Log log = LogFactory.getLog(IndexGeneratorTimerTask.class);
    private static List<String> resourceMapNamespaces = null;
    //objects whose modified date is younger than the last process date
    private boolean needReindexSinceLastProcessDate = false;
    
    /**
     * Constructor
     * @param solrIndex
     * @param systemMetadataListener
     */
    public IndexGeneratorTimerTask() {
        resourceMapNamespaces = ResourceMapNamespaces.getNamespaces();
        try {
            needReindexSinceLastProcessDate = Settings.getConfiguration()
                                            .getBoolean("index.regenerate.sincelastProcessDate");
        } catch (Exception e) {
            log.warn("IndexGeneratorTimeTask.constructor - the value of property - "
                       + "index.regenerate.sincelastProcessDate can't be got since "
                        + e.getMessage()+" and we will set it to true as default.");
            needReindexSinceLastProcessDate = false;
        }
    }

    /**
     * Build the index for all documents.
     * @throws SolrServerException 
     * @throws ServiceFailure 
     * @throws NotImplemented 
     * @throws NotAuthorized 
     * @throws InvalidToken 
     * @throws InvalidRequest 
     * @throws IndexEventLogException 
     * @throws IllegalAccessException 
     * @throws InstantiationException 
     * @throws ClassNotFoundException 
     * @throws ParserConfigurationException 
     * @throws SAXException 
     * @throws IOException 
     * @throws UnsupportedType 
     * @throws NotFound 
     * @throws XPathExpressionException 
     * @throws OREParserException 
     */
    public void indexAll() throws InvalidRequest, InvalidToken,
                NotAuthorized, NotImplemented, ServiceFailure, SolrServerException,
                ClassNotFoundException, InstantiationException, IllegalAccessException,
                XPathExpressionException, NotFound, UnsupportedType, IOException, SAXException,
                ParserConfigurationException, OREParserException {
        Date since = null;
        Date until = null;
        index(since, until);
    }
    
    /**
     * Build the index for the docs which have been modified since the specified date.
     * @param since
     * @throws SolrServerException 
     * @throws ServiceFailure 
     * @throws NotImplemented 
     * @throws NotAuthorized 
     * @throws InvalidToken 
     * @throws InvalidRequest 
     * @throws IndexEventLogException 
     * @throws IllegalAccessException 
     * @throws InstantiationException 
     * @throws ClassNotFoundException 
     * @throws ParserConfigurationException 
     * @throws SAXException 
     * @throws IOException 
     * @throws UnsupportedType 
     * @throws NotFound 
     * @throws XPathExpressionException 
     * @throws OREParserException 
     */
    public void index(Date since) throws InvalidRequest, InvalidToken,
                    NotAuthorized, NotImplemented, ServiceFailure, SolrServerException,
                    ClassNotFoundException, InstantiationException, IllegalAccessException,
                    XPathExpressionException, NotFound, UnsupportedType, IOException, SAXException,
                    ParserConfigurationException, OREParserException {
        Date until = null;
        index(since, until);
    }
    
    /**
     *  Build the index for the docs which have been modified between the specified dates.
     * @param since  the start date
     * @param until  the end date
     * @throws SolrServerException 
     * @throws ServiceFailure 
     * @throws NotImplemented 
     * @throws NotAuthorized 
     * @throws InvalidToken 
     * @throws InvalidRequest 
     * @throws IndexEventLogException 
     * @throws IllegalAccessException 
     * @throws InstantiationException 
     * @throws ClassNotFoundException 
     * @throws ParserConfigurationException 
     * @throws SAXException 
     * @throws IOException 
     * @throws UnsupportedType 
     * @throws NotFound 
     * @throws XPathExpressionException 
     * @throws OREParserException 
     */
    public void index(Date since, Date until) throws SolrServerException, InvalidRequest,
                                                InvalidToken, NotAuthorized, NotImplemented,
                                                ServiceFailure, ClassNotFoundException,
                                                InstantiationException, IllegalAccessException,
                                                XPathExpressionException, NotFound, UnsupportedType,
                                                IOException, SAXException,
                                                ParserConfigurationException, OREParserException {
        Date processedDate = null;
        List<String> solrIds = null;
        List[] metacatIds = getMetacatIds(since, until);
        List<String> otherMetacatIds = metacatIds[FIRST];
        List<String> resourceMapIds =  metacatIds[SECOND];
        //figure out the procesedDate by comparing the last element
        //of otherMetacatIds and resourceMapIds.
        List<Long> maxCollection = new ArrayList<Long>();
        Date latestOtherId = null;
        if (otherMetacatIds != null && !otherMetacatIds.isEmpty()) {
            int size = otherMetacatIds.size();
            String id = otherMetacatIds.get(size-1);
            SystemMetadata sysmeta = getSystemMetadata(id);
            latestOtherId = sysmeta.getDateSysMetadataModified();
            maxCollection.add(new Long(latestOtherId.getTime()));
        }
        Date latestResourceId = null;
        if (resourceMapIds != null && !resourceMapIds.isEmpty()) {
            int size = resourceMapIds.size();
            String id = resourceMapIds.get(size-1);
            SystemMetadata sysmeta = getSystemMetadata(id);
            latestResourceId = sysmeta.getDateSysMetadataModified();
            maxCollection.add(new Long(latestResourceId.getTime()));
        }
        if(!maxCollection.isEmpty()) {
            Long max = Collections.max(maxCollection);
            processedDate = new Date(max.longValue());
        }
        index(otherMetacatIds);
        index(resourceMapIds);
       
        //record the timed index.
        if(processedDate != null) {
            LastReindexDateManager.getInstance().setLastProcessDate(processedDate);
        }
        
    }

    /**
     * Put the ids into the index queue
     * @param metacatIds  the list of ids will be put into the index queue
     */
    protected static void index(List<String> metacatIds) {
        if(metacatIds != null) {
            for(String metacatId : metacatIds) {
                try {
                    submitIndex(metacatId);
                } catch (Exception e) {
                    log.warn("IndexGeneratorTimeTask.index - can't submit the index task for the id"
                             + " " + metacatId +" since "+e.getMessage());
                }
            }
        }
    }
    
    /**
     * Submit the index task to the queue for the given id
     * @param id the id will be submitted
     * @throws Exception 
     */
    protected static void submitIndex(String id) throws Exception {
        if(id != null) {
            SystemMetadata sysmeta = getSystemMetadata(id);
            Identifier pid = new Identifier();
            pid.setValue(id);
            boolean isSysmetaChangeOnly = false;
            boolean followRevisions = false;
            MetacatSolrIndex.getInstance()
                                .submit(pid, sysmeta, isSysmetaChangeOnly, followRevisions);
            log.info("IndexGenerator.index - submitted the pid " + pid.getValue()
                       + " into RabbitMQ successfully.");
       }
    }
    
    public void run() {
        log.info("IndexGenerator.run - start to run the index generator timer");
        if(needReindexSinceLastProcessDate) {
            log.info("IndexGenerator.run - start to index objects whose modified date is younger "
                        + " than the last process date");
            Date since;
            try {
                since = LastReindexDateManager.getInstance().getLastProcessDate();
                index(since);
            } catch (IOException | ParseException | InvalidRequest | InvalidToken
                       | NotAuthorized | NotImplemented | ServiceFailure
                       | ClassNotFoundException | InstantiationException
                       | IllegalAccessException | XPathExpressionException
                       | NotFound | UnsupportedType | SolrServerException
                       | SAXException | ParserConfigurationException | OREParserException e) {
                log.info("IndexGenerator.run - Metadata-Index couldn't generate indexes for "
                           + "those documents which haven't been indexed : " + e.getMessage());
            }
        }
    }
    
   
    

    /**
     * Get an array list of ids of the metacat which has the systemmetadata modification date
     * in the range. If since and util are null, it will return all of them.
     * The first element of the list is the ids except the resource map. The second elements of
     * the list is the ids of the resource map. The reason to split them is when we index the
     * resource map, we need the index of the documents in the resource map ready.
     * The last element in the each list has the latest SystemMetadata modification date.
     * But they are not sorted.
     * @param since
     * @param until
     * @return  the list of pids which should be reindexed
     * @throws InvalidRequest
     * @throws InvalidToken
     * @throws NotAuthorized
     * @throws NotImplemented
     * @throws ServiceFailure
     * @throws FileNotFoundException
     */
    private List[] getMetacatIds(Date since, Date until) throws InvalidRequest,
                        InvalidToken, NotAuthorized, NotImplemented, ServiceFailure,
                        FileNotFoundException {
        String fileName = "ids-from-hazelcast";
        List<String> resourceMapIds = new ArrayList();
        List<String> otherIds = new ArrayList();
        List[] ids = new List[2];
        ids[FIRST]= otherIds;
        ids[SECOND] = resourceMapIds;
        List<String> metacatIds = IdentifierManager.getInstance().getGUIDsByTimeRange(since, until);
        if(metacatIds != null) {
            for(String identifier : metacatIds) {
                if(identifier != null && !identifier.trim().equals("")) {
                    SystemMetadata sysmeta = getSystemMetadata(identifier);
                    if(sysmeta != null) {
                        ObjectFormatIdentifier formatId =sysmeta.getFormatId();
                        if(formatId != null && formatId.getValue() != null
                                && resourceMapNamespaces != null && isResourceMap(formatId)) {
                            resourceMapIds.add(identifier);
                        } else {
                            otherIds.add(identifier);
                        }
                    }
                }
            }
        }
        return ids;
    }
    
    /**
     * If the specified ObjectFormatIdentifier is a resroucemap namespace.
     * @param formatId  the given format id
     */
   public static boolean isResourceMap(ObjectFormatIdentifier formatId) {
       return ResourceMapNamespaces.isResourceMap(formatId);
    }
    
    /**
     * Get the SystemMetadata for the specified id from the distributed Map.
     * The null maybe is returned if there is no system metadata found.
     * @param id  the specified id.
     * @return the SystemMetadata associated with the id.
     * @throws ServiceFailure
     */
    protected static SystemMetadata getSystemMetadata(String id) throws ServiceFailure {
        SystemMetadata metadata = null;
        if(id != null && !id.trim().equals("")) {
            Identifier identifier = new Identifier();
            identifier.setValue(id);
            metadata = SystemMetadataManager.getInstance().get(identifier);
        }
        return metadata;
    }
    
    /**
     * Overwrite and do nothing
     */
    public boolean cancel() {
        return true;
    }

}
