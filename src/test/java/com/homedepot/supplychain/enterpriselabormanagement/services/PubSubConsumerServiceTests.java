package com.homedepot.supplychain.enterpriselabormanagement.services;

import com.fasterxml.jackson.core.JsonParseException;
import com.google.cloud.bigquery.FieldValue;
import com.google.cloud.bigquery.FieldValueList;
import com.google.cloud.bigquery.TableResult;
import com.google.cloud.spring.pubsub.support.BasicAcknowledgeablePubsubMessage;
import com.homedepot.supplychain.enterpriselabormanagement.constants.ErrorMessages;
import com.homedepot.supplychain.enterpriselabormanagement.exceptions.DataProcessingException;
import com.homedepot.supplychain.enterpriselabormanagement.exceptions.ElmBusinessException;
import com.homedepot.supplychain.enterpriselabormanagement.exceptions.JsonValidationException;
import com.homedepot.supplychain.enterpriselabormanagement.utils.TestData;
import com.homedepot.supplychain.enterpriselabormanagement.utils.TestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;


@ExtendWith(SpringExtension.class)
class PubSubConsumerServiceTests {
    @InjectMocks
    PubSubConsumerService pubSubConsumerService;
    @Mock
    PubSubPublisherService pubSubPublisherService;
    @Mock
    CicoProcessorService cicoProcessorService;
    @Mock
    private ElmTransactionBigQueryService elmTransactionBigQueryService;
    @Mock
    private BasicAcknowledgeablePubsubMessage message;
    @Mock
    private TableResult tableResult;
    @Mock
    FieldValue fieldValue;
    @Mock
    FieldValueList fieldValueList;
    @Captor
    private ArgumentCaptor<String> dcNumberCaptor;
    @Captor
    private ArgumentCaptor<String> traceIdCaptor;
    @Captor
    private ArgumentCaptor<String> eventTypeCaptor;

    @BeforeEach()
    public void setUp() {
    }

    @Test()
    @DisplayName("test- Message with Blank Body")
    void testProcessPubSubToBqWithBlankMessageIDAndBody() {
        String messageBody = TestData.getEmptyPayload();
        //Asserting that ElmBusinessException is thrown
        ElmBusinessException elmBusinessException = Assertions.assertThrows(ElmBusinessException.class, () -> pubSubConsumerService.processPubSubToBq(messageBody, message, ""));
        //Asserting that Error message is properly populated
        Assertions.assertTrue(elmBusinessException.getMessage().contains(ErrorMessages.JSON_READ_FAILED));
        //Asserting that the reason of exception is JsonValidationException
        Assertions.assertEquals(JsonValidationException.class, elmBusinessException.getCause().getClass());
        Assertions.assertTrue(elmBusinessException.getCause().getMessage().contains(ErrorMessages.MESSAGE_BODY_BLANK));
        //Verifying that message was acknowledged only once
        Mockito.verify(message, Mockito.times(1)).ack();
    }

    @Test()
    @DisplayName("test- Message with Invalid JSON Syntax e.g. text or xml")
    void testProcessPubSubToBqWithJsonParseException() {
        String messageBody = TestData.getTextPayload();
        //Asserting that elmBusinessException is thrown in this case as It is a known Business Exception regarding JSON Parsing and should be handled by code
        ElmBusinessException elmBusinessException = Assertions.assertThrows(ElmBusinessException.class, () -> pubSubConsumerService.processPubSubToBq(messageBody, message, ""));
        //Asserting that Error message is properly populated
        Assertions.assertTrue(elmBusinessException.getMessage().contains(ErrorMessages.JSON_READ_FAILED));
        //Asserting that the reason of exception is JsonParseException
        Assertions.assertEquals(JsonParseException.class, elmBusinessException.getCause().getClass());
        //Asserting that Exception has valid data
        Assertions.assertNotNull(elmBusinessException.getMessage());
        Assertions.assertNotNull(elmBusinessException.getCause());
        //Verifying that message was acknowledged only once
        Mockito.verify(message, Mockito.times(1)).ack();
    }

