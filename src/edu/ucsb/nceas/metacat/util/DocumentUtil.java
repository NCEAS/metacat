package edu.ucsb.nceas.metacat.util;

import java.sql.SQLException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.SimpleTimeZone;
import java.util.TimeZone;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.ucsb.nceas.dbadapter.AbstractDatabase;
import edu.ucsb.nceas.metacat.DBSAXHandler;
import edu.ucsb.nceas.metacat.DBUtil;
import edu.ucsb.nceas.metacat.McdbDocNotFoundException;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;

/**
 * A suite of utility classes for the metadata catalog server
 */
public class DocumentUtil {

    private static int documentIdCounter = 0;

    public static AbstractDatabase dbAdapter;
    public static String startTag = "<systemMetadata>";
    public static String endTag = "</systemMetadata>";

    private static Log logMetacat = LogFactory.getLog(DocumentUtil.class);
    private static char separator = '.';
    private static String prefix = "autogen";

    static {
        try {
            separator = PropertyService.getProperty("document.accNumSeparator").charAt(0);
        } catch (PropertyNotFoundException pnfe) {
            logMetacat.error("DocumentUtil() - Could not retrieve accession number separator. "
                    + "Separator set to '.' : " + pnfe.getMessage());
        }
        try {
            prefix = PropertyService.getProperty("document.accNumPrefix");
        } catch (PropertyNotFoundException pnfe) {
            logMetacat.error("DocumentUtil() - Could not retrieve accession number prefix. "
                    + "Prefix set to " + prefix + ": " + pnfe.getMessage());
        }
    }


    /**
     * Eocgorid identifier will look like: ecogrid://knb/tao.1.1
     * The AccessionNumber tao.1.1 will be returned. If the given doesn't
     * contains ecogrid, null will be returned.
     * @param identifier String
     * @return String
     */
    public static String getAccessionNumberFromEcogridIdentifier(String identifier)
    {
      String accessionNumber = null;
      if (identifier != null && identifier.startsWith(DBSAXHandler.ECOGRID))
      {
        // find the last "/" in identifier
        int indexOfLastSlash = identifier.lastIndexOf("/");
        int start = indexOfLastSlash+1;
        int end   = identifier.length();
        accessionNumber = identifier.substring(start, end);
      }
      logMetacat.debug("DocumentUtil.getAccessionNumberFromEcogridIdentifier - The accession number"
                      + " from url is " + accessionNumber);
      return accessionNumber;
    }


    /**
     * Utility method to get docid from a given string
     *
     * @param string, the given string should be these two format: 1) str1.str2
     *            in this case docid= str1.str2 2) str1.str2.str3, in this case
     *            docid =str1.str2
     * @param the sperator char
     */
    public static String getDocIdFromString(String str)
    {
        String docId = null;
        if (str == null) {
            logMetacat.debug(
                    "DocumentUtil.getDocIdFromString - The given str is null and null will be "
                     + "returned" + " in getDocIdfromString");
            return docId;
        } //make sure docid is not null
        int dotNumber = 0;//count how many dots in given string
        int indexOfLastDot = 0;

        for (int i = 0; i < str.length(); i++) {
            if (str.charAt(i) == separator) {
                dotNumber++;//count how many dots
                indexOfLastDot = i;//keep the last dot postion
            }
        }//for

        //The string formatt is wrong, because it has more than two or less
        // than
        //one seperator
        if (dotNumber > 2 || dotNumber < 1) {
            docId = null;
        } else if (dotNumber == 2) //the case for str1.str2.str3
        {
            docId = str.substring(0, indexOfLastDot);
        } else if (dotNumber == 1) //the case for str1.str2
        {
            docId = str;
        }

        return docId;
    }//getDocIdFromString

    /**
     * Utility method to get version number from a given string
     *
     * @param string, the given string should be these two format: 1)
     *            str1.str2(no version) version =-1; 2) str1.str2.str3, in this
     *            case version = str3; 3) other, vresion =-2
     */
    public static int getVersionFromString(String str)
            throws NumberFormatException
    {
        int version = -1;
        String versionString = null;
        int dotNumber = 0;//count how many dots in given string
        int indexOfLastDot = 0;

        for (int i = 0; i < str.length(); i++) {
            if (str.charAt(i) == separator) {
                dotNumber++;//count how many dots
                indexOfLastDot = i;//keep the last dot postion
            }
        }//for

        //The string formatt is wrong, because it has more than two or less
        // than
        //one seperator
        if (dotNumber > 2 || dotNumber < 1) {
            version = -2;
        } else if (dotNumber == 2 && (indexOfLastDot != (str.length() - 1)))
        //the case for str1.str2.str3
        {
            versionString = str.substring((indexOfLastDot + 1), str.length());
            version = Integer.parseInt(versionString);
        } else if (dotNumber == 1) //the case for str1.str2
        {
            version = -1;
        }

        return version;
    }//getVersionFromString

