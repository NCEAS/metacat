/**
 * KeplerWebService.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.3 Oct 05, 2005 (05:23:37 EDT) WSDL2Java emitter.
 */

package org.kepler.executionWS;

public interface KeplerWebService extends javax.xml.rpc.Service {
    public java.lang.String getKeplerWebServiceHttpSoap12EndpointAddress();

    public org.kepler.executionWS.KeplerWebServicePortType getKeplerWebServiceHttpSoap12Endpoint() throws javax.xml.rpc.ServiceException;

    public org.kepler.executionWS.KeplerWebServicePortType getKeplerWebServiceHttpSoap12Endpoint(java.net.URL portAddress) throws javax.xml.rpc.ServiceException;
    public java.lang.String getKeplerWebServiceHttpSoap11EndpointAddress();

    public org.kepler.executionWS.KeplerWebServicePortType getKeplerWebServiceHttpSoap11Endpoint() throws javax.xml.rpc.ServiceException;

    public org.kepler.executionWS.KeplerWebServicePortType getKeplerWebServiceHttpSoap11Endpoint(java.net.URL portAddress) throws javax.xml.rpc.ServiceException;
}
