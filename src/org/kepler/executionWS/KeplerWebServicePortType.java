/**
 * KeplerWebServicePortType.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.3 Oct 05, 2005 (05:23:37 EDT) WSDL2Java emitter.
 */

package org.kepler.executionWS;

public interface KeplerWebServicePortType extends java.rmi.Remote {
    public java.lang.String startExeByURI(java.lang.String wfURI) throws java.rmi.RemoteException, org.kepler.executionWS.xsd.ExceptionType0, org.kepler.executionWS.xsd.FileNotFoundException;
    public java.lang.Object startExeByAttach(java.lang.Object startExeByAttach) throws java.rmi.RemoteException, org.kepler.executionWS.xsd.ExceptionType0;
    public void main(java.lang.String[] argus) throws java.rmi.RemoteException;
    public java.lang.Boolean pauseExecution(java.lang.String wfID) throws java.rmi.RemoteException, org.kepler.executionWS.xsd.ExceptionType0;
    public java.lang.String startExeByContent(java.lang.String wfContent) throws java.rmi.RemoteException, org.kepler.executionWS.xsd.ExceptionType0;
    public java.lang.Object executeByAttachWithPara(java.lang.Object executeByAttachWithPara) throws java.rmi.RemoteException, org.kepler.executionWS.xsd.ExceptionType0;
    public java.lang.Object startExeByAttachWithPara(java.lang.Object startExeByAttachWithPara) throws java.rmi.RemoteException, org.kepler.executionWS.xsd.ExceptionType0;
    public java.lang.Object executeByAttach(java.lang.Object executeByAttach) throws java.rmi.RemoteException, org.kepler.executionWS.xsd.ExceptionType0;
    public java.lang.String startExeByURIWithPara(java.lang.String wfURI, org.kepler.executionWS.xsd.KeplerWfParameter[] wfParas) throws java.rmi.RemoteException, org.kepler.executionWS.xsd.ExceptionType0;
    public org.kepler.executionWS.xsd.KeplerWfOutput[] getResults(java.lang.String wfID) throws java.rmi.RemoteException, org.kepler.executionWS.xsd.ExceptionType0, org.kepler.executionWS.xsd.FileNotFoundException;
    public java.lang.String execute(java.lang.String karLSID) throws java.rmi.RemoteException, org.kepler.executionWS.xsd.ExceptionType0;
    public org.kepler.executionWS.xsd.KeplerWfOutput[] executeByURI(java.lang.String wfURI) throws java.rmi.RemoteException, org.kepler.executionWS.xsd.ExceptionType0, org.kepler.executionWS.xsd.FileNotFoundException;
    public java.lang.Object getResultsByAttach(java.lang.Object getResultsByAttach) throws java.rmi.RemoteException, org.kepler.executionWS.xsd.ExceptionType0, org.kepler.executionWS.xsd.FileNotFoundException;
    public java.lang.String getStatus(java.lang.String workflowRunID) throws java.rmi.RemoteException;
    public java.lang.String startExeByContentWithPara(java.lang.String wfContent, org.kepler.executionWS.xsd.KeplerWfParameter[] wfParas) throws java.rmi.RemoteException, org.kepler.executionWS.xsd.ExceptionType0;
    public java.lang.String getExecutionStatus(java.lang.String wfID) throws java.rmi.RemoteException, org.kepler.executionWS.xsd.ExceptionType0;
    public java.lang.Boolean resumeExecution(java.lang.String wfID) throws java.rmi.RemoteException, org.kepler.executionWS.xsd.ExceptionType0;
    public java.lang.Boolean stopExecution(java.lang.String wfID) throws java.rmi.RemoteException, org.kepler.executionWS.xsd.ExceptionType0;
    public java.lang.Boolean delLogDir(java.lang.String wfID) throws java.rmi.RemoteException, org.kepler.executionWS.xsd.ExceptionType0, org.kepler.executionWS.xsd.FileNotFoundException;
    public org.kepler.executionWS.xsd.KeplerWfOutput[] executeByURIWithPara(java.lang.String wfURI, org.kepler.executionWS.xsd.KeplerWfParameter[] wfParas) throws java.rmi.RemoteException, org.kepler.executionWS.xsd.ExceptionType0;
    public org.kepler.executionWS.xsd.KeplerWfOutput[] executeByContentWithPara(java.lang.String wfContent, org.kepler.executionWS.xsd.KeplerWfParameter[] wfParas) throws java.rmi.RemoteException, org.kepler.executionWS.xsd.ExceptionType0;
    public org.kepler.executionWS.xsd.KeplerWfOutput[] executeByContent(java.lang.String wfContent) throws java.rmi.RemoteException, org.kepler.executionWS.xsd.ExceptionType0;
    public java.lang.Object uploadFilesByAttach(java.lang.Object uploadFilesByAttach) throws java.rmi.RemoteException, org.kepler.executionWS.xsd.ExceptionType0;
}
