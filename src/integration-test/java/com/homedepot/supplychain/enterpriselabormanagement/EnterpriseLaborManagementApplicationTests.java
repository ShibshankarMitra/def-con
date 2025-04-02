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
import com.homedepot.supplychain.enterpriselabormanagement.constants.CommonConstants;
import com.homedepot.supplychain.enterpriselabormanagement.constants.ElmTransactionBqHeaders;
import com.homedepot.supplychain.enterpriselabormanagement.exceptions.ElmSystemException;
import com.homedepot.supplychain.enterpriselabormanagement.services.PubSubConsumerService;
import com.homedepot.supplychain.enterpriselabormanagement.utils.TestData;
import com.homedepot.supplychain.enterpriselabormanagement.utils.TestUtils;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
    private static final Logger WarningLogger = (Logger) LoggerFactory.getLogger(PubSubConsumerService.class);

    private static final String PROJECT_ID = "test-project";
    private static final String ELM_TOPIC_NAME = "elm_topic_test";
    private static final String CICO_TOPIC_NAME = "cico_topic_test";
    private static final String ELM_SUBSCRIPTION_ID = "elm_subscription_test";
    private static final String CICO_SUBSCRIPTION_ID = "cico_subscription_test";
    private static final String R2R_TOPIC_NAME = "r2r_topic_test";
    private static final String R2R_SUBSCRIPTION_ID = "r2r_subscription_test";
    private static final String DATASET_ID = "elm_integration_test";
    private static final String TABLE_NAME = "elm_events_test";
    private static final String VIEW_NAME = "elm_events_view_test";

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
        registry.add("elm.gcp.bigquery.elm-view-name", () -> VIEW_NAME);
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

    /**
     * Creating GCP Resources before running end-to-end test
     * @NOTE: As of now BigQuery Test Container does not supported to create Materialized View that why in functional tests we've used Logical View only for tests.
     * In future if they add this feature then, We'll update it in the functional tests.
     */
    @BeforeEach
    public void setup() throws Exception {
        initializePubSub();
        initializeBigQuerySchema();
        initializeLogicalView();
        initializeLoggerCapture();
    }

    private void initializeLoggerCapture() {
        listAppender = new ListAppender<>();
        listAppender.start();
        ExceptionLogger.addAppender(listAppender);
        WarningLogger.addAppender(listAppender);
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
            admin.deleteTopic(CICO_TOPIC_NAME);
            log.info("Topic deleted with name {}", CICO_TOPIC_NAME);
            admin.deleteTopic(R2R_TOPIC_NAME);
            log.info("Topic deleted with name {}", R2R_TOPIC_NAME);
            admin.deleteSubscription(ELM_SUBSCRIPTION_ID);
            log.info("Subscription deleted with name {}", ELM_SUBSCRIPTION_ID);
            admin.deleteSubscription(CICO_SUBSCRIPTION_ID);
            log.info("Subscription deleted with name {}", CICO_SUBSCRIPTION_ID);
            admin.deleteSubscription(R2R_SUBSCRIPTION_ID);
            log.info("Subscription deleted with name {}", R2R_SUBSCRIPTION_ID);
        } catch (NotFoundException e) {
            log.info("Topic or Subscription not found. Skipping delete {}", e.getMessage());
        }
        Topic elmTopic = admin.createTopic(ELM_TOPIC_NAME);
        log.info("Topic created with name {}", elmTopic.getName());
        Topic cicoTopic = admin.createTopic(CICO_TOPIC_NAME);
        log.info("Topic created with name {}", cicoTopic.getName());
        Topic r2rTopic = admin.createTopic(R2R_TOPIC_NAME);
        log.info("Topic created with name {}", r2rTopic.getName());
        Subscription elmSubscription = admin.createSubscription(ELM_SUBSCRIPTION_ID, ELM_TOPIC_NAME);
        log.info("Subscription created with name {} Listening to topic {} ", elmSubscription.getName(), elmTopic.getName());
        Subscription cicoSubscription = admin.createSubscription(CICO_SUBSCRIPTION_ID, CICO_TOPIC_NAME);
        log.info("Subscription created with name {} Listening to topic {} ", cicoSubscription.getName(), cicoTopic.getName());
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

    private void clearLogicalView() {
        try {
            bigquery.query(QueryJobConfiguration.newBuilder("DROP VIEW " + DATASET_ID + "." + VIEW_NAME).build());
            log.info("View {} dropped", VIEW_NAME);
        } catch (InterruptedException | BigQueryException e) {
            log.info("Error while deleting view: {}", e.toString());
            Thread.currentThread().interrupt();
        }
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

    /**
     * Bigquery Emulator Test Container has a limitation that it does not support Materialized View, that why we used Logical View in functional test.
     */
    private void initializeLogicalView() {
        try {
            String query = String.format("SELECT trace_id, partition_date, dc_number FROM %s.%s.%s", PROJECT_ID,DATASET_ID, TABLE_NAME);
            var tableId = TableId.of(PROJECT_ID, DATASET_ID, VIEW_NAME);
            ViewDefinition viewDefinition = ViewDefinition.newBuilder(query).setUseLegacySql(false).build();
            Table table = bigquery.create(TableInfo.of(tableId, viewDefinition));
            log.info("Logical View '{}' created", table.getTableId());
        } catch (BigQueryException e) {
            log.info("Logical View not created {}", e.getMessage());
        }
    }

    @AfterEach
    public void tearDown() {
        clearLogicalView();
        clearBigQuerySchema();
    }

    private void publishToTopic(String message, String topicName) throws IOException {
        Publisher publisher = null;
        try {
            publisher = Publisher.newBuilder(TopicName.of(PROJECT_ID, topicName))
                    .setCredentialsProvider(NoCredentialsProvider.create())
                    .setChannelProvider(channelProvider)
                    .build();
            ApiFuture<String> future = publisher.publish(PubsubMessage.newBuilder().setData(ByteString.copyFromUtf8(message)).putAttributes(ElmTransactionBqHeaders.CONTRACT_VERSION, CommonConstants.CONTRACT_VERSION_DEFAULT).build());
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

    private void listenToElmSubscription(String subscriptionId) {
        MessageReceiver receiver =
                (PubsubMessage pubsubMessage, AckReplyConsumer consumer) -> {
                    BasicAcknowledgeablePubsubMessage originalMessage = convertToBasicAckPubSubMessage(pubsubMessage, consumer);
                    String contractVersion = originalMessage.getPubsubMessage().getAttributesMap().get(ElmTransactionBqHeaders.CONTRACT_VERSION);
                    String messageId = originalMessage.getPubsubMessage().getMessageId();
                    String messageBody = originalMessage.getPubsubMessage().getData().toStringUtf8();
                    log.info("Subscription: {} PubSub message received with ID: {} Payload: {}", subscriptionId, messageId, messageBody);
                    try {
                        pubSubConsumerService.processPubSubToBq(messageBody, originalMessage, contractVersion);
                    } catch (Exception ignored) {
                        //This is an empty catch block as exceptions are handled using aspect
                    }
                };
        Subscriber subscriber = null;
        try {
            subscriber = Subscriber.newBuilder(ProjectSubscriptionName.of(PROJECT_ID, subscriptionId), receiver)
                    .setChannelProvider(channelProvider)
                    .setCredentialsProvider(NoCredentialsProvider.create())
                    .build();
            subscriber.startAsync().awaitRunning();
            log.info("Listening to subscription {}", subscriptionId);
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
        publishToTopic("Hello World", ELM_TOPIC_NAME);
        listenToElmSubscription(ELM_SUBSCRIPTION_ID);
        List<ILoggingEvent> logsList = listAppender.list;
        String query = TestUtils.getSelectAllQuery(DATASET_ID, TABLE_NAME);
        TableResult tableResult = runQuery(query);
        Assertions.assertEquals(0, tableResult.getTotalRows());
        ILoggingEvent logEvent = logsList.get(1);
        Assertions.assertEquals(Level.ERROR, logEvent.getLevel());
        Assertions.assertTrue(logEvent.getFormattedMessage().contains(ElmSystemException.class.getSimpleName()));
        listAppender.stop();
    }

    @Test
    public void testValidMessagePickLpnFromActive() throws IOException, InterruptedException, JSONException {
        String hdwMessage = TestData.getValidJsonPayloadPickLpnFromActive();
        publishToTopic(hdwMessage, ELM_TOPIC_NAME);
        listenToElmSubscription(ELM_SUBSCRIPTION_ID);
        String query = TestUtils.getSelectAllQuery(DATASET_ID, TABLE_NAME);
        TableResult tableResult = runQuery(query);
        TestUtils.assertTableResultsAgainstJson(tableResult, hdwMessage);
        listenToR2RSubscription(hdwMessage);
    }

    @Test
    public void testValidMessageReplenishmentAllocation() throws IOException, InterruptedException, JSONException {
        String hdwMessage = TestData.getValidJsonPayloadReplenAllocation();
        publishToTopic(hdwMessage, ELM_TOPIC_NAME);
        listenToElmSubscription(ELM_SUBSCRIPTION_ID);
        String query = TestUtils.getSelectAllQuery(DATASET_ID, TABLE_NAME);
        TableResult tableResult = runQuery(query);
        TestUtils.assertTableResultsAgainstJson(tableResult, hdwMessage);
        listenToR2RSubscription(hdwMessage);
    }

    @Test
    public void testValidMessageComplex() throws IOException, InterruptedException, JSONException {
        String hdwMessage = TestData.getValidJsonPayloadWithMultipleBuilds();
        publishToTopic(hdwMessage, ELM_TOPIC_NAME);
        listenToElmSubscription(ELM_SUBSCRIPTION_ID);
        String query = TestUtils.getSelectAllQuery(DATASET_ID, TABLE_NAME);
        TableResult tableResult = runQuery(query);
        TestUtils.assertTableResultsAgainstJson(tableResult, hdwMessage);
        listenToR2RSubscription(hdwMessage);
    }

    @Test
    public void testValidMessageIndirect() throws IOException, InterruptedException, JSONException {
        String hdwMessage = TestData.getValidJsonPayloadIndirect();
        publishToTopic(hdwMessage, ELM_TOPIC_NAME);
        listenToElmSubscription(ELM_SUBSCRIPTION_ID);
        String query = TestUtils.getSelectAllQuery(DATASET_ID, TABLE_NAME);
        TableResult tableResult = runQuery(query);
        TestUtils.assertTableResultsAgainstJson(tableResult, hdwMessage);
        listenToR2RSubscription(hdwMessage);
    }

    @Test
    public void testValidMessageLoadLpnLm() throws IOException, InterruptedException, JSONException {
        String hdwMessage = TestData.getValidJsonPayloadLoadLpnLm();
        publishToTopic(hdwMessage, ELM_TOPIC_NAME);
        listenToElmSubscription(ELM_SUBSCRIPTION_ID);
        String query = TestUtils.getSelectAllQuery(DATASET_ID, TABLE_NAME);
        TableResult tableResult = runQuery(query);
        TestUtils.assertTableResultsAgainstJson(tableResult, hdwMessage);
        listenToR2RSubscription(hdwMessage);
    }

    @Test
    public void testValidMessageMoveFromLiftToLpnLocationLm() throws IOException, InterruptedException, JSONException {
        String hdwMessage = TestData.getValidJsonPayloadMoveFromLiftToLpnLocationLm();
        publishToTopic(hdwMessage, ELM_TOPIC_NAME);
        listenToElmSubscription(ELM_SUBSCRIPTION_ID);
        String query = TestUtils.getSelectAllQuery(DATASET_ID, TABLE_NAME);
        TableResult tableResult = runQuery(query);
        TestUtils.assertTableResultsAgainstJson(tableResult, hdwMessage);
        listenToR2RSubscription(hdwMessage);
    }

    @Test
    public void testValidMessageMoveToLiftLm() throws IOException, InterruptedException, JSONException {
        String hdwMessage = TestData.getValidJsonPayloadMoveToLiftLm();
        publishToTopic(hdwMessage, ELM_TOPIC_NAME);
        listenToElmSubscription(ELM_SUBSCRIPTION_ID);
        String query = TestUtils.getSelectAllQuery(DATASET_ID, TABLE_NAME);
        TableResult tableResult = runQuery(query);
        TestUtils.assertTableResultsAgainstJson(tableResult, hdwMessage);
        listenToR2RSubscription(hdwMessage);
    }

    @Test
    public void testValidMessageReceiveLpnLm() throws IOException, InterruptedException, JSONException {
        String hdwMessage = TestData.getValidJsonPayloadReceiveLpnLm();
        publishToTopic(hdwMessage, ELM_TOPIC_NAME);
        listenToElmSubscription(ELM_SUBSCRIPTION_ID);
        String query = TestUtils.getSelectAllQuery(DATASET_ID, TABLE_NAME);
        TableResult tableResult = runQuery(query);
        TestUtils.assertTableResultsAgainstJson(tableResult, hdwMessage);
        listenToR2RSubscription(hdwMessage);
    }

    @Test
    public void testValidMessageWithDuplicateTraceId() throws IOException, InterruptedException, JSONException {
        String hdwMessage = TestData.getValidJsonPayloadPickLpnFromActive();
        publishToTopic(hdwMessage, ELM_TOPIC_NAME);
        listenToElmSubscription(ELM_SUBSCRIPTION_ID);
        String query = TestUtils.getSelectAllQuery(DATASET_ID, TABLE_NAME);
        TableResult tableResult = runQuery(query);
        TestUtils.assertTableResultsAgainstJson(tableResult, hdwMessage);
        //Asserting that if traceId not exist then bigquery insertion will happen.
        Assertions.assertEquals(1, tableResult.getTotalRows());
        listenToR2RSubscription(hdwMessage);
        //Same message published again
        publishToTopic(hdwMessage, ELM_TOPIC_NAME);
        listenToElmSubscription(ELM_SUBSCRIPTION_ID);
        List<ILoggingEvent> logsList = listAppender.list;
        query = TestUtils.getSelectAllQuery(DATASET_ID, TABLE_NAME);
        tableResult = runQuery(query);
        ILoggingEvent logEvent = logsList.get(3);
        //Asserting that if traceId duplicate then bigquery insertion will not happen.
        Assertions.assertEquals(1, tableResult.getTotalRows());
        //Asserting WARN logs present in case of duplicate traceId found
        Assertions.assertEquals(Level.WARN,logEvent.getLevel());
        Assertions.assertTrue(logEvent.getFormattedMessage().contains("Duplicate message has been found"));
        listenToR2RSubscription(hdwMessage);
    }

    @Test
    public void testValidMessageCicoEvent() throws IOException {
        String hdwMessage = TestData.getValidJsonPayloadCicoEvent();
        publishToTopic(hdwMessage, CICO_TOPIC_NAME);
        listenToElmSubscription(CICO_SUBSCRIPTION_ID);
        List<ILoggingEvent> logsList = listAppender.list;
        log.info("LogEvent: {}", logsList);
        ILoggingEvent logEvent = logsList.get(0);
        Assertions.assertEquals(Level.INFO, logEvent.getLevel());
        Assertions.assertTrue(logEvent.getFormattedMessage().contains("Published message received for event_type: CICO"));
    }

}
