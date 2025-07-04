package edu.ucsb.nceas.metacat.dataone;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dataone.client.v2.formats.ObjectFormatCache;
import org.dataone.exceptions.MarshallingException;
import org.dataone.service.exceptions.BaseException;
import org.dataone.service.exceptions.NotFound;
import org.dataone.service.types.v1.AccessPolicy;
import org.dataone.service.types.v1.AccessRule;
import org.dataone.service.types.v1.Checksum;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.NodeReference;
import org.dataone.service.types.v1.ObjectFormatIdentifier;
import org.dataone.service.types.v1.ReplicationPolicy;
import org.dataone.service.types.v1.Subject;
import org.dataone.service.types.v2.SystemMetadata;
import org.dataone.service.types.v1.util.ChecksumUtil;
import org.dataone.service.util.DateTimeMarshaller;
import org.dataone.service.util.TypeMarshaller;
import org.xml.sax.ContentHandler;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

import java.util.Calendar;

import edu.ucsb.nceas.metacat.AccessionNumber;
import edu.ucsb.nceas.metacat.AccessionNumberException;
import edu.ucsb.nceas.metacat.DBUtil;
import edu.ucsb.nceas.metacat.DocumentImpl;
import edu.ucsb.nceas.metacat.IdentifierManager;
import edu.ucsb.nceas.metacat.McdbDocNotFoundException;
import edu.ucsb.nceas.metacat.McdbException;
import edu.ucsb.nceas.metacat.MetacatHandler;
import edu.ucsb.nceas.metacat.accesscontrol.AccessControlException;
import edu.ucsb.nceas.metacat.accesscontrol.AccessControlForSingleFile;
import edu.ucsb.nceas.metacat.client.InsufficientKarmaException;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.metacat.shared.AccessException;
import edu.ucsb.nceas.metacat.shared.HandlerException;
import edu.ucsb.nceas.metacat.systemmetadata.SystemMetadataManager;
import edu.ucsb.nceas.metacat.util.DocumentUtil;
import edu.ucsb.nceas.utilities.ParseLSIDException;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;
import edu.ucsb.nceas.utilities.access.DocInfoHandler;

public class SystemMetadataFactory {

    public static final String RESOURCE_MAP_PREFIX = "resourceMap_";
    private static Log logMetacat = LogFactory.getLog(SystemMetadataFactory.class);
    /**
     * use this flag if you want to update any existing system metadata values with generated
     * content
     */
    private static boolean updateExisting = true;
    private static String legacyDataDir = null;
    private static String legacyDocDir = null;
    private static String accNumSeparator = ".";
    static {
        try {
            legacyDataDir = PropertyService.getProperty("application.datafilepath");
            if (!legacyDataDir.endsWith("/")) {
                legacyDataDir = legacyDataDir + "/";
            }
        } catch (PropertyNotFoundException e) {
            logMetacat.error("Metacat can't find the property value of application.datafilepath");
        }
        try {
            legacyDocDir = PropertyService.getProperty("application.documentfilepath");
            if (!legacyDocDir.endsWith("/")) {
                legacyDocDir = legacyDocDir + "/";
            }
        } catch (PropertyNotFoundException e) {
            logMetacat.error("Metacat can't find the property value of application.documentfilepath");
        }
        try {
            accNumSeparator = PropertyService.getProperty("document.accNumSeparator");
            logMetacat.debug("Metacat will use " + accNumSeparator + "as the docid separator.");
        } catch (PropertyNotFoundException e) {
            accNumSeparator = ".";
            logMetacat.debug("Metacat can't find the property value of document.accNumSeparator."
                                + "It will use the default docid separator: . (dot).");
        }
    }


