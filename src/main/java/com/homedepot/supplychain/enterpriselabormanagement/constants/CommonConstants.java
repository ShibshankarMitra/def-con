package com.homedepot.supplychain.enterpriselabormanagement.constants;

public final class CommonConstants {


    //ELM APPLICATION CONSTANTS
    public static final String FILE_PATH_DELIMITER="/";
    public static final String JSON_SCHEMA_DIRECTORY ="json-schema";
    public static final String HDW_DIRECT_SCHEMA_FILE_NAME= "hdw-transaction-direct-schema.json";
    public static final String HDW_INDIRECT_SCHEMA_FILE_NAME= "hdw-transaction-indirect-schema.json";
    public static final String BIG_QUERY_DATETIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS";
    public static final String BIG_QUERY_DATE_FORMAT = "yyyy-MM-dd";
    //PubSub Flow Control settings
    public static final Integer BYTE_MULTIPLIER = 1024;
    //JSON paths
    public static final String ATTRIBUTES_JSON_PATH = "attributes";
    public static final String EVENT_TYPE_JSON_PATH = "event_type";
    //Transaction Events constants
    public static final String DIRECT_EVENT = "DIRECT";
    public static final String INDIRECT_EVENT = "INDIRECT";
    private CommonConstants() {
        //Constant class
    }
}