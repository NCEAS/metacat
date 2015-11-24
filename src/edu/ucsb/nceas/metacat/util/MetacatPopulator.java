/**
 *  '$RCSfile$'
 *    Purpose: A Class that implements administrative methods 
 *  Copyright: 2010 Regents of the University of California and the
 *             National Center for Ecological Analysis and Synthesis
 *    Authors: Michael Daigle
 * 
 *   '$Author: berkley $'
 *     '$Date: 2010-06-08 12:34:30 -0700 (Tue, 08 Jun 2010) $'
 * '$Revision: 5374 $'
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
package edu.ucsb.nceas.metacat.util;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.apache.commons.io.IOUtils;
import org.dataone.client.v2.itk.D1Client;
import org.dataone.client.v2.MNode;
import org.dataone.client.v2.formats.ObjectFormatCache;
import org.dataone.client.auth.CertificateManager;
import org.dataone.ore.ResourceMapFactory;
import org.dataone.service.exceptions.NotFound;
import org.dataone.service.types.v1.AccessPolicy;
import org.dataone.service.types.v1.AccessRule;
import org.dataone.service.types.v1.Checksum;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.NodeReference;
import org.dataone.service.types.v1.ObjectFormat;
import org.dataone.service.types.v1.Permission;
import org.dataone.service.types.v1.Session;
import org.dataone.service.types.v1.Subject;
import org.dataone.service.types.v2.SystemMetadata;
import org.dataone.service.types.v1.util.ChecksumUtil;
import org.dataone.service.util.Constants;
import org.dspace.foresite.ResourceMap;
import org.ecoinformatics.datamanager.DataManager;
import org.ecoinformatics.datamanager.database.DatabaseConnectionPoolInterface;
import org.ecoinformatics.datamanager.parser.DataPackage;

import edu.ucsb.nceas.metacat.MetaCatServlet;
import edu.ucsb.nceas.metacat.dataquery.MetacatDatabaseConnectionPoolFactory;
import edu.ucsb.nceas.metacat.properties.PropertyService;

/**
 * @author berkley
 * A class to populate a metacat instance based on documents returned from a query
 */
public class MetacatPopulator
{
    private String sourceUrl = null;
    private String destinationUrl = null;
    private String query = null;
    private String username = null;
    private String password = null;
    private Session session = null;
    private String subjectDN = null;
    
    /**
     * create a new MetacatPopulator with given source and destination urls.  
     * These should be
     * of the form "http://<url>/<metacat_instance>"
     * If username and/or password is null, the query will be run as public
     * @param sourceUrl
     * @param destUrl
     * @param query
     * @param username
     * @param password
     */
    public MetacatPopulator(String sourceUrl, String destUrl, String query, String username, String password)
    {
        this.sourceUrl = sourceUrl;
        this.query = query;
        this.username = username;
        this.password = password;
        this.destinationUrl = destUrl;
        // TODO: use specific certificate?
        this.session = null; //new Session();
        this.subjectDN = CertificateManager.getInstance().getSubjectDN(CertificateManager.getInstance().loadCertificate());
    }
    
