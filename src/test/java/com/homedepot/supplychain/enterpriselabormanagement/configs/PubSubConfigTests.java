package com.homedepot.supplychain.enterpriselabormanagement.configs;

import com.google.cloud.spring.pubsub.core.PubSubConfiguration;
import com.google.cloud.spring.pubsub.core.subscriber.PubSubSubscriberTemplate;
import com.google.cloud.spring.pubsub.integration.AckMode;
import com.google.cloud.spring.pubsub.integration.inbound.PubSubInboundChannelAdapter;
import com.google.cloud.spring.pubsub.support.BasicAcknowledgeablePubsubMessage;
import com.google.cloud.spring.pubsub.support.DefaultSubscriberFactory;
import com.google.cloud.spring.pubsub.support.GcpPubSubHeaders;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;
import com.homedepot.supplychain.enterpriselabormanagement.constants.CommonConstants;
import com.homedepot.supplychain.enterpriselabormanagement.services.PubSubConsumerService;
import com.homedepot.supplychain.enterpriselabormanagement.utils.TestConstants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(SpringExtension.class)
class PubSubConfigTests {
    @InjectMocks
    PubSubConfig pubSubConfig;
    @Mock
    PubSubConsumerService pubSubConsumerService;
    @Mock
    PubSubConfiguration pubSubConfiguration;

    @Value(TestConstants.TEST_SUBSCRIPTION_NAME)
    private String subscriptionName;

    @Value(TestConstants.TEST_PROJECT_ID)
    private String projectId;

    @Value(TestConstants.TEST_FLOW_CONTROL_COUNT)
    private String maxOutstandingElementCount;

