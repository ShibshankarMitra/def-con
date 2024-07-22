package com.homedepot.supplychain.enterpriselabormanagement;


import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutureCallback;
import com.google.api.core.ApiFutures;
import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.core.NoCredentialsProvider;
import com.google.api.gax.grpc.GrpcTransportChannel;
import com.google.api.gax.rpc.FixedTransportChannelProvider;
import com.google.api.gax.rpc.NotFoundException;
import com.google.api.gax.rpc.TransportChannelProvider;
import com.google.cloud.NoCredentials;
import com.google.cloud.bigquery.*;
import com.google.cloud.pubsub.v1.*;
import com.google.cloud.spring.pubsub.PubSubAdmin;
import com.google.cloud.spring.pubsub.support.BasicAcknowledgeablePubsubMessage;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.*;
import com.homedepot.supplychain.enterpriselabormanagement.aspects.ExceptionAspect;
import com.homedepot.supplychain.enterpriselabormanagement.exceptions.ElmBusinessException;
import com.homedepot.supplychain.enterpriselabormanagement.services.PubSubConsumerService;
import com.homedepot.supplychain.enterpriselabormanagement.utils.TestData;
import com.homedepot.supplychain.enterpriselabormanagement.utils.TestUtils;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONException;
import org.junit.jupiter.api.*;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.BigQueryEmulatorContainer;
import org.testcontainers.containers.PubSubEmulatorContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.inject.Inject;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
@Testcontainers
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {"spring.main.allow-bean-definition-overriding=true"})
public class EnterpriseLaborManagementApplicationTests {

    private static final Logger ExceptionLogger = (Logger) LoggerFactory.getLogger(ExceptionAspect.class);

    private static final String PROJECT_ID = "test-project";
    private static final String ELM_TOPIC_NAME = "elm_topic_test";
    private static final String ELM_SUBSCRIPTION_ID = "elm_subscription_test";
    private static final String R2R_TOPIC_NAME = "r2r_topic_test";
    private static final String R2R_SUBSCRIPTION_ID = "r2r_subscription_test";
    private static final String DATASET_ID = "elm_integration_test";
    private static final String TABLE_NAME = "elm_events_test";

    private ListAppender<ILoggingEvent> listAppender;

    @Container
    private static final BigQueryEmulatorContainer bigqueryContainer = new BigQueryEmulatorContainer("ghcr.io/goccy/bigquery-emulator:0.4.3");

