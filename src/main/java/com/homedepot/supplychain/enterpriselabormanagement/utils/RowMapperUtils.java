package com.homedepot.supplychain.enterpriselabormanagement.utils;

import static com.homedepot.supplychain.enterpriselabormanagement.constants.ElmTransactionBqHeaders.*;

import com.homedepot.supplychain.enterpriselabormanagement.models.hdw.HdwTransaction;
import org.apache.commons.lang3.ObjectUtils;

import java.util.*;

public final class RowMapperUtils {
    private RowMapperUtils() {
        //Utils class
    }

    public static List<Map<String, Object>> getRows(HdwTransaction hdwTransaction) {
        List<Map<String, Object>> rows = new ArrayList<>();
        TransactionUtils.processTransaction(hdwTransaction);
        populateRowsFromTransaction(rows, hdwTransaction);
        return rows;
    }

    private static void populateRowsFromTransaction(List<Map<String, Object>> rows, HdwTransaction hdwTransaction) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put(CONTRACT_VERSION, hdwTransaction.getAttributes().get(CONTRACT_VERSION));
        row.put(SOURCE, hdwTransaction.getAttributes().get(SOURCE));
        row.put(EVENT_TYPE, hdwTransaction.getAttributes().get(EVENT_TYPE));
        row.put(PLATFORM, hdwTransaction.getAttributes().get(PLATFORM));
        row.put(DC_NUMBER, hdwTransaction.getAttributes().get(DC_NUMBER));
        row.put(ACTIVITY, hdwTransaction.getAttributes().get(ACTIVITY));
        row.put(ACTION, hdwTransaction.getAttributes().get(ACTION));
        row.put(TRACE_ID, hdwTransaction.getAttributes().get(TRACE_ID));
        row.put(TASK_ID, hdwTransaction.getAttributes().get(TASK_ID));
        row.put(PUBLISH_TIMESTAMP, hdwTransaction.getAttributes().get(PUBLISH_TIMESTAMP));
        row.put(USER_ID, hdwTransaction.getData().getUserId());
        row.put(LDAP_ID, hdwTransaction.getData().getLdapId());
        row.put(TRANSACTION_ID, hdwTransaction.getData().getTransactionId());
        if (!ObjectUtils.isEmpty(hdwTransaction.getData().getTransactionTimestamp())) {
            row.put(TRANSACTION_TIMESTAMP, hdwTransaction.getData().getTransactionTimestamp());
            row.put(PARTITION_DATE, CommonUtils.getDateFromTimeStampString(hdwTransaction.getData().getTransactionTimestamp()));
        }
        if (!ObjectUtils.isEmpty(hdwTransaction.getData().getVehicle())) {
            row.put(VEHICLE_ID, hdwTransaction.getData().getVehicle().getVehicleId());
            row.put(ASSIGNED_VEHICLE, hdwTransaction.getData().getVehicle().getAssignedVehicle());
        }
        if (!ObjectUtils.isEmpty(hdwTransaction.getData().getLpns())) {
            hdwTransaction.getData().getLpns().forEach(lpn -> {
                row.put(PARENT_LPN_ID, lpn.getParentLpnId());
                row.put(LPN_NUMBER, lpn.getLpnNumber());
                row.put(CONTAINER_TYPE, lpn.getContainerType());
                lpn.getLocations().forEach(location -> {
                    row.put(TRANSACTION_TIMESTAMP, location.getTransactionTimeStamp());
                    row.put(PARTITION_DATE, CommonUtils.getDateFromTimeStampString(location.getTransactionTimeStamp()));
                    row.put(START_LOCATION, location.getStartLocation());
                    row.put(END_LOCATION, location.getEndLocation());
                    row.put(START_ZONE, location.getStartZone());
                    row.put(END_ZONE, location.getEndZone());
                    row.put(START_LOCATION_TYPE, location.getStartLocationType());
                    row.put(END_LOCATION_TYPE, location.getEndLocationType());
                    row.put(PICK_AREA, location.getPickArea());
                    row.put(PUT_AREA, location.getPutArea());
                    row.put(LOCATION_UOM, location.getLocationUom());
                    row.put(LOCATION_QTY, location.getLocationQty());
                    row.put(UOM_QTY, location.getUomQty());
                    row.put(REASON_CODE, location.getReasonCode());
                    row.put(INBOUND_OUTBOUND_INDICATOR, location.getInboundOutboundIndicator());
                    row.put(ORDER_CATEGORY, location.getOrderCategory());
                    row.put(SHIPMENT_NUMBER, location.getShipmentNumber());
                    row.put(SHIPMENT_TYPE_ID, location.getShipmentTypeId());
                    row.put(SHIPMENT_ROUTE, location.getShipmentRoute());
                    row.put(SHIPMENT_STOP, location.getShipmentStop());
                    row.put(STORE_NUMBER, location.getStoreNumber());
                    row.put(SERVICE_TYPE, location.getServiceType());
                    row.put(VENDOR_NUMBER, location.getVendorNumber());
                    row.put(TRAILER_NUMBER, location.getTrailerNumber());
                    row.put(RAIL_CAR_NUMBER, location.getRailCarNumber());
                    row.put(SCAC, location.getScac());
                    row.put(LPN_STATUS, location.getLpnStatus());
                    row.put(SHIPMENT_LPN_ERROR_TYPE, location.getShipmentLpnErrorType());
                    row.put(MHE_LOADED, location.getMheLoaded());
                    location.getSkus().forEach(sku -> {
                        row.put(SKU_NUMBER, sku.getSkuNumber());
                        row.put(BUILD_ID, sku.getBuildId());
                        row.put(SKU_DESCRIPTION, sku.getSkuDescription());
                        row.put(DEPARTMENT, sku.getDepartment());
                        row.put(CLASS, sku.getSkuClass());
                        row.put(SUB_CLASS, sku.getSkuSubClass());
                        row.put(WEIGHT, sku.getWeight());
                        row.put(LENGTH, sku.getLength());
                        row.put(WIDTH, sku.getWidth());
                        row.put(HEIGHT, sku.getHeight());
                        row.put(VOLUME, sku.getVolume());
                        row.put(WEIGHT_UOM, sku.getWeightUom());
                        row.put(SIZE_UOM, sku.getSizeUom());
                        row.put(PACKAGE_EACH_QTY, sku.getPackageEachQty());
                        row.put(PACKAGE_UNIT_QTY, sku.getPackageUnitQty());
                        row.put(SPECIAL_HANDLING, sku.getSpecialHandling());
                        row.put(BUILD_ON_METHOD, sku.getBuildOnMethod());
                        row.put(SECURE_METHOD, sku.getSecureMethod());
                        row.put(UNLOAD_TYPE, sku.getUnloadType());
                        row.put(ELM_ID, CommonUtils.getRandomUuid());
                        row.put(BQ_CREATE_DTTM, CommonUtils.getCurrentTimeStampToBqFormat());
                        HashMap<String, Object> rowToBeInserted = new LinkedHashMap<>(row);
                        rows.add(rowToBeInserted);
                    });
                });
            });
        } else {
            row.put(ELM_ID, CommonUtils.getRandomUuid());
            row.put(BQ_CREATE_DTTM, CommonUtils.getCurrentTimeStampToBqFormat());
            HashMap<String, Object> rowToBeInserted = new LinkedHashMap<>(row);
            rows.add(rowToBeInserted);
        }
    }
}
