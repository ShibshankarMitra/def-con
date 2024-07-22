package com.homedepot.supplychain.enterpriselabormanagement.models.hdw;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
@Getter
@Setter
@NoArgsConstructor
public class Build {

    @JsonProperty("build_id")
    private String buildId;
    @JsonProperty("sku")
    private int sku;
    @JsonProperty("package_hierarchy")
    private List<PackageHierarchy> packageHierarchies;

}