    @Test()
    @DisplayName("test- Message with Invalid JSON data/ schema")
    void testProcessPubSubToBqWithJsonValidationException() throws IOException {
        String messageBody = TestData.getInvalidFieldsJsonPayload();
        //Asserting that elmBusinessException is thrown in this case as It is a known Business Exception regarding JSON Parsing and should be handled by code
        ElmBusinessException elmBusinessException = Assertions.assertThrows(ElmBusinessException.class, () -> pubSubConsumerService.processPubSubToBq(messageBody, message, ""));
        //Asserting that Error message is properly populated
        Assertions.assertTrue(elmBusinessException.getMessage().contains(ErrorMessages.JSON_READ_FAILED));
        //Asserting that the reason of exception is JsonValidationException
        Assertions.assertEquals(JsonValidationException.class, elmBusinessException.getCause().getClass());
        Assertions.assertTrue(elmBusinessException.getCause().getMessage().contains(ErrorMessages.JSON_SCHEMA_VALIDATION_FAILED));
        //Asserting that Exception has valid data
        Assertions.assertNotNull(elmBusinessException.getMessage());
        Assertions.assertNotNull(elmBusinessException.getCause());
        //Verifying that message was acknowledged only once
        Mockito.verify(message, Mockito.times(1)).ack();
    }

    @Test()
    @DisplayName("test- Message with Invalid event_type in JSON")
    void testProcessPubSubToBqWithInvalidEventType() throws IOException {
        String messageBody = TestData.getInvalidEventTypeJsonPayload();
        //Asserting that elmBusinessException is thrown in this case as It is a known Business Exception regarding JSON Parsing and should be handled by code
        ElmBusinessException elmBusinessException = Assertions.assertThrows(ElmBusinessException.class, () -> pubSubConsumerService.processPubSubToBq(messageBody, message, ""));
        //Asserting that Error message is properly populated
        Assertions.assertTrue(elmBusinessException.getMessage().contains(ErrorMessages.JSON_READ_FAILED));
        //Asserting that the reason of exception is JsonValidationException
        Assertions.assertEquals(IllegalStateException.class, elmBusinessException.getCause().getClass());
    }

    @Test()
    @DisplayName("test- Message with Invalid Build data")
    void testProcessPubSubToBqWithInvalidBuilds() throws IOException {
        String messageBody = TestData.getInvalidBuildsJsonPayload();
        when(elmTransactionBigQueryService.getExistingTraceIdsFromBqViewOrTable(anyString(),anyString(), anyString())).thenReturn(tableResult);
        ElmBusinessException elmBusinessException = Assertions.assertThrows(ElmBusinessException.class, () -> pubSubConsumerService.processPubSubToBq(messageBody, message, ""));
        //Verifying the proper exception details were caught
        Assertions.assertSame(DataProcessingException.class, elmBusinessException.getCause().getClass());
        //Verifying that message was acknowledged only once
        Mockito.verify(message, Mockito.times(1)).ack();
    }

    @Test()
    @DisplayName("test- Message with duplicate Build data")
    void testProcessPubSubToBqWithDuplicateBuilds() throws IOException {
        String messageBody = TestData.getDuplicateBuildsJsonPayload();
        when(elmTransactionBigQueryService.getExistingTraceIdsFromBqViewOrTable(anyString(),anyString(), anyString())).thenReturn(tableResult);
        ElmBusinessException elmBusinessException = Assertions.assertThrows(ElmBusinessException.class, () -> pubSubConsumerService.processPubSubToBq(messageBody, message, ""));
        //Verifying the proper exception details were caught
        Assertions.assertSame(DataProcessingException.class, elmBusinessException.getCause().getClass());
        //Verifying that message was acknowledged only once
        Mockito.verify(message, Mockito.times(1)).ack();
    }

    @Test()
    @DisplayName("test- Message with invalid package hierarchy data")
    void testProcessPubSubToBqWithInvalidPackageHierarchies() throws IOException {
        String messageBody = TestData.getInvalidPackageHierarchyJsonPayload();
        when(elmTransactionBigQueryService.getExistingTraceIdsFromBqViewOrTable(anyString(),anyString(), anyString())).thenReturn(tableResult);
        ElmBusinessException elmBusinessException = Assertions.assertThrows(ElmBusinessException.class, () -> pubSubConsumerService.processPubSubToBq(messageBody, message, ""));
        //Verifying the proper exception details were caught
        Assertions.assertSame(DataProcessingException.class, elmBusinessException.getCause().getClass());
        //Verifying that message was acknowledged only once
        Mockito.verify(message, Mockito.times(1)).ack();
    }

