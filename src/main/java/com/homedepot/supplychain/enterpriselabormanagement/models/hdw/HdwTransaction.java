package com.homedepot.supplychain.enterpriselabormanagement.models.hdw;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class HdwTransaction {
    @JsonProperty("laborEvent")
    private LaborEvent laborEvent;
    @JsonProperty("laborEventDetail")
    private Data laborEventDetail;
}