    /**
     * Utility method to get version string from a given string
     *
     * @param string, the given string should be these two format: 1)
     *            str1.str2(no version) version=null; 2) str1.str2.str3, in
     *            this case version = str3; 3) other, vresion =null;
     */
    public static String getRevisionStringFromString(String str)
            throws NumberFormatException
    {
        // String to store the version
        String versionString = null;
        int dotNumber = 0;//count how many dots in given string
        int indexOfLastDot = 0;

        for (int i = 0; i < str.length(); i++) {
            if (str.charAt(i) == separator) {
                dotNumber++;//count how many dots
                indexOfLastDot = i;//keep the last dot postion
            }
        }//for

        //The string formatt is wrong, because it has more than two or less
        // than
        //one seperator
        if (dotNumber > 2 || dotNumber < 1) {
            versionString = null;
        } else if (dotNumber == 2 && (indexOfLastDot != (str.length() - 1))) {
            //the case for str1.str2.str3
            // indexOfLastDot != (str.length() -1) means get rid of str1.str2.
            versionString = str.substring((indexOfLastDot + 1), str.length());
        } else if (dotNumber == 1) //the case for str1.str2 or str1.str2.
        {
            versionString = null;
        }

        return versionString;
    }//getVersionFromString
    
    /**
     * If the given docid only have one seperter, we need
     * append rev for it. The rev come from xml_documents
     */
    public static String appendRev(String docid) 
      throws PropertyNotFoundException, SQLException, McdbDocNotFoundException {
        String newAccNum = null;
        String separator = PropertyService.getProperty("document.accNumSeparator");
        int firstIndex = docid.indexOf(separator);
        int lastIndex = docid.lastIndexOf(separator);
        if (firstIndex == lastIndex) {
            
            //only one seperater
            int rev = DBUtil.getLatestRevisionInDocumentTable(docid);
            if (rev == -1) {
                throw new McdbDocNotFoundException("the requested docid '"
                        + docid+ "' does not exist");
            } else {
                newAccNum = docid+ separator+ rev;
            }
        } else {
            // in other suituation we don't change the docid
            newAccNum = docid;
        }
        //logMetacat.debug("The docid will be read is "+newAccNum);
        return newAccNum;
    }

    /**
     * This method will get docid from an AccessionNumber. There is no
     * assumption the accessnumber will be str1.str2.str3. It can be more. So
     * we think the docid will be get rid of last part
     */
    public static String getDocIdFromAccessionNumber(String accessionNumber)
    {
        String docid = null;
        if (accessionNumber == null) { return docid; }
        int indexOfLastSeperator = accessionNumber.lastIndexOf(separator);
        if (indexOfLastSeperator > 0) {
            docid = accessionNumber.substring(0, indexOfLastSeperator);
        }
        logMetacat.debug("DocumentUtil.getDocIdFromAccessionNumber - after parsing accession "
                            + "number, docid is " + docid);
        return docid;
    }


    /**
     * This method will call both getDocIdFromString and
     * getDocIdFromAccessionNumber. So first, if the string looks str1.str2,
     * the docid will be str1.str2. If the string is str1.str2.str3, the docid
     * will be str1.str2. If the string is str1.str2.str3.str4 or more, the
     * docid will be str1.str2.str3. If the string look like str1, null will be
     * returned
     *
     */
    public static String getSmartDocId(String str)
    {
        String docid = null;
        //call geDocIdFromString first.
        docid = getDocIdFromString(str);
        // If docid is null, try to call getDocIdFromAccessionNumber
        // it will handle the seperator more than2
        if (docid == null) {
            docid = getDocIdFromAccessionNumber(str);
        }
        logMetacat.debug("DocumentUtil.getSmartDocId - The docid get from smart docid getor is "
                + docid);
        return docid;
    }