    /**
     * Creates a system metadata object for insertion into metacat
     * @param localId
     *            The local document identifier, which is doicd + "." + rev.
     * @return sysMeta The system metadata object created
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
    public static SystemMetadata createSystemMetadata(String localId)
        throws McdbException, McdbDocNotFoundException, SQLException, IOException,
        AccessionNumberException, ClassNotFoundException, InsufficientKarmaException,
        ParseLSIDException, PropertyNotFoundException, BaseException, NoSuchAlgorithmException,
        MarshallingException, AccessControlException, HandlerException, SAXException,
        AccessException {
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
            logMetacat.debug(
                "No guid found in the identifier table.  Creating mapping for " + localId);
            IdentifierManager.getInstance().createMapping(localId, localId);
            guid = IdentifierManager.getInstance().getGUID(accNum.getDocid(), rev);
        }

        // look up existing system metadata if it exists
        Identifier identifier = new Identifier();
        identifier.setValue(guid);
        try {
            logMetacat.debug("Getting system metadata");
            sysMeta = SystemMetadataManager.getInstance().get(identifier);
            if (!updateExisting) {
                return sysMeta;
            }
        } catch (Exception e) {
            logMetacat.debug("No system metadata found: " + e.getMessage(), e);
        }

        if (sysMeta == null) {
            // create system metadata
            sysMeta = new SystemMetadata();
            sysMeta.setIdentifier(identifier);
            sysMeta.setSerialVersion(BigInteger.valueOf(1));
            sysMeta.setArchived(false);
        }

        // get additional docinfo
        Hashtable<String, String> docInfo = getDocumentInfoMap(localId);
        // set the default object format
        String docType = docInfo.get("doctype");
        logMetacat.debug("The docType is " + docType);
        ObjectFormatIdentifier fmtid = null;
        // set the object format, fall back to defaults
        if (docType.trim().equals("BIN")) {
            // we don't know much about this file (yet)
            fmtid = ObjectFormatCache.getInstance().getFormat("application/octet-stream").getFormatId();
        } else if (docType.trim().equals("metadata")) {
            // special ESRI FGDC format
            fmtid = ObjectFormatCache.getInstance().getFormat("FGDC-STD-001-1998").getFormatId();
        } else {
            try {
                // do we know the given format?
                fmtid = ObjectFormatCache.getInstance().getFormat(docType).getFormatId();
            } catch (NotFound nfe) {
                // format is not registered, use default
                fmtid = ObjectFormatCache.getInstance().getFormat("text/plain").getFormatId();
            }
        }
        sysMeta.setFormatId(fmtid);
        logMetacat.debug(
            "The ObjectFormat for " + identifier.getValue() + " is " + fmtid.getValue());


        // for retrieving the actual object
        try (InputStream inputStream = MetacatHandler.read(identifier)) {
            // create the checksum
            String algorithm = PropertyService.getProperty("dataone.checksumAlgorithm.default");
            Checksum checksum = ChecksumUtil.checksum(inputStream, algorithm);
            logMetacat.debug(
                "The checksum for " + identifier.getValue() + " is " + checksum.getValue());
            sysMeta.setChecksum(checksum);
        } catch (McdbDocNotFoundException e) {
            // try to read it from the legacy store
            Identifier useLocalIdAsPid = new Identifier();
            useLocalIdAsPid.setValue(localId);
            try (InputStream inputStream = readInputStreamFromLegacyStore(useLocalIdAsPid)) {
                // create the checksum
                String algorithm = PropertyService.getProperty("dataone.checksumAlgorithm.default");
                Checksum checksum = ChecksumUtil.checksum(inputStream, algorithm);
                logMetacat.debug("The checksum for " + localId + " is " + checksum.getValue());
                sysMeta.setChecksum(checksum);
            }
        }

        // set the size
        long fileSize = 0;
        try (InputStream inputStream = MetacatHandler.read(identifier)) {
            fileSize = length(inputStream);
        } catch (McdbDocNotFoundException e) {
            // Try to read from the legacy store
            Identifier useLocalIdAsPid = new Identifier();
            useLocalIdAsPid.setValue(localId);
            try (InputStream inputStream = readInputStreamFromLegacyStore(useLocalIdAsPid)) {
                fileSize = length(inputStream);
            }
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
        Date createdDate = DateTimeMarshaller.deserializeDateToUTC(createdDateString);
        sysMeta.setDateUploaded(createdDate);
        // use current datetime
        sysMeta.setDateSysMetadataModified(Calendar.getInstance().getTime());

        // set the revision history
        String docidWithoutRev = accNum.getDocid();
        Identifier obsoletedBy = null;
        Identifier obsoletes = null;
        Vector<Integer> revisions = DBUtil.getRevListFromRevisionTable(docidWithoutRev);
        int latestVersion = DBUtil.getLatestRevisionInDocumentTable(docidWithoutRev);
        if (latestVersion != -1) {
            revisions.add(latestVersion);
        }
        logMetacat.debug("The version history of " + docidWithoutRev + " is " + revisions);
        String obsoletedByStr = null;
        // The obsoletedBy docid always is one bigger than the current version.
        int obsoletedByRev = rev + 1;
        try {
            // We first look at the identifier table to figure out the pid
            obsoletedByStr = IdentifierManager.getInstance().getGUID(docidWithoutRev, obsoletedByRev);
            logMetacat.debug("Use pid from the identifier table as the obsoletedBy pid "
                                 + obsoletedByStr);
        } catch (McdbDocNotFoundException mdfe) {
            // Metacat can't find the pid on the identifier table for the given doicd.
            // It will use docid + rev as the pid
            if (revisions.contains(obsoletedByRev) && !docidWithoutRev.startsWith("autogen.")) {
                obsoletedByStr = docidWithoutRev + accNumSeparator + obsoletedByRev;
                logMetacat.debug("Use docid + rev as the obsoletedBy pid " + obsoletedByStr);
            }
        }
        if (obsoletedByStr != null) {
            obsoletedBy = new Identifier();
            obsoletedBy.setValue(obsoletedByStr);
        }
        String obsoletesStr = null;
        // Obsoletes docid always is one lesser than the current version.
        int obsoletesRev = rev - 1;
        try {
             // We first look at the identifier table to figure out the pid
            obsoletesStr = IdentifierManager.getInstance().getGUID(docidWithoutRev, obsoletesRev);
            logMetacat.debug("Use pid from the identifier table as the obsoletes pid "
                                 + obsoletesStr);
        } catch (McdbDocNotFoundException mdfe) {
            // Metacat can't find the pid on the identifier table for the given docid.
            // It will use docid + rev as the pid
            if (revisions.contains(obsoletesRev) && !docidWithoutRev.startsWith("autogen.")) {
                obsoletesStr = docidWithoutRev + accNumSeparator + (obsoletesRev);
                logMetacat.debug("Use docid + rev as the obsoletes pid " + obsoletesStr);
            }
        }
        if (obsoletesStr != null) {
            obsoletes = new Identifier();
            obsoletes.setValue(obsoletesStr);
        }
        // set them on our object
        sysMeta.setObsoletedBy(obsoletedBy);
        sysMeta.setObsoletes(obsoletes);

        // update the system metadata for the object[s] we are revising
        if (obsoletedBy != null) {
            try {
                SystemMetadataManager.lock(obsoletedBy);
                SystemMetadata obsoletedBySysMeta = null;
                try {
                    obsoletedBySysMeta =
                        IdentifierManager.getInstance().getSystemMetadata(obsoletedBy.getValue());
                } catch (McdbDocNotFoundException e) {
                    // ignore
                }
                if (obsoletedBySysMeta != null) {
                    obsoletedBySysMeta.setObsoletes(identifier);
                    SystemMetadataManager.getInstance().store(obsoletedBySysMeta);
                }
            } finally {
                SystemMetadataManager.unLock(obsoletedBy);
            }
        }
        if (obsoletes != null) {
            try {
                SystemMetadataManager.lock(obsoletes);
                SystemMetadata obsoletesSysMeta = null;
                try {
                    obsoletesSysMeta =
                        IdentifierManager.getInstance().getSystemMetadata(obsoletes.getValue());
                } catch (McdbDocNotFoundException e) {
                    // ignore
                }
                if (obsoletesSysMeta != null) {
                    obsoletesSysMeta.setObsoletedBy(identifier);
                    // DO NOT set archived to true -- it will have unintended consequences if the CN
                    // sees this.
                    SystemMetadataManager.getInstance().store(obsoletesSysMeta);
                }
            } finally {
                SystemMetadataManager.unLock(obsoletes);
            }
        }

        // look up the access control policy we have in metacat
        AccessPolicy accessPolicy = IdentifierManager.getInstance().getAccessPolicy(guid);
        try {
            List<AccessRule> allowList = accessPolicy.getAllowList();
            int listSize = allowList.size();
            sysMeta.setAccessPolicy(accessPolicy);

        } catch (NullPointerException npe) {
            logMetacat.info("The allow list is empty, can't include an empty "
                                + "access policy in the system metadata for " + guid);

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


        return sysMeta;
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
            numReplicas = Integer.parseInt(PropertyService
                                .getProperty("dataone.replicationpolicy.default.numreplicas"));
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
                String preferredNodeList = PropertyService.getProperty(
                    "dataone.replicationpolicy.default.preferredNodeList");
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
                String blockedNodeList = PropertyService.getProperty(
                    "dataone.replicationpolicy.default.blockedNodeList");
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

    /**
     * Get the document information in the format of a map.
     * @param docid  the id of the document
     * @return a map containing the document information
     * @throws HandlerException
     * @throws AccessControlException
     * @throws MarshallingException
     * @throws IOException
     * @throws McdbException
     * @throws SAXException
     */
    public static Hashtable<String, String> getDocumentInfoMap(String docid)
            throws HandlerException, AccessControlException, MarshallingException,
            IOException, McdbException, SAXException {

        // Try get docid info from remote server
        DocInfoHandler dih = new DocInfoHandler();
        XMLReader docinfoParser = initParser(dih);

        String docInfoStr = getDocumentInfo(docid);

        // strip out the system metadata portion
        String systemMetadataXML = DocumentUtil.getSystemMetadataContent(docInfoStr);
        docInfoStr = DocumentUtil.getContentWithoutSystemMetadata(docInfoStr);

        docinfoParser.parse(new InputSource(new StringReader(docInfoStr)));
        Hashtable<String, String> docinfoHash = dih.getDocInfo();

        return docinfoHash;
    }

