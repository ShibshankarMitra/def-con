package com.homedepot.supplychain.enterpriselabormanagement.configs;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.api.gax.batching.FlowControlSettings;
import com.google.cloud.spring.autoconfigure.pubsub.GcpPubSubAutoConfiguration;
import com.google.cloud.spring.core.GcpProjectIdProvider;
import com.google.cloud.spring.pubsub.core.PubSubConfiguration;
import com.google.cloud.spring.pubsub.core.subscriber.PubSubSubscriberTemplate;
import com.google.cloud.spring.pubsub.integration.AckMode;
import com.google.cloud.spring.pubsub.integration.inbound.PubSubInboundChannelAdapter;
import com.google.cloud.spring.pubsub.support.BasicAcknowledgeablePubsubMessage;
import com.google.cloud.spring.pubsub.support.DefaultSubscriberFactory;
import com.google.cloud.spring.pubsub.support.GcpPubSubHeaders;
import com.homedepot.supplychain.enterpriselabormanagement.constants.CommonConstants;
import com.homedepot.supplychain.enterpriselabormanagement.services.PubSubConsumerService;
import com.homedepot.supplychain.enterpriselabormanagement.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;

import static com.homedepot.supplychain.enterpriselabormanagement.constants.CommonConstants.*;

@Slf4j
@Configuration
@Import({GcpPubSubAutoConfiguration.class})
public class PubSubConfig {

    /**
     * This is the Custom @Configuration class for subscriber.
     */
    @Value("${spring.cloud.gcp.project-id}")
    private String projectId;

    @Value("${elm.gcp.pubsub.elm-transactions-subscription-name}")
    private String subscriptionName;
    @Value("${elm.gcp.pubsub.elm-cico-subscription-name}")
    private String cicoSubscriptionName;
    @Value("${elm.gcp.pubsub.subscriber.flow-control.max-outstanding-element-count}")
    private String maxOutstandingElementCount;

    @Value("${elm.gcp.pubsub.subscriber.flow-control.max-outstanding-request-bytes}")
    private String maxOutstandingRequestBytes;

    private final PubSubConsumerService pubSubConsumerService;

    public PubSubConfig(PubSubConsumerService pubSubConsumerService) {
        this.pubSubConsumerService = pubSubConsumerService;
    }

    /**
     * Creates a bean of type org.springframework.messaging.MessageChannel
     */
    @Bean
    public MessageChannel pubsubInputChannel() {
        return new DirectChannel();
    }

    @Bean
    public MessageChannel pubsubCicoInputChannel() {
        return new DirectChannel();
    }

    /**
     * @param pubSubConfiguration (Provided by GcpPubSubAutoConfiguration class)
     * @return customSubscriberFactory.
     * This SubscriberFactory will have Flow Control settings embedded
     */
    @Bean
    @Primary
    public DefaultSubscriberFactory customSubscriberFactory(PubSubConfiguration pubSubConfiguration) {
        GcpProjectIdProvider idProvider = () -> projectId;
        FlowControlSettings flowControlSettings =
                FlowControlSettings.newBuilder()
                        /*1,000 outstanding messages. Must be >0. It controls   the maximum number of messages
                        the subscriber receives before pausing the message stream.*/
                        .setMaxOutstandingElementCount(Long.parseLong(maxOutstandingElementCount))
                        /*100 MiB. Must be >0. It controls the maximum size of messages the subscriber
                        receives before pausing the message stream.*/
                        .setMaxOutstandingRequestBytes(Long.parseLong(maxOutstandingRequestBytes) * CommonConstants.BYTE_MULTIPLIER * CommonConstants.BYTE_MULTIPLIER)
                        .build();
        DefaultSubscriberFactory customSubscriberFactory = new DefaultSubscriberFactory(idProvider, pubSubConfiguration);
        customSubscriberFactory.setFlowControlSettings(flowControlSettings);
        return customSubscriberFactory;
    }

