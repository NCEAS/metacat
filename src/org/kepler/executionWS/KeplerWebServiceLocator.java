/**
 * KeplerWebServiceLocator.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.3 Oct 05, 2005 (05:23:37 EDT) WSDL2Java emitter.
 */

package org.kepler.executionWS;

import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;

public class KeplerWebServiceLocator extends org.apache.axis.client.Service implements org.kepler.executionWS.KeplerWebService {

	private static final long serialVersionUID = -6004917965491182326L;
	
	private static String endpointAddress = null;
    // Use to get a proxy class for KeplerWebServiceHttpSoap12Endpoint
    private String KeplerWebServiceHttpSoap12Endpoint_address = null;
    
    // Use to get a proxy class for KeplerWebServiceHttpSoap11Endpoint
    private java.lang.String KeplerWebServiceHttpSoap11Endpoint_address = null;

	public KeplerWebServiceLocator() throws javax.xml.rpc.ServiceException {
		try {
			endpointAddress = PropertyService.getProperty("executionEngine.endPointAddress");
			KeplerWebServiceHttpSoap12Endpoint_address = endpointAddress;
			KeplerWebServiceHttpSoap11Endpoint_address = endpointAddress;
		} catch (PropertyNotFoundException pnfe) {
			throw new javax.xml.rpc.ServiceException("KeplerWebServiceLocator() - Property Error while trying to intialize locator.");
		}
    }


    public KeplerWebServiceLocator(org.apache.axis.EngineConfiguration config) {
        super(config);
    }

    public KeplerWebServiceLocator(java.lang.String wsdlLoc, javax.xml.namespace.QName sName) throws javax.xml.rpc.ServiceException {
        super(wsdlLoc, sName);
    }

    public java.lang.String getKeplerWebServiceHttpSoap12EndpointAddress() {
        return KeplerWebServiceHttpSoap12Endpoint_address;
    }

    // The WSDD service name defaults to the port name.
    private java.lang.String KeplerWebServiceHttpSoap12EndpointWSDDServiceName = "KeplerWebServiceHttpSoap12Endpoint";

    public java.lang.String getKeplerWebServiceHttpSoap12EndpointWSDDServiceName() {
        return KeplerWebServiceHttpSoap12EndpointWSDDServiceName;
    }

    public void setKeplerWebServiceHttpSoap12EndpointWSDDServiceName(java.lang.String name) {
        KeplerWebServiceHttpSoap12EndpointWSDDServiceName = name;
    }

    public org.kepler.executionWS.KeplerWebServicePortType getKeplerWebServiceHttpSoap12Endpoint() throws javax.xml.rpc.ServiceException {
       java.net.URL endpoint;
        try {
            endpoint = new java.net.URL(KeplerWebServiceHttpSoap12Endpoint_address);
        }
        catch (java.net.MalformedURLException e) {
            throw new javax.xml.rpc.ServiceException(e);
        }
        return getKeplerWebServiceHttpSoap12Endpoint(endpoint);
    }

    public org.kepler.executionWS.KeplerWebServicePortType getKeplerWebServiceHttpSoap12Endpoint(java.net.URL portAddress) throws javax.xml.rpc.ServiceException {
        try {
            org.kepler.executionWS.KeplerWebServiceSoap12BindingStub _stub = new org.kepler.executionWS.KeplerWebServiceSoap12BindingStub(portAddress, this);
            _stub.setPortName(getKeplerWebServiceHttpSoap12EndpointWSDDServiceName());
            return _stub;
        }
        catch (org.apache.axis.AxisFault e) {
            return null;
        }
    }

    public void setKeplerWebServiceHttpSoap12EndpointEndpointAddress(java.lang.String address) {
        KeplerWebServiceHttpSoap12Endpoint_address = address;
    }

    public java.lang.String getKeplerWebServiceHttpSoap11EndpointAddress() {
        return KeplerWebServiceHttpSoap11Endpoint_address;
    }

    // The WSDD service name defaults to the port name.
    private java.lang.String KeplerWebServiceHttpSoap11EndpointWSDDServiceName = "KeplerWebServiceHttpSoap11Endpoint";

    public java.lang.String getKeplerWebServiceHttpSoap11EndpointWSDDServiceName() {
        return KeplerWebServiceHttpSoap11EndpointWSDDServiceName;
    }

    public void setKeplerWebServiceHttpSoap11EndpointWSDDServiceName(java.lang.String name) {
        KeplerWebServiceHttpSoap11EndpointWSDDServiceName = name;
    }

    public org.kepler.executionWS.KeplerWebServicePortType getKeplerWebServiceHttpSoap11Endpoint() throws javax.xml.rpc.ServiceException {
       java.net.URL endpoint;
        try {
            endpoint = new java.net.URL(KeplerWebServiceHttpSoap11Endpoint_address);
        }
        catch (java.net.MalformedURLException e) {
            throw new javax.xml.rpc.ServiceException(e);
        }
        return getKeplerWebServiceHttpSoap11Endpoint(endpoint);
    }