    /**
     * Gets a docInfo XML snippet for the replication API
     * @param docid
     * @return the doc information
     * @throws AccessControlException
     * @throws JiBXException
     * @throws IOException
     * @throws McdbException
     */
    private static String getDocumentInfo(String docid) throws AccessControlException,
                                                MarshallingException, IOException, McdbException {
        StringBuffer sb = new StringBuffer();

        DocumentImpl doc = new DocumentImpl(docid);
        sb.append("<documentinfo><docid>").append(docid);
        sb.append("</docid>");

        try {
            // serialize the System Metadata as XML for docinfo
            String guid = IdentifierManager.getInstance().getGUID(doc.getDocID(), doc.getRev());
            SystemMetadata systemMetadata = IdentifierManager.getInstance().getSystemMetadata(guid);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            TypeMarshaller.marshalTypeToOutputStream(systemMetadata, baos);
            String systemMetadataXML = baos.toString("UTF-8");
            sb.append("<systemMetadata>");
            sb.append(systemMetadataXML);
            sb.append("</systemMetadata>");
        } catch(McdbDocNotFoundException e) {
          logMetacat.warn("No SystemMetadata found for: " + docid);
        }

        Calendar created = Calendar.getInstance();
        created.setTime(doc.getCreateDate());
        Calendar updated = Calendar.getInstance();
        updated.setTime(doc.getUpdateDate());

        sb.append("<docname><![CDATA[").append(doc.getDocname());
        sb.append("]]></docname><doctype>").append(doc.getDoctype());
        sb.append("</doctype>");
        sb.append("<user_owner>").append(doc.getUserowner());
        sb.append("</user_owner><user_updated>").append(doc.getUserupdated());
        sb.append("</user_updated>");
        sb.append("<date_created>");
        sb.append(DateTimeMarshaller.serializeDateToUTC(doc.getCreateDate()));
        sb.append("</date_created>");
        sb.append("<date_updated>");
        sb.append(DateTimeMarshaller.serializeDateToUTC(doc.getUpdateDate()));
        sb.append("</date_updated>");
        sb.append("<rev>").append(doc.getRev());
        sb.append("</rev>");

        sb.append("<accessControl>");

        AccessControlForSingleFile acfsf = new AccessControlForSingleFile(docid);
        sb.append(acfsf.getAccessString());

        sb.append("</accessControl>");

        sb.append("</documentinfo>");

        return sb.toString();
    }

