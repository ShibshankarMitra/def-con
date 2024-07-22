package com.homedepot.supplychain.enterpriselabormanagement.services;

import com.google.cloud.bigquery.*;
import com.homedepot.supplychain.enterpriselabormanagement.annotations.NoIntercept;
import com.homedepot.supplychain.enterpriselabormanagement.constants.ElmTransactionBqHeaders;
import com.homedepot.supplychain.enterpriselabormanagement.exceptions.BigQueryResponseException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ElmTransactionBigQueryService {

    private final BigQuery bigquery;

    @Getter
    @Value("${spring.cloud.gcp.bigquery.dataset-name}")
    private String datasetName;

    @Getter
    @Value("${elm.gcp.bigquery.elm-transactions-table-name}")
    private String tableName;

    @Autowired
    public ElmTransactionBigQueryService(BigQuery bigquery) {
        this.bigquery = bigquery;
    }

    @NoIntercept
    public void insertAll(List<Map<String, Object>> rowEntryMapList) throws BigQueryResponseException {
        InsertAllRequest.Builder insertAllRequestBuilder = InsertAllRequest.newBuilder(datasetName, tableName);
        if (!ObjectUtils.isEmpty(rowEntryMapList)) {
            for (Map<String, Object> rowEntryMap : rowEntryMapList) {
                insertAllRequestBuilder.addRow(rowEntryMap);
            }
            InsertAllRequest insertAllRequest = insertAllRequestBuilder.build();
            InsertAllResponse insertAllResponse = bigquery.insertAll(insertAllRequest);
            if (insertAllResponse.hasErrors()) {
                Map<String, String> rowErrorMap = insertAllResponse.getInsertErrors().entrySet().stream().filter(e -> !StringUtils.isBlank(e.getValue().get(0).getMessage()))
                        .collect(Collectors.toMap(e -> rowEntryMapList.get(Math.toIntExact(e.getKey())).toString(), e -> e.getValue().toString()));
                log.warn("InsertAllResponse has errors for {} rows out of total {} rows", rowErrorMap.size(), insertAllRequest.getRows().size());
                StringBuilder errorMessageBuilder = new StringBuilder();
                rowErrorMap.forEach((row, error) -> errorMessageBuilder.append("\n").append("ROW : ").append(row).append(" ERROR : ").append(error));
                throw new BigQueryResponseException(errorMessageBuilder.toString());
            } else {
                rowEntryMapList.forEach(e -> log.info("Successfully inserted transaction with id: {} ",
                        e.get(ElmTransactionBqHeaders.TRANSACTION_ID)));
            }
        }
    }
}