    /**
     * This method will get revision from an AccessionNumber. There is no
     * assumption the accessnumber will be str1.str2.str3. It can be more. So
     * we think the docid will be get rid of last part
     */
    public static int getRevisionFromAccessionNumber(String accessionNumber)
            throws NumberFormatException
    {
        String rev = null;
        int revNumber = -1;
        if (accessionNumber == null) { return revNumber; }
        int indexOfLastSeperator = accessionNumber.lastIndexOf(separator);
        rev = accessionNumber.substring(indexOfLastSeperator + 1,
                accessionNumber.length());
        revNumber = Integer.parseInt(rev);
        logMetacat.debug("DocumentUtil.getRevisionFromAccessionNumber - after parsing "
                            + "accessionnumber, rev is " + revNumber);
        return revNumber;
    }

    /**
     * Create a unique docid for use in inserts and updates using the default
     * prefix from the document.accNumPrefix property. Does not include the 
     * 'revision' part of the id if revision is '0', otherwise sets the 
     * revision number to 'revision'.
     * 
     * @param idPrefix the prefix to be used to construct the scope portion of the docid
     * @param revision the integer revision to use for this docid
     * @return a String docid based on the current date and time
     */
    public static String generateDocumentId(int revision) {
        return generateDocumentId(prefix, revision);
    }
    
    /**
     * Create a unique docid for use in inserts and updates using the prefix
     * that is provided. Does not include the 'revision' part of the id if 
     * revision is '0', otherwise sets the revision number to 'revision'.
     * 
     * @param idPrefix the prefix to be used to construct the scope portion of the docid
     * @param revision the integer revision to use for this docid
     * @return a String docid based on the current date and time
     */
    public static String generateDocumentId(String idPrefix, int revision)
    {
        StringBuffer docid = new StringBuffer(idPrefix);
        docid.append(".");

        // Create a calendar to get the date formatted properly
        String[] ids = TimeZone.getAvailableIDs(-8 * 60 * 60 * 1000);
        SimpleTimeZone pdt = new SimpleTimeZone(-8 * 60 * 60 * 1000, ids[0]);
        pdt.setStartRule(Calendar.APRIL, 1, Calendar.SUNDAY, 2 * 60 * 60 * 1000);
        pdt.setEndRule(Calendar.OCTOBER, -1, Calendar.SUNDAY, 2 * 60 * 60 * 1000);
        Calendar calendar = new GregorianCalendar(pdt);
        Date trialTime = new Date();
        calendar.setTime(trialTime);
        // using yyyymmddhhmmssmmm by convention (zero padding to preserve places)
        // will help with looking at logs and especially database tables.
        // for each millisecond we can support up to 99 before resetting to 0
        // NOTE: if you make it larger, docid is too big for a Long 
        if (documentIdCounter > 100) {
            documentIdCounter = 0;
        }
        docid.append(String.format("%04d%02d%02d%02d%02d%02d%03d%02d",
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH) + 1,  // adjust 0-11 range to 1-12
        calendar.get(Calendar.DAY_OF_MONTH),
        calendar.get(Calendar.HOUR_OF_DAY),
        calendar.get(Calendar.MINUTE),
        calendar.get(Calendar.SECOND),
        calendar.get(Calendar.MILLISECOND),
        documentIdCounter++)
        );
        if (revision > 0) {
            docid.append(".").append(revision);
        }
        return docid.toString();
    }

    /**
     * Get the content between the system metadata start and end tag
     */
    public static String getSystemMetadataContent(String docInfoStr) {
        // get the system metadata portion
        String systemMetadataXML = null;
        if (docInfoStr.indexOf(startTag) > -1) {
          systemMetadataXML = docInfoStr.substring(docInfoStr.indexOf(startTag)
                  + startTag.length(), docInfoStr.lastIndexOf(endTag));
        }
        return systemMetadataXML;
    }

    /**
     * Get the string WITHOUT the content between the system metadata start and end tag
     */
    public static String getContentWithoutSystemMetadata(String docInfoStr) {
        // strip out the system metadata portion
        if (docInfoStr.contains(startTag)) {
          docInfoStr = docInfoStr.substring(0, docInfoStr.indexOf(startTag))
                  + docInfoStr.substring(docInfoStr.indexOf(endTag) + endTag.length());
        }
        return docInfoStr;
    }
}
