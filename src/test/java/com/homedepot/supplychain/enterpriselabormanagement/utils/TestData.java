package com.homedepot.supplychain.enterpriselabormanagement.utils;

import com.google.cloud.bigquery.BigQueryError;
import com.google.cloud.bigquery.FieldValue;
import com.google.cloud.bigquery.FieldValueList;
import com.google.cloud.bigquery.TableResult;
import com.homedepot.supplychain.enterpriselabormanagement.constants.CommonConstants;
import com.homedepot.supplychain.enterpriselabormanagement.models.hdw.Data;
import com.homedepot.supplychain.enterpriselabormanagement.models.hdw.HdwTransaction;
import com.homedepot.supplychain.enterpriselabormanagement.models.hdw.LaborEvent;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static com.homedepot.supplychain.enterpriselabormanagement.constants.ElmTransactionBqHeaders.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public final class TestData {

    public static final String TEST_MESSAGE_ID = "test message id";

    public static final String TEST_COLUMN_VALUE = "test";
    //JSON Validation
    public static final String TEST_TEXT_PAYLOAD = "INVALID TEXT";
    public static final String TEST_EMPTY_PAYLOAD = "   ";
    public static final String TEST_CONTRACT_VERSION = "0.1";

    public static final String TEST_MESSAGE_BODY_BLANK = " ";

    //GCP Resources
    public static final String TEST_PROJECT_ID = "test project id";
    public static final String TEST_SUBSCRIPTION_NAME = "test subscription name";
    public static final String TEST_TRANSACTION_TABLE_NAME = "test transaction table name";
    public static final String TEST_CICO_TABLE_NAME = "test transaction table name";
    public static final String TEST_CICO_SUMMARY_TABLE_NAME = "cico_summary";
    public static final String TEST_DATASET_NAME = "test dataset name";
    public static final String TEST_FLOW_CONTROL_COUNT = "1000";
    public static final String TEST_FLOW_CONTROL_BYTES = "100";
    public static final String TEST_CONSUMER_TOPIC_NAME = "test";
    public static final String TEST_PAYLOAD = "payload";
    public static final String TEST_VIEW_NAME = "test-view";
    public static final String TEST_TRACE_ID = "7cee3651-f5e4-46f5-8731-f604d86afb55";
    public static final String TEST_DC_NUMBER = "5434";
    public static final String TEST_EVENT_TYPE = "TEST_EVENT";
    public static final String TEST_CICO_EVENT_TYPE = "CICO";
    public static final String TEST_TRANSACTION_ID = "08ac8-46b76-8aca";
    public static final String TEST_PUNCH_DATE = "2024-01-11";
    public static final String TEST_LAST_EVENT_PUNCH_DATE = "2024-01-10";
    public static final String TEST_PUNCH_LOCAL_TIMESTAMP = "2024-01-11T15:54:58.813000";
    public static final String TEST_TRANSACTIONS_TIMESTAMP = "2024-01-11T16:54:58.813000";
    public static final String TEST_LAST_PUNCH_LOCAL_TIMESTAMP = "2024-01-11T23:54:58.813000";
    public static final String TEST_PUNCH_LOCAL_TIMESTAMP_PREVIOUS_DAY = "2024-01-11T02:54:58.813000";
    public static final String TEST_THRESHOLD_HOURS = "14";
    public static final float TEST_TOTAL_HOURS_WORKED_FLOAT = 2;
    public static final String TEST_USER_ID = "test id";
    public static final String TEST_USER_NAME = "test user";
    public static final String TEST_PUNCH_TYPE_IN = "IN";
    public static final String TEST_PUNCH_TYPE_OUT = "OUT";
    public static final String TEST_ERROR_MESSAGE = "Operation Failed";
    public static final String TEST_ENTITY = "Test Entity";

    private TestData() {
        //Constant class
    }

    public static List<Map<String, Object>> getRowMapperList() {
        List<Map<String, Object>> rowMapperList = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            Map<String, Object> rowMap = new LinkedHashMap<>();
            rowMap.put(ELM_ID, TEST_COLUMN_VALUE);
            rowMap.put(BQ_CREATE_DTTM, TEST_COLUMN_VALUE);
            rowMap.put(CONTRACT_VERSION, TEST_COLUMN_VALUE);
            rowMap.put(SOURCE, TEST_COLUMN_VALUE);
            rowMap.put(EVENT_TYPE, TEST_COLUMN_VALUE);
            rowMap.put(PLATFORM, TEST_COLUMN_VALUE);
            rowMap.put(DC_NUMBER, TEST_COLUMN_VALUE);
            rowMap.put(ACTIVITY, TEST_COLUMN_VALUE);
            rowMap.put(ACTION, TEST_COLUMN_VALUE);
            rowMap.put(TRACE_ID, TEST_COLUMN_VALUE);
            rowMap.put(TASK_ID, TEST_COLUMN_VALUE);
            rowMap.put(PARTITION_DATE, TEST_COLUMN_VALUE);
            rowMap.put(PUBLISH_TIMESTAMP, TEST_COLUMN_VALUE);
            rowMap.put(USER_ID, TEST_COLUMN_VALUE);
            rowMap.put(LDAP_ID, TEST_COLUMN_VALUE);
            rowMap.put(TRANSACTION_ID, TEST_COLUMN_VALUE);
            rowMap.put(ASSIGNED_VEHICLE, TEST_COLUMN_VALUE);
            rowMap.put(VEHICLE_ID, TEST_COLUMN_VALUE);
            rowMap.put(PARENT_LPN_ID, TEST_COLUMN_VALUE);
            rowMap.put(LPN_NUMBER, TEST_COLUMN_VALUE);
            rowMap.put(CONTAINER_TYPE, TEST_COLUMN_VALUE);
            rowMap.put(TRANSACTION_TIMESTAMP, TEST_COLUMN_VALUE);
            rowMap.put(START_LOCATION, TEST_COLUMN_VALUE);
            rowMap.put(END_LOCATION, TEST_COLUMN_VALUE);
            rowMap.put(START_ZONE, TEST_COLUMN_VALUE);
            rowMap.put(END_ZONE, TEST_COLUMN_VALUE);
            rowMap.put(START_LOCATION_TYPE, TEST_COLUMN_VALUE);
            rowMap.put(END_LOCATION_TYPE, TEST_COLUMN_VALUE);
            rowMap.put(PICK_AREA, TEST_COLUMN_VALUE);
            rowMap.put(PUT_AREA, TEST_COLUMN_VALUE);
            rowMap.put(SKU_NUMBER, TEST_COLUMN_VALUE);
            rowMap.put(BUILD_ID, TEST_COLUMN_VALUE);
            rowMap.put(SKU_DESCRIPTION, TEST_COLUMN_VALUE);
            rowMap.put(DEPARTMENT, TEST_COLUMN_VALUE);
            rowMap.put(SKU_CLASS, TEST_COLUMN_VALUE);
            rowMap.put(SKU_SUB_CLASS, TEST_COLUMN_VALUE);
            rowMap.put(WEIGHT, TEST_COLUMN_VALUE);
            rowMap.put(LENGTH, TEST_COLUMN_VALUE);
            rowMap.put(WIDTH, TEST_COLUMN_VALUE);
            rowMap.put(HEIGHT, TEST_COLUMN_VALUE);
            rowMap.put(VOLUME, TEST_COLUMN_VALUE);
            rowMap.put(WEIGHT_UOM, TEST_COLUMN_VALUE);
            rowMap.put(SIZE_UOM, TEST_COLUMN_VALUE);
            rowMap.put(PACKAGE_UNIT_QTY, TEST_COLUMN_VALUE);
            rowMap.put(PACKAGE_EACH_QUANTITY, TEST_COLUMN_VALUE);
            rowMap.put(PACKAGE_PALLET_QUANTITY, TEST_COLUMN_VALUE);
            rowMap.put(PACKAGE_CASE_QUANTITY, TEST_COLUMN_VALUE);
            rowMap.put(PACKAGE_PACK_QUANTITY, TEST_COLUMN_VALUE);
            rowMap.put(SPECIAL_HANDLING, TEST_COLUMN_VALUE);
            rowMap.put(BUILD_ON_METHOD, TEST_COLUMN_VALUE);
            rowMap.put(SECURE_METHOD, TEST_COLUMN_VALUE);
            rowMap.put(UNLOAD_TYPE, TEST_COLUMN_VALUE);
            rowMap.put(LOCATION_UOM, TEST_COLUMN_VALUE);
            rowMap.put(QUANTITY, TEST_COLUMN_VALUE);
            rowMap.put(UOM_QTY, TEST_COLUMN_VALUE);
            rowMap.put(REASON_CODE, TEST_COLUMN_VALUE);
            rowMap.put(INBOUND_OUTBOUND_INDICATOR, TEST_COLUMN_VALUE);
            rowMap.put(ORDER_CATEGORY, TEST_COLUMN_VALUE);
            rowMap.put(SHIPMENT_NUMBER, TEST_COLUMN_VALUE);
            rowMap.put(SHIPMENT_TYPE_ID, TEST_COLUMN_VALUE);
            rowMap.put(SHIPMENT_ROUTE, TEST_COLUMN_VALUE);
            rowMap.put(SHIPMENT_STOP, TEST_COLUMN_VALUE);
            rowMap.put(STORE_NUMBER, TEST_COLUMN_VALUE);
            rowMap.put(SERVICE_TYPE, TEST_COLUMN_VALUE);
            rowMap.put(VENDOR_NUMBER, TEST_COLUMN_VALUE);
            rowMap.put(TRAILER_NUMBER, TEST_COLUMN_VALUE);
            rowMap.put(RAIL_CAR_NUMBER, TEST_COLUMN_VALUE);
            rowMap.put(SCAC, TEST_COLUMN_VALUE);
            rowMap.put(LPN_STATUS, TEST_COLUMN_VALUE);
            rowMap.put(SHIPMENT_LPN_ERROR_TYPE, TEST_COLUMN_VALUE);
            rowMap.put(MHE_LOADED, TEST_COLUMN_VALUE);
            rowMapperList.add(rowMap);
        }
        return rowMapperList;
    }

    public static List<Map<String, Object>> getEmptyRowMapperList() {
        return new ArrayList<>();
    }

    public static Map<Long, List<BigQueryError>> getInsertErrorMap() {
        Map<Long, List<BigQueryError>> errorMap = new HashMap<>();
        errorMap.put(0L, List.of(new BigQueryError("reason1", "location1", "message1")));
        errorMap.put(1L, List.of(new BigQueryError("reason2", "location2", null)));
        return errorMap;
    }

    public static String getEmptyPayload() {
        return TEST_EMPTY_PAYLOAD;
    }

    public static String getTextPayload() {
        return TEST_TEXT_PAYLOAD;
    }

    public static String getValidJsonPayloadPickLpnFromActive() throws IOException {
        return FileUtils.readFileToString(new File("src/test/resources/json-test-data/data-pick-lpn-from-active.json"), StandardCharsets.UTF_8);
    }

    public static String getValidJsonPayloadReplenAllocation() throws IOException {
        return FileUtils.readFileToString(new File("src/test/resources/json-test-data/data-replen-allocation.json"), StandardCharsets.UTF_8);
    }

    public static String getValidJsonPayloadIndirect() throws IOException {
        return FileUtils.readFileToString(new File("src/test/resources/json-test-data/indirect-data.json"), StandardCharsets.UTF_8);
    }

    public static String getValidJsonPayloadWithMultipleBuilds() throws IOException {
        return FileUtils.readFileToString(new File("src/test/resources/json-test-data/data-with-complex-list.json"), StandardCharsets.UTF_8);
    }

    public static String getValidJsonPayloadLoadLpnLm() throws IOException {
        return FileUtils.readFileToString(new File("src/test/resources/json-test-data/data-load-lpn-lm.json"), StandardCharsets.UTF_8);
    }

    public static String getValidJsonPayloadMoveFromLiftToLpnLocationLm() throws IOException {
        return FileUtils.readFileToString(new File("src/test/resources/json-test-data/data-move-from-lift-to-lpn-location-lm.json"), StandardCharsets.UTF_8);
    }

    public static String getValidJsonPayloadMoveToLiftLm() throws IOException {
        return FileUtils.readFileToString(new File("src/test/resources/json-test-data/data-move-to-lift-lm.json"), StandardCharsets.UTF_8);
    }

    public static String getValidJsonPayloadReceiveLpnLm() throws IOException {
        return FileUtils.readFileToString(new File("src/test/resources/json-test-data/data-receive-lpn-lm.json"), StandardCharsets.UTF_8);
    }

    public static String getInvalidFieldsJsonPayload() throws IOException {
        return FileUtils.readFileToString(new File("src/test/resources/json-test-data/data-with-invalid-fields.json"), StandardCharsets.UTF_8);
    }

    public static String getInvalidBuildsJsonPayload() throws IOException {
        return FileUtils.readFileToString(new File("src/test/resources/json-test-data/data-with-invalid-builds.json"), StandardCharsets.UTF_8);
    }

    public static String getDuplicateBuildsJsonPayload() throws IOException {
        return FileUtils.readFileToString(new File("src/test/resources/json-test-data/data-with-duplicate-builds.json"), StandardCharsets.UTF_8);
    }

    public static String getInvalidPackageHierarchyJsonPayload() throws IOException {
        return FileUtils.readFileToString(new File("src/test/resources/json-test-data/data-with-invalid-package-hierarchy.json"), StandardCharsets.UTF_8);
    }

    public static String getDuplicatePackageHierarchyJsonPayload() throws IOException {
        return FileUtils.readFileToString(new File("src/test/resources/json-test-data/data-with-duplicate-package-hierarchy.json"), StandardCharsets.UTF_8);
    }

    public static String getInvalidEventTypeJsonPayload() throws IOException {
        return FileUtils.readFileToString(new File("src/test/resources/json-test-data/indirect-data-with-invalid-event_type.json"), StandardCharsets.UTF_8);
    }

    public static String getValidJsonPayloadCicoEvent() throws IOException {
        return FileUtils.readFileToString(new File("src/test/resources/json-test-data/cico-data.json"), StandardCharsets.UTF_8);
    }

    public static String getMoveFromLiftToLpnLocationLmMissingUom() throws IOException {
        return FileUtils.readFileToString(new File("src/test/resources/json-test-data/data-move-from-lift-to-lpn-location-lm-missing-uom.json"), StandardCharsets.UTF_8);
    }
    public static String getMissingUOMJsonPayloadPickLpnFromActive() throws IOException {
        return FileUtils.readFileToString(new File("src/test/resources/json-test-data/data-pick-lpn-from-active-missing-uom.json"), StandardCharsets.UTF_8);
    }
    public static String getMoveToListLmMissingUom() throws IOException {
        return FileUtils.readFileToString(new File("src/test/resources/json-test-data/data-move-to-lift-lm-missing-location-uom.json"), StandardCharsets.UTF_8);
    }

    public static TableResult mockTableResult() {
        List<FieldValueList> rows = new ArrayList<>();
        FieldValueList fieldValueList = mock(FieldValueList.class);
        FieldValue fieldValue = mock(FieldValue.class);
        FieldValue fieldValue2 = mock(FieldValue.class);
        when(fieldValue.getStringValue()).thenReturn("2024-03-11T07:10:21.865372");
        when(fieldValue.getStringValue()).thenReturn("IN");
        when(fieldValue.getStringValue()).thenReturn("2024-03-11");
        when(fieldValue2.getStringValue()).thenReturn("2024-03-11T07:10:21.865372");
        when(fieldValueList.get("punch_local_time")).thenReturn(fieldValue);
        when(fieldValueList.get("punch_type")).thenReturn(fieldValue2);
        when(fieldValueList.get("punch_date")).thenReturn(fieldValue2);
        when(fieldValueList.get("adjusted_punch_date")).thenReturn(fieldValue2);
        rows.add(fieldValueList);
        TableResult tableResult = mock(TableResult.class);
        when(tableResult.iterateAll()).thenReturn(rows);
        return tableResult;
    }

    public static HdwTransaction getHdwTransaction(String punchType) {
        HdwTransaction hdwTransaction = new HdwTransaction();
        hdwTransaction.setLaborEventDetail(new Data());
        hdwTransaction.setLaborEvent(new LaborEvent());
        hdwTransaction.getLaborEventDetail().setTransactionId(TestData.TEST_TRANSACTION_ID);
        hdwTransaction.getLaborEventDetail().setUserId(TestData.TEST_USER_ID);
        hdwTransaction.getLaborEventDetail().setUserName(TestData.TEST_USER_NAME);
        hdwTransaction.getLaborEventDetail().setUserName(TestData.TEST_USER_ID);
        hdwTransaction.getLaborEventDetail().setTransactionLocalTimestamp(TestData.TEST_PUNCH_LOCAL_TIMESTAMP_PREVIOUS_DAY);
        hdwTransaction.getLaborEventDetail().setTransactionTimestamp(TestData.TEST_TRANSACTIONS_TIMESTAMP);
        hdwTransaction.getLaborEventDetail().setTransactionId(TestData.TEST_TRANSACTION_ID);
        hdwTransaction.getLaborEvent().setActivity(TestData.TEST_PUNCH_TYPE_IN);
        if(CommonConstants.CLOCK_OUT.equals(punchType)) {
            hdwTransaction.getLaborEvent().setActivity(TestData.TEST_PUNCH_TYPE_OUT);
        }else {
            hdwTransaction.getLaborEvent().setActivity(TestData.TEST_PUNCH_TYPE_IN);
        }
        hdwTransaction.getLaborEvent().setDcNumber(TestData.TEST_DC_NUMBER);
        hdwTransaction.getLaborEvent().setTraceId(TestData.TEST_TRACE_ID);
        return hdwTransaction;
    }
}
