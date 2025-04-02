package com.homedepot.supplychain.enterpriselabormanagement.services;

import com.google.cloud.bigquery.*;
import com.google.cloud.spring.pubsub.support.BasicAcknowledgeablePubsubMessage;
import com.homedepot.supplychain.enterpriselabormanagement.constants.CommonConstants;
import com.homedepot.supplychain.enterpriselabormanagement.constants.ErrorMessages;
import com.homedepot.supplychain.enterpriselabormanagement.exceptions.BigQueryResponseException;
import com.homedepot.supplychain.enterpriselabormanagement.exceptions.ElmBusinessException;
import com.homedepot.supplychain.enterpriselabormanagement.exceptions.ElmSystemException;
import com.homedepot.supplychain.enterpriselabormanagement.models.hdw.Data;
import com.homedepot.supplychain.enterpriselabormanagement.models.hdw.HdwTransaction;
import com.homedepot.supplychain.enterpriselabormanagement.utils.TestData;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
class ElmTransactionBigQueryServiceTests {
    @InjectMocks
    ElmTransactionBigQueryService elmTransactionBigQueryService;
    @Mock
    BigQuery bigQuery;
    @Mock
    PubSubPublisherService pubSubPublisherService;
    @Mock
    InsertAllResponse insertAllResponse;
    @Mock
    Map<String, Object> rowEntryMap;
    @Captor
    ArgumentCaptor<InsertAllRequest> insertAllRequestCaptor;
    @Mock
    HdwTransaction hdwTransaction;
    @Mock
    Data data;
    @Mock
    Iterator<Map<String, Object>> iterator;
    @Value(TestData.TEST_DATASET_NAME)
    private String datasetName;
    @Value(TestData.TEST_TRANSACTION_TABLE_NAME)
    private String tableName;
    @Value(TestData.TEST_VIEW_NAME)
    private String elmViewName;
    @Value(TestData.TEST_CICO_TABLE_NAME)
    private String cicoPunchesTableName;
    @Value(TestData.TEST_CICO_SUMMARY_TABLE_NAME)
    private String cicoSummaryTableName;
    @Mock
    private BasicAcknowledgeablePubsubMessage message;
    @Mock
    private List<Map<String, Object>> rowEntryMapList;

    @BeforeEach
    public void setup() {
        ReflectionTestUtils.setField(elmTransactionBigQueryService, "datasetName", datasetName);
        ReflectionTestUtils.setField(elmTransactionBigQueryService, "tableName", tableName);
        ReflectionTestUtils.setField(elmTransactionBigQueryService, "elmViewName", elmViewName);
        ReflectionTestUtils.setField(elmTransactionBigQueryService, "cicoPunchesTableName", cicoPunchesTableName);
        ReflectionTestUtils.setField(elmTransactionBigQueryService, "cicoSummaryTableName", cicoSummaryTableName);
    }

    @Test
    void testInsertAllWithSuccess() {
        List<Map<String, Object>> rowMapperList = TestData.getRowMapperList();
        Mockito.when(bigQuery.insertAll(ArgumentMatchers.any(InsertAllRequest.class))).thenReturn(insertAllResponse);
        Mockito.when(insertAllResponse.hasErrors()).thenReturn(false);
        elmTransactionBigQueryService.insertAll(rowMapperList, TestData.TEST_EVENT_TYPE);
        //Verifying that bigQuery.insertAll() was called only once and also Capturing the InsertAllRequest Argument passed for insertAll()
        Mockito.verify(bigQuery, Mockito.times(1)).insertAll(insertAllRequestCaptor.capture());
        //Asserting  that the InsertAllRequest has accurate table name and Dataset Name
        InsertAllRequest insertAllRequestCaptorValue = insertAllRequestCaptor.getValue();
        Assertions.assertEquals(insertAllRequestCaptorValue.getTable().getTable(), tableName);
        Assertions.assertEquals(insertAllRequestCaptorValue.getTable().getDataset(), datasetName);
        //Asserting  that the InsertAllRequest has same number of rows as ElmHdwTransactionList
        Assertions.assertEquals(insertAllRequestCaptorValue.getRows().size(), rowMapperList.size());
        //Iterating through the Row Content for each row passed with the InsertAllRequest
        List<InsertAllRequest.RowToInsert> rows = insertAllRequestCaptor.getValue().getRows();
        rows.forEach(row ->
            //Validating row contents: all columns of a row should be not null as we have passed all not null fields for the transaction
            row.getContent().forEach((key, value) -> Assertions.assertNotNull(value)));
    }

