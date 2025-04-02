package com.homedepot.supplychain.enterpriselabormanagement.services;

import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;
import com.homedepot.osc.run2run.commons.ConsumerAckMessage;
import com.homedepot.osc.run2run.commons.domain.ConsumerEntity;
import com.homedepot.supplychain.enterpriselabormanagement.constants.CommonConstants;
import com.homedepot.supplychain.enterpriselabormanagement.constants.FullElmR2RMetadata;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

@Service
public class Run2RunService {
    public PubsubMessage createConsumerAck(final String traceId, final String dcNumber, final String entity) {
        ConsumerAckMessage consumerAckMessage = ConsumerAckMessage.builder()
                .consumer(FullElmR2RMetadata.CONSUMER)
                .producer(FullElmR2RMetadata.PRODUCER)
                .entity(entity)
                .locationId(dcNumber)
                .transactionTimestamp(Instant.now())
                .transactionId(UUID.randomUUID().toString())
                .entities(createConsumerAckEntityBody(traceId))
                .build();
        return PubsubMessage.newBuilder()
                .putAllAttributes(consumerAckMessage.getAttributes())
                .setData(ByteString.copyFromUtf8(consumerAckMessage.getData()))
                .build();
    }

    public PubsubMessage createConsumerNack(final String dcNumber, final String traceId, final String entity, final String errorMessage) {
        ConsumerAckMessage consumerAckMessage = ConsumerAckMessage.builder()
                .nack()
                .consumer(FullElmR2RMetadata.CONSUMER)
                .producer(FullElmR2RMetadata.PRODUCER)
                .entity(entity)
                .locationId(dcNumber)
                .transactionTimestamp(Instant.now())
                .transactionId(UUID.randomUUID().toString())
                .entities(createConsumerNackEntityBody(traceId, errorMessage))
                .build();

        return PubsubMessage.newBuilder()
                .putAllAttributes(consumerAckMessage.getAttributes())
                .setData(ByteString.copyFromUtf8(consumerAckMessage.getData()))
                .build();
    }

    private List<ConsumerEntity> createConsumerAckEntityBody(String traceId) {
        List<ConsumerEntity> entities = new ArrayList<>();
        entities.add(ConsumerEntity.builder()
                .id(traceId)
                .build());
        return entities;
    }

    private List<ConsumerEntity> createConsumerNackEntityBody(String traceId, String errorMessage) {
        Map<String, Object> nackData = new LinkedHashMap<>();
        nackData.put(CommonConstants.ERROR_KEY, errorMessage);
        List<ConsumerEntity> entities = new ArrayList<>();
        entities.add(ConsumerEntity.builder()
                .id(traceId)
                .nack(nackData)
                .build());
        return entities;
    }
}
