package edu.ucsb.nceas.metacat.dataone.convert;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import org.dataone.exceptions.MarshallingException;
import org.dataone.service.types.v1.Event;
import org.dataone.service.types.v2.Log;
import org.dataone.service.types.v2.LogEntry;

/**
 * This class represents a converter to convert an DataONE v2 Log object to a v1 Log object.
 * It probably will be removed to d1_common_java in the future. 
 * @author tao
 *
 */
public class LogV2toV1Converter {
    
    /**
     * Default constructor
     */
    public LogV2toV1Converter() {
        
    }
    
    /**
     * Convert a v2 Log object to a v1 Log object
     * @param logV2  - the v2 Log object which needs to be converted
     * @return a v1 Log object. If the logV2 is null, null will be returned.
     * @throws IOException 
     * @throws JiBXException 
     * @throws InvocationTargetException 
     * @throws IllegalAccessException 
     * @throws InstantiationException 
     */
    public org.dataone.service.types.v1.Log convert(Log logV2) 
            throws InstantiationException, IllegalAccessException, InvocationTargetException, MarshallingException, IOException {
        org.dataone.service.types.v1.Log logV1 = null;
        int removedLogCount =0;
        if(logV2 != null) {
            //System.out.println("====================== logV2 is not null and the size is "+logV2.getCount());
            LogEntryV2toV1Converter converter = new LogEntryV2toV1Converter();
            logV1 = new org.dataone.service.types.v1.Log();
            for(int i=0; i<logV2.getCount(); i++) {
                LogEntry v2LogEntry = logV2.getLogEntry(i);
                org.dataone.service.types.v1.LogEntry v1LogEntry = converter.convert(v2LogEntry);
                if(v1LogEntry.getEvent() != null) {
                    logV1.addLogEntry(v1LogEntry);
                } else {
                    removedLogCount ++;
                }
                
            }
            logV1.setCount(logV2.getCount()-removedLogCount);
            logV1.setStart(logV2.getStart());
            logV1.setTotal(logV2.getTotal()-removedLogCount);
        }
        return logV1;
    }
    
    /**
     * A class to convert a v2 LogEntry object to a v1 LogEntry object
     * @author tao
     *
     */
    public static class LogEntryV2toV1Converter {
        /**
         * Default constructor
         */
        public LogEntryV2toV1Converter(){
            
        }
        
        /**
         * Convert a v2 LogEntry object to a v1 LogEntry object
         * @param logV2  - the v2 Log object which needs to be converted
         * @return a v1 Log object. If the logV2 is null, null will be returned.
         * @throws IOException 
         * @throws JiBXException 
         * @throws InvocationTargetException 
         * @throws IllegalAccessException 
         * @throws InstantiationException 
         */
        public org.dataone.service.types.v1.LogEntry convert(LogEntry logEntryV2) 
                throws InstantiationException, IllegalAccessException, InvocationTargetException, MarshallingException, IOException {
            org.dataone.service.types.v1.LogEntry logEntryV1 = null;;
            if(logEntryV2 != null) {
                logEntryV1 = new org.dataone.service.types.v1.LogEntry();
               
                logEntryV1.setDateLogged(logEntryV2.getDateLogged());
                
                logEntryV1.setEntryId(logEntryV2.getEntryId());
              
                if(logEntryV2.getEvent() != null) {
                    logEntryV1.setEvent(Event.convert(logEntryV2.getEvent().toLowerCase()));
                }
                
                logEntryV1.setIdentifier(logEntryV2.getIdentifier());
                
                logEntryV1.setIpAddress(logEntryV2.getIpAddress());
                
                logEntryV1.setNodeIdentifier(logEntryV2.getNodeIdentifier());
                
                logEntryV1.setSubject(logEntryV2.getSubject());
                
                logEntryV1.setUserAgent(logEntryV2.getUserAgent());
                
            }
            return logEntryV1;
        }
    }
    
    /**
     * A wrapper class to gap the difference in the signature of setEvent between the v2 and v1 LogEntry objects.
     * (The v2 LogEntry doesn't have the setEvent(Event) method, so TypeMarshaller can't handle it)
     * @author tao
     *
     */
    public static class V1LogEntryWrapper extends org.dataone.service.types.v1.LogEntry{
        
        public void setEvent(String event) { 
            if(event != null) {
                super.setEvent(Event.convert(event));
            }
        }
    }
    
    /**
     * A wrapper class to gap the difference in the signature of setEvent between the v2 and v1 LogEntry objects.
     * (The v2 LogEntry doesn't have the setEvent(Event) method, so TypeMarshaller can't handle it)
     * @author tao
     *
     */
    public static class V2LogEntryWrapper extends LogEntry{
        
        public void setEvent(Event event) { 
            if(event != null) {
                super.setEvent(event.xmlValue());
            }
        }
    }

}
