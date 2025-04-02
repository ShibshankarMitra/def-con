package com.homedepot.supplychain.enterpriselabormanagement.models.hdw;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
public class Sku {
    @JsonProperty("sku_number")
    private String skuNumber;
    @JsonProperty("build_id")
    private String buildId;
    @JsonProperty("sku_description")
    private String skuDescription;
    @JsonProperty("department")
    private String department;
    @JsonProperty("sku_class")
    private String skuClass;
    @JsonProperty("sku_sub_class")
    private String skuSubClass;
    @JsonIgnore
    private BigDecimal weight;
    @JsonIgnore
    private BigDecimal length;
    @JsonIgnore
    private BigDecimal width;
    @JsonIgnore
    private BigDecimal height;
    @JsonIgnore
    private BigDecimal volume;
    @JsonIgnore
    private String weightUom;
    @JsonIgnore
    private String sizeUom;
    @JsonIgnore
    private BigDecimal packageEachQuantity;
    @JsonIgnore
    private BigDecimal packagePalletQuantity;
    @JsonIgnore
    private BigDecimal packageCaseQuantity;
    @JsonIgnore
    private BigDecimal packagePackQuantity;
    @JsonIgnore
    private BigDecimal packageUnitQty;
    @JsonProperty("special_handling")
    private String specialHandling;
    @JsonProperty("build_on_method")
    private String buildOnMethod;
    @JsonProperty("secure_method")
    private String secureMethod;
    @JsonProperty("unload_type")
    private String unloadType;
    @JsonProperty("quantity")
    private BigDecimal quantity;
    @JsonProperty("vendor_number")
    private String vendorNumber;
    @JsonIgnore
    private String skuBuildUom;
    @JsonIgnore
    private Boolean isMx;
    @JsonProperty("asn_vendor_number")
    private String asnVendorNumber;
    @JsonProperty("buy_pack_quantity")
    private String buyPackQuantity;
}
