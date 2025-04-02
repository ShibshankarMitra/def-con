package com.homedepot.supplychain.enterpriselabormanagement.constants;

public final class ElmTransactionBqHeaders {

    //ELM_HDW_TRANSACTIONS BQ Table Headers

    public static final String ELM_ID="elm_id";
    public static final String BQ_CREATE_DTTM="bq_create_dttm";
    public static final String CONTRACT_VERSION="contract_version";
    public static final String SOURCE="source";
    public static final String EVENT_TYPE ="event_type";
    public static final String PLATFORM="platform";
    public static final String DC_NUMBER="dc_number";
    public static final String ACTIVITY ="activity";
    public static final String ACTION ="action";
    public static final String TRACE_ID="trace_id";
    public static final String TASK_ID="task_id";
    public static final String PARTITION_DATE="partition_date";
    public static final String PUBLISH_TIMESTAMP="publish_timestamp";
    public static final String USER_ID="user_id";
    public static final String LDAP_ID="ldap_id";
    public static final String TRANSACTION_ID="transaction_id";
    public static final String ASSIGNED_VEHICLE="assigned_vehicle";
    public static final String VEHICLE_ID="vehicle_id";
    public static final String PARENT_LPN_ID="parent_lpn_id";
    public static final String LPN_NUMBER="lpn_number";
    public static final String CROSSDOCK="crossdock";
    public static final String CONTAINER_TYPE="container_type";
    public static final String TRANSACTION_TIMESTAMP ="transaction_timestamp";
    public static final String START_LOCATION="start_location";
    public static final String END_LOCATION="end_location";
    public static final String START_ZONE="start_zone";
    public static final String END_ZONE="end_zone";
    public static final String START_LOCATION_TYPE="start_location_type";
    public static final String END_LOCATION_TYPE="end_location_type";
    public static final String PICK_AREA="pick_area";
    public static final String PUT_AREA="put_area";
    public static final String SKU_NUMBER="sku_number";
    public static final String IS_MX ="is_mx";
    public static final String BUILD_ID="build_id";
    public static final String SKU_DESCRIPTION="sku_description";
    public static final String DEPARTMENT="department";
    public static final String SKU_CLASS ="sku_class";
    public static final String SKU_SUB_CLASS ="sku_sub_class";
    public static final String WEIGHT="weight";
    public static final String LENGTH="length";
    public static final String WIDTH="width";
    public static final String HEIGHT="height";
    public static final String VOLUME="volume";
    public static final String WEIGHT_UOM="weight_uom";
    public static final String SIZE_UOM="size_uom";
    public static final String PACKAGE_UNIT_QTY="package_unit_qty";
    public static final String PACKAGE_EACH_QUANTITY ="package_each_quantity";
    public static final String PACKAGE_PALLET_QUANTITY ="package_pallet_quantity";
    public static final String PACKAGE_CASE_QUANTITY ="package_case_quantity";
    public static final String PACKAGE_PACK_QUANTITY ="package_pack_quantity";
    public static final String SPECIAL_HANDLING="special_handling";
    public static final String BUILD_ON_METHOD="build_on_method";
    public static final String SECURE_METHOD="secure_method";
    public static final String UNLOAD_TYPE="unload_type";
    public static final String LOCATION_UOM="location_uom";
    public static final String QUANTITY ="quantity";
    public static final String UOM_QTY="uom_qty";
    public static final String REASON_CODE="reason_code";
    public static final String INBOUND_OUTBOUND_INDICATOR="inbound_outbound_indicator";
    public static final String ORDER_CATEGORY="order_category";
    public static final String SHIPMENT_NUMBER="shipment_number";
    public static final String SHIPMENT_TYPE_ID="shipment_type_id";
    public static final String SHIPMENT_ROUTE="shipment_route";
    public static final String SHIPMENT_STOP="shipment_stop";
    public static final String STORE_NUMBER="store_number";
    public static final String SERVICE_TYPE="service_type";
    public static final String VENDOR_NUMBER="vendor_number";
    public static final String ASN_VENDOR_NUMBER="asn_vendor_number";
    public static final String TRAILER_NUMBER="trailer_number";
    public static final String RAIL_CAR_NUMBER="rail_car_number";
    public static final String SCAC="scac";
    public static final String LPN_STATUS="lpn_status";
    public static final String SHIPMENT_LPN_ERROR_TYPE="shipment_lpn_error_type";
    public static final String MHE_LOADED="mhe_loaded";
    public static final String SKU_BUILD_UOM="sku_build_uom";
    public static final String TRANSACTION_SEQ_NBR="transaction_seq_nbr";
    public static final String RECEIVING_TYPE="receiving_type";
    public static final String BUY_PACK_QUANTITY="buy_pack_quantity";

    // CICO transaction BQ Table Headers
    public static final String USER_NAME= "user_name";
    public static final String PUNCH_DATE= "punch_date";
    public static final String ADJUSTED_PUNCH_DATE= "adjusted_punch_date";
    public static final String PUNCH_UTC_TIME= "punch_utc_time";
    public static final String PUNCH_LOCAL_TIME= "punch_local_time";
    public static final String PUNCH_TYPE= "punch_type";
    public static final String TOTAL_HOURS_WORKED= "total_hours_worked";

    private ElmTransactionBqHeaders() {
        //Constant class
    }
}