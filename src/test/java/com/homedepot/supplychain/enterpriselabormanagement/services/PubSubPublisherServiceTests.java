package com.homedepot.supplychain.enterpriselabormanagement.services;

import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutures;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.ProjectTopicName;
import com.google.pubsub.v1.PubsubMessage;
import com.homedepot.supplychain.enterpriselabormanagement.utils.TestConstants;
import com.homedepot.supplychain.enterpriselabormanagement.utils.TestData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mock;


@ExtendWith(SpringExtension.class)
class PubSubPublisherServiceTests extends TestData {

    @InjectMocks
    private PubSubPublisherService pubSubPublisherService;
    @Mock
    private Publisher.Builder mockPublisherBuilder;
    MockedStatic<ProjectTopicName> mockStaticProjectTopic;
    MockedStatic<Publisher> mockStaticPublisher;
    @BeforeEach
    public void beforeEach() {
        mockStaticProjectTopic = Mockito.mockStatic(ProjectTopicName.class);
        mockStaticPublisher = Mockito.mockStatic(Publisher.class);
    }

    @Test
    void testPublishToTopicSuccessful() throws IOException {
        // Mock dependencies (replace with mocks for Publisher and ApiFuture)
        ProjectTopicName  projectTopicName = ProjectTopicName.of(TestConstants.TEST_PROJECT_ID,TestConstants.TEST_CONSUMER_TOPIC_NAME);
        when(ProjectTopicName.of(TestConstants.TEST_PROJECT_ID, TestConstants.TEST_CONSUMER_TOPIC_NAME)).thenReturn(projectTopicName);
        Publisher mockPublisher = mock(Publisher.class);
        when(mockPublisherBuilder.build()).thenReturn(mockPublisher);
        when(Publisher.newBuilder(projectTopicName)).thenReturn(mockPublisherBuilder);
        ApiFuture<String> myFuture = ApiFutures.immediateFuture("test message id");
        when(mockPublisher.publish(PubsubMessage.newBuilder().setData(ByteString.copyFromUtf8(TestConstants.MESSAGE_BODY)).build())).thenReturn(myFuture);
        // Call the service method
        pubSubPublisherService.publishToTopic(TestConstants.MESSAGE_BODY, TestConstants.TEST_PROJECT_ID,TestConstants.TEST_CONSUMER_TOPIC_NAME);
        // Verify logging (if applicable)
        verify(mockPublisher).shutdown();
    }

    @Test
    void testPublishToTopicFailure() throws IOException {
        // Arrange
        String messageBody = "Test message";
        String projectId = "test-project";
        String topicName = "test-topic";
        ProjectTopicName projectTopicName = ProjectTopicName.of(projectId, topicName);
        ByteString byteString = ByteString.copyFromUtf8(messageBody);
        PubsubMessage pubsubMessage = PubsubMessage.newBuilder().setData(byteString).build();
        ApiFuture<String> future = ApiFutures.immediateFailedFuture(new RuntimeException("Failed to publish message"));
        Publisher mockPublisher = mock(Publisher.class);
        when(mockPublisher.publish(pubsubMessage)).thenReturn(future);
        when(mockPublisherBuilder.build()).thenReturn(mockPublisher);
        when(Publisher.newBuilder(projectTopicName)).thenReturn(mockPublisherBuilder);
        PubSubPublisherService pubSubPublisherServiceObj = new PubSubPublisherService();
        pubSubPublisherServiceObj.publishToTopic(messageBody, projectId, topicName);
        verify(mockPublisher).shutdown();
    }

    @Test
    void testPublishToTopicWithNullPublisher() throws IOException {
        // Mock dependencies (replace with mocks for Publisher and ApiFuture)
        ProjectTopicName  projectTopicName = ProjectTopicName.of(TestConstants.TEST_PROJECT_ID,TestConstants.TEST_CONSUMER_TOPIC_NAME);
        when(ProjectTopicName.of(TestConstants.TEST_PROJECT_ID, TestConstants.TEST_CONSUMER_TOPIC_NAME)).thenReturn(projectTopicName);
        when(mockPublisherBuilder.build()).thenReturn(null);
        when(Publisher.newBuilder(projectTopicName)).thenReturn(mockPublisherBuilder);
        Assertions.assertThrows(NullPointerException.class,()->pubSubPublisherService.publishToTopic(TestConstants.MESSAGE_BODY, TestConstants.TEST_PROJECT_ID,TestConstants.TEST_CONSUMER_TOPIC_NAME));
    }

    @AfterEach
    public void afterEach() {
        mockStaticProjectTopic.close();
        mockStaticPublisher.close();
    }
}