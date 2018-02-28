package edu.ucsb.nceas.metacat.dataone;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dataone.client.auth.AuthTokenSession;
import org.dataone.client.exception.ClientSideException;
import org.dataone.client.v2.MNode;
import org.dataone.client.v2.impl.MultipartMNode;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v2.SystemMetadata;
import org.dataone.service.util.TypeMarshaller;
import org.junit.Test;
import edu.ucsb.nceas.MCTestCase;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.metacat.util.SystemUtil;

public class MultiThreadClientIT extends MCTestCase {
    //need to configure this.
    private static String tokenPath = "/Users/tao/curl-files/token";
    
    protected static Log log = LogFactory.getLog(MultiThreadClientIT.class);
    private static ExecutorService executor = Executors.newFixedThreadPool(5);
    private static int number_of_objects = 30;
    private static String mnURL = null;
    static {
        try {
            mnURL = SystemUtil.getContextURL()+"/d1/mn";
            //System.out.println("============================the member node url is "+mnURL);
        } catch (Exception e) {
            log.error("MultiThreadClientIT - can't find the contexturl from the metacat.porperties file.", e);
        }
    }
    
    @Test 
    public void testConstructUsingRegisteredCertificate() throws IOException, ClientSideException, Exception {
        String token = FileUtils.readFileToString(new File(tokenPath), "UTF-8");
        final AuthTokenSession tokenSession = new AuthTokenSession(token);
        final MNode node = new MultipartMNode(tokenSession.getMultipartRestClient(), mnURL, tokenSession);
        Runnable runner = new Runnable() {
            @Override
            public void run() {
                try {
                    double random = Math.random()*10000;
                    long round = Math.round(random);
                    long time = System.nanoTime();
                    String identifier ="test"+time+round;
                    Identifier pid = new Identifier();
                    pid.setValue(identifier);
                    FileInputStream sysFile = new FileInputStream("test/sysmeta-noaa.xml");
                    SystemMetadata sysmeta = TypeMarshaller.unmarshalTypeFromStream(SystemMetadata.class, sysFile);
                    sysmeta.setIdentifier(pid);
                    FileInputStream object = new FileInputStream("test/sciencemetadata-noaa.xml");
                    node.create(tokenSession, pid, object, sysmeta);
                    System.out.println("after sending -------"+identifier);
                    object.close();
                    sysFile.close();
                } catch (Exception e) {
                    log.error("Error running: " + e.getMessage(), e);
                    //throw new RuntimeException(e.getMessage(), e);
                }
            }
        };
        // submit the task, and that's it
        Future future = null;
        for(int i=0; i<number_of_objects; i++) {
            future = executor.submit(runner);
        }
        while (!future.isDone()) {
            Thread.sleep(5000);
        }
        Thread.sleep(20000);
    }

}
