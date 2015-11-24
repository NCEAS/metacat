/**
 * FileNotFoundException.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.3 Oct 05, 2005 (05:23:37 EDT) WSDL2Java emitter.
 */

package org.kepler.executionWS.xsd;

public class FileNotFoundException  extends org.apache.axis.AxisFault  implements java.io.Serializable {

	private static final long serialVersionUID = 8554993551179967192L;
	
	private edu.ucsb.nceas.shared.xsd.FileNotFoundException fileNotFoundException;

    public FileNotFoundException() {
    }

    public FileNotFoundException(
    		edu.ucsb.nceas.shared.xsd.FileNotFoundException fileNotFoundException) {
        this.fileNotFoundException = fileNotFoundException;
    }


    /**
     * Gets the fileNotFoundException value for this FileNotFoundException.
     * 
     * @return fileNotFoundException
     */
    public edu.ucsb.nceas.shared.xsd.FileNotFoundException getFileNotFoundException() {
        return fileNotFoundException;
    }


    /**
     * Sets the fileNotFoundException value for this FileNotFoundException.
     * 
     * @param fileNotFoundException
     */
    public void setFileNotFoundException(edu.ucsb.nceas.shared.xsd.FileNotFoundException fileNotFoundException) {
        this.fileNotFoundException = fileNotFoundException;
    }

    private java.lang.Object __equalsCalc = null;
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof FileNotFoundException)) return false;
        FileNotFoundException other = (FileNotFoundException) obj;
        if (obj == null) return false;
        if (this == obj) return true;
        if (__equalsCalc != null) {
            return (__equalsCalc == obj);
        }
        __equalsCalc = obj;
        boolean _equals;
        _equals = true && 
            ((this.fileNotFoundException==null && other.getFileNotFoundException()==null) || 
             (this.fileNotFoundException!=null &&
              this.fileNotFoundException.equals(other.getFileNotFoundException())));
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
        if (getFileNotFoundException() != null) {
            _hashCode += getFileNotFoundException().hashCode();
        }
        __hashCodeCalc = false;
        return _hashCode;
    }

    // Type metadata
    private static org.apache.axis.description.TypeDesc typeDesc =
        new org.apache.axis.description.TypeDesc(FileNotFoundException.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", ">FileNotFoundException"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("fileNotFoundException");
        elemField.setXmlName(new javax.xml.namespace.QName("http://executionWS.kepler.org/xsd", "FileNotFoundException"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://io.java/xsd", "FileNotFoundException"));
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


    /**
     * Writes the exception data to the faultDetails
     */
    public void writeDetails(javax.xml.namespace.QName qname, org.apache.axis.encoding.SerializationContext context) throws java.io.IOException {
        context.serialize(qname, null, this);
    }
}
