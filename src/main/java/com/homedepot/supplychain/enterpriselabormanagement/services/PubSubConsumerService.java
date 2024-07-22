package com.homedepot.supplychain.enterpriselabormanagement.services;

import com.google.cloud.bigquery.BigQueryException;
import com.google.cloud.spring.pubsub.support.BasicAcknowledgeablePubsubMessage;
import com.homedepot.supplychain.enterpriselabormanagement.constants.ErrorMessages;
import com.homedepot.supplychain.enterpriselabormanagement.exceptions.BigQueryResponseException;
import com.homedepot.supplychain.enterpriselabormanagement.exceptions.ElmBusinessException;
import com.homedepot.supplychain.enterpriselabormanagement.exceptions.ElmSystemException;
import com.homedepot.supplychain.enterpriselabormanagement.exceptions.JsonValidationException;
import com.homedepot.supplychain.enterpriselabormanagement.models.elm.R2RMessage;
import com.homedepot.supplychain.enterpriselabormanagement.models.hdw.HdwTransaction;
import com.homedepot.supplychain.enterpriselabormanagement.utils.CommonUtils;
import com.homedepot.supplychain.enterpriselabormanagement.utils.R2RMessageUtils;
import com.homedepot.supplychain.enterpriselabormanagement.utils.JsonUtils;
import com.homedepot.supplychain.enterpriselabormanagement.utils.RowMapperUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static com.homedepot.supplychain.enterpriselabormanagement.constants.CommonConstants.*;
import static com.homedepot.supplychain.enterpriselabormanagement.constants.CommonConstants.ELM_APP;

@Service
@Slf4j
public class PubSubConsumerService {

    @Value("${spring.cloud.gcp.r2r.project-id}")
    private String projectId;

    @Value("${elm-r2r.gcp.pubsub.r2r-consumer-ack-topic-name}")
    private String consumerAckTopicName;


    private final ElmTransactionBigQueryService elmTransactionBigQueryService;

    private final PubSubPublisherService pubSubPublisherService;

    @Autowired
    public PubSubConsumerService(ElmTransactionBigQueryService elmTransactionBigQueryService,PubSubPublisherService pubSubPublisherService){
        this.elmTransactionBigQueryService = elmTransactionBigQueryService;
        this.pubSubPublisherService =  pubSubPublisherService;
    }

    public void processPubSubToBq(String messageId, String messageBody, BasicAcknowledgeablePubsubMessage originalMessage) {
        //Reading pubSub Message to HdwTransaction
        HdwTransaction hdwTransaction;
        List<Map<String, Object>> rows;
        try {
            if (!StringUtils.isBlank(messageBody)) {
                String eventType = JsonUtils.readJsonNode(messageBody,
                        FILE_PATH_DELIMITER+ATTRIBUTES_JSON_PATH+FILE_PATH_DELIMITER+EVENT_TYPE_JSON_PATH);
                hdwTransaction = switch (eventType) {
                    case DIRECT_EVENT ->
                            (HdwTransaction) JsonUtils.validateAndReadJsonMessage(messageBody, HDW_DIRECT_SCHEMA_FILE_NAME, HdwTransaction.class);
                    case INDIRECT_EVENT ->
                            (HdwTransaction) JsonUtils.validateAndReadJsonMessage(messageBody, HDW_INDIRECT_SCHEMA_FILE_NAME, HdwTransaction.class);
                    default -> throw new IllegalStateException("Missing or Unexpected value for event_type: " + eventType);
                };
            } else {
                throw new JsonValidationException(ErrorMessages.MESSAGE_BODY_BLANK);
            }
        } catch (Exception e) {
            originalMessage.ack();
            throw new ElmBusinessException(messageId, ErrorMessages.JSON_READ_FAILED, e);
        }
        try {
            rows = RowMapperUtils.getRows(hdwTransaction);
        } catch (Exception e){
            originalMessage.ack();
            throw new ElmBusinessException(messageId, ErrorMessages.ROW_MAPPER_PROCESSING_FAILED, e);
        }
        log.debug("PubSubConsumerService: Calling bigQueryService.insertAll()");
        try {
            elmTransactionBigQueryService.insertAll(rows);
            originalMessage.ack();
            log.info("ElmTransactionBigQueryService: Acknowledgement sent for PubSub message with ID: {}", messageId);
        }catch (BigQueryResponseException e) {
            originalMessage.ack();
            throw new ElmBusinessException(messageId, ErrorMessages.BIGQUERY_INSERT_RESPONSE_ERROR, e);
        } catch (BigQueryException e) {
            throw new ElmSystemException(this.elmTransactionBigQueryService.getTableName(), e.getMessage(), e);
        }
        try {
            createAndPublishConsumerAckMessage(hdwTransaction);
        } catch (Exception e) {
            throw new ElmSystemException(ELM_APP, "Failed to publish message to 1SC run2run consumer ack topic", e);
        }
    }

    private void createAndPublishConsumerAckMessage(HdwTransaction hdwTransaction) throws IOException {
        R2RMessage r2RMessage = R2RMessageUtils.createConsumerAck(hdwTransaction);
        pubSubPublisherService.publishToTopic(CommonUtils.convertToJsonMessage(r2RMessage), projectId,consumerAckTopicName);
    }
}