    /**
     * populate from the source
     */
    public void populate()
      throws Exception
    {
        //String sourceSessionid = login();
        
        //do a query
        String params = "returndoctype=eml://ecoinformatics.org/eml-2.1.0&" +
                        "returndoctype=eml://ecoinformatics.org/eml-2.0.1&" +
                        "returndoctype=eml://ecoinformatics.org/eml-2.0.0&";
        params += "action=query&";
        params += "qformat=xml&";
        params += "anyfield=" + query;
        
        printHeader("Searching source");
        System.out.println("searching '" + sourceUrl + "' for '" + query + "'");
        InputStream is = getResponse(sourceUrl, "/metacat", params, "POST");
        String response = IOUtils.toString(is, MetaCatServlet.DEFAULT_ENCODING);
        //System.out.println("response: " + response);
        Vector<Document> docs = parseResponse(response);
        
        printHeader("Parsing source results");
        System.out.println("creating MN with url: " + destinationUrl + "/");
        MNode mn = D1Client.getMN(destinationUrl + "/");
        
        printHeader("Processing " + docs.size() + " results.");
        for (int i=0; i<docs.size(); i++) {
        	
        	// for generating the ORE map
            Map<Identifier, List<Identifier>> idMap = new HashMap<Identifier, List<Identifier>>();
            List<Identifier> dataIds = new ArrayList<Identifier>();
            
            //for each document in the query
            Document doc = docs.get(i);
            String docid = doc.docid;
            //get the doc from source
            printHeader("Getting document " + doc.docid + " from source " + sourceUrl);
            params = "action=read&qformat=xml&docid=" + docid;
            is = getResponse(sourceUrl, "/metacat", params, "POST");
            String doctext = IOUtils.toString(is, MetaCatServlet.DEFAULT_ENCODING);
            System.out.println("doctext: " + doctext);
            is = IOUtils.toInputStream(doctext, MetaCatServlet.DEFAULT_ENCODING);
            //parse the document
            DatabaseConnectionPoolInterface connectionPool = MetacatDatabaseConnectionPoolFactory.getDatabaseConnectionPoolInterface();
        	DataManager dataManager = DataManager.getInstance(connectionPool, connectionPool.getDBAdapterName());
        	DataPackage dataPackage = dataManager.parseMetadata(is);
        	
            if (dataPackage == null) {
                continue;
            }
            
            //go through the DistributionMetadata and download any described data
            is = IOUtils.toInputStream(doctext, MetaCatServlet.DEFAULT_ENCODING);
            doc.doctext = doctext;

            printHeader("creating document on destination " + destinationUrl);            
            SystemMetadata sysmeta = generateSystemMetadata(doc);
            
            // iterate through the data objects
            if (dataPackage.getEntityList() != null) {
	            for (int j=0; j < dataPackage.getEntityList().length; j++) {
	                String dataDocUrl = dataPackage.getEntityList()[j].getURL();
	                String dataDocMimeType = dataPackage.getEntityList()[j].getDataFormat();
	                if (dataDocMimeType == null) {
		                dataDocMimeType = 
		                	ObjectFormatCache.getInstance().getFormat("application/octet-stream").getFormatId().getValue();
	                }
	                String dataDocLocalId = "";
	                if (dataDocUrl.trim().startsWith("ecogrid://knb/")) { //we only handle ecogrid urls right now
	                    dataDocLocalId = dataDocUrl.substring(dataDocUrl.indexOf("ecogrid://knb/") + 
	                            "ecogrid://knb/".length(), dataDocUrl.length());
	                    //get the file
	                    params = "action=read&qformat=xml&docid=" + dataDocLocalId;
	                    InputStream dataDocIs = getResponse(sourceUrl, "/metacat", params, "POST");
	                    String dataDocText = IOUtils.toString(dataDocIs, MetaCatServlet.DEFAULT_ENCODING);
	                    
	                    //set the id
	                    Identifier did = new Identifier();
	                    did.setValue(dataDocLocalId);
	                    
	                    // add the data identifier for ORE map 
	                    dataIds.add(did);
	                    
	                    //create sysmeta for the data doc                    
	                    SystemMetadata dataDocSysMeta = generateSystemMetadata(doc);
	                    //overwrite the bogus values from the last call 
	                    dataDocSysMeta.setIdentifier(did);
	                    ObjectFormat format = null;
	                    try {
	                    	format = ObjectFormatCache.getInstance().getFormat(dataDocMimeType);
							dataDocSysMeta.setFormatId(format.getFormatId());
	                    } catch (NotFound e) {
							System.out.println(e.getMessage());
						}
	                    dataDocIs = IOUtils.toInputStream(dataDocText, MetaCatServlet.DEFAULT_ENCODING);
	                    String algorithm = PropertyService.getProperty("dataone.checksumAlgorithm.default");
	                    Checksum checksum = ChecksumUtil.checksum(dataDocIs, algorithm);
	                    dataDocSysMeta.setChecksum(checksum);
	                    String sizeStr = 
	                    	Long.toString(dataDocText.getBytes(MetaCatServlet.DEFAULT_ENCODING).length);
	                    dataDocSysMeta.setSize(new BigInteger(sizeStr));

	                    boolean error = false;
	                    
	                    //create the data doc on d1
	                    try {
	                        mn.create(session, dataDocSysMeta.getIdentifier(), IOUtils.toInputStream(dataDocText, MetaCatServlet.DEFAULT_ENCODING), dataDocSysMeta);
	                    }
	                    catch(Exception e) {
	                        error = true;
	                        System.out.println("ERROR: Could not create data document with id " + 
	                                dataDocSysMeta.getIdentifier().getValue() + " : " + e.getMessage());
	                    }
	                    finally {
	                        if (error) {
	                            printHeader("Insertion of document " + dataDocSysMeta.getIdentifier().getValue() + 
	                                    "FAILED.");
	                        }
	                        else {
	                            printHeader("Done inserting document " + dataDocSysMeta.getIdentifier().getValue() +
	                                " which is described by " + sysmeta.getIdentifier().getValue());
	                        }
	                    }
	                }
	                else {
	                    System.out.println("WARNING: Could not process describes url " +
	                            dataDocUrl + " for document " + doc.docid + 
	                    ".  Only ecogrid://knb/ urls are currently supported.");
	                }
	            }
            }
            
            try {
              Identifier id = 
            	  mn.create(session, sysmeta.getIdentifier(), IOUtils.toInputStream(doc.doctext, MetaCatServlet.DEFAULT_ENCODING), sysmeta);
              System.out.println("Success inserting document " + id.getValue());
              
              // no need for an ORE map if there's no data
              if (!dataIds.isEmpty()) {
	              // generate the ORE map for this datapackage
	              Identifier resourceMapId = new Identifier();
	              resourceMapId.setValue("resourceMap_" + sysmeta.getIdentifier().getValue());
	              idMap.put(sysmeta.getIdentifier(), dataIds);
	              ResourceMap rm = ResourceMapFactory.getInstance().createResourceMap(resourceMapId, idMap);
	              String resourceMapXML = ResourceMapFactory.getInstance().serializeResourceMap(rm);
	              Document rmDoc = new Document(resourceMapId.getValue(), "http://www.openarchives.org/ore/terms", "", "");
	              rmDoc.doctext = resourceMapXML;
	              SystemMetadata resourceMapSysMeta = generateSystemMetadata(rmDoc);
	              mn.create(session, resourceMapId, IOUtils.toInputStream(resourceMapXML, MetaCatServlet.DEFAULT_ENCODING), resourceMapSysMeta);
	              
            }
              
            }
            catch(Exception e) {
                e.printStackTrace();
                System.out.println("Could not create document with id " + 
                        sysmeta.getIdentifier().getValue() + " : " + e.getMessage());
            }
            finally {
                printHeader("Done processing document " + sysmeta.getIdentifier().getValue());
            }
        }
        
        //logout();
    }
    

    
    /**
     * @param doc
     * @return
     */
    private SystemMetadata generateSystemMetadata(Document doc)
      throws Exception {
        SystemMetadata sm = new SystemMetadata();
        sm.setSerialVersion(BigInteger.valueOf(1));
        //set the id
        Identifier id = new Identifier();
        id.setValue(doc.docid.trim());
        sm.setIdentifier(id);
        
        //set the object format
        ObjectFormat format = ObjectFormatCache.getInstance().getFormat(doc.doctype);
        if (format == null) {
            if (doc.doctype.trim().equals("BIN")) {
                format = ObjectFormatCache.getInstance().getFormat("application/octet-stream");
            }
            else {
                format = ObjectFormatCache.getInstance().getFormat("text/plain");
            }
        }
        sm.setFormatId(format.getFormatId());
        
        //create the checksum
        ByteArrayInputStream bais = new ByteArrayInputStream(doc.doctext.getBytes(MetaCatServlet.DEFAULT_ENCODING));
        String algorithm = PropertyService.getProperty("dataone.checksumAlgorithm.default");
        Checksum checksum = ChecksumUtil.checksum(bais, algorithm);
        sm.setChecksum(checksum);
        
        //set the size
        String sizeStr = Long.toString(doc.doctext.getBytes(MetaCatServlet.DEFAULT_ENCODING).length);
        sm.setSize(new BigInteger(sizeStr));
        
        //submitter, rights holder
        Subject p = new Subject();
        p.setValue(subjectDN);
        sm.setSubmitter(p);
        sm.setRightsHolder(p);
        try {
            Date dateCreated = parseMetacatDate(doc.createDate);
            sm.setDateUploaded(dateCreated);
            Date dateUpdated = parseMetacatDate(doc.updateDate);
            sm.setDateSysMetadataModified(dateUpdated);
        }
        catch(Exception e) {
            System.out.println("couldn't parse a date: " + e.getMessage());
            Date dateCreated = new Date();
            sm.setDateUploaded(dateCreated);
            Date dateUpdated = new Date();
            sm.setDateSysMetadataModified(dateUpdated);
        }
        NodeReference nr = new NodeReference();
        nr.setValue(PropertyService.getProperty("dataone.nodeId"));
        sm.setOriginMemberNode(nr);
        sm.setAuthoritativeMemberNode(nr);
        
        // create access policy
        AccessPolicy accessPolicy = new AccessPolicy();
        AccessRule accessRule = new AccessRule();
		accessRule.addPermission(Permission.READ);
        Subject subject = new Subject();
        subject.setValue(Constants.SUBJECT_PUBLIC);
		accessRule.addSubject(subject);
		accessPolicy.addAllow(accessRule);
		
		sm.setAccessPolicy(accessPolicy);
        
        return sm;
    }
    
