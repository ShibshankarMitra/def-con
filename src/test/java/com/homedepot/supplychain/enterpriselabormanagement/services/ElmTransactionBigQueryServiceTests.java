package com.homedepot.supplychain.enterpriselabormanagement.services;

import com.google.cloud.bigquery.*;
import com.homedepot.supplychain.enterpriselabormanagement.exceptions.BigQueryResponseException;
import com.homedepot.supplychain.enterpriselabormanagement.utils.TestConstants;
import com.homedepot.supplychain.enterpriselabormanagement.utils.TestData;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

@ExtendWith(SpringExtension.class)
class ElmTransactionBigQueryServiceTests extends TestData {
    @InjectMocks
    ElmTransactionBigQueryService elmTransactionBigQueryService;
    @Mock
    BigQuery bigQuery;
    @Mock
    InsertAllResponse insertAllResponse;
    @Captor
    ArgumentCaptor<InsertAllRequest> insertAllRequestCaptor;

    @Value(TestConstants.TEST_DATASET_NAME)
    private String datasetName;
    @Value(TestConstants.TEST_TRANSACTION_TABLE_NAME)
    private String tableName;

    @BeforeEach
    public void setup() {
        ReflectionTestUtils.setField(elmTransactionBigQueryService, "datasetName", datasetName);
        ReflectionTestUtils.setField(elmTransactionBigQueryService, "tableName", tableName);
    }

    @Test
    void testInsertAllWithSuccess() {
        List<Map<String, Object>> rowMapperList = super.getRowMapperList();
        Mockito.when(bigQuery.insertAll(ArgumentMatchers.any(InsertAllRequest.class))).thenReturn(insertAllResponse);
        Mockito.when(insertAllResponse.hasErrors()).thenReturn(false);
        elmTransactionBigQueryService.insertAll(rowMapperList);
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
        List<Map<String, Object>> rowMapperList = super.getEmptyRowMapperList();
        //Verifying that insertAll() was never invoked as the rowMapperList is empty
        Mockito.verify(bigQuery, Mockito.times(0)).insertAll(insertAllRequestCaptor.capture());
        //Asserting that BigQueryResponseException is not thrown
        Assertions.assertDoesNotThrow(() -> elmTransactionBigQueryService.insertAll(rowMapperList));
    }

    @Test
    void testInsertAllWithInsertErrors() {
        List<Map<String, Object>> rowMapperList = super.getRowMapperList();
        Map<Long, List<BigQueryError>> insertErrorMap = getInsertErrorMap();
        Mockito.when(bigQuery.insertAll(ArgumentMatchers.any(InsertAllRequest.class))).thenReturn(insertAllResponse);
        Mockito.when(insertAllResponse.hasErrors()).thenReturn(true);
        Mockito.when(insertAllResponse.getInsertErrors()).thenReturn(insertErrorMap);
        //Asserting that BigQueryResponseException is thrown
        BigQueryResponseException bigQueryResponseException = Assertions.assertThrows(BigQueryResponseException.class, () -> elmTransactionBigQueryService.insertAll(rowMapperList));
        Assertions.assertTrue(bigQueryResponseException.getMessage().contains(insertErrorMap.get(0L).toString()));
    }

    @Test
    void testInsertAllWithWithBigQueryException() {
        List<Map<String, Object>> rowMapperList = super.getRowMapperList();
        Mockito.when(bigQuery.insertAll(ArgumentMatchers.any(InsertAllRequest.class))).thenThrow(BigQueryException.class);
        //Asserting that If any BigQuery exception is encountered it will be returned as is for the caller method to handle.
        Assertions.assertThrows(BigQueryException.class, () -> elmTransactionBigQueryService.insertAll(rowMapperList));
    }

    @Test
    void testGetTableName() {
        Assertions.assertNotNull(elmTransactionBigQueryService.getTableName());
    }

    @Test
    void testGetDatasetName() {
        Assertions.assertNotNull(elmTransactionBigQueryService.getDatasetName());
    }
}