/**
 *    Purpose: Implements a service for managing a Hazelcast cluster member
 *  Copyright: 2020 Regents of the University of California and the
 *             National Center for Ecological Analysis and Synthesis
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
package edu.ucsb.nceas.metacat.dataone.quota;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.message.BasicHeader;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.dataone.bookkeeper.api.Quota;
import org.dataone.bookkeeper.api.Usage;
import org.dataone.configuration.Settings;
import org.dataone.service.types.v1.Subject;

import com.fasterxml.jackson.databind.ObjectMapper;


/**
 * A client class to access the BookKeeper service
 * @author tao
 *
 */
public class BookKeeperClient {
    
    private String bookKeeperURL = null;
    private CloseableHttpClient httpClient = null;
    private static final String QUOTAS = "quotas";
    private static Log logMetacat  = LogFactory.getLog(BookKeeperClient.class);
    private static BookKeeperClient bookKeeperClient = null;
    private static final String NAME = "name";
    private static final String SUBSCRIPTIONSUBJECT = "subscriptionSubject";
    private static final String PROXYSUBJECT= "proxySubject";
    private static final String USAGE = "usage";
    private ObjectMapper mapper = new ObjectMapper();
    private BasicHeader header = null;
    
    
    /**
     * A private constructor
     * @throws IOException 
     */
    protected BookKeeperClient() throws IOException {
        if (bookKeeperURL == null) {
            bookKeeperURL = Settings.getConfiguration().getString("dataone.quotas.bookkeeper.serviceUrl");
            logMetacat.debug("BookKeeperClient.BookKeeperClient - the bookkeeper service url is " + bookKeeperURL);
            if (bookKeeperURL != null && !bookKeeperURL.endsWith("/")) {
                bookKeeperURL = bookKeeperURL + "/";
            }
        }
        if (header == null) {
            String tokenFilePath = Settings.getConfiguration().getString("dataone.bearToken.file");
            File tokenFile = new File(tokenFilePath);
            String token = FileUtils.readFileToString(tokenFile, "UTF-8");
            header = new BasicHeader("Authorization", "Beaer " + token);
        }
        logMetacat.debug("BookKeeperClient.BookKeeperClient - the bookekeeper service final url is " + bookKeeperURL);
        httpClient = HttpClientBuilder.create().build();
    }
    
    /**
     * Get the singleton instance of the BookKeeplerClient class
     * @return  the instance of the class
     * @throws IOException 
     */
    public static BookKeeperClient getInstance() throws IOException {
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
     * @param subject  the subject who owns the list of quotas
     * @param name  the name of the quotas (storage or portal)
     * @param proxySubject  the submitter's subject of this object 
     * @return  the list of quotas associated with the subject. null may be returned if the subject is null.
     * @throws IOException 
     * @throws ClientProtocolException 
     */
    public List<Quota> listQuotas(Subject subscriptionSubject, String name, Subject proxySubject) throws ClientProtocolException, IOException {
        List<Quota> result = null;
        if (subscriptionSubject != null && name != null && proxySubject != null) {
            String restStr = bookKeeperURL + QUOTAS + "?"+ SUBSCRIPTIONSUBJECT + "=" + subscriptionSubject.getValue() + "&" + NAME + "=" + name + "&" + PROXYSUBJECT + proxySubject.getValue();
            logMetacat.debug("BookKeeperClient.listQuotas - the rest request to list the quotas is " + restStr);
            HttpGet get = new HttpGet(restStr);
            get.addHeader(header);
            CloseableHttpResponse response = null;
            try {
                response = httpClient.execute(get);
                result = mapper.readValue(response.getEntity().getContent(), List.class);
            } finally {
                if (response != null) {
                    response.close();
                }
            }
        }
        return result;
    }
    
    /**
     * Create a usage record for a given quota identifier in the book keeper service
     * @param quotaId  the id of the quota which the usage will belong to
     * @param usage  the object of the usage will be created
     * @throws ClientProtocolException
     * @throws IOException
     */
    public void createUsage(String quotaId, Usage usage) throws ClientProtocolException, IOException {
        String restStr = bookKeeperURL + QUOTAS + "/" + USAGE;
        logMetacat.debug("BookKeeperClient.updateUsage - the rest request to create the usuage is " + restStr);
        String jsonStr = mapper.writeValueAsString(usage); 
        logMetacat.debug("BookKeeperClient.updateUsage - the json string will be sent is " + jsonStr);
        StringEntity reqEntity = new StringEntity(jsonStr, ContentType.APPLICATION_JSON);
        reqEntity.setChunked(true);
        HttpPost post = new HttpPost(restStr);
        post.setEntity(reqEntity);
        post.addHeader(header);
        CloseableHttpResponse response = null;
        try {
            response = httpClient.execute(post);
        } finally {
            if (response != null) {
                response.close();
            }
        }
    }

}
