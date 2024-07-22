package com.homedepot.supplychain.enterpriselabormanagement.models.hdw;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
public class PackageHierarchy {
    @JsonProperty("depth")
    private BigDecimal depth;
    @JsonProperty("width")
    private BigDecimal width;
    @JsonProperty("weight")
    private BigDecimal weight;
    @JsonProperty("height")
    private BigDecimal height;
    @JsonProperty("weight_uom")
    private String weightUom;
    @JsonProperty("size_uom")
    private String sizeUom;
    @JsonProperty("package_unit_qty")
    private BigDecimal packageUnitQty;
    @JsonProperty("package_each_qty")
    private BigDecimal packageEachQty;
    @Getter(AccessLevel.NONE)
    @JsonProperty("i_package_unit_qty")
    private BigDecimal intPackageUnitQty;
    @Getter(AccessLevel.NONE)
    @JsonProperty("i_package_each_qty")
    private BigDecimal intPackageEachQty;
    @JsonProperty("hierarchy_level_code")
    private String hierarchyLevelCode;
}
