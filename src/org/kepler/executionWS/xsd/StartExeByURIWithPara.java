/**
 * StartExeByURIWithPara.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.3 Oct 05, 2005 (05:23:37 EDT) WSDL2Java emitter.
 */

package org.kepler.executionWS.xsd;

public class StartExeByURIWithPara  implements java.io.Serializable {
    private java.lang.String wfURI;

    private org.kepler.executionWS.xsd.KeplerWfParameter[] wfParas;

    public StartExeByURIWithPara() {
    }

    public StartExeByURIWithPara(
           java.lang.String wfURI,
           org.kepler.executionWS.xsd.KeplerWfParameter[] wfParas) {
           this.wfURI = wfURI;
           this.wfParas = wfParas;
    }


    /**
     * Gets the wfURI value for this StartExeByURIWithPara.
     * 
     * @return wfURI
     */
    public java.lang.String getWfURI() {
        return wfURI;
    }


    /**
     * Sets the wfURI value for this StartExeByURIWithPara.
     * 
     * @param wfURI
     */
    public void setWfURI(java.lang.String wfURI) {
        this.wfURI = wfURI;
    }


    /**
     * Gets the wfParas value for this StartExeByURIWithPara.
     * 
     * @return wfParas
     */
    public org.kepler.executionWS.xsd.KeplerWfParameter[] getWfParas() {
        return wfParas;
    }


    /**
     * Sets the wfParas value for this StartExeByURIWithPara.
     * 
     * @param wfParas
     */
    public void setWfParas(org.kepler.executionWS.xsd.KeplerWfParameter[] wfParas) {
        this.wfParas = wfParas;
    }

    public org.kepler.executionWS.xsd.KeplerWfParameter getWfParas(int i) {
        return this.wfParas[i];
    }

    public void setWfParas(int i, org.kepler.executionWS.xsd.KeplerWfParameter _value) {
        this.wfParas[i] = _value;
    }

    private java.lang.Object __equalsCalc = null;
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof StartExeByURIWithPara)) return false;
        StartExeByURIWithPara other = (StartExeByURIWithPara) obj;
        if (obj == null) return false;
        if (this == obj) return true;
        if (__equalsCalc != null) {
            return (__equalsCalc == obj);
        }
        __equalsCalc = obj;
        boolean _equals;
        _equals = true && 
            ((this.wfURI==null && other.getWfURI()==null) || 
             (this.wfURI!=null &&
              this.wfURI.equals(other.getWfURI()))) &&
            ((this.wfParas==null && other.getWfParas()==null) || 
             (this.wfParas!=null &&
              java.util.Arrays.equals(this.wfParas, other.getWfParas())));
        __equalsCalc = null;
        return _equals;
    }

    private boolean __hashCodeCalc = false;
    public synchronized int hashCode() {
        if (__hashCodeCalc) {
            return 0;
        }
        __hashCodeCalc = true;
        int _hashCode = 1;
        if (getWfURI() != null) {
            _hashCode += getWfURI().hashCode();
        }
        if (getWfParas() != null) {
            for (int i=0;
                 i<java.lang.reflect.Array.getLength(getWfParas());
                 i++) {
                java.lang.Object obj = java.lang.reflect.Array.get(getWfParas(), i);
                if (obj != null &&
                    !obj.getClass().isArray()) {
                    _hashCode += obj.hashCode();
                }
            }
        }
        __hashCodeCalc = false;
        return _hashCode;
    }

    // Type metadata
    private static org.apache.axis.description.TypeDesc typeDesc =
        new org.apache.axis.description.TypeDesc(StartExeByURIWithPara.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", ">startExeByURIWithPara"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("wfURI");
        elemField.setXmlName(new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", "wfURI"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("wfParas");
        elemField.setXmlName(new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", "wfParas"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", "KeplerWfParameter"));
        elemField.setMinOccurs(0);
        elemField.setNillable(true);
        elemField.setMaxOccursUnbounded(true);
        typeDesc.addFieldDesc(elemField);
    }

    /**
     * Return type metadata object
     */
    public static org.apache.axis.description.TypeDesc getTypeDesc() {
        return typeDesc;
    }

    /**
     * Get Custom Serializer
     */
    public static org.apache.axis.encoding.Serializer getSerializer(
           java.lang.String mechType, 
           java.lang.Class _javaType,  
           javax.xml.namespace.QName _xmlType) {
        return 
          new  org.apache.axis.encoding.ser.BeanSerializer(
            _javaType, _xmlType, typeDesc);
    }

    /**
     * Get Custom Deserializer
     */
    public static org.apache.axis.encoding.Deserializer getDeserializer(
           java.lang.String mechType, 
           java.lang.Class _javaType,  
           javax.xml.namespace.QName _xmlType) {
        return 
          new  org.apache.axis.encoding.ser.BeanDeserializer(
            _javaType, _xmlType, typeDesc);
    }

}
