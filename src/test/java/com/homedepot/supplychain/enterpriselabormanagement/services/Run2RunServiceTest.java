package com.homedepot.supplychain.enterpriselabormanagement.services;

import com.google.pubsub.v1.PubsubMessage;
import com.homedepot.supplychain.enterpriselabormanagement.utils.TestData;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
class Run2RunServiceTest {

    @InjectMocks
    private Run2RunService run2RunService;

    @Test
    void testCreateConsumerAck() {
        PubsubMessage consumerAck = run2RunService.createConsumerAck(TestData.TEST_TRACE_ID, TestData.TEST_DC_NUMBER, TestData.TEST_ENTITY);
        //Asserting that consumerAck message object created
        Assertions.assertNotNull(consumerAck);
        Assertions.assertFalse(consumerAck.getAttributesMap().isEmpty());
    }

    @Test
    void testCreateConsumerNack() {
        PubsubMessage consumerNack = run2RunService.createConsumerNack(TestData.TEST_TRACE_ID, TestData.TEST_DC_NUMBER, TestData.TEST_ENTITY, TestData.TEST_ERROR_MESSAGE);
        //Asserting that consumerNack message object created
        Assertions.assertNotNull(consumerNack);
        Assertions.assertFalse(consumerNack.getAttributesMap().isEmpty());
    }
}