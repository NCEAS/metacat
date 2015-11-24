/**
 * KeplerWebServiceSoap12BindingStub.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.3 Oct 05, 2005 (05:23:37 EDT) WSDL2Java emitter.
 */

package org.kepler.executionWS;

public class KeplerWebServiceSoap12BindingStub extends org.apache.axis.client.Stub implements org.kepler.executionWS.KeplerWebServicePortType {
    private java.util.Vector cachedSerClasses = new java.util.Vector();
    private java.util.Vector cachedSerQNames = new java.util.Vector();
    private java.util.Vector cachedSerFactories = new java.util.Vector();
    private java.util.Vector cachedDeserFactories = new java.util.Vector();

    static org.apache.axis.description.OperationDesc [] _operations;

    static {
        _operations = new org.apache.axis.description.OperationDesc[23];
        _initOperationDesc1();
        _initOperationDesc2();
        _initOperationDesc3();
    }

    private static void _initOperationDesc1(){
        org.apache.axis.description.OperationDesc oper;
        org.apache.axis.description.ParameterDesc param;
        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("startExeByURI");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", "wfURI"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false);
        param.setOmittable(true);
        param.setNillable(true);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        oper.setReturnClass(java.lang.String.class);
        oper.setReturnQName(new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", "return"));
        oper.setStyle(org.apache.axis.constants.Style.WRAPPED);
        oper.setUse(org.apache.axis.constants.Use.LITERAL);
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", "Exception"),
                      "org.kepler.executionWS.xsd.ExceptionType0",
                      new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", ">Exception"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", "FileNotFoundException"),
                      "org.kepler.executionWS.xsd.FileNotFoundException",
                      new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", ">FileNotFoundException"), 
                      true
                     ));
        _operations[0] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("startExeByAttach");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", "startExeByAttach"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "anyType"), java.lang.Object.class, false, false);
        param.setOmittable(true);
        param.setNillable(true);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "anyType"));
        oper.setReturnClass(java.lang.Object.class);
        oper.setReturnQName(new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", "return"));
        oper.setStyle(org.apache.axis.constants.Style.WRAPPED);
        oper.setUse(org.apache.axis.constants.Use.LITERAL);
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", "Exception"),
                      "org.kepler.executionWS.xsd.ExceptionType0",
                      new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", ">Exception"), 
                      true
                     ));
        _operations[1] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("main");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", "argus"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String[].class, false, false);
        param.setOmittable(true);
        param.setNillable(true);
        oper.addParameter(param);
        oper.setReturnType(org.apache.axis.encoding.XMLType.AXIS_VOID);
        oper.setStyle(org.apache.axis.constants.Style.WRAPPED);
        oper.setUse(org.apache.axis.constants.Use.LITERAL);
        _operations[2] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("startExeByContent");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", "wfContent"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false);
        param.setOmittable(true);
        param.setNillable(true);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        oper.setReturnClass(java.lang.String.class);
        oper.setReturnQName(new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", "return"));
        oper.setStyle(org.apache.axis.constants.Style.WRAPPED);
        oper.setUse(org.apache.axis.constants.Use.LITERAL);
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", "Exception"),
                      "org.kepler.executionWS.xsd.ExceptionType0",
                      new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", ">Exception"), 
                      true
                     ));
        _operations[3] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("pauseExecution");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", "wfID"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false);
        param.setOmittable(true);
        param.setNillable(true);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "boolean"));
        oper.setReturnClass(java.lang.Boolean.class);
        oper.setReturnQName(new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", "return"));
        oper.setStyle(org.apache.axis.constants.Style.WRAPPED);
        oper.setUse(org.apache.axis.constants.Use.LITERAL);
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", "Exception"),
                      "org.kepler.executionWS.xsd.ExceptionType0",
                      new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", ">Exception"), 
                      true
                     ));
        _operations[4] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("startExeByAttachWithPara");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", "startExeByAttachWithPara"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "anyType"), java.lang.Object.class, false, false);
        param.setOmittable(true);
        param.setNillable(true);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "anyType"));
        oper.setReturnClass(java.lang.Object.class);
        oper.setReturnQName(new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", "return"));
        oper.setStyle(org.apache.axis.constants.Style.WRAPPED);
        oper.setUse(org.apache.axis.constants.Use.LITERAL);
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", "Exception"),
                      "org.kepler.executionWS.xsd.ExceptionType0",
                      new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", ">Exception"), 
                      true
                     ));
        _operations[5] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("executeByAttachWithPara");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", "executeByAttachWithPara"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "anyType"), java.lang.Object.class, false, false);
        param.setOmittable(true);
        param.setNillable(true);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "anyType"));
        oper.setReturnClass(java.lang.Object.class);
        oper.setReturnQName(new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", "return"));
        oper.setStyle(org.apache.axis.constants.Style.WRAPPED);
        oper.setUse(org.apache.axis.constants.Use.LITERAL);
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", "Exception"),
                      "org.kepler.executionWS.xsd.ExceptionType0",
                      new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", ">Exception"), 
                      true
                     ));
        _operations[6] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("executeByAttach");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", "executeByAttach"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "anyType"), java.lang.Object.class, false, false);
        param.setOmittable(true);
        param.setNillable(true);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "anyType"));
        oper.setReturnClass(java.lang.Object.class);
        oper.setReturnQName(new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", "return"));
        oper.setStyle(org.apache.axis.constants.Style.WRAPPED);
        oper.setUse(org.apache.axis.constants.Use.LITERAL);
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", "Exception"),
                      "org.kepler.executionWS.xsd.ExceptionType0",
                      new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", ">Exception"), 
                      true
                     ));
        _operations[7] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("startExeByURIWithPara");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", "wfURI"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false);
        param.setOmittable(true);
        param.setNillable(true);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", "wfParas"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", "KeplerWfParameter"), org.kepler.executionWS.xsd.KeplerWfParameter[].class, false, false);
        param.setOmittable(true);
        param.setNillable(true);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        oper.setReturnClass(java.lang.String.class);
        oper.setReturnQName(new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", "return"));
        oper.setStyle(org.apache.axis.constants.Style.WRAPPED);
        oper.setUse(org.apache.axis.constants.Use.LITERAL);
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", "Exception"),
                      "org.kepler.executionWS.xsd.ExceptionType0",
                      new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", ">Exception"), 
                      true
                     ));
        _operations[8] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("getResults");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", "wfID"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false);
        param.setOmittable(true);
        param.setNillable(true);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", "KeplerWfOutput"));
        oper.setReturnClass(org.kepler.executionWS.xsd.KeplerWfOutput[].class);
        oper.setReturnQName(new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", "return"));
        oper.setStyle(org.apache.axis.constants.Style.WRAPPED);
        oper.setUse(org.apache.axis.constants.Use.LITERAL);
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", "Exception"),
                      "org.kepler.executionWS.xsd.ExceptionType0",
                      new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", ">Exception"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", "FileNotFoundException"),
                      "org.kepler.executionWS.xsd.FileNotFoundException",
                      new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", ">FileNotFoundException"), 
                      true
                     ));
        _operations[9] = oper;

    }

    private static void _initOperationDesc2(){
        org.apache.axis.description.OperationDesc oper;
        org.apache.axis.description.ParameterDesc param;
        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("execute");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", "karLSID"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false);
        param.setOmittable(true);
        param.setNillable(true);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        oper.setReturnClass(java.lang.String.class);
        oper.setReturnQName(new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", "return"));
        oper.setStyle(org.apache.axis.constants.Style.WRAPPED);
        oper.setUse(org.apache.axis.constants.Use.LITERAL);
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", "Exception"),
                      "org.kepler.executionWS.xsd.ExceptionType0",
                      new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", ">Exception"), 
                      true
                     ));
        _operations[10] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("executeByURI");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", "wfURI"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false);
        param.setOmittable(true);
        param.setNillable(true);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", "KeplerWfOutput"));
        oper.setReturnClass(org.kepler.executionWS.xsd.KeplerWfOutput[].class);
        oper.setReturnQName(new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", "return"));
        oper.setStyle(org.apache.axis.constants.Style.WRAPPED);
        oper.setUse(org.apache.axis.constants.Use.LITERAL);
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", "Exception"),
                      "org.kepler.executionWS.xsd.ExceptionType0",
                      new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", ">Exception"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", "FileNotFoundException"),
                      "org.kepler.executionWS.xsd.FileNotFoundException",
                      new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", ">FileNotFoundException"), 
                      true
                     ));
        _operations[11] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("getResultsByAttach");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", "getResultsByAttach"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "anyType"), java.lang.Object.class, false, false);
        param.setOmittable(true);
        param.setNillable(true);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "anyType"));
        oper.setReturnClass(java.lang.Object.class);
        oper.setReturnQName(new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", "return"));
        oper.setStyle(org.apache.axis.constants.Style.WRAPPED);
        oper.setUse(org.apache.axis.constants.Use.LITERAL);
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", "Exception"),
                      "org.kepler.executionWS.xsd.ExceptionType0",
                      new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", ">Exception"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", "FileNotFoundException"),
                      "org.kepler.executionWS.xsd.FileNotFoundException",
                      new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", ">FileNotFoundException"), 
                      true
                     ));
        _operations[12] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("getStatus");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", "workflowRunID"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false);
        param.setOmittable(true);
        param.setNillable(true);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        oper.setReturnClass(java.lang.String.class);
        oper.setReturnQName(new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", "return"));
        oper.setStyle(org.apache.axis.constants.Style.WRAPPED);
        oper.setUse(org.apache.axis.constants.Use.LITERAL);
        _operations[13] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("startExeByContentWithPara");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", "wfContent"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false);
        param.setOmittable(true);
        param.setNillable(true);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", "wfParas"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", "KeplerWfParameter"), org.kepler.executionWS.xsd.KeplerWfParameter[].class, false, false);
        param.setOmittable(true);
        param.setNillable(true);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        oper.setReturnClass(java.lang.String.class);
        oper.setReturnQName(new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", "return"));
        oper.setStyle(org.apache.axis.constants.Style.WRAPPED);
        oper.setUse(org.apache.axis.constants.Use.LITERAL);
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", "Exception"),
                      "org.kepler.executionWS.xsd.ExceptionType0",
                      new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", ">Exception"), 
                      true
                     ));
        _operations[14] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("getExecutionStatus");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", "wfID"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false);
        param.setOmittable(true);
        param.setNillable(true);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        oper.setReturnClass(java.lang.String.class);
        oper.setReturnQName(new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", "return"));
        oper.setStyle(org.apache.axis.constants.Style.WRAPPED);
        oper.setUse(org.apache.axis.constants.Use.LITERAL);
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", "Exception"),
                      "org.kepler.executionWS.xsd.ExceptionType0",
                      new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", ">Exception"), 
                      true
                     ));
        _operations[15] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("resumeExecution");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", "wfID"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false);
        param.setOmittable(true);
        param.setNillable(true);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "boolean"));
        oper.setReturnClass(java.lang.Boolean.class);
        oper.setReturnQName(new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", "return"));
        oper.setStyle(org.apache.axis.constants.Style.WRAPPED);
        oper.setUse(org.apache.axis.constants.Use.LITERAL);
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", "Exception"),
                      "org.kepler.executionWS.xsd.ExceptionType0",
                      new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", ">Exception"), 
                      true
                     ));
        _operations[16] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("stopExecution");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", "wfID"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false);
        param.setOmittable(true);
        param.setNillable(true);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "boolean"));
        oper.setReturnClass(java.lang.Boolean.class);
        oper.setReturnQName(new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", "return"));
        oper.setStyle(org.apache.axis.constants.Style.WRAPPED);
        oper.setUse(org.apache.axis.constants.Use.LITERAL);
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", "Exception"),
                      "org.kepler.executionWS.xsd.ExceptionType0",
                      new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", ">Exception"), 
                      true
                     ));
        _operations[17] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("executeByURIWithPara");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", "wfURI"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false);
        param.setOmittable(true);
        param.setNillable(true);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", "wfParas"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", "KeplerWfParameter"), org.kepler.executionWS.xsd.KeplerWfParameter[].class, false, false);
        param.setOmittable(true);
        param.setNillable(true);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", "KeplerWfOutput"));
        oper.setReturnClass(org.kepler.executionWS.xsd.KeplerWfOutput[].class);
        oper.setReturnQName(new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", "return"));
        oper.setStyle(org.apache.axis.constants.Style.WRAPPED);
        oper.setUse(org.apache.axis.constants.Use.LITERAL);
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", "Exception"),
                      "org.kepler.executionWS.xsd.ExceptionType0",
                      new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", ">Exception"), 
                      true
                     ));
        _operations[18] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("delLogDir");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", "wfID"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false);
        param.setOmittable(true);
        param.setNillable(true);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "boolean"));
        oper.setReturnClass(java.lang.Boolean.class);
        oper.setReturnQName(new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", "return"));
        oper.setStyle(org.apache.axis.constants.Style.WRAPPED);
        oper.setUse(org.apache.axis.constants.Use.LITERAL);
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", "Exception"),
                      "org.kepler.executionWS.xsd.ExceptionType0",
                      new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", ">Exception"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", "FileNotFoundException"),
                      "org.kepler.executionWS.xsd.FileNotFoundException",
                      new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", ">FileNotFoundException"), 
                      true
                     ));
        _operations[19] = oper;

    }

    private static void _initOperationDesc3(){
        org.apache.axis.description.OperationDesc oper;
        org.apache.axis.description.ParameterDesc param;
        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("executeByContent");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", "wfContent"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false);
        param.setOmittable(true);
        param.setNillable(true);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", "KeplerWfOutput"));
        oper.setReturnClass(org.kepler.executionWS.xsd.KeplerWfOutput[].class);
        oper.setReturnQName(new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", "return"));
        oper.setStyle(org.apache.axis.constants.Style.WRAPPED);
        oper.setUse(org.apache.axis.constants.Use.LITERAL);
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", "Exception"),
                      "org.kepler.executionWS.xsd.ExceptionType0",
                      new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", ">Exception"), 
                      true
                     ));
        _operations[20] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("executeByContentWithPara");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", "wfContent"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false);
        param.setOmittable(true);
        param.setNillable(true);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", "wfParas"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", "KeplerWfParameter"), org.kepler.executionWS.xsd.KeplerWfParameter[].class, false, false);
        param.setOmittable(true);
        param.setNillable(true);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", "KeplerWfOutput"));
        oper.setReturnClass(org.kepler.executionWS.xsd.KeplerWfOutput[].class);
        oper.setReturnQName(new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", "return"));
        oper.setStyle(org.apache.axis.constants.Style.WRAPPED);
        oper.setUse(org.apache.axis.constants.Use.LITERAL);
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", "Exception"),
                      "org.kepler.executionWS.xsd.ExceptionType0",
                      new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", ">Exception"), 
                      true
                     ));
        _operations[21] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("uploadFilesByAttach");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", "uploadFilesByAttach"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "anyType"), java.lang.Object.class, false, false);
        param.setOmittable(true);
        param.setNillable(true);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "anyType"));
        oper.setReturnClass(java.lang.Object.class);
        oper.setReturnQName(new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", "return"));
        oper.setStyle(org.apache.axis.constants.Style.WRAPPED);
        oper.setUse(org.apache.axis.constants.Use.LITERAL);
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", "Exception"),
                      "org.kepler.executionWS.xsd.ExceptionType0",
                      new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", ">Exception"), 
                      true
                     ));
        _operations[22] = oper;

    }

    public KeplerWebServiceSoap12BindingStub() throws org.apache.axis.AxisFault {
         this(null);
    }

    public KeplerWebServiceSoap12BindingStub(java.net.URL endpointURL, javax.xml.rpc.Service service) throws org.apache.axis.AxisFault {
         this(service);
         super.cachedEndpoint = endpointURL;
    }

    public KeplerWebServiceSoap12BindingStub(javax.xml.rpc.Service service) throws org.apache.axis.AxisFault {
        if (service == null) {
            super.service = new org.apache.axis.client.Service();
        } else {
            super.service = service;
        }
        ((org.apache.axis.client.Service)super.service).setTypeMappingVersion("1.2");
            java.lang.Class cls;
            javax.xml.namespace.QName qName;
            javax.xml.namespace.QName qName2;
            java.lang.Class beansf = org.apache.axis.encoding.ser.BeanSerializerFactory.class;
            java.lang.Class beandf = org.apache.axis.encoding.ser.BeanDeserializerFactory.class;
            java.lang.Class enumsf = org.apache.axis.encoding.ser.EnumSerializerFactory.class;
            java.lang.Class enumdf = org.apache.axis.encoding.ser.EnumDeserializerFactory.class;
            java.lang.Class arraysf = org.apache.axis.encoding.ser.ArraySerializerFactory.class;
            java.lang.Class arraydf = org.apache.axis.encoding.ser.ArrayDeserializerFactory.class;
            java.lang.Class simplesf = org.apache.axis.encoding.ser.SimpleSerializerFactory.class;
            java.lang.Class simpledf = org.apache.axis.encoding.ser.SimpleDeserializerFactory.class;
            java.lang.Class simplelistsf = org.apache.axis.encoding.ser.SimpleListSerializerFactory.class;
            java.lang.Class simplelistdf = org.apache.axis.encoding.ser.SimpleListDeserializerFactory.class;
            qName = new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", ">delLogDir");
            cachedSerQNames.add(qName);
            cls = org.kepler.executionWS.xsd.DelLogDir.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", ">delLogDirResponse");
            cachedSerQNames.add(qName);
            cls = org.kepler.executionWS.xsd.DelLogDirResponse.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", ">Exception");
            cachedSerQNames.add(qName);
            cls = org.kepler.executionWS.xsd.ExceptionType0.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", ">execute");
            cachedSerQNames.add(qName);
            cls = org.kepler.executionWS.xsd.Execute.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", ">executeByAttach");
            cachedSerQNames.add(qName);
            cls = org.kepler.executionWS.xsd.ExecuteByAttach.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", ">executeByAttachResponse");
            cachedSerQNames.add(qName);
            cls = org.kepler.executionWS.xsd.ExecuteByAttachResponse.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", ">executeByAttachWithPara");
            cachedSerQNames.add(qName);
            cls = org.kepler.executionWS.xsd.ExecuteByAttachWithPara.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", ">executeByAttachWithParaResponse");
            cachedSerQNames.add(qName);
            cls = org.kepler.executionWS.xsd.ExecuteByAttachWithParaResponse.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", ">executeByContent");
            cachedSerQNames.add(qName);
            cls = org.kepler.executionWS.xsd.ExecuteByContent.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", ">executeByContentResponse");
            cachedSerQNames.add(qName);
            cls = org.kepler.executionWS.xsd.KeplerWfOutput[].class;
            cachedSerClasses.add(cls);
            qName = new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", "KeplerWfOutput");
            qName2 = new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", "return");
            cachedSerFactories.add(new org.apache.axis.encoding.ser.ArraySerializerFactory(qName, qName2));
            cachedDeserFactories.add(new org.apache.axis.encoding.ser.ArrayDeserializerFactory());

            qName = new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", ">executeByContentWithPara");
            cachedSerQNames.add(qName);
            cls = org.kepler.executionWS.xsd.ExecuteByContentWithPara.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", ">executeByContentWithParaResponse");
            cachedSerQNames.add(qName);
            cls = org.kepler.executionWS.xsd.KeplerWfOutput[].class;
            cachedSerClasses.add(cls);
            qName = new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", "KeplerWfOutput");
            qName2 = new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", "return");
            cachedSerFactories.add(new org.apache.axis.encoding.ser.ArraySerializerFactory(qName, qName2));
            cachedDeserFactories.add(new org.apache.axis.encoding.ser.ArrayDeserializerFactory());

            qName = new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", ">executeByURI");
            cachedSerQNames.add(qName);
            cls = org.kepler.executionWS.xsd.ExecuteByURI.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", ">executeByURIResponse");
            cachedSerQNames.add(qName);
            cls = org.kepler.executionWS.xsd.KeplerWfOutput[].class;
            cachedSerClasses.add(cls);
            qName = new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", "KeplerWfOutput");
            qName2 = new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", "return");
            cachedSerFactories.add(new org.apache.axis.encoding.ser.ArraySerializerFactory(qName, qName2));
            cachedDeserFactories.add(new org.apache.axis.encoding.ser.ArrayDeserializerFactory());

            qName = new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", ">executeByURIWithPara");
            cachedSerQNames.add(qName);
            cls = org.kepler.executionWS.xsd.ExecuteByURIWithPara.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", ">executeByURIWithParaResponse");
            cachedSerQNames.add(qName);
            cls = org.kepler.executionWS.xsd.KeplerWfOutput[].class;
            cachedSerClasses.add(cls);
            qName = new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", "KeplerWfOutput");
            qName2 = new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", "return");
            cachedSerFactories.add(new org.apache.axis.encoding.ser.ArraySerializerFactory(qName, qName2));
            cachedDeserFactories.add(new org.apache.axis.encoding.ser.ArrayDeserializerFactory());

            qName = new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", ">executeResponse");
            cachedSerQNames.add(qName);
            cls = org.kepler.executionWS.xsd.ExecuteResponse.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", ">FileNotFoundException");
            cachedSerQNames.add(qName);
            cls = org.kepler.executionWS.xsd.FileNotFoundException.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", ">getExecutionStatus");
            cachedSerQNames.add(qName);
            cls = org.kepler.executionWS.xsd.GetExecutionStatus.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", ">getExecutionStatusResponse");
            cachedSerQNames.add(qName);
            cls = org.kepler.executionWS.xsd.GetExecutionStatusResponse.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", ">getResults");
            cachedSerQNames.add(qName);
            cls = org.kepler.executionWS.xsd.GetResults.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", ">getResultsByAttach");
            cachedSerQNames.add(qName);
            cls = org.kepler.executionWS.xsd.GetResultsByAttach.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", ">getResultsByAttachResponse");
            cachedSerQNames.add(qName);
            cls = org.kepler.executionWS.xsd.GetResultsByAttachResponse.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", ">getResultsResponse");
            cachedSerQNames.add(qName);
            cls = org.kepler.executionWS.xsd.KeplerWfOutput[].class;
            cachedSerClasses.add(cls);
            qName = new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", "KeplerWfOutput");
            qName2 = new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", "return");
            cachedSerFactories.add(new org.apache.axis.encoding.ser.ArraySerializerFactory(qName, qName2));
            cachedDeserFactories.add(new org.apache.axis.encoding.ser.ArrayDeserializerFactory());

            qName = new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", ">getStatus");
            cachedSerQNames.add(qName);
            cls = org.kepler.executionWS.xsd.GetStatus.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", ">getStatusResponse");
            cachedSerQNames.add(qName);
            cls = org.kepler.executionWS.xsd.GetStatusResponse.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", ">main");
            cachedSerQNames.add(qName);
            cls = java.lang.String[].class;
            cachedSerClasses.add(cls);
            qName = new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string");
            qName2 = new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", "argus");
            cachedSerFactories.add(new org.apache.axis.encoding.ser.ArraySerializerFactory(qName, qName2));
            cachedDeserFactories.add(new org.apache.axis.encoding.ser.ArrayDeserializerFactory());

            qName = new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", ">pauseExecution");
            cachedSerQNames.add(qName);
            cls = org.kepler.executionWS.xsd.PauseExecution.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", ">pauseExecutionResponse");
            cachedSerQNames.add(qName);
            cls = org.kepler.executionWS.xsd.PauseExecutionResponse.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", ">resumeExecution");
            cachedSerQNames.add(qName);
            cls = org.kepler.executionWS.xsd.ResumeExecution.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", ">resumeExecutionResponse");
            cachedSerQNames.add(qName);
            cls = org.kepler.executionWS.xsd.ResumeExecutionResponse.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", ">startExeByAttach");
            cachedSerQNames.add(qName);
            cls = org.kepler.executionWS.xsd.StartExeByAttach.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", ">startExeByAttachResponse");
            cachedSerQNames.add(qName);
            cls = org.kepler.executionWS.xsd.StartExeByAttachResponse.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", ">startExeByAttachWithPara");
            cachedSerQNames.add(qName);
            cls = org.kepler.executionWS.xsd.StartExeByAttachWithPara.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", ">startExeByAttachWithParaResponse");
            cachedSerQNames.add(qName);
            cls = org.kepler.executionWS.xsd.StartExeByAttachWithParaResponse.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", ">startExeByContent");
            cachedSerQNames.add(qName);
            cls = org.kepler.executionWS.xsd.StartExeByContent.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", ">startExeByContentResponse");
            cachedSerQNames.add(qName);
            cls = org.kepler.executionWS.xsd.StartExeByContentResponse.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", ">startExeByContentWithPara");
            cachedSerQNames.add(qName);
            cls = org.kepler.executionWS.xsd.StartExeByContentWithPara.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", ">startExeByContentWithParaResponse");
            cachedSerQNames.add(qName);
            cls = org.kepler.executionWS.xsd.StartExeByContentWithParaResponse.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", ">startExeByURIWithPara");
            cachedSerQNames.add(qName);
            cls = org.kepler.executionWS.xsd.StartExeByURIWithPara.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", ">startExeByURIWithParaResponse");
            cachedSerQNames.add(qName);
            cls = org.kepler.executionWS.xsd.StartExeByURIWithParaResponse.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", ">stopExecution");
            cachedSerQNames.add(qName);
            cls = org.kepler.executionWS.xsd.StopExecution.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", ">stopExecutionResponse");
            cachedSerQNames.add(qName);
            cls = org.kepler.executionWS.xsd.StopExecutionResponse.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", ">uploadFilesByAttach");
            cachedSerQNames.add(qName);
            cls = org.kepler.executionWS.xsd.UploadFilesByAttach.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", ">uploadFilesByAttachResponse");
            cachedSerQNames.add(qName);
            cls = org.kepler.executionWS.xsd.UploadFilesByAttachResponse.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", "Exception");
            cachedSerQNames.add(qName);
            cls = org.kepler.executionWS.xsd.Exception.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", "KeplerWfOutput");
            cachedSerQNames.add(qName);
            cls = org.kepler.executionWS.xsd.KeplerWfOutput.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", "KeplerWfParameter");
            cachedSerQNames.add(qName);
            cls = org.kepler.executionWS.xsd.KeplerWfParameter.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://io.java/xsd", "FileNotFoundException");
            cachedSerQNames.add(qName);
            cls = edu.ucsb.nceas.shared.xsd.FileNotFoundException.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://io.java/xsd", "IOException");
            cachedSerQNames.add(qName);
            cls = edu.ucsb.nceas.shared.xsd.IOException.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

    }

    protected org.apache.axis.client.Call createCall() throws java.rmi.RemoteException {
        try {
            org.apache.axis.client.Call _call = super._createCall();
            if (super.maintainSessionSet) {
                _call.setMaintainSession(super.maintainSession);
            }
            if (super.cachedUsername != null) {
                _call.setUsername(super.cachedUsername);
            }
            if (super.cachedPassword != null) {
                _call.setPassword(super.cachedPassword);
            }
            if (super.cachedEndpoint != null) {
                _call.setTargetEndpointAddress(super.cachedEndpoint);
            }
            if (super.cachedTimeout != null) {
                _call.setTimeout(super.cachedTimeout);
            }
            if (super.cachedPortName != null) {
                _call.setPortName(super.cachedPortName);
            }
            java.util.Enumeration keys = super.cachedProperties.keys();
            while (keys.hasMoreElements()) {
                java.lang.String key = (java.lang.String) keys.nextElement();
                _call.setProperty(key, super.cachedProperties.get(key));
            }
            // All the type mapping information is registered
            // when the first call is made.
            // The type mapping information is actually registered in
            // the TypeMappingRegistry of the service, which
            // is the reason why registration is only needed for the first call.
            synchronized (this) {
                if (firstCall()) {
                    // must set encoding style before registering serializers
                    _call.setEncodingStyle(null);
                    for (int i = 0; i < cachedSerFactories.size(); ++i) {
                        java.lang.Class cls = (java.lang.Class) cachedSerClasses.get(i);
                        javax.xml.namespace.QName qName =
                                (javax.xml.namespace.QName) cachedSerQNames.get(i);
                        java.lang.Object x = cachedSerFactories.get(i);
                        if (x instanceof Class) {
                            java.lang.Class sf = (java.lang.Class)
                                 cachedSerFactories.get(i);
                            java.lang.Class df = (java.lang.Class)
                                 cachedDeserFactories.get(i);
                            _call.registerTypeMapping(cls, qName, sf, df, false);
                        }
                        else if (x instanceof javax.xml.rpc.encoding.SerializerFactory) {
                            org.apache.axis.encoding.SerializerFactory sf = (org.apache.axis.encoding.SerializerFactory)
                                 cachedSerFactories.get(i);
                            org.apache.axis.encoding.DeserializerFactory df = (org.apache.axis.encoding.DeserializerFactory)
                                 cachedDeserFactories.get(i);
                            _call.registerTypeMapping(cls, qName, sf, df, false);
                        }
                    }
                }
            }
            return _call;
        }
        catch (java.lang.Throwable _t) {
            throw new org.apache.axis.AxisFault("Failure trying to get the Call object", _t);
        }
    }

    public java.lang.String startExeByURI(java.lang.String wfURI) throws java.rmi.RemoteException, org.kepler.executionWS.xsd.ExceptionType0, org.kepler.executionWS.xsd.FileNotFoundException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[0]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("urn:startExeByURI");
        _call.setEncodingStyle(null);
        _call.setProperty(org.apache.axis.client.Call.SEND_TYPE_ATTR, Boolean.FALSE);
        _call.setProperty(org.apache.axis.AxisEngine.PROP_DOMULTIREFS, Boolean.FALSE);
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP12_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", "startExeByURI"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {wfURI});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        else {
            extractAttachments(_call);
            try {
                return (java.lang.String) _resp;
            } catch (java.lang.Exception _exception) {
                return (java.lang.String) org.apache.axis.utils.JavaUtils.convert(_resp, java.lang.String.class);
            }
        }
  } catch (org.apache.axis.AxisFault axisFaultException) {
    if (axisFaultException.detail != null) {
        if (axisFaultException.detail instanceof java.rmi.RemoteException) {
              throw (java.rmi.RemoteException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof org.kepler.executionWS.xsd.ExceptionType0) {
              throw (org.kepler.executionWS.xsd.ExceptionType0) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof org.kepler.executionWS.xsd.FileNotFoundException) {
              throw (org.kepler.executionWS.xsd.FileNotFoundException) axisFaultException.detail;
         }
   }
  throw axisFaultException;
}
    }

    public java.lang.Object startExeByAttach(java.lang.Object startExeByAttach) throws java.rmi.RemoteException, org.kepler.executionWS.xsd.ExceptionType0 {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[1]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("urn:startExeByAttach");
        _call.setEncodingStyle(null);
        _call.setProperty(org.apache.axis.client.Call.SEND_TYPE_ATTR, Boolean.FALSE);
        _call.setProperty(org.apache.axis.AxisEngine.PROP_DOMULTIREFS, Boolean.FALSE);
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP12_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", "startExeByAttach"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {startExeByAttach});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        else {
            extractAttachments(_call);
            try {
                return (java.lang.Object) _resp;
            } catch (java.lang.Exception _exception) {
                return (java.lang.Object) org.apache.axis.utils.JavaUtils.convert(_resp, java.lang.Object.class);
            }
        }
  } catch (org.apache.axis.AxisFault axisFaultException) {
    if (axisFaultException.detail != null) {
        if (axisFaultException.detail instanceof java.rmi.RemoteException) {
              throw (java.rmi.RemoteException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof org.kepler.executionWS.xsd.ExceptionType0) {
              throw (org.kepler.executionWS.xsd.ExceptionType0) axisFaultException.detail;
         }
   }
  throw axisFaultException;
}
    }

    public void main(java.lang.String[] argus) throws java.rmi.RemoteException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[2]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("urn:main");
        _call.setEncodingStyle(null);
        _call.setProperty(org.apache.axis.client.Call.SEND_TYPE_ATTR, Boolean.FALSE);
        _call.setProperty(org.apache.axis.AxisEngine.PROP_DOMULTIREFS, Boolean.FALSE);
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP12_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", "main"));

        setRequestHeaders(_call);
        setAttachments(_call);
        _call.invokeOneWay(new java.lang.Object[] {argus});

    }

    public java.lang.String startExeByContent(java.lang.String wfContent) throws java.rmi.RemoteException, org.kepler.executionWS.xsd.ExceptionType0 {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[3]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("urn:startExeByContent");
        _call.setEncodingStyle(null);
        _call.setProperty(org.apache.axis.client.Call.SEND_TYPE_ATTR, Boolean.FALSE);
        _call.setProperty(org.apache.axis.AxisEngine.PROP_DOMULTIREFS, Boolean.FALSE);
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP12_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", "startExeByContent"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {wfContent});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        else {
            extractAttachments(_call);
            try {
                return (java.lang.String) _resp;
            } catch (java.lang.Exception _exception) {
                return (java.lang.String) org.apache.axis.utils.JavaUtils.convert(_resp, java.lang.String.class);
            }
        }
  } catch (org.apache.axis.AxisFault axisFaultException) {
    if (axisFaultException.detail != null) {
        if (axisFaultException.detail instanceof java.rmi.RemoteException) {
              throw (java.rmi.RemoteException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof org.kepler.executionWS.xsd.ExceptionType0) {
              throw (org.kepler.executionWS.xsd.ExceptionType0) axisFaultException.detail;
         }
   }
  throw axisFaultException;
}
    }

    public java.lang.Boolean pauseExecution(java.lang.String wfID) throws java.rmi.RemoteException, org.kepler.executionWS.xsd.ExceptionType0 {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[4]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("urn:pauseExecution");
        _call.setEncodingStyle(null);
        _call.setProperty(org.apache.axis.client.Call.SEND_TYPE_ATTR, Boolean.FALSE);
        _call.setProperty(org.apache.axis.AxisEngine.PROP_DOMULTIREFS, Boolean.FALSE);
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP12_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", "pauseExecution"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {wfID});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        else {
            extractAttachments(_call);
            try {
                return (java.lang.Boolean) _resp;
            } catch (java.lang.Exception _exception) {
                return (java.lang.Boolean) org.apache.axis.utils.JavaUtils.convert(_resp, java.lang.Boolean.class);
            }
        }
  } catch (org.apache.axis.AxisFault axisFaultException) {
    if (axisFaultException.detail != null) {
        if (axisFaultException.detail instanceof java.rmi.RemoteException) {
              throw (java.rmi.RemoteException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof org.kepler.executionWS.xsd.ExceptionType0) {
              throw (org.kepler.executionWS.xsd.ExceptionType0) axisFaultException.detail;
         }
   }
  throw axisFaultException;
}
    }

    public java.lang.Object startExeByAttachWithPara(java.lang.Object startExeByAttachWithPara) throws java.rmi.RemoteException, org.kepler.executionWS.xsd.ExceptionType0 {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[5]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("urn:startExeByAttachWithPara");
        _call.setEncodingStyle(null);
        _call.setProperty(org.apache.axis.client.Call.SEND_TYPE_ATTR, Boolean.FALSE);
        _call.setProperty(org.apache.axis.AxisEngine.PROP_DOMULTIREFS, Boolean.FALSE);
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP12_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", "startExeByAttachWithPara"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {startExeByAttachWithPara});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        else {
            extractAttachments(_call);
            try {
                return (java.lang.Object) _resp;
            } catch (java.lang.Exception _exception) {
                return (java.lang.Object) org.apache.axis.utils.JavaUtils.convert(_resp, java.lang.Object.class);
            }
        }
  } catch (org.apache.axis.AxisFault axisFaultException) {
    if (axisFaultException.detail != null) {
        if (axisFaultException.detail instanceof java.rmi.RemoteException) {
              throw (java.rmi.RemoteException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof org.kepler.executionWS.xsd.ExceptionType0) {
              throw (org.kepler.executionWS.xsd.ExceptionType0) axisFaultException.detail;
         }
   }
  throw axisFaultException;
}
    }

    public java.lang.Object executeByAttachWithPara(java.lang.Object executeByAttachWithPara) throws java.rmi.RemoteException, org.kepler.executionWS.xsd.ExceptionType0 {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[6]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("urn:executeByAttachWithPara");
        _call.setEncodingStyle(null);
        _call.setProperty(org.apache.axis.client.Call.SEND_TYPE_ATTR, Boolean.FALSE);
        _call.setProperty(org.apache.axis.AxisEngine.PROP_DOMULTIREFS, Boolean.FALSE);
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP12_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", "executeByAttachWithPara"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {executeByAttachWithPara});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        else {
            extractAttachments(_call);
            try {
                return (java.lang.Object) _resp;
            } catch (java.lang.Exception _exception) {
                return (java.lang.Object) org.apache.axis.utils.JavaUtils.convert(_resp, java.lang.Object.class);
            }
        }
  } catch (org.apache.axis.AxisFault axisFaultException) {
    if (axisFaultException.detail != null) {
        if (axisFaultException.detail instanceof java.rmi.RemoteException) {
              throw (java.rmi.RemoteException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof org.kepler.executionWS.xsd.ExceptionType0) {
              throw (org.kepler.executionWS.xsd.ExceptionType0) axisFaultException.detail;
         }
   }
  throw axisFaultException;
}
    }

    public java.lang.Object executeByAttach(java.lang.Object executeByAttach) throws java.rmi.RemoteException, org.kepler.executionWS.xsd.ExceptionType0 {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[7]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("urn:executeByAttach");
        _call.setEncodingStyle(null);
        _call.setProperty(org.apache.axis.client.Call.SEND_TYPE_ATTR, Boolean.FALSE);
        _call.setProperty(org.apache.axis.AxisEngine.PROP_DOMULTIREFS, Boolean.FALSE);
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP12_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", "executeByAttach"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {executeByAttach});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        else {
            extractAttachments(_call);
            try {
                return (java.lang.Object) _resp;
            } catch (java.lang.Exception _exception) {
                return (java.lang.Object) org.apache.axis.utils.JavaUtils.convert(_resp, java.lang.Object.class);
            }
        }
  } catch (org.apache.axis.AxisFault axisFaultException) {
    if (axisFaultException.detail != null) {
        if (axisFaultException.detail instanceof java.rmi.RemoteException) {
              throw (java.rmi.RemoteException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof org.kepler.executionWS.xsd.ExceptionType0) {
              throw (org.kepler.executionWS.xsd.ExceptionType0) axisFaultException.detail;
         }
   }
  throw axisFaultException;
}
    }

    public java.lang.String startExeByURIWithPara(java.lang.String wfURI, org.kepler.executionWS.xsd.KeplerWfParameter[] wfParas) throws java.rmi.RemoteException, org.kepler.executionWS.xsd.ExceptionType0 {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[8]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("urn:startExeByURIWithPara");
        _call.setEncodingStyle(null);
        _call.setProperty(org.apache.axis.client.Call.SEND_TYPE_ATTR, Boolean.FALSE);
        _call.setProperty(org.apache.axis.AxisEngine.PROP_DOMULTIREFS, Boolean.FALSE);
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP12_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", "startExeByURIWithPara"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {wfURI, wfParas});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        else {
            extractAttachments(_call);
            try {
                return (java.lang.String) _resp;
            } catch (java.lang.Exception _exception) {
                return (java.lang.String) org.apache.axis.utils.JavaUtils.convert(_resp, java.lang.String.class);
            }
        }
  } catch (org.apache.axis.AxisFault axisFaultException) {
    if (axisFaultException.detail != null) {
        if (axisFaultException.detail instanceof java.rmi.RemoteException) {
              throw (java.rmi.RemoteException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof org.kepler.executionWS.xsd.ExceptionType0) {
              throw (org.kepler.executionWS.xsd.ExceptionType0) axisFaultException.detail;
         }
   }
  throw axisFaultException;
}
    }

    public org.kepler.executionWS.xsd.KeplerWfOutput[] getResults(java.lang.String wfID) throws java.rmi.RemoteException, org.kepler.executionWS.xsd.ExceptionType0, org.kepler.executionWS.xsd.FileNotFoundException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[9]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("urn:getResults");
        _call.setEncodingStyle(null);
        _call.setProperty(org.apache.axis.client.Call.SEND_TYPE_ATTR, Boolean.FALSE);
        _call.setProperty(org.apache.axis.AxisEngine.PROP_DOMULTIREFS, Boolean.FALSE);
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP12_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", "getResults"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {wfID});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        else {
            extractAttachments(_call);
            try {
                return (org.kepler.executionWS.xsd.KeplerWfOutput[]) _resp;
            } catch (java.lang.Exception _exception) {
                return (org.kepler.executionWS.xsd.KeplerWfOutput[]) org.apache.axis.utils.JavaUtils.convert(_resp, org.kepler.executionWS.xsd.KeplerWfOutput[].class);
            }
        }
  } catch (org.apache.axis.AxisFault axisFaultException) {
    if (axisFaultException.detail != null) {
        if (axisFaultException.detail instanceof java.rmi.RemoteException) {
              throw (java.rmi.RemoteException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof org.kepler.executionWS.xsd.ExceptionType0) {
              throw (org.kepler.executionWS.xsd.ExceptionType0) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof org.kepler.executionWS.xsd.FileNotFoundException) {
              throw (org.kepler.executionWS.xsd.FileNotFoundException) axisFaultException.detail;
         }
   }
  throw axisFaultException;
}
    }

    public java.lang.String execute(java.lang.String karLSID) throws java.rmi.RemoteException, org.kepler.executionWS.xsd.ExceptionType0 {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[10]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("urn:execute");
        _call.setEncodingStyle(null);
        _call.setProperty(org.apache.axis.client.Call.SEND_TYPE_ATTR, Boolean.FALSE);
        _call.setProperty(org.apache.axis.AxisEngine.PROP_DOMULTIREFS, Boolean.FALSE);
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP12_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", "execute"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {karLSID});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        else {
            extractAttachments(_call);
            try {
                return (java.lang.String) _resp;
            } catch (java.lang.Exception _exception) {
                return (java.lang.String) org.apache.axis.utils.JavaUtils.convert(_resp, java.lang.String.class);
            }
        }
  } catch (org.apache.axis.AxisFault axisFaultException) {
    if (axisFaultException.detail != null) {
        if (axisFaultException.detail instanceof java.rmi.RemoteException) {
              throw (java.rmi.RemoteException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof org.kepler.executionWS.xsd.ExceptionType0) {
              throw (org.kepler.executionWS.xsd.ExceptionType0) axisFaultException.detail;
         }
   }
  throw axisFaultException;
}
    }

    public org.kepler.executionWS.xsd.KeplerWfOutput[] executeByURI(java.lang.String wfURI) throws java.rmi.RemoteException, org.kepler.executionWS.xsd.ExceptionType0, org.kepler.executionWS.xsd.FileNotFoundException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[11]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("urn:executeByURI");
        _call.setEncodingStyle(null);
        _call.setProperty(org.apache.axis.client.Call.SEND_TYPE_ATTR, Boolean.FALSE);
        _call.setProperty(org.apache.axis.AxisEngine.PROP_DOMULTIREFS, Boolean.FALSE);
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP12_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", "executeByURI"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {wfURI});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        else {
            extractAttachments(_call);
            try {
                return (org.kepler.executionWS.xsd.KeplerWfOutput[]) _resp;
            } catch (java.lang.Exception _exception) {
                return (org.kepler.executionWS.xsd.KeplerWfOutput[]) org.apache.axis.utils.JavaUtils.convert(_resp, org.kepler.executionWS.xsd.KeplerWfOutput[].class);
            }
        }
  } catch (org.apache.axis.AxisFault axisFaultException) {
    if (axisFaultException.detail != null) {
        if (axisFaultException.detail instanceof java.rmi.RemoteException) {
              throw (java.rmi.RemoteException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof org.kepler.executionWS.xsd.ExceptionType0) {
              throw (org.kepler.executionWS.xsd.ExceptionType0) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof org.kepler.executionWS.xsd.FileNotFoundException) {
              throw (org.kepler.executionWS.xsd.FileNotFoundException) axisFaultException.detail;
         }
   }
  throw axisFaultException;
}
    }

    public java.lang.Object getResultsByAttach(java.lang.Object getResultsByAttach) throws java.rmi.RemoteException, org.kepler.executionWS.xsd.ExceptionType0, org.kepler.executionWS.xsd.FileNotFoundException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[12]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("urn:getResultsByAttach");
        _call.setEncodingStyle(null);
        _call.setProperty(org.apache.axis.client.Call.SEND_TYPE_ATTR, Boolean.FALSE);
        _call.setProperty(org.apache.axis.AxisEngine.PROP_DOMULTIREFS, Boolean.FALSE);
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP12_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", "getResultsByAttach"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {getResultsByAttach});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        else {
            extractAttachments(_call);
            try {
                return (java.lang.Object) _resp;
            } catch (java.lang.Exception _exception) {
                return (java.lang.Object) org.apache.axis.utils.JavaUtils.convert(_resp, java.lang.Object.class);
            }
        }
  } catch (org.apache.axis.AxisFault axisFaultException) {
    if (axisFaultException.detail != null) {
        if (axisFaultException.detail instanceof java.rmi.RemoteException) {
              throw (java.rmi.RemoteException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof org.kepler.executionWS.xsd.ExceptionType0) {
              throw (org.kepler.executionWS.xsd.ExceptionType0) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof org.kepler.executionWS.xsd.FileNotFoundException) {
              throw (org.kepler.executionWS.xsd.FileNotFoundException) axisFaultException.detail;
         }
   }
  throw axisFaultException;
}
    }

    public java.lang.String getStatus(java.lang.String workflowRunID) throws java.rmi.RemoteException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[13]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("urn:getStatus");
        _call.setEncodingStyle(null);
        _call.setProperty(org.apache.axis.client.Call.SEND_TYPE_ATTR, Boolean.FALSE);
        _call.setProperty(org.apache.axis.AxisEngine.PROP_DOMULTIREFS, Boolean.FALSE);
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP12_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", "getStatus"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {workflowRunID});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        else {
            extractAttachments(_call);
            try {
                return (java.lang.String) _resp;
            } catch (java.lang.Exception _exception) {
                return (java.lang.String) org.apache.axis.utils.JavaUtils.convert(_resp, java.lang.String.class);
            }
        }
  } catch (org.apache.axis.AxisFault axisFaultException) {
  throw axisFaultException;
}
    }

    public java.lang.String startExeByContentWithPara(java.lang.String wfContent, org.kepler.executionWS.xsd.KeplerWfParameter[] wfParas) throws java.rmi.RemoteException, org.kepler.executionWS.xsd.ExceptionType0 {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[14]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("urn:startExeByContentWithPara");
        _call.setEncodingStyle(null);
        _call.setProperty(org.apache.axis.client.Call.SEND_TYPE_ATTR, Boolean.FALSE);
        _call.setProperty(org.apache.axis.AxisEngine.PROP_DOMULTIREFS, Boolean.FALSE);
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP12_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", "startExeByContentWithPara"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {wfContent, wfParas});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        else {
            extractAttachments(_call);
            try {
                return (java.lang.String) _resp;
            } catch (java.lang.Exception _exception) {
                return (java.lang.String) org.apache.axis.utils.JavaUtils.convert(_resp, java.lang.String.class);
            }
        }
  } catch (org.apache.axis.AxisFault axisFaultException) {
    if (axisFaultException.detail != null) {
        if (axisFaultException.detail instanceof java.rmi.RemoteException) {
              throw (java.rmi.RemoteException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof org.kepler.executionWS.xsd.ExceptionType0) {
              throw (org.kepler.executionWS.xsd.ExceptionType0) axisFaultException.detail;
         }
   }
  throw axisFaultException;
}
    }

    public java.lang.String getExecutionStatus(java.lang.String wfID) throws java.rmi.RemoteException, org.kepler.executionWS.xsd.ExceptionType0 {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[15]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("urn:getExecutionStatus");
        _call.setEncodingStyle(null);
        _call.setProperty(org.apache.axis.client.Call.SEND_TYPE_ATTR, Boolean.FALSE);
        _call.setProperty(org.apache.axis.AxisEngine.PROP_DOMULTIREFS, Boolean.FALSE);
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP12_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", "getExecutionStatus"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {wfID});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        else {
            extractAttachments(_call);
            try {
                return (java.lang.String) _resp;
            } catch (java.lang.Exception _exception) {
                return (java.lang.String) org.apache.axis.utils.JavaUtils.convert(_resp, java.lang.String.class);
            }
        }
  } catch (org.apache.axis.AxisFault axisFaultException) {
    if (axisFaultException.detail != null) {
        if (axisFaultException.detail instanceof java.rmi.RemoteException) {
              throw (java.rmi.RemoteException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof org.kepler.executionWS.xsd.ExceptionType0) {
              throw (org.kepler.executionWS.xsd.ExceptionType0) axisFaultException.detail;
         }
   }
  throw axisFaultException;
}
    }

    public java.lang.Boolean resumeExecution(java.lang.String wfID) throws java.rmi.RemoteException, org.kepler.executionWS.xsd.ExceptionType0 {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[16]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("urn:resumeExecution");
        _call.setEncodingStyle(null);
        _call.setProperty(org.apache.axis.client.Call.SEND_TYPE_ATTR, Boolean.FALSE);
        _call.setProperty(org.apache.axis.AxisEngine.PROP_DOMULTIREFS, Boolean.FALSE);
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP12_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", "resumeExecution"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {wfID});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        else {
            extractAttachments(_call);
            try {
                return (java.lang.Boolean) _resp;
            } catch (java.lang.Exception _exception) {
                return (java.lang.Boolean) org.apache.axis.utils.JavaUtils.convert(_resp, java.lang.Boolean.class);
            }
        }
  } catch (org.apache.axis.AxisFault axisFaultException) {
    if (axisFaultException.detail != null) {
        if (axisFaultException.detail instanceof java.rmi.RemoteException) {
              throw (java.rmi.RemoteException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof org.kepler.executionWS.xsd.ExceptionType0) {
              throw (org.kepler.executionWS.xsd.ExceptionType0) axisFaultException.detail;
         }
   }
  throw axisFaultException;
}
    }

    public java.lang.Boolean stopExecution(java.lang.String wfID) throws java.rmi.RemoteException, org.kepler.executionWS.xsd.ExceptionType0 {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[17]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("urn:stopExecution");
        _call.setEncodingStyle(null);
        _call.setProperty(org.apache.axis.client.Call.SEND_TYPE_ATTR, Boolean.FALSE);
        _call.setProperty(org.apache.axis.AxisEngine.PROP_DOMULTIREFS, Boolean.FALSE);
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP12_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", "stopExecution"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {wfID});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        else {
            extractAttachments(_call);
            try {
                return (java.lang.Boolean) _resp;
            } catch (java.lang.Exception _exception) {
                return (java.lang.Boolean) org.apache.axis.utils.JavaUtils.convert(_resp, java.lang.Boolean.class);
            }
        }
  } catch (org.apache.axis.AxisFault axisFaultException) {
    if (axisFaultException.detail != null) {
        if (axisFaultException.detail instanceof java.rmi.RemoteException) {
              throw (java.rmi.RemoteException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof org.kepler.executionWS.xsd.ExceptionType0) {
              throw (org.kepler.executionWS.xsd.ExceptionType0) axisFaultException.detail;
         }
   }
  throw axisFaultException;
}
    }

    public org.kepler.executionWS.xsd.KeplerWfOutput[] executeByURIWithPara(java.lang.String wfURI, org.kepler.executionWS.xsd.KeplerWfParameter[] wfParas) throws java.rmi.RemoteException, org.kepler.executionWS.xsd.ExceptionType0 {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[18]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("urn:executeByURIWithPara");
        _call.setEncodingStyle(null);
        _call.setProperty(org.apache.axis.client.Call.SEND_TYPE_ATTR, Boolean.FALSE);
        _call.setProperty(org.apache.axis.AxisEngine.PROP_DOMULTIREFS, Boolean.FALSE);
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP12_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", "executeByURIWithPara"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {wfURI, wfParas});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        else {
            extractAttachments(_call);
            try {
                return (org.kepler.executionWS.xsd.KeplerWfOutput[]) _resp;
            } catch (java.lang.Exception _exception) {
                return (org.kepler.executionWS.xsd.KeplerWfOutput[]) org.apache.axis.utils.JavaUtils.convert(_resp, org.kepler.executionWS.xsd.KeplerWfOutput[].class);
            }
        }
  } catch (org.apache.axis.AxisFault axisFaultException) {
    if (axisFaultException.detail != null) {
        if (axisFaultException.detail instanceof java.rmi.RemoteException) {
              throw (java.rmi.RemoteException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof org.kepler.executionWS.xsd.ExceptionType0) {
              throw (org.kepler.executionWS.xsd.ExceptionType0) axisFaultException.detail;
         }
   }
  throw axisFaultException;
}
    }

    public java.lang.Boolean delLogDir(java.lang.String wfID) throws java.rmi.RemoteException, org.kepler.executionWS.xsd.ExceptionType0, org.kepler.executionWS.xsd.FileNotFoundException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[19]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("urn:delLogDir");
        _call.setEncodingStyle(null);
        _call.setProperty(org.apache.axis.client.Call.SEND_TYPE_ATTR, Boolean.FALSE);
        _call.setProperty(org.apache.axis.AxisEngine.PROP_DOMULTIREFS, Boolean.FALSE);
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP12_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", "delLogDir"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {wfID});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        else {
            extractAttachments(_call);
            try {
                return (java.lang.Boolean) _resp;
            } catch (java.lang.Exception _exception) {
                return (java.lang.Boolean) org.apache.axis.utils.JavaUtils.convert(_resp, java.lang.Boolean.class);
            }
        }
  } catch (org.apache.axis.AxisFault axisFaultException) {
    if (axisFaultException.detail != null) {
        if (axisFaultException.detail instanceof java.rmi.RemoteException) {
              throw (java.rmi.RemoteException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof org.kepler.executionWS.xsd.ExceptionType0) {
              throw (org.kepler.executionWS.xsd.ExceptionType0) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof org.kepler.executionWS.xsd.FileNotFoundException) {
              throw (org.kepler.executionWS.xsd.FileNotFoundException) axisFaultException.detail;
         }
   }
  throw axisFaultException;
}
    }

    public org.kepler.executionWS.xsd.KeplerWfOutput[] executeByContent(java.lang.String wfContent) throws java.rmi.RemoteException, org.kepler.executionWS.xsd.ExceptionType0 {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[20]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("urn:executeByContent");
        _call.setEncodingStyle(null);
        _call.setProperty(org.apache.axis.client.Call.SEND_TYPE_ATTR, Boolean.FALSE);
        _call.setProperty(org.apache.axis.AxisEngine.PROP_DOMULTIREFS, Boolean.FALSE);
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP12_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", "executeByContent"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {wfContent});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        else {
            extractAttachments(_call);
            try {
                return (org.kepler.executionWS.xsd.KeplerWfOutput[]) _resp;
            } catch (java.lang.Exception _exception) {
                return (org.kepler.executionWS.xsd.KeplerWfOutput[]) org.apache.axis.utils.JavaUtils.convert(_resp, org.kepler.executionWS.xsd.KeplerWfOutput[].class);
            }
        }
  } catch (org.apache.axis.AxisFault axisFaultException) {
    if (axisFaultException.detail != null) {
        if (axisFaultException.detail instanceof java.rmi.RemoteException) {
              throw (java.rmi.RemoteException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof org.kepler.executionWS.xsd.ExceptionType0) {
              throw (org.kepler.executionWS.xsd.ExceptionType0) axisFaultException.detail;
         }
   }
  throw axisFaultException;
}
    }

    public org.kepler.executionWS.xsd.KeplerWfOutput[] executeByContentWithPara(java.lang.String wfContent, org.kepler.executionWS.xsd.KeplerWfParameter[] wfParas) throws java.rmi.RemoteException, org.kepler.executionWS.xsd.ExceptionType0 {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[21]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("urn:executeByContentWithPara");
        _call.setEncodingStyle(null);
        _call.setProperty(org.apache.axis.client.Call.SEND_TYPE_ATTR, Boolean.FALSE);
        _call.setProperty(org.apache.axis.AxisEngine.PROP_DOMULTIREFS, Boolean.FALSE);
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP12_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", "executeByContentWithPara"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {wfContent, wfParas});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        else {
            extractAttachments(_call);
            try {
                return (org.kepler.executionWS.xsd.KeplerWfOutput[]) _resp;
            } catch (java.lang.Exception _exception) {
                return (org.kepler.executionWS.xsd.KeplerWfOutput[]) org.apache.axis.utils.JavaUtils.convert(_resp, org.kepler.executionWS.xsd.KeplerWfOutput[].class);
            }
        }
  } catch (org.apache.axis.AxisFault axisFaultException) {
    if (axisFaultException.detail != null) {
        if (axisFaultException.detail instanceof java.rmi.RemoteException) {
              throw (java.rmi.RemoteException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof org.kepler.executionWS.xsd.ExceptionType0) {
              throw (org.kepler.executionWS.xsd.ExceptionType0) axisFaultException.detail;
         }
   }
  throw axisFaultException;
}
    }

    public java.lang.Object uploadFilesByAttach(java.lang.Object uploadFilesByAttach) throws java.rmi.RemoteException, org.kepler.executionWS.xsd.ExceptionType0 {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[22]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("urn:uploadFilesByAttach");
        _call.setEncodingStyle(null);
        _call.setProperty(org.apache.axis.client.Call.SEND_TYPE_ATTR, Boolean.FALSE);
        _call.setProperty(org.apache.axis.AxisEngine.PROP_DOMULTIREFS, Boolean.FALSE);
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP12_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", "uploadFilesByAttach"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {uploadFilesByAttach});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        else {
            extractAttachments(_call);
            try {
                return (java.lang.Object) _resp;
            } catch (java.lang.Exception _exception) {
                return (java.lang.Object) org.apache.axis.utils.JavaUtils.convert(_resp, java.lang.Object.class);
            }
        }
  } catch (org.apache.axis.AxisFault axisFaultException) {
    if (axisFaultException.detail != null) {
        if (axisFaultException.detail instanceof java.rmi.RemoteException) {
              throw (java.rmi.RemoteException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof org.kepler.executionWS.xsd.ExceptionType0) {
              throw (org.kepler.executionWS.xsd.ExceptionType0) axisFaultException.detail;
         }
   }
  throw axisFaultException;
}
    }

}
