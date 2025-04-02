package com.homedepot.supplychain.enterpriselabormanagement.utils;

import static com.homedepot.supplychain.enterpriselabormanagement.constants.ElmTransactionBqHeaders.*;

import com.homedepot.supplychain.enterpriselabormanagement.models.hdw.HdwTransaction;
import org.apache.commons.lang3.ObjectUtils;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

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
        row.put(CONTRACT_VERSION, hdwTransaction.getLaborEvent().getContractVersion());
        row.put(SOURCE, hdwTransaction.getLaborEvent().getSource());
        row.put(EVENT_TYPE, hdwTransaction.getLaborEvent().getEventType());
        row.put(PLATFORM, hdwTransaction.getLaborEvent().getPlatform());
        row.put(DC_NUMBER, hdwTransaction.getLaborEvent().getDcNumber());
        row.put(ACTIVITY, hdwTransaction.getLaborEvent().getActivity());
        row.put(ACTION, hdwTransaction.getLaborEvent().getAction());
        row.put(TRACE_ID, hdwTransaction.getLaborEvent().getTraceId());
        row.put(TASK_ID, ObjectUtils.isEmpty(hdwTransaction.getLaborEvent().getTaskId()) ? null : hdwTransaction.getLaborEvent().getTaskId());
        row.put(PUBLISH_TIMESTAMP, hdwTransaction.getLaborEvent().getPublishTimestamp());
        row.put(USER_ID, hdwTransaction.getLaborEventDetail().getUserId());
        row.put(LDAP_ID, hdwTransaction.getLaborEventDetail().getLdapId());
        row.put(TRANSACTION_ID, hdwTransaction.getLaborEventDetail().getTransactionId());
        if (!ObjectUtils.isEmpty(hdwTransaction.getLaborEventDetail().getTransactionTimestamp())) {
            row.put(TRANSACTION_TIMESTAMP, hdwTransaction.getLaborEventDetail().getTransactionTimestamp());
            row.put(PARTITION_DATE, CommonUtils.getDateFromTimeStampString(hdwTransaction.getLaborEventDetail().getTransactionTimestamp()));
        }
        if (!ObjectUtils.isEmpty(hdwTransaction.getLaborEventDetail().getVehicle())) {
            row.put(VEHICLE_ID, hdwTransaction.getLaborEventDetail().getVehicle().getVehicleId());
            row.put(ASSIGNED_VEHICLE, hdwTransaction.getLaborEventDetail().getVehicle().getAssignedVehicle());
        }
        if (!ObjectUtils.isEmpty(hdwTransaction.getLaborEventDetail().getLpns())) {
            AtomicInteger transactionSeqNbr = new AtomicInteger();
            hdwTransaction.getLaborEventDetail().getLpns().forEach(lpn -> {
                row.put(PARENT_LPN_ID, CommonUtils.getValueOrNull(lpn.getParentLpnId()));
                row.put(LPN_NUMBER, lpn.getLpnNumber());
                row.put(CROSSDOCK, lpn.getCrossdock());
                row.put(RECEIVING_TYPE, lpn.getReceivingType());
                row.put(CONTAINER_TYPE, CommonUtils.getValueOrNull(lpn.getContainerType()));
                lpn.getLocations().forEach(location -> {
                    row.put(TRANSACTION_TIMESTAMP, location.getTransactionTimeStamp());
                    row.put(PARTITION_DATE, CommonUtils.getDateFromTimeStampString(location.getTransactionTimeStamp()));
                    row.put(START_LOCATION, location.getStartLocation());
                    row.put(END_LOCATION, location.getEndLocation());
                    row.put(START_ZONE, location.getStartZone());
                    row.put(END_ZONE, location.getEndZone());
                    row.put(START_LOCATION_TYPE, location.getStartLocationType());
                    row.put(END_LOCATION_TYPE, location.getEndLocationType());
                    row.put(PICK_AREA, CommonUtils.getValueOrNull(location.getPickArea()));
                    row.put(PUT_AREA, CommonUtils.getValueOrNull(location.getPutArea()));
                    row.put(LOCATION_UOM, location.getLocationUom());
                    row.put(UOM_QTY, location.getUomQty());
                    row.put(REASON_CODE, CommonUtils.getValueOrNull(location.getReasonCode()));
                    row.put(INBOUND_OUTBOUND_INDICATOR, location.getInboundOutboundIndicator());
                    row.put(ORDER_CATEGORY, CommonUtils.getValueOrNull(location.getOrderCategory()));
                    row.put(SHIPMENT_NUMBER, CommonUtils.getValueOrNull(location.getShipmentNumber()));
                    row.put(SHIPMENT_TYPE_ID, CommonUtils.getValueOrNull(location.getShipmentTypeId()));
                    row.put(SHIPMENT_ROUTE, CommonUtils.getValueOrNull(location.getShipmentRoute()));
                    row.put(SHIPMENT_STOP, CommonUtils.getValueOrNull(location.getShipmentStop()));
                    row.put(STORE_NUMBER, CommonUtils.getValueOrNull(location.getStoreNumber()));
                    row.put(SERVICE_TYPE, CommonUtils.getValueOrNull(location.getServiceType()));
                    row.put(TRAILER_NUMBER, CommonUtils.getValueOrNull(location.getTrailerNumber()));
                    row.put(RAIL_CAR_NUMBER, CommonUtils.getValueOrNull(location.getRailCarNumber()));
                    row.put(SCAC, CommonUtils.getValueOrNull(location.getScac()));
                    row.put(LPN_STATUS, CommonUtils.getValueOrNull(location.getLpnStatus()));
                    row.put(SHIPMENT_LPN_ERROR_TYPE, CommonUtils.getValueOrNull(location.getShipmentLpnErrorType()));
                    row.put(MHE_LOADED, CommonUtils.getValueOrNull(location.getMheLoaded()));
                    location.getSkus().forEach(sku -> {
                        row.put(TRANSACTION_SEQ_NBR, transactionSeqNbr.incrementAndGet());
                        row.put(SKU_NUMBER, sku.getSkuNumber());
                        row.put(IS_MX, sku.getIsMx());
                        row.put(BUILD_ID, sku.getBuildId());
                        row.put(DEPARTMENT, sku.getDepartment());
                        row.put(SKU_CLASS, sku.getSkuClass());
                        row.put(SKU_SUB_CLASS, sku.getSkuSubClass());
                        row.put(QUANTITY, sku.getQuantity());
                        row.put(WEIGHT, sku.getWeight());
                        row.put(LENGTH, sku.getLength());
                        row.put(WIDTH, sku.getWidth());
                        row.put(HEIGHT, sku.getHeight());
                        row.put(VOLUME, sku.getVolume());
                        row.put(WEIGHT_UOM, sku.getWeightUom());
                        row.put(SIZE_UOM, sku.getSizeUom());
                        row.put(PACKAGE_EACH_QUANTITY, sku.getPackageEachQuantity());
                        row.put(PACKAGE_PALLET_QUANTITY, sku.getPackagePalletQuantity());
                        row.put(PACKAGE_CASE_QUANTITY, sku.getPackageCaseQuantity());
                        row.put(PACKAGE_PACK_QUANTITY, sku.getPackagePackQuantity());
                        row.put(PACKAGE_UNIT_QTY, sku.getPackageUnitQty());
                        row.put(SKU_DESCRIPTION, CommonUtils.getValueOrNull(sku.getSkuDescription()));
                        row.put(SPECIAL_HANDLING, CommonUtils.getValueOrNull(sku.getSpecialHandling()));
                        row.put(BUILD_ON_METHOD, CommonUtils.getValueOrNull(sku.getBuildOnMethod()));
                        row.put(SECURE_METHOD, CommonUtils.getValueOrNull(sku.getSecureMethod()));
                        row.put(UNLOAD_TYPE, CommonUtils.getValueOrNull(sku.getUnloadType()));
                        row.put(VENDOR_NUMBER, CommonUtils.getValueOrNull(sku.getVendorNumber()));
                        row.put(ASN_VENDOR_NUMBER, CommonUtils.getValueOrNull(sku.getAsnVendorNumber()));
                        row.put(BUY_PACK_QUANTITY, CommonUtils.getValueOrNull(sku.getBuyPackQuantity()));
                        row.put(SKU_BUILD_UOM, sku.getSkuBuildUom());
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

    public static List<Map<String, Object>> populatePunchesRowsFromCicoTransactions(HdwTransaction hdwTransaction) {
        Map<String, Object> row = new LinkedHashMap<>();
        List<Map<String, Object>> rows = new ArrayList<>();
        row.put(USER_ID, hdwTransaction.getLaborEventDetail().getUserId());
        row.put(USER_NAME, hdwTransaction.getLaborEventDetail().getUserName());
        row.put(PUNCH_DATE, hdwTransaction.getLaborEventDetail().getPunchDate());
        row.put(ADJUSTED_PUNCH_DATE, ObjectUtils.isEmpty(hdwTransaction.getLaborEventDetail().getAdjustPunchDate()) ?
                row.get(PUNCH_DATE) : hdwTransaction.getLaborEventDetail().getAdjustPunchDate());
        row.put(PUNCH_LOCAL_TIME, hdwTransaction.getLaborEventDetail().getTransactionLocalTimestamp());
        row.put(PUNCH_UTC_TIME, hdwTransaction.getLaborEventDetail().getTransactionTimestamp());
        row.put(PUNCH_TYPE, CommonUtils.getPunchTypeHdwFormat(hdwTransaction.getLaborEvent().getActivity()));
        row.put(DC_NUMBER, hdwTransaction.getLaborEvent().getDcNumber());
        row.put(TRACE_ID, hdwTransaction.getLaborEvent().getTraceId());
        row.put(TRANSACTION_ID, hdwTransaction.getLaborEventDetail().getTransactionId());
        row.put(ELM_ID, CommonUtils.getRandomUuid());
        row.put(BQ_CREATE_DTTM, CommonUtils.getCurrentTimeStampToBqFormat());
        rows.add(row);
        return rows;
    }
}
