package com.homedepot.supplychain.enterpriselabormanagement.models.hdw;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class LaborEvent {
    @JsonProperty("contract_version")
    private String contractVersion;
    private String source;
    @JsonProperty("event_type")
    private String eventType;
    private String action;
    private String activity;
    private String platform;
    @JsonProperty("dc_number")
    private String dcNumber;
    @JsonProperty("publish_timestamp")
    private String publishTimestamp;
    @JsonProperty("trace_id")
    private String traceId;
    @JsonProperty("task_id")
    private String taskId;
    private String punchType;
}