    /**
     * @return customPubSubSubscriberTemplate. This PubSubSubscriberTemplate will have customSubscriberFactory bean embedded
     */
    @Bean
    @Primary
    public PubSubSubscriberTemplate customPubSubSubscriberTemplate(@Qualifier("customSubscriberFactory") DefaultSubscriberFactory customSubscriberFactory) {
        return new PubSubSubscriberTemplate(customSubscriberFactory);
    }

    /**
     * @return adapter
     */
    @Bean
    public PubSubInboundChannelAdapter messageChannelAdapter(
            @Qualifier("pubsubInputChannel") MessageChannel inputChannel, @Qualifier("customPubSubSubscriberTemplate") PubSubSubscriberTemplate customPubSubSubscriberTemplate) {
        PubSubInboundChannelAdapter adapter = new PubSubInboundChannelAdapter(customPubSubSubscriberTemplate, subscriptionName);
        /* Setting the adapter output channel as message channel */
        adapter.setOutputChannel(inputChannel);
        /* Setting the adapter ack mode as manual */
        adapter.setAckMode(AckMode.MANUAL);
        return adapter;
    }

    @Bean
    public PubSubInboundChannelAdapter messageChannelCicoAdapter(
            @Qualifier("pubsubCicoInputChannel") MessageChannel inputChannel, @Qualifier("customPubSubSubscriberTemplate") PubSubSubscriberTemplate customPubSubSubscriberTemplate) {
        PubSubInboundChannelAdapter adapter = new PubSubInboundChannelAdapter(customPubSubSubscriberTemplate, cicoSubscriptionName);
        /* Setting the adapter output channel as message channel */
        adapter.setOutputChannel(inputChannel);
        /* Setting the adapter ack mode as manual */
        adapter.setAckMode(AckMode.MANUAL);
        return adapter;
    }

    @Bean
    @ServiceActivator(inputChannel = "pubsubInputChannel")
    public MessageHandler messageReceiverFullELM() {
        return messageReceiver();
    }

    @Bean
    @ServiceActivator(inputChannel = "pubsubCicoInputChannel")
    public MessageHandler messageReceiverCico() {
        return messageReceiver();
    }

    /**
     * Providing the implementation for MessageHandler.handleMessage()-> {}
     */
    public MessageHandler messageReceiver() {
        return message -> {
            BasicAcknowledgeablePubsubMessage originalMessage = message.getHeaders()
                    .get(GcpPubSubHeaders.ORIGINAL_MESSAGE, BasicAcknowledgeablePubsubMessage.class);
            if (!ObjectUtils.isEmpty(originalMessage)) {
                //Checked that the PubSub message is not Null
                String messageId = originalMessage.getPubsubMessage().getMessageId();
                String messageBody = originalMessage.getPubsubMessage().getData().toStringUtf8();
                try {
                    // Set MDC context
                    setMDC(originalMessage);
                    log.info("pubsubInputChannel: PubSub message received with ID: {} Payload: {}", messageId, messageBody);
                    String contractVersion = JsonUtils.readJsonNode(messageBody,
                            FILE_PATH_DELIMITER+ LABOR_EVENT_JSON_PATH +FILE_PATH_DELIMITER+CONTRACT_VERSION);
                    pubSubConsumerService.processPubSubToBq(messageBody, originalMessage, contractVersion);
                } catch (Exception ignored) {
                    //This is an empty catch block as exceptions are handled using aspect
                }finally {
                    //Clear MDC after completion
                    MDC.clear();
                }
            }
        };
    }

    public void setMDC(BasicAcknowledgeablePubsubMessage message) throws JsonProcessingException  {
        MDC.put("messageId", message.getPubsubMessage().getMessageId());
        MDC.put("traceId", JsonUtils.readJsonNode(message.getPubsubMessage().getData().toStringUtf8(),FILE_PATH_DELIMITER+ LABOR_EVENT_JSON_PATH +FILE_PATH_DELIMITER+TRACE_ID));
        MDC.put("transactionId", JsonUtils.readJsonNode(message.getPubsubMessage().getData().toStringUtf8(),FILE_PATH_DELIMITER+ LABOR_EVENT_DETAIL_JSON_PATH +FILE_PATH_DELIMITER+TRANSACTION_ID));
    }

}