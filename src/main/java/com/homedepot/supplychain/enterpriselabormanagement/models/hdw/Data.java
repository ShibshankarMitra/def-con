package com.homedepot.supplychain.enterpriselabormanagement.models.hdw;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
public class Data {
    @JsonProperty("user_id")
    private String userId;
    @JsonProperty("user_name")
    private String userName;
    @JsonProperty("ldap_id")
    private String ldapId;
    @JsonProperty("transaction_id")
    private String transactionId;
    @JsonProperty("transaction_timestamp")
    private String transactionTimestamp;
    @JsonProperty("vehicle")
    private Vehicle vehicle;
    @JsonProperty("lpns")
    private List<Lpn> lpns;
    @JsonProperty("builds")
    private List<Build> builds;
    @JsonProperty("transaction_local_timestamp")
    private String transactionLocalTimestamp;
    @JsonIgnore
    private String punchDate;
    @JsonIgnore
    private String adjustPunchDate;
}
