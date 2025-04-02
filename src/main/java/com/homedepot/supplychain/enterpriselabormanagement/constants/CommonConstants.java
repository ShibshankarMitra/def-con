package com.homedepot.supplychain.enterpriselabormanagement.constants;

public final class CommonConstants {


    //ELM APPLICATION CONSTANTS
    public static final String ELM_APP = "FULL-ELM";
    public static final String FILE_PATH_DELIMITER="/";
    public static final String JSON_SCHEMA_DIRECTORY ="json-schema";
    public static final String HDW_DIRECT_SCHEMA_FILE_NAME= "hdw-transaction-direct-schema.json";
    public static final String HDW_INDIRECT_SCHEMA_FILE_NAME= "hdw-transaction-indirect-schema.json";
    public static final String HDW_CICO_SCHEMA_FILE_NAME= "hdw-transaction-cico-schema.json";
    public static final String BIG_QUERY_DATETIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS";
    public static final String BIG_QUERY_DATE_FORMAT = "yyyy-MM-dd";
    //PubSub Flow Control settings
    public static final Integer BYTE_MULTIPLIER = 1024;
    //JSON paths
    public static final String LABOR_EVENT_JSON_PATH = "laborEvent";
    public static final String LABOR_EVENT_DETAIL_JSON_PATH = "laborEventDetail";
    public static final String EVENT_TYPE_JSON_PATH = "event_type";
    public static final String TRACE_ID = "trace_id";
    public static final String TRANSACTION_ID = "transaction_id";
    public static final String DC_NUMBER = "dc_number";
    public static final String CONTRACT_VERSION = "contract_version";
    //Transaction Events constants
    public static final String DIRECT_EVENT = "DIRECT";
    public static final String INDIRECT_EVENT = "INDIRECT";
    public static final String CICO_EVENT = "CICO";
    public static final String CLOCK_IN = "IN";
    public static final String CLOCK_OUT = "OUT";
    public static final String CICO_PUNCHES_TABLE = "cico_events";
    public static final String CICO_SUMMARY_TABLE = "cico_summary";
    public static final int DAY_1 = 1;
    public static final double ONE_HOUR_IN_MINUTES = 60.0;
    public static final String CICO_TOTAL_HOURS_PATTERN = "#.##";
    public static final String ERROR_KEY = "error";
    public static final int ROUNDING_DIGIT = 2;
    public static final String COMMA_SEPARATOR = ",";
    public static final String CONTRACT_VERSION_DEFAULT = "0.1";

    private CommonConstants() {
        //Constant class
    }
}