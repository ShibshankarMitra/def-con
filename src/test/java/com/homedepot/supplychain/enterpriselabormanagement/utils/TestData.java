package com.homedepot.supplychain.enterpriselabormanagement.utils;

import com.google.cloud.bigquery.BigQueryError;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

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

}