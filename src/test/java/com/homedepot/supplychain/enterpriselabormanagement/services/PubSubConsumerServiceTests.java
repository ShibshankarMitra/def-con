package com.homedepot.supplychain.enterpriselabormanagement.services;

import com.fasterxml.jackson.core.JsonParseException;
import com.google.cloud.bigquery.BigQueryException;
import com.google.cloud.spring.pubsub.support.BasicAcknowledgeablePubsubMessage;
import com.homedepot.supplychain.enterpriselabormanagement.constants.ErrorMessages;
import com.homedepot.supplychain.enterpriselabormanagement.exceptions.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;
import com.homedepot.supplychain.enterpriselabormanagement.utils.TestData;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static com.homedepot.supplychain.enterpriselabormanagement.utils.TestData.TEST_CONSUMER_TOPIC_NAME;
import static com.homedepot.supplychain.enterpriselabormanagement.utils.TestData.TEST_PROJECT_ID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;


@ExtendWith(SpringExtension.class)
class PubSubConsumerServiceTests {
    @InjectMocks
    PubSubConsumerService pubSubConsumerService;

    @Mock
    PubSubPublisherService pubSubPublisherService;

    @Mock
    private ElmTransactionBigQueryService elmTransactionBigQueryService;
    @Mock
    private BasicAcknowledgeablePubsubMessage message;
    @Captor
    private ArgumentCaptor<List<Map<String, Object>>> rowsCaptor;

    @BeforeEach()
    public void setUp() {

    }

    @Test()
    @DisplayName("test- Message with Blank Body")
    void testProcessPubSubToBqWithBlankMessageIDAndBody() {
        String messageId = TestData.getMessageId();
        String messageBody = TestData.getEmptyPayload();
        //Asserting that ElmBusinessException is thrown
        ElmBusinessException elmBusinessException = Assertions.assertThrows(ElmBusinessException.class, () -> pubSubConsumerService.processPubSubToBq(messageId, messageBody, message));
        //Asserting that Error message is properly populated
        Assertions.assertTrue(elmBusinessException.getMessage().contains(ErrorMessages.JSON_READ_FAILED));
        //Asserting that the reason of exception is JsonValidationException
        Assertions.assertEquals(elmBusinessException.getCause().getClass(), JsonValidationException.class);
        Assertions.assertTrue(elmBusinessException.getCause().getMessage().contains(ErrorMessages.MESSAGE_BODY_BLANK));
        //Verifying that message was acknowledged only once
        Mockito.verify(message, Mockito.times(1)).ack();
    }

    @Test()
    @DisplayName("test- Message with Invalid JSON Syntax e.g. text or xml")
    void testProcessPubSubToBqWithJsonParseException() {
        String messageId = TestData.getMessageId();
        String messageBody = TestData.getTextPayload();
        //Asserting that elmBusinessException is thrown in this case as It is a known Business Exception regarding JSON Parsing and should be handled by code
        ElmBusinessException elmBusinessException = Assertions.assertThrows(ElmBusinessException.class, () -> pubSubConsumerService.processPubSubToBq(messageId, messageBody, message));
        //Asserting that Error message is properly populated
        Assertions.assertTrue(elmBusinessException.getMessage().contains(ErrorMessages.JSON_READ_FAILED));
        //Asserting that the reason of exception is JsonParseException
        Assertions.assertEquals(elmBusinessException.getCause().getClass(), JsonParseException.class);
        //Asserting that Exception has valid data
        Assertions.assertNotNull(elmBusinessException.getMessageId());
        Assertions.assertNotNull(elmBusinessException.getMessage());
        Assertions.assertNotNull(elmBusinessException.getCause());
        //Verifying that message was acknowledged only once
        Mockito.verify(message, Mockito.times(1)).ack();
    }

