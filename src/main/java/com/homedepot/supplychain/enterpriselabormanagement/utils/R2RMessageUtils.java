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
    public static R2RMessage createConsumerAck(HdwTransaction hdwTransaction){
        return createELMBaseMessage(hdwTransaction, FullElmR2RMetadata.TRANSACTION_TYPE_CONSUMER_ACK);
    }

    private static R2RMessage createELMBaseMessage(HdwTransaction hdwTransaction, String transactionType){
        R2RMessage r2RMessage = new R2RMessage();
        r2RMessage.setHeader(createHeader(transactionType,(String) hdwTransaction.getAttributes().get(DC_NUMBER)));
        r2RMessage.setBody(createBody(hdwTransaction));
        return r2RMessage;
    }

    private static Body createBody(HdwTransaction hdwTransaction) {
        Body body = new Body();
        List<Entity> entities = new ArrayList<>();
        Entity entity = new Entity();
        entity.setId((String) hdwTransaction.getAttributes().get(TRACE_ID));
        entities.add(entity);
        body.setEntities(entities);
        return body;
    }

    private static Map<String, String> createHeader(String transactionType, String dcNumber) {
        Map<String, String> header = new LinkedHashMap<>();
        String currentTimeStampUtc = CommonUtils.convertTimeStampToR2rFormat(LocalDateTime.now(ZoneOffset.UTC));
        header.put(FullElmR2RConsumerMessageHeaders.VERSION, FullElmR2RMetadata.VERSION_FLAG);
        header.put(FullElmR2RConsumerMessageHeaders.CONSUMER, FullElmR2RMetadata.CONSUMER);
        header.put(FullElmR2RConsumerMessageHeaders.ENTITY, FullElmR2RMetadata.ENTITY);
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