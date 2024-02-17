package edu.ucsb.nceas.metacat.admin.upgrade.solr;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Vector;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.OrFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dataone.service.types.v1.Checksum;
import org.dataone.service.types.v1.util.ChecksumUtil;

import edu.ucsb.nceas.metacat.admin.AdminException;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.metacat.service.ServiceService;
import edu.ucsb.nceas.metacat.shared.ServiceException;
import edu.ucsb.nceas.utilities.FileUtil;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;
import edu.ucsb.nceas.utilities.StringUtil;

/**
 * This class represents an object to upgrade the solrconfig.xml on the Metacat upgrade process.
 * @author tao
 *
 */
public class SolrConfigUpgrader {
    private static Log logMetacat = LogFactory.getLog(SolrSchemaUpgrader.class);
    private static final String CONFIGFILERELATIVEPATH = "/conf/solrconfig.xml";
    private static final String MD5 = "MD5";
    private Vector<String> releasedConfigHashList = new Vector<String>();
    private String solrHomePath = null;
    private String metacatIndexSolrHome = null;
    private String currentHash = null;
    
    /**
     * Constructor
     * @throws PropertyNotFoundException
     * @throws ServiceException
     */
    public SolrConfigUpgrader() throws PropertyNotFoundException, ServiceException {
        String hashString = null;
        try {
            hashString = PropertyService.getProperty("index.configFile.released.hash");
            solrHomePath = PropertyService.getProperty("solr.homeDir");
            String indexContext = PropertyService.getProperty("index.context");
            currentHash = PropertyService.getProperty("index.configFile.current.hash");
            logMetacat.info("SolrConfigUpgrader.constructor - the current hash is " + currentHash);
            String metacatWebInf = ServiceService.getRealConfigDir();
            metacatIndexSolrHome = metacatWebInf + "/../../" + indexContext
                                                                    + "/WEB-INF/classes/solr-home";
        } catch (PropertyNotFoundException pnfe) {
            throw new PropertyNotFoundException("SolrConfigUpdator.Constructor - "
                            + "could not get a metacat property in the metacat.properties file - "
                            + pnfe.getMessage());
        }
        releasedConfigHashList = StringUtil.toVector(hashString, ';');
        logMetacat.info("the released hash is "+releasedConfigHashList);
    }
    
    /**
     * Upgrade the solrconfig.xml on the solr home. If the existing solrconfigure.xml's hash codes
     * doesn't match released ones, we will copy it as solrconfigure.xml and give user an warning.
     * @throws AdminException
     * @throws IOException
     * @throws NoSuchAlgorithmException
     * @throws SolrSchemaModificationException
     */
    public void upgrade() throws AdminException, IOException, NoSuchAlgorithmException,
                                                                SolrSchemaModificationException {
        boolean solrHomeExists = new File(solrHomePath).exists();
        if (!solrHomeExists) {
            //create the solr home and copy the files to it if it didn't exist
            try {
                // only attempt to copy if we have the source directory to copy from
                File sourceDir = new File(metacatIndexSolrHome);
                if (sourceDir.exists()) {
                    FileUtil.createDirectory(solrHomePath);
                    OrFileFilter fileFilter = new OrFileFilter();
                    fileFilter.addFileFilter(DirectoryFileFilter.DIRECTORY);
                    fileFilter.addFileFilter(new WildcardFileFilter("*"));
                    FileUtils.copyDirectory(new File(metacatIndexSolrHome),
                                                            new File(solrHomePath), fileFilter );
                } 
            } catch (Exception ue) {    
                String errorString = "SolrConfigUpdator.update - Could not initialize directory: "
                                                          + solrHomePath + " : " + ue.getMessage();
                throw new AdminException(errorString);
            }
        } else {
            // check it
            if (!FileUtil.isDirectory(solrHomePath)) {
                String errorString =
                        "SolrConfigUpdator.update - SOLR home is not a directory: " + solrHomePath;
                throw new AdminException(errorString);
            } else {
                File metacatIndexConfigFile = new File(metacatIndexSolrHome+CONFIGFILERELATIVEPATH);
                File configFile = new File(solrHomePath+CONFIGFILERELATIVEPATH);
                if(metacatIndexConfigFile.exists()) {
                    if(!configFile.exists()) {
                        FileUtils.copyFile(metacatIndexConfigFile, configFile);
                    } else {
                        FileInputStream configInputStream = new FileInputStream(configFile);
                        Checksum checkSum = null;
                        try {
                            checkSum = ChecksumUtil.checksum(configInputStream, MD5);
                            if(configInputStream != null) {
                                IOUtils.closeQuietly(configInputStream);
                            }
                        } finally {
                            if(configInputStream != null) {
                                IOUtils.closeQuietly(configInputStream);
                            }
                        }
                        String error1 = "Metacat couldn't determine if the solrconfig.xml in the "
                                     + solrHomePath + "/conf" + " was modified or not. We backup "
                                + "the file to solrconfig.xml.org. If you did modify it, please "
                                + "manually merge the change back to the file solrconfig.xml"
                                + " at " + solrHomePath + "/conf.";
                        String error3 = "Metacat determined the solrconfig.xml in the "
                                        + solrHomePath + "/conf was customized. Metacat backuped "
                                        + "the file to solrconfig.xml.org. You have to manually fix"
                               + " the issue - merge the change back to the solrconfig.xml in the "
                                        + solrHomePath
                               + "/conf. You may click the OK button when you finish the merging.";
                        if(checkSum != null) {
                            String checksumValue = checkSum.getValue();
                            logMetacat.info("the existing schema.xml in the solr home has the "
                                                                + "checksum " + checksumValue);
                            if(checksumValue != null) {
                                if(checksumValue.equals(currentHash)) {
                                    //it has the newest schema, do nothing
                                    logMetacat.info("the existing config.xml in the solr home has "
                                    + "the same checksum as our current release, do nothing") ;
                                } else {
                                    boolean found = false;
                                    for(String value : releasedConfigHashList) {
                                        if (value.equals(checksumValue)) {
                                            found = true;
                                            break;
                                        }
                                    }
                                    if(found) {
                                        //there is no change in the solrconfig.
                                        //We can silently overwrite it.
                                        FileUtils.copyFile(metacatIndexConfigFile, configFile);
                                    } else {
                                        //users changed the solrconfig.xml file.
                                        //backup the original file:
                                        File backupFile =
                                           new File(solrHomePath + CONFIGFILERELATIVEPATH + ".org");
                                        FileUtils.copyFile(configFile, backupFile);
                                        //overwrite the solr.config file
                                        FileUtils.copyFile(metacatIndexConfigFile, configFile);
                                        //throw an exception to give users a warning
                                        throw new SolrSchemaModificationException(error3);
                                    }
                                }
                            } else {
                                File backupFile =
                                        new File(solrHomePath + CONFIGFILERELATIVEPATH + ".org");
                                FileUtils.copyFile(configFile, backupFile);
                                //overwrite the solr.config file
                                FileUtils.copyFile(metacatIndexConfigFile, configFile);
                                throw new SolrSchemaModificationException(error1);
                            }
                        } else {
                            File backupFile =
                                    new File(solrHomePath + CONFIGFILERELATIVEPATH + ".org");
                            FileUtils.copyFile(configFile, backupFile);
                            //overwrite the solr.config file
                            FileUtils.copyFile(metacatIndexConfigFile, configFile);
                            throw new SolrSchemaModificationException(error1);
                        }
                    }
                }

            }
        }
    }

}
