/*
 * ClientViewBeanInfo.java
 *
 * Created on June 25, 2007, 10:24 AM
 */

package edu.ucsb.nceas.metacat.clientview;

import java.beans.*;

/**
 * @author barteau
 */
public class ClientViewBeanInfo extends SimpleBeanInfo {
    
    // Bean descriptor//GEN-FIRST:BeanDescriptor
    /*lazy BeanDescriptor*/
    private static BeanDescriptor getBdescriptor(){
        BeanDescriptor beanDescriptor = new BeanDescriptor  ( edu.ucsb.nceas.metacat.clientview.ClientView.class , null ); // NOI18N
        beanDescriptor.setDisplayName ( "Client View" );
        beanDescriptor.setShortDescription ( "Client view bean" );//GEN-HEADEREND:BeanDescriptor
        
        // Here you can add code for customizing the BeanDescriptor.
        
        return beanDescriptor;     }//GEN-LAST:BeanDescriptor
    
    
    // Property identifiers//GEN-FIRST:Properties
    private static final int PROPERTY_action = 0;
    private static final int PROPERTY_anyfield = 1;
    private static final int PROPERTY_contentStandard = 2;
    private static final int PROPERTY_dataFileName = 3;
    private static final int PROPERTY_docId = 4;
    private static final int PROPERTY_message = 5;
    private static final int PROPERTY_metaFileDocId = 6;
    private static final int PROPERTY_metaFileName = 7;
    private static final int PROPERTY_organization = 8;
    private static final int PROPERTY_password = 9;
    private static final int PROPERTY_pathExpr = 10;
    private static final int PROPERTY_pathValue = 11;
    private static final int PROPERTY_publicAccess = 12;
    private static final int PROPERTY_qformat = 13;
    private static final int PROPERTY_returnfield = 14;
    private static final int PROPERTY_sessionid = 15;
    private static final int PROPERTY_username = 16;

