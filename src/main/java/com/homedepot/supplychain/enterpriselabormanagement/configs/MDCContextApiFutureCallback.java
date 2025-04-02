package com.homedepot.supplychain.enterpriselabormanagement.configs;

import com.google.api.core.ApiFutureCallback;
import org.slf4j.MDC;

import java.util.Map;

/**
 * This implementation required to facilitates the MDC logs populate require fields in asynchronous methods
 * @param <T>
 */
public class MDCContextApiFutureCallback<T> implements ApiFutureCallback<T> {
    private final ApiFutureCallback<T> delegate;
    private final Map<String, String> contextMap;

    public MDCContextApiFutureCallback(ApiFutureCallback<T> delegate) {
        this.delegate = delegate;
        this.contextMap = MDC.getCopyOfContextMap();
    }

    @Override
    public void onFailure(Throwable throwable) {
        if (contextMap != null) {
            MDC.setContextMap(contextMap);
        }
        try {
            delegate.onFailure(throwable);
        } finally {
            MDC.clear();
        }
    }

    @Override
    public void onSuccess(T result) {
        if (contextMap != null) {
            MDC.setContextMap(contextMap);
        }
        try {
            delegate.onSuccess(result);
        } finally {
            MDC.clear();
        }
    }
}