/**
 * StartExeByAttachWithPara.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.3 Oct 05, 2005 (05:23:37 EDT) WSDL2Java emitter.
 */

package org.kepler.executionWS.xsd;

public class StartExeByAttachWithPara  implements java.io.Serializable {
    private java.lang.Object startExeByAttachWithPara;

    public StartExeByAttachWithPara() {
    }

    public StartExeByAttachWithPara(
           java.lang.Object startExeByAttachWithPara) {
           this.startExeByAttachWithPara = startExeByAttachWithPara;
    }


    /**
     * Gets the startExeByAttachWithPara value for this StartExeByAttachWithPara.
     * 
     * @return startExeByAttachWithPara
     */
    public java.lang.Object getStartExeByAttachWithPara() {
        return startExeByAttachWithPara;
    }


    /**
     * Sets the startExeByAttachWithPara value for this StartExeByAttachWithPara.
     * 
     * @param startExeByAttachWithPara
     */
    public void setStartExeByAttachWithPara(java.lang.Object startExeByAttachWithPara) {
        this.startExeByAttachWithPara = startExeByAttachWithPara;
    }

    private java.lang.Object __equalsCalc = null;
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof StartExeByAttachWithPara)) return false;
        StartExeByAttachWithPara other = (StartExeByAttachWithPara) obj;
        if (obj == null) return false;
        if (this == obj) return true;
        if (__equalsCalc != null) {
            return (__equalsCalc == obj);
        }
        __equalsCalc = obj;
        boolean _equals;
        _equals = true && 
            ((this.startExeByAttachWithPara==null && other.getStartExeByAttachWithPara()==null) || 
             (this.startExeByAttachWithPara!=null &&
              this.startExeByAttachWithPara.equals(other.getStartExeByAttachWithPara())));
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
        if (getStartExeByAttachWithPara() != null) {
            _hashCode += getStartExeByAttachWithPara().hashCode();
        }
        __hashCodeCalc = false;
        return _hashCode;
    }

    // Type metadata
    private static org.apache.axis.description.TypeDesc typeDesc =
        new org.apache.axis.description.TypeDesc(StartExeByAttachWithPara.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", ">startExeByAttachWithPara"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("startExeByAttachWithPara");
        elemField.setXmlName(new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", "startExeByAttachWithPara"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "anyType"));
        elemField.setMinOccurs(0);
        elemField.setNillable(true);
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
