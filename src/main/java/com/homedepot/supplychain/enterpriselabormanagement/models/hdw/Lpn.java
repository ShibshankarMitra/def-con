package com.homedepot.supplychain.enterpriselabormanagement.models.hdw;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
@Getter
@Setter
@NoArgsConstructor
public class Lpn {
    @JsonProperty("parent_lpn_id")
    private String parentLpnId;
    @JsonProperty("lpn_number")
    private String lpnNumber;
    @JsonProperty("crossdock")
    private String crossdock;
    @JsonProperty("receiving_type")
    private String receivingType;
    @JsonProperty("container_type")
    private String containerType;
    @JsonProperty("locations")
    private List<Location> locations;
}
