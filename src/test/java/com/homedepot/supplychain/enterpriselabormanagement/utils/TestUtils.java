package com.homedepot.supplychain.enterpriselabormanagement.utils;

import com.google.cloud.bigquery.*;
import com.jayway.jsonpath.JsonPath;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Assertions;

import java.util.*;

import static com.homedepot.supplychain.enterpriselabormanagement.constants.ElmTransactionBqHeaders.*;

@Slf4j
public final class TestUtils {

    public static final String TIMESTAMP_ZONE_APPENDER = "Z";

    public static final List<String> LABOR_EVENT_OPTIONAL_FIELDS = List.of(TASK_ID);
    public static final List<String> LPN_OPTIONAL_FIELDS = List.of(PARENT_LPN_ID, CONTAINER_TYPE);
    public static final List<String> LOCATION_OPTIONAL_FIELDS = List.of(PICK_AREA, PUT_AREA,
            REASON_CODE, ORDER_CATEGORY, SHIPMENT_NUMBER, SHIPMENT_TYPE_ID, SHIPMENT_ROUTE,
            SHIPMENT_STOP, STORE_NUMBER, SERVICE_TYPE, TRAILER_NUMBER, SCAC, LPN_STATUS,
            SHIPMENT_LPN_ERROR_TYPE, MHE_LOADED);
    public static final List<String> SKU_OPTIONAL_FIELDS = List.of(
            SPECIAL_HANDLING, BUILD_ON_METHOD,
            SECURE_METHOD, UNLOAD_TYPE, VENDOR_NUMBER);
    public static final String TRACE_ID_PATH = "$.laborEvent.trace_id";
    public static final String DC_NUMBER_PATH = "$.laborEvent.dc_number";

    private TestUtils() {
        //Utils class
    }

    public static Schema getElmSchema() {
        return Schema.of(
                Field.of(ELM_ID, StandardSQLTypeName.STRING),
                Field.of(BQ_CREATE_DTTM, StandardSQLTypeName.TIMESTAMP),
                Field.of(CONTRACT_VERSION, StandardSQLTypeName.STRING),
                Field.of(SOURCE, StandardSQLTypeName.STRING),
                Field.of(EVENT_TYPE, StandardSQLTypeName.STRING),
                Field.of(PLATFORM, StandardSQLTypeName.STRING),
                Field.of(DC_NUMBER, StandardSQLTypeName.STRING),
                Field.of(ACTIVITY, StandardSQLTypeName.STRING),
                Field.of(ACTION, StandardSQLTypeName.STRING),
                Field.of(TRACE_ID, StandardSQLTypeName.STRING),
                Field.of(TASK_ID, StandardSQLTypeName.STRING),
                Field.of(PARTITION_DATE, StandardSQLTypeName.DATE),
                Field.of(PUBLISH_TIMESTAMP, StandardSQLTypeName.TIMESTAMP),
                Field.of(USER_ID, StandardSQLTypeName.STRING),
                Field.of(LDAP_ID, StandardSQLTypeName.STRING),
                Field.of(TRANSACTION_ID, StandardSQLTypeName.STRING),
                Field.of(ASSIGNED_VEHICLE, StandardSQLTypeName.STRING),
                Field.of(VEHICLE_ID, StandardSQLTypeName.STRING),
                Field.of(PARENT_LPN_ID, StandardSQLTypeName.STRING),
                Field.of(LPN_NUMBER, StandardSQLTypeName.STRING),
                Field.of(CROSSDOCK, StandardSQLTypeName.BOOL),
                Field.of(RECEIVING_TYPE, StandardSQLTypeName.STRING),
                Field.of(CONTAINER_TYPE, StandardSQLTypeName.STRING),
                Field.of(TRANSACTION_TIMESTAMP, StandardSQLTypeName.TIMESTAMP),
                Field.of(START_LOCATION, StandardSQLTypeName.STRING),
                Field.of(END_LOCATION, StandardSQLTypeName.STRING),
                Field.of(START_ZONE, StandardSQLTypeName.STRING),
                Field.of(END_ZONE, StandardSQLTypeName.STRING),
                Field.of(START_LOCATION_TYPE, StandardSQLTypeName.STRING),
                Field.of(END_LOCATION_TYPE, StandardSQLTypeName.STRING),
                Field.of(PICK_AREA, StandardSQLTypeName.STRING),
                Field.of(PUT_AREA, StandardSQLTypeName.STRING),
                Field.of(SKU_NUMBER, StandardSQLTypeName.STRING),
                Field.of(IS_MX, StandardSQLTypeName.BOOL),
                Field.of(BUILD_ID, StandardSQLTypeName.STRING),
                Field.of(SKU_DESCRIPTION, StandardSQLTypeName.STRING),
                Field.of(DEPARTMENT, StandardSQLTypeName.STRING),
                Field.of(SKU_CLASS, StandardSQLTypeName.STRING),
                Field.of(SKU_SUB_CLASS, StandardSQLTypeName.STRING),
                Field.of(WEIGHT, StandardSQLTypeName.NUMERIC),
                Field.of(LENGTH, StandardSQLTypeName.NUMERIC),
                Field.of(WIDTH, StandardSQLTypeName.NUMERIC),
                Field.of(HEIGHT, StandardSQLTypeName.NUMERIC),
                Field.of(VOLUME, StandardSQLTypeName.NUMERIC),
                Field.of(WEIGHT_UOM, StandardSQLTypeName.STRING),
                Field.of(SIZE_UOM, StandardSQLTypeName.STRING),
                Field.of(PACKAGE_UNIT_QTY, StandardSQLTypeName.NUMERIC),
                Field.of(PACKAGE_EACH_QUANTITY, StandardSQLTypeName.NUMERIC),
                Field.of(PACKAGE_PALLET_QUANTITY, StandardSQLTypeName.NUMERIC),
                Field.of(PACKAGE_CASE_QUANTITY, StandardSQLTypeName.NUMERIC),
                Field.of(PACKAGE_PACK_QUANTITY, StandardSQLTypeName.NUMERIC),
                Field.of(SPECIAL_HANDLING, StandardSQLTypeName.STRING),
                Field.of(BUILD_ON_METHOD, StandardSQLTypeName.STRING),
                Field.of(SECURE_METHOD, StandardSQLTypeName.STRING),
                Field.of(UNLOAD_TYPE, StandardSQLTypeName.STRING),
                Field.of(LOCATION_UOM, StandardSQLTypeName.STRING),
                Field.of(QUANTITY, StandardSQLTypeName.NUMERIC),
                Field.of(UOM_QTY, StandardSQLTypeName.NUMERIC),
                Field.of(REASON_CODE, StandardSQLTypeName.STRING),
                Field.of(INBOUND_OUTBOUND_INDICATOR, StandardSQLTypeName.STRING),
                Field.of(ORDER_CATEGORY, StandardSQLTypeName.STRING),
                Field.of(SHIPMENT_NUMBER, StandardSQLTypeName.STRING),
                Field.of(SHIPMENT_TYPE_ID, StandardSQLTypeName.STRING),
                Field.of(SHIPMENT_ROUTE, StandardSQLTypeName.STRING),
                Field.of(SHIPMENT_STOP, StandardSQLTypeName.STRING),
                Field.of(STORE_NUMBER, StandardSQLTypeName.STRING),
                Field.of(SERVICE_TYPE, StandardSQLTypeName.STRING),
                Field.of(VENDOR_NUMBER, StandardSQLTypeName.STRING),
                Field.of(TRAILER_NUMBER, StandardSQLTypeName.STRING),
                Field.of(RAIL_CAR_NUMBER, StandardSQLTypeName.STRING),
                Field.of(SCAC, StandardSQLTypeName.STRING),
                Field.of(LPN_STATUS, StandardSQLTypeName.STRING),
                Field.of(SHIPMENT_LPN_ERROR_TYPE, StandardSQLTypeName.STRING),
                Field.of(MHE_LOADED, StandardSQLTypeName.STRING),
                Field.of(ASN_VENDOR_NUMBER, StandardSQLTypeName.STRING),
                Field.of(BUY_PACK_QUANTITY, StandardSQLTypeName.STRING)
                );
    }

    public static String getSelectAllQuery(String datasetID, String tableName) {
        return String.format("SELECT * FROM %s.%s", datasetID, tableName);
    }

    public static void assertTableResultsAgainstJson(TableResult tableResult, String jsonMessage) throws JSONException {
        Iterator<FieldValueList> fieldValueListIterator = tableResult.iterateAll().iterator();
        Map<String, String> row = new LinkedHashMap<>();
        JSONObject jsonObject = new JSONObject(jsonMessage);

        //Mapping for laborEvent section
        JSONObject laborEvent = jsonObject.getJSONObject("laborEvent");
        row.put(CONTRACT_VERSION, laborEvent.getString(CONTRACT_VERSION));
        row.put(SOURCE, laborEvent.getString(SOURCE));
        row.put(EVENT_TYPE, laborEvent.getString(EVENT_TYPE));
        row.put(PLATFORM, laborEvent.getString(PLATFORM));
        row.put(DC_NUMBER, laborEvent.getString(DC_NUMBER));
        row.put(ACTIVITY, laborEvent.getString(ACTIVITY));
        row.put(ACTION, laborEvent.getString(ACTION));
        row.put(TRACE_ID, laborEvent.getString(TRACE_ID));
        row.put(PUBLISH_TIMESTAMP, laborEvent.getString(PUBLISH_TIMESTAMP));
        LABOR_EVENT_OPTIONAL_FIELDS.forEach(optionalField -> {
            if (laborEvent.has(optionalField)) {
                try {
                    row.put(optionalField, laborEvent.getString(optionalField));
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
            } else {
                row.put(optionalField, "");
            }
        });

        //Mapping for laborEventDetail section
        JSONObject laborEventDetail = jsonObject.getJSONObject("laborEventDetail");
        row.put(USER_ID, laborEventDetail.getString(USER_ID));
        row.put(LDAP_ID, laborEventDetail.getString(LDAP_ID));
        row.put(TRANSACTION_ID, laborEventDetail.getString(TRANSACTION_ID));
        if (laborEventDetail.has("vehicle")) {
            JSONObject vehicle = laborEventDetail.getJSONObject("vehicle");
            row.put(VEHICLE_ID, vehicle.getString(VEHICLE_ID));
            row.put(ASSIGNED_VEHICLE, vehicle.getString(ASSIGNED_VEHICLE));
        }
        if (laborEventDetail.has(TRANSACTION_TIMESTAMP)) {
            row.put(TRANSACTION_TIMESTAMP, laborEventDetail.getString(TRANSACTION_TIMESTAMP));
        }

        //Mapping for Lpn section
        if (laborEventDetail.has("lpns")) {
            JSONArray lpnsJsonArray = laborEventDetail.getJSONArray("lpns");
            for (int i = 0; i < lpnsJsonArray.length(); i++) {
                JSONObject lpn = lpnsJsonArray.getJSONObject(i);

                row.put(LPN_NUMBER, lpn.getString(LPN_NUMBER));
                row.put(RECEIVING_TYPE, lpn.optString(RECEIVING_TYPE));
                LPN_OPTIONAL_FIELDS.forEach(optionalField -> {
                    if (lpn.has(optionalField)) {
                        try {
                            row.put(optionalField, lpn.getString(optionalField));
                        } catch (JSONException e) {
                            throw new RuntimeException(e);
                        }
                    } else {
                        row.put(optionalField, "");
                    }
                });

                //Mapping for Locations section
                JSONArray locationsJsonArray = lpn.getJSONArray("locations");
                for (int j = 0; j < locationsJsonArray.length(); j++) {
                    JSONObject location = locationsJsonArray.getJSONObject(j);
                    row.put(TRANSACTION_TIMESTAMP, location.getString(TRANSACTION_TIMESTAMP));
                    row.put(START_LOCATION, location.getString(START_LOCATION));
                    row.put(END_LOCATION, location.getString(END_LOCATION));
                    row.put(START_ZONE, location.getString(START_ZONE));
                    row.put(END_ZONE, location.getString(END_ZONE));
                    row.put(START_LOCATION_TYPE, location.getString(START_LOCATION_TYPE));
                    row.put(END_LOCATION_TYPE, location.getString(END_LOCATION_TYPE));
                    row.put(LOCATION_UOM, location.getString(LOCATION_UOM));
                    row.put(UOM_QTY, location.getString(UOM_QTY));
                    row.put(INBOUND_OUTBOUND_INDICATOR, location.getString(INBOUND_OUTBOUND_INDICATOR));
                    LOCATION_OPTIONAL_FIELDS.forEach(optionalField -> {
                        if (location.has(optionalField)) {
                            try {
                                row.put(optionalField, location.getString(optionalField));
                            } catch (JSONException e) {
                                throw new RuntimeException(e);
                            }
                        } else {
                            row.put(optionalField, "");
                        }
                    });


                    //Mappings for SKU
                    JSONArray skusJsonArray = location.getJSONArray("skus");
                    for (int k = 0; k < skusJsonArray.length(); k++) {
                        JSONObject sku = skusJsonArray.getJSONObject(k);
                        row.put(SKU_NUMBER, sku.getString(SKU_NUMBER));
                        row.put(BUILD_ID, sku.getString(BUILD_ID));
                        row.put(SKU_DESCRIPTION, sku.getString(SKU_DESCRIPTION));
                        row.put(DEPARTMENT, sku.getString(DEPARTMENT));
                        row.put(SKU_CLASS, sku.getString(SKU_CLASS));
                        row.put(SKU_SUB_CLASS, sku.getString(SKU_SUB_CLASS));
                        row.put(QUANTITY, sku.getString(QUANTITY));
                        row.put(BUY_PACK_QUANTITY, sku.optString(BUY_PACK_QUANTITY));
                        row.put(ASN_VENDOR_NUMBER, sku.optString(ASN_VENDOR_NUMBER));
                        SKU_OPTIONAL_FIELDS.forEach(optionalField -> {
                            if (sku.has(optionalField)) {
                                try {
                                    row.put(optionalField, sku.getString(optionalField));
                                } catch (JSONException e) {
                                    throw new RuntimeException(e);
                                }
                            } else {
                                row.put(optionalField, "");
                            }
                        });
                        compareRows(fieldValueListIterator.next(), row);
                    }
                }
            }
        } else {
            compareRows(fieldValueListIterator.next(), row);
        }
    }

    private static void compareRows(FieldValueList actualRow, Map<String, String> expectedRow) {
        log.info("Running comparison for row :{}", expectedRow.toString());
        Assertions.assertNotNull(actualRow.get(ELM_ID));
        Assertions.assertNotNull(actualRow.get(BQ_CREATE_DTTM));
        Assertions.assertNotNull(actualRow.get(PARTITION_DATE));
        Assertions.assertTrue(actualRow.get(TRANSACTION_TIMESTAMP)
                .getTimestampInstant().toString()
                .contains(actualRow.get(PARTITION_DATE).getStringValue()));
        expectedRow.forEach((k, v) -> {
            String actualValue;
            String expectedValue;
            if (!actualRow.get(k).isNull()) {
                if (Objects.equals(k, PUBLISH_TIMESTAMP) || Objects.equals(k, TRANSACTION_TIMESTAMP)) {
                    expectedValue = v + TIMESTAMP_ZONE_APPENDER;
                    actualValue = actualRow.get(k).getTimestampInstant().toString();
                } else {
                    expectedValue = v;
                    actualValue = actualRow.get(k).getStringValue();
                }
                log.info("Comparing column:{} [expected= {}, actual= {}]", k, expectedValue, actualValue);
                Assertions.assertEquals(expectedValue, actualValue);
            }
        });
    }

    public static void validateR2RJsonPayloadAgainstHdw(String r2rPayload, String hdwPayload) throws JSONException {
        JSONObject r2rJsonObject = new JSONObject(r2rPayload);
        JSONObject hdwJsonObject = new JSONObject(hdwPayload);
        String dc_number = hdwJsonObject.getJSONObject("laborEvent").getString(DC_NUMBER);
        String trace_id = hdwJsonObject.getJSONObject("laborEvent").getString(TRACE_ID);
        String transactionId = r2rJsonObject.getJSONObject("header").getString("transactionId");
        String locationId = r2rJsonObject.getJSONObject("header").getString("locationId");
        JSONArray entitiesJsonArray = r2rJsonObject.getJSONObject("body").getJSONArray("entities");
        for (int k = 0; k < entitiesJsonArray.length(); k++) {
            JSONObject entity = entitiesJsonArray.getJSONObject(k);
            String id = entity.getString("id");
            log.info("Comparing id={} from R2R message with trace_id={} from hdw message", id, trace_id);
            Assertions.assertEquals(id, trace_id);
            log.info("Comparing locationId={} from R2R message with dc_number={} from hdw message", locationId, dc_number);
            Assertions.assertEquals(locationId, dc_number);
            log.info("Asserting not null transactionId={} from R2R message", transactionId);
            Assertions.assertNotNull(transactionId);
        }
    }

    public static String getJsonFieldValue(String messageBody, String jsonPath) {
        return JsonPath.read(messageBody, jsonPath);
    }
}