    @Test()
    @DisplayName("test- Message with Duplicate package hierarchy data")
    void testProcessPubSubToBqWithDuplicatePackageHierarchies() throws IOException {
        String messageBody = TestData.getDuplicatePackageHierarchyJsonPayload();
        when(elmTransactionBigQueryService.getExistingTraceIdsFromBqViewOrTable(anyString(),anyString(), anyString())).thenReturn(tableResult);
        ElmBusinessException elmBusinessException = Assertions.assertThrows(ElmBusinessException.class, () -> pubSubConsumerService.processPubSubToBq(messageBody, message, ""));
        //Verifying the proper exception details were caught
        Assertions.assertSame(DataProcessingException.class, elmBusinessException.getCause().getClass());
        //Verifying that message was acknowledged only once
        Mockito.verify(message, Mockito.times(1)).ack();
    }

    @Test()
    @DisplayName("test- successful reading of PubSub pick lpn from active message and insert to Transactions table")
    void testProcessPubSubToBqWithSuccessPickLpnFromActive() throws IOException {
        String messageBody = TestData.getValidJsonPayloadPickLpnFromActive();
        when(elmTransactionBigQueryService.getExistingTraceIdsFromBqViewOrTable(anyString(),anyString(), anyString())).thenReturn(tableResult);
        Mockito.doNothing().when(elmTransactionBigQueryService).insertAll(ArgumentMatchers.anyList(), anyString());
        pubSubConsumerService.processPubSubToBq(messageBody, message, "");
        //Verifying that message was acknowledged only once
        Mockito.verify(message, Mockito.times(1)).ack();
    }

    @Test()
    @DisplayName("test- successful reading of PubSub replen allocation message and insert to Transactions table")
    void testProcessPubSubToBqWithSuccessReplenishment() throws IOException {
        String messageBody = TestData.getValidJsonPayloadReplenAllocation();
        when(elmTransactionBigQueryService.getExistingTraceIdsFromBqViewOrTable(anyString(),anyString(), anyString())).thenReturn(tableResult);
        Mockito.doNothing().when(elmTransactionBigQueryService).insertAll(ArgumentMatchers.anyList(), anyString());
        pubSubConsumerService.processPubSubToBq(messageBody, message, "");
        //Verifying that message was acknowledged only once
        Mockito.verify(message, Mockito.times(1)).ack();
    }

    @Test()
    @DisplayName("test- successful reading of PubSub message and insert to Transactions table for Indirect events")
    void testProcessPubSubToBqWithSuccessIndirectEvents() throws IOException {
        String messageBody = TestData.getValidJsonPayloadIndirect();
        when(elmTransactionBigQueryService.getExistingTraceIdsFromBqViewOrTable(anyString(),anyString(), anyString())).thenReturn(tableResult);
        Mockito.doNothing().when(elmTransactionBigQueryService).insertAll(ArgumentMatchers.anyList(), anyString());
        pubSubConsumerService.processPubSubToBq(messageBody, message, "");
        //Verifying that message was acknowledged only once
        Mockito.verify(message, Mockito.times(1)).ack();
    }

    @Test()
    @DisplayName("test- successful reading of PubSub message with Multiple build ids and insert to Transactions table")
    void testProcessPubSubToBqWithSuccessMultipleBuilds() throws IOException {
        String messageBody = TestData.getValidJsonPayloadWithMultipleBuilds();
        when(elmTransactionBigQueryService.getExistingTraceIdsFromBqViewOrTable(anyString(),anyString(), anyString())).thenReturn(tableResult);
        Mockito.doNothing().when(elmTransactionBigQueryService).insertAll(ArgumentMatchers.anyList(), anyString());
        pubSubConsumerService.processPubSubToBq(messageBody, message, "");
        //Verifying that message was acknowledged only once
        Mockito.verify(message, Mockito.times(1)).ack();
    }

    @Test()
    @DisplayName("test- successful reading of PubSub Load-lpn-lm message by ignoring locationQty and vendor_number under location object and insert to Transactions table")
    void testProcessPubSubToBqWithSuccessLoadLpnLm() throws IOException {
        String messageBody = TestData.getValidJsonPayloadLoadLpnLm();
        when(elmTransactionBigQueryService.getExistingTraceIdsFromBqViewOrTable(anyString(),anyString(), anyString())).thenReturn(tableResult);
        Mockito.doNothing().when(elmTransactionBigQueryService).insertAll(ArgumentMatchers.anyList(), anyString());
        pubSubConsumerService.processPubSubToBq(messageBody, message, "");
        //Verifying that message was acknowledged only once
        Mockito.verify(message, Mockito.times(1)).ack();
    }