    /**
     * Method to initialize the message parser
     * @param dh
     * @return a sax parser
     * @throws HandlerException
     */
    private static XMLReader initParser(DefaultHandler dh) throws HandlerException {
        XMLReader parser = null;
        try {
            ContentHandler chandler = dh;
            // Get an instance of the parser
            String parserName = PropertyService.getProperty("xml.saxparser");
            parser = XMLReaderFactory.createXMLReader(parserName);
            // Turn off validation
            parser.setFeature("http://xml.org/sax/features/validation", false);
            parser.setContentHandler((ContentHandler) chandler);
            parser.setErrorHandler((ErrorHandler) chandler);
        } catch (SAXException se) {
            throw new HandlerException(
                "ReplicationHandler.initParser - Sax error when " + " initializing parser: "
                    + se.getMessage());
        } catch (PropertyNotFoundException pnfe) {
            throw new HandlerException(
                "ReplicationHandler.initParser - Property error when " + " getting parser name: "
                    + pnfe.getMessage());
        }

        return parser;
    }

    protected static long length(InputStream inputStream) throws IOException {
        byte[] buffer = new byte[8192];
        int chunkBytesRead = 0;
        long length = 0;
        while((chunkBytesRead = inputStream.read(buffer)) != -1) {
            length += chunkBytesRead;
        }
        return length;
    }

