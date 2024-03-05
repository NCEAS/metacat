package edu.ucsb.nceas.metacat.admin;

import edu.ucsb.nceas.metacat.restservice.D1ResourceHandler;
import edu.ucsb.nceas.metacat.restservice.v2.MNResourceHandler;
import org.apache.commons.logging.LogFactory;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class AdminAuthHandler extends D1ResourceHandler {

    protected static boolean enableAppendLdapGroups = false;

    public AdminAuthHandler(ServletContext servletContext,
                                HttpServletRequest request, HttpServletResponse response) {
        super(servletContext, request, response);
        logMetacat = LogFactory.getLog(MNResourceHandler.class);
    }

    @Override
    public void handle(byte httpVerb) {
        super.handle(httpVerb);


    }
}