    @Test
    void testInsertAllWithEmptyRowMapperList() {
        List<Map<String, Object>> rowMapperList = TestData.getEmptyRowMapperList();
        //Verifying that insertAll() was never invoked as the rowMapperList is empty
        Mockito.verify(bigQuery, Mockito.times(0)).insertAll(insertAllRequestCaptor.capture());
        //Asserting that BigQueryResponseException is not thrown
        Assertions.assertDoesNotThrow(() -> elmTransactionBigQueryService.insertAll(rowMapperList, TestData.TEST_CICO_EVENT_TYPE ));
    }

    @Test
    void testInsertAllWithCicoEmptyRowMapperList() {
        List<Map<String, Object>> rowMapperList = TestData.getEmptyRowMapperList();
        //Verifying that insertAll() was never invoked as the rowMapperList is empty
        Mockito.verify(bigQuery, Mockito.times(0)).insertAll(insertAllRequestCaptor.capture());
        //Asserting that BigQueryResponseException is not thrown
        Assertions.assertDoesNotThrow(() -> elmTransactionBigQueryService.insertAll(rowMapperList, TestData.TEST_EVENT_TYPE ));
    }

    @Test
    void testInsertAllWithInsertErrors() {
        List<Map<String, Object>> rowMapperList = TestData.getRowMapperList();
        Map<Long, List<BigQueryError>> insertErrorMap = TestData.getInsertErrorMap();
        Mockito.when(bigQuery.insertAll(ArgumentMatchers.any(InsertAllRequest.class))).thenReturn(insertAllResponse);
        Mockito.when(insertAllResponse.hasErrors()).thenReturn(true);
        Mockito.when(insertAllResponse.getInsertErrors()).thenReturn(insertErrorMap);
        //Asserting that BigQueryResponseException is thrown
        BigQueryResponseException bigQueryResponseException = Assertions.assertThrows(BigQueryResponseException.class, () -> elmTransactionBigQueryService.insertAll(rowMapperList, TestData.TEST_EVENT_TYPE));
        Assertions.assertTrue(bigQueryResponseException.getMessage().contains(insertErrorMap.get(0L).toString()));
    }

    @Test
    void testInsertAllWithWithBigQueryException() {
        List<Map<String, Object>> rowMapperList = TestData.getRowMapperList();
        Mockito.when(bigQuery.insertAll(ArgumentMatchers.any(InsertAllRequest.class))).thenThrow(BigQueryException.class);
        //Asserting that If any BigQuery exception is encountered it will be returned as is for the caller method to handle.
        Assertions.assertThrows(BigQueryException.class, () -> elmTransactionBigQueryService.insertAll(rowMapperList, TestData.TEST_EVENT_TYPE));
    }

    @Test
    void testGetTableName() {
        Assertions.assertNotNull(elmTransactionBigQueryService.getTableName());
    }

    @Test
    void testGetDatasetName() {
        Assertions.assertNotNull(elmTransactionBigQueryService.getDatasetName());
    }

    @Test
    void testSelectQueryJobWithoutBigQueryException() throws InterruptedException {
        TableResult tableResult = Mockito.mock(TableResult.class);
        Mockito.when(bigQuery.query(ArgumentMatchers.any(QueryJobConfiguration.class))).thenReturn(tableResult);
        Assertions.assertDoesNotThrow(() -> elmTransactionBigQueryService.getExistingTraceIdsFromBqViewOrTable(TestData.TEST_TRACE_ID, TestData.TEST_DC_NUMBER, TestData.TEST_EVENT_TYPE));
    }

    @Test
    void testSelectQueryJobWithBigQueryException() throws InterruptedException, BigQueryException {
        Mockito.when(bigQuery.query(ArgumentMatchers.any(QueryJobConfiguration.class))).thenThrow(BigQueryException.class);
        ElmSystemException exception = Assertions.assertThrows(ElmSystemException.class, () -> elmTransactionBigQueryService.getExistingTraceIdsFromBqViewOrTable(TestData.TEST_TRACE_ID, TestData.TEST_DC_NUMBER, TestData.TEST_EVENT_TYPE));
        Assertions.assertEquals(elmViewName,exception.getSource());
        Assertions.assertEquals(ErrorMessages.QUERY_JOB_FAILED, exception.getMessage());
    }