    @Test()
    @DisplayName("test- Invalid contract_version passed in message attribute then throw ElmBusinessException")
    void testProcessLoadLpnLmMessageInvalidContractVersion() throws IOException {
        String messageBody = TestData.getValidJsonPayloadLoadLpnLm();
        when(elmTransactionBigQueryService.getExistingTraceIdsFromBqViewOrTable(anyString(),anyString(), anyString())).thenReturn(tableResult);
        Mockito.doNothing().when(elmTransactionBigQueryService).insertAll(ArgumentMatchers.anyList(), anyString());
        //Asserting unknown contract_version in pubsub message should throw an exception
        Assertions.assertThrows(ElmBusinessException.class,()->pubSubConsumerService.processPubSubToBq(messageBody, message, "2.0"));
        //Verifying that message was acknowledged only once
        Mockito.verify(message, Mockito.times(1)).ack();
    }

    @Test()
    @DisplayName("test- successful reading of 'PubSub Move From Lift To Lpn Location Lm' message by ignoring locationQty and vendor_number under location object and insert to Transactions table")
    void testProcessPubSubToBqWithSuccessMoveFromLiftToLpnLocationLm() throws IOException {
        String messageBody = TestData.getValidJsonPayloadMoveFromLiftToLpnLocationLm();
        when(elmTransactionBigQueryService.getExistingTraceIdsFromBqViewOrTable(anyString(),anyString(), anyString())).thenReturn(tableResult);
        Mockito.doNothing().when(elmTransactionBigQueryService).insertAll(ArgumentMatchers.anyList(), anyString());
        pubSubConsumerService.processPubSubToBq(messageBody, message, "");
        //Verifying that message was acknowledged only once
        Mockito.verify(message, Mockito.times(1)).ack();
    }

    @Test()
    @DisplayName("test- successful reading of 'PubSub Move To Lift Lm' message and insert to Transactions table")
    void testProcessPubSubToBqWithSuccessMoveToLiftLm() throws IOException {
        String messageBody = TestData.getValidJsonPayloadMoveToLiftLm();
        when(elmTransactionBigQueryService.getExistingTraceIdsFromBqViewOrTable(anyString(),anyString(), anyString())).thenReturn(tableResult);
        Mockito.doNothing().when(elmTransactionBigQueryService).insertAll(ArgumentMatchers.anyList(), anyString());
        pubSubConsumerService.processPubSubToBq(messageBody, message, "");
        //Verifying that message was acknowledged only once
        Mockito.verify(message, Mockito.times(1)).ack();
    }

    @Test()
    @DisplayName("test- successful reading of 'PubSub Receive Lpn Lm' message with receiving_type field and inserted to Transactions table")
    void testProcessPubSubToBqWithSuccessReceiveLpnLm() throws IOException {
        String messageBody = TestData.getValidJsonPayloadReceiveLpnLm();
        when(elmTransactionBigQueryService.getExistingTraceIdsFromBqViewOrTable(anyString(),anyString(), anyString())).thenReturn(tableResult);
        Mockito.doNothing().when(elmTransactionBigQueryService).insertAll(ArgumentMatchers.anyList(), anyString());
        pubSubConsumerService.processPubSubToBq(messageBody, message, TestData.TEST_CONTRACT_VERSION);
        verify(elmTransactionBigQueryService).getExistingTraceIdsFromBqViewOrTable(traceIdCaptor.capture(),dcNumberCaptor.capture(), eventTypeCaptor.capture());
        //Verifying that message was acknowledged only once
        Mockito.verify(message, Mockito.times(1)).ack();
    }

    @Test()
    @DisplayName("test- Message with Valid JSON Data Successful publish to r2r-consumer Topic")
    void testProcessMessageWithOutPublishingException() throws IOException {
        String messageBody = TestData.getValidJsonPayloadPickLpnFromActive();
        when(elmTransactionBigQueryService.getExistingTraceIdsFromBqViewOrTable(anyString(),anyString(), anyString())).thenReturn(tableResult);
        Mockito.doNothing().when(elmTransactionBigQueryService).insertAll(ArgumentMatchers.anyList(), anyString());
        Mockito.doNothing().when(pubSubPublisherService).publishToTopic(anyString(), anyString(), anyString(), any());
        Assertions.assertDoesNotThrow(() -> pubSubConsumerService.processPubSubToBq(messageBody, message, ""));
        Mockito.verify(message, Mockito.times(1)).ack();
    }

