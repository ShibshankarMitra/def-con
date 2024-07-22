package com.homedepot.supplychain.enterpriselabormanagement;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutureCallback;
import com.google.api.core.ApiFutures;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.BigQueryEmulatorContainer;
import org.testcontainers.containers.PubSubEmulatorContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;


import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


@Slf4j
@Testcontainers
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class EnterpriseLaborManagementApplicationTests extends TestData {

    private static final Logger ExceptionLogger = (Logger) LoggerFactory.getLogger(ExceptionAspect.class);

    private static final String PROJECT_ID = "test-project";
    private static final String TOPIC_NAME = "elm_topic_test";
    private static final String SUBSCRIPTION_ID = "elm_subscription_test";
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
        registry.add("spring.cloud.gcp.project-id", bigqueryContainer::getProjectId);
        registry.add("spring.cloud.gcp.bigquery-host", bigqueryContainer::getEmulatorHttpEndpoint);
        registry.add("spring.cloud.gcp.bigquery.dataset-name", () -> DATASET_ID);
        registry.add("elm.gcp.bigquery.elm-transactions-table-name", () -> TABLE_NAME);
        registry.add("elm.gcp.pubsub.elm-transactions-subscription-name", () -> SUBSCRIPTION_ID);
    }

    @TestConfiguration
    static class TestContainersConfiguration {
        @Bean
        @Profile("test")
        public TransportChannelProvider channelProvider() {
            ManagedChannel channel =
                    ManagedChannelBuilder.forTarget("dns:///" + pubsubEmulator.getEmulatorEndpoint())
                            .usePlaintext()
                            .build();
            return FixedTransportChannelProvider.create(GrpcTransportChannel.create(channel));
        }

        @Bean
        @Profile("test")
        public BigQuery bigquery() {
            return BigQueryOptions.newBuilder()
                    .setHost(bigqueryContainer.getEmulatorHttpEndpoint())
                    .setProjectId(bigqueryContainer.getProjectId())
                    .setCredentials(NoCredentials.getInstance())
                    .build().getService();
        }
    }

    @Autowired
    @Qualifier("bigquery")
    private BigQuery bigquery;

    @Autowired
    @Qualifier("channelProvider")
    TransportChannelProvider channelProvider;

    @Autowired
    private PubSubConsumerService pubSubConsumerService;

    //Creating GCP Resources before running end-to-end test
    @BeforeEach
    public void setup() throws Exception {
        initializePubSub();
        clearBigQuerySchema();
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
            admin.deleteTopic(TOPIC_NAME);
            log.info("Topic deleted with name {}", TOPIC_NAME);
            admin.deleteSubscription(SUBSCRIPTION_ID);
            log.info("Subscription deleted with name {}", SUBSCRIPTION_ID);
        } catch (NotFoundException e) {
            log.info("Topic {} or Subscription {} does not exist. Skipping delete", TOPIC_NAME, SUBSCRIPTION_ID);
        }
        Topic topic = admin.createTopic(TOPIC_NAME);
        log.info("Topic created with name {}", topic.getName());
        Subscription subscription = admin.createSubscription(SUBSCRIPTION_ID, TOPIC_NAME);
        log.info("Subscription created with name {} Listening to topic {} ", subscription.getName(), topic.getName());
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
        var schema = super.getElmSchema();
        var tableId = TableId.of(DATASET_ID, TABLE_NAME);
        var tableDefinition = StandardTableDefinition.of(schema);
        var tableInfo = TableInfo.newBuilder(tableId, tableDefinition).build();
        Table table = bigquery.create(tableInfo);
        log.info("Table '{}' created", table.getTableId());
    }

    @AfterEach
    public void tearDown() {
    }

    private void publishToTopic(String message) throws IOException {
        Publisher publisher = null;
        try {
            publisher = Publisher.newBuilder(TopicName.of(PROJECT_ID, TOPIC_NAME))
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
                            log.info("Published message ID: {}", messageId);
                        }
                    },
                    MoreExecutors.directExecutor());
        } finally {
            if (publisher != null) {
                publisher.shutdown();
            }
        }
    }

    private void listenToSubscription() {
        MessageReceiver receiver =
                (PubsubMessage pubsubMessage, AckReplyConsumer consumer) -> {
                    BasicAcknowledgeablePubsubMessage originalMessage = convertToBasicAckPubSubMessage(pubsubMessage, consumer);
                    String messageId = originalMessage.getPubsubMessage().getMessageId();
                    String messageBody = originalMessage.getPubsubMessage().getData().toStringUtf8();
                    log.info("pubsubInputChannel: PubSub message received with ID: {} Payload: {}", messageId, messageBody);
                    try {
                        pubSubConsumerService.processPubSubToBq(messageId, messageBody, originalMessage);
                    } catch (Exception ignored) {
                        //This is an empty catch block as exceptions are handled using aspect
                    }
                };
        Subscriber subscriber = null;
        try {
            subscriber = Subscriber.newBuilder(ProjectSubscriptionName.of(PROJECT_ID, SUBSCRIPTION_ID), receiver)
                    .setChannelProvider(channelProvider)
                    .setCredentialsProvider(NoCredentialsProvider.create())
                    .build();
            subscriber.startAsync().awaitRunning();
            log.info("Listening for messages on {}", SUBSCRIPTION_ID);
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
        listenToSubscription();
        List<ILoggingEvent> logsList = listAppender.list;
        String query = super.getSelectAllQuery(DATASET_ID, TABLE_NAME);
        TableResult tableResult = runQuery(query);
        Assertions.assertEquals(0, tableResult.getTotalRows());
        ILoggingEvent logEvent = logsList.get(0);
        Assertions.assertEquals(logEvent.getLevel(), Level.ERROR);
        Assertions.assertTrue(logEvent.getFormattedMessage().contains(ElmBusinessException.class.getSimpleName()));
        listAppender.stop();
    }

    @Test
    public void testValidMessagePickLpnFromActive() throws IOException, InterruptedException, JSONException {
        String message = super.getValidJsonPayloadPickLpnFromActive();
        publishToTopic(message);
        listenToSubscription();
        String query = super.getSelectAllQuery(DATASET_ID, TABLE_NAME);
        TableResult tableResult = runQuery(query);
        TestUtils.assertTableResultsAgainstJson(tableResult, message);
    }

    @Test
    public void testValidMessageReplenishmentAllocation() throws IOException, InterruptedException, JSONException {
        String message = super.getValidJsonPayloadReplenAllocation();
        publishToTopic(message);
        listenToSubscription();
        String query = super.getSelectAllQuery(DATASET_ID, TABLE_NAME);
        TableResult tableResult = runQuery(query);
        TestUtils.assertTableResultsAgainstJson(tableResult, message);
    }

    @Test
    public void testValidMessageComplex() throws IOException, InterruptedException, JSONException {
        String message = super.getValidJsonPayloadwWithMultipleBuilds();
        publishToTopic(message);
        listenToSubscription();
        String query = super.getSelectAllQuery(DATASET_ID, TABLE_NAME);
        TableResult tableResult = runQuery(query);
        TestUtils.assertTableResultsAgainstJson(tableResult, message);
    }

    @Test
    public void testValidMessageIndirect() throws IOException, InterruptedException, JSONException {
        String message = super.getValidJsonPayloadIndirect();
        publishToTopic(message);
        listenToSubscription();
        String query = super.getSelectAllQuery(DATASET_ID, TABLE_NAME);
        TableResult tableResult = runQuery(query);
        TestUtils.assertTableResultsAgainstJson(tableResult, message);
    }
}

