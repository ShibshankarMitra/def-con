package com.homedepot.supplychain.enterpriselabormanagement.configs;

import com.google.api.core.ApiFutureCallback;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.slf4j.MDC;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

@ExtendWith(SpringExtension.class)
class MDCContextApiFutureCallbackTest {

    @Mock
    private ApiFutureCallback<String> delegate;
    @Captor
    private ArgumentCaptor<Throwable> throwableCaptor;
    @Captor
    private ArgumentCaptor<String> resultCaptor;
    private MDCContextApiFutureCallback<String> mdcContextApiFutureCallback;

    @BeforeEach
    void setUp() {
        Map<String, String> contextMap = Map.of("traceId", "852037e8-9f6c-4188-9c0f-7d3d20bc87c6");
        MDC.setContextMap(contextMap);
        mdcContextApiFutureCallback = new MDCContextApiFutureCallback<>(delegate);
        MDC.clear();
    }

    @Test
    void testOnFailure() {
        Throwable throwable = new RuntimeException("Test Exception");
        mdcContextApiFutureCallback.onFailure(throwable);
        verify(delegate).onFailure(throwableCaptor.capture());
        assertEquals(throwable, throwableCaptor.getValue());
    }

    @Test
    void testOnSuccess() {
        String result = "Test Result";
        mdcContextApiFutureCallback.onSuccess(result);
        verify(delegate).onSuccess(resultCaptor.capture());
        assertEquals(result, resultCaptor.getValue());
    }
}