    @Test
    void testSelectQueryJobWithInterruptedException() throws InterruptedException, BigQueryException {
        Mockito.when(bigQuery.query(ArgumentMatchers.any(QueryJobConfiguration.class))).thenThrow(InterruptedException.class);
        ElmSystemException exception = Assertions.assertThrows(ElmSystemException.class, () -> elmTransactionBigQueryService.getExistingTraceIdsFromBqViewOrTable(TestData.TEST_TRACE_ID, TestData.TEST_DC_NUMBER, TestData.TEST_EVENT_TYPE));
        Assertions.assertEquals(elmViewName,exception.getSource());
        Assertions.assertEquals(ErrorMessages.QUERY_JOB_INTERRUPTED, exception.getMessage());
    }

    @Test
    void testCicoSelectQueryJobWithoutBigQueryException() throws InterruptedException {
        TableResult tableResult = Mockito.mock(TableResult.class);
        Mockito.when(bigQuery.query(ArgumentMatchers.any(QueryJobConfiguration.class))).thenReturn(tableResult);
        Assertions.assertDoesNotThrow(() -> elmTransactionBigQueryService.getExistingTraceIdsFromBqViewOrTable(TestData.TEST_TRACE_ID, TestData.TEST_DC_NUMBER, TestData.TEST_CICO_EVENT_TYPE));
    }

    @Test()
    @DisplayName("test- Run query job with Return Value as TableResult ")
    void testSelectCicoTablesQueryJobCicoPunchesWithReturnAsTableResult() {
        TableResult tableResult = TestData.mockTableResult();
        when(elmTransactionBigQueryService.selectQueryJob(TestData.TEST_TRANSACTION_ID,TestData.TEST_MESSAGE_ID, CommonConstants.CICO_PUNCHES_TABLE))
                .thenReturn(tableResult);
        Assertions.assertNotNull(tableResult);
    }
    @Test()
    @DisplayName("test- Run query job with Return Value as TableResult ")
    void testSelectCicoTablesQueryJobCicoSummaryWithReturnAsTableResult() {
        TableResult tableResult = TestData.mockTableResult();
        when(elmTransactionBigQueryService.selectQueryJob(TestData.TEST_USER_ID,TestData.TEST_PUNCH_DATE,CommonConstants.CICO_SUMMARY_TABLE))
                .thenReturn(tableResult);
        Assertions.assertNotNull(tableResult);
    }

    @Test()
    @DisplayName("test- Run query job throw IllegalStateException exception if table not specify")
    void testSelectQueryJobNotTableSpecifyThrowIllegalStateException() {
        IllegalStateException illegalStateException = Assertions.assertThrows(IllegalStateException.class, () -> elmTransactionBigQueryService.selectQueryJob(TestData.TEST_USER_ID, TestData.TEST_PUNCH_DATE, ""));
        Assertions.assertTrue(illegalStateException.getMessage().contains("Missing or Unexpected value for queryTable:"));
    }

    @Test()
    @DisplayName("test- Run query job throw BigQueryException")
    void testSelectQueryJobAndThrowBigqueryException() throws InterruptedException {
        Mockito.when(bigQuery.query(any(QueryJobConfiguration.class))).thenThrow(BigQueryException.class);
        ElmSystemException exception = Assertions.assertThrows(ElmSystemException.class, () -> elmTransactionBigQueryService.selectQueryJob(TestData.TEST_USER_ID, TestData.TEST_PUNCH_DATE, CommonConstants.CICO_SUMMARY_TABLE));
        Assertions.assertNotNull(exception);
        //Asserting thrown exception should contain expected message and source.
        Assertions.assertEquals(TestData.TEST_CICO_SUMMARY_TABLE_NAME, exception.getSource());
        Assertions.assertEquals(ErrorMessages.QUERY_JOB_FAILED, exception.getMessage());
    }

