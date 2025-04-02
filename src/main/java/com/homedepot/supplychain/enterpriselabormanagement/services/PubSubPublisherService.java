package com.homedepot.supplychain.enterpriselabormanagement.services;

import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutureCallback;
import com.google.api.core.ApiFutures;
import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.rpc.TransportChannelProvider;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.pubsub.v1.ProjectTopicName;
import com.google.pubsub.v1.PubsubMessage;
import com.homedepot.supplychain.enterpriselabormanagement.annotations.NoIntercept;
import com.homedepot.supplychain.enterpriselabormanagement.configs.MDCContextApiFutureCallback;
import com.homedepot.supplychain.enterpriselabormanagement.constants.ErrorMessages;
import com.homedepot.supplychain.enterpriselabormanagement.constants.FullElmR2RMetadata;
import com.homedepot.supplychain.enterpriselabormanagement.exceptions.ElmBusinessException;
import com.homedepot.supplychain.enterpriselabormanagement.exceptions.ElmSystemException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;

import static com.homedepot.supplychain.enterpriselabormanagement.constants.CommonConstants.ELM_APP;

@Slf4j
@Service
public class PubSubPublisherService {

    @Value("${spring.cloud.gcp.r2r.project-id}")
    private String projectId;
    @Value("${elm-r2r.gcp.pubsub.r2r-consumer-ack-topic-name}")
    private String consumerAckTopicName;
    @Value("${spring.cloud.gcp.r2r.project-entity}")
    private String entity;
    private final CredentialsProvider defaultCredentialsProvider;
    private final TransportChannelProvider defaultTransportChannelProvider;
    private final Run2RunService run2RunService;

    public PubSubPublisherService(CredentialsProvider defaultCredentialsProvider, @Qualifier("publisherTransportChannelProvider") TransportChannelProvider defaultTransportChannelProvider, Run2RunService run2RunService) {
        this.defaultCredentialsProvider = defaultCredentialsProvider;
        this.defaultTransportChannelProvider = defaultTransportChannelProvider;
        this.run2RunService = run2RunService;
    }

    @NoIntercept
    public void publishToTopic(String projectId, String topicName, String traceId, PubsubMessage pubsubMessage) throws IOException {
        Publisher publisher = null;
        try {
            ProjectTopicName projectTopicName = ProjectTopicName.of(projectId, topicName);
            publisher = Publisher.newBuilder(projectTopicName)
                    .setCredentialsProvider(defaultCredentialsProvider)
                    .setChannelProvider(defaultTransportChannelProvider)
                    .build();
            ApiFuture<String> future = publisher.publish(pubsubMessage);
            ApiFutures.addCallback(
                    future,
                    new MDCContextApiFutureCallback<>(new ApiFutureCallback<>() {
                        @Override
                        public void onFailure(Throwable throwable) {
                            throw new ElmBusinessException(String.format(ErrorMessages.PUBLISH_FAILED, topicName), throwable);
                        }
                        @Override
                        public void onSuccess(String messageId) {
                            // Once published, returns server-assigned message ids (unique within the topic)
                            log.info("Message published successfully to topic: {}, Message_Id: {}, Trace_Id: {}, Message_Headers: {} ", topicName, messageId, traceId, pubsubMessage.getAttributesMap());
                        }
                    }),
                    MoreExecutors.directExecutor());
        } finally {
            if (publisher != null) {
                // When finished with the publisher, shutdown to free up resources
                publisher.shutdown();
            }
        }
    }

    @NoIntercept
    public void createAndPublishConsumerAckMessage(String dcNumber, String traceId) throws ElmSystemException {
        try {
            PubsubMessage consumerAck = run2RunService.createConsumerAck(traceId, dcNumber, entity);
            publishToTopic(projectId,consumerAckTopicName,traceId,consumerAck);
        }catch (Exception exception) {
            throw new ElmSystemException(ELM_APP, String.format("Failed to publish message to 1SC run2run consumer ack topic for messageType %s", FullElmR2RMetadata.TRANSACTION_TYPE_CONSUMER_ACK), exception);
        }
    }

    @NoIntercept
    public void createAndPublishConsumerNackMessage(String dcNumber, String traceId, String errorMessage) throws ElmSystemException {
        try {
            PubsubMessage consumerAck = run2RunService.createConsumerNack(dcNumber,traceId, entity, errorMessage);
            publishToTopic(projectId,consumerAckTopicName,traceId,consumerAck);
        }catch (Exception exception) {
            throw new ElmSystemException(ELM_APP, String.format("Failed to publish message to 1SC run2run consumer ack topic for messageType %s", FullElmR2RMetadata.TRANSACTION_TYPE_CONSUMER_NACK), exception);
        }
    }
}
