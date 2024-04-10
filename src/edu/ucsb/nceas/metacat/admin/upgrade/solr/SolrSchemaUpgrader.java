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
import edu.ucsb.nceas.metacat.common.Settings;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.metacat.service.ServiceService;
import edu.ucsb.nceas.metacat.shared.ServiceException;
import edu.ucsb.nceas.utilities.FileUtil;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;
import edu.ucsb.nceas.utilities.StringUtil;



/**
 * This class will overwrite the existing schema under the /solr-home/conf/schema.xml
 * Here is the algorithm: 
 * 1. If the hash value of the existing schema.xml is a one in the list of released schema (this means the administrator
 *    didn't customize the schema ), we will overwrite the schema.xml and remove the solr-last-proccessed-date file. The removal
 *    of the solr-last-proccessed-date file will force the metacat-index to rebuild all solr index when the administrator restart
 *    the tomcat at next time.
 * 2. If the hash value of the existing schema.xml isn't in the list, an exception will be throw.  
 * @author tao
 *
 */
public class SolrSchemaUpgrader {
    
    private static Log logMetacat = LogFactory.getLog(SolrSchemaUpgrader.class);
    private static final String SCHEMAFILERELATIVEPATH = "/conf/schema.xml";
    private static final String MD5 = "MD5";
    private Vector<String> releasedSchemaHashList = new Vector<String>();
    private String solrHomePath = null;
    private String metacatIndexSolrHome = null;
    private String currentHash = null;
    
    /**
     * Constructor
     * @throws PropertyNotFoundException 
     * @throws ServiceException 
     */
    public SolrSchemaUpgrader() throws PropertyNotFoundException, ServiceException
    {
        String hashString = null;
        try {
            hashString = 
                PropertyService.getProperty("index.schema.previous.hash");
            currentHash = PropertyService.getProperty("index.schema.current.hash");
            logMetacat.info("the current hash is ================== "+currentHash);
            solrHomePath = PropertyService.getProperty("solr.homeDir");
            String indexContext = PropertyService.getProperty("index.context");
            String metacatWebInf = ServiceService.getRealConfigDir();
            metacatIndexSolrHome = metacatWebInf + "/../../" + indexContext + "/WEB-INF/classes/solr-home";
        } catch (PropertyNotFoundException pnfe) {
            throw new PropertyNotFoundException("SolrSchemaUpdator.Constructor - could not get a metacat property in the metacat.properties file - "
                            + pnfe.getMessage());
        }
        releasedSchemaHashList = StringUtil.toVector(hashString, ';');
        logMetacat.info("the released hash is ================== "+releasedSchemaHashList);
    }
    
