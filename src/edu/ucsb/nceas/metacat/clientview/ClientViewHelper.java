/*
 * ClientViewHelper.java
 *
 * Created on June 25, 2007, 9:57 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package edu.ucsb.nceas.metacat.clientview;

import com.oreilly.servlet.multipart.FilePart;
import com.oreilly.servlet.multipart.MultipartParser;
import com.oreilly.servlet.multipart.ParamPart;
import com.oreilly.servlet.multipart.Part;

import edu.ucsb.nceas.metacat.IdentifierManager;
import edu.ucsb.nceas.metacat.McdbDocNotFoundException;
import edu.ucsb.nceas.metacat.accesscontrol.AccessControlException;
import edu.ucsb.nceas.metacat.accesscontrol.AccessControlForSingleFile;
import edu.ucsb.nceas.metacat.accesscontrol.AccessControlList;
import edu.ucsb.nceas.metacat.accesscontrol.XMLAccessAccess;
import edu.ucsb.nceas.metacat.client.InsufficientKarmaException;
import edu.ucsb.nceas.metacat.client.MetacatClient;
import edu.ucsb.nceas.metacat.client.MetacatException;
import edu.ucsb.nceas.metacat.client.MetacatFactory;
import edu.ucsb.nceas.metacat.client.MetacatInaccessibleException;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.metacat.service.SessionService;
import edu.ucsb.nceas.metacat.shared.AccessException;
import edu.ucsb.nceas.metacat.util.DocumentUtil;
import edu.ucsb.nceas.metacat.util.SessionData;
import edu.ucsb.nceas.utilities.XMLUtilities;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;
import edu.ucsb.nceas.utilities.access.AccessControlInterface;
import edu.ucsb.nceas.utilities.access.DocInfoHandler;
import edu.ucsb.nceas.utilities.access.XMLAccessDAO;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;
import java.util.Stack;
import java.util.TreeMap;
import java.util.Vector;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.ContentHandler;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

/**
 *
 * @author barteau
 */
public class ClientViewHelper {
    private XPath                                       xpath = XPathFactory.newInstance().newXPath();
    private HttpSession                                 clientSession;
    private ClientView                                  clientViewBean = null;
    private MetacatClient                               metacatClient = null;
    private boolean                                     loggedIn = false;
    private Document                                    metadataDoc = null;
    private int                                         sizeLimit;
    private String                                      contactName = "";
    
    private static final int DEFAULTFILESIZE = -1;
    private static final String                         LDAP_TEMPLATE = "uid=%1s,o=%2s,dc=ecoinformatics,dc=org";
    
    public static final String                          DOWNLOAD_ACTION = "Download";
    
    public static final String                          PERMISSION_TYPE_ALLOW = "allow";
    
    public static final String                          PERMISSION_TYPE_DISALLOW = "deny";
    
    /**
     * Creates a new instance of ClientViewHelper, using info in an HttpServletRequest
     * for initializing.
     * @param request HttpServletRequest, sent from the client browser.
     * @throws edu.ucsb.nceas.metacat.client.MetacatInaccessibleException Thrown
     */
    public ClientViewHelper(HttpServletRequest request) throws MetacatInaccessibleException {
        String                              host, context;
        
        clientSession = request.getSession(false);
        host = request.getHeader("host");
        context = request.getContextPath();
        init(host, context);
    }
    
    /**
     * Creates a new instance of ClientViewHelper, using parameter values
     * for initializing.  This constructor is plain java code so it's the portal of
     * choice for JUnit testing.
     * @param host The host with port (if needed), such as "localhost:8084".
     * @param context The application root context.
     * @param bean ClientView instance, with pre-populated values.
     * @throws edu.ucsb.nceas.metacat.client.MetacatInaccessibleException thrown
     */
    public ClientViewHelper(String host, String context, ClientView bean) throws MetacatInaccessibleException {
        clientViewBean = bean;
        init(host, context);
    }
    
    private void init(String host, String context) throws MetacatInaccessibleException {
        String                              metacatPath = "http://%1s%2s/metacat";
        String                              tmp;
        
        tmp = metacatPath.replaceFirst("%1s", host);
        metacatPath = tmp.replaceFirst("%2s", context);
        metacatClient = (MetacatClient) MetacatFactory.createMetacatConnection(metacatPath);
        try {
        	sizeLimit = 
        		(new Integer(PropertyService.getProperty("replication.datafilesizelimit"))).intValue();
        } catch (PropertyNotFoundException pnfe) {
        	throw new MetacatInaccessibleException(pnfe.getMessage());
        }
    }
    
    /**
     * Main web API method for handling various actions.
     * @param request HttpServletRequest
     * @param response HttpServletResponse
     * @return String message
     */
    public String clientRequest(HttpServletRequest request, HttpServletResponse response)  {
        String                              result = null, action, contentType;
        MultipartParser                     multipartParser;
        HashMap<String, Object>             responseMap;
        
        getMetacatClient().setSessionId(request.getSession().getId());
        
        if (clientViewBean == null) {
            clientViewBean = (ClientView) clientSession.getAttribute(ClientView.CLIENT_VIEW_BEAN);
            
            if (clientViewBean == null) {
            	//make a new one and shove it in the session
            	clientViewBean = new ClientView();
            	clientSession.setAttribute(ClientView.CLIENT_VIEW_BEAN, clientViewBean);
            }
        }
        
        if (clientViewBean != null) {
            action = clientViewBean.getAction();
            contentType = request.getContentType();
            clientViewBean.setSessionid(request.getSession().getId());
            //*** BEGIN: manual bind params to bean (if we arrived here via the ClientViewHelper.jspx).
            if (action == null || action.equals("")) {
                if (contentType != null && contentType.indexOf("multipart/form-data") > -1) {
                    action = "Upload";
                } else {
                    action = request.getParameter("action");
                    clientViewBean.setDocId(request.getParameter("docid"));
                    clientViewBean.setMetaFileDocId(request.getParameter("metadataDocId"));
                    clientViewBean.setQformat(request.getParameter("qformat"));
                    clientViewBean.setPublicAccess(request.getParameter("publicAccess") != null);
                    clientViewBean.setContentStandard(request.getParameter("contentStandard"));
                }
                clientViewBean.setAction(action);
            }
            //*** END: manual bind params to bean.
            
            if (action != null) {
                if (action.equals("Login")) {
                    responseMap = handleClientRequest(null);
                    //*** Now that the action has been processed, clear it.
                    clientViewBean.setAction("");
                    if (isLoggedIn()) {
//                    	HttpSession session = request.getSession(false);                 	
//                    	session.setAttribute("ClientViewHelper", this);
                    	Cookie jSessionCookie = new Cookie("JSESSIONID", clientViewBean.getSessionid());
                    	response.addCookie(jSessionCookie);
                    }
                } else if (action.equals("Logout")) {
                    responseMap = handleClientRequest(null);
                    clientViewBean.setAction("");
                } else if (action.equals("Upload")) {
                    try {
                        //*** Init the MultipartParser.
                        multipartParser = new MultipartParser(request, sizeLimit * 1024 * 1024);
                        responseMap = handleClientRequest(multipartParser);
                    } catch (IOException ex) {
                        responseMap = new HashMap<String, Object>();
                        responseMap.put("message", ex.getMessage());
                    }
                    clientViewBean.setAction("");
                } else if (action.equals("Download")) {
                    responseMap = handleClientRequest(null);
                    try {
                        handleDownloadResponse(responseMap, response);
                    } catch (IOException ex) {
                        responseMap = new HashMap<String, Object>();
                        responseMap.put("message", ex.getMessage());
                    }
                    
                } else if (action.equals("Set Access")) {
                    responseMap = handleClientRequest(null);
                    clientViewBean.setAction("");
                } else {
                    responseMap = handleClientRequest(null);
                }
                result = (String) responseMap.get("message");
            }
        } else {
            result = "ClientViewHelper.clientRequest: ClientView bean is not instantiated.";
        }
        return(result);
    }
    