    private void printHeader(String s) {
        System.out.println("****** " + s + " *******");
    }
    
    /**
     * parse the metacat date which looks like 2010-06-08 (YYYY-MM-DD) into
     * a proper date object
     * @param date
     * @return
     */
    private Date parseMetacatDate(String date)
    {
        String year = date.substring(0, 4);
        String month = date.substring(5, 7);
        String day = date.substring(8, 10);
        Calendar c = Calendar.getInstance();
        c.set(new Integer(year).intValue(), 
              new Integer(month).intValue(), 
              new Integer(day).intValue());
        return c.getTime();
    }
    
    /**
     * parse a metacat query response and return a vector of docids
     * @param response
     * @return
     */
    private Vector<Document> parseResponse(String response)
    {
        Vector<Document> v = new Vector<Document>();
        int dstart = response.indexOf("<document>");
        int dend = response.indexOf("</document>", dstart);
        while(dstart != -1)
        {
            String doc = response.substring(dstart + "<document>".length(), dend);
            //System.out.println("adding " + docid);
            Document d = new Document(getFieldFromDoc(doc, "docid"),
                    getFieldFromDoc(doc, "doctype"),
                    getFieldFromDoc(doc, "createdate"),
                    getFieldFromDoc(doc, "updatedate"));
            v.add(d);
            dstart = response.indexOf("<document>", dend);
            dend = response.indexOf("</document>", dstart);
        }
        
        return v;
    }
    