    /**
     * This method is used for the HashStoreUpgrader class to read the object from the Metacat
     * legacy store since the Hashtore hasn't been successfully converted.
     * @param pid  the identifier the object. Note: We assume the pid's value equals the docid
     * @return the InputStream presentation of the object
     */
    protected static InputStream readInputStreamFromLegacyStore(Identifier pid)
        throws FileNotFoundException {
        String localId = pid.getValue();
       return new FileInputStream(getFileFromLegacyStore(localId));
    }

    /**
     * Get a file object for the given localId from the legacyStore. It will try both the document
     * and data directories.
     * @param localId  the local id (doc id) will be look.
     * @return the file which have the content of the local id
     * @throws FileNotFoundException
     */
    public static File getFileFromLegacyStore(String localId)
        throws FileNotFoundException {
        if (legacyDocDir != null) {
            File file = new File(legacyDocDir + localId);
            if (file.exists()) {
                return file;
            } else if (legacyDataDir != null) {
                file = new File(legacyDataDir + localId);
                if (file.exists()) {
                    return file;
                } else {
                    throw new FileNotFoundException(
                        "Can't find the object " + file.getAbsolutePath() + " in the"
                            + " Metacat legacy store.");
                }
            } else {
                throw new FileNotFoundException("Can't find the object " + localId + " in the"
                                                    + " Metacat legacy store.");
            }
        } else if (legacyDataDir != null) {
            File file = new File(legacyDataDir + localId);
            if (file.exists()) {
                return file;
            } else {
                throw new FileNotFoundException("Can't find the object " + file.getAbsolutePath()
                                                    + " in the Metacat legacy store.");
            }
        } else {
            throw new FileNotFoundException("Can't find the object " + localId + " in the"
                                                + " Metacat legacy store since the its data and"
                                                + " document root directories are null.");
        }
    }
}
