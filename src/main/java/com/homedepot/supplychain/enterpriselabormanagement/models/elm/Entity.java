package com.homedepot.supplychain.enterpriselabormanagement.models.elm;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class Entity {
    @JsonProperty("id")
    private String id;
}