    /**
     * Main method for handling various actions.
     *
     * Note: This is mostly plain java code so it is JUnit friendly
     * (pass null as the MulipartParser).
     *
     * @param multipartParser Only needed if the action is "Upload".
     * @return HashMap containing "message", and possibly several other values.  If
     * the action is Download, than this will contain all needed values
     * to pass to handleDownloadResponse.
     */
    public HashMap<String, Object> handleClientRequest(MultipartParser multipartParser)  {
        String                              result = "", serverResponse;
        String                              posted_ldapUserName, tmp, action;
        HashMap<String, Object>             responseMap = new HashMap<String, Object>();
        
        
        if (clientViewBean != null) {
            action = clientViewBean.getAction();
            if (action != null) {
                try {
                    if (action.equals("Login")) {
                        tmp = LDAP_TEMPLATE.replaceFirst("%1s", clientViewBean.getUsername());
                        posted_ldapUserName = tmp.replaceFirst("%2s", clientViewBean.getOrganization());
                        
//                        if (metacatSessionId != null) {
//                        	metacatClient.setSessionId(metacatSessionId);
//                        }                       
                        serverResponse = metacatClient.login(posted_ldapUserName, clientViewBean.getPassword());
                        setLoggedIn(serverResponse);
                        result = parseXml("message", serverResponse);
                        contactName = parseXml("name", serverResponse);
                        clientViewBean.setMessage(ClientView.LOGIN_MESSAGE, result);
                        clientViewBean.setMessage(ClientView.UPLOAD_MESSAGE, "");
                        if (isLoggedIn()) {
                            clientViewBean.setSessionid(metacatClient.getSessionId());
                            
                        }
                    } else if (action.equals("Logout")) {
                        result = metacatClient.logout();
                        setLoggedIn(result);
                        result = parseXml("message", result);
                        clientViewBean.setMessage(ClientView.LOGIN_MESSAGE, result);
                        clientViewBean.setMessage(ClientView.UPLOAD_MESSAGE, "");
                        if (!isLoggedIn()) {
                            clientViewBean.setUsername("");
                            clientViewBean.setPassword("");
                            clientViewBean.setOrganization("");
                            clientViewBean.setSessionid(null);
                        }
                    } else if (action.equals("Delete")) {
                        ClientFgdcHelper.clientDeleteRequest(clientViewBean, this);
                        clientViewBean.setAction("read"); //*** Set for re-query.
                        //*** Note: the clientViewBean will already have the updated Meta Doc Id.
                        
                    } else if (action.equals("Upload")) {
                        //*** Only process request if logged in.
                        if (isLoggedIn()) {
                            if (multipartParser == null)
                                result = "ClientViewHelper.handleClientRequest: MultipartParser is not instantiated.";
                            else
                                result = handlePackageUpload(clientViewBean, multipartParser);
                        } else {
                            result = "You must be logged in to perform an upload.";
                        }
                        clientViewBean.setMessage(ClientView.UPLOAD_MESSAGE, result);
                    } else if (action.equals("Update")) {
                        result = "This is not implemented here.  Call ClientViewHelper.jspx";
                    } else if (action.equals("Scope")) {
                        result = handleDocIdSelect();
                        clientViewBean.setMessage(ClientView.SELECT_MESSAGE, result);
                    } else if (action.equals("Download")) {
                        responseMap = download(clientViewBean);
                    } else if (action.equals("Set Access")) {
                        result = handleChangeAccess(clientViewBean.getMetaFileDocId(),
                                (clientViewBean.isPublicAccess()? PERMISSION_TYPE_ALLOW: PERMISSION_TYPE_DISALLOW));
                        clientViewBean.setMessage(ClientView.UPDATE_MESSAGE, result);
                    } else {
                        result = action + " action not recognized.";
                    }
                } catch (Exception ex) {
                    result = ex.getMessage();
                    clientViewBean.setMessage(ClientView.ERROR_MESSAGE, result);
                    ex.printStackTrace();
                }
            }
        } else {
            result = "ClientViewHelper.clientRequest: ClientView bean is not instantiated.";
        }
        responseMap.put("message", result);
        return(responseMap);
    }
    
    /**
     * This is a convenience method to reduce the amount of code in a Metacat Client.
     * It handles creating/reusing (per session) an instance of a ClientViewHelper.
     * @param request Since this is intended to be used by an Http client, it is passed the
     * available "request" variable (the HttpServletRequest).
     * @throws edu.ucsb.nceas.metacat.client.MetacatInaccessibleException Received by MetacatFactory.
     * @return ClientViewHelper instance.
     */
    public static ClientViewHelper clientViewHelperInstance(HttpServletRequest request) {
        ClientViewHelper result;
        
        String sessionId = request.getSession(false).getId();
        
        result = (ClientViewHelper) request.getSession().getAttribute("ClientViewHelper");
        if (result == null) {
            try {
                result = new ClientViewHelper(request);
                request.getSession().setAttribute("ClientViewHelper", result);
            } catch (MetacatInaccessibleException ex) {
                ex.printStackTrace();
            }
        }
        
        if (result.clientViewBean == null) {
        	result.clientViewBean = (ClientView) request.getSession().getAttribute(ClientView.CLIENT_VIEW_BEAN);
            
            if (result.clientViewBean == null) {
            	//make a new one and shove it in the session
            	result.clientViewBean = new ClientView();
            	request.getSession().setAttribute(ClientView.CLIENT_VIEW_BEAN, result.clientViewBean);
            }
        }
        
        boolean oldLoginValue = result.loggedIn;
        result.setLoggedIn(SessionService.getInstance().validateSession(sessionId));
        if (result.isLoggedIn()) {
        	SessionData sessionData = SessionService.getInstance().getRegisteredSession(sessionId);
        	result.setUserName(sessionData.getName());
        }
        
        if (!oldLoginValue || result.loggedIn) {
        	result.clientViewBean.setMessage(ClientView.UPLOAD_MESSAGE, "");
        }
        
        return(result);
    }
    
