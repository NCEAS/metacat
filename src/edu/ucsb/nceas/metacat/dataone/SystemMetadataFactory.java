/**
 *  '$RCSfile$'
 *    Purpose: A Class for upgrading the database to version 1.5
 *  Copyright: 2000 Regents of the University of California and the
 *             National Center for Ecological Analysis and Synthesis
 *    Authors: Saurabh Garg
 *
 *   '$Author$'
 *     '$Date$'
 * '$Revision$'
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
package edu.ucsb.nceas.metacat.dataone;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.URL;
import java.net.URLConnection;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.apache.wicket.protocol.http.mock.MockHttpServletRequest;
import org.dataone.client.v2.formats.ObjectFormatCache;
import org.dataone.eml.DataoneEMLParser;
import org.dataone.eml.EMLDocument;
import org.dataone.eml.EMLDocument.DistributionMetadata;
import org.dataone.exceptions.MarshallingException;
import org.dataone.ore.ResourceMapFactory;
import org.dataone.service.exceptions.BaseException;
import org.dataone.service.exceptions.NotFound;
import org.dataone.service.types.v1.AccessPolicy;
import org.dataone.service.types.v1.AccessRule;
import org.dataone.service.types.v1.Checksum;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.NodeReference;
import org.dataone.service.types.v1.ObjectFormatIdentifier;
import org.dataone.service.types.v1.ReplicationPolicy;
import org.dataone.service.types.v1.Session;
import org.dataone.service.types.v1.Subject;
import org.dataone.service.types.v2.SystemMetadata;
import org.dataone.service.types.v1.util.ChecksumUtil;
import org.dataone.service.util.DateTimeMarshaller;
import org.dspace.foresite.ResourceMap;
import org.xml.sax.SAXException;

import java.util.Calendar;

import edu.ucsb.nceas.metacat.AccessionNumber;
import edu.ucsb.nceas.metacat.AccessionNumberException;
import edu.ucsb.nceas.metacat.DBUtil;
import edu.ucsb.nceas.metacat.DocumentImpl;
import edu.ucsb.nceas.metacat.IdentifierManager;
import edu.ucsb.nceas.metacat.McdbDocNotFoundException;
import edu.ucsb.nceas.metacat.McdbException;
import edu.ucsb.nceas.metacat.MetaCatServlet;
import edu.ucsb.nceas.metacat.MetacatHandler;
import edu.ucsb.nceas.metacat.accesscontrol.AccessControlException;
import edu.ucsb.nceas.metacat.client.InsufficientKarmaException;
import edu.ucsb.nceas.metacat.dataone.hazelcast.HazelcastService;
import edu.ucsb.nceas.metacat.index.MetacatSolrIndex;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.metacat.replication.ReplicationService;
import edu.ucsb.nceas.metacat.shared.AccessException;
import edu.ucsb.nceas.metacat.shared.HandlerException;
import edu.ucsb.nceas.metacat.util.DocumentUtil;
import edu.ucsb.nceas.utilities.ParseLSIDException;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;

public class SystemMetadataFactory {

	public static final String RESOURCE_MAP_PREFIX = "resourceMap_";
	private static Logger logMetacat = Logger.getLogger(SystemMetadataFactory.class);
	/**
	 * use this flag if you want to update any existing system metadata values with generated content
	 */
	private static boolean updateExisting = true;
	
	
	
	/**
	 * Create a system metadata object for insertion into metacat
	 * @param localId
	 * @param includeORE
	 * @param downloadData
	 * @return
	 * @throws McdbException
	 * @throws McdbDocNotFoundException
	 * @throws SQLException
	 * @throws IOException
	 * @throws AccessionNumberException
	 * @throws ClassNotFoundException
	 * @throws InsufficientKarmaException
	 * @throws ParseLSIDException
	 * @throws PropertyNotFoundException
	 * @throws BaseException
	 * @throws NoSuchAlgorithmException
	 * @throws MarshallingException
	 * @throws AccessControlException
	 * @throws HandlerException
	 * @throws SAXException
	 * @throws AccessException
	 */
	public static SystemMetadata createSystemMetadata(String localId, boolean includeORE, boolean downloadData)
            throws McdbException, McdbDocNotFoundException, SQLException,
            IOException, AccessionNumberException, ClassNotFoundException,
            InsufficientKarmaException, ParseLSIDException,
            PropertyNotFoundException, BaseException, NoSuchAlgorithmException,
            MarshallingException, AccessControlException, HandlerException, SAXException, AccessException {
	        boolean indexDataFile = false;
	        return createSystemMetadata(indexDataFile, localId, includeORE, downloadData);
	}
	/**
	 * Creates a system metadata object for insertion into metacat
	 * @param indexDataFile
	 *            Indicate if we need to index data file.
	 * 
	 * @param localId
	 *            The local document identifier
	 * @param user
	 *            The user submitting the system metadata document
	 * @param groups
	 *            The groups the user belongs to
	 * 
	 * @return sysMeta The system metadata object created
	 * @throws SAXException 
	 * @throws HandlerException 
	 * @throws AccessControlException 
	 * @throws AccessException 
	 */
	public static SystemMetadata createSystemMetadata(boolean indexDataFile, String localId, boolean includeORE, boolean downloadData)
			throws McdbException, McdbDocNotFoundException, SQLException,
			IOException, AccessionNumberException, ClassNotFoundException,
			InsufficientKarmaException, ParseLSIDException,
			PropertyNotFoundException, BaseException, NoSuchAlgorithmException,
			MarshallingException, AccessControlException, HandlerException, SAXException, AccessException {
		
		logMetacat.debug("createSystemMetadata() called for localId " + localId);

		// check for system metadata
		SystemMetadata sysMeta = null;
		
		AccessionNumber accNum = new AccessionNumber(localId, "NONE");
		int rev = Integer.valueOf(accNum.getRev());
		
		// get/make the guid
		String guid = null;
		try {
			// get the guid if it exists
			guid = IdentifierManager.getInstance().getGUID(accNum.getDocid(), rev);
		} catch (McdbDocNotFoundException dnfe) {
			// otherwise create the mapping
			logMetacat.debug("No guid found in the identifier table.  Creating mapping for " + localId);
			IdentifierManager.getInstance().createMapping(localId, localId);
			guid = IdentifierManager.getInstance().getGUID(accNum.getDocid(), rev);			
		}
		
		// look up existing system metadata if it exists
		Identifier identifier = new Identifier();
		identifier.setValue(guid);
		try {
			logMetacat.debug("Using hazelcast to get system metadata");
			sysMeta = HazelcastService.getInstance().getSystemMetadataMap().get(identifier);
			// TODO: if this is the case, we could return here -- what else do we gain?
			if (!updateExisting ) {
				return sysMeta;
			}
		} catch (Exception e) {
			logMetacat.debug("No system metadata found in hz: " + e.getMessage());

		}

		if (sysMeta == null) {
			// create system metadata
			sysMeta = new SystemMetadata();
			sysMeta.setIdentifier(identifier);
			sysMeta.setSerialVersion(BigInteger.valueOf(1));
			sysMeta.setArchived(false);
		}
		
		// get additional docinfo
		Hashtable<String, String> docInfo = ReplicationService.getDocumentInfoMap(localId);
		// set the default object format
		String doctype = docInfo.get("doctype");
		ObjectFormatIdentifier fmtid = null;

		// set the object format, fall back to defaults
		if (doctype.trim().equals("BIN")) {
			// we don't know much about this file (yet)
			fmtid = ObjectFormatCache.getInstance().getFormat("application/octet-stream").getFormatId();
		} else if (doctype.trim().equals("metadata")) {
			// special ESRI FGDC format
			fmtid = ObjectFormatCache.getInstance().getFormat("FGDC-STD-001-1998").getFormatId();
		} else {
			try {
				// do we know the given format?
				fmtid = ObjectFormatCache.getInstance().getFormat(doctype).getFormatId();
			} catch (NotFound nfe) {
				// format is not registered, use default
				fmtid = ObjectFormatCache.getInstance().getFormat("text/plain").getFormatId();
			}
		}

		sysMeta.setFormatId(fmtid);
		logMetacat.debug("The ObjectFormat for " + localId + " is " + fmtid.getValue());

		// for retrieving the actual object
		InputStream inputStream = null;
		inputStream = MetacatHandler.read(localId);

		// create the checksum
		String algorithm = PropertyService.getProperty("dataone.checksumAlgorithm.default");
		Checksum checksum = ChecksumUtil.checksum(inputStream, algorithm);
		logMetacat.debug("The checksum for " + localId + " is " + checksum.getValue());
		sysMeta.setChecksum(checksum);
		
		// set the size from file on disk, don't read bytes again
		File fileOnDisk = getFileOnDisk(localId);
		long fileSize = 0;
		if (fileOnDisk.exists()) {
			fileSize = fileOnDisk.length();
		}
		sysMeta.setSize(BigInteger.valueOf(fileSize));
		
		// submitter
		Subject submitter = new Subject();
		submitter.setValue(docInfo.get("user_updated"));
		sysMeta.setSubmitter(submitter);
		
		// rights holder
		Subject owner = new Subject();
		owner.setValue(docInfo.get("user_owner"));
		sysMeta.setRightsHolder(owner);

		// dates
		String createdDateString = docInfo.get("date_created");
		String updatedDateString = docInfo.get("date_updated");
		Date createdDate = DateTimeMarshaller.deserializeDateToUTC(createdDateString);
		Date updatedDate = DateTimeMarshaller.deserializeDateToUTC(updatedDateString);  
		sysMeta.setDateUploaded(createdDate);
		//sysMeta.setDateSysMetadataModified(updatedDate);
		// use current datetime 
		sysMeta.setDateSysMetadataModified(Calendar.getInstance().getTime());
		
		// set the revision history
		String docidWithoutRev = accNum.getDocid();
		Identifier obsoletedBy = null;
		Identifier obsoletes = null;
		Vector<Integer> revisions = DBUtil.getRevListFromRevisionTable(docidWithoutRev);
		// ensure this ordering since processing depends on it
		Collections.sort(revisions);
		for (int existingRev: revisions) {
			// use the docid+rev as the guid
			String existingPid = docidWithoutRev + "." + existingRev;
			try {
				existingPid = IdentifierManager.getInstance().getGUID(docidWithoutRev, existingRev);
			} catch (McdbDocNotFoundException mdfe) {
				// we'll be defaulting to the local id
				logMetacat.warn("could not locate guid when processing revision history for localId: " + localId);
			}
			if (existingRev < rev) {
				// it's the old docid, until it's not
				obsoletes = new Identifier();
				obsoletes.setValue(existingPid);
			}
			if (existingRev > rev) {
				// it's the newer docid
				obsoletedBy = new Identifier();
				obsoletedBy.setValue(existingPid);
				// only want the version just after it
				break;
			}
		}
		// set them on our object
		sysMeta.setObsoletedBy(obsoletedBy);
		sysMeta.setObsoletes(obsoletes);
		
		// update the system metadata for the object[s] we are revising
		if (obsoletedBy != null) {
			SystemMetadata obsoletedBySysMeta = null;
			try {
				//obsoletedBySysMeta = HazelcastService.getInstance().getSystemMetadataMap().get(obsoletedBy);
				obsoletedBySysMeta = IdentifierManager.getInstance().getSystemMetadata(obsoletedBy.getValue());
			} catch (McdbDocNotFoundException e) {
				// ignore
			}
			if (obsoletedBySysMeta != null) {
				obsoletedBySysMeta.setObsoletes(identifier);
				obsoletedBySysMeta.setDateSysMetadataModified(Calendar.getInstance().getTime());
				HazelcastService.getInstance().getSystemMetadataMap().put(obsoletedBy, obsoletedBySysMeta);
			}
		}
		if (obsoletes != null) {
			SystemMetadata obsoletesSysMeta = null;
			try {
				//obsoletesSysMeta = HazelcastService.getInstance().getSystemMetadataMap().get(obsoletes);
				obsoletesSysMeta = IdentifierManager.getInstance().getSystemMetadata(obsoletes.getValue());
			} catch (McdbDocNotFoundException e) {
				// ignore
			}
			if (obsoletesSysMeta != null) {
				obsoletesSysMeta.setObsoletedBy(identifier);
				// DO NOT set archived to true -- it will have unintended consequences if the CN sees this.
				//obsoletesSysMeta.setArchived(true);
				obsoletesSysMeta.setDateSysMetadataModified(Calendar.getInstance().getTime());
				HazelcastService.getInstance().getSystemMetadataMap().put(obsoletes, obsoletesSysMeta);
			}
		}
		
		// look up the access control policy we have in metacat
		AccessPolicy accessPolicy = IdentifierManager.getInstance().getAccessPolicy(guid);
		try {
        List<AccessRule> allowList = accessPolicy.getAllowList();
        int listSize = allowList.size();
        sysMeta.setAccessPolicy(accessPolicy);
        
    } catch (NullPointerException npe) {
        logMetacat.info("The allow list is empty, can't include an empty " +
            "access policy in the system metadata for " + guid);
        
    }
		
		// authoritative node
		NodeReference nr = new NodeReference();
		nr.setValue(PropertyService.getProperty("dataone.nodeId"));
		sysMeta.setOriginMemberNode(nr);
		sysMeta.setAuthoritativeMemberNode(nr);
		
		// Set a default replication policy
        ReplicationPolicy rp = getDefaultReplicationPolicy();
        if (rp != null) {
            sysMeta.setReplicationPolicy(rp);
        }
		
		// further parse EML documents to get data object format,
		// describes and describedBy information
		if (fmtid == ObjectFormatCache.getInstance().getFormat(
				"eml://ecoinformatics.org/eml-2.0.0").getFormatId()
				|| fmtid == ObjectFormatCache.getInstance().getFormat(
						"eml://ecoinformatics.org/eml-2.0.1").getFormatId()
				|| fmtid == ObjectFormatCache.getInstance().getFormat(
						"eml://ecoinformatics.org/eml-2.1.0").getFormatId()
				|| fmtid == ObjectFormatCache.getInstance().getFormat(
						"eml://ecoinformatics.org/eml-2.1.1").getFormatId()) {

			try {
				
				// get it again to parse the document
				logMetacat.debug("Re-reading document inputStream");
				inputStream = MetacatHandler.read(localId);
				
				DataoneEMLParser emlParser = DataoneEMLParser.getInstance();
		        EMLDocument emlDocument = emlParser.parseDocument(inputStream);
				
				// iterate through the data objects in the EML doc and add sysmeta
				logMetacat.debug("In createSystemMetadata() the number of data "
								+ "entities is: "
								+ emlDocument.distributionMetadata);

				// for generating the ORE map
	            Map<Identifier, List<Identifier>> idMap = new HashMap<Identifier, List<Identifier>>();
	            List<Identifier> dataIds = new ArrayList<Identifier>();
				
				// iterate through data objects described by the EML
	            if (emlDocument.distributionMetadata != null) {
					for (int j = 0; j < emlDocument.distributionMetadata.size(); j++) {
	
						DistributionMetadata distMetadata = emlDocument.distributionMetadata.elementAt(j);
				        String dataDocUrl = distMetadata.url;
				        String dataDocMimeType = distMetadata.mimeType;
						// default to binary
						if (dataDocMimeType == null) {
							dataDocMimeType = "application/octet-stream";
						}

						// process the data
						boolean remoteData = false;
						String dataDocLocalId = null;
						Identifier dataGuid = new Identifier();

						// handle ecogrid, or downloadable data
						String ecogridPrefix = "ecogrid://knb/";
						if (dataDocUrl.trim().startsWith(ecogridPrefix)) {
							dataDocLocalId = dataDocUrl.substring(dataDocUrl.indexOf(ecogridPrefix) + ecogridPrefix.length());
						} else {
							// should we try downloading the remote data?
							if (downloadData) {
								InputStream dataObject = null;
								try {
									// download the data from the URL
									URL dataURL = new URL(dataDocUrl);
									URLConnection dataConnection = dataURL.openConnection();
									
									// default is to download the data
									dataObject = dataConnection.getInputStream();

									String detectedContentType = dataConnection.getContentType();
									logMetacat.info("Detected content type: " + detectedContentType);

									if (detectedContentType != null) {
										// seems to be HTML from the remote location
										if (detectedContentType.contains("html")) {
											// if we are not expecting it, we skip it
											if (!dataDocMimeType.contains("html")) {
												// set to null so we don't download it
												dataObject = null;
												logMetacat.warn("Skipping remote resource, unexpected HTML content type at: " + dataDocUrl);
											}
										}
										
									} else {
										// if we don't know what it is, should we skip it?
										dataObject = null;
										logMetacat.warn("Skipping remote resource, unknown content type at: " + dataDocUrl);
									}
									
								} catch (Exception e) {
									// error with the download
									logMetacat.warn("Error downloading remote data. " + e.getMessage());
								}
								
								if (dataObject != null) {
									// create the local version of it
									dataDocLocalId = DocumentUtil.generateDocumentId(1);
									IdentifierManager.getInstance().createMapping(dataDocLocalId, dataDocLocalId);
									dataGuid.setValue(dataDocLocalId);
									
									// save it locally
									Session session = new Session();
									session.setSubject(submitter);
									MockHttpServletRequest request = new MockHttpServletRequest(null, null, null);
									MNodeService.getInstance(request).insertDataObject(dataObject, dataGuid, session);
									
									remoteData = true;
								}
							}
							
						}
						
						logMetacat.debug("Data local ID: " + dataDocLocalId);
						logMetacat.debug("Data URL     : " + dataDocUrl);
						logMetacat.debug("Data mime    : " + dataDocMimeType);
						
						// check for valid docid.rev
						String dataDocid = null;
						int dataRev = 0;
						if (dataDocLocalId != null) {
							// look up the guid for the data
							try {
								dataDocid = DocumentUtil.getSmartDocId(dataDocLocalId);
								dataRev = DocumentUtil.getRevisionFromAccessionNumber(dataDocLocalId);
							} catch (Exception e) {
								logMetacat.warn(e.getClass().getName() + " - Problem parsing accession number for: " + dataDocLocalId + ". Message: " + e.getMessage());
								dataDocLocalId = null;
							}
						}
						
						// now we have a local id for the data
						if (dataDocLocalId != null) {
	
							// check if data system metadata exists already
							SystemMetadata dataSysMeta = null;
							String dataGuidString = null;
							try {
								// look for the identifier
								dataGuidString = IdentifierManager.getInstance().getGUID(dataDocid, dataRev);
								// set it
								dataGuid.setValue(dataGuidString);
								// look up the system metadata
								try {
									dataSysMeta = HazelcastService.getInstance().getSystemMetadataMap().get(dataGuid);
								} catch (Exception e) {
									// probably not in the system
									dataSysMeta = null;
								}
								//dataSysMeta = IdentifierManager.getInstance().getSystemMetadata(dataGuidString);
							} catch (McdbDocNotFoundException nf) {
								// we didn't find it
								dataSysMeta = null;
							}
								
							// we'll have to generate it	
							if (dataSysMeta == null) {
								// System metadata for data doesn't exist yet, so create it
								logMetacat.debug("No exisiting SystemMetdata found, creating for: " + dataDocLocalId);
								dataSysMeta = createSystemMetadata(dataDocLocalId, includeORE, false);

								// now look it up again
								dataGuidString = IdentifierManager.getInstance().getGUID(dataDocid, dataRev);

								// set the guid
								dataGuid.setValue(dataGuidString);
								
								// inherit access rules from metadata, if we don't have our own
								if (remoteData) {
									dataSysMeta.setAccessPolicy(sysMeta.getAccessPolicy());
									// TODO: use access rules defined in EML, per data file
								}
	
							}
							
							// set object format for the data file
							logMetacat.debug("Updating system metadata for " + dataGuid.getValue() + " to " + dataDocMimeType);
							ObjectFormatIdentifier fmt = null;
							try {
								fmt = ObjectFormatCache.getInstance().getFormat(dataDocMimeType).getFormatId();
							} catch (NotFound nfe) {
								logMetacat.debug("Couldn't find format identifier for: "
												+ dataDocMimeType
												+ ". Setting it to application/octet-stream.");
								fmt = new ObjectFormatIdentifier();
								fmt.setValue("application/octet-stream");
							}
							dataSysMeta.setFormatId(fmt);

							// update the values
							HazelcastService.getInstance().getSystemMetadataMap().put(dataSysMeta.getIdentifier(), dataSysMeta);
							
							// reindex data file if need it.
							logMetacat.debug("do we need to reindex guid "+dataGuid.getValue()+"~~~~~~~~~~~~~~~~~~~~~~~~~~~~~?"+indexDataFile);
							if(indexDataFile) {
							    reindexDataFile(dataSysMeta.getIdentifier(), dataSysMeta);
							}

							// include as part of the ORE package
							dataIds.add(dataGuid);
	
						} // end if (EML package)
	
					} // end for (data entities)
					
	            } // data entities not null
	            
				// ORE map
				if (includeORE) {
					// can we generate them?
			        if (!dataIds.isEmpty()) {
			        	// it doesn't exist in the system?
			        	if (!oreExistsFor(sysMeta.getIdentifier())) {
			        	
				            // generate the ORE map for this datapackage
				            Identifier resourceMapId = new Identifier();
				            // use the local id, not the guid in case we have DOIs for them already
				            resourceMapId.setValue(RESOURCE_MAP_PREFIX + localId);
				            idMap.put(sysMeta.getIdentifier(), dataIds);
				            ResourceMap rm = ResourceMapFactory.getInstance().createResourceMap(resourceMapId, idMap);
				            String resourceMapXML = ResourceMapFactory.getInstance().serializeResourceMap(rm);
				            // copy most of the same system metadata as the packaging metadata
				            SystemMetadata resourceMapSysMeta = new SystemMetadata();
				            BeanUtils.copyProperties(resourceMapSysMeta, sysMeta);
				            resourceMapSysMeta.setIdentifier(resourceMapId);
				            Checksum oreChecksum = ChecksumUtil.checksum(IOUtils.toInputStream(resourceMapXML, MetaCatServlet.DEFAULT_ENCODING), algorithm);
							resourceMapSysMeta.setChecksum(oreChecksum);
				            ObjectFormatIdentifier formatId = ObjectFormatCache.getInstance().getFormat("http://www.openarchives.org/ore/terms").getFormatId();
							resourceMapSysMeta.setFormatId(formatId);
							resourceMapSysMeta.setSize(BigInteger.valueOf(sizeOfStream(IOUtils.toInputStream(resourceMapXML, MetaCatServlet.DEFAULT_ENCODING))));
							
							// set the revision graph
							resourceMapSysMeta.setObsoletes(null);
							resourceMapSysMeta.setObsoletedBy(null);
							// look up the resource map that this one obsoletes
							if (sysMeta.getObsoletes() != null) {
								// use the localId in case we have a DOI
								String obsoletesLocalId = IdentifierManager.getInstance().getLocalId(sysMeta.getObsoletes().getValue());
								Identifier resourceMapObsoletes = new Identifier();
								resourceMapObsoletes.setValue(RESOURCE_MAP_PREFIX + obsoletesLocalId );
								resourceMapSysMeta.setObsoletes(resourceMapObsoletes);
								SystemMetadata resourceMapObsoletesSystemMetadata = HazelcastService.getInstance().getSystemMetadataMap().get(resourceMapObsoletes);
								if (resourceMapObsoletesSystemMetadata != null) {
									resourceMapObsoletesSystemMetadata.setObsoletedBy(resourceMapId);
									resourceMapObsoletesSystemMetadata.setArchived(true);
									HazelcastService.getInstance().getSystemMetadataMap().put(resourceMapObsoletes, resourceMapObsoletesSystemMetadata);
								}
							}
							// look up the resource map that this one is obsoletedBy
							if (sysMeta.getObsoletedBy() != null) {
								// use the localId in case we have a DOI
								String obsoletedByLocalId = IdentifierManager.getInstance().getLocalId(sysMeta.getObsoletedBy().getValue());
								Identifier resourceMapObsoletedBy = new Identifier();
								resourceMapObsoletedBy.setValue(RESOURCE_MAP_PREFIX + obsoletedByLocalId);
								resourceMapSysMeta.setObsoletedBy(resourceMapObsoletedBy);
								resourceMapSysMeta.setArchived(true);
								SystemMetadata resourceMapObsoletedBySystemMetadata = HazelcastService.getInstance().getSystemMetadataMap().get(resourceMapObsoletedBy);
								if (resourceMapObsoletedBySystemMetadata != null) {
									resourceMapObsoletedBySystemMetadata.setObsoletes(resourceMapId);
									HazelcastService.getInstance().getSystemMetadataMap().put(resourceMapObsoletedBy, resourceMapObsoletedBySystemMetadata);
								}
							}
				            
							// save it locally, if it doesn't already exist
							if (!IdentifierManager.getInstance().identifierExists(resourceMapId.getValue())) {
								Session session = new Session();
								session.setSubject(submitter);
								MockHttpServletRequest request = new MockHttpServletRequest(null, null, null);
								MNodeService.getInstance(request).insertDataObject(IOUtils.toInputStream(resourceMapXML, MetaCatServlet.DEFAULT_ENCODING), resourceMapId, session);
								MNodeService.getInstance(request).insertSystemMetadata(resourceMapSysMeta);
								logMetacat.info("Inserted ORE package: " + resourceMapId.getValue());
							}
			        	}
			        }
				}

			} catch (ParserConfigurationException pce) {
				logMetacat.debug("There was a problem parsing the EML document. "
								+ "The error message was: " + pce.getMessage());

			} catch (SAXException saxe) {
				logMetacat.debug("There was a problem traversing the EML document. "
								+ "The error message was: " + saxe.getMessage());

			} catch (XPathExpressionException xpee) {
				logMetacat.debug("There was a problem searching the EML document. "
								+ "The error message was: " + xpee.getMessage());
			} catch (Exception e) {
				logMetacat.debug("There was a problem creating System Metadata. "
								+ "The error message was: " + e.getMessage());
				e.printStackTrace();
			} // end try()

		} // end if()

		return sysMeta;
	}
	
	/*
	 * Re-index the data file since the access rule was changed during the inserting of the eml document.
	 * (During first time to index the data file in Metacat API, the eml hasn't been inserted)
	 */
	private static void reindexDataFile(Identifier id, SystemMetadata sysmeta) {
	    try {
	        logMetacat.debug("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@ reindex"+id.getValue());
	        if(sysmeta != null) {
	            if(!sysmeta.getArchived()) {
	                //set the archive to true to remove index.
	                sysmeta.setArchived(true);
	                MetacatSolrIndex.getInstance().submit(id, sysmeta, null, true);
	                //re-insert the index
	                sysmeta.setArchived(false);
	                MetacatSolrIndex.getInstance().submit(id, sysmeta, null, true);
	            } else {
	                MetacatSolrIndex.getInstance().submit(id, sysmeta, null, true);
	            }
	        }
	       
        } catch (Exception e) {
            // TODO Auto-generated catch block
            logMetacat.warn("Can't reindex the data object "+id.getValue()+" since "+e.getMessage());
            //e.printStackTrace();
        }
	}

	/**
	 * Checks for potential ORE object existence 
	 * @param identifier
	 * @return
	 */
    public static boolean oreExistsFor(Identifier identifier) {
    	MockHttpServletRequest request = new MockHttpServletRequest(null, null, null);
		List<Identifier> ids = MNodeService.getInstance(request).lookupOreFor(identifier, true);
		return (ids != null && ids.size() > 0);
	}

	/**
     * Generate SystemMetadata for any object in the object store that does
     * not already have it.  SystemMetadata documents themselves, are, of course,
     * exempt.  This is a utility method for migration of existing object 
     * stores to DataONE where SystemMetadata is required for all objects.
     * @param idList
     * @param includeOre
     * @param downloadData
     * @throws PropertyNotFoundException
     * @throws NoSuchAlgorithmException
     * @throws AccessionNumberException
     * @throws SQLException
	 * @throws SAXException 
	 * @throws HandlerException 
	 * @throws MarshallingException 
	 * @throws BaseException 
	 * @throws ParseLSIDException 
	 * @throws InsufficientKarmaException 
	 * @throws ClassNotFoundException 
	 * @throws IOException 
	 * @throws McdbException 
	 * @throws AccessException 
	 * @throws AccessControlException 
     */
    public static void generateSystemMetadata(List<String> idList, boolean includeOre, boolean downloadData) 
    throws PropertyNotFoundException, NoSuchAlgorithmException, AccessionNumberException, SQLException, AccessControlException, AccessException, McdbException, IOException, ClassNotFoundException, InsufficientKarmaException, ParseLSIDException, BaseException, MarshallingException, HandlerException, SAXException 
    {
        
        for (String localId : idList) { 
        	logMetacat.debug("Creating SystemMetadata for localId " + localId);
        	logMetacat.trace("METRICS:\tGENERATE_SYSTEM_METADATA:\tBEGIN:\tLOCALID:\t" + localId);

            SystemMetadata sm = null;

            //generate required system metadata fields from the document
            try {
            	sm = SystemMetadataFactory.createSystemMetadata(localId, includeOre, downloadData);
            } catch (Exception e) {
				logMetacat.error("Could not create/process system metadata for docid: " + localId, e);
				continue;
			}
            
            //insert the systemmetadata object or just update it as needed
        	IdentifierManager.getInstance().insertOrUpdateSystemMetadata(sm);
        	logMetacat.info("Generated or Updated SystemMetadata for " + localId);
            
        	logMetacat.trace("METRICS:\tGENERATE_SYSTEM_METADATA:\tEND:\tLOCALID:\t" + localId);

        }
        logMetacat.info("done generating system metadata for given list");
    }

	/**
	 * Find the size (in bytes) of a stream. Note: This needs to refactored out
	 * of MetacatHandler and into a utility when stream i/o in Metacat is
	 * evaluated.
	 * 
	 * @param is The InputStream of bytes
	 * 
	 * @return size The size in bytes of the input stream as a long
	 * 
	 * @throws IOException
	 */
	public static long sizeOfStream(InputStream is) throws IOException {

		long size = 0;
		byte[] b = new byte[1024];
		int numread = is.read(b, 0, 1024);
		while (numread != -1) {
			size += numread;
			numread = is.read(b, 0, 1024);
		}
		return size;

	}
	
	private static File getFileOnDisk(String docid) throws McdbException, PropertyNotFoundException {
		
		DocumentImpl doc = new DocumentImpl(docid, false);
		String filepath = null;
		String filename = null;

		// deal with data or metadata cases
		if (doc.getRootNodeID() == 0) {
			// this is a data file
			filepath = PropertyService.getProperty("application.datafilepath");
		} else {
			filepath = PropertyService.getProperty("application.documentfilepath");
		}
		// ensure it is a directory path
		if (!(filepath.endsWith("/"))) {
			filepath += "/";
		}
		filename = filepath + docid;
		File documentFile = new File(filename);
		
		return documentFile;
	}

	/**
	 * Create a default ReplicationPolicy by reading properties from metacat's configuration
	 * and using those defaults. If the numReplicas property is not found, malformed, or less
	 * than or equal to zero, no policy needs to be set, so return null.
	 * @return ReplicationPolicy, or null if no replication policy is needed
	 */
    protected static ReplicationPolicy getDefaultReplicationPolicy() {
        ReplicationPolicy rp = null;
        int numReplicas = -1;
        try {
            numReplicas = new Integer(PropertyService.getProperty("dataone.replicationpolicy.default.numreplicas"));
        } catch (NumberFormatException e) {
            // The property is not a valid integer, so set it to 0
            numReplicas = 0;
        } catch (PropertyNotFoundException e) {
            // The property is not found, so set it to 0
            numReplicas = 0;
        }
        
        rp = new ReplicationPolicy();
        if (numReplicas > 0) {
            rp.setReplicationAllowed(true);
            rp.setNumberReplicas(numReplicas);
            try {
                String preferredNodeList = PropertyService.getProperty("dataone.replicationpolicy.default.preferredNodeList");
                if (preferredNodeList != null) {
                    List<NodeReference> pNodes = extractNodeReferences(preferredNodeList);
                    if (pNodes != null && !pNodes.isEmpty()) {
                        rp.setPreferredMemberNodeList(pNodes);
                    }
                }
            } catch (PropertyNotFoundException e) {
                // No preferred list found in properties, so just ignore it; no action needed
            }
            try {
                String blockedNodeList = PropertyService.getProperty("dataone.replicationpolicy.default.blockedNodeList");
                if (blockedNodeList != null) {
                    List<NodeReference> bNodes = extractNodeReferences(blockedNodeList);
                    if (bNodes != null && !bNodes.isEmpty()) {
                        rp.setBlockedMemberNodeList(bNodes);
                    }
                }
            } catch (PropertyNotFoundException e) {
                // No blocked list found in properties, so just ignore it; no action needed
            }
        } else {
            rp.setReplicationAllowed(false);
            rp.setNumberReplicas(0);
        }
        return rp;
    }

    /**
     * Extract a List of NodeReferences from a String listing the node identifiers where
     * each identifier is separated by whitespace, comma, or semicolon characters.
     * @param nodeString the string containing the list of nodes
     * @return the List of NodeReference objects parsed from the input string
     */
    private static List<NodeReference> extractNodeReferences(String nodeString) {
        List<NodeReference> nodeList = new ArrayList<NodeReference>();
        String[] result = nodeString.split("[,;\\s]");
        for (String r : result) {
        	if (r != null && r.length() > 0) {
	            NodeReference noderef = new NodeReference();
	            noderef.setValue(r);
	            nodeList.add(noderef);
	        }
        }
        return nodeList;
    }
}
