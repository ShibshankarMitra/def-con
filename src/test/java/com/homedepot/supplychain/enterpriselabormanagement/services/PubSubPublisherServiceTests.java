package com.homedepot.supplychain.enterpriselabormanagement.services;

import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutures;
import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.rpc.TransportChannelProvider;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.pubsub.v1.ProjectTopicName;
import com.google.pubsub.v1.PubsubMessage;
import com.homedepot.supplychain.enterpriselabormanagement.exceptions.ElmSystemException;
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
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;

import static com.homedepot.supplychain.enterpriselabormanagement.utils.TestData.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


@ExtendWith(SpringExtension.class)
class PubSubPublisherServiceTests {

    @InjectMocks
    private PubSubPublisherService pubSubPublisherService;
    @MockBean
    TransportChannelProvider defaultTransportChannelProvider;
    @MockBean
    CredentialsProvider defaultCredentialsProvider;
    @Mock
    private Publisher.Builder mockPublisherBuilder;
    @Mock
    private PubsubMessage pubsubMessage;
    @Mock
    private Run2RunService run2RunService;
    @Mock
    private ApiFuture<String> apiFuture;
    MockedStatic<ProjectTopicName> mockStaticProjectTopic;
    MockedStatic<Publisher> mockStaticPublisher;

    @BeforeEach
    public void beforeEach() {
        ReflectionTestUtils.setField(pubSubPublisherService, "defaultTransportChannelProvider", defaultTransportChannelProvider);
        ReflectionTestUtils.setField(pubSubPublisherService, "defaultCredentialsProvider", defaultCredentialsProvider);
        mockStaticProjectTopic = Mockito.mockStatic(ProjectTopicName.class);
        mockStaticPublisher = Mockito.mockStatic(Publisher.class);
    }

    @Test
    void testPublishToTopicSuccessful() throws IOException {
        // Mock dependencies (replace with mocks for Publisher and ApiFuture)
        ProjectTopicName  projectTopicName = ProjectTopicName.of(TEST_PROJECT_ID, TestData.TEST_CONSUMER_TOPIC_NAME);
        when(ProjectTopicName.of(TEST_PROJECT_ID, TestData.TEST_CONSUMER_TOPIC_NAME)).thenReturn(projectTopicName);
        Publisher mockPublisher = mock(Publisher.class);
        when(mockPublisherBuilder.build()).thenReturn(mockPublisher);
        when(mockPublisherBuilder.setChannelProvider(any(TransportChannelProvider.class))).thenReturn(mockPublisherBuilder);
        when(mockPublisherBuilder.setCredentialsProvider(any(CredentialsProvider.class))).thenReturn(mockPublisherBuilder);
        when(Publisher.newBuilder(projectTopicName)).thenReturn(mockPublisherBuilder);
        ApiFuture<String> myFuture = ApiFutures.immediateFuture("test message id");
        when(mockPublisher.publish(pubsubMessage)).thenReturn(myFuture);
        // Call the service method
        pubSubPublisherService.publishToTopic(TEST_PROJECT_ID, TestData.TEST_CONSUMER_TOPIC_NAME, TestData.TEST_TRACE_ID, pubsubMessage);
        // Verify logging (if applicable)
        verify(mockPublisher).shutdown();
    }

    @Test
    void testPublishToTopicFailure() throws IOException {
        // Arrange
        String projectId = "test-project";
        String topicName = "test-topic";
        ProjectTopicName projectTopicName = ProjectTopicName.of(projectId, topicName);
        ApiFuture<String> future = ApiFutures.immediateFailedFuture(new RuntimeException("Failed to publish message"));
        Publisher mockPublisher = mock(Publisher.class);
        when(mockPublisher.publish(pubsubMessage)).thenReturn(future);
        when(mockPublisherBuilder.build()).thenReturn(mockPublisher);
        when(mockPublisherBuilder.setChannelProvider(any(TransportChannelProvider.class))).thenReturn(mockPublisherBuilder);
        when(mockPublisherBuilder.setCredentialsProvider(any(CredentialsProvider.class))).thenReturn(mockPublisherBuilder);
        when(Publisher.newBuilder(projectTopicName)).thenReturn(mockPublisherBuilder);
        pubSubPublisherService.publishToTopic(TEST_PROJECT_ID, TestData.TEST_CONSUMER_TOPIC_NAME, TestData.TEST_TRACE_ID, pubsubMessage);
        verify(mockPublisher).shutdown();
    }

    @Test
    void testPublishToTopicWithNullPublisher() throws IOException {
        // Mock dependencies (replace with mocks for Publisher and ApiFuture)
        ProjectTopicName  projectTopicName = ProjectTopicName.of(TEST_PROJECT_ID, TestData.TEST_CONSUMER_TOPIC_NAME);
        when(ProjectTopicName.of(TEST_PROJECT_ID, TestData.TEST_CONSUMER_TOPIC_NAME)).thenReturn(projectTopicName);
        when(mockPublisherBuilder.build()).thenReturn(null);
        when(Publisher.newBuilder(projectTopicName)).thenReturn(mockPublisherBuilder);
        Assertions.assertThrows(NullPointerException.class,()->pubSubPublisherService.publishToTopic(TEST_PROJECT_ID, TestData.TEST_CONSUMER_TOPIC_NAME, TestData.TEST_TRACE_ID, pubsubMessage));
    }