    @Container
    private static final PubSubEmulatorContainer pubsubEmulator = new PubSubEmulatorContainer("gcr.io/google.com/cloudsdktool/cloud-sdk:317.0.0-emulators");

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.cloud.gcp.pubsub.emulator-host", pubsubEmulator::getEmulatorEndpoint);
        registry.add("spring.cloud.gcp.bigquery-host", bigqueryContainer::getEmulatorHttpEndpoint);
        registry.add("spring.cloud.gcp.project-id", bigqueryContainer::getProjectId);
        registry.add("spring.cloud.gcp.r2r.project-id", bigqueryContainer::getProjectId);
        registry.add("spring.cloud.gcp.bigquery.dataset-name", () -> DATASET_ID);
        registry.add("elm.gcp.bigquery.elm-transactions-table-name", () -> TABLE_NAME);
        registry.add("elm.gcp.pubsub.elm-transactions-subscription-name", () -> ELM_SUBSCRIPTION_ID);
        registry.add("elm-r2r.gcp.pubsub.r2r-consumer-ack-topic-name", () -> R2R_TOPIC_NAME);

    }

    @TestConfiguration
    static class TestContainersConfiguration {
        @Bean
        @Profile("test")
        @Primary
        public TransportChannelProvider channelProvider() {
            ManagedChannel channel =
                    ManagedChannelBuilder.forTarget("dns:///" + pubsubEmulator.getEmulatorEndpoint())
                            .usePlaintext()
                            .build();
            return FixedTransportChannelProvider.create(GrpcTransportChannel.create(channel));
        }

        @Bean
        @Profile("test")
        @Primary
        public TransportChannelProvider publisherTransportChannelProvider() {
            return channelProvider();
        }

        @Bean
        @Profile("test")
        @Primary
        public BigQuery bigquery() {
            return BigQueryOptions.newBuilder()
                    .setHost(bigqueryContainer.getEmulatorHttpEndpoint())
                    .setProjectId(bigqueryContainer.getProjectId())
                    .setCredentials(NoCredentials.getInstance())
                    .build().getService();
        }

        @Bean
        @Profile("test")
        @Primary
        CredentialsProvider googleCredentials() {
            return NoCredentialsProvider.create();
        }
    }

    @Inject
    @Qualifier("bigquery")
    private BigQuery bigquery;

    @Inject
    @Qualifier("channelProvider")
    TransportChannelProvider channelProvider;

    @Inject
    private PubSubConsumerService pubSubConsumerService;


    //Creating GCP Resources before running end-to-end test
    @BeforeEach
    public void setup() throws Exception {
        initializePubSub();
        initializeBigQuerySchema();
        initializeLoggerCapture();
    }

    private void initializeLoggerCapture() {
        listAppender = new ListAppender<>();
        listAppender.start();
        ExceptionLogger.addAppender(listAppender);
    }

    private void initializePubSub() throws IOException {
        TopicAdminClient topicAdminClient =
                TopicAdminClient.create(TopicAdminSettings.newBuilder()
                        .setCredentialsProvider(NoCredentialsProvider.create())
                        .setTransportChannelProvider(channelProvider)
                        .build());
        SubscriptionAdminClient subscriptionAdminClient =
                SubscriptionAdminClient.create(SubscriptionAdminSettings.newBuilder()
                        .setTransportChannelProvider(channelProvider)
                        .setCredentialsProvider(NoCredentialsProvider.create())
                        .build());
        PubSubAdmin admin = new PubSubAdmin(() -> PROJECT_ID, topicAdminClient, subscriptionAdminClient);
        try {
            admin.deleteTopic(ELM_TOPIC_NAME);
            log.info("Topic deleted with name {}", ELM_TOPIC_NAME);
            admin.deleteTopic(R2R_TOPIC_NAME);
            log.info("Topic deleted with name {}", R2R_TOPIC_NAME);
            admin.deleteSubscription(ELM_SUBSCRIPTION_ID);
            log.info("Subscription deleted with name {}", ELM_SUBSCRIPTION_ID);
            admin.deleteSubscription(R2R_SUBSCRIPTION_ID);
            log.info("Subscription deleted with name {}", R2R_SUBSCRIPTION_ID);
        } catch (NotFoundException e) {
            log.info("Topic or Subscription not found. Skipping delete {}", e.getMessage());
        }
        Topic elmTopic = admin.createTopic(ELM_TOPIC_NAME);
        log.info("Topic created with name {}", elmTopic.getName());
        Topic r2rTopic = admin.createTopic(R2R_TOPIC_NAME);
        log.info("Topic created with name {}", r2rTopic.getName());
        Subscription elmSubscription = admin.createSubscription(ELM_SUBSCRIPTION_ID, ELM_TOPIC_NAME);
        log.info("Subscription created with name {} Listening to topic {} ", elmSubscription.getName(), elmTopic.getName());
        Subscription r2rSubscription = admin.createSubscription(R2R_SUBSCRIPTION_ID, R2R_TOPIC_NAME);
        log.info("Subscription created with name {} Listening to topic {} ", r2rSubscription.getName(), r2rTopic.getName());
        admin.close();
    }


    private void clearBigQuerySchema() {
        bigquery.delete(TableId.of(DATASET_ID, TABLE_NAME));
        log.info("Table '{}' removed", TABLE_NAME);
        bigquery.delete(DatasetId.of(bigqueryContainer.getProjectId(), DATASET_ID));
        log.info("Dataset '{}' removed", DATASET_ID);
    }

    private void initializeBigQuerySchema() {
        var datasetInfo = DatasetInfo.newBuilder(DATASET_ID).build();
        bigquery.create(datasetInfo);
        log.info("Dataset '{}' created", DATASET_ID);
        var schema = TestUtils.getElmSchema();
        var tableId = TableId.of(DATASET_ID, TABLE_NAME);
        var tableDefinition = StandardTableDefinition.of(schema);
        var tableInfo = TableInfo.newBuilder(tableId, tableDefinition).build();
        Table table = bigquery.create(tableInfo);
        log.info("Table '{}' created", table.getTableId());
    }

    @AfterEach
    public void tearDown() {
        clearBigQuerySchema();
    }

    private void publishToTopic(String message) throws IOException {
        Publisher publisher = null;
        try {
            publisher = Publisher.newBuilder(TopicName.of(PROJECT_ID, ELM_TOPIC_NAME))
                    .setCredentialsProvider(NoCredentialsProvider.create())
                    .setChannelProvider(channelProvider)
                    .build();
            ApiFuture<String> future = publisher.publish(PubsubMessage.newBuilder().setData(ByteString.copyFromUtf8(message)).build());
            ApiFutures.addCallback(
                    future,
                    new ApiFutureCallback<>() {
                        @Override
                        public void onFailure(Throwable throwable) {
                            log.info(throwable.getMessage());
                        }

                        @Override
                        public void onSuccess(String messageId) {
                            log.info("Published message with ID: {}", messageId);
                        }
                    },
                    MoreExecutors.directExecutor());
        } finally {
            if (publisher != null) {
                publisher.shutdown();
            }
        }
    }

    private void listenToElmSubscription() {
        MessageReceiver receiver =
                (PubsubMessage pubsubMessage, AckReplyConsumer consumer) -> {
                    BasicAcknowledgeablePubsubMessage originalMessage = convertToBasicAckPubSubMessage(pubsubMessage, consumer);
                    String messageId = originalMessage.getPubsubMessage().getMessageId();
                    String messageBody = originalMessage.getPubsubMessage().getData().toStringUtf8();
                    log.info("Subscription: {} PubSub message received with ID: {} Payload: {}", ELM_SUBSCRIPTION_ID, messageId, messageBody);
                    try {
                        pubSubConsumerService.processPubSubToBq(messageId, messageBody, originalMessage);
                    } catch (Exception ignored) {
                        //This is an empty catch block as exceptions are handled using aspect
                    }
                };
        Subscriber subscriber = null;
        try {
            subscriber = Subscriber.newBuilder(ProjectSubscriptionName.of(PROJECT_ID, ELM_SUBSCRIPTION_ID), receiver)
                    .setChannelProvider(channelProvider)
                    .setCredentialsProvider(NoCredentialsProvider.create())
                    .build();
            subscriber.startAsync().awaitRunning();
            log.info("Listening to subscription {}", ELM_SUBSCRIPTION_ID);
            subscriber.awaitTerminated(5, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            subscriber.stopAsync();
        }
    }

    private void listenToR2RSubscription(String hdwMessage) {
        MessageReceiver receiver =
                (PubsubMessage pubsubMessage, AckReplyConsumer consumer) -> {
                    BasicAcknowledgeablePubsubMessage originalMessage = convertToBasicAckPubSubMessage(pubsubMessage, consumer);
                    String messageId = originalMessage.getPubsubMessage().getMessageId();
                    String messageBody = originalMessage.getPubsubMessage().getData().toStringUtf8();
                    log.info("Subscription: {} PubSub message received with ID: {} Payload: {}", R2R_SUBSCRIPTION_ID, messageId, messageBody);
                    try {
                        TestUtils.validateR2RJsonPayloadAgainstHdw(messageBody, hdwMessage);
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                };
        Subscriber subscriber = null;
        try {
            subscriber = Subscriber.newBuilder(ProjectSubscriptionName.of(PROJECT_ID, R2R_SUBSCRIPTION_ID), receiver)
                    .setChannelProvider(channelProvider)
                    .setCredentialsProvider(NoCredentialsProvider.create())
                    .build();
            subscriber.startAsync().awaitRunning();
            log.info("Listening to subscription {}", R2R_SUBSCRIPTION_ID);
            subscriber.awaitTerminated(5, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            subscriber.stopAsync();
        }
    }

    private BasicAcknowledgeablePubsubMessage convertToBasicAckPubSubMessage(PubsubMessage pubsubMessage, AckReplyConsumer consumer) {
        return new BasicAcknowledgeablePubsubMessage() {
            @Override
            public ProjectSubscriptionName getProjectSubscriptionName() {
                return null;
            }

            @Override
            public PubsubMessage getPubsubMessage() {
                return pubsubMessage;
            }

            @Override
            public CompletableFuture<Void> ack() {
                return CompletableFuture.runAsync(consumer::ack);
            }

            @Override
            public CompletableFuture<Void> nack() {
                return CompletableFuture.runAsync(consumer::nack);
            }
        };
    }

    private TableResult runQuery(String query) throws InterruptedException {
        QueryJobConfiguration queryConfig =
                QueryJobConfiguration.newBuilder(query)
                        .setUseLegacySql(false)
                        .build();
        return bigquery.query(queryConfig);
    }

    @Test
    public void testInvalidMessage() throws IOException, InterruptedException {
        publishToTopic("Hello World");
        listenToElmSubscription();
        List<ILoggingEvent> logsList = listAppender.list;
        String query = TestUtils.getSelectAllQuery(DATASET_ID, TABLE_NAME);
        TableResult tableResult = runQuery(query);
        Assertions.assertEquals(0, tableResult.getTotalRows());
        ILoggingEvent logEvent = logsList.get(0);
        Assertions.assertEquals(logEvent.getLevel(), Level.ERROR);
        Assertions.assertTrue(logEvent.getFormattedMessage().contains(ElmBusinessException.class.getSimpleName()));
        listAppender.stop();
    }

    @Test
    public void testValidMessagePickLpnFromActive() throws IOException, InterruptedException, JSONException {
        String hdwMessage = TestData.getValidJsonPayloadPickLpnFromActive();
        publishToTopic(hdwMessage);
        listenToElmSubscription();
        String query = TestUtils.getSelectAllQuery(DATASET_ID, TABLE_NAME);
        TableResult tableResult = runQuery(query);
        TestUtils.assertTableResultsAgainstJson(tableResult, hdwMessage);
        listenToR2RSubscription(hdwMessage);
    }

    @Test
    public void testValidMessageReplenishmentAllocation() throws IOException, InterruptedException, JSONException {
        String hdwMessage = TestData.getValidJsonPayloadReplenAllocation();
        publishToTopic(hdwMessage);
        listenToElmSubscription();
        String query = TestUtils.getSelectAllQuery(DATASET_ID, TABLE_NAME);
        TableResult tableResult = runQuery(query);
        TestUtils.assertTableResultsAgainstJson(tableResult, hdwMessage);
        listenToR2RSubscription(hdwMessage);
    }

    @Test
    public void testValidMessageComplex() throws IOException, InterruptedException, JSONException {
        String hdwMessage = TestData.getValidJsonPayloadwWithMultipleBuilds();
        publishToTopic(hdwMessage);
        listenToElmSubscription();
        String query = TestUtils.getSelectAllQuery(DATASET_ID, TABLE_NAME);
        TableResult tableResult = runQuery(query);
        TestUtils.assertTableResultsAgainstJson(tableResult, hdwMessage);
        listenToR2RSubscription(hdwMessage);
    }

    @Test
    public void testValidMessageIndirect() throws IOException, InterruptedException, JSONException {
        String hdwMessage = TestData.getValidJsonPayloadIndirect();
        publishToTopic(hdwMessage);
        listenToElmSubscription();
        String query = TestUtils.getSelectAllQuery(DATASET_ID, TABLE_NAME);
        TableResult tableResult = runQuery(query);
        TestUtils.assertTableResultsAgainstJson(tableResult, hdwMessage);
        listenToR2RSubscription(hdwMessage);
    }
}
