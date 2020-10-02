package edu.ucsb.nceas.metacat.common;

public class Settings {
    //the file to store the latest systemmetadata modification time of the object 
    //which has been harvested during the timed solr-index building (In the IndexGenerator).
    public static final String LASTPROCESSEDDATEFILENAME = "solr-last-proccessed-date";
    public static final String PERFORMANCELOG = "MetacatPerformanceLog ";
    public static final String PERFORMANCELOG_DURASION = " duration ";
    public static final String PERFORMANCELOG_CREATE_UPDATE_METHOD = " create/update method ";
    public static final String PERFORMANCELOG_QUERY_METHOD = " query method ";
    public static final String PERFORMANCELOG_GET_METHOD = " get method ";
    public static final String PERFORMANCELOG_INDEX_METHOD = " index process ";
}