    @Test
    void testCreateAndPublishConsumerAckMessageDoesNotThrowElmSystemException() throws IOException {
        ProjectTopicName  projectTopicName = ProjectTopicName.of(TEST_PROJECT_ID, TestData.TEST_CONSUMER_TOPIC_NAME);
        Publisher mockPublisher = mock(Publisher.class);
        when(run2RunService.createConsumerAck(anyString(),anyString(),anyString())).thenReturn(pubsubMessage);
        when(mockPublisherBuilder.build()).thenReturn(mockPublisher);
        when(mockPublisherBuilder.setChannelProvider(any(TransportChannelProvider.class))).thenReturn(mockPublisherBuilder);
        when(mockPublisherBuilder.setCredentialsProvider(any(CredentialsProvider.class))).thenReturn(mockPublisherBuilder);
        when(Publisher.newBuilder(projectTopicName)).thenReturn(mockPublisherBuilder);
        when(mockPublisher.publish(any())).thenReturn(apiFuture);
        Assertions.assertDoesNotThrow(() -> pubSubPublisherService.createAndPublishConsumerAckMessage(TEST_DC_NUMBER, TestData.TEST_TRACE_ID));
    }

    @Test
    void testCreateAndPublishConsumerAckMessageThrowElmSystemException() throws IOException {
        ProjectTopicName  projectTopicName = ProjectTopicName.of(TEST_PROJECT_ID, TestData.TEST_CONSUMER_TOPIC_NAME);
        Publisher mockPublisher = mock(Publisher.class);
        when(run2RunService.createConsumerAck(anyString(),anyString(),anyString())).thenReturn(pubsubMessage);
        when(mockPublisherBuilder.build()).thenReturn(mockPublisher);
        when(mockPublisherBuilder.setChannelProvider(any(TransportChannelProvider.class))).thenReturn(mockPublisherBuilder);
        when(mockPublisherBuilder.setCredentialsProvider(any(CredentialsProvider.class))).thenReturn(mockPublisherBuilder);
        when(Publisher.newBuilder(projectTopicName)).thenReturn(mockPublisherBuilder);
        when(mockPublisher.publish(any())).thenThrow(ElmSystemException.class);
        Assertions.assertThrows(ElmSystemException.class,() -> pubSubPublisherService.createAndPublishConsumerAckMessage(TEST_DC_NUMBER, TestData.TEST_TRACE_ID));
    }

    @Test
    void testCreateAndPublishConsumerNackMessageDoesNotThrowElmSystemException() throws IOException {
        ProjectTopicName  projectTopicName = ProjectTopicName.of(TEST_PROJECT_ID, TestData.TEST_CONSUMER_TOPIC_NAME);
        Publisher mockPublisher = mock(Publisher.class);
        when(run2RunService.createConsumerAck(anyString(),anyString(),anyString())).thenReturn(pubsubMessage);
        when(mockPublisherBuilder.build()).thenReturn(mockPublisher);
        when(mockPublisherBuilder.setChannelProvider(any(TransportChannelProvider.class))).thenReturn(mockPublisherBuilder);
        when(mockPublisherBuilder.setCredentialsProvider(any(CredentialsProvider.class))).thenReturn(mockPublisherBuilder);
        when(Publisher.newBuilder(projectTopicName)).thenReturn(mockPublisherBuilder);
        when(mockPublisher.publish(any())).thenReturn(apiFuture);
        Assertions.assertDoesNotThrow(() -> pubSubPublisherService.createAndPublishConsumerNackMessage(TEST_DC_NUMBER, TestData.TEST_TRACE_ID, TEST_ERROR_MESSAGE));
    }

    @Test
    void testCreateAndPublishConsumerNackMessageThrowElmSystemException() throws IOException {
        ProjectTopicName  projectTopicName = ProjectTopicName.of(TEST_PROJECT_ID, TestData.TEST_CONSUMER_TOPIC_NAME);
        when(run2RunService.createConsumerAck(anyString(),anyString(),anyString())).thenReturn(pubsubMessage);
        Publisher mockPublisher = mock(Publisher.class);
        when(mockPublisherBuilder.build()).thenReturn(mockPublisher);
        when(mockPublisherBuilder.setChannelProvider(any(TransportChannelProvider.class))).thenReturn(mockPublisherBuilder);
        when(mockPublisherBuilder.setCredentialsProvider(any(CredentialsProvider.class))).thenReturn(mockPublisherBuilder);
        when(Publisher.newBuilder(projectTopicName)).thenReturn(mockPublisherBuilder);
        when(mockPublisher.publish(any())).thenThrow(ElmSystemException.class);
        Assertions.assertThrows(ElmSystemException.class, () -> pubSubPublisherService.createAndPublishConsumerNackMessage(TEST_DC_NUMBER, TestData.TEST_TRACE_ID, TEST_ERROR_MESSAGE));
    }

    @AfterEach
    public void afterEach() {
        mockStaticProjectTopic.close();
        mockStaticPublisher.close();
    }
}