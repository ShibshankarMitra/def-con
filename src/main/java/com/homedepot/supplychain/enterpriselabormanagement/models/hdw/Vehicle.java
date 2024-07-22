package com.homedepot.supplychain.enterpriselabormanagement.models.hdw;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class Vehicle {
    @JsonProperty("assigned_vehicle")
    private String assignedVehicle;
    @JsonProperty("vehicle_id")
    private String vehicleId;


}