    @Test()
    @DisplayName("test- Run query job throw InterruptedException")
    void testSelectQueryJobAndThrowInterruptedException() throws InterruptedException {
        Mockito.when(bigQuery.query(any(QueryJobConfiguration.class))).thenThrow(InterruptedException.class);
        ElmSystemException exception = Assertions.assertThrows(ElmSystemException.class, () -> elmTransactionBigQueryService.selectQueryJob(TestData.TEST_USER_ID, TestData.TEST_PUNCH_DATE, CommonConstants.CICO_SUMMARY_TABLE));
        Assertions.assertNotNull(exception);
        //Asserting thrown exception should contain expected message and source.
        Assertions.assertEquals(TestData.TEST_CICO_SUMMARY_TABLE_NAME, exception.getSource());
        Assertions.assertEquals(ErrorMessages.QUERY_JOB_INTERRUPTED, exception.getMessage());
    }

    @Test()
    @DisplayName("test- Run Insert CICO query job Successfully")
    void testInsertCicoSummaryQueryJobSuccessfully() {
        Mockito.when(hdwTransaction.getLaborEventDetail()).thenReturn(data);
        Assertions.assertDoesNotThrow(() -> elmTransactionBigQueryService.insertCicoSummaryQueryJob(hdwTransaction, TestData.TEST_DC_NUMBER, TestData.TEST_TOTAL_HOURS_WORKED_FLOAT, TestData.TEST_PUNCH_DATE));
    }

    @Test()
    @DisplayName("test- Run query job throw BigQueryException")
    void testInsertCicoSummaryQueryJobAndThrowBigQueryException() throws InterruptedException {
        Mockito.when(hdwTransaction.getLaborEventDetail()).thenReturn(data);
        Mockito.when(bigQuery.query(any(QueryJobConfiguration.class))).thenThrow(BigQueryException.class);
        ElmSystemException exception = Assertions.assertThrows(ElmSystemException.class, () -> elmTransactionBigQueryService.insertCicoSummaryQueryJob(hdwTransaction, TestData.TEST_DC_NUMBER, TestData.TEST_TOTAL_HOURS_WORKED_FLOAT, TestData.TEST_PUNCH_DATE));
        Assertions.assertNotNull(exception);
        //Asserting thrown exception should contain expected message and source.
        Assertions.assertEquals(TestData.TEST_CICO_SUMMARY_TABLE_NAME, exception.getSource());
        Assertions.assertEquals(ErrorMessages.QUERY_JOB_FAILED, exception.getMessage());
    }

    @Test()
    @DisplayName("test- Run query job throw InterruptedException")
    void testInsertCicoSummaryQueryJobAndThrowInterruptedException() throws InterruptedException {
        Mockito.when(bigQuery.query(any(QueryJobConfiguration.class))).thenThrow(InterruptedException.class);
        Mockito.when(hdwTransaction.getLaborEventDetail()).thenReturn(data);
        ElmSystemException exception = Assertions.assertThrows(ElmSystemException.class, () -> elmTransactionBigQueryService.insertCicoSummaryQueryJob(hdwTransaction, TestData.TEST_DC_NUMBER, TestData.TEST_TOTAL_HOURS_WORKED_FLOAT, TestData.TEST_PUNCH_DATE));
        Assertions.assertNotNull(exception);
        //Asserting thrown exception should contain expected message and source.
        Assertions.assertEquals(TestData.TEST_CICO_SUMMARY_TABLE_NAME, exception.getSource());
        Assertions.assertEquals(ErrorMessages.QUERY_JOB_INTERRUPTED, exception.getMessage());
    }

    @Test()
    @DisplayName("test- Run Update CICO query job Successfully")
    void testUpdateCicoSummaryQueryJobSuccessfully() {
        Mockito.when(hdwTransaction.getLaborEventDetail()).thenReturn(data);
        Assertions.assertDoesNotThrow(() -> elmTransactionBigQueryService.updateCicoSummaryQueryJob(TestData.TEST_USER_ID, TestData.TEST_TOTAL_HOURS_WORKED_FLOAT, TestData.TEST_PUNCH_DATE));
    }

    @Test()
    @DisplayName("test- Run query job throw InterruptedException")
    void testUpdateCicoSummaryQueryJobAndThrowInterruptedException() throws InterruptedException {
        Mockito.when(bigQuery.query(any(QueryJobConfiguration.class))).thenThrow(InterruptedException.class);
        Mockito.when(hdwTransaction.getLaborEventDetail()).thenReturn(data);
        ElmSystemException exception = Assertions.assertThrows(ElmSystemException.class, () -> elmTransactionBigQueryService.updateCicoSummaryQueryJob(TestData.TEST_USER_ID, TestData.TEST_TOTAL_HOURS_WORKED_FLOAT, TestData.TEST_PUNCH_DATE));
        Assertions.assertNotNull(exception);
        //Asserting thrown exception should contain expected message and source.
        Assertions.assertEquals(TestData.TEST_CICO_SUMMARY_TABLE_NAME, exception.getSource());
        Assertions.assertEquals(ErrorMessages.QUERY_JOB_INTERRUPTED, exception.getMessage());
    }