    // Property array 
    /*lazy PropertyDescriptor*/
    private static PropertyDescriptor[] getPdescriptor(){
        PropertyDescriptor[] properties = new PropertyDescriptor[17];
    
        try {
            properties[PROPERTY_action] = new PropertyDescriptor ( "action", edu.ucsb.nceas.metacat.clientview.ClientView.class, "getAction", "setAction" ); // NOI18N
            properties[PROPERTY_action].setDisplayName ( "Action" );
            properties[PROPERTY_action].setShortDescription ( "Server action" );
            properties[PROPERTY_anyfield] = new PropertyDescriptor ( "anyfield", edu.ucsb.nceas.metacat.clientview.ClientView.class, "getAnyfield", "setAnyfield" ); // NOI18N
            properties[PROPERTY_anyfield].setDisplayName ( "Any Field" );
            properties[PROPERTY_anyfield].setShortDescription ( "Search any field for data" );
            properties[PROPERTY_contentStandard] = new PropertyDescriptor ( "contentStandard", edu.ucsb.nceas.metacat.clientview.ClientView.class, "getContentStandard", "setContentStandard" ); // NOI18N
            properties[PROPERTY_contentStandard].setDisplayName ( "Content Standard" );
            properties[PROPERTY_contentStandard].setShortDescription ( "XML metadata content standard" );
            properties[PROPERTY_dataFileName] = new IndexedPropertyDescriptor ( "dataFileName", edu.ucsb.nceas.metacat.clientview.ClientView.class, "getDataFileName", "setDataFileName", "getDataFileName", "setDataFileName" ); // NOI18N
            properties[PROPERTY_dataFileName].setDisplayName ( "Data File" );
            properties[PROPERTY_dataFileName].setShortDescription ( "Data file name" );
            properties[PROPERTY_docId] = new PropertyDescriptor ( "docId", edu.ucsb.nceas.metacat.clientview.ClientView.class, "getDocId", "setDocId" ); // NOI18N
            properties[PROPERTY_docId].setDisplayName ( "Doc ID" );
            properties[PROPERTY_docId].setShortDescription ( "Metacat Document Identifier" );
            properties[PROPERTY_message] = new IndexedPropertyDescriptor ( "message", edu.ucsb.nceas.metacat.clientview.ClientView.class, "getMessage", "setMessage", "getMessage", "setMessage" ); // NOI18N
            properties[PROPERTY_message].setDisplayName ( "Message" );
            properties[PROPERTY_message].setShortDescription ( "Server messages" );
            properties[PROPERTY_metaFileDocId] = new PropertyDescriptor ( "metaFileDocId", edu.ucsb.nceas.metacat.clientview.ClientView.class, "getMetaFileDocId", "setMetaFileDocId" ); // NOI18N
            properties[PROPERTY_metaFileDocId].setDisplayName ( "Metadata File Doc Id" );
            properties[PROPERTY_metaFileDocId].setShortDescription ( "Metadata File Document Id" );
            properties[PROPERTY_metaFileName] = new PropertyDescriptor ( "metaFileName", edu.ucsb.nceas.metacat.clientview.ClientView.class, "getMetaFileName", "setMetaFileName" ); // NOI18N
            properties[PROPERTY_metaFileName].setDisplayName ( "Metadata File" );
            properties[PROPERTY_metaFileName].setShortDescription ( "XML metadata file name" );
            properties[PROPERTY_organization] = new PropertyDescriptor ( "organization", edu.ucsb.nceas.metacat.clientview.ClientView.class, "getOrganization", "setOrganization" ); // NOI18N
            properties[PROPERTY_organization].setDisplayName ( "Organization" );
            properties[PROPERTY_organization].setShortDescription ( "User's organization" );
            properties[PROPERTY_password] = new PropertyDescriptor ( "password", edu.ucsb.nceas.metacat.clientview.ClientView.class, "getPassword", "setPassword" ); // NOI18N
            properties[PROPERTY_password].setDisplayName ( "Password" );
            properties[PROPERTY_password].setShortDescription ( "Metacat user password" );
            properties[PROPERTY_pathExpr] = new PropertyDescriptor ( "pathExpr", edu.ucsb.nceas.metacat.clientview.ClientView.class, "getPathExpr", "setPathExpr" ); // NOI18N
            properties[PROPERTY_pathExpr].setDisplayName ( "Path Expression" );
            properties[PROPERTY_pathExpr].setShortDescription ( "Query path expression" );
            properties[PROPERTY_pathValue] = new PropertyDescriptor ( "pathValue", edu.ucsb.nceas.metacat.clientview.ClientView.class, "getPathValue", "setPathValue" ); // NOI18N
            properties[PROPERTY_pathValue].setDisplayName ( "Path Value" );
            properties[PROPERTY_pathValue].setShortDescription ( "Query path comparison value" );
            properties[PROPERTY_publicAccess] = new PropertyDescriptor ( "publicAccess", edu.ucsb.nceas.metacat.clientview.ClientView.class, "isPublicAccess", "setPublicAccess" ); // NOI18N
            properties[PROPERTY_publicAccess].setDisplayName ( "Public Access" );
            properties[PROPERTY_publicAccess].setShortDescription ( "Grant public read access to data package" );
            properties[PROPERTY_qformat] = new PropertyDescriptor ( "qformat", edu.ucsb.nceas.metacat.clientview.ClientView.class, "getQformat", "setQformat" ); // NOI18N
            properties[PROPERTY_qformat].setDisplayName ( "Format" );
            properties[PROPERTY_qformat].setShortDescription ( "Query format" );
            properties[PROPERTY_returnfield] = new PropertyDescriptor ( "returnfield", edu.ucsb.nceas.metacat.clientview.ClientView.class, "getReturnfield", "setReturnfield" ); // NOI18N
            properties[PROPERTY_returnfield].setDisplayName ( "Return Field" );
            properties[PROPERTY_returnfield].setShortDescription ( "Query Return Field" );
            properties[PROPERTY_sessionid] = new PropertyDescriptor ( "sessionid", edu.ucsb.nceas.metacat.clientview.ClientView.class, "getSessionid", "setSessionid" ); // NOI18N
            properties[PROPERTY_sessionid].setDisplayName ( "Sess ID" );
            properties[PROPERTY_sessionid].setShortDescription ( "Http Session Id" );
            properties[PROPERTY_username] = new PropertyDescriptor ( "username", edu.ucsb.nceas.metacat.clientview.ClientView.class, "getUsername", "setUsername" ); // NOI18N
            properties[PROPERTY_username].setDisplayName ( "User Name" );
            properties[PROPERTY_username].setShortDescription ( "Metacat user name" );
        }
        catch(IntrospectionException e) {
            e.printStackTrace();
        }//GEN-HEADEREND:Properties
        
                
        return properties;     }//GEN-LAST:Properties
    
