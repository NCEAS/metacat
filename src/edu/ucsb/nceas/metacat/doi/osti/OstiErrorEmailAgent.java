package edu.ucsb.nceas.metacat.doi.osti;

import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.osti_elink.OSTIElinkErrorAgent;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A class sends emails to clients when errors happened during the 
 * interaction process between Metacat and the OSTI Elink service.   
 * @author tao
 *
 */
public class OstiErrorEmailAgent implements OSTIElinkErrorAgent {
    private static String emailPreamble = "Dear operators:\n\nMetacat got the following error message " +
                                            "when it interacted with the OSTI ELink Service.\n\n";
    private static String mailSubject = "OSTI Errors";
    private static String timeFormatPattern = "EEE, d MMM yyyy HH:mm:ss z";
    private static Log logMetacat = LogFactory.getLog(OstiErrorEmailAgent.class);
    private static String server = null;
    private static String smtpHost = null;
    private static int port = 587;
    private static String toMail = null;
    private static String fromMail = null;
    private static Session session = null;
    private static SimpleDateFormat dateFormat = null;
    
    /**
     * Constructor - set the email settings
     */
    public OstiErrorEmailAgent() {
        try {
            server = PropertyService.getProperty("server.name");
            smtpHost = PropertyService.getProperty("guid.doi.mail.smtp.host");
            toMail = PropertyService.getProperty("guid.doi.mail.to");
            fromMail = PropertyService.getProperty("guid.doi.mail.from");
            port = new Integer(PropertyService.getProperty("guid.doi.mail.smtp.port")).intValue();
        } catch (PropertyNotFoundException e) {
            logMetacat.error("OstiEmailErorrAgent.constructor - can't find the property " + e.getMessage());
        } catch (NumberFormatException e) {
            logMetacat.error("OstiEmailErorrAgent.constructor - can't transform the port configuration to a number: " + e.getMessage());
        }
        Properties props = System.getProperties();
        props.put("mail.smtp.host", smtpHost);
        props.put("mail.smtp.port", port);
        session = Session.getDefaultInstance(props);
        dateFormat = new SimpleDateFormat(timeFormatPattern);
    }
    
    /**
     * Send the email with error messages to recipients 
     */
    public void notify(String error) {
        try {
            String serverMessage = "Server: " + server + "\n\n";
            String body = emailPreamble + serverMessage + error;
            MimeMessage msg = new MimeMessage(session);
            //set message headers
            msg.addHeader("Content-type", "text/HTML; charset=UTF-8");
            msg.addHeader("format", "flowed");
            msg.addHeader("Content-Transfer-Encoding", "8bit");
            msg.setFrom(new InternetAddress(fromMail)); 
            msg.setSubject(mailSubject + " from " + server + " at " + dateFormat.format(new Date()), "UTF-8");
            msg.setText(body, "UTF-8");
            msg.setSentDate(new Date());
            msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toMail, false));
            Transport.send(msg);  
        } catch (Exception ee) {
            logMetacat.error("OstiEmailErorrAgent.notify - Metacat got the error message from OSTI: " + error);
            logMetacat.error("OstiEmailErorrAgent.notify - can't send out emails with the above message since " + ee.getMessage());
            
        }
        
    }

}