    @Value(TestConstants.TEST_FLOW_CONTROL_BYTES)
    private String maxOutstandingRequestBytes;
    @Captor
    ArgumentCaptor<String> messageIdCaptor;
    @Captor
    ArgumentCaptor<String> messageBodyCaptor;
    @Captor
    ArgumentCaptor<BasicAcknowledgeablePubsubMessage> originalMessageCaptor;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(pubSubConfig, "subscriptionName", subscriptionName);
        ReflectionTestUtils.setField(pubSubConfig, "projectId", projectId);
        ReflectionTestUtils.setField(pubSubConfig, "maxOutstandingElementCount", maxOutstandingElementCount);
        ReflectionTestUtils.setField(pubSubConfig, "maxOutstandingRequestBytes", maxOutstandingRequestBytes);
    }

    @Test
    void testPubSubInputChannel() {
        //Asserting that pubsubInputChannel bean is Not Null
        Assertions.assertDoesNotThrow(() -> pubSubConfig.pubsubInputChannel());
    }

    @Test
    void testCustomSubscriberFactory() {
        DefaultSubscriberFactory defaultSubscriberFactory = pubSubConfig.customSubscriberFactory(pubSubConfiguration);

        //Asserting that defaultSubscriberFactory bean has proper project id and flow control settings assigned
        Assertions.assertEquals(defaultSubscriberFactory.getProjectId(), projectId);
        Assertions.assertEquals(defaultSubscriberFactory.getFlowControlSettings(subscriptionName).getMaxOutstandingElementCount(), Long.parseLong(maxOutstandingElementCount));
        Assertions.assertEquals(defaultSubscriberFactory.getFlowControlSettings(subscriptionName).getMaxOutstandingRequestBytes(),
                Long.parseLong(maxOutstandingRequestBytes) * CommonConstants.BYTE_MULTIPLIER * CommonConstants.BYTE_MULTIPLIER);
    }

    @Test
    void testCustomPubSubSubscriberTemplate() {
        DefaultSubscriberFactory defaultSubscriberFactory = pubSubConfig.customSubscriberFactory(pubSubConfiguration);
        PubSubSubscriberTemplate pubSubSubscriberTemplate = pubSubConfig.customPubSubSubscriberTemplate(defaultSubscriberFactory);
        //Asserting that pubSubSubscriberTemplate has not null defaultSubscriberFactory successfully assigned
        Assertions.assertNotNull(pubSubSubscriberTemplate.getSubscriberFactory());
        Assertions.assertEquals(pubSubSubscriberTemplate.getSubscriberFactory(), defaultSubscriberFactory);
    }

    @Test
    void testMessageChannelAdapter() {
        MessageChannel messageChannel = pubSubConfig.pubsubInputChannel();
        DefaultSubscriberFactory customSubscriberFactory = pubSubConfig.customSubscriberFactory(pubSubConfiguration);
        PubSubSubscriberTemplate pubSubSubscriberTemplate = pubSubConfig.customPubSubSubscriberTemplate(customSubscriberFactory);
        PubSubInboundChannelAdapter pubSubInboundChannelAdapter = pubSubConfig.messageChannelAdapter(messageChannel, pubSubSubscriberTemplate);

        //Asserting that pubSubInboundChannelAdapter has the same output channel assigned as our Message Channel bean
        Assertions.assertEquals(pubSubInboundChannelAdapter.getOutputChannel(), messageChannel);
        //Asserting that acknowledgement mode is set to Manual
        Assertions.assertEquals(AckMode.MANUAL, pubSubInboundChannelAdapter.getAckMode());
    }

    @Test
    void testMessageReceiver() {
        BasicAcknowledgeablePubsubMessage basicAcknowledgeablePubsubMessage = Mockito.mock(BasicAcknowledgeablePubsubMessage.class);
        PubsubMessage pubsubMessage = Mockito.mock(PubsubMessage.class);
        Message<String> message = MessageBuilder.withPayload(TestConstants.TEST_PAYLOAD).setHeader(GcpPubSubHeaders.ORIGINAL_MESSAGE, basicAcknowledgeablePubsubMessage).build();
        Mockito.when(basicAcknowledgeablePubsubMessage.getPubsubMessage()).thenReturn(pubsubMessage);
        Mockito.when(pubsubMessage.getMessageId()).thenReturn(TestConstants.TEST_MESSAGE_ID);
        Mockito.when(pubsubMessage.getData()).thenReturn(ByteString.copyFromUtf8(TestConstants.TEST_MESSAGE_BODY));
        MessageHandler messageHandler = pubSubConfig.messageReceiver(pubSubConsumerService);
        messageHandler.handleMessage(message);
        //Verifying that pubSubConsumerService.processPubSubToBq() has been triggered at least once and only once per a message is received
        //Also capturing the messageId, messageBody and originalMessage arguments passed to the method
        Mockito.verify(pubSubConsumerService, Mockito.times(1)).processPubSubToBq(messageIdCaptor.capture(), messageBodyCaptor.capture(), originalMessageCaptor.capture());

        //Asserting that messageId, messageBody, and originalMessage sent to be processed is same as received from the headers
        Assertions.assertEquals(TestConstants.TEST_MESSAGE_ID, messageIdCaptor.getValue());
        Assertions.assertEquals(TestConstants.TEST_MESSAGE_BODY, messageBodyCaptor.getValue());
        Assertions.assertEquals(originalMessageCaptor.getValue(), basicAcknowledgeablePubsubMessage);
    }

    @Test
    void testMessageReceiverWithException() {
        BasicAcknowledgeablePubsubMessage basicAcknowledgeablePubsubMessage = Mockito.mock(BasicAcknowledgeablePubsubMessage.class);
        PubsubMessage pubsubMessage = Mockito.mock(PubsubMessage.class);
        Message<String> message = MessageBuilder.withPayload(TestConstants.TEST_PAYLOAD).setHeader(GcpPubSubHeaders.ORIGINAL_MESSAGE, basicAcknowledgeablePubsubMessage).build();
        Mockito.when(basicAcknowledgeablePubsubMessage.getPubsubMessage()).thenReturn(pubsubMessage);
        Mockito.when(pubsubMessage.getMessageId()).thenReturn(TestConstants.TEST_MESSAGE_ID);
        Mockito.when(pubsubMessage.getData()).thenReturn(ByteString.copyFromUtf8(TestConstants.TEST_MESSAGE_BODY_BLANK));
        Mockito.doThrow(RuntimeException.class).when(pubSubConsumerService).processPubSubToBq(TestConstants.TEST_MESSAGE_ID, TestConstants.TEST_MESSAGE_BODY_BLANK, basicAcknowledgeablePubsubMessage);
        MessageHandler messageHandler = pubSubConfig.messageReceiver(pubSubConsumerService);
        //Asserting that message handler will not throw an exception to the adapter, as we are handling the exceptions using aspects
        Assertions.assertDoesNotThrow(() -> messageHandler.handleMessage(message));
    }

    @Test
    void testMessageReceiverWithEmptyMessage() {
        Message<String> message = MessageBuilder.withPayload(TestConstants.TEST_PAYLOAD).setHeader(GcpPubSubHeaders.ORIGINAL_MESSAGE, null).build();
        MessageHandler messageHandler = pubSubConfig.messageReceiver(pubSubConsumerService);
        messageHandler.handleMessage(message);
        //Verifying that pubSubConsumerService.processPubSubToBq() has not  been triggered at all as the message received is Null
        Mockito.verify(pubSubConsumerService, Mockito.times(0)).processPubSubToBq(messageIdCaptor.capture(), messageBodyCaptor.capture(), originalMessageCaptor.capture());
    }
}