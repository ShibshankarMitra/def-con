package com.homedepot.supplychain.enterpriselabormanagement.utils;

import com.google.cloud.bigquery.BigQueryError;
import com.google.cloud.bigquery.Field;
import com.google.cloud.bigquery.Schema;
import com.google.cloud.bigquery.StandardSQLTypeName;
import com.homedepot.supplychain.enterpriselabormanagement.constants.ElmTransactionBqHeaders;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static com.homedepot.supplychain.enterpriselabormanagement.constants.ElmTransactionBqHeaders.*;

public class TestData {

    protected String getMessageId() {
        return TestConstants.TEST_MESSAGE_ID;
    }

    protected List<Map<String, Object>> getRowMapperList() {
        List<Map<String, Object>> rowMapperList = new ArrayList<>();
        for(int i=0;i<3;i++){
            Map<String, Object> rowMap= new LinkedHashMap<>();
            rowMap.put("id", TestConstants.TEST_ELM_ID);
            rowMap.put("source_id",TestConstants.TEST_SOURCE_TRANSACTION_ID);
            rowMap.put("facility_id", TestConstants.TEST_FACILITY_ID);
            rowMap.put("metrics",TestConstants.TEST_METRICS);
            rowMapperList.add(rowMap);
        }
        return rowMapperList;
    }

    protected List<Map<String, Object>> getEmptyRowMapperList() {
        return new ArrayList<>();
    }

    protected Map<Long, List<BigQueryError>> getInsertErrorMap() {
        Map<Long, List<BigQueryError>> errorMap = new HashMap<>();
        errorMap.put(0L, List.of(new BigQueryError("reason1", "location1", "message1")));
        errorMap.put(1L, List.of(new BigQueryError("reason2", "location2", null)));
        return errorMap;
    }

    protected String getEmptyPayload() {
        return TestConstants.TEST_EMPTY_PAYLOAD;
    }

    protected String getTextPayload() {
        return TestConstants.TEST_TEXT_PAYLOAD;
    }

    protected String getValidJsonPayloadPickLpnFromActive() throws IOException {
        return FileUtils.readFileToString(new File("src/test/resources/json-test-data/data-pick-lpn-from-active.json"), StandardCharsets.UTF_8);
    }

    protected String getValidJsonPayloadReplenAllocation() throws IOException {
        return FileUtils.readFileToString(new File("src/test/resources/json-test-data/data-replen-allocation.json"), StandardCharsets.UTF_8);
    }

    protected String getValidJsonPayloadIndirect() throws IOException {
        return FileUtils.readFileToString(new File("src/test/resources/json-test-data/indirect-data.json"), StandardCharsets.UTF_8);
    }

    protected String getValidJsonPayloadwWithMultipleBuilds() throws IOException {
        return FileUtils.readFileToString(new File("src/test/resources/json-test-data/data-with-complex-list.json"), StandardCharsets.UTF_8);
    }

    protected String getInvalidFieldsJsonPayload() throws IOException {
        return FileUtils.readFileToString(new File("src/test/resources/json-test-data/data-with-invalid-fields.json"), StandardCharsets.UTF_8);
    }

    protected String getInvalidBuildsJsonPayload() throws IOException {
        return FileUtils.readFileToString(new File("src/test/resources/json-test-data/data-with-invalid-builds.json"), StandardCharsets.UTF_8);
    }

    protected String getDuplicateBuildsJsonPayload() throws IOException {
        return FileUtils.readFileToString(new File("src/test/resources/json-test-data/data-with-duplicate-builds.json"), StandardCharsets.UTF_8);
    }

    protected String getInvalidPackageHierarchyJsonPayload() throws IOException {
        return FileUtils.readFileToString(new File("src/test/resources/json-test-data/data-with-invalid-package-hierarchy.json"), StandardCharsets.UTF_8);
    }

    protected String getDuplicatePackageHierarchyJsonPayload() throws IOException {
        return FileUtils.readFileToString(new File("src/test/resources/json-test-data/data-with-duplicate-package-hierarchy.json"), StandardCharsets.UTF_8);
    }

    protected String getInvalidEventTypeJsonPayload() throws IOException {
        return FileUtils.readFileToString(new File("src/test/resources/json-test-data/indirect-data-with-invalid-event_type.json"), StandardCharsets.UTF_8);
    }

