package com.homedepot.supplychain.enterpriselabormanagement.services;

import com.google.cloud.bigquery.*;
import com.google.cloud.spring.pubsub.support.BasicAcknowledgeablePubsubMessage;
import com.homedepot.supplychain.enterpriselabormanagement.annotations.NoIntercept;
import com.homedepot.supplychain.enterpriselabormanagement.constants.*;
import com.homedepot.supplychain.enterpriselabormanagement.exceptions.BigQueryResponseException;
import com.homedepot.supplychain.enterpriselabormanagement.exceptions.ElmBusinessException;
import com.homedepot.supplychain.enterpriselabormanagement.exceptions.ElmSystemException;
import com.homedepot.supplychain.enterpriselabormanagement.models.hdw.HdwTransaction;
import com.homedepot.supplychain.enterpriselabormanagement.utils.CommonUtils;
import com.homedepot.supplychain.enterpriselabormanagement.utils.TransactionUtils;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ElmTransactionBigQueryService {

    private final BigQuery bigquery;
    private final PubSubPublisherService pubSubPublisherService;

    @Getter
    @Value("${spring.cloud.gcp.bigquery.dataset-name}")
    private String datasetName;

    @Getter
    @Value("${elm.gcp.bigquery.elm-transactions-table-name}")
    private String tableName;
    @Value("${elm.gcp.bigquery.cico-punches-table-name}")
    private String cicoPunchesTableName;
    @Value("${elm.gcp.bigquery.cico-summary-table-name}")
    private String cicoSummaryTableName;
    @Value("${elm.gcp.bigquery.elm-view-name}")
    private String elmViewName;
    @Value("${spring.cloud.gcp.project-id}")
    private String elmProjectId;

    @Autowired
    public ElmTransactionBigQueryService(BigQuery bigquery, PubSubPublisherService pubSubPublisherService) {
        this.bigquery = bigquery;
        this.pubSubPublisherService = pubSubPublisherService;
    }

    @NoIntercept
    public void insertAll(List<Map<String, Object>> rowEntryMapList, String eventType) throws BigQueryResponseException {
        InsertAllRequest.Builder insertAllRequestBuilder;
        if (!CommonConstants.CICO_EVENT.equals(eventType)) {
            insertAllRequestBuilder = InsertAllRequest.newBuilder(datasetName, tableName);
        } else {
            insertAllRequestBuilder = InsertAllRequest.newBuilder(datasetName, cicoPunchesTableName);
        }
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
                rowEntryMapList.forEach(e -> log.info("Successfully inserted into BQ"));
            }
        }
    }

    @NoIntercept
    public TableResult getExistingTraceIdsFromBqViewOrTable(String traceId, String dcNumber, String eventType){
        TableResult tableResult;
        String query;
        if(!CommonConstants.CICO_EVENT.equals(eventType)) {
           query = String.format(BigQueries.CICO_PUNCHES_QUERY_BY_TRACE_ID_AND_DC_NUMBER, elmProjectId, datasetName, elmViewName, traceId, dcNumber);
        }else{
           query = String.format(BigQueries.ELM_EVENTS_VIEW_QUERY_BY_TRACE_ID_AND_DC_NUMBER, elmProjectId, datasetName, cicoPunchesTableName, traceId, dcNumber);
        }
        QueryJobConfiguration queryConfig = QueryJobConfiguration.newBuilder(query).build();
        try {
            tableResult = bigquery.query(queryConfig);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ElmSystemException(elmViewName, ErrorMessages.QUERY_JOB_INTERRUPTED, e);
        } catch (BigQueryException e) {
            throw new ElmSystemException(elmViewName, ErrorMessages.QUERY_JOB_FAILED, e);
        }
        return tableResult;
    }

    @NoIntercept
    public TableResult selectQueryJob(String userId, String punchDate, String queryTable) {
        TableResult result;
        try {
            // creating Query
            String cicoTableQuery = switch (queryTable){
                case CommonConstants.CICO_PUNCHES_TABLE ->
                         String.format(BigQueries.CICO_PUNCHES_QUERY_BY_USER_ID_AND_PUNCH_DATE, elmProjectId, datasetName, cicoPunchesTableName, userId, punchDate);
                case CommonConstants.CICO_SUMMARY_TABLE ->
                        String.format(BigQueries.CICO_SUMMARY_QUERY_BY_USER_ID_AND_PUNCH_DATE, elmProjectId, datasetName, cicoSummaryTableName, userId, punchDate);
                default -> throw new IllegalStateException("Missing or Unexpected value for queryTable: " + queryTable);
            };
            // Create the query job.
            QueryJobConfiguration queryConfig = QueryJobConfiguration.newBuilder(cicoTableQuery).build();
            // Execute the query.
            result = bigquery.query(queryConfig);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ElmSystemException(queryTable, ErrorMessages.QUERY_JOB_INTERRUPTED, e);
        } catch (BigQueryException e) {
            throw new ElmSystemException(queryTable, ErrorMessages.QUERY_JOB_FAILED, e);
        }
        return result;
    }

    @NoIntercept
    public void insertCicoSummaryQueryJob(HdwTransaction hdwTransaction, String dcNumber, double totalHoursWorked, String punchDate) {
        try {
            // creating Query
            String cicoSummaryQuery = String.format(BigQueries.CICO_SUMMARY_INSERT, elmProjectId, datasetName, cicoSummaryTableName, hdwTransaction.getLaborEventDetail().getUserId(), hdwTransaction.getLaborEventDetail().getUserName(), punchDate, totalHoursWorked, dcNumber, CommonUtils.getCurrentTimeStampToBqFormat(), CommonUtils.getCurrentTimeStampToBqFormat());
            // Create the query job.
            QueryJobConfiguration queryConfig = QueryJobConfiguration.newBuilder(cicoSummaryQuery).build();
            // Execute the query.
            bigquery.query(queryConfig);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ElmSystemException(cicoSummaryTableName, ErrorMessages.QUERY_JOB_INTERRUPTED, e);
        } catch (BigQueryException e) {
            throw new ElmSystemException(cicoSummaryTableName, ErrorMessages.QUERY_JOB_FAILED, e);
        }
    }

    @NoIntercept
    public void updateCicoSummaryQueryJob(String userId, double totalHoursWorked, String punchDate) {
        try {
            // creating Query
            String cicoSummaryInsertQuery = String.format(BigQueries.CICO_SUMMARY_UPDATE_TOTAL_HOURS_WORKED, elmProjectId, datasetName, cicoSummaryTableName, TransactionUtils.formatCicoTotalHoursWorked(totalHoursWorked), CommonUtils.getCurrentTimeStampToBqFormat(), userId, punchDate);
            // Create the query job.
            QueryJobConfiguration queryConfig = QueryJobConfiguration.newBuilder(cicoSummaryInsertQuery).build();
            // Execute the query.
            bigquery.query(queryConfig);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ElmSystemException(cicoSummaryTableName, ErrorMessages.QUERY_JOB_INTERRUPTED, e);
        } catch (BigQueryException e) {
            throw new ElmSystemException(cicoSummaryTableName, ErrorMessages.QUERY_JOB_FAILED, e);
        }
    }

    @NoIntercept
    public void insertBqStreaming(HdwTransaction hdwTransaction, BasicAcknowledgeablePubsubMessage originalMessage, String dcNumber, List<Map<String, Object>> rows) {
        try {
            insertAll(rows, hdwTransaction.getLaborEvent().getEventType());
        }catch (BigQueryResponseException e) {
            originalMessage.ack();
            pubSubPublisherService.createAndPublishConsumerNackMessage(dcNumber, hdwTransaction.getLaborEvent().getTraceId(), ErrorMessages.BIGQUERY_INSERT_RESPONSE_ERROR);
            throw new ElmBusinessException(ErrorMessages.BIGQUERY_INSERT_RESPONSE_ERROR, e);
        } catch (BigQueryException e) {
            throw new ElmSystemException(this.getTableName(), e.getMessage(), e);
        }
    }
}