package com.homedepot.supplychain.enterpriselabormanagement.services;

import com.google.cloud.bigquery.FieldValueList;
import com.google.cloud.bigquery.TableResult;
import com.google.cloud.spring.pubsub.support.BasicAcknowledgeablePubsubMessage;
import com.homedepot.supplychain.enterpriselabormanagement.constants.ElmTransactionBqHeaders;
import com.homedepot.supplychain.enterpriselabormanagement.constants.ErrorMessages;
import com.homedepot.supplychain.enterpriselabormanagement.exceptions.ElmBusinessException;
import com.homedepot.supplychain.enterpriselabormanagement.exceptions.JsonValidationException;
import com.homedepot.supplychain.enterpriselabormanagement.models.hdw.HdwTransaction;
import com.homedepot.supplychain.enterpriselabormanagement.utils.JsonUtils;
import com.homedepot.supplychain.enterpriselabormanagement.utils.RowMapperUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static com.homedepot.supplychain.enterpriselabormanagement.constants.CommonConstants.FILE_PATH_DELIMITER;
import static com.homedepot.supplychain.enterpriselabormanagement.constants.CommonConstants.DC_NUMBER;
import static com.homedepot.supplychain.enterpriselabormanagement.constants.CommonConstants.TRACE_ID;
import static com.homedepot.supplychain.enterpriselabormanagement.constants.CommonConstants.DIRECT_EVENT;
import static com.homedepot.supplychain.enterpriselabormanagement.constants.CommonConstants.INDIRECT_EVENT;
import static com.homedepot.supplychain.enterpriselabormanagement.constants.CommonConstants.HDW_INDIRECT_SCHEMA_FILE_NAME;
import static com.homedepot.supplychain.enterpriselabormanagement.constants.CommonConstants.LABOR_EVENT_JSON_PATH;
import static com.homedepot.supplychain.enterpriselabormanagement.constants.CommonConstants.HDW_DIRECT_SCHEMA_FILE_NAME;
import static com.homedepot.supplychain.enterpriselabormanagement.constants.CommonConstants.EVENT_TYPE_JSON_PATH;
import static com.homedepot.supplychain.enterpriselabormanagement.constants.CommonConstants.CICO_EVENT;
import static com.homedepot.supplychain.enterpriselabormanagement.constants.CommonConstants.HDW_CICO_SCHEMA_FILE_NAME;

@Service
@Slf4j
public class PubSubConsumerService {

    private final ElmTransactionBigQueryService elmTransactionBigQueryService;
    private final PubSubPublisherService pubSubPublisherService;
    private final CicoProcessorService cicoProcessorService;

    @Autowired
    public PubSubConsumerService(ElmTransactionBigQueryService elmTransactionBigQueryService, PubSubPublisherService pubSubPublisherService, CicoProcessorService cicoProcessorService){
        this.elmTransactionBigQueryService = elmTransactionBigQueryService;
        this.pubSubPublisherService = pubSubPublisherService;
        this.cicoProcessorService = cicoProcessorService;
    }

    public void processPubSubToBq(String messageBody, BasicAcknowledgeablePubsubMessage originalMessage, String contractVersion) {
        //Reading pubSub Message to HdwTransaction
        HdwTransaction hdwTransaction;
        List<Map<String, Object>> rows;
        String traceId = null;
        String dcNumber = null;
        String eventType;
        try {
            if (!StringUtils.isBlank(messageBody)) {
                eventType = JsonUtils.readJsonNode(messageBody,
                        FILE_PATH_DELIMITER+ LABOR_EVENT_JSON_PATH +FILE_PATH_DELIMITER+EVENT_TYPE_JSON_PATH);
                traceId = JsonUtils.readJsonNode(messageBody,
                        FILE_PATH_DELIMITER+ LABOR_EVENT_JSON_PATH +FILE_PATH_DELIMITER+TRACE_ID);
                dcNumber = JsonUtils.readJsonNode(messageBody,
                        FILE_PATH_DELIMITER+ LABOR_EVENT_JSON_PATH +FILE_PATH_DELIMITER+DC_NUMBER);
                hdwTransaction = switch (eventType) {
                    case DIRECT_EVENT ->
                            (HdwTransaction) JsonUtils.validateAndReadJsonMessage(messageBody, HDW_DIRECT_SCHEMA_FILE_NAME, HdwTransaction.class, contractVersion);
                    case INDIRECT_EVENT ->
                            (HdwTransaction) JsonUtils.validateAndReadJsonMessage(messageBody, HDW_INDIRECT_SCHEMA_FILE_NAME, HdwTransaction.class, contractVersion);
                    case CICO_EVENT ->
                            (HdwTransaction) JsonUtils.validateAndReadJsonMessage(messageBody, HDW_CICO_SCHEMA_FILE_NAME, HdwTransaction.class, contractVersion);
                    default -> throw new IllegalStateException("Missing or Unexpected value for event_type: " + eventType);
                };
                log.info("Published message received for event_type: {}", eventType);
            } else {
                throw new JsonValidationException(ErrorMessages.MESSAGE_BODY_BLANK);
            }
        } catch (Exception e) {
            originalMessage.ack();
            pubSubPublisherService.createAndPublishConsumerNackMessage(dcNumber, traceId,ErrorMessages.JSON_READ_FAILED);
            throw new ElmBusinessException(ErrorMessages.JSON_READ_FAILED, e);
        }
        if (isTraceIdDuplicate(hdwTransaction, eventType)) {
            originalMessage.ack();
            return;
        }
        if (CICO_EVENT.equals(eventType)) {
            cicoProcessorService.processCicoEvents(hdwTransaction, originalMessage, dcNumber);
            originalMessage.ack();
            pubSubPublisherService.createAndPublishConsumerAckMessage(dcNumber, traceId);
        } else {
            try {
                rows = RowMapperUtils.getRows(hdwTransaction);
            } catch (Exception e) {
                originalMessage.ack();
                pubSubPublisherService.createAndPublishConsumerNackMessage(dcNumber, traceId, ErrorMessages.ROW_MAPPER_PROCESSING_FAILED);
                throw new ElmBusinessException(ErrorMessages.ROW_MAPPER_PROCESSING_FAILED, e);
            }
            log.debug("PubSubConsumerService: Calling bigQueryService.insertAll()");
            elmTransactionBigQueryService.insertBqStreaming(hdwTransaction, originalMessage, dcNumber, rows);
            originalMessage.ack();
            log.info("PubSubConsumerService: Acknowledgement sent for PubSub");
            pubSubPublisherService.createAndPublishConsumerAckMessage(dcNumber, traceId);
        }
    }

    private boolean isTraceIdDuplicate(HdwTransaction hdwTransaction, String eventType) {
        String existingTraceId = null;
        TableResult tableResult = elmTransactionBigQueryService.getExistingTraceIdsFromBqViewOrTable(hdwTransaction.getLaborEvent().getTraceId(), hdwTransaction.getLaborEvent().getDcNumber(), eventType);
        Iterator<FieldValueList> fieldValueListIterator = tableResult.iterateAll().iterator();
        if (fieldValueListIterator.hasNext()) {
            existingTraceId = fieldValueListIterator.next().get(ElmTransactionBqHeaders.TRACE_ID).getStringValue();
        }
        if (!StringUtils.isEmpty(existingTraceId)) {
            log.warn("Duplicate message has been found, dc_number: {}, and publish_timestamp: {}", hdwTransaction.getLaborEvent().getDcNumber(), hdwTransaction.getLaborEvent().getPublishTimestamp());
            return true;
        }
        return false;
    }
}