    protected Schema getElmSchema() {
        return Schema.of(
                Field.of(ELM_ID, StandardSQLTypeName.STRING),
                Field.of(BQ_CREATE_DTTM,StandardSQLTypeName.TIMESTAMP),
                Field.of(CONTRACT_VERSION,StandardSQLTypeName.STRING),
                Field.of(SOURCE,StandardSQLTypeName.STRING),
                Field.of(EVENT_TYPE,StandardSQLTypeName.STRING),
                Field.of(PLATFORM,StandardSQLTypeName.STRING),
                Field.of(DC_NUMBER,StandardSQLTypeName.STRING),
                Field.of(ACTIVITY,StandardSQLTypeName.STRING),
                Field.of(ACTION,StandardSQLTypeName.STRING),
                Field.of(TRACE_ID,StandardSQLTypeName.STRING),
                Field.of(TASK_ID,StandardSQLTypeName.STRING),
                Field.of(PARTITION_DATE,StandardSQLTypeName.DATE),
                Field.of(PUBLISH_TIMESTAMP,StandardSQLTypeName.TIMESTAMP),
                Field.of(USER_ID,StandardSQLTypeName.STRING),
                Field.of(LDAP_ID,StandardSQLTypeName.STRING),
                Field.of(TRANSACTION_ID,StandardSQLTypeName.STRING),
                Field.of(ASSIGNED_VEHICLE,StandardSQLTypeName.STRING),
                Field.of(VEHICLE_ID,StandardSQLTypeName.STRING),
                Field.of(PARENT_LPN_ID,StandardSQLTypeName.STRING),
                Field.of(LPN_NUMBER,StandardSQLTypeName.STRING),
                Field.of(CONTAINER_TYPE,StandardSQLTypeName.STRING),
                Field.of(TRANSACTION_TIMESTAMP,StandardSQLTypeName.TIMESTAMP),
                Field.of(START_LOCATION,StandardSQLTypeName.STRING),
                Field.of(END_LOCATION,StandardSQLTypeName.STRING),
                Field.of(START_ZONE,StandardSQLTypeName.STRING),
                Field.of(END_ZONE,StandardSQLTypeName.STRING),
                Field.of(START_LOCATION_TYPE,StandardSQLTypeName.STRING),
                Field.of(END_LOCATION_TYPE,StandardSQLTypeName.STRING),
                Field.of(PICK_AREA,StandardSQLTypeName.STRING),
                Field.of(PUT_AREA,StandardSQLTypeName.STRING),
                Field.of(SKU_NUMBER,StandardSQLTypeName.STRING),
                Field.of(BUILD_ID,StandardSQLTypeName.STRING),
                Field.of(SKU_DESCRIPTION,StandardSQLTypeName.STRING),
                Field.of(DEPARTMENT,StandardSQLTypeName.STRING),
                Field.of(SKU_CLASS,StandardSQLTypeName.STRING),
                Field.of(SKU_SUB_CLASS,StandardSQLTypeName.STRING),
                Field.of(WEIGHT,StandardSQLTypeName.NUMERIC),
                Field.of(LENGTH,StandardSQLTypeName.NUMERIC),
                Field.of(WIDTH,StandardSQLTypeName.NUMERIC),
                Field.of(HEIGHT,StandardSQLTypeName.NUMERIC),
                Field.of(VOLUME,StandardSQLTypeName.NUMERIC),
                Field.of(WEIGHT_UOM,StandardSQLTypeName.STRING),
                Field.of(SIZE_UOM,StandardSQLTypeName.STRING),
                Field.of(PACKAGE_UNIT_QTY,StandardSQLTypeName.NUMERIC),
                Field.of(PACKAGE_EACH_QTY,StandardSQLTypeName.NUMERIC),
                Field.of(SPECIAL_HANDLING,StandardSQLTypeName.STRING),
                Field.of(BUILD_ON_METHOD,StandardSQLTypeName.STRING),
                Field.of(SECURE_METHOD,StandardSQLTypeName.STRING),
                Field.of(UNLOAD_TYPE,StandardSQLTypeName.STRING),
                Field.of(LOCATION_UOM,StandardSQLTypeName.STRING),
                Field.of(LOCATION_QTY,StandardSQLTypeName.NUMERIC),
                Field.of(UOM_QTY,StandardSQLTypeName.NUMERIC),
                Field.of(REASON_CODE,StandardSQLTypeName.STRING),
                Field.of(INBOUND_OUTBOUND_INDICATOR,StandardSQLTypeName.STRING),
                Field.of(ORDER_CATEGORY,StandardSQLTypeName.STRING),
                Field.of(SHIPMENT_NUMBER,StandardSQLTypeName.STRING),
                Field.of(SHIPMENT_TYPE_ID,StandardSQLTypeName.STRING),
                Field.of(SHIPMENT_ROUTE,StandardSQLTypeName.STRING),
                Field.of(SHIPMENT_STOP,StandardSQLTypeName.STRING),
                Field.of(STORE_NUMBER,StandardSQLTypeName.STRING),
                Field.of(SERVICE_TYPE,StandardSQLTypeName.STRING),
                Field.of(VENDOR_NUMBER,StandardSQLTypeName.STRING),
                Field.of(TRAILER_NUMBER,StandardSQLTypeName.STRING),
                Field.of(RAIL_CAR_NUMBER,StandardSQLTypeName.STRING),
                Field.of(SCAC,StandardSQLTypeName.STRING),
                Field.of(LPN_STATUS,StandardSQLTypeName.STRING),
                Field.of(SHIPMENT_LPN_ERROR_TYPE,StandardSQLTypeName.STRING),
                Field.of(MHE_LOADED,StandardSQLTypeName.STRING)
        );
    }

    protected String getSelectAllQuery(String datasetID, String tableName){
        return String.format("SELECT * FROM %s.%s", datasetID, tableName);
    }
}