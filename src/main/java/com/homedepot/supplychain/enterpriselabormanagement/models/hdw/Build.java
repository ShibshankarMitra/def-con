package com.homedepot.supplychain.enterpriselabormanagement.models.hdw;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Build {

    @JsonProperty("build_id")
    private String buildId;
    @JsonProperty("sku")
    private String sku;
    @JsonProperty("is_mx")
    private Boolean isMx;
    @JsonProperty("package_hierarchy")
    private List<PackageHierarchy> packageHierarchies;

}
