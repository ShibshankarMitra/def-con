package com.homedepot.supplychain.enterpriselabormanagement.services;

import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutureCallback;
import com.google.api.core.ApiFutures;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.ProjectTopicName;
import com.google.pubsub.v1.PubsubMessage;
import com.homedepot.supplychain.enterpriselabormanagement.annotations.NoIntercept;
import com.homedepot.supplychain.enterpriselabormanagement.constants.ErrorMessages;
import com.homedepot.supplychain.enterpriselabormanagement.exceptions.ElmBusinessException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;

import static com.homedepot.supplychain.enterpriselabormanagement.constants.CommonConstants.ELM_APP;

@Slf4j
@Service
public class PubSubPublisherService {

    @NoIntercept
    public void publishToTopic(String messageBody, String projectId, String topicName) throws IOException {
        Publisher publisher = null;
        try {
            ProjectTopicName projectTopicName = ProjectTopicName.of(projectId, topicName);
            publisher = Publisher.newBuilder(projectTopicName).build();
            ByteString byteString = ByteString.copyFromUtf8(messageBody);
            PubsubMessage pubsubMessage = PubsubMessage.newBuilder().setData(byteString).build();
            ApiFuture<String> future = publisher.publish(pubsubMessage);
            ApiFutures.addCallback(
                    future,
                    new ApiFutureCallback<>() {
                        @Override
                        public void onFailure(Throwable throwable) {
                            throw new ElmBusinessException(ELM_APP, ErrorMessages.PUBLISH_FAILED, throwable);
                        }
                        @Override
                        public void onSuccess(String messageId) {
                            // Once published, returns server-assigned message ids (unique within the topic)
                            log.info("Message published successfully to 1SC run2run consumer ack topic: {}, Message_Id: {}, Message_Body: {}", topicName, messageId, messageBody);
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
