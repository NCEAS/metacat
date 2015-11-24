/*
 * ClientView.java
 *
 * Created on June 25, 2007, 9:57 AM
 *
 */

package edu.ucsb.nceas.metacat.clientview;

import java.io.Serializable;

/**
 * Description information for the ClientView bean.
 * @author Christopher Barteau
 */
public class ClientView implements Serializable {
    
    public static final String                          CLIENT_VIEW_BEAN = "clientViewBean";
    public static final String                          ECOLOGICAL_METADATA_LANGUAGE = "EML";
    public static final String                          FEDERAL_GEOGRAPHIC_DATA_COMMITTEE = "FGDC";
    
    public static final int                             LOGIN_MESSAGE = 0;
    public static final int                             UPLOAD_MESSAGE = 1;
    public static final int                             DELETE_MESSAGE = 2;
    public static final int                             SELECT_MESSAGE = 3;
    public static final int                             ERROR_MESSAGE = 4;
    public static final int                             REPLACE_MESSAGE = 5;
    public static final int                             UPDATE_MESSAGE = 6;
    public static final int                             FORMAT_TYPE = 0;
    public static final int                             FILE_NAME = 1;
    
    
    /**
     * Creates a new instance of ClientView
     */
    public ClientView() {
    }

    /**
     * Holds value of property action.
     */
    private String action;

    /**
     * Getter for property action.
     * @return Value of property action.
     */
    public String getAction() {
        return this.action;
    }

    /**
     * Setter for property action.
     * @param action New value of property action.
     */
    public void setAction(String action) {
        this.action = action;
    }

    /**
     * Holds value of property qFormat.
     */
    private String qformat;

    /**
     * Getter for property qformat.
     * @return Value of property qformat.
     */
    public String getQformat() {
        return this.qformat;
    }

    /**
     * Setter for property qformat.
     * @param qformat New value of property qformat.
     */
    public void setQformat(String qformat) {
        this.qformat = qformat;
    }

    /**
     * Holds value of property username.
     */
    private String username;

    /**
     * Getter for property userName.
     * @return Value of property userName.
     */
    public String getUsername() {
        return this.username;
    }

    /**
     * Setter for property username.
     * @param username New value of property username.
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * Holds value of property organization.
     */
    private String organization;

    /**
     * Getter for property organization.
     * @return Value of property organization.
     */
    public String getOrganization() {
        return this.organization;
    }

    /**
     * Setter for property organization.
     * @param organization New value of property organization.
     */
    public void setOrganization(String organization) {
        this.organization = organization;
    }

    /**
     * Holds value of property password.
     */
    private String password;

    /**
     * Getter for property password.
     * @return Value of property password.
     */
    public String getPassword() {
        return this.password;
    }

    /**
     * Setter for property password.
     * @param password New value of property password.
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * Holds value of property sessionid.
     */
    private String sessionid;

    /**
     * Getter for property sessionId.
     * @return Value of property sessionId.
     */
    public String getSessionid() {
        return this.sessionid;
    }

    /**
     * Setter for property sessionid.
     * @param sessionid New value of property sessionid.
     */
    public void setSessionid(String sessionid) {
        this.sessionid = sessionid;
    }

    /**
     * Holds value of property anyfield.
     */
    private String anyfield;

    /**
     * Getter for property anyField.
     * @return Value of property anyField.
     */
    public String getAnyfield() {
        return this.anyfield;
    }

    /**
     * Setter for property anyfield.
     * @param anyfield New value of property anyfield.
     */
    public void setAnyfield(String anyfield) {
        this.anyfield = anyfield;
    }

    /**
     * Holds value of property pathExpr.
     */
    private String pathExpr;

    /**
     * Getter for property pathExpr.
     * @return Value of property pathExpr.
     */
    public String getPathExpr() {
        return this.pathExpr;
    }

    /**
     * Setter for property pathExpr.
     * @param pathExpr New value of property pathExpr.
     */
    public void setPathExpr(String pathExpr) {
        this.pathExpr = pathExpr;
    }

    /**
     * Holds value of property pathValue.
     */
    private String pathValue;

    /**
     * Getter for property pathValue.
     * @return Value of property pathValue.
     */
    public String getPathValue() {
        return this.pathValue;
    }

    /**
     * Setter for property pathValue.
     * @param pathValue New value of property pathValue.
     */
    public void setPathValue(String pathValue) {
        this.pathValue = pathValue;
    }

