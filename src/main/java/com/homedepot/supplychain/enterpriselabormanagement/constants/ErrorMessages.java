package com.homedepot.supplychain.enterpriselabormanagement.constants;

public final class ErrorMessages {

    //Common Error messages
    public static final String MESSAGE_BODY_BLANK = "Message body is blank. valid JSON is expected ";
    public static final String JSON_READ_FAILED = "Failed to read PubSub input JSON message ";
    public static final String JSON_SCHEMA_VALIDATION_FAILED = "JSON schema validation failed ";
    public static final String ROW_MAPPER_PROCESSING_FAILED = "Failed to map transaction data to bigquery row ";
    public static final String BIGQUERY_INSERT_RESPONSE_ERROR = "BigQuery insertAll failed with response errors";
    public static final String PUBLISH_FAILED = "Failed to publish to topic: %s";
    public static final String QUERY_JOB_INTERRUPTED = "BigQuery job interrupted: ";
    public static final String QUERY_JOB_FAILED = "BigQuery job failed to execute: ";
    private ErrorMessages() {
        //Constant class
    }
}