    public org.kepler.executionWS.KeplerWebServicePortType getKeplerWebServiceHttpSoap11Endpoint(java.net.URL portAddress) throws javax.xml.rpc.ServiceException {
        try {
            org.kepler.executionWS.KeplerWebServiceSoap11BindingStub _stub = new org.kepler.executionWS.KeplerWebServiceSoap11BindingStub(portAddress, this);
            _stub.setPortName(getKeplerWebServiceHttpSoap11EndpointWSDDServiceName());
            return _stub;
        }
        catch (org.apache.axis.AxisFault e) {
            return null;
        }
    }

    public void setKeplerWebServiceHttpSoap11EndpointEndpointAddress(java.lang.String address) {
        KeplerWebServiceHttpSoap11Endpoint_address = address;
    }

    /**
     * For the given interface, get the stub implementation.
     * If this service has no port for the given interface,
     * then ServiceException is thrown.
     * This service has multiple ports for a given interface;
     * the proxy implementation returned may be indeterminate.
     */
    public java.rmi.Remote getPort(Class serviceEndpointInterface) throws javax.xml.rpc.ServiceException {
        try {
            if (org.kepler.executionWS.KeplerWebServicePortType.class.isAssignableFrom(serviceEndpointInterface)) {
                org.kepler.executionWS.KeplerWebServiceSoap12BindingStub _stub = new org.kepler.executionWS.KeplerWebServiceSoap12BindingStub(new java.net.URL(KeplerWebServiceHttpSoap12Endpoint_address), this);
                _stub.setPortName(getKeplerWebServiceHttpSoap12EndpointWSDDServiceName());
                return _stub;
            }
            if (org.kepler.executionWS.KeplerWebServicePortType.class.isAssignableFrom(serviceEndpointInterface)) {
                org.kepler.executionWS.KeplerWebServiceSoap11BindingStub _stub = new org.kepler.executionWS.KeplerWebServiceSoap11BindingStub(new java.net.URL(KeplerWebServiceHttpSoap11Endpoint_address), this);
                _stub.setPortName(getKeplerWebServiceHttpSoap11EndpointWSDDServiceName());
                return _stub;
            }
        }
        catch (java.lang.Throwable t) {
            throw new javax.xml.rpc.ServiceException(t);
        }
        throw new javax.xml.rpc.ServiceException("There is no stub implementation for the interface:  " + (serviceEndpointInterface == null ? "null" : serviceEndpointInterface.getName()));
    }

    /**
     * For the given interface, get the stub implementation.
     * If this service has no port for the given interface,
     * then ServiceException is thrown.
     */
    public java.rmi.Remote getPort(javax.xml.namespace.QName portName, Class serviceEndpointInterface) throws javax.xml.rpc.ServiceException {
        if (portName == null) {
            return getPort(serviceEndpointInterface);
        }
        java.lang.String inputPortName = portName.getLocalPart();
        if ("KeplerWebServiceHttpSoap12Endpoint".equals(inputPortName)) {
            return getKeplerWebServiceHttpSoap12Endpoint();
        }
        else if ("KeplerWebServiceHttpSoap11Endpoint".equals(inputPortName)) {
            return getKeplerWebServiceHttpSoap11Endpoint();
        }
        else  {
            java.rmi.Remote _stub = getPort(serviceEndpointInterface);
            ((org.apache.axis.client.Stub) _stub).setPortName(portName);
            return _stub;
        }
    }

    public javax.xml.namespace.QName getServiceName() {
        return new javax.xml.namespace.QName("http://executionWS.kepler.org/", "KeplerWebService");
    }

    private java.util.HashSet ports = null;

    public java.util.Iterator getPorts() {
        if (ports == null) {
            ports = new java.util.HashSet();
            ports.add(new javax.xml.namespace.QName("http://executionWS.kepler.org/", "KeplerWebServiceHttpSoap12Endpoint"));
            ports.add(new javax.xml.namespace.QName("http://executionWS.kepler.org/", "KeplerWebServiceHttpSoap11Endpoint"));
        }
        return ports.iterator();
    }

    /**
    * Set the endpoint address for the specified port name.
    */
    public void setEndpointAddress(java.lang.String portName, java.lang.String address) throws javax.xml.rpc.ServiceException {
        
if ("KeplerWebServiceHttpSoap12Endpoint".equals(portName)) {
            setKeplerWebServiceHttpSoap12EndpointEndpointAddress(address);
        }
        else 
if ("KeplerWebServiceHttpSoap11Endpoint".equals(portName)) {
            setKeplerWebServiceHttpSoap11EndpointEndpointAddress(address);
        }
        else 
{ // Unknown Port Name
            throw new javax.xml.rpc.ServiceException(" Cannot set Endpoint Address for Unknown Port" + portName);
        }
    }

    /**
    * Set the endpoint address for the specified port name.
    */
    public void setEndpointAddress(javax.xml.namespace.QName portName, java.lang.String address) throws javax.xml.rpc.ServiceException {
        setEndpointAddress(portName.getLocalPart(), address);
    }

}