    /**
     * Holds value of property returnfield.
     */
    private String returnfield;

    /**
     * Getter for property returnField.
     * @return Value of property returnField.
     */
    public String getReturnfield() {
        return this.returnfield;
    }

    /**
     * Setter for property returnfield.
     * @param returnfield New value of property returnField.
     */
    public void setReturnfield(String returnfield) {
        this.returnfield = returnfield;
    }

    /**
     * Holds value of property publicAccess.
     */
    private boolean publicAccess;

    /**
     * Getter for property publicAccess.
     * @return Value of property publicAccess.
     */
    public boolean isPublicAccess() {
        return this.publicAccess;
    }

    /**
     * Setter for property publicAccess.
     * @param publicAccess New value of property publicAccess.
     */
    public void setPublicAccess(boolean publicAccess) {
        this.publicAccess = publicAccess;
    }

    /**
     * Holds value of property metaFileName.
     */
    private String metaFileName;

    /**
     * Getter for property metaFileName.
     * @return Value of property metaFileName.
     */
    public String getMetaFileName() {
        return this.metaFileName;
    }

    /**
     * Setter for property metaFileName.
     * @param metaFileName New value of property metaFileName.
     */
    public void setMetaFileName(String metaFileName) {
        this.metaFileName = metaFileName;
    }

    /**
     * Holds value of property dataFileName.
     */
    private String[] dataFileName;

    /**
     * Indexed getter for property dataFileNames.
     * @param index Index of the property.
     * @return Value of the property at <CODE>index</CODE>.
     */
    public String getDataFileName(int index) {
        return this.dataFileName[index];
    }

    /**
     * Getter for property dataFileNames.
     * @return Value of property dataFileNames.
     */
    public String[] getDataFileName() {
        return this.dataFileName;
    }

    /**
     * Indexed setter for property dataFileName.
     * @param index Index of the property.
     * @param dataFileName New value of the property at <CODE>index</CODE>.
     */
    public void setDataFileName(int index, String dataFileName) {
        this.dataFileName[index] = dataFileName;
    }

    /**
     * Setter for property dataFileName.
     * @param dataFileName New value of property dataFileName.
     */
    public void setDataFileName(String[] dataFileName) {
        this.dataFileName = dataFileName;
    }

    /**
     * Holds value of property docId.
     */
    private String docId;

    /**
     * Getter for property docId.
     * @return Value of property docId.
     */
    public String getDocId() {
        return this.docId;
    }

    /**
     * Setter for property docId.
     * @param docId New value of property docId.
     */
    public void setDocId(String docId) {
        this.docId = docId;
    }

    /**
     * Holds value of property message.
     */
    private String[] message = new String[10];

    /**
     * Indexed getter for property message.
     * @param index Index of the property.
     * @return Value of the property at <CODE>index</CODE>.
     */
    public String getMessage(int index) {
        return this.message[index];
    }

    /**
     * Getter for property message.
     * @return Value of property message.
     */
    public String[] getMessage() {
        return this.message;
    }

    /**
     * Indexed setter for property message.
     * @param index Index of the property.
     * @param message New value of the property at <CODE>index</CODE>.
     */
    public void setMessage(int index, String message) {
        this.message[index] = message;
    }

    /**
     * Setter for property message.
     * @param message New value of property message.
     */
    public void setMessage(String[] message) {
        this.message = message;
    }

    /**
     * Holds value of property contentStandard.
     */
    private String contentStandard;

    /**
     * Getter for property contentStandard.
     * @return Value of property contentStandard.
     */
    public String getContentStandard() {
        return this.contentStandard;
    }

    /**
     * Setter for property contentStandard.
     * @param contentStandard New value of property contentStandard.
     */
    public void setContentStandard(String contentStandard) {
        this.contentStandard = contentStandard;
    }

    /**
     * Holds value of property isMetaFileDocId.
     */
    private String metaFileDocId;

    /**
     * Getter for property isMetaFileDocId.
     * @return Value of property isMetaFileDocId.
     */
    public String getMetaFileDocId() {
        return this.metaFileDocId;
    }

    /**
     * Setter for property isMetaFileDocId.
     * @param isMetaFileDocId New value of property isMetaFileDocId.
     */
    public void setMetaFileDocId(String metaFileDocId) {
        this.metaFileDocId = metaFileDocId;
    }
    
}