    @Test()
    @DisplayName("test- Message with Invalid JSON data/ schema")
    void testProcessPubSubToBqWithJsonValidationException() throws IOException {
        String messageId = TestData.getMessageId();
        String messageBody = TestData.getInvalidFieldsJsonPayload();
        //Asserting that elmBusinessException is thrown in this case as It is a known Business Exception regarding JSON Parsing and should be handled by code
        ElmBusinessException elmBusinessException = Assertions.assertThrows(ElmBusinessException.class, () -> pubSubConsumerService.processPubSubToBq(messageId, messageBody, message));
        //Asserting that Error message is properly populated
        Assertions.assertTrue(elmBusinessException.getMessage().contains(ErrorMessages.JSON_READ_FAILED));
        //Asserting that the reason of exception is JsonValidationException
        Assertions.assertEquals(elmBusinessException.getCause().getClass(), JsonValidationException.class);
        Assertions.assertTrue(elmBusinessException.getCause().getMessage().contains(ErrorMessages.JSON_SCHEMA_VALIDATION_FAILED));
        //Asserting that Exception has valid data
        Assertions.assertNotNull(elmBusinessException.getMessageId());
        Assertions.assertNotNull(elmBusinessException.getMessage());
        Assertions.assertNotNull(elmBusinessException.getCause());
        //Verifying that message was acknowledged only once
        Mockito.verify(message, Mockito.times(1)).ack();
    }

    @Test()
    @DisplayName("test- Message with Invalid event_type in JSON")
    void testProcessPubSubToBqWithInvalidEventType() throws IOException {
        String messageId = TestData.getMessageId();
        String messageBody = TestData.getInvalidEventTypeJsonPayload();
        //Asserting that elmBusinessException is thrown in this case as It is a known Business Exception regarding JSON Parsing and should be handled by code
        ElmBusinessException elmBusinessException = Assertions.assertThrows(ElmBusinessException.class, () -> pubSubConsumerService.processPubSubToBq(messageId, messageBody, message));
        //Asserting that Error message is properly populated
        Assertions.assertTrue(elmBusinessException.getMessage().contains(ErrorMessages.JSON_READ_FAILED));
        //Asserting that the reason of exception is JsonValidationException
        Assertions.assertEquals(elmBusinessException.getCause().getClass(), IllegalStateException.class);
    }

    @Test()
    @DisplayName("test- Message with Invalid Build data")
    void testProcessPubSubToBqWithInvalidBuilds() throws IOException {
        String messageId = TestData.getMessageId();
        String messageBody = TestData.getInvalidBuildsJsonPayload();
        ElmBusinessException elmBusinessException = Assertions.assertThrows(ElmBusinessException.class, () -> pubSubConsumerService.processPubSubToBq(messageId, messageBody, message));
        //Verifying the proper exception details were caught
        Assertions.assertSame(elmBusinessException.getCause().getClass(), DataProcessingException.class);
        //Verifying that message was acknowledged only once
        Mockito.verify(message, Mockito.times(1)).ack();
    }

    @Test()
    @DisplayName("test- Message with duplicate Build data")
    void testProcessPubSubToBqWithDuplicateBuilds() throws IOException {
        String messageId = TestData.getMessageId();
        String messageBody = TestData.getDuplicateBuildsJsonPayload();
        ElmBusinessException elmBusinessException = Assertions.assertThrows(ElmBusinessException.class, () -> pubSubConsumerService.processPubSubToBq(messageId, messageBody, message));
        //Verifying the proper exception details were caught
        Assertions.assertSame(elmBusinessException.getCause().getClass(), DataProcessingException.class);
        //Verifying that message was acknowledged only once
        Mockito.verify(message, Mockito.times(1)).ack();
    }

    @Test()
    @DisplayName("test- Message with invalid package hierarchy data")
    void testProcessPubSubToBqWithInvalidPackageHierarchies() throws IOException {
        String messageId = TestData.getMessageId();
        String messageBody = TestData.getInvalidPackageHierarchyJsonPayload();
        ElmBusinessException elmBusinessException = Assertions.assertThrows(ElmBusinessException.class, () -> pubSubConsumerService.processPubSubToBq(messageId, messageBody, message));
        //Verifying the proper exception details were caught
        Assertions.assertSame(elmBusinessException.getCause().getClass(), DataProcessingException.class);
        //Verifying that message was acknowledged only once
        Mockito.verify(message, Mockito.times(1)).ack();
    }

    @Test()
    @DisplayName("test- Message with Duplicate package hierarchy data")
    void testProcessPubSubToBqWithDuplicatePackageHierarchies() throws IOException {
        String messageId = TestData.getMessageId();
        String messageBody = TestData.getDuplicatePackageHierarchyJsonPayload();
        ElmBusinessException elmBusinessException = Assertions.assertThrows(ElmBusinessException.class, () -> pubSubConsumerService.processPubSubToBq(messageId, messageBody, message));
        //Verifying the proper exception details were caught
        Assertions.assertSame(elmBusinessException.getCause().getClass(), DataProcessingException.class);
        //Verifying that message was acknowledged only once
        Mockito.verify(message, Mockito.times(1)).ack();
    }

