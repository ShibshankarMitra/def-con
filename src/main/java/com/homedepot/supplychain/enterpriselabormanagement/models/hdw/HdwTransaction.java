package com.homedepot.supplychain.enterpriselabormanagement.models.hdw;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
public class HdwTransaction {
    @JsonProperty("attributes")
    private Map<String, Object> attributes;
    @JsonProperty("data")
    private Data data;
}
