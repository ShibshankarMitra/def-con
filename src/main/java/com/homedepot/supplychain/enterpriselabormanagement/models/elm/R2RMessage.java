package com.homedepot.supplychain.enterpriselabormanagement.models.elm;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;
@Getter
@Setter
@NoArgsConstructor
public class R2RMessage {

    @JsonProperty("body")
    private Body body;

    @JsonProperty("header")
    private Map<String, String> header;
}