    private String getFieldFromDoc(String doc, String fieldname)
    {
        String field = "<" + fieldname + ">";
        String fieldend = "</" + fieldname + ">";
        int start = doc.indexOf(field);
        int end = doc.indexOf(fieldend);
        String s = doc.substring(start + field.length(), end);
        //System.out.println("field: " + fieldname + " : " + s);
        return s;
    }
    
    
    /**
     * returns a sessionid
     * @return
     */
    private String login()
      throws Exception
    {
        InputStream is = getResponse(sourceUrl, "/metacat", 
                "action=login&username=" + username + "&password=" + password + "&qformat=xml", "POST");
        String response = IOUtils.toString(is, MetaCatServlet.DEFAULT_ENCODING);
        //System.out.println("response: " + response);
        if(response.indexOf("sessionId") == -1)
        {
            throw new Exception("Error logging into " + sourceUrl);
        }
        
        String sessionid = response.substring(
                response.indexOf("<sessionId>") + "<sessionId>".length(), 
                response.indexOf("</sessionId>"));
        System.out.println("sessionid: " + sessionid);
        return sessionid;
    }
    
    /**
     * logout both the source and destination
     * @throws Exception
     */
    private void logout()
        throws Exception
    {
        getResponse(sourceUrl, "/metacat", "action=logout&username=" + username, "POST");
    }
    
    /**
     * get an http response
     * @param contextRootUrl
     * @param resource
     * @param urlParameters
     * @param method
     * @return
     * @throws Exception
     */
    private InputStream getResponse(String contextRootUrl, String resource, 
            String urlParameters, String method)
      throws Exception
    {
        HttpURLConnection connection = null ;

        String restURL = contextRootUrl+resource;

        if (urlParameters != null) {
            if (restURL.indexOf("?") == -1)             
                restURL += "?";
            restURL += urlParameters; 
            if(restURL.indexOf(" ") != -1)
            {
                restURL = restURL.replaceAll("\\s", "%20");
            }
        }

        URL u = null;
        InputStream content = null;            
        System.out.println("url: " + restURL);
        System.out.println("method: " + method);
        u = new URL(restURL);
        connection = (HttpURLConnection) u.openConnection();
        connection.setDoOutput(true);
        connection.setDoInput(true);
        connection.setRequestMethod(method);
        content = connection.getInputStream();
        return content;
    }
    
    private class Document
    {
        public String docid;
        public String doctype;
        public String createDate;
        public String updateDate;
        public String doctext;
        
        public Document(String docid, String doctype, String createDate, String updateDate)
        {
            this.docid = docid.trim();
            this.doctype = doctype.trim();
            this.createDate = createDate.trim();
            this.updateDate = updateDate.trim();
        }
    }
}
