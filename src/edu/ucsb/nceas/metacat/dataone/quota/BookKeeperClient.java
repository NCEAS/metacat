package edu.ucsb.nceas.metacat.dataone.quota;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.message.BasicHeader;
import org.apache.wicket.util.io.IOUtils;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.dataone.bookkeeper.api.Quota;
import org.dataone.bookkeeper.api.QuotaList;
import org.dataone.bookkeeper.api.Usage;
import org.dataone.bookkeeper.api.UsageList;
import org.dataone.configuration.Settings;
import org.dataone.service.exceptions.InvalidRequest;
import org.dataone.service.exceptions.NotFound;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.types.v1.Subject;

import com.fasterxml.jackson.databind.ObjectMapper;


/**
 * A client class to access the BookKeeper service
 * @author tao
 *
 */
public class BookKeeperClient {
    public static final int DEFAULT_REMOTE_USAGE_ID = -1;
    
    private static final String QUOTAS = "quotas";
    private static final String QUOTATYPE = "quotaType";
    private static final String QUOTAID = "quotaId";
    private static final String QUOTASUBJECT = "subject";
    private static final String REQUESTOR = "requestor";
    private static final String USAGES = "usages";
    private static final String INSTANCEID = "instanceId";
    private static final String AUTHORIZATION = "Authorization";
    private static final String BEARER= "Bearer";
    private static Log logMetacat  = LogFactory.getLog(BookKeeperClient.class);
    private static BookKeeperClient bookKeeperClient = null;
    