    /**
     * Upgrade the schema in the solr home
     * @throws NoSuchAlgorithmException 
     * @throws SolrSchemaModificationException 
     */
    public void upgrade() throws AdminException, IOException, NoSuchAlgorithmException, SolrSchemaModificationException {
        boolean solrHomeExists = new File(solrHomePath).exists();
        if (!solrHomeExists) {
            //System.out.println("solr home doesn't exist ================== ");
            //create the solr home and copy the files to it if it didn't exist
            try {
                // only attempt to copy if we have the source directory to copy from
                File sourceDir = new File(metacatIndexSolrHome);
                if (sourceDir.exists()) {
                    FileUtil.createDirectory(solrHomePath);
                    OrFileFilter fileFilter = new OrFileFilter();
                    fileFilter.addFileFilter(DirectoryFileFilter.DIRECTORY);
                    fileFilter.addFileFilter(new WildcardFileFilter("*"));
                    FileUtils.copyDirectory(new File(metacatIndexSolrHome), new File(solrHomePath), fileFilter );
                } 
            } catch (Exception ue) {    
                String errorString = "SolrSchemaUpdator.update - Could not initialize directory: " + solrHomePath +
                        " : " + ue.getMessage();
                throw new AdminException(errorString);
                
            }
        } else {
            //System.out.println("solr home does  exist ================== ");
            // check it
            if (!FileUtil.isDirectory(solrHomePath)) {
                String errorString = "SolrSchemaUpdator.update - SOLR home is not a directory: " + solrHomePath;
                throw new AdminException(errorString);
            } else {
                File metacatIndexSchemaFile = new File(metacatIndexSolrHome+SCHEMAFILERELATIVEPATH);
                File schemaFile = new File(solrHomePath+SCHEMAFILERELATIVEPATH);
                File processDateFile = new File(solrHomePath+"/"+Settings.LASTPROCESSEDDATEFILENAME);
                if(metacatIndexSchemaFile.exists()) {
                    if(!schemaFile.exists()) {
                        FileUtils.copyFile(metacatIndexSchemaFile, schemaFile);
                        if(processDateFile.exists()) {
                            processDateFile.delete();
                        }
                        
                    } else {
                        FileInputStream schemaInputStream = new FileInputStream(schemaFile);
                        Checksum checkSum = null;
                        try {
                            checkSum = ChecksumUtil.checksum(schemaInputStream, MD5);
                            if(schemaInputStream != null) {
                                IOUtils.closeQuietly(schemaInputStream);
                            }
                        } finally {
                            if(schemaInputStream != null) {
                                IOUtils.closeQuietly(schemaInputStream);
                            }
                        }
                        String error1 = "SolrSchemaUpdator.update - couldn't determine if the schema.xml in the "+solrHomePath+"/conf"+
                                        " was modified or not. If you did modify it, please manually merge the change to the file "+metacatIndexSolrHome+SCHEMAFILERELATIVEPATH +" and copy it to "+
                                        solrHomePath+"/conf; otherwise, just copy the file "+metacatIndexSolrHome+SCHEMAFILERELATIVEPATH +" to "+solrHomePath+"/conf.";
                        //String error2 ="After configuring Metacat and restarting Tomcat, you have to issue a 'reindexall' action as an administrator to rebuild the Solr index.";
                        String error3 = "Metacat determined the schema.xml in the "+solrHomePath+"/conf"+
                                        " was customized. You have to manually fix the issue - merge the change to the file "+metacatIndexSolrHome+SCHEMAFILERELATIVEPATH +" and copy it to overwrite the schema.xml in the "+
                                        solrHomePath+"/conf. You may click the OK button When you finish the merging. ";
                        if(checkSum != null) {
                            String checksumValue = checkSum.getValue();
                            logMetacat.info("the existing schema.xml in the solr home has the checksum ================== "+checksumValue);
                            if(checksumValue != null) {
                                if(checksumValue.equals(currentHash)) {
                                    //it has the newest schema, do nothing
                                    logMetacat.info("=====the existing schema.xml in the solr home has the same checksum as our current release, do nothing") ;
                                } else {
                                    boolean found = false;
                                    for(String value : releasedSchemaHashList) {
                                        if (value.equals(checksumValue)) {
                                            found = true;
                                            break;
                                        }
                                    }
                                    if(found) {
                                        //there is  no change in the schema. We can silently overwrite and remove the solr-last-process-date file.
                                        //The removal of the solr-last-process-date file will force metacat-index to build all data objects in the
                                        //next tomcat restart
                                        //System.out.println("it is an old copy, overwrite it an delete the process data file ==========================") ;
                                        FileUtils.copyFile(metacatIndexSchemaFile, schemaFile);
                                        if(processDateFile.exists()) {
                                            processDateFile.delete();
                                        }
                                        
                                    } else {
                                        //users changed the schema, we have to throw an exception to ask the administrator to manually merge and overwrite.
                                        throw new SolrSchemaModificationException(error3);
                                    }
                                }
                            } else {
                                throw new SolrSchemaModificationException(error1);
                            }
                        } else {
                            throw new SolrSchemaModificationException(error1);
                        }
                    }
                }

            }
        }
    }
    
   
}