    /**
     * A convenience method to be used by client code that requires
     * the user to be logged in.  NOTE: setUser() must have been called first,
     * otherwise it will always return false.
     * @return boolean  true if user has logged in for this session, false otherwise.
     */
    public boolean isLoggedIn() {
        return(loggedIn);
    }
    
    /**
     * After calling "login(ldapUserName, pwd)", call this with the username
     * and servers response message.  You can than use isLoggedIn() to determine if
     * the user is logged in, getLoginResponseElement(), etc.  The user name will also
     * used by calls to doMetadataUpload() for Document Id creation (scope).
     * @param userName User name
     * @param serverResponse XML login response sent from Metacat.
     */
    public void setLoggedIn(String serverResponse) {
        loggedIn = (serverResponse != null && serverResponse.indexOf("login") > -1);
    }
    
    public void setLoggedIn(boolean isLoggedIn) {
        this.loggedIn = isLoggedIn;
    }
    
    public void setUserName(String userName) {
        clientViewBean.setUsername(userName);
    }
    
    public String parseXml(String elementName, String xml) {
        String                      result = null;
        Document                    doc;
        
        try {
            doc = XMLUtilities.getXMLReaderAsDOMDocument(new StringReader(xml));
            result = (String) xpath.evaluate(elementName, doc.getDocumentElement(), XPathConstants.STRING);
            if (result != null)
                result = result.trim();
        } catch (IOException ex) {
            ex.printStackTrace();
        } catch (XPathExpressionException ex) {
            ex.printStackTrace();
        }
        return(result);
    }
    
    public String handleDocIdSelect() {
        String                              result = "";
        TreeMap                             allDocIds;
        
        if (!clientViewBean.getPathValue().equals("")) {
            allDocIds = getSelectQueryMap();
            result = ClientHtmlHelper.mapToHtmlSelect(allDocIds, "docId", "width: 240", 10);
        }
        return(result);
    }
    
    /**
     * Handles metadata file and data file uploads for inserting new
     * Metacat data packages.  Note: if content type is not "multipart/form-data",
     * nothing will happen.
     * @param request HTTP request.
     * @return A 1-line status message for the user.
     */
    public String handlePackageUpload(ClientView clientViewBean, MultipartParser multipartParser) throws Exception {
        String                      result = "", contentType, formatType;
        String                      lastDocId, nextDocId, metaDocId, metaFileName;
        String                      fileInfo[];
        Reader                      reader;
        int                         sizeLimit, idx;
        InputStream                 inputStream;
        HashMap                     paramsMap, dataDocIDs;
        StringBuffer                fileName;
        boolean                     sendIt;
        Iterator                    iterIt;
        Stack                       docIdStack;
        
        //*** Get the First file, which should be the metadata file.
        paramsMap = new HashMap();
        fileName = new StringBuffer();
        inputStream = getNextInputStream(multipartParser, fileName, paramsMap);
        metaFileName = fileName.toString();
        if (metaFileName.toLowerCase().endsWith(".xml")) {
            //*** Keep it here for updating.
            setMetadataDoc(inputStream);
            //*** Get the Metadata File's DOC ID.
            String scope = clientViewBean.getUsername();
            scope = scope.replaceAll(" ", "_");
            scope = scope.toLowerCase();
            lastDocId = getMetacatClient().getLastDocid(scope);
            metaDocId = lastDocId = nextDocId(lastDocId, scope);
            
            //*** Loop thru all of the data files, get fileName and inputStream.
            dataDocIDs = new HashMap();
            fileName = new StringBuffer();
            while ((inputStream = getNextInputStream(multipartParser, fileName, paramsMap)) != null) {
                //*** Get the data file's DOC ID.
                nextDocId = nextDocId(lastDocId, scope);
                
                fileInfo = parseFileInfo(fileName.toString());
                dataDocIDs.put(nextDocId, fileInfo);
                
                //*** Upload the data file to metacat.
                getMetacatClient().upload(nextDocId, fileName.toString(), inputStream, DEFAULTFILESIZE);
                
                lastDocId = nextDocId;
                fileName = new StringBuffer();
            }
            
            if (ClientFgdcHelper.isFGDC(getMetadataDoc())) {
                sendIt = ClientFgdcHelper.handlePackageUpload(metaDocId, dataDocIDs, contactName, metaFileName, getMetadataDoc());
            } else {
                //TODO add other types of metadata grammars here...
                System.out.println("ClientViewHelper.handlePackageUpload: not an FGDC file = " + fileName);
                result = fileName + " is not an FGDC file.  Files not uploaded.";
                sendIt = false;
            }
            
            if (sendIt) {
                //*** Upload the metadata file to metacat.
                reader = XMLUtilities.getDOMTreeAsReader(metadataDoc.getDocumentElement(), false);
                getMetacatClient().insert(metaDocId, reader, null);
                
                result = "MetaCat Package Inserted:  the Document Identifier is " + metaDocId;
                reader.close();
                //*** Grant the public read access to the meta file.
                if (paramsMap.containsKey("publicAccess")) {
                    docIdStack = new Stack();
                    docIdStack.addAll(dataDocIDs.keySet());
                    setPublicAccess(this.PERMISSION_TYPE_ALLOW, metaDocId, docIdStack);
                }
            }
        } else {
            result = "The first file must be an XML Metadata file.  Files not uploaded.";
        }
        if (inputStream != null)
            inputStream.close();
        return(result);
    }
    
    private String setPublicAccess(String permissionType, String metaDocId, Stack docIdStack)
    throws InsufficientKarmaException, MetacatException, MetacatInaccessibleException, AccessControlException, McdbDocNotFoundException{
        String                      result = " for Documents ";
        String                      docId, lst = metaDocId, permOrder;
        
        /*if (permissionType.equals("allow"))
            permOrder = "denyFirst";
        else
            permOrder = "allowFirst";
        permOrder = "allowFirst";
        
        getMetacatClient().setAccess(metaDocId, "public", "read", permissionType, permOrder);*/
        setPublicReadAccess(permissionType, metaDocId) ;
        //*** Grant the public read access to the data files.
        while(!docIdStack.isEmpty()) {
            docId = (String) docIdStack.pop();
            //getMetacatClient().setAccess(docId, "public", "read", permissionType, permOrder);
            setPublicReadAccess(permissionType, docId);
            lst += ", " + docId;
        }
        result = "Changed public read access to '" + permissionType + "' for " + result + lst;
        return(result);
    }
    