    @Test()
    @DisplayName("test- Run query job throw BigQueryException")
    void testUpdateCicoSummaryQueryJobAndThrowBigQueryException() throws InterruptedException {
        Mockito.when(bigQuery.query(any(QueryJobConfiguration.class))).thenThrow(BigQueryException.class);
        Mockito.when(hdwTransaction.getLaborEventDetail()).thenReturn(data);
        ElmSystemException exception = Assertions.assertThrows(ElmSystemException.class, () -> elmTransactionBigQueryService.updateCicoSummaryQueryJob(TestData.TEST_USER_ID, TestData.TEST_TOTAL_HOURS_WORKED_FLOAT, TestData.TEST_PUNCH_DATE));
        Assertions.assertNotNull(exception);
        //Asserting thrown exception should contain expected message and source.
        Assertions.assertEquals(TestData.TEST_CICO_SUMMARY_TABLE_NAME, exception.getSource());
        Assertions.assertEquals(ErrorMessages.QUERY_JOB_FAILED, exception.getMessage());
    }

    @Test()
    @DisplayName("test- Run InsertBqStreaming with no exception throws")
    void testInsertBqStreamingExceptionNotThrown() {
        HdwTransaction hdwTransaction = TestData.getHdwTransaction(TestData.TEST_PUNCH_TYPE_IN);
        Mockito.when(rowEntryMapList.iterator()).thenReturn(iterator);
        Mockito.when(iterator.hasNext()).thenReturn(Boolean.TRUE);
        Assertions.assertDoesNotThrow(() -> elmTransactionBigQueryService.insertBqStreaming(hdwTransaction,message,TestData.TEST_DC_NUMBER, null));
    }

    @Test()
    @DisplayName("test- Run InsertBqStreaming with BigQueryResponseException exception throws")
    void testInsertBqStreamingBigQueryResponseExceptionThrow() {
        HdwTransaction hdwTransaction = TestData.getHdwTransaction(TestData.TEST_PUNCH_TYPE_IN);
        Mockito.when(rowEntryMapList.iterator()).thenReturn(iterator);
        Mockito.when(iterator.hasNext()).thenReturn(true,false);
        Mockito.when(iterator.next()).thenReturn(rowEntryMap,Mockito.mock(Map.class));
        Mockito.when(bigQuery.insertAll(any())).thenThrow(BigQueryResponseException.class);
        Mockito.doNothing().when(pubSubPublisherService).createAndPublishConsumerAckMessage(anyString(),anyString());
        ElmBusinessException elmBusinessException = Assertions.assertThrows(ElmBusinessException.class, () -> elmTransactionBigQueryService.insertBqStreaming(hdwTransaction, message, TestData.TEST_DC_NUMBER, rowEntryMapList));
        Assertions.assertEquals(ErrorMessages.BIGQUERY_INSERT_RESPONSE_ERROR,elmBusinessException.getMessage());
        verify(message, Mockito.times(1)).ack();
    }

    @Test()
    @DisplayName("test- Run InsertBqStreaming with BigQueryException exception throws")
    void testInsertBqStreamingBigQueryExceptionThrow() {
        HdwTransaction hdwTransaction = TestData.getHdwTransaction(TestData.TEST_PUNCH_TYPE_IN);
        Mockito.when(rowEntryMapList.iterator()).thenReturn(iterator);
        Mockito.when(iterator.hasNext()).thenReturn(true,false);
        Mockito.when(iterator.next()).thenReturn(rowEntryMap,Mockito.mock(Map.class));
        Mockito.when(bigQuery.insertAll(any())).thenThrow(BigQueryException.class);
        Mockito.doNothing().when(pubSubPublisherService).createAndPublishConsumerAckMessage(anyString(),anyString());
        Assertions.assertThrows(ElmSystemException.class,() -> elmTransactionBigQueryService.insertBqStreaming(hdwTransaction,message,TestData.TEST_DC_NUMBER, rowEntryMapList));
        verify(message, Mockito.times(0)).ack();
    }
}