    private String bookKeeperURL = null;
    private CloseableHttpClient httpClient = null;
    private ObjectMapper mapper = new ObjectMapper();
    private BasicHeader header = null;
    
    
    /**
     * A private constructor
     * @throws IOException 
     * @throws ServiceFailure 
     */
    protected BookKeeperClient() throws ServiceFailure {
        if (bookKeeperURL == null) {
            bookKeeperURL = Settings.getConfiguration().getString("dataone.quotas.bookkeeper.serviceUrl");
            logMetacat.debug("BookKeeperClient.BookKeeperClient - the bookkeeper service url from the metacat.properties file is " + bookKeeperURL);
            if (bookKeeperURL == null || bookKeeperURL.trim().equals("")) {
                throw new ServiceFailure("0000", "The quota service url can't be null or blank. Please ask the Metacat admin to check the property \"dataone.quotas.bookkeeper.serviceUrl\" in its metacat.properties file.");
            }
            if (!bookKeeperURL.endsWith("/")) {
                bookKeeperURL = bookKeeperURL + "/";
            }
            logMetacat.debug("BookKeeperClient.BookKeeperClient - the final bookkeeper service url is " + bookKeeperURL);
        }
        if (header == null) {
            String token = readTokenFromFile();
            header = new BasicHeader(AUTHORIZATION,  BEARER + " " + token);
        }
        //Since the token always expires every three months, we need reload it very two hours in case the token has been renewed.
        Timer timer = new Timer("Signing Certificate Monitor");
        long tokenMonitorPeriod = 120 * 60 * 1000;
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    String token = readTokenFromFile();
                    header = new BasicHeader(AUTHORIZATION,  BEARER + " " + token);
                } catch (Exception e) {
                    logMetacat.error("BookKeeperClient - the timer thread couldn't read the token from a file since " + e.getMessage());
                }
            }
        }, new Date(), tokenMonitorPeriod);
        //httpClient = HttpClientBuilder.create().build();
        RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(5 * 1000).build();
        httpClient = HttpClientBuilder.create().setDefaultRequestConfig(requestConfig).build();
    }
    
    /**
     * Read the token from the token file
     * @return  the token string
     * @throws ServiceFailure
     */
    private String readTokenFromFile() throws ServiceFailure {
        String tokenFilePath = Settings.getConfiguration().getString("dataone.nodeToken.file");
        File tokenFile = new File(tokenFilePath);
        String token = null;
        try {
            token = FileUtils.readFileToString(tokenFile, "UTF-8");
        } catch (IOException e) {
            throw new ServiceFailure("1190", "BookKeeperClient.readTokenFromFile - The BookKeeper client can't read the token file since " +e.getMessage());
        }
        if (token == null || token.trim().equals("")) {
            throw new ServiceFailure("1190", "BookKeeperClient.readTokenFromFile - The member node token can't be null or blank when it access the remote quota service. Please ask the Metacat admin to check the content of the token file with the path " + tokenFilePath + 
                    ". If the token file path is null or blank, please ask the Metacat admin to set the proper token file path at the property \"dataone.bearToken.file\" in its metacat.properties file.");
        }
        logMetacat.info("BookKeeperClient.readTokenFromFile - successfully read a token from the file " + tokenFilePath);
        return token;
    }
    
    /**
     * Get the singleton instance of the BookKeeplerClient class
     * @return  the instance of the class
     * @throws IOException 
     * @throws ServiceFailure 
     */
    public static BookKeeperClient getInstance() throws ServiceFailure {
        if (bookKeeperClient == null) {
            synchronized (BookKeeperClient.class) {
              if (bookKeeperClient == null) {
                  bookKeeperClient = new BookKeeperClient();
              }
           }
         }
         return bookKeeperClient;
    }
    
    
    /**
     * List the quotas associated with the given subject
     * @param quotaSubject  the subject who owns the quotas
     * @param requestor  the subject of user who will request a usage 
     * @param quotaType  the type of the quotas (storage or portal)
     * @return  the list of quotas associated with the subject.
     * @throws IOException 
     * @throws ClientProtocolException 
     * @throws NotFound 
     * @throws ServiceFailure 
     * @throws InvalidRequest 
     * @throws UnsupportedEncodingException 
     */
    public List<Quota> listQuotas(String quotaSubject, String requestor, String quotaType) throws ServiceFailure, NotFound, InvalidRequest, UnsupportedEncodingException {
        List<Quota> result = null;
        String restStr = bookKeeperURL + QUOTAS;
        boolean hasQuestionMark = false;
        if (quotaSubject != null && !quotaSubject.trim().equals("")) {
            restStr = restStr + "?"+ QUOTASUBJECT + "=" + escapeURL(quotaSubject);
            hasQuestionMark = true;
        }
        if (quotaType != null && !quotaType.trim().equals("")) {
            if (!hasQuestionMark) {
                restStr = restStr + "?"+ QUOTATYPE + "=" + escapeURL(quotaType);
                hasQuestionMark = true;
            } else {
                restStr = restStr + "&" + QUOTATYPE + "=" + escapeURL(quotaType);
            }
        }
        if (requestor != null && !requestor.trim().equals("")) {
            if (!hasQuestionMark) {
                restStr = restStr + "?" + REQUESTOR + "=" + escapeURL(requestor);
                hasQuestionMark = true;
            } else {
                restStr = restStr + "&" + REQUESTOR + "=" + escapeURL(requestor);
            }
        }
        logMetacat.debug("===========================BookKeeperClient.listQuotas - the rest request to list the quotas is " + restStr);
        HttpGet get = new HttpGet(restStr);
        get.addHeader(header);
        CloseableHttpResponse response = null;
        try {
            response = httpClient.execute(get);
            int status = response.getStatusLine().getStatusCode();
            if (status == 200) {
                QuotaList list = mapper.readValue(response.getEntity().getContent(), QuotaList.class);
                if (list != null && list.getQuotas() != null && list.getQuotas().size() > 0) {
                    result = list.getQuotas();
                    logMetacat.debug("BookKeeperClient.listQuotas - the bookkeeper service return a list of quotas with the size " + result.size());
                } else {
                    logMetacat.debug("BookKeeperClient.listQuotas - the bookkeeper service return null or empty");
                    throw new NotFound("1103", "QuotaService didn't find a quota for the quota subject " + quotaSubject + " with quota type " + quotaType + " for the requestor " + requestor);
                }
            } else if (status == 404) {
                throw new NotFound("1103", "The quota with the quota subject " + quotaSubject + " is not found");
            } else {
                String error = IOUtils.toString(response.getEntity().getContent());
                throw new ServiceFailure("1190", "Quota service can't fulfill to list quotas since " + error);
            }
        } catch (ClientProtocolException e) {
            throw new ServiceFailure("1190", "Quota service can't fulfill to list quotas since " + e.getMessage());
        } catch (IOException e) {
            throw new ServiceFailure("1190", "Quota service can't fulfill to list quotas since " + e.getMessage());
        } finally {
            if (response != null) {
                try {
                    response.close();
                } catch (IOException ee) {
                    logMetacat.warn("BookKeeperClient.listQuotas - can't close the reponse at the finally cluae since " + ee.getMessage());
                }
                
            }
        }
        return result;
    }
    
    /**
     * Create a usage record for a given quota identifier in the book keeper service. If it fails, an exception will be thrown
     * @param usage  the object of the usage will be created
     * @return  the usage id from the remote book keeper server
     * @throws ClientProtocolException
     * @throws IOException
     * @throws ServiceFailure
     */
    public int createUsage(Usage usage) throws ClientProtocolException, IOException, ServiceFailure {
        int remoteUsageId = DEFAULT_REMOTE_USAGE_ID;
        String restStr = bookKeeperURL + USAGES;
        logMetacat.debug("BookKeeperClient.createUsage - the rest request to create the usuage is " + restStr);
        String jsonStr = mapper.writeValueAsString(usage); 
        logMetacat.debug("BookKeeperClient.createUsage - the json string will be sent is " + jsonStr);
        StringEntity reqEntity = new StringEntity(jsonStr, ContentType.APPLICATION_JSON);
        reqEntity.setChunked(true);
        HttpPost post = new HttpPost(restStr);
        post.setEntity(reqEntity);
        post.addHeader(header);
        CloseableHttpResponse response = null;
        try {
            response = httpClient.execute(post);
            if (response.getStatusLine().getStatusCode() != 200) {
                String error = IOUtils.toString(response.getEntity().getContent());
                throw new ServiceFailure("0000", "Quota service can't create the usage since " + error);
            } else {
                Usage returnedUsage = mapper.readValue(response.getEntity().getContent(), Usage.class);
                if (returnedUsage != null && returnedUsage.getId() != null) {
                    remoteUsageId = returnedUsage.getId();
                    logMetacat.info("BookKeeperClient.createUsage - successfully create the usage for quota id " + usage.getQuotaId() + " and the instance id " + usage.getInstanceId() + " in the remote book keeper server with the remote usage id " + remoteUsageId);
                }
            }
        } finally {
            if (response != null) {
                response.close();
            }
        }
        logMetacat.debug("BookKeeperClient.createUsage - the final remoteUsageId is "+ remoteUsageId);
        return remoteUsageId;
    }
    
    /**
     * Delete the usage with the given quota type and instance id in the remote book keeper service
     * @param quotaType  the quota type associated with the usage
     * @param instanceId  the instance id associated with the usage
     * @return  the remote usage id which was deleted. If it returns the default value -1, it means there is no remote usage matching the quota id and instance id and Metacat did nothing.
     * @throws InvalidRequest
     * @throws ClientProtocolException
     * @throws ServiceFailure
     * @throws IOException
     */
    public int deleteUsage(int quotaId, String instanceId) throws InvalidRequest, ClientProtocolException, ServiceFailure, IOException {
        int remoteUsageId = DEFAULT_REMOTE_USAGE_ID;
        if (instanceId != null && !instanceId.trim().equals("")) {
            int id = getRemoteUsageId(quotaId, instanceId);
            if (id != DEFAULT_REMOTE_USAGE_ID) {
                logMetacat.debug("BookKeeperClient.deleteUsage - the book keeper service find the usage with id " + id + " matching the quota id " + quotaId + " and instance id " + instanceId);
                String restStr = bookKeeperURL + USAGES + "/" + id;
                logMetacat.debug("BookKeeperClient.deleteUsage - the delete rest command is " + restStr);
                CloseableHttpResponse response = null;
                try {
                    HttpDelete httpdelete = new HttpDelete(restStr);
                    httpdelete.addHeader(header);
                    response = httpClient.execute(httpdelete);
                    int status = response.getStatusLine().getStatusCode();
                    if (status == 200) {
                        remoteUsageId = id;
                        logMetacat.info("BookKeeperClient.deleteUsage - successfully delete the usage with id " + id);
                    } else {
                        String error = IOUtils.toString(response.getEntity().getContent());
                        throw new ServiceFailure("0000", "BookKeeperClient.deleteUsage - can't delete the usage with the id " + id + " since " + error);
                    }
                } finally {
                    if (response != null) {
                        response.close();
                    }
                }
            } else {
                logMetacat.info("BookKeeperClient.deleteUsage - the book keeper service can't find the usage matching the quota id " + quotaId + " and instance id " + instanceId + ". So we don't need to delete anything.");
                remoteUsageId = DEFAULT_REMOTE_USAGE_ID;
            }
        } else {
            throw new InvalidRequest("0000", "The instance id can't be null or blank when you try to delete a usage.");
        }
        logMetacat.debug("BookKeeperClient.deleteUsage - the final deleted remoteUsageId is "+ remoteUsageId);
        return remoteUsageId;
    }
    
    /**
     * Update an existing usage with the given quota id and instance id
     * @param quotaId  the quota id which the existing usage matches
     * @param instanceId  the instance id which the existing usage matches
     * @param usage  the new usage value will be used
     * @return  the remote usage id which was updated. If it returns the default value -1, it means there is no remote usage matching the quota id and instance id and Metacat did nothing.
     * @throws InvalidRequest
     * @throws ClientProtocolException
     * @throws ServiceFailure
     * @throws IOException
     */
    public int updateUsage(int quotaId, String instanceId, Usage usage) throws InvalidRequest, ClientProtocolException, ServiceFailure, IOException {
        int remoteUsageId = DEFAULT_REMOTE_USAGE_ID;
        if (instanceId != null && !instanceId.trim().equals("")) {
            int id = getRemoteUsageId(quotaId, instanceId);//the usage id in the remote book keeper server for this usage
            if (id != DEFAULT_REMOTE_USAGE_ID) {
                usage.setId(id);//set the real usage id from the remote book keeper server for this usage
                logMetacat.debug("BookKeeperClient.updateUsage - the book keeper service find the usage with id " + id + " matching the quota id " + quotaId + " and instance id " + instanceId);
                String restStr = bookKeeperURL + USAGES + "/" + id;
                logMetacat.debug("BookKeeperClient.updateUsage - the update rest command is " + restStr);
                CloseableHttpResponse response = null;
                try {
                    String jsonStr = mapper.writeValueAsString(usage); 
                    logMetacat.debug("BookKeeperClient.updateUsage - the json string will be sent is " + jsonStr);
                    StringEntity reqEntity = new StringEntity(jsonStr, ContentType.APPLICATION_JSON);
                    reqEntity.setChunked(true);
                    HttpPut put = new HttpPut(restStr);
                    put.setEntity(reqEntity);
                    put.addHeader(header);
                    response = httpClient.execute(put);
                    int status = response.getStatusLine().getStatusCode();
                    if (status == 200) {
                        logMetacat.info("BookKeeperClient.updateUsage - successfully update the usage with id " + id);
                        remoteUsageId = id;
                    } else {
                        String error = IOUtils.toString(response.getEntity().getContent());
                        throw new ServiceFailure("0000", "BookKeeperClient.updateUsage - can't delete the usage with the id " + id + " since " + error);
                    }
                } finally {
                    if (response != null) {
                        response.close();
                    }
                }
            } else {
                logMetacat.info("BookKeeperClient.updateUsage - the book keeper service can't find the usage matching the quota id " + quotaId + " and instance id " + instanceId + ". So we don't need to update anything.");
                remoteUsageId = DEFAULT_REMOTE_USAGE_ID;
            }
        } else {
            throw new InvalidRequest("0000", "The instance id can't be null or blank when you try to update a usage.");
        }
        logMetacat.debug("BookKeeperClient.updateUsage - the final remoteUsageId is "+ remoteUsageId);
        return remoteUsageId;
    }
    
    /**
     * Get the remote usage id for the given quota id and instance id
     * @param quotaId  the remote usage associated with the quota id
     * @param instanceId  the remote usage associated with the instance id
     * @return  the remote usage id. If there is no remote usage found, -1 will be returned
     * @throws ClientProtocolException
     * @throws ServiceFailure
     * @throws IOException
     */
    int getRemoteUsageId(int quotaId, String instanceId) throws ClientProtocolException, ServiceFailure, IOException {
        int remoteUsageId = DEFAULT_REMOTE_USAGE_ID;
        try {
            remoteUsageId = QuotaDBManager.lookupRemoteUsageId(quotaId, instanceId);
        } catch (Exception e) {
            logMetacat.debug("BookKeeperClient.getRemoteUsageId - failed to get the remote usage id locally for the quota id " + quotaId + " and instance id " + instanceId + " since " +e.getMessage());
        }
        if (remoteUsageId == DEFAULT_REMOTE_USAGE_ID) {
            logMetacat.debug("BookKeeperClient.getRemoteUsageId -  Metacat can't find the remote usage id locally. It will get the remote usage id from the remote book keeper server for the quota id " + quotaId + " and instance id " + instanceId);
            List<Usage> usages = null;
            try {
                usages = listUsages(quotaId, instanceId);
                if (usages == null || usages.size() == 0) {
                    logMetacat.warn("BookKeeperClient.getRemoteUsageId - the book keeper service don't find any usages matching the quota id " + quotaId + " and instance id " + instanceId + ". So we set the remote id to " + DEFAULT_REMOTE_USAGE_ID);
                    remoteUsageId = DEFAULT_REMOTE_USAGE_ID;
                } else if (usages.size() == 1) {
                    Usage existedUsage = usages.get(0);
                    remoteUsageId= existedUsage.getId();//the usage id in the remote book keeper server for this usage
                } else {
                    throw new ServiceFailure("0000", "BookKeeperClient.getRemoteUsageId - the bookkeeper service should only send back one record with the given quota id "+ quotaId + " and instance id " + instanceId 
                            + ". However, it sent back more than one. Something is wrong in the bookkeeper service.");
                }
            } catch (NotFound e) {
                logMetacat.warn("BookKeeperClient.getRemoteUsageId - the book keeper service don't find any usages matching the quota id " + quotaId + " and instance id " + instanceId + ". So we set the remote id to " + DEFAULT_REMOTE_USAGE_ID);
                remoteUsageId = DEFAULT_REMOTE_USAGE_ID;
            }
        }
        logMetacat.debug("BookKeeperClient.getRemoteUsageId - the final returned remote usage id is " + remoteUsageId + " for the quota id " + quotaId + " and instance id " + instanceId);
        return remoteUsageId;
    }
    
    /**
     * Get the list of usage from the book keeper service with the given quota id and instance id
     * @param quotaId  the quota id associated with the usage
     * @param instanceId  the instance id associated with the usage
     * @return the list of usages matching the query 
     * @throws ClientProtocolException
     * @throws IOException
     * @throws NotFound
     * @throws ServiceFailure
     */
     List<Usage> listUsages(int quotaId, String instanceId) throws ClientProtocolException, IOException, NotFound, ServiceFailure {
        String restStr = bookKeeperURL + USAGES + "/?" + QUOTAID + "=" + quotaId + "&" + INSTANCEID + "=" + escapeURL(instanceId);
        logMetacat.debug("BookKeeperClient.getUsageId - the rest request to get the usuage id is " + restStr);
        HttpGet get = new HttpGet(restStr);
        get.addHeader(header);
        CloseableHttpResponse response = null;
        List<Usage> result = null;
        try {
            response = httpClient.execute(get);
            int status = response.getStatusLine().getStatusCode();
            if (status == 200) {
                UsageList list = mapper.readValue(response.getEntity().getContent(), UsageList.class);
                if (list != null) {
                    result = list.getUsages();
                }
            } else if (status == 404) {
                throw new NotFound("0000", "BookKeeperClient.getUsageId - the usage with the quota id " + quotaId + " and instance id " + instanceId + "is not found");
            } else {
                String error = IOUtils.toString(response.getEntity().getContent());
                throw new ServiceFailure("0000", "BookKeeperClient.getUsageId - quota service can't fulfill to list usages since " + error);
            }
        } finally {
            if (response != null) {
                response.close();
            }
        }
        return result;
    }
    
     /**
      * Encode the str to be URL safe
      * @param str  the original string will be encoded
      * @return  the encoded str for URL
      * @throws UnsupportedEncodingException
      */
    private String escapeURL(String str) throws UnsupportedEncodingException {
        return URLEncoder.encode(str, "UTF-8");
    }
}