    /*
     * Set up public access by using accessblock to replace old success rules.
     * First, we need to get original access rules.
     * Second, remove all access rules of the user public, then add the new rules. 
     */
    private void setPublicReadAccess(String permissionType, String docid) 
        throws InsufficientKarmaException, MetacatException, MetacatInaccessibleException, AccessControlException, McdbDocNotFoundException{
      String originalAccessBlock = getMetacatClient().getAccessControl(docid);
      Vector<XMLAccessDAO> accessList = parseAccessXMLBlock(docid, originalAccessBlock);
      if(accessList != null  && !accessList.isEmpty()) {
        //there are some access rules in the metacat server for this docid
        XMLAccessDAO rule = accessList.elementAt(0);
        //we should persist the perm order from original access block
        String permOrder = rule.getPermOrder();
        //remove all public access rule but preserver other access rules.
        removeAllPublicAccessRules(accessList);
        //generate a new access rule and add it to the list
        XMLAccessDAO newRule = generateXMLAccessDAO(docid, AccessControlInterface.PUBLIC,
            AccessControlInterface.READSTRING, permissionType, permOrder);
        accessList.add(newRule);
      } else {
        //generate a new access rule and add it to the list
        accessList = new Vector<XMLAccessDAO>();
        XMLAccessDAO newRule = generateXMLAccessDAO(docid, AccessControlInterface.PUBLIC,
            AccessControlInterface.READSTRING, permissionType, AccessControlInterface.ALLOWFIRST);
        accessList.add(newRule);
      }
      //transform the new XMLAccessDAO object list to the String 
      AccessControlForSingleFile controller = new AccessControlForSingleFile(docid);
      String accessBlock = controller.getAccessString(accessList);
      //send the access block to the metacat
      getMetacatClient().setAccess(docid, accessBlock);
    }
    
    /*
     * Parse the access xml block to get access rule list object.
     */
    private Vector<XMLAccessDAO> parseAccessXMLBlock(String docId, String accessXMLBlock) throws AccessControlException{
      try { 
        // use DocInfoHandler to parse the access section into DAO objects
        XMLReader parser = null;
        DocInfoHandler docInfoHandler = new DocInfoHandler(docId); 
        ContentHandler chandler = docInfoHandler;

        // Get an instance of the parser
        String parserName = PropertyService.getProperty("xml.saxparser");
        parser = XMLReaderFactory.createXMLReader(parserName);

        // Turn off validation
        parser.setFeature("http://xml.org/sax/features/validation", false);
        parser.setContentHandler((ContentHandler)chandler);
        parser.setErrorHandler((ErrorHandler)chandler);

        parser.parse(new InputSource(new StringReader(accessXMLBlock)));
        
        XMLAccessAccess xmlAccessAccess = new XMLAccessAccess();
         Vector<XMLAccessDAO> accessRuleList = docInfoHandler.getAccessControlList();
         return accessRuleList;
           

      } catch (PropertyNotFoundException pnfe) {
        throw new AccessControlException("ClientViewHelper.parseAccessXMLBlock - "
            + "property error when replacing permissions: " + pnfe.getMessage());
      } catch (AccessException ae) {
        throw new AccessControlException("ClientViewHelper.parseAccessXMLBlock - "
            + "DB access error when replacing permissions: " + ae.getMessage());
      } catch (SAXException se) {
        throw new AccessControlException("ClientViewHelper.parseAccessXMLBlock - "
            + "SAX error when replacing permissions: " + se.getMessage());
      } catch(IOException ioe) {
        throw new AccessControlException("ClientViewHelper.parseAccessXMLBlock - "
            + "I/O error when replacing permissions: " + ioe.getMessage());
      }
    }
    
    /*
     * Populate xmlAccessDAO object with the some parameters.
     */
    private XMLAccessDAO generateXMLAccessDAO(String docid, String principalName,
        String permission, String permType, String permOrder) throws McdbDocNotFoundException {
      String localId = DocumentUtil.getDocIdFromString(docid);
      int rev = DocumentUtil.getRevisionFromAccessionNumber(docid);
      String identifier = IdentifierManager.getInstance().getGUID(localId, rev);
      XMLAccessDAO xmlAccessDAO = new XMLAccessDAO();
      xmlAccessDAO.setGuid(identifier);
      xmlAccessDAO.setPrincipalName(principalName);
      xmlAccessDAO.setPermission(Integer.valueOf(AccessControlList.intValue(permission)).longValue());
      xmlAccessDAO.setPermType(permType);
      xmlAccessDAO.setPermOrder(permOrder);
      return xmlAccessDAO;
    }
    
    
    /*
     * Remove every access rule for user public from the specified access rule list
     */
    private void removeAllPublicAccessRules(Vector<XMLAccessDAO> accessList) {
      if(accessList != null && !accessList.isEmpty()) {
        Vector<Integer> removingIndexList = new Vector<Integer>();
        for(int i=0; i<accessList.size(); i++) {
          XMLAccessDAO rule = accessList.elementAt(i);
          if(rule != null && rule.getPrincipalName() != null && rule.getPrincipalName().equalsIgnoreCase(AccessControlInterface.PUBLIC)) {
            //store the index which should be remove
            removingIndexList.add(new Integer(i));
          }      
        }
        if(!removingIndexList.isEmpty()) {
          for(int i=removingIndexList.size()-1; i>=0; i--) {
            accessList.remove(removingIndexList.elementAt(i).intValue());
          }
       }
      }
    }
    
    private String handleChangeAccess(String metaDocId, String permissionType) throws Exception {
        Stack                       dataDocIDs;
        String                      result = "", xpathExpr = null;
        
        setMetadataDoc(metaDocId);
        //*** Build list of sub-documents.
        if (clientViewBean.getContentStandard().equals(ClientView.FEDERAL_GEOGRAPHIC_DATA_COMMITTEE)) {
            xpathExpr = ClientFgdcHelper.SUB_DOCS_PATH;
        } else if (clientViewBean.getContentStandard().equals(ClientView.ECOLOGICAL_METADATA_LANGUAGE)) {
            xpathExpr = null; //TODO  - EML
        }
        if (xpathExpr != null) {
            dataDocIDs = getNodeTextStack(xpath, xpathExpr, getMetadataDoc().getDocumentElement());
            result = setPublicAccess(permissionType, metaDocId, dataDocIDs);
        }
        return(result);
    }
    
