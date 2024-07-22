package com.homedepot.supplychain.enterpriselabormanagement.utils;

import com.homedepot.supplychain.enterpriselabormanagement.constants.FullElmR2RConsumerMessageHeaders;
import com.homedepot.supplychain.enterpriselabormanagement.constants.FullElmR2RMetadata;
import com.homedepot.supplychain.enterpriselabormanagement.models.elm.Body;
import com.homedepot.supplychain.enterpriselabormanagement.models.elm.R2RMessage;
import com.homedepot.supplychain.enterpriselabormanagement.models.elm.Entity;
import com.homedepot.supplychain.enterpriselabormanagement.models.hdw.HdwTransaction;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;

import static com.homedepot.supplychain.enterpriselabormanagement.constants.ElmTransactionBqHeaders.DC_NUMBER;
import static com.homedepot.supplychain.enterpriselabormanagement.constants.ElmTransactionBqHeaders.TRACE_ID;

public final class R2RMessageUtils {
    private R2RMessageUtils() {
        //util class
    }
    public static R2RMessage createConsumerAck(String dcNumber, String traceId, String messageType, String entity){
        return createELMBaseMessage(dcNumber, traceId, messageType, entity);
    }

    private static R2RMessage createELMBaseMessage(String dcNumber, String traceId, String transactionType, String entity){
        R2RMessage r2RMessage = new R2RMessage();
        r2RMessage.setHeader(createHeader(transactionType,dcNumber, entity));
        r2RMessage.setBody(createBody(traceId));
        return r2RMessage;
    }

    private static Body createBody(String traceId) {
        Body body = new Body();
        List<Entity> entities = new ArrayList<>();
        Entity entity = new Entity();
        entity.setId(traceId);
        entities.add(entity);
        body.setEntities(entities);
        return body;
    }

    private static Map<String, String> createHeader(String transactionType, String dcNumber, String entity) {
        Map<String, String> header = new LinkedHashMap<>();
        String currentTimeStampUtc = CommonUtils.convertTimeStampToR2rFormat(LocalDateTime.now(ZoneOffset.UTC));
        header.put(FullElmR2RConsumerMessageHeaders.VERSION, FullElmR2RMetadata.VERSION_FLAG);
        header.put(FullElmR2RConsumerMessageHeaders.CONSUMER, FullElmR2RMetadata.CONSUMER);
        header.put(FullElmR2RConsumerMessageHeaders.ENTITY, entity);
        header.put(FullElmR2RConsumerMessageHeaders.LOCATION_ID, dcNumber);
        header.put(FullElmR2RConsumerMessageHeaders.PRODUCER, FullElmR2RMetadata.PRODUCER);
        header.put(FullElmR2RConsumerMessageHeaders.TRANSACTION_TIMESTAMP, currentTimeStampUtc);
        header.put(FullElmR2RConsumerMessageHeaders.TRANSACTION_TYPE, transactionType);
        header.put(FullElmR2RConsumerMessageHeaders.TRANSACTION_ID, UUID.randomUUID().toString());
        header.put(FullElmR2RConsumerMessageHeaders.START_TIMESTAMP, currentTimeStampUtc);
        header.put(FullElmR2RConsumerMessageHeaders.END_TIMESTAMP, currentTimeStampUtc);
        return header;
    }

}