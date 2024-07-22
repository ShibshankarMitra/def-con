package com.homedepot.supplychain.enterpriselabormanagement.models.hdw;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
@Getter
@Setter
@NoArgsConstructor
public class Location {
    @JsonProperty("transaction_timestamp")
    private String transactionTimeStamp;
    @JsonProperty("start_location")
    private String startLocation;
    @JsonProperty("end_location")
    private String endLocation;
    @JsonProperty("start_zone")
    private String startZone;
    @JsonProperty("end_zone")
    private String endZone;
    @JsonProperty("start_location_type")
    private String startLocationType;
    @JsonProperty("end_location_type")
    private String endLocationType;
    @JsonProperty("pick_area")
    private String pickArea;
    @JsonProperty("put_area")
    private String putArea;
    @JsonProperty("skus")
    private List<Sku> skus;
    @JsonProperty("location_uom")
    private String locationUom;
    @JsonProperty("location_qty")
    private double locationQty;
    @JsonProperty("uom_qty")
    private double uomQty;
    @JsonProperty("reason_code")
    private String reasonCode;
    @JsonProperty("inbound_outbound_indicator")
    private String inboundOutboundIndicator;
    @JsonProperty("order_category")
    private String orderCategory;
    @JsonProperty("shipment_number")
    private String shipmentNumber;
    @JsonProperty("shipment_type_id")
    private String shipmentTypeId;
    @JsonProperty("shipment_route")
    private String shipmentRoute;
    @JsonProperty("shipment_stop")
    private String shipmentStop;
    @JsonProperty("store_number")
    private String storeNumber;
    @JsonProperty("service_type")
    private String serviceType;
    @JsonProperty("vendor_number")
    private String vendorNumber;
    @JsonProperty("trailer_number")
    private String trailerNumber;
    @JsonProperty("rail_car_number")
    private String railCarNumber;
    @JsonProperty("scac")
    private String scac;
    @JsonProperty("lpn_status")
    private String lpnStatus;
    @JsonProperty("shipment_lpn_error_type")
    private String shipmentLpnErrorType;
    @JsonProperty("mhe_loaded")
    private String mheLoaded;
}