    @Test()
    @DisplayName("test- successful reading of PubSub pick lpn from active message and insert to Transactions table")
    void testProcessPubSubToBqWithSuccessPickLpnFromActive() throws IOException {
        String messageId = TestData.getMessageId();
        String messageBody = TestData.getValidJsonPayloadPickLpnFromActive();
        Mockito.doNothing().when(elmTransactionBigQueryService).insertAll(ArgumentMatchers.anyList());
        pubSubConsumerService.processPubSubToBq(messageId, messageBody, message);
        //Verifying that message was acknowledged only once
        Mockito.verify(message, Mockito.times(1)).ack();
    }

    @Test()
    @DisplayName("test- successful reading of PubSub replen allocation message and insert to Transactions table")
    void testProcessPubSubToBqWithSuccessReplenishment() throws IOException {
        String messageId = TestData.getMessageId();
        String messageBody = TestData.getValidJsonPayloadReplenAllocation();
        Mockito.doNothing().when(elmTransactionBigQueryService).insertAll(ArgumentMatchers.anyList());
        pubSubConsumerService.processPubSubToBq(messageId, messageBody, message);
        //Verifying that message was acknowledged only once
        Mockito.verify(message, Mockito.times(1)).ack();
    }

    @Test()
    @DisplayName("test- successful reading of PubSub message and insert to Transactions table for Indirect events")
    void testProcessPubSubToBqWithSuccessIndirectEvents() throws IOException {
        String messageId = TestData.getMessageId();
        String messageBody = TestData.getValidJsonPayloadIndirect();
        Mockito.doNothing().when(elmTransactionBigQueryService).insertAll(ArgumentMatchers.anyList());
        pubSubConsumerService.processPubSubToBq(messageId, messageBody, message);
        //Verifying that message was acknowledged only once
        Mockito.verify(message, Mockito.times(1)).ack();
    }

    @Test()
    @DisplayName("test- successful reading of PubSub message with Multiple build ids and insert to Transactions table")
    void testProcessPubSubToBqWithSuccessMultipleBuilds() throws IOException {
        String messageId = TestData.getMessageId();
        String messageBody = TestData.getValidJsonPayloadwWithMultipleBuilds();
        Mockito.doNothing().when(elmTransactionBigQueryService).insertAll(ArgumentMatchers.anyList());
        pubSubConsumerService.processPubSubToBq(messageId, messageBody, message);
        Mockito.verify(elmTransactionBigQueryService).insertAll(rowsCaptor.capture());
        //rowsCaptor
        List<Map<String, Object>> value = rowsCaptor.getValue();
        Assertions.assertEquals(5, value.size());
        //Verifying that message was acknowledged only once
        Mockito.verify(message, Mockito.times(1)).ack();
    }

    @Test()
    @DisplayName("test- BigQueryException while insertAll() to Transactions table")
    void testProcessPubSubToBqWithBigQueryException() throws IOException {
        String messageId = TestData.getMessageId();
        String messageBody = TestData.getValidJsonPayloadPickLpnFromActive();
        Mockito.doThrow(BigQueryException.class).when(elmTransactionBigQueryService).insertAll(ArgumentMatchers.anyList());
        //Asserting that ElmSystemException is thrown in this case as It is an HTTP system Exception
        Assertions.assertThrows(ElmSystemException.class, () -> pubSubConsumerService.processPubSubToBq(messageId, messageBody, message));
        //Verifying that message was never acknowledged only once
        Mockito.verify(message, Mockito.times(0)).ack();
    }

    @Test()
    @DisplayName("test- BigQueryResponseException while insertAll() to Transactions table")
    void testProcessPubSubToBqWithBigQueryResponseException() throws IOException {
        String messageId = TestData.getMessageId();
        String messageBody = TestData.getValidJsonPayloadPickLpnFromActive();
        Mockito.doThrow(BigQueryResponseException.class).when(elmTransactionBigQueryService).insertAll(ArgumentMatchers.anyList());
        //Asserting that ElmBusinessException is thrown in this case as it is known Business Exception regarding BQ Response
        ElmBusinessException elmBusinessException = Assertions.assertThrows(ElmBusinessException.class, () -> pubSubConsumerService.processPubSubToBq(messageId, messageBody, message));
        //Asserting that ElmBusinessException has valid Exception details along with BQ Table name
        Assertions.assertNotNull(elmBusinessException.getCause());
        //Verifying that message was acknowledged as this a business Exception
        Mockito.verify(message, Mockito.times(1)).ack();
    }