    // EventSet identifiers//GEN-FIRST:Events

    // EventSet array
    /*lazy EventSetDescriptor*/
    private static EventSetDescriptor[] getEdescriptor(){
        EventSetDescriptor[] eventSets = new EventSetDescriptor[0];//GEN-HEADEREND:Events
        
        // Here you can add code for customizing the event sets array.
        
        return eventSets;     }//GEN-LAST:Events
    
    // Method identifiers//GEN-FIRST:Methods

    // Method array 
    /*lazy MethodDescriptor*/
    private static MethodDescriptor[] getMdescriptor(){
        MethodDescriptor[] methods = new MethodDescriptor[0];//GEN-HEADEREND:Methods
        
        // Here you can add code for customizing the methods array.
        
        return methods;     }//GEN-LAST:Methods
    
    
    private static final int defaultPropertyIndex = -1;//GEN-BEGIN:Idx
    private static final int defaultEventIndex = -1;//GEN-END:Idx
    
    
//GEN-FIRST:Superclass
    
    // Here you can add code for customizing the Superclass BeanInfo.
    
//GEN-LAST:Superclass
    
    /**
     * Gets the bean's <code>BeanDescriptor</code>s.
     *
     * @return BeanDescriptor describing the editable
     * properties of this bean.  May return null if the
     * information should be obtained by automatic analysis.
     */
    public BeanDescriptor getBeanDescriptor() {
        return getBdescriptor();
    }
    
    /**
     * Gets the bean's <code>PropertyDescriptor</code>s.
     *
     * @return An array of PropertyDescriptors describing the editable
     * properties supported by this bean.  May return null if the
     * information should be obtained by automatic analysis.
     * <p>
     * If a property is indexed, then its entry in the result array will
     * belong to the IndexedPropertyDescriptor subclass of PropertyDescriptor.
     * A client of getPropertyDescriptors can use "instanceof" to check
     * if a given PropertyDescriptor is an IndexedPropertyDescriptor.
     */
    public PropertyDescriptor[] getPropertyDescriptors() {
        return getPdescriptor();
    }
    
    /**
     * Gets the bean's <code>EventSetDescriptor</code>s.
     *
     * @return  An array of EventSetDescriptors describing the kinds of
     * events fired by this bean.  May return null if the information
     * should be obtained by automatic analysis.
     */
    public EventSetDescriptor[] getEventSetDescriptors() {
        return getEdescriptor();
    }
    
    /**
     * Gets the bean's <code>MethodDescriptor</code>s.
     *
     * @return  An array of MethodDescriptors describing the methods
     * implemented by this bean.  May return null if the information
     * should be obtained by automatic analysis.
     */
    public MethodDescriptor[] getMethodDescriptors() {
        return getMdescriptor();
    }
    
    /**
     * A bean may have a "default" property that is the property that will
     * mostly commonly be initially chosen for update by human's who are
     * customizing the bean.
     * @return  Index of default property in the PropertyDescriptor array
     * 		returned by getPropertyDescriptors.
     * <P>	Returns -1 if there is no default property.
     */
    public int getDefaultPropertyIndex() {
        return defaultPropertyIndex;
    }
    
    /**
     * A bean may have a "default" event that is the event that will
     * mostly commonly be used by human's when using the bean.
     * @return Index of default event in the EventSetDescriptor array
     *		returned by getEventSetDescriptors.
     * <P>	Returns -1 if there is no default event.
     */
    public int getDefaultEventIndex() {
        return defaultEventIndex;
    }

    
}

