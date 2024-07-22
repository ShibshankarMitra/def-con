package com.homedepot.supplychain.enterpriselabormanagement.utils;

import java.util.List;

import static com.homedepot.supplychain.enterpriselabormanagement.constants.ElmTransactionBqHeaders.*;

public final class TestConstants {

    public static final String TEST_MESSAGE_ID = "test message id";
    public static final String TEST_ELM_ID = "test elm id";
    public static final String TEST_METRICS = "test metrics";
    public static final String TEST_SOURCE_TRANSACTION_ID = "test source transaction id";
    public static final String TEST_FACILITY_ID = "test facility id";

    //JSON Validation
    public static final String TEST_TEXT_PAYLOAD = "INVALID TEXT";
    public static final String TEST_EMPTY_PAYLOAD = "   ";

    public static final String TEST_MESSAGE_BODY = "test message Body";
    public static final String TEST_MESSAGE_BODY_BLANK = " ";

    //GCP Resources
    public static final String TEST_PROJECT_ID = "test project id";
    public static final String TEST_SUBSCRIPTION_NAME = "test subscription name";
    public static final String TEST_TRANSACTION_TABLE_NAME = "test transaction table name";
    public static final String TEST_ERROR_TABLE_NAME = "test error table name";
    public static final String TEST_DATASET_NAME = "test dataset name";
    public static final String TEST_FLOW_CONTROL_COUNT = "1000";
    public static final String TEST_FLOW_CONTROL_BYTES = "100";

    public static final String TEST_PAYLOAD = "payload";

    public static final List<String> ATTRIBUTE_OPTIONAL_FIELDS = List.of(TASK_ID);
    public static final List<String> LPN_OPTIONAL_FIELDS = List.of(PARENT_LPN_ID, CONTAINER_TYPE);
    public static final List<String> LOCATION_OPTIONAL_FIELDS = List.of(PICK_AREA, PUT_AREA,
            REASON_CODE,ORDER_CATEGORY,SHIPMENT_NUMBER,SHIPMENT_TYPE_ID, SHIPMENT_ROUTE,
            SHIPMENT_STOP,STORE_NUMBER, SERVICE_TYPE, VENDOR_NUMBER, TRAILER_NUMBER, SCAC, LPN_STATUS,
            SHIPMENT_LPN_ERROR_TYPE,MHE_LOADED);
    public static final List<String> SKU_OPTIONAL_FIELDS = List.of(
             SPECIAL_HANDLING, BUILD_ON_METHOD,
            SECURE_METHOD, UNLOAD_TYPE);

    private TestConstants() {
        //Constant class
    }
}
