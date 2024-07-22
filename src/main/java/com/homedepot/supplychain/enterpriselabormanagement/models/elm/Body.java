package com.homedepot.supplychain.enterpriselabormanagement.models.elm;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class Body {

    @JsonProperty("entities")
    List<Entity> entities;
}