    @Test()
    @DisplayName("test- Message with Invalid Json Data and publish to r2r-consumer Topic with transaction type CONSUMER_NACK")
    void testProcessMessageAndPublishMessageWithConsumerNack() throws IOException {
        String messageBody = TestData.getInvalidFieldsJsonPayload();
        when(elmTransactionBigQueryService.getExistingTraceIdsFromBqViewOrTable(anyString(),anyString(), anyString())).thenReturn(tableResult);
        Mockito.doNothing().when(pubSubPublisherService).publishToTopic(anyString(), anyString(), anyString(), any());
        ElmBusinessException elmBusinessException = Assertions.assertThrows(ElmBusinessException.class, () -> pubSubConsumerService.processPubSubToBq(messageBody, message, ""));
        //Asserting that Error message is properly populated
        Assertions.assertTrue(elmBusinessException.getMessage().contains(ErrorMessages.JSON_READ_FAILED));
    }
    @Test()
    @DisplayName("test- Message with Valid JSON but Duplicate TraceId")
    void testProcessMessageWithoutDuplicateTraceId() throws IOException {
        List<FieldValueList> listOfFieldValueList = List.of(fieldValueList);
        String messageBody = TestData.getValidJsonPayloadPickLpnFromActive();
        when(elmTransactionBigQueryService.getExistingTraceIdsFromBqViewOrTable(anyString(),anyString(), anyString())).thenReturn(tableResult);
        when(tableResult.iterateAll()).thenReturn(listOfFieldValueList);
        when(fieldValueList.get(anyString())).thenReturn(fieldValue);
        when(fieldValue.getStringValue()).thenReturn(TestData.TEST_TRACE_ID);
        Assertions.assertDoesNotThrow(() -> pubSubConsumerService.processPubSubToBq(messageBody, message, ""));
        Mockito.verify(message, Mockito.times(1)).ack();
        //Asserting that if trace_id is duplicate then bigquery insertion will not happen
        Mockito.verify(elmTransactionBigQueryService, Mockito.times(0)).insertAll(any(), anyString());
        verify(elmTransactionBigQueryService).getExistingTraceIdsFromBqViewOrTable(traceIdCaptor.capture(),dcNumberCaptor.capture(), eventTypeCaptor.capture());
        //Asserting that actual arguments passed while query ELMEventsView
        Assertions.assertEquals(TestUtils.getJsonFieldValue(messageBody, TestUtils.TRACE_ID_PATH), traceIdCaptor.getValue());
        Assertions.assertEquals(TestUtils.getJsonFieldValue(messageBody, TestUtils.DC_NUMBER_PATH),dcNumberCaptor.getValue());
    }

    @Test()
    @DisplayName("test- successful reading of PubSub cico events and insert to Transactions table")
    void testProcessPubSubToBqWithSuccessCicoEventsWhenTraceIdDuplicate() throws IOException {
        List<FieldValueList> listOfFieldValueList = List.of(fieldValueList);
        String messageBody = TestData.getValidJsonPayloadCicoEvent();
        when(elmTransactionBigQueryService.getExistingTraceIdsFromBqViewOrTable(anyString(),anyString(), anyString())).thenReturn(tableResult);
        when(tableResult.iterateAll()).thenReturn(listOfFieldValueList);
        when(fieldValueList.get(anyString())).thenReturn(fieldValue);
        when(fieldValue.getStringValue()).thenReturn(TestData.TEST_TRACE_ID);
        Assertions.assertDoesNotThrow(() -> pubSubConsumerService.processPubSubToBq(messageBody, message, ""));
        Mockito.verify(message, Mockito.times(1)).ack();
        //Asserting that if trace_id is duplicate then bigquery insertion will not happen
        Mockito.verify(elmTransactionBigQueryService, Mockito.times(0)).insertAll(any(), anyString());
        verify(elmTransactionBigQueryService).getExistingTraceIdsFromBqViewOrTable(traceIdCaptor.capture(),dcNumberCaptor.capture(), eventTypeCaptor.capture());
        //Asserting that actual arguments passed while query ELMEventsView
        Assertions.assertEquals(TestUtils.getJsonFieldValue(messageBody, TestUtils.TRACE_ID_PATH), traceIdCaptor.getValue());
        Assertions.assertEquals(TestUtils.getJsonFieldValue(messageBody, TestUtils.DC_NUMBER_PATH),dcNumberCaptor.getValue());
    }

    @Test()
    @DisplayName("test- successful reading of PubSub cico events and insert to Transactions table")
    void testProcessPubSubToBqWithSuccessCicoEventsWhenTraceIdUnique() throws IOException {
        List<FieldValueList> listOfFieldValueList = List.of();
        String messageBody = TestData.getValidJsonPayloadCicoEvent();
        when(elmTransactionBigQueryService.getExistingTraceIdsFromBqViewOrTable(anyString(),anyString(), anyString())).thenReturn(tableResult);
        when(tableResult.iterateAll()).thenReturn(listOfFieldValueList);
        doNothing().when(cicoProcessorService).processCicoEvents(any(),any(),anyString());
        Assertions.assertDoesNotThrow(() -> pubSubConsumerService.processPubSubToBq(messageBody, message, ""));
        Mockito.verify(message, Mockito.times(1)).ack();
        //Asserting that if trace_id is duplicate then bigquery insertion will not happen
        Mockito.verify(elmTransactionBigQueryService, Mockito.times(0)).insertAll(any(), anyString());
        verify(elmTransactionBigQueryService).getExistingTraceIdsFromBqViewOrTable(traceIdCaptor.capture(),dcNumberCaptor.capture(), eventTypeCaptor.capture());
        //Asserting that actual arguments passed while query ELMEventsView
        Assertions.assertEquals(TestUtils.getJsonFieldValue(messageBody, TestUtils.TRACE_ID_PATH), traceIdCaptor.getValue());
        Assertions.assertEquals(TestUtils.getJsonFieldValue(messageBody, TestUtils.DC_NUMBER_PATH),dcNumberCaptor.getValue());
    }

    @Test()
    @DisplayName("test- Message with package hierarchy Missing UOM valid data")
    void testProcessPubSubToBqWithValidPackageHierarchies() throws IOException {
        String messageBody = TestData.getMoveFromLiftToLpnLocationLmMissingUom();
        when(elmTransactionBigQueryService.getExistingTraceIdsFromBqViewOrTable(anyString(),anyString(), anyString())).thenReturn(tableResult);
        Assertions.assertDoesNotThrow(() -> pubSubConsumerService.processPubSubToBq(messageBody, message, ""));
        //Verifying that message was acknowledged only once
        Mockito.verify(message, Mockito.times(1)).ack();
    }

    @Test()
    @DisplayName("test- Message with valid Picking Transaction")
    void testProcessPubSubToBqWithMissingUomInPickTransaction() throws IOException {
        String messageBody = TestData.getMissingUOMJsonPayloadPickLpnFromActive();
        when(elmTransactionBigQueryService.getExistingTraceIdsFromBqViewOrTable(anyString(),anyString(), anyString())).thenReturn(tableResult);
        ElmBusinessException elmBusinessException = Assertions.assertThrows(ElmBusinessException.class, () -> pubSubConsumerService.processPubSubToBq(messageBody, message, ""));
        //Asserting Error message is properly populated
        Assertions.assertEquals(ErrorMessages.ROW_MAPPER_PROCESSING_FAILED, elmBusinessException.getMessage());
        //Verifying that message was acknowledged only once
        Mockito.verify(message, Mockito.times(1)).ack();
    }

    @Test()
    @DisplayName("test- Message with Missing Location UOM and float package_each_quantity")
    void testProcessPubSubToBqWithFloatPackageEachQuantity() throws IOException {
        String messageBody = TestData.getMoveToListLmMissingUom();
        when(elmTransactionBigQueryService.getExistingTraceIdsFromBqViewOrTable(anyString(),anyString(), anyString())).thenReturn(tableResult);
        Assertions.assertDoesNotThrow(() -> pubSubConsumerService.processPubSubToBq(messageBody, message, ""));
        //Verifying that message was acknowledged only once
        Mockito.verify(message, Mockito.times(1)).ack();
    }
}