    @Test()
    @DisplayName("test- BigQueryException while insertAll() to Transactions table")
    void testProcessPubSubToBqWithRuntimeException() throws IOException {
        String messageId = TestData.getMessageId();
        String messageBody = TestData.getValidJsonPayloadPickLpnFromActive();
        Mockito.doThrow(RuntimeException.class).when(elmTransactionBigQueryService).insertAll(ArgumentMatchers.anyList());
        //Asserting that RuntimeException is thrown in this case as It is an HTTP system Exception
        Assertions.assertThrows(RuntimeException.class, () -> pubSubConsumerService.processPubSubToBq(messageId, messageBody, message));
        //Verifying that message was never acknowledged only once
        Mockito.verify(message, Mockito.times(0)).ack();
    }

    @Test()
    @DisplayName("test- Message with Valid JSON Data Failed to publish to r2r-consumer Topic")
    void testProcessMessageWithPublishingException() throws IOException {

        String messageId = TestData.getMessageId();
        String messageBody = TestData.getValidJsonPayloadPickLpnFromActive();
        Mockito.doNothing().when(elmTransactionBigQueryService).insertAll(ArgumentMatchers.anyList());
        ReflectionTestUtils.setField(pubSubConsumerService, "projectId", TEST_PROJECT_ID);
        ReflectionTestUtils.setField(pubSubConsumerService, "consumerAckTopicName", TEST_CONSUMER_TOPIC_NAME);
        Mockito.doThrow(RuntimeException.class).when(pubSubPublisherService).publishToTopic(anyString(), anyString(), anyString(), anyString());
        Assertions.assertThrows(ElmSystemException.class, () -> pubSubConsumerService.processPubSubToBq(messageId, messageBody, message));
        Mockito.verify(message, Mockito.times(1)).ack();

    }

    @Test()
    @DisplayName("test- Message with Valid JSON Data Successful publish to r2r-consumer Topic")
    void testProcessMessageWithOutPublishingException() throws IOException {
        String messageId = TestData.getMessageId();
        String messageBody = TestData.getValidJsonPayloadPickLpnFromActive();
        Mockito.doNothing().when(elmTransactionBigQueryService).insertAll(ArgumentMatchers.anyList());
        ReflectionTestUtils.setField(pubSubConsumerService, "projectId", TEST_PROJECT_ID);
        ReflectionTestUtils.setField(pubSubConsumerService, "consumerAckTopicName", TEST_CONSUMER_TOPIC_NAME);
        Mockito.doNothing().when(pubSubPublisherService).publishToTopic(anyString(), anyString(), anyString(), anyString());
        Assertions.assertDoesNotThrow(() -> pubSubConsumerService.processPubSubToBq(messageId, messageBody, message));
        Mockito.verify(message, Mockito.times(1)).ack();
    }

    @Test()
    @DisplayName("test- Message with Invalid Json Data and publish to r2r-consumer Topic with transaction type CONSUMER_NACK")
    void testProcessMessageAndPublishMessageWithConsumerNack() throws IOException {
        String messageId = TestData.getMessageId();
        String messageBody = TestData.getInvalidFieldsJsonPayload();
        ReflectionTestUtils.setField(pubSubConsumerService, "projectId", TEST_PROJECT_ID);
        ReflectionTestUtils.setField(pubSubConsumerService, "consumerAckTopicName", TEST_CONSUMER_TOPIC_NAME);
        Mockito.doNothing().when(pubSubPublisherService).publishToTopic(anyString(), anyString(), anyString(), anyString());
        ElmBusinessException elmBusinessException = Assertions.assertThrows(ElmBusinessException.class, () -> pubSubConsumerService.processPubSubToBq(messageId, messageBody, message));
        //Asserting that Error message is properly populated
        Assertions.assertTrue(elmBusinessException.getMessage().contains(ErrorMessages.JSON_READ_FAILED));
    }
}