    public String handleFileUpdate(MultipartParser multipartParser) throws Exception {
        String                      result = "", fNm, action, lastDocId, newDocId, xPathQuery, qFrmt;
        InputStream                 inputStream;
        HashMap                     paramsMap;
        StringBuffer                fileName;
        Iterator                    iterIt;
        boolean                     sendIt;
        String                      metadataDocId, fileInfo[];
        
        paramsMap = new HashMap();
        fileName = new StringBuffer();
        if ((inputStream = getNextInputStream(multipartParser, fileName, paramsMap)) != null) {
            action = (String) paramsMap.get("action");
            //*** Get the Doc Id.
            lastDocId = (String) paramsMap.get("docid");
            
            //*** Get the metadata Doc Id.
            metadataDocId = (String) paramsMap.get("metadataDocId");
            clientViewBean.setMetaFileDocId(metadataDocId);
            
            //*** Get the qformat.
            qFrmt = (String) paramsMap.get("qformat");
            clientViewBean.setQformat(qFrmt);
            
            fNm = fileName.toString();
            
            try {
                if (lastDocId.equals(metadataDocId)) { //*** This is the metadata file.
                    //*** Keep it here for updating.
                    setMetadataDoc(inputStream);
                    if (ClientFgdcHelper.isFGDC(getMetadataDoc())) {
                        clientViewBean.setContentStandard(ClientView.FEDERAL_GEOGRAPHIC_DATA_COMMITTEE);
                        if (!ClientFgdcHelper.hasMetacatInfo(lastDocId, getMetadataDoc())) {
                            
                            //*** Save the Doc Id for re-query.
                            clientViewBean.setMetaFileDocId(lastDocId);
                            clientViewBean.setAction("read"); //*** Set for re-query.
                            result = "Update not performed: the Metadata file has no prior Metacat info in it.";
                        } else {
                            xPathQuery = ClientFgdcHelper.XPATH_QUERY_TEMPLATE.replaceFirst("%1s", lastDocId);
                            newDocId = updateMetadataDoc(lastDocId, xPathQuery, fNm);
                            
                            //*** Save the Doc Id for re-query.
                            clientViewBean.setMetaFileDocId(newDocId);
                            clientViewBean.setAction("read"); //*** Set for re-query.
                            result = "Updated to new document (from " + lastDocId + " to " + newDocId + ")";
                        }
                    } else {
                        //***TODO This is EML.
                        clientViewBean.setContentStandard(ClientView.ECOLOGICAL_METADATA_LANGUAGE);
                        
                        //*** Save the Doc Id for re-query.
                        clientViewBean.setMetaFileDocId(lastDocId);
                        clientViewBean.setAction("read"); //*** Set for re-query.
                        result = "Currently this functionality only supports FGDC metadata.";
                    }
                } else {
                    //*** This is a data file.
                    //*** Query for the metadata, we need to update it with the new data file doc id.
                    setMetadataDoc(metadataDocId);
                    
                    if (ClientFgdcHelper.isFGDC(getMetadataDoc())) {
                        clientViewBean.setContentStandard(ClientView.FEDERAL_GEOGRAPHIC_DATA_COMMITTEE);
                        fileInfo = parseFileInfo(fNm);
                        
                        xPathQuery = ClientFgdcHelper.FGDC_DATA_FILE_QUERY_XPATH.replaceFirst("%1s", lastDocId);
                        newDocId = nextVersion(lastDocId, xPathQuery);
                        ClientFgdcHelper.updateFileNameAndType(getMetadataDoc().getDocumentElement(), newDocId, fileInfo);
                        //*** Upload the data file to metacat.
                        getMetacatClient().upload(newDocId, fNm, inputStream, DEFAULTFILESIZE);
                        result = "Updated to new document (from " + lastDocId + " to " + newDocId + ")";
                        
                        //*** Upload the metadata file to metacat.
                        xPathQuery = ClientFgdcHelper.XPATH_QUERY_TEMPLATE.replaceFirst("%1s", metadataDocId);
                        newDocId = updateMetadataDoc(metadataDocId, xPathQuery, null);
                        
                        //*** Save the new meta Doc Id for re-query.
                        clientViewBean.setMetaFileDocId(newDocId);
                        clientViewBean.setAction("read"); //*** Set for re-query.
                        
                    } else {
                        //***TODO This is EML.
                        clientViewBean.setContentStandard(ClientView.ECOLOGICAL_METADATA_LANGUAGE);
                        
                        //*** Save the old meta Doc Id for re-query.
                        clientViewBean.setMetaFileDocId(metadataDocId);
                        clientViewBean.setAction("read"); //*** Set for re-query.
                        result = "Currently this functionality only supports FGDC metadata.";
                    }
                }
            } catch (java.io.IOException ex) {
                ex.printStackTrace();
            }
        } else {
            result = "Please enter the updated file path/name.";
        }
        clientViewBean.setMessage(ClientView.UPDATE_MESSAGE, result);
        return(result);
    }
    
    private String updateMetadataDoc(String lastDocId, String docIdPath, String origFileName) {
        String                      newDocId = null;
        Reader                      reader;
        
        //*** Update the metadata with the new Doc Id version.
        try {
            newDocId = nextVersion(lastDocId, docIdPath);
            if (origFileName != null) {
                if (clientViewBean.getContentStandard().equals(ClientView.FEDERAL_GEOGRAPHIC_DATA_COMMITTEE))
                    ClientFgdcHelper.updateMetadataFileName(getMetadataDoc().getDocumentElement(), newDocId, origFileName);
                else
                    ; //TODO EML, etc.
            }
            //*** Upload the metadata file to metacat.
            reader = XMLUtilities.getDOMTreeAsReader(getMetadataDoc().getDocumentElement(), false);
            getMetacatClient().update(newDocId, reader, null);
            reader.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return(newDocId);
    }
    
    private InputStream getNextInputStream(MultipartParser multipartParser, StringBuffer fileName, HashMap paramsMap)
    throws IOException {
        InputStream                     result = null;
        Part                            part;
        String                          parmName = null, value = null, fnam;
        
        while ((part = multipartParser.readNextPart()) != null) {
            if (part.isParam()) {
                parmName = part.getName();
                value = ((ParamPart) part).getStringValue();
                paramsMap.put(parmName, value);
                
            } else if (part.isFile()) {
                fnam = ((FilePart) part).getFileName();
                if (fnam != null && !fnam.equals("")) {
                    //*** File name is passed back via StringBuffer fileName param.
                    fileName.append(fnam);
                    result = ((FilePart) part).getInputStream();
                    break;
                }
            }
        }
        return(result);
    }
    
    private void getRemainingParameters(MultipartParser multipartParser, HashMap paramsMap)
    throws IOException {
        InputStream                     result = null;
        Part                            part;
        String                          parmName = null, value = null, fnam;
        
        while ((part = multipartParser.readNextPart()) != null) {
            if (part.isParam()) {
                parmName = part.getName();
                value = ((ParamPart) part).getStringValue();
                paramsMap.put(parmName, value);
            }
        }
    }
    
    /**
     * Queries Metacat for document listings, and returns the results in a TreeMap,
     * where the key is the Doc Id, and the value is the Create Date.  If the document
     * contains the specified 'returnfield', an addtional entry will be created with
     * the value being a Vector of sub-DocId's.  The key of this entry will be the
     * original DocId with some addtional text added.
     * Reads bean properties 'pathExpr' (String[]), 'pathValue' (String)
     * and 'returnfield' (String).
     * @return TreeMap
     */
    public TreeMap getSelectQueryMap() {
        TreeMap                         result;
        Document                        doc;
        NodeList                        nodeLst, subNodeLst;
        Node                            node, subNode;
        String                          key, val, paramExpr, paramVal;
        String                          value, returnFld;
        String                          path;
        Vector                          optGroup;
        final String                    DOCID_EXPR = "./docid";
        final String                    DOCNAME_EXPR = "./createdate";
        final String                    PARAM_EXPR = "./param[@name='%1s']";
        
        path = clientViewBean.getPathExpr();
        returnFld = clientViewBean.getReturnfield();
        value = clientViewBean.getPathValue();
        
        result = new TreeMap();
        //paramExpr = String.format(PARAM_EXPR, returnFld);
        paramExpr = PARAM_EXPR.replaceFirst("%1s", returnFld);
        //*** Query the database ***
        doc = query(path, value, returnFld);
        //*** Build the TreeMap to return ***
        try {
            nodeLst = (NodeList) xpath.evaluate("/resultset/document", doc, XPathConstants.NODESET);
            for (int i = 0; i < nodeLst.getLength(); i++) {
                node = nodeLst.item(i);
                key = xpath.evaluate(DOCID_EXPR, node);
                val = xpath.evaluate(DOCNAME_EXPR, node);
                result.put(key, key + " (" + val + ")");
                
                //*** returnfield values ***
                subNodeLst = (NodeList) xpath.evaluate(paramExpr, node, XPathConstants.NODESET);
                if (subNodeLst.getLength() > 0) {
                    optGroup = new Vector();
                    for (int k = 0; k < subNodeLst.getLength(); k++) {
                        subNode =  subNodeLst.item(k);
                        paramVal = xpath.evaluate("text()", subNode);
                        optGroup.add(paramVal);
                    }
                    result.put(key + " Data Files", optGroup);
                }
                
            }
        } catch (XPathExpressionException ex) {
            ex.printStackTrace();
        }
        return(result);
    }
    
    /**
     * Query metacat for documents that 'CONTAINS' the value at the specified XPath
     * expression.  Additionally, returns another non-standard field value.
     * Standard info contains: DocId, DocName, DocType, CreateDate, and UpdateDate.
     * @param pathExpr String contianing an XPath expression.
     * @param pathValue String containing a comparison value at the XPath expression.
     * @param returnFld String containing an XPath expression to a field which will be returned
     * in addition to the standard info.
     * @return DOM Document containing the results.
     */
    public Document query(String pathExpr, String pathValue, String returnFld) {
        Document                        result = null;
        InputStream                     response;
        BufferedReader                  buffy;
        Properties                      prop;
        
        try {
            prop = new Properties();
            prop.put("action", "query");
            prop.put("qformat", "xml");
            prop.put(pathExpr, pathValue);
            if (returnFld != null) {
                prop.put("returnfield", returnFld);
            }
            
            response = metacatClient.sendParameters(prop);
            if (response != null) {
                buffy = new BufferedReader(new InputStreamReader(response));
                result = XMLUtilities.getXMLReaderAsDOMDocument(buffy);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return(result);
    }
    
    public void setMetadataDoc(Document doc) {
        metadataDoc = doc;
    }
    
    public void setMetadataDoc(String docId) throws Exception {
        Document                        doc = null;
        BufferedReader                  buffy;
        InputStream                     response;

        response = metacatClient.read(docId);
        if (response != null) {
            buffy = new BufferedReader(new InputStreamReader(response));
            doc = XMLUtilities.getXMLReaderAsDOMDocument(buffy);
            response.close();
        }
        setMetadataDoc(doc);
    }
    
    public void setMetadataDoc(InputStream ioStream) throws IOException {
        BufferedReader                          buffy;
        
        if (ioStream != null) {
            buffy = new BufferedReader(new InputStreamReader(ioStream));
            metadataDoc = XMLUtilities.getXMLReaderAsDOMDocument(buffy);
        }
    }
    
    public Document getMetadataDoc() {
        return(metadataDoc);
    }
    
    public String nextVersion(String lastDocId, String xPathQuery) throws XPathExpressionException {
        String                      result = null, tokens[], scope, ready2Split, tmp;
        int                         vers, docNum;
        final int                   LAST_TOKEN = 2;
        final String                TEMPLATE = "%1s.%2d.%3d";
        Node                        node;
        
        //*** Parse the last Doc Id, and increment the version number.
        if(lastDocId != null && lastDocId.indexOf(".") > -1) {
            ready2Split = lastDocId.replace('.','~'); //*** This is necessary for the split to work.
            tokens = ready2Split.split("~");
            if(tokens.length > LAST_TOKEN && !tokens[LAST_TOKEN].equals("")) {
                scope = tokens[LAST_TOKEN - 2];
                docNum = Integer.parseInt(tokens[LAST_TOKEN - 1]);
                try {
                    vers = Integer.parseInt(tokens[LAST_TOKEN]);
                    //result = String.format(TEMPLATE, scope, docNum, 1 + vers);
                    tmp = TEMPLATE.replaceFirst("%1s", scope);
                    tmp = tmp.replaceFirst("%2d", String.valueOf(docNum));
                    result = tmp.replaceFirst("%3d", String.valueOf(vers + 1));
                    
                } catch (NumberFormatException ex) {
                    //*** In case the lastDocId has something other than a number.
                    //result = String.format(TEMPLATE, scope, docNum, 1);
                    tmp = TEMPLATE.replaceFirst("%1s", scope);
                    tmp = tmp.replaceFirst("%2d", String.valueOf(docNum));
                    result = tmp.replaceFirst("%3d", "1");
                }
            } else {
                //*** In case the lastDocId ends with a '.'
                result = lastDocId + "1";
            }
        } else {
            //*** In case of missing doc Id.
            result = null;
        }
        //*** Update the Doc Id in the metadata file.
        if (getMetadataDoc() != null) {
            node = (Node) xpath.evaluate(xPathQuery, getMetadataDoc().getDocumentElement(), XPathConstants.NODE);
            setTextContent(xpath, node, result);
        }
        return(result);
    }
    
    private String nextDocId(String lastDocId, String scope) {
        String                      result = null, tokens[], tmp;
        int                         vers;
        String                      template = scope.toLowerCase() + ".%1d.%2d";
        
        if(lastDocId != null && lastDocId.indexOf(".") > -1) {
            lastDocId = lastDocId.replace('.','~'); //*** This is necessary for the split to work.
            tokens = lastDocId.split("~");
            if(tokens.length > 1 && !tokens[1].equals("")) {
                try {
                    vers = Integer.parseInt(tokens[1]);
                    //result = String.format(template, 1 + vers, 1);
                    tmp = template.replaceFirst("%1d", String.valueOf(1 + vers));
                    result = tmp.replaceFirst("%2d", "1");
                } catch (NumberFormatException ex) {
                    //*** In case the lastDocId has something other than a number.
                    //result = String.format(template, 1, 1);
                    tmp = template.replaceFirst("%1d", "1");
                    result = tmp.replaceFirst("%2d", "1");
                }
            } else {
                //*** In case the lastDocId ends with a '.'
                //result = String.format(template, 1, 1);
                tmp = template.replaceFirst("%1d", "1");
                result = tmp.replaceFirst("%2d", "1");
            }
        } else {
            //*** In case there isn't any doc Id's with the user name.
            //result = String.format(template, 1, 1);
            tmp = template.replaceFirst("%1d", "1");
            result = tmp.replaceFirst("%2d", "1");
        }
        return(result);
    }
    
    public MetacatClient getMetacatClient() {
        return(metacatClient);
    }
    
    //*** BEGIN: Static utility methods ***
    
    public static String[] parseFileInfo(String fileName) {
        String[]                        result = new String[2];
        int                             idx;
        String                          formatType;
        
        //*** Set the file format (just using file extension for now).
        idx = fileName.lastIndexOf(".");
        if (idx > 1)
            formatType = fileName.substring(idx+1).toUpperCase();
        else
            formatType = "";
        
        result[ClientView.FORMAT_TYPE] = formatType;
        result[ClientView.FILE_NAME] = fileName.toString();
        return(result);
    }
    
    public static void updateNodeText(Node root, XPath xPath, String expression, String text) {
        Node                    targetNode;
        
        if (text != null && !text.equals("")) {
            try {
                targetNode = (Node) xPath.evaluate(expression, root, XPathConstants.NODE);
                setTextContent(xPath, targetNode, text);
                //targetNode.setTextContent(text);
            } catch (XPathExpressionException ex) {
                ex.printStackTrace();
            }
        }
    }
    
    
    public static Node getNode(XPath xPath, String expression, Node root) {
        Node                        result = null;
        
        try {
            result = (Node) xPath.evaluate(expression, root, XPathConstants.NODE);
        } catch (XPathExpressionException ex) {
            ex.printStackTrace();
        }
        return(result);
        
    }
    
    public static String getNodeText(XPath xPath, String expression, Node root) {
        Node                        node;
        String                      result = null;
        
        node = getNode(xPath, expression, root);
        if (node != null && !node.equals(""))
            result = getTextContent(xPath, node);
        //result = node.getTextContent(); Not in java 1.4
        return(result);
    }
    
    public static String[] getNodeTextList(XPath xPath, String expression, Node root) {
        NodeList                    nodes;
        String                      result[] = new String[0];
        int                         size;
        
        try {
            nodes = (NodeList) xPath.evaluate(expression, root, XPathConstants.NODESET);
            if (nodes != null && (size = nodes.getLength()) > 0) {
                result = new String[size];
                for(int i = 0; i < size; i++)
                    result[i] = getTextContent(xPath, nodes.item(i));
                //result[i] = nodes.item(i).getTextContent(); Not in java 1.4
            }
        } catch (XPathExpressionException ex) {
            ex.printStackTrace();
        }
        return(result);
    }
    
    public static Stack getNodeTextStack(XPath xpathInstance, String xpathExpr, Node parentNode) {
        String                      nodeLst[];
        Stack                       result = new Stack();
        
        nodeLst = getNodeTextList(xpathInstance, xpathExpr, parentNode);
        for(int i = 0; i < nodeLst.length; i++)
            result.push(nodeLst[i]);
        return(result);
    }
    
    public static String getStringFromInputStream(InputStream input) {
        StringBuffer result = new StringBuffer();
        BufferedReader in = new BufferedReader(new InputStreamReader(input));
        String line;
        try {
            while ((line = in.readLine()) != null) {
                result.append(line);
            }
        } catch (IOException e) {
            System.out.println("ClientViewHelper.getStringFromInputStream: " + e);
        }
        return result.toString();
    }
    
    //*** END: Static utility methods ***
    
    public String makeRedirectUrl() {
        String                      result, docId, message;
        
        docId = clientViewBean.getMetaFileDocId();
        //System.out.println("get the the session id "+clientViewBean.getSessionid()+ " from the client view bean object "+clientViewBean.toString());
        if (clientViewBean.getAction().equals(DOWNLOAD_ACTION)) {
            result = null;
        } else if (docId != null && !docId.equals("")) {
            message = clientViewBean.getMessage(ClientView.UPDATE_MESSAGE);
            result = "metacat?action=read&qformat=" +clientViewBean.getQformat()
            + "&docid=" + docId + "&sessionid=" + clientViewBean.getSessionid() + "&message=" + message;
        } else {
            result = "style/skins/" + clientViewBean.getQformat() + "/confirm.jsp";
        }
        //*** Reset bean action property.
        clientViewBean.setAction("");
        return(result);
    }
    
    private HashMap<String, Object> download(ClientView bean) {
        Properties                      args;
        InputStream                     inStream;
        String                          docId, metaId, fNm = null, pth, txtLst[];
        String                          msg = "File '~' (~) downloaded";
        Node                            branchRoot, metaRoot;
        ByteArrayOutputStream           outStream;
        int                             intMe;
        HashMap<String, Object>         responseMap = new HashMap<String, Object>();
        
        docId = bean.getDocId();
        metaId = bean.getMetaFileDocId();
        if (docId != null && metaId != null && !docId.equals("") && !metaId.equals("")) {
            //*** Properties args: key=param_value, value=param_name.
            args = new Properties();
            args.put("read", "action");
            try {
                //*** First, retrieve the metadata and get the original filename.
                //*** Also, if this is the metadata, get a list of docId's for the package.
                setMetadataDoc(metaId);
                metaRoot = getMetadataDoc().getDocumentElement();
                if (ClientFgdcHelper.isFGDC(getMetadataDoc())) {
                    //*** FGDC
                    if (docId.equals(metaId)) { //*** This is the metadata file.
                        pth = ClientFgdcHelper.FGDC_DOCID_ROOT_XPATH.replaceFirst("%1s", docId);
                        branchRoot = getNode(xpath, pth, getMetadataDoc());
                        fNm = getNodeText(xpath, ClientFgdcHelper.FGDC_FILE_NAME_XPATH, branchRoot);
                        //include the filename for the docid
                        args.put(fNm, docId);
                        fNm = toZipFileName(fNm);
                        responseMap.put("contentType", "application/zip");
                        //*** Get the list of docId's for the entire package.
                        args.put(metaId, "docid");
                        txtLst = getNodeTextList(xpath, ClientFgdcHelper.FGDC_DATA_FILE_NODES_XPATH, branchRoot);
                        for (int i = 0; i < txtLst.length; i++) {
                        	String additionalDocId = txtLst[i];
                        	if (additionalDocId != null && additionalDocId.length() > 1) {
                        		//look up the filename from the metadata
                        		String tempPath = ClientFgdcHelper.PATH4ANCESTOR.replaceFirst("%1s", additionalDocId);
                        		tempPath = tempPath.replaceFirst("%2s", "digform");
                                Node tempBranchRoot = getNode(xpath, tempPath, getMetadataDoc());
                                String tempFileName = getNodeText(xpath, ClientFgdcHelper.FGDC_DATA_FILE_NAME_XPATH, tempBranchRoot);
                                //include the docid
                        		args.put(additionalDocId, "docid");
                        		//include the filename for the docid
                        		args.put(tempFileName, additionalDocId);
                        	}
                        }
                        args.put("zip", "qformat");
                    } else { //*** This is a data file.
                        pth = ClientFgdcHelper.PATH4ANCESTOR.replaceFirst("%1s", docId);
                        pth = pth.replaceFirst("%2s", "digform");
                        branchRoot = getNode(xpath, pth, getMetadataDoc());
                        fNm = getNodeText(xpath, ClientFgdcHelper.FGDC_DATA_FILE_NAME_XPATH, branchRoot);
                        responseMap.put("contentType", "application/octet-stream");
                        args.put(docId, "docid");
                        args.put("xml", "qformat");
                    }
                } else {
                    //*** TODO: EML -  this is just some basic code to start with.
                    if (docId.equals(metaId)) {
                        fNm = "emlMetadata.xml";
                        txtLst = new String[] {docId};
                        args.put(txtLst[0], "docid");
                        args.put("zip", "qformat");
                        responseMap.put("contentType", "application/zip");
                    } else {
                        fNm = "emlData.dat";
                        args.put("xml", "qformat");
                        args.put(docId, "docid");
                        responseMap.put("contentType", "application/octet-stream");
                    }
                }
                
                //*** Set the filename in the response.
                responseMap.put("Content-Disposition", "attachment; filename=" + fNm);
                
                //*** Next, read the file from metacat.
                inStream = metacatClient.sendParametersInverted(args);
                
                //*** Then, convert the input stream into an output stream.
                outStream = new ByteArrayOutputStream();
                while ((intMe = inStream.read()) != -1) {
                    outStream.write(intMe);
                }
                
                //*** Now, write the output stream to the response.
                responseMap.put("outputStream", outStream);
                
                //*** Finally, set the message for the user interface to display.
                msg = msg.replaceFirst("~", fNm);
                msg = msg.replaceFirst("~", docId);
                bean.setMessage(ClientView.SELECT_MESSAGE, msg);
            } catch (Exception ex) {
                ex.printStackTrace();
                bean.setMessage(ClientView.SELECT_MESSAGE, ex.getMessage());
            }
        }
        responseMap.put("message", bean.getMessage(ClientView.SELECT_MESSAGE));
        return(responseMap);
    }
    
    private void handleDownloadResponse(HashMap responseMap, HttpServletResponse response) throws IOException {
        ByteArrayOutputStream                       outStream;
        String                                      contentDisposition, contentType;
        
        contentType = (String) responseMap.get("contentType");
        contentDisposition = (String) responseMap.get("Content-Disposition");
        outStream = (ByteArrayOutputStream) responseMap.get("outputStream");
        
        response.setContentType(contentType);
        response.setHeader("Content-Disposition", contentDisposition);
        response.setContentLength(outStream.size());
        outStream.writeTo(response.getOutputStream());
        response.flushBuffer();
    }
    
    public static String toZipFileName(String fileName) {
        String                      result = "metacat";
        int                         idx;
        
        if (fileName != null && !fileName.equals("") && !fileName.equals(".")) {
            idx = fileName.indexOf('.');
            if (idx > -1)
                result = fileName.substring(0, idx);
            else
                result = fileName;
        }
        result += ".zip";
        return(result);
    }
    
    public static void setTextContent(XPath xPath, Node elementNode, String content) throws DOMException {
        Text                        textNode, newTxtNode;
        Document                    document;
        
        textNode = (Text) getNode(xPath, "text()", elementNode);
        if (textNode != null) {
            if (isElementContentWhitespace(textNode)) {
                //*** If there is an existing text node, and it's whitespace,
                //*** create a new text node and insert it before whitespace.
                document = elementNode.getOwnerDocument();
                newTxtNode = document.createTextNode(content);
                elementNode.insertBefore(newTxtNode, textNode);
            } else {
                //*** If there is an existing text node, and it has content,
                //*** overwrite the existing text.
                textNode.setNodeValue(content);
            }
        } else {
            //*** If there isn't an existing text node,
            //*** create a new text node and append it to the elementNode.
            document = elementNode.getOwnerDocument();
            newTxtNode = document.createTextNode(content);
            elementNode.appendChild(newTxtNode);
        }
    }
    
    public static String getTextContent(XPath xPath, Node elementNode) throws DOMException {
        String                      result = "";
        Text                        textNode;
        
        if (elementNode.getNodeType() != Node.TEXT_NODE)
            textNode = (Text) getNode(xPath, "text()", elementNode);
        else
            textNode = (Text) elementNode;
        if (textNode != null)
            result = textNode.getNodeValue();
        return(result);
    }
    
    public static boolean isElementContentWhitespace(Text textNode) {
        boolean                     result = false;
        String                      val;
        
        if ((val = textNode.getNodeValue()) != null) {
            if (val != null) {
                val = val.trim();
                result = (val.length() == 0);
            }
        }
        return(result);
    }
    
}
