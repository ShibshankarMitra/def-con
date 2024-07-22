package com.homedepot.supplychain.enterpriselabormanagement.services;

import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutureCallback;
import com.google.api.core.ApiFutures;
import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.rpc.TransportChannelProvider;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.ProjectTopicName;
import com.google.pubsub.v1.PubsubMessage;
import com.homedepot.supplychain.enterpriselabormanagement.annotations.NoIntercept;
import com.homedepot.supplychain.enterpriselabormanagement.constants.ErrorMessages;
import com.homedepot.supplychain.enterpriselabormanagement.exceptions.ElmBusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.io.IOException;

import static com.homedepot.supplychain.enterpriselabormanagement.constants.CommonConstants.ELM_APP;

@Slf4j
@Service
public class PubSubPublisherService {

    @Autowired
    CredentialsProvider defaultCredentialsProvider;

    @Autowired
    @Qualifier("publisherTransportChannelProvider")
    TransportChannelProvider defaultTransportChannelProvider;

    @NoIntercept
    public void publishToTopic(String messageBody, String projectId, String topicName, String messageType) throws IOException {
        Publisher publisher = null;
        try {
            ProjectTopicName projectTopicName = ProjectTopicName.of(projectId, topicName);
            publisher = Publisher.newBuilder(projectTopicName)
                    .setCredentialsProvider(defaultCredentialsProvider)
                    .setChannelProvider(defaultTransportChannelProvider)
                    .build();
            ByteString byteString = ByteString.copyFromUtf8(messageBody);
            PubsubMessage pubsubMessage = PubsubMessage.newBuilder().setData(byteString).build();
            ApiFuture<String> future = publisher.publish(pubsubMessage);
            ApiFutures.addCallback(
                    future,
                    new ApiFutureCallback<>() {
                        @Override
                        public void onFailure(Throwable throwable) {
                            throw new ElmBusinessException(ELM_APP, String.format(ErrorMessages.PUBLISH_FAILED, topicName), throwable);
                        }
                        @Override
                        public void onSuccess(String messageId) {
                            // Once published, returns server-assigned message ids (unique within the topic)
                            log.info("Message published successfully to topic: {}, Message_Id: {}, Transaction_Type: {}, Message_Body: {} ", topicName, messageId, messageType, messageBody);
                        }
                    },
                    MoreExecutors.directExecutor());
        } finally {
            if (publisher != null) {
                // When finished with the publisher, shutdown to free up resources
                publisher.shutdown();
            }
        }
    }
}
