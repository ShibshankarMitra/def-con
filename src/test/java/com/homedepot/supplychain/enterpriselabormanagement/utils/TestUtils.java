package com.homedepot.supplychain.enterpriselabormanagement.utils;

import com.google.cloud.bigquery.FieldValueList;
import com.google.cloud.bigquery.TableResult;
import io.micrometer.common.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Assertions;

import java.util.*;

import static com.homedepot.supplychain.enterpriselabormanagement.constants.ElmTransactionBqHeaders.*;
import static com.homedepot.supplychain.enterpriselabormanagement.utils.TestConstants.*;

@Slf4j
public final class TestUtils {

    private TestUtils() {
        //Utils class
    }


    public static String getValueFromJson(String jsonMessage, String path) throws JSONException {
        String value = null;
        JSONObject jsonObject = new JSONObject(jsonMessage);
        String[] paths = path.split("\\.");
        for (int i = 0; i < paths.length - 1; i++) {
            if (paths[i].contains("[") && paths[i].contains("]")) {
                int startIndex = paths[i].lastIndexOf("[");
                int index = Integer.parseInt(paths[i].substring(startIndex + 1, startIndex + 2));
                String jsonArrayName = paths[i].substring(0, startIndex);
                JSONArray jsonArray = null;
                jsonArray = jsonObject.getJSONArray(jsonArrayName);
                jsonObject = jsonArray.getJSONObject(index);
            } else {
                jsonObject = jsonObject.getJSONObject(paths[i]);
            }
        }
        if (!StringUtils.isBlank(jsonObject.getString(paths[paths.length - 1]))) {
            value = jsonObject.getString(paths[paths.length - 1]);
        }
        return value;
    }

    public static void assertTableResultsAgainstJson(TableResult tableResult, String jsonMessage) throws JSONException {
        boolean results = false;
        Iterator<FieldValueList> fieldValueListIterator = tableResult.iterateAll().iterator();
        Map<String, String> row = new LinkedHashMap<>();
        JSONObject jsonObject = new JSONObject(jsonMessage);

        //Mapping for attributes section
        JSONObject attributes = jsonObject.getJSONObject("attributes");
        row.put(CONTRACT_VERSION, attributes.getString(CONTRACT_VERSION));
        row.put(SOURCE, attributes.getString(SOURCE));
        row.put(EVENT_TYPE, attributes.getString(EVENT_TYPE));
        row.put(PLATFORM, attributes.getString(PLATFORM));
        row.put(DC_NUMBER, attributes.getString(DC_NUMBER));
        row.put(ACTIVITY, attributes.getString(ACTIVITY));
        row.put(ACTION, attributes.getString(ACTION));
        row.put(TRACE_ID, attributes.getString(TRACE_ID));
        row.put(PUBLISH_TIMESTAMP, attributes.getString(PUBLISH_TIMESTAMP));
        ATTRIBUTE_OPTIONAL_FIELDS.forEach(optionalField -> {
            if (attributes.has(optionalField)) {
                try {
                    row.put(optionalField, attributes.getString(optionalField));
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
            } else {
                row.put(optionalField, "");
            }
        });

        //Mapping for data section
        JSONObject data = jsonObject.getJSONObject("data");
        row.put(USER_ID, data.getString(USER_ID));
        row.put(LDAP_ID, data.getString(LDAP_ID));
        row.put(TRANSACTION_ID, data.getString(TRANSACTION_ID));
        if (data.has("vehicle")) {
            JSONObject vehicle = data.getJSONObject("vehicle");
            row.put(VEHICLE_ID, vehicle.getString(VEHICLE_ID));
            row.put(ASSIGNED_VEHICLE, vehicle.getString(ASSIGNED_VEHICLE));
        }
        if (data.has(TRANSACTION_TIMESTAMP)) {
            row.put(TRANSACTION_TIMESTAMP, data.getString(TRANSACTION_TIMESTAMP));
        }

        //Mapping for Lpn section
        if (data.has("lpns")) {
            JSONArray lpnsJsonArray = data.getJSONArray("lpns");
            for (int i = 0; i < lpnsJsonArray.length(); i++) {
                JSONObject lpn = lpnsJsonArray.getJSONObject(i);

                row.put(LPN_NUMBER, lpn.getString(LPN_NUMBER));
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
                    row.put(LOCATION_QTY, location.getString(LOCATION_QTY));
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
        }else {
            compareRows(fieldValueListIterator.next(), row);
        }
    }

    public static void compareRows(FieldValueList actualRow, Map<String, String> expectedRow) {
        log.info("Running comparison for row :{}", expectedRow.toString());
        Assertions.assertNotNull(actualRow.get(ELM_ID));
        Assertions.assertNotNull(actualRow.get(BQ_CREATE_DTTM));
        Assertions.assertNotNull(actualRow.get(PARTITION_DATE));
        Assertions.assertTrue(actualRow.get(TRANSACTION_TIMESTAMP)
                .getTimestampInstant().toString()
                .contains(actualRow.get(PARTITION_DATE).getStringValue()));
        expectedRow.forEach((k, v) -> {
            if (StringUtils.isEmpty(v)) {
                if(!actualRow.get(k).isNull()) {
                    Assertions.assertEquals(actualRow.get(k).getStringValue(),v);
                }
            } else {
                log.info("Comparing column:{} actual {} expected {} ", k, actualRow.get(k).getStringValue(), v);
                if (Objects.equals(k, PUBLISH_TIMESTAMP)
                        || Objects.equals(k, TRANSACTION_TIMESTAMP)) {
                    Assertions.assertTrue(actualRow.get(k).getTimestampInstant().toString().contains(v));
                } else {
                    Assertions.assertEquals(actualRow.get(k).getStringValue(), v);
                }
            }